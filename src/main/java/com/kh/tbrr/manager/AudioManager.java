package com.kh.tbrr.manager;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Audio Manager for SE (Sound Effects) and BGM
 * Handles sound playback for the game
 */
public class AudioManager {

	// SE cache (AudioClip is suitable for short sound effects)
	private Map<String, AudioClip> seCache;

	// Current BGM player
	private MediaPlayer currentBgmPlayer;

	// Volume settings (0.0 to 1.0)
	private double seVolume;
	private double bgmVolume;

	// Enable/disable flags
	private boolean seEnabled;
	private boolean bgmEnabled;

	public AudioManager() {
		this.seCache = new HashMap<>();
		this.seVolume = 0.7;
		this.bgmVolume = 0.5;
		this.seEnabled = true;
		this.bgmEnabled = true;
	}

	/**
	 * Play a sound effect
	 * 
	 * @param fileName SE file name (e.g., "roar01.mp3")
	 */
	public void playSE(String fileName) {
		System.err.println("[AudioManager] playSE called: " + fileName);

		if (!seEnabled || fileName == null || fileName.isEmpty()) {
			System.err.println("[AudioManager] SE disabled or filename empty");
			return;
		}

		try {
			AudioClip clip = seCache.get(fileName);

			if (clip == null) {
				String path = "/data/audio/se/" + fileName;
				URL resource = getClass().getResource(path);

				System.err.println("[AudioManager] Looking for resource: " + path);
				System.err.println("[AudioManager] Resource found: " + (resource != null));

				if (resource == null) {
					System.err.println("[AudioManager] SE file not found: " + path);
					return;
				}

				String uri = resource.toExternalForm();
				System.err.println("[AudioManager] Creating AudioClip from: " + uri);

				clip = new AudioClip(uri);
				seCache.put(fileName, clip);
				System.err.println("[AudioManager] AudioClip created successfully");
			} else {
				System.err.println("[AudioManager] Using cached AudioClip");
			}

			clip.setVolume(seVolume);
			System.err.println("[AudioManager] Volume set to: " + seVolume);
			System.err.println("[AudioManager] Calling play()...");
			clip.play();
			System.err.println("[AudioManager] play() called - clip.isPlaying(): " + clip.isPlaying());

		} catch (Exception e) {
			System.err.println("[AudioManager] Failed to play SE: " + fileName);
			e.printStackTrace();
		}
	}

	/**
	 * Play background music (loops)
	 * 
	 * @param fileName BGM file name (e.g., "dungeon01.mp3")
	 */
	public void playBGM(String fileName) {
		if (!bgmEnabled || fileName == null || fileName.isEmpty()) {
			return;
		}

		try {
			// Stop current BGM
			stopBGM();

			String path = "/data/audio/music/" + fileName;
			URL resource = getClass().getResource(path);

			if (resource == null) {
				System.err.println("[AudioManager] BGM file not found: " + path);
				return;
			}

			Media media = new Media(resource.toExternalForm());
			currentBgmPlayer = new MediaPlayer(media);
			currentBgmPlayer.setVolume(bgmVolume);
			currentBgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
			currentBgmPlayer.play();

		} catch (Exception e) {
			System.err.println("[AudioManager] Failed to play BGM: " + fileName);
			e.printStackTrace();
		}
	}

	/**
	 * Stop current BGM
	 */
	public void stopBGM() {
		if (currentBgmPlayer != null) {
			currentBgmPlayer.stop();
			currentBgmPlayer.dispose();
			currentBgmPlayer = null;
		}
	}

	/**
	 * Pause current BGM
	 */
	public void pauseBGM() {
		if (currentBgmPlayer != null) {
			currentBgmPlayer.pause();
		}
	}

	/**
	 * Resume current BGM
	 */
	public void resumeBGM() {
		if (currentBgmPlayer != null) {
			currentBgmPlayer.play();
		}
	}

	// Volume controls

	public void setSEVolume(double volume) {
		this.seVolume = Math.max(0.0, Math.min(1.0, volume));
	}

	public double getSEVolume() {
		return seVolume;
	}

	public void setBGMVolume(double volume) {
		this.bgmVolume = Math.max(0.0, Math.min(1.0, volume));
		if (currentBgmPlayer != null) {
			currentBgmPlayer.setVolume(bgmVolume);
		}
	}

	public double getBGMVolume() {
		return bgmVolume;
	}

	// Enable/disable controls

	public void setSEEnabled(boolean enabled) {
		this.seEnabled = enabled;
	}

	public boolean isSEEnabled() {
		return seEnabled;
	}

	public void setBGMEnabled(boolean enabled) {
		this.bgmEnabled = enabled;
		if (!enabled) {
			stopBGM();
		}
	}

	public boolean isBGMEnabled() {
		return bgmEnabled;
	}

	/**
	 * Clear SE cache
	 */
	public void clearCache() {
		seCache.clear();
	}

	/**
	 * Cleanup resources
	 */
	public void dispose() {
		stopBGM();
		// SEキャッシュ内のすべてのAudioClipを停止
		for (AudioClip clip : seCache.values()) {
			if (clip != null) {
				clip.stop();
			}
		}
		clearCache();
	}
}