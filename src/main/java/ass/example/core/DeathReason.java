package ass.example.core;

import ass.example.system.LanguageSystem;

/**
 * DeathReason
 *
 * 遊戲中的死亡原因列表。
 *
 * 每一個 DeathReason 都包含：
 * 1. id：
 *    - 程式內部識別用。
 *    - 可用於存檔、統計、成就、死亡紀錄。
 *
 * 2. titleKey：
 *    - 死亡畫面標題的語言 key。
 *
 * 3. subtitleKey：
 *    - 死亡畫面吐槽文字 / 副標題的語言 key。
 *
 * 4. iconPath：
 *    - 死亡原因對應的圖示路徑。
 *    - 目前可為 null。
 *
 * 注意：
 * enum 名稱與 id 建議保持一致，
 * 避免存檔、死亡統計或成就系統混淆。
 */
public enum DeathReason {

    // =========================================================
    // HouseScene - Bedroom
    // =========================================================

    /**
     * 離開臥室時沒有折棉被。
     */
    LEFT_BEDROOM_WITHOUT_FOLDING_QUILT(
            "LEFT_BEDROOM_WITHOUT_FOLDING_QUILT",
            "death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.title",
            "death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.subtitle",
            null
    ),

    /**
     * 撞到天花板。
     */
    HIT_CEILING(
            "HIT_CEILING",
            "death.HIT_CEILING.title",
            "death.HIT_CEILING.subtitle",
            null
    ),

    /**
     * 在床上跳太多次。
     */
    JUMPING_ON_BED(
            "JUMPING_ON_BED",
            "death.JUMPING_ON_BED.title",
            "death.JUMPING_ON_BED.subtitle",
            null
    ),


    // =========================================================
    // HouseScene - Toilet
    // =========================================================

    /**
     * 撞到浴簾桿。
     */
    HIT_SHOWER_CURTAIN_ROD(
            "HIT_SHOWER_CURTAIN_ROD",
            "death.HIT_SHOWER_CURTAIN_ROD.title",
            "death.HIT_SHOWER_CURTAIN_ROD.subtitle",
            null
    ),

    /**
     * 跳進浴缸。
     */
    JUMPED_IN_BATHTUB(
            "JUMPED_IN_BATHTUB",
            "death.JUMPED_IN_BATHTUB.title",
            "death.JUMPED_IN_BATHTUB.subtitle",
            null
    ),

    /**
     * 喝水後死亡。
     */
    DRINK_WATER(
            "DRINK_WATER",
            "death.DRINK_WATER.title",
            "death.DRINK_WATER.subtitle",
            null
    ),

    /**
     * 離開家前沒有刷牙。
     */
    LEFT_WITHOUT_BRUSHING_TEETH(
            "LEFT_WITHOUT_BRUSHING_TEETH",
            "death.LEFT_WITHOUT_BRUSHING_TEETH.title",
            "death.LEFT_WITHOUT_BRUSHING_TEETH.subtitle",
            null
    ),


    // =========================================================
    // HouseScene - Doors / Rooms
    // =========================================================

    /**
     * 撞到門框。
     */
    HIT_DOORFRAME(
            "HIT_DOORFRAME",
            "death.HIT_DOORFRAME.title",
            "death.HIT_DOORFRAME.subtitle",
            null
    ),

    /**
     * 穿鞋進入客廳。
     */
    ENTER_LIVING_ROOM_WITH_SHOES(
            "ENTER_LIVING_ROOM_WITH_SHOES",
            "death.ENTER_LIVING_ROOM_WITH_SHOES.title",
            "death.ENTER_LIVING_ROOM_WITH_SHOES.subtitle",
            null
    ),

    /**
     * 把自己鎖進衣櫃。
     */
    LOCK_YOURSELF_IN_THE_CLOSET(
            "LOCK_YOURSELF_IN_THE_CLOSET",
            "death.LOCK_YOURSELF_IN_THE_CLOSET.title",
            "death.LOCK_YOURSELF_IN_THE_CLOSET.subtitle",
            null
    ),


    // =========================================================
    // HouseScene - Mom Battle
    // =========================================================

    /**
     * 媽媽 Boss 戰失敗 A。
     */
    MOM_BATTLE_LOSE_A(
            "MOM_BATTLE_LOSE_A",
            "death.MOM_BATTLE_LOSE_A.title",
            "death.MOM_BATTLE_LOSE_A.subtitle",
            null
    ),

    /**
     * 媽媽 Boss 戰失敗 B。
     */
    MOM_BATTLE_LOSE_B(
            "MOM_BATTLE_LOSE_B",
            "death.MOM_BATTLE_LOSE_B.title",
            "death.MOM_BATTLE_LOSE_B.subtitle",
            null
    ),

    /**
     * 媽媽 Boss 戰失敗 C。
     */
    MOM_BATTLE_LOSE_C(
            "MOM_BATTLE_LOSE_C",
            "death.MOM_BATTLE_LOSE_C.title",
            "death.MOM_BATTLE_LOSE_C.subtitle",
            null
    ),

    /**
     * 媽媽跳舞制裁。
     *
     * 注意：
     * 原本 id 寫成 MOM_BATTLE_LOSE_C，
     * 這裡修正為 MOM_DANCE_OFF。
     */
    MOM_DANCE_OFF(
            "MOM_DANCE_OFF",
            "death.MOM_DANCE_OFF.title",
            "death.MOM_DANCE_OFF.subtitle",
            null
    ),


    // =========================================================
    // StreetScene - Obstacles
    // =========================================================

    /**
     * 被凸起的人行道磁磚絆倒。
     */
    TRIPPED_BY_SIDEWALK_TILE(
            "TRIPPED_BY_SIDEWALK_TILE",
            "death.TRIPPED_BY_SIDEWALK_TILE.title",
            "death.TRIPPED_BY_SIDEWALK_TILE.subtitle",
            null
    ),

    /**
     * 被機車撞到。
     */
    HIT_BY_SCOOTER(
            "HIT_BY_SCOOTER",
            "death.HIT_BY_SCOOTER.title",
            "death.HIT_BY_SCOOTER.subtitle",
            null
    ),

    /**
     * 被掉落的冰箱砸到。
     */
    FALLING_FRIDGE(
            "FALLING_FRIDGE",
            "death.FALLING_FRIDGE.title",
            "death.FALLING_FRIDGE.subtitle",
            null
    ),

    /**
     * 被掉落的直升機砸到。
     */
    FALLING_HELI(
            "FALLING_HELI",
            "death.FALLING_HELI.title",
            "death.FALLING_HELI.subtitle",
            null
    );


    // =========================================================
    // Basic Settings
    // =========================================================

    /**
     * 死亡原因 ID。
     *
     * 建議與 enum 名稱一致。
     */
    private final String id;

    /**
     * 死亡畫面標題語言 key。
     */
    private final String titleKey;

    /**
     * 死亡畫面副標題語言 key。
     */
    private final String subtitleKey;

    /**
     * 死亡原因 icon 路徑。
     *
     * 目前可為 null。
     */
    private final String iconPath;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立死亡原因資料。
     *
     * @param id 死亡原因 ID
     * @param titleKey 死亡標題語言 key
     * @param subtitleKey 死亡副標題語言 key
     * @param iconPath 死亡 icon 路徑，可為 null
     */
    DeathReason(
            String id,
            String titleKey,
            String subtitleKey,
            String iconPath
    ) {
        this.id = id;
        this.titleKey = titleKey;
        this.subtitleKey = subtitleKey;
        this.iconPath = iconPath;
    }


    // =========================================================
    // Getters - Raw Data
    // =========================================================

    /**
     * 取得死亡原因 ID。
     *
     * @return 死亡原因 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 取得死亡標題語言 key。
     *
     * @return 標題語言 key
     */
    public String getTitleKey() {
        return titleKey;
    }

    /**
     * 取得死亡副標題語言 key。
     *
     * @return 副標題語言 key
     */
    public String getSubtitleKey() {
        return subtitleKey;
    }

    /**
     * 取得死亡 icon 路徑。
     *
     * @return icon 路徑，可為 null
     */
    public String getIconPath() {
        return iconPath;
    }


    // =========================================================
    // Getters - Localized Text
    // =========================================================

    /**
     * 取得目前語言下的死亡標題。
     *
     * @return 已翻譯死亡標題
     */
    public String getTitle() {
        return LanguageSystem.getInstance().text(titleKey);
    }

    /**
     * 取得目前語言下的死亡副標題。
     *
     * @return 已翻譯死亡副標題
     */
    public String getSubtitle() {
        return LanguageSystem.getInstance().text(subtitleKey);
    }
}