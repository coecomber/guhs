package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/**
 * One drawing, as the server keeps it ({@link TekeningenData}): its id, who made it (UUID + the name at the last save), what
 * it is for, a version that goes up with every edit (clients drop their cached picture then), when it was made and last
 * changed (epoch millis), and the 64x64 ARGB pixels ({@link TekenPixels}).
 */
public record Tekening(UUID id, UUID maker, String makerNaam, Doel doel, int versie, long gemaakt, long bijgewerkt, int[] pixels) {
    /** The pixels on disk: deflated bytes (a few hundred bytes for a sketch instead of 16 KB of ints). */
    public static final Codec<int[]> PIXELS_CODEC = Codec.BYTE_BUFFER.comapFlatMap(buf -> {
        ByteBuffer b = buf.duplicate();
        byte[] data = new byte[b.remaining()];
        b.get(data);
        int[] px = TekenPixels.pakUit(data);
        return px == null ? DataResult.error(() -> "guhwinter tekening: broken pixels") : DataResult.success(px);
    }, px -> ByteBuffer.wrap(TekenPixels.pak(px)));

    public static final Codec<Tekening> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Tekening::id),
            UUIDUtil.STRING_CODEC.fieldOf("maker").forGetter(Tekening::maker),
            Codec.STRING.optionalFieldOf("maker_naam", "?").forGetter(Tekening::makerNaam),
            Codec.STRING.xmap(Doel::van, Doel::id).optionalFieldOf("doel", Doel.LOOTJE).forGetter(Tekening::doel),
            Codec.INT.optionalFieldOf("versie", 1).forGetter(Tekening::versie),
            Codec.LONG.optionalFieldOf("gemaakt", 0L).forGetter(Tekening::gemaakt),
            Codec.LONG.optionalFieldOf("bijgewerkt", 0L).forGetter(Tekening::bijgewerkt),
            PIXELS_CODEC.fieldOf("pixels").forGetter(Tekening::pixels)
    ).apply(i, Tekening::new));

    /** The same drawing with new pixels (and the maker's current name), one version further. */
    public Tekening bewerkt(String naam, int[] nieuw, long nu) {
        return new Tekening(id, maker, naam, doel, versie + 1, gemaakt, nu, nieuw.clone());
    }

    /** Same content (records compare arrays by reference). */
    public boolean zelfde(Tekening o) {
        return id.equals(o.id) && maker.equals(o.maker) && makerNaam.equals(o.makerNaam) && doel == o.doel && versie == o.versie
                && gemaakt == o.gemaakt && bijgewerkt == o.bijgewerkt && Arrays.equals(pixels, o.pixels);
    }
}
