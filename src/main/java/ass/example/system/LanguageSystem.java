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
        zhTw.put("menu.endless", "無盡模式");
        zhTw.put("menu.achievement", "成就");
        zhTw.put("menu.settings", "設定");
        zhTw.put("menu.exit", "退出");

        enUs.put("menu.story", "Story Mode");
        enUs.put("menu.endless", "Endless Mode");
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

        enUs.put("menu.storyMode.back", "New Game");
        enUs.put("menu.storyMode.apply", "Load Saves");
        enUs.put("menu.storyMode.on", "Edit Saves");

        // Endless Mode Page
        zhTw.put("menu.endlessMode.description", "選擇一個關卡。");

        zhTw.put("menu.endlessMode.theStreet", "街頭");
        zhTw.put("menu.endlessMode.comingSoon", "敬請期待");

        zhTw.put("menu.endlessMode.comingSoon.description", "此關卡尚未開放。");

        enUs.put("menu.endlessMode.description", "Choose a level.");

        enUs.put("menu.endlessMode.theStreet", "The Street");
        enUs.put("menu.endlessMode.comingSoon", "Coming Soon");

        enUs.put("menu.endlessMode.comingSoon.description", "This level has yet to be finished.");

        //Achievement Page
        zhTw.put("menu.achievement.locked", "尚未解鎖");
        zhTw.put("menu.achievement.description", "透過不同死亡方式解鎖。");

        enUs.put("menu.achievement.locked", "Locked");
        enUs.put("menu.achievement.description", "Lock via different dying methods!");

        // Settings Page
        zhTw.put("menu.settings.description", "選擇選項來調整遊戲設定");

        zhTw.put("menu.settings.KeyConfig", "操作配置");
        zhTw.put("menu.settings.keyConfig.left", "：向左移動");
        zhTw.put("menu.settings.keyConfig.right", "：向右移動");
        zhTw.put("menu.settings.keyConfig.jump", "：跳躍");
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
        enUs.put("menu.settings.volume.button_sound", "Sound");

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
        enUs.put("menu.settings.dev_mode.toggleOff", "Switch to Developer Mode");
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
        enUs.put("menu.settings.about.description.construct", "This project is mainly developed with JavaFX and built using the FXGL game framework.\n");
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
        zhTw.put("menu.settings.about.credits.testing", "測試");
        zhTw.put("menu.settings.about.credits.online_assets", "網路素材");
        zhTw.put("menu.settings.about.credits.tiro_online_assets", "Tiro、網路素材");

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
        enUs.put("menu.settings.about.credits.sound_design", "Sound Design");
        enUs.put("menu.settings.about.credits.music", "Music");
        enUs.put("menu.settings.about.credits.testing", "Testing");
        enUs.put("menu.settings.about.credits.online_assets", "Online Assets");
        enUs.put("menu.settings.about.credits.tiro_online_assets", "Tiro, Online Assets");

        // Dead Menu
        zhTw.put("menu.rebirth", "重生");
        enUs.put("story.house.rebirth", "Regenerate");

        // Story Mode
        zhTw.put("story.house.foldQuilt", "折被子");
        enUs.put("story.house.foldQuilt", "Fold Quilt");

        zhTw.put("story.house.drinkWater", "喝水");
        enUs.put("story.house.drinkWater", "Drink Water");

        zhTw.put("story.house.openDoor", "開門");
        zhTw.put("story.house.closeDoor", "關門");
        enUs.put("story.house.openDoor", "Open Door");
        enUs.put("story.house.closeDoor", "Close Door");

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