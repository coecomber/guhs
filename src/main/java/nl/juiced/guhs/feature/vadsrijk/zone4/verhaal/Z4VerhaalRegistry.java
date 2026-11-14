package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
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
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;

/**
 * Het Vadsrijk zone 4 "De Lampionkust" (lv 50-56), night round T, slice t-z4verhaal: the story of zone 4.
 * <ul>
 *   <li>the main line "De Grote Knuffeltij" (7 quests, z4_hq01..07) and 16 side quests, as data in
 *       data/guhs/vadsrijk/npc/quests_z4.json (zone 1's quest system; texts + checks: tools/features/vadsrijk_t_z4verhaal.py);</li>
 *   <li>the NPC guhs of Lampionhaven and every island ({@link Z4Npcs}), the scenes ({@link Z4Verhaal}), the hooks, finds, small
 *       games and secrets ({@link Z4Haken}), the API for the other slices ({@link Z4VerhaalApi});</li>
 *   <li>the zone-4 page of the Verkenningsboek, the travel-map place "lampionkust", the flat fallback areas, the dev commands
 *       {@code /guhs vadsrijk z4verhaal ...}.</li>
 * </ul>
 */
public final class Z4VerhaalRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        for (String id : new String[] {"vlieger", "krans", "zeewier", "flesje", "knuffelzak", "lampion"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z4_npc_" + id, () -> new Item.Properties().stacksTo(id.equals("zeewier") ? 64 : 16)));
        }
        for (String id : new String[] {"limonade", "vissoep", "soepkom"}) {
            boolean drank = id.equals("limonade");
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z4_npc_" + id, () -> new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(drank ? 3 : 6).saturationModifier(drank ? 0.4f : 0.7f).alwaysEdible().build(),
                            drank ? net.minecraft.world.item.component.Consumables.defaultDrink().build()
                                    : net.minecraft.world.item.component.Consumables.defaultFood().build())));
        }
    }

    public static final List<String> GEBIEDEN_Z4 = List.of("z4_lampionhaven", "z4_knuffeltij", "z4_strandfeest", "z4_bloemeneiland", "z4_koraalbaai",
            "z4_vliegereiland", "z4_vuurtoreneiland", "z4_schildpad", "z4_mysterie", "z4_pierkop");   // (T-INT: t-z4wereld's ids)
    /** The travel-map place of zone 4 (npc/Reiskaart.PLEKKEN). */
    public static final String REISKAART = "lampionkust";

    private Z4VerhaalRegistry() {
    }

    public static Item item(String id) {
        return QUEST_ITEMS.get(id).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Z4Npcs.init();
        Z4Haken.registreer();
        reizen();
        Voltooiing.registreer(new Voltooiing.ZoneBoek("zone4", "vadsrijk.voltooiing.zone.zone4", "vr_kenner_van_de_lampionkust", 80000,
                Z4VerhaalRegistry::boek));
        nl.juiced.guhs.world.TikWacht.post("Z4VerhaalRegistry:97", e -> {
            Z4Haken.tick(e.getServer());
            Z4Verhaal.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ItemFishedEvent e) -> {
            if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p) {
                Z4Haken.gevist(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ServerStartedEvent e) -> {
            rollen();
            gebieden();
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
    }

    // ---- travel: Windvaan's airship at Lampionhaven, the travel-map place "lampionkust" -------------------------------------------

    static void reizen() {
        QuestMotor.REIZEN.put("z4_luchthaven", List.of("schemerwortel", "kandijkom", "zwamzoet", "knuffelkamp"));
        for (String npc : List.of("z3_kapitein", "kapitein", "luchtschipper", "z2_kapitein")) {
            QuestMotor.REIZEN.computeIfPresent(npc, (k, l) -> {
                List<String> n = new ArrayList<>(l);
                if (!n.contains(REISKAART)) {
                    n.add(REISKAART);
                }
                return List.copyOf(n);
            });
        }
    }

    // ---- roles: our own small services + fallbacks for the ones other slices own -----------------------------------------------------

    private static final Map<UUID, Long> GEHAD = new ConcurrentHashMap<>();

    static void rollen() {
        NpcRollen.koppel("luchtschip_z4", Reiskaart::open);
        koppelAlsLeeg("z4_zeilboot", p -> p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "rol.zeilboot.nog").withStyle(ChatFormatting.AQUA)));
        NpcRollen.koppel("z4_vissoep", p -> geef(p, "vissoep", "rol.vissoep"));
        NpcRollen.koppel("z4_limonade", p -> geef(p, "limonade", "rol.limonade"));
        NpcRollen.koppel("z4_soep", p -> geef(p, "soepkom", "rol.soep"));
    }

    private static void koppelAlsLeeg(String rol, java.util.function.Consumer<ServerPlayer> actie) {
        if (!NpcRollen.gekoppeld(rol)) {
            NpcRollen.koppel(rol, actie);
        }
    }

    /** A free snack from a kitchen NPC, once per 2 minutes per player. */
    static void geef(ServerPlayer p, String item, String tekst) {
        long nu = p.level().getGameTime();
        Long was = GEHAD.get(p.getUUID());
        if (was != null && nu - was < 20 * 120) {
            p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + "rol.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        GEHAD.put(p.getUUID(), nu);
        Questlog.geef(p, new ItemStack(item(item)));
        p.sendSystemMessage(Component.translatable(Z4Verhaal.Z + tekst).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Our fallback areas, only when t-z4wereld did not register them: added in reverse so the islands come before the coast. */
    static void gebieden() {
        if (true) {
            return;   // T-INT: t-z4wereld is in the tree and registers the real zone-4 areas (z4_*): no fallbacks (they shadowed them)
        }
        List<Z4VerhaalPlekken.Gebied> l = new ArrayList<>(Z4VerhaalPlekken.GEBIEDEN);
        java.util.Collections.reverse(l);
        for (Z4VerhaalPlekken.Gebied g : l) {
            if (Gebieden.van(g.id()) == null) {
                Gebieden.voegVooraan(new Gebieden.Gebied(g.id(), g.minX(), g.minZ(), g.maxX(), g.maxZ(), g.minLv(), g.maxLv(), g.stad()));
            }
        }
    }

    // ---- the Verkenningsboek page (i-voltooiing) ------------------------------------------------------------------------------------

    static List<Categorie> boek() {
        String T = ZoneBoeken.T;
        String Z = Z4Verhaal.Z;
        List<Categorie> l = new ArrayList<>();
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", ZoneBoeken.quests(4, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", ZoneBoeken.quests(4, Quest.Soort.ZIJ)));
        List<Voltooiing.Ding> geheim = new ArrayList<>();
        geheim.addAll(ZoneBoeken.geheimen(Z4Haken.SCHATTEN, Z4VerhaalPlekken.SCHAT, Z + "schat.naam.", GEBIEDEN_Z4));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("flessenpost", T + "cat.flessenpost", "minecraft:glass_bottle",
                ZoneBoeken.geheimen(Z4Haken.FLES, Z4VerhaalPlekken.FLESSENPOST, Z + "fles.titel.", GEBIEDEN_Z4)));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool",
                ZoneBoeken.geheimen(Z4Haken.KNUFFELS, Z4VerhaalPlekken.KNUFFEL, Z + "knuffel.", GEBIEDEN_Z4)));
        l.add(new Categorie("uitzicht", T + "cat.uitzicht", "minecraft:spyglass",
                ZoneBoeken.geheimen(Z4Haken.UITZICHTEN, Z4VerhaalPlekken.UITZICHT, Z + "uitzicht.titel.", GEBIEDEN_Z4)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                ZoneBoeken.baas("zeeslurf", Z + "baas.zeeslurf", "z4_koraalbaai", false, Z4Haken.S05, "guhs:vr_z4_baas_zeeslurf", "guhs:vr_z4_zeeslurf"),
                ZoneBoeken.baas("vliegervanger", Z + "baas.vliegervanger", "z4_wrak",   // (T-INT: his arena is in Het Wrak)
                false, Z4Haken.HQ05, "guhs:vr_z4_baas_vliegervanger",
                        "guhs:vr_z4_vliegervanger"))));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire",
                ZoneBoeken.kampvuren(List.copyOf(Z4VerhaalPlekken.KAMPVUUR.values()), GEBIEDEN_Z4)));
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", ZoneBoeken.gebieden(GEBIEDEN_Z4)));
        return l;
    }

    // ---- dev commands: /guhs vadsrijk z4verhaal ... ----------------------------------------------------------------------------------

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("z4verhaal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        // ready for zone 4: the zone-3 story done (quests + flag zone3_klaar) and at least level 50
        z.then(Commands.literal("klaarzetten").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            klaarzetten(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone 3 klaar + level 50: praat met Ordemeester Wasja in de Nestkapel (z4_hq01), "
                    + "of /guhs vadsrijk z4verhaal vlieg."), false);
            return 1;
        }));
        // jump to main step n (1..7): everything before it handed in, step n taken
        z.then(Commands.literal("hoofdlijn").then(Commands.argument("stap", IntegerArgumentType.integer(1, 7)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "stap");
            Quest q = naarStap(p, n);
            c.getSource().sendSuccess(() -> Component.literal("Hoofdlijn stap " + n + ": " + (q == null ? "?" : q.id()) + " loopt nu."), false);
            return 1;
        })));
        // all objectives of the running zone-4 main quest done (hand it in yourself)
        z.then(Commands.literal("skip").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Quest q : Questlog.actieve(p)) {
                if (q.id().startsWith("z4_") && q.hoofd()) {
                    vul(p, q);
                    c.getSource().sendSuccess(() -> Component.literal("Alle doelen van " + q.id() + " klaar: lever in bij " + q.inleveren()), false);
                    return 1;
                }
            }
            c.getSource().sendFailure(Component.literal("Geen lopende zone-4-hoofdquest."));
            return 0;
        }));
        // a quest of zone 4: start / all objectives / hand in at once
        z.then(Commands.literal("quest").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Quests.alle().keySet().stream().filter(k -> k.startsWith("z4_")), b))
                .then(Commands.literal("start").executes(c -> questDo(c.getSource(), StringArgumentType.getString(c, "id"), 0)))
                .then(Commands.literal("klaar").executes(c -> questDo(c.getSource(), StringArgumentType.getString(c, "id"), 1)))
                .then(Commands.literal("af").executes(c -> questDo(c.getSource(), StringArgumentType.getString(c, "id"), 2)))));
        // teleport to an NPC / POI / find spot of zone 4
        z.then(Commands.literal("tp").then(Commands.argument("plek", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(tpPlekken().keySet(), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos b = tpPlekken().get(StringArgumentType.getString(c, "plek"));
                    ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
                    if (b == null || sl == null) {
                        c.getSource().sendFailure(Component.literal("Onbekende plek."));
                        return 0;
                    }
                    p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 1.5, Npcs.vloer(sl, b.offset(1, 0, 0)), b.getZ() + 0.5), Vec3.ZERO, p.getYRot(),
                            p.getXRot(), TeleportTransition.DO_NOTHING));
                    return 1;
                })));
        // the scenes
        for (String s : new String[] {"opening", "vlieg", "mist", "feest", "wolkwal", "cliffhanger", "wrak", "zeeslurf"}) {
            z.then(Commands.literal(s).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                switch (s) {
                    case "opening" -> Z4Verhaal.opening(p);
                    case "vlieg" -> Z4Verhaal.vlieg(p);
                    case "mist" -> Z4Verhaal.mist(p);
                    case "feest" -> Z4Verhaal.feest(p);
                    case "wolkwal" -> Z4Verhaal.wolkwal(p);
                    case "cliffhanger" -> Z4Verhaal.cliffhanger(p);
                    case "wrak" -> Z4Verhaal.wrak(p);
                    default -> Z4Verhaal.zeeslurf(p);
                }
                return 1;
            }));
        }
        z.then(Commands.literal("status").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            c.getSource().sendSuccess(() -> Component.literal("zone4_klaar=" + Z4VerhaalApi.zone4Klaar(p) + " z5_luchtschip_open="
                    + Z4VerhaalApi.luchtschipNaarHoogvads(p) + " zeilboot=" + Z4VerhaalApi.heeftZeilboot(p) + " knuffeltij=" + Z4VerhaalApi.knuffeltij(p)
                    + "/12 knuffels=" + Questlog.map(p, Z4Haken.KNUFFELS).size() + "/8 flessenpost=" + Questlog.map(p, Z4Haken.FLES).size() + "/6"), false);
            return 1;
        }));
        z.then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z4Verhaal.vergeet(p);
            Z4Haken.vergeet(p);
            for (Quest q : Quests.alle().values()) {
                if (q.id().startsWith("z4_")) {
                    Questlog.vergeet(p, q.id());
                }
            }
            for (String v : List.of(Z4Verhaal.VLAG_ZONE4, Z4Verhaal.VLAG_Z5_LUCHTSCHIP, "z4_zeilboot")) {
                Questlog.zetVlag(p, v, false);
            }
            Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:z4_"));
            c.getSource().sendSuccess(() -> Component.literal("Zone 4 vergeten: quests, knuffels, geheimen, scènes."), false);
            return 1;
        }));
        event.getDispatcher().register(nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando.wortel(z));
    }

    /** Zone 3 done (its main quests + the flags of zones 2 and 3) and at least level 50. */
    static void klaarzetten(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.soort() == Quest.Soort.HOOFD && (q.id().startsWith("z2_") || q.id().startsWith("z3_")) && !Questlog.klaar(p, q.id())) {
                Questlog.voltooi(p, q);
            }
        }
        Questlog.zetVlag(p, "zone2_klaar", true);
        Questlog.zetVlag(p, "zone3_klaar", true);
        if (Spelers.level(p) < 50) {
            Spelers.zetLevel(p, 50);
        }
    }

    /** (dev/tests) Zone 3 done, main quests 1..n-1 of zone 4 handed in, quest n taken. */
    static Quest naarStap(ServerPlayer p, int n) {
        klaarzetten(p);
        int i = 0;
        Quest stap = null;
        for (Quest q : Quests.alle().values()) {
            if (!q.hoofd() || !q.id().startsWith("z4_")) {
                continue;
            }
            i++;
            if (i < n) {
                if (!Questlog.klaar(p, q.id())) {
                    Questlog.voltooi(p, q);
                }
                if (q.id().equals(Z4Haken.HQ02)) {
                    Questlog.zetVlag(p, "z4_zeilboot", true);
                }
            } else if (i == n) {
                Questlog.vergeet(p, q.id());
                Questlog.neemAan(p, q);
                stap = q;
            }
        }
        if (Spelers.level(p) < 49 + n) {
            Spelers.zetLevel(p, 49 + n);
        }
        return stap;
    }

    /** All objectives of a quest done (through the real counter). */
    static void vul(ServerPlayer p, Quest q) {
        for (Quest.Doel d : q.doelen()) {
            Questlog.tel(p, d.type(), x -> x == d, d.n());
        }
    }

    private static int questDo(CommandSourceStack src, String id, int wat) {
        ServerPlayer p;
        try {
            p = src.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return 0;
        }
        Quest q = Quests.van(id);
        if (q == null) {
            src.sendFailure(Component.literal("Onbekende quest: " + id));
            return 0;
        }
        if (wat == 0 || !Questlog.actief(p, id) && !Questlog.klaar(p, id)) {
            Questlog.vergeet(p, id);
            Questlog.neemAan(p, q);
        }
        if (wat >= 1) {
            vul(p, q);
        }
        if (wat == 2 && !Questlog.klaar(p, id)) {
            Questlog.voltooi(p, q);
            if (q.actieKlaar().startsWith("start:")) {
                QuestMotor.doeActie(p, q.actieKlaar(), null);
            }
        }
        src.sendSuccess(() -> Component.literal(id + (wat == 0 ? " gestart" : wat == 1 ? ": alle doelen klaar, lever in bij " + q.inleveren() : " afgerond")), false);
        return 1;
    }

    static Map<String, BlockPos> tpPlekken() {
        Map<String, BlockPos> m = new LinkedHashMap<>(Z4VerhaalPlekken.NPC);
        Z4VerhaalPlekken.POI.forEach((k, v) -> m.put("poi." + k, v));
        Z4VerhaalPlekken.TIJKNUFFEL.forEach((k, v) -> m.put("tij." + k, v));
        Z4VerhaalPlekken.WRAKKNUFFEL.forEach((k, v) -> m.put("tij." + k, v));
        Z4VerhaalPlekken.KNUFFEL.forEach((k, v) -> m.put("knuffel." + k, v));
        Z4VerhaalPlekken.FLESSENPOST.forEach((k, v) -> m.put("fles." + k, v));
        Z4VerhaalPlekken.SCHAT.forEach((k, v) -> m.put("schat." + k, v));
        Z4VerhaalPlekken.UITZICHT.forEach((k, v) -> m.put("uitzicht." + k, v));
        return m;
    }
}
