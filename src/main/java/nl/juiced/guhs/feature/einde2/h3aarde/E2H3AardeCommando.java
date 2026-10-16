package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * The dev command of chapter 3 (CONTRACT_E2_B 8; a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 h3 ga <stap>}: puts you at any step (a name of {@link E2H3Aarde#STAP_NAMEN} or its number):
 *       every earlier chapter done, this line at that step, the flags and Appa as they would be, and a teleport to where
 *       that step starts (in the Guheinde, at the city's guaranteed copy); what the step starts then starts by itself;</li>
 *   <li>{@code /guhs einde2 h3 scene <naam>}: forgets and plays a scene of the chapter ({@link H3Scenes#ALLE});</li>
 *   <li>{@code /guhs einde2 h3 wis}: forgets the whole chapter for you;</li>
 *   <li>{@code /guhs einde2 h3 paisho}: a game of Pai Sho with Miroh (at any step; the end only counts at step pai_sho).</li>
 * </ul>
 */
public final class E2H3AardeCommando {
    /** Where each step puts you (a marker of the sidecar). */
    static final String[] START = {"aankomst_speler", "rustpunt_1", "rustpunt_2", "arena_speler", "station_buiten", "dai_li_3", "pai_sho", "pai_sho",
            "joo_dee", "aankomst_speler", "aardekoning", "rustpunt_3", "aankomst_speler", "rustpunt_4", "aankomst_speler", "aankomst_speler"};

    private E2H3AardeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("h3")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(E2H3Aarde.STAP_NAMEN, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(H3Scenes.ALLE.keySet(), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource())))
                        .then(Commands.literal("paisho").executes(c -> {
                            PaiShoSpel.metMiroh(c.getSource().getPlayerOrException());
                            return 1;
                        })))));
    }

    static int stapVan(String s) {
        int i = E2H3Aarde.STAP_NAMEN.indexOf(s);
        if (i >= 0) {
            return i;
        }
        try {
            return Math.max(0, Math.min(E2H3Aarde.STAPPEN, Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int ga(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int stap = stapVan(naam);
        if (stap < 0) {
            src.sendFailure(Component.literal("Onbekende stap " + naam + ": " + E2H3Aarde.STAP_NAMEN));
            return 0;
        }
        zet(p, stap);
        if (!Appa.heeft(p)) {
            Appa.geef(p);
        }
        if (!Momo.heeft(p)) {
            Momo.geef(p);
        }
        Appa.houdWeg(p, stap > E2H3Aarde.KAMP && stap <= E2H3Aarde.LAOGAI);
        ServerLevel guheinde = E2H3Aarde.guheinde(p);
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(guheinde);
        Vec3 waar = stad == null ? null : stad.voet(START[stap]);
        if (waar == null) {
            src.sendFailure(Component.literal("De plek van Ba Sing Guh is (nog) niet bekend; de stap is wel gezet."));
            return 1;
        }
        p.teleportTo(guheinde, waar.x, waar.y, waar.z, Set.of(), p.getYRot(), p.getXRot(), false);
        src.sendSuccess(() -> Component.literal("h3: stap " + stap + " (" + E2H3Aarde.STAP_NAMEN.get(stap) + "), bij " + START[stap]), false);
        return 1;
    }

    /** The state of step {@code stap} for this player (also the game tests). */
    static void zet(ServerPlayer p, int stap) {
        wisAlles(p);
        E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, stap);
        if (stap > E2H3Aarde.BLINDDOEK) {
            E2VerhaalDev.vlag(p, E2Verhaal.AARDE, E2Verhaal.V_GELEERD, true);
        }
        if (stap > E2H3Aarde.LAOGAI) {
            E2VerhaalDev.vlag(p, E2Verhaal.AARDE, H3Schijn.V_TRALIES, true);
        }
        Appa.houdWeg(p, stap > E2H3Aarde.KAMP && stap <= E2H3Aarde.LAOGAI);
    }

    private static void wisAlles(ServerPlayer p) {
        E2VerhaalDev.wis(p, E2Verhaal.AARDE);
        for (H3Scenes.Scene s : H3Scenes.ALLE.values()) {
            Cutscenes.vergeet(p, s.id());
        }
        Verteller.vergeet(p, E2H3Aarde.KAART);
        PaiShoSpel.vergeet(p);
        H3Verloop.vergeet(p);
        H3Hints.vergeet(p);
        H3Acteurs.wegAlle(p, "");
        H3Monorail.uit(p);
        H3Blinddoek.stop(p);
        Rustpunten.wis(p, H3Sluipen.SOORT);
        Appa.houdWeg(p, false);
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        H3Scenes.Scene s = H3Scenes.ALLE.get(naam);
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(E2H3Aarde.guheinde(p));
        if (s == null || stad == null) {
            src.sendFailure(Component.literal(s == null ? "Onbekende scene " + naam + ": " + H3Scenes.ALLE.keySet() : "Ba Sing Guh is (nog) niet bekend"));
            return 0;
        }
        Cutscenes.vergeet(p, s.id());
        return H3Scenes.speel(p, stad, s, null) ? 1 : 0;
    }

    private static int wis(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        wisAlles(p);
        src.sendSuccess(() -> Component.literal("h3: hoofdstuk 3 vergeten"), false);
        return 1;
    }
}
