package com.kh.tbrr.interaction;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * ピックロック解錠インタラクション
 *
 * <p>
 * 錠前内部のピン（縦バー）が正弦波で上下に動き続ける。
 * クリックまたはスペースキーで「今の高さに固定」を試みる。
 * SHEAR LINE付近の成功ゾーンに入っていれば固定成功、外れていればピック消耗。
 * 全ピン固定で解錠成功、ピック切れで失敗。
 *
 * <p>
 * JSONパラメータ:
 * <ul>
 * <li>difficulty : 難易度（"easy" / "normal" / "hard"、省略時は "normal"）</li>
 * </ul>
 *
 * <p>
 * 返す結果キー: "success" / "failure"
 */
public class LockPickInteraction implements InteractionHandler {

    private static final String TYPE = "lock_pick";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player) {
        return new LockPickSession(params).start();
    }

    // =========================================================================
    // セッションクラス
    // ハンドラ本体（シングルトン）に状態を残さないための使い捨てクラス
    // =========================================================================

    private static class LockPickSession {

        private final Map<String, Object> params;
        private final CompletableFuture<InteractionResult> future;

        // UI参照
        private StackPane subWindowPane;
        private VBox container;
        private Canvas lockCanvas;
        private GraphicsContext gc;
        private Label pickLabel;
        private Label statusLabel;
        private Runnable cleanupAction; // キーボードフィルター解除用

        // ゲーム設定（JSONパラメータで上書き可）
        private int pinCount;
        private int pickCount;
        private int remainingPicks;

        // ピン状態
        private double[] pinY; // 各ピンの中心Y座標（ピクセル）
        private double[] pinSpeed; // 各ピンの振動速度（rad/sec）
        private double[] pinPhase; // 各ピンの初期位相（rad）
        private boolean[] pinFixed; // 固定済みフラグ
        private int currentPin; // 現在操作中のピン番号
        private boolean gameOver;

        // =========================================================================
        // 難易度プリセット
        // =========================================================================

        /**
         * 難易度プリセット。JSONの "difficulty" キーで選択される。
         * pinCount : ピン本数
         * pickCount : ピック本数（失敗できる回数）
         * baseSpeed : ピン速度の基準値（rad/sec）
         * successRadius : 成功ゾーン半径（±px）
         */
        private enum Difficulty {
            EASY(3, 5, 0.65, 28),
            NORMAL(4, 3, 1.00, 20),
            HARD(5, 2, 1.50, 13);

            final int pinCount;
            final int pickCount;
            final double baseSpeed;
            final double successRadius;

            Difficulty(int pinCount, int pickCount, double baseSpeed, double successRadius) {
                this.pinCount = pinCount;
                this.pickCount = pickCount;
                this.baseSpeed = baseSpeed;
                this.successRadius = successRadius;
            }

            /** paramsの "difficulty" キーから対応する enum を返す（未指定/不明はNORMAL） */
            static Difficulty fromParams(Map<String, Object> params) {
                Object val = params.get("difficulty");
                if (val instanceof String) {
                    switch (((String) val).toLowerCase()) {
                        case "easy":
                            return EASY;
                        case "hard":
                            return HARD;
                        default:
                            return NORMAL;
                    }
                }
                return NORMAL;
            }
        }

        // キャンバスレイアウト定数
        private static final double CANVAS_W = 400; // キャンバス幅
        private static final double CANVAS_H = 280; // キャンバス高さ
        private static final double PIN_WIDTH = 28; // ピン幅（px）
        private static final double PIN_HEIGHT = 52; // ピン高さ（px）
        private static final double SHEAR_Y = 140; // シアーライン（固定ターゲット）Y座標
        private static final double TRAVEL = 72; // ピン移動振幅（±px）

        // 難易度プリセットから設定されるパラメータ（インスタンスごとに異なる）
        private double successRadius; // 成功ゾーン半径（±px）
        private double baseSpeed; // ピン速度の基準値（rad/sec）

        // アニメーション
        private Timeline gameLoop;
        private double time; // 経過時間（秒）
        private double flashTimer; // フラッシュエフェクト残り時間（秒）
        private boolean flashOK; // trueなら成功フラッシュ、falseなら失敗フラッシュ

        // キーボードフィルター参照（クリーンアップ時に解除するため保持）
        private final AtomicReference<javafx.event.EventHandler<KeyEvent>> keyFilterRef = new AtomicReference<>();
        private final AtomicReference<javafx.beans.value.ChangeListener<javafx.scene.Scene>> sceneListenerRef = new AtomicReference<>();

        LockPickSession(Map<String, Object> params) {
            this.params = params;
            this.future = new CompletableFuture<>();

            if (params.containsKey("_subWindowPane")) {
                this.subWindowPane = (StackPane) params.get("_subWindowPane");
            }

            // 難易度プリセットを読み込んでパラメータを設定
            Difficulty diff = Difficulty.fromParams(params);
            this.pinCount = diff.pinCount;
            this.pickCount = diff.pickCount;
            this.baseSpeed = diff.baseSpeed;
            this.successRadius = diff.successRadius;
            this.remainingPicks = pickCount;
        }

        CompletableFuture<InteractionResult> start() {
            Platform.runLater(this::showUI);
            return future;
        }

        // =====================================================================
        // UIの構築
        // =====================================================================

        @SuppressWarnings("unchecked")
        private void showUI() {
            if (subWindowPane == null) {
                future.complete(new InteractionResult("failure"));
                return;
            }

            // InteractionUIHelperでコンテナを作成（背景画像対応）
            container = InteractionUIHelper.createContainer(params, subWindowPane, 10);
            container.setAlignment(Pos.CENTER);

            // タイトル行
            HBox titleRow = new HBox(20);
            titleRow.setAlignment(Pos.CENTER);

            Label titleLabel = new Label("LOCK PICK");
            titleLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
            titleLabel.setTextFill(Color.LIGHTGREEN);
            titleLabel.setStyle("-fx-effect: dropshadow(gaussian, lime, 10, 0.5, 0, 0);");

            pickLabel = new Label(buildPickString());
            pickLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
            pickLabel.setTextFill(Color.LIGHTYELLOW);

            titleRow.getChildren().addAll(titleLabel, pickLabel);

            // 錠前Canvas（クリックで操作）
            lockCanvas = new Canvas(CANVAS_W, CANVAS_H);
            gc = lockCanvas.getGraphicsContext2D();
            lockCanvas.setOnMouseClicked(e -> attemptFix());

            // ステータスラベル
            statusLabel = new Label("クリック または スペースキーで固定を試みる");
            statusLabel.setFont(Font.font("Meiryo", 13));
            statusLabel.setTextFill(Color.LIGHTGRAY);

            container.getChildren().addAll(titleRow, lockCanvas, statusLabel);

            // ピン初期化
            initPins();

            // キーボードフィルタークリーンアップ定義
            cleanupAction = () -> {
                if (sceneListenerRef.get() != null) {
                    container.sceneProperty().removeListener(sceneListenerRef.get());
                }
                if (keyFilterRef.get() != null && container.getScene() != null) {
                    container.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyFilterRef.get());
                }
            };

            // キーボード入力フィルター（スペース/Enter/Z/テンキー5 で attemptFix）
            javafx.event.EventHandler<KeyEvent> keyFilter = e -> {
                if (gameOver)
                    return;
                switch (e.getCode()) {
                    case SPACE:
                    case ENTER:
                    case Z:
                    case NUMPAD5:
                        Platform.runLater(this::attemptFix);
                        e.consume();
                        break;
                    default:
                        break;
                }
            };

            javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = (obs, oldS, newS) -> {
                if (oldS != null)
                    oldS.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                if (newS != null)
                    newS.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            };
            container.sceneProperty().addListener(sceneListener);
            if (container.getScene() != null) {
                container.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            }
            keyFilterRef.set(keyFilter);
            sceneListenerRef.set(sceneListener);

            // ○ボタン（コントローラー）対応
            if (params.containsKey("_inputCallback")) {
                AtomicReference<Consumer<String>> cbRef = (AtomicReference<Consumer<String>>) params
                        .get("_inputCallback");
                cbRef.set(input -> {
                    if ("ACTION".equals(input)) {
                        Platform.runLater(this::attemptFix);
                    }
                });
            }

            // ゲームループ（約60fps、16ms間隔）
            gameLoop = new Timeline();
            gameLoop.setCycleCount(Timeline.INDEFINITE);
            gameLoop.getKeyFrames().add(new KeyFrame(Duration.millis(16), e -> {
                if (!gameOver) {
                    time += 0.016;
                    if (flashTimer > 0)
                        flashTimer -= 0.016;
                    updatePins();
                    drawLock();
                }
            }));
            gameLoop.play();
        }

        // =====================================================================
        // ゲームロジック
        // =====================================================================

        /** ピンの初期状態をランダム生成 */
        private void initPins() {
            Random rand = new Random();
            pinY = new double[pinCount];
            pinSpeed = new double[pinCount];
            pinPhase = new double[pinCount];
            pinFixed = new boolean[pinCount];
            currentPin = 0;
            gameOver = false;
            time = 0;
            flashTimer = 0;

            for (int i = 0; i < pinCount; i++) {
                // 後半のピンほど少し速い（難易度感の演出）
                // baseSpeedは難易度プリセットから設定される
                pinSpeed[i] = baseSpeed + rand.nextDouble() * 0.4 + i * 0.12;
                pinPhase[i] = rand.nextDouble() * Math.PI * 2;
                pinY[i] = SHEAR_Y;
            }
        }

        /** 毎フレーム：ピンのY座標を正弦波で更新 */
        private void updatePins() {
            for (int i = 0; i < pinCount; i++) {
                if (!pinFixed[i]) {
                    pinY[i] = SHEAR_Y + Math.sin(time * pinSpeed[i] + pinPhase[i]) * TRAVEL;
                }
            }
        }

        /**
         * 固定を試みる（クリック / スペースキー / ○ボタン 時に呼ばれる）
         *
         * <p>
         * 現在のピンのY座標がSHEAR_Y ± successRadius の範囲内なら固定成功、
         * 範囲外なら失敗してピックを１本消耗する。
         */
        private void attemptFix() {
            if (gameOver || currentPin >= pinCount)
                return;

            boolean inZone = Math.abs(pinY[currentPin] - SHEAR_Y) <= successRadius;

            if (inZone) {
                // 成功：ピンを固定
                pinFixed[currentPin] = true;
                pinY[currentPin] = SHEAR_Y;
                flashTimer = 0.3;
                flashOK = true;
                statusLabel.setText("カチッ！");
                statusLabel.setTextFill(Color.LIMEGREEN);
                currentPin++;

                if (currentPin >= pinCount) {
                    // 全ピン固定 → 解錠成功
                    gameOver = true;
                    finishGame(true);
                } else {
                    // 少し待ってからメッセージを元に戻す
                    new Timeline(new KeyFrame(Duration.millis(500), e -> {
                        if (!gameOver) {
                            statusLabel.setText("クリック または スペースキーで固定を試みる");
                            statusLabel.setTextFill(Color.LIGHTGRAY);
                        }
                    })).play();
                }

            } else {
                // 失敗：ピック消耗
                remainingPicks--;
                flashTimer = 0.4;
                flashOK = false;
                pickLabel.setText(buildPickString());
                statusLabel.setText("外れた...  残りピック: " + remainingPicks);
                statusLabel.setTextFill(Color.TOMATO);

                if (remainingPicks <= 0) {
                    // ピック切れ → 失敗
                    gameOver = true;
                    finishGame(false);
                } else {
                    new Timeline(new KeyFrame(Duration.millis(800), e -> {
                        if (!gameOver) {
                            statusLabel.setText("クリック または スペースキーで固定を試みる");
                            statusLabel.setTextFill(Color.LIGHTGRAY);
                        }
                    })).play();
                }
            }
        }

        /** ゲーム終了処理（成功・失敗共通） */
        private void finishGame(boolean success) {
            if (gameLoop != null)
                gameLoop.stop();
            if (cleanupAction != null)
                cleanupAction.run();
            drawLock(); // 最終状態（全ピン固定等）を確実に1フレーム描画

            String msg = success ? "解錠成功！" : "ピックが折れた...";
            Color color = success ? Color.LIMEGREEN : Color.TOMATO;
            statusLabel.setText(msg);
            statusLabel.setTextFill(color);
            statusLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 18));

            String resultKey = success ? "success" : "failure";
            new Timeline(new KeyFrame(Duration.millis(1500), e -> future.complete(new InteractionResult(resultKey))))
                    .play();
        }

        /** ピック残数を ●○ で表現 */
        private String buildPickString() {
            StringBuilder sb = new StringBuilder("  Pick: ");
            for (int i = 0; i < pickCount; i++) {
                sb.append(i < remainingPicks ? "●" : "○");
            }
            return sb.toString();
        }

        // =====================================================================
        // 描画
        // =====================================================================

        /**
         * 毎フレーム：Canvasに錠前UIを描画
         *
         * <p>
         * 描画構造:
         * <ol>
         * <li>錠前外枠（角丸矩形）</li>
         * <li>各ピンのチャネル（暗い縦溝）+ 成功ゾーン（緑帯）</li>
         * <li>シアーライン（水平破線）</li>
         * <li>ピン本体（色で状態を表現）</li>
         * <li>現在ピンの▼インジケーター</li>
         * <li>フラッシュエフェクト（成功=緑 / 失敗=赤）</li>
         * </ol>
         */
        private void drawLock() {
            gc.clearRect(0, 0, CANVAS_W, CANVAS_H);

            // 錠前外枠
            gc.setFill(Color.rgb(32, 35, 48));
            gc.fillRoundRect(8, 8, CANVAS_W - 16, CANVAS_H - 16, 14, 14);
            gc.setStroke(Color.rgb(85, 90, 115));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(8, 8, CANVAS_W - 16, CANVAS_H - 16, 14, 14);

            // ピン配置計算
            double areaLeft = 30;
            double areaRight = CANVAS_W - 30;
            double pinTop = 22;
            double pinBot = CANVAS_H - 22;
            double spacing = (areaRight - areaLeft) / (pinCount + 1);

            // チャネル（ピンの通路）と成功ゾーン帯を描画
            for (int i = 0; i < pinCount; i++) {
                double cx = areaLeft + spacing * (i + 1);
                double left = cx - PIN_WIDTH / 2;

                // チャネル背景（暗い溝）
                gc.setFill(Color.rgb(12, 13, 20));
                gc.fillRect(left, pinTop, PIN_WIDTH, pinBot - pinTop);

                // 成功ゾーン帯（固定済みでなければ表示）
                if (!pinFixed[i]) {
                    gc.setFill(Color.rgb(0, 210, 80, 0.18));
                    gc.fillRect(left, SHEAR_Y - successRadius, PIN_WIDTH, successRadius * 2);
                }

                // チャネル枠
                gc.setStroke(Color.rgb(55, 58, 75));
                gc.setLineWidth(1.0);
                gc.strokeRect(left, pinTop, PIN_WIDTH, pinBot - pinTop);
            }

            // シアーライン（水平破線）
            gc.setStroke(Color.rgb(200, 200, 200, 0.55));
            gc.setLineWidth(1.5);
            gc.setLineDashes(7, 4);
            gc.strokeLine(areaLeft, SHEAR_Y, areaRight, SHEAR_Y);
            gc.setLineDashes(0);

            // ピン本体を描画（色で状態を表現）
            for (int i = 0; i < pinCount; i++) {
                double cx = areaLeft + spacing * (i + 1);
                double left = cx - PIN_WIDTH / 2;
                double py = pinY[i]; // ピン中心Y座標

                if (pinFixed[i]) {
                    // 固定済み：緑（静止）
                    gc.setFill(Color.rgb(30, 195, 95));
                    gc.fillRoundRect(left + 2, py - PIN_HEIGHT / 2, PIN_WIDTH - 4, PIN_HEIGHT, 7, 7);
                    // 中央に固定インジケーター（白バー）
                    gc.setFill(Color.rgb(255, 255, 255, 0.55));
                    gc.fillRect(left + 6, py - 2, PIN_WIDTH - 12, 4);

                } else if (i == currentPin) {
                    // 現在の操作対象ピン：色で状態を示す
                    boolean inZone = Math.abs(py - SHEAR_Y) <= successRadius;

                    Color base;
                    if (flashTimer > 0) {
                        base = flashOK ? Color.LIMEGREEN : Color.TOMATO;
                    } else if (inZone) {
                        base = Color.rgb(255, 210, 30); // ゴールド（成功ゾーン内）
                    } else {
                        base = Color.rgb(55, 125, 215); // 青（通常）
                    }

                    // グロー（外側の光彩）
                    gc.setFill(Color.color(
                            base.getRed(), base.getGreen(), base.getBlue(), 0.22));
                    gc.fillRoundRect(left - 4, py - PIN_HEIGHT / 2 - 4,
                            PIN_WIDTH + 8, PIN_HEIGHT + 8, 10, 10);

                    // ピン本体
                    gc.setFill(base);
                    gc.fillRoundRect(left + 2, py - PIN_HEIGHT / 2, PIN_WIDTH - 4, PIN_HEIGHT, 7, 7);

                    // 中央インジケーター（操作中）
                    gc.setFill(Color.rgb(255, 255, 255, 0.45));
                    gc.fillRect(left + 6, py - 2, PIN_WIDTH - 12, 4);

                } else {
                    // 未対象：グレー
                    gc.setFill(Color.rgb(68, 70, 84));
                    gc.fillRoundRect(left + 2, py - PIN_HEIGHT / 2, PIN_WIDTH - 4, PIN_HEIGHT, 7, 7);
                }

                // ピン番号（チャネル下端に小さく表示）
                gc.setFill(Color.rgb(140, 140, 160, 0.50));
                gc.setFont(Font.font("Monospaced", 9));
                gc.fillText(String.valueOf(i + 1), cx - 3, pinBot - 4);
            }

            // 現在ピンの上端に▼インジケーター（三角形）
            if (!gameOver && currentPin < pinCount) {
                double cx = areaLeft + spacing * (currentPin + 1);
                double[] xs = { cx - 6, cx + 6, cx };
                double[] ys = { pinTop + 2, pinTop + 2, pinTop + 10 };
                gc.setFill(Color.LIGHTCYAN);
                gc.fillPolygon(xs, ys, 3);
            }

            // フラッシュ演出（成功=緑 / 失敗=赤 で画面全体を染める）
            if (flashTimer > 0) {
                double alpha = Math.min(flashTimer * 1.4, 0.28);
                if (flashOK) {
                    gc.setFill(Color.rgb(0, 255, 100, alpha));
                } else {
                    gc.setFill(Color.rgb(255, 55, 55, alpha));
                }
                gc.fillRoundRect(8, 8, CANVAS_W - 16, CANVAS_H - 16, 14, 14);
            }
        }
    }
}
