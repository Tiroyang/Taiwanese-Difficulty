package ass.example.system.save;

public class SaveSlotData {

    private final int slotIndex;
    private final boolean exists;

    private final String saveName;
    private final String sceneName;
    private final String thumbnailBase64;

    private final long createdAt;
    private final long savedAt;
    private final long lastOpenedAt;

    public SaveSlotData(
            int slotIndex,
            boolean exists,
            String saveName,
            String sceneName,
            String thumbnailBase64,
            long createdAt,
            long savedAt,
            long lastOpenedAt
    ) {
        this.slotIndex = slotIndex;
        this.exists = exists;
        this.saveName = saveName;
        this.sceneName = sceneName;
        this.thumbnailBase64 = thumbnailBase64;
        this.createdAt = createdAt;
        this.savedAt = savedAt;
        this.lastOpenedAt = lastOpenedAt;
    }

    public static SaveSlotData empty(int slotIndex) {
        return new SaveSlotData(
                slotIndex,
                false,
                "",
                "",
                "",
                0,
                0,
                0
        );
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public boolean exists() {
        return exists;
    }

    public String getSaveName() {
        return saveName;
    }

    public String getSceneName() {
        return sceneName;
    }

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }
}