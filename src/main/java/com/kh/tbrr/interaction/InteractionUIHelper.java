package com.kh.tbrr.interaction;

import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * インタラクション用ユーティリティ
 * 背景画像の読み込みやコンテナ作成など共通処理を提供
 */
public class InteractionUIHelper {

    private static final int WINDOW_SIZE = 450;
    private static final String DEFAULT_BG_COLOR = "#333333";

    /**
     * インタラクション用のコンテナを作成
     * 背景画像があれば表示、なければ単色背景
     * 
     * @param params        インタラクションパラメータ
     * @param subWindowPane サブウィンドウペイン
     * @return 作成されたVBoxコンテナ
     */
    public static VBox createContainer(Map<String, Object> params, StackPane subWindowPane) {
        return createContainer(params, subWindowPane, 20);
    }

    /**
     * インタラクション用のコンテナを作成（間隔指定）
     * 
     * @param params        インタラクションパラメータ
     * @param subWindowPane サブウィンドウペイン
     * @param spacing       VBoxの間隔
     * @return 作成されたVBoxコンテナ
     */
    public static VBox createContainer(Map<String, Object> params, StackPane subWindowPane, int spacing) {
        // サブウィンドウをクリア
        subWindowPane.getChildren().clear();

        // 背景画像があれば表示
        String backgroundImage = getBackgroundImage(params);
        System.out.println("[InteractionUIHelper] backgroundImage パラメータ: " + backgroundImage);
        if (backgroundImage != null) {
            ImageView bgImageView = loadBackgroundImage(backgroundImage);
            System.out.println("[InteractionUIHelper] 画像読み込み結果: " + (bgImageView != null ? "成功" : "失敗"));
            if (bgImageView != null) {
                subWindowPane.getChildren().add(bgImageView);
            }
        }

        // メインコンテナ
        VBox container = new VBox(spacing);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(WINDOW_SIZE, WINDOW_SIZE);

        // 背景画像がない場合のみ背景色を設定
        if (backgroundImage == null) {
            container.setStyle("-fx-background-color: " + DEFAULT_BG_COLOR + ";");
        } else {
            // 背景画像がある場合は透明（画像を見せる）
            container.setStyle("-fx-background-color: transparent;");
        }

        subWindowPane.getChildren().add(container);
        return container;
    }

    /**
     * パラメータから背景画像パスを取得
     */
    public static String getBackgroundImage(Map<String, Object> params) {
        if (params == null)
            return null;
        Object bg = params.get("backgroundImage");
        if (bg instanceof String && !((String) bg).isEmpty()) {
            return (String) bg;
        }
        return null;
    }

    /**
     * 背景画像を読み込む
     */
    public static ImageView loadBackgroundImage(String imagePath) {
        try {
            // interaction_imagesフォルダから読み込み
            String fullPath = "/data/images/interaction_images/" + imagePath;
            var resource = InteractionUIHelper.class.getResourceAsStream(fullPath);

            if (resource == null) {
                // 直接パスを試す（フルパスが指定されている場合）
                if (imagePath.startsWith("/")) {
                    resource = InteractionUIHelper.class.getResourceAsStream(imagePath);
                }
            }

            if (resource != null) {
                Image image = new Image(resource);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(WINDOW_SIZE);
                imageView.setFitHeight(WINDOW_SIZE);
                imageView.setPreserveRatio(false);
                return imageView;
            } else {
                System.err.println("[InteractionUIHelper] 背景画像が見つかりません: " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("[InteractionUIHelper] 背景画像読み込みエラー: " + e.getMessage());
        }
        return null;
    }

    /**
     * ウィンドウサイズを取得
     */
    public static int getWindowSize() {
        return WINDOW_SIZE;
    }
}
