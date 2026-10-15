package nl.juiced.guhs.feature.park;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * 1.4.2 (Het Knabbelpark): op commands (dev checks, AutoCheck scripts). Texts are literals (dev only).
 * <ul>
 *   <li>{@code /guhs park keur [straal]}: the walk check ({@link ParkKeur}) of every generated park with a piece within
 *       the radius (loaded chunks); also written to {@code <world>/bouwcheck/knabbelpark_keur.txt};</li>
 *   <li>{@code /guhs park layout [aantal]}: draws park layouts with this level's own generator at that many chunks around
 *       you (no blocks): how many are dry and gentle enough, how many layouts are complete (and after how many attempts),
 *       how many pieces;</li>
 *   <li>{@code /guhs park stap <n>}, {@code /guhs park wis}: your own step of the questline.</li>
 * </ul>
 */
final class ParkCommando {
    private ParkCommando() {
    }

    static void registreer(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("park").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("keur").executes(c -> keur(c, 160))
                        .then(Commands.argument("straal", IntegerArgumentType.integer(16, 2000)).executes(c -> keur(c, IntegerArgumentType.getInteger(c, "straal")))))
                .then(Commands.literal("layout").executes(c -> layout(c, 64))
                        .then(Commands.argument("aantal", IntegerArgumentType.integer(1, 4000)).executes(c -> layout(c, IntegerArgumentType.getInteger(c, "aantal")))))
                .then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, Park.KLAAR)).executes(ParkCommando::stap)))
                .then(Commands.literal("wis").executes(ParkCommando::wis))));
    }

    private static int keur(CommandContext<CommandSourceStack> c, int straal) {
        ServerLevel level = c.getSource().getLevel();
        Structure park = Kopieen.structuur(level, ParkFeature.STRUCTUUR);
        List<String> regels = new ArrayList<>();
        int goed = 0;
        List<StructureStart> starts = park == null ? List.of() : Kopieen.bij(level, park, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()), straal);
        for (StructureStart start : starts) {
            ParkKeur.Uitslag u = ParkKeur.keur(level, start);
            goed += u.goed() ? 1 : 0;
            regels.add("park " + start.getChunkPos() + " (" + start.getBoundingBox().getCenter().toShortString() + "): " + u);
        }
        regels.add(0, "knabbelpark keur: " + starts.size() + " parken, " + goed + " goed");
        for (String r : regels) {
            c.getSource().sendSuccess(() -> Component.literal(r), false);
        }
        schrijf(level, "knabbelpark_keur.txt", regels);
        return goed;
    }

    private static int layout(CommandContext<CommandSourceStack> c, int aantal) {
        ServerLevel level = c.getSource().getLevel();
        Structure s = Kopieen.structuur(level, ParkFeature.STRUCTUUR);
        if (!(s instanceof ParkStructure park)) {
            return 0;
        }
        ChunkPos midden = ChunkPos.containing(net.minecraft.core.BlockPos.containing(c.getSource().getPosition()));
        var gen = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int geschikt = 0, compleet = 0, stukken = 0, laatPoging = 0;
        WorldgenRandom kies = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
        for (int i = 0; i < aantal; i++) {
            ChunkPos chunk = new ChunkPos(midden.x() + kies.nextInt(161) - 80, midden.z() + kies.nextInt(161) - 80);
            if (!park.droogEnGlooiend(gen, random, level, chunk, 1)) {
                continue;
            }
            geschikt++;
            WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
            r.setLargeFeatureSeed(level.getSeed(), chunk.x(), chunk.z());
            Structure.GenerationContext ctx = new Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(), random,
                    level.getStructureManager(), r, level.getSeed(), chunk, level, b -> true);
            Optional<ParkStructure.Layout> l = park.layout(ctx);
            if (l.isPresent()) {
                compleet++;
                stukken += l.get().stub().getPiecesBuilder().build().pieces().size();
                laatPoging = Math.max(laatPoging, l.get().poging());
            }
        }
        String uit = "knabbelpark layout: " + aantal + " chunks, " + geschikt + " droog en glooiend, " + compleet + " compleet"
                + (compleet > 0 ? ", gemiddeld " + stukken / compleet + " stukken, hoogste poging " + laatPoging : "");
        c.getSource().sendSuccess(() -> Component.literal(uit), false);
        schrijf(level, "knabbelpark_layout.txt", List.of(uit));
        return compleet;
    }

    private static int stap(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = IntegerArgumentType.getInteger(c, "n");
        Park.LIJN.zet(p, n);
        c.getSource().sendSuccess(() -> Component.literal("knabbelpark: stap " + n), false);
        return n;
    }

    private static int wis(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Park.LIJN.wis(p);
        c.getSource().sendSuccess(() -> Component.literal("knabbelpark: gewist"), false);
        return 1;
    }

    private static void schrijf(ServerLevel level, String naam, List<String> regels) {
        try {
            java.nio.file.Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("bouwcheck");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.write(dir.resolve(naam), regels);
        } catch (java.io.IOException e) {
            // (only a report)
        }
    }
}
