package com.marcos.tailoredtraits.power;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

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
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;

public final class PassivePowerHandler {

    /*
     * 240 ticks = 12 segundos.
     */
    private static final int NIGHT_VISION_DURATION =
        240;

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
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

    /*
     * Botas de Quartzo:
     * salto de aproximadamente 2,5 blocos.
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
     * Calça de Quartzo:
     * +20% de dano corpo a corpo.
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
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

    /*
     * Peitoral de Quartzo:
     * +1 bloco de alcance contra entidades.
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
     * Peitoral de Ametista:
     * aumenta o knockback dos ataques corpo a corpo.
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
     * Calça de Ferro:
     * reduz o dano de queda em 10%.
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
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

    /*
     * Capacete de Esmeralda:
     * raio de identificação dos aldeões.
     */
    private static final double
        EMERALD_VILLAGER_MARK_RADIUS =
            24.0D;

    /*
     * 60 ticks = 3 segundos.
     */
    private static final int
        EMERALD_VILLAGER_GLOW_DURATION =
            60;

    /*
     * Capacete de Ouro:
     * raio de identificação das criaturas do Nether.
     */
    private static final double
        GOLD_NETHER_MOB_MARK_RADIUS =
            24.0D;

    /*
     * O brilho permanece durante 3 segundos.
     */
    private static final int
        GOLD_NETHER_MOB_GLOW_DURATION =
            60;

    /*
     * Peitoral de Netherite:
     * 100 ticks = 5 segundos de resistência ao fogo.
     */
    private static final int
        NETHERITE_FIRE_RESISTANCE_DURATION =
            100;

    /*
     * Guarda os jogadores cuja visão noturna
     * foi aplicada pelo Tailored Traits.
     */
    private static final Set<UUID>
        AMETHYST_VISION_PLAYERS =
            new HashSet<>();

    /*
     * Guarda os jogadores que já ativaram o poder
     * do peitoral de Netherite durante a exposição
     * atual ao fogo ou à lava.
     *
     * O jogador só será removido desta lista quando
     * não estiver mais pegando fogo.
     */
    private static final Set<UUID>
        NETHERITE_FIRE_POWER_LOCKED =
            new HashSet<>();

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
                     * Poderes dos capacetes.
                     */
                    updateAmethystHelmetPower(player);
                    updateCopperHelmetPower(player);
                    updateEmeraldHelmetPower(player);
                    updateGoldHelmetPower(player);

                    /*
                     * Poderes dos peitorais.
                     */
                    updateQuartzChestplatePower(player);
                    updateAmethystChestplatePower(player);
                    updateNetheriteChestplatePower(player);

                    /*
                     * Poderes das calças.
                     */
                    updateQuartzLeggingsPower(player);
                    updateIronLeggingsPower(player);

                    /*
                     * Poderes das botas.
                     */
                    updateCopperBootsPower(player);
                    updateQuartzBootsPower(player);
                }
            }
        );

        TailoredTraits.LOGGER.info(
            "Poderes passivos do Tailored Traits registrados."
        );
    }

    /**
     * Capacete configurado como Ametista:
     * concede visão no escuro.
     */
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

            /*
             * Não substitui uma visão noturna recebida
             * por poção, comando ou outro mod.
             */
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

            /*
             * Renova o efeito quando restarem
             * menos de dois segundos.
             */
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

    /**
     * Capacete configurado como Cobre:
     * mostra o período atual do dia.
     */
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

        /*
         * Atualiza a mensagem a cada dois segundos.
         */
        if (player.tickCount % 40 != 0) {
            return;
        }

        /*
         * Um dia completo possui 24.000 ticks.
         */
        long dayTime =
            player.level()
                .getOverworldClockTime()
                % 24000L;

        Component currentPeriod =
            getDayPeriodName(dayTime);

        /*
         * Mostra a mensagem acima da barra de itens.
         */
        player.sendOverlayMessage(
            Component.translatable(
                "message.tailored-traits.current_day_period",
                currentPeriod
            )
        );
    }

    /**
     * Converte o horário do mundo em um período do dia.
     */
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

    /**
     * Capacete configurado como Esmeralda:
     * destaca aldeões próximos.
     */
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

    /**
     * Capacete configurado como Ouro:
     * destaca criaturas nativas do Nether.
     */
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

    /**
     * Retorna true quando a entidade pertence
     * ao grupo de criaturas nativas do Nether.
     */
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

    /**
     * Peitoral configurado como Quartzo:
     * aumenta o alcance contra entidades em um bloco.
     */
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

    /**
     * Peitoral configurado como Ametista:
     * aumenta o knockback causado pelos ataques.
     */
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

    /**
     * Peitoral configurado como Netherite:
     *
     * ativa resistência ao fogo uma única vez
     * durante cada exposição ao fogo ou à lava.
     */
    private static void updateNetheriteChestplatePower(
        ServerPlayer player
    ) {
        UUID playerId =
            player.getUUID();

        /*
         * O jogador é considerado exposto enquanto
         * estiver pegando fogo ou dentro da lava.
         */
        boolean isExposedToFire =
            player.isOnFire()
                || player.isInLava();

        /*
         * Quando o jogador não estiver mais queimando,
         * o poder será liberado para uma nova ativação.
         */
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

        /*
         * Se o jogador já ativou o poder durante
         * esta exposição, não renova o efeito.
         */
        if (
            NETHERITE_FIRE_POWER_LOCKED.contains(
                playerId
            )
        ) {
            return;
        }

        /*
         * Bloqueia imediatamente uma nova ativação
         * durante esta mesma exposição ao fogo.
         */
        NETHERITE_FIRE_POWER_LOCKED.add(
            playerId
        );

        MobEffectInstance currentEffect =
            player.getEffect(
                MobEffects.FIRE_RESISTANCE
            );

        /*
         * Caso o jogador já possua uma resistência
         * ao fogo mais longa, ela não será substituída.
         *
         * Mesmo assim, a habilidade é considerada usada
         * durante esta exposição ao fogo.
         */
        if (
            currentEffect != null
                && currentEffect.getDuration()
                    >= NETHERITE_FIRE_RESISTANCE_DURATION
        ) {
            return;
        }

        /*
         * Aplica exatamente cinco segundos.
         *
         * O efeito não será renovado enquanto
         * o jogador continuar queimando.
         */
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

    /**
     * Calça configurada como Quartzo:
     * aumenta o dano corpo a corpo em 20%.
     */
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

    /**
     * Calça configurada como Ferro:
     * reduz o dano de queda em 10%.
     */
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

    /**
     * Botas configuradas como Cobre:
     * aumenta a velocidade de movimento em 10%.
     */
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

    /**
     * Botas configuradas como Quartzo:
     * salto de aproximadamente 2,5 blocos.
     */
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
     * Cria a visão noturna fornecida
     * pelo capacete de Ametista.
     */
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

    /**
     * Verifica se a visão noturna atual
     * foi criada pelo Tailored Traits.
     */
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