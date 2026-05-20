package ass.example.ui;

import ass.example.core.DeathReasons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * 死亡畫面，待重製。
 */
public class DeathScreen extends StackPane {

    private final Text titleText = new Text();
    private final Text subtitleText = new Text();
    private final Text deathCountText = new Text();

    private final Button respawnButton = new Button("重生");
    private final Button mainMenuButton = new Button("回到主頁");

    public DeathScreen(Runnable onRespawn, Runnable onMainMenu) {
        setPrefSize(1280, 720);
        setVisible(false);

        Rectangle background = new Rectangle(1280, 720);
        background.setFill(Color.rgb(0, 0, 0, 0.78));

        Text deathTitle = new Text("你死了");
        deathTitle.setStyle("""
                -fx-font-size: 64px;
                -fx-fill: #ff5555;
                -fx-font-weight: bold;
                """);

        titleText.setStyle("""
                -fx-font-size: 32px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        subtitleText.setStyle("""
                -fx-font-size: 20px;
                -fx-fill: #cccccc;
                """);

        deathCountText.setStyle("""
        -fx-font-size: 18px;
        -fx-fill: #aaaaaa;
        """);

        respawnButton.setPrefWidth(180);
        respawnButton.setPrefHeight(44);
        respawnButton.setStyle("""
                -fx-font-size: 18px;
                -fx-background-color: #ffffff;
                -fx-text-fill: #111111;
                """);

        mainMenuButton.setPrefWidth(180);
        mainMenuButton.setPrefHeight(44);
        mainMenuButton.setStyle("""
                -fx-font-size: 18px;
                -fx-background-color: transparent;
                -fx-border-color: white;
                -fx-text-fill: white;
                """);

        respawnButton.setOnAction(e -> onRespawn.run());
        mainMenuButton.setOnAction(e -> onMainMenu.run());

        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                deathTitle,
                titleText,
                subtitleText,
                deathCountText,
                respawnButton,
                mainMenuButton
        );

        getChildren().addAll(background, box);
    }

    public void show(DeathReasons reason, int deathCount) {
        titleText.setText(reason.getTitle());
        subtitleText.setText(reason.getSubtitle());
        deathCountText.setText("死亡次數：" + deathCount);
        setVisible(true);
        toFront();
    }

    public void hide() {
        setVisible(false);
    }
}