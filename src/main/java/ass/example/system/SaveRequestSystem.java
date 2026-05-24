package ass.example.system;

public class SaveRequestSystem {

    private static Integer pendingLoadSlotIndex = null;

    public static void requestLoadSlot(int slotIndex) {
        pendingLoadSlotIndex = slotIndex;
    }

    public static boolean hasPendingLoadSlot() {
        return pendingLoadSlotIndex != null;
    }

    public static int consumePendingLoadSlot() {
        int slot = pendingLoadSlotIndex;
        pendingLoadSlotIndex = null;
        return slot;
    }

    public static void clear() {
        pendingLoadSlotIndex = null;
    }
}