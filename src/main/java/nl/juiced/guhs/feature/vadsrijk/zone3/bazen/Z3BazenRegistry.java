package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart.BaronBleu;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart.Nestwachter;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart.Wortelwurger;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.piepvel.Piepvel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.roofvogel.Kruinroofvogel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Brokkel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Roquefort;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Taleggio;

/**
 * Het Vadsrijk zone 3 "De Schemerwortel", slice "bazen" (Z3_BRIEF): the Kruinroofvogel (world boss, chase phase on your mount),
 * De Kaaszeppelinwerf (Taleggio, Brokkel, Kapitein Roquefort + rooms), Graaf Piepvel in the Pelszaal (cages, the arm of the
 * Keizer), Het Hart van de Boom (Wortelwurger, the Oude Nestwachter, Baron Bleu + Schimmelkolos + rooms), Vahoegnir (the
 * splinters, the egg, the beam, the throne room), the Vahoeg mode, the loot, the pets and the dev commands
 * ({@code /guhs vadsrijk z3bazen ...}). Ids {@code vr_z3_baas_*}, lang {@code vadsrijk.z3bazen.*}, generator
 * tools/features/vadsrijk_z3_bazen.py (+ _modellen.py).
 */
public final class Z3BazenRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String ARENA_ROOF = "z3bazen:kruinroofvogel", ARENA_TALEGGIO = "z3bazen:taleggio", ARENA_BROKKEL = "z3bazen:brokkel",
            ARENA_ROQUEFORT = "z3bazen:roquefort", ARENA_PIEPVEL = "z3bazen:piepvel", ARENA_WURGER = "z3bazen:wortelwurger",
            ARENA_WACHTER = "z3bazen:nestwachter", ARENA_BLEU = "z3bazen:bleu", ARENA_NEST = "z3bazen:vahoegnir";

    public static final DeferredHolder<EntityType<?>, EntityType<Kruinroofvogel>> KRUINROOFVOGEL = baas("vr_z3_baas_kruinroofvogel", Kruinroofvogel::new, 2.6f, 3.2f, 2.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Taleggio>> TALEGGIO = baas("vr_z3_baas_taleggio", Taleggio::new, 1.5f, 1.7f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<Brokkel>> BROKKEL = baas("vr_z3_baas_brokkel", Brokkel::new, 1.5f, 1.7f, 1.3f);
    public static final DeferredHolder<EntityType<?>, EntityType<Roquefort>> ROQUEFORT = baas("vr_z3_baas_roquefort", Roquefort::new, 1.5f, 1.8f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Piepvel>> PIEPVEL = baas("vr_z3_baas_piepvel", Piepvel::new, 1.5f, 1.9f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Wortelwurger>> WORTELWURGER = baas("vr_z3_baas_wortelwurger", Wortelwurger::new, 3.0f, 3.4f, 2.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Nestwachter>> NESTWACHTER = baas("vr_z3_baas_nestwachter", Nestwachter::new, 2.2f, 3.8f, 3.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<BaronBleu>> BLEU = baas("vr_z3_baas_bleu", BaronBleu::new, 1.5f, 1.8f, 1.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Vahoegnir>> VAHOEGNIR = baas("vr_z3_baas_vahoegnir", Vahoegnir::new, 4.0f, 3.6f, 3.0f);

    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KOOI = baas("vr_z3_baas_kooi", Z3Doel::new, 0.9f, 1.0f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> BALLON = baas("vr_z3_baas_ballon", Z3Doel::new, 1.4f, 1.5f, 0.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> VOEDINGSWORTEL = baas("vr_z3_baas_voedingswortel", Z3Doel::new, 1.2f, 2.2f, 1.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KANON = baas("vr_z3_baas_kanon", Z3Doel::new, 1.4f, 1.2f, 0.8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KETEL = baas("vr_z3_baas_ketel", Z3Doel::new, 1.4f, 1.8f, 1.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KOLOS = baas("vr_z3_baas_schimmelkolos", Z3Doel::new, 3.2f, 4.6f, 3.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Hulpje>> MATROOS = baas("vr_z3_baas_matroos", Z3Hulpje::new, 0.9f, 1.1f, 0.85f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Hulpje>> SCHIMMELBOL = baas("vr_z3_baas_schimmelbol", Z3Hulpje::new, 0.8f, 0.8f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Hulpje>> KLODDERTJE = baas("vr_z3_baas_kloddertje", Z3Hulpje::new, 0.7f, 0.6f, 0.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Figurant>> FIGURANT = ENTITIES.register("vr_z3_baas_figurant",
            () -> EntityType.Builder.<Z3Figurant>of(Z3Figurant::new, MobCategory.MISC).sized(0.5f, 0.5f).noSave().fireImmune().clientTrackingRange(12)
                    .updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_z3_baas_figurant"))));

    private static BlockBehaviour.Properties vast(Block kopie) {
        return BlockBehaviour.Properties.ofFullCopy(kopie).strength(-1, 3600000).noLootTable();
    }

    public static final DeferredBlock<Z3Blokken.Lantaarnpaal> LANTAARNPAAL = BLOCKS.registerBlock("vr_z3_baas_lantaarnpaal", Z3Blokken.Lantaarnpaal::new,
            () -> vast(Blocks.SPRUCE_FENCE).noOcclusion().lightLevel(s -> s.getValue(Z3Blokken.Lantaarnpaal.LIT) ? 15 : 0));
    public static final DeferredBlock<Z3Blokken.Altaar> ALTAAR = BLOCKS.registerBlock("vr_z3_baas_altaar", Z3Blokken.Altaar::new,
            () -> vast(Blocks.CALCITE).noOcclusion().sound(SoundType.AMETHYST).lightLevel(s -> 9));
    public static final DeferredBlock<Block> POORT = BLOCKS.registerBlock("vr_z3_baas_poort", Block::new, () -> vast(Blocks.MUD_BRICKS).lightLevel(s -> 2));
    public static final DeferredBlock<Z3Blokken.Hendel> HENDEL = BLOCKS.registerBlock("vr_z3_baas_hendel", Z3Blokken.Hendel::new,
            () -> vast(Blocks.SPRUCE_PLANKS).noOcclusion());
    public static final DeferredBlock<Z3Blokken.Ringknop> RINGKNOP = BLOCKS.registerBlock("vr_z3_baas_ringknop", Z3Blokken.Ringknop::new,
            () -> vast(Blocks.STRIPPED_OAK_WOOD));
    public static final DeferredBlock<Block> NERF = BLOCKS.registerBlock("vr_z3_baas_nerf", Block::new, () -> vast(Blocks.PINK_CONCRETE).lightLevel(s -> 12));
    public static final DeferredBlock<Z3Blokken.Nesttak> NESTTAK = BLOCKS.registerBlock("vr_z3_baas_nesttak", Z3Blokken.Nesttak::new,
            () -> vast(Blocks.STRIPPED_OAK_WOOD));

    /** The pets (G-PETS: the boss's first kill unlocks it). */
    public static final Huisdier PET_ROOFVOGEL = Huisdieren.registreer(new Huisdier("kruinroofvogel", "guhs:vr_z3_baas_kruinroofvogel", "vr_z3_baas_kruinroofvogel",
            "textures/vadsrijk/z3bazen/kruinroofvogel.png", "vr_z3_baas_kruinroofvogel", 0.2f, "textures/vadsrijk/z3bazen/kruinroofvogel_glowmask.png"));
    public static final Huisdier PET_PIEPJE = Huisdieren.registreer(new Huisdier("piepje", "guhs:vr_z3_baas_piepvel", "pieppiepmuisje",
            "textures/entity/pieppiepmuisje.png", "vr_huisdier_piepje", 1.0f, null));
    public static final Huisdier PET_VAHOEGJE = Huisdieren.registreer(new Huisdier("vahoegje", "guhs:vr_z3_baas_vahoegnir", "vadsrijk/wezens/draak",
            "textures/vadsrijk/z3bazen/vahoegje.png", "vr_huisdier_vahoegje", 0.12f, null));

    private Z3BazenRegistry() {
    }

    private static <T extends VadsrijkMob> DeferredHolder<EntityType<?>, EntityType<T>> baas(String id, EntityType.EntityFactory<T> f, float w, float h, float oog) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(oog).clientTrackingRange(16)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static void register(IEventBus modBus) {
        Z3BazenLoot.BENOEMD.size();   // (the item holders exist before ITEMS is registered)
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : List.of(KRUINROOFVOGEL, TALEGGIO, BROKKEL, ROQUEFORT, PIEPVEL, WORTELWURGER, NESTWACHTER, BLEU, VAHOEGNIR)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            for (var t : List.of(KOOI, BALLON, VOEDINGSWORTEL, KANON, KETEL, KOLOS)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
            for (var t : List.of(MATROOS, SCHIMMELBOL, KLODDERTJE)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32).build());
            }
        });
        Z3BazenLoot.typen();
        arenas();
        Z3BazenEffecten.register();
        Rijdieren.RIJDEN_IN_ARENA.add(Z3BazenApi::magRijden);
        Flauwvallen.CHECKPOINT_KIEZER.add(p -> {
            for (BlockPos z : List.of(Z3BazenPlekken.ROOF_ZIT_1, Z3BazenPlekken.ROOF_ZIT_2)) {
                if (p.distanceToSqr(z.getX(), z.getY(), z.getZ()) < 40 * 40) {
                    return Z3BazenPlekken.ROOF_DEUR;
                }
            }
            if (Arenas.op(p.getX(), p.getY(), p.getZ()) == null) {
                if (Z3Kerkers.inWerf(p.getX(), p.getY(), p.getZ()) && p.getY() > Z3BazenPlekken.WERF_Y0 - 60 && p.getY() < Z3BazenPlekken.WERF_Y1 + 10) {
                    return Z3BazenPlekken.WERF_INGANG;
                }
                double dx = p.getX() - Z3BazenPlekken.STAM.getX(), dz = p.getZ() - Z3BazenPlekken.STAM.getZ();
                if (dx * dx + dz * dz < Math.pow(Z3BazenPlekken.HART_STRAAL_BOX, 2) && p.getY() >= Z3BazenPlekken.HART_Y0 && p.getY() <= Z3BazenPlekken.HART_Y1) {
                    return Z3BazenPlekken.HART_INGANG;
                }
            }
            return null;
        });
        NeoForge.EVENT_BUS.addListener(Z3BazenRegistry::tick);
        NeoForge.EVENT_BUS.addListener(Z3BazenRegistry::commando);
    }

    private static <T extends Z3Baas> void arena(String id, BlockPos c, int r, BlockPos deur, int respawn, BlockPos bord, String titel, int lv, Element e,
            Function<ServerLevel, T> maak) {
        Arenas.registreer(new Arena(id, c, r, deur, respawn, bord, titel), sl -> {
            T b = maak.apply(sl);
            if (b != null) {
                b.stelIn(lv, e, false, Set.of());
                VahoegModus.pas(sl, b);
            }
            return b;
        });
    }

    private static void arenas() {
        arena(ARENA_ROOF, Z3BazenPlekken.ROOF_CENTRUM, Z3BazenPlekken.ROOF_STRAAL, Z3BazenPlekken.ROOF_DEUR, Z3BazenPlekken.ROOF_RESPAWN, Z3BazenPlekken.ROOF_BORD,
                "vadsrijk.z3bazen.kruinroofvogel", 38, Element.NATUUR, sl -> KRUINROOFVOGEL.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_TALEGGIO, Z3BazenPlekken.TALEGGIO_CENTRUM, Z3BazenPlekken.D1_STRAAL, Z3BazenPlekken.TALEGGIO_DEUR, Z3BazenPlekken.D1_RESPAWN,
                Z3BazenPlekken.TALEGGIO_BORD, "vadsrijk.z3bazen.taleggio", 38, Element.NATUUR, sl -> TALEGGIO.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_BROKKEL, Z3BazenPlekken.BROKKEL_CENTRUM, Z3BazenPlekken.D1_STRAAL, Z3BazenPlekken.BROKKEL_DEUR, Z3BazenPlekken.D1_RESPAWN,
                Z3BazenPlekken.BROKKEL_BORD, "vadsrijk.z3bazen.brokkel", 39, Element.VUUR, sl -> BROKKEL.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_ROQUEFORT, Z3BazenPlekken.ROQUEFORT_CENTRUM, Z3BazenPlekken.ROQUEFORT_STRAAL, Z3BazenPlekken.ROQUEFORT_DEUR, Z3BazenPlekken.D1_RESPAWN,
                Z3BazenPlekken.ROQUEFORT_BORD, "vadsrijk.z3bazen.roquefort", 40, Element.VUUR, sl -> ROQUEFORT.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_PIEPVEL, Z3BazenPlekken.PIEP_CENTRUM, Z3BazenPlekken.PIEP_STRAAL, Z3BazenPlekken.PIEP_DEUR, Z3BazenPlekken.PIEP_RESPAWN, Z3BazenPlekken.PIEP_BORD,
                "vadsrijk.z3bazen.piepvel", 42, Element.WATER, sl -> PIEPVEL.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_WURGER, Z3BazenPlekken.WURGER_CENTRUM, Z3BazenPlekken.D2_STRAAL, Z3BazenPlekken.WURGER_DEUR, Z3BazenPlekken.D2_RESPAWN,
                Z3BazenPlekken.WURGER_BORD, "vadsrijk.z3bazen.wortelwurger", 49, Element.NATUUR, sl -> WORTELWURGER.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_WACHTER, Z3BazenPlekken.WACHTER_CENTRUM, Z3BazenPlekken.D2_STRAAL, Z3BazenPlekken.WACHTER_DEUR, Z3BazenPlekken.D2_RESPAWN,
                Z3BazenPlekken.WACHTER_BORD, "vadsrijk.z3bazen.nestwachter", 49, Element.NATUUR, sl -> NESTWACHTER.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(ARENA_BLEU, Z3BazenPlekken.BLEU_CENTRUM, Z3BazenPlekken.BLEU_STRAAL, Z3BazenPlekken.BLEU_DEUR, Z3BazenPlekken.D2_RESPAWN,
                Z3BazenPlekken.BLEU_BORD, "vadsrijk.z3bazen.bleu", 50, Element.WATER, sl -> BLEU.get().create(sl, EntitySpawnReason.SPAWNER));
        // Vahoegnir: only when the story or the Droomsteen asks (Z3BazenApi.startVahoegnir); the board hangs out of sight
        Arenas.registreer(new Arena(ARENA_NEST, Z3BazenPlekken.NEST_CENTRUM, Z3BazenPlekken.NEST_STRAAL, Z3BazenPlekken.NEST_DEUR, 10,
                Z3BazenPlekken.NEST_CENTRUM.below(40), "vadsrijk.z3bazen.vahoegnir"), Z3BazenApi::maakVahoegnir);
    }

    private static void tick(ServerTickEvent.Post e) {
        Z3Vast.tick(e.getServer());
        Z3Scenes.tick(e.getServer());
        Z3Kerkers.tick(e.getServer());
    }

    // ---- dev commands -------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static ServerLevel rijk(CommandContext<CommandSourceStack> c) {
        return Vadsrijk.level(c.getSource().getServer());
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static void naar(ServerPlayer p, ServerLevel sl, BlockPos b) {
        sl.getChunk(b);
        p.teleportTo(sl, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
    }

    private static VadsrijkBaas dichtste(ServerPlayer p) {
        return p.level().getEntitiesOfClass(VadsrijkBaas.class, p.getBoundingBox().inflate(64), b -> !b.geplofd()).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    private static final List<String> PLEKKEN = List.of("kruinroofvogel", "werf", "taleggio", "brokkel", "loods", "roquefort", "piepvel", "hart", "aderloop",
            "wortelwurger", "kwekerij", "nestwachter", "ringen", "bleu", "nest");

    private static BlockPos plek(String w) {
        return switch (w) {
            case "kruinroofvogel" -> Z3BazenPlekken.ROOF_DEUR;
            case "werf" -> Z3BazenPlekken.WERF_INGANG;
            case "taleggio" -> Z3BazenPlekken.TALEGGIO_BORD;
            case "brokkel" -> Z3BazenPlekken.BROKKEL_BORD;
            case "loods" -> Z3BazenPlekken.LOODS.offset(0, 0, -4);
            case "roquefort" -> Z3BazenPlekken.ROQUEFORT_BORD;
            case "piepvel" -> Z3BazenPlekken.PIEP_DEUR;
            case "aderloop" -> Z3BazenPlekken.ADERLOOP;
            case "wortelwurger" -> Z3BazenPlekken.WURGER_BORD;
            case "kwekerij" -> Z3BazenPlekken.KWEKERIJ.offset(-10, 0, 0);
            case "nestwachter" -> Z3BazenPlekken.WACHTER_BORD;
            case "ringen" -> Z3BazenPlekken.RINGEN.offset(-9, 0, -6);
            case "bleu" -> Z3BazenPlekken.BLEU_BORD;
            case "nest" -> Z3BazenPlekken.NEST_DEUR;
            default -> Z3BazenPlekken.HART_INGANG;
        };
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("z3bazen");
        b.then(dev("tp").then(Commands.argument("waar", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(PLEKKEN, s)).executes(c -> {
            naar(c.getSource().getPlayerOrException(), rijk(c), plek(StringArgumentType.getString(c, "waar")));
            return 1;
        })));
        b.then(dev("bouw").executes(c -> zeg(c, "Noodvloeren: " + Z3Bouw.alles(rijk(c)) + " blokken (alleen in lucht).")));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 4)).executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (!(baas instanceof Z3Baas z)) {
                return zeg(c, "Geen zone-3-baas in de buurt.");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            while (z.fase() < n && !z.geplofd()) {
                double[] f = z instanceof Vahoegnir ? new double[] {0.75, 0.5, 0.25} : z instanceof Kruinroofvogel ? new double[] {0.7, 0.35}
                        : z instanceof Taleggio ? new double[] {0.5} : z instanceof Piepvel ? new double[] {0.7, 0.4} : new double[] {0.66, 0.33};
                if (z.fase() - 1 >= f.length) {
                    break;
                }
                double doel = z.maxVads() * f[z.fase() - 1] - 1;
                if (z.vads() > doel) {
                    z.heal(doel - z.vads());
                }
                z.krijgSchade(1, null);
            }
            return zeg(c, "Fase nu " + z.fase());
        })));
        b.then(dev("vahoegnir").executes(c -> zeg(c, Z3BazenApi.startVahoegnir(c.getSource().getPlayerOrException(), false) ? "Vahoegnir landt in het oude nest."
                : "Hij is er al.")).then(Commands.literal("herinnering").executes(c -> zeg(c,
                        Z3BazenApi.startVahoegnir(c.getSource().getPlayerOrException(), true) ? "Een herinnering aan Vahoegnir..." : "Hij is er al."))));
        b.then(dev("vahoeg").then(Commands.argument("kerker", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("werf", "hart"), s))
                .executes(c -> {
                    String k = StringArgumentType.getString(c, "kerker");
                    VahoegModus.start(rijk(c), k, c.getSource().getPlayerOrException(), c.getSource().getPosition());
                    return zeg(c, "Vahoeg-modus aan in " + k + " (zegel: " + VahoegModus.zegel() + ").");
                })));
        b.then(dev("zegel").then(Commands.argument("naam", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("donker", "haastig", "splitsend", "taai", "week"), s)).executes(c -> {
                    String n = StringArgumentType.getString(c, "naam");
                    VahoegModus.testZegel = n.equals("week") ? null : VahoegModus.Zegel.valueOf(n.toUpperCase(java.util.Locale.ROOT));
                    return zeg(c, "Weekzegel: " + VahoegModus.zegel());
                })));
        b.then(dev("kerkers").then(Commands.literal("reset").executes(c -> {
            Z3Kerkers.reset(rijk(c));
            return zeg(c, "Kerkerkamers gereset.");
        })).then(Commands.literal("loods").executes(c -> {
            for (int i : new int[] {0, 2, 2}) {
                Z3Kerkers.draaiLoods(rijk(c), i);
            }
            return zeg(c, "Loods opgelost: " + Z3Kerkers.loodsOpgelost());
        })).then(Commands.literal("ringen").executes(c -> {
            for (int i : new int[] {0, 1, 1, 2, 2, 2, 2}) {
                Z3Kerkers.draaiRing(rijk(c), i);
            }
            return zeg(c, "Ringen opgelost: " + Z3Kerkers.ringenOpgelost());
        })));
        b.then(dev("loot").then(Commands.argument("baas", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(Z3BazenLoot.BAZEN, s))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String baas = StringArgumentType.getString(c, "baas");
                    BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                            Z3BazenLoot.kist(baas, p, 45, p.getRandom(), false, false));
                    return zeg(c, "Een buitkist van " + baas + " voor jou.");
                })));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Z3BazenLoot.Benoemd x : Z3BazenLoot.BENOEMD) {
                p.getInventory().add(Z3BazenLoot.maak(x, 45, p.getRandom()));
            }
            for (String t : Z3BazenLoot.BAZEN) {
                p.getInventory().add(Z3BazenLoot.trofee(t));
            }
            p.getInventory().add(new net.minecraft.world.item.ItemStack(Z3BazenLoot.VAHOEGZEGEL.get(), 4));
            p.getInventory().add(new net.minecraft.world.item.ItemStack(Z3BazenLoot.GOUDEN_VEER.get(), 25));
            return zeg(c, "Alle zone-3-baasitems, trofeeën, 4 Vahoegzegels en 25 Gouden Veren.");
        }));
        b.then(dev("herstel").executes(c -> {
            ServerLevel sl = rijk(c);
            int n = 0;
            for (String k : Herstel.sleutels(sl)) {
                if (k.startsWith("z3_")) {
                    n += Herstel.terug(sl, k);
                }
            }
            return zeg(c, "Hersteld: " + n + " blokken.");
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
