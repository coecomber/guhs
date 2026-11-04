package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Stal;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * G-MOUNTS: the mount screen ("Rijdieren"; at the stalmeester "Stal" with buy buttons). Left: every mount (yours in colour, the
 * others greyed with a "?"); right: a turning preview of the selected mount (a dark silhouette when it isn't yours), the Pluimguh's
 * colour swatches, its level/xp/speed/jump or where to get it, and the buttons (ride this one / buy).
 */
public class RijdierScherm extends EcoScherm {
    private static final int RIJ = 24;
    private final Map<RijdierSoort, RijdierEntity> voorbeelden = new EnumMap<>(RijdierSoort.class);
    private int gekozen = -1;
    @Nullable
    private PluimKleur kleurKeuze;

    public RijdierScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.STAL, Component.translatable(data.getBooleanOr("stal", false) ? "vadsrijk.economie.stal.titel" : "vadsrijk.economie.rijdieren.titel"),
                data, items, 360, 240);
    }

    /** (fix round 3) The collection tabs (Rijdieren | Huisdieren | ...) replace the old "Huisdieren" button; not at the stal. */
    @Override
    protected String collectieTab() {
        return data.getBooleanOr("stal", false) ? null : RijdierClient.TAB;
    }

    private ListTag lijst() {
        return data.getListOrEmpty("rijdieren");
    }

    private CompoundTag rij(int i) {
        return lijst().getCompoundOrEmpty(i);
    }

    @Override
    protected void init() {
        super.init();
        if (gekozen < 0) {
            gekozen = 0;
            for (int i = 0; i < lijst().size(); i++) {
                if (rij(i).getBooleanOr("actief", false)) {
                    gekozen = i;
                }
            }
        }
    }

    @Nullable
    private RijdierEntity voorbeeld(RijdierSoort s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return voorbeelden.computeIfAbsent(s, x -> {
            RijdierEntity e = EcoSpul.type(x).create(mc.level, EntitySpawnReason.LOAD);
            if (e != null) {
                e.voorbeeld = true;
            }
            return e;
        });
    }

    private PluimKleur toonKleur(CompoundTag t) {
        return kleurKeuze != null ? kleurKeuze : PluimKleur.van(t.getStringOr("kleur", "roze"));
    }

    private static boolean heeftKleur(CompoundTag t, PluimKleur k) {
        ListTag kl = t.getListOrEmpty("kleuren");
        for (int i = 0; i < kl.size(); i++) {
            CompoundTag c = kl.getCompoundOrEmpty(i);
            if (c.getStringOr("id", "").equals(k.id())) {
                return c.getBooleanOr("heeft", false);
            }
        }
        return false;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        ListTag l = lijst();
        // ---- the list ------------------------------------------------------------------------------------------------------
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            RijdierSoort s = RijdierSoort.van(t.getStringOr("soort", ""));
            if (s == null) {
                continue;
            }
            boolean heeft = t.getBooleanOr("heeft", false), sel = i == gekozen;
            int x = left + 8, y = top + 22 + i * (RIJ + 1), bw = 122;
            boolean hover = binnen(mx, my, x, y, bw, RIJ);
            g.fill(x, y, x + bw, y + RIJ, sel ? 0xFFFF5FAF : hover ? 0xFFFFB8DC : 0xFFF0B8D8);
            g.fill(x + 1, y + 1, x + bw - 1, y + RIJ - 1, heeft ? (sel ? 0xFFFFE0F0 : 0xFFFFF4FA) : 0xFFE6DCE4);
            ItemStack icoon = EcoSpul.icoon(s, s.kleuren() ? PluimKleur.van(t.getStringOr("kleur", "roze")) : PluimKleur.ROZE);
            g.item(icoon, x + 5, y + 4);
            if (!heeft) {
                g.fill(x + 4, y + 3, x + 22, y + 21, 0xB0E6DCE4);
                g.text(font, "?", x + 11, y + 8, 0xFF8A5A84, false);
            }
            g.text(font, Component.translatable(s.naamKey()).withStyle(ChatFormatting.BOLD), x + 26, y + 3, heeft ? TEKST : ZACHT, false);
            Component sub = heeft ? Component.translatable("vadsrijk.economie.stal.level", t.getIntOr("level", 1), RijdierSoort.MAX_LEVEL)
                    : Component.translatable("vadsrijk.economie.rijdieren.nog_niet");
            g.text(font, sub, x + 26, y + 13, heeft ? GOUD : ZACHT, false);
            if (t.getBooleanOr("actief", false)) {
                g.text(font, "★", x + bw - 12, y + 4, 0xFFFF5FAF, false);
            }
            final int idx = i;
            klikvlak(x, y, bw, RIJ, () -> {
                gekozen = idx;
                kleurKeuze = null;
            });
        }
        if (gekozen < 0 || gekozen >= l.size()) {
            return;
        }
        CompoundTag t = rij(gekozen);
        RijdierSoort s = RijdierSoort.van(t.getStringOr("soort", ""));
        if (s == null) {
            return;
        }
        boolean heeft = t.getBooleanOr("heeft", false);
        PluimKleur kleur = toonKleur(t);
        boolean eigen = heeft && (!s.kleuren() || heeftKleur(t, kleur));
        boolean stal = data.getBooleanOr("stal", false);

        // ---- the preview ---------------------------------------------------------------------------------------------------
        int px = left + 138, py = top + 24, pw = 112, ph = 120;
        g.fill(px, py, px + pw, py + ph, 0xFFF0B8D8);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, eigen ? 0xFFFFF8FC : 0xFFEDE6EE);
        g.fill(px + 1, py + ph - 22, px + pw - 1, py + ph - 1, eigen ? 0xFFFFE6F3 : 0xFFE2DAE4);   // a little floor
        RijdierEntity e = voorbeeld(s);
        if (e != null) {
            e.silhouet = !eigen;
            e.zetKleur(kleur);
            teken(g, e, px + 1, py + 1, px + pw - 1, py + ph - 1);
        }
        if (!eigen) {
            g.centeredText(font, Component.literal("?").withStyle(ChatFormatting.BOLD), px + pw / 2, py + 8, 0xFF8A5A84);
        }

        // ---- the colour swatches (Pluimguh) ----------------------------------------------------------------------------------
        int sy = py + ph + 6;
        if (s.kleuren()) {
            int sx = px;
            for (PluimKleur k : PluimKleur.values()) {
                boolean hk = heeftKleur(t, k);
                if (k.verborgen() && !hk) {
                    continue;
                }
                boolean sel = k == kleur;
                g.fill(sx, sy, sx + 14, sy + 14, sel ? 0xFFFF2F8F : 0xFFB890AC);
                g.fill(sx + 1, sy + 1, sx + 13, sy + 13, hk ? k.kleur : (k.kleur & 0x00FFFFFF) | 0x50000000);
                if (!hk) {
                    g.text(font, "?", sx + 5, sy + 3, 0xFF6A4A64, false);
                }
                tip(mx, my, sx, sy, 14, 14, List.of(Component.translatable(k.naamKey()).withStyle(ChatFormatting.BOLD),
                        Component.translatable(hk ? "vadsrijk.economie.rijdieren.van_jou" : "vadsrijk.economie.pluimkleur.bron." + k.id())
                                .withStyle(ChatFormatting.GRAY)));
                klikvlak(sx, sy, 14, 14, () -> kleurKeuze = k);
                sx += 16;
            }
        }

        if (stal && s.kleuren()) {   // (the stalmeester's little fourth-wall joke)
            int gy = sy + 18;
            for (var line : font.split(Component.translatable("vadsrijk.economie.stal.grapje").withStyle(ChatFormatting.ITALIC), (int) (pw / 0.75f))) {
                g.pose().pushMatrix();
                g.pose().translate(px, gy);
                g.pose().scale(0.75f, 0.75f);
                g.text(font, line, 0, 0, ZACHT, false);
                g.pose().popMatrix();
                gy += 7;
            }
        }

        // ---- info -----------------------------------------------------------------------------------------------------------
        int ix = px + pw + 8, iw = left + w - 8 - ix, iy = py;
        Component naam = Component.translatable(s.naamKey());
        if (s.kleuren()) {
            naam = Component.translatable("vadsrijk.economie.rijdieren.naam_kleur", naam, Component.translatable(kleur.naamKey()));
        }
        for (var line : font.split(naam.copy().withStyle(ChatFormatting.BOLD), iw)) {
            g.text(font, line, ix, iy, eigen ? TEKST : ZACHT, false);
            iy += 10;
        }
        iy += 2;
        for (var line : font.split(Component.translatable("vadsrijk.economie.rijdier.eigen." + s.id()), iw)) {
            g.text(font, line, ix, iy, ZACHT, false);
            iy += 9;
        }
        iy += 4;
        int level = t.getIntOr("level", 1);
        if (heeft) {
            g.text(font, Component.translatable("vadsrijk.economie.stal.level", level, RijdierSoort.MAX_LEVEL), ix, iy, GOUD, false);
            iy += 11;
            long xp = t.getLongOr("xp", 0), nodig = t.getLongOr("nodig", 0);
            g.fill(ix, iy, ix + iw, iy + 5, 0xFFE8C8DC);
            if (nodig > 0) {
                g.fill(ix, iy, ix + (int) (iw * Math.min(1.0, xp / (double) nodig)), iy + 5, 0xFFFF5FAF);
                iy += 7;
                g.text(font, xp + " / " + nodig + " xp", ix, iy, ZACHT, false);
            } else {
                g.fill(ix, iy, ix + iw, iy + 5, 0xFFFFC040);
                iy += 7;
                g.text(font, Component.translatable("vadsrijk.economie.stal.max"), ix, iy, GOUD, false);
            }
            iy += 12;
        }
        double sn = t.getDoubleOr("snelheid", 0), sp = t.getDoubleOr("sprong", 0);
        balkje(g, ix, iy, iw, Component.translatable("vadsrijk.economie.stal.snelheid", Math.round(sn / 0.1 * 100)), sn / 0.42);
        iy += 16;
        balkje(g, ix, iy, iw, Component.translatable("vadsrijk.economie.stal.sprong", String.format(Locale.ROOT, "%.1f", sp * sp / 0.16 + 0.5)),
                sp / 1.05);
        iy += 18;
        if (!eigen) {
            String bron = s.kleuren() ? "vadsrijk.economie.pluimkleur.bron." + kleur.id() : "vadsrijk.economie.stal.nog_niet." + s.id();
            for (var line : font.split(Component.translatable(bron), iw)) {
                g.text(font, line, ix, iy, 0xFF9A3A7A, false);
                iy += 9;
            }
        }

        // ---- buttons --------------------------------------------------------------------------------------------------------
        int by = top + h - 34;
        if (eigen) {
            boolean isActief = t.getBooleanOr("actief", false) && (!s.kleuren() || kleur.id().equals(t.getStringOr("kleur", "")));
            if (isActief) {
                g.centeredText(font, Component.translatable("vadsrijk.economie.stal.gekozen_label").withStyle(ChatFormatting.BOLD), ix + iw / 2, by + 5, GROEN);
            } else {
                knop(g, mx, my, ix, by, iw, 18, Component.translatable("vadsrijk.economie.rijdieren.kies"), true, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putString("soort", s.id());
                    a.putString("kleur", kleur.id());
                    doe("kies", a);
                });
            }
        } else if (stal) {
            long prijs = s.kleuren() ? kleur.prijs : t.getLongOr("prijs", 0);
            if (prijs > 0) {
                boolean kan = data.getLongOr("munten", 0) >= prijs;
                knop(g, mx, my, ix, by, iw, 18, Component.translatable("vadsrijk.economie.stal.koop", Spelers.munten(prijs)), kan, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putString("soort", s.id());
                    a.putString("kleur", kleur.id());
                    doe("koop", a);
                });
            }
        }
        if (stal) {
            boolean fluitje = data.getBooleanOr("fluitje", false);
            knop(g, mx, my, left + 8, by, 122, 18, Component.translatable("vadsrijk.economie.stal.koop_fluitje", Stal.FLUITJE_PRIJS), !fluitje,
                    () -> doe("fluitje"));
        } else {
            String toets = RijdierClient.RIJDEN.getTranslatedKeyMessage().getString();
            for (var line : font.split(Component.translatable("vadsrijk.economie.rijdieren.uitleg", toets), 124)) {
                g.text(font, line, left + 8, by, ZACHT, false);
                by += 9;
            }
        }
    }

    private void balkje(GuiGraphicsExtractor g, int x, int y, int bw, Component label, double frac) {
        g.text(font, label, x, y, TEKST, false);
        g.fill(x, y + 10, x + bw, y + 13, 0xFFE8C8DC);
        g.fill(x, y + 10, x + (int) (bw * Mth.clamp(frac, 0.05, 1.0)), y + 13, 0xFFFF7FC0);
    }

    /** Draws the mount turning slowly, sized to fit the box. */
    private void teken(GuiGraphicsExtractor g, RijdierEntity e, int x1, int y1, int x2, int y2) {
        Minecraft mc = Minecraft.getInstance();
        g.enableScissor(x1, y1, x2, y2);
        float hoog = Math.max(1.2f, e.getBbHeight() * 1.2f), breed = 2.0f;
        float schaal = Math.min((y2 - y1) * 0.62f / hoog, (x2 - x1) * 0.72f / breed);
        float yaw = (System.currentTimeMillis() % 12000L) / 12000f * 360f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(14 * Mth.DEG_TO_RAD);
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
        g.entity(state, schaal, new Vector3f(0, hoog * 0.5f + 4f / schaal, 0), pose, camera, x1, y1, x2, y2);
        g.disableScissor();
    }
}
