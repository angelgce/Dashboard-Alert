import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ini4j.Ini;

public class Schedule {

    // ------| Singleton |------
    private static Schedule schedule = null;
    private boolean stop = false;
    private String dateTime;
    private String savedDate = "";

    public static Schedule getInstance() {
        if (schedule == null) {
            schedule = new Schedule();
            System.out.println("Singleton Schedule ...");
        }
        return schedule;
    }

    public static void killSingleton() {
        schedule = null;
    }

    public void killThread() {
        this.stop = true;
        System.out.println("Thread death");
    }

    // ------| Thread-Engine |------
    private Schedule() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    try {
                        // ------| Date & Time |------
                        System.out.println("---------| DASHBOARD-ALERT SCHEDULE |---------");
                        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                        dateTime = dt.format(LocalDateTime.now());
                        System.out.println("Actual Day [" + dateTime + "]");
                        System.out.println("Last Update [" + savedDate + "]");
                        if (isDay() && !savedDate.equals(dateTime) || savedDate.equals("")) {
                            savedDate = dateTime;
                            try {
                                // ------| STARTING COLLECTOR |------
                                Finder finder = new Finder("SCHEDULE");
                            } catch (Exception e) {
                                System.out.println("Error aqui : " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                    }

                    try {
                        TimeUnit.HOURS.sleep(12);
                    } catch (Exception e) {
                    }
                }

            }
        });
        thread.start();
    }

    private boolean isDay() {
        AtomicBoolean isDay = new AtomicBoolean(false);
        try {
            Ini.Section section = Config.ini.get("ENGINE");
            String days = section.get("days", String.class);
            String[] split = days.split(",");
            Arrays.asList(split).forEach(day -> {
                if (dateTime.endsWith(day)) {
                    isDay.set(true);
                }
            });

        } catch (Exception e) {
            System.out.println("isDay Error :: " + e.getMessage());
        }

        return isDay.get();
    }
}