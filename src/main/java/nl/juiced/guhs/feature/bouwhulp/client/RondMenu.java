package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Gereedschap;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import org.lwjgl.glfw.GLFW;

/**
 * The vormstaf's radial menu (design 2.4): hold the Vormstaf key. The shapes (or roofs) lie in two rings around the
 * middle; point at one and let go of the key (or click it). Scroll: the size; shift+scroll: the height; ctrl+scroll: the
 * wall thickness; H: hollow or filled; click the middle: shape → roof → capture.
 */
public class RondMenu extends Screen {
    private record Punt(int index, int x, int y, boolean dicht, Component naam) {
    }

    private CompoundTag staf;
    private final List<Punt> punten = new ArrayList<>();
    private int hover = -1, cx, cy;

    RondMenu(CompoundTag staf) {
        super(Component.translatable("item.guhs.vormstaf"));
        this.staf = staf;
    }

    void update(CompoundTag nieuw) {
        this.staf = nieuw;
        rebuildWidgets();
    }

    private CompoundTag optie(String key) {
        ListTag l = staf.getListOrEmpty("Opties");
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getStringOr("Key", "").equals(key)) {
                return l.getCompoundOrEmpty(i);
            }
        }
        return new CompoundTag();
    }

    private int modus() {
        return staf.getCompoundOrEmpty("Instelling").getIntOr("modus", 0);
    }

    private void zet(String key, int waarde) {
        ClientPacketDistributor.sendToServer(new GereedschapPayloads.Doe("optie", "vormstaf", key, waarde, ""));
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
        punten.clear();
        String key = modus() == Gereedschap.DAK ? "dak" : modus() == Gereedschap.VORM ? "vorm" : null;
        if (key == null) {
            return;
        }
        ListTag keuzes = optie(key).getListOrEmpty("Keuzes");
        int n = keuzes.size(), binnen = n > 8 ? (n + 1) / 2 : n;
        for (int i = 0; i < n; i++) {
            boolean ring1 = i < binnen;
            int k = ring1 ? i : i - binnen, tal = ring1 ? binnen : n - binnen;
            double r = ring1 ? 70 : 118, a = -Math.PI / 2 + (k + (ring1 ? 0 : 0.5)) * Math.PI * 2 / tal;
            CompoundTag c = keuzes.getCompoundOrEmpty(i);
            punten.add(new Punt(i, cx + (int) (Math.cos(a) * r), cy + (int) (Math.sin(a) * r * 0.82), c.contains("Dicht"),
                    Component.translatable("gui.guhs.bouwhulp.keuze." + key + "." + c.getStringOr("Id", ""))));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        hover = -1;
        double beste = 26 * 26;
        for (Punt p : punten) {
            double d = (p.x - mouseX) * (p.x - mouseX) + (p.y - mouseY) * (p.y - mouseY);
            if (d < beste) {
                beste = d;
                hover = p.index;
            }
        }
        int gekozen = optie(modus() == Gereedschap.DAK ? "dak" : "vorm").getIntOr("Waarde", -1);
        for (Punt p : punten) {
            boolean h = p.index == hover, k = p.index == gekozen;
            int kleur = p.dicht ? 0xB0606060 : k ? 0xE0E07AAE : h ? 0xE0FFE9B8 : 0xC0FFF9EC;
            g.fill(p.x - 24, p.y - 7, p.x + 24, p.y + 7, kleur);
            GidsTekst.passend(g, p.naam, p.x - 22, p.y - 3, 44, 0.6f, p.dicht ? 0xFF303030 : 0xFF5A3A1E, false);
        }
        // the middle: the mode and the sizes
        boolean midden = (mouseX - cx) * (mouseX - cx) + (mouseY - cy) * (mouseY - cy) < 28 * 28;
        g.fill(cx - 34, cy - 24, cx + 34, cy + 24, midden ? 0xE0FFE9B8 : 0xD0FFF9EC);
        int modus = modus();
        Component m = Component.translatable("gui.guhs.bouwhulp.keuze.modus." + new String[] {"vorm", "dak", "vastleg"}[Math.min(2, modus)]);
        GidsTekst.passend(g, m.copy().withStyle(ChatFormatting.BOLD), cx - 32, cy - 20, 64, 0.75f, 0xFF5A3A1E, false);
        if (modus != Gereedschap.VASTLEG) {
            int b = optie("b").getIntOr("Waarde", 0), d = optie("d").getIntOr("Waarde", 0);
            String maat = modus == Gereedschap.VORM ? b + "×" + optie("h").getIntOr("Waarde", 0) + "×" + d : b + "×" + d;
            GidsTekst.passend(g, Component.literal(maat), cx - 32, cy - 9, 64, 0.75f, 0xFF6A4A2A, false);
            if (modus == Gereedschap.VORM) {
                CompoundTag hol = optie("hol");
                Component h = hol.isEmpty() ? Component.empty() : Component.translatable(hol.getIntOr("Waarde", 1) != 0 ? "gui.guhs.bouwhulp.hol" : "gui.guhs.bouwhulp.gevuld");
                GidsTekst.passend(g, h, cx - 32, cy + 1, 64, 0.65f, 0xFF6A4A2A, false);
            }
        }
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.rond.uitleg"), cx - 32, cy + 11, 64, 0.5f, 0xFFA0845E, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if ((mx - cx) * (mx - cx) + (my - cy) * (my - cy) < 28 * 28) {
            zet("modus", (modus() + 1) % 3);
            return true;
        }
        if (kies()) {
            onClose();
        }
        return true;
    }

    private boolean kies() {
        for (Punt p : punten) {
            if (p.index == hover && !p.dicht) {
                zet(modus() == Gereedschap.DAK ? "dak" : "vorm", p.index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int stap = (int) Math.signum(scrollY);
        if (stap == 0 || modus() == Gereedschap.VASTLEG) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasControlDown() && modus() == Gereedschap.VORM) {
            zet("dikte", optie("dikte").getIntOr("Waarde", 1) + stap);
        } else if (mc.hasShiftDown()) {
            if (modus() == Gereedschap.VORM) {
                zet("h", optie("h").getIntOr("Waarde", 1) + stap);
            } else {
                zet("d", optie("d").getIntOr("Waarde", 1) + stap);
            }
        } else {
            zet("b", optie("b").getIntOr("Waarde", 1) + stap);
            if (modus() == Gereedschap.VORM) {
                zet("d", optie("d").getIntOr("Waarde", 1) + stap);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_H && modus() == Gereedschap.VORM && !optie("hol").isEmpty()) {
            zet("hol", optie("hol").getIntOr("Waarde", 1) != 0 ? 0 : 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (GereedschapClient.MENU.matches(event)) {
            kies();
            onClose();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
