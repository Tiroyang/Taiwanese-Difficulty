    package ass.example.system.save;
    
    import ass.example.core.DeathReason;
    import ass.example.core.QuestType;
    import ass.example.core.SaveKey;
    import com.almasb.fxgl.core.serialization.Bundle;
    import javafx.scene.Node;
    
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.*;
    
    /**
     * SaveSlotManager
     *
     * 存檔槽位管理系統。
     *
     * 功能：
     * 1. 管理最多 MAX_SLOTS 個存檔槽位。
     * 2. 讀取槽位摘要資料。
     * 3. 儲存指定槽位。
     * 4. 快速存檔。
     * 5. 載入指定槽位。
     * 6. 刪除槽位。
     * 7. 重新命名槽位。
     * 8. 判斷目前是否有未儲存變更。
     * 9. 管理「主選單請求載入某槽位」的 pending request。
     */
    public final class SaveSlotManager {
    
        // =========================================================
        // Singleton
        // =========================================================
    
        private static final SaveSlotManager INSTANCE = new SaveSlotManager();
    
        public static SaveSlotManager getInstance() {
            return INSTANCE;
        }
    
    
        // =========================================================
        // Constants
        // =========================================================
    
        /**
         * 最大存檔槽位數。
         */
        public static final int MAX_SLOTS = 6;

        /**
         * 存檔檔案副檔名。
         */
        private static final String SAVE_FILE_EXTENSION = ".properties";

        /**
         * 存檔檔名前綴。
         */
        private static final String SLOT_FILE_PREFIX = "slot_";

        /**
         * 預設存檔名稱時間格式。
         */
        private static final DateTimeFormatter SAVE_NAME_TIME_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm");

    
        // =========================================================
        // Save Folder
        // =========================================================
    
        /**
         * 存檔資料夾。
         */
        private final Path saveFolder = Path.of(
                System.getProperty("user.home"),
                ".taiwanese_difficulty",
                "saves"
        );
    
    
        // =========================================================
        // Runtime State
        // =========================================================
    
        /**
         * 目前操作中的存檔槽位。
         *
         * -1 表示尚未指定。
         */
        private int currentSlotIndex = -1;
    
        /**
         * 最近一次載入或儲存內容的 hash。
         *
         * 用於判斷是否有未儲存變更。
         */
        private String lastLoadedHash = "";
    
    
        // =========================================================
        // Pending Load Request
        // =========================================================
    
        /**
         * 等待載入的槽位。
         *
         * 用途：
         * 主選單或 UI 可以先呼叫 requestLoadSlot(slot)，
         * 之後 Main / Scene 啟動時再 consume。
         */
        private Integer pendingLoadSlotIndex = null;
    
    
        // =========================================================
        // Constructor
        // =========================================================
    
        private SaveSlotManager() {
            createSaveFolderIfNeeded();
        }
    
    
        // =========================================================
        // Slot Query
        // =========================================================
    
        /**
         * 取得所有槽位資料。
         *
         * @return 所有槽位摘要
         */
        public List<SaveSlotData> getSlots() {
            List<SaveSlotData> result = new ArrayList<>();
    
            for (int i = 0; i < MAX_SLOTS; i++) {
                result.add(readSlot(i));
            }
    
            return result;
        }
    
        /**
         * 讀取指定槽位摘要。
         *
         * 若檔案不存在或讀取失敗，回傳 empty slot。
         *
         * @param slotIndex 槽位 index
         * @return 槽位資料
         */
        public SaveSlotData readSlot(int slotIndex) {
            Path path = getSlotPath(slotIndex);
    
            if (!Files.exists(path)) {
                return SaveSlotData.empty(slotIndex);
            }
    
            try {
                Properties props = loadProperties(path);
    
                return new SaveSlotData(
                        slotIndex,
                        true,
                        props.getProperty("meta.saveName", getDefaultSaveName(slotIndex)),
                        props.getProperty("bundle.sceneType", "UNKNOWN"),
                        props.getProperty("bundle.thumbnailBase64", ""),
                        getLong(props, "meta.createdAt"),
                        getLong(props, "meta.savedAt"),
                        getLong(props, "meta.lastOpenedAt")
                );
    
            } catch (Exception exception) {
                exception.printStackTrace();
                return SaveSlotData.empty(slotIndex);
            }
        }
    
        /**
         * 尋找第一個空槽位。
         *
         * @return 空槽位 index；若沒有空槽位則回傳 -1
         */
        private int findFirstEmptySlot() {
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (!readSlot(i).exists()) {
                    return i;
                }
            }
    
            return -1;
        }
    
    
        // =========================================================
        // Save
        // =========================================================
    
        /**
         * 快速存檔。
         *
         * 若目前已有 currentSlotIndex，會覆蓋目前槽位。
         * 否則找第一個空槽位。
         * 若沒有空槽位，覆蓋第 0 格。
         *
         * @param saveSystem SaveSystem
         * @return 實際儲存槽位
         */
        public int quickSave(SaveSystem saveSystem) {
            return quickSave(saveSystem, null);
        }
    
        /**
         * 快速存檔。
         *
         * @param saveSystem SaveSystem
         * @param nodeToHideBeforeScreenshot 截圖前要暫時隱藏的 UI，可為 null
         * @return 實際儲存槽位
         */
        public int quickSave(
                SaveSystem saveSystem,
                Node nodeToHideBeforeScreenshot
        ) {
            if (hasValidCurrentSlot()) {
                SaveSlotData currentSlot = readSlot(currentSlotIndex);
    
                saveToSlot(
                        currentSlotIndex,
                        currentSlot.getSaveName(),
                        saveSystem,
                        true,
                        nodeToHideBeforeScreenshot
                );
    
                return currentSlotIndex;
            }
    
            int targetSlot = findFirstEmptySlot();
    
            if (targetSlot == -1) {
                targetSlot = 0;
            }
    
            saveToSlot(
                    targetSlot,
                    createAutoSaveName(),
                    saveSystem,
                    true,
                    nodeToHideBeforeScreenshot
            );
    
            return targetSlot;
        }
    
        /**
         * 儲存到指定槽位。
         */
        public void saveToSlot(
                int slotIndex,
                String saveName,
                SaveSystem saveSystem,
                boolean overwrite
        ) {
            saveToSlot(slotIndex, saveName, saveSystem, overwrite, null);
        }
    
        /**
         * 儲存到指定槽位。
         *
         * @param slotIndex 槽位 index
         * @param saveName 存檔名稱
         * @param saveSystem SaveSystem
         * @param overwrite 是否覆蓋既有存檔
         * @param nodeToHideBeforeScreenshot 截圖前要暫時隱藏的 UI，可為 null
         */
        public void saveToSlot(
                int slotIndex,
                String saveName,
                SaveSystem saveSystem,
                boolean overwrite,
                Node nodeToHideBeforeScreenshot
        ) {
            if (saveSystem == null) {
                return;
            }
    
            SaveSlotData oldData = readSlot(slotIndex);
    
            if (oldData.exists() && !overwrite) {
                return;
            }
    
            Bundle bundle = saveSystem.createSaveBundle(nodeToHideBeforeScreenshot);
    
            long now = System.currentTimeMillis();
            long createdAt = oldData.exists() ? oldData.getCreatedAt() : now;
    
            Properties props = new Properties();
    
            writeMetaToProperties(
                    props,
                    slotIndex,
                    normalizeSaveName(slotIndex, saveName),
                    createdAt,
                    now,
                    now
            );
    
            writeBundleToProperties(bundle, props);
    
            try {
                createSaveFolderIfNeeded();
    
                props.store(
                        Files.newOutputStream(getSlotPath(slotIndex)),
                        "Taiwanese Difficulty Save Slot " + slotIndex
                );
    
                currentSlotIndex = slotIndex;
                lastLoadedHash = computeContentHash(bundle);
    
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    
    
        // =========================================================
        // Load
        // =========================================================
    
        /**
         * 載入指定槽位。
         *
         * @param slotIndex 槽位 index
         * @param saveSystem SaveSystem
         */
        public void loadSlot(
                int slotIndex,
                SaveSystem saveSystem
        ) {
            if (saveSystem == null) {
                return;
            }
    
            Path path = getSlotPath(slotIndex);
    
            if (!Files.exists(path)) {
                return;
            }
    
            try {
                Properties props = loadProperties(path);
    
                updateLastOpenedAt(path, props);
    
                Bundle bundle = readBundleFromProperties(props);
    
                saveSystem.loadFromBundle(bundle);
    
                currentSlotIndex = slotIndex;
                lastLoadedHash = computeContentHash(bundle);
    
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    
        /**
         * 更新槽位最後開啟時間。
         */
        private void updateLastOpenedAt(
                Path path,
                Properties props
        ) throws IOException {
            props.setProperty(
                    "meta.lastOpenedAt",
                    String.valueOf(System.currentTimeMillis())
            );
    
            props.store(
                    Files.newOutputStream(path),
                    "Taiwanese Difficulty Save Slot"
            );
        }
    
    
        // =========================================================
        // Delete / Rename
        // =========================================================
    
        /**
         * 刪除指定槽位。
         *
         * @param slotIndex 槽位 index
         */
        public void deleteSlot(int slotIndex) {
            try {
                Files.deleteIfExists(getSlotPath(slotIndex));
    
                if (currentSlotIndex == slotIndex) {
                    currentSlotIndex = -1;
                    lastLoadedHash = "";
                }
    
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    
        /**
         * 重新命名指定槽位。
         *
         * @param slotIndex 槽位 index
         * @param newName 新名稱
         */
        public void renameSlot(
                int slotIndex,
                String newName
        ) {
            if (newName == null || newName.isBlank()) {
                return;
            }
    
            Path path = getSlotPath(slotIndex);
    
            if (!Files.exists(path)) {
                return;
            }
    
            try {
                Properties props = loadProperties(path);
                props.setProperty("meta.saveName", newName);
    
                props.store(
                        Files.newOutputStream(path),
                        "Taiwanese Difficulty Save Slot " + slotIndex
                );
    
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    
    
        // =========================================================
        // Unsaved Changes
        // =========================================================
    
        /**
         * 判斷目前是否有未儲存變更。
         *
         * @param saveSystem SaveSystem
         * @return true 表示目前內容與最近載入/儲存內容不同
         */
        public boolean hasUnsavedChanges(SaveSystem saveSystem) {
            if (saveSystem == null) {
                return false;
            }
    
            Bundle current = saveSystem.createSaveBundle();
            String currentHash = computeContentHash(current);
    
            if (lastLoadedHash == null || lastLoadedHash.isBlank()) {
                return true;
            }
    
            return !currentHash.equals(lastLoadedHash);
        }
    
        /**
         * 計算存檔內容 hash。
         *
         * savedAt 與 thumbnailBase64 不參與 hash。
         * 因為它們每次存檔都可能改變，不能用來判斷遊戲內容是否改變。
         */
        private String computeContentHash(Bundle bundle) {
            Properties props = new Properties();
    
            writeBundleToProperties(bundle, props);
    
            props.remove("bundle.savedAt");
            props.remove("bundle.thumbnailBase64");
    
            List<String> keys = new ArrayList<>();
    
            for (Object key : props.keySet()) {
                keys.add(String.valueOf(key));
            }
    
            Collections.sort(keys);
    
            StringBuilder builder = new StringBuilder();
    
            for (String key : keys) {
                builder.append(key)
                        .append("=")
                        .append(props.getProperty(key))
                        .append("\n");
            }
    
            return Integer.toHexString(builder.toString().hashCode());
        }
    
    
        // =========================================================
        // Pending Load Request
        // =========================================================
    
        /**
         * 請求稍後載入指定槽位。
         *
         * 取代原本 SaveRequestSystem.requestLoadSlot(...)。
         *
         * @param slotIndex 槽位 index
         */
        public void requestLoadSlot(int slotIndex) {
            pendingLoadSlotIndex = slotIndex;
        }
    
        /**
         * 是否有等待載入的槽位。
         */
        public boolean hasPendingLoadSlot() {
            return pendingLoadSlotIndex != null;
        }
    
        /**
         * 取出並清除等待載入的槽位。
         *
         * @return 槽位 index
         */
        public int consumePendingLoadSlot() {
            int slot = pendingLoadSlotIndex;
            pendingLoadSlotIndex = null;
            return slot;
        }
    
        /**
         * 清除等待載入請求。
         */
        public void clearPendingLoadSlot() {
            pendingLoadSlotIndex = null;
        }
    
    
        // =========================================================
        // Bundle -> Properties
        // =========================================================
    
        /**
         * 將 Bundle 內容寫入 Properties。
         */
        private void writeBundleToProperties(
                Bundle bundle,
                Properties props
        ) {
            putString(props, bundle, SaveKey.SCENE_TYPE, "bundle.sceneType");
    
            putDouble(props, bundle, SaveKey.PLAYER_X, "bundle.playerX");
            putDouble(props, bundle, SaveKey.PLAYER_Y, "bundle.playerY");
    
            putInt(props, bundle, SaveKey.DEATH_COUNT, "bundle.deathCount");
    
            putBool(props, bundle, SaveKey.QUILT_FOLDED, "bundle.quiltFolded");
            putBool(props, bundle, SaveKey.WATER_DRUNK, "bundle.waterDrunk");
            putBool(props, bundle, SaveKey.TEETH_BRUSHED, "bundle.teethBrushed");
            putBool(props, bundle, SaveKey.SHOES_WORN, "bundle.shoesWorn");
            putBool(props, bundle, SaveKey.PLAYER_ON_BED_COLLIDER, "bundle.playerOnBedCollider");
    
            putBool(props, bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED, "bundle.roomLivingRoomRevealed");
            putBool(props, bundle, SaveKey.ROOM_TOILET_REVEALED, "bundle.roomToiletRevealed");
    
            putBool(props, bundle, SaveKey.DOOR_1_OPENED, "bundle.door1Opened");
            putBool(props, bundle, SaveKey.DOOR_2_OPENED, "bundle.door2Opened");
    
            putString(props, bundle, SaveKey.STREET_SEGMENTS, "bundle.streetSegments");
            putString(props, bundle, SaveKey.STREET_OBSTACLES, "bundle.streetObstacles");
    
            putBool(props, bundle, SaveKey.PLAYER_DEAD, "bundle.playerDead");
            putString(props, bundle, SaveKey.LAST_DEATH_REASON, "bundle.lastDeathReason");
    
            putLong(props, bundle, SaveKey.SAVED_AT, "bundle.savedAt");
            putString(props, bundle, SaveKey.THUMBNAIL_BASE64, "bundle.thumbnailBase64");
    
            writeDeathAchievementsToProperties(bundle, props);
            writeQuestStatesToProperties(bundle, props);
        }
    
        /**
         * 寫入死亡成就狀態。
         */
        private void writeDeathAchievementsToProperties(
                Bundle bundle,
                Properties props
        ) {
            for (DeathReason reason : DeathReason.values()) {
                String key = "death_" + reason.name();
                putBool(props, bundle, key, "bundle." + key);
            }
        }
    
        /**
         * 寫入任務狀態。
         */
        private void writeQuestStatesToProperties(
                Bundle bundle,
                Properties props
        ) {
            putInt(
                    props,
                    bundle,
                    SaveKey.QUEST_VISIBLE_START_INDEX,
                    "bundle." + SaveKey.QUEST_VISIBLE_START_INDEX
            );
    
            for (QuestType quest : QuestType.values()) {
                String id = quest.name();
    
                putInt(
                        props,
                        bundle,
                        SaveKey.QUEST_AMOUNT_PREFIX + id,
                        "bundle." + SaveKey.QUEST_AMOUNT_PREFIX + id
                );
    
                putBool(
                        props,
                        bundle,
                        SaveKey.QUEST_COMPLETED_PREFIX + id,
                        "bundle." + SaveKey.QUEST_COMPLETED_PREFIX + id
                );
    
                putBool(
                        props,
                        bundle,
                        SaveKey.QUEST_ANIM_PLAYED_PREFIX + id,
                        "bundle." + SaveKey.QUEST_ANIM_PLAYED_PREFIX + id
                );
            }
        }
    
    
        // =========================================================
        // Properties -> Bundle
        // =========================================================
    
        /**
         * 從 Properties 還原 Bundle。
         */
        private Bundle readBundleFromProperties(Properties props) {
            Bundle bundle = new Bundle(SaveKey.BUNDLE_NAME);
    
            bundle.put(SaveKey.SCENE_TYPE, props.getProperty("bundle.sceneType", "HOUSE"));
    
            bundle.put(SaveKey.PLAYER_X, getDouble(props, "bundle.playerX"));
            bundle.put(SaveKey.PLAYER_Y, getDouble(props, "bundle.playerY"));
    
            bundle.put(SaveKey.DEATH_COUNT, getInt(props, "bundle.deathCount"));
    
            bundle.put(SaveKey.QUILT_FOLDED, getBool(props, "bundle.quiltFolded"));
            bundle.put(SaveKey.WATER_DRUNK, getBool(props, "bundle.waterDrunk"));
            bundle.put(SaveKey.TEETH_BRUSHED, getBool(props, "bundle.teethBrushed"));
            bundle.put(SaveKey.SHOES_WORN, getBool(props, "bundle.shoesWorn"));
            bundle.put(SaveKey.PLAYER_ON_BED_COLLIDER, getBool(props, "bundle.playerOnBedCollider"));
    
            bundle.put(SaveKey.ROOM_LIVING_ROOM_REVEALED, getBool(props, "bundle.roomLivingRoomRevealed"));
            bundle.put(SaveKey.ROOM_TOILET_REVEALED, getBool(props, "bundle.roomToiletRevealed"));
    
            bundle.put(SaveKey.DOOR_1_OPENED, getBool(props, "bundle.door1Opened"));
            bundle.put(SaveKey.DOOR_2_OPENED, getBool(props, "bundle.door2Opened"));
    
            bundle.put(SaveKey.STREET_SEGMENTS, props.getProperty("bundle.streetSegments", ""));
            bundle.put(SaveKey.STREET_OBSTACLES, props.getProperty("bundle.streetObstacles", ""));
    
            bundle.put(SaveKey.PLAYER_DEAD, getBool(props, "bundle.playerDead"));
            bundle.put(SaveKey.LAST_DEATH_REASON, props.getProperty("bundle.lastDeathReason", ""));
    
            bundle.put(SaveKey.SAVED_AT, getLong(props, "bundle.savedAt"));
            bundle.put(SaveKey.THUMBNAIL_BASE64, props.getProperty("bundle.thumbnailBase64", ""));
    
            readDeathAchievementsFromProperties(props, bundle);
            readQuestStatesFromProperties(props, bundle);
    
            return bundle;
        }
    
        /**
         * 讀取死亡成就狀態。
         */
        private void readDeathAchievementsFromProperties(
                Properties props,
                Bundle bundle
        ) {
            for (DeathReason reason : DeathReason.values()) {
                String key = "death_" + reason.name();
                bundle.put(key, getBool(props, "bundle." + key));
            }
        }
    
        /**
         * 讀取任務狀態。
         */
        private void readQuestStatesFromProperties(
                Properties props,
                Bundle bundle
        ) {
            bundle.put(
                    SaveKey.QUEST_VISIBLE_START_INDEX,
                    getInt(props, "bundle." + SaveKey.QUEST_VISIBLE_START_INDEX)
            );
    
            for (QuestType quest : QuestType.values()) {
                String id = quest.name();
    
                bundle.put(
                        SaveKey.QUEST_AMOUNT_PREFIX + id,
                        getInt(props, "bundle." + SaveKey.QUEST_AMOUNT_PREFIX + id)
                );
    
                bundle.put(
                        SaveKey.QUEST_COMPLETED_PREFIX + id,
                        getBool(props, "bundle." + SaveKey.QUEST_COMPLETED_PREFIX + id)
                );
    
                bundle.put(
                        SaveKey.QUEST_ANIM_PLAYED_PREFIX + id,
                        getBool(props, "bundle." + SaveKey.QUEST_ANIM_PLAYED_PREFIX + id)
                );
            }
        }
    
    
        // =========================================================
        // Properties Helpers
        // =========================================================
    
        private void writeMetaToProperties(
                Properties props,
                int slotIndex,
                String saveName,
                long createdAt,
                long savedAt,
                long lastOpenedAt
        ) {
            props.setProperty("meta.slotIndex", String.valueOf(slotIndex));
            props.setProperty("meta.saveName", saveName);
            props.setProperty("meta.createdAt", String.valueOf(createdAt));
            props.setProperty("meta.savedAt", String.valueOf(savedAt));
            props.setProperty("meta.lastOpenedAt", String.valueOf(lastOpenedAt));
        }
    
        private Properties loadProperties(Path path) throws IOException {
            Properties props = new Properties();
            props.load(Files.newInputStream(path));
            return props;
        }
    
        private void putString(
                Properties props,
                Bundle bundle,
                String bundleKey,
                String propKey
        ) {
            try {
                String value = bundle.get(bundleKey);
                props.setProperty(propKey, value == null ? "" : value);
            } catch (Exception ignored) {
            }
        }
    
        private void putDouble(
                Properties props,
                Bundle bundle,
                String bundleKey,
                String propKey
        ) {
            try {
                double value = bundle.get(bundleKey);
                props.setProperty(propKey, String.valueOf(value));
            } catch (Exception ignored) {
            }
        }
    
        private void putInt(
                Properties props,
                Bundle bundle,
                String bundleKey,
                String propKey
        ) {
            try {
                int value = bundle.get(bundleKey);
                props.setProperty(propKey, String.valueOf(value));
            } catch (Exception ignored) {
            }
        }
    
        private void putLong(
                Properties props,
                Bundle bundle,
                String bundleKey,
                String propKey
        ) {
            try {
                long value = bundle.get(bundleKey);
                props.setProperty(propKey, String.valueOf(value));
            } catch (Exception ignored) {
            }
        }
    
        private void putBool(
                Properties props,
                Bundle bundle,
                String bundleKey,
                String propKey
        ) {
            try {
                boolean value = bundle.get(bundleKey);
                props.setProperty(propKey, String.valueOf(value));
            } catch (Exception ignored) {
            }
        }
    
        private double getDouble(Properties props, String key) {
            try {
                return Double.parseDouble(props.getProperty(key, "0"));
            } catch (Exception exception) {
                return 0;
            }
        }
    
        private int getInt(Properties props, String key) {
            try {
                return Integer.parseInt(props.getProperty(key, "0"));
            } catch (Exception exception) {
                return 0;
            }
        }
    
        private long getLong(Properties props, String key) {
            try {
                return Long.parseLong(props.getProperty(key, "0"));
            } catch (Exception exception) {
                return 0;
            }
        }
    
        private boolean getBool(Properties props, String key) {
            return Boolean.parseBoolean(props.getProperty(key, "false"));
        }
    
    
        // =========================================================
        // File / Name Helpers
        // =========================================================
    
        private void createSaveFolderIfNeeded() {
            try {
                Files.createDirectories(saveFolder);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    
        private boolean hasValidCurrentSlot() {
            return currentSlotIndex >= 0 && readSlot(currentSlotIndex).exists();
        }
    
        private Path getSlotPath(int slotIndex) {
            return saveFolder.resolve(
                    SLOT_FILE_PREFIX + slotIndex + SAVE_FILE_EXTENSION
            );
        }
    
        private String getDefaultSaveName(int slotIndex) {
            return "Save " + (slotIndex + 1);
        }
    
        private String normalizeSaveName(
                int slotIndex,
                String saveName
        ) {
            if (saveName == null || saveName.isBlank()) {
                return getDefaultSaveName(slotIndex);
            }
    
            return saveName;
        }
    
        private String createAutoSaveName() {
            return "Save " + LocalDateTime.now().format(SAVE_NAME_TIME_FORMAT);
        }
    
    
        // =========================================================
        // Getters
        // =========================================================
    
        public int getCurrentSlotIndex() {
            return currentSlotIndex;
        }
    
    
        // =========================================================
        // Nested Data Class
        // =========================================================
    
        /**
         * SaveSlotData
         *
         * 單一存檔槽位的摘要資料。
         * 合併進 SaveSlotManager。
         */
        public static class SaveSlotData {
    
            private final int slotIndex;
            private final boolean exists;
    
            private final String saveName;
            private final String sceneName;
            private final String thumbnailBase64;
    
            private final long createdAt;
            private final long savedAt;
            private final long lastOpenedAt;
    
            public SaveSlotData(
                    int slotIndex,
                    boolean exists,
                    String saveName,
                    String sceneName,
                    String thumbnailBase64,
                    long createdAt,
                    long savedAt,
                    long lastOpenedAt
            ) {
                this.slotIndex = slotIndex;
                this.exists = exists;
                this.saveName = saveName;
                this.sceneName = sceneName;
                this.thumbnailBase64 = thumbnailBase64;
                this.createdAt = createdAt;
                this.savedAt = savedAt;
                this.lastOpenedAt = lastOpenedAt;
            }
    
            public static SaveSlotData empty(int slotIndex) {
                return new SaveSlotData(
                        slotIndex,
                        false,
                        "",
                        "",
                        "",
                        0,
                        0,
                        0
                );
            }
    
            public int getSlotIndex() {
                return slotIndex;
            }
    
            public boolean exists() {
                return exists;
            }
    
            public String getSaveName() {
                return saveName;
            }
    
            public String getSceneName() {
                return sceneName;
            }
    
            public String getThumbnailBase64() {
                return thumbnailBase64;
            }
    
            public long getCreatedAt() {
                return createdAt;
            }
    
            public long getSavedAt() {
                return savedAt;
            }
    
            public long getLastOpenedAt() {
                return lastOpenedAt;
            }
        }
    }