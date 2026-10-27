package nl.juiced.guhs.feature.blub;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 1.8.0 test build, op commands for the Blub-guh (permission level 2; Brigadier merges them into the /guhs tree):
 * <ul>
 *   <li>{@code /guhs blub spawn <meter> <soort> [tint] [grootte] [baby]}: one in front of you;</li>
 *   <li>{@code /guhs blub raster}: the m06 grid in front of you (5 soorten x meter 0/25/50/75/100);</li>
 *   <li>{@code /guhs blub groepje [aantal]}: random Blubs with the real spawn dice (default 12);</li>
 *   <li>{@code /guhs blub tam}: every Blub-guh within 16 blocks becomes yours;</li>
 *   <li>{@code /guhs blub info}: the dice of the Blub you look at (or the nearest one);</li>
 *   <li>{@code /guhs blub snoes aan|uit}: your "snoes_geholpen" flag (wild Blubs only show with it: {@link BlubSnoes}).</li>
 * </ul>
 */
public final class BlubCommando {
    private BlubCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("blub").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("meter", IntegerArgumentType.integer(0, 100))
                                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "meter"), "blauw", 0, 1f, false))
                                .then(Commands.argument("soort", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(BlubTrekking.Soort.values()).map(BlubTrekking.Soort::id), b))
                                        .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "meter"),
                                                StringArgumentType.getString(c, "soort"), 0, 1f, false))
                                        .then(Commands.argument("tint", IntegerArgumentType.integer(0, 2))
                                                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "meter"),
                                                        StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "tint"), 1f, false))
                                                .then(Commands.argument("grootte", FloatArgumentType.floatArg(BlubTrekking.GROOTTE_MIN, BlubTrekking.GROOTTE_MAX))
                                                        .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "meter"),
                                                                StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "tint"),
                                                                FloatArgumentType.getFloat(c, "grootte"), false))
                                                        .then(Commands.argument("baby", BoolArgumentType.bool())
                                                                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "meter"),
                                                                        StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "tint"),
                                                                        FloatArgumentType.getFloat(c, "grootte"), BoolArgumentType.getBool(c, "baby")))))))))
                .then(Commands.literal("raster").executes(c -> raster(c.getSource())))
                .then(Commands.literal("groepje").executes(c -> groepje(c.getSource(), 12))
                        .then(Commands.argument("aantal", IntegerArgumentType.integer(1, 64))
                                .executes(c -> groepje(c.getSource(), IntegerArgumentType.getInteger(c, "aantal")))))
                .then(Commands.literal("tam").executes(c -> tam(c.getSource())))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("snoes")
                        .then(Commands.literal("aan").executes(c -> snoes(c.getSource(), true)))
                        .then(Commands.literal("uit").executes(c -> snoes(c.getSource(), false))))));
    }

    /** A Blub with these dice at this spot (not wild-rolled: finalizeSpawn keeps them), looking at the player. */
    public static BlubGuhEntity maak(ServerLevel level, Vec3 at, float yaw, BlubTrekking.Waarden w, boolean baby) {
        BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(level, EntitySpawnReason.COMMAND);
        b.zetWaarden(w);
        b.snapTo(at.x, at.y, at.z, yaw, 0);
        b.yHeadRot = yaw;
        b.yBodyRot = yaw;
        if (baby) {
            b.setAge(AgeableMob.BABY_START_AGE);
        }
        b.setPersistenceRequired();
        level.addFreshEntity(b);
        return b;
    }

    /** Where "in front of the player" is, and the yaw that looks back at the player. */
    private static Vec3 voor(ServerPlayer p, double afstand, double opzij) {
        Vec3 kijk = Vec3.directionFromRotation(0, p.getYRot());
        Vec3 rechts = new Vec3(-kijk.z, 0, kijk.x);
        return p.position().add(kijk.scale(afstand)).add(rechts.scale(opzij));
    }

    private static int spawn(CommandSourceStack source, int meter, String soort, int tint, float grootte, boolean baby) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BlubTrekking.Soort s = BlubTrekking.Soort.van(soort);
        maak(p.level(), voor(p, 3, 0), p.getYRot() + 180, new BlubTrekking.Waarden(meter, s, tint, grootte), baby);
        source.sendSuccess(() -> Component.translatable("commands.guhs.blub.spawn", meter, s.id(), tint, String.format(java.util.Locale.ROOT, "%.2f", grootte)), false);
        return 1;
    }

    private static int raster(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int[] meters = {0, 25, 50, 75, 100};
        BlubTrekking.Soort[] soorten = BlubTrekking.Soort.values();
        for (int r = 0; r < soorten.length; r++) {
            for (int k = 0; k < meters.length; k++) {
                BlubGuhEntity b = maak(p.level(), voor(p, 4 + r * 1.6, (k - 2) * 1.6), p.getYRot() + 180,
                        new BlubTrekking.Waarden(meters[k], soorten[r], 0, 1f), false);
                b.zet(nl.juiced.guhs.feature.piep.PiepInstelling.RONDVADSEN, false);
                b.tame(p);
                b.setOrderedToSit(true);
                b.setInSittingPose(true);
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.guhs.blub.raster"), false);
        return meters.length * soorten.length;
    }

    private static int groepje(CommandSourceStack source, int aantal) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel level = p.level();
        Vec3 midden = voor(p, 5, 0);
        for (int i = 0; i < aantal; i++) {
            double hoek = level.getRandom().nextDouble() * Math.PI * 2, r = 1 + level.getRandom().nextDouble() * 3.5;
            Vec3 at = midden.add(Math.cos(hoek) * r, 0, Math.sin(hoek) * r);
            at = new Vec3(at.x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z)), at.z);
            maak(level, at, level.getRandom().nextFloat() * 360, BlubTrekking.trek(level.getRandom()), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.guhs.blub.groepje", aantal), false);
        return aantal;
    }

    private static int tam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        List<BlubGuhEntity> blubs = p.level().getEntitiesOfClass(BlubGuhEntity.class, p.getBoundingBox().inflate(16));
        for (BlubGuhEntity b : blubs) {
            if (!b.isTame()) {
                b.tame(p);
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.guhs.blub.tam", blubs.size()), false);
        return blubs.size();
    }

    private static int snoes(CommandSourceStack source, boolean aan) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        BlubSnoes.zet(p, aan);
        source.sendSuccess(() -> Component.translatable(aan ? "commands.guhs.blub.snoes.aan" : "commands.guhs.blub.snoes.uit"), false);
        return 1;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Vec3 oog = p.getEyePosition(), kijk = p.getViewVector(1f);
        BlubGuhEntity b = p.level().getEntitiesOfClass(BlubGuhEntity.class, p.getBoundingBox().inflate(24)).stream()
                .min(Comparator.comparingDouble(e -> {
                    Vec3 naar = e.position().add(0, e.getBbHeight() / 2, 0).subtract(oog);
                    double langs = naar.dot(kijk);
                    return langs <= 0 ? 1e9 + naar.lengthSqr() : naar.lengthSqr() - langs * langs + langs * 0.01;
                })).orElse(null);
        if (b == null) {
            source.sendFailure(Component.translatable("commands.guhs.blub.geen"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.guhs.blub.info", b.getDisplayName(), BlubTrekking.metertje(b.meter()), b.meter(),
                b.blubSoort().id(), b.tint(), String.format(java.util.Locale.ROOT, "%.2f", b.grootte()), b.isBaby() ? "baby" : "groot",
                b.isTame() ? "tam" : "wild", b.draagt(BlubKleding.Plek.STRIK) == null ? "-" : b.draagt(BlubKleding.Plek.STRIK).id()), false);
        return b.meter();
    }
}
