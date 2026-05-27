package ass.example.components.HouseScene;

import ass.example.components.PlayerComponent;
import ass.example.components.LoadSaveComponent;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.quest.QuestSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

public class ShoeComponent extends Component implements LoadSaveComponent {

    private final Entity shoeVisual;
    private final Entity player;

    private final String defaultTexture;
    private final String wornTexture;

    private final AudioSystem audioSystem;

    private boolean worn = false;

    public ShoeComponent(
            Entity shoeVisual,
            Entity player,
            String defaultTexture,
            String wornTexture,
            AudioSystem audioSystem
    ) {
        this.shoeVisual = shoeVisual;
        this.player = player;
        this.defaultTexture = defaultTexture;
        this.wornTexture = wornTexture;
        this.audioSystem = audioSystem;
    }

    @Override
    public void onAdded() {
        applySavedState();
    }

    @Override
    public void applySavedState() {
        worn = getb("shoesWorn");

        if (worn) {
            restoreWornState();
        } else {
            restoreDefaultState();
        }
    }

    public void toggle() {
        if (worn) {
            takeOff();
        } else {
            wear();
        }
    }

    public void wear() {
        if (worn) {
            return;
        }

        worn = true;
        set("shoesWorn", true);

        setVisualTexture(wornTexture);
        updatePlayerShoesState();

        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.EQUIP);
        }

        QuestSystem.getInstance().completeQuest(QuestType.WEAR_SHOES);
    }

    public void takeOff() {
        if (!worn) {
            return;
        }

        worn = false;
        set("shoesWorn", false);

        setVisualTexture(defaultTexture);
        updatePlayerShoesState();

        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.EQUIP);
        }
    }

    private void restoreWornState() {
        worn = true;
        setVisualTexture(wornTexture);
        updatePlayerShoesState();
    }

    private void restoreDefaultState() {
        worn = false;
        setVisualTexture(defaultTexture);
        updatePlayerShoesState();
    }

    private void setVisualTexture(String texturePath) {
        if (shoeVisual == null) {
            return;
        }

        shoeVisual.getViewComponent().clearChildren();
        shoeVisual.getViewComponent().addChild(texture(texturePath));
    }

    private void updatePlayerShoesState() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return;
        }

        player.getComponent(PlayerComponent.class).setShoesWorn(worn);
    }

    public boolean isWorn() {
        return worn;
    }
}