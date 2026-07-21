package com.marcos.tailoredtraits.component;

import com.marcos.tailoredtraits.TailoredTraits;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {

    /*
     * Guarda na peça o ID do material selecionado.
     *
     * Exemplos:
     * iron
     * redstone
     * amethyst
     */
    public static final DataComponentType<String>
        SELECTED_POWER_MATERIAL =
            Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                TailoredTraits.id(
                    "selected_power_material"
                ),
                DataComponentType
                    .<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(
                        ByteBufCodecs.STRING_UTF8
                    )
                    .build()
            );

    private ModComponents() {
    }

    public static void initialize() {
        TailoredTraits.LOGGER.info(
            "Componentes do Tailored Traits registrados."
        );
    }
}