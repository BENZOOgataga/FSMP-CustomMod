package com.flazesmp.companies.market;

import com.flazesmp.companies.FlazeSMP;
import com.flazesmp.companies.common.network.NetworkHandler;
import com.flazesmp.companies.common.network.packets.OpenMarketGuiPacket;
import com.flazesmp.companies.data.Company;
import com.flazesmp.companies.data.CompanyManager;
import com.flazesmp.companies.data.CompanyPermission;
import com.flazesmp.companies.economy.EconomyManager;
import com.flazesmp.companies.market.MarketConfig;
import com.flazesmp.companies.market.MarketManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarketCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("market")
                        .requires(source -> hasPermission(source, "fsmp.economy.market"))
                        .then(Commands.literal("buy")
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                .executes(MarketCommands::buyItem))))
                        .then(Commands.literal("sell")
                                .executes(MarketCommands::sellHeldItem)
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                        .executes(MarketCommands::sellHeldItemQuantity)))
                        .then(Commands.literal("price")
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(MarketCommands::checkPrice)))
                        .then(Commands.literal("list")
                                .executes(MarketCommands::listMarketItems))
                        .then(Commands.literal("disable")
                                .requires(source -> hasPermission(source, "fsmp.economy.market.disableitem"))
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(MarketCommands::disableItem)))
                        .then(Commands.literal("enable")
                                .requires(source -> hasPermission(source, "fsmp.economy.market.disableitem"))
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(MarketCommands::enableItem)))
                        .then(Commands.literal("threshold")
                                .requires(source -> hasPermission(source, "fsmp.economy.market.threshold"))
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .then(Commands.argument("threshold", IntegerArgumentType.integer(-1))
                                                .executes(MarketCommands::setItemThreshold))))
                        .then(Commands.literal("graph")
                                .requires(source -> hasPermission(source, "fsmp.economy.market.graph"))
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(context -> showPriceGraph(context, "1d"))
                                        .then(Commands.argument("timeframe", StringArgumentType.word())
                                                .executes(context -> showPriceGraph(context, StringArgumentType.getString(context, "timeframe"))))))
        );
    }

    private static boolean hasPermission(CommandSourceStack source, String permission) {
        if (source.hasPermission(2)) { // OP level 2+
            return true;
        }

        if (!source.isPlayer()) {
            return true; // Console always has access
        }


        return false;
    }

    /**
     * Buy an item from the market
     */
    private static int buyItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        String itemName = StringArgumentType.getString(context, "item");
        int quantity = IntegerArgumentType.getInteger(context, "quantity");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Check if item is disabled
        if (MarketConfig.getInstance().isItemDisabled(item)) {
            context.getSource().sendFailure(Component.literal("This item is currently unavailable for purchase."));
            return 0;
        }

        // Calculate total cost
        MarketManager marketManager = MarketManager.getInstance(level);
        double pricePerItem = marketManager.getBuyPrice(item);
        double totalCost = pricePerItem * quantity;

        // Format for display
        String formattedPrice = EconomyManager.formatCurrency(pricePerItem);
        String formattedTotal = EconomyManager.formatCurrency(totalCost);

        // Get player's company
        CompanyManager companyManager = CompanyManager.get(level);
        Company company = companyManager.getPlayerCompany(player.getUUID());

        // Check if player can use company funds
        boolean canUseCompanyFunds = false;
        if (company != null &&
                (player.getUUID().equals(company.getCeoId()) ||
                        company.hasPermission(player.getUUID(), CompanyPermission.WITHDRAW_FUNDS))) {
            canUseCompanyFunds = true;
        }

        // Ask player which balance to use
        if (canUseCompanyFunds) {
            // Show confirmation with options
            MutableComponent message = Component.literal("You are about to buy ")
                    .append(Component.literal("" + quantity).withStyle(net.minecraft.ChatFormatting.YELLOW))
                    .append(Component.literal(" " + item.getDescription().getString() + " for "))
                    .append(Component.literal(formattedTotal).withStyle(net.minecraft.ChatFormatting.GREEN))
                    .append(Component.literal(".\nChoose payment method:"));

            // Personal balance button
            Component personalButton = Component.literal("[Personal Balance]")
                    .withStyle(net.minecraft.ChatFormatting.AQUA)
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                            "/market buyconfirm " + itemName + " " + quantity + " personal")));

            // Company balance button
            Component companyButton = Component.literal("[Company Balance]")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                            "/market buyconfirm " + itemName + " " + quantity + " company")));

            context.getSource().sendSuccess(() -> message, false);
            context.getSource().sendSuccess(() -> personalButton.copy().append(Component.literal(" ")).append(companyButton), false);

            return 1;
        } else {
            // Use personal balance directly
            return executeBuyItem(player, level, item, quantity, totalCost, false);
        }
    }

    /**
     * Execute the item purchase after confirmation
     */
    private static int executeBuyItem(ServerPlayer player, ServerLevel level, Item item, int quantity, double totalCost, boolean useCompanyFunds) {
        EconomyManager economyManager = EconomyManager.get(level);
        CompanyManager companyManager = CompanyManager.get(level);
        Company company = companyManager.getPlayerCompany(player.getUUID());

        // Check if player/company has enough money
        boolean canAfford = false;

        if (useCompanyFunds) {
            if (company != null && company.getFunds() >= totalCost) {
                canAfford = true;
            }
        } else {
            if (economyManager.getPlayerBalance(player.getUUID()) >= totalCost) {
                canAfford = true;
            }
        }

        if (!canAfford) {
            player.sendSystemMessage(Component.literal("You don't have enough money to complete this purchase."));
            return 0;
        }

        // Deduct money
        if (useCompanyFunds) {
            company.withdrawFunds(totalCost);
            companyManager.setDirty();

            // Add transaction record
            company.addTransaction(player.getUUID(), com.flazesmp.companies.data.CompanyTransaction.TransactionType.PAYMENT,
                    totalCost, "Market purchase: " + quantity + " " + item.getDescription().getString());
        } else {
            economyManager.removeFromPlayerBalance(player.getUUID(), totalCost);
        }

        // Give items to player
        ItemStack itemStack = new ItemStack(item, quantity);
        if (!player.getInventory().add(itemStack)) {
            // If inventory is full, drop the items
            player.drop(itemStack, false);
            player.sendSystemMessage(Component.literal("Your inventory is full. Items have been dropped on the ground."));
        }

        // Notify player
        String source = useCompanyFunds ? "company balance" : "personal balance";
        player.sendSystemMessage(Component.literal("You bought " + quantity + " " +
                item.getDescription().getString() + " for " + EconomyManager.formatCurrency(totalCost) +
                " from your " + source + "."));

        return 1;
    }

    /**
     * Sell the item the player is holding
     */
    private static int sellHeldItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item to sell."));
            return 0;
        }

        return sellItem(context, heldItem.getItem(), heldItem.getCount());
    }

    /**
     * Sell a specific quantity of the item the player is holding
     */
    private static int sellHeldItemQuantity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int quantity = IntegerArgumentType.getInteger(context, "quantity");
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item to sell."));
            return 0;
        }

        if (heldItem.getCount() < quantity) {
            context.getSource().sendFailure(Component.literal("You don't have enough items. You only have " +
                    heldItem.getCount() + " " + heldItem.getItem().getDescription().getString() + "."));
            return 0;
        }

        return sellItem(context, heldItem.getItem(), quantity);
    }

    /**
     * Sell an item to the market
     */
    private static int sellItem(CommandContext<CommandSourceStack> context, Item item, int quantity) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();

        // Check if item is disabled
        if (MarketConfig.getInstance().isItemDisabled(item)) {
            context.getSource().sendFailure(Component.literal("This item is currently unavailable for sale."));
            return 0;
        }

        // Check if player is in a company
        CompanyManager companyManager = CompanyManager.get(level);
        Company company = companyManager.getPlayerCompany(player.getUUID());

        if (company == null) {
            context.getSource().sendFailure(Component.literal("You must be in a company to sell items."));
            return 0;
        }

        // Check if item is in player's subdomain
        if (!isItemInSubdomain(item, company.getSubdomain()) &&
                !isItemInDefaultMarket(item)) {
            context.getSource().sendFailure(Component.literal("Your company cannot sell this item. It's not in your subdomain."));
            return 0;
        }

        // Check sales threshold
        MarketManager marketManager = MarketManager.getInstance(level);
        if (marketManager.isSalesThresholdReached(item, quantity)) {
            context.getSource().sendFailure(Component.literal("Sales threshold reached for this item. Try again later."));
            return 0;
        }

        // Calculate total value
        double pricePerItem = marketManager.getSellPrice(item);
        double totalValue = pricePerItem * quantity;

        // Remove items from player's inventory
        int removed = removeItemsFromInventory(player, item, quantity);
        if (removed < quantity) {
            context.getSource().sendFailure(Component.literal("You don't have enough items."));
            return 0;
        }

        // Add money to company
        company.addFunds(totalValue);
        companyManager.setDirty();

        // Add transaction record
        company.addTransaction(player.getUUID(), com.flazesmp.companies.data.CompanyTransaction.TransactionType.DEPOSIT,
                totalValue, "Market sale: " + quantity + " " + item.getDescription().getString());

        // Record sale in market data
        marketManager.recordSale(item, quantity, player.getUUID(), company.getId(),
                player.getName().getString(), company.getName());

        // Notify player
        String formattedTotal = EconomyManager.formatCurrency(totalValue);
        context.getSource().sendSuccess(() ->
                Component.literal("Sold " + quantity + " " + item.getDescription().getString() +
                        " for " + formattedTotal + ". Funds added to your company balance."), false);

        return 1;
    }

    /**
     * Check the price of an item
     */
    private static int checkPrice(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        String itemName = StringArgumentType.getString(context, "item");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Get prices
        MarketManager marketManager = MarketManager.getInstance(level);
        double sellPrice = marketManager.getSellPrice(item);
        double buyPrice = marketManager.getBuyPrice(item);

        // Check if item is disabled
        boolean disabled = MarketConfig.getInstance().isItemDisabled(item);

        // Format for display
        String formattedSellPrice = EconomyManager.formatCurrency(sellPrice);
        String formattedBuyPrice = EconomyManager.formatCurrency(buyPrice);

        // Display prices
        context.getSource().sendSuccess(() -> Component.literal("=== Market Prices for " +
                item.getDescription().getString() + " ==="), false);

        if (disabled) {
            context.getSource().sendSuccess(() -> Component.literal("§cThis item is currently disabled in the market."), false);
        }

        context.getSource().sendSuccess(() -> Component.literal("Sell Price: " + formattedSellPrice), false);
        context.getSource().sendSuccess(() -> Component.literal("Buy Price: " + formattedBuyPrice), false);

        // Check sales threshold
        int threshold = MarketConfig.getInstance().getItemSalesThreshold(item);
        if (threshold != -1) {
            context.getSource().sendSuccess(() -> Component.literal("Sales Threshold: " + threshold + " per day"), false);
        }

        return 1;
    }

    /**
     * List available market items for the player
     */
    private static int listMarketItems(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();

        // Get player's company
        CompanyManager companyManager = CompanyManager.get(level);
        Company company = companyManager.getPlayerCompany(player.getUUID());

        List<MarketConfig.MarketItem> availableItems = new ArrayList<>();

        if (company != null) {
            // Get items for player's subdomain
            availableItems.addAll(MarketConfig.getInstance().getItemsForSubdomain(company.getSubdomain()));
        }

        // Add default items
        availableItems.addAll(MarketConfig.getInstance().getDefaultMarketItems());

        if (availableItems.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No market items available."));
            return 0;
        }

        // Display available items
        context.getSource().sendSuccess(() -> Component.literal("=== Available Market Items ==="), false);

        MarketManager marketManager = MarketManager.getInstance(level);

        for (MarketConfig.MarketItem marketItem : availableItems) {
            Item item = marketItem.getItem();
            if (item == null || item == Items.AIR) continue;

            double sellPrice = marketManager.getSellPrice(item);
            double buyPrice = marketManager.getBuyPrice(item);

            boolean disabled = MarketConfig.getInstance().isItemDisabled(item);

            // Format for display
            String formattedSellPrice = EconomyManager.formatCurrency(sellPrice);
            String formattedBuyPrice = EconomyManager.formatCurrency(buyPrice);

            String status = disabled ? " §c[DISABLED]" : "";

            context.getSource().sendSuccess(() ->
                    Component.literal("- " + item.getDescription().getString() + status +
                            "\n  Sell: " + formattedSellPrice + " | Buy: " + formattedBuyPrice), false);
        }

        return 1;
    }

    /**
     * Disable an item in the market
     */
    private static int disableItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String itemName = StringArgumentType.getString(context, "item");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Disable the item
        MarketConfig.getInstance().disableItem(item);

        context.getSource().sendSuccess(() ->
                Component.literal("Disabled " + item.getDescription().getString() + " in the market."), true);

        return 1;
    }

    /**
     * Enable an item in the market
     */
    private static int enableItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String itemName = StringArgumentType.getString(context, "item");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Enable the item
        MarketConfig.getInstance().enableItem(item);

        context.getSource().sendSuccess(() ->
                Component.literal("Enabled " + item.getDescription().getString() + " in the market."), true);

        return 1;
    }

    /**
     * Set sales threshold for an item
     */
    private static int setItemThreshold(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String itemName = StringArgumentType.getString(context, "item");
        int threshold = IntegerArgumentType.getInteger(context, "threshold");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Set threshold
        MarketConfig.getInstance().setItemSalesThreshold(item, threshold);

        String thresholdText = threshold == -1 ? "unlimited" : String.valueOf(threshold);
        context.getSource().sendSuccess(() ->
                Component.literal("Set sales threshold for " + item.getDescription().getString() +
                        " to " + thresholdText + "."), true);

        return 1;
    }

    /**
     * Show price graph for an item
     */
    private static int showPriceGraph(CommandContext<CommandSourceStack> context, String timeframe) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        String itemName = StringArgumentType.getString(context, "item");

        // Find the item
        Item item = findItem(itemName);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found: " + itemName));
            return 0;
        }

        // Determine time range
        long timeRangeMillis;
        switch (timeframe.toLowerCase()) {
            case "1h":
                timeRangeMillis = 60 * 60 * 1000L;
                break;
            case "1d":
            case "24h":
                timeRangeMillis = 24 * 60 * 60 * 1000L;
                break;
            case "1w":
            case "7d":
                timeRangeMillis = 7 * 24 * 60 * 60 * 1000L;
                break;
            case "1m":
            case "30d":
                timeRangeMillis = 30 * 24 * 60 * 60 * 1000L;
                break;
            default:
                context.getSource().sendFailure(Component.literal("Invalid timeframe. Use 1h, 1d, 1w, or 1m."));
                return 0;
        }

        // Get price history
        MarketManager marketManager = MarketManager.getInstance(level);
        List<MarketManager.PriceHistoryEntry> history = marketManager.getPriceHistory(item, timeRangeMillis);

        if (history.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No price history available for this item in the selected timeframe."));
            return 0;
        }

        // Display graph header
        context.getSource().sendSuccess(() -> Component.literal("=== Price History for " +
                item.getDescription().getString() + " (" + timeframe + ") ==="), false);

        // Display current price
        double currentPrice = marketManager.getSellPrice(item);
        context.getSource().sendSuccess(() -> Component.literal("Current Price: " +
                EconomyManager.formatCurrency(currentPrice)), false);

        // Display price range
        double minPrice = history.stream().mapToDouble(MarketManager.PriceHistoryEntry::getPrice).min().orElse(0);
        double maxPrice = history.stream().mapToDouble(MarketManager.PriceHistoryEntry::getPrice).max().orElse(0);
        context.getSource().sendSuccess(() -> Component.literal("Price Range: " +
                EconomyManager.formatCurrency(minPrice) + " - " + EconomyManager.formatCurrency(maxPrice)), false);

        // Display total sales
        int totalSales = history.stream().mapToInt(MarketManager.PriceHistoryEntry::getSales).sum();
        context.getSource().sendSuccess(() -> Component.literal("Total Sales: " + totalSales), false);

        // Display recent sales
        List<MarketManager.SaleRecord> recentSales = marketManager.getRecentSales(item, 5);
        if (!recentSales.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("=== Recent Sales ==="), false);

            for (MarketManager.SaleRecord sale : recentSales) {
                context.getSource().sendSuccess(() ->
                        Component.literal(sale.getPlayerName() + " sold " + sale.getQuantity() +
                                " items from company " + sale.getCompanyName()), false);
            }
        }

        // Note: In a real implementation, you might want to send a more visual graph
        // using custom packets and client-side rendering. For now, we'll just show text.
        context.getSource().sendSuccess(() -> Component.literal("Note: Visual graphs are not implemented in this version."), false);

        return 1;
    }

    /**
     * Find an item by name
     */
    private static Item findItem(String name) {
        // Try exact match first
        ResourceLocation exactLocation = new ResourceLocation(name);
        Item exactItem = ForgeRegistries.ITEMS.getValue(exactLocation);
        if (exactItem != null && exactItem != Items.AIR) {
            return exactItem;
        }

        // Try minecraft: prefix
        if (!name.contains(":")) {
            ResourceLocation minecraftLocation = new ResourceLocation("minecraft", name);
            Item minecraftItem = ForgeRegistries.ITEMS.getValue(minecraftLocation);
            if (minecraftItem != null && minecraftItem != Items.AIR) {
                return minecraftItem;
            }
        }

        // Try partial match
        for (ResourceLocation itemId : ForgeRegistries.ITEMS.getKeys()) {
            if (itemId.getPath().contains(name.toLowerCase()) ||
                    itemId.toString().contains(name.toLowerCase())) {
                return ForgeRegistries.ITEMS.getValue(itemId);
            }
        }

        return null;
    }

    /**
     * Check if an item is in a subdomain
     */
    private static boolean isItemInSubdomain(Item item, String subdomain) {
        List<MarketConfig.MarketItem> subdomainItems = MarketConfig.getInstance().getItemsForSubdomain(subdomain);

        for (MarketConfig.MarketItem marketItem : subdomainItems) {
            if (marketItem.getItem() == item) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an item is in the default market
     */
    private static boolean isItemInDefaultMarket(Item item) {
        List<MarketConfig.MarketItem> defaultItems = MarketConfig.getInstance().getDefaultMarketItems();

        for (MarketConfig.MarketItem marketItem : defaultItems) {
            if (marketItem.getItem() == item) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove items from player's inventory
     */
    private static int removeItemsFromInventory(ServerPlayer player, Item item, int quantity) {
        int remaining = quantity;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                if (remaining <= 0) {
                    break;
                }
            }
        }

        return quantity - remaining;
    }

    /**
     * Open the market GUI
     */
    private static int openMarketGui(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Send packet to open the GUI on the client
        NetworkHandler.sendToPlayer(new OpenMarketGuiPacket(), player);

        return 1;
    }
}

