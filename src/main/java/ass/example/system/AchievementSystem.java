package ass.example.system;

import ass.example.core.DeathReason;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getb;

public class AchievementSystem {

    private static final String SAVE_FOLDER_NAME = ".taiwanese_difficulty";
    private static final String SAVE_FILE_NAME = "achievements.txt";

    private final Set<String> unlockedDeathIds = new HashSet<>();
    private final Path saveFilePath;

    public AchievementSystem() {
        saveFilePath = Path.of(
                System.getProperty("user.home"),
                SAVE_FOLDER_NAME,
                SAVE_FILE_NAME
        );

        load();
    }

    public boolean isUnlocked(DeathReason reason) {
        return unlockedDeathIds.contains(reason.getId());
    }

    /**
     * @return true = 第一次解鎖；false = 已經解鎖過
     */
    public boolean unlockDeathReason(DeathReason reason) {
        if (reason == null) {
            return false;
        }

        boolean newlyUnlocked = unlockedDeathIds.add(reason.getId());

        if (newlyUnlocked) {
            save();
        }

        return newlyUnlocked;
    }

    public int getUnlockedCount() {
        return unlockedDeathIds.size();
    }

    public int getTotalCount() {
        return DeathReason.values().length;
    }

    public void resetAll() {
        unlockedDeathIds.clear();
        save();
    }

    private void load() {
        try {
            Files.createDirectories(saveFilePath.getParent());

            if (!Files.exists(saveFilePath)) {
                return;
            }

            unlockedDeathIds.clear();

            for (String line : Files.readAllLines(saveFilePath)) {
                String id = line.trim();

                if (!id.isEmpty()) {
                    unlockedDeathIds.add(id);
                }
            }

        } catch (IOException e) {
            System.out.println("Achievement load failed.");
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(saveFilePath.getParent());
            Files.write(saveFilePath, unlockedDeathIds);

        } catch (IOException e) {
            System.out.println("Achievement save failed.");
            e.printStackTrace();
        }
    }
}