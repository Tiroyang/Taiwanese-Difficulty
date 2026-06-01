package ass.example.system.save;

import ass.example.core.DeathReason;
import ass.example.core.QuestType;
import ass.example.core.SaveKey;
import ass.example.system.SaveSystem;
import com.almasb.fxgl.core.serialization.Bundle;
import javafx.scene.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SaveSlotManager {

    private static final SaveSlotManager INSTANCE = new SaveSlotManager();

    public static SaveSlotManager getInstance() {
        return INSTANCE;
    }

    public static final int MAX_SLOTS = 6;

    private final Path saveFolder = Path.of(
            System.getProperty("user.home"),
            ".taiwanese_difficulty",
            "saves"
    );

    private int currentSlotIndex = -1;
    private String lastLoadedHash = "";

    private SaveSlotManager() {
        try {
            Files.createDirectories(saveFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SaveSlotData> getSlots() {
        List<SaveSlotData> result = new ArrayList<>();

        for (int i = 0; i < MAX_SLOTS; i++) {
            result.add(readSlot(i));
        }

        return result;
    }

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
                    props.getProperty("meta.saveName", "Save " + (slotIndex + 1)),
                    props.getProperty("bundle.sceneType", "UNKNOWN"),
                    props.getProperty("bundle.thumbnailBase64", ""),
                    longValue(props, "meta.createdAt"),
                    longValue(props, "meta.savedAt"),
                    longValue(props, "meta.lastOpenedAt")
            );

        } catch (Exception e) {
            e.printStackTrace();
            return SaveSlotData.empty(slotIndex);
        }
    }

    public int quickSave(SaveSystem saveSystem) {
        return quickSave(saveSystem, null);
    }

    public int quickSave(SaveSystem saveSystem, Node nodeToHideBeforeScreenshot) {
        if (currentSlotIndex >= 0 && readSlot(currentSlotIndex).exists()) {
            saveToSlot(
                    currentSlotIndex,
                    readSlot(currentSlotIndex).getSaveName(),
                    saveSystem,
                    true,
                    nodeToHideBeforeScreenshot
            );
            return currentSlotIndex;
        }

        int emptySlot = findFirstEmptySlot();

        if (emptySlot == -1) {
            emptySlot = 0;
        }

        String name = "Save " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"));

        saveToSlot(
                emptySlot,
                name,
                saveSystem,
                true,
                nodeToHideBeforeScreenshot
        );

        return emptySlot;
    }

    public void saveToSlot(
            int slotIndex,
            String saveName,
            SaveSystem saveSystem,
            boolean overwrite
    ) {
        saveToSlot(slotIndex, saveName, saveSystem, overwrite, null);
    }

    public void saveToSlot(
            int slotIndex,
            String saveName,
            SaveSystem saveSystem,
            boolean overwrite,
            Node nodeToHideBeforeScreenshot
    ) {
        SaveSlotData oldData = readSlot(slotIndex);

        if (oldData.exists() && !overwrite) {
            return;
        }

        Bundle bundle = saveSystem.createSaveBundle(nodeToHideBeforeScreenshot);

        long now = System.currentTimeMillis();
        long createdAt = oldData.exists() ? oldData.getCreatedAt() : now;

        Properties props = new Properties();

        props.setProperty("meta.slotIndex", String.valueOf(slotIndex));
        props.setProperty("meta.saveName", saveName == null || saveName.isBlank()
                ? "Save " + (slotIndex + 1)
                : saveName);
        props.setProperty("meta.createdAt", String.valueOf(createdAt));
        props.setProperty("meta.savedAt", String.valueOf(now));
        props.setProperty("meta.lastOpenedAt", String.valueOf(now));

        writeBundleToProperties(bundle, props);

        try {
            Files.createDirectories(saveFolder);
            props.store(
                    Files.newOutputStream(getSlotPath(slotIndex)),
                    "Taiwanese Difficulty Save Slot " + slotIndex
            );

            currentSlotIndex = slotIndex;
            lastLoadedHash = computeContentHash(bundle);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSlot(int slotIndex, SaveSystem saveSystem) {
        Path path = getSlotPath(slotIndex);

        if (!Files.exists(path)) {
            return;
        }

        try {
            Properties props = loadProperties(path);

            props.setProperty("meta.lastOpenedAt", String.valueOf(System.currentTimeMillis()));
            props.store(
                    Files.newOutputStream(path),
                    "Taiwanese Difficulty Save Slot " + slotIndex
            );

            Bundle bundle = readBundleFromProperties(props);

            saveSystem.loadFromBundle(bundle);

            currentSlotIndex = slotIndex;
            lastLoadedHash = computeContentHash(bundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteSlot(int slotIndex) {
        try {
            Files.deleteIfExists(getSlotPath(slotIndex));

            if (currentSlotIndex == slotIndex) {
                currentSlotIndex = -1;
                lastLoadedHash = "";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void renameSlot(int slotIndex, String newName) {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }

    private int findFirstEmptySlot() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (!readSlot(i).exists()) {
                return i;
            }
        }

        return -1;
    }

    private Path getSlotPath(int slotIndex) {
        return saveFolder.resolve("slot_" + slotIndex + ".properties");
    }

    private Properties loadProperties(Path path) throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(path));
        return props;
    }

    private long longValue(Properties props, String key) {
        try {
            return Long.parseLong(props.getProperty(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeBundleToProperties(Bundle bundle, Properties props) {
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

        for (DeathReason reason : DeathReason.values()) {
            String key = "death_" + reason.name();
            putBool(props, bundle, key, "bundle." + key);
        }

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

    private Bundle readBundleFromProperties(Properties props) {
        Bundle bundle = new Bundle(SaveKey.BUNDLE_NAME);

        bundle.put(SaveKey.SCENE_TYPE, props.getProperty("bundle.sceneType", "HOUSE"));

        bundle.put(SaveKey.PLAYER_X, doubleValue(props, "bundle.playerX"));
        bundle.put(SaveKey.PLAYER_Y, doubleValue(props, "bundle.playerY"));

        bundle.put(SaveKey.DEATH_COUNT, intValue(props, "bundle.deathCount"));

        bundle.put(SaveKey.QUILT_FOLDED, boolValue(props, "bundle.quiltFolded"));
        bundle.put(SaveKey.WATER_DRUNK, boolValue(props, "bundle.waterDrunk"));
        bundle.put(SaveKey.TEETH_BRUSHED, boolValue(props, "bundle.teethBrushed"));
        bundle.put(SaveKey.SHOES_WORN, boolValue(props, "bundle.shoesWorn"));
        bundle.put(SaveKey.PLAYER_ON_BED_COLLIDER, boolValue(props, "bundle.playerOnBedCollider"));

        bundle.put(SaveKey.ROOM_LIVING_ROOM_REVEALED, boolValue(props, "bundle.roomLivingRoomRevealed"));
        bundle.put(SaveKey.ROOM_TOILET_REVEALED, boolValue(props, "bundle.roomToiletRevealed"));

        bundle.put(SaveKey.DOOR_1_OPENED, boolValue(props, "bundle.door1Opened"));
        bundle.put(SaveKey.DOOR_2_OPENED, boolValue(props, "bundle.door2Opened"));

        bundle.put(SaveKey.STREET_SEGMENTS, props.getProperty("bundle.streetSegments", ""));
        bundle.put(SaveKey.STREET_OBSTACLES, props.getProperty("bundle.streetObstacles", ""));

        bundle.put(SaveKey.PLAYER_DEAD, boolValue(props, "bundle.playerDead"));
        bundle.put(SaveKey.LAST_DEATH_REASON, props.getProperty("bundle.lastDeathReason", ""));

        bundle.put(SaveKey.SAVED_AT, longValue(props, "bundle.savedAt"));
        bundle.put(SaveKey.THUMBNAIL_BASE64, props.getProperty("bundle.thumbnailBase64", ""));

        for (DeathReason reason : DeathReason.values()) {
            String key = "death_" + reason.name();
            bundle.put(key, boolValue(props, "bundle." + key));
        }

        bundle.put(
                SaveKey.QUEST_VISIBLE_START_INDEX,
                intValue(props, "bundle." + SaveKey.QUEST_VISIBLE_START_INDEX)
        );

        for (QuestType quest : QuestType.values()) {
            String id = quest.name();

            bundle.put(
                    SaveKey.QUEST_AMOUNT_PREFIX + id,
                    intValue(props, "bundle." + SaveKey.QUEST_AMOUNT_PREFIX + id)
            );

            bundle.put(
                    SaveKey.QUEST_COMPLETED_PREFIX + id,
                    boolValue(props, "bundle." + SaveKey.QUEST_COMPLETED_PREFIX + id)
            );

            bundle.put(
                    SaveKey.QUEST_ANIM_PLAYED_PREFIX + id,
                    boolValue(props, "bundle." + SaveKey.QUEST_ANIM_PLAYED_PREFIX + id)
            );
        }

        return bundle;
    }

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

        StringBuilder sb = new StringBuilder();

        for (String key : keys) {
            sb.append(key).append("=").append(props.getProperty(key)).append("\n");
        }

        return Integer.toHexString(sb.toString().hashCode());
    }

    private double doubleValue(Properties props, String key) {
        try {
            return Double.parseDouble(props.getProperty(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    private int intValue(Properties props, String key) {
        try {
            return Integer.parseInt(props.getProperty(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean boolValue(Properties props, String key) {
        return Boolean.parseBoolean(props.getProperty(key, "false"));
    }

    private void putString(Properties props, Bundle bundle, String bundleKey, String propKey) {
        try {
            String value = bundle.get(bundleKey);
            props.setProperty(propKey, value == null ? "" : value);
        } catch (Exception ignored) {
        }
    }

    private void putDouble(Properties props, Bundle bundle, String bundleKey, String propKey) {
        try {
            double value = bundle.get(bundleKey);
            props.setProperty(propKey, String.valueOf(value));
        } catch (Exception ignored) {
        }
    }

    private void putInt(Properties props, Bundle bundle, String bundleKey, String propKey) {
        try {
            int value = bundle.get(bundleKey);
            props.setProperty(propKey, String.valueOf(value));
        } catch (Exception ignored) {
        }
    }

    private void putLong(Properties props, Bundle bundle, String bundleKey, String propKey) {
        try {
            long value = bundle.get(bundleKey);
            props.setProperty(propKey, String.valueOf(value));
        } catch (Exception ignored) {
        }
    }

    private void putBool(Properties props, Bundle bundle, String bundleKey, String propKey) {
        try {
            boolean value = bundle.get(bundleKey);
            props.setProperty(propKey, String.valueOf(value));
        } catch (Exception ignored) {
        }
    }
}