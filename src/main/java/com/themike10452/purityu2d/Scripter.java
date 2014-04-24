package com.themike10452.purityu2d;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

import eu.chainfire.libsuperuser.Shell;

public class Scripter extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... args) {

        String f = args[1];

        String serialno = (new File(Environment.getExternalStorageDirectory() + "/TWRP/BACKUPS/")).listFiles()[0].getName();

        ArrayList<String> list = new ArrayList();

        if (f.charAt(0) == '1') {
            list.add("backup B purity_ota");
        }
        if (f.charAt(1) == '1') {
        }
        if (f.charAt(2) == '1') {
            list.add("wipe cache");
            list.add("wipe dalvik");
        }
        list.add("install " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + args[0].trim());

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + lib.onPostInstallFolder);
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.toString().contains(".zip"))
                    list.add("install " + file.toString());
            }
        }

        if (f.charAt(0) == '1')
            list.add("restore /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");

        Shell.SU.run("rm /cache/recovery/openrecoveryscript");
        Shell.SH.run("rm -r /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");
        //Shell.SH.run("rmdir /sdcard/TWRP/BACKUPS/" + serialno + "/purity_ota");

        for (String line : list)
            Shell.SU.run("echo " + line + " >> /cache/recovery/openrecoveryscript");

        return null;
    }
}
