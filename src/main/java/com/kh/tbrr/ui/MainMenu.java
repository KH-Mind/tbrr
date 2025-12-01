package com.kh.tbrr.ui;

import java.util.List;

import com.kh.tbrr.core.GameEngine;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.manager.DataManager;
import com.kh.tbrr.manager.PersonalityManager;
import com.kh.tbrr.system.CharacterCreator;
import com.kh.tbrr.system.CharacterLoader;
import com.kh.tbrr.system.DeveloperMode;

/**
 * メインメニュー
 * 
 */
public class MainMenu {
	private ConsoleUI ui;
	private DataManager dataManager;
	private GameEngine gameEngine;
	private DeveloperMode developerMode;
	

	public MainMenu() {
		// DeveloperMode を先に作り、UI は後からセットする（相互参照回避）
		this.developerMode = new DeveloperMode(null);
		this.ui = new ConsoleUI(developerMode);
		this.developerMode.setUI(ui);
		this.dataManager = new DataManager(developerMode);
		this.gameEngine = new GameEngine(developerMode, ui, dataManager);
		
		
	}

	private void showGraveyardMenu() {
		ExtraMenuUI extraMenu = new ExtraMenuUI(gameEngine.getGameState(), ui);
		extraMenu.showGraveyardMenu();
	}

	public void show() {
		boolean running = true;

		while (running) {
			showTitle();
			ui.print("【メインメニュー】");
			ui.print("1. キャラクター選択");  
			ui.print("2. キャラクター作成");  
			ui.print("3. 図鑑");
			ui.print("4. 実績");
			ui.print("5. 墓地メニュー");
			ui.print("6. 設定");
			ui.print("7. 終了");
			ui.print("");

			int choice = ui.getPlayerChoice(7);

			// 開発者モード有効時は選択肢を表示しない仕様のため -999 を受け取ったらスキップ
			if (choice == -999) {
				// 何もしないでメニューを再表示（要求どおり「選択肢を表示しない」動作）
				continue;
			}

			switch (choice) {
			case 1:
				startNewGame();  // ← キャラ選択 → シナリオ選択
				break;
			case 2:
				characterManagementMenu();  // ← キャラ編集メニュー
				break;
			case 3:
				showDeathEncyclopedia();
				break;
			case 4:
				showAchievements();
				break;
			case 5:
				showGraveyardMenu();
				break;
			case 6:
				showSettings();
				break;
			case 7:
				running = false;
				break;
			}
		}

		gameEngine.shutdown();
	}

	private void showTitle() {
		ui.clearScreen();
		ui.print("");
		ui.print("╔═══════════════════════════════════════╗");
		ui.print("║                                       ║");
		ui.print("║     Text-Based Roguelite RPG          ║");
		ui.print("║            ( TBRR )                   ║");
		ui.print("║                                       ║");
		ui.print("╚═══════════════════════════════════════╝");
		ui.print("");
	}

	/**
	 * 新規ゲーム: キャラ選択 → シナリオ選択 → ゲーム開始
	 */
	private void startNewGame() {
		ui.printTitleBar("新規ゲーム");
		
		// 1. キャラクター選択
		CharacterLoader loader = new CharacterLoader();
		List<String> savedCharacters = loader.listSavedCharacters();
		
		if (savedCharacters.isEmpty()) {
			ui.print("保存されたキャラクターがありません。");
			ui.print("先に「キャラクター作成」でキャラクターを作成してください。");
			ui.waitForEnter();
			return;
		}
		
		ui.print("【キャラクター選択】");
		for (int i = 0; i < savedCharacters.size(); i++) {
			String filename = savedCharacters.get(i);
			String displayName = filename.replace(".json", "");
			ui.print((i + 1) + ". " + displayName);
		}
		ui.print((savedCharacters.size() + 1) + ". 戻る");
		ui.print("");
		
		int charChoice = ui.getPlayerChoice(savedCharacters.size() + 1);
		
		if (charChoice == -999 || charChoice > savedCharacters.size()) {
			return;
		}
		
		String selectedCharFile = savedCharacters.get(charChoice - 1);
		Player player = loader.loadCharacter(selectedCharFile);
		
		if (player == null) {
			ui.printError("キャラクター情報の読み込みに失敗しました。");
			ui.waitForEnter();
			return;
		}
		
		ui.print("");
		ui.print("✅ " + player.getName() + " を選択しました。");
		ui.print("");
		
		// 2. シナリオ選択
		selectScenarioForCharacter(player);
	}

	/**
	 * 選択したキャラクターでシナリオを選ぶ
	 */
	private void selectScenarioForCharacter(Player player) {
		ui.printTitleBar("シナリオ選択");
		
		List<String> scenarioIds = dataManager.getAllScenarioIds();
		
		if (scenarioIds.isEmpty()) {
			ui.print("利用可能なシナリオがありません。");
			ui.waitForEnter();
			return;
		}
		
		ui.print("プレイするシナリオを選択してください:");
		ui.print("");
		
		for (int i = 0; i < scenarioIds.size(); i++) {
			ui.print((i + 1) + ". " + scenarioIds.get(i));
		}
		ui.print((scenarioIds.size() + 1) + ". 戻る");
		ui.print("");
		
		int choice = ui.getPlayerChoice(scenarioIds.size() + 1);
		
		if (choice == -999 || choice > scenarioIds.size()) {
			return;
		}
		
		String selectedScenario = scenarioIds.get(choice - 1);
		ui.print("");
		ui.print("「" + selectedScenario + "」を開始します...");
		ui.print("キャラクター: " + player.getName());
		ui.waitForEnter();
		
		// ✅ 修正: startNewGameWithPlayer を使用
		gameEngine.startNewGameWithPlayer(selectedScenario, player);
	}

	/**
	 * キャラクター編集メニュー
	 */
	private void characterManagementMenu() {
		boolean inMenu = true;
		
		while (inMenu) {
			ui.printTitleBar("キャラクター編集");
			ui.print("【キャラクター編集メニュー】");
			ui.print("1. 新規作成");
			ui.print("2. 既存キャラ確認");
			ui.print("3. 既存キャラ削除");
			ui.print("4. 戻る");
			ui.print("");
			
			int choice = ui.getPlayerChoice(4);
			
			if (choice == -999) {
				return;
			}
			
			switch (choice) {
			case 1:
				createNewCharacter();
				break;
			case 2:
				viewExistingCharacter();
				break;
			case 3:
				deleteCharacter();
				break;
			case 4:
				inMenu = false;
				break;
			}
		}
	}

	/**
	 * 新規キャラクター作成
	 */
	private void createNewCharacter() {
		ui.printTitleBar("キャラクター作成");

		CharacterCreator creator = new CharacterCreator(ui, new PersonalityManager());
		Player player = creator.createCharacter();

		ui.print("このキャラクターを保存しますか？ (y/n): ");
		String confirm = ui.getInput().trim().toLowerCase();
		developerMode.handleDevCommand(confirm, null);
		if (confirm.equals("y")) {
			CharacterLoader loader = new CharacterLoader();
			boolean ok = loader.saveCharacter(player);
			if (ok) {
				ui.print("✅ キャラクター保存完了: " + player.getEnglishName() + ".json");
			} else {
				ui.print("⚠ 保存に失敗しました。");
			}
		} else {
			ui.print("保存せずに終了します。");
		}

		ui.waitForEnter();
	}

	/**
	 * 既存キャラクター確認
	 */
	private void viewExistingCharacter() {
		ui.printTitleBar("既存キャラ確認");
		
		CharacterLoader loader = new CharacterLoader();
		List<String> savedCharacters = loader.listSavedCharacters();
		
		if (savedCharacters.isEmpty()) {
			ui.print("保存されたキャラクターがありません。");
			ui.waitForEnter();
			return;
		}
		
		ui.print("【保存されているキャラクター】");
		for (int i = 0; i < savedCharacters.size(); i++) {
			String filename = savedCharacters.get(i);
			String displayName = filename.replace(".json", "");
			ui.print((i + 1) + ". " + displayName);
		}
		ui.print((savedCharacters.size() + 1) + ". 戻る");
		ui.print("");
		
		int choice = ui.getPlayerChoice(savedCharacters.size() + 1);
		
		if (choice == -999 || choice > savedCharacters.size()) {
			return;
		}
		
		String selectedFile = savedCharacters.get(choice - 1);
		Player player = loader.loadCharacter(selectedFile);
		
		if (player != null) {
			ui.print("");
			ui.print(player.getCharacterSheet());
		} else {
			ui.printError("キャラクター情報の読み込みに失敗しました。");
		}
		
		ui.waitForEnter();
	}

	/**
	 * キャラクター削除
	 */
	private void deleteCharacter() {
		ui.printTitleBar("既存キャラ削除");
		ui.print("この機能は未実装です。");
		ui.print("手動で userdata/character/ フォルダ内の .json ファイルを削除してください。");
		ui.waitForEnter();
	}

	private void showDeathEncyclopedia() {
		ui.printTitleBar("図鑑");
		ui.print("この機能は実装予定です。");
		ui.waitForEnter();
	}

	private void showAchievements() {
		ui.printTitleBar("実績");
		ui.print("この機能は実装予定です。");
		ui.waitForEnter();
	}

	private void showSettings() {
		ui.printTitleBar("設定");

		ui.print("【設定メニュー】");
		ui.print("1. テキスト速度");
		ui.print("2. 難易度");
		ui.print("3. データ削除");
		ui.print("4. 戻る");
		ui.print("");

		int choice = ui.getPlayerChoice(4);

		if (choice == -999) {
			return;
		}

		switch (choice) {
		case 1:
			ui.print("テキスト速度設定は未実装です。");
			ui.waitForEnter();
			break;
		case 2:
			ui.print("難易度設定は未実装です。");
			ui.waitForEnter();
			break;
		case 3:
			confirmDataDeletion();
			break;
		case 4:
			break;
		}
	}

	private void confirmDataDeletion() {
		ui.print("");
		ui.printWarning("全てのセーブデータを削除しますか？");
		ui.print("この操作は取り消せません！");
		ui.print("");

		if (ui.getYesNo()) {
			ui.print("セーブデータを削除しました。");
			ui.print("（実際の削除処理は未実装）");
		} else {
			ui.print("キャンセルしました。");
		}

		ui.waitForEnter();
	}
}