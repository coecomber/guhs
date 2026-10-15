package nl.juiced.guhs.feature.bio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The dev-server self test of biomes3 ({@code /guhs bio zelftest [naam]}, also from the server console; no player
 * needed): the game test server has no Guhmensie, so everything that needs the real dimension (is the biome in the
 * biome source, does the terrain look as meant, does a structure land) is checked here. Every slice registers checks
 * with {@link #registreer}; the command prints one line per report, "[bio-zelftest] &lt;name&gt;: OK ..." or
 * "[bio-zelftest] &lt;name&gt;: FOUT ...", and a last line "[bio-zelftest] klaar: n OK, m FOUT".
 */
public final class BioZelftest {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** What a check reports. */
    public interface Melder {
        void ok(String tekst);

        void fout(String tekst);

        /** ok when {@code goed}, else fout. */
        default void check(boolean goed, String tekst) {
            if (goed) {
                ok(tekst);
            } else {
                fout(tekst);
            }
        }
    }

    /** One check; level is the Guhmensie (never null here). */
    @FunctionalInterface
    public interface Check {
        void test(MinecraftServer server, ServerLevel level, Melder meld);
    }

    private static final Map<String, Check> CHECKS = new LinkedHashMap<>();

    public static synchronized void registreer(String naam, Check check) {
        CHECKS.put(naam, check);
    }

    /**
     * The nearest spot of this biome within {@code straal} blocks of {@code van} (the biome source is asked, no chunk is
     * made), or null. For checks: find the biome, then load and look at the chunks there.
     */
    @Nullable
    public static BlockPos vind(ServerLevel level, ResourceKey<Biome> biome, BlockPos van, int straal) {
        var p = level.findClosestBiome3d(h -> h.is(biome), van, straal, 32, 64);
        return p == null ? null : p.getFirst();
    }

    /** Runs every check (or only the one with this name); returns the report lines (the last one is the summary). */
    public static List<String> draai(MinecraftServer server, @Nullable String alleen) {
        List<String> uit = new ArrayList<>();
        int[] tel = new int[2];
        ServerLevel level = server.getLevel(ModDimensions.GUHMENSION);
        Map<String, Check> checks;
        synchronized (BioZelftest.class) {
            checks = new LinkedHashMap<>(CHECKS);
        }
        if (level == null) {
            uit.add("[bio-zelftest] kern: FOUT the dimension guhs:guhmension does not exist");
            tel[1]++;
        } else {
            for (Map.Entry<String, Check> c : checks.entrySet()) {
                String naam = c.getKey();
                if (alleen != null && !alleen.equals(naam)) {
                    continue;
                }
                Melder meld = new Melder() {
                    @Override
                    public void ok(String tekst) {
                        uit.add("[bio-zelftest] " + naam + ": OK " + tekst);
                        tel[0]++;
                    }

                    @Override
                    public void fout(String tekst) {
                        uit.add("[bio-zelftest] " + naam + ": FOUT " + tekst);
                        tel[1]++;
                    }
                };
                try {
                    c.getValue().test(server, level, meld);
                } catch (RuntimeException e) {
                    LOGGER.error("biomes3 zelftest: {} threw", naam, e);
                    meld.fout("exception " + e);
                }
            }
        }
        uit.add("[bio-zelftest] klaar: " + tel[0] + " OK, " + tel[1] + " FOUT");
        for (String regel : uit) {
            LOGGER.info(regel);
        }
        return uit;
    }

    /** The kern's own checks: the three biomes exist, and every listed biome is a real biome. */
    static void kern() {
        registreer("kern", (server, level, meld) -> {
            var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
            for (ResourceKey<Biome> b : List.of(Bio.BLOESEMMEERTJE, Bio.KLATERDAL, Bio.WOLKENWEIDE)) {
                meld.check(biomes.containsKey(b), "biome " + b.identifier() + " is registered");
            }
            for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
                for (String b : BiomeLijst.biomes(s.id())) {
                    if (!biomes.containsKey(BiomeLijst.sleutel(b))) {
                        meld.fout("section " + s.id() + " lists " + b + ", which is no biome");
                    }
                }
                meld.ok("section " + s.id() + ": " + BiomeLijst.biomes(s.id()).size() + " biomes" + (s.teaser() ? " (teaser)" : ""));
            }
        });
    }

    /** /guhs bio zelftest [naam] (gamemasters and the console). A slice adds its own under /guhs bio &lt;slice&gt; ... */
    static void commando(RegisterCommandsEvent event) {
        var bio = Commands.literal("bio").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        bio.then(Commands.literal("zelftest").executes(ctx -> {
            List<String> regels = draai(ctx.getSource().getServer(), null);
            regels.forEach(r -> ctx.getSource().sendSuccess(() -> Component.literal(r), false));
            return regels.get(regels.size() - 1).contains(" 0 FOUT") ? 1 : 0;
        }).then(Commands.argument("naam", StringArgumentType.word()).executes(ctx -> {
            List<String> regels = draai(ctx.getSource().getServer(), StringArgumentType.getString(ctx, "naam"));
            regels.forEach(r -> ctx.getSource().sendSuccess(() -> Component.literal(r), false));
            return regels.get(regels.size() - 1).contains(" 0 FOUT") ? 1 : 0;
        })));
        event.getDispatcher().register(Commands.literal("guhs").then(bio));
    }

    private BioZelftest() {
    }
}
