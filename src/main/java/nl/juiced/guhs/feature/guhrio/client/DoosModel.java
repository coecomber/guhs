package nl.juiced.guhs.feature.guhrio.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;

/**
 * A model of plain boxes for the creatures of Super Guhrio, read from {@code assets/guhs/guhrio_model/<naam>.json} (made by
 * tools/features/guhrio_modellen.py, which also draws the texture {@code textures/entity/<naam>.png} and can render a
 * preview of exactly these boxes). The file: {@code {"textuur": [w, h], "delen": {"<deel>": [{"van": [x, y, z], "tot":
 * [x, y, z], "voor": [u, v, w, h], "zij": [...], "boven": [...], "achter": [...], "onder": [...]}]}}}: sizes in pixels
 * (16 = one block), the front of the creature is +z, every face gets a rectangle of the texture ({@code zij} is the default
 * for every face without one of its own; {@code links} / {@code rechts} are the creature's own left (+x) and right). A renderer draws part by part ({@link #dien}), each with its own place and turn.
 */
public final class DoosModel {
    private record Doos(float x0, float y0, float z0, float x1, float y1, float z1, float[] voor, float[] achter, float[] links, float[] rechts,
                        float[] boven, float[] onder) {
    }

    private static final Map<String, DoosModel> ALLE = new ConcurrentHashMap<>();

    public final Identifier textuur;
    private final float tw, th;
    private final Map<String, List<Doos>> delen = new HashMap<>();

    private DoosModel(String naam) {
        this.textuur = Guhs.id("textures/entity/" + naam + ".png");
        float w = 64, h = 64;
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(Guhs.id("guhrio_model/" + naam + ".json"));
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray maat = json.getAsJsonArray("textuur");
                    w = maat.get(0).getAsFloat();
                    h = maat.get(1).getAsFloat();
                    for (var deel : json.getAsJsonObject("delen").entrySet()) {
                        List<Doos> dozen = new ArrayList<>();
                        for (JsonElement e : deel.getValue().getAsJsonArray()) {
                            JsonObject d = e.getAsJsonObject();
                            float[] van = getallen(d.getAsJsonArray("van")), tot = getallen(d.getAsJsonArray("tot"));
                            float[] zij = getallen(d.getAsJsonArray("zij"));
                            dozen.add(new Doos(van[0], van[1], van[2], tot[0], tot[1], tot[2], of(d, "voor", zij), of(d, "achter", zij),
                                    of(d, "links", zij), of(d, "rechts", zij), of(d, "boven", zij), of(d, "onder", zij)));
                        }
                        delen.put(deel.getKey(), dozen);
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("guhs").error("Guhrio: model {} can't be read", naam, e);
        }
        this.tw = w;
        this.th = h;
    }

    private static float[] getallen(JsonArray a) {
        float[] uit = new float[a.size()];
        for (int i = 0; i < uit.length; i++) {
            uit[i] = a.get(i).getAsFloat();
        }
        return uit;
    }

    private static float[] of(JsonObject d, String naam, float[] anders) {
        return d.has(naam) ? getallen(d.getAsJsonArray(naam)) : anders;
    }

    /** The model with this name (read once). */
    public static DoosModel van(String naam) {
        return ALLE.computeIfAbsent(naam, DoosModel::new);
    }

    /** (the resources were loaded again) */
    static void vergeet() {
        ALLE.clear();
    }

    /** Draws one part with the pose as it is now (the caller scales by 1/16 and puts the part where it belongs). */
    public void dien(String deel, PoseStack pose, SubmitNodeCollector collector, int licht) {
        List<Doos> dozen = delen.get(deel);
        if (dozen == null || dozen.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(textuur), (last, vc) -> {
            for (Doos d : dozen) {
                doos(vc, last, licht, d);
            }
        });
    }

    private void doos(VertexConsumer vc, PoseStack.Pose pose, int licht, Doos d) {
        float x0 = d.x0, y0 = d.y0, z0 = d.z0, x1 = d.x1, y1 = d.y1, z1 = d.z1;
        vlak(vc, pose, licht, d.voor, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);      // front
        vlak(vc, pose, licht, d.achter, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);   // back
        vlak(vc, pose, licht, d.links, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);     // its left
        vlak(vc, pose, licht, d.rechts, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);   // its right
        vlak(vc, pose, licht, d.boven, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0);     // top
        vlak(vc, pose, licht, d.onder, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);    // bottom
    }

    /** One face: bottom left, bottom right, top right, top left (seen from outside). */
    private void vlak(VertexConsumer vc, PoseStack.Pose pose, int licht, float[] uv, float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz) {
        float u0 = uv[0] / tw, v0 = uv[1] / th, u1 = (uv[0] + uv[2]) / tw, v1 = (uv[1] + uv[3]) / th;
        punt(vc, pose, licht, ax, ay, az, u0, v1, nx, ny, nz);
        punt(vc, pose, licht, bx, by, bz, u1, v1, nx, ny, nz);
        punt(vc, pose, licht, cx, cy, cz, u1, v0, nx, ny, nz);
        punt(vc, pose, licht, dx, dy, dz, u0, v0, nx, ny, nz);
    }

    private static void punt(VertexConsumer vc, PoseStack.Pose pose, int licht, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(licht).setNormal(pose, nx, ny, nz);
    }
}
