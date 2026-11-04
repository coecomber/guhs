package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The quest tracker (fix round 1): every second each player in the Vadsrijk gets the main story's current step and the step of
 * the quest they pinned in the questlog (J), with the spot the client draws the light beam over. Choices (kept in
 * {@code extra("npc")}): {@code volg_pin} (a quest id), {@code volg_uit} (tracker off), {@code straal_uit} (beam off).
 */
public final class NpcVolg {
    private static final Map<UUID, NpcPayloads.Volg> LAATST = new HashMap<>();

    private NpcVolg() {
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 7) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            KamerTips.stuur(p);   // (the puzzle-room line, twice a second)
        }
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (RijkScenes.bezig(p)) {
                continue;
            }
            stuur(p, false);
        }
    }

    /** Builds what the tracker shows now (also for tests). */
    public static NpcPayloads.Volg bouw(ServerPlayer p) {
        List<NpcPayloads.VolgRegel> regels = new ArrayList<>();
        QuestStap h = QuestStap.hoofdlijn(p);
        if (h != null) {
            regels.add(regel(h));
        }
        // (fix round 2) the job advancement of your class: shown as soon as you may start it, until it is handed in
        Quest k = klasseQuest(p);
        if (k != null) {
            regels.add(regel(QuestStap.van(p, k)));
        }
        // (fix round 2, g-werver) a mount return quest (Knuffelhert / Guhwolf): shown once it is available, until it is handed in
        Quest r = RijdierQuests.trackerQuest(p);
        if (r != null) {
            regels.add(regel(QuestStap.van(p, r)));
        }
        String pin = pin(p);
        Quest q = pin.isEmpty() ? null : Quests.van(pin);
        if (q != null && Questlog.actief(p, q.id()) && (h == null || !h.quest().equals(q.id())) && (k == null || !k.id().equals(q.id()))
                && (r == null || !r.id().equals(q.id()))) {
            regels.add(regel(QuestStap.van(p, q)));
        }
        return new NpcPayloads.Volg(!Questlog.vlag(p, "volg_uit"), !Questlog.vlag(p, "straal_uit"), regels);
    }

    /**
     * The job-advancement quest (soort KLASSE) of the player's current class that is active, or that may be started now
     * (level and earlier quests done); the first in story order. Null = none right now.
     */
    @Nullable
    public static Quest klasseQuest(ServerPlayer p) {
        String klasse = nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).klasse;
        if (klasse == null || klasse.isEmpty()) {
            return null;
        }
        for (Quest q : Quests.alle().values()) {
            if (q.soort() == Quest.Soort.KLASSE && klasse.equals(q.klasse()) && (Questlog.actief(p, q.id()) || Questlog.beschikbaar(p, q))) {
                return q;
            }
        }
        return null;
    }

    /** Is this quest id a job advancement? (the client colours its line and beam differently: ids adv.. / z2_adv..) */
    public static boolean isKlasse(String quest) {
        return quest.startsWith("adv") || quest.startsWith("z2_adv");
    }

    private static NpcPayloads.VolgRegel regel(QuestStap s) {
        return new NpcPayloads.VolgRegel(s.quest(), s.hoofd(), s.titel(), s.wat(), s.waar(), Optional.ofNullable(s.extra()), Optional.ofNullable(s.doel()));
    }

    static void stuur(ServerPlayer p, boolean altijd) {
        NpcPayloads.Volg v = bouw(p);
        if (altijd || !v.equals(LAATST.get(p.getUUID()))) {
            LAATST.put(p.getUUID(), v);
            NpcPayloads.stuur(p, v);
        }
    }

    public static String pin(ServerPlayer p) {
        return Questlog.data(p).getStringOr("volg_pin", "");
    }

    static void keuze(ServerPlayer p, NpcPayloads.VolgKeuze k) {
        switch (k.soort()) {
            case NpcPayloads.VolgKeuze.PIN -> {
                Quest q = Quests.van(k.quest());
                if (q == null || pin(p).equals(k.quest())) {
                    Questlog.data(p).remove("volg_pin");
                } else {
                    Questlog.data(p).putString("volg_pin", q.id());
                }
            }
            case NpcPayloads.VolgKeuze.TRACKER -> Questlog.zetVlag(p, "volg_uit", !Questlog.vlag(p, "volg_uit"));
            case NpcPayloads.VolgKeuze.STRAAL -> {
                boolean uit = !Questlog.vlag(p, "straal_uit");
                Questlog.zetVlag(p, "straal_uit", uit);
                p.sendOverlayMessage(Component.translatable(uit ? "vadsrijk.npc.volg.straal_uit" : "vadsrijk.npc.volg.straal_aan")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
            }
        }
        stuur(p, true);
        if (Vadsrijk.in(p)) {
            NpcPayloads.stuur(p, new NpcPayloads.Log(NpcLog.bouw(p), false));   // (refresh an open questlog)
        }
    }

    /** After a hand-in: tell the next step of the main story in chat (only when the main story moved on). */
    static void meldVolgende(ServerPlayer p, Quest klaar) {
        if (!klaar.hoofd()) {
            return;
        }
        if (klaar.actieKlaar().equals("bam") || klaar.actieKlaar().equals("cliffhanger")) {
            stuur(p, true);   // (a story action follows: the BAM starts hq05, the cliffhanger ends the story; no chat line now)
            return;
        }
        QuestStap s = QuestStap.hoofdlijn(p);
        if (s != null) {
            p.sendSystemMessage(s.regel().copy().withStyle(ChatFormatting.GOLD));
        }
        stuur(p, true);
    }

    @Nullable
    static NpcPayloads.Volg laatst(UUID id) {
        return LAATST.get(id);
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
    }
}
