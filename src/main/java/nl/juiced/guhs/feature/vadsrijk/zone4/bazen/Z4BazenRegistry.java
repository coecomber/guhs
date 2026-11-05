package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.List;
import java.util.Set;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Toegang;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensCommando;

/**
 * Het Vadsrijk zone 4 "De Lampionkust", slice "t-z4wezens", the bosses: De Grauwe Zeeslurf (world boss in the Koraalbaai lagoon,
 * you cheer it up; diving for the polyps) and Kootje de Vliegervanger (end boss of the small dungeon "Het Wrak van de
 * Vliegervanger": the crew of Vliegerknechten on the rock is the trash, the winches bring his war kite down, the plushie rain),
 * their loot ({@link Z4BazenLoot}), 2 pets and the dev commands ({@code /guhs vadsrijk z4bazen ...}). Ids {@code vr_z4_baas_*},
 * lang {@code vadsrijk.z4bazen.*}, arenas {@code z4bazen:zeeslurf|vliegervanger}.
 */
public final class Z4BazenRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String ARENA_ZEESLURF = "z4bazen:zeeslurf", ARENA_WRAK = "z4bazen:vliegervanger";
    public static final int LEVEL_ZEESLURF = 54, LEVEL_WRAK = 56;

    private static <T extends VadsrijkMob> DeferredHolder<EntityType<?>, EntityType<T>> baas(String id, EntityType.EntityFactory<T> f, float w, float h, float oog) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(oog).clientTrackingRange(16)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Zeeslurf>> ZEESLURF = baas("vr_z4_baas_zeeslurf", Zeeslurf::new, 4.0f, 3.0f, 2.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Vliegervanger>> VLIEGERVANGER = baas("vr_z4_baas_vliegervanger", Vliegervanger::new, 1.0f, 2.2f, 1.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> POLIEP = baas("vr_z4_baas_poliep", Z3Doel::new, 1.2f, 1.6f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> LIER = baas("vr_z4_baas_lier", Z3Doel::new, 1.1f, 1.3f, 0.8f);

    /** The pets (first kill unlocks them): a tiny pink Zeeslurfje and the rescued war kite, now a happy little kite. */
    public static final Huisdier PET_ZEESLURFJE = Huisdieren.registreer(new Huisdier("zeeslurfje", "guhs:vr_z4_baas_zeeslurf", "vr_z4_baas_zeeslurf",
            "textures/vadsrijk/z4bazen/zeeslurf_blij.png", "vr_huisdier_zeeslurfje", 0.16f, null));
    public static final Huisdier PET_VLIEGERTJE = Huisdieren.registreer(new Huisdier("vliegertje", "guhs:vr_z4_baas_vliegervanger", "vr_huisdier_vliegertje",
            "textures/vadsrijk/z4bazen/vliegertje.png", "vr_huisdier_vliegertje", 0.6f, null));

    private Z4BazenRegistry() {
    }

    public static void register(IEventBus modBus) {
        Z4BazenLoot.BENOEMD.size();   // (the item holders exist before ITEMS is registered)
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : List.of(ZEESLURF, VLIEGERVANGER)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                        .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            }
            for (var t : List.of(POLIEP, LIER)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        Z4BazenLoot.typen();
        Z4BazenEffecten.register();
        arenas();
        NeoForge.EVENT_BUS.addListener(Z4BazenRegistry::commando);
    }

    private static void arenas() {
        Arenas.registreer(new Arena(ARENA_ZEESLURF, Z4BazenPlekken.ZEESLURF_CENTRUM, Z4BazenPlekken.ZEESLURF_STRAAL, Z4BazenPlekken.ZEESLURF_DEUR,
                Z4BazenPlekken.ZEESLURF_RESPAWN, Z4BazenPlekken.ZEESLURF_BORD, "vadsrijk.z4bazen.zeeslurf", Z4BazenPlekken.ZEESLURF_ONDER,
                Z4BazenPlekken.ZEESLURF_BOVEN), sl -> maak(sl, ZEESLURF.get(), LEVEL_ZEESLURF, Element.WATER));
        Arenas.registreer(new Arena(ARENA_WRAK, Z4BazenPlekken.WRAK_CENTRUM, Z4BazenPlekken.WRAK_STRAAL, Z4BazenPlekken.WRAK_DEUR, Z4BazenPlekken.WRAK_RESPAWN,
                Z4BazenPlekken.WRAK_BORD, "vadsrijk.z4bazen.vliegervanger", Z4BazenPlekken.WRAK_ONDER, Z4BazenPlekken.WRAK_BOVEN),
                sl -> maak(sl, VLIEGERVANGER.get(), LEVEL_WRAK, Element.VUUR));
    }

    private static VadsrijkBaas maak(ServerLevel sl, EntityType<? extends Z4Baas> t, int lv, Element e) {
        Z4Baas b = t.create(sl, EntitySpawnReason.SPAWNER);
        if (b != null) {
            b.stelIn(lv, e, false, Set.of());
        }
        return b;
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

    public static final List<String> BAZEN_DEV = List.of("zeeslurf", "vliegervanger");
    private static final List<String> PLEKKEN = List.of("zeeslurf", "lagune", "grotje", "wrak", "vliegervanger");

    private static BlockPos plek(String w) {
        return switch (w) {
            case "lagune" -> Z4BazenPlekken.ZEESLURF_CENTRUM.above(10);
            case "grotje" -> Z4BazenPlekken.ZEESLURF_GROTJE.above(1);
            case "wrak" -> Z4BazenPlekken.WRAK_DEUR.above(1);
            case "vliegervanger" -> Z4BazenPlekken.WRAK_BORD.above(1);
            default -> Z4BazenPlekken.ZEESLURF_DEUR.above(1);
        };
    }

    private static <T> T dichtste(ServerPlayer p, Class<T> soort) {
        return p.level().getEntitiesOfClass(VadsrijkBaas.class, p.getBoundingBox().inflate(64), b -> !b.geplofd() && soort.isInstance(b)).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).map(soort::cast).orElse(null);
    }

    private static String arena(String baas) {
        return "z4bazen:" + baas;
    }

    private static int spawn(CommandContext<CommandSourceStack> c, String baas) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = rijk(c);
        Arena a = Arenas.van(arena(baas));
        if (a == null) {
            return zeg(c, "Onbekende baas: " + baas + ". Kies uit " + BAZEN_DEV);
        }
        Z3Toegang.vergeet(p);
        sl.getChunk(a.centrum());
        Arenas.reset(sl.getServer(), a.id());
        naar(p, sl, plek(baas));
        return zeg(c, baas + " staat zo klaar (volle Vads) in arena " + a.id() + " rond " + a.centrum().toShortString() + " (straal " + a.straal() + ")."
                + (nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensPlekken.echt ? "" : " Let op: placeholder-plek; bouw eventueel eerst met /guhs vadsrijk z4bazen bouw."));
    }

    /** Dev only (a test world): a pool for the Zeeslurf (sand floor, water to y 62, a rim) and a plank deck for the Wrak. */
    private static int bouw(ServerLevel sl) {
        int n = 0;
        BlockPos c = Z4BazenPlekken.ZEESLURF_CENTRUM;
        BlockState zand = Blocks.SAND.defaultBlockState(), water = Blocks.WATER.defaultBlockState(), lucht = Blocks.AIR.defaultBlockState(),
                rand = Blocks.PRISMARINE_BRICKS.defaultBlockState(), hout = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        int r = Z4BazenPlekken.ZEESLURF_STRAAL - 4, bodem = Math.min(Z4BazenPlekken.ZEESLURF_GROTJE.getY() - 1, Z4BazenPlekken.ZEE_Y - 14);
        for (int x = -r - 2; x <= r + 2; x++) {
            for (int z = -r - 2; z <= r + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > r + 2) {
                    continue;
                }
                for (int y = bodem; y <= Z4BazenPlekken.ZEE_Y + 4; y++) {
                    BlockPos b = c.offset(x, 0, z).atY(y);
                    sl.getChunk(b);
                    boolean vlot = Math.abs(x) <= 2 && Math.abs(z) <= 2 && y == c.getY() - 1;   // the raft the fight starts on
                    BlockState s = vlot ? hout : d > r ? (y <= Z4BazenPlekken.ZEE_Y + 1 ? rand : lucht) : y == bodem ? zand
                            : y <= Z4BazenPlekken.ZEE_Y ? water : lucht;
                    if (!sl.getBlockState(b).equals(s)) {
                        Herstel.zet(sl, "z4_lagune", b, s);
                        n++;
                    }
                }
            }
        }
        BlockPos g = Z4BazenPlekken.ZEESLURF_GROTJE;   // a little grotto: a ring of coral stone round the spot
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            BlockPos b = g.offset((int) Math.round(Math.cos(a) * 4), 2, (int) Math.round(Math.sin(a) * 4));
            if (i % 4 != 0) {
                Herstel.zet(sl, "z4_lagune", b, Blocks.PINK_TERRACOTTA.defaultBlockState());
                n++;
            }
        }
        BlockPos w = Z4BazenPlekken.WRAK_CENTRUM;
        for (int x = -Z4BazenPlekken.WRAK_STRAAL; x <= Z4BazenPlekken.WRAK_STRAAL; x++) {
            for (int z = -Z4BazenPlekken.WRAK_STRAAL; z <= Z4BazenPlekken.WRAK_STRAAL; z++) {
                if (x * x + z * z <= Z4BazenPlekken.WRAK_STRAAL * Z4BazenPlekken.WRAK_STRAAL + 2) {
                    BlockPos b = w.offset(x, -1, z);
                    sl.getChunk(b);
                    if (sl.getBlockState(b).isAir() || !sl.getFluidState(b).isEmpty()) {
                        Herstel.zet(sl, "z4_wrak", b, hout);
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("z4bazen");
        b.then(dev("tp").then(Commands.argument("waar", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(PLEKKEN, s)).executes(c -> {
            naar(c.getSource().getPlayerOrException(), rijk(c), plek(StringArgumentType.getString(c, "waar")));
            return 1;
        })));
        b.then(dev("spawn").then(Commands.argument("baas", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(BAZEN_DEV, s))
                .executes(c -> spawn(c, StringArgumentType.getString(c, "baas")))));
        b.then(dev("reset").then(Commands.argument("baas", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(BAZEN_DEV, s))
                .executes(c -> {
                    String id = arena(StringArgumentType.getString(c, "baas"));
                    boolean ok = Arenas.reset(c.getSource().getServer(), id);
                    return zeg(c, ok ? id + " gereset: de baas staat er zo weer, met volle Vads." : "Onbekende arena " + id);
                })));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 3)).executes(c -> {
            Z4Baas z = dichtste(c.getSource().getPlayerOrException(), Z4Baas.class);
            if (z == null) {
                return zeg(c, "Geen zone-4-baas in de buurt.");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            double[] f = z instanceof Zeeslurf ? new double[] {0.66, 0.33} : new double[] {0.6, 0.25};
            if (z instanceof Zeeslurf s && s.inGrotje()) {
                zeg(c, "(de Zeeslurf zit in zijn grotje: eerst /guhs vadsrijk z4bazen poliepen)");
            }
            while (z.fase() < n && !z.geplofd() && z.fase() - 1 < f.length) {
                double doel = z.maxVads() * f[z.fase() - 1] - 1;
                if (z.vads() > doel) {
                    z.heal(doel - z.vads());
                }
                int voor = z.fase();
                z.krijgSchade(1, null);
                if (z.fase() == voor) {
                    break;   // (immune: a mechanic waits)
                }
            }
            return zeg(c, "Fase nu " + z.fase());
        })));
        b.then(dev("poliepen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = 0;
            for (Z3Doel d : p.level().getEntitiesOfClass(Z3Doel.class, p.getBoundingBox().inflate(64), d -> d.getType() == POLIEP.get())) {
                d.krijgSchade(d.vads() + 1, null);
                n++;
            }
            return zeg(c, n + " poliepen kapot.");
        }));
        b.then(dev("lieren").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Vliegervanger v = dichtste(p, Vliegervanger.class);
            if (v == null) {
                return zeg(c, "Geen Vliegervanger in de buurt.");
            }
            int n = 0;
            for (Z3Doel d : p.level().getEntitiesOfClass(Z3Doel.class, p.getBoundingBox().inflate(64), d -> d.getType() == LIER.get())) {
                for (int i = 0; i < 3; i++) {
                    v.doelGebruikt(d, p);
                }
                n++;
            }
            return zeg(c, n + " lieren binnengedraaid.");
        }));
        b.then(dev("loot").then(Commands.argument("baas", StringArgumentType.word()).suggests((c, s) -> SharedSuggestionProvider.suggest(Z4BazenLoot.BAZEN, s))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String baas = StringArgumentType.getString(c, "baas");
                    BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                            Z4BazenLoot.kist(baas, p, 55, p.getRandom()));
                    return zeg(c, "Een buitkist van " + baas + " voor jou.");
                })));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Z4BazenLoot.Benoemd x : Z4BazenLoot.BENOEMD) {
                p.getInventory().add(Z4BazenLoot.maak(x, 55, p.getRandom()));
            }
            for (String t : Z4BazenLoot.BAZEN) {
                p.getInventory().add(Z4BazenLoot.trofee(t));
            }
            return zeg(c, "Alle zone-4-baasitems en beide trofeeën.");
        }));
        b.then(dev("bouw").executes(c -> zeg(c, "Testlagune + noodek gebouwd: " + bouw(rijk(c)) + " blokken (terug met /guhs vadsrijk z4bazen herstel).")));
        b.then(dev("herstel").executes(c -> {
            ServerLevel sl = rijk(c);
            int n = Herstel.terug(sl, "z4_lagune") + Herstel.terug(sl, "z4_wrak");
            return zeg(c, "Hersteld: " + n + " blokken.");
        }));
        b.then(dev("herlaad").executes(c -> {
            Z4BazenPlekken.herlaad();
            return zeg(c, "Baasplekken opnieuw gelezen (de arena's zelf veranderen pas na een herstart). " + Z4WezensCommando.spawnersTekst());
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
