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
    static final int AANNEMEN = 1, LATER = 2, ROL = 10, VLIEG_STAD = 30, VLIEG_KAMP = 31, KLASSE = 40, QUIZ = 50, REIS = 80, REIS_KAART = 79;
    /** The element quiz (hq03b): the questions and the answer that wins (0 vuur, 1 zwam, 2 slijm). */
    static final String[] QUIZ_VRAAG = {"zwam", "slijm", "vuur"};
    static final int[] QUIZ_JUIST = {0, 1, 2};
    static final String[] QUIZ_OPTIE = {"vuur", "zwam", "slijm"};

    /** What the open conversation is about: the NPC, a quest to accept, an action to run after reading (na). */
    record Gesprek(String npc, int entity, String quest, String rol, String na) {
    }

    private static final Map<UUID, Gesprek> LOPEND = new ConcurrentHashMap<>();

    // ---- zone 2 (and later zones) hook in here without a parallel quest system --------------------------------------------------
    /** Quest actions of other zones: "z2_cliffhanger" -> (player, the rest after the first ':'). Looked up for unknown actions. */
    public static final Map<String, java.util.function.BiConsumer<ServerPlayer, String>> ACTIES = new ConcurrentHashMap<>();
    /** Asked first on every NPC click; true = handled (e.g. a petrified guh you already freed thanks you). */
    public static final List<java.util.function.BiPredicate<ServerPlayer, RijkGuh>> KLIK_VOOR = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** An extra choice in an NPC's role dialogue (zone 2: the haven pilot's line to Kandijkom), shown while {@code zichtbaar}. */
    public record ExtraOptie(String tekstKey, java.util.function.Predicate<ServerPlayer> zichtbaar, java.util.function.Consumer<ServerPlayer> doe) {
    }

    /** npc id -> its extra choices. */
    public static final Map<String, List<ExtraOptie>> EXTRA = new ConcurrentHashMap<>();
    static final int EXTRA_OPTIE = 70;
    private static final Map<UUID, List<ExtraOptie>> EXTRA_OPEN = new ConcurrentHashMap<>();

    /** The base choices plus the extra ones of this NPC this player may see now (remembered for the answer). */
    private static Praat.Optie[] metExtra(ServerPlayer p, String npc, Praat.Optie... basis) {
        List<ExtraOptie> zicht = new ArrayList<>();
        for (ExtraOptie e : EXTRA.getOrDefault(npc, List.of())) {
            if (e.zichtbaar().test(p)) {
                zicht.add(e);
            }
        }
        EXTRA_OPEN.put(p.getUUID(), zicht);
        List<Praat.Optie> l = new ArrayList<>(List.of(basis));
        for (int i = 0; i < zicht.size(); i++) {
            l.add(new Praat.Optie(EXTRA_OPTIE + i, zicht.get(i).tekstKey()));
        }
        return l.toArray(new Praat.Optie[0]);
    }

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
        for (java.util.function.BiPredicate<ServerPlayer, RijkGuh> h : KLIK_VOOR) {
            if (h.test(p, npc)) {
                return;
            }
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
        // 1b. the element quiz of Opa Sterrenstof (hq03b): a question with three answers
        Quest quiz = Quests.van("hq03b_elementen");
        if (quiz != null && id.equals(quiz.gever()) && Questlog.actief(p, quiz.id()) && !Questlog.doelKlaar(p, quiz, 0)) {
            quiz(p, npc, false);
            return;
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
                String na = q.actieKlaar();
                if (na.startsWith("start:")) {   // (the next quest starts at once, so the tracker and the chat line are right)
                    doeActie(p, na, npc);
                    na = "";
                }
                NpcVolg.meldVolgende(p, q);
                regels.addAll(regels(p, npc, q.klaar(), q.sleutel() + ".klaar."));
                open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", na), na.isEmpty() ? reisOpties(p, id) : new Praat.Optie[0]);
                return;
            }
        }
        if (!regels.isEmpty()) {
            open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""), reisOpties(p, id));
            return;
        }
        // 4. a quest to offer
        for (Quest q : Quests.alle().values()) {
            // (an auto main quest whose start was missed, e.g. a log-out during the BAM, is offered by its giver: no soft-lock)
            if (q.gever().equals(id) && (!q.auto() || q.hoofd()) && Questlog.beschikbaar(p, q)) {
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
        // 5. how is it going (fix round 2: plus what the NPC's line promises: the airship's destinations, the class choice, its shop)
        NpcDef eigen = Npcs.van(id);
        String eigenRol = eigen == null ? null : eigen.rol();
        for (Quest q : Questlog.actieve(p)) {
            if (q.gever().equals(id) || q.inleveren().equals(id)) {
                if ("klasse".equals(eigenRol) && heeftOpen(p, q, Quest.Doel.Type.KLASSE)) {
                    klasseKeuze(p, npc);   // ("click me again to see the choice")
                    return;
                }
                regels.add(new Praat.Regel(npc, "", q.sleutel() + ".bezig"));
                QuestStap s = QuestStap.van(p, q);
                regels.add(new Praat.Regel(npc, "", QuestStap.P + "hint", s.wat(), s.waar()));
                Praat.Optie[] reis = reisOpties(p, id);
                if (reis.length > 0) {   // (an airship NPC: the real choices, and the bell as the alternative)
                    regels.add(new Praat.Regel(npc, "", "vadsrijk.npc.reis.kies." + id));
                    open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""), reis);
                } else if (dienst(eigenRol)) {   // (a shop/service NPC keeps its service choice during a quest)
                    open(p, npc, regels, new Gesprek(id, npc.getId(), "", eigenRol, ""),
                            metExtra(p, id, new Praat.Optie(ROL, "vadsrijk.npc.optie.rol." + eigenRol)));
                } else {
                    open(p, npc, regels, new Gesprek(id, npc.getId(), "", "", ""));
                }
                return;
            }
        }
        // 6. the role, or a little chatter
        NpcDef def = Npcs.van(id);
        rol(p, npc, def == null ? null : def.rol());
    }

    /** A role with its own "do it" choice (a shop, the bank...), not chatter, the class temple, the airship or the board. */
    static boolean dienst(@Nullable String rol) {
        return rol != null && !List.of("training", "kampwacht", "marktguh", "klasse", "questbord", "versteend", "luchtschip", "luchtschip_kamp", "luchtschip_z2")
                .contains(rol);
    }

    static boolean heeftOpen(ServerPlayer p, Quest q, Quest.Doel.Type t) {
        for (int i = 0; i < q.doelen().size(); i++) {
            if (q.doelen().get(i).type() == t && !Questlog.doelKlaar(p, q, i)) {
                return true;
            }
        }
        return false;
    }

    static void open(ServerPlayer p, @Nullable Entity npc, List<Praat.Regel> regels, Gesprek g, Praat.Optie... opties) {
        LOPEND.put(p.getUUID(), g);
        Praat.scene(p, SLEUTEL, regels, opties);
    }

    /** One Praat page per speaker in the list: "npc" = this NPC, "jij" = the player, else that NPC (drawn when it is near). */
    public static List<Praat.Regel> regels(ServerPlayer p, @Nullable Entity npc, List<String> sprekers, String prefix) {
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
        Praat.Regel hint = hint(p, id, npc);
        if (rol == null || rol.equals("training") || rol.equals("kampwacht") || rol.equals("marktguh")) {
            // (g-int: + the extra choices of this NPC, e.g. the trainers' starter weapons)
            if (hint != null) {
                open(p, npc, List.of(hint), new Gesprek(id, npc.getId(), "", "", ""), metExtra(p, id));
            } else {
                praat(p, npc, gedag, new Gesprek(id, npc.getId(), "", "", ""), metExtra(p, id));
            }
            return;
        }
        switch (rol) {
            case "luchtschip", "luchtschip_kamp", "luchtschip_z2" -> {
                if (hint == null) {
                    Reiskaart.open(p);   // (the travel map: every place, flying with one click)
                    return;
                }
                List<Praat.Regel> l = new ArrayList<>();
                l.add(hint);
                l.add(new Praat.Regel(npc, "", "vadsrijk.npc.reis.kies." + id));
                open(p, npc, l, new Gesprek(id, npc.getId(), "", rol, ""), reisOpties(p, id));
            }
            case "klasse" -> klasseKeuze(p, npc);
            case "questbord" -> questbord(p, npc);
            default -> {
                List<Praat.Regel> l = new ArrayList<>();
                if (hint != null) {
                    l.add(hint);
                }
                l.add(new Praat.Regel(npc, "", gedag));
                open(p, npc, l, new Gesprek(id, npc.getId(), "", rol, ""), metExtra(p, id, new Praat.Optie(ROL, "vadsrijk.npc.optie.rol." + rol)));
            }
        }
    }

    /**
     * Re-talking to whoever sent you on (the one you last handed a main quest in to) repeats where to go next, with who and
     * where; null when this NPC has nothing to remind you of.
     */
    @Nullable
    static Praat.Regel hint(ServerPlayer p, String id, Entity npc) {
        List<Quest> gedaan = QuestStap.hoofdGedaan(p);
        QuestStap s = QuestStap.hoofdlijn(p);
        if (gedaan.isEmpty() || s == null || !gedaan.get(gedaan.size() - 1).inleveren().equals(id)) {
            return null;
        }
        return new Praat.Regel(npc, "", QuestStap.P + "hint", s.wat(), s.waar());
    }

    /** One quiz question (a random one); after a wrong answer first a friendly hint. */
    static void quiz(ServerPlayer p, RijkGuh npc, boolean fout) {
        int v = p.getRandom().nextInt(QUIZ_VRAAG.length);
        List<Praat.Regel> l = new ArrayList<>();
        if (fout) {
            l.add(new Praat.Regel(npc, "", "vadsrijk.npc.quiz.fout"));
        }
        l.add(new Praat.Regel(npc, "", "vadsrijk.npc.quiz.vraag." + QUIZ_VRAAG[v]));
        Praat.Optie[] o = new Praat.Optie[QUIZ_OPTIE.length];
        for (int i = 0; i < o.length; i++) {
            o[i] = new Praat.Optie(QUIZ + i, "vadsrijk.npc.quiz." + QUIZ_OPTIE[i]);
        }
        open(p, npc, l, new Gesprek(npc.npcId(), npc.getId(), "", "quiz:" + QUIZ_JUIST[v], ""), o);
    }

    static boolean klaarVoorOntmaskeren(ServerPlayer p) {
        Quest q = Quests.van("geheim_spion");
        return q != null && Questlog.actief(p, q.id()) && Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1);
    }

    private static void praat(ServerPlayer p, Entity npc, String key, Gesprek g, Praat.Optie... opties) {
        open(p, npc, List.of(new Praat.Regel(npc, "", key)), g, opties);
    }

    // ---- the airship NPCs: a real choice per destination (fix round 2) -----------------------------------------------------------

    /** Airship NPC id -> the destinations it offers, in order ("zwamzoet" = De Haven, "knuffelkamp", "kandijkom"). */
    public static final Map<String, List<String>> REIZEN = new ConcurrentHashMap<>(Map.of(
            "kapitein", List.of("zwamzoet", "kandijkom"),
            "luchtschipper", List.of("knuffelkamp", "kandijkom"),
            "z2_kapitein", List.of("zwamzoet", "knuffelkamp")));
    private static final Map<UUID, List<String>> REIS_OPEN = new ConcurrentHashMap<>();

    /** Is this an airship NPC? */
    public static boolean reisNpc(String npc) {
        return REIZEN.containsKey(npc);
    }

    /** The destinations this player sees at this NPC now (Kandijkom only once zone 1 is done; a locked one is still shown). */
    public static List<String> reisDoelen(ServerPlayer p, String npc) {
        List<String> l = new ArrayList<>();
        for (String naar : REIZEN.getOrDefault(npc, List.of())) {
            if (!naar.equals("kandijkom") || NpcApi.zone1Klaar(p)) {
                l.add(naar);
            }
        }
        return l;
    }

    /** Null when the line is open for this player, else the lang key of why not (arg: the player's level). */
    @Nullable
    public static String reisSlot(ServerPlayer p, String naar) {
        return switch (naar) {
            case "zwamzoet" -> Questlog.klaar(p, "hq05_klodderreus") || Questlog.actief(p, "hq06_zwamzoet") ? null : "vadsrijk.npc.reis.slot.zwamzoet";
            case "kandijkom" -> Questlog.status(p, "z2_hq01_splinterpieken") != 0 ? null   // (the zone-2 story started: always open)
                    : !NpcApi.zone1Klaar(p) ? "vadsrijk.npc.reis.slot.kandijkom_verhaal"
                    : Spelers.level(p) < 20 ? "vadsrijk.npc.reis.slot.kandijkom_level" : null;
            default -> null;
        };
    }

    /** The travel choices of an airship NPC (none for any other NPC); remembered for the answer. */
    static Praat.Optie[] reisOpties(ServerPlayer p, String npc) {
        if (!reisNpc(npc)) {
            return new Praat.Optie[0];
        }
        List<String> doelen = reisDoelen(p, npc);
        REIS_OPEN.put(p.getUUID(), doelen);
        Praat.Optie[] o = new Praat.Optie[doelen.size() + 1];
        o[0] = new Praat.Optie(REIS_KAART, "vadsrijk.npc.optie.reiskaart");
        for (int i = 0; i < doelen.size(); i++) {
            String naar = doelen.get(i);
            o[i + 1] = new Praat.Optie(REIS + i, "vadsrijk.npc.optie.vlieg." + naar + (reisSlot(p, naar) != null ? ".slot" : ""));
        }
        return o;
    }

    /** (tests) When set, a chosen flight is reported here ("npc>destination") instead of flown. */
    @Nullable
    static java.util.function.BiConsumer<ServerPlayer, String> VLIEG_TEST;

    /** Fly from this NPC's dock to {@code naar} (the same flights as the bells: WereldReizen / Z2Reizen through the bindings). */
    public static void vliegNaar(ServerPlayer p, String van, String naar) {
        if (VLIEG_TEST != null) {
            VLIEG_TEST.accept(p, van + ">" + naar);
            return;
        }
        switch (naar) {
            case "kandijkom" -> {
                Quest q = Quests.van("z2_hq01_splinterpieken");
                if (q != null && Questlog.beschikbaar(p, q)) {
                    Questlog.neemAan(p, q);
                }
                doeActie(p, "z2_vlieg:kandijkom", null);
            }
            case "zwamzoet" -> {
                if (van.equals("z2_kapitein") || QuestStap.zone2(p.getZ())) {
                    doeActie(p, "z2_vlieg:haven", null);
                } else {
                    Verhaal.vlieg(p, "zwamzoet");
                }
            }
            default -> Verhaal.vlieg(p, "knuffelkamp");
        }
    }

    /** (tests) Answer the open dialogue as if the player clicked choice {@code optie} (0 = the first choice shown). */
    public static void kiesVoorTest(ServerPlayer p, int optie) {
        antwoord(p, null, optie);
    }

    /** (tests) The open travel choices of this player (destination ids, in order) and their first option id. */
    public static List<String> reisOpen(ServerPlayer p) {
        return REIS_OPEN.getOrDefault(p.getUUID(), List.of());
    }

    public static final int REIS_OPTIE = REIS;

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
        } else if (optie >= QUIZ && optie < QUIZ + 3) {
            Praat.sluit(p);
            afronden(p);
            RijkGuh npc = p.level().getEntity(g.entity()) instanceof RijkGuh r ? r : null;
            if (g.rol().equals("quiz:" + (optie - QUIZ))) {
                NpcApi.meld(p, "quiz", "elementen");
                Tips.toon(p, "vadsrijk.npc.tip.elementen");
                if (npc != null) {
                    klik(p, npc);   // (straight to the hand-in)
                }
            } else if (npc != null) {
                quiz(p, npc, true);
            }
        } else if (optie == REIS_KAART) {
            Praat.sluit(p);
            afronden(p);
            Reiskaart.open(p);
        } else if (optie >= REIS && optie < REIS + 5) {
            List<String> l = REIS_OPEN.remove(p.getUUID());
            if (l != null && optie - REIS < l.size()) {
                String naar = l.get(optie - REIS);
                String slot = reisSlot(p, naar);
                if (slot != null) {   // (a locked line: the captain says why)
                    RijkGuh npc = p.level().getEntity(g.entity()) instanceof RijkGuh r ? r : null;
                    LOPEND.put(p.getUUID(), new Gesprek(g.npc(), g.entity(), "", "", ""));
                    Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(npc, "", slot, Spelers.level(p))));
                    return;
                }
                Praat.sluit(p);
                afronden(p);
                vliegNaar(p, g.npc(), naar);
            }
        } else if (optie >= EXTRA_OPTIE && optie < EXTRA_OPTIE + 10) {
            List<ExtraOptie> l = EXTRA_OPEN.remove(p.getUUID());
            Praat.sluit(p);
            afronden(p);
            if (l != null && optie - EXTRA_OPTIE < l.size()) {
                l.get(optie - EXTRA_OPTIE).doe().accept(p);
            }
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
    public static void afronden(ServerPlayer p) {
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
    public static void doeActie(ServerPlayer p, String actie, @Nullable Entity spreker) {
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
                if (!NpcRollen.doe("rijdier", p, a.length > 1 ? a[1] : "wolkenschaapje")) {
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
                java.util.function.BiConsumer<ServerPlayer, String> x = ACTIES.get(a[0]);
                if (x != null) {
                    x.accept(p, actie.indexOf(':') < 0 ? "" : actie.substring(actie.indexOf(':') + 1));
                }
            }
        }
    }

    /** (tests) */
    public static void vergeet(ServerPlayer p) {
        LOPEND.remove(p.getUUID());
    }
}
