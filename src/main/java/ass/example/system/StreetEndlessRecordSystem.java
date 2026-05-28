package ass.example.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class StreetEndlessRecordSystem {

    private static final StreetEndlessRecordSystem INSTANCE = new StreetEndlessRecordSystem();

    private final Path saveFile = Path.of(
            System.getProperty("user.home"),
            ".taiwanese_difficulty",
            "street_endless_record.properties"
    );

    private double bestDistance = 0;

    private StreetEndlessRecordSystem() {
        load();
    }

    public static StreetEndlessRecordSystem getInstance() {
        return INSTANCE;
    }

    public double getBestDistance() {
        return bestDistance;
    }

    public boolean tryUpdateBestDistance(double distance) {
        if (distance <= bestDistance) {
            return false;
        }

        bestDistance = distance;
        save();
        return true;
    }

    public void reset() {
        bestDistance = 0;

        try {
            Files.deleteIfExists(saveFile);
        } catch (IOException e) {
            System.out.println("Failed to delete street endless record.");
            e.printStackTrace();
        }
    }

    private void load() {
        if (!Files.exists(saveFile)) {
            bestDistance = 0;
            return;
        }

        try {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(saveFile));

            bestDistance = Double.parseDouble(
                    properties.getProperty("bestDistance", "0")
            );

        } catch (Exception e) {
            bestDistance = 0;
            System.out.println("Failed to load street endless record.");
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(saveFile.getParent());

            Properties properties = new Properties();
            properties.setProperty("bestDistance", Double.toString(bestDistance));

            properties.store(
                    Files.newOutputStream(saveFile),
                    "Street Endless Record"
            );

        } catch (IOException e) {
            System.out.println("Failed to save street endless record.");
            e.printStackTrace();
        }
    }
}