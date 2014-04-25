package com.themike10452.purityu2d;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;

public class DownloadActivity extends Activity {
    private Activity thisActivity;
    private String[] INF;
    public static DownloadActivity THIS;
    private final View.OnClickListener download = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (thisActivity != null) {
                final Intent intent = new Intent(thisActivity, DownloadService.class);
                intent.putExtra("0x0", INF[1]);
                intent.putExtra("0x1", INF[2]);
                intent.putExtra("0x2", ((CheckBox) findViewById(R.id.kmCB)).isChecked());
                intent.putExtra("0x3", ((CheckBox) findViewById(R.id.cdCB)).isChecked());
                (findViewById(R.id.button1Layout)).setVisibility(View.GONE);
                (findViewById(R.id.options)).setVisibility(View.GONE);
                (findViewById(R.id.message_downloading)).setVisibility(View.VISIBLE);
                startService(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        if (THIS == null)
            THIS = this;

        thisActivity = this;
        INF = getIntent().getExtras().getString("0x0").split(">>");

        Button btnDownload = (Button) findViewById(R.id.btnDownload);
        TextView versionDisplay = (TextView) findViewById(R.id.newVersion);
        versionDisplay.setText(INF[0]);
        btnDownload.setText(INF[1]);
        btnDownload.setOnClickListener(download);
        ((TextView) findViewById(R.id.line01)).setText(
                String.format(
                        getString(R.string.info_line1),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                + File.separator + INF[1].trim()
                )
        );
        ((TextView) findViewById(R.id.line02)).setText(
                String.format(
                        getString(R.string.info_line2),
                        Environment.getExternalStorageDirectory() + File.separator + lib.onPostInstallFolder
                )
        );
        ((TextView) findViewById(R.id.line03)).setText(getString(R.string.info_line3));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void updateMessage(int resID) {
        ((TextView) findViewById(R.id.message_downloading)).setText(getString(resID));
    }
}
