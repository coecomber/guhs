package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * The dev commands and the self test of slice bouw-wolk1 (gamemasters and the console; every line starts with
 * "[bio-bouw-wolk1]"; run them in the Guhmensie: {@code execute in guhs:guhmension run guhs bio bouw-wolk1 ...}).
 * <ul>
 *   <li>{@code inspecteer <structuur> <aantal>}: finds that many different ones of the structure (from 0 0 outwards),
 *       generates the chunks of each, and compares the world with the template block by block: template blocks that
 *       are not there ("anders"), blocks inside the building's box, more than 6 above the meadow, that are not the
 *       template's ("vreemd": a hill of older terrain, a natural island or cloud in the box) and how many of those lie
 *       within two blocks of the building itself ("raakt": it really touches), water outside the template's water ("lek") and template water
 *       that is gone ("droog"), the NPCs, herd schaapjes and balloons present, the lift pads against the real meadow.
 *       It also gives every water block of the template a fluid tick and keeps the chunks loaded, so a second
 *       {@code inspecteer} a while later shows whether the water stayed where it was written;</li>
 *   <li>{@code dump <structuur> <nummer>}: writes the blocks in and around the nummer-th one to
 *       {@code bio_bouw_wolk1_<structuur>_<nummer>.txt} in the server directory (the offline renderer draws it).</li>
 * </ul>
 */
public final class BouwWolk1Commando {
    /** Template layers below this are the cloud feet (WolkvoetProcessor): they may be missing where the meadow is. */
    public static final int VOET = 5;

    /** One found building: its start, its only piece, the template blocks turned and moved to their place in the world. */
    record Gevonden(String naam, BlockPos anker, BoundingBox box, Map<BlockPos, String> blokken, Set<BlockPos> voet) {
    }

    static void registreer(RegisterCommandsEvent event) {
        var wolk = Commands.literal("bouw-wolk1").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        wolk.then(Commands.literal("inspecteer").then(Commands.argument("structuur", StringArgumentType.word())
                .then(Commands.argument("aantal", IntegerArgumentType.integer(1, 6)).executes(c -> {
                    for (String r : inspecteer(c.getSource().getLevel(), StringArgumentType.getString(c, "structuur"), IntegerArgumentType.getInteger(c, "aantal"))) {
                        zeg(c.getSource(), r);
                    }
                    return 1;
                }))));
        wolk.then(Commands.literal("dump").then(Commands.argument("structuur", StringArgumentType.word())
                .then(Commands.argument("nummer", IntegerArgumentType.integer(1, 6)).executes(c -> {
                    zeg(c.getSource(), dump(c.getSource().getLevel(), StringArgumentType.getString(c, "structuur"), IntegerArgumentType.getInteger(c, "nummer")));
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(wolk)));
    }

    private static void zeg(CommandSourceStack bron, String regel) {
        bron.sendSuccess(() -> Component.literal("[bio-bouw-wolk1] " + regel), false);
    }

    // --- finding --------------------------------------------------------------------------------------------------------

    /** Up to {@code aantal} different starts of this structure, looked for from 0 0 and then from spots around what was found. */
    static List<StructureStart> vind(ServerLevel level, String naam, int aantal) {
        List<StructureStart> uit = new ArrayList<>();
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam))).orElse(null);
        if (holder == null) {
            return uit;
        }
        List<BlockPos> van = new ArrayList<>(List.of(BlockPos.ZERO));
        for (int ring = 1; ring <= 2; ring++) {                  // (other Wolkenweides: a coarse grid of places to look from)
            for (int gx = -ring; gx <= ring; gx++) {
                for (int gz = -ring; gz <= ring; gz++) {
                    if (Math.max(Math.abs(gx), Math.abs(gz)) == ring) {
                        van.add(new BlockPos(gx * 1600, 0, gz * 1600));
                    }
                }
            }
        }
        Set<Long> gezien = new HashSet<>();
        for (int i = 0; i < van.size() && uit.size() < aantal && i < 60; i++) {
            Pair<BlockPos, Holder<Structure>> p = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), van.get(i), 100, false);
            if (p == null) {
                continue;
            }
            BlockPos pos = p.getFirst();
            if (!gezien.add(net.minecraft.world.level.ChunkPos.pack(pos))) {
                continue;
            }
            StructureStart start = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(holder.value());
            if (start != null && start.isValid()) {
                uit.add(start);
            }
        }
        return uit;
    }

    /** The template of a start, block by block, where the world should have it. */
    static Gevonden lees(ServerLevel level, String naam, StructureStart start) {
        Map<BlockPos, String> blokken = new HashMap<>();
        Set<BlockPos> voet = new HashSet<>();
        BlockPos anker = BlockPos.ZERO;
        if (start.getPieces().isEmpty() || !(start.getPieces().get(0) instanceof PoolElementStructurePiece stuk)) {
            return new Gevonden(naam, anker, start.getBoundingBox(), blokken, voet);
        }
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        if (template == null) {
            return new Gevonden(naam, anker, start.getBoundingBox(), blokken, voet);
        }
        StructurePlaceSettings zo = new StructurePlaceSettings().setRotation(stuk.getRotation());
        CompoundTag nbt = template.save(new CompoundTag());
        ListTag palet = nbt.getListOrEmpty("palette");
        for (var t : nbt.getListOrEmpty("blocks")) {
            CompoundTag b = (CompoundTag) t;
            int[] p = b.getIntArray("pos").orElse(new int[0]);
            if (p.length != 3) {
                ListTag lijst = b.getListOrEmpty("pos");
                p = new int[]{lijst.getIntOr(0, 0), lijst.getIntOr(1, 0), lijst.getIntOr(2, 0)};
            }
            CompoundTag staat = palet.getCompoundOrEmpty(b.getIntOr("state", 0));
            String blok = staat.getStringOr("Name", "minecraft:air");
            BlockPos wereld = stuk.getPosition().offset(StructureTemplate.calculateRelativePosition(zo, new BlockPos(p[0], p[1], p[2])));
            if (blok.equals("minecraft:jigsaw")) {
                anker = wereld;
                blok = b.getCompoundOrEmpty("nbt").getStringOr("final_state", "minecraft:air");
            }
            blokken.put(wereld, blok);
            if (p[1] < VOET) {
                voet.add(wereld);
            }
        }
        return new Gevonden(naam, anker, stuk.getBoundingBox(), blokken, voet);
    }

    private static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static void laad(ServerLevel level, BoundingBox box, boolean blijf) {
        for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
            for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                level.getChunk(cx, cz);
                if (blijf) {
                    level.setChunkForced(cx, cz, true);
                }
            }
        }
    }

    // --- inspecting -----------------------------------------------------------------------------------------------------

    static List<String> inspecteer(ServerLevel level, String naam, int aantal) {
        List<String> uit = new ArrayList<>();
        List<StructureStart> starts = vind(level, naam, aantal);
        uit.add(naam + ": " + starts.size() + " of " + aantal + " found");
        int nr = 0;
        for (StructureStart start : starts) {
            nr++;
            Gevonden g = lees(level, naam, start);
            laad(level, g.box(), true);
            uit.add(rapport(level, g, nr));
        }
        return uit;
    }

    /** One line: what the world has of this building. Every water block of the template also gets a fluid tick. */
    static String rapport(ServerLevel level, Gevonden g, int nr) {
        int anders = 0, voetGezet = 0, droog = 0, lek = 0, vreemd = 0, raakt = 0, vreemdTop = Integer.MIN_VALUE, water = 0;
        List<String> voorbeelden = new ArrayList<>();
        for (Map.Entry<BlockPos, String> e : g.blokken().entrySet()) {
            BlockState echt = level.getBlockState(e.getKey());
            boolean klopt = id(echt).equals(e.getValue());
            if (g.voet().contains(e.getKey())) {
                voetGezet += klopt ? 1 : 0;
                continue;
            }
            if (e.getValue().equals("minecraft:water")) {
                water++;
                if (echt.getFluidState().isEmpty()) {
                    droog++;
                } else {
                    level.scheduleTick(e.getKey(), Fluids.WATER, 1);
                }
                continue;
            }
            if (!klopt) {
                anders++;
                if (voorbeelden.size() < 4) {
                    voorbeelden.add(e.getValue() + " -> " + id(echt) + " at " + e.getKey().toShortString());
                }
            }
        }
        BoundingBox box = g.box();
        int voetTop = g.voet().stream().mapToInt(BlockPos::getY).max().orElse(box.minY());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        List<String> vreemde = new ArrayList<>();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    BlockState echt = level.getBlockState(p.set(x, y, z));
                    if (echt.isAir() || g.blokken().containsKey(p)) {
                        continue;
                    }
                    if (!echt.getFluidState().isEmpty()) {
                        lek++;
                        if (vreemde.size() < 4) {
                            vreemde.add("water at " + p.toShortString());
                        }
                    } else if (y > voetTop + 6) {            // (below that the meadow itself lies in the box)
                        vreemd++;
                        vreemdTop = Math.max(vreemdTop, y);
                        // does it touch the building (a template block within two blocks)?
                        boolean dichtbij = false;
                        for (int a = -2; a <= 2 && !dichtbij; a++) {
                            for (int b = -2; b <= 2 && !dichtbij; b++) {
                                for (int c = -2; c <= 2 && !dichtbij; c++) {
                                    BlockPos q = new BlockPos(x + a, y + b, z + c);
                                    dichtbij = g.blokken().containsKey(q) && !g.voet().contains(q) && !g.blokken().get(q).startsWith("guhs:wolken");
                                }
                            }
                        }
                        if (dichtbij) {
                            raakt++;
                            if (vreemde.size() < 4) {
                                vreemde.add(id(echt) + " at " + p.toShortString());
                            }
                        }
                    }
                }
            }
        }
        AABB ruim = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(4);
        Map<String, Integer> wezens = new HashMap<>();
        for (Entity e : level.getEntities((Entity) null, ruim, e -> true)) {
            String soort = e instanceof GuhNpcEntity n ? "npc " + n.getKind().id() + (NpcRollen.van(n) instanceof Hoeder ? " (les)" : "")
                    : Kudde.isKudde(e) ? "kudde-schaapje" : BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            wezens.merge(soort, 1, Integer::sum);
        }
        // the lift pads against the real meadow: how far the ground beside a pad lies from the pad's own height
        StringBuilder pads = new StringBuilder();
        for (Map.Entry<BlockPos, String> e : g.blokken().entrySet()) {
            BlockPos q = e.getKey();
            if (e.getValue().equals("guhs:wolkenlift") && g.blokken().getOrDefault(q.north(), "").equals("guhs:wolkenlift")
                    && g.blokken().getOrDefault(q.south(), "").equals("guhs:wolkenlift") && g.blokken().getOrDefault(q.east(), "").equals("guhs:wolkenlift")
                    && g.blokken().getOrDefault(q.west(), "").equals("guhs:wolkenlift")) {
                int laag = Integer.MAX_VALUE, hoog = Integer.MIN_VALUE;
                for (int[] d : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                    int h = stap(level, q.getX() + d[0], q.getZ() + d[1], q.getY() + 8);
                    laag = Math.min(laag, h);
                    hoog = Math.max(hoog, h);
                }
                int kolom = 0;
                while (id(level.getBlockState(q.above(kolom + 1))).equals("guhs:wolkenstroom")) {
                    kolom++;
                }
                pads.append(" pad ").append(q.toShortString()).append(" stream ").append(kolom).append(" high, ground beside it ")
                        .append(laag - q.getY()).append("..").append(hoog - q.getY()).append(" from the pad;");
            }
        }
        return g.naam() + " #" + nr + " anchor " + g.anker().toShortString() + " box " + box.minX() + " " + box.minY() + " " + box.minZ() + " .. " + box.maxX() + " "
                + box.maxY() + " " + box.maxZ() + ": template " + (g.blokken().size() - g.voet().size()) + " blocks, anders " + anders + ", vreemd " + vreemd
                + (vreemd > 0 ? " (up to y " + vreemdTop + ", island bottom y " + eilandBodem(g) + ")" : "") + " raakt " + raakt
                + ", water " + water + " droog " + droog + " lek " + lek + ", feet " + voetGezet + " of " + g.voet().size() + " placed;" + pads + " entities "
                + new java.util.TreeMap<>(wezens) + (voorbeelden.isEmpty() ? "" : " | anders: " + voorbeelden) + (vreemde.isEmpty() ? "" : " | vreemd/lek: " + vreemde);
    }

    /** The lowest block of the building itself (not a lift column, not a cloud foot). */
    private static int eilandBodem(Gevonden g) {
        int laag = Integer.MAX_VALUE;
        for (Map.Entry<BlockPos, String> e : g.blokken().entrySet()) {
            if (!g.voet().contains(e.getKey()) && !e.getValue().startsWith("guhs:wolken")) {
                laag = Math.min(laag, e.getKey().getY());
            }
        }
        return laag;
    }

    /** The y of the highest block one can stand on in this column at or below {@code vanaf} (the top of the ground or of a foot). */
    private static int stap(ServerLevel level, int x, int z, int vanaf) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, vanaf, z);
        while (p.getY() > level.getMinY() && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            p.move(0, -1, 0);
        }
        return p.getY();
    }

    // --- dumping --------------------------------------------------------------------------------------------------------

    static String dump(ServerLevel level, String naam, int nummer) {
        List<StructureStart> starts = vind(level, naam, nummer);
        if (starts.size() < nummer) {
            return "dump " + naam + " " + nummer + ": only " + starts.size() + " found";
        }
        Gevonden g = lees(level, naam, starts.get(nummer - 1));
        laad(level, g.box(), false);
        BoundingBox box = g.box();
        int rand = 10;
        Path uit = Path.of("bio_bouw_wolk1_" + naam + "_" + nummer + ".txt");
        int n = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(uit, StandardCharsets.UTF_8))) {
            w.println("# " + naam + " " + nummer + " anchor " + g.anker().toShortString() + " size " + (box.getXSpan() + 2 * rand) + " " + (box.getYSpan() + 6) + " "
                    + (box.getZSpan() + 2 * rand));
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = box.minX() - rand; x <= box.maxX() + rand; x++) {
                for (int z = box.minZ() - rand; z <= box.maxZ() + rand; z++) {
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = box.minY() - 4; y <= Math.min(top, box.maxY() + 1); y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (!s.isAir() && s.getBlock() != Blocks.CAVE_AIR) {
                            w.println((x - box.minX() + rand) + " " + (y - box.minY() + 4) + " " + (z - box.minZ() + rand) + " " + BlockStateParser.serialize(s));
                            n++;
                        }
                    }
                }
            }
            for (Entity e : level.getEntities((Entity) null, new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(4), e -> true)) {
                String soort = e instanceof GuhNpcEntity npc ? "npc_" + npc.getKind().id() : BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
                w.println("@ " + (e.getX() - box.minX() + rand) + " " + (e.getY() - box.minY() + 4) + " " + (e.getZ() - box.minZ() + rand) + " " + e.getYRot() + " " + soort);
            }
        } catch (IOException e) {
            return "dump " + naam + ": " + e;
        }
        return "dump " + naam + " " + nummer + ": " + n + " blocks written to " + uit.toAbsolutePath();
    }

    // --- the self test ----------------------------------------------------------------------------------------------------

    /** {@code /guhs bio zelftest bouw_wolk1}: the data landed (structures, sets, templates, roles) and each structure can be found. */
    static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        var structuren = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String naam : BouwWolk1Slice.STRUCTUREN) {
            var s = structuren.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam)));
            meld.check(s.isPresent() && BuiltInRegistries.STRUCTURE_TYPE.getKey(s.get().value().type()).equals(Guhs.id("bio_plek")), naam + " is a guhs:bio_plek structure");
            var set = sets.get(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(naam)));
            boolean dun = set.isPresent() && set.get().value().placement() instanceof RandomSpreadStructurePlacement r && r.spacing() >= 8;
            meld.check(dun, naam + ": its set is a random spread with spacing 8 or more"
                    + (set.isPresent() && set.get().value().placement() instanceof RandomSpreadStructurePlacement r ? " (" + r.spacing() + "/" + r.separation() + ")" : ""));
            var template = level.getStructureManager().get(Guhs.id(naam));
            meld.check(template.isPresent(), naam + ": template " + template.map(t -> t.getSize().toShortString()).orElse("missing"));
            List<StructureStart> starts = vind(level, naam, 1);
            if (starts.isEmpty()) {
                meld.fout(naam + ": none found within 100 chunks of 0 0 and of the spots tried");
            } else {
                Gevonden g = lees(level, naam, starts.get(0));
                meld.ok(naam + ": nearest at " + g.anker().toShortString() + " (" + (int) Math.sqrt(g.anker().distSqr(new BlockPos(0, g.anker().getY(), 0))) + " blocks from 0 0)");
            }
        }
        meld.check(NpcRollen.rol(GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER) != null && NpcRollen.rol(GuhNpcEntity.Kind.BALLONVAARDERGUH) != null,
                "the two new NPC kinds have their roles");
        meld.check(Kudde.soort().isPresent(), "the wolkenschaapje of slice dieren is there for the herd");
    }

    private BouwWolk1Commando() {
    }
}
