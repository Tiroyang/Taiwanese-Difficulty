package ass.example.components.HouseScene;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import static com.almasb.fxgl.dsl.FXGL.*;

public class QuiltComponent extends Component {

    private final Entity visualEntity;

    private final String messyTexture;
    private final String foldedTexture;

    private boolean folded = false;

    public QuiltComponent(
            Entity visualEntity,
            String messyTexture,
            String foldedTexture
    ) {
        this.visualEntity = visualEntity;
        this.messyTexture = messyTexture;
        this.foldedTexture = foldedTexture;
    }

    @Override
    public void onAdded() {
        setVisualTexture(messyTexture);
    }

    public void fold() {
        if (folded) {
            return;
        }

        folded = true;

        setVisualTexture(foldedTexture);

        set("quiltFolded", true);

        entity.removeFromWorld();
    }

    private void setVisualTexture(String texturePath) {
        visualEntity.getViewComponent().clearChildren();
        visualEntity.getViewComponent().addChild(texture(texturePath));
    }

    public boolean isFolded() {
        return folded;
    }
}