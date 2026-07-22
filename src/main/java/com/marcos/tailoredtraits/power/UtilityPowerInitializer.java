package com.marcos.tailoredtraits.power;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class UtilityPowerInitializer
    implements ModInitializer {

    /*
     * Capacete configurado como Quartzo:
     *
     * distância máxima na qual a vida
     * de uma criatura pode ser visualizada.
     */
    private static final double
        QUARTZ_HEALTH_SCAN_RANGE =
            24.0D;

    /*
     * Define o alinhamento necessário entre
     * a visão do jogador e a criatura.
     */
    private static final double
        QUARTZ_MINIMUM_VIEW_ALIGNMENT =
            0.985D;

    /*
     * Atualiza o indicador de vida
     * quatro vezes por segundo.
     *
     * 5 ticks = 0,25 segundo.
     */
    private static final int
        QUARTZ_HEALTH_UPDATE_INTERVAL =
            5;

    /*
     * Guarda os jogadores que estão vendo
     * o indicador de vida do Quartzo.
     */
    private static final Set<UUID>
        QUARTZ_HEALTH_DISPLAY_ACTIVE =
            new HashSet<>();

    /*
     * Calça configurada como Resina:
     *
     * velocidade vertical enquanto o jogador
     * estiver aderido à parede.
     *
     * Um valor levemente negativo faz o jogador
     * deslizar lentamente para baixo.
     */
    private static final double
        RESIN_WALL_SLIDE_SPEED =
            -0.03D;

    /*
     * Reduz a velocidade horizontal do jogador
     * quando ele encosta na parede.
     *
     * Isso impede que o impulso anterior faça
     * o jogador se soltar imediatamente.
     */
    private static final double
        RESIN_HORIZONTAL_DAMPING =
            0.20D;

    /*
     * Pequena força aplicada em direção à parede.
     *
     * Ela mantém o jogador aderido, mas ainda
     * permite que ele se afaste pressionando
     * uma tecla na direção contrária.
     */
    private static final double
        RESIN_WALL_PULL_STRENGTH =
            0.015D;

    /*
     * Distância adicional utilizada para verificar
     * blocos muito próximos da caixa do jogador.
     */
    private static final double
        RESIN_WALL_CHECK_DISTANCE =
            0.08D;

    /*
     * Evita verificar apenas as extremidades
     * exatas da caixa do jogador.
     */
    private static final double
        RESIN_SAMPLE_INSET =
            0.05D;

    /*
     * O Fabric cria esta classe por meio do
     * entrypoint registrado no fabric.mod.json.
     */
    public UtilityPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            server -> {

                for (
                    ServerPlayer player :
                    server.getPlayerList().getPlayers()
                ) {
                    updateQuartzHelmetPower(
                        player
                    );

                    updateResinLeggingsPower(
                        player
                    );
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poderes utilitários do Tailored Traits registrados."
        );
    }

    /**
     * Capacete configurado como Quartzo:
     *
     * mostra o nome e a vida da criatura
     * para a qual o jogador estiver olhando.
     */
    private static void updateQuartzHelmetPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasQuartzPower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.QUARTZ
            );

        if (
            !hasQuartzPower
                || player.isSpectator()
        ) {
            clearQuartzHealthDisplay(
                player
            );

            return;
        }

        if (
            player.tickCount
                % QUARTZ_HEALTH_UPDATE_INTERVAL
                != 0
        ) {
            return;
        }

        LivingEntity target =
            findEntityPlayerIsLookingAt(
                player
            );

        if (target == null) {
            clearQuartzHealthDisplay(
                player
            );

            return;
        }

        String currentHealth =
            formatHealthValue(
                target.getHealth()
            );

        String maximumHealth =
            formatHealthValue(
                target.getMaxHealth()
            );

        Component healthMessage =
            Component.literal("")
                .append(
                    target.getDisplayName()
                )
                .append(
                    Component.literal(
                        ": "
                            + currentHealth
                            + " / "
                            + maximumHealth
                            + " HP"
                    )
                );

        player.sendOverlayMessage(
            healthMessage
        );

        QUARTZ_HEALTH_DISPLAY_ACTIVE.add(
            playerId
        );
    }

    /**
     * Procura uma entidade viva próxima
     * do centro da visão do jogador.
     */
    private static LivingEntity
        findEntityPlayerIsLookingAt(
            ServerPlayer player
        ) {
        Vec3 eyePosition =
            player.getEyePosition();

        Vec3 viewDirection =
            player.getViewVector(
                1.0F
            ).normalize();

        LivingEntity bestTarget =
            null;

        double bestTargetScore =
            Double.MAX_VALUE;

        for (
            LivingEntity entity :
            player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(
                    QUARTZ_HEALTH_SCAN_RANGE
                ),
                entity ->
                    entity.isAlive()
                        && entity != player
            )
        ) {
            Vec3 targetPosition =
                entity.position().add(
                    0.0D,
                    entity.getBbHeight()
                        * 0.5D,
                    0.0D
                );

            Vec3 directionToTarget =
                targetPosition.subtract(
                    eyePosition
                );

            double distanceSquared =
                directionToTarget.lengthSqr();

            if (distanceSquared <= 0.0001D) {
                continue;
            }

            double distance =
                Math.sqrt(
                    distanceSquared
                );

            if (
                distance
                    > QUARTZ_HEALTH_SCAN_RANGE
            ) {
                continue;
            }

            Vec3 normalizedDirection =
                directionToTarget.normalize();

            double viewAlignment =
                viewDirection.dot(
                    normalizedDirection
                );

            if (
                viewAlignment
                    < QUARTZ_MINIMUM_VIEW_ALIGNMENT
            ) {
                continue;
            }

            if (
                !player.hasLineOfSight(
                    entity
                )
            ) {
                continue;
            }

            double alignmentPenalty =
                1.0D
                    - viewAlignment;

            double targetScore =
                alignmentPenalty
                    * 1000.0D
                    + distance;

            if (
                targetScore
                    >= bestTargetScore
            ) {
                continue;
            }

            bestTarget =
                entity;

            bestTargetScore =
                targetScore;
        }

        return bestTarget;
    }

    /**
     * Formata a vida para mostrar no máximo
     * uma casa decimal.
     */
    private static String formatHealthValue(
        float health
    ) {
        double roundedHealth =
            Math.round(
                health * 10.0D
            ) / 10.0D;

        if (
            roundedHealth
                == Math.rint(
                    roundedHealth
                )
        ) {
            return Integer.toString(
                (int) roundedHealth
            );
        }

        return Double.toString(
            roundedHealth
        );
    }

    /**
     * Limpa a mensagem de vida criada
     * pelo capacete de Quartzo.
     */
    private static void clearQuartzHealthDisplay(
        ServerPlayer player
    ) {
        boolean wasDisplayingHealth =
            QUARTZ_HEALTH_DISPLAY_ACTIVE.remove(
                player.getUUID()
            );

        if (!wasDisplayingHealth) {
            return;
        }

        player.sendOverlayMessage(
            Component.literal("")
        );
    }

    /**
     * Calça configurada como Resina:
     *
     * basta encostar em uma parede enquanto estiver
     * no ar para aderir e deslizar lentamente.
     */
    private static void updateResinLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasResinPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.RESIN
            );

        if (
            !hasResinPower
                || player.isSpectator()
        ) {
            return;
        }

        /*
         * Não interfere no voo criativo.
         */
        if (
            player.getAbilities().flying
        ) {
            return;
        }

        /*
         * Só atua quando o jogador estiver no ar.
         */
        if (player.onGround()) {
            return;
        }

        /*
         * Não interfere durante:
         *
         * - voo com elytra;
         * - uso de veículos;
         * - água;
         * - lava.
         */
        if (
            player.isFallFlying()
                || player.isPassenger()
                || player.isInWater()
                || player.isInLava()
        ) {
            return;
        }

        /*
         * Procura diretamente blocos sólidos
         * encostados nos quatro lados do jogador.
         *
         * Não depende mais de horizontalCollision
         * nem de uma tecla pressionada.
         */
        WallContact wallContact =
            findWallContact(
                player
            );

        if (!wallContact.touching()) {
            return;
        }

        Vec3 currentMovement =
            player.getDeltaMovement();

        /*
         * Reduz o impulso horizontal anterior.
         */
        double newMovementX =
            currentMovement.x
                * RESIN_HORIZONTAL_DAMPING;

        double newMovementZ =
            currentMovement.z
                * RESIN_HORIZONTAL_DAMPING;

        /*
         * Aplica uma força muito pequena em direção
         * à parede para manter o jogador aderido.
         */
        newMovementX +=
            wallContact.pullX()
                * RESIN_WALL_PULL_STRENGTH;

        newMovementZ +=
            wallContact.pullZ()
                * RESIN_WALL_PULL_STRENGTH;

        /*
         * Substitui a queda normal por uma
         * descida lenta e constante.
         */
        player.setDeltaMovement(
            newMovementX,
            RESIN_WALL_SLIDE_SPEED,
            newMovementZ
        );

        /*
         * Impede que o período aderido à parede
         * seja contado como uma queda longa.
         */
        player.resetFallDistance();

        /*
         * Sincroniza o movimento com o cliente.
         */
        player.hurtMarked =
            true;
    }

    /**
     * Verifica blocos encostados nos quatro lados
     * da caixa de colisão do jogador.
     */
    private static WallContact findWallContact(
        ServerPlayer player
    ) {
        AABB boundingBox =
            player.getBoundingBox();

        /*
         * Ignora uma pequena parte dos pés
         * para não confundir o chão com parede.
         */
        int minimumY =
            (int) Math.floor(
                boundingBox.minY
                    + 0.20D
            );

        int maximumY =
            (int) Math.floor(
                boundingBox.maxY
                    - 0.20D
            );

        if (maximumY < minimumY) {
            maximumY =
                minimumY;
        }

        double minimumXSample =
            boundingBox.minX
                + RESIN_SAMPLE_INSET;

        double centerXSample =
            (
                boundingBox.minX
                    + boundingBox.maxX
            ) / 2.0D;

        double maximumXSample =
            boundingBox.maxX
                - RESIN_SAMPLE_INSET;

        double minimumZSample =
            boundingBox.minZ
                + RESIN_SAMPLE_INSET;

        double centerZSample =
            (
                boundingBox.minZ
                    + boundingBox.maxZ
            ) / 2.0D;

        double maximumZSample =
            boundingBox.maxZ
                - RESIN_SAMPLE_INSET;

        /*
         * Parede no lado oeste:
         * eixo X negativo.
         */
        boolean touchingWest =
            touchesWallOnXSide(
                player,
                boundingBox.minX
                    - RESIN_WALL_CHECK_DISTANCE,
                minimumZSample,
                centerZSample,
                maximumZSample,
                minimumY,
                maximumY
            );

        /*
         * Parede no lado leste:
         * eixo X positivo.
         */
        boolean touchingEast =
            touchesWallOnXSide(
                player,
                boundingBox.maxX
                    + RESIN_WALL_CHECK_DISTANCE,
                minimumZSample,
                centerZSample,
                maximumZSample,
                minimumY,
                maximumY
            );

        /*
         * Parede no lado norte:
         * eixo Z negativo.
         */
        boolean touchingNorth =
            touchesWallOnZSide(
                player,
                boundingBox.minZ
                    - RESIN_WALL_CHECK_DISTANCE,
                minimumXSample,
                centerXSample,
                maximumXSample,
                minimumY,
                maximumY
            );

        /*
         * Parede no lado sul:
         * eixo Z positivo.
         */
        boolean touchingSouth =
            touchesWallOnZSide(
                player,
                boundingBox.maxZ
                    + RESIN_WALL_CHECK_DISTANCE,
                minimumXSample,
                centerXSample,
                maximumXSample,
                minimumY,
                maximumY
            );

        boolean touchingAnyWall =
            touchingWest
                || touchingEast
                || touchingNorth
                || touchingSouth;

        if (!touchingAnyWall) {
            return WallContact.NONE;
        }

        double pullX =
            0.0D;

        double pullZ =
            0.0D;

        if (touchingWest) {
            pullX -=
                1.0D;
        }

        if (touchingEast) {
            pullX +=
                1.0D;
        }

        if (touchingNorth) {
            pullZ -=
                1.0D;
        }

        if (touchingSouth) {
            pullZ +=
                1.0D;
        }

        /*
         * Em cantos, normaliza a força para não
         * produzir um impulso diagonal mais forte.
         */
        double pullLength =
            Math.sqrt(
                pullX * pullX
                    + pullZ * pullZ
            );

        if (pullLength > 0.0D) {
            pullX /=
                pullLength;

            pullZ /=
                pullLength;
        }

        return new WallContact(
            true,
            pullX,
            pullZ
        );
    }

    /**
     * Verifica uma parede nos lados leste
     * ou oeste do jogador.
     */
    private static boolean touchesWallOnXSide(
        ServerPlayer player,
        double xPosition,
        double firstZPosition,
        double centerZPosition,
        double lastZPosition,
        int minimumY,
        int maximumY
    ) {
        double[] zSamples = {
            firstZPosition,
            centerZPosition,
            lastZPosition
        };

        for (
            int y = minimumY;
            y <= maximumY;
            y++
        ) {
            for (
                double zPosition :
                zSamples
            ) {
                BlockPos blockPosition =
                    new BlockPos(
                        (int) Math.floor(
                            xPosition
                        ),
                        y,
                        (int) Math.floor(
                            zPosition
                        )
                    );

                if (
                    hasCollisionAt(
                        player,
                        blockPosition
                    )
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Verifica uma parede nos lados norte
     * ou sul do jogador.
     */
    private static boolean touchesWallOnZSide(
        ServerPlayer player,
        double zPosition,
        double firstXPosition,
        double centerXPosition,
        double lastXPosition,
        int minimumY,
        int maximumY
    ) {
        double[] xSamples = {
            firstXPosition,
            centerXPosition,
            lastXPosition
        };

        for (
            int y = minimumY;
            y <= maximumY;
            y++
        ) {
            for (
                double xPosition :
                xSamples
            ) {
                BlockPos blockPosition =
                    new BlockPos(
                        (int) Math.floor(
                            xPosition
                        ),
                        y,
                        (int) Math.floor(
                            zPosition
                        )
                    );

                if (
                    hasCollisionAt(
                        player,
                        blockPosition
                    )
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Verifica se existe uma forma de colisão
     * no bloco consultado.
     *
     * Isso evita considerar ar, flores, tochas
     * e outros blocos sem colisão como paredes.
     */
    private static boolean hasCollisionAt(
        ServerPlayer player,
        BlockPos blockPosition
    ) {
        BlockState blockState =
            player.level().getBlockState(
                blockPosition
            );

        if (blockState.isAir()) {
            return false;
        }

        return !blockState
            .getCollisionShape(
                player.level(),
                blockPosition
            )
            .isEmpty();
    }

    /**
     * Verifica se a peça possui acabamento
     * de Stevium e está configurada com
     * o material esperado.
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
     * Resultado da procura por uma parede.
     *
     * pullX e pullZ indicam a direção da pequena
     * força que mantém o jogador aderido.
     */
    private record WallContact(
        boolean touching,
        double pullX,
        double pullZ
    ) {

        private static final WallContact NONE =
            new WallContact(
                false,
                0.0D,
                0.0D
            );
    }
}