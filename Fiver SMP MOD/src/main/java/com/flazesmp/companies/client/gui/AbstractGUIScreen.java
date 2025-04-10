package com.flazesmp.companies.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.flazesmp.companies.FlazeSMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;

/**
 * Base class for all custom GUI screens
 */
public abstract class AbstractGUIScreen extends Screen {
    // GUI dimensions
    protected int xSize;
    protected int ySize;
    protected int guiLeft;
    protected int guiTop;

    // Background texture - can be null for solid color background
    protected ResourceLocation backgroundTexture;

    // Colors
    protected int backgroundColor = 0xC0101010;
    protected int headerColor = 0xD0303030;
    protected int titleColor = 0xFFFFFF;

    public AbstractGUIScreen(Component title, int width, int height) {
        super(title);
        this.xSize = width;
        this.ySize = height;
    }

    public AbstractGUIScreen(Component title, int width, int height, ResourceLocation backgroundTexture) {
        this(title, width, height);
        this.backgroundTexture = backgroundTexture;
    }

    @Override
    protected void init() {
        super.init();

        // Calculate GUI position (centered)
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        // Initialize GUI elements
        initElements();
    }

    /**
     * Initialize GUI elements like buttons, text fields, etc.
     */
    protected abstract void initElements();

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render darkened background
        this.renderBackground(graphics);

        // Draw GUI background
        if (backgroundTexture != null) {
            // Use texture if provided
            graphics.blit(backgroundTexture, guiLeft, guiTop, 0, 0, xSize, ySize);
        } else {
            // Draw semi-transparent dark background
            graphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, backgroundColor);

            // Draw header bar
            graphics.fill(guiLeft, guiTop, guiLeft + xSize, guiTop + 20, headerColor);
        }

        // Draw title
        graphics.drawCenteredString(font, this.title, guiLeft + xSize / 2, guiTop + 6, titleColor);

        // Render GUI elements
        super.render(graphics, mouseX, mouseY, partialTick);

        // Render custom content
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Render custom content for this screen
     */
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * Helper method to add a standard button
     */
    protected Button addStandardButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        Button button = Button.builder(text, onPress)
                .pos(guiLeft + x, guiTop + y)
                .size(width, height)
                .build();
        this.addRenderableWidget(button);
        return button;
    }

    /**
     * Helper method to draw a tooltip if mouse is over the specified area
     */
    protected void drawTooltipIfHovered(GuiGraphics graphics, Component tooltip, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (mouseX >= guiLeft + x && mouseX < guiLeft + x + width &&
                mouseY >= guiTop + y && mouseY < guiTop + y + height) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Helper method to check if mouse is over an area
     */
    protected boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + width &&
                mouseY >= guiTop + y && mouseY < guiTop + y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
