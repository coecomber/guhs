package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every helper task (the ids of verblijf.json's "taken", design 2.8 and the build animals of 1.3) and its adaptive
 * template. A task without its own search looks for a flat dry spot near the board.
 */
public final class Sjablonen {
    private static final Map<String, Sjabloon> ALLE = new LinkedHashMap<>();
    /** How many variants a task has (the screen cycles them; the names: gui.guhs.bouwopdracht.variant.<taak>.<n>). */
    private static final Map<String, Integer> VARIANTEN = Map.of("planten", 4, "zebrapad", 2, "inrichten", 4, "slingers", 2, "decoreren", 5,
            "houtsnijwerk", 3, "ijsvloer", 2);

    private record Een(Function<Klus, BlockPos> zoeker, BiFunction<Klus, BlockPos, Map<BlockPos, BlockState>> maker, int varianten) implements Sjabloon {
        @Override
        public List<BlockPos> zoek(Klus k) {
            return List.of(zoeker.apply(k));
        }

        @Override
        public Map<BlockPos, BlockState> maak(Klus k, List<BlockPos> punten) {
            return maker.apply(k, punten.get(0));
        }
    }

    private record Twee(Function<Klus, List<BlockPos>> zoeker, TriMaker maker, int varianten, Soort soort) implements Sjabloon {
        @Override
        public List<BlockPos> zoek(Klus k) {
            return zoeker.apply(k);
        }

        @Override
        public Map<BlockPos, BlockState> maak(Klus k, List<BlockPos> punten) {
            return maker.maak(k, punten.get(0), punten.size() > 1 ? punten.get(1) : punten.get(0));
        }

        @Override
        public int punten() {
            return 2;
        }
    }

    @FunctionalInterface
    private interface TriMaker {
        Map<BlockPos, BlockState> maak(Klus k, BlockPos a, BlockPos b);
    }

    private static void een(String taak, Function<Klus, BlockPos> zoeker, BiFunction<Klus, BlockPos, Map<BlockPos, BlockState>> maker) {
        ALLE.put(taak, new Een(zoeker, maker, VARIANTEN.getOrDefault(taak, 1)));
    }

    private static Function<Klus, BlockPos> vlak(int half) {
        return k -> k.vlakkePlek(half);
    }

    private static BlockPos smalste(Klus k, int max) {
        WaterWerk.Oversteek o = WaterWerk.smalste(k, max);
        BlockPos c = o.kolom(o.midden(), o.s(), o.oppervlak());
        return c;
    }

    static {
        // the beaver (water & wood)
        een("dam", k -> smalste(k, 9), WaterWerk::dam);
        een("burcht", WaterWerk::burchtZoek, WaterWerk::burcht);
        een("brug", k -> smalste(k, 12), WaterWerk::brug);
        een("steiger", WaterWerk::waterBijBord, WaterWerk::steiger);
        een("vlonder", WaterWerk::waterBijBord, WaterWerk::vlonder);
        een("kappen", TerreinWerk::boomZoek, TerreinWerk::kappen);
        // the digging crew (meerkat, badger, wombat)
        een("kelder", vlak(3), GraafWerk::kelder);
        een("tunnel", GraafWerk::tunnelZoek, GraafWerk::tunnel);
        een("vijver", vlak(4), (k, p) -> GraafWerk.vijver(k, p, "vijver"));
        een("holletjesnet", vlak(3), GraafWerk::holletjesnet);
        een("beekje", vlak(2), GraafWerk::beekje);
        // the elephant
        ALLE.put("sjouwen", new Sjabloon() {
            @Override
            public List<BlockPos> zoek(Klus k) {
                return List.of(Bouwopdracht.keetZoek(k));
            }

            @Override
            public Map<BlockPos, BlockState> maak(Klus k, List<BlockPos> punten) {
                return Map.of();
            }

            @Override
            public Soort soort() {
                return Soort.SJOUWEN;
            }
        });
        een("opruimen", k -> k.grondPos(k.bord().getX(), k.bord().getZ()), TerreinWerk::opruimen);
        een("stampen", vlak(0), TerreinWerk::stampen);
        een("modderpoel", vlak(3), (k, p) -> GraafWerk.vijver(k, p, "modderpoel"));
        // the demolition crew, the zebras, the planters
        een("slopen", k -> {
            throw Klus.nee("wijs_aan");
        }, TerreinWerk::slopen);
        een("zebrapad", TerreinWerk::padZoek, (k, p) -> TerreinWerk.zebrapad(k, k.grondPos(k.bord().getX(), k.bord().getZ()), p));
        een("planten", vlak(2), TerreinWerk::planten);
        // the orangutan
        een("boomhut", TerreinWerk::boomZoek, BouwWerk::boomhut);
        ALLE.put("touwbrug", new Twee(k -> List.of(BouwWerk.tweeBomen(k, TerreinWerk.boomZoek(k))), (k, a, b) -> {
            BlockPos[] t = a.equals(b) ? BouwWerk.tweeBomen(k, a) : new BlockPos[]{TerreinWerk.boomBij(k, a, 3), TerreinWerk.boomBij(k, b, 3)};
            return BouwWerk.brug(k, nietNul(t[0]), nietNul(t[1]), false);
        }, 1, Sjabloon.Soort.SPOOK));
        ALLE.put("hangbrug", new Twee(k -> List.of(BouwWerk.tweeBomen(k, TerreinWerk.boomZoek(k))), (k, a, b) -> {
            BlockPos[] t = a.equals(b) ? BouwWerk.tweeBomen(k, a) : new BlockPos[]{TerreinWerk.boomBij(k, a, 3), TerreinWerk.boomBij(k, b, 3)};
            return BouwWerk.brug(k, nietNul(t[0]), nietNul(t[1]), true);
        }, 1, Sjabloon.Soort.SPOOK));
        // the penguin
        een("iglo", vlak(4), BouwWerk::iglo);
        een("ijsglijbaan", vlak(2), BouwWerk::ijsglijbaan);
        een("ijssculptuur", vlak(2), BouwWerk::ijssculptuur);
        een("ijsvloer", WaterWerk::waterBijBord, WaterWerk::ijsvloer);
        // the capybara
        een("badje", vlak(3), GraafWerk::badje);
        een("vijvertje", vlak(3), (k, p) -> GraafWerk.vijver(k, p, "vijvertje"));
        een("hottub", vlak(4), GraafWerk::hottub);
        // the otter, the flamingo, the mammoth
        een("stapstenen", k -> smalste(k, 12), WaterWerk::stapstenen);
        een("modderglijbaan", WaterWerk::waterBijBord, WaterWerk::modderglijbaan);
        een("oever", WaterWerk::waterBijBord, WaterWerk::oever);
        een("sneeuwschuiven", k -> k.grondPos(k.bord().getX(), k.bord().getZ()), TerreinWerk::sneeuwschuiven);
        een("sneeuwlaag", k -> k.grondPos(k.bord().getX(), k.bord().getZ()), TerreinWerk::sneeuwlaag);
        // the kangaroo: a running job between two chests
        ALLE.put("buideltransport", new Sjabloon() {
            @Override
            public List<BlockPos> zoek(Klus k) {
                return Bouwopdracht.tweeKisten(k);
            }

            @Override
            public Map<BlockPos, BlockState> maak(Klus k, List<BlockPos> punten) {
                return Map.of();
            }

            @Override
            public int punten() {
                return 2;
            }

            @Override
            public Soort soort() {
                return Soort.TRANSPORT;
            }
        });
        // the build animals
        een("houtsnijwerk", SierWerk::houtZoek, SierWerk::houtsnijwerk);
        een("inrichten", k -> {
            throw Klus.nee("wijs_aan");
        }, SierWerk::inrichten);
        ALLE.put("slingers", new Twee(SierWerk::tweeToppen, SierWerk::slingers, VARIANTEN.get("slingers"), Sjabloon.Soort.SPOOK));
        een("gevel", k -> {
            throw Klus.nee("wijs_aan");
        }, SierWerk::gevel);
        een("licht", SierWerk::lichtZoek, SierWerk::licht);
        een("decoreren", vlak(1), SierWerk::decoreren);
    }

    private static BlockPos nietNul(@Nullable BlockPos p) {
        if (p == null) {
            throw Klus.nee("geen_bomen");
        }
        return p;
    }

    @Nullable
    public static Sjabloon van(String taak) {
        return ALLE.get(taak);
    }

    public static boolean bestaat(String taak) {
        return ALLE.containsKey(taak);
    }

    public static java.util.Set<String> taken() {
        return ALLE.keySet();
    }

    public static int varianten(String taak) {
        Sjabloon s = ALLE.get(taak);
        if (s instanceof Een e) {
            return e.varianten();
        }
        if (s instanceof Twee t) {
            return t.varianten();
        }
        return s == null ? 1 : s.varianten();
    }

    private Sjablonen() {
    }
}
