package com.kh.tbrr.ui;

import java.util.ArrayList;
import java.util.List;

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
 */
public class ManualScreen {

    private Stage stage;
    private Runnable onBack;

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

        // データ初期化
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
        // -fx-alternative-row-fill-visible: true; で奇数・偶数行の色分けを有効化
        // -fx-control-inner-background: ベースの色
        // -fx-control-inner-background-alt: 縞々の色
        // -fx-selection-bar: 選択時の背景色（デフォルトの濃い水色 #0096C9）
        // -fx-selection-bar-non-focused: 非フォーカス時の選択背景色（同じ色に統一）
        // -fx-focus-color: transparent; フォーカス時の枠線を消す
        // -fx-faint-focus-color: transparent; フォーカス時の薄い枠線を消す
        // -fx-fixed-cell-size: 30; セルの高さを固定してズレを完全に防ぐ
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
                            // 原点回帰：標準のテキスト表示を使用し、CSSで余白と枠線を完全に固定する
                            setText(item.getTitle());
                            setGraphic(null); // グラフィックは使用しない

                            // -fx-border-style: none; 枠線スタイルなし
                            // -fx-border-width: 0; 枠線幅0
                            // -fx-padding: 8px 5px; 上下8px 左右5px（高さ30pxに合わせて調整）
                            // -fx-background-insets: 0; 背景のインセットなし
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
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px; -fx-font-smoothing-type: lcd;"); // 白色に変更
        contentTitleLabel.setWrapText(true);
        contentTitleLabel.setFocusTraversable(false);

        // 区切り線
        Separator separator = new Separator(Orientation.HORIZONTAL);

        // コンテンツ本文
        contentBodyLabel = new Label("ここに内容が表示されます。");
        contentBodyLabel.setFont(Font.font("Arial", 18));
        contentBodyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-smoothing-type: lcd;");
        contentBodyLabel.setWrapText(true);
        contentBodyLabel.setFocusTraversable(false); // フォーカスを受け取らないようにする
        // 行間などを調整したい場合はCSSで -fx-line-spacing などを指定可能

        // スクロールペインに入れる
        VBox contentBox = new VBox(15);
        contentBox.getChildren().addAll(contentTitleLabel, separator, contentBodyLabel);
        contentBox.setStyle("-fx-background-color: transparent;");

        contentScrollPane = new ScrollPane(contentBox);
        contentScrollPane.setFitToWidth(true); // 横幅を合わせる
        contentScrollPane.setStyle("-fx-background: #333132; -fx-background-color: transparent;"); // 背景色統一
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 横スクロールバーは出さない
        contentScrollPane.setFocusTraversable(false); // スクロールペインもフォーカスを受け取らない

        VBox.setVgrow(contentScrollPane, Priority.ALWAYS);
        pane.getChildren().add(contentScrollPane);

        return pane;
    }

    /**
     * コンテンツ表示を更新
     */
    private void updateContent(ManualItem item) {
        contentTitleLabel.setText(item.getTitle());
        contentBodyLabel.setText(item.getContent());
        // スクロールを一番上に戻す
        contentScrollPane.setVvalue(0);
    }

    /**
     * データを初期化（ここでテキストを設定します）
     */
    private void initData() {
        rootNode = new TreeItem<>(new ManualItem("Root", ""));
        rootNode.setExpanded(true);
        allItems.clear();

        // --- 1. マニュアル ---
        TreeItem<ManualItem> manualRoot = createItem("マニュアル", "ゲームの基本的な遊び方について解説します。\n左のツリーから項目を選んでください。");
        manualRoot.setExpanded(true); // マニュアルは開いておく
        rootNode.getChildren().add(manualRoot);

        manualRoot.getChildren().add(createItem("はじめに",
                "【チュートリアル兼プロローグ】\n\n" +
                        "（ここにプロローグやチュートリアルのテキストを入力してください）\n\n" +
                        "テキストは ManualScreen.java の initData() メソッド内で編集できます。"));

        manualRoot.getChildren().add(createItem("基本操作",
                "【基本操作】\n\n" +
                        "（マウス操作やショートカットキーなどの説明）"));

        // --- 2. ヒント集 ---
        TreeItem<ManualItem> hintsRoot = createItem("ヒント集", "冒険に役立つヒント集です。");
        hintsRoot.setExpanded(false); // 最初は閉じておく
        rootNode.getChildren().add(hintsRoot);

        hintsRoot.getChildren().add(createItem("戦闘のコツ",
                "【戦闘のコツ】\n\n" +
                        "（敵の弱点や、スキルの使い方など）"));

        hintsRoot.getChildren().add(createItem("探索のアドバイス",
                "【探索のアドバイス】\n\n" +
                        "（食料の管理や、イベントの選び方など）"));

        // --- 3. 世界設定（旧：用語） ---
        TreeItem<ManualItem> worldRoot = createItem("世界設定", "T.B.R.R.の世界観や用語についての解説です。");
        worldRoot.setExpanded(false); // 最初は閉じておく
        rootNode.getChildren().add(worldRoot);

        // ア行～ワ行（項目のみ作成、中身は空またはプレースホルダー）
        worldRoot.getChildren().add(createItem("ア行",
                "【ア行の用語】\n\n" +
                        "■ 用語A\n" +
                        "  用語Aの説明文です。\n" +
                        "  用語Aの説明文です。\n\n" +
                        "■ 用語B\n" +
                        "  用語Bの説明文です。"));
        worldRoot.getChildren().add(createItem("カ行", "【カ行の用語】\n\n（ここにカ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("サ行", "【サ行の用語】\n\n（ここにサ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("タ行", "【タ行の用語】\n\n（ここにタ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("ナ行", "【ナ行の用語】\n\n（ここにナ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("ハ行", "【ハ行の用語】\n\n（ここにハ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("マ行", "【マ行の用語】\n\n（ここにマ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("ヤ行", "【ヤ行の用語】\n\n（ここにヤ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("ラ行", "【ラ行の用語】\n\n（ここにラ行の用語を記述）"));
        worldRoot.getChildren().add(createItem("ワ行", "【ワ行の用語】\n\n（ここにワ行の用語を記述）"));

        // --- 4. クレジット ---
        TreeItem<ManualItem> creditRoot = createItem("クレジット", "T.B.R.R. 制作クレジットです。");
        creditRoot.setExpanded(false); // 最初は閉じておく
        rootNode.getChildren().add(creditRoot);

        // サブ項目：使用素材
        creditRoot.getChildren().add(createItem("使用素材",
                "【使用素材】\n\n" +
                        "■ 画像素材\n" +
                        "・(素材サイト名など)\n\n" +
                        "■ 音響素材\n" +
                        "・(素材サイト名など)"));

        // サブ項目：ライブラリ
        creditRoot.getChildren().add(createItem("ライブラリ",
                "【使用ライブラリ】\n\n" +
                        "■ JavaFX\n" +
                        "  OpenJFX Project\n\n" +
                        "■ Gson\n" +
                        "  Google LLC"));

        treeView.setRoot(rootNode);
    }

    /**
     * ツリーアイテム作成ヘルパー
     */
    private TreeItem<ManualItem> createItem(String title, String content) {
        ManualItem item = new ManualItem(title, content);
        TreeItem<ManualItem> treeItem = new TreeItem<>(item);
        treeItem.setExpanded(false); // デフォルトでは閉じておく
        allItems.add(treeItem); // 検索用にリストにも追加
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

        TreeItem<ManualItem> filteredRoot = new TreeItem<>(new ManualItem("Root", ""));
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
        boolean match = item.getValue().getTitle().contains(filter) || item.getValue().getContent().contains(filter);

        // 子要素をチェック
        List<TreeItem<ManualItem>> matchingChildren = new ArrayList<>();
        for (TreeItem<ManualItem> child : item.getChildren()) {
            // フィルタリング用の一時的な親を作成して再帰呼び出し（構造を維持するため）
            // ここでは単純化のため、マッチした子だけをフラットに追加するか、構造を維持するかの選択になる。
            // 構造維持は少し複雑なので、今回は「親がマッチすれば子も全部表示」「子がマッチすれば親も表示」のロジックにする。

            // 簡易実装: 自分がマッチするか、子がマッチするか
            if (hasMatchingChild(child, filter)) {
                // 子をディープコピーして追加する必要があるが、JavaFXのTreeItemは親を1つしか持てないため、
                // 新しいTreeItemを作る必要がある。
                TreeItem<ManualItem> newChild = new TreeItem<>(child.getValue());
                newChild.setExpanded(true);
                filterItem(child, filter, newChild); // 再帰的に子を追加
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
        if (item.getValue().getTitle().contains(filter) || item.getValue().getContent().contains(filter)) {
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
     * マニュアル項目データクラス
     */
    public static class ManualItem {
        private String title;
        private String content;

        public ManualItem(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return title; // TreeViewでの表示に使われる
        }
    }
}
