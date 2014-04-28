package com.themike10452.purityu2d;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by Mike on 4/22/2014.
 */
public class lib {
    //for personal testing
    public static String test_host = "https://dl.dropboxusercontent.com/s/gzigrceq6jtxri7/purity_host?dl=1";
    public static String host = "https://dl.dropboxusercontent.com/s/h5o6c6d8qd5wynj/host.txt?dl=1";
    public static String onPostInstallFolder = "PurityU2D/onPostUpdate";
    public static String buildProp = "/system/build.prop";
    public static String versionPropTag = "ro.build.version.incremental=";
    public static String buildDatePropTag = "ro.build.date=";
    public static String devicePropTag = "ro.product.device=";
    public static String modelPropTag = "ro.product.model=";

    public static String emergency_host = "https://dl.dropboxusercontent.com/s/w9lp0xxfyxmvsiv/purity_emergency.txt?dl=1";

    public static String FLAG_ACTION_REBOOT = "reboot_recovery";

    public static String ACTION_CLEAR_CACHE = "_CK_";
    public static String ACTION_MAINTAIN_KERNEL = "_MK_";
    public static String ACTION_MAINTAIN_INITD = "_MI_";

    public static String shellOut(String command, String delim, int tok) {
        String string;
        try {
            string = Shell.SH.run(String.format(command)).get(0).split(delim)[tok];
        } catch (IndexOutOfBoundsException e) {
            string = "n/a";
        }
        return string;
    }
}

