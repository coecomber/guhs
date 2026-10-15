package nl.juiced.guhs.feature.guhriow3;

import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (guhrio-w3): where things are in the duel arena, in the level's own frame (the start block is 0, 0, 0; +x further
 * along the lane; y 0 is where you stand). Written by tools/features/guhrio_w3.py from the very numbers it builds the arena
 * with ({@code data/guhs/guhriow3/duel.json}), so the Grote Nether-Mika and the arena can't drift apart.
 *
 * @param brugVan    the first column of the bridge
 * @param brugTot    the last column of the bridge
 * @param breuk      the first column that falls when the lever is pulled (everything from here to {@code brugTot})
 * @param muurLinks  the column of the wall at the start side
 * @param muurRechts the column of the wall at the far side
 * @param hendel     the lever
 * @param vraag      the ?-block with the Vuurpeper (empty until round 3)
 * @param blok       the block the bridge is made of
 */
public record DuelPlan(int brugVan, int brugTot, int breuk, int muurLinks, int muurRechts, BlockPos hendel, BlockPos vraag, String blok) {
    /** The arena as tools/features/guhrio_w3.py builds it (used when the file can't be read). */
    public static final DuelPlan STANDAARD = new DuelPlan(7, 26, 17, -2, 37, new BlockPos(33, 0, 0), new BlockPos(4, 3, 0), "guhs:guhriow3_brug");

    private static final Map<MinecraftServer, Optional<DuelPlan>> GELEZEN = new ConcurrentHashMap<>();

    /** The plan of this server's data pack. */
    public static DuelPlan van(@Nullable MinecraftServer server) {
        if (server == null) {
            return STANDAARD;
        }
        return GELEZEN.computeIfAbsent(server, s -> {
            var res = s.getResourceManager().getResource(Guhs.id("guhriow3/duel.json"));
            if (res.isEmpty()) {
                return Optional.empty();
            }
            try (Reader reader = res.get().openAsReader()) {
                JsonObject j = JsonParser.parseReader(reader).getAsJsonObject();
                return Optional.of(new DuelPlan(j.get("brug_van").getAsInt(), j.get("brug_tot").getAsInt(), j.get("breuk").getAsInt(),
                        j.get("muur_links").getAsInt(), j.get("muur_rechts").getAsInt(), pos(j.getAsJsonArray("hendel")),
                        pos(j.getAsJsonArray("vraag")), j.get("blok").getAsString()));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("guhs").error("Guhrio: the duel plan can't be read", e);
                return Optional.empty();
            }
        }).orElse(STANDAARD);
    }

    /** (the server stopped, or its data was loaded again) */
    public static void vergeet() {
        GELEZEN.clear();
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    /** How far the shell rolls to the start side: against the wall. */
    public double rolMin() {
        return muurLinks + 0.5 + GroteNetherMikaEntity.SCHILD_BREED / 2 + 0.15;
    }

    /** Where the shell brakes on the broken bridge: just before the edge. */
    public double rolMax() {
        return breuk - 0.5 - GroteNetherMikaEntity.SCHILD_BREED / 2 - 0.2;
    }

    /** Round 1: how far he walks to either side on the whole bridge. */
    public double loopMin() {
        return brugVan + 3.5;
    }

    public double loopMax() {
        return brugTot - 2.5;
    }

    /** Where he sits and sulks: on the far ledge, right behind the bridge. */
    public double mokPlek() {
        return brugTot + 0.5 + 1.8;
    }
}
