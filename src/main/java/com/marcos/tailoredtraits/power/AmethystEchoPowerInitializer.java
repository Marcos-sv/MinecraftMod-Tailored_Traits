package com.marcos.tailoredtraits.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.EntityPositionSource;

public final class AmethystEchoPowerInitializer
    implements ModInitializer {

    /*
     * Raio em que os mobs serão detectados.
     *
     * 48 blocos equivalem aproximadamente
     * a três chunks.
     */
    private static final double
        AMETHYST_ECHO_RADIUS =
            48.0D;

    /*
     * Velocidade visual da vibração.
     *
     * A partícula percorre aproximadamente
     * dois blocos por tick.
     *
     * 20 ticks equivalem a um segundo.
     */
    private static final double
        VIBRATION_BLOCKS_PER_TICK =
            2.0D;

    /*
     * Mesmo para mobs muito próximos, a partícula
     * terá pelo menos três ticks de duração.
     *
     * Isso impede que ela apareça e desapareça
     * rápido demais para ser visualizada.
     */
    private static final int
        MINIMUM_TRAVEL_TICKS =
            3;

    /*
     * Guarda se o jogador estava encostando
     * no chão no tick anterior.
     *
     * Isso permite diferenciar:
     *
     * - salto verdadeiro;
     * - queda;
     * - aterrissagem;
     * - saída de uma borda.
     */
    private static final Map<UUID, Boolean>
        WAS_ON_GROUND =
            new HashMap<>();

    /*
     * Guarda as vibrações que já chegaram
     * aos mobs e ainda precisam retornar.
     */
    private static final List<PendingReturnVibration>
        PENDING_RETURN_VIBRATIONS =
            new ArrayList<>();

    /*
     * O construtor precisa ser público porque
     * o Fabric cria esta classe pelo entrypoint.
     */
    public AmethystEchoPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            AmethystEchoPowerInitializer
                ::updateAmethystEchoPower
        );

        TailoredTraits.LOGGER.info(
            "Poder de sonar das botas de Ametista registrado."
        );
    }

    /**
     * Atualiza o sistema a cada tick do servidor.
     */
    private static void updateAmethystEchoPower(
        MinecraftServer server
    ) {
        /*
         * Primeiro processa as partículas
         * que precisam voltar dos mobs.
         */
        processPendingReturnVibrations(
            server
        );

        int currentServerTick =
            server.getTickCount();

        /*
         * Depois verifica se algum jogador
         * acabou de realizar um salto.
         */
        for (
            ServerPlayer player :
            server.getPlayerList().getPlayers()
        ) {
            detectGroundJump(
                player,
                currentServerTick
            );
        }
    }

    /**
     * Detecta somente um salto iniciado
     * enquanto o jogador estava no chão.
     */
    private static void detectGroundJump(
        ServerPlayer player,
        int currentServerTick
    ) {
        UUID playerId =
            player.getUUID();

        boolean isOnGroundNow =
            player.onGround();

        boolean wasOnGround =
            WAS_ON_GROUND.getOrDefault(
                playerId,
                isOnGroundNow
            );

        /*
         * Guarda o estado atual para
         * comparar no próximo tick.
         */
        WAS_ON_GROUND.put(
            playerId,
            isOnGroundNow
        );

        /*
         * Sem as botas configuradas como
         * Ametista, o poder não funciona.
         */
        if (
            !hasAmethystBootsPower(
                player
            )
        ) {
            return;
        }

        /*
         * Impede ativação em situações
         * incompatíveis com um salto comum.
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
         * Para ser um salto válido:
         *
         * - estava no chão;
         * - agora está no ar;
         * - a tecla de pular está pressionada;
         * - o movimento vertical é para cima.
         */
        boolean startedRealGroundJump =
            wasOnGround
                && !isOnGroundNow
                && input.jump()
                && player.getDeltaMovement().y
                    > 0.0D;

        if (!startedRealGroundJump) {
            return;
        }

        emitOutgoingVibrations(
            player,
            currentServerTick
        );
    }

    /**
     * Cria uma vibração saindo do jogador
     * em direção a cada mob encontrado.
     */
    private static void emitOutgoingVibrations(
        ServerPlayer player,
        int currentServerTick
    ) {
        ServerLevel level =
            player.level();

        double radiusSquared =
            AMETHYST_ECHO_RADIUS
                * AMETHYST_ECHO_RADIUS;

        /*
         * Procura mobs vivos dentro do raio.
         *
         * Isso inclui:
         *
         * - monstros;
         * - animais;
         * - aldeões;
         * - criaturas aquáticas;
         * - criaturas do Nether;
         * - criaturas do End.
         *
         * Outros jogadores não entram na procura.
         */
        List<Mob> nearbyMobs =
            level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(
                    AMETHYST_ECHO_RADIUS
                ),
                mob ->
                    mob.isAlive()
                        && mob.distanceToSqr(
                            player
                        ) <= radiusSquared
            );

        for (
            Mob mob :
            nearbyMobs
        ) {
            double distance =
                Math.sqrt(
                    mob.distanceToSqr(
                        player
                    )
                );

            int travelTicks =
                calculateTravelTicks(
                    distance
                );

            /*
             * Cria a partícula de vibração.
             *
             * Ela começa na posição do jogador
             * e acompanha o mob como destino.
             */
            VibrationParticleOption
                outgoingVibration =
                    new VibrationParticleOption(
                        new EntityPositionSource(
                            mob,
                            mob.getBbHeight()
                                * 0.5F
                        ),
                        travelTicks
                    );

            /*
             * Envia a partícula somente para
             * o jogador que possui as botas.
             *
             * A origem da partícula é a posição
             * dos olhos do jogador.
             */
            level.sendParticles(
                player,
                outgoingVibration,
                true,
                true,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
            );

            /*
             * Agenda a vibração de retorno
             * para começar quando a primeira
             * partícula alcançar o mob.
             */
            PENDING_RETURN_VIBRATIONS.add(
                new PendingReturnVibration(
                    currentServerTick
                        + travelTicks,
                    player.getUUID(),
                    mob.getUUID(),
                    travelTicks
                )
            );
        }
    }

    /**
     * Inicia as vibrações de retorno
     * quando as ondas chegam aos mobs.
     */
    private static void processPendingReturnVibrations(
        MinecraftServer server
    ) {
        int currentServerTick =
            server.getTickCount();

        Iterator<PendingReturnVibration>
            iterator =
                PENDING_RETURN_VIBRATIONS
                    .iterator();

        while (
            iterator.hasNext()
        ) {
            PendingReturnVibration pending =
                iterator.next();

            /*
             * A vibração de ida ainda
             * não alcançou o mob.
             */
            if (
                pending.startAtTick()
                    > currentServerTick
            ) {
                continue;
            }

            /*
             * Remove a entrada da fila.
             */
            iterator.remove();

            ServerPlayer player =
                server.getPlayerList().getPlayer(
                    pending.playerId()
                );

            /*
             * O jogador pode ter saído
             * do servidor durante o trajeto.
             */
            if (player == null) {
                continue;
            }

            ServerLevel level =
                player.level();

            Entity mobEntity =
                level.getEntityInAnyDimension(
                    pending.mobId()
                );

            /*
             * O mob pode ter morrido, desaparecido
             * ou mudado de dimensão.
             */
            if (
                !(mobEntity
                    instanceof Mob mob)
                    || !mob.isAlive()
                    || mob.level() != level
            ) {
                continue;
            }

            /*
             * Cria uma nova vibração cujo
             * destino agora é o jogador.
             */
            VibrationParticleOption
                returnVibration =
                    new VibrationParticleOption(
                        new EntityPositionSource(
                            player,
                            player.getBbHeight()
                                * 0.5F
                        ),
                        pending.travelTicks()
                    );

            /*
             * A vibração começa na posição atual
             * do mob e volta até o jogador.
             */
            level.sendParticles(
                player,
                returnVibration,
                true,
                true,
                mob.getX(),
                mob.getEyeY(),
                mob.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
            );
        }
    }

    /**
     * Calcula o tempo que a partícula levará
     * para percorrer a distância.
     */
    private static int calculateTravelTicks(
        double distance
    ) {
        return Math.max(
            MINIMUM_TRAVEL_TICKS,
            (int) Math.ceil(
                distance
                    / VIBRATION_BLOCKS_PER_TICK
            )
        );
    }

    /**
     * Verifica se o jogador utiliza botas
     * com acabamento de Stevium configuradas
     * como Ametista.
     */
    private static boolean hasAmethystBootsPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        if (
            !SteviumArmorUtil.hasSteviumTrim(
                boots
            )
        ) {
            return false;
        }

        String selectedMaterial =
            boots.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        return selectedMaterial.equals(
            PowerMaterial.AMETHYST.getId()
        );
    }

    /**
     * Guarda os dados necessários para
     * iniciar uma vibração de retorno.
     */
    private record PendingReturnVibration(
        int startAtTick,
        UUID playerId,
        UUID mobId,
        int travelTicks
    ) {
    }
}