package ass.example.system.dialogue;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.MusicSystem;
import ass.example.ui.DialogueUI;
import ass.example.ui.MomBattleMiniGame;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.Map;

import static ass.example.core.DeathReason.MOM_DANCE_OFF;
import static com.almasb.fxgl.dsl.FXGL.addUINode;
import static com.almasb.fxgl.dsl.FXGL.removeUINode;

/**
 * DialogueSystem
 *
 * 對話系統。
 *
 * 功能：
 * 1. 管理目前是否正在對話。
 * 2. 開始對話。
 * 3. 切換對話行。
 * 4. 處理下一句對話。
 * 5. 結束對話。
 * 6. 控制玩家在對話期間不能移動。
 * 7. 控制對話 BGM 與場景 BGM。
 * 8. 處理特殊對話事件，例如媽媽 Boss 戰、媽媽跳舞制裁。
 *
 * 單例設計：
 * DialogueSystem 適合使用單例，因為整個遊戲同一時間只需要一個對話系統。
 */
public class DialogueSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * DialogueSystem 單例。
     */
    private static final DialogueSystem INSTANCE = new DialogueSystem();

    /**
     * 取得 DialogueSystem 單例。
     *
     * @return DialogueSystem
     */
    public static DialogueSystem getInstance() {
        return INSTANCE;
    }


    // =========================================================
    // Dialogue Data
    // =========================================================

    /**
     * 所有對話資料。
     *
     * key：
     * - DialogueLine id
     *
     * value：
     * - DialogueLine
     */
    private final Map<String, DialogueLine> lines;


    // =========================================================
    // Runtime References
    // =========================================================

    /**
     * 目前顯示中的 DialogueUI。
     */
    private DialogueUI dialogueUI;

    /**
     * 目前參與對話的玩家。
     */
    private Entity player;

    /**
     * 對話結束後要恢復的場景 BGM。
     */
    private String sceneBgmPath;


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 目前是否正在對話。
     */
    private boolean active = false;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 音效系統。
     *
     * AudioSystem 是全域服務，適合單例。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立對話系統。
     *
     * private：
     * - 確保只能透過 getInstance() 取得。
     */
    private DialogueSystem() {
        this.lines = DialogueDatabase.create(this);
    }


    // =========================================================
    // Start Dialogue
    // =========================================================

    /**
     * 開始對話。
     *
     * 流程：
     * 1. 若目前已經有對話，直接返回。
     * 2. 查找起始對話。
     * 3. 保存玩家與場景 BGM。
     * 4. 禁用玩家控制。
     * 5. 播放對話 BGM 或暫停目前 BGM。
     * 6. 建立 DialogueUI。
     * 7. 顯示第一句對話。
     *
     * @param startId 起始對話 ID
     * @param player 玩家 Entity
     * @param sceneBgmPath 對話結束後要恢復的場景 BGM
     * @param dialogueBgmPath 對話期間播放的 BGM
     */
    public void startDialogue(
            String startId,
            Entity player,
            String sceneBgmPath,
            String dialogueBgmPath
    ) {
        if (active) {
            return;
        }

        DialogueLine firstLine = getLine(startId);

        if (firstLine == null) {
            System.out.println("Dialogue not found: " + startId);
            return;
        }

        this.player = player;
        this.sceneBgmPath = sceneBgmPath;
        this.active = true;

        disablePlayerControl();

        playDialogueBGM(dialogueBgmPath);

        dialogueUI = new DialogueUI(this);
        addUINode(dialogueUI, 0, 0);

        dialogueUI.showLine(firstLine);
    }

    /**
     * 播放對話 BGM。
     *
     * 若 dialogueBgmPath 為空，則暫停目前 BGM。
     *
     * @param dialogueBgmPath 對話 BGM 路徑
     */
    private void playDialogueBGM(String dialogueBgmPath) {
        if (dialogueBgmPath != null && !dialogueBgmPath.isBlank()) {
            MusicSystem.getInstance().playBGM(dialogueBgmPath, true);
        } else {
            MusicSystem.getInstance().pauseBGM();
        }
    }


    // =========================================================
    // Dialogue Flow
    // =========================================================

    /**
     * 切換到指定對話行。
     *
     * @param id 對話 ID
     */
    public void goToLine(String id) {
        if (!active || dialogueUI == null) {
            return;
        }

        DialogueLine line = getLine(id);

        if (line == null) {
            System.out.println("Dialogue line not found: " + id);
            endDialogue();
            return;
        }

        dialogueUI.showLine(line);
    }

    /**
     * 從目前對話行進入下一步。
     *
     * 由 DialogueUI 在玩家點擊下一句時呼叫。
     *
     * @param currentLine 目前對話行
     */
    public void nextFrom(DialogueLine currentLine) {
        if (currentLine == null) {
            return;
        }

        if (currentLine.isEndDialogue()) {
            currentLine.runOnFinish();
            endDialogue();
            return;
        }

        String nextId = currentLine.getNextId();

        if (nextId == null || nextId.isBlank()) {
            currentLine.runOnFinish();
            endDialogue();
            return;
        }

        currentLine.runOnFinish();
        goToLine(nextId);
    }

    /**
     * 取得指定對話行。
     *
     * @param id 對話 ID
     * @return DialogueLine，若不存在則回傳 null
     */
    private DialogueLine getLine(String id) {
        return lines.get(id);
    }


    // =========================================================
    // End Dialogue
    // =========================================================

    /**
     * 立即結束對話。
     */
    public void endDialogue() {
        endDialogue(0, null);
    }

    /**
     * 延遲結束對話。
     *
     * @param delaySeconds 延遲秒數
     * @param afterEnd 對話結束後執行的事件，可為 null
     */
    private void endDialogue(
            double delaySeconds,
            Runnable afterEnd
    ) {
        if (!active) {
            return;
        }

        active = false;

        removeDialogueUI();

        Runnable finishEndDialogue = () -> {
            enablePlayerControl();
            restoreSceneBGM();

            player = null;
            sceneBgmPath = null;

            if (afterEnd != null) {
                afterEnd.run();
            }
        };

        if (delaySeconds <= 0) {
            finishEndDialogue.run();
            return;
        }

        PauseTransition wait =
                new PauseTransition(Duration.seconds(delaySeconds));

        wait.setOnFinished(event -> finishEndDialogue.run());
        wait.play();
    }

    /**
     * 移除 DialogueUI。
     */
    private void removeDialogueUI() {
        if (dialogueUI != null) {
            removeUINode(dialogueUI);
            dialogueUI = null;
        }
    }

    /**
     * 恢復場景 BGM。
     */
    private void restoreSceneBGM() {
        if (sceneBgmPath != null && !sceneBgmPath.isBlank()) {
            MusicSystem.getInstance().playBGM(sceneBgmPath, true);
        } else {
            MusicSystem.getInstance().resumeBGM();
        }
    }


    // =========================================================
    // Player Control
    // =========================================================

    /**
     * 禁用玩家控制。
     */
    private void disablePlayerControl() {
        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(false);
    }

    /**
     * 恢復玩家控制。
     */
    private void enablePlayerControl() {
        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(true);
    }

    /**
     * 取得目前玩家的 PlayerComponent。
     *
     * @return PlayerComponent，若不存在則回傳 null
     */
    private PlayerComponent getPlayerComponent() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }


    // =========================================================
    // Special Dialogue Events
    // =========================================================

    /**
     * 開始媽媽 Boss 戰小遊戲。
     *
     * package-private：
     * - 讓 DialogueDatabase 可以使用 method reference。
     */
    void startMomBattleMiniGame() {
        MomBattleMiniGame layer = new MomBattleMiniGame(
                DeathReason.MOM_BATTLE_LOSE_A,
                DeathReason.MOM_BATTLE_LOSE_B,
                DeathReason.MOM_BATTLE_LOSE_C
        );

        addUINode(layer, 0, 0);
        layer.start();
    }

    /**
     * 呼叫媽媽跳舞制裁。
     *
     * 流程：
     * 1. 播放媽媽跳舞音效。
     * 2. 強制玩家播放跳舞制裁動畫。
     * 3. 延遲結束對話。
     * 4. 對話結束後觸發 MOM_DANCE_OFF 死亡。
     *
     * package-private：
     * - 讓 DialogueDatabase 可以使用 method reference。
     */
    void callMomDanceOff() {
        audioSystem.playSFX(SoundId.MOM_DANCE_OFF);

        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent != null) {
            playerComponent.playMomDanceOffAnimation(1.2);
        }

        endDialogue(3.0, () ->
                DeathSystem.getInstance().die(MOM_DANCE_OFF)
        );
    }


    // =========================================================
    // State Getter
    // =========================================================

    /**
     * 取得目前是否正在對話。
     *
     * @return true 表示對話中
     */
    public boolean isActive() {
        return active;
    }
}