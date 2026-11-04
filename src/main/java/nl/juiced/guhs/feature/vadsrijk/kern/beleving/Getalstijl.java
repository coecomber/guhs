package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;

/**
 * The polished damage numbers (idea 21), drawn by kern's {@link Getallen} (same API: {@code Getallen.toon}):
 * <ul>
 *   <li>the size grows with the damage (log scale, 0.75..2.4);</li>
 *   <li>crit = gold with a star; otherwise the element colour (white without element; red on a player);</li>
 *   <li>"Zwak!" on a weakness hit (x1.5), "Taai" on a resisted one (x0.7);</li>
 *   <li>DoT ticks (Brandend/Gesporen) on one target merge into one rising sum instead of a rain of small numbers.</li>
 * </ul>
 */
public final class Getalstijl {
    public static final int GOUD = 0xFFC93C, SPELER_ROOD = 0xFF5555, WIT = 0xFFFFFF, ZWAK_KLEUR = 0xFFE45C, TAAI_KLEUR = 0x9A9A9A;
    /** DoT ticks within this many ticks of the last one join its number. */
    public static final int DOT_SAMEN = 30;

    /** What a number looks like. {@code label}: "" / "zwak" / "taai". */
    public record Stijl(String getal, int kleur, float schaal, String label, boolean vet) {
    }

    private record Som(Display.TextDisplay d, double totaal, Element el, long laatst) {
    }

    private static final Map<Integer, Som> DOT = new HashMap<>();

    private Getalstijl() {
    }

    /** Pure: how to show {@code n} damage. */
    public static Stijl van(double n, boolean krit, Element el, double factor, boolean dot, boolean opSpeler) {
        float schaal = (float) Math.max(0.75, Math.min(2.4, 0.75 + 0.3 * (Math.log(1 + Math.max(0, n) / 6.0) / Math.log(2))));
        if (krit) {
            schaal = Math.min(2.6f, schaal * 1.2f);
        }
        if (dot) {
            schaal = Math.min(schaal, 1.0f);
        }
        if (opSpeler) {
            schaal = Math.min(schaal, 1.2f);
        }
        String label = dot ? "" : factor >= 1.45 ? "zwak" : factor > 0 && factor <= 0.75 ? "taai" : "";
        int kleur = opSpeler ? SPELER_ROOD : krit ? GOUD : el != null && el != Element.GEEN ? el.rgb : WIT;
        String getal = (krit ? "★" : "") + (int) Math.ceil(n);
        return new Stijl(getal, kleur, schaal, label, krit || "zwak".equals(label));
    }

    public static MutableComponent tekst(Stijl s) {
        MutableComponent t = Component.literal(s.getal()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(s.kleur())).withBold(s.vet()));
        if ("zwak".equals(s.label())) {
            t.append(Component.literal(" ").append(Component.translatableWithFallback("vadsrijk.kern.beleving.zwak", "Zwak!"))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ZWAK_KLEUR)).withBold(true)));
        } else if ("taai".equals(s.label())) {
            t.append(Component.literal(" ").append(Component.translatableWithFallback("vadsrijk.kern.beleving.taai", "Taai"))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TAAI_KLEUR)).withBold(false).withItalic(true)));
        }
        return t;
    }

    /** Kern's {@code Getallen.toon} lands here. */
    public static void toon(ServerLevel sl, Entity doel, double n, boolean krit, Element el, double factor) {
        boolean dot = Beleving.huidigeIsDot();
        boolean opSpeler = doel instanceof ServerPlayer;
        long nu = sl.getGameTime();
        if (dot && !opSpeler) {
            Som s = DOT.get(doel.getId());
            if (s != null && !s.d().isRemoved() && nu - s.laatst() <= DOT_SAMEN && s.el() == el) {
                double totaal = s.totaal() + n;
                Getallen.zetTekst(s.d(), tekst(van(totaal, false, el, 1, true, false)).append(Component.literal(" ♨").withStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(el.rgb)))));
                s.d().tickCount = Math.min(s.d().tickCount, 6);   // keep it alive a little longer
                DOT.put(doel.getId(), new Som(s.d(), totaal, el, nu));
                return;
            }
        }
        Stijl st = van(n, krit, el, factor, dot, opSpeler);
        double x = doel.getX() + (sl.getRandom().nextDouble() - 0.5) * 0.8, z = doel.getZ() + (sl.getRandom().nextDouble() - 0.5) * 0.8;
        double y = doel.getY() + doel.getBbHeight() + 0.2 + (st.schaal() - 1) * 0.25;
        Display.TextDisplay d = Getallen.tekst(sl, x, y, z, tekst(st), st.schaal());
        if (dot && !opSpeler && d != null) {
            DOT.put(doel.getId(), new Som(d, n, el, nu));
        }
        if (DOT.size() > 256) {
            DOT.values().removeIf(s -> s.d().isRemoved());
        }
    }
}
