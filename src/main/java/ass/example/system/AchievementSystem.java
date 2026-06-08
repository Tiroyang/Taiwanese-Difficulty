package ass.example.system;

import ass.example.core.DeathReason;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * AchievementSystem
 *
 * 成就系統。
 *
 * 功能：
 * 1. 記錄已解鎖的死亡成就。
 * 2. 判斷指定死亡原因是否已解鎖。
 * 3. 第一次觸發死亡原因時解鎖成就。
 * 4. 將成就資料儲存在使用者資料夾。
 * 5. 啟動遊戲時讀取已解鎖成就。
 * 6. 提供成就數量統計。
 * 7. 支援重置全部成就。
 */
public final class AchievementSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * AchievementSystem 單例。
     */
    private static final AchievementSystem INSTANCE = new AchievementSystem();

    /**
     * 取得 AchievementSystem 單例。
     *
     * @return AchievementSystem
     */
    public static AchievementSystem getInstance() {
        return INSTANCE;
    }


    // =========================================================
    // Save File Constants
    // =========================================================

    /**
     * 遊戲資料資料夾名稱。
     *
     * 位置：
     * 使用者家目錄 / .taiwanese_difficulty
     */
    private static final String SAVE_FOLDER_NAME = ".taiwanese_difficulty";

    /**
     * 成就存檔檔名。
     */
    private static final String SAVE_FILE_NAME = "achievements.txt";


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 已解鎖死亡成就 ID 集合。
     *
     * 使用 DeathReason.getId() 作為儲存內容。
     */
    private final Set<String> unlockedDeathIds = new HashSet<>();

    /**
     * 成就存檔完整路徑。
     */
    private final Path saveFilePath;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立成就系統。
     *
     * private：
     * - 避免外部 new AchievementSystem()
     * - 確保全遊戲只使用同一份 AchievementSystem
     *
     * 建立時會自動讀取成就檔案。
     */
    private AchievementSystem() {
        this.saveFilePath = Path.of(
                System.getProperty("user.home"),
                SAVE_FOLDER_NAME,
                SAVE_FILE_NAME
        );

        load();
    }


    // =========================================================
    // Query
    // =========================================================

    /**
     * 判斷指定死亡原因是否已解鎖。
     *
     * @param reason 死亡原因
     * @return true 表示已解鎖
     */
    public boolean isUnlocked(DeathReason reason) {
        if (reason == null) {
            return false;
        }

        return unlockedDeathIds.contains(reason.getId());
    }

    /**
     * 取得目前已解鎖成就數。
     *
     * @return 已解鎖數量
     */
    public int getUnlockedCount() {
        return unlockedDeathIds.size();
    }

    /**
     * 取得全部死亡成就數。
     *
     * 目前每一個 DeathReason 都對應一個死亡成就。
     *
     * @return 成就總數
     */
    public int getTotalCount() {
        return DeathReason.values().length;
    }


    // =========================================================
    // Unlock
    // =========================================================

    /**
     * 解鎖指定死亡原因成就。
     *
     * 若該死亡原因已經解鎖，不會重複存檔。
     *
     * @param reason 死亡原因
     * @return true 表示本次是第一次解鎖；false 表示已解鎖過或 reason 為 null
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


    // =========================================================
    // Reset
    // =========================================================

    /**
     * 重置全部成就。
     *
     * 用途：
     * - Debug
     * - 設定選單中的清除成就資料
     */
    public void resetAll() {
        unlockedDeathIds.clear();
        save();
    }


    // =========================================================
    // Load / Save
    // =========================================================

    /**
     * 從檔案讀取成就資料。
     *
     * 檔案格式：
     * 每一行是一個 DeathReason id。
     */
    private void load() {
        try {
            createSaveFolderIfNeeded();

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

        } catch (IOException exception) {
            System.out.println("Achievement load failed.");
            exception.printStackTrace();
        }
    }

    /**
     * 將目前成就資料存入檔案。
     */
    private void save() {
        try {
            createSaveFolderIfNeeded();

            Files.write(
                    saveFilePath,
                    unlockedDeathIds
            );

        } catch (IOException exception) {
            System.out.println("Achievement save failed.");
            exception.printStackTrace();
        }
    }

    /**
     * 確保存檔資料夾存在。
     */
    private void createSaveFolderIfNeeded() throws IOException {
        Files.createDirectories(saveFilePath.getParent());
    }
}