package com.marcos.tailoredtraits.power;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.network.ActivateFullSetAbilityPayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ActiveFullSetPowerInitializer
    implements ModInitializer {

    /*
     * ==================================================
     * DIAMANTE
     * ==================================================
     */

    private static final double
        DIAMOND_TELEPORT_RANGE =
            25.0D;

    private static final double
        DIAMOND_TELEPORT_STEP =
            0.25D;

    private static final int
        DIAMOND_COOLDOWN_TICKS =
            80;

    /*
     * ==================================================
     * AMETISTA
     * ==================================================
     */

    private static final double
        AMETHYST_SONIC_RANGE =
            20.0D;

    private static final double
        AMETHYST_TARGET_INFLATION =
            0.75D;

    private static final float
        AMETHYST_SONIC_DAMAGE =
            18.0F;

    private static final double
        AMETHYST_HORIZONTAL_KNOCKBACK =
            2.5D;

    private static final double
        AMETHYST_VERTICAL_KNOCKBACK =
            0.5D;

    private static final int
        AMETHYST_COOLDOWN_TICKS =
            160;

    /*
     * ==================================================
     * COBRE
     * ==================================================
     */

    private static final double
        COPPER_LIGHTNING_RANGE =
            24.0D;

    private static final double
        COPPER_TARGET_INFLATION =
            0.60D;

    private static final int
        COPPER_COOLDOWN_TICKS =
            160;

    /*
     * ==================================================
     * LÁPIS-LAZÚLI
     * ==================================================
     */

    private static final int
        LAPIS_FANG_COUNT =
            12;

    private static final double
        LAPIS_FANG_SPACING =
            1.25D;

    private static final double
        LAPIS_FIRST_FANG_DISTANCE =
            1.50D;

    private static final int
        LAPIS_FANG_WARMUP_STEP =
            2;

    private static final int
        LAPIS_GROUND_SEARCH_ABOVE =
            2;

    private static final int
        LAPIS_GROUND_SEARCH_BELOW =
            5;

    private static final int
        LAPIS_COOLDOWN_TICKS =
            140;

    /*
     * ==================================================
     * ESMERALDA
     * ==================================================
     */

    private static final double
        EMERALD_GIFT_RANGE =
            12.0D;

    private static final double
        EMERALD_TARGET_INFLATION =
            0.75D;

    private static final int
        EMERALD_COOLDOWN_TICKS =
            500;

    /*
     * ==================================================
     * RECARGAS
     * ==================================================
     */

    private static final Map<UUID, Integer>
        DIAMOND_NEXT_USE_TICK =
            new HashMap<>();

    private static final Map<UUID, Integer>
        AMETHYST_NEXT_USE_TICK =
            new HashMap<>();

    private static final Map<UUID, Integer>
        COPPER_NEXT_USE_TICK =
            new HashMap<>();

    private static final Map<UUID, Integer>
        LAPIS_NEXT_USE_TICK =
            new HashMap<>();

    private static final Map<UUID, Integer>
        EMERALD_NEXT_USE_TICK =
            new HashMap<>();

    public ActiveFullSetPowerInitializer() {
    }

    @Override
    public void onInitialize() {

        PayloadTypeRegistry
            .serverboundPlay()
            .register(
                ActivateFullSetAbilityPayload.TYPE,
                ActivateFullSetAbilityPayload.CODEC
            );

        ServerPlayNetworking.registerGlobalReceiver(
            ActivateFullSetAbilityPayload.TYPE,
            ActiveFullSetPowerInitializer
                ::handleAbilityActivation
        );

        TailoredTraits.LOGGER.info(
            "Poderes ativos de conjunto registrados."
        );
    }

    /*
     * ==================================================
     * ATIVAÇÃO
     * ==================================================
     */

    private static void handleAbilityActivation(
        ActivateFullSetAbilityPayload payload,
        ServerPlayNetworking.Context context
    ) {
        if (!payload.activated()) {
            return;
        }

        ServerPlayer player =
            context.player();

        if (
            !player.isAlive()
                || player.isSpectator()
        ) {
            return;
        }

        PowerMaterial activeMaterial =
            FullSetPowerUtil
                .getActiveFullSetMaterial(
                    player
                );

        if (activeMaterial == null) {
            player.sendSystemMessage(
                Component.literal(
                    "Nenhum poder de conjunto está ativo."
                ),
                true
            );

            return;
        }

        switch (activeMaterial) {
            case DIAMOND ->
                tryDiamondTeleport(
                    player
                );

            case AMETHYST ->
                tryAmethystSonicBoom(
                    player
                );

            case COPPER ->
                tryCopperLightning(
                    player
                );

            case LAPIS_LAZULI ->
                tryLapisEvokerFangs(
                    player
                );

            case EMERALD ->
                tryEmeraldGift(
                    player
                );

            default ->
                player.sendSystemMessage(
                    Component.literal(
                        "Esse conjunto não possui uma "
                            + "habilidade ativa na tecla R."
                    ),
                    true
                );
        }
    }

    /*
     * ==================================================
     * DIAMANTE
     * ==================================================
     */

    private static void tryDiamondTeleport(
        ServerPlayer player
    ) {
        int currentTick =
            getCurrentServerTick(
                player
            );

        if (
            isOnCooldown(
                player,
                DIAMOND_NEXT_USE_TICK,
                currentTick
            )
        ) {
            return;
        }

        ServerLevel level =
            player.level();

        Vec3 direction =
            player.getLookAngle();

        if (
            direction.lengthSqr()
                < 1.0E-6D
        ) {
            return;
        }

        direction =
            direction.normalize();

        Vec3 startPosition =
            player.position();

        AABB originalBox =
            player.getBoundingBox();

        Vec3 lastSafePosition =
            startPosition;

        for (
            double distance =
                DIAMOND_TELEPORT_STEP;
            distance <= DIAMOND_TELEPORT_RANGE;
            distance += DIAMOND_TELEPORT_STEP
        ) {
            Vec3 candidatePosition =
                startPosition.add(
                    direction.scale(
                        distance
                    )
                );

            Vec3 movement =
                candidatePosition.subtract(
                    startPosition
                );

            AABB candidateBox =
                originalBox.move(
                    movement.x,
                    movement.y,
                    movement.z
                );

            if (
                !level.noCollision(
                    player,
                    candidateBox
                )
            ) {
                break;
            }

            lastSafePosition =
                candidatePosition;
        }

        if (
            lastSafePosition.distanceToSqr(
                startPosition
            ) < 1.0D
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "Não há espaço livre para "
                        + "o teletransporte."
                ),
                true
            );

            return;
        }

        spawnPortalParticles(
            level,
            startPosition.add(
                0.0D,
                player.getBbHeight() * 0.5D,
                0.0D
            )
        );

        player.playSound(
            SoundEvents.ENDERMAN_TELEPORT,
            1.0F,
            1.0F
        );

        player.teleportTo(
            lastSafePosition.x,
            lastSafePosition.y,
            lastSafePosition.z
        );

        player.setDeltaMovement(
            Vec3.ZERO
        );

        player.fallDistance =
            0.0F;

        player.hurtMarked =
            true;

        spawnPortalParticles(
            level,
            lastSafePosition.add(
                0.0D,
                player.getBbHeight() * 0.5D,
                0.0D
            )
        );

        player.playSound(
            SoundEvents.ENDERMAN_TELEPORT,
            1.0F,
            1.0F
        );

        DIAMOND_NEXT_USE_TICK.put(
            player.getUUID(),
            currentTick
                + DIAMOND_COOLDOWN_TICKS
        );
    }

    private static void spawnPortalParticles(
        ServerLevel level,
        Vec3 position
    ) {
        level.sendParticles(
            ParticleTypes.PORTAL,
            position.x,
            position.y,
            position.z,
            40,
            0.45D,
            0.80D,
            0.45D,
            0.20D
        );
    }

    /*
     * ==================================================
     * AMETISTA
     * ==================================================
     */

    private static void tryAmethystSonicBoom(
        ServerPlayer player
    ) {
        int currentTick =
            getCurrentServerTick(
                player
            );

        if (
            isOnCooldown(
                player,
                AMETHYST_NEXT_USE_TICK,
                currentTick
            )
        ) {
            return;
        }

        LivingEntity target =
            findLivingTargetOnRay(
                player,
                AMETHYST_SONIC_RANGE,
                AMETHYST_TARGET_INFLATION
            );

        if (target == null) {
            player.sendSystemMessage(
                Component.literal(
                    "Nenhum alvo foi encontrado "
                        + "na direção da mira."
                ),
                true
            );

            return;
        }

        ServerLevel level =
            player.level();

        Vec3 start =
            player.getEyePosition();

        Vec3 targetCenter =
            target.getBoundingBox()
                .getCenter();

        Vec3 direction =
            targetCenter.subtract(
                start
            ).normalize();

        spawnSonicBeam(
            level,
            start,
            targetCenter
        );

        player.playSound(
            SoundEvents.WARDEN_SONIC_BOOM,
            3.0F,
            1.0F
        );

        boolean damaged =
            target.hurtServer(
                level,
                player.damageSources()
                    .sonicBoom(
                        player
                    ),
                AMETHYST_SONIC_DAMAGE
            );

        if (damaged) {
            target.push(
                direction.x
                    * AMETHYST_HORIZONTAL_KNOCKBACK,
                AMETHYST_VERTICAL_KNOCKBACK,
                direction.z
                    * AMETHYST_HORIZONTAL_KNOCKBACK
            );

            target.hurtMarked =
                true;
        }

        AMETHYST_NEXT_USE_TICK.put(
            player.getUUID(),
            currentTick
                + AMETHYST_COOLDOWN_TICKS
        );
    }

    private static void spawnSonicBeam(
        ServerLevel level,
        Vec3 start,
        Vec3 end
    ) {
        Vec3 difference =
            end.subtract(
                start
            );

        double length =
            difference.length();

        if (length <= 0.0D) {
            return;
        }

        Vec3 direction =
            difference.normalize();

        for (
            double distance = 1.0D;
            distance < length;
            distance += 1.0D
        ) {
            Vec3 particlePosition =
                start.add(
                    direction.scale(
                        distance
                    )
                );

            level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                particlePosition.x,
                particlePosition.y,
                particlePosition.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
            );
        }
    }

    /*
     * ==================================================
     * COBRE
     * ==================================================
     */

    private static void tryCopperLightning(
        ServerPlayer player
    ) {
        int currentTick =
            getCurrentServerTick(
                player
            );

        if (
            isOnCooldown(
                player,
                COPPER_NEXT_USE_TICK,
                currentTick
            )
        ) {
            return;
        }

        ServerLevel level =
            player.level();

        Vec3 strikePosition =
            findCopperStrikePosition(
                player
            );

        if (strikePosition == null) {
            player.sendSystemMessage(
                Component.literal(
                    "Mire em um bloco ou criatura "
                        + "para invocar o raio."
                ),
                true
            );

            return;
        }

        LightningBolt lightning =
            new LightningBolt(
                EntityTypes.LIGHTNING_BOLT,
                level
            );

        lightning.snapTo(
            strikePosition.x,
            strikePosition.y,
            strikePosition.z
        );

        lightning.setCause(
            player
        );

        level.addFreshEntity(
            lightning
        );

        level.sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            player.getX(),
            player.getEyeY(),
            player.getZ(),
            24,
            0.40D,
            0.50D,
            0.40D,
            0.12D
        );

        COPPER_NEXT_USE_TICK.put(
            player.getUUID(),
            currentTick
                + COPPER_COOLDOWN_TICKS
        );
    }

    private static Vec3 findCopperStrikePosition(
        ServerPlayer player
    ) {
        ServerLevel level =
            player.level();

        Vec3 start =
            player.getEyePosition();

        Vec3 lookDirection =
            player.getLookAngle();

        if (
            lookDirection.lengthSqr()
                < 1.0E-6D
        ) {
            return null;
        }

        Vec3 end =
            start.add(
                lookDirection
                    .normalize()
                    .scale(
                        COPPER_LIGHTNING_RANGE
                    )
            );

        BlockHitResult blockHit =
            level.clip(
                new ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                )
            );

        LivingEntity entityTarget =
            findLivingTargetOnRay(
                player,
                COPPER_LIGHTNING_RANGE,
                COPPER_TARGET_INFLATION
            );

        double blockDistanceSquared =
            Double.MAX_VALUE;

        if (
            blockHit.getType()
                != HitResult.Type.MISS
        ) {
            blockDistanceSquared =
                start.distanceToSqr(
                    blockHit.getLocation()
                );
        }

        if (entityTarget != null) {
            Vec3 entityCenter =
                entityTarget
                    .getBoundingBox()
                    .getCenter();

            double entityDistanceSquared =
                start.distanceToSqr(
                    entityCenter
                );

            if (
                entityDistanceSquared
                    <= blockDistanceSquared
            ) {
                return entityTarget.position();
            }
        }

        if (
            blockHit.getType()
                == HitResult.Type.MISS
        ) {
            return null;
        }

        return blockHit.getLocation();
    }

    /*
     * ==================================================
     * LÁPIS-LAZÚLI
     * ==================================================
     */

    private static void tryLapisEvokerFangs(
        ServerPlayer player
    ) {
        int currentTick =
            getCurrentServerTick(
                player
            );

        if (
            isOnCooldown(
                player,
                LAPIS_NEXT_USE_TICK,
                currentTick
            )
        ) {
            return;
        }

        ServerLevel level =
            player.level();

        Vec3 lookDirection =
            player.getLookAngle();

        Vec3 horizontalDirection =
            new Vec3(
                lookDirection.x,
                0.0D,
                lookDirection.z
            );

        if (
            horizontalDirection.lengthSqr()
                < 1.0E-6D
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "Olhe para uma direção horizontal "
                        + "para criar as presas."
                ),
                true
            );

            return;
        }

        horizontalDirection =
            horizontalDirection.normalize();

        float fangYaw =
            (float) Math.atan2(
                horizontalDirection.z,
                horizontalDirection.x
            );

        int spawnedFangCount =
            0;

        for (
            int index = 0;
            index < LAPIS_FANG_COUNT;
            index++
        ) {
            double distance =
                LAPIS_FIRST_FANG_DISTANCE
                    + index
                        * LAPIS_FANG_SPACING;

            double fangX =
                player.getX()
                    + horizontalDirection.x
                        * distance;

            double fangZ =
                player.getZ()
                    + horizontalDirection.z
                        * distance;

            Double fangY =
                findFangGroundHeight(
                    level,
                    player,
                    fangX,
                    player.getY(),
                    fangZ
                );

            if (fangY == null) {
                continue;
            }

            int warmupTicks =
                index
                    * LAPIS_FANG_WARMUP_STEP;

            EvokerFangs fangs =
                new EvokerFangs(
                    level,
                    fangX,
                    fangY,
                    fangZ,
                    fangYaw,
                    warmupTicks,
                    player
                );

            if (
                level.addFreshEntity(
                    fangs
                )
            ) {
                spawnedFangCount++;
            }
        }

        if (spawnedFangCount == 0) {
            player.sendSystemMessage(
                Component.literal(
                    "Não existe chão adequado "
                        + "para criar as presas."
                ),
                true
            );

            return;
        }

        LAPIS_NEXT_USE_TICK.put(
            player.getUUID(),
            currentTick
                + LAPIS_COOLDOWN_TICKS
        );
    }

    private static Double findFangGroundHeight(
        ServerLevel level,
        ServerPlayer player,
        double x,
        double referenceY,
        double z
    ) {
        int blockX =
            (int) Math.floor(
                x
            );

        int blockZ =
            (int) Math.floor(
                z
            );

        int startingY =
            (int) Math.floor(
                referenceY
            ) + LAPIS_GROUND_SEARCH_ABOVE;

        int endingY =
            (int) Math.floor(
                referenceY
            ) - LAPIS_GROUND_SEARCH_BELOW;

        for (
            int blockY = startingY;
            blockY >= endingY;
            blockY--
        ) {
            BlockPos blockPosition =
                new BlockPos(
                    blockX,
                    blockY,
                    blockZ
                );

            BlockState blockState =
                level.getBlockState(
                    blockPosition
                );

            VoxelShape collisionShape =
                blockState.getCollisionShape(
                    level,
                    blockPosition
                );

            if (
                collisionShape.isEmpty()
            ) {
                continue;
            }

            double surfaceY =
                blockPosition.getY()
                    + collisionShape.max(
                        Direction.Axis.Y
                    );

            AABB fangSpace =
                new AABB(
                    x - 0.25D,
                    surfaceY,
                    z - 0.25D,
                    x + 0.25D,
                    surfaceY + 0.80D,
                    z + 0.25D
                );

            if (
                !level.noCollision(
                    player,
                    fangSpace
                )
            ) {
                continue;
            }

            return surfaceY;
        }

        return null;
    }

    /*
     * ==================================================
     * ESMERALDA
     * ==================================================
     */

    private static void tryEmeraldGift(
        ServerPlayer player
    ) {
        int currentTick =
            getCurrentServerTick(
                player
            );

        if (
            isOnCooldown(
                player,
                EMERALD_NEXT_USE_TICK,
                currentTick
            )
        ) {
            return;
        }

        LivingEntity lookedTarget =
            findLivingTargetOnRay(
                player,
                EMERALD_GIFT_RANGE,
                EMERALD_TARGET_INFLATION
            );

        if (
            !(lookedTarget
                instanceof Villager villager)
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "Mire em um aldeão adulto "
                        + "para receber um presente."
                ),
                true
            );

            return;
        }

        if (
            !isTargetVisible(
                player,
                villager
            )
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "O aldeão está atrás de um obstáculo."
                ),
                true
            );

            return;
        }

        if (villager.isBaby()) {
            player.sendSystemMessage(
                Component.literal(
                    "Apenas aldeões adultos podem "
                        + "entregar presentes."
                ),
                true
            );

            return;
        }

        ItemStack gift =
            createEmeraldGift(
                player
            );

        BehaviorUtils.throwItem(
            villager,
            gift,
            player.position().add(
                0.0D,
                1.0D,
                0.0D
            )
        );

        ServerLevel level =
            player.level();

        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            villager.getX(),
            villager.getY()
                + villager.getBbHeight()
                    * 0.75D,
            villager.getZ(),
            14,
            0.35D,
            0.50D,
            0.35D,
            0.05D
        );

        villager.playSound(
            SoundEvents.VILLAGER_YES,
            1.0F,
            1.0F
        );

        player.sendSystemMessage(
            Component.literal(
                "O aldeão entregou: "
            ).append(
                gift.getHoverName()
            ).append(
                Component.literal(
                    " x"
                        + gift.getCount()
                )
            ),
            true
        );

        EMERALD_NEXT_USE_TICK.put(
            player.getUUID(),
            currentTick
                + EMERALD_COOLDOWN_TICKS
        );
    }

    private static ItemStack createEmeraldGift(
        ServerPlayer player
    ) {
        int giftType =
            player.getRandom()
                .nextInt(
                    8
                );

        return switch (giftType) {
            case 0 ->
                new ItemStack(
                    Items.EMERALD,
                    1
                        + player.getRandom()
                            .nextInt(
                                2
                            )
                );

            case 1 ->
                new ItemStack(
                    Items.BREAD,
                    2
                        + player.getRandom()
                            .nextInt(
                                3
                            )
                );

            case 2 ->
                new ItemStack(
                    Items.BOOK,
                    1
                        + player.getRandom()
                            .nextInt(
                                2
                            )
                );

            case 3 ->
                new ItemStack(
                    Items.ARROW,
                    8
                        + player.getRandom()
                            .nextInt(
                                9
                            )
                );

            case 4 ->
                new ItemStack(
                    Items.REDSTONE,
                    4
                        + player.getRandom()
                            .nextInt(
                                5
                            )
                );

            case 5 ->
                new ItemStack(
                    Items.LAPIS_LAZULI,
                    4
                        + player.getRandom()
                            .nextInt(
                                5
                            )
                );

            case 6 ->
                new ItemStack(
                    Items.IRON_INGOT,
                    1
                        + player.getRandom()
                            .nextInt(
                                3
                            )
                );

            default ->
                new ItemStack(
                    Items.GOLDEN_CARROT,
                    1
                        + player.getRandom()
                            .nextInt(
                                3
                            )
                );
        };
    }

    /*
     * ==================================================
     * MÉTODOS COMPARTILHADOS
     * ==================================================
     */

    private static LivingEntity
        findLivingTargetOnRay(
            ServerPlayer player,
            double range,
            double targetInflation
        ) {
        ServerLevel level =
            player.level();

        Vec3 start =
            player.getEyePosition();

        Vec3 lookDirection =
            player.getLookAngle();

        if (
            lookDirection.lengthSqr()
                < 1.0E-6D
        ) {
            return null;
        }

        Vec3 end =
            start.add(
                lookDirection
                    .normalize()
                    .scale(
                        range
                    )
            );

        AABB searchArea =
            new AABB(
                start,
                end
            ).inflate(
                2.0D
            );

        List<LivingEntity> candidates =
            level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                entity ->
                    entity != player
                        && entity.isAlive()
                        && !entity.isSpectator()
            );

        LivingEntity bestTarget =
            null;

        double bestDistanceSquared =
            range * range;

        for (
            LivingEntity candidate :
            candidates
        ) {
            Optional<Vec3> hitPosition =
                candidate
                    .getBoundingBox()
                    .inflate(
                        targetInflation
                    )
                    .clip(
                        start,
                        end
                    );

            if (hitPosition.isEmpty()) {
                continue;
            }

            double distanceSquared =
                start.distanceToSqr(
                    hitPosition.get()
                );

            if (
                distanceSquared
                    < bestDistanceSquared
            ) {
                bestDistanceSquared =
                    distanceSquared;

                bestTarget =
                    candidate;
            }
        }

        return bestTarget;
    }

    private static boolean isTargetVisible(
        ServerPlayer player,
        LivingEntity target
    ) {
        ServerLevel level =
            player.level();

        Vec3 start =
            player.getEyePosition();

        Vec3 targetCenter =
            target.getBoundingBox()
                .getCenter();

        BlockHitResult blockHit =
            level.clip(
                new ClipContext(
                    start,
                    targetCenter,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                )
            );

        if (
            blockHit.getType()
                == HitResult.Type.MISS
        ) {
            return true;
        }

        double targetDistanceSquared =
            start.distanceToSqr(
                targetCenter
            );

        double blockDistanceSquared =
            start.distanceToSqr(
                blockHit.getLocation()
            );

        return blockDistanceSquared
            + 0.25D
            >= targetDistanceSquared;
    }

    private static int getCurrentServerTick(
        ServerPlayer player
    ) {
        return player.level()
            .getServer()
            .getTickCount();
    }

    private static boolean isOnCooldown(
        ServerPlayer player,
        Map<UUID, Integer> cooldownMap,
        int currentTick
    ) {
        int nextUseTick =
            cooldownMap.getOrDefault(
                player.getUUID(),
                0
            );

        if (
            currentTick
                >= nextUseTick
        ) {
            return false;
        }

        int remainingTicks =
            nextUseTick
                - currentTick;

        int remainingSeconds =
            Math.max(
                1,
                (int) Math.ceil(
                    remainingTicks
                        / 20.0D
                )
            );

        player.sendSystemMessage(
            Component.literal(
                "A habilidade estará pronta em "
                    + remainingSeconds
                    + "s."
            ),
            true
        );

        return true;
    }
}