package com.marcos.tailoredtraits.network;

import com.marcos.tailoredtraits.TailoredTraits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EquipmentSlot;

public record SelectIndividualPowerPayload(
    EquipmentSlot slot,
    String materialId
) implements CustomPacketPayload {

    public static final Type<
        SelectIndividualPowerPayload
    > TYPE = new Type<>(
        TailoredTraits.id(
            "select_individual_power"
        )
    );

    public static final StreamCodec<
        RegistryFriendlyByteBuf,
        SelectIndividualPowerPayload
    > CODEC = StreamCodec.composite(
        EquipmentSlot.STREAM_CODEC,
        SelectIndividualPowerPayload::slot,

        ByteBufCodecs.STRING_UTF8,
        SelectIndividualPowerPayload::materialId,

        SelectIndividualPowerPayload::new
    );

    @Override
    public Type<
        ? extends CustomPacketPayload
    > type() {
        return TYPE;
    }
}