package com.marcos.tailoredtraits.mixin;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.power.PowerMaterial;
import com.marcos.tailoredtraits.power.SteviumArmorUtil;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTradeDiscountMixin {

    /*
     * Calça configurada como Esmeralda:
     *
     * reduz em duas unidades o custo principal
     * de cada troca do aldeão.
     */
    private static final int
        EMERALD_LEGGINGS_DISCOUNT =
            2;

    /*
     * Botas configuradas como Esmeralda:
     *
     * reduz em mais duas unidades o custo
     * principal das trocas.
     */
    private static final int
        EMERALD_BOOTS_DISCOUNT =
            2;

    /**
     * O método updateSpecialPrices é chamado pelo
     * aldeão antes de abrir a tela de comércio.
     *
     * Nesse momento, o Minecraft já calculou:
     *
     * - reputação;
     * - procura do item;
     * - descontos vanilla;
     * - aumentos por reputação negativa.
     *
     * O Tailored Traits adiciona o desconto das
     * peças de Esmeralda depois desses cálculos.
     */
    @Inject(
        method = "updateSpecialPrices",
        at = @At("TAIL")
    )
    private void tailoredTraits$applyEmeraldArmorDiscount(
        Player player,
        CallbackInfo info
    ) {
        int totalDiscount =
            calculateEmeraldDiscount(
                player
            );

        /*
         * O jogador não possui nenhuma das
         * peças configuradas como Esmeralda.
         */
        if (totalDiscount <= 0) {
            return;
        }

        Villager villager =
            (Villager) (Object) this;

        /*
         * Percorre todas as ofertas atuais
         * do aldeão.
         */
        for (
            MerchantOffer offer :
            villager.getOffers()
        ) {
            /*
             * Um valor negativo reduz o custo
             * principal da oferta.
             *
             * O Minecraft limita automaticamente
             * o preço final ao mínimo permitido.
             */
            offer.addToSpecialPriceDiff(
                -totalDiscount
            );
        }
    }

    /**
     * Calcula o desconto fornecido pelas
     * peças configuradas como Esmeralda.
     */
    private static int calculateEmeraldDiscount(
        Player player
    ) {
        int totalDiscount =
            0;

        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasEmeraldLeggings =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.EMERALD
            );

        if (hasEmeraldLeggings) {
            totalDiscount +=
                EMERALD_LEGGINGS_DISCOUNT;
        }

        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasEmeraldBoots =
            hasSelectedMaterial(
                boots,
                PowerMaterial.EMERALD
            );

        if (hasEmeraldBoots) {
            totalDiscount +=
                EMERALD_BOOTS_DISCOUNT;
        }

        return totalDiscount;
    }

    /**
     * Verifica se a peça possui acabamento
     * de Stevium e está configurada com
     * o material solicitado.
     */
    private static boolean hasSelectedMaterial(
        ItemStack armorPiece,
        PowerMaterial expectedMaterial
    ) {
        if (
            !SteviumArmorUtil.hasSteviumTrim(
                armorPiece
            )
        ) {
            return false;
        }

        String selectedMaterial =
            armorPiece.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        return selectedMaterial.equals(
            expectedMaterial.getId()
        );
    }
}