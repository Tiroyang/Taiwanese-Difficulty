package ass.example.system;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.QuestType;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.scenes.SceneManager;
import ass.example.system.quest.QuestState;
import ass.example.system.quest.QuestSystem;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static com.almasb.fxgl.dsl.FXGL.*;

public class SaveSystem {

    private final SceneManager sceneManager;

    private static SaveSystem instance;

    public static SaveSystem getInstance() {
        return instance;
    }

    public SaveSystem(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        instance = this;
    }

    public Bundle createSaveBundle() {
        return createSaveBundle(null);
    }

    public Bundle createSaveBundle(Node nodeToHideBeforeScreenshot) {
        Bundle bundle = new Bundle(SaveKey.BUNDLE_NAME);

        Entity player = sceneManager.getPlayer();

        if (player == null || sceneManager.getCurrentSceneType() == null) {
            return bundle;
        }

        bundle.put(SaveKey.SCENE_TYPE, sceneManager.getCurrentSceneType().name());

        bundle.put(SaveKey.PLAYER_X, player.getX());
        bundle.put(SaveKey.PLAYER_Y, player.getY());

        bundle.put(SaveKey.SAVED_AT, System.currentTimeMillis());

        saveGameVars(bundle);
        saveDeathAchievements(bundle);
        saveQuests(bundle);

        putBoolIfExists(bundle, SaveKey.PLAYER_DEAD);
        putStringIfExists(bundle, SaveKey.LAST_DEATH_REASON);

        bundle.put(
                SaveKey.THUMBNAIL_BASE64,
                captureThumbnail(nodeToHideBeforeScreenshot)
        );

        return bundle;
    }

    private void restoreDeathStateIfNeeded(Bundle bundle) {
        boolean savedPlayerDead = getBoolFromBundle(bundle, SaveKey.PLAYER_DEAD, false);
        String reasonName = getStringFromBundle(bundle, SaveKey.LAST_DEATH_REASON, "");

        /*
         * 重點：
         * 只有存檔本身就是死亡狀態，才恢復死亡畫面。
         * 否則一律清掉死亡畫面與死亡狀態。
         */
        if (!savedPlayerDead) {
            set(SaveKey.PLAYER_DEAD, false);
            set(SaveKey.LAST_DEATH_REASON, "");

            sceneManager.clearDeathStateForLoad();
            return;
        }

        if (reasonName == null || reasonName.isBlank()) {
            sceneManager.clearDeathStateForLoad();
            return;
        }

        try {
            DeathReason reason = DeathReason.valueOf(reasonName);
            sceneManager.restoreDeathFromSave(reason);

        } catch (Exception e) {
            System.out.println("Invalid saved death reason: " + reasonName);
            sceneManager.clearDeathStateForLoad();
        }
    }

    private boolean getBoolFromBundle(Bundle bundle, String key, boolean defaultValue) {
        try {
            return bundle.get(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getStringFromBundle(Bundle bundle, String key, String defaultValue) {
        try {
            String value = bundle.get(key);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 從 Bundle 還原遊戲。
     */
    public void loadFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        /*
         * 先清掉目前可能殘留的死亡畫面。
         * 尤其是從死亡畫面讀檔時。
         */
        sceneManager.clearDeathStateForLoad();

        String sceneTypeName = bundle.get(SaveKey.SCENE_TYPE);
        SceneType sceneType = SceneType.valueOf(sceneTypeName);

        sceneManager.loadSceneByType(sceneType);

        sceneManager.resetCurrentSceneRuntimeSystems();

        loadGameVars(bundle);
        loadDeathAchievements(bundle);
        loadQuests(bundle);

        double playerX = bundle.get(SaveKey.PLAYER_X);
        double playerY = bundle.get(SaveKey.PLAYER_Y);

        Entity player = sceneManager.getPlayer();

        if (player != null) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.respawnAt(playerX, playerY);
        }

        sceneManager.applySavedState();

        /*
         * 最後根據存檔內容決定是否恢復死亡畫面。
         * 如果存檔不是死亡狀態，這裡會再次確保 DeathScreen 被關掉。
         */
        restoreDeathStateIfNeeded(bundle);
    }

    private void saveGameVars(Bundle bundle) {
        putIntIfExists(bundle, SaveKey.DEATH_COUNT);

        putBoolIfExists(bundle, SaveKey.QUILT_FOLDED);
        putBoolIfExists(bundle, SaveKey.WATER_DRUNK);
        putBoolIfExists(bundle, SaveKey.TEETH_BRUSHED);
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
        setBoolIfExists(bundle, SaveKey.PLAYER_ON_BED_COLLIDER);

        setBoolIfExists(bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED);
        setBoolIfExists(bundle, SaveKey.ROOM_TOILET_REVEALED);

        setBoolIfExists(bundle, SaveKey.DOOR_1_OPENED);
        setBoolIfExists(bundle, SaveKey.DOOR_2_OPENED);

        setBoolIfExists(bundle, SaveKey.PLAYER_DEAD);
        setStringIfExists(bundle, SaveKey.LAST_DEATH_REASON);
    }

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

    private void saveQuests(Bundle bundle) {
        QuestSystem questSystem = QuestSystem.getInstance();

        bundle.put(SaveKey.QUEST_VISIBLE_START_INDEX, questSystem.getVisibleStartIndex());

        for (QuestType quest : questSystem.getStoryQuests()) {
            QuestState state = questSystem.getState(quest);

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

        try {
            int visibleStartIndex = bundle.get(SaveKey.QUEST_VISIBLE_START_INDEX);
            questSystem.setVisibleStartIndex(visibleStartIndex);
        } catch (Exception ignored) {
        }

        for (QuestType quest : questSystem.getStoryQuests()) {
            QuestState state = questSystem.getState(quest);

            if (state == null) {
                continue;
            }

            String id = quest.name();

            try {
                int amount = bundle.get(SaveKey.QUEST_AMOUNT_PREFIX + id);
                state.setAmount(amount);
            } catch (Exception ignored) {
            }

            try {
                boolean completed = bundle.get(SaveKey.QUEST_COMPLETED_PREFIX + id);
                state.setCompleted(completed);
            } catch (Exception ignored) {
            }

            try {
                boolean animationPlayed = bundle.get(SaveKey.QUEST_ANIM_PLAYED_PREFIX + id);
                state.setCompletionAnimationPlayed(animationPlayed);
            } catch (Exception ignored) {
            }
        }

        /*
         * 讀檔後整理一次。
         * 如果前面的任務已經完成且動畫也播過，
         * 就直接推到下一個任務。
         */
        questSystem.advancePastCompletedQuests();
    }

    private void putBoolIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, getb(key));
        } catch (Exception ignored) {
        }
    }

    private void setBoolIfExists(Bundle bundle, String key) {
        try {
            boolean value = bundle.get(key);
            set(key, value);
        } catch (Exception ignored) {
        }
    }

    private void putIntIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, geti(key));
        } catch (Exception ignored) {
        }
    }

    private void setIntIfExists(Bundle bundle, String key) {
        try {
            int value = bundle.get(key);
            set(key, value);
        } catch (Exception ignored) {
        }
    }

    private void putStringIfExists(Bundle bundle, String key) {
        try {
            bundle.put(key, gets(key));
        } catch (Exception ignored) {
        }
    }

    private void setStringIfExists(Bundle bundle, String key) {
        try {
            String value = bundle.get(key);
            set(key, value);
        } catch (Exception ignored) {
        }
    }

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

            int width = 1280;
            int height = 720;

            javafx.scene.Node gameRoot = getGameScene().getRoot();

            gameRoot.applyCss();

            WritableImage image = new WritableImage(width, height);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            params.setViewport(new javafx.geometry.Rectangle2D(
                    0,
                    0,
                    width,
                    height
            ));

            gameRoot.snapshot(params, image);

            return imageToBase64(image);

        } catch (Exception e) {
            System.out.println("Capture thumbnail failed.");
            e.printStackTrace();
            return "";

        } finally {
            if (nodeToHide != null) {
                nodeToHide.setVisible(oldVisible);
            }
        }
    }

    private String imageToBase64(WritableImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ImageIO.write(
                    SwingFXUtils.fromFXImage(image, null),
                    "png",
                    output
            );

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 主選單顯示縮圖用。
     */
    public Image imageFromBase64(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new Image(new ByteArrayInputStream(bytes));
    }
}