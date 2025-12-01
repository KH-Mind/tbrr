package com.kh.tbrr.system;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.ui.GameUI;

/**
 * 開発者モード
 * デバッグ用の機能を提供
 */
public class DeveloperMode {
	private boolean debugVisible = true;
	private GameUI ui; // ConsoleUI から GameUI に変更
	private boolean enabled;
	private Player currentPlayer;

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
		}
	}
}