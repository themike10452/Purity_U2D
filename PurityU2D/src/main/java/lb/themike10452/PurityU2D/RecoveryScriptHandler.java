package lb.themike10452.purityu2d;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.themike10452.purityu2d.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Mike on 4/16/2015.
 */
public class RecoveryScriptHandler {

    public static int FLAG_WIPE_CACHE = 1 << 1;
    public static int FLAG_WIPE_DALVIK = 1 << 2;
    public static int FLAG_WIPE_DATA = 1 << 3;
    public static int FLAG_MAINTAIN_KERNEL = 1 << 4;

    public static String[] FILES;
    public static int FLAGS;

    public static boolean flush(final Context context) {
        if (FILES == null) {
            return false;
        }

        final ArrayList<String> commandList = new ArrayList<>();

        if (FLAGS != 0) {
            String timestamp = null;
            if (hasFlag(FLAG_MAINTAIN_KERNEL)) {
                timestamp = new SimpleDateFormat("yy_MM_dd_hh_mm_ss", Locale.US).format(new Date());
                commandList.add("backup B purity_ota_" + timestamp);
            }
            for (String file : FILES) {
                commandList.add("install " + file);
            }
            if (hasFlag(FLAG_WIPE_CACHE)) {
                commandList.add("wipe cache");
            }
            if (hasFlag(FLAG_WIPE_DALVIK)) {
                commandList.add("wipe dalvik");
            }
            if (hasFlag(FLAG_WIPE_DATA)) {
                commandList.add("wipe data");
            }
            if (hasFlag(FLAG_MAINTAIN_KERNEL)) {
                commandList.add("restore /sdcard/TWRP/BACKUPS/" + Build.SERIAL + "/purity_ota_" + timestamp);
                commandList.add("cmd rm -r /sdcard/TWRP/BACKUPS/" + Build.SERIAL + "/purity_ota_" + timestamp);
            }
        } else {
            for (String file : FILES) {
                commandList.add("install " + file);
            }
        }

        final SUShell suShell = SUShell.getInstance();
        if (!SUShell.RUNNING) {
            new AsyncTask<Void, Boolean, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    if (suShell.startShell()) {
                        String ors = "/cache/recovery/openrecoveryscript";
                        boolean firstRun = true;
                        for (String command : commandList) {
                            suShell.run("echo " + command + (firstRun ? " > " : " >> ") + ors);
                            firstRun = false;
                        }
                        suShell.run("reboot recovery");
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean hasRoot) {
                    if (!hasRoot) {
                        Toast.makeText(context, R.string.prompt_rootFail, Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();
        }

        return true;
    }

    private static boolean hasFlag(int flag) {
        return (FLAGS & flag) == flag;
    }

}
