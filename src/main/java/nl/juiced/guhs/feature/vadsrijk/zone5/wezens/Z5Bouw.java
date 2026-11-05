package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The places of the zone-5 combat slice that it builds ITSELF (stand-ins; t-z5wereld builds the city and may replace any of them):
 * <ul>
 *   <li>{@link #buikgangen}: the small dungeon inside the hollow whale (x 2704..2808, y 185..196, z -530..-508): an entrance hall, the
 *       Ribbengang (bone ribs every 5 blocks), the Kleppenzaal (copper pipes), a short passage and the round Ketelkamer (r 14);
 *       walkways of birch planks over whale flesh (pink terracotta), froglights in the ceiling. Real floors, 5 high (the
 *       Ketelkamer 8), no steps, no pits.</li>
 *   <li>{@link #schuilnest}: the secret Mika hideout (35 x 25, 5 high) with barrels, cheese and grey fluff sacks.</li>
 *   <li>{@link #vloeren}: stand-in floors where the city has none yet (Proefarena, fins, tail fluke, the world-boss arena, the
 *       Wolkwaldroom), cloud-white, only into air; and the hatches/lift pads.</li>
 * </ul>
 * EVERYTHING here is placed ONLY INTO AIR (nothing of the city is ever overwritten). The hatches ({@link #luik}) and the fall net
 * ({@link #vangnet}) are runtime rules.
 */
public final class Z5Bouw {
    private Z5Bouw() {
    }

    private static int zet(ServerLevel sl, BlockPos p, BlockState s) {
        sl.getChunk(p);
        if (!sl.getBlockState(p).isAir()) {
            return 0;
        }
        sl.setBlock(p, s, Block.UPDATE_CLIENTS);
        return 1;
    }

    /** Our own decoration: only into air or onto our own whale-flesh shell. */
    private static void eigen(ServerLevel sl, BlockPos p, BlockState s) {
        BlockState nu = sl.getBlockState(p);
        if (nu.isAir() || nu.is(Blocks.PINK_TERRACOTTA) || nu.is(Blocks.PEARLESCENT_FROGLIGHT)) {
            sl.setBlock(p, s, Block.UPDATE_CLIENTS);
        }
    }

    /** One room: a box of air (interior) + its shell (floor, walls, ceiling) into air. Returns blocks placed. */
    private static int kamer(ServerLevel sl, Set<Long> binnen, int x0, int z0, int x1, int z1, int y, int h) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int dy = 0; dy < h; dy++) {
                    binnen.add(BlockPos.asLong(x, y + dy, z));
                }
            }
        }
        return 0;
    }

    private static void rond(Set<Long> binnen, int cx, int cz, int r, int y, int h) {
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r + r) {
                    for (int dy = 0; dy < h; dy++) {
                        binnen.add(BlockPos.asLong(x, y + dy, z));
                    }
                }
            }
        }
    }

    /** Shell around an interior: floor below (walkway or flesh), walls, ceiling (flesh + froglights). */
    private static int schil(ServerLevel sl, Set<Long> binnen, BlockState vloer, BlockState wand, BlockState plafond, BlockState licht, int lichtElke) {
        int n = 0;
        Set<Long> klaar = new HashSet<>();
        for (long l : binnen) {
            BlockPos p = BlockPos.of(l);
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                long ql = q.asLong();
                if (binnen.contains(ql) || !klaar.add(ql)) {
                    continue;
                }
                BlockState s = d == Direction.DOWN ? vloer : d == Direction.UP ? ((q.getX() + q.getZ()) % lichtElke == 0 ? licht : plafond) : wand;
                n += zet(sl, q, s);
            }
            for (int[] dd : new int[][] {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                BlockPos q = p.offset(dd[0], 0, dd[1]);
                long ql = q.asLong();
                if (!binnen.contains(ql) && klaar.add(ql)) {
                    n += zet(sl, q, wand);
                }
            }
        }
        return n;
    }

    /** De Buikgangen (dungeon). */
    public static int buikgangen(ServerLevel sl) {
        int y = Z5WezensPlekken.BUIK_Y;
        Set<Long> binnen = new HashSet<>();
        kamer(sl, binnen, 2706, -526, 2716, -514, y, 5);          // Ingangshal
        kamer(sl, binnen, 2717, -523, 2752, -517, y, 5);          // Ribbengang
        kamer(sl, binnen, 2753, -528, 2769, -512, y, 6);          // Kleppenzaal
        kamer(sl, binnen, 2770, -522, 2779, -518, y, 5);          // passage
        BlockPos k = Z5WezensPlekken.KETELKAMER;
        rond(binnen, k.getX(), k.getZ(), 14, y, 8);               // Ketelkamer
        int n = schil(sl, binnen, Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState(), Blocks.PINK_TERRACOTTA.defaultBlockState(),
                Blocks.PINK_TERRACOTTA.defaultBlockState(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), 5);
        // ribs along the Ribbengang: bone arches every 5 blocks (inside the walls, so the path stays 7 wide)
        for (int x = 2720; x <= 2750; x += 5) {
            for (int dy = 0; dy < 6; dy++) {
                eigen(sl, new BlockPos(x, y + dy, -524), Blocks.BONE_BLOCK.defaultBlockState());
                eigen(sl, new BlockPos(x, y + dy, -516), Blocks.BONE_BLOCK.defaultBlockState());
            }
            for (int z = -524; z <= -516; z++) {
                eigen(sl, new BlockPos(x, y + 5, z), Blocks.BONE_BLOCK.defaultBlockState());
            }
        }
        // copper pipes on the Kleppenzaal walls
        for (int x = 2754; x <= 2768; x += 3) {
            for (int dy = 0; dy < 6; dy++) {
                eigen(sl, new BlockPos(x, y + dy, -529), Blocks.WAXED_CUT_COPPER.defaultBlockState());
                eigen(sl, new BlockPos(x, y + dy, -511), Blocks.WAXED_CUT_COPPER.defaultBlockState());
            }
        }
        // the exit hatch inside + the hatch on the street (with a pad if the city is not there yet)
        n += luikBlok(sl, Z5WezensPlekken.BUIK_UITLUIK);
        n += luikBlok(sl, Z5WezensPlekken.BUIK_LUIK);
        return n;
    }

    /** Het Schuilnest. */
    public static int schuilnest(ServerLevel sl) {
        int y = Z5WezensPlekken.NEST_Y;
        Set<Long> binnen = new HashSet<>();
        kamer(sl, binnen, 2589, -439, 2623, -415, y, 5);
        int n = schil(sl, binnen, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState(),
                Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 6);
        // crates, cheese and grey fluff sacks along the walls (never on the walking lines)
        for (int x = 2592; x <= 2620; x += 4) {
            n += zet(sl, new BlockPos(x, y, -438), Blocks.BARREL.defaultBlockState());
            n += zet(sl, new BlockPos(x + 1, y, -438), (x / 4) % 2 == 0 ? Blocks.YELLOW_WOOL.defaultBlockState() : Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
            n += zet(sl, new BlockPos(x, y, -416), Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
        }
        n += zet(sl, new BlockPos(2606, y, -427), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        n += luikBlok(sl, Z5WezensPlekken.NEST_UITLUIK);
        n += luikBlok(sl, Z5WezensPlekken.NEST_LUIK);
        return n;
    }

    private static int luikBlok(ServerLevel sl, BlockPos p) {
        int n = 0;
        if (sl.getBlockState(p.below()).isAir()) {
            n += schijf(sl, p.below(), 1, Blocks.WHITE_CONCRETE.defaultBlockState());
        }
        return n + zet(sl, p, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
    }

    private static int schijf(ServerLevel sl, BlockPos c, int r, BlockState s) {
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z <= r * r + r) {
                    n += zet(sl, c.offset(x, 0, z), s);
                }
            }
        }
        return n;
    }

    /** Stand-in floors (cloud-white) for the open places + the lift pads, only into air. */
    public static int vloeren(ServerLevel sl) {
        BlockState wolk = Blocks.WHITE_WOOL.defaultBlockState();
        int n = 0;
        BlockPos pa = Z5WezensPlekken.PROEFARENA;
        n += schijf(sl, pa.below(), Z5WezensPlekken.PROEF_STRAAL + 1, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        // a low fence ring with four wide gaps (no fence ever on a path)
        int r = Z5WezensPlekken.PROEF_STRAAL + 1;
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI * 2 / 96;
            if (Math.abs(Math.sin(a * 2)) < 0.3) {
                continue;
            }
            n += zet(sl, pa.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)), Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        for (BlockPos v : List.of(Z5WezensPlekken.ZUIDVIN, Z5WezensPlekken.NOORDVIN)) {
            n += schijf(sl, v.below(), 13, wolk);
        }
        n += schijf(sl, Z5WezensPlekken.STAARTVLAKTE.below(), 15, wolk);
        n += schijf(sl, Z5WezensPlekken.DONDERBUIK.below(), Z5WezensPlekken.DONDER_STRAAL + 2, wolk);
        n += schijf(sl, Z5WezensPlekken.WOLKWALDROOM.below(), Z5WezensPlekken.DROOM_STRAAL + 2, Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
        for (Z5WezensPlekken.Luik l : Z5WezensPlekken.LUIKEN) {
            if (l.id().endsWith("_af")) {
                n += schijf(sl, l.klik().below(), 1, wolk);
                n += zet(sl, l.klik(), Blocks.WHITE_GLAZED_TERRACOTTA.defaultBlockState());
                n += schijf(sl, l.naar().below(), 2, wolk);
                n += zet(sl, l.naar().offset(2, 0, 0), Blocks.WHITE_GLAZED_TERRACOTTA.defaultBlockState());
            }
        }
        return n;
    }

    // ---- the hatches and lifts ------------------------------------------------------------------------------------------------------

    /** Right-click on a hatch/lift block: off you go (cancels the click). */
    public static void luik(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return;
        }
        Z5WezensPlekken.Luik l = luikBij(e.getPos());
        if (l == null) {
            return;
        }
        e.setCanceled(true);
        ga(p, sl, l);
    }

    public static Z5WezensPlekken.Luik luikBij(BlockPos pos) {
        for (Z5WezensPlekken.Luik l : Z5WezensPlekken.LUIKEN) {
            if (l.klik().distManhattan(pos) <= 1) {
                return l;
            }
        }
        return null;
    }

    public static void ga(ServerPlayer p, ServerLevel sl, Z5WezensPlekken.Luik l) {
        BlockPos n = l.naar();
        sl.getChunk(n);
        sl.playSound(null, p.blockPosition(), SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1f, 0.9f);
        p.teleportTo(sl, n.getX() + 0.5, n.getY() + 0.1, n.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
        p.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.luik." + l.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static final Map<UUID, Long> HINT = new ConcurrentHashMap<>();

    /** Every 10 ticks: hatch hints nearby, and the fall net at the cloud edge. */
    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!Z5WezensPlekken.inZone(p.getX(), p.getY() + 60, p.getZ())) {
                continue;
            }
            vangnet(p, sl);
            for (Z5WezensPlekken.Luik l : Z5WezensPlekken.LUIKEN) {
                if (l.klik().distToCenterSqr(p.position()) < 9) {
                    Long vorig = HINT.get(p.getUUID());
                    if (vorig == null || sl.getGameTime() - vorig > 80) {
                        HINT.put(p.getUUID(), sl.getGameTime());
                        p.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.luik.hint." + l.id()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
    }

    /** A player who falls off a fin or the tail fluke is caught by a cloud and put back at the lift (never a long fall into the sea). */
    public static boolean vangnet(ServerPlayer p, ServerLevel sl) {
        if (p.isCreative() || p.isSpectator()) {
            return false;
        }
        for (Map.Entry<String, BlockPos> e : Z5WezensPlekken.VANGNET.entrySet()) {
            Gebieden.Gebied g = Z5WezensPlekken.gebied(e.getKey());
            if (g == null || g.laag() == null || !g.bevat(p.getX(), p.getZ())) {
                continue;
            }
            int bodem = g.laag().minY();
            if (p.getY() < bodem && p.getY() > bodem - 90) {
                BlockPos t = e.getValue();
                p.teleportTo(sl, t.getX() + 0.5, t.getY() + 0.1, t.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
                p.resetFallDistance();
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
                p.sendOverlayMessage(Component.translatable("vadsrijk.z5wezens.vangnet").withStyle(ChatFormatting.LIGHT_PURPLE));
                sl.playSound(null, t, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
                return true;
            }
        }
        return false;
    }
}
