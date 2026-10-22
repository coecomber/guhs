package nl.juiced.guhs.feature.safari;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.Inflater;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * Test round 5 (FX-A): the kaartwand in the entreegebouw: the real park on 6 x 6 maps in fixed item frames. The park
 * generator (tools/features/safari_park/kaartwand.py) draws the map pixels from the finished park into
 * {@code data/guhs/safari/kaartwand.json}; here, when a park version is stamped, the map data is made (locked, so the
 * game never redraws it) under map ids kept in {@link SafariOpslag} (a restamp reuses them), and the frames are hung.
 * Maps in item frames reach every player like any map on a wall.
 */
public final class Kaartwand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PAD = "/data/guhs/safari/kaartwand.json";
    public static final String TAG = "guhs_kaartwand";

    private Kaartwand() {
    }

    /**
     * After a stamping: as soon as every chunk of the wall has its entities loaded (a player came by), the maps are made
     * and hung; old frames of the wall are then in memory too, so they are replaced, never doubled.
     */
    static void tick(net.minecraft.server.MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        if (s.kaartwand >= s.gebouwd || server.getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel level = Safari.level(server);
        JsonObject data = level == null ? null : lees();
        if (data == null) {
            return;
        }
        for (JsonElement e : data.getAsJsonArray("kaarten")) {
            JsonArray f = e.getAsJsonObject().getAsJsonArray("frame");
            BlockPos pos = new BlockPos(f.get(0).getAsInt(), f.get(1).getAsInt(), f.get(2).getAsInt());
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(pos))) {
                return;
            }
        }
        plaats(level);
        s.kaartwand = s.gebouwd;
        s.setDirty();
    }

    /** (Dev command) hang the maps again as soon as the wall is loaded; returns the number of maps. */
    static int opnieuw(net.minecraft.server.MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        s.kaartwand = 0;
        s.setDirty();
        JsonObject data = lees();
        return data == null ? 0 : data.getAsJsonArray("kaarten").size();
    }

    /** Makes the maps and hangs them (the wall's chunks must be loaded, see {@link #tick}). Returns the number of frames. */
    public static int plaats(ServerLevel level) {
        JsonObject data = lees();
        if (data == null) {
            return 0;
        }
        SafariOpslag opslag = SafariOpslag.get(level.getServer());
        JsonArray palet = data.getAsJsonArray("palet");
        MapColor[] kleuren = new MapColor[palet.size()];
        for (int i = 0; i < kleuren.length; i++) {
            kleuren[i] = kleur(palet.get(i).getAsString());
        }
        byte schaal = data.has("schaal") ? data.get("schaal").getAsByte() : 0;
        Direction facing = Direction.byName(data.has("facing") ? data.get("facing").getAsString() : "south");
        if (facing == null) {
            facing = Direction.SOUTH;
        }
        JsonArray kaarten = data.getAsJsonArray("kaarten");
        List<BlockPos> frames = new ArrayList<>();
        int n = 0;
        for (int k = 0; k < kaarten.size(); k++) {
            JsonObject o = kaarten.get(k).getAsJsonObject();
            JsonArray m = o.getAsJsonArray("midden");
            MapId id = opslag.kaart(k, level);
            MapItemSavedData kaart = MapItemSavedData.createFresh(m.get(0).getAsDouble(), m.get(1).getAsDouble(), schaal, false, false,
                    Safari.DIM).locked();
            byte[] pix = pixels(o.get("pixels").getAsString());
            for (int i = 0; i < 128 * 128 && 2 * i + 1 < pix.length; i++) {
                int w = (pix[2 * i] & 0xFF) | (pix[2 * i + 1] & 0xFF) << 8;
                int idx = w >> 2, helder = w & 3;
                MapColor c = idx < kleuren.length ? kleuren[idx] : MapColor.NONE;
                kaart.colors[i] = c == MapColor.NONE ? 0 : c.getPackedId(MapColor.Brightness.byId(helder));
            }
            kaart.setDirty();
            level.setMapData(id, kaart);
            JsonArray f = o.getAsJsonArray("frame");
            BlockPos pos = new BlockPos(f.get(0).getAsInt(), f.get(1).getAsInt(), f.get(2).getAsInt());
            frames.add(pos);
            if (hang(level, pos, facing, id)) {
                n++;
            }
        }
        opslag.setDirty();
        LOGGER.info("Safari: kaartwand {} maps, {} frames hung", kaarten.size(), n);
        return n;
    }

    /** One fixed, unbreakable frame with the map (an old one there goes first). */
    private static boolean hang(ServerLevel level, BlockPos pos, Direction facing, MapId id) {
        for (ItemFrame oud : level.getEntitiesOfClass(ItemFrame.class, new AABB(pos))) {
            if (oud.entityTags().contains(TAG)) {
                oud.discard();
            }
        }
        ItemFrame frame = new ItemFrame(level, pos, facing);
        ItemStack kaart = new ItemStack(Items.FILLED_MAP);
        kaart.set(DataComponents.MAP_ID, id);
        frame.setItem(kaart, false);
        frame.fixed = true;
        frame.setInvulnerable(true);
        frame.addTag(TAG);
        return level.addFreshEntity(frame);
    }

    static MapColor kleur(String state) {
        String naam = state.contains("[") ? state.substring(0, state.indexOf('[')) : state;
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(naam));
        if (b == null || b == Blocks.AIR) {
            return MapColor.NONE;
        }
        MapColor c = b.defaultBlockState().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        return c == MapColor.NONE ? MapColor.GRASS : c;
    }

    static byte[] pixels(String b64) {
        byte[] in = Base64.getDecoder().decode(b64);
        Inflater inf = new Inflater();
        inf.setInput(in);
        byte[] uit = new byte[128 * 128 * 2];
        try {
            int n = 0;
            while (!inf.finished() && n < uit.length) {
                int k = inf.inflate(uit, n, uit.length - n);
                if (k == 0 && inf.needsInput()) {
                    break;
                }
                n += k;
            }
        } catch (java.util.zip.DataFormatException e) {
            LOGGER.error("Safari: kaartwand pixels unreadable", e);
        } finally {
            inf.end();
        }
        return uit;
    }

    @Nullable
    static JsonObject lees() {
        try (InputStream in = Kaartwand.class.getResourceAsStream(PAD)) {
            if (in == null) {
                return null;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement e = JsonParser.parseReader(r);
                return e.isJsonObject() ? e.getAsJsonObject() : null;
            }
        } catch (Exception e) {
            LOGGER.error("Safari: could not read {}", PAD, e);
            return null;
        }
    }
}
