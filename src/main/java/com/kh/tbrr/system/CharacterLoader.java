package com.kh.tbrr.system;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
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