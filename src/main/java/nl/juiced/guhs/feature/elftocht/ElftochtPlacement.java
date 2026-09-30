package nl.juiced.guhs.feature.elftocht;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Placement {@code guhs:elftocht_piek}: one potential start chunk per cell of {@code spacing} x {@code spacing} chunks,
 * and that chunk is the chunk of the cell's polder peak ({@link ElftochtPiek}). It is a {@link RandomSpreadStructurePlacement}
 * (spacing = the cell, no separation), so /locate, the Superkompas and the guh compasses find it the usual way: they ask
 * {@link #getPotentialStructureChunk} per cell. With a {@code vlak} (dead-flat polder rules) the start chunk is the chunk of
 * the best flat spot near the peak instead of the peak itself.
 */
public class ElftochtPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<ElftochtPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i).and(i.group(
            Codec.intRange(2, 64).fieldOf("spacing").forGetter(ElftochtPlacement::spacing),
            NormalNoise.NoiseParameters.CODEC.fieldOf("noise").forGetter(p -> p.noise),
            ElftochtPiek.Vlak.CODEC.optionalFieldOf("vlak", ElftochtPiek.Vlak.GEEN).forGetter(p -> p.vlak)
    )).apply(i, ElftochtPlacement::new));

    private final Holder<NormalNoise.NoiseParameters> noise;
    private final ElftochtPiek.Vlak vlak;

    public ElftochtPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                             Optional<ExclusionZone> exclusion, int spacing, Holder<NormalNoise.NoiseParameters> noise, ElftochtPiek.Vlak vlak) {
        super(locateOffset, method, frequency, salt, exclusion, spacing, 0, RandomSpreadType.LINEAR);
        this.noise = noise;
        this.vlak = vlak;
    }

    public ElftochtPiek.Vlak vlak() {
        return vlak;
    }

    public Holder<NormalNoise.NoiseParameters> noise() {
        return noise;
    }

    /** The chunk of the tour's spot in the cell that holds (regionX, regionZ) (chunk coordinates): near the polder peak,
     *  where the square lies best on dead-flat polder ({@link ElftochtPiek#spot}). */
    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
        int cx = Math.floorDiv(regionX, spacing()), cz = Math.floorDiv(regionZ, spacing());
        ElftochtPiek.Spot spot = ElftochtPiek.spot(seed, noise, vlak, spacing(), cx, cz);
        return new ChunkPos(spot.x() >> 4, spot.z() >> 4);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ElftochtFeature.PIEK_PLACEMENT.get();
    }
}
