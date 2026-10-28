package nl.juiced.guhs.feature.droomwolkje;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomRegels;

/**
 * 1.8.0 (uitbreiding U1, dromen-ontwerp/BOUWPLAN_180_UITBREIDING.md): after Snoes' story everyone gets an own droomwolkje in
 * Guhdromenland ({@link Wolkjes}: build there, friends may help, others visit), reached by the "Mijn droomwolkje"
 * brievenbusje on the Droomplein ({@link WolkjeBusBlock}, placed by code next to the plein's arrival spot) and left by its
 * steiger; and droomgasten on the plein ({@link Droomgasten}). Commands: {@link DroomwolkjeCommando}.
 */
public final class DroomwolkjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<WolkjeBusBlock> BUS = BLOCKS.registerBlock("droombrievenbus_wolkje", WolkjeBusBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.SNOW).strength(1f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> BUS_ITEM = ITEMS.registerItem("droombrievenbus_wolkje", p -> new BlockItem(BUS.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.droombrievenbus_wolkje.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());

    /** Persistent data: when a player arrived on a wolkje (its steiger waits a moment after that). */
    static final String AANGEKOMEN = "guhs_droomwolkje_aangekomen";

    private DroomwolkjeFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        DroomRegels.MAG_HIER = Wolkjes::magBouwen;
        DroomRegels.PLEK = Wolkjes::plek;
        DroomRegels.TERUG = Wolkjes::terug;
        NeoForge.EVENT_BUS.addListener(DroomwolkjeFeature::tick);
        NeoForge.EVENT_BUS.addListener(DroomwolkjeFeature::spelerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, DroomwolkjeFeature::klikBlok);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.EntityInteract e) -> {
            if (Droomgasten.klik(e.getTarget(), e.getEntity(), e.getHand())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.EntityInteractSpecific e) -> {
            if (Droomgasten.isGast(e.getTarget())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> Droomgasten.opJoin(e));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Droomgasten.allesWeg(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                for (Component c : Droomgasten.bezorg(p.level().getServer(), p.getUUID())) {
                    p.sendSystemMessage(c);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(DroomwolkjeCommando::register);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BUS_ITEM.get()));
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 40 != 11) {
            return;
        }
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        if (droom == null || droom.players().isEmpty()) {
            return;
        }
        if (t % 200 == 11) {
            zetBus(droom);
        }
        terugOpWolkjes(droom);
        if (t % Droomgasten.ELKE == 11) {
            Droomgasten.tick(server);
        }
    }

    /** The wolkje's steiger: back to the plein (a moment after arriving). */
    private static void spelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0 || !DroomReis.inDroom(p) || !p.onGround()) {
            return;
        }
        long sinds = p.level().getGameTime() - p.getPersistentData().getLongOr(AANGEKOMEN, 0L);
        if (sinds >= 60 && Wolkjes.opSteiger(p)) {
            DroomReis.zweef(p, DroomEilanden.PLEIN);
        }
    }

    /** Visitors look, but don't open chests and the like on someone else's wolkje. */
    private static void klikBlok(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || !DromenFeature.isDroom(level) || p.getAbilities().instabuild) {
            return;
        }
        Wolkjes.Stand s = Wolkjes.stand(level);
        BlockPos pos = event.getPos();
        if (s.eigenaarVan(pos) != null && !s.mag(p.getUUID(), pos) && level.getBlockEntity(pos) != null) {
            event.setCanceled(true);
            if (p instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.droomwolkje.niet_van_jou").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The "Mijn droomwolkje" brievenbusje next to the plein's arrival spot (again when an island update took it away). */
    static void zetBus(ServerLevel droom) {
        if (DroomEilanden.gebouwd(droom, DroomEilanden.PLEIN) < 0) {
            return;
        }
        Wolkjes.Stand s = Wolkjes.stand(droom);
        if (s.bus != null && droom.isLoaded(s.bus) && droom.getBlockState(s.bus).is(BUS.get())) {
            return;
        }
        Vec3 a = DroomEilanden.wereld(DroomEilanden.PLEIN, DroomEilanden.opzet(DroomEilanden.PLEIN).aankomst().plek());
        BlockPos aankomst = BlockPos.containing(a);
        if (!droom.isLoaded(aankomst)) {
            return;
        }
        int[][] plekken = {{2, 0}, {-2, 0}, {2, 1}, {-2, 1}, {2, -1}, {-2, -1}, {3, 0}, {-3, 0}, {0, 3}, {3, 2}, {-3, 2}, {1, 3}, {-1, 3}};
        for (int[] o : plekken) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos q = aankomst.offset(o[0], dy, o[1]);
                if (droom.isEmptyBlock(q) && droom.isEmptyBlock(q.above())
                        && droom.getBlockState(q.below()).isFaceSturdy(droom, q.below(), Direction.UP)) {
                    Direction kijkt = Direction.getApproximateNearest(aankomst.getX() - q.getX(), 0, aankomst.getZ() - q.getZ());
                    droom.setBlock(q, Wolkjes.bus(kijkt.getAxis().isHorizontal() ? kijkt : Direction.NORTH), 3);
                    s.bus = q;
                    s.setDirty();
                    return;
                }
            }
        }
    }

    /** A creature (no player) that tumbled off a built wolkje floats back onto it. */
    static void terugOpWolkjes(ServerLevel droom) {
        Wolkjes.Stand s = Wolkjes.stand(droom);
        for (Wolkjes.Wolkje w : s.wolkjes.values()) {
            if (!w.gebouwd) {
                continue;
            }
            BlockPos m = Wolkjes.midden(w.nummer);
            if (!droom.isLoaded(m)) {
                continue;
            }
            AABB onder = new AABB(m.getX() - Wolkjes.HALF, droom.getMinY(), m.getZ() - Wolkjes.HALF, m.getX() + Wolkjes.HALF + 1,
                    m.getY() - 30, m.getZ() + Wolkjes.HALF + 1);
            Vec3 terug = Wolkjes.wereld(w.nummer, Wolkjes.AANKOMST);
            for (Entity e : droom.getEntities((Entity) null, onder, e -> e instanceof LivingEntity && !(e instanceof Player))) {
                e.teleportTo(terug.x, terug.y, terug.z);
                e.setDeltaMovement(Vec3.ZERO);
                e.fallDistance = 0;
            }
        }
    }

    /** (commands) the owner of the wolkje this player stands on, or null. */
    static UUID eigenaarHier(ServerPlayer p) {
        return p.level() instanceof ServerLevel level ? Wolkjes.stand(level).eigenaarVan(p.blockPosition().below()) : null;
    }
}
