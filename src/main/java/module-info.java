module com.kh.tbrr {
	// JavaFXモジュール
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.media;

	// Gson - 正確なモジュール名を使用
	requires com.google.gson;

	// JavaFXとGsonのために、すべてのパッケージを「名前のないモジュール」に公開
	// これにより、リフレクション（GsonのJSON変換など）が正しく動作します
	opens com.kh.tbrr to javafx.graphics;
	opens com.kh.tbrr.ui to javafx.fxml;
	opens com.kh.tbrr.core to javafx.fxml;
	opens com.kh.tbrr.manager to javafx.fxml;
	opens com.kh.tbrr.data.models;
	opens com.kh.tbrr.system;
	opens com.kh.tbrr.event;
	opens com.kh.tbrr.data;

	// パッケージのエクスポート
	exports com.kh.tbrr;
	exports com.kh.tbrr.ui;
	exports com.kh.tbrr.core;
	exports com.kh.tbrr.manager;
	exports com.kh.tbrr.system;
	exports com.kh.tbrr.data.models;
	exports com.kh.tbrr.event;
	exports com.kh.tbrr.data;
}
