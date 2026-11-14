package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

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
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Het Vadsrijk zone 5 "Hoogvads" (T_BRIEF night round T), slice t-z5verhaal: the story of the capital on Moeder Wolkwal. The NPC
 * guhs ({@link Z5Npcs}), the scenes and the airship ({@link Z5Verhaal}), the detective work ({@link Z5Speurwerk}), the quest hooks
 * and secrets ({@link Z5Haken}), the finale hook for the bosses ({@link Z5Finale}) and the API for the other slices
 * ({@link Z5VerhaalApi}). It reuses zone 1's quest system: the quests are data in data/guhs/vadsrijk/npc/quests_z5.json. Texts,
 * quests, the detective data and FTB: tools/features/vadsrijk_t_z5verhaal.py. Every coordinate: {@link Z5VerhaalPlekken}.
 * <p>Also here: the zone-5 page of the Verkenningsboek and the dev commands {@code /guhs vadsrijk z5verhaal ...}.
 */
public final class Z5VerhaalRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        QUEST_ITEMS.put("speurdersboekje", ITEMS.registerItem("vr_z5_npc_speurdersboekje", Z5Speurwerk.Boekje::new, () -> new Item.Properties().stacksTo(1)));
        for (String id : new String[] {"bladzijde", "bevelbrief", "grauwe_brief", "wolkbes", "wolbol", "brieven", "flap", "kaasje"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z5_npc_" + id, () -> new Item.Properties().stacksTo(16)));
        }
    }

    public static final List<String> GEBIEDEN_Z5 = List.of("z5_hoogvads", "z5_luchthaven", "z5_staartpark", "z5_ooggaarde", "z5_wolkwaldroom");
    /** The airship destinations of the Hoogvads havenmeester, and "hoogvads" added to the other captains' lists. */
    static final List<String> HAVEN_REIZEN = List.of("zwamzoet", "kandijkom", "schemerwortel", "knuffelkamp");

    private Z5VerhaalRegistry() {
    }

    public static Item item(String id) {
        return QUEST_ITEMS.get(id).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Z5Npcs.init();
        Z5Haken.registreer();
        Z5Speurwerk.init();
        reizen();
        Voltooiing.registreer(new Voltooiing.ZoneBoek("zone5", "vadsrijk.voltooiing.zone.zone5", "vr_kenner_van_hoogvads", 90000, Z5VerhaalRegistry::boek));
        nl.juiced.guhs.world.TikWacht.post("Z5VerhaalRegistry:84", e -> {
            Z5Haken.tick(e.getServer());
            Z5Verhaal.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ServerStartedEvent e) -> {
            rollen();
            gebieden();
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
    }

    /** The havenmeester's lines, and Hoogvads on the other captains' lists (shown once zone 4 is done: QuestMotor.reisDoelen). */
    static void reizen() {
        QuestMotor.REIZEN.put("z5_havenmeester", HAVEN_REIZEN);
        for (String kapitein : List.of("kapitein", "luchtschipper", "z2_kapitein", "z3_kapitein")) {
            List<String> l = new ArrayList<>(QuestMotor.REIZEN.getOrDefault(kapitein, List.of()));
            if (!l.contains("hoogvads")) {
                l.add("hoogvads");
                QuestMotor.REIZEN.put(kapitein, List.copyOf(l));
            }
        }
    }

    /** Our roles; the guild's only when the professions slice has not bound them (a binding always wins). */
    static void rollen() {
        NpcRollen.koppelAls("z5_bewijsbord", (p, a) -> {
            Z5Speurwerk.bord(p, levend(p, "z5_inspecteur"));
            return true;
        });
        NpcRollen.koppelAls("z5_vitrine", (p, a) -> {
            vitrine(p);
            return true;
        });
        for (String a : List.of("gilde", "koken", "brouwen", "smeden", "naaien", "imkeren")) {
            String rol = "ambacht_" + a;
            if (!NpcRollen.gekoppeld(rol)) {
                NpcRollen.koppelAls(rol, (p, x) -> {
                    RijkGuh g = levend(p, a.equals("gilde") ? "z5_gildemeester" : "z5_leermeester_" + a);
                    Praat.scene(p, Z5Verhaal.SLEUTEL, List.of(new Praat.Regel(g, g == null ? "vadsrijk.npc.naam.z5_gildemeester" : "",
                            Z5Verhaal.Z + (a.equals("gilde") ? "ambacht.gilde" : "ambacht.binnenkort"))));
                    return true;
                });
            }
        }
    }

    /** The teaser's showcase: whichever teaser stands nearest shows its "binnenkort" weapon. */
    static void vitrine(ServerPlayer p) {
        RijkGuh zanger = levend(p, "z5_vadszanger"), ninja = levend(p, "z5_pluisninja");
        boolean z = ninja == null || zanger != null && zanger.distanceToSqr(p) <= ninja.distanceToSqr(p);
        String id = z ? "z5_vadszanger" : "z5_pluisninja";
        RijkGuh g = z ? zanger : ninja;
        Praat.scene(p, Z5Verhaal.SLEUTEL, List.of(new Praat.Regel(g, Z5Verhaal.Z + "vitrine.naam", Z5Verhaal.Z + "vitrine." + id + ".0"),
                new Praat.Regel(g, Z5Verhaal.Z + "vitrine.naam", Z5Verhaal.Z + "vitrine." + id + ".1")));
    }

    static RijkGuh levend(ServerPlayer p, String id) {
        return p.level() instanceof ServerLevel sl ? Npcs.levend(sl, id) : null;
    }

    /** Our fallback areas, only when t-z5wereld did not register them (it owns the real shapes). Small ones in front. */
    static void gebieden() {
        for (Z5VerhaalPlekken.Gebied g : Z5VerhaalPlekken.GEBIEDEN) {
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
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", ZoneBoeken.quests(5, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", ZoneBoeken.quests(5, Quest.Soort.ZIJ)));
        List<Ding> geheim = new ArrayList<>(ZoneBoeken.geheimen(Z5Haken.SCHATTEN, Z5VerhaalPlekken.SCHAT, "vadsrijk.z5verhaal.schat.naam.", GEBIEDEN_Z5));
        geheim.addAll(ZoneBoeken.geheimen("z5_brieven", Map.of("ringvinger", Z5VerhaalPlekken.POI.get("z5_kanselarij")), "vadsrijk.z5verhaal.brief.naam.",
                GEBIEDEN_Z5));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool",
                ZoneBoeken.geheimen(Z5Haken.KNUFFELS, Z5VerhaalPlekken.KNUFFEL, "vadsrijk.z5verhaal.knuffel.", GEBIEDEN_Z5)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                ZoneBoeken.baas("zilverstaart", "vadsrijk.z5verhaal.baas.zilverstaart", "z5_wolkwaldroom", false, Z5Haken.HQ08, Z5Finale.BAAS_SPION),
                ZoneBoeken.baas("grauwe_droom", "vadsrijk.z5verhaal.baas.droom", "z5_wolkwaldroom", false, Z5Haken.HQ08, Z5Finale.BAAS_DROOM))));
        List<WereldPlekken.Uitkijk> uit = new ArrayList<>();
        for (String id : Z5VerhaalPlekken.UITKIJK_IDS) {
            uit.add(new WereldPlekken.Uitkijk(id, Z5VerhaalPlekken.poi(id)));
        }
        l.add(new Categorie("uitkijk", T + "cat.uitkijk", "minecraft:spyglass", ZoneBoeken.uitkijken(uit, GEBIEDEN_Z5)));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire", ZoneBoeken.kampvuren(Z5VerhaalPlekken.KAMPVUREN, GEBIEDEN_Z5)));
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", ZoneBoeken.gebieden(GEBIEDEN_Z5)));
        return l;
    }

    // ---- dev commands: /guhs vadsrijk z5verhaal ... ------------------------------------------------------------------------------

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("z5verhaal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        // klaarzetten: zones 1-4 done (main quests + flags) and level 56; z5_hq01 starts
        z.then(Commands.literal("klaarzetten").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            klaarzetten(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone 1-4 klaar + level 56. z5_hq01 loopt: /guhs vadsrijk z5verhaal vlieg (aankomstfilm) "
                    + "of /guhs vadsrijk z5verhaal tp havenmeester."), false);
            return 1;
        }));
        z.then(Commands.literal("vlieg").executes(c -> {
            Z5Verhaal.vlieg(c.getSource().getPlayerOrException());
            return 1;
        }));
        // scenes
        for (String s : new String[] {"aankomst", "herinnering", "ontmasker", "spion", "droom", "ontwaken", "brief", "cliffhanger", "bord", "klaar"}) {
            z.then(Commands.literal("scene").then(Commands.literal(s).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                switch (s) {
                    case "aankomst" -> Z5Verhaal.aankomst(p);
                    case "herinnering" -> Z5Verhaal.herinnering(p);
                    case "ontmasker" -> Z5Verhaal.ontmasker(p);
                    case "spion" -> Z5Verhaal.droom(p, Z5Finale.Fase.SPION);
                    case "droom" -> Z5Verhaal.droom(p, Z5Finale.Fase.DROOM);
                    case "ontwaken" -> Z5Verhaal.ontwaken(p);
                    case "brief" -> {
                        Questlog.map(p, Z5Haken.GEGEVEN).remove("grauwe_brief");
                        Z5Verhaal.brief(p);
                    }
                    case "cliffhanger" -> Z5Verhaal.cliffhanger(p);
                    case "bord" -> Z5Speurwerk.bord(p, null);
                    default -> Z5Verhaal.zone5Klaar(p);
                }
                return 1;
            })));
        }
        // quest skip: finish every main quest before the given one (1..10) and start it
        z.then(Commands.literal("quest").then(Commands.argument("stap", IntegerArgumentType.integer(1, 10)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "stap");
            naarStap(p, n);
            c.getSource().sendSuccess(() -> Component.literal("Zone 5: hoofdquest " + n + " (" + Z5Haken.HOOFD.get(n - 1) + ") loopt nu."), false);
            return 1;
        })));
        // the current objective done at once (the tracker's step)
        z.then(Commands.literal("stap").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String wat = stapKlaar(p);
            c.getSource().sendSuccess(() -> Component.literal(wat), false);
            return 1;
        }));
        z.then(Commands.literal("bewijs").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z5Speurwerk.allesGevonden(p);
            c.getSource().sendSuccess(() -> Component.literal("Alle 10 sporen in je Speurdersboekje."), false);
            return 1;
        }));
        z.then(Commands.literal("verhoor").then(Commands.argument("wie", StringArgumentType.word()).suggests((c, b) -> {
            Z5Speurwerk.VERHOREN.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z5Speurwerk.Verhoor v = Z5Speurwerk.VERHOREN.get(StringArgumentType.getString(c, "wie"));
            if (v != null) {
                Z5Speurwerk.verhoor(p, levend(p, v.npc()), v);
            }
            return v == null ? 0 : 1;
        })));
        // npc tp: to an NPC's spot (or a POI)
        z.then(Commands.literal("tp").then(Commands.argument("wie", StringArgumentType.word()).suggests((c, b) -> {
            Z5VerhaalPlekken.NPC.keySet().forEach(k -> b.suggest(k.substring(3)));
            Z5VerhaalPlekken.POI.keySet().forEach(k -> b.suggest(k.substring(3)));
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String wie = "z5_" + StringArgumentType.getString(c, "wie");
            BlockPos b = Z5VerhaalPlekken.NPC.containsKey(wie) ? Z5VerhaalPlekken.NPC.get(wie) : Z5VerhaalPlekken.POI.get(wie);
            if (b == null || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
                c.getSource().sendFailure(Component.literal("Onbekend (of je bent niet in het Vadsrijk): " + wie));
                return 0;
            }
            p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 1.5, Npcs.vloer(sl, b), b.getZ() + 0.5), Vec3.ZERO, p.getYRot(), 0f,
                    TeleportTransition.DO_NOTHING));
            return 1;
        })));
        z.then(Commands.literal("finale").executes(c -> {
            Z5Finale.start(c.getSource().getPlayerOrException());
            return 1;
        }));
        z.then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            vergeet(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone 5 vergeten (quests, sporen, geheimen, scenes)."), false);
            return 1;
        }));
        event.getDispatcher().register(nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando.wortel(z));
    }

    /** Zones 1-4 done (their main quests + flags), level 56, z5_hq01 started. */
    static void klaarzetten(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.soort() == Quest.Soort.HOOFD && !q.id().startsWith("z5_") && !Questlog.klaar(p, q.id())) {
                if (Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                Questlog.voltooi(p, q);
            }
        }
        for (String vlag : List.of("zone1_klaar", "zone2_klaar", "zone3_klaar", Z5Verhaal.VLAG_ZONE4)) {
            Questlog.zetVlag(p, vlag, true);
        }
        if (Spelers.level(p) < 56) {
            Spelers.zetLevel(p, 56);
        }
        Z5Haken.start(p);
    }

    /** Every main quest before step n done, step n taken. */
    static void naarStap(ServerPlayer p, int n) {
        klaarzetten(p);
        for (int i = 0; i < n - 1; i++) {
            Quest q = Quests.van(Z5Haken.HOOFD.get(i));
            if (q != null && !Questlog.klaar(p, q.id())) {
                if (Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                Questlog.voltooi(p, q);
            }
        }
        if (n >= 3) {   // (the clues of the skipped steps)
            for (String b : List.of("haren", "theevlek")) {
                Z5Speurwerk.vindStil(p, b);
            }
        }
        if (n >= 4) {
            Z5Speurwerk.vindStil(p, "bevelbrief");
            Z5Speurwerk.vindStil(p, "soepkom");
        }
        if (n >= 5) {
            Z5Speurwerk.vindStil(p, "kaasvoorraad");
            Z5Speurwerk.vindStil(p, "staart");
        }
        if (n >= 6) {
            Z5Speurwerk.vindStil(p, "logboek");
            Z5Speurwerk.vindStil(p, "zegel");
        }
        if (n >= 7) {
            Z5Speurwerk.allesGevonden(p);
        }
        if (n >= 2) {
            Spelers.data(p).gezien.add("npc:z5_aankomst");
        }
        Quest q = Quests.van(Z5Haken.HOOFD.get(n - 1));
        if (q != null && Questlog.status(p, q.id()) == 0) {
            Questlog.neemAan(p, q);
        }
    }

    /** Counts the first open objective of the first active zone-5 quest (dev: "skip this step"). */
    static String stapKlaar(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            if (!q.id().startsWith("z5_")) {
                continue;
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                if (Questlog.doelKlaar(p, q, i)) {
                    continue;
                }
                Quest.Doel d = q.doelen().get(i);
                int nodig = d.n() - Questlog.waarde(p, q, i);
                Questlog.tel(p, d.type(), x -> x == d, nodig);
                return "Klaar: " + q.id() + " stap " + i + " (" + d.type() + " " + d.doel() + "). Lever in bij " + q.inleveren() + ".";
            }
            return q.id() + ": alles klaar, lever in bij " + q.inleveren() + ".";
        }
        return "Geen actieve zone-5 quest.";
    }

    static void vergeet(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.id().startsWith("z5_")) {
                Questlog.vergeet(p, q.id());
            }
        }
        Questlog.zetVlag(p, Z5Verhaal.VLAG_ZONE5, false);
        Z5Verhaal.vergeet(p);
        Z5Haken.vergeet(p);
        Z5Speurwerk.vergeet(p);
        p.sendSystemMessage(Component.literal("(de aankomstfilm speelt weer bij de volgende vlucht)").withStyle(ChatFormatting.GRAY));
    }
}
