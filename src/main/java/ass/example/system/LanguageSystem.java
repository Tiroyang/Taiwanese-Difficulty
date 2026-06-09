package ass.example.system;

import ass.example.core.Language;
import ass.example.core.LanguageTextDatabase;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * LanguageSystem
 *
 * 全遊戲語言系統。
 *
 * 功能：
 * 1. 保存目前語言。
 * 2. 從 Preferences 讀取語言設定。
 * 3. 將語言設定寫入 Preferences。
 * 4. 根據 key 取得目前語言的文字。
 * 5. 支援中英文切換。
 * 6. 支援恢復預設語言。
 */
public final class LanguageSystem {
 
    // Preferences Keys 

    /**
     * Preferences 中保存語言設定的 key。
     */
    private static final String KEY_LANGUAGE = "language";
 
    // Default Settings 

    /**
     * 預設語言。
     */
    private static final Language DEFAULT_LANGUAGE = Language.ZH_TW;
 
    // Singleton 

    /**
     * LanguageSystem 單例。
     */
    private static final LanguageSystem INSTANCE = new LanguageSystem();

    /**
     * 取得 LanguageSystem 單例。
     *
     * @return LanguageSystem
     */
    public static LanguageSystem getInstance() {
        return INSTANCE;
    }
 
    // Runtime State 

    /**
     * Java Preferences。
     *
     * 用於保存目前語言設定。
     */
    private final Preferences prefs =
            Preferences.userNodeForPackage(LanguageSystem.class);

    /**
     * 目前語言。
     */
    private Language currentLanguage = DEFAULT_LANGUAGE;

    /**
     * 所有語言對應的翻譯資料。
     *
     * key：
     * - Language.ZH_TW
     * - Language.EN_US
     *
     * value：
     * - 該語言的文字表
     */
    private final Map<Language, Map<String, String>> texts;

 
    // Constructor 

    /**
     * 建立語言系統。
     *
     * private：
     * - 確保外部只能透過 getInstance() 使用。
     */
    private LanguageSystem() {
        this.texts = LanguageTextDatabase.create();
        loadSettings();
    }

 
    // Text Query 

    /**
     * 根據 key 取得目前語言文字。
     *
     * 若目前語言找不到該 key，會嘗試從預設語言取得。
     * 若預設語言也找不到，回傳 [key]，方便 Debug。
     *
     * @param key 文字 key
     * @return 翻譯後文字
     */
    public String text(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }

        Map<String, String> currentMap = getTextMap(currentLanguage);

        if (currentMap.containsKey(key)) {
            return currentMap.get(key);
        }

        Map<String, String> defaultMap = getTextMap(DEFAULT_LANGUAGE);

        if (defaultMap.containsKey(key)) {
            return defaultMap.get(key);
        }

        return "[" + key + "]";
    }

    /**
     * 取得指定語言的文字表。
     *
     * @param language 語言
     * @return 該語言文字表
     */
    private Map<String, String> getTextMap(Language language) {
        return texts.getOrDefault(
                language,
                texts.get(DEFAULT_LANGUAGE)
        );
    }

 
    // Language Control 

    /**
     * 設定目前語言。
     *
     * @param language 目標語言
     */
    public void setLanguage(Language language) {
        if (language == null) {
            return;
        }

        currentLanguage = language;
        saveSettings();
    }

    /**
     * 切換語言。
     */
    public void toggleLanguage() {
        if (currentLanguage == Language.ZH_TW) {
            setLanguage(Language.EN_US);
            return;
        }

        setLanguage(Language.ZH_TW);
    }

    /**
     * 重置語言設定。
     *
     * 會回到預設語言。
     */
    public void resetSettings() {
        currentLanguage = DEFAULT_LANGUAGE;
        saveSettings();
    }

 
    // Load / Save Settings 

    /**
     * 從 Preferences 讀取語言設定。
     */
    private void loadSettings() {
        String languageName = prefs.get(
                KEY_LANGUAGE,
                DEFAULT_LANGUAGE.name()
        );

        try {
            currentLanguage = Language.valueOf(languageName);
        } catch (Exception exception) {
            currentLanguage = DEFAULT_LANGUAGE;
        }
    }

    /**
     * 將目前語言設定寫入 Preferences。
     */
    private void saveSettings() {
        prefs.put(KEY_LANGUAGE, currentLanguage.name());
    }

 
    // Getters 

    /**
     * 取得目前語言。
     *
     * @return 目前語言
     */
    public Language getCurrentLanguage() {
        return currentLanguage;
    }
}