package com.kh.tbrr.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.kh.tbrr.core.GameEngine;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Scenario;
import com.kh.tbrr.manager.DataManager;
import com.kh.tbrr.system.CharacterLoader;
import com.kh.tbrr.system.DeveloperMode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * メインメニュー画面
 * キャラ作成、キャラ選択、図鑑、設定などを選べる
 */
public class MainMenuScreen {

	private Stage stage;
	private JavaFXUI gameUI;
	private com.kh.tbrr.manager.ImageManager imageManager; // 画像管理用

	public MainMenuScreen(Stage stage) {
		this.stage = stage;
		this.imageManager = new com.kh.tbrr.manager.ImageManager();
	}

	/**
	 * メインメニューを表示
	 */
	public void show() {
		// 背景画像を読み込み
		Image backgroundImage = imageManager.loadBackground("mainmenu.png");
		ImageView backgroundView = new ImageView(backgroundImage);
		backgroundView.setFitWidth(1600);
		backgroundView.setFitHeight(900);
		backgroundView.setPreserveRatio(false);

		// ボタンを配置するVBox
		VBox buttonBox = new VBox(20);
		buttonBox.setPadding(new Insets(50));
		buttonBox.setAlignment(Pos.BOTTOM_RIGHT);

		// メニューボタン
		Button selectCharButton = createMenuButton("キャラクターの選択");
		Button createCharButton = createMenuButton("キャラクターの作成");
		Button manualButton = createMenuButton("マニュアル");
		Button achievementsButton = createMenuButton("実績");
		Button settingsButton = createMenuButton("設定");
		Button exitButton = createMenuButton("ゲームを終了する");

		// ボタンのイベント設定
		selectCharButton.setOnAction(e -> onSelectCharacter());
		createCharButton.setOnAction(e -> onCreateCharacter());
		manualButton.setOnAction(e -> onManual());
		achievementsButton.setOnAction(e -> onAchievements());
		settingsButton.setOnAction(e -> onSettings());
		exitButton.setOnAction(e -> onExitGame());

		// ボタンをVBoxに追加
		buttonBox.getChildren().addAll(
				selectCharButton,
				createCharButton,
				manualButton,
				achievementsButton,
				settingsButton,
				exitButton);

		// StackPaneで背景画像とボタンを重ねる
		StackPane root = new StackPane();
		root.getChildren().addAll(backgroundView, buttonBox);

		Scene scene = new Scene(root, 1600, 900);
		stage.setScene(scene);
		stage.setTitle("TBRR - Main Menu");
		stage.setResizable(false); // ウィンドウサイズを固定
		stage.show();
	}

	/**
	 * メニューボタンを作成
	 */
	private Button createMenuButton(String text) {
		Button button = new Button(text);
		button.setPrefWidth(350);
		button.setPrefHeight(55);
		button.setFont(Font.font("Arial", 18));
		button.setStyle(
				"-fx-background-color: #444444; " +
						"-fx-text-fill: white; " +
						"-fx-border-color: #666666; " +
						"-fx-border-width: 2px;");

		// ホバー効果
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

	/**
	 * キャラクターの作成
	 */
	private void onCreateCharacter() {
		try {
			// PersonalityManagerを初期化（引数なし）
			com.kh.tbrr.manager.PersonalityManager personalityManager = new com.kh.tbrr.manager.PersonalityManager();

			// キャラクター制作画面を表示
			CharacterCreationScreen creationScreen = new CharacterCreationScreen(stage, personalityManager);
			creationScreen.show(() -> {
				// 作成完了後、メインメニューに戻る
				show();
			});

		} catch (Exception e) {
			showAlert("エラー", "キャラクター作成画面の起動に失敗しました: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * キャラクターの選択 → ゲーム開始
	 */
	private void onSelectCharacter() {
		showCharacterSelection();
	}

	/**
	 * マニュアル
	 */
	private void onManual() {
		ManualScreen manualScreen = new ManualScreen(stage);
		manualScreen.show(() -> show());
	}

	/**
	 * 実績
	 */
	private void onAchievements() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("実績");
		alert.setHeaderText("実績");
		alert.setContentText("実績機能は未実装です。\n今後のアップデートで追加予定です。");
		alert.showAndWait();
	}

	/**
	 * 設定
	 */
	private void onSettings() {
		ConfigDialog configDialog = new ConfigDialog(stage, null);
		configDialog.show();
	}

	/**
	 * ゲームを終了する
	 */
	private void onExitGame() {
		Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
		confirmDialog.setTitle("ゲーム終了");
		confirmDialog.setHeaderText("ゲームを終了しますか？");
		confirmDialog.setContentText("アプリケーションを終了します。");

		Optional<ButtonType> result = confirmDialog.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			Platform.exit();
			System.exit(0);
		}
	}

	/**
	 * キャラクター選択画面を表示
	 */
	private void showCharacterSelection() {
		// 保存されたキャラクターファイルを取得
		File saveDir = new File("userdata/character");
		if (!saveDir.exists() || !saveDir.isDirectory()) {
			showAlert("エラー", "キャラクターフォルダが見つかりません。\n\n「キャラクターの作成」からキャラクターを作成してください。\n（フォルダは自動的に作成されます）");
			return;
		}

		File[] files = saveDir.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null || files.length == 0) {
			showAlert("エラー", "保存されたキャラクターが見つかりません。\n\nまずはキャラクターを作成してください。");
			return;
		}

		// キャラクターリストを表示
		List<String> characterNames = new ArrayList<>();
		for (File file : files) {
			characterNames.add(file.getName().replace(".json", ""));
		}

		ChoiceDialog<String> dialog = new ChoiceDialog<>(characterNames.get(0), characterNames);
		dialog.setTitle("キャラクター選択");
		dialog.setHeaderText("使用するキャラクターを選択してください");
		dialog.setContentText("キャラクター:");

		dialog.showAndWait().ifPresent(selectedName -> {
			startGameWithCharacter(selectedName);
		});
	}

	/**
	 * 選択したキャラクターでゲームを開始
	 */
	private void startGameWithCharacter(String characterName) {
		try {
			// キャラクターをロード（拡張子を追加）
			CharacterLoader loader = new CharacterLoader();
			String filename = characterName.endsWith(".json") ? characterName : characterName + ".json";
			Player player = loader.loadCharacter(filename);

			if (player == null) {
				showAlert("エラー", "キャラクター情報の読み込みに失敗しました。");
				return;
			}

			// ゲーム画面を初期化してシナリオ選択へ
			initializeGameScreen(player);

		} catch (Exception e) {
			showAlert("エラー", "ゲームの起動に失敗しました: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * ゲーム画面を初期化（選択したプレイヤーで）
	 */
	private void initializeGameScreen(Player player) {
		// 別スレッドでゲームを実行（デーモンスレッドに設定）
		Thread gameThread = new Thread(() -> {
			System.err.println("[DEBUG] Game thread started");
			try {
				// 正しい順序でマネージャー初期化
				// 1. DeveloperModeを作成 (引数なし)
				DeveloperMode developerMode = new DeveloperMode();

				// 2. JavaFXUIを作成
				gameUI = new JavaFXUI(stage, developerMode);

				// メインメニューに戻るコールバックを設定
				gameUI.setReturnToMainMenuCallback(() -> {
					Platform.runLater(() -> {
						show();
					});
				});

				// 3. DeveloperModeにUIを設定
				developerMode.setUI(gameUI);

				// 4. 画面を表示
				Platform.runLater(() -> gameUI.initializeGameScreen());

				// 少し待機(画面が表示されるまで)
				Thread.sleep(500);

				// 5. DataManagerを作成 (DeveloperModeが必要)
				DataManager dataManager = new DataManager(developerMode);

				// 6. GameEngineを作成 (DeveloperMode, UI, DataManager)
				// GameEngineのコンストラクタ内で他のマネージャーが初期化される
				GameEngine engine = new GameEngine(
						developerMode,
						gameUI,
						dataManager);

				// 7. プレイヤー情報を表示
				gameUI.printPlayerStatus(player);

				// 7.5. シナリオ選択前の画像を表示（冒険者ギルド、自宅、酒場など）
				Platform.runLater(() -> {
					gameUI.updateBackgroundImage("mainmenu_default.jpg");
					// ★追加: サブウィンドウにもデフォルト画像を表示
					gameUI.updateSubImage("mainmenu_sub_default.jpg");
				});
				Thread.sleep(200); // 画像表示を待つ

				// 8. ゲーム画面でシナリオ選択を促す
				// 8. ゲーム画面でシナリオ選択を促す
				gameUI.print("=".repeat(60));
				gameUI.print("");
				gameUI.print("　なんちゃらプレゼンツ");
				gameUI.print("　ソフト名とかバージョン");
				gameUI.print("");
				gameUI.print("  ようこそ、 " + player.getName() + "！");
				gameUI.print("");
				gameUI.print("=".repeat(60));

				gameUI.print("");
				// ScenarioManagerを作成してシナリオリストを取得
				com.kh.tbrr.manager.ScenarioManager scenarioManager = new com.kh.tbrr.manager.ScenarioManager(
						dataManager, developerMode);

				List<String> scenarioIds = scenarioManager.getAllScenarioIds();

				if (scenarioIds.isEmpty()) {
					gameUI.printError("シナリオが見つかりません。");
					return;
				}

				// シナリオ選択ループ（Noを選んだ場合は再選択）
				boolean scenarioConfirmed = false;
				String selectedScenarioId = null;

				while (!scenarioConfirmed) {
					gameUI.print("プレイするシナリオを選択してください：");
					gameUI.print("");

					// シナリオリストを表示（nameを表示）
					for (int i = 0; i < scenarioIds.size(); i++) {
						String scenarioId = scenarioIds.get(i);
						Scenario scenario = scenarioManager.getScenario(scenarioId);
						if (scenario != null && scenario.getName() != null) {
							gameUI.print((i + 1) + ". " + scenario.getName());
						} else {
							gameUI.print((i + 1) + ". " + scenarioId); // nameがない場合はIDを表示
						}
					}
					gameUI.print("");

					// シナリオ選択
					int choice = gameUI.getPlayerChoice(scenarioIds.size());

					if (choice < 1 || choice > scenarioIds.size()) {
						gameUI.printError("無効な選択です。");
						continue; // 再選択
					}

					selectedScenarioId = scenarioIds.get(choice - 1);
					Scenario selectedScenario = scenarioManager.getScenario(selectedScenarioId);

					// ★改善: シナリオ概要画像を表示（背景とサブ両方対応）
					if (selectedScenario != null) {
						String thumbnailImage = selectedScenario.getThumbnailImage();
						if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
							// thumbnailImageが設定されている場合は背景画像として表示
							Platform.runLater(() -> {
								gameUI.updateBackgroundImage(thumbnailImage);
							});
							Thread.sleep(200); // 画像表示を待つ
						}

						// ★追加: サブウィンドウ画像も変更可能に
						String subImage = selectedScenario.getSubImage();
						if (subImage != null && !subImage.isEmpty()) {
							Platform.runLater(() -> {
								gameUI.updateSubImage(subImage);
							});
							Thread.sleep(100);
						}
					}

					// シナリオの確認（descriptionを表示）
					gameUI.print("");
					gameUI.print("=".repeat(60));
					if (selectedScenario != null && selectedScenario.getName() != null) {
						gameUI.print("【" + selectedScenario.getName() + "】");
					} else {
						gameUI.print("【" + selectedScenarioId + "】");
					}
					gameUI.print("");

					if (selectedScenario != null && selectedScenario.getDescription() != null) {
						gameUI.print(selectedScenario.getDescription());
					} else {
						gameUI.print("（説明なし）");
					}
					gameUI.print("");
					gameUI.print("=".repeat(60));
					gameUI.print("");
					gameUI.print("このシナリオでスタートしますか？");
					gameUI.print("");
					gameUI.print("1. はい");
					gameUI.print("2. いいえ（別のシナリオを選ぶ）");
					gameUI.print("");

					int confirmChoice = gameUI.getPlayerChoice(2);

					if (confirmChoice == 1) {
						scenarioConfirmed = true; // 確認完了
					} else {
						// Noの場合は再選択
						gameUI.print("");
						gameUI.print("シナリオ選択に戻ります。");
						gameUI.print("");

						// ★追加: シナリオを選ばなかった場合はデフォルト画像に戻す
						Platform.runLater(() -> {
							gameUI.updateBackgroundImage("mainmenu_default.jpg");
							gameUI.updateSubImage("mainmenu_sub_default.jpg");
						});
						Thread.sleep(200);
					}
				}

				gameUI.print("");
				if (scenarioManager.getScenario(selectedScenarioId) != null
						&& scenarioManager.getScenario(selectedScenarioId).getName() != null) {
					gameUI.print("「" + scenarioManager.getScenario(selectedScenarioId).getName() + "」を開始します...");
				} else {
					gameUI.print("「" + selectedScenarioId + "」を開始します...");
				}
				gameUI.print("");

				// 9. ゲーム開始（プレイヤーは既に選択済み）
				engine.startNewGameWithPlayer(selectedScenarioId, player);

				// ★★★ ゲーム終了後、メインメニューに戻る ★★★
				gameUI.print("");
				gameUI.print("=".repeat(60));
				gameUI.print("ゲームが終了しました。");
				gameUI.print("=".repeat(60));
				gameUI.print("");
				gameUI.waitForEnter();

				// JavaFXスレッドでメインメニューに戻る
				Platform.runLater(() -> {
					show();
				});

			} catch (Exception e) {
				Platform.runLater(() -> {
					showAlert("エラー", "ゲームの実行中にエラーが発生しました: " + e.getMessage());
				});
				e.printStackTrace();
			}
		});
		gameThread.setDaemon(true); // デーモンスレッドに設定（メインスレッド終了時に自動終了）
		gameThread.start();
	}

	/**
	 * アラートを表示
	 */
	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}