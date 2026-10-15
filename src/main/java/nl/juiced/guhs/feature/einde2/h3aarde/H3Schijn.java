package nl.juiced.guhs.feature.einde2.h3aarde;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import org.slf4j.Logger;

/**
 * What only ONE player sees of Ba Sing Guh (main's {@link Schijn}: block updates to that player alone; the world, and so
 * every other player, keeps the real blocks):
 * <ul>
 *   <li>the outer gate shut with bars until your passport is stamped (step {@link E2H3Aarde#LOKET} and before);</li>
 *   <li>the cabbage man's three stalls: whole, or wrecked once they broke for you ({@link H3Kool}); after the chapter he has
 *       built all three again;</li>
 *   <li>Appa's bars in the base under Meer Laogai: bent open once you freed him (the flag {@link #V_TRALIES}).</li>
 * </ul>
 * The two stall overlays come as a list of blocks from the chain (data/guhs/einde2/h3_aarde/koolkraam.json, written by
 * tools/features/einde2_h3_aarde.py from the same builder as the build's overlay templates).
 */
public final class H3Schijn {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The flag of the line: Appa's bars are open for this player. */
    public static final String V_TRALIES = "tralies";
    /** The outer gate: template x, the y range and z range of its opening (CONTRACT phase A report bouw_aarde 4). */
    static final int POORT_X = 22, POORT_Y0 = 46, POORT_Y1 = 53, POORT_Z0 = 125, POORT_Z1 = 131;
    /** Appa's bars: 7 wide (x 219-225) and 5 high (y 28-32) at z 137 (the marker appa_poort is their middle). */
    static final int TRALIES_HALF = 3, TRALIES_HOOG = 5;

    private record Blok(int dx, int dy, int dz, BlockState state) {
    }

    private static List<Blok> heel, kapot;

    private H3Schijn() {
    }

    static void registreer() {
        Schijn.registreer(H3Schijn::vul);
    }

    /** Sends this player's blocks again right away (after a change of their story). */
    static void ververs(ServerPlayer p) {
        Schijn.straks(p);
    }

    private static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad == null || !stad.doos(Schijn.BEREIK).contains(p.position()) || !H3Verloop.open.test(p)) {
            return;
        }
        vul(p, stad, gewenst);
    }

    /** What this player sees in this copy of the city (also the game tests). */
    static void vul(ServerPlayer p, E2H3Aarde.Stad stad, Map<BlockPos, BlockState> gewenst) {
        ServerLevel level = stad.level();
        int stap = E2H3Aarde.stap(p);
        if (stap <= E2H3Aarde.LOKET) {
            for (int y = POORT_Y0; y <= POORT_Y1; y++) {
                for (int z = POORT_Z0; z <= POORT_Z1; z++) {
                    BlockPos b = stad.o().offset(POORT_X, y, z);
                    if (Schijn.dichtbij(p, b) && level.getBlockState(b).isAir()) {
                        gewenst.put(b, tralie(true, false));
                    }
                }
            }
        }
        for (int nr = 1; nr <= 3; nr++) {
            BlockPos hoek = stad.pos("koolkraam_" + nr);
            if (hoek == null || !Schijn.dichtbij(p, hoek)) {
                continue;
            }
            boolean stuk = stap < E2H3Aarde.STAPPEN && E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_KOOL + nr);
            for (Blok k : kraam(stuk)) {
                BlockPos b = hoek.offset(k.dx(), k.dy(), k.dz());
                if (level.getBlockState(b).isAir()) {
                    gewenst.put(b, k.state());
                }
            }
        }
        BlockPos poort = stad.pos("appa_poort");
        if (poort != null && Schijn.dichtbij(p, poort) && (E2H3Aarde.LIJN.vlag(p, V_TRALIES) || stap > E2H3Aarde.LAOGAI)) {
            for (int dx = -TRALIES_HALF; dx <= TRALIES_HALF; dx++) {
                for (int dy = 0; dy < TRALIES_HOOG; dy++) {
                    BlockPos b = poort.offset(dx, dy, 0);
                    if (level.getBlockState(b).getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock) {
                        // bent open: the middle three columns are gone, the rest stays (a broken frame)
                        gewenst.put(b, Math.abs(dx) <= 2 || dy >= 3 ? Blocks.AIR.defaultBlockState() : tralie(false, false));
                    }
                }
            }
        }
    }

    /** An iron bar of a wall that runs north-south (along z) or east-west. */
    static BlockState tralie(boolean langsZ, boolean los) {
        return Blocks.IRON_BARS.defaultBlockState().setValue(CrossCollisionBlock.NORTH, langsZ && !los).setValue(CrossCollisionBlock.SOUTH, langsZ && !los)
                .setValue(CrossCollisionBlock.EAST, !langsZ && !los).setValue(CrossCollisionBlock.WEST, !langsZ && !los);
    }

    /** The blocks of the stall, whole or wrecked (relative to its corner). */
    static synchronized List<Blok> kraam(boolean stuk) {
        if (heel == null) {
            heel = new ArrayList<>();
            kapot = new ArrayList<>();
            String pad = "/data/" + Guhs.MODID + "/einde2/h3_aarde/koolkraam.json";
            try (InputStream in = H3Schijn.class.getResourceAsStream(pad)) {
                if (in != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    lees(root.getAsJsonArray("heel"), heel);
                    lees(root.getAsJsonArray("kapot"), kapot);
                } else {
                    LOGGER.warn("einde2 h3_aarde: no {} (the stalls stay as they are)", pad);
                }
            } catch (Exception e) {
                LOGGER.warn("einde2 h3_aarde: could not read {}: {}", pad, e.toString());
            }
        }
        return stuk ? kapot : heel;
    }

    private static void lees(JsonArray lijst, List<Blok> uit) {
        if (lijst == null) {
            return;
        }
        for (JsonElement el : lijst) {
            JsonObject o = el.getAsJsonObject();
            BlockState s = BlockState.CODEC.parse(JsonOps.INSTANCE, o.get("state")).result().orElse(null);
            if (s != null) {
                uit.add(new Blok(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(), s));
            }
        }
    }
}
