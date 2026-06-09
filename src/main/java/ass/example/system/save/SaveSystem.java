package ass.example.system.save;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.QuestType;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.scenes.system.SceneManager;
import ass.example.system.QuestSystem;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * SaveSystem
 *
 * 遊戲存讀檔資料轉換系統。
 *
 * 功能：
 * 1. 將目前遊戲狀態建立成 Bundle。
 * 2. 從 Bundle 還原遊戲狀態。
 * 3. 儲存 / 還原玩家座標。
 * 4. 儲存 / 還原 game vars。
 * 5. 儲存 / 還原死亡成就。
 * 6. 儲存 / 還原任務進度。
 * 7. 儲存 / 還原目前場景額外資料。
 * 8. 擷取縮圖並轉成 Base64。
 * 9. 從 Base64 還原縮圖 Image。
 */
public final class SaveSystem {
 
    // Singleton 

    private static SaveSystem INSTANCE;

    public static void init(SaveSystem saveSystem) {
        INSTANCE = saveSystem;
    }

    public static SaveSystem getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SaveSystem has not been initialized.");
        }

        return INSTANCE;
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }

 
    // Screenshot Settings 

    private static final int THUMBNAIL_WIDTH = 1280;
    private static final int THUMBNAIL_HEIGHT = 720;

    private static final String THUMBNAIL_FORMAT = "png";

 
    // Dependencies 

    /**
     * 場景管理器。
     */
    private final SceneManager sceneManager;

 
    // Constructor 

    public SaveSystem(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

 
    // Create Save Bundle 

    public Bundle createSaveBundle() {
        return createSaveBundle(null);
    }

    /**
     * 建立存檔 Bundle。
     *
     * @param nodeToHideBeforeScreenshot 截圖前要暫時隱藏的 UI，可為 null
     * @return 存檔 Bundle
     */
    public Bundle createSaveBundle(Node nodeToHideBeforeScreenshot) {
        Bundle bundle = new Bundle(SaveKey.BUNDLE_NAME);

        Entity player = sceneManager.getPlayer();

        if (player == null || sceneManager.getCurrentSceneType() == null) {
            return bundle;
        }

        saveSceneAndPlayer(bundle, player);
        saveGameVars(bundle);
        saveDeathAchievements(bundle);
        saveQuests(bundle);

        sceneManager.saveCurrentSceneExtraState(bundle);

        putBoolIfExists(bundle, SaveKey.PLAYER_DEAD);
        putStringIfExists(bundle, SaveKey.LAST_DEATH_REASON);

        bundle.put(
                SaveKey.THUMBNAIL_BASE64,
                captureThumbnail(nodeToHideBeforeScreenshot)
        );

        return bundle;
    }

    /**
     * 儲存目前場景與玩家基礎資料。
     */
    private void saveSceneAndPlayer(
            Bundle bundle,
            Entity player
    ) {
        bundle.put(
                SaveKey.SCENE_TYPE,
                sceneManager.getCurrentSceneType().name()
        );

        bundle.put(SaveKey.PLAYER_X, player.getX());
        bundle.put(SaveKey.PLAYER_Y, player.getY());

        bundle.put(SaveKey.SAVED_AT, System.currentTimeMillis());
    }

 
    // Load From Bundle 

    /**
     * 從 Bundle 還原遊戲。
     *
     * 流程：
     * 1. 清掉可能殘留的死亡畫面。
     * 2. 根據存檔 SceneType 載入場景。
     * 3. 重設場景 runtime systems。
     * 4. 還原 game vars / 成就 / 任務。
     * 5. 還原場景額外資料。
     * 6. 還原玩家位置。
     * 7. 套用場景物件外觀狀態。
     * 8. 最後依存檔決定是否恢復死亡畫面。
     */
    public void loadFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        sceneManager.clearDeathStateForLoad();

        SceneType sceneType = getSceneTypeFromBundle(bundle);

        sceneManager.loadSceneByTypeFromSave(sceneType);
        sceneManager.resetCurrentSceneRuntimeSystems();

        loadGameVars(bundle);
        loadDeathAchievements(bundle);
        loadQuests(bundle);

        sceneManager.loadCurrentSceneExtraState(bundle);

        restorePlayerPosition(bundle);

        sceneManager.applySavedState();

        restoreDeathStateIfNeeded(bundle);
    }

    /**
     * 從 Bundle 取得 SceneType。
     */
    private SceneType getSceneTypeFromBundle(Bundle bundle) {
        try {
            String sceneTypeName = bundle.get(SaveKey.SCENE_TYPE);
            return SceneType.valueOf(sceneTypeName);
        } catch (Exception exception) {
            return SceneType.HOUSE;
        }
    }

    /**
     * 還原玩家位置。
     */
    private void restorePlayerPosition(Bundle bundle) {
        double playerX = getDoubleFromBundle(bundle, SaveKey.PLAYER_X, 0);
        double playerY = getDoubleFromBundle(bundle, SaveKey.PLAYER_Y, 0);

        Entity player = sceneManager.getPlayer();

        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

        /*
         * 目前保留原本 +50 的邏輯。
         * 如果你之後修正存檔座標偏移問題，可以改成 playerX。
         */
        playerComponent.respawnAt(playerX + 50, playerY);
    }

 
    // Death State Restore 

    /**
     * 根據存檔內容恢復死亡狀態。
     *
     * 只有存檔本身是死亡狀態，才會恢復死亡畫面。
     * 否則清除死亡畫面。
     */
    private void restoreDeathStateIfNeeded(Bundle bundle) {
        boolean savedPlayerDead = getBoolFromBundle(
                bundle,
                SaveKey.PLAYER_DEAD,
                false
        );

        String reasonName = getStringFromBundle(
                bundle,
                SaveKey.LAST_DEATH_REASON,
                ""
        );

        if (!savedPlayerDead) {
            clearSavedDeathState();
            return;
        }

        if (reasonName == null || reasonName.isBlank()) {
            sceneManager.clearDeathStateForLoad();
            return;
        }

        try {
            DeathReason reason = DeathReason.valueOf(reasonName);
            sceneManager.restoreDeathFromSave(reason);

        } catch (Exception exception) {
            System.out.println("Invalid saved death reason: " + reasonName);
            sceneManager.clearDeathStateForLoad();
        }
    }

    /**
     * 清除存檔還原時的死亡狀態。
     */
    private void clearSavedDeathState() {
        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");

        sceneManager.clearDeathStateForLoad();
    }

 
    // Game Vars 
    // 新增欄位時也需要在此新增

    private void saveGameVars(Bundle bundle) {
        putIntIfExists(bundle, SaveKey.DEATH_COUNT);

        putBoolIfExists(bundle, SaveKey.QUILT_FOLDED);
        putBoolIfExists(bundle, SaveKey.WATER_DRUNK);
        putBoolIfExists(bundle, SaveKey.TEETH_BRUSHED);
        putBoolIfExists(bundle, SaveKey.SHOES_WORN);
        putBoolIfExists(bundle, SaveKey.PLAYER_ON_BED_COLLIDER);

        putBoolIfExists(bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED);
        putBoolIfExists(bundle, SaveKey.ROOM_TOILET_REVEALED);

        putBoolIfExists(bundle, SaveKey.DOOR_1_OPENED);
        putBoolIfExists(bundle, SaveKey.DOOR_2_OPENED);
    }

    private void loadGameVars(Bundle bundle) {
        setIntIfExists(bundle, SaveKey.DEATH_COUNT);

        setBoolIfExists(bundle, SaveKey.QUILT_FOLDED);
        setBoolIfExists(bundle, SaveKey.WATER_DRUNK);
        setBoolIfExists(bundle, SaveKey.TEETH_BRUSHED);
        setBoolIfExists(bundle, SaveKey.SHOES_WORN);
        setBoolIfExists(bundle, SaveKey.PLAYER_ON_BED_COLLIDER);

        setBoolIfExists(bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED);
        setBoolIfExists(bundle, SaveKey.ROOM_TOILET_REVEALED);

        setBoolIfExists(bundle, SaveKey.DOOR_1_OPENED);
        setBoolIfExists(bundle, SaveKey.DOOR_2_OPENED);

        setBoolIfExists(bundle, SaveKey.PLAYER_DEAD);
        setStringIfExists(bundle, SaveKey.LAST_DEATH_REASON);
    }

 
    // Death Achievements 

    private void saveDeathAchievements(Bundle bundle) {
        for (DeathReason reason : DeathReason.values()) {
            String key = "death_" + reason.name();
            putBoolIfExists(bundle, key);
        }
    }

    private void loadDeathAchievements(Bundle bundle) {
        for (DeathReason reason : DeathReason.values()) {
            String key = "death_" + reason.name();
            setBoolIfExists(bundle, key);
        }
    }

 
    // Quests 

    private void saveQuests(Bundle bundle) {
        QuestSystem questSystem = QuestSystem.getInstance();

        bundle.put(
                SaveKey.QUEST_VISIBLE_START_INDEX,
                questSystem.getVisibleStartIndex()
        );

        for (QuestType quest : questSystem.getStoryQuests()) {
            QuestSystem.QuestState state = questSystem.getState(quest);

            if (state == null) {
                continue;
            }

            String id = quest.name();

            bundle.put(SaveKey.QUEST_AMOUNT_PREFIX + id, state.getAmount());
            bundle.put(SaveKey.QUEST_COMPLETED_PREFIX + id, state.isCompleted());
            bundle.put(SaveKey.QUEST_ANIM_PLAYED_PREFIX + id, state.isCompletionAnimationPlayed());
        }
    }

    private void loadQuests(Bundle bundle) {
        QuestSystem questSystem = QuestSystem.getInstance();

        questSystem.resetRuntimeState();

        questSystem.setVisibleStartIndex(
                getIntFromBundle(
                        bundle,
                        SaveKey.QUEST_VISIBLE_START_INDEX,
                        0
                )
        );

        for (QuestType quest : questSystem.getStoryQuests()) {
            loadSingleQuestState(bundle, questSystem, quest);
        }

        questSystem.advancePastCompletedQuests();
    }

    private void loadSingleQuestState(
            Bundle bundle,
            QuestSystem questSystem,
            QuestType quest
    ) {
        QuestSystem.QuestState state = questSystem.getState(quest);

        if (state == null) {
            return;
        }

        String id = quest.name();

        state.setAmount(
                getIntFromBundle(
                        bundle,
                        SaveKey.QUEST_AMOUNT_PREFIX + id,
                        0
                )
        );

        state.setCompleted(
                getBoolFromBundle(
                        bundle,
                        SaveKey.QUEST_COMPLETED_PREFIX + id,
                        false
                )
        );

        state.setCompletionAnimationPlayed(
                getBoolFromBundle(
                        bundle,
                        SaveKey.QUEST_ANIM_PLAYED_PREFIX + id,
                        false
                )
        );
    }

 
    // Thumbnail 

    /**
     * 擷取遊戲畫面並轉為 Base64。
     */
    private String captureThumbnail(Node nodeToHide) {
        boolean oldVisible = false;

        try {
            if (nodeToHide != null) {
                oldVisible = nodeToHide.isVisible();
                nodeToHide.setVisible(false);
            }

            javafx.scene.Node gameRoot = getGameScene().getRoot();

            gameRoot.applyCss();

            WritableImage image = new WritableImage(
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT
            );

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            params.setViewport(new Rectangle2D(
                    0,
                    0,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT
            ));

            gameRoot.snapshot(params, image);

            return imageToBase64(image);

        } catch (Exception exception) {
            System.out.println("Capture thumbnail failed.");
            exception.printStackTrace();
            return "";

        } finally {
            if (nodeToHide != null) {
                nodeToHide.setVisible(oldVisible);
            }
        }
    }

    /**
     * 將 WritableImage 轉成 Base64 PNG。
     */
    private String imageToBase64(WritableImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ImageIO.write(
                    SwingFXUtils.fromFXImage(image, null),
                    THUMBNAIL_FORMAT,
                    output
            );

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (Exception exception) {
            exception.printStackTrace();
            return "";
        }
    }

    /**
     * 將 Base64 還原成 Image。
     *
     * 目前使用 SaveSlotManager 中的方法，暫時備用。
     */
    public Image imageFromBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception exception) {
            return null;
        }
    }

 
    // Bundle Write Helpers 

    private void putBoolIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, getb(key));
        } catch (Exception ignored) {
        }
    }

    private void putIntIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, geti(key));
        } catch (Exception ignored) {
        }
    }

    private void putStringIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, gets(key));
        } catch (Exception ignored) {
        }
    }

 
    // Bundle Read Helpers 

    private void setBoolIfExists(Bundle bundle, String key) {
        try {
            set(key, (boolean) bundle.get(key));
        } catch (Exception ignored) {
        }
    }

    private void setIntIfExists(Bundle bundle, String key) {
        try {
            set(key, (int) bundle.get(key));
        } catch (Exception ignored) {
        }
    }

    private void setStringIfExists(Bundle bundle, String key) {
        try {
            set(key, (String) bundle.get(key));
        } catch (Exception ignored) {
        }
    }

    private boolean getBoolFromBundle(
            Bundle bundle,
            String key,
            boolean defaultValue
    ) {
        try {
            return bundle.get(key);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private int getIntFromBundle(
            Bundle bundle,
            String key,
            int defaultValue
    ) {
        try {
            return bundle.get(key);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private double getDoubleFromBundle(
            Bundle bundle,
            String key,
            double defaultValue
    ) {
        try {
            return bundle.get(key);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private String getStringFromBundle(
            Bundle bundle,
            String key,
            String defaultValue
    ) {
        try {
            String value = bundle.get(key);
            return value == null ? defaultValue : value;
        } catch (Exception exception) {
            return defaultValue;
        }
    }
}