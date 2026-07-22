package com.marcos.tailoredtraits.mixin;

import java.util.Arrays;
import java.util.List;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.power.PowerMaterial;
import com.marcos.tailoredtraits.power.SteviumArmorUtil;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuLapisHelmetMixin {

    /*
     * 0,75 significa que o jogador precisa
     * de 75% do nível normalmente exigido.
     *
     * Isso representa uma redução de 25%.
     */
    @Unique
    private static final double
        LAPIS_HELMET_COST_MULTIPLIER =
            0.75D;

    /*
     * Contêiner interno da mesa
     * de encantamentos.
     */
    @Shadow
    @Final
    private Container enchantSlots;

    /*
     * Valores de nível mostrados nas
     * três opções da mesa.
     */
    @Shadow
    @Final
    public int[] costs;

    /*
     * Método original utilizado para gerar
     * os encantamentos de uma opção.
     */
    @Shadow
    private List<EnchantmentInstance>
        getEnchantmentList(
            RegistryAccess registryAccess,
            ItemStack itemStack,
            int option,
            int enchantmentPower
        ) {
        throw new AssertionError();
    }

    /*
     * Jogador que abriu esta mesa.
     */
    @Unique
    private Player
        tailoredTraits$enchantingPlayer;

    /*
     * Guarda os custos originais.
     *
     * Eles são usados para manter a qualidade
     * dos encantamentos mesmo que o nível
     * exigido seja reduzido.
     */
    @Unique
    private final int[]
        tailoredTraits$originalCosts =
            new int[3];

    /**
     * Salva o jogador associado ao menu.
     */
    @Inject(
        method =
            "<init>(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL")
    )
    private void tailoredTraits$storeEnchantingPlayer(
        int containerId,
        Inventory inventory,
        ContainerLevelAccess access,
        CallbackInfo info
    ) {
        tailoredTraits$enchantingPlayer =
            inventory.player;
    }

    /**
     * Limpa os custos anteriores antes
     * de o Minecraft recalculá-los.
     */
    @Inject(
        method = "slotsChanged",
        at = @At("HEAD")
    )
    private void tailoredTraits$prepareCostCalculation(
        Container container,
        CallbackInfo info
    ) {
        if (container != enchantSlots) {
            return;
        }

        Arrays.fill(
            tailoredTraits$originalCosts,
            0
        );
    }

    /**
     * Depois que a mesa calcula os valores
     * normais, aplica o desconto do capacete.
     */
    @Inject(
        method = "slotsChanged",
        at = @At("TAIL")
    )
    private void tailoredTraits$reduceEnchantingCosts(
        Container container,
        CallbackInfo info
    ) {
        if (container != enchantSlots) {
            return;
        }

        Player player =
            tailoredTraits$enchantingPlayer;

        /*
         * O cálculo real deve acontecer
         * somente no servidor.
         *
         * Depois, o servidor sincroniza
         * os valores com o cliente.
         */
        if (
            player == null
                || player.level().isClientSide()
        ) {
            return;
        }

        ItemStack helmet =
            player.getItemBySlot(
                EquipmentSlot.HEAD
            );

        boolean hasLapisHelmetPower =
            tailoredTraits$hasSelectedMaterial(
                helmet,
                PowerMaterial.LAPIS_LAZULI
            );

        if (!hasLapisHelmetPower) {
            return;
        }

        for (
            int option = 0;
            option < costs.length;
            option++
        ) {
            int originalCost =
                costs[option];

            tailoredTraits$originalCosts[option] =
                originalCost;

            /*
             * Valor zero significa que aquela
             * opção não está disponível.
             */
            if (originalCost <= 0) {
                continue;
            }

            int discountedCost =
                (int) Math.ceil(
                    originalCost
                        * LAPIS_HELMET_COST_MULTIPLIER
                );

            /*
             * A primeira opção nunca pode exigir
             * menos de 1 nível, a segunda menos
             * de 2 e a terceira menos de 3.
             */
            int minimumCost =
                option + 1;

            costs[option] =
                Math.max(
                    minimumCost,
                    discountedCost
                );
        }

        /*
         * Envia os custos reduzidos para
         * a interface do jogador.
         */
        EnchantmentMenu menu =
            (EnchantmentMenu) (Object) this;

        menu.broadcastChanges();
    }

    /**
     * A mesa usa o custo mostrado também para
     * determinar a força dos encantamentos.
     *
     * Esta correção faz a geração continuar
     * utilizando o custo original, evitando que
     * o desconto diminua a qualidade.
     */
    @Redirect(
        method = "lambda$clickMenuButton$0",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/inventory/EnchantmentMenu;"
                    + "getEnchantmentList("
                    + "Lnet/minecraft/core/RegistryAccess;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "II)"
                    + "Ljava/util/List;"
        )
    )
    private List<EnchantmentInstance>
        tailoredTraits$preserveEnchantingPower(
            EnchantmentMenu menu,
            RegistryAccess registryAccess,
            ItemStack itemStack,
            int option,
            int displayedCost
        ) {
        Player player =
            tailoredTraits$enchantingPlayer;

        int enchantmentPower =
            displayedCost;

        if (
            player != null
                && option >= 0
                && option
                    < tailoredTraits$originalCosts.length
        ) {
            ItemStack helmet =
                player.getItemBySlot(
                    EquipmentSlot.HEAD
                );

            boolean hasLapisHelmetPower =
                tailoredTraits$hasSelectedMaterial(
                    helmet,
                    PowerMaterial.LAPIS_LAZULI
                );

            int originalCost =
                tailoredTraits$originalCosts[option];

            if (
                hasLapisHelmetPower
                    && originalCost > 0
            ) {
                enchantmentPower =
                    originalCost;
            }
        }

        return getEnchantmentList(
            registryAccess,
            itemStack,
            option,
            enchantmentPower
        );
    }

    /**
     * Verifica se a peça possui acabamento
     * de Stevium e está configurada com
     * o material solicitado.
     */
    @Unique
    private static boolean
        tailoredTraits$hasSelectedMaterial(
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