package com.kh.tbrr.manager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.image.Image;

/**
 * 画像リソースを管理するクラス
 * 立ち絵、背景、イベント挿絵などの画像を読み込む
 * JAR化にも対応
 */
public class ImageManager {

	// 画像キャッシュ
	private Map<String, Image> imageCache = new HashMap<>();

	// Randomインスタンス
	private Random random = new Random();

	// 画像のベースパス
	private static final String PORTRAITS_BASE_PATH = "/data/images/portraits/";
	private static final String BG_BASE_PATH = "/data/images/bg/";
	private static final String EVENT_IMAGES_BASE_PATH = "/data/images/event_images/";

	/**
	 * 立ち絵を読み込む
	 * 
	 * @param fileName ファイル名（拡張子含む）
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	public Image loadPortrait(String fileName) {
		return loadImage(PORTRAITS_BASE_PATH + fileName);
	}

	/**
	 * 背景画像を読み込む
	 * 
	 * @param fileName ファイル名（拡張子含む）
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	public Image loadBackground(String fileName) {
		return loadImage(BG_BASE_PATH + fileName);
	}

	/**
	 * イベント挿絵を読み込む
	 * 
	 * @param fileName ファイル名（拡張子含む）
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	public Image loadEventImage(String fileName) {
		return loadImage(EVENT_IMAGES_BASE_PATH + fileName);
	}

	/**
	 * 利用可能な基本立ち絵のリストを取得
	 * *.base.* という命名規則のファイルのみを返す
	 * 例: warrior.base.png, mage.base.jpg など
	 * 
	 * @return 基本立ち絵のファイル名リスト
	 */
	public List<String> getAvailableBasePortraits() {
	    List<String> portraits = new ArrayList<>();
	    
	    try {
	        // ビルド時に自動生成されたリストを読み込み
	        InputStream is = getClass().getResourceAsStream(PORTRAITS_BASE_PATH + "portrait_list.txt");
	        
	        if (is != null) {
	            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	            String line;
	            while ((line = reader.readLine()) != null) {
	                line = line.trim();
	                if (!line.isEmpty()) {
	                    portraits.add(line);
	                }
	            }
	            reader.close();
	        }
	        
	    } catch (Exception e) {
	        System.err.println("[ImageManager] 立ち絵リスト取得エラー: " + e.getMessage());
	        e.printStackTrace();
	    }
	    
	    return portraits;
	}

	/**
	 * Classpathから基本立ち絵のリストを取得（代替方法）
	 * 
	 * @return 基本立ち絵のファイル名リスト
	 */
	private List<String> getAvailableBasePortraitsFromClasspath() {
		List<String> portraits = new ArrayList<>();
		
		try {
			// Classpathから直接ディレクトリを探索
			URL url = getClass().getResource(PORTRAITS_BASE_PATH);
			if (url != null) {
				// file:// プロトコルの場合（開発環境）
				if (url.getProtocol().equals("file")) {
					java.io.File dir = new java.io.File(url.toURI());
					if (dir.exists() && dir.isDirectory()) {
						java.io.File[] files = dir.listFiles();
						if (files != null) {
							for (java.io.File file : files) {
								String name = file.getName();
								if (name.contains(".base.") && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
									portraits.add(name);
								}
							}
						}
					}
				}
				// jar:// プロトコルの場合（JAR内）
				else if (url.getProtocol().equals("jar")) {
					// JARファイル内のリソースを列挙
					String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
					java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"));
					java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
					
					String basePath = PORTRAITS_BASE_PATH.substring(1); // 先頭の"/"を除去
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (name.startsWith(basePath) && name.contains(".base.") && 
							(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
							// パスからファイル名だけを抽出
							String fileName = name.substring(basePath.length());
							if (!fileName.isEmpty() && !fileName.contains("/")) {
								portraits.add(fileName);
							}
						}
					}
					jar.close();
				}
			}
		} catch (Exception e) {
			System.err.println("[ImageManager] Classpathからの立ち絵リスト取得エラー: " + e.getMessage());
			e.printStackTrace();
		}
		
		return portraits;
	}

	/**
	 * 指定されたパスから画像を読み込む
	 * キャッシュを使用して同じ画像を再利用する
	 * JAR化にも対応
	 * 
	 * @param path リソースパス
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	private Image loadImage(String path) {
		// キャッシュにあれば返す
		if (imageCache.containsKey(path)) {
			return imageCache.get(path);
		}

		try {
			// リソースから読み込み（JAR内でも動作）
			InputStream is = getClass().getResourceAsStream(path);
			if (is == null) {
				System.err.println("[ImageManager] 画像が見つかりません: " + path);
				return null;
			}

			Image image = new Image(is);
			is.close();
			
			if (image.isError()) {
				System.err.println("[ImageManager] 画像の読み込みに失敗しました: " + path);
				return null;
			}

			// キャッシュに保存
			imageCache.put(path, image);
			return image;

		} catch (Exception e) {
			System.err.println("[ImageManager] 画像読み込みエラー: " + path);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 立ち絵の表情差分を読み込む
	 * 例: warrior.base.png → warrior.smile.png, warrior.angry.png など
	 * 
	 * @param baseFileName 基本ファイル名（*.base.*）
	 * @param expression 表情名（smile, angry, sad など）
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	public Image loadPortraitExpression(String baseFileName, String expression) {
		// *.base.* を *.expression.* に置換
		String expressionFileName = baseFileName.replace(".base.", "." + expression + ".");
		return loadPortrait(expressionFileName);
	}

	/**
	 * キャッシュをクリア（メモリ解放用）
	 */
	public void clearCache() {
		imageCache.clear();
	}

	/**
	 * 特定の画像をキャッシュから削除
	 * 
	 * @param path リソースパス
	 */
	public void removeFromCache(String path) {
		imageCache.remove(path);
	}
	
	
	
	/**
	 * リストから背景画像をランダムに選択して読み込む
	 * 
	 * @param fileNames 背景画像ファイル名のリスト
	 * @return ランダムに選択された画像オブジェクト。リストが空の場合はnull
	 */
	public Image loadRandomBackground(List<String> fileNames) {
		if (fileNames == null || fileNames.isEmpty()) {
			return null;
		}
		String selectedFile = fileNames.get(random.nextInt(fileNames.size()));
		return loadBackground(selectedFile);
	}

	/**
	 * リストから背景画像をランダムに選択してファイル名を返す
	 * 
	 * @param fileNames 背景画像ファイル名のリスト
	 * @return ランダムに選択されたファイル名。リストが空の場合はnull
	 */
	public String selectRandomBackgroundFileName(List<String> fileNames) {
		if (fileNames == null || fileNames.isEmpty()) {
			return null;
		}
		return fileNames.get(random.nextInt(fileNames.size()));
	}

	/**
	 * サブウィンドウ用の画像を読み込む（event_imagesフォルダから）
	 * 
	 * @param fileName ファイル名（拡張子含む）
	 * @return 画像オブジェクト。読み込み失敗時はnull
	 */
	public Image loadSubImage(String fileName) {
		return loadEventImage(fileName);
	}
	
	
	
	
	
	
	
	
	
}