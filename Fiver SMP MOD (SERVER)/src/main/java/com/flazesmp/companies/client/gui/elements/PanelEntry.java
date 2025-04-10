package com.flazesmp.companies.client.gui.elements;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for panel entries
 */
public abstract class PanelEntry {
    protected int x;
    protected int y;
    protected final int width;
    protected final int height;

    public PanelEntry(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    public abstract void mouseClicked(double mouseX, double mouseY, int button);
}