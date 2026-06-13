package com.kh.tbrr.ui;

import java.util.List;

import com.kh.tbrr.data.models.GraveRecord;
import com.kh.tbrr.manager.GraveyardManager;
import com.kh.tbrr.manager.ImageManager;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 墓地 閲覧画面
 * 過去に死亡したキャラクターの記録一覧を表示する。
 * GraveyardManager.loadAllRecords() でuserdata/memory/graveyard/ からデータを読み込む。
 */
public class GraveyardScreen {

    private Stage stage;
    private ImageManager imageManager;

    public GraveyardScreen(Stage stage) {
        this.stage = stage;
        this.imageManager = new ImageManager();
    }

    /**
     * 墓地画面を表示する
     *
     * @param onBack 「戻る」ボタンを押したときの処理
     */
    public void show(Runnable onBack) {
        // 背景画像
        Image backgroundImage = imageManager.loadBackground("mainmenu.png");
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setFitWidth(1600);
        backgroundView.setFitHeight(900);
        backgroundView.setPreserveRatio(false);

        // メインコンテンツのVBox
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(60, 80, 60, 80));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // タイトルラベル
        Label titleLabel = new Label("墓　地");
        titleLabel.setFont(Font.font("Arial", 32));
        titleLabel.setStyle("-fx-text-fill: #cccccc;");

        // 記録の読み込み
        List<GraveRecord> records = GraveyardManager.loadAllRecords();

        if (records.isEmpty()) {
            // 記録がない場合のメッセージ
            Label emptyLabel = new Label("まだ誰も倒れていない。");
            emptyLabel.setFont(Font.font("Arial", 20));
            emptyLabel.setStyle("-fx-text-fill: #aaaaaa;");

            Button backBtn = createButton("戻る");
            backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

            contentBox.getChildren().addAll(titleLabel, emptyLabel, backBtn);
        } else {
            // 記録一覧のTableView
            TableView<GraveRecord> table = buildTable(records);

            // 件数ラベル
            Label countLabel = new Label(records.size() + " 件の記録");
            countLabel.setFont(Font.font("Arial", 14));
            countLabel.setStyle("-fx-text-fill: #888888;");

            Button backBtn = createButton("戻る");
            backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

            contentBox.getChildren().addAll(titleLabel, countLabel, table, backBtn);
        }

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundView, contentBox);
        // 半透明の暗いオーバーレイ（背景を暗くしてテキストを読みやすくする）
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(1600, 900);
        overlay.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.6));
        root.getChildren().add(1, overlay); // 背景画像の上、コンテンツの下に挿入

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("TBRR - 墓地");
        stage.show();
    }

    /**
     * 墓地記録のTableViewを構築する
     */
    @SuppressWarnings("unchecked")
    private TableView<GraveRecord> buildTable(List<GraveRecord> records) {
        TableView<GraveRecord> table = new TableView<>();
        table.setMaxWidth(1200);
        table.setPrefHeight(600);
        table.setStyle(
                "-fx-background-color: rgba(30,30,30,0.85); " +
                        "-fx-control-inner-background: #222222; " +
                        "-fx-table-header-border-color: #555555; " +
                        "-fx-text-fill: #cccccc;");

        // 列：キャラクター名
        TableColumn<GraveRecord, String> nameCol = new TableColumn<>("名前");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCharacterName()));
        nameCol.setPrefWidth(250);
        nameCol.setStyle("-fx-text-fill: #cccccc;");

        // 列：職業
        TableColumn<GraveRecord, String> jobCol = new TableColumn<>("職業");
        jobCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCharacterJob()));
        jobCol.setPrefWidth(200);

        // 列：到達フロア
        TableColumn<GraveRecord, Number> floorCol = new TableColumn<>("到達フロア");
        floorCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getFloor()));
        floorCol.setPrefWidth(150);

        // 列：死亡イベント
        TableColumn<GraveRecord, String> deathCol = new TableColumn<>("死亡の原因");
        deathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeathEvent()));
        deathCol.setPrefWidth(400);

        table.getColumns().addAll(nameCol, jobCol, floorCol, deathCol);
        table.setItems(FXCollections.observableArrayList(records));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // テーブルのスタイル調整
        table.setStyle(
                "-fx-background-color: #1a1a1a; " +
                        "-fx-border-color: #555555; " +
                        "-fx-border-width: 1px;");

        return table;
    }

    /**
     * ボタンを作成（MainMenuScreenと同じスタイル）
     */
    private Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(55);
        button.setFont(Font.font("Arial", 18));
        button.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;");

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #555555; " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: #888888; " +
                            "-fx-border-width: 2px;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: #444444; " +
                            "-fx-text-fill: white; " +
                            "-fx-border-color: #666666; " +
                            "-fx-border-width: 2px;");
        });

        return button;
    }
}
