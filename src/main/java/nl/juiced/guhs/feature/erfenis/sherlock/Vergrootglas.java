package nl.juiced.guhs.feature.erfenis.sherlock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Sherlock's vergrootglas (from chapter 1): hold it in your own village while a case runs and the clues nearby glitter; use
 * it while looking at one to turn it into a card. Without a case it only has opinions ("Dit is gras. Heel verdacht gras.").
 */
public class Vergrootglas extends Item {
    public Vergrootglas(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            Sherlock.speur(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp) {
            Sherlock.speur(sp);
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean vast(Player p) {
        return p.getMainHandItem().getItem() instanceof Vergrootglas || p.getOffhandItem().getItem() instanceof Vergrootglas;
    }

    public static boolean heeft(Player p) {
        return vast(p) || p.getInventory().countItem(SherlockFeature.VERGROOTGLAS.get()) > 0;
    }
}
