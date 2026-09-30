package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The guh-brandslang (loaned by Brandweercommandant Blusguh): hold right-click to spray a jet of water where you look
 * ({@link #BEREIK} blocks). Every {@link #BLUS_TICKS} ticks of spraying on a marshmallow fire makes it one step smaller.
 * The water is just water: it doesn't hurt a soul.
 */
public class BrandslangItem extends Item {
    public static final double BEREIK = 11.0;
    public static final int BLUS_TICKS = 5;

    public BrandslangItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            int t = getUseDuration(stack, user) - remaining;
            spuit(player, t);
        }
    }

    /** One tick of spraying: the jet (particles, every few ticks the sound) and, now and then, the fire it hits. */
    public static void spuit(ServerPlayer player, int t) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(BEREIK));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double lengte = hit.getType() == HitResult.Type.MISS ? BEREIK : hit.getLocation().distanceTo(eye);
        if (t % 2 == 0) {
            for (double d = 1.2; d < lengte; d += 0.9) {
                Vec3 at = eye.add(look.scale(d)).add(0, -0.25 - 0.012 * d * d, 0);
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 3, 0.08, 0.08, 0.08, 0.05);
                if (((int) (d * 10)) % 3 == 0) {
                    level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
                }
            }
            Vec3 at = eye.add(look.scale(lengte));
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 10, 0.25, 0.1, 0.25, 0.1);
        }
        if (t % 8 == 0) {
            level.playSound(null, player.blockPosition(), BeroepenFeature.SPUITEN.get(), SoundSource.PLAYERS, 0.6f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        }
        if (t % BLUS_TICKS == 0) {
            BlockPos doel = raak(level, eye, look, lengte, hit);
            if (doel != null) {
                MarshmallowvuurBlock.blus(level, doel, player);
            }
        }
    }

    /** The burning fire the jet hits (or passes close by: its flames are tall), or null. */
    static BlockPos raak(ServerLevel level, Vec3 eye, Vec3 look, double lengte, BlockHitResult hit) {
        if (hit.getType() == HitResult.Type.BLOCK && brandt(level, hit.getBlockPos())) {
            return hit.getBlockPos();
        }
        for (double d = 0.5; d <= lengte + 0.5; d += 0.4) {
            BlockPos p = BlockPos.containing(eye.add(look.scale(d)));
            for (BlockPos q : new BlockPos[] {p, p.below(), p.below(2)}) {
                if (brandt(level, q)) {
                    return q;
                }
            }
        }
        return null;
    }

    private static boolean brandt(ServerLevel level, BlockPos p) {
        var s = level.getBlockState(p);
        return s.getBlock() instanceof MarshmallowvuurBlock && s.getValue(MarshmallowvuurBlock.VUUR) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guh_brandslang.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.guh_brandslang.lore2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
