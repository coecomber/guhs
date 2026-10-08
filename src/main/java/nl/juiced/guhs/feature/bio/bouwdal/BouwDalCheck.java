package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.world.ModDimensions;

/**
 * biomes3 bouw-dal: the checks in the real Guhmensie (a scratch dev server) and the dev commands.
 * <ul>
 *   <li>{@code /guhs bio zelftest bouw-dal}: finds the nearest copy of each of the eight structures near the first
 *       Klaterdal, generates it, and looks at its blocks: is it all there, does it stand on the ground (not floating, not
 *       buried), does the bridge span water with both ends on the banks, does the big torii stand in water, did the stair
 *       stay dry. It keeps the chunks loaded for:</li>
 *   <li>{@code /guhs bio bouw-dal check}: the characters and the balloon of the copies found (entities load a moment
 *       after their chunk), and what the house's repair had to put right (nothing, on a fresh copy).</li>
 *   <li>{@code /guhs bio bouw-dal reis start|status|zorg} and {@code reis klok <ticks>}: sends the two off from the house
 *       found (or the one the player stands in), reports its state, moves its clock.</li>
 * </ul>
 */
public final class BouwDalCheck {
    /** The start jigsaw of each template (local x, y, z): y is the ground layer. tools/features/bio_bouw_dal.py checks them. */
    static final Map<String, int[]> ANKER = new LinkedHashMap<>();

    static {
        ANKER.put("dal_torii", new int[]{4, 1, 2});
        ANKER.put("dal_torii_water", new int[]{5, 3, 1});
        ANKER.put("dal_lantaarns", new int[]{5, 0, 3});
        ANKER.put("dal_boogbrug", new int[]{6, 2, 1});
        ANKER.put("dal_theehuisje", new int[]{3, 5, 1});
        ANKER.put("dal_zenhoek", new int[]{6, 0, 5});
        ANKER.put("dal_staptreden", new int[]{1, 0, 8});
        ANKER.put("weebhuisje", new int[]{7, 1, 7});
    }

    /** What the last zelftest found: structure id -> its start piece. */
    private static final Map<String, PoolElementStructurePiece> GEVONDEN = new LinkedHashMap<>();
    private static final UUID NEPSPELER = UUID.nameUUIDFromBytes("guhs bouw-dal dev".getBytes());

    /** Local template coordinates to the world, for this placed piece. */
    static BlockPos wereld(PoolElementStructurePiece stuk, int x, int y, int z) {
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, stuk.getRotation(), BlockPos.ZERO).offset(stuk.getPosition());
    }

    private static int tel(ServerLevel level, BoundingBox box, String... ids) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).getPath();
            for (String wil : ids) {
                if (id.equals(wil)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean water(ServerLevel level, BlockPos p) {
        return level.getFluidState(p).is(Fluids.WATER) || level.getFluidState(p).is(Fluids.FLOWING_WATER);
    }

    private static boolean vast(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && !s.getCollisionShape(level, p).isEmpty();
    }

    /** Around the box: how many columns just outside it have their ground within one block of gy. "a of b". */
    private static int[] rand(ServerLevel level, BoundingBox box, int gy) {
        int goed = 0, alle = 0;
        for (int x = box.minX() - 1; x <= box.maxX() + 1; x++) {
            for (int z = box.minZ() - 1; z <= box.maxZ() + 1; z++) {
                boolean opRand = x == box.minX() - 1 || x == box.maxX() + 1 || z == box.minZ() - 1 || z == box.maxZ() + 1;
                if (!opRand || ((x + z) & 1) != 0) {
                    continue;
                }
                alle++;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                if (Math.abs(top - gy) <= 1) {
                    goed++;
                }
            }
        }
        return new int[]{goed, alle};
    }

    static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        GEVONDEN.clear();
        BlockPos dal = BioZelftest.vind(level, Bio.KLATERDAL, BlockPos.ZERO, 6400);
        if (dal == null) {
            meld.fout("no Klaterdal within 6400 blocks");
            return;
        }
        var register = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Map.Entry<String, int[]> e : ANKER.entrySet()) {
            String id = e.getKey();
            var holder = register.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(id))).orElse(null);
            if (holder == null) {
                meld.fout("no structure " + id);
                continue;
            }
            var paar = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), dal, 80, false);
            if (paar == null) {
                meld.fout(id + ": none within 80 chunks of the Klaterdal at " + dal.toShortString());
                continue;
            }
            ChunkPos cp = ChunkPos.containing(paar.getFirst());
            ChunkAccess chunk = level.getChunk(cp.x(), cp.z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), holder.value(), chunk);
            if (start == null || !start.isValid() || start.getPieces().isEmpty() || !(start.getPieces().get(0) instanceof PoolElementStructurePiece stuk)) {
                meld.fout(id + ": no start in chunk " + cp);
                continue;
            }
            BoundingBox box = stuk.getBoundingBox();
            for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
                for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                    level.getChunk(cx, cz);
                }
            }
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    level.setChunkForced(cx, cz, true);
                }
            }
            GEVONDEN.put(id, stuk);
            int[] a = e.getValue();
            BlockPos anker = wereld(stuk, a[0], a[1], a[2]);
            String waar = id + " at " + anker.toShortString() + " (" + stuk.getRotation().name().toLowerCase() + ", "
                    + (int) Math.sqrt(anker.distSqr(dal)) + " from " + dal.toShortString() + "): ";
            // biomes3 fix-plaatsing: dal_torii_water stands in the pond at a river's mouth, which is biome Bloesemmeertje as often as Klaterdal
            meld.check(Bio.in(level, anker, Bio.KLATERDAL) || id.equals("dal_torii_water") && Bio.in(level, anker, Bio.BLOESEMMEERTJE), waar + "stands in the Klaterdal");
            kijk(level, id, stuk, anker, meld);
        }
    }

    private static void kijk(ServerLevel level, String id, PoolElementStructurePiece stuk, BlockPos anker, BioZelftest.Melder meld) {
        BoundingBox box = stuk.getBoundingBox();
        int gy = anker.getY();
        switch (id) {
            case "dal_torii", "dal_torii_water" -> {
                int balk = tel(level, box, "roze_lakhout_balk"), gezicht = tel(level, box, "knuffelsteen_gezicht"), krul = tel(level, box,
                        "guh_dakpan_grijs_hoek", "guh_dakpan_roze_hoek");
                meld.check(balk >= 14 && gezicht == 1 && krul == 2, id + ": " + balk + " balk, " + gezicht + " face plaque, " + krul + " krul ends");
                if (id.equals("dal_torii")) {
                    int[] r = rand(level, box, gy);
                    // (not inside a cliff, not on a slope: the ground around it is level, and its gate is open air)
                    boolean vrij = !vast(level, anker.above(1)) && !vast(level, anker.above(2)) && !vast(level, anker.above(3));
                    meld.check(r[0] * 4 >= r[1] * 3 && vrij, id + ": level ground at " + r[0] + " of " + r[1] + " spots around it, the gate is "
                            + (vrij ? "open" : "BLOCKED"));
                } else {
                    boolean nat = water(level, anker.below());
                    BlockPos voetL = wereld(stuk, 2, 1, 1), voetR = wereld(stuk, 8, 1, 1);
                    meld.check(nat && vast(level, voetL.below()) && vast(level, voetR.below()), id + ": water under the gate: " + nat
                            + ", posts stand on something: " + vast(level, voetL.below()) + "/" + vast(level, voetR.below())
                            + ", water beside the posts: " + water(level, wereld(stuk, 3, 2, 1)) + "/" + water(level, wereld(stuk, 7, 2, 1)));
                }
            }
            case "dal_lantaarns" -> {
                int toro = tel(level, box, "toro"), steen = tel(level, box, "gladde_knuffelsteen", "gladde_knuffelsteen_plaat");
                int[] r = rand(level, box, gy);
                meld.check(toro >= 1 && steen >= 5 && r[0] * 4 >= r[1] * 3, id + ": " + toro + " toro, " + steen + " path stones, level ground at "
                        + r[0] + " of " + r[1] + " spots around it");
            }
            case "dal_boogbrug" -> {
                int plaat = tel(level, box, "roze_lakhout_plaat");
                boolean nat = water(level, anker.below());
                // both ends: the first slab lies one above the bank, and there is ground under it
                BlockPos west = wereld(stuk, 0, 2, 1), oost = wereld(stuk, 12, 2, 1);
                int natOnder = 0;
                for (int x = 0; x <= 12; x++) {
                    natOnder += water(level, wereld(stuk, x, 1, 1)) ? 1 : 0;
                }
                meld.check(plaat >= 40 && nat && vast(level, west) && vast(level, oost) && natOnder >= 3 && natOnder <= 9,
                        id + ": " + plaat + " deck slabs, water under the middle: " + nat + ", the river is " + natOnder
                                + " wide under it, both ends rest on the bank: " + vast(level, west) + "/" + vast(level, oost));
            }
            case "dal_theehuisje" -> {
                int tafel = tel(level, box, "theetafel"), vloer = tel(level, box, "roze_lakhout_planken"), dak = tel(level, box, "guh_dakpan_roze",
                        "guh_dakpan_roze_plaat", "guh_dakpan_roze_hoek");
                int gedragen = 0, boven = 0;
                for (int x = 1; x <= 5; x++) {
                    for (int z = 1; z <= 5; z++) {
                        BlockPos onder = wereld(stuk, x, 5, z);
                        gedragen += vast(level, onder) ? 1 : 0;
                        boven += water(level, onder) || water(level, onder.below()) ? 1 : 0;
                    }
                }
                meld.check(tafel == 1 && vloer >= 9 && dak >= 30 && gedragen >= 10, id + ": the table, " + vloer + " floor planks, " + dak
                        + " roof pieces; " + gedragen + " of 25 deck columns stand on rock, " + boven + " hang over water");
            }
            case "dal_zenhoek" -> {
                int zand = tel(level, box, "geharkt_zand"), ring = tel(level, box, "geharkt_zand_ring");
                int[] r = rand(level, box, gy);
                meld.check(zand >= 20 && ring >= 12 && r[0] * 4 >= r[1] * 3, id + ": " + zand + " raked sand, " + ring + " ring pieces, level ground at "
                        + r[0] + " of " + r[1] + " spots around it");
            }
            case "dal_staptreden" -> {
                int treden = 0, nat = 0;
                for (int k = 1; k <= 7; k++) {
                    BlockPos trede = wereld(stuk, 1, k, 8 - k);
                    treden += level.getBlockState(trede).getBlock() instanceof StairBlock ? 1 : 0;
                    for (int y = 1; y <= 3; y++) {
                        nat += water(level, trede.above(y)) ? 1 : 0;
                    }
                }
                BlockPos voet = wereld(stuk, 1, 0, 8), top = wereld(stuk, 1, 7, 0), verder = wereld(stuk, 1, 7, -1);
                boolean boven = vast(level, verder) && !vast(level, verder.above());
                meld.check(treden == 7 && nat == 0 && vast(level, voet), id + ": " + treden + " steps, " + nat + " water blocks in the stair, foot on the ground: "
                        + vast(level, voet) + ", the top (" + top.toShortString() + ") leads onto level ground: " + boven);
            }
            case "weebhuisje" -> {
                int bord = tel(level, box, "weeb_loketbord"), brief = tel(level, box, "weeb_briefje"), fig = tel(level, box, "weeb_figuurtjes"),
                        poster = tel(level, box, "weeb_poster"), tatami = tel(level, box, "tatami"), steiger = tel(level, box, "ballonsteiger"),
                        shoji = tel(level, box, "shoji"), deur = tel(level, box, "roze_lakhout_deur");
                int[] r = rand(level, box, gy);
                meld.check(bord == 1 && brief == 1 && fig == 9 && poster == 4 && tatami == 35 && steiger == 9 && shoji == 12 && deur == 2
                                && tel(level, box, "weeb_loket") == 2 && tel(level, box, "weeb_nummerautomaat") == 1 && tel(level, box, "weeb_mangastapel") == 1
                                && tel(level, box, "weeb_dakimakura") == 1,
                        id + ": sign " + bord + ", note " + brief + ", shelves " + fig + ", posters " + poster + ", tatami " + tatami + ", steiger " + steiger
                                + ", shoji " + shoji + ", door halves " + deur + ", loket, automaat, manga, dakimakura");
                meld.check(r[0] * 4 >= r[1] * 3, id + ": level ground at " + r[0] + " of " + r[1] + " spots around it (ground y " + gy + ")");
            }
            default -> {
            }
        }
    }

    /**
     * How many of each structure really start within this many chunks of the first Klaterdal (the structure starts of every
     * chunk the set would try are worked out: slow, a dev command for a scratch server).
     */
    static List<String> tel(MinecraftServer server, int straal) {
        List<String> uit = new java.util.ArrayList<>();
        ServerLevel level = server.getLevel(ModDimensions.GUHMENSION);
        BlockPos dal = level == null ? null : BioZelftest.vind(level, Bio.KLATERDAL, BlockPos.ZERO, 6400);
        if (dal == null) {
            uit.add("[bouw-dal] tel: no Klaterdal");
            return uit;
        }
        var register = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var state = level.getChunkSource().getGeneratorState();
        int mx = dal.getX() >> 4, mz = dal.getZ() >> 4;
        for (String id : ANKER.keySet()) {
            var holder = register.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(id))).orElse(null);
            if (holder == null) {
                continue;
            }
            int geprobeerd = 0, gestart = 0;
            StringBuilder waar = new StringBuilder();
            for (var placement : state.getPlacementsForStructure(holder)) {
                for (int cx = mx - straal; cx <= mx + straal; cx++) {
                    for (int cz = mz - straal; cz <= mz + straal; cz++) {
                        if (!placement.isStructureChunk(state, cx, cz)) {
                            continue;
                        }
                        geprobeerd++;
                        ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
                        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), holder.value(), chunk);
                        if (start != null && start.isValid()) {
                            gestart++;
                            if (gestart <= 6) {
                                waar.append(' ').append(start.getBoundingBox().getCenter().toShortString().replace(", ", "/"));
                            }
                        }
                    }
                }
            }
            uit.add("[bouw-dal] tel " + id + ": " + gestart + " within " + straal + " chunks of " + dal.toShortString() + " (" + geprobeerd
                    + " chunks tried):" + waar);
        }
        return uit;
    }

    // --- the dev commands ---------------------------------------------------------------------------------------------------

    @Nullable
    private static WeebHuis huis(CommandSourceStack bron) {
        ServerLevel level = bron.getLevel();
        WeebHuis hier = WeebHuizen.get(bron.getServer()).op(level, BlockPos.containing(bron.getPosition()));
        if (hier != null) {
            return hier;
        }
        PoolElementStructurePiece stuk = GEVONDEN.get("weebhuisje");
        ServerLevel guhmensie = bron.getServer().getLevel(ModDimensions.GUHMENSION);
        if (stuk == null || guhmensie == null) {
            return null;
        }
        return WeebHuizen.bij(guhmensie, stuk.getBoundingBox().getCenter());
    }

    private static String status(MinecraftServer server, WeebHuis h) {
        ServerLevel level = server.getLevel(h.dim);
        if (level == null) {
            return "no level";
        }
        AABB zoek = h.zoek();
        int e = level.getEntitiesOfClass(GuhNpcEntity.class, zoek, x -> x.getKind() == GuhNpcEntity.Kind.WEEB_EVIVADS).size();
        int n = level.getEntitiesOfClass(GuhNpcEntity.class, zoek, x -> x.getKind() == GuhNpcEntity.Kind.WEEB_NIELSVADS).size();
        List<WeebBallonEntity> b = level.getEntitiesOfClass(WeebBallonEntity.class, zoek, x -> true);
        String bord = h.bord == null ? "?" : String.valueOf(level.getBlockState(h.bord).getOptionalValue(KleinBlok.Stand.AAN).orElse(null));
        String brief = h.briefje == null ? "?" : String.valueOf(level.getBlockState(h.briefje).getOptionalValue(KleinBlok.Stand.AAN).orElse(null));
        return "[bouw-dal] huis " + h.box.getCenter().toShortString() + ": " + h.staat() + " stap " + h.stap + ", reizen " + h.reizen() + ", geladen "
                + h.geladen(level) + ", Evivads " + e + ", Nielsvads " + n + ", ballon " + b.size() + (b.isEmpty() ? "" : " (y " + (int) b.get(0).getY()
                + (b.get(0).stijgt() ? ", stijgt" : "") + ")") + ", loket open " + bord + ", briefje hangt " + brief + ", zwaaiers " + h.zwaaiers.size()
                + ", tegoed " + h.tegoed + ", deur " + h.deur + ", steiger " + h.steiger;
    }

    static void commando(RegisterCommandsEvent event) {
        var cmd = Commands.literal("bouw-dal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        cmd.then(Commands.literal("check").executes(ctx -> {
            MinecraftServer server = ctx.getSource().getServer();
            ServerLevel level = server.getLevel(ModDimensions.GUHMENSION);
            int fout = 0;
            for (Map.Entry<String, PoolElementStructurePiece> e : GEVONDEN.entrySet()) {
                BoundingBox box = e.getValue().getBoundingBox();
                AABB zoek = AABB.of(box).inflate(1);
                String regel = null;
                boolean goed = true;
                if (e.getKey().equals("dal_theehuisje")) {
                    int n = level.getEntitiesOfClass(GuhNpcEntity.class, zoek, x -> x.getKind() == GuhNpcEntity.Kind.DAL_THEEGUH).size();
                    goed = n == 1;
                    regel = "dal_theehuisje: " + n + " thee-guh";
                } else if (e.getKey().equals("weebhuisje")) {
                    int ev = level.getEntitiesOfClass(GuhNpcEntity.class, zoek, x -> x.getKind() == GuhNpcEntity.Kind.WEEB_EVIVADS).size();
                    int ni = level.getEntitiesOfClass(GuhNpcEntity.class, zoek, x -> x.getKind() == GuhNpcEntity.Kind.WEEB_NIELSVADS).size();
                    int ba = level.getEntitiesOfClass(WeebBallonEntity.class, zoek.inflate(4), x -> true).size();
                    WeebHuis h = WeebHuizen.bij(level, box.getCenter());
                    int hersteld = h.geladen(level) ? h.zorg(level) : -1;
                    goed = ev == 1 && ni == 1 && ba == 1 && hersteld == 0;
                    regel = "weebhuisje: Evivads " + ev + ", Nielsvads " + ni + ", ballon " + ba + ", the repair put right: " + hersteld
                            + " (deur " + h.deur + ", steiger " + h.steiger + ", bord " + h.bord + ", briefje " + h.briefje + ")";
                }
                if (regel != null) {
                    String uit = "[bio-zelftest] bouw-dal check: " + (goed ? "OK " : "FOUT ") + regel;
                    fout += goed ? 0 : 1;
                    ctx.getSource().sendSuccess(() -> Component.literal(uit), false);
                }
            }
            return fout == 0 ? 1 : 0;
        }));
        cmd.then(Commands.literal("tel").then(Commands.argument("chunks", IntegerArgumentType.integer(1, 80)).executes(ctx -> {
            for (String regel : tel(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "chunks"))) {
                ctx.getSource().sendSuccess(() -> Component.literal(regel), false);
            }
            return 1;
        })));
        var reis = Commands.literal("reis");
        reis.then(Commands.literal("status").executes(ctx -> {
            WeebHuis h = huis(ctx.getSource());
            String uit = h == null ? "[bouw-dal] no house (run /guhs bio zelftest bouw-dal first, or stand in one)" : status(ctx.getSource().getServer(), h);
            ctx.getSource().sendSuccess(() -> Component.literal(uit), false);
            return h == null ? 0 : 1;
        }));
        reis.then(Commands.literal("start").executes(ctx -> {
            WeebHuis h = huis(ctx.getSource());
            ServerLevel level = h == null ? null : ctx.getSource().getServer().getLevel(h.dim);
            UUID wie = ctx.getSource().getPlayer() != null ? ctx.getSource().getPlayer().getUUID() : NEPSPELER;
            boolean ok = level != null && h.begin(level, wie);
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-dal] reis start: " + ok), false);
            return ok ? 1 : 0;
        }));
        reis.then(Commands.literal("zorg").executes(ctx -> {
            WeebHuis h = huis(ctx.getSource());
            ServerLevel level = h == null ? null : ctx.getSource().getServer().getLevel(h.dim);
            int n = level == null ? -1 : h.zorg(level);
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-dal] zorg put right: " + n), false);
            return 1;
        }));
        reis.then(Commands.literal("klok").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 1000000)).executes(ctx -> {
            WeebHuis h = huis(ctx.getSource());
            if (h != null) {
                h.klokVooruit(IntegerArgumentType.getInteger(ctx, "ticks"));
            }
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-dal] klok vooruit: " + (h != null)), false);
            return h == null ? 0 : 1;
        })));
        cmd.then(reis);
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(cmd)));
    }

    private BouwDalCheck() {
    }
}
