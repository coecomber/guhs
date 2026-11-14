package nl.juiced.guhs.feature.vadsrijk.bazenboek;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBazen;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerLoot;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasStats;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * 1.10.3 "Bazen" tab of the Vadsrijk-menu: every boss per zone (arena bosses, story fights, the cave dungeon bosses, the zone-6 raid),
 * with your statistics ({@link BaasStats}), its rare drops with your dry streak (mount drops {@link RijdierBuit}, cave pets
 * {@link KerkerLoot}, first-kill pets {@link Huisdieren}) and whether you can fight it right now ({@link #toegang}: a rule per boss in
 * {@link #REGELS}, then the arena respawn / dungeon cooldown, else "Toegankelijk").
 * <p>Only zones open for the player are listed ({@link ZonePoort#dicht}); the next closed one shows only its requirement. A boss you
 * have not beaten yet shows as "???" with its area (like the Verkenningsboek).
 */
public final class Bazenboek {
    public static final String K = "vadsrijk.bazenboek.";

    /** "Can I fight it now?": yes/no + why (and what to do). */
    public record Toegang(boolean ja, Component reden) {
        public static Toegang ja(Component r) {
            return new Toegang(true, r);
        }

        public static Toegang nee(Component r) {
            return new Toegang(false, r);
        }
    }

    /** One boss of the tab. */
    public static final class Baas {
        public final String id;
        public final int zone;
        public final String naamKey;
        public final List<String> types;
        @Nullable
        String gebiedKey, arena, dungeon, grotPet;
        String soort = "veld";
        final List<String> mounts = new ArrayList<>();

        Baas(String id, int zone, String naamKey, List<String> types) {
            this.id = id;
            this.zone = zone;
            this.naamKey = naamKey;
            this.types = types;
        }

        Baas gebied(String g) {
            gebiedKey = g.startsWith("vadsrijk.") ? g : "vadsrijk.kern.gebied." + g;
            return this;
        }

        Baas arena(String a) {
            arena = a;
            return this;
        }

        Baas soort(String s) {
            soort = s;
            return this;
        }

        Baas mount(String bron) {
            mounts.add(bron);
            return this;
        }

        @Nullable
        public String arena() {
            return arena;
        }
    }

    /** Boss id -> its own access rule (null answer = no objection: then the arena / dungeon / default rule). */
    public static final Map<String, Function<ServerPlayer, Toegang>> REGELS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Baas> BAZEN = new LinkedHashMap<>();
    private static final Map<String, String> PER_TYPE = new java.util.HashMap<>();
    private static boolean klaar;

    private Bazenboek() {
    }

    /** From VInt.register: the type -> boss id link for the statistics, and the access rules. */
    public static void register() {
        BaasStats.ID = t -> {
            vul();
            return PER_TYPE.get(t);
        };
        Regels.register();
    }

    private static Baas b(String id, int zone, String naamKey, String... types) {
        Baas b = new Baas(id, zone, naamKey, List.of(types));
        BAZEN.put(id, b);
        for (String t : types) {
            PER_TYPE.put(t, id);
        }
        return b;
    }

    private static Baas e(String id, int zone, String type) {
        return b(id, zone, "entity." + type.replace(':', '.'), type);
    }

    /** The list (built on first use: the cave and arena registries are complete by then). */
    static synchronized void vul() {
        if (klaar) {
            return;
        }
        klaar = true;
        // zone 1: De Knuffelzwammen
        e("klodderreus", 1, "guhs:vr_klodderreus").gebied("knuffelkamp").arena(nl.juiced.guhs.feature.vadsrijk.npc.Verhaal.ARENA);
        e("koning_klodder", 1, "guhs:vr_baas_koning_klodder").gebied("klodderweide").arena("bazen:koning_klodder");
        e("opperzwam", 1, "guhs:vr_baas_opperzwam").gebied("zwamtempel").arena("bazen:opperzwam").mount("opperzwam_zwamzwijntje");
        b("mika_spion", 1, "entity.guhs.vr_baas_mika_spion", "guhs:vr_baas_mika_spion", "guhs:vr_baas_lok_mika").gebied("zwamzoet").soort("verhaal");
        // zone 2: De Splinterpieken
        e("galmwolf", 2, "guhs:vr_z2_baas_galmwolf").gebied("z2_galmhellingen").arena("z2bazen:galmwolf");
        e("spiegelzucht", 2, "guhs:vr_z2_baas_spiegelzucht").gebied("z2_spiegelburcht").arena("z2bazen:spiegelzucht").mount("spiegelzucht_kristal");
        e("boormeester", 2, "guhs:vr_z2_baas_boormeester").gebied("z2_splinterkloof").soort("geheim");
        // zone 3: De Schemerwortel
        for (String z3 : new String[] {"roquefort", "taleggio", "brokkel", "kruinroofvogel", "piepvel", "wortelwurger", "nestwachter", "bleu",
                "vahoegnir"}) {
            e(z3, 3, "guhs:vr_z3_baas_" + z3).arena("z3bazen:" + z3);
        }
        BAZEN.get("bleu").mount("vahoeg_goud");
        // zone 4: De Lampionkust
        b("zeeslurf", 4, "entity.guhs.vr_z4_baas_zeeslurf", "guhs:vr_z4_baas_zeeslurf", "guhs:vr_z4_zeeslurf").gebied("z4_koraalbaai").arena("z4bazen:zeeslurf");
        e("vliegervanger", 4, "guhs:vr_z4_baas_vliegervanger").gebied("z4_wrak").arena("z4bazen:vliegervanger");
        // zone 5: Hoogvads en Moeder Wolkwal
        e("zilverstaart", 5, "guhs:vr_z5_baas_zilverstaart").gebied("z5_wolkwaldroom").arena("z5wezens:zilverstaart");
        e("gorgonzola", 5, "guhs:vr_z5_baas_gorgonzola").gebied("z5_buikgangen").arena("z5wezens:gorgonzola");
        e("donderbuik", 5, "guhs:vr_z5_baas_donderbuik").arena("z5wezens:donderbuik");
        e("grauwe_droom", 5, "guhs:vr_z5_baas_grauwe_droom").gebied("z5d_droom").soort("verhaal");
        // zone 6: Het Zijdeland (+ the raid De Sterrendovende Toren)
        e("wormelien", 6, "guhs:vr_z6_baas_wormelien").gebied("z6_roze_duinen").arena("z6wezens:wormelien");
        e("zoutkolos", 6, "guhs:vr_z6_baas_zoutkolos").gebied("z6_zoutvlakte").arena("z6wezens:zoutkolos");
        e("astrolabiumwachter", 6, "guhs:vr_z6_baas_astrolabiumwachter").arena("z6wezens:astrolabiumwachter");
        e("glimmertje", 6, "guhs:vr_z6_baas_glimmertje").arena("z6wezens:glimmertje");
        e("sterrenguh", 6, "guhs:vr_z6_baas_sterrenguh").gebied("z6_ruinedal").arena("z6wezens:sterrenguh");
        e("z6raid_zandwachter", 6, "guhs:vr_z6raid_baas_zandwachter").gebied("z6_toren").soort("raid").arena("z6raid:zandwachter");
        e("z6raid_spiegelvizier", 6, "guhs:vr_z6raid_baas_spiegelvizier").gebied("z6_toren").soort("raid").arena("z6raid:spiegelvizier");
        b("z6raid_sterrenbeelden", 6, K + "naam.sterrenbeelden", "guhs:vr_z6raid_baas_ster_knuffel", "guhs:vr_z6raid_baas_ster_bultguh",
                "guhs:vr_z6raid_baas_ster_pluis").gebied("z6_toren").soort("raid").arena("z6raid:sterrenbeelden");
        b("z6raid_ringeling", 6, "entity.guhs.vr_z6raid_baas_ringeling", "guhs:vr_z6raid_baas_ringeling", "guhs:vr_z6_baas_ringeling").gebied("z6_toren")
                .soort("raid").arena("z6raid:ringeling").mount(nl.juiced.guhs.feature.vadsrijk.vint.VInt.Z6RAID_BRON);
        // the cave dungeons (one boss per cave, in its zone)
        for (GrotBazen.Def d : GrotBazen.alle().values()) {
            Baas g = e("grot_" + d.grot(), grotZone(d.grot()), "guhs:vr_grot_baas_" + d.grot()).gebied("vadsrijk.grotten.naam." + d.grot()).soort("dungeon")
                    .mount(d.mountBron());
            g.dungeon = d.grot();
            g.grotPet = d.pet();
        }
    }

    static int grotZone(String grot) {
        return switch (grot) {
            case "kristalgrot" -> 2;
            case "wortelgrot" -> 3;
            case "koraalgrot" -> 4;
            case "wolkengrot" -> 5;
            case "zoutgrot" -> 6;
            default -> 1;
        };
    }

    public static List<Baas> alle() {
        vul();
        synchronized (Bazenboek.class) {
            return List.copyOf(BAZEN.values());
        }
    }

    @Nullable
    public static Baas van(String id) {
        vul();
        return BAZEN.get(id);
    }

    /** The boss a mount drop source belongs to (for the "received" list), or null. */
    @Nullable
    public static String baasVanMount(String bron) {
        for (Baas b : alle()) {
            if (b.mounts.contains(bron)) {
                return b.id;
            }
        }
        return null;
    }

    /** RijdierBuit: a rare mount was just given from source {@code bron}: remembered as a special drop of its boss. */
    public static void mountGekregen(ServerPlayer p, String bron) {
        BaasStats.speciaal(p, baasVanMount(bron), mountNaam(bron));
    }

    /** KerkerLoot: the rare cave pet was just given. */
    public static void petGekregen(ServerPlayer p, String grot, String pet) {
        BaasStats.speciaal(p, "grot_" + grot, Component.translatable("vadsrijk.huisdier." + pet + ".naam"));
    }

    static Component mountNaam(String bron) {
        RijdierBuit.Doel d = RijdierBuit.DOELEN.get(bron);
        if (d == null) {
            return Component.literal(bron);
        }
        if (d.kleur() != null) {
            return Component.translatable("vadsrijk.economie.rijdieren.naam_kleur", Component.translatable(d.soort().naamKey()),
                    Component.translatable(d.kleur().naamKey()));
        }
        if (bron.equals(nl.juiced.guhs.feature.vadsrijk.vint.VInt.Z6RAID_BRON)) {
            return Component.translatable(K + "naam.sterrennacht");
        }
        return Component.translatable(d.soort().naamKey());
    }

    // ---- access ----------------------------------------------------------------------------------------------------------------

    /** Can this player fight boss {@code b} right now? Its own rule, then the arena respawn or dungeon cooldown, else yes. */
    public static Toegang toegang(ServerPlayer p, Baas b) {
        if (ZonePoort.dicht(p, b.zone)) {
            return Toegang.nee(Component.translatable(K + "toegang.zone", ZonePoort.zoneNaam(b.zone)));
        }
        Function<ServerPlayer, Toegang> r = REGELS.get(b.id);
        Toegang eigen = r == null ? null : r.apply(p);
        if (eigen != null && !eigen.ja()) {
            return eigen;
        }
        if (b.dungeon != null) {
            long w = nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij.wachttijd(p, b.dungeon);
            Component grot = Component.translatable("vadsrijk.grotten.naam." + b.dungeon);
            return w > 0 ? Toegang.nee(Component.translatable(K + "toegang.dungeon.wacht", grot, nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij.tijd(w)))
                    : Toegang.ja(Component.translatable(K + "toegang.dungeon", grot));
        }
        if (b.arena != null && Arenas.van(b.arena) != null && !"raid".equals(b.soort)) {
            Toegang t = arenaToegang(p, b.arena);
            if (t != null) {
                return t;
            }
        }
        return eigen != null ? eigen : Toegang.ja(Component.translatable(K + "toegang.ja"));
    }

    /** The arena's respawn: "Terug over 12:30" (no), "Is er nu" (yes), or null when unknown. */
    @Nullable
    static Toegang arenaToegang(ServerPlayer p, String arena) {
        long op = Arenas.respawnOp(arena), nu = p.level().getGameTime();   // (every level shares the overworld's clock)
        if (op == -2) {
            return Toegang.nee(Component.translatable(K + "toegang.net_verslagen"));
        }
        if (op > nu) {
            long s = (op - nu) / 20;
            return Toegang.nee(Component.translatable(K + "toegang.terug", String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)));
        }
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        VadsrijkBaas levend = sl == null ? null : Arenas.baas(sl, arena);
        return levend != null ? Toegang.ja(Component.translatable(K + "toegang.er_nu")) : null;
    }

    /** A quest-only fight: yes while the quest runs, else "Alleen tijdens de quest X". */
    static Toegang alleenTijdensQuest(ServerPlayer p, String quest) {
        Quest q = Quests.van(quest);
        Component naam = q == null ? Component.literal(quest) : Component.translatable(q.titelKey());
        if (Questlog.actief(p, quest)) {
            return Toegang.ja(Component.translatable(K + "toegang.quest.nu", naam));
        }
        return Toegang.nee(Component.translatable(Questlog.klaar(p, quest) ? K + "toegang.quest.klaar" : K + "toegang.quest", naam));
    }

    // ---- the tab's data ----------------------------------------------------------------------------------------------------------

    static String json(ServerPlayer p, Component c) {
        try {
            return net.minecraft.network.chat.ComponentSerialization.CODEC
                    .encodeStart(p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), c).getOrThrow().toString();
        } catch (RuntimeException e) {
            return "\"" + c.getString().replace("\"", "'") + "\"";
        }
    }

    /** Has this player beaten boss {@code b} (statistics, or an old first-kill key: then it is seeded as "at least once")? */
    static boolean verslagen(ServerPlayer p, Baas b) {
        if (BaasStats.alle(p).getCompoundOrEmpty(b.id).getIntOr("kills", 0) > 0) {
            return true;
        }
        for (String t : b.types) {
            if (Spelers.data(p).gezien.contains("baaskill:" + t)) {
                BaasStats.zaai(p, b.id);
                return true;
            }
        }
        if (b.dungeon != null && Spelers.data(p).gezien.contains("grotbaas:" + b.dungeon)) {
            BaasStats.zaai(p, b.id);
            return true;
        }
        return false;
    }

    /**
     * The tab: {@code zones} = [{zone, naam (lang key), open, eis (component JSON, closed zone only), bazen: [...]}] for the open zones
     * plus the first closed one; one boss = {id, naam (lang key), bekend, gebied (lang key), soort, lv, respawn (s), toegang, reden
     * (JSON), the statistics of {@link BaasStats} (kills, oud, eerste, snelst, schade, heal, tank, munten, items, r, sp) and
     * {@code zeldzaam}: [{n (JSON), regel (JSON), pech, pity, heeft}]}.
     */
    public static CompoundTag scherm(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        ListTag zl = new ListTag();
        Map<Integer, ListTag> per = new LinkedHashMap<>();
        boolean dichtGezien = false;
        for (int z = 1; z <= 6; z++) {
            boolean open = !ZonePoort.dicht(p, z);
            if (!open && (dichtGezien || ZonePoort.serverDicht(p, z))) {
                break;
            }
            CompoundTag zt = new CompoundTag();
            zt.putInt("zone", z);
            zt.putString("naam", "vadsrijk.zonepoort.zone." + z);
            zt.putBoolean("open", open);
            if (!open) {
                dichtGezien = true;
                zt.putString("eis", json(p, ZonePoort.eisTekst(p, z)));
            } else {
                ListTag bl = new ListTag();
                per.put(z, bl);
                zt.put("bazen", bl);
            }
            zl.add(zt);
        }
        for (Baas b : alle()) {
            ListTag bl = per.get(b.zone);
            if (bl != null) {
                bl.add(baas(p, b));
            }
        }
        t.put("zones", zl);
        return t;
    }

    static CompoundTag baas(ServerPlayer p, Baas b) {
        CompoundTag c = BaasStats.alle(p).getCompoundOrEmpty(b.id).copy();
        boolean bekend = verslagen(p, b);
        c = BaasStats.alle(p).getCompoundOrEmpty(b.id).copy();   // (after a possible seed)
        c.remove("lt");
        c.remove("lt_type");
        c.putString("id", b.id);
        c.putInt("zone", b.zone);
        c.putString("naam", b.naamKey);
        c.putBoolean("bekend", bekend);
        c.putString("soort", b.soort);
        String gebied = b.gebiedKey;
        Arena ar = b.arena == null ? null : Arenas.van(b.arena);
        if (gebied == null && ar != null) {
            Gebieden.Gebied g = Gebieden.op(ar.centrum());
            gebied = g == null ? null : "vadsrijk.kern.gebied." + g.id();
        }
        c.putString("gebied", gebied == null ? "vadsrijk.zonepoort.zone." + b.zone : gebied);
        if (ar != null && !"raid".equals(b.soort)) {
            c.putInt("respawn", ar.respawnSeconden());
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            VadsrijkBaas levend = sl == null ? null : Arenas.baas(sl, b.arena);
            if (levend != null) {
                c.putInt("lv", levend.mobLevel());
            }
        }
        Toegang tg = toegang(p, b);
        c.putBoolean("toegang", tg.ja());
        c.putString("reden", json(p, tg.reden()));
        c.put("zeldzaam", zeldzaam(p, b));
        return c;
    }

    /** The rare drops of a boss with the player's dry streak. */
    static ListTag zeldzaam(ServerPlayer p, Baas b) {
        ListTag l = new ListTag();
        for (String bron : b.mounts) {
            RijdierBuit.Bron rb = RijdierBuit.BRONNEN.get(bron);
            RijdierBuit.Doel d = RijdierBuit.DOELEN.get(bron);
            if (rb == null || d == null) {
                continue;
            }
            boolean heeft = rb.heeft().test(p);
            int pech = Math.min(RijdierBuit.PITY, RijdierBuit.pech(p, bron));
            boolean geheim = d.kleur() == RijdierSoort.PluimKleur.GOUD && !heeft && pech == 0;   // (the gold egg stays a secret until tried)
            Component per = Component.translatable("vadsrijk.vint.pech.per." + d.per());
            Component regel = heeft ? Component.translatable(K + "drop.heb")
                    : !rb.mag().test(p) ? Component.translatable("vadsrijk.vint.pech.mag_niet", Math.round(RijdierBuit.KANS * 100))
                    : kans((int) Math.round(RijdierBuit.KANS * 100), per, pech, RijdierBuit.PITY);
            l.add(drop(p, geheim ? Component.literal("???") : Component.translatable(K + "drop.rijdier", mountNaam(bron)), regel, pech,
                    RijdierBuit.PITY, heeft));
        }
        if (b.grotPet != null) {
            GrotBazen.Def d = GrotBazen.def(b.dungeon);
            boolean heeft = Huisdieren.heeft(p, b.grotPet);
            int pech = d == null ? 0 : Math.min(KerkerLoot.PET_PITY, KerkerLoot.petPech(p, d));
            Component regel = heeft ? Component.translatable(K + "drop.heb")
                    : kans((int) Math.round(KerkerLoot.PET_KANS * 100), Component.translatable("vadsrijk.vint.pech.per.clear"), pech, KerkerLoot.PET_PITY);
            l.add(drop(p, Component.translatable(K + "drop.huisdier", Component.translatable("vadsrijk.huisdier." + b.grotPet + ".naam")), regel, pech,
                    KerkerLoot.PET_PITY, heeft));
        }
        for (Huisdier h : Huisdieren.alle()) {   // the first-kill pets ("baaskill:<type>")
            if (b.types.contains(h.baas())) {
                boolean heeft = Huisdieren.heeft(p, h.id());
                l.add(drop(p, Component.translatable(K + "drop.huisdier", Component.translatable(h.naamKey())),
                        Component.translatable(heeft ? K + "drop.heb" : K + "drop.eerste"), 0, 0, heeft));
            }
        }
        return l;
    }

    static Component kans(int procent, Component per, int pech, int pity) {
        int nog = Math.max(0, pity - pech);
        return nog == 0 ? Component.translatable("vadsrijk.vint.pech.zeker", procent, per, per)
                : Component.translatable("vadsrijk.vint.pech.kans", procent, per, nog);
    }

    private static CompoundTag drop(ServerPlayer p, Component naam, Component regel, int pech, int pity, boolean heeft) {
        CompoundTag d = new CompoundTag();
        d.put("n", StringTag.valueOf(json(p, naam)));
        d.put("regel", StringTag.valueOf(json(p, regel)));
        d.putInt("pech", pech);
        d.putInt("pity", pity);
        d.putBoolean("heeft", heeft);
        return d;
    }
}
