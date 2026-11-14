package nl.juiced.guhs.feature.vadsrijk.momenten;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.wereld.Littekens;
import nl.juiced.guhs.registry.ModCreativeTabs;

/**
 * Het Vadsrijk, fix round 1 "momenten" (owner: agent MOMENTEN; notes in NOTITIES_FIX_MOMENTEN.md): set pieces that make zone 1 feel
 * alive. Ids {@code vr_mom_*}, lang {@code vadsrijk.momenten.*}, payloads {@code guhs:vr_momenten_*}, generator
 * tools/features/vadsrijk_momenten.py, gametests {@code VadsrijkMomentenGameTests}.
 * <ul>
 *   <li>{@link Golf}: the pink recolour wave at {@code zone1_klaar} (scene + client tint driven by the wave);</li>
 *   <li>{@link Karavaan}: the travelling snail caravan with its rotating shop and the klodder attacks.</li>
 * </ul>
 * Dev: {@code /guhs vadsrijk momenten golf [plan|grijs]}, {@code /guhs vadsrijk momenten karavaan info|hier|tp|aanval|winkel}.
 */
public final class MomentenRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KaravaanSlak>> KARAVAAN = ENTITIES.register("vr_mom_karavaan",
            () -> EntityType.Builder.<KaravaanSlak>of(KaravaanSlak::new, MobCategory.MISC).sized(2.0f, 1.9f).noSave().fireImmune()
                    .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_mom_karavaan"))));
    public static final DeferredHolder<EntityType<?>, EntityType<HandelGuh>> HANDELAAR = ENTITIES.register("vr_mom_handelaar",
            () -> EntityType.Builder.<HandelGuh>of(HandelGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10)
                    .noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_mom_handelaar"))));

    public static final DeferredItem<RegenboogDrankje> REGENBOOGDRANKJE = ITEMS.registerItem("vr_mom_regenboogdrankje", RegenboogDrankje::new,
            () -> nl.juiced.guhs.feature.vadsrijk.economie.Drinken.eigenschappen(true).stacksTo(8).rarity(Rarity.EPIC));

    private MomentenRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(HANDELAAR.get(), GuhEntity.createAttributes().build()));
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == ModCreativeTabs.GUHS_TAB.getKey()) {
                e.accept(new ItemStack(REGENBOOGDRANKJE.get()));
            }
        });
        Golf.init();
        Karavaan.init();
        nl.juiced.guhs.world.TikWacht.post("MomentenRegistry:78", e -> {
            Golf.tick(e.getServer());
            Karavaan.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            // caravan attackers from an earlier attack (saved with their chunk) never come back
            if (!e.getLevel().isClientSide() && e.getEntity().entityTags().contains(Karavaan.TAG) && !Karavaan.hoortBij(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Golf.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> Karavaan.wis());
        NeoForge.EVENT_BUS.addListener(MomentenRegistry::commandos);
    }

    public static void payloads(PayloadRegistrar registrar) {
        MomentenPayloads.registreer(registrar);
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("momenten");
        m.then(dev("golf").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (!Vadsrijk.in(p)) {
                return 0;
            }
            Golf.speel(p);
            return 1;
        }).then(Commands.literal("plan").executes(c -> {
            Golf.plan(c.getSource().getPlayerOrException());   // as after the cliffhanger: grey now, the wave in ~14 s
            return 1;
        })).then(Commands.literal("grijs").executes(c -> {
            Littekens.zetKlaar(c.getSource().getPlayerOrException(), false);
            return 1;
        })));
        m.then(dev("karavaan").then(Commands.literal("info").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal(Karavaan.info()), false);
            return 1;
        })).then(Commands.literal("hier").executes(c -> {
            Karavaan.hier(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal(Karavaan.info()), false);
            return 1;
        })).then(Commands.literal("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (sl == null) {
                return 0;
            }
            KaravaanStaat st = Karavaan.staat(sl.getRandom());
            Vec3 pos = st.route().op(st.s());
            Vec3 zij = st.route().richting(st.s(), st.richting());
            Vec3 naar = pos.add(-zij.z * 8, 2, zij.x * 8);
            float yaw = (float) Math.toDegrees(Math.atan2(-(pos.x - naar.x), pos.z - naar.z));
            p.teleport(new TeleportTransition(sl, naar, Vec3.ZERO, yaw, 15f, TeleportTransition.DO_NOTHING));
            return 1;
        })).then(Commands.literal("aanval").executes(c -> {
            boolean ok = Karavaan.aanvalNu(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal(ok ? "aanval komt (als je binnen 40 blokken bent)" : "kan nu niet"), false);
            return ok ? 1 : 0;
        })).then(Commands.literal("winkel").executes(c -> {
            Winkels.open(c.getSource().getPlayerOrException(), Karavaan.WINKEL);
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }
}
