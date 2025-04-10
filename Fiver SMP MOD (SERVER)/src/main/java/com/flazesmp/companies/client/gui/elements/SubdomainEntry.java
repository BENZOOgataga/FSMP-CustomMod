package com.flazesmp.companies.client.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Subdomain entry for the subdomain selection panel
 */
public class SubdomainEntry extends PanelEntry {
    private final Screen parent;
    private final String name;
    private final ItemStack icon;
    private final String buff;
    private final boolean locked;
    private boolean selected = false;
    private Runnable clickListener;

    public SubdomainEntry(Screen parent, int width, int height, String name, ItemStack icon, String buff, boolean locked) {
        super(width, height);
        this.parent = parent;
        this.name = name;
        this.icon = icon;
        this.buff = buff;
        this.locked = locked;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        int bgColor;
        if (locked) {
            bgColor = 0x80202020;
        } else {
            bgColor = selected ? 0xA0505050 : isMouseOver(mouseX, mouseY) ? 0x80404040 : 0x80303030;
        }
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Draw border
        if (selected) {
            graphics.fill(x, y, x + width, y + 1, 0xFFAAAAAA); // Top
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFAAAAAA); // Bottom
            graphics.fill(x, y, x + 1, y + height, 0xFFAAAAAA); // Left
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFFAAAAAA); // Right
        }

        // Draw icon
        if (locked) {
            // Draw with reduced opacity for locked items
            graphics.setColor(0.5f, 0.5f, 0.5f, 0.5f);
            graphics.renderItem(icon, x + 5, y + (height - 16) / 2);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            graphics.renderItem(icon, x + 5, y + (height - 16) / 2);
        }

        // Draw name
        int nameColor = locked ? 0x888888 : 0xFFFFFF;
        graphics.drawString(Minecraft.getInstance().font, name, x + 30, y + 6, nameColor);

        // Draw buff
        int buffColor = locked ? 0x666666 : 0xAAAAAA;
        graphics.drawString(Minecraft.getInstance().font, buff, x + 30, y + 18, buffColor);

        // Draw locked indicator
        if (locked) {
            graphics.drawString(Minecraft.getInstance().font, "Locked", x + width - 50, y + 10, 0xFF5555);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !locked && clickListener != null) {
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

    public boolean isLocked() {
        return locked;
    }
}