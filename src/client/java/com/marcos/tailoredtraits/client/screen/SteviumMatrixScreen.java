package com.marcos.tailoredtraits.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.marcos.tailoredtraits.component.ModComponents;
import com.marcos.tailoredtraits.network.SelectFullSetPowerPayload;
import com.marcos.tailoredtraits.network.SelectIndividualPowerPayload;
import com.marcos.tailoredtraits.power.FullSetPowerUtil;
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

    private static final int
        PANEL_WIDTH =
            440;

    private static final int
        PANEL_HEIGHT =
            298;

    private static final int
        ARMOR_BUTTON_WIDTH =
            78;

    private static final int
        ARMOR_BUTTON_HEIGHT =
            24;

    private static final int
        ARMOR_BUTTON_GAP =
            8;

    private static final int
        MATERIAL_BUTTON_WIDTH =
            78;

    private static final int
        MATERIAL_BUTTON_HEIGHT =
            20;

    private static final int
        MATERIAL_BUTTON_GAP_X =
            6;

    private static final int
        MATERIAL_BUTTON_GAP_Y =
            3;

    /*
     * Peça individual selecionada.
     *
     * Fica null durante a edição
     * do conjunto completo.
     */
    private EquipmentSlot
        selectedSlot;

    /*
     * Indica que o jogador selecionou
     * o botão de conjunto completo.
     */
    private boolean
        fullSetMode;

    /*
     * Valores individuais já salvos
     * nas armaduras.
     */
    private final Map<
        EquipmentSlot,
        PowerMaterial
    > savedSelections =
        new EnumMap<>(
            EquipmentSlot.class
        );

    /*
     * Valores individuais escolhidos
     * no menu antes da confirmação.
     */
    private final Map<
        EquipmentSlot,
        PowerMaterial
    > temporarySelections =
        new EnumMap<>(
            EquipmentSlot.class
        );

    /*
     * Material de conjunto atualmente
     * salvo nas quatro peças.
     */
    private PowerMaterial
        savedFullSetSelection;

    /*
     * Material de conjunto selecionado
     * temporariamente no menu.
     */
    private PowerMaterial
        temporaryFullSetSelection;

    private final List<MaterialButtonEntry>
        materialButtons =
            new ArrayList<>();

    private Button
        confirmButton;

    private boolean
        helmetUnlocked;

    private boolean
        chestplateUnlocked;

    private boolean
        leggingsUnlocked;

    private boolean
        bootsUnlocked;

    private boolean
        fullSetUnlocked;

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
            (
                this.width
                    - PANEL_WIDTH
            ) / 2;

        int panelTop =
            (
                this.height
                    - PANEL_HEIGHT
            ) / 2;

        createSelectionButtons(
            panelLeft,
            panelTop
        );

        createMaterialButtons(
            panelLeft,
            panelTop
        );

        /*
         * Botão que envia a alteração
         * escolhida ao servidor.
         */
        confirmButton =
            Button.builder(
                Component.translatable(
                    "screen.tailored-traits.confirm"
                ),
                button ->
                    confirmSelection()
            ).bounds(
                panelLeft
                    + (
                        PANEL_WIDTH
                            - 110
                    ) / 2,
                panelTop + 182,
                110,
                20
            ).build();

        confirmButton.active =
            false;

        this.addRenderableWidget(
            confirmButton
        );

        /*
         * Botão para fechar o menu.
         */
        this.addRenderableWidget(
            Button.builder(
                Component.translatable(
                    "gui.done"
                ),
                button ->
                    this.onClose()
            ).bounds(
                panelLeft
                    + (
                        PANEL_WIDTH
                            - 90
                    ) / 2,
                panelTop + 268,
                90,
                20
            ).build()
        );

        updateMaterialButtonVisibility();
        updateConfirmButton();
    }

    /**
     * Cria os quatro botões individuais
     * e o botão de conjunto completo.
     */
    private void createSelectionButtons(
        int panelLeft,
        int panelTop
    ) {
        int buttonCount =
            5;

        int totalWidth =
            ARMOR_BUTTON_WIDTH
                * buttonCount
                + ARMOR_BUTTON_GAP
                    * (
                        buttonCount - 1
                    );

        int firstButtonX =
            panelLeft
                + (
                    PANEL_WIDTH
                        - totalWidth
                ) / 2;

        int buttonY =
            panelTop + 30;

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

        addFullSetButton(
            firstButtonX
                + (
                    ARMOR_BUTTON_WIDTH
                        + ARMOR_BUTTON_GAP
                ) * 4,
            buttonY
        );
    }

    /**
     * Cria um botão para uma peça
     * individual da armadura.
     */
    private void addArmorButton(
        int x,
        int y,
        EquipmentSlot slot,
        Component name,
        boolean unlocked
    ) {
        Button button =
            Button.builder(
                name,
                pressedButton ->
                    selectArmorPart(
                        slot
                    )
            ).bounds(
                x,
                y,
                ARMOR_BUTTON_WIDTH,
                ARMOR_BUTTON_HEIGHT
            ).build();

        button.active =
            unlocked;

        this.addRenderableWidget(
            button
        );
    }

    /**
     * Cria o botão de conjunto.
     */
    private void addFullSetButton(
        int x,
        int y
    ) {
        Button button =
            Button.builder(
                Component.literal(
                    "Conjunto"
                ),
                pressedButton ->
                    selectFullSet()
            ).bounds(
                x,
                y,
                ARMOR_BUTTON_WIDTH,
                ARMOR_BUTTON_HEIGHT
            ).build();

        /*
         * O botão somente funciona quando
         * as quatro peças possuem Stevium.
         */
        button.active =
            fullSetUnlocked;

        this.addRenderableWidget(
            button
        );
    }

    /**
     * Cria os botões dos materiais.
     *
     * Todos os materiais são criados,
     * incluindo Stevium.
     *
     * A opção Stevium será escondida
     * durante seleções individuais.
     */
    private void createMaterialButtons(
        int panelLeft,
        int panelTop
    ) {
        int columnCount =
            4;

        int totalWidth =
            MATERIAL_BUTTON_WIDTH
                * columnCount
                + MATERIAL_BUTTON_GAP_X
                    * (
                        columnCount - 1
                    );

        int firstButtonX =
            panelLeft
                + (
                    PANEL_WIDTH
                        - totalWidth
                ) / 2;

        int firstButtonY =
            panelTop + 94;

        int materialIndex =
            0;

        for (
            PowerMaterial material :
            PowerMaterial.values()
        ) {
            int column =
                materialIndex
                    % columnCount;

            int row =
                materialIndex
                    / columnCount;

            int buttonX =
                firstButtonX
                    + column
                        * (
                            MATERIAL_BUTTON_WIDTH
                                + MATERIAL_BUTTON_GAP_X
                        );

            int buttonY =
                firstButtonY
                    + row
                        * (
                            MATERIAL_BUTTON_HEIGHT
                                + MATERIAL_BUTTON_GAP_Y
                        );

            Button button =
                Button.builder(
                    material.getDisplayName(),
                    pressedButton ->
                        selectMaterial(
                            material
                        )
                ).bounds(
                    buttonX,
                    buttonY,
                    MATERIAL_BUTTON_WIDTH,
                    MATERIAL_BUTTON_HEIGHT
                ).build();

            this.addRenderableWidget(
                button
            );

            materialButtons.add(
                new MaterialButtonEntry(
                    button,
                    material
                )
            );

            materialIndex++;
        }
    }

    /**
     * Seleciona uma peça individual.
     */
    private void selectArmorPart(
        EquipmentSlot slot
    ) {
        if (
            !isSlotUnlocked(
                slot
            )
        ) {
            return;
        }

        selectedSlot =
            slot;

        fullSetMode =
            false;

        updateMaterialButtonVisibility();
        updateConfirmButton();
    }

    /**
     * Seleciona a edição do
     * conjunto completo.
     */
    private void selectFullSet() {
        if (!fullSetUnlocked) {
            return;
        }

        selectedSlot =
            null;

        fullSetMode =
            true;

        updateMaterialButtonVisibility();
        updateConfirmButton();
    }

    /**
     * Guarda temporariamente o material
     * escolhido no menu.
     */
    private void selectMaterial(
        PowerMaterial material
    ) {
        if (fullSetMode) {
            temporaryFullSetSelection =
                material;

            updateConfirmButton();

            return;
        }

        if (selectedSlot == null) {
            return;
        }

        /*
         * Stevium não pode ser escolhido
         * para uma peça individual.
         */
        if (
            !material.isIndividualOption()
        ) {
            return;
        }

        temporarySelections.put(
            selectedSlot,
            material
        );

        updateConfirmButton();
    }

    /**
     * Envia a seleção atual ao servidor.
     */
    private void confirmSelection() {
        if (fullSetMode) {
            confirmFullSetSelection();

            return;
        }

        confirmIndividualSelection();
    }

    /**
     * Confirma uma seleção individual.
     */
    private void confirmIndividualSelection() {
        if (selectedSlot == null) {
            return;
        }

        PowerMaterial material =
            temporarySelections.get(
                selectedSlot
            );

        if (
            material == null
                || !material
                    .isIndividualOption()
        ) {
            return;
        }

        ClientPlayNetworking.send(
            new SelectIndividualPowerPayload(
                selectedSlot,
                material.getId()
            )
        );

        this.onClose();
    }

    /**
     * Confirma a seleção do conjunto.
     */
    private void confirmFullSetSelection() {
        if (!fullSetUnlocked) {
            return;
        }

        PowerMaterial material =
            temporaryFullSetSelection;

        if (material == null) {
            return;
        }

        ClientPlayNetworking.send(
            new SelectFullSetPowerPayload(
                material.getId()
            )
        );

        this.onClose();
    }

    /**
     * Atualiza o estado do botão
     * de confirmação.
     */
    private void updateConfirmButton() {
        if (confirmButton == null) {
            return;
        }

        if (fullSetMode) {
            confirmButton.active =
                fullSetUnlocked
                    && temporaryFullSetSelection
                        != null
                    && temporaryFullSetSelection
                        != savedFullSetSelection;

            return;
        }

        if (selectedSlot == null) {
            confirmButton.active =
                false;

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

    /**
     * Mostra apenas os materiais válidos
     * para a seleção atual.
     */
    private void updateMaterialButtonVisibility() {
        boolean hasSelectedTarget =
            fullSetMode
                || selectedSlot != null;

        for (
            MaterialButtonEntry entry :
            materialButtons
        ) {
            boolean materialAllowed =
                fullSetMode
                    || entry.material()
                        .isIndividualOption();

            boolean shouldShow =
                hasSelectedTarget
                    && materialAllowed;

            entry.button().visible =
                shouldShow;

            entry.button().active =
                shouldShow;
        }
    }

    /**
     * Lê os materiais individuais
     * já gravados nas peças equipadas.
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

        loadSavedFullSetSelection();
    }

    /**
     * Lê a seleção individual de uma peça.
     */
    private void loadSavedSelection(
        EquipmentSlot slot
    ) {
        if (
            this.minecraft == null
                || this.minecraft.player == null
                || !isSlotUnlocked(
                    slot
                )
        ) {
            return;
        }

        ItemStack stack =
            this.minecraft.player
                .getItemBySlot(
                    slot
                );

        String materialId =
            stack.getOrDefault(
                ModComponents.SELECTED_POWER_MATERIAL,
                ""
            );

        PowerMaterial material =
            PowerMaterial.fromId(
                materialId
            );

        if (
            material == null
                || !material
                    .isIndividualOption()
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

    /**
     * Lê o poder de conjunto ativo
     * nas quatro peças.
     */
    private void loadSavedFullSetSelection() {
        savedFullSetSelection =
            null;

        temporaryFullSetSelection =
            null;

        if (
            this.minecraft == null
                || this.minecraft.player == null
                || !fullSetUnlocked
        ) {
            return;
        }

        savedFullSetSelection =
            FullSetPowerUtil
                .getActiveFullSetMaterial(
                    this.minecraft.player
                );

        temporaryFullSetSelection =
            savedFullSetSelection;
    }

    /**
     * Verifica se uma peça individual
     * está desbloqueada.
     */
    private boolean isSlotUnlocked(
        EquipmentSlot slot
    ) {
        return switch (slot) {
            case HEAD ->
                helmetUnlocked;

            case CHEST ->
                chestplateUnlocked;

            case LEGS ->
                leggingsUnlocked;

            case FEET ->
                bootsUnlocked;

            default ->
                false;
        };
    }

    /**
     * Atualiza quais seleções estão
     * disponíveis no menu.
     */
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

    /**
     * Verifica o acabamento de uma
     * peça equipada.
     */
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
                .getItemBySlot(
                    slot
                );

        return SteviumArmorUtil
            .hasSteviumTrim(
                stack
            );
    }

    /**
     * Retorna o nome da seleção atual.
     */
    private Component getSelectedTargetName() {
        if (fullSetMode) {
            return Component.literal(
                "Conjunto completo"
            );
        }

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

    /**
     * Retorna o nome do material
     * atualmente selecionado no menu.
     */
    private Component getSelectedMaterialName() {
        if (fullSetMode) {
            if (
                temporaryFullSetSelection
                    == null
            ) {
                return Component.translatable(
                    "screen.tailored-traits.none_selected"
                );
            }

            return temporaryFullSetSelection
                .getDisplayName();
        }

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

    /**
     * Retorna o nome do poder de conjunto
     * que já está salvo.
     */
    private Component
        getSavedFullSetMaterialName() {
        if (
            savedFullSetSelection
                == null
        ) {
            return Component.literal(
                "Nenhum"
            );
        }

        return savedFullSetSelection
            .getDisplayName();
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        int panelLeft =
            (
                this.width
                    - PANEL_WIDTH
            ) / 2;

        int panelTop =
            (
                this.height
                    - PANEL_HEIGHT
            ) / 2;

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
                getSelectedTargetName()
            ),
            this.width / 2,
            panelTop + 62,
            0xFFD7C7FF
        );

        boolean hasSelectedTarget =
            fullSetMode
                || selectedSlot != null;

        if (!hasSelectedTarget) {
            graphics.centeredText(
                this.font,
                Component.literal(
                    "Escolha uma peça ou o conjunto completo."
                ),
                this.width / 2,
                panelTop + 110,
                0xFFAAAAAA
            );
        } else {
            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.choose_material"
                ),
                this.width / 2,
                panelTop + 78,
                0xFFFFFFFF
            );

            graphics.centeredText(
                this.font,
                Component.translatable(
                    "screen.tailored-traits.selected_material",
                    getSelectedMaterialName()
                ),
                this.width / 2,
                panelTop + 166,
                0xFFFFD37F
            );

            Component costMessage =
                fullSetMode
                    ? Component.literal(
                        "Custo: 5 níveis de experiência"
                    )
                    : Component.translatable(
                        "screen.tailored-traits.individual_cost"
                    );

            graphics.centeredText(
                this.font,
                costMessage,
                this.width / 2,
                panelTop + 207,
                0xFFAAAAAA
            );
        }

        graphics.fill(
            panelLeft + 18,
            panelTop + 219,
            panelLeft + PANEL_WIDTH - 18,
            panelTop + 220,
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
            panelTop + 228,
            fullSetUnlocked
                ? 0xFF77FF99
                : 0xFFFF7777
        );

        /*
         * Mostra qual material de conjunto
         * está atualmente salvo.
         */
        if (fullSetUnlocked) {
            graphics.centeredText(
                this.font,
                Component.literal(
                    "Poder de conjunto atual: "
                ).append(
                    getSavedFullSetMaterialName()
                ),
                this.width / 2,
                panelTop + 243,
                0xFFFFD37F
            );
        }

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