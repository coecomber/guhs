package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * einde2: the big places of the outer islands can't be broken or built in, so every player finds them whole: no
 * breaking, no placing blocks, no buckets, no explosions, no griefing mobs inside the boxes of a registered structure's
 * pieces (plus a rim), and no fire or fluids from outside ({@link Protected}). Doors, chests, buttons and riding still
 * work; creative players may change everything. The empty rim piece of a story structure protects nothing. A bed in a
 * protected place never explodes ({@link #bedBlijftHeel}), a cake that stands there for show is never eaten
 * ({@link #taartBlijftHeel}: the first visitor would finish it for everybody after them), and what the builders put down
 * as decoration never hurts ({@link #doetGeenPijn}: a camp fire, a stalagmite, a hot floor).
 * <p>
 * Since the merge of origin/main (1.4.4) the protection itself is bbq2's {@link Bescherming}: {@link #registreer} and
 * {@link #uitzondering} hand everything to it (so machines, pistons and a sluier know the einde2 places too), and the
 * questions here answer "which einde2 place is this". What stays in this class: the beds, cakes and harmless decoration,
 * a bucket emptied in the air, and the {@link #TEST_AREAS} of the game tests (only those boxes are guarded by the events
 * below; every real place is guarded by Bescherming's own events).
 */
public final class Einde2Bescherming {
    /** Extra protected boxes, only for the game tests (the test world has no generated structures). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();
    /** The name the {@link #TEST_AREAS} are protected under (for {@link #uitzondering} in a test). */
    public static final String TEST = "einde2_test";

    /** The structures einde2 registered with {@link Bescherming} (their names). */
    private static final Set<String> EIGEN = ConcurrentHashMap.newKeySet();
    /** The exceptions of the {@link #TEST} areas (the real ones live in Bescherming). */
    private static final Map<String, List<BiPredicate<ServerPlayer, BlockPos>>> UITZONDERINGEN = new ConcurrentHashMap<>();

    private Einde2Bescherming() {
    }

    /** Nobody breaks, places, explodes, floods or griefs inside the boxes of this structure's pieces (+ rand blocks). */
    public static void registreer(String structuur, int rand) {
        registreer(structuur, rand, start -> true);
    }

    /** The same for only some copies (the modular Mika-vestingen: the ones generated before einde2 stay as they are). */
    public static void registreer(String structuur, int rand, Predicate<StructureStart> welke) {
        EIGEN.add(structuur);
        // (the empty rim piece of a story structure keeps room free for the sluier of phase B: it protects nothing)
        Bescherming.registreer(structuur, rand, welke, piece -> !(piece instanceof LeegteBouwStructure.Rand));
    }

    /** Blocks of this structure a player may change after all (a quest block, a door that opens for them): Bescherming's. */
    public static void uitzondering(String structuur, BiPredicate<ServerPlayer, BlockPos> mag) {
        if (TEST.equals(structuur)) {
            UITZONDERINGEN.computeIfAbsent(structuur, k -> new CopyOnWriteArrayList<>()).add(mag);
        } else {
            Bescherming.uitzondering(structuur, mag);
        }
    }

    /** Is this spot part of a protected einde2 structure (server side; false on the client)? */
    public static boolean beschermd(Level level, BlockPos pos) {
        return structuurBij(level, pos) != null;
    }

    /** The protected structure this spot belongs to (null: none). */
    @Nullable
    public static String structuurBij(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        if (testStructuur(pos) != null) {
            return TEST;
        }
        if (EIGEN.isEmpty() || server.dimension() != GuheindeFeature.GUHEINDE) {
            return null;
        }
        String structuur = Bescherming.structuurBij(server, pos);
        return structuur != null && EIGEN.contains(structuur) ? structuur : null;
    }

    /** {@link #TEST} when the spot lies in one of the {@link #TEST_AREAS}, else null. */
    @Nullable
    private static String testStructuur(BlockPos pos) {
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return TEST;
            }
        }
        return null;
    }

    /** (the {@link #TEST_AREAS} only: Bescherming guards every real place with its own events) */
    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild || player.level().isClientSide()) {
            return false;
        }
        String structuur = testStructuur(pos);
        if (structuur == null) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            for (BiPredicate<ServerPlayer, BlockPos> mag : UITZONDERINGEN.getOrDefault(structuur, List.of())) {
                if (mag.test(sp, pos)) {
                    return false;
                }
            }
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onBreak);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onPlace);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onUseItem);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onExplosion);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onMobGriefing);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onDamage);
        // (fire and fluids from outside: Bescherming adds its own places to Protected; the test areas are added here)
        Protected.add((level, pos) -> level instanceof ServerLevel && testStructuur(pos) != null);
    }

    private static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), false)
                : entity != null && !entity.level().isClientSide() && testStructuur(event.getPos()) != null) {
            event.setCanceled(true);
        }
    }

    /**
     * A bed in a protected place: the Guheinde's beds explode when clicked (the dimension's bed rule), and the towns of
     * the outer islands are full of them. Here a bed only says a line; nobody gets hurt.
     */
    public static boolean bedBlijftHeel(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BedBlock && beschermd(level, pos);
    }

    /**
     * A cake in a protected place (a vanilla cake, a guh taart, a cake with a candle): it stands there for show, for every
     * player who comes by. Eaten it would be gone for good after the first visitor, and nobody can put a new one down. A
     * click only says a line (creative players may eat or change it).
     */
    public static boolean taartBlijftHeel(Level level, BlockPos pos) {
        return (level.getBlockState(pos).getBlock() instanceof CakeBlock || level.getBlockState(pos).getBlock() instanceof CandleCakeBlock)
                && beschermd(level, pos);
    }

    /**
     * The harm a decoration can do: the builders used lit camp fires, stalagmites as teeth and spires, and hot floors. In a
     * protected place none of that hurts anybody, player or guh (nobody is really harmed in the towns of the outer islands;
     * what a creature or a fall does is not touched).
     */
    public static boolean doetGeenPijn(DamageSource source) {
        return source.is(DamageTypes.CAMPFIRE) || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH);
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (doetGeenPijn(event.getSource()) && beschermd(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (bedBlijftHeel(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.getEntity().sendOverlayMessage(Component.translatable("gui.guhs.einde2.bed").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!event.getEntity().getAbilities().instabuild && taartBlijftHeel(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.getEntity().sendOverlayMessage(Component.translatable("gui.guhs.einde2.taart").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** A bucket emptied in the air while standing in an einde2 place (Bescherming only looks at a clicked block). */
    private static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !(event.getItemStack().getItem() instanceof BucketItem)) {
            return;
        }
        BlockPos pos = event.getEntity().blockPosition();
        if (denied(event.getEntity(), pos, false)) {
            event.setCanceled(true);
        } else if (testStructuur(pos) == null && structuurBij(event.getLevel(), pos) != null && !Bescherming.mag(event.getEntity(), pos)) {
            event.getEntity().sendOverlayMessage(Component.translatable("gui.guhs.einde2.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!TEST_AREAS.isEmpty() && event.getLevel() instanceof ServerLevel) {
            event.getAffectedBlocks().removeIf(pos -> testStructuur(pos) != null);
        }
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        if (!TEST_AREAS.isEmpty() && event.getEntity() != null && !(event.getEntity() instanceof Player)
                && !event.getEntity().level().isClientSide() && testStructuur(event.getEntity().blockPosition()) != null) {
            event.setCanGrief(false);
        }
    }
}
