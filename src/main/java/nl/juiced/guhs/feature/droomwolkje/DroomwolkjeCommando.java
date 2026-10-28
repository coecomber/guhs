package nl.juiced.guhs.feature.droomwolkje;

import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Droomwolkje commands (TESTEN_DROOMWOLKJE.md):
 * <ul>
 *   <li>for everyone, {@code /droomwolkje}: {@code vriend <speler>} (may build along on your wolkje), {@code weg <speler>},
 *       {@code vrienden}, {@code bezoek <speler>} (visit their wolkje, in a dream);</li>
 *   <li>op (level 2), {@code /guhs droomwolkje}: {@code ga} (to your own wolkje, before Snoes too), {@code reset} (forget
 *       your wolkje: the next one is a fresh one), {@code gast} (a droomgast now, from one of your own guhs if no one sleeps).</li>
 * </ul>
 */
public final class DroomwolkjeCommando {
    private DroomwolkjeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("droomwolkje")
                .then(Commands.literal("vriend").then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> vriend(c.getSource(), EntityArgument.getPlayer(c, "speler"), true))))
                .then(Commands.literal("weg").then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> vriend(c.getSource(), EntityArgument.getPlayer(c, "speler"), false))))
                .then(Commands.literal("vrienden").executes(c -> vrienden(c.getSource())))
                .then(Commands.literal("bezoek").then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> bezoek(c.getSource(), EntityArgument.getPlayer(c, "speler"))))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("droomwolkje")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").executes(c -> ga(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.literal("gast").executes(c -> gast(c.getSource())))));
    }

    private static ServerLevel droom(CommandSourceStack source) {
        return source.getServer().getLevel(DromenFeature.DIM);
    }

    private static int vriend(CommandSourceStack source, ServerPlayer vriend, boolean erbij) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel droom = droom(source);
        if (droom == null || !Wolkjes.magEenWolkje(p)) {
            source.sendFailure(Component.translatable("gui.guhs.droomwolkje.nog_niet"));
            return 0;
        }
        if (vriend == p) {
            source.sendFailure(Component.translatable("gui.guhs.droomwolkje.vriend.zelf"));
            return 0;
        }
        Wolkjes.Stand s = Wolkjes.stand(droom);
        Wolkjes.Wolkje w = s.maak(p.getUUID());
        boolean veranderd = erbij ? w.vrienden.add(vriend.getUUID()) : w.vrienden.remove(vriend.getUUID());
        s.setDirty();
        source.sendSuccess(() -> Component.translatable("gui.guhs.droomwolkje.vriend." + (erbij ? "erbij" : "weg"), vriend.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        if (veranderd && erbij) {
            vriend.sendSystemMessage(Component.translatable("gui.guhs.droomwolkje.vriend.melding", p.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return 1;
    }

    private static int vrienden(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel droom = droom(source);
        Wolkjes.Wolkje w = droom == null ? null : Wolkjes.stand(droom).van(p.getUUID());
        if (w == null || w.vrienden.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("gui.guhs.droomwolkje.vrienden.geen").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        StringBuilder namen = new StringBuilder();
        for (UUID id : w.vrienden) {
            ServerPlayer v = source.getServer().getPlayerList().getPlayer(id);
            namen.append(namen.isEmpty() ? "" : ", ").append(v != null ? v.getGameProfile().name() : id.toString().substring(0, 8));
        }
        source.sendSuccess(() -> Component.translatable("gui.guhs.droomwolkje.vrienden.lijst", namen.toString()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return w.vrienden.size();
    }

    private static int bezoek(CommandSourceStack source, ServerPlayer gastheer) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!DroomReis.inDroom(p)) {
            source.sendFailure(Component.translatable("gui.guhs.droomwolkje.bezoek.droom"));
            return 0;
        }
        return Wolkjes.naar(p, gastheer.getUUID()) ? 1 : 0;
    }

    private static int ga(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        return Wolkjes.naar(p, p.getUUID()) ? 1 : 0;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel droom = droom(source);
        if (droom == null) {
            return 0;
        }
        Wolkjes.stand(droom).vergeet(p.getUUID());
        source.sendSuccess(() -> Component.literal("Droomwolkje vergeten: de volgende keer krijg je een nieuw wolkje (op een nieuwe plek)."), false);
        return 1;
    }

    /** A droomgast now: of a sleeping guh of someone else, else of one of your own guhs, else a stranger. */
    private static int gast(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel droom = droom(source);
        if (droom == null) {
            return 0;
        }
        GuhEntity bron = Droomgasten.kies(source.getServer(), java.util.List.of(p), droom.getRandom());
        if (bron == null) {
            for (ServerLevel level : source.getServer().getAllLevels()) {
                java.util.List<GuhEntity> eigen = new java.util.ArrayList<>();
                level.getEntities(EntityTypeTest.forClass(GuhEntity.class), g -> g.isTame() && p.getUUID().equals(g.getOwnerUUID())
                        && !Droomgasten.isGast(g), eigen, 1);
                if (!eigen.isEmpty()) {
                    bron = eigen.get(0);
                    break;
                }
            }
        }
        if (bron == null) {
            bron = ModEntities.GUH.get().create(droom, EntitySpawnReason.COMMAND);
            if (bron == null) {
                return 0;
            }
            bron.setVariant(GuhVariant.NORMAL);
            bron.tame(p);
        }
        GuhEntity g = Droomgasten.maak(droom, bron);
        source.sendSuccess(() -> Component.literal(g != null ? "Droomgast op het plein: " + g.getName().getString() : "Geen droomgast gelukt"), false);
        return g != null ? 1 : 0;
    }
}
