package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.core.SoundId;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import ass.example.system.AudioSystem;
import static com.almasb.fxgl.dsl.FXGL.*;

public class DoorComponent extends Component implements LoadSaveComponent {

    private final String id;

    private final String closedTexture;
    private final String openTexture;

    private final double colliderOffsetX;
    private final double colliderOffsetY;
    private final double colliderWidth;
    private final double colliderHeight;

    private boolean opened = false;
    private Runnable onOpen = () -> {};

    private Entity collider;

    private final AudioSystem audioSystem;

    public DoorComponent(
            String id,
            String closedTexture,
            String openTexture,
            double colliderOffsetX,
            double colliderOffsetY,
            double colliderWidth,
            double colliderHeight,
            AudioSystem audioSystem
    ) {
        this.id = id;
        this.closedTexture = closedTexture;
        this.openTexture = openTexture;
        this.colliderOffsetX = colliderOffsetX;
        this.colliderOffsetY = colliderOffsetY;
        this.colliderWidth = colliderWidth;
        this.colliderHeight = colliderHeight;
        this.audioSystem = audioSystem;
    }

    @Override
    public void onAdded() {
        opened = false;
        createCollider();
    }

    @Override
    public void applySavedState() {
        boolean shouldOpen = getb("door_" + id + "_opened");

        if (shouldOpen) {
            restoreOpenedState();
        } else {
            restoreClosedState();
        }
    }

    public void restoreOpenedState() {
        opened = true;

        setTexture(openTexture);

        if (collider != null) {
            collider.removeFromWorld();
            collider = null;
        }
    }

    public void restoreClosedState() {
        opened = false;

        setTexture(closedTexture);

        if (collider == null) {
            createCollider();
        }
    }

    public void toggle() {
        if (opened) {
            close();
        } else {
            open();
        }
    }

    public void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen != null ? onOpen : () -> {};
    }

    public void open() {
        if (opened) {
            return;
        }

        opened = true;
        set("door_" + id + "_opened", true);

        setTexture(openTexture);
        audioSystem.playSFX(SoundId.DOOR_OPEN);

        if (collider != null) {
            collider.removeFromWorld();
            collider = null;
        }

        onOpen.run();
    }

    public void close() {
        if (!opened && collider != null) {
            return;
        }

        opened = false;
        set("door_" + id + "_opened", false);

        setTexture(closedTexture);
        createCollider();
        audioSystem.playSFX(SoundId.DOOR_CLOSE);
    }

    private void createCollider() {
        if (collider != null) {
            return;
        }

        collider = spawn("door_collider", new SpawnData(
                        entity.getX() + colliderOffsetX,
                        entity.getY() + colliderOffsetY
                )
                        .put("width", colliderWidth)
                        .put("height", colliderHeight)
        );
    }

    private void setTexture(String texturePath) {
        entity.getViewComponent().clearChildren();
        entity.getViewComponent().addChild(texture(texturePath));
    }

    public boolean isOpened() {
        return opened;
    }
}