package nl.juiced.guhs.feature.guhrio;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;

/**
 * A Super Guhrio level as data: its lanes, in the level's own frame. The frame hangs on the level's start block
 * ({@link GuhrioBlocks.StartBlok}): the start block is (0, 0, 0), +x is the way the start block faces ("into the level"),
 * +z is on the right hand of that, +y is up. A level that is built facing east has exactly the template's coordinates
 * minus the start block's; turned templates (a jigsaw piece) keep working because the start block turns with them.
 * <p>
 * The data is a file {@code data/guhs/guhrio_level/<id>.json} (made by the level's generator, see tools/features/guhrio.py:
 * {@code level_json}):
 * <pre>
 * { "banen": [ { "id": "hoofd", "punten": [[0,0,0],[60,0,0],[60,0,-20]], "camera": "rechts",
 *                "afstand": 13, "hoogte": 6, "onder": -6, "boven": 14 } ],
 *   "wereld": "1-1", "uitgang": [84, 1, 0], "ingang": [-3, 0, 0], "na": "kasteel_1_1" }
 * </pre>
 * {@code uitgang}: where you are put after the flagpole; {@code ingang}: where you are put when you leave the level any
 * other way (stopping, logging out) - both optional, both in the level's frame, and both may lie far outside the lanes (the
 * hall of the castle); {@code na}: the level that this player must have finished first (the start block refuses otherwise).
 * {@code camera}: on which hand of the lane's direction the camera stands (rechts: further along the lane is right on
 * screen); {@code afstand}: how far away; {@code hoogte}: how many blocks it shows above and below its middle;
 * {@code onder}/{@code boven}: the floor you fall out under and the ceiling, relative to the start block. The first lane
 * is the one the start block is on. Everything else (coins, ?-blocks, flags, pipes, Guhmba's) is simply built in the lane
 * with the Guhrio blocks: the engine finds them ({@link GuhrioSpel}).
 * <p>
 * {@link #plaats} turns the definition into the lanes in the world for one start block ({@link Geplaatst}).
 */
public record GuhrioLevel(String id, String wereld, List<BaanDef> banen, @Nullable BlockPos uitgang, @Nullable BlockPos ingang,
                          @Nullable String na) {
    /** A level without an entrance spot of its own and without a level that must be done first. */
    public GuhrioLevel(String id, String wereld, List<BaanDef> banen, @Nullable BlockPos uitgang) {
        this(id, wereld, banen, uitgang, null, null);
    }

    /** One lane in the level's own frame. */
    public record BaanDef(String id, List<BlockPos> punten, boolean cameraRechts, double afstand, double hoogte, int onder, int boven) {
    }

    /** Levels made in code (tests, and a level that has no file). */
    private static final Map<String, GuhrioLevel> VAST = new ConcurrentHashMap<>();
    /** Levels read from the data pack (forgotten on a reload). */
    private static final Map<String, Optional<GuhrioLevel>> GELEZEN = new ConcurrentHashMap<>();

    /** Registers a level from code (it wins from a file with the same id). */
    public static void zet(GuhrioLevel level) {
        VAST.put(level.id, level);
    }

    public static void vergeet() {
        GELEZEN.clear();
    }

    /** Takes a level made in code away again (tests). */
    public static void vergeet(String id) {
        VAST.remove(id);
    }

    /** The level with this id: from code, else from {@code data/guhs/guhrio_level/<id>.json}. */
    @Nullable
    public static GuhrioLevel vind(MinecraftServer server, String id) {
        GuhrioLevel vast = VAST.get(id);
        return vast != null ? vast : bestand(server, id);
    }

    /** The level of the file {@code data/guhs/guhrio_level/<id>.json}, whatever was registered from code; null: no such file. */
    @Nullable
    public static GuhrioLevel bestand(MinecraftServer server, String id) {
        if (id == null || id.isEmpty() || !id.matches("[a-z0-9_/.-]+")) {
            return null;
        }
        return GELEZEN.computeIfAbsent(id, k -> {
            var res = server.getResourceManager().getResource(Guhs.id("guhrio_level/" + k + ".json"));
            if (res.isEmpty()) {
                return Optional.empty();
            }
            try (Reader reader = res.get().openAsReader()) {
                return Optional.of(lees(k, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("guhs").error("Guhrio: level {} can't be read", k, e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    public static GuhrioLevel lees(String id, JsonObject json) {
        List<BaanDef> banen = new ArrayList<>();
        int i = 0;
        for (JsonElement e : json.getAsJsonArray("banen")) {
            JsonObject b = e.getAsJsonObject();
            List<BlockPos> punten = new ArrayList<>();
            for (JsonElement p : b.getAsJsonArray("punten")) {
                punten.add(pos(p.getAsJsonArray()));
            }
            banen.add(new BaanDef(b.has("id") ? b.get("id").getAsString() : "baan" + i, punten,
                    !b.has("camera") || !"links".equals(b.get("camera").getAsString()),
                    b.has("afstand") ? b.get("afstand").getAsDouble() : 13, b.has("hoogte") ? b.get("hoogte").getAsDouble() : 6,
                    b.has("onder") ? b.get("onder").getAsInt() : -8, b.has("boven") ? b.get("boven").getAsInt() : 16));
            i++;
        }
        if (banen.isEmpty()) {
            throw new IllegalArgumentException("guhrio level " + id + ": no lanes");
        }
        return new GuhrioLevel(id, json.has("wereld") ? json.get("wereld").getAsString() : id, banen,
                json.has("uitgang") ? pos(json.getAsJsonArray("uitgang")) : null, json.has("ingang") ? pos(json.getAsJsonArray("ingang")) : null,
                json.has("na") ? json.get("na").getAsString() : null);
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    /** A spot of the level's own frame in the world, for a start block at {@code anker} facing {@code kant}. */
    public static BlockPos wereld(BlockPos anker, Direction kant, BlockPos eigen) {
        int fx = kant.getStepX(), fz = kant.getStepZ();
        return new BlockPos(anker.getX() + eigen.getX() * fx - eigen.getZ() * fz, anker.getY() + eigen.getY(),
                anker.getZ() + eigen.getX() * fz + eigen.getZ() * fx);
    }

    /** This level in the world, hanging on the start block at {@code anker} that faces {@code kant}. */
    public Geplaatst plaats(ResourceKey<Level> dimensie, BlockPos anker, Direction kant) {
        List<Baan> uit = new ArrayList<>();
        for (BaanDef def : banen) {
            List<BlockPos> punten = def.punten.stream().map(p -> wereld(anker, kant, p)).toList();
            uit.add(new Baan(def.id, punten, def.cameraRechts, def.afstand, def.hoogte, anker.getY() + def.onder, anker.getY() + def.boven));
        }
        return new Geplaatst(this, dimensie, anker, kant, List.copyOf(uit), uitgang == null ? null : wereld(anker, kant, uitgang));
    }

    /** A level in the world: the lanes with their real coordinates, for the start block at {@link #anker}. */
    public record Geplaatst(GuhrioLevel level, ResourceKey<Level> dimensie, BlockPos anker, Direction kant, List<Baan> banen,
                            @Nullable BlockPos uitgang) {
        /** The lane this block belongs to, or -1. */
        public int baanVan(BlockPos pos) {
            for (int i = 0; i < banen.size(); i++) {
                if (banen.get(i).bevat(pos)) {
                    return i;
                }
            }
            return -1;
        }

        /** Where you stand after leaving the level without finishing it (the level's {@code ingang}), or null. */
        @Nullable
        public BlockPos ingang() {
            return level.ingang == null ? null : GuhrioLevel.wereld(anker, kant, level.ingang);
        }

        /** A spot of the level's own frame in the world. */
        public BlockPos wereld(BlockPos eigen) {
            return GuhrioLevel.wereld(anker, kant, eigen);
        }

        /** Where a player starts (and comes back to before the first flag): on the first lane, at the start block. */
        public Vec3 start() {
            Baan baan = banen.get(0);
            Baan.Plek plek = baan.plek(anker.getX() + 0.5, anker.getZ() + 0.5);
            return baan.punt(plek.s(), anker.getY());
        }

        /** For the player's game: the lanes. */
        public CompoundTag naarTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Level", level.id);
            tag.putString("Wereld", level.wereld);
            tag.putLong("Anker", anker.asLong());
            ListTag lijst = new ListTag();
            for (Baan b : banen) {
                CompoundTag t = new CompoundTag();
                t.putString("Id", b.id);
                t.putLongArray("Punten", b.punten().stream().mapToLong(BlockPos::asLong).toArray());
                t.putBoolean("CameraRechts", b.cameraRechts);
                t.putDouble("Afstand", b.afstand);
                t.putDouble("Hoogte", b.hoogte);
                t.putInt("Onder", b.onder);
                t.putInt("Boven", b.boven);
                lijst.add(t);
            }
            tag.put("Banen", lijst);
            return tag;
        }
    }

    /** The lanes out of {@link Geplaatst#naarTag} (in the player's game). */
    public static List<Baan> banenUit(CompoundTag tag) {
        List<Baan> uit = new ArrayList<>();
        for (var e : tag.getListOrEmpty("Banen")) {
            if (e instanceof CompoundTag t) {
                List<BlockPos> punten = new ArrayList<>();
                for (long l : t.getLongArray("Punten").orElse(new long[0])) {
                    punten.add(BlockPos.of(l));
                }
                if (punten.size() >= 2) {
                    uit.add(new Baan(t.getStringOr("Id", ""), punten, t.getBooleanOr("CameraRechts", true), t.getDoubleOr("Afstand", 13),
                            t.getDoubleOr("Hoogte", 6), t.getIntOr("Onder", 0), t.getIntOr("Boven", 0)));
                }
            }
        }
        return uit;
    }
}
