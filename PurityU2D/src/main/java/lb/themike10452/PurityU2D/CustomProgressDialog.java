package lb.themike10452.PurityU2D;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.themike10452.purityu2d.R;

/**
 * Created by Mike on 9/22/2014.
 */
public class CustomProgressDialog extends Dialog {

    public static String UNIT = " MB";
    private ProgressBar progressBar;
    private int MAX;
    private TextView FILENAME, FILESIZE, DOWNLOADED, PERCENTAGE, SPEED;

    public CustomProgressDialog(Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.progress_dialog_layout);
        MAX = 100;
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        FILENAME = (TextView) findViewById(R.id.textView_filename);
        FILESIZE = (TextView) findViewById(R.id.textView_filesize);
        DOWNLOADED = (TextView) findViewById(R.id.textView_downloaded);
        PERCENTAGE = (TextView) findViewById(R.id.percentage);
        SPEED = (TextView) findViewById(R.id.speed);

        progressBar.setMax(MAX);
    }

    public void update(String filename, String downloaded, String filesize) {
        FILENAME.setText(filename);
        FILESIZE.setText(filesize + UNIT);
        DOWNLOADED.setText(downloaded + UNIT);
    }

    public void update(double speed) {
        if (speed >= 0) {
            double kbSpeed = speed / 1000;
            SPEED.setText(kbSpeed < 1000 ? String.format("%.0f Kb/s", kbSpeed) : String.format("%.2f Mb/s", kbSpeed / 1000));
        }
    }

    public void setProgress(int percentage) {
        if (percentage < 0)
            return;
        progressBar.setProgress(percentage);
        PERCENTAGE.setText(percentage + "%");
    }

    public void setMax(int max) {
        progressBar.setMax(MAX = max);
    }

    public void setIndeterminate(boolean b) {
        progressBar.setIndeterminate(b);
    }
}
