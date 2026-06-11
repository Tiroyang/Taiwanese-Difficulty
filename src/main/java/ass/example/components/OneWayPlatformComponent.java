package ass.example.components;

import com.almasb.fxgl.entity.component.Component;

/**
 * OneWayPlatformComponent
 *
 * 一般單向平台資料 Component。
 *
 * 功能：
 * 1. 標記某個 Entity 是單向平台。
 * 2. 保存平台 ID。
 * 3. 保存平台寬度與高度。
 * 4. 保存玩家站在平台上時應使用的 zIndex。
 *
 * ----
 * 單向平台設計
 * ----
 *
 * 單向平台通常具有以下特性：
 *
 * 1. 玩家從上方落下時，可以站在平台上。
 * 2. 玩家從下方往上跳時，可以穿過平台。
 * 3. 玩家站在平台上時，可以按下落鍵往下穿過。
 * 4. 玩家站上平台時，可能需要調整玩家 zIndex。
 */
public class OneWayPlatformComponent extends Component {
 
    // Platform Settings 

    /**
     * 平台 ID。
     *
     * 用途：
     * 1. 區分不同平台。
     * 2. 儲存或還原平台狀態。
     * 3. 除錯時辨識平台來源。
     */
    private final String platformId;

    /**
     * 平台寬度。
     *
     * 通常需與 Entity 的 bounding box 寬度一致。
     */
    private final double width;

    /**
     * 平台高度。
     *
     * 通常需與 Entity 的 bounding box 高度一致。
     */
    private final double height;

    /**
     * 玩家站在平台上時的 zIndex。
     *
     * 用途：
     * 讓玩家站上平台時可以顯示在正確圖層。
     */
    private final int playerZIndexOnTop;

 
    // Constructor 

    /**
     * 建立單向平台 Component。
     *
     * @param platformId 平台 ID
     * @param width 平台寬度
     * @param height 平台高度
     * @param playerZIndexOnTop 玩家站上平台時的 zIndex
     */
    public OneWayPlatformComponent(
            String platformId,
            double width,
            double height,
            int playerZIndexOnTop
    ) {
        this.platformId = platformId;
        this.width = width;
        this.height = height;
        this.playerZIndexOnTop = playerZIndexOnTop;
    }

 
    // Getters 

    /**
     * 取得平台 ID。
     *
     * @return 平台 ID
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * 取得平台寬度。
     *
     * @return 平台寬度
     */
    public double getWidth() {
        return width;
    }

    /**
     * 取得平台高度。
     *
     * @return 平台高度
     */
    public double getHeight() {
        return height;
    }

    /**
     * 取得玩家站在平台上時的 zIndex。
     *
     * @return 玩家站上平台時的 zIndex
     */
    public int getPlayerZIndexOnTop() {
        return playerZIndexOnTop;
    }
}