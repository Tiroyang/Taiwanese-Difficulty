package ass.example.components.HouseScene;

import ass.example.core.DeathReason;
import com.almasb.fxgl.entity.component.Component;

/**
 * BedComponent
 *
 * 床的資料型 Component。
 *
 * 這個 Component 不負責處理玩家跳床、死亡、ZIndex 切換等邏輯，而是保存床物件需要用到的設定資料。
 *
 * 主要用途：
 * 1. 標記目前 Entity 是床的平台區，還是床的碰撞區。
 * 2. 保存床的 ID，方便同一張床的平台與碰撞區互相對應。
 * 3. 保存平台尺寸。
 * 4. 保存床額外碰撞區的位置與尺寸。
 * 5. 保存玩家站在床上時的 ZIndex。
 * 6. 保存玩家離開床後恢復的 ZIndex。
 * 7. 保存第二次落在床上時要觸發的死亡原因。
 *
 * 一張床通常可能會拆成數個 Entity：
 *
 * 1. PLATFORM
 *    - 玩家可以站上去的一方通行平台。
 *
 * 2. COLLIDER
 *    - 床的側邊、床架、床頭等不能穿越的碰撞區。
 *
 * 這些 Entity 可以透過相同的 bedId 判定屬於同一張床。
 */
public class BedComponent extends Component {

    // =========================================================
    // Enums
    // =========================================================

    /**
     * 床 Entity 的角色。
     *
     * PLATFORM：
     * - 代表此 Entity 是床面平台。
     * - 通常會搭配 OneWayPlatform 或類似機制使用。
     *
     * COLLIDER：
     * - 代表此 Entity 是床的阻擋碰撞區。
     * - 例如床架、床頭、側邊障礙。
     */
    public enum Role {
        PLATFORM,
        COLLIDER
    }


    // =========================================================
    // Nested Data Classes
    // =========================================================

    /**
     * ColliderArea
     *
     * 用來描述一個矩形碰撞區的位置與尺寸。
     */
    public static class ColliderArea {

        /**
         * 碰撞區相對於床 Entity 的 X 偏移。
         */
        private final double offsetX;

        /**
         * 碰撞區相對於床 Entity 的 Y 偏移。
         */
        private final double offsetY;

        /**
         * 碰撞區寬度。
         */
        private final double width;

        /**
         * 碰撞區高度。
         */
        private final double height;

        /**
         * 建立一個矩形碰撞區資料。
         *
         * @param offsetX 相對 X 偏移
         * @param offsetY 相對 Y 偏移
         * @param width 碰撞區寬度
         * @param height 碰撞區高度
         */
        public ColliderArea(
                double offsetX,
                double offsetY,
                double width,
                double height
        ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.width = width;
            this.height = height;
        }

        /**
         * 判斷此碰撞區是否有效。
         *
         * 只要寬度或高度小於等於 0，
         * 就代表這個碰撞區不應該被生成。
         *
         * @return true 表示此碰撞區有效
         */
        public boolean isValid() {
            return width > 0 && height > 0;
        }

        public double getOffsetX() {
            return offsetX;
        }

        public double getOffsetY() {
            return offsetY;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }


    // =========================================================
    // Basic Bed Settings
    // =========================================================

    /**
     * 此 Entity 在床系統中的角色。
     */
    private final Role role;

    /**
     * 床 ID。
     *
     * 用來辨識不同 Entity 是否屬於同一張床。
     */
    private final String bedId;


    // =========================================================
    // Platform Settings
    // =========================================================

    /**
     * 床平台寬度。
     *
     * 通常用於生成一方通行平台的碰撞範圍。
     */
    private final double platformWidth;

    /**
     * 床平台高度。
     *
     * 通常用於生成一方通行平台的碰撞範圍。
     */
    private final double platformHeight;


    // =========================================================
    // Collider Settings
    // =========================================================

    /**
     * 第一組床碰撞區。
     *
     * 通常可用於床頭、床架或主要阻擋區。
     */
    private final ColliderArea firstColliderArea;

    /**
     * 第二組床碰撞區。
     *
     * 若床需要兩段阻擋區，可以使用此欄位。
     * 若不需要第二組碰撞區，width 或 height 可設為 0。
     */
    private final ColliderArea secondColliderArea;


    // =========================================================
    // Player Layer Settings
    // =========================================================

    /**
     * 玩家站在床上時的 ZIndex。
     *
     * 讓玩家在床上時可以顯示在正確圖層。
     */
    private final int playerZIndexOnBed;

    /**
     * 玩家離開床後恢復的普通 ZIndex。
     */
    private final int normalPlayerZIndex;


    // =========================================================
    // Death Settings
    // =========================================================

    /**
     * 玩家第二次落在床上時要觸發的死亡原因。
     */
    private final DeathReason deathReasonOnSecondLanding;


    // =========================================================
    // Constructors
    // =========================================================

    /**
     * 建立床 Component。
     *
     * @param role 此 Entity 的床角色
     * @param bedId 床 ID
     *
     * @param platformWidth 床平台寬度
     * @param platformHeight 床平台高度
     *
     * @param collider1OffsetX 第一組碰撞區 X 偏移
     * @param collider1OffsetY 第一組碰撞區 Y 偏移
     * @param collider1Width 第一組碰撞區寬度
     * @param collider1Height 第一組碰撞區高度
     *
     * @param collider2OffsetX 第二組碰撞區 X 偏移
     * @param collider2OffsetY 第二組碰撞區 Y 偏移
     * @param collider2Width 第二組碰撞區寬度
     * @param collider2Height 第二組碰撞區高度
     *
     * @param playerZIndexOnBed 玩家在床上時的 ZIndex
     * @param normalPlayerZIndex 玩家離開床後恢復的 ZIndex
     * @param deathReasonOnSecondLanding 第二次落床死亡原因
     */
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
        this(
                role,
                bedId,
                platformWidth,
                platformHeight,
                new ColliderArea(
                        collider1OffsetX,
                        collider1OffsetY,
                        collider1Width,
                        collider1Height
                ),
                new ColliderArea(
                        collider2OffsetX,
                        collider2OffsetY,
                        collider2Width,
                        collider2Height
                ),
                playerZIndexOnBed,
                normalPlayerZIndex,
                deathReasonOnSecondLanding
        );
    }

    /**
     * 建立床 Component。
     *
     * @param role 此 Entity 的床角色
     * @param bedId 床 ID
     * @param platformWidth 床平台寬度
     * @param platformHeight 床平台高度
     * @param firstColliderArea 第一組碰撞區
     * @param secondColliderArea 第二組碰撞區
     * @param playerZIndexOnBed 玩家在床上時的 ZIndex
     * @param normalPlayerZIndex 玩家離開床後恢復的 ZIndex
     * @param deathReasonOnSecondLanding 第二次落床死亡原因
     */
    public BedComponent(
            Role role,
            String bedId,
            double platformWidth,
            double platformHeight,
            ColliderArea firstColliderArea,
            ColliderArea secondColliderArea,
            int playerZIndexOnBed,
            int normalPlayerZIndex,
            DeathReason deathReasonOnSecondLanding
    ) {
        this.role = role;
        this.bedId = bedId;
        this.platformWidth = platformWidth;
        this.platformHeight = platformHeight;
        this.firstColliderArea = firstColliderArea;
        this.secondColliderArea = secondColliderArea;
        this.playerZIndexOnBed = playerZIndexOnBed;
        this.normalPlayerZIndex = normalPlayerZIndex;
        this.deathReasonOnSecondLanding = deathReasonOnSecondLanding;
    }


    // =========================================================
    // Role Checks
    // =========================================================

    /**
     * 判斷此 Entity 是否為床平台。
     *
     * @return true 表示此 Entity 是床平台
     */
    public boolean isPlatform() {
        return role == Role.PLATFORM;
    }

    /**
     * 判斷此 Entity 是否為床碰撞區。
     *
     * @return true 表示此 Entity 是床碰撞區
     */
    public boolean isCollider() {
        return role == Role.COLLIDER;
    }


    // =========================================================
    // Collider Checks
    // =========================================================

    /**
     * 判斷是否有有效的第一組碰撞區。
     *
     * @return true 表示第一組碰撞區有效
     */
    public boolean hasFirstColliderArea() {
        return firstColliderArea != null && firstColliderArea.isValid();
    }

    /**
     * 判斷是否有有效的第二組碰撞區。
     *
     * @return true 表示第二組碰撞區有效
     */
    public boolean hasSecondColliderArea() {
        return secondColliderArea != null && secondColliderArea.isValid();
    }


    // =========================================================
    // Basic Getters
    // =========================================================

    public Role getRole() {
        return role;
    }

    public String getBedId() {
        return bedId;
    }


    // =========================================================
    // Platform Getters
    // =========================================================

    public double getPlatformWidth() {
        return platformWidth;
    }

    public double getPlatformHeight() {
        return platformHeight;
    }


    // =========================================================
    // Collider Getters
    // =========================================================

    public ColliderArea getFirstColliderArea() {
        return firstColliderArea;
    }

    public ColliderArea getSecondColliderArea() {
        return secondColliderArea;
    }


    // =========================================================
    // Player Layer Getters
    // =========================================================

    public int getPlayerZIndexOnBed() {
        return playerZIndexOnBed;
    }

    public int getNormalPlayerZIndex() {
        return normalPlayerZIndex;
    }


    // =========================================================
    // Death Getters
    // =========================================================

    public DeathReason getDeathReasonOnSecondLanding() {
        return deathReasonOnSecondLanding;
    }
}