package com.marcos.tailoredtraits.power;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class CopperRedstonePowerInitializer
    implements ModInitializer {

    /*
     * Guarda os blocos que atualmente estão
     * emitindo energia por causa de um jogador
     * com a calça configurada como Cobre.
     *
     * O long representa uma BlockPos compactada.
     */
    private static final Map<
        ServerLevel,
        Set<Long>
    >
        ACTIVE_COPPER_SOURCES =
            new IdentityHashMap<>();

    /*
     * O construtor precisa ser público porque
     * esta classe é um entrypoint do Fabric.
     */
    public CopperRedstonePowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            CopperRedstonePowerInitializer
                ::updateActiveCopperSources
        );

        TailoredTraits.LOGGER.info(
            "Poder da calça de Cobre registrado."
        );
    }

    /**
     * Atualiza os blocos que devem funcionar
     * como fontes de redstone.
     */
    private static void updateActiveCopperSources(
        MinecraftServer server
    ) {
        Map<ServerLevel, Set<Long>>
            previousSources =
                copyActiveSources();

        Map<ServerLevel, Set<Long>>
            nextSources =
                new IdentityHashMap<>();

        for (
            ServerPlayer player :
            server.getPlayerList().getPlayers()
        ) {
            if (
                !canActivateCopperSignal(
                    player
                )
            ) {
                continue;
            }

            /*
             * Localiza o bloco diretamente
             * abaixo dos pés do jogador.
             *
             * O pequeno valor subtraído garante
             * que seja selecionado o bloco de apoio,
             * e não o espaço ocupado pelo jogador.
             */
            BlockPos supportPosition =
                BlockPos.containing(
                    player.getX(),
                    player.getBoundingBox().minY
                        - 0.01D,
                    player.getZ()
                );

            nextSources
                .computeIfAbsent(
                    player.level(),
                    ignored ->
                        new HashSet<>()
                )
                .add(
                    supportPosition.asLong()
                );
        }

        /*
         * Atualiza primeiro a lista usada
         * pelo mixin de sinal.
         */
        ACTIVE_COPPER_SOURCES.clear();

        for (
            Map.Entry<ServerLevel, Set<Long>> entry :
            nextSources.entrySet()
        ) {
            ACTIVE_COPPER_SOURCES.put(
                entry.getKey(),
                new HashSet<>(
                    entry.getValue()
                )
            );
        }

        /*
         * Depois avisa os componentes de redstone
         * que alguma fonte foi ligada ou desligada.
         */
        notifyChangedSources(
            previousSources,
            nextSources
        );
    }

    /**
     * Cria uma cópia do estado do tick anterior.
     */
    private static Map<ServerLevel, Set<Long>>
        copyActiveSources() {
        Map<ServerLevel, Set<Long>> copy =
            new IdentityHashMap<>();

        for (
            Map.Entry<ServerLevel, Set<Long>> entry :
            ACTIVE_COPPER_SOURCES.entrySet()
        ) {
            copy.put(
                entry.getKey(),
                new HashSet<>(
                    entry.getValue()
                )
            );
        }

        return copy;
    }

    /**
     * Atualiza os vizinhos somente quando
     * uma fonte foi ligada ou desligada.
     */
    private static void notifyChangedSources(
        Map<ServerLevel, Set<Long>>
            previousSources,
        Map<ServerLevel, Set<Long>>
            nextSources
    ) {
        Set<ServerLevel> affectedLevels =
            java.util.Collections
                .newSetFromMap(
                    new IdentityHashMap<>()
                );

        affectedLevels.addAll(
            previousSources.keySet()
        );

        affectedLevels.addAll(
            nextSources.keySet()
        );

        for (
            ServerLevel level :
            affectedLevels
        ) {
            Set<Long> previous =
                previousSources.getOrDefault(
                    level,
                    Set.of()
                );

            Set<Long> next =
                nextSources.getOrDefault(
                    level,
                    Set.of()
                );

            Set<Long> changedPositions =
                new HashSet<>(
                    previous
                );

            changedPositions.addAll(
                next
            );

            for (
                long packedPosition :
                changedPositions
            ) {
                boolean wasActive =
                    previous.contains(
                        packedPosition
                    );

                boolean isActive =
                    next.contains(
                        packedPosition
                    );

                /*
                 * O estado não mudou.
                 */
                if (wasActive == isActive) {
                    continue;
                }

                BlockPos position =
                    BlockPos.of(
                        packedPosition
                    );

                Block sourceBlock =
                    level
                        .getBlockState(
                            position
                        )
                        .getBlock();

                /*
                 * Faz os componentes próximos
                 * consultarem novamente o sinal.
                 */
                level.updateNeighborsAt(
                    position,
                    sourceBlock
                );
            }
        }
    }

    /**
     * Define quando a calça pode ativar
     * a energia de redstone.
     */
    private static boolean
        canActivateCopperSignal(
            ServerPlayer player
        ) {
        if (
            player.isSpectator()
                || player.getAbilities().flying
                || !player.onGround()
        ) {
            return false;
        }

        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        return hasSelectedMaterial(
            leggings,
            PowerMaterial.COPPER
        );
    }

    /**
     * Usado pelo mixin para verificar se
     * um determinado bloco deve emitir sinal.
     */
    public static boolean isActiveSource(
        ServerLevel level,
        BlockPos position
    ) {
        Set<Long> sources =
            ACTIVE_COPPER_SOURCES.get(
                level
            );

        return sources != null
            && sources.contains(
                position.asLong()
            );
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