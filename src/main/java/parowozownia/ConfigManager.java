package parowozownia;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static int locomotivesCount = 8;
    private static int depotSpotsCount = 4;
    private static final String FILE_NAME = "config.properties";

    public static void loadConfig() {
        Properties prop = new Properties();
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            createDefaultConfig();
        }

        try (InputStream input = new FileInputStream(file)) {
            prop.load(input);
            locomotivesCount = Integer.parseInt(prop.getProperty("locomotives", "8"));
            depotSpotsCount = Integer.parseInt(prop.getProperty("depot_spots", "4"));
        } catch (IOException | NumberFormatException ex) {
            System.out.println("Błąd wczytywania. Załadowano wartości domyślne.");
        }
    }

    private static void createDefaultConfig() {
        Properties prop = new Properties();
        prop.setProperty("locomotives", "8");
        prop.setProperty("depot_spots", "4");
        try (OutputStream output = new FileOutputStream(FILE_NAME)) {
            prop.store(output, "Konfiguracja Parowozowni");
        } catch (IOException e) {
            System.out.println("Nie udało się utworzyć pliku domyślnego.");
        }
    }

    public static int getLocomotivesCount() { return locomotivesCount; }
    public static int getDepotSpotsCount() { return depotSpotsCount; }
}