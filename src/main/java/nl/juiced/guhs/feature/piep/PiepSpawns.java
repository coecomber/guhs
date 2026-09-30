package nl.juiced.guhs.feature.piep;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.Structure;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Where pieppiepmuisjes come from: the lieve guh buildings of the Guhmensie, about one muisje per {@link #PER_GUHS} guhs
 * a structure puts into the world. Never in the Overworld, the Barbecuether or elsewhere, and never in the Mika places
 * ({@link #NIET_HIER}).
 * <p>
 * <b>The one hook</b>: {@link #maybeAddMuisje(ServerLevel, BlockPos, RandomSource)}. Every guh (and guh character) that a
 * structure template places passes it once, automatically (PiepEvents.onJoin, spawn type STRUCTURE). Code that puts guhs
 * into a building by itself (not from a template) can call it once per guh it spawns.
 */
public final class PiepSpawns {
    /** About one muisje per this many structure guhs. */
    public static final int PER_GUHS = 10;
    /** Structures that are not lief (Mika's, the nest, the Barbecuether ones that also turn up in the Guhmensie). */
    public static final Set<String> NIET_HIER = Set.of("mika_kamp", "moerasheks_hut", "knabbelkelder", "evil_mika_home", "challenging_guh_caves",
            "stille_voorraadkelder", "barbecueput", "barbecueput_groot", "barbecueput_klein", "spiesburcht", "mika_grillpaleis", "mika_vesting",
            "kaasknabbel_nest", "voorraadschuur");
    /** Persistent data: this guh has had its (one) chance at a muisje. */
    public static final String GEHAD = "guhs_piep_muisje_kans";

    /**
     * One structure guh at this spot: with a chance of 1 in {@link #PER_GUHS} a pieppiepmuisje comes to live next to it, if
     * the spot is in a lieve building of the Guhmensie ({@link #magHier}). Returns the muisje, or null.
     */
    @Nullable
    public static PieppiepmuisjeEntity maybeAddMuisje(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!kans(random) || !magHier(level, pos)) {
            return null;
        }
        return spawnMuisje(level, pos, random);
    }

    /** The roll: 1 in {@link #PER_GUHS}. */
    public static boolean kans(RandomSource random) {
        return random.nextInt(PER_GUHS) == 0;
    }

    /** May a muisje live here? Only in the Guhmensie, and not in a Mika place or the Barbecuether's buildings. */
    public static boolean magHier(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return false;
        }
        if (!level.isLoaded(pos)) {
            return true;                                       // (no structure data to ask: the guh was placed by a lief template)
        }
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Structure s : level.structureManager().getAllStructuresAt(pos).keySet()) {
            Identifier id = registry.getKey(s);
            if (id != null && id.getNamespace().equals(Guhs.MODID) && NIET_HIER.contains(id.getPath())) {
                return false;
            }
        }
        return true;
    }

    /** A wild muisje next to this spot (on the floor, where there is room). */
    @Nullable
    public static PieppiepmuisjeEntity spawnMuisje(ServerLevel level, BlockPos pos, RandomSource random) {
        PieppiepmuisjeEntity muis = PiepFeature.PIEPPIEPMUISJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (muis == null) {
            return null;
        }
        BlockPos at = pos;
        for (int i = 0; i < 8; i++) {
            BlockPos p = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
                at = p;
                break;
            }
        }
        muis.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
        muis.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
        muis.setPersistenceRequired();
        level.addFreshEntity(muis);
        return muis;
    }

    private PiepSpawns() {
    }
}
