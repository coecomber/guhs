package nl.juiced.guhs.feature.dirigent;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.zingendeweide.ZingendeWeideFeature;

/**
 * The dirigeerstokje (and its golden twin, a rare prize that conducts just the same): click near (or on) a Koorkom's podium
 * to start a {@link Concert}; while you conduct, every click is a beat.
 */
public class Dirigeerstokje extends Item {
    private final boolean goud;

    public Dirigeerstokje(Properties properties, boolean goud) {
        super(properties);
        this.goud = goud;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            klik(sp, null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        boolean podium = context.getLevel().getBlockState(context.getClickedPos()).is(ZingendeWeideFeature.KOORKOM_PODIUM.get());
        if (podium || Concert.van(sp) != null) {
            klik(sp, podium ? context.getClickedPos() : null);
            return InteractionResult.SUCCESS;
        }
        klik(sp, null);
        return InteractionResult.SUCCESS;
    }

    /** A beat when you conduct; otherwise the start of a concert at the nearest podium. */
    static void klik(ServerPlayer p, BlockPos podium) {
        Concert c = Concert.van(p);
        if (c != null) {
            c.tik(p);
            return;
        }
        BlockPos waar = podium != null ? podium : Concert.podiumBij(p);
        if (waar == null) {
            p.sendOverlayMessage(Component.translatable(Concert.T + "geen_podium").withStyle(ChatFormatting.GRAY));
            return;
        }
        Concert.begin(p, waar);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return goud;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(goud ? "item.guhs.gouden_dirigeerstokje.lore" : "item.guhs.dirigeerstokje.lore")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.dirigeerstokje.gebruik").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
