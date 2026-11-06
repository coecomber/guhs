package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * T-OPSLAG dev commands (op 2): {@code /guhs vadsrijk opslag ...}
 * <ul>
 *   <li>{@code knapzak [tab]}, {@code tas}, {@code bank}: open (bank here = as if at a banker);</li>
 *   <li>{@code stand}: what is in your knapzak / bank / bag;</li>
 *   <li>{@code knapzak ontgrendel <bron>}, {@code knapzak reset}, {@code bank tabs <0-4>}: unlocks;</li>
 *   <li>{@code strooi}: drops materials around you (auto-pickup test); {@code materiaal <n>}: n Verbetersteentjes into the bag;</li>
 *   <li>{@code hud buff <status> <sec>}, {@code hud alles}: statuses for the buff row; {@code hud doel}: the nearest mob is your
 *   target; {@code hud giet [sec]}: your cast bar; {@code hud baasgiet [sec]}: a boss cast bar from the nearest mob.</li>
 * </ul>
 */
public final class OpslagCommando {
    private OpslagCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("opslag").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("knapzak").executes(c -> Knapzak.open(speler(c)) ? 1 : 0)
                .then(Commands.argument("tab", IntegerArgumentType.integer(1, Knapzak.MAX_TABS))
                        .executes(c -> Knapzak.open(speler(c), IntegerArgumentType.getInteger(c, "tab") - 1) ? 1 : 0))
                .then(Commands.literal("ontgrendel").then(Commands.argument("bron", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = speler(c);
                    boolean nieuw = Knapzak.ontgrendel(p, StringArgumentType.getString(c, "bron"));
                    zeg(c, "knapzak: " + Knapzak.tabs(p) + " tabs" + (nieuw ? "" : " (die bron telde al)"));
                    return 1;
                })))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = speler(c);
                    OpslagData.Zak z = Knapzak.zak(p);
                    z.gekocht = false;
                    z.bronnen.clear();
                    zeg(c, "knapzak-ontgrendelingen gewist (spullen blijven); nu " + Knapzak.tabs(p) + " tabs");
                    return 1;
                })));
        k.then(Commands.literal("tas").executes(c -> MateriaalMenu.open(speler(c)) ? 1 : 0));
        k.then(Commands.literal("bank").executes(c -> Bank.open(speler(c)) ? 1 : 0)
                .then(Commands.literal("tabs").then(Commands.argument("gekocht", IntegerArgumentType.integer(0, Bank.MAX_TABS - 1)).executes(c -> {
                    ServerPlayer p = speler(c);
                    Opslag.data(p).bankGekocht = IntegerArgumentType.getInteger(c, "gekocht");
                    zeg(c, "bank: " + Bank.tabs(p) + " tabs");
                    return 1;
                }))));
        k.then(Commands.literal("stand").executes(c -> {
            ServerPlayer p = speler(c);
            OpslagData d = Opslag.data(p);
            OpslagData.Zak z = Knapzak.zak(p);
            StringBuilder sb = new StringBuilder("knapzak (karakter " + Opslag.KARAKTER.apply(p) + "): " + Knapzak.tabs(p) + " tabs open, gevuld");
            for (Vak v : z.tabs) {
                sb.append(' ').append(v.gevuld()).append('/').append(Knapzak.VAKKEN);
            }
            sb.append(" | bank: ").append(Bank.tabs(p)).append(" tabs, gevuld");
            for (Vak v : d.bank) {
                sb.append(' ').append(v.gevuld()).append('/').append(Bank.VAKKEN);
            }
            long tot = d.materialen.stream().mapToLong(m -> m.aantal).sum();
            sb.append(" | tas: ").append(d.materialen.size()).append(" soorten, ").append(tot).append(" stuks, auto ").append(d.autoUit ? "UIT" : "aan");
            if (!d.onleesbaar.isEmpty()) {
                sb.append(" | onleesbaar bewaard: ").append(d.onleesbaar.values().stream().mapToInt(java.util.List::size).sum());
            }
            zeg(c, sb.toString());
            return 1;
        }));
        k.then(Commands.literal("materiaal").then(Commands.argument("n", IntegerArgumentType.integer(1, 100000)).executes(c -> {
            ServerPlayer p = speler(c);
            Materiaaltas.stop(Opslag.data(p), new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), IntegerArgumentType.getInteger(c, "n")));
            zeg(c, "tas: " + Materiaaltas.tel(p, EcoSpul.VERBETERSTEENTJE.get()) + " Verbetersteentjes");
            return 1;
        })));
        k.then(Commands.literal("strooi").executes(c -> {
            ServerPlayer p = speler(c);
            ServerLevel sl = (ServerLevel) p.level();
            ItemStack[] l = {new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 5), new ItemStack(EcoSpul.PLUISDRAAD.get(), 3),
                    new ItemStack(EcoSpul.HERROLKNABBEL.get(), 2), Kristallen.stack(Kristallen.SOORTEN.keySet().iterator().next(), 0, 4),
                    Kristallen.stack(Kristallen.SOORTEN.keySet().iterator().next(), 1, 1), new ItemStack(net.minecraft.world.item.Items.BREAD, 2)};
            for (int i = 0; i < l.length; i++) {
                double h = i * Math.PI * 2 / l.length;
                ItemEntity e = new ItemEntity(sl, p.getX() + Math.cos(h) * 3, p.getY() + 0.5, p.getZ() + Math.sin(h) * 3, l[i]);
                e.setPickUpDelay(20);
                sl.addFreshEntity(e);
            }
            zeg(c, "6 stapels rond je gestrooid (5 materialen + brood: brood gaat in je inventaris)");
            return 1;
        }));
        LiteralArgumentBuilder<CommandSourceStack> hud = Commands.literal("hud");
        hud.then(Commands.literal("buff").then(Commands.argument("status", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Status.values()).map(s -> s.name().toLowerCase(Locale.ROOT)), b))
                .then(Commands.argument("sec", IntegerArgumentType.integer(1, 600)).executes(c -> {
                    ServerPlayer p = speler(c);
                    Status s = Status.valueOf(StringArgumentType.getString(c, "status").toUpperCase(Locale.ROOT));
                    Statussen.geef(p, s, IntegerArgumentType.getInteger(c, "sec") * 20, s == Status.KNUFFELSCHILD ? 50 : 0.0001, null);
                    return 1;
                }))));
        hud.then(Commands.literal("alles").executes(c -> {
            ServerPlayer p = speler(c);
            int t = 0;
            for (Status s : Status.values()) {
                if (s == Status.VERDOOFD || s == Status.BRANDEND || s == Status.GESPOREN) {
                    continue;   // (no stun lock / damage on the tester; the others show the row)
                }
                Statussen.geef(p, s, 200 + 100 * t++, s == Status.KNUFFELSCHILD ? 50 : 0.0001, null);
            }
            zeg(c, "alle niet-schadelijke statussen gezet (10-60 s)");
            return 1;
        }));
        hud.then(Commands.literal("doel").executes(c -> {
            ServerPlayer p = speler(c);
            LivingEntity m = dichtsteMob(p);
            if (m == null) {
                zeg(c, "geen Vadsrijk-wezen binnen 32 blokken");
                return 0;
            }
            HudBron.zetDoel(p, m);
            Statussen.geef(m, Status.BRANDEND, 100, 0.0001, null);
            zeg(c, "doelwit: " + m.getName().getString());
            return 1;
        }));
        hud.then(Commands.literal("giet").executes(c -> giet(c, 3))
                .then(Commands.argument("sec", IntegerArgumentType.integer(1, 60)).executes(c -> giet(c, IntegerArgumentType.getInteger(c, "sec")))));
        hud.then(Commands.literal("baasgiet").executes(c -> baasGiet(c, 3))
                .then(Commands.argument("sec", IntegerArgumentType.integer(1, 60)).executes(c -> baasGiet(c, IntegerArgumentType.getInteger(c, "sec")))));
        k.then(hud);
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int giet(CommandContext<CommandSourceStack> c, int sec) throws CommandSyntaxException {
        Gietbalk.speler(speler(c), "vadsrijk.opslag.giet.test", sec * 20, 0x8CC4FF);
        return 1;
    }

    private static int baasGiet(CommandContext<CommandSourceStack> c, int sec) throws CommandSyntaxException {
        ServerPlayer p = speler(c);
        LivingEntity m = dichtsteMob(p);
        Gietbalk.baas(m != null ? m : p, "vadsrijk.opslag.giet.baastest", sec * 20, 0xFF6A6A, true, 64);
        return 1;
    }

    private static LivingEntity dichtsteMob(ServerPlayer p) {
        return p.level().getEntitiesOfClass(VadsrijkMob.class, new AABB(p.blockPosition()).inflate(32), m -> m.isAlive())
                .stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(p))).orElse(null);
    }

    private static ServerPlayer speler(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static void zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }
}
