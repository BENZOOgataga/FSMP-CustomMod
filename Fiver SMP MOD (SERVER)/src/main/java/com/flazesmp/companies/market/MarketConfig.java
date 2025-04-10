// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.market;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class MarketConfig {
    private static MarketConfig INSTANCE;
    private static final String CONFIG_FILE = "market_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // General market settings
    private double buyMarkupPercentage = 20.0; // Buy price is sell price + this percentage
    private double minPricePercentage = 50.0; // Minimum price as percentage of base price
    private double maxPricePercentage = 200.0; // Maximum price as percentage of base price
    private int priceUpdateIntervalMinutes = 60; // How often prices are recalculated
    private int salesResetHours = 24; // How often sales counters reset

    // Price adjustment settings
    private int lowSalesThreshold = 50; // 0-50 items in 24h
    private int moderateSalesThreshold = 200; // 50-200 items in 24h
    private double lowSalesPriceDropPercentage = 2.0; // 1-2% drop
    private double moderateSalesPriceDropPercentage = 5.0; // 3-5% drop
    private double highSalesPriceDropPercentage = 10.0; // 6-10% drop

    // Price recovery settings
    private double hourlyPriceRecoveryPercentage = 0.5; // How much prices recover per hour

    // Item categories by subdomain
    private Map<String, List<MarketItem>> subdomainItems = new HashMap<>();

    // Default market category for players without a company
    private List<MarketItem> defaultMarketItems = new ArrayList<>();

    // Disabled items
    private Set<String> disabledItems = new HashSet<>();

    // Sales thresholds for items
    private Map<String, Integer> itemSalesThresholds = new HashMap<>();

    private MarketConfig() {
        loadConfig();
    }

    public static MarketConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MarketConfig();
        }
        return INSTANCE;
    }

    private void loadConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<MarketConfig>(){}.getType();
                MarketConfig loadedConfig = GSON.fromJson(reader, type);

                // Copy values from loaded config
                if (loadedConfig != null) {
                    this.buyMarkupPercentage = loadedConfig.buyMarkupPercentage;
                    this.minPricePercentage = loadedConfig.minPricePercentage;
                    this.maxPricePercentage = loadedConfig.maxPricePercentage;
                    this.priceUpdateIntervalMinutes = loadedConfig.priceUpdateIntervalMinutes;
                    this.salesResetHours = loadedConfig.salesResetHours;
                    this.lowSalesThreshold = loadedConfig.lowSalesThreshold;
                    this.moderateSalesThreshold = loadedConfig.moderateSalesThreshold;
                    this.lowSalesPriceDropPercentage = loadedConfig.lowSalesPriceDropPercentage;
                    this.moderateSalesPriceDropPercentage = loadedConfig.moderateSalesPriceDropPercentage;
                    this.highSalesPriceDropPercentage = loadedConfig.highSalesPriceDropPercentage;
                    this.hourlyPriceRecoveryPercentage = loadedConfig.hourlyPriceRecoveryPercentage;
                    this.subdomainItems = loadedConfig.subdomainItems;
                    this.defaultMarketItems = loadedConfig.defaultMarketItems;
                    this.disabledItems = loadedConfig.disabledItems;
                    this.itemSalesThresholds = loadedConfig.itemSalesThresholds;
                }

                FlazeSMP.LOGGER.info("Loaded market configuration");
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load market configuration", e);
                initializeDefaultConfig();
            }
        } else {
            initializeDefaultConfig();
            saveConfig();
        }
    }

    private void initializeDefaultConfig() {
        // Initialize default market items
        defaultMarketItems = new ArrayList<>();
        defaultMarketItems.add(new MarketItem(Items.COBBLESTONE, 5.0));
        defaultMarketItems.add(new MarketItem(Items.DIRT, 2.0));
        defaultMarketItems.add(new MarketItem(Items.GRAVEL, 3.0));

        // Initialize subdomain items
        subdomainItems = new HashMap<>();

        // Mining domain
        List<MarketItem> stoneQuarryingItems = new ArrayList<>();
        stoneQuarryingItems.add(new MarketItem(Items.COBBLESTONE, 5.0));
        stoneQuarryingItems.add(new MarketItem(Items.STONE, 10.0));
        stoneQuarryingItems.add(new MarketItem(Items.GRANITE, 15.0));
        stoneQuarryingItems.add(new MarketItem(Items.DIORITE, 15.0));
        stoneQuarryingItems.add(new MarketItem(Items.ANDESITE, 15.0));
        subdomainItems.put("Stone Quarrying", stoneQuarryingItems);

        List<MarketItem> oreExtractionItems = new ArrayList<>();
        oreExtractionItems.add(new MarketItem(Items.IRON_ORE, 100.0));
        oreExtractionItems.add(new MarketItem(Items.GOLD_ORE, 200.0));
        oreExtractionItems.add(new MarketItem(Items.DIAMOND, 1000.0));
        oreExtractionItems.add(new MarketItem(Items.COAL, 50.0));
        oreExtractionItems.add(new MarketItem(Items.REDSTONE, 30.0));
        oreExtractionItems.add(new MarketItem(Items.LAPIS_LAZULI, 80.0));
        subdomainItems.put("Ore Extraction", oreExtractionItems);

        // Farming domain
        List<MarketItem> cropFarmingItems = new ArrayList<>();
        cropFarmingItems.add(new MarketItem(Items.WHEAT, 20.0));
        cropFarmingItems.add(new MarketItem(Items.CARROT, 15.0));
        cropFarmingItems.add(new MarketItem(Items.POTATO, 15.0));
        cropFarmingItems.add(new MarketItem(Items.BEETROOT, 25.0));
        subdomainItems.put("Crop Farming", cropFarmingItems);

        List<MarketItem> animalHusbandryItems = new ArrayList<>();
        animalHusbandryItems.add(new MarketItem(Items.BEEF, 40.0));
        animalHusbandryItems.add(new MarketItem(Items.PORKCHOP, 40.0));
        animalHusbandryItems.add(new MarketItem(Items.CHICKEN, 30.0));
        animalHusbandryItems.add(new MarketItem(Items.MUTTON, 35.0));
        animalHusbandryItems.add(new MarketItem(Items.EGG, 10.0));
        animalHusbandryItems.add(new MarketItem(Items.LEATHER, 50.0));
        subdomainItems.put("Animal Husbandry", animalHusbandryItems);

        // Initialize default sales thresholds (-1 means unlimited)
        itemSalesThresholds.put(ForgeRegistries.ITEMS.getKey(Items.COBBLESTONE).toString(), 1000);
        itemSalesThresholds.put(ForgeRegistries.ITEMS.getKey(Items.DIRT).toString(), 1000);

        FlazeSMP.LOGGER.info("Initialized default market configuration");
    }

    public void saveConfig() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(FlazeSMP.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, CONFIG_FILE);

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
            FlazeSMP.LOGGER.info("Saved market configuration");
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save market configuration", e);
        }
    }

    // Getters and setters
    public double getBuyMarkupPercentage() {
        return buyMarkupPercentage;
    }

    public void setBuyMarkupPercentage(double buyMarkupPercentage) {
        this.buyMarkupPercentage = buyMarkupPercentage;
        saveConfig();
    }

    public double getMinPricePercentage() {
        return minPricePercentage;
    }

    public void setMinPricePercentage(double minPricePercentage) {
        this.minPricePercentage = minPricePercentage;
        saveConfig();
    }

    public double getMaxPricePercentage() {
        return maxPricePercentage;
    }

    public void setMaxPricePercentage(double maxPricePercentage) {
        this.maxPricePercentage = maxPricePercentage;
        saveConfig();
    }

    public int getPriceUpdateIntervalMinutes() {
        return priceUpdateIntervalMinutes;
    }

    public void setPriceUpdateIntervalMinutes(int priceUpdateIntervalMinutes) {
        this.priceUpdateIntervalMinutes = priceUpdateIntervalMinutes;
        saveConfig();
    }

    public int getSalesResetHours() {
        return salesResetHours;
    }

    public void setSalesResetHours(int salesResetHours) {
        this.salesResetHours = salesResetHours;
        saveConfig();
    }

    public int getLowSalesThreshold() {
        return lowSalesThreshold;
    }

    public void setLowSalesThreshold(int lowSalesThreshold) {
        this.lowSalesThreshold = lowSalesThreshold;
        saveConfig();
    }

    public int getModerateSalesThreshold() {
        return moderateSalesThreshold;
    }

    public void setModerateSalesThreshold(int moderateSalesThreshold) {
        this.moderateSalesThreshold = moderateSalesThreshold;
        saveConfig();
    }

    public double getLowSalesPriceDropPercentage() {
        return lowSalesPriceDropPercentage;
    }

    public void setLowSalesPriceDropPercentage(double lowSalesPriceDropPercentage) {
        this.lowSalesPriceDropPercentage = lowSalesPriceDropPercentage;
        saveConfig();
    }

    public double getModerateSalesPriceDropPercentage() {
        return moderateSalesPriceDropPercentage;
    }

    public void setModerateSalesPriceDropPercentage(double moderateSalesPriceDropPercentage) {
        this.moderateSalesPriceDropPercentage = moderateSalesPriceDropPercentage;
        saveConfig();
    }

    public double getHighSalesPriceDropPercentage() {
        return highSalesPriceDropPercentage;
    }

    public void setHighSalesPriceDropPercentage(double highSalesPriceDropPercentage) {
        this.highSalesPriceDropPercentage = highSalesPriceDropPercentage;
        saveConfig();
    }

    public double getHourlyPriceRecoveryPercentage() {
        return hourlyPriceRecoveryPercentage;
    }

    public void setHourlyPriceRecoveryPercentage(double hourlyPriceRecoveryPercentage) {
        this.hourlyPriceRecoveryPercentage = hourlyPriceRecoveryPercentage;
        saveConfig();
    }

    public Map<String, List<MarketItem>> getSubdomainItems() {
        return subdomainItems;
    }

    public List<MarketItem> getItemsForSubdomain(String subdomain) {
        return subdomainItems.getOrDefault(subdomain, new ArrayList<>());
    }

    public List<MarketItem> getDefaultMarketItems() {
        return defaultMarketItems;
    }

    public boolean isItemDisabled(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        return itemId != null && disabledItems.contains(itemId.toString());
    }

    public void disableItem(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            disabledItems.add(itemId.toString());
            saveConfig();
        }
    }

    public void enableItem(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            disabledItems.remove(itemId.toString());
            saveConfig();
        }
    }

    public int getItemSalesThreshold(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            return itemSalesThresholds.getOrDefault(itemId.toString(), -1);
        }
        return -1; // -1 means unlimited
    }

    public void setItemSalesThreshold(Item item, int threshold) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        if (itemId != null) {
            itemSalesThresholds.put(itemId.toString(), threshold);
            saveConfig();
        }
    }

    // Inner class for market items
    public static class MarketItem {
        private String itemId;
        private double basePrice;

        // Required for GSON
        public MarketItem() {
        }

        public MarketItem(Item item, double basePrice) {
            this.itemId = ForgeRegistries.ITEMS.getKey(item).toString();
            this.basePrice = basePrice;
        }

        public Item getItem() {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            return ForgeRegistries.ITEMS.getValue(resourceLocation);
        }

        public String getItemId() {
            return itemId;
        }

        public double getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(double basePrice) {
            this.basePrice = basePrice;
        }
    }
}
