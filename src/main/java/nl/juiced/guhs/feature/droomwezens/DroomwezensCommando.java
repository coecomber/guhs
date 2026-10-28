package nl.juiced.guhs.feature.droomwezens;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * Test commands (permission level 2, merged into /guhs):
 * <ul>
 *   <li>{@code /guhs droomwezens spawn <naam>}: one in front of you. Names: the NPC kinds (snoes, snoes_blij, zandguh, wekkerguh,
 *       rommelguh, droom_omaguh, kussengevecht_guh), {@code sterrenvisje} (a little school of every colour; stand in water),
 *       {@code kussengevecht} (two kussengevecht-guhs facing each other: they throw pillows) and {@code alle} (every NPC in a row).</li>
 * </ul>
 */
public final class DroomwezensCommando {
    private DroomwezensCommando() {
    }

    static List<String> namen() {
        List<String> n = new ArrayList<>();
        DroomNpc.KINDS.forEach(k -> n.add(k.id()));
        n.addAll(List.of("sterrenvisje", "kussengevecht", "alle"));
        return n;
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("droomwezens").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("naam", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(namen(), b))
                        .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "naam")))))));
    }

    static int spawn(CommandSourceStack src, String naam) {
        ServerPlayer p = src.getPlayer();
        ServerLevel level = src.getLevel();
        Vec3 kijk = p == null ? Vec3.ZERO : Vec3.directionFromRotation(0, p.getYRot());
        Vec3 voor = (p == null ? src.getPosition() : p.position()).add(kijk.scale(3));
        float naarMij = (p == null ? 0 : p.getYRot()) + 180;
        switch (naam) {
            case "alle" -> {
                Vec3 zij = new Vec3(-kijk.z, 0, kijk.x);
                List<GuhNpcEntity.Kind> kinds = DroomNpc.KINDS;
                for (int i = 0; i < kinds.size(); i++) {
                    DroomNpc.spawn(level, voor.add(zij.scale((i - (kinds.size() - 1) / 2.0) * 1.6)), naarMij, kinds.get(i));
                }
            }
            case "kussengevecht" -> {
                Vec3 zij = new Vec3(-kijk.z, 0, kijk.x);
                Vec3 a = voor.add(zij.scale(1.8)), b = voor.add(zij.scale(-1.8));
                DroomNpc.spawn(level, a, yaw(a, b), GuhNpcEntity.Kind.KUSSENGEVECHT_GUH);
                DroomNpc.spawn(level, b, yaw(b, a), GuhNpcEntity.Kind.KUSSENGEVECHT_GUH);
            }
            case "sterrenvisje" -> {
                for (SterrenvisjeEntity.Kleur k : SterrenvisjeEntity.Kleur.values()) {
                    for (int i = 0; i < 2; i++) {
                        SterrenvisjeEntity v = DroomwezensFeature.STERRENVISJE.get().create(level, EntitySpawnReason.COMMAND);
                        v.setKleur(k);
                        v.snapTo(voor.x + level.getRandom().nextGaussian() * 0.8, voor.y + 0.4, voor.z + level.getRandom().nextGaussian() * 0.8,
                                level.getRandom().nextFloat() * 360, 0);
                        level.addFreshEntity(v);
                    }
                }
            }
            default -> {
                GuhNpcEntity.Kind kind = DroomNpc.van(naam);
                if (kind == null) {
                    src.sendFailure(Component.translatable("gui.guhs.droomwezens.onbekend", naam, String.join(", ", namen())));
                    return 0;
                }
                DroomNpc.spawn(level, voor, naarMij, kind);
            }
        }
        src.sendSuccess(() -> Component.translatable("gui.guhs.droomwezens.gespawnd", naam), false);
        return 1;
    }

    private static float yaw(Vec3 van, Vec3 naar) {
        Vec3 d = naar.subtract(van);
        return (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
    }
}
