package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * Het Vadsrijk zone 6 "Het Zijdeland", slice U-Z6WERELD: the living parts of the map (tiles from tools/vadsrijk/zone6 via
 * {@code maak_kaart.py --zone 6}, placed by kern's KaartFeature like the other zones):
 * <ul>
 *   <li>areas: {@link Z6WereldPlekken#GEBIEDEN} go into kern {@link Gebieden} (the two cities are towns = safe; levels 60-70 per
 *       area: Rozenkloof 60, De Roze Duinen 60-62, De Palmenkloven 62-64, Saffraanpoort 62, De Zoutvlakte 64-66, Het Ruïnedal 66-68,
 *       De Moederoase 68-70, the tower 70); other zone-6 slices can register their own areas in front via {@link #NA_GEBIEDEN};</li>
 *   <li>the biome "Het Vadsrijk - &lt;gebied&gt;" per area ({@link #biome}, asked first by VadsrijkBiomen);</li>
 *   <li>{@link Z6Bewoners}: townsfolk + the three mounts (bultguh, flying carpet, Pluimguh) at NPCs, stalls and structures;</li>
 *   <li>dev commands {@code /guhs vadsrijk z6wereld plek|huis|gebied|bewoners}.</li>
 * </ul>
 * The barrier between zone 5 and 6 (De Roze Kliffen + de Zijdewind) is in indeling.Barrieres. Registered from Z6WereldRegistry.
 */
public final class Z6Wereld {
    /** Other zone-6 slices (wezens, raid, ...): run after the zone-6 areas are in, so theirs end up in FRONT (voegVooraan). */
    public static final List<Runnable> NA_GEBIEDEN = new CopyOnWriteArrayList<>();

    private Z6Wereld() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> gebieden());
        nl.juiced.guhs.world.TikWacht.post("Z6Wereld:49", e -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl != null && sl.getGameTime() % 20 == 0) {
                Z6Bewoners.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> Z6Bewoners.wis());
        NeoForge.EVENT_BUS.addListener(Z6Wereld::commandos);
    }

    // ------------------------------------------------------------------------------------------------ areas + biomes

    /** The areas as kern areas, the first area first (the last one, the whole zone, at the back). */
    static void gebieden() {
        List<Z6WereldPlekken.Gebied> g = Z6WereldPlekken.GEBIEDEN;
        for (int i = g.size() - 1; i >= 0; i--) {
            Gebieden.voegVooraan(gebied(g.get(i)));
        }
        for (Runnable r : NA_GEBIEDEN) {
            r.run();
        }
    }

    static Gebieden.Gebied gebied(Z6WereldPlekken.Gebied k) {
        return new Gebieden.Gebied(k.id(), k.x1(), k.z1(), k.x2(), k.z2(), k.minLevel(), k.maxLevel(), k.stad());
    }

    /** The zone-6 biome id at block x/z ("zijdeland_rozenkloof", ...), or null outside zone 6 (VadsrijkBiomen asks first). */
    @Nullable
    public static String biome(int x, int z) {
        Z6WereldPlekken.Gebied g = Z6WereldPlekken.gebied(x + 0.5, z + 0.5);
        return g == null ? null : g.biome();
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static int naar(CommandSourceStack src, @Nullable BlockPos b) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (b == null || sl == null) {
            src.sendFailure(Component.literal("Onbekende plek (zie POI_Z6.md)"));
            return 0;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
        return 1;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z6wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z6WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> naar(c.getSource(), Z6WereldPlekken.plek(StringArgumentType.getString(c, "id"))))));
        w.then(dev("huis").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z6WereldPlekken.HUIZEN.forEach(h -> b.suggest(h.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (Z6WereldPlekken.Huis h : Z6WereldPlekken.HUIZEN) {
                if (h.id().equals(id)) {
                    return naar(c.getSource(), h.binnen());
                }
            }
            return naar(c.getSource(), null);
        })));
        w.then(dev("gebied").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z6WereldPlekken.Gebied k = Z6WereldPlekken.gebied(p.getX(), p.getZ());
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
            c.getSource().sendSuccess(() -> Component.literal("gebied " + (k == null ? "-" : k.id() + " (biome vadsrijk_" + k.biome() + ", lv "
                    + k.minLevel() + "-" + k.maxLevel() + (k.stad() ? ", stad" : "") + ")") + ", kern-gebied " + (g == null ? "-" : g.id())), false);
            return 1;
        }));
        w.then(dev("bewoners").then(Commands.literal("aan").executes(c -> {
            Z6Bewoners.aan = true;
            return 1;
        })).then(Commands.literal("uit").executes(c -> {
            Z6Bewoners.aan = false;
            Z6Bewoners.wis();
            return 1;
        })).executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("bewoners: " + Z6Bewoners.aantal() + " in de wereld; " + Z6WereldPlekken.BEWONERS.size()
                    + " figuren + " + Z6WereldPlekken.RIJDIEREN.size() + " rijdieren op de kaart, " + (Z6Bewoners.aan ? "aan" : "uit")), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
