package ass.example.components;

import ass.example.system.LanguageSystem;
import com.almasb.fxgl.entity.component.Component;
import java.util.function.Supplier;

/**
 * InteractableComponent
 *
 * 通用互動 Component。
 *
 * 此 Component 只保存一個 Entity 作為「可互動物件」所需的資料與行為。
 *
 * 通常會由 InteractionSystem 讀取此 Component，並負責：
 * 1. 判斷玩家是否在 interactRange 內。
 * 2. 判斷 canInteract() 是否允許互動。
 * 3. 顯示 getPromptText() 回傳的互動提示。
 * 4. 玩家按下互動鍵時呼叫 interact()。
 *
 *   ----
 * 主要功能
 *   ----
 *
 * 1. 提供互動提示文字 key。
 * 2. 支援動態互動提示。
 * 3. 儲存互動行為。
 * 4. 設定互動距離。
 * 5. 設定提示文字顯示位置。
 * 6. 支援互動條件。
 *
 *   ----
 * 使用範例
 *   ----
 *
 * 固定提示文字：
 *
 * new InteractableComponent(
 *         "story.house.foldQuilt",
 *         quiltComponent::fold
 * )
 *
 * 動態提示文字：
 *
 * new InteractableComponent(
 *         () -> doorComponent.isOpened()
 *                 ? "story.house.closeDoor"
 *                 : "story.house.openDoor",
 *         doorComponent::toggle,
 *         120,
 *         false,
 *         35
 * )
 *
 * 帶互動條件：
 *
 * new InteractableComponent(
 *         () -> "story.house.brush_teeth",
 *         this::brushTeeth,
 *         180,
 *         true,
 *         40,
 *         () -> !getb("teethBrushed")
 * )
 */
public class InteractableComponent extends Component {
 
    // Default Settings 

    /**
     * 預設互動距離。
     *
     * 若沒有特別指定，玩家與互動物件距離小於此值時可互動。
     */
    private static final double DEFAULT_INTERACT_RANGE = 220.0;

    /**
     * 預設提示文字是否顯示在 Entity 上。
     *
     * false：
     * - 通常由 InteractionSystem 統一顯示在 HUD 或玩家附近。
     *
     * true：
     * - 通常顯示在互動物件本身附近。
     */
    private static final boolean DEFAULT_PROMPT_ON_ENTITY = false;

    /**
     * 預設提示文字 Y 偏移。
     *
     * 當 promptOnEntity == true 時，
     * 可用此值讓提示文字顯示在物件上方。
     */
    private static final double DEFAULT_PROMPT_OFFSET_Y = 30.0;

 
    // Prompt Settings 

    /**
     * 互動提示文字 key 的供應器。
     *
     * 回傳值是 LanguageSystem 使用的語言 key。
     *
     * 使用 Supplier 的原因：
     * - 可支援動態提示。
     *
     * 例如門：
     * - 門關著時：story.house.openDoor
     * - 門開著時：story.house.closeDoor
     */
    private final Supplier<String> promptKeySupplier;

 
    // Interaction Action 

    /**
     * 玩家執行互動時要觸發的行為。
     */
    private final Runnable interactAction;

 
    // Interaction Range Settings 

    /**
     * 玩家可互動距離。
     *
     * InteractionSystem 通常會用玩家與此 Entity 的距離
     * 判斷是否顯示提示與允許互動。
     */
    private final double interactRange;

    /**
     * 提示文字是否顯示在 Entity 上。
     *
     * true：
     * - 提示跟著互動物件。
     *
     * false：
     * - 提示可能由 UI 系統統一管理。
     */
    private final boolean promptOnEntity;

    /**
     * 提示文字相對 Entity 的 Y 偏移。
     *
     * 通常用於讓提示顯示在物件上方。
     */
    private final double promptOffsetY;

 
    // Interaction Condition 

    /**
     * 是否允許互動的條件。
     *
     * 回傳 true：
     * - 可以互動。
     *
     * 回傳 false：
     * - 不顯示或不執行互動。
     *
     * 使用 Supplier 的原因：
     * - 條件可能會根據遊戲狀態動態改變。
     *
     * 例如：
     * - () -> !getb("teethBrushed")
     * - () -> !getb("playerDead")
     * - () -> questSystem.isCompleted(...)
     */
    private final Supplier<Boolean> canInteractCondition;

 
    // Constructors - Simple 

    /**
     * 建立最簡單的互動 Component。
     *
     * 使用預設設定：
     * - 互動距離：DEFAULT_INTERACT_RANGE
     * - 提示不綁在 Entity 上
     * - 提示 Y 偏移：DEFAULT_PROMPT_OFFSET_Y
     * - 永遠可以互動
     *
     * @param promptKey 互動提示語言 key
     * @param interactAction 互動行為
     */
    public InteractableComponent(
            String promptKey,
            Runnable interactAction
    ) {
        this(
                () -> promptKey,
                interactAction,
                DEFAULT_INTERACT_RANGE,
                DEFAULT_PROMPT_ON_ENTITY,
                DEFAULT_PROMPT_OFFSET_Y,
                () -> true
        );
    }

    /**
     * 建立可指定互動距離與提示位置的互動 Component。
     *
     * @param promptKey 互動提示語言 key
     * @param interactAction 互動行為
     * @param interactRange 互動距離
     * @param promptOnEntity 提示是否顯示在 Entity 上
     */
    public InteractableComponent(
            String promptKey,
            Runnable interactAction,
            double interactRange,
            boolean promptOnEntity
    ) {
        this(
                () -> promptKey,
                interactAction,
                interactRange,
                promptOnEntity,
                DEFAULT_PROMPT_OFFSET_Y,
                () -> true
        );
    }

 
    // Constructors - Dynamic Prompt 

    /**
     * 建立支援動態提示文字的互動 Component。
     *
     * 適合用於提示文字會隨狀態改變的物件。
     *
     * 例如：
     * - 門：開門 / 關門
     * - 鞋櫃：穿鞋 / 脫鞋
     *
     * @param promptKeySupplier 互動提示語言 key 供應器
     * @param interactAction 互動行為
     * @param interactRange 互動距離互動距離
     * @param promptOnEntity 提示是否顯示在 Entity 上
     * @param promptOffsetY 提示 Y 偏移
     */
    public InteractableComponent(
            Supplier<String> promptKeySupplier,
            Runnable interactAction,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY
    ) {
        this(
                promptKeySupplier,
                interactAction,
                interactRange,
                promptOnEntity,
                promptOffsetY,
                () -> true
        );
    }

    /**
     * 建立完整功能的互動 Component。
     *
     * 可自訂：
     * 1. 動態提示文字。
     * 2. 互動行為。
     * 3. 互動距離。
     * 4. 提示顯示位置。
     * 5. 互動條件。
     *
     * @param promptKeySupplier 互動提示語言 key 供應器
     * @param interactAction 互動行為
     * @param interactRange 互動距離
     * @param promptOnEntity 提示是否顯示在 Entity 上
     * @param promptOffsetY 提示 Y 偏移
     * @param canInteractCondition 是否可互動條件
     */
    public InteractableComponent(
            Supplier<String> promptKeySupplier,
            Runnable interactAction,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY,
            Supplier<Boolean> canInteractCondition
    ) {
        this.promptKeySupplier = promptKeySupplier != null
                ? promptKeySupplier
                : () -> "";

        this.interactAction = interactAction;

        this.interactRange = interactRange;
        this.promptOnEntity = promptOnEntity;
        this.promptOffsetY = promptOffsetY;

        this.canInteractCondition = canInteractCondition != null
                ? canInteractCondition
                : () -> true;
    }

 
    // Interaction State 

    /**
     * 判斷目前是否允許互動。
     *
     * InteractionSystem 可使用這個方法決定：
     * - 是否顯示互動提示。
     * - 玩家按下互動鍵時是否執行 interact()。
     *
     * @return true 表示目前可互動
     */
    public boolean canInteract() {
        return canInteractCondition.get();
    }

 
    // Interaction Execute 

    /**
     * 執行互動行為。
     *
     * 若 interactAction 為 null，則不執行任何事。
     *
     * 本方法本身不會再次檢查 canInteract()，應由 InteractionSystem 在呼叫前先判斷。
     */
    public void interact() {
        if (interactAction != null) {
            interactAction.run();
        }
    }

 
    // Prompt Text 

    /**
     * 取得目前互動提示的語言 key。
     *
     * 若 promptKeySupplier 是動態的，每次呼叫可能會回傳不同 key。
     *
     * @return 互動提示語言 key
     */
    public String getPromptKey() {
        return promptKeySupplier.get();
    }

    /**
     * 取得目前互動提示的顯示文字。
     *
     * 會透過 LanguageSystem 將 getPromptKey() 轉成目前語言的文字。
     *
     * @return 已翻譯的互動提示文字
     */
    public String getPromptText() {
        return LanguageSystem.getInstance().text(getPromptKey());
    }

 
    // Getters 

    /**
     * 取得互動距離。
     *
     * @return 互動距離
     */
    public double getInteractRange() {
        return interactRange;
    }

    /**
     * 取得提示是否顯示在 Entity 上。
     *
     * @return true 表示提示顯示在 Entity 上
     */
    public boolean isPromptOnEntity() {
        return promptOnEntity;
    }

    /**
     * 取得提示文字 Y 偏移。
     *
     * @return 提示文字 Y 偏移
     */
    public double getPromptOffsetY() {
        return promptOffsetY;
    }
}