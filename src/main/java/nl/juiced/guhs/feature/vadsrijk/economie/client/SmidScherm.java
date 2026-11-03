package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.SterRegels;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The smidguh's screen: pick a piece, see its stars and the chances, then "Ster erbij!" (hammer swing on the anvil, then a
 * golden fanfare or a soft pink plof), reroll the bonuses (click a bonus to keep it), or repair a verpluisd piece.
 */
public class SmidScherm extends EcoScherm {
    private static final int ANIM = 22, TOON = 50;
    private int kies = -1, houd = -1;
    /** Animation: ticks since the result came in (-1: none), the outcome, the stars to show while the hammer swings. */
    private int anim = -1, vorigeSterren = -1;
    private String uitkomst = "";
    private final RandomSource rng = RandomSource.create();

    public SmidScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.SMID, Component.translatable("vadsrijk.economie.smid.titel"), data, items, 330, 232);
        kies = data.getIntOr("kies", -1);
    }

    @Override
    protected void opNieuweData() {
        int k = data.getIntOr("kies", -1);
        if (k >= 0) {
            kies = k;
        }
        String u = data.getStringOr("uitkomst", "");
        if (!u.isEmpty()) {
            uitkomst = u;
            anim = 0;
            geluid(SoundEvents.ANVIL_PLACE, 1.6f);
        }
        if (stuk() == null || stuk().bonussen().size() <= houd) {
            houd = -1;
        }
    }

    private CompoundTag gekozen() {
        ListTag l = data.getListOrEmpty("gear");
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getIntOr("slot", -2) == kies) {
                return l.getCompoundOrEmpty(i);
            }
        }
        return null;
    }

    private ItemStack stapel() {
        CompoundTag t = gekozen();
        return t == null ? ItemStack.EMPTY : item(t.getIntOr("item", -1));
    }

    private Uitrusting stuk() {
        return Rollen.van(stapel());
    }

    @Override
    public void tick() {
        super.tick();
        if (anim >= 0) {
            anim++;
            if (anim == ANIM) {
                onthul();
            }
            if (anim > ANIM + TOON) {
                anim = -1;
                vorigeSterren = -1;
            }
        }
    }

    private void onthul() {
        switch (uitkomst) {
            case "GELUKT" -> {
                geluid(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.1f);
                geluid(SoundEvents.PLAYER_LEVELUP, 1.4f);
            }
            case "VERPLUISD" -> geluid(SoundEvents.WOOL_BREAK, 0.6f);
            case "STER_KWIJT" -> {
                geluid(SoundEvents.SLIME_SQUISH, 0.8f);
                geluid(SoundEvents.CHICKEN_EGG, 0.7f);
            }
            case "MISLUKT" -> geluid(SoundEvents.SLIME_SQUISH, 1.1f);
            case "HERROLD" -> geluid(SoundEvents.ENCHANTMENT_TABLE_USE, 1.3f);
            case "GEREPAREERD" -> geluid(SoundEvents.ANVIL_USE, 1.8f);
            default -> {
            }
        }
    }

    private void actie(String a) {
        Uitrusting u = stuk();
        vorigeSterren = u == null ? -1 : u.sterren();
        CompoundTag t = new CompoundTag();
        t.putInt("slot", kies);
        t.putInt("houd", houd);
        doe(a, t);
        geluid(SoundEvents.ANVIL_HIT, 1.2f);
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // left: your gear
        kop(g, Component.translatable("vadsrijk.economie.smid.jouw_spullen"), left + 8, top + 24);
        ListTag l = data.getListOrEmpty("gear");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 6) * 19, y = top + 36 + (i / 6) * 19;
            int slot = t.getIntOr("slot", -1);
            slot(g, mx, my, x, y, item(t.getIntOr("item", -1)), slot == kies);
            klikvlak(x, y, 18, 18, () -> {
                kies = slot;
                houd = -1;
                anim = -1;
            });
        }
        if (l.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.economie.smid.geen_gear"), left + 8, top + 40, ZACHT, false);
        }
        // materials
        int my0 = top + h - 46;
        materiaal(g, mx, my, left + 8, my0, new ItemStack(EcoSpul.VERBETERSTEENTJE.get()), data.getIntOr("steentjes", 0));
        materiaal(g, mx, my, left + 48, my0, new ItemStack(EcoSpul.HERROLKNABBEL.get()), data.getIntOr("knabbels", 0));
        materiaal(g, mx, my, left + 88, my0, new ItemStack(EcoSpul.PLUISDRAAD.get()), data.getIntOr("draad", 0));

        // right: the piece on the anvil
        int rx = left + 128, rw = w - 136;
        g.fill(rx, top + 22, rx + rw, top + h - 18, 0xFFFFF6FB);
        Uitrusting u = stuk();
        ItemStack s = stapel();
        if (u == null) {
            g.centeredText(font, Component.translatable("vadsrijk.economie.smid.kies"), rx + rw / 2, top + 90, ZACHT);
            return;
        }
        boolean bezig = anim >= 0 && anim < ANIM;
        // the anvil with the piece, big
        int ax = rx + 12, ay = top + 30;
        g.pose().pushMatrix();
        g.pose().translate(ax, ay + 14);
        g.pose().scale(2f, 2f);
        g.item(new ItemStack(Items.ANVIL), 0, 0);
        g.pose().popMatrix();
        float bob = bezig ? 0 : Mth.sin((System.currentTimeMillis() % 2000) / 2000f * Mth.TWO_PI) * 1.5f;
        g.pose().pushMatrix();
        g.pose().translate(ax + 4, ay - 2 + bob);
        g.pose().scale(1.5f, 1.5f);
        g.item(s, 0, 0);
        g.pose().popMatrix();
        if (bezig) {
            // the hammer swings
            float hoek = Mth.sin(anim / (float) ANIM * Mth.PI * 3) * 0.9f;
            g.pose().pushMatrix();
            g.pose().translate(ax + 34, ay + 4);
            g.pose().rotate(hoek - 0.6f);
            g.pose().scale(1.6f, 1.6f);
            g.item(new ItemStack(Items.MACE), -8, -14);
            g.pose().popMatrix();
            if (anim % 6 < 2) {
                for (int i = 0; i < 6; i++) {
                    int sx = ax + 16 + rng.nextInt(24) - 12, sy = ay + 14 + rng.nextInt(12) - 6;
                    g.fill(sx, sy, sx + 2, sy + 2, 0xFFFFE070);
                }
            }
        }
        int tx = ax + 46;
        g.text(font, s.getHoverName(), tx, top + 28, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.economie.smid.level", u.level()), tx, top + 38, ZACHT, false);
        // the stars
        int toonSterren = bezig && vorigeSterren >= 0 ? vorigeSterren : u.sterren();
        StringBuilder rij = new StringBuilder();
        for (int i = 0; i < SterRegels.MAX; i++) {
            rij.append(i < toonSterren ? '★' : '☆');
        }
        g.pose().pushMatrix();
        g.pose().translate(tx, top + 50);
        g.pose().scale(1.3f, 1.3f);
        g.text(font, Component.literal(rij.toString()), 0, 0, u.verpluisd() ? 0xFFB0A0B0 : 0xFFF0A800, false);
        g.pose().popMatrix();
        int y = top + 70;
        if (u.verpluisd() && !bezig) {
            g.text(font, Component.translatable("vadsrijk.economie.smid.is_verpluisd").withStyle(ChatFormatting.BOLD), rx + 8, y, ROOD, false);
            y += 12;
            long prijs = SterRegels.reparatiePrijs(u.level(), u.sterren());
            int draad = SterRegels.reparatieDraad(u.sterren());
            g.text(font, Component.translatable("vadsrijk.economie.smid.reparatie_kost", draad), rx + 8, y, TEKST, false);
            munten(g, rx + rw - 8, y, prijs, true, false);
            y += 14;
            knop(g, mx, my, rx + 8, y, rw - 16, 18, Component.translatable("vadsrijk.economie.smid.knop.repareer"),
                    data.getLongOr("munten", 0) >= prijs && data.getIntOr("draad", 0) >= draad, () -> actie("repareer"));
            y += 26;
        } else if (u.sterren() < SterRegels.MAX) {
            double[] risico = SterRegels.risico(u.sterren());
            g.text(font, Component.translatable("vadsrijk.economie.smid.kans", pct(SterRegels.KANS[u.sterren()])), rx + 8, y, GROEN, false);
            y += 10;
            if (risico[0] > 0 || risico[1] > 0) {
                g.text(font, Component.translatable("vadsrijk.economie.smid.risico", pct(risico[0]), pct(risico[1])), rx + 8, y, ROOD, false);
                y += 10;
            } else {
                g.text(font, Component.translatable("vadsrijk.economie.smid.veilig"), rx + 8, y, ZACHT, false);
                y += 10;
            }
            long prijs = SterRegels.prijs(u.level(), u.sterren());
            int steen = SterRegels.steentjes(u.sterren());
            g.text(font, Component.translatable("vadsrijk.economie.smid.ster_kost", steen), rx + 8, y + 2, TEKST, false);
            munten(g, rx + rw - 8, y + 2, prijs, true, false);
            y += 14;
            knop(g, mx, my, rx + 8, y, rw - 16, 18, Component.translatable("vadsrijk.economie.smid.knop.ster").withStyle(ChatFormatting.BOLD),
                    !bezig && data.getLongOr("munten", 0) >= prijs && data.getIntOr("steentjes", 0) >= steen, () -> actie("ster"));
            y += 24;
        } else {
            g.text(font, Component.translatable("vadsrijk.economie.smid.max_uitleg"), rx + 8, y, GOUD, false);
            y += 14;
        }
        // the bonuses (click one to keep it while rerolling)
        if (!u.bonussen().isEmpty() && !u.verpluisd()) {
            kop(g, Component.translatable("vadsrijk.economie.smid.bonussen"), rx + 8, y);
            y += 11;
            for (int i = 0; i < u.bonussen().size(); i++) {
                Bonus b = u.bonussen().get(i);
                boolean vast = i == houd;
                int by = y + i * 11;
                if (binnen(mx, my, rx + 6, by - 1, rw - 12, 11)) {
                    g.fill(rx + 6, by - 1, rx + rw - 6, by + 10, HOVER);
                }
                String w = b.stat().procent ? String.format(Locale.ROOT, "+%.1f%%", b.waarde()) : String.format(Locale.ROOT, "+%.0f", b.waarde());
                g.text(font, Component.literal(vast ? "♥ " : "• ").append(Component.translatable(b.stat().key())).append(" " + w), rx + 10, by,
                        vast ? GOUD : TEKST, false);
                final int idx = i;
                klikvlak(rx + 6, by - 1, rw - 12, 11, () -> houd = houd == idx ? -1 : idx);
            }
            y += u.bonussen().size() * 11 + 3;
            boolean slot = houd >= 0 && u.bonussen().size() > 1;
            long prijs = SterRegels.herrolPrijs(u.level(), slot);
            int kn = SterRegels.herrolKnabbels(slot);
            g.text(font, Component.translatable(slot ? "vadsrijk.economie.smid.herrol_kost_slot" : "vadsrijk.economie.smid.herrol_kost", kn), rx + 8, y + 4,
                    ZACHT, false);
            munten(g, rx + rw - 8, y + 4, prijs, true, false);
            y += 15;
            knop(g, mx, my, rx + 8, y, rw - 16, 16, Component.translatable("vadsrijk.economie.smid.knop.herrol"),
                    !bezig && data.getLongOr("munten", 0) >= prijs && data.getIntOr("knabbels", 0) >= kn, () -> actie("herrol"));
        }
        // the result card
        if (anim >= ANIM) {
            uitslag(g);
        }
    }

    private void uitslag(GuiGraphicsExtractor g) {
        int t = anim - ANIM;
        float a = Math.min(1f, t / 5f) * (t > TOON - 10 ? (TOON - t) / 10f : 1f);
        if (a <= 0) {
            return;
        }
        int alpha = (int) (a * 230) << 24;
        int cy = top + h / 2;
        int kleur;
        String key;
        switch (uitkomst) {
            case "GELUKT" -> {
                kleur = 0xFFD000;
                key = "vadsrijk.economie.smid.groot.gelukt";
            }
            case "VERPLUISD" -> {
                kleur = 0xC060C0;
                key = "vadsrijk.economie.smid.groot.verpluisd";
            }
            case "STER_KWIJT" -> {
                kleur = 0xFF7090;
                key = "vadsrijk.economie.smid.groot.ster_kwijt";
            }
            case "HERROLD" -> {
                kleur = 0x9070FF;
                key = "vadsrijk.economie.smid.groot.herrold";
            }
            case "GEREPAREERD" -> {
                kleur = 0x40C080;
                key = "vadsrijk.economie.smid.groot.gerepareerd";
            }
            default -> {
                kleur = 0xFF90C0;
                key = "vadsrijk.economie.smid.groot.mislukt";
            }
        }
        g.fill(left + 20, cy - 22, left + w - 20, cy + 22, alpha | 0xFFFFFF);
        g.fill(left + 20, cy - 22, left + w - 20, cy - 20, alpha | kleur);
        g.fill(left + 20, cy + 20, left + w - 20, cy + 22, alpha | kleur);
        float schaal = 2f + (uitkomst.equals("GELUKT") ? Mth.sin(t * 0.3f) * 0.15f : 0);
        Component c = Component.translatable(key).withStyle(ChatFormatting.BOLD);
        g.pose().pushMatrix();
        g.pose().translate(left + w / 2f, cy - 8);
        g.pose().scale(schaal, schaal);
        g.centeredText(font, c, 0, 0, alpha | kleur);
        g.pose().popMatrix();
        if (uitkomst.equals("GELUKT")) {
            for (int i = 0; i < 14; i++) {
                double hoek = i / 14.0 * Math.PI * 2 + t * 0.05;
                int r = 20 + t * 2;
                int sx = (int) (left + w / 2 + Math.cos(hoek) * r * 1.6), sy = (int) (cy + Math.sin(hoek) * r * 0.6);
                g.text(font, "★", sx, sy, alpha | 0xFFD000, false);
            }
        } else if (uitkomst.equals("MISLUKT") || uitkomst.equals("STER_KWIJT") || uitkomst.equals("VERPLUISD")) {
            for (int i = 0; i < 10; i++) {
                double hoek = i / 10.0 * Math.PI * 2;
                int r = 10 + t;
                int sx = (int) (left + w / 2 + Math.cos(hoek) * r * 1.4), sy = (int) (cy + 14 + Math.sin(hoek) * r * 0.4);
                g.fill(sx, sy, sx + 3, sy + 3, alpha | 0xFFC0E0);
            }
        }
    }

    private void materiaal(GuiGraphicsExtractor g, int mx, int my, int x, int y, ItemStack s, int n) {
        slot(g, mx, my, x, y, s, false);
        g.text(font, "x" + n, x + 20, y + 5, TEKST, false);
    }

    private static String pct(double d) {
        return Math.round(d * 1000) / 10.0 + "%";
    }
}
