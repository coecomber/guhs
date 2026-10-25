package nl.juiced.guhs.feature.grotspellen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.spelen.Spelhal;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Keeps the cave games fair and tidy: the halls and their huts can't be broken or built in (creative players may; the rock
 * around a hall and its extra entrance tunnels can: {@link Spelhal}), players
 * in a game can't get hurt, the borrowed tools never leave the game (no dropping; gone when you log out, die or change
 * dimension), a tap on a glimworm catches it, and a left-click with the shovel digs.
 */
public final class GrotSpellenEvents {
    /** Extra protected boxes, only for the GameTests (the test world has no generated halls). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();

    /** Is this spot part of a cave-game hall with its own walls, or its hut (server side)? Not the rock around it or the
     * extra entrance tunnels ({@link Spelhal}). */
    public static boolean inZaal(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return server.dimension() == ModDimensions.GUHMENSION && Spelhal.beschermd(server, pos, GrotSpellenFeature.STRUCTUREN);
    }

    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || !inZaal(player.level(), pos)) {
            return false;
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.grotspel.no_build").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && inZaal(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        if (GrotSpellenFeature.geleend(stack) && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseBlock(TriState.FALSE);                  // (with a borrowed tool, chests and such don't open)
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem && denied(event.getEntity(), event.getEntity().blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inZaal(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inZaal(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    // --- the game ------------------------------------------------------------------------------------------------------
    /** Left-click with the shovel on the treasure field: dig. */
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                && event.getItemStack().is(GrotSpellenFeature.SCHATSCHEPJE.get()) && event.getLevel().getBlockState(event.getPos()).is(GrotSpellenFeature.SCHATGROND.get())) {
            Schatgraven.graaf(p, event.getPos());
            event.setCanceled(true);
        }
    }

    /** A tap on a glimworm: caught (if it's in your game). Nobody can hurt them anyway. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof VangGlimwormEntity worm) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer p && GrotSpel.spelVan(p) instanceof Glimwormvangen spel && spel.fase() == GrotSpel.Fase.SPELEN
                    && spel.wormen().contains(worm.getUUID())) {
                spel.vang(p.level(), p, worm);
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && GrotSpel.speelt(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (GrotSpellenFeature.geleend(event.getEntity().getItem())) {
            event.setCanceled(true);
            if (GrotSpel.speelt(event.getPlayer())) {
                event.getPlayer().getInventory().add(event.getEntity().getItem().copy());
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GrotSpel.speelt(player)) {
            GrotSpel.verlaat(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> GrotSpellenFeature.geleend(item.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GrotSpel.verlaat(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!GrotSpel.speelt(event.getEntity())) {
            GrotSpel.neemTerug(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GrotSpel.speelt(player)) {
            GrotSpel.verlaat(player);
        }
    }

    /** A player whose host isn't there any more (its chunk unloaded): the game is over. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0 && GrotSpel.speelt(player) && !GrotSpel.hostInDeBuurt(player)) {
            GrotSpel.verlaat(player);
        }
    }

    private GrotSpellenEvents() {
    }
}
