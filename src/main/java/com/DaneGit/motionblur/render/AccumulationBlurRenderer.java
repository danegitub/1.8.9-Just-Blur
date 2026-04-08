package com.DaneGit.motionblur.render;

import com.DaneGit.motionblur.MotionBlurMod;
import com.DaneGit.motionblur.gui.BlurGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class AccumulationBlurRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static int texture = -1;
    private static int width = -1;
    private static int height = -1;

    private static int refreshFrame = 0;

    private static float lastYaw = 0F;
    private static float lastPitch = 0F;

    public void render() {

        if (Minecraft.getDebugFPS() < 35) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof BlurGui)) return;

        int w = mc.displayWidth;
        int h = mc.displayHeight;

        // texture setup
        if (texture == -1 || width != w || height != h) {

            if (texture != -1) {
                GlStateManager.deleteTexture(texture);
            }

            texture = GlStateManager.generateTexture();
            width = w;
            height = h;

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w, h, 0,
                    GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            copy();
            updateCamera();
            return;
        }

        double motion = getMotion();
        float cam = getCameraDelta();

        // early exit
        if (motion < 0.01 && !MotionBlurMod.config.cameraBased) return;

        double alpha = getAlpha(motion, cam);
        if (alpha < 0.02) {
            if (shouldRefresh()) copy();
            return;
        }

        // ===== PASS 1: DECAY (fade previous frame) =====
        decayPass();

        // ===== PASS 2: BLEND PREVIOUS FRAME =====
        draw(texture, alpha);

        // ===== PASS 3: CAPTURE NEW FRAME =====
        if (shouldRefresh()) {
            copy();
        }
    }

    // ===== DECAY PASS (REMOVES GHOSTING + DARKENING) =====
    private void decayPass() {

        ScaledResolution sr = new ScaledResolution(mc);

        GlStateManager.pushMatrix();
        mc.entityRenderer.setupOverlayRendering();

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glEnable(GL11.GL_BLEND);

        // IMPORTANT: multiplicative fade (prevents dark screen bug)
        GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);

        double decay = MotionBlurMod.config.accumulationDecay;

        GL11.glColor4d(1, 1, 1, decay);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(0, sr.getScaledHeight_double(), 0).endVertex();
        wr.pos(sr.getScaledWidth_double(), sr.getScaledHeight_double(), 0).endVertex();
        wr.pos(sr.getScaledWidth_double(), 0, 0).endVertex();
        wr.pos(0, 0, 0).endVertex();
        t.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.popMatrix();
    }

    private void draw(int tex, double alpha) {

        ScaledResolution sr = new ScaledResolution(mc);

        GlStateManager.pushMatrix();
        mc.entityRenderer.setupOverlayRendering();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glColor4d(1, 1, 1, alpha);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0, sr.getScaledHeight_double(), 0).tex(0, 0).endVertex();
        wr.pos(sr.getScaledWidth_double(), sr.getScaledHeight_double(), 0).tex(1, 0).endVertex();
        wr.pos(sr.getScaledWidth_double(), 0, 0).tex(1, 1).endVertex();
        wr.pos(0, 0, 0).tex(0, 1).endVertex();
        t.draw();

        GL11.glColor4d(1, 1, 1, 1);

        GlStateManager.popMatrix();
    }

    private void copy() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    private boolean shouldRefresh() {
        int interval = 1;

        if (MotionBlurMod.config.frameSkipping) {
            int fps = Minecraft.getDebugFPS();

            if (fps < 30) interval = 4;
            else if (fps < 45) interval = 3;
            else if (fps < 60) interval = 2;
        }

        if (MotionBlurMod.config.halfResolution) {
            interval = Math.max(interval, 2);
        }

        return (refreshFrame++ % interval) == 0;
    }

    private double getAlpha(double motion, float cam) {

        double alpha = MotionBlurMod.config.strength * 0.45;

        if (MotionBlurMod.config.adaptive) {
            double move = Math.min(1.0, motion * 8.0);
            double camF = MotionBlurMod.config.cameraBased ? Math.min(1.0, cam / 30.0) : 0.0;

            alpha *= (0.5 + move * 0.3 + camF * 0.2);
        }

        if (mc.currentScreen != null) {
            alpha *= 0.75;
        }

        return clamp(alpha, 0.0, 0.55);
    }

    private double getMotion() {
        return Math.abs(mc.thePlayer.motionX)
                + Math.abs(mc.thePlayer.motionY)
                + Math.abs(mc.thePlayer.motionZ);
    }

    private float getCameraDelta() {

        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;

        float dy = Math.abs(wrap(yaw - lastYaw));
        float dp = Math.abs(pitch - lastPitch);

        lastYaw = yaw;
        lastPitch = pitch;

        return dy + dp;
    }

    private void updateCamera() {
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
    }

    private float wrap(float v) {
        while (v >= 180) v -= 360;
        while (v < -180) v += 360;
        return v;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}