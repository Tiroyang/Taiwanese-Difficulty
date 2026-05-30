package ass.example;

import ass.example.components.LethalComponent;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.factories.CommonFactory;
import ass.example.factories.HouseFactory;
import ass.example.factories.PlayerFactory;
import ass.example.factories.StreetFactory;
import ass.example.system.*;
import ass.example.system.save.SaveSlotManager;
import ass.example.ui.MainMenu;
import ass.example.ui.PauseMenu;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import ass.example.scenes.SceneManager;
import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.profile.DataFile;
import com.almasb.fxgl.profile.SaveLoadHandler;
import javafx.scene.input.KeyCode;
import ass.example.components.PlayerComponent;
import javafx.scene.input.KeyEvent;
import java.util.Map;

public class Main extends GameApplication {

    private SceneManager sceneManager;
    private DeathSystem deathSystem;
    private AudioSystem audioSystem;
    private SaveSystem saveSystem;
    private AchievementSystem achievementSystem;

    private int jumpKeyHoldCount = 0;

    public static boolean devMode = false;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Taiwanese Difficulty");
        settings.setVersion("0.5");

        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);

        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(false);

        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new MainMenu();
            }

            @Override
            public FXGLMenu newGameMenu() {
                return new PauseMenu();
            }
        });
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlayerFactory());
        getGameWorld().addEntityFactory(new CommonFactory());
        getGameWorld().addEntityFactory(new HouseFactory());
        getGameWorld().addEntityFactory(new StreetFactory());

        audioSystem = AudioSystem.getInstance();
        achievementSystem = new AchievementSystem();

        sceneManager = new SceneManager();

        deathSystem = new DeathSystem(
                sceneManager,
                audioSystem,
                achievementSystem
        );
        DeathSystem.init(deathSystem);

        sceneManager.setDeathSystem(deathSystem);
        sceneManager.setAudioSystem(audioSystem);

        saveSystem = new SaveSystem(sceneManager);
        sceneManager.setSaveSystem(saveSystem);

        if (SaveRequestSystem.hasPendingLoadSlot()) {
            int slotIndex = SaveRequestSystem.consumePendingLoadSlot();
            SaveSlotManager.getInstance().loadSlot(slotIndex, saveSystem);
            return;
        }

        if (SceneManager.hasPendingStartScene()) {
            SceneType sceneType = SceneManager.consumePendingStartScene();
            sceneManager.loadSceneByTypeForNewGame(sceneType);
            return;
        }

        sceneManager.loadHouseSceneForNewGame();
    }

    @Override
    protected void onPreInit() {
        getSaveLoadService().addHandler(new SaveLoadHandler() {
            @Override
            public void onSave(DataFile dataFile) {
                Bundle bundle = saveSystem.createSaveBundle();
                dataFile.putBundle(bundle);
            }

            @Override
            public void onLoad(DataFile dataFile) {
                Bundle bundle = dataFile.getBundle(SaveKey.BUNDLE_NAME);
                saveSystem.loadFromBundle(bundle);
            }
        });
    }

    @Override
    protected void initPhysics() {

        getPhysicsWorld().setGravity(0, 1600);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityType.PLAYER_GROUND_SENSOR,
                EntityType.WALL
        ) {
            @Override
            protected void onCollisionBegin(Entity sensor, Entity wall) {
                Entity player = sceneManager.getPlayer();

                if (player == null) {
                    return;
                }

                player.getComponent(PlayerComponent.class).addGroundContact();
            }

            @Override
            protected void onCollisionEnd(Entity sensor, Entity wall) {
                Entity player = sceneManager.getPlayer();

                if (player == null) {
                    return;
                }

                player.getComponent(PlayerComponent.class).removeGroundContact();
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityType.PLAYER,
                EntityType.DEATH_ZONE
        ) {
            @Override
            protected void onCollisionBegin(Entity player, Entity deathSolid) {
                if (getb("playerDead")) {
                    return;
                }

                LethalComponent lethal =
                        deathSolid.getComponent(LethalComponent.class);

                deathSystem.die(lethal.getDeathReason());
            }
        });

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityType.PLAYER_GROUND_SENSOR,
                EntityType.BED_ONE_WAY_PLATFORM_COLLIDER
        ) {
            @Override
            protected void onCollisionBegin(Entity sensor, Entity bedCollider) {
                Entity player = sceneManager.getPlayer();

                if (player == null || getb("playerDead")) {
                    return;
                }

                player.getComponent(PlayerComponent.class).addGroundContact();
            }

            @Override
            protected void onCollisionEnd(Entity sensor, Entity bedCollider) {
                Entity player = sceneManager.getPlayer();

                if (player == null || getb("playerDead")) {
                    return;
                }

                player.getComponent(PlayerComponent.class).removeGroundContact();
            }
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("playerDead", false);
        vars.put("deathCount", 0);
        vars.put("lastDeathReason", "");

        for (DeathReason reason : DeathReason.values()) {
            vars.put("death_" + reason.name(), false);
        }

        // HouseScene
        vars.put("quiltFolded", false);
        vars.put("waterDrunk", false);
        vars.put("teethBrushed", false);
        vars.put("shoesWorn", false);
        vars.put("playerOnBedCollider", false);

        vars.put("room_LIVING_ROOM_revealed", false);
        vars.put("room_TOILET_revealed", false);

        vars.put("door_Door1_opened", false);
        vars.put("door_Door2_opened", false);

        // StreetEndlessScene
        vars.put("streetEndlessMode", false);
        vars.put("streetRunDistance", 0.0);
        vars.put("streetBestDistanceBeforeRun", 0.0);
        vars.put("streetBestDistance", 0.0);
        vars.put("streetNewRecord", false);
        vars.put("saveDisabled", false);
        vars.put("achievementDisabled", false);
    }

    public DeathSystem getDeathSystem() {
        return deathSystem;
    }

    private PlayerComponent getPlayerComponent() {
        if (sceneManager == null) {
            return null;
        }

        Entity player = sceneManager.getPlayer();

        if (player == null) {
            return null;
        }

        try {
            return player.getComponent(PlayerComponent.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void pressJumpKey() {
        jumpKeyHoldCount++;

        if (jumpKeyHoldCount == 1) {
            getPlayerComponent().jumpPressed();
        }
    }

    private void releaseJumpKey() {
        jumpKeyHoldCount--;

        if (jumpKeyHoldCount < 0) {
            jumpKeyHoldCount = 0;
        }

        if (jumpKeyHoldCount == 0) {
            getPlayerComponent().jumpReleased();
        }
    }

    private void withPlayerComponent(java.util.function.Consumer<PlayerComponent> action) {
        PlayerComponent pc = getPlayerComponent();

        if (pc == null) {
            return;
        }

        action.accept(pc);
    }

    @Override
    protected void initInput() {

        getInput().addAction(new UserAction("Move Left A") {
            @Override
            protected void onActionBegin() {
                withPlayerComponent(PlayerComponent::moveLeft);
            }

            @Override
            protected void onActionEnd() {
                withPlayerComponent(PlayerComponent::stopLeft);
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Move Left Arrow") {
            @Override
            protected void onActionBegin() {
                withPlayerComponent(PlayerComponent::moveLeft);
            }

            @Override
            protected void onActionEnd() {
                withPlayerComponent(PlayerComponent::stopLeft);
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("Move Right D") {
            @Override
            protected void onActionBegin() {
                withPlayerComponent(PlayerComponent::moveRight);
            }

            @Override
            protected void onActionEnd() {
                withPlayerComponent(PlayerComponent::stopRight);
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Move Right Arrow") {
            @Override
            protected void onActionBegin() {
                withPlayerComponent(PlayerComponent::moveRight);
            }

            @Override
            protected void onActionEnd() {
                withPlayerComponent(PlayerComponent::stopRight);
            }
        }, KeyCode.RIGHT);

        getInput().addAction(new UserAction("Jump Space") {
            @Override
            protected void onActionBegin() {
                pressJumpKey();

                if (sceneManager != null) {
                    sceneManager.onPlayerJumpPressed();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.SPACE);

        getInput().addAction(new UserAction("Jump W") {
            @Override
            protected void onActionBegin() {
                pressJumpKey();

                if (sceneManager != null) {
                    sceneManager.onPlayerJumpPressed();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Jump Arrow") {
            @Override
            protected void onActionBegin() {
                pressJumpKey();

                if (sceneManager != null) {
                    sceneManager.onPlayerJumpPressed();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("Drop S") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.dropThroughOneWayPlatform();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Drop Down") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.dropThroughOneWayPlatform();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.DOWN);

        getPrimaryStage().getScene().addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() == KeyCode.SHIFT) {
                        withPlayerComponent(PlayerComponent::dashPressed);
                        e.consume();
                    }
                }
        );

        getInput().addAction(new UserAction("Interact F") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.tryInteract();
                }
            }
        }, KeyCode.F);
    }

    @Override
    protected void onUpdate(double tpf) {
        sceneManager.onUpdate(tpf);
    }

    public static void main(String[] args) {
        launch(args);
    }
}