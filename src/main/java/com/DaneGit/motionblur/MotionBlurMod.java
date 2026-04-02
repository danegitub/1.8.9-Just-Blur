package com.DaneGit.motionblur;

import com.DaneGit.motionblur.config.BlurConfig;
import com.DaneGit.motionblur.gui.BlurGui;
import com.DaneGit.motionblur.render.BasicBlurRenderer;
import com.DaneGit.motionblur.render.FaithfulBlurRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "motionblur", name = "Motion Blur", version = "1.4")
public class MotionBlurMod {

    public static final BlurConfig config = new BlurConfig();

    private static final BasicBlurRenderer BASIC = new BasicBlurRenderer();
    private static final FaithfulBlurRenderer FAITHFUL = new FaithfulBlurRenderer();

    private KeyBinding toggleKey;
    private KeyBinding guiKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        toggleKey = new KeyBinding("Toggle Motion Blur", Keyboard.KEY_B, "Motion Blur");
        guiKey = new KeyBinding("Open Blur Settings", Keyboard.KEY_RSHIFT, "Motion Blur");

        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        config.load();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            config.enabled = !config.enabled;
            config.save();
        }

        if (guiKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new BlurGui());
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!config.enabled) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        // FIXED: removed event argument
        if (config.useFaithful) {
            FAITHFUL.render();
        } else {
            BASIC.render();
        }
    }
}