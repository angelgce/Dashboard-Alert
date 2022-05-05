import java.util.concurrent.TimeUnit;
import org.ini4j.Ini;

public class Timer implements Runnable {

    private int limitMinuts = 0;
    private int curentMinuts = 1;
    private boolean stop = false;
    private StringBuilder time = new StringBuilder();

    public Timer(int limitMinuts) {
        this.limitMinuts = limitMinuts;
        System.out.println("Starting time for next : " + limitMinuts +" minutes");
    }

    public StringBuilder getTime() {
        return time;
    }

    public void setTime(StringBuilder time) {
        this.time = time;
    }

    @Override
    public void run() {
        while (!stop) {
            System.out.println("Starting wating time ... ");
            if (curentMinuts <= limitMinuts) {
                System.out.println("--> " + curentMinuts);
                time = new StringBuilder();
                time.append("<h2>This information will be available for the next " + limitMinuts + " Minutes.</h2>");
                time.append("<center><p><h1>Current time: " + curentMinuts + " / " + limitMinuts
                        + " Minutes. </p></h1></center>");
                try {
                    curentMinuts += 1;
                    TimeUnit.MINUTES.sleep(1);
                } catch (Exception e) {
                }
            } else {
                API.totlDash = 0;
                API.countDash = 0;
                API.requestMAP.clear();
                API.isMetricsFill = false;
                API.isConnError = false;
                API.isTimerError = false;
                stop = true;
            }

        }

    }

}