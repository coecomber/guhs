package nl.juiced.guhs.feature.vadsrijk.kleding.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.kleding.Kledingkast;

/**
 * T-KLEDING: what the wardrobe changes on a player model.
 * <ul>
 *   <li>{@link #extract} (render-state modifier, every rendered player): the transmog (the armour render state gets the chosen look's
 *   item, or nothing when hidden; Vadsrijk only), the cosmetic cape (the skin's cape texture swapped for ours; with wings on the cape
 *   is hidden: wings and capes share the back), and the item render states of the wings + head accessory (with the dye);</li>
 *   <li>{@link #submit}: draws the two wing halves on the upper back and the accessory on the head.</li>
 * </ul>
 * <b>Wings</b> are two item models ({@code assets/guhs/items/vr_t_kleding_vleugel_<naam>.json}, cuboid feathers, tinted by the dye),
 * animated in code like GeckoLib bones would be: folded while walking/standing, spread while in the air, a soft flap while gliding
 * (slow sinking), lerped per frame. LOD: further than {@link #LOD} blocks from the camera the pose is static (no flap, no lerp).
 * (Decision: no GeckoLib geo on the player: a GeckoLib render layer needs a GeoRenderer parent, and the avatar renderer is vanilla;
 * the "bones" here are the two halves with a yaw + roll each, which is all the three poses need.)
 */
public class KledingLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final double LOD = 24;
    static final ContextKey<Teken> TEKEN = new ContextKey<>(Guhs.id("vr_t_kleding_teken"));

    /** What to draw for one player this frame. */
    record Teken(ItemStackRenderState vleugel, ItemStackRenderState hoofd, float spreid, float rol, boolean helm) {
    }

    /** Per player: the current spread/roll (lerped towards the target pose). */
    private static final Map<UUID, float[]> POSE = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack> STAPELS = new ConcurrentHashMap<>();

    public KledingLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** The paper stack drawn as one of our models, tinted. */
    static ItemStack stapel(String model, int verf) {
        return STAPELS.computeIfAbsent(model + "#" + verf, k -> {
            ItemStack s = new ItemStack(Items.PAPER);
            s.set(DataComponents.ITEM_MODEL, Guhs.id(model));
            if (verf >= 0) {
                s.set(DataComponents.DYED_COLOR, new DyedItemColor(verf));
            }
            return s;
        });
    }

    static ItemStack look(String itemId) {
        return STAPELS.computeIfAbsent("look#" + itemId, k -> {
            Identifier id = Identifier.tryParse(itemId);
            Item it = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
            return it == null || it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
        });
    }

    /** Extract time: the player is still here. */
    static void extract(Player player, AvatarRenderState state) {
        UUID u = player.getUUID();
        CompoundTag uit = KledingCache.uiterlijk(u);
        if (uit.isEmpty() || player.isInvisible()) {
            return;
        }
        // transmog (Vadsrijk gear only)
        if (Vadsrijk.in(player)) {
            state.headEquipment = tm(uit, EquipmentSlot.HEAD, state.headEquipment);
            state.chestEquipment = tm(uit, EquipmentSlot.CHEST, state.chestEquipment);
            state.legsEquipment = tm(uit, EquipmentSlot.LEGS, state.legsEquipment);
            state.feetEquipment = tm(uit, EquipmentSlot.FEET, state.feetEquipment);
        }
        int verf = uit.getIntOr("verf", -1);
        Cosmetica.Ding vleugel = KledingCache.vleugel(u);
        Cosmetica.Ding kaap = KledingCache.kaap(u);
        if (kaap != null) {
            Identifier tex = Guhs.id("textures/vadsrijk/kleding/kaap_" + kaap.naam() + ".png");
            PlayerSkin s = state.skin;
            state.skin = new PlayerSkin(s.body(), new ClientAsset.ResourceTexture(Guhs.id("vadsrijk/kleding/kaap_" + kaap.naam()), tex), s.elytra(),
                    s.model(), s.secure());
            state.showCape = true;
        } else if (vleugel != null) {
            state.showCape = false;   // (wings on the back: no cape under them)
        }
        Cosmetica.Ding hoofd = Cosmetica.van(uit.getStringOr("hoofd", ""));
        if (vleugel == null && hoofd == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState vl = new ItemStackRenderState(), hd = new ItemStackRenderState();
        if (vleugel != null) {
            mc.getItemModelResolver().updateForLiving(vl, stapel("vr_t_kleding_vleugel_" + vleugel.naam(), verf), ItemDisplayContext.NONE, player);
        }
        if (hoofd != null) {
            mc.getItemModelResolver().updateForLiving(hd, stapel("vr_t_kleding_accessoire_" + hoofd.naam(), verf), ItemDisplayContext.NONE, player);
        }
        float[] pose = pose(player, mc);
        state.setRenderData(TEKEN, new Teken(vl, hd, pose[0], pose[1], !state.headEquipment.isEmpty()));
    }

    private static ItemStack tm(CompoundTag uit, EquipmentSlot slot, ItemStack echt) {
        String k = uit.getStringOr("tm." + slot.getName(), "");
        if (k.isEmpty() || echt.isEmpty()) {
            return echt;
        }
        if (k.equals(Kledingkast.VERBORGEN)) {
            return ItemStack.EMPTY;
        }
        ItemStack l = look(k);
        return l.isEmpty() ? echt : l;
    }

    /** Target pose: {spread yaw (deg, 0 = straight out sideways, 75 = folded on the back), roll (deg, + = tips down)}. */
    private static float[] pose(Player p, Minecraft mc) {
        float t = p.tickCount + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean ver = mc.gameRenderer.getMainCamera().position().distanceToSqr(p.position()) > LOD * LOD;
        boolean lucht = !p.onGround() && !p.isPassenger() && !p.isInWater() && !p.onClimbable();
        double dy = p.getY() - p.yo;
        boolean zweeft = p == mc.player ? KledingClient.zweeftNu() : lucht && dy < -0.02 && dy > -0.17;
        float spreid, rol;
        if (!lucht) {
            spreid = 72f;
            rol = 18f + (ver ? 0 : (float) Math.sin(t * 0.08f) * 2f);   // (a little breathing)
        } else if (zweeft) {
            spreid = 8f;
            rol = ver ? -6f : -6f + (float) Math.sin(t * 0.35f) * 14f;   // (a soft flap)
        } else {
            spreid = 18f;
            rol = -14f;
        }
        if (ver) {
            POSE.remove(p.getUUID());
            return new float[] {spreid, rol};
        }
        float[] nu = POSE.computeIfAbsent(p.getUUID(), k -> new float[] {spreid, rol});
        nu[0] += (spreid - nu[0]) * 0.2f;
        nu[1] += (rol - nu[1]) * 0.25f;
        if (POSE.size() > 256) {
            POSE.clear();
        }
        return new float[] {nu[0], nu[1]};
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Teken t = state.getRenderData(TEKEN);
        if (t == null) {
            return;
        }
        PlayerModel model = getParentModel();
        if (!t.vleugel().isEmpty()) {
            for (int kant : new int[] {1, -1}) {
                ps.pushPose();
                model.root().translateAndRotate(ps);
                model.body.translateAndRotate(ps);
                // body space (blocks, y down, +z = back): the hinge on the upper back, just off the middle
                ps.translate(kant * 0.06, 0.18, 0.16);
                if (kant < 0) {
                    ps.mulPose(Axis.YP.rotationDegrees(180));   // (the right half: the same model turned round)
                }
                ps.mulPose(Axis.YP.rotationDegrees(-kant * t.spreid()));   // (both tips swing to the back: +z)
                ps.mulPose(Axis.ZP.rotationDegrees(t.rol()));
                // item space (y up, hinge at (0, 8, 8) px) onto the hinge
                ps.mulPose(Axis.XP.rotationDegrees(180));
                ps.scale(0.85f, 0.85f, 0.85f);
                ps.translate(0, -0.5, -0.5);
                t.vleugel().submit(ps, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                ps.popPose();
            }
        }
        if (!t.hoofd().isEmpty()) {
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.head.translateAndRotate(ps);
            if (t.helm()) {
                ps.translate(0, -1.2 / 16.0, 0);   // (on top of a helmet)
                ps.scale(1.12f, 1.12f, 1.12f);
            }
            // head space (y down, head cube y -8..0 px, x/z -4..4): item space (y up) with the head as px (4..12, 0..8, 4..12)
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(-0.5, 0, -0.5);
            t.hoofd().submit(ps, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            ps.popPose();
        }
    }
}
