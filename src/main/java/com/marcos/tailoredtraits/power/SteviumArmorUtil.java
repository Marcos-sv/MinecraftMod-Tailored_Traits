package com.marcos.tailoredtraits.power;

import com.marcos.tailoredtraits.trim.ModTrimMaterials;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public final class SteviumArmorUtil {

    private SteviumArmorUtil() {
    }

    /**
     * Verifica se a peça possui um enfeite
     * cujo material é Stevium.
     */
    public static boolean hasSteviumTrim(
        ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ArmorTrim trim =
            stack.get(DataComponents.TRIM);

        return trim != null
            && trim.material().is(
                ModTrimMaterials.STEVIUM
            );
    }
}