package ass.example.core;

/**
 * 死亡原因列表
 */
public enum DeathReasons {

    LEFT_BEDROOM_WITHOUT_FOLDING_QUILT(
            "LEFT_BEDROOM_WITHOUT_FOLDING_QUILT",
            "沒折被子",
            "人家是西點軍校你是西點蛋糕啊。"
    ),

    HIT_CEILING(
            "HIT_CEILING",
            "撞到天花板",
            "小心碰頭。"
    ),

    HIT_SHOWER_CURTAIN_ROD(
            "HIT_SHOWER_CURTAIN_ROD",
            "撞到浴簾桿",
            "小心碰頭。"
    ),

    JUMPING_ON_BED(
            "JUMPING_ON_BED",
            "在床上跳被媽媽制裁了",
            "WHY ARE YOU BREAKING BED?"
    );

    private final String id;
    private final String title;
    private final String subtitle;

    DeathReasons(String id, String title, String subtitle) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}