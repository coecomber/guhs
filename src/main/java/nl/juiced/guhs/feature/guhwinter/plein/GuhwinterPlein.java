package nl.juiced.guhs.feature.guhwinter.plein;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.ModDimensions;
import org.slf4j.Logger;

/**
 * 1.10.9 Guhwinter, slice plein: the event square in the Guhmension (75 x 75, x -350..-276, z -735..-661, floor y 30), its
 * two looks (the winter square and the empty frame for after the event, templates by tools/features/guhwinter_plein.py), the
 * arrival around the new spawn portal and the one-time setup ({@code /guhs winter inrichten}: {@link #inrichten}).
 * <p>
 * All work runs as a {@link Klus} over the next ticks (a time budget per tick), never in one go: the chunks are loaded through
 * tickets first. {@link #plaatsWinter} / {@link #plaatsRaam} replace whatever stands inside the square; the ground around it
 * is shaped only the first time.
 */
public final class GuhwinterPlein {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int X1 = -350, Z1 = -735, X2 = -276, Z2 = -661, VLOER = 30;
    /** The square (y 20..90, as kern's GuhwinterZone.PLEIN). */
    public static final BoundingBox PLEIN = new BoundingBox(X1, 20, Z1, X2, 90, Z2);
    public static final Identifier WINTER = Guhs.id("guhwinter_plein_winter"), RAAM = Guhs.id("guhwinter_plein_raam"),
            AANKOMST = Guhs.id("guhwinter_aankomst");
    /** Where the templates go (their min corner; tools/features/guhwinter_plein.py PLEIN_MIN, AANKOMST_MIN). */
    static final BlockPos PLEIN_MIN = new BlockPos(X1, 24, Z1), AANKOMST_MIN = new BlockPos(-310, 26, -660);
    /** The new spawn portal: GuhPortalForcer's base (first portal column, frame bottom); frame x -301..-298, y 30..34, z -655. */
    public static final BlockPos PORTAAL = new BlockPos(-300, 30, -655);
    /** Where the Reisguh "Spawn" stands now (3 blocks from the portal, looking at it). */
    static final BlockPos REISGUH = new BlockPos(-303, 30, -653);
    static final float REISGUH_YAW = -90f;
    /** The ground: the square with a band of 12 back to nature; the arrival (floor y 29) with a band of 6. */
    static final GuhwinterGrond.Vlak PLEIN_VLAK = new GuhwinterGrond.Vlak(X1, Z1, X2, Z2, VLOER, 12);
    static final GuhwinterGrond.Vlak AANKOMST_VLAK = new GuhwinterGrond.Vlak(-307, -659, -285, -649, 29, 6);
    /** Around the portal no ground is ever shaped. */
    static final BoundingBox PORTAAL_VRIJ = new BoundingBox(-303, 25, -658, -296, 40, -652);
    /** Ozovads' hiding spot down the valley (outside the square): two little spruces and some rocks go around it. */
    static final BlockPos VERSTOP = new BlockPos(-294, 30, -644);

    private static final long BUDGET = 15_000_000L;
    private static Klus klus;

    /**
     * (1.10.9 integration) Other slices' own pieces on the square: {@link #voorStempel} runs before either square is
     * stamped (take your blocks and floating texts away), {@link #naStempel} after (put them on it; check {@code welke}).
     * Both return a line for the summary, or null.
     */
    public interface PleinHaak {
        @Nullable
        String doe(ServerLevel level, Identifier welke);
    }

    private static final List<PleinHaak> VOOR_STEMPEL = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final List<PleinHaak> NA_STEMPEL = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void voorStempel(PleinHaak haak) {
        VOOR_STEMPEL.add(haak);
    }

    public static void naStempel(PleinHaak haak) {
        NA_STEMPEL.add(haak);
    }

    @Nullable
    private static String haken(List<PleinHaak> haken, ServerLevel level, Identifier welke) {
        List<String> regels = new java.util.ArrayList<>();
        for (PleinHaak h : haken) {
            try {
                String r = h.doe(level, welke);
                if (r != null) {
                    regels.add(r);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Guhwinter: a square hook failed", e);
                regels.add("a hook FAILED: " + e);
            }
        }
        return regels.isEmpty() ? null : String.join("; ", regels);
    }

    private GuhwinterPlein() {
    }

    /** Is a square or setup at work now? */
    public static boolean bezig() {
        return klus != null;
    }

    /** What is at work (for the command), or null. */
    @Nullable
    public static String stand() {
        return klus == null ? null : klus.stand();
    }

    /** The winter square (snow, houses, the canal...). Replaces whatever stands inside the square. */
    public static void plaatsWinter(ServerLevel level) {
        plaats(level, WINTER, null);
    }

    /** The empty frame for after the event (low wall, gate "Hier komt het volgende feest! VAHOEG", a plain square). */
    public static void plaatsRaam(ServerLevel level) {
        plaats(level, RAAM, null);
    }

    /** Places one of the two looks; {@code klaar} hears the summary. False when something else is at work. */
    public static boolean plaats(ServerLevel level, Identifier welke, @Nullable Consumer<String> klaar) {
        if (klus != null) {
            LOGGER.warn("Guhwinter: {} waits: {} is at work", welke, klus.stand());
            return false;
        }
        Klus k = new Klus(level, klaar);
        k.plein(welke);
        klus = k;
        return true;
    }

    /**
     * {@code /guhs winter inrichten}: the new portal, the Reisguh "Spawn" next to it, the old portal gone and the old peak
     * restored, the winter square (the first time, with its ground), the arrival, Ozovads' hiding spot. Parts that were done
     * before are skipped ({@link GuhwinterPleinData}); the square is only placed when none stands yet.
     */
    public static boolean inrichten(ServerLevel level, @Nullable Consumer<String> klaar) {
        if (klus != null) {
            return false;
        }
        GuhwinterPleinData d = GuhwinterPleinData.van(level);
        Klus k = new Klus(level, klaar);
        k.stap("portaal", () -> GuhwinterPortaal.bouwNieuw(level));
        k.reisguh();
        k.stap("oude top", () -> GuhwinterPortaal.oudStaat(level) || !d.oudeTop ? GuhwinterPortaal.herstelTop(level) : "old peak already restored");
        k.stap("data", () -> {
            d.zet(() -> {
                d.portaal = true;
                d.oudeTop = true;
            });
            return null;
        });
        if (d.plein().isEmpty()) {
            k.plein(WINTER);
        } else {
            k.stap("plein", () -> "square '" + d.plein() + "' already stands: left as it is (/guhs winter plein winter|raam to place it again)");
        }
        k.aankomst();
        return start(k);
    }

    private static boolean start(Klus k) {
        if (klus != null) {
            return false;
        }
        klus = k;
        return true;
    }

    static void tick(MinecraftServer server) {
        Klus k = klus;
        if (k == null) {
            return;
        }
        try {
            if (k.werk(BUDGET)) {
                klus = null;
            }
        } catch (RuntimeException e) {
            LOGGER.error("Guhwinter: the work failed at '{}'", k.stand(), e);
            k.meld("FAILED at " + k.stand() + ": " + e);
            klus = null;
        }
    }

    static void stop() {
        klus = null;
    }

    /** Every chunk the setup touches. */
    static List<ChunkPos> chunks() {
        Set<ChunkPos> uit = new LinkedHashSet<>();
        int[][] vakken = {{X1 - 13, Z1 - 13, X2 + 13, Z2 + 13}, {-315, -667, -277, -628}, {-312, -690, -230, -612}};
        for (int[] v : vakken) {
            for (int cx = v[0] >> 4; cx <= v[2] >> 4; cx++) {
                for (int cz = v[1] >> 4; cz <= v[3] >> 4; cz++) {
                    uit.add(new ChunkPos(cx, cz));
                }
            }
        }
        return new ArrayList<>(uit);
    }

    /** The work: a row of steps, each step a bit per tick. */
    static final class Klus {
        private final ServerLevel level;
        private final Consumer<String> klaar;
        private final List<Stap> stappen = new ArrayList<>();
        private final List<String> verslag = new ArrayList<>();
        private final List<ChunkPos> chunks = chunks();
        private int i, wacht;
        private boolean geladen;

        interface Stap {
            String naam();

            /** Some of the work within the budget (nanoTime deadline); true when done. */
            boolean werk(long tot);
        }

        Klus(ServerLevel level, @Nullable Consumer<String> klaar) {
            this.level = level;
            this.klaar = klaar;
        }

        String stand() {
            return (geladen ? (i < stappen.size() ? stappen.get(i).naam() : "done") : "loading chunks") + " (step " + (i + 1) + " of " + stappen.size() + ")";
        }

        void meld(String regel) {
            verslag.add(regel);
            if (klaar != null) {
                klaar.accept(regel);
            }
        }

        /** A step done in one go; its line (if any) goes in the report. */
        void stap(String naam, java.util.function.Supplier<String> werk) {
            stappen.add(new Stap() {
                public String naam() {
                    return naam;
                }

                public boolean werk(long tot) {
                    String r = werk.get();
                    if (r != null) {
                        LOGGER.info("Guhwinter [{}]: {}", naam, r);
                        verslag.add(r);
                    }
                    return true;
                }
            });
        }

        /** The Reisguh "Spawn": her entities may need a few ticks to load; gives up after 30 s. */
        void reisguh() {
            stappen.add(new Stap() {
                int pogingen;

                public String naam() {
                    return "reisguh";
                }

                public boolean werk(long tot) {
                    GuhwinterPleinData d = GuhwinterPleinData.van(level);
                    for (BlockPos p : List.of(GuhwinterPortaal.OUD_REISGUH, REISGUH)) {
                        level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(p), 2);
                    }
                    boolean klaarGeladen = level.areEntitiesLoaded(ChunkPos.containing(GuhwinterPortaal.OUD_REISGUH).pack())
                            && level.areEntitiesLoaded(ChunkPos.containing(REISGUH).pack());
                    String r = klaarGeladen ? GuhwinterPortaal.verhuisReisguh(level) : null;
                    if (r == null && ++pogingen < 600) {
                        return false;
                    }
                    if (r == null) {
                        r = d.reisguh ? "Reisguh 'Spawn' not found near the old portal (moved before)" : "WARNING: Reisguh 'Spawn' not found near the old portal: nothing moved";
                    } else {
                        d.zet(() -> d.reisguh = true);
                    }
                    LOGGER.info("Guhwinter [reisguh]: {}", r);
                    verslag.add(r);
                    return true;
                }
            });
        }

        /** Ground (the first time with its band) and the template of one of the two squares. */
        void plein(Identifier welke) {
            GuhwinterPleinData d = GuhwinterPleinData.van(level);
            boolean band = !d.grondKlaar;
            stap("slices away", () -> haken(VOOR_STEMPEL, level, welke));
            stappen.add(new Grond("ground of the square" + (band ? " (with its band)" : ""), PLEIN_VLAK, !band));
            stap("square", () -> {
                String r = stempel(welke, PLEIN_MIN);
                optillen(new AABB(X1, VLOER - 2, Z1, X2 + 1, VLOER + 40, Z2 + 1));
                d.zet(() -> {
                    d.grondKlaar = true;
                    d.plein = welke == RAAM ? "raam" : "winter";
                });
                return r;
            });
            stap("slices", () -> haken(NA_STEMPEL, level, welke));
        }

        /** The arrival: its ground, its template, Ozovads' hiding spot. */
        void aankomst() {
            GuhwinterPleinData d = GuhwinterPleinData.van(level);
            if (!d.aankomst) {
                stappen.add(new Grond("ground of the arrival", AANKOMST_VLAK, false));
            }
            stap("arrival", () -> {
                String r = stempel(AANKOMST, AANKOMST_MIN);
                optillen(new AABB(-312, 27, -662, -280, 40, -638));
                d.zet(() -> d.aankomst = true);
                return r;
            });
            stap("hiding spot", () -> {
                if (d.verstop) {
                    return "Ozovads' hiding spot already stands";
                }
                String r = GuhwinterVerstop.bouw(level, VERSTOP);
                d.zet(() -> d.verstop = true);
                return r;
            });
        }

        private String stempel(Identifier id, BlockPos min) {
            StructureTemplate t = level.getStructureManager().get(id).orElseThrow(() -> new IllegalStateException("template " + id + " is missing"));
            StructurePlaceSettings set = new StructurePlaceSettings().setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
            t.placeInWorld(level, min, min, set, level.getRandom(), 2);
            return id.getPath() + " stamped at " + min.toShortString() + " (" + t.getSize().toShortString() + ")";
        }

        /** Whoever stands in the new ground or the new blocks stands on top again. */
        private void optillen(AABB doos) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, doos, e -> !level.noCollision(e))) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, e.getBlockX(), e.getBlockZ());
                e.teleportTo(e.getX(), y, e.getZ());
            }
        }

        boolean werk(long budget) {
            if (!geladen) {
                if (wacht-- > 0) {
                    return false;
                }
                wacht = 20;
                boolean alle = true;
                for (ChunkPos c : chunks) {
                    level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, c, 0);
                    alle &= level.getChunkSource().getChunkNow(c.x(), c.z()) != null;
                }
                if (!alle) {
                    return false;
                }
                geladen = true;
                LOGGER.info("Guhwinter: {} chunks loaded, the work starts ({} steps)", chunks.size(), stappen.size());
            }
            if (++wacht % 100 == 0) {
                for (ChunkPos c : chunks) {
                    level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, c, 0);   // (they run out after 15 s)
                }
            }
            long tot = System.nanoTime() + budget;
            while (i < stappen.size()) {
                if (!stappen.get(i).werk(tot)) {
                    return false;
                }
                i++;
                if (System.nanoTime() > tot) {
                    return false;
                }
            }
            String samen = String.join("; ", verslag);
            LOGGER.info("Guhwinter: all done: {}", samen);
            if (klaar != null) {
                klaar.accept(samen);
            }
            return true;
        }

        /** Ground of a rectangle, column by column within the budget; then the trees that were cut. */
        final class Grond implements Stap {
            private final String naam;
            private final GuhwinterGrond.Vlak vlak;
            private final boolean alleenBinnen;
            private int x, z;
            private long blokken, kolommen;
            private final Set<Long> bomen = new HashSet<>();

            Grond(String naam, GuhwinterGrond.Vlak vlak, boolean alleenBinnen) {
                this.naam = naam;
                this.vlak = vlak;
                this.alleenBinnen = alleenBinnen;
                this.x = vlak.x1() - vlak.band();
                this.z = vlak.z1() - vlak.band();
            }

            public String naam() {
                return naam;
            }

            public boolean werk(long tot) {
                int x2 = vlak.x2() + vlak.band(), z2 = vlak.z2() + vlak.band();
                while (x <= x2) {
                    while (z <= z2) {
                        if (!vrij(x, z)) {
                            int n = GuhwinterGrond.kolom(level, vlak, x, z, alleenBinnen, bomen);
                            blokken += n;
                            kolommen += n > 0 ? 1 : 0;
                        }
                        z++;
                    }
                    z = vlak.z1() - vlak.band();
                    x++;
                    if (System.nanoTime() > tot) {
                        return false;
                    }
                }
                int boom = GuhwinterGrond.bomen(level, bomen, 6000, p -> vrij(p.getX(), p.getZ()) || vlak.afstand(p.getX(), p.getZ()) > vlak.band() + 6);
                String r = naam + ": " + kolommen + " columns, " + blokken + " blocks changed, " + boom + " tree blocks of cut trees removed";
                LOGGER.info("Guhwinter: {}", r);
                verslag.add(r);
                return true;
            }

            /** Never touched: the portal, and (for the arrival) the square itself; the Blub on the old peak. */
            private boolean vrij(int x, int z) {
                if (x >= PORTAAL_VRIJ.minX() && x <= PORTAAL_VRIJ.maxX() && z >= PORTAAL_VRIJ.minZ() && z <= PORTAAL_VRIJ.maxZ()) {
                    return true;
                }
                if (vlak != PLEIN_VLAK && PLEIN_VLAK.binnen(x, z)) {
                    return true;
                }
                return GuhwinterPortaal.BLUB.contains(x + 0.5, GuhwinterPortaal.BLUB.minY + 1, z + 0.5);
            }
        }
    }

    /** (dev) The Guhmension, or null. */
    @Nullable
    static ServerLevel guhmension(MinecraftServer server) {
        return server.getLevel(ModDimensions.GUHMENSION);
    }

    static boolean isLucht(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).is(Blocks.AIR);
    }
}
