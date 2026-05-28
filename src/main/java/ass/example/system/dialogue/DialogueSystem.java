package ass.example.system.dialogue;

import ass.example.components.PlayerComponent;
import ass.example.core.QuestType;
import ass.example.system.MusicSystem;
import ass.example.system.quest.QuestSystem;
import ass.example.ui.DialogueUI;
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
         * 之後拆到 DialogueDatabase 。
         */
        lines.put("mom_001", new DialogueLine(
                "mom_001",
                "/assets/textures/characters/mom/mom_default.png",
                "/assets/textures/characters/mom/mom_speaking.png",
                "媽媽",
                "崽，你還在玩那些尪仔喔。",
                true,
                "mom_002",
                false
        ));

        DialogueLine mom_002 = new DialogueLine(
                "mom_002",
                "/assets/textures/characters/mom/mom.png",
                "/assets/textures/characters/mom/mom_speaking.png",
                "媽媽",
                "休息一下吧，去幫我買個東西好不好。",
                false,
                null,
                false
        );
        mom_002.addButton(new DialogueButton("好", () -> goToLine("mom_003_1")));
        mom_002.addButton(new DialogueButton("煩耶", () -> goToLine("mom_003_2")));
        lines.put("mom_002", mom_002);

        lines.put("mom_003_1", new DialogueLine(
                "mom_003_1",
                "/assets/textures/characters/mom/mom.png",
                "/assets/textures/characters/mom/mom_speaking.png",
                "媽媽",
                "幫我去買一打雞蛋、兩支青蔥跟一顆高麗菜。",
                true,
                null,
                true
        ).onFinish(() -> {
            QuestSystem.getInstance().completeQuest(QuestType.TALK_TO_MOM);
        }));

        lines.put("mom_003_2", new DialogueLine(
                "mom_003_2",
                "/assets/textures/characters/mom/mom_rage.png",
                "/assets/textures/characters/mom/mom_rage_speaking.png",
                "媽媽",
                "我才講你兩句，你就說我煩，翅膀硬了是不是？",
                true,
                "mom_004_2",
                false
        ));

        DialogueLine mom_004_2 = new DialogueLine(
                "mom_004_2",
                "/assets/textures/characters/mom/mom_rage.png",
                "/assets/textures/characters/mom/mom_rage_speaking.png",
                null,
                "媽媽看起來好像有點生氣了。",
                false,
                null,
                false
        );
        mom_002.addButton(new DialogueButton("戰鬥", this::endDialogue));
        mom_002.addButton(new DialogueButton("跳舞", this::endDialogue));
        mom_002.addButton(new DialogueButton("逃跑", this::endDialogue));
        lines.put("mom_002", mom_002);
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