package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** The Vadsrijk-vonk: right-click a closed frame of Vadsrijk-poortsteen to light your own portal (after your first visit). */
public class VonkItem extends Item {
    public VonkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        if (!c.getLevel().getBlockState(c.getClickedPos()).is(WereldRegistry.POORTSTEEN.get())) {
            return InteractionResult.PASS;
        }
        if (!(c.getLevel() instanceof ServerLevel sl) || !(c.getPlayer() instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        if (!Portalen.magBouwen(p)) {
            Portalen.geweigerd(p);
            return InteractionResult.FAIL;
        }
        int n = Portalen.ontsteek(sl, c.getClickedPos());
        if (n == 0) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.portaal.geen_raam").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.FAIL;
        }
        sl.playSound(null, c.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1.2f);
        sl.sendParticles(ParticleTypes.END_ROD, c.getClickedPos().getX() + 0.5, c.getClickedPos().getY() + 1, c.getClickedPos().getZ() + 0.5,
                20, 0.8, 0.8, 0.8, 0.05);
        if (!p.getAbilities().instabuild) {
            c.getItemInHand().shrink(1);
        }
        p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.portaal.aan").withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }
}
