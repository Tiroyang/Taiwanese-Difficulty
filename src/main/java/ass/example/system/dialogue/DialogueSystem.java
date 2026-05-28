package ass.example.system.dialogue;

import ass.example.components.PlayerComponent;
import ass.example.system.MusicSystem;
import com.almasb.fxgl.entity.Entity;

import java.util.HashMap;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class DialogueSystem {

    private static final DialogueSystem INSTANCE = new DialogueSystem();

    public static DialogueSystem getInstance() {
        return INSTANCE;
    }

    private final Map<String, DialogueLine> lines = new HashMap<>();

    private DialogueUI dialogueUI;

    private Entity player;
    private String sceneBgmPath;

    private boolean active = false;

    private DialogueSystem() {
        registerDialogues();
    }

    private void registerDialogues() {
        /*
         * 範例：媽媽第一次對話。
         * 之後你可以把這裡拆到 DialogueDatabase，但目前先不需要新增太多檔案。
         */
        lines.put("mom_intro_001", new DialogueLine(
                "mom_intro_001",
                "/assets/textures/dialogue/mom_default.png",
                "/assets/textures/dialogue/mom_speaking.png",
                "媽媽",
                "你是不是又想穿著鞋子在客廳裡亂跑？",
                true,
                "mom_intro_002",
                false
        ));

        lines.put("mom_intro_002", new DialogueLine(
                "mom_intro_002",
                "/assets/textures/dialogue/mom_default.png",
                "/assets/textures/dialogue/mom_speaking.png",
                "媽媽",
                "先去把該做的事情做完，再來想出門。",
                true,
                "mom_intro_choice",
                false
        ));

        DialogueLine choice = new DialogueLine(
                "mom_intro_choice",
                "/assets/textures/dialogue/mom_default.png",
                "/assets/textures/dialogue/mom_speaking.png",
                "媽媽",
                "聽懂了嗎？",
                false,
                null,
                false
        );

        choice.addButton(new DialogueButton("知道了", () -> goToLine("mom_intro_end")));
        choice.addButton(new DialogueButton("假裝沒聽到", () -> goToLine("mom_intro_angry")));

        lines.put("mom_intro_choice", choice);

        lines.put("mom_intro_angry", new DialogueLine(
                "mom_intro_angry",
                "/assets/textures/dialogue/mom_default.png",
                "/assets/textures/dialogue/mom_speaking.png",
                "媽媽",
                "你再裝傻看看。",
                true,
                "mom_intro_end",
                false
        ));

        lines.put("mom_intro_end", new DialogueLine(
                "mom_intro_end",
                "/assets/textures/dialogue/mom_default.png",
                "/assets/textures/dialogue/mom_speaking.png",
                "媽媽",
                "去吧。",
                true,
                null,
                true
        ));
    }

    public void startDialogue(
            String startId,
            Entity player,
            String sceneBgmPath,
            String dialogueBgmPath
    ) {
        if (active) {
            return;
        }

        DialogueLine firstLine = lines.get(startId);

        if (firstLine == null) {
            System.out.println("Dialogue not found: " + startId);
            return;
        }

        this.player = player;
        this.sceneBgmPath = sceneBgmPath;

        active = true;

        disablePlayerControl();

        /*
         * 對話開啟後，停止目前關卡 BGM，改播對話專屬 BGM。
         */
        if (dialogueBgmPath != null && !dialogueBgmPath.isBlank()) {
            MusicSystem.getInstance().playBGM(dialogueBgmPath, true);
        } else {
            MusicSystem.getInstance().pauseBGM();
        }

        dialogueUI = new DialogueUI(this);
        addUINode(dialogueUI, 0, 0);

        dialogueUI.showLine(firstLine);
    }

    public void goToLine(String id) {
        if (!active || dialogueUI == null) {
            return;
        }

        DialogueLine line = lines.get(id);

        if (line == null) {
            System.out.println("Dialogue line not found: " + id);
            endDialogue();
            return;
        }

        dialogueUI.showLine(line);
    }

    public void nextFrom(DialogueLine currentLine) {
        if (currentLine == null) {
            return;
        }

        if (currentLine.isEndDialogue()) {
            endDialogue();
            return;
        }

        String nextId = currentLine.getNextId();

        if (nextId == null || nextId.isBlank()) {
            endDialogue();
            return;
        }

        goToLine(nextId);
    }

    public void endDialogue() {
        if (!active) {
            return;
        }

        active = false;

        if (dialogueUI != null) {
            removeUINode(dialogueUI);
            dialogueUI = null;
        }

        enablePlayerControl();

        /*
         * 對話結束後，播回場景 BGM。
         * 如果你希望回到原本播放秒數，要再擴充 MusicSystem 記錄 current time。
         */
        if (sceneBgmPath != null && !sceneBgmPath.isBlank()) {
            MusicSystem.getInstance().playBGM(sceneBgmPath, true);
        } else {
            MusicSystem.getInstance().resumeBGM();
        }

        player = null;
        sceneBgmPath = null;
    }

    private void disablePlayerControl() {
        if (player == null) {
            return;
        }

        try {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.stopAllMovement();
            pc.setControlEnabled(false);
        } catch (Exception ignored) {
        }
    }

    private void enablePlayerControl() {
        if (player == null) {
            return;
        }

        try {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.stopAllMovement();
            pc.setControlEnabled(true);
        } catch (Exception ignored) {
        }
    }

    public boolean isActive() {
        return active;
    }
}