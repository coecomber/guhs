package nl.juiced.guhs.feature.guhpixel.parkour.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.parkour.ObstakelBlock;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourPayloads;
import nl.juiced.guhs.feature.guhpixel.parkour.ParkourSlice;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.speelgoed.SpeelDeelBlock;

/**
 * Client side of the Guh-parkour: the post screen, the text on the Scorebord, and a guh on a route that crawls through a
 * kruiptunnel is not drawn (the tunnel shows its bump instead: a block state).
 */
public final class ParkourClient {
    public static void init(IEventBus modBus) {
        ParkourPayloads.schermOpener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ParkourScherm s && s.pos().asLong() == p.data().getLongOr("Pos", 0L)) {
                s.update(p.data());
            } else {
                mc.setScreen(new ParkourScherm(p.data()));
            }
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ParkourSlice.SCOREBORD_BE.get(), ScorebordRenderer::new));
        modBus.addListener((RegisterRenderStateModifiersEvent event) -> event.<GuhEntity, LivingEntityRenderState>registerEntityModifier(GuhRenderer.class,
                (guh, state) -> {
                    if (GuhHooks.heeft(guh, PxVlaggen.OP_ROUTE) && inKruiptunnel(guh)) {
                        state.isInvisible = true;
                        state.isInvisibleToPlayer = true;
                        state.nameTag = null;
                        state.scoreText = null;
                        state.shadowPieces.clear();
                        state.displayFireAnimation = false;
                        state.outlineColor = 0;
                    }
                }));
    }

    /** Is this guh inside the cloth of a kruiptunnel (its controller block or one of its two parts)? */
    private static boolean inKruiptunnel(GuhEntity guh) {
        BlockPos pos = guh.blockPosition();
        BlockState state = guh.level().getBlockState(pos);
        if (state.getBlock() instanceof SpeelDeelBlock) {
            state = guh.level().getBlockState(SpeelDeelBlock.controller(state, pos));
        }
        return state.getBlock() instanceof ObstakelBlock.Kruiptunnel;
    }

    private ParkourClient() {
    }
}
