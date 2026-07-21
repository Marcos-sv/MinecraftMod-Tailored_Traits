package com.marcos.tailoredtraits.item;

import java.util.function.Function;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.trim.ModTrimMaterials;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {

    /*
     * Materiais principais do Tailored Traits.
     */

    public static final Item MINEUM = register(
        "mineum",
        Item::new,
        new Item.Properties()
    );

    public static final Item CRAFTIUM = register(
        "craftium",
        Item::new,
        new Item.Properties()
    );

    public static final Item STEVIUM = register(
        "stevium",
        Item::new,
        new Item.Properties()
            .trimMaterial(ModTrimMaterials.STEVIUM)
    );

    private ModItems() {
        /*
         * Impede a criação de objetos desta classe.
         * Todos os itens são acessados de forma estática.
         */
    }

    /**
     * Registra um item no registro oficial do Minecraft.
     */
    private static <T extends Item> T register(
        String name,
        Function<Item.Properties, T> itemFactory,
        Item.Properties properties
    ) {
        ResourceKey<Item> itemKey = ResourceKey.create(
            Registries.ITEM,
            TailoredTraits.id(name)
        );

        T item = itemFactory.apply(
            properties.setId(itemKey)
        );

        Registry.register(
            BuiltInRegistries.ITEM,
            itemKey,
            item
        );

        return item;
    }

    /**
     * Inicializa os itens e adiciona todos à aba de ingredientes.
     */
    public static void initialize() {
        CreativeModeTabEvents
            .modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> {
                entries.accept(MINEUM);
                entries.accept(CRAFTIUM);
                entries.accept(STEVIUM);
            });

        TailoredTraits.LOGGER.info(
            "Itens do Tailored Traits registrados."
        );
    }
}