package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * "What now?" (fix round 1): the current step of a quest, or of the whole main story, always with WHAT, WHO and WHERE and a
 * spot to point at. Used by the quest tracker (top right + the light beam), the questlog (J), the hand-in message and the
 * NPCs' "remember?" hint. Place texts: {@code vadsrijk.npc.plek.*} (tools/features/vadsrijk_npc.py, PLEK_*).
 *
 * @param wat   what to do ("Praat met Kapitein Wolkewiet", "Vrolijk klodders op (3/8)")
 * @param waar  where, a full phrase ("bij zijn luchtschip op de oostelijke steiger van Knuffelkamp")
 * @param doel  the spot the tracker/beam points at (null = nowhere in particular, e.g. "anywhere in the Vadsrijk")
 * @param extra an optional extra line (a recommended level)
 */
public record QuestStap(String quest, boolean hoofd, Component titel, Component wat, Component waar, @Nullable BlockPos doel, @Nullable Component extra) {
    public static final String P = "vadsrijk.npc.stap.";
    public static final String PLEK = "vadsrijk.npc.plek.";

    /** The current step of the main story; null when the main story of zone 1 is done. */
    @Nullable
    public static QuestStap hoofdlijn(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (!q.hoofd() || Questlog.klaar(p, q.id())) {
                continue;
            }
            return van(p, q);
        }
        return null;
    }

    /** The main quests already done, in story order (the questlog's history). */
    public static List<Quest> hoofdGedaan(ServerPlayer p) {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && Questlog.klaar(p, q.id())) {
                l.add(q);
            }
        }
        return l;
    }

    /** The step of one quest: active = its first open objective (or the hand-in), not taken yet = go to the giver. */
    public static QuestStap van(ServerPlayer p, Quest q) {
        Component titel = Component.translatable(q.titelKey());
        int lv = Spelers.level(p);
        if (Questlog.actief(p, q.id())) {
            Component extra = q.lv() > lv ? Component.translatable(P + "aanrader", q.lv(), lv) : null;
            if (Questlog.allesKlaar(p, q)) {
                return reis(p, new QuestStap(q.id(), q.hoofd(), titel, Component.translatable(P + "inleveren", naam(q.inleveren())), npcPlek(q.inleveren()),
                        npcPos(p, q.inleveren()), extra));
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                if (!Questlog.doelKlaar(p, q, i)) {
                    return reis(p, doel(p, q, i, titel, extra));
                }
            }
        }
        if (Questlog.klaar(p, q.id())) {
            return new QuestStap(q.id(), q.hoofd(), titel, Component.translatable(P + "af"), Component.empty(), null, null);
        }
        // not taken yet: go to the giver (first the level, when it is too low)
        if (lv < q.lv()) {
            return reis(p, new QuestStap(q.id(), q.hoofd(), titel, Component.translatable(P + "level", q.lv(), lv),
                    Component.translatable(P + "daarna", naam(q.gever()), npcPlek(q.gever())), npcPos(p, q.gever()),
                    Component.translatable(P + "level.tip", levelPlek(q.lv()))));
        }
        return reis(p, new QuestStap(q.id(), q.hoofd(), titel, Component.translatable(P + "praat", naam(q.gever())), npcPlek(q.gever()),
                npcPos(p, q.gever()), null));
    }

    private static QuestStap doel(ServerPlayer p, Quest q, int i, Component titel, @Nullable Component extra) {
        Quest.Doel d = q.doelen().get(i);
        MutableComponent wat = Component.translatable(q.doelKey(i)).copy();
        if (d.n() > 1) {
            wat.append(Component.literal(" (" + Questlog.waarde(p, q, i) + "/" + d.n() + ")"));
        }
        Component waar;
        BlockPos pos;
        // (integration, zone 2) an explicit target on the objective wins (QuestPlek: "npc:"/"poi:"/"gebied:"/"baas:")
        if (d.plek() != null && !d.plek().isEmpty()) {
            BlockPos b = QuestPlek.pos(p.level() instanceof ServerLevel sl ? sl : null, d.plek());
            if (b != null) {
                return new QuestStap(q.id(), q.hoofd(), titel, wat, Component.translatable(QuestPlek.tekstKey(d.plek())), b, extra);
            }
        }
        switch (d.type()) {
            case PRAAT -> {
                waar = npcPlek(d.doel());
                pos = npcPos(p, d.doel());
            }
            case ESCORTE -> {
                RijkGuh g = p.level() instanceof ServerLevel sl ? Npcs.levend(sl, d.doel()) : null;
                String poi = d.bron().startsWith("poi:") ? d.bron().substring(4) : d.bron();
                if (g != null && p.getUUID().equals(g.volgt())) {
                    waar = poiPlek(poi);
                    pos = NpcPlekken.poi(poi);
                } else {
                    waar = Component.translatable(P + "escorte.ophalen", naam(d.doel()), npcPlek(d.doel()));
                    pos = npcPos(p, d.doel());
                }
            }
            case GEBIED, GEHEIM -> {
                if (d.doel().startsWith("poi:")) {
                    waar = poiPlek(d.doel().substring(4));
                    pos = NpcPlekken.poi(d.doel().substring(4));
                } else if (d.type() == Quest.Doel.Type.GEBIED) {
                    String g = d.doel().split("\\|")[0];
                    waar = gebiedPlek(g);
                    pos = NpcPlekken.GEBIED_DOEL.get(g);
                } else {
                    waar = Component.translatable(PLEK + "overal." + d.doel());
                    pos = null;
                }
            }
            case KAMPVUUR -> {
                waar = d.doel().equals("*") ? Component.translatable(PLEK + "overal") : Component.translatable(PLEK + "kampvuur." + d.doel());
                pos = NpcPlekken.KAMPVUUR.get(d.doel());
            }
            case OPVROLIJK, ELITE -> {
                String g = gebied(d.doel());
                waar = g == null ? Component.translatable(PLEK + "overal") : gebiedPlek(g);
                pos = g == null ? null : NpcPlekken.GEBIED_DOEL.get(g);
            }
            case VERZAMEL -> {
                String g = gebied(d.bron());
                waar = g == null ? Component.translatable(PLEK + "overal") : gebiedPlek(g);
                pos = g == null ? null : NpcPlekken.GEBIED_DOEL.get(g);
            }
            case BAAS -> {
                String b = d.doel().startsWith("gebied:") ? d.doel().substring(7) : d.doel().substring(d.doel().indexOf(':') + 1);
                waar = Component.translatable(PLEK + "baas." + b);
                pos = NpcPlekken.BAAS.get(b);
            }
            case KLASSE -> {
                waar = npcPlek("priesteres");
                pos = npcPos(p, "priesteres");
            }
            case POP -> {
                waar = Component.translatable(PLEK + "training");
                pos = NpcPlekken.TRAINING;
            }
            case LEVEL -> {
                waar = Component.translatable(P + "level.tip", levelPlek(d.n()));
                pos = null;
            }
            default -> {   // MELD
                if (d.doel().equals("spion:ontmaskerd")) {
                    waar = npcPlek("akim_kaasguh");
                    pos = npcPos(p, "akim_kaasguh");
                } else if (d.doel().startsWith("quiz:")) {   // (the element quiz: at the quest giver)
                    waar = npcPlek(q.gever());
                    pos = npcPos(p, q.gever());
                } else if (d.doel().equals("spion:gevangen")) {
                    waar = Component.translatable(PLEK + "daken");
                    pos = NpcPlekken.MIKA_KRAAM;
                } else {
                    waar = Component.translatable(PLEK + "overal");
                    pos = null;
                }
            }
        }
        return new QuestStap(q.id(), q.hoofd(), titel, wat, waar, pos, extra);
    }

    /** The target is on the other side of the sea (camp island vs. mainland): first the airship. */
    private static QuestStap reis(ServerPlayer p, QuestStap s) {
        if (s.doel() == null || !Questlog.klaar(p, "hq05_klodderreus")) {
            return s;
        }
        // (integration) zone 2 (De Splinterpieken, z < -1700): the airship line Haven <-> Kandijkom
        boolean ikZone2 = zone2(p.getZ()), doelZone2 = zone2(s.doel().getZ());
        if (ikZone2 != doelZone2 && !(doelZone2 && eiland(p.getX(), p.getZ()))) {
            String piloot = ikZone2 ? "z2_kapitein" : "luchtschipper";
            Component waar = Component.translatable(P + "luchtschip", s.waar(), naam(piloot), npcPlek(piloot));
            return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, npcPos(p, piloot), s.extra());
        }
        if (ikZone2 && doelZone2) {
            return s;
        }
        boolean ikEiland = eiland(p.getX(), p.getZ()), doelEiland = eiland(s.doel().getX(), s.doel().getZ());
        if (ikEiland == doelEiland) {
            return s;
        }
        String piloot = ikEiland ? "kapitein" : "luchtschipper";
        Component waar = Component.translatable(P + "luchtschip", s.waar(), naam(piloot), npcPlek(piloot));
        return new QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(), waar, npcPos(p, piloot), s.extra());
    }

    /** The camp island (with its east dock just outside the knuffelkamp area box): everything south of z 800. */
    static boolean eiland(double x, double z) {
        return z > 800;
    }

    /** Zone 2 (De Splinterpieken): its box starts at z -1800; the sea/cloud gap lies between. */
    static boolean zone2(double z) {
        return z < -1700;
    }

    @Nullable
    private static String gebied(String doel) {
        return doel.startsWith("gebied:") ? doel.substring(7) : null;
    }

    public static Component naam(String npc) {
        NpcDef d = Npcs.van(npc);
        return d == null ? Component.literal(npc) : Component.translatable(d.naamKey());
    }

    public static Component npcPlek(String npc) {
        return Component.translatable(PLEK + "npc." + npc);
    }

    public static Component poiPlek(String poi) {
        return Component.translatable(PLEK + "poi." + poi);
    }

    public static Component gebiedPlek(String gebied) {
        return Component.translatable(PLEK + "gebied." + gebied);
    }

    /** Where to level up to n (the zone's level bands, CONTRACT §2). */
    static Component levelPlek(int n) {
        String g = n <= 6 ? "klodderweide" : n <= 12 ? "sporenwoud" : n <= 18 ? "druipgrotten" : n <= 20 ? "zwamtempel"
                : n <= 23 ? "z2_galmhellingen" : n <= 26 ? "z2_knuffelmeer" : "z2_splinterkloof";   // (g-z2: zone 2 = lv 20-30)
        return gebiedPlek(g);
    }

    /** Where an NPC is now: the live guh when it is loaded (an escort walks along), else its spot. */
    @Nullable
    public static BlockPos npcPos(ServerPlayer p, String npc) {
        RijkGuh g = p.level() instanceof ServerLevel sl ? Npcs.levend(sl, npc) : null;
        if (g != null) {
            return g.blockPosition();
        }
        BlockPos b = NpcPlekken.NPC.get(npc);
        if (b == null) {
            b = NpcPlekken.KAMPWACHT.get(npc);
        }
        if (b == null) {   // (zone 2 NPCs: their own spot)
            NpcDef d = Npcs.van(npc);
            b = d == null ? null : d.plek();
        }
        return b;
    }

    /** One line for chat: "Volgende stap: <wat>, <waar>." */
    public Component regel() {
        return Component.translatable(P + "regel", wat, waar);
    }
}
