package com.marcos.tailoredtraits.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.network.SelectIndividualPowerPayload;
import com.marcos.tailoredtraits.power.PowerMaterial;
import com.marcos.tailoredtraits.power.SteviumArmorUtil;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class SteviumMatrixScreen extends Screen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 276;

    private static final int ARMOR_BUTTON_WIDTH = 78;
    private static final int ARMOR_BUTTON_HEIGHT = 24;
    private static final int ARMOR_BUTTON_GAP = 8;

    private static final int MATERIAL_BUTTON_WIDTH = 78;
    private static final int MATERIAL_BUTTON_HEIGHT = 20;
    private static final int MATERIAL_BUTTON_GAP_X = 6;
    private static final int MATERIAL_BUTTON_GAP_Y = 3;

    private EquipmentSlot selectedSlot;

    /*
     * Valores já salvos nas armaduras.
     */
    private final Map<
        EquipmentSlot,
        PowerMaterial
    > savedSelections =
        new EnumMap<>(EquipmentSlot.class);

    /*
     * Valores escolhidos no menu antes da confirmação.
     */
    private final Map<
        EquipmentSlot,
        PowerMaterial
    > temporarySelections =
        new EnumMap<>(EquipmentSlot.class);

    private final List<MaterialButtonEntry>
        materialButtons =
            new ArrayList<>();

    private Button confirmButton;

    private boolean helmetUnlocked;
    private boolean chestplateUnlocked;
    private boolean leggingsUnlocked;
    private boolean bootsUnlocked;
    private boolean fullSetUnlocked;

    public SteviumMatrixScreen() {
        super(
            Component.translatable(
                "screen.tailored-traits.stevium_matrix"
            )
        );
    }

    @Override
    protected void init() {
        updateUnlockedSlots();
        loadSavedSelections();

        materialButtons.clear();

        int panelLeft =
            (this.width - PANEL_WIDTH) / 2;

        int panelTop =
            (this.height - PANEL_HEIGHT) / 2;

        createArmorButtons(
            panelLeft,
            panelTop
        );

        createMaterialButtons(
            panelLeft,
            panelTop
        );

        /*
         * Botão que envia a alteração ao servidor.
         */
        confirmButton = Button.builder(
            Component.translatable(
                "screen.tailored-traits.confirm"
            ),
            button -> confirmSelection()
        ).bounds(
            panelLeft + (PANEL_WIDTH - 110) / 2,
            panelTop + 180,
            110,
            20
        ).build();

        confirmButton.active = false;

        this.addRenderableWidget(confirmButton);

        /*
         * Botão para fechar o menu.
         */
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("gui.done"),
                button -> this.onClose()
            ).bounds(
                panelLeft + (PANEL_WIDTH - 90) / 2,
                panelTop + 248,
                90,
                20
            ).build()
        );

        updateMaterialButtonVisibility();
        updateConfirmButton();
    }

    private void createArmorButtons(
        int panelLeft,
        int panelTop
    ) {
        int totalWidth =
            ARMOR_BUTTON_WIDTH * 4
            + ARMOR_BUTTON_GAP * 3;

        int firstButtonX =
            panelLeft
            + (PANEL_WIDTH - totalWidth) / 2;

        int buttonY = panelTop + 30;

        addArmorButton(
            firstButtonX,
            buttonY,
            EquipmentSlot.HEAD,
            Component.translatable(
                "screen.tailored-traits.helmet"
            ),
            helmetUnlocked
        );

        addArmorButton(
            firstButtonX
                + ARMOR_BUTTON_WIDTH
                + ARMOR_BUTTON_GAP,
            buttonY,
            EquipmentSlot.CHEST,
            Component.translatable(
                "screen.tailored-traits.chestplate"
            ),
            chestplateUnlocked
        );

        addArmorButton(
            firstButtonX
                + (
                    ARMOR_BUTTON_WIDTH
                    + ARMOR_BUTTON_GAP
                ) * 2,
            buttonY,
            EquipmentSlot.LEGS,
            Component.translatable(
                "screen.tailored-traits.leggings"
            ),
            leggingsUnlocked
        );

        addArmorButton(
            firstButtonX
                + (
                    ARMOR_BUTTON_WIDTH
                    + ARMOR_BUTTON_GAP
                ) * 3,
            buttonY,
            EquipmentSlot.FEET,
            Component.translatable(
                "screen.tailored-traits.boots"
            ),
            bootsUnlocked
        );
    }

    private void addArmorButton(
        int x,
        int y,
        EquipmentSlot slot,
        Component name,
        boolean unlocked
    ) {
        Button button = Button.builder(
            name,
            pressedButton ->
                selectArmorPart(slot)
        ).bounds(
            x,
            y,
            ARMOR_BUTTON_WIDTH,
            ARMOR_BUTTON_HEIGHT
        ).build();

        button.active = unlocked;

        this.addRenderableWidget(button);
    }

    private void createMaterialButtons(
        int panelLeft,
        int panelTop
    ) {
        int columnCount = 4;

        int totalWidth =
            MATERIAL_BUTTON_WIDTH * columnCount
            + MATERIAL_BUTTON_GAP_X
                * (columnCount - 1);

        int firstButtonX =
            panelLeft
            + (PANEL_WIDTH - totalWidth) / 2;

        int firstButtonY = panelTop + 92;

        int materialIndex = 0;

        for (
            PowerMaterial material :
            PowerMaterial.values()
        ) {
            if (!material.isIndividualOption()) {
                continue;
            }

            int column =
                materialIndex % columnCount;

            int row =
                materialIndex / columnCount;

            int buttonX =
                firstButtonX
                + column * (
                    MATERIAL_BUTTON_WIDTH
                    + MATERIAL_BUTTON_GAP_X
                );

            int buttonY =
                firstButtonY
                + row * (
                    MATERIAL_BUTTON_HEIGHT
                    + MATERIAL_BUTTON_GAP_Y
                );

            Button button = Button.builder(
                material.getDisplayName(),
                pressedButton ->
                    selectMaterial(material)
            ).bounds(
                buttonX,
                buttonY,
                MATERIAL_BUTTON_WIDTH,
                MATERIAL_BUTTON_HEIGHT
            ).build();

            this.addRenderableWidget(button);

            materialButtons.add(
                new MaterialButtonEntry(
                    button,
                    material
                )
            );

            materialIndex++;
        }
    }

    private void selectArmorPart(
        EquipmentSlot slot
    ) {
        if (!isSlotUnlocked(slot)) {
            return;
        }

        selectedSlot = slot;

        updateMaterialButtonVisibility();
        updateConfirmButton();
    }

    private void selectMaterial(
        PowerMaterial material
    ) {
        if (selectedSlot == null) {
            return;
        }

        temporarySelections.put(
            selectedSlot,
            material
        );

        updateConfirmButton();
    }

    private void confirmSelection() {
        if (selectedSlot == null) {
            return;
        }

        PowerMaterial material =
            temporarySelections.get(
                selectedSlot
            );

        if (
            material == null
            || !material.isIndividualOption()
        ) {
            return;
        }

        /*
         * Envia a peça e o material ao servidor.
         */
        ClientPlayNetworking.send(
            new SelectIndividualPowerPayload(
                selectedSlot,
                material.getId()
            )
        );

        /*
         * Fecha o menu.
         *
         * A mensagem do servidor informará
         * se a alteração foi aceita.
         */
        this.onClose();
    }

    private void updateConfirmButton() {
        if (confirmButton == null) {
            return;
        }

        if (selectedSlot == null) {
            confirmButton.active = false;
            return;
        }

        PowerMaterial temporary =
            temporarySelections.get(
                selectedSlot
            );

        PowerMaterial saved =
            savedSelections.get(
                selectedSlot
            );

        confirmButton.active =
            temporary != null
            && temporary != saved;
    }

    private void updateMaterialButtonVisibility() {
        boolean shouldShow =
            selectedSlot != null;

        for (
            MaterialButtonEntry entry :
            materialButtons
        ) {
            entry.button().visible = shouldShow;
            entry.button().active = shouldShow;
        }
    }

    /**
     * Lê os materiais que já estavam gravados
     * nas peças equipadas.
     */
    private void loadSavedSelections() {
        savedSelections.clear();
        temporarySelections.clear();

        loadSavedSelection(
            EquipmentSlot.HEAD
        );

        loadSavedSelection(
            EquipmentSlot.CHEST
        );

        loadSavedSelection(
            EquipmentSlot.LEGS
        );

        loadSavedSelection(
            EquipmentSlot.FEET
        );
    }

    private void loadSavedSelection(
        EquipmentSlot slot
    ) {
        if (
            this.minecraft == null
            || this.minecraft.player == null
            || !isSlotUnlocked(slot)
        ) {
            return;
        }

        ItemStack stack =
            this.minecraft.player
                .getItemBySlot(slot);

        String materialId =
            stack.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        PowerMaterial material =
            PowerMaterial.fromId(materialId);

        if (
            material == null
            || !material.isIndividualOption()
        ) {
            return;
        }

        savedSelections.put(
            slot,
            material
        );

        temporarySelections.put(
            slot,
            material
        );
    }

    private boolean isSlotUnlocked(
        EquipmentSlot slot
    ) {
        return switch (slot) {
            case HEAD -> helmetUnlocked;
            case CHEST -> chestplateUnlocked;
            case LEGS -> leggingsUnlocked;
            case FEET -> bootsUnlocked;
            default -> false;
        };
    }

    private void updateUnlockedSlots() {
        helmetUnlocked =
            hasSteviumTrim(
                EquipmentSlot.HEAD
            );

        chestplateUnlocked =
            hasSteviumTrim(
                EquipmentSlot.CHEST
            );

        leggingsUnlocked =
            hasSteviumTrim(
                EquipmentSlot.LEGS
            );

        bootsUnlocked =
            hasSteviumTrim(
                EquipmentSlot.FEET
            );

        fullSetUnlocked =
            helmetUnlocked
            && chestplateUnlocked
            && leggingsUnlocked
            && bootsUnlocked;
    }

    private boolean hasSteviumTrim(
        EquipmentSlot slot
    ) {
        if (
            this.minecraft == null
            || this.minecraft.player == null
        ) {
            return false;
        }

        ItemStack stack =
            this.minecraft.player
                .getItemBySlot(slot);

        return SteviumArmorUtil
            .hasSteviumTrim(stack);
    }

    private Component getSelectedSlotName() {
        if (selectedSlot == null) {
            return Component.translatable(
                "screen.tailored-traits.none_selected"
            );
        }

        return switch (selectedSlot) {
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
                Component.translatable(
                    "screen.tailored-traits.none_selected"
                );
        };
    }

    private Component getSelectedMaterialName() {
        if (selectedSlot == null) {
            return Component.translatable(
                "screen.tailored-traits.none_selected"
            );
        }

        PowerMaterial material =
            temporarySelections.get(
                selectedSlot
            );

        if (material == null) {
            return Component.translatable(
                "screen.tailored-traits.none_selected"
            );
        }

        return material.getDisplayName();
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        int panelLeft =
            (this.width - PANEL_WIDTH) / 2;

        int panelTop =
            (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(
            panelLeft,
            panelTop,
            panelLeft + PANEL_WIDTH,
            panelTop + PANEL_HEIGHT,
            0xE61A1A24
        );

        graphics.outline(
            panelLeft,
            panelTop,
            PANEL_WIDTH,
            PANEL_HEIGHT,
            0xFFB978FF
        );

        graphics.centeredText(
            this.font,
            this.title,
            this.width / 2,
            panelTop + 10,
            0xFFFFFFFF
        );

        graphics.centeredText(
            this.font,
            Component.translatable(
                "screen.tailored-traits.selected",
                getSelectedSlotName()
            ),
            this.width / 2,
            panelTop + 61,
            0xFFD7C7FF
        );

        if (selectedSlot == null) {
            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.click_piece"
                ),
                this.width / 2,
                panelTop + 108,
                0xFFAAAAAA
            );
        } else {
            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.choose_material"
                ),
                this.width / 2,
                panelTop + 77,
                0xFFFFFFFF
            );

            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.selected_material",
                    getSelectedMaterialName()
                ),
                this.width / 2,
                panelTop + 164,
                0xFFFFD37F
            );

            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.individual_cost"
                ),
                this.width / 2,
                panelTop + 204,
                0xFFAAAAAA
            );
        }

        graphics.fill(
            panelLeft + 18,
            panelTop + 216,
            panelLeft + PANEL_WIDTH - 18,
            panelTop + 217,
            0xFF765A91
        );

        Component fullPowerMessage =
            Component.translatable(
                fullSetUnlocked
                    ? "screen.tailored-traits.full_power_unlocked"
                    : "screen.tailored-traits.full_power_locked"
            );

        graphics.centeredText(
            this.font,
            fullPowerMessage,
            this.width / 2,
            panelTop + 225,
            fullSetUnlocked
                ? 0xFF77FF99
                : 0xFFFF7777
        );

        super.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            partialTick
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MaterialButtonEntry(
        Button button,
        PowerMaterial material
    ) {
    }
}