package com.DaneGit.motionblur.gui;

import com.DaneGit.motionblur.MotionBlurMod;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class BlurGui extends GuiScreen {

    private int sliderX;
    private int sliderY;
    private int sliderWidth = 220;

    private double visualStrength; // smooth animation
    private boolean dragging = false;

    @Override
    public void initGui() {
        sliderX = width / 2 - sliderWidth / 2;
        sliderY = height / 2 + 10;

        visualStrength = MotionBlurMod.config.strength;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isHoveringSlider(mouseX, mouseY)) {
            dragging = true;
        }

        // toggle zones (cinematic style, no buttons)
        if (mouseY > height / 2 - 10 && mouseY < height / 2 + 5) {
            if (mouseX < width / 2) {
                MotionBlurMod.config.enabled = !MotionBlurMod.config.enabled;
            } else {
                MotionBlurMod.config.useFaithful = !MotionBlurMod.config.useFaithful;
            }
        }

        if (mouseY > height / 2 + 40 && mouseY < height / 2 + 55) {
            MotionBlurMod.config.adaptive = !MotionBlurMod.config.adaptive;
        }

        MotionBlurMod.config.save();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        // -------- subtle background (not pure black) --------
        drawRect(0, 0, width, height, 0xAA101010);

        // -------- smooth slider animation --------
        visualStrength += (MotionBlurMod.config.strength - visualStrength) * 0.15;

        // -------- title --------
        drawCenteredString(fontRendererObj,
                "Motion Blur",
                width / 2,
                height / 2 - 60,
                0xFFFFFF);

        // -------- toggles (clean text layout) --------
        drawCenteredString(fontRendererObj,
                "Enabled: " + (MotionBlurMod.config.enabled ? "ON" : "OFF"),
                width / 2 - 80,
                height / 2 - 5,
                MotionBlurMod.config.enabled ? 0x55FF55 : 0xFF5555);

        drawCenteredString(fontRendererObj,
                "Mode: " + (MotionBlurMod.config.useFaithful ? "Faithful" : "Basic"),
                width / 2 + 80,
                height / 2 - 5,
                0xAAAAFF);

        drawCenteredString(fontRendererObj,
                "Adaptive: " + (MotionBlurMod.config.adaptive ? "ON" : "OFF"),
                width / 2,
                height / 2 + 45,
                0xFFFFFF);

        // -------- slider logic --------
        if (dragging) {
            double percent = (double)(mouseX - sliderX) / sliderWidth;
            percent = clamp(percent);

            MotionBlurMod.config.strength = percent;
        }

        // -------- slider background --------
        drawRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + 4, 0xFF333333);

        // -------- animated fill --------
        int fill = (int)(sliderX + visualStrength * sliderWidth);
        drawRect(sliderX, sliderY, fill, sliderY + 4, 0xFFFFFFFF);

        // -------- knob --------
        drawRect(fill - 2, sliderY - 2, fill + 2, sliderY + 6, 0xFFFFFFFF);

        // -------- value text --------
        drawCenteredString(fontRendererObj,
                "Strength: " + format(visualStrength),
                width / 2,
                sliderY - 12,
                0xDDDDDD);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean isHoveringSlider(int mouseX, int mouseY) {
        return mouseX >= sliderX && mouseX <= sliderX + sliderWidth
                && mouseY >= sliderY - 4 && mouseY <= sliderY + 8;
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String format(double v) {
        return String.format("%.2f", v);
    }
}