package com.marcos.tailoredtraits.power;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class DamagePowerInitializer
    implements ModInitializer {

    /*
     * Capacete configurado como Ferro:
     *
     * reduz em 10% o dano causado por projéteis.
     */
    private static final float
        IRON_HELMET_PROJECTILE_REDUCTION =
            0.10F;

    /*
     * Peitoral configurado como Ferro:
     *
     * reduz em 10% o dano cuja origem
     * seja uma entidade.
     */
    private static final float
        IRON_CHESTPLATE_ENTITY_REDUCTION =
            0.10F;

    /*
     * Peitoral configurado como Diamante:
     *
     * reduz em 25% o dano causado por
     * criaturas nativas do End.
     */
    private static final float
        DIAMOND_CHESTPLATE_END_DAMAGE_REDUCTION =
            0.25F;

    /*
     * Calça configurada como Diamante:
     *
     * aumenta em 25% o dano causado pelo
     * jogador contra criaturas do End.
     */
    private static final float
        DIAMOND_LEGGINGS_END_DAMAGE_BONUS =
            0.25F;

    /*
     * Peitoral configurado como Ouro:
     *
     * reduz em 25% o dano causado por
     * criaturas nativas do Nether.
     */
    private static final float
        GOLD_CHESTPLATE_NETHER_DAMAGE_REDUCTION =
            0.25F;

    /*
     * Calça configurada como Ouro:
     *
     * aumenta em 25% o dano causado pelo
     * jogador contra criaturas do Nether.
     */
    private static final float
        GOLD_LEGGINGS_NETHER_DAMAGE_BONUS =
            0.25F;

    /*
     * Peitoral configurado como Cobre:
     *
     * 60 ticks = 3 segundos.
     */
    private static final int
        COPPER_PARALYSIS_DURATION =
            60;

    /*
     * Lentidão nível XI.
     *
     * O amplificador começa em zero.
     * Portanto, 10 corresponde ao nível 11.
     */
    private static final int
        COPPER_SLOWNESS_AMPLIFIER =
            10;

    /*
     * Fraqueza nível VI.
     */
    private static final int
        COPPER_WEAKNESS_AMPLIFIER =
            5;

    /*
     * Peitoral configurado como Redstone:
     *
     * portas e alçapões dentro desse
     * raio serão ativados.
     */
    private static final int
        REDSTONE_ACTIVATION_RADIUS =
            4;

    /*
     * 20 ticks = 1 segundo.
     *
     * Impede ativações seguidas causadas por
     * fogo, veneno ou outros danos contínuos.
     */
    private static final long
        REDSTONE_ACTIVATION_COOLDOWN =
            20L;

    /*
     * Guarda as entidades cujo dano está sendo
     * reaplicado pelo próprio Tailored Traits.
     *
     * Isso impede repetição infinita:
     *
     * evento recebe dano
     * -> modifica dano
     * -> aplica novo dano
     * -> evento recebe novamente
     */
    private static final Set<UUID>
        REAPPLYING_MODIFIED_DAMAGE =
            new HashSet<>();

    /*
     * Guarda o último momento em que o poder
     * do peitoral de Redstone foi ativado.
     */
    private static final Map<UUID, Long>
        LAST_REDSTONE_ACTIVATION =
            new HashMap<>();

    @Override
    public void onInitialize() {

        /*
         * Executado antes da aplicação das
         * proteções da armadura vanilla.
         *
         * Aqui são processados:
         *
         * - Capacete de Ferro;
         * - Peitoral de Ferro;
         * - Peitoral de Diamante;
         * - Calça de Diamante;
         * - Peitoral de Ouro;
         * - Calça de Ouro.
         */
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
            (
                livingEntity,
                damageSource,
                originalDamage
            ) -> handleDamageBeforeApplication(
                livingEntity,
                damageSource,
                originalDamage
            )
        );

        /*
         * Executado depois que o dano
         * foi realmente aceito.
         *
         * Aqui são processados:
         *
         * - Peitoral de Cobre;
         * - Peitoral de Redstone.
         */
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
            (
                livingEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            ) -> handleDamageAfterApplication(
                livingEntity,
                damageSource,
                baseDamageTaken,
                damageTaken,
                blocked
            )
        );

        TailoredTraits.LOGGER.info(
            "Eventos de dano do Tailored Traits registrados."
        );
    }

    /**
     * Processa alterações no valor do dano
     * antes da aplicação das proteções vanilla.
     */
    private static boolean handleDamageBeforeApplication(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float originalDamage
    ) {
        if (originalDamage <= 0.0F) {
            return true;
        }

        UUID damagedEntityId =
            damagedEntity.getUUID();

        /*
         * Esta é a chamada interna criada
         * pelo próprio Tailored Traits.
         *
         * O novo dano já foi calculado e deve
         * continuar normalmente.
         */
        if (
            REAPPLYING_MODIFIED_DAMAGE.contains(
                damagedEntityId
            )
        ) {
            return true;
        }

        float modifiedDamage =
            calculateModifiedDamage(
                damagedEntity,
                damageSource,
                originalDamage
            );

        /*
         * Nenhuma habilidade alterou o dano.
         *
         * O Minecraft continua normalmente
         * usando o valor original.
         */
        if (
            Float.compare(
                modifiedDamage,
                originalDamage
            ) == 0
        ) {
            return true;
        }

        /*
         * Garante que nunca seja aplicado
         * um valor negativo.
         */
        modifiedDamage =
            Math.max(
                0.0F,
                modifiedDamage
            );

        /*
         * Confirma que a entidade está em
         * um mundo do lado do servidor.
         */
        if (
            !(damagedEntity.level()
                instanceof ServerLevel serverLevel)
        ) {
            return true;
        }

        REAPPLYING_MODIFIED_DAMAGE.add(
            damagedEntityId
        );

        try {
            /*
             * Reaplica o mesmo ataque usando
             * o valor já modificado.
             */
            damagedEntity.hurtServer(
                serverLevel,
                damageSource,
                modifiedDamage
            );
        } finally {
            REAPPLYING_MODIFIED_DAMAGE.remove(
                damagedEntityId
            );
        }

        /*
         * Cancela o dano original, pois o dano
         * modificado já foi aplicado.
         */
        return false;
    }

    /**
     * Aplica todos os aumentos e reduções
     * relacionados ao evento de dano.
     */
    private static float calculateModifiedDamage(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float originalDamage
    ) {
        float modifiedDamage =
            originalDamage;

        /*
         * Primeiro são aplicados os bônus ofensivos
         * do jogador que causou o ataque.
         */
        modifiedDamage =
            applyDiamondLeggingsDamageBonus(
                damagedEntity,
                damageSource,
                modifiedDamage
            );

        modifiedDamage =
            applyGoldLeggingsDamageBonus(
                damagedEntity,
                damageSource,
                modifiedDamage
            );

        /*
         * As reduções defensivas atuais
         * são exclusivas para jogadores.
         */
        if (
            damagedEntity
                instanceof ServerPlayer player
        ) {
            modifiedDamage =
                applyPlayerDamageReductions(
                    player,
                    damageSource,
                    modifiedDamage
                );
        }

        return modifiedDamage;
    }

    /**
     * Calça configurada como Diamante:
     *
     * aumenta em 25% o dano causado pelo
     * jogador contra criaturas do End.
     */
    private static float applyDiamondLeggingsDamageBonus(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float currentDamage
    ) {
        /*
         * O alvo precisa pertencer ao grupo
         * de criaturas nativas do End.
         */
        if (!isEndMob(damagedEntity)) {
            return currentDamage;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

        /*
         * O responsável pelo ataque precisa
         * ser um jogador do servidor.
         *
         * Isso também permite que flechas e
         * tridentes lançados pelo jogador
         * recebam o bônus.
         */
        if (
            !(responsibleEntity
                instanceof ServerPlayer attacker)
        ) {
            return currentDamage;
        }

        ItemStack leggings =
            attacker.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasDiamondLeggingsPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.DIAMOND
            );

        if (!hasDiamondLeggingsPower) {
            return currentDamage;
        }

        float bonusMultiplier =
            1.0F
                + DIAMOND_LEGGINGS_END_DAMAGE_BONUS;

        return currentDamage
            * bonusMultiplier;
    }

    /**
     * Calça configurada como Ouro:
     *
     * aumenta em 25% o dano causado pelo
     * jogador contra criaturas do Nether.
     */
    private static float applyGoldLeggingsDamageBonus(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float currentDamage
    ) {
        /*
         * O alvo precisa ser uma criatura
         * nativa do Nether.
         */
        if (!isNetherMob(damagedEntity)) {
            return currentDamage;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

        /*
         * O responsável precisa ser um jogador.
         *
         * Flechas e tridentes disparados pelo
         * jogador também recebem o bônus.
         */
        if (
            !(responsibleEntity
                instanceof ServerPlayer attacker)
        ) {
            return currentDamage;
        }

        ItemStack leggings =
            attacker.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasGoldLeggingsPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.GOLD
            );

        if (!hasGoldLeggingsPower) {
            return currentDamage;
        }

        float bonusMultiplier =
            1.0F
                + GOLD_LEGGINGS_NETHER_DAMAGE_BONUS;

        return currentDamage
            * bonusMultiplier;
    }

    /**
     * Aplica as reduções defensivas do jogador.
     */
    private static float applyPlayerDamageReductions(
        ServerPlayer player,
        DamageSource damageSource,
        float currentDamage
    ) {
        float totalReduction =
            0.0F;

        /*
         * Capacete configurado como Ferro:
         * redução contra projéteis.
         */
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasIronHelmetPower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.IRON
            );

        boolean isProjectileDamage =
            damageSource.is(
                DamageTypeTags.IS_PROJECTILE
            );

        if (
            hasIronHelmetPower
                && isProjectileDamage
        ) {
            totalReduction +=
                IRON_HELMET_PROJECTILE_REDUCTION;
        }

        /*
         * Verifica o peitoral atual.
         */
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        /*
         * Peitoral configurado como Ferro:
         * redução contra ataques causados
         * por entidades.
         */
        boolean hasIronChestplatePower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.IRON
            );

        Entity attackingEntity =
            damageSource.getEntity();

        boolean wasCausedByEntity =
            attackingEntity != null;

        if (
            hasIronChestplatePower
                && wasCausedByEntity
        ) {
            totalReduction +=
                IRON_CHESTPLATE_ENTITY_REDUCTION;
        }

        /*
         * Peitoral configurado como Diamante:
         * redução contra criaturas do End.
         */
        boolean hasDiamondChestplatePower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.DIAMOND
            );

        boolean wasCausedByEndMob =
            isEndDamageSource(
                damageSource
            );

        if (
            hasDiamondChestplatePower
                && wasCausedByEndMob
        ) {
            totalReduction +=
                DIAMOND_CHESTPLATE_END_DAMAGE_REDUCTION;
        }

        /*
         * Peitoral configurado como Ouro:
         * redução contra criaturas do Nether.
         */
        boolean hasGoldChestplatePower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.GOLD
            );

        boolean wasCausedByNetherMob =
            isNetherDamageSource(
                damageSource
            );

        if (
            hasGoldChestplatePower
                && wasCausedByNetherMob
        ) {
            totalReduction +=
                GOLD_CHESTPLATE_NETHER_DAMAGE_REDUCTION;
        }

        /*
         * Nenhuma redução foi aplicada.
         */
        if (totalReduction <= 0.0F) {
            return currentDamage;
        }

        /*
         * Impede que futuras combinações
         * ultrapassem 100% de redução.
         */
        totalReduction =
            Math.min(
                totalReduction,
                1.0F
            );

        float damageMultiplier =
            1.0F - totalReduction;

        return currentDamage
            * damageMultiplier;
    }

    /**
     * Verifica se a origem do dano pertence
     * a uma criatura nativa do End.
     */
    private static boolean isEndDamageSource(
        DamageSource damageSource
    ) {
        Entity responsibleEntity =
            damageSource.getEntity();

        if (
            responsibleEntity != null
                && isEndMob(
                    responsibleEntity
                )
        ) {
            return true;
        }

        Entity directEntity =
            damageSource.getDirectEntity();

        if (directEntity == null) {
            return false;
        }

        return isEndMob(
                directEntity
            )
            || directEntity.getType()
                == EntityTypes.SHULKER_BULLET
            || directEntity.getType()
                == EntityTypes.DRAGON_FIREBALL;
    }

    /**
     * Verifica se uma entidade pertence
     * ao grupo de criaturas do End.
     */
    private static boolean isEndMob(
        Entity entity
    ) {
        return entity.getType()
                == EntityTypes.ENDERMAN
            || entity.getType()
                == EntityTypes.ENDERMITE
            || entity.getType()
                == EntityTypes.SHULKER
            || entity.getType()
                == EntityTypes.ENDER_DRAGON;
    }

    /**
     * Verifica se a origem do dano pertence
     * a uma criatura nativa do Nether.
     */
    private static boolean isNetherDamageSource(
        DamageSource damageSource
    ) {
        /*
         * Em ataques corpo a corpo ou projéteis,
         * getEntity() normalmente retorna:
         *
         * - Blaze;
         * - Ghast;
         * - Piglin;
         * - Esqueleto Wither;
         * - outras criaturas responsáveis.
         */
        Entity responsibleEntity =
            damageSource.getEntity();

        if (responsibleEntity == null) {
            return false;
        }

        return isNetherMob(
            responsibleEntity
        );
    }

    /**
     * Verifica se uma entidade pertence
     * ao grupo de criaturas nativas do Nether.
     */
    private static boolean isNetherMob(
        Entity entity
    ) {
        return entity.getType()
                == EntityTypes.BLAZE
            || entity.getType()
                == EntityTypes.GHAST
            || entity.getType()
                == EntityTypes.HAPPY_GHAST
            || entity.getType()
                == EntityTypes.HOGLIN
            || entity.getType()
                == EntityTypes.MAGMA_CUBE
            || entity.getType()
                == EntityTypes.SULFUR_CUBE
            || entity.getType()
                == EntityTypes.PIGLIN
            || entity.getType()
                == EntityTypes.PIGLIN_BRUTE
            || entity.getType()
                == EntityTypes.STRIDER
            || entity.getType()
                == EntityTypes.WITHER_SKELETON
            || entity.getType()
                == EntityTypes.ZOGLIN
            || entity.getType()
                == EntityTypes.ZOMBIFIED_PIGLIN;
    }

    /**
     * Processa poderes que devem ocorrer
     * depois que o dano for aceito.
     */
    private static void handleDamageAfterApplication(
        LivingEntity livingEntity,
        DamageSource damageSource,
        float baseDamageTaken,
        float damageTaken,
        boolean blocked
    ) {
        if (
            !(livingEntity
                instanceof ServerPlayer player)
        ) {
            return;
        }

        /*
         * Ataques totalmente bloqueados por
         * escudo não ativam os poderes.
         */
        if (blocked) {
            return;
        }

        /*
         * Nenhum dano real foi processado.
         */
        if (
            baseDamageTaken <= 0.0F
                || damageTaken <= 0.0F
        ) {
            return;
        }

        applyCopperChestplatePower(
            player,
            damageSource
        );

        applyRedstoneChestplatePower(
            player,
            player.level()
        );
    }

    /**
     * Peitoral configurado como Cobre:
     *
     * paralisa a entidade que atacar
     * diretamente o jogador.
     */
    private static void applyCopperChestplatePower(
        ServerPlayer player,
        DamageSource damageSource
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasCopperPower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.COPPER
            );

        if (!hasCopperPower) {
            return;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

        if (
            !(responsibleEntity
                instanceof LivingEntity attacker)
        ) {
            return;
        }

        if (attacker == player) {
            return;
        }

        boolean isProjectile =
            damageSource.is(
                DamageTypeTags.IS_PROJECTILE
            );

        boolean isExplosion =
            damageSource.is(
                DamageTypeTags.IS_EXPLOSION
            );

        /*
         * Flechas, tridentes, explosões e outros
         * ataques indiretos não ativam o poder.
         */
        boolean isMeleeAttack =
            damageSource.isDirect()
                && !isProjectile
                && !isExplosion;

        if (!isMeleeAttack) {
            return;
        }

        /*
         * Interrompe imediatamente o movimento
         * atual do atacante.
         */
        attacker.setDeltaMovement(
            0.0D,
            0.0D,
            0.0D
        );

        /*
         * Lentidão extremamente alta
         * durante três segundos.
         */
        attacker.addEffect(
            new MobEffectInstance(
                MobEffects.SLOWNESS,
                COPPER_PARALYSIS_DURATION,
                COPPER_SLOWNESS_AMPLIFIER,
                true,
                true,
                true
            )
        );

        /*
         * Também reduz fortemente o dano
         * causado pelo atacante paralisado.
         */
        attacker.addEffect(
            new MobEffectInstance(
                MobEffects.WEAKNESS,
                COPPER_PARALYSIS_DURATION,
                COPPER_WEAKNESS_AMPLIFIER,
                true,
                true,
                true
            )
        );
    }

    /**
     * Peitoral configurado como Redstone:
     *
     * alterna portas e alçapões próximos
     * quando o jogador recebe dano.
     */
    private static void applyRedstoneChestplatePower(
        ServerPlayer player,
        ServerLevel level
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasRedstonePower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.REDSTONE
            );

        if (!hasRedstonePower) {
            return;
        }

        UUID playerId =
            player.getUUID();

        long currentGameTime =
            level.getGameTime();

        Long previousActivation =
            LAST_REDSTONE_ACTIVATION.get(
                playerId
            );

        /*
         * Impede nova ativação dentro
         * do intervalo de um segundo.
         */
        if (
            previousActivation != null
                && currentGameTime
                    - previousActivation
                    < REDSTONE_ACTIVATION_COOLDOWN
        ) {
            return;
        }

        LAST_REDSTONE_ACTIVATION.put(
            playerId,
            currentGameTime
        );

        BlockPos center =
            player.blockPosition();

        BlockPos minimumPosition =
            center.offset(
                -REDSTONE_ACTIVATION_RADIUS,
                -REDSTONE_ACTIVATION_RADIUS,
                -REDSTONE_ACTIVATION_RADIUS
            );

        BlockPos maximumPosition =
            center.offset(
                REDSTONE_ACTIVATION_RADIUS,
                REDSTONE_ACTIVATION_RADIUS,
                REDSTONE_ACTIVATION_RADIUS
            );

        /*
         * Percorre todos os blocos
         * dentro do raio definido.
         */
        for (
            BlockPos position :
            BlockPos.betweenClosed(
                minimumPosition,
                maximumPosition
            )
        ) {
            BlockState blockState =
                level.getBlockState(
                    position
                );

            Block block =
                blockState.getBlock();

            /*
             * Portas ocupam dois blocos.
             *
             * Apenas a metade inferior é processada
             * para evitar duas ativações.
             */
            if (
                block
                    instanceof DoorBlock doorBlock
            ) {
                DoubleBlockHalf half =
                    blockState.getValue(
                        DoorBlock.HALF
                    );

                if (
                    half
                        != DoubleBlockHalf.LOWER
                ) {
                    continue;
                }

                boolean currentlyOpen =
                    doorBlock.isOpen(
                        blockState
                    );

                doorBlock.setOpen(
                    player,
                    level,
                    blockState,
                    position,
                    !currentlyOpen
                );

                continue;
            }

            /*
             * Alterna o estado dos alçapões.
             */
            if (
                block
                    instanceof TrapDoorBlock
            ) {
                boolean currentlyOpen =
                    blockState.getValue(
                        TrapDoorBlock.OPEN
                    );

                BlockState newState =
                    blockState.setValue(
                        TrapDoorBlock.OPEN,
                        !currentlyOpen
                    );

                level.setBlock(
                    position,
                    newState,
                    3
                );
            }
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
}