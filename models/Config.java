import java.io.File;

import org.apache.commons.cli.*;
import org.ini4j.Ini;

public class Config {

    private static Config config = null;
    public static Ini ini;

    private Config(String[] args) {
        start(args);
    }

    // Singleton
    public static Config getInstance(String[] args) {
        if (config == null) {
            System.out.println("Singleton config is loaded ...");
            config = new Config(args);
        }
        return config;
    }

    public static void killSingleton() {
        config = null;
    }

    private void start(String[] args) {
        Options options = new Options();
        Option cfg = new Option("c", "config", true, "config file path");
        cfg.setRequired(true);
        options.addOption(cfg);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String configFile = cmd.getOptionValue("config");

        if (configFile == null) {
            configFile = "./config.ini";
        }

        try {
            ini = new Ini(new File(configFile));
        } catch (Exception e) {
            System.out.println("Error INI : " + e.getMessage());
        }
    }

}