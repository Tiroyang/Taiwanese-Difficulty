package ass.example.scenes;

public class SceneConfig {

    private final int mapWidth;
    private final int mapHeight;

    private final double playerStartX;
    private final double playerStartY;

    public SceneConfig(
            int mapWidth,
            int mapHeight,
            double playerStartX,
            double playerStartY
    ) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.playerStartX = playerStartX;
        this.playerStartY = playerStartY;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public double getPlayerStartX() {
        return playerStartX;
    }

    public double getPlayerStartY() {
        return playerStartY;
    }
}