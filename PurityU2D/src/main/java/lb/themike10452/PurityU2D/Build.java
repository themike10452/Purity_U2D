package lb.themike10452.purityu2d;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class Build {

    public Set<String> getBASE() {
        String[] all = BASE.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public Set<String> getAPI() {
        String[] all = API.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public String getVERSION() {
        return VERSION;
    }

    public String getZIPNAME() {
        return ZIPNAME;
    }

    public String getHTTPLINK() {
        return HTTPLINK;
    }

    public boolean isTestBuild() {
        return ISTESTBUILD;
    }

    public String getMD5() {
        return MD5;
    }

    private String BASE, API, VERSION, ZIPNAME, HTTPLINK, MD5;
    private boolean ISTESTBUILD;

    public Build(String parameters) {
        if (parameters == null)
            return;

        Scanner s = new Scanner(parameters);

        try {
            String line;
            while (s.hasNextLine()) {
                line = s.nextLine().trim();
                if (line.length() > 0 && line.startsWith("_")) {
                    if (line.contains(Keys.KEY_ROM_BASE))
                        BASE = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_API))
                        API = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_VERSION))
                        VERSION = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_ZIPNAME))
                        ZIPNAME = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_HTTPLINK))
                        HTTPLINK = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_MD5))
                        MD5 = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_ROM_test))
                        try {
                            ISTESTBUILD = Boolean.parseBoolean(line.split(":=")[1].trim());
                        } catch (Exception ignored) {

                        }
                }
            }
        } finally {
            s.close();
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", VERSION, ISTESTBUILD, API);
    }

}
