package nl.juiced.guhs.feature.safari.verblijf.client;

import javax.annotation.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.client.SafariClient;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfPayloads;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * Op Safari! step 2 (S5), client: draws the verblijfsbord (GeckoLib, the board model with the species' portrait texture
 * {@code textures/block/safari/verblijfsbord_<soort>.png}), the animals at home (the park's renderer, plus the family's fur
 * tint and the stork's bundle) and opens the board's menu ({@link VerblijfScreen}).
 */
public final class VerblijfClient {
    private static final DataTicket<String> SOORT = DataTicket.create("guhs_verblijf_soort", String.class);
    private static final DataTicket<Boolean> BUNDEL = DataTicket.create("guhs_verblijf_bundel", Boolean.class);
    private static final Identifier MODEL = Guhs.id("block/safari/verblijfsbord");

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerBlockEntityRenderer(VerblijfFeature.BORD_BE.get(), BordRenderer::new);
            event.registerEntityRenderer(VerblijfFeature.THUISDIER.get(), ThuisRenderer::new);
        });
        VerblijfPayloads.opener = VerblijfClient::open;
    }

    private static void open(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof VerblijfScreen s && s.pos().asLong() == data.getLongOr("Pos", 0L)) {
                s.update(data);
            } else {
                mc.setScreen(new VerblijfScreen(data));
            }
        });
    }

    /** The board: the shared model, a texture per species (the plain lion-less board when the species is unknown). */
    public static class BordRenderer extends GeoBlockRenderer<VerblijfsbordBlockEntity, BlockEntityRenderState> {
        public BordRenderer(BlockEntityRendererProvider.Context context) {
            super(context, new GeoModel<>() {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return MODEL;
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    String s = state.getOrDefaultGeckolibData(SOORT, "");
                    return Guhs.id("textures/block/safari/verblijfsbord_" + (s.isEmpty() ? "leeuw" : s) + ".png");
                }

                @Override
                public Identifier getAnimationResource(VerblijfsbordBlockEntity animatable) {
                    return MODEL;
                }
            });
        }

        @Override
        public void addRenderData(VerblijfsbordBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
            state.addGeckolibData(SOORT, be.soortClient());
        }

        @Override
        public AABB getRenderBoundingBox(VerblijfsbordBlockEntity be) {
            return new AABB(be.getBlockPos()).inflate(1, 1, 1);
        }
    }

    /** An animal at home: the park animal, with its family's fur tint and (the stork) its bundle when it carries a parcel. */
    public static class ThuisRenderer extends SafariClient.DierRenderer {
        public ThuisRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void addRenderData(SafariDier dier, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            super.addRenderData(dier, related, state, partialTick);
            state.addGeckolibData(BUNDEL, dier instanceof ThuisDier t && t.bundel());
        }

        @Override
        public int getRenderColor(SafariDier dier, @Nullable Void related, float partialTick) {
            return dier instanceof ThuisDier t ? ThuisDier.tint(t.variatie()) : super.getRenderColor(dier, related, partialTick);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            super.adjustModelBonesForRender(info, bones);
            boolean bundel = info.getOrDefaultGeckolibData(BUNDEL, false);
            for (GeoBone bone : info.model().boneLookup().get().values()) {
                if (bone.name().equals("bundeltje")) {
                    bones.get(bone).skipRender(!bundel).skipChildrenRender(!bundel);
                }
            }
        }
    }

    private VerblijfClient() {
    }
}
