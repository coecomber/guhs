package nl.juiced.guhs.feature.band.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.registry.ModEntities;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A little client-side stand-in of one of your guhs, built from its looks snapshot (variant, clothes, hair, baby), for
 * the Guhdex tab "Mijn guhs" and the huisje screen: the real guh is usually far away (or an item in a chest).
 */
public final class GuhPop {
    private GuhPop() {
    }

    /** A guh from its looks (null when there is no level). */
    @Nullable
    public static GuhEntity van(CompoundTag looks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        GuhEntity guh = ModEntities.GUH.get().create(mc.level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.byId(looks.getStringOr("Variant", "")));
        GuhPersonality aard = GuhPersonality.byId(looks.getStringOr("Personality", ""));
        if (aard != null) {
            guh.setPersonality(aard);
        }
        CompoundTag clothes = looks.getCompoundOrEmpty("Clothes");
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(clothes.getStringOr(slot.name(), ""));
            if (c != null) {
                guh.wear(c);
            }
        }
        if (looks.contains("Haar")) {
            guh.setHaarkleur(looks.getIntOr("Haar", 0));
        }
        if (looks.getBooleanOr("Baby", false)) {
            guh.setAge(-24000);
        }
        guh.hideName = true;
        return guh;
    }

    /** A maatje stand-in (muisje, Schilly, Poepschilly) by its kind, or null. */
    @Nullable
    public static LivingEntity maatje(String soort) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity e = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id(soort)).map(t -> t.create(mc.level, EntitySpawnReason.TRIGGERED)).orElse(null);
        return e instanceof LivingEntity le ? le : null;
    }

    /**
     * Draws the stand-in inside the box, turned by yaw (dragging) and tilted by pitch, sized by the model (a guh with its
     * tail, paws and a hat is about 2.2 x 1.6 blocks at scale 1).
     */
    public static void teken(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, LivingEntity e, float yaw, float pitch) {
        g.enableScissor(x1, y1, x2, y2);
        boolean guh = e instanceof GuhEntity;
        float groot = e instanceof AgeableMob a && a.isBaby() ? 0.6f : 1f;
        float breed = guh ? 2.2f : Math.max(0.6f, e.getBbWidth() * 2.2f), hoog = guh ? 1.6f : Math.max(0.5f, e.getBbHeight() * 2f);
        float schaal = Math.min((y2 - y1) * 0.72f / (hoog * groot), (x2 - x1) * 0.9f / (breed * groot));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);
        pose.mul(camera);
        e.yBodyRot = 180 + yaw;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(180 + yaw);
        e.setXRot(0);
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        // (1.1.0: entities in a GUI are drawn picture-in-picture, centred in the box; the old 8 % lower centre is in the translation)
        net.minecraft.client.renderer.entity.state.EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(e, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        g.entity(state, schaal, new Vector3f(0, (guh ? 0.42f : e.getBbHeight() * 0.5f) * groot + (y2 - y1) * 0.08f / schaal, 0), pose,
                camera, x1, y1, x2, y2);
        g.disableScissor();
    }
}
