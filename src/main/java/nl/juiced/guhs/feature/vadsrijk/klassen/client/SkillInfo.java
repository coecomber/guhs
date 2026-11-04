package nl.juiced.guhs.feature.vadsrijk.klassen.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Knuffelsprong;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;

/**
 * (fix round 2, g-ui) The skill tooltip: name, key, energy/ult + cooldown, the base explanation, then every tree node and
 * advancement step that improves this skill: learned ones green with a tick, the rest dark grey. Used by the skill list in
 * {@link BoomScherm}, by the tree nodes ("Verbetert: Kussenklap") and by the HUD skill slots while the inventory is open
 * (the HUD shows through it; the slot positions come from {@link RijkHud#skillVak}).
 * <p>Only visuals/text: which node improves which skill is a fixed table here ({@link #VERBETERINGEN}, from the node texts);
 * the class logic itself stays in the klassen code.
 */
public final class SkillInfo {
    /** Skill id -> the node ids (tree + advancement steps) that change it, in tree order. */
    static final Map<String, List<String>> VERBETERINGEN = new LinkedHashMap<>();
    /** Node id -> the skill ids it improves (the reverse of {@link #VERBETERINGEN}). */
    private static final Map<String, List<String>> VOOR = new HashMap<>();

    static {
        // Knuffelridder
        v("kussenklap", "r_vuurkussen", "r_slijmkussen", "r_kussenstorm", "r_ijsklap", "r_adv1", "r_adv3");
        v("stormsprong", "r_aanloop", "r_schokgolf", "r_galmstoot");
        v("knuffelschild", "r_doornknuffel", "r_beschermknuffel", "r_bolwerk", "r_adv3");
        v("vahoege_plof", "r_reuzenkussen");
        // Pluisjager
        v("pluizenregen", "j_vuurregen", "j_kleefregen", "j_splinterregen");
        v("rolsalto", "j_plakbom", "j_pluizenmantel", "j_windloper", "j_adv1");
        v("knuffelpijl", "j_hartjespijl");
        v("sterrenschot", "j_vallende_ster", "j_adv3");
        // Vadsmagier
        v("roze_vuurbal", "m_reuzenbal", "m_drievuur", "m_tijdsprong", "m_adv1", "m_adv3");
        v("blink", "m_glitterblink", "m_tijdsprong");
        v("vadsbron", "m_grote_bron", "m_bronschild");
        v("meteoorguh", "m_meteoorregen", "m_ijsnova");
    }

    private SkillInfo() {
    }

    private static void v(String skill, String... knopen) {
        VERBETERINGEN.put(skill, List.of(knopen));
        for (String k : knopen) {
            VOOR.computeIfAbsent(k, x -> new ArrayList<>()).add(skill);
        }
    }

    /** The node ids of the current class as the tree screen last received them (null = not opened yet this session). */
    @Nullable
    private static volatile Set<String> bekend;

    /** {@link BoomScherm} passes its node list here (every time it gets new data). */
    static void bekend(Set<String> knopen) {
        bekend = Set.copyOf(knopen);
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> bekend = null);
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Foreground e) -> {
            if (!(e.getScreen() instanceof InventoryScreen) || !RijkHud.skillsZichtbaar()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
            for (int i = 0; i < 5; i++) {
                int[] r = RijkHud.skillVak(i, w, h);
                if (e.getMouseX() >= r[0] && e.getMouseX() < r[0] + r[2] && e.getMouseY() >= r[1] && e.getMouseY() < r[1] + r[2]) {
                    String id = RijkHud.skillId(i);
                    if (id == null || mc.player == null) {
                        return;
                    }
                    // the client has no copy of the bought nodes (SpelerData is not synced): use what the tree screen last showed
                    Set<String> b = bekend;
                    List<Component> tip = tip(id, i, mc.font, knoop -> b != null && b.contains(knoop));
                    if (b == null && i < 4) {
                        tip.add(Component.translatable("vadsrijk.klassen.skilltip.open_boom").withStyle(ChatFormatting.DARK_AQUA));
                    }
                    e.getGuiGraphics().setComponentTooltipForNextFrame(mc.font, tip, e.getMouseX(), e.getMouseY());
                    return;
                }
            }
        });
    }

    /** The skill with this id in any class (and its class), or null. */
    @Nullable
    static Object[] zoek(String id) {
        for (Klasse k : Klassen.alle()) {
            for (Vaardigheid v : k.vaardigheden()) {
                if (v.id().equals(id)) {
                    return new Object[] {k, v};
                }
            }
        }
        return null;
    }

    /**
     * The full tooltip of skill {@code id} (slot 0..3 = R/V/G/Z, 4 = the Knuffelsprong). {@code heeft} says which nodes the
     * player learned (the tree screen passes its own data, the HUD the client player data).
     */
    public static List<Component> tip(String id, int slot, Font font, Predicate<String> heeft) {
        List<Component> l = new ArrayList<>();
        String[] toetsen = {"R", "V", "G", "Z", "C"};
        boolean sprong = Knuffelsprong.ID.equals(id);
        Object[] gevonden = sprong ? null : zoek(id);
        Klasse k = gevonden == null ? null : (Klasse) gevonden[0];
        Vaardigheid v = gevonden == null ? null : (Vaardigheid) gevonden[1];
        String naamKey = v != null ? v.naamKey() : sprong ? "vadsrijk.kern.skill.knuffelsprong" : "vadsrijk.klassen.skill." + id;
        l.add(Component.translatable(naamKey).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("  [" + toetsen[Math.max(0, Math.min(4, slot))] + "]").withStyle(ChatFormatting.GRAY)));
        int cd = v != null ? v.cooldownTicks() : sprong ? Knuffelsprong.COOLDOWN : 0;
        String sec = String.format(Locale.ROOT, cd % 20 == 0 ? "%.0f" : "%.1f", cd / 20.0).replace('.', ',');
        Component kost = v != null && v.ult() ? Component.translatable("vadsrijk.klassen.skilltip.ult").withStyle(ChatFormatting.GOLD)
                : v != null ? Component.translatable("vadsrijk.klassen.skilltip.energie", String.format(Locale.ROOT, "%.0f", v.energie()))
                        .withStyle(ChatFormatting.AQUA)
                : Component.translatable("vadsrijk.klassen.skilltip.gratis").withStyle(ChatFormatting.AQUA);
        l.add(kost.copy().append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("vadsrijk.klassen.skilltip.cooldown", sec).withStyle(ChatFormatting.YELLOW)));
        l.addAll(regels(font, "vadsrijk.klassen.skilluitleg." + id, Style.EMPTY.withColor(ChatFormatting.WHITE)));
        List<String> knopen = VERBETERINGEN.getOrDefault(id, List.of());
        if (sprong) {
            return l;
        }
        List<String> echt = new ArrayList<>();
        for (String n : knopen) {
            if (k == null || Boom.knoop(k, n) != null || n.contains("_adv")) {
                echt.add(n);
            }
        }
        if (echt.isEmpty()) {
            return l;
        }
        l.add(Component.empty());
        l.add(Component.translatable("vadsrijk.klassen.skilltip.verbeteringen").withStyle(ChatFormatting.GOLD));
        for (String n : echt) {
            boolean al = heeft.test(n);
            Component naam = Component.translatable(knoopNaam(n, k));
            l.add(Component.literal(al ? "✔ " : "✖ ").withStyle(al ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .append(naam.copy().withStyle(al ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
            l.addAll(regels(font, knoopUitleg(n, k), Style.EMPTY.withColor(al ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY), "   "));
        }
        return l;
    }

    /** "Verbetert: Kussenklap" for a tree node (empty when it changes no skill). */
    public static List<Component> verbetert(BoomKnoop b) {
        List<String> skills = VOOR.getOrDefault(b.id(), List.of());
        if (skills.isEmpty()) {
            return List.of();
        }
        List<String> namen = new ArrayList<>();
        for (String s : skills) {
            Object[] g = zoek(s);
            namen.add(Component.translatable(g != null ? ((Vaardigheid) g[1]).naamKey() : "vadsrijk.klassen.skill." + s).getString());
        }
        return List.of(Component.translatable("vadsrijk.klassen.skilltip.verbetert", String.join(", ", namen)).withStyle(ChatFormatting.AQUA));
    }

    private static String klasseVan(String knoop) {
        return knoop.startsWith("j_") ? Klassen.JAGER : knoop.startsWith("m_") ? Klassen.MAGIER : Klassen.RIDDER;
    }

    /** Advancement steps use their title ("Kussenkampioen") and explanation; other nodes their own name. */
    private static String knoopNaam(String n, @Nullable Klasse k) {
        int i = n.indexOf("_adv");
        if (i >= 0) {
            return "vadsrijk.klassen.titel." + (k != null ? k.id() : klasseVan(n)) + "." + n.substring(i + 4);
        }
        return "vadsrijk.klassen.knoop." + n;
    }

    private static String knoopUitleg(String n, @Nullable Klasse k) {
        int i = n.indexOf("_adv");
        if (i >= 0) {
            return "vadsrijk.klassen.adv.uitleg." + (k != null ? k.id() : klasseVan(n)) + "." + n.substring(i + 4);
        }
        return "vadsrijk.klassen.knoop." + n + ".uitleg";
    }

    private static List<Component> regels(Font font, String key, Style stijl) {
        return regels(font, key, stijl, "");
    }

    private static List<Component> regels(Font font, String key, Style stijl, String inspring) {
        List<Component> l = new ArrayList<>();
        String tekst = Component.translatable(key).getString();
        for (var r : font.getSplitter().splitLines(tekst, 210, Style.EMPTY)) {
            l.add(Component.literal(inspring + r.getString()).withStyle(stijl));
        }
        return l;
    }
}
