package com.alienseczero;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public class LotteryConfig {
    public static final String CONFIG_FILE = "config/ASZlottery/lottery.properties";

    // Human-friendly configuration values:
    public static int BONUS_POT = 10;
    public static int MAX_TICKETS_PER_PLAYER = 10;

    // Times in seconds/hours:
    public static int announcementIntervalSeconds = 43200;  // 43200 seconds = 12 hours
    public static int winnerEffectDurationHours = 24;       // 24 hours
    public static int advertisementIntervalSeconds = 1800;   // 900 seconds = 15 minutes

    // Derived internal values:
    public static int ANNOUNCEMENT_INTERVAL_TICKS;  // = announcementIntervalSeconds * 20
    public static long WINNER_EFFECT_DURATION_MS;    // = winnerEffectDurationHours * 3600000L
    public static int ADVERTISEMENT_INTERVAL_TICKS;  // = advertisementIntervalSeconds * 20

    // Configurable particle types (resource IDs)
    public static String GROUND_PARTICLE = "minecraft:wax_off";
    public static String PLAYER_PARTICLE = "minecraft:glow";

    // Advertisement message (with color codes)
    public static String ADVERTISEMENT_MESSAGE = "§aDon't miss out on the lottery! §fBuy your tickets now and win big!";

    // Configurable currency item (resource ID) for the lottery.
    // Default is "minecraft:diamond". Users can change this (e.g., "minecraft:emerald").
    public static String CURRENCY_ITEM = "minecraft:diamond";

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            saveDefaults();
            // Load defaults manually:
            BONUS_POT = 10;
            MAX_TICKETS_PER_PLAYER = 10;
            announcementIntervalSeconds = 43200;
            winnerEffectDurationHours = 24;
            advertisementIntervalSeconds = 1800;
            ANNOUNCEMENT_INTERVAL_TICKS = announcementIntervalSeconds * 20;
            WINNER_EFFECT_DURATION_MS = winnerEffectDurationHours * 3600000L;
            ADVERTISEMENT_INTERVAL_TICKS = advertisementIntervalSeconds * 20;
            GROUND_PARTICLE = "minecraft:wax_off";
            PLAYER_PARTICLE = "minecraft:glow";
            ADVERTISEMENT_MESSAGE = "§aDon't miss out on the lottery! §fBuy your tickets now and win big!";
            CURRENCY_ITEM = "minecraft:diamond";
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties prop = new Properties();
            prop.load(fis);
            BONUS_POT = Integer.parseInt(prop.getProperty("bonusPot", "10"));
            MAX_TICKETS_PER_PLAYER = Integer.parseInt(prop.getProperty("maxTicketsPerPlayer", "10"));
            announcementIntervalSeconds = Integer.parseInt(prop.getProperty("announcementIntervalSeconds", "43200"));
            winnerEffectDurationHours = Integer.parseInt(prop.getProperty("winnerEffectDurationHours", "24"));
            advertisementIntervalSeconds = Integer.parseInt(prop.getProperty("advertisementIntervalSeconds", "1800"));

            ANNOUNCEMENT_INTERVAL_TICKS = announcementIntervalSeconds * 20;
            WINNER_EFFECT_DURATION_MS = winnerEffectDurationHours * 3600000L;
            ADVERTISEMENT_INTERVAL_TICKS = advertisementIntervalSeconds * 20;

            GROUND_PARTICLE = prop.getProperty("groundParticle", "minecraft:wax_off");
            PLAYER_PARTICLE = prop.getProperty("playerParticle", "minecraft:glow");
            ADVERTISEMENT_MESSAGE = prop.getProperty("advertisementMessage", "§aDon't miss out on the lottery! §fBuy your tickets now and win big!");
            CURRENCY_ITEM = prop.getProperty("currencyItem", "minecraft:diamond");
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void saveDefaults() {
        Properties prop = new Properties();
        prop.setProperty("bonusPot", "10");
        prop.setProperty("maxTicketsPerPlayer", "10");
        prop.setProperty("announcementIntervalSeconds", "43200");
        prop.setProperty("winnerEffectDurationHours", "24");
        prop.setProperty("advertisementIntervalSeconds", "1800");
        prop.setProperty("groundParticle", "minecraft:wax_off");
        prop.setProperty("playerParticle", "minecraft:glow");
        prop.setProperty("advertisementMessage", "§aDon't miss out on the lottery! §fBuy your tickets now and win big!");
        prop.setProperty("currencyItem", "minecraft:diamond");
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

    public static Item getCurrencyItem() {
        ResourceLocation rl = ResourceLocation.tryParse(CURRENCY_ITEM);
        if (rl == null) {
            rl = ResourceLocation.tryParse("minecraft:diamond");
        }
        // Explicitly cast the lookup result to an Item.
        Object obj = BuiltInRegistries.ITEM.get(rl);
        return (obj instanceof Item) ? (Item) obj : Items.DIAMOND;
    }


}