import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ini4j.Ini;

public class Dashboard implements Runnable {

    private String titulo;
    private int id;
    private String uid;
    private String url;
    private List<String> variables = new ArrayList<String>();
    private List<String> datasource = new ArrayList<String>();
    private List<String> query = new ArrayList<String>();
    private List<String> regx = new ArrayList<String>();
    private String regex;
    private String keyJob = "";
    private String keyInstance = "";
    private StringBuilder errores = new StringBuilder();
    HashMap<String, String> finalDataObject = new HashMap<String, String>();
    private boolean isOnline = true;
    private boolean isIP = true;
    private String requestName = "";
    private Connection conn;

    public Dashboard(String requestName) {
        this.requestName = requestName;
    }

    public void dbMain() {
        url = "/d/" + uid;
        try {
            // ------------------------ #1 Parsing -----------------------
            finalDataObject = new HashMap<String, String>();
            errores = new StringBuilder();

            // ------| information from Prometheus-API |------
            if (!datasource.get(0).equals("PostgreSQL")) {
                String promPath = getDSURL(datasource.get(0)) + "/api/v1/query";
                // >>> Request of the query to the prom-API <<<
                List apiData = promAPI(promPath, formtQuery());
                if (apiData != null) {
                    finalDataObject = parsingQuery(apiData); // Getting jobs and ip of each Dashboard
                    if (finalDataObject.size() > 0) {
                        // System.out.println("DONE :: PARSING ");
                    } else {
                        // System.out.println("Error :: " + " Regx Doesnt Match");
                        setError("Error :: " + " Regx Doesnt Match");
                    }
                } else {
                    // ------| Error:: Request Time-out from Prometheus-API |------
                    // System.out.println("Error :: " + " failed: Connection refused");
                    setError("Error :: " + " Failed: Connection refused");
                }

            } else { // ------| information from PostgreSQL .INI |------
                // finalDataObject = parsingPSQL();
                isOnline = true;
                // System.out.println("DONE :: PostgreSQL");
            }

            // ------------------------ #2 Updating DB -----------------------
            // ------| updating dashboard information to database |------
            upadateDB();
            // ------| updating Server information to database |------
            if (finalDataObject.size() > 0) {
                upadateServers();
            }

        } catch (Exception e) {
            System.out.println("Error logic :: " + e.getMessage());
            setError("Logic error :: " + e.getMessage());
        }
    }

    private void setError(String msg) {
        errores.append(msg + " ");
        isOnline = false;
    }

    private HashMap<String, String> parsingPSQL() {
        // getting keywords from .ini
        HashMap<String, String> hashPSQL = new HashMap<String, String>();
        Ini.Section section = Config.ini.get("DB_FORMAT");
        String wordKeys = section.get("queryJob", String.class);
        String[] split = wordKeys.split(",");
        query.forEach(psqlQuery -> {
            Arrays.asList(split).forEach(key -> { // if my queries contains some of the keywords
                if (psqlQuery.contains(key)) {
                    try {
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(psqlQuery);
                        while (rs.next()) {
                            if (!hashPSQL.containsKey(rs.getString(1))) {
                                hashPSQL.put(rs.getString(1), "");
                            }
                        }
                        rs.close();
                        stmt.close();
                    } catch (Exception e) {
                        System.out.println("Error getting info from PSQL :: " + e.getMessage());
                        setError("Error getting info from PSQL :: " + e.getMessage());
                    }
                }
            });
        });
        return hashPSQL;
    }

    private void formtRegx(int index) {
        // ------------- Some regex need to be changed to worki with java
        regex = regx.get(index); // <- getting regx from json
        switch (index) {
            case 0:
                // > Case 1
                if (regex.matches("[\\^/].+[\\$/]")) {
                    regex = regex.substring(1, regex.length() - 1);
                }
                // > Case 2
                if (regex.contains("|")) {
                    if (!regex.matches("[\\^.][+][\\$|]|[^*][\\^(][\\S]+[\\$)]") && regex.matches("[.][+].+")) {
                        regex = regex.replace("|", ".+|");
                        regex += ".+";
                    }
                }
                break;
        }
    }

    private HashMap<String, String> parsingQuery(List data) {
        HashMap<String, String> hashJobs = new HashMap<String, String>();
        formtRegx(0); // formatting regex
        // Parsing the result of the API
        data.forEach(apiResult -> {
            // Getting Jobs
            String job = "";
            Pattern pattern = Pattern.compile(keyJob + "=(.+)},[\\s]");
            Matcher matcher = pattern.matcher(apiResult.toString());
            if (matcher.find()) {
                job = matcher.group(1).replaceAll("([,].+)", "");
            }
            // Getting IP
            String ip = "";
            if (isIP) { // If there's a IP
                pattern = Pattern.compile(keyInstance + "=([\\S]+)");
                matcher = pattern.matcher(apiResult.toString());
                if (matcher.find()) {
                    ip = matcher.group(1).replaceAll("[:].+", "");
                }
            }

            // Updating hashmap
            hashJobs.put(job, ip);
        });

        // Matching regx to Jobs
        if (regex != "") { // if the job has a regx
            HashMap<String, String> hashRegx = new HashMap<String, String>();
            hashJobs.forEach((key, value) -> {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(key.toString());
                if (matcher.find()) {
                    hashRegx.put(key, value);
                }
            });
            return hashRegx;
        } else { // if there's not regx return the default map
            return hashJobs;
        }
    }

    private List promAPI(String path, String query) {
        try {
            HttpPost httpPost = new HttpPost(path);
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20000).build();
            httpPost.setConfig(requestConfig);
            ArrayList<BasicNameValuePair> arrayList = new ArrayList();
            arrayList.add(new BasicNameValuePair("query", query));
            arrayList.add(new BasicNameValuePair("start", "$(date -d '24 hours ago'+%s)"));
            arrayList.add(new BasicNameValuePair("end", "$(date +%s)"));
            arrayList.add(new BasicNameValuePair("step", "1h"));
            httpPost.setEntity((HttpEntity) new UrlEncodedFormEntity(arrayList));
            CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient
                    .execute((HttpUriRequest) httpPost);
            // Parceamos json
            DocumentContext documentContext = JsonPath.parse(EntityUtils.toString(closeableHttpResponse.getEntity()));
            List variables = null;
            return variables = (List) documentContext.read("$['data']['result']",
                    new com.jayway.jsonpath.Predicate[0]);
        } catch (Exception e) {
        }
        return null;
    }

    private boolean upadateDB() {
        try {
            String db = "public.dashboards_api";
            if (requestName.equals("SCHEDULE")) {
                db = "public.dashboards";
            }
            String query = "INSERT INTO " + db
                    + "(dashboard_id, dashboard, status, status_info, url, date_time, datasource,ds_ip) VALUES('"
                    + id + "', '" + titulo + "', '" + booleanToString(isOnline) + "', '" + errores + "', '" + url
                    + "', '"
                    + API.dateTime + "', '" + datasource.get(0) + "','" + getDSURL(datasource.get(0)) + "');";

            PreparedStatement pst = conn.prepareStatement(query);
            pst.executeUpdate();
            pst.close();
            return true;
            // System.out.println("DONE :: UPDATE DASHBOARD");
        } catch (Exception e) {
            System.out.println("Error :: updateDB " + e.getMessage());
            return false;
        }
    }

    private String dbServ = "public.servers_api";

    private boolean upadateServers() {
        if (requestName.equals("SCHEDULE")) {
            dbServ = "public.servers";
        }
        try {
            finalDataObject.forEach((job, ip) -> {
                try {
                    String query = "INSERT INTO " + dbServ + "(server,date_time,dashboard_id, ip) VALUES ('"
                            + job
                            + "', '" + API.dateTime + "', '" + id + "','" + ip + "');";
                    PreparedStatement pst = conn.prepareStatement(query);
                    pst.executeUpdate();
                    pst.close();
                } catch (Exception e) {
                    System.out.println("Error :: upadtaing - server-update :: " + e.getMessage());
                    return;
                }
            });
            return true;
            // System.out.println("DONE :: UPDATE SERVER(S)");
        } catch (Exception e) {
            System.out.println("Error ::  global - server-update :: " + e.getMessage());
            return false;
        }

    }

    private String formtQuery() {
        String string = "";
        String rgx = "";

        // Some dashboard doesnt has the variable IP
        if (query.size() > 1) {
            string = query.get(1).toString();
            rgx = "label_values[\\^\\(](.+)[\\^{](.+)[\\=][\\S]+[\\s](.+)[\\)]";
        } else {
            string = query.get(0).toString();
            rgx = "label_values[\\^\\(](.+)[\\$,][\\s]+([\\^\\S]+)[\\$)]";
            isIP = false;
        }
        Pattern pattern = Pattern.compile(rgx);
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            if (isIP) {
                keyInstance = matcher.group(3);
            }
            keyJob = matcher.group(2);
            return matcher.group(1);
        }
        return null;
    }

    private static String getDSURL(String string) {
        switch (string) {
            case "PrometheusSoporte":
                return "http://10.119.249.53:9090";

            case "Nuevo Prometheus":
                return "http://10.119.199.200";

            case "Prom_Dist_DayServer":
                return "http://10.119.130.11:9090";

            case "Prom_Dist_WeekServer":
                return "http://10.119.130.12:9090";

            case "Prom_Dist_MonthServer":
                return "http://10.119.130.13:9090";
            case "PostgreSQL":
                return "http://10.119.249.53:5432";
        }
        return null;
    }

    private String booleanToString(boolean status) {
        if (status) {
            return "1";
        } else {
            return "0";
        }
    }

    @Override
    public synchronized void run() {
        boolean stop = false;
        while (!stop) {
            dbMain();
            System.out.println("Request-" + requestName + " :: [" + API.countDash + "/" + API.totlDash + "] " + titulo);
            stop = true;
            if (requestName.equals("API")) {
                API.countDash += 1;
                Finder.threadRunning -= 1;
            }

        }

    }
    // ------| Getters & Setters |------

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public List<String> getDatasource() {
        return datasource;
    }

    public void setDatasource(List<String> datasource) {
        this.datasource = datasource;
    }

    public List<String> getQuery() {
        return query;
    }

    public void setQuery(List<String> query) {
        this.query = query;
    }

    public List<String> getRegx() {
        return regx;
    }

    public void setRegx(List<String> regx) {
        this.regx = regx;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }
}