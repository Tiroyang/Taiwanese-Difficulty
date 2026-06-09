package ass.example.core;

import ass.example.scenes.system.SceneConfig;

import java.util.EnumMap;
import java.util.Map;

/**
 * SceneConfigs
 *
 * 全部場景設定的集中管理類別。
 *
 * 功能：
 * 1. 保存每個 SceneType 對應的 SceneConfig。
 * 2. 讓 SceneManager 不需要自己註冊場景尺寸與玩家出生點。
 * 3. 避免 SceneManager 同時負責「流程控制」與「設定資料」。
 *
 * 設計原則：
 * - SceneConfig 表示一個場景設定。
 * - SceneConfigs 表示所有場景設定集合。
 * - 此類別只保存靜態設定，不保存 runtime 狀態。
 */
public final class SceneConfigs {
 
    // Config Registry 

    /**
     * 每個 SceneType 對應的 SceneConfig。
     *
     * 使用 EnumMap 比 HashMap 更適合 enum key。
     */
    private static final Map<SceneType, SceneConfig> CONFIGS =
            new EnumMap<>(SceneType.class);

    static {
        registerConfigs();
    }

 
    // Constructor 

    /**
     * 工具類別不允許建立實例。
     */
    private SceneConfigs() {
    }

 
    // Registration 

    /**
     * 註冊所有場景設定。
     */
    private static void registerConfigs() {
        CONFIGS.put(
                SceneType.HOUSE,
                new SceneConfig(
                        3200,
                        720,
                        2500,
                        422.0
                )
        );

        CONFIGS.put(
                SceneType.STREET,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );

        CONFIGS.put(
                SceneType.STREET_ENDLESS,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );
    }

 
    // Public API 

    /**
     * 取得指定場景的設定。
     *
     * @param sceneType 場景類型
     * @return SceneConfig
     * @throws IllegalArgumentException 若找不到對應設定
     */
    public static SceneConfig get(SceneType sceneType) {
        SceneConfig config = CONFIGS.get(sceneType);

        if (config == null) {
            throw new IllegalArgumentException(
                    "No SceneConfig registered for scene type: " + sceneType
            );
        }

        return config;
    }
}