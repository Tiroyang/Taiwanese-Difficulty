package ass.example.core;

import ass.example.system.QuestSystem;
import ass.example.system.dialogue.DialogueLine;
import ass.example.system.dialogue.DialogueSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * DialogueDatabase
 *
 * 對話資料庫。
 *
 * 功能：
 * 1. 集中建立所有 DialogueLine。
 * 2. 讓 DialogueSystem 不再塞滿對話資料。
 * 3. 將「對話資料」與「對話流程控制」分離。
 *
 * 設計說明：
 * - 此類別不保存 runtime 狀態。
 * - 不需要做成單例。
 * - 使用 static create(...) 建立對話資料即可。
 */
public final class DialogueDatabase {

    // =========================================================
    // Portrait Paths - Mom
    // =========================================================

    private static final String MOM_DEFAULT =
            "/assets/textures/characters/mom/mom_chat.png";

    private static final String MOM_SPEAKING =
            "/assets/textures/characters/mom/mom_chat_speak.png";

    private static final String MOM_RAGE_DEFAULT =
            "/assets/textures/characters/mom/mom_chat_rage.png";

    private static final String MOM_RAGE_SPEAKING =
            "/assets/textures/characters/mom/mom_chat_rage_speak.png";


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 工具類別不允許建立實例。
     */
    private DialogueDatabase() {
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 建立所有對話資料。
     *
     * @param dialogueSystem DialogueSystem，用於選項按鈕呼叫流程方法
     * @return 對話資料表
     */
    public static Map<String, DialogueLine> create(
            DialogueSystem dialogueSystem
    ) {
        Map<String, DialogueLine> lines = new HashMap<>();

        registerMomDialogue(lines, dialogueSystem);

        return lines;
    }


    // =========================================================
    // Mom Dialogue
    // =========================================================

    /**
     * 註冊媽媽對話。
     */
    private static void registerMomDialogue(
            Map<String, DialogueLine> lines,
            DialogueSystem dialogueSystem
    ) {
        put(lines, new DialogueLine(
                "mom_001",
                MOM_DEFAULT,
                MOM_SPEAKING,
                "dialog.character.mom",
                "dialog.mom.001",
                true,
                "mom_002",
                false
        ));

        put(lines, new DialogueLine(
                        "mom_002",
                        MOM_DEFAULT,
                        MOM_SPEAKING,
                        "dialog.character.mom",
                        "dialog.mom.002",
                        false,
                        null,
                        false
                )
                        .addButton(
                                "dialog.mom.option.1.1",
                                () -> dialogueSystem.goToLine("mom_003_1")
                        )
                        .addButton(
                                "dialog.mom.option.1.2",
                                () -> dialogueSystem.goToLine("mom_003_2")
                        )
        );

        put(lines, new DialogueLine(
                "mom_003_1",
                MOM_DEFAULT,
                MOM_SPEAKING,
                "dialog.character.mom",
                "dialog.mom.003.1",
                true,
                null,
                true
        ).onFinish(() ->
                QuestSystem.getInstance().completeQuest(QuestType.TALK_TO_MOM)
        ));

        put(lines, new DialogueLine(
                "mom_003_2",
                MOM_RAGE_DEFAULT,
                MOM_RAGE_SPEAKING,
                "dialog.character.mom",
                "dialog.mom.003.2",
                true,
                "mom_004_2",
                false
        ));

        put(lines, new DialogueLine(
                        "mom_004_2",
                        MOM_RAGE_DEFAULT,
                        MOM_RAGE_SPEAKING,
                        null,
                        "dialog.mom.004.2",
                        false,
                        null,
                        false
                )
                        .addButton(
                                "dialog.mom.option.2.1",
                                dialogueSystem::startMomBattleMiniGame
                        )
                        .addButton(
                                "dialog.mom.option.2.2",
                                dialogueSystem::callMomDanceOff
                        )
                        .addButton(
                                "dialog.mom.option.2.3",
                                dialogueSystem::endDialogue
                        )
        );
    }


    // =========================================================
    // Helpers
    // =========================================================

    /**
     * 將 DialogueLine 放入資料表。
     *
     * @param lines 對話資料表
     * @param line 對話行
     */
    private static void put(
            Map<String, DialogueLine> lines,
            DialogueLine line
    ) {
        lines.put(line.getId(), line);
    }
}