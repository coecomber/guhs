package nl.juiced.guhs.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.FryingPanBlock;
import nl.juiced.guhs.block.GuhPortalBlock;
import nl.juiced.guhs.feature.huisje.HuisjeDeelBlock;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenBlocks;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature;
import nl.juiced.guhs.feature.knabbelvelden.KnabbelportaalBlock;
import nl.juiced.guhs.feature.knabbelvelden.KnabbelveldenFeature;
import nl.juiced.guhs.feature.safari.rit.SafariPoortBlock;
import nl.juiced.guhs.feature.safari.rit.SafariRitten;
import nl.juiced.guhs.feature.sjoelen.SjoelenBlocks;
import nl.juiced.guhs.feature.sjoelen.SjoelenFeature;
import nl.juiced.guhs.feature.speelgoed.SpeelDeelBlock;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.speelgoed.TunnelBlock;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * 1.7.0: blocks in a structure placed turned or mirrored turn along. Every guhs block with a direction-dependent property
 * (a Direction, an Axis, north/east/south/west sides or a 16-step rotation) must change it the vanilla way under
 * {@code rotate(CLOCKWISE_90)} and {@code mirror(FRONT_BACK / LEFT_RIGHT)}; {@code Block.rotate} keeps the state as it is,
 * so a block class without an override would stay pointing the old way. Multi-block parts must still find each other.
 */
public class DraaiGameTests {
    private static final String BATCH = "draai";
    private static final String EMPTY = "empty";
    private static final Map<Direction, String> SIDES = Map.of(Direction.NORTH, "north", Direction.EAST, "east",
            Direction.SOUTH, "south", Direction.WEST, "west");
    /**
     * Blocks that turn but deliberately don't mirror: a slee rail piece's blocks (SleePath.cells) are not left/right
     * symmetric (x -1..2), so a mirrored piece would not fit its own parts; its parts don't mirror either.
     */
    private static final Set<String> NIET_SPIEGELEN = Set.of(Guhs.MODID + ":slee_rail");
    private static final Rotation[] DRAAIEN = {Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final Mirror[] SPIEGELS = {Mirror.FRONT_BACK, Mirror.LEFT_RIGHT};

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void draaiAlleGuhsBlokkenDraaienMee(GameTestHelper helper) {
        List<String> fouten = new ArrayList<>();
        int bekeken = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (!id.startsWith(Guhs.MODID + ":")) {
                continue;
            }
            boolean relevant = false;
            for (Property<?> p : block.getStateDefinition().getProperties()) {
                relevant |= kind(p) != null;
            }
            if (!relevant) {
                continue;
            }
            bekeken++;
            String fout = null;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                fout = check(state, !NIET_SPIEGELEN.contains(id));
                if (fout != null) {
                    break;
                }
            }
            if (fout != null) {
                fouten.add(id + " " + fout);
            }
        }
        helper.assertTrue(bekeken > 20, "the sweep found guhs blocks with a direction property: " + bekeken);
        helper.assertTrue(fouten.isEmpty(), fouten.size() + " blocks don't turn along: " + String.join("; ", fouten));
        helper.succeed();
    }

    /** A few fixed blocks by hand: the property changes as expected. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void draaiVoorbeelden(GameTestHelper helper) {
        BlockState pan = ModBlocks.FRYING_PAN.get().defaultBlockState().setValue(FryingPanBlock.FACING, Direction.NORTH);
        helper.assertTrue(pan.rotate(Rotation.CLOCKWISE_90).getValue(FryingPanBlock.FACING) == Direction.EAST, "the frying pan turns north -> east");
        helper.assertTrue(pan.mirror(Mirror.LEFT_RIGHT).getValue(FryingPanBlock.FACING) == Direction.SOUTH, "and mirrors north -> south");

        BlockState portaal = ModBlocks.GUH_PORTAL.get().defaultBlockState().setValue(GuhPortalBlock.AXIS, Direction.Axis.X);
        helper.assertTrue(portaal.rotate(Rotation.CLOCKWISE_90).getValue(GuhPortalBlock.AXIS) == Direction.Axis.Z, "the guh portal turns x -> z");
        helper.assertTrue(portaal.rotate(Rotation.CLOCKWISE_180).getValue(GuhPortalBlock.AXIS) == Direction.Axis.X, "and stays x after half a turn");

        BlockState tunnel = SpeelgoedFeature.TUNNEL.get().defaultBlockState().setValue(TunnelBlock.NOORD, TunnelBlock.Kant.INGANG)
                .setValue(TunnelBlock.OOST, TunnelBlock.Kant.OPEN).setValue(TunnelBlock.ZUID, TunnelBlock.Kant.DICHT)
                .setValue(TunnelBlock.WEST, TunnelBlock.Kant.DICHT).setValue(TunnelBlock.AXIS, Direction.Axis.Z);
        BlockState t = tunnel.rotate(Rotation.CLOCKWISE_90);
        helper.assertTrue(t.getValue(TunnelBlock.OOST) == TunnelBlock.Kant.INGANG && t.getValue(TunnelBlock.ZUID) == TunnelBlock.Kant.OPEN
                && t.getValue(TunnelBlock.WEST) == TunnelBlock.Kant.DICHT && t.getValue(TunnelBlock.NOORD) == TunnelBlock.Kant.DICHT
                && t.getValue(TunnelBlock.AXIS) == Direction.Axis.X, "the tunnel's sides go round with it: " + t);
        BlockState tm = tunnel.mirror(Mirror.FRONT_BACK);
        helper.assertTrue(tm.getValue(TunnelBlock.WEST) == TunnelBlock.Kant.OPEN && tm.getValue(TunnelBlock.OOST) == TunnelBlock.Kant.DICHT
                && tm.getValue(TunnelBlock.NOORD) == TunnelBlock.Kant.INGANG, "the mirrored tunnel opens to the west: " + tm);

        BlockState kop = SjoelenFeature.KOP.get().defaultBlockState().setValue(SjoelenBlocks.DEEL, 1);
        helper.assertTrue(kop.mirror(Mirror.LEFT_RIGHT).getValue(SjoelenBlocks.DEEL) == 3, "a mirrored sjoelbak numbers its blocks the other way");
        helper.assertTrue(kop.rotate(Rotation.CLOCKWISE_90).getValue(SjoelenBlocks.DEEL) == 1, "but a turned one doesn't");

        BlockState blik = KnabbelspelenFeature.BLIK.get().defaultBlockState().setValue(KnabbelspelenBlocks.Blik.SCHUIF, KnabbelspelenBlocks.Schuif.LINKS);
        helper.assertTrue(blik.mirror(Mirror.FRONT_BACK).getValue(KnabbelspelenBlocks.Blik.SCHUIF) == KnabbelspelenBlocks.Schuif.RECHTS,
                "a mirrored tin shifts to the other side");
        helper.succeed();
    }

    /** Multi-block parts turned/mirrored around the origin (as a structure template does) still find their controller. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void draaiDelenVindenElkaar(GameTestHelper helper) {
        List<String> fouten = new ArrayList<>();
        // the Knabbelportaal (2 x 2, deel bit 0 along the axis) and the safaripoort (4 x 6, deel_x along the axis)
        for (Direction.Axis as : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction langs = as == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            BlockPos basis = new BlockPos(1, 0, 2);
            List<BlockPos> posities = new ArrayList<>();
            List<BlockState> states = new ArrayList<>();
            for (int deel = 0; deel < 4; deel++) {
                posities.add(basis.relative(langs, deel & 1).above(deel >> 1));
                states.add(KnabbelveldenFeature.PORTAAL.get().defaultBlockState().setValue(KnabbelportaalBlock.AS, as).setValue(KnabbelportaalBlock.DEEL, deel));
            }
            delen(fouten, "knabbelportaal " + as, posities, states, (s, p) -> KnabbelportaalBlock.basis(s, p),
                    s -> s.getValue(KnabbelportaalBlock.DEEL));

            posities = new ArrayList<>();
            states = new ArrayList<>();
            for (int dy = 0; dy < SafariPoortBlock.HOOG; dy++) {
                for (int dx = 0; dx < SafariPoortBlock.BREED; dx++) {
                    posities.add(SafariPoortBlock.deel(basis, as, dx, dy));
                    states.add(SafariRitten.POORT.get().defaultBlockState().setValue(SafariPoortBlock.AS, as).setValue(SafariPoortBlock.DEEL_X, dx)
                            .setValue(SafariPoortBlock.DEEL_Y, dy));
                }
            }
            delen(fouten, "safaripoort " + as, posities, states, (s, p) -> SafariPoortBlock.hoek(p, s),
                    s -> s.getValue(SafariPoortBlock.DEEL_X) * 10 + s.getValue(SafariPoortBlock.DEEL_Y));
        }
        // the huisje and toy parts: an offset to the controller (here at the origin)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos p = new BlockPos(dx, 1, dz);
                BlockState huis = HuisjeFeature.DEEL.get().defaultBlockState().setValue(HuisjeDeelBlock.DX, dx + 3).setValue(HuisjeDeelBlock.DY, 1)
                        .setValue(HuisjeDeelBlock.DZ, dz + 3);
                controller(fouten, "huisje deel", p, huis, HuisjeDeelBlock::controller);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    BlockState speel = SpeelgoedFeature.DEEL.get().defaultBlockState().setValue(SpeelDeelBlock.DX, dx + 1).setValue(SpeelDeelBlock.DY, 1)
                            .setValue(SpeelDeelBlock.DZ, dz + 1);
                    controller(fouten, "speelgoed deel", p, speel, SpeelDeelBlock::controller);
                }
            }
        }
        helper.assertTrue(fouten.isEmpty(), String.join("; ", fouten));
        helper.succeed();
    }

    private static BlockPos transform(BlockPos p, Rotation r, Mirror m) {
        BlockPos q = switch (m) {
            case FRONT_BACK -> new BlockPos(-p.getX(), p.getY(), p.getZ());
            case LEFT_RIGHT -> new BlockPos(p.getX(), p.getY(), -p.getZ());
            default -> p;
        };
        return q.rotate(r);
    }

    private static BlockState transform(BlockState s, Rotation r, Mirror m) {
        return s.mirror(m).rotate(r);
    }

    private static List<Rotation> rotaties() {
        List<Rotation> out = new ArrayList<>(List.of(DRAAIEN));
        out.add(Rotation.NONE);
        return out;
    }

    /** All parts of one multi-block, turned/mirrored: they point at one common corner and keep distinct numbers. */
    private static void delen(List<String> fouten, String naam, List<BlockPos> posities, List<BlockState> states,
                              java.util.function.BiFunction<BlockState, BlockPos, BlockPos> hoek, java.util.function.ToIntFunction<BlockState> nummer) {
        for (Mirror m : new Mirror[]{Mirror.NONE, Mirror.FRONT_BACK, Mirror.LEFT_RIGHT}) {
            for (Rotation r : rotaties()) {
                Set<BlockPos> hoeken = new HashSet<>();
                Set<Integer> nummers = new HashSet<>();
                for (int i = 0; i < posities.size(); i++) {
                    BlockState s = transform(states.get(i), r, m);
                    hoeken.add(hoek.apply(s, transform(posities.get(i), r, m)));
                    nummers.add(nummer.applyAsInt(s));
                }
                if (hoeken.size() != 1 || nummers.size() != posities.size()) {
                    fouten.add(naam + " " + r + "/" + m + ": corners " + hoeken + ", " + nummers.size() + " distinct parts");
                }
            }
        }
    }

    private static void controller(List<String> fouten, String naam, BlockPos p, BlockState s,
                                   java.util.function.BiFunction<BlockState, BlockPos, BlockPos> controller) {
        for (Mirror m : new Mirror[]{Mirror.NONE, Mirror.FRONT_BACK, Mirror.LEFT_RIGHT}) {
            for (Rotation r : rotaties()) {
                BlockPos c = controller.apply(transform(s, r, m), transform(p, r, m));
                if (!c.equals(BlockPos.ZERO)) {
                    fouten.add(naam + " at " + p + " " + r + "/" + m + " points at " + c);
                    return;
                }
            }
        }
    }

    /** "dir", "axis", "side", "rot16" or null. */
    static String kind(Property<?> p) {
        if (p.getValueClass() == Direction.class) {
            return "dir";
        }
        if (p.getValueClass() == Direction.Axis.class) {
            return "axis";
        }
        if (SIDES.containsValue(p.getName()) && p.getPossibleValues().size() > 1) {
            return "side";
        }
        if (p.getName().equals("rotation") && p.getValueClass() == Integer.class && p.getPossibleValues().size() == 16) {
            return "rot16";
        }
        return null;
    }

    /** Null when the state turns (and, if spiegelen, mirrors) right, otherwise what goes wrong. */
    static String check(BlockState state, boolean spiegelen) {
        BlockState cw, fb, lr;
        try {
            cw = state.rotate(Rotation.CLOCKWISE_90);
            fb = state.mirror(Mirror.FRONT_BACK);
            lr = state.mirror(Mirror.LEFT_RIGHT);
        } catch (RuntimeException e) {
            return "throws " + e + " for " + state;
        }
        for (Property<?> p : state.getProperties()) {
            String kind = kind(p);
            if (kind == null) {
                continue;
            }
            switch (kind) {
                case "dir" -> {
                    Direction d = (Direction) state.getValue(p);
                    if (cw.getValue(p) != Rotation.CLOCKWISE_90.rotate(d)) {
                        return "rotate " + p.getName() + "=" + d + " -> " + cw.getValue(p);
                    }
                    if (spiegelen && (fb.getValue(p) != Mirror.FRONT_BACK.mirror(d) || lr.getValue(p) != Mirror.LEFT_RIGHT.mirror(d))) {
                        return "mirror " + p.getName() + "=" + d + " -> " + fb.getValue(p) + "/" + lr.getValue(p);
                    }
                }
                case "axis" -> {
                    Direction.Axis a = (Direction.Axis) state.getValue(p);
                    Direction.Axis want = a == Direction.Axis.X ? Direction.Axis.Z : a == Direction.Axis.Z ? Direction.Axis.X : a;
                    if (cw.getValue(p) != want) {
                        return "rotate " + p.getName() + "=" + a + " -> " + cw.getValue(p);
                    }
                    if (spiegelen && (fb.getValue(p) != a || lr.getValue(p) != a)) {
                        return "mirror " + p.getName() + "=" + a + " -> " + fb.getValue(p) + "/" + lr.getValue(p);
                    }
                }
                case "side" -> {
                    Direction d = Direction.byName(p.getName());
                    Object v = state.getValue(p);
                    Property<?> naCw = state.getBlock().getStateDefinition().getProperty(SIDES.get(d.getClockWise()));
                    Property<?> naFb = state.getBlock().getStateDefinition().getProperty(SIDES.get(Mirror.FRONT_BACK.mirror(d)));
                    Property<?> naLr = state.getBlock().getStateDefinition().getProperty(SIDES.get(Mirror.LEFT_RIGHT.mirror(d)));
                    if (naCw == null || !v.equals(cw.getValue(naCw))) {
                        return "rotate side " + p.getName() + "=" + v + " not on " + (naCw == null ? "?" : naCw.getName()) + " in " + cw;
                    }
                    if (spiegelen && (naFb == null || !v.equals(fb.getValue(naFb)) || naLr == null || !v.equals(lr.getValue(naLr)))) {
                        return "mirror side " + p.getName() + "=" + v + " in " + fb + " / " + lr;
                    }
                }
                case "rot16" -> {
                    int r = (Integer) state.getValue(p);
                    if ((Integer) cw.getValue(p) != (r + 4) % 16) {
                        return "rotate rotation=" + r + " -> " + cw.getValue(p);
                    }
                    if (spiegelen && ((Integer) fb.getValue(p) != Mirror.FRONT_BACK.mirror(r, 16)
                            || (Integer) lr.getValue(p) != Mirror.LEFT_RIGHT.mirror(r, 16))) {
                        return "mirror rotation=" + r + " -> " + fb.getValue(p) + "/" + lr.getValue(p);
                    }
                }
                default -> {
                }
            }
        }
        return null;
    }
}
