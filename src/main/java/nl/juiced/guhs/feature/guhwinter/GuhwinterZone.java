package nl.juiced.guhs.feature.guhwinter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guhwinter.kern.KernRegistry;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.10.10 Guhwinter: the plaza ({@link #PLEIN}, x -350..-276, z -735..-661, y 20..90 in the Guhmension) is unbreakable,
 * during the event and after it (then it is the empty feestplein frame):
 * <ul>
 *   <li>no breaking, no placing, no items used on blocks (food, fireworks and the tag {@code guhs:guhwinter_plein_mag}
 *       are fine; clicking blocks and NPCs works as usual), no buckets, no hitting item frames / paintings / armour stands;</li>
 *   <li>no explosions, no mob griefing, no pistons pushing in or out, no trampling;</li>
 *   <li>fire and fluids from outside: through {@link Protected};</li>
 *   <li>no hostile mobs spawn inside by themselves (spawn eggs and commands still work).</li>
 * </ul>
 * Who may change it anyway: {@link #uitzondering} (lootjes: a player in their own plot), and ops in creative mode.
 * NOTE: plain creative is NOT enough (the lootjes builders are in creative).
 */
public final class GuhwinterZone {
    public static final int MIN_X = -350, MAX_X = -276, MIN_Z = -735, MAX_Z = -661, MIN_Y = 20, MAX_Y = 90;
    /** The plaza in block coordinates (inclusive), as an AABB (the far edges + 1). */
    public static final AABB PLEIN = new AABB(MIN_X, MIN_Y, MIN_Z, MAX_X + 1, MAX_Y + 1, MAX_Z + 1);
    /** The dimension of the plaza. */
    public static final ResourceKey<Level> DIMENSIE = ModDimensions.GUHMENSION;

    private static final List<BiPredicate<Player, BlockPos>> UITZONDERINGEN = new CopyOnWriteArrayList<>();

    private GuhwinterZone() {
    }

    /** Is this spot on the plaza (in the Guhmension, server side)? */
    public static boolean isPlein(Level level, BlockPos pos) {
        return !level.isClientSide() && level.dimension() == DIMENSIE && binnen(pos);
    }

    /** Only the coordinates (any dimension). */
    public static boolean binnen(BlockPos pos) {
        return pos.getX() >= MIN_X && pos.getX() <= MAX_X && pos.getZ() >= MIN_Z && pos.getZ() <= MAX_Z
                && pos.getY() >= MIN_Y && pos.getY() <= MAX_Y;
    }

    /** Lootjes (and others): this player may change this spot of the plaza after all (e.g. inside their own 7x7 plot). */
    public static void uitzondering(BiPredicate<Player, BlockPos> mag) {
        UITZONDERINGEN.add(mag);
    }

    /** Does one of the {@link #uitzondering}s allow this? */
    public static boolean uitzondering(Player player, BlockPos pos) {
        for (BiPredicate<Player, BlockPos> u : UITZONDERINGEN) {
            if (u.test(player, pos)) {
                return true;
            }
        }
        return false;
    }

    /** May this player break / place / use items at this spot? Outside the plaza always; inside only by exception or as an op in creative. */
    public static boolean mag(Player player, BlockPos pos) {
        if (!isPlein(player.level(), pos)) {
            return true;
        }
        if (player.getAbilities().instabuild && player instanceof ServerPlayer sp && sp.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        return uitzondering(player, pos);
    }

    private static boolean geweigerd(Player player, BlockPos pos, boolean melden) {
        if (mag(player, pos)) {
            return false;
        }
        if (melden) {
            player.sendOverlayMessage(Component.translatable("guhwinter.kern.geen_bouw").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    /** (GuhwinterFeature) the protection. */
    public static void register() {
        Protected.add(GuhwinterZone::isPlein);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opBreek);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opPlaats);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opGebruik);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opItem);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opAanval);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opEntiteit);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opExplosie);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opGriefing);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opZuiger);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opVertrappen);
        NeoForge.EVENT_BUS.addListener(GuhwinterZone::opSpawn);
    }

    static void opBreek(BreakBlockEvent event) {
        if (geweigerd(event.getPlayer(), event.getPos(), true)) {
            event.setCanceled(true);
        }
    }

    static void opPlaats(BlockEvent.EntityPlaceEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Player p ? geweigerd(p, event.getPos(), true) : e != null && isPlein(e.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Is this item fine to use on plaza blocks (it changes nothing)? */
    static boolean onschuldig(ItemStack stack) {
        return stack.isEmpty() || stack.has(DataComponents.FOOD) || stack.is(net.minecraft.world.item.Items.FIREWORK_ROCKET) || stack.is(KernRegistry.PLEIN_MAG);
    }

    /** An item used on a block: placing and pouring look at the spot next to it, the rest (axe, hoe, egg...) at the block itself. */
    static void opGebruik(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || onschuldig(stack)) {
            return;
        }
        boolean ernaast = stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem;
        BlockPos pos = ernaast ? event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()) : event.getPos();
        boolean melden = ernaast || stack.getItem() instanceof SpawnEggItem;
        if (geweigerd(event.getEntity(), pos, melden)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are emptied where you look, also without clicking a block. */
    static void opItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !(event.getItemStack().getItem() instanceof BucketItem)) {
            return;
        }
        Player player = event.getEntity();
        Vec3 oog = player.getEyePosition();
        Vec3 eind = oog.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = event.getLevel().clip(new net.minecraft.world.level.ClipContext(oog, eind, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.ANY, player));
        BlockPos doel = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : player.blockPosition();
        if (geweigerd(player, doel, true) || hit.getType() == HitResult.Type.BLOCK && geweigerd(player, hit.getBlockPos(), true)) {
            event.setCanceled(true);
        }
    }

    /** Hitting an item frame, painting or armour stand. */
    static void opAanval(AttackEntityEvent event) {
        Entity t = event.getTarget();
        if ((t instanceof HangingEntity || t instanceof ArmorStand) && geweigerd(event.getEntity(), t.blockPosition(), true)) {
            event.setCanceled(true);
        }
    }

    /** Taking or turning what's in an item frame, dressing an armour stand. */
    static void opEntiteit(PlayerInteractEvent.EntityInteractSpecific event) {
        Entity t = event.getTarget();
        if (!event.getLevel().isClientSide() && (t instanceof HangingEntity || t instanceof ArmorStand) && geweigerd(event.getEntity(), t.blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    static void opExplosie(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == DIMENSIE) {
            event.getAffectedBlocks().removeIf(GuhwinterZone::binnen);
        }
    }

    static void opGriefing(EntityMobGriefingEvent event) {
        Entity e = event.getEntity();
        if (e != null && !(e instanceof Player) && (isPlein(e.level(), e.blockPosition()) || isPlein(e.level(), e.blockPosition().below()))) {
            event.setCanGrief(false);
        }
    }

    /** A piston never pushes or pulls blocks of the plaza, nor blocks into it. */
    static void opZuiger(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != DIMENSIE) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!PLEIN.inflate(14).contains(Vec3.atCenterOf(pos))) {
            return;
        }
        PistonStructureResolver r = event.getStructureHelper();
        if (r == null) {
            return;
        }
        boolean raakt = binnen(pos.relative(event.getDirection()));
        if (r.resolve()) {
            Direction d = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
            for (BlockPos p : r.getToPush()) {
                raakt |= binnen(p) || binnen(p.relative(d));
            }
            for (BlockPos p : r.getToDestroy()) {
                raakt |= binnen(p);
            }
        }
        if (raakt) {
            event.setCanceled(true);
        }
    }

    static void opVertrappen(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isPlein(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** No monster comes by itself (natural, chunk generation, patrols, reinforcements, spawners). */
    static void opSpawn(MobSpawnEvent.PositionCheck event) {
        Entity e = event.getEntity();
        EntitySpawnReason r = event.getSpawnType();
        if ((e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER)
                && (r == EntitySpawnReason.NATURAL || r == EntitySpawnReason.CHUNK_GENERATION || r == EntitySpawnReason.PATROL
                || r == EntitySpawnReason.REINFORCEMENT || r == EntitySpawnReason.SPAWNER || r == EntitySpawnReason.TRIAL_SPAWNER)
                && isPlein(event.getLevel().getLevel(), e.blockPosition())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
