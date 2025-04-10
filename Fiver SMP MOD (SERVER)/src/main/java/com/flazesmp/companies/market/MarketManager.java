// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.market;

import com.flazesmp.companies.FlazeSMP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;

public class MarketManager extends SavedData {
    private static final String DATA_NAME = FlazeSMP.MOD_ID + "_market";
    private static final String MARKET_DATA_FILE = "market_data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MarketManager INSTANCE;

    // Current prices and sales data
    private Map<String, ItemMarketData> marketData = new HashMap<>();

    // Last update time
    private long lastPriceUpdateTime = 0;
    private long lastSalesResetTime = 0;

    // Historical price data for graphs
    private Map<String, List<PriceHistoryEntry>> priceHistory = new HashMap<>();

    public MarketManager() {
        loadMarketData();
    }

    public static MarketManager getInstance(ServerLevel level) {
        if (INSTANCE == null) {
            INSTANCE = level.getDataStorage()
                    .computeIfAbsent(MarketManager::load, MarketManager::new, DATA_NAME);
        }
        return INSTANCE;
    }

    private void loadMarketData() {
        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            return;
        }

        File dataFile = new File(dataDir, MARKET_DATA_FILE);

        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                Type type = new TypeToken<Map<String, ItemMarketData>>() {
                }.getType();
                Map<String, ItemMarketData> loadedData = GSON.fromJson(reader, type);

                if (loadedData != null) {
                    this.marketData = loadedData;
                }

                FlazeSMP.LOGGER.info("Loaded market data for " + marketData.size() + " items");
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to load market data", e);
            }
        }

        // Load price history
        File historyDir = new File(dataDir, "market_history");
        if (historyDir.exists()) {
            for (File file : historyDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    String itemId = file.getName().replace(".json", "");
                    try (FileReader reader = new FileReader(file)) {
                        Type type = new TypeToken<List<PriceHistoryEntry>>() {
                        }.getType();
                        List<PriceHistoryEntry> history = GSON.fromJson(reader, type);
                        if (history != null) {
                            priceHistory.put(itemId, history);
                        }
                    } catch (IOException e) {
                        FlazeSMP.LOGGER.error("Failed to load price history for " + itemId, e);
                    }
                }
            }
        }
    }

    public void saveMarketData() {
        File dataDir = FMLPaths.GAMEDIR.get().resolve("data").resolve(FlazeSMP.MOD_ID).toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File dataFile = new File(dataDir, MARKET_DATA_FILE);

        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(marketData, writer);
        } catch (IOException e) {
            FlazeSMP.LOGGER.error("Failed to save market data", e);
        }

        // Save price history
        File historyDir = new File(dataDir, "market_history");
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }

        for (Map.Entry<String, List<PriceHistoryEntry>> entry : priceHistory.entrySet()) {
            File historyFile = new File(historyDir, entry.getKey() + ".json");
            try (FileWriter writer = new FileWriter(historyFile)) {
                GSON.toJson(entry.getValue(), writer);
            } catch (IOException e) {
                FlazeSMP.LOGGER.error("Failed to save price history for " + entry.getKey(), e);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save to file
        saveMarketData();

        // Also save last update times to NBT
        tag.putLong("LastPriceUpdateTime", lastPriceUpdateTime);
        tag.putLong("LastSalesResetTime", lastSalesResetTime);

        return tag;
    }

    public static MarketManager load(CompoundTag tag) {
        MarketManager manager = new MarketManager();

        // Load last update times from NBT
        if (tag.contains("LastPriceUpdateTime")) {
            manager.lastPriceUpdateTime = tag.getLong("LastPriceUpdateTime");
        }

        if (tag.contains("LastSalesResetTime")) {
            manager.lastSalesResetTime = tag.getLong("LastSalesResetTime");
        }

        return manager;
    }

    /**
     * Get the current sell price for an item
     */
    public double getSellPrice(Item item) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        ItemMarketData data = getOrCreateItemData(itemId);

        // Check if we need to update prices
        checkAndUpdatePrices();

        return data.getCurrentPrice();
    }

    /**
     * Get the current buy price for an item
     */
    public double getBuyPrice(Item item) {
        double sellPrice = getSellPrice(item);
        double markup = MarketConfig.getInstance().getBuyMarkupPercentage() / 100.0;

        return sellPrice * (1 + markup);
    }

    /**
     * Record a sale of items
     */
    public void recordSale(Item item, int quantity, UUID playerId, int companyId, String playerName, String companyName) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        ItemMarketData data = getOrCreateItemData(itemId);

        // Add to sales count
        data.addSales(quantity);

        // Record sale in history
        data.addSaleRecord(new SaleRecord(playerId, companyId, quantity, System.currentTimeMillis(), playerName, companyName));

        // Update price immediately if it's a large sale
        if (quantity > MarketConfig.getInstance().getModerateSalesThreshold()) {
            updateItemPrice(itemId, data);
        }

        setDirty();
    }

    /**
     * Check if sales threshold has been reached for an item
     */
    public boolean isSalesThresholdReached(Item item, int additionalQuantity) {
        MarketConfig config = MarketConfig.getInstance();
        int threshold = config.getItemSalesThreshold(item);

        // -1 means unlimited
        if (threshold == -1) {
            return false;
        }

        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        ItemMarketData data = getOrCreateItemData(itemId);

        return (data.getTotalSales() + additionalQuantity) > threshold;
    }

    /**
     * Check and update prices if needed
     */
    private void checkAndUpdatePrices() {
        long currentTime = System.currentTimeMillis();
        MarketConfig config = MarketConfig.getInstance();

        // Check if it's time to update prices
        if (currentTime - lastPriceUpdateTime > config.getPriceUpdateIntervalMinutes() * 60 * 1000) {
            updateAllPrices();
            lastPriceUpdateTime = currentTime;
            setDirty();
        }

        // Check if it's time to reset sales counters
        if (currentTime - lastSalesResetTime > config.getSalesResetHours() * 60 * 60 * 1000) {
            resetSalesCounters();
            lastSalesResetTime = currentTime;
            setDirty();
        }
    }

    /**
     * Update prices for all items
     */
    private void updateAllPrices() {
        for (Map.Entry<String, ItemMarketData> entry : marketData.entrySet()) {
            updateItemPrice(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Update price for a specific item
     */
    private void updateItemPrice(String itemId, ItemMarketData data) {
        MarketConfig config = MarketConfig.getInstance();

        // Get base price
        double basePrice = getBasePrice(itemId);
        if (basePrice <= 0) {
            return; // Skip invalid items
        }

        // Calculate price adjustment based on sales
        double priceAdjustment = 0;
        int totalSales = data.getTotalSales();

        if (totalSales <= config.getLowSalesThreshold()) {
            // Low sales - small price drop
            priceAdjustment = -(config.getLowSalesPriceDropPercentage() / 100.0) *
                    (totalSales / (double) config.getLowSalesThreshold());
        } else if (totalSales <= config.getModerateSalesThreshold()) {
            // Moderate sales - medium price drop
            priceAdjustment = -(config.getLowSalesPriceDropPercentage() / 100.0) -
                    ((config.getModerateSalesPriceDropPercentage() - config.getLowSalesPriceDropPercentage()) / 100.0) *
                            ((totalSales - config.getLowSalesThreshold()) /
                                    (double) (config.getModerateSalesThreshold() - config.getLowSalesThreshold()));
        } else {
            // High sales - large price drop
            priceAdjustment = -(config.getModerateSalesPriceDropPercentage() / 100.0) -
                    ((config.getHighSalesPriceDropPercentage() - config.getModerateSalesPriceDropPercentage()) / 100.0) *
                            Math.min(1.0, (totalSales - config.getModerateSalesThreshold()) /
                                    (double) config.getModerateSalesThreshold());
        }

        // Apply price recovery if no recent sales
        if (data.getLastSaleTime() > 0) {
            long hoursSinceLastSale = (System.currentTimeMillis() - data.getLastSaleTime()) / (60 * 60 * 1000);
            if (hoursSinceLastSale > 0) {
                double recoveryAdjustment = (config.getHourlyPriceRecoveryPercentage() / 100.0) * hoursSinceLastSale;
                priceAdjustment += recoveryAdjustment;
            }
        }

        // Calculate new price
        double newPrice = basePrice * (1 + priceAdjustment);

        // Apply min/max constraints
        double minPrice = basePrice * (config.getMinPricePercentage() / 100.0);
        double maxPrice = basePrice * (config.getMaxPricePercentage() / 100.0);

        newPrice = Math.max(minPrice, Math.min(maxPrice, newPrice));

        // Update the price
        data.setCurrentPrice(newPrice);

        // Add to price history
        addToPriceHistory(itemId, newPrice, data.getTotalSales());
    }

    /**
     * Reset sales counters for all items
     */
    private void resetSalesCounters() {
        for (ItemMarketData data : marketData.values()) {
            data.resetSales();
        }
    }

    /**
     * Get the base price for an item
     */
    private double getBasePrice(String itemId) {
        MarketConfig config = MarketConfig.getInstance();
        ResourceLocation resourceLocation = new ResourceLocation(itemId);
        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);

        if (item == null) {
            return 0;
        }

        // Check all subdomains
        for (List<MarketConfig.MarketItem> items : config.getSubdomainItems().values()) {
            for (MarketConfig.MarketItem marketItem : items) {
                if (marketItem.getItemId().equals(itemId)) {
                    return marketItem.getBasePrice();
                }
            }
        }

        // Check default items
        for (MarketConfig.MarketItem marketItem : config.getDefaultMarketItems()) {
            if (marketItem.getItemId().equals(itemId)) {
                return marketItem.getBasePrice();
            }
        }

        return 0; // Not found
    }

    /**
     * Get or create market data for an item
     */
    private ItemMarketData getOrCreateItemData(String itemId) {
        ItemMarketData data = marketData.get(itemId);

        if (data == null) {
            double basePrice = getBasePrice(itemId);
            if (basePrice <= 0) {
                basePrice = 10.0; // Default price if not configured
            }

            data = new ItemMarketData(basePrice);
            marketData.put(itemId, data);
        }

        return data;
    }

    /**
     * Add an entry to the price history
     */
    private void addToPriceHistory(String itemId, double price, int sales) {
        List<PriceHistoryEntry> history = priceHistory.computeIfAbsent(itemId, k -> new ArrayList<>());

        // Add new entry
        history.add(new PriceHistoryEntry(System.currentTimeMillis(), price, sales));

        // Limit history size (keep last 1000 entries)
        if (history.size() > 1000) {
            history = history.subList(history.size() - 1000, history.size());
            priceHistory.put(itemId, history);
        }
    }

    /**
     * Get price history for an item
     */
    public List<PriceHistoryEntry> getPriceHistory(Item item, long timeRangeMillis) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        List<PriceHistoryEntry> history = priceHistory.getOrDefault(itemId, new ArrayList<>());

        if (timeRangeMillis <= 0) {
            return new ArrayList<>(history);
        }

        // Filter by time range
        long cutoffTime = System.currentTimeMillis() - timeRangeMillis;
        return history.stream()
                .filter(entry -> entry.getTimestamp() >= cutoffTime)
                .toList();
    }

    /**
     * Get recent sales for an item
     */
    public List<SaleRecord> getRecentSales(Item item, int limit) {
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        ItemMarketData data = getOrCreateItemData(itemId);

        List<SaleRecord> sales = data.getSaleRecords();
        if (sales.size() <= limit) {
            return new ArrayList<>(sales);
        }

        return sales.subList(sales.size() - limit, sales.size());
    }

    /**
     * Class to store market data for an item
     */
    public static class ItemMarketData {
        private double currentPrice;
        private int totalSales = 0;
        private long lastSaleTime = 0;
        private List<SaleRecord> saleRecords = new ArrayList<>();

        public ItemMarketData() {
            // For GSON
        }

        public ItemMarketData(double initialPrice) {
            this.currentPrice = initialPrice;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
        }

        public int getTotalSales() {
            return totalSales;
        }

        public void addSales(int quantity) {
            this.totalSales += quantity;
            this.lastSaleTime = System.currentTimeMillis();
        }

        public void resetSales() {
            this.totalSales = 0;
        }

        public long getLastSaleTime() {
            return lastSaleTime;
        }

        public List<SaleRecord> getSaleRecords() {
            return saleRecords;
        }

        public void addSaleRecord(SaleRecord record) {
            saleRecords.add(record);

            // Keep only last 100 sales
            if (saleRecords.size() > 100) {
                saleRecords = saleRecords.subList(saleRecords.size() - 100, saleRecords.size());
            }
        }
    }

    /**
     * Class to store price history entries
     */
    public static class PriceHistoryEntry {
        private long timestamp;
        private double price;
        private int sales;

        public PriceHistoryEntry() {
            // For GSON
        }

        public PriceHistoryEntry(long timestamp, double price, int sales) {
            this.timestamp = timestamp;
            this.price = price;
            this.sales = sales;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double getPrice() {
            return price;
        }

        public int getSales() {
            return sales;
        }
    }

    /**
     * Class to store sale records
     */
    public static class SaleRecord {
        private UUID playerId;
        private int companyId;
        private int quantity;
        private long timestamp;
        private String playerName;
        private String companyName;

        public SaleRecord() {
            // For GSON
        }

        public SaleRecord(UUID playerId, int companyId, int quantity, long timestamp, String playerName, String companyName) {
            this.playerId = playerId;
            this.companyId = companyId;
            this.quantity = quantity;
            this.timestamp = timestamp;
            this.playerName = playerName;
            this.companyName = companyName;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public int getCompanyId() {
            return companyId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getCompanyName() {
            return companyName;
        }
    }
}