package com.marcos.tailoredtraits.power;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.trim.ModTrimMaterials;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;

public final class SteviumArmorUtil {

    private SteviumArmorUtil() {
    }

    /**
     * Verifica se a peça possui um material de acabamento
     * capaz de fornecer poderes.
     *
     * Regras:
     *
     * - Stevium continua usando os materiais escolhidos na Matriz;
     * - materiais normais ativam automaticamente o próprio poder;
     * - o formato do template é somente visual.
     *
     * O nome deste método foi mantido para não ser necessário
     * modificar todos os arquivos de poderes já implementados.
     */
    public static boolean hasSteviumTrim(
        ItemStack stack
    ) {
        ArmorTrim trim =
            getArmorTrim(
                stack
            );

        if (trim == null) {
            return false;
        }

        /*
         * Stevium mantém o funcionamento antigo.
         *
         * Nesse caso, o poder individual e o poder
         * de conjunto continuam sendo definidos
         * pelas escolhas feitas na Matriz.
         */
        if (
            trim.material().is(
                ModTrimMaterials.STEVIUM
            )
        ) {
            return true;
        }

        PowerMaterial directMaterial =
            getDirectPowerMaterial(
                trim
            );

        if (directMaterial == null) {
            return false;
        }

        /*
         * Para materiais normais, o próprio material
         * usado no acabamento define o poder.
         *
         * Exemplo:
         *
         * acabamento de ametista
         * = poderes de ametista.
         */
        synchronizeDirectMaterial(
            stack,
            directMaterial
        );

        return true;
    }

    /**
     * Verifica especificamente se o material
     * do acabamento é Stevium.
     *
     * Diferente de hasSteviumTrim, este método
     * não aceita os materiais normais.
     */
    public static boolean isSteviumTrim(
        ItemStack stack
    ) {
        ArmorTrim trim =
            getArmorTrim(
                stack
            );

        return trim != null
            && trim.material().is(
                ModTrimMaterials.STEVIUM
            );
    }

    /**
     * Retorna o poder ligado diretamente ao
     * material usado no acabamento.
     *
     * Stevium retorna null, porque seus poderes
     * são definidos pela Matriz.
     */
    public static PowerMaterial getDirectPowerMaterial(
        ItemStack stack
    ) {
        ArmorTrim trim =
            getArmorTrim(
                stack
            );

        if (trim == null) {
            return null;
        }

        return getDirectPowerMaterial(
            trim
        );
    }

    /**
     * Converte cada material de acabamento
     * no PowerMaterial correspondente.
     */
    private static PowerMaterial getDirectPowerMaterial(
        ArmorTrim trim
    ) {
        if (
            trim.material().is(
                TrimMaterials.IRON
            )
        ) {
            return PowerMaterial.IRON;
        }

        if (
            trim.material().is(
                TrimMaterials.DIAMOND
            )
        ) {
            return PowerMaterial.DIAMOND;
        }

        if (
            trim.material().is(
                TrimMaterials.GOLD
            )
        ) {
            return PowerMaterial.GOLD;
        }

        if (
            trim.material().is(
                TrimMaterials.COPPER
            )
        ) {
            return PowerMaterial.COPPER;
        }

        if (
            trim.material().is(
                TrimMaterials.REDSTONE
            )
        ) {
            return PowerMaterial.REDSTONE;
        }

        if (
            trim.material().is(
                TrimMaterials.LAPIS
            )
        ) {
            return PowerMaterial.LAPIS_LAZULI;
        }

        if (
            trim.material().is(
                TrimMaterials.QUARTZ
            )
        ) {
            return PowerMaterial.QUARTZ;
        }

        if (
            trim.material().is(
                TrimMaterials.EMERALD
            )
        ) {
            return PowerMaterial.EMERALD;
        }

        if (
            trim.material().is(
                TrimMaterials.RESIN
            )
        ) {
            return PowerMaterial.RESIN;
        }

        if (
            trim.material().is(
                TrimMaterials.AMETHYST
            )
        ) {
            return PowerMaterial.AMETHYST;
        }

        if (
            trim.material().is(
                TrimMaterials.NETHERITE
            )
        ) {
            return PowerMaterial.NETHERITE;
        }

        return null;
    }

    /**
     * Coloca automaticamente o material correto
     * nos componentes que os poderes existentes
     * já verificam.
     *
     * Isso permite reutilizar todos os poderes
     * sem alterar seus arquivos.
     */
    private static void synchronizeDirectMaterial(
        ItemStack stack,
        PowerMaterial material
    ) {
        String materialId =
            material.getId();

        String selectedIndividualMaterial =
            stack.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        /*
         * Corrige o poder individual da peça.
         *
         * Um acabamento de ametista sempre será
         * AMETHYST, mesmo que a peça possua algum
         * valor antigo salvo.
         */
        if (
            !materialId.equals(
                selectedIndividualMaterial
            )
        ) {
            stack.set(
                ModComponents.SELECTED_POWER_MATERIAL,
                materialId
            );
        }

        String selectedSetMaterial =
            stack.getOrDefault(
                ModComponents.SELECTED_SET_POWER_MATERIAL,
                ""
            );

        /*
         * Corrige também o material usado pelo
         * sistema de poder de conjunto completo.
         */
        if (
            !materialId.equals(
                selectedSetMaterial
            )
        ) {
            stack.set(
                ModComponents.SELECTED_SET_POWER_MATERIAL,
                materialId
            );
        }
    }

    /**
     * Retorna os dados completos do acabamento
     * aplicado à peça.
     */
    public static ArmorTrim getArmorTrim(
        ItemStack stack
    ) {
        if (
            stack == null
                || stack.isEmpty()
        ) {
            return null;
        }

        return stack.get(
            DataComponents.TRIM
        );
    }
}