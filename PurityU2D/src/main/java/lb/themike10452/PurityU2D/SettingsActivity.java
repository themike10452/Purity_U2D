package lb.themike10452.purityu2d;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.themike10452.purityu2d.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import lb.themike10452.purityu2d.filebrowser.FileBrowser;
import lb.themike10452.purityu2d.services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/22/2014.
 */
public class SettingsActivity extends Activity {

    private Activity activity;
    private String DEVICE_PART;
    private boolean screenUpdating;

    private TextView AC_H, AC_M;
    private TextWatcher intervalChanger = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            findViewById(R.id.editText_autocheck_h).post(new Runnable() {
                @Override
                public void run() {
                    String hours = AC_H.getText().toString(), minutes = AC_M.getText().toString();
                    if ((Tools.isAllDigits(hours) && Integer.parseInt(hours) == 0) && (Tools.isAllDigits(minutes) && Integer.parseInt(minutes) == 0))
                        return;
                    MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, (hours.length() > 0 ? hours : "0") + ":" + (minutes.length() > 0 ? minutes : "0")).apply();
                }
            });
        }
    };
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            updateScreen();
            if (s.equals(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL) && MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true)) {
                Intent intent = new Intent(SettingsActivity.this, BackgroundAutoCheckService.class);
                stopService(intent);
                startService(intent);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        activity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        overridePendingTransition(R.anim.slide_in_rtl, R.anim.slide_out_rtl);

        ((TextView) findViewById(R.id.topInfo)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Light.ttf"));

        ((Switch) findViewById(R.id.switch_useProxy)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USEPROXY, b).apply();
                findViewById(R.id.btn_editProxy).setEnabled(b);
                if (!screenUpdating)
                    Toast.makeText(getApplicationContext(), R.string.toast_restartApp, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_editProxy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog d = new Dialog(SettingsActivity.this);
                d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                d.setContentView(R.layout.dialog_proxy);
                d.setCancelable(true);
                d.show();

                final EditText IP = (EditText) d.findViewById(R.id.ip), PORT = (EditText) d.findViewById(R.id.port);
                String currentHost = MainActivity.preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY);
                IP.setText(currentHost.substring(0, currentHost.indexOf(":")));
                PORT.setText(currentHost.substring(currentHost.indexOf(":") + 1));

                SpannableString ss0 = new SpannableString(getString(R.string.proxy_list));
                ss0.setSpan(new UnderlineSpan(), 0, ss0.length(), 0);
                ((TextView) d.findViewById(R.id.pl)).setText(ss0);

                SpannableString ss1 = new SpannableString(getString(R.string.defaultt));
                ss1.setSpan(new UnderlineSpan(), 0, ss1.length(), 0);
                ((TextView) d.findViewById(R.id.dp)).setText(ss1);

                d.findViewById(R.id.dp).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String host = Keys.DEFAULT_PROXY;
                        IP.setText(host.substring(0, host.indexOf(":")));
                        PORT.setText(host.substring(host.indexOf(":") + 1));
                    }
                });

                d.findViewById(R.id.pl).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Keys.PROXY_LIST));
                        startActivity(intent);
                    }
                });

                d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String ip = IP.getText().toString(), port = PORT.getText().toString();
                        if (Tools.validateIP(ip) && port.length() > 0) {
                            MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_PROXYHOST, ip + ":" + port).apply();
                            Toast.makeText(getApplicationContext(), R.string.toast_restartApp, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), R.string.msg_invalidProxy, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        ((Switch) findViewById(R.id.switch_useAndDM)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USEANDM, b).apply();
            }
        });

        findViewById(R.id.btn_dlLoc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SettingsActivity.this, FileBrowser.class);
                i.putExtra(FileBrowser.EXTRA_START_DIRECTORY, MainActivity.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
                i.putExtra(FileBrowser.EXTRA_SHOW_FOLDERS_ONLY, true);
                startActivity(i);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(FileBrowser.ACTION_DIRECTORY_SELECTED)) {
                            String newF = intent.getStringExtra("folder");
                            if (newF != null) {
                                MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, newF).apply();
                                ((TextView) findViewById(R.id.textView_dlLoc)).setText(newF);
                            }
                        }
                        unregisterReceiver(this);
                    }
                };
                IntentFilter filter = new IntentFilter(FileBrowser.ACTION_DIRECTORY_SELECTED);
                filter.addAction(FileBrowser.ACTION_CANCELED);
                registerReceiver(receiver, filter);
            }
        });

        findViewById(R.id.btn_upSrc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentSource = MainActivity.preferences.getString(Keys.KEY_SETTINGS_SOURCE, null);

                if (Keys.DEFAULT_SOURCE.equalsIgnoreCase(currentSource))
                    currentSource = "";

                Object[] obj = showDialog(getString(R.string.prompt_blankDefault), "http://", currentSource);
                final Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newSource = editText.getText().toString().trim();
                        if (newSource.length() == 0) {
                            newSource = Keys.DEFAULT_SOURCE;
                        }
                        MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_SOURCE, newSource).apply();
                    }
                });
            }
        });

        ((Switch) findViewById(R.id.switch_useStaticFilename)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                findViewById(R.id.btn_staticFilename).setEnabled(b);
                findViewById(R.id.btn_staticFilename).setClickable(b);

                if (!b) {
                    MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false).apply();
                    return;
                }

                if (MainActivity.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null) != null) {
                    MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, true).apply();
                    return;
                }

                Object[] obj = showDialog(getString(R.string.prompt_zipExtension), "filename.zip", MainActivity.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null));
                final Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newName = editText.getText().toString().trim();
                        if (newName.length() < 1 || newName.equalsIgnoreCase(".zip")) {
                            Toast.makeText(getApplicationContext(), R.string.msg_invalidFilename, Toast.LENGTH_LONG).show();
                            MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false).apply();
                            updateScreen();
                            return;
                        }
                        MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, true).apply();
                        MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, newName).apply();
                    }
                });
            }
        });

        ((Switch) findViewById(R.id.switch_receiveBeta)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, b).apply();
            }
        });

        findViewById(R.id.btn_staticFilename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object[] obj = showDialog(getString(R.string.prompt_zipExtension), "filename.zip", MainActivity.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, null));
                final Dialog dialog = (Dialog) obj[0];
                final EditText editText = (EditText) obj[1];
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newName = editText.getText().toString().trim();
                        if (newName.length() < 1 || newName.equalsIgnoreCase(".zip")) {
                            Toast.makeText(getApplicationContext(), R.string.msg_invalidFilename, Toast.LENGTH_LONG).show();
                            return;
                        } else
                            MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, editText.getText().toString().trim()).apply();
                    }
                });
            }
        });

        findViewById(R.id.btn_romBase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {

                    ProgressDialog d;
                    String versions;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        d = new ProgressDialog(SettingsActivity.this);
                        d.setMessage(getString(R.string.msg_pleaseWait));
                        d.setIndeterminate(true);
                        d.setCancelable(false);
                        d.show();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {

                            if (!getDevicePart())
                                throw new Exception(getString(R.string.msg_device_not_supported));

                            Scanner s = new Scanner(DEVICE_PART);
                            while (s.hasNextLine()) {
                                String line = s.nextLine();
                                if (line.startsWith("#define") && line.contains(Keys.KEY_DEFINE_BB)) {
                                    versions = line.split("=")[1];
                                    break;
                                }
                            }

                            s.close();

                        } catch (final Exception e) {
                            SettingsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        d.dismiss();

                        if (versions == null)
                            return;

                        final String[] choices = versions.split(",");
                        for (int i = 0; i < choices.length; i++) {
                            choices[i] = choices[i].trim();
                        }
                        Dialog d = new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(R.string.prompt_romBase)
                                .setSingleChoiceItems(choices, Tools.findIndex(choices, MainActivity.preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "null")), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_ROMBASE, choices[i]).apply();
                                    }
                                })
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_ok, null)
                                .show();
                        Tools.userDialog = d;
                    }
                }.execute();
            }
        });

        findViewById(R.id.btn_romApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {

                    ProgressDialog d;
                    String versions;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        d = new ProgressDialog(SettingsActivity.this);
                        d.setMessage(getString(R.string.msg_pleaseWait));
                        d.setIndeterminate(true);
                        d.setCancelable(false);
                        d.show();
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {

                            if (!getDevicePart())
                                throw new Exception(getString(R.string.msg_device_not_supported));

                            Scanner s = new Scanner(DEVICE_PART);
                            while (s.hasNextLine()) {
                                String line = s.nextLine();
                                if (line.startsWith("#define") && line.contains(Keys.KEY_DEFINE_AV)) {
                                    versions = line.split("=")[1];
                                    break;
                                }
                            }

                            s.close();

                        } catch (final Exception e) {
                            SettingsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        d.dismiss();

                        if (versions == null)
                            return;

                        final String[] choices = versions.split(",");
                        String[] displayChoices = new String[choices.length];
                        for (int i = 0; i < displayChoices.length; i++) {
                            displayChoices[i] = choices[i].replace("*latest*", getString(R.string.receive_latest_version));
                        }
                        for (int i = 0; i < choices.length; i++) {
                            choices[i] = choices[i].trim();
                        }
                        Dialog d = new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(R.string.prompt_android_version)
                                .setSingleChoiceItems(displayChoices, Tools.findIndex(choices, MainActivity.preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "null")), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        MainActivity.preferences.edit().putString(Keys.KEY_SETTINGS_ROMAPI, choices[i]).apply();
                                    }
                                })
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_ok, null)
                                .show();
                        Tools.userDialog = d;
                    }
                }.execute();
            }
        });

        ((Switch) findViewById(R.id.switch_bkg_check)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
            }
        });

        (AC_H = (TextView) findViewById(R.id.editText_autocheck_h)).addTextChangedListener(intervalChanger);
        (AC_M = (TextView) findViewById(R.id.editText_autocheck_m)).addTextChangedListener(intervalChanger);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                View child = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blank, null);
                final NumberPicker picker = new NumberPicker(SettingsActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                picker.setMaxValue(view == AC_H ? 168 : 59);
                picker.setMinValue(0);
                picker.setValue(Integer.parseInt(MainActivity.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[(view == AC_H ? 0 : 1)]));
                ((LinearLayout) child).addView(picker, params);
                ((LinearLayout) child).setGravity(Gravity.CENTER_HORIZONTAL);
                child.setPadding(30, 0, 30, 0);
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.settings_textView_backgroundCheckInterval);
                builder.setView(child);
                builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateTextView((TextView) view, picker.getValue() + "");
                    }
                });
                builder.setNegativeButton(R.string.btn_cancel, null);
                builder.show();
            }
        };

        AC_H.setOnClickListener(listener);
        AC_M.setOnClickListener(listener);

        AC_H.post(new Runnable() {
            @Override
            public void run() {
                AC_H.setLayoutParams(new LinearLayout.LayoutParams(AC_H.getMeasuredWidth(), AC_H.getMeasuredWidth()));
                AC_M.setLayoutParams(new LinearLayout.LayoutParams(AC_M.getMeasuredWidth(), AC_M.getMeasuredWidth()));
            }
        });

        ((Switch) findViewById(R.id.switch_bkg_check)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linear_bkg_check_edit);
                for (int i = 0; i < (linearLayout).getChildCount(); i++) {
                    linearLayout.getChildAt(i).setEnabled(b);
                }
                if (!b && BackgroundAutoCheckService.running)
                    stopService(new Intent(SettingsActivity.this, BackgroundAutoCheckService.class));
                else if (b && !BackgroundAutoCheckService.running)
                    startService(new Intent(SettingsActivity.this, BackgroundAutoCheckService.class));
            }
        });

        updateScreen();

        MainActivity.preferences.registerOnSharedPreferenceChangeListener(prefListener);

    }

    void updateTextView(TextView v, String s) {
        v.setText(s);
    }

    private void updateScreen() {
        screenUpdating = true;
        ((Switch) findViewById(R.id.switch_bkg_check)).setChecked(MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true));
        ((TextView) findViewById(R.id.textView_dlLoc)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
        ((TextView) findViewById(R.id.proxyHost)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY));
        ((Switch) findViewById(R.id.switch_useProxy)).setChecked(MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false));
        ((Switch) findViewById(R.id.switch_useAndDM)).setChecked(MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false));
        ((Switch) findViewById(R.id.switch_receiveBeta)).setChecked(MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, true));

        AC_H.setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[0]);
        AC_M.setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[1]);

        if (MainActivity.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE).equalsIgnoreCase(Keys.DEFAULT_SOURCE))
            ((TextView) findViewById(R.id.textView_upSrc)).setText(getString(R.string.defaultt));
        else
            ((TextView) findViewById(R.id.textView_upSrc)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE));

        ((Switch) findViewById(R.id.switch_useStaticFilename)).setChecked(MainActivity.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false));

        ((TextView) findViewById(R.id.textView_staticFilename)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, getString(R.string.undefined)));

        ((TextView) findViewById(R.id.textView_romBase)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "n/a").toUpperCase());

        ((TextView) findViewById(R.id.textView_romApi)).setText(MainActivity.preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "n/a").toUpperCase());
        screenUpdating = false;
    }

    private Object[] showDialog(String msg, String hint, String editTextContent) {
        View child = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blank, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        child.setLayoutParams(params);
        child.setPadding(30, 30, 30, 30);
        final EditText editText = new EditText(SettingsActivity.this);
        editText.setSingleLine();
        editText.setHorizontallyScrolling(true);
        editText.setHint(hint);
        if (editTextContent != null) {
            editText.setText(editTextContent);
        }
        TextView textView = new TextView(SettingsActivity.this);
        textView.setText(msg);
        ((LinearLayout) child).addView(textView, params);
        ((LinearLayout) child).addView(editText, params);
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(child);
        dialog.show();
        return new Object[]{dialog, editText};
    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s = null;
        HttpURLConnection connection = null;
        DEVICE_PART = "";
        boolean DEVICE_SUPPORTED = false;
        try {
            try {
                connection = (HttpURLConnection) new URL(MainActivity.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE)).openConnection();
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
            String pattern = String.format("<%s>", Build.DEVICE);
            while (s.hasNextLine()) {
                if (s.nextLine().equalsIgnoreCase(pattern)) {
                    DEVICE_SUPPORTED = true;
                    break;
                }
            }
            if (DEVICE_SUPPORTED) {
                String line;
                while (s.hasNextLine()) {
                    line = s.nextLine().trim();
                    if (line.equalsIgnoreCase(String.format("</%s>", Build.DEVICE)))
                        break;

                    DEVICE_PART += line + "\n";
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

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }*/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
    }
}
