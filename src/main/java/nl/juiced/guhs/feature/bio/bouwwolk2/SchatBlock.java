package nl.juiced.guhs.feature.bio.bouwwolk2;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The hoard of the giant, at the back of his hall: every player takes ONE gouden knabbelkruimel a day
 * ({@link Dagtraktatie}). Whoever takes it with the giant lying there got past him (the proof for the quest).
 */
public class SchatBlock extends DecoBlock {
    public SchatBlock(Properties properties, VoxelShape noord) {
        super(properties, noord);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer speler) {
            pak(speler, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The crumb of today for this player. True when they got it. */
    public static boolean pak(ServerPlayer speler, BlockPos pos) {
        if (!Dagtraktatie.neem(speler, Dagtraktatie.SCHAT)) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.wolkenkasteeltje.schat.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Minigames.give(speler, new ItemStack(BouwWolk2Slice.KRUIMEL_ITEM.get()));
        speler.sendSystemMessage(Component.translatable("gui.guhs.wolkenkasteeltje.schat.pak").withStyle(ChatFormatting.LIGHT_PURPLE));
        speler.level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.6f);
        boolean reus = !speler.level().getEntitiesOfClass(ReuzenguhEntity.class, new AABB(pos).inflate(16), ReuzenguhEntity::isAlive).isEmpty();
        if (reus) {
            GuhAdvancements.grant(speler, "wolkenkasteeltje_langs_reus");
        }
        return true;
    }
}
