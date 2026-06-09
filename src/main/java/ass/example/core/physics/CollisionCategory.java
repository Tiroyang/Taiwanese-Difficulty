package ass.example.core.physics;

/**
 * CollisionCategory
 *
 * Box2D / FXGL 碰撞分類常數。
 *
 * 用途：
 * 配合 FixtureFilterUtil.applyFilter(...)，
 * 控制不同物件之間是否要產生物理碰撞。
 *
 * ------------------------------------------------------------
 * 使用範例
 * ------------------------------------------------------------
 *
 * FixtureFilterUtil.applyFilter(
 *         fixtureDef,
 *         CollisionCategory.WALL,
 *         CollisionCategory.PLAYER
 * );
 *
 * 代表：
 * - 目前物件的碰撞分類是 WALL。
 * - 只與 PLAYER 發生碰撞。
 *
 * ------------------------------------------------------------
 * 位元設計
 * ------------------------------------------------------------
 *
 * 每個分類使用不同 bit：
 *
 * PLAYER         = 0x0001
 * FLOOR          = 0x0002
 * WALL           = 0x0004
 * FALLING_OBJECT = 0x0008
 *
 * 可以用 OR 組合多個碰撞對象：
 *
 * (short) (CollisionCategory.PLAYER | CollisionCategory.FALLING_OBJECT)
 */
public final class CollisionCategory {
 
    // Constructor 

    private CollisionCategory() {
    }

 
    // Collision Categories 

    /**
     * 玩家分類。
     */
    public static final short PLAYER = 0x0001;

    /**
     * 地板分類。
     */
    public static final short FLOOR = 0x0002;

    /**
     * 牆壁分類。
     */
    public static final short WALL = 0x0004;

    /**
     * 掉落物分類。
     */
    public static final short FALLING_OBJECT = 0x0008;

    /**
     * 所有分類。
     *
     * -1 在 bit mask 中代表所有位元皆為 1。
     */
    public static final short ALL = -1;
}