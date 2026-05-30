package ass.example.core;

import ass.example.system.LanguageSystem;

/**
 * 死亡原因列表
 */
public enum DeathReason {

    LEFT_BEDROOM_WITHOUT_FOLDING_QUILT(
            "LEFT_BEDROOM_WITHOUT_FOLDING_QUILT",
            "death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.title",
            "death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.subtitle",
            null
    ),

    HIT_CEILING(
            "HIT_CEILING",
            "death.HIT_CEILING.title",
            "death.HIT_CEILING.subtitle",
            null
    ),

    HIT_SHOWER_CURTAIN_ROD(
            "HIT_SHOWER_CURTAIN_ROD",
            "death.HIT_SHOWER_CURTAIN_ROD.title",
            "death.HIT_SHOWER_CURTAIN_ROD.subtitle",
            null
    ),

    HIT_DOORFRAME(
            "HIT_DOORFRAME",
            "death.HIT_DOORFRAME.title",
            "death.HIT_DOORFRAME.subtitle",
            null
    ),

    JUMPING_ON_BED(
            "JUMPING_ON_BED",
            "death.JUMPING_ON_BED.title",
            "death.JUMPING_ON_BED.subtitle",
            null
            // WHY ARE YOU BREAKING BED?
    ),

    DRINK_WATER(
            "DRINK_WATER",
            "death.DRINK_WATER.title",
            "death.DRINK_WATER.subtitle",
            null
    ),

    TRIPPED_BY_SIDEWALK_TILE(
            "TRIPPED_BY_SIDEWALK_TILE",
            "death.TRIPPED_BY_SIDEWALK_TILE.title",
            "death.TRIPPED_BY_SIDEWALK_TILE.subtitle",
            null
    ),

    HIT_BY_SCOOTER(
            "HIT_BY_SCOOTER",
            "death.HIT_BY_SCOOTER.title",
            "death.HIT_BY_SCOOTER.subtitle",
            null
    ),

    FALLING_FRIDGE(
            "FALLING_FRIDGE",
            "death.FALLING_FRIDGE.title",
            "death.FALLING_FRIDGE.subtitle",
            null
    ),

    FALLING_HELI(
            "FALLING_HELI",
            "death.FALLING_HELI.title",
            "death.FALLING_HELI.subtitle",
            null
    ),

    JUMPED_IN_BATHTUB(
            "JUMPED_IN_BATHTUB",
            "death.JUMPED_IN_BATHTUB.title",
            "death.JUMPED_IN_BATHTUB.subtitle",
            null
    ),

    LEFT_WITHOUT_BRUSHING_TEETH(
            "LEFT_WITHOUT_BRUSHING_TEETH",
            "death.LEFT_WITHOUT_BRUSHING_TEETH.title",
            "death.LEFT_WITHOUT_BRUSHING_TEETH.subtitle",
            null
    ),

    ENTER_LIVING_ROOM_WITH_SHOES(
            "ENTER_LIVING_ROOM_WITH_SHOES",
            "death.ENTER_LIVING_ROOM_WITH_SHOES.title",
            "death.ENTER_LIVING_ROOM_WITH_SHOES.subtitle",
            null
    ),

    LOCK_YOURSELF_IN_THE_CLOSET(
            "LOCK_YOURSELF_IN_THE_CLOSET",
            "death.LOCK_YOURSELF_IN_THE_CLOSET.title",
            "death.LOCK_YOURSELF_IN_THE_CLOSET.subtitle",
            null
    ),

    MOM_BATTLE_LOSE_A(
            "MOM_BATTLE_LOSE_A",
            "death.MOM_BATTLE_LOSE_A.title",
            "death.MOM_BATTLE_LOSE_A.subtitle",
            null
    ),

    MOM_BATTLE_LOSE_B(
            "MOM_BATTLE_LOSE_B",
            "death.MOM_BATTLE_LOSE_B.title",
            "death.MOM_BATTLE_LOSE_B.subtitle",
            null
    ),

    MOM_BATTLE_LOSE_C(
            "MOM_BATTLE_LOSE_C",
            "death.MOM_BATTLE_LOSE_C.title",
            "death.MOM_BATTLE_LOSE_C.subtitle",
            null
    ),

    MOM_DANCE_OFF(
            "MOM_BATTLE_LOSE_C",
            "death.MOM_DANCE_OFF.title",
            "death.MOM_DANCE_OFF.subtitle",
            null
    ),

    /*
    MOM_RUN(
            "MOM_RUN",
            "death.MOM_RUN.title",
            "death.MOM_RUN.subtitle",
            null
    )
     */
    ;


    private final String id;
    private final String titleKey;
    private final String subtitleKey;
    private final String iconPath;

    DeathReason (
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

    public String getId() {
        return id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getSubtitleKey() {
        return subtitleKey;
    }

    public String getTitle() {
        return LanguageSystem.getInstance().text(titleKey);
    }

    public String getSubtitle() {
        return LanguageSystem.getInstance().text(subtitleKey);
    }

    public String getIconPath() {
        return iconPath;
    }
}