package ass.example.system.dialogue;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.MusicSystem;
import ass.example.system.quest.QuestSystem;
import ass.example.ui.DialogueUI;
import ass.example.ui.MomBattleMiniGame;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ass.example.core.DeathReason.MOM_DANCE_OFF;
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

    private final AudioSystem audioSystem = AudioSystem.getInstance();
    private final DeathSystem deathSystem = DeathSystem.getInstance();

    private DialogueSystem() {
        registerDialogues();
    }

    private void registerDialogues() {
        /*
         * 之後拆到 DialogueDatabase 。
         */
        lines.put("mom_001", new DialogueLine(
                "mom_001",
                "/assets/textures/characters/mom/mom_chat.png",
                "/assets/textures/characters/mom/mom_chat_speak.png",
                "dialog.character.mom",
                "dialog.mom.001",
                true,
                "mom_002",
                false
        ));

        DialogueLine mom_002 = new DialogueLine(
                "mom_002",
                "/assets/textures/characters/mom/mom_chat.png",
                "/assets/textures/characters/mom/mom_chat_speak.png",
                "dialog.character.mom",
                "dialog.mom.002",
                false,
                null,
                false
        );
        mom_002.addButton(new DialogueButton("dialog.mom.option.1.1", () -> goToLine("mom_003_1")));
        mom_002.addButton(new DialogueButton("dialog.mom.option.1.2", () -> goToLine("mom_003_2")));
        lines.put("mom_002", mom_002);

        lines.put("mom_003_1", new DialogueLine(
                "mom_003_1",
                "/assets/textures/characters/mom/mom_chat.png",
                "/assets/textures/characters/mom/mom_chat_speak.png",
                "dialog.character.mom",
                "dialog.mom.003.1",
                true,
                null,
                true
        ).onFinish(() -> {
            QuestSystem.getInstance().completeQuest(QuestType.TALK_TO_MOM);
        }));

        lines.put("mom_003_2", new DialogueLine(
                "mom_003_2",
                "/assets/textures/characters/mom/mom_chat_rage.png",
                "/assets/textures/characters/mom/mom_chat_rage_speak.png",
                "dialog.character.mom",
                "dialog.mom.003.2",
                true,
                "mom_004_2",
                false
        ));

        DialogueLine mom_004_2 = new DialogueLine(
                "mom_004_2",
                "/assets/textures/characters/mom/mom_chat_rage.png",
                "/assets/textures/characters/mom/mom_chat_rage_speak.png",
                null,
                "dialog.mom.004.2",
                false,
                null,
                false
        );
        mom_004_2.addButton(new DialogueButton("dialog.mom.option.2.1", this::startMomBattleMiniGame));
        mom_004_2.addButton(new DialogueButton("dialog.mom.option.2.2", this::callMomDanceOff));
        mom_004_2.addButton(new DialogueButton("dialog.mom.option.2.3", this::endDialogue));
        lines.put("mom_004_2", mom_004_2);
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
        endDialogue(0, null);
    }

    private void endDialogue(double delaySeconds, Runnable afterEnd) {
        if (!active) {
            return;
        }

        active = false;

        if (dialogueUI != null) {
            removeUINode(dialogueUI);
            dialogueUI = null;
        }

        Runnable finishEndDialogue = () -> {
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

            if (afterEnd != null) {
                afterEnd.run();
            }
        };

        if (delaySeconds <= 0) {
            finishEndDialogue.run();
            return;
        }

        PauseTransition wait = new PauseTransition(Duration.seconds(delaySeconds));
        wait.setOnFinished(e -> finishEndDialogue.run());
        wait.play();
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

    private void startMomBattleMiniGame() {
        MomBattleMiniGame layer = new MomBattleMiniGame(
                this,
                DeathReason.MOM_BATTLE_LOSE_A,
                DeathReason.MOM_BATTLE_LOSE_B,
                DeathReason.MOM_BATTLE_LOSE_C
        );

        addUINode(layer, 0, 0);
        layer.start();
    }

    private void callMomDanceOff() {
        audioSystem.playSFX(SoundId.MOM_DANCE_OFF);

        if (player != null) {
            try {
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                pc.playMomDanceOffAnimation(1.2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        endDialogue(3.0, () -> {
            deathSystem.die(MOM_DANCE_OFF);
        });
    }
}