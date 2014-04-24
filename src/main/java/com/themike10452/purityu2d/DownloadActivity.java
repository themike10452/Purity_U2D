package com.themike10452.purityu2d;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DownloadActivity extends Activity {
    private Activity thisActivity;
    private String[] INF;
    public static DownloadActivity THIS;
    private View.OnClickListener download = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (thisActivity != null) {
                final Intent intent = new Intent(thisActivity, DownloadService.class);
                intent.putExtra("0x0", INF[1]);
                intent.putExtra("0x1", INF[2]);
                (findViewById(R.id.button1Layout)).setVisibility(View.GONE);
                (findViewById(R.id.message_downloading)).setVisibility(View.VISIBLE);
                startService(intent);
            }
        }
    };
    private Button btnDownload;
    private TextView versionDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        if (THIS == null)
            THIS = this;

        thisActivity = this;
        INF = getIntent().getExtras().getString("0x0").split(">>");

        btnDownload = (Button) findViewById(R.id.btnDownload);
        versionDisplay = (TextView) findViewById(R.id.newVersion);
        versionDisplay.setText(INF[0]);
        btnDownload.setText(INF[1]);
        btnDownload.setOnClickListener(download);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public DownloadActivity getInstance() {
        return THIS;
    }

    public void updateMessage(int resID) {
        ((TextView)findViewById(R.id.message_downloading)).setText(getString(resID));
    }
}
