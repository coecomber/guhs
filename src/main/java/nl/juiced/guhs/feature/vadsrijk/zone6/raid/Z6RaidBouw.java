package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;

/**
 * U-Z6RAID: the blocks of the binnentoren. The template {@code guhs:vadsrijk/zone6/raid_binnentoren} (made by
 * tools/vadsrijk/zone6/raid/binnentoren.py, with the u-hekken connection pass) is placed ONCE at the anchor (saved version
 * {@link #VERSIE} in {@link Z6RaidData}; a newer generator bumps the version and it is placed again), when a player comes within
 * 160 blocks of the anchor and the chunks exist; or with {@code /guhs vadsrijk z6raid bouw}. Known shape (connection states kept
 * as written), no entities.
 * <p>The live parts: the sterrenvlies gates ({@link #poort}: purple glass on the first steps of each floor's stair up) and the
 * stars of the roof dome ({@link #sterren}: light grey glass = out, glowstone = burning again).
 */
public final class Z6RaidBouw {
    /** Bump when binnentoren.py changes (the template is placed again over the old one). */
    public static final int VERSIE = 1;
    public static final Identifier TEMPLATE = Guhs.id("vadsrijk/zone6/raid_binnentoren");
    public static final BlockState POORT = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
    public static final BlockState STER_UIT = Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState(), STER_AAN = Blocks.GLOWSTONE.defaultBlockState();
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static long volgendeKijk;

    private Z6RaidBouw() {
    }

    /** Server tick (from Z6Raid, every 10 ticks): place the template when someone is near and it is not there yet. */
    static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        if (nu < volgendeKijk) {
            return;
        }
        volgendeKijk = nu + 100;
        if (Z6RaidData.get(sl.getServer()).bouwVersie >= VERSIE) {
            return;
        }
        BlockPos a = Z6RaidPlekken.anker();
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(a) < 160 * 160) {
                plaats(sl);
                return;
            }
        }
    }

    /** Places the template (loads/generates the chunks it needs). Returns false when the template is missing. */
    public static boolean plaats(ServerLevel sl) {
        Optional<StructureTemplate> t = sl.getStructureManager().get(TEMPLATE);
        if (t.isEmpty()) {
            LOG.warn("U-Z6RAID: template {} missing", TEMPLATE);
            return false;
        }
        StructureTemplate tpl = t.get();
        BlockPos hoek = Z6RaidPlekken.anker().offset(Z6RaidPlekken.TEMPLATE_MIN);
        var maat = tpl.getSize();
        for (int cx = hoek.getX() >> 4; cx <= (hoek.getX() + maat.getX()) >> 4; cx++) {
            for (int cz = hoek.getZ() >> 4; cz <= (hoek.getZ() + maat.getZ()) >> 4; cz++) {
                sl.getChunk(cx, cz);
            }
        }
        BoundingBox doos = new BoundingBox(hoek.getX(), hoek.getY(), hoek.getZ(), hoek.getX() + maat.getX() - 1, hoek.getY() + maat.getY() - 1,
                hoek.getZ() + maat.getZ() - 1);
        StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(doos).setIgnoreEntities(true).setKnownShape(true);
        tpl.placeInWorld(sl, hoek, hoek, s, sl.getRandom(), 2);
        Z6RaidData d = Z6RaidData.get(sl.getServer());
        d.zet(() -> d.bouwVersie = VERSIE);
        if (d.sterrenAan) {
            sterren(sl, true);
        }
        LOG.info("U-Z6RAID: binnentoren v{} placed at {}", VERSIE, hoek);
        return true;
    }

    /** Opens (false) or closes (true) the sterrenvlies gate of floor k (0-3). */
    public static void poort(ServerLevel sl, int k, boolean dicht) {
        BlockPos[] l = Z6RaidPlekken.poortBlokken(k);
        for (BlockPos b : l) {
            BlockState nu = sl.getBlockState(b);
            if (dicht && nu.isAir()) {
                sl.setBlock(b, POORT, 3);
            } else if (!dicht && nu.is(Blocks.PURPLE_STAINED_GLASS)) {
                sl.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (!dicht) {
            BlockPos m = l[1];
            Vfx.deeltjes(sl, Deeltje.STER, net.minecraft.world.phys.Vec3.atCenterOf(m), 40, 0xE0B0FF, 1.2, 0.08);
            sl.playSound(null, m, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2f, 1.2f);
        }
    }

    /** Is the gate of floor k open? */
    public static boolean poortOpen(ServerLevel sl, int k) {
        return sl.getBlockState(Z6RaidPlekken.poortBlokken(k)[1]).isAir();
    }

    /** Lights (or puts out) every star in the roof dome. Returns how many changed. */
    public static int sterren(ServerLevel sl, boolean aan) {
        BlockPos m = Z6RaidPlekken.midden(4);
        BlockState van = aan ? STER_UIT : STER_AAN, naar = aan ? STER_AAN : STER_UIT;
        int n = 0;
        if (!sl.hasChunkAt(m)) {
            return 0;   // (not loaded: placing the template later lights them from Z6RaidData.sterrenAan)
        }
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                for (int y = 12; y <= 18; y++) {
                    BlockPos b = m.offset(x, y, z);
                    if (sl.getBlockState(b).equals(van)) {
                        sl.setBlock(b, naar, 2);
                        n++;
                    }
                }
            }
        }
        return n;
    }
}
