package nl.juiced.guhs.feature.kapper.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.kapper.Haarverf;
import nl.juiced.guhs.feature.kapper.KapperFeature;
import nl.juiced.guhs.feature.kapper.KapperHaar;
import nl.juiced.guhs.feature.kapper.KapperKlantEntity;
import nl.juiced.guhs.feature.kapper.KapperPayloads;
import nl.juiced.guhs.feature.kapper.KappersShow;
import nl.juiced.guhs.feature.kapper.Kapsel;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The knip screen of the kappersshow: left the customer's picture ("zo wil ik het!": a guh with the wished hairstyle and
 * colour; from the 4th customer on it's put away after a few seconds), in the middle the customer in the chair (live:
 * what you do shows at once), right the four steps: wash (3x), cut (8 styles), dye (8 colours or natural) and the föhn,
 * which finishes the customer. On top the customer's patience, the score and the combo. Closing the screen doesn't stop
 * the show: right-click the customer (or Krulletje) to come back.
 */
public class KnipScreen extends Screen {
    private static final int W = 360, H = 214;
    private static final int PANEL = 0xF0301A26, TEXT = 0xFFFFE6EE, ROZE = 0xFFF7B6CB, GOUD = 0xFFFFD27A;
    private final int npcId;
    private CompoundTag data;
    private int left, top;
    /** The timer as the server sent it, and the client game time it arrived. */
    private int timer;
    private long ontvangen;
    @Nullable
    private KapperKlantEntity foto;
    private final List<Button> stijlKnoppen = new ArrayList<>();
    private final List<Button> verfKnoppen = new ArrayList<>();

    public KnipScreen(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.kapper.knip.titel"));
        this.npcId = npcId;
        this.data = data;
        ontvang();
    }

    public int npcId() {
        return npcId;
    }

    public void update(CompoundTag newData) {
        this.data = newData;
        ontvang();
        rebuildWidgets();
    }

    private void ontvang() {
        timer = data.getIntOr("Timer", 0);
        ontvangen = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime()
                : net.minecraft.client.Minecraft.getInstance().level == null ? 0 : net.minecraft.client.Minecraft.getInstance().level.getGameTime();
        foto = null;
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new KapperPayloads.Action(npcId, action));
    }

    private boolean klant() {
        return "KLANT".equals(data.getStringOr("Fase", ""));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        stijlKnoppen.clear();
        verfKnoppen.clear();
        boolean aan = klant();
        int cx = left + 240;
        int gewassen = data.getIntOr("Gewassen", 0);
        Button was = Button.builder(Component.translatable("gui.guhs.kapper.knip.wassen", gewassen, KappersShow.WASSEN), b -> send(KappersShow.WAS))
                .bounds(cx, top + 36, 112, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.knip.wassen.tooltip"))).build();
        was.active = aan && gewassen < KappersShow.WASSEN;
        addRenderableWidget(was);
        Kapsel[] kapsels = Kapsel.values();
        for (int i = 0; i < kapsels.length; i++) {
            int n = i;
            Button b = Button.builder(Component.empty(), x -> send(KappersShow.KNIP + n))
                    .bounds(cx + (i % 4) * 28, top + 68 + (i / 4) * 20, 26, 18)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.kapsel." + kapsels[i].stijl()))).build();
            b.active = aan && gewassen >= KappersShow.WASSEN;
            stijlKnoppen.add(addRenderableWidget(b));
        }
        boolean geknipt = data.getIntOr("Gekozen", 0) >= 0;
        for (int i = 0; i <= KappersShow.NATUREL; i++) {
            int n = i;
            String key = i == KappersShow.NATUREL ? "gui.guhs.kapper.verf.naturel" : "gui.guhs.kapper.verf." + Haarverf.values()[i].kleur();
            Button b = Button.builder(Component.empty(), x -> send(KappersShow.VERF + n))
                    .bounds(cx + (i % 5) * 22, top + 122 + (i / 5) * 18, 20, 16).tooltip(Tooltip.create(Component.translatable(key))).build();
            b.active = aan && geknipt;
            verfKnoppen.add(addRenderableWidget(b));
        }
        Button fohn = Button.builder(Component.translatable("gui.guhs.kapper.knip.fohn").withStyle(ChatFormatting.BOLD), b -> send(KappersShow.FOHN))
                .bounds(cx, top + 162, 112, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.knip.fohn.tooltip"))).build();
        fohn.active = aan && geknipt;
        addRenderableWidget(fohn);
        if (aan && !data.getBooleanOr("Foto", false)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kapper.knip.kijk", KappersShow.KIJK_PRIJS), b -> send(KappersShow.KIJK))
                    .bounds(left + 8, top + 162, 108, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kapper.knip.stop"), b -> {
            send(KappersShow.STOP);
            onClose();
        }).bounds(left + 8, top + H - 24, 80, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.knip.stop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kapper.knip.sluit"), b -> onClose())
                .bounds(left + W - 88, top + H - 24, 80, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.knip.sluit.tooltip"))).build());
    }

    private int tijdOver() {
        if (!klant() || minecraft == null || minecraft.level == null) {
            return 0;
        }
        return Math.max(0, timer - (int) (minecraft.level.getGameTime() - ontvangen));
    }

    @Nullable
    private KapperKlantEntity foto() {
        if (foto == null && minecraft != null && minecraft.level != null && data.getIntOr("Wens", 0) >= 0) {
            foto = KapperFeature.KAPPER_KLANT.get().create(minecraft.level, EntitySpawnReason.TRIGGERED);
            if (foto != null) {
                foto.setVariant(GuhVariant.byId(data.getStringOr("Variant", "")));
                foto.wear(Kapsel.values()[data.getIntOr("Wens", 0)].kleding);
                int v = data.getIntOr("WensVerf", 0);
                foto.setHaarkleur(v < 0 ? -1 : Haarverf.values()[v].rgb);
            }
        }
        if (foto != null && data.getIntOr("WensVerf", 0) == Haarverf.REGENBOOG.ordinal() && minecraft.level != null) {
            foto.setHaarkleur(KapperHaar.regenboog(minecraft.level.getGameTime()));
        }
        return foto;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // the icons on the buttons: the hairstyles, the colours
        Kapsel[] kapsels = Kapsel.values();
        for (int i = 0; i < stijlKnoppen.size(); i++) {
            Button b = stijlKnoppen.get(i);
            g.item(new ItemStack(ModItems.clothingItem(kapsels[i].kleding)), b.getX() + 5, b.getY() + 1);
            if (data.getIntOr("Gekozen", 0) == i) {
                outline(g, b.getX() - 1, b.getY() - 1, b.getWidth() + 2, b.getHeight() + 2, GOUD);
            }
        }
        for (int i = 0; i < verfKnoppen.size(); i++) {
            Button b = verfKnoppen.get(i);
            if (i == KappersShow.NATUREL) {
                g.centeredText(font, "N", b.getX() + b.getWidth() / 2, b.getY() + 4, 0xFFFFF0DD);
            } else {
                int rgb = Haarverf.values()[i] == Haarverf.REGENBOOG && minecraft.level != null
                        ? KapperHaar.regenboog(minecraft.level.getGameTime()) : Haarverf.values()[i].rgb;
                g.fill(b.getX() + 4, b.getY() + 3, b.getX() + b.getWidth() - 4, b.getY() + b.getHeight() - 3, 0xFF000000 | rgb);
            }
            if (data.getIntOr("GekozenVerf", 0) == i) {
                outline(g, b.getX() - 1, b.getY() - 1, b.getWidth() + 2, b.getHeight() + 2, GOUD);
            }
        }
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, ROZE);
        g.fill(left, top, left + W, top + H, PANEL);
        boolean feest = data.getBooleanOr("Feest", false);
        Component kop = Component.translatable(feest ? "gui.guhs.kapper.knip.kop_feest" : "gui.guhs.kapper.knip.kop",
                Math.min(data.getIntOr("Nr", 0) + 1, data.getIntOr("Aantal", 0)), data.getIntOr("Aantal", 0));
        g.text(font, kop.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 7, TEXT, false);
        Component score = Component.translatable("gui.guhs.kapper.knip.score", data.getIntOr("Score", 0), data.getIntOr("Combo", 0));
        g.text(font, score, left + W - 8 - font.width(score), top + 7, GOUD, false);
        // the patience bar
        int geduld = Math.max(1, data.getIntOr("Geduld", 0));
        int over = tijdOver();
        float f = klant() ? Math.min(1f, over / (float) geduld) : 0f;
        g.fill(left + 8, top + 19, left + W - 8, top + 25, 0xFF4A2A3A);
        int col = f > 0.5f ? 0xFF7CE08A : f > 0.25f ? 0xFFFFD25A : 0xFFFF6A7A;
        g.fill(left + 8, top + 19, left + 8 + (int) ((W - 16) * f), top + 25, col);
        if (klant()) {
            String s = ((over + 19) / 20) + " s";
            g.centeredText(font, s, width / 2, top + 18, 0xFFFFFFFF);
        }
        // the three panels
        int py0 = top + 32, py1 = top + 158;
        g.fill(left + 8, py0, left + 116, py1, 0xFFFFF6F0);                   // the picture: a polaroid
        g.fill(left + 12, py0 + 4, left + 112, py1 - 22, 0xFFBFE6FF);
        g.fill(left + 124, py0, left + 232, py1, 0x30F7B6CB);
        g.text(font, Component.translatable("gui.guhs.kapper.knip.stap_was"), left + 240, top + 28, stapKleur(data.getIntOr("Gewassen", 0) >= KappersShow.WASSEN), false);
        g.text(font, Component.translatable("gui.guhs.kapper.knip.stap_knip"), left + 240, top + 58, stapKleur(data.getIntOr("Gekozen", 0) >= 0), false);
        g.text(font, Component.translatable("gui.guhs.kapper.knip.stap_verf"), left + 240, top + 112, stapKleur(data.getIntOr("GekozenVerf", 0) >= 0), false);
        boolean fotoZichtbaar = data.getBooleanOr("Foto", false);
        int wens = data.getIntOr("Wens", 0), wensVerf = data.getIntOr("WensVerf", 0);
        if (klant() && fotoZichtbaar && wens >= 0) {
            KapperKlantEntity f1 = foto();
            if (f1 != null) {
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, left + 12, py0 + 4, left + 112, py1 - 22, 34, 0.0625f, mouseX, mouseY, f1);
            }
            Component naam = Component.translatable("gui.guhs.kapper.kapsel." + Kapsel.values()[wens].stijl());
            Component kleur = Component.translatable(wensVerf < 0 ? "gui.guhs.kapper.verf.naturel" : "gui.guhs.kapper.verf." + Haarverf.values()[wensVerf].kleur());
            g.centeredText(font, naam, left + 62, py1 - 19, 0xFF7A2848);
            g.centeredText(font, kleur, left + 62, py1 - 9, 0xFF7A2848);
        } else if (klant()) {
            g.centeredText(font, Component.literal("?").withStyle(ChatFormatting.BOLD), left + 62, py0 + 50, 0xFF7A2848);
            g.centeredText(font, Component.translatable("gui.guhs.kapper.knip.foto_weg"), left + 62, py1 - 14, 0xFF7A2848);
        }
        g.centeredText(font, Component.translatable("gui.guhs.kapper.knip.foto"), left + 62, py0 - 9 + 1, ROZE);
        // the customer in the chair (live)
        g.centeredText(font, Component.translatable("gui.guhs.kapper.knip.klant"), left + 178, py0 - 8, ROZE);
        Entity e = minecraft != null && minecraft.level != null ? minecraft.level.getEntity(data.getIntOr("Klant", 0)) : null;
        if (klant() && e instanceof LivingEntity living) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, left + 124, py0 + 4, left + 232, py1 - 4, 34, 0.0625f, mouseX, mouseY, living);
        } else {
            String fase = data.getStringOr("Fase", "");
            Component t = "TUSSEN".equals(fase) && !data.getStringOr("Uitslag", "").isEmpty()
                    ? Component.translatable(data.getStringOr("Uitslag", ""), data.getIntOr("UitslagPunten", 0), data.getIntOr("Combo", 0))
                    : Component.translatable("AFTELLEN".equals(fase) ? "gui.guhs.kapper.knip.aftellen" : "gui.guhs.kapper.knip.volgende");
            int y = py0 + 40;
            for (var line : font.split(t, 100)) {
                g.centeredText(font, line, left + 178, y, TEXT);
                y += 10;
            }
        }
        g.centeredText(font, Component.translatable("gui.guhs.kapper.knip.hint"), width / 2, top + H - 36, 0xFFB898C8);
    }

    private static int stapKleur(boolean klaar) {
        return klaar ? 0xFF7CE08A : 0xFFFFE6EE;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
