package lb.themike10452.purityu2d;

import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 3/6/2015.
 */
public class SUShell {

    protected static final String VALID_EOF = "--@M^I*K#E_--";
    protected static final String ERR_EOF = "--!@M^I*K#E_--";
    protected static boolean RUNNING;
    private static SUShell instance;
    private Handler mHandler;
    private ArrayList<String> STDIN;
    private Process process;
    private StreamReader streamHandler;
    private Thread readerThread;

    public static SUShell getInstance() {
        return instance != null ? instance : new SUShell();
    }

    private SUShell() {
        instance = this;
        mHandler = new Handler();
        RUNNING = false;
        STDIN = new ArrayList<>();
    }

    public boolean startShell() {
        if (!RUNNING) {
            try {
                process = new ProcessBuilder("su", "-c", "/system/bin/sh")
                        .redirectErrorStream(false)
                        .start();
                streamHandler = new StreamReader(process, STDIN);
                readerThread = new Thread(streamHandler);
                readerThread.start();

                while (STDIN.size() == 0) {
                    if (run("id") == null)
                        return false;
                }

                return RUNNING = STDIN.size() > 0
                        && STDIN.get(0).contains("uid=0");

            } catch (IOException e) {
                e.printStackTrace();
                return RUNNING = false;
            }
        } else {
            return RUNNING = false;
        }
    }

    public void stopShell() {
        RUNNING = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                readerThread.interrupt();
            }
        }, 100);
    }

    public synchronized List<String> run(String cmd) {
        clear();
        try {
            streamHandler.addCommand(cmd);
            while (streamHandler.hasUnfinishedJobs()) {
                //wait
            }
            if (STDIN.size() == 0 || STDIN.contains(ERR_EOF)) {
                return null;
            } else {
                return (List<String>) STDIN.clone();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void clear() {
        STDIN.clear();
    }

    public class StreamReader implements Runnable {

        private List<String> STDIN;
        private Process process;
        private boolean hasUnfinishedJobs;

        public StreamReader(Process process, List<String> STDIN) {
            this.process = process;
            this.STDIN = STDIN;
        }

        @Override
        public void run() {
            RUNNING = true;
            setHasUnfinishedJobs(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                while (RUNNING) {
                    String line;
                    while (RUNNING && (line = reader.readLine()) != null) {
                        boolean reachedEOF = line.equals(VALID_EOF) || line.equals(ERR_EOF);

                        if (!line.equals(VALID_EOF))
                            addToStdIn(line);

                        setHasUnfinishedJobs(!reachedEOF);
                    }
                    setHasUnfinishedJobs(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized void addCommand(String command) throws IOException {
            setHasUnfinishedJobs(true);
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
            writer.write(command.concat(" && echo ").concat(VALID_EOF).concat(" || echo ").concat(ERR_EOF).concat("\n"));
            writer.flush();
        }

        private synchronized void addToStdIn(String line) {
            STDIN.add(line);
        }

        public synchronized boolean hasUnfinishedJobs() {
            return hasUnfinishedJobs;
        }

        private synchronized void setHasUnfinishedJobs(boolean hasJobs) {
            hasUnfinishedJobs = hasJobs;
        }
    }
}
