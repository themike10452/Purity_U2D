package lb.themike10452.PurityU2D;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class ROMManager {

    public static boolean baseMatchedOnce = false, apiMatchedOnce = false;

    private static ROMManager instance = null;

    private static Set<ROM> kernelSet;

    public ROMManager() {
        kernelSet = new HashSet<ROM>(5, 0.8f);
        kernelSet.clear();
        baseMatchedOnce = false;
        instance = this;
    }

    public static ROMManager getFreshInstance() {
        if (instance != null) {
            baseMatchedOnce = false;
            kernelSet.clear();
            return instance;
        } else {
            return instance = new ROMManager();
        }
    }

    public static ROMManager getInstance() {
        return instance == null ? new ROMManager() : instance;
    }

    public boolean add(ROM k) {
        return kernelSet.add(k);
    }

    public ROM getProperKernel(Context c) {
        apiMatchedOnce = false;

        if (kernelSet.isEmpty()) {
            return null;
        }

        SharedPreferences preferences = c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);

        ROM res = null;

        for (ROM k : kernelSet) {
            try {
                boolean a = k.getBASE().contains(preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").trim().toUpperCase());
                boolean b = k.getAPI().contains(preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").trim().toUpperCase());
                if (a)
                    baseMatchedOnce = a;
                if (b)
                    apiMatchedOnce = b;
                if (a & b) {
                    if (k.isTestBuild() && !preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, true)) {
                        res = null;
                    } else {
                        res = k;
                        break;
                    }
                }
            } catch (NullPointerException ignored) {
            }
        }

        return res;
    }

}
