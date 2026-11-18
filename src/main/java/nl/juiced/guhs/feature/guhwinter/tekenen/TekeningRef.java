package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The item component {@code guhs:guhwinter_tekening}: which drawing a hangable drawing item shows, and its maker's name. */
public record TekeningRef(UUID id, String maker) {
    public static final Codec<TekeningRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(TekeningRef::id),
            Codec.STRING.optionalFieldOf("maker", "?").forGetter(TekeningRef::maker)
    ).apply(i, TekeningRef::new));
    public static final StreamCodec<ByteBuf, TekeningRef> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TekeningRef::id, ByteBufCodecs.stringUtf8(64), TekeningRef::maker, TekeningRef::new);
}
