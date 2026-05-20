package ass.example.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

public class DoorComponent extends Component {

    private final String id;

    private final String closedTexture;
    private final String openTexture;

    private final double colliderOffsetX;
    private final double colliderOffsetY;
    private final double colliderWidth;
    private final double colliderHeight;

    private boolean opened = false;

    private Entity collider;

    public DoorComponent(
            String id,
            String closedTexture,
            String openTexture,
            double colliderOffsetX,
            double colliderOffsetY,
            double colliderWidth,
            double colliderHeight
    ) {
        this.id = id;
        this.closedTexture = closedTexture;
        this.openTexture = openTexture;
        this.colliderOffsetX = colliderOffsetX;
        this.colliderOffsetY = colliderOffsetY;
        this.colliderWidth = colliderWidth;
        this.colliderHeight = colliderHeight;
    }

    @Override
    public void onAdded() {
        opened = false;
        createCollider();
    }

    public void toggle() {
        if (opened) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        if (opened) {
            return;
        }

        opened = true;

        setTexture(openTexture);
        FXGL.play("door_open.wav");

        if (collider != null) {
            collider.removeFromWorld();
            collider = null;
        }
    }

    public void close() {
        if (!opened && collider != null) {
            return;
        }

        opened = false;

        setTexture(closedTexture);
        createCollider();
        FXGL.play("door_close.wav");
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