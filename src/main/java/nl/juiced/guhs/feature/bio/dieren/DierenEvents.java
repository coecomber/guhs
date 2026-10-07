package nl.juiced.guhs.feature.bio.dieren;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.Mikas;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.WildeDieren;

/**
 * The game-bus side of the biomes3 animals: a koi or wolkenschaapje that a spawner brings is a come-and-go animal (never
 * saved), the tidy-up above the cap, the biome guhs at birth, and no Mika is ever born in the three biomes.
 */
public final class DierenEvents {
    private DierenEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (magNietGeboren(mob.getType(), event.getSpawnType(), Bio.inNieuw(mob.level(), mob.blockPosition()))) {
            event.setSpawnCancelled(true);
            return;
        }
        if (DierenRegels.komtEnGaat(event.getSpawnType()) && DierenRegels.soorten().contains(mob.getType()) && !mob.isPersistenceRequired()) {
            WildeDieren.markeer(mob);
        }
    }

    /** No Mika is born by itself (natural spawner, chunk generation, a spawner block) in the three new biomes. */
    public static boolean magNietGeboren(EntityType<?> type, EntitySpawnReason reden, boolean inNieuwBiome) {
        return inNieuwBiome && Mikas.isMika(type) && (DierenRegels.natuurlijk(reden) || EntitySpawnReason.isSpawner(reden));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof GuhEntity guh && level.dimension() == ModDimensions.GUHMENSION) {
            BiomeGuhs.kies(guh, BiomeGuhs.nieuwBiome(level, guh.blockPosition()));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % WildeDieren.OPRUIM_TIJD == 17) {
            DierenRegels.tidyUp(level);
        }
    }

    /**
     * The dev-server self test ({@code /guhs bio zelftest dieren}): the spawn lists of the three real biomes hold our
     * animals (the biome modifiers landed) and no Mika.
     */
    static void zelftest() {
        BioZelftest.registreer("dieren", (server, level, meld) -> {
            var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
            Map<ResourceKey<Biome>, List<EntityType<?>>> verwacht = Map.of(
                    Bio.BLOESEMMEERTJE, List.of(DierenSlice.KOI.get(), KaasmoerasFeature.KIKKERGUH.get()),
                    Bio.KLATERDAL, List.of(DierenSlice.KOI.get(), KaasmoerasFeature.KIKKERGUH.get()),
                    Bio.WOLKENWEIDE, List.of(DierenSlice.WOLKENSCHAAPJE.get()));
            for (var e : verwacht.entrySet()) {
                var holder = biomes.get(e.getKey());
                if (holder.isEmpty()) {
                    meld.fout("biome " + e.getKey().identifier() + " is missing");
                    continue;
                }
                var spawns = holder.get().value().getMobSettings();
                for (EntityType<?> type : e.getValue()) {
                    boolean staatErin = false;
                    for (MobCategory cat : MobCategory.values()) {
                        staatErin |= spawns.getMobs(cat).unwrap().stream().anyMatch(w -> w.value().type() == type);
                    }
                    meld.check(staatErin, e.getKey().identifier().getPath() + " spawns " + EntityType.getKey(type).getPath());
                }
                boolean mika = false;
                for (MobCategory cat : MobCategory.values()) {
                    mika |= spawns.getMobs(cat).unwrap().stream().anyMatch(w -> Mikas.isMika(w.value().type()));
                }
                meld.check(!mika, e.getKey().identifier().getPath() + " spawns no Mika");
            }
            // the predicates at a real spot of each biome (asked from the biome source: no chunk is made)
            BlockPos van = new BlockPos(0, 80, 0);
            for (ResourceKey<Biome> b : List.of(Bio.BLOESEMMEERTJE, Bio.KLATERDAL, Bio.WOLKENWEIDE)) {
                BlockPos plek = BioZelftest.vind(level, b, van, 6400);
                if (plek == null) {
                    meld.ok(b.identifier().getPath() + ": not in the biome source yet (the wereld slice puts it there), predicates not asked");
                } else {
                    meld.check(BiomeGuhs.variantVoor(BiomeGuhs.nieuwBiome(level, plek)) != null,
                            b.identifier().getPath() + " at " + plek.toShortString() + " has its guh: " + BiomeGuhs.variantVoor(BiomeGuhs.nieuwBiome(level, plek)));
                }
            }
        });
    }
}
