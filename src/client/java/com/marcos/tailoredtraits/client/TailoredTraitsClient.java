package com.marcos.tailoredtraits.client;

import com.marcos.tailoredtraits.TailoredTraits;
import com.mojang.blaze3d.platform.InputConstants;
import com.marcos.tailoredtraits.client.screen.SteviumMatrixScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

public class TailoredTraitsClient implements ClientModInitializer {

    /*
     * Categoria que aparecerá na tela de controles do Minecraft.
     */
    private static final KeyMapping.Category TAILORED_TRAITS_CATEGORY =
        KeyMapping.Category.register(
            TailoredTraits.id("general")
        );

    /*
     * Tecla usada para abrir a Matriz Stevium.
     */
    private static final KeyMapping OPEN_STEVIUM_MENU =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.tailored-traits.open_stevium_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                TAILORED_TRAITS_CATEGORY
            )
        );

    @Override
    public void onInitializeClient() {
        registerKeyEvents();

        TailoredTraits.LOGGER.info(
            "Sistema cliente do Tailored Traits iniciado."
        );
    }

    private static void registerKeyEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(
            client -> {

                while (
                    OPEN_STEVIUM_MENU.consumeClick()
                ) {
                    if (client.player == null) {
                        continue;
                    }

                    client.setScreenAndShow(
                        new SteviumMatrixScreen()
                    );
                }
            }
        );
    }
}