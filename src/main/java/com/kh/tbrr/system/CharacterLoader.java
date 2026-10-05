package com.kh.tbrr.system;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kh.tbrr.data.models.Player;

public class CharacterLoader {
	private static final Path SAVE_DIR = Paths.get("userdata/character"); // 実行カレント下の userdata/character/
	private final Gson gson;

	public CharacterLoader() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		ensureSaveDir();
	}

	private void ensureSaveDir() {
		try {
			if (Files.notExists(SAVE_DIR))
				Files.createDirectories(SAVE_DIR);
		} catch (IOException e) {
			throw new RuntimeException("Cannot create save directory: " + SAVE_DIR, e);
		}
	}

	public boolean saveCharacter(Player player) {
		String requested = player.getEnglishName();
		String filename = chooseFilename(player, requested);
		Path out = SAVE_DIR.resolve(filename);
		try (FileWriter writer = new FileWriter(out.toFile())) {
			gson.toJson(player, writer);
			return true;
		} catch (IOException e) {
			System.err.println("保存失敗: " + e.getMessage());
			return false;
		}
	}

	/**
	 * 既存のキャラクターJSONを上書き保存する。
	 * ファイル名は「englishName.json」で固定。重複チェックは行わない。
	 * 引継ぎシステムがキャラシを更新する際に使用する。
	 *
	 * @param player 保存するPlayerデータ
	 * @return 成功すればtrue
	 */
	public boolean overwriteCharacter(Player player) {
		String englishName = player.getEnglishName();
		if (englishName == null || englishName.trim().isEmpty()) {
			System.err.println("上書き保存失敗: englishNameが未設定です。");
			return false;
		}
		String filename = sanitizeFilename(englishName.trim()) + ".json";
		Path out = SAVE_DIR.resolve(filename);
		try (FileWriter writer = new FileWriter(out.toFile())) {
			gson.toJson(player, writer);
			System.out.println("キャラシを上書き保存しました: " + out);
			return true;
		} catch (IOException e) {
			System.err.println("上書き保存失敗: " + e.getMessage());
			return false;
		}
	}

	public Player loadCharacter(String filename) {
		Path in = SAVE_DIR.resolve(filename);
		if (!Files.exists(in)) {
			System.err.println("読み込み失敗: ファイルが存在しません -> " + in);
			return null;
		}
		try (FileReader reader = new FileReader(in.toFile())) {
			return gson.fromJson(reader, Player.class);
		} catch (IOException e) {
			System.err.println("読み込み失敗: " + e.getMessage());
			return null;
		}
	}

	public List<String> listSavedCharacters() {
		try {
			return Files.list(SAVE_DIR)
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
					.map(p -> p.getFileName().toString())
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			return List.of();
		}
	}

	// Resurrection
	public static Player loadFromFile(String id) {
		String path = "userdata/character/" + id + ".json";
		try (Reader reader = new FileReader(path)) {
			Gson gson = new Gson();
			return gson.fromJson(reader, Player.class);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * ゲーム定義の固定キャラクターをクラスパスから読み込む。
	 * /data/characters/{characterId}.json を読む（JAR・jpackage環境でも動作）。
	 * userdata/character/ のユーザー作成キャラとは完全に別管理。
	 *
	 * @param characterId キャラID（拡張子.jsonの有無どちらでも可）
	 * @return 読み込んだPlayer（nullは返さない）
	 * @throws IOException データ不在・JSON空・不正の場合
	 */
	public static Player loadFixedCharacter(String characterId) throws IOException {
		// 拡張子が付与されている場合・いない場合の両方に対応
		String filename = characterId.endsWith(".json") ? characterId : characterId + ".json";
		String path = "/data/characters/" + filename;

		// クラスローダーフォールバック（開発環境・JAR・jpackage環境の差異を吸収するための保険）
		InputStream is = CharacterLoader.class.getResourceAsStream(path);
		if (is == null) {
			String fallbackPath = path.startsWith("/") ? path.substring(1) : path;
			is = CharacterLoader.class.getClassLoader().getResourceAsStream(fallbackPath);
		}

		if (is == null) {
			throw new IOException("固定キャラデータが見つかりません: " + path);
		}

		try (InputStream stream = is;
				InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			Player player = new Gson().fromJson(reader, Player.class);
			if (player == null) {
				throw new IOException("固定キャラデータの読み込みに失敗しました（JSON形式が空または不正）: " + path);
			}
			return player;
		}
	}

	private String chooseFilename(Player player, String requestedFilename) {
		String base = (requestedFilename != null && !requestedFilename.trim().isEmpty())
				? stripExtension(requestedFilename.trim())
				: null;

		if (base != null) {
			base = sanitizeFilename(base);
			String candidate = base + ".json";
			int suffix = 1;
			while (Files.exists(SAVE_DIR.resolve(candidate))) {
				candidate = base + "_" + suffix + ".json";
				suffix++;
			}
			return candidate;
		}

		// fallback: CharaNN.json
		for (int i = 1; i <= 999; i++) {
			String candidate = String.format("Chara%03d.json", i);
			if (Files.notExists(SAVE_DIR.resolve(candidate))) {
				return candidate;
			}
		}
		// last resort
		return "Chara_" + UUID.randomUUID() + ".json";
	}

	private String sanitizeFilename(String name) {
		// replace spaces with underscore, restrict to ASCII letters, digits, underscore, hyphen
		String s = name.replaceAll("\\s+", "_");
		s = s.replaceAll("[^A-Za-z0-9_\\-]", "_");
		if (s.isEmpty())
			s = "Chara";
		if (s.length() > 32)
			s = s.substring(0, 32);
		return s;
	}

	private String stripExtension(String fname) {
		if (fname.toLowerCase().endsWith(".json"))
			return fname.substring(0, fname.length() - 5);
		return fname;
	}
}