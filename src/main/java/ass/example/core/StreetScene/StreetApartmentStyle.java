package ass.example.core.StreetScene;

/**
 * StreetApartmentStyle
 *
 * StreetScene 中公寓背景的排列樣式。
 *
 * 用途：
 * 生成街道區塊時，決定公寓圖片如何放置與銜接。
 *
 * 每種樣式會定義：
 * 1. 左側是否需要與前一棟建築銜接。
 * 2. 右側是否需要與下一棟建築銜接。
 * 3. 是否真的需要生成可見公寓。
 */
public enum StreetApartmentStyle {

    /**
     * 建築貼左。
     *
     * 左側需要與前一棟銜接，
     * 右側不需要銜接。
     */
    LEFT(
            true,
            false
    ),

    /**
     * 建築貼右。
     *
     * 右側需要與下一棟銜接，
     * 左側不需要銜接。
     */
    RIGHT(
            false,
            true
    ),

    /**
     * 建築置中。
     *
     * 左右都不銜接。
     */
    CENTER(
            false,
            false
    ),

    /**
     * 建築填滿整個區段。
     *
     * 左右都需要銜接。
     */
    FILL(
            true,
            true
    ),

    /**
     * 不生成公寓。
     */
    EMPTY(
            false,
            false
    );

 
    // Connection Settings 

    /**
     * 是否需要與左側建築銜接。
     */
    private final boolean connectsLeft;

    /**
     * 是否需要與右側建築銜接。
     */
    private final boolean connectsRight;

 
    // Constructor 

    /**
     * 建立公寓排列樣式。
     *
     * @param connectsLeft 是否銜接左側
     * @param connectsRight 是否銜接右側
     */
    StreetApartmentStyle(
            boolean connectsLeft,
            boolean connectsRight
    ) {
        this.connectsLeft = connectsLeft;
        this.connectsRight = connectsRight;
    }

 
    // Getters 

    /**
     * 是否需要與左側建築銜接。
     *
     * @return true 表示左側需要銜接
     */
    public boolean connectsLeft() {
        return connectsLeft;
    }

    /**
     * 是否需要與右側建築銜接。
     *
     * @return true 表示右側需要銜接
     */
    public boolean connectsRight() {
        return connectsRight;
    }

    /**
     * 是否需要生成可見公寓。
     *
     * EMPTY 代表不生成公寓。
     *
     * @return true 表示此樣式會生成公寓
     */
    public boolean isVisibleApartment() {
        return this != EMPTY;
    }
}