package com.marcos.tailoredtraits.power;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

public final class RedstoneFullSetPowerInitializer
    implements ModInitializer {

    /*
     * Raio em que os blocos ao redor
     * do jogador emitem energia.
     */
    private static final int
        REDSTONE_POWER_RADIUS =
            5;

    /*
     * Um bloco adicional é atualizado
     * para garantir que mecanismos nas
     * bordas do campo sejam recalculados.
     */
    private static final int
        NOTIFICATION_RADIUS =
            REDSTONE_POWER_RADIUS + 1;

    /*
     * Guarda o centro do campo de energia
     * de cada jogador que está utilizando
     * o conjunto completo de Redstone.
     */
    private static final Map<
        UUID,
        ActiveRedstoneField
    > ACTIVE_FIELDS =
        new HashMap<>();

    /*
     * O construtor precisa ser público
     * porque esta classe é um entrypoint.
     */
    public RedstoneFullSetPowerInitializer() {
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(
            RedstoneFullSetPowerInitializer
                ::updateRedstoneFields
        );

        TailoredTraits.LOGGER.info(
            "Poder de conjunto da Redstone registrado."
        );
    }

    /**
     * Atualiza os campos de energia
     * ativos no servidor.
     */
    private static void updateRedstoneFields(
        MinecraftServer server
    ) {
        Map<UUID, ActiveRedstoneField>
            previousFields =
                new HashMap<>(
                    ACTIVE_FIELDS
                );

        Map<UUID, ActiveRedstoneField>
            nextFields =
                new HashMap<>();

        for (
            ServerPlayer player :
            server.getPlayerList().getPlayers()
        ) {
            if (
                !canActivateRedstoneField(
                    player
                )
            ) {
                continue;
            }

            ActiveRedstoneField field =
                new ActiveRedstoneField(
                    player.level(),
                    player.blockPosition()
                        .immutable()
                );

            nextFields.put(
                player.getUUID(),
                field
            );
        }

        /*
         * A lista é alterada antes das
         * atualizações dos blocos.
         *
         * Dessa forma, os componentes já
         * consultam o campo correto ao serem
         * atualizados.
         */
        ACTIVE_FIELDS.clear();
        ACTIVE_FIELDS.putAll(
            nextFields
        );

        notifyChangedFields(
            previousFields,
            nextFields
        );
    }

    /**
     * Verifica se o jogador pode manter
     * o campo de Redstone ativo.
     */
    private static boolean canActivateRedstoneField(
        ServerPlayer player
    ) {
        if (
            !player.isAlive()
                || player.isSpectator()
        ) {
            return false;
        }

        return FullSetPowerUtil
            .hasActiveFullSetMaterial(
                player,
                PowerMaterial.REDSTONE
            );
    }

    /**
     * Atualiza os blocos quando:
     *
     * - o jogador ativa o conjunto;
     * - o jogador remove uma peça;
     * - o jogador muda o material;
     * - o jogador muda de bloco;
     * - o jogador muda de dimensão.
     */
    private static void notifyChangedFields(
        Map<UUID, ActiveRedstoneField>
            previousFields,
        Map<UUID, ActiveRedstoneField>
            nextFields
    ) {
        Set<UUID> affectedPlayers =
            new HashSet<>();

        affectedPlayers.addAll(
            previousFields.keySet()
        );

        affectedPlayers.addAll(
            nextFields.keySet()
        );

        for (
            UUID playerId :
            affectedPlayers
        ) {
            ActiveRedstoneField previous =
                previousFields.get(
                    playerId
                );

            ActiveRedstoneField next =
                nextFields.get(
                    playerId
                );

            /*
             * O campo permanece na mesma
             * posição e dimensão.
             */
            if (
                Objects.equals(
                    previous,
                    next
                )
            ) {
                continue;
            }

            /*
             * Desliga e atualiza a área antiga.
             */
            if (previous != null) {
                notifyFieldArea(
                    previous
                );
            }

            /*
             * Liga e atualiza a nova área.
             */
            if (next != null) {
                notifyFieldArea(
                    next
                );
            }
        }
    }

    /**
     * Avisa os blocos dentro e ao redor
     * de um campo para recalcularem
     * suas conexões de Redstone.
     */
    private static void notifyFieldArea(
        ActiveRedstoneField field
    ) {
        ServerLevel level =
            field.level();

        BlockPos center =
            field.center();

        int maximumDistanceSquared =
            NOTIFICATION_RADIUS
                * NOTIFICATION_RADIUS;

        for (
            int offsetX =
                -NOTIFICATION_RADIUS;
            offsetX <= NOTIFICATION_RADIUS;
            offsetX++
        ) {
            for (
                int offsetY =
                    -NOTIFICATION_RADIUS;
                offsetY <= NOTIFICATION_RADIUS;
                offsetY++
            ) {
                for (
                    int offsetZ =
                        -NOTIFICATION_RADIUS;
                    offsetZ <= NOTIFICATION_RADIUS;
                    offsetZ++
                ) {
                    int distanceSquared =
                        offsetX * offsetX
                            + offsetY * offsetY
                            + offsetZ * offsetZ;

                    if (
                        distanceSquared
                            > maximumDistanceSquared
                    ) {
                        continue;
                    }

                    BlockPos position =
                        center.offset(
                            offsetX,
                            offsetY,
                            offsetZ
                        );

                    Block sourceBlock =
                        level
                            .getBlockState(
                                position
                            )
                            .getBlock();

                    level.updateNeighborsAt(
                        position,
                        sourceBlock
                    );
                }
            }
        }
    }

    /**
     * Chamado pelo mixin de sinal.
     *
     * Retorna true quando o bloco consultado
     * está dentro do campo de algum jogador
     * com conjunto completo de Redstone.
     */
    public static boolean isWithinActiveField(
        ServerLevel level,
        BlockPos position
    ) {
        int radiusSquared =
            REDSTONE_POWER_RADIUS
                * REDSTONE_POWER_RADIUS;

        for (
            ActiveRedstoneField field :
            ACTIVE_FIELDS.values()
        ) {
            if (
                field.level() != level
            ) {
                continue;
            }

            BlockPos center =
                field.center();

            long differenceX =
                position.getX()
                    - center.getX();

            long differenceY =
                position.getY()
                    - center.getY();

            long differenceZ =
                position.getZ()
                    - center.getZ();

            long distanceSquared =
                differenceX * differenceX
                    + differenceY * differenceY
                    + differenceZ * differenceZ;

            if (
                distanceSquared
                    <= radiusSquared
            ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Representa um campo ativo
     * em uma dimensão específica.
     */
    private record ActiveRedstoneField(
        ServerLevel level,
        BlockPos center
    ) {
    }
}