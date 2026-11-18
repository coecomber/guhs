package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Puts the games' blocks at plein's anchors (and takes them away again): the arcade cabinet ({@code arcadekast}), the two
 * highscore boards ({@code highscorebord_race}, {@code highscorebord_schaats}), the shop's stall ({@code winkel}), the
 * oliebollen stall ({@code oliebollenkraam}), the skating lap's start sign ({@code schaats_start}, else next to
 * {@code vijver_route_0}) and the hidden bags ({@code pepernoot_verstop_*}). An anchor's yaw is the way the block's front
 * looks. A spot that holds something else than air (or our own block) is left alone and named in the answer.
 * <p>
 * plein's {@code GuhwinterPlein.plaatsWinter} can call {@link #plaats} after building the plaza; the end-of-event switch
 * calls {@link #weg} before the frame goes in (it takes the floating boards away too).
 */
public final class SpellenPlaatsing {
    private SpellenPlaatsing() {
    }

    private static Direction kant(float yaw) {
        return Direction.fromYRot(yaw);
    }

    /** The start sign: its own anchor, or two blocks outside the first poortje (seen from the pond's middle). */
    @Nullable
    static SpelBrug.Anker schaatsStart(MinecraftServer server) {
        SpelBrug.Anker eigen = SpelBrug.anker(server, "schaats_start");
        if (eigen != null) {
            return eigen;
        }
        List<SpelBrug.Anker> route = SpelBrug.ankers(server, "vijver_route");
        if (route.isEmpty()) {
            return null;
        }
        SpelBrug.Anker m = SpelBrug.anker(server, "vijver_midden");
        Vec3 midden = m != null ? m.pos() : Vec3.atBottomCenterOf(SpelBrug.VIJVER);
        Vec3 eerste = route.get(0).pos();
        Vec3 uit = eerste.subtract(midden).multiply(1, 0, 1);
        uit = uit.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : uit.normalize();
        Vec3 plek = eerste.add(uit.scale(2.5));
        float yaw = (float) Math.toDegrees(Math.atan2(-(-uit.x), -uit.z));    // (the sign looks at the pond)
        return new SpelBrug.Anker("schaats_start", plek, yaw);
    }

    private static boolean vrij(ServerLevel level, BlockPos pos, Block eigen) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.is(eigen) || s.canBeReplaced();
    }

    private static int zet(ServerLevel level, List<String> fouten, @Nullable SpelBrug.Anker a, BlockState state) {
        if (a == null) {
            return 0;
        }
        BlockPos pos = a.blok();
        if (!vrij(level, pos, state.getBlock())) {
            fouten.add(a.naam() + " " + pos.toShortString());
            return 0;
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, kant(a.yaw()));
        }
        level.setBlock(pos, state, 3);
        return 1;
    }

    /** Puts every block at its anchor; returns a short report. */
    public static String plaats(ServerLevel level) {
        MinecraftServer server = level.getServer();
        SpelBrug.vergeet();
        List<String> fouten = new ArrayList<>();
        int n = 0;
        SpelBrug.Anker kast = SpelBrug.anker(server, "arcadekast");
        if (kast != null) {
            BlockPos pos = kast.blok();
            if (vrij(level, pos, SpellenFeature.ARCADEKAST.get()) && vrij(level, pos.above(), SpellenFeature.ARCADEKAST.get())) {
                SpelBlokken.Arcadekast.bouw(level, pos, SpellenFeature.ARCADEKAST.get(), kant(kast.yaw()));
                n++;
            } else {
                fouten.add("arcadekast " + pos.toShortString());
            }
        }
        n += zet(level, fouten, SpelBrug.anker(server, "highscorebord_race"),
                SpellenFeature.HIGHSCOREBORD.get().defaultBlockState().setValue(HighscorebordBlock.SPEL, HighscorebordBlock.Spel.RACE));
        n += zet(level, fouten, SpelBrug.anker(server, "highscorebord_schaats"),
                SpellenFeature.HIGHSCOREBORD.get().defaultBlockState().setValue(HighscorebordBlock.SPEL, HighscorebordBlock.Spel.SCHAATS));
        n += zet(level, fouten, SpelBrug.anker(server, "winkel"), SpellenFeature.WINKELKRAAM.get().defaultBlockState());
        n += zet(level, fouten, SpelBrug.anker(server, "oliebollenkraam"), SpellenFeature.BAKKRAAM.get().defaultBlockState());
        n += zet(level, fouten, schaatsStart(server), SpellenFeature.SCHAATSBORD.get().defaultBlockState());
        List<SpelBrug.Anker> zakjes = SpelBrug.ankers(server, "pepernoot_verstop");
        for (SpelBrug.Anker a : zakjes) {
            n += zet(level, fouten, a, SpellenFeature.ZAKJE.get().defaultBlockState());
        }
        HighscorebordBlock.verversAlles(server);
        return (SpelBrug.heeftAnkers(server) ? "" : "(terugvalplekken) ") + n + " blokken gezet"
                + (fouten.isEmpty() ? "" : "; bezet, niet gezet: " + String.join(", ", fouten));
    }

    /** Takes every block of the games away from the anchors (and the boards' floating text). */
    public static String weg(ServerLevel level) {
        MinecraftServer server = level.getServer();
        int n = 0;
        List<SpelBrug.Anker> alle = new ArrayList<>();
        for (String naam : List.of("arcadekast", "highscorebord_race", "highscorebord_schaats", "winkel", "oliebollenkraam")) {
            SpelBrug.Anker a = SpelBrug.anker(server, naam);
            if (a != null) {
                alle.add(a);
            }
        }
        SpelBrug.Anker start = schaatsStart(server);
        if (start != null) {
            alle.add(start);
        }
        alle.addAll(SpelBrug.ankers(server, "pepernoot_verstop"));
        for (SpelBrug.Anker a : alle) {
            BlockPos pos = a.blok();
            for (BlockPos p : List.of(pos, pos.above())) {
                BlockState s = level.getBlockState(p);
                if (s.is(SpellenFeature.ARCADEKAST.get()) || s.is(SpellenFeature.HIGHSCOREBORD.get()) || s.is(SpellenFeature.WINKELKRAAM.get())
                        || s.is(SpellenFeature.BAKKRAAM.get()) || s.is(SpellenFeature.SCHAATSBORD.get()) || s.is(SpellenFeature.ZAKJE.get())) {
                    if (s.is(SpellenFeature.HIGHSCOREBORD.get())) {
                        HighscorebordBlock.weg(level, p);
                    }
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    n++;
                }
            }
        }
        return n + " blokken weggehaald";
    }
}
