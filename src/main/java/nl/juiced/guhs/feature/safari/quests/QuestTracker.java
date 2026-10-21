package nl.juiced.guhs.feature.safari.quests;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Test round 4 (design ronde 49): where the player is in the safari quests, read from the player's safari state (the
 * server's SafariSpeler tag, or its client copy {@code SafariSync.stand}): the quest tracker in the corner of the screen
 * and the "Quests" page in the Safari tab of the Guhdex both show this. No chat.
 * <p>
 * {@link #regels}: one line per quest that is busy now (nothing for a quest not started or finished);
 * {@link #overzicht}: every quest with its state (for the Guhdex).
 */
public final class QuestTracker {
    /** A quest in the overview: its name, where it stands (one line) and whether it is done. */
    public record Quest(Component naam, Component stand, boolean klaar) {
    }

    /** The lines of the tracker (empty: nothing busy, the tracker hides). */
    public static List<Component> regels(CompoundTag stand) {
        List<Component> uit = new ArrayList<>();
        Component v = vermomd(stand);
        if (v != null) {
            uit.add(v);
        }
        Component o = olifant(stand);
        if (o != null) {
            uit.add(o);
        }
        int n = geheimpjes(stand), totaal = totaal();
        if (n > 0 && n < totaal) {
            uit.add(Component.translatable("gui.guhs.safari.tracker.geheimpjes", n, totaal));
        }
        return uit;
    }

    /** Every safari quest with its state, for the Guhdex. */
    public static List<Quest> overzicht(CompoundTag stand) {
        List<Quest> uit = new ArrayList<>();
        CompoundTag q = stand.getCompoundOrEmpty("Quests");
        String vs = q.getStringOr(VermomdeGuh.QUEST, "");
        Component v = vermomd(stand);
        uit.add(new Quest(Component.translatable("gui.guhs.safari.tracker.naam.vermomd"),
                vs.equals("klaar") ? Component.translatable("gui.guhs.safari.tracker.klaar")
                        : v != null ? v : Component.translatable("gui.guhs.safari.tracker.start.vermomd"), vs.equals("klaar")));
        String os = q.getStringOr(VaheOlifant.QUEST, "");
        Component o = olifant(stand);
        uit.add(new Quest(Component.translatable("gui.guhs.safari.tracker.naam.olifant"),
                os.equals("klaar") ? Component.translatable("gui.guhs.safari.tracker.klaar")
                        : o != null ? o : Component.translatable("gui.guhs.safari.tracker.start.olifant"), os.equals("klaar")));
        int n = geheimpjes(stand), totaal = totaal();
        uit.add(new Quest(Component.translatable("gui.guhs.safari.tracker.naam.geheimpjes"),
                Component.translatable("gui.guhs.safari.tracker.geheimpjes", n, totaal), n >= totaal));
        return uit;
    }

    /** De vermomde guh: "Vermomde guh (1/3): zoek bij de reuzenpanda's in de bamboe" (null: not started or done). */
    static Component vermomd(CompoundTag stand) {
        String s = stand.getCompoundOrEmpty("Quests").getStringOr(VermomdeGuh.QUEST, "");
        int gevonden = switch (s) {
            case "gevraagd" -> 0;
            case "1" -> 1;
            case "2" -> 2;
            default -> -1;
        };
        if (gevonden < 0) {
            return null;
        }
        String waar = VermomdeGuh.PLEKKEN.get(gevonden).soort();
        return Component.translatable("gui.guhs.safari.tracker.vermomd", gevonden, VermomdeGuh.PLEKKEN.size(),
                Component.translatable("gui.guhs.safari.tracker.waar." + waar));
    }

    /** De te vahe olifant: what Dikkie still wants, or the laps on the treadmill (null: not started or done). */
    static Component olifant(CompoundTag stand) {
        CompoundTag q = stand.getCompoundOrEmpty("Quests");
        String s = q.getStringOr(VaheOlifant.QUEST, "");
        if (s.equals("dieet")) {
            List<String> gegeten = List.of(q.getStringOr(VaheOlifant.GEGETEN, "").split(","));
            MutableComponent nog = Component.empty();
            int k = 0;
            for (String eten : VaheOlifant.DIEET.keySet()) {
                if (!gegeten.contains(eten)) {
                    if (k++ > 0) {
                        nog.append(", ");
                    }
                    nog.append(Component.translatable("gui.guhs.safari.olifant." + eten));
                }
            }
            return Component.translatable("gui.guhs.safari.tracker.dieet", nog);
        }
        if (s.equals("loopband")) {
            int rondes;
            try {
                rondes = Integer.parseInt(q.getStringOr(VaheOlifant.RONDEN, "0"));
            } catch (NumberFormatException e) {
                rondes = 0;
            }
            return Component.translatable("gui.guhs.safari.tracker.loopband", rondes, VaheOlifant.RONDES);
        }
        return null;
    }

    static int geheimpjes(CompoundTag stand) {
        return stand.getCompoundOrEmpty("Geheimpjes").keySet().size();
    }

    static int totaal() {
        int n = QuestData.get().geheimpjes.size();
        return n > 0 ? n : 8;
    }

    private QuestTracker() {
    }
}
