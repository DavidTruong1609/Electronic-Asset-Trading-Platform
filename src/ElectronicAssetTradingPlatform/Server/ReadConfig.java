package ElectronicAssetTradingPlatform.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

public class ReadConfig {
    private static final String FILENAME = "ipconfig.props";

    public static HashMap<String, String> readConfigFile() {
        HashMap<String, String> str = new HashMap<>();

        try {
            FileInputStream file = new FileInputStream(FILENAME);

            // Get file as string
            String keyword = "";
            String value = "";
            boolean gettingValue = false;
            while (true) {
                char c = (char) file.read();
                if (c != (char) -1) {
                    if (c == '\n' || c == '\r') {
                        str.put(keyword, value);
                        gettingValue = false;
                        // Clear words
                        keyword = "";
                        value = "";
                    } else if (c == '=') {
                        gettingValue = true;
                    } else if (gettingValue) {
                        // Get value
                        value += c;
                    } else {
                        keyword += c;
                    }
                } else {
                    str.put(keyword, value);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Config file not found.");
            e.printStackTrace();
        }

        return str;
    }

    public static String getHostname(HashMap<String, String> configFile) throws IOException {
        String hostname = configFile.get("HOSTNAME");

        if (hostname != null) {
            return hostname;
        } else {
            throw new IOException("Host name not found in config file.");
        }
    }

    public static int getPort(HashMap<String, String> configFile) throws IOException {
        String port = configFile.get("PORT");

        if (port != null) {
            return Integer.parseInt(port);
        } else {
            throw new IOException("Port not found in config file.");
        }
    }
}
