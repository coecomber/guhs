package nl.juiced.guhs.feature.gids;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.spelen.SpelGroepen;

/**
 * De gids (2.9): the Guhdex as all-in-one (icon tabs Guhs, Knus, Minigames, Kleding; client.screen.GuhDexScreen) and the
 * Superkompas with icon tabs (client.screen.SuperkompasScreen, item.SuperkompasItem.CATEGORIES). No registry content:
 * the screens read the synced data of the Highscores, {@link SpelGroepen} (visited) and the clothing unlocks
 * ({@link GidsData} puts it in order). The server side only hands out the two explorer advancements of the tab
 * "De Grote Guhspelen":
 * <ul>
 *   <li>{@link #ONTDEKKER}: you've been in all six buildings of De Grote Guhspelen;</li>
 *   <li>{@link #WERELDREIZIGER}: you've been in every minigame building of the Guhmensie (a challenge).</li>
 * </ul>
 * Resources: tools/features/gids.py.
 */
public final class GidsFeature {
    /** How often (ticks) the explorer advancements are checked (the visits themselves: SpelGroepen, every 40 ticks). */
    public static final int CHECK_TICKS = 100;
    public static final String ONTDEKKER = "grote_guhspelen/gids_ontdekker";
    public static final String WERELDREIZIGER = "grote_guhspelen/gids_wereldreiziger";

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(GidsFeature::onPlayerTick);
    }

    public static void payloads(PayloadRegistrar registrar) {
        VerhalenPayloads.register(registrar);   // (the Guhdex tab "Verhalen")
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && (player.tickCount + player.getId()) % CHECK_TICKS == 0 && !player.isSpectator()) {
            kijk(player);
        }
    }

    /** The groups (with a building) you need for an advancement: only De Grote Guhspelen, or all of them. */
    public static List<SpelGroepen.Groep> nodig(boolean alleenGroteGuhspelen) {
        return SpelGroepen.alle().stream().filter(g -> g.structuur() != null
                && (!alleenGroteGuhspelen || g.tijdperk() == SpelGroepen.Tijdperk.GROTE_GUHSPELEN)).toList();
    }

    /** Checks the explorer advancements (server). */
    public static void kijk(ServerPlayer player) {
        List<String> bezocht = SpelGroepen.bezocht(player);
        if (nodig(true).stream().allMatch(g -> bezocht.contains(g.id()))) {
            grant(player, ONTDEKKER);
        }
        if (nodig(false).stream().allMatch(g -> bezocht.contains(g.id()))) {
            grant(player, WERELDREIZIGER);
        }
    }

    /** Grants a code-granted advancement (criterion "done", trigger minecraft:impossible). */
    public static void grant(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    public static boolean heeft(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private GidsFeature() {
    }
}
