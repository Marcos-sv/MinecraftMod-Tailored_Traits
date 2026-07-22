package com.marcos.tailoredtraits.network;

import com.marcos.tailoredtraits.TailoredTraits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ActivateFullSetAbilityPayload(
    boolean activated
) implements CustomPacketPayload {

    /*
     * Identificador do pacote enviado
     * quando o jogador pressiona a tecla
     * da habilidade de conjunto.
     */
    public static final Type<
        ActivateFullSetAbilityPayload
    > TYPE =
        new Type<>(
            TailoredTraits.id(
                "activate_full_set_ability"
            )
        );

    /*
     * Formato do pacote.
     *
     * O booleano serve apenas para indicar
     * que a tecla foi pressionada.
     */
    public static final StreamCodec<
        RegistryFriendlyByteBuf,
        ActivateFullSetAbilityPayload
    > CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ActivateFullSetAbilityPayload::activated,

            ActivateFullSetAbilityPayload::new
        );

    @Override
    public Type<
        ? extends CustomPacketPayload
    > type() {
        return TYPE;
    }
}