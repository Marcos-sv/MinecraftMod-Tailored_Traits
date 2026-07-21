package com.marcos.tailoredtraits;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.item.ModItems;
import com.marcos.tailoredtraits.network.ModNetworking;
import com.marcos.tailoredtraits.power.PassivePowerHandler;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TailoredTraits
    implements ModInitializer {

    public static final String MOD_ID =
        "tailored-traits";

    public static final Logger LOGGER =
        LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        /*
         * A ordem é importante:
         *
         * 1. componentes;
         * 2. itens;
         * 3. comunicação de rede.
         */
        ModComponents.initialize();
        ModItems.initialize();
        ModNetworking.initialize();
		PassivePowerHandler.initialize();

        LOGGER.info(
            "Tailored Traits foi iniciado."
        );
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(
            MOD_ID,
            path
        );
    }
}