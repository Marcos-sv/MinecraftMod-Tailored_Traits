package com.marcos.tailoredtraits.power;

import com.marcos.tailoredtraits.component.ModComponents;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class FullSetPowerUtil {

    /*
     * Peças obrigatórias para que uma
     * habilidade de conjunto fique ativa.
     */
    private static final EquipmentSlot[]
        FULL_SET_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

    private FullSetPowerUtil() {
    }

    /**
     * Verifica se a entidade está usando
     * quatro peças com acabamento de Stevium.
     */
    public static boolean hasCompleteSteviumSet(
        LivingEntity entity
    ) {
        if (entity == null) {
            return false;
        }

        for (
            EquipmentSlot slot :
            FULL_SET_SLOTS
        ) {
            ItemStack armorPiece =
                entity.getItemBySlot(
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
     * Retorna o material do poder de conjunto
     * atualmente ativo.
     *
     * Retorna null quando:
     *
     * - alguma peça está ausente;
     * - alguma peça não possui Stevium;
     * - alguma peça possui uma escolha diferente;
     * - o material salvo não existe;
     * - nenhuma escolha foi feita.
     */
    public static PowerMaterial
        getActiveFullSetMaterial(
            LivingEntity entity
        ) {
        if (
            !hasCompleteSteviumSet(
                entity
            )
        ) {
            return null;
        }

        String commonMaterialId =
            null;

        for (
            EquipmentSlot slot :
            FULL_SET_SLOTS
        ) {
            ItemStack armorPiece =
                entity.getItemBySlot(
                    slot
                );

            String pieceMaterialId =
                armorPiece.getOrDefault(
                    ModComponents
                        .SELECTED_SET_POWER_MATERIAL,
                    ""
                );

            /*
             * Uma das peças não possui
             * material de conjunto salvo.
             */
            if (pieceMaterialId.isBlank()) {
                return null;
            }

            /*
             * A primeira peça define o material
             * que as outras precisam possuir.
             */
            if (commonMaterialId == null) {
                commonMaterialId =
                    pieceMaterialId;

                continue;
            }

            /*
             * As quatro peças precisam possuir
             * exatamente a mesma escolha.
             */
            if (
                !commonMaterialId.equals(
                    pieceMaterialId
                )
            ) {
                return null;
            }
        }

        return PowerMaterial.fromId(
            commonMaterialId
        );
    }

    /**
     * Verifica se um material específico
     * está ativo como poder de conjunto.
     *
     * Exemplo futuro:
     *
     * FullSetPowerUtil.hasActiveFullSetMaterial(
     *     player,
     *     PowerMaterial.IRON
     * );
     */
    public static boolean
        hasActiveFullSetMaterial(
            LivingEntity entity,
            PowerMaterial expectedMaterial
        ) {
        if (expectedMaterial == null) {
            return false;
        }

        PowerMaterial activeMaterial =
            getActiveFullSetMaterial(
                entity
            );

        return activeMaterial
            == expectedMaterial;
    }
}