package com.marcos.tailoredtraits.power;

import net.minecraft.network.chat.Component;

public enum PowerMaterial {

    IRON("iron"),
    DIAMOND("diamond"),
    GOLD("gold"),
    COPPER("copper"),
    REDSTONE("redstone"),
    LAPIS_LAZULI("lapis_lazuli"),
    QUARTZ("quartz"),
    EMERALD("emerald"),
    RESIN("resin"),
    AMETHYST("amethyst"),
    NETHERITE("netherite"),
    STEVIUM("stevium");

    private final String id;

    PowerMaterial(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(
            "power_material.tailored-traits." + id
        );
    }

    /*
     * Stevium só aparecerá como poder completo.
     */
    public boolean isIndividualOption() {
        return this != STEVIUM;
    }

    /**
     * Converte uma String salva na armadura
     * novamente em um PowerMaterial.
     */
    public static PowerMaterial fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        for (PowerMaterial material : values()) {
            if (material.id.equals(id)) {
                return material;
            }
        }

        return null;
    }
}