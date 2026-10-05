package nl.juiced.guhs.feature.guhpixel.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.taal.Tekst;

/**
 * "Pick one of your guhs" for any screen: a scrolling list of names (with hearts) on the left and a stand-in of the
 * picked guh on the right that you can turn by dragging. The list is what the server made with GuhKiezer.lijst (per guh:
 * Id, Naam, Looks, Hartjes, PlekSoort; a screen may add its own keys, e.g. "Uit" = a Component why this guh cannot be
 * picked, shown as the tooltip of a greyed-out row).
 * <pre>
 * kiezer = new GuhKiezerLijst(id -> rebuildWidgets());   // in the screen's constructor
 * kiezer.plaats(left + 8, top + 30, 200, 120); kiezer.zet(data.getListOrEmpty("Guhs"));   // in init / update
 * kiezer.teken(g, mouseX, mouseY);                        // in extractBackground
 * // mouseClicked -> klik, mouseScrolled -> wiel, mouseDragged -> sleep, mouseReleased -> los, tooltips -> tip
 * </pre>
 */
public final class GuhKiezerLijst {
    private static final int RIJ = 16;
    private final GidsLijst lijst = new GidsLijst();
    private final Consumer<UUID> opKies;
    private final List<CompoundTag> guhs = new ArrayList<>();
    private int x, y, w, h, lijstW;
    @Nullable
    private UUID gekozen;
    @Nullable
    private GuhEntity pop;
    private float yaw = 25f;
    private boolean draait;
    /** Colours: light screens (the Guhdex look) or the dark plum panels. */
    private boolean donker = true;

    public GuhKiezerLijst(Consumer<UUID> opKies) {
        this.opKies = opKies;
    }

    /** For a light panel (dark text). */
    public GuhKiezerLijst licht() {
        donker = false;
        return this;
    }

    /** The box: the list takes the left 58%, the stand-in the rest. */
    public void plaats(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.lijstW = Math.round(w * 0.58f);
        lijst.plaats(x, y, lijstW, h);
    }

    /** The guhs to pick from; the picked one stays picked when it is still in the list. */
    public void zet(ListTag lijstTag) {
        guhs.clear();
        List<GidsLijst.Regel> regels = new ArrayList<>();
        boolean nogDaar = false;
        for (int i = 0; i < lijstTag.size(); i++) {
            CompoundTag t = lijstTag.getCompoundOrEmpty(i);
            UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
            if (id == null) {
                continue;
            }
            guhs.add(t);
            regels.add(new Rij(t, id));
            nogDaar |= id.equals(gekozen);
        }
        if (!nogDaar) {
            gekozen = null;
            pop = null;
        }
        if (regels.isEmpty()) {
            regels.add(new Leeg());
        }
        lijst.zet(regels);
    }

    @Nullable
    public UUID gekozen() {
        return gekozen;
    }

    /** The whole tag of the picked guh (null: none). */
    @Nullable
    public CompoundTag gekozenTag() {
        for (CompoundTag t : guhs) {
            if (gekozen != null && gekozen.equals(t.read("Id", UUIDUtil.CODEC).orElse(null))) {
                return t;
            }
        }
        return null;
    }

    public void kies(@Nullable UUID id) {
        gekozen = id;
        pop = null;
        CompoundTag t = gekozenTag();
        if (t != null) {
            pop = GuhPop.van(t.getCompoundOrEmpty("Looks"));
        }
    }

    public boolean leeg() {
        return guhs.isEmpty();
    }

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
        int px = x + lijstW + 4;
        g.fill(px, y, x + w, y + h, donker ? 0x30F7B6CB : 0x24D27A9C);
        if (pop != null) {
            if (!draait) {
                yaw += 0.4f;
            }
            pop.tickCount++;
            GuhPop.teken(g, px, y, x + w, y + h, pop, yaw, -8f);
        } else {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.guhpixel.kiezer.kies"), px + 4, y + h / 2 - 10, x + w - px - 8, 0.75f, donker ? 0xFFB090A0 : 0xFFB0708A);
        }
    }

    public boolean wiel(double mx, double my, double delta) {
        return lijst.wiel(mx, my, delta);
    }

    public boolean klik(double mx, double my, int button) {
        if (mx >= x + lijstW + 4 && mx < x + w && my >= y && my < y + h) {
            draait = pop != null;
            return draait;
        }
        return lijst.klik(mx, my, button);
    }

    public boolean sleep(double mx, double my, double dx, double dy) {
        if (draait) {
            yaw += (float) dx * 1.5f;
            return true;
        }
        return lijst.sleep(my);
    }

    public void los() {
        draait = false;
        lijst.los();
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        return lijst.tip(mx, my);
    }

    private final class Leeg implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 30;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int rx, int ry, int rw, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.guhpixel.kiezer.geen"), rx + 3, ry + 3, rw - 6, 0.875f, donker ? 0xFFB090A0 : 0xFF9A8090);
        }
    }

    private final class Rij implements GidsLijst.Regel {
        private final CompoundTag t;
        private final UUID id;
        private final Component naam, uit;

        Rij(CompoundTag t, UUID id) {
            this.t = t;
            this.id = id;
            this.naam = Tekst.get(t, "Naam");
            this.uit = Tekst.get(t, "Uit");
        }

        private boolean kan() {
            return Tekst.empty(uit);
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int rx, int ry, int rw, int mouseX, int mouseY, boolean hover) {
            boolean ik = id.equals(gekozen);
            g.fill(rx + 1, ry + 1, rx + rw - 1, ry + RIJ - 1, ik ? 0x6068D88A : hover && kan() ? 0x50F7B6CB : 0x20F7B6CB);
            int tekst = !kan() ? 0xFF9A8090 : donker ? 0xFFFFE6EE : 0xFF3A1C30;
            Component hart = Component.literal("❤ " + t.getIntOr("Hartjes", 0));
            int gebruikt = GidsTekst.passend(g, hart, rx + rw - 4, ry + 5, 44, 0.75f, 0xFFE0629E, true);
            GidsTekst.passend(g, ik ? naam.copy().withStyle(ChatFormatting.BOLD) : naam, rx + 5, ry + 4, rw - gebruikt - 14, 1f, tekst, false);
        }

        @Override
        public boolean klik(double mx, double my, int rx, int ry, int rw) {
            if (kan()) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                kies(id);
                opKies.accept(id);
            }
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int rx, int ry, int rw) {
            return kan() ? null : List.of(naam.copy().withStyle(ChatFormatting.GRAY), uit.copy().withStyle(ChatFormatting.GOLD));
        }
    }
}
