package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;

/**
 * G-MOUNTS: the zone-3 starter choice ({@code Rijdieren.kiesStarter}): three Pluimguhs side by side (pink, mint, lavender), each
 * turning on its little stage with a short description; "Deze wil ik!" picks one for free. The other two are for sale at the stal.
 */
public class StarterScherm extends EcoScherm {
    private final List<PluimKleur> starters = new ArrayList<>();
    private final List<RijdierEntity> voorbeelden = new ArrayList<>();

    public StarterScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.STARTER, Component.translatable("vadsrijk.economie.starter.titel"), data, items, 360, 232);
        ListTag l = data.getListOrEmpty("starters");
        for (int i = 0; i < l.size(); i++) {
            starters.add(PluimKleur.van(l.getStringOr(i, "roze")));
        }
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (voorbeelden.isEmpty() && mc.level != null) {
            for (PluimKleur k : starters) {
                RijdierEntity e = EcoSpul.type(RijdierSoort.PLUIMGUH).create(mc.level, EntitySpawnReason.LOAD);
                if (e != null) {
                    e.voorbeeld = true;
                    e.zetKleur(k);
                }
                voorbeelden.add(e);
            }
        }
        g.centeredText(font, Component.translatable("vadsrijk.economie.starter.vraag"), left + w / 2, top + 23, TEKST);
        g.centeredText(font, Component.translatable("vadsrijk.economie.starter.grapje").withStyle(net.minecraft.ChatFormatting.ITALIC), left + w / 2, top + 35, ZACHT);
        int n = Math.max(1, starters.size()), kw = (w - 16 - (n - 1) * 8) / n;
        for (int i = 0; i < starters.size(); i++) {
            PluimKleur k = starters.get(i);
            int x = left + 8 + i * (kw + 8), y = top + 48;
            boolean hover = binnen(mx, my, x, y, kw, 168);
            g.fill(x, y, x + kw, y + 168, hover ? 0xFFFF5FAF : 0xFFF0B8D8);
            g.fill(x + 1, y + 1, x + kw - 1, y + 167, 0xFFFFF8FC);
            g.fill(x + 1, y + 1, x + kw - 1, y + 6, k.kleur);
            RijdierEntity e = i < voorbeelden.size() ? voorbeelden.get(i) : null;
            if (e != null) {
                teken(g, e, x + 2, y + 6, x + kw - 2, y + 96, i);
            }
            g.centeredText(font, Component.translatable(k.naamKey()).withStyle(ChatFormatting.BOLD), x + kw / 2, y + 100, TEKST);
            int ty = y + 112;
            for (var line : font.split(Component.translatable("vadsrijk.economie.starter.over." + k.id()), kw - 10)) {
                g.text(font, line, x + 5, ty, ZACHT, false);
                ty += 9;
            }
            knop(g, mx, my, x + 6, y + 144, kw - 12, 18, Component.translatable("vadsrijk.economie.starter.kies"), true, () -> {
                CompoundTag a = new CompoundTag();
                a.putString("kleur", k.id());
                doe("kies", a);
            });
        }
    }

    private void teken(GuiGraphicsExtractor g, RijdierEntity e, int x1, int y1, int x2, int y2, int i) {
        Minecraft mc = Minecraft.getInstance();
        g.enableScissor(x1, y1, x2, y2);
        float hoog = Math.max(1.2f, e.getBbHeight() * 1.2f);
        float schaal = Math.min((y2 - y1) * 0.66f / hoog, (x2 - x1) * 0.7f / 2.0f);
        float yaw = ((System.currentTimeMillis() + i * 1500L) % 10000L) / 10000f * 360f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(12 * Mth.DEG_TO_RAD);
        pose.mul(camera);
        e.yBodyRot = 180 + yaw;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(180 + yaw);
        e.yRotO = e.getYRot();
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        var state = mc.getEntityRenderDispatcher().extractEntity(e, 1.0F);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        g.entity(state, schaal, new Vector3f(0, hoog * 0.5f + 4f / schaal, 0), pose, camera, x1, y1, x2, y2);
        g.disableScissor();
    }
}
