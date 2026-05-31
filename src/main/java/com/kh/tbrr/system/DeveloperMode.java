package com.kh.tbrr.system;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.data.models.Item;
import com.kh.tbrr.data.ItemRegistry;
import com.kh.tbrr.ui.GameUI;
import com.kh.tbrr.core.GameState;

/**
 * 開発者モード
 * デバッグ用の機能を提供
 * それっぽいコマンド名になっているが、コマンド・イベント駆動方式ではなく直接操作である
 */
public class DeveloperMode {
	private boolean debugVisible = true;
	private GameUI ui; // ConsoleUI から GameUI に変更
	private boolean enabled;
	private Player currentPlayer;
	private GameState gameState;

	public DeveloperMode() {
		this.enabled = false;
	}

	public DeveloperMode(GameUI ui) {
		this.ui = ui;
		this.enabled = false;
	}

	public void setUI(GameUI ui) {
		this.ui = ui;
	}

	public void setCurrentPlayer(Player player) {
		this.currentPlayer = player;
	}

	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isDebugVisible() {
		return enabled && debugVisible;
	}

	public void toggle() {
		enabled = !enabled;
		debugVisible = enabled;
		// ログ出力は handleDevCommand 側で行うため、ここでは出力しない
	}

	public void enable() {
		enabled = true;
		debugVisible = true;
		if (ui != null)
			ui.print("🔧 開発者モードを有効にしました");
	}

	public void disable() {
		if (enabled) {
			enabled = false;
			if (ui != null)
				ui.print("開発者モードを無効にしました");
		}
	}

	public void handleDevCommand(String input, Player player) {
		if (input == null || input.trim().isEmpty())
			return;

		String cmd = input.trim().toLowerCase();

		// admin または dev 単語だけで切り替え
		if (cmd.equals("admin") || cmd.equals("dev")) {
			toggle();
			if (ui != null) {
				ui.print("🔧 開発者モードを" + (enabled ? "有効" : "無効") + "にしました");
			}
			return;
		}

		// admin on, admin off, debug on, debug off の処理
		switch (cmd) {
			case "admin on":
				enable();
				return;
			case "admin off":
				disable();
				return;
			case "debug on":
				if (enabled) {
					debugVisible = true;
					if (ui != null)
						ui.print("[DEV] DEBUG 表示 ON");
				}
				return;
			case "debug off":
				if (enabled) {
					debugVisible = false;
					if (ui != null)
						ui.print("[DEV] DEBUG 表示 OFF");
				}
				return;
			case "helper on":
				if (enabled) {
					if (gameState != null) {
						gameState.setFlag("system:helper_enabled");
						if (ui != null)
							ui.print("[DEV] ヘルパー機能を ON にしました");
					} else {
						if (ui != null)
							ui.printError("[DEV] エラー: ゲーム状態が初期化されていません");
					}
				}
				return;
			case "helper off":
				if (enabled) {
					if (gameState != null) {
						gameState.removeFlag("system:helper_enabled");
						if (ui != null)
							ui.print("[DEV] ヘルパー機能を OFF にしました");
					} else {
						if (ui != null)
							ui.printError("[DEV] エラー: ゲーム状態が初期化されていません");
					}
				}
				return;
		}

		// ここから先は開発者モードが有効かつPlayerが必要なコマンド
		if (!enabled)
			return;

		if (player == null) {
			if (ui != null)
				ui.print("[エラー] プレイヤー情報が設定されていません");
			return;
		}

		if (cmd.startsWith("player.sethp ")) {
			try {
				int value = Integer.parseInt(cmd.substring("player.sethp ".length()));
				int newHp = Math.max(1, value);
				player.setHp(newHp);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] HP を " + newHp + " に変更しました");
			} catch (NumberFormatException e) {
				if (ui != null)
					ui.printError("[DEBUG] HPの値が不正です");
			}

		} else if (cmd.startsWith("player.setap ")) {
			try {
				int value = Integer.parseInt(cmd.substring("player.setap ".length()));
				int newAp = Math.max(0, value);
				player.setAp(newAp);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] AP を " + newAp + " に変更しました");
			} catch (NumberFormatException e) {
				if (ui != null)
					ui.printError("[DEBUG] APの値が不正です");
			}
		}

		else if (cmd.startsWith("player.setmoney ")) {
			try {
				int value = Integer.parseInt(cmd.substring("player.setmoney ".length()));
				int newMoney = Math.max(0, value);
				player.setMoney(newMoney);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 銀貨 を " + newMoney + " に変更しました");
			} catch (NumberFormatException e) {
				if (ui != null)
					ui.printError("[DEBUG] 銀貨の値が不正です");
			}

		} else if (cmd.startsWith("player.additem ")) {
			String itemId = cmd.substring("player.additem ".length()).trim();
			if (!itemId.isEmpty()) {
				player.addItem(itemId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] アイテム '" + itemId + "' を追加しました");
			}
		} else if (cmd.startsWith("player.removeitem ")) {
			String itemId = cmd.substring("player.removeitem ".length()).trim();
			if (!itemId.isEmpty()) {
				player.removeItem(itemId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] アイテム '" + itemId + "' を削除しました");
			}
		} else if (cmd.startsWith("player.addskill ")) {
			String skillName = cmd.substring("player.addskill ".length()).trim();
			if (!skillName.isEmpty()) {
				player.addSkill(skillName);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 技能 '" + skillName + "' を追加しました");
			}
		} else if (cmd.startsWith("player.removeskill ")) {
			String skillName = cmd.substring("player.removeskill ".length()).trim();
			if (!skillName.isEmpty()) {
				player.getSkills().remove(skillName);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 技能 '" + skillName + "' を削除しました");
			}
		} else if (cmd.startsWith("player.setstatuseffect ")) {
			// player.setstatuseffect <状態異常ID> <数値>
			String[] parts = cmd.substring("player.setstatuseffect ".length()).trim().split("\\s+");
			if (parts.length >= 2) {
				String effectId = parts[0];
				try {
					int value = Integer.parseInt(parts[1]);
					player.setStatusEffect(effectId, value);
					if (debugVisible && ui != null)
						ui.print("[DEBUG] 状態異常 '" + effectId + "' を " + value + " に設定しました");
				} catch (NumberFormatException e) {
					if (ui != null)
						ui.printError("[DEBUG] 数値が不正です");
				}
			} else {
				if (ui != null)
					ui.printError("[DEBUG] 使用法: player.setstatuseffect <状態異常ID> <数値>");
			}
		} else if (cmd.startsWith("player.removestatuseffect ")) {
			// player.removestatuseffect <状態異常ID>
			String effectId = cmd.substring("player.removestatuseffect ".length()).trim();
			if (!effectId.isEmpty()) {
				player.removeStatusEffect(effectId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 状態異常 '" + effectId + "' を削除しました");
			}
		} else if (cmd.startsWith("player.addtrait ")) {
			String traitId = cmd.substring("player.addtrait ".length()).trim();
			if (!traitId.isEmpty()) {
				player.addTrait(traitId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 特徴 '" + traitId + "' を追加しました");
			}
		} else if (cmd.startsWith("player.removetrait ")) {
			String traitId = cmd.substring("player.removetrait ".length()).trim();
			if (!traitId.isEmpty()) {
				player.getTraits().remove(traitId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] 特徴 '" + traitId + "' を削除しました");
			}
		} else if (cmd.startsWith("player.addability ")) {
			String abilityId = cmd.substring("player.addability ".length()).trim();
			if (!abilityId.isEmpty()) {
				player.addAbility(abilityId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] アビリティ '" + abilityId + "' を追加しました");
			}
		} else if (cmd.startsWith("player.removeability ")) {
			String abilityId = cmd.substring("player.removeability ".length()).trim();
			if (!abilityId.isEmpty()) {
				player.getAbilities().remove(abilityId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] アビリティ '" + abilityId + "' を削除しました");
			}
		} else if (cmd.startsWith("player.addstance ")) {
			String stanceId = cmd.substring("player.addstance ".length()).trim();
			if (!stanceId.isEmpty()) {
				player.addStance(stanceId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] スタンス '" + stanceId + "' を追加しました");
			}
		} else if (cmd.startsWith("player.removestance ")) {
			String stanceId = cmd.substring("player.removestance ".length()).trim();
			if (!stanceId.isEmpty()) {
				player.getStances().remove(stanceId);
				if (debugVisible && ui != null)
					ui.print("[DEBUG] スタンス '" + stanceId + "' を削除しました");
			}
		} else if (cmd.startsWith("player.equipweapon ")) {
			String itemId = cmd.substring("player.equipweapon ".length()).trim();
			if (!itemId.isEmpty()) {
				if (itemId.equalsIgnoreCase("null") || itemId.equalsIgnoreCase("none")) {
					player.setEquippedMainWeapon(null);
					if (debugVisible && ui != null)
						ui.print("[DEBUG] メイン武器を外しました");
				} else {
					Item item = ItemRegistry.getItemById(itemId);
					if (item == null) {
						if (ui != null) ui.printError("[DEBUG] エラー: アイテム '" + itemId + "' は存在しません");
					} else if (!"WEAPON".equalsIgnoreCase(item.getEquipmentCategory())) {
						if (ui != null) ui.printError("[DEBUG] エラー: '" + itemId + "' は武器ではありません");
					} else {
						player.setEquippedMainWeapon(itemId);
						if (!player.getInventory().contains(itemId)) {
							player.addItem(itemId);
						}
						if (debugVisible && ui != null)
							ui.print("[DEBUG] メイン武器に '" + itemId + "' を装備しました");
					}
				}
			}
		} else if (cmd.startsWith("player.equipaccessory ")) {
			String[] parts = cmd.substring("player.equipaccessory ".length()).trim().split("\\s+");
			if (parts.length >= 2) {
				try {
					int slot = Integer.parseInt(parts[0]);
					String itemId = parts[1];
					if (slot >= 1 && slot <= 3) {
						java.util.List<String> accs = player.getEquippedAccessories();
						while (accs.size() < 3) {
							accs.add(null);
						}
						if (itemId.equalsIgnoreCase("null") || itemId.equalsIgnoreCase("none")) {
							accs.set(slot - 1, null);
							if (debugVisible && ui != null)
								ui.print("[DEBUG] アクセサリー枠 " + slot + " を外しました");
						} else {
							Item item = ItemRegistry.getItemById(itemId);
							if (item == null) {
								if (ui != null) ui.printError("[DEBUG] エラー: アイテム '" + itemId + "' は存在しません");
							} else if (!"ACCESSORY".equalsIgnoreCase(item.getEquipmentCategory())) {
								if (ui != null) ui.printError("[DEBUG] エラー: '" + itemId + "' はアクセサリーではありません");
							} else {
								accs.set(slot - 1, itemId);
								if (!player.getInventory().contains(itemId)) {
									player.addItem(itemId);
								}
								if (debugVisible && ui != null)
									ui.print("[DEBUG] アクセサリー枠 " + slot + " に '" + itemId + "' を装備しました");
							}
						}
					} else {
						if (ui != null)
							ui.printError("[DEBUG] アクセサリースロットは 1 から 3 の間です");
					}
				} catch (NumberFormatException e) {
					if (ui != null)
						ui.printError("[DEBUG] スロット番号が不正です");
				}
			} else {
				if (ui != null)
					ui.printError("[DEBUG] 使用法: player.equipaccessory <スロット番号1-3> <アイテムID>");
			}
		} else if (cmd.startsWith("player.equipreserve ")) {
			String[] parts = cmd.substring("player.equipreserve ".length()).trim().split("\\s+");
			if (parts.length >= 2) {
				try {
					int slot = Integer.parseInt(parts[0]);
					String itemId = parts[1];
					int maxSlots = player.getMaxReserveSlots();
					if (slot >= 1 && slot <= maxSlots) {
						java.util.List<String> res = player.getReserveEquipments();
						while (res.size() < maxSlots) {
							res.add(null);
						}
						if (itemId.equalsIgnoreCase("null") || itemId.equalsIgnoreCase("none")) {
							res.set(slot - 1, null);
							if (debugVisible && ui != null)
								ui.print("[DEBUG] 予備装備枠 " + slot + " を外しました");
						} else {
							Item item = ItemRegistry.getItemById(itemId);
							if (item == null) {
								if (ui != null) ui.printError("[DEBUG] エラー: アイテム '" + itemId + "' は存在しません");
							} else if (!("WEAPON".equalsIgnoreCase(item.getEquipmentCategory()) || "ACCESSORY".equalsIgnoreCase(item.getEquipmentCategory()))) {
								if (ui != null) ui.printError("[DEBUG] エラー: '" + itemId + "' は装備品ではありません");
							} else {
								res.set(slot - 1, itemId);
								if (!player.getInventory().contains(itemId)) {
									player.addItem(itemId);
								}
								if (debugVisible && ui != null)
									ui.print("[DEBUG] 予備装備枠 " + slot + " に '" + itemId + "' を装備しました");
							}
						}
					} else {
						if (ui != null)
							ui.printError("[DEBUG] 予備スロットは 1 から " + maxSlots + " の間です");
					}
				} catch (NumberFormatException e) {
					if (ui != null)
						ui.printError("[DEBUG] スロット番号が不正です");
				}
			} else {
				if (ui != null)
					ui.printError("[DEBUG] 使用法: player.equipreserve <スロット番号> <アイテムID>");
			}
		}
	}
}