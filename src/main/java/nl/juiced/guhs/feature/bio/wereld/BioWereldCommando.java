package nl.juiced.guhs.feature.bio.wereld;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 wereld: the dev commands and the self test of the terrain (gamemasters and the console; texts are literals).
 * Run them in the Guhmensie: {@code execute in guhs:guhmension run guhs bio wereld ...}. Every line starts with
 * "[bio-wereld]".
 * <ul>
 *   <li>{@code kolom <x> <z>}: what the terrain model says about a column;</li>
 *   <li>{@code plek <soort> <x> <z> [chunks]}: the nearest chunk (default within 48 chunks) with a structure spot of that
 *       kind ({@link BioPlekken}), and how many chunks within that distance have one;</li>
 *   <li>{@code check <x> <z> <chunks>}: generates the chunks around and compares the blocks with the model: the ground,
 *       the water, water outside its bed ("lek"), and how many falls there are and flow;</li>
 *   <li>{@code kaart <x> <z> <chunks>}: generates the chunks around and writes the top blocks and the model to
 *       {@code bio_kaart_<x>_<z>.txt} in the server directory (tools/features/bio_wereld_kaart.py draws it);</li>
 *   <li>{@code tijd <x> <z> <chunks>}: generates the chunks around and says how long a chunk took;</li>
 *   <li>{@code stroom <x> <z> <chunks>}: keeps the chunks around loaded and ticking, so the falls start to flow;</li>
 *   <li>{@code check|kaart|tijd|stroom bij <name> <chunks>}: the same at the middle of the nearest klaterdal, bloesemmeertje
 *       or wolkenweide from 0 0, or (name = a kind of spot, e.g. waterval) at the nearest such spot from there; the place
 *       is printed first, as "bij &lt;name&gt; = x z";</li>
 *   <li>{@code plektest <soort>}: on a server started with GUHS_BIO_PLEKTEST: finds the nearest test structure of that kind
 *       of spot and checks that its marker stands where the model says.</li>
 * </ul>
 */
public final class BioWereldCommando {
    private static final String[] SOORT = {"buiten", "klaterdal", "bloesemmeertje", "wolkenweide"};

    static void registreer(RegisterCommandsEvent event) {
        var wereld = Commands.literal("wereld").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        wereld.then(Commands.literal("kolom").then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                .executes(c -> zeg(c, List.of(kolom(model(c), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"))))))));
        wereld.then(Commands.literal("plek").then(Commands.argument("soort", StringArgumentType.word())
                .then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes(c -> zeg(c, plek(model(c), StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "x"),
                                IntegerArgumentType.getInteger(c, "z"), 48)))
                        .then(Commands.argument("chunks", IntegerArgumentType.integer(1, 400))
                                .executes(c -> zeg(c, plek(model(c), StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "x"),
                                        IntegerArgumentType.getInteger(c, "z"), IntegerArgumentType.getInteger(c, "chunks")))))))));
        for (String wat : new String[]{"check", "kaart", "tijd", "stroom"}) {
            wereld.then(Commands.literal(wat).then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                    .then(Commands.argument("chunks", IntegerArgumentType.integer(0, 12)).executes(c -> {
                        ServerLevel level = c.getSource().getLevel();
                        int x = IntegerArgumentType.getInteger(c, "x"), z = IntegerArgumentType.getInteger(c, "z"), r = IntegerArgumentType.getInteger(c, "chunks");
                        return zeg(c, switch (wat) {
                            case "check" -> List.of(check(level, x, z, r).tekst());
                            case "kaart" -> List.of(kaart(level, x, z, r));
                            case "stroom" -> List.of(stroom(level, x, z, r));
                            default -> List.of(tijd(level, x, z, r));
                        });
                    })))));
        }
        // the same three at a biome: "bij <biome> <chunks>" finds the nearest one from 0 0 and goes to its middle
        for (String wat : new String[]{"check", "kaart", "tijd", "stroom"}) {
            wereld.then(Commands.literal(wat).then(Commands.literal("bij").then(Commands.argument("biome", StringArgumentType.word())
                    .then(Commands.argument("chunks", IntegerArgumentType.integer(0, 12)).executes(c -> {
                        ServerLevel level = c.getSource().getLevel();
                        BlockPos p = midden(level, StringArgumentType.getString(c, "biome"));
                        if (p == null) {
                            return zeg(c, List.of(wat + ": no such biome or spot within 12000 blocks of 0 0"));
                        }
                        int r = IntegerArgumentType.getInteger(c, "chunks");
                        return zeg(c, List.of("bij " + StringArgumentType.getString(c, "biome") + " = " + p.getX() + " " + p.getZ(), switch (wat) {
                            case "check" -> check(level, p.getX(), p.getZ(), r).tekst();
                            case "kaart" -> kaart(level, p.getX(), p.getZ(), r);
                            case "stroom" -> stroom(level, p.getX(), p.getZ(), r);
                            default -> tijd(level, p.getX(), p.getZ(), r);
                        }));
                    })))));
        }
        wereld.then(Commands.literal("plektest").then(Commands.argument("soort", StringArgumentType.word())
                .executes(c -> zeg(c, List.of(plektest(c.getSource().getLevel(), StringArgumentType.getString(c, "soort")))))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(wereld)));
    }

    private static BioModel model(CommandContext<CommandSourceStack> c) {
        return model(c.getSource().getLevel());
    }

    static BioModel model(ServerLevel level) {
        return BioModel.van(level.getChunkSource().randomState());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, List<String> regels) {
        for (String r : regels) {
            c.getSource().sendSuccess(() -> Component.literal("[bio-wereld] " + r), false);
        }
        return 1;
    }

    static String kolom(BioModel m, int x, int z) {
        Kaart k = m.kaart(x >> 4, z >> 4);
        if (k.leeg) {
            return String.format(Locale.ROOT, "kolom %d %d: buiten (dal %.3f, weide %.3f)", x, z, m.eDal(x, z), m.eWeide(x, z));
        }
        int o = Kaart.index(x, z);
        StringBuilder vl = new StringBuilder();
        String[] namen = {"rivier", "lip", "val", "eiland", "groot"};
        for (int b = 0; b < namen.length; b++) {
            if ((k.vlag[o] & 1 << b) != 0) {
                vl.append(' ').append(namen[b]);
            }
        }
        return String.format(Locale.ROOT, "kolom %d %d: %s meng %.2f hoogte %d water %s terras %d vlag[%s] spans %s (dal %.3f, weide %.3f)", x, z,
                SOORT[k.soort[o]], k.meng[o], k.hoogte[o], k.water[o] == Kaart.GEEN ? "-" : String.valueOf(k.water[o]), k.terras[o], vl.toString().trim(),
                k.spans[o] == null ? "-" : java.util.Arrays.toString(k.spans[o]), m.eDal(x, z), m.eWeide(x, z));
    }

    /** The nearest chunk with a spot of this kind, searching rings of chunks outward; also how many chunks have one. */
    static List<String> plek(BioModel m, String naam, int x, int z, int straal) {
        BioPlekken.Soort soort;
        try {
            soort = BioPlekken.Soort.valueOf(naam.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return List.of("plek: unknown kind " + naam);
        }
        int cx = x >> 4, cz = z >> 4, aantal = 0;
        BioPlekken.Plek eerste = null;
        for (int r = 0; r <= straal; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Optional<BioPlekken.Plek> p = BioPlekken.zoek(m, soort, cx + dx, cz + dz, 24);
                    if (p.isPresent()) {
                        aantal++;
                        if (eerste == null) {
                            eerste = p.get();
                        }
                    }
                }
            }
        }
        if (eerste == null) {
            return List.of("plek " + naam + ": none within " + straal + " chunks of " + x + " " + z);
        }
        return List.of(String.format(Locale.ROOT, "plek %s: %d %d %d kijk %s; %d chunks within %d chunks have one", naam, eerste.x(), eerste.y(), eerste.z(),
                eerste.kijk().getName(), aantal, straal));
    }

    /** What a check found. */
    public record Uitslag(int kolommen, int grond, int water, int lek, int vallen, int stromend, int eilandBlokken, String eerste) {
        public boolean goed() {
            return kolommen > 0 && grond == 0 && water == 0 && lek == 0;
        }

        public String tekst() {
            return "check: " + kolommen + " columns of ours, ground wrong " + grond + ", water wrong " + water + ", water outside its bed " + lek
                    + ", fall columns " + vallen + ", flowing water blocks " + stromend + ", floating island blocks " + eilandBlokken
                    + (eerste.isEmpty() ? "" : " (first: " + eerste + ")");
        }
    }

    /** Generates the chunks around (x, z) and compares their blocks with the terrain model. */
    public static Uitslag check(ServerLevel level, int x, int z, int straal) {
        BioModel m = model(level);
        int kolommen = 0, grond = 0, water = 0, lek = 0, vallen = 0, stromend = 0, eiland = 0;
        String eerste = "";
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int cx = (x >> 4) - straal; cx <= (x >> 4) + straal; cx++) {
            for (int cz = (z >> 4) - straal; cz <= (z >> 4) + straal; cz++) {
                Kaart k = m.kaart(cx, cz);
                if (k.leeg) {
                    continue;
                }
                level.getChunk(cx, cz);
                for (int o = 0; o < 256; o++) {
                    if (k.meng[o] < 1f) {
                        continue;
                    }
                    kolommen++;
                    int px = (cx << 4) + (o & 15), pz = (cz << 4) + (o >> 4), h = k.hoogte[o], w = k.water[o];
                    BlockState onder = level.getBlockState(p.set(px, h, pz));
                    if (onder.isAir() || !onder.getFluidState().isEmpty()) {
                        grond++;
                        eerste = eerste.isEmpty() ? "ground " + px + " " + h + " " + pz + " is " + onder : eerste;
                    }
                    boolean valBuur = false;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        int bx = px + d.getStepX(), bz = pz + d.getStepZ();
                        valBuur |= (m.vlag(bx, bz) & Kaart.VAL) != 0 && m.water(bx, bz) > (w == Kaart.GEEN ? h : w);
                    }
                    if (w != Kaart.GEEN) {
                        for (int y = h + 1; y <= w; y++) {
                            var f = level.getFluidState(p.set(px, y, pz));
                            if (!f.is(Fluids.WATER) || !f.isSource()) {
                                water++;
                                eerste = eerste.isEmpty() ? "water " + px + " " + y + " " + pz + " is " + level.getBlockState(p) : eerste;
                            }
                        }
                        if ((k.vlag[o] & Kaart.VAL) != 0) {
                            vallen++;
                        }
                    }
                    // above the column's own water (or dry ground): only the water of a fall next to it may be here
                    for (int y = (w == Kaart.GEEN ? h : w) + 1; y <= (w == Kaart.GEEN ? h : w) + 16; y++) {
                        var f = level.getFluidState(p.set(px, y, pz));
                        if (!f.isEmpty()) {
                            if (valBuur || k.soort[o] == Kaart.WEIDE && WolkTerrein.water(m, px, y, pz)) { // biomes3 wereld-wolk: island ponds and falls
                                stromend++;
                            } else {
                                lek++;
                                eerste = eerste.isEmpty() ? "leak " + px + " " + y + " " + pz : eerste;
                            }
                        }
                    }
                    int[] sp = k.spans[o];
                    if (sp != null) {
                        for (int s = 0; s < sp.length; s += 2) {
                            for (int y = sp[s]; y <= sp[s + 1]; y++) {
                                if (level.getBlockState(p.set(px, y, pz)).isAir()) {
                                    grond++;
                                    eerste = eerste.isEmpty() ? "island " + px + " " + y + " " + pz + " is air" : eerste;
                                } else {
                                    eiland++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return new Uitslag(kolommen, grond, water, lek, vallen, stromend, eiland, eerste);
    }

    static String tijd(ServerLevel level, int x, int z, int straal) {
        int n = 0, onze = 0;
        BioModel m = model(level);
        long t0 = System.nanoTime();
        for (int cx = (x >> 4) - straal; cx <= (x >> 4) + straal; cx++) {
            for (int cz = (z >> 4) - straal; cz <= (z >> 4) + straal; cz++) {
                level.getChunk(cx, cz);
                n++;
                onze += m.kaart(cx, cz).leeg ? 0 : 1;
            }
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        return String.format(Locale.ROOT, "tijd %d %d: %d chunks (%d of ours) in %.0f ms, %.1f ms per chunk", x, z, n, onze, ms, ms / n);
    }

    /** Keeps the chunks around loaded and ticking (so the water of the falls starts to flow); undo with /forceload remove all. */
    static String stroom(ServerLevel level, int x, int z, int straal) {
        int n = 0;
        for (int cx = (x >> 4) - straal; cx <= (x >> 4) + straal; cx++) {
            for (int cz = (z >> 4) - straal; cz <= (z >> 4) + straal; cz++) {
                n += level.setChunkForced(cx, cz, true) ? 1 : 0;
            }
        }
        return "stroom " + x + " " + z + ": " + n + " chunks now stay loaded and tick";
    }

    static String kaart(ServerLevel level, int x, int z, int straal) {
        BioModel m = model(level);
        Path uit = Path.of("bio_kaart_" + x + "_" + z + ".txt");
        int n = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(uit, StandardCharsets.UTF_8))) {
            w.println("# x z top block floor soort terras vlag water hoogte");
            for (int cx = (x >> 4) - straal; cx <= (x >> 4) + straal; cx++) {
                for (int cz = (z >> 4) - straal; cz <= (z >> 4) + straal; cz++) {
                    var chunk = level.getChunk(cx, cz);
                    Kaart k = m.kaart(cx, cz);
                    for (int o = 0; o < 256; o++) {
                        int px = (cx << 4) + (o & 15), pz = (cz << 4) + (o >> 4);
                        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz), vloer = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz);
                        String blok = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(new BlockPos(px, top, pz)).getBlock()).getPath();
                        w.println(px + " " + pz + " " + top + " " + blok + " " + vloer + " " + (k.leeg ? 0 : k.soort[o]) + " " + (k.leeg ? -1 : k.terras[o]) + " "
                                + (k.leeg ? 0 : k.vlag[o]) + " " + (k.leeg || k.water[o] == Kaart.GEEN ? -1 : k.water[o]) + " " + (k.leeg ? -1 : k.hoogte[o]));
                        n++;
                    }
                }
            }
        } catch (IOException e) {
            return "kaart: " + e;
        }
        return "kaart: " + n + " columns written to " + uit.toAbsolutePath();
    }

    /**
     * The middle of the nearest biome of this name from 0 0: from where the biome source first finds it, uphill on the
     * model's "how far in" value (for the Klaterdal: until the second terrace; else to the top), or null.
     */
    static BlockPos midden(ServerLevel level, String biome) {
        BioPlekken.Soort plek = null;
        for (BioPlekken.Soort srt : BioPlekken.Soort.values()) {
            if (srt.getSerializedName().equals(biome)) {
                plek = srt;
            }
        }
        if (plek != null) {
            // a kind of spot: the nearest one from the middle of its biome
            boolean lucht = plek == BioPlekken.Soort.WEIDE || plek == BioPlekken.Soort.LUCHT || plek == BioPlekken.Soort.ZWEEFEILAND;
            BlockPos van = midden(level, lucht ? "wolkenweide" : biome.startsWith("meer_") ? "bloesemmeertje" : "klaterdal");
            if (van == null) {
                return null;
            }
            BioModel model = model(level);
            for (int r = 0; r <= 60; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                            Optional<BioPlekken.Plek> gev = BioPlekken.zoek(model, plek, (van.getX() >> 4) + dx, (van.getZ() >> 4) + dz, 30);
                            if (gev.isPresent()) {
                                return new BlockPos(gev.get().x(), gev.get().y(), gev.get().z());
                            }
                        }
                    }
                }
            }
            return null;
        }
        ResourceKey<Biome> key = switch (biome) {
            case "klaterdal" -> Bio.KLATERDAL;
            case "bloesemmeertje" -> Bio.BLOESEMMEERTJE;
            case "wolkenweide" -> Bio.WOLKENWEIDE;
            default -> null;
        };
        BlockPos p = key == null ? null : BioZelftest.vind(level, key, new BlockPos(0, 80, 0), 12000);
        if (p == null) {
            return null;
        }
        BioModel m = model(level);
        boolean weide = key == Bio.WOLKENWEIDE;
        int x = p.getX(), z = p.getZ();
        for (int stap = 0; stap < 300; stap++) {
            if (key == Bio.KLATERDAL && m.terras(x, z) >= 0 && m.terras(x, z) <= 1) {
                break;
            }
            double best = weide ? m.eWeide(x, z) : m.eDal(x, z);
            int bx = x, bz = z;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                double v = weide ? m.eWeide(x + d.getStepX() * 8, z + d.getStepZ() * 8) : m.eDal(x + d.getStepX() * 8, z + d.getStepZ() * 8);
                if (v > best) {
                    best = v;
                    bx = x + d.getStepX() * 8;
                    bz = z + d.getStepZ() * 8;
                }
            }
            if (bx == x && bz == z) {
                break;
            }
            x = bx;
            z = bz;
        }
        return new BlockPos(x, 80, z);
    }

    /**
     * Finds the nearest generated TEST structure of a kind of spot (a server started with GUHS_BIO_PLEKTEST), generates
     * its chunk and checks that its marker stands on the spot the model gives for that chunk: the glowing post on the
     * spot, the red block two to the side the spot looks at.
     */
    static String plektest(ServerLevel level, String naam) {
        BioPlekken.Soort soort;
        try {
            soort = BioPlekken.Soort.valueOf(naam.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return "plektest: unknown kind " + naam;
        }
        if (!BioPlekStructure.TEST_AAN) {
            return "plektest: start the server with the environment variable GUHS_BIO_PLEKTEST set";
        }
        boolean lucht = soort == BioPlekken.Soort.WEIDE || soort == BioPlekken.Soort.LUCHT || soort == BioPlekken.Soort.ZWEEFEILAND;
        boolean meer = soort.getSerializedName().startsWith("meer_");
        String biome = lucht ? "wolkenweide" : meer ? "bloesemmeertje" : "klaterdal";
        BlockPos van = midden(level, biome);
        var structures = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder = structures.get(nl.juiced.guhs.Guhs.id(biome + "_plektest_" + soort.getSerializedName()));
        if (van == null || holder.isEmpty() || !(holder.get().value() instanceof BioPlekStructure)) {
            return "plektest " + naam + ": FOUT no biome or no test structure";
        }
        var gevonden = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder.get()), van, 60, false);
        if (gevonden == null) {
            return "plektest " + naam + ": FOUT no structure within 60 chunks of " + van.getX() + " " + van.getZ();
        }
        int cx = gevonden.getFirst().getX() >> 4, cz = gevonden.getFirst().getZ() >> 4;
        BioModel m = model(level);
        Optional<BioPlekken.Plek> plek = BioPlekken.zoek(m, soort, cx, cz, 30);
        if (plek.isEmpty()) {
            return "plektest " + naam + ": FOUT a structure started in chunk " + cx + " " + cz + " but the model has no spot there";
        }
        BioPlekken.Plek p = plek.get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
        String post = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(p.x(), p.y() + 1, p.z())).getBlock()).getPath();
        String anker = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(p.x(), p.y(), p.z())).getBlock()).getPath();
        String rood = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(p.x() + p.kijk().getStepX() * 2, p.y() + 1,
                p.z() + p.kijk().getStepZ() * 2)).getBlock()).getPath();
        boolean goed = post.equals("glowstone") && rood.equals("red_wool") && anker.endsWith("_concrete");
        return "plektest " + naam + ": " + (goed ? "OK" : "FOUT") + " spot " + p.x() + " " + p.y() + " " + p.z() + " kijk " + p.kijk().getName() + ": plate "
                + anker + ", post " + post + ", north marker " + rood + "; " + kolom(m, p.x(), p.z());
    }

    /** The self test "wereld": each biome is found, and around the find the generated blocks agree with the model. */
    static void zelftest() {
        BioZelftest.registreer("wereld", (server, level, meld) -> {
            BioModel m = model(level);
            List<ResourceKey<Biome>> biomes = List.of(Bio.KLATERDAL, Bio.BLOESEMMEERTJE, Bio.WOLKENWEIDE);
            for (ResourceKey<Biome> b : biomes) {
                BlockPos p = BioZelftest.vind(level, b, new BlockPos(0, 80, 0), 12000);
                if (p == null) {
                    meld.fout(b.identifier().getPath() + ": not found within 12000 blocks of 0 0");
                    continue;
                }
                byte soort = m.soort(p.getX(), p.getZ());
                meld.check(soort != Kaart.BUITEN, b.identifier().getPath() + " at " + p.getX() + " " + p.getZ() + ": the model says " + SOORT[soort]);
                Uitslag u = check(level, p.getX(), p.getZ(), 2);
                meld.check(u.goed(), b.identifier().getPath() + " " + u.tekst());
            }
            List<String> plekken = new ArrayList<>();
            BlockPos dal = BioZelftest.vind(level, Bio.KLATERDAL, new BlockPos(0, 80, 0), 12000);
            BlockPos weide = BioZelftest.vind(level, Bio.WOLKENWEIDE, new BlockPos(0, 80, 0), 12000);
            for (BioPlekken.Soort s : BioPlekken.Soort.values()) {
                boolean lucht = s == BioPlekken.Soort.WEIDE || s == BioPlekken.Soort.LUCHT || s == BioPlekken.Soort.ZWEEFEILAND;
                BlockPos van = lucht ? weide : dal;
                if (van != null) {
                    String regel = plek(m, s.getSerializedName(), van.getX(), van.getZ(), 60).get(0);
                    plekken.add(regel);
                    meld.check(!regel.contains(": none"), regel);
                }
            }
            meld.ok("reserved air: " + Luchtruim.aantal() + " structure sets of kind lucht");
        });
    }

    private BioWereldCommando() {
    }
}
