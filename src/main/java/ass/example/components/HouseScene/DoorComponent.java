package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * DoorComponent
 *
 * 家中場景使用的門 Component。
 *
 * 功能：
 * 1. 控制門的開關狀態。
 * 2. 根據門狀態切換開門 / 關門貼圖。
 * 3. 門關閉時生成阻擋用 collider。
 * 4. 門開啟時移除阻擋用 collider。
 * 5. 開門與關門時播放對應音效。
 * 6. 將門的開關狀態寫入 FXGL game vars，供 SaveSystem 儲存。
 * 7. 讀檔後可透過 applySavedState() 還原門狀態。
 * 8. 可設定 onOpen callback，在開門後執行額外事件。
 *
 * 使用方式：
 *
 * EntityFactory 中建立門 Entity 時，可掛上此 Component：
 *
 * .with(new DoorComponent(
 *         "bathroom",
 *         "Scene1/props/DoorClosed.png",
 *         "Scene1/props/DoorOpen.png",
 *         100, 0,
 *         40, 180,
 *         audioSystem
 * ))
 *
 * 存檔變數格式：
 *
 * door_{id}_opened
 *
 * 例如：
 *
 * door_bathroom_opened
 */
public class DoorComponent extends Component implements LoadSaveComponent {

    // =========================================================
    // Game Var Keys
    // =========================================================

    /**
     * 門狀態存檔 key 的前綴。
     *
     * 實際 key 格式：
     * door_{id}_opened
     */
    private static final String DOOR_VAR_PREFIX = "door_";

    /**
     * 門狀態存檔 key 的後綴。
     */
    private static final String DOOR_VAR_SUFFIX_OPENED = "_opened";


    // =========================================================
    // Spawn Names
    // =========================================================

    /**
     * 門關閉時生成的碰撞箱 spawn name。
     *
     * 需要在 EntityFactory 中有對應：
     *
     * @Spawns("door_collider")
     */
    private static final String SPAWN_DOOR_COLLIDER = "door_collider";


    // =========================================================
    // Basic Door Settings
    // =========================================================

    /**
     * 門的唯一 ID。
     *
     * 用途：
     * 1. 組成存檔 key。
     * 2. 區分不同門的開關狀態。
     *
     * 例如：
     * - bathroom
     * - bedroom
     * - living_room
     */
    private final String id;

    /**
     * 門關閉時使用的貼圖路徑。
     */
    private final String closedTexture;

    /**
     * 門開啟時使用的貼圖路徑。
     */
    private final String openTexture;


    // =========================================================
    // Collider Settings
    // =========================================================

    /**
     * 門碰撞箱相對於門 Entity 的 X 偏移。
     */
    private final double colliderOffsetX;

    /**
     * 門碰撞箱相對於門 Entity 的 Y 偏移。
     */
    private final double colliderOffsetY;

    /**
     * 門碰撞箱寬度。
     */
    private final double colliderWidth;

    /**
     * 門碰撞箱高度。
     */
    private final double colliderHeight;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 音效系統。
     *
     * 用於播放開門與關門音效。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 門目前是否開啟。
     *
     * true：
     * - 顯示 openTexture。
     * - 不存在阻擋 collider。
     *
     * false：
     * - 顯示 closedTexture。
     * - 生成阻擋 collider。
     */
    private boolean opened = false;

    /**
     * 門關閉時生成的阻擋碰撞箱 Entity。
     *
     * 門開啟時會移除。
     * 門關閉時會生成。
     */
    private Entity collider;

    /**
     * 開門後執行的額外事件。
     *
     * 預設為空 Runnable，
     * 避免呼叫時發生 NullPointerException。
     *
     * 例如：
     * - 第一次開門後觸發任務。
     * - 開門後播放劇情。
     * - 開門後解除某個遮罩。
     */
    private Runnable onOpen = () -> {};


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立門 Component。
     *
     * @param id 門 ID，用於存檔 key
     * @param closedTexture 關門貼圖
     * @param openTexture 開門貼圖
     * @param colliderOffsetX 碰撞箱 X 偏移
     * @param colliderOffsetY 碰撞箱 Y 偏移
     * @param colliderWidth 碰撞箱寬度
     * @param colliderHeight 碰撞箱高度
     */
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


    // =========================================================
    // FXGL Lifecycle
    // =========================================================

    /**
     * Component 被加入 Entity 時呼叫。
     *
     * 預設門會以「關閉狀態」建立：
     * 1. opened = false。
     * 2. 顯示關門貼圖。
     * 3. 生成門的阻擋 collider。
     *
     * 注意：
     * 若之後讀檔，applySavedState() 會再根據存檔資料覆蓋狀態。
     */
    @Override
    public void onAdded() {
        restoreClosedState();
    }


    // =========================================================
    // Save / Load
    // =========================================================

    /**
     * 套用存檔中的門狀態。
     *
     * 存檔 key 格式：
     *
     * door_{id}_opened
     *
     * 若值為 true：
     * - 還原為開門狀態。
     *
     * 若值為 false：
     * - 還原為關門狀態。
     */
    @Override
    public void applySavedState() {
        if (getb(getDoorOpenedVarKey())) {
            restoreOpenedState();
        } else {
            restoreClosedState();
        }
    }

    /**
     * 取得此門對應的 game var key。
     *
     * @return 門開啟狀態的 game var key
     */
    private String getDoorOpenedVarKey() {
        return DOOR_VAR_PREFIX + id + DOOR_VAR_SUFFIX_OPENED;
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 切換門的開關狀態。
     *
     * 若目前為開啟，則關閉。
     * 若目前為關閉，則開啟。
     */
    public void toggle() {
        if (opened) {
            close();
        } else {
            open();
        }
    }

    /**
     * 設定開門後要執行的事件。
     *
     * 若傳入 null，會改成空 Runnable，
     * 避免 onOpen.run() 時發生錯誤。
     *
     * @param onOpen 開門後事件
     */
    public void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen != null ? onOpen : () -> {};
    }

    /**
     * 開門。
     *
     * 流程：
     * 1. 若門已經開啟，直接返回。
     * 2. 設定 opened = true。
     * 3. 更新 game var。
     * 4. 切換開門貼圖。
     * 5. 播放開門音效。
     * 6. 移除阻擋 collider。
     * 7. 執行 onOpen callback。
     */
    public void open() {
        if (opened) {
            return;
        }

        opened = true;
        set(getDoorOpenedVarKey(), true);

        showOpenTexture();
        playDoorOpenSound();
        removeCollider();

        onOpen.run();
    }

    /**
     * 關門。
     *
     * 流程：
     * 1. 若門已經關閉，且 collider 已存在，直接返回。
     * 2. 設定 opened = false。
     * 3. 更新 game var。
     * 4. 切換關門貼圖。
     * 5. 生成阻擋 collider。
     * 6. 播放關門音效。
     */
    public void close() {
        if (!opened && collider != null) {
            return;
        }

        opened = false;
        set(getDoorOpenedVarKey(), false);

        showClosedTexture();
        createColliderIfAbsent();
        playDoorCloseSound();
    }


    // =========================================================
    // Restore State
    // =========================================================

    /**
     * 直接還原為開門狀態。
     *
     * 與 open() 不同：
     * - 不播放音效。
     * - 不更新 game var。
     * - 不執行 onOpen。
     *
     * 適合用於：
     * - 讀檔。
     * - 場景初始化後套用狀態。
     */
    public void restoreOpenedState() {
        opened = true;

        showOpenTexture();
        removeCollider();
    }

    /**
     * 直接還原為關門狀態。
     *
     * 與 close() 不同：
     * - 不播放音效。
     * - 不更新 game var。
     *
     * 適合用於：
     * - 初始建立。
     * - 讀檔。
     * - 場景重置。
     */
    public void restoreClosedState() {
        opened = false;

        showClosedTexture();
        createColliderIfAbsent();
    }


    // =========================================================
    // Collider Management
    // =========================================================

    /**
     * 若目前沒有門 collider，則建立新的阻擋 collider。
     *
     * collider 位置：
     *
     * X = doorEntity.x + colliderOffsetX
     * Y = doorEntity.y + colliderOffsetY
     *
     * collider 尺寸：
     *
     * width = colliderWidth
     * height = colliderHeight
     */
    private void createColliderIfAbsent() {
        if (collider != null) {
            return;
        }

        collider = spawn(SPAWN_DOOR_COLLIDER, new SpawnData(
                        entity.getX() + colliderOffsetX,
                        entity.getY() + colliderOffsetY
                )
                        .put("width", colliderWidth)
                        .put("height", colliderHeight)
        );
    }

    /**
     * 移除目前的門 collider。
     *
     * 若 collider 本來就不存在，則不做任何事。
     */
    private void removeCollider() {
        if (collider == null) {
            return;
        }

        collider.removeFromWorld();
        collider = null;
    }


    // =========================================================
    // View Management
    // =========================================================

    /**
     * 顯示開門貼圖。
     */
    private void showOpenTexture() {
        setTexture(openTexture);
    }

    /**
     * 顯示關門貼圖。
     */
    private void showClosedTexture() {
        setTexture(closedTexture);
    }

    /**
     * 切換門的貼圖。
     *
     * 這裡會先清空原本的 view children，
     * 再加入新的貼圖。
     *
     * @param texturePath 新貼圖路徑
     */
    private void setTexture(String texturePath) {
        entity.getViewComponent().clearChildren();
        entity.getViewComponent().addChild(texture(texturePath));
    }


    // =========================================================
    // Audio
    // =========================================================

    /**
     * 播放開門音效。
     */
    private void playDoorOpenSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.DOOR_OPEN);
        }
    }

    /**
     * 播放關門音效。
     */
    private void playDoorCloseSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.DOOR_CLOSE);
        }
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得門目前是否開啟。
     *
     * @return true 表示門目前開啟
     */
    public boolean isOpened() {
        return opened;
    }
}