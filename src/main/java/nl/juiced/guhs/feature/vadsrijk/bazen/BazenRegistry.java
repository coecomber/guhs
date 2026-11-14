package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.ArrayList;
import java.util.Iterator;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.ElementKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.KoningKlodder;
import nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion;
import nl.juiced.guhs.feature.vadsrijk.bazen.mika.SpionBallon;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.LichtzwamBlok;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Opperzwam;
import nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Zwamtempel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Het Vadsrijk, slice "bazen" (CONTRACT_VADSRIJK 1): Koning Klodder, de Opperzwam (+ the Zwamtempel), de Mika-spion on
 * kern.baas.VadsrijkBaas, their arenas, loot, blocks and dev commands ({@code /guhs vadsrijk bazen ...}).
 */
public final class BazenRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KoningKlodder>> KONING_KLODDER = entity("vr_baas_koning_klodder",
            KoningKlodder::new, 3.3f, 3.0f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<ElementKlodder>> KK_KLODDER = entity("vr_baas_kk_klodder", ElementKlodder::new, 1.0f, 0.85f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<Opperzwam>> OPPERZWAM = entity("vr_baas_opperzwam", Opperzwam::new, 1.6f, 3.4f, 2.6f);
    public static final DeferredHolder<EntityType<?>, EntityType<MikaSpion>> MIKA_SPION = entity("vr_baas_mika_spion", MikaSpion::new, 1.2f, 1.2f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<nl.juiced.guhs.feature.vadsrijk.bazen.mika.LokMika>> LOK_MIKA = entity("vr_baas_lok_mika",
            nl.juiced.guhs.feature.vadsrijk.bazen.mika.LokMika::new, 1.2f, 1.2f, 0.9f);
    public static final DeferredHolder<EntityType<?>, EntityType<SpionBallon>> SPION_BALLON = entity("vr_baas_spion_ballon", SpionBallon::new, 1.6f, 2.6f, 1.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<HulpSlijmpje>> HULPSLIJMPJE = ENTITIES.register("vr_baas_hulpslijmpje",
            () -> EntityType.Builder.<HulpSlijmpje>of(HulpSlijmpje::new, MobCategory.MISC).sized(0.52f, 0.52f).eyeHeight(0.3f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_baas_hulpslijmpje"))));

    public static final DeferredBlock<LichtzwamBlok> LICHTZWAM = BLOCKS.registerBlock("vr_baas_lichtzwam", LichtzwamBlok::new,
            () -> BlockBehaviour.Properties.of().strength(-1, 3600000).noLootTable().noOcclusion().sound(SoundType.FUNGUS)
                    .lightLevel(s -> s.getValue(LichtzwamBlok.LIT) ? 13 : 1));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> ZWAMPOORT = BLOCKS.registerBlock("vr_baas_zwampoort",
            net.minecraft.world.level.block.Block::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).strength(-1, 3600000).noLootTable());

    private record Taak(MinecraftServer server, long op, Runnable wat) {
    }

    private static final List<Taak> LATER = new ArrayList<>();

    private BazenRegistry() {
    }

    private static <T extends VadsrijkMob> DeferredHolder<EntityType<?>, EntityType<T>> entity(String id, EntityType.EntityFactory<T> f, float w, float h,
            float oog) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(oog).clientTrackingRange(16)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    /** Runs {@code wat} on the server thread after {@code ticks}. */
    public static void later(MinecraftServer server, int ticks, Runnable wat) {
        synchronized (LATER) {
            LATER.add(new Taak(server, server.overworld().getGameTime() + ticks, wat));
        }
    }

    public static void register(IEventBus modBus) {
        BazenLoot.BENOEMD.size();   // (makes sure the item holders exist before ITEMS is registered)
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        BazenMuziek.SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            var baas = VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48);
            e.put(KONING_KLODDER.get(), baas.build());
            e.put(OPPERZWAM.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48)
                    .add(Attributes.MOVEMENT_SPEED, 0.24).build());
            e.put(MIKA_SPION.get(), VadsrijkMob.basisAttributen().add(Attributes.FOLLOW_RANGE, 32).add(Attributes.MOVEMENT_SPEED, 0.3).build());
            e.put(LOK_MIKA.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.3).build());
            e.put(KK_KLODDER.get(), VadsrijkMob.basisAttributen().add(Attributes.MOVEMENT_SPEED, 0.3).build());
            e.put(SPION_BALLON.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            e.put(HULPSLIJMPJE.get(), HulpSlijmpje.attributen().build());
        });
        BazenLoot.typen();
        arenas();
        BazenEffecten.register();
        LichtzwamBlok.AANGESTOKEN.add((sl, pos, p) -> {
            for (Opperzwam o : sl.getEntitiesOfClass(Opperzwam.class, new net.minecraft.world.phys.AABB(pos).inflate(40), o -> o.donker())) {
                o.aangestoken(sl, pos);
            }
        });
        LichtzwamBlok.AANGESTOKEN.add(ZwamtempelToegang::k1);
        nl.juiced.guhs.world.TikWacht.post("BazenRegistry.tick", BazenRegistry::tick);
        NeoForge.EVENT_BUS.addListener(BazenRegistry::commando);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> Herstel.allesTerug(e.getServer()));
        ElementKlodder.registreerSchild();
        // fix round 1: the temple gate goes before wereld's door click (a cancelled click never reaches it)
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,
                (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
                    if (e.getEntity() instanceof ServerPlayer p && Zwamtempel.klikDeur(p, e.getPos())) {
                        e.setCanceled(true);
                        e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                        return;
                    }
                    if (e.getEntity() instanceof ServerPlayer p && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && gooiKroon(p)) {
                        e.setCanceled(true);
                        e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    }
                });
        // the crown of Koning Klodder: its carrier throws it back with a right-click (item, air, on him)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && gooiKroon(p)) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && gooiKroon(p)) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
    }

    /** If this player carries Koning Klodder's crown: throw it at him. */
    static boolean gooiKroon(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return false;
        }
        for (KoningKlodder k : sl.getEntitiesOfClass(KoningKlodder.class, p.getBoundingBox().inflate(48), k -> k.kroonDrager() == p)) {
            return k.gooiKroon(p);
        }
        return false;
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    private static void arenas() {
        Arenas.registreer(new Arena("bazen:koning_klodder", BazenPlekken.KK_CENTRUM, BazenPlekken.KK_STRAAL, BazenPlekken.KK_DEUR, BazenPlekken.KK_RESPAWN,
                BazenPlekken.KK_BORD, "vadsrijk.bazen.koning_klodder"), sl -> {
                    KoningKlodder k = KONING_KLODDER.get().create(sl, EntitySpawnReason.SPAWNER);
                    if (k != null) {
                        k.stelIn(8, Element.WATER, false, Set.of());
                    }
                    return k;
                });
        Arenas.registreer(new Arena("bazen:opperzwam", BazenPlekken.OZ_CENTRUM, BazenPlekken.OZ_STRAAL, BazenPlekken.OZ_DEUR, BazenPlekken.OZ_RESPAWN,
                BazenPlekken.OZ_BORD, "vadsrijk.bazen.opperzwam"), sl -> {
                    Opperzwam o = OPPERZWAM.get().create(sl, EntitySpawnReason.SPAWNER);
                    if (o != null) {
                        o.stelIn(19, Element.NATUUR, false, Set.of());
                    }
                    return o;
                });
    }

    private static void tick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        long nu = s.overworld().getGameTime();
        List<Taak> klaar = new ArrayList<>();
        synchronized (LATER) {
            for (Iterator<Taak> it = LATER.iterator(); it.hasNext(); ) {
                Taak t = it.next();
                if (t.server() != s) {
                    it.remove();
                } else if (nu >= t.op()) {
                    klaar.add(t);
                    it.remove();
                }
            }
        }
        klaar.forEach(t -> t.wat().run());
        BazenEffecten.tick(s);
        Zwamtempel.tick(s);
        MikaSpion.beheer(s);
    }

    // ---- dev commands -------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static ServerLevel rijk(CommandContext<CommandSourceStack> c) {
        ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
        return sl;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static VadsrijkBaas dichtste(ServerPlayer p) {
        return p.level().getEntitiesOfClass(VadsrijkBaas.class, p.getBoundingBox().inflate(64), b -> !b.geplofd()).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p))).orElse(null);
    }

    private static void naar(ServerPlayer p, ServerLevel sl, BlockPos b) {
        sl.getChunk(b);
        p.teleportTo(sl, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("bazen");
        b.then(dev("tp").then(Commands.argument("waar", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("koning", "tempel", "opperzwam", "markt", "mika"), s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel sl = rijk(c);
                    String w = StringArgumentType.getString(c, "waar");
                    switch (w) {
                        case "koning" -> naar(p, sl, BazenPlekken.KK_DEUR);
                        case "tempel" -> naar(p, sl, BazenPlekken.TEMPELDEUR_VOOR.south(4));
                        case "opperzwam" -> naar(p, sl, BazenPlekken.OZ_DEUR);
                        case "mika" -> naar(p, sl, BlockPos.containing(MikaSpion.hoedTop(sl, 0)).east(4));
                        default -> naar(p, sl, BazenPlekken.MIKA_KRAAM.south(4));
                    }
                    return 1;
                })));
        b.then(dev("bouw").then(Commands.argument("wat", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("koning", "tempel", "mika"), s)).executes(c -> {
                    ServerLevel sl = rijk(c);
                    String w = StringArgumentType.getString(c, "wat");
                    int n = switch (w) {
                        case "tempel" -> BazenBouw.tempel(sl);
                        case "mika" -> BazenBouw.mika(sl);
                        default -> BazenBouw.koning(sl);
                    };
                    return zeg(c, "Noodbouw " + w + ": " + n + " blokken (alleen in lucht).");
                })));
        b.then(dev("mika")
                .then(Commands.literal("ontmasker").executes(c -> zeg(c, MikaSpion.ontmasker(c.getSource().getPlayerOrException())
                        ? "De Mika-spion is ontmaskerd!" : "Geen vermomde Mika-spion gevonden (staat hij al te vechten, of is hij nog weg?).")))
                .then(Commands.literal("reset").executes(c -> {
                    ServerLevel sl = rijk(c);
                    MikaSpion m = MikaSpion.zoek(sl, false);
                    if (m != null) {
                        m.discard();
                    }
                    MikaSpion.zoek(sl, true);
                    return zeg(c, "Akim de Kaasguh staat weer bij zijn kraam.");
                })));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 4)).executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (baas == null) {
                return zeg(c, "Geen baas in de buurt.");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            double[] drempels = baas instanceof KoningKlodder ? new double[] {0.6, 0.35} : baas instanceof Opperzwam ? new double[] {0.66, 0.33}
                    : new double[] {0.7, 0.4, 0.15};
            if (n - 2 >= drempels.length) {
                return zeg(c, "Die fase heeft deze baas niet.");
            }
            double doel = baas.maxVads() * drempels[n - 2] - 1;
            if (baas.vads() > doel) {
                baas.krijgSchade(baas.vads() - doel, c.getSource().getPlayerOrException());
            }
            return zeg(c, "Fase nu " + baas.fase());
        })));
        b.then(dev("loot").then(Commands.argument("baas", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("koning_klodder", "opperzwam", "mika_spion"), s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String baas = StringArgumentType.getString(c, "baas");
                    BaasKist.plaats((ServerLevel) p.level(), p, BlockPos.containing(p.position().add(p.getLookAngle().multiply(2, 0, 2))),
                            BazenLoot.kist(baas, p, 20, p.getRandom()));
                    return zeg(c, "Een buitkist van " + baas + " voor jou.");
                })));
        b.then(dev("items").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (BazenLoot.Benoemd x : BazenLoot.BENOEMD) {
                p.getInventory().add(BazenLoot.maak(x, 20, p.getRandom()));
            }
            for (String t : List.of("koning_klodder", "opperzwam", "mika_spion")) {
                p.getInventory().add(BazenLoot.trofee(t));
            }
            return zeg(c, "Alle benoemde baasitems + trofeeën (lv 20).");
        }));
        b.then(dev("herstel").executes(c -> zeg(c, "Hersteld: " + Herstel.allesTerug(rijk(c)) + " blokken.")));
        b.then(dev("tempel")
                .then(Commands.literal("reset").executes(c -> {
                    Zwamtempel.resetAlles(rijk(c));
                    return zeg(c, "Zwamtempel gereset.");
                }))
                .then(Commands.literal("open").executes(c -> {
                    Zwamtempel.ceremonie(rijk(c), rijk(c).getGameTime());
                    return 1;
                }))
                .then(Commands.literal("oplossing").executes(c -> zeg(c, Zwamtempel.oplossing()))));
        b.then(dev("split").executes(c -> {
            VadsrijkBaas baas = dichtste(c.getSource().getPlayerOrException());
            if (baas instanceof KoningKlodder k) {
                k.splits((ServerLevel) k.level());
                return 1;
            }
            return zeg(c, "Geen Koning Klodder in de buurt.");
        }));
        b.then(dev("spawn").then(Commands.argument("baas", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(List.of("koning_klodder", "opperzwam", "mika_spion"), s)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel sl = (ServerLevel) p.level();
                    Vec3 v = p.position().add(p.getLookAngle().multiply(6, 0, 6));
                    VadsrijkBaas baas = switch (StringArgumentType.getString(c, "baas")) {
                        case "opperzwam" -> OPPERZWAM.get().create(sl, EntitySpawnReason.COMMAND);
                        case "mika_spion" -> MIKA_SPION.get().create(sl, EntitySpawnReason.COMMAND);
                        default -> KONING_KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
                    };
                    if (baas == null) {
                        return 0;
                    }
                    baas.stelIn(baas instanceof Opperzwam ? 19 : baas instanceof MikaSpion ? 15 : 8, null, false, Set.of());
                    baas.snapTo(v.x, v.y, v.z, p.getYRot() + 180, 0);
                    sl.addFreshEntity(baas);
                    if (baas instanceof MikaSpion m) {
                        m.vechtNu(sl);
                    }
                    return zeg(c, "Losse baas (zonder arena) gespawnd.");
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    /** (tiny bridge so the listener list doesn't need Zwamtempel's package-private method) */
    static final class ZwamtempelToegang {
        static void k1(ServerLevel sl, BlockPos pos, ServerPlayer p) {
            Zwamtempel.lichtzwamAangestoken(sl, pos, p);
        }
    }
}
