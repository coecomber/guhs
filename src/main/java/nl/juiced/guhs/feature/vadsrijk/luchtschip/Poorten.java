package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;

/**
 * U-LUCHTSCHIP: WHY a line is still closed for you, in words (the gates themselves stay {@link Luchtschepen#SLOT}).
 * <p>
 * Per line: the main-story quest after which you may fly ({@link Poort#naQuest}), the story check, and the level. When the gate
 * says no, the message tells which one is missing: "Volg eerst de hoofdquest: na "Een grauwe schaduw" kun je hier vliegen (je
 * bent nu bij: "Welkom, rekruut!")", or "Kom terug vanaf level 20 (jij: level 12)". Shown in the chat when you click the ship,
 * the pier or the departure board, and short in the action bar near the pier ({@link #kort}).
 */
public final class Poorten {
    /** naQuest: the quest whose end opens the line; verhaalKlaar: the story part is done; minLevel: 0 = none. */
    public record Poort(String naQuest, int minLevel, Predicate<ServerPlayer> verhaalKlaar) {
    }

    public static final Map<String, Poort> POORTEN = new HashMap<>();
    /** No chat repeat within 3 s (clicking the pier a few times). */
    private static final Map<UUID, Long> LAATST = new HashMap<>();

    static {
        POORTEN.put("kamp_z1", new Poort("hq05_klodderreus", 0,
                p -> Questlog.klaar(p, "hq05_klodderreus") || Questlog.status(p, "hq06_zwamzoet") != 0));
        POORTEN.put("z1_z2", new Poort("hq12_schaduw", 20,
                p -> nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.zone1Klaar(p) || Questlog.status(p, "z2_hq01_splinterpieken") != 0));
        POORTEN.put("z2_z3", new Poort("z2_hq12_hartsplinter", nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.MIN_LEVEL,
                p -> Questlog.vlag(p, "zone2_klaar") || nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.bereikt(p)));
        POORTEN.put("z3_z4", new Poort("z3_hq14_vahoegnir", 50,
                p -> Questlog.vlag(p, "zone3_klaar") || nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4Verhaal.bereikt(p)));
        POORTEN.put("z4_z5", new Poort("z4_hq07_opstijgpier", nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Verhaal.MIN_LEVEL,
                nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Verhaal::magZien));
    }

    private Poorten() {
    }

    /** Null when this player may board this line in this direction now, else the gate's lang key (the old short reason). */
    @Nullable
    public static String dicht(ServerPlayer p, Lijnen.Lijn l, boolean heen) {
        if (p.getAbilities().instabuild || l.test()) {
            return null;
        }
        String poort = nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.lijnSlot(p, (heen ? l.b() : l.a()).zone());   // X-ZONE1: zone closed
        if (poort != null) {
            return poort;
        }
        return Luchtschepen.SLOT.getOrDefault(l.id(), (q, h) -> null).apply(p, heen);
    }

    private static Component questNaam(String id) {
        Quest q = Quests.van(id);
        return Component.literal("\"").append(q != null ? Component.translatable(q.titelKey()) : Component.literal(id)).append("\"")
                .withStyle(ChatFormatting.YELLOW);
    }

    /** The main quest you are on now (the first active story quest), or null. */
    @Nullable
    static Quest huidig(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            if (q.soort() == Quest.Soort.HOOFD) {
                return q;
            }
        }
        return null;
    }

    /** The full explanation (chat): what to do first and why. {@code slot}: the gate's own key (fallback). */
    public static MutableComponent uitleg(ServerPlayer p, Lijnen.Lijn l, String slot) {
        Poort g = POORTEN.get(l.id());
        MutableComponent kop = Component.translatable("vadsrijk.luchtschip.dicht.kop", l.letter()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.isPoortSlot(slot)) {   // X-ZONE1: the zone is closed (soon)
            return kop.append(" ").append(nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.tekst(p, slot).withStyle(ChatFormatting.WHITE));   // 1.10.2: + the per-player gate
        }
        if (g == null) {
            return kop.append(" ").append(Component.translatable(slot, Spelers.level(p)).withStyle(ChatFormatting.WHITE));
        }
        int lv = Spelers.level(p);
        if (!g.verhaalKlaar().test(p)) {
            MutableComponent m = kop.append(" ").append(Component.translatable("vadsrijk.luchtschip.dicht.verhaal", questNaam(g.naQuest()))
                    .withStyle(ChatFormatting.WHITE));
            Quest nu = huidig(p);
            if (nu != null) {
                m.append(" ").append(Component.translatable("vadsrijk.luchtschip.dicht.nu", questNaam(nu.id())).withStyle(ChatFormatting.GRAY));
            }
            if (g.minLevel() > 0 && lv < g.minLevel()) {
                m.append(" ").append(Component.translatable("vadsrijk.luchtschip.dicht.ook_level", g.minLevel(), lv).withStyle(ChatFormatting.GRAY));
            }
            return m;
        }
        if (g.minLevel() > 0 && lv < g.minLevel()) {
            return kop.append(" ").append(Component.translatable("vadsrijk.luchtschip.dicht.level", g.minLevel(), lv).withStyle(ChatFormatting.WHITE));
        }
        return kop.append(" ").append(Component.translatable(slot, lv).withStyle(ChatFormatting.WHITE));
    }

    /** Short, for the action bar: "nog dicht voor jou: na "X"" / "vanaf level 20". */
    public static MutableComponent kort(ServerPlayer p, Lijnen.Lijn l) {
        Poort g = POORTEN.get(l.id());
        String poort = dicht(p, l, true);   // X-ZONE1: the destination zone is closed
        if (nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.isPoortSlot(poort)) {
            return Component.translatable(nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.isEis(poort) ? "vadsrijk.zonepoort.eis.kort" : "vadsrijk.zonepoort.kort")
                    .withStyle(ChatFormatting.RED);
        }
        if (g != null && !g.verhaalKlaar().test(p)) {
            return Component.translatable("vadsrijk.luchtschip.kort.verhaal", questNaam(g.naQuest())).withStyle(ChatFormatting.RED);
        }
        if (g != null && g.minLevel() > 0 && Spelers.level(p) < g.minLevel()) {
            return Component.translatable("vadsrijk.luchtschip.kort.level", g.minLevel()).withStyle(ChatFormatting.RED);
        }
        return Component.translatable("vadsrijk.luchtschip.kort.dicht").withStyle(ChatFormatting.RED);
    }

    /** Tell this player why the line is closed (chat, at most every 3 s) + short in the action bar. */
    public static void meld(ServerPlayer p, Lijnen.Lijn l, String slot) {
        long nu = p.level().getServer().getTickCount();
        Long vorig = LAATST.get(p.getUUID());
        if (vorig == null || nu - vorig > 60 || nu < vorig) {
            LAATST.put(p.getUUID(), nu);
            p.sendSystemMessage(uitleg(p, l, slot));
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.pier.kop", l.letter()).withStyle(ChatFormatting.GOLD).append(" ").append(kort(p, l)));
    }

    static void vergeet(ServerPlayer p) {
        LAATST.remove(p.getUUID());
    }
}
