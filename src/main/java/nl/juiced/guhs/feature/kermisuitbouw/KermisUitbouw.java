package nl.juiced.guhs.feature.kermisuitbouw;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.Kermis;
import org.slf4j.Logger;

/**
 * 1.8.1: the extension of the Guhkermis, the same at every kermis ({@code guh_kermis_uitbouw}, by
 * tools/features/kermisuitbouw.py). The kermis template stays as it is. Its Kermis-guh looks ({@link #kijk}) every 5 seconds, so
 * old kermissen in a running world get the extension the first time somebody comes near, and new ones as soon as they
 * generate. The work never stops the server: {@link Klus} waits for the chunks (loading or generating them through tickets),
 * then flattens the ground column by column ({@link KermisGrond}) within a budget per tick, and stamps. Everything is forced:
 * hills are cut and valleys filled whatever stands there; only containers of players are saved ({@link #bewaar}).
 * <p>
 * All measures are in the frame of the kermis template, so everything turns with the kermis ({@link Plek}).
 * What is stamped is saved in {@link KermisUitbouwData}, which also protects the new ground ({@link Kermis#protectedAt}).
 */
public final class KermisUitbouw {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Raise this when the template changes in a way old kermissen must get too: they are stamped again, the ground is left. */
    public static final int VERSIE = 1;
    static final Identifier TEMPLATE = Guhs.id("guh_kermis_uitbouw");
    /** The template's min corner in the kermis frame and its size (tools/features/kermisuitbouw.py PLEK, MAAT). */
    public static final BlockPos PLEK = new BlockPos(-40, 0, 2);
    static final Vec3i MAAT = new Vec3i(121, 49, 90);
    /** The kermis floor in its own template. */
    public static final int VLOER = 1;
    /** How wide the ground slopes back to the natural height around the fields. */
    static final int BAND = 10;
    /** What is flattened, in the kermis frame: the main field and the Booster's field. */
    public static final Veld HOOFD = new Veld(-40, 30, 80, 91), BOOSTER = new Veld(-40, 2, -3, 29);
    /** Where the Booster's arm and cages turn: out over the fields, so protected too (not flattened). */
    public static final Veld BOOSTER_ARM = new Veld(-46, 20, 2, 30);
    /** The kermis itself and a little around it: nothing here is touched. */
    public static final Veld VRIJ = new Veld(-2, -2, 41, 33);
    /** How far the protection goes around the fields, below the floor and above it (the Booster reaches floor + 65). */
    static final int RAND = 2, ONDER = 8, BOVEN = 72;
    /** Time per tick for the ground. */
    private static final long BUDGET = 12_000_000L;
    /** A tree that is cut goes all the way, but never more blocks than this. */
    private static final int MAX_BOOM = 8000;
    /** Where the chests with saved things come (kermis frame, on the floor): at the end of the main street. */
    private static final BlockPos BEWAARPLEK = new BlockPos(15, VLOER + 1, 87);

    /** Klussen at work, by level and template origin. */
    private static final Map<String, Klus> KLUSSEN = new LinkedHashMap<>();

    /** A rectangle in the kermis frame. */
    public record Veld(int x1, int z1, int x2, int z2) {
        public boolean bevat(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }

        /** How far (x, z) lies from the rectangle: 0 inside. */
        public double afstand(int x, int z) {
            return Math.hypot(Math.max(0, Math.max(x1 - x, x - x2)), Math.max(0, Math.max(z1 - z, z - z2)));
        }
    }

    /** Where a kermis stands: its template origin in the world and how it turned. */
    public record Plek(BlockPos oorsprong, Rotation draai) {
        /** A spot of the kermis frame in the world. */
        public BlockPos wereld(BlockPos lokaal) {
            return oorsprong.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
        }

        /** A spot in the world in the kermis frame. */
        public BlockPos lokaal(BlockPos wereld) {
            Rotation terug = switch (draai) {
                case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
                default -> draai;
            };
            return StructureTemplate.transform(wereld.subtract(oorsprong), Mirror.NONE, terug, BlockPos.ZERO);
        }

        /** The floor of the kermis in the world. */
        public int vloer() {
            return oorsprong.getY() + VLOER;
        }

        /** The world box of a field, with room around it. */
        BoundingBox doos(Veld v, int rand, int onder, int boven) {
            BlockPos a = wereld(new BlockPos(v.x1() - rand, VLOER - onder, v.z1() - rand));
            BlockPos b = wereld(new BlockPos(v.x2() + rand, VLOER + boven, v.z2() + rand));
            return BoundingBox.fromCorners(a, b);
        }

        /** The protected boxes. */
        public List<BoundingBox> dozen() {
            return List.of(doos(HOOFD, RAND, ONDER, BOVEN), doos(BOOSTER, RAND, ONDER, BOVEN), doos(BOOSTER_ARM, 0, -17, BOVEN));
        }

        /** Every chunk the work touches. */
        List<ChunkPos> chunks() {
            Set<ChunkPos> uit = new java.util.LinkedHashSet<>();
            for (Veld v : List.of(HOOFD, BOOSTER)) {
                BoundingBox d = doos(v, BAND + 1, 0, 0);
                for (int cx = d.minX() >> 4; cx <= d.maxX() >> 4; cx++) {
                    for (int cz = d.minZ() >> 4; cz <= d.maxZ() >> 4; cz++) {
                        uit.add(new ChunkPos(cx, cz));
                    }
                }
            }
            return new ArrayList<>(uit);
        }
    }

    private KermisUitbouw() {
    }

    private static String sleutel(ServerLevel level, Plek plek) {
        return level.dimension().identifier() + "@" + plek.oorsprong().asLong();
    }

    /** The Kermis-guh's look, every 5 seconds: starts the work here if this kermis has an older extension. */
    public static void kijk(ServerLevel level, Kermis.Area area) {
        if (level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer) {
            return;                                                 // (the tests stamp with bouwNu)
        }
        Plek plek = new Plek(area.origin(), area.rotation());
        KermisUitbouwData data = KermisUitbouwData.van(level);
        int versie = data.versie(plek);
        if (versie >= VERSIE || KLUSSEN.containsKey(sleutel(level, plek))) {
            return;
        }
        if (level.getStructureManager().get(TEMPLATE).isEmpty()) {
            return;                                                 // (error logged by the template manager)
        }
        data.zet(plek, versie);                                     // the ground is protected from now on
        KLUSSEN.put(sleutel(level, plek), new Klus(level, plek, versie == 0));
        LOGGER.info("Guhkermis at {} ({}): the extension starts (version {} -> {})", plek.oorsprong().toShortString(), plek.draai(), versie, VERSIE);
    }

    /** Every tick. */
    static void tick() {
        var it = KLUSSEN.values().iterator();
        while (it.hasNext()) {
            Klus k = it.next();
            try {
                if (k.stap(BUDGET)) {
                    it.remove();
                }
            } catch (RuntimeException e) {
                LOGGER.error("Guhkermis at {}: the extension failed, it tries again after a restart", k.plek.oorsprong().toShortString(), e);
                it.remove();
            }
            break;                                                  // one kermis at a time
        }
    }

    static void stop() {
        KLUSSEN.clear();
    }

    /**
     * (Tests) the whole work at once, chunks loaded at once. True when it stamped.
     */
    public static boolean bouwNu(ServerLevel level, Plek plek) {
        KermisUitbouwData data = KermisUitbouwData.van(level);
        int versie = data.versie(plek);
        if (versie >= VERSIE) {
            return false;
        }
        data.zet(plek, versie);
        for (ChunkPos c : plek.chunks()) {
            level.getChunk(c.x(), c.z());
        }
        Klus k = new Klus(level, plek, versie == 0);
        while (!k.stap(Long.MAX_VALUE)) {
            // (all in one go)
        }
        return true;
    }

    /** (Tests) starts the real work, with its waiting and its budget, as a Kermis-guh would. */
    public static void startNormaal(ServerLevel level, Plek plek) {
        KermisUitbouwData.van(level).zet(plek, 0);
        KLUSSEN.put(sleutel(level, plek), new Klus(level, plek, true));
    }

    /** (Tests) what the work here is doing (null: nothing). */
    public static String stand(ServerLevel level, Plek plek) {
        Klus k = KLUSSEN.get(sleutel(level, plek));
        return k == null ? null : k.fase + " " + k.ontbreekt() + " of " + k.chunks.size() + " chunks missing";
    }

    /** Puts the saved things of players in chests at the end of the main street. */
    private static void bewaar(ServerLevel level, Plek plek, List<ItemStack> bewaard) {
        if (bewaard.isEmpty()) {
            return;
        }
        ArrayDeque<ItemStack> rij = new ArrayDeque<>(bewaard);
        for (int i = 0; !rij.isEmpty(); i++) {
            BlockPos p = plek.wereld(BEWAARPLEK.offset(i % 7, i / 7, 0));
            level.setBlock(p, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(p) instanceof ChestBlockEntity kist) {
                for (int slot = 0; slot < kist.getContainerSize() && !rij.isEmpty(); slot++) {
                    kist.setItem(slot, rij.poll());
                }
                kist.setChanged();
            }
        }
        LOGGER.warn("Guhkermis at {}: {} stacks from containers on the new ground are saved in chests at {}", plek.oorsprong().toShortString(),
                bewaard.size(), plek.wereld(BEWAARPLEK).toShortString());
    }

    /** Whatever got stuck in the new ground stands on it again. */
    private static void opTillen(ServerLevel level, Plek plek) {
        for (BoundingBox d : plek.dozen()) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, AABB.of(d), e -> !level.noCollision(e))) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, e.getBlockX(), e.getBlockZ());
                e.teleportTo(e.getX(), y, e.getZ());
            }
        }
    }

    private static void zeg(ServerLevel level, Plek plek) {
        MinecraftServer server = level.getServer();
        BlockPos midden = plek.wereld(new BlockPos(20, VLOER, 50));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() == level && p.blockPosition().closerThan(midden, 220)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.kermis.groeit"));
            }
        }
    }

    private enum Fase { LADEN, GROND, BOMEN, STEMPEL }

    /** The work at one kermis. */
    static final class Klus {
        final ServerLevel level;
        final Plek plek;
        private final List<ChunkPos> chunks;
        private Fase fase;
        private int wacht;
        private int kx, kz;
        private final ArrayDeque<BlockPos> bomen = new ArrayDeque<>();
        private final Set<Long> gezien = new HashSet<>();
        private int boomBlokken;
        private final List<ItemStack> bewaard = new ArrayList<>();

        Klus(ServerLevel level, Plek plek, boolean grond) {
            this.level = level;
            this.plek = plek;
            this.chunks = plek.chunks();
            this.fase = grond ? Fase.LADEN : Fase.STEMPEL;
            this.kx = HOOFD.x1() - BAND;
            this.kz = BOOSTER.z1() - BAND;
        }

        /** Some of the work; true when it is done. */
        boolean stap(long budget) {
            switch (fase) {
                case LADEN -> {
                    if (wacht-- <= 0) {
                        wacht = 40;
                        boolean klaar = true;
                        for (ChunkPos c : chunks) {
                            level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, c, 0);   // (it runs out by itself)
                            klaar &= level.getChunkSource().getChunkNow(c.x(), c.z()) != null;
                        }
                        if (klaar) {
                            fase = Fase.GROND;
                        }
                    }
                }
                case GROND -> {
                    grond(budget);
                }
                case BOMEN -> {
                    boom(budget);
                }
                case STEMPEL -> {
                    stempel();
                    return true;
                }
            }
            return false;
        }

        long ontbreekt() {
            return chunks.stream().filter(c -> level.getChunkSource().getChunkNow(c.x(), c.z()) == null).count();
        }

        /** Keeps the chunks loaded while this work goes on. */
        private void houdVast() {
            if (wacht-- <= 0) {
                wacht = 100;
                for (ChunkPos c : chunks) {
                    level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, c, 0);
                }
            }
        }

        private void grond(long budget) {
            long tot = budget == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + budget;
            if (budget != Long.MAX_VALUE) {
                houdVast();
            }
            int x2 = HOOFD.x2() + BAND, z2 = HOOFD.z2() + BAND;
            while (kx <= x2) {
                while (kz <= z2) {
                    KermisGrond.kolom(level, plek, kx, kz, this);
                    kz++;
                }
                kz = BOOSTER.z1() - BAND;
                kx++;
                if (budget != Long.MAX_VALUE && System.nanoTime() > tot) {
                    return;
                }
            }
            fase = Fase.BOMEN;
        }

        /** The rest of the trees that were cut. */
        private void boom(long budget) {
            long tot = budget == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + budget;
            if (budget != Long.MAX_VALUE) {
                houdVast();
            }
            int n = 0;
            while (!bomen.isEmpty() && boomBlokken < MAX_BOOM) {
                BlockPos p = bomen.poll();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos q = p.offset(dx, dy, dz);
                            if (gezien.add(q.asLong()) && level.isLoaded(q) && KermisGrond.boom(level.getBlockState(q))
                                    && !vrij(q)) {
                                KermisGrond.weg(level, q, this);
                                boomBlokken++;
                                bomen.add(q);
                            }
                        }
                    }
                }
                if (++n % 32 == 0 && budget != Long.MAX_VALUE && System.nanoTime() > tot) {
                    return;
                }
            }
            bomen.clear();
            fase = Fase.STEMPEL;
        }

        private boolean vrij(BlockPos wereld) {
            BlockPos l = plek.lokaal(wereld);
            return VRIJ.bevat(l.getX(), l.getZ());
        }

        void boomGezien(BlockPos p) {
            gezien.add(p.asLong());
            bomen.add(p.immutable());
        }

        void bewaar(ItemStack stack) {
            bewaard.add(stack);
        }

        private void stempel() {
            StructureTemplate t = level.getStructureManager().get(TEMPLATE).orElseThrow();
            BlockPos min = plek.wereld(PLEK);
            StructurePlaceSettings set = new StructurePlaceSettings().setRotation(plek.draai()).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
            t.placeInWorld(level, min, min, set, level.getRandom(), 2);
            KermisUitbouw.bewaar(level, plek, bewaard);
            opTillen(level, plek);
            KermisUitbouwData.van(level).zet(plek, VERSIE);
            zeg(level, plek);
            LOGGER.info("Guhkermis at {} ({}): extension stamped (version {})", plek.oorsprong().toShortString(), plek.draai(), VERSIE);
        }
    }
}
