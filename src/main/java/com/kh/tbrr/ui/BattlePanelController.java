package com.kh.tbrr.ui;

import com.kh.tbrr.battle.BattleState;
import com.kh.tbrr.battle.EnemyData;
import com.kh.tbrr.data.CombatConditionRegistry;
import com.kh.tbrr.data.models.CombatConditionData;
import com.kh.tbrr.data.models.Player;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 戦闘中にサブウィンドウへ表示する情報UIパネルのコントローラー。
 * JavaFXUI から生成・更新される。
 *
 * レイアウト（450×450px）:
 *   ┌─────────────────────────┐
 *   │ ヘッダー（ターン・距離） │
 *   ├─────────────────────────┤
 *   │ 戦闘ログ（スクロール）  │
 *   ├────────────┬────────────┤
 *   │ プレイヤー │   敵       │
 *   │ ステータス │ ステータス │
 *   ├─────────────────────────┤
 *   │      距離マップ          │
 *   └─────────────────────────┘
 */
public class BattlePanelController {

    // ---- パネル全体 ----
    private VBox rootPane;

    // ---- ヘッダー行 ----
    private Label headerLabel;

    // ---- 戦闘ログエリア ----
    private TextArea logArea;

    // ---- ステータス比較エリア ----
    private Label playerNameLabel;
    private Label playerHpLabel;
    private Label playerApLabel;
    private Label playerSpLabel;
    private Label playerCondLabel;

    private Label enemyNameLabel;
    private Label enemyHpLabel;
    private Label enemySpLabel;
    private Label enemyCondLabel;

    // ---- 距離マップ ----
    private HBox distanceMapBox;
    /** 距離0〜4の各マス（Rectangle）*/
    private Rectangle[] distanceCells;
    /** プレイヤーシンボル（▽）*/
    private Polygon playerSymbol;
    /** 敵シンボル（○）*/
    private Circle enemySymbol;

    // 距離マップ定数
    private static final int MAP_CELL_COUNT = 5;
    private static final int CELL_W = 72;   // 5マス × 72 = 360px
    private static final int CELL_H = 50;
    private static final int SYMBOL_AREA_H = 36;

    // ---- 色定数 ----
    private static final String COLOR_BG           = "#1e1e1e";
    private static final String COLOR_HEADER_BG    = "#2a2a3a";
    private static final String COLOR_LOG_BG       = "#141414";
    private static final String COLOR_STATUS_BG    = "#1a1a2a";
    private static final String COLOR_MAP_BG       = "#0f0f1a";
    private static final String COLOR_CELL_NORMAL  = "#2d2d40";
    private static final String COLOR_CELL_ACTIVE  = "#3a3a55"; // プレイヤー/敵がいるマス
    private static final String COLOR_TEXT_HEAD    = "#d0d0ff";
    private static final String COLOR_TEXT_NORMAL  = "#cccccc";
    private static final String COLOR_HP           = "#ff6666";
    private static final String COLOR_AP           = "#6699ff";
    private static final String COLOR_SP           = "#66ddaa";
    private static final String COLOR_COND         = "#ffcc44";

    // ---- フォント ----
    private static final String FONT_FAMILY = "MS Gothic";

    /**
     * 生成済みのパネルを返す（装備画面から戻るときなど、再表示に使う）。
     * createPanel() を呼んでいない場合は null を返す。
     */
    public VBox getPanel() {
        return rootPane;
    }

    /**
     * パネルを生成して返す。
     * このメソッドは JavaFX Application Thread から呼ぶこと。
     */
    public VBox createPanel() {
        rootPane = new VBox(0);
        rootPane.setPrefSize(450, 450);
        rootPane.setMaxSize(450, 450);
        rootPane.setMinSize(450, 450);
        rootPane.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // --- ヘッダー ---
        VBox headerBox = buildHeader();
        VBox.setVgrow(headerBox, Priority.NEVER);

        // --- 戦闘ログ ---
        VBox logBox = buildLogArea();
        VBox.setVgrow(logBox, Priority.ALWAYS);

        // --- ステータス比較 ---
        HBox statusBox = buildStatusArea();
        VBox.setVgrow(statusBox, Priority.NEVER);

        // --- 距離マップ ---
        VBox mapBox = buildDistanceMap();
        VBox.setVgrow(mapBox, Priority.NEVER);

        rootPane.getChildren().addAll(headerBox, logBox, statusBox, mapBox);
        return rootPane;
    }

    // =============================================
    // --- ビルダー（初期レイアウト構築）--------
    // =============================================

    private VBox buildHeader() {
        VBox box = new VBox();
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle("-fx-background-color: " + COLOR_HEADER_BG + ";"
                + "-fx-border-color: #444466; -fx-border-width: 0 0 1 0;");
        box.setPrefHeight(34);
        box.setMinHeight(34);
        box.setMaxHeight(34);

        headerLabel = new Label("--- 戦闘開始 ---");
        headerLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        headerLabel.setStyle("-fx-text-fill: " + COLOR_TEXT_HEAD + ";");

        box.getChildren().add(headerLabel);
        return box;
    }

    private VBox buildLogArea() {
        VBox box = new VBox(0);
        box.setStyle("-fx-background-color: " + COLOR_LOG_BG + ";"
                + "-fx-border-color: #333355; -fx-border-width: 0 0 1 0;");
        box.setPrefHeight(140);
        box.setMinHeight(100);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFocusTraversable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font(FONT_FAMILY, 13));
        logArea.setStyle("-fx-control-inner-background: " + COLOR_LOG_BG + ";"
                + "-fx-text-fill: " + COLOR_TEXT_NORMAL + ";"
                + "-fx-border-width: 0;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        box.getChildren().add(logArea);
        return box;
    }

    private HBox buildStatusArea() {
        HBox box = new HBox(0);
        box.setStyle("-fx-background-color: " + COLOR_STATUS_BG + ";"
                + "-fx-border-color: #333355; -fx-border-width: 0 0 1 0;");
        box.setPrefHeight(130);
        box.setMinHeight(130);
        box.setMaxHeight(130);

        // プレイヤー側
        VBox playerBox = buildStatusColumn(true);
        HBox.setHgrow(playerBox, Priority.ALWAYS);

        // 区切り線
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setStyle("-fx-background-color: #444466;");

        // 敵側
        VBox enemyBox = buildStatusColumn(false);
        HBox.setHgrow(enemyBox, Priority.ALWAYS);

        box.getChildren().addAll(playerBox, divider, enemyBox);
        return box;
    }

    private VBox buildStatusColumn(boolean isPlayer) {
        VBox col = new VBox(3);
        col.setPadding(new Insets(6, 8, 6, 8));
        col.setStyle("-fx-background-color: transparent;");

        if (isPlayer) {
            playerNameLabel = makeLabel("〔プレイヤー〕", FONT_FAMILY, 13, COLOR_TEXT_HEAD);
            playerHpLabel   = makeLabel("HP: -/-", FONT_FAMILY, 14, COLOR_HP);
            playerApLabel   = makeLabel("AP: -/-", FONT_FAMILY, 14, COLOR_AP);
            playerSpLabel   = makeLabel("SP: -",   FONT_FAMILY, 14, COLOR_SP);
            playerCondLabel = makeLabel("状態: なし", FONT_FAMILY, 13, COLOR_COND);
            col.getChildren().addAll(
                    playerNameLabel, playerHpLabel, playerApLabel, playerSpLabel, playerCondLabel);
        } else {
            enemyNameLabel = makeLabel("〔敵〕", FONT_FAMILY, 13, COLOR_TEXT_HEAD);
            enemyHpLabel   = makeLabel("HP: -/-", FONT_FAMILY, 14, COLOR_HP);
            enemySpLabel   = makeLabel("SP: -",   FONT_FAMILY, 14, COLOR_SP);
            enemyCondLabel = makeLabel("状態: なし", FONT_FAMILY, 13, COLOR_COND);
            col.getChildren().addAll(
                    enemyNameLabel, enemyHpLabel, enemySpLabel, enemyCondLabel);
        }
        return col;
    }

    private VBox buildDistanceMap() {
        VBox outer = new VBox(2);
        outer.setPadding(new Insets(6, 8, 6, 8));
        outer.setStyle("-fx-background-color: " + COLOR_MAP_BG + ";");
        outer.setPrefHeight(136);
        outer.setMinHeight(136);
        outer.setMaxHeight(136);

        // シンボル行（▽と○を各マス中央の上に浮かせる）
        StackPane symbolLayer = buildSymbolLayer();

        // マス行
        HBox cellRow = buildCellRow();

        // 距離マップ全体のラベル（下部）
        Label distLabel = makeLabel("← 至近  0  1  2  3  4  遠方 →", FONT_FAMILY, 10, "#888899");
        distLabel.setPadding(new Insets(2, 0, 0, 0));

        outer.getChildren().addAll(symbolLayer, cellRow, distLabel);
        return outer;
    }

    private HBox buildCellRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);

        distanceCells = new Rectangle[MAP_CELL_COUNT];
        for (int i = 0; i < MAP_CELL_COUNT; i++) {
            Rectangle cell = new Rectangle(CELL_W, CELL_H);
            cell.setArcWidth(6);
            cell.setArcHeight(6);
            cell.setFill(Color.web(COLOR_CELL_NORMAL));
            cell.setStroke(Color.web("#44445a"));
            cell.setStrokeWidth(1);
            distanceCells[i] = cell;
            row.getChildren().add(cell);
        }
        return row;
    }

    private StackPane buildSymbolLayer() {
        // シンボル行：各マスの中央に▽か○を浮かせる
        // HBoxで各マスの幅に合わせて並べる
        HBox symbolRow = new HBox(4);
        symbolRow.setAlignment(Pos.BOTTOM_CENTER);
        symbolRow.setPrefHeight(SYMBOL_AREA_H);

        for (int i = 0; i < MAP_CELL_COUNT; i++) {
            StackPane slot = new StackPane();
            slot.setPrefWidth(CELL_W);
            slot.setMinWidth(CELL_W);
            slot.setMaxWidth(CELL_W);
            slot.setPrefHeight(SYMBOL_AREA_H);
            symbolRow.getChildren().add(slot);
        }

        // プレイヤーシンボル（逆三角形 ▽）
        playerSymbol = new Polygon(
            0.0, 0.0,
            22.0, 0.0,
            11.0, 20.0
        );
        playerSymbol.setFill(Color.web("#7799ff"));
        playerSymbol.setStroke(Color.web("#aabbff"));
        playerSymbol.setStrokeWidth(1.5);

        // 敵シンボル（○ 円錐代わりの楕円）
        enemySymbol = new Circle(10);
        enemySymbol.setFill(Color.web("#cc4444"));
        enemySymbol.setStroke(Color.web("#ff8888"));
        enemySymbol.setStrokeWidth(1.5);

        // 初期位置（距離1 = プレイヤー, 距離1 = 敵 ← 初期距離）
        // ※ updateDistanceMap() で正しく配置される
        // symbolRowの各スロットへの配置はupdateDistanceMap()に任せる

        // StackPane でシンボル行をラップ（位置更新で子要素を入れ替えるため）
        distanceMapBox = (HBox) symbolRow;
        StackPane wrapper = new StackPane(symbolRow);
        wrapper.setPrefHeight(SYMBOL_AREA_H);
        return wrapper;
    }

    // =============================================
    // --- 更新メソッド（BattleManager側から呼ばれる）---
    // =============================================

    /**
     * ヘッダー行（ターン数・距離）を更新する。
     * UIスレッド外から呼んでよい（内部でPlatform.runLater）。
     */
    public void updateHeader(int turn, int distance) {
        String distLabel = distanceLabel(distance);
        Platform.runLater(() -> {
            if (headerLabel != null) {
                headerLabel.setText("ターン " + turn + "  |  距離: " + distance + "（" + distLabel + "）");
            }
        });
    }

    /**
     * ログエリアに1行追記する。
     */
    public void appendLog(String message) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * ログエリアをクリアする（戦闘開始時）。
     */
    public void clearLog() {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.clear();
            }
        });
    }

    /**
     * プレイヤーのステータス欄を更新する。
     * @param player     プレイヤーデータ
     * @param conditions 現在の戦闘状態リスト（BattleState.ActiveCombatCondition）
     */
    public void updatePlayerStatus(Player player,
            List<BattleState.ActiveCombatCondition> conditions) {
        if (player == null) return;

        String name = player.getName() != null ? player.getName() : "冒険者";
        String hp   = player.getCurrentHP() + " / " + player.getMaxHP();
        String ap   = player.getCurrentAP() + " / " + player.getMaxAP();
        String sp   = String.valueOf(player.getCurrentSp());
        String cond = buildConditionText(conditions);

        Platform.runLater(() -> {
            if (playerNameLabel != null) {
                playerNameLabel.setText("〔" + name + "〕");
                playerHpLabel.setText("HP: " + hp);
                playerApLabel.setText("AP: " + ap);
                playerSpLabel.setText("SP: " + sp);
                playerCondLabel.setText("状態: " + cond);
            }
        });
    }

    /**
     * 敵のステータス欄を更新する。
     * @param enemy      敵データ
     * @param conditions 敵の現在の戦闘状態リスト
     */
    public void updateEnemyStatus(EnemyData enemy,
            List<BattleState.ActiveCombatCondition> conditions) {
        if (enemy == null) return;

        String name = enemy.getName() != null ? enemy.getName() : "???";
        String hp   = enemy.getHp() + " / " + enemy.getMaxHp();
        String sp   = String.valueOf(enemy.getCurrentSp());
        String cond = buildConditionText(conditions);

        Platform.runLater(() -> {
            if (enemyNameLabel != null) {
                enemyNameLabel.setText("〔" + name + "〕");
                enemyHpLabel.setText("HP: " + hp);
                enemySpLabel.setText("SP: " + sp);
                enemyCondLabel.setText("状態: " + cond);
            }
        });
    }

    /**
     * 距離マップを更新する（▽と○の位置を動かす）。
     * @param distance 現在の距離（0〜4）
     */
    public void updateDistanceMap(int distance) {
        int safeDistance = Math.max(0, Math.min(4, distance));

        Platform.runLater(() -> {
            if (distanceCells == null || distanceMapBox == null) return;

            // マスの色をリセット
            for (int i = 0; i < MAP_CELL_COUNT; i++) {
                distanceCells[i].setFill(Color.web(COLOR_CELL_NORMAL));
            }

            // 距離に対応するセル位置（ルックアップテーブル方式）
            // 距離 0: 中央重なり 距離 1: 隣接、距離2以降は1距離各1マスの隙間が増える
            //  距離 | プレイヤー | 敵 | 隙間
            //    0   |   2      |  2  |  重なり
            //    1   |   2      |  3  |  0（隣接）
            //    2   |   1      |  3  |  1
            //    3   |   1      |  4  |  2
            //    4   |   0      |  4  |  3（最大）
            int[] playerPositions = {2, 2, 1, 1, 0};
            int[] enemyPositions  = {2, 3, 3, 4, 4};
            int playerCell = playerPositions[safeDistance];
            int enemyCell  = enemyPositions[safeDistance];

            distanceCells[playerCell].setFill(Color.web(COLOR_CELL_ACTIVE));
            if (enemyCell != playerCell) {
                distanceCells[enemyCell].setFill(Color.web(COLOR_CELL_ACTIVE));
            }

            // シンボルをそれぞれのスロットに配置
            for (int i = 0; i < MAP_CELL_COUNT; i++) {
                StackPane slot = (StackPane) distanceMapBox.getChildren().get(i);
                slot.getChildren().clear();
            }
            StackPane playerSlot = (StackPane) distanceMapBox.getChildren().get(playerCell);
            StackPane enemySlot  = (StackPane) distanceMapBox.getChildren().get(enemyCell);

            playerSlot.getChildren().add(playerSymbol);
            if (enemyCell != playerCell) {
                enemySlot.getChildren().add(enemySymbol);
            } else {
                // 同じマスに居る場合は重ねて表示（HBox横並び）
                HBox sameCell = new HBox(4);
                sameCell.setAlignment(Pos.CENTER);
                sameCell.getChildren().addAll(playerSymbol, enemySymbol);
                playerSlot.getChildren().clear();
                playerSlot.getChildren().add(sameCell);
            }
        });
    }

    // =============================================
    // --- ユーティリティ --------------------
    // =============================================

    /** 距離数値を日本語ラベルに変換 */
    private String distanceLabel(int distance) {
        return switch (distance) {
            case 0 -> "至近";
            case 1 -> "近";
            case 2 -> "中";
            case 3 -> "遠";
            case 4 -> "超遠";
            default -> "?";
        };
    }

    /**
     * ActiveCombatCondition のリストを表示用テキストに変換する。
     * 例: "転倒(2T)  飛行(1T)"
     */
    private String buildConditionText(List<BattleState.ActiveCombatCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) return "なし";

        StringBuilder sb = new StringBuilder();
        for (BattleState.ActiveCombatCondition cond : conditions) {
            if (cond.getDuration() <= 0) continue; // 残ターン0は表示しない
            CombatConditionData data = CombatConditionRegistry.getConditionById(cond.getConditionId());
            String displayName = (data != null) ? data.getName() : cond.getConditionId();
            if (sb.length() > 0) sb.append("  ");
            if (cond.getDuration() == -1) {
                sb.append(displayName).append("(永)");
            } else {
                sb.append(displayName).append("(").append(cond.getDuration()).append("T)");
            }
        }
        return sb.length() > 0 ? sb.toString() : "なし";
    }

    /** ラベルを生成するユーティリティ */
    private Label makeLabel(String text, String fontFamily, int size, String color) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font(fontFamily, size));
        lbl.setStyle("-fx-text-fill: " + color + ";");
        lbl.setWrapText(false);
        return lbl;
    }
}
