package org.github.sawervatoryy.mirageClans.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Пакетное свечение: сущность светится только для конкретного зрителя (нужен ProtocolLib). */
public final class ProtocolGlow {

    private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    /** Отправляет зрителю метаданные цели с включённым/выключенным флагом свечения. */
    public void send(Player viewer, Player target, boolean glow) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, target.getEntityId());
            WrappedDataWatcher.Serializer ser = WrappedDataWatcher.Registry.get(Byte.class);
            List<WrappedDataValue> values = new ArrayList<>();
            values.add(new WrappedDataValue(0, ser, Byte.valueOf(flags(target, glow))));
            packet.getDataValueCollectionModifier().write(0, values);
            manager.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
            // зритель мог выйти — игнорируем
        }
    }

    private byte flags(Player t, boolean glow) {
        int b = 0;
        if (t.getFireTicks() > 0) {
            b |= 0x01;
        }
        if (t.isSneaking()) {
            b |= 0x02;
        }
        if (t.isSprinting()) {
            b |= 0x08;
        }
        if (t.isSwimming()) {
            b |= 0x10;
        }
        if (t.isInvisible()) {
            b |= 0x20;
        }
        if (glow || t.isGlowing()) {
            b |= 0x40;
        }
        if (t.isGliding()) {
            b |= 0x80;
        }
        return (byte) b;
    }
}
