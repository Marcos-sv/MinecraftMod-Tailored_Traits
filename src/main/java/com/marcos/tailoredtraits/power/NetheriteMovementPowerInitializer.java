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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NetheriteMovementPowerInitializer
    implements ModInitializer {

    /*
     * Botas configuradas como Netherite:
     *
     * 40 ticks = 2 segundos caminhando
     * sobre a superfície da lava.
     */
    private static final int
        NETHERITE_BOOTS_LAVA_WALK_DURATION =
            40;

    /*
     * Coloca os pés do jogador ligeiramente
     * acima da superfície da lava.
     *
     * Isso evita que ele fique entrando e saindo
     * constantemente do fluido.
     */
    private static final double
        NETHERITE_BOOTS_SURFACE_OFFSET =
            0.02D;

    /*
     * Distância máxima que o jogador pode estar
     * da superfície para as botas conseguirem
     * segurá-lo sobre a lava.
     */
    private static final double
        NETHERITE_BOOTS_MAX_SNAP_DISTANCE =
            0.75D;

    /*
     * Distância usada para verificar os cantos
     * dos pés do jogador.
     */
    private static final double
        NETHERITE_BOOTS_FOOT_SAMPLE_INSET =
            0.08D;

    /*
     * Multiplicador aplicado quando o jogador
     * está correndo dentro da lava.
     */
    private static final double
        NETHERITE_LEGGINGS_SPRINT_MULTIPLIER =
            1.30D;

    /*
     * Guarda quantos ticks ainda restam para
     * cada jogador caminhar sobre a lava.
     */
    private static final Map<UUID, Integer>
        LAVA_WALK_TICKS_REMAINING =
            new HashMap<>();

    /*
     * Guarda os jogadores que já gastaram
     * os dois segundos das botas.
     *
     * O poder só poderá ser usado novamente
     * depois que o jogador sair da lava.
     */
    private static final Set<UUID>
        LAVA_WALK_EXHAUSTED =
            new HashSet<>();

    /*
     * O construtor precisa ser público porque
     * o Fabric cria esta classe pelo entrypoint.
     */
    public NetheriteMovementPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            server -> {

                for (
                    ServerPlayer player :
                    server.getPlayerList().getPlayers()
                ) {
                    updateNetheriteLeggingsPower(
                        player
                    );

                    updateNetheriteBootsPower(
                        player
                    );
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poderes de movimento da Netherite registrados."
        );
    }

    /**
     * Calça configurada como Netherite:
     *
     * restaura uma velocidade horizontal próxima
     * à velocidade normal do jogador enquanto
     * ele estiver dentro da lava.
     */
    private static void updateNetheriteLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasNetheritePower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.NETHERITE
            );

        /*
         * O poder só funciona:
         *
         * - com a calça correta;
         * - dentro da lava;
         * - fora do modo espectador;
         * - sem voo criativo.
         */
        if (
            !hasNetheritePower
                || player.isSpectator()
                || player.getAbilities().flying
                || !player.isInLava()
        ) {
            return;
        }

        Input input =
            player.getLastClientInput();

        /*
         * Valor positivo significa frente.
         * Valor negativo significa trás.
         */
        double forwardInput =
            (input.forward() ? 5.0D : 0.0D)
                - (input.backward() ? 5.0D : 0.0D);

        /*
         * Valor positivo significa direita.
         * Valor negativo significa esquerda.
         */
        double sidewaysInput =
            (input.right() ? 5.0D : 0.0D)
                - (input.left() ? 5.0D : 0.0D);

        /*
         * Sem entrada de movimento, não é
         * necessário alterar a velocidade.
         */
        if (
            forwardInput == 0.0D
                && sidewaysInput == 0.0D
        ) {
            return;
        }

        double yawRadians =
            Math.toRadians(
                player.getYRot()
            );

        /*
         * Direção para frente em relação
         * à rotação atual do jogador.
         */
        double forwardX =
            -Math.sin(
                yawRadians
            );

        double forwardZ =
            Math.cos(
                yawRadians
            );

        /*
         * Direção para a direita em relação
         * à rotação atual do jogador.
         */
        double rightX =
            -Math.cos(
                yawRadians
            );

        double rightZ =
            -Math.sin(
                yawRadians
            );

        /*
         * Combina W, A, S e D em uma
         * única direção horizontal.
         */
        double directionX =
            forwardX * forwardInput
                + rightX * sidewaysInput;

        double directionZ =
            forwardZ * forwardInput
                + rightZ * sidewaysInput;

        double directionLength =
            Math.sqrt(
                directionX * directionX
                    + directionZ * directionZ
            );

        if (directionLength <= 0.0001D) {
            return;
        }

        /*
         * Normaliza a direção para movimentos
         * diagonais não ficarem mais rápidos.
         */
        directionX /=
            directionLength;

        directionZ /=
            directionLength;

        /*
         * Usa a velocidade real do jogador.
         *
         * Isso mantém compatibilidade com outros
         * bônus, como as botas de Cobre.
         */
        double targetSpeed =
            player.getAttributeValue(
                Attributes.MOVEMENT_SPEED
            );

        if (player.isSprinting()) {
            targetSpeed *=
                NETHERITE_LEGGINGS_SPRINT_MULTIPLIER;
        }

        Vec3 currentMovement =
            player.getDeltaMovement();

        /*
         * Substitui apenas o movimento horizontal.
         *
         * O movimento vertical da lava continua
         * funcionando normalmente.
         */
        player.setDeltaMovement(
            directionX * targetSpeed,
            currentMovement.y,
            directionZ * targetSpeed
        );

        player.hurtMarked =
            true;
    }

    /**
     * Botas configuradas como Netherite:
     *
     * mantém o jogador sobre a superfície
     * da lava durante dois segundos.
     */
    private static void updateNetheriteBootsPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasNetheritePower =
            hasSelectedMaterial(
                boots,
                PowerMaterial.NETHERITE
            );

        /*
         * Situações nas quais o poder não
         * deve funcionar.
         */
        if (
            !hasNetheritePower
                || player.isSpectator()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.isPassenger()
        ) {
            clearLavaWalkState(
                playerId
            );

            return;
        }

        LavaSurface lavaSurface =
            findLavaSurfaceUnderPlayer(
                player
            );

        /*
         * Não existe lava imediatamente
         * abaixo dos pés.
         */
        if (lavaSurface == null) {

            /*
             * Quando o jogador sai completamente
             * da lava, o poder é recarregado.
             */
            if (!player.isInLava()) {
                LAVA_WALK_EXHAUSTED.remove(
                    playerId
                );

                LAVA_WALK_TICKS_REMAINING.remove(
                    playerId
                );
            }

            return;
        }

        /*
         * O jogador já gastou os dois segundos.
         *
         * Ele deverá sair da lava antes de
         * poder utilizar o poder novamente.
         */
        if (
            LAVA_WALK_EXHAUSTED.contains(
                playerId
            )
        ) {
            return;
        }

        /*
         * Na primeira ativação são concedidos
         * 40 ticks.
         */
        int remainingTicks =
            LAVA_WALK_TICKS_REMAINING
                .getOrDefault(
                    playerId,
                    NETHERITE_BOOTS_LAVA_WALK_DURATION
                );

        if (remainingTicks <= 0) {
            LAVA_WALK_TICKS_REMAINING.remove(
                playerId
            );

            LAVA_WALK_EXHAUSTED.add(
                playerId
            );

            return;
        }

        double targetY =
            lavaSurface.surfaceY()
                + NETHERITE_BOOTS_SURFACE_OFFSET;

        double verticalDifference =
            targetY
                - player.getY();

        /*
         * Evita puxar o jogador até a superfície
         * quando ele já estiver muito fundo.
         */
        if (
            Math.abs(
                verticalDifference
            ) > NETHERITE_BOOTS_MAX_SNAP_DISTANCE
        ) {
            return;
        }

        Vec3 currentMovement =
            player.getDeltaMovement();

        /*
         * Mantém os pés sobre a superfície
         * da lava.
         */
        player.setPos(
            player.getX(),
            targetY,
            player.getZ()
        );

        /*
         * Remove a queda vertical, mas preserva
         * o movimento horizontal.
         */
        player.setDeltaMovement(
            currentMovement.x,
            0.0D,
            currentMovement.z
        );

        /*
         * Faz o Minecraft tratar a superfície
         * como um apoio durante o poder.
         */
        player.setOnGround(
            true
        );

        /*
         * Impede dano de queda depois que
         * os dois segundos terminarem.
         */
        player.resetFallDistance();

        player.hurtMarked =
            true;

        remainingTicks--;

        /*
         * O tempo terminou.
         */
        if (remainingTicks <= 0) {
            LAVA_WALK_TICKS_REMAINING.remove(
                playerId
            );

            LAVA_WALK_EXHAUSTED.add(
                playerId
            );

            return;
        }

        LAVA_WALK_TICKS_REMAINING.put(
            playerId,
            remainingTicks
        );
    }

    /**
     * Procura a superfície da lava abaixo
     * do centro e dos quatro cantos dos pés.
     */
    private static LavaSurface
        findLavaSurfaceUnderPlayer(
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

        double minimumX =
            boundingBox.minX
                + NETHERITE_BOOTS_FOOT_SAMPLE_INSET;

        double maximumX =
            boundingBox.maxX
                - NETHERITE_BOOTS_FOOT_SAMPLE_INSET;

        double minimumZ =
            boundingBox.minZ
                + NETHERITE_BOOTS_FOOT_SAMPLE_INSET;

        double maximumZ =
            boundingBox.maxZ
                - NETHERITE_BOOTS_FOOT_SAMPLE_INSET;

        /*
         * Centro e quatro cantos dos pés.
         *
         * Isso permite caminhar sobre as bordas
         * dos blocos de lava sem afundar.
         */
        double[][] samples = {
            {
                centerX,
                centerZ
            },
            {
                minimumX,
                minimumZ
            },
            {
                minimumX,
                maximumZ
            },
            {
                maximumX,
                minimumZ
            },
            {
                maximumX,
                maximumZ
            }
        };

        LavaSurface highestSurface =
            null;

        for (
            double[] sample :
            samples
        ) {
            LavaSurface candidate =
                findLavaSurfaceAt(
                    player,
                    sample[0],
                    sample[1]
                );

            if (candidate == null) {
                continue;
            }

            /*
             * Em lava corrente, superfícies próximas
             * podem possuir alturas diferentes.
             *
             * Utiliza a mais alta para evitar
             * que os pés fiquem submersos.
             */
            if (
                highestSurface == null
                    || candidate.surfaceY()
                        > highestSurface.surfaceY()
            ) {
                highestSurface =
                    candidate;
            }
        }

        return highestSurface;
    }

    /**
     * Procura lava em uma coordenada específica
     * abaixo dos pés do jogador.
     */
    private static LavaSurface findLavaSurfaceAt(
        ServerPlayer player,
        double sampleX,
        double sampleZ
    ) {
        int baseY =
            (int) Math.floor(
                player.getY()
            );

        /*
         * Verifica o bloco atual dos pés
         * e o bloco imediatamente abaixo.
         */
        for (
            int y = baseY;
            y >= baseY - 1;
            y--
        ) {
            BlockPos blockPosition =
                BlockPos.containing(
                    sampleX,
                    y,
                    sampleZ
                );

            FluidState fluidState =
                player.level().getFluidState(
                    blockPosition
                );

            if (
                !fluidState.is(
                    FluidTags.LAVA
                )
            ) {
                continue;
            }

            /*
             * Obtém a altura real da lava.
             *
             * Isso também funciona com
             * lava corrente e níveis menores.
             */
            double surfaceY =
                blockPosition.getY()
                    + fluidState.getHeight(
                        player.level(),
                        blockPosition
                    );

            double playerDistanceFromSurface =
                player.getY()
                    - surfaceY;

            if (
                playerDistanceFromSurface
                    > NETHERITE_BOOTS_MAX_SNAP_DISTANCE
                    || playerDistanceFromSurface
                        < -NETHERITE_BOOTS_MAX_SNAP_DISTANCE
            ) {
                continue;
            }

            return new LavaSurface(
                surfaceY
            );
        }

        return null;
    }

    /**
     * Apaga todos os dados temporários
     * relacionados às botas de Netherite.
     */
    private static void clearLavaWalkState(
        UUID playerId
    ) {
        LAVA_WALK_TICKS_REMAINING.remove(
            playerId
        );

        LAVA_WALK_EXHAUSTED.remove(
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
     * Guarda a altura encontrada para
     * a superfície da lava.
     */
    private record LavaSurface(
        double surfaceY
    ) {
    }
}