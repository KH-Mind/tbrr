package com.kh.tbrr.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kh.tbrr.data.models.Personality;
import com.kh.tbrr.data.models.Player;

public class TextReplacerTest {

    public static void main(String[] args) {
        testReplacements();
    }

    private static void testReplacements() {
        System.out.println("Starting TextReplacer Verification...");

        // 1. Setup Mock Personality
        Personality personality = new Personality();
        personality.setId("cheerful");
        personality.setName("Cheerful");

        Map<String, List<String>> dialogue = new HashMap<>();

        List<String> screamList = new ArrayList<>();
        screamList.add("Scream Test");
        dialogue.put("scream", screamList);

        List<String> shockList = new ArrayList<>();
        shockList.add("Shock Test");
        dialogue.put("shock", shockList);

        List<String> disgustList = new ArrayList<>();
        disgustList.add("Disgust Test");
        dialogue.put("disgust", disgustList);

        List<String> refusalList = new ArrayList<>();
        refusalList.add("Refusal Test");
        dialogue.put("refusal", refusalList);

        List<String> metaDeathList = new ArrayList<>();
        metaDeathList.add("Meta Death Test");
        dialogue.put("meta_death", metaDeathList);

        personality.setDialogue(dialogue);

        // 2. Setup Mock Player
        Player player = new Player();
        player.setName("TestPlayer");
        player.setPersonality(personality);

        // 3. Test Cases
        String[] testInputs = {
                "Test (悲鳴口上)",
                "Test (驚愕口上)",
                "Test (嫌悪口上)",
                "Test (拒否口上)",
                "Test (メタ死口上)"
        };

        String[] expectedOutputs = {
                "Test Scream Test",
                "Test Shock Test",
                "Test Disgust Test",
                "Test Refusal Test",
                "Test Meta Death Test"
        };

        boolean allPassed = true;

        for (int i = 0; i < testInputs.length; i++) {
            String input = testInputs[i];
            String expected = expectedOutputs[i];
            String actual = TextReplacer.replace(input, player);

            if (!actual.equals(expected)) {
                System.out.println("FAILED: Input='" + input + "'");
                System.out.println("  Expected='" + expected + "'");
                System.out.println("  Actual  ='" + actual + "'");
                allPassed = false;
            } else {
                System.out.println("PASSED: " + input + " -> " + actual);
            }
        }

        if (allPassed) {
            System.out.println("All tests passed successfully!");
        } else {
            System.out.println("Some tests failed.");
            System.exit(1);
        }
    }
}
