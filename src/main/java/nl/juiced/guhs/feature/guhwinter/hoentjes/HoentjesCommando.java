package nl.juiced.guhs.feature.guhwinter.hoentjes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhwinter.GuhwinterCommando;

/**
 * Op commands (level 2), added to kern's /guhs winter tree with {@link GuhwinterCommando#voegToe}:
 * <ul>
 *   <li>{@code /guhs winter hoentje scene <waterhoen|alpinehoen>}: one crosses in front of you, from 6 blocks to your left
 *       to 6 blocks to your right, 4 blocks ahead (exactly what the kerstwandeling does, {@link GuhwinterHoentjes#spawnScene});</li>
 *   <li>{@code /guhs winter hoentje zoek}: one natural spawn attempt around you right now (no dice, no cool-down);</li>
 *   <li>{@code /guhs winter hoentje noway}: forget your NOWAY and spawn cool-downs (to see the chat line again).</li>
 * </ul>
 * Spawn eggs are in the creative tab.
 */
public final class HoentjesCommando {
    private HoentjesCommando() {
    }

    /** (GuhwinterHoentjes.register) under kern's {@code /guhs winter}. */
    static void register() {
        GuhwinterCommando.voegToe(ctx -> GuhwinterCommando.dev("hoentje")
                .then(Commands.literal("scene").then(Commands.argument("soort", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {GuhwinterHoentjes.WATERHOEN, GuhwinterHoentjes.ALPINEHOEN}, b))
                        .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "soort")))))
                .then(Commands.literal("zoek").executes(c -> zoek(c.getSource())))
                .then(Commands.literal("noway").executes(c -> noway(c.getSource()))));
    }

    private static int scene(CommandSourceStack src, String soort) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Vec3 voor = Vec3.directionFromRotation(0, p.getYRot());
        Vec3 links = new Vec3(voor.z, 0, -voor.x);
        Vec3 midden = p.position().add(voor.scale(4));
        Hoentje h = GuhwinterHoentjes.spawnScene(p.level(), soort, midden.add(links.scale(6)), midden.add(links.scale(-6)));
        if (h == null) {
            src.sendFailure(Component.literal("Onbekend hoentje: " + soort + " (waterhoen of alpinehoen)"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Daar gaat een " + h.soort() + "! (scène)"), false);
        return 1;
    }

    private static int zoek(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Hoentje h = HoentjesSpawner.probeer(p, p.getRandom(), true);
        if (h == null) {
            src.sendFailure(Component.literal("Geen plekje gevonden (zoek een waterkant of hoge sneeuw, 20-48 blokken om je heen)."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Een " + h.soort() + " op " + h.blockPosition().toShortString()), false);
        return 1;
    }

    private static int noway(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        HoentjesEvents.vergeet(p.getUUID());
        HoentjesSpawner.vergeet(p.getUUID());
        src.sendSuccess(() -> Component.literal("Hoentjes-afkoeltijden vergeten."), false);
        return 1;
    }
}
