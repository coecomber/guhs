package nl.juiced.guhs.feature.guhvillagers.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import nl.juiced.guhs.Guhs;

/**
 * The geometry of the new guh villager (render "guh_villager_voorstellen", mijnen-ontwerp/_bron/villagers/villager_ontwerp.py):
 * <ul>
 *   <li>{@link #volwassen}: model A, the Guhkop-villager: the vanilla villager body, robe, crossed arms and legs (same boxes
 *       and texture spots, so a profession texture drawn for the vanilla villager fits it), a round stepped guh head
 *       (three crossing boxes) with a short upright snout and round ears, and a little tail.</li>
 *   <li>{@link #kind}: model C, the Knuffel-villager: a small round guh in a little robe, arms crossed (drawn 0.68x).</li>
 *   <li>{@link #hoedjes}: what the four 1.7.0 professions wear on the guh head (Toverguh's pointed hat, the Mijnwerker's
 *       helmet with lamp, the Vadsmonteur's goggles, the Kristalslijper's headband with a crystal and loupe).</li>
 * </ul>
 * Textures: tools/features/guhvillagers_tex.py (4 texels per model pixel; the UV spots below are in model pixels).
 * Part names are the vanilla VillagerModel's (head, arms, right_leg, left_leg), so VillagerModel animates them.
 */
public final class GuhVillagerModellen {
    public static final ModelLayerLocation VOLWASSEN = new ModelLayerLocation(Guhs.id("guh_villager"), "main");
    public static final ModelLayerLocation KIND = new ModelLayerLocation(Guhs.id("guh_villager_kind"), "main");
    public static final ModelLayerLocation HOEDJES = new ModelLayerLocation(Guhs.id("guh_villager_hoedjes"), "main");
    public static final ModelLayerLocation IJZERGUHLEM = new ModelLayerLocation(Guhs.id("ijzerguhlem"), "main");

    private static final float GRAAD = (float) (Math.PI / 180.0);

    /** A round guh ear (two crossing plates) on the head, turned and tilted outwards. side: +1 = x+, -1 = x-. */
    private static void oor(PartDefinition head, String name, int side, float x, float y, float z, int u1, int v1, int u2, int v2,
                            float w, float h, float yaw, float tilt) {
        head.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(u1, v1).addBox(-w / 2, -h / 2, -0.5f, w, h, 1)
                        .texOffs(u2, v2).addBox(-h / 2, -w / 2, -0.6f, h, w, 1.2f),
                PartPose.offsetAndRotation(side * x, y, z, 0, -side * yaw * GRAAD, side * tilt * GRAAD));
    }

    /** Model A (texture 128x64 model pixels: the vanilla villager body spots, the head at x 64+). */
    public static LayerDefinition volwassen() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-6, -10, -4.5f, 12, 9, 8)       // wide
                .texOffs(64, 17).addBox(-5, -11, -4.5f, 10, 11, 8)     // tall
                .texOffs(64, 36).addBox(-5, -10, -5.5f, 10, 9, 10),    // deep (the face)
                PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(104, 0).addBox(-1, -5.5f, -7, 2, 2.5f, 1.5f), PartPose.ZERO);
        oor(head, "right_ear", -1, 6.6f, -8.4f, -1.5f, 104, 6, 104, 12, 7, 5, 22, 22);
        oor(head, "left_ear", 1, 6.6f, -8.4f, -1.5f, 104, 6, 104, 12, 7, 5, 22, 22);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 20).addBox(-4, 0, -3, 8, 12, 6), PartPose.ZERO);
        body.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4, 0, -3, 8, 20, 6, new CubeDeformation(0.5f)), PartPose.ZERO);
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(104, 20).addBox(-1, -1, 0, 2, 2, 4), PartPose.offsetAndRotation(0, 16.5f, 3.2f, 0.3f, 0, 0));
        root.addOrReplaceChild("arms", CubeListBuilder.create()
                        .texOffs(44, 22).addBox(-8, -2, -2, 4, 8, 4)
                        .texOffs(44, 22).addBox(4, -2, -2, 4, 8, 4, true)
                        .texOffs(40, 38).addBox(-4, 2, -2, 8, 4, 4),
                PartPose.offsetAndRotation(0, 3, -1, -0.75f, 0, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Model C, the child (texture 128x64 model pixels). Drawn at 0.68x (GuhVillagerRenderer#scale). */
    public static LayerDefinition kind() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7, -11, -5.2f, 14, 10, 9)
                .texOffs(0, 19).addBox(-6, -12, -5.2f, 12, 12, 9)
                .texOffs(0, 40).addBox(-6, -11, -6.2f, 12, 10, 11),
                PartPose.offset(0, 13.2f, 0));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(46, 0).addBox(-1.25f, -4.6f, -7.2f, 2.5f, 2, 1), PartPose.ZERO);
        oor(head, "right_ear", -1, 7.6f, -9.6f, -1.7f, 46, 3, 46, 10, 8, 6, 22, 24);
        oor(head, "left_ear", 1, 7.6f, -9.6f, -1.7f, 46, 3, 46, 10, 8, 6, 22, 24);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4, 12.5f, -3, 8, 9, 6, new CubeDeformation(0.3f))
                .texOffs(64, 15).addBox(-5, 19, -4, 10, 3, 8), PartPose.ZERO);
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(78, 34).addBox(-0.75f, 17, 3.2f, 1.5f, 1.5f, 3.5f), PartPose.ZERO);
        root.addOrReplaceChild("arms", CubeListBuilder.create()
                        .texOffs(64, 26).addBox(2.75f, -0.8f, -2, 2.5f, 5, 2.5f)
                        .texOffs(64, 26).mirror().addBox(-5.25f, -0.8f, -2, 2.5f, 5, 2.5f)
                        .texOffs(76, 26).mirror(false).addBox(-2.75f, 1.7f, -2, 5.5f, 2.5f, 2.5f),
                PartPose.offsetAndRotation(0, 13.6f, -1.2f, -0.7f, 0, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(64, 34).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offset(-2.1f, 21, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(64, 34).mirror().addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offset(2.1f, 21, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** The profession hats on the head of model A (texture 64x96 model pixels). */
    public static LayerDefinition hoedjes() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("toverhoed", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7, 0.5f, -6, 14, 1, 12)        // brim
                        .texOffs(0, 13).addBox(-4, -0.5f, -4, 8, 1, 8)        // golden band
                        .texOffs(32, 13).addBox(-4, -4.5f, -4, 8, 4, 8)
                        .texOffs(0, 25).addBox(-3, -7.5f, -2.5f, 6, 3, 6)
                        .texOffs(24, 25).addBox(-2, -10.5f, -1, 4, 3, 4)
                        .texOffs(40, 25).addBox(-1, -13.5f, 0.5f, 2, 3, 2)
                        .texOffs(48, 25).addBox(-1, -15.5f, 1, 2, 2, 2),    // star pompom
                PartPose.offsetAndRotation(0, -11, -0.5f, -0.17f, 0, 0));
        head.addOrReplaceChild("mijnhelm", CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-6.5f, -11.5f, -6, 13, 3.5f, 11)
                        .texOffs(0, 49).addBox(-4.5f, -12.7f, -4, 9, 1.2f, 7)
                        .texOffs(32, 49).addBox(-6.5f, -8.8f, -7.5f, 13, 0.8f, 1.5f)     // visor
                        .texOffs(32, 52).addBox(-1.75f, -11.2f, -7.2f, 3.5f, 2.8f, 1.2f), // lamp
                PartPose.ZERO);
        head.addOrReplaceChild("vadsbril", CubeListBuilder.create()
                        .texOffs(0, 58).addBox(-6, -10, -5.5f, 12, 1.5f, 10, new CubeDeformation(0.3f))
                        .texOffs(44, 58).addBox(-4.25f, -11.2f, -7.2f, 3.5f, 3.5f, 1.5f)
                        .texOffs(44, 58).addBox(0.75f, -11.2f, -7.2f, 3.5f, 3.5f, 1.5f),
                PartPose.ZERO);
        head.addOrReplaceChild("kristalband", CubeListBuilder.create()
                        .texOffs(0, 70).addBox(-6, -10, -5.5f, 12, 1.25f, 10, new CubeDeformation(0.3f))
                        .texOffs(44, 70).addBox(-0.75f, -12, -6.4f, 1.5f, 3, 1.5f)       // crystal
                        .texOffs(52, 70).addBox(1.25f, -11.5f, -7.4f, 2.5f, 2.5f, 1.5f),  // loupe
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 96);
    }

    /**
     * Model B of the IJzerguhlem render ("Pluizige IJzerguhlem"): the vanilla iron golem's body, arms and legs (same boxes
     * and texture spots, so IronGolemModel animates it and the crack textures fit), a round guh head with ears, snout and
     * a little flower crown, a fluffy collar over the shoulders, fluff at wrists and knees, and a bunch of pluisgras in
     * the left hand (texture 128x128 model pixels).
     */
    public static LayerDefinition ijzerguhlem() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 81).addBox(-6, -11, -6, 12, 9, 8)
                .texOffs(0, 98).addBox(-5, -12, -6, 10, 11, 8)
                .texOffs(82, 0).addBox(-5, -11, -7, 10, 9, 10),
                PartPose.offset(0, -7, -2));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(82, 19).addBox(-1, -6.5f, -8.5f, 2, 2.5f, 1.5f), PartPose.ZERO);
        oor(head, "right_ear", -1, 6.6f, -9.6f, -3, 90, 19, 106, 19, 7, 5, 22, 22);
        oor(head, "left_ear", 1, 6.6f, -9.6f, -3, 90, 19, 106, 19, 7, 5, 22, 22);
        float[] kroonX = {-3.5f, -1.2f, 1.2f, 3.5f};
        CubeListBuilder kroon = CubeListBuilder.create();
        for (int i = 0; i < 4; i++) {
            kroon.texOffs(i * 6, 117).addBox(kroonX[i] - 0.75f, -13, i % 2 == 0 ? -5.5f : -4.5f, 1.5f, 1, 1.5f);
        }
        head.addOrReplaceChild("kroon", kroon, PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-9, -2, -6, 18, 12, 11)
                        .texOffs(0, 70).addBox(-4.5f, 10, -3, 9, 5, 6, new CubeDeformation(0.5f)),
                PartPose.offset(0, -7, 0));
        CubeListBuilder kraag = CubeListBuilder.create();
        for (float x : new float[]{-9, -4.5f, 0, 4.5f}) {
            kraag.texOffs(82, 28).addBox(x, -3.5f, -7.5f, 4.5f, 3, 3);
            kraag.texOffs(82, 28).addBox(x, -3.5f, 2.5f, 4.5f, 3, 3);
        }
        for (float x : new float[]{-10.5f, 7.5f}) {
            kraag.texOffs(82, 34).addBox(x, -3.5f, -4.5f, 3, 3, 4.5f);
            kraag.texOffs(82, 34).addBox(x, -3.5f, 0, 3, 3, 4.5f);
        }
        kraag.texOffs(82, 42).addBox(-13.5f, -4, -3.5f, 5, 3, 7);
        kraag.texOffs(82, 42).addBox(8.5f, -4, -3.5f, 5, 3, 7);
        body.addOrReplaceChild("kraag", kraag, PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(60, 21).addBox(-13, -2.5f, -3, 4, 30, 6)
                        .texOffs(82, 52).addBox(-13.5f, 19, -3.5f, 5, 2, 7),
                PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(60, 58).addBox(9, -2.5f, -3, 4, 30, 6)
                        .texOffs(82, 52).addBox(8.5f, 19, -3.5f, 5, 2, 7)
                        // the bunch of pluisgras in the hand: stems and fluffy heads
                        .texOffs(110, 28).addBox(10.5f, 17, -4.5f, 1, 9, 1)
                        .texOffs(110, 28).addBox(9.3f, 18.5f, -4.2f, 1, 7.5f, 1)
                        .texOffs(110, 28).addBox(11.7f, 18, -4.3f, 1, 8, 1)
                        .texOffs(110, 40).addBox(9.5f, 14, -6, 3, 3, 3)
                        .texOffs(110, 40).addBox(8.2f, 16, -5.4f, 2.5f, 2.5f, 2.5f)
                        .texOffs(110, 40).addBox(11.4f, 15.5f, -5.6f, 2.5f, 2.5f, 2.5f),
                PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(37, 0).addBox(-3.5f, -3, -3, 6, 16, 5)
                        .texOffs(82, 61).addBox(-4, 4, -3.5f, 7, 2, 6),
                PartPose.offset(-4, 11, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(60, 0).mirror().addBox(-3.5f, -3, -3, 6, 16, 5)
                        .texOffs(82, 61).addBox(-4, 4, -3.5f, 7, 2, 6),
                PartPose.offset(5, 11, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private GuhVillagerModellen() {
    }
}
