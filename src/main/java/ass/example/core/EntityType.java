package ass.example.core;

/**
 * Entity類型列表
 */
public enum EntityType {
    // PlayerFactory
    PLAYER,
    PLAYER_GROUND_SENSOR,

    // CommonFactory
    MAP_BACKGROUND,
    WALL,
    DEATH_ZONE,
    FLOOR,
    PLATFORM,
    ONE_WAY_PLATFORM,
    ONE_WAY_PLATFORM_COLLIDER,
    INTERACTABLE,
    TRIGGER,
    PROP,
    ENEMY,
    ITEM,

    // House
    DOOR,
    DOOR_COLLIDER,

    BED_ONE_WAY_PLATFORM,
    BED_ONE_WAY_PLATFORM_COLLIDER,

    WINDOW
}