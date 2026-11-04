package nl.juiced.guhs.feature.vadsrijk.huisdieren.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierPayloads;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;

/**
 * G-PETS: the pet screen (same pink style as the mount screen). Top right "Verzameld x/y"; left every pet in a grid (yours in
 * colour, the rest as dark silhouettes with a "?"; a star on the one walking with you); hover = name, flavour text and how to get it;
 * click = select. Right: a turning preview of the selected pet, its text, how to get it, and "Meenemen" / "Thuislaten".
 */
public class HuisdierScherm extends EcoScherm {
    private static final int KOLOMMEN = 4, VAK = 36;
    private final Map<String, HuisdierEntity> voorbeelden = new HashMap<>();
    @Nullable
    private String gekozen;

    public HuisdierScherm(CompoundTag data) {
        super("huisdieren", Component.translatable("vadsrijk.huisdieren.titel"), data, List.of(), 340, 224);
    }

    /** (fix round 3) The collection tabs (Rijdieren | Huisdieren | ...). */
    @Override
    protected String collectieTab() {
        return HuisdierClient.TAB;
    }

    private List<String> bezit() {
        List<String> l = new ArrayList<>();
        for (String s : data.getStringOr("bezit", "").split(",")) {
            if (!s.isEmpty()) {
                l.add(s);
            }
        }
        return l;
    }

    private String actief() {
        return data.getStringOr("actief", "");
    }

    @Override
    protected void init() {
        super.init();
        if (gekozen == null) {
            List<Huisdier> alle = List.copyOf(Huisdieren.alle());
            gekozen = !actief().isEmpty() ? actief() : !bezit().isEmpty() ? bezit().get(0) : alle.isEmpty() ? null : alle.get(0).id();
        }
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        if (!actie.equals("dicht")) {
            ClientPacketDistributor.sendToServer(new HuisdierPayloads.Doe(actie, args.getStringOr("id", "")));
        }
    }

    @Nullable
    private HuisdierEntity voorbeeld(Huisdier h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return voorbeelden.computeIfAbsent(h.id(), x -> {
            HuisdierEntity e = HuisdierenRegistry.HUISDIER.get().create(mc.level, EntitySpawnReason.LOAD);
            if (e != null) {
                e.voorbeeld = true;
                e.zetSoort(h.id());
            }
            return e;
        });
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        List<Huisdier> alle = List.copyOf(Huisdieren.alle());
        List<String> bezit = bezit();
        // ---- the counter in the title band -------------------------------------------------------------------------------------
        Component teller = Component.translatable("vadsrijk.huisdieren.verzameld", bezit.size(), alle.size());
        g.text(font, teller.copy().withStyle(ChatFormatting.BOLD), left + w - 8 - font.width(teller.copy().withStyle(ChatFormatting.BOLD)), top + 5, WIT, true);

        // ---- the grid ------------------------------------------------------------------------------------------------------------
        int gx = left + 8, gy = top + 24;
        for (int i = 0; i < alle.size(); i++) {
            Huisdier h = alle.get(i);
            int x = gx + (i % KOLOMMEN) * (VAK + 3), y = gy + (i / KOLOMMEN) * (VAK + 3);
            boolean heeft = bezit.contains(h.id()), sel = h.id().equals(gekozen), hover = binnen(mx, my, x, y, VAK, VAK);
            g.fill(x, y, x + VAK, y + VAK, sel ? 0xFFFF2F8F : hover ? 0xFFFFB8DC : heeft ? 0xFFF0B8D8 : 0xFFC8B8C4);
            g.fill(x + 1, y + 1, x + VAK - 1, y + VAK - 1, heeft ? (sel ? 0xFFFFE0F0 : 0xFFFFF6FB) : 0xFFE2DAE2);
            HuisdierEntity e = voorbeeld(h);
            if (e != null) {
                e.silhouet = !heeft;
                teken(g, e, x + 1, y + 1, x + VAK - 1, y + VAK - 1, 0.8f);
            }
            if (!heeft) {
                g.text(font, "?", x + VAK - 8, y + 3, 0xFF6A4A64, false);
            }
            if (h.id().equals(actief())) {
                g.text(font, "★", x + 3, y + 2, 0xFFFF5FAF, false);
            }
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(h.naamKey()).withStyle(heeft ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY).withStyle(ChatFormatting.BOLD));
            for (var line : font.getSplitter().splitLines(Component.translatable(h.tekstKey()), 200, net.minecraft.network.chat.Style.EMPTY)) {
                tip.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            if (h.id().equals(actief())) {
                tip.add(Component.translatable("vadsrijk.huisdieren.loopt_mee").withStyle(ChatFormatting.GREEN));
            }
            tip.add(Component.translatable("vadsrijk.huisdieren.hoe").withStyle(ChatFormatting.GOLD));
            for (var line : font.getSplitter().splitLines(Component.translatable(h.bronKey()), 200, net.minecraft.network.chat.Style.EMPTY)) {
                tip.add(Component.literal(line.getString()).withStyle(heeft ? ChatFormatting.GRAY : ChatFormatting.YELLOW));
            }
            tip(mx, my, x, y, VAK, VAK, tip);
            klikvlak(x, y, VAK, VAK, () -> gekozen = h.id());
        }
        // the explanation under the grid
        int uy = top + h - 40;
        for (var line : font.split(Component.translatable("vadsrijk.huisdieren.uitleg"), (int) ((KOLOMMEN * (VAK + 3)) / 0.75f))) {
            g.pose().pushMatrix();
            g.pose().translate(gx, uy);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, line, 0, 0, ZACHT, false);
            g.pose().popMatrix();
            uy += 7;
        }

        // ---- the selected pet ------------------------------------------------------------------------------------------------------
        Huisdier h = Huisdieren.van(gekozen);
        if (h == null) {
            return;
        }
        boolean heeft = bezit.contains(h.id()), loopt = h.id().equals(actief());
        int px = gx + KOLOMMEN * (VAK + 3) + 6, pw = left + w - 8 - px, py = top + 24, ph = 62;
        g.fill(px, py, px + pw, py + ph, 0xFFF0B8D8);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, heeft ? 0xFFFFF8FC : 0xFFEDE6EE);
        g.fill(px + 1, py + ph - 16, px + pw - 1, py + ph - 1, heeft ? 0xFFFFE6F3 : 0xFFE2DAE4);   // a little floor
        HuisdierEntity e = voorbeeld(h);
        if (e != null) {
            e.silhouet = !heeft;
            teken(g, e, px + 1, py + 1, px + pw - 1, py + ph - 1, 1f);
        }
        if (!heeft) {
            g.centeredText(font, Component.literal("?").withStyle(ChatFormatting.BOLD), px + pw / 2, py + 6, 0xFF8A5A84);
        }
        int iy = py + ph + 5;
        g.text(font, Component.translatable(h.naamKey()).withStyle(ChatFormatting.BOLD), px, iy, heeft ? TEKST : ZACHT, false);
        iy += 10;
        Component staat = Component.translatable(loopt ? "vadsrijk.huisdieren.loopt_mee" : heeft ? "vadsrijk.huisdieren.titel" : "vadsrijk.huisdieren.nog_niet");
        if (loopt || !heeft) {
            g.text(font, staat, px, iy, loopt ? GROEN : ROOD, false);
            iy += 10;
        }
        iy += 1;
        for (var line : font.split(Component.translatable(h.tekstKey()).withStyle(ChatFormatting.ITALIC), pw)) {
            if (iy > top + h() - 64) {
                break;
            }
            g.text(font, line, px, iy, ZACHT, false);
            iy += 9;
        }
        iy += 3;
        g.text(font, Component.translatable("vadsrijk.huisdieren.hoe").withStyle(ChatFormatting.BOLD), px, iy, GOUD, false);
        iy += 10;
        int stop = top + h() - 34;   // (never into the button)
        for (var line : font.split(Component.translatable(h.bronKey()), pw)) {
            if (iy > stop) {
                break;
            }
            g.text(font, line, px, iy, heeft ? ZACHT : 0xFF9A3A7A, false);
            iy += 9;
        }

        // ---- buttons -----------------------------------------------------------------------------------------------------------------
        int by = top + h() - 24;
        if (heeft && !loopt) {
            knop(g, mx, my, px, by, pw, 18, Component.translatable("vadsrijk.huisdieren.meenemen"), true, () -> {
                CompoundTag a = new CompoundTag();
                a.putString("id", h.id());
                doe("kies", a);
            });
        } else if (loopt) {
            knop(g, mx, my, px, by, pw, 18, Component.translatable("vadsrijk.huisdieren.thuislaten"), true, () -> doe("geen"));
        }
        if (actief().isEmpty()) {
            for (var line : font.split(Component.translatable("vadsrijk.huisdieren.geen_actief"), (int) (pw / 0.75f))) {
                g.pose().pushMatrix();
                g.pose().translate(px, by - 9);
                g.pose().scale(0.75f, 0.75f);
                g.text(font, line, 0, 0, ZACHT, false);
                g.pose().popMatrix();
                break;
            }
        }
    }

    private int h() {
        return h;
    }

    /** Draws the pet turning slowly, sized to fit the box. */
    private void teken(GuiGraphicsExtractor g, HuisdierEntity e, int x1, int y1, int x2, int y2, float zoom) {
        Minecraft mc = Minecraft.getInstance();
        g.enableScissor(x1, y1, x2, y2);
        float hoog = 0.6f, breed = 0.7f;
        float schaal = Math.min((y2 - y1) * 0.72f / hoog, (x2 - x1) * 0.8f / breed) * zoom;
        float yaw = (System.currentTimeMillis() % 10000L) / 10000f * 360f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(12 * Mth.DEG_TO_RAD);
        pose.mul(camera);
        e.yBodyRot = 180 + yaw;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(180 + yaw);
        e.yRotO = e.getYRot();
        e.setXRot(0);
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        var state = mc.getEntityRenderDispatcher().extractEntity(e, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        g.entity(state, schaal, new Vector3f(0, hoog * 0.5f + 2f / schaal, 0), pose, camera, x1, y1, x2, y2);
        g.disableScissor();
    }
}
