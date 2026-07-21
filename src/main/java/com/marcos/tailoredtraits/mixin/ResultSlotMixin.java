package com.marcos.tailoredtraits.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    /*
     * Executado depois que o Minecraft calcula quais itens devem
     * permanecer na bancada após uma fabricação.
     */
    @Inject(
        method = "getRemainingItems",
        at = @At("RETURN"),
        cancellable = true
    )
    private void tailoredTraits$keepHeartOfTheSea(
        CraftingInput input,
        Level level,
        CallbackInfoReturnable<NonNullList<ItemStack>> info
    ) {
        /*
         * Só altera o resultado quando a grade corresponde
         * exatamente ao Mineum ou ao Craftium.
         */
        if (!isMineumRecipe(input) && !isCraftiumRecipe(input)) {
            return;
        }

        NonNullList<ItemStack> remainingItems = info.getReturnValue();

        /*
         * Encontra o Coração do Mar na grade e o coloca novamente
         * no mesmo espaço depois da fabricação.
         */
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).is(Items.HEART_OF_THE_SEA)) {
                remainingItems.set(
                    slot,
                    new ItemStack(Items.HEART_OF_THE_SEA)
                );

                break;
            }
        }

        info.setReturnValue(remainingItems);
    }

    private static boolean isMineumRecipe(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }

        return isItem(input, 0, Items.NETHERITE_INGOT)
            && isEmpty(input, 1)
            && isItem(input, 2, Items.IRON_INGOT)

            && isItem(input, 3, Items.DIAMOND)
            && isItem(input, 4, Items.HEART_OF_THE_SEA)
            && isItem(input, 5, Items.COPPER_INGOT)

            && isEmpty(input, 6)
            && isItem(input, 7, Items.GOLD_INGOT)
            && isEmpty(input, 8);
    }

    private static boolean isCraftiumRecipe(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }

        return isItem(input, 0, Items.AMETHYST_SHARD)
            && isEmpty(input, 1)
            && isItem(input, 2, Items.EMERALD)

            && isItem(input, 3, Items.QUARTZ)
            && isItem(input, 4, Items.HEART_OF_THE_SEA)
            && isItem(input, 5, Items.REDSTONE)

            && isEmpty(input, 6)
            && isItem(input, 7, Items.LAPIS_LAZULI)
            && isEmpty(input, 8);
    }

    private static boolean isItem(
        CraftingInput input,
        int slot,
        Item expectedItem
    ) {
        return input.getItem(slot).is(expectedItem);
    }

    private static boolean isEmpty(
        CraftingInput input,
        int slot
    ) {
        return input.getItem(slot).isEmpty();
    }
}