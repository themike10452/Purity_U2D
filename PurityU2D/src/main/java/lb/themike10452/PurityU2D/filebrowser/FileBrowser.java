package lb.themike10452.purityu2d.filebrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.themike10452.purityu2d.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import lb.themike10452.purityu2d.Tools;

/**
 * Created by Mike on 9/22/2014.
 */
public class FileBrowser extends Activity {

    public static String EXTRA_ALLOWED_EXTENSIONS = "ALLOWED_EXTENSIONS";
    public static String EXTRA_SHOW_FOLDERS_ONLY = "SHOW_FOLDERS_ONLY";
    public static String EXTRA_START_DIRECTORY = "START";
    public static String ACTION_DIRECTORY_SELECTED = "THEMIKE10452.FB.FOLDER.SELECTED";
    public static String ACTION_FILE_SELECTED = "THEMIKE10452.FB.FILE.SELECTED";
    public static String ACTION_CANCELED = "THEMIKE10452.FB.CANCELED";
    public File WORKING_DIRECTORY;
    public Boolean SHOW_FOLDERS_ONLY;

    private Comparator<File> comparator = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.isDirectory() && f2.isFile())
                return -2;
            else if (f1.isFile() && f2.isDirectory())
                return 2;
            else
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    };

    private Adapter adapter;
    private ArrayList<File> items;
    private HashMap<String, Parcelable> scrollHistory;
    private ListView listView;
    private String[] ALLOWED_EXTENSIONS;
    private int ScrollY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assert getActionBar() != null;
            getActionBar().setElevation(0);
        }
        setContentView(R.layout.file_browser);
        overridePendingTransition(R.anim.slide_in_btt, R.anim.stay_still);

        Bundle extras = getIntent().getExtras();
        scrollHistory = new HashMap<>();

        listView = (ListView) findViewById(R.id.list);

        try {
            ALLOWED_EXTENSIONS = extras.getStringArray(EXTRA_ALLOWED_EXTENSIONS);
        } catch (NullPointerException e) {
            ALLOWED_EXTENSIONS = null;
        }
        try {
            SHOW_FOLDERS_ONLY = extras.getBoolean(EXTRA_SHOW_FOLDERS_ONLY);
        } catch (NullPointerException e) {
            SHOW_FOLDERS_ONLY = false;
        }
        try {
            Bundle bundle = new Bundle();
            bundle.putString("folder", extras.getString(EXTRA_START_DIRECTORY));
            updateScreen(bundle);
        } catch (NullPointerException ignored) {
            updateScreen(null);
        }

        findViewById(R.id.btn_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent out = new Intent(ACTION_DIRECTORY_SELECTED);
                out.putExtra("folder", WORKING_DIRECTORY.getAbsolutePath() + File.separator);
                sendBroadcast(out);
                finish();
            }
        });

        findViewById(R.id.btn_select).setVisibility(SHOW_FOLDERS_ONLY ? View.VISIBLE : View.INVISIBLE);

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(new Intent(ACTION_CANCELED));
                finish();
            }
        });
    }

    public void updateScreen(Bundle pac) {
        final File root = pac == null ? Environment.getExternalStorageDirectory() : new File(pac.getString("folder"));
        WORKING_DIRECTORY = root;
        ((TextView) findViewById(R.id.textView_cd)).setText(root.getAbsolutePath());

        if (items == null)
            items = new ArrayList<>();
        else
            items.clear();

        if (root.listFiles() != null) {
            for (File f : root.listFiles()) {
                if (f.isDirectory()) {
                    items.add(f);
                } else if (!SHOW_FOLDERS_ONLY) {
                    if (ALLOWED_EXTENSIONS == null) {
                        items.add(f);
                    } else if (arrayContains(ALLOWED_EXTENSIONS, Tools.getFileExtension(f))) {
                        items.add(f);
                    }
                }
            }

            Collections.sort(items, comparator);
        }

        if (root.getParentFile() != null)
            items.add(0, root.getParentFile());

        listView.setAdapter(adapter = new Adapter(FileBrowser.this, R.layout.file_browser_list_item, items));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapter.files.get(i).isDirectory()) {
                    if (i == 0 && root.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
                        return;

                    scrollHistory.put(WORKING_DIRECTORY.getAbsolutePath(), listView.onSaveInstanceState());

                    Bundle pac = new Bundle();
                    pac.putString("folder", adapter.files.get(i).getAbsolutePath());
                    updateScreen(pac);
                } else {
                    Intent out = new Intent(ACTION_FILE_SELECTED);
                    out.putExtra("folder", WORKING_DIRECTORY.getAbsolutePath() + File.separator);
                    out.putExtra("file", adapter.files.get(i).getAbsolutePath());
                    out.putExtra("filename", adapter.files.get(i).getName());
                    sendBroadcast(out);
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (WORKING_DIRECTORY.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return;

        String path = adapter.files.get(0).getAbsolutePath();

        Bundle pac = new Bundle();
        pac.putString("folder", path);
        updateScreen(pac);

        if (scrollHistory.containsKey(path)) {
            listView.onRestoreInstanceState(scrollHistory.get(path));
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay_still, R.anim.slide_in_ttb);
    }

    private boolean arrayContains(String[] array, String element) {
        for (String s : array) {
            if (s.equalsIgnoreCase(element))
                return true;
        }
        return false;
    }

}
