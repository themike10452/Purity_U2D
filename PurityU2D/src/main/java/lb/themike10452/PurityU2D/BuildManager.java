package lb.themike10452.PurityU2D;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class BuildManager {

    public static boolean baseMatchedOnce = false, apiMatchedOnce = false;

    private static BuildManager instance = null;

    private static Set<Build> buildSet;

    public BuildManager() {
        buildSet = new HashSet<Build>(5, 0.8f);
        buildSet.clear();
        baseMatchedOnce = false;
        instance = this;
    }

    public static BuildManager getFreshInstance() {
        if (instance != null) {
            baseMatchedOnce = false;
            buildSet.clear();
            return instance;
        } else {
            return instance = new BuildManager();
        }
    }

    public static BuildManager getInstance() {
        return instance == null ? new BuildManager() : instance;
    }

    public boolean add(Build k) {
        return buildSet.add(k);
    }

    public Build getProperBuild(Context c) {
        apiMatchedOnce = false;

        if (buildSet.isEmpty()) {
            return null;
        }

        SharedPreferences preferences = c.getSharedPreferences(Keys.SharedPrefsKey, Context.MODE_MULTI_PROCESS);

        Build res = null;

        for (Build k : buildSet) {
            try {
                boolean a = k.getBASE().contains(preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").trim().toUpperCase());
                boolean b = k.getAPI().contains(preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").trim().toUpperCase());
                if (a)
                    baseMatchedOnce = true;
                if (b)
                    apiMatchedOnce = true;
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
