package com.flazesmp.companies.client.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable panel that can contain multiple entries
 */
public class ScrollablePanel {
    private final Screen parent;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<PanelEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isDragging = false;

    private static final int SCROLL_AMOUNT = 12;

    public ScrollablePanel(Screen parent, int x, int y, int width, int height) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addEntry(PanelEntry entry) {
        entries.add(entry);
        updateEntryPositions();
    }

    private void updateEntryPositions() {
        int currentY = y - scrollOffset;

        for (PanelEntry entry : entries) {
            entry.setPosition(x, currentY);
            currentY += entry.getHeight() + 2; // 2 pixel gap between entries
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw panel background
        graphics.fill(x, y, x + width, y + height, 0x80000000);

        // Draw entries
        for (PanelEntry entry : entries) {
            // Only render visible entries
            if (entry.getY() + entry.getHeight() >= y && entry.getY() <= y + height) {
                entry.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        // Draw scrollbar if needed
        if (getTotalContentHeight() > height) {
            int scrollbarHeight = Math.max(20, height * height / getTotalContentHeight());
            int scrollbarY = y + (int)((float)scrollOffset / (getTotalContentHeight() - height) * (height - scrollbarHeight));

            // Scrollbar background
            graphics.fill(x + width - 8, y, x + width - 2, y + height, 0x40000000);

            // Scrollbar handle
            graphics.fill(x + width - 7, scrollbarY, x + width - 3, scrollbarY + scrollbarHeight, 0xC0808080);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is within panel bounds
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            // Check if click is on scrollbar
            if (mouseX >= x + width - 8 && mouseX <= x + width - 2) {
                isDragging = true;
                return true;
            }

            // Check if click is on an entry
            for (PanelEntry entry : entries) {
                if (entry.isMouseOver(mouseX, mouseY) && entry.getY() + entry.getHeight() >= y && entry.getY() <= y + height) {
                    entry.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Only handle scroll if mouse is over panel
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scroll((int)(-delta * SCROLL_AMOUNT));
            return true;
        }

        return false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            int totalHeight = getTotalContentHeight();
            if (totalHeight > height) {
                double scrollFactor = (double)height / totalHeight;
                scroll((int)(dragY / scrollFactor));
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
    }

    private void scroll(int amount) {
        int maxScroll = Math.max(0, getTotalContentHeight() - height);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + amount));
        updateEntryPositions();
    }

    private int getTotalContentHeight() {
        int totalHeight = 0;
        for (PanelEntry entry : entries) {
            totalHeight += entry.getHeight() + 2; // 2 pixel gap
        }
        return totalHeight;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
