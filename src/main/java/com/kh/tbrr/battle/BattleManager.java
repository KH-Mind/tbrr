package com.kh.tbrr.battle;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import com.kh.tbrr.data.models.Player;
import com.kh.tbrr.ui.GameUI;
import com.google.gson.Gson;

public class BattleManager {
    private GameUI ui;
    private Player player;
    private BattleState state;
    private Random random;

    public BattleManager(GameUI ui, Player player) {
        this.ui = ui;
        this.player = player;
        this.random = new Random();
    }

    public String getDeathCause() {
        if (state != null && state.getCurrentEnemy() != null) {
            String cause = state.getCurrentEnemy().getDeathCause();
            return (cause != null && !cause.isEmpty()) ? cause : "generic";
        }
        return "generic";
    }

    private EnemyData loadEnemyData(String enemyId) {
        try {
            String path = "/data/enemies/" + enemyId + ".json";
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                return new Gson().fromJson(reader, EnemyData.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startBattle(String enemyId) {
        state = new BattleState();
        
        EnemyData enemy = loadEnemyData(enemyId);
        if (enemy == null) {
            ui.print("【エラー】敵データの読み込みに失敗しました: " + enemyId);
            return;
        }
        state.setCurrentEnemy(enemy);
        
        ui.print("【バトル開始！】 " + enemy.getName() + " に遭遇した！");

        if (ui instanceof com.kh.tbrr.ui.JavaFXUI) {
            com.kh.tbrr.ui.JavaFXUI jfxUi = (com.kh.tbrr.ui.JavaFXUI) ui;
            jfxUi.setBattleMode(true);

            // 背景と敵画像の表示
            if(enemy.getBattleBackground() != null && !enemy.getBattleBackground().isEmpty()) {
                ui.showImage("background", enemy.getBattleBackground());
            } else {
                ui.showImage("background", "bg_000.png"); // デフォルトの戦闘背景
            }
            if(enemy.getImagePath() != null && !enemy.getImagePath().isEmpty()) {
                ui.showImage("enemy", enemy.getImagePath());
            }

            boolean battleEnded = false;
            while (!battleEnded) {
                ui.print(" ");
                ui.print("--- ターン " + state.getTurnCount() + " --- [現在距離: " + state.getDistance() + "]");
                ui.print("【敵】" + enemy.getName() + " (HP: " + enemy.getHp() + "/" + enemy.getMaxHp() + ")");
                ui.print("コマンドを選択してください。");
                
                BattleCommand cmd = jfxUi.getBattleCommand();
                if (cmd != null) {
                    ui.print("＞プレイヤーの行動: " + cmd.toString());

                    // --- プレイヤーのターン処理 ---
                    boolean escapeSuccess = processPlayerTurn(cmd, enemy);
                    if (escapeSuccess) {
                    	// 逃走完了
                        battleEnded = true;
                        continue;
                    }

                    // 敵の死亡判定
                    if (enemy.getHp() <= 0) {
                        ui.print("【勝利！】 " + enemy.getName() + " を倒した！");
                        battleEnded = true;
                        continue;
                    }

                    // --- 敵のターン処理 ---
                    processEnemyTurn(enemy);
                    
                    // プレイヤーの死亡判定
                    if (player.getHp() <= 0) {
                        ui.print("【敗北】 プレイヤーのHPが0になった……");
                        battleEnded = true;
                        continue;
                    }
                    
                    state.incrementTurn();
                }
            }

            // 戦闘終了時にUIを通常モードに戻す
            jfxUi.setBattleMode(false);
            ui.showImage("enemy", ""); // 敵画像を消去（非表示にする）
        }
    }

    private boolean processPlayerTurn(BattleCommand cmd, EnemyData enemy) {
        // [1] ムーブ処理
        String move = cmd.getMove();
        if ("前進".equals(move)) {
            state.setDistance(Math.max(0, state.getDistance() - 1));
            ui.print("　プレイヤーは前進した。（現在距離: " + state.getDistance() + "）");
        } else if ("後退".equals(move)) {
            state.setDistance(Math.min(4, state.getDistance() + 1));
            ui.print("　プレイヤーは後退した。（現在距離: " + state.getDistance() + "）");
        }

        // [2] アクション処理
        String action = cmd.getAction();
        
        if ("逃げる".equals(action)) {
            // ダミー：今の所は無条件で成功とする
            ui.print("　戦闘から無事に逃走した！");
            return true; 
        }

        if ("攻撃".equals(action)) {
        	// 属性・依存ステータスの初期値 (近接・強靭)
            DistanceType distType = DistanceType.MELEE;
            StatType statType = StatType.MIGHT;

            // スタンスによる上書き処理
            String stance = cmd.getStance();
            if ("魔法攻撃".equals(stance)) {
                distType = DistanceType.SPECIAL;
                statType = StatType.INSIGHT;
                player.modifyAp(-1);
                ui.print("　魔法攻撃の構え！(APを1消費した)");
            } else if ("射撃の名手".equals(stance)) {
                distType = DistanceType.RANGED;
                statType = StatType.FINESSE;
                ui.print("　射撃の名手の構えをとった！");
            }

            // 属性と距離による確定ミスの判定
            if (isMissByDistance(distType, state.getDistance())) {
            	if(distType == DistanceType.MELEE) {
            		ui.print("　ミス！遠すぎて攻撃が届かない！");
            	} else {
            		ui.print("　ミス！近すぎて攻撃が当たらない！");
            	}
                return false;
            }

            // 命中判定 (自機敏 vs 敵機敏)
            int atkFinesse = player.getCombatStats().finesse();
            int defFinesse = enemy.getFinesse();

            if (checkHit(atkFinesse, defFinesse)) {
                // ダメージ計算（要求されたステータスの数値をそのままダメージにする）
                int baseDamage = 0;
                switch(statType) {
                    case MIGHT: baseDamage = player.getCombatStats().might(); break;
                    case FINESSE: baseDamage = player.getCombatStats().finesse(); break;
                    case INSIGHT: baseDamage = player.getCombatStats().insight(); break;
                    case PRESENCE: baseDamage = player.getCombatStats().presence(); break;
                    case SENSUALITY: baseDamage = player.getCombatStats().sensuality(); break;
                }

                // 距離によるダメージボーナス (+3)
                int bonus = getDistanceDamageBonus(distType, state.getDistance());
                int totalDamage = baseDamage + bonus;

                enemy.setHp(enemy.getHp() - totalDamage);
                
                String bonusMsg = bonus > 0 ? " (距離ボーナス+" + bonus + ")" : "";
                ui.print("　攻撃が命中！ " + enemy.getName() + " に " + totalDamage + " のダメージ！" + bonusMsg);
            } else {
                ui.print("　攻撃をかわされた！（ミス！）");
            }
        }
        return false;
    }

    private void processEnemyTurn(EnemyData enemy) {
        ui.print("＞敵の行動: [" + enemy.getName() + " の攻撃]");
        
        int distance = state.getDistance();
        
        // 簡単な敵AI：中・遠距離なら詰めてくる。近距離なら近接攻撃。
        if (distance >= 2) {
            state.setDistance(Math.max(0, distance - 1));
            ui.print("　" + enemy.getName() + " はにじり寄ってきた。（現在距離: " + state.getDistance() + "）");
            ui.print("　攻撃は届かなかった…");
        } else {
            int atkFinesse = enemy.getFinesse();
            int defFinesse = player.getCombatStats().finesse();
            
            if (checkHit(atkFinesse, defFinesse)) {
                int baseDmg = enemy.getMight();
                int bonus = getDistanceDamageBonus(DistanceType.MELEE, distance);
                int totalDamage = baseDmg + bonus;
                
                player.modifyHp(-totalDamage);
                
                String bonusMsg = bonus > 0 ? " (距離ボーナス+" + bonus + ")" : "";
                ui.print("　" + enemy.getName() + " の攻撃が命中！ プレイヤーに " + totalDamage + " のダメージ！" + bonusMsg);
            } else {
                ui.print("　プレイヤーは攻撃をかわした！（ミス！）");
            }
        }
    }

    // --- 各種計算用のヘルパーメソッド ---

    /**
     * 命中判定を行う（ベース60%、機敏差1〜3で10%、4〜5で20%、6以上で30%の修正。上限95%, 下限5%）
     */
    private boolean checkHit(int attackerFinesse, int defenderFinesse) {
        int diff = attackerFinesse - defenderFinesse;
        int hitChance = 60;

        if (diff >= 6) {
            hitChance += 30;
        } else if (diff >= 4) {
            hitChance += 20;
        } else if (diff >= 1) {
            hitChance += 10;
        } else if (diff <= -6) {
            hitChance -= 30;
        } else if (diff <= -4) {
            hitChance -= 20;
        } else if (diff <= -1) {
            hitChance -= 10;
        }

        // 上限下限
        hitChance = Math.max(5, Math.min(95, hitChance));
        
        // 1から100までのダイスロール
        int roll = random.nextInt(100) + 1;
        return roll <= hitChance;
    }

    /**
     * 距離によってその属性の攻撃が確定ミスになるかどうかを判定する
     */
    private boolean isMissByDistance(DistanceType type, int distance) {
    	// MELEE は 距離2以上 で届かない
        if (type == DistanceType.MELEE && distance >= 2) return true;
        
        // RANGED は 距離0 だと近すぎて攻撃できない
        if (type == DistanceType.RANGED && distance == 0) return true;
        
        // SPECIAL の場合は距離によるミス判定なし
        return false;
    }

    /**
     * 距離によってその属性の攻撃にダメージボーナス(+3)が入るかを判定する
     */
    private int getDistanceDamageBonus(DistanceType type, int distance) {
    	// MELEE は至近距離(0)でダメージボーナス
        if (type == DistanceType.MELEE && distance == 0) return 3;
        
        // RANGED は中・遠距離(2以上)でダメージボーナス
        if (type == DistanceType.RANGED && distance >= 2) return 3;
        
        // SPECIAL は距離によるボーナスなし
        return 0;
    }
}
