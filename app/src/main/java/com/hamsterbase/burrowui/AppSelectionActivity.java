package com.hamsterbase.burrowui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hamsterbase.burrowui.service.AppInfo;
import com.hamsterbase.burrowui.service.AppManagementService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppSelectionActivity extends Activity implements NavigationBar.OnBackClickListener {
    private List<AppInfo> allApps;
    private List<SettingsManager.SelectedItem> selectedItems;
    private AppAdapter appAdapter;
    private SettingsManager settingsManager;
    private AppManagementService appManagementService;
    private SparseBooleanArray selectedState;
    private List<AppInfo> filteredApps;
    private List<Integer> filteredAppIndexes;
    private ListView appListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_app_selection);
        appListView = findViewById(R.id.appListView);
        appListView.setDivider(null);
        appListView.setDividerHeight(0);
        appListView.setVerticalScrollBarEnabled(false);
        appListView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        settingsManager = new SettingsManager(this);
        appManagementService = ((BurrowUIApplication) getApplication()).getAppManagementService();
        loadApps();
        appAdapter = new AppAdapter();
        appListView.setAdapter(appAdapter);

        EditText appSearchEditText = findViewById(R.id.appSearchEditText);
        ImageButton clearSearchButton = findViewById(R.id.clearSearchButton);
        appSearchEditText.setCursorVisible(false);
        appSearchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            appSearchEditText.setCursorVisible(hasFocus);
            if (!hasFocus) {
                hideKeyboardAndClearFocus(appSearchEditText);
            }
        });
        appSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
                clearSearchButton.setVisibility(s == null || s.length() == 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        clearSearchButton.setOnClickListener(v -> {
            appSearchEditText.setText("");
            hideKeyboardAndClearFocus(appSearchEditText);
        });
        appSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            boolean isHandledAction = actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_SEND;
            boolean isEnterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            if (isHandledAction || isEnterKey) {
                hideKeyboardAndClearFocus(appSearchEditText);
                return true;
            }
            return false;
        });
        appListView.requestFocus();
        hideKeyboardAndClearFocus(appSearchEditText);

        NavigationBar navigationBar = findViewById(R.id.navigation_bar);
        navigationBar.setListView(appListView);
        navigationBar.setOnBackClickListener(this);
    }

    private void loadApps() {
        allApps = appManagementService.listApps();
        selectedItems = settingsManager.getSelectedItems();
        selectedState = new SparseBooleanArray();
        for (int i = 0; i < allApps.size(); i++) {
            selectedState.put(i, appManagementService.isAppSelected(allApps.get(i), selectedItems));
        }
        applyFilter("");
    }

    private void applyFilter(String query) {
        filteredApps = new ArrayList<>();
        filteredAppIndexes = new ArrayList<>();
        String normalizedQuery = query.toLowerCase(Locale.getDefault()).trim();

        for (int i = 0; i < allApps.size(); i++) {
            AppInfo app = allApps.get(i);
            String label = app.getLabel();
            if (normalizedQuery.isEmpty() || (label != null && label.toLowerCase(Locale.getDefault()).contains(normalizedQuery))) {
                filteredApps.add(app);
                filteredAppIndexes.add(i);
            }
        }

        if (appAdapter != null) {
            appAdapter.notifyDataSetChanged();
        }
    }

    public void onBackClick() {
        finish();
    }

    private class AppAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        AppAdapter() {
            inflater = LayoutInflater.from(AppSelectionActivity.this);
        }

        @Override
        public int getCount() {
            return filteredApps.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.settings_app_item, parent, false);
                holder = new ViewHolder();
                holder.appIcon = convertView.findViewById(R.id.appIcon);
                holder.appName = convertView.findViewById(R.id.appName);
                holder.appCheckImage = convertView.findViewById(R.id.appCheckImage);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppInfo app = filteredApps.get(position);
            final int originalIndex = filteredAppIndexes.get(position);
            holder.appName.setText(app.getLabel());

            boolean isSelected = selectedState.get(originalIndex);
            updateCheckImage(holder.appCheckImage, isSelected);

            loadAppIcon(holder.appIcon, app);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean newState = !selectedState.get(originalIndex);
                    selectedState.put(originalIndex, newState);
                    if (newState) {
                        addSelectedApp(app);
                    } else {
                        removeSelectedApp(app);
                    }
                    updateCheckImage(holder.appCheckImage, newState);
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }

        private void updateCheckImage(ImageView imageView, boolean isSelected) {
            imageView.setImageResource(isSelected ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        }
    }

    private static class ViewHolder {
        ImageView appIcon;
        TextView appName;
        ImageView appCheckImage;
    }

    private void loadAppIcon(ImageView imageView, AppInfo app) {
        new LoadIconTask(imageView).execute(app);
    }

    private class LoadIconTask extends AsyncTask<AppInfo, Void, Drawable> {
        private final WeakReference<ImageView> imageViewReference;

        LoadIconTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Drawable doInBackground(AppInfo... params) {
            AppInfo app = params[0];
            return appManagementService.getIcon(app.getPackageName(), app.getUserId());
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            if (isCancelled()) {
                drawable = null;
            }

            ImageView imageView = imageViewReference.get();
            if (imageView != null && drawable != null) {
                imageView.setImageDrawable(drawable);
            }
        }
    }


    private void addSelectedApp(AppInfo app) {
        SettingsManager.SelectedItem newItem = appManagementService.to(app);
        settingsManager.pushSelectedItem(newItem);
        selectedItems.add(newItem);
    }

    private void removeSelectedApp(AppInfo app) {
        for (int i = 0; i < selectedItems.size(); i++) {
            SettingsManager.SelectedItem item = selectedItems.get(i);
            if (appManagementService.isSelectItemEqualWith(app, item)) {
                settingsManager.deleteSelectedItem(i);
                selectedItems.remove(i);
                break;
            }
        }
    }

    private void hideKeyboardAndClearFocus(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        if (view instanceof EditText) {
            ((EditText) view).setCursorVisible(false);
        }
        view.clearFocus();
        if (appListView != null) {
            appListView.requestFocus();
        }
    }
}
