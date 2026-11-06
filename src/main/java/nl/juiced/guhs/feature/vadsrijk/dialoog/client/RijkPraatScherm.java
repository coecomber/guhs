package nl.juiced.guhs.feature.vadsrijk.dialoog.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.knuffeldal.client.PraatScherm;
import nl.juiced.guhs.feature.vadsrijk.dialoog.Emotie;
import nl.juiced.guhs.feature.vadsrijk.dialoog.RijkDialoog;
import nl.juiced.guhs.taal.Tekst;

/**
 * T-DIALOOG (roadmap §5.4): the Vadsrijk talking screen. The server marks a conversation "Rijk" (RijkDialoog); everything else in
 * the mod keeps the plain {@link PraatScherm}. Same data, same answers and the same -1 "read/closed" report (all inherited), but:
 * <ul>
 *   <li>the text types itself (click / space = all of it at once, again = next page; speed in the options), with a soft blip per
 *       word in the speaker's voice (guh, Mika, narrator, you, the dragon) and a volume slider behind the gear;</li>
 *   <li>a name plate in the speaker's faction colour with where they live; the frame in the same colour;</li>
 *   <li>the portrait shows a mood (it hops, droops, jumps or shakes, with a little sign);</li>
 *   <li>page dots, "« Terug" (and backspace) to page back, a log of everything said (L, the "≡" button);</li>
 *   <li>answers with an icon (a quest, a shop, travel, leave...) and their number key; "Wat moet ik doen?" at a quest NPC repeats
 *       the current objective (who/what/where), and a strip above the answers says it in short;</li>
 *   <li>the narrator gets a full-width parchment page in italics, your own lines have your portrait on the right;</li>
 *   <li>GUI scale safe: narrower windows shrink the panel, a low window puts the answers in two columns and lets the text scroll.</li>
 * </ul>
 */
public class RijkPraatScherm extends PraatScherm {
    /** Name plate / frame colour per faction (RijkDialoog.kenmerk). */
    static final Map<String, Integer> KLEUR = Map.ofEntries(Map.entry("z1", 0xFFFFB6D8), Map.entry("z2", 0xFF9ED8FF), Map.entry("z3", 0xFFFFC85A),
            Map.entry("z4", 0xFFFF9F6B), Map.entry("z5", 0xFFFFE89A), Map.entry("orde", 0xFFB49CFF), Map.entry("mika", 0xFFB8B8C4),
            Map.entry("draak", 0xFFFF7A9A), Map.entry("speler", 0xFF8EF0C0), Map.entry("verteller", 0xFFE8D5A8), Map.entry("wezen", 0xFFC8E6A0));
    private static final int MAX_W = 330, KOP = 21, REGEL = 10, KNOP = 22;
    private static final int PANEEL = 0xF0301A26, TEKST = 0xFF3A1C30;

    /** One line of the conversation log (kept for this session, newest last). */
    record LogRegel(Component naam, Component tekst, int kleur) {
    }

    static final ArrayDeque<LogRegel> LOG = new ArrayDeque<>();
    static final int LOG_MAX = 80;

    // layout (init)
    private int sw, sh, sx, sy, pic, bx, by, bw, bh, optY, rijen, onderY, stripY;
    private boolean kolommen, verteller, speler;
    private int zichtRegels;
    // the page shown
    @Nullable
    private CompoundTag getoondData;
    private int getoondPagina = -2;
    private boolean getoondDoel;
    private boolean doelOpen;
    @Nullable
    private CompoundTag doelPagina;
    private final Set<String> gelogd = new HashSet<>();
    private List<FormattedCharSequence> regels = List.of();
    private int[] lengtes = new int[0];
    private int[] tekens = new int[0];
    private int totaal;
    private long start;
    private int gemeld;
    private long laatsteBlip;
    private boolean typen;
    private int scroll;
    private Emotie emotie = Emotie.NEUTRAAL;
    private String stem = "guh";
    private float toon = 1.3f;
    private int kleur = 0xFFFFB6D8;
    // panels
    private boolean logOpen, instOpen;
    private int logScroll;

    public RijkPraatScherm(int npcId, CompoundTag data) {
        super(npcId, data);
    }

    @Override
    public boolean past(CompoundTag newData) {
        return newData.getBooleanOr("Rijk", false);
    }

    /** The page shown: the "Wat moet ik doen?" page while it is open, else the scene's page. */
    @Override
    protected CompoundTag nu() {
        return doelOpen && doelPagina != null ? doelPagina : super.nu();
    }

    private boolean opties() {
        return laatstePagina() || doelOpen;
    }

    // ---- layout + buttons ----------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        boolean nieuw = getoondData != data;
        if (nieuw) {
            getoondData = data;
            doelOpen = false;
            doelPagina = maakDoelPagina();
            gelogd.clear();
            logOpen = false;
        }
        boolean andere = nieuw || getoondPagina != pagina || getoondDoel != doelOpen;
        if (andere) {
            getoondPagina = pagina;
            getoondDoel = doelOpen;
            kenmerken();
            nl.juiced.guhs.feature.vadsrijk.geluid.client.StemSpeler.pagina(npcId, nieuw, stem, nu().getStringOr("Factie", "z1"), emotie.id(), tekst(nu()).getString());   // U-AUDIO2: NPC voice
        }
        layout();
        if (andere) {
            start = Util.getMillis();
            gemeld = 0;
            scroll = 0;
            typen = DialoogConfig.snelheid() > 0 && totaal > 0;
            log();
        }
        knoppen();
    }

    /** Voice, pitch, colour and mood of the page shown. */
    private void kenmerken() {
        CompoundTag pg = nu();
        stem = pg.getStringOr("Stem", "guh");
        toon = pg.getFloatOr("Toon", 1.3f);
        kleur = KLEUR.getOrDefault(pg.getStringOr("Factie", "z1"), 0xFFFFB6D8);
        Emotie e = Emotie.van(pg.getStringOr("Emotie", ""));
        emotie = e != null ? e : Emotie.raad(tekst(pg).getString());
    }

    private void layout() {
        CompoundTag pg = nu();
        speler = pg.getBooleanOr("Speler", false);
        verteller = !speler && portret() == null;
        sw = Math.min(MAX_W, width - 8);
        pic = height < 240 ? 54 : 70;
        int smal = sw - 16 - pic - 8;
        // the balloon is as tall as the longest page needs (every page alike: nothing jumps while reading)
        int nodig = 0;
        ListTag paginas = data.getListOrEmpty("Paginas");
        if (paginas.isEmpty()) {
            nodig = font.split(tekstVan(data), smal - 10).size();
        }
        for (Tag t : paginas) {
            nodig = Math.max(nodig, font.split(tekstVan((CompoundTag) t), smal - 10).size());
        }
        if (doelPagina != null) {
            nodig = Math.max(nodig, font.split(tekstVan(doelPagina), smal - 10).size());
        }
        int n = data.getListOrEmpty("Opties").size() + (doelPagina != null ? 1 : 0);
        int strip = doelPagina != null ? 13 : 0;
        int bhWens = Math.max(pic, nodig * REGEL + 18);
        kolommen = false;
        rijen = Math.max(1, n);
        int rest = 6 + strip + rijen * KNOP + 2 + KNOP + 6;
        if (KOP + bhWens + rest > height - 4 && n > 2) {
            kolommen = true;   // (a low window, GUI scale 3-4: the answers side by side)
            rijen = (n + 1) / 2;
            rest = 6 + strip + rijen * KNOP + 2 + KNOP + 6;
        }
        bh = Math.min(bhWens, Math.max(40, height - 4 - KOP - rest));
        if (bh < pic) {
            pic = Math.max(36, bh);
        }
        sh = KOP + bh + rest;
        sx = (width - sw) / 2;
        sy = Math.max(2, (height - sh) / 2);
        by = sy + KOP;
        if (verteller) {
            bx = sx + 8;
            bw = sw - 16;
        } else if (speler) {
            bx = sx + 8;
            bw = sw - 16 - pic - 8;
        } else {
            bx = sx + 8 + pic + 8;
            bw = sw - 16 - pic - 8;
        }
        stripY = by + bh + 5;
        optY = stripY + strip;
        onderY = sy + sh - KNOP - 4;
        zichtRegels = Math.max(1, (bh - 18) / REGEL);
        // the shown page's lines and letters (again on a resize: the width may differ)
        regels = font.split(tekstVan(pg), bw - 10);
        lengtes = new int[regels.size()];
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < regels.size(); i++) {
            int voor = l.size();
            regels.get(i).accept((pos, style, cp) -> {
                l.add(cp);
                return true;
            });
            lengtes[i] = l.size() - voor;
        }
        tekens = l.stream().mapToInt(Integer::intValue).toArray();
        totaal = tekens.length;
    }

    /** A page's text, in the narrator's italics where it is a narrator page. */
    private Component tekstVan(CompoundTag pg) {
        Component t = tekst(pg);
        boolean v = pg.getStringOr("Stem", "").equals("verteller") && !pg.getBooleanOr("Speler", false);
        return v ? Component.empty().append(t).withStyle(ChatFormatting.ITALIC) : t;
    }

    private void knoppen() {
        // the header: log + options
        addRenderableWidget(Button.builder(Component.literal("≡"), b -> {
            logOpen = !logOpen;
            instOpen = false;
            logScroll = 0;
            rebuildWidgets();
        }).bounds(sx + sw - 8 - 16 - 2 - 16, sy + 3, 16, 15).tooltip(Tooltip.create(Component.translatable("vadsrijk.dialoog.knop.log"))).build());
        addRenderableWidget(Button.builder(Component.literal("⚙"), b -> {
            instOpen = !instOpen;
            logOpen = false;
            rebuildWidgets();
        }).bounds(sx + sw - 8 - 16, sy + 3, 16, 15).tooltip(Tooltip.create(Component.translatable("vadsrijk.dialoog.knop.instellingen"))).build());
        if (logOpen) {
            addRenderableWidget(Button.builder(Component.translatable("vadsrijk.dialoog.knop.log_dicht"), b -> {
                logOpen = false;
                rebuildWidgets();
            }).bounds(sx + sw - 8 - 90, onderY, 90, 20).build());
            return;
        }
        // the bottom row: « Terug, Verder », Doei!
        if (pagina > 0 || doelOpen) {
            addRenderableWidget(Button.builder(Component.translatable("vadsrijk.dialoog.knop.terug"), b -> terug())
                    .bounds(sx + 8, onderY, 64, 20).build());
        }
        int doeiW = 70;
        if (!opties()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verhaal.verder"), b -> verder())
                    .bounds(sx + sw - 8 - doeiW - 4 - 80, onderY, 80, 20).build());
        }
        addRenderableWidget(new KeuzeKnop(sx + sw - 8 - doeiW, onderY, doeiW, Component.translatable("gui.guhs.knuffeldal.doei"),
                new ItemStack(Items.OAK_DOOR), 0xFF9A9A9A, 0, b -> onClose()));
        // the answers (or the options strip)
        int x = sx + 8, w = sw - 16;
        int kw = kolommen ? (w - 4) / 2 : w;
        if (instOpen) {
            int hw = (w - 4) / 2;
            addRenderableWidget(Button.builder(snelheidTekst(), b -> {
                DialoogConfig.zetSnelheid(DialoogConfig.snelheid() + 1);
                b.setMessage(snelheidTekst());
            }).bounds(x, optY, hw, 20).build());
            addRenderableWidget(new StemSchuif(x + hw + 4, optY, hw, 20));
            return;
        }
        if (!opties()) {
            return;
        }
        List<CompoundTag> lijst = new ArrayList<>();
        for (Tag t : data.getListOrEmpty("Opties")) {
            lijst.add((CompoundTag) t);
        }
        int i = 0;
        for (CompoundTag o : lijst) {
            int id = o.getIntOr("Id", 0);
            String key = o.getStringOr("Tekst", "");
            Icoon ic = icoon(key);
            int kx = kolommen ? x + (i % 2) * (kw + 4) : x, ky = optY + (kolommen ? i / 2 : i) * KNOP;
            addRenderableWidget(new KeuzeKnop(kx, ky, kw, Component.translatable(key), ic.item(), ic.kleur(), i + 1, b -> send(id)));
            i++;
        }
        if (doelPagina != null && !doelOpen) {
            int kx = kolommen ? x + (i % 2) * (kw + 4) : x, ky = optY + (kolommen ? i / 2 : i) * KNOP;
            addRenderableWidget(new KeuzeKnop(kx, ky, kw, Component.translatable("vadsrijk.dialoog.knop.wat_doen"), new ItemStack(Items.COMPASS),
                    0xFFFFD27A, i + 1, b -> {
                        doelOpen = true;
                        rebuildWidgets();
                    }));
        }
    }

    private static Component snelheidTekst() {
        return Component.translatable("vadsrijk.dialoog.snelheid", Component.translatable("vadsrijk.dialoog.snelheid." + DialoogConfig.snelheid()));
    }

    /** An answer's icon and stripe colour, from its text key. */
    record Icoon(ItemStack item, int kleur) {
    }

    static Icoon icoon(String key) {
        String soort = RijkDialoog.optieSoort(key);
        return switch (soort) {
            case "opdracht" -> new Icoon(new ItemStack(Items.WRITABLE_BOOK), 0xFFFFC83A);
            case "weg" -> new Icoon(new ItemStack(Items.OAK_DOOR), 0xFF9A9A9A);
            case "reis" -> new Icoon(new ItemStack(Items.FEATHER), 0xFF9ED8FF);
            case "reis_slot" -> new Icoon(new ItemStack(Items.FEATHER), 0xFF707070);
            case "kaart" -> new Icoon(new ItemStack(Items.FILLED_MAP), 0xFF9ED8FF);
            case "wapen" -> new Icoon(new ItemStack(Items.IRON_SWORD), 0xFFB49CFF);
            case "bank" -> new Icoon(new ItemStack(Items.CHEST), 0xFF68D88A);
            case "stal" -> new Icoon(new ItemStack(Items.SADDLE), 0xFF68D88A);
            case "smid" -> new Icoon(new ItemStack(Items.ANVIL), 0xFF68D88A);
            case "gondel" -> new Icoon(new ItemStack(Items.MINECART), 0xFF9ED8FF);
            case "orde" -> new Icoon(new ItemStack(Items.NETHER_STAR), 0xFFB49CFF);
            case "drinken" -> new Icoon(new ItemStack(Items.MILK_BUCKET), 0xFF68D88A);
            case "winkel" -> new Icoon(new ItemStack(Items.EMERALD), 0xFF68D88A);
            case "quiz" -> new Icoon(new ItemStack(Items.PAPER), 0xFFFFE6EE);
            default -> new Icoon(ItemStack.EMPTY, 0xFFFFB6D8);
        };
    }

    // ---- the "Wat moet ik doen?" page + the log ------------------------------------------------------------------------------

    /** The page the first NPC of the conversation says when you ask "Wat moet ik doen?" (null: no objective sent). */
    @Nullable
    private CompoundTag maakDoelPagina() {
        CompoundTag d = data.getCompoundOrEmpty("Doel");
        if (d.isEmpty()) {
            return null;
        }
        ListTag l = data.getListOrEmpty("Paginas");
        CompoundTag bron = l.isEmpty() ? data : null;
        for (Tag t : l) {
            CompoundTag pg = (CompoundTag) t;
            if (!pg.getBooleanOr("Speler", false) && (pg.getIntOr("Spreker", -1) >= 0 || pg.contains("Portret"))) {
                bron = pg;
                break;
            }
        }
        if (bron == null) {
            bron = l.getCompoundOrEmpty(0);
        }
        CompoundTag pg = new CompoundTag();
        for (String k : List.of("Spreker", "Portret", "Naam", "Factie", "Stem", "Toon", "Plek")) {
            Tag v = bron.get(k);
            if (v != null) {
                pg.put(k, v.copy());
            }
        }
        String soort = d.getStringOr("Soort", "actief");
        pg.putString("Tekst", "vadsrijk.dialoog.doel.zin." + soort);
        ListTag args = d.getListOrEmpty("Args").copy();
        CompoundTag extra = new CompoundTag();
        Tekst.put(extra, "A", d.contains("Extra") ? Component.translatable("vadsrijk.dialoog.doel.extra", Tekst.get(d.getCompoundOrEmpty("Extra"), "A"))
                : Component.empty());
        args.add(extra);
        pg.put("Args", args);
        return pg;
    }

    /** The short line above the answers: "Opdracht: wat, waar". */
    @Nullable
    private Component strip() {
        CompoundTag d = data.getCompoundOrEmpty("Doel");
        if (d.isEmpty()) {
            return null;
        }
        ListTag a = d.getListOrEmpty("Args");
        return Component.translatable("vadsrijk.dialoog.doel.strip." + d.getStringOr("Soort", "actief"), Tekst.get(a.getCompoundOrEmpty(1), "A"),
                Tekst.get(a.getCompoundOrEmpty(2), "A"));
    }

    private void log() {
        String k = pagina + (doelOpen ? "?" : "");
        if (!gelogd.add(k)) {
            return;
        }
        CompoundTag pg = nu();
        LOG.addLast(new LogRegel(naam(pg), tekst(pg), kleur));
        while (LOG.size() > LOG_MAX) {
            LOG.removeFirst();
        }
    }

    /** (tests/dev) */
    public static int logGrootte() {
        return LOG.size();
    }

    // ---- reading: type, next, back ---------------------------------------------------------------------------------------------

    private int zichtbaar() {
        if (!typen) {
            return totaal;
        }
        int cps = DialoogConfig.LETTERS_PER_SECONDE[Math.max(0, Math.min(3, DialoogConfig.snelheid()))];
        if (cps <= 0) {
            typen = false;
            return totaal;
        }
        int n = (int) Math.min(totaal, (Util.getMillis() - start) * cps / 1000L);
        if (n >= totaal) {
            typen = false;
        }
        return n;
    }

    /** Click / space: first all the text, then the next page. */
    private void verder() {
        if (typen) {
            typen = false;
            return;
        }
        if (scroll + zichtRegels < regels.size()) {
            scroll = Math.min(regels.size() - zichtRegels, scroll + zichtRegels);   // (more text below: show it first)
            return;
        }
        if (!opties()) {
            pagina++;
            rebuildWidgets();
        }
    }

    /** Shift+space: to the last page (the choices are never skipped). */
    private void naarEinde() {
        typen = false;
        if (!opties()) {
            pagina = Math.max(0, paginas() - 1);
            rebuildWidgets();
        }
    }

    private void terug() {
        if (doelOpen) {
            doelOpen = false;
        } else if (pagina > 0) {
            pagina--;
        } else {
            return;
        }
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        int n = zichtbaar();
        if (n > gemeld) {
            boolean woord = false;
            for (int i = gemeld; i < n && i < tekens.length; i++) {
                if (Character.isLetterOrDigit(tekens[i]) && (i == 0 || !Character.isLetterOrDigit(tekens[i - 1]))) {
                    woord = true;
                }
            }
            gemeld = n;
            if (woord) {
                blip();
            }
        }
        // keep the line being typed in view
        int regel = 0, som = 0;
        while (regel < lengtes.length && som + lengtes[regel] < n) {
            som += lengtes[regel++];
        }
        if (typen && regel >= scroll + zichtRegels) {
            scroll = regel - zichtRegels + 1;
        }
    }

    /** A soft blip in the speaker's voice (never more than ~14 a second). */
    private void blip() {
        float vol = DialoogConfig.stemmen();
        long nu = Util.getMillis();
        if (vol <= 0 || nu - laatsteBlip < 70 || minecraft == null) {
            return;
        }
        laatsteBlip = nu;
        SoundEvent ev;
        float hard;
        switch (stem) {
            case "mika" -> {
                ev = SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
                hard = 0.28f;
            }
            case "verteller" -> {
                ev = SoundEvents.NOTE_BLOCK_HARP.value();
                hard = 0.16f;
            }
            case "speler" -> {
                ev = SoundEvents.NOTE_BLOCK_PLING.value();
                hard = 0.14f;
            }
            case "draak" -> {
                ev = SoundEvents.NOTE_BLOCK_BASS.value();
                hard = 0.4f;
            }
            default -> {
                ev = SoundEvents.NOTE_BLOCK_BIT.value();
                hard = 0.2f;
            }
        }
        float pitch = toon * (0.94f + (float) Math.random() * 0.12f);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol * hard));
    }

    // ---- input ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (logOpen || event.button() != 0) {
            return false;
        }
        if (event.x() >= sx && event.x() < sx + sw && event.y() >= sy && event.y() < sy + sh) {
            verder();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (logOpen) {
            logScroll = Math.max(0, logScroll + (int) Math.signum(scrollY));
            return true;
        }
        if (regels.size() > zichtRegels) {
            scroll = Math.max(0, Math.min(regels.size() - zichtRegels, scroll - (int) Math.signum(scrollY)));
            typen = false;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (logOpen && (k == 256 || k == 76)) {   // (Esc or L closes the log, not the conversation)
            logOpen = false;
            rebuildWidgets();
            return true;
        }
        if (k == 32 || ((k == 257 || k == 335) && getFocused() == null)) {   // space / enter
            if (event.hasShiftDown()) {
                naarEinde();
            } else {
                verder();
            }
            return true;
        }
        if (k == 259 || k == 263) {   // backspace / left
            terug();
            return true;
        }
        if (k == 262 && !opties()) {   // right
            verder();
            return true;
        }
        if (k == 76) {   // L: the log
            logOpen = !logOpen;
            logScroll = 0;
            rebuildWidgets();
            return true;
        }
        if (k >= 49 && k <= 57 && opties() && !logOpen && !instOpen) {   // 1-9: that answer
            int n = k - 49;
            for (var c : children()) {
                if (c instanceof KeuzeKnop kk && kk.nummer == n + 1) {
                    kk.onPress(event);
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        schermAchtergrond(g, mouseX, mouseY, partialTick);
        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, kleur);
        g.fill(sx, sy, sx + sw, sy + sh, PANEEL);
        if (logOpen) {
            tekenLog(g);
            return;
        }
        CompoundTag pg = nu();
        // the name plate
        Component naam = naam(pg).copy().withStyle(ChatFormatting.BOLD);
        int nw = font.width(naam) + 10;
        g.fill(sx + 8, sy + 4, sx + 8 + nw, sy + 17, kleur);
        g.text(font, naam, sx + 13, sy + 7, 0xFF2A1420, false);
        Component label = label(pg);
        if (label != null) {
            int ruimte = sw - 16 - nw - 4 - 40;
            if (ruimte > 20) {
                g.text(font, font.plainSubstrByWidth(label.getString(), ruimte), sx + 8 + nw + 5, sy + 7, (kleur & 0x00FFFFFF) | 0xC0000000, false);
            }
        }
        // the portrait
        LivingEntity portret = verteller ? null : portret();
        int px = speler ? sx + sw - 8 - pic : sx + 8, py = by;
        if (!verteller) {
            g.fill(px - 1, py - 1, px + pic + 1, py + pic + 1, (kleur & 0x00FFFFFF) | 0xA0000000);
            g.fill(px, py, px + pic, py + pic, (kleur & 0x00FFFFFF) | 0x30000000);
            if (portret != null) {
                tekenPortret(g, portret, px, py, px + pic, py + pic, mouseX, mouseY, partialTick);
            }
        }
        // the balloon
        int vul = verteller ? 0xFFF3E7C9 : speler ? 0xFFE6FFF4 : 0xFFFFF4F8;
        g.fill(bx, by, bx + bw, by + bh, vul);
        if (!verteller) {
            if (speler) {
                g.fill(bx + bw, by + 12, bx + bw + 4, by + 18, vul);
            } else {
                g.fill(bx - 4, by + 12, bx, by + 18, vul);
            }
        } else {
            // (the narrator: a little book in the corner of the page)
            g.pose().pushMatrix();
            g.pose().translate(bx + bw - 14, by + bh - 14);
            g.pose().scale(0.75f, 0.75f);
            g.item(new ItemStack(Items.WRITABLE_BOOK), 0, 0);
            g.pose().popMatrix();
        }
        int over = zichtbaar();
        int kleurTekst = verteller ? 0xFF4A3520 : TEKST;
        for (int i = 0; i < regels.size(); i++) {
            int len = lengtes[i];
            if (i >= scroll && i < scroll + zichtRegels && over > 0) {
                g.text(font, kort(regels.get(i), Math.min(len, over)), bx + 5, by + 5 + (i - scroll) * REGEL, kleurTekst, false);
            }
            over -= len;
        }
        // page dots / more text / continue sign
        int onder = by + bh - 10;
        tekenStipjes(g, bx + 5, onder);
        boolean meer = scroll + zichtRegels < regels.size();
        if (!typen && (meer || !opties()) && (Util.getMillis() / 400) % 2 == 0) {
            g.text(font, meer ? "▼" : "»", bx + bw - (verteller ? 20 : 9), onder, 0xFFB08AA0, false);
        }
        // the objective strip
        Component strip = strip();
        if (strip != null && opties()) {
            Component kop = Component.translatable("vadsrijk.dialoog.doel.kop." + data.getCompoundOrEmpty("Doel").getStringOr("Soort", "actief"))
                    .withStyle(ChatFormatting.BOLD);
            int kw = font.width(kop);
            g.text(font, kop, sx + 8, stripY, 0xFFFFD27A, false);
            String s = strip.getString();
            g.text(font, font.plainSubstrByWidth(s, sw - 16 - kw - 4), sx + 8 + kw + 4, stripY, 0xFFFFE6EE, false);
            if (mouseY >= stripY - 1 && mouseY < stripY + 10 && mouseX >= sx && mouseX < sx + sw) {
                g.setTooltipForNextFrame(font, font.split(strip, 220), mouseX, mouseY);
            }
        }
        if (!opties() && !instOpen) {
            g.text(font, Component.translatable("vadsrijk.dialoog.hint"), sx + 8, optY + 6, 0xFF9A7A8E, false);
        }
        if (minecraft.options.advancedItemTooltips) {
            // (q-dialoog dev aid, F3+H, + the T-DIALOOG tags)
            String info = data.getStringOr("Sleutel", "-") + " | " + pg.getStringOr("Tekst", "") + " | " + pg.getStringOr("Factie", "?") + "/"
                    + stem + "/" + emotie.id() + (pg.contains("Emotie") ? "!" : "~") + " | " + (portret == null ? "geen" : portret == minecraft.player ? "speler"
                    : STAND_INS.containsValue(portret) ? "stand-in" : "entity");
            g.text(font, font.plainSubstrByWidth(info, Math.max(sw, 300)), sx, sy + sh + 3, 0xFFB0B0B0, true);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (logOpen || verteller || emotie == Emotie.NEUTRAAL) {
            return;
        }
        // the mood sign, on top of the portrait's corner
        g.nextStratum();
        int px = speler ? sx + sw - 8 - pic : sx + 8;
        int x = speler ? px - 4 : px + pic - 9, y = by - 4;
        String teken;
        int col;
        switch (emotie) {
            case BLIJ -> {
                teken = "♥";
                col = 0xFFFF6FAE;
            }
            case VERDRIETIG -> {
                teken = "…";
                col = 0xFF5A8FD8;
            }
            case VERRAST -> {
                teken = "!";
                col = 0xFFE0A000;
            }
            default -> {
                teken = "╬";
                col = 0xFFE04040;
            }
        }
        float t = (Util.getMillis() - start) / 1000f;
        int hop = t < 0.3f ? (int) Math.round(Math.sin(t / 0.3f * Math.PI) * -3) : 0;
        g.fill(x, y + hop, x + 13, y + 12 + hop, 0xFFFFFFFF);
        g.outline(x, y + hop, 13, 12, col);
        g.centeredText(font, teken, x + 7, y + 2 + hop, col);
    }

    /** The dots under the text: one per page (the current one in the speaker's colour), or "3/15" for long scenes. */
    private void tekenStipjes(GuiGraphicsExtractor g, int x, int y) {
        int n = paginas();
        if (doelOpen) {
            g.text(font, "?", x, y, 0xFFB08AA0, false);
            return;
        }
        if (n <= 1) {
            return;
        }
        if (n > 12) {
            g.text(font, (Math.min(pagina, n - 1) + 1) + "/" + n, x, y, 0xFFB08AA0, false);
            return;
        }
        for (int i = 0; i < n; i++) {
            if (i == pagina) {
                g.fill(x + i * 7 - 1, y + 1, x + i * 7 + 6, y + 8, 0xFF3A1C30);
                g.fill(x + i * 7, y + 2, x + i * 7 + 5, y + 7, kleur);
            } else {
                g.fill(x + i * 7, y + 2, x + i * 7 + 5, y + 7, i < pagina ? 0xFFC8A8B8 : 0xFFE8D8E0);
            }
        }
    }

    private void tekenLog(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("vadsrijk.dialoog.log.kop").withStyle(ChatFormatting.BOLD), sx + 8, sy + 7, 0xFFFFB6D8, false);
        int top = sy + KOP, bodem = onderY - 4, w = sw - 16;
        List<FormattedCharSequence> alle = new ArrayList<>();
        List<Integer> kleuren = new ArrayList<>();
        for (LogRegel r : LOG) {
            Component regel = Component.empty().append(r.naam().copy().withStyle(ChatFormatting.BOLD)).append(": ").append(r.tekst());
            for (FormattedCharSequence s : font.split(regel, w)) {
                alle.add(s);
                kleuren.add(r.kleur());
            }
            alle.add(FormattedCharSequence.EMPTY);
            kleuren.add(0);
        }
        if (alle.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.dialoog.log.leeg"), sx + 8, top + 4, 0xFF9A7A8E, false);
            return;
        }
        int pas = Math.max(1, (bodem - top) / REGEL);
        logScroll = Math.min(logScroll, Math.max(0, alle.size() - pas));
        int eind = alle.size() - logScroll;
        int begin = Math.max(0, eind - pas);
        g.enableScissor(sx + 4, top, sx + sw - 4, bodem);
        for (int i = begin; i < eind; i++) {
            int y = top + (i - begin) * REGEL;
            if (kleuren.get(i) != 0) {
                g.fill(sx + 5, y, sx + 7, y + 9, kleuren.get(i));
            }
            g.text(font, alle.get(i), sx + 9, y, 0xFFFFE6EE, false);
        }
        g.disableScissor();
        if (begin > 0) {
            g.text(font, "▲", sx + sw - 16, top, 0xFF9A7A8E, false);
        }
    }

    /** The name above a page: its name key, else the speaker's own name. */
    private Component naam(CompoundTag pg) {
        String naamKey = pg.getStringOr("Naam", "");
        if (!naamKey.isEmpty()) {
            return Component.translatable(naamKey);
        }
        Entity e = spreker(pg);
        return e != null ? e.getDisplayName() : title;
    }

    /** The small line after the name: the faction (Knuffelorde, Mika...) or where the speaker lives. */
    @Nullable
    private Component label(CompoundTag pg) {
        String f = pg.getStringOr("Factie", "");
        if (f.equals("orde") || f.equals("mika") || f.equals("draak")) {
            return Component.translatable("vadsrijk.dialoog.factie." + f);
        }
        if (pg.contains("Plek")) {
            return Component.translatable(pg.getStringOr("Plek", ""));
        }
        if (f.matches("z[2-9]")) {
            return Component.translatable("vadsrijk.dialoog.factie." + f);
        }
        return null;
    }

    @Nullable
    private Entity spreker(CompoundTag pg) {
        int id = pg.contains("Spreker") ? pg.getIntOr("Spreker", -1) : npcId;
        return minecraft == null || minecraft.level == null || id < 0 ? null : minecraft.level.getEntity(id);
    }

    /**
     * The portrait (q-dialoog's framing) with the page's mood: happy hops, sad sinks and looks down, surprised jumps and pops a
     * little bigger, angry shakes now and then.
     */
    private void tekenPortret(GuiGraphicsExtractor g, LivingEntity living, int x0, int y0, int x1, int y1, int mouseX, int mouseY, float pt) {
        float tijd = ((minecraft.level == null ? 0 : minecraft.level.getGameTime()) + pt) / 20f;
        float t = (Util.getMillis() - start) / 1000f;
        int dx = 0, dy = 0;
        float groei = 1f, kijk = 0f;
        switch (emotie) {
            case BLIJ -> dy = -(int) Math.round(Math.abs(Math.sin(tijd * 4.5f)) * 3);
            case VERDRIETIG -> {
                dy = 3;
                kijk = -0.3f;
            }
            case VERRAST -> {
                if (t < 0.5f) {
                    dy = -(int) Math.round(Math.sin(t / 0.5f * Math.PI) * 6);
                }
                groei = 1f + 0.12f * Math.max(0f, 1f - t / 0.6f);
            }
            case BOOS -> {
                if (t < 0.7f || tijd % 2.5f < 0.3f) {
                    dx = (int) Math.round(Math.sin(tijd * 60f) * 1.6f);
                }
            }
            default -> {
            }
        }
        g.enableScissor(x0, y0, x1, y1);
        if (living instanceof nl.juiced.guhs.entity.GuhNpcEntity g2) {
            int kijkY = mouseY;
            if (nl.juiced.guhs.client.SittingGuhRenderers.kopOpDeGrond(g2.getKind())) {
                int midden = (y0 + y1) / 2;
                kijkY = Math.max(midden - 30, Math.min(midden + 6, mouseY));
            }
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0 + dx, y0 + dy, x1 + dx, y1 + dy, Math.round(26 * groei * (y1 - y0) / 70f), 0.0625f,
                    mouseX, kijkY, living);
            g.disableScissor();
            return;
        }
        float hoog = living.getBbHeight() / Math.max(0.01f, living.getScale());
        float breed = living.getBbWidth() / Math.max(0.01f, living.getScale());
        float vak = y1 - y0;
        float midden, span;
        if (living instanceof nl.juiced.guhs.entity.GuhEntity) {
            midden = 0.62f;
            span = 1.65f;
        } else if (hoog > 1.2f && breed < hoog * 0.7f) {
            midden = hoog * 0.68f;
            span = hoog * 0.68f;
        } else {
            midden = hoog * 0.5f;
            span = Math.max(hoog, breed) * 1.35f;
        }
        int maat = Math.max(8, Math.round(vak / span * groei));
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f;
        float xHoek = clamp((float) Math.atan((cx - mouseX) / 40f), 0.55f) + (float) Math.sin(tijd * 0.9f) * 0.08f;
        float yHoek = clamp((float) Math.atan((cy - mouseY) / 40f), 0.35f) + (float) Math.sin(tijd * 0.6f + 1f) * 0.03f + kijk;
        if (speler) {
            xHoek = -xHoek;   // (your own portrait stands on the right: it looks toward the speaker side)
        }
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x0 + dx, y0 + dy, x1 + dx, y1 + dy, maat, midden - hoog / 2f, xHoek, yHoek, living);
        g.disableScissor();
    }

    private static float clamp(float v, float max) {
        return Math.max(-max, Math.min(max, v));
    }

    /** The first {@code n} letters of a line. */
    static FormattedCharSequence kort(FormattedCharSequence s, int n) {
        return sink -> {
            int[] i = {0};
            return s.accept((pos, style, cp) -> i[0]++ < n && sink.accept(pos, style, cp));
        };
    }

    // ---- widgets ---------------------------------------------------------------------------------------------------------------

    /** An answer: a stripe in its colour, its icon, its text (left), its number key. */
    final class KeuzeKnop extends AbstractButton {
        final int nummer;
        private final ItemStack icoon;
        private final int streep;
        private final java.util.function.Consumer<KeuzeKnop> actie;

        KeuzeKnop(int x, int y, int w, Component tekst, ItemStack icoon, int streep, int nummer, java.util.function.Consumer<KeuzeKnop> actie) {
            super(x, y, w, 20, tekst);
            this.icoon = icoon;
            this.streep = streep;
            this.nummer = nummer;
            this.actie = actie;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            actie.accept(this);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            extractDefaultSprite(g);
            g.fill(getX() + 1, getY() + 1, getX() + 3, getY() + getHeight() - 1, streep);
            int tx = getX() + 6;
            if (!icoon.isEmpty()) {
                g.item(icoon, getX() + 5, getY() + 2);
                tx = getX() + 24;
            }
            String nr = nummer > 0 && nummer <= 9 ? String.valueOf(nummer) : "";
            int ruimte = getWidth() - (tx - getX()) - 4 - (nr.isEmpty() ? 0 : font.width(nr) + 4);
            boolean slot = (streep & 0x00FFFFFF) == 0x707070;
            g.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(), ruimte)), tx, getY() + 6,
                    slot ? 0xFFA0A0A0 : isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFFFFFFF, true);
            if (!nr.isEmpty()) {
                g.text(font, nr, getX() + getWidth() - 4 - font.width(nr), getY() + 6, 0xFF8A8A8A, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** The voice volume (0 = no blips), with a sample blip when it changes. */
    final class StemSchuif extends AbstractSliderButton {
        StemSchuif(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), DialoogConfig.stemmen());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = (int) Math.round(value * 100);
            setMessage(v == 0 ? Component.translatable("vadsrijk.dialoog.stemmen.uit") : Component.translatable("vadsrijk.dialoog.stemmen", v));
        }

        @Override
        protected void applyValue() {
            DialoogConfig.zetStemmen((int) Math.round(value * 100));
            laatsteBlip = 0;
            blip();
        }
    }
}
