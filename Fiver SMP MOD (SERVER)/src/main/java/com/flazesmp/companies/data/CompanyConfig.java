package com.flazesmp.companies.config;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CompanyConfig {
    private static CompanyConfig INSTANCE;
    private static final String CONFIG_FILE = "company_settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Maintenance settings
    private double minimumBalancePercentage = 5.0; // 5% of company value
    private int gracePeriodDays = 7; // 7 days to fix financial issues
    private double baseCompanyValue = 10000.0; // Base value for calculating minimum balance

    // Mother company settings
    private int weeklyRevenueShareDay = 1; // Day of the week for revenue sharing (1 = Monday)

    private CompanyConfig() {
        // Private constructor for singleton
    }

    public static CompanyConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadConfig();
        }
        return INSTANCE;
    }

    public static CompanyConfig loadConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                CompanyConfig config = GSON.fromJson(reader, CompanyConfig.class);
                FlazeSMP.LOGGER.info("Loaded company configuration");
                return config;
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load company configuration", e);
            }
        }

        // Create default config
        CompanyConfig config = new CompanyConfig();
        config.saveConfig();
        return config;
    }

    public void saveConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
            FlazeSMP.LOGGER.info("Saved company configuration");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save company configuration", e);
        }
    }

    // Getters and setters
    public double getMinimumBalancePercentage() {
        return minimumBalancePercentage;
    }

    public void setMinimumBalancePercentage(double minimumBalancePercentage) {
        this.minimumBalancePercentage = minimumBalancePercentage;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public double getBaseCompanyValue() {
        return baseCompanyValue;
    }

    public void setBaseCompanyValue(double baseCompanyValue) {
        this.baseCompanyValue = baseCompanyValue;
    }

    public int getWeeklyRevenueShareDay() {
        return weeklyRevenueShareDay;
    }

    public void setWeeklyRevenueShareDay(int weeklyRevenueShareDay) {
        this.weeklyRevenueShareDay = weeklyRevenueShareDay;
    }

    // Calculate minimum balance for a company
    public double calculateMinimumBalance(int tier) {
        // Higher tier companies have higher minimum balance requirements
        double tierMultiplier = 1.0 + ((tier - 1) * 0.5); // Tier 1: 1.0x, Tier 2: 1.5x, Tier 3: 2.0x, Tier 4: 2.5x
        return (baseCompanyValue * tierMultiplier * minimumBalancePercentage) / 100.0;
    }
}
