package lb.themike10452.purityu2d;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.themike10452.purityu2d.R;

import java.io.File;
import java.util.ArrayList;

import lb.themike10452.purityu2d.filebrowser.FileBrowser;

/**
 * Created by Mike on 4/15/2015.
 */
public class InstallationActivity extends Activity {

    public static String EXTRA_INITIAL_ZIP = "extra_initial_zip";

    private Activity mActivity;
    private ArrayList<ViewHolder> queue;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private SharedPreferences preferences;
    private ViewGroup queueList;

    @Override
    protected void onStart() {
        super.onStart();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(Keys.TAG_NOTIF, 3721);
        manager.cancel(Keys.TAG_NOTIF, 3722);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (MainActivity.preferences != null) {
                onBackPressed();
            } else {
                Intent i = new Intent(this, MainActivity.class);
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivity(i);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_rtl, R.anim.slide_out_rtl);
        setContentView(R.layout.install);

        assert getActionBar() != null;
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mActivity = this;
        preferences = getSharedPreferences(Keys.SharedPrefsKey, MODE_PRIVATE);
        queue = new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        queueList = (ViewGroup) findViewById(R.id.queueList);
        seekBar = (SeekBar) findViewById(R.id.install_slider);

        findViewById(R.id.button_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (queueList.getChildCount() <= 10) {
                    addFileToQueue();
                } else {
                    Toast.makeText(mActivity.getApplicationContext(), "10/10", Toast.LENGTH_SHORT).show();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (seekBar.getProgress() < seekBar.getMax()) {
                    resetSlider(seekBar);
                } else {
                    seekBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            resetSlider(seekBar);
                        }
                    }, 3000);
                    rebootAndInstall();
                }
            }
        });

        if (getIntent().hasExtra(EXTRA_INITIAL_ZIP)) {
            addFileToQueue(getIntent().getStringExtra(EXTRA_INITIAL_ZIP));
        }

        onPostQueueModification();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void rebootAndInstall() {
        Switch backupKernel = (Switch) findViewById(R.id.switch_restoreKernel);
        Switch wipeCache = (Switch) findViewById(R.id.switch_wipeCache);
        Switch wipeDalvik = (Switch) findViewById(R.id.switch_wipeDalvik);
        Switch factoryReset = (Switch) findViewById(R.id.switch_wipeData);

        int FLAGS = 0;

        if (backupKernel.isChecked())
            FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_MAINTAIN_KERNEL);

        if (factoryReset.isChecked()) {
            FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_WIPE_DATA);
            FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_WIPE_CACHE);
            FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_WIPE_DALVIK);
        } else {
            if (wipeCache.isChecked())
                FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_WIPE_CACHE);

            if (wipeDalvik.isChecked())
                FLAGS = addFlag(FLAGS, RecoveryScriptHandler.FLAG_WIPE_DALVIK);
        }

        int size;
        String[] FILES = new String[size = queue.size()];

        for (int i = 0; i < size; i++) {
            FILES[i] = queue.get(i).absolutePath;
        }

        RecoveryScriptHandler.FLAGS = FLAGS;
        RecoveryScriptHandler.FILES = FILES;
        RecoveryScriptHandler.flush(getApplicationContext());
    }

    private int addFlag(int flags, int flag) {
        return flags | flag;
    }

    private void addFileToQueue() {
        Intent intent = new Intent(mActivity, FileBrowser.class);
        intent.putExtra(FileBrowser.EXTRA_START_DIRECTORY, preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
        intent.putExtra(FileBrowser.EXTRA_SHOW_FOLDERS_ONLY, false);
        intent.putExtra(FileBrowser.EXTRA_ALLOWED_EXTENSIONS, new String[]{"zip"});
        startActivity(intent);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                if (FileBrowser.ACTION_FILE_SELECTED.equals(intent.getAction())) {
                    addFileToQueue(intent.getStringExtra("file"));
                }
            }
        };

        IntentFilter filter = new IntentFilter(FileBrowser.ACTION_FILE_SELECTED);
        filter.addAction(FileBrowser.ACTION_CANCELED);
        registerReceiver(receiver, filter);
    }

    private void addFileToQueue(String filePath) {
        final View view = getLayoutInflater().inflate(R.layout.queue_item, null, false);
        final ViewHolder holder = new ViewHolder();
        holder.textView = (TextView) view.findViewById(R.id.text);
        holder.button = (ImageButton) view.findViewById(R.id.button_remove);
        holder.absolutePath = filePath;
        holder.tag = holder.toString();

        holder.textView.setText(filePath.substring(filePath.lastIndexOf(File.separator) + 1));
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queueList.removeView(view);
                queue.remove(holder);
                onPostQueueModification();
            }
        });

        queue.add(holder);
        queueList.addView(view);
        onPostQueueModification();
    }

    private void setQueueProgress(int progress) {
        int currentProgress;
        if (progress >= 0 && progress != (currentProgress = progressBar.getProgress())) {
            ValueAnimator animator = ValueAnimator.ofInt(currentProgress, progress * 100);
            animator.setDuration(1000);
            animator.setStartDelay(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    progressBar.setProgress((Integer) animation.getAnimatedValue());
                }
            });
            animator.start();
        }
    }

    private void onPostQueueModification() {
        setQueueProgress((queueList.getChildCount() - 1));
        if (queue.size() == 0) {
            seekBar.setEnabled(false);
            queueList.getChildAt(0).setVisibility(View.VISIBLE);
        } else {
            seekBar.setEnabled(true);
            queueList.getChildAt(0).setVisibility(View.GONE);
        }
    }

    private void resetSlider(final SeekBar seekBar) {
        ValueAnimator animator = ValueAnimator.ofInt(seekBar.getProgress(), 0);
        animator.setDuration(500);
        animator.setStartDelay(100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                seekBar.setProgress((Integer) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    private class ViewHolder {
        String tag;
        String absolutePath;
        TextView textView;
        ImageButton button;
    }
}
