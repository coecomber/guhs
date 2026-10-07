package nl.juiced.guhs.feature.snuffel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;

/**
 * The game-bus events of Het Snuffeleiland: the life cycle of the dog form ({@link Hondvorm}: every tick, login, logout,
 * respawn, a change of dimension, who sees whom, the dog's size, a dog's death) and the island's rules.
 * <p>
 * <b>The rules for a dog</b> (a creative builder passes the block rules): no breaking and no placing of blocks, no using
 * an item on the world, only blocks of the tag {@code guhs:snuffel_bruikbaar} (doors, gates, bells...) can be used; a dog
 * attacks nothing and nothing hurts it (only /kill gets through); it tosses nothing (the memory card comes straight back),
 * picks nothing up, puts nothing in an item frame or on an armour stand, rides nothing unless a slice says so
 * ({@link #RIJDBAAR}), and a bed never sets the spawn point on the island. On an island nothing explodes or griefs.
 */
public final class SnuffelEvents {
    /** Blocks a dog can use with a right-click. */
    public static final TagKey<Block> BRUIKBAAR = TagKey.create(Registries.BLOCK, Guhs.id("snuffel_bruikbaar"));
    /** What a dog may ride after all (the island slice's boat, a cart...): any of these says yes. */
    public static final List<Predicate<Entity>> RIJDBAAR = new CopyOnWriteArrayList<>();

    private SnuffelEvents() {
    }

    /** A dog, on either side (the client knows it from the look it was sent). */
    private static boolean hond(Player p) {
        return p.level().isClientSide() ? Hondvorm.clientRas.apply(p) != null : Hondvorm.actief(p);
    }

    private static boolean vrij(Player p) {
        return p.getAbilities().instabuild;
    }

    private static void nee(Player p) {
        if (p instanceof ServerPlayer sp && (sp.tickCount & 7) == 0) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.regels.poten").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // =====================================================================================================================
    // the dog form's life cycle
    // =====================================================================================================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Hondvorm.tick(p);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Eiland.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Hondvorm.opLogin(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Hondvorm.opLogout(p);
            GuhstationBlock.vergeet(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Hondvorm.opRespawn(p);
        }
    }

    /** After everybody else's (a Guhpixel game gives its inventory back in this event too): then the one rule is checked. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Hondvorm.controleer(p);
            Hondvorm.sync(p);
            Stand.stuur(p);
        }
    }

    /** Somebody is about to enter the island's dimension another way than {@link Reis}: where they stand now is their home. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer p && event.getDimension() == Eiland.DIM
                && p.level().dimension() != Eiland.DIM && !Hondvorm.actief(p)) {
            Reis.onthoudThuis(p);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer wie && event.getEntity() instanceof ServerPlayer kijker) {
            Hondvorm.toon(wie, kijker);
        }
    }

    /** A dog is as big as a dog: its box and its eyes (both sides; every pose). */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player p && p.isAddedToLevel()) {
            EntityDimensions maat = Hondvorm.maat(p);
            if (maat != null) {
                event.setNewSize(maat);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer nieuw && event.getOriginal() instanceof ServerPlayer oud
                && Hondvorm.stierfAlsHond(oud)) {
            // (dying on the island costs nothing: the experience stays too)
            nieuw.experienceLevel = oud.experienceLevel;
            nieuw.totalExperience = oud.totalExperience;
            nieuw.experienceProgress = oud.experienceProgress;
        }
    }

    // =====================================================================================================================
    // a dog's death costs nothing
    // =====================================================================================================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Hondvorm.actief(p)) {
            // whatever lies in a dog's pockets besides the memory card (a creative builder's blocks) goes home with the post
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                SnuffelKluis.post(p, inv.getItem(i).copy());
            }
            inv.clearContent();
            Hondvorm.opDood(p);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Hondvorm.stierfAlsHond(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Hondvorm.stierfAlsHond(p)) {
            event.setCanceled(true);
        }
    }

    // =====================================================================================================================
    // the rules
    // =====================================================================================================================

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if (hond(p) && !vrij(p)) {
            event.setCanceled(true);
            nee(p);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (Hondvorm.actief(sp) && !vrij(sp)) {
                event.setCanceled(true);
                nee(sp);
            }
        } else if (Eiland.in(level, event.getPos())) {
            event.setCanceled(true);   // (nothing but a builder changes the island)
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (!hond(p) || vrij(p)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!state.is(BRUIKBAAR)) {
            event.setUseBlock(TriState.FALSE);
        }
        // (no buckets, boats, bone meal, spawn eggs...; the memory card opens its menu through its own "use")
        event.setUseItem(TriState.FALSE);
    }

    /** A paw puts nothing in an item frame or on an armour stand (the memory card would leave its slot that way). */
    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (ding(event.getTarget()) && hond(event.getEntity()) && !vrij(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (ding(event.getTarget()) && hond(event.getEntity()) && !vrij(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean ding(Entity e) {
        return e instanceof net.minecraft.world.entity.decoration.HangingEntity || e instanceof net.minecraft.world.entity.decoration.ArmorStand;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (hond(event.getEntity()) && !vrij(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Hondvorm.actief(p) && !event.getSource().is(DamageTypes.GENERIC_KILL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        Player p = event.getPlayer();
        if (!(p instanceof ServerPlayer sp) || !Hondvorm.actief(sp)) {
            return;
        }
        ItemStack stack = event.getEntity().getItem().copy();
        if (stack.is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get())) {
            event.setCanceled(true);            // (back in its slot at once)
            Hondvorm.geefKaart(sp);
            sp.inventoryMenu.broadcastChanges();
            return;
        }
        if (vrij(p)) {
            return;
        }
        if (p.getInventory().add(stack) && stack.isEmpty()) {
            event.setCanceled(true);
            p.inventoryMenu.broadcastChanges();
        } else {
            // the pockets are full (it came off the cursor): it lies at the dog's feet and only they can take it
            event.getEntity().setItem(stack);
            event.getEntity().setTarget(p.getUUID());
            event.getEntity().setDeltaMovement(Vec3.ZERO);
            event.getEntity().setPos(p.getX(), p.getY() + 0.2, p.getZ());
            event.getEntity().setNoPickUpDelay();
            event.getEntity().setUnlimitedLifetime();
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player p = event.getPlayer();
        if (hond(p) && !vrij(p)) {
            Entity eigenaar = event.getItemEntity().getOwner();
            if (eigenaar != p) {
                event.setCanPickup(TriState.FALSE);   // (a dog's pockets hold nothing: what lies on the island stays there)
            }
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player p && hond(p) && !vrij(p)) {
            Entity waarop = event.getEntityBeingMounted();
            for (Predicate<Entity> mag : RIJDBAAR) {
                if (mag.test(waarop)) {
                    return;
                }
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (!event.isForced() && event.getSpawnLevel() == Eiland.DIM) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> Eiland.in(level, pos));
        event.getAffectedEntities().removeIf(Eiland::in);
    }

    @SubscribeEvent
    public static void onGriefing(EntityMobGriefingEvent event) {
        if (Eiland.in(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Hondvorm.opStop();
        Snuffelen.opStop();
        Maatjes.opStop();
        Eiland.opStop();
    }
}
