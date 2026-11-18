package nl.juiced.guhs.feature.guhwinter.lootjes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The item component {@code guhs:guhwinter_surprise}: a captured lootjes plot ({@link Inhoud#opslaan}, front to the south),
 * who made it and for whom. Never changed in place (a new record for a new gift).
 */
public record SurpriseData(CompoundTag inhoud, String maker, String voor) {
    public static final Codec<SurpriseData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CompoundTag.CODEC.fieldOf("inhoud").forGetter(SurpriseData::inhoud),
            Codec.STRING.optionalFieldOf("maker", "?").forGetter(SurpriseData::maker),
            Codec.STRING.optionalFieldOf("voor", "").forGetter(SurpriseData::voor)
    ).apply(i, SurpriseData::new));
    public static final StreamCodec<ByteBuf, SurpriseData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SurpriseData::inhoud, ByteBufCodecs.stringUtf8(64), SurpriseData::maker,
            ByteBufCodecs.stringUtf8(64), SurpriseData::voor, SurpriseData::new);

    public static SurpriseData van(Inhoud in, String maker, String voor) {
        return new SurpriseData(in.opslaan(), maker, voor);
    }

    /** How many blocks are in it. */
    public int aantal() {
        return Inhoud.aantal(inhoud);
    }

    public boolean leeg() {
        return aantal() == 0;
    }

    public Inhoud lees() {
        return Inhoud.laad(inhoud);
    }
}
