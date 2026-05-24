package ass.example.core;

public final class CollisionCategory {

    private CollisionCategory() {
    }

    public static final short PLAYER = 0x0001;
    public static final short FLOOR = 0x0002;
    public static final short WALL = 0x0004;
    public static final short FALLING_OBJECT = 0x0008;

    public static final short ALL = -1;
}