package com.marcos.tailoredtraits.power;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class QuartzFullSetPowerInitializer
    implements ModInitializer {

    /*
     * Probabilidade de um ataque corpo a corpo
     * se transformar em crítico.
     *
     * 0,99 equivale a 99%.
     */
    private static final float
        QUARTZ_CRITICAL_CHANCE =
            0.99F;

    /*
     * Dano adicional do ataque crítico.
     *
     * 0,50 equivale a 50% do dano base.
     *
     * Exemplo:
     *
     * ataque normal: 10 de dano;
     * bônus crítico: 5 de dano;
     * total aproximado: 15 de dano.
     */
    private static final float
        QUARTZ_CRITICAL_BONUS =
            0.50F;

    /*
     * Guarda temporariamente as entidades
     * que estão recebendo o dano adicional.
     *
     * Isso impede que o dano crítico crie
     * outro crítico infinitamente.
     */
    private static final Set<UUID>
        CRITICAL_DAMAGE_IN_PROGRESS =
            new HashSet<>();

    /*
     * O construtor precisa ser público porque
     * esta classe é criada pelo Fabric.
     */
    public QuartzFullSetPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        ServerLivingEntityEvents.AFTER_DAMAGE.register(
            (
                damagedEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            ) -> handleQuartzCriticalAttack(
                damagedEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            )
        );

        TailoredTraits.LOGGER.info(
            "Poder de conjunto do Quartzo registrado."
        );
    }

    /**
     * Verifica se o dano recebido deve
     * ganhar o bônus crítico do Quartzo.
     */
    private static void handleQuartzCriticalAttack(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float baseDamageTaken,
        float damageTaken,
        boolean blocked
    ) {
        /*
         * Ataques completamente bloqueados
         * não recebem bônus crítico.
         */
        if (blocked) {
            return;
        }

        /*
         * Ignora eventos sem dano válido.
         */
        if (
            baseDamageTaken <= 0.0F
                || damageTaken <= 0.0F
                || !damagedEntity.isAlive()
        ) {
            return;
        }

        UUID damagedEntityId =
            damagedEntity.getUUID();

        /*
         * Impede que o dano adicional
         * seja processado novamente.
         */
        if (
            CRITICAL_DAMAGE_IN_PROGRESS.contains(
                damagedEntityId
            )
        ) {
            return;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

        /*
         * O responsável pelo dano precisa
         * ser um jogador no servidor.
         */
        if (
            !(responsibleEntity
                instanceof ServerPlayer attacker)
        ) {
            return;
        }

        Entity directEntity =
            damageSource.getDirectEntity();

        /*
         * O responsável e a entidade direta
         * precisam ser o próprio jogador.
         *
         * Isso limita a habilidade a ataques
         * corpo a corpo.
         *
         * Flechas, tridentes lançados e outros
         * projéteis não recebem o bônus.
         */
        if (directEntity != attacker) {
            return;
        }

        /*
         * Verifica se o atacante possui
         * o conjunto completo configurado
         * como Quartzo.
         */
        boolean hasQuartzFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    attacker,
                    PowerMaterial.QUARTZ
                );

        if (!hasQuartzFullSet) {
            return;
        }

        /*
         * Existe 1% de chance de o ataque
         * continuar sendo um ataque normal.
         */
        if (
            attacker.getRandom().nextFloat()
                >= QUARTZ_CRITICAL_CHANCE
        ) {
            return;
        }

        if (
            !(damagedEntity.level()
                instanceof ServerLevel serverLevel)
        ) {
            return;
        }

        /*
         * Calcula 50% do dano base original.
         *
         * A armadura e as proteções do alvo
         * ainda poderão reduzir esse bônus
         * quando ele for aplicado.
         */
        float additionalDamage =
            baseDamageTaken
                * QUARTZ_CRITICAL_BONUS;

        if (additionalDamage <= 0.0F) {
            return;
        }

        /*
         * Guarda o tempo de invulnerabilidade
         * criado pelo primeiro dano.
         *
         * Sem essa alteração, o Minecraft
         * poderia ignorar o dano adicional
         * por ele ocorrer imediatamente depois
         * do ataque original.
         */
        int previousInvulnerableTime =
            damagedEntity.invulnerableTime;

        CRITICAL_DAMAGE_IN_PROGRESS.add(
            damagedEntityId
        );

        try {
            /*
             * Libera temporariamente a entidade
             * para receber o bônus crítico.
             */
            damagedEntity.invulnerableTime =
                0;

            damagedEntity.hurtServer(
                serverLevel,
                attacker
                    .damageSources()
                    .playerAttack(
                        attacker
                    ),
                additionalDamage
            );
        } finally {
            /*
             * Mantém o maior tempo de
             * invulnerabilidade gerado pelos
             * dois danos.
             */
            damagedEntity.invulnerableTime =
                Math.max(
                    damagedEntity.invulnerableTime,
                    previousInvulnerableTime
                );

            CRITICAL_DAMAGE_IN_PROGRESS.remove(
                damagedEntityId
            );
        }
    }
}