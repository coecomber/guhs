package nl.juiced.guhs.feature.bouwhulp.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;

/**
 * The Bouwkeet: the centred 3 x 3 x 3 model, moved from the kern (bottom middle of the front) one block back to the
 * middle of the hut, then turned to its facing (GeckoLib's own turn by the block state).
 */
public class KeetRenderer extends GeoBlockRenderer<BouwkeetBlockEntity, BlockEntityRenderState> {
    private static final DataTicket<Direction> ACHTER = DataTicket.create("guhs_bouwkeet_achter", Direction.class);

    public KeetRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<BouwkeetBlockEntity>(Guhs.id("bouwkeet")));
    }

    @Override
    public void addRenderData(BouwkeetBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        state.addGeckolibData(ACHTER, be.voorkant().getOpposite());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> info) {
        Direction achter = info.getOrDefaultGeckolibData(ACHTER, Direction.SOUTH);
        info.poseStack().translate(achter.getStepX(), 0, achter.getStepZ());
        super.adjustRenderPose(info);
    }

    @Override
    public AABB getRenderBoundingBox(BouwkeetBlockEntity be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p).inflate(3, 0, 3).expandTowards(0, 5, 0);
    }
}
