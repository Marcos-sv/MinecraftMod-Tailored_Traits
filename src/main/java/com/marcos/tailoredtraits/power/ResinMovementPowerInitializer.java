package com.marcos.tailoredtraits.power;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ResinMovementPowerInitializer
    implements ModInitializer {

    /*
     * Capacete configurado como Resina:
     *
     * 40 ticks = 2 segundos grudado no teto.
     */
    private static final int
        RESIN_HELMET_CEILING_STICK_DURATION =
            40;

    /*
     * Distância máxima entre a cabeça
     * e o teto para detectar o contato.
     */
    private static final double
        RESIN_HELMET_CEILING_CHECK_DISTANCE =
            0.10D;

    /*
     * Reduz o movimento horizontal enquanto
     * o jogador está grudado no teto.
     */
    private static final double
        RESIN_HELMET_HORIZONTAL_DAMPING =
            0.75D;

    /*
     * Pequeno espaço entre a cabeça
     * do jogador e o teto.
     */
    private static final double
        RESIN_HELMET_POSITION_EPSILON =
            0.002D;

    /*
     * Tempo restante do capacete de Resina
     * para cada jogador.
     */
    private static final Map<UUID, Integer>
        CEILING_STICK_TICKS_REMAINING =
            new HashMap<>();

    /*
     * Guarda os jogadores que já consumiram
     * os dois segundos da ativação atual.
     */
    private static final Set<UUID>
        CEILING_STICK_EXHAUSTED =
            new HashSet<>();

    /*
     * O construtor precisa ser público porque
     * esta classe é um entrypoint do Fabric.
     */
    public ResinMovementPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            server -> {

                for (
                    ServerPlayer player :
                    server.getPlayerList().getPlayers()
                ) {
                    updateResinHelmetPower(
                        player
                    );
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poder do capacete de Resina registrado."
        );
    }

    /**
     * Capacete configurado como Resina:
     *
     * ao encostar a cabeça no teto, o jogador
     * fica grudado por até dois segundos.
     */
    private static void updateResinHelmetPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasResinPower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.RESIN
            );

        /*
         * Situações nas quais o poder
         * não deve funcionar.
         */
        if (
            !hasResinPower
                || player.isSpectator()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.isPassenger()
                || player.isInWater()
                || player.isInLava()
                || player.onGround()
        ) {
            clearResinHelmetState(
                playerId
            );

            return;
        }

        CeilingContact ceilingContact =
            findCeilingContact(
                player
            );

        /*
         * O jogador não está mais
         * encostando no teto.
         *
         * Isso também recarrega o poder.
         */
        if (ceilingContact == null) {
            clearResinHelmetState(
                playerId
            );

            return;
        }

        /*
         * Os dois segundos desta ativação
         * já foram consumidos.
         */
        if (
            CEILING_STICK_EXHAUSTED.contains(
                playerId
            )
        ) {
            return;
        }

        int remainingTicks =
            CEILING_STICK_TICKS_REMAINING
                .getOrDefault(
                    playerId,
                    RESIN_HELMET_CEILING_STICK_DURATION
                );

        if (remainingTicks <= 0) {
            CEILING_STICK_TICKS_REMAINING.remove(
                playerId
            );

            CEILING_STICK_EXHAUSTED.add(
                playerId
            );

            return;
        }

        AABB boundingBox =
            player.getBoundingBox();

        /*
         * Calcula a posição necessária para
         * manter a cabeça junto ao teto.
         */
        double targetY =
            ceilingContact.ceilingBottomY()
                - boundingBox.getYsize()
                - RESIN_HELMET_POSITION_EPSILON;

        double verticalCorrection =
            targetY
                - player.getY();

        /*
         * Só reposiciona quando o jogador
         * realmente estiver próximo do teto.
         */
        if (
            Math.abs(
                verticalCorrection
            )
                <= RESIN_HELMET_CEILING_CHECK_DISTANCE
                    + 0.10D
        ) {
            player.setPos(
                player.getX(),
                targetY,
                player.getZ()
            );
        }

        Vec3 currentMovement =
            player.getDeltaMovement();

        /*
         * Remove a movimentação vertical
         * e reduz o movimento horizontal.
         */
        player.setDeltaMovement(
            currentMovement.x
                * RESIN_HELMET_HORIZONTAL_DAMPING,
            0.0D,
            currentMovement.z
                * RESIN_HELMET_HORIZONTAL_DAMPING
        );

        player.resetFallDistance();

        player.hurtMarked =
            true;

        remainingTicks--;

        /*
         * O tempo de dois segundos terminou.
         */
        if (remainingTicks <= 0) {
            CEILING_STICK_TICKS_REMAINING.remove(
                playerId
            );

            CEILING_STICK_EXHAUSTED.add(
                playerId
            );

            return;
        }

        CEILING_STICK_TICKS_REMAINING.put(
            playerId,
            remainingTicks
        );
    }

    /**
     * Procura um bloco com colisão
     * imediatamente acima da cabeça.
     */
    private static CeilingContact findCeilingContact(
        ServerPlayer player
    ) {
        AABB boundingBox =
            player.getBoundingBox();

        double centerX =
            (
                boundingBox.minX
                    + boundingBox.maxX
            ) / 2.0D;

        double centerZ =
            (
                boundingBox.minZ
                    + boundingBox.maxZ
            ) / 2.0D;

        double sampleInset =
            0.05D;

        /*
         * Verifica o centro e os quatro
         * cantos da cabeça.
         */
        double[][] samples = {
            {
                centerX,
                centerZ
            },
            {
                boundingBox.minX
                    + sampleInset,
                boundingBox.minZ
                    + sampleInset
            },
            {
                boundingBox.minX
                    + sampleInset,
                boundingBox.maxZ
                    - sampleInset
            },
            {
                boundingBox.maxX
                    - sampleInset,
                boundingBox.minZ
                    + sampleInset
            },
            {
                boundingBox.maxX
                    - sampleInset,
                boundingBox.maxZ
                    - sampleInset
            }
        };

        double highestAllowedY =
            boundingBox.maxY
                + RESIN_HELMET_CEILING_CHECK_DISTANCE;

        CeilingContact closestContact =
            null;

        for (
            double[] sample :
            samples
        ) {
            BlockPos blockPosition =
                BlockPos.containing(
                    sample[0],
                    highestAllowedY,
                    sample[1]
                );

            BlockState blockState =
                player.level().getBlockState(
                    blockPosition
                );

            if (blockState.isAir()) {
                continue;
            }

            VoxelShape collisionShape =
                blockState.getCollisionShape(
                    player.level(),
                    blockPosition
                );

            if (collisionShape.isEmpty()) {
                continue;
            }

            /*
             * Obtém a altura da parte inferior
             * da colisão do bloco.
             */
            double ceilingBottomY =
                blockPosition.getY()
                    + collisionShape.bounds().minY;

            double distanceFromHead =
                ceilingBottomY
                    - boundingBox.maxY;

            if (
                distanceFromHead < -0.05D
                    || distanceFromHead
                        > RESIN_HELMET_CEILING_CHECK_DISTANCE
            ) {
                continue;
            }

            if (
                closestContact == null
                    || ceilingBottomY
                        < closestContact.ceilingBottomY()
            ) {
                closestContact =
                    new CeilingContact(
                        ceilingBottomY
                    );
            }
        }

        return closestContact;
    }

    /**
     * Limpa e recarrega os estados
     * do capacete de Resina.
     */
    private static void clearResinHelmetState(
        UUID playerId
    ) {
        CEILING_STICK_TICKS_REMAINING.remove(
            playerId
        );

        CEILING_STICK_EXHAUSTED.remove(
            playerId
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

    /**
     * Guarda a altura da parte inferior
     * do bloco encontrado no teto.
     */
    private record CeilingContact(
        double ceilingBottomY
    ) {
    }
}