package com.DaneGit.motionblur.render;

import com.DaneGit.motionblur.MotionBlurMod;
import com.DaneGit.motionblur.gui.BlurGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class BasicBlurRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static int texture = -1;
    private static int width = -1;
    private static int height = -1;

    private static int refreshFrame = 0;

    private static long lastFpsUpdate = 0L;
    private static int cachedFps = 60;

    private static float lastYaw = 0.0F;
    private static float lastPitch = 0.0F;

    public void render() {
        if (Minecraft.getDebugFPS() < 35) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof BlurGui)) return;

        int w = mc.displayWidth;
        int h = mc.displayHeight;

        if (texture == -1 || width != w || height != h) {
            if (texture != -1) {
                GlStateManager.deleteTexture(texture);
            }

            texture = GlStateManager.generateTexture();
            width = w;
            height = h;

            setup(texture, width, height);
            copy();
            updateCameraHistory();
            return;
        }

        double motion = getMotion();
        float cameraDelta = getCameraDelta();

        if (motion < 0.01D && !MotionBlurMod.config.cameraBased) {
            return;
        }

        if (cameraDelta < 0.10F && !MotionBlurMod.config.adaptive && motion < 0.01D) {
            return;
        }

        double alpha = getStrength(motion, cameraDelta);
        if (alpha < 0.02D) {
            if (shouldRefreshBuffer()) {
                copy();
            }
            return;
        }

        draw(texture, alpha);

        if (shouldRefreshBuffer()) {
            copy();
        }
    }

    private static void setup(int tex, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGB,
                w,
                h,
                0,
                GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static void copy() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                0,
                0,
                width,
                height
        );
    }

    private static void draw(int tex, double alpha) {
        ScaledResolution sr = new ScaledResolution(mc);
        double sw = sr.getScaledWidth_double();
        double sh = sr.getScaledHeight_double();

        GlStateManager.pushMatrix();
        mc.entityRenderer.setupOverlayRendering();

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glColor4d(1.0D, 1.0D, 1.0D, alpha);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0.0D, sh, 0.0D).tex(0.0D, 0.0D).endVertex();
        wr.pos(sw, sh, 0.0D).tex(1.0D, 0.0D).endVertex();
        wr.pos(sw, 0.0D, 0.0D).tex(1.0D, 1.0D).endVertex();
        wr.pos(0.0D, 0.0D, 0.0D).tex(0.0D, 1.0D).endVertex();
        t.draw();

        GL11.glColor4d(1.0D, 1.0D, 1.0D, 1.0D);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();

        GlStateManager.popMatrix();
    }

    private static double getStrength(double motion, float cameraDelta) {
        double strength = MotionBlurMod.config.strength;

        if (MotionBlurMod.config.adaptive) {
            double fpsFactor = Math.min(1.0D, getFps() / 120.0D);
            double moveFactor = Math.min(1.0D, motion * 8.0D);
            double cameraFactor = MotionBlurMod.config.cameraBased ? Math.min(1.0D, cameraDelta / 30.0D) : 0.0D;

            strength = strength * (0.40D + moveFactor * 0.40D + cameraFactor * 0.20D) * fpsFactor;
        } else if (MotionBlurMod.config.cameraBased) {
            strength = strength * (0.75D + Math.min(1.0D, cameraDelta / 30.0D) * 0.25D);
        }

        if (mc.currentScreen != null) {
            strength *= 0.75D;
        }

        if (MotionBlurMod.config.halfResolution) {
            strength *= 0.92D;
        }

        return clamp(strength, 0.0D, 0.70D);
    }

    private static boolean shouldRefreshBuffer() {
        int fps = getFps();
        int interval = 1;

        if (MotionBlurMod.config.frameSkipping) {
            if (fps < 25) interval = 4;
            else if (fps < 40) interval = 3;
            else if (fps < 60) interval = 2;
        }

        if (MotionBlurMod.config.halfResolution) {
            interval = Math.max(interval, 2);
        }

        return (refreshFrame++ % interval) == 0;
    }

    private static int getFps() {
        long now = System.currentTimeMillis();
        if (now - lastFpsUpdate > 200L) {
            cachedFps = Minecraft.getDebugFPS();
            lastFpsUpdate = now;
        }
        return cachedFps <= 0 ? 60 : cachedFps;
    }

    private static double getMotion() {
        return Math.abs(mc.thePlayer.motionX)
                + Math.abs(mc.thePlayer.motionY)
                + Math.abs(mc.thePlayer.motionZ);
    }

    private static float getCameraDelta() {
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;

        float dy = Math.abs(wrapAngleTo180(yaw - lastYaw));
        float dp = Math.abs(pitch - lastPitch);

        lastYaw = yaw;
        lastPitch = pitch;

        return dy + dp;
    }

    private static void updateCameraHistory() {
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
    }

    private static float wrapAngleTo180(float angle) {
        while (angle >= 180.0F) angle -= 360.0F;
        while (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}