package ass.example;

import ass.example.components.LethalComponent;
import ass.example.core.DeathReasons;
import ass.example.core.EntityTypes;
import ass.example.factories.HouseFactory;
import ass.example.factories.PlayerFactory;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import ass.example.scenes.SceneManager;
import static com.almasb.fxgl.dsl.FXGL.*;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import javafx.scene.input.KeyCode;
import ass.example.components.PlayerComponent;
import javafx.scene.input.KeyEvent;
import java.util.Map;

public class Main extends GameApplication {

    private SceneManager sceneManager;
    private DeathSystem deathSystem;
    private AudioSystem audioSystem;

    private int jumpKeyHoldCount = 0;

    public static boolean devMode = true;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setTitle("Taiwanese Difficulty");
        settings.setVersion("0.1");

        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlayerFactory());
        getGameWorld().addEntityFactory(new HouseFactory());

        audioSystem = new AudioSystem();

        sceneManager = new SceneManager();

        deathSystem = new DeathSystem(sceneManager);

        sceneManager.setDeathSystem(deathSystem);
        sceneManager.setAudioSystem(audioSystem);

        sceneManager.loadHouseScene();
    }

    @Override
    protected void initPhysics() {

        getPhysicsWorld().setGravity(0, 1600);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityTypes.PLAYER_GROUND_SENSOR,
                EntityTypes.WALL
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
                EntityTypes.PLAYER,
                EntityTypes.DEATH_WALL
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
                EntityTypes.PLAYER_GROUND_SENSOR,
                EntityTypes.BED_ONE_WAY_PLATFORM_COLLIDER
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
        for (DeathReasons reason : DeathReasons.values()) {
            vars.put("death_" + reason.name(), false);
        }

        // House
        vars.put("quiltFolded", false);
        vars.put("playerOnBedCollider", false);
    }

    public DeathSystem getDeathSystem() {
        return deathSystem;
    }

    private PlayerComponent getPlayerComponent() {
        return sceneManager.getPlayer()
                .getComponent(PlayerComponent.class);
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

    @Override
    protected void initInput() {

        getInput().addAction(new UserAction("Move Left A") {
            @Override
            protected void onActionBegin() {
                getPlayerComponent().moveLeft();
            }

            @Override
            protected void onActionEnd() {
                getPlayerComponent().stopLeft();
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Move Left Arrow") {
            @Override
            protected void onActionBegin() {
                getPlayerComponent().moveLeft();
            }

            @Override
            protected void onActionEnd() {
                getPlayerComponent().stopLeft();
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("Move Right D") {
            @Override
            protected void onActionBegin() {
                getPlayerComponent().moveRight();
            }

            @Override
            protected void onActionEnd() {
                getPlayerComponent().stopRight();
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Move Right Arrow") {
            @Override
            protected void onActionBegin() {
                getPlayerComponent().moveRight();
            }

            @Override
            protected void onActionEnd() {
                getPlayerComponent().stopRight();
            }
        }, KeyCode.RIGHT);

        getInput().addAction(new UserAction("Jump Space") {
            @Override
            protected void onActionBegin() {
                pressJumpKey();
                sceneManager.onPlayerJumpPressed();
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
                sceneManager.onPlayerJumpPressed();
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
                sceneManager.onPlayerJumpPressed();
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("Drop S") {
            @Override
            protected void onActionBegin() {
                sceneManager.dropThroughOneWayPlatform();
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, KeyCode.S);

        getPrimaryStage().getScene().addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() == KeyCode.SHIFT) {

                        sceneManager.dropThroughOneWayPlatform();

                        e.consume();
                    }
                }
        );

        getInput().addAction(new UserAction("Interact F") {
            @Override
            protected void onActionBegin() {
                sceneManager.tryInteract();
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