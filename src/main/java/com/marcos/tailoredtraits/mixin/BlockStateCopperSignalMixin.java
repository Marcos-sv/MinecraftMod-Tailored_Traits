package com.marcos.tailoredtraits.mixin;

import com.marcos.tailoredtraits.power
    .CopperRedstonePowerInitializer;

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
     * Força máxima de redstone.
     */
    @Unique
    private static final int
        COPPER_SIGNAL_STRENGTH =
            15;

    /**
     * Faz o bloco de apoio emitir
     * um sinal comum de redstone.
     */
    @Inject(
        method = "getSignal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tailoredTraits$emitCopperSignal(
        BlockGetter level,
        BlockPos position,
        Direction direction,
        CallbackInfoReturnable<Integer> info
    ) {
        if (
            level
                instanceof ServerLevel serverLevel
                && CopperRedstonePowerInitializer
                    .isActiveSource(
                        serverLevel,
                        position
                    )
        ) {
            info.setReturnValue(
                COPPER_SIGNAL_STRENGTH
            );
        }
    }

    /**
     * Também fornece sinal direto para
     * pistões e outros componentes que
     * utilizam energia forte.
     */
    @Inject(
        method = "getDirectSignal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void
        tailoredTraits$emitDirectCopperSignal(
            BlockGetter level,
            BlockPos position,
            Direction direction,
            CallbackInfoReturnable<Integer> info
        ) {
        if (
            level
                instanceof ServerLevel serverLevel
                && CopperRedstonePowerInitializer
                    .isActiveSource(
                        serverLevel,
                        position
                    )
        ) {
            info.setReturnValue(
                COPPER_SIGNAL_STRENGTH
            );
        }
    }
}