package io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public record DebugSectionInfo<T>(T data, ChunkSectionPos pos, DebugSectionType<T> type) {
    public void write(final PacketByteBuf buf) {
        final Identifier id = DebugSectionType.REGISTRY.getId(type);
        buf.writeIdentifier(id);
        buf.writeChunkSectionPos(pos);
        type.encoder.accept(data, buf);
    }

    public static @Nullable DebugSectionInfo<?> read(final PacketByteBuf buf) {
        final Identifier id = buf.readIdentifier();
        final DebugSectionType<?> type = DebugSectionType.REGISTRY.get(id);
        if (type == null) {
            return null;
        }
        final ChunkSectionPos pos = buf.readChunkSectionPos();
        return read0(buf, type, pos);
    }

    private static <T> DebugSectionInfo<T> read0(final PacketByteBuf buf, final DebugSectionType<T> type, final ChunkSectionPos pos) {
        return new DebugSectionInfo<>(type.decoder.apply(buf), pos, type);
    }
}
