package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtSchermen;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.opslag.Bank;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Het Vadsrijk zone 6 "Het Zijdeland" (U_BRIEF round U), slice u-z6verhaal: the story "De Sterrenkaravaan" (lv 60-70). The NPC
 * guhs ({@link Z6Npcs}), the scenes, travel and rewards ({@link Z6Verhaal}), the star beacons and other puzzles ({@link Z6Puzzels}),
 * the quest hooks and secrets ({@link Z6Haken}), airship line E ({@link Z6Lijn}) and the API for the other slices
 * ({@link Z6VerhaalApi}). It reuses zone 1's quest system: the quests are data in data/guhs/vadsrijk/npc/quests_z6.json. Texts,
 * quests, puzzles and FTB: tools/features/vadsrijk_u_z6verhaal.py. Every coordinate: {@link Z6VerhaalPlekken}.
 * <p>Also here: the zone-6 page of the Verkenningsboek, the bankers / teachers of both cities and the dev commands
 * {@code /guhs vadsrijk z6verhaal ...}.
 */
public final class Z6VerhaalRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        QUEST_ITEMS.put("sterrenlens", ITEMS.registerItem("vr_z6_npc_sterrenlens", Lens::new, () -> new Item.Properties().stacksTo(1)));
        for (String id : new String[] {"sterrenscherf", "sterrenkompas", "grauwe_brief", "cocon", "dadeltaart", "ingredient", "brief", "kwastje",
                "badeendje", "dadels", "zoutkreeftje", "tablet", "ring"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z6_npc_" + id, () -> new Item.Properties().stacksTo(16)));
        }
    }

    public static final List<String> GEBIEDEN_Z6 = List.of("z6_rozenkloof", "z6_saffraanpoort", "z6_roze_duinen", "z6_palmenkloven", "z6_zoutvlakte",
            "z6_ruinedal", "z6_moederoase");
    /** The airship destinations of Havenmeester Zandloper; "zijdeland" is added to the other captains' lists. */
    static final List<String> HAVEN_REIZEN = List.of("hoogvads", "lampionkust", "schemerwortel", "kandijkom", "zwamzoet", "knuffelkamp");
    /** Teachers -> their craft (they open t-ambacht's workshop / book like Hoogvads' guild: QuestMotor.EXTRA). */
    static final Map<String, Ambacht> LEERMEESTERS = Map.of("z6_leermeester_smeden", Ambacht.SMEDEN, "z6_leermeester_naaien", Ambacht.NAAIEN,
            "z6_leermeester_koken", Ambacht.KOKEN, "z6_leermeester_brouwen", Ambacht.BROUWEN);

    private Z6VerhaalRegistry() {
    }

    public static Item item(String id) {
        return QUEST_ITEMS.get(id).get();
    }

    /** The Sterrenlens: right click = how many beacons burn, which is next (and a glimpse of the night sky). */
    public static final class Lens extends Item {
        public Lens(Properties p) {
            super(p);
        }

        @Override
        public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                Z6Puzzels.lens(sp);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Z6Npcs.init();
        Z6Haken.registreer();
        Z6Puzzels.init();
        Z6Lijn.registreer();
        reizen();
        ambachten();
        Voltooiing.registreer(new Voltooiing.ZoneBoek("zone6", "vadsrijk.voltooiing.zone.zone6", "vr_kenner_van_het_zijdeland", 120000,
                Z6VerhaalRegistry::boek));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Z6Haken.tick(e.getServer());
            Z6Verhaal.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ServerStartedEvent e) -> {
            rollen();
            gebieden();
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
    }

    /** Zandloper's lines, and the Zijdeland on the other captains' lists (shown once line E is open: QuestMotor.reisDoelen). */
    static void reizen() {
        QuestMotor.REIZEN.put("z6_zandloper", HAVEN_REIZEN);
        for (String kapitein : List.of("kapitein", "luchtschipper", "z2_kapitein", "z3_kapitein", "z5_havenmeester")) {
            List<String> l = new ArrayList<>(QuestMotor.REIZEN.getOrDefault(kapitein, List.of()));
            if (!l.contains("zijdeland")) {
                l.add("zijdeland");
                QuestMotor.REIZEN.put(kapitein, List.copyOf(l));
            }
        }
    }

    /** The bankers of both cities (t-opslag) and the profession teachers (t-ambacht's workshops). */
    static void ambachten() {
        Bank.bankier("z6_bankier_kloof");
        Bank.bankier("z6_bankier_poort");
        LEERMEESTERS.forEach((npc, a) -> optie(npc, new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.werk", p -> true,
                p -> AmbachtSchermen.werkplaats(p, a, npc))));
        optie("z6_leermeester_imkeren", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.boek", p -> true,
                p -> AmbachtSchermen.boek(p, "recepten", Ambacht.IMKEREN)));
    }

    private static void optie(String npc, QuestMotor.ExtraOptie o) {
        QuestMotor.EXTRA.computeIfAbsent(npc, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(o);
    }

    /** Our roles (a binding of another slice always wins). */
    static void rollen() {
        NpcRollen.koppelAls("z6_tapijtwinkel", (p, a) -> {
            RijkGuh g = levend(p, "z6_franje");
            Praat.scene(p, Z6Verhaal.SLEUTEL, List.of(new Praat.Regel(g, g == null ? "vadsrijk.npc.naam.z6_franje" : "", Z6Verhaal.Z + "tapijtwinkel")));
            return true;
        });
    }

    static RijkGuh levend(ServerPlayer p, String id) {
        return p.level() instanceof ServerLevel sl ? Npcs.levend(sl, id) : null;
    }

    /** Our fallback areas, only when u-z6wereld did not register them (it owns the real shapes). Each goes in front of the earlier. */
    static void gebieden() {
        for (Z6VerhaalPlekken.Gebied g : Z6VerhaalPlekken.GEBIEDEN) {
            if (Gebieden.van(g.id()) == null) {
                Gebieden.voegVooraan(new Gebieden.Gebied(g.id(), g.minX(), g.minZ(), g.maxX(), g.maxZ(), g.minLv(), g.maxLv(), g.stad(),
                        Gebieden.Laag.y(g.minY(), g.maxY())));
            }
        }
    }

    // ---- the Verkenningsboek page (i-voltooiing) ------------------------------------------------------------------------------------

    static List<Categorie> boek() {
        String T = ZoneBoeken.T;
        List<Categorie> l = new ArrayList<>();
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", ZoneBoeken.quests(6, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", ZoneBoeken.quests(6, Quest.Soort.ZIJ)));
        List<Ding> geheim = new ArrayList<>(ZoneBoeken.geheimen(Z6Haken.SCHATTEN, Z6VerhaalPlekken.SCHAT, "vadsrijk.z6verhaal.schat.naam.", GEBIEDEN_Z6));
        geheim.addAll(ZoneBoeken.geheimen("z6_brieven", Map.of("ringvinger", Z6VerhaalPlekken.poi("z6_mikakamp")), "vadsrijk.z6verhaal.brief.naam.",
                GEBIEDEN_Z6));
        geheim.addAll(ZoneBoeken.geheimen(Z6Puzzels.BAKENS, Z6VerhaalPlekken.BAKEN, "vadsrijk.z6verhaal.bakens.", GEBIEDEN_Z6));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool",
                ZoneBoeken.geheimen(Z6Haken.KNUFFELS, Z6VerhaalPlekken.KNUFFEL, "vadsrijk.z6verhaal.knuffel.", GEBIEDEN_Z6)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                ZoneBoeken.baas("wormelien", "vadsrijk.z6verhaal.baas.wormelien", "z6_roze_duinen", false, "z6_s13_wormelien", "guhs:vr_z6_baas_wormelien"),
                ZoneBoeken.baas("zoutkolos", "vadsrijk.z6verhaal.baas.zoutkolos", "z6_zoutvlakte", false, null, "guhs:vr_z6_baas_zoutkolos"),
                ZoneBoeken.baas("sterrenguh", "vadsrijk.z6verhaal.baas.sterrenguh", "z6_ruinedal", false, Z6Haken.HQ08, "guhs:vr_z6_baas_sterrenguh"),
                ZoneBoeken.baas("ringeling", "vadsrijk.z6verhaal.baas.ringeling", "z6_moederoase", true, null, "guhs:vr_z6_baas_ringeling"))));
        List<WereldPlekken.Uitkijk> uit = new ArrayList<>();
        for (String id : Z6VerhaalPlekken.UITKIJK_IDS) {
            uit.add(new WereldPlekken.Uitkijk(id, Z6VerhaalPlekken.poi(id)));
        }
        l.add(new Categorie("uitkijk", T + "cat.uitkijk", "minecraft:spyglass", ZoneBoeken.uitkijken(uit, GEBIEDEN_Z6)));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire", ZoneBoeken.kampvuren(Z6VerhaalPlekken.KAMPVUREN, GEBIEDEN_Z6)));
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", ZoneBoeken.gebieden(GEBIEDEN_Z6)));
        return l;
    }

    // ---- dev commands: /guhs vadsrijk z6verhaal ... ------------------------------------------------------------------------------

    static final String[] SCENES = {"wolkwaltocht", "aankomst", "visioen", "zoutkolos", "kerker", "ontmasker", "ontsnapt", "luchtspiegeling", "bron",
            "toren", "ringeling", "f4", "brief", "klaar"};

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("z6verhaal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        // klaarzetten: zones 1-5 done (main quests + flags), level 60, line E open, z6_hq01 runs
        z.then(Commands.literal("klaarzetten").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            klaarzetten(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone 1-5 klaar + level 60 + lijn E open. z6_hq01 loopt: /guhs vadsrijk z6verhaal vlieg "
                    + "(aankomstfilm in Rozenkloof) of /guhs vadsrijk z6verhaal scene wolkwaltocht."), false);
            return 1;
        }));
        z.then(Commands.literal("vlieg").executes(c -> {
            Z6Verhaal.vlieg(c.getSource().getPlayerOrException());
            return 1;
        }));
        for (String s : SCENES) {
            z.then(Commands.literal("scene").then(Commands.literal(s).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                switch (s) {
                    case "wolkwaltocht" -> Z6Verhaal.wolkwaltocht(p);
                    case "aankomst" -> Z6Verhaal.aankomst(p);
                    case "visioen" -> Z6Verhaal.visioen(p, Math.max(1, Z6Puzzels.aantal(p)), p.blockPosition());
                    case "brief" -> {
                        Questlog.map(p, Z6Haken.GEGEVEN).remove("grauwe_brief");
                        Z6Verhaal.brief(p);
                    }
                    case "ringeling" -> Z6Verhaal.ringelingScene(p);
                    case "f4" -> Z6Verhaal.f4(p);
                    case "klaar" -> {
                        Questlog.zetVlag(p, Z6Verhaal.VLAG_ZONE6, false);
                        Z6Verhaal.zone6Klaar(p);
                    }
                    default -> Z6Verhaal.speel(p, s, null);
                }
                return 1;
            })));
        }
        // quest skip: finish every main quest before the given one (1..10) and start it
        z.then(Commands.literal("quest").then(Commands.argument("stap", IntegerArgumentType.integer(1, 10)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "stap");
            naarStap(p, n);
            c.getSource().sendSuccess(() -> Component.literal("Zone 6: hoofdquest " + n + " (" + Z6Haken.HOOFD.get(n - 1) + ") loopt nu."), false);
            return 1;
        })));
        // the current objective done at once (the tracker's step)
        z.then(Commands.literal("stap").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String wat = stapKlaar(p);
            c.getSource().sendSuccess(() -> Component.literal(wat), false);
            return 1;
        }));
        // a beacon: light it at once (with its vision), or open its puzzle here
        z.then(Commands.literal("baken").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z6VerhaalPlekken.BAKEN.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            if (!Z6VerhaalPlekken.BAKEN.containsKey(id)) {
                return 0;
            }
            Z6Puzzels.steekAan(p, id);
            return 1;
        })));
        z.then(Commands.literal("puzzel").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Z6Puzzels.laad();
            Z6Puzzels.PUZZELS.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            String baken = "";
            for (Map.Entry<String, String> e : Z6Puzzels.BAKEN_PUZZEL.entrySet()) {
                if (e.getValue().equals(id)) {
                    baken = e.getKey();
                }
            }
            Z6Puzzels.open(p, id, baken, null);
            return 1;
        })));
        // the raid's end (for u-z6raid's testing and the user): Ringeling cheered up -> her scene -> F4
        z.then(Commands.literal("ringeling").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Spelers.data(p).gezien.remove("npc:z6_ringeling");
            Questlog.map(p, Z6Haken.GEGEVEN).remove("ring");
            Z6VerhaalApi.ringelingOpgevrolijkt(p);
            return 1;
        }));
        // npc tp: to an NPC's spot (or a POI); "z6_" may be left out
        z.then(Commands.literal("tp").then(Commands.argument("wie", StringArgumentType.word()).suggests((c, b) -> {
            Z6VerhaalPlekken.NPC.keySet().forEach(k -> b.suggest(k.startsWith("z6_") ? k.substring(3) : k));
            Z6VerhaalPlekken.POI.keySet().forEach(k -> b.suggest(k.substring(3)));
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String wie = StringArgumentType.getString(c, "wie");
            BlockPos b = Z6VerhaalPlekken.NPC.containsKey(wie) ? Z6VerhaalPlekken.NPC.get(wie)
                    : Z6VerhaalPlekken.NPC.containsKey("z6_" + wie) ? Z6VerhaalPlekken.NPC.get("z6_" + wie) : Z6VerhaalPlekken.POI.get("z6_" + wie);
            if (b == null || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
                c.getSource().sendFailure(Component.literal("Onbekend (of je bent niet in het Vadsrijk): " + wie));
                return 0;
            }
            p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 1.5, Npcs.vloer(sl, b), b.getZ() + 0.5), Vec3.ZERO, p.getYRot(), 0f,
                    TeleportTransition.DO_NOTHING));
            return 1;
        })));
        z.then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            vergeet(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone 6 vergeten (quests, bakens, geheimen, scenes, lijn E, Ringeling)."), false);
            return 1;
        }));
        event.getDispatcher().register(nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando.wortel(z));
    }

    /** Zones 1-5 done (their main quests + flags), level 60, line E open, z6_hq01 started. */
    static void klaarzetten(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.soort() == Quest.Soort.HOOFD && !q.id().startsWith("z6_") && !Questlog.klaar(p, q.id())) {
                if (Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                Questlog.voltooi(p, q);
            }
        }
        for (String vlag : List.of("zone1_klaar", "zone2_klaar", "zone3_klaar", "zone4_klaar", "zone5_klaar")) {
            Questlog.zetVlag(p, vlag, true);
        }
        if (Spelers.level(p) < 60) {
            Spelers.zetLevel(p, 60);
        }
        Questlog.zetVlag(p, Z6Verhaal.VLAG_LIJN_E, true);
        Spelers.data(p).gezien.add("npc:z6_wolkwaltocht");
        Z6Haken.start(p);
        nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "z6", "wolkwaltocht");
    }

    /** Every main quest before step n done (with its side effects: beacons, flags), step n taken. */
    static void naarStap(ServerPlayer p, int n) {
        klaarzetten(p);
        for (int i = 0; i < n - 1; i++) {
            Quest q = Quests.van(Z6Haken.HOOFD.get(i));
            if (q != null && !Questlog.klaar(p, q.id())) {
                if (Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                Questlog.voltooi(p, q);
            }
        }
        if (n >= 2) {
            Spelers.data(p).gezien.add("npc:z6_aankomst");
        }
        String[] bakens = {"palm", "zout", "ruine", "duin", "oase"};
        int[] na = {6, 8, 9, 11, 11};   // (step after which each beacon burns: palm after hq05, zout after hq07, ruine after hq08)
        for (int i = 0; i < bakens.length; i++) {
            if (n >= na[i]) {
                Z6Puzzels.zetBrandend(p, bakens[i]);
            }
        }
        if (n >= 7) {
            Z6Verhaal.tapijt(p);
        }
        Quest q = Quests.van(Z6Haken.HOOFD.get(n - 1));
        if (q != null && Questlog.status(p, q.id()) == 0) {
            Questlog.neemAan(p, q);
        }
    }

    /** Counts the first open objective of the first active zone-6 quest (dev: "skip this step"). */
    static String stapKlaar(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            if (!q.id().startsWith("z6_")) {
                continue;
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                if (Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                Quest.Doel d = q.doelen().get(i);
                if (d.doel().startsWith("z6_baken:")) {
                    Z6Puzzels.zetBrandend(p, d.doel().substring(9));
                }
                int nodig = d.n() - Questlog.waarde(p, q, i);
                Questlog.tel(p, d.type(), x -> x == d, nodig);
                return "Klaar: " + q.id() + " stap " + i + " (" + d.type() + " " + d.doel() + "). Lever in bij " + q.inleveren() + ".";
            }
            return q.id() + ": alles klaar, lever in bij " + q.inleveren() + ".";
        }
        return "Geen actieve zone-6 quest.";
    }

    static void vergeet(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z6_")) {
                Questlog.vergeet(p, q.id());
            }
        }
        for (String vlag : List.of(Z6Verhaal.VLAG_ZONE6, Z6Verhaal.VLAG_LIJN_E, Z6Verhaal.VLAG_RINGELING, Z6Verhaal.VLAG_BULTGUH, Z6Verhaal.VLAG_TAPIJT,
                "z6_uiterlijk_z6_kwastjes", "z6_uiterlijk_z6_eerste_karavaan")) {
            Questlog.zetVlag(p, vlag, false);
        }
        Z6Verhaal.vergeet(p);
        Z6Haken.vergeet(p);
        Z6Puzzels.vergeet(p);
        p.sendSystemMessage(Component.literal("(de zwemtocht speelt weer zodra zone 5 klaar is en je vrij bent)").withStyle(ChatFormatting.GRAY));
    }
}
