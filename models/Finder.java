import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import org.ini4j.Ini;

public class Finder {

    private HashMap<Integer, String> dbTitles = new HashMap<Integer, String>();
    private HashMap<Integer, String> dbData = new HashMap<Integer, String>();
    private List<Dashboard> listDashboards = new ArrayList<Dashboard>();
    public static int threadRunning = 1;
    private Connection conn = null;
    public String dateTime = null;
    public Config config;
    private String savedDate = "";
    private String actualDate = "";
    private String requestName;

    public Finder(String requestName) {
        this.requestName = requestName;
        try {
            fillHashMaps(); // ---> Gettigng the JSON of each dashboard from grafana.db
            dashObject(); // ---> Dashboards Object from Dashboards.class
            runThreads(); // ---> Making the process concurrent
        } catch (Exception e) {
            System.out.println("Error Finder : " + e.getMessage());
        }
    }

    private void fillHashMaps() throws SQLException {
        dbTitles.clear();
        dbTitles.clear();
        listDashboards.clear();
        Ini.Section section = Config.ini.get("DATABASE");
        String pathDB = section.get("pathDB", String.class);
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + pathDB);
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery("select title,data,id from dashboard");
        while (resultSet.next()) {
            dbTitles.put(resultSet.getInt("id"), resultSet.getString("title"));
            dbData.put(resultSet.getInt("id"), resultSet.getString("data"));
        }
    }

    private void dashObject() throws Exception {

        dbTitles.forEach((key, title) -> {
            // ------------- Parte 1 -------------
            Dashboard dashboard = new Dashboard(requestName);
            DocumentContext documentContext = JsonPath.parse(dbData.get(key));
            String uid = (String) documentContext.read("$['uid']", new com.jayway.jsonpath.Predicate[0]);
            dashboard.setTitulo(title);
            dashboard.setId(key);
            dashboard.setUid(uid);
            // ------------- Parte 2 -------------
            try {
                List variables = (List) documentContext.read("$['templating']['list']",
                        new com.jayway.jsonpath.Predicate[0]);

                if (variables.size() > 0) {
                    variables.forEach(variable -> {
                        DocumentContext context1 = JsonPath.parse(variable);
                        String label = (String) context1.read("$['label']",
                                new com.jayway.jsonpath.Predicate[0]);
                        String datasource = (String) context1.read("$['datasource']",
                                new com.jayway.jsonpath.Predicate[0]);
                        String query = (String) context1.read("$['query']",
                                new com.jayway.jsonpath.Predicate[0]);
                        String regex = (String) context1.read("$['regex']",
                                new com.jayway.jsonpath.Predicate[0]);
                        dashboard.getVariables().add(label);
                        dashboard.getDatasource().add(datasource);
                        dashboard.getQuery().add(query);
                        dashboard.getRegx().add(regex);
                    });
                } else {
                    dashboard.getVariables().add("null");
                    dashboard.getDatasource().add("null");
                    dashboard.getQuery().add("null");
                    dashboard.getRegx().add("null");
                }

                listDashboards.add(dashboard);
            } catch (Exception e) {
            }

        });
    }

    private void runThreads() throws SQLException {
        // ------| Getting values from config.ini |------
        Ini.Section section = Config.ini.get("ENGINE");
        int maxThreads = section.get("maxThreads", Integer.class);

        // ------| Database |------
        conn = startConnection();

        // ------| API - Request |------
        if (requestName.equals("API")) {
            if (!truncateTable()) {
                API.isConnError = true;
                return;
            }
            // ------| Thread-Engine |------
            // >>> Global variable (API.class)
            API.totlDash = listDashboards.size();
            AtomicInteger index = new AtomicInteger(0);
            listDashboards.forEach(dashboard -> {
                // >>> Creating Threads
                Thread thread = new Thread(dashboard);
                // >>> DB CONNECTION - validation
                connValidation(dashboard.getId(), dashboard.getTitulo());
                if (conn != null) {
                    dashboard.setConn(conn);
                    thread.start();
                }
                threadRunning += 1;
                index.incrementAndGet();
                // >>> Thread-Queue #1
                // sleep while the queue is over the limit of threads
                // sleep while the amount of threads are less than the total of dashboards
                while (threadRunning > maxThreads && index.get() < listDashboards.size()) {
                    try {
                        String format = "---> %1$-10s\n";
                        System.out.format(format, "waiting in the thread queue");
                        TimeUnit.SECONDS.sleep(20);
                    } catch (Exception e) {
                    }
                }
            });

            // >>> Thread-Queue #2
            // sleep untill all the threads are done
            while (API.countDash < API.totlDash) {
                try {
                    System.out.println("waiting for the threads to finish working");
                    TimeUnit.SECONDS.sleep(20);
                } catch (Exception e) {
                }
            }
            // >>> Closing streams
            System.out.println("Dashboard Alert - " + requestName + " :: [DONE]");
            conn.close();
            conn = null;
            // >>> Global variables (API.class)
            API.isMetricsFill = true; // saving Running time
            API.time_current = (System.currentTimeMillis() - API.time_start);

            // ------| Timer-Thread-Engine |------
            section = Config.ini.get("ENGINE");
            int limitMinuts = section.get("requestMinutes", Integer.class);
            API.timer = new Timer(limitMinuts);
            Thread hiloRequest = new Thread(API.timer);
            API.isChronometer = true; // API will save information while isChronometer = true
            hiloRequest.start(); // More information RequestTime.class

        } else {
            // ------| SCHEDULE - Request |------
            AtomicInteger count = new AtomicInteger(1);
            listDashboards.forEach(dashboard -> {
                // >>> DB CONNECTION - validation
                connValidation(dashboard.getId(), dashboard.getTitulo());
                if (conn != null) {
                    dashboard.setConn(conn);
                    dashboard.dbMain();
                }

                System.out.println(
                        "Request-" + requestName + " :: [" + count.get() + "/" + listDashboards.size() + "] "
                                + dashboard.getTitulo());
                count.incrementAndGet();

            });
        }

    }

    public Connection startConnection() {

        Connection conn = null;
        try {
            Ini.Section section = Config.ini.get("DATABASE");
            String psqlHost = section.get("psqlHost", String.class);
            String psqlDBName = section.get("psqlDBName", String.class);
            String psqlUser = section.get("psqlUser", String.class);
            String psqlPass = section.get("psqlPass", String.class);
            conn = DriverManager.getConnection("jdbc:postgresql://" + psqlHost + "/" + psqlDBName, psqlUser, psqlPass);
            System.out.println("You are now connected to the server");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return conn;
    }

    private boolean truncateTable() {
        boolean stop = false;
        while (!stop) {
            try {
                PreparedStatement pst = conn.prepareStatement("TRUNCATE public.dashboards_api;");
                pst.executeUpdate();
                pst = conn.prepareStatement("TRUNCATE public.servers_api;");
                pst.executeUpdate();
                System.out.println("TRUNCATED TABLES");
                stop = false;
                return true;
            } catch (Exception e) {
                System.out.println("Error TRUNCATE: " + e.getMessage());
                connValidation(0, "TRUNCATE");
                if (conn == null) {
                    System.out.println("Error:: cannot connect ");
                    stop = true;
                }
            }
        }
        return false;
    }

    private void connValidation(int index, String dashName) {
        // ------| DBConnection-validation |------

        // ------| isClosed Error-validation |------
        try {
            if (conn != null && conn.isClosed()) {
                conn = null;
            }
        } catch (Exception e) {
        }
        // ------| Conn-validation |------
        int attempts = 0;
        try {
            while (conn == null && attempts <= 3) {
                attempts++;
                conn = startConnection();
                // validating attemps
                if (attempts == 3) {
                    System.out
                            .println("ERROR :: -> [DB # " + index + " " + dashName
                                    + "] :: The connection attempt failed");
                    return;
                }
                if (conn == null) {
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        } catch (Exception e) {
            System.out.println("Error :: connTest :: " + e.getMessage());
        }

    }

}
