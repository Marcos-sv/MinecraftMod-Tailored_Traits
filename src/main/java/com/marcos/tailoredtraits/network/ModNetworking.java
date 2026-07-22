package com.marcos.tailoredtraits.network;

import com.marcos.tailoredtraits.TailoredTraits;
import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.power.PowerMaterial;
import com.marcos.tailoredtraits.power.SteviumArmorUtil;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ModNetworking {

    /*
     * Custo para alterar o poder
     * individual de uma peça.
     */
    private static final int
        INDIVIDUAL_POWER_COST =
            2;

    /*
     * Custo para alterar o poder
     * do conjunto completo.
     */
    private static final int
        FULL_SET_POWER_COST =
            5;

    /*
     * Peças obrigatórias para salvar
     * o poder do conjunto completo.
     */
    private static final EquipmentSlot[]
        FULL_SET_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

    private ModNetworking() {
    }

    public static void initialize() {

        /*
         * Pacote do poder individual.
         */
        PayloadTypeRegistry
            .serverboundPlay()
            .register(
                SelectIndividualPowerPayload.TYPE,
                SelectIndividualPowerPayload.CODEC
            );

        /*
         * Pacote do poder de conjunto.
         */
        PayloadTypeRegistry
            .serverboundPlay()
            .register(
                SelectFullSetPowerPayload.TYPE,
                SelectFullSetPowerPayload.CODEC
            );

        /*
         * Processamento da escolha individual.
         */
        ServerPlayNetworking.registerGlobalReceiver(
            SelectIndividualPowerPayload.TYPE,
            ModNetworking
                ::handleIndividualPowerSelection
        );

        /*
         * Processamento da escolha do conjunto.
         */
        ServerPlayNetworking.registerGlobalReceiver(
            SelectFullSetPowerPayload.TYPE,
            ModNetworking
                ::handleFullSetPowerSelection
        );

        TailoredTraits.LOGGER.info(
            "Rede do Tailored Traits registrada."
        );
    }

    /**
     * Processa a escolha individual
     * de uma peça da armadura.
     */
    private static void handleIndividualPowerSelection(
        SelectIndividualPowerPayload payload,
        ServerPlayNetworking.Context context
    ) {
        ServerPlayer player =
            context.player();

        EquipmentSlot slot =
            payload.slot();

        /*
         * Impede que o cliente envie mão principal,
         * sela, corpo de animal ou outro espaço inválido.
         */
        if (
            !isSupportedArmorSlot(
                slot
            )
        ) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.invalid_selection"
                )
            );

            return;
        }

        PowerMaterial selectedMaterial =
            PowerMaterial.fromId(
                payload.materialId()
            );

        /*
         * O Stevium não pode ser usado como
         * poder individual.
         */
        if (
            selectedMaterial == null
                || !selectedMaterial
                    .isIndividualOption()
        ) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.invalid_selection"
                )
            );

            return;
        }

        ItemStack armorStack =
            player.getItemBySlot(
                slot
            );

        /*
         * O servidor confirma que a peça equipada
         * realmente tem um enfeite de Stevium.
         */
        if (
            !SteviumArmorUtil.hasSteviumTrim(
                armorStack
            )
        ) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.piece_not_equipped"
                )
            );

            return;
        }

        String currentMaterialId =
            armorStack.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        /*
         * Selecionar novamente a mesma opção
         * não cobra experiência.
         */
        if (
            currentMaterialId.equals(
                selectedMaterial.getId()
            )
        ) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.already_selected"
                )
            );

            return;
        }

        /*
         * No modo criativo, a alteração é gratuita.
         */
        if (
            !player.getAbilities().instabuild
        ) {
            if (
                player.experienceLevel
                    < INDIVIDUAL_POWER_COST
            ) {
                player.sendSystemMessage(
                    Component.translatable(
                        "message.tailored-traits.not_enough_levels"
                    )
                );

                return;
            }

            player.giveExperienceLevels(
                -INDIVIDUAL_POWER_COST
            );
        }

        /*
         * Salva a escolha diretamente na peça.
         */
        armorStack.set(
            ModComponents.SELECTED_POWER_MATERIAL,
            selectedMaterial.getId()
        );

        /*
         * Envia a armadura atualizada ao cliente.
         */
        player.inventoryMenu.broadcastChanges();

        player.sendSystemMessage(
            Component.translatable(
                "message.tailored-traits.power_saved",
                selectedMaterial.getDisplayName(),
                getSlotDisplayName(slot)
            )
        );
    }

    /**
     * Processa a seleção do poder
     * de conjunto completo.
     */
    private static void handleFullSetPowerSelection(
        SelectFullSetPowerPayload payload,
        ServerPlayNetworking.Context context
    ) {
        ServerPlayer player =
            context.player();

        PowerMaterial selectedMaterial =
            PowerMaterial.fromId(
                payload.materialId()
            );

        /*
         * Todos os materiais existentes podem
         * ser usados como poder de conjunto.
         *
         * Isso inclui o próprio Stevium.
         */
        if (selectedMaterial == null) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.invalid_selection"
                )
            );

            return;
        }

        /*
         * O servidor verifica novamente as quatro
         * peças, sem confiar apenas no menu cliente.
         */
        if (
            !hasCompleteSteviumSet(
                player
            )
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "Equipe as quatro peças com acabamento "
                        + "de Stevium para selecionar um "
                        + "poder de conjunto."
                )
            );

            return;
        }

        /*
         * Selecionar novamente o mesmo material
         * não cobra níveis.
         *
         * A escolha precisa estar igual nas
         * quatro peças para ser considerada
         * a mesma seleção.
         */
        if (
            allPiecesAlreadyUseSetMaterial(
                player,
                selectedMaterial
            )
        ) {
            player.sendSystemMessage(
                Component.literal(
                    "Esse poder de conjunto já está selecionado."
                )
            );

            return;
        }

        /*
         * No modo criativo, a seleção
         * do conjunto é gratuita.
         */
        if (
            !player.getAbilities().instabuild
        ) {
            if (
                player.experienceLevel
                    < FULL_SET_POWER_COST
            ) {
                player.sendSystemMessage(
                    Component.literal(
                        "São necessários 5 níveis de experiência "
                            + "para alterar o poder do conjunto."
                    )
                );

                return;
            }

            player.giveExperienceLevels(
                -FULL_SET_POWER_COST
            );
        }

        /*
         * Salva exatamente a mesma seleção
         * nas quatro peças equipadas.
         */
        for (
            EquipmentSlot slot :
            FULL_SET_SLOTS
        ) {
            ItemStack armorPiece =
                player.getItemBySlot(
                    slot
                );

            armorPiece.set(
                ModComponents
                    .SELECTED_SET_POWER_MATERIAL,
                selectedMaterial.getId()
            );
        }

        /*
         * Sincroniza as quatro peças
         * modificadas com o cliente.
         */
        player.inventoryMenu.broadcastChanges();

        player.sendSystemMessage(
            Component.literal(
                "Poder de conjunto salvo: "
            ).append(
                selectedMaterial.getDisplayName()
            ).append(
                Component.literal(".")
            )
        );
    }

    /**
     * Verifica se o jogador está usando
     * quatro peças com acabamento de Stevium.
     */
    private static boolean hasCompleteSteviumSet(
        ServerPlayer player
    ) {
        for (
            EquipmentSlot slot :
            FULL_SET_SLOTS
        ) {
            ItemStack armorPiece =
                player.getItemBySlot(
                    slot
                );

            if (
                !SteviumArmorUtil.hasSteviumTrim(
                    armorPiece
                )
            ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se todas as quatro peças
     * já possuem o material escolhido.
     */
    private static boolean
        allPiecesAlreadyUseSetMaterial(
            ServerPlayer player,
            PowerMaterial selectedMaterial
        ) {
        String selectedMaterialId =
            selectedMaterial.getId();

        for (
            EquipmentSlot slot :
            FULL_SET_SLOTS
        ) {
            ItemStack armorPiece =
                player.getItemBySlot(
                    slot
                );

            String currentMaterialId =
                armorPiece.getOrDefault(
                    ModComponents
                        .SELECTED_SET_POWER_MATERIAL,
                    ""
                );

            if (
                !selectedMaterialId.equals(
                    currentMaterialId
                )
            ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se o espaço enviado corresponde
     * a uma peça de armadura suportada.
     */
    private static boolean isSupportedArmorSlot(
        EquipmentSlot slot
    ) {
        return slot == EquipmentSlot.HEAD
            || slot == EquipmentSlot.CHEST
            || slot == EquipmentSlot.LEGS
            || slot == EquipmentSlot.FEET;
    }

    /**
     * Retorna o nome traduzido da peça
     * alterada individualmente.
     */
    private static Component getSlotDisplayName(
        EquipmentSlot slot
    ) {
        return switch (slot) {
            case HEAD ->
                Component.translatable(
                    "screen.tailored-traits.helmet"
                );

            case CHEST ->
                Component.translatable(
                    "screen.tailored-traits.chestplate"
                );

            case LEGS ->
                Component.translatable(
                    "screen.tailored-traits.leggings"
                );

            case FEET ->
                Component.translatable(
                    "screen.tailored-traits.boots"
                );

            default ->
                Component.literal("?");
        };
    }
}