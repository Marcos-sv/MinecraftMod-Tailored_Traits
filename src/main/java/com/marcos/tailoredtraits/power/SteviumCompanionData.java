package com.marcos.tailoredtraits.power;

import java.util.UUID;

import com.marcos.tailoredtraits.TailoredTraits;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.world.entity.Mob;

public final class SteviumCompanionData {

    /*
     * Guarda permanentemente no próprio mob
     * o UUID do jogador que o domesticou.
     *
     * O valor é salvo como texto para que
     * possa ser serializado pelo Codec.STRING.
     */
    public static final AttachmentType<String>
        STEVIUM_OWNER_UUID =
            AttachmentRegistry.createPersistent(
                TailoredTraits.id(
                    "stevium_owner_uuid"
                ),
                Codec.STRING
            );

    private SteviumCompanionData() {
    }

    /**
     * Força o carregamento e o registro
     * do tipo de dado durante a inicialização
     * do mod, antes dos mundos serem carregados.
     */
    public static void initialize() {
        TailoredTraits.LOGGER.info(
            "Dados dos companheiros de Stevium registrados."
        );
    }

    /**
     * Retorna o UUID do dono do mob.
     *
     * Retorna null quando:
     *
     * - o mob ainda não foi domesticado;
     * - o dado salvo está vazio;
     * - o dado salvo não representa um UUID.
     */
    public static UUID getOwnerUuid(
        Mob mob
    ) {
        if (mob == null) {
            return null;
        }

        String storedOwnerUuid =
            mob.getAttached(
                STEVIUM_OWNER_UUID
            );

        if (
            storedOwnerUuid == null
                || storedOwnerUuid.isBlank()
        ) {
            return null;
        }

        try {
            return UUID.fromString(
                storedOwnerUuid
            );
        } catch (
            IllegalArgumentException exception
        ) {
            TailoredTraits.LOGGER.warn(
                "UUID de dono inválido encontrado "
                    + "em um companheiro de Stevium: {}",
                storedOwnerUuid
            );

            return null;
        }
    }

    /**
     * Verifica se o mob já possui
     * algum dono de Stevium.
     */
    public static boolean hasOwner(
        Mob mob
    ) {
        return getOwnerUuid(
            mob
        ) != null;
    }

    /**
     * Verifica se o mob pertence
     * a um jogador específico.
     */
    public static boolean isOwnedBy(
        Mob mob,
        UUID playerUuid
    ) {
        if (playerUuid == null) {
            return false;
        }

        UUID ownerUuid =
            getOwnerUuid(
                mob
            );

        return playerUuid.equals(
            ownerUuid
        );
    }

    /**
     * Define o dono de um mob.
     *
     * O vínculo não pode ser substituído
     * quando o mob já possui um dono.
     */
    public static boolean setOwner(
        Mob mob,
        UUID playerUuid
    ) {
        if (
            mob == null
                || playerUuid == null
                || hasOwner(mob)
        ) {
            return false;
        }

        mob.setAttached(
            STEVIUM_OWNER_UUID,
            playerUuid.toString()
        );

        /*
         * Impede o desaparecimento natural
         * do mob quando o jogador se afasta.
         */
        mob.setPersistenceRequired();

        return true;
    }
}