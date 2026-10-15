package nl.juiced.guhs.feature.bio.dieren;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Koivoer: little pellets for the koi. Holding it near the water makes the koi come up and beg; right-click the water to
 * sprinkle a handful ({@link #strooi}): every koi close by swims to it and eats (hearts). You can also give it to one koi
 * directly. Crafted from kaasknabbels and seeds; the visser-guh of the botenhuisje hands it out too.
 */
public class KoivoerItem extends Item {
    /** Sprinkled koivoer lures the koi within this many blocks. */
    public static final double STROOI_AFSTAND = 8.0;

    public KoivoerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            ItemStack stack = player.getItemInHand(hand);
            strooi(server, sp, hit.getLocation());
            player.getCooldowns().addCooldown(stack, 12);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** A handful of koivoer on the water at this spot: the koi around it come and eat. Returns how many were lured. */
    public static int strooi(ServerLevel level, @Nullable ServerPlayer speler, Vec3 plek) {
        level.sendParticles(ParticleTypes.SPLASH, plek.x, plek.y + 0.1, plek.z, 10, 0.35, 0.05, 0.35, 0.02);
        level.playSound(null, plek.x, plek.y, plek.z, DierenSlice.KOI_STROOI.get(), SoundSource.NEUTRAL, 0.7f, 0.95f + level.getRandom().nextFloat() * 0.2f);
        List<KoiEntity> koi = level.getEntitiesOfClass(KoiEntity.class, new AABB(plek, plek).inflate(STROOI_AFSTAND), k -> k.isAlive() && k.isInWater());
        for (KoiEntity k : koi) {
            k.lok(plek, speler);
        }
        return koi.size();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.koivoer.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
