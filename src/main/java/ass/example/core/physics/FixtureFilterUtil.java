package ass.example.core.physics;

import com.almasb.fxgl.physics.box2d.dynamics.Filter;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;

/**
 * FixtureFilterUtil
 *
 * Box2D / FXGL FixtureDef 碰撞過濾工具。
 *
 * 功能：
 * 1. 將 categoryBits 套用到 FixtureDef。
 * 2. 將 maskBits 套用到 FixtureDef。
 * 3. 回傳同一個 FixtureDef，方便鏈式使用。
 *
 *   ----
 * 使用範例
 *   ----
 *
 * FixtureDef fixtureDef = new FixtureDef()
 *         .friction(0.8f)
 *         .restitution(0.1f);
 *
 * FixtureFilterUtil.applyFilter(
 *         fixtureDef,
 *         CollisionCategory.FLOOR,
 *         (short) (CollisionCategory.PLAYER | CollisionCategory.FALLING_OBJECT)
 * );
 *
 *   ----
 * categoryBits / maskBits 說明
 *   ----
 *
 * categoryBits：
 * - 代表「這個物件屬於哪一類」。
 *
 * maskBits：
 * - 代表「這個物件要跟哪些類別碰撞」。
 */
public final class FixtureFilterUtil {
 
    // Constructor 

    /**
     * 工具類別不允許建立實例。
     */
    private FixtureFilterUtil() {
    }

 
    // Public API 

    /**
     * 將 Box2D Filter 套用到 FixtureDef。
     *
     * @param fixtureDef 目標 FixtureDef
     * @param categoryBits 此物件的碰撞分類
     * @param maskBits 此物件會碰撞的對象分類
     * @return 已套用 Filter 的 FixtureDef
     */
    public static FixtureDef applyFilter(
            FixtureDef fixtureDef,
            short categoryBits,
            short maskBits
    ) {
        Filter filter = new Filter();

        filter.categoryBits = categoryBits;
        filter.maskBits = maskBits;

        fixtureDef.setFilter(filter);

        return fixtureDef;
    }
}