package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;

/**
 * 1.10.4 "Ambachten in het verhaal" (ONTWERP_1104 phase 2): the crafts become part of the main story.
 * <ul>
 *   <li><b>The chains</b> ({@link #KETENS}): Meester Knutsel's lesson on the Ambachtseiland (zone-1 tutorial, between Opruimen and
 *   the campfire) and per zone a gathering step + a making step. The quests are data (tools/features/vadsrijk_ambacht_verhaal.py
 *   -> quests_ambacht.json, spliced into the questlog by {@code Quests}); the quest that used to follow a chain now waits for it.</li>
 *   <li><b>Missed steps</b> (user decision): who already got past a chain's place before 1.10.4 gets the chain as an active quest
 *   ({@link #bijwerken}, after login: the lesson comes as "a letter from Meester Knutsel"); the main-story tracker skips it
 *   ({@link #gemist}) so the story the player is in stays in front, and the per-player zone gate asks for every chain of the zones
 *   up to the one he wants to leave ({@link #ok}, {@code ZonePoort.toegang}). Who already stands in a later zone keeps it.</li>
 *   <li><b>Story items</b> ({@link #ITEMS}, {@code guhs:vr_amb_verhaal_<id>}) are {@code Recepten.verhaalRecept}s at the zone's tier
 *   level (z1 5, z2 12, z3 22, z4 32, z5 40, z6 48), only makeable while their step asks for them ({@link #magMaken}).</li>
 *   <li><b>Listening</b>: gathering ({@code MateriaalTas.GEGEVEN}: {@code meld "ambacht_oogst:<kind>"}) and making
 *   ({@link #gemaakt}: {@code meld "ambacht_maak:<recipe>"} and {@code "ambacht_maak:<soort>_<doel>"}).</li>
 * </ul>
 * The islet itself (Meester Knutsel, the hut masters, the gathering groups, the boards) is {@link EilandRegistratie}.
 */
public final class AmbachtVerhaal {
    private AmbachtVerhaal() {
    }

    /** One inserted chain: its quests in order, the zone (for the gate), and the main quest that used to follow it. */
    public record Keten(String id, int zone, List<String> quests, String volgende) {
    }

    public static final List<Keten> KETENS = List.of(
            new Keten("les", 1, List.of("hq03d_knutsel", "hq03e_verzamelen", "hq03f_maken"), "hq04_kampvuur"),
            new Keten("z1", 1, List.of("hq10a_zwamhout", "hq10b_tempelsleutel"), "hq11_opperzwam"),
            new Keten("z2", 2, List.of("z2_hq06a_ijsforellen", "z2_hq06b_kandijsoep"), "z2_hq07_onder_het_ijs"),
            new Keten("z3", 3, List.of("z3_hq07a_hars", "z3_hq07b_lijmdrankje"), "z3_hq08_werf"),
            new Keten("z4", 4, List.of("z4_hq05a_koraal", "z4_hq05b_lampion"), "z4_hq06_feest"),
            new Keten("z5", 5, List.of("z5_hq08a_wolkenwol", "z5_hq08b_dekentje"), "z5_hq09_slaaplied"),
            new Keten("z6", 6, List.of("z6_hq07a_zijde", "z6_hq07b_kompaskoord"), "z6_hq08_sterrenwacht"));

    /** The story-item recipe -> the quest whose step asks for it (made only while that quest is active and still needs it). */
    public static final Map<String, String> RECEPT_STAP = Map.of("tempelsleutel", "hq10b_tempelsleutel", "warme_kandijsoep", "z2_hq06b_kandijsoep",
            "lijmdrankje", "z3_hq07b_lijmdrankje", "koraallampion", "z4_hq05b_lampion", "wolkendekentje", "z5_hq08b_dekentje",
            "kompaskoord", "z6_hq07b_kompaskoord");

    // ---- the story items + recipes ------------------------------------------------------------------------------------------------

    public static final DeferredRegister.Items ITEMS_REG = DeferredRegister.createItems(Guhs.MODID);
    /** id -> {@code guhs:vr_amb_verhaal_<id>} (names, lore and models: tools/features/vadsrijk_ambacht_verhaal.py). */
    public static final Map<String, DeferredItem<Item>> ITEMS = new LinkedHashMap<>();

    static {
        for (String id : List.of("tempelsleutel", "warme_kandijsoep", "lijmdrankje", "koraallampion", "wolkendekentje", "kompaskoord")) {
            ITEMS.put(id, ITEMS_REG.registerSimpleItem("vr_amb_verhaal_" + id, () -> nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry.metLore(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), id, "vr_amb_verhaal_", id)));
        }
    }

    /** The recipes: per zone the tier level of that zone ({@link AmbachtXp#TIER_MIN}), from that zone's gathered materials. */
    static void recepten() {
        Recepten.verhaalRecept("tempelsleutel", Ambacht.SMEDEN, 5, 1, () -> new ItemStack(ITEMS.get("tempelsleutel").get()),
                Recepten.ingredient(4, "zwamsteel"), Recepten.ingredient(4, "pluisijzer"));
        Recepten.verhaalRecept("warme_kandijsoep", Ambacht.KOKEN, 12, 2, () -> new ItemStack(ITEMS.get("warme_kandijsoep").get()),
                Recepten.ingredient(5, "spiegelforel"));
        Recepten.verhaalRecept("lijmdrankje", Ambacht.KOKEN, 22, 3, () -> new ItemStack(ITEMS.get("lijmdrankje").get()),
                Recepten.ingredient(3, "roze_hars"), Recepten.ingredient(2, "schemerhoning"));
        Recepten.verhaalRecept("koraallampion", Ambacht.SMEDEN, 32, 4, () -> new ItemStack(ITEMS.get("koraallampion").get()),
                Recepten.ingredient(4, "roze_koraal"), Recepten.ingredient(2, "zeesponsje"));
        Recepten.verhaalRecept("wolkendekentje", Ambacht.SMEDEN, 40, 5, () -> new ItemStack(ITEMS.get("wolkendekentje").get()),
                Recepten.ingredient(5, "wolkenwol"));
        // 1.10.4: zone 6 has its own T6 materials now (Zone6Haken): silk thread from the mulberry trees + star gold, at the smidse of
        // Saffraanpoort's bazaar (T6 recipes: only at the zone-6 teachers)
        Recepten.verhaalRecept("kompaskoord", Ambacht.SMEDEN, 48, 6, () -> new ItemStack(ITEMS.get("kompaskoord").get()),
                Recepten.ingredient(3, "zijdedraad"), Recepten.ingredient(2, "sterrengoud"));
        // Meester Knutsel's lesson: the Klein Vadsdrankje (the only potion the shops still sell) is a Koken T1 recipe too
        Recepten.WAREN.put("klein_vadsdrankje", () -> new ItemStack(EcoSpul.KLEIN_VADSDRANKJE.get()));
        Recepten.ALLE.put("klein_vadsdrankje", new Recepten.Recept("klein_vadsdrankje", Ambacht.KOKEN, 1, 1,
                List.of(Recepten.ingredient(1, "klodderklaver")), "item:klein_vadsdrankje", 3));
    }

    public static void register(IEventBus modBus) {
        ITEMS.size();
        ITEMS_REG.register(modBus);
        recepten();
        EilandRegistratie.registreer();
        Zone6Haken.registreer();   // 1.10.4: the T6 gathering groups of zone 6
        plekken();
        MateriaalTas.GEGEVEN.add(AmbachtVerhaal::verzameld);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                WACHT.put(p.getUUID(), 0L);   // (checked a moment later, once the player's data is there and he is in the Vadsrijk)
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> WACHT.remove(e.getEntity().getUUID()));
        nl.juiced.guhs.world.TikWacht.post("AmbachtVerhaal.tik", e -> tik(e.getServer()));
    }

    /** The tracker spots of the craft steps ("poi:&lt;id&gt;", texts {@code vadsrijk.npc.plek.poi.<id>}). */
    static void plekken() {
        poi("ambacht_brug_kamp", 1, "ambacht_brug_kamp");
        poi("ambacht_knutsel", 1, "ambacht_knutsel");
        poi("ambacht_groeve", 1, "ambacht_groeve.midden");
        poi("ambacht_bloemenwei", 1, "ambacht_bloemenwei.midden");
        poi("ambacht_visvijver", 1, "ambacht_visvijver.midden");
        poi("ambacht_smederij", 1, "ambacht_smederij.voor");
        poi("ambacht_keuken", 1, "ambacht_keuken.voor");
        poi("amb_z1_hout", 1, "kampvuur_sporenwoud_1");
        poi("amb_z1_erts", 1, "kampvuur_druipgrotten_1");
        poi("amb_smidse_z1", 1, "plein");
        poi("amb_z2_vis", 2, "kampvuur_knuffelmeer_1");
        poi("amb_keuken_z2", 2, "plein");
        poi("amb_z3_hout", 3, "lichtkeverbos");
        poi("amb_z3_korf", 3, "raatweide_plein");
        poi("amb_keuken_z3", 3, "markt");
        poi("amb_z4_duik", 4, "duiksteiger");
        poi("amb_z5_wol", 5, "staartpark_prieel");
        poi("amb_z5_erts", 5, "z5w_noordvin");
        poi("amb_z6_zijde", 6, "palmenkloven_moerbei");
        poi("amb_z6_erts", 6, "zoutgrot_mijn");
    }

    private static void poi(String id, int zone, String wereldPoi) {
        BlockPos b = AmbachtPlekken.plek(zone, wereldPoi);
        if (b != null) {
            QuestPlek.POI.put(id, b);
        }
    }

    // ---- the chains ------------------------------------------------------------------------------------------------------------------

    @Nullable
    public static Keten keten(String quest) {
        for (Keten k : KETENS) {
            if (k.quests().contains(quest)) {
                return k;
            }
        }
        return null;
    }

    /** Is this quest one of the craft steps? (not a Verkenningsboek row) */
    public static boolean isStap(String quest) {
        return keten(quest) != null;
    }

    /** Did this player get past the chain's place in the story (the quest after it started or done)? */
    public static boolean voorbij(ServerPlayer p, Keten k) {
        return Questlog.status(p, k.volgende()) != 0;
    }

    /**
     * Is this a craft step the story already went past for this player (an older player)? Then the main-story tracker skips it: it
     * waits in the questlog and at the zone gate.
     */
    public static boolean gemist(ServerPlayer p, String quest) {
        Keten k = keten(quest);
        return k != null && !Questlog.klaar(p, quest) && voorbij(p, k);
    }

    /** {done, total} quests of every chain of zones 1..n. */
    public static int[] telling(ServerPlayer p, int n) {
        int klaar = 0, alle = 0;
        for (Keten k : KETENS) {
            if (k.zone() > n) {
                continue;
            }
            for (String q : k.quests()) {
                alle++;
                if (Questlog.klaar(p, q)) {
                    klaar++;
                }
            }
        }
        return new int[] {klaar, alle};
    }

    /** Every craft step of zones 1..n done? (the per-player zone gate) */
    public static boolean ok(ServerPlayer p, int n) {
        int[] t = telling(p, n);
        return t[0] >= t[1];
    }

    /** The first open craft step of zones 1..n (in chain order), or null. */
    @Nullable
    public static Quest eersteOpen(ServerPlayer p, int n) {
        for (Keten k : KETENS) {
            if (k.zone() > n) {
                continue;
            }
            for (String id : k.quests()) {
                Quest q = Quests.van(id);
                if (q != null && !Questlog.klaar(p, id)) {
                    return q;
                }
            }
        }
        return null;
    }

    /**
     * 1.10.9: the first active craft step the story already went past (chain order, so the islet lesson first), or null. The main
     * line in the tracker skips these ({@code QuestStap.hoofdlijn}), so without this an older player on the Ambachtseiland saw
     * only his next main quest somewhere else; NpcVolg shows this one as its own tracker line.
     */
    @Nullable
    public static Quest eersteGemist(ServerPlayer p) {
        for (Keten k : KETENS) {
            if (!voorbij(p, k)) {
                continue;
            }
            for (String id : k.quests()) {
                if (Questlog.klaar(p, id)) {
                    continue;
                }
                Quest q = Quests.van(id);
                if (q != null && Questlog.actief(p, id)) {
                    return q;
                }
                break;   // (one open step per chain)
            }
        }
        return null;
    }

    // ---- missed chains of older players ----------------------------------------------------------------------------------------------

    /** Players to check (uuid -> the tick it may happen; 0 = soon). */
    private static final Map<UUID, Long> WACHT = new ConcurrentHashMap<>();

    static void tik(MinecraftServer server) {
        if (WACHT.isEmpty() || server.getTickCount() % 20 != 7) {
            return;
        }
        long nu = server.getTickCount();
        for (Map.Entry<UUID, Long> e : WACHT.entrySet()) {
            if (e.getValue() == 0L) {
                e.setValue(nu + 60);   // (3 s after the login: the data is loaded, the welcome messages are out)
                continue;
            }
            if (nu < e.getValue()) {
                continue;
            }
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                WACHT.remove(e.getKey());
            } else if (Vadsrijk.in(p)) {   // (outside the Vadsrijk: wait until he comes in)
                WACHT.remove(e.getKey());
                bijwerken(p);
            }
        }
    }

    /**
     * Gives an older player every chain the story already went past as an active quest (the first open step whose previous quests
     * are done; a chain's next steps start by themselves). Returns how many steps were started. Tells the player once: the lesson as
     * a letter from Meester Knutsel, the zone steps as one line.
     */
    public static int bijwerken(ServerPlayer p) {
        int n = 0;
        boolean les = false;
        for (Keten k : KETENS) {
            if (!voorbij(p, k)) {
                continue;
            }
            for (String id : k.quests()) {
                Quest q = Quests.van(id);
                if (q == null || Questlog.klaar(p, id)) {
                    continue;
                }
                if (Questlog.status(p, id) == 0 && naKlaar(p, q)) {
                    Questlog.neemAan(p, q);
                    n++;
                    les |= k.id().equals("les");
                }
                break;   // (one open step per chain)
            }
        }
        if (n > 0) {
            if (les) {
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.verhaal.brief").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            int gemist = 0;
            for (Keten k : KETENS) {
                if (voorbij(p, k)) {
                    for (String id : k.quests()) {
                        gemist += Questlog.klaar(p, id) ? 0 : 1;
                    }
                }
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.verhaal.gemist", gemist).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.9f);
        }
        return n;
    }

    private static boolean naKlaar(ServerPlayer p, Quest q) {
        for (String na : q.na()) {
            if (Quests.van(na) != null && !Questlog.klaar(p, na)) {
                return false;
            }
        }
        return true;
    }

    // ---- story recipes ---------------------------------------------------------------------------------------------------------------

    /** May this player make this story recipe now? (its step is active and still needs the item) Other recipes: always. */
    public static boolean magMaken(ServerPlayer p, String recept) {
        String quest = RECEPT_STAP.get(recept);
        if (quest == null) {
            return true;
        }
        Quest q = Quests.van(quest);
        if (q == null || !Questlog.actief(p, quest)) {
            return false;
        }
        for (int i = 0; i < q.doelen().size(); i++) {
            if (q.doelen().get(i).type() == Quest.Doel.Type.VERZAMEL && !Questlog.doelKlaar(p, q, i)) {
                return true;
            }
        }
        return false;
    }

    // ---- listening -------------------------------------------------------------------------------------------------------------------

    /** MateriaalTas.GEGEVEN: a gathered material ("ambacht_oogst:erts|hout|kruid|korf|vis|duik"). */
    static void verzameld(ServerPlayer p, ItemStack s) {
        Materialen.Materiaal m = Materialen.van(s);
        if (m != null) {
            NpcApi.meld(p, "ambacht_oogst", m.soort().id);
        }
    }

    /** Werkplaats.maak: something was made ("ambacht_maak:&lt;recipe&gt;" and "ambacht_maak:&lt;soort&gt;_&lt;doel&gt;", e.g. sieraad_ring). */
    public static void gemaakt(ServerPlayer p, Recepten.Recept r) {
        NpcApi.meld(p, "ambacht_maak", r.id());
        NpcApi.meld(p, "ambacht_maak", r.soort() + "_" + r.doel());
        Questlog.kijkLive(p);   // (a story item in the bag: "go back to ...")
    }

    // ---- (tests) ---------------------------------------------------------------------------------------------------------------------

    /** (tests) What is wrong with the chains in the loaded quest data (empty = all right): missing quests, wrong zone, broken na links. */
    public static List<String> fouten() {
        List<String> l = new ArrayList<>();
        for (Keten k : KETENS) {
            Quest volgende = Quests.van(k.volgende());
            if (volgende == null) {
                l.add(k.id() + ": volgende " + k.volgende() + " bestaat niet");
            } else if (!volgende.na().equals(List.of(k.quests().get(k.quests().size() - 1)))) {
                l.add(k.id() + ": " + k.volgende() + " wacht niet op de laatste stap: " + volgende.na());
            }
            for (int i = 0; i < k.quests().size(); i++) {
                Quest q = Quests.van(k.quests().get(i));
                if (q == null) {
                    l.add(k.id() + ": quest " + k.quests().get(i) + " bestaat niet");
                    continue;
                }
                if (ZoneBoeken.zoneVan(q.id()) != k.zone()) {
                    l.add(q.id() + ": zone " + ZoneBoeken.zoneVan(q.id()) + " != " + k.zone());
                }
                if (!q.hoofd()) {
                    l.add(q.id() + ": geen hoofdquest");
                }
                if (i > 0 && !q.na().equals(List.of(k.quests().get(i - 1)))) {
                    l.add(q.id() + ": na " + q.na());
                }
            }
        }
        return l;
    }
}
