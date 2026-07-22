package com.marcos.tailoredtraits.power;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class SteviumTamingInitializer
    implements ModInitializer {

    /*
     * Velocidade usada quando o
     * companheiro segue seu dono.
     */
    private static final double
        FOLLOW_SPEED =
            1.15D;

    /*
     * Velocidade usada durante
     * a perseguição de um inimigo.
     */
    private static final double
        COMBAT_SPEED =
            1.25D;

    /*
     * O companheiro começa a seguir
     * quando está a mais de cinco blocos.
     */
    private static final double
        FOLLOW_START_DISTANCE_SQUARED =
            25.0D;

    /*
     * O companheiro interrompe a navegação
     * quando está a aproximadamente
     * três blocos do dono.
     */
    private static final double
        FOLLOW_STOP_DISTANCE_SQUARED =
            9.0D;

    /*
     * Acima de 32 blocos, o companheiro
     * tenta se teletransportar.
     */
    private static final double
        TELEPORT_DISTANCE_SQUARED =
            1024.0D;

    /*
     * O companheiro não perseguirá um alvo
     * que esteja a mais de 48 blocos do dono.
     */
    private static final double
        MAX_COMBAT_DISTANCE_SQUARED =
            2304.0D;

    /*
     * Distância necessária para
     * executar um ataque corpo a corpo.
     */
    private static final double
        MINIMUM_ATTACK_REACH =
            2.0D;

    /*
     * Intervalo entre os ataques.
     *
     * Vinte ticks equivalem a
     * aproximadamente um segundo.
     */
    private static final int
        ATTACK_INTERVAL_TICKS =
            20;

    /*
     * Dano usado por entidades que não
     * possuem o atributo ATTACK_DAMAGE.
     *
     * Isso inclui aldeões e grande parte
     * dos animais normalmente passivos.
     *
     * Três unidades equivalem a
     * um coração e meio.
     */
    private static final float
        DEFAULT_COMPANION_ATTACK_DAMAGE =
            3.0F;

    /*
     * Menor dano permitido para um
     * companheiro que tenha um atributo
     * de ataque igual ou menor que zero.
     */
    private static final float
        MINIMUM_COMPANION_ATTACK_DAMAGE =
            1.0F;

    /*
     * Força horizontal aplicada ao alvo
     * quando o ataque é bem-sucedido.
     */
    private static final double
        ATTACK_KNOCKBACK_STRENGTH =
            0.30D;

    /*
     * Força vertical aplicada ao alvo.
     */
    private static final double
        ATTACK_VERTICAL_KNOCKBACK =
            0.10D;

    /*
     * Companheiros que estão atualmente
     * carregados no servidor.
     */
    private static final Set<Mob>
        LOADED_COMPANIONS =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

    /*
     * Informações temporárias de combate.
     *
     * O dono permanente continua sendo
     * salvo pelo SteviumCompanionData.
     */
    private static final Map<
        UUID,
        CompanionRuntimeState
    > RUNTIME_STATES =
        new HashMap<>();

    /*
     * Posições testadas ao teletransportar
     * um companheiro para perto do dono.
     */
    private static final double[][]
        TELEPORT_OFFSETS = {
            {2.0D, 0.0D},
            {-2.0D, 0.0D},
            {0.0D, 2.0D},
            {0.0D, -2.0D},
            {2.0D, 2.0D},
            {2.0D, -2.0D},
            {-2.0D, 2.0D},
            {-2.0D, -2.0D},
            {3.0D, 0.0D},
            {-3.0D, 0.0D},
            {0.0D, 3.0D},
            {0.0D, -3.0D}
        };

    public SteviumTamingInitializer() {
    }

    @Override
    public void onInitialize() {

        /*
         * Registra os dados permanentes
         * usados pelos companheiros.
         */
        SteviumCompanionData.initialize();

        /*
         * Detecta o clique direito com
         * um osso sobre um mob.
         */
        UseEntityCallback.EVENT.register(
            SteviumTamingInitializer
                ::handleEntityInteraction
        );

        /*
         * Reconhece companheiros que foram
         * carregados junto com o mundo.
         */
        ServerEntityEvents.ENTITY_LOAD.register(
            (
                entity,
                level
            ) -> handleEntityLoad(
                entity
            )
        );

        /*
         * Remove referências temporárias
         * quando a entidade é descarregada.
         */
        ServerEntityEvents.ENTITY_UNLOAD.register(
            (
                entity,
                level
            ) -> handleEntityUnload(
                entity
            )
        );

        /*
         * Atualiza os companheiros
         * no final de cada tick do nível.
         */
        ServerTickEvents.END_LEVEL_TICK.register(
            SteviumTamingInitializer
                ::tickCompanions
        );

        /*
         * Impede que um companheiro ataque:
         *
         * - seu próprio dono;
         * - companheiros do mesmo dono;
         * - alvos não autorizados.
         */
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
            SteviumTamingInitializer
                ::allowCompanionDamage
        );

        TailoredTraits.LOGGER.info(
            "Sistema de domesticação do Stevium registrado."
        );
    }

    /**
     * Processa o clique direito
     * realizado sobre uma entidade.
     */
    private static InteractionResult
        handleEntityInteraction(
            Player player,
            Level level,
            InteractionHand hand,
            Entity entity,
            EntityHitResult hitResult
        ) {

        /*
         * Somente entidades da classe Mob
         * podem ser transformadas em
         * companheiros.
         */
        if (
            !(entity instanceof Mob mob)
        ) {
            return InteractionResult.PASS;
        }

        ItemStack heldStack =
            player.getItemInHand(
                hand
            );

        /*
         * A domesticação exige um osso.
         */
        if (
            !heldStack.is(
                Items.BONE
            )
        ) {
            return InteractionResult.PASS;
        }

        /*
         * O poder de conjunto precisa
         * estar configurado como Stevium.
         */
        boolean hasSteviumFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    player,
                    PowerMaterial.STEVIUM
                );

        if (!hasSteviumFullSet) {
            return InteractionResult.PASS;
        }

        /*
         * No cliente, confirma a interação
         * e envia o clique ao servidor.
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (
            !(player
                instanceof ServerPlayer
                    serverPlayer)
                || !(level
                    instanceof ServerLevel
                        serverLevel)
        ) {
            return InteractionResult.PASS;
        }

        return tameMob(
            serverPlayer,
            serverLevel,
            mob,
            heldStack
        );
    }

    /**
     * Salva o dono e transforma o mob
     * em um companheiro de Stevium.
     */
    private static InteractionResult tameMob(
        ServerPlayer player,
        ServerLevel level,
        Mob mob,
        ItemStack boneStack
    ) {
        UUID existingOwnerUuid =
            SteviumCompanionData
                .getOwnerUuid(
                    mob
                );

        /*
         * O mob já possui dono.
         */
        if (existingOwnerUuid != null) {
            if (
                existingOwnerUuid.equals(
                    player.getUUID()
                )
            ) {
                player.sendSystemMessage(
                    Component.literal(
                        "Este mob já é seu companheiro."
                    ),
                    true
                );
            } else {
                player.sendSystemMessage(
                    Component.literal(
                        "Este mob já possui outro dono."
                    ),
                    true
                );
            }

            return InteractionResult.SUCCESS;
        }

        boolean ownerSaved =
            SteviumCompanionData
                .setOwner(
                    mob,
                    player.getUUID()
                );

        if (!ownerSaved) {
            player.sendSystemMessage(
                Component.literal(
                    "Não foi possível domesticar este mob."
                ),
                true
            );

            return InteractionResult.SUCCESS;
        }

        /*
         * Remove o alvo antigo do mob.
         *
         * Isso impede que um monstro continue
         * atacando o jogador depois de ser
         * domesticado.
         */
        clearNativeTarget(
            mob
        );

        mob.getNavigation()
            .stop();

        mob.setPersistenceRequired();

        /*
         * Consome um osso no sobrevivência.
         */
        if (
            !player.getAbilities()
                .instabuild
        ) {
            boneStack.shrink(
                1
            );
        }

        LOADED_COMPANIONS.add(
            mob
        );

        RUNTIME_STATES.put(
            mob.getUUID(),
            CompanionRuntimeState.create(
                mob,
                player
            )
        );

        level.sendParticles(
            ParticleTypes.HEART,
            mob.getX(),
            mob.getY()
                + mob.getBbHeight()
                    * 0.75D,
            mob.getZ(),
            18,
            0.45D,
            0.55D,
            0.45D,
            0.10D
        );

        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            mob.getX(),
            mob.getY()
                + mob.getBbHeight()
                    * 0.50D,
            mob.getZ(),
            10,
            0.40D,
            0.40D,
            0.40D,
            0.05D
        );

        player.playSound(
            SoundEvents.PLAYER_LEVELUP,
            0.75F,
            1.40F
        );

        player.sendSystemMessage(
            Component.literal(
                "Você domesticou "
            ).append(
                mob.getDisplayName()
            ).append(
                Component.literal(
                    " usando o poder do Stevium."
                )
            ),
            true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Reconhece um companheiro
     * que foi carregado pelo mundo.
     */
    private static void handleEntityLoad(
        Entity entity
    ) {
        if (
            !(entity instanceof Mob mob)
        ) {
            return;
        }

        if (
            !SteviumCompanionData
                .hasOwner(
                    mob
                )
        ) {
            return;
        }

        mob.setPersistenceRequired();

        clearNativeTarget(
            mob
        );

        LOADED_COMPANIONS.add(
            mob
        );
    }

    /**
     * Remove os dados temporários
     * de uma entidade descarregada.
     */
    private static void handleEntityUnload(
        Entity entity
    ) {
        if (
            !(entity instanceof Mob mob)
        ) {
            return;
        }

        LOADED_COMPANIONS.remove(
            mob
        );

        RUNTIME_STATES.remove(
            mob.getUUID()
        );
    }

    /**
     * Atualiza os companheiros
     * pertencentes ao nível atual.
     */
    private static void tickCompanions(
        ServerLevel level
    ) {
        for (
            Mob mob :
            Set.copyOf(
                LOADED_COMPANIONS
            )
        ) {
            if (
                mob.level() != level
            ) {
                continue;
            }

            if (
                !mob.isAlive()
                    || mob.isRemoved()
            ) {
                LOADED_COMPANIONS.remove(
                    mob
                );

                RUNTIME_STATES.remove(
                    mob.getUUID()
                );

                continue;
            }

            tickCompanion(
                level,
                mob
            );
        }
    }

    /**
     * Atualiza um companheiro específico.
     */
    private static void tickCompanion(
        ServerLevel level,
        Mob mob
    ) {
        UUID ownerUuid =
            SteviumCompanionData
                .getOwnerUuid(
                    mob
                );

        if (ownerUuid == null) {
            LOADED_COMPANIONS.remove(
                mob
            );

            RUNTIME_STATES.remove(
                mob.getUUID()
            );

            return;
        }

        ServerPlayer owner =
            level.getServer()
                .getPlayerList()
                .getPlayer(
                    ownerUuid
                );

        /*
         * Impede que a inteligência original
         * do mob mantenha um alvo próprio.
         */
        clearNativeTarget(
            mob
        );

        /*
         * Sem dono disponível, o companheiro
         * permanece parado e não luta.
         */
        if (
            owner == null
                || !owner.isAlive()
                || owner.isSpectator()
                || owner.level() != level
        ) {
            mob.getNavigation()
                .stop();

            CompanionRuntimeState state =
                RUNTIME_STATES.get(
                    mob.getUUID()
                );

            if (state != null) {
                state.commandedTarget =
                    null;
            }

            return;
        }

        CompanionRuntimeState state =
            RUNTIME_STATES.computeIfAbsent(
                mob.getUUID(),
                ignored ->
                    CompanionRuntimeState.create(
                        mob,
                        owner
                    )
            );

        updateCombatOrders(
            level,
            mob,
            owner,
            ownerUuid,
            state
        );

        LivingEntity combatTarget =
            state.commandedTarget;

        if (
            isValidCombatTarget(
                level,
                owner,
                ownerUuid,
                combatTarget
            )
        ) {
            updateCompanionCombat(
                level,
                mob,
                combatTarget,
                state
            );

            return;
        }

        state.commandedTarget =
            null;

        updateFollowing(
            level,
            mob,
            owner
        );
    }

    /**
     * Detecta ataques envolvendo
     * o dono ou o companheiro.
     */
    private static void updateCombatOrders(
        ServerLevel level,
        Mob mob,
        ServerPlayer owner,
        UUID ownerUuid,
        CompanionRuntimeState state
    ) {

        /*
         * O companheiro foi atacado.
         */
        int companionHurtTimestamp =
            mob.getLastHurtByMobTimestamp();

        if (
            companionHurtTimestamp
                != state.lastCompanionHurtTimestamp
        ) {
            state.lastCompanionHurtTimestamp =
                companionHurtTimestamp;

            LivingEntity attacker =
                mob.getLastHurtByMob();

            assignCombatTarget(
                level,
                owner,
                ownerUuid,
                state,
                attacker
            );
        }

        /*
         * O dono foi atacado.
         */
        int ownerHurtTimestamp =
            owner.getLastHurtByMobTimestamp();

        if (
            ownerHurtTimestamp
                != state.lastOwnerHurtTimestamp
        ) {
            state.lastOwnerHurtTimestamp =
                ownerHurtTimestamp;

            LivingEntity attacker =
                owner.getLastHurtByMob();

            assignCombatTarget(
                level,
                owner,
                ownerUuid,
                state,
                attacker
            );
        }

        /*
         * O dono atacou uma entidade.
         */
        int ownerAttackTimestamp =
            owner.getLastHurtMobTimestamp();

        if (
            ownerAttackTimestamp
                != state.lastOwnerAttackTimestamp
        ) {
            state.lastOwnerAttackTimestamp =
                ownerAttackTimestamp;

            LivingEntity attackedEntity =
                owner.getLastHurtMob();

            assignCombatTarget(
                level,
                owner,
                ownerUuid,
                state,
                attackedEntity
            );
        }
    }

    /**
     * Define um novo alvo autorizado.
     */
    private static void assignCombatTarget(
        ServerLevel level,
        ServerPlayer owner,
        UUID ownerUuid,
        CompanionRuntimeState state,
        LivingEntity candidate
    ) {
        if (
            !isValidCombatTarget(
                level,
                owner,
                ownerUuid,
                candidate
            )
        ) {
            return;
        }

        state.commandedTarget =
            candidate;
    }

    /**
     * Verifica se uma entidade pode
     * ser atacada pelo companheiro.
     */
    private static boolean isValidCombatTarget(
        ServerLevel level,
        ServerPlayer owner,
        UUID ownerUuid,
        LivingEntity candidate
    ) {
        if (
            candidate == null
                || !candidate.isAlive()
                || candidate.isRemoved()
                || candidate == owner
                || candidate.level() != level
                || candidate.isSpectator()
        ) {
            return false;
        }

        /*
         * Não permite atacar um companheiro
         * pertencente ao mesmo jogador.
         */
        if (
            candidate
                instanceof Mob candidateMob
                && SteviumCompanionData
                    .isOwnedBy(
                        candidateMob,
                        ownerUuid
                    )
        ) {
            return false;
        }

        return candidate.distanceToSqr(
            owner
        ) <= MAX_COMBAT_DISTANCE_SQUARED;
    }

    /**
     * Faz o companheiro perseguir
     * e atacar o alvo autorizado.
     */
    private static void updateCompanionCombat(
        ServerLevel level,
        Mob mob,
        LivingEntity target,
        CompanionRuntimeState state
    ) {
        /*
         * Não usa mob.setTarget(target).
         *
         * Aldeões e outros mobs passivos não
         * possuem uma inteligência de ataque
         * compatível com alvos de combate.
         *
         * O caminho é controlado diretamente.
         */
        clearNativeTarget(
            mob
        );

        mob.getNavigation()
            .moveTo(
                target,
                COMBAT_SPEED
            );

        double attackReach =
            Math.max(
                MINIMUM_ATTACK_REACH,
                mob.getBbWidth()
                    * 1.50D
                    + target.getBbWidth()
            );

        double attackReachSquared =
            attackReach
                * attackReach;

        if (
            mob.distanceToSqr(
                target
            ) > attackReachSquared
        ) {
            return;
        }

        int currentTick =
            level.getServer()
                .getTickCount();

        if (
            currentTick
                < state.nextAttackTick
        ) {
            return;
        }

        performUniversalCompanionAttack(
            level,
            mob,
            target
        );

        state.nextAttackTick =
            currentTick
                + ATTACK_INTERVAL_TICKS;
    }

    /**
     * Executa um ataque que funciona
     * para qualquer tipo de Mob.
     *
     * Não utiliza doHurtTarget, pois esse
     * método depende da implementação
     * específica de combate do mob.
     */
    private static void
        performUniversalCompanionAttack(
            ServerLevel level,
            Mob companion,
            LivingEntity target
        ) {
        companion.swing(
            InteractionHand.MAIN_HAND
        );

        float attackDamage =
            getUniversalAttackDamage(
                companion
            );

        DamageSource damageSource =
            companion.damageSources()
                .mobAttack(
                    companion
                );

        boolean damaged =
            target.hurtServer(
                level,
                damageSource,
                attackDamage
            );

        if (!damaged) {
            return;
        }

        Vec3 knockbackDirection =
            target.position()
                .subtract(
                    companion.position()
                );

        if (
            knockbackDirection.lengthSqr()
                > 1.0E-6D
        ) {
            knockbackDirection =
                knockbackDirection.normalize();

            target.push(
                knockbackDirection.x
                    * ATTACK_KNOCKBACK_STRENGTH,
                ATTACK_VERTICAL_KNOCKBACK,
                knockbackDirection.z
                    * ATTACK_KNOCKBACK_STRENGTH
            );

            target.hurtMarked =
                true;
        }
    }

    /**
     * Retorna o dano do ataque universal.
     *
     * Mobs combatentes utilizam o próprio
     * atributo ATTACK_DAMAGE.
     *
     * Mobs sem esse atributo, como aldeões,
     * utilizam o dano padrão do sistema.
     */
    private static float getUniversalAttackDamage(
        Mob companion
    ) {
        if (
            companion.getAttributes()
                .hasAttribute(
                    Attributes.ATTACK_DAMAGE
                )
        ) {
            double attributeDamage =
                companion.getAttributeValue(
                    Attributes.ATTACK_DAMAGE
                );

            return (float) Math.max(
                MINIMUM_COMPANION_ATTACK_DAMAGE,
                attributeDamage
            );
        }

        return DEFAULT_COMPANION_ATTACK_DAMAGE;
    }

    /**
     * Faz o companheiro acompanhar
     * o dono quando não está lutando.
     */
    private static void updateFollowing(
        ServerLevel level,
        Mob mob,
        ServerPlayer owner
    ) {
        clearNativeTarget(
            mob
        );

        if (mob.isPassenger()) {
            return;
        }

        double ownerDistanceSquared =
            mob.distanceToSqr(
                owner
            );

        if (
            ownerDistanceSquared
                >= TELEPORT_DISTANCE_SQUARED
        ) {
            if (
                tryTeleportNearOwner(
                    level,
                    mob,
                    owner
                )
            ) {
                return;
            }
        }

        if (
            ownerDistanceSquared
                > FOLLOW_START_DISTANCE_SQUARED
        ) {
            mob.getNavigation()
                .moveTo(
                    owner,
                    FOLLOW_SPEED
                );

            return;
        }

        if (
            ownerDistanceSquared
                <= FOLLOW_STOP_DISTANCE_SQUARED
        ) {
            mob.getNavigation()
                .stop();
        }
    }

    /**
     * Tenta colocar o companheiro
     * em uma posição livre perto do dono.
     */
    private static boolean tryTeleportNearOwner(
        ServerLevel level,
        Mob mob,
        ServerPlayer owner
    ) {
        for (
            double[] offset :
            TELEPORT_OFFSETS
        ) {
            double destinationX =
                owner.getX()
                    + offset[0];

            double destinationY =
                owner.getY();

            double destinationZ =
                owner.getZ()
                    + offset[1];

            double movementX =
                destinationX
                    - mob.getX();

            double movementY =
                destinationY
                    - mob.getY();

            double movementZ =
                destinationZ
                    - mob.getZ();

            AABB destinationBox =
                mob.getBoundingBox()
                    .move(
                        movementX,
                        movementY,
                        movementZ
                    );

            if (
                !level.noCollision(
                    mob,
                    destinationBox
                )
            ) {
                continue;
            }

            level.sendParticles(
                ParticleTypes.PORTAL,
                mob.getX(),
                mob.getY()
                    + mob.getBbHeight()
                        * 0.50D,
                mob.getZ(),
                20,
                0.40D,
                0.60D,
                0.40D,
                0.15D
            );

            mob.snapTo(
                destinationX,
                destinationY,
                destinationZ
            );

            mob.setDeltaMovement(
                Vec3.ZERO
            );

            mob.fallDistance =
                0.0F;

            mob.hurtMarked =
                true;

            clearNativeTarget(
                mob
            );

            mob.getNavigation()
                .stop();

            level.sendParticles(
                ParticleTypes.PORTAL,
                destinationX,
                destinationY
                    + mob.getBbHeight()
                        * 0.50D,
                destinationZ,
                30,
                0.45D,
                0.65D,
                0.45D,
                0.18D
            );

            return true;
        }

        return false;
    }

    /**
     * Remove o alvo definido pela
     * inteligência original do mob.
     */
    private static void clearNativeTarget(
        Mob mob
    ) {
        if (
            mob.getTarget() != null
        ) {
            mob.setTarget(
                null
            );
        }
    }

    /**
     * Controla os danos causados
     * pelos companheiros.
     */
    private static boolean allowCompanionDamage(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float amount
    ) {
        Entity responsibleEntity =
            damageSource.getEntity();

        /*
         * A fonte não é um companheiro.
         */
        if (
            !(responsibleEntity
                instanceof Mob companion)
        ) {
            return true;
        }

        UUID ownerUuid =
            SteviumCompanionData
                .getOwnerUuid(
                    companion
                );

        /*
         * O mob não foi domesticado
         * pelo sistema de Stevium.
         */
        if (ownerUuid == null) {
            return true;
        }

        /*
         * Nunca permite ferir o dono.
         */
        if (
            ownerUuid.equals(
                damagedEntity.getUUID()
            )
        ) {
            return false;
        }

        /*
         * Nunca permite ferir outro
         * companheiro do mesmo dono.
         */
        if (
            damagedEntity
                instanceof Mob otherCompanion
                && SteviumCompanionData
                    .isOwnedBy(
                        otherCompanion,
                        ownerUuid
                    )
        ) {
            return false;
        }

        CompanionRuntimeState state =
            RUNTIME_STATES.get(
                companion.getUUID()
            );

        /*
         * Bloqueia ataques aleatórios
         * produzidos pela inteligência
         * original da entidade.
         */
        if (
            state == null
                || state.commandedTarget == null
        ) {
            return false;
        }

        return state.commandedTarget
            .getUUID()
            .equals(
                damagedEntity.getUUID()
            );
    }

    /**
     * Informações temporárias usadas
     * durante o combate.
     */
    private static final class
        CompanionRuntimeState {

        private int
            lastOwnerHurtTimestamp;

        private int
            lastOwnerAttackTimestamp;

        private int
            lastCompanionHurtTimestamp;

        private int
            nextAttackTick;

        private LivingEntity
            commandedTarget;

        private CompanionRuntimeState(
            int lastOwnerHurtTimestamp,
            int lastOwnerAttackTimestamp,
            int lastCompanionHurtTimestamp
        ) {
            this.lastOwnerHurtTimestamp =
                lastOwnerHurtTimestamp;

            this.lastOwnerAttackTimestamp =
                lastOwnerAttackTimestamp;

            this.lastCompanionHurtTimestamp =
                lastCompanionHurtTimestamp;

            this.nextAttackTick =
                0;

            this.commandedTarget =
                null;
        }

        private static CompanionRuntimeState
            create(
                Mob mob,
                ServerPlayer owner
            ) {
            return new CompanionRuntimeState(
                owner.getLastHurtByMobTimestamp(),
                owner.getLastHurtMobTimestamp(),
                mob.getLastHurtByMobTimestamp()
            );
        }
    }
}