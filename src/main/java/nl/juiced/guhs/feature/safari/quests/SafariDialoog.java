package nl.juiced.guhs.feature.safari.quests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Test round 4 (design ronde 49): the safari quests talk in the guhs talking screen ({@link Praat}), never in the chat.
 * <p>
 * Everything a quest says in one tick is collected per player ({@link #zeg}, {@link #beloning}) and shown at the end of
 * the tick as ONE scene of pages ("Verder »"), the speaker drawn on each page (the disguised guh, Dikkie, the head
 * ranger; nobody for "the park" itself). A scene waits while the player is using an item (the camera: a dialog never
 * pops up in the middle of taking a photo) and is dropped when the player left. What a quest is busy with is shown by
 * the tracker ({@link QuestTracker}) and the Safari tab of the Guhdex.
 */
public final class SafariDialoog {
    /** The sleutel of every safari quest scene (its answer: just "Top!", nothing to do). */
    public static final String SLEUTEL = "safari_quest";
    /** Names above a page whose speaker is not drawn (lang keys). */
    public static final String PARK = "gui.guhs.safari.dialoog.park", RANGER = "gui.guhs.safari.dialoog.hoofdranger",
            VERKLEED = "gui.guhs.safari.dialoog.verkleed", DIKKIE = "gui.guhs.safari.olifant.naam";

    private static final Map<UUID, List<Praat.Regel>> WACHT = new ConcurrentHashMap<>();

    static void register() {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> flush(event.getServer()));
    }

    /**
     * One page for this player: the speaker (null: nobody drawn, then {@code naamKey} is the name above it; "" with a
     * speaker = its own name), the text and its arguments.
     */
    public static void zeg(ServerPlayer p, @Nullable Entity spreker, String naamKey, String tekstKey, Object... args) {
        WACHT.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Praat.Regel(spreker, naamKey, tekstKey, args));
    }

    /** A page "Je krijgt: ...!" (the item itself goes to the inventory, see {@link SafariQuests#geef}). */
    public static void beloning(ServerPlayer p, ItemStack stack) {
        zeg(p, null, PARK, "gui.guhs.safari.beloning.item", stack.getHoverName());
    }

    /** (Gametests) the text keys waiting for this player, in order. */
    public static List<String> wachtend(ServerPlayer p) {
        List<String> uit = new ArrayList<>();
        for (Praat.Regel r : WACHT.getOrDefault(p.getUUID(), List.of())) {
            uit.add(r.tekstKey());
        }
        return uit;
    }

    /** (Gametests) forget what waits for this player. */
    public static void vergeet(ServerPlayer p) {
        WACHT.remove(p.getUUID());
    }

    private static void flush(MinecraftServer server) {
        if (WACHT.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, List<Praat.Regel>> e : WACHT.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || p.hasDisconnected()) {
                WACHT.remove(e.getKey());
                continue;
            }
            if (p.isUsingItem() || e.getValue().isEmpty()) {
                continue;                               // (the camera: wait until the photo is taken)
            }
            List<Praat.Regel> regels = WACHT.remove(e.getKey());
            if (regels == null || regels.isEmpty()) {
                continue;
            }
            try {
                Praat.scene(p, SLEUTEL, regels, new Praat.Optie(1, "gui.guhs.safari.dialoog.top"));
            } catch (RuntimeException ex) {
                // (a gametest player has no real connection: the scene simply is not shown)
            }
        }
    }

    private SafariDialoog() {
    }
}
