package ass.example.system;

import ass.example.core.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class LanguageSystem {

    private static final LanguageSystem INSTANCE = new LanguageSystem();

    public static LanguageSystem getInstance() {
        return INSTANCE;
    }

    private final Preferences prefs =
            Preferences.userNodeForPackage(LanguageSystem.class);

    private Language currentLanguage = Language.ZH_TW;

    private final Map<String, String> zhTw = new HashMap<>();
    private final Map<String, String> enUs = new HashMap<>();

    private LanguageSystem() {
        initTexts();
        loadSettings();
    }

    private void initTexts() {
        // Main Menu
        zhTw.put("menu.story", "故事模式");
        zhTw.put("menu.miniGame", "迷你模式");
        zhTw.put("menu.achievement", "成就");
        zhTw.put("menu.settings", "設定");
        zhTw.put("menu.exit", "退出");

        enUs.put("menu.story", "Story Mode");
        enUs.put("menu.miniGame", "Minigame Mode");
        enUs.put("menu.achievement", "Achievements");
        enUs.put("menu.settings", "Settings");
        enUs.put("menu.exit", "Exit");

        // Common
        zhTw.put("menu.common.back", "回上一步");
        zhTw.put("menu.common.apply", "套用");
        zhTw.put("menu.common.on", "開啟");
        zhTw.put("menu.common.off", "關閉");
        zhTw.put("menu.common.confirm", "確定");
        zhTw.put("menu.common.cancel", "取消");

        enUs.put("menu.common.back", "Back");
        enUs.put("menu.common.apply", "Apply");
        enUs.put("menu.common.on", "On");
        enUs.put("menu.common.off", "Off");
        enUs.put("menu.common.confirm", "Confirm");
        enUs.put("menu.common.cancel", "Cancel");

        // Story Mode Page
        zhTw.put("menu.storyMode.description", "選擇開始新遊戲，或從右側選擇既有存檔。");

        zhTw.put("menu.storyMode.newGame", "新遊戲");
        zhTw.put("menu.storyMode.loadSaves", "讀取存檔");
        zhTw.put("menu.storyMode.editSave", "編輯存檔");

        enUs.put("menu.storyMode.description", "Select an existing save, or start a new adventure.");

        enUs.put("menu.storyMode.newGame", "New Game");
        enUs.put("menu.storyMode.loadSaves", "Load Saves");
        enUs.put("menu.storyMode.editSave", "Edit Saves");

        // miniGame Mode Page
        zhTw.put("menu.miniGameMode.description", "選擇一個關卡。");

        zhTw.put("menu.miniGameMode.StreetEndless", "街頭跑酷");
        zhTw.put("menu.miniGameMode.comingSoon", "敬請期待");

        zhTw.put("menu.miniGameMode.comingSoon.description", "此關卡尚未開放。");

        enUs.put("menu.miniGameMode.description", "Choose a level.");

        enUs.put("menu.miniGameMode.StreetEndless", "The Street Parkord");
        enUs.put("menu.miniGameMode.comingSoon", "Coming Soon");

        enUs.put("menu.miniGameMode.comingSoon.description", "This level has yet to be finished.");

        //Achievement Page
        zhTw.put("menu.achievement.locked", "尚未解鎖");
        zhTw.put("menu.achievement.description", "透過不同死亡方式解鎖。");

        enUs.put("menu.achievement.locked", "Locked");
        enUs.put("menu.achievement.description", "Lock via different dying methods!");

        // Settings Page
        zhTw.put("menu.settings.description", "選擇選項來調整遊戲設定");

        enUs.put("menu.settings.description", "Select to adjust game settings.");

        zhTw.put("menu.settings.KeyConfig", "操作配置");
        zhTw.put("menu.settings.keyConfig.left", "：向左移動");
        zhTw.put("menu.settings.keyConfig.right", "：向右移動");
        zhTw.put("menu.settings.keyConfig.jump", "：跳躍（長按大跳）");
        zhTw.put("menu.settings.keyConfig.drop", "從單向平台上降落");
        zhTw.put("menu.settings.keyConfig.interact", "：互動");
        zhTw.put("menu.settings.keyConfig.dash", "：短暫衝刺");
        zhTw.put("menu.settings.keyConfig.pause", "：暫停遊戲");

        enUs.put("menu.settings.KeyConfig", "Controls");
        enUs.put("menu.settings.keyConfig.left", ": Move Left");
        enUs.put("menu.settings.keyConfig.right", ": Move Right");
        enUs.put("menu.settings.keyConfig.jump", ": Jump");
        enUs.put("menu.settings.keyConfig.drop", "Drop Through One-Way Platform");
        enUs.put("menu.settings.keyConfig.interact", ": Interact");
        enUs.put("menu.settings.keyConfig.dash", ": Dash");
        enUs.put("menu.settings.keyConfig.pause", ": Pause Game");

        zhTw.put("menu.settings.volume", "調整音量");
        zhTw.put("menu.settings.volume.global", "全局音量");
        zhTw.put("menu.settings.volume.music", "音樂音量");
        zhTw.put("menu.settings.volume.sound", "音校音量");
        zhTw.put("menu.settings.volume.button_sound", "按鈕聲音");

        enUs.put("menu.settings.volume", "Volume");
        enUs.put("menu.settings.volume.global", "Global");
        enUs.put("menu.settings.volume.music", "Music");
        enUs.put("menu.settings.volume.sound", "Sound");
        enUs.put("menu.settings.volume.button_sound", "Button Sfx");

        zhTw.put("menu.settings.window", "視窗大小");
        zhTw.put("menu.settings.window.current", "目前視窗：");
        zhTw.put("menu.settings.window.description", "選擇視窗模式");
        zhTw.put("menu.settings.window.defaultSize", "預設視窗大小");
        zhTw.put("menu.settings.window.customSize", "自定義視窗大小");
        zhTw.put("menu.settings.window.fullscreenSize", "全螢幕");
        zhTw.put("menu.settings.window.apply", "套用");

        enUs.put("menu.settings.window", "Screen Size");
        enUs.put("menu.settings.window.current", "Current Size: ");
        enUs.put("menu.settings.window.description", "Select a Mode");
        enUs.put("menu.settings.window.defaultSize", "Default");
        enUs.put("menu.settings.window.customSize", "Custom");
        enUs.put("menu.settings.window.fullscreenSize", "Fullscreen");
        enUs.put("menu.settings.window.apply", "Apply");

        zhTw.put("menu.settings.language", "切換語言");
        zhTw.put("menu.settings.language.current", "目前語言：");

        enUs.put("menu.settings.language", "Language");
        enUs.put("menu.settings.language.current", "Current Language: ");

        zhTw.put("menu.settings.reset", "重置選項");
        zhTw.put("menu.settings.reset.description", "你可以在這裡清除本地資料。");
        zhTw.put("menu.settings.reset.resetSettingsToDefault", "恢復設定預設值");
        zhTw.put("menu.settings.reset.resetSettingsToDefault.notification", "設定已恢復預設值。");
        zhTw.put("menu.settings.reset.clearAchievement", "清除成就");
        zhTw.put("menu.settings.reset.clearAchievement.notification", "成就紀錄已清除。");
        zhTw.put("menu.settings.reset.deleteLocalData", "格式化本地存檔");
        zhTw.put("menu.settings.reset.deleteLocalData.comfirmNotice", "確定要格式化所有本地資料嗎？\n這會清除設定、成就與遊戲存檔。");
        zhTw.put("menu.settings.reset.deleteLocalData.notification", "本地存檔已格式化。");

        enUs.put("menu.settings.reset", "Reset Options");
        enUs.put("menu.settings.reset.description", "You can clear local data here.");
        enUs.put("menu.settings.reset.resetSettingsToDefault", "Restore Preset Values");
        enUs.put("menu.settings.reset.resetSettingsToDefault.notification", "Settings have been restored to default values.");
        enUs.put("menu.settings.reset.clearAchievement", "Clear Achievement");
        enUs.put("menu.settings.reset.clearAchievement.notification", "Achievements have been cleared.");
        enUs.put("menu.settings.reset.deleteLocalData", "Format Local Data");
        enUs.put("menu.settings.reset.deleteLocalData.comfirmNotice", "Are you sure to delete all the local data?\nThis includes settings, achievement and saves.");
        enUs.put("menu.settings.reset.deleteLocalData.notification", "Local data has been formated.");

        zhTw.put("menu.settings.dev_mode", "開發模式");
        zhTw.put("menu.settings.dev_mode.toggleOff", "切換成開發模式");
        zhTw.put("menu.settings.dev_mode.toggleOn", "切換成普通模式");
        zhTw.put("menu.settings.dev_mode.activated", "開發者模式已開啟。");
        zhTw.put("menu.settings.dev_mode.deactivated", "開發者模式已關閉。");

        enUs.put("menu.settings.dev_mode", "Developer Mode");
        enUs.put("menu.settings.dev_mode.toggleOff", "Switch to Dev Mode");
        enUs.put("menu.settings.dev_mode.toggleOn", "Switch to Norm mode");
        enUs.put("menu.settings.dev_mode.activated", "Developer Mode activated.");
        enUs.put("menu.settings.dev_mode.deactivated", "Developer Mode deactivated.");

        zhTw.put("menu.settings.about", "關於遊戲");
        zhTw.put("menu.settings.about.info", "遊戲資訊");
        zhTw.put("menu.settings.about.info.name", "遊戲名稱：台灣難度\n");
        zhTw.put("menu.settings.about.info.genre", "類型：2D 橫向卷軸日常遊戲\n");
        zhTw.put("menu.settings.about.info.version", "版本：");
        zhTw.put("menu.settings.about.description", "專案說明");
        zhTw.put("menu.settings.about.description.construct", "本專案使用 JavaFX 作為主要開發平台，並搭配 FXGL 遊戲框架製作。\n");
        zhTw.put("menu.settings.about.description.content", "以台灣日常生活為主題製作的一款遊戲(?)。");

        enUs.put("menu.settings.about", "About Game");
        enUs.put("menu.settings.about.info", "Game Info");
        enUs.put("menu.settings.about.info.name", "Game Title: Taiwanese Difficulty\n");
        enUs.put("menu.settings.about.info.genre", "Genre: 2D Side-Scrolling Game\n");
        enUs.put("menu.settings.about.info.version", "Version: ");

        enUs.put("menu.settings.about.description", "Project Description");
        enUs.put("menu.settings.about.description.construct", "This is a project mainly developed with JavaFX and built using the FXGL game framework.\n");
        enUs.put("menu.settings.about.description.content", "A game about a perfectly normal day in Taiwan.(?)");

        zhTw.put("menu.settings.about.credits", "製作名單");
        zhTw.put("menu.settings.about.credits.game_design", "遊戲企劃");
        zhTw.put("menu.settings.about.credits.level_design", "關卡設計");
        zhTw.put("menu.settings.about.credits.narrative_design", "劇情設計");
        zhTw.put("menu.settings.about.credits.game_programming", "遊戲程式");
        zhTw.put("menu.settings.about.credits.system_logic", "系統邏輯");
        zhTw.put("menu.settings.about.credits.ui_programming", "UI 編寫");
        zhTw.put("menu.settings.about.credits.art_2d", "2D 美術");
        zhTw.put("menu.settings.about.credits.character_design", "角色設計");
        zhTw.put("menu.settings.about.credits.animation", "動畫繪製");
        zhTw.put("menu.settings.about.credits.sound_design", "音效設計");
        zhTw.put("menu.settings.about.credits.music", "音樂");
        zhTw.put("menu.settings.about.credits.translation.en", "英文本地化");
        zhTw.put("menu.settings.about.credits.testing", "測試");
        zhTw.put("menu.settings.about.credits.online_assets", "網路素材");
        zhTw.put("menu.settings.about.credits.comma", "、");

        enUs.put("menu.settings.about.credits", "Credits");
        enUs.put("menu.settings.about.credits.game_design", "Game Design");
        enUs.put("menu.settings.about.credits.level_design", "Level Design");
        enUs.put("menu.settings.about.credits.narrative_design", "Narrative Design");
        enUs.put("menu.settings.about.credits.game_programming", "Game Programming");
        enUs.put("menu.settings.about.credits.system_logic", "System Logic");
        enUs.put("menu.settings.about.credits.ui_programming", "UI Programming");
        enUs.put("menu.settings.about.credits.art_2d", "2D Art");
        enUs.put("menu.settings.about.credits.character_design", "Character Design");
        enUs.put("menu.settings.about.credits.animation", "Animation");
        enUs.put("menu.settings.about.credits.sound_design", "Sound Effect");
        enUs.put("menu.settings.about.credits.translation.en", "English Localization");
        enUs.put("menu.settings.about.credits.music", "Music");
        enUs.put("menu.settings.about.credits.testing", "Testing");
        enUs.put("menu.settings.about.credits.online_assets", "Online Assets");
        enUs.put("menu.settings.about.credits.comma", ", ");

        // Exit
        zhTw.put("menu.exit.confirm", "確定要退出遊戲嗎？");

        enUs.put("menu.exit.confirm", "Are you sure you want to quit the game?");

        // Pause Menu
        zhTw.put("pause.resume", "繼續遊戲");
        zhTw.put("pause.save", "存檔");
        zhTw.put("pause.settings", "設定");
        zhTw.put("pause.exitToMain", "退出到主畫面");
        zhTw.put("pause.save.placeholder", "存檔功能尚未完成。");

        enUs.put("pause.resume", "Resume");
        enUs.put("pause.save", "Save");
        enUs.put("pause.settings", "Settings");
        enUs.put("pause.exitToMain", "Exit to Main Menu");
        enUs.put("pause.save.placeholder", "Save system is not implemented yet.");

        zhTw.put("menu.settings.language.changed", "語言設定已更新。");
        enUs.put("menu.settings.language.changed", "Language setting updated.");

        zhTw.put("pause.exitToMain.confirm", "確定要回到主畫面嗎？未儲存的進度可能會遺失。");
        enUs.put("pause.exitToMain.confirm", "Return to the main menu? Unsaved progress may be lost.");

        // Dead Menu
        zhTw.put("menu.rebirth", "重生");
        zhTw.put("menu.deathCount", "死亡次數：");
        zhTw.put("menu.score", "分數：");
        zhTw.put("menu.highestScore", "最高分數：");
        zhTw.put("menu.newRecord", "新紀錄！");

        enUs.put("menu.rebirth", "Regenerate");
        enUs.put("menu.deathCount", "Death Count: ");
        enUs.put("menu.score", "Score: ");
        enUs.put("menu.highestScore", "Highest Score: ");
        zhTw.put("menu.newRecord", "New Record!");

        // Achievement Toast
        zhTw.put("achievement.unlocked", "已解鎖");

        enUs.put("achievement.unlocked", "Unlocked");

        // Save System
        zhTw.put("save.load", "讀取存檔");
        zhTw.put("save.edit", "編輯存檔");
        zhTw.put("save.saveTo", "存檔至");
        zhTw.put("save.quickSave", "快速存檔");
        zhTw.put("save.quickSave.done", "已快速存檔。");
        zhTw.put("save.saved", "存檔完成。");
        zhTw.put("save.loaded", "讀取完成。");
        zhTw.put("save.description", "管理目前遊戲的存檔資料。");

        zhTw.put("save.emptySlot", "空存檔槽");
        zhTw.put("save.empty", "EMPTY");
        zhTw.put("save.noData", "尚無資料");
        zhTw.put("save.scene", "場景：");
        zhTw.put("save.name", "名稱：");
        zhTw.put("save.createdAt", "建立日期：");
        zhTw.put("save.savedAt", "存檔日期：");
        zhTw.put("save.lastOpenedAt", "上次開啟：");

        zhTw.put("save.rename", "重新命名");
        zhTw.put("save.rename.header", "修改存檔名稱");
        zhTw.put("save.rename.content", "請輸入新的存檔名稱：");

        zhTw.put("save.delete", "刪除存檔");
        zhTw.put("save.delete.confirm", "確定要刪除此存檔嗎？");

        zhTw.put("save.confirm", "確認");
        zhTw.put("save.warning.unsaved", "目前遊戲內容可能尚未存檔，讀取後會遺失變更。是否繼續？");
        zhTw.put("save.overwrite.confirm", "此存檔槽已有資料，是否覆蓋？");
        zhTw.put("save.inputName.header", "輸入存檔名稱");
        zhTw.put("save.inputName.content", "存檔名稱：");

        enUs.put("save.load", "Load Save");
        enUs.put("save.edit", "Edit Saves");
        enUs.put("save.saveTo", "Save To");
        enUs.put("save.quickSave", "Quick Save");
        enUs.put("save.quickSave.done", "Quick save completed.");
        enUs.put("save.saved", "Game saved.");
        enUs.put("save.loaded", "Save loaded.");
        enUs.put("save.description", "Manage the current game save data.");

        enUs.put("save.emptySlot", "Empty Slot");
        enUs.put("save.empty", "EMPTY");
        enUs.put("save.noData", "No Data");
        enUs.put("save.scene", "Scene: ");
        enUs.put("save.name", "Name: ");
        enUs.put("save.createdAt", "Created: ");
        enUs.put("save.savedAt", "Saved: ");
        enUs.put("save.lastOpenedAt", "Last Opened: ");

        enUs.put("save.rename", "Rename");
        enUs.put("save.rename.header", "Rename Save");
        enUs.put("save.rename.content", "Enter a new save name:");

        enUs.put("save.delete", "Delete Save");
        enUs.put("save.delete.confirm", "Are you sure you want to delete this save?");

        enUs.put("save.confirm", "Confirm");
        enUs.put("save.warning.unsaved", "Your current progress may not be saved. Loading another save may discard unsaved changes. Continue?");
        enUs.put("save.overwrite.confirm", "This slot already contains a save. Overwrite it?");
        enUs.put("save.inputName.header", "Enter Save Name");
        enUs.put("save.inputName.content", "Save name:");

        // Story Mode
        zhTw.put("story.house.foldQuilt", "折被子");
        enUs.put("story.house.foldQuilt", "Fold Quilt");

        zhTw.put("story.house.drinkWater", "喝水");
        enUs.put("story.house.drinkWater", "Drink Water");

        zhTw.put("story.house.brush_teeth", "刷牙");
        enUs.put("story.house.brush_teeth", "Brush teeth");

        zhTw.put("story.house.openDoor", "開門");
        zhTw.put("story.house.closeDoor", "關門");
        enUs.put("story.house.openDoor", "Open Door");
        enUs.put("story.house.closeDoor", "Close Door");

        zhTw.put("story.house.wearShoes", "穿鞋");
        zhTw.put("story.house.takeOffShoes", "脫鞋");
        enUs.put("story.house.wearShoes", "Put on shoes");
        enUs.put("story.house.takeOffShoes", "Take off shoes");

        zhTw.put("story.house.talkToMom", "跟 媽咪 對話");
        enUs.put("story.house.talkToMom", "Talk to Mom");

        zhTw.put("story.house.exit", "離開 家");
        zhTw.put("story.house.exit.locked", "現在還不能出門。");
        enUs.put("story.house.exit", "Leave Home");
        enUs.put("story.house.exit.locked", "You can't go outside yet.");

        zhTw.put("story.street.enter", "返回 家");
        enUs.put("story.street.enter", "Go Home");

        // Quest
        zhTw.put("quest.fold_quilt", "摺好被子");
        zhTw.put("quest.brush_teeth", "刷牙");
        zhTw.put("quest.talk_to_mom", "跟媽媽講話");
        zhTw.put("quest.wear_shoes", "穿上鞋子");
        zhTw.put("quest.exit_house", "離開 家");

        enUs.put("quest.fold_quilt", "Fold the quilt");
        enUs.put("quest.brush_teeth", "Brush your teeth");
        enUs.put("quest.talk_to_mom", "Talk to mom");
        enUs.put("quest.wear_shoes", "Put on shoes");
        enUs.put("quest.exit_house", "Exit Home");

        // Dialog
        zhTw.put("dialog.character.mom", "媽媽");
        enUs.put("dialog.character.mom", "Mom");

        zhTw.put("dialog.mom.001", "崽，你還在玩那些尪仔喔。");
        zhTw.put("dialog.mom.002", "休息一下吧，去幫我買個東西好不好。");
        zhTw.put("dialog.mom.003.1", "幫我去買一打雞蛋、兩支青蔥跟一顆高麗菜。");
        zhTw.put("dialog.mom.003.2", "我才講你兩句，你就說我煩，翅膀硬了是不是？");
        zhTw.put("dialog.mom.004.2", "媽媽看起來好像有點生氣了。");

        enUs.put("dialog.mom.001", "Wei, you still doing that video-making dream thing?");
        enUs.put("dialog.mom.002", "Stop being a failure. Be a lawyer or go buy me something lah, okay?");
        enUs.put("dialog.mom.003.1", "Buy a dozen eggs, two green onions, and a cabbage.");
        enUs.put("dialog.mom.003.2", "Are you talking back right now? Say one word...");
        enUs.put("dialog.mom.004.2", "Mom looks ticked off. You should...");

        zhTw.put("dialog.mom.option.1.1", "好");
        zhTw.put("dialog.mom.option.1.2", "煩耶");
        zhTw.put("dialog.mom.option.2.1", "戰鬥");
        zhTw.put("dialog.mom.option.2.2", "跳舞");
        zhTw.put("dialog.mom.option.2.3", "逃跑");

        enUs.put("dialog.mom.option.1.1", "Sure");
        enUs.put("dialog.mom.option.1.2", "Nah");
        enUs.put("dialog.mom.option.2.1", "\"Gubernatorial\"");
        enUs.put("dialog.mom.option.2.2", "Dance Off");
        enUs.put("dialog.mom.option.2.3", "Run");

        // Mini Game
        zhTw.put("pause.save.disabled", "此模式無法使用存檔功能。");
        enUs.put("pause.save.disabled", "Saving is not available in this mode.");

        // Deaths
        zhTw.put("death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.title", "沒折被子");
        zhTw.put("death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.subtitle", "人家是西點軍校，你們是西點麵包啊。");

        enUs.put("death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.title", "FORGET TO FOLD THE QUILT");
        enUs.put("death.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT.subtitle", "Seems like someone is folded.");

        zhTw.put("death.HIT_CEILING.title", "撞到天花板");
        zhTw.put("death.HIT_CEILING.subtitle", "It's-a-me Mario.");

        enUs.put("death.HIT_CEILING.title", "HIT THE CEILING");
        enUs.put("death.HIT_CEILING.subtitle", "It's-a-me Mario.");

        zhTw.put("death.HIT_SHOWER_CURTAIN_ROD.title", "撞到浴簾桿");
        zhTw.put("death.HIT_SHOWER_CURTAIN_ROD.subtitle", "鋼管掉落.mp3");

        enUs.put("death.HIT_SHOWER_CURTAIN_ROD.title", "HIT SHOWER CURTAIN ROD");
        enUs.put("death.HIT_SHOWER_CURTAIN_ROD.subtitle", "Metal Pipe Falling.mp3");

        zhTw.put("death.HIT_DOORFRAME.title", "撞到門框");
        zhTw.put("death.HIT_DOORFRAME.subtitle", "\"Watch your head\"的中文直譯是「看你個頭」，相信我。");

        enUs.put("death.HIT_DOORFRAME.title", "HIT DOORFRAME");
        enUs.put("death.HIT_DOORFRAME.subtitle", "The door framed you.");

        zhTw.put("death.JUMPING_ON_BED.title", "在床上跳被媽媽制裁了");
        zhTw.put("death.JUMPING_ON_BED.subtitle", "超大雙人床ㄟ。");

        enUs.put("death.JUMPING_ON_BED.title", "JUMPING ON BED");
        enUs.put("death.JUMPING_ON_BED.subtitle", "You know, breaking bed is illegal.");

        zhTw.put("death.DRINK_WATER.title", "喝下過夜水");
        zhTw.put("death.DRINK_WATER.subtitle", "眾所周知，水放過夜不能喝，但水放一整天可以喝。");

        enUs.put("death.DRINK_WATER.title", "DRINK THE OVERNIGHT WATER");
        enUs.put("death.DRINK_WATER.subtitle", "Water lasted all day long: totally fine; Water left overnight: poisoned.");

        zhTw.put("death.TRIPPED_BY_SIDEWALK_TILE.title", "被凸起磁磚絆倒");
        zhTw.put("death.TRIPPED_BY_SIDEWALK_TILE.subtitle", "人生有起也有落落落落落。");

        enUs.put("death.TRIPPED_BY_SIDEWALK_TILE.title", "TRIPPED BY PROTRUDING TILES");
        enUs.put("death.TRIPPED_BY_SIDEWALK_TILE.subtitle", "DIVE HEAD-FIRST FOR THE BAG! And... Out!");

        zhTw.put("death.HIT_BY_SCOOTER.title", "被摩托車創飛");
        zhTw.put("death.HIT_BY_SCOOTER.subtitle", "麥可 are you okay?");

        enUs.put("death.HIT_BY_SCOOTER.title", "SENT FLYING BY A SCOOTER");
        enUs.put("death.HIT_BY_SCOOTER.subtitle", "Ah ha! Classic.");

        zhTw.put("death.FALLING_FRIDGE.title", "被墜落的冰箱擊中");
        zhTw.put("death.FALLING_FRIDGE.subtitle", "F＝ma-ma-mia");

        enUs.put("death.FALLING_FRIDGE.title", "CRASHED BY A FALLEN FRIDGE");
        enUs.put("death.FALLING_FRIDGE.subtitle", "F＝ma-mma-mia");

        zhTw.put("death.FALLING_HELI.title", "被墜落的阿帕契擊中");
        zhTw.put("death.FALLING_HELI.subtitle", "甚麼都掉，甚麼都不奇怪。");

        enUs.put("death.FALLING_HELI.title", "CRASHED BY AN APACHE");
        enUs.put("death.FALLING_HELI.subtitle", "Nothing weird these days.");

        zhTw.put("death.JUMPED_IN_BATHTUB.title", "在浴缸裡滑倒");
        zhTw.put("death.JUMPED_IN_BATHTUB.subtitle", "要泡蛇酒是嗎？");

        enUs.put("death.JUMPED_IN_BATHTUB.title", "SLIPPED IN THE BATHTUB");
        enUs.put("death.JUMPED_IN_BATHTUB.subtitle", "Final Destination Side Story - Granny's Bathtub");

        zhTw.put("death.LEFT_WITHOUT_BRUSHING_TEETH.title", "起床沒刷牙");
        zhTw.put("death.LEFT_WITHOUT_BRUSHING_TEETH.subtitle", "早上起來刷刷牙。");

        enUs.put("death.LEFT_WITHOUT_BRUSHING_TEETH.title", "FORGET TO BRUSH TEETH");
        enUs.put("death.LEFT_WITHOUT_BRUSHING_TEETH.subtitle", "I guess it starts from a quarter to one.");

        zhTw.put("death.ENTER_LIVING_ROOM_WITH_SHOES.title", "穿鞋在室內亂跑被制裁");
        zhTw.put("death.ENTER_LIVING_ROOM_WITH_SHOES.subtitle", "被管家活活打斷雙腿。");

        enUs.put("death.ENTER_LIVING_ROOM_WITH_SHOES.title", "WANDERING AROUND THE HOUSE WITH SHOES ON");
        enUs.put("death.ENTER_LIVING_ROOM_WITH_SHOES.subtitle", "Sent to Jesus by Mom.");

        zhTw.put("death.LOCK_YOURSELF_IN_THE_CLOSET.title", "把自己關在衣櫃");
        zhTw.put("death.LOCK_YOURSELF_IN_THE_CLOSET.subtitle", "衣櫃並不能免於核爆，冰箱也不行。");

        enUs.put("death.LOCK_YOURSELF_IN_THE_CLOSET.title", "TRAP YOURSELF IN THE CLOSET");
        enUs.put("death.LOCK_YOURSELF_IN_THE_CLOSET.subtitle", "ThE ClOsEt eNdInG Is mY FaVoRiTe!");

        zhTw.put("death.MOM_BATTLE_LOSE_A.title", "被媽媽擊敗");
        zhTw.put("death.MOM_BATTLE_LOSE_A.subtitle", "Yo Battle。");

        enUs.put("death.MOM_BATTLE_LOSE_A.title", "DEFEATED BY MOM");
        enUs.put("death.MOM_BATTLE_LOSE_A.subtitle", "One-shotted by la chancla.");

        zhTw.put("death.MOM_BATTLE_LOSE_B.title", "被媽媽擊敗");
        zhTw.put("death.MOM_BATTLE_LOSE_B.subtitle", "勝敗乃兵家常事，下一次你還是會輸的。");

        enUs.put("death.MOM_BATTLE_LOSE_B.title", "DEFEATED BY MOM");
        enUs.put("death.MOM_BATTLE_LOSE_B.subtitle", "nt, stop trying.");

        zhTw.put("death.MOM_BATTLE_LOSE_C.title", "被媽媽擊敗");
        zhTw.put("death.MOM_BATTLE_LOSE_C.subtitle", "你有甚麼問題，後面真沒了。");

        enUs.put("death.MOM_BATTLE_LOSE_C.title", "DEFEATED BY MOM");
        enUs.put("death.MOM_BATTLE_LOSE_C.subtitle", "What's wrong with you, there's no more.");

        zhTw.put("death.MOM_DANCE_OFF.title", "激怒媽媽");
        zhTw.put("death.MOM_DANCE_OFF.subtitle", "假如你生氣，假如你生氣，仰望耶穌，仰望耶穌。");

        enUs.put("death.MOM_DANCE_OFF.title", "TRIGGERED MOM");
        enUs.put("death.MOM_DANCE_OFF.subtitle", "What do you expect?");
    }

    public String text(String key) {
        Map<String, String> map = getCurrentMap();

        if (map.containsKey(key)) {
            return map.get(key);
        }

        return "[" + key + "]";
    }

    private Map<String, String> getCurrentMap() {
        return switch (currentLanguage) {
            case ZH_TW -> zhTw;
            case EN_US -> enUs;
        };
    }

    public Language getCurrentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(Language language) {
        if (language == null) {
            return;
        }

        currentLanguage = language;
        saveSettings();
    }

    public void toggleLanguage() {
        if (currentLanguage == Language.ZH_TW) {
            setLanguage(Language.EN_US);
        } else {
            setLanguage(Language.ZH_TW);
        }
    }

    private void loadSettings() {
        String name = prefs.get("language", Language.ZH_TW.name());

        try {
            currentLanguage = Language.valueOf(name);
        } catch (Exception e) {
            currentLanguage = Language.ZH_TW;
        }
    }

    private void saveSettings() {
        prefs.put("language", currentLanguage.name());
    }

    public void resetSettings() {
        currentLanguage = Language.ZH_TW;
        saveSettings();
    }
}