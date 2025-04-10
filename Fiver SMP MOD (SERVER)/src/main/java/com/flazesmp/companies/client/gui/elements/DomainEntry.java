/**
 * Domain entry for the domain selection panel
 */
package com.flazesmp.companies.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class DomainEntry extends PanelEntry {
    private final Screen parent;
    private final String name;
    private final ItemStack icon;
    private final String description;
    private boolean selected = false;
    private Runnable clickListener;

    public DomainEntry(Screen parent, int width, int height, String name, ItemStack icon, String description) {
        super(width, height);
        this.parent = parent;
        this.name = name;
        this.icon = icon;
        this.description = description;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        int bgColor = selected ? 0xA0505050 : isMouseOver(mouseX, mouseY) ? 0x80404040 : 0x80303030;
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Draw border
        if (selected) {
            graphics.fill(x, y, x + width, y + 1, 0xFFAAAAAA); // Top
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFAAAAAA); // Bottom
            graphics.fill(x, y, x + 1, y + height, 0xFFAAAAAA); // Left
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFFAAAAAA); // Right
        }

        // Draw icon
        graphics.renderItem(icon, x + 5, y + (height - 16) / 2);

        // Draw name
        graphics.drawString(Minecraft.getInstance().font, name, x + 30, y + 6, 0xFFFFFF);

        // Draw description
        graphics.drawString(Minecraft.getInstance().font, description, x + 30, y + 18, 0xAAAAAA);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickListener != null) {
            clickListener.run();
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setClickListener(Runnable clickListener) {
        this.clickListener = clickListener;
    }

    public String getName() {
        return name;
    }
}
