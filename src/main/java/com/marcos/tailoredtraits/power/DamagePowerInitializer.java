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
     * reduz em 10% o dano de projéteis.
     */
    private static final float
        IRON_HELMET_PROJECTILE_REDUCTION =
            0.10F;

    /*
     * Peitoral configurado como Ferro:
     * reduz em 10% danos causados por entidades.
     */
    private static final float
        IRON_CHESTPLATE_ENTITY_REDUCTION =
            0.10F;

    /*
     * Capacete configurado como Netherite:
     * reduz em 10% danos classificados como fogo.
     *
     * Isso inclui, por exemplo:
     *
     * - fogo;
     * - estar queimando;
     * - lava;
     * - ataques incendiários.
     */
    private static final float
        NETHERITE_HELMET_FIRE_DAMAGE_REDUCTION =
            0.10F;

    /*
     * Peitoral configurado como Diamante:
     * reduz em 25% o dano de criaturas do End.
     */
    private static final float
        DIAMOND_CHESTPLATE_END_DAMAGE_REDUCTION =
            0.25F;

    /*
     * Calça configurada como Diamante:
     * aumenta em 25% o dano contra criaturas do End.
     */
    private static final float
        DIAMOND_LEGGINGS_END_DAMAGE_BONUS =
            0.25F;

    /*
     * Peitoral configurado como Ouro:
     * reduz em 25% o dano de criaturas do Nether.
     */
    private static final float
        GOLD_CHESTPLATE_NETHER_DAMAGE_REDUCTION =
            0.25F;

    /*
     * Calça configurada como Ouro:
     * aumenta em 25% o dano contra criaturas do Nether.
     */
    private static final float
        GOLD_LEGGINGS_NETHER_DAMAGE_BONUS =
            0.25F;

    /*
     * Peitoral configurado como Cobre:
     *
     * 60 ticks = 3 segundos de paralisia.
     */
    private static final int
        COPPER_PARALYSIS_DURATION =
            60;

    /*
     * Lentidão nível XI.
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
     * Peitoral configurado como Resina:
     *
     * 80 ticks = 4 segundos de lentidão.
     */
    private static final int
        RESIN_SLOWNESS_DURATION =
            80;

    /*
     * Amplificador 1 representa Lentidão II.
     *
     * É uma lentidão perceptível, mas muito menos
     * forte que a paralisia do peitoral de Cobre.
     */
    private static final int
        RESIN_SLOWNESS_AMPLIFIER =
            1;

    /*
     * Peitoral configurado como Redstone:
     * raio de ativação das portas e alçapões.
     */
    private static final int
        REDSTONE_ACTIVATION_RADIUS =
            4;

    /*
     * 20 ticks = 1 segundo.
     */
    private static final long
        REDSTONE_ACTIVATION_COOLDOWN =
            20L;

    /*
     * Guarda entidades cujo dano está sendo
     * reaplicado pelo Tailored Traits.
     *
     * Impede repetição infinita ao substituir
     * o dano original por um dano modificado.
     */
    private static final Set<UUID>
        REAPPLYING_MODIFIED_DAMAGE =
            new HashSet<>();

    /*
     * Guarda a última ativação do peitoral
     * configurado como Redstone.
     */
    private static final Map<UUID, Long>
        LAST_REDSTONE_ACTIVATION =
            new HashMap<>();

    @Override
    public void onInitialize() {

        /*
         * Executado antes da aplicação das
         * proteções vanilla.
         *
         * Utilizado para alterar o valor do dano.
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
         * Utilizado para poderes que reagem
         * ao jogador ser atingido.
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
     * Processa modificações no valor do dano.
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
         * Esta é a segunda chamada, criada
         * pelo próprio Tailored Traits.
         *
         * O dano já foi modificado e deve
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
         * Nenhuma habilidade modificou o dano.
         */
        if (
            Float.compare(
                modifiedDamage,
                originalDamage
            ) == 0
        ) {
            return true;
        }

        modifiedDamage =
            Math.max(
                0.0F,
                modifiedDamage
            );

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
             * Aplica o ataque novamente usando
             * o valor modificado.
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
         * Cancela o dano original, pois o valor
         * modificado já foi aplicado.
         */
        return false;
    }

    /**
     * Calcula todos os bônus e reduções
     * aplicáveis ao dano atual.
     */
    private static float calculateModifiedDamage(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float originalDamage
    ) {
        float modifiedDamage =
            originalDamage;

        /*
         * Primeiro são aplicados os bônus
         * ofensivos do atacante.
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
         * Depois são aplicadas as reduções
         * defensivas do jogador atingido.
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
     * aumenta o dano contra criaturas do End.
     */
    private static float applyDiamondLeggingsDamageBonus(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float currentDamage
    ) {
        if (!isEndMob(damagedEntity)) {
            return currentDamage;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

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

        boolean hasDiamondPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.DIAMOND
            );

        if (!hasDiamondPower) {
            return currentDamage;
        }

        return currentDamage
            * (
                1.0F
                    + DIAMOND_LEGGINGS_END_DAMAGE_BONUS
            );
    }

    /**
     * Calça configurada como Ouro:
     * aumenta o dano contra criaturas do Nether.
     */
    private static float applyGoldLeggingsDamageBonus(
        LivingEntity damagedEntity,
        DamageSource damageSource,
        float currentDamage
    ) {
        if (!isNetherMob(damagedEntity)) {
            return currentDamage;
        }

        Entity responsibleEntity =
            damageSource.getEntity();

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

        boolean hasGoldPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.GOLD
            );

        if (!hasGoldPower) {
            return currentDamage;
        }

        return currentDamage
            * (
                1.0F
                    + GOLD_LEGGINGS_NETHER_DAMAGE_BONUS
            );
    }

    /**
     * Aplica as reduções defensivas das peças.
     */
    private static float applyPlayerDamageReductions(
        ServerPlayer player,
        DamageSource damageSource,
        float currentDamage
    ) {
        float totalReduction =
            0.0F;

        /*
         * Verifica o capacete atual.
         */
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        /*
         * Capacete configurado como Ferro:
         * redução contra projéteis.
         */
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
         * Capacete configurado como Netherite:
         * redução contra danos de fogo.
         */
        boolean hasNetheriteHelmetPower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.NETHERITE
            );

        boolean isFireDamage =
            damageSource.is(
                DamageTypeTags.IS_FIRE
            );

        if (
            hasNetheriteHelmetPower
                && isFireDamage
        ) {
            totalReduction +=
                NETHERITE_HELMET_FIRE_DAMAGE_REDUCTION;
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
         * redução contra ataques de entidades.
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

        if (
            hasDiamondChestplatePower
                && isEndDamageSource(
                    damageSource
                )
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

        if (
            hasGoldChestplatePower
                && isNetherDamageSource(
                    damageSource
                )
        ) {
            totalReduction +=
                GOLD_CHESTPLATE_NETHER_DAMAGE_REDUCTION;
        }

        if (totalReduction <= 0.0F) {
            return currentDamage;
        }

        /*
         * Evita que futuras combinações
         * ultrapassem 100% de redução.
         */
        totalReduction =
            Math.min(
                totalReduction,
                1.0F
            );

        return currentDamage
            * (
                1.0F
                    - totalReduction
            );
    }

    /**
     * Verifica se o dano veio de uma
     * criatura ou ataque do End.
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
     * Verifica se a entidade é uma
     * criatura nativa do End.
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
     * Verifica se o dano veio de uma
     * criatura nativa do Nether.
     */
    private static boolean isNetherDamageSource(
        DamageSource damageSource
    ) {
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
     * Verifica se a entidade é uma
     * criatura nativa do Nether.
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
     * Processa habilidades ativadas depois
     * que o jogador recebe dano.
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
         * Ataques bloqueados por escudo
         * não ativam as habilidades.
         */
        if (blocked) {
            return;
        }

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

        applyResinChestplatePower(
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
     * paralisa completamente o atacante
     * corpo a corpo por três segundos.
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

        LivingEntity attacker =
            getDirectMeleeAttacker(
                player,
                damageSource
            );

        if (attacker == null) {
            return;
        }

        attacker.setDeltaMovement(
            0.0D,
            0.0D,
            0.0D
        );

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
     * Peitoral configurado como Resina:
     *
     * ataques corpo a corpo deixam
     * o atacante mais lento.
     */
    private static void applyResinChestplatePower(
        ServerPlayer player,
        DamageSource damageSource
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasResinPower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.RESIN
            );

        if (!hasResinPower) {
            return;
        }

        LivingEntity attacker =
            getDirectMeleeAttacker(
                player,
                damageSource
            );

        if (attacker == null) {
            return;
        }

        attacker.addEffect(
            new MobEffectInstance(
                MobEffects.SLOWNESS,
                RESIN_SLOWNESS_DURATION,
                RESIN_SLOWNESS_AMPLIFIER,
                true,
                true,
                true
            )
        );
    }

    /**
     * Retorna o atacante somente quando o dano
     * for um ataque corpo a corpo direto.
     *
     * Flechas, tridentes e explosões
     * não são considerados.
     */
    private static LivingEntity getDirectMeleeAttacker(
        ServerPlayer player,
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

        if (attacker == player) {
            return null;
        }

        boolean isProjectile =
            damageSource.is(
                DamageTypeTags.IS_PROJECTILE
            );

        boolean isExplosion =
            damageSource.is(
                DamageTypeTags.IS_EXPLOSION
            );

        boolean isMeleeAttack =
            damageSource.isDirect()
                && !isProjectile
                && !isExplosion;

        if (!isMeleeAttack) {
            return null;
        }

        return attacker;
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
             * Apenas a metade inferior
             * deve ser processada.
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
             * Alçapões ocupam apenas um bloco.
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
}