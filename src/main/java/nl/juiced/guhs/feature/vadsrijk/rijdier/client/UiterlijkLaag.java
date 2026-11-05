package nl.juiced.guhs.feature.vadsrijk.rijdier.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.rijdier.RijdierExtraRegistry;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;

/**
 * T-RIJDIER, client: the look of a mount on top of its normal render (added to economie's RijdierRenderer):
 * <ul>
 *   <li>the coat ({@link #textuur}): the species texture per vacht ("naturel" = the normal one);</li>
 *   <li>the pattern: the model once more with {@code vr_rijpatroon_<soort>_<patroon>.png} (only the fur pixels are drawn), tinted from
 *       the coat colour; glitter is drawn full bright;</li>
 *   <li>the tack: the tack item's 3D model on the head ({@link Plek#KOP}), in front of the neck ({@link Plek#NEK}) or on the back
 *       ({@link Plek#RUG}); lanterns are full bright.</li>
 * </ul>
 * A silhouette preview (a mount you don't have) shows none of it. One extra model pass + at most one small item model: cheap.
 * <p>A new species: add its anchors in {@link #ANKERS} (bone + offset from the bone pivot in model pixels + a scale), its coat
 * textures and pattern textures (tools/features/vadsrijk_t_rijdier.py).
 */
public final class UiterlijkLaag extends GeoRenderLayer<RijdierEntity, Void, LivingEntityRenderState> {
    public enum Plek {
        KOP, NEK, RUG
    }

    /** Where tack goes on a species: bone, offset from its pivot (Bedrock model pixels; x is mirrored in-game, all 0 here), scale. */
    public record Anker(String bot, float x, float y, float z, float schaal) {
    }

    public static final Map<RijdierSoort, Map<Plek, Anker>> ANKERS = new EnumMap<>(RijdierSoort.class);

    static {
        ankers(RijdierSoort.PLUIMGUH, new Anker("hoofd", 0, 11.6f, -2.5f, 1.3f), new Anker("nek", 0, 3f, -7.2f, 1.2f), new Anker("zadel", 0, 4.5f, 0.5f, 1.35f));
        ankers(RijdierSoort.KNUFFELHERT, new Anker("hoofd", 0, 8.1f, -2f, 1.05f), new Anker("nek", 0, 3.5f, -6.2f, 1.0f), new Anker("zadel", 0, 4.5f, 0f, 1.2f));
        ankers(RijdierSoort.GUHWOLF, new Anker("hoofd", 0, 11.4f, -5f, 1.2f), new Anker("body", 0, 5.5f, -12.6f, 1.1f), new Anker("zadel", 0, 4.5f, 0.25f, 1.25f));
        ankers(RijdierSoort.WOLKENSCHAAPJE, new Anker("head", 0, 5.9f, -2.6f, 0.75f), new Anker("head", 0, -3.6f, -4.2f, 0.7f), new Anker("body", 0, 9f, 0.5f, 0.95f));
        ankers(RijdierSoort.SLAKKIE, new Anker("kop", 0, 9.4f, -3f, 0.85f), new Anker("kop", 0, 0.5f, -7.2f, 0.8f), new Anker("zitje", 0, 1f, 0f, 0.95f));
        // (T-RIJDIER's own models: see tools/features/vadsrijk_t_rijdier.py, ANKERS there)
        ankers(RijdierSoort.DUINHUPPER, new Anker("hoofd", 0, 6.0f, -2.5f, 0.8f), new Anker("hoofd", 0, -1.5f, -6.3f, 0.75f), new Anker("zadel", 0, 1f, 0.5f, 0.95f));
        ankers(RijdierSoort.WOLKWALKALFJE, new Anker("body", 0, 6.0f, -7f, 0.95f), new Anker("body", 0, -3f, -11.4f, 0.9f), new Anker("zadel", 0, 1f, 0.5f, 1.1f));
        ankers(RijdierSoort.ZWAMZWIJNTJE, new Anker("hoofd", 0, 4.0f, -2.5f, 0.8f), new Anker("hoofd", 0, -3.5f, -6.2f, 0.7f), new Anker("zadel", 0, 3f, 0f, 1.1f));
    }

    private static void ankers(RijdierSoort s, Anker kop, Anker nek, Anker rug) {
        Map<Plek, Anker> m = new EnumMap<>(Plek.class);
        m.put(Plek.KOP, kop);
        m.put(Plek.NEK, nek);
        m.put(Plek.RUG, rug);
        ANKERS.put(s, m);
    }

    /** Where each tack piece goes and whether it glows. */
    static Plek plek(String tuig) {
        return switch (tuig) {
            case "bloemenkrans", "hoedje", "kroontje" -> Plek.KOP;
            case "belletjes" -> Plek.NEK;
            default -> Plek.RUG;
        };
    }

    static boolean gloeit(String tuig) {
        return tuig.equals("lampionnetjes");
    }

    private record Laag(String soort, String vacht, String patroon, String tuig, boolean donker, @Nullable ItemStackRenderState item) {
    }

    private static final DataTicket<Laag> LAAG = DataTicket.create("guhs_vr_rijdier_uiterlijk", Laag.class);
    private final RijdierSoort soort;
    private final Map<String, ItemStack> stapels = new java.util.HashMap<>();

    public UiterlijkLaag(GeoRenderer<RijdierEntity, Void, LivingEntityRenderState> renderer, RijdierSoort soort) {
        super(renderer);
        this.soort = soort;
    }

    /** The coat texture of a species (the Pluimguh is not handled here: it keeps its colour textures). */
    public static Identifier textuur(RijdierSoort s, String look) {
        String vacht = Uiterlijk.delen(look)[0];
        if (vacht.isEmpty() || vacht.equals(Uiterlijk.NATUREL)) {
            return Guhs.id("textures/entity/vr_eco_" + s.id() + ".png");
        }
        return Guhs.id("textures/entity/vr_eco_" + s.id() + "_" + vacht + ".png");
    }

    @Override
    public void addRenderData(RijdierEntity r, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        String[] d = Uiterlijk.delen(r.uiterlijk());
        ItemStackRenderState item = null;
        if (!d[2].isEmpty() && !r.silhouet && RijdierExtraRegistry.TUIG.containsKey(d[2])) {
            ItemStack st = stapels.computeIfAbsent(d[2], k -> new ItemStack(RijdierExtraRegistry.TUIG.get(k).get()));
            item = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(item, st, ItemDisplayContext.FIXED, r.level(), r, r.getId());
        }
        String vacht = d[0];
        if (soort.kleuren()) {
            vacht = "";   // (the Pluimguh's colour is its PluimKleur; the pattern tint uses the swatch of that)
        }
        state.addGeckolibData(LAAG, new Laag(soort.id(), soort.kleuren() ? r.kleur().id() : vacht, d[1], d[2], r.silhouet, item));
    }

    private int tint(Laag l) {
        int basis = soort.kleuren() ? RijdierSoort.PluimKleur.van(l.vacht()).kleur : Uiterlijk.vachtKleur(soort, l.vacht());
        return switch (l.patroon()) {
            case "vlekjes" -> meng(basis, 0xFF4A2C40, 0.42f);
            case "streepjes" -> meng(basis, 0xFFFFFFFF, 0.6f);
            case "sterretjes" -> 0xFFFFE070;
            case "hartjes" -> 0xFFFF6FA8;
            default -> 0xFFFFFFFF;
        };
    }

    private static int meng(int a, int b, float f) {
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    @Override
    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
        Laag l = info.getGeckolibData(LAAG);
        if (l == null || l.donker() || l.patroon().isEmpty() || !info.willRender()) {
            return;
        }
        Identifier tex = Guhs.id("textures/entity/vr_rijpatroon_" + l.soort() + "_" + l.patroon() + ".png");
        if (l.patroon().equals("glitter")) {
            getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(tex));
        } else {
            GuhRenderer.submitPass(info, collector.order(1), RenderTypes.entityCutout(tex), info.packedLight(), tint(l), b -> true);
        }
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        Laag l = info.getGeckolibData(LAAG);
        if (l == null || l.donker() || l.item() == null || l.item().isEmpty() || !info.willRender()) {
            return;
        }
        Map<Plek, Anker> m = ANKERS.get(soort);
        Anker a = m == null ? null : m.get(plek(l.tuig()));
        if (a == null) {
            return;
        }
        ItemStackRenderState item = l.item();
        boolean licht = gloeit(l.tuig());
        info.model().getBone(a.bot()).ifPresent(bot -> consumer.accept(bot, (pass, bone, c) -> {
            PoseStack pose = pass.poseStack();
            pose.translate(-a.x() / 16f, a.y() / 16f, a.z() / 16f);
            pose.scale(a.schaal(), a.schaal(), a.schaal());
            item.submit(pose, c, licht ? LightCoordsUtil.FULL_BRIGHT : pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor);
        }));
    }

    /** (tests) every tack id with an anchor on every species that wears tack. */
    public static List<String> ontbrekend() {
        List<String> l = new ArrayList<>();
        for (RijdierSoort s : RijdierSoort.values()) {
            if (!Uiterlijk.van(s, Uiterlijk.As.TUIG).isEmpty() && !ANKERS.containsKey(s)) {
                l.add(s.id());
            }
        }
        return l;
    }
}
