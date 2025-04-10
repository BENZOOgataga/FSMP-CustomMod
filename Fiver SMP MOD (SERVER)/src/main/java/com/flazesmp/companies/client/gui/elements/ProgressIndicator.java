package com.flazesmp.companies.client.gui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class ProgressIndicator {
    private final Screen parent;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int steps;
    private int currentStep = 0;

    public ProgressIndicator(Screen parent, int x, int y, int width, int height, int steps) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.steps = steps;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        graphics.fill(x, y, x + width, y + height, 0x80000000);

        // Calculate step width
        int stepWidth = width / steps;

        // Draw completed steps
        for (int i = 0; i <= currentStep; i++) {
            int stepX = x + i * stepWidth;
            int nextStepX = i < steps - 1 ? x + (i + 1) * stepWidth : x + width;

            // Draw step
            graphics.fill(stepX, y, nextStepX, y + height, 0xFF00AA00);

            // Draw separator
            if (i < steps - 1) {
                graphics.fill(nextStepX - 1, y, nextStepX, y + height, 0xFF000000);
            }
        }
    }

    public void setCurrentStep(int step) {
        this.currentStep = Math.max(0, Math.min(steps - 1, step));
    }

    public int getCurrentStep() {
        return currentStep;
    }
}