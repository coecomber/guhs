package nl.juiced.guhs.feature.snuffel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws one of the island's models (a dog, a companion) in a screen: inside a box, turned by {@code yaw} (0 = it looks at
 * you), at {@code schaal} pixels per block, with its feet {@code voet} pixels below the middle of the box. The entity is a
 * client-only instance that the screen keeps and ticks itself.
 */
final class Tekenaar {
    private Tekenaar() {
    }

    static void teken(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, Entity e, float yaw, float pitch, float schaal, float voet) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float draai = 180 + yaw;
        e.setYRot(draai);
        e.yRotO = draai;
        e.setXRot(0);
        e.xRotO = 0;
        if (e instanceof LivingEntity l) {
            l.yBodyRot = draai;
            l.yBodyRotO = draai;
            l.yHeadRot = draai;
            l.yHeadRotO = draai;
        }
        EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(e, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        // (entities in a GUI are drawn picture-in-picture with their feet in the middle of the box; the translation moves them down)
        g.entity(state, schaal, new Vector3f(0, voet / schaal, 0), pose, camera, x1, y1, x2, y2);
    }
}
