package com.marcos.tailoredtraits.client.mixin;

import java.util.ArrayDeque;
import java.util.Deque;

import com.marcos.tailoredtraits.trim.ModTrimMaterials;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    /*
     * Cores usadas pelo ciclo do Stevium:
     *
     * ferro;
     * quartzo;
     * ouro;
     * cobre;
     * redstone;
     * esmeralda;
     * diamante;
     * lápis-lazúli;
     * ametista;
     * resina;
     * netherite.
     */
    @Unique
    private static final int[]
        TAILORED_TRAITS$STEVIUM_COLORS = {
            0xFFECECEC,
            0xFFE3D4C4,
            0xFFDEB12D,
            0xFFB4684D,
            0xFF971607,
            0xFF11A036,
            0xFF6EECD2,
            0xFF416E97,
            0xFF9A5CC6,
            0xFFFC7812,
            0xFF625859
        };

    /*
     * Duração da transição entre
     * cada par de cores.
     */
    @Unique
    private static final double
        TAILORED_TRAITS$SECONDS_PER_COLOR =
            1.20D;

    /*
     * Guarda temporariamente a armadura
     * que está sendo renderizada.
     *
     * Uma pilha é usada para evitar problemas
     * caso uma renderização aconteça dentro
     * de outra renderização.
     */
    @Unique
    private static final ThreadLocal<
        Deque<ItemStack>
    > TAILORED_TRAITS$RENDERING_STACKS =
        ThreadLocal.withInitial(
            ArrayDeque::new
        );

    /*
     * Antes de renderizar as camadas,
     * guarda a armadura atual.
     */
    @Inject(
        method =
            "renderLayers("
                + "Lnet/minecraft/client/resources/model/"
                + "EquipmentClientInfo$LayerType;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/client/model/Model;"
                + "Ljava/lang/Object;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/"
                + "SubmitNodeCollector;"
                + "I"
                + "Lnet/minecraft/resources/Identifier;"
                + "II"
                + ")V",
        at = @At("HEAD")
    )
    private void tailoredTraits$storeRenderingStack(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAsset,
        Model<?> model,
        Object renderState,
        ItemStack armorStack,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier playerTexture,
        int outlineColor,
        int renderOrder,
        CallbackInfo callbackInfo
    ) {
        TAILORED_TRAITS$RENDERING_STACKS
            .get()
            .push(
                armorStack
            );
    }

    /*
     * Depois que a renderização termina,
     * remove a armadura da pilha.
     */
    @Inject(
        method =
            "renderLayers("
                + "Lnet/minecraft/client/resources/model/"
                + "EquipmentClientInfo$LayerType;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/client/model/Model;"
                + "Ljava/lang/Object;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/"
                + "SubmitNodeCollector;"
                + "I"
                + "Lnet/minecraft/resources/Identifier;"
                + "II"
                + ")V",
        at = @At("RETURN")
    )
    private void tailoredTraits$removeRenderingStack(
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> equipmentAsset,
        Model<?> model,
        Object renderState,
        ItemStack armorStack,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier playerTexture,
        int outlineColor,
        int renderOrder,
        CallbackInfo callbackInfo
    ) {
        Deque<ItemStack> renderingStacks =
            TAILORED_TRAITS$RENDERING_STACKS
                .get();

        if (!renderingStacks.isEmpty()) {
            renderingStacks.pop();
        }

        if (renderingStacks.isEmpty()) {
            TAILORED_TRAITS$RENDERING_STACKS
                .remove();
        }
    }

    /*
     * Modifica o terceiro argumento inteiro
     * da chamada responsável pelo enfeite.
     *
     * O ordinal 2 aponta para a terceira
     * chamada de submitModel, que é justamente
     * a renderização do ArmorTrim.
     */
    @ModifyArg(
        method =
            "renderLayers("
                + "Lnet/minecraft/client/resources/model/"
                + "EquipmentClientInfo$LayerType;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/client/model/Model;"
                + "Ljava/lang/Object;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/"
                + "SubmitNodeCollector;"
                + "I"
                + "Lnet/minecraft/resources/Identifier;"
                + "II"
                + ")V",
        at = @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/client/renderer/"
                    + "OrderedSubmitNodeCollector;"
                    + "submitModel("
                    + "Lnet/minecraft/client/model/Model;"
                    + "Ljava/lang/Object;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/"
                    + "rendertype/RenderType;"
                    + "III"
                    + "Lnet/minecraft/client/renderer/"
                    + "texture/TextureAtlasSprite;"
                    + "I"
                    + "Lnet/minecraft/client/renderer/"
                    + "feature/ModelFeatureRenderer"
                    + "$CrumblingOverlay;"
                    + ")V",
            ordinal = 2
        ),
        index = 6
    )
    private int tailoredTraits$animateSteviumTrim(
        int originalColor
    ) {
        Deque<ItemStack> renderingStacks =
            TAILORED_TRAITS$RENDERING_STACKS
                .get();

        if (renderingStacks.isEmpty()) {
            return originalColor;
        }

        ItemStack armorStack =
            renderingStacks.peek();

        ArmorTrim trim =
            armorStack.get(
                DataComponents.TRIM
            );

        /*
         * Somente o material Stevium
         * recebe a animação.
         */
        if (
            trim == null
                || !trim.material().is(
                    ModTrimMaterials.STEVIUM
                )
        ) {
            return originalColor;
        }

        return tailoredTraits$getAnimatedColor();
    }

    /*
     * Calcula a cor atual do ciclo.
     */
    @Unique
    private static int
        tailoredTraits$getAnimatedColor() {

        double elapsedSeconds =
            System.nanoTime()
                / 1_000_000_000.0D;

        double palettePosition =
            elapsedSeconds
                / TAILORED_TRAITS$SECONDS_PER_COLOR;

        int baseIndex =
            (int) Math.floor(
                palettePosition
            );

        double rawProgress =
            palettePosition
                - Math.floor(
                    palettePosition
                );

        /*
         * Smoothstep.
         *
         * Suaviza tanto o começo quanto
         * o fim de cada transição.
         */
        double smoothProgress =
            rawProgress
                * rawProgress
                * (
                    3.0D
                        - 2.0D
                        * rawProgress
                );

        int firstColor =
            TAILORED_TRAITS$STEVIUM_COLORS[
                Math.floorMod(
                    baseIndex,
                    TAILORED_TRAITS$STEVIUM_COLORS.length
                )
            ];

        int secondColor =
            TAILORED_TRAITS$STEVIUM_COLORS[
                Math.floorMod(
                    baseIndex + 1,
                    TAILORED_TRAITS$STEVIUM_COLORS.length
                )
            ];

        int red =
            tailoredTraits$interpolateChannel(
                (firstColor >> 16) & 0xFF,
                (secondColor >> 16) & 0xFF,
                smoothProgress
            );

        int green =
            tailoredTraits$interpolateChannel(
                (firstColor >> 8) & 0xFF,
                (secondColor >> 8) & 0xFF,
                smoothProgress
            );

        int blue =
            tailoredTraits$interpolateChannel(
                firstColor & 0xFF,
                secondColor & 0xFF,
                smoothProgress
            );

        return 0xFF000000
            | (red << 16)
            | (green << 8)
            | blue;
    }

    /*
     * Interpola um canal RGB
     * entre duas cores.
     */
    @Unique
    private static int
        tailoredTraits$interpolateChannel(
            int start,
            int end,
            double progress
        ) {
        return (int) Math.round(
            start
                + (
                    end - start
                ) * progress
        );
    }
}