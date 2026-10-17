package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 8.2): {@code /guhs einde2 sponsguh zij ...} (a dev run only, operators):
 * <ul>
 *   <li>{@code zij ga <lijn> <stap>}: the Guhvatar story done, part 1 past the job interview (when it was not), the line wiped
 *       and set to that step (begun), you at the step's spot, its hint shown. {@code <stap>} = the line's step count: done;</li>
 *   <li>{@code zij spel karate}: {@code ga sandy_boomkoepel 1} and onto the mat;</li>
 *   <li>{@code zij scene <naam>}: forget the scene and play it at its anchor ({@code sandy, sandy_raket, guhlander, hakker,
 *       helden}; the ship is put up for {@code guhlander} and {@code hakker});</li>
 *   <li>{@code zij stand}: the three lines, the ship; {@code zij wis}: forgets the three lines.</li>
 * </ul>
 */
final class SpZijCommando {
    static final List<String> SCENES = List.of("sandy", "sandy_raket", "guhlander", "hakker", "helden");

    private SpZijCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("zij")
                .then(Commands.literal("ga").then(Commands.argument("lijn", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpZijLijnen.ALLE, b))
                        .then(Commands.argument("stap", IntegerArgumentType.integer(0, 4))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "lijn"), IntegerArgumentType.getInteger(c, "stap"))))))
                .then(Commands.literal("spel").then(Commands.literal("karate").executes(c -> ga(c.getSource(), SpZijLijnen.SANDY, SpZijLijnen.S_KARATE))))
                .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SCENES, b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("stand").executes(c -> stand(c.getSource())))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    wis(p);
                    c.getSource().sendSuccess(() -> Component.literal("sp_zij: the three side lines forgotten"), false);
                    return 1;
                })));
    }

    /** (dev, tests) forgets the three side lines of this player and what runs for them. */
    static void wis(ServerPlayer p) {
        for (Verhaallijn l : SpZijLijnen.lijnen()) {
            l.wis(p);
        }
        Boomkoepel.vergeet(p.getUUID());
        Guhlander.vergeet(p.getUUID());
        Helden.vergeet(p.getUUID());
        SpZijEigen.vergeet(p);
    }

    /** (dev, tests) the state of a step: the gate open, the job done, the line at that step. */
    static void zetTot(ServerPlayer p, String lijn, int stap) {
        SpVerhaalDev.guhvatarKlaar(p);
        if (!SpVerhaal.receptKlaar(p) && SpVerhaal.stap(p, SpVerhaal.RECEPT) <= SpVerhaal.R_SOLLICITATIE) {
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, SpVerhaal.R_SOLLICITATIE + 1);
        }
        Verhaallijn l = SpZijLijnen.lijn(lijn);
        l.wis(p);
        Boomkoepel.vergeet(p.getUUID());
        Guhlander.vergeet(p.getUUID());
        Helden.vergeet(p.getUUID());
        l.begin(p);
        if (stap > 0) {
            l.zet(p, stap);
        }
    }

    private static int ga(CommandSourceStack src, String lijn, int stap) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!SpZijLijnen.ALLE.contains(lijn)) {
            src.sendFailure(Component.literal("unknown side line " + lijn + " (" + SpZijLijnen.ALLE + ")"));
            return 0;
        }
        zetTot(p, lijn, stap);
        ServerLevel g = SpZij.guheinde(src.getServer());
        Vec3 naar = g == null ? null : startPlek(g, lijn, stap);
        if (naar == null) {
            src.sendFailure(Component.literal("sp_zij: Bikinibodem is not found yet (try /guhs einde2 waar bikinibodem)"));
            return 0;
        }
        p.teleportTo(g, naar.x, naar.y + 0.1, naar.z, Set.of(), p.getYRot(), 0f, true);
        p.resetFallDistance();
        StapHint.later(p, 20, s -> StapHint.toon(s, lijn));
        src.sendSuccess(() -> Component.literal("sp_zij: " + lijn + " at step " + stap), false);
        return 1;
    }

    /** Where {@code ga} puts you for a step: just before its spot. */
    static Vec3 startPlek(ServerLevel g, String lijn, int stap) {
        String marker = switch (lijn) {
            case SpZijLijnen.SANDY -> stap == SpZijLijnen.S_KARATE ? null : stap == SpZijLijnen.S_ONDERDELEN ? "raket_onderdeel_1" : "koepel_sluis";
            case SpZijLijnen.GUHLANDER -> stap == SpZijLijnen.G_LANTAARNS ? "zij_lantaarn_1" : "guhlander_trap";
            default -> stap == SpZijLijnen.Z_RIEM ? "riem" : stap == SpZijLijnen.Z_LUNCH ? "mobiel_start" : null;
        };
        if (lijn.equals(SpZijLijnen.SANDY) && stap == SpZijLijnen.S_KARATE) {
            Vec3 m = Boomkoepel.zoneMidden(g, "karate_mat");
            return m == null ? null : m.add(0, 0, 1.5);
        }
        if (marker == null) {
            Vec3 z = SpZij.voeten(g, "zeemeerman");          // in the room of the rest home, west of the heroes
            return z == null ? null : z.add(-3.5, 0, 1.0);
        }
        Vec3 v = SpZij.voeten(g, marker);
        return v == null ? null : marker.equals("guhlander_trap") ? v.add(0, 0, 3.0) : v.add(1.5, 0, 0);
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel g = SpZij.guheinde(src.getServer());
        Cutscene s = Cutscene.van(SpZijScenes.VOOR + naam);
        String anker = switch (naam) {
            case "sandy" -> "sandy";
            case "sandy_raket" -> "raket";
            case "guhlander" -> "guhlander_trap";
            case "hakker" -> "guhlander_dek";
            case "helden" -> "zeemeerman";
            default -> null;
        };
        BlockPos a = g == null || anker == null ? null : SpZij.plek(g, anker);
        if (s == null || a == null) {
            src.sendFailure(Component.literal("sp_zij: no scene " + naam + " " + SCENES + ", or Bikinibodem not found yet"));
            return 0;
        }
        if (naam.equals("guhlander") || naam.equals("hakker")) {
            Guhlander.SCHIP_VOOR.add(p.getUUID());
            Guhlander.plaats(g);
        }
        Cutscenes.vergeet(p, s.id());
        boolean ok = Cutscenes.speel(p, s, a, Rotation.NONE, null);
        src.sendSuccess(() -> Component.literal("sp_zij: scene " + s.id() + (ok ? "" : " NOT played (watching something?)")), false);
        return ok ? 1 : 0;
    }

    private static int stand(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        StringBuilder sb = new StringBuilder("sp_zij: open=" + SpZij.open(p) + " ship=" + Guhlander.staat + " shipForYou=" + Guhlander.SCHIP_VOOR.contains(p.getUUID()));
        for (Verhaallijn l : SpZijLijnen.lijnen()) {
            sb.append("\n ").append(l.id()).append(": stap ").append(l.stap(p)).append("/").append(l.stappen()).append(" (").append(l.sleutel(p)).append(")")
                    .append(l.begonnen(p) ? " begun" : "").append(l.klaar(p) ? " DONE" : "");
        }
        sb.append("\n karate ").append(SpZijLijnen.SANDY_LIJN.teller(p, "karate")).append("/").append(Boomkoepel.Karate.NODIG)
                .append(", parts ").append(Boomkoepel.gevonden(p)).append("/3, lanterns ").append(Guhlander.gevonden(p)).append("/3");
        String uit = sb.toString();
        src.sendSuccess(() -> Component.literal(uit), false);
        return 1;
    }
}
