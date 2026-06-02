package ass.example.ui;

import javafx.animation.PauseTransition;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * CursorManager
 *
 * 統一管理遊戲中的自訂滑鼠游標。
 *
 * 功能：
 * 1. 載入自訂游標圖片。
 * 2. 將自訂游標套用到指定 Node 與其所有子節點。
 * 3. 支援多個 UI Root，例如 MainMenu、PauseMenu、DeathScreen、DialogueUI。
 * 4. 滑鼠閒置一段時間後自動隱藏。
 * 5. 滑鼠移動、拖曳、點擊、進入畫面時重新顯示。
 *
 * 為什麼不用單一 installedRoot：
 * - FXGLMenu、DeathScreen、DialogueUI 可能是不同 UI root。
 * - 如果只記錄一個 root，後安裝的 UI 可能會讓前一個 UI 的游標狀態失效。
 * - 使用 WeakHashMap 可以避免 root 被銷毀後仍被 CursorManager 強制保留。
 */
public final class CursorManager {

    // =========================================================
    // Cursor Settings
    // =========================================================

    /**
     * 自訂游標圖片路徑。
     */
    private static final String CURSOR_PATH =
            "/assets/textures/ui/cursor/cursor.png";

    /**
     * 滑鼠閒置幾秒後隱藏。
     *
     * 如果你不想自動隱藏，可以把秒數改很大，
     * 或在 install() 裡不要呼叫 restartIdleTimer()。
     */
    private static final double IDLE_HIDE_SECONDS = 2.0;

    /**
     * 游標熱點。
     *
     * 這是實際點擊判定的位置。
     * 依你的圖片調整：
     * - 左上角點擊：0, 0
     * - 圖片尖端在 x=25, y=0：25, 0
     */
    private static final double HOTSPOT_X = 25;
    private static final double HOTSPOT_Y = 0;


    // =========================================================
    // Runtime State
    // =========================================================

    private static Cursor customCursor;

    private static PauseTransition idleTimer;

    private static Node currentActiveRoot;

    private static boolean cursorVisible = true;

    /**
     * 已安裝過事件監聽的 root。
     *
     * 使用 WeakHashMap：
     * root 被 JavaFX 回收後，這裡不會硬保留引用。
     */
    private static final Set<Node> installedRoots =
            Collections.newSetFromMap(new WeakHashMap<>());


    // =========================================================
    // Constructor
    // =========================================================

    private CursorManager() {
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 安裝自訂游標。
     *
     * 建議在以下地方呼叫：
     * - MainMenu constructor / onCreate
     * - PauseMenu constructor / onCreate
     * - DeathScreen constructor
     * - DialogueUI constructor
     *
     * @param root 要套用自訂游標的 UI root
     */
    public static void install(Node root) {
        if (root == null) {
            return;
        }

        ensureCursorLoaded();

        currentActiveRoot = root;

        /*
         * 關鍵：
         * 不只 root，要連子節點一起套用。
         * 否則滑鼠移到 button、overlay、VBox、StackPane 上時，
         * 可能仍然顯示預設游標或看不到游標。
         */
        applyCursorRecursively(root, customCursor);

        /*
         * 同一個 root 只安裝一次事件監聽，避免事件重複觸發。
         */
        if (!installedRoots.contains(root)) {
            installedRoots.add(root);
            installActivityListeners(root);
        }

        showCursor(root);
        restartIdleTimer(root);
    }

    /**
     * 重新把游標套用到目前 active root。
     *
     * 適合用在：
     * - 動態新增子頁面後
     * - 動態新增 button / popup 後
     * - 切換語言重新建立按鈕後
     */
    public static void refresh() {
        if (currentActiveRoot == null) {
            return;
        }

        ensureCursorLoaded();
        applyCursorRecursively(currentActiveRoot, customCursor);
        showCursor(currentActiveRoot);
        restartIdleTimer(currentActiveRoot);
    }

    /**
     * 隱藏指定節點的游標。
     *
     * 通常在正式遊戲場景中使用。
     */
    public static void hideCursor(Node root) {
        if (root == null) {
            return;
        }

        applyCursorRecursively(root, Cursor.NONE);
        cursorVisible = false;
    }

    /**
     * 顯示指定節點的自訂游標。
     */
    public static void showCursor(Node root) {
        if (root == null) {
            return;
        }

        ensureCursorLoaded();
        applyCursorRecursively(root, customCursor);
        cursorVisible = true;
    }

    /**
     * 是否目前游標為顯示狀態。
     */
    public static boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * 清除 CursorManager 狀態。
     *
     * 通常不需要呼叫。
     */
    public static void uninstallAll() {
        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }

        for (Node root : installedRoots) {
            if (root != null) {
                applyCursorRecursively(root, Cursor.DEFAULT);
            }
        }

        installedRoots.clear();

        currentActiveRoot = null;
        customCursor = null;
        cursorVisible = true;
    }


    // =========================================================
    // Cursor Loading
    // =========================================================

    private static void ensureCursorLoaded() {
        if (customCursor != null) {
            return;
        }

        customCursor = loadCustomCursor();
    }

    private static Cursor loadCustomCursor() {
        try {
            var url = CursorManager.class.getResource(CURSOR_PATH);

            if (url == null) {
                System.out.println("Custom cursor not found: " + CURSOR_PATH);
                return Cursor.DEFAULT;
            }

            Image image = new Image(url.toExternalForm());

            return new ImageCursor(
                    image,
                    HOTSPOT_X,
                    HOTSPOT_Y
            );

        } catch (Exception e) {
            System.out.println("Custom cursor load failed: " + CURSOR_PATH);
            e.printStackTrace();
            return Cursor.DEFAULT;
        }
    }


    // =========================================================
    // Activity / Idle Handling
    // =========================================================

    private static void installActivityListeners(Node root) {
        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> onMouseActivity(root));
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> onMouseActivity(root));
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> onMouseActivity(root));
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> onMouseActivity(root));
        root.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> onMouseActivity(root));
    }

    private static void onMouseActivity(Node root) {
        if (root == null) {
            return;
        }

        currentActiveRoot = root;

        showCursor(root);
        restartIdleTimer(root);
    }

    private static void restartIdleTimer(Node root) {
        if (root == null) {
            return;
        }

        if (idleTimer == null) {
            idleTimer = new PauseTransition(Duration.seconds(IDLE_HIDE_SECONDS));
        }

        idleTimer.stop();

        idleTimer.setDuration(Duration.seconds(IDLE_HIDE_SECONDS));
        idleTimer.setOnFinished(event -> {
            /*
             * 只隱藏目前 active root。
             * 避免 DeathScreen / DialogueUI 關閉後，把其他 UI root 的游標一起弄亂。
             */
            if (currentActiveRoot != null) {
                hideCursor(currentActiveRoot);
            }
        });

        idleTimer.playFromStart();
    }


    // =========================================================
    // Recursive Cursor Apply
    // =========================================================

    /**
     * 將游標套用到 node 與其所有子節點。
     *
     * 這是修正 PauseMenu / DeathScreen / DialogueUI 游標不出現的關鍵。
     */
    private static void applyCursorRecursively(Node node, Cursor cursor) {
        if (node == null) {
            return;
        }

        node.setCursor(cursor);

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyCursorRecursively(child, cursor);
            }
        }
    }
}