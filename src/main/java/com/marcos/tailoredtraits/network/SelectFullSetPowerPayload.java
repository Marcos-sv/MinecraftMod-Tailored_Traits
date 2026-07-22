package com.marcos.tailoredtraits.network;

import com.marcos.tailoredtraits.TailoredTraits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SelectFullSetPowerPayload(
    String materialId
) implements CustomPacketPayload {

    /*
     * Identificador do pacote enviado
     * pelo cliente ao servidor.
     */
    public static final Type<
        SelectFullSetPowerPayload
    > TYPE =
        new Type<>(
            TailoredTraits.id(
                "select_full_set_power"
            )
        );

    /*
     * Formato utilizado para enviar
     * o ID do material pela rede.
     */
    public static final StreamCodec<
        RegistryFriendlyByteBuf,
        SelectFullSetPowerPayload
    > CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SelectFullSetPowerPayload::materialId,

            SelectFullSetPowerPayload::new
        );

    @Override
    public Type<
        ? extends CustomPacketPayload
    > type() {
        return TYPE;
    }
}