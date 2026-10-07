package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Op commands of the slice sponsguh_wezens (testing, the autocheck; nothing here builds or changes blocks), under
 * {@code /guhs einde2 sponsguh_wezens}:
 * <ul>
 *   <li>{@code geef sponsguh|gerrit}: your own buddy / pet (what the story's reward does), {@code roep sponsguh|gerrit};</li>
 *   <li>{@code mobiel}: a Knabbelburgermobiel of your own in front of you, {@code ruimop}: it goes again;</li>
 *   <li>{@code draag}: David Hasselguh carries you 40 blocks in the direction you look;</li>
 *   <li>{@code animatie <naam>}: the nearest character of the cast plays it, {@code vlag <aan>}: its flag;</li>
 *   <li>{@code cast}: the whole cast in a row in front of you (they stand still), to look at them.</li>
 * </ul>
 */
public final class E2SponsguhCommando {
    private static final String T = "gui.guhs.einde2_sponsguh_wezens.commando.";

    private E2SponsguhCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("sponsguh_wezens")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("geef").then(Commands.argument("wie", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("sponsguh", "gerrit"), b))
                        .executes(c -> maatje(c.getSource(), StringArgumentType.getString(c, "wie"), false))))
                .then(Commands.literal("roep").then(Commands.argument("wie", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("sponsguh", "gerrit"), b))
                        .executes(c -> maatje(c.getSource(), StringArgumentType.getString(c, "wie"), true))))
                .then(Commands.literal("mobiel").executes(c -> mobiel(c.getSource())))
                .then(Commands.literal("ruimop").executes(c -> {
                    Knabbelburgermobiel.ruimOp(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.translatable(T + "ruimop"), false);
                    return 1;
                }))
                .then(Commands.literal("draag").executes(c -> draag(c.getSource())))
                .then(Commands.literal("animatie").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Cast.values()).flatMap(k -> k.animaties().stream())
                                .filter(n -> !n.startsWith("vlag_") && !n.startsWith("soort_")).distinct().sorted(), b))
                        .executes(c -> animatie(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("vlag").then(Commands.argument("aan", BoolArgumentType.bool())
                        .executes(c -> vlag(c.getSource(), BoolArgumentType.getBool(c, "aan")))))
                .then(Commands.literal("cast").executes(c -> cast(c.getSource()))))));
    }

    private static int maatje(CommandSourceStack source, String wie, boolean roep) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        EigenWezen e = wie.equals("gerrit") ? (roep ? Gerrit.roep(p) : Gerrit.geef(p)) : (roep ? Sponsguh.roep(p) : Sponsguh.geef(p));
        if (e == null) {
            source.sendFailure(Component.translatable(T + "geen_maatje"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(T + "maatje", e.getDisplayName()), false);
        return 1;
    }

    private static int mobiel(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Vec3 voor = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(3));
        boolean gelukt = Knabbelburgermobiel.zet(p.level(), voor, p.getYRot(), p) != null;
        if (gelukt) {
            source.sendSuccess(() -> Component.translatable(T + "mobiel"), false);
        }
        return gelukt ? 1 : 0;
    }

    private static int draag(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Vec3 doel = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(40));
        if (!Hasselguh.draag(p, doel)) {
            source.sendFailure(Component.translatable(T + "geen_rit"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(T + "draag"), false);
        return 1;
    }

    private static SponsguhWezen dichtstbij(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        return p.level().getEntitiesOfClass(SponsguhWezen.class, p.getBoundingBox().inflate(12)).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    private static int animatie(CommandSourceStack source, String naam) throws CommandSyntaxException {
        SponsguhWezen wezen = dichtstbij(source);
        if (wezen == null || !wezen.cast().heeft(naam)) {
            source.sendFailure(Component.translatable(T + "geen_animatie", naam));
            return 0;
        }
        wezen.zetAnimatie(naam);
        source.sendSuccess(() -> Component.translatable(T + "animatie", wezen.getDisplayName(), naam), false);
        return 1;
    }

    private static int vlag(CommandSourceStack source, boolean aan) throws CommandSyntaxException {
        SponsguhWezen wezen = dichtstbij(source);
        if (wezen == null || wezen.cast().vlag == null) {
            source.sendFailure(Component.translatable(T + "geen_vlag"));
            return 0;
        }
        if (wezen instanceof JufPufguhEntity juf) {
            juf.setOpgeblazen(aan);
        } else if (wezen instanceof MeneerKrabguhEntity krab) {
            krab.setBevroren(aan);
        } else {
            wezen.zetVlag(aan);
        }
        source.sendSuccess(() -> Component.translatable(T + "vlag", wezen.getDisplayName(), wezen.cast().vlag, String.valueOf(aan)), false);
        return 1;
    }

    /** The whole cast in a row to the right of where you look, facing you, every look of the ones that have more. */
    private static int cast(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel level = p.level();
        Vec3 vooruit = Vec3.directionFromRotation(0, p.getYRot()), opzij = vooruit.yRot(-(float) Math.PI / 2);
        double langs = -8;
        int n = 0;
        for (Cast cast : Cast.values()) {
            for (int soort = 0; soort < Math.max(1, cast.soorten); soort++) {
                SponsguhWezen wezen = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.COMMAND);
                if (wezen == null) {
                    continue;
                }
                langs += cast.breed / 2 + 0.6;
                Vec3 plek = p.position().add(vooruit.scale(cast == Cast.DAVID_HASSELGUH ? 12 : 5)).add(opzij.scale(langs));
                langs += cast.breed / 2 + 0.6;
                wezen.snapTo(plek.x, plek.y, plek.z, p.getYRot() + 180, 0);
                wezen.yBodyRot = wezen.yHeadRot = p.getYRot() + 180;
                wezen.finalizeSpawn(level, level.getCurrentDifficultyAt(wezen.blockPosition()), EntitySpawnReason.COMMAND, null);
                wezen.zetSoort(soort);
                wezen.zetStil(true);
                level.addFreshEntity(wezen);
                n++;
            }
        }
        int aantal = n;
        source.sendSuccess(() -> Component.translatable(T + "cast", aantal), false);
        return n;
    }
}
