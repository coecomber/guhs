package nl.juiced.guhs.feature.bio.wereld;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 wereld, the Klaterdal: its dev commands and self test (gamemasters and the console; texts are literals; run them
 * in the Guhmensie). Every line starts with "[bio-wereld-dal]".
 * <ul>
 *   <li>{@code /guhs bio wereld-dal vorm <x> <z>}: what the shape rules say about a column (terrace, the distances to the
 *       three edges in blocks, the river's level, the kind of cascade);</li>
 *   <li>{@code /guhs bio wereld-dal tel <x> <z> <chunks>}: generates the chunks around and counts what stands there:
 *       water, falls, moss, trees, bamboo, petals, reeds, natural steps, and how much of the river lies under a tree;</li>
 *   <li>{@code /guhs bio wereld-dal kosten}: what the valley has cost world generation since the last call (the terrain
 *       model's chunk maps, and the blocks and plants per chunk).</li>
 * </ul>
 * Self test "wereld-dal" ({@code /guhs bio zelftest wereld-dal}): around the nearest Klaterdal the valley has its water,
 * rock, moss and trees, every natural step is a stair block, and no tree stands over the river.
 */
public final class DalCommando {
    /** What {@link #tel} found. */
    public record Telling(int kolommen, int water, int vallen, int mos, int tapijt, int bloesem, int esdoorn, int bonsai, int bamboe, int blaadjes,
                          int riet, int bloemen, int treden, int tredenFout, int rivierOnderBoom, int rots) {
        public String tekst() {
            return String.format(Locale.ROOT, "tel: %d columns of the valley; water columns %d (falls %d), rock columns %d, moss %d (+%d carpets), "
                            + "guhbloesem logs %d, esdoorn logs %d, bonsai leaves %d, bamboo blocks %d, petals %d, reeds %d, flowers %d, "
                            + "natural steps %d (not a stair: %d), river columns under a tree %d", kolommen, water, vallen, rots, mos, tapijt, bloesem,
                    esdoorn, bonsai, bamboe, blaadjes, riet, bloemen, treden, tredenFout, rivierOnderBoom);
        }
    }

    static void registreer(RegisterCommandsEvent event) {
        var dal = Commands.literal("wereld-dal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        dal.then(Commands.literal("vorm").then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                .executes(c -> {
                    String r = vorm(BioWereldCommando.model(c.getSource().getLevel()), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"));
                    c.getSource().sendSuccess(() -> Component.literal("[bio-wereld-dal] " + r), false);
                    return 1;
                }))));
        dal.then(Commands.literal("tel").then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                .then(Commands.argument("chunks", IntegerArgumentType.integer(0, 12)).executes(c -> {
                    String r = tel(c.getSource().getLevel(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"),
                            IntegerArgumentType.getInteger(c, "chunks")).tekst();
                    c.getSource().sendSuccess(() -> Component.literal("[bio-wereld-dal] " + r), false);
                    return 1;
                })))));
        dal.then(Commands.literal("kosten").executes(c -> {
            String r = kosten();
            c.getSource().sendSuccess(() -> Component.literal("[bio-wereld-dal] " + r), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(dal)));
    }

    /** What the Klaterdal has cost world generation since the server started (and resets the count). */
    static String kosten() {
        long kaarten = DalTerrein.KAARTEN.sumThenReset(), kaartNs = DalTerrein.KAART_NS.sumThenReset();
        long chunks = DalVulling.CHUNKS.sumThenReset(), chunkNs = DalVulling.CHUNK_NS.sumThenReset();
        return String.format(Locale.ROOT, "kosten: the terrain model worked out %d chunk maps in %.0f ms (%.3f ms each); blocks and plants for %d chunks "
                        + "took %.0f ms (%.3f ms each); per generated chunk of the valley that is %.1f maps and %.2f ms in all", kaarten, kaartNs / 1e6,
                kaarten == 0 ? 0 : kaartNs / 1e6 / kaarten, chunks, chunkNs / 1e6, chunks == 0 ? 0 : chunkNs / 1e6 / chunks,
                chunks == 0 ? 0 : kaarten / (double) chunks, chunks == 0 ? 0 : (kaartNs + chunkNs) / 1e6 / chunks);
    }

    static String vorm(BioModel m, int x, int z) {
        DalTerrein.Vorm v = DalTerrein.bij(m, x, z);
        if (!v.kern) {
            return String.format(Locale.ROOT, "vorm %d %d: not on the terraces (e %.4f)", x, z, m.eDal(x, z));
        }
        return String.format(Locale.ROOT, "vorm %d %d: terrace %d, steepness %.5f (a roomy terrace would be %.0f wide), edges at %.4f / %.4f / %.4f, "
                        + "blocks past them %.1f / %.1f / %.1f, the river's level here %d (dropped %d), nearest edge: %s%s, river width x %.2f; "
                        + "model: height %d water %s flags %d", x, z, v.t, v.g, DalTerrein.TRAP_E / v.g, v.t0, v.t1, v.t2, v.s[0], v.s[1], v.s[2], v.peil, v.val,
                v.hoog ? "the tall waterfall" : v.echt ? "a cascade with a fall at its foot" : "a gentle cascade", "", v.bron, m.hoogte(x, z),
                m.water(x, z) == Kaart.GEEN ? "-" : String.valueOf(m.water(x, z)), m.vlag(x, z));
    }

    /** Generates the chunks around (x, z) and counts what the Klaterdal put there. */
    public static Telling tel(ServerLevel level, int x, int z, int straal) {
        BioModel m = BioWereldCommando.model(level);
        int kolommen = 0, water = 0, vallen = 0, mos = 0, tapijt = 0, bloesem = 0, esdoorn = 0, bonsai = 0, bamboe = 0, blaadjes = 0, riet = 0, bloemen = 0,
                treden = 0, tredenFout = 0, onderBoom = 0, rots = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int cx = (x >> 4) - straal; cx <= (x >> 4) + straal; cx++) {
            for (int cz = (z >> 4) - straal; cz <= (z >> 4) + straal; cz++) {
                Kaart k = m.kaart(cx, cz);
                if (k.leeg) {
                    continue;
                }
                level.getChunk(cx, cz);
                for (int o = 0; o < 256; o++) {
                    if (k.terras[o] < 0 || k.meng[o] < 1f) {
                        continue;
                    }
                    kolommen++;
                    int px = (cx << 4) + (o & 15), pz = (cz << 4) + (o >> 4), h = k.hoogte[o];
                    boolean nat = k.water[o] != Kaart.GEEN;
                    water += nat ? 1 : 0;
                    vallen += nat && (k.vlag[o] & Kaart.VAL) != 0 ? 1 : 0;
                    rots += !nat && (k.vlag[o] & (DalTerrein.VORM | Kaart.LIP)) != 0 ? 1 : 0;
                    if (!nat && (k.vlag[o] & DalTerrein.TREDE) != 0) {
                        treden++;
                        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.set(px, h, pz)).getBlock()).getPath();
                        // (the top and the foot of a flight may be a full block)
                        tredenFout += id.endsWith("_trap") || id.endsWith("_stairs") || id.equals("gladde_knuffelsteen") ? 0 : 1;
                    }
                    boolean boom = false;
                    for (int y = (nat ? k.water[o] : h); y <= h + 24; y++) {
                        BlockState s = level.getBlockState(p.set(px, y, pz));
                        if (s.isAir() || s.getFluidState().is(FluidTags.WATER)) {
                            continue;
                        }
                        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                        switch (id) {
                            case "klaterdal_mos" -> mos++;
                            case "klaterdal_mos_tapijt" -> tapijt++;
                            case "guhbloesem_log" -> bloesem++;
                            case "esdoorn_stam" -> esdoorn++;
                            case "klaterdal_bonsaiblad" -> bonsai++;
                            case "guh_bamboe" -> bamboe++;
                            case "drijvende_bloesemblaadjes" -> blaadjes++;
                            case "klaterdal_riet" -> riet++;
                            case "roze_guhbloem", "knabbelroos", "roze_hibiscus", "guhoortjes" -> bloemen++;
                            default -> {
                            }
                        }
                        boom |= y > h + 2 && (id.endsWith("_leaves") || id.contains("bladeren") || id.endsWith("_log") || id.endsWith("_stam"));
                    }
                    onderBoom += nat && boom ? 1 : 0;
                }
            }
        }
        return new Telling(kolommen, water, vallen, mos, tapijt, bloesem, esdoorn, bonsai, bamboe, blaadjes, riet, bloemen, treden, tredenFout, onderBoom, rots);
    }

    static void zelftest() {
        BioZelftest.registreer("wereld-dal", (server, level, meld) -> {
            BlockPos p = BioWereldCommando.midden(level, "klaterdal");
            if (p == null) {
                meld.fout("no Klaterdal within 12000 blocks of 0 0");
                return;
            }
            Telling t = tel(level, p.getX(), p.getZ(), 6);
            meld.check(t.kolommen() > 3000 && t.water() > 50, "the valley at " + p.getX() + " " + p.getZ() + ": " + t.tekst());
            meld.check(t.mos() > 100, "soft moss on the ground: " + t.mos() + " blocks, " + t.tapijt() + " carpets on rock");
            meld.check(t.bloesem() > 0, "guhbloesem trees: " + t.bloesem() + " logs; esdoorn " + t.esdoorn() + ", bonsai leaves " + t.bonsai()
                    + ", bamboo " + t.bamboe());
            meld.check(t.tredenFout() == 0, "every natural step is rock or a stair: " + t.treden() + " steps, " + t.tredenFout() + " wrong");
            meld.check(t.rivierOnderBoom() * 50 <= t.water(), "the river is not hidden under trees: " + t.rivierOnderBoom() + " of " + t.water()
                    + " water columns have a crown above them");
            meld.check(Bio.heeftBlok("klaterdal_mos") && Bio.heeftBlok("klaterdal_riet") && Bio.heeftBlok("klaterdal_bonsaiblad"),
                    "the valley's own blocks are registered");
        });
    }

    private DalCommando() {
    }
}
