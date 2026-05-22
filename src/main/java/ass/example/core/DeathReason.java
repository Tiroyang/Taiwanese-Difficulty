package ass.example.core;

/**
 * 死亡原因列表
 */
public enum DeathReason {

    LEFT_BEDROOM_WITHOUT_FOLDING_QUILT(
            "LEFT_BEDROOM_WITHOUT_FOLDING_QUILT",
            "沒折被子",
            "人家是西點軍校你是西點蛋糕啊。",
            null
    ),

    HIT_CEILING(
            "HIT_CEILING",
            "撞到天花板",
            "小心碰頭。",
            null
    ),

    HIT_SHOWER_CURTAIN_ROD(
            "HIT_SHOWER_CURTAIN_ROD",
            "撞到浴簾桿",
            "小心碰頭。",
            null
    ),

    HIT_DOORFRAME(
            "HIT_DOORFRAME",
            "撞到門框",
            "小心碰頭。",
            null
    ),

    JUMPING_ON_BED(
            "JUMPING_ON_BED",
            "在床上跳被媽媽制裁了",
            "超大雙人床！",
            null
            // WHY ARE YOU BREAKING BED?
    ),

    DRINK_WATER(
            "DRINK_WATER",
            "喝下過夜水",
            "眾所周知，水放一整天可以喝，水放過夜不能喝。",
            null
    );

    private final String id;
    private final String title;
    private final String subtitle;
    private final String iconPath;

    DeathReason(String id, String title, String subtitle, String iconPath) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconPath = iconPath;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() { return subtitle; }

    public String getIconPath() { return iconPath; }
}