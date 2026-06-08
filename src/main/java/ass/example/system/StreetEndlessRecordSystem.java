package ass.example.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * StreetEndlessRecordSystem
 *
 * Street Endless 小遊戲最高距離紀錄系統。
 *
 * 功能：
 * 1. 保存 Street Endless 模式的最高距離。
 * 2. 啟動時從本地檔案讀取最高距離。
 * 3. 若本局距離超過最高紀錄，更新並存檔。
 * 4. 支援重置最高紀錄。
 */
public final class StreetEndlessRecordSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * StreetEndlessRecordSystem 單例。
     */
    private static final StreetEndlessRecordSystem INSTANCE =
            new StreetEndlessRecordSystem();

    /**
     * 取得 StreetEndlessRecordSystem 單例。
     *
     * @return StreetEndlessRecordSystem
     */
    public static StreetEndlessRecordSystem getInstance() {
        return INSTANCE;
    }


    // =========================================================
    // Save File Constants
    // =========================================================

    /**
     * 遊戲資料資料夾名稱。
     */
    private static final String SAVE_FOLDER_NAME = ".taiwanese_difficulty";

    /**
     * Street Endless 紀錄檔案名稱。
     */
    private static final String SAVE_FILE_NAME =
            "street_endless_record.properties";

    /**
     * Properties 裡保存最高距離的 key。
     */
    private static final String KEY_BEST_DISTANCE = "bestDistance";


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 紀錄檔完整路徑。
     */
    private final Path saveFilePath = Path.of(
            System.getProperty("user.home"),
            SAVE_FOLDER_NAME,
            SAVE_FILE_NAME
    );

    /**
     * Street Endless 最高距離。
     */
    private double bestDistance = 0.0;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 Street Endless 紀錄系統。
     *
     * private：
     * - 避免外部 new。
     * - 確保全遊戲只使用同一份紀錄系統。
     */
    private StreetEndlessRecordSystem() {
        load();
    }


    // =========================================================
    // Query
    // =========================================================

    /**
     * 取得目前最高距離。
     *
     * @return 最高距離
     */
    public double getBestDistance() {
        return bestDistance;
    }


    // =========================================================
    // Update Record
    // =========================================================

    /**
     * 嘗試更新最高距離。
     *
     * 若傳入距離大於目前最高距離：
     * 1. 更新 bestDistance。
     * 2. 寫入本地檔案。
     * 3. 回傳 true。
     *
     * 若沒有超過最高紀錄，回傳 false。
     *
     * @param distance 本局距離
     * @return true 表示產生新紀錄
     */
    public boolean tryUpdateBestDistance(double distance) {
        if (distance <= bestDistance) {
            return false;
        }

        bestDistance = distance;
        save();

        return true;
    }


    // =========================================================
    // Reset
    // =========================================================

    /**
     * 重置最高紀錄。
     *
     * 會：
     * 1. 將 bestDistance 設為 0。
     * 2. 刪除本地紀錄檔。
     */
    public void reset() {
        bestDistance = 0.0;

        try {
            Files.deleteIfExists(saveFilePath);

        } catch (IOException exception) {
            System.out.println("Failed to delete street endless record.");
            exception.printStackTrace();
        }
    }


    // =========================================================
    // Load / Save
    // =========================================================

    /**
     * 從本地檔案讀取最高距離。
     *
     * 若檔案不存在或讀取失敗，最高距離會回到 0。
     */
    private void load() {
        if (!Files.exists(saveFilePath)) {
            bestDistance = 0.0;
            return;
        }

        try {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(saveFilePath));

            bestDistance = Double.parseDouble(
                    properties.getProperty(KEY_BEST_DISTANCE, "0")
            );

        } catch (Exception exception) {
            bestDistance = 0.0;

            System.out.println("Failed to load street endless record.");
            exception.printStackTrace();
        }
    }

    /**
     * 將目前最高距離寫入本地檔案。
     */
    private void save() {
        try {
            createSaveFolderIfNeeded();

            Properties properties = new Properties();
            properties.setProperty(
                    KEY_BEST_DISTANCE,
                    Double.toString(bestDistance)
            );

            properties.store(
                    Files.newOutputStream(saveFilePath),
                    "Street Endless Record"
            );

        } catch (IOException exception) {
            System.out.println("Failed to save street endless record.");
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