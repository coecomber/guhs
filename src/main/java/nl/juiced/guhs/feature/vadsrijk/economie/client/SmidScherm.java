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
    /** T-ITEMS: the tab (Sterren / Bonussen / Doorgroeien / Overvadsen) and the bridge to the items slice's tab drawing. */
    private static int tab = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.STERREN;
    private final nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.Brug brug = new nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.Brug() {
        @Override
        public net.minecraft.client.gui.Font font() {
            return font;
        }

        @Override
        public CompoundTag data() {
            return data;
        }

        @Override
        public ItemStack item(int i) {
            return SmidScherm.this.item(i);
        }

        @Override
        public int kies() {
            return kies;
        }

        @Override
        public void knop(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, Component label, boolean actief, Runnable actie) {
            SmidScherm.this.knop(g, mx, my, x, y, w, h, label, actief, () -> {
                Uitrusting u = stuk();
                vorigeSterren = u == null ? -1 : u.sterren();
                actie.run();
            });
        }

        @Override
        public void klikvlak(int x, int y, int w, int h, Runnable actie) {
            SmidScherm.this.klikvlak(x, y, w, h, actie);
        }

        @Override
        public void slot(GuiGraphicsExtractor g, int mx, int my, int x, int y, ItemStack s, boolean gekozen) {
            SmidScherm.this.slot(g, mx, my, x, y, s, gekozen);
        }

        @Override
        public int munten(GuiGraphicsExtractor g, int x, int y, long brons) {
            return SmidScherm.this.munten(g, x, y, brons, true, false);
        }

        @Override
        public void doe(String actie, CompoundTag args) {
            SmidScherm.this.doe(actie, args);
        }
    };

    public SmidScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.SMID, Component.translatable("vadsrijk.economie.smid.titel"), data, items, 330, 232);
        kies = data.getIntOr("kies", -1);
        ListTag gear = data.getListOrEmpty("gear");
        if (kies < 0 && !gear.isEmpty()) {
            kies = gear.getCompoundOrEmpty(0).getIntOr("slot", -1);   // 1.10.24: open on your first piece instead of an empty anvil
        }
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
            case "GEPOLIJST", "OVERGEVADST", "LIEF" -> geluid(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);   // T-ITEMS
            case "DOORGEGROEID" -> geluid(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f);   // T-ITEMS
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

    /** 1.10.24: the left column (your gear, 7 per row), the tab panel's scroll, its content height last frame and its clip. */
    private static final int LINKS = 7 * 20 + 4, KOLOM = 7;
    private int scroll, inhoudH, gearScroll;
    private int[] vak = new int[4], gearVak = new int[4];

    /**
     * 1.10.15: wider. 1.10.24 (the user: "heel veel knoppen met slecht leesbare tekst, knoppen over elkaar"): a roomier logical
     * size (EcoScherm scales it up to the window, so the text gets bigger too), taller buttons and tabs, prices beside the buttons
     * instead of under them, and a tab that is taller than its panel scrolls instead of running over the bottom.
     */
    @Override
    protected int[] groeit() {
        return new int[] {460, 250, 540, 320};
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (binnen((int) mx, (int) my, vak[0], vak[1], vak[2] - vak[0], vak[3] - vak[1])) {
            scroll = Mth.clamp(scroll - (int) Math.signum(sy) * 14, 0, Math.max(0, inhoudH - (vak[3] - vak[1])));
            return true;
        }
        if (binnen((int) mx, (int) my, gearVak[0], gearVak[1], gearVak[2] - gearVak[0], gearVak[3] - gearVak[1])) {
            gearScroll = Math.max(0, gearScroll - (int) Math.signum(sy));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // ---- left: your gear (scrolls when it does not fit), the materials, the shop and Opruimen
        int lx = left + 8;
        kop(g, Component.translatable("vadsrijk.economie.smid.jouw_spullen"), lx, top + 25);
        ListTag l = data.getListOrEmpty("gear");
        int matY = top + h - 78;
        int rijen = Math.max(1, (matY - 6 - (top + 37)) / 20);
        int totRijen = (l.size() + KOLOM - 1) / KOLOM;
        gearScroll = Math.min(gearScroll, Math.max(0, totRijen - rijen));
        gearVak = new int[] {lx, top + 37, lx + LINKS, top + 37 + rijen * 20};
        for (int i = gearScroll * KOLOM; i < l.size() && i < (gearScroll + rijen) * KOLOM; i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int r = i / KOLOM - gearScroll;
            int x = lx + (i % KOLOM) * 20, y = top + 37 + r * 20;
            int slot = t.getIntOr("slot", -1);
            slot(g, mx, my, x, y, item(t.getIntOr("item", -1)), slot == kies);
            klikvlak(x, y, 18, 18, () -> {
                if (kies != slot) {
                    scroll = 0;
                }
                kies = slot;
                houd = -1;
                anim = -1;
            });
        }
        if (totRijen > rijen) {   // a thin scroll bar to the right of the grid
            int bh = gearVak[3] - gearVak[1], dh = Math.max(8, bh * rijen / totRijen), dy = (bh - dh) * gearScroll / Math.max(1, totRijen - rijen);
            g.fill(lx + LINKS - 3, gearVak[1], lx + LINKS - 1, gearVak[3], 0x40C0287C);
            g.fill(lx + LINKS - 3, gearVak[1] + dy, lx + LINKS - 1, gearVak[1] + dy + dh, 0xFFE0489A);
        }
        if (l.isEmpty()) {
            nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.geen_gear"), lx, top + 40, LINKS, ZACHT);
        }
        materiaal(g, mx, my, lx, matY, new ItemStack(EcoSpul.VERBETERSTEENTJE.get()), data.getIntOr("steentjes", 0));
        materiaal(g, mx, my, lx + 48, matY, new ItemStack(EcoSpul.HERROLKNABBEL.get()), data.getIntOr("knabbels", 0));
        materiaal(g, mx, my, lx + 96, matY, new ItemStack(EcoSpul.PLUISDRAAD.get()), data.getIntOr("draad", 0));
        // 1.10.1: Opruimen (salvage / sell old gear); 1.10.15: the smid's own shop
        knop(g, mx, my, lx, top + h - 55, LINKS, 17, Component.translatable("vadsrijk.economie.smid.knop.winkel"), true,
                () -> doe("winkel", new CompoundTag()));
        knop(g, mx, my, lx, top + h - 35, LINKS, 17, Component.translatable("vadsrijk.economie.opruim.smid_knop", data.getIntOr("slechter", 0)), true,
                () -> doe("opruimen", new CompoundTag()));

        // ---- right: the piece on the anvil
        int rx = left + LINKS + 16, rw = left + w - 8 - rx;
        g.fill(rx, top + 22, rx + rw, top + h - 16, 0xFFFFF6FB);
        Uitrusting u = stuk();
        ItemStack s = stapel();
        if (u == null) {
            nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.kies"), rx + 12, top + 90, rw - 24, ZACHT);
            return;
        }
        boolean bezig = anim >= 0 && anim < ANIM;
        // the anvil with the piece, big
        int ax = rx + 10, ay = top + 30;
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
        int tx = ax + 50;
        g.text(font, nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.kort(font, s.getHoverName(), rx + rw - tx - 6), tx, top + 28, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.economie.smid.level", u.level()), tx, top + 40, ZACHT, false);
        // the stars
        int toonSterren = bezig && vorigeSterren >= 0 ? vorigeSterren : u.sterren();
        StringBuilder rij = new StringBuilder();
        for (int i = 0; i < SterRegels.MAX; i++) {
            rij.append(i < toonSterren ? '★' : '☆');
        }
        g.pose().pushMatrix();
        g.pose().translate(tx, top + 52);
        g.pose().scale(1.3f, 1.3f);
        g.text(font, Component.literal(rij.toString()), 0, 0, u.verpluisd() ? 0xFFB0A0B0 : 0xFFF0A800, false);
        g.pose().popMatrix();
        // T-ITEMS: tabs (Sterren · Bonussen · Doorgroeien · Overvadsen)
        int y0 = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.tabRij(brug, g, mx, my, rx + 6, top + 70, rw - 12, tab, t -> {
            tab = t;
            scroll = 0;
        });
        // 1.10.24: the tab content in its own clipped, scrolling box (it ran over the bottom and the message line)
        vak = new int[] {rx + 2, y0 - 2, rx + rw - 2, top + h - 18};
        int maxScroll = Math.max(0, inhoudH - (vak[3] - vak[1]));
        scroll = Math.min(scroll, maxScroll);
        zetKlip(g, vak[0], vak[1], vak[2], vak[3]);
        int bx = rx + 8, bw = rw - 16 - (maxScroll > 0 ? 6 : 0);
        int y = y0 - scroll;
        if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.DOORGROEIEN) {
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.doorgroei(brug, g, mx, my, bx, y, bw, u);
        } else if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.OVERVADSEN) {
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.overvads(brug, g, mx, my, bx, y, bw, u);
        } else if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.STERREN && u.verpluisd() && !bezig) {
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.is_verpluisd").withStyle(ChatFormatting.BOLD), bx, y, bw, ROOD) + 3;
            long prijs = SterRegels.reparatiePrijs(u.level(), u.sterren());
            int draad = SterRegels.reparatieDraad(u.sterren());
            int mb = munten(g, bx + bw, y, prijs, true, false);
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.reparatie_kost", draad), bx, y, bw - mb - 8, TEKST) + 4;
            knop(g, mx, my, bx, y, bw, 20, Component.translatable("vadsrijk.economie.smid.knop.repareer"),
                    data.getLongOr("munten", 0) >= prijs && data.getIntOr("draad", 0) >= draad, () -> actie("repareer"));
            y += 26;
        } else if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.STERREN && u.sterren() < SterRegels.MAX) {
            double[] risico = SterRegels.risico(u.sterren());
            // T-ITEMS: the chance with the smid's patience, the patience meter, the talisman switch
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.sterBoven(brug, g, mx, my, bx, y, bw, u);
            if (risico[0] > 0 || risico[1] > 0) {
                y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.risico", pct(risico[0]), pct(risico[1])), bx, y, bw, ROOD);
            } else {
                y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.veilig"), bx, y, bw, ZACHT);
            }
            long prijs = SterRegels.prijs(u.level(), u.sterren());
            int steen = SterRegels.steentjes(u.sterren());
            int mb = munten(g, bx + bw, y + 3, prijs, true, false);
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.ster_kost", steen), bx, y + 3, bw - mb - 8, TEKST) + 3;
            knop(g, mx, my, bx, y, bw, 20, Component.translatable("vadsrijk.economie.smid.knop.ster").withStyle(ChatFormatting.BOLD),
                    !bezig && data.getLongOr("munten", 0) >= prijs && data.getIntOr("steentjes", 0) >= steen, () -> actie("ster"));
            y += 25;
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.sterVoorbeeld(brug, g, bx, y, bw, u);   // T-ITEMS: preview
        } else if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.STERREN) {
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable("vadsrijk.economie.smid.max_uitleg"), bx, y, bw, GOUD) + 4;
        }
        // the bonuses (click one to keep it while rerolling; T-ITEMS: its own tab, + polijsten)
        if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.BONUSSEN && !u.bonussen().isEmpty() && !u.verpluisd()) {
            kop(g, Component.translatable("vadsrijk.economie.smid.bonussen"), bx, y);
            y += 13;
            for (int i = 0; i < u.bonussen().size(); i++) {
                Bonus b = u.bonussen().get(i);
                boolean vast = i == houd;
                int by = y + i * 13;
                if (binnen(mx, my, bx - 2, by - 2, bw + 4, 13) && binnen(mx, my, vak[0], vak[1], vak[2] - vak[0], vak[3] - vak[1])) {
                    g.fill(bx - 2, by - 2, bx + bw + 2, by + 11, HOVER);
                }
                double bwaarde = b.waarde() * Rollen.budget(u);   // T-ITEMS: the slot budget, as in the tooltip
                String w = b.stat().procent ? String.format(Locale.ROOT, "+%.1f%%", bwaarde) : String.format(Locale.ROOT, "+%.0f", bwaarde);
                g.text(font, nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.kort(font,
                        Component.literal(vast ? "♥ " : "• ").append(Component.translatable(b.stat().key())).append(" " + w), bw - 4), bx + 2, by, vast ? GOUD : TEKST, false);
                final int idx = i;
                klikvlak(bx - 2, by - 2, bw + 4, 13, () -> houd = houd == idx ? -1 : idx);
            }
            y += u.bonussen().size() * 13 + 4;
            boolean slot = houd >= 0 && u.bonussen().size() > 1;
            long prijs = SterRegels.herrolPrijs(u.level(), slot);
            int kn = SterRegels.herrolKnabbels(slot);
            int mb = munten(g, bx + bw, y, prijs, true, false);
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable(slot ? "vadsrijk.economie.smid.herrol_kost_slot" : "vadsrijk.economie.smid.herrol_kost", kn), bx,
                    y, bw - mb - 8, ZACHT) + 3;
            knop(g, mx, my, bx, y, bw, 18, Component.translatable("vadsrijk.economie.smid.knop.herrol"),
                    !bezig && data.getLongOr("munten", 0) >= prijs && data.getIntOr("knabbels", 0) >= kn, () -> actie("herrol"));
            y += 24;
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.polijst(brug, g, mx, my, bx, y, bw, u, houd);   // T-ITEMS
        } else if (tab == nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.BONUSSEN) {
            y = nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs.regels(font, g, Component.translatable(u.verpluisd() ? "vadsrijk.economie.smid.eerst_repareren" : "vadsrijk.economie.smid.geen_bonussen"),
                    bx, y, bw, ZACHT);   // T-ITEMS
        }
        geenKlip(g);
        inhoudH = y + scroll - y0 + 4;
        if (maxScroll > 0) {   // a scroll bar on the right edge of the tab panel
            int bh = vak[3] - vak[1], dh = Math.max(10, bh * bh / Math.max(1, inhoudH)), dy = (bh - dh) * scroll / maxScroll;
            g.fill(vak[2] - 4, vak[1], vak[2] - 1, vak[3], 0x40C0287C);
            g.fill(vak[2] - 4, vak[1] + dy, vak[2] - 1, vak[1] + dy + dh, 0xFFE0489A);
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
            case "GEPOLIJST", "DOORGEGROEID", "OVERGEVADST", "LIEF" -> {   // T-ITEMS
                kleur = uitkomst.equals("LIEF") ? 0xFF90C0 : 0xD0A000;
                key = "vadsrijk.items.smid.groot." + uitkomst.toLowerCase(Locale.ROOT);
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
