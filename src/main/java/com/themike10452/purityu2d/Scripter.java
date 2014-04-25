package com.themike10452.purityu2d;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

import eu.chainfire.libsuperuser.Shell;

public class Scripter extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... args) {

        String flags = args[1];

        String serialno = (new File(Environment.getExternalStorageDirectory() + "/TWRP/BACKUPS/")).listFiles()[0].getName();

        ArrayList<String> list = new ArrayList<String>();

        if (flags.contains(lib.ACTION_MAINTAIN_KERNEL)) {
            list.add("backup B purity_ota");
        }
        if (flags.contains(lib.ACTION_CLEAR_CACHE)) {
            list.add("wipe cache");
            list.add("wipe dalvik");
        }
        if (flags.contains(lib.ACTION_MAINTAIN_INITD)) {
            //TODO
        }

        list.add("install " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + args[0].trim());

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + lib.onPostInstallFolder);
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.toString().contains(".zip"))
                    list.add("install " + file.toString());
            }
        }

        if (flags.contains(lib.ACTION_MAINTAIN_KERNEL)) {
            list.add("restore /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");
            list.add("cmd rm -r /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");
        }


        Shell.SU.run("rm /cache/recovery/openrecoveryscript");
        Shell.SH.run("rm -r /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");

        for (String line : list)
            Shell.SU.run("echo " + line + " >> /cache/recovery/openrecoveryscript");

        return null;
    }
}
