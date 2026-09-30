package nl.juiced.guhs.feature.verhaal.wereld;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import nl.juiced.guhs.feature.verhaal.VerhaalFeature;

/**
 * Placement {@code guhs:regio_piek} (3.0): one potential start chunk per cell of {@code plek.cell} chunks: the chunk of the
 * cell's {@link RegioPlek} spot (the generalised Elf-Guhjestocht placement). A {@link RandomSpreadStructurePlacement} (spacing
 * = the cell, no separation), so /locate, the Superkompas and the guh compasses ask {@link #getPotentialStructureChunk} per
 * cell the usual way. Cells without a spot give the chunk of their (unused) peak: the structure says no there.
 */
public class RegioPiekPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<RegioPiekPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i).and(
            RegioPlek.CODEC.fieldOf("plek").forGetter(p -> p.plek)
    ).apply(i, RegioPiekPlacement::new));

    private final RegioPlek plek;

    public RegioPiekPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                              Optional<ExclusionZone> exclusion, RegioPlek plek) {
        super(locateOffset, method, frequency, salt, exclusion, plek.cell(), 0, RandomSpreadType.LINEAR);
        this.plek = plek;
    }

    public RegioPlek plek() {
        return plek;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
        int cx = Math.floorDiv(regionX, spacing()), cz = Math.floorDiv(regionZ, spacing());
        return plek.plek(seed, cx, cz).map(p -> new ChunkPos(p.x() >> 4, p.z() >> 4)).orElseGet(() -> {
            Regio.Piek piek = plek.regio().piek(seed, plek.cell(), cx, cz);
            return new ChunkPos(piek.x() >> 4, piek.z() >> 4);
        });
    }

    @Override
    public StructurePlacementType<?> type() {
        return VerhaalFeature.REGIO_PLACEMENT.get();
    }
}
