package nl.juiced.guhs.feature.snuffeldorp;

import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.snuffel.BewonerEntity;
import nl.juiced.guhs.feature.snuffel.Bewoners;
import nl.juiced.guhs.feature.snuffel.Boom;
import nl.juiced.guhs.feature.snuffel.Daden;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Examen;
import nl.juiced.guhs.feature.snuffel.Geurbronnen;
import nl.juiced.guhs.feature.snuffel.Geuren;
import nl.juiced.guhs.feature.snuffel.Maatjes;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * {@code /guhs snuffeldorp ...} (operators): what a dev check, an AutoCheck script or an admin needs to walk through the
 * first series without playing it for half an hour.
 * <pre>
 * stand                 the step, its text variant, the flags, lessons, lost things, exam of the player
 * ga &lt;waar&gt;             to a named spot (strand, strandpoort, plein, wei, boom, haven, versperring...), a resident's key
 *                       (dokter, trainer, bakker...) or a scent source (dorp_deegroller...) of the island you are on
 * praat &lt;sleutel&gt;       what a right-click on that resident does (it has to be loaded: go there first)
 * antwoord &lt;n&gt;          the answer button n of the talking screen that is open (the captain's "Vaar me maar naar huis" = 1)
 * stap &lt;n&gt;              jump to step n of the questline (2..9) with what a dog has by then (awake, the companion from 6 on)
 * </pre>
 * Sniffing and digging themselves are the kern's: {@code /guhs snuffel houding 1} (nose down) and {@code /guhs snuffel graaf}.
 */
public final class DorpCommando {
    private static final Verhaallijn LIJN = SnuffelFeature.LIJN;

    private DorpCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private static int fout(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendFailure(Component.literal(tekst));
        return 0;
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        StringBuilder klussen = new StringBuilder();
        for (Dorp.Klus k : Dorp.KLUSSEN) {
            klussen.append(' ').append(k.bewoner()).append('=').append(Daden.heeft(p, k.daad()) ? "gedaan" : LIJN.vlag(p, k.gevonden()) ? "gevonden"
                    : LIJN.vlag(p, k.gevraagd()) ? "gevraagd" : "-");
        }
        Examen.Loop loop = Examen.bezig(p);
        return zeg(c, "stap=" + LIJN.stap(p) + " sleutel=" + LIJN.sleutel(p) + " wakker=" + LIJN.vlag(p, Dorp.WAKKER) + " les=" + LIJN.teller(p, Dorp.LES)
                + (LIJN.vlag(p, Dorp.LES_GEVONDEN) ? "(gevonden)" : "") + " maatje=" + Maatjes.heeft(p) + " daden=" + Daden.aantal(p) + " boom=" + Boom.stap(p)
                + " klussen:" + klussen + " examen=" + (loop == null ? LIJN.vlag(p, Dorp.GESLAAGD) ? "geslaagd" : "-" : loop.gevonden() + "/"
                + loop.examen().bronnen().size()) + " bloesem=" + LIJN.vlag(p, Dorp.BLOESEM) + " sjaal=" + LIJN.vlag(p, Dorp.SJAAL) + " geuren="
                + Geuren.aantal(p) + " rang=" + Rang.van(p).nummer() + " klaar=" + LIJN.klaar(p) + " mag_door=" + Wegversperring.mag(p));
    }

    private static int ga(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String waar = StringArgumentType.getString(c, "waar");
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null) {
            return fout(c, "Je bent niet op het eiland (/guhs snuffel ga)");
        }
        Plekken pl = Plekken.van(plaats);
        Vec3 doel = pl == null ? null : pl.midden(plaats, waar.equals(Plekken.EMMER) ? Plekken.PLEIN : waar);
        if (doel == null) {
            for (Eiland.BewonerPlek b : plaats.opzet().bewoners()) {
                if (b.sleutel().equals(waar)) {
                    doel = plaats.wereld(b.plek()).add(0.8, 0, 0.8);
                }
            }
        }
        if (doel == null) {
            for (Eiland.BronPlek b : plaats.opzet().bronnen()) {
                if (b.id().equals(waar)) {
                    doel = Vec3.atBottomCenterOf(plaats.wereld(b.plek()));
                }
            }
        }
        if (doel == null) {
            return fout(c, "Onbekende plek, bewoner of geurbron: " + waar);
        }
        p.teleportTo(plaats.level(), doel.x, doel.y, doel.z, Set.of(), p.getYRot(), p.getXRot(), true);
        return zeg(c, "Bij " + waar + " (" + doel + ")");
    }

    private static int praat(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String sleutel = StringArgumentType.getString(c, "sleutel");
        Eiland.Plaats plaats = Eiland.van(p);
        BewonerEntity npc = plaats == null ? null : Eiland.bewoner(plaats, sleutel);
        Bewoners.Rol rol = Bewoners.rol(sleutel);
        if (npc == null || rol == null) {
            return fout(c, "Die bewoner is hier niet (ga er eerst heen): " + sleutel);
        }
        rol.praat(npc, p);
        return zeg(c, sleutel + " sprak; stap=" + LIJN.stap(p) + " sleutel=" + LIJN.sleutel(p));
    }

    private static int stap(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = IntegerArgumentType.getInteger(c, "n");
        Dorp.wakker(p);
        if (n >= SnuffelFeature.STAP_DADEN) {
            Maatjes.geef(p);
            Geuren.leer(p, Dorp.GEEST);
        }
        if (n >= SnuffelFeature.STAPPEN) {
            Dorp.einde(p);
        } else {
            LIJN.zet(p, n);
        }
        return zeg(c, "stap=" + LIJN.stap(p) + " sleutel=" + LIJN.sleutel(p));
    }

    static void registreer(RegisterCommandsEvent event) {
        var wortel = Commands.literal("snuffeldorp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(DorpCommando::stand))
                .then(Commands.literal("ga").then(Commands.argument("waar", StringArgumentType.word()).suggests((c, b) -> {
                    Plekken.echt().plekken().keySet().forEach(b::suggest);
                    Eiland.opzet().bewoners().forEach(x -> b.suggest(x.sleutel()));
                    Eiland.opzet().bronnen().forEach(x -> b.suggest(x.id()));
                    return b.buildFuture();
                }).executes(DorpCommando::ga)))
                .then(Commands.literal("praat").then(Commands.argument("sleutel", StringArgumentType.word()).suggests((c, b) -> {
                    Eiland.opzet().bewoners().forEach(x -> b.suggest(x.sleutel()));
                    return b.buildFuture();
                }).executes(DorpCommando::praat)))
                .then(Commands.literal("antwoord").then(Commands.argument("n", IntegerArgumentType.integer(-1, 9)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Praat.antwoord(p, null, IntegerArgumentType.getInteger(c, "n"));
                    return 1;
                })))
                .then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(SnuffelFeature.STAP_STRAND, SnuffelFeature.STAPPEN))
                        .executes(DorpCommando::stap)))
                .then(Commands.literal("ruik").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Geurbronnen.Neus neus = Geurbronnen.ruik(p);
                    return zeg(c, neus == null ? "niets in de lucht" : neus.bron().id() + " op " + Math.round(neus.afstand()) + " blokken, sterkte "
                            + neus.sterkte() + (neus.opDePlek() ? " (op de plek)" : ""));
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }
}
