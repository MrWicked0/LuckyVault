package com.alienseczero;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public class LotteryConfig {
    public static final String CONFIG_FILE = "config/ASZlottery/lottery.properties";

    // Human-friendly configuration values:
    public static int BONUS_DIAMONDS = 5;
    public static int MAX_TICKETS_PER_PLAYER = 5;

    // Times in seconds/hours:
    public static int announcementIntervalSeconds = 600;  // 600 seconds = 10 minutes
    public static int winnerEffectDurationHours = 24;       // 24 hours
    public static int advertisementIntervalSeconds = 300;   // 300 seconds = 5 minutes

    // Derived internal values:
    public static int ANNOUNCEMENT_INTERVAL_TICKS;  // = announcementIntervalSeconds * 20
    public static long WINNER_EFFECT_DURATION_MS;    // = winnerEffectDurationHours * 3600000L
    public static int ADVERTISEMENT_INTERVAL_TICKS;  // = advertisementIntervalSeconds * 20

    // Configurable particle types (resource IDs)
    public static String GROUND_PARTICLE = "minecraft:wax_off";
    public static String PLAYER_PARTICLE = "minecraft:glow";

    // Advertisement message (with color codes)
    public static String ADVERTISEMENT_MESSAGE = "§aDon't miss out on the lottery! Buy your tickets now and win big!";

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            saveDefaults();
            // Load defaults manually:
            BONUS_DIAMONDS = 5;
            MAX_TICKETS_PER_PLAYER = 5;
            announcementIntervalSeconds = 600;
            winnerEffectDurationHours = 24;
            advertisementIntervalSeconds = 300;
            ANNOUNCEMENT_INTERVAL_TICKS = announcementIntervalSeconds * 20;
            WINNER_EFFECT_DURATION_MS = winnerEffectDurationHours * 3600000L;
            ADVERTISEMENT_INTERVAL_TICKS = advertisementIntervalSeconds * 20;
            GROUND_PARTICLE = "minecraft:wax_off";
            PLAYER_PARTICLE = "minecraft:glow";
            ADVERTISEMENT_MESSAGE = "§aDon't miss out on the lottery! Buy your tickets now and win big!";
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties prop = new Properties();
            prop.load(fis);
            BONUS_DIAMONDS = Integer.parseInt(prop.getProperty("bonusDiamonds", "5"));
            MAX_TICKETS_PER_PLAYER = Integer.parseInt(prop.getProperty("maxTicketsPerPlayer", "5"));
            announcementIntervalSeconds = Integer.parseInt(prop.getProperty("announcementIntervalSeconds", "600"));
            winnerEffectDurationHours = Integer.parseInt(prop.getProperty("winnerEffectDurationHours", "24"));
            advertisementIntervalSeconds = Integer.parseInt(prop.getProperty("advertisementIntervalSeconds", "300"));

            ANNOUNCEMENT_INTERVAL_TICKS = announcementIntervalSeconds * 20;
            WINNER_EFFECT_DURATION_MS = winnerEffectDurationHours * 3600000L;
            ADVERTISEMENT_INTERVAL_TICKS = advertisementIntervalSeconds * 20;

            GROUND_PARTICLE = prop.getProperty("groundParticle", "minecraft:wax_off");
            PLAYER_PARTICLE = prop.getProperty("playerParticle", "minecraft:glow");
            ADVERTISEMENT_MESSAGE = prop.getProperty("advertisementMessage", "§aDon't miss out on the lottery! Buy your tickets now and win big!");
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void saveDefaults() {
        Properties prop = new Properties();
        prop.setProperty("bonusDiamonds", "5");
        prop.setProperty("maxTicketsPerPlayer", "5");
        prop.setProperty("announcementIntervalSeconds", "600");
        prop.setProperty("winnerEffectDurationHours", "24");
        prop.setProperty("advertisementIntervalSeconds", "300");
        prop.setProperty("groundParticle", "minecraft:wax_off");
        prop.setProperty("playerParticle", "minecraft:glow");
        prop.setProperty("advertisementMessage", "§aDon't miss out on the lottery! Buy your tickets now and win big!");
        File file = new File(CONFIG_FILE);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            prop.store(fos, "Lottery Mod Configuration (times in seconds/hours)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper to map a config string to a ParticleOptions instance.
    private static ParticleOptions parseParticle(String particle) {
        if (particle.equalsIgnoreCase("minecraft:happy_villager")) {
            return ParticleTypes.HAPPY_VILLAGER;
        } else if (particle.equalsIgnoreCase("minecraft:crit")) {
            return ParticleTypes.CRIT;
        } else if (particle.equalsIgnoreCase("minecraft:smoke")) {
            return ParticleTypes.SMOKE;
        } else if (particle.equalsIgnoreCase("minecraft:glow")) {
            return ParticleTypes.GLOW;
        } else if (particle.equalsIgnoreCase("minecraft:wax_off")) {
            return ParticleTypes.WAX_OFF;
        }
        return ParticleTypes.WAX_OFF;
    }

    public static ParticleOptions getGroundParticle() {
        return parseParticle(GROUND_PARTICLE);
    }

    public static ParticleOptions getPlayerParticle() {
        return parseParticle(PLAYER_PARTICLE);
    }
}