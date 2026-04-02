package com.DaneGit.motionblur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

@Mod(modid = "motionblur", name = "Motion Blur", version = "1.0")
public class MotionBlurMod {

    private final Minecraft mc = Minecraft.getMinecraft();

    private int textureLast = -1;
    private int textureAccum = -1;

    private int texWidth = -1;
    private int texHeight = -1;

    public double strength = 0.35;
    private boolean enabled = true;

    private KeyBinding toggleKey;
    private KeyBinding guiKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        toggleKey = new KeyBinding("Toggle Motion Blur", Keyboard.KEY_B, "Motion Blur");
        guiKey = new KeyBinding("Open Blur Settings", Keyboard.KEY_RSHIFT, "Motion Blur");

        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            enabled = !enabled;
            send("Motion Blur: " + (enabled ? "ON" : "OFF"));
        }

        if (guiKey.isPressed()) {
            mc.displayGuiScreen(new BlurGUI(this));
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        int displayW = mc.displayWidth;
        int displayH = mc.displayHeight;

        if (textureLast == -1 || displayW != texWidth || displayH != texHeight) {
            textureLast = GlStateManager.generateTexture();
            textureAccum = GlStateManager.generateTexture();

            setup(textureLast, displayW, displayH);
            setup(textureAccum, displayW, displayH);

            texWidth = displayW;
            texHeight = displayH;

            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        double w = sr.getScaledWidth_double();
        double h = sr.getScaledHeight_double();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();

        mc.entityRenderer.setupOverlayRendering();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        double a1 = Math.min(strength, 0.7);
        double a2 = Math.min(strength * 0.5, 0.5);

        draw(textureLast, a1, w, h);
        draw(textureAccum, a2, w, h);

        GlStateManager.popMatrix();
        GL11.glPopAttrib();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureAccum);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, displayW, displayH);

        int temp = textureLast;
        textureLast = textureAccum;
        textureAccum = temp;
    }

    private void setup(int tex, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w, h, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private void draw(int tex, double alpha, double w, double h) {
        if (tex == -1) return;

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
    }

    private void send(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(msg));
        }
    }

    // ===== GUI =====
    public static class BlurGUI extends GuiScreen {

        private final MotionBlurMod mod;
        private int sliderX;
        private int sliderWidth = 200;
        private boolean dragging = false;

        public BlurGUI(MotionBlurMod mod) {
            this.mod = mod;
        }

        @Override
        public void initGui() {
            sliderX = width / 2 - sliderWidth / 2;
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);

            int sliderY = height / 2;

            if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                mouseY >= sliderY && mouseY <= sliderY + 20) {
                dragging = true;
            }
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int state) {
            super.mouseReleased(mouseX, mouseY, state);
            dragging = false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();

            drawCenteredString(fontRendererObj, "Motion Blur Settings", width / 2, height / 2 - 40, 0xFFFFFF);

            int sliderY = height / 2;

            if (dragging) {
                double percent = (double)(mouseX - sliderX) / sliderWidth;
                percent = Math.max(0.0, Math.min(1.0, percent));
                mod.strength = percent * 0.7;
            }

            drawRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + 20, 0xFF555555);

            int knobX = (int)(sliderX + (mod.strength / 0.7) * sliderWidth);
            drawRect(knobX - 2, sliderY, knobX + 2, sliderY + 20, 0xFFFFFFFF);

            drawCenteredString(fontRendererObj,
                    "Strength: " + String.format("%.2f", mod.strength),
                    width / 2, sliderY + 25, 0xFFFFFF);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}