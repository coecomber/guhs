package nl.juiced.guhs.feature.vadsrijk.klassen.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Basisaanval;

/**
 * h-melee (round 3): melee basic attacks in the dimension, client side.
 * <ul>
 *   <li><b>Hold to attack</b>: holding the attack button with a melee weapon (or the fist) keeps hitting the Vadsrijk mob under
 *   the crosshair every time the vanilla attack indicator is full (kern sets the attack speed attribute to the weapon's
 *   interval), like ranged/magic already did. The hit goes through the normal vanilla attack packet, so the server's
 *   rhythm (kern {@code Schade.basisaanval}) stays the judge.</li>
 *   <li><b>Through plants</b>: short grass, flowers and reeds have an outline, so the vanilla crosshair picks the plant instead
 *   of the small Klodder behind it (the click then "breaks" grass and does nothing). When the crosshair is on a block, a
 *   Vadsrijk mob along the same line that is not behind a solid (colliding) block is hit instead.</li>
 * </ul>
 */
public final class NabijClient {
    private NabijClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (!e.isAttack() || p == null || mc.gameMode == null || !nabij(p) || mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.ENTITY) {
                return;
            }
            VadsrijkMob m = doel(mc, p);
            if (m != null) {
                mc.gameMode.attack(p, m);
                e.setSwingHand(true);
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || mc.gameMode == null || mc.screen != null || !mc.options.keyAttack.isDown() || !nabij(p) || p.isUsingItem()
                    || p.isSpectator() || p.getAttackStrengthScale(0f) < 1f) {
                return;
            }
            VadsrijkMob m = doel(mc, p);
            if (m != null) {
                mc.gameMode.attack(p, m);   // (resets the attack indicator: the next one waits a full interval)
                p.swing(InteractionHand.MAIN_HAND);
            }
        });
    }

    /** In the dimension with a melee weapon (or no class weapon: the punch). */
    static boolean nabij(LocalPlayer p) {
        return Vadsrijk.in(p) && !Basisaanval.isAfstandswapen(p.getMainHandItem());
    }

    /** The Vadsrijk mob to hit: the one under the crosshair, else one right behind plants along the view line (reach). */
    @Nullable
    static VadsrijkMob doel(Minecraft mc, LocalPlayer p) {
        HitResult h = mc.hitResult;
        if (h instanceof EntityHitResult eh) {
            return eh.getEntity() instanceof VadsrijkMob m && raakbaar(m) ? m : null;
        }
        double reik = p.entityInteractionRange();
        Vec3 oog = p.getEyePosition(1f), kijk = p.getViewVector(1f), eind = oog.add(kijk.scale(reik));
        AABB vak = p.getBoundingBox().expandTowards(kijk.scale(reik)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, oog, eind, vak, x -> x instanceof VadsrijkMob m && raakbaar(m), reik * reik);
        if (r == null) {
            return null;
        }
        // only through plants: a solid block between the eye and the mob still blocks (no hitting through walls)
        HitResult muur = p.level().clip(new ClipContext(oog, r.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return muur.getType() == HitResult.Type.MISS ? (VadsrijkMob) r.getEntity() : null;
    }

    private static boolean raakbaar(VadsrijkMob m) {
        return m.isAlive() && !m.geplofd() && m.isPickable();
    }
}
