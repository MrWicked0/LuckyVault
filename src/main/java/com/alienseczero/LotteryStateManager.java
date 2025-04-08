package com.alienseczero;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LotteryStateManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String LOTTERY_STATE_FILE = "config/ASZlottery/lottery_data.json";

    // LotteryState holds the lottery pot, ticket entries, and next draw time.
    public static class LotteryState {
        public int lotteryPot;
        public Map<String, Integer> entries;
        public long nextDrawTime; // in milliseconds

        public LotteryState() {
            lotteryPot = 0;
            entries = new HashMap<>();
            // Set next draw time to current time plus the configured announcement interval (in ms).
            nextDrawTime = System.currentTimeMillis() + LotteryConfig.announcementIntervalSeconds * 1000L;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveLotteryState(int lotteryPot, Map<UUID, Integer> entries, long nextDrawTime) {
        LotteryState state = new LotteryState();
        state.lotteryPot = lotteryPot;
        state.nextDrawTime = nextDrawTime;
        for (Map.Entry<UUID, Integer> e : entries.entrySet()) {
            state.entries.put(e.getKey().toString(), e.getValue());
        }
        File file = new File(LOTTERY_STATE_FILE);
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(state, writer);
            LOGGER.info("Saved lottery state with nextDrawTime = " + state.nextDrawTime);
        } catch (IOException e) {
            LOGGER.error("Failed to save lottery state!", e);
        }
    }

    public static LotteryState loadLotteryState() {
        File file = new File(LOTTERY_STATE_FILE);
        if (!file.exists()) {
            LOGGER.info("No lottery state file found; using defaults.");
            return new LotteryState();
        }
        try (Reader reader = new FileReader(file)) {
            LotteryState state = GSON.fromJson(reader, LotteryState.class);
            if (state == null) {
                state = new LotteryState();
            }
            if (state.entries == null) {
                state.entries = new HashMap<>();
            }
            // If the saved nextDrawTime is in the past, update it.
            if (state.nextDrawTime < System.currentTimeMillis()) {
                state.nextDrawTime = System.currentTimeMillis() + LotteryConfig.announcementIntervalSeconds * 1000L;
                LOGGER.info("Next draw time was in the past; updated to " + state.nextDrawTime);
            }
            LOGGER.info("Loaded lottery state with nextDrawTime = " + state.nextDrawTime);
            return state;
        } catch (IOException e) {
            LOGGER.error("Failed to load lottery state!", e);
            return new LotteryState();
        }
    }

    public static void saveUnclaimedWinnings(Map<UUID, Integer> winnings) {
        File file = new File("config/ASZlottery/unclaimed_winnings.txt");
        file.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Map.Entry<UUID, Integer> entry : winnings.entrySet()) {
                writer.println(entry.getKey().toString() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save unclaimed winnings!", e);
        }
    }

    public static Map<UUID, Integer> loadUnclaimedWinnings() {
        Map<UUID, Integer> winnings = new HashMap<>();
        File file = new File("config/ASZlottery/unclaimed_winnings.txt");
        if (!file.exists()) return winnings;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        int count = Integer.parseInt(parts[1]);
                        winnings.put(uuid, count);
                    } catch (IllegalArgumentException ex) {
                        LOGGER.error("Invalid data in unclaimed winnings file: " + line, ex);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load unclaimed winnings!", e);
        }
        return winnings;
    }
}