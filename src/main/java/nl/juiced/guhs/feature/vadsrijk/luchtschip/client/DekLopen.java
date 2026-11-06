package nl.juiced.guhs.feature.vadsrijk.luchtschip.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.LuchtschipDek;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.LuchtschipEntity;

/**
 * U-LUCHTSCHIP, client: walk on the deck of the airship you are on (WASD, sprint a bit faster), predicted here so it never lags;
 * the place goes to the server ({@link LuchtschipDek.Stap}). Jumping does nothing on board and the railing holds you
 * ({@link LuchtschipEntity#klem}).
 */
public final class DekLopen {
    private static int schip = -1;
    private static Vec3 vorigGestuurd;

    private DekLopen() {
    }

    static void init() {
        LuchtschipDek.eigen = () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p == null ? -1 : p.getId();
        };
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused() || !(p.getVehicle() instanceof LuchtschipEntity e)) {
            schip = -1;
            vorigGestuurd = null;
            return;
        }
        if (schip != e.getId()) {
            schip = e.getId();
            vorigGestuurd = null;
        }
        Vec3 nu = e.dekPlek(p);
        Vec3 correctie = LuchtschipDek.correctie;
        if (correctie != null) {
            LuchtschipDek.correctie = null;
            nu = correctie;
            vorigGestuurd = correctie;
        }
        Vec2 in = mc.screen == null ? p.input.getMoveVector() : Vec2.ZERO;
        double lengte = Math.sqrt(in.x * in.x + in.y * in.y);
        Vec3 nieuw = nu;
        if (lengte > 1e-4) {
            double snel = (p.input.keyPresses.sprint() || mc.options.keySprint.isDown() ? LuchtschipEntity.DEK_REN : LuchtschipEntity.DEK_LOOP)
                    / Math.max(1, lengte);
            double links = in.x * snel, vooruit = in.y * snel;
            float yaw = p.getYRot() * Mth.DEG_TO_RAD;
            double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
            // the world direction as LivingEntity#moveRelative computes it, then turned into the ship's frame
            Vec3 wereld = new Vec3(links * cos - vooruit * sin, 0, vooruit * cos + links * sin);
            Vec3 inSchip = wereld.yRot(e.getYRot() * Mth.DEG_TO_RAD);
            nieuw = LuchtschipEntity.klem(nu.x + inSchip.x, nu.z + inSchip.z);
        }
        if (vorigGestuurd == null) {
            vorigGestuurd = nu;   // (the start place is the server's: nothing to send until you walk)
        }
        if (vorigGestuurd.distanceToSqr(nieuw) > 1e-6) {
            LuchtschipDek.zetEigen(e, p, nieuw);
            vorigGestuurd = nieuw;
            ClientPacketDistributor.sendToServer(new LuchtschipDek.Stap(e.getId(), (float) nieuw.x, (float) nieuw.z));
        }
    }
}
