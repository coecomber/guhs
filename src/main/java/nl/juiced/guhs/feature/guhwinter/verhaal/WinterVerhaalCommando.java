package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.Arrays;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhwinter.GuhwinterCommando;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * {@code /guhs winter verhaal ...} (op): see and set a player's story, play any scene, reset, and moderate the prikbord.
 * <ul>
 *   <li>{@code toon [speler]}: the three chapters' steps and flags;</li>
 *   <li>{@code stap <speler> <sint|kerst|oudjaar> <n>}: set a step (n = the number of steps = done, with its reward);</li>
 *   <li>{@code scene <naam> [speler]}: a camera scene, a talk, or {@code wandeling} (the live kerstwandeling);</li>
 *   <li>{@code reset <speler> [sint|kerst|oudjaar]}: start over (forgets the scenes as seen too);</li>
 *   <li>{@code schoen nacht <speler>}: pretend a night passed for the schoentje;</li>
 *   <li>{@code wandeling stop};</li>
 *   <li>{@code kaartjes} (every card) and {@code kaartje wis <speler naam>}.</li>
 * </ul>
 */
final class WinterVerhaalCommando {
    private static final String[] HOOFDSTUKKEN = {"sint", "kerst", "oudjaar"};

    private WinterVerhaalCommando() {
    }

    static void register() {
        GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("verhaal")
                .then(Commands.literal("toon").executes(c -> toon(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("speler", EntityArgument.player()).executes(c -> toon(c, EntityArgument.getPlayer(c, "speler")))))
                .then(Commands.literal("stap").then(Commands.argument("speler", EntityArgument.player())
                        .then(Commands.argument("hoofdstuk", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(HOOFDSTUKKEN, b))
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 6)).executes(WinterVerhaalCommando::stap)))))
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(sceneNamen(), b))
                        .executes(c -> scene(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("speler", EntityArgument.player()).executes(c -> scene(c, EntityArgument.getPlayer(c, "speler"))))))
                .then(Commands.literal("reset").then(Commands.argument("speler", EntityArgument.player()).executes(c -> reset(c, null))
                        .then(Commands.argument("hoofdstuk", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(HOOFDSTUKKEN, b))
                                .executes(c -> reset(c, StringArgumentType.getString(c, "hoofdstuk"))))))
                .then(Commands.literal("schoen").then(Commands.literal("nacht").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                    WinterVerhaal.SINT.teller(p, "schoen_dag", VerhaalStappen.dag(p.level().getServer()) - 1);
                    VerhaalStappen.schoenGevuld(p);
                    c.getSource().sendSuccess(() -> Component.literal("schoentje: a night passed for " + p.getGameProfile().name()), true);
                    return 1;
                }))))
                .then(Commands.literal("wandeling").then(Commands.literal("stop").executes(c -> {
                    boolean was = Wandeling.stop();
                    c.getSource().sendSuccess(() -> Component.literal(was ? "kerstwandeling stopped" : "no kerstwandeling running"), true);
                    return was ? 1 : 0;
                })))
                .then(Commands.literal("kaartjes").executes(c -> {
                    var lijst = Voornemens.lijst(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("prikbord: " + lijst.size() + " card(s)"), false);
                    for (CompoundTag k : lijst) {
                        c.getSource().sendSuccess(() -> Component.literal("  " + k.getStringOr("Naam", "?") + ": " + k.getStringOr("Tekst", "")), false);
                    }
                    return lijst.size();
                }))
                .then(Commands.literal("kaartje").then(Commands.literal("wis").then(Commands.argument("naam", StringArgumentType.word()).executes(c -> {
                    String naam = StringArgumentType.getString(c, "naam");
                    boolean was = Voornemens.wis(c.getSource().getServer(), naam);
                    c.getSource().sendSuccess(() -> Component.literal(was ? "removed the card of " + naam : "no card of " + naam), true);
                    return was ? 1 : 0;
                })))));
    }

    private static String[] sceneNamen() {
        java.util.List<String> namen = new java.util.ArrayList<>(WinterScenes.NAMEN.keySet());
        namen.addAll(WinterGesprek.DEV);
        namen.add("wandeling");
        return namen.toArray(String[]::new);
    }

    private static int toon(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        c.getSource().sendSuccess(() -> Component.literal("Guhwinter story of " + p.getGameProfile().name() + " (phase " + WinterVerhaal.fase(p) + ")")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        for (WinterVerhaal.Hoofdstuk h : WinterVerhaal.Hoofdstuk.values()) {
            Verhaallijn l = h.lijn();
            c.getSource().sendSuccess(() -> Component.literal("  " + h.name().toLowerCase(java.util.Locale.ROOT) + ": step " + l.stap(p) + "/" + h.stappen
                    + " (" + l.sleutel(p) + ")" + (h.open(WinterVerhaal.fase(p)) ? "" : " [not open]") + (l.begonnen(p) ? "" : " [not started]")), false);
        }
        Verhaallijn s = WinterVerhaal.SINT;
        c.getSource().sendSuccess(() -> Component.literal("  pakjes " + VerhaalStappen.pakjesGevonden(p) + "/" + WinterVerhaal.PAKJES_NODIG
                + ", schoen " + (s.vlag(p, "schoen") ? "day " + s.teller(p, "schoen_dag") + " (today " + VerhaalStappen.dag(p.level().getServer()) + ")" : "-")
                + (s.vlag(p, "schoen_vol") ? " filled" : "") + ", voornemens heard " + WinterVerhaal.OUDJAAR.vlag(p, "gehoord")
                + ", wandeling " + (Wandeling.bezig() ? "running" : "-")), false);
        return 1;
    }

    private static int stap(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "speler");
        WinterVerhaal.Hoofdstuk h = WinterVerhaal.Hoofdstuk.van(StringArgumentType.getString(c, "hoofdstuk"));
        if (h == null) {
            c.getSource().sendFailure(Component.literal("hoofdstuk: one of " + Arrays.toString(HOOFDSTUKKEN)));
            return 0;
        }
        int n = Math.min(IntegerArgumentType.getInteger(c, "n"), h.stappen);
        Verhaallijn l = h.lijn();
        if (n < l.stap(p)) {
            // (a step back: forget the line, keep what was rewarded)
            String key = "guhs_" + l.id() + "_e_beloning";
            boolean beloond = nl.juiced.guhs.quest.GuhQuests.saved(p).getBooleanOr(key, false);
            l.wis(p);
            if (beloond) {
                nl.juiced.guhs.quest.GuhQuests.saved(p).putBoolean(key, true);
            }
        }
        l.begin(p);
        l.zet(p, n);
        VerhaalStappen.vergeet(p);
        c.getSource().sendSuccess(() -> Component.literal(h.name().toLowerCase(java.util.Locale.ROOT) + " of " + p.getGameProfile().name() + " is now at step "
                + l.stap(p) + "/" + h.stappen), true);
        return 1;
    }

    private static int scene(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        String naam = StringArgumentType.getString(c, "naam");
        Cutscene s = WinterScenes.NAMEN.get(naam);
        boolean ok;
        if (s != null) {
            Cutscenes.vergeet(p, s.id());
            ok = WinterScenes.speel(p, s, null);
        } else if (naam.equals("wandeling")) {
            ok = Wandeling.start(p);
        } else {
            Runnable r = WinterGesprek.dev(p, naam);
            ok = r != null;
            if (ok) {
                r.run();
            }
        }
        boolean gelukt = ok;
        if (gelukt) {
            c.getSource().sendSuccess(() -> Component.literal("playing " + naam + " for " + p.getGameProfile().name() + " (no step changes)"), false);
        } else {
            c.getSource().sendFailure(Component.literal("could not play " + naam + " (unknown, or the player is busy)"));
        }
        return gelukt ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> c, String hoofdstuk) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "speler");
        for (WinterVerhaal.Hoofdstuk h : WinterVerhaal.Hoofdstuk.values()) {
            if (hoofdstuk == null || h == WinterVerhaal.Hoofdstuk.van(hoofdstuk)) {
                h.lijn().wis(p);
            }
        }
        for (Cutscene s : WinterScenes.NAMEN.values()) {
            Cutscenes.vergeet(p, s.id());
        }
        VerhaalStappen.vergeet(p);
        c.getSource().sendSuccess(() -> Component.literal("winter story of " + p.getGameProfile().name() + " reset" + (hoofdstuk == null ? "" : " (" + hoofdstuk + ")")), true);
        return 1;
    }
}
