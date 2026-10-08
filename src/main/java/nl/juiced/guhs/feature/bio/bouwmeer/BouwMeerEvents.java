package nl.juiced.guhs.feature.bio.bouwmeer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.Terugkeer;

/**
 * The game-bus side of the biomes3 slice "bouw-meer":
 * <ul>
 *   <li>every two seconds, for a player in the new biomes of the Guhmensie: the botenhuisjes and picknickeilandjes within
 *       two chunks are kept in order ({@link MeerpaalBlock#zorg}, {@link MandBlock#zorg}). Which ones are near is looked
 *       up from the chunks' structure references (no block scan) and remembered per player for ten seconds;</li>
 *   <li>the deeds of the visser-guh's lessons: koivoer used near a koi, and once a second the island and the bucket
 *       ({@link Visserguh});</li>
 *   <li>the dev commands {@code /guhs bio bouw-meer ...} and the dev-server self test.</li>
 * </ul>
 */
public final class BouwMeerEvents {
    public static final ResourceKey<Structure> BOTENHUISJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("botenhuisje"));
    public static final ResourceKey<Structure> PICKNICKEILANDJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("picknickeilandje"));
    /** Where the anchors stand in their templates (tools/features/bio_bouw_meer_bouw.py: MEERPAAL, MAND). */
    public static final BlockPos MEERPAAL_LOKAAL = new BlockPos(6, 8, 15), MAND_LOKAAL = new BlockPos(7, 1, 5);
    /** How often a player's surroundings are kept in order (ticks), how far (chunks), how long the lookup is kept. */
    public static final int ZORG_STAP = 40, ZORG_CHUNKS = 2, ZOEK_STAP = 200;

    private record Anker(BlockPos pos, boolean steiger) {
    }

    private record Buurt(long tijd, long chunk, List<Anker> ankers) {
    }

    private static final Map<UUID, Buurt> BUURT = new ConcurrentHashMap<>();

    private BouwMeerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        int t = p.tickCount + p.getId();
        if (t % 20 == 0) {
            Visserguh.tik(p);
        }
        if (t % ZORG_STAP == 0 && p.level().dimension() == ModDimensions.GUHMENSION && Bio.inNieuw(p.level(), p.blockPosition())) {
            omgeving(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BUURT.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer p) {
            Visserguh.vergeet(p);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getItemStack().is(Visserguh.koivoer())) {
            Visserguh.gevoerd(p);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getItemStack().is(Visserguh.koivoer())
                && BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()).equals(Guhs.id("koi"))) {
            Visserguh.gevoerd(p);
        }
    }

    /** Keeps the botenhuisjes and picknickeilandjes around this player in order; returns how many it found. */
    public static int omgeving(ServerPlayer p) {
        ServerLevel level = p.level();
        long nu = level.getGameTime(), chunk = p.chunkPosition().pack();
        Buurt b = BUURT.get(p.getUUID());
        if (b == null || b.chunk() != chunk || nu - b.tijd() >= ZOEK_STAP) {
            b = new Buurt(nu, chunk, zoek(level, p.chunkPosition(), ZORG_CHUNKS));
            BUURT.put(p.getUUID(), b);
        }
        for (Anker a : b.ankers()) {
            if (!level.isLoaded(a.pos())) {
                continue;
            }
            if (a.steiger()) {
                MeerpaalBlock.zorg(level, a.pos());
            } else {
                MandBlock.zorg(level, a.pos());
            }
        }
        return b.ankers().size();
    }

    /** The anchors of our two structures whose pieces reach into the chunks around this one (loaded chunks only). */
    static List<Anker> zoek(ServerLevel level, ChunkPos midden, int chunks) {
        List<Anker> uit = new ArrayList<>();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Structure steiger = registry.getValue(BOTENHUISJE), picknick = registry.getValue(PICKNICKEILANDJE);
        if (steiger == null && picknick == null) {
            return uit;
        }
        Set<Long> gehad = new HashSet<>();
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                if (!level.hasChunk(midden.x() + dx, midden.z() + dz)) {
                    continue;
                }
                for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(midden.x() + dx, midden.z() + dz),
                        s -> s == steiger || s == picknick)) {
                    if (!start.isValid() || !gehad.add(start.getChunkPos().pack() ^ (start.getStructure() == steiger ? 1L << 62 : 0))) {
                        continue;
                    }
                    for (StructurePiece piece : start.getPieces()) {
                        if (piece instanceof PoolElementStructurePiece stuk) {
                            boolean isSteiger = start.getStructure() == steiger;
                            uit.add(new Anker(Terugkeer.wereld(stuk, isSteiger ? MEERPAAL_LOKAAL : MAND_LOKAAL), isSteiger));
                            break;
                        }
                    }
                }
            }
        }
        return uit;
    }

    // --- dev ------------------------------------------------------------------------------------------------------------------

    /**
     * {@code /guhs bio bouw-meer stap <0..5>} puts you at that step of the visser-guh's lessons;
     * {@code ... klok <dag> <tijd>} sets this slice's own clock in a box of 96 around you ({@code klok echt}: the real one);
     * {@code ... zorg [x y z]} keeps the buildings near you (or near that spot) in order now and says what it found;
     * {@code ... kijk <x y z> <x y z> <naam>} writes the blocks of a box to {@code bio_bouw_meer_<naam>.json} in the
     * server directory (guhs_workbio/reports/screens/bouw-meer/_bron/wereld.py draws it): to look at a generated
     * building without a client;
     * {@code ... tel <straal>} counts the buildings per lake around you without generating anything ({@link Telling}).
     * Gamemasters only.
     */
    static void commando(RegisterCommandsEvent event) {
        var stap = Commands.literal("stap").then(Commands.argument("stap", IntegerArgumentType.integer(0, 5)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Visserguh.zetStap(p, IntegerArgumentType.getInteger(ctx, "stap"));
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] stap " + Visserguh.stap(p)), false);
            return 1;
        }));
        var klok = Commands.literal("klok")
                .then(Commands.literal("echt").executes(ctx -> {
                    KLOK_BOXEN.forEach(box -> Klok.zet(box, null, 0));
                    KLOK_BOXEN.clear();
                    ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] de echte klok"), false);
                    return 1;
                }))
                .then(Commands.argument("dag", IntegerArgumentType.integer(0)).then(Commands.argument("tijd", IntegerArgumentType.integer(0, 23999)).executes(ctx -> {
                    AABB box = new AABB(BlockPos.containing(ctx.getSource().getPosition())).inflate(96);
                    KLOK_BOXEN.add(box);
                    Klok.zet(box, (long) IntegerArgumentType.getInteger(ctx, "dag"), IntegerArgumentType.getInteger(ctx, "tijd"));
                    ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] klok hier: dag " + IntegerArgumentType.getInteger(ctx, "dag")
                            + ", tijd " + IntegerArgumentType.getInteger(ctx, "tijd")), false);
                    return 1;
                })));
        var zorg = Commands.literal("zorg").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BUURT.remove(p.getUUID());
            int n = omgeving(p);
            Buurt b = BUURT.get(p.getUUID());
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] " + n + " in de buurt: " + (b == null ? "" : b.ankers())), false);
            return n;
        }).then(Commands.argument("plek", BlockPosArgument.blockPos()).executes(ctx -> {
            // (also from the console, in loaded chunks: forceload first) the buildings around a spot
            ServerLevel level = ctx.getSource().getLevel();
            List<Anker> ankers = zoek(level, ChunkPos.containing(BlockPosArgument.getBlockPos(ctx, "plek")), ZORG_CHUNKS);
            StringBuilder regel = new StringBuilder();
            for (Anker a : ankers) {
                int nieuw = a.steiger() ? MeerpaalBlock.zorg(level, a.pos()) : MandBlock.zorg(level, a.pos());
                regel.append(a.steiger() ? " botenhuisje " : " picknickeilandje ").append(a.pos().toShortString()).append(" (").append(nieuw).append(" nieuw)");
            }
            ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] zorg: " + ankers.size() + regel), false);
            return ankers.size();
        }));
        var kijk = Commands.literal("kijk").then(Commands.argument("van", BlockPosArgument.blockPos()).then(Commands.argument("tot", BlockPosArgument.blockPos())
                .then(Commands.argument("naam", StringArgumentType.word()).executes(ctx -> {
                    String regel = kijk(ctx.getSource().getLevel(), BlockPosArgument.getBlockPos(ctx, "van"), BlockPosArgument.getBlockPos(ctx, "tot"),
                            StringArgumentType.getString(ctx, "naam"));
                    ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] " + regel), false);
                    return 1;
                }))));
        var tel = Commands.literal("tel").then(Commands.argument("straal", IntegerArgumentType.integer(200, 8000)).executes(ctx -> {
            List<String> regels = Telling.tel(ctx.getSource().getLevel(), BlockPos.containing(ctx.getSource().getPosition()), IntegerArgumentType.getInteger(ctx, "straal"));
            for (String regel : regels) {
                ctx.getSource().sendSuccess(() -> Component.literal("[bouw-meer] tel: " + regel), false);
            }
            return regels.size();
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("bouw-meer").then(stap).then(klok).then(zorg).then(kijk).then(tel))));
    }

    private static final List<AABB> KLOK_BOXEN = new ArrayList<>();

    private static String staat(BlockState s) {
        StringBuilder b = new StringBuilder(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString());
        if (!s.getProperties().isEmpty()) {
            b.append('[');
            boolean eerste = true;
            for (Property<?> prop : s.getProperties()) {
                if (!eerste) {
                    b.append(',');
                }
                eerste = false;
                b.append(prop.getName()).append('=').append(waarde(s, prop));
            }
            b.append(']');
        }
        return b.toString();
    }

    private static <T extends Comparable<T>> String waarde(BlockState s, Property<T> prop) {
        return prop.getName(s.getValue(prop));
    }

    /** Writes the blocks of the box (chunks are generated when needed) and the entities in it; returns a line about it. */
    static String kijk(ServerLevel level, BlockPos a, BlockPos b, String naam) {
        int x0 = Math.min(a.getX(), b.getX()), y0 = Math.min(a.getY(), b.getY()), z0 = Math.min(a.getZ(), b.getZ());
        int x1 = Math.max(a.getX(), b.getX()), y1 = Math.max(a.getY(), b.getY()), z1 = Math.max(a.getZ(), b.getZ());
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > 400000) {
            return "kijk: the box is too big";
        }
        Map<String, Integer> palet = new LinkedHashMap<>();
        StringBuilder blokken = new StringBuilder();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    String s = staat(level.getBlockState(p.set(x, y, z)));
                    Integer i = palet.get(s);
                    if (i == null) {
                        i = palet.size();
                        palet.put(s, i);
                    }
                    blokken.append(i).append(',');
                }
            }
        }
        StringBuilder uit = new StringBuilder("{\"van\":[" + x0 + "," + y0 + "," + z0 + "],\"tot\":[" + x1 + "," + y1 + "," + z1 + "],\"palet\":[");
        boolean eerste = true;
        for (String s : palet.keySet()) {
            uit.append(eerste ? "" : ",").append('"').append(s).append('"');
            eerste = false;
        }
        uit.append("],\"blokken\":[").append(blokken, 0, Math.max(0, blokken.length() - 1)).append("],\"wezens\":[");
        eerste = true;
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1), e -> true)) {
            String soort = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            String kind = e instanceof GuhNpcEntity npc ? npc.getKind().id() : "";
            uit.append(eerste ? "" : ",").append("{\"soort\":\"").append(soort).append("\",\"kind\":\"").append(kind).append("\",\"pos\":[")
                    .append(e.getX()).append(',').append(e.getY()).append(',').append(e.getZ()).append("],\"yaw\":").append(e.getYRot()).append('}');
            eerste = false;
        }
        uit.append("]}");
        Path pad = Path.of("bio_bouw_meer_" + naam + ".json");
        try {
            Files.writeString(pad, uit.toString());
        } catch (IOException e) {
            return "kijk: " + e;
        }
        return "kijk: " + palet.size() + " kinds of block written to " + pad.toAbsolutePath();
    }

    // --- the dev-server self test (the real Guhmensie) -------------------------------------------------------------------------

    /** Lakes are looked for around these spots (blocks); a building within this many chunks of the lake belongs to it. */
    private static final int[][] ZOEK_VANAF = {{0, 0}, {2500, 0}, {0, 2500}, {-2500, 0}, {0, -2500}, {2500, 2500}, {-2500, -2500}, {2500, -2500}};
    private static final int BIJ_MEER = 20;

    static void zelftest() {
        BioZelftest.registreer("bouw_meer", (server, level, meld) -> {
            var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (ResourceKey<Structure> key : List.of(BOTENHUISJE, PICKNICKEILANDJE)) {
                var holder = registry.get(key);
                meld.check(holder.isPresent() && BuiltInRegistries.STRUCTURE_TYPE.getKey(holder.get().value().type()).equals(Guhs.id("bio_plek")),
                        "structure " + key.identifier() + " is a guhs:bio_plek structure");
            }
            meld.check(!server.reloadableRegistries().getLootTable(MandBlock.LOOT).getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST)).isEmpty(),
                    "the picknickmand's loot table gives something");
            int adv = 0;
            for (String a : BouwMeerSlice.BEWIJZEN) {
                adv += server.getAdvancements().get(Guhs.id("quest/" + a)) != null ? 1 : 0;
            }
            meld.check(adv == BouwMeerSlice.BEWIJZEN.size(), "the proof advancements are loaded (" + adv + " of " + BouwMeerSlice.BEWIJZEN.size() + ")");
            int recepten = 0;
            for (String id : List.of("botenhuisje_steigerlantaarn", "botenhuisje_hengelstandaard", "botenhuisje_koiwindzak")) {
                recepten += server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent() ? 1 : 0;
            }
            meld.check(recepten == 3, "the recipes of the three decorations are loaded (" + recepten + " of 3)");
            // the buildings themselves, at the lakes nearest to a few far-apart spots
            Set<BlockPos> meren = new HashSet<>();
            Set<BlockPos> gezien = new HashSet<>();
            int steigers = 0, picknicks = 0;
            for (int[] van : ZOEK_VANAF) {
                BlockPos meer = BioZelftest.vind(level, Bio.BLOESEMMEERTJE, new BlockPos(van[0], 60, van[1]), 2400);
                if (meer == null || meren.stream().anyMatch(m -> m.distSqr(meer) < 300 * 300)) {
                    continue;
                }
                meren.add(meer);
                for (ResourceKey<Structure> key : List.of(BOTENHUISJE, PICKNICKEILANDJE)) {
                    var holder = registry.get(key).orElse(null);
                    if (holder == null) {
                        continue;
                    }
                    var paar = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), meer, BIJ_MEER, false);
                    if (paar == null || !gezien.add(paar.getFirst())) {
                        meld.ok("lake at " + meer.getX() + " " + meer.getZ() + ": no " + key.identifier().getPath() + " within " + BIJ_MEER + " chunks");
                        continue;
                    }
                    BlockPos bij = paar.getFirst();
                    if (key == BOTENHUISJE && steigers < 3) {
                        steigers++;
                        bekijkSteiger(level, meer, bij, meld);
                    } else if (key == PICKNICKEILANDJE && picknicks < 3) {
                        picknicks++;
                        bekijkPicknick(level, meer, bij, meld);
                    }
                }
            }
            meld.check(steigers >= 2, "looked at " + steigers + " botenhuisjes at " + meren.size() + " lakes (at least two wanted)");
            meld.check(picknicks >= 2, "looked at " + picknicks + " picknickeilandjes at " + meren.size() + " lakes (at least two wanted)");
        });
    }

    private static PoolElementStructurePiece stuk(ServerLevel level, ResourceKey<Structure> key, BlockPos bij) {
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(key);
        ChunkPos c = ChunkPos.containing(bij);
        level.getChunk(c.x(), c.z());                    // (generates it: a scratch dev server may)
        for (StructureStart start : level.structureManager().startsForStructure(c, s -> s == structure)) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof PoolElementStructurePiece p) {
                    return p;
                }
            }
        }
        return null;
    }

    private static void laad(ServerLevel level, BlockPos midden, int straal) {
        for (int cx = (midden.getX() - straal) >> 4; cx <= (midden.getX() + straal) >> 4; cx++) {
            for (int cz = (midden.getZ() - straal) >> 4; cz <= (midden.getZ() + straal) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    private static void bekijkSteiger(ServerLevel level, BlockPos meer, BlockPos bij, BioZelftest.Melder meld) {
        PoolElementStructurePiece stuk = stuk(level, BOTENHUISJE, bij);
        String waar = "botenhuisje near " + bij.getX() + " " + bij.getZ() + " (lake " + meer.getX() + " " + meer.getZ() + ")";
        if (stuk == null) {
            meld.fout(waar + ": no piece found");
            return;
        }
        BlockPos anker = Terugkeer.wereld(stuk, MEERPAAL_LOKAAL);
        laad(level, anker, 24);
        BlockState state = level.getBlockState(anker);
        if (!(state.getBlock() instanceof MeerpaalBlock)) {
            meld.fout(waar + ": no meerpaal at " + anker.toShortString() + " but " + staat(state));
            return;
        }
        Direction kijk = state.getValue(MeerpaalBlock.FACING);
        BlockPos ligplaats = MeerpaalBlock.ligplaats(level, anker, kijk);
        meld.check(ligplaats != null && anker.getY() - ligplaats.getY() == 1,
                waar + ": meerpaal " + anker.toShortString() + " faces " + kijk + ", the berth's water is " + (ligplaats == null ? "MISSING" : (anker.getY() - ligplaats.getY()) + " below the deck (1 wanted)"));
        // open water in the berth and ahead of the jetty; deep enough to row away
        int open = 0, kolommen = 0;
        StringBuilder dicht = new StringBuilder();
        for (int zuid = -14; zuid <= 3; zuid++) {
            for (int oost = 1; oost <= 3; oost++) {
                kolommen++;
                BlockPos p = MeerpaalBlock.plek(anker, kijk, oost, -1, zuid);
                boolean vrij = level.getFluidState(p).is(FluidTags.WATER) && level.getBlockState(p.above()).isAir();
                open += vrij ? 1 : 0;
                if (!vrij && dicht.length() < 300) {
                    dicht.append(" [").append(oost).append('/').append(zuid).append(": ").append(staat(level.getBlockState(p))).append(" under ")
                            .append(staat(level.getBlockState(p.above()))).append(']');
                }
            }
        }
        meld.check(open >= kolommen - 3, waar + ": the berth and the way out are open water (" + open + " of " + kolommen + " columns)" + dicht);
        // the jetty's end: over water, and its post stands on the bottom
        BlockPos eind = MeerpaalBlock.plek(anker, kijk, (int) MeerpaalBlock.VISSER[0], 0, (int) MeerpaalBlock.VISSER[2]);
        boolean boven = level.getFluidState(eind.below()).is(FluidTags.WATER);
        BlockPos paal = MeerpaalBlock.plek(anker, kijk, MeerpaalBlock.LANTAARN[0], -1, MeerpaalBlock.LANTAARN[2]);
        int diep = 0;
        while (diep < 12 && level.getBlockState(paal.below(diep)).is(BlockTags.LOGS)) {
            diep++;
        }
        BlockState onder = level.getBlockState(paal.below(diep));
        meld.check(boven && diep > 0 && diep < 12 && !onder.isAir() && onder.getFluidState().isEmpty(),
                waar + ": the jetty's end is over water (" + boven + "), its post goes " + diep + " down and stands on " + staat(onder));
        BlockState lamp = level.getBlockState(MeerpaalBlock.plek(anker, kijk, MeerpaalBlock.LANTAARN[0], MeerpaalBlock.LANTAARN[1], MeerpaalBlock.LANTAARN[2]));
        meld.check(lamp.getBlock() instanceof SteigerlantaarnBlock, waar + ": the lantern stands on the jetty's end (" + staat(lamp) + ")");
        // nothing of the deck is buried: air above the walkway
        int vrij = 0, pad = 0;
        for (int zuid = -9; zuid <= 5; zuid++) {
            for (int oost = -3; oost <= 0; oost++) {
                BlockPos p = MeerpaalBlock.plek(anker, kijk, oost, 1, zuid);
                if (p.equals(anker.above())) {
                    continue;
                }
                pad++;
                vrij += level.getBlockState(p).getCollisionShape(level, p).isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty() ? 1 : 0;
            }
        }
        meld.check(vrij >= pad - 2, waar + ": the walkway is free to walk (" + vrij + " of " + pad + " columns)");
        int nieuw = MeerpaalBlock.zorg(level, anker);
        meld.ok(waar + ": upkeep ran (" + nieuw + " new), boat in the berth: "
                + (ligplaats == null ? "-" : String.valueOf(MeerpaalBlock.liggend(level, ligplaats).size())) + "; look: /guhs bio bouw-meer kijk "
                + (anker.getX() - 14) + " " + (anker.getY() - 9) + " " + (anker.getZ() - 14) + " " + (anker.getX() + 14) + " " + (anker.getY() + 10) + " " + (anker.getZ() + 14) + " steiger");
    }

    private static void bekijkPicknick(ServerLevel level, BlockPos meer, BlockPos bij, BioZelftest.Melder meld) {
        PoolElementStructurePiece stuk = stuk(level, PICKNICKEILANDJE, bij);
        String waar = "picknickeilandje near " + bij.getX() + " " + bij.getZ() + " (lake " + meer.getX() + " " + meer.getZ() + ")";
        if (stuk == null) {
            meld.fout(waar + ": no piece found");
            return;
        }
        BlockPos mand = Terugkeer.wereld(stuk, MAND_LOKAAL);
        laad(level, mand, 24);
        BlockState state = level.getBlockState(mand);
        if (!(state.getBlock() instanceof MandBlock)) {
            meld.fout(waar + ": no mand at " + mand.toShortString() + " but " + staat(state));
            return;
        }
        Direction kijk = state.getValue(MandBlock.FACING);
        // the rug lies on the island: wool with ground under it, no water at its level within two blocks
        int kleed = 0, nat = 0;
        for (int oost = -5; oost <= 3; oost++) {
            for (int zuid = -3; zuid <= 2; zuid++) {
                BlockPos p = MeerpaalBlock.plek(mand, kijk, oost, -1, zuid);
                nat += level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty() ? 0 : 1;
                BlockState tegel = level.getBlockState(p);
                kleed += (tegel.is(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_WOOL) || tegel.is(net.minecraft.world.level.block.Blocks.WHITE_WOOL))
                        && !level.getBlockState(p.below()).isAir() ? 1 : 0;
            }
        }
        meld.check(kleed == 27 && nat == 0, waar + ": mand " + mand.toShortString() + " faces " + kijk + ", the rug has " + kleed + " of 27 tiles on the ground, "
                + nat + " wet columns around it");
        // the island's own tree stands where the picnic looks
        int stam = 0, blad = 0;
        for (BlockPos p : BlockPos.betweenClosed(MeerpaalBlock.plek(mand, kijk, -4, 0, -5), MeerpaalBlock.plek(mand, kijk, 2, 9, -1))) {
            stam += level.getBlockState(p).is(BlockTags.LOGS) ? 1 : 0;
        }
        for (BlockPos p : BlockPos.betweenClosed(mand.offset(-7, 3, -7), mand.offset(7, 12, 7))) {
            blad += level.getBlockState(p).is(BlockTags.LEAVES) ? 1 : 0;
        }
        meld.check(stam >= 3 && blad >= 20, waar + ": a tree stands at the head of the rug (" + stam + " logs there, " + blad + " leaves over the picnic)");
        int lampen = MandBlock.versier(level, mand, kijk);
        int hangend = 0;
        for (BlockPos p : BlockPos.betweenClosed(mand.offset(-8, 2, -8), mand.offset(8, 12, 8))) {
            BlockState s = level.getBlockState(p);
            hangend += s.getBlock() instanceof LanternBlock && s.getValue(LanternBlock.HANGING) && !level.getBlockState(p.above()).isAir() ? 1 : 0;
        }
        meld.check(lampen >= 3 && hangend == lampen, waar + ": " + lampen + " paper lanterns hang in the tree (" + hangend + " from something)");
        meld.ok(waar + ": look: /guhs bio bouw-meer kijk " + (mand.getX() - 12) + " " + (mand.getY() - 4) + " " + (mand.getZ() - 12) + " "
                + (mand.getX() + 12) + " " + (mand.getY() + 14) + " " + (mand.getZ() + 12) + " picknick");
    }

    /** (Tests) forget the remembered surroundings. */
    static void vergeet(MinecraftServer server) {
        BUURT.clear();
    }
}
