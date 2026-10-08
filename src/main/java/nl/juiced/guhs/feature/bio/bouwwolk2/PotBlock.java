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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The pot of kaasknabbels at the end of the rainbow. Nobody guards it: every player takes a handful a day
 * ({@link Dagtraktatie}), and a few blocks of rainbow with it, so the rainbow is a building material for everybody who
 * finds a bridge (the recipe from wolkenpluis and dye is the other way).
 */
public class PotBlock extends DecoBlock {
    /** A handful: this many kaasknabbels, plus up to {@link #KNABBELS_EXTRA}. */
    public static final int KNABBELS = 3, KNABBELS_EXTRA = 2;
    /** The blocks of rainbow that come with it. */
    public static final int REGENBOOG = 4;

    public PotBlock(Properties properties, VoxelShape noord) {
        super(properties, noord);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer speler) {
            pak(speler, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The handful of today for this player. True when they got it. */
    public static boolean pak(ServerPlayer speler, BlockPos pos) {
        if (!Dagtraktatie.neem(speler, Dagtraktatie.POT)) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.regenboogbrug.pot.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Minigames.give(speler, new ItemStack(ModItems.KAAS_KNABBELS.get(), KNABBELS + speler.getRandom().nextInt(KNABBELS_EXTRA + 1)));
        Minigames.give(speler, new ItemStack(Bio.item("regenboogblok", Items.WHITE_WOOL), REGENBOOG));
        speler.sendSystemMessage(Component.translatable("gui.guhs.regenboogbrug.pot.pak").withStyle(ChatFormatting.LIGHT_PURPLE));
        speler.level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
        GuhAdvancements.grant(speler, "regenboogbrug_pot");
        return true;
    }
}
