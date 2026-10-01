package nl.juiced.guhs.taal;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 1.2.0: the Dutch Guhs texts on the server side, read from the jar's assets/guhs/lang/nl_nl.json. The server itself
 * resolves in en_us (English since 1.2.0); this is only for what must know the Dutch: the gametests, and recognising names
 * that were saved as resolved Dutch text before 1.2.0. Never use it to send text to players (send Components).
 */
public final class NlTekst {
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([sd%])");
    private static volatile Map<String, String> teksten;

    private static Map<String, String> teksten() {
        Map<String, String> t = teksten;
        if (t == null) {
            Map<String, String> m = new HashMap<>();
            try (InputStream in = NlTekst.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
                if (in != null) {
                    Language.loadFromJson(in, m::put);
                }
            } catch (Exception e) {
                // (no Dutch: everything falls back on the server's language)
            }
            teksten = t = Map.copyOf(m);
        }
        return t;
    }

    public static boolean has(String key) {
        return teksten().containsKey(key);
    }

    /** The Dutch of a key (the server's language when it isn't a Guhs key, the key itself when nobody knows it). */
    public static String get(String key) {
        String s = teksten().get(key);
        return s != null ? s : Language.getInstance().getOrDefault(key);
    }

    /** A Component as a Dutch reader sees it (translatable parts in Dutch, arguments included). */
    public static String tekst(Component c) {
        StringBuilder out = new StringBuilder();
        if (c.getContents() instanceof TranslatableContents t) {
            String format = teksten().containsKey(t.getKey()) ? teksten().get(t.getKey())
                    : t.getFallback() != null ? t.getFallback() : Language.getInstance().getOrDefault(t.getKey());
            Object[] args = t.getArgs();
            Matcher m = FORMAT.matcher(format);
            int i = 0, at = 0;
            while (m.find()) {
                out.append(format, at, m.start());
                at = m.end();
                if (m.group(2).equals("%")) {
                    out.append('%');
                    continue;
                }
                int n = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : i++;
                Object a = n >= 0 && n < args.length ? args[n] : "";
                out.append(a instanceof Component ac ? tekst(ac) : String.valueOf(a));
            }
            out.append(format, at, format.length());
        } else if (c.getContents() instanceof PlainTextContents p) {
            out.append(p.text());
        } else {
            out.append(c.plainCopy().getString());
        }
        for (Component sibling : c.getSiblings()) {
            out.append(tekst(sibling));
        }
        return out.toString();
    }

    private NlTekst() {
    }
}
