package ass.example.components;

import com.almasb.fxgl.entity.component.Component;

public class OneWayPlatformComponent extends Component {

    private final String platformId;
    private final double width;
    private final double height;
    private final int playerZIndexOnTop;

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

    public String getPlatformId() {
        return platformId;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public int getPlayerZIndexOnTop() {
        return playerZIndexOnTop;
    }
}