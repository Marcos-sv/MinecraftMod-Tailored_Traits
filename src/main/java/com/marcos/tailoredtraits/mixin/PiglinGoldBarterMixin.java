package com.marcos.tailoredtraits.mixin;

import java.util.ArrayList;
import java.util.List;

import com.marcos.tailoredtraits.power
    .FullSetPowerUtil;
import com.marcos.tailoredtraits.power
    .PowerMaterial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior
    .BehaviorUtils;
import net.minecraft.world.entity.monster.piglin
    .Piglin;
import net.minecraft.world.entity.monster.piglin
    .PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback
    .CallbackInfo;

@Mixin(PiglinAi.class)
public abstract class PiglinGoldBarterMixin {

    /**
     * Intercepta o momento em que o Piglin
     * arremessa a recompensa em direção
     * ao jogador responsável pela troca.
     */
    @Inject(
        method = "throwItemsTowardPlayer",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void
        tailoredTraits$improveGoldBarter(
            Piglin piglin,
            Player player,
            List<ItemStack> originalRewards,
            CallbackInfo info
        ) {
        /*
         * Esta lógica ocorre no servidor.
         */
        if (
            !(player
                instanceof ServerPlayer
                    serverPlayer)
        ) {
            return;
        }

        /*
         * Sem o conjunto completo configurado
         * como Ouro, a troca vanilla continua
         * sem qualquer alteração.
         */
        boolean hasGoldFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    serverPlayer,
                    PowerMaterial.GOLD
                );

        if (!hasGoldFullSet) {
            return;
        }

        if (
            originalRewards == null
                || originalRewards.isEmpty()
        ) {
            return;
        }

        /*
         * Cria uma nova lista com capacidade
         * para duas cópias de cada recompensa.
         */
        List<ItemStack> improvedRewards =
            new ArrayList<>(
                originalRewards.size() * 2
            );

        for (
            ItemStack originalReward :
            originalRewards
        ) {
            if (
                originalReward == null
                    || originalReward.isEmpty()
            ) {
                continue;
            }

            /*
             * A recompensa original.
             */
            improvedRewards.add(
                originalReward
            );

            /*
             * Uma cópia completa:
             *
             * - mesmo item;
             * - mesma quantidade;
             * - mesmos componentes;
             * - mesmos encantamentos;
             * - mesmos dados internos.
             */
            improvedRewards.add(
                originalReward.copy()
            );
            improvedRewards.add(
                originalReward.copy()
            );
        }

        if (improvedRewards.isEmpty()) {
            return;
        }

        /*
         * Reproduz a animação vanilla
         * da mão secundária do Piglin.
         */
        piglin.swing(
            InteractionHand.OFF_HAND
        );

        /*
         * Os itens são lançados na direção
         * do jogador, um bloco acima da
         * posição dos seus pés.
         */
        Vec3 targetPosition =
            player.position().add(
                0.0D,
                1.0D,
                0.0D
            );

        for (
            ItemStack reward :
            improvedRewards
        ) {
            BehaviorUtils.throwItem(
                piglin,
                reward,
                targetPosition
            );
        }

        /*
         * Impede que o método vanilla
         * lance a lista original novamente.
         */
        info.cancel();
    }
}