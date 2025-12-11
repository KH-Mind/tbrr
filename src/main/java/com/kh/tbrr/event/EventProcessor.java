package com.kh.tbrr.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.kh.tbrr.core.GameState;
import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.data.models.GameEvent;
import com.kh.tbrr.data.models.GameEvent.Choice;
import com.kh.tbrr.data.models.GameEvent.InitialEffects;
import com.kh.tbrr.data.models.GameEvent.Result;
import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.interaction.InteractionHandler;
import com.kh.tbrr.interaction.InteractionRegistry;
import com.kh.tbrr.interaction.InteractionResult;
import com.kh.tbrr.manager.DataManager;
import com.kh.tbrr.manager.DeathManager;
import com.kh.tbrr.system.DeveloperMode;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.utils.TextReplacer;

public class EventProcessor {
	private GameUI ui;
	private DataManager dataManager;
	private DeathManager deathManager;
	private Random random;
	private com.kh.tbrr.manager.AudioManager audioManager;

	/**
	 * 重要ログにフロア区切りを出力（必要な場合）
	 * 現在のフロアが最後にログ出力したフロアと異なる場合、区切りを追加
	 */
	private void printFloorDividerIfNeeded(GameState gameState) {
		int currentFloor = gameState.getCurrentFloor();
		if (currentFloor != gameState.getLastLoggedFloor()) {
			ui.printImportantLog("--- フロア" + currentFloor + " ---");
			gameState.setLastLoggedFloor(currentFloor);
		}
	}

	private DeveloperMode developerMode;

	/**
	 * イベント処理クラス
	 * 選択肢の表示・結果の分岐・状態変化を担当
	 */

	public EventProcessor(GameUI ui, DeathManager deathManager, DeveloperMode developerMode,
			DataManager dataManager, com.kh.tbrr.manager.AudioManager audioManager) {
		this.ui = ui;
		this.dataManager = dataManager;
		this.deathManager = deathManager;
		this.developerMode = developerMode;
		this.random = new Random();
		this.audioManager = audioManager;
	}

	public String processEvent(GameEvent event, Player player, GameState gameState) {

		System.err.println("[DEBUG] processEvent called: " + event.getId());

		// イベントのタイトルを表示
		if (event.getTitle() != null && !event.getTitle().isEmpty()) {
			ui.showEventInfo(event.getTitle());
		}

		// イベント表示時の画像変更処理
		if (event.getBackgroundImageChange() != null && !event.getBackgroundImageChange().isEmpty()) {
			gameState.setCurrentBackgroundImage(event.getBackgroundImageChange());
			ui.showImage("background", event.getBackgroundImageChange());
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] イベント表示時に背景画像を変更: " + event.getBackgroundImageChange());
			}
		}

		if (event.getSubImageChange() != null && !event.getSubImageChange().isEmpty()) {
			gameState.setCurrentSubImage(event.getSubImageChange());
			ui.showImage("sub", event.getSubImageChange());
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] イベント表示時にサブ画像を変更: " + event.getSubImageChange());
			}
		}

		// イベント開始時の自動効果を適用
		applyInitialEffects(event.getInitialEffects(), player, gameState);

		// ★追加: イベント開始時のSE再生
		if (event.getSoundEffect() != null && !event.getSoundEffect().isEmpty()) {
			if (audioManager != null) {
				audioManager.playSE(event.getSoundEffect());
			}
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] SE再生: " + event.getSoundEffect());
			}
		}

		// イベントの説明文を表示
		List<String> descriptions = event.getDescription();
		if (descriptions != null) {
			for (String line : descriptions) {
				String replaced = TextReplacer.replace(line, player);
				ui.print(replaced);
			}
		}
		ui.print("");

		// ★追加: ヘルパーヒントの表示
		if (gameState.hasFlag("system:helper_enabled") && !event.isSuppressHelperHint()) {
			String hint = event.getHelperHint();
			if (hint != null && !hint.isEmpty()) {
				ui.print("【ヘルパー】" + TextReplacer.replace(hint, player));
			} else {
				// フォールバックメッセージ
				ui.print("【ヘルパー】う～ん？僕にもちょっとわからないや、ごめんね！");
			}
		}

		List<Choice> availableChoices = getAvailableChoices(event, player);
		if (availableChoices == null || availableChoices.isEmpty()) {
			ui.print("【システム】選択可能な行動がありません。イベントをスキップします。");
			ui.waitForEnter();
			return null;
		}

		displayChoices(availableChoices, player);

		int choiceIndex = ui.getPlayerChoice(availableChoices.size(), player);
		if (choiceIndex <= 0 || choiceIndex > availableChoices.size()) {
			ui.print("【システム】選択肢の取得に失敗しました。イベントをスキップします。");
			ui.waitForEnter();
			return null;
		}

		Choice selectedChoice = availableChoices.get(choiceIndex - 1);
		processChoice(selectedChoice, player, gameState);

		if (!gameState.isInRecursiveEvent()) {
			gameState.incrementEventCount();
		}

		if (!gameState.isInRecursiveEvent()) {
			ui.print("");
			ui.waitForEnter();
		}

		return null;
	}

	/**
	 * イベント開始時の自動効果を適用
	 */
	private void applyInitialEffects(InitialEffects effects, Player player, GameState gameState) {
		if (effects == null) {
			return;
		}

		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[DEBUG] イベント開始時の自動効果を適用");
		}

		// メッセージ表示
		if (effects.getMessage() != null && !effects.getMessage().isEmpty()) {
			String msg = TextReplacer.replace(effects.getMessage(), player);
			ui.print(msg);
		}

		// HP変化
		if (effects.getHpChange() != null) {
			int hpChange = parseValueChange(effects.getHpChange(), player, "hp");
			if (hpChange != 0) {
				player.modifyHp(hpChange);
				if (hpChange > 0) {
					String logMessage = "【" + player.getName() + "は" + hpChange + "回復した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);

					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【" + player.getName() + "は" + (-hpChange) + "のダメージを受けた】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		// AP変化
		if (effects.getApChange() != null) {
			int apChange = parseValueChange(effects.getApChange(), player, "ap");
			if (apChange != 0) {
				player.modifyAp(apChange);
				if (apChange > 0) {
					String logMessage = "【APが" + apChange + "回復した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【APを" + (-apChange) + "消費した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		// お金変化
		if (effects.getMoneyChange() != null) {
			int moneyChange = parseValueChange(effects.getMoneyChange(), player, "money");
			if (moneyChange != 0) {
				player.modifyMoney(moneyChange);
				if (moneyChange > 0) {
					String logMessage = "【銀貨を" + moneyChange + "枚得た】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【銀貨を" + (-moneyChange) + "枚失った】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		// アイテム入手（複数対応）
		if (effects.getItemsGained() != null && !effects.getItemsGained().isEmpty()) {
			for (String itemId : effects.getItemsGained()) {
				player.addItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を手に入れた】";
				// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage); // 重要ログには表示
			}
		}

		// アイテム喪失（複数対応）
		if (effects.getItemsLost() != null && !effects.getItemsLost().isEmpty()) {
			for (String itemId : effects.getItemsLost()) {
				player.removeItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を失った】";
				// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage); // 重要ログには表示
			}
		}

		// スキル習得（複数対応）
		if (effects.getSkillsGained() != null && !effects.getSkillsGained().isEmpty()) {
			for (String skillName : effects.getSkillsGained()) {
				player.addSkill(skillName);
				ui.print("【技能「" + skillName + "」を習得した】");
			}
		}

		// スキルロスト（複数対応）
		if (effects.getSkillsLost() != null && !effects.getSkillsLost().isEmpty()) {
			for (String skillName : effects.getSkillsLost()) {
				player.removeSkill(skillName);
				String logMessage = "【技能「" + skillName + "」を失った】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			}
		}

		// 状態異常変化
		if (effects.getStatusEffectChanges() != null) {
			for (java.util.Map.Entry<String, Object> entry : effects.getStatusEffectChanges().entrySet()) {
				String effectId = entry.getKey();
				Object value = entry.getValue();

				int changeValue = 0;
				if (value instanceof Integer) {
					changeValue = (Integer) value;
				} else if (value instanceof Double) {
					changeValue = ((Double) value).intValue();
				} else if (value instanceof String) {
					try {
						changeValue = Integer.parseInt((String) value);
					} catch (NumberFormatException e) {
						continue;
					}
				}

				if (changeValue != 0) {
					boolean wasPresent = player.hasStatusEffect(effectId);
					int oldValue = player.getStatusEffectValue(effectId);
					player.modifyStatusEffectWithInit(effectId, changeValue);
					int newValue = player.getStatusEffectValue(effectId);

					String effectName = com.kh.tbrr.data.StatusEffectRegistry.getNameById(effectId);
					if (effectName == null) {
						effectName = effectId;
					}

					if (!wasPresent) {
						// 初回設定時（レイジー初期化含む）
						ui.print("【" + effectName + "が" + newValue + "になった】");
					} else {
						// 既存値の変更
						if (newValue > oldValue) {
							ui.print("【" + effectName + "が" + (newValue - oldValue) + "増加した】");
						} else if (newValue < oldValue) {
							ui.print("【" + effectName + "が" + (oldValue - newValue) + "減少した】");
						}
					}
				}
			}
		}

		// フラグ操作
		if (effects.getFlagsToAdd() != null) {
			for (String flag : effects.getFlagsToAdd()) {
				gameState.setFlag(flag);
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.err.println("[DEBUG] Flag added: " + flag);
				}
			}
		}

		if (effects.getFlagsToRemove() != null) {
			for (String flag : effects.getFlagsToRemove()) {
				gameState.removeFlag(flag);
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.err.println("[DEBUG] Flag removed: " + flag);
				}
			}
		}
	}

	private List<Choice> getAvailableChoices(GameEvent event, Player player) {
		List<Choice> available = new ArrayList<>();
		if (event.getChoices() == null)
			return available;

		for (Choice choice : event.getChoices()) {
			// 表示条件をチェック
			String displayCond = choice.getDisplayCondition();
			if (displayCond == null || displayCond.trim().isEmpty()) {
				// 条件なし → 常に表示
				available.add(choice);
			} else if (matchesCondition(displayCond, player, null)) {
				// 条件を満たす → 表示
				available.add(choice);
			}
			// 条件を満たさない → 表示しない
		}
		return available;
	}

	private void displayChoices(List<Choice> choices, Player player) {
		ui.print("━━━━━━━━━━━━━━━━━━━━━━");
		for (int i = 0; i < choices.size(); i++) {
			Choice choice = choices.get(i);
			String text = TextReplacer.replace(choice.getText(), player);
			StringBuilder line = new StringBuilder();
			line.append((i + 1)).append(". ").append(text);
			if (choice.getApCost() > 0) {
				line.append(" 【AP:").append(choice.getApCost()).append("】");
			}
			ui.print(line.toString());
		}
		ui.print("━━━━━━━━━━━━━━━━━━━━━━");
	}

	private boolean processChoice(Choice choice, Player player, GameState gameState) {
		if (choice.getApCost() > 0) {
			player.modifyAp(-choice.getApCost());
		}

		Result result = determineResult(choice, player, gameState);
		if (result == null) {
			ui.print("【システム】結果の処理に失敗しました。");
			return false;
		}

		List<String> resultTexts = TextReplacer.replaceAll(result.getText(), player);
		if (resultTexts != null && !resultTexts.isEmpty()) {
			for (String text : resultTexts) {
				ui.print(text);
			}
		}

		return applyEffects(result, player, gameState);
	}

	private Result determineResult(Choice choice, Player player, GameState state) {
		List<Result> results = choice.getResults();
		if (results == null || results.isEmpty())
			return null;

		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[EventProcessor][DEBUG] 全Result数=" + results.size());
		}

		List<Result> candidates = new ArrayList<>();
		for (int i = 0; i < results.size(); i++) {
			Result r = results.get(i);
			boolean matched = matchesCondition(r.getCondition(), player, state);
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println(
						"[EventProcessor][DEBUG] Result[" + i + "] condition='" + r.getCondition() + "' matched="
								+ matched + " chance=" + r.getChance());
			}
			if (matched)
				candidates.add(r);
		}

		if (candidates.isEmpty()) {
			candidates = new ArrayList<>(results);
		}

		int total = candidates.stream().mapToInt(r -> Math.max(0, r.getChance())).sum();
		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[EventProcessor][DEBUG] 候補数=" + candidates.size() + " total=" + total);
		}

		if (total <= 0) {
			return candidates.get(0);
		}

		int roll = random.nextInt(total) + 1;
		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[EventProcessor][DEBUG] roll=" + roll);
		}

		int cum = 0;
		for (int i = 0; i < candidates.size(); i++) {
			Result r = candidates.get(i);
			cum += Math.max(0, r.getChance());
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[EventProcessor][DEBUG] checking candidate[" + i + "] cum=" + cum + " chance="
						+ r.getChance());
			}
			if (roll <= cum) {
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.err.println("[EventProcessor][DEBUG] selected candidate index=" + i);
					System.err.println("[EventProcessor][DEBUG] result.getText() = " + r.getText());
				}
				return r;
			}
		}

		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[EventProcessor][DEBUG] fallback to last candidate");
		}
		return candidates.get(candidates.size() - 1);
	}

	/**
	 * 条件文字列の判定（AND/OR両対応版）
	 * 
	 * 記法:
	 * - `|` でOR条件（どれか1つでもtrue）
	 * - `&` でAND条件（全部true）
	 * - 例: "skill:判断力&has_any_item:common" → 判断力 AND アイテム所持
	 * - 例: "skill:判断力|skill:商才" → 判断力 OR 商才
	 * - 例: "skill:判断力&hp>10|skill:商才&hp>20" → (判断力 AND HP>10) OR (商才 AND HP>20)
	 */
	public boolean matchesCondition(String cond, Player player, GameState state) {
		if (cond == null || cond.trim().isEmpty())
			return true;

		// OR条件（|で分割）
		String[] orParts = cond.split("\\|");
		for (String orPart : orParts) {
			// AND条件（&で分割）
			String[] andParts = orPart.split("&");
			boolean allMatch = true;

			for (String raw : andParts) {
				String p = raw.trim();
				boolean negate = false;
				if (p.startsWith("not:")) {
					negate = true;
					p = p.substring(4);
				}

				boolean ok = false;
				if (p.startsWith("skill:")) {
					ok = player.hasSkill(p.substring(6));
				} else if (p.startsWith("job:")) {
					ok = p.substring(4).equalsIgnoreCase(player.getJob());
				} else if (p.startsWith("item:")) {
					ok = player.hasItem(p.substring(5));
				} else if (p.startsWith("ap>=")) {
					try {
						ok = player.getAp() >= Integer.parseInt(p.substring(4));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("ap<=")) {
					try {
						ok = player.getAp() <= Integer.parseInt(p.substring(4));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("ap<")) {
					try {
						ok = player.getAp() < Integer.parseInt(p.substring(3));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("ap>")) {
					try {
						ok = player.getAp() > Integer.parseInt(p.substring(3));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("money>=")) {
					try {
						ok = player.getMoney() >= Integer.parseInt(p.substring(7));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("money<=")) {
					try {
						ok = player.getMoney() <= Integer.parseInt(p.substring(7));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("money<")) {
					try {
						ok = player.getMoney() < Integer.parseInt(p.substring(6));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("money>")) {
					try {
						ok = player.getMoney() > Integer.parseInt(p.substring(6));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("hp>=")) {
					try {
						ok = player.getHp() >= Integer.parseInt(p.substring(4));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("hp<=")) {
					try {
						ok = player.getHp() <= Integer.parseInt(p.substring(4));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("hp<")) {
					try {
						ok = player.getHp() < Integer.parseInt(p.substring(3));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("hp>")) {
					try {
						ok = player.getHp() > Integer.parseInt(p.substring(3));
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("area:")) {
					String expected = p.substring(5);
					String current = state.getCurrentAreaName();
					ok = expected.equalsIgnoreCase(current);
				} else if (p.startsWith("status_effect_value>=:")) {
					try {
						String[] tokens = p.substring(22).split(":");
						if (tokens.length == 2) {
							String effectId = tokens[0];
							int threshold = Integer.parseInt(tokens[1]);
							ok = player.getStatusEffectValue(effectId) >= threshold;
						}
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("status_effect_value<=:")) {
					try {
						String[] tokens = p.substring(22).split(":");
						if (tokens.length == 2) {
							String effectId = tokens[0];
							int threshold = Integer.parseInt(tokens[1]);
							ok = player.getStatusEffectValue(effectId) <= threshold;
						}
					} catch (Exception e) {
						ok = false;
					}
				} else if (p.startsWith("status_effect:")) {
					String effectId = p.substring(14);
					ok = player.hasStatusEffect(effectId);
				} else if (p.startsWith("gender:")) {
					// 性別（MALE, FEMALE, OTHER）
					String expected = p.substring(7).toUpperCase();
					ok = player.getGender() != null && player.getGender().name().equals(expected);
				} else if (p.startsWith("gender_identity:")) {
					// 性自認（自由記載文字列の完全一致）
					String expected = p.substring(16);
					ok = expected.equals(player.getGenderIdentity());
				} else if (p.startsWith("clothing:")) {
					// 服装（文字列の完全一致）
					String expected = p.substring(9);
					ok = expected.equals(player.getClothing());
				} else if (p.startsWith("clothing_contains:")) {
					// 服装（部分一致）
					String keyword = p.substring(18);
					ok = player.getClothing() != null && player.getClothing().contains(keyword);
				} else if (p.startsWith("racename_contains:")) {
					// 種族名（部分一致）
					String keyword = p.substring(18);
					ok = player.getRaceName() != null && player.getRaceName().contains(keyword);
				} else if (p.startsWith("job_contains:")) {
					// 職業（部分一致）
					String keyword = p.substring(13);
					ok = player.getJob() != null && player.getJob().contains(keyword);
				} else if (p.startsWith("background_contains:")) {
					// 背景（部分一致）
					String keyword = p.substring(20);
					ok = player.getBackground() != null && player.getBackground().contains(keyword);
				} else if (p.startsWith("gender_identity_contains:")) {
					// 性自認（部分一致）
					String keyword = p.substring(25);
					ok = player.getGenderIdentity() != null && player.getGenderIdentity().contains(keyword);
				} else if (p.equals("cruel_world")) {
					// 残酷な世界モードが有効か
					ok = player.isCruelWorldEnabled();
				} else if (p.equals("fated_one")) {
					// 運命に選ばれし者か
					ok = player.isFatedOne();
				} else if (p.startsWith("has_any_item:")) {
					// 指定レアリティのアイテムを1つでも持っているか
					String rarity = p.substring(13);
					List<String> items = ItemRegistry.getLosableItemIdsByRarity(rarity);
					ok = player.getInventory().stream().anyMatch(items::contains);
				} else if (p.startsWith("flag:")) {
					// グローバルフラグチェック
					String flagName = p.substring(5);
					ok = state.hasFlag(flagName);
				}

				if (negate)
					ok = !ok;

				// AND条件なので、1つでもfalseなら全体がfalse
				if (!ok) {
					allMatch = false;
					break;
				}
			}

			// OR条件なので、1つでもtrueなら全体がtrue
			if (allMatch)
				return true;
		}

		return false;
	}

	private int parseValueChange(Object value, Player player, String changeType) {
		if (value == null) {
			return 0;
		}

		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Double) {
			return ((Double) value).intValue();
		}

		String strValue = value.toString().trim().toLowerCase();

		// ダメージ系キーワード
		if (strValue.equals("low_damage")) {
			return -(5 + random.nextInt(8));
		}
		if (strValue.equals("medium_damage")) {
			return -(18 + random.nextInt(8));
		}
		if (strValue.equals("large_damage")) {
			return -(25 + random.nextInt(16));
		}
		if (strValue.equals("sudden_death")) {
			return -999;
		}

		// 回復系キーワード
		if (strValue.equals("small_heal")) {
			return 10 + random.nextInt(11);
		}
		if (strValue.equals("medium_heal")) {
			return 30 + random.nextInt(21);
		}
		if (strValue.equals("large_heal")) {
			return 50 + random.nextInt(31);
		}

		// HP特殊操作
		if (changeType != null && changeType.equals("hp")) {
			if (strValue.startsWith("hp_set:")) {
				try {
					int targetHp = Integer.parseInt(strValue.substring(7));
					return targetHp - player.getHp();
				} catch (NumberFormatException e) {
					ui.print("【システム】HPセット値が不正です: " + strValue);
					return 0;
				}
			}

			if (strValue.equals("hp_to_half")) {
				int targetHp = player.getHp() / 2;
				return targetHp - player.getHp();
			}

			if (strValue.equals("hp_to_one")) {
				return 1 - player.getHp();
			}

			if (strValue.startsWith("hp_to_percent:")) {
				try {
					int percent = Integer.parseInt(strValue.substring(14));
					int targetHp = (player.getMaxHp() * percent) / 100;
					return targetHp - player.getHp();
				} catch (NumberFormatException e) {
					ui.print("【システム】HP割合指定が不正です: " + strValue);
					return 0;
				}
			}
		}

		// AP特殊操作
		if (changeType != null && changeType.equals("ap")) {
			if (strValue.startsWith("ap_set:")) {
				try {
					int targetAp = Integer.parseInt(strValue.substring(7));
					return targetAp - player.getAp();
				} catch (NumberFormatException e) {
					ui.print("【システム】APセット値が不正です: " + strValue);
					return 0;
				}
			}

			if (strValue.equals("ap_to_max")) {
				return player.getMaxAp() - player.getAp();
			}
		}

		// ランダム値
		if (strValue.equals("small_random")) {
			return 3 + random.nextInt(5);
		}
		if (strValue.equals("medium_random")) {
			return 8 + random.nextInt(8);
		}
		if (strValue.equals("large_random")) {
			return 15 + random.nextInt(11);
		}

		ui.print("【システム】不明なキーワード: " + strValue);
		return 0;
	}

	private boolean applyEffects(Result result, Player player, GameState gameState) {
		boolean died = false;

		// ★追加: 結果表示時のSE再生
		if (result.getSoundEffect() != null && !result.getSoundEffect().isEmpty()) {
			if (audioManager != null) {
				audioManager.playSE(result.getSoundEffect());
			}
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] Result SE再生: " + result.getSoundEffect());
			}
		}

		// 立ち絵の表情変更
		if (result.getExpressionChange() != null && !result.getExpressionChange().isEmpty()) {
			ui.changePortraitExpression(result.getExpressionChange());
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] 表情を「" + result.getExpressionChange() + "」に変更しました");
			}
		}

		if (result.getHpChange() != null) {
			int hpChange = parseValueChange(result.getHpChange(), player, "hp");

			if (hpChange != 0) {
				player.modifyHp(hpChange);

				if (hpChange < 0) {
					String logMessage = "【" + player.getName() + "は" + (-hpChange) + "のダメージを受けた】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);

					if (player.getHp() <= 0 && !gameState.isGameOver()) {
						String deathCause = null;
						if (result.getDeath() != null && result.getDeath().getDeathCause() != null) {
							deathCause = result.getDeath().getDeathCause();
						}
						deathManager.processDeath(deathCause, player, gameState);
						return true;
					}
				} else {
					String logMessage = "【" + player.getName() + "は" + hpChange + "回復した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		if (result.getApChange() != null) {
			int apChange = parseValueChange(result.getApChange(), player, "ap");

			if (apChange != 0) {
				player.modifyAp(apChange);

				if (apChange > 0) {
					String logMessage = "【APが" + apChange + "回復した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【APを" + (-apChange) + "消費した】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		if (result.getMoneyChange() != null) {
			int moneyChange = parseValueChange(result.getMoneyChange(), player, "money");

			if (moneyChange != 0) {
				player.modifyMoney(moneyChange);

				if (moneyChange > 0) {
					String logMessage = "【銀貨を" + moneyChange + "枚得た】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【銀貨を" + (-moneyChange) + "枚失った】";
					// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				}
			}
		}

		// 単一アイテム入手（後方互換性）
		if (result.getItemGained() != null && !result.getItemGained().isEmpty()) {
			player.addItem(result.getItemGained());
			String itemName = ItemRegistry.getNameById(result.getItemGained());
			if (itemName == null) {
				itemName = result.getItemGained();
			}
			String logMessage = "【" + itemName + "を手に入れた】";
			// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
			printFloorDividerIfNeeded(gameState);
			ui.printImportantLog(logMessage); // 重要ログには表示
		}

		// 複数アイテム入手
		if (result.getItemsGained() != null && !result.getItemsGained().isEmpty()) {
			for (String itemId : result.getItemsGained()) {
				player.addItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を手に入れた】";
				// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage); // 重要ログには表示
			}
		}

		// ★追加: ランダムアイテム入手
		if (result.getItemGainedRandom() != null && !result.getItemGainedRandom().isEmpty()) {
			String rarity = result.getItemGainedRandom();
			List<String> availableItems = ItemRegistry.getLosableItemIdsByRarity(rarity);
			// プレイヤーが持っていないアイテムのみを候補に
			availableItems.removeAll(player.getInventory());

			if (!availableItems.isEmpty()) {
				String itemId = availableItems.get(random.nextInt(availableItems.size()));
				player.addItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を手に入れた】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			} else {
				String logMessage = "【入手できるアイテムがなかった】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			}
		}

		// 単一アイテム喪失（後方互換性）
		if (result.getItemLost() != null && !result.getItemLost().isEmpty()) {
			player.removeItem(result.getItemLost());
			String itemName = ItemRegistry.getNameById(result.getItemLost());
			if (itemName == null) {
				itemName = result.getItemLost();
			}
			String logMessage = "【" + itemName + "を失った】";
			// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
			printFloorDividerIfNeeded(gameState);
			ui.printImportantLog(logMessage); // 重要ログには表示
		}

		// 複数アイテム喪失
		if (result.getItemsLost() != null && !result.getItemsLost().isEmpty()) {
			for (String itemId : result.getItemsLost()) {
				player.removeItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を失った】";
				// ui.print(logMessage); // ← コメントアウト：左下メッセージエリアには表示しない
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage); // 重要ログには表示
			}
		}

		// ★追加: ランダムアイテム喪失
		if (result.getItemLostRandom() != null && !result.getItemLostRandom().isEmpty()) {
			String rarity = result.getItemLostRandom();
			List<String> losableItems = ItemRegistry.getLosableItemIdsByRarity(rarity);
			// プレイヤーが持っているアイテムのみを候補に
			losableItems.retainAll(player.getInventory());

			if (!losableItems.isEmpty()) {
				String itemId = losableItems.get(random.nextInt(losableItems.size()));
				player.removeItem(itemId);
				String itemName = ItemRegistry.getNameById(itemId);
				if (itemName == null) {
					itemName = itemId;
				}
				String logMessage = "【" + itemName + "を失った】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			} else {
				String logMessage = "【失うアイテムがなかった】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			}
		}

		// ★追加: 複数ランダムアイテム喪失
		if (result.getItemLostRandomList() != null && !result.getItemLostRandomList().isEmpty()) {
			for (String rarity : result.getItemLostRandomList()) {
				List<String> losableItems = ItemRegistry.getLosableItemIdsByRarity(rarity);
				losableItems.retainAll(player.getInventory());

				if (!losableItems.isEmpty()) {
					String itemId = losableItems.get(random.nextInt(losableItems.size()));
					player.removeItem(itemId);
					String itemName = ItemRegistry.getNameById(itemId);
					if (itemName == null) {
						itemName = itemId;
					}
					String logMessage = "【" + itemName + "を失った】";
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
				} else {
					String logMessage = "【失うアイテムがなかった】";
					printFloorDividerIfNeeded(gameState);
					ui.printImportantLog(logMessage);
					break; // これ以上失うアイテムがないので中断
				}
			}
		}

		// 単一スキル習得
		if (result.getSkillGained() != null && !result.getSkillGained().isEmpty()) {
			player.addSkill(result.getSkillGained());
			ui.print("【技能「" + result.getSkillGained() + "」を習得した】");
		}

		// 複数スキル習得
		if (result.getSkillsGained() != null && !result.getSkillsGained().isEmpty()) {
			for (String skillName : result.getSkillsGained()) {
				player.addSkill(skillName);
				ui.print("【技能「" + skillName + "」を習得した】");
			}
		}

		// 単一スキルロスト
		if (result.getSkillLost() != null && !result.getSkillLost().isEmpty()) {
			player.removeSkill(result.getSkillLost());
			String logMessage = "【技能「" + result.getSkillLost() + "」を失った】";
			printFloorDividerIfNeeded(gameState);
			ui.printImportantLog(logMessage);
		}

		// 複数スキルロスト
		if (result.getSkillsLost() != null && !result.getSkillsLost().isEmpty()) {
			for (String skillName : result.getSkillsLost()) {
				player.removeSkill(skillName);
				String logMessage = "【技能「" + skillName + "」を失った】";
				printFloorDividerIfNeeded(gameState);
				ui.printImportantLog(logMessage);
			}
		}

		// 状態異常の処理
		if (result.getStatusEffectChanges() != null) {
			for (java.util.Map.Entry<String, Object> entry : result.getStatusEffectChanges().entrySet()) {
				String effectId = entry.getKey();
				Object value = entry.getValue();

				int changeValue = 0;
				if (value instanceof Integer) {
					changeValue = (Integer) value;
				} else if (value instanceof Double) {
					changeValue = ((Double) value).intValue();
				} else if (value instanceof String) {
					try {
						changeValue = Integer.parseInt((String) value);
					} catch (NumberFormatException e) {
						ui.print("【システム】状態異常の値が不正です: " + value);
						continue;
					}
				}

				if (changeValue != 0) {
					boolean wasPresent = player.hasStatusEffect(effectId);
					int oldValue = player.getStatusEffectValue(effectId);
					player.modifyStatusEffectWithInit(effectId, changeValue);
					int newValue = player.getStatusEffectValue(effectId);

					String effectName = com.kh.tbrr.data.StatusEffectRegistry.getNameById(effectId);
					if (effectName == null) {
						effectName = effectId;
					}

					if (!wasPresent) {
						// 初回設定時(レイジー初期化含む)
						String logMessage = "【" + effectName + "が" + newValue + "になった】";
						ui.print(logMessage); // テキスト表示エリアにも表示
						printFloorDividerIfNeeded(gameState);
						ui.printImportantLog(logMessage);
					} else {
						// 既存値の変更
						if (newValue > oldValue) {
							String logMessage = "【" + effectName + "が" + (newValue - oldValue) + "増加した】";
							ui.print(logMessage); // テキスト表示エリアにも表示
							printFloorDividerIfNeeded(gameState);
							ui.printImportantLog(logMessage);
						} else if (newValue < oldValue) {
							String logMessage = "【" + effectName + "が" + (oldValue - newValue) + "減少した】";
							ui.print(logMessage); // テキスト表示エリアにも表示
							printFloorDividerIfNeeded(gameState);
							ui.printImportantLog(logMessage);
						}
					}

					if (oldValue > 0 && newValue == 0) {
						String logMessage = "【" + effectName + "が治った】";
						ui.print(logMessage); // テキスト表示エリアにも表示
						printFloorDividerIfNeeded(gameState);
						ui.printImportantLog(logMessage);
					}
				}
			}
		}

		// 画像変更の処理
		if (result.getBackgroundImageChange() != null && !result.getBackgroundImageChange().isEmpty()) {
			gameState.setCurrentBackgroundImage(result.getBackgroundImageChange());
			ui.showImage("background", result.getBackgroundImageChange());
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] 背景画像を変更: " + result.getBackgroundImageChange());
			}
		}

		if (result.getSubImageChange() != null && !result.getSubImageChange().isEmpty()) {
			gameState.setCurrentSubImage(result.getSubImageChange());
			ui.showImage("sub", result.getSubImageChange());
			if (developerMode != null && developerMode.isDebugVisible()) {
				ui.print("[DEBUG] サブ画像を変更: " + result.getSubImageChange());
			}
		}

		if (result.getNextEventId() != null && !result.getNextEventId().isEmpty()) {
			boolean wasInRecursive = gameState.isInRecursiveEvent();

			if (!wasInRecursive) {
				gameState.setInRecursiveEvent(true);
			}

			GameEvent next = dataManager.loadEvent(result.getNextEventId());
			if (next != null) {
				processEvent(next, player, gameState);
			}

			gameState.setInRecursiveEvent(wasInRecursive);
		}

		// キャラシ項目の変更

		if (result.getClothingChange() != null && !result.getClothingChange().isEmpty()) {
			player.setClothing(result.getClothingChange());
			ui.print("【服装が「" + result.getClothingChange() + "」に変わった】");
		}

		if (result.getJobChange() != null && !result.getJobChange().isEmpty()) {
			player.setJob(result.getJobChange());
			ui.print("【職業が「" + result.getJobChange() + "」に変わった】");
		}

		if (result.getBackgroundChange() != null && !result.getBackgroundChange().isEmpty()) {
			player.setBackground(result.getBackgroundChange());
			ui.print("【背景が「" + result.getBackgroundChange() + "」に変わった】");
		}

		if (result.getBodyTypeChange() != null && !result.getBodyTypeChange().isEmpty()) {
			player.setBodyType(result.getBodyTypeChange());
			ui.print("【体型が「" + result.getBodyTypeChange() + "」に変わった】");
		}

		if (result.getRaceNameChange() != null && !result.getRaceNameChange().isEmpty()) {
			player.setRaceName(result.getRaceNameChange());
			ui.print("【種族名が「" + result.getRaceNameChange() + "」に変わった】");
		}

		if (result.getRaceTypeChange() != null && !result.getRaceTypeChange().isEmpty()) {
			try {
				Player.RaceType type = Player.RaceType.valueOf(result.getRaceTypeChange().toUpperCase());
				player.setRaceType(type);
				ui.print("【種族タイプが「" + type.getDisplayName() + "」に変わった】");
			} catch (IllegalArgumentException e) {
				ui.print("【種族タイプの変更に失敗しました（無効な値）】");
			}
		}

		if (result.getGenderChange() != null && !result.getGenderChange().isEmpty()) {
			try {
				Player.Gender gender = Player.Gender.valueOf(result.getGenderChange().toUpperCase());
				player.setGender(gender);
				ui.print("【性別が「" + gender.getDisplayName() + "」に変わった】");
			} catch (IllegalArgumentException e) {
				ui.print("【性別の変更に失敗しました(無効な値)】");
			}
		}

		if (result.getGenderIdentityChange() != null && !result.getGenderIdentityChange().isEmpty()) {
			player.setGenderIdentity(result.getGenderIdentityChange());
			ui.print("【性自認が「" + result.getGenderIdentityChange() + "」に変わった】");
		}

		// アナザーエンディングの処理
		if (result.isAlternateEnding()) {
			gameState.setAlternateEndingId(result.getAlternateEndingId());
			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] アナザーエンディングがトリガーされました: " + result.getAlternateEndingId());
			}
		}

		// フラグ操作
		if (result.getFlagsToAdd() != null) {
			for (String flag : result.getFlagsToAdd()) {
				gameState.setFlag(flag);
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.err.println("[DEBUG] Flag added: " + flag);
				}
			}
		}

		if (result.getFlagsToRemove() != null) {
			for (String flag : result.getFlagsToRemove()) {
				gameState.removeFlag(flag);
				if (developerMode != null && developerMode.isDebugVisible()) {
					System.err.println("[DEBUG] Flag removed: " + flag);
				}
			}
		}

		// インタラクション（ミニゲーム・判定・戦闘など）の処理
		if (result.getInteraction() != null && !result.getInteraction().isEmpty()) {
			executeInteraction(result.getInteraction(), result.getInteractionParams(), player, gameState);
		}

		return died;
	}

	/**
	 * インタラクションを実行
	 * 
	 * @param type      インタラクションタイプ（例: "coin_toss"）
	 * @param params    インタラクションパラメータ
	 * @param player    プレイヤー
	 * @param gameState ゲーム状態
	 */
	@SuppressWarnings("unchecked")
	private void executeInteraction(String type, Map<String, Object> params, Player player, GameState gameState) {
		InteractionHandler handler = InteractionRegistry.get(type);
		if (handler == null) {
			ui.print("【システム】未知のインタラクション: " + type);
			return;
		}

		if (developerMode != null && developerMode.isDebugVisible()) {
			System.err.println("[DEBUG] インタラクション実行: " + type);
		}

		try {
			// UI参照をパラメータに追加（JavaFXUI専用機能）
			Map<String, Object> enrichedParams = new java.util.HashMap<>();
			if (params != null) {
				enrichedParams.putAll(params);
			}

			// CountDownLatchで結果を待機
			java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
			java.util.concurrent.atomic.AtomicReference<InteractionResult> resultRef = new java.util.concurrent.atomic.AtomicReference<>();

			// インタラクション用の入力コールバックを作成
			java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> interactionCallback = new java.util.concurrent.atomic.AtomicReference<>();

			// FXスレッドでUI操作とインタラクション実行を行う
			javafx.application.Platform.runLater(() -> {
				try {
					// サブウィンドウペインを取得して追加（FXスレッド内で実行）
					Object subWindowPane = ui.getSubWindowPane();
					if (subWindowPane != null && ui instanceof com.kh.tbrr.ui.JavaFXUI) {
						com.kh.tbrr.ui.JavaFXUI jfxUI = (com.kh.tbrr.ui.JavaFXUI) ui;
						javafx.scene.layout.StackPane stackPane = jfxUI.getSubWindowAsStackPane();
						enrichedParams.put("_subWindowPane", stackPane);

						// 入力ハンドラーをパラメータに追加して○ボタン入力を受け取れるようにする
						enrichedParams.put("_inputCallback", interactionCallback);

						// JavaFXUIに一時的な入力ハンドラーを設定
						jfxUI.setInteractionInputHandler(input -> {
							java.util.function.Consumer<String> callback = interactionCallback.get();
							if (callback != null) {
								callback.accept(input);
							}
						});
					}

					// インタラクションを非同期実行
					handler.execute(enrichedParams, player).thenAccept(result -> {
						// 入力ハンドラーをクリア
						if (ui instanceof com.kh.tbrr.ui.JavaFXUI) {
							((com.kh.tbrr.ui.JavaFXUI) ui).setInteractionInputHandler(null);
						}
						resultRef.set(result);
						latch.countDown();
					}).exceptionally(ex -> {
						// 入力ハンドラーをクリア
						if (ui instanceof com.kh.tbrr.ui.JavaFXUI) {
							((com.kh.tbrr.ui.JavaFXUI) ui).setInteractionInputHandler(null);
						}
						resultRef.set(new InteractionResult("error"));
						latch.countDown();
						return null;
					});
				} catch (Exception ex) {
					resultRef.set(new InteractionResult("error"));
					latch.countDown();
				}
			});

			// 結果を待機（最大60秒）
			boolean completed = latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
			if (!completed) {
				ui.print("【システム】インタラクションがタイムアウトしました");
				return;
			}

			InteractionResult interactionResult = resultRef.get();
			if (interactionResult == null) {
				ui.print("【システム】インタラクション結果がありません");
				return;
			}

			if (developerMode != null && developerMode.isDebugVisible()) {
				System.err.println("[DEBUG] インタラクション結果: " + interactionResult.getResultKey());
			}

			// 結果に応じた処理
			handleInteractionResult(interactionResult, params, player, gameState);

		} catch (Exception e) {
			ui.print("【システム】インタラクション実行エラー: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * インタラクション結果を処理
	 * 
	 * @param result    インタラクション結果
	 * @param params    パラメータ（results マップを含む）
	 * @param player    プレイヤー
	 * @param gameState ゲーム状態
	 */
	@SuppressWarnings("unchecked")
	private void handleInteractionResult(InteractionResult result, Map<String, Object> params, Player player,
			GameState gameState) {
		if (params == null)
			return;

		Map<String, Object> results = (Map<String, Object>) params.get("results");
		if (results == null) {
			ui.print("【システム】インタラクション結果マッピングがありません");
			return;
		}

		Map<String, Object> outcomeData = (Map<String, Object>) results.get(result.getResultKey());
		if (outcomeData == null) {
			ui.print("【システム】結果キーが見つかりません: " + result.getResultKey());
			return;
		}

		// 説明文の表示
		if (outcomeData.get("description") != null) {
			List<String> descriptions = (List<String>) outcomeData.get("description");
			for (String line : descriptions) {
				ui.print(TextReplacer.replace(line, player));
			}
		}

		// 次のイベントへの遷移
		if (outcomeData.get("nextEventId") != null) {
			String nextEventId = (String) outcomeData.get("nextEventId");
			GameEvent nextEvent = com.kh.tbrr.manager.EventManager.getEventById(nextEventId);
			if (nextEvent != null) {
				gameState.setInRecursiveEvent(true);
				ui.waitForEnter();
				processEvent(nextEvent, player, gameState);
				gameState.setInRecursiveEvent(false);
			}
		}

		// HP変化
		if (outcomeData.get("hpChange") != null) {
			int hpChange = parseValueChange(outcomeData.get("hpChange"), player, "hp");
			if (hpChange != 0) {
				player.modifyHp(hpChange);
				if (hpChange > 0) {
					ui.print("【" + player.getName() + "は" + hpChange + "回復した】");
				} else {
					ui.print("【" + player.getName() + "は" + (-hpChange) + "のダメージを受けた】");
				}
			}
		}

		// お金変化
		if (outcomeData.get("moneyChange") != null) {
			int moneyChange = parseValueChange(outcomeData.get("moneyChange"), player, "money");
			if (moneyChange != 0) {
				player.modifyMoney(moneyChange);
				if (moneyChange > 0) {
					ui.printImportantLog("【銀貨を" + moneyChange + "枚得た】");
				} else {
					ui.printImportantLog("【銀貨を" + (-moneyChange) + "枚失った】");
				}
			}
		}
	}
}