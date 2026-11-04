package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;

/**
 * Het Vadsrijk zone 3 "De Schemerwortel" (Z3_BRIEF.md), slice npc: the NPC guhs of Honingtak and the zone ({@link Z3Npcs}), the
 * zone-3 story scenes and the airship ({@link Z3Verhaal}), the quest hooks and secrets ({@link Z3Haken}), the roles/adapters
 * ({@link Z3Rollen}) and the API for the other zone-3 slices ({@link Z3NpcApi}). It reuses zone 1's quest system: the quests are data
 * in data/guhs/vadsrijk/npc/quests_z3.json. Texts, quests, FTB and the wiki: tools/features/vadsrijk_z3_npc.py.
 * <p>Also here: the special pet Vahoegje (the baby dragon from the egg, z3_hq14) and the zone-3 page of the Verkenningsboek.
 */
public final class Z3NpcRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final Map<String, DeferredItem<Item>> QUEST_ITEMS = new LinkedHashMap<>();
    static {
        for (String id : new String[] {"hartsplinter", "piepje", "nestlantaarn", "boomvolkkaart", "bleu_brief", "grauwe_brief", "drakenei", "moerasbloem",
                "mika_bevel"}) {
            QUEST_ITEMS.put(id, ITEMS.registerSimpleItem("vr_z3_npc_" + id, () -> new Item.Properties().stacksTo(id.equals("moerasbloem") ? 64 : 16)));
        }
        QUEST_ITEMS.put("honingthee", ITEMS.registerSimpleItem("vr_z3_npc_honingthee",
                () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build(),
                        net.minecraft.world.item.component.Consumables.defaultDrink().build())));
    }

    /** The baby dragon (special pet). K-INT: one registration only, z3-bazen's (it also unlocks on Vahoegnir's first "kill"). */
    public static final String VAHOEGJE = "vahoegje";
    public static final Huisdier VAHOEGJE_PET = nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry.PET_VAHOEGJE;

    public static final List<String> GEBIEDEN_Z3 = List.of("z3_honingtak", "z3_kruinzee", "z3_takkenbanen", "z3_zeppelinwerf", "z3_schorsstad",
            "z3_wortelvelden", "z3_wortelduister", "z3_hart");

    private Z3NpcRegistry() {
    }

    public static Item item(String id) {
        return QUEST_ITEMS.get(id).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Z3Npcs.init();
        Z3Haken.registreer();
        Voltooiing.registreer(new Voltooiing.ZoneBoek("zone3", "vadsrijk.voltooiing.zone.zone3", "vr_kenner_van_de_schemerwortel", 60000,
                Z3NpcRegistry::boek));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Z3Haken.tick(e.getServer());
            Z3Verhaal.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (ServerStartedEvent e) -> {
            Z3Rollen.koppel();
            Z3Koppelingen.registreer();   // K-INT: bazen moments, bindings, the one opening
            gebieden();
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> commando(e));
    }

    /** Our fallback areas, only when z3-wereld/z3-systemen did not register them (they own the y-aware ones). */
    static void gebieden() {
        for (Z3NpcPlekken.Gebied g : Z3NpcPlekken.GEBIEDEN) {
            if (Gebieden.van(g.id()) == null) {
                Gebieden.voegVooraan(new Gebieden.Gebied(g.id(), g.minX(), g.minZ(), g.maxX(), g.maxZ(), g.minLv(), g.maxLv(), g.stad()));
            }
        }
    }

    // ---- the Verkenningsboek page (i-voltooiing) ------------------------------------------------------------------------------------

    static List<Categorie> boek() {
        String T = ZoneBoeken.T;
        List<Categorie> l = new ArrayList<>();
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", ZoneBoeken.quests(3, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", ZoneBoeken.quests(3, Quest.Soort.ZIJ)));
        List<Ding> geheim = new ArrayList<>();
        geheim.addAll(ZoneBoeken.geheimen(Z3Haken.AANWIJZINGEN, Z3NpcPlekken.AANWIJZING, "vadsrijk.z3npc.aanwijzing.titel.", GEBIEDEN_Z3));
        geheim.addAll(ZoneBoeken.geheimen(Z3Haken.SCHATTEN, Z3NpcPlekken.SCHAT, "vadsrijk.z3npc.schat.naam.", GEBIEDEN_Z3));
        geheim.addAll(ZoneBoeken.geheimen(Z3Haken.RELIEFS, Z3NpcPlekken.RELIEF, "vadsrijk.z3npc.relief.titel.", GEBIEDEN_Z3));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool",
                ZoneBoeken.geheimen(Z3Haken.KNUFFELS, Z3NpcPlekken.KNUFFEL, "vadsrijk.z3npc.knuffel.", GEBIEDEN_Z3)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                ZoneBoeken.baas("roquefort", "vadsrijk.z3npc.baas.roquefort", "z3_zeppelinwerf", false, Z3Haken.HQ08, "guhs:vr_z3_baas_roquefort"),
                ZoneBoeken.baas("kruinroofvogel", "vadsrijk.z3npc.baas.kruinroofvogel", "z3_kruinzee", false, null, "guhs:vr_z3_baas_kruinroofvogel"),
                ZoneBoeken.baas("piepvel", "vadsrijk.z3npc.baas.piepvel", "z3_schorsstad", false, Z3Haken.HQ10, "guhs:vr_z3_baas_piepvel"),
                ZoneBoeken.baas("bleu", "vadsrijk.z3npc.baas.bleu", "z3_hart", false, Z3Haken.HQ13, "guhs:vr_z3_baas_bleu"),
                ZoneBoeken.baas("vahoegnir", "vadsrijk.z3npc.baas.vahoegnir", "z3_kruinzee", false, Z3Haken.HQ14, "guhs:vr_z3_baas_vahoegnir"),
                ZoneBoeken.baas("imker", "vadsrijk.z3npc.baas.imker", "z3_honingtak", true, "z3_geheim_imker"))));   // (K-INT: a lore scene, no boss entity)
        // K-INT: z3-wereld's lookouts + campfires (typed lists now that both slices are in one tree)
        l.add(new Categorie("uitkijk", T + "cat.uitkijk", "minecraft:spyglass",
                ZoneBoeken.uitkijken(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.UITKIJKEN, GEBIEDEN_Z3)));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire",
                ZoneBoeken.kampvuren(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.KAMPVUREN, GEBIEDEN_Z3)));
        l.add(new Categorie("huisdier", T + "cat.huisdier", "minecraft:lead", ZoneBoeken.huisdieren(3)));
        l.add(new Categorie("rijdier", T + "cat.rijdier", "minecraft:saddle", ZoneBoeken.rijdieren(RijdierSoort.PLUIMGUH)));
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", ZoneBoeken.gebieden(GEBIEDEN_Z3)));
        return l;
    }

    // ---- dev command: /guhs vadsrijk z3npc ... (via the dimension's own root if it exists, else /z3npc) ------------------------------

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("z3npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        String[] scenes = {"opening", "bam", "nest", "f1", "ei", "rit", "finale", "cliffhanger"};
        for (String s : scenes) {
            z.then(Commands.literal(s).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                switch (s) {
                    case "opening" -> Z3Verhaal.opening(p);
                    case "bam" -> Z3Verhaal.bam(p);
                    case "nest" -> Z3Verhaal.nest(p);
                    case "f1" -> Z3Verhaal.flashF1(p);
                    case "ei" -> Z3Verhaal.ei(p);
                    case "rit" -> Z3Verhaal.rit(p);
                    case "finale" -> Z3Verhaal.finale(p);
                    default -> Z3Verhaal.cliffhanger(p);
                }
                return 1;
            }));
        }
        // K-INT: ready for zone 3 (testing): the zone-2 story done (quests + flag zone2_klaar) and at least level 30
        z.then(Commands.literal("klaarzetten").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (nl.juiced.guhs.feature.vadsrijk.npc.Quest q : nl.juiced.guhs.feature.vadsrijk.npc.Quests.alle().values()) {
                if (q.soort() == Quest.Soort.HOOFD && q.id().startsWith("z2_") && !nl.juiced.guhs.feature.vadsrijk.npc.Questlog.klaar(p, q.id())) {
                    nl.juiced.guhs.feature.vadsrijk.npc.Questlog.voltooi(p, q);
                }
            }
            nl.juiced.guhs.feature.vadsrijk.npc.Questlog.zetVlag(p, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Verhaal.VLAG_ZONE2, true);
            if (nl.juiced.guhs.feature.vadsrijk.kern.Spelers.level(p) < 30) {
                nl.juiced.guhs.feature.vadsrijk.kern.Spelers.zetLevel(p, 30);
            }
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "Zone 2 klaar + level 30: praat met Kapitein Poolster in Kandijkom (z3_hq01) of bel de scheepsbel."), false);
            return 1;
        }));
        z.then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z3Verhaal.vergeet(p);
            Z3Haken.vergeet(p);
            return 1;
        }));
        event.getDispatcher().register(nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando.wortel(z));
    }
}
