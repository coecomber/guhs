package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * U-LEVEN-STAD (round U, FEEDBACK_RONDE4 V7 "levendigheid"): life, coziness and guh-ness in the towns of zones 1-6. Entities
 * {@code vr_sl_prop} ({@link LevenProp}: critters, ducklings, birds, the cart, the playground's moving parts), {@code vr_sl_guh}
 * ({@link LevenGuh}: walkers, musicians, picnickers, terraces, children) and {@code vr_sl_kat} ({@link LevenKat}); the manager
 * {@link StadsLeven}; the towns {@link Steden}; the playgrounds {@link Speeltuin}. Texts and models:
 * tools/features/vadsrijk_u_leven_stad.py; the playgrounds in the map: tools/vadsrijk/stadsleven/speeltuin.py.
 */
public final class StadslevenRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LevenProp>> PROP = ENTITIES.register("vr_sl_prop",
            () -> EntityType.Builder.<LevenProp>of(LevenProp::new, MobCategory.MISC).sized(0.4f, 0.4f).noSave().fireImmune()
                    .clientTrackingRange(5).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_sl_prop"))));
    public static final DeferredHolder<EntityType<?>, EntityType<LevenGuh>> GUH = ENTITIES.register("vr_sl_guh",
            () -> EntityType.Builder.<LevenGuh>of(LevenGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).noSave().fireImmune()
                    .clientTrackingRange(6).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_sl_guh"))));
    public static final DeferredHolder<EntityType<?>, EntityType<LevenKat>> KAT = ENTITIES.register("vr_sl_kat",
            () -> EntityType.Builder.<LevenKat>of(LevenKat::new, MobCategory.MISC).sized(0.6f, 0.7f).noSave().fireImmune()
                    .clientTrackingRange(5).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_sl_kat"))));

    private StadslevenRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(GUH.get(), GuhEntity.createAttributes().build());
            e.put(KAT.get(), Cat.createAttributes().build());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> StadsLeven.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && !StadsLeven.magErin(e.getEntity())) {
                e.setCanceled(true);   // a town-life entity this manager didn't spawn (never from disk: the types are noSave)
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent e) -> {
            if (!e.getLevel().isClientSide()) {
                StadsLeven.verlaten(e.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> StadsLeven.NIVEAU.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> StadsLeven.wis());
        NeoForge.EVENT_BUS.addListener(StadslevenRegistry::commandos);
        // u-dubbel's heuristic scan leaves town life alone (its own one-per-scene rule; the town guhs have no npc id)
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(StadsLeven::isLeven);
    }

    public static void payloads(PayloadRegistrar registrar) {
        StadslevenPayloads.register(registrar);
    }

    /** Dev teleport spots per town (from POI_*.md: a square, a campfire; zone 6 from POI_Z6.md). */
    static final Map<String, BlockPos> TP = Map.ofEntries(
            Map.entry("knuffelkamp", new BlockPos(-384, 87, 1025)), Map.entry("zwamzoet", new BlockPos(0, 64, 59)),
            Map.entry("haven", new BlockPos(90, 65, 470)), Map.entry("kandijkom", new BlockPos(704, 91, -1068)),
            Map.entry("honingtak", new BlockPos(1396, 167, -525)), Map.entry("lampionhaven", new BlockPos(1848, 67, -467)),
            Map.entry("hoogvads", new BlockPos(2696, 221, -520)), Map.entry("saffraanpoort", new BlockPos(3762, 71, -812)),
            Map.entry("rozenkloof", new BlockPos(3476, 74, -708)));

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("leven").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("status").executes(c -> {
            StringBuilder sb = new StringBuilder("Stadsleven: " + (StadsLeven.aan ? "AAN" : "UIT")
                    + (StadsLeven.devNiveau != null ? " (dev-niveau " + StadsLeven.devNiveau + ")" : ""));
            if (c.getSource().getEntity() instanceof ServerPlayer p) {
                Steden.Stad s = Steden.op(p.position());
                sb.append("\n jouw instelling: ").append(StadsLeven.niveau(p)).append(", stad hier: ").append(s == null ? "-" : s.id());
            }
            int ents = 0, guhs = 0;
            for (Map.Entry<Groep.Soort, int[]> e : StadsLeven.stats().entrySet()) {
                sb.append(String.format("\n %s: %d scène(s), %d entities (max %d per stad)", e.getKey().name().toLowerCase(Locale.ROOT), e.getValue()[0],
                        e.getValue()[1], e.getKey().max));
                ents += e.getValue()[1];
            }
            for (Groep g : StadsLeven.GROEPEN.values()) {
                guhs += g.guhs();
            }
            sb.append(String.format("\n totaal: %d scènes, %d entities (max %d), %d guhs (max %d); %d plekvakken gescand",
                    StadsLeven.GROEPEN.size(), ents, StadsLeven.MAX_ENTITEITEN, guhs, StadsLeven.MAX_GUHS, Plekken.aantalVakken()));
            c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return StadsLeven.GROEPEN.size();
        }));
        m.then(Commands.literal("aan").executes(c -> {
            StadsLeven.aan = true;
            c.getSource().sendSuccess(() -> Component.literal("Stadsleven: AAN"), false);
            return 1;
        }));
        m.then(Commands.literal("uit").executes(c -> {
            StadsLeven.aan = false;
            StadsLeven.allesWeg();
            c.getSource().sendSuccess(() -> Component.literal("Stadsleven: UIT (alles weg)"), false);
            return 1;
        }));
        m.then(Commands.literal("wis").executes(c -> {
            StadsLeven.allesWeg();
            c.getSource().sendSuccess(() -> Component.literal("Alle stadsleven-scènes weg (komen vanzelf terug)"), false);
            return 1;
        }));
        m.then(Commands.literal("niveau").then(Commands.argument("niveau", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"vol", "zacht", "uit", "eigen"}, b))
                .executes(c -> {
                    String n = StringArgumentType.getString(c, "niveau").toUpperCase(Locale.ROOT);
                    StadsLeven.devNiveau = n.equals("EIGEN") ? null : StadsLeven.Niveau.valueOf(n);
                    StadsLeven.allesWeg();
                    c.getSource().sendSuccess(() -> Component.literal("Stadsleven-niveau voor iedereen: "
                            + (StadsLeven.devNiveau == null ? "eigen instelling van elke speler" : StadsLeven.devNiveau)), false);
                    return 1;
                })));
        m.then(Commands.literal("spawn").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Groep.Soort.values()).map(s -> s.name().toLowerCase(Locale.ROOT)), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Steden.Stad stad = Steden.op(p.position());
                    if (!Vadsrijk.in(p) || stad == null) {
                        c.getSource().sendFailure(Component.literal("Alleen in een stad van Het Vadsrijk (zie /guhs vadsrijk leven tp <stad>)."));
                        return 0;
                    }
                    Groep.Soort s;
                    try {
                        s = Groep.Soort.valueOf(StringArgumentType.getString(c, "soort").toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        c.getSource().sendFailure(Component.literal("Onbekende soort"));
                        return 0;
                    }
                    ServerLevel sl = (ServerLevel) p.level();
                    Plekken.budget = 40;
                    Plekken.verken(sl, p.position(), 3);
                    Groep g = StadsLeven.maak(sl, s, stad, p.position(), sl.getRandom(), StadsLeven.Niveau.VOL);
                    if (g == null) {
                        c.getSource().sendFailure(Component.literal("Geen geschikte plek hier (of die is er al / het is nacht). Probeer een eindje verderop."));
                        return 0;
                    }
                    StadsLeven.GROEPEN.put(g.sleutel, g);
                    Vec3 w = g.midden();
                    c.getSource().sendSuccess(() -> Component.literal(String.format("%s verschenen bij %d %d %d", s.name().toLowerCase(Locale.ROOT),
                            (int) w.x, (int) w.y, (int) w.z)), false);
                    return 1;
                })));
        m.then(Commands.literal("tp").then(Commands.argument("stad", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(TP.keySet(), b))
                .executes(c -> tp(c.getSource(), StringArgumentType.getString(c, "stad"), false))));
        m.then(Commands.literal("speeltuin").then(Commands.argument("stad", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Speeltuin.alle().keySet(), b))
                .executes(c -> tp(c.getSource(), StringArgumentType.getString(c, "stad"), true))));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }

    private static int tp(CommandSourceStack src, String stad, boolean speeltuin) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(src.getServer());
        BlockPos h = Speeltuin.van(stad);
        BlockPos naar;
        float yaw = 0;
        if (speeltuin) {
            if (h == null) {
                src.sendFailure(Component.literal("Geen speeltuin voor " + stad + " (speeltuinen: " + Speeltuin.alle().keySet() + ")"));
                return 0;
            }
            naar = h.offset(6, 2, -2);   // just north of the playground, looking in through the opening
        } else {
            naar = TP.get(stad);
            if (naar == null) {
                src.sendFailure(Component.literal("Onbekende stad: " + stad));
                return 0;
            }
        }
        if (sl == null) {
            src.sendFailure(Component.literal("De Vadsrijk-dimensie is niet geladen"));
            return 0;
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(naar), Vec3.ZERO, yaw, 15f, TeleportTransition.DO_NOTHING));
        src.sendSuccess(() -> Component.literal(speeltuin ? "Bij de speeltuin van " + stad + " (kinderen spelen er overdag)" : "In " + stad), false);
        return 1;
    }
}
