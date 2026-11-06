package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;

/**
 * U-GILDEN: the Bondenvallei (V1 "Gildenvallei bij Hoogvads"). ONE shared valley in the Vadsrijk dimension itself, far away in the
 * pink sky (centre {@link #X0}, {@link #Z0}, ground y {@link #Y}): a square with the gate back to Hoogvads and, south of it, a grid
 * of plot cells ({@link #KOLOMMEN} wide, rows grow southwards). Every bond gets the lowest free cell; a cell is a floating island
 * with a road on its north and west side and a 64x64 plot with the castle.
 * <p>Why in the Vadsrijk dimension and not an own one: the Vadsrijk inventory, HUD, adventure rules, character slots, mounts
 * and the airship code all keep working without any change (an own dimension would swap the inventory to the normal world).
 * Scales: a cell is only stamped when a bond claims it, chunks only load when someone is there; 1000 bonds = 167 rows.
 * <p>Stamping: structure templates from {@code tools/vadsrijk/gilden/kasteel.py} ({@code guhs:vadsrijk/gilden/plein|kavel|kasteel_<n>}),
 * placed with known shape OFF so vanilla connects every fence/wall/pane. The railings at the open ends of the roads are code
 * ({@link #randen}): they follow which neighbour cells exist.
 */
public final class Vallei {
    private static final Logger LOG = LogUtils.getLogger();
    public static final int X0 = -12000, Z0 = 12000, Y = 120;
    public static final int KOLOMMEN = 6, CEL = 72, WEG = 8, KAVEL = 64, PLEIN = 28, ONDER = 16;
    /** Columns filled from the middle out, so the north roads of a row always touch. */
    static final int[] VOLGORDE = {2, 3, 1, 4, 0, 5};

    /** Tests: stamp in this level instead of the Vadsrijk (the gametest server has no Vadsrijk dimension). */
    @Nullable
    static volatile ServerLevel TEST;

    private Vallei() {
    }

    /** The level the valley lives in: the Vadsrijk (tests: {@link #TEST}). */
    @Nullable
    public static ServerLevel wereld(MinecraftServer s) {
        return TEST != null ? TEST : Vadsrijk.level(s);
    }

    // ---- geometry ------------------------------------------------------------------------------------------------------------

    public static int kolom(int cel) {
        return VOLGORDE[Math.floorMod(cel, KOLOMMEN)];
    }

    public static int rij(int cel) {
        return cel / KOLOMMEN;
    }

    /** The cell index of a grid position (-1: outside the grid). */
    public static int cel(int kolom, int rij) {
        if (kolom < 0 || kolom >= KOLOMMEN || rij < 0) {
            return -1;
        }
        for (int i = 0; i < KOLOMMEN; i++) {
            if (VOLGORDE[i] == kolom) {
                return rij * KOLOMMEN + i;
            }
        }
        return -1;
    }

    /** North-west corner of a cell, at ground y. */
    public static BlockPos celHoek(int cel) {
        return new BlockPos(X0 - (KOLOMMEN / 2) * CEL + kolom(cel) * CEL, Y, Z0 + PLEIN + rij(cel) * CEL);
    }

    /** North-west corner of the plot (inside the cell), at ground y. */
    public static BlockPos kavelHoek(int cel) {
        return celHoek(cel).offset(WEG, 0, WEG);
    }

    /** The cell at x/z (-1: none: the square or outside). */
    public static int celOp(double x, double z) {
        int kx = (int) Math.floor((x - (X0 - (KOLOMMEN / 2) * CEL)) / CEL), rz = (int) Math.floor((z - (Z0 + PLEIN)) / CEL);
        if (z < Z0 + PLEIN) {
            return -1;
        }
        return cel(kx, rz);
    }

    /** Inside the plot (castle area) of this cell, any height above the ground - 1? */
    public static boolean opKavel(int cel, BlockPos pos) {
        BlockPos h = kavelHoek(cel);
        return pos.getX() >= h.getX() && pos.getX() < h.getX() + KAVEL && pos.getZ() >= h.getZ() && pos.getZ() < h.getZ() + KAVEL
                && pos.getY() >= Y - 1 && pos.getY() < Y + 100;
    }

    /** Anywhere in the valley (square + all rows that could exist)? */
    public static boolean in(double x, double z) {
        return x >= X0 - (KOLOMMEN / 2) * CEL - 40 && x < X0 + (KOLOMMEN / 2) * CEL + 40 && z >= Z0 - PLEIN - 40 && z < Z0 + 20000;
    }

    public static Gebieden.Gebied gebied() {
        return new Gebieden.Gebied("bondenvallei", X0 - (KOLOMMEN / 2) * CEL - 40, Z0 - PLEIN - 40, X0 + (KOLOMMEN / 2) * CEL + 40, Z0 + 20000, 1, 60, true);
    }

    /** Where you arrive on the square (south of the gate, looking south). */
    public static BlockPos pleinAankomst() {
        return new BlockPos(X0, Y + 1, Z0 - 14);
    }

    /** The walk-in gate on the square's north side (the arch, from the template). */
    public static boolean inPoort(double x, double y, double z) {
        return Math.abs(x - (X0 + 0.5)) <= 1.6 && z >= Z0 - 23 && z <= Z0 - 21 && y >= Y + 0.5 && y < Y + 4;
    }

    /** The gate in Hoogvads: the Staartpark's gazebo (POI {@code staartpark_prieel}). */
    public static BlockPos hoogvadsPoort() {
        BlockPos b = Z5WereldPlekken.plek("staartpark_prieel");
        return b != null ? b : new BlockPos(2880, 203, -502);
    }

    // ---- stamping ------------------------------------------------------------------------------------------------------------

    static Optional<StructureTemplate> sjabloon(ServerLevel sl, String naam) {
        return sl.getStructureManager().get(Guhs.id("vadsrijk/gilden/" + naam));
    }

    static boolean stempel(ServerLevel sl, String naam, BlockPos hoek) {
        Optional<StructureTemplate> t = sjabloon(sl, naam);
        if (t.isEmpty()) {
            LOG.warn("[vadsrijk gilden] structure {} missing", naam);
            return false;
        }
        laad(sl, hoek, t.get().getSize().getX(), t.get().getSize().getZ());
        StructurePlaceSettings s = new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(false);
        return t.get().placeInWorld(sl, hoek, hoek, s, sl.getRandom(), Block.UPDATE_CLIENTS);
    }

    /** Loads (generates: the void) the chunks of an area. */
    static void laad(ServerLevel sl, BlockPos hoek, int dx, int dz) {
        for (int cx = hoek.getX() >> 4; cx <= (hoek.getX() + dx) >> 4; cx++) {
            for (int cz = hoek.getZ() >> 4; cz <= (hoek.getZ() + dz) >> 4; cz++) {
                sl.getChunk(cx, cz);
            }
        }
    }

    /** The square, once. */
    static void zorgVoorPlein(ServerLevel sl, Bonden d) {
        if (d.plein) {
            return;
        }
        if (stempel(sl, "plein", new BlockPos(X0 - PLEIN, Y - ONDER, Z0 - PLEIN))) {
            d.plein = true;
            d.setDirty();
        }
    }

    /** Stamps the ground of every cell up to and including this one (cells are claimed in order; a freed one keeps its island). */
    static void zorgVoorKavel(ServerLevel sl, Bonden d, int cel) {
        zorgVoorPlein(sl, d);
        cel = Math.max(cel, 1);   // (cells 0 and 1 together cover the square's whole south edge)
        while (d.gestempeld <= cel) {
            int c = d.gestempeld;
            BlockPos h = celHoek(c);
            if (!stempel(sl, "kavel", new BlockPos(h.getX(), Y - ONDER, h.getZ()))) {
                return;
            }
            d.gestempeld = c + 1;
            d.setDirty();
            randen(sl, d, c);
            for (int[] n : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                int nb = cel(kolom(c) + n[0], rij(c) + n[1]);
                if (nb >= 0 && nb < d.gestempeld) {
                    randen(sl, d, nb);
                }
            }
        }
    }

    static boolean bestaat(Bonden d, int kolom, int rij) {
        int c = cel(kolom, rij);
        return c >= 0 && c < d.gestempeld;
    }

    /**
     * The railings of a cell's roads (cherry fences, connected by vanilla): the north road's outer edge when there is no cell (or
     * square) north of it, the west road's outer edge when there is no cell west of it, and the open road ends east / south.
     */
    static void randen(ServerLevel sl, Bonden d, int cel) {
        BlockPos h = celHoek(cel);
        int k = kolom(cel), r = rij(cel);
        boolean noord = r == 0 ? (h.getX() + CEL > X0 - PLEIN && h.getX() < X0 + PLEIN) : bestaat(d, k, r - 1);
        boolean west = bestaat(d, k - 1, r), oost = bestaat(d, k + 1, r), zuid = bestaat(d, k, r + 1);
        for (int i = 0; i < CEL; i++) {
            // north edge of the north road; where the square only covers part of it, only the part in front of the square is open
            boolean open = noord && (r > 0 || (h.getX() + i >= X0 - PLEIN && h.getX() + i < X0 + PLEIN));
            hek(sl, h.offset(i, 1, 0), !open);
            hek(sl, h.offset(0, 1, i), !west);
        }
        for (int j = 0; j < WEG; j++) {
            hek(sl, h.offset(CEL - 1, 1, j), !oost);   // the north road's east end
            hek(sl, h.offset(j, 1, CEL - 1), !zuid);   // the west road's south end
        }
    }

    private static void hek(ServerLevel sl, BlockPos p, boolean aan) {
        BlockState st = sl.getBlockState(p);
        if (aan) {
            if (st.isAir() && !sl.getBlockState(p.below()).isAir()) {
                BlockState f = Block.updateFromNeighbourShapes(Blocks.CHERRY_FENCE.defaultBlockState(), sl, p);
                sl.setBlock(p, f, Block.UPDATE_ALL);
            }
        } else if (st.is(Blocks.CHERRY_FENCE)) {
            sl.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ---- moving people ---------------------------------------------------------------------------------------------------------

    static void zet(ServerPlayer p, BlockPos pos, float yaw) {
        ServerLevel sl = Vallei.wereld(p.level().getServer());
        if (sl == null) {
            return;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        laad(sl, pos.offset(-8, 0, -8), 16, 16);
        p.teleportTo(sl, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), yaw, 0, true);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.6, 0.8, 0.6, 0.02);
        sl.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** Through the gate in Hoogvads: onto the square (stamps it the first time). */
    public static void naarVallei(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        ServerLevel sl = Vallei.wereld(s);
        if (sl == null) {
            return;
        }
        Bonden d = Bonden.get(s);
        zorgVoorPlein(sl, d);
        zet(p, pleinAankomst(), 180);
        p.sendSystemMessage(Component.translatable("vadsrijk.gilden.vallei.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
        nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.gilden.tip.vallei");
    }

    public static void naarHoogvads(ServerPlayer p) {
        BlockPos g = hoogvadsPoort();
        zet(p, g.offset(0, 0, 3), 180);
    }

    @Nullable
    static BlockPos kasteelAankomst(Bond b) {
        if (b.kavel < 0) {
            return null;
        }
        int[] a = Kasteel.markers(Math.max(1, b.kasteel)).aankomst();
        return kavelHoek(b.kavel).offset(a[0], a[1], a[2]);
    }

    // ---- ticking ---------------------------------------------------------------------------------------------------------------

    /** Every 10 ticks: the walk-in gate on the square, a fall off the islands, gate sparkles. */
    static void tik(MinecraftServer s, long t) {
        ServerLevel sl = Vallei.wereld(s);
        if (sl == null || t % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!in(p.getX(), p.getZ())) {
                continue;
            }
            if (p.getY() < Y - 30 && !p.isSpectator()) {
                zet(p, pleinAankomst(), 180);
                p.sendSystemMessage(Component.translatable("vadsrijk.gilden.vallei.gevangen").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (inPoort(p.getX(), p.getY(), p.getZ())) {
                naarHoogvads(p);
            }
        }
        if (t % 20 == 0) {
            BlockPos g = hoogvadsPoort();
            if (sl.isLoaded(g)) {
                for (int i = 0; i < 8; i++) {
                    double a = (t / 20.0 + i / 8.0) * Math.PI * 2;
                    sl.sendParticles(ParticleTypes.END_ROD, g.getX() + 0.5 + Math.cos(a) * 1.2, g.getY() + 1.5 + Math.sin(a) * 1.2, g.getZ() + 2.5, 1, 0, 0, 0, 0);
                }
            }
            if (sl.isLoaded(new BlockPos(X0, Y, Z0 - 22))) {
                sl.sendParticles(ParticleTypes.PORTAL, X0 + 0.5, Y + 2.2, Z0 - 21.5, 20, 0.8, 1, 0.2, 0.2);
            }
        }
    }
}
