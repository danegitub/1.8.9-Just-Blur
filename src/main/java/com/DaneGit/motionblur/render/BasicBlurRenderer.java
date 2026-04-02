package com.DaneGit.motionblur.render;

import com.DaneGit.motionblur.MotionBlurMod;
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

    public static void render() {

        if (!MotionBlurMod.config.enabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        int w = mc.displayWidth;
        int h = mc.displayHeight;

        if (texture == -1 || w != width || h != height) {
            texture = GlStateManager.generateTexture();
            setup(texture, w, h);
            width = w;
            height = h;

            copy(texture, w, h);
            return;
        }

        double strength = getAdaptive(MotionBlurMod.config.strength);
        strength = clamp(strength, 0.0, 0.65);

        draw(texture, strength);

        copy(texture, w, h);
    }

    private static double getAdaptive(double base) {
        if (!MotionBlurMod.config.adaptive) return base;

        int fps = Minecraft.getDebugFPS();
        double fpsFactor = fps <= 0 ? 1.0 : Math.min(1.0, fps / 120.0);

        double motion = 0;
        if (mc.thePlayer != null) {
            motion = Math.abs(mc.thePlayer.motionX)
                   + Math.abs(mc.thePlayer.motionY)
                   + Math.abs(mc.thePlayer.motionZ);
        }

        double moveFactor = Math.min(1.0, motion * 8.0);

        return base * (0.5 + moveFactor * 0.5) * fpsFactor;
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
                (ByteBuffer) null // FIXED
        );

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static void copy(int tex, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
    }

    private static void draw(int tex, double alpha) {
        if (tex == -1) return;

        ScaledResolution sr = new ScaledResolution(mc);
        double w = sr.getScaledWidth_double();
        double h = sr.getScaledHeight_double();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();

        mc.entityRenderer.setupOverlayRendering();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glColor4d(1, 1, 1, alpha);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0, h, 0).tex(0, 0).endVertex();
        wr.pos(w, h, 0).tex(1, 0).endVertex();
        wr.pos(w, 0, 0).tex(1, 1).endVertex();
        wr.pos(0, 0, 0).tex(0, 1).endVertex();
        t.draw();

        GL11.glColor4d(1, 1, 1, 1);

        GlStateManager.popMatrix();
        GL11.glPopAttrib();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}