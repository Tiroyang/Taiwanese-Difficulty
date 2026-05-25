package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.quest.QuestSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import static com.almasb.fxgl.dsl.FXGL.*;

public class QuiltComponent extends Component implements LoadSaveComponent {

    private final AudioSystem audioSystem;

    private final Entity visualEntity;

    private final String messyTexture;
    private final String foldedTexture;

    private boolean folded = false;

    public QuiltComponent(
            Entity visualEntity,
            String messyTexture,
            String foldedTexture,
            AudioSystem audioSystem
    ) {
        this.visualEntity = visualEntity;
        this.messyTexture = messyTexture;
        this.foldedTexture = foldedTexture;
        this.audioSystem = audioSystem;
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
        QuestSystem.getInstance().completeQuest(QuestType.FOLD_QUILT);

        audioSystem.playSFX(SoundId.FOLDING_QUILT);

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