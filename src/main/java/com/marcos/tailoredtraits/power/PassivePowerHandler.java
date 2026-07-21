package com.marcos.tailoredtraits.power;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class PassivePowerHandler {

    private static final int NIGHT_VISION_DURATION =
        240;

    private static final double
        DIAMOND_ENDERMAN_VISION_RANGE =
            12.0D;

    private static final double
        DIAMOND_ENDERMAN_CHECK_RADIUS =
            32.0D;

    /*
     * Botas de Cobre:
     * +10% de velocidade.
     */
    private static final Identifier
        COPPER_BOOTS_SPEED_ID =
            TailoredTraits.id(
                "copper_boots_speed"
            );

    private static final AttributeModifier
        COPPER_BOOTS_SPEED_MODIFIER =
            new AttributeModifier(
                COPPER_BOOTS_SPEED_ID,
                0.10D,
                AttributeModifier.Operation
                    .ADD_MULTIPLIED_TOTAL
            );

    /*
     * Botas de Ouro:
     * velocidade-base do Piglin.
     */
    private static final Identifier
        GOLD_BOOTS_SPEED_ID =
            TailoredTraits.id(
                "gold_boots_piglin_speed"
            );

    private static final double
        PIGLIN_MOVEMENT_SPEED =
            Piglin.createAttributes()
                .build()
                .getBaseValue(
                    Attributes.MOVEMENT_SPEED
                );

    /*
     * Botas de Diamante:
     * 5 segundos carregando e 3 segundos
     * de Levitação.
     */
    private static final int
        DIAMOND_LEVITATION_CHARGE_DURATION =
            100;

    private static final int
        DIAMOND_LEVITATION_EFFECT_DURATION =
            60;

    /*
     * Botas de Quartzo.
     */
    private static final Identifier
        QUARTZ_BOOTS_JUMP_ID =
            TailoredTraits.id(
                "quartz_boots_jump"
            );

    private static final AttributeModifier
        QUARTZ_BOOTS_JUMP_MODIFIER =
            new AttributeModifier(
                QUARTZ_BOOTS_JUMP_ID,
                0.20D,
                AttributeModifier.Operation.ADD_VALUE
            );

    /*
     * Botas de Redstone:
     *
     * MOVEMENT_EFFICIENCY varia de 0 até 1.
     * O valor 1 remove a penalidade de velocidade
     * aplicada pelo bloco sob o jogador.
     */
    private static final Identifier
        REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_ID =
            TailoredTraits.id(
                "redstone_boots_movement_efficiency"
            );

    private static final AttributeModifier
        REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_MODIFIER =
            new AttributeModifier(
                REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
            );

    /*
     * Calça de Quartzo:
     * +20% de dano.
     */
    private static final Identifier
        QUARTZ_LEGGINGS_DAMAGE_ID =
            TailoredTraits.id(
                "quartz_leggings_damage"
            );

    private static final AttributeModifier
        QUARTZ_LEGGINGS_DAMAGE_MODIFIER =
            new AttributeModifier(
                QUARTZ_LEGGINGS_DAMAGE_ID,
                0.20D,
                AttributeModifier.Operation
                    .ADD_MULTIPLIED_TOTAL
            );

    /*
     * Calça de Ferro:
     * -10% de dano de queda.
     */
    private static final Identifier
        IRON_LEGGINGS_FALL_DAMAGE_ID =
            TailoredTraits.id(
                "iron_leggings_fall_damage"
            );

    private static final AttributeModifier
        IRON_LEGGINGS_FALL_DAMAGE_MODIFIER =
            new AttributeModifier(
                IRON_LEGGINGS_FALL_DAMAGE_ID,
                -0.10D,
                AttributeModifier.Operation
                    .ADD_MULTIPLIED_TOTAL
            );

    /*
     * Calça de Ametista:
     * ecolocalização.
     */
    private static final double
        AMETHYST_ECHOLOCATION_RADIUS =
            16.0D;

    private static final int
        AMETHYST_ECHOLOCATION_GLOW_DURATION =
            30;

    /*
     * Calça de Redstone:
     * recuperação de fome sobre bloco de redstone.
     */
    private static final int
        REDSTONE_HUNGER_INTERVAL =
            40;

    private static final int
        REDSTONE_HUNGER_AMOUNT =
            1;

    private static final float
        REDSTONE_HUNGER_SATURATION =
            0.25F;

    /*
     * Calça de Lápis-lazúli:
     * concede 25% de experiência adicional.
     */
    private static final double
        LAPIS_EXPERIENCE_BONUS_PERCENTAGE =
            0.25D;

    /*
     * Peitoral de Quartzo.
     */
    private static final Identifier
        QUARTZ_CHESTPLATE_REACH_ID =
            TailoredTraits.id(
                "quartz_chestplate_reach"
            );

    private static final AttributeModifier
        QUARTZ_CHESTPLATE_REACH_MODIFIER =
            new AttributeModifier(
                QUARTZ_CHESTPLATE_REACH_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
            );

    /*
     * Peitoral de Ametista.
     */
    private static final Identifier
        AMETHYST_CHESTPLATE_KNOCKBACK_ID =
            TailoredTraits.id(
                "amethyst_chestplate_knockback"
            );

    private static final AttributeModifier
        AMETHYST_CHESTPLATE_KNOCKBACK_MODIFIER =
            new AttributeModifier(
                AMETHYST_CHESTPLATE_KNOCKBACK_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
            );

    /*
     * Peitoral de Esmeralda.
     */
    private static final double
        EMERALD_VILLAGER_ATTRACTION_RADIUS =
            20.0D;

    private static final double
        EMERALD_VILLAGER_STOP_DISTANCE =
            2.5D;

    private static final double
        EMERALD_VILLAGER_MOVEMENT_SPEED =
            0.8D;

    /*
     * Peitoral de Lápis-lazúli:
     * 10 pontos de vida = 5 corações.
     */
    private static final float
        LAPIS_LEVEL_UP_HEAL_AMOUNT =
            10.0F;

    /*
     * Capacete de Esmeralda.
     */
    private static final double
        EMERALD_VILLAGER_MARK_RADIUS =
            24.0D;

    private static final int
        EMERALD_VILLAGER_GLOW_DURATION =
            60;

    /*
     * Capacete de Ouro.
     */
    private static final double
        GOLD_NETHER_MOB_MARK_RADIUS =
            24.0D;

    private static final int
        GOLD_NETHER_MOB_GLOW_DURATION =
            60;

    /*
     * Peitoral de Netherite.
     */
    private static final int
        NETHERITE_FIRE_RESISTANCE_DURATION =
            100;

    private static final Set<UUID>
        AMETHYST_VISION_PLAYERS =
            new HashSet<>();

    private static final Set<UUID>
        NETHERITE_FIRE_POWER_LOCKED =
            new HashSet<>();

    private static final Map<UUID, Integer>
        DIAMOND_LEVITATION_CHARGE_TICKS =
            new HashMap<>();

    private static final Set<UUID>
        DIAMOND_LEVITATION_LOCKED =
            new HashSet<>();

    /*
     * Usado pelo peitoral de Lápis-lazúli.
     */
    private static final Map<UUID, Integer>
        LAST_EXPERIENCE_LEVELS =
            new HashMap<>();

    /*
     * Usado pela calça de Lápis-lazúli.
     *
     * Guarda a quantidade total de XP que o jogador
     * possuía na última verificação.
     */
    private static final Map<UUID, Integer>
        LAST_TOTAL_EXPERIENCE =
            new HashMap<>();

    /*
     * Guarda frações do bônus de XP.
     *
     * Exemplo:
     * ganhar 1 XP gera 0,25 de bônus.
     * Após quatro ganhos de 1 XP, o jogador recebe
     * 1 ponto adicional.
     */
    private static final Map<UUID, Double>
        LAPIS_EXPERIENCE_BONUS_REMAINDERS =
            new HashMap<>();

    private PassivePowerHandler() {
    }

    public static void initialize() {

        ServerTickEvents.END_SERVER_TICK.register(
            server -> {

                for (
                    ServerPlayer player :
                    server.getPlayerList().getPlayers()
                ) {
                    /*
                     * Capacetes.
                     */
                    updateAmethystHelmetPower(player);
                    updateCopperHelmetPower(player);
                    updateDiamondHelmetPower(player);
                    updateEmeraldHelmetPower(player);
                    updateGoldHelmetPower(player);
                    updateRedstoneHelmetPower(player);

                    /*
                     * Peitorais.
                     */
                    updateQuartzChestplatePower(player);
                    updateAmethystChestplatePower(player);
                    updateEmeraldChestplatePower(player);
                    updateNetheriteChestplatePower(player);
                    updateLapisChestplatePower(player);

                    /*
                     * Calças.
                     */
                    updateQuartzLeggingsPower(player);
                    updateIronLeggingsPower(player);
                    updateAmethystLeggingsPower(player);
                    updateRedstoneLeggingsPower(player);
                    updateLapisLeggingsPower(player);

                    /*
                     * Botas.
                     */
                    updateCopperBootsPower(player);
                    updateGoldBootsPower(player);
                    updateDiamondBootsLevitationPower(player);
                    updateQuartzBootsPower(player);
                    updateRedstoneBootsPower(player);
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poderes passivos do Tailored Traits registrados."
        );
    }

    private static void updateAmethystHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean shouldHaveNightVision =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.AMETHYST
            );

        MobEffectInstance currentEffect =
            player.getEffect(
                MobEffects.NIGHT_VISION
            );

        if (shouldHaveNightVision) {

            if (
                currentEffect != null
                    && !isTailoredTraitsNightVision(
                        currentEffect
                    )
            ) {
                AMETHYST_VISION_PLAYERS.remove(
                    player.getUUID()
                );

                return;
            }

            if (
                currentEffect == null
                    || currentEffect.endsWithin(40)
            ) {
                player.addEffect(
                    createNightVisionEffect()
                );
            }

            AMETHYST_VISION_PLAYERS.add(
                player.getUUID()
            );

            return;
        }

        boolean wasAppliedByMod =
            AMETHYST_VISION_PLAYERS.remove(
                player.getUUID()
            );

        if (
            wasAppliedByMod
                && isTailoredTraitsNightVision(
                    currentEffect
                )
        ) {
            player.removeEffect(
                MobEffects.NIGHT_VISION
            );
        }
    }

    private static void updateCopperHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean shouldShowDayPeriod =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.COPPER
            );

        if (!shouldShowDayPeriod) {
            return;
        }

        if (player.tickCount % 40 != 0) {
            return;
        }

        long dayTime =
            player.level()
                .getOverworldClockTime()
                % 24000L;

        Component currentPeriod =
            getDayPeriodName(dayTime);

        player.sendOverlayMessage(
            Component.translatable(
                "message.tailored-traits.current_day_period",
                currentPeriod
            )
        );
    }

    private static Component getDayPeriodName(
        long dayTime
    ) {
        if (
            dayTime >= 23000L
                || dayTime < 1000L
        ) {
            return Component.translatable(
                "day_period.tailored-traits.dawn"
            );
        }

        if (dayTime < 6000L) {
            return Component.translatable(
                "day_period.tailored-traits.morning"
            );
        }

        if (dayTime < 12000L) {
            return Component.translatable(
                "day_period.tailored-traits.afternoon"
            );
        }

        if (dayTime < 13000L) {
            return Component.translatable(
                "day_period.tailored-traits.sunset"
            );
        }

        return Component.translatable(
            "day_period.tailored-traits.night"
        );
    }

    private static void updateDiamondHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasDiamondPower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.DIAMOND
            );

        if (!hasDiamondPower) {
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }

        double maximumDistanceSquared =
            DIAMOND_ENDERMAN_VISION_RANGE
                * DIAMOND_ENDERMAN_VISION_RANGE;

        for (
            EnderMan enderman :
            player.level().getEntitiesOfClass(
                EnderMan.class,
                player.getBoundingBox().inflate(
                    DIAMOND_ENDERMAN_CHECK_RADIUS
                ),
                enderman ->
                    enderman.isAlive()
                        && enderman.getTarget() == player
            )
        ) {
            if (
                enderman.distanceToSqr(player)
                    > maximumDistanceSquared
            ) {
                enderman.setTarget(null);
                enderman.getNavigation().stop();
            }
        }
    }

    private static void updateEmeraldHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean shouldMarkVillagers =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.EMERALD
            );

        if (!shouldMarkVillagers) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        for (
            AbstractVillager villager :
            player.level().getEntitiesOfClass(
                AbstractVillager.class,
                player.getBoundingBox().inflate(
                    EMERALD_VILLAGER_MARK_RADIUS
                ),
                villager -> villager.isAlive()
            )
        ) {
            villager.addEffect(
                new MobEffectInstance(
                    MobEffects.GLOWING,
                    EMERALD_VILLAGER_GLOW_DURATION,
                    0,
                    true,
                    false,
                    false
                )
            );
        }
    }

    private static void updateGoldHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean shouldMarkNetherMobs =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.GOLD
            );

        if (!shouldMarkNetherMobs) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        for (
            LivingEntity entity :
            player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(
                    GOLD_NETHER_MOB_MARK_RADIUS
                ),
                entity ->
                    entity.isAlive()
                        && isNativeNetherMob(entity)
            )
        ) {
            entity.addEffect(
                new MobEffectInstance(
                    MobEffects.GLOWING,
                    GOLD_NETHER_MOB_GLOW_DURATION,
                    0,
                    true,
                    false,
                    false
                )
            );
        }
    }

    private static boolean isNativeNetherMob(
        LivingEntity entity
    ) {
        return entity.getType() == EntityTypes.BLAZE
            || entity.getType() == EntityTypes.GHAST
            || entity.getType() == EntityTypes.HAPPY_GHAST
            || entity.getType() == EntityTypes.HOGLIN
            || entity.getType() == EntityTypes.MAGMA_CUBE
            || entity.getType() == EntityTypes.SULFUR_CUBE
            || entity.getType() == EntityTypes.PIGLIN
            || entity.getType() == EntityTypes.PIGLIN_BRUTE
            || entity.getType() == EntityTypes.STRIDER
            || entity.getType() == EntityTypes.WITHER_SKELETON
            || entity.getType() == EntityTypes.ZOGLIN
            || entity.getType() == EntityTypes.ZOMBIFIED_PIGLIN;
    }

    private static void updateRedstoneHelmetPower(
        ServerPlayer player
    ) {
        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasRedstonePower =
            hasSelectedMaterial(
                helmet,
                PowerMaterial.REDSTONE
            );

        if (!hasRedstonePower) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        ServerPlayer.RespawnConfig respawnConfig =
            player.getRespawnConfig();

        if (respawnConfig == null) {
            player.sendOverlayMessage(
                Component.literal(
                    "Cama: não definida"
                )
            );

            return;
        }

        if (
            !respawnConfig
                .respawnData()
                .dimension()
                .equals(
                    player.level().dimension()
                )
        ) {
            player.sendOverlayMessage(
                Component.literal(
                    "Cama: em outra dimensão"
                )
            );

            return;
        }

        BlockPos bedPosition =
            respawnConfig
                .respawnData()
                .pos();

        double differenceX =
            bedPosition.getX()
                + 0.5D
                - player.getX();

        double differenceZ =
            bedPosition.getZ()
                + 0.5D
                - player.getZ();

        int distance =
            (int) Math.round(
                Math.sqrt(
                    differenceX * differenceX
                        + differenceZ * differenceZ
                )
            );

        String direction =
            getCompassDirection(
                differenceX,
                differenceZ
            );

        player.sendOverlayMessage(
            Component.literal(
                "Cama: "
                    + direction
                    + " | "
                    + distance
                    + " blocos"
            )
        );
    }

    private static String getCompassDirection(
        double differenceX,
        double differenceZ
    ) {
        double angle =
            Math.toDegrees(
                Math.atan2(
                    differenceZ,
                    differenceX
                )
            );

        if (
            angle >= -22.5D
                && angle < 22.5D
        ) {
            return "Leste";
        }

        if (
            angle >= 22.5D
                && angle < 67.5D
        ) {
            return "Sudeste";
        }

        if (
            angle >= 67.5D
                && angle < 112.5D
        ) {
            return "Sul";
        }

        if (
            angle >= 112.5D
                && angle < 157.5D
        ) {
            return "Sudoeste";
        }

        if (
            angle >= 157.5D
                || angle < -157.5D
        ) {
            return "Oeste";
        }

        if (
            angle >= -157.5D
                && angle < -112.5D
        ) {
            return "Noroeste";
        }

        if (
            angle >= -112.5D
                && angle < -67.5D
        ) {
            return "Norte";
        }

        return "Nordeste";
    }

    private static void updateQuartzChestplatePower(
        ServerPlayer player
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean shouldHaveReachBonus =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.QUARTZ
            );

        AttributeInstance entityInteractionRange =
            player.getAttribute(
                Attributes.ENTITY_INTERACTION_RANGE
            );

        if (entityInteractionRange == null) {
            return;
        }

        if (shouldHaveReachBonus) {

            if (
                !entityInteractionRange.hasModifier(
                    QUARTZ_CHESTPLATE_REACH_ID
                )
            ) {
                entityInteractionRange.addTransientModifier(
                    QUARTZ_CHESTPLATE_REACH_MODIFIER
                );
            }

            return;
        }

        if (
            entityInteractionRange.hasModifier(
                QUARTZ_CHESTPLATE_REACH_ID
            )
        ) {
            entityInteractionRange.removeModifier(
                QUARTZ_CHESTPLATE_REACH_ID
            );
        }
    }

    private static void updateAmethystChestplatePower(
        ServerPlayer player
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean shouldHaveKnockbackBonus =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.AMETHYST
            );

        AttributeInstance attackKnockback =
            player.getAttribute(
                Attributes.ATTACK_KNOCKBACK
            );

        if (attackKnockback == null) {
            return;
        }

        if (shouldHaveKnockbackBonus) {

            if (
                !attackKnockback.hasModifier(
                    AMETHYST_CHESTPLATE_KNOCKBACK_ID
                )
            ) {
                attackKnockback.addTransientModifier(
                    AMETHYST_CHESTPLATE_KNOCKBACK_MODIFIER
                );
            }

            return;
        }

        if (
            attackKnockback.hasModifier(
                AMETHYST_CHESTPLATE_KNOCKBACK_ID
            )
        ) {
            attackKnockback.removeModifier(
                AMETHYST_CHESTPLATE_KNOCKBACK_ID
            );
        }
    }

    private static void updateEmeraldChestplatePower(
        ServerPlayer player
    ) {
        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasEmeraldPower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.EMERALD
            );

        if (
            !hasEmeraldPower
                || player.isSpectator()
        ) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        double stopDistanceSquared =
            EMERALD_VILLAGER_STOP_DISTANCE
                * EMERALD_VILLAGER_STOP_DISTANCE;

        for (
            Villager villager :
            player.level().getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(
                    EMERALD_VILLAGER_ATTRACTION_RADIUS
                ),
                villager ->
                    villager.isAlive()
                        && !villager.isTrading()
            )
        ) {
            if (
                villager.distanceToSqr(player)
                    <= stopDistanceSquared
            ) {
                continue;
            }

            villager.getNavigation().moveTo(
                player,
                EMERALD_VILLAGER_MOVEMENT_SPEED
            );
        }
    }

    private static void updateNetheriteChestplatePower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        boolean isExposedToFire =
            player.isOnFire()
                || player.isInLava();

        if (!isExposedToFire) {
            NETHERITE_FIRE_POWER_LOCKED.remove(
                playerId
            );

            return;
        }

        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasNetheritePower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.NETHERITE
            );

        if (!hasNetheritePower) {
            return;
        }

        if (
            NETHERITE_FIRE_POWER_LOCKED.contains(
                playerId
            )
        ) {
            return;
        }

        NETHERITE_FIRE_POWER_LOCKED.add(
            playerId
        );

        MobEffectInstance currentEffect =
            player.getEffect(
                MobEffects.FIRE_RESISTANCE
            );

        if (
            currentEffect != null
                && currentEffect.getDuration()
                    >= NETHERITE_FIRE_RESISTANCE_DURATION
        ) {
            return;
        }

        player.addEffect(
            new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                NETHERITE_FIRE_RESISTANCE_DURATION,
                0,
                true,
                false,
                true
            )
        );
    }

    private static void updateLapisChestplatePower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        int currentExperienceLevel =
            player.experienceLevel;

        Integer previousExperienceLevel =
            LAST_EXPERIENCE_LEVELS.put(
                playerId,
                currentExperienceLevel
            );

        if (previousExperienceLevel == null) {
            return;
        }

        if (
            currentExperienceLevel
                <= previousExperienceLevel
        ) {
            return;
        }

        ItemStack chestplate =
            player.getItemBySlot(
                EquipmentSlot.CHEST
            );

        boolean hasLapisPower =
            hasSelectedMaterial(
                chestplate,
                PowerMaterial.LAPIS_LAZULI
            );

        if (!hasLapisPower) {
            return;
        }

        int levelsGained =
            currentExperienceLevel
                - previousExperienceLevel;

        player.heal(
            LAPIS_LEVEL_UP_HEAL_AMOUNT
                * levelsGained
        );
    }

    private static void updateQuartzLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean shouldHaveDamageBonus =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.QUARTZ
            );

        AttributeInstance attackDamage =
            player.getAttribute(
                Attributes.ATTACK_DAMAGE
            );

        if (attackDamage == null) {
            return;
        }

        if (shouldHaveDamageBonus) {

            if (
                !attackDamage.hasModifier(
                    QUARTZ_LEGGINGS_DAMAGE_ID
                )
            ) {
                attackDamage.addTransientModifier(
                    QUARTZ_LEGGINGS_DAMAGE_MODIFIER
                );
            }

            return;
        }

        if (
            attackDamage.hasModifier(
                QUARTZ_LEGGINGS_DAMAGE_ID
            )
        ) {
            attackDamage.removeModifier(
                QUARTZ_LEGGINGS_DAMAGE_ID
            );
        }
    }

    private static void updateIronLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean shouldHaveFallResistance =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.IRON
            );

        AttributeInstance fallDamageMultiplier =
            player.getAttribute(
                Attributes.FALL_DAMAGE_MULTIPLIER
            );

        if (fallDamageMultiplier == null) {
            return;
        }

        if (shouldHaveFallResistance) {

            if (
                !fallDamageMultiplier.hasModifier(
                    IRON_LEGGINGS_FALL_DAMAGE_ID
                )
            ) {
                fallDamageMultiplier.addTransientModifier(
                    IRON_LEGGINGS_FALL_DAMAGE_MODIFIER
                );
            }

            return;
        }

        if (
            fallDamageMultiplier.hasModifier(
                IRON_LEGGINGS_FALL_DAMAGE_ID
            )
        ) {
            fallDamageMultiplier.removeModifier(
                IRON_LEGGINGS_FALL_DAMAGE_ID
            );
        }
    }

    private static void updateAmethystLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasAmethystPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.AMETHYST
            );

        if (
            !hasAmethystPower
                || !player.isShiftKeyDown()
                || player.isSpectator()
        ) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        for (
            LivingEntity entity :
            player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(
                    AMETHYST_ECHOLOCATION_RADIUS
                ),
                entity ->
                    entity.isAlive()
                        && entity != player
            )
        ) {
            entity.addEffect(
                new MobEffectInstance(
                    MobEffects.GLOWING,
                    AMETHYST_ECHOLOCATION_GLOW_DURATION,
                    0,
                    true,
                    false,
                    false
                )
            );
        }
    }

    private static void updateRedstoneLeggingsPower(
        ServerPlayer player
    ) {
        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasRedstonePower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.REDSTONE
            );

        if (
            !hasRedstonePower
                || player.isSpectator()
        ) {
            return;
        }

        BlockPos blockBelow =
            player.getBlockPosBelowThatAffectsMyMovement();

        boolean isStandingOnRedstone =
            player.level()
                .getBlockState(blockBelow)
                .is(Blocks.REDSTONE_BLOCK);

        if (!isStandingOnRedstone) {
            return;
        }

        if (
            player.tickCount
                % REDSTONE_HUNGER_INTERVAL
                != 0
        ) {
            return;
        }

        FoodData foodData =
            player.getFoodData();

        if (foodData.getFoodLevel() >= 20) {
            return;
        }

        foodData.eat(
            REDSTONE_HUNGER_AMOUNT,
            REDSTONE_HUNGER_SATURATION
        );
    }

    /**
     * Calça de Lápis-lazúli:
     * concede 25% de experiência adicional.
     */
    private static void updateLapisLeggingsPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        int currentTotalExperience =
            player.totalExperience;

        Integer previousTotalExperience =
            LAST_TOTAL_EXPERIENCE.put(
                playerId,
                currentTotalExperience
            );

        /*
         * Primeira verificação do jogador.
         */
        if (previousTotalExperience == null) {
            return;
        }

        ItemStack leggings =
            player.getItemBySlot(
                EquipmentSlot.LEGS
            );

        boolean hasLapisPower =
            hasSelectedMaterial(
                leggings,
                PowerMaterial.LAPIS_LAZULI
            );

        /*
         * Sem a calça, não acumula frações do bônus.
         */
        if (!hasLapisPower) {
            LAPIS_EXPERIENCE_BONUS_REMAINDERS.remove(
                playerId
            );

            return;
        }

        /*
         * Nenhuma experiência foi obtida.
         *
         * Reduções de experiência também não
         * concedem bônus.
         */
        if (
            currentTotalExperience
                <= previousTotalExperience
        ) {
            return;
        }

        int experienceGained =
            currentTotalExperience
                - previousTotalExperience;

        double previousRemainder =
            LAPIS_EXPERIENCE_BONUS_REMAINDERS
                .getOrDefault(
                    playerId,
                    0.0D
                );

        double calculatedBonus =
            experienceGained
                * LAPIS_EXPERIENCE_BONUS_PERCENTAGE
                + previousRemainder;

        int wholeBonusPoints =
            (int) Math.floor(
                calculatedBonus
            );

        double newRemainder =
            calculatedBonus
                - wholeBonusPoints;

        LAPIS_EXPERIENCE_BONUS_REMAINDERS.put(
            playerId,
            newRemainder
        );

        /*
         * Ainda não acumulou um ponto inteiro.
         */
        if (wholeBonusPoints <= 0) {
            return;
        }

        player.giveExperiencePoints(
            wholeBonusPoints
        );

        /*
         * Salva o total após o bônus para impedir
         * que a experiência concedida pelo próprio
         * poder gere outro bônus no próximo tick.
         */
        LAST_TOTAL_EXPERIENCE.put(
            playerId,
            player.totalExperience
        );
    }

    private static void updateCopperBootsPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean shouldHaveSpeedBonus =
            hasSelectedMaterial(
                boots,
                PowerMaterial.COPPER
            );

        AttributeInstance movementSpeed =
            player.getAttribute(
                Attributes.MOVEMENT_SPEED
            );

        if (movementSpeed == null) {
            return;
        }

        if (shouldHaveSpeedBonus) {

            if (
                !movementSpeed.hasModifier(
                    COPPER_BOOTS_SPEED_ID
                )
            ) {
                movementSpeed.addTransientModifier(
                    COPPER_BOOTS_SPEED_MODIFIER
                );
            }

            return;
        }

        if (
            movementSpeed.hasModifier(
                COPPER_BOOTS_SPEED_ID
            )
        ) {
            movementSpeed.removeModifier(
                COPPER_BOOTS_SPEED_ID
            );
        }
    }

    private static void updateGoldBootsPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean shouldHavePiglinSpeed =
            hasSelectedMaterial(
                boots,
                PowerMaterial.GOLD
            );

        AttributeInstance movementSpeed =
            player.getAttribute(
                Attributes.MOVEMENT_SPEED
            );

        if (movementSpeed == null) {
            return;
        }

        if (!shouldHavePiglinSpeed) {

            if (
                movementSpeed.hasModifier(
                    GOLD_BOOTS_SPEED_ID
                )
            ) {
                movementSpeed.removeModifier(
                    GOLD_BOOTS_SPEED_ID
                );
            }

            return;
        }

        double requiredSpeedDifference =
            PIGLIN_MOVEMENT_SPEED
                - movementSpeed.getBaseValue();

        AttributeModifier piglinSpeedModifier =
            new AttributeModifier(
                GOLD_BOOTS_SPEED_ID,
                requiredSpeedDifference,
                AttributeModifier.Operation.ADD_VALUE
            );

        movementSpeed.addOrUpdateTransientModifier(
            piglinSpeedModifier
        );
    }

    private static void updateDiamondBootsLevitationPower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasDiamondPower =
            hasSelectedMaterial(
                boots,
                PowerMaterial.DIAMOND
            );

        if (
            !hasDiamondPower
                || player.isSpectator()
        ) {
            resetDiamondLevitationCharge(
                playerId
            );

            return;
        }

        boolean isHoldingJump =
            player.getLastClientInput().jump();

        if (!isHoldingJump) {
            resetDiamondLevitationCharge(
                playerId
            );

            return;
        }

        if (
            DIAMOND_LEVITATION_LOCKED.contains(
                playerId
            )
        ) {
            return;
        }

        int currentCharge =
            DIAMOND_LEVITATION_CHARGE_TICKS
                .getOrDefault(
                    playerId,
                    0
                );

        int newCharge =
            currentCharge + 1;

        if (
            newCharge
                < DIAMOND_LEVITATION_CHARGE_DURATION
        ) {
            DIAMOND_LEVITATION_CHARGE_TICKS.put(
                playerId,
                newCharge
            );

            return;
        }

        DIAMOND_LEVITATION_CHARGE_TICKS.remove(
            playerId
        );

        DIAMOND_LEVITATION_LOCKED.add(
            playerId
        );

        MobEffectInstance currentLevitation =
            player.getEffect(
                MobEffects.LEVITATION
            );

        if (
            currentLevitation != null
                && currentLevitation.getDuration()
                    >= DIAMOND_LEVITATION_EFFECT_DURATION
        ) {
            return;
        }

        player.addEffect(
            new MobEffectInstance(
                MobEffects.LEVITATION,
                DIAMOND_LEVITATION_EFFECT_DURATION,
                0,
                true,
                false,
                true
            )
        );
    }

    private static void resetDiamondLevitationCharge(
        UUID playerId
    ) {
        DIAMOND_LEVITATION_CHARGE_TICKS.remove(
            playerId
        );

        DIAMOND_LEVITATION_LOCKED.remove(
            playerId
        );
    }

    private static void updateQuartzBootsPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean shouldHaveJumpBonus =
            hasSelectedMaterial(
                boots,
                PowerMaterial.QUARTZ
            );

        AttributeInstance jumpStrength =
            player.getAttribute(
                Attributes.JUMP_STRENGTH
            );

        if (jumpStrength == null) {
            return;
        }

        if (shouldHaveJumpBonus) {

            if (
                !jumpStrength.hasModifier(
                    QUARTZ_BOOTS_JUMP_ID
                )
            ) {
                jumpStrength.addTransientModifier(
                    QUARTZ_BOOTS_JUMP_MODIFIER
                );
            }

            return;
        }

        if (
            jumpStrength.hasModifier(
                QUARTZ_BOOTS_JUMP_ID
            )
        ) {
            jumpStrength.removeModifier(
                QUARTZ_BOOTS_JUMP_ID
            );
        }
    }

    /**
     * Botas de Redstone:
     * remove a redução de velocidade aplicada
     * pelo bloco sob o jogador.
     */
    private static void updateRedstoneBootsPower(
        ServerPlayer player
    ) {
        ItemStack boots =
            player.getItemBySlot(
                EquipmentSlot.FEET
            );

        boolean hasRedstonePower =
            hasSelectedMaterial(
                boots,
                PowerMaterial.REDSTONE
            );

        AttributeInstance movementEfficiency =
            player.getAttribute(
                Attributes.MOVEMENT_EFFICIENCY
            );

        if (movementEfficiency == null) {
            return;
        }

        if (hasRedstonePower) {

            if (
                !movementEfficiency.hasModifier(
                    REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_ID
                )
            ) {
                movementEfficiency.addTransientModifier(
                    REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_MODIFIER
                );
            }

            return;
        }

        if (
            movementEfficiency.hasModifier(
                REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_ID
            )
        ) {
            movementEfficiency.removeModifier(
                REDSTONE_BOOTS_MOVEMENT_EFFICIENCY_ID
            );
        }
    }

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

    private static MobEffectInstance
        createNightVisionEffect() {

        return new MobEffectInstance(
            MobEffects.NIGHT_VISION,
            NIGHT_VISION_DURATION,
            0,
            true,
            false,
            false
        );
    }

    private static boolean
        isTailoredTraitsNightVision(
            MobEffectInstance effect
        ) {
        if (effect == null) {
            return false;
        }

        return effect.getEffect().equals(
                MobEffects.NIGHT_VISION
            )
            && effect.getAmplifier() == 0
            && effect.isAmbient()
            && !effect.isVisible()
            && !effect.showIcon()
            && effect.getDuration()
                <= NIGHT_VISION_DURATION;
    }
}