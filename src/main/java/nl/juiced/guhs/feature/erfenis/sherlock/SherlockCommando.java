package nl.juiced.guhs.feature.erfenis.sherlock;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.erfenis.ErfenisMomenten;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Kaart;

/**
 * /guhs erfenis sherlock — {@code bureau} (everyone: the Speurboekje of your Detectivebureau). Dev (op):
 * {@code zaak <id>} (everything before it solved, its moment true, and it starts), {@code volgende} (solve the running case
 * at once and start the next when it is ready), {@code reset}, {@code info}, {@code vind} (every card of the running case
 * found), {@code demo} (three threads + the red herring pinned, the board opens), {@code moment <id>} (fire a village moment
 * as the core would), {@code cast} (the whole cast in a row in front of you), {@code rechtbank} (look at the courtroom).
 */
final class SherlockCommando {
    private SherlockCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> dev = Commands.literal("sherlock");
        dev.then(Commands.literal("bureau").executes(c -> {
            Sherlock.openBureau(c.getSource().getPlayerOrException());
            return 1;
        }));
        dev.then(Commands.literal("zaak").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Zaken.ALLE.stream().map(Zaak::id), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Zaak z = Zaken.van(StringArgumentType.getString(c, "id"));
                            if (z == null) {
                                c.getSource().sendFailure(Component.translatable("sherlock.cmd.onbekend"));
                                return 0;
                            }
                            Sherlock.forceer(p, z);
                            return 1;
                        })));
        dev.then(Commands.literal("volgende").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (!Sherlock.losHuidigeOp(p)) {
                c.getSource().sendFailure(Component.translatable("sherlock.cmd.alles_klaar"));
                return 0;
            }
            Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
            Zaak n = v.huidige();
            if (n != null && v.status(n) == Voortgang.Status.KLAAR && !n.finale()) {
                Sherlock.start(p, n.id());
            }
            return 1;
        }));
        dev.then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            Sherlock.reset(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.translatable("sherlock.cmd.reset"), false);
            return 1;
        }));
        dev.then(Commands.literal("info").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
            StringBuilder s = new StringBuilder();
            for (Zaak z : Zaken.ALLE) {
                s.append(z.nr()).append(' ').append(z.id()).append(": ").append(v.status(z)).append('\n');
            }
            s.append("hoeve ").append(v.hoeve).append(", bestellingen ").append(v.bestellingen).append(", gebouwd ").append(v.gebouwd)
                    .append(", actief ").append(v.actief).append(", gevonden ").append(v.gevonden).append(", ronde ").append(v.ronde);
            c.getSource().sendSuccess(() -> Component.literal(s.toString()), false);
            return 1;
        }));
        dev.then(Commands.literal("vind").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
            Zaak z = v.actief == null ? null : Zaken.van(v.actief);
            if (z == null) {
                c.getSource().sendFailure(Component.translatable("sherlock.cmd.geen_zaak"));
                return 0;
            }
            z.kaarten().forEach(k -> v.gevonden.add(k.id()));
            SherlockData.veranderd(p.level().getServer());
            if (z.verzamelZaak()) {
                Sherlock.losOp(p, z);
            }
            return 1;
        }));
        dev.then(Commands.literal("demo").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
            Zaak z = v.actief == null ? null : Zaken.van(v.actief);
            if (z == null || z.dader() == null) {
                c.getSource().sendFailure(Component.translatable("sherlock.cmd.geen_zaak"));
                return 0;
            }
            int n = 0;
            for (Kaart k : z.kaarten()) {
                v.gevonden.add(k.id());
                if (k.goed() && n < 3) {
                    v.draadjes.put(k.id(), z.dader());
                    n++;
                } else if (!k.goed()) {
                    v.draadjes.put(k.id(), k.wijstNaar());
                }
            }
            SherlockData.veranderd(p.level().getServer());
            Sherlock.stuurBord(p, z);
            return 1;
        }));
        dev.then(Commands.literal("moment").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("id", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ErfenisMomenten.melding(p.level().getServer(), p.getUUID(), StringArgumentType.getString(c, "id"));
                    return 1;
                })));
        dev.then(Commands.literal("cast").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Kind[] cast = {Kind.MAKELAAR_GUHRRIT, Kind.DOKTER_VADSON, Kind.SHERLOCK_GUH, Kind.OEHOE, Kind.KNABBELHAMER, Kind.MORRIS_MIKA, Kind.PIM_MIKA,
                    Kind.GUHRRIT_VAKANTIE};
            String[] rollen = {"guhrrit", "vadson", "sherlock", "oehoe", "knabbelhamer", "morris", "pim", "guhrrit"};
            Vec3 kijk = Vec3.directionFromRotation(0, p.getYRot());
            Vec3 zij = new Vec3(-kijk.z, 0, kijk.x);
            Vec3 midden = p.position().add(kijk.scale(5));
            for (int i = 0; i < cast.length; i++) {
                Vec3 pos = Cast.opGrond(p.level(), midden.x + zij.x * (i - 3.5) * 1.6, p.getY(), midden.z + zij.z * (i - 3.5) * 1.6);
                Cast.zet(p.level(), p.getUUID(), "cast:" + i, rollen[i], cast[i], pos, Cast.kijkNaar(pos, p.position()), null, true);
            }
            return 1;
        }));
        dev.then(Commands.literal("rechtbank").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var level = nl.juiced.guhs.feature.erfenis.Erfenis.level(p.level().getServer());
            if (level == null) {
                return 0;
            }
            Rechtbank.bouw(level);
            Rechtbank.zetNpcs(level);
            Rechtbank.naarBinnen(p, level);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("erfenis").then(dev)));
    }
}
