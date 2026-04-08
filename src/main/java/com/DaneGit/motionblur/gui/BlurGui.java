package com.DaneGit.motionblur.gui;

import com.DaneGit.motionblur.MotionBlurMod;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class BlurGui extends GuiScreen {

    private int panelX, panelY, panelW = 260, panelH = 230;

    private int sliderX, sliderY, sliderW = 200;
    private boolean dragging = false;

    private int decaySliderY;
    private boolean draggingDecay = false;

    @Override
    public void initGui() {
        panelX = width / 2 - panelW / 2;
        panelY = height / 2 - panelH / 2;

        sliderX = panelX + 30;
        sliderW = 200;

        // Put sliders BELOW all text rows
        sliderY = panelY + 165;
        decaySliderY = panelY + 200;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);

        if (hover(mx, my, panelY + 35)) {
            MotionBlurMod.config.enabled = !MotionBlurMod.config.enabled;
            MotionBlurMod.config.save();
            return;
        }

        if (hover(mx, my, panelY + 55)) {
            MotionBlurMod.config.mode++;
            if (MotionBlurMod.config.mode > 2) MotionBlurMod.config.mode = 0;
            MotionBlurMod.config.save();
            return;
        }

        if (hover(mx, my, panelY + 75)) {
            MotionBlurMod.config.adaptive = !MotionBlurMod.config.adaptive;
            MotionBlurMod.config.save();
            return;
        }

        if (hover(mx, my, panelY + 95)) {
            MotionBlurMod.config.cameraBased = !MotionBlurMod.config.cameraBased;
            MotionBlurMod.config.save();
            return;
        }

        if (hover(mx, my, panelY + 115)) {
            MotionBlurMod.config.halfResolution = !MotionBlurMod.config.halfResolution;
            MotionBlurMod.config.save();
            return;
        }

        if (hover(mx, my, panelY + 135)) {
            MotionBlurMod.config.frameSkipping = !MotionBlurMod.config.frameSkipping;
            MotionBlurMod.config.save();
            return;
        }

        if (hoverSlider(mx, my)) {
            dragging = true;
            updateStrength(mx);
            MotionBlurMod.config.save();
        }

        if (hoverDecay(mx, my) && MotionBlurMod.config.mode == 2) {
            draggingDecay = true;
            updateDecay(mx);
            MotionBlurMod.config.save();
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        dragging = false;
        draggingDecay = false;
        MotionBlurMod.config.save();
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        drawDefaultBackground();

        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA101010);
        drawCenteredString(fontRendererObj, "Motion Blur", width / 2, panelY + 10, 0xFFFFFF);

        drawRow("Enabled", MotionBlurMod.config.enabled, panelY + 35);
        drawMode(panelY + 55);
        drawRow("Adaptive", MotionBlurMod.config.adaptive, panelY + 75);
        drawRow("Camera Based", MotionBlurMod.config.cameraBased, panelY + 95);
        drawRow("Half Resolution", MotionBlurMod.config.halfResolution, panelY + 115);
        drawRow("Frame Skipping", MotionBlurMod.config.frameSkipping, panelY + 135);

        // Strength slider
        if (dragging) {
            updateStrength(mx);
        }

        drawCenteredString(fontRendererObj,
                "Strength: " + format(MotionBlurMod.config.strength),
                width / 2,
                sliderY - 12,
                0xDDDDDD);

        drawRect(sliderX, sliderY, sliderX + sliderW, sliderY + 4, 0xFF333333);

        int fill = (int) (sliderX + MotionBlurMod.config.strength * sliderW);
        drawRect(sliderX, sliderY, fill, sliderY + 4, 0xFFFFFFFF);

        // Decay slider only for Accumulation
        if (MotionBlurMod.config.mode == 2) {
            if (draggingDecay) {
                updateDecay(mx);
            }

            drawCenteredString(fontRendererObj,
                    "Persistence: " + format(MotionBlurMod.config.accumulationDecay),
                    width / 2,
                    decaySliderY - 12,
                    0xDDDDDD);

            drawRect(sliderX, decaySliderY, sliderX + sliderW, decaySliderY + 4, 0xFF333333);

            double percent = (MotionBlurMod.config.accumulationDecay - 0.04) / 0.12;
            percent = clamp(percent);
            int fill2 = (int) (sliderX + percent * sliderW);

            drawRect(sliderX, decaySliderY, fill2, decaySliderY + 4, 0xFFFFFFFF);
        }

        super.drawScreen(mx, my, partialTicks);
    }

    private void drawRow(String name, boolean state, int y) {
        drawCenteredString(fontRendererObj,
                name + ": " + (state ? "ON" : "OFF"),
                width / 2,
                y,
                state ? 0x55FF55 : 0xFF5555);
    }

    private void drawMode(int y) {
        String name = "Basic";
        if (MotionBlurMod.config.mode == 1) name = "Faithful";
        if (MotionBlurMod.config.mode == 2) name = "Accumulation";

        drawCenteredString(fontRendererObj,
                "Mode: " + name,
                width / 2,
                y,
                0xAAAAFF);
    }

    private boolean hover(int mx, int my, int y) {
        return mx > panelX && mx < panelX + panelW && my > y - 2 && my < y + 10;
    }

    private boolean hoverSlider(int mx, int my) {
        return mx >= sliderX && mx <= sliderX + sliderW
                && my >= sliderY - 4 && my <= sliderY + 8;
    }

    private boolean hoverDecay(int mx, int my) {
        return mx >= sliderX && mx <= sliderX + sliderW
                && my >= decaySliderY - 4 && my <= decaySliderY + 8;
    }

    private void updateStrength(int mx) {
        double p = (mx - sliderX) / (double) sliderW;
        MotionBlurMod.config.strength = clamp(p);
    }

    private void updateDecay(int mx) {
        double p = (mx - sliderX) / (double) sliderW;
        p = clamp(p);
        MotionBlurMod.config.accumulationDecay = 0.04 + p * 0.12;
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private String format(double v) {
        return String.format("%.2f", v);
    }
}