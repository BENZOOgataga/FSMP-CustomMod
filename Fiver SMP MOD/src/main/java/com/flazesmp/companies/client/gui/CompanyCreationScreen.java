package com.flazesmp.companies.client.gui;

import com.flazesmp.companies.client.ClientDomainCache;
import com.flazesmp.companies.client.gui.elements.DomainEntry;
import com.flazesmp.companies.client.gui.elements.ProgressIndicator;
import com.flazesmp.companies.client.gui.elements.ScrollablePanel;
import com.flazesmp.companies.client.gui.elements.SubdomainEntry;
import com.flazesmp.companies.common.network.NetworkHandler;
import com.flazesmp.companies.common.network.packets.CompanyCreatePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CompanyCreationScreen extends Screen {
    private enum CreationStage {
        NAME_INPUT,
        DOMAIN_SELECTION,
        SUBDOMAIN_SELECTION,
        CONFIRMATION
    }

    private CreationStage currentStage = CreationStage.NAME_INPUT;

    // GUI dimensions
    private final int xSize = 320;
    private final int ySize = 200;
    private int guiLeft;
    private int guiTop;

    // Company data
    private String companyName = "";
    private String selectedDomain = "";
    private String selectedSubdomain = "";

    // UI Elements
    private EditBox nameField;
    private ScrollablePanel domainPanel;
    private ScrollablePanel subdomainPanel;
    private Button nextButton;
    private Button backButton;
    private List<DomainEntry> domainEntries = new ArrayList<>();
    private List<SubdomainEntry> subdomainEntries = new ArrayList<>();

    // Progress indicators
    private ProgressIndicator progressIndicator;

    public CompanyCreationScreen() {
        super(Component.literal("Create Company"));
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        // Add navigation buttons
        this.backButton = this.addRenderableWidget(Button.builder(Component.literal("Back"), (button) -> {
            goBack();
        }).pos(guiLeft + 10, guiTop + ySize - 30).size(80, 20).build());

        this.nextButton = this.addRenderableWidget(Button.builder(Component.literal("Next"), (button) -> {
            goNext();
        }).pos(guiLeft + xSize - 90, guiTop + ySize - 30).size(80, 20).build());

        // Add progress indicator
        this.progressIndicator = new ProgressIndicator(this, guiLeft + 60, guiTop + ySize - 40, 200, 4, 4);
        this.progressIndicator.setCurrentStep(currentStage.ordinal());

        // Initialize stage-specific elements
        initStageElements();

        // Update button states
        updateButtonStates();
    }

    private void initStageElements() {
        // Clear previous stage elements
        this.clearWidgets();

        // Re-add navigation buttons
        this.addRenderableWidget(backButton);
        this.addRenderableWidget(nextButton);

        // Add stage-specific elements
        switch (currentStage) {
            case NAME_INPUT:
                initNameInputStage();
                break;
            case DOMAIN_SELECTION:
                initDomainSelectionStage();
                break;
            case SUBDOMAIN_SELECTION:
                initSubdomainSelectionStage();
                break;
            case CONFIRMATION:
                initConfirmationStage();
                break;
        }

        // Update progress indicator
        this.progressIndicator.setCurrentStep(currentStage.ordinal());
    }

    private void initNameInputStage() {
        // Add company name field
        this.nameField = new EditBox(this.font, guiLeft + 60, guiTop + 80, 200, 20, Component.literal(""));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(companyName);
        this.nameField.setFocused(true);
        this.addRenderableWidget(nameField);

        // Set as the focused element for the screen
        setFocused(nameField);
    }

    private void initDomainSelectionStage() {
        // Create domain panel with more height
        this.domainPanel = new ScrollablePanel(this, guiLeft + 20, guiTop + 40, xSize - 40, ySize - 100);

        // Add domain entries
        this.domainEntries.clear();

        // Use domains from ClientDomainCache instead of hard-coded values
        for (ClientDomainCache.DomainData domain : ClientDomainCache.getDomains()) {
            addDomainEntry(domain.getName(), domain.getIcon(), domain.getDescription());
        }
    }


    private void addDomainEntry(String name, ItemStack icon, String description) {
        DomainEntry entry = new DomainEntry(this, domainPanel.getWidth() - 0, 30, name, icon, description);
        entry.setSelected(name.equals(selectedDomain));
        entry.setClickListener(() -> {
            selectDomain(name);
        });

        domainEntries.add(entry);
        domainPanel.addEntry(entry);
    }

    private void initSubdomainSelectionStage() {
        // Create subdomain panel
        this.subdomainPanel = new ScrollablePanel(this, guiLeft + 20, guiTop + 40, xSize - 40, ySize - 100);

        // Add subdomain entries based on selected domain
        this.subdomainEntries.clear();

        // Use subdomains from ClientDomainCache instead of hard-coded values
        List<ClientDomainCache.SubdomainData> subdomains = ClientDomainCache.getSubdomains(selectedDomain);
        for (ClientDomainCache.SubdomainData subdomain : subdomains) {
            addSubdomainEntry(
                    subdomain.getName(),
                    subdomain.getIcon(),
                    subdomain.getBuff(),
                    subdomain.isLocked(),
                    subdomain.isShowBuff() // Pass the showBuff value
            );
        }
    }


    private void addSubdomainEntry(String name, ItemStack icon, String buff, boolean locked, boolean showBuff) {
        SubdomainEntry entry = new SubdomainEntry(this, subdomainPanel.getWidth() - 0, 30, name, icon, buff, locked, showBuff);

        if (!locked) {
            entry.setSelected(name.equals(selectedSubdomain));
            entry.setClickListener(() -> {
                selectSubdomain(name);
            });
        }

        subdomainEntries.add(entry);
        subdomainPanel.addEntry(entry);
    }

    private void addSubdomainEntry(String name, ItemStack icon, String buff, boolean locked) {
        SubdomainEntry entry = new SubdomainEntry(this, subdomainPanel.getWidth() - 0, 30, name, icon, buff, locked);

        if (!locked) {
            entry.setSelected(name.equals(selectedSubdomain));
            entry.setClickListener(() -> {
                selectSubdomain(name);
            });
        }

        subdomainEntries.add(entry);
        subdomainPanel.addEntry(entry);
    }

    private void initConfirmationStage() {
        // No special elements needed for confirmation stage
    }

    private void selectDomain(String domain) {
        this.selectedDomain = domain;

        for (DomainEntry entry : domainEntries) {
            entry.setSelected(entry.getName().equals(domain));
        }

        updateButtonStates();
    }
    private void selectSubdomain(String subdomain) {
        this.selectedSubdomain = subdomain;

        for (SubdomainEntry entry : subdomainEntries) {
            entry.setSelected(entry.getName().equals(subdomain));
        }

        updateButtonStates();
    }

    private void updateButtonStates() {
        // Update back button
        backButton.active = currentStage != CreationStage.NAME_INPUT;

        // Update next button
        switch (currentStage) {
            case NAME_INPUT:
                nextButton.active = nameField != null && !nameField.getValue().isEmpty();
                nextButton.setMessage(Component.literal("Next"));
                break;
            case DOMAIN_SELECTION:
                nextButton.active = !selectedDomain.isEmpty();
                nextButton.setMessage(Component.literal("Next"));
                break;
            case SUBDOMAIN_SELECTION:
                nextButton.active = !selectedSubdomain.isEmpty();
                nextButton.setMessage(Component.literal("Next"));
                break;
            case CONFIRMATION:
                nextButton.active = true;
                nextButton.setMessage(Component.literal("Create Company"));
                break;
        }
    }

    private void goNext() {
        switch (currentStage) {
            case NAME_INPUT:
                companyName = nameField.getValue();
                currentStage = CreationStage.DOMAIN_SELECTION;
                break;
            case DOMAIN_SELECTION:
                currentStage = CreationStage.SUBDOMAIN_SELECTION;
                break;
            case SUBDOMAIN_SELECTION:
                currentStage = CreationStage.CONFIRMATION;
                break;
            case CONFIRMATION:
                createCompany();
                return;
        }

        initStageElements();
    }

    private void goBack() {
        switch (currentStage) {
            case DOMAIN_SELECTION:
                currentStage = CreationStage.NAME_INPUT;
                break;
            case SUBDOMAIN_SELECTION:
                currentStage = CreationStage.DOMAIN_SELECTION;
                break;
            case CONFIRMATION:
                currentStage = CreationStage.SUBDOMAIN_SELECTION;
                break;
            default:
                return;
        }

        initStageElements();
    }

    private void createCompany() {
        // Send company creation data to server
        NetworkHandler.sendToServer(new CompanyCreatePacket(companyName, selectedDomain, selectedSubdomain));

        // Print company details to chat for testing
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§aSending company creation data to server:"));
            minecraft.player.sendSystemMessage(Component.literal("§7Name: §f" + companyName));
            minecraft.player.sendSystemMessage(Component.literal("§7Domain: §f" + selectedDomain));
            minecraft.player.sendSystemMessage(Component.literal("§7Subdomain: §f" + selectedSubdomain));
        }

        // Close the screen
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBackground(graphics);

        // Draw semi-transparent dark background
        graphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xC0101010);

        // Draw header bar
        graphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + 20, 0xD0303030);

        // Draw title
        graphics.drawCenteredString(font, this.title, guiLeft + xSize / 2, guiTop + 6, 0xFFFFFF);

        // Render stage title
        String stageTitle = "";
        switch (currentStage) {
            case NAME_INPUT:
                stageTitle = "Enter Company Name";
                break;
            case DOMAIN_SELECTION:
                stageTitle = "Select Company Domain";
                break;
            case SUBDOMAIN_SELECTION:
                stageTitle = "Select Company Subdomain";
                break;
            case CONFIRMATION:
                stageTitle = "Confirm Company Creation";
                break;
        }

        graphics.drawCenteredString(font, stageTitle, guiLeft + xSize / 2, guiTop + 30, 0xFFFFFF);

        // Render stage-specific content
        switch (currentStage) {
            case NAME_INPUT:
                graphics.drawCenteredString(font, "Enter the name for your new company:",
                        guiLeft + xSize / 2, guiTop + 60, 0xAAAAAA);
                break;
            case DOMAIN_SELECTION:
                // Set a scissor to prevent rendering outside the panel area
                enableScissor(graphics, guiLeft + 20, guiTop + 40, xSize - 40, ySize - 90);
                domainPanel.render(graphics, mouseX, mouseY, partialTick);
                disableScissor(graphics);
                break;
            case SUBDOMAIN_SELECTION:
                // Set a scissor to prevent rendering outside the panel area
                enableScissor(graphics, guiLeft + 20, guiTop + 40, xSize - 40, ySize - 90);
                subdomainPanel.render(graphics, mouseX, mouseY, partialTick);
                disableScissor(graphics);
                break;
            case CONFIRMATION:
                renderConfirmationScreen(graphics);
                break;
        }

        // Render progress indicator (now below the panels)
        progressIndicator.render(graphics, mouseX, mouseY, partialTick);

        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderConfirmationScreen(GuiGraphics graphics) {
        int y = guiTop + 60;
        int leftCol = guiLeft + 40;
        int rightCol = guiLeft + 120;

        // Render company details
        graphics.drawString(font, "Company Name:", leftCol, y, 0xFFFFFF);
        graphics.drawString(font, companyName, rightCol, y, 0xAAAAAA);
        y += 20;

        graphics.drawString(font, "Domain:", leftCol, y, 0xFFFFFF);
        graphics.drawString(font, selectedDomain, rightCol, y, 0xAAAAAA);
        y += 20;

        graphics.drawString(font, "Subdomain:", leftCol, y, 0xFFFFFF);
        graphics.drawString(font, selectedSubdomain, rightCol, y, 0xAAAAAA);
        y += 30;

        graphics.drawCenteredString(font, "Please confirm your company details",
                guiLeft + xSize / 2, y, 0xFFFFFF);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close the screen when escape is pressed
        if (keyCode == 256) { // 256 is the key code for escape
            this.onClose();
            return true;
        }

        // Handle name input field
        if (currentStage == CreationStage.NAME_INPUT && nameField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) { // Enter key
                if (!nameField.getValue().isEmpty()) {
                    goNext();
                    return true;
                }
            }

            boolean result = nameField.keyPressed(keyCode, scanCode, modifiers);
            updateButtonStates();
            return result;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentStage == CreationStage.DOMAIN_SELECTION) {
            domainPanel.mouseClicked(mouseX, mouseY, button);
        } else if (currentStage == CreationStage.SUBDOMAIN_SELECTION) {
            subdomainPanel.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentStage == CreationStage.DOMAIN_SELECTION) {
            domainPanel.mouseScrolled(mouseX, mouseY, delta);
        } else if (currentStage == CreationStage.SUBDOMAIN_SELECTION) {
            subdomainPanel.mouseScrolled(mouseX, mouseY, delta);
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void enableScissor(GuiGraphics graphics, int x, int y, int width, int height) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int scaledX = (int)(x * scale);
        int scaledY = (int)(Minecraft.getInstance().getWindow().getHeight() - (y + height) * scale);
        int scaledWidth = (int)(width * scale);
        int scaledHeight = (int)(height * scale);

        RenderSystem.enableScissor(scaledX, scaledY, scaledWidth, scaledHeight);
    }

    private void disableScissor(GuiGraphics graphics) {
        RenderSystem.disableScissor();
    }
}

