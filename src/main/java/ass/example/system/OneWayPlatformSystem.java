package ass.example.system;

import ass.example.components.OneWayPlatformComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.EntityType;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 專門處理「普通單向平台」。
 *
 * 單向平台的規則：
 * 1. 玩家從下方往上跳時，可以穿過平台
 * 2. 玩家從上方落下時，可以站在平台上
 * 3. 玩家站上平台後，系統會生成一個實體 collider 讓玩家真正踩住
 * 4. 玩家跳起、離開平台、或按 Shift 下落時，移除這個實體 collider
 */
public class OneWayPlatformSystem {

    private final Entity player;

    private Entity currentPlatform;
    private Entity currentCollider;

    private double previousPlayerBottom;

    private double dropTimer = 0;
    private final double dropDuration = 0.28;

    private final double landingTolerance = 0;
    private final double sidePadding = 8;

    public OneWayPlatformSystem(Entity player) {
        this.player = player;
        this.previousPlayerBottom = getPlayerBottom();
    }

    public void update(double tpf) {
        if (getb("playerDead")) {
            return;
        }

        PlayerComponent playerComponent = getPlayerComponent();

        updateDropTimer(tpf);

        if (isDroppingThrough()) {
            playerComponent.setOnOneWayPlatform(false);
            previousPlayerBottom = getPlayerBottom();
            return;
        }

        if (currentPlatform != null) {
            if (isPlayerJumpingUpFromCurrentPlatform()) {
                leaveCurrentPlatform();
            } else if (isPlayerStillOnCurrentPlatform()) {
                playerComponent.setOnOneWayPlatform(true);
                previousPlayerBottom = getPlayerBottom();
                return;
            } else {
                leaveCurrentPlatform();
            }
        }

        findPlatformToLandOn()
                .ifPresentOrElse(
                        this::landOnPlatform,
                        () -> playerComponent.setOnOneWayPlatform(false)
                );

        previousPlayerBottom = getPlayerBottom();
    }

    private Optional<Entity> findPlatformToLandOn() {
        if (getPhysics().getVelocityY() < 0) {
            return Optional.empty();
        }

        return getGameWorld()
                .getEntitiesByType(EntityType.ONE_WAY_PLATFORM)
                .stream()
                .filter(this::canLandOn)
                .min(Comparator.comparingDouble(Entity::getY));
    }

    private boolean canLandOn(Entity platform) {
        OneWayPlatformComponent component =
                platform.getComponent(OneWayPlatformComponent.class);

        double platformTop = platform.getY();
        double platformLeft = platform.getX();
        double platformRight = platform.getX() + component.getWidth();

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();
        double playerBottom = getPlayerBottom();

        boolean xOverlap =
                playerRight > platformLeft + sidePadding &&
                        playerLeft < platformRight - sidePadding;

        boolean crossedPlatformTop =
                previousPlayerBottom <= platformTop + landingTolerance &&
                        playerBottom >= platformTop - landingTolerance;

        return xOverlap && crossedPlatformTop;
    }

    private void landOnPlatform(Entity platform) {
        if (currentPlatform == platform && currentCollider != null) {
            return;
        }

        OneWayPlatformComponent platformComponent =
                platform.getComponent(OneWayPlatformComponent.class);

        removeCurrentSolid();

        currentPlatform = platform;
        currentCollider = createSolidForPlatform(platform, platformComponent);

        getPhysics().setVelocityY(0);
        getPlayerComponent().setOnOneWayPlatform(true);
    }

    private Entity createSolidForPlatform(
            Entity platform,
            OneWayPlatformComponent platformComponent
    ) {
        return spawn("one_way_platform_collider", new SpawnData(
                platform.getX(),
                platform.getY()
        )
                .put("width", platformComponent.getWidth())
                .put("height", platformComponent.getHeight()));
    }

    private boolean isPlayerStillOnCurrentPlatform() {
        if (currentPlatform == null) {
            return false;
        }

        OneWayPlatformComponent component =
                currentPlatform.getComponent(OneWayPlatformComponent.class);

        double platformTop = currentPlatform.getY();
        double platformLeft = currentPlatform.getX();
        double platformRight = currentPlatform.getX() + component.getWidth();

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();
        double playerBottom = getPlayerBottom();

        boolean xOverlap =
                playerRight > platformLeft + sidePadding &&
                        playerLeft < platformRight - sidePadding;

        boolean nearPlatformTop =
                Math.abs(playerBottom - platformTop) <= landingTolerance + 8;

        return xOverlap && nearPlatformTop;
    }

    public void onPlayerJumpPressed() {
        if (currentPlatform == null) {
            return;
        }

        leaveCurrentPlatform();
    }

    private boolean isPlayerJumpingUpFromCurrentPlatform() {
        if (currentPlatform == null) {
            return false;
        }

        return getPhysics().getVelocityY() < 0;
    }

    public void dropThrough() {
        if (currentPlatform == null) {
            return;
        }

        getPlayerComponent().setOnOneWayPlatform(false);
        leaveCurrentPlatform();

        dropTimer = dropDuration;

        getPhysics().setVelocityY(260);
    }

    private void updateDropTimer(double tpf) {
        if (dropTimer <= 0) {
            return;
        }

        dropTimer -= tpf;

        if (dropTimer < 0) {
            dropTimer = 0;
        }
    }

    private boolean isDroppingThrough() {
        return dropTimer > 0;
    }

    private void leaveCurrentPlatform() {
        getPlayerComponent().setOnOneWayPlatform(false);

        currentPlatform = null;
        removeCurrentSolid();
    }

    private void removeCurrentSolid() {
        if (currentCollider != null) {
            currentCollider.removeFromWorld();
            currentCollider = null;
        }
    }

    public void reset() {
        removeCurrentSolid();

        getGameWorld()
                .getEntitiesByType(EntityType.ONE_WAY_PLATFORM_COLLIDER)
                .forEach(Entity::removeFromWorld);

        currentPlatform = null;
        currentCollider = null;

        dropTimer = 0;
        previousPlayerBottom = getPlayerBottom();

        getPlayerComponent().setOnOneWayPlatform(false);
    }

    private double getPlayerBottom() {
        return player.getBoundingBoxComponent().getMaxYWorld();
    }

    private PlayerComponent getPlayerComponent() {
        if (player == null) {
            System.out.println("[OneWayPlatformSystem] player is null");
            return null;
        }

        if (!player.hasComponent(PlayerComponent.class)) {
            System.out.println("[OneWayPlatformSystem] player has no PlayerComponent: " + player);
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }

    private PhysicsComponent getPhysics() {
        return player.getComponent(PhysicsComponent.class);
    }
}