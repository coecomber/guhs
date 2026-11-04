package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The airship's travel map (fix round 2, g-ux): every airship NPC opens an illustrated map of the Vadsrijk (client
 * {@code npc/client/ReiskaartScherm}, art by tools/features/vadsrijk_reiskaart_art.py). A place is open once the main story
 * brought you there; the others are greyed with a "?" and a short requirement line; De Schemerwortel (zone 3) and a nameless
 * zone 4 are teasers. A click on an open place flies there through {@link QuestMotor#vliegNaar} (the same flights as the bells).
 */
public final class Reiskaart {
    /** All places in map order. */
    public static final List<String> PLEKKEN = List.of("knuffelkamp", "zwamzoet", "kandijkom", "schemerwortel", "zone4");
    /** States: open (click to fly), you are here, not discovered yet (grey "?", reason), a teaser for later (grey "?"). */
    public static final int OPEN = 0, HIER = 1, ONBEKEND = 2, TEASER = 3;
    /** How long after opening the map a choice is accepted (ticks): the map can only be used at an airship NPC. */
    static final long GELDIG = 20 * 60 * 3;
    private static final Map<UUID, Long> OPEN_SINDS = new ConcurrentHashMap<>();

    private Reiskaart() {
    }

    /** Where the player is now: the camp island, zone 1's mainland or zone 2. */
    public static String hier(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return "";   // (g-int: outside the dimension you are at none of them; a gametest far at z -7M looked like zone 2)
        }
        if (nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.inZone3(p.getX(), p.getZ())) {
            return "schemerwortel";   // [z3-wereld]
        }
        return QuestStap.zone2(p.getZ()) ? "kandijkom" : QuestStap.eiland(p.getX(), p.getZ()) ? "knuffelkamp" : "zwamzoet";
    }

    /** One place for this player: its state, the reason key (ONBEKEND/TEASER) and its argument (the player's level). */
    public static NpcPayloads.ReisPlek plek(ServerPlayer p, String id) {
        if (id.equals("schemerwortel") && !nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.zone2Klaar(p)
                && !nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.bereikt(p)) {   // [z3-wereld] a real place once zone 2 is done
            return new NpcPayloads.ReisPlek(id, TEASER, "vadsrijk.npc.reiskaart.binnenkort", 0);
        }
        if (id.equals("zone4")) {
            return new NpcPayloads.ReisPlek(id, TEASER, "vadsrijk.npc.reiskaart.binnenkort", 0);
        }
        String slot = QuestMotor.reisSlot(p, id);
        if (slot != null) {
            return new NpcPayloads.ReisPlek(id, ONBEKEND, slot + ".kort", Spelers.level(p));
        }
        return new NpcPayloads.ReisPlek(id, id.equals(hier(p)) ? HIER : OPEN, "", 0);
    }

    public static NpcPayloads.Reiskaart bouw(ServerPlayer p) {
        List<NpcPayloads.ReisPlek> l = new ArrayList<>();
        for (String id : PLEKKEN) {
            l.add(plek(p, id));
        }
        return new NpcPayloads.Reiskaart(hier(p), l);
    }

    /** Opens the map for this player (at an airship NPC). */
    public static void open(ServerPlayer p) {
        OPEN_SINDS.put(p.getUUID(), p.level().getGameTime());
        NpcPayloads.stuur(p, bouw(p));
    }

    /** The player clicked a place on the map: fly when it is open (and the map was opened at an airship NPC just now). */
    static void kies(ServerPlayer p, String id) {
        Long sinds = OPEN_SINDS.remove(p.getUUID());
        if (sinds == null || p.level().getGameTime() - sinds > GELDIG || !(Vadsrijk.in(p) || QuestMotor.VLIEG_TEST != null)) {
            return;
        }
        NpcPayloads.ReisPlek pl = PLEKKEN.contains(id) ? plek(p, id) : null;
        if (pl == null || pl.staat() != OPEN) {
            if (pl != null && pl.staat() == ONBEKEND) {
                p.sendOverlayMessage(Component.translatable(QuestMotor.reisSlot(p, id), Spelers.level(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        QuestMotor.vliegNaar(p, "kaart", id);
    }

    /** (tests) choose as if clicked on the open map. */
    public static void kiesVoorTest(ServerPlayer p, String id) {
        kies(p, id);
    }

    @Nullable
    static Long openSinds(UUID id) {
        return OPEN_SINDS.get(id);
    }

    static void vergeet(UUID id) {
        OPEN_SINDS.remove(id);
    }
}
