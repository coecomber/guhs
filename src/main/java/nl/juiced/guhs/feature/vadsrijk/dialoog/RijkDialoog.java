package nl.juiced.guhs.feature.vadsrijk.dialoog;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.taal.Tekst;

/**
 * T-DIALOOG (roadmap §5.4 "betere dialogen"): the server half of the Vadsrijk talking screen. Every conversation that goes
 * through {@link Praat} while the reader is in Het Vadsrijk (or whose sleutel starts with {@code vr_}) gets, via a
 * {@link Praat.Versierder}:
 * <ul>
 *   <li>{@code Rijk: true}: the client opens {@code client.RijkPraatScherm} instead of the plain Knuffeldal screen (the rest of the
 *       mod keeps that one untouched);</li>
 *   <li>per page: {@code Factie} (name plate colour), {@code Plek} (the speaker's home area, lang key), {@code Stem} (the blip
 *       voice: guh / mika / verteller / speler / draak / wezen), {@code Toon} (its pitch) and an explicit {@code Emotie};</li>
 *   <li>{@code Doel}: at a quest NPC, what to do next (who/what/where) for the "Wat moet ik doen?" button and the strip above the
 *       answers: the active quest's step, the quest on offer's first step, or the main story's next step.</li>
 * </ul>
 * Nothing here changes who answers what: QuestMotor, the zone 2/3 scenes and Erfenis keep their own listeners.
 */
public final class RijkDialoog {
    /** The player's own lines, the narrator, and the factions with their own plate colour (client: RijkPraatScherm.KLEUR). */
    public static final String SPELER = "speler", VERTELLER = "verteller", MIKA = "mika", DRAAK = "draak", ORDE = "orde", WEZEN = "wezen";
    /** Roles of the Knuffelorde (the trainers, the class temple, the camp guards). */
    static final Set<String> ORDE_ROLLEN = Set.of("training", "klasse", "kampwacht", "ordetempel");

    /** Text key -> mood, set by quest data ("npc:blij") or a feature ({@link #emotie}). */
    private static final Map<String, Emotie> EMOTIES = new ConcurrentHashMap<>();
    /** (dev) players who asked for the old screen ({@code /guhs vadsrijk dialoog oud}). */
    static final Set<UUID> OUD = ConcurrentHashMap.newKeySet();

    private RijkDialoog() {
    }

    public static void register(IEventBus modBus) {
        Praat.versierder(RijkDialoog::versier);
        // (q-dialoog's open point) the Mika-imker scene names a Mika without an entity: draw a Mika, not the book
        Praat.portretBron(naamKey -> naamKey.equals("vadsrijk.z3npc.imker.naam") ? soort("guhs:mika") : null);
        emotiesStandaard();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> DialoogCommando.register(e));
    }

    /** A line's mood (its text key), e.g. from quest data {@code "npc:blij"}. Unknown words are ignored. */
    public static void emotie(String tekstKey, String emotie) {
        Emotie e = Emotie.van(emotie);
        if (e != null) {
            EMOTIES.put(tekstKey, e);
        }
    }

    public static void emotie(String tekstKey, Emotie e) {
        EMOTIES.put(tekstKey, e);
    }

    @Nullable
    public static Emotie emotieVan(String tekstKey) {
        Emotie e = EMOTIES.get(tekstKey);
        if (e != null) {
            return e;
        }
        if (tekstKey.endsWith(".klaar.0")) {
            return Emotie.BLIJ;   // (the first line of a hand-in: the NPC is glad you are back)
        }
        return null;
    }

    private static void emotiesStandaard() {
        emotie("vadsrijk.npc.quiz.fout", Emotie.VERRAST);
        for (int i = 0; i < DialoogCommando.DEMO_EMOTIES.length; i++) {
            if (DialoogCommando.DEMO_EMOTIES[i] != null) {
                emotie(DialoogCommando.DEMO + i, DialoogCommando.DEMO_EMOTIES[i]);
            }
        }
    }

    /** Is this conversation drawn in the Vadsrijk style? */
    public static boolean rijk(ServerPlayer p, @Nullable String sleutel) {
        if (OUD.contains(p.getUUID())) {
            return false;
        }
        return Vadsrijk.in(p) || (sleutel != null && sleutel.startsWith("vr_"));
    }

    /** (Praat.Versierder) */
    static void versier(ServerPlayer p, @Nullable String sleutel, @Nullable Entity spreker, CompoundTag data) {
        if (!rijk(p, sleutel) || !data.getStringOr("Scherm", "verhaal").equals("verhaal")) {
            return;   // (the Knusfeest list etc. keep their own screen)
        }
        data.putBoolean("Rijk", true);
        ListTag paginas = data.getListOrEmpty("Paginas");
        if (paginas.isEmpty()) {
            versierPagina(p, data, spreker);   // (Praat.open: the screen's own data is the page)
        } else {
            for (Tag t : paginas) {
                versierPagina(p, (CompoundTag) t, null);
            }
        }
        // the "what now?" objective: at a Vadsrijk NPC guh (the conversation's first speaker)
        if (spreker instanceof RijkGuh g) {
            CompoundTag doel = doel(p, g.npcId());
            if (doel != null) {
                data.put("Doel", doel);
            }
        }
        CompoundTag dev = DialoogCommando.DOEL_VOLGEND.remove(p.getUUID());   // (the dev demo's made-up objective)
        if (dev != null) {
            data.put("Doel", dev);
        }
    }

    /** Faction, home area, voice, pitch and explicit mood of one page. */
    static void versierPagina(ServerPlayer p, CompoundTag pg, @Nullable Entity spreker) {
        int id = pg.getIntOr("Spreker", -1);
        if (spreker == null && id >= 0) {
            spreker = p.level().getEntity(id);
        }
        String naamKey = pg.getStringOr("Naam", "");
        boolean speler = pg.getBooleanOr("Speler", false);
        NpcDef def = spreker instanceof RijkGuh g ? Npcs.van(g.npcId())
                : naamKey.startsWith("vadsrijk.npc.naam.") ? Npcs.van(naamKey.substring("vadsrijk.npc.naam.".length())) : null;
        String type = spreker != null ? BuiltInRegistries.ENTITY_TYPE.getKey(spreker.getType()).getPath()
                : pg.getCompoundOrEmpty("Portret").getStringOr("Type", "");
        boolean heeftPortret = spreker != null || pg.contains("Portret");
        Kenmerk k = kenmerk(speler, naamKey, type, heeftPortret, def);
        pg.putString("Factie", k.factie());
        pg.putString("Stem", k.stem());
        pg.putFloat("Toon", toon(k.stem(), def != null ? def.id() : naamKey.isEmpty() ? type : naamKey));
        if (def != null) {
            Gebieden.Gebied gb = Gebieden.op(def.plek().getX(), def.plek().getY(), def.plek().getZ());
            if (gb != null && !gb.id().equals("ring") && !gb.id().equals("proeftuin")) {
                pg.putString("Plek", gb.naamKey());
            }
        }
        Emotie e = emotieVan(pg.getStringOr("Tekst", ""));
        if (e != null) {
            pg.putString("Emotie", e.id());
        }
    }

    /** A page's faction and voice. */
    public record Kenmerk(String factie, String stem) {
    }

    /**
     * Who is talking, as a faction (plate colour) and a voice. Pure, so it is gametested: the player; the narrator (its key, or
     * nobody to draw); a Mika (entity type or name); the dragon; a Knuffelorde NPC; a guh by its zone (id prefix z2_, z3_...);
     * any other creature.
     */
    public static Kenmerk kenmerk(boolean speler, String naamKey, String type, boolean heeftPortret, @Nullable NpcDef def) {
        if (speler) {
            return new Kenmerk(SPELER, SPELER);
        }
        String n = naamKey.toLowerCase(Locale.ROOT);
        if (n.equals("vadsrijk.npc.verteller") || n.endsWith(".verteller") || (!heeftPortret && def == null)) {
            return new Kenmerk(VERTELLER, VERTELLER);
        }
        if (type.contains("mika") || n.contains("mika")) {
            return new Kenmerk(MIKA, MIKA);
        }
        if (type.contains("draak") || type.contains("vahoeg") || type.contains("dragon") || n.contains("vahoeg") || n.contains("draak")) {
            return new Kenmerk(DRAAK, DRAAK);
        }
        String id = def != null ? def.id() : n.startsWith("vadsrijk.npc.naam.") ? n.substring("vadsrijk.npc.naam.".length()) : "";
        boolean guh = def != null || type.contains("guh") || n.startsWith("vadsrijk.npc.naam.") || n.matches("vadsrijk\\.z\\dnpc\\..*");
        if (def != null && (def.rol() != null && ORDE_ROLLEN.contains(def.rol()) || id.contains("ridder") || id.contains("priesteres") || id.contains("orde"))) {
            return new Kenmerk(ORDE, "guh");
        }
        if (!guh) {
            return new Kenmerk(WEZEN, WEZEN);
        }
        return new Kenmerk(zone(id, n), "guh");
    }

    /** "z1".."z9" from an NPC id ("z2_poollicht") or a zone name key ("vadsrijk.z3npc.x"); zone 1 has no prefix. */
    static String zone(String id, String naamKey) {
        if (id.length() > 2 && id.charAt(0) == 'z' && Character.isDigit(id.charAt(1)) && id.charAt(2) == '_') {
            return id.substring(0, 2);
        }
        if (naamKey.matches("vadsrijk\\.z\\dnpc\\..*")) {
            return "z" + naamKey.charAt(10);
        }
        return "z1";
    }

    /** The voice's pitch: its base, a little higher or lower per speaker (always the same for the same NPC). */
    public static float toon(String stem, String wie) {
        float basis = switch (stem) {
            case "guh" -> 1.35f;
            case MIKA -> 0.8f;
            case VERTELLER -> 0.7f;
            case SPELER -> 1.15f;
            case DRAAK -> 0.6f;
            default -> 1.0f;
        };
        int h = Math.floorMod(wie.hashCode(), 21) - 10;   // -10..10
        return Math.max(0.5f, Math.min(2.0f, basis + h * 0.025f));
    }

    /**
     * What kind of answer a text key is, for its icon in the screen: opdracht (take a quest), weg (leave), reis / reis_slot,
     * kaart, wapen, a service (bank, stal, smid, gondel, orde, drinken, winkel), quiz, klasse, or "" (no icon).
     */
    public static String optieSoort(String key) {
        if (key.endsWith(".optie.ja") || key.endsWith(".optie.hoofd") || key.contains("aannemen")) {
            return "opdracht";
        }
        if (key.endsWith(".optie.later") || key.contains("doei") || key.endsWith(".nee")) {
            return "weg";
        }
        if (key.contains(".optie.vlieg.") || key.contains("naar_stad") || key.contains("naar_kamp")) {
            return key.endsWith(".slot") ? "reis_slot" : "reis";
        }
        if (key.contains("reiskaart")) {
            return "kaart";
        }
        if (key.contains("startwapen")) {
            return "wapen";
        }
        int r = key.indexOf(".optie.rol.");
        if (r >= 0) {
            return switch (key.substring(r + 11)) {
                case "bank" -> "bank";
                case "stal" -> "stal";
                case "smid" -> "smid";
                case "gondel", "gondel_terug" -> "gondel";
                case "ordetempel" -> "orde";
                case "chocomelk" -> "drinken";
                default -> "winkel";
            };
        }
        if (key.contains(".quiz.")) {
            return "quiz";
        }
        if (key.contains(".klasse.")) {
            return "orde";
        }
        return "";
    }

    // ---- "Wat moet ik doen?" -------------------------------------------------------------------------------------------------

    /**
     * What to do next, said by this NPC: an active quest it gives, takes in or is part of (its current step); else the quest it
     * offers now (its first step, "if you say yes"); else, when it is who you last handed a main quest in to, the main story's
     * next step. Null: nothing to remind you of (no button).
     */
    @Nullable
    public static CompoundTag doel(ServerPlayer p, String npc) {
        if (npc.isEmpty()) {
            return null;
        }
        for (Quest q : Questlog.actieve(p)) {
            if (q.gever().equals(npc) || q.inleveren().equals(npc) || doetMee(q, npc)) {
                return doel("actief", QuestStap.van(p, q));
            }
        }
        for (Quest q : Quests.alle().values()) {
            if (q.gever().equals(npc) && (!q.auto() || q.hoofd()) && Questlog.beschikbaar(p, q)) {
                return doel("aanbod", QuestStap.eerste(p, q));
            }
        }
        List<Quest> gedaan = QuestStap.hoofdGedaan(p);
        QuestStap s = QuestStap.hoofdlijn(p);
        if (s != null && !gedaan.isEmpty() && gedaan.get(gedaan.size() - 1).inleveren().equals(npc)) {
            return doel("hoofd", s);
        }
        return null;
    }

    private static boolean doetMee(Quest q, String npc) {
        for (Quest.Doel d : q.doelen()) {
            if ((d.type() == Quest.Doel.Type.PRAAT || d.type() == Quest.Doel.Type.ESCORTE) && d.doel().equals(npc)) {
                return true;
            }
        }
        return false;
    }

    /** {Soort, Args: [titel, wat, waar(, extra)]}: the client builds the strip and the "Wat moet ik doen?" page from it. */
    public static CompoundTag doel(String soort, QuestStap s) {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort);
        ListTag args = new ListTag();
        args.add(arg(s.titel()));
        args.add(arg(s.wat()));
        args.add(arg(s.waar()));
        t.put("Args", args);
        if (s.extra() != null) {
            CompoundTag x = new CompoundTag();
            Tekst.put(x, "A", s.extra());
            t.put("Extra", x);
        }
        return t;
    }

    static CompoundTag arg(Component c) {
        CompoundTag a = new CompoundTag();
        Tekst.put(a, "A", c);
        return a;
    }

    /** A portrait of just an entity type (a Mika without an entity). */
    static CompoundTag soort(String type) {
        CompoundTag t = new CompoundTag();
        t.putString("Type", type);
        return t;
    }
}
