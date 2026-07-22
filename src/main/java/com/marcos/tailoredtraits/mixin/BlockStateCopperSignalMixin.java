package com.marcos.tailoredtraits.mixin;

import com.marcos.tailoredtraits.power
    .CopperRedstonePowerInitializer;
import com.marcos.tailoredtraits.power
    .RedstoneFullSetPowerInitializer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state
    .BlockBehaviour;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback
    .CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCopperSignalMixin {

    /*
     * Força máxima de Redstone.
     */
    @Unique
    private static final int
        TAILORED_TRAITS_SIGNAL_STRENGTH =
            15;

    /**
     * Faz o bloco emitir um sinal comum
     * quando ele estiver:
     *
     * - diretamente sob um jogador com
     *   a calça configurada como Cobre;
     *
     * ou
     *
     * - dentro do campo criado pelo
     *   conjunto completo de Redstone.
     */
    @Inject(
        method = "getSignal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tailoredTraits$emitPowerSignal(
        BlockGetter level,
        BlockPos position,
        Direction direction,
        CallbackInfoReturnable<Integer> info
    ) {
        if (
            !(level
                instanceof ServerLevel serverLevel)
        ) {
            return;
        }

        boolean isCopperSource =
            CopperRedstonePowerInitializer
                .isActiveSource(
                    serverLevel,
                    position
                );

        boolean isInsideRedstoneField =
            RedstoneFullSetPowerInitializer
                .isWithinActiveField(
                    serverLevel,
                    position
                );

        if (
            isCopperSource
                || isInsideRedstoneField
        ) {
            info.setReturnValue(
                TAILORED_TRAITS_SIGNAL_STRENGTH
            );
        }
    }

    /**
     * Também fornece sinal direto para
     * pistões, lâmpadas e componentes
     * que verificam energia forte.
     */
    @Inject(
        method = "getDirectSignal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void
        tailoredTraits$emitDirectPowerSignal(
            BlockGetter level,
            BlockPos position,
            Direction direction,
            CallbackInfoReturnable<Integer> info
        ) {
        if (
            !(level
                instanceof ServerLevel serverLevel)
        ) {
            return;
        }

        boolean isCopperSource =
            CopperRedstonePowerInitializer
                .isActiveSource(
                    serverLevel,
                    position
                );

        boolean isInsideRedstoneField =
            RedstoneFullSetPowerInitializer
                .isWithinActiveField(
                    serverLevel,
                    position
                );

        if (
            isCopperSource
                || isInsideRedstoneField
        ) {
            info.setReturnValue(
                TAILORED_TRAITS_SIGNAL_STRENGTH
            );
        }
    }
}