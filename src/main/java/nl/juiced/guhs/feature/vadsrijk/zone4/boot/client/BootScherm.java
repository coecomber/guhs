package nl.juiced.guhs.feature.vadsrijk.zone4.boot.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.economie.client.RijdierClient;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootEntity;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootPayloads;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootSpul;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootVariant;

/**
 * T-Z4BOOT: the boat screen (tab "Zeilbootje"): a turning preview in the looks you pick, its level / xp / speed and last dock,
 * and the three rows of looks (sail colour, pennant, figurehead): click one to try it on the preview, then "Kiezen" (owned) or
 * "Kopen" (for sale); rewards show where you get them.
 */
public class BootScherm extends EcoScherm {
    private static final int VAK = 20;
    @Nullable
    private BootEntity voorbeeld;
    /** What the preview shows per axis (index), -1 = the chosen one. */
    private final int[] pas = {-1, -1, -1};
    @Nullable
    private BootVariant geselecteerd;

    public BootScherm(CompoundTag data) {
        super("zeilbootje", Component.translatable("vadsrijk.z4boot.scherm.titel"), data, List.of(), 340, 250);
    }

    @Override
    protected String collectieTab() {
        return BootClient.TAB;
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        if (!actie.equals("dicht")) {
            ClientPacketDistributor.sendToServer(new BootPayloads.Doe(actie, args.getStringOr("id", "")));
        }
    }

    private CompoundTag rij(BootVariant v) {
        ListTag l = data.getListOrEmpty("varianten");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            if (t.getStringOr("id", "").equals(v.id())) {
                return t;
            }
        }
        return new CompoundTag();
    }

    private boolean heeft(BootVariant v) {
        return rij(v).getBooleanOr("heeft", false);
    }

    private boolean gekozen(BootVariant v) {
        return rij(v).getBooleanOr("gekozen", false);
    }

    private BootVariant toon(BootVariant.As as) {
        int i = pas[as.ordinal()];
        if (i >= 0) {
            return BootVariant.van(as, i);
        }
        for (BootVariant v : as.varianten()) {
            if (gekozen(v)) {
                return v;
            }
        }
        return as.standaard();
    }

    @Nullable
    private BootEntity voorbeeld() {
        Minecraft mc = Minecraft.getInstance();
        if (voorbeeld == null && mc.level != null && BootSpul.ZEILBOOTJE.get().create(mc.level, EntitySpawnReason.LOAD) instanceof BootEntity b) {
            b.voorbeeld = true;
            voorbeeld = b;
        }
        return voorbeeld;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        boolean boot = data.getBooleanOr("heeft", false);
        // ---- the preview ---------------------------------------------------------------------------------------------------------
        int px = left + 8, py = top + 24, pw = 150, ph = 104;
        g.fill(px, py, px + pw, py + ph, 0xFF8CC8F0);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFFD8F0FF);
        g.fill(px + 1, py + ph - 26, px + pw - 1, py + ph - 1, 0xFF9CD4F4);   // a bit of sea
        BootEntity e = voorbeeld();
        if (e != null) {
            for (BootVariant.As as : BootVariant.As.values()) {
                e.zetVariant(toon(as));
            }
            e.silhouet = !boot;
            teken(g, e, px + 1, py + 1, px + pw - 1, py + ph - 1);
        }
        int iy = py + ph + 5;
        g.text(font, Component.translatable("vadsrijk.economie.rijdier.zeilbootje").withStyle(ChatFormatting.BOLD), px, iy, boot ? TEKST : ZACHT, false);
        iy += 11;
        if (boot) {
            int level = data.getIntOr("level", 1);
            g.text(font, Component.translatable("vadsrijk.economie.stal.level", level, RijdierSoort.MAX_LEVEL), px, iy, GOUD, false);
            iy += 10;
            long xp = data.getLongOr("xp", 0), nodig = data.getLongOr("nodig", 0);
            g.fill(px, iy, px + pw, iy + 4, 0xFFE8C8DC);
            g.fill(px, iy, px + (int) (pw * (nodig <= 0 ? 1 : Mth.clamp(xp / (double) nodig, 0, 1))), iy + 4, nodig <= 0 ? 0xFFFFC040 : 0xFFFF7FC0);
            iy += 7;
            g.text(font, Component.translatable("vadsrijk.z4boot.scherm.snelheid", Math.round(data.getDoubleOr("snelheid", 0) / 0.1 * 100)), px, iy, ZACHT, false);
            iy += 10;
            g.text(font, Component.translatable("vadsrijk.z4boot.scherm.steiger", Component.translatable(data.getStringOr("steiger", ""))), px, iy, ZACHT, false);
        } else {
            for (var line : font.split(Component.translatable("vadsrijk.z4boot.scherm.nog_niet"), pw)) {
                g.text(font, line, px, iy, 0xFF9A3A7A, false);
                iy += 9;
            }
        }

        // ---- the three rows of looks ---------------------------------------------------------------------------------------------
        int rx = left + 168, ry = top + 24;
        for (BootVariant.As as : BootVariant.As.values()) {
            g.text(font, Component.translatable("vadsrijk.z4boot.as." + as.id()).withStyle(ChatFormatting.BOLD), rx, ry, TEKST, false);
            ry += 11;
            int x = rx;
            for (BootVariant v : as.varianten()) {
                boolean heeft = heeft(v), aan = gekozen(v), sel = v == geselecteerd, hover = binnen(mx, my, x, ry, VAK, VAK);
                g.fill(x, ry, x + VAK, ry + VAK, sel ? 0xFFFF2F8F : aan ? 0xFF2E9E5A : hover ? 0xFFFFB8DC : 0xFFF0B8D8);
                g.fill(x + 2, ry + 2, x + VAK - 2, ry + VAK - 2, v.kleur);
                if (!heeft) {
                    g.fill(x + 2, ry + 2, x + VAK - 2, ry + VAK - 2, 0x90E6DCE4);
                    g.text(font, "?", x + 7, ry + 6, 0xFF6A4A64, false);
                }
                if (aan) {
                    g.text(font, "★", x + 1, ry + 1, 0xFFFFFFFF, true);
                }
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(v.naamKey()).withStyle(ChatFormatting.BOLD));
                tip.add(Component.translatable(heeft ? "vadsrijk.economie.rijdieren.van_jou" : "vadsrijk.z4boot.bron." + v.bron, Spelers.munten(v.prijs))
                        .withStyle(heeft ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                tip(mx, my, x, ry, VAK, VAK, tip);
                klikvlak(x, ry, VAK, VAK, () -> {
                    geselecteerd = v;
                    pas[v.as.ordinal()] = v.index();
                });
                x += VAK + 3;
            }
            ry += VAK + 8;
        }

        // ---- the selected look -------------------------------------------------------------------------------------------------------
        int by = top + h - 34, bw = left + w - 8 - rx;
        if (geselecteerd != null && boot) {
            BootVariant v = geselecteerd;
            g.text(font, Component.translatable(v.naamKey()).withStyle(ChatFormatting.BOLD), rx, ry, TEKST, false);
            ry += 10;
            if (!heeft(v)) {
                for (var line : font.split(Component.translatable("vadsrijk.z4boot.bron." + v.bron, Spelers.munten(v.prijs)), bw)) {
                    g.text(font, line, rx, ry, 0xFF9A3A7A, false);
                    ry += 9;
                }
            }
            CompoundTag a = new CompoundTag();
            a.putString("id", v.id());
            if (heeft(v) && !gekozen(v)) {
                knop(g, mx, my, rx, by, bw, 18, Component.translatable("vadsrijk.z4boot.scherm.kies"), true, () -> doe("kies", a));
            } else if (heeft(v)) {
                g.centeredText(font, Component.translatable("vadsrijk.economie.stal.gekozen_label").withStyle(ChatFormatting.BOLD), rx + bw / 2, by + 5, GROEN);
            } else if (v.prijs > 0) {
                boolean kan = data.getLongOr("munten", 0) >= v.prijs;
                knop(g, mx, my, rx, by, bw, 18, Component.translatable("vadsrijk.economie.stal.koop", Spelers.munten(v.prijs)), kan, () -> doe("koop", a));
            }
        }
        // ---- how it works ------------------------------------------------------------------------------------------------------------
        String toets = RijdierClient.RIJDEN.getTranslatedKeyMessage().getString();
        int uy = top + h - 50;
        for (var line : font.split(Component.translatable("vadsrijk.z4boot.scherm.uitleg", toets), (int) (pw / 0.75f))) {
            g.pose().pushMatrix();
            g.pose().translate(px, uy);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, line, 0, 0, ZACHT, false);
            g.pose().popMatrix();
            uy += 7;
        }
    }

    private void teken(GuiGraphicsExtractor g, BootEntity e, int x1, int y1, int x2, int y2) {
        Minecraft mc = Minecraft.getInstance();
        g.enableScissor(x1, y1, x2, y2);
        float s = BootClient.VOORBEELD_SCHAAL, hoog = 4.0f * s, breed = 5.6f * s;
        float schaal = Math.min((y2 - y1) * 0.8f / hoog, (x2 - x1) * 0.85f / breed);
        float yaw = (System.currentTimeMillis() % 16000L) / 16000f * 360f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(16 * Mth.DEG_TO_RAD);
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
