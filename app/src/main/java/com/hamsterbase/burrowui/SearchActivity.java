package com.hamsterbase.burrowui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hamsterbase.burrowui.service.AppInfo;
import com.hamsterbase.burrowui.service.AppManagementService;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchActivity extends Activity {

    private static final String TAG = "SearchActivity";

    private EditText searchInput;
    private Button exitButton;
    private ImageButton clearButton;
    private ListView appListView;
    private List<AppInfo> allApps;
    private List<SearchItem> allSearchItems;
    private List<SearchItem> filteredSearchItems;
    private AppAdapter adapter;
    private AppManagementService appManagementService;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        searchInput = findViewById(R.id.search_input);
        searchInput.requestFocus();
        exitButton = findViewById(R.id.exit_button);
        clearButton = findViewById(R.id.clear_button);
        appListView = findViewById(R.id.app_list);
        appListView.setDivider(null);
        appListView.setVerticalScrollBarEnabled(false);
        appListView.setDividerHeight(0);
        appListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        appManagementService = ((BurrowUIApplication) getApplication()).getAppManagementService();
        settingsManager = new SettingsManager(this);

        allApps = appManagementService.listApps();
        allSearchItems = buildSearchItems();
        filteredSearchItems = new ArrayList<>();
        adapter = new AppAdapter();
        appListView.setAdapter(adapter);


        // 设置 IME 选项为 "Done"
        searchInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // 设置 Editor Action Listener
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // 在这里处理 "Done" 按钮的点击事件
                    // 例如，可以隐藏软键盘
                    // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    // imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    // 返回 true 表示我们已经处理了这个事件
                    return true;
                }
                // 如果不是 "Done" 动作，让系统继续处理
                return false;
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
                updateClearButtonVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
            }
        });

        appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchItem item = filteredSearchItems.get(position);
                if (item.isShortcut()) {
                    launchShortcut(item.getShortcutIntentUri());
                } else {
                    appManagementService.launchApp(item.getApp());
                }
            }
        });

        updateClearButtonVisibility();
    }


    private void filterApps(String query) {
        filteredSearchItems.clear();
        String normalizedQuery = query.toLowerCase(Locale.getDefault());
        for (SearchItem item : allSearchItems) {
            if (item.getLabel().toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                filteredSearchItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private List<SearchItem> buildSearchItems() {
        List<SearchItem> items = new ArrayList<>();
        for (AppInfo app : allApps) {
            items.add(SearchItem.forApp(app));
        }

        List<SettingsManager.SelectedItem> selectedItems = settingsManager.getSelectedItems();
        for (SettingsManager.SelectedItem selectedItem : selectedItems) {
            if (!"shortcut".equals(selectedItem.getType())) {
                continue;
            }

            Map<String, String> meta = selectedItem.getMeta();
            String name = meta.get("name");
            String intentUri = meta.get("intent");
            String iconPath = meta.get("iconPath");

            if (name != null && intentUri != null) {
                items.add(SearchItem.forShortcut(name, intentUri, iconPath));
            }
        }

        return items;
    }

    private void launchShortcut(String intentUri) {
        try {
            Intent intent = Intent.parseUri(intentUri, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (URISyntaxException ignored) {
        }
    }

    private void updateClearButtonVisibility() {
        if (searchInput.getText().length() > 0) {
            clearButton.setVisibility(View.VISIBLE);
        } else {
            clearButton.setVisibility(View.GONE);
        }
    }

    private class AppAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        AppAdapter() {
            inflater = LayoutInflater.from(SearchActivity.this);
        }

        @Override
        public int getCount() {
            return filteredSearchItems.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredSearchItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.app_item, parent, false);
                holder = new ViewHolder();
                holder.appIcon = convertView.findViewById(R.id.appIcon);
                holder.appName = convertView.findViewById(R.id.appName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            SearchItem item = filteredSearchItems.get(position);
            holder.appName.setText(item.getLabel());
            if (item.isShortcut()) {
                holder.appIcon.setImageDrawable(null);
                String iconPath = item.getShortcutIconPath();
                if (iconPath != null) {
                    File iconFile = new File(iconPath);
                    if (iconFile.exists()) {
                        holder.appIcon.setImageBitmap(BitmapFactory.decodeFile(iconPath));
                    }
                }
            } else {
                loadAppIcon(holder.appIcon, item.getApp());
            }

            return convertView;
        }
    }

    private static class SearchItem {
        private final AppInfo app;
        private final String label;
        private final String shortcutIntentUri;
        private final String shortcutIconPath;

        private SearchItem(AppInfo app, String label, String shortcutIntentUri, String shortcutIconPath) {
            this.app = app;
            this.label = label;
            this.shortcutIntentUri = shortcutIntentUri;
            this.shortcutIconPath = shortcutIconPath;
        }

        static SearchItem forApp(AppInfo app) {
            return new SearchItem(app, app.getLabel(), null, null);
        }

        static SearchItem forShortcut(String label, String shortcutIntentUri, String shortcutIconPath) {
            return new SearchItem(null, label, shortcutIntentUri, shortcutIconPath);
        }

        boolean isShortcut() {
            return shortcutIntentUri != null;
        }

        AppInfo getApp() {
            return app;
        }

        String getLabel() {
            return label;
        }

        String getShortcutIntentUri() {
            return shortcutIntentUri;
        }

        String getShortcutIconPath() {
            return shortcutIconPath;
        }
    }

    private static class ViewHolder {
        ImageView appIcon;
        TextView appName;
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
}
