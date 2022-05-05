import static spark.Spark.get;
import static spark.Spark.port;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

public class API {

    private static Config config;
    private static Finder finder = null; // clase que hace la busqueda
    public static HashMap<String, String> requestMAP = new HashMap<String, String>();
    public static String dateTime = "";
    // vemos el tiempo de los dashboards
    public static int totlDash = 0;
    public static int countDash = 0;
    // verificamos la hora
    public static long time_start, time_current;
    // si las metricas ya estan llenas
    public static boolean isMetricsFill = false;
    // si el cronometro esta activo
    public static boolean isChronometer = false;
    // if there is a error on connection
    public static boolean isConnError = false;
    public static boolean isTimerError = false;
    public static Timer timer;

    private static Ini.Section section;

    public static void main(String[] args) throws Exception {

        // config INI
        config = Config.getInstance(args);
        section = Config.ini.get("API_PORT");
        int port = section.get("port", Integer.class);
        section = Config.ini.get("ENGINE");
        port(port);

        // Starting software with schedulle
        // DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        // dateTime = dtf.format(LocalDateTime.now());
        // Schedule dasSchedule = Schedule.getInstance();

        // API
        get("/", (request, response) -> { // #1 SPARK GET API
            response.type("text/html");
            // ------------- DATE FORMAT -------------
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            dateTime = dt.format(LocalDateTime.now());
            StringBuilder htmlCode = new StringBuilder();
            htmlCode.append("<html><h1>DASHBOARD ALERT :: " + dateTime + " :: </h1>");
            htmlCode.append("<h2>Process Status: " + countDash + "/" + totlDash);
            // ------------- PARSING PROCESS -------------
            // if is a new request (to avoid grafana timeout)
            if (!requestMAP.containsKey(dateTime)) {
                requestMAP.put(dateTime, dateTime);
                System.out.println("---> Starting process <---");
                time_start = System.currentTimeMillis();
                isChronometer = false;
                timer = null;
                Config.killSingleton();
                config = Config.getInstance(args);
                // Starting process ...
                finder = new Finder("API");
            } else {
                if (!isMetricsFill) {
                    time_current = (System.currentTimeMillis() - time_start);
                }
                htmlCode.append("<br>Running time: " + formtTime(time_current) + " </h2>");
                if (!isChronometer) {
                    htmlCode.append("<h1>This process still working, please Reload (F5) </h1>");
                } else {
                    htmlCode.append(timer.getTime().toString());
                }
                System.out.println("---> Request denied due to previous work <---");
            }
            htmlCode.append("</html>");

            if (isConnError) {
                htmlCode = new StringBuilder();
                htmlCode.append("<html><h1>DASHBOARD ALERT :: " + dateTime + " :: </h1>");
                htmlCode.append(
                        "<center><h1 style=\"color:red\"> ERROR :: API CANNOT CONNECT TO THE DATABASE </h1><html></center>");
                // ------| Timer-Thread-Engine |------
                if (!isTimerError) {
                    section = Config.ini.get("ENGINE");
                    int limitMinuts = section.get("ErrorTimer", Integer.class);
                    timer = new Timer(limitMinuts);
                    Thread hiloRequest = new Thread(API.timer);
                    hiloRequest.start(); // More information timer.class
                    isTimerError = true;
                }
            }
            return htmlCode.toString();
        });

    }

    public static Connection startConnection() { // DB connection

        Connection conn = null;
        try {
            Ini.Section section = Config.ini.get("DATABASE");
            String psqlHost = section.get("psqlHost", String.class);
            String psqlDBName = section.get("psqlDBName", String.class);
            String psqlUser = section.get("psqlUser", String.class);
            String psqlPass = section.get("psqlPass", String.class);
            conn = DriverManager.getConnection("jdbc:postgresql://" + psqlHost + "/" + psqlDBName, psqlUser, psqlPass);
            System.out.println("You are now connected to the server \n");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return conn;
    }

    private static String formtTime(long millis) {
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis)
                        - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return hms;
    }
}