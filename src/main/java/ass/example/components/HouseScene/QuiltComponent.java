package ass.example.components.HouseScene;

import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

public class QuiltComponent extends Component {

    private final String messyTexture;
    private final String foldedTexture;

    private boolean folded = false;

    public QuiltVisualComponent(String messyTexture, String foldedTexture) {
        this.messyTexture = messyTexture;
        this.foldedTexture = foldedTexture;
    }

    @Override
    public void onAdded() {
        showMessy();
    }

    public void fold() {
        if (folded) {
            return;
        }

        folded = true;
        setTexture(foldedTexture);

        System.out.println("Quilt folded");
    }

    private void showMessy() {
        setTexture(messyTexture);
    }

    private void setTexture(String texturePath) {
        entity.getViewComponent().clearChildren();
        entity.getViewComponent().addChild(texture(texturePath));
    }

    public boolean isFolded() {
        return folded;
    }
}