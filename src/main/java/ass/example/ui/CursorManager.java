package ass.example.ui;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;

public final class CursorManager {

    private static ImageCursor customCursor;

    private CursorManager() {
    }

    public static ImageCursor getCustomCursor() {
        if (customCursor == null) {
            Image cursorImage = new Image(
                    CursorManager.class.getResource("/assets/textures/ui/cursor/cursor.png").toExternalForm(),
                    32,
                    46,
                    true,
                    true
            );

            customCursor = new ImageCursor(cursorImage, 0, 16);
        }

        return customCursor;
    }

    public static void hideCursor(Node node) {
        if (node != null) {
            node.setCursor(Cursor.NONE);
        }
    }

    public static void showCustomCursor(Node node) {
        if (node != null) {
            node.setCursor(getCustomCursor());
        }
    }

    public static void applyCustomCursorRecursively(Node node) {
        if (node == null) {
            return;
        }

        node.setCursor(getCustomCursor());

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyCustomCursorRecursively(child);
            }
        }
    }

    public static void hideCursorRecursively(Node node) {
        if (node == null) {
            return;
        }

        node.setCursor(Cursor.NONE);

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                hideCursorRecursively(child);
            }
        }
    }
}