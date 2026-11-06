package nl.juiced.guhs.feature.vadsrijk.zichtbaar.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kleding.client.KledingCache;
import nl.juiced.guhs.feature.vadsrijk.zichtbaar.Looks;

/**
 * U-ZICHTBAAR: the gear of the virtual slots on the player model (Vadsrijk only).
 * <ul>
 *   <li><b>Cape</b>: through the VANILLA cape layer (cloth physics for free): {@link #extract} swaps the skin's cape texture for
 *   the cape's look ({@code textures/vadsrijk/zichtbaar/cape_<naam>.png}) and turns the cape on, unless the wardrobe draws on the
 *   back (a cosmetic cape replaces it, wings hide it: T-KLEDING's rule, {@link Looks#statCapeTonen}). Elytra still win (vanilla).</li>
 *   <li><b>Gloves</b> on both lower arms, the <b>armband</b> on the left wrist, <b>ring</b> 1/2 on the right/left hand, the
 *   <b>necklace</b> on the chest, <b>earrings</b> on both sides of the head: small cuboid item models (generator
 *   {@code vadsrijk_u_zichtbaar.py}), gem colours by dye tint. Pushed outward over worn armour (chest piece / helmet).</li>
 *   <li><b>Budget</b>: LOD per part ({@link Looks#teken}: rings/earrings 16 blocks, necklace/armband 32, gloves 48) and at most
 *   {@link Looks#BUDGET} other players per frame get the small parts; you always do. The cape costs nothing extra.</li>
 * </ul>
 * The armour pieces (real armour slots with equipment assets) and the weapon (main hand) are drawn by vanilla already; the
 * wardrobe's transmog changes them in {@code KledingLaag}.
 */
public class ZichtbaarLaag extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final ContextKey<Teken> TEKEN = new ContextKey<>(Guhs.id("vr_u_zichtbaar_teken"));

    /** What to draw for one player this frame (empty states = nothing). */
    record Teken(ItemStackRenderState handschoen, ItemStackRenderState armband, ItemStackRenderState ring1, ItemStackRenderState ring2,
            ItemStackRenderState ketting, ItemStackRenderState oorbel) {
    }

    private static final Map<String, ItemStack> STAPELS = new ConcurrentHashMap<>();
    private final boolean slim;

    public ZichtbaarLaag(RenderLayerParent<AvatarRenderState, PlayerModel> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    /** A paper stack drawn as one of our models, tinted (verf < 0 = no tint). */
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

    static void extract(Player player, AvatarRenderState state) {
        UUID u = player.getUUID();
        List<String> looks = ZichtbaarClient.looks(u);
        if (looks.isEmpty() || player.isInvisible() || player.isSpectator() || !Vadsrijk.in(player)) {
            return;
        }
        // the cape (vanilla layer)
        String cape = Looks.van(looks, Slot.CAPE);
        if (Looks.statCapeTonen(!cape.isEmpty(), KledingCache.kaap(u) != null, KledingCache.vleugel(u) != null)) {
            String tex = Looks.kaapTextuur(cape);
            PlayerSkin s = state.skin;
            state.skin = new PlayerSkin(s.body(), new ClientAsset.ResourceTexture(Guhs.id(tex.substring("textures/".length(), tex.length() - 4)),
                    Guhs.id(tex)), s.elytra(), s.model(), s.secure());
            state.showCape = true;
        }
        // the small parts: LOD + budget
        Minecraft mc = Minecraft.getInstance();
        boolean zelf = player == mc.player;
        double d2 = mc.gameRenderer.getMainCamera().position().distanceToSqr(player.position());
        if (!Looks.teken(Looks.Deel.HANDSCHOENEN, d2)) {
            return;
        }
        boolean iets = false;
        for (Slot s : Looks.VOLGORDE) {
            iets |= s != Slot.CAPE && !Looks.van(looks, s).isEmpty();
        }
        if (!iets) {
            return;
        }
        if (!zelf) {
            if (ZichtbaarClient.budgetGebruikt >= Looks.BUDGET) {
                return;
            }
            ZichtbaarClient.budgetGebruikt++;
        }
        state.setRenderData(TEKEN, new Teken(
                deel(mc, player, looks, Slot.HANDSCHOENEN, Looks.Deel.HANDSCHOENEN, d2),
                deel(mc, player, looks, Slot.ARMBAND, Looks.Deel.ARMBAND, d2),
                deel(mc, player, looks, Slot.RING, Looks.Deel.RING, d2),
                deel(mc, player, looks, Slot.RING2, Looks.Deel.RING, d2),
                deel(mc, player, looks, Slot.KETTING, Looks.Deel.KETTING, d2),
                deel(mc, player, looks, Slot.OORBEL, Looks.Deel.OORBEL, d2)));
    }

    private static ItemStackRenderState deel(Minecraft mc, Player p, List<String> looks, Slot s, Looks.Deel d, double d2) {
        ItemStackRenderState r = new ItemStackRenderState();
        String id = Looks.van(looks, s);
        if (id.isEmpty() || !Looks.teken(d, d2)) {
            return r;
        }
        boolean getint = s == Slot.KETTING || s == Slot.OORBEL || s == Slot.RING || s == Slot.RING2
                || Looks.model(s, id).endsWith("armband_standaard");
        mc.getItemModelResolver().updateForLiving(r, stapel(Looks.model(s, id), getint ? Looks.kleur(id) : -1), ItemDisplayContext.NONE, p);
        return r;
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Teken t = state.getRenderData(TEKEN);
        if (t == null) {
            return;
        }
        PlayerModel model = getParentModel();
        boolean jas = !state.chestEquipment.isEmpty(), helm = !state.headEquipment.isEmpty();
        // arms: centre of the arm box (wide: right -3..1, left -1..3; slim: right -2..1, left -1..2), slim arms are 3 px wide
        float rechts = slim ? -0.5f : -1f, links = -rechts, breed = slim ? 0.75f : 1f;
        arm(ps, collector, light, state, model, model.rightArm, rechts, breed, jas, t.handschoen());
        arm(ps, collector, light, state, model, model.leftArm, links, breed, jas, t.handschoen());
        arm(ps, collector, light, state, model, model.leftArm, links, breed, jas, t.armband());
        arm(ps, collector, light, state, model, model.rightArm, rechts, breed, jas, t.ring1());
        arm(ps, collector, light, state, model, model.leftArm, links, breed, jas, t.ring2());
        if (!t.ketting().isEmpty()) {
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.body.translateAndRotate(ps);
            if (jas) {
                ps.translate(0, 0, -0.8 / 16.0);   // (in front of a chest piece)
                ps.scale(1.2f, 1f, 1f);
            }
            teken(ps, collector, light, state, t.ketting());
        }
        if (!t.oorbel().isEmpty()) {
            ps.pushPose();
            model.root().translateAndRotate(ps);
            model.head.translateAndRotate(ps);
            if (helm) {
                ps.scale(1.2f, 1f, 1f);   // (outside a helmet)
            }
            teken(ps, collector, light, state, t.oorbel());
        }
    }

    private static void arm(PoseStack ps, SubmitNodeCollector collector, int light, AvatarRenderState state, PlayerModel model, ModelPart arm,
            float midden, float breed, boolean jas, ItemStackRenderState r) {
        if (r.isEmpty() || !arm.visible) {
            return;
        }
        ps.pushPose();
        model.root().translateAndRotate(ps);
        arm.translateAndRotate(ps);
        ps.translate(midden / 16.0, 0, 0);
        if (jas) {
            ps.scale(1.3f * breed, 1.08f, 1.3f);   // (over the sleeve of a chest piece)
        } else if (breed != 1f) {
            ps.scale(breed, 1f, 1f);
        }
        teken(ps, collector, light, state, r);
    }

    /** Item space -> body-part space (as T-KLEDING's head accessories): Z 180 deg, up half a block; pops the pose. */
    private static void teken(PoseStack ps, SubmitNodeCollector collector, int light, AvatarRenderState state, ItemStackRenderState r) {
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(0, 0.5, 0);
        r.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        ps.popPose();
    }
}
