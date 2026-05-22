package ass.example.system;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.scenes.SceneManager;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
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

    public SaveSystem(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    /**
     * 寫入目前遊戲資料到 Bundle。
     */
    public Bundle createSaveBundle() {
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

        String thumbnailBase64 = captureThumbnailAsBase64(320, 180);
        bundle.put(SaveKey.THUMBNAIL_BASE64, thumbnailBase64);

        return bundle;
    }

    /**
     * 從 Bundle 還原遊戲。
     */
    public void loadFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        String sceneTypeName = bundle.get(SaveKey.SCENE_TYPE);
        SceneType sceneType = SceneType.valueOf(sceneTypeName);

        sceneManager.loadSceneByType(sceneType);

        loadGameVars(bundle);
        loadDeathAchievements(bundle);

        double playerX = bundle.get(SaveKey.PLAYER_X);
        double playerY = bundle.get(SaveKey.PLAYER_Y);

        Entity player = sceneManager.getPlayer();

        if (player != null) {
            player.getComponent(PlayerComponent.class)
                    .respawnAt(playerX, playerY);
        }

        sceneManager.applySavedState();
    }

    private void saveGameVars(Bundle bundle) {
        putBoolIfExists(bundle, SaveKey.DEATH_COUNT);

        putBoolIfExists(bundle, SaveKey.QUILT_FOLDED);
        putBoolIfExists(bundle, SaveKey.WATER_DRUNK);
        putBoolIfExists(bundle, SaveKey.PLAYER_ON_BED_COLLIDER);

        putBoolIfExists(bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED);
        putBoolIfExists(bundle, SaveKey.ROOM_TOILET_REVEALED);

        putBoolIfExists(bundle, SaveKey.DOOR_1_OPENED);
        putBoolIfExists(bundle, SaveKey.DOOR_2_OPENED);
    }

    private void loadGameVars(Bundle bundle) {
        setBoolIfExists(bundle, SaveKey.DEATH_COUNT);

        setBoolIfExists(bundle, SaveKey.QUILT_FOLDED);
        setBoolIfExists(bundle, SaveKey.WATER_DRUNK);
        setBoolIfExists(bundle, SaveKey.PLAYER_ON_BED_COLLIDER);

        setBoolIfExists(bundle, SaveKey.ROOM_LIVING_ROOM_REVEALED);
        setBoolIfExists(bundle, SaveKey.ROOM_TOILET_REVEALED);

        setBoolIfExists(bundle, SaveKey.DOOR_1_OPENED);
        setBoolIfExists(bundle, SaveKey.DOOR_2_OPENED);
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

    /**
     * 擷取遊戲畫面並轉為 Base64。
     */
    private String captureThumbnailAsBase64(int targetWidth, int targetHeight) {
        try {
            WritableImage snapshot = getGameScene()
                    .getRoot()
                    .snapshot(new SnapshotParameters(), null);

            BufferedImage original = SwingFXUtils.fromFXImage(snapshot, null);

            BufferedImage resized = new BufferedImage(
                    targetWidth,
                    targetHeight,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", output);

            return Base64.getEncoder().encodeToString(output.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to capture thumbnail", e);
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