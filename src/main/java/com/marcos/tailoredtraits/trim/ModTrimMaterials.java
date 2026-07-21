package com.marcos.tailoredtraits.trim;

import com.marcos.tailoredtraits.TailoredTraits;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public final class ModTrimMaterials {

    /*
     * Chave que aponta para:
     *
     * data/tailored-traits/trim_material/stevium.json
     */
    public static final ResourceKey<TrimMaterial> STEVIUM =
        ResourceKey.create(
            Registries.TRIM_MATERIAL,
            TailoredTraits.id("stevium")
        );

    private ModTrimMaterials() {
    }
}