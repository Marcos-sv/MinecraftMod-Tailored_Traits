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

public final class ReactiveFullSetPowerInitializer
    implements ModInitializer {

    /*
     * Conjunto completo configurado como Ferro:
     *
     * 0,50 significa que 50% do dano recebido
     * será devolvido ao responsável pelo ataque.
     *
     * Exemplo:
     *
     * jogador recebe 10 de dano;
     * atacante recebe 5 de dano refletido.
     */
    private static final float
        IRON_REFLECTION_MULTIPLIER =
            0.50F;

    /*
     * Mesmo ataques muito fracos refletem
     * pelo menos uma unidade de dano.
     *
     * Uma unidade equivale a meio coração.
     */
    private static final float
        IRON_MINIMUM_REFLECTED_DAMAGE =
            1.0F;

    /*
     * Conjunto completo configurado
     * como Netherite:
     *
     * o atacante permanece em chamas
     * durante cinco segundos.
     */
    private static final float
        NETHERITE_FIRE_DURATION_SECONDS =
            5.0F;

    /*
     * Guarda temporariamente as entidades que
     * estão recebendo dano refletido.
     *
     * Isso impede situações como:
     *
     * jogador A possui conjunto de Ferro;
     * jogador B também possui conjunto de Ferro;
     * A reflete para B;
     * B reflete novamente para A;
     * o processo se repete infinitamente.
     */
    private static final Set<UUID>
        REFLECTION_DAMAGE_IN_PROGRESS =
            new HashSet<>();

    /*
     * O construtor precisa ser público porque
     * esta classe será criada pelo Fabric
     * através do entrypoint.
     */
    public ReactiveFullSetPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        /*
         * Executado depois que uma entidade
         * recebe um dano aceito pelo jogo.
         */
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
            (
                damagedEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            ) -> handleDamageReaction(
                damagedEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            )
        );

        TailoredTraits.LOGGER.info(
            "Poderes reativos de conjunto registrados."
        );
    }

    /**
     * Processa as habilidades de conjunto
     * relacionadas ao recebimento de dano.
     */
    private static void handleDamageReaction(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float baseDamageTaken,
        float damageTaken,
        boolean blocked
    ) {
        /*
         * Os poderes de conjunto pertencem
         * ao jogador.
         */
        if (
            !(damagedEntity
                instanceof ServerPlayer player)
        ) {
            return;
        }

        /*
         * Um ataque completamente bloqueado
         * pelo escudo não ativa as habilidades.
         */
        if (blocked) {
            return;
        }

        /*
         * Ignora chamadas sem dano válido.
         */
        if (
            baseDamageTaken <= 0.0F
                || damageTaken <= 0.0F
        ) {
            return;
        }

        /*
         * Não processa reações criadas por
         * um dano refletido pelo próprio mod.
         */
        if (
            REFLECTION_DAMAGE_IN_PROGRESS.contains(
                player.getUUID()
            )
        ) {
            return;
        }

        /*
         * Obtém a entidade responsável
         * pelo ataque.
         *
         * Em um ataque com flecha, por exemplo,
         * normalmente será o arqueiro que
         * disparou o projétil.
         */
        LivingEntity attacker =
            findResponsibleAttacker(
                player,
                damageSource
            );

        /*
         * Queda, fome, fogo natural e outros
         * danos ambientais não possuem
         * um atacante vivo para reagir.
         */
        if (attacker == null) {
            return;
        }

        applyIronFullSetPower(
            player,
            attacker,
            damageTaken
        );

        applyNetheriteFullSetPower(
            player,
            attacker
        );
    }

    /**
     * Conjunto completo de Ferro:
     *
     * reflete metade do dano recebido.
     */
    private static void applyIronFullSetPower(
        ServerPlayer player,
        LivingEntity attacker,
        float damageTaken
    ) {
        boolean hasIronFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    player,
                    PowerMaterial.IRON
                );

        if (!hasIronFullSet) {
            return;
        }

        /*
         * Calcula metade do dano recebido.
         */
        float reflectedDamage =
            damageTaken
                * IRON_REFLECTION_MULTIPLIER;

        /*
         * Garante que a reflexão seja
         * perceptível em ataques fracos.
         */
        reflectedDamage =
            Math.max(
                IRON_MINIMUM_REFLECTED_DAMAGE,
                reflectedDamage
            );

        if (
            !(attacker.level()
                instanceof ServerLevel serverLevel)
        ) {
            return;
        }

        UUID attackerId =
            attacker.getUUID();

        /*
         * Marca o atacante antes de aplicar
         * o dano refletido.
         */
        REFLECTION_DAMAGE_IN_PROGRESS.add(
            attackerId
        );

        try {
            /*
             * Utiliza a fonte de dano de espinhos.
             *
             * O jogador que possui o conjunto
             * aparece como responsável pelo dano.
             */
            attacker.hurtServer(
                serverLevel,
                player.damageSources().thorns(
                    player
                ),
                reflectedDamage
            );
        } finally {
            /*
             * A marca sempre é removida,
             * mesmo que alguma outra lógica
             * interrompa a aplicação do dano.
             */
            REFLECTION_DAMAGE_IN_PROGRESS.remove(
                attackerId
            );
        }
    }

    /**
     * Conjunto completo de Netherite:
     *
     * incendeia o responsável pelo ataque
     * durante cinco segundos.
     */
    private static void applyNetheriteFullSetPower(
        ServerPlayer player,
        LivingEntity attacker
    ) {
        boolean hasNetheriteFullSet =
            FullSetPowerUtil
                .hasActiveFullSetMaterial(
                    player,
                    PowerMaterial.NETHERITE
                );

        if (!hasNetheriteFullSet) {
            return;
        }

        /*
         * Caso o atacante já esteja queimando,
         * o Minecraft atualiza a duração
         * conforme o tempo aplicado.
         */
        attacker.igniteForSeconds(
            NETHERITE_FIRE_DURATION_SECONDS
        );
    }

    /**
     * Retorna a entidade viva responsável
     * pelo dano.
     */
    private static LivingEntity
        findResponsibleAttacker(
            ServerPlayer damagedPlayer,
            DamageSource damageSource
        ) {
        Entity responsibleEntity =
            damageSource.getEntity();

        if (
            !(responsibleEntity
                instanceof LivingEntity attacker)
        ) {
            return null;
        }

        /*
         * Impede que um dano provocado pelo
         * próprio jogador seja tratado
         * como um ataque inimigo.
         */
        if (attacker == damagedPlayer) {
            return null;
        }

        if (!attacker.isAlive()) {
            return null;
        }

        return attacker;
    }
}