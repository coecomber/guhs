package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.Optional;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * 1.10.3: {@code /guhs vadsrijk patch <naam> [--echt]} (dry run by default) and {@code /guhs vadsrijk patch herstel <bestand>}
 * (ops; works from the console / rcon). See {@link WereldPatch}.
 */
final class PatchCommando {
    private PatchCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> t = Commands.literal("patch").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        t.executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("Patches: " + String.join(", ", WereldPatch.NAMEN)
                    + ". /guhs vadsrijk patch <naam> = proefrun, --echt = plaatsen, herstel <bestand> = terugzetten."), false);
            return WereldPatch.NAMEN.size();
        });
        t.then(Commands.literal("herstel").then(Commands.argument("bestand", StringArgumentType.string())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(WereldPatch.backups(c.getSource().getServer()), b))
                .executes(c -> herstel(c, StringArgumentType.getString(c, "bestand")))));
        for (String naam : WereldPatch.NAMEN) {
            t.then(Commands.literal(naam).executes(c -> patch(c, naam, false))
                    .then(Commands.literal("--echt").executes(c -> patch(c, naam, true))));
        }
        for (var g : WereldPatch.GROEPEN.entrySet()) {
            t.then(Commands.literal(g.getKey()).executes(c -> groep(c, g.getKey(), g.getValue(), false))
                    .then(Commands.literal("--echt").executes(c -> groep(c, g.getKey(), g.getValue(), true))));
        }
        event.getDispatcher().register(VadsrijkCommando.wortel(t));
    }

    private static int patch(CommandContext<CommandSourceStack> c, String naam, boolean echt) {
        CommandSourceStack src = c.getSource();
        if (WereldStempel.isStempel(naam)) {
            return stempel(src, naam, echt);
        }
        Optional<WereldPatch.Patch> op = WereldPatch.laad(naam);
        if (op.isEmpty()) {
            src.sendFailure(Component.literal("Patch " + naam + " is niet te laden (zie de log)."));
            return 0;
        }
        WereldPatch.Patch p = op.get();
        ServerLevel level = src.getServer().getLevel(p.dim());
        if (level == null) {
            src.sendFailure(Component.literal("Dimensie " + p.dim().identifier() + " bestaat niet."));
            return 0;
        }
        WereldPatch.Uitslag u = WereldPatch.toepassen(level, p, echt);
        if (u.fout() != null) {
            src.sendFailure(Component.literal("Patch " + naam + " niet uitgevoerd: " + u.fout()));
            return 0;
        }
        src.sendSuccess(() -> Component.literal((echt ? "Patch " : "Proefrun patch ") + naam + " (" + p.beschrijving() + "), " + p.dim().identifier()
                + ": " + p.dozen().size() + " dozen, " + p.cellen() + " cellen"), false);
        for (WereldPatch.Telling tl : u.tellingen()) {
            WereldPatch.Doos d = tl.doos();
            String blok = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(d.blok().getBlock()).toString();
            src.sendSuccess(() -> Component.literal("  " + d.naam() + " " + d.min().toShortString() + " .. " + d.max().toShortString() + " " + blok
                    + ": " + (echt ? "geplaatst " : "zou plaatsen ") + tl.plaatsen() + ", al goed " + tl.alGoed() + ", bezet (blijft) " + tl.bezet()), false);
        }
        String slot = echt
                ? (u.backup() == null ? "Niets veranderd (alles stond al goed of was bezet)."
                        : u.plaatsen() + " blokken geplaatst. Ongedaan maken: /guhs vadsrijk patch herstel " + u.backup())
                : "Totaal: zou " + u.plaatsen() + " blokken plaatsen (al goed " + u.alGoed() + ", bezet " + u.bezet() + "). Echt doen: /guhs vadsrijk patch "
                        + naam + " --echt";
        src.sendSuccess(() -> Component.literal(slot), echt);
        return Math.max(1, u.plaatsen());
    }

    /** Every member of a group in turn; a member that cannot run (terrain not generated yet) is reported and skipped. */
    private static int groep(CommandContext<CommandSourceStack> c, String groep, java.util.List<String> namen, boolean echt) {
        int totaal = 0, fout = 0;
        for (String naam : namen) {
            int n = patch(c, naam, echt);
            if (n == 0) {
                fout++;
            } else {
                totaal += n;
            }
        }
        int t = totaal, f = fout;
        c.getSource().sendSuccess(() -> Component.literal("Groep " + groep + ": " + namen.size() + " patches, " + (namen.size() - f) + " uitgevoerd, " + f
                + " niet (zie hierboven)." + (echt ? "" : " Echt doen: /guhs vadsrijk patch " + groep + " --echt")), echt);
        return Math.max(1, t);
    }

    /** 1.10.4: a stamp patch runs over several ticks; the counts come when it is done (dry run: scan only). */
    private static int stempel(CommandSourceStack src, String naam, boolean echt) {
        Optional<WereldStempel.Stempel> op = WereldStempel.laad(naam);
        if (op.isEmpty()) {
            src.sendFailure(Component.literal("Stempel " + naam + " is niet te laden (zie de log)."));
            return 0;
        }
        if (WereldStempel.bezig(naam)) {
            src.sendFailure(Component.literal("Stempel " + naam + " loopt al."));
            return 0;
        }
        WereldStempel.Stempel s = op.get();
        ServerLevel level = src.getServer().getLevel(s.dim());
        if (level == null) {
            src.sendFailure(Component.literal("Dimensie " + s.dim().identifier() + " bestaat niet."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal((echt ? "Stempel " : "Proefrun stempel ") + naam + " (" + s.beschrijving() + "), " + s.dim().identifier()
                + ": " + s.cellen() + " cellen, beleid " + s.beleid().name().toLowerCase(java.util.Locale.ROOT) + ". Bezig..."), false);
        WereldStempel.start(new WereldStempel.Klus(level, s, echt, src, WereldStempel.PER_TICK), u -> {
            if (u.fout() != null) {
                src.sendFailure(Component.literal("Stempel " + naam + " niet (helemaal) uitgevoerd: " + u.fout()));
                return;
            }
            String niet = u.nietGegenereerd() == 0 ? "" : ", nog niet gegenereerd " + u.nietGegenereerd() + " (" + u.chunksNietGegenereerd()
                    + " chunks: die krijgen het vanzelf uit de nieuwe tegels)";
            String tel = (echt ? "geplaatst " : "zou plaatsen ") + u.plaatsen() + ", al goed " + u.alGoed() + ", bezet (blijft) " + u.bezet() + niet;
            String slot = echt
                    ? (u.backup() == null ? "Niets veranderd (" + tel + ")."
                            : "Stempel " + naam + " klaar: " + tel + ". Ongedaan maken: /guhs vadsrijk patch herstel " + u.backup())
                    : "Proefrun stempel " + naam + ": " + tel + ". Echt doen: /guhs vadsrijk patch " + naam + " --echt";
            src.sendSuccess(() -> Component.literal(slot), echt);
        });
        return Math.max(1, s.cellen());
    }

    private static int herstel(CommandContext<CommandSourceStack> c, String bestand) {
        if (WereldStempel.backupGrootte(c.getSource().getServer(), bestand) > WereldStempel.PER_TICK) {   // 1.10.4: a stamp's backup: in steps
            String fout = WereldStempel.herstelInStappen(c.getSource().getServer(), bestand, c.getSource());
            if (fout != null) {
                c.getSource().sendFailure(Component.literal("Herstel niet gelukt: " + fout));
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.literal("Herstel uit " + bestand + " loopt (in stappen)..."), false);
            return 1;
        }
        WereldPatch.Herstel h = WereldPatch.herstel(c.getSource().getServer(), bestand);
        if (h.fout() != null) {
            c.getSource().sendFailure(Component.literal("Herstel niet gelukt: " + h.fout()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Hersteld uit " + bestand + ": " + h.teruggezet() + " blokken teruggezet, " + h.overgeslagen()
                + " overgeslagen (stonden niet meer zoals de patch ze zette)."), true);
        return Math.max(1, h.teruggezet());
    }
}
