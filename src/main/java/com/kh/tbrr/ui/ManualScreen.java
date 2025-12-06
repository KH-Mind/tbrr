package com.kh.tbrr.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * マニュアル画面
 * 左側に検索付きツリーメニュー、右側に内容を表示する構成
 * コンテンツは外部JSONファイルから読み込む
 */
public class ManualScreen {

    private static final String MANUAL_ROOT = "data/manual/";

    private Stage stage;
    private Runnable onBack;
    private Gson gson;

    // UI Components
    private TextField searchField;
    private TreeView<ManualItem> treeView;
    private Label contentTitleLabel;
    private Label contentBodyLabel;
    private ScrollPane contentScrollPane;

    // Data
    private TreeItem<ManualItem> rootNode;
    private List<TreeItem<ManualItem>> allItems; // 検索用フラットリスト

    public ManualScreen(Stage stage) {
        this.stage = stage;
        this.allItems = new ArrayList<>();
        this.gson = new GsonBuilder().create();
    }

    /**
     * マニュアル画面を表示
     * 
     * @param onBack 戻るボタンが押されたときのコールバック
     */
    public void show(Runnable onBack) {
        this.onBack = onBack;

        // メインレイアウト
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333132;");

        // 左側：ナビゲーション（検索 + ツリー）
        VBox leftPane = createLeftPane();
        // 左側の幅を固定（元の画像くらいに狭く）
        leftPane.setPrefWidth(280);
        leftPane.setMinWidth(280);
        leftPane.setMaxWidth(280);

        // 区切り線（縦）
        Separator verticalSeparator = new Separator(Orientation.VERTICAL);

        // 左ペインと区切り線をまとめる
        HBox leftContainer = new HBox(leftPane, verticalSeparator);
        root.setLeft(leftContainer);

        // 右側：内容表示エリア
        VBox rightPane = createRightPane();
        root.setCenter(rightPane);

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setTitle("T.B.R.R. ゲームマニュアル");
        stage.show();

        // データ初期化（JSONから読み込み）
        initData();

        // 初期選択（最初の項目を開く）
        if (!rootNode.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(rootNode.getChildren().get(0));
        }

        // 検索バーにフォーカスがいかないように、ルートにフォーカスを当てる
        root.requestFocus();
    }

    /**
     * 左側ペインを作成（検索バー + ツリー + 戻るボタン）
     */
    private VBox createLeftPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.setStyle("-fx-background-color: #333132;");

        // 検索バー
        searchField = new TextField();
        searchField.setPromptText("検索...");
        searchField.setFont(Font.font("Arial", 14));
        // -fx-prompt-text-fill: プレースホルダーの色
        searchField.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-prompt-text-fill: #aaaaaa;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTree(newVal));

        // ツリービュー
        treeView = new TreeView<>();
        // 縞々模様（黒の濃淡）を有効にする設定
        treeView.setStyle("-fx-font-size: 16px; " +
                "-fx-alternative-row-fill-visible: true; " +
                "-fx-control-inner-background: #333132; " +
                "-fx-control-inner-background-alt: #3b393a; " +
                "-fx-background-color: transparent; " +
                "-fx-selection-bar: #0096C9; " +
                "-fx-selection-bar-non-focused: #0096C9; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; " +
                "-fx-fixed-cell-size: 30;");
        treeView.setShowRoot(false); // ルートノードは隠す

        // ツリーセルのカスタマイズ（文字色など）
        treeView.setCellFactory(new Callback<TreeView<ManualItem>, TreeCell<ManualItem>>() {
            @Override
            public TreeCell<ManualItem> call(TreeView<ManualItem> p) {
                return new TreeCell<ManualItem>() {
                    @Override
                    protected void updateItem(ManualItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            setText(item.getTitle());
                            setGraphic(null);
                            setStyle(
                                    "-fx-text-fill: white; -fx-background-color: transparent; -fx-border-style: none; -fx-border-width: 0; -fx-padding: 5px; -fx-background-insets: 0; -fx-font-smoothing-type: lcd;");
                        }
                    }
                };
            }
        });

        // 選択時のイベント
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                updateContent(newVal.getValue());
            }
        });

        VBox.setVgrow(treeView, Priority.ALWAYS);

        // 戻るボタン
        Button backButton = new Button("戻る");
        backButton.setMaxWidth(Double.MAX_VALUE); // 横幅いっぱいに
        backButton.setPrefHeight(40);
        backButton.setFont(Font.font("Arial", 16));
        backButton.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;");

        backButton.setOnMouseEntered(e -> backButton.setStyle(
                "-fx-background-color: #555555; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #888888; " +
                        "-fx-border-width: 2px;"));

        backButton.setOnMouseExited(e -> backButton.setStyle(
                "-fx-background-color: #444444; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #666666; " +
                        "-fx-border-width: 2px;"));

        backButton.setOnAction(e -> {
            if (onBack != null)
                onBack.run();
        });

        pane.getChildren().addAll(searchField, treeView, backButton);

        return pane;
    }

    /**
     * 右側ペインを作成（内容表示）
     */
    private VBox createRightPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: #333132;"); // 背景色を左側と統一

        // コンテンツタイトル
        contentTitleLabel = new Label("タイトル");
        contentTitleLabel.setFont(Font.font("Arial", 24));
        contentTitleLabel.setStyle(
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px; -fx-font-smoothing-type: lcd;");
        contentTitleLabel.setWrapText(true);
        contentTitleLabel.setFocusTraversable(false);

        // 区切り線
        Separator separator = new Separator(Orientation.HORIZONTAL);

        // コンテンツ本文
        contentBodyLabel = new Label("ここに内容が表示されます。");
        contentBodyLabel.setFont(Font.font("Arial", 18));
        contentBodyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-smoothing-type: lcd;");
        contentBodyLabel.setWrapText(true);
        contentBodyLabel.setFocusTraversable(false);

        // スクロールペインに入れる
        VBox contentBox = new VBox(15);
        contentBox.getChildren().addAll(contentTitleLabel, separator, contentBodyLabel);
        contentBox.setStyle("-fx-background-color: transparent;");

        contentScrollPane = new ScrollPane(contentBox);
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setStyle("-fx-background: #333132; -fx-background-color: transparent;");
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScrollPane.setFocusTraversable(false);

        VBox.setVgrow(contentScrollPane, Priority.ALWAYS);
        pane.getChildren().add(contentScrollPane);

        return pane;
    }

    /**
     * コンテンツ表示を更新
     */
    private void updateContent(ManualItem item) {
        contentTitleLabel.setText(item.getTitle());

        // 子項目の場合はJSONファイルから本文を読み込む
        if (item.getFilePath() != null && !item.getFilePath().isEmpty()) {
            String content = loadContentFromFile(item.getFilePath());
            contentBodyLabel.setText(content != null ? content : item.getContent());
        } else {
            contentBodyLabel.setText(item.getContent());
        }

        // スクロールを一番上に戻す
        contentScrollPane.setVvalue(0);
    }

    /**
     * JSONファイルからコンテンツを読み込む
     */
    private String loadContentFromFile(String filePath) {
        try {
            String fullPath = MANUAL_ROOT + filePath;
            String json = loadResourceContent(fullPath);
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            if (obj != null && obj.has("content")) {
                return obj.get("content").getAsString();
            }
        } catch (Exception e) {
            System.err.println("[ManualScreen] コンテンツ読み込みエラー: " + filePath + " - " + e.getMessage());
        }
        return null;
    }

    /**
     * データを初期化（JSONから読み込み）
     */
    private void initData() {
        rootNode = new TreeItem<>(new ManualItem("Root", "", null));
        rootNode.setExpanded(true);
        allItems.clear();

        try {
            // manual_index.jsonを読み込む
            String indexJson = loadResourceContent(MANUAL_ROOT + "manual_index.json");
            JsonObject indexObj = gson.fromJson(indexJson, JsonObject.class);

            if (indexObj != null && indexObj.has("sections")) {
                JsonArray sections = indexObj.getAsJsonArray("sections");

                for (JsonElement sectionElem : sections) {
                    JsonObject section = sectionElem.getAsJsonObject();
                    String id = section.get("id").getAsString();
                    String title = section.get("title").getAsString();
                    String description = section.has("description") ? section.get("description").getAsString() : "";

                    // 大項目を作成（descriptionは内部コーディング）
                    TreeItem<ManualItem> sectionNode = createItem(title, description, null);
                    sectionNode.setExpanded("manual".equals(id)); // マニュアルのみ開いておく
                    rootNode.getChildren().add(sectionNode);

                    // 子項目を追加
                    if (section.has("children")) {
                        JsonArray children = section.getAsJsonArray("children");
                        for (JsonElement childElem : children) {
                            JsonObject child = childElem.getAsJsonObject();
                            String childTitle = child.get("title").getAsString();
                            String childFile = child.has("file") ? child.get("file").getAsString() : null;

                            // 子項目を作成（本文は選択時にJSONから読み込む）
                            TreeItem<ManualItem> childNode = createItem(childTitle, "読み込み中...", childFile);
                            sectionNode.getChildren().add(childNode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ManualScreen] インデックス読み込みエラー: " + e.getMessage());
            e.printStackTrace();
            // フォールバック：エラーメッセージを表示
            rootNode.getChildren().add(createItem("エラー", "マニュアルデータの読み込みに失敗しました。\n" + e.getMessage(), null));
        }

        treeView.setRoot(rootNode);
    }

    /**
     * ツリーアイテム作成ヘルパー
     */
    private TreeItem<ManualItem> createItem(String title, String content, String filePath) {
        ManualItem item = new ManualItem(title, content, filePath);
        TreeItem<ManualItem> treeItem = new TreeItem<>(item);
        treeItem.setExpanded(false);
        allItems.add(treeItem);
        return treeItem;
    }

    /**
     * ツリーのフィルタリング
     */
    private void filterTree(String filter) {
        if (filter == null || filter.isEmpty()) {
            treeView.setRoot(rootNode);
            return;
        }

        TreeItem<ManualItem> filteredRoot = new TreeItem<>(new ManualItem("Root", "", null));
        filteredRoot.setExpanded(true);

        for (TreeItem<ManualItem> child : rootNode.getChildren()) {
            filterItem(child, filter, filteredRoot);
        }

        treeView.setRoot(filteredRoot);
    }

    /**
     * 再帰的にアイテムをフィルタリング
     */
    private void filterItem(TreeItem<ManualItem> item, String filter, TreeItem<ManualItem> newParent) {
        ManualItem manualItem = item.getValue();

        // タイトルでマッチを確認
        boolean match = manualItem.getTitle().contains(filter);

        // コンテンツでもマッチを確認（ファイルから読み込んで検索）
        if (!match && manualItem.getFilePath() != null) {
            String content = loadContentFromFile(manualItem.getFilePath());
            if (content != null && content.contains(filter)) {
                match = true;
            }
        } else if (!match) {
            match = manualItem.getContent().contains(filter);
        }

        // 子要素をチェック
        List<TreeItem<ManualItem>> matchingChildren = new ArrayList<>();
        for (TreeItem<ManualItem> child : item.getChildren()) {
            if (hasMatchingChild(child, filter)) {
                TreeItem<ManualItem> newChild = new TreeItem<>(child.getValue());
                newChild.setExpanded(true);
                filterItem(child, filter, newChild);
                matchingChildren.add(newChild);
            }
        }

        if (match || !matchingChildren.isEmpty()) {
            TreeItem<ManualItem> newItem = new TreeItem<>(item.getValue());
            newItem.setExpanded(true);
            newItem.getChildren().addAll(matchingChildren);
            newParent.getChildren().add(newItem);
        }
    }

    // ヘルパー：自分または子孫にマッチするものがあるか
    private boolean hasMatchingChild(TreeItem<ManualItem> item, String filter) {
        ManualItem manualItem = item.getValue();

        // タイトルでマッチ
        if (manualItem.getTitle().contains(filter)) {
            return true;
        }

        // コンテンツでマッチ（ファイルから読み込んで検索）
        if (manualItem.getFilePath() != null) {
            String content = loadContentFromFile(manualItem.getFilePath());
            if (content != null && content.contains(filter)) {
                return true;
            }
        } else if (manualItem.getContent().contains(filter)) {
            return true;
        }

        for (TreeItem<ManualItem> child : item.getChildren()) {
            if (hasMatchingChild(child, filter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * リソースコンテンツを読み込む（jpackage対応）
     * DataManagerと同様のロジック
     */
    private String loadResourceContent(String path) throws IOException {
        // 1. ローカルファイルシステム (優先: MOD/ユーザーデータ)
        File file = new File(path);
        if (file.exists()) {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }

        // 2. 開発環境用フォールバック (src/main/resources)
        File devFile = new File("src/main/resources/" + path);
        if (devFile.exists()) {
            return new String(Files.readAllBytes(devFile.toPath()), StandardCharsets.UTF_8);
        }

        // 3. jpackageポータブル版用パス（app/data/...）
        String jpackagePath = "app/" + path;
        File jpackageFile = new File(jpackagePath);
        if (jpackageFile.exists()) {
            return new String(Files.readAllBytes(jpackageFile.toPath()), StandardCharsets.UTF_8);
        }

        // 4. クラスパス (JAR内 / ビルド済みリソース)
        String resourcePath = path.replace("\\", "/");

        // A. 相対パスで試行
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // B. 絶対パス（先頭スラッシュあり）で試行
        String absolutePath = "/" + resourcePath;
        try (InputStream is = getClass().getResourceAsStream(absolutePath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // C. data/ を除いたパスでも試行
        if (resourcePath.startsWith("data/")) {
            String strippedPath = resourcePath.substring(5);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(strippedPath)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            try (InputStream is = getClass().getResourceAsStream("/" + strippedPath)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        throw new IOException("Resource not found: " + path);
    }

    /**
     * マニュアル項目データクラス
     */
    public static class ManualItem {
        private String title;
        private String content;
        private String filePath; // JSONファイルへのパス（子項目用）

        public ManualItem(String title, String content, String filePath) {
            this.title = title;
            this.content = content;
            this.filePath = filePath;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getFilePath() {
            return filePath;
        }

        @Override
        public String toString() {
            return title; // TreeViewでの表示に使われる
        }
    }
}
