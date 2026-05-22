package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import static com.almasb.fxgl.dsl.FXGL.*;

public class QuiltComponent extends Component implements LoadSaveComponent {

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

    @Override
    public void applySavedState() {
        if (getb("quiltFolded")) {
            restoreFoldedState();
        }
    }

    private void restoreFoldedState() {
        folded = true;

        setVisualTexture(foldedTexture);

        entity.removeFromWorld();
    }

    public void fold() {
        if (folded) {
            return;
        }

        folded = true;
        set("quiltFolded", true);

        setVisualTexture(foldedTexture);

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