package com.marcos.tailoredtraits.mixin;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.power.PowerMaterial;
import com.marcos.tailoredtraits.power.SteviumArmorUtil;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerResinBootsMixin {

    /**
     * O Minecraft chama isStayingOnGroundSurface()
     * antes de aplicar o movimento do jogador.
     *
     * Normalmente esse método retorna verdadeiro
     * somente quando o jogador está agachado.
     *
     * Com as botas de Resina, ele também retorna
     * verdadeiro sem precisar pressionar Shift.
     */
    @Redirect(
        method = "maybeBackOffFromEdge",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/entity/player/Player;"
                    + "isStayingOnGroundSurface()Z"
        )
    )
    private boolean tailoredTraits$activateResinEdgeProtection(
        Player player
    ) {
        /*
         * Preserva o funcionamento vanilla:
         * agachar continua protegendo bordas.
         */
        if (player.isShiftKeyDown()) {
            return true;
        }

        return tailoredTraits$hasResinBootsPower(
            player
        );
    }

    /**
     * Verifica se o jogador está utilizando
     * botas com acabamento de Stevium
     * configuradas como Resina.
     */
    private static boolean tailoredTraits$hasResinBootsPower(
        Player player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        if (
            !SteviumArmorUtil.hasSteviumTrim(
                boots
            )
        ) {
            return false;
        }

        String selectedMaterial =
            boots.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        return selectedMaterial.equals(
            PowerMaterial.RESIN.getId()
        );
    }
}