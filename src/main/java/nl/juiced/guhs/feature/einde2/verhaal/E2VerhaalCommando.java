package nl.juiced.guhs.feature.einde2.verhaal;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestspel.GeestToegang;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;

/**
 * einde2 phase B (CONTRACT_E2_B 8): the generic dev command of the story (a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 verhaal stand}: every einde2 line with your step and flags;</li>
 *   <li>{@code /guhs einde2 verhaal zet <lijn> <stap>}: {@link E2VerhaalDev#zetTot};</li>
 *   <li>{@code /guhs einde2 verhaal vlag <lijn> <naam> <aan|uit>};</li>
 *   <li>{@code /guhs einde2 verhaal wis <lijn|alles>};</li>
 *   <li>{@code /guhs einde2 verhaal kracht <id>}: uses a power without the key ({@link Krachten#gebruik});</li>
 *   <li>{@code /guhs einde2 verhaal open <structuur|zone>}: what the veil and the Geestenwereld zones answer for you;</li>
 *   <li>{@code /guhs einde2 verhaal verder}: "Verder met het verhaal" as the kaasbol does it ({@link E2Hervat#verder}; no step change);</li>
 *   <li>{@code /guhs einde2 verhaal waar}: your current story line, step and resume location ({@link E2Hervat#waar});</li>
 *   <li>{@code /guhs einde2 verhaal hint [lijn]}: shows the hint of your current step ({@link StapHint#toon}).</li>
 * </ul>
 * Each slice has its own {@code /guhs einde2 <cmd> ga|scene|wis} besides this.
 */
public final class E2VerhaalCommando {
    /** Every line of einde2 (CONTRACT_E2_B 2.1), in table order. */
    public static final List<String> LIJNEN = List.of(E2Verhaal.LUCHT, E2Verhaal.WATER, E2Verhaal.AARDE, E2Verhaal.VUUR, E2Verhaal.FINALE,
            "omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leeuwschildpad", "mika_kok",
            "vetbootje", "geest_witte_lotus", "geest_lantaarns", "geest_hei_bai", "geest_koh", "geest_chakra", "geest_verschijningen",
            "geest_wan", "leegtewalvis", "werktuigguh", "vredesballon");

    private E2VerhaalCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("verhaal")
                        .then(Commands.literal("stand").executes(c -> stand(c.getSource())))
                        .then(Commands.literal("zet").then(lijn().then(Commands.argument("stap", IntegerArgumentType.integer(0))
                                .executes(c -> zet(c.getSource(), StringArgumentType.getString(c, "lijn"), IntegerArgumentType.getInteger(c, "stap"))))))
                        .then(Commands.literal("vlag").then(lijn().then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of(E2Verhaal.V_GELEERD, E2Verhaal.V_GEEST, E2Verhaal.V_ZUKA_TEAM,
                                        E2Verhaal.V_WITTE_LOTUS), b))
                                .then(Commands.literal("aan").executes(c -> vlag(c.getSource(), StringArgumentType.getString(c, "lijn"),
                                        StringArgumentType.getString(c, "naam"), true)))
                                .then(Commands.literal("uit").executes(c -> vlag(c.getSource(), StringArgumentType.getString(c, "lijn"),
                                        StringArgumentType.getString(c, "naam"), false))))))
                        .then(Commands.literal("wis").then(Commands.argument("lijn", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(metAlles(), b))
                                .executes(c -> wis(c.getSource(), StringArgumentType.getString(c, "lijn")))))
                        .then(Commands.literal("kracht").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Krachten.alle().stream().map(Krachten.Kracht::id), b))
                                .executes(c -> kracht(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(Commands.literal("open").then(Commands.argument("waar", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(waar(), b))
                                .executes(c -> open(c.getSource(), StringArgumentType.getString(c, "waar")))))
                        .then(Commands.literal("verder").executes(c -> E2Hervat.verder(c.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("waar").executes(c -> waarNu(c.getSource())))
                        .then(Commands.literal("hint").executes(c -> hint(c.getSource(), null))
                                .then(lijn().executes(c -> hint(c.getSource(), StringArgumentType.getString(c, "lijn"))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> lijn() {
        return Commands.argument("lijn", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(lijnen(), b));
    }

    /** The table's lines and every other line of the group that is registered. */
    static List<String> lijnen() {
        List<String> uit = new ArrayList<>(LIJNEN);
        for (Verhaallijn l : Verhaallijnen.vanGroep(E2Verhaal.GROEP)) {
            if (!uit.contains(l.id())) {
                uit.add(l.id());
            }
        }
        return uit;
    }

    private static List<String> metAlles() {
        List<String> uit = new ArrayList<>(lijnen());
        uit.add("alles");
        return uit;
    }

    private static List<String> waar() {
        List<String> uit = new ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
        uit.addAll(Geestenwereld.ZONES);
        return uit;
    }

    private static int stand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("Guhvatar: may begin " + E2Verhaal.magBeginnen(p) + ", finale done " + E2Verhaal.finaleKlaar(p)
                + ", unlocked " + E2Verhaal.ontgrendeld(p)), false);
        for (String id : lijnen()) {
            Verhaallijn l = E2Verhaal.lijn(id);
            List<String> vlaggen = new ArrayList<>();
            for (String v : List.of(E2Verhaal.V_GELEERD, E2Verhaal.V_GEEST, E2Verhaal.V_ZUKA_TEAM, E2Verhaal.V_WITTE_LOTUS)) {
                if (E2Verhaal.vlag(p, id, v)) {
                    vlaggen.add(v);
                }
            }
            String regel = "  " + id + ": " + (l == null ? "(not registered)" : E2Verhaal.stap(p, id) + "/" + l.stappen()
                    + (E2Verhaal.klaar(p, id) ? " done" : "")) + (vlaggen.isEmpty() ? "" : " " + vlaggen);
            source.sendSuccess(() -> Component.literal(regel), false);
        }
        return 1;
    }

    private static int zet(CommandSourceStack source, String lijn, int stap) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (E2Verhaal.lijn(lijn) == null) {
            source.sendFailure(Component.literal("Line " + lijn + " is not registered"));
            return 0;
        }
        E2VerhaalDev.zetTot(p, lijn, stap);
        source.sendSuccess(() -> Component.literal(lijn + " is now at step " + E2Verhaal.stap(p, lijn)), false);
        return 1;
    }

    private static int vlag(CommandSourceStack source, String lijn, String naam, boolean aan) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        E2VerhaalDev.vlag(p, lijn, naam, aan);
        source.sendSuccess(() -> Component.literal(lijn + " flag " + naam + " = " + E2Verhaal.vlag(p, lijn, naam)), false);
        return 1;
    }

    private static int wis(CommandSourceStack source, String lijn) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        List<String> welke = "alles".equals(lijn) ? lijnen() : List.of(lijn);
        for (String id : welke) {
            E2VerhaalDev.wis(p, id);
        }
        source.sendSuccess(() -> Component.literal("Wiped: " + welke), false);
        return 1;
    }

    private static int kracht(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (Krachten.van(id) == null) {
            source.sendFailure(Component.literal("No power " + id));
            return 0;
        }
        boolean gedaan = Krachten.gebruik(p, id);
        source.sendSuccess(() -> Component.literal("Power " + id + (gedaan ? ": used" : ": not now (not here, or cooling down)")), false);
        return gedaan ? 1 : 0;
    }

    private static int waarNu(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Verhaallijn l = E2Hervat.huidig(p);
        E2Hervat.Plek w = l == null ? null : E2Hervat.waar(p, l);
        String regel = l == null ? "no story line on" : l.id() + " step " + l.stap(p) + " (" + l.sleutel(p) + "), hint "
                + (StapHint.hint(l.id(), l.stap(p)) != null) + ", resume " + (w == null ? "none" : w.soort() + " " + w.dim().identifier()
                + " " + (w.pos() == null ? "-" : (int) w.pos().x + " " + (int) w.pos().y + " " + (int) w.pos().z));
        source.sendSuccess(() -> Component.literal(regel), false);
        return 1;
    }

    private static int hint(CommandSourceStack source, @javax.annotation.Nullable String lijn) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Verhaallijn l = lijn != null ? E2Verhaal.lijn(lijn) : E2Hervat.huidig(p);
        if (l == null || !StapHint.toon(p, l.id(), true)) {
            source.sendFailure(Component.literal("no hint to show" + (l == null ? "" : " for " + l.id() + " step " + l.stap(p))));
            return 0;
        }
        return 1;
    }

    private static int open(CommandSourceStack source, String waar) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (Geestenwereld.ZONES.contains(waar)) {
            source.sendSuccess(() -> Component.literal("Geestenwereld zone " + waar + ": open " + GeestToegang.zoneOpen(p, waar)), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(waar + ": unlocked " + E2Verhaal.ontgrendeld(p, waar) + ", open (with a host) "
                + E2Verhaal.open(p, waar) + ", veil open " + Sluiers.open(p, waar) + ", chapter " + E2Verhaal.lijnVan(waar)), false);
        return 1;
    }
}
