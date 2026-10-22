package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads;
import nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads.Actie;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * The control panel of a ghost (design 2.1, ronde 29): move it with arrows (forward / back / left / right from where you
 * look, higher / lower), turn it a quarter, mirror it, see what is missing, "Bouw!" (and "alsnog bouwen?" when something
 * is missing: the rest stays a ghost), stop, undo, take it away, and add a friend who may help. Someone else's ghost: look
 * only.
 */
public class SpookScherm extends Screen {
    private static final int W = 340, H = 270;
    private static final int PANEEL = 0xF0FFF6FA, RAND = 0xFFE58CB8, KOP = 0xFFFBE0EE, DONKER = 0xFF5A2C44, TEKST = 0xFF6A4058,
            LICHT = 0xFFA07A90, GROEN = 0xFF4E8A3A, ROOD = 0xFFB0484A;

    private CompoundTag data;
    private final UUID id;
    private int left, top, tik;
    private EditBox vriend;

    public SpookScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.bouwhulp.spook.titel"));
        this.data = data;
        this.id = data.read("Id", UUIDUtil.CODEC).orElse(new UUID(0, 0));
    }

    public UUID id() {
        return id;
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        if (vriend != null && vriend.isFocused()) {
            return;
        }
        rebuildWidgets();
    }

    private boolean mag() {
        return data.getBooleanOr("Mag", false);
    }

    private int status() {
        return data.getIntOr("Status", 0);
    }

    private void doe(Actie a, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new SpookPayloads.Bedien(id, a.ordinal(), waarde, tekst));
    }

    /** An arrow: a direction seen from where the player looks. */
    private void schuif(int stap) {
        var p = minecraft == null ? null : minecraft.player;
        Direction kijk = p == null ? Direction.NORTH : p.getDirection();
        Direction d = switch (stap) {
            case 0 -> kijk;
            case 1 -> kijk.getOpposite();
            case 2 -> kijk.getCounterClockWise();
            case 3 -> kijk.getClockWise();
            case 4 -> Direction.UP;
            default -> Direction.DOWN;
        };
        doe(Actie.SCHUIF, d.ordinal(), "");
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean mag = mag();
        boolean bouwt = status() == 1;
        boolean vrij = mag && !bouwt;
        int y = top + 92;
        // arrows: a little cross, plus up / down
        int ax = left + 10;
        knop(ax + 22, y, 20, "↑", b -> schuif(0), vrij, "gui.guhs.bouwhulp.spook.vooruit");
        knop(ax, y + 20, 20, "←", b -> schuif(2), vrij, "gui.guhs.bouwhulp.spook.links");
        knop(ax + 44, y + 20, 20, "→", b -> schuif(3), vrij, "gui.guhs.bouwhulp.spook.rechts");
        knop(ax + 22, y + 40, 20, "↓", b -> schuif(1), vrij, "gui.guhs.bouwhulp.spook.achteruit");
        knop(ax + 72, y, 20, "▲", b -> schuif(4), vrij, "gui.guhs.bouwhulp.spook.hoger");
        knop(ax + 72, y + 40, 20, "▼", b -> schuif(5), vrij, "gui.guhs.bouwhulp.spook.lager");
        Button draai = Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.draai"), b -> doe(Actie.DRAAI, 0, ""))
                .bounds(ax + 100, y, 80, 18).build();
        Button spiegel = Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.spiegel"), b -> doe(Actie.SPIEGEL, 0, ""))
                .bounds(ax + 100, y + 22, 80, 18).build();
        draai.active = spiegel.active = vrij;
        addRenderableWidget(draai);
        addRenderableWidget(spiegel);
        // test round 5: the missing blocks as a written book
        Button lijstje = Button.builder(Component.translatable("gui.guhs.bouwhulp.lijstje.knop"), b -> doe(Actie.LIJSTJE, 0, ""))
                .bounds(left + 196, top + 200, W - 206, 18).tooltip(Tooltip.create(Component.translatable(data.getListOrEmpty("Tekort").isEmpty()
                        ? "gui.guhs.bouwhulp.lijstje.niets.uitleg" : "gui.guhs.bouwhulp.lijstje.uitleg"))).build();
        lijstje.active = !data.getListOrEmpty("Tekort").isEmpty();
        addRenderableWidget(lijstje);
        // building
        int by = top + H - 46;
        if (data.getBooleanOr("VraagAlsnog", false) && mag) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.alsnog.ja"), b -> doe(Actie.ALSNOG, 0, ""))
                    .bounds(left + 10, by, 136, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.alsnog.nee"), b -> {
                data.putBoolean("VraagAlsnog", false);
                rebuildWidgets();
            }).bounds(left + 152, by, 136, 18).build());
        } else {
            Button bouw = bouwt
                    ? Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.pauze"), b -> doe(Actie.PAUZE, 0, "")).bounds(left + 10, by, 90, 18).build()
                    : Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.bouw").withStyle(ChatFormatting.BOLD), b -> doe(Actie.BOUW, 0, ""))
                            .bounds(left + 10, by, 90, 18).build();
            bouw.active = mag;
            addRenderableWidget(bouw);
            Button terug = Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.terug", data.getIntOr("Ongedaan", 0)), b -> doe(Actie.TERUG, 0, ""))
                    .bounds(left + 104, by, 90, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.terug.uitleg"))).build();
            terug.active = data.getIntOr("Ongedaan", 0) > 0;
            addRenderableWidget(terug);
            Button weg = Button.builder(Component.translatable("gui.guhs.bouwhulp.spook.weg"), b -> doe(Actie.ANNULEER, 0, ""))
                    .bounds(left + 198, by, 90, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.weg.uitleg"))).build();
            weg.active = mag;
            addRenderableWidget(weg);
        }
        // a friend who may help
        int vy = top + H - 24;
        vriend = new EditBox(font, left + 10, vy, 96, 16, Component.translatable("gui.guhs.bouwhulp.spook.vriend"));
        vriend.setMaxLength(16);
        vriend.setHint(Component.translatable("gui.guhs.bouwhulp.spook.vriend").withStyle(ChatFormatting.GRAY));
        vriend.setEditable(mag);
        addRenderableWidget(vriend);
        Button erbij = Button.builder(Component.literal("+"), b -> doe(Actie.TOEVOEGEN, 0, vriend.getValue())).bounds(left + 108, vy, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.vriend.erbij"))).build();
        Button eraf = Button.builder(Component.literal("−"), b -> doe(Actie.WEGHALEN, 0, vriend.getValue())).bounds(left + 128, vy, 18, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.vriend.eraf"))).build();
        erbij.active = eraf.active = mag && data.getBooleanOr("Mag", false);
        addRenderableWidget(erbij);
        addRenderableWidget(eraf);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwkeet.sluit"), b -> onClose()).bounds(left + W - 58, vy - 1, 48, 18).build());
    }

    private void knop(int x, int y, int w, String tekst, Button.OnPress actie, boolean actief, String uitleg) {
        Button b = Button.builder(Component.literal(tekst), actie).bounds(x, y, w, 18).tooltip(Tooltip.create(Component.translatable(uitleg))).build();
        b.active = actief;
        addRenderableWidget(b);
    }

    @Override
    public void tick() {
        super.tick();
        if (status() == 1 && ++tik % 40 == 0) {
            doe(Actie.VERVERS, 0, "");
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.fill(left, top + 18, left + W, top + 19, RAND);
        String naam = data.getStringOr("Naam", "");
        Component kop = naam.isEmpty() ? title : Component.literal(naam);
        g.text(font, kop.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        Component van = Component.translatable("gui.guhs.bouwhulp.spook.van", data.getStringOr("Eigenaar", "?"));
        if (!mag()) {
            van = van.copy().append(" ").append(Component.translatable("gui.guhs.bouwhulp.spook.alleen_kijken").withStyle(ChatFormatting.ITALIC));
        }
        GidsTekst.passend(g, van, left + W - 8, top + 5, 150, 0.75f, LICHT, true);
        int[] gr = data.getIntArray("Grootte").orElse(new int[] {0, 0, 0});
        int x = left + 10, y = top + 24;
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.maat", gr[0], gr[1], gr[2], data.getIntOr("Blokken", 0)), x, y, W - 20,
                0.875f, TEKST, false);
        Component st = Component.translatable("gui.guhs.bouwhulp.spook.status." + status(), data.getIntOr("Gedaan", 0), data.getIntOr("Rest", 0),
                data.getIntOr("Mist", 0));
        GidsTekst.passend(g, st, x, y + 12, W - 20, 0.875f, status() == 1 ? GROEN : TEKST, false);
        int tot = data.getIntOr("Gedaan", 0) + data.getIntOr("Rest", 0);
        if (status() == 1 && tot > 0) {
            GidsTekst.balk(g, x, y + 26, W - 20, 5, data.getIntOr("Gedaan", 0) / (float) tot);
        }
        // test round 5: part of it out of the keet's reach (drawn red in the world)
        int buiten = data.getIntOr("Buiten", 0);
        if (buiten > 0) {
            Component tip = data.contains("BereikVolgend") ? Component.translatable("gui.guhs.bouwhulp.bereik.tip.volgend", data.getIntOr("BereikVolgend", 0))
                    : Component.translatable("gui.guhs.bouwhulp.bereik.tip.max");
            GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwhulp.bereik.buiten", buiten, data.getIntOr("Bereik", 0)).append(" ").append(tip),
                    x, y + 34, W - 20, 0.7f, ROOD);
        }
        // the shortage (or "alles is er"): the item, how many, its name
        int lx = left + 196, ly = top + 92;
        ListTag tekort = data.getListOrEmpty("Tekort");
        if (data.getBooleanOr("VraagAlsnog", false)) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwhulp.spook.alsnog.vraag"), left + 10, top + 156, 180, 0.75f, ROOD);
        }
        if (tekort.isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwhulp.spook.alles_er"), lx, ly, W - 206, 0.75f, GROEN);
        } else {
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.tekort").withStyle(ChatFormatting.BOLD), lx, ly, W - 206, 0.75f, ROOD, false);
            for (int i = 0; i < Math.min(5, tekort.size()); i++) {
                CompoundTag c = tekort.getCompoundOrEmpty(i);
                int iy = ly + 10 + i * 17;
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(c.getStringOr("Id", "minecraft:air")));
                if (item.isPresent()) {
                    ItemStack s = new ItemStack(item.get());
                    g.item(s, lx, iy);
                    GidsTekst.passend(g, Component.literal(c.getIntOr("N", 0) + "× ").withStyle(ChatFormatting.BOLD).append(s.getHoverName().copy()
                            .withStyle(ChatFormatting.RESET)), lx + 18, iy + 5, W - 226, 0.7f, ROOD, false);
                }
            }
            if (tekort.size() > 5) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.meer", tekort.size() - 5), lx, ly + 10 + 5 * 17, W - 206, 0.7f, LICHT, false);
            }
        }
        ListTag helpers = data.getListOrEmpty("Helpers");
        if (!helpers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < helpers.size(); i++) {
                sb.append(i == 0 ? "" : ", ").append(helpers.getStringOr(i, ""));
            }
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.helpers", sb.toString()), left + 150, top + H - 20, 90, 0.6f, LICHT, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
