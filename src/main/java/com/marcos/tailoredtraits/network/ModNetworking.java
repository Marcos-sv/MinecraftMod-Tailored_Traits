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

    private static final int INDIVIDUAL_POWER_COST = 2;

    private ModNetworking() {
    }

    public static void initialize() {
        /*
         * Registra o tipo de pacote que vai
         * do cliente para o servidor.
         */
        PayloadTypeRegistry
            .serverboundPlay()
            .register(
                SelectIndividualPowerPayload.TYPE,
                SelectIndividualPowerPayload.CODEC
            );

        /*
         * Registra o código executado pelo servidor
         * quando o pacote chega.
         */
        ServerPlayNetworking.registerGlobalReceiver(
            SelectIndividualPowerPayload.TYPE,
            ModNetworking::handleIndividualPowerSelection
        );

        TailoredTraits.LOGGER.info(
            "Rede do Tailored Traits registrada."
        );
    }

    private static void handleIndividualPowerSelection(
        SelectIndividualPowerPayload payload,
        ServerPlayNetworking.Context context
    ) {
        ServerPlayer player = context.player();

        EquipmentSlot slot = payload.slot();

        /*
         * Impede que o cliente envie mão principal,
         * sela, corpo de animal ou outro espaço inválido.
         */
        if (!isSupportedArmorSlot(slot)) {
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
            || !selectedMaterial.isIndividualOption()
        ) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.tailored-traits.invalid_selection"
                )
            );

            return;
        }

        ItemStack armorStack =
            player.getItemBySlot(slot);

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
        if (!player.getAbilities().instabuild) {
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

    private static boolean isSupportedArmorSlot(
        EquipmentSlot slot
    ) {
        return slot == EquipmentSlot.HEAD
            || slot == EquipmentSlot.CHEST
            || slot == EquipmentSlot.LEGS
            || slot == EquipmentSlot.FEET;
    }

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