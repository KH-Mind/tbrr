package com.kh.tbrr.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kh.tbrr.data.models.Player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 16パズル（スライドパズル）インタラクション
 * 4x4のタイルを移動させて完成させるパズル
 */
public class SlidePuzzleInteraction implements InteractionHandler {

    private static final String TYPE = "slide_puzzle";
    private static final int GRID_SIZE = 4;
    private static final int TILE_SIZE = 80;

    // UI参照
    private StackPane subWindowPane;

    // パズル状態
    private int[][] puzzle;
    private int emptyRow, emptyCol;
    private Button[][] tileButtons;
    private int moveCount;
    private boolean solved;
    private int maxMoves;

    // UI要素の参照（クラスフィールドとして保持）
    private VBox container;
    private Label infoLabel;
    private Label instructionLabel;
    private CompletableFuture<InteractionResult> currentFuture;
    private Timeline currentTimer; // タイマー参照（次回開始時にキャンセル用）

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public CompletableFuture<InteractionResult> execute(Map<String, Object> params, Player player) {
        CompletableFuture<InteractionResult> future = new CompletableFuture<>();
        currentFuture = future;

        // UI参照を取得
        if (params.containsKey("_subWindowPane")) {
            subWindowPane = (StackPane) params.get("_subWindowPane");
        }

        // パラメータから設定を取得
        maxMoves = 100; // デフォルト最大手数
        int timeLimit = 120; // デフォルト制限時間（秒）

        if (params.containsKey("maxMoves")) {
            Object val = params.get("maxMoves");
            if (val instanceof Number) {
                maxMoves = ((Number) val).intValue();
            }
        }
        if (params.containsKey("timeLimit")) {
            Object val = params.get("timeLimit");
            if (val instanceof Number) {
                timeLimit = ((Number) val).intValue();
            }
        }

        final int finalTimeLimit = timeLimit;
        final Map<String, Object> finalParams = params;

        Platform.runLater(() -> {
            showPuzzleUI(finalTimeLimit, future, finalParams);
        });

        return future;
    }

    /**
     * パズルUIを表示
     */
    @SuppressWarnings("unchecked")
    private void showPuzzleUI(int timeLimit,
            CompletableFuture<InteractionResult> future,
            Map<String, Object> params) {
        if (subWindowPane == null) {
            future.complete(new InteractionResult("failure"));
            return;
        }

        // 前回のタイマーが残っていればキャンセル
        if (currentTimer != null) {
            currentTimer.stop();
            currentTimer = null;
        }

        // InteractionUIHelperでコンテナを作成（背景画像対応）
        container = InteractionUIHelper.createContainer(params, subWindowPane, 5);

        // ヘッダー（1行にまとめる）
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("16パズル");
        titleLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);

        infoLabel = new Label("手数: 0/" + maxMoves);
        infoLabel.setFont(Font.font("Meiryo", 14));
        infoLabel.setTextFill(Color.LIGHTGRAY);

        Label timerLabel = new Label("残り " + timeLimit + " 秒");
        timerLabel.setFont(Font.font("Meiryo", 14));
        timerLabel.setTextFill(Color.LIGHTCORAL);

        headerBox.getChildren().addAll(titleLabel, infoLabel, timerLabel);

        // パズル初期化
        initPuzzle();
        shufflePuzzle();
        moveCount = 0;
        solved = false;

        // パズルグリッド
        GridPane grid = createPuzzleGrid();
        grid.setAlignment(Pos.CENTER);

        // 説明
        instructionLabel = new Label("タイルをクリックして移動");
        instructionLabel.setFont(Font.font("Meiryo", 12));
        instructionLabel.setTextFill(Color.LIGHTGRAY);

        container.getChildren().addAll(headerBox, grid, instructionLabel);

        // タイマー
        final int[] timeRemaining = { timeLimit };
        Timeline timer = new Timeline();
        timer.setCycleCount(timeLimit);
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining[0]--;
            timerLabel.setText("残り " + timeRemaining[0] + " 秒");

            if (timeRemaining[0] <= 10) {
                timerLabel.setTextFill(Color.RED);
            }
        }));

        timer.setOnFinished(e -> {
            if (!solved) {
                showResult(false, "時間切れ");
            }
        });

        // ○ボタン用入力コールバック（十字キーで操作）
        if (params != null && params.containsKey("_inputCallback")) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>> callbackRef = (java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<String>>) params
                    .get("_inputCallback");
            callbackRef.set(input -> {
                Platform.runLater(() -> {
                    switch (input) {
                        case "UP":
                            moveTile(emptyRow + 1, emptyCol);
                            break;
                        case "DOWN":
                            moveTile(emptyRow - 1, emptyCol);
                            break;
                        case "LEFT":
                            moveTile(emptyRow, emptyCol + 1);
                            break;
                        case "RIGHT":
                            moveTile(emptyRow, emptyCol - 1);
                            break;
                    }
                });
            });
        }

        // タイマーを保持してからスタート
        currentTimer = timer;
        timer.play();
    }

    /**
     * パズル初期化（正解状態）
     */
    private void initPuzzle() {
        puzzle = new int[GRID_SIZE][GRID_SIZE];
        int num = 1;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (row == GRID_SIZE - 1 && col == GRID_SIZE - 1) {
                    puzzle[row][col] = 0; // 空白
                    emptyRow = row;
                    emptyCol = col;
                } else {
                    puzzle[row][col] = num++;
                }
            }
        }
    }

    /**
     * パズルをシャッフル（解ける状態を保証）
     */
    private void shufflePuzzle() {
        // 正解状態から逆向きに移動してシャッフル（必ず解ける）
        java.util.Random rand = new java.util.Random();
        int[] dr = { -1, 1, 0, 0 };
        int[] dc = { 0, 0, -1, 1 };

        for (int i = 0; i < 100; i++) {
            List<Integer> validMoves = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nr = emptyRow + dr[d];
                int nc = emptyCol + dc[d];
                if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                    validMoves.add(d);
                }
            }
            int move = validMoves.get(rand.nextInt(validMoves.size()));
            int nr = emptyRow + dr[move];
            int nc = emptyCol + dc[move];
            puzzle[emptyRow][emptyCol] = puzzle[nr][nc];
            puzzle[nr][nc] = 0;
            emptyRow = nr;
            emptyCol = nc;
        }
    }

    /**
     * パズルグリッドを作成
     */
    private GridPane createPuzzleGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setPadding(new Insets(5));
        grid.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 5;");

        tileButtons = new Button[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                // 正解の数字（1-15、右下は16）
                int expectedNum = row * GRID_SIZE + col + 1;

                // StackPaneで背景ラベルとボタンを重ねる
                StackPane tilePane = new StackPane();
                tilePane.setPrefSize(TILE_SIZE, TILE_SIZE);

                // 背景に薄く正解数字を表示
                Label bgLabel = new Label(String.valueOf(expectedNum));
                bgLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 20));
                bgLabel.setTextFill(Color.rgb(255, 255, 255, 0.2));

                // タイルボタン
                Button btn = createTileButton(row, col);
                tileButtons[row][col] = btn;

                tilePane.getChildren().addAll(bgLabel, btn);
                grid.add(tilePane, col, row);
            }
        }

        return grid;
    }

    /**
     * タイルボタンを作成
     */
    private Button createTileButton(int row, int col) {
        int value = puzzle[row][col];
        Button btn = new Button();
        btn.setPrefSize(TILE_SIZE, TILE_SIZE);
        btn.setMinSize(TILE_SIZE, TILE_SIZE);
        btn.setMaxSize(TILE_SIZE, TILE_SIZE);
        btn.setFont(Font.font("Meiryo", FontWeight.BOLD, 24));

        if (value == 0) {
            // 空白マス（透明、背景ラベルが見える）
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent;");
            btn.setDisable(true);
        } else {
            btn.setText(String.valueOf(value));
            btn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-background-radius: 5;");
            final int r = row, c = col;
            btn.setOnAction(e -> moveTile(r, c));
        }

        return btn;
    }

    /**
     * タイルを移動
     */
    private void moveTile(int row, int col) {
        if (solved)
            return;
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE)
            return;

        // 空白と隣接しているかチェック
        if ((Math.abs(row - emptyRow) == 1 && col == emptyCol) ||
                (Math.abs(col - emptyCol) == 1 && row == emptyRow)) {

            // 移動
            puzzle[emptyRow][emptyCol] = puzzle[row][col];
            puzzle[row][col] = 0;

            // ボタンの更新
            updateTileButton(emptyRow, emptyCol, puzzle[emptyRow][emptyCol]);
            updateTileButton(row, col, 0);

            emptyRow = row;
            emptyCol = col;
            moveCount++;

            infoLabel.setText("手数: " + moveCount + "/" + maxMoves);

            // クリア判定
            if (isSolved()) {
                solved = true;
                showResult(true, "");
            } else if (moveCount >= maxMoves) {
                solved = true;
                showResult(false, "手数オーバー");
            }
        }
    }

    /**
     * タイルボタンを更新
     */
    private void updateTileButton(int row, int col, int value) {
        Button btn = tileButtons[row][col];

        if (value == 0) {
            // 空白マス（透明、背景ラベルが見える）
            btn.setText("");
            btn.setStyle("-fx-background-color: transparent;");
            btn.setDisable(true);
            btn.setOnAction(null);
        } else {
            btn.setText(String.valueOf(value));
            btn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-background-radius: 5;");
            btn.setDisable(false);
            final int r = row, c = col;
            btn.setOnAction(e -> moveTile(r, c));
        }
    }

    /**
     * パズルが完成したかチェック
     */
    private boolean isSolved() {
        int expected = 1;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (row == GRID_SIZE - 1 && col == GRID_SIZE - 1) {
                    if (puzzle[row][col] != 0)
                        return false;
                } else {
                    if (puzzle[row][col] != expected++)
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * 結果を表示
     */
    private void showResult(boolean success, String reason) {
        // タイマーを停止
        if (currentTimer != null) {
            currentTimer.stop();
            currentTimer = null;
        }

        String resultKey = success ? "success" : "failure";

        // 失敗理由に応じたメッセージ
        String failureMessage = reason.isEmpty() ? "失敗..." : reason + "...";
        Label resultLabel = new Label(success ? "完成！" : failureMessage);
        resultLabel.setFont(Font.font("Meiryo", FontWeight.BOLD, 28));
        resultLabel.setTextFill(success ? Color.LIMEGREEN : Color.TOMATO);

        if (instructionLabel != null) {
            instructionLabel.setText(moveCount + " 手で" + (success ? "クリア" : "終了"));
        }

        if (container != null) {
            container.getChildren().add(resultLabel);
        }

        // 少し待ってから結果を返す
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
            if (currentFuture != null) {
                currentFuture.complete(new InteractionResult(resultKey));
            }
        }));
        delay.play();
    }
}
