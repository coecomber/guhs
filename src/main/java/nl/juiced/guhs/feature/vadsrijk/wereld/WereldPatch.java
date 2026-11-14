package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/**
 * 1.10.3: one-time block patches for the ALREADY generated Vadsrijk world (the map tiles are only stamped into new chunks, so a
 * generator fix never reaches the live server by itself). A patch is a named list of boxes, each filled with one block, read from
 * {@code data/guhs/vadsrijk/patch/<naam>.json} (registry {@link #NAMEN}):
 * <ul>
 *   <li>only air / replaceable, fluid-free blocks without a block entity are ever changed (never a structure or player block);</li>
 *   <li>{@link #toepassen} with {@code echt = false} is a dry run that counts per box;</li>
 *   <li>with {@code echt = true} the old states of exactly the cells it changes are written first to
 *       {@code <world>/data/vr_patch_backup_<naam>_<stamp>.nbt}, then the blocks are placed;</li>
 *   <li>{@link #herstel} puts those old states back where the patch block is still standing.</li>
 * </ul>
 * Commands: {@code /guhs vadsrijk patch <naam> [--echt]}, {@code /guhs vadsrijk patch herstel <bestand>} (see {@link PatchCommando}).
 */
public final class WereldPatch {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The central return portals of the safe towns (tools/vadsrijk/stadsleven/terugportalen.py writes their json). */
    public static final List<String> PORTALEN = List.of("portaal_zwamzoet", "portaal_knuffelkamp", "portaal_kandijkom", "portaal_kandijkom_dok", "portaal_honingtak",
            "portaal_lampionhaven", "portaal_hoogvads", "portaal_luchthaven", "portaal_rozenkloof", "portaal_saffraanpoort");
    /** The registry: every patch name that has a json under data/guhs/vadsrijk/patch/. Add new patches here. */
    public static final List<String> NAMEN = java.util.stream.Stream.concat(java.util.stream.Stream.of("spiegelzaal"), PORTALEN.stream()).toList();
    /** Named groups ({@code /guhs vadsrijk patch <groep> [--echt]} runs every member). */
    public static final Map<String, List<String>> GROEPEN = Map.of("alle-portalen", PORTALEN);
    static final Pattern BESTAND = Pattern.compile("vr_patch_backup_[a-z0-9_]+_\\d+\\.nbt");
    /** Most cells one patch may hold (a typo in a box must not fill a mountain). */
    static final int MAX_CELLEN = 20_000;

    /** A box filled with {@code blok}; besides air/replaceable blocks it may also replace the blocks in {@code vervang} (a floor). */
    public record Doos(String naam, BlockPos min, BlockPos max, BlockState blok, java.util.Set<Block> vervang) {
        public Doos(String naam, BlockPos min, BlockPos max, BlockState blok) {
            this(naam, min, max, blok, java.util.Set.of());
        }

        int cellen() {
            return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }

    /** {@code gebied} / {@code poi} are optional (the return portals: the kern area it serves and the stand spot in front). */
    public record Patch(String naam, ResourceKey<Level> dim, String beschrijving, List<Doos> dozen, @Nullable String gebied, @Nullable String poiId,
                        @Nullable BlockPos poi) {
        public int cellen() {
            return dozen.stream().mapToInt(Doos::cellen).sum();
        }
    }

    /** Per box: cells, would change / changed, already the patch block, left alone (something else stands there). */
    public record Telling(Doos doos, int cellen, int plaatsen, int alGoed, int bezet) {
    }

    /** Result of {@link #toepassen}: the counts, the backup file name (echt only) or the reason it did nothing. */
    public record Uitslag(List<Telling> tellingen, @Nullable String backup, @Nullable String fout) {
        public int plaatsen() {
            return tellingen.stream().mapToInt(Telling::plaatsen).sum();
        }

        public int alGoed() {
            return tellingen.stream().mapToInt(Telling::alGoed).sum();
        }

        public int bezet() {
            return tellingen.stream().mapToInt(Telling::bezet).sum();
        }
    }

    private WereldPatch() {
    }

    // ---- loading ---------------------------------------------------------------------------------------------------------------

    /** The registered patch {@code naam}, read from its json, or empty (unknown name / unreadable file, logged). */
    public static Optional<Patch> laad(String naam) {
        if (!NAMEN.contains(naam)) {
            return Optional.empty();
        }
        String pad = "/data/guhs/vadsrijk/patch/" + naam + ".json";
        try (InputStream in = WereldPatch.class.getResourceAsStream(pad)) {
            if (in == null) {
                LOGGER.warn("[Vadsrijk] patch {}: {} ontbreekt", naam, pad);
                return Optional.empty();
            }
            return Optional.of(lees(naam, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()));
        } catch (Exception e) {
            LOGGER.warn("[Vadsrijk] patch {} niet leesbaar: {}", naam, e.toString());
            return Optional.empty();
        }
    }

    /** Parses a patch json: {dimensie, beschrijving, dozen: [{naam, min: [x,y,z], max: [x,y,z], blok}]}. */
    public static Patch lees(String naam, JsonObject o) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(o.get("dimensie").getAsString()));
        List<Doos> dozen = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("dozen")) {
            JsonObject d = e.getAsJsonObject();
            BlockState b = staat(d.get("blok").getAsString());
            java.util.Set<Block> vervang = new java.util.HashSet<>();
            if (d.has("vervang")) {
                for (JsonElement v : d.getAsJsonArray("vervang")) {
                    vervang.add(staat(v.getAsString()).getBlock());
                }
            }
            BlockPos a = pos(d.getAsJsonArray("min")), z = pos(d.getAsJsonArray("max"));
            dozen.add(new Doos(d.has("naam") ? d.get("naam").getAsString() : "doos " + (dozen.size() + 1), BlockPos.min(a, z), BlockPos.max(a, z), b,
                    java.util.Set.copyOf(vervang)));
        }
        JsonObject poi = o.has("poi") ? o.getAsJsonObject("poi") : null;
        Patch p = new Patch(naam, dim, o.has("beschrijving") ? o.get("beschrijving").getAsString() : naam, List.copyOf(dozen),
                o.has("gebied") ? o.get("gebied").getAsString() : null, poi == null ? null : poi.get("id").getAsString(),
                poi == null ? null : pos(poi.getAsJsonArray("pos")));
        if (p.cellen() > MAX_CELLEN) {
            throw new IllegalArgumentException("patch " + naam + " is te groot (" + p.cellen() + " cellen)");
        }
        return p;
    }

    /** "ns:block" or "ns:block[prop=value,...]" (the portal: axis + wakker). */
    static BlockState staat(String s) {
        try {
            return net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, s, false).blockState();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalArgumentException("onbekend blok " + s + ": " + e.getMessage());
        }
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    // ---- applying ---------------------------------------------------------------------------------------------------------------

    /** May the patch put a block here? Air, or a replaceable block without fluid and without a block entity (grass, snow layer). */
    public static boolean vrij(BlockState s) {
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty() && !s.hasBlockEntity());
    }

    /** Dry run ({@code echt = false}) or the real thing: counts per box; echt first writes the backup, then places. */
    public static Uitslag toepassen(ServerLevel level, Patch p, boolean echt) {
        for (Doos d : p.dozen) {
            if (!VasteTempel.geladen(level, d.min.getX(), d.min.getZ(), d.max.getX(), d.max.getZ(), true)) {
                return new Uitslag(List.of(), null, "het terrein bij " + d.naam + " (" + d.min.toShortString() + ") is nog niet gegenereerd");
            }
        }
        List<Telling> tellingen = new ArrayList<>();
        List<BlockPos> plekken = new ArrayList<>();
        List<BlockState> oud = new ArrayList<>(), nieuw = new ArrayList<>();
        for (Doos d : p.dozen) {
            int plaats = 0, goed = 0, bezet = 0;
            for (BlockPos b : BlockPos.betweenClosed(d.min, d.max)) {
                BlockState s = level.getBlockState(b);
                if (s == d.blok) {
                    goed++;
                } else if (vrij(s) || (d.vervang.contains(s.getBlock()) && !s.hasBlockEntity())) {
                    plaats++;
                    plekken.add(b.immutable());
                    oud.add(s);
                    nieuw.add(d.blok);
                } else {
                    bezet++;
                }
            }
            tellingen.add(new Telling(d, d.cellen(), plaats, goed, bezet));
        }
        if (!echt || plekken.isEmpty()) {
            return new Uitslag(tellingen, null, null);
        }
        String naam = "vr_patch_backup_" + p.naam + "_" + System.currentTimeMillis() + ".nbt";
        try {
            Path map = map(level.getServer());
            Files.createDirectories(map);
            NbtIo.writeCompressed(backupTag(p, level, plekken, oud, nieuw), map.resolve(naam));
        } catch (IOException e) {
            LOGGER.error("[Vadsrijk] patch {}: backup schrijven mislukt", p.naam, e);
            return new Uitslag(tellingen, null, "backup schrijven mislukt: " + e.getMessage());
        }
        for (int i = 0; i < plekken.size(); i++) {
            level.setBlock(plekken.get(i), nieuw.get(i), Block.UPDATE_CLIENTS);
        }
        LOGGER.info("[Vadsrijk] patch {} toegepast in {}: {} blokken geplaatst, backup data/{}", p.naam, level.dimension().identifier(), plekken.size(), naam);
        return new Uitslag(tellingen, naam, null);
    }

    static Path map(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data");
    }

    private static CompoundTag backupTag(Patch p, ServerLevel level, List<BlockPos> plekken, List<BlockState> oud, List<BlockState> nieuw) {
        CompoundTag t = new CompoundTag();
        t.putString("patch", p.naam);
        t.putString("dim", level.dimension().identifier().toString());
        t.putLong("tijd", System.currentTimeMillis());
        Map<BlockState, Integer> index = new LinkedHashMap<>();
        long[] pos = new long[plekken.size()];
        int[] st = new int[plekken.size() * 2];
        for (int i = 0; i < plekken.size(); i++) {
            pos[i] = plekken.get(i).asLong();
            st[2 * i] = index.computeIfAbsent(oud.get(i), s -> index.size());
            st[2 * i + 1] = index.computeIfAbsent(nieuw.get(i), s -> index.size());
        }
        ListTag palet = new ListTag();
        index.keySet().forEach(s -> palet.add(NbtUtils.writeBlockState(s)));
        t.put("palet", palet);
        t.put("plekken", new LongArrayTag(pos));
        t.put("staten", new IntArrayTag(st));
        return t;
    }

    // ---- undo -------------------------------------------------------------------------------------------------------------------

    /** Result of {@link #herstel}: restored cells, cells skipped (no longer the patch block), or the reason it did nothing. */
    public record Herstel(int teruggezet, int overgeslagen, @Nullable String fout) {
    }

    /** Puts the old blocks of backup file {@code bestand} (a name in world/data) back where the patch block still stands. */
    public static Herstel herstel(MinecraftServer server, String bestand) {
        if (!BESTAND.matcher(bestand).matches()) {
            return new Herstel(0, 0, "geen patch-backup: " + bestand + " (verwacht vr_patch_backup_<naam>_<tijd>.nbt)");
        }
        Path f = map(server).resolve(bestand);
        if (!Files.isRegularFile(f)) {
            return new Herstel(0, 0, "bestand " + f + " bestaat niet");
        }
        CompoundTag t;
        try {
            t = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            return new Herstel(0, 0, "lezen mislukt: " + e.getMessage());
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(t.getStringOr("dim", "minecraft:overworld"))));
        if (level == null) {
            return new Herstel(0, 0, "dimensie " + t.getStringOr("dim", "?") + " bestaat niet");
        }
        ListTag palet = t.getListOrEmpty("palet");
        List<BlockState> staten = new ArrayList<>();
        for (int i = 0; i < palet.size(); i++) {
            staten.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, palet.getCompoundOrEmpty(i)));
        }
        long[] pos = t.getLongArray("plekken").orElse(new long[0]);
        int[] st = t.getIntArray("staten").orElse(new int[0]);
        int terug = 0, over = 0;
        for (int i = Math.min(pos.length, st.length / 2) - 1; i >= 0; i--) {     // last placed first (a portal before its frame)
            BlockPos b = BlockPos.of(pos[i]);
            level.getChunk(b);
            BlockState oud = staten.get(st[2 * i]), nieuw = staten.get(st[2 * i + 1]);
            if (level.getBlockState(b) == nieuw) {
                level.setBlock(b, oud, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);   // (no shape cascade: a portal cell stays until its turn)
                terug++;
            } else {
                over++;
            }
        }
        LOGGER.info("[Vadsrijk] patch-backup {} hersteld: {} teruggezet, {} overgeslagen", bestand, terug, over);
        return new Herstel(terug, over, null);
    }

    /** The patch backups in world/data, newest first (for the command's suggestions). */
    static List<String> backups(MinecraftServer server) {
        try (var s = Files.list(map(server))) {
            return s.map(x -> x.getFileName().toString()).filter(n -> BESTAND.matcher(n).matches()).sorted((a, b) -> b.compareTo(a)).toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
