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
    );

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