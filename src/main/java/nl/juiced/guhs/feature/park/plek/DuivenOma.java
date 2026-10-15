package nl.juiced.guhs.feature.park.plek;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.4.2 (park-plekjes b): Oma-guh on her bench in the plekje duivenvoer_oma, feeding the Duifguhtjes.
 * <ul>
 *   <li>Talk to her: once a day (the day of the overworld) she gives you a handful ({@link #HANDJE}) of broodkruimels, as
 *       long as you have fewer than that; otherwise she chats (four lines).</li>
 *   <li>Every {@link #OMA_STROOIT} ticks, while somebody is near, she strews a handful herself: the Duifguhtjes come down at
 *       her feet and peck.</li>
 *   <li>{@link Kruimels}: right-click to strew a handful at your feet ({@link #strooi}): the Duifguhtjes round you come and
 *       peck there. Holding them makes the Duifguhtjes flutter round you ({@link DuifguhtjeEntity#voerder}).</li>
 * </ul>
 * Per player (GuhQuests.saved): {@value #DAG} = the day of the last handful + 1. Texts: quest.guhs.parkb.oma.*.
 */
public final class DuivenOma implements NpcRole {
    public static final String DAG = "guhs_parkb_kruimel_dag";
    public static final int HANDJE = 8, ZINNEN = 4;
    /** How often Oma-guh strews crumbs herself (ticks), and for how long strewn crumbs lie there. */
    public static final int OMA_STROOIT = 300, LIGT = 400;
    private static final String Q = "quest.guhs.parkb.oma.";

    /** Strewn crumbs: where and until when (game time), per dimension. Not saved: they are gone in 20 seconds anyway. */
    private record Voer(BlockPos plek, long tot) {
    }

    private static final Map<ResourceKey<Level>, List<Voer>> VOER = new ConcurrentHashMap<>();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        long dag = dag(p.level());
        if (GuhQuests.count(p, PlekB.KRUIMELS.get()) >= HANDJE) {
            GuhQuests.say(p, npc, Q + "genoeg");
            return;
        }
        if (GuhQuests.saved(p).getLongOr(DAG, 0L) == dag + 1) {
            GuhQuests.say(p, npc, p.getRandom().nextInt(3) == 0 ? Q + "morgen" : Q + "hallo" + p.getRandom().nextInt(ZINNEN));
            return;
        }
        GuhQuests.saved(p).putLong(DAG, dag + 1);
        Minigames.give(p, new ItemStack(PlekB.KRUIMELS.get(), HANDJE));
        GuhQuests.say(p, npc, Q + "kruimels");
        p.level().playSound(null, npc.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 0.6f, 1.2f);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level) || (npc.tickCount + npc.getId()) % OMA_STROOIT != 0
                || level.getNearestPlayer(npc, 32) == null) {
            return;
        }
        Vec3 voor = Vec3.directionFromRotation(0, npc.getYRot()).scale(1.6);
        strooi(level, npc.position().add(voor.x, 0, voor.z), null);
    }

    /** The day of the overworld (Oma-guh's crumbs and the sulking Mika count in whole days). */
    public static long dag(ServerLevel level) {
        return nl.juiced.guhs.world.GuhTime.day(level);
    }

    /**
     * A handful of crumbs strewn here: crumb particles, and every Duifguhtje within 20 blocks comes down round it to peck
     * (for {@link #LIGT} ticks). {@code wie}: the player who strewed (an advancement), or null (Oma-guh).
     */
    public static void strooi(ServerLevel level, Vec3 waar, @Nullable ServerPlayer wie) {
        BlockPos plek = BlockPos.containing(waar);
        List<Voer> lijst = VOER.computeIfAbsent(level.dimension(), d -> new CopyOnWriteArrayList<>());
        long nu = level.getGameTime();
        lijst.removeIf(v -> v.tot() < nu);
        lijst.add(new Voer(plek, nu + LIGT));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), waar.x, waar.y + 0.2, waar.z,
                14, 0.6, 0.05, 0.6, 0.05);
        level.playSound(null, plek, SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 0.4f, 1.6f);
        for (DuifguhtjeEntity d : level.getEntitiesOfClass(DuifguhtjeEntity.class, new net.minecraft.world.phys.AABB(plek).inflate(20))) {
            d.kom(plek);
        }
        if (wie != null) {
            GuhAdvancements.grant(wie, "parkb_kruimels");
        }
    }

    /** The newest crumbs that still lie within {@code r} blocks of this spot, or null. */
    @Nullable
    public static BlockPos voerplek(ServerLevel level, BlockPos bij, int r) {
        List<Voer> lijst = VOER.get(level.dimension());
        if (lijst == null || lijst.isEmpty()) {
            return null;
        }
        long nu = level.getGameTime();
        Voer best = null;
        for (Voer v : lijst) {
            if (v.tot() >= nu && v.plek().distSqr(bij) <= (double) r * r && (best == null || v.tot() > best.tot())) {
                best = v;
            }
        }
        return best == null ? null : best.plek();
    }

    /** (tests) forget all strewn crumbs. */
    public static void vergeetVoer() {
        VOER.clear();
    }

    // =====================================================================================================================
    /** Broodkruimels: right-click to strew a handful at your feet. */
    public static class Kruimels extends Item {
        public Kruimels(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
                Vec3 voor = Vec3.directionFromRotation(0, player.getYRot()).scale(1.2);
                strooi(server, player.position().add(voor.x, 0, voor.z), sp);
                sp.sendOverlayMessage(Component.translatable("gui.guhs.parkb.kruimels.gestrooid").withStyle(ChatFormatting.LIGHT_PURPLE));
                stack.consume(1, player);
            }
            player.getCooldowns().addCooldown(new ItemStack(this), 10);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.park_kruimels.lore").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
