package nl.juiced.guhs.feature.wereld;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.storage.GuhSavedData;
import org.slf4j.Logger;

/**
 * 1.4.2 (fixes): mends the copies of a structure that were built BEFORE its template was fixed (a pit you could fall into
 * and never leave). A repair is a list of template spots with the block that was there and the block it becomes, written by
 * the structure's python builder ({@code assets/guhs/herstel/<id>.json}: {@code structuur}, {@code stuk} (optional), and
 * {@code blokken}: {@code [{pos: [x, y, z], oud: [block ids], blok: id, props: {..}}]}).
 * <p>
 * About every two seconds per player the copies around them are looked at (through the loaded chunks, {@link Kopieen}); a
 * copy is mended ONCE, when every spot of it is loaded: a spot changes only if it still holds one of the {@code oud}
 * blocks (whatever a player in creative mode changed there stays). Which copies were mended is saved per dimension
 * ({@code guhs:reparaties}), so it never happens twice. New copies come from the fixed template: their spots already
 * hold the new block, so nothing changes there either.
 */
public final class Reparaties {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How far around a player (blocks, per axis) copies are looked for. */
    public static final int BEREIK = 96;
    /** Every this many ticks per player. */
    public static final int ELKE = 40;

    /** One spot: template coordinates, the blocks that may be replaced, and the new block (unturned). */
    public record Blok(BlockPos lokaal, Set<Block> oud, BlockState nieuw) {
    }

    /** One repair of one structure. */
    public record Reparatie(String id, String structuur, @Nullable String stuk, List<Blok> blokken) {
    }

    /** The ids registered; read from the jar the first time they are needed (the blocks exist by then). */
    private static final List<String> IDS = new CopyOnWriteArrayList<>();
    private static volatile List<Reparatie> alle;
    private static boolean luistert;

    private Reparaties() {
    }

    /** Mend the copies of a structure with the spots of {@code assets/guhs/herstel/<id>.json}. From a Feature.register. */
    public static synchronized void registreer(String id) {
        IDS.add(id);
        alle = null;
        if (!luistert) {
            luistert = true;
            NeoForge.EVENT_BUS.addListener(Reparaties::opSpelerTick);
        }
    }

    private static List<Reparatie> alle() {
        List<Reparatie> a = alle;
        if (a == null) {
            List<Reparatie> lijst = new ArrayList<>();
            for (String id : IDS) {
                lijst.add(lees(id));
            }
            alle = a = List.copyOf(lijst);
        }
        return a;
    }

    /** The repair of this id as it is in the jar (also for the tests). */
    public static Reparatie lees(String id) {
        String pad = "/assets/guhs/herstel/" + id + ".json";
        try (InputStream in = Reparaties.class.getResourceAsStream(pad)) {
            if (in == null) {
                throw new IllegalStateException("no " + pad);
            }
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Blok> blokken = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("blokken")) {
                JsonObject b = e.getAsJsonObject();
                var p = b.getAsJsonArray("pos");
                Set<Block> oud = new HashSet<>();
                for (JsonElement s : b.getAsJsonArray("oud")) {
                    oud.add(blok(s.getAsString()));
                }
                BlockState nieuw = blok(b.get("blok").getAsString()).defaultBlockState();
                if (b.has("props")) {
                    for (Map.Entry<String, JsonElement> prop : b.getAsJsonObject("props").entrySet()) {
                        nieuw = zet(nieuw, prop.getKey(), prop.getValue().getAsString());
                    }
                }
                blokken.add(new Blok(new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt()), Set.copyOf(oud), nieuw));
            }
            return new Reparatie(id, o.get("structuur").getAsString(), o.has("stuk") ? o.get("stuk").getAsString() : null, List.copyOf(blokken));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Block blok(String id) {
        Identifier key = Identifier.parse(id);
        return BuiltInRegistries.BLOCK.getOptional(key).orElseThrow(() -> new IllegalStateException("herstel: no block " + id));
    }

    private static <T extends Comparable<T>> BlockState zet(BlockState state, String naam, String waarde, Property<T> prop) {
        Optional<T> v = prop.getValue(waarde);
        return v.isPresent() ? state.setValue(prop, v.get()) : state;
    }

    private static BlockState zet(BlockState state, String naam, String waarde) {
        Property<?> prop = state.getBlock().getStateDefinition().getProperty(naam);
        return prop == null ? state : zet(state, naam, waarde, prop);
    }

    // --- mending -----------------------------------------------------------------------------------------------------

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + 7) % ELKE == 0) {
            bijSpeler(p.level(), p.blockPosition());
        }
    }

    /** Mends what is due around this spot; returns how many copies were mended. */
    public static int bijSpeler(ServerLevel level, BlockPos bij) {
        int n = 0;
        for (Reparatie r : alle()) {
            Structure s = Kopieen.structuur(level, r.structuur());
            if (s == null) {
                continue;
            }
            for (StructureStart start : Kopieen.bij(level, s, bij, BEREIK)) {
                if (herstel(level, r, start) > 0) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Mends this copy if that was not done before and all its spots are loaded: returns how many blocks changed (-1: not
     * now, a spot is not loaded; 0: done before, or nothing to change).
     */
    public static int herstel(ServerLevel level, Reparatie r, StructureStart start) {
        Gedaan gedaan = Gedaan.van(level);
        String sleutel = r.id() + "@" + start.getChunkPos().pack();
        if (gedaan.heeft(sleutel)) {
            return 0;
        }
        Rotation draai = Kopieen.draai(start, r.stuk());
        List<BlockPos> plekken = new ArrayList<>();
        for (Blok b : r.blokken()) {
            BlockPos w = Kopieen.wereld(start, r.stuk(), b.lokaal());
            if (w == null) {
                gedaan.zet(sleutel);      // (this copy has no such piece: nothing to mend)
                return 0;
            }
            if (!level.isLoaded(w)) {
                return -1;
            }
            plekken.add(w);
        }
        int veranderd = 0;
        for (int i = 0; i < plekken.size(); i++) {
            Blok b = r.blokken().get(i);
            BlockPos w = plekken.get(i);
            BlockState nu = level.getBlockState(w);
            BlockState nieuw = b.nieuw().rotate(draai);
            if (b.oud().contains(nu.getBlock()) && nu != nieuw) {
                level.setBlock(w, nieuw, Block.UPDATE_ALL);
                veranderd++;
            }
        }
        gedaan.zet(sleutel);
        if (veranderd > 0) {
            LOGGER.info("Guhs: mended {} blocks of {} at {} ({})", veranderd, r.structuur(), plekken.get(0).toShortString(), r.id());
        }
        return veranderd;
    }

    /** (tests) forget that this copy was mended. */
    public static void vergeet(ServerLevel level, Reparatie r, StructureStart start) {
        Gedaan.van(level).wis(r.id() + "@" + start.getChunkPos().pack());
    }

    /** Which copies were mended (per dimension). */
    static final class Gedaan extends SavedData {
        private final Set<String> klaar = new HashSet<>();

        static final SavedDataType<Gedaan> TYPE = GuhSavedData.tagType("reparaties", Gedaan::new, Gedaan::load, Gedaan::save);

        static Gedaan van(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        boolean heeft(String sleutel) {
            return klaar.contains(sleutel);
        }

        void zet(String sleutel) {
            if (klaar.add(sleutel)) {
                setDirty();
            }
        }

        void wis(String sleutel) {
            if (klaar.remove(sleutel)) {
                setDirty();
            }
        }

        private static Gedaan load(CompoundTag tag) {
            Gedaan g = new Gedaan();
            for (Tag t : tag.getListOrEmpty("Klaar")) {
                t.asString().ifPresent(g.klaar::add);
            }
            return g;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            klaar.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s)));
            tag.put("Klaar", list);
            return tag;
        }
    }
}
