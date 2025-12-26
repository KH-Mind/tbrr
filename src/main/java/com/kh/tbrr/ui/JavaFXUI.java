package com.kh.tbrr.ui;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.system.DeveloperMode;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX版UI実装（1600x900、Enterキー対応、立ち絵エリア追加）
 */
public class JavaFXUI implements GameUI {

	private Stage stage;
	private DeveloperMode developerMode;

	// ゲーム画面の主要コンポーネント
	private Label hpLabel;
	private Label apLabel;
	private Label moneyLabel;
	private TextArea messageArea;
	private ImageView eventImageView;
	private TextField inputField;
	private FlowPane skillsPane; // 技能アイコン表示用
	private FlowPane itemsPane; // アイテムアイコン表示用
	private FlowPane statusEffectsPane; // 状態異常アイコン表示用

	private Label characterImageLabel;

	// 画像読み込みとか
	private ImageView characterPortraitView;
	private com.kh.tbrr.manager.ImageManager imageManager;
	private Player currentPlayer;

	// マップ・イベント情報（イベント挿画エリア上部に重ねる）
	private Label mapInfoLabel;
	private Label eventInfoLabel;

	// サブウィンドウ
	private VBox subWindowBox;
	private ImageView subWindowImageView; // サブウィンドウ用画像表示
	private Scene gameScene;

	// 重要ログ置き場
	private TextArea importantLogArea;

	// 入力待機用
	private CountDownLatch inputLatch;
	private AtomicReference<String> inputResult = new AtomicReference<>("");

	// インタラクション（ミニゲーム）用入力ハンドラー
	private java.util.function.Consumer<String> interactionInputHandler;

	public JavaFXUI(Stage stage, DeveloperMode developerMode) {
		this.stage = stage;
		this.developerMode = developerMode;
		this.imageManager = new com.kh.tbrr.manager.ImageManager(); // ← 追加
	}

	/**
	 * ゲーム画面を初期化
	 */
	public void initializeGameScreen() {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10));
		root.setStyle("-fx-background-color: #333132;");
		// 中央エリア
		VBox leftBox = createLeftArea();
		HBox.setHgrow(leftBox, Priority.ALWAYS);

		root.setCenter(leftBox);

		// 右側（立ち絵など）
		root.setRight(createRightPanel());

		// シーン設定
		gameScene = new Scene(root, 1600, 900);
		stage.setTitle("T.B.R.R.");
		stage.setScene(gameScene);
		stage.setResizable(false);
		stage.show();

		// ★追加: スクロールバーを常に表示する（二重表示防止のため内部ScrollPaneに適用）
		Platform.runLater(() -> {
			javafx.scene.Node scrollPane = messageArea.lookup(".scroll-pane");
			if (scrollPane != null) {
				scrollPane.setStyle("-fx-vbar-policy: always;");
			}
		});
	}

	/**
	 * 左側エリア全体（イベント挿画、メッセージ、入力）
	 */
	private VBox createLeftArea() {
		VBox leftBox = new VBox(10);
		leftBox.setPadding(new Insets(0, 10, 0, 0)); // 右側だけ10px（右パネルとの間隔）

		// ━━━ 全体を横に並べる：左列（イベント挿画 + メッセージ + 入力）+ サブウィンドウ ━━━
		HBox mainRow = new HBox(10);

		// 左列：イベント挿画、メッセージエリア、入力フィールドを縦に配置
		VBox leftColumn = new VBox(10);

		StackPane eventStackPane = createEventAreaWithOverlay();

		VBox messageBox = createMessageArea();
		VBox.setVgrow(messageBox, Priority.ALWAYS);

		HBox inputBox = createBottomInputArea();

		leftColumn.getChildren().addAll(eventStackPane, messageBox, inputBox);
		HBox.setHgrow(leftColumn, Priority.NEVER); // 固定幅

		// 右列：サブウィンドウ、重要ログ置き場、テンキーエリアを含むVBox
		VBox rightColumn = createRightColumn();

		mainRow.getChildren().addAll(leftColumn, rightColumn);
		VBox.setVgrow(mainRow, Priority.ALWAYS);

		leftBox.getChildren().add(mainRow);
		return leftBox;
	}

	/**
	 * イベント挿画エリア（800×450）+ 上部に重ねるマップ/イベント情報
	 */
	private StackPane createEventAreaWithOverlay() {
		StackPane stackPane = new StackPane();
		stackPane.setPrefSize(804, 454);
		stackPane.setMaxSize(804, 454);
		stackPane.setMinSize(804, 454);
		stackPane.setStyle(
				"-fx-background-color: #333333; " +
						"-fx-border-color: #666666; " +
						"-fx-border-width: 2px;");

		// 背景：イベント画像エリア（800×450）
		eventImageView = new ImageView();
		eventImageView.setFitWidth(800);
		eventImageView.setFitHeight(450);
		eventImageView.setPreserveRatio(false);
		eventImageView.setSmooth(true);
		eventImageView.setStyle(
				"-fx-background-color: #222222;");

		// 上部オーバーレイ：マップ情報とイベント情報（内側に配置）
		// 上部オーバーレイ：マップ情報とイベント情報（1行表示、マップ画像の上に重ねて配置）
		HBox overlayBox = new HBox(30);
		overlayBox.setAlignment(Pos.CENTER_LEFT);
		overlayBox.setPadding(new Insets(5, 10, 5, 10)); // 上下5px、左右10px
		overlayBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);"); // 透過率60%の黒
		overlayBox.setPrefHeight(35); // 1行分の高さ
		overlayBox.setMaxHeight(35);
		overlayBox.setMaxWidth(784); // 内側に収める（804-20）

		// マップ情報ラベル（1行表示）
		mapInfoLabel = new Label("フロア1 | 現在の地形：エリア名");
		mapInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

		// イベント情報ラベル（1行表示）
		eventInfoLabel = new Label("現在のイベント情報：（イベント情報）");
		eventInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

		overlayBox.getChildren().addAll(mapInfoLabel, eventInfoLabel);
		StackPane.setAlignment(overlayBox, Pos.TOP_LEFT);
		StackPane.setMargin(overlayBox, new Insets(10)); // 枠の内側に配置

		stackPane.getChildren().addAll(eventImageView, overlayBox);
		return stackPane;
	}

	/**
	 * サブウィンドウ（正方形：450×450固定）
	 */
	private VBox createSubWindow() {
		VBox subWindow = new VBox(0); // 間隔を0に変更
		subWindow.setPadding(new Insets(0)); // パディングも0に変更
		subWindow.setStyle(
				"-fx-background-color: #3a3a3a; " +
						"-fx-border-color: #666666; " +
						"-fx-border-width: 2px;");
		subWindow.setPrefWidth(454);
		subWindow.setMaxWidth(454);
		subWindow.setMinWidth(454);
		subWindow.setPrefHeight(454);
		subWindow.setMinHeight(454);
		subWindow.setMaxHeight(454);

		// サブウィンドウ用画像表示エリア
		subWindowImageView = new ImageView();
		subWindowImageView.setFitWidth(450); // 枠いっぱいに表示
		subWindowImageView.setFitHeight(450); // 正方形いっぱいに表示
		subWindowImageView.setPreserveRatio(false);
		subWindowImageView.setSmooth(true);
		subWindowImageView.setStyle("-fx-background-color: #222222;");

		subWindow.getChildren().add(subWindowImageView); // 画像のみ追加
		return subWindow;
	}

	/**
	 * 右列全体（サブウィンドウ + 重要ログ置き場 + テンキーエリア）
	 */
	private VBox createRightColumn() {
		VBox rightColumn = new VBox(10);

		// サブウィンドウ（450×450固定）
		subWindowBox = createSubWindow();

		// 重要ログ置き場（可変サイズ、残りのスペースを埋める）
		VBox importantLogBox = createImportantLogArea();
		VBox.setVgrow(importantLogBox, Priority.ALWAYS); // 残りのスペースを埋める

		// テンキーエリア
		VBox numpadContainer = createNumpadArea();

		rightColumn.getChildren().addAll(subWindowBox, importantLogBox, numpadContainer);
		return rightColumn;
	}

	/**
	 * テンキーボタンクリック時の処理
	 */
	private void handleNumpadButtonClick(String buttonText) {
		Platform.runLater(() -> {
			switch (buttonText) {
				case "D":
					// 入力フィールドをクリア
					inputField.clear();
					break;
				case "E":
					// エンター処理（決定）
					if (inputLatch != null) {
						inputResult.set(inputField.getText().trim());
						inputField.clear();
						inputLatch.countDown();
					}
					break;
				default:
					// 数字を追加
					inputField.appendText(buttonText);
					break;
			}
		});
	}

	/**
	 * 重要ログ置き場を作成
	 */
	private VBox createImportantLogArea() {
		VBox logBox = new VBox(0);
		logBox.setPadding(new Insets(0));
		logBox.setStyle("-fx-background-color: #2b2b2b;");
		logBox.setPrefWidth(454);
		logBox.setMaxWidth(454);
		logBox.setMinWidth(454);
		// 高さは可変（VBox.setVgrowで制御）

		importantLogArea = new TextArea();
		importantLogArea.setEditable(false);
		importantLogArea.setWrapText(true);
		importantLogArea.setFont(Font.font("MS Gothic", 14));
		importantLogArea.setStyle("-fx-control-inner-background: #2b2b2b; -fx-text-fill: #cccccc;");
		VBox.setVgrow(importantLogArea, Priority.ALWAYS);

		logBox.getChildren().add(importantLogArea);
		return logBox;
	}

	/**
	 * テンキーエリアを作成（VBoxで返す）
	 * レイアウト:
	 * [7][8][9] [？][↑][？] [設定]
	 * [4][5][6] [←][○][→] [ステ]
	 * [1][2][3] [？][↓][？]
	 * [0][D][E]
	 */
	private VBox createNumpadArea() {
		VBox container = new VBox(0);
		container.setStyle("-fx-background-color: #2b2b2b;");
		container.setPadding(new Insets(10));

		GridPane numpad = new GridPane();
		numpad.setHgap(0);
		numpad.setVgap(0);

		int buttonSize = 35;
		int wideButtonWidth = buttonSize * 3; // 105px

		// 数字ボタンとD, E
		String[][] buttonDef = {
				{ "7", "0", "0", "1" }, { "8", "1", "0", "1" }, { "9", "2", "0", "1" },
				{ "4", "0", "1", "1" }, { "5", "1", "1", "1" }, { "6", "2", "1", "1" },
				{ "1", "0", "2", "1" }, { "2", "1", "2", "1" }, { "3", "2", "2", "1" },
				{ "0", "0", "3", "1" }, { "D", "1", "3", "1" }, { "E", "2", "3", "1" }
		};

		for (String[] def : buttonDef) {
			String text = def[0];
			int col = Integer.parseInt(def[1]);
			int row = Integer.parseInt(def[2]);
			int colspan = Integer.parseInt(def[3]);

			Button btn = new Button(text);
			btn.setPrefSize(buttonSize, buttonSize);
			btn.setMinSize(buttonSize, buttonSize);
			btn.setMaxSize(buttonSize, buttonSize);
			btn.setFont(Font.font("MS Gothic", 14));
			btn.setOnAction(e -> handleNumpadButtonClick(text));

			numpad.add(btn, col, row, colspan, 1);
		}

		// ================== 十字キーパッド（3x3グリッド） ==================
		// 配置: 列3-5, 行0-2
		// [？][↑][？] (row 0)
		// [←][○][→] (row 1)
		// [？][↓][？] (row 2)

		// 将来拡張用プレースホルダー（左上）
		Button placeholder1 = createPlaceholderButton("？", buttonSize);
		GridPane.setMargin(placeholder1, new Insets(0, 0, 0, 10));
		numpad.add(placeholder1, 3, 0);

		// ↑ボタン
		Button upBtn = new Button("↑");
		upBtn.setPrefSize(buttonSize, buttonSize);
		upBtn.setMinSize(buttonSize, buttonSize);
		upBtn.setMaxSize(buttonSize, buttonSize);
		upBtn.setFont(Font.font("Meiryo", 14));
		upBtn.setOnAction(e -> handleDirectionButtonClick("UP"));
		numpad.add(upBtn, 4, 0);

		// 将来拡張用プレースホルダー（右上）
		Button placeholder2 = createPlaceholderButton("？", buttonSize);
		numpad.add(placeholder2, 5, 0);

		// ←ボタン
		Button leftBtn = new Button("←");
		leftBtn.setPrefSize(buttonSize, buttonSize);
		leftBtn.setMinSize(buttonSize, buttonSize);
		leftBtn.setMaxSize(buttonSize, buttonSize);
		leftBtn.setFont(Font.font("Meiryo", 14));
		leftBtn.setOnAction(e -> handleDirectionButtonClick("LEFT"));
		GridPane.setMargin(leftBtn, new Insets(0, 0, 0, 10));
		numpad.add(leftBtn, 3, 1);

		// ○ボタン（決定/連打用）
		Button actionBtn = new Button("○");
		actionBtn.setPrefSize(buttonSize, buttonSize);
		actionBtn.setMinSize(buttonSize, buttonSize);
		actionBtn.setMaxSize(buttonSize, buttonSize);
		actionBtn.setFont(Font.font("Meiryo", 14));
		actionBtn.setOnAction(e -> handleDirectionButtonClick("ACTION"));
		numpad.add(actionBtn, 4, 1);

		// →ボタン
		Button rightBtn = new Button("→");
		rightBtn.setPrefSize(buttonSize, buttonSize);
		rightBtn.setMinSize(buttonSize, buttonSize);
		rightBtn.setMaxSize(buttonSize, buttonSize);
		rightBtn.setFont(Font.font("Meiryo", 14));
		rightBtn.setOnAction(e -> handleDirectionButtonClick("RIGHT"));
		numpad.add(rightBtn, 5, 1);

		// 将来拡張用プレースホルダー（左下）
		Button placeholder3 = createPlaceholderButton("？", buttonSize);
		GridPane.setMargin(placeholder3, new Insets(0, 0, 0, 10));
		numpad.add(placeholder3, 3, 2);

		// ↓ボタン
		Button downBtn = new Button("↓");
		downBtn.setPrefSize(buttonSize, buttonSize);
		downBtn.setMinSize(buttonSize, buttonSize);
		downBtn.setMaxSize(buttonSize, buttonSize);
		downBtn.setFont(Font.font("Meiryo", 14));
		downBtn.setOnAction(e -> handleDirectionButtonClick("DOWN"));
		numpad.add(downBtn, 4, 2);

		// 将来拡張用プレースホルダー（右下）
		Button placeholder4 = createPlaceholderButton("？", buttonSize);
		numpad.add(placeholder4, 5, 2);

		// ================== 設定・ステータスボタン（6列目、縦配置） ==================

		// 設定ボタン（row 0）
		Button configBtn = new Button("設定");
		configBtn.setPrefSize(wideButtonWidth, buttonSize);
		configBtn.setMinSize(wideButtonWidth, buttonSize);
		configBtn.setMaxSize(wideButtonWidth, buttonSize);
		configBtn.setFont(Font.font("MS Gothic", 14));
		configBtn.setOnAction(e -> showConfigDialog());
		GridPane.setMargin(configBtn, new Insets(0, 0, 0, 10));
		numpad.add(configBtn, 6, 0, 1, 1);

		// ステータスボタン（row 1）
		Button statusBtn = new Button("ステータス");
		statusBtn.setPrefSize(wideButtonWidth, buttonSize);
		statusBtn.setMinSize(wideButtonWidth, buttonSize);
		statusBtn.setMaxSize(wideButtonWidth, buttonSize);
		statusBtn.setFont(Font.font("MS Gothic", 14));
		statusBtn.setOnAction(e -> showStatusDialog());
		GridPane.setMargin(statusBtn, new Insets(0, 0, 0, 10));
		numpad.add(statusBtn, 6, 1, 1, 1);

		// 将来拡張用プレースホルダー（6列目 row 2, 3）
		for (int i = 2; i <= 3; i++) {
			Button placeholder = createPlaceholderButton("？", buttonSize);
			placeholder.setPrefSize(wideButtonWidth, buttonSize);
			placeholder.setMinSize(wideButtonWidth, buttonSize);
			placeholder.setMaxSize(wideButtonWidth, buttonSize);
			GridPane.setMargin(placeholder, new Insets(0, 0, 0, 10));
			numpad.add(placeholder, 6, i, 1, 1);
		}

		container.getChildren().add(numpad);
		return container;
	}

	/**
	 * 将来拡張用のプレースホルダーボタンを作成
	 */
	private Button createPlaceholderButton(String text, int size) {
		Button btn = new Button(text);
		btn.setPrefSize(size, size);
		btn.setMinSize(size, size);
		btn.setMaxSize(size, size);
		btn.setFont(Font.font("MS Gothic", 11));
		btn.setDisable(true);
		btn.setStyle("-fx-background-color: #555555; -fx-text-fill: #888888;");
		return btn;
	}

	/**
	 * 方向ボタン・アクションボタンのクリック処理
	 */
	private void handleDirectionButtonClick(String direction) {
		Platform.runLater(() -> {
			// インタラクション実行中の場合はイベントをディスパッチ
			if (interactionInputHandler != null) {
				interactionInputHandler.accept(direction);
				return;
			}

			// 通常時はEnter相当（ACTION）または無視
			if ("ACTION".equals(direction)) {
				// Enterと同等の処理
				if (inputLatch != null) {
					inputResult.set(inputField.getText().trim());
					inputField.clear();
					inputLatch.countDown();
				}
			}
			// 方向キーは通常時は何もしない（将来的にメニュー操作等に使用可能）
		});
	}

	/**
	 * メッセージエリア
	 */
	private VBox createMessageArea() {
		VBox messageBox = new VBox(0);
		messageBox.setStyle("-fx-background-color: #2b2b2b;");
		messageBox.setPadding(new Insets(0));

		messageArea = new TextArea();
		messageArea.setEditable(false);
		messageArea.setWrapText(true);
		messageArea.setFont(Font.font("MS Gothic", 16));
		messageArea.setStyle("-fx-control-inner-background: #2b2b2b; -fx-text-fill: white;");
		VBox.setVgrow(messageArea, Priority.ALWAYS);

		messageBox.getChildren().add(messageArea);
		return messageBox;
	}

	/**
	 * 下部入力エリアを作成（右パネルを貫通させない）
	 */
	private HBox createBottomInputArea() {
		HBox bottomBox = new HBox(10);
		bottomBox.setPadding(new Insets(0));
		bottomBox.setStyle("-fx-background-color: #2b2b2b;");
		bottomBox.setAlignment(Pos.CENTER_LEFT);

		inputField = new TextField();
		inputField.setPromptText("選択肢の番号を入力してEnter（Enterだけで次へ）");
		inputField.setFont(Font.font("MS Gothic", 14));
		inputField.setPrefHeight(35);
		HBox.setHgrow(inputField, Priority.ALWAYS);

		inputField.setOnAction(e -> {
			String input = inputField.getText().trim();
			if (inputLatch != null) {
				inputResult.set(input);
				inputField.clear();
				inputLatch.countDown();
			}
		});

		bottomBox.getChildren().add(inputField);
		return bottomBox;
	}

	/**
	 * 右側パネルを作成（HP/AP、スキル、アイテム、立ち絵）
	 */
	private VBox createRightPanel() {
		VBox rightPanel = new VBox(10);
		rightPanel.setPadding(new Insets(10)); // 内部マージン復活
		rightPanel.setPrefWidth(308);
		rightPanel.setMaxWidth(308);
		rightPanel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");

		// HP/AP/お金表示（最上部・横並び）
		HBox statusBox = new HBox(15);
		statusBox.setAlignment(Pos.CENTER);
		statusBox.setPadding(new Insets(8));
		statusBox.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #aaaaaa; -fx-border-width: 1px;");

		hpLabel = new Label("HP: 100/100");
		hpLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 14px; -fx-font-weight: bold;");

		apLabel = new Label("AP: 20/20");
		apLabel.setStyle("-fx-text-fill: #6666ff; -fx-font-size: 14px; -fx-font-weight: bold;");

		moneyLabel = new Label("銀貨: 30/100");
		moneyLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 14px; -fx-font-weight: bold;");

		statusBox.getChildren().addAll(hpLabel, apLabel, moneyLabel);

		// 技能表示（アイコン横8列表示、白枠なし）
		skillsPane = new FlowPane(4, 4); // 水平・垂直間隔4px
		skillsPane.setPrefWidth(284); // 32*8 + 4*7 = 284px
		skillsPane.setPrefHeight(68); // 2行分: 32*2 + 4 = 68px
		skillsPane.setStyle("-fx-background-color: transparent;");

		// 所持品表示（アイコン横8列表示、白枠なし）
		itemsPane = new FlowPane(4, 4); // 水平・垂直間隔4px
		itemsPane.setPrefWidth(284); // 32*8 + 4*7 = 284px
		itemsPane.setPrefHeight(68); // 2行分: 32*2 + 4 = 68px
		itemsPane.setStyle("-fx-background-color: transparent;");

		// 状態異常表示（アイコン横8列表示、白枠なし）
		statusEffectsPane = new FlowPane(4, 4); // 水平・垂直間隔4px
		statusEffectsPane.setPrefWidth(284); // 32*8 + 4*7 = 284px
		statusEffectsPane.setPrefHeight(68); // 2行分: 32*2 + 4 = 68px
		statusEffectsPane.setStyle("-fx-background-color: transparent;");

		// 立ち絵エリア
		characterPortraitView = new ImageView();
		characterPortraitView.setFitWidth(288);
		characterPortraitView.setFitHeight(512);
		characterPortraitView.setPreserveRatio(true);
		characterPortraitView.setStyle(
				"-fx-background-color: #e0e0e0; " +
						"-fx-border-color: #999999; " +
						"-fx-border-width: 2px;");
		VBox.setVgrow(characterPortraitView, Priority.ALWAYS);

		rightPanel.getChildren().addAll(
				statusBox,
				new Separator(),
				skillsPane,
				new Separator(),
				itemsPane,
				new Separator(),
				statusEffectsPane,
				new Separator(),
				characterPortraitView);

		return rightPanel;
	}

	// ========================================
	// GameUIインターフェースの実装
	// ========================================

	@Override
	public void print(String message) {
		Platform.runLater(() -> {
			messageArea.appendText(message + "\n");
			// ★改善: PauseTransitionで少し待ってからスクロール（レイアウト更新待ち）
			PauseTransition pause = new PauseTransition(Duration.millis(50));
			pause.setOnFinished(e -> {
				messageArea.selectEnd(); // 末尾を選択して表示位置を合わせる
				messageArea.deselect(); // 選択解除
				messageArea.setScrollTop(Double.MAX_VALUE); // 強制的に最下部へ
			});
			pause.play();
		});
	}

	@Override
	public void printError(String message) {
		print("【エラー】" + message);
	}

	@Override
	public void printWarning(String message) {
		print("【警告】" + message);
	}

	@Override
	public void printTitleBar(String title) {
		Platform.runLater(() -> {
			messageArea.appendText("\n" + "╔".repeat(40) + "\n");
			messageArea.appendText(" " + title + "\n");
			messageArea.appendText("╔".repeat(40) + "\n");
			messageArea.setScrollTop(Double.MAX_VALUE);
		});
	}

	@Override
	public void printSeparator() {
		print("─".repeat(40));
	}

	@Override
	public void clearScreen() {
		Platform.runLater(() -> {
			messageArea.clear();
		});
	}

	@Override
	public String getInput() {
		return getInput("");
	}

	@Override
	public String getInput(String prompt) {
		while (true) {
			if (prompt != null && !prompt.isEmpty()) {
				print(prompt);
			}

			inputLatch = new CountDownLatch(1);
			inputResult.set("");

			Platform.runLater(() -> inputField.requestFocus());

			try {
				inputLatch.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return "";
			}

			String result = inputResult.get();

			// 開発者コマンドのチェック
			if (result != null && !result.isEmpty()) {
				String lowerInput = result.trim().toLowerCase();

				// 開発者コマンドの判定（admin, dev, admin on/off, debug on/off, player.*）
				if (lowerInput.equals("admin") ||
						lowerInput.equals("dev") ||
						lowerInput.equals("admin on") ||
						lowerInput.equals("admin off") ||
						lowerInput.equals("debug on") ||
						lowerInput.equals("debug off") ||
						lowerInput.startsWith("player.")) {

					if (developerMode != null) {
						Player currentPlayer = developerMode.getCurrentPlayer();
						developerMode.handleDevCommand(result, currentPlayer);
					}
					continue;
				}
			}

			return result;
		}
	}

	/**
	 * 内部実装：再帰的に入力を受け付ける
	 */

	@Override
	public int getPlayerChoice(int max) {
		return getPlayerChoice(max, null);
	}

	@Override
	public int getPlayerChoice(int max, Player currentPlayer) {
		return getPlayerChoiceInternal(max, currentPlayer, true);
	}

	@Override
	public int getPlayerChoiceWithDevCheck(int max, boolean checkDevMode) {
		return getPlayerChoice(max);
	}

	private int getPlayerChoiceInternal(int max, Player currentPlayer, boolean firstCall) {
		if (firstCall) {
			print("選択してください (1-" + max + "): ");
		}

		inputLatch = new CountDownLatch(1);
		AtomicInteger result = new AtomicInteger(-1);

		Platform.runLater(() -> inputField.requestFocus());

		try {
			inputLatch.await();
			String input = inputResult.get();

			// 開発者コマンドのチェック
			if (input != null && !input.isEmpty()) {
				String lowerInput = input.trim().toLowerCase();

				if (lowerInput.equals("admin") || lowerInput.equals("dev")) {
					if (developerMode != null) {
						developerMode.handleDevCommand(input, currentPlayer);
					}
					return getPlayerChoiceInternal(max, currentPlayer, false);
				}

				if (developerMode != null && developerMode.isEnabled()) {
					if (lowerInput.startsWith("player.") ||
							lowerInput.equals("admin on") ||
							lowerInput.equals("admin off") ||
							lowerInput.equals("debug on") ||
							lowerInput.equals("debug off")) {

						if (currentPlayer != null) {
							developerMode.handleDevCommand(input, currentPlayer);
						}
						return getPlayerChoiceInternal(max, currentPlayer, false);
					}
				}
			}

			if (input == null || input.isEmpty()) {
				print("⚠ 入力が空です。もう一度入力してください。");
				return getPlayerChoiceInternal(max, currentPlayer, false);
			}

			// ステータス確認コマンド（開発者モード不要）
			if (currentPlayer != null && !input.matches("\\d+")) {
				String lowerInput = input.trim().toLowerCase();
				if (lowerInput.equals("status") || lowerInput.equals("ステータス") || lowerInput.equals("s")) {
					print("━━━━━━━━━━━━━━━━━━━━━━━━");
					print(currentPlayer.getCharacterSheet());
					print("━━━━━━━━━━━━━━━━━━━━━━━━");
					print(""); // 空行を追加
					return getPlayerChoiceInternal(max, currentPlayer, false);
				}
			}

			try {
				int choice = Integer.parseInt(input);
				if (choice >= 1 && choice <= max) {
					result.set(choice);
				} else {
					print("⚠ 無効な選択です（範囲外: 1-" + max + "）");
					return getPlayerChoiceInternal(max, currentPlayer, false);
				}
			} catch (NumberFormatException e) {
				print("⚠ 無効な入力です（数値を入力してください）");
				return getPlayerChoiceInternal(max, currentPlayer, false);
			}

		} catch (InterruptedException e) {
			print("⚠ 入力待機中にエラーが発生しました");
			e.printStackTrace();
			return getPlayerChoiceInternal(max, currentPlayer, false);
		}

		return result.get();
	}

	@Override
	public boolean getYesNo() {

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Boolean> result = new AtomicReference<>(false);

		Platform.runLater(() -> {
			javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
					javafx.scene.control.Alert.AlertType.CONFIRMATION);
			alert.setTitle("確認");
			alert.setHeaderText(null);
			alert.setContentText("よろしいですか？");

			javafx.scene.control.ButtonType yesButton = new javafx.scene.control.ButtonType("はい",
					javafx.scene.control.ButtonBar.ButtonData.YES);
			javafx.scene.control.ButtonType noButton = new javafx.scene.control.ButtonType("いいえ",
					javafx.scene.control.ButtonBar.ButtonData.NO);
			alert.getButtonTypes().setAll(yesButton, noButton);

			alert.showAndWait().ifPresent(buttonType -> {
				result.set(buttonType == yesButton);
				latch.countDown();
			});
		});

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return result.get();
	}

	public void waitForEnter() {
		print("（Enterキーを押して続ける）");
		print("");
		getInput();
	}

	@Override
	public void printPlayerStatus(Player player) {
		Platform.runLater(() -> {
			// HP/AP/銀貨更新
			hpLabel.setText(String.format("HP: %s/%s",
					player.getCurrentHP(), player.getMaxHP()));
			apLabel.setText(String.format("AP: %s/%s",
					player.getCurrentAP(), player.getMaxAP()));

			moneyLabel.setText(String.format("銀貨: %s/%s",
					player.getMoney(), player.getMaxMoney()));

			// スキル表示（アイコン表示）
			List<String> skills = player.getEffectiveSkills();
			skillsPane.getChildren().clear();
			if (skills != null && !skills.isEmpty()) {
				for (String skillName : skills) {
					ImageView iconView = createSkillIconView(skillName);
					if (iconView != null) {
						skillsPane.getChildren().add(iconView);
					}
				}
			} else {
				// スキルがない場合は「なし」ラベルを表示
				Label noSkillLabel = new Label("（なし）");
				noSkillLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
				skillsPane.getChildren().add(noSkillLabel);
			}

			// アイテム表示（アイコン表示）
			List<String> itemIds = player.getInventory();
			itemsPane.getChildren().clear();
			if (itemIds != null && !itemIds.isEmpty()) {
				for (String itemId : itemIds) {
					ImageView iconView = createItemIconView(itemId);
					if (iconView != null) {
						itemsPane.getChildren().add(iconView);
					}
				}
			} else {
				// アイテムがない場合は「なし」ラベルを表示
				Label noItemLabel = new Label("（なし）");
				noItemLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
				itemsPane.getChildren().add(noItemLabel);
			}

			// 状態異常表示（アイコン表示）
			java.util.Map<String, Integer> statusEffects = player.getStatusEffects();
			statusEffectsPane.getChildren().clear();
			if (statusEffects != null && !statusEffects.isEmpty()) {
				for (java.util.Map.Entry<String, Integer> entry : statusEffects.entrySet()) {
					String effectId = entry.getKey();
					int value = entry.getValue();
					ImageView iconView = createStatusEffectIconView(effectId, value);
					if (iconView != null) {
						statusEffectsPane.getChildren().add(iconView);
					}
				}
			} else {
				// 状態異常がない場合は「なし」ラベルを表示
				Label noStatusLabel = new Label("（状態異常：なし）");
				noStatusLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
				statusEffectsPane.getChildren().add(noStatusLabel);
			}

			// 立ち絵の更新
			updatePortrait(player);
		});
	}

	/**
	 * 技能アイコンを作成する
	 * 
	 * @param skillName 技能名
	 * @return アイコンImageView
	 */
	private ImageView createSkillIconView(String skillName) {
		Image icon = com.kh.tbrr.manager.IconHelper.loadSkillIcon(skillName);
		if (icon == null) {
			return null;
		}

		ImageView iconView = new ImageView(icon);
		iconView.setFitWidth(32);
		iconView.setFitHeight(32);
		iconView.setPreserveRatio(true);

		// ツールチップで技能の詳細説明を表示
		String description = com.kh.tbrr.manager.IconHelper.getSkillDescription(skillName);
		Tooltip tooltip = new Tooltip(description);
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(300);
		tooltip.setShowDelay(Duration.millis(200)); // 表示までの遅延を200msに設定
		tooltip.setFont(Font.font(14)); // フォントサイズを14pxに設定
		Tooltip.install(iconView, tooltip);

		return iconView;
	}

	/**
	 * アイテムアイコンを作成する
	 * 
	 * @param itemId アイテムID
	 * @return アイコンImageView
	 */
	private ImageView createItemIconView(String itemId) {
		Image icon = com.kh.tbrr.manager.IconHelper.loadItemIcon(itemId);
		if (icon == null) {
			return null;
		}

		ImageView iconView = new ImageView(icon);
		iconView.setFitWidth(32);
		iconView.setFitHeight(32);
		iconView.setPreserveRatio(true);

		// ツールチップでアイテムの詳細説明を表示
		String description = com.kh.tbrr.manager.IconHelper.getItemDescription(itemId);
		Tooltip tooltip = new Tooltip(description);
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(300);
		tooltip.setShowDelay(Duration.millis(200)); // 表示までの遅延を200msに設定
		tooltip.setFont(Font.font(14)); // フォントサイズを14pxに設定
		Tooltip.install(iconView, tooltip);

		return iconView;
	}

	/**
	 * 状態異常アイコンを作成する
	 * 
	 * @param effectId 状態異常ID
	 * @param value    状態異常の値
	 * @return アイコンImageView
	 */
	private ImageView createStatusEffectIconView(String effectId, int value) {
		Image icon = com.kh.tbrr.manager.IconHelper.loadStatusEffectIcon(effectId);
		if (icon == null) {
			return null;
		}

		ImageView iconView = new ImageView(icon);
		iconView.setFitWidth(32);
		iconView.setFitHeight(32);
		iconView.setPreserveRatio(true);

		// ツールチップで状態異常の詳細説明と値を表示
		String description = com.kh.tbrr.manager.IconHelper.getStatusEffectDescription(effectId);
		String tooltipText = description + "\n現在値: " + value;
		Tooltip tooltip = new Tooltip(tooltipText);
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(300);
		tooltip.setShowDelay(Duration.millis(200)); // 表示までの遅延を200msに設定
		tooltip.setFont(Font.font(14)); // フォントサイズを14pxに設定
		Tooltip.install(iconView, tooltip);

		return iconView;
	}

	/**
	 * 立ち絵を更新する
	 */
	private void updatePortrait(Player player) {
		this.currentPlayer = player;

		if (player.getPortraitFileName() != null && !player.getPortraitFileName().isEmpty()) {
			javafx.scene.image.Image portrait = loadPortraitWithCustomSupport(player.getPortraitFileName());
			if (portrait != null) {
				characterPortraitView.setImage(portrait);
			}
		}
	}

	/**
	 * 立ち絵の表情を変更する
	 * 
	 * @param expression 表情名（joy, angry, sad, など）
	 */
	public void changePortraitExpression(String expression) {
		if (currentPlayer != null && currentPlayer.getPortraitFileName() != null) {
			String fileName = currentPlayer.getPortraitFileName();
			javafx.scene.image.Image portrait;

			if (com.kh.tbrr.manager.ImageManager.isCustomPortrait(fileName)) {
				// カスタム立ち絵の表情差分
				String actualFileName = com.kh.tbrr.manager.ImageManager.stripCustomPrefix(fileName);
				portrait = imageManager.loadCustomPortraitExpression(actualFileName, expression);
			} else {
				// 内蔵立ち絵の表情差分
				portrait = imageManager.loadPortraitExpression(fileName, expression);
			}

			if (portrait != null) {
				characterPortraitView.setImage(portrait);
			}
		}
	}

	/**
	 * 立ち絵を基本表情に戻す
	 */
	public void resetPortraitExpression() {
		if (currentPlayer != null && currentPlayer.getPortraitFileName() != null) {
			javafx.scene.image.Image portrait = loadPortraitWithCustomSupport(currentPlayer.getPortraitFileName());
			if (portrait != null) {
				characterPortraitView.setImage(portrait);
			}
		}
	}

	/**
	 * カスタム立ち絵に対応した立ち絵読み込み
	 * custom: プレフィックスがある場合は外部ファイルから読み込む
	 */
	private javafx.scene.image.Image loadPortraitWithCustomSupport(String fileName) {
		if (com.kh.tbrr.manager.ImageManager.isCustomPortrait(fileName)) {
			// カスタム立ち絵
			String actualFileName = com.kh.tbrr.manager.ImageManager.stripCustomPrefix(fileName);
			return imageManager.loadCustomPortrait(actualFileName);
		} else {
			// 内蔵立ち絵
			return imageManager.loadPortrait(fileName);
		}
	}

	@Override
	public int showChoices(List<String> choices) {
		for (int i = 0; i < choices.size(); i++) {
			print((i + 1) + ". " + choices.get(i));
		}
		return getPlayerChoice(choices.size());
	}

	@Override
	public void showFloorInfo(int floorNumber, String terrainName) {
		Platform.runLater(() -> {
			if (floorNumber <= 0) {
				mapInfoLabel.setText(String.format("プロローグ | 現在の地形：%s", terrainName));
			} else {
				mapInfoLabel.setText(String.format("フロア%d | 現在の地形：%s", floorNumber, terrainName));
			}
		});
	}

	@Override
	public void showEventInfo(String eventTitle) {
		Platform.runLater(() -> {
			if (eventTitle != null && !eventTitle.isEmpty()) {
				eventInfoLabel.setText(eventTitle);
			} else {
				eventInfoLabel.setText("");
			}
		});
	}

	@Override
	public void showImage(String imageType, String imagePath) {
		if (developerMode != null && developerMode.isDebugVisible()) {
			print("[画像: " + imageType + " - " + imagePath + "]");
		}

		// 実際に画像を表示
		Platform.runLater(() -> {
			if ("background".equals(imageType)) {
				updateBackgroundImage(imagePath);
			} else if ("sub".equals(imageType)) {
				updateSubImage(imagePath);
			}
		});
	}

	/**
	 * 背景画像を更新
	 * 
	 * @param fileName 画像ファイル名
	 */
	public void updateBackgroundImage(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			eventImageView.setImage(null);
			return;
		}

		Platform.runLater(() -> {
			Image bgImage = imageManager.loadBackground(fileName);
			if (bgImage != null) {
				eventImageView.setImage(bgImage);
			} else {
				System.err.println("[JavaFXUI] 背景画像の読み込みに失敗: " + fileName);
			}
		});
	}

	/**
	 * サブウィンドウ画像を更新
	 * 
	 * @param fileName 画像ファイル名
	 */
	public void updateSubImage(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			if (subWindowImageView != null) {
				subWindowImageView.setImage(null);
			}
			return;
		}

		Platform.runLater(() -> {
			if (subWindowImageView != null) {
				Image subImage = imageManager.loadSubImage(fileName);
				if (subImage != null) {
					subWindowImageView.setImage(subImage);
				} else {
					System.err.println("[JavaFXUI] サブ画像の読み込みに失敗: " + fileName);
				}
			}
		});
	}

	@Override
	public void printImportantLog(String message) {
		Platform.runLater(() -> {
			importantLogArea.appendText(message + "\n");
			importantLogArea.setScrollTop(Double.MAX_VALUE);
		});
	}

	/**
	 * メインメニューに戻るコールバックを設定
	 * 
	 * @param callback メインメニューに戻る際に実行されるRunnable
	 */
	private Runnable returnToMainMenuCallback;
	// 中断処理用コールバック
	private Runnable onSuspendGameCallback;

	public void setReturnToMainMenuCallback(Runnable callback) {
		this.returnToMainMenuCallback = callback;
	}

	public void setOnSuspendGameCallback(Runnable callback) {
		this.onSuspendGameCallback = callback;
	}

	/**
	 * コンフィグダイアログを表示
	 */
	private void showConfigDialog() {
		Platform.runLater(() -> {
			ConfigDialog dialog = new ConfigDialog(stage, returnToMainMenuCallback);
			// 中断コールバックを設定（プレイヤーが存在する場合のみ＝ゲーム開始後のみ）
			if (currentPlayer != null) {
				dialog.setOnSuspendGame(onSuspendGameCallback);
			} else {
				dialog.setOnSuspendGame(null);
			}
			dialog.show();
		});
	}

	/**
	 * ステータスダイアログを表示
	 */
	private void showStatusDialog() {
		Platform.runLater(() -> {
			if (currentPlayer == null) {
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setTitle("警告");
				alert.setHeaderText(null);
				alert.setContentText("プレイヤー情報が読み込まれていません。");
				alert.showAndWait();
				return;
			}

			Alert statusDialog = new Alert(Alert.AlertType.INFORMATION);
			statusDialog.setTitle("ステータス");
			statusDialog.setHeaderText("現在のキャラクター情報");
			statusDialog.setResizable(true);

			// キャラクターシートを取得
			String characterSheet = currentPlayer.getCharacterSheet();

			// TextAreaで表示（スクロール可能）
			TextArea textArea = new TextArea(characterSheet);
			textArea.setEditable(false);
			textArea.setWrapText(true);
			textArea.setFont(Font.font("MS Gothic", 12));
			textArea.setPrefWidth(500);
			textArea.setPrefHeight(400);

			statusDialog.getDialogPane().setContent(textArea);
			statusDialog.getDialogPane().setPrefWidth(550);
			statusDialog.getDialogPane().setPrefHeight(500);

			statusDialog.showAndWait();
		});
	}

	@Override
	public void close() {
		// 入力待機中のスレッドを解放
		if (inputLatch != null) {
			inputLatch.countDown();
		}

		// ステージを閉じる
		Platform.runLater(() -> {
			stage.close();
		});
	}

	// ========== インタラクション（ミニゲーム）用メソッド ==========

	@Override
	public Object getSubWindowPane() {
		// subWindowBoxをStackPaneでラップして返す
		// ミニゲーム表示用にサブウィンドウの子要素を操作可能にする
		return subWindowBox;
	}

	@Override
	public void setInteractionInputHandler(java.util.function.Consumer<String> handler) {
		this.interactionInputHandler = handler;
	}

	/**
	 * サブウィンドウをミニゲーム用のStackPaneに変換
	 * 
	 * @return サブウィンドウ用のStackPane
	 */
	public javafx.scene.layout.StackPane getSubWindowAsStackPane() {
		if (subWindowBox == null)
			return null;

		// 既にStackPaneがあればそれを返す
		for (javafx.scene.Node child : subWindowBox.getChildren()) {
			if (child instanceof javafx.scene.layout.StackPane) {
				return (javafx.scene.layout.StackPane) child;
			}
		}

		// なければ作成して追加
		javafx.scene.layout.StackPane interactionPane = new javafx.scene.layout.StackPane();
		interactionPane.setPrefSize(450, 450);
		interactionPane.setMaxSize(450, 450);
		interactionPane.setMinSize(450, 450);
		subWindowBox.getChildren().clear();
		subWindowBox.getChildren().add(interactionPane);
		return interactionPane;
	}

	/**
	 * サブウィンドウを通常の画像表示モードに戻す
	 */
	public void restoreSubWindowToNormal() {
		Platform.runLater(() -> {
			if (subWindowBox != null) {
				subWindowBox.getChildren().clear();
				subWindowBox.getChildren().add(subWindowImageView);
			}
		});
	}
}