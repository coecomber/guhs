package nl.juiced.guhs.feature.bio.wereld;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 wereld, the Wolkenweide polish: the entry of everything that is the Wolkenweide's own (its blocks, sounds and
 * particles: {@link WolkBlokken}), its dev commands and its self test. {@link WereldSlice} calls {@link #register}.
 * <p>
 * Dev commands (in the Guhmensie):
 * <ul>
 *   <li>{@code /guhs bio wereld-wolk model <x> <z> <chunks>}: writes the stacks around a spot (islands, lift columns,
 *       stepping stones, falls) to {@code bio_wolk_model_<x>_<z>.json}, for the offline measuring tool;</li>
 *   <li>{@code /guhs bio wereld-wolk kosten}: what the Wolkenweide cost so far per chunk (model and blocks).</li>
 * </ul>
 */
public final class WolkWereld {
    static void register(IEventBus modBus) {
        WolkBlokken.register(modBus);
        NeoForge.EVENT_BUS.addListener(WolkWereld::commando);
        BioZelftest.registreer("wereld_wolk", (server, level, meld) -> {
            BlockPos p = BioZelftest.vind(level, Bio.WOLKENWEIDE, new BlockPos(0, 80, 0), 12000);
            if (p == null) {
                meld.fout("wolkenweide: not found within 12000 blocks of 0 0");
                return;
            }
            BioModel m = BioWereldCommando.model(level);
            int stapels = 0, los = 0, eilanden = 0, kolommen = 0, stappen = 0, vallen = 0, top = 0;
            for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, p.getX() - 160, p.getZ() - 160, p.getX() + 160, p.getZ() + 160)) {
                stapels += s.los ? 0 : 1;
                los += s.los ? 1 : 0;
                eilanden += s.eilanden.size();
                kolommen += s.kolommen.size();
                stappen += s.stappen.size();
                vallen += s.vallen.size();
                top = Math.max(top, s.hoogste().top);
            }
            meld.check(stapels >= 1 && eilanden >= 6 && kolommen >= 2 && stappen >= 8, "wolkenweide around " + p.getX() + " " + p.getZ() + ": " + stapels
                    + " stacks, " + los + " loose islands and rocks, " + eilanden + " islands in all, " + kolommen + " lift columns, " + stappen
                    + " stepping stones, " + vallen + " falls, highest top y " + top);
            meld.check(WolkBlokken.GRAS.get().defaultBlockState().is(BlockTags.DIRT), "wolkenweide_gras is ground for plants and trees (tag minecraft:dirt)");
            int gras = 0, n = 0;
            for (int dx = -24; dx <= 24; dx += 8) {
                for (int dz = -24; dz <= 24; dz += 8) {
                    int x = p.getX() + dx, z = p.getZ() + dz;
                    if (m.soort(x, z) == Kaart.WEIDE && m.meng(x, z) >= 1f && m.water(x, z) == Kaart.GEEN) {
                        n++;
                        level.getChunk(x >> 4, z >> 4);
                        gras += level.getBlockState(new BlockPos(x, m.hoogte(x, z), z)).is(WolkBlokken.GRAS.get()) ? 1 : 0;
                    }
                }
            }
            meld.check(n > 0 && gras == n, "the meadow's top block is wolkenweide_gras (" + gras + " of " + n + " columns)");
        });
    }

    private static void commando(RegisterCommandsEvent event) {
        var wolk = Commands.literal("wereld-wolk");
        wolk.then(Commands.literal("model").then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                .then(Commands.argument("chunks", IntegerArgumentType.integer(1, 40)).executes(c -> {
                    String r = model(c.getSource().getLevel(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"),
                            IntegerArgumentType.getInteger(c, "chunks"));
                    c.getSource().sendSuccess(() -> Component.literal("[bio-wolk] " + r), false);
                    return 1;
                })))));
        wolk.then(Commands.literal("kosten").executes(c -> {
            String r = kosten();
            c.getSource().sendSuccess(() -> Component.literal("[bio-wolk] " + r), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(wolk)));
    }

    static String kosten() {
        long kv = Math.max(1, WolkVulling.KEER.get()), km = Math.max(1, WolkTerrein.KEER.get());
        return String.format(Locale.ROOT, "kosten: blocks %d chunks, %d us per chunk, %d blocks per chunk; model %d maps, %d us per map", WolkVulling.KEER.get(),
                WolkVulling.TIJD.get() / kv / 1000, WolkVulling.BLOKKEN.get() / kv, WolkTerrein.KEER.get(), WolkTerrein.TIJD.get() / km / 1000);
    }

    static String model(ServerLevel level, int x, int z, int chunks) {
        BioModel m = BioWereldCommando.model(level);
        List<WolkTerrein.Stapel> stapels = WolkTerrein.stapels(m, x - chunks * 16, z - chunks * 16, x + chunks * 16, z + chunks * 16);
        Path uit = Path.of("bio_wolk_model_" + x + "_" + z + ".json");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(uit, StandardCharsets.UTF_8))) {
            w.println("{\"clusters\":[");
            int id = 0;
            for (WolkTerrein.Stapel s : stapels) {
                StringBuilder b = new StringBuilder();
                b.append(id > 0 ? "," : "").append("{\"id\":").append(id++).append(",\"x\":").append(s.x).append(",\"z\":").append(s.z).append(",\"los\":").append(s.los)
                        .append(",\"straal\":").append(s.straal).append(",\"stappen\":").append(s.stappen.size()).append(",\"vallen\":").append(s.vallen.size())
                        .append(",\"plas\":").append(s.plas != null).append(",\"islands\":[");
                for (int i = 0; i < s.eilanden.size(); i++) {
                    WolkTerrein.Eiland e = s.eilanden.get(i);
                    b.append(i > 0 ? "," : "").append("{\"x\":").append(e.x).append(",\"z\":").append(e.z).append(",\"top\":").append(e.top).append(",\"klasse\":")
                            .append(e.klasse).append(",\"vorm\":").append(e.vorm).append(",\"niveau\":").append(e.niveau).append("}");
                }
                b.append("],\"lifts\":[");
                for (int i = 0; i < s.kolommen.size(); i++) {
                    WolkTerrein.Kolom k = s.kolommen.get(i);
                    b.append(i > 0 ? "," : "").append("{\"x\":").append(k.x()).append(",\"z\":").append(k.z()).append(",\"y0\":").append(k.voet()).append(",\"y1\":")
                            .append(k.boven()).append(",\"down\":").append(k.omlaag()).append("}");
                }
                b.append("]}");
                w.println(b);
            }
            w.println("]}");
        } catch (IOException e) {
            return "model: " + e;
        }
        return "model: " + stapels.size() + " stacks written to " + uit.toAbsolutePath();
    }

    private WolkWereld() {
    }
}
