package com.marcos.tailoredtraits.power;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;

public final class MovementPowerInitializer
    implements ModInitializer {

    /*
     * Botas configuradas como Ferro:
     *
     * quantidade máxima de ticks permitida
     * entre os dois toques na mesma tecla.
     *
     * 8 ticks equivalem a aproximadamente
     * 0,4 segundo.
     */
    private static final int
        IRON_DASH_DOUBLE_TAP_WINDOW =
            8;

    /*
     * Intervalo mínimo entre dois avanços.
     *
     * 20 ticks = 1 segundo.
     */
    private static final int
        IRON_DASH_COOLDOWN =
            20;

    /*
     * Força horizontal do avanço.
     */
    private static final double
        IRON_DASH_STRENGTH =
            1.15D;

    /*
     * Pequeno impulso vertical.
     *
     * Ele ajuda o dash a atravessar pequenos
     * desníveis e também funciona no ar.
     */
    private static final double
        IRON_DASH_VERTICAL_STRENGTH =
            0.05D;

    /*
     * Botas configuradas como Lápis-lazúli:
     *
     * identificador do modificador
     * de supervelocidade.
     */
    private static final Identifier
        LAPIS_BOOTS_SPEED_ID =
            TailoredTraits.id(
                "lapis_boots_super_speed"
            );

    /*
     * +100% de velocidade.
     *
     * ADD_MULTIPLIED_TOTAL com valor 1.0
     * dobra a velocidade total.
     */
    private static final AttributeModifier
        LAPIS_BOOTS_SPEED_MODIFIER =
            new AttributeModifier(
                LAPIS_BOOTS_SPEED_ID,
                1.0D,
                AttributeModifier.Operation
                    .ADD_MULTIPLIED_TOTAL
            );

    /*
     * Intervalo de consumo de experiência.
     *
     * 10 ticks = 0,5 segundo.
     */
    private static final int
        LAPIS_EXPERIENCE_DRAIN_INTERVAL =
            10;

    /*
     * Quantidade de pontos de experiência
     * consumidos a cada intervalo.
     */
    private static final int
        LAPIS_EXPERIENCE_DRAIN_AMOUNT =
            1;

    /*
     * Estado das teclas de movimento
     * no tick anterior.
     *
     * Isso permite identificar o momento exato
     * em que W, A, S ou D foi pressionado.
     */
    private static final Map<UUID, MovementInputState>
        PREVIOUS_MOVEMENT_INPUT =
            new HashMap<>();

    /*
     * Guarda separadamente o último toque
     * de cada tecla de movimento.
     *
     * Dessa forma:
     *
     * - dois toques em W ativam dash para frente;
     * - dois toques em S ativam dash para trás;
     * - dois toques em A ativam dash para a esquerda;
     * - dois toques em D ativam dash para a direita.
     */
    private static final Map<
        UUID,
        EnumMap<MovementKey, Integer>
    >
        LAST_MOVEMENT_PRESS_TICKS =
            new HashMap<>();

    /*
     * Guarda até qual tick o dash de Ferro
     * permanecerá em recarga.
     */
    private static final Map<UUID, Integer>
        IRON_DASH_COOLDOWN_UNTIL =
            new HashMap<>();

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            server -> {

                for (
                    ServerPlayer player :
                    server.getPlayerList().getPlayers()
                ) {
                    updateIronBootsDashPower(
                        player
                    );

                    updateLapisBootsSpeedPower(
                        player
                    );
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poderes adicionais de movimento registrados."
        );
    }

    /**
     * Botas configuradas como Ferro:
     *
     * pressionar rapidamente duas vezes a mesma
     * tecla de movimento executa um avanço.
     *
     * O dash pode ser usado no chão ou no ar.
     */
    private static void updateIronBootsDashPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasIronPower =
            hasSelectedMaterial(
                boots,
                PowerMaterial.IRON
            );

        /*
         * Sem as botas corretas, todo o estado
         * temporário do dash é apagado.
         */
        if (
            !hasIronPower
                || player.isSpectator()
        ) {
            clearIronDashState(
                playerId
            );

            return;
        }

        Input input =
            player.getLastClientInput();

        MovementInputState currentInput =
            MovementInputState.from(
                input
            );

        MovementInputState previousInput =
            PREVIOUS_MOVEMENT_INPUT
                .getOrDefault(
                    playerId,
                    MovementInputState.EMPTY
                );

        /*
         * Salva o estado atual para comparação
         * no próximo tick.
         */
        PREVIOUS_MOVEMENT_INPUT.put(
            playerId,
            currentInput
        );

        /*
         * Verifica separadamente cada uma
         * das quatro teclas de movimento.
         */
        for (
            MovementKey movementKey :
            MovementKey.values()
        ) {
            boolean isPressedNow =
                currentInput.isPressed(
                    movementKey
                );

            boolean wasPressedBefore =
                previousInput.isPressed(
                    movementKey
                );

            /*
             * Só processa quando a tecla acabou
             * de ser pressionada.
             *
             * Manter a tecla pressionada não conta
             * como vários toques.
             */
            if (
                !isPressedNow
                    || wasPressedBefore
            ) {
                continue;
            }

            boolean dashActivated =
                registerMovementKeyPress(
                    player,
                    movementKey
                );

            /*
             * Apenas um dash pode ser ativado
             * no mesmo tick.
             */
            if (dashActivated) {
                break;
            }
        }
    }

    /**
     * Registra um novo toque em W, A, S ou D
     * e verifica se ele completa o toque duplo.
     */
    private static boolean registerMovementKeyPress(
        ServerPlayer player,
        MovementKey movementKey
    ) {
        UUID playerId =
            player.getUUID();

        int currentTick =
            player.tickCount;

        EnumMap<MovementKey, Integer>
            playerPressTicks =
                LAST_MOVEMENT_PRESS_TICKS
                    .computeIfAbsent(
                        playerId,
                        ignored ->
                            new EnumMap<>(
                                MovementKey.class
                            )
                    );

        /*
         * Salva o toque atual e recebe o momento
         * em que essa mesma tecla foi pressionada
         * anteriormente.
         */
        Integer previousPressTick =
            playerPressTicks.put(
                movementKey,
                currentTick
            );

        /*
         * Este foi o primeiro toque dessa tecla.
         */
        if (previousPressTick == null) {
            return false;
        }

        int ticksBetweenPresses =
            currentTick
                - previousPressTick;

        /*
         * Os dois toques precisam ocorrer dentro
         * da janela de aproximadamente 0,4 segundo.
         *
         * Quando o intervalo é maior, o toque atual
         * já fica registrado como o primeiro toque
         * de uma nova tentativa.
         */
        if (
            ticksBetweenPresses <= 0
                || ticksBetweenPresses
                    > IRON_DASH_DOUBLE_TAP_WINDOW
        ) {
            return false;
        }

        int cooldownUntil =
            IRON_DASH_COOLDOWN_UNTIL
                .getOrDefault(
                    playerId,
                    0
                );

        /*
         * O dash ainda está em recarga.
         */
        if (currentTick < cooldownUntil) {
            return false;
        }

        /*
         * O dash pode ser realizado tanto no chão
         * quanto durante um salto ou uma queda.
         *
         * Por isso, não existe verificação
         * de player.onGround().
         */

        IRON_DASH_COOLDOWN_UNTIL.put(
            playerId,
            currentTick
                + IRON_DASH_COOLDOWN
        );

        /*
         * Depois da ativação, todos os toques
         * anteriores são removidos.
         *
         * Isso obriga o jogador a executar uma
         * nova sequência completa de dois toques.
         */
        playerPressTicks.clear();

        applyIronDash(
            player,
            movementKey
        );

        return true;
    }

    /**
     * Aplica o impulso do dash na direção
     * correspondente à tecla utilizada.
     */
    private static void applyIronDash(
        ServerPlayer player,
        MovementKey movementKey
    ) {
        double yawRadians =
            Math.toRadians(
                player.getYRot()
            );

        double sine =
            Math.sin(
                yawRadians
            );

        double cosine =
            Math.cos(
                yawRadians
            );

        /*
         * Direção para frente em relação
         * à rotação atual do jogador.
         */
        double forwardX =
            -sine;

        double forwardZ =
            cosine;

        double directionX;
        double directionZ;

        switch (movementKey) {

            /*
             * W:
             * avanço para a direção em que
             * o jogador está olhando.
             */
            case FORWARD -> {
                directionX =
                    forwardX;

                directionZ =
                    forwardZ;
            }

            /*
             * S:
             * avanço para trás.
             */
            case BACKWARD -> {
                directionX =
                    -forwardX;

                directionZ =
                    -forwardZ;
            }

            /*
             * A:
             * avanço para a esquerda em relação
             * à direção do jogador.
             */
            case LEFT -> {
                directionX =
                    cosine;

                directionZ =
                    sine;
            }

            /*
             * D:
             * avanço para a direita em relação
             * à direção do jogador.
             */
            case RIGHT -> {
                directionX =
                    -cosine;

                directionZ =
                    -sine;
            }

            default -> {
                return;
            }
        }

        /*
         * Adiciona o impulso à velocidade atual.
         *
         * Como não existe restrição de chão,
         * esse mesmo impulso funciona durante
         * saltos e quedas.
         */
        player.push(
            directionX
                * IRON_DASH_STRENGTH,
            IRON_DASH_VERTICAL_STRENGTH,
            directionZ
                * IRON_DASH_STRENGTH
        );

        /*
         * Obriga o servidor a sincronizar
         * a nova velocidade com o cliente.
         */
        player.hurtMarked =
            true;
    }

    /**
     * Limpa todos os dados temporários
     * relacionados ao dash de Ferro.
     */
    private static void clearIronDashState(
        UUID playerId
    ) {
        PREVIOUS_MOVEMENT_INPUT.remove(
            playerId
        );

        LAST_MOVEMENT_PRESS_TICKS.remove(
            playerId
        );

        IRON_DASH_COOLDOWN_UNTIL.remove(
            playerId
        );
    }

    /**
     * Botas configuradas como Lápis-lazúli:
     *
     * correr concede +100% de velocidade,
     * consumindo experiência a cada 0,5 segundo.
     */
    private static void updateLapisBootsSpeedPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasLapisPower =
            hasSelectedMaterial(
                boots,
                PowerMaterial.LAPIS_LAZULI
            );

        AttributeInstance movementSpeed =
            player.getAttribute(
                Attributes.MOVEMENT_SPEED
            );

        if (movementSpeed == null) {
            return;
        }

        boolean isCreative =
            player.getAbilities()
                .instabuild;

        boolean hasExperience =
            player.totalExperience
                > 0;

        /*
         * A supervelocidade funciona enquanto:
         *
         * - as botas estiverem configuradas;
         * - o jogador estiver correndo;
         * - houver experiência disponível;
         * - ou o jogador estiver no criativo;
         * - o jogador não estiver como espectador.
         */
        boolean shouldHaveSuperSpeed =
            hasLapisPower
                && player.isSprinting()
                && !player.isSpectator()
                && (
                    isCreative
                        || hasExperience
                );

        if (!shouldHaveSuperSpeed) {

            removeLapisSpeedModifier(
                movementSpeed
            );

            return;
        }

        /*
         * Adiciona o modificador apenas uma vez.
         */
        if (
            !movementSpeed.hasModifier(
                LAPIS_BOOTS_SPEED_ID
            )
        ) {
            movementSpeed.addTransientModifier(
                LAPIS_BOOTS_SPEED_MODIFIER
            );
        }

        /*
         * Jogadores no criativo utilizam
         * o poder sem consumir experiência.
         */
        if (isCreative) {
            return;
        }

        /*
         * Consome um ponto de experiência
         * a cada 0,5 segundo.
         */
        if (
            player.tickCount
                % LAPIS_EXPERIENCE_DRAIN_INTERVAL
                != 0
        ) {
            return;
        }

        player.giveExperiencePoints(
            -LAPIS_EXPERIENCE_DRAIN_AMOUNT
        );

        /*
         * Ao consumir o último ponto,
         * remove imediatamente a velocidade.
         */
        if (player.totalExperience <= 0) {
            removeLapisSpeedModifier(
                movementSpeed
            );
        }
    }

    /**
     * Remove o bônus de supervelocidade
     * das botas de Lápis-lazúli.
     */
    private static void removeLapisSpeedModifier(
        AttributeInstance movementSpeed
    ) {
        if (
            movementSpeed.hasModifier(
                LAPIS_BOOTS_SPEED_ID
            )
        ) {
            movementSpeed.removeModifier(
                LAPIS_BOOTS_SPEED_ID
            );
        }
    }

    /**
     * Verifica se a peça possui enfeite de Stevium
     * e está configurada com o material esperado.
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
     * Teclas aceitas para ativar o dash.
     */
    private enum MovementKey {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT
    }

    /**
     * Guarda o estado das quatro teclas
     * de movimento em um determinado tick.
     */
    private record MovementInputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right
    ) {

        private static final MovementInputState EMPTY =
            new MovementInputState(
                false,
                false,
                false,
                false
            );

        /**
         * Converte os dados recebidos do cliente
         * para o estado utilizado pelo sistema.
         */
        private static MovementInputState from(
            Input input
        ) {
            return new MovementInputState(
                input.forward(),
                input.backward(),
                input.left(),
                input.right()
            );
        }

        /**
         * Informa se a tecla solicitada
         * está pressionada.
         */
        private boolean isPressed(
            MovementKey movementKey
        ) {
            return switch (movementKey) {
                case FORWARD ->
                    forward;

                case BACKWARD ->
                    backward;

                case LEFT ->
                    left;

                case RIGHT ->
                    right;
            };
        }
    }
}