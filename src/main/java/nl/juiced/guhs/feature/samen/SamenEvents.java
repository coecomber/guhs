package nl.juiced.guhs.feature.samen;

import java.util.Comparator;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.Reden;

/**
 * The game events of samen (2.10): login (the hartjes rewards catch-up and the emote sync), death and respawn (the
 * comfort), going to bed (the goodnight wave), and the op command {@code /guhs samen} for tests and the visual QA:
 * {@code /guhs samen hartjes <lief|mega|zielsguh>} fills up the nearest own guh to that level (as days of hearts would),
 * {@code /guhs samen welkom|troost|welterusten|bff} shows that reaction with the nearest own guh.
 */
public final class SamenEvents {
    private SamenEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SamenBeloning.inhalen(player);
            SamenVriendjes.inhalen(player);   // 1.2.7: friends made while you were offline
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            SamenReacties.gestorven(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
            SamenReacties.troost(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SamenReacties.spelerTick(player);
        }
    }

    /** Before everything is saved: no guh stays in a kart (the race guh is never saved, its passenger would go with it). */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            SamenMee.stapUit(event.getServer(), p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("samen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("hartjes").then(Commands.argument("niveau", StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"lief", "mega", "zielsguh"}, b))
                        .executes(ctx -> hartjes(ctx.getSource(), StringArgumentType.getString(ctx, "niveau")))))
                .then(Commands.literal("welkom").executes(ctx -> reactie(ctx.getSource(), "welkom")))
                .then(Commands.literal("troost").executes(ctx -> reactie(ctx.getSource(), "troost")))
                .then(Commands.literal("welterusten").executes(ctx -> reactie(ctx.getSource(), "welterusten")))
                .then(Commands.literal("bff").executes(ctx -> reactie(ctx.getSource(), "bff")))));
    }

    private static GuhEntity naaste(ServerPlayer player) {
        return player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(24),
                        g -> Band.isBandGuh(g) && player.getUUID().equals(g.getOwnerUUID()))
                .stream().min(Comparator.comparingDouble(g -> g.distanceToSqr(player))).orElse(null);
    }

    private static int hartjes(CommandSourceStack source, String niveauId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BandNiveau doel = null;
        for (BandNiveau n : BandNiveau.values()) {
            if (n.id().equals(niveauId)) {
                doel = n;
            }
        }
        GuhEntity guh = naaste(player);
        if (doel == null || guh == null) {
            source.sendFailure(Component.translatable("commands.guhs.samen.geen_guh"));
            return 0;
        }
        for (int dag = 0; dag < 40 && Band.hartjes(guh) < doel.drempel(); dag++) {
            BandData.Rec r = BandData.get(player.level().getServer()).vind(player.getUUID(), guh.getUUID());
            if (r != null) {
                r.dag = -1;   // (a new "day": the caps start over)
            }
            for (Reden reden : Reden.values()) {
                if (Band.hartjes(guh) < doel.drempel()) {
                    Band.geefHartjes(guh, player, Math.min(reden.dagMax(), doel.drempel() - Band.hartjes(guh)), reden);
                }
            }
        }
        int h = Band.hartjes(guh);
        source.sendSuccess(() -> Component.translatable("commands.guhs.samen.hartjes", guh.getName(), h, Band.niveau(guh).naam()), false);
        return 1;
    }

    private static int reactie(CommandSourceStack source, String wat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GuhEntity guh = naaste(player);
        boolean ok = switch (wat) {
            case "welkom" -> guh != null && SamenReacties.welkom(guh, player);
            case "troost" -> SamenReacties.troost(player) != null;
            case "welterusten" -> SamenReacties.welterusten(player) > 0;
            case "bff" -> {
                if (guh != null && guh.emotes.start(nl.juiced.guhs.feature.emotes.Emote.BFF_KNUFFEL, false,
                        nl.juiced.guhs.feature.emotes.GuhEmotes.Source.OWNER)) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        if (!ok) {
            source.sendFailure(Component.translatable("commands.guhs.samen.mislukt"));
            return 0;
        }
        return 1;
    }
}
