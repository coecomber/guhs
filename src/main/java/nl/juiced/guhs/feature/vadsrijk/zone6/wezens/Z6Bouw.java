package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * What the zone-6 combat slice builds ITSELF:
 * <ul>
 *   <li>{@link #sterrenwacht}: the INSIDE of the dungeon De Verzonken Sterrenwacht, as its own rooms under the Ruïnedal (floor
 *       y {@value Z6WezensPlekken#KERKER_Y}, x 3940..4061, z -983..-959): the Ingangshal, a corridor, the round Ringenzaal (boss 1),
 *       the Kaartenzaal (trash, star maps), the Vuurschalenzaal (boss 2), and the domed Koepel with a lapis star sky (boss 3).
 *       Flat floors everywhere (no steps, no pits), corridors 5 wide and 5 high, light in every ceiling. Built ONCE and kept (a
 *       marker block at the arrival spot tells it is there); also built on the fly when someone uses the hatch or is found
 *       inside without rooms. The surface ruin (the half-sunk dome with the broken telescope) is u-z6wereld's: its hatch is the
 *       POI {@code z6w_sterrenwacht_ingang}.</li>
 *   <li>the hatches ({@link #luik}): right-click the hatch at the ruin = down into the Ingangshal; the ladder hatch in the
 *       Ingangshal or the one behind the Koepel = back up (slow falling, a short message; a grey hint within 3 blocks);</li>
 *   <li>{@link #arenaVloeren}: dev only: sand discs under the two field-boss arenas while the map is not there yet (only into air
 *       or water; undone by the bazen Herstel at the next server start).</li>
 * </ul>
 */
public final class Z6Bouw {
    private Z6Bouw() {
    }

    private static final BlockState LUCHT = Blocks.AIR.defaultBlockState();
    private static final BlockState MARKER = Blocks.WAXED_CHISELED_COPPER.defaultBlockState();

    /** Is the dungeon built (the marker under the arrival spot)? */
    public static boolean gebouwd(ServerLevel sl) {
        BlockPos m = Z6WezensPlekken.KERKER_AANKOMST.below();
        sl.getChunk(m);
        return sl.getBlockState(m).is(Blocks.WAXED_CHISELED_COPPER) && sl.getBlockState(Z6WezensPlekken.KERKER_AANKOMST).isAir();
    }

    // ---- the rooms -------------------------------------------------------------------------------------------------------------

    private static void doos(Set<Long> binnen, int x0, int z0, int x1, int z1, int h) {
        int y = Z6WezensPlekken.KERKER_Y;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int dy = 0; dy < h; dy++) {
                    binnen.add(BlockPos.asLong(x, y + dy, z));
                }
            }
        }
    }

    private static void rond(Set<Long> binnen, BlockPos c, int r, int h, boolean koepel) {
        int y = Z6WezensPlekken.KERKER_Y;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > r + 0.4) {
                    continue;
                }
                int hoog = koepel ? (int) Math.round(h - (h - 6) * (d / r) * (d / r)) : h;
                for (int dy = 0; dy < hoog; dy++) {
                    binnen.add(BlockPos.asLong(c.getX() + x, y + dy, c.getZ() + z));
                }
            }
        }
    }

    /** All air cells of the dungeon. */
    public static Set<Long> ruimte() {
        Set<Long> b = new HashSet<>();
        doos(b, 3940, -976, 3954, -966, 6);                                    // Ingangshal
        doos(b, 3955, -973, 3962, -969, 5);                                    // gang 1
        rond(b, Z6WezensPlekken.RINGENZAAL, Z6WezensPlekken.RINGEN_STRAAL + 1, 8, false);   // Ringenzaal
        doos(b, 3981, -973, 3989, -969, 5);                                    // gang 2
        doos(b, 3989, -979, 4006, -963, 7);                                    // Kaartenzaal
        doos(b, 4007, -973, 4011, -969, 5);                                    // gang 3
        int v = Z6WezensPlekken.VUUR_HALF + 1;
        BlockPos vz = Z6WezensPlekken.VUURSCHALENZAAL;
        doos(b, vz.getX() - v, vz.getZ() - v, vz.getX() + v, vz.getZ() + v, 8);   // Vuurschalenzaal
        doos(b, 4033, -973, 4037, -969, 5);                                    // gang 4
        rond(b, Z6WezensPlekken.KOEPEL, Z6WezensPlekken.KOEPEL_STRAAL + 1, 14, true);   // Koepel
        return b;
    }

    private static final Direction[] RICHTINGEN = Direction.values();

    /** Builds the dungeon (once; forced = again over the old one). Returns blocks placed (0 = it was there). */
    public static int sterrenwacht(ServerLevel sl) {
        return sterrenwacht(sl, false);
    }

    public static int sterrenwacht(ServerLevel sl, boolean opnieuw) {
        if (!opnieuw && gebouwd(sl)) {
            return 0;
        }
        Set<Long> binnen = ruimte();
        int n = 0;
        int vloerY = Z6WezensPlekken.KERKER_Y - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Set<Long> schil = new HashSet<>();
        for (long l : binnen) {
            BlockPos p = BlockPos.of(l);
            for (Direction d : RICHTINGEN) {
                long q = BlockPos.offset(l, d);
                if (!binnen.contains(q)) {
                    schil.add(q);
                    long q2 = BlockPos.offset(q, d);   // a second layer, so no water or cave ever leaks in
                    if (!binnen.contains(q2)) {
                        schil.add(q2);
                    }
                }
            }
            sl.getChunk(p);
        }
        for (long l : schil) {
            BlockPos p = BlockPos.of(l);
            BlockState s = p.getY() <= vloerY ? vloer(p) : p.getY() >= Z6WezensPlekken.KERKER_Y + 5 ? plafond(p, binnen) : muur(p);
            sl.setBlock(p, s, Block.UPDATE_CLIENTS);
            n++;
        }
        for (long l : binnen) {
            m.set(l);
            if (!sl.getBlockState(m).isAir() || !sl.getFluidState(m).isEmpty()) {
                sl.setBlock(m, LUCHT, Block.UPDATE_CLIENTS);
                n++;
            }
        }
        n += inrichting(sl);
        sl.setBlock(Z6WezensPlekken.KERKER_AANKOMST.below(), MARKER, Block.UPDATE_CLIENTS);
        return n;
    }

    private static BlockState vloer(BlockPos p) {
        int x = p.getX(), z = p.getZ();
        double dr = Math.sqrt(p.distSqr(Z6WezensPlekken.RINGENZAAL.atY(p.getY())));
        if (Math.abs(dr - 6) < 0.5 || Math.abs(dr - 9) < 0.5) {
            return Blocks.WAXED_CUT_COPPER.defaultBlockState();   // brass rings in the Ringenzaal floor
        }
        double dk = Math.sqrt(p.distSqr(Z6WezensPlekken.KOEPEL.atY(p.getY())));
        if (dk < 12.5) {
            return dk < 2.5 ? Blocks.GOLD_BLOCK.defaultBlockState() : Math.abs(dk - 8) < 0.5 ? Blocks.WAXED_CUT_COPPER.defaultBlockState()
                    : Blocks.SMOOTH_QUARTZ.defaultBlockState();
        }
        if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) {
            return Blocks.CHISELED_SANDSTONE.defaultBlockState();
        }
        return (x + z) % 2 == 0 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.CUT_SANDSTONE.defaultBlockState();
    }

    private static BlockState muur(BlockPos p) {
        int y = p.getY() - Z6WezensPlekken.KERKER_Y;
        if (y == 1) {
            return Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();   // a rose band at hip height
        }
        if (y == 3 && Math.floorMod(p.getX() + p.getZ(), 6) == 0) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        }
        return Blocks.CUT_SANDSTONE.defaultBlockState();
    }

    private static BlockState plafond(BlockPos p, Set<Long> binnen) {
        double dk = Math.sqrt(p.distSqr(Z6WezensPlekken.KOEPEL.atY(p.getY())));
        if (dk < 14) {   // the star sky of the dome
            int h = Math.floorMod(p.getX() * 73856093 ^ p.getZ() * 19349663 ^ p.getY() * 83492791, 9);
            return h == 0 ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.LAPIS_BLOCK.defaultBlockState();
        }
        if (binnen.contains(BlockPos.asLong(p.getX(), p.getY() - 1, p.getZ())) && Math.floorMod(p.getX(), 5) == 0 && Math.floorMod(p.getZ(), 4) == 1) {
            return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
        }
        return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    }

    /** The furniture: hatches, star maps, bookshelves (all against walls, never on a path). */
    private static int inrichting(ServerLevel sl) {
        int n = 0;
        int y = Z6WezensPlekken.KERKER_Y;
        // the way back up: a trapdoor-ladder in the Ingangshal (west wall) and behind the Koepel (east)
        for (BlockPos l : List.of(terugLuik(), Z6WezensPlekken.KERKER_UITGANG)) {
            Direction f = l.getX() < Z6WezensPlekken.RINGENZAAL.getX() ? Direction.EAST : Direction.WEST;
            for (int dy = 0; dy < 4; dy++) {
                sl.setBlock(l.above(dy), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, f), Block.UPDATE_CLIENTS);
                n++;
            }
        }
        // star maps on the Kaartenzaal walls (blue glazed terracotta) and bookshelves between
        for (int x = 3991; x <= 4004; x += 3) {
            for (int z : new int[] {-980, -962}) {
                sl.setBlock(new BlockPos(x, y + 2, z), Blocks.BLUE_GLAZED_TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
                sl.setBlock(new BlockPos(x + 1, y + 2, z), Blocks.CHISELED_BOOKSHELF.defaultBlockState(), Block.UPDATE_CLIENTS);
                n += 2;
            }
        }
        return n;
    }

    // ---- the hatches ---------------------------------------------------------------------------------------------------------------

    public record Luik(String id, BlockPos klik, @Nullable BlockPos naar) {
    }

    static BlockPos terugLuik() {
        return Z6WezensPlekken.KERKER_AANKOMST.offset(-3, 0, 0);
    }

    /** The surface spot above the hatch (heightmap, so it is right on any map). */
    public static BlockPos boven(ServerLevel sl) {
        BlockPos i = Z6WezensPlekken.STERRENWACHT_INGANG;
        sl.getChunk(i);
        int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, i.getX() + 1, i.getZ());
        return new BlockPos(i.getX() + 1, Math.max(top, i.getY()), i.getZ());
    }

    public static List<Luik> luiken() {
        return List.of(new Luik("in", Z6WezensPlekken.STERRENWACHT_INGANG, Z6WezensPlekken.KERKER_AANKOMST),
                new Luik("uit", terugLuik(), null), new Luik("uit", Z6WezensPlekken.KERKER_UITGANG, null));
    }

    @Nullable
    public static Luik luikBij(BlockPos pos) {
        for (Luik l : luiken()) {
            if (l.klik().distManhattan(pos) <= 1) {
                return l;
            }
        }
        return null;
    }

    public static void luik(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return;
        }
        Luik l = luikBij(e.getPos());
        if (l == null) {
            return;
        }
        e.setCanceled(true);
        ga(p, sl, l);
    }

    public static void ga(ServerPlayer p, ServerLevel sl, Luik l) {
        BlockPos n;
        if (l.naar() != null) {
            if (!gebouwd(sl)) {
                sterrenwacht(sl);
            }
            n = l.naar();
        } else {
            n = boven(sl);
        }
        sl.getChunk(n);
        sl.playSound(null, p.blockPosition(), SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1f, 0.9f);
        p.teleportTo(sl, n.getX() + 0.5, n.getY() + 0.1, n.getZ() + 0.5, Set.of(), l.naar() != null ? 270 : p.getYRot(), p.getXRot(), true);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
        p.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.luik." + l.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static final Map<UUID, Long> HINT = new ConcurrentHashMap<>();

    /** Every 10 ticks: the hatch hints, and the rooms are built for anyone found inside them without rooms (after a reset). */
    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!Z6WezensPlekken.inZone(p.getX(), p.getZ())) {
                continue;
            }
            if (Z6WezensPlekken.inKerker(p.getX(), p.getY(), p.getZ()) && !p.isSpectator() && !gebouwd(sl)) {
                sterrenwacht(sl);
            }
            for (Luik l : luiken()) {
                if (l.klik().distToCenterSqr(p.position()) < 9) {
                    Long vorig = HINT.get(p.getUUID());
                    if (vorig == null || sl.getGameTime() - vorig > 80) {
                        HINT.put(p.getUUID(), sl.getGameTime());
                        p.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.luik.hint." + l.id()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
    }

    // ---- dev: stand-in floors for the field-boss arenas ------------------------------------------------------------------------

    /** Sand discs under both field-boss arenas (only into air/water; Herstel key z6_arenas, undone at the next start). */
    public static int arenaVloeren(ServerLevel sl) {
        int n = 0;
        for (BlockPos[] a : new BlockPos[][] {{Z6WezensPlekken.WORMELIEN, Z6WezensPlekken.WORMELIEN_DEUR}, {Z6WezensPlekken.ZOUTKOLOS, Z6WezensPlekken.ZOUTKOLOS_DEUR}}) {
            BlockPos c = a[0];
            int r = (int) Math.ceil(Math.sqrt(c.distSqr(a[1]))) + 3;
            BlockState s = a[0] == Z6WezensPlekken.WORMELIEN ? Blocks.SAND.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) {
                        continue;
                    }
                    BlockPos p = c.offset(x, -1, z);
                    sl.getChunk(p);
                    BlockState nu = sl.getBlockState(p);
                    if (nu.isAir() || !sl.getFluidState(p).isEmpty()) {
                        Herstel.zet(sl, "z6_arenas", p, s);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** (unused trapdoor state, kept for u-z6wereld: the hatch block they may place at z6w_sterrenwacht_ingang) */
    public static BlockState luikBlok() {
        return Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH);
    }
}
