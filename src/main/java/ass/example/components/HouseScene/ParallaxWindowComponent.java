package ass.example.components.HouseScene;

import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

public class ParallaxWindowComponent extends Component {

    private final double baseX;
    private final double baseY;

    private final double parallaxFactor;

    public ParallaxWindowComponent(double baseX, double baseY, double parallaxFactor) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.parallaxFactor = parallaxFactor;
    }

    @Override
    public void onUpdate(double tpf) {
        double cameraX = getGameScene().getViewport().getX();

        entity.setX(baseX + cameraX * parallaxFactor);
        entity.setY(baseY);
    }
}