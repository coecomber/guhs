package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal;

import java.util.ArrayList;
import java.util.List;

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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;

/**
 * Het Vadsrijk zone 5, slice T-WOLKWAL (the leftovers of T-Z5WERELD): the Wolkwacht-ridder model ({@link Wolkwacht}), Moeder
 * Wolkwal's moving fin and fluke tips + spouting blowholes ({@link WolkwalBeweging}), the panoramas of the Ooggaarde and the
 * Staartpark ({@link WolkwalUitzicht}); the enterable upper floors of the inn, the bank and four shops are in the map
 * (tools/vadsrijk/zone5/bouw5.py, spots {@code <house>_boven}, {@link #BOVEN}). Called from {@code Z5WereldRegistry} (one marked line).
 * Dev commands: {@code /guhs vadsrijk wolkwal ...}.
 */
public final class WolkwalRegistry {
    /** The houses with an enterable upper floor (spot "<id>_boven" in Z5WereldPlekken). */
    public static final List<String> BOVEN = List.of("herberg_hemelbed", "bank_hoogvads", "winkel_smid", "winkel_bakker", "winkel_wapens",
            "winkel_kruiden");
    private static final List<Z3Figurant> TOON = new ArrayList<>();

    private WolkwalRegistry() {
    }

    public static void register() {
        nl.juiced.guhs.world.TikWacht.post("WolkwalRegistry:43", e -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            WolkwalBeweging.tick(sl);
            WolkwalUitzicht.tick(sl);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            WolkwalBeweging.wis();
            TOON.clear();
        });
        NeoForge.EVENT_BUS.addListener(WolkwalRegistry::commandos);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int naar(CommandSourceStack src, BlockPos b, float yaw, float pitch) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (b == null || sl == null) {
            src.sendFailure(Component.literal("Onbekende plek (draai eerst maak_kaart --zone 5)"));
            return 0;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
        return 1;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("wolkwal");
        // the knights: all six furs in a row in front of you (60 s)
        w.then(dev("ridders").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = (ServerLevel) p.level();
            TOON.removeIf(f -> f.isRemoved());
            Vec3 voor = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 zij = new Vec3(-voor.z, 0, voor.x);
            float yaw = p.getYRot() + 180;
            for (int i = 0; i < Wolkwacht.VACHTEN.size(); i++) {
                Vec3 v = p.position().add(voor.scale(4)).add(zij.scale((i - 2.5) * 1.6));
                Z3Figurant f = Wolkwacht.prop(sl, v, yaw, Wolkwacht.VACHTEN.get(i), i == 5, 1f);
                if (f != null) {
                    f.leven = 20 * 60;
                    TOON.add(f);
                }
            }
            c.getSource().sendSuccess(() -> Component.literal("6 Wolkwacht-ridders (vachten: " + String.join(", ", Wolkwacht.VACHTEN)
                    + "), de laatste zit. Ze verdwijnen na 60 s."), false);
            return 1;
        }));
        // the moving whale
        w.then(dev("beweging").then(Commands.literal("aan").executes(c -> {
            WolkwalBeweging.aan = true;
            return 1;
        })).then(Commands.literal("uit").executes(c -> {
            WolkwalBeweging.aan = false;
            WolkwalBeweging.wis();
            return 1;
        })).executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("wolkwal-beweging: " + (WolkwalBeweging.aan ? "aan" : "uit") + ", "
                    + WolkwalBeweging.aantal() + " bewegende punten in de wereld (max 4, binnen " + WolkwalBeweging.ZICHT + " blokken)"), false);
            return 1;
        }));
        w.then(dev("spuit").executes(c -> {
            WolkwalBeweging.spuit((ServerLevel) c.getSource().getPlayerOrException().level());
            return 1;
        }));
        w.then(dev("kijk").then(Commands.argument("waar", StringArgumentType.word()).suggests((c, b) -> {
            for (String s : List.of("vin", "staart", "spuitgat", "kop")) {
                b.suggest(s);
            }
            return b.buildFuture();
        }).executes(c -> {
            String waar = StringArgumentType.getString(c, "waar");
            return switch (waar) {
                case "vin" -> naar(c.getSource(), BlockPos.containing(2690, 150, -755), 40, 18);
                case "staart" -> naar(c.getSource(), BlockPos.containing(3060, 235, -600), 127, 14);
                case "spuitgat" -> naar(c.getSource(), BlockPos.containing(2490, 235, -560), -37, -10);
                default -> naar(c.getSource(), BlockPos.containing(2380, 225, -560), -66, 14);
            };
        })));
        // the viewpoints
        w.then(dev("uitzicht").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            b.suggest("ooggaarde");
            b.suggest("staartpark");
            b.suggest("reset");
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            if (id.equals("reset")) {
                for (WolkwalUitzicht.Uitzicht u : WolkwalUitzicht.UITZICHTEN) {
                    Spelers.data(p).gezien.remove(WolkwalUitzicht.gezienSleutel(u.id()));
                }
                c.getSource().sendSuccess(() -> Component.literal("Uitzichten vergeten: loop naar de Ooggaarde of het Staartpark."), false);
                return 1;
            }
            WolkwalUitzicht.Uitzicht u = WolkwalUitzicht.van(id);
            if (u == null) {
                c.getSource().sendFailure(Component.literal("ooggaarde, staartpark of reset"));
                return 0;
            }
            WolkwalUitzicht.speel(p, u);
            return 1;
        })));
        // the upper floors
        w.then(dev("boven").then(Commands.argument("huis", StringArgumentType.word()).suggests((c, b) -> {
            BOVEN.forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> naar(c.getSource(), Z5WereldPlekken.plek(StringArgumentType.getString(c, "huis") + "_boven"), 0, 0))));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
