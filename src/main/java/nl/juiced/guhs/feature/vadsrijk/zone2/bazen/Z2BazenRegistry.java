package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boorkop;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boormeester;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf.Galmwolf;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf.IJspilaarBlok;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.KristalBlokken;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.SpiegelBlok;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelbeeld;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelburcht;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelzucht;

/**
 * Het Vadsrijk zone 2 "De Splinterpieken", slice "bazen" (Z2_BRIEF): De Galmwolf (world boss), Vrouwe Spiegelzucht + the
 * Spiegelburcht rooms (dungeon), De Mika-boormeester (secret), their loot, blocks and dev commands
 * ({@code /guhs vadsrijk z2bazen ...}). Ids {@code vr_z2_baas_*}, lang {@code vadsrijk.z2bazen.*}, generator
 * tools/features/vadsrijk_z2_bazen.py.
 */
public final class Z2BazenRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String ARENA_GALM = "z2bazen:galmwolf", ARENA_SPIEGEL = "z2bazen:spiegelzucht";

    public static final DeferredHolder<EntityType<?>, EntityType<Galmwolf>> GALMWOLF = entity("vr_z2_baas_galmwolf", Galmwolf::new, 2.6f, 2.8f, 2.2f, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<Spiegelzucht>> SPIEGELZUCHT = entity("vr_z2_baas_spiegelzucht", Spiegelzucht::new, 0.9f, 2.7f,
            2.3f, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<Spiegelbeeld>> SPIEGELBEELD = entity("vr_z2_baas_spiegelbeeld", Spiegelbeeld::new, 0.9f, 2.7f,
            2.3f, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<Boormeester>> BOORMEESTER = entity("vr_z2_baas_boormeester", Boormeester::new, 3.4f, 3.0f,
            2.4f, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<Boorkop>> BOORKOP = entity("vr_z2_baas_boorkop", Boorkop::new, 1.1f, 1.1f, 0.55f, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<Z2Hulpje>> WELP = entity("vr_z2_baas_welp", Z2Hulpje::new, 0.8f, 0.75f, 0.55f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<Z2Hulpje>> MIJNWERKER = entity("vr_z2_baas_mijnwerker", Z2Hulpje::new, 0.9f, 1.1f, 0.85f, 10);

    private static BlockBehaviour.Properties vast(Block kopie) {
        return BlockBehaviour.Properties.ofFullCopy(kopie).strength(-1, 3600000).noLootTable();
    }

    public static final DeferredBlock<IJspilaarBlok> IJSPILAAR = BLOCKS.registerBlock("vr_z2_baas_ijspilaar", IJspilaarBlok::new,
            () -> vast(Blocks.PACKED_ICE).friction(0.98f));
    public static final DeferredBlock<SpiegelBlok> SPIEGEL = BLOCKS.registerBlock("vr_z2_baas_spiegel", SpiegelBlok::new,
            () -> vast(Blocks.CALCITE).noOcclusion().sound(SoundType.AMETHYST));
    public static final DeferredBlock<KristalBlokken.Lichtbron> LICHTBRON = BLOCKS.registerBlock("vr_z2_baas_lichtbron", KristalBlokken.Lichtbron::new,
            () -> vast(Blocks.CALCITE).sound(SoundType.AMETHYST).lightLevel(s -> 12));
    public static final DeferredBlock<KristalBlokken.Slot> KRISTALSLOT = BLOCKS.registerBlock("vr_z2_baas_kristalslot", KristalBlokken.Slot::new,
            () -> vast(Blocks.AMETHYST_BLOCK).noOcclusion().lightLevel(s -> s.getValue(KristalBlokken.Slot.LIT) ? 14 : 2));
    public static final DeferredBlock<Block> KRISTALPOORT = BLOCKS.registerBlock("vr_z2_baas_kristalpoort", Block::new,
            () -> vast(Blocks.AMETHYST_BLOCK).lightLevel(s -> 5));

    private Z2BazenRegistry() {
    }

    private static <T extends VadsrijkMob> DeferredHolder<EntityType<?>, EntityType<T>> entity(String id, EntityType.EntityFactory<T> f, float w, float h,
            float oog, int bereik) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(oog).clientTrackingRange(bereik)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static void register(IEventBus modBus) {
        Z2BazenLoot.BENOEMD.size();   // (the item holders exist before ITEMS is registered)
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            var baas = VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64);
            e.put(GALMWOLF.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                    .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.STEP_HEIGHT, 1.5).build());
            e.put(SPIEGELZUCHT.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                    .add(Attributes.MOVEMENT_SPEED, 0.28).build());
            e.put(SPIEGELBEELD.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            e.put(BOORMEESTER.get(), baas.add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.STEP_HEIGHT, 1.0).build());
            e.put(BOORKOP.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            e.put(WELP.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 32).build());
            e.put(MIJNWERKER.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32).build());
        });
        Z2BazenLoot.typen();
        arenas();
        Z2BazenEffecten.register();
        Flauwvallen.CHECKPOINT_KIEZER.add(p -> {
            BlockPos b = Z2BazenPlekken.BOOR_CENTRUM;
            double dx = p.getX() - b.getX(), dz = p.getZ() - b.getZ();
            if (dx * dx + dz * dz <= Math.pow(Z2BazenPlekken.BOOR_STRAAL + 4, 2) && Math.abs(p.getY() - b.getY()) < 24) {
                return Z2BazenPlekken.BOOR_DEUR;
            }
            if (Spiegelburcht.binnen(p.getX(), p.getY(), p.getZ()) && Arenas.op(p.getX(), p.getY(), p.getZ()) == null) {
                return Z2BazenPlekken.BURCHT_VOOR;
            }
            return null;
        });
        NeoForge.EVENT_BUS.addListener(Z2BazenRegistry::tick);
        NeoForge.EVENT_BUS.addListener(Z2BazenRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    private static void arenas() {
        Arenas.registreer(new Arena(ARENA_GALM, Z2BazenPlekken.GALM_CENTRUM, Z2BazenPlekken.GALM_STRAAL, Z2BazenPlekken.GALM_DEUR,
                Z2BazenPlekken.GALM_RESPAWN, Z2BazenPlekken.GALM_BORD, "vadsrijk.z2bazen.galmwolf"), sl -> {
                    Galmwolf g = GALMWOLF.get().create(sl, EntitySpawnReason.SPAWNER);
                    if (g != null) {
                        g.stelIn(24, Element.WATER, false, Set.of());
                    }
                    return g;
                });
        Arenas.registreer(new Arena(ARENA_SPIEGEL, Z2BazenPlekken.SPIEGEL_CENTRUM, Z2BazenPlekken.SPIEGEL_STRAAL, Z2BazenPlekken.SPIEGEL_DEUR,
                Z2BazenPlekken.SPIEGEL_RESPAWN, Z2BazenPlekken.SPIEGEL_BORD, "vadsrijk.z2bazen.spiegelzucht"), sl -> {
                    Spiegelzucht s = SPIEGELZUCHT.get().create(sl, EntitySpawnReason.SPAWNER);
                    if (s != null) {
                        s.stelIn(33, Element.NATUUR, false, Set.of());
                    }
                    return s;
                });
    }

    private static void tick(ServerTickEvent.Post e) {
        Spiegelburcht.tick(e.getServer());
        Boormeester.beheer(e.getServer());
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

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("z2bazen");
        b.then(dev("tp").then(Commands.argument("waar", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("galmwolf", "burcht", "spiegelgang", "spiegelzucht", "boor"), s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel sl = rijk(c);
                    switch (StringArgumentType.getString(c, "waar")) {
                        case "galmwolf" -> naar(p, sl, Z2BazenPlekken.GALM_DEUR);
                        case "spiegelgang" -> naar(p, sl, Z2BazenPlekken.k(0, Z2BazenPlekken.GANG_Z1));
                        case "spiegelzucht" -> naar(p, sl, Z2BazenPlekken.SPIEGEL_DEUR);
                        case "boor" -> naar(p, sl, Z2BazenPlekken.BOOR_DEUR);
                        default -> naar(p, sl, Z2BazenPlekken.k(0, Z2BazenPlekken.VOORHAL_Z1 - 2));
                    }
                    return 1;
                })));
        b.then(dev("bouw").then(Commands.argument("wat", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("galmwolf", "burcht", "boor", "alles"), s)).executes(c -> {
                    ServerLevel sl = rijk(c);
                    String w = StringArgumentType.getString(c, "wat");
                    int n = switch (w) {
                        case "galmwolf" -> Z2Bouw.galmwolf(sl);
                        case "burcht" -> Z2Bouw.burcht(sl);
                        case "boor" -> Z2Bouw.boor(sl);
                        default -> Z2Bouw.galmwolf(sl) + Z2Bouw.burcht(sl) + Z2Bouw.boor(sl);
                    };
                    return zeg(c, "Noodbouw " + w + ": " + n + " blokken (alleen in lucht).");
                })));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 3)).executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (!(baas instanceof Z2Baas)) {
                return zeg(c, "Geen zone-2-baas in de buurt.");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            double[] drempels = baas instanceof Galmwolf ? new double[] {0.6, 0.3} : baas instanceof Spiegelzucht ? new double[] {0.7, 0.35}
                    : new double[] {0.66, 0.33};
            double doel = baas.maxVads() * drempels[n - 2] - 1;
            if (baas.vads() > doel) {
                baas.krijgSchade(baas.vads() - doel, null);
            }
            return zeg(c, "Fase nu " + baas.fase());
        })));
        b.then(dev("boor")
                .then(Commands.literal("ontgrendel").executes(c -> zeg(c, Boormeester.ontgrendel(c.getSource().getPlayerOrException())
                        ? "De Mika-boormeester ronkt wakker!" : "De boormachine is er nu niet (gevlucht, nog niet terug?).")))
                .then(Commands.literal("reset").executes(c -> {
                    Boormeester.reset(rijk(c));
                    return zeg(c, "De boormachine staat weer te slapen op de boorplek.");
                }))
                .then(Commands.literal("volgorde").executes(c -> {
                    Boormeester m = Boormeester.zoek(rijk(c), false);
                    if (m == null) {
                        return zeg(c, "Geen boormachine.");
                    }
                    int[] v = m.volgorde();
                    return zeg(c, "Volgorde: " + Boorkop.KLEUREN[v[0]] + ", " + Boorkop.KLEUREN[v[1]] + ", " + Boorkop.KLEUREN[v[2]] + " (stap " + m.stap() + ")");
                })));
        b.then(dev("burcht")
                .then(Commands.literal("reset").executes(c -> {
                    Spiegelburcht.reset(rijk(c));
                    return zeg(c, "Spiegelburcht gereset.");
                }))
                .then(Commands.literal("los").executes(c -> {
                    Spiegelburcht.los(rijk(c));
                    return zeg(c, "Alle spiegels op de oplossing gezet.");
                }))
                .then(Commands.literal("oplossing").executes(c -> zeg(c, Spiegelburcht.oplossing()))));
        b.then(dev("split").executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (baas instanceof Spiegelzucht s) {
                s.splits((ServerLevel) s.level());
                return 1;
            }
            return zeg(c, "Geen Vrouwe Spiegelzucht in de buurt.");
        }));
        b.then(dev("loot").then(Commands.argument("baas", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("galmwolf", "spiegelzucht", "boormeester"), s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String baas = StringArgumentType.getString(c, "baas");
                    BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                            Z2BazenLoot.kist(baas, p, 32, p.getRandom()));
                    return zeg(c, "Een buitkist van " + baas + " voor jou.");
                })));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Z2BazenLoot.Benoemd x : Z2BazenLoot.BENOEMD) {
                p.getInventory().add(Z2BazenLoot.maak(x, 32, p.getRandom()));
            }
            for (String t : List.of("galmwolf", "spiegelzucht", "boormeester")) {
                p.getInventory().add(Z2BazenLoot.trofee(t));
            }
            return zeg(c, "Alle benoemde zone-2-baasitems + trofeeën (lv 32).");
        }));
        b.then(dev("herstel").executes(c -> {
            ServerLevel sl = rijk(c);
            int n = 0;
            for (String k : Herstel.sleutels(sl)) {
                if (k.startsWith("z2_")) {
                    n += Herstel.terug(sl, k);
                }
            }
            return zeg(c, "Hersteld: " + n + " blokken.");
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
