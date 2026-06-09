package ass.example.core;

import ass.example.core.Language;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * LanguageTextDatabase
 *
 * 翻譯文字資料庫。
 *
 * 功能：
 * 1. 建立繁體中文文字表。
 * 2. 建立英文文字表。
 * 3. 回傳 Language -> texts 的總表。
 *
 * 使用方式：
 * LanguageSystem 會在初始化時呼叫 LanguageTextDatabase.create()。
 */
public final class LanguageTextDatabase {
 
    // Constructor 

    /**
     * 工具資料類別不允許建立實例。
     */
    private LanguageTextDatabase() {
    }

 
    // Public API 

    /**
     * 建立所有語言文字資料。
     *
     * @return 所有語言文字表
     */
    public static Map<Language, Map<String, String>> create() {
        Map<Language, Map<String, String>> texts = new EnumMap<>(Language.class);

        Map<String, String> zhTw = new HashMap<>();
        Map<String, String> enUs = new HashMap<>();

        registerMainMenuTexts(zhTw, enUs);
        registerCommonMenuTexts(zhTw, enUs);
        registerStoryModeMenuTexts(zhTw, enUs);
        registerMiniGameMenuTexts(zhTw, enUs);
        registerAchievementMenuTexts(zhTw, enUs);
        registerSettingsTexts(zhTw, enUs);
        registerPauseMenuTexts(zhTw, enUs);
        registerDeathMenuTexts(zhTw, enUs);
        registerSaveTexts(zhTw, enUs);
        registerStoryInteractionTexts(zhTw, enUs);
        registerQuestTexts(zhTw, enUs);
        registerDialogueTexts(zhTw, enUs);
        registerModeNoticeTexts(zhTw, enUs);
        registerDeathReasonTexts(zhTw, enUs);

        texts.put(Language.ZH_TW, zhTw);
        texts.put(Language.EN_US, enUs);

        return texts;
    }

 
    // Main Menu 

    private static void registerMainMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.story", "故事模式", "Story Mode");
        put(zhTw, enUs, "menu.miniGame", "迷你模式", "Minigame Mode");
        put(zhTw, enUs, "menu.achievement", "成就", "Achievements");
        put(zhTw, enUs, "menu.settings", "設定", "Settings");
        put(zhTw, enUs, "menu.exit", "退出", "Exit");

        put(
                zhTw,
                enUs,
                "menu.exit.confirm",
                "確定要退出遊戲嗎？",
                "Are you sure you want to quit the game?"
        );
    }

 
    // Common Menu 

    private static void registerCommonMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.common.back", "回上一步", "Back");
        put(zhTw, enUs, "menu.common.apply", "套用", "Apply");
        put(zhTw, enUs, "menu.common.on", "開啟", "On");
        put(zhTw, enUs, "menu.common.off", "關閉", "Off");
        put(zhTw, enUs, "menu.common.confirm", "確定", "Confirm");
        put(zhTw, enUs, "menu.common.cancel", "取消", "Cancel");
    }

 
    // Story Mode Menu 

    private static void registerStoryModeMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(
                zhTw,
                enUs,
                "menu.storyMode.description",
                "選擇開始新遊戲，或從右側選擇既有存檔。",
                "Select an existing save, or start a new adventure."
        );

        put(zhTw, enUs, "menu.storyMode.newGame", "新遊戲", "New Game");
        put(zhTw, enUs, "menu.storyMode.loadSaves", "讀取存檔", "Load Saves");
        put(zhTw, enUs, "menu.storyMode.editSave", "編輯存檔", "Edit Saves");
    }

 
    // Mini Game Menu 

    private static void registerMiniGameMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(
                zhTw,
                enUs,
                "menu.miniGameMode.description",
                "選擇一個關卡。",
                "Choose a level."
        );

        put(
                zhTw,
                enUs,
                "menu.miniGameMode.StreetEndless",
                "街頭跑酷",
                "The Street Parkord"
        );

        put(
                zhTw,
                enUs,
                "menu.miniGameMode.comingSoon",
                "敬請期待",
                "Coming Soon"
        );

        put(
                zhTw,
                enUs,
                "menu.miniGameMode.comingSoon.description",
                "此關卡尚未開放。",
                "This level has yet to be finished."
        );
    }

 
    // Achievement Menu 

    private static void registerAchievementMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.achievement.locked", "尚未解鎖", "Locked");

        put(
                zhTw,
                enUs,
                "menu.achievement.description",
                "透過不同死亡方式解鎖。",
                "Unlock via different dying methods!"
        );

        put(zhTw, enUs, "achievement.unlocked", "已解鎖", "Unlocked");
    }

 
    // Settings 

    private static void registerSettingsTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(
                zhTw,
                enUs,
                "menu.settings.description",
                "選擇選項來調整遊戲設定",
                "Select to adjust game settings."
        );

        registerKeyConfigTexts(zhTw, enUs);
        registerVolumeTexts(zhTw, enUs);
        registerWindowTexts(zhTw, enUs);
        registerLanguageTexts(zhTw, enUs);
        registerResetTexts(zhTw, enUs);
        registerDevModeTexts(zhTw, enUs);
        registerAboutTexts(zhTw, enUs);
    }

    private static void registerKeyConfigTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.KeyConfig", "操作配置", "Controls");
        put(zhTw, enUs, "menu.settings.keyConfig.left", "：向左移動", ": Move Left");
        put(zhTw, enUs, "menu.settings.keyConfig.right", "：向右移動", ": Move Right");
        put(zhTw, enUs, "menu.settings.keyConfig.jump", "：跳躍（長按大跳）", ": Jump");
        put(zhTw, enUs, "menu.settings.keyConfig.drop", "從單向平台上降落", "Drop Through One-Way Platform");
        put(zhTw, enUs, "menu.settings.keyConfig.interact", "：互動", ": Interact");
        put(zhTw, enUs, "menu.settings.keyConfig.dash", "：短暫衝刺", ": Dash");
        put(zhTw, enUs, "menu.settings.keyConfig.pause", "：暫停遊戲", ": Pause Game");
    }

    private static void registerVolumeTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.volume", "調整音量", "Volume");
        put(zhTw, enUs, "menu.settings.volume.global", "全局音量", "Global");
        put(zhTw, enUs, "menu.settings.volume.music", "音樂音量", "Music");
        put(zhTw, enUs, "menu.settings.volume.sound", "音效音量", "Sound");
        put(zhTw, enUs, "menu.settings.volume.button_sound", "按鈕聲音", "Button Sfx");
    }

    private static void registerWindowTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.window", "視窗大小", "Screen Size");
        put(zhTw, enUs, "menu.settings.window.current", "目前視窗：", "Current Size: ");
        put(zhTw, enUs, "menu.settings.window.description", "選擇視窗模式", "Select a Mode");
        put(zhTw, enUs, "menu.settings.window.defaultSize", "預設視窗大小", "Default");
        put(zhTw, enUs, "menu.settings.window.customSize", "自定義視窗大小", "Custom");
        put(zhTw, enUs, "menu.settings.window.fullscreenSize", "全螢幕", "Fullscreen");
        put(zhTw, enUs, "menu.settings.window.apply", "套用", "Apply");
    }

    private static void registerLanguageTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.language", "切換語言", "Language");
        put(zhTw, enUs, "menu.settings.language.current", "目前語言：", "Current Language: ");
        put(zhTw, enUs, "menu.settings.language.changed", "語言設定已更新。", "Language setting updated.");
    }

    private static void registerResetTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.reset", "重置選項", "Reset Options");
        put(zhTw, enUs, "menu.settings.reset.description", "你可以在這裡清除本地資料。", "You can clear local data here.");
        put(zhTw, enUs, "menu.settings.reset.resetSettingsToDefault", "恢復設定預設值", "Restore Preset Values");
        put(zhTw, enUs, "menu.settings.reset.resetSettingsToDefault.notification", "設定已恢復預設值。", "Settings have been restored to default values.");
        put(zhTw, enUs, "menu.settings.reset.clearAchievement", "清除成就", "Clear Achievement");
        put(zhTw, enUs, "menu.settings.reset.clearAchievement.notification", "成就紀錄已清除。", "Achievements have been cleared.");
        put(zhTw, enUs, "menu.settings.reset.deleteLocalData", "格式化本地存檔", "Format Local Data");
        put(
                zhTw,
                enUs,
                "menu.settings.reset.deleteLocalData.comfirmNotice",
                "確定要格式化所有本地資料嗎？\n這會清除設定、成就與遊戲存檔。",
                "Are you sure to delete all the local data?\nThis includes settings, achievement and saves."
        );
        put(zhTw, enUs, "menu.settings.reset.deleteLocalData.notification", "本地存檔已格式化。", "Local data has been formatted.");
    }

    private static void registerDevModeTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.dev_mode", "開發模式", "Developer Mode");
        put(zhTw, enUs, "menu.settings.dev_mode.toggleOff", "切換成開發模式", "Switch to Dev Mode");
        put(zhTw, enUs, "menu.settings.dev_mode.toggleOn", "切換成普通模式", "Switch to Norm Mode");
        put(zhTw, enUs, "menu.settings.dev_mode.activated", "開發者模式已開啟。", "Developer Mode activated.");
        put(zhTw, enUs, "menu.settings.dev_mode.deactivated", "開發者模式已關閉。", "Developer Mode deactivated.");
    }

    private static void registerAboutTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.about", "關於遊戲", "About Game");
        put(zhTw, enUs, "menu.settings.about.info", "遊戲資訊", "Game Info");
        put(zhTw, enUs, "menu.settings.about.info.name", "遊戲名稱：台灣難度\n", "Game Title: Taiwanese Difficulty\n");
        put(zhTw, enUs, "menu.settings.about.info.genre", "類型：2D 橫向卷軸日常遊戲\n", "Genre: 2D Side-Scrolling Game\n");
        put(zhTw, enUs, "menu.settings.about.info.version", "版本：", "Version: ");
        put(zhTw, enUs, "menu.settings.about.description", "專案說明", "Project Description");
        put(zhTw, enUs, "menu.settings.about.description.construct", "本專案使用 JavaFX 作為主要開發平台，並搭配 FXGL 遊戲框架製作。\n", "This project is mainly developed with JavaFX and built using the FXGL game framework.\n");
        put(zhTw, enUs, "menu.settings.about.description.content", "以台灣日常生活為主題製作的一款遊戲(?)。", "A game about a perfectly normal day in Taiwan.(?)");

        registerCreditsTexts(zhTw, enUs);
    }

    private static void registerCreditsTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.settings.about.credits", "製作名單", "Credits");
        put(zhTw, enUs, "menu.settings.about.credits.game_design", "遊戲企劃", "Game Design");
        put(zhTw, enUs, "menu.settings.about.credits.level_design", "關卡設計", "Level Design");
        put(zhTw, enUs, "menu.settings.about.credits.narrative_design", "劇情設計", "Narrative Design");
        put(zhTw, enUs, "menu.settings.about.credits.game_programming", "遊戲程式", "Game Programming");
        put(zhTw, enUs, "menu.settings.about.credits.system_logic", "系統邏輯", "System Logic");
        put(zhTw, enUs, "menu.settings.about.credits.ui_programming", "UI 編寫", "UI Programming");
        put(zhTw, enUs, "menu.settings.about.credits.art_2d", "2D 美術", "2D Art");
        put(zhTw, enUs, "menu.settings.about.credits.character_design", "角色設計", "Character Design");
        put(zhTw, enUs, "menu.settings.about.credits.animation", "動畫繪製", "Animation");
        put(zhTw, enUs, "menu.settings.about.credits.sound_design", "音效設計", "Sound Effect");
        put(zhTw, enUs, "menu.settings.about.credits.translation.en", "英文本地化", "English Localization");
        put(zhTw, enUs, "menu.settings.about.credits.music", "音樂", "Music");
        put(zhTw, enUs, "menu.settings.about.credits.testing", "測試", "Testing");
        put(zhTw, enUs, "menu.settings.about.credits.online_assets", "網路素材", "Online Assets");
        put(zhTw, enUs, "menu.settings.about.credits.comma", "、", ", ");
    }

 
    // Pause Menu 

    private static void registerPauseMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "pause.resume", "繼續遊戲", "Resume");
        put(zhTw, enUs, "pause.save", "存檔", "Save");
        put(zhTw, enUs, "pause.settings", "設定", "Settings");
        put(zhTw, enUs, "pause.exitToMain", "退出到主畫面", "Exit to Main Menu");
        put(zhTw, enUs, "pause.save.placeholder", "存檔功能尚未完成。", "Save system is not implemented yet.");
        put(zhTw, enUs, "pause.exitToMain.confirm", "確定要回到主畫面嗎？未儲存的進度可能會遺失。", "Return to the main menu? Unsaved progress may be lost.");
        put(zhTw, enUs, "pause.save.disabled", "此模式無法使用存檔功能。", "Saving is not available in this mode.");
    }

 
    // Death Menu 

    private static void registerDeathMenuTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "menu.rebirth", "重生", "Regenerate");
        put(zhTw, enUs, "menu.deathCount", "死亡次數：", "Death Count: ");
        put(zhTw, enUs, "menu.score", "分數：", "Score: ");
        put(zhTw, enUs, "menu.highestScore", "最高分數：", "Highest Score: ");
        put(zhTw, enUs, "menu.newRecord", "新紀錄！", "New Record!");
    }

 
    // Save System 

    private static void registerSaveTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "save.load", "讀取存檔", "Load Save");
        put(zhTw, enUs, "save.edit", "編輯存檔", "Edit Saves");
        put(zhTw, enUs, "save.saveTo", "存檔至", "Save To");
        put(zhTw, enUs, "save.quickSave", "快速存檔", "Quick Save");
        put(zhTw, enUs, "save.quickSave.done", "已快速存檔。", "Quick save completed.");
        put(zhTw, enUs, "save.saved", "存檔完成。", "Game saved.");
        put(zhTw, enUs, "save.loaded", "讀取完成。", "Save loaded.");
        put(zhTw, enUs, "save.description", "管理目前遊戲的存檔資料。", "Manage the current game save data.");

        put(zhTw, enUs, "save.emptySlot", "空存檔槽", "Empty Slot");
        put(zhTw, enUs, "save.empty", "EMPTY", "EMPTY");
        put(zhTw, enUs, "save.noData", "尚無資料", "No Data");
        put(zhTw, enUs, "save.scene", "場景：", "Scene: ");
        put(zhTw, enUs, "save.name", "名稱：", "Name: ");
        put(zhTw, enUs, "save.createdAt", "建立日期：", "Created: ");
        put(zhTw, enUs, "save.savedAt", "存檔日期：", "Saved: ");
        put(zhTw, enUs, "save.lastOpenedAt", "上次開啟：", "Last Opened: ");

        put(zhTw, enUs, "save.rename", "重新命名", "Rename");
        put(zhTw, enUs, "save.rename.header", "修改存檔名稱", "Rename Save");
        put(zhTw, enUs, "save.rename.content", "請輸入新的存檔名稱：", "Enter a new save name:");

        put(zhTw, enUs, "save.delete", "刪除存檔", "Delete Save");
        put(zhTw, enUs, "save.delete.confirm", "確定要刪除此存檔嗎？", "Are you sure you want to delete this save?");

        put(zhTw, enUs, "save.confirm", "確認", "Confirm");
        put(zhTw, enUs, "save.warning.unsaved", "目前遊戲內容可能尚未存檔，讀取後會遺失變更。是否繼續？", "Your current progress may not be saved. Loading another save may discard unsaved changes. Continue?");
        put(zhTw, enUs, "save.overwrite.confirm", "此存檔槽已有資料，是否覆蓋？", "This slot already contains a save. Overwrite it?");
        put(zhTw, enUs, "save.inputName.header", "輸入存檔名稱", "Enter Save Name");
        put(zhTw, enUs, "save.inputName.content", "存檔名稱：", "Save name:");
    }

 
    // Story Interaction 

    private static void registerStoryInteractionTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "story.house.foldQuilt", "折被子", "Fold Quilt");
        put(zhTw, enUs, "story.house.drinkWater", "喝水", "Drink Water");
        put(zhTw, enUs, "story.house.brush_teeth", "刷牙", "Brush teeth");

        put(zhTw, enUs, "story.house.openDoor", "開門", "Open Door");
        put(zhTw, enUs, "story.house.closeDoor", "關門", "Close Door");

        put(zhTw, enUs, "story.house.wearShoes", "穿鞋", "Put on shoes");
        put(zhTw, enUs, "story.house.takeOffShoes", "脫鞋", "Take off shoes");

        put(zhTw, enUs, "story.house.talkToMom", "跟 媽咪 對話", "Talk to Mom");

        put(zhTw, enUs, "story.house.exit", "離開 家", "Leave Home");
        put(zhTw, enUs, "story.house.exit.locked", "現在還不能出門。", "You can't go outside yet.");

        put(zhTw, enUs, "story.street.enter", "返回 家", "Go Home");
    }

 
    // Quest 

    private static void registerQuestTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "quest.fold_quilt", "摺好被子", "Fold the quilt");
        put(zhTw, enUs, "quest.brush_teeth", "刷牙", "Brush your teeth");
        put(zhTw, enUs, "quest.talk_to_mom", "跟媽媽講話", "Talk to mom");
        put(zhTw, enUs, "quest.wear_shoes", "穿上鞋子", "Put on shoes");
        put(zhTw, enUs, "quest.exit_house", "離開 家", "Exit Home");
    }

 
    // Dialogue 

    private static void registerDialogueTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "dialog.character.mom", "媽媽", "Mom");

        put(zhTw, enUs, "dialog.mom.001", "崽，你還在玩那些尪仔喔。", "Wei, you still doing that video-making dream thing?");
        put(zhTw, enUs, "dialog.mom.002", "休息一下吧，去幫我買個東西好不好。", "Stop being a failure. Be a lawyer or go buy me something lah, okay?");
        put(zhTw, enUs, "dialog.mom.003.1", "幫我去買一打雞蛋、兩支青蔥跟一顆高麗菜。", "Buy a dozen eggs, two green onions, and a cabbage.");
        put(zhTw, enUs, "dialog.mom.003.2", "我才講你兩句，你就說我煩，翅膀硬了是不是？", "Are you talking back right now? Say one word...");
        put(zhTw, enUs, "dialog.mom.004.2", "媽媽看起來好像有點生氣了。", "Mom looks ticked off. You should...");

        put(zhTw, enUs, "dialog.mom.option.1.1", "好", "Sure");
        put(zhTw, enUs, "dialog.mom.option.1.2", "煩耶", "Nah");
        put(zhTw, enUs, "dialog.mom.option.2.1", "戰鬥", "\"Gubernatorial\"");
        put(zhTw, enUs, "dialog.mom.option.2.2", "跳舞", "Dance Off");
        put(zhTw, enUs, "dialog.mom.option.2.3", "逃跑", "Run");
    }

 
    // Mode Notice 

    private static void registerModeNoticeTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        put(zhTw, enUs, "pause.save.disabled", "此模式無法使用存檔功能。", "Saving is not available in this mode.");
    }

 
    // Death Reasons 

    private static void registerDeathReasonTexts(
            Map<String, String> zhTw,
            Map<String, String> enUs
    ) {
        putDeath(
                zhTw,
                enUs,
                "LEFT_BEDROOM_WITHOUT_FOLDING_QUILT",
                "沒折被子",
                "人家是西點軍校，你們是西點麵包啊。",
                "FORGET TO FOLD THE QUILT",
                "Seems like someone is folded."
        );

        putDeath(
                zhTw,
                enUs,
                "HIT_CEILING",
                "撞到天花板",
                "It's-a-me Mario.",
                "HIT THE CEILING",
                "It's-a-me Mario."
        );

        putDeath(
                zhTw,
                enUs,
                "HIT_SHOWER_CURTAIN_ROD",
                "撞到浴簾桿",
                "鋼管掉落.mp3",
                "HIT SHOWER CURTAIN ROD",
                "Metal Pipe Falling.mp3"
        );

        putDeath(
                zhTw,
                enUs,
                "HIT_DOORFRAME",
                "撞到門框",
                "\"Watch your head\"的中文直譯是「看你個頭」，相信我。",
                "HIT DOORFRAME",
                "The door framed you."
        );

        putDeath(
                zhTw,
                enUs,
                "JUMPING_ON_BED",
                "在床上跳被媽媽制裁了",
                "超大雙人床ㄟ。",
                "JUMPING ON BED",
                "You know, breaking bed is illegal."
        );

        putDeath(
                zhTw,
                enUs,
                "DRINK_WATER",
                "喝下過夜水",
                "眾所周知，水放過夜不能喝，但水放一整天可以喝。",
                "DRINK THE OVERNIGHT WATER",
                "Water lasted all day long: totally fine; Water left overnight: poisoned."
        );

        putDeath(
                zhTw,
                enUs,
                "TRIPPED_BY_SIDEWALK_TILE",
                "被凸起磁磚絆倒",
                "人生有起也有落落落落落。",
                "TRIPPED BY PROTRUDING TILES",
                "DIVE HEAD-FIRST FOR THE BAG! And... Out!"
        );

        putDeath(
                zhTw,
                enUs,
                "HIT_BY_SCOOTER",
                "被摩托車創飛",
                "麥可 are you okay?",
                "SENT FLYING BY A SCOOTER",
                "Ah ha! Classic."
        );

        putDeath(
                zhTw,
                enUs,
                "FALLING_FRIDGE",
                "被墜落的冰箱擊中",
                "F＝ma-ma-mia",
                "CRASHED BY A FALLEN FRIDGE",
                "F＝ma-mma-mia"
        );

        putDeath(
                zhTw,
                enUs,
                "FALLING_HELI",
                "被墜落的阿帕契擊中",
                "甚麼都掉，甚麼都不奇怪。",
                "CRASHED BY AN APACHE",
                "Nothing weird these days."
        );

        putDeath(
                zhTw,
                enUs,
                "JUMPED_IN_BATHTUB",
                "在浴缸裡滑倒",
                "要泡蛇酒是嗎？",
                "SLIPPED IN THE BATHTUB",
                "Final Destination Side Story - Granny's Bathtub"
        );

        putDeath(
                zhTw,
                enUs,
                "LEFT_WITHOUT_BRUSHING_TEETH",
                "起床沒刷牙",
                "早上起來刷刷牙。",
                "FORGET TO BRUSH TEETH",
                "I guess it starts from a quarter to one."
        );

        putDeath(
                zhTw,
                enUs,
                "ENTER_LIVING_ROOM_WITH_SHOES",
                "穿鞋在室內亂跑被制裁",
                "被管家活活打斷雙腿。",
                "WANDERING AROUND THE HOUSE WITH SHOES ON",
                "Sent to Jesus by Mom."
        );

        putDeath(
                zhTw,
                enUs,
                "LOCK_YOURSELF_IN_THE_CLOSET",
                "把自己關在衣櫃",
                "衣櫃並不能免於核爆，冰箱也不行。",
                "TRAP YOURSELF IN THE CLOSET",
                "ThE ClOsEt eNdInG Is mY FaVoRiTe!"
        );

        putDeath(
                zhTw,
                enUs,
                "MOM_BATTLE_LOSE_A",
                "被媽媽擊敗",
                "Yo Battle。",
                "DEFEATED BY MOM",
                "One-shotted by la chancla."
        );

        putDeath(
                zhTw,
                enUs,
                "MOM_BATTLE_LOSE_B",
                "被媽媽擊敗",
                "勝敗乃兵家常事，下一次你還是會輸的。",
                "DEFEATED BY MOM",
                "nt, stop trying."
        );

        putDeath(
                zhTw,
                enUs,
                "MOM_BATTLE_LOSE_C",
                "被媽媽擊敗",
                "你有甚麼問題，後面真沒了。",
                "DEFEATED BY MOM",
                "What's wrong with you, there's no more."
        );

        putDeath(
                zhTw,
                enUs,
                "MOM_DANCE_OFF",
                "激怒媽媽",
                "假如你生氣，假如你生氣，仰望耶穌，仰望耶穌。",
                "TRIGGERED MOM",
                "What do you expect?"
        );
    }

 
    // Helpers 

    /**
     * 同時加入中英文文字。
     */
    private static void put(
            Map<String, String> zhTw,
            Map<String, String> enUs,
            String key,
            String zhText,
            String enText
    ) {
        zhTw.put(key, zhText);
        enUs.put(key, enText);
    }

    /**
     * 加入死亡原因標題與副標題。
     *
     * key 規則：
     * death.{deathId}.title
     * death.{deathId}.subtitle
     */
    private static void putDeath(
            Map<String, String> zhTw,
            Map<String, String> enUs,
            String deathId,
            String zhTitle,
            String zhSubtitle,
            String enTitle,
            String enSubtitle
    ) {
        put(
                zhTw,
                enUs,
                "death." + deathId + ".title",
                zhTitle,
                enTitle
        );

        put(
                zhTw,
                enUs,
                "death." + deathId + ".subtitle",
                zhSubtitle,
                enSubtitle
        );
    }
}