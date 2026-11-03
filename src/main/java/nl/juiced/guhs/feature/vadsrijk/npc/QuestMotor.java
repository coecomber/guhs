package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * What a click on an NPC guh does (quest dialogue is click-to-continue: the existing verhaal.Praat / knuffeldal PraatScherm).
 * In this order: an escort starts following; a "talk to" objective; a quest to hand in; a quest to offer; "how is it going";
 * and otherwise the NPC's role (a shop, the airship...) or a little chatter.
 */
public final class QuestMotor {
    public static final String SLEUTEL = "vr_npc";
    static final int AANNEMEN = 1, LATER = 2, ROL = 10, VLIEG_STAD = 30, VLIEG_KAMP = 31, KLASSE = 40;

    /** What the open conversation is about: the NPC, a quest to accept, an action to run after reading (na). */
    record Gesprek(String npc, int entity, String quest, String rol, String na) {
    }

    private static final Map<UUID, Gesprek> LOPEND = new ConcurrentHashMap<>();

    private QuestMotor() {
    }

    static void init() {
        Praat.luister(SLEUTEL, QuestMotor::antwoord);
    }

    public static void klik(ServerPlayer p, RijkGuh npc) {
        afronden(p);
        String id = npc.npcId();
        if (id.isEmpty()) {
            return;
        }
        if (p.getUUID().equals(npc.volgt())) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.npc.escorte.volgt", npc.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        List<Praat.Regel> regels = new ArrayList<>();
        // 1. an escort: this NPC starts following you
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == Quest.Doel.Type.ESCORTE && d.doel().equals(id) && !Questlog.doelKlaar(p, q, i)) {
                    npc.volg(p);
                    regels.addAll(regels(p, npc, q.praat().getOrDefault(id, List.of("npc")), q.sleutel() + ".praat." + id + "."));
                    open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""));
                    return;
                }
            }
        }
        // 2. "talk to me" objectives
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == Quest.Doel.Type.PRAAT && d.doel().equals(id) && !Questlog.doelKlaar(p, q, i)) {
                    Questlog.tel(p, Quest.Doel.Type.PRAAT, x -> x == d, 1);
                    List<String> sprekers = q.praat().get(id);
                    if (sprekers != null && !q.inleveren().equals(id)) {
                        regels.addAll(regels(p, npc, sprekers, q.sleutel() + ".praat." + id + "."));
                    }
                }
            }
        }
        // 3. a quest to hand in here
        for (Quest q : Questlog.actieve(p)) {
            if (q.inleveren().equals(id) && Questlog.allesKlaar(p, q)) {
                Questlog.voltooi(p, q);
                regels.addAll(regels(p, npc, q.klaar(), q.sleutel() + ".klaar."));
                open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", q.actieKlaar()));
                return;
            }
        }
        if (!regels.isEmpty()) {
            open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""));
            return;
        }
        // 4. a quest to offer
        for (Quest q : Quests.alle().values()) {
            if (q.gever().equals(id) && !q.auto() && Questlog.beschikbaar(p, q)) {
                regels.addAll(regels(p, npc, q.aanbod(), q.sleutel() + ".aanbod."));
                if (q.hoofd()) {
                    open(p, npc, regels, new Gesprek(id, npc.getId(), q.id(), "", ""), new Praat.Optie(AANNEMEN, "vadsrijk.npc.optie.hoofd"));
                } else {
                    open(p, npc, regels, new Gesprek(id, npc.getId(), q.id(), "", ""), new Praat.Optie(AANNEMEN, "vadsrijk.npc.optie.ja"),
                            new Praat.Optie(LATER, "vadsrijk.npc.optie.later"));
                }
                return;
            }
        }
        // 5. how is it going
        for (Quest q : Questlog.actieve(p)) {
            if (q.gever().equals(id) || q.inleveren().equals(id)) {
                regels.add(new Praat.Regel(npc, "", q.sleutel() + ".bezig"));
                open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""));
                return;
            }
        }
        // 6. the role, or a little chatter
        NpcDef def = Npcs.van(id);
        rol(p, npc, def == null ? null : def.rol());
    }

    private static void open(ServerPlayer p, Entity npc, List<Praat.Regel> regels, Gesprek g, Praat.Optie... opties) {
        LOPEND.put(p.getUUID(), g);
        Praat.scene(p, SLEUTEL, regels, opties);
    }

    /** One Praat page per speaker in the list: "npc" = this NPC, "jij" = the player, else that NPC (drawn when it is near). */
    static List<Praat.Regel> regels(ServerPlayer p, @Nullable Entity npc, List<String> sprekers, String prefix) {
        List<Praat.Regel> l = new ArrayList<>();
        for (int i = 0; i < sprekers.size(); i++) {
            String s = sprekers.get(i);
            String key = prefix + i;
            if (s.equals("npc")) {
                l.add(new Praat.Regel(npc, "", key));
            } else if (s.equals("jij")) {
                l.add(new Praat.Regel(null, "vadsrijk.npc.jij", key));
            } else {
                RijkGuh ander = p.level() instanceof ServerLevel sl ? Npcs.levend(sl, s) : null;
                NpcDef d = Npcs.van(s);
                boolean bij = ander != null && ander.distanceTo(p) < 16;
                l.add(new Praat.Regel(bij ? ander : null, d == null ? "vadsrijk.npc.verteller" : d.naamKey(), key));
            }
        }
        return l;
    }

    // ---- roles -------------------------------------------------------------------------------------------------------------------

    private static void rol(ServerPlayer p, RijkGuh npc, @Nullable String rol) {
        String id = npc.npcId();
        String gedag = "vadsrijk.npc.praat." + id + "." + (1 + p.getRandom().nextInt(3));
        if ("marktguh".equals(rol) && klaarVoorOntmaskeren(p)) {
            // (our fallback Akim, when the bazen slice is not in this tree: the unmasking dialogue, then the action)
            open(p, npc, regels(p, npc, List.of("jij", "npc", "npc", "jij", "npc"), "vadsrijk.npc.spion.dialoog."),
                    new Gesprek(id, npc.getId(), "", "", "ontmasker"));
            return;
        }
        if (rol == null || rol.equals("training") || rol.equals("kampwacht") || rol.equals("marktguh")) {
            praat(p, npc, gedag, new Gesprek(id, npc.getId(), "", "", ""));
            return;
        }
        switch (rol) {
            case "luchtschip" -> {
                if (!Questlog.klaar(p, "hq05_klodderreus")) {
                    praat(p, npc, "vadsrijk.npc.rol.luchtschip.nog_niet", new Gesprek(id, npc.getId(), "", "", ""));
                } else {
                    praat(p, npc, "vadsrijk.npc.rol.luchtschip.vraag", new Gesprek(id, npc.getId(), "", rol, ""),
                            new Praat.Optie(VLIEG_STAD, "vadsrijk.npc.optie.naar_stad"));
                }
            }
            case "luchtschip_kamp" -> praat(p, npc, "vadsrijk.npc.rol.luchtschip_kamp.vraag", new Gesprek(id, npc.getId(), "", rol, ""),
                    new Praat.Optie(VLIEG_KAMP, "vadsrijk.npc.optie.naar_kamp"));
            case "klasse" -> klasseKeuze(p, npc);
            case "questbord" -> questbord(p, npc);
            default -> praat(p, npc, gedag, new Gesprek(id, npc.getId(), "", rol, ""), new Praat.Optie(ROL, "vadsrijk.npc.optie.rol." + rol));
        }
    }

    static boolean klaarVoorOntmaskeren(ServerPlayer p) {
        Quest q = Quests.van("geheim_spion");
        return q != null && Questlog.actief(p, q.id()) && Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1);
    }

    private static void praat(ServerPlayer p, Entity npc, String key, Gesprek g, Praat.Optie... opties) {
        open(p, npc, List.of(new Praat.Regel(npc, "", key)), g, opties);
    }

    /** The klasse-tempeltje: the klassen slice's screen when it is there, else a choice between the registered classes. */
    static void klasseKeuze(ServerPlayer p, Entity npc) {
        if (NpcRollen.bestaat("klasse_kiezen")) {
            Praat.sluit(p);
            NpcRollen.doe("klasse_kiezen", p);   // (the pick itself is seen by NpcHaken.klasseGekozen: extra("klassen").gekozen)
            return;
        }
        List<Praat.Optie> opties = new ArrayList<>();
        List<Klasse> alle = new ArrayList<>(Klassen.alle());
        for (int i = 0; i < alle.size() && i < 9; i++) {
            opties.add(new Praat.Optie(KLASSE + i, alle.get(i).naamKey()));
        }
        open(p, npc, List.of(new Praat.Regel(npc, "", "vadsrijk.npc.rol.klasse.kies")), new Gesprek("priesteres", npc.getId(), "", "klasse", ""),
                opties.toArray(new Praat.Optie[0]));
    }

    static void kiesKlasse(ServerPlayer p, String klasse) {
        if (Klassen.van(klasse) == null) {
            return;
        }
        Spelers.data(p).klasse = klasse;
        Spelers.data(p).vads = -1;
        Spelers.sync(p);
        RijkScenes.titel(p, Component.translatable(Klassen.van(klasse).naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("vadsrijk.npc.klasse.gekozen"), 60, 0xFF8AD0);
        NpcApi.meld(p, "klasse", klasse);
        Tips.toon(p, "vadsrijk.npc.tip.skill");
    }

    private static void questbord(ServerPlayer p, RijkGuh npc) {
        MutableComponent lijst = Component.empty();
        int n = 0;
        for (Quest q : Quests.alle().values()) {
            if (q.soort() == Quest.Soort.ZIJ && !q.auto() && Questlog.beschikbaar(p, q) && n < 8) {
                NpcDef d = Npcs.van(q.gever());
                lijst.append(Component.literal("\n• ")).append(Component.translatable(q.titelKey()))
                        .append(Component.literal(" (")).append(d == null ? Component.literal("?") : Component.translatable(d.naamKey())).append(")");
                n++;
            }
        }
        String key = n == 0 ? "vadsrijk.npc.rol.questbord.leeg" : "vadsrijk.npc.rol.questbord.lijst";
        LOPEND.put(p.getUUID(), new Gesprek(npc.npcId(), npc.getId(), "", "", ""));
        Praat.open(p, npc, SLEUTEL, key, new Object[] {lijst});
    }

    // ---- answers ------------------------------------------------------------------------------------------------------------------

    private static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        Gesprek g = LOPEND.get(p.getUUID());
        if (g == null) {
            return;
        }
        if (optie < 0) {
            LOPEND.remove(p.getUUID());
            if (!g.na().isEmpty()) {
                doeActie(p, g.na(), spreker);
            }
            return;
        }
        if (optie == AANNEMEN) {
            Quest q = Quests.van(g.quest());
            LOPEND.put(p.getUUID(), new Gesprek(g.npc(), g.entity(), "", "", g.na()));
            if (q != null && Questlog.beschikbaar(p, q)) {
                Questlog.neemAan(p, q);
                startEscorte(p, q, spreker);
                Praat.sluit(p);
                afronden(p);
                if (!q.actieAanname().isEmpty()) {
                    doeActie(p, q.actieAanname(), spreker);
                }
            }
        } else if (optie == LATER) {
            Praat.sluit(p);
            afronden(p);
        } else if (optie == ROL) {
            Praat.sluit(p);
            afronden(p);
            if (!NpcRollen.doe(g.rol(), p)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.npc.rol.nog_niet", Component.translatable("vadsrijk.npc.optie.rol." + g.rol()))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else if (optie == VLIEG_STAD || optie == VLIEG_KAMP) {
            Praat.sluit(p);
            afronden(p);
            Verhaal.vlieg(p, optie == VLIEG_STAD ? "zwamzoet" : "knuffelkamp");
        } else if (optie >= KLASSE && optie < KLASSE + 9) {
            List<Klasse> alle = new ArrayList<>(Klassen.alle());
            Praat.sluit(p);
            afronden(p);
            if (optie - KLASSE < alle.size()) {
                kiesKlasse(p, alle.get(optie - KLASSE).id());
            }
        }
    }

    /** Runs a pending after-reading action (the screen was closed without telling us) and forgets the conversation. */
    static void afronden(ServerPlayer p) {
        Gesprek g = LOPEND.remove(p.getUUID());
        if (g != null && !g.na().isEmpty()) {
            doeActie(p, g.na(), null);
        }
    }

    static void startEscorte(ServerPlayer p, Quest q, @Nullable Entity spreker) {
        for (Quest.Doel d : q.doelen()) {
            if (d.type() == Quest.Doel.Type.ESCORTE && spreker instanceof RijkGuh g && g.npcId().equals(d.doel())) {
                g.volg(p);
            }
        }
    }

    /** The quest actions (after accepting: actie_aanname; after the hand-in dialogue: actie_klaar). */
    static void doeActie(ServerPlayer p, String actie, @Nullable Entity spreker) {
        if (actie.isEmpty()) {
            return;
        }
        String[] a = actie.split(":");
        switch (a[0]) {
            case "klasse" -> klasseKeuze(p, spreker != null ? spreker : p);
            case "bam" -> Verhaal.bam(p);
            case "kampklodder" -> NpcRollen.doe("kampklodder", p);
            case "luchtschip" -> Verhaal.vlieg(p, "zwamzoet");
            case "cliffhanger" -> Verhaal.cliffhanger(p);
            case "ontmasker" -> Verhaal.ontmasker(p);
            case "rijdier" -> {
                if (!NpcRollen.doe("rijdier", p, a.length > 1 ? a[1] : "hoedhupper")) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.npc.rijdier.later").withStyle(ChatFormatting.GRAY));
                }
            }
            case "advancement" -> Verhaal.advancement(p, a.length > 1 ? a[1] : Spelers.data(p).klasse, a.length > 2 ? Integer.parseInt(a[2]) : 1);
            case "start" -> {
                Quest q = a.length > 1 ? Quests.van(a[1]) : null;
                if (q != null && Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
            }
            default -> {
            }
        }
    }

    /** (tests) */
    static void vergeet(ServerPlayer p) {
        LOPEND.remove(p.getUUID());
    }
}
