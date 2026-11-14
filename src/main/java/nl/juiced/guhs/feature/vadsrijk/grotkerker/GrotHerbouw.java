package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;
import nl.juiced.guhs.feature.vadsrijk.puzzels.PuzzelData;
import nl.juiced.guhs.feature.vadsrijk.puzzels.PuzzelDef;
import nl.juiced.guhs.feature.vadsrijk.puzzels.Puzzels;

/**
 * 1.10.3 "Grotere dungeons": the one-time server command that puts the NEW (twice as big) cave into a world whose chunks were
 * generated with the old one (map tiles only reach NEW chunks, {@link KaartFeature}). The user's rule: underground only, inside the
 * cave box, with a backup first so it can be undone.
 * <ul>
 *   <li>{@code /guhs vadsrijk grotten herbouw <cave>}: DRY RUN (the default): how many blocks would change, how many differ AT or
 *       ABOVE the surface (those are never written), what is skipped (the dungeon door, a stamped puzzle), how much stray water
 *       under the box would be cleared;</li>
 *   <li>{@code /guhs vadsrijk grotten herbouw <cave> --echt}: refused while a group is in that dungeon or anyone stands in the box;
 *       saves every block of the box (+ the water sweep below it) to {@code world/data/vr_grot_backup_<cave>_<stamp>.nbt}, resets the
 *       cave's run, then writes the tile's block for every cell of the box that differs and lies UNDER the surface limit of its
 *       column; entities of the tile are skipped;</li>
 *   <li>{@code /guhs vadsrijk grotten herstel <cave> <file>}: puts such a backup back (the file name as the herbouw told it).</li>
 * </ul>
 * The box and the per-column limit (2 under the lowest sky-connected cell above the cave in that column, so the ground surface
 * block and anything on it, the sea and lakes, are never touched) come from data/guhs/vadsrijk/grotkerker/&lt;cave&gt;.json (tools/vadsrijk/grotten/herbouw.py); the blocks come
 * from the same tiles worldgen uses ({@link KaartFeature#tegel}: the compact pack or the .nbt). Below the box, water that the tile
 * does not have (the Kristalgrot's melted ice ran down into the hollow rock) is cleared down to {@link #ONDER} blocks.
 */
public final class GrotHerbouw {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /** How deep under the box stray water is cleared. */
    public static final int ONDER = 40;

    private GrotHerbouw() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GrotHerbouw::commando);
    }

    /** The rebuild data of a cave: its box and the surface limit per column. */
    public record Data(String id, BoundingBox doos, int gx0, int gz0, int gw, int gd, short[] grens) {
        /** The highest y the command may write in this column (Integer.MIN_VALUE outside the grid). */
        public int grens(int x, int z) {
            int i = x - gx0, j = z - gz0;
            if (i < 0 || j < 0 || i >= gw || j >= gd) {
                return Integer.MIN_VALUE;
            }
            return grens[j * gw + i];
        }
    }

    @Nullable
    public static Data data(String id) {
        JsonObject o = Kerkers.json(id);
        if (o == null || !o.has("herbouw")) {
            return null;
        }
        JsonObject h = o.getAsJsonObject("herbouw");
        var d = h.getAsJsonArray("doos");
        JsonObject g = h.getAsJsonObject("grens");
        return new Data(id, new BoundingBox(d.get(0).getAsInt(), d.get(1).getAsInt(), d.get(2).getAsInt(), d.get(3).getAsInt(), d.get(4).getAsInt(),
                d.get(5).getAsInt()), g.get("x0").getAsInt(), g.get("z0").getAsInt(), g.get("w").getAsInt(), g.get("d").getAsInt(),
                Kerkers.shorts(g.get("data").getAsString()));
    }

    /** The outcome of a (dry) run. */
    public static final class Uitslag {
        public final String id;
        public int veranderd, bovenAnders, deur, puzzel, waterOnder, blokEntiteiten, geschreven;
        public final List<BlockPos> posities = new ArrayList<>();
        final List<BlockState> staten = new ArrayList<>();
        @Nullable
        public String backup;

        Uitslag(String id) {
            this.id = id;
        }

        public String tekst(boolean echt) {
            return (echt ? "Herbouw " : "Proefrun herbouw ") + id + ": " + veranderd + " blokken onder het oppervlak " + (echt ? "veranderd" : "anders")
                    + ", " + bovenAnders + " anders op/boven het oppervlak (nooit aangeraakt), " + waterOnder + " los water onder de grot"
                    + (echt ? " weg" : " weg te halen") + ", overgeslagen: " + deur + " deurblokken, " + puzzel + " puzzelblokken"
                    + (blokEntiteiten > 0 ? ", " + blokEntiteiten + " blokken met data (zonder hun data gezet)" : "")
                    + (backup != null ? ". Backup: world/data/" + backup + " (terug: /guhs vadsrijk grotten herstel " + id + " " + backup + ")" : "");
        }
    }

    /** A tile's blocks by world position (from the tile source worldgen uses). */
    static Map<Long, BlockState> tegelBlokken(ServerLevel sl, int tx, int tz, int[] nbtTeller) {
        Map<Long, BlockState> uit = new HashMap<>();
        Optional<StructureTemplate> t = KaartFeature.tegel(sl, tx, tz);
        if (t.isEmpty()) {
            return uit;
        }
        CompoundTag tag = t.get().save(new CompoundTag());
        HolderGetter<Block> blokken = sl.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag pal = tag.getListOrEmpty("palette");
        BlockState[] staten = new BlockState[pal.size()];
        for (int i = 0; i < pal.size(); i++) {
            staten[i] = NbtUtils.readBlockState(blokken, pal.getCompoundOrEmpty(i));
        }
        ListTag bl = tag.getListOrEmpty("blocks");
        for (int i = 0; i < bl.size(); i++) {
            CompoundTag b = bl.getCompoundOrEmpty(i);
            ListTag pos = b.getListOrEmpty("pos");
            int s = b.getIntOr("state", 0);
            if (s < 0 || s >= staten.length) {
                continue;
            }
            if (b.contains("nbt")) {
                nbtTeller[0]++;
            }
            uit.put(BlockPos.asLong(tx * KaartFeature.TEGEL + pos.getIntOr(0, 0), pos.getIntOr(1, 0), tz * KaartFeature.TEGEL + pos.getIntOr(2, 0)), staten[s]);
        }
        return uit;
    }

    /** Puzzle boxes (stamped by code, not in the tiles) that touch the box: kept as they are. */
    static List<BoundingBox> puzzels(MinecraftServer server, BoundingBox doos) {
        List<BoundingBox> uit = new ArrayList<>();
        PuzzelData data = PuzzelData.get(server);
        for (PuzzelDef d : Puzzels.defs()) {
            PuzzelData.Plek p = data.plek(d.id());
            if (p == null) {
                continue;
            }
            BoundingBox b = new BoundingBox(p.origin().getX() - 2, p.origin().getY() - 10, p.origin().getZ() - 2, p.origin().getX() + p.breed() + 2,
                    p.origin().getY() + p.hoogte() + 2, p.origin().getZ() + p.diep() + 2);
            if (b.intersects(doos)) {
                uit.add(b);
            }
        }
        return uit;
    }

    /**
     * Compares the world with the tiles over the cave's box (and the water sweep under it). {@code beperk}: only this part of the
     * box (gametests). Nothing is written.
     */
    public static Uitslag plan(ServerLevel sl, Data d, @Nullable BoundingBox beperk) {
        Uitslag u = new Uitslag(d.id());
        BoundingBox b = beperk == null ? d.doos() : beperk;
        Map<Long, Map<Long, BlockState>> tegels = new HashMap<>();
        int[] nbt = {0};
        List<BoundingBox> puzzels = puzzels(sl.getServer(), b);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                int tx = Math.floorDiv(x, KaartFeature.TEGEL), tz = Math.floorDiv(z, KaartFeature.TEGEL);
                Map<Long, BlockState> tegel = tegels.computeIfAbsent(((long) tx << 32) | (tz & 0xFFFFFFFFL), k -> tegelBlokken(sl, tx, tz, nbt));
                int grens = d.grens(x, z);
                sl.getChunk(x >> 4, z >> 4);
                for (int y = b.minY() - (beperk == null ? ONDER : 0); y <= b.maxY(); y++) {
                    m.set(x, y, z);
                    BlockState wil = tegel.getOrDefault(m.asLong(), Blocks.AIR.defaultBlockState());
                    BlockState nu = sl.getBlockState(m);
                    if (nu == wil) {
                        continue;
                    }
                    if (y < b.minY()) {   // the sweep under the box: only stray water
                        if (nu.getFluidState().is(Fluids.WATER) || nu.getFluidState().is(Fluids.FLOWING_WATER)) {
                            if (wil.getFluidState().isEmpty() && y <= grens) {
                                u.waterOnder++;
                                u.posities.add(m.immutable());
                                u.staten.add(wil);
                            }
                        }
                        continue;
                    }
                    if (y > grens) {
                        u.bovenAnders++;
                        continue;
                    }
                    if (nu.is(DungeonDeur.BLOK.get())) {
                        u.deur++;
                        continue;
                    }
                    final int fx = x, fy = y, fz = z;
                    if (puzzels.stream().anyMatch(p -> p.isInside(fx, fy, fz))) {
                        u.puzzel++;
                        continue;
                    }
                    u.veranderd++;
                    u.posities.add(m.immutable());
                    u.staten.add(wil);
                }
            }
        }
        u.blokEntiteiten = nbt[0];
        return u;
    }

    /** Why the real rebuild can't run now (null: it may). */
    @Nullable
    public static String weiger(ServerLevel sl, Kerker k, Data d) {
        if (DungeonRij.instantie(k.id()) != null) {
            return "er is een groep in deze dungeon: wacht tot hij leeg is (of /guhs vadsrijk grot reset " + k.id() + ")";
        }
        for (ServerPlayer p : sl.players()) {
            if (d.doos().inflatedBy(4).isInside(p.blockPosition())) {
                return p.getGameProfile().name() + " staat in de grot";
            }
        }
        return null;
    }

    /** The real rebuild: the backup, the run reset, then every planned block. Returns the outcome (with the backup's name). */
    public static Uitslag echt(ServerLevel sl, Kerker k, Data d, @Nullable BoundingBox beperk) throws IOException {
        BoundingBox b = beperk == null ? d.doos() : beperk;
        int onder = beperk == null ? ONDER : 0;
        BlockPos min = new BlockPos(b.minX(), b.minY() - onder, b.minZ());
        StructureTemplate kopie = new StructureTemplate();
        kopie.fillFromWorld(sl, min, new Vec3i(b.getXSpan(), b.getYSpan() + onder, b.getZSpan()), false, List.of());
        String stempel = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String naam = "vr_grot_backup_" + k.id() + "_" + stempel + ".nbt";
        Path map = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
        Files.createDirectories(map);
        CompoundTag t = new CompoundTag();
        t.putString("grot", k.id());
        t.putIntArray("oorsprong", new int[] {min.getX(), min.getY(), min.getZ()});
        t.put("blokken", kopie.save(new CompoundTag()));
        NbtIo.writeCompressed(t, map.resolve(naam));
        GrotKerker.reset(sl, k, GrotKerker.run(k.id()), null);   // (mobs, barriers, chests of a run gone before the plan)
        Uitslag u = plan(sl, d, beperk);
        u.backup = naam;
        for (int i = 0; i < u.posities.size(); i++) {
            sl.setBlock(u.posities.get(i), u.staten.get(i), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            u.geschreven++;
        }
        LOGGER.info("[Vadsrijk] grot {} herbouwd: {} blokken, {} water onder de grot weg, backup data/{}", k.id(), u.veranderd, u.waterOnder, naam);
        return u;
    }

    /** Puts a backup of {@link #echt} back. Null = done, else why not. */
    @Nullable
    public static String herstel(ServerLevel sl, String id, String bestand) {
        if (bestand.contains("/") || bestand.contains("\\") || bestand.contains("..") || !bestand.startsWith("vr_grot_backup_" + id + "_")) {
            return "geen backup van " + id + ": " + bestand;
        }
        Path f = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(bestand);
        CompoundTag t;
        try {
            t = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            return "backup " + f + " lezen mislukt: " + e.getMessage();
        }
        int[] o = t.getIntArray("oorsprong").orElse(null);
        if (o == null || o.length != 3 || !id.equals(t.getStringOr("grot", ""))) {
            return "backup " + bestand + " is geen grotbackup van " + id;
        }
        StructureTemplate kopie = new StructureTemplate();
        kopie.load(sl.registryAccess().lookupOrThrow(Registries.BLOCK), t.getCompoundOrEmpty("blokken"));
        BlockPos min = new BlockPos(o[0], o[1], o[2]);
        Vec3i s = kopie.getSize();
        for (int cx = min.getX() >> 4; cx <= (min.getX() + s.getX()) >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= (min.getZ() + s.getZ()) >> 4; cz++) {
                sl.getChunk(cx, cz);
            }
        }
        Kerker k = Kerkers.van(id);
        if (k != null) {
            GrotKerker.reset(sl, k, GrotKerker.run(id), null);
        }
        kopie.placeInWorld(sl, min, min, new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true), sl.getRandom(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
        LOGGER.info("[Vadsrijk] grot {}: backup data/{} teruggezet", id, bestand);
        return null;
    }

    // ---- the command -------------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(
                Kerkers.alle().stream().map(Kerker::id).filter(i -> data(i) != null), b);
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal("grotten").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        g.then(Commands.literal("herbouw").then(Commands.argument("grot", StringArgumentType.word()).suggests(ids)
                .executes(c -> herbouwCmd(c.getSource(), StringArgumentType.getString(c, "grot"), false))
                .then(Commands.literal("--echt").executes(c -> herbouwCmd(c.getSource(), StringArgumentType.getString(c, "grot"), true)))));
        g.then(Commands.literal("herstel").then(Commands.argument("grot", StringArgumentType.word()).suggests(ids)
                .then(Commands.argument("bestand", StringArgumentType.greedyString()).executes(c -> {
                    ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
                    if (sl == null) {
                        return 0;
                    }
                    String id = StringArgumentType.getString(c, "grot");
                    String f = herstel(sl, id, StringArgumentType.getString(c, "bestand").trim());
                    if (f != null) {
                        c.getSource().sendFailure(Component.literal(f));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Backup van " + id + " teruggezet."), true);
                    return 1;
                }))));
        event.getDispatcher().register(VadsrijkCommando.wortel(g));
    }

    private static int herbouwCmd(CommandSourceStack src, String id, boolean echt) {
        ServerLevel sl = Vadsrijk.level(src.getServer());
        Kerker k = Kerkers.van(id);
        Data d = data(id);
        if (sl == null || k == null || d == null) {
            src.sendFailure(Component.literal("Geen herbouwdata voor " + id + " (alleen de grotten vanaf zone 2)"));
            return 0;
        }
        if (echt) {
            String w = weiger(sl, k, d);
            if (w != null) {
                src.sendFailure(Component.literal("Herbouw " + id + " kan nu niet: " + w));
                return 0;
            }
            try {
                Uitslag u = echt(sl, k, d, null);
                src.sendSuccess(() -> Component.literal(u.tekst(true)), true);
                return 1;
            } catch (IOException e) {
                LOGGER.error("[Vadsrijk] grot {}: backup schrijven mislukt, niets veranderd", id, e);
                src.sendFailure(Component.literal("Backup schrijven mislukt, niets veranderd: " + e.getMessage()));
                return 0;
            }
        }
        Uitslag u = plan(sl, d, null);
        src.sendSuccess(() -> Component.literal(u.tekst(false) + ". Echt doen: /guhs vadsrijk grotten herbouw " + id + " --echt"), false);
        return 1;
    }
}
