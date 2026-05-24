package ass.example.core.StreetScene;

public enum StreetApartmentStyle {
    /*
     * LEFT：
     * 建築貼左邊，左側需要和前一棟銜接，右側不銜接。
     */
    LEFT(true, false),

    /*
     * RIGHT：
     * 建築貼右邊，右側需要和下一棟銜接，左側不銜接。
     */
    RIGHT(false, true),

    /*
     * CENTER：
     * 置中，不銜接左右。
     */
    CENTER(false, false),

    /*
     * FILL：
     * 置中填滿，左右都銜接。
     */
    FILL(true, true),

    /*
     * EMPTY：
     * 不生成公寓。
     */
    EMPTY(false, false);

    private final boolean connectsLeft;
    private final boolean connectsRight;

    StreetApartmentStyle(boolean connectsLeft, boolean connectsRight) {
        this.connectsLeft = connectsLeft;
        this.connectsRight = connectsRight;
    }

    public boolean connectsLeft() {
        return connectsLeft;
    }

    public boolean connectsRight() {
        return connectsRight;
    }

    public boolean isVisibleApartment() {
        return this != EMPTY;
    }
}