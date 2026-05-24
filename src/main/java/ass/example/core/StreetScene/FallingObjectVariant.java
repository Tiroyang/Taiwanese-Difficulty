package ass.example.core.StreetScene;

import ass.example.core.DeathReason;

public enum FallingObjectVariant {
    FRIDGE(
            DeathReason.FALLING_FRIDGE,
            92,
            52
    ),

    HELI(
            DeathReason.FALLING_HELI,
            58,
            58
    );

    private final DeathReason deathReason;
    private final double width;
    private final double height;

    FallingObjectVariant(
            DeathReason deathReason,
            double width,
            double height
    ) {
        this.deathReason = deathReason;
        this.width = width;
        this.height = height;
    }

    public DeathReason getDeathReason() {
        return deathReason;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}