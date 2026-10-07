package nl.juiced.guhs.feature.snuffelsteiger;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.item.GuhCompassItem;

/**
 * {@code /guhs steiger ...} (ops; for the AutoCheck script and dev checks; the answers are plain literals):
 * <pre>
 * stand                     where the executing player is in the dock's story, and which dock they stand at
 * waar                      the nearest steigerhuisje (what the Superkompas finds)
 * zoek                      would a steigerhuisje start in this chunk, and if not: why not
 * kust [chunks]             the shores around: every chunk of the normal set within so many chunks, counted by outcome
 * scene feest|overtocht|vaart|thuis    play that cutscene at the dock here (no dock: anchored on your own block)
 * ziekbed                   as if the player heard the sickbed's story
 * vaar                      what the captain's "Hijs de zeilen!" does
 * bewoners                  look after the dock here now, and say who is there
 * keur                      inspect the copy here as it stands in the world: markers left over (must be none), the pier's
 *                           planks, the water under it, how high the land stands against the plot's back and sides
 * ga [kade|tuin|binnen|steiger|platform|zee|achter]   to the nearest steigerhuisje; with a spot (when you are at one): to that
 *                           spot of it, looking the right way (for pictures: the same view at every copy, however it is turned)
 * </pre>
 */
final class SteigerCommando {
    private SteigerCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private static int fout(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendFailure(Component.literal(tekst));
        return 0;
    }

    private static KustStructure structuur(ServerLevel level) {
        return Kopieen.structuur(level, Steiger.STRUCTUUR) instanceof KustStructure k ? k : null;
    }

    private static KustStructure.Uitkomst zoek(ServerLevel level, KustStructure k, ChunkPos chunk) {
        NormalNoise noise = level.getChunkSource().randomState().getOrCreateNoise(k.zee().noise());
        return k.zoek(noise, chunk, (x, z) -> level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                level.getChunkSource().randomState()));
    }

    private static int scene(CommandContext<CommandSourceStack> c, Cutscene s) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Steiger.Oord o = Steiger.oord(p.level(), p.blockPosition());
        BlockPos anker = o != null ? o.anker() : p.blockPosition();
        Rotation draai = o != null ? o.draai() : Rotation.NONE;
        return Cutscenes.speel(p, s, anker, draai, null) ? zeg(c, s.id() + (o != null ? " bij de steiger " + anker + " " + draai : " op je eigen blok")) : fout(c, "Je kijkt al naar iets");
    }

    private static int kust(CommandContext<CommandSourceStack> c, int straal) {
        ServerLevel level = c.getSource().getLevel();
        KustStructure k = structuur(level);
        StructureSet set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Steiger.STRUCTUUR));
        if (k == null || set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return fout(c, "Geen structuur of set guhs:" + Steiger.STRUCTUUR);
        }
        ChunkPos hier = ChunkPos.containing(BlockPos.containing(c.getSource().getPosition()));
        long seed = level.getSeed();
        Map<KustStructure.Reden, Integer> telling = new EnumMap<>(KustStructure.Reden.class);
        int s = spread.spacing(), cellen = straal / s + 1, kandidaten = 0;
        StringBuilder goed = new StringBuilder();
        for (int dx = -cellen; dx <= cellen; dx++) {
            for (int dz = -cellen; dz <= cellen; dz++) {
                ChunkPos kandidaat = spread.getPotentialStructureChunk(seed, (Math.floorDiv(hier.x(), s) + dx) * s, (Math.floorDiv(hier.z(), s) + dz) * s);
                if (Math.abs(kandidaat.x() - hier.x()) > straal || Math.abs(kandidaat.z() - hier.z()) > straal) {
                    continue;
                }
                kandidaten++;
                KustStructure.Uitkomst u = zoek(level, k, kandidaat);
                telling.merge(u.reden(), 1, Integer::sum);
                if (u.plek() != null && goed.length() < 400) {
                    goed.append(' ').append(u.plek().anker().toShortString()).append(' ').append(u.plek().zee().getName()).append(';');
                }
            }
        }
        return zeg(c, kandidaten + " kandidaten binnen " + straal + " chunks: " + telling + (goed.length() > 0 ? " | goed:" + goed : ""));
    }

    /** Spots for pictures: {template x, y above the deck, z, yaw, pitch}. */
    private static final Map<String, double[]> PLEKKEN = Map.of(
            "kade", new double[] {10.5, 0, 12.5, 0, 5},
            "tuin", new double[] {17.5, 0, 1.5, 20, 8},
            "binnen", new double[] {9.5, 0, 8.3, 125, 12},
            "steiger", new double[] {10.5, 0, 16.5, 0, 5},
            "platform", new double[] {9.5, 0, 31.5, -100, 12},
            "zee", new double[] {24.5, 5, 36.5, 135, 18},
            "achter", new double[] {-6.5, 6, -6.5, -40, 20});

    private static int ga(CommandContext<CommandSourceStack> c, String plek) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.level();
        Steiger.Oord o = Steiger.oord(level, p.blockPosition());
        if (o == null) {
            BlockPos daar = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id(Steiger.STRUCTUUR)), p.blockPosition());
            if (daar == null) {
                return fout(c, "Geen steigerhuisje gevonden");
            }
            p.teleportTo(level, daar.getX() + 0.5, Steiger.DEK_Y + 14, daar.getZ() + 0.5, java.util.Set.of(), p.getYRot(), 60f, true);
            return zeg(c, "Boven het steigerhuisje bij " + daar.toShortString() + (plek == null ? "" : " (nog een keer voor de plek " + plek + ")"));
        }
        double[] d = PLEKKEN.get(plek == null ? "kade" : plek);
        net.minecraft.world.phys.Vec3 w = o.wereld(d[0], d[1], d[2]);
        p.teleportTo(level, w.x, w.y, w.z, java.util.Set.of(), o.yaw((float) d[3]), (float) d[4], true);
        return zeg(c, "Op " + (plek == null ? "kade" : plek) + " van het steigerhuisje bij " + o.anker().toShortString() + " (" + o.draai() + ")");
    }

    /** What stands in the world at the copy here (a real, generated one): see the class text. */
    private static int keur(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        Steiger.Oord o = Steiger.oord(level, BlockPos.containing(c.getSource().getPosition()));
        if (o == null) {
            return fout(c, "Hier staat geen steigerhuisje");
        }
        int markers = 0, planken = 0, water = 0, muur = 0, hoogste = 0, blokken = 0;
        net.minecraft.world.level.levelgen.structure.BoundingBox doos = o.doos();
        for (BlockPos pos : BlockPos.betweenClosed(doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ())) {
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(pos);
            if (!st.isAir()) {
                blokken++;
            }
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().endsWith("stained_glass")) {
                markers++;
            }
        }
        for (int z = Steiger.PLOT_Z + 1; z <= 30; z++) {
            BlockPos dek = BlockPos.containing(o.wereld(10.5, -0.5, z + 0.5));
            if (level.getBlockState(dek).is(net.minecraft.tags.BlockTags.PLANKS) || level.getBlockState(dek).is(net.minecraft.tags.BlockTags.LOGS)) {
                planken++;
            }
            if (level.getFluidState(dek.below(2)).is(net.minecraft.tags.FluidTags.WATER)) {
                water++;
            }
        }
        // the plot's back row and its two sides: how many blocks of retaining wall stand over the plot
        for (int x = 0; x < Steiger.SX; x++) {
            for (int z = 0; z <= Steiger.PLOT_Z; z++) {
                if (z != 0 && x != 0 && x != Steiger.SX - 1) {
                    continue;
                }
                int hoog = 0;
                for (int y = 1; y <= 9; y++) {
                    BlockPos pos = BlockPos.containing(o.wereld(x + 0.5, y - 0.5, z + 0.5));
                    if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.STONE_BRICKS)) {
                        hoog = y;
                    }
                }
                muur += hoog > 1 ? 1 : 0;
                hoogste = Math.max(hoogste, hoog);
            }
        }
        BlockPos anker = o.anker().below();
        return zeg(c, "steigerhuisje " + o.anker().toShortString() + " " + o.draai() + ": " + blokken + " blokken in zijn doos, markers over=" + markers
                + ", anker=" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(anker).getBlock()).getPath() + ", pier " + planken
                + "/17 planken met " + water + "/17 water eronder, keermuur op " + muur + " plekken (hoogste " + hoogste + ")");
    }

    static void registreer(RegisterCommandsEvent event) {
        var wortel = Commands.literal("steiger").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Steiger.Oord o = Steiger.oord(p.level(), p.blockPosition());
                    return zeg(c, "stap=" + SnuffelFeature.LIJN.stap(p) + " begonnen=" + SnuffelFeature.LIJN.begonnen(p) + " ziekbed="
                            + SnuffelFeature.LIJN.vlag(p, SteigerVerhaal.ZIEKBED) + " gekozen=" + Keuze.heeft(p) + " steiger="
                            + (o == null ? "geen" : o.anker().toShortString() + " " + o.draai() + " erop=" + o.op(p.position())));
                }))
                .then(Commands.literal("waar").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    BlockPos hier = BlockPos.containing(c.getSource().getPosition());
                    BlockPos daar = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id(Steiger.STRUCTUUR)), hier);
                    return daar == null ? fout(c, "Geen steigerhuisje gevonden") : zeg(c, "Steigerhuisje bij " + daar.toShortString() + " ("
                            + (int) Math.sqrt(daar.distSqr(hier)) + " blokken)");
                }))
                .then(Commands.literal("zoek").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    KustStructure k = structuur(level);
                    if (k == null) {
                        return fout(c, "Geen structuur guhs:" + Steiger.STRUCTUUR);
                    }
                    ChunkPos chunk = ChunkPos.containing(BlockPos.containing(c.getSource().getPosition()));
                    KustStructure.Uitkomst u = zoek(level, k, chunk);
                    double n = level.getChunkSource().randomState().getOrCreateNoise(k.zee().noise()).getValue(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
                    return zeg(c, "chunk " + chunk + " zee-ruis " + String.format(java.util.Locale.ROOT, "%.3f", n) + ": " + u.reden()
                            + (u.plek() == null ? "" : " anker " + u.plek().anker().toShortString() + " zee " + u.plek().zee().getName()));
                }))
                .then(Commands.literal("kust").executes(c -> kust(c, 64))
                        .then(Commands.argument("chunks", IntegerArgumentType.integer(8, 512)).executes(c -> kust(c, IntegerArgumentType.getInteger(c, "chunks")))))
                .then(Commands.literal("scene")
                        .then(Commands.literal("feest").executes(c -> scene(c, SteigerScenes.FEEST)))
                        .then(Commands.literal("overtocht").executes(c -> scene(c, SteigerScenes.OVERTOCHT)))
                        .then(Commands.literal("vaart").executes(c -> scene(c, SteigerScenes.VAART)))
                        .then(Commands.literal("thuis").executes(c -> scene(c, SteigerScenes.THUISKOMST))))
                .then(Commands.literal("ga").executes(c -> ga(c, null))
                        .then(Commands.literal("kade").executes(c -> ga(c, "kade")))
                        .then(Commands.literal("tuin").executes(c -> ga(c, "tuin")))
                        .then(Commands.literal("binnen").executes(c -> ga(c, "binnen")))
                        .then(Commands.literal("steiger").executes(c -> ga(c, "steiger")))
                        .then(Commands.literal("platform").executes(c -> ga(c, "platform")))
                        .then(Commands.literal("zee").executes(c -> ga(c, "zee")))
                        .then(Commands.literal("achter").executes(c -> ga(c, "achter"))))
                .then(Commands.literal("keur").executes(SteigerCommando::keur))
                .then(Commands.literal("ziekbed").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SnuffelFeature.LIJN.begin(p);
                    SnuffelFeature.LIJN.zet(p, Math.max(SnuffelFeature.LIJN.stap(p), SnuffelFeature.STAP_UITVAREN));
                    SnuffelFeature.LIJN.vlag(p, SteigerVerhaal.ZIEKBED, true);
                    return zeg(c, "Het ziekbed is gehoord");
                }))
                .then(Commands.literal("vaar").executes(c -> SteigerVerhaal.vaarUit(c.getSource().getPlayerOrException()) ? zeg(c, "Daar gaat de boot")
                        : fout(c, "Dat gaat nu niet")))
                .then(Commands.literal("bewoners").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    BlockPos hier = BlockPos.containing(c.getSource().getPosition());
                    int gemaakt = Bezetting.controleer(level, hier);
                    StringBuilder wie = new StringBuilder();
                    for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(hier).inflate(64),
                            x -> x instanceof SteigerBewoner || x instanceof SteigerBoot)) {
                        wie.append(' ').append(e instanceof SteigerBewoner b ? b.rol() : "boot").append('@').append(e.blockPosition().toShortString()).append(';');
                    }
                    return zeg(c, "gemaakt=" + gemaakt + " aanwezig:" + wie);
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }
}
