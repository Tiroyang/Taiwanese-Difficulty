package ass.example.components.HouseScene;

import ass.example.core.DeathReason;
import com.almasb.fxgl.entity.component.Component;

public class BedComponent extends Component {

    public enum Role {
        PLATFORM,
        COLLIDER
    }

    private final Role role;
    private final String bedId;

    private final double platformWidth;
    private final double platformHeight;

    private final double collider1OffsetX;
    private final double collider1OffsetY;
    private final double collider1Width;
    private final double collider1Height;

    private final double collider2OffsetX;
    private final double collider2OffsetY;
    private final double collider2Width;
    private final double collider2Height;

    private final int playerZIndexOnBed;
    private final int normalPlayerZIndex;

    private final DeathReason deathReasonOnSecondLanding;

    public BedComponent(
            Role role,
            String bedId,
            double platformWidth,
            double platformHeight,

            double collider1OffsetX,
            double collider1OffsetY,
            double collider1Width,
            double collider1Height,

            double collider2OffsetX,
            double collider2OffsetY,
            double collider2Width,
            double collider2Height,

            int playerZIndexOnBed,
            int normalPlayerZIndex,
            DeathReason deathReasonOnSecondLanding
    ) {
        this.role = role;
        this.bedId = bedId;
        this.platformWidth = platformWidth;
        this.platformHeight = platformHeight;

        this.collider1OffsetX = collider1OffsetX;
        this.collider1OffsetY = collider1OffsetY;
        this.collider1Width = collider1Width;
        this.collider1Height = collider1Height;

        this.collider2OffsetX = collider2OffsetX;
        this.collider2OffsetY = collider2OffsetY;
        this.collider2Width = collider2Width;
        this.collider2Height = collider2Height;

        this.playerZIndexOnBed = playerZIndexOnBed;
        this.normalPlayerZIndex = normalPlayerZIndex;
        this.deathReasonOnSecondLanding = deathReasonOnSecondLanding;
    }

    public Role getRole() {
        return role;
    }

    public boolean isPlatform() {
        return role == Role.PLATFORM;
    }

    public boolean isCollider() {
        return role == Role.COLLIDER;
    }

    public String getBedId() {
        return bedId;
    }

    public double getPlatformWidth() {
        return platformWidth;
    }

    public double getPlatformHeight() {
        return platformHeight;
    }

    public double getCollider1OffsetX() {
        return collider1OffsetX;
    }

    public double getCollider1OffsetY() {
        return collider1OffsetY;
    }

    public double getCollider1Width() {
        return collider1Width;
    }

    public double getCollider1Height() {
        return collider1Height;
    }

    public double getCollider2OffsetX() {
        return collider2OffsetX;
    }

    public double getCollider2OffsetY() {
        return collider2OffsetY;
    }

    public double getCollider2Width() {
        return collider2Width;
    }

    public double getCollider2Height() {
        return collider2Height;
    }

    public boolean hasSecondCollider() {
        return collider2Width > 0 && collider2Height > 0;
    }

    public int getPlayerZIndexOnBed() {
        return playerZIndexOnBed;
    }

    public int getNormalPlayerZIndex() {
        return normalPlayerZIndex;
    }

    public DeathReason getDeathReasonOnSecondLanding() {
        return deathReasonOnSecondLanding;
    }
}