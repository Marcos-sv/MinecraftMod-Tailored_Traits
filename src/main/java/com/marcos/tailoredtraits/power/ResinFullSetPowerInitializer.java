package com.marcos.tailoredtraits.power;

import com.marcos.tailoredtraits.TailoredTraits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ResinFullSetPowerInitializer
    implements ModInitializer {

    /*
     * Velocidade vertical da escalada.
     *
     * Um salto comum começa próximo de 0,42.
     * O valor 0,22 cria uma subida contínua,
     * mas controlada.
     */
    private static final double
        WALL_CLIMB_SPEED =
            0.22D;

    /*
     * Reduz levemente o movimento horizontal
     * durante a escalada.
     *
     * Isso ajuda o jogador a continuar
     * encostado na parede.
     */
    private static final double
        HORIZONTAL_MOVEMENT_MULTIPLIER =
            0.85D;

    /*
     * Distância adicional utilizada para
     * procurar blocos nas laterais do jogador.
     */
    private static final double
        WALL_DETECTION_DISTANCE =
            0.10D;

    /*
     * Pequena parte da área de detecção
     * entra dentro da caixa do jogador.
     *
     * Isso evita falhas quando o jogador está
     * exatamente encostado na borda do bloco.
     */
    private static final double
        PROBE_OVERLAP =
            0.025D;

    /*
     * Remove as partes muito próximas do chão
     * e do teto durante a verificação.
     *
     * Assim, o bloco abaixo do jogador não é
     * confundido com uma parede.
     */
    private static final double
        VERTICAL_INSET =
            0.10D;

    /*
     * Reduz as extremidades laterais das áreas
     * de detecção, evitando que um bloco em um
     * canto seja detectado de maneira incorreta.
     */
    private static final double
        SIDE_INSET =
            0.05D;

    /*
     * O construtor precisa ser público porque
     * esta classe é criada pelo Fabric.
     */
    public ResinFullSetPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            ResinFullSetPowerInitializer
                ::updateResinWallClimbing
        );

        TailoredTraits.LOGGER.info(
            "Poder de conjunto da Resina registrado."
        );
    }

    /**
     * Atualiza a escalada de todos os jogadores
     * conectados ao servidor.
     */
    private static void updateResinWallClimbing(
        MinecraftServer server
    ) {
        for (
            ServerPlayer player :
            server.getPlayerList().getPlayers()
        ) {
            updatePlayerWallClimbing(
                player
            );
        }
    }

    /**
     * Verifica e aplica a escalada para
     * um jogador específico.
     */
    private static void updatePlayerWallClimbing(
        ServerPlayer player
    ) {
        /*
         * O poder somente funciona quando
         * as quatro peças estão equipadas,
         * possuem acabamento de Stevium e
         * estão configuradas como Resina.
         */
        boolean hasResinFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    player,
                    PowerMaterial.RESIN
                );

        if (!hasResinFullSet) {
            return;
        }

        /*
         * Situações em que a escalada
         * deve permanecer desativada.
         */
        if (
            player.isSpectator()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.isPassenger()
                || player.isInWater()
                || player.isInLava()
        ) {
            return;
        }

        Input input =
            player.getLastClientInput();

        /*
         * O jogador precisa manter a tecla
         * de salto pressionada.
         *
         * Normalmente ele também estará
         * pressionando W para permanecer
         * contra a parede.
         */
        if (!input.jump()) {
            return;
        }

        /*
         * Verifica manualmente as quatro
         * laterais da caixa do jogador.
         *
         * Não depende mais de:
         *
         * horizontalCollision
         * minorHorizontalCollision
         */
        if (
            !isTouchingSolidWall(
                player
            )
        ) {
            return;
        }

        Vec3 currentMovement =
            player.getDeltaMovement();

        /*
         * Caso o jogador tenha acabado de pular,
         * mantém a velocidade normal do salto.
         *
         * Quando a velocidade do salto começa
         * a diminuir, a força da escalada assume.
         */
        double verticalMovement =
            Math.max(
                currentMovement.y,
                WALL_CLIMB_SPEED
            );

        player.setDeltaMovement(
            currentMovement.x
                * HORIZONTAL_MOVEMENT_MULTIPLIER,
            verticalMovement,
            currentMovement.z
                * HORIZONTAL_MOVEMENT_MULTIPLIER
        );

        /*
         * Força a sincronização da nova
         * velocidade com o cliente.
         */
        player.hurtMarked =
            true;

        /*
         * Escalar uma parede remove toda
         * distância de queda acumulada.
         */
        player.fallDistance =
            0.0F;
    }

    /**
     * Cria áreas finas nas quatro laterais
     * do jogador e verifica se alguma delas
     * toca a colisão de um bloco.
     */
    private static boolean isTouchingSolidWall(
        ServerPlayer player
    ) {
        ServerLevel level =
            player.level();

        AABB playerBox =
            player.getBoundingBox();

        /*
         * Área ao norte do jogador.
         */
        AABB northProbe =
            new AABB(
                playerBox.minX
                    + SIDE_INSET,
                playerBox.minY
                    + VERTICAL_INSET,
                playerBox.minZ
                    - WALL_DETECTION_DISTANCE,

                playerBox.maxX
                    - SIDE_INSET,
                playerBox.maxY
                    - VERTICAL_INSET,
                playerBox.minZ
                    + PROBE_OVERLAP
            );

        /*
         * Área ao sul do jogador.
         */
        AABB southProbe =
            new AABB(
                playerBox.minX
                    + SIDE_INSET,
                playerBox.minY
                    + VERTICAL_INSET,
                playerBox.maxZ
                    - PROBE_OVERLAP,

                playerBox.maxX
                    - SIDE_INSET,
                playerBox.maxY
                    - VERTICAL_INSET,
                playerBox.maxZ
                    + WALL_DETECTION_DISTANCE
            );

        /*
         * Área ao oeste do jogador.
         */
        AABB westProbe =
            new AABB(
                playerBox.minX
                    - WALL_DETECTION_DISTANCE,
                playerBox.minY
                    + VERTICAL_INSET,
                playerBox.minZ
                    + SIDE_INSET,

                playerBox.minX
                    + PROBE_OVERLAP,
                playerBox.maxY
                    - VERTICAL_INSET,
                playerBox.maxZ
                    - SIDE_INSET
            );

        /*
         * Área ao leste do jogador.
         */
        AABB eastProbe =
            new AABB(
                playerBox.maxX
                    - PROBE_OVERLAP,
                playerBox.minY
                    + VERTICAL_INSET,
                playerBox.minZ
                    + SIDE_INSET,

                playerBox.maxX
                    + WALL_DETECTION_DISTANCE,
                playerBox.maxY
                    - VERTICAL_INSET,
                playerBox.maxZ
                    - SIDE_INSET
            );

        return intersectsBlockCollision(
            level,
            northProbe
        ) || intersectsBlockCollision(
            level,
            southProbe
        ) || intersectsBlockCollision(
            level,
            westProbe
        ) || intersectsBlockCollision(
            level,
            eastProbe
        );
    }

    /**
     * Verifica se uma área toca a forma
     * de colisão de algum bloco.
     */
    private static boolean intersectsBlockCollision(
        ServerLevel level,
        AABB detectionArea
    ) {
        int minimumX =
            Mth.floor(
                detectionArea.minX
            );

        int maximumX =
            Mth.floor(
                detectionArea.maxX
            );

        int minimumY =
            Mth.floor(
                detectionArea.minY
            );

        int maximumY =
            Mth.floor(
                detectionArea.maxY
            );

        int minimumZ =
            Mth.floor(
                detectionArea.minZ
            );

        int maximumZ =
            Mth.floor(
                detectionArea.maxZ
            );

        /*
         * Percorre apenas os poucos blocos
         * próximos da área de detecção.
         */
        for (
            int x = minimumX;
            x <= maximumX;
            x++
        ) {
            for (
                int y = minimumY;
                y <= maximumY;
                y++
            ) {
                for (
                    int z = minimumZ;
                    z <= maximumZ;
                    z++
                ) {
                    BlockPos blockPosition =
                        new BlockPos(
                            x,
                            y,
                            z
                        );

                    BlockState blockState =
                        level.getBlockState(
                            blockPosition
                        );

                    VoxelShape collisionShape =
                        blockState.getCollisionShape(
                            level,
                            blockPosition
                        );

                    /*
                     * Ar, plantas e outros blocos
                     * sem colisão são ignorados.
                     */
                    if (
                        collisionShape.isEmpty()
                    ) {
                        continue;
                    }

                    /*
                     * Uma forma de colisão pode
                     * possuir várias caixas.
                     *
                     * Escadas, cercas e muros são
                     * exemplos de colisões que não
                     * ocupam um cubo completo.
                     */
                    for (
                        AABB localCollisionBox :
                        collisionShape.toAabbs()
                    ) {
                        AABB worldCollisionBox =
                            localCollisionBox.move(
                                blockPosition.getX(),
                                blockPosition.getY(),
                                blockPosition.getZ()
                            );

                        if (
                            worldCollisionBox.intersects(
                                detectionArea
                            )
                        ) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}