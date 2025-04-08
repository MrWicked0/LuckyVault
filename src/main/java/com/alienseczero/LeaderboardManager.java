package com.alienseczero;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeaderboardManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String LEADERBOARD_FILE = "config/ASZlottery/leaderboard_data.txt";

    public static void saveLeaderboard(Map<UUID, Integer> leaderboard) {
        File file = new File(LEADERBOARD_FILE);
        file.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Map.Entry<UUID, Integer> entry : leaderboard.entrySet()) {
                writer.println(entry.getKey().toString() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save leaderboard data!", e);
        }
    }

    public static Map<UUID, Integer> loadLeaderboard() {
        Map<UUID, Integer> leaderboard = new HashMap<>();
        File file = new File(LEADERBOARD_FILE);
        if (!file.exists()) return leaderboard;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        int wins = Integer.parseInt(parts[1]);
                        leaderboard.put(uuid, wins);
                    } catch (Exception e) {
                        LOGGER.error("Invalid leaderboard entry: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load leaderboard data!", e);
        }
        return leaderboard;
    }
}