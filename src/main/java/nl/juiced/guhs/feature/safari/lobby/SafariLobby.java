package nl.juiced.guhs.feature.safari.lobby;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Op Safari! step 2, slice S2: the lobby of Safaripark Vadse Bergen (design 1.5, 1.7, 1.8).
 * <ul>
 *   <li>the guh characters ({@link SafariNpcs}, spots from {@link LobbyPlekken}) and what they say ({@link Rollen}):
 *       hoofdranger (tip of the day; S4 hooks its quests in with {@link Rollen#haak}), fotograaf (the camera), dierenverzorger,
 *       giftshop seller ({@link SafariWinkel}), lodgebeheerder and the eight zone experts of the expertenpaviljoen (zone
 *       badge + painting + 100 coins when a zone of the dex is complete; their counter sparkles for you then);</li>
 *   <li>the aaihoek chores ({@link Aaihoek});</li>
 *   <li>the lodge: sleeping there sets your spawn (the dimension's bed rule) and the flag {@code vroege_vogel} (S1 doubles
 *       the coins of the first photo of the day and clears it);</li>
 *   <li>the welcome at the first visit (the hoofdranger, the camera, "njeg, veel spotplezier!");</li>
 *   <li>the visitor guhs ({@link Bezoekers});</li>
 *   <li>test round 3 (J3): the ijskraam (the ijsverkoper's animal ice creams), the guh-speeltuin ({@link Speeltuin}), the
 *       kampvuuravond ({@link Kampvuur}) and the kijk-guhs on the lookouts ({@link Kijkguhs}); spots in {@link Beleving}.</li>
 * </ul>
 */
public final class SafariLobby {
    /** The SafariSpeler flag the lodge sets on waking up (S1 reads and clears it). */
    public static final String VROEGE_VOGEL = "vroege_vogel";

    public static void register(IEventBus modBus) {
        LobbyItems.ITEMS.register(modBus);
        Rollen.register();
        SafariWinkel.registreerVast();
        NeoForge.EVENT_BUS.addListener(SafariLobby::tick);
        NeoForge.EVENT_BUS.addListener(SafariLobby::opInteractie);
        NeoForge.EVENT_BUS.addListener(Bezoekers::opInteractieSpecifiek);
        NeoForge.EVENT_BUS.addListener(Kijkguhs::opInteractieSpecifiek);
        NeoForge.EVENT_BUS.addListener(SafariLobby::opWakker);
    }

    public static void payloads(PayloadRegistrar registrar) {
        LobbyPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> out) {
        LobbyItems.creative(out);
    }

    private static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        if (!Safari.in(event.getEntity())) {
            return;
        }
        Aaihoek.opInteractie(event);
        if (!event.isCanceled()) {
            Kijkguhs.opInteractie(event);
        }
        if (!event.isCanceled()) {
            Bezoekers.opInteractie(event);
        }
    }

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SafariNpcs.tick(server);
        Aaihoek.tick(server);
        Bezoekers.tick(server);
        Kijkguhs.tick(server);
        Speeltuin.tick(server);
        Kampvuur.tick(server);
        if (server.getTickCount() % 20 == 3) {
            welkom(server);
        }
        if (server.getTickCount() % 40 == 11) {
            balies(server);
        }
    }

    /** The first visit: the hoofdranger welcomes you (a click-to-continue scene) and you get the camera. */
    private static void welkom(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            if (SafariSpeler.vlag(p, "welkom") || Praat.bezig(p) || p.isSpectator()) {
                continue;
            }
            SafariSpeler.zetVlag(p, "welkom", true);
            Entity ranger = SafariNpcs.van(level, "hoofdranger");
            String naam = "entity.guhs.guh_npc.safari_hoofdranger";
            boolean camera = Rollen.geefCamera(p);
            Praat.scene(p, "safari_welkom", List.of(
                    new Praat.Regel(ranger, naam, "gui.guhs.safari.welkom.1"),
                    new Praat.Regel(ranger, naam, "gui.guhs.safari.welkom.2"),
                    new Praat.Regel(ranger, naam, camera ? "gui.guhs.safari.welkom.3.camera" : "gui.guhs.safari.welkom.3"),
                    new Praat.Regel(ranger, naam, "gui.guhs.safari.welkom.4")),
                    new Praat.Optie(0, "gui.guhs.safari.welkom.optie"));
        }
    }

    /** A counter in the expertenpaviljoen sparkles for a player who has something to claim there. */
    private static void balies(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            for (LobbyPlekken.Npc n : LobbyPlekken.get().npcs) {
                BlockPos b = n.balie();
                if (n.zone() == null || b == null || p.distanceToSqr(b.getX(), b.getY(), b.getZ()) > 32 * 32) {
                    continue;
                }
                if (Rollen.claimbaar(server, p.getUUID(), n.zone())) {
                    level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, b.getX() + 0.5, b.getY() + 1.4, b.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                    level.sendParticles(p, ParticleTypes.END_ROD, true, false, b.getX() + 0.5, b.getY() + 2.0, b.getZ() + 0.5, 1, 0.1, 0.2, 0.1, 0.01);
                }
            }
        }
    }

    /** Waking up in a lodge hut: the vroege-vogel bonus for the first photo of the day (S1). */
    private static void opWakker(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Safari.in(p)) {
            return;
        }
        if (!LobbyPlekken.get().lodge.contains(p.position())) {
            return;
        }
        SafariSpeler.zetVlag(p, "lodge", true);
        if (!SafariSpeler.vlag(p, VROEGE_VOGEL)) {
            SafariSpeler.zetVlag(p, VROEGE_VOGEL, true);
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.lodge.vroege_vogel").withStyle(ChatFormatting.GOLD));
        }
    }

    private SafariLobby() {
    }
}
