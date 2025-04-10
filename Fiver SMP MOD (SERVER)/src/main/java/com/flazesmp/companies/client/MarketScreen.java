// This file is part of a private project owned by BENZOOgataga.
// Unauthorized use or distribution is strictly prohibited.

package com.flazesmp.companies.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class MarketScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("flazesmp", "textures/gui/market_background.png");

    // GUI dimensions
    private final int xSize = 480;
    private final int ySize = 240;
    private int guiLeft;
    private int guiTop;
    private float scrollPosition = 0.0f; // 0.0 = top, 1.0 = bottom
    private int itemHeight = 11; // Height of each item row
    private boolean isDraggingScrollbar = false;

    // Category dropdowns
    private Button categoryButton;
    private boolean categoryDropdownOpen = false;
    private List<String> availableSubdomains = new ArrayList<>();
    private String selectedSubdomain = "Blocks";

    // Search box
    private EditBox searchBox;

    // Items display
    private List<MarketItem> displayedItems = new ArrayList<>();
    private int itemScrollOffset = 0;
    private final int ITEMS_PER_PAGE = 16; // Increased due to smaller item bars
    private Map<String, List<MarketItem>> categorizedItems = new HashMap<>();

    // Cart
    private List<CartItem> cartItems = new ArrayList<>();
    private double totalPrice = 0.0;
    private Button clearCartButton;

    // Balance display
    private double playerBalance = 76052;
    private double companyBalance = 100000;
    private boolean useCompanyFunds = true;
    private Button fundSourceButton;

    // Buy button
    private Button buyButton;

    // Page indicator
    private String pageIndicator = "1/5";

    // Mouse tracking
    private boolean isLeftMouseButtonDown = false;

    public MarketScreen() {
        super(Component.literal("Shop"));
        populateSampleData();
    }

    private void populateSampleData() {
        // Add sample subdomains
        availableSubdomains.add("Blocks");
        availableSubdomains.add("Building");
        availableSubdomains.add("Redstone");
        availableSubdomains.add("Decorative");
        availableSubdomains.add("Ores");
        availableSubdomains.add("Tools");

        // Populate sample data for each category
        categorizedItems.put("Blocks", createSampleItems("Blocks", 20));
        categorizedItems.put("Building", createSampleItems("Building", 15));
        categorizedItems.put("Redstone", createSampleItems("Redstone", 10));
        categorizedItems.put("Decorative", createSampleItems("Decorative", 25));
        categorizedItems.put("Ores", createSampleItems("Ores", 12));
        categorizedItems.put("Tools", createSampleItems("Tools", 18));

        // Set initial category
        displayedItems = categorizedItems.get("Blocks");
        selectedSubdomain = "Blocks";

        // Add sample cart items
        // cartItems.add(new CartItem("minecraft:oak_log", 64, 40, 2560));
        cartItems.add(new CartItem("minecraft:oak_planks", 256, 40, 10240));
    }

    private List<MarketItem> createSampleItems(String category, int count) {
        List<MarketItem> items = new ArrayList<>();
        String[] itemTypes = new String[] {
                "stone", "oak_log", "iron_ingot", "gold_ingot", "diamond",
                "redstone", "glowstone", "quartz", "obsidian", "glass"
        };

        for (int i = 0; i < count; i++) {
            String itemId = "minecraft:" + itemTypes[i % itemTypes.length];
            String displayName = itemTypes[i % itemTypes.length].replace("_", " ");
            double basePrice = 10 + (i * 5);
            double fluctuation = (Math.random() * 6) - 3; // -3 to +3 percent
            double sellPrice = basePrice * (1 + (fluctuation / 100));
            double buyPrice = sellPrice * 1.1; // 10% markup

            items.add(new MarketItem(itemId, displayName, basePrice, sellPrice, buyPrice, fluctuation));
        }
        return items;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        // Initialize category dropdown
        this.categoryButton = this.addRenderableWidget(Button.builder(
                        Component.literal(selectedSubdomain + " ▼"),
                        button -> toggleCategoryDropdown())
                .pos(guiLeft + 8, guiTop + 40).size(70, 17).build());

        // Initialize search box
        this.searchBox = new EditBox(this.font, guiLeft + xSize - 385, guiTop + 40, 70, 15, Component.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(16777215);
        this.searchBox.setResponder(this::updateSearch);
        this.addRenderableWidget(this.searchBox);

        // Initialize clear cart button
        this.clearCartButton = this.addRenderableWidget(Button.builder(
                        Component.literal("×"),
                        button -> clearCart())
                .pos(guiLeft + xSize - 20, guiTop + 150).size(15, 15).build());

        // Calculate cart dimensions for button positioning
        int cartWidth = xSize / 4;
        int cartX = guiLeft + xSize - cartWidth;

        // Initialize fund source button
        this.fundSourceButton = this.addRenderableWidget(Button.builder(
                        Component.literal(useCompanyFunds ? "Company Funds" : "Personal Funds"),
                        button -> toggleFundSource())
                .pos(cartX - 30, guiTop + ySize - 20).size(85, 15).build());

        // Initialize buy button
        this.buyButton = this.addRenderableWidget(Button.builder(
                        Component.literal("Buy"),
                        button -> executePurchase())
                .pos(cartX + 10, guiTop + ySize - 60).size(cartWidth - 20, 20).build());

        // Update page indicator
        updatePageIndicator();
    }

    private void toggleCategoryDropdown() {
        categoryDropdownOpen = !categoryDropdownOpen;
    }

    private void selectCategory(String category) {
        selectedSubdomain = category;
        categoryButton.setMessage(Component.literal(selectedSubdomain + " ▼"));
        categoryDropdownOpen = false;
        displayedItems = categorizedItems.get(category);
        itemScrollOffset = 0;
        updatePageIndicator();
    }

    private void clearCart() {
        cartItems.clear();
        updateTotalPrice();
    }

    private void toggleFundSource() {
        useCompanyFunds = !useCompanyFunds;
        fundSourceButton.setMessage(Component.literal(useCompanyFunds ? "Company Funds" : "Personal Funds"));
    }

    private void executePurchase() {
        if (cartItems.isEmpty()) {
            return;
        }

        // This would normally send packets to the server
        // For this demo, we just clear the cart
        cartItems.clear();
        updateTotalPrice();
    }

    private void updateSearch(String searchText) {
        // Filter items based on search text
        if (searchText.isEmpty()) {
            // Reset filter
            displayedItems = categorizedItems.get(selectedSubdomain);
            itemScrollOffset = 0;
            updatePageIndicator();
            return;
        }

        // Filter items by search text
        List<MarketItem> filteredItems = new ArrayList<>();
        for (MarketItem item : categorizedItems.get(selectedSubdomain)) {
            if (item.displayName.toLowerCase().contains(searchText.toLowerCase())) {
                filteredItems.add(item);
            }
        }
        displayedItems = filteredItems;
        itemScrollOffset = 0;
        updatePageIndicator();
    }

    private void updateTotalPrice() {
        totalPrice = 0;
        for (CartItem item : cartItems) {
            totalPrice += item.totalPrice;
        }
    }

    private void updatePageIndicator() {
        int maxPages = Math.max(1, (displayedItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int currentPage = itemScrollOffset + 1;
        pageIndicator = currentPage + "/" + maxPages;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fillGradient(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xC0101010, 0xD0101010);

        graphics.fillGradient(guiLeft, guiTop, guiLeft + xSize, guiTop + 20, 0xD0303030, 0xD0303030);
        graphics.drawCenteredString(font, "FlazeSMP Shop", guiLeft + xSize / 2, guiTop + 6, 0xFFFFFF);

        graphics.drawString(font, "Category:", guiLeft + 10, guiTop + 25, 0xAAAAAA);
        graphics.drawString(font, "Search:", guiLeft + xSize - 385, guiTop + 25, 0xAAAAAA);

        if (!categoryDropdownOpen) {
            renderItems(graphics, mouseX, mouseY);
        }

        renderCart(graphics, mouseX, mouseY);
        renderTotals(graphics);

        if (categoryDropdownOpen) {
            renderCategoryDropdown(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
        // Calculate visible area
        int listTop = guiTop + 80;
        int listBottom = guiTop + ySize - 10;
        int listHeight = listBottom - listTop;

        // Total height of all items
        int totalItemsHeight = displayedItems.size() * itemHeight;

        // Calculate scroll offset in pixels
        int scrollPixels = 0;
        if (totalItemsHeight > listHeight) {
            scrollPixels = (int)((totalItemsHeight - listHeight) * scrollPosition);
        }

        // Draw header (fixed position) - REMOVED BUTTON COLUMNS FROM HEADER
        int headerWidth = 255; // Reduced width to not include buttons
        graphics.fillGradient(guiLeft + 10, guiTop + 65, guiLeft + headerWidth, guiTop + 75, 0x60000000, 0x60000000);
        graphics.drawString(font, "Item", guiLeft + 35, guiTop + 67, 0xFFFFFF, false);
        graphics.drawString(font, "Buy", guiLeft + 140, guiTop + 67, 0xFFFFFF, false);
        graphics.drawString(font, "Sell", guiLeft + 180, guiTop + 67, 0xFFFFFF, false);
        graphics.drawString(font, "Flux", guiLeft + 220, guiTop + 67, 0xFFFFFF, false);

        // Set up scissor to clip items outside the list area
        RenderSystem.enableScissor(
                (int)(guiLeft * minecraft.getWindow().getGuiScale()),
                (int)((minecraft.getWindow().getHeight() - (listBottom * minecraft.getWindow().getGuiScale()))),
                (int)((380) * minecraft.getWindow().getGuiScale()), // Wider scissor to include buttons
                (int)(listHeight * minecraft.getWindow().getGuiScale())
        );

        // Draw items with scroll offset
        for (int i = 0; i < displayedItems.size(); i++) {
            MarketItem item = displayedItems.get(i);
            int y = listTop + (i * itemHeight) - scrollPixels;

            // Skip items completely outside visible area
            if (y + itemHeight < listTop || y > listBottom) continue;

            // Item slot background - REDUCED WIDTH TO NOT INCLUDE BUTTONS
            graphics.fillGradient(guiLeft + 10, y, guiLeft + headerWidth, y + itemHeight, 0x40000000, 0x40000000);

            // Highlight on hover
            if (mouseX >= guiLeft + 10 && mouseX <= guiLeft + headerWidth &&
                    mouseY >= y && mouseY <= y + itemHeight && mouseY >= listTop && mouseY <= listBottom) {
                graphics.fillGradient(guiLeft + 10, y, guiLeft + headerWidth, y + itemHeight, 0x40FFFFFF, 0x40FFFFFF);
            }

            // Render item icon (scaled down)
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.itemId)));
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            graphics.renderItem(stack, (guiLeft + 15) * 2, (y + 1) * 2);
            graphics.pose().popPose();

            // Render item name (scaled down)
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            graphics.drawString(font, item.displayName, (int)((guiLeft + 35) / 0.8f), (int)((y + 2) / 0.8f), 0xFFFFFF, false);
            graphics.pose().popPose();

            // Render prices and flux (scaled down)
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            String buyPrice = String.format("%.0f", item.buyPrice);
            String sellPrice = String.format("%.0f", item.sellPrice);
            String fluxText = String.format("%.1f%%", item.fluxPercent);
            int fluxColor = item.fluxPercent > 0 ? 0x55FF55 : (item.fluxPercent < 0 ? 0xFF5555 : 0xFFFFFF);

            graphics.drawString(font, buyPrice, (int)((guiLeft + 140) / 0.8f), (int)((y + 2) / 0.8f), 0xFFD700, false);
            graphics.drawString(font, sellPrice, (int)((guiLeft + 180) / 0.8f), (int)((y + 2) / 0.8f), 0xFFD700, false);
            graphics.drawString(font, fluxText, (int)((guiLeft + 220) / 0.8f), (int)((y + 2) / 0.8f), fluxColor, false);
            graphics.pose().popPose();

            if (!categoryDropdownOpen) {
                // Position buttons to the right of the item bar
                int buttonX = headerWidth + 15;

                // "1" button with rounded corners - updated position
                renderRoundedButton(graphics, "1", guiLeft + buttonX - 10, y + 1, 18, 9, mouseX, mouseY);

                // "64" button with rounded corners - updated position
                renderRoundedButton(graphics, "64", guiLeft + buttonX + 10, y + 1, 18, 9, mouseX, mouseY);

                // "INV" button with rounded corners - updated position
                renderRoundedButton(graphics, "INV", guiLeft + buttonX + 30, y + 1, 25, 9, mouseX, mouseY);
            }
        }

        // Disable scissor after drawing items
        RenderSystem.disableScissor();

        // Draw scrollbar if needed
        if (totalItemsHeight > listHeight) {
            int scrollbarX = guiLeft + 260 - 5; // Position scrollbar at the far right
            int scrollbarWidth = 5;

            // Draw scrollbar background
            graphics.fillGradient(scrollbarX + 10, listTop, scrollbarX + scrollbarWidth, listBottom, 0x40000000, 0x40000000);

            // Draw scrollbar handle
            int handleHeight = Math.max(20, (int)(listHeight * listHeight / (float)totalItemsHeight));
            int handleY = listTop + (int)((listHeight - handleHeight) * scrollPosition);
            graphics.fillGradient(scrollbarX, handleY, scrollbarX + scrollbarWidth, handleY + handleHeight, 0x80FFFFFF, 0x80FFFFFF);
        }
    }

    // New method to render a rounded button
    private void renderRoundedButton(GuiGraphics graphics, String text, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Button background (outer rounded rectangle)
        int backgroundColor = 0x80404040;
        int hoverColor = 0x80FFFFFF;

        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Draw rounded rectangle background
        // Top and bottom edges
        graphics.fillGradient(x + 1, y, x + width - 1, y + 1, backgroundColor, backgroundColor);
        graphics.fillGradient(x + 1, y + height - 1, x + width - 1, y + height, backgroundColor, backgroundColor);

        // Left and right edges
        graphics.fillGradient(x, y + 1, x + 1, y + height - 1, backgroundColor, backgroundColor);
        graphics.fillGradient(x + width - 1, y + 1, x + width, y + height - 1, backgroundColor, backgroundColor);

        // Center fill
        graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor, backgroundColor);

        // Highlight on hover
        if (isHovered) {
            // Top and bottom edges
            graphics.fillGradient(x + 1, y, x + width - 1, y + 1, hoverColor, hoverColor);
            graphics.fillGradient(x + 1, y + height - 1, x + width - 1, y + height, hoverColor, hoverColor);

            // Left and right edges
            graphics.fillGradient(x, y + 1, x + 1, y + height - 1, hoverColor, hoverColor);
            graphics.fillGradient(x + width - 1, y + 1, x + width, y + height - 1, hoverColor, hoverColor);

            // Center fill
            graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, hoverColor, hoverColor);
        }

        // Draw text centered in button
        graphics.drawCenteredString(font, text, x + width/2, y + (height - 8)/2, 0xFFFFFF);
    }

    private void renderBuyButton(GuiGraphics graphics, String text, int x, int y, int mouseX, int mouseY) {
        int buttonWidth = text.equals("INV") ? 25 : 20;
        graphics.fillGradient(x, y, x + buttonWidth, y + itemHeight, 0x80404040, 0x80404040);
        if (mouseX >= x && mouseX <= x + buttonWidth &&
                mouseY >= y && mouseY <= y + itemHeight) {
            graphics.fillGradient(x, y, x + buttonWidth, y + itemHeight, 0x80FFFFFF, 0x80FFFFFF);
        }
        graphics.drawCenteredString(font, text, x + buttonWidth/2, y + 2, 0xFFFFFF);
    }

    private void renderCart(GuiGraphics graphics, int mouseX, int mouseY) {
        int cartWidth = (int) (xSize / 3.3); // Half of previous width
        int cartX = guiLeft + xSize - cartWidth;
        int y = guiTop + 70;

        graphics.fillGradient(cartX, y -10, guiLeft + xSize, guiTop + ySize - 40, 0x60000000, 0x60000000);

        for (CartItem item : cartItems) {
            // Skip if outside visible area
            if (y + 30 > guiTop + ySize - 65) break;

            // Cart item background
            graphics.fillGradient(cartX + 5, y, guiLeft + xSize - 5, y + 25, 0x40000000, 0x40000000);

            // Render item icon
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.itemId)));
            graphics.renderItem(stack, cartX + 10, y + 5);

            // Render quantity
            String quantityText = "× " + item.quantity;
            graphics.drawString(font, quantityText, cartX + 30, y + 8, 0xFFFFFF);

            // Render total price for this item
            String priceText = String.format("%.0f", item.totalPrice);
            int priceWidth = font.width(priceText);
            graphics.drawString(font, priceText, guiLeft + xSize - 45 - priceWidth, y + 8, 0xFFD700);

            // Render coin icon
            ItemStack coinStack = new ItemStack(Items.GOLD_NUGGET);
            graphics.renderItem(coinStack, guiLeft + xSize - 40, y + 3);

            // Render remove button (X)
            renderRoundedButton(graphics, "×", guiLeft + xSize - 20, y + 5, 12, 12, mouseX, mouseY);

            y += 30;
        }
    }

    private void renderTotals(GuiGraphics graphics) {
        int cartWidth = xSize / 4;
        int cartX = guiLeft + xSize - cartWidth;

        // Draw total price
        graphics.drawString(font, "Total:", cartX - 25, guiTop + ySize - 37, 0xFFFFFF);
        String totalPriceText = String.format("%.0f", totalPrice);
        int totalPriceWidth = font.width(totalPriceText);
        graphics.drawString(font, totalPriceText, guiLeft + xSize - 25 - totalPriceWidth, guiTop + ySize - 60, 0xFFD700);
        ItemStack coinStack = new ItemStack(Items.GOLD_NUGGET);
        graphics.renderItem(coinStack, guiLeft + xSize - 20, guiTop + ySize - 65);

        // Draw player balance
        //  String balanceLabel = useCompanyFunds ? "Company:" : "Personal:";
        double balance = useCompanyFunds ? companyBalance : playerBalance;
        // graphics.drawString(font, balanceLabel, cartX - 25, guiTop + ySize - 25, 0xFFFFFF);
        String balanceText = String.format("%.0f", balance);
        int balanceWidth = font.width(balanceText);
        graphics.drawString(font, balanceText, guiLeft + xSize - 17 - balanceWidth, guiTop + ySize - 16, 0xFFD700);
        graphics.renderItem(coinStack, guiLeft + xSize - 20, guiTop + ySize - 20);
    }

    private void renderCategoryDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int dropdownX = categoryButton.getX();
        int dropdownY = categoryButton.getY() + categoryButton.getHeight();
        int dropdownWidth = categoryButton.getWidth();
        int dropdownHeight = availableSubdomains.size() * 20;

        // Draw dropdown background
        graphics.fillGradient(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xE0000000, 0xE0000000);

        // Draw options
        for (int i = 0; i < availableSubdomains.size(); i++) {
            String option = availableSubdomains.get(i);
            int optionY = dropdownY + i * 20;

            // Highlight on hover
            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                    mouseY >= optionY && mouseY <= optionY + 20) {
                graphics.fillGradient(dropdownX, optionY, dropdownX + dropdownWidth, optionY + 20, 0x80FFFFFF, 0x80FFFFFF);
            }

            graphics.drawCenteredString(font, option, dropdownX + dropdownWidth / 2, optionY + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isLeftMouseButtonDown = true;

            if (categoryDropdownOpen) {
                int dropdownX = categoryButton.getX();
                int dropdownY = categoryButton.getY() + categoryButton.getHeight();
                int dropdownWidth = categoryButton.getWidth();

                for (int i = 0; i < availableSubdomains.size(); i++) {
                    int optionY = dropdownY + i * 20;
                    if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                            mouseY >= optionY && mouseY <= optionY + 20) {
                        selectCategory(availableSubdomains.get(i));
                        return true;
                    }
                }

                categoryDropdownOpen = false;
                return true;
            }

            int listTop = guiTop + 80;
            int listBottom = guiTop + ySize - 10;
            int headerWidth = 255;
            int scrollbarX = guiLeft + 380 - 5;
            int scrollbarWidth = 5;

            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                    mouseY >= listTop && mouseY <= listBottom) {
                isDraggingScrollbar = true;
                float clickPosition = (float)(mouseY - listTop) / (listBottom - listTop);
                scrollPosition = Math.max(0.0f, Math.min(1.0f, clickPosition));
                return true;
            }

            if (!categoryDropdownOpen) {
                int listHeight = listBottom - listTop;
                int totalItemsHeight = displayedItems.size() * itemHeight;
                int scrollPixels = (totalItemsHeight > listHeight) ?
                        (int)((totalItemsHeight - listHeight) * scrollPosition) : 0;

                for (int i = 0; i < displayedItems.size(); i++) {
                    int y = listTop + (i * itemHeight) - scrollPixels;

                    // Skip if outside visible area
                    if (y + itemHeight < listTop || y > listBottom) continue;

                    MarketItem item = displayedItems.get(i);

                    // Position buttons to match renderRoundedButton positions
                    int buttonX = headerWidth + 15;

                    // "1" button - updated position to match rendering
                    if (mouseX >= guiLeft + buttonX - 10 && mouseX <= guiLeft + buttonX - 10 + 18 &&
                            mouseY >= y + 1 && mouseY <= y + 1 + 9) {
                        addToCart(item, 1);
                        return true;
                    }

                    // "64" button - updated position to match rendering
                    if (mouseX >= guiLeft + buttonX + 10 && mouseX <= guiLeft + buttonX + 10 + 18 &&
                            mouseY >= y + 1 && mouseY <= y + 1 + 9) {
                        addToCart(item, 64);
                        return true;
                    }

                    // "INV" button - updated position to match rendering
                    if (mouseX >= guiLeft + buttonX + 30 && mouseX <= guiLeft + buttonX + 30 + 25 &&
                            mouseY >= y + 1 && mouseY <= y + 1 + 9) {
                        addToCart(item, calculateInventorySpace());
                        return true;
                    }

                    // If clicking on the item row but not on a button
                    if (mouseX >= guiLeft + 10 && mouseX <= guiLeft + headerWidth &&
                            mouseY >= y && mouseY <= y + itemHeight) {
                        // Default action for clicking on item row (do nothing)
                        return true;
                    }
                }
            }

            // NEW CART ITEM CONTROLS
            int cartWidth = xSize / 4;
            int cartX = guiLeft + xSize - cartWidth;
            int y = guiTop + 70;

            for (CartItem item : cartItems) {
                // Skip if outside visible area
                if (y + 30 > guiTop + ySize - 65) break;

                // Check for clicks on the remove button
                if (mouseX >= guiLeft + xSize - 35 && mouseX <= guiLeft + xSize - 35 + 15 &&
                        mouseY >= y + 7 && mouseY <= y + 7 + 15) {
                    removeFromCart(item);
                    return true;
                }

                y += 30;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isLeftMouseButtonDown = false;
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int listTop = guiTop + 80;
            int listBottom = guiTop + ySize - 10;

            // Update scroll position based on drag
            float dragPosition = (float)(mouseY - listTop) / (listBottom - listTop);
            scrollPosition = Math.max(0.0f, Math.min(1.0f, dragPosition));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Only scroll if mouse is over the item list
        int headerWidth = 380;
        if (mouseX >= guiLeft + 10 && mouseX <= guiLeft + headerWidth &&
                mouseY >= guiTop + 80 && mouseY <= guiTop + ySize - 10) {

            // Calculate total height of all items
            int listHeight = (guiTop + ySize - 10) - (guiTop + 80);
            int totalItemsHeight = displayedItems.size() * itemHeight;

            if (totalItemsHeight > listHeight) {
                // Adjust scroll position (delta is positive for scroll up, negative for scroll down)
                float scrollAmount = 0.1f; // How much to scroll per mouse wheel click
                scrollPosition -= delta * scrollAmount;

                // Clamp scroll position between 0.0 and 1.0
                scrollPosition = Math.max(0.0f, Math.min(1.0f, scrollPosition));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addToCart(MarketItem item, int quantity) {
        for (CartItem cartItem : cartItems) {
            if (cartItem.itemId.equals(item.itemId)) {
                cartItem.quantity += quantity;
                cartItem.totalPrice += (item.buyPrice * quantity);
                updateTotalPrice();
                return;
            }
        }
        cartItems.add(new CartItem(item.itemId, quantity, item.buyPrice, item.buyPrice * quantity));
        updateTotalPrice();
    }

    private int calculateInventorySpace() {
        // In a real implementation, you would check the player's inventory
        // For this demo, we'll return a reasonable value
        return 256; // About 4 full stacks of items
    }

    private void increaseCartItemQuantity(CartItem item) {
        item.quantity++;
        item.totalPrice += item.pricePerItem;
        updateTotalPrice();
    }

    private void decreaseCartItemQuantity(CartItem item) {
        if (item.quantity > 1) {
            item.quantity--;
            item.totalPrice -= item.pricePerItem;
        } else {
            removeFromCart(item);
        }
        updateTotalPrice();
    }

    private void removeFromCart(CartItem item) {
        cartItems.remove(item);
        updateTotalPrice();
    }

    // Data class for market items
    private static class MarketItem {
        public final String itemId;
        public final String displayName;
        public final double basePrice;
        public final double sellPrice;
        public final double buyPrice;
        public final double fluxPercent;

        public MarketItem(String itemId, String displayName, double basePrice,
                          double sellPrice, double buyPrice, double fluxPercent) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.basePrice = basePrice;
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
            this.fluxPercent = fluxPercent;
        }
    }

    // Data class for cart items
    private static class CartItem {
        public final String itemId;
        public int quantity;
        public final double pricePerItem;
        public double totalPrice;

        public CartItem(String itemId, int quantity, double pricePerItem, double totalPrice) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.pricePerItem = pricePerItem;
            this.totalPrice = totalPrice;
        }
    }
}