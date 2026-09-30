package nl.juiced.guhs.feature.speelgoed;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.huisje.Speeltje;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;

/**
 * The four toy kinds for the huisje residents' random play (and {@link SpeelGoal}): each finds a toy of its kind near
 * a spot and makes the play session ({@link KnabbelbalSpel}, {@link ToestelSpel}, {@link TunnelSpel}).
 */
public final class Speeltjes {
    private Speeltjes() {
    }

    static void registreer() {
        Speelgoed.registreer(new Soort("knabbelbal") {
            @Nullable
            @Override
            public KlusTaak zoek(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
                if (!(wie instanceof GuhEntity)) {
                    return null;
                }
                KnabbelbalEntity bal = bal(level, wie, rond, bereik);
                return bal == null ? null : new KnabbelbalSpel(wie, level, bal);
            }
        });
        Speelgoed.registreer(new Soort("glijbaantje") {
            @Nullable
            @Override
            public KlusTaak zoek(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
                if (!(wie instanceof GuhEntity)) {
                    return null;
                }
                return toestellen(level, rond, bereik).filter(p -> level.getBlockState(p).getBlock() instanceof GlijbaanBlock)
                        .filter(p -> ToestelBlock.zitje(level, p, 0) == null)
                        .min(Comparator.comparingDouble(p -> p.distSqr(wie.blockPosition())))
                        .map(p -> (KlusTaak) new ToestelSpel(wie, level, p, 0)).orElse(null);
            }
        });
        Speelgoed.registreer(new Soort("tunnel") {
            @Nullable
            @Override
            public KlusTaak zoek(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
                if (!(wie instanceof GuhEntity) && !(wie instanceof PieppiepmuisjeEntity)) {
                    return null;
                }
                return tunnel(level, wie, rond, bereik);
            }
        });
        Speelgoed.registreer(new Soort("wip_schommel") {
            @Nullable
            @Override
            public KlusTaak zoek(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
                if (!(wie instanceof GuhEntity)) {
                    return null;
                }
                // a wip where a friend already sits comes first (together is more fun), then the nearest
                return toestellen(level, rond, bereik).filter(p -> level.getBlockState(p).getBlock() instanceof SchommelToestel t && t.vrijePlek(level, p) >= 0)
                        .min(Comparator.<BlockPos>comparingInt(p -> -((ToestelBlock) level.getBlockState(p).getBlock()).bezet(level, p))
                                .thenComparingDouble(p -> p.distSqr(wie.blockPosition())))
                        .map(p -> (KlusTaak) new ToestelSpel(wie, level, p, ((ToestelBlock) level.getBlockState(p).getBlock()).vrijePlek(level, p)))
                        .orElse(null);
            }
        });
    }

    private abstract static class Soort implements Speeltje {
        private final String id;

        Soort(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    /** The toy blocks (glijbaantje, wip, schommel controllers and tunnel pieces) within bereik of rond. */
    static Stream<BlockPos> toestellen(ServerLevel level, BlockPos rond, int bereik) {
        return level.getPoiManager().getInRange(h -> h.is(SpeelgoedFeature.POI.getKey()), rond, bereik, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos);
    }

    /** The nearest knabbelbal within bereik (horizontally) of rond. */
    @Nullable
    static KnabbelbalEntity bal(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
        List<KnabbelbalEntity> ballen = level.getEntitiesOfClass(KnabbelbalEntity.class, new AABB(rond).inflate(bereik, 6, bereik));
        return ballen.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(wie))).orElse(null);
    }

    /** A run through the nearest tunnel: in at the nearest entrance, out at the one farthest along the way. */
    @Nullable
    static TunnelSpel tunnel(ServerLevel level, Mob wie, BlockPos rond, int bereik) {
        BlockPos stuk = toestellen(level, rond, bereik).filter(p -> level.getBlockState(p).getBlock() instanceof TunnelBlock)
                .min(Comparator.comparingDouble(p -> p.distSqr(wie.blockPosition()))).orElse(null);
        if (stuk == null) {
            return null;
        }
        Set<BlockPos> netwerk = TunnelBlock.netwerk(level, stuk, 64);
        List<TunnelBlock.Ingang> ingangen = TunnelBlock.ingangen(level, netwerk);
        if (ingangen.isEmpty()) {
            return null;
        }
        Vec3 bij = wie.position();
        TunnelBlock.Ingang in = ingangen.stream().min(Comparator.comparingDouble(i -> i.buiten().distanceToSqr(bij))).orElseThrow();
        TunnelBlock.Ingang uit = in;
        List<BlockPos> route = List.of(in.pos());
        for (TunnelBlock.Ingang i : ingangen) {
            if (i.equals(in)) {
                continue;
            }
            List<BlockPos> r = TunnelBlock.route(level, in.pos(), i.pos(), 128);
            if (!r.isEmpty() && (uit == in || r.size() > route.size() || (r.size() == route.size() && level.getRandom().nextBoolean()))) {
                uit = i;
                route = r;
            }
        }
        if (uit == in && netwerk.size() > 1) {     // only one hole: in to the far end and back out again
            BlockPos ver = netwerk.stream().max(Comparator.comparingInt(p -> TunnelBlock.route(level, in.pos(), p, 128).size())).orElse(in.pos());
            List<BlockPos> heen = TunnelBlock.route(level, in.pos(), ver, 128);
            List<BlockPos> terug = new java.util.ArrayList<>(heen);
            java.util.Collections.reverse(terug);
            route = new java.util.ArrayList<>(heen);
            route.addAll(terug.subList(1, terug.size()));
        }
        return new TunnelSpel(wie, level, in, uit, route, netwerk);
    }

    /** (tests) a toy near this block of the given kind, as a session for this mob. */
    @Nullable
    public static KlusTaak voor(ServerLevel level, Mob wie, String soort, BlockPos rond, int bereik) {
        var s = Speelgoed.van(soort);
        return s == null ? null : s.zoek(level, wie, rond, bereik);
    }

    static boolean isToestel(BlockState s) {
        return s.getBlock() instanceof ToestelBlock || s.getBlock() instanceof TunnelBlock;
    }
}
