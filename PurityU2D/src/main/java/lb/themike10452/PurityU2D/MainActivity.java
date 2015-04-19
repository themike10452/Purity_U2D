package lb.themike10452.purityu2d;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.themike10452.purityu2d.R;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import lb.themike10452.purityu2d.services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/19/2014.
 */
public class MainActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {

    public static SharedPreferences preferences;
    public static boolean isVisible;
    private String DEVICE = Build.DEVICE;
    private String DEVICE_PART, CHANGELOG;
    private Tools tools;
    private TextView tag;
    private LinearLayout main;
    private SwipeRefreshLayout refreshLayout;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AsyncTask<Void, Void, Boolean>() {
                        Card card;
                        boolean DEVICE_SUPPORTED;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();

                            refreshLayout.setRefreshing(true);

                            View v1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.rom_info, null);
                            ((TextView) v1.findViewById(R.id.text)).setText(Tools.getBuildVersion());

                            tag = new TextView(MainActivity.this);
                            tag.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Small);
                            tag.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf"), Typeface.BOLD);
                            tag.setTextSize(10f);

                            Card card1 = new Card(MainActivity.this, getString(R.string.card_title_installedROM), tag, false, v1);
                            card1.getPARENT().setAnimation(getIntroSet(1000, 0));

                            main.addView(card1.getPARENT());
                            card1.getPARENT().animate();
                        }

                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            try {
                                DEVICE_SUPPORTED = true;
                                boolean b = getDevicePart();
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        initSettings();
                                    }
                                });
                                return b;
                            } catch (DeviceNotSupportedException e) {
                                DEVICE_SUPPORTED = false;
                                return true;
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            super.onPostExecute(success);

                            tag.setText(preferences.getString(Keys.KEY_SETTINGS_ROMBASE, getString(R.string.undefined)).toUpperCase());

                            refreshLayout.setRefreshing(false);

                            if (!success) {
                                displayOnScreenMessage(main, R.string.msg_failed_try_again);
                                return;
                            }

                            if (!DEVICE_SUPPORTED) {
                                displayOnScreenMessage(main, R.string.msg_device_not_supported);
                                return;
                            }

                            try {
                                if (Tools.getMinVer(DEVICE_PART) != null && Tools.getMinVer(DEVICE_PART) > Double.parseDouble(Tools.retainDigits(getPackageManager().getPackageInfo(getPackageName(), 0).versionName))) {
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(R.string.msg_updateRequired)
                                            .setTitle(R.string.msgTitle_versionObs)
                                            .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    try {
                                                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.android.vending");
                                                        ComponentName comp = new ComponentName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity"); // package name and activity
                                                        intent.setComponent(comp);
                                                        intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                                                        startActivity(intent);
                                                    } catch (Exception ignored) {

                                                    }
                                                }
                                            })
                                            .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            MainActivity.this.finish();
                                                        }
                                                    }
                                            )
                                            .show();
                                    return;
                                }
                            } catch (Exception ignored) {

                            }

                            Tools.sniffBuilds(DEVICE_PART);

                            if (BuildManager.getInstance().getProperBuild(getApplicationContext()) == null) {
                                if (!BuildManager.baseMatchedOnce) {
                                    displayOnScreenMessage(main, getString(R.string.msg_noROMForYou, preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").toUpperCase(), DEVICE.toUpperCase()));
                                    return;
                                } else if (!BuildManager.apiMatchedOnce) {
                                    displayOnScreenMessage(main, getString(R.string.msg_noROMForYou, preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").toUpperCase(), DEVICE.toUpperCase()));
                                    return;
                                } else {
                                    displayOnScreenMessage(main, getString(R.string.msg_noROMForYou, preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").toUpperCase() + " " + preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").toUpperCase(), DEVICE.toUpperCase()));
                                    return;
                                }
                            }

                            String latestVersion = BuildManager.getInstance().getProperBuild(getApplicationContext()).getVERSION();
                            if (latestVersion != null && Tools.INSTALLED_ROM_VERSION != null) {
                                try {
                                    int lv = Integer.parseInt(latestVersion.trim());
                                    int cv = Integer.parseInt(Tools.INSTALLED_ROM_VERSION);
                                    System.out.println(lv + " " + cv);
                                    if (cv >= lv) {
                                        if (preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").equalsIgnoreCase("*latest*"))
                                            displayOnScreenMessage(main, R.string.msg_up_to_date);
                                        else
                                            displayOnScreenMessage(main, getString(R.string.msg_up_to_date) + " (" + preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "n/a") + ")");
                                        return;
                                    }
                                } catch (Exception ignored) {
                                }
                            } else if (Tools.INSTALLED_ROM_VERSION.equalsIgnoreCase(latestVersion)) {
                                if (preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").equalsIgnoreCase("*latest*"))
                                    displayOnScreenMessage(main, R.string.msg_up_to_date);
                                else
                                    displayOnScreenMessage(main, getString(R.string.msg_up_to_date) + " (" + preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "n/a") + ")");
                                return;
                            }

                            View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.new_rom, null);
                            String ver = BuildManager.getInstance().getProperBuild(getApplicationContext()).getVERSION();

                            ((TextView) v.findViewById(R.id.text)).setText(ver);

                            ((Button) v.findViewById(R.id.btn_changelog)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf"), Typeface.BOLD);
                            ((Button) v.findViewById(R.id.btn_getLatestVersion)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf"), Typeface.BOLD);

                            final View.OnClickListener chlg = new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    showChangelog();
                                }
                            };

                            v.findViewById(R.id.btn_changelog).setOnClickListener(chlg);

                            v.findViewById(R.id.btn_getLatestVersion).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(final View view) {
                                    getIt();
                                }
                            });

                            if (!preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").equalsIgnoreCase("*latest*"))
                                card = new Card(getApplicationContext(), getString(R.string.card_title_latestVersion) + " (" + preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "") + ")", false, v);
                            else
                                card = new Card(getApplicationContext(), getString(R.string.card_title_latestVersion), false, v);
                            main.addView(card.getPARENT());
                            card.getPARENT().startAnimation(getIntroSet(1000, 200));

                        }
                    }.execute();
                }
            });
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true) && !BackgroundAutoCheckService.running) {
            startService(new Intent(this, BackgroundAutoCheckService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        this.tools = new Tools(this);
        preferences = getSharedPreferences(Keys.SharedPrefsKey, MODE_PRIVATE);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        refreshLayout.setColorSchemeResources(R.color.teal, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        refreshLayout.setOnRefreshListener(this);

        main = ((LinearLayout) findViewById(R.id.main));

        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                chuckNorris();
            }
        }, 10);

        ((TextView) findViewById(R.id.bottom_msg)).setText(getString(R.string.msg_troubleProxy, getString(R.string.activity_settings), getString(R.string.settings_btn_useProxy)));
        findViewById(R.id.bottom_bar).setVisibility(preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false) ? View.GONE : View.VISIBLE);
    }

    private void chuckNorris() {
        if (main.getChildCount() > 0) {
            int count = main.getChildCount();
            final View lastChild = main.getChildAt(count - 1);
            for (int i = 0; i < count; i++) {
                final View v = main.getChildAt(i);
                AnimationSet set = getOutroSet(500, (count - 1 - i) * 200);
                set.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                main.removeView(v);
                                if (v == lastChild) {
                                    mHandler.sendEmptyMessage(0);
                                }
                            }
                        });
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                v.startAnimation(set);
            }

        } else {
            mHandler.sendEmptyMessage(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_about: {
                showAboutDialog();
                return true;
            }
            case R.id.action_settings: {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
                return true;
            }
            case R.id.action_flashZip: {
                Intent i = new Intent(MainActivity.this, InstallationActivity.class);
                startActivity(i);
                return true;
            }
        }
        return false;
    }

    private void showChangelog() {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(CHANGELOG);
        textView.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Small);
        textView.setTextColor(getResources().getColor(R.color.card_text));
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.blank, null);
        view1.setPadding(15, 15, 15, 15);
        ((LinearLayout) view1).addView(textView, params);
        new AlertDialog.Builder(MainActivity.this)
                .setView(view1)
                .setTitle(R.string.dialog_title_changelog)
                .setCancelable(false)
                .setNeutralButton(R.string.btn_dismiss, null)
                .setPositiveButton(preferences.getBoolean("wrap_changelog", false) ? R.string.unwrap : R.string.wrap, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putBoolean("wrap_changelog", !preferences.getBoolean("wrap_changelog", false)).apply();
                        showChangelog();
                    }
                })
                .show();

        textView.setHorizontallyScrolling(!preferences.getBoolean("wrap_changelog", false));
        textView.setHorizontalScrollBarEnabled(textView.isHorizontalScrollBarEnabled());
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    private synchronized AnimationSet getIntroSet(int duration, int startOffset) {

        TranslateAnimation animation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_PARENT, -1,
                Animation.RELATIVE_TO_SELF, 0);

        final AnimationSet set = new AnimationSet(false);
        set.addAnimation(animation2);
        set.setDuration(duration);
        set.setStartOffset(startOffset);

        return set;
    }

    private synchronized AnimationSet getOutroSet(int duration, int startOffset) {
        AlphaAnimation animation1 = new AlphaAnimation(1, 0);

        TranslateAnimation animation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0,
                Animation.RELATIVE_TO_PARENT, 0,
                Animation.RELATIVE_TO_PARENT, 0,
                Animation.RELATIVE_TO_PARENT, 1);

        final AnimationSet set = new AnimationSet(false);
        set.addAnimation(animation1);
        set.addAnimation(animation2);
        set.setDuration(duration);
        set.setStartOffset(startOffset);

        return set;
    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s = null;
        HttpURLConnection connection = null;
        DEVICE_PART = "";
        CHANGELOG = "";
        boolean DEVICE_SUPPORTED = false;
        try {
            try {
                if (preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false)) {
                    final String proxyHost = preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY);
                    System.setProperty("http.proxySet", "true");
                    System.setProperty("http.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                    System.setProperty("http.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
                    System.setProperty("https.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                    System.setProperty("https.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Proxy: " + proxyHost, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    System.setProperty("http.proxySet", "false");
                }
                connection = (HttpURLConnection) new URL(preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE)).openConnection();
                s = new Scanner(connection.getInputStream());
            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                return false;
            }

            String firstLine = s.nextLine();
            if (firstLine != null && !firstLine.equals(Keys.VALIDITY_KEY)) {
                return false;
            }

            String pattern = String.format("<%s>", DEVICE);
            while (s.hasNextLine()) {
                if (s.nextLine().equalsIgnoreCase(pattern)) {
                    DEVICE_SUPPORTED = true;
                    break;
                }
            }
            if (DEVICE_SUPPORTED) {
                boolean multichangelog = false;
                String line;
                while (s.hasNextLine()) {
                    line = s.nextLine().trim();
                    if (line.equalsIgnoreCase(String.format("</%s>", DEVICE)))
                        break;

                    if (line.equalsIgnoreCase("<changelog>")) {
                        multichangelog = true;
                        DEVICE_PART += line + "\n";
                        while (s.hasNextLine() && !(line = s.nextLine()).equalsIgnoreCase("</changelog>")) {
                            CHANGELOG += line + "\n";
                            DEVICE_PART += line + "\n";
                        }
                    }

                    DEVICE_PART += line + "\n";
                }

                if (!multichangelog)
                    while (s.hasNextLine()) {
                        line = s.nextLine().trim();
                        if (line.equalsIgnoreCase("<changelog>")) {
                            while (s.hasNextLine() && !(line = s.nextLine()).equalsIgnoreCase("</changelog>")) {
                                CHANGELOG += line + "\n";
                            }
                            break;
                        }
                    }

                return true;
            } else {
                throw new DeviceNotSupportedException();
            }
        } finally {
            if (s != null)
                s.close();
            if (connection != null)
                connection.disconnect();
        }
    }

    private void displayOnScreenMessage(LinearLayout main, int msgId) {
        displayOnScreenMessage(main, getString(msgId));
    }

    private void displayOnScreenMessage(LinearLayout main, String msgStr) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(msgStr);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
        textView.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-LightItalic.ttf"), Typeface.BOLD_ITALIC);
        textView.setTextColor(getResources().getColor(R.color.card_text_light));
        main.addView(textView);
        textView.startAnimation(getIntroSet(1200, 0));
    }

    private void getIt() {
        final String link = BuildManager.getInstance().getProperBuild(getApplicationContext()).getHTTPLINK();
        if (link != null) {
            final boolean useADM = preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false);
            String destination = preferences
                    .getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator);

            BroadcastReceiver downloadHandler = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, final Intent intent) {
                    unregisterReceiver(this);
                    String action = intent.getAction();
                    handleDownloadEvent(action, intent.getStringExtra(Tools.EXTRA_MD5), intent.getBooleanExtra(Tools.EXTRA_MD5_MATCHED, false));
                }
            };

            try {
                unregisterReceiver(downloadHandler);
            } catch (Exception ignored) {
            }

            IntentFilter filter = new IntentFilter(Tools.EVENT_DOWNLOAD_COMPLETE);
            filter.addAction(Tools.EVENT_DOWNLOAD_CANCELED);
            filter.addAction(Tools.EVENT_DOWNLOADEDFILE_EXISTS);

            registerReceiver(downloadHandler, filter);

            lb.themike10452.purityu2d.Build properBuild = BuildManager.getInstance().getProperBuild(getApplicationContext());

            tools.downloadFile(
                    link,
                    destination,
                    properBuild.getZIPNAME(),
                    properBuild.getMD5(),
                    useADM,
                    this
            );
        }
    }

    private void proceedToInstallation() {
        Intent intent = new Intent(MainActivity.this, InstallationActivity.class);
        intent.putExtra(InstallationActivity.EXTRA_INITIAL_ZIP, tools.lastDownloadedFile.getAbsolutePath());
        startActivity(intent);
    }

    private void handleDownloadEvent(String action, String md5hash, boolean md5match) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        Dialog d = null;
        if (action != null)
            if (action.equals(Tools.EVENT_DOWNLOAD_COMPLETE)) {
                if (md5match) {
                    d = builder
                            .setTitle(R.string.dialog_title_readyToInstall)
                            .setCancelable(false)
                            .setMessage(getString(R.string.prompt_install1, getString(R.string.btn_install), getString(R.string.btn_dismiss)))
                            .setPositiveButton(R.string.btn_install, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    proceedToInstallation();
                                }
                            })
                            .setNegativeButton(R.string.btn_dismiss, null)
                            .show();
                    Tools.userDialog = d;
                } else {
                    d = builder.setTitle(R.string.dialog_title_md5mismatch)
                            .setCancelable(false)
                            .setMessage(getString(R.string.prompt_md5mismatch, BuildManager.getInstance().getProperBuild(getApplicationContext()).getMD5(), md5hash))
                            .setPositiveButton(R.string.btn_downloadAgain, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getIt();
                                }
                            })
                            .setNegativeButton(R.string.btn_installAnyway, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    proceedToInstallation();
                                }
                            })
                            .setNeutralButton(R.string.btn_dismiss, null)
                            .show();
                    Tools.userDialog = d;
                }
            } else if (action.equals(Tools.EVENT_DOWNLOADEDFILE_EXISTS)) {
                if (Tools.userDialog != null) {
                    Tools.userDialog.dismiss();
                }
                d = builder
                        .setTitle(R.string.dialog_title_readyToInstall)
                        .setCancelable(false)
                        .setMessage(getString(R.string.prompt_install2, getString(R.string.btn_install), getString(R.string.btn_dismiss)))
                        .setPositiveButton(R.string.btn_install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                proceedToInstallation();
                            }
                        })
                        .setNegativeButton(R.string.btn_dismiss, null)
                        .show();
                Tools.userDialog = d;
            } else if (action.equals(Tools.EVENT_DOWNLOAD_CANCELED)) {
                Toast.makeText(getApplicationContext(), R.string.msg_downloadCanceled, Toast.LENGTH_SHORT).show();
            }
        if (d != null && d.findViewById(android.R.id.message) != null) {
            ((TextView) d.findViewById(android.R.id.message)).setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Small);
            ((TextView) d.findViewById(android.R.id.message)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf"));
        }
    }

    private void showAboutDialog() {
        View contentView = getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        TextView devText = (TextView) contentView.findViewById(R.id.devinfo);
        devText.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf"));
        devText.setText(getString(R.string.dialog_content_about, "Michael Mouawad"));

        TextView verText = (TextView) contentView.findViewById(R.id.verinfo);
        try {
            verText.setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        SpannableString content = new SpannableString(getString(R.string.dialog_content_github));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);

        TextView gitText = (TextView) contentView.findViewById(R.id.gitinfo);
        gitText.setTextColor(getResources().getColor(R.color.teal));
        gitText.setText(content);
        gitText.setClickable(true);
        gitText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Keys.SOURCE_CODE));
                startActivity(intent);
            }
        });

        Dialog d = new Dialog(this, R.style.DialogStyle);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(contentView);
        d.show();
    }

    private void initSettings() {

        SharedPreferences.Editor editor = preferences.edit();

        if (preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, null) == null) {
            editor.putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator);
            Dialog d = new AlertDialog.Builder(this)
                    .setMessage(R.string.msg_twrp)
                    .setNeutralButton(R.string.btn_ok, null)
                    .setTitle("TWRP")
                    .show();
            ((TextView) d.findViewById(android.R.id.message)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf"));
        }

        if (preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, null) == null)
            editor.putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");
        else if (!Tools.isAllDigits(preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, null).replace(":", "")))
            editor.putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");

        editor.apply();

        final boolean a = preferences.getString(Keys.KEY_SETTINGS_ROMBASE, null) == null;
        final boolean b = preferences.getString(Keys.KEY_SETTINGS_ROMAPI, null) == null;

        if (b) {
            showRomApiChooserDialog(a);
        } else if (a) {
            showRomBaseChooserDialog();
        }

    }

    private void showRomApiChooserDialog(final boolean chooseRomBase) {

        ProgressDialog d;

        d = new ProgressDialog(MainActivity.this);
        d.setMessage(getString(R.string.msg_pleaseWait));
        d.setIndeterminate(true);
        d.setCancelable(false);
        d.show();
        Tools.userDialog = d;

        Scanner scanner = new Scanner(DEVICE_PART);
        String line, keyword = "#define";
        String[] versions = null;
        String[] displayVersions = null;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine().trim().toLowerCase();
            if (line.startsWith("#define")) {
                if (line.length() > keyword.length()) {
                    if (line.contains(Keys.KEY_DEFINE_AV)) {
                        versions = line.split("=")[1].split(",");
                        displayVersions = line.split("=")[1].split(",");
                        for (int i = 0; i < versions.length; i++) {
                            versions[i] = versions[i].trim();
                            displayVersions[i] = versions[i].replace("*latest*", getString(R.string.receive_latest_version));
                        }
                        break;
                    }
                }
            }
        }
        scanner.close();

        d.dismiss();

        if (versions != null) {
            if (versions.length == 1) {
                preferences.edit().putString(Keys.KEY_SETTINGS_ROMAPI, versions[0]).apply();
                if (chooseRomBase)
                    showRomBaseChooserDialog();
                return;
            }
            final String[] choices = versions;
            new AlertDialog.Builder(MainActivity.this)
                    .setSingleChoiceItems(displayVersions, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            preferences.edit().putString(Keys.KEY_SETTINGS_ROMAPI, choices[i]).apply();
                        }
                    })
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (chooseRomBase)
                                showRomBaseChooserDialog();
                            else
                                chuckNorris();
                        }
                    })
                    .setTitle(R.string.prompt_android_version)
                    .setCancelable(false).show();
        } else if (chooseRomBase)
            showRomBaseChooserDialog();
    }

    private void showRomBaseChooserDialog() {

        ProgressDialog d;

        d = new ProgressDialog(MainActivity.this);
        d.setMessage(getString(R.string.msg_pleaseWait));
        d.setIndeterminate(true);
        d.setCancelable(false);
        d.show();
        Tools.userDialog = d;

        Scanner scanner = new Scanner(DEVICE_PART);
        String line, keyword = "#define";
        String[] bases = null;
        while (scanner.hasNextLine()) {
            line = scanner.nextLine().trim().toLowerCase();
            if (line.startsWith("#define")) {
                if (line.length() > keyword.length()) {
                    if (line.contains(Keys.KEY_DEFINE_BB)) {
                        bases = line.split("=")[1].split(",");
                        for (int i = 0; i < bases.length; i++) {
                            bases[i] = bases[i].trim();
                        }
                        break;
                    }
                }
            }
        }
        scanner.close();

        d.dismiss();

        if (bases != null) {
            if (bases.length == 1) {
                preferences.edit().putString(Keys.KEY_SETTINGS_ROMBASE, bases[0]).apply();
                chuckNorris();
                return;
            }

            final String[] choices = bases;

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.prompt_romBase))
                    .setCancelable(false)
                    .setSingleChoiceItems(bases, Tools.findIndex(bases, preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "null")), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            preferences.edit().putString(Keys.KEY_SETTINGS_ROMBASE, choices[i]).apply();
                        }
                    })
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startService(new Intent(MainActivity.this, BackgroundAutoCheckService.class));
                            chuckNorris();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!Tools.isDownloading)
            super.onBackPressed();
        else
            Toast.makeText(this, R.string.msg_activeDownloads, Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    public void onRefresh() {
        chuckNorris();
    }
}
