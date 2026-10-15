package nl.juiced.guhs.feature.sausdieren;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * Een Sausblubje in een potje ({@code guhs:sausblubje_potje}, a fixed id of CONTRACT_130 7): a small Sausblubje that hopped
 * into a glass bottle ({@link SausblubjeEntity}: right-click a small one with a bottle). Use it on a block to let it out
 * again (you keep the bottle); the Blubkacheltje of the tech-bronnen slice takes the item as it is. The item carries no
 * data of its own (only the Sausblubje's name, when it had one), so jars stack.
 */
public class SausblubjePotjeItem extends Item {
    public SausblubjePotjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos = pos.relative(context.getClickedFace());
        }
        SausblubjeEntity blubje = laatVrij(level, Vec3.atBottomCenterOf(pos), context.getItemInHand());
        if (blubje == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player != null) {
            blubje.setYRot(player.getYRot() + 180f);
            player.setItemInHand(context.getHand(), ItemUtils.createFilledResult(context.getItemInHand(), player, new ItemStack(Items.GLASS_BOTTLE)));
        } else {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** The small Sausblubje of this jar, in the world at this spot (it stays: it is somebody's). Null when it can't be made. */
    public static SausblubjeEntity laatVrij(ServerLevel level, Vec3 plek, ItemStack potje) {
        SausblubjeEntity blubje = SausdierenFeature.SAUSBLUBJE.get().create(level, EntitySpawnReason.BUCKET);
        if (blubje == null) {
            return null;
        }
        blubje.setGrootte(SausblubjeEntity.KLEIN);
        blubje.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0);
        blubje.setPersistenceRequired();
        Component naam = potje.get(DataComponents.CUSTOM_NAME);
        if (naam != null) {
            blubje.setCustomName(naam);
        }
        level.addFreshEntity(blubje);
        blubje.playSound(SausdierenFeature.POTJE.get(), 1.0f, 0.9f);
        level.sendParticles(ParticleTypes.HEART, plek.x, plek.y + 0.6, plek.z, 3, 0.2, 0.2, 0.2, 0.02);
        return blubje;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
