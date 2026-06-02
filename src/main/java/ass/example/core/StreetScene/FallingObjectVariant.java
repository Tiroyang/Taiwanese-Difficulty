package ass.example.core.StreetScene;

import ass.example.core.DeathReason;

/**
 * FallingObjectVariant
 *
 * StreetScene 中從天而降的物件種類。
 *
 * 每個種類都定義：
 * 1. 對應死亡原因。
 * 2. 碰撞箱寬度。
 * 3. 碰撞箱高度。
 *
 * 用途：
 * FallingObject 生成時可以根據 variant 決定：
 * - 掉落物圖片。
 * - 掉落物碰撞箱大小。
 * - 玩家撞到後的 DeathReason。
 */
public enum FallingObjectVariant {

    /**
     * 冰箱掉落物。
     *
     * 寬扁型碰撞箱。
     */
    FRIDGE(
            DeathReason.FALLING_FRIDGE,
            92,
            52
    ),

    /**
     * 直升機掉落物。
     *
     * 接近正方形碰撞箱。
     */
    HELI(
            DeathReason.FALLING_HELI,
            58,
            58
    );


    // =========================================================
    // Death Settings
    // =========================================================

    /**
     * 此掉落物碰到玩家時造成的死亡原因。
     */
    private final DeathReason deathReason;


    // =========================================================
    // Collider Settings
    // =========================================================

    /**
     * 掉落物碰撞箱寬度。
     */
    private final double width;

    /**
     * 掉落物碰撞箱高度。
     */
    private final double height;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立掉落物種類資料。
     *
     * @param deathReason 掉落物造成的死亡原因
     * @param width 碰撞箱寬度
     * @param height 碰撞箱高度
     */
    FallingObjectVariant(
            DeathReason deathReason,
            double width,
            double height
    ) {
        this.deathReason = deathReason;
        this.width = width;
        this.height = height;
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得此掉落物造成的死亡原因。
     *
     * @return 死亡原因
     */
    public DeathReason getDeathReason() {
        return deathReason;
    }

    /**
     * 取得掉落物碰撞箱寬度。
     *
     * @return 碰撞箱寬度
     */
    public double getWidth() {
        return width;
    }

    /**
     * 取得掉落物碰撞箱高度。
     *
     * @return 碰撞箱高度
     */
    public double getHeight() {
        return height;
    }
}