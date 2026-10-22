package nl.juiced.guhs.feature.safari.camera;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * What one click of the {@link SafariCameraItem} saw (server side, {@link Fotograaf#kijk}): the animal in the middle of the
 * picture and the points of design 1.7 (S1).
 * <ul>
 *   <li>{@code afstand} 0..2: how big the animal is in the picture (and not behind leaves or half hidden);</li>
 *   <li>{@code midden} 0..1: in the middle and looking at you;</li>
 *   <li>{@code actie} 0..1: doing something special (sleeping, eating, playing, hanging, any extra animation);</li>
 *   <li>{@code extra} 0..1: a baby, a family or a second species in the picture.</li>
 * </ul>
 * 0-1 points = 1 star, 2-3 = 2 stars, 4-5 = 3 stars. The {@link #tag} is what SafariSpeler keeps as the dex photo (and what a
 * printed {@link SafariFotoItem} carries), the caption is built from it again ({@link #onderschrift}), so it reads in the
 * language of whoever looks at it.
 */
public record Foto(String soort, String variant, String zone, String actie, boolean slaapt, boolean baby, boolean water,
                   int zelfde, int soorten, int afstand, int midden, int actiePunt, int extra) {

    public int punten() {
        return afstand + midden + actiePunt + extra;
    }

    public int sterren() {
        int p = punten();
        return p <= 1 ? 1 : p <= 3 ? 2 : 3;
    }

    /** What the action word of the caption is: "slaap", "eet", an extra animation's name, or "" (just standing). */
    public String actieWoord() {
        if (slaapt) {
            return "slaap";
        }
        return actie == null || actie.equals("idle") || actie.equals("walk") ? "" : actie;
    }

    public CompoundTag tag(long dag, String fotograaf) {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort);
        t.putString("Variant", variant);
        t.putString("Actie", actieWoord());
        t.putBoolean("Baby", baby);
        t.putBoolean("Dichtbij", afstand >= 2);
        t.putBoolean("Familie", zelfde >= 2);
        t.putBoolean("Samen", soorten >= 2);
        t.putInt("Sterren", sterren());
        t.putLong("Dag", dag);
        t.putString("Fotograaf", fotograaf);
        return t;
    }

    /**
     * The caption of a photo tag: "Wauw, een slapende rode panda van dichtbij!" (lang keys gui.guhs.safari.foto.*). The
     * action word is an adjective key ({@code gui.guhs.safari.foto.actie.<actie>}); an action without its own word is left
     * out, so a new animation never shows a raw key.
     */
    public static Component onderschrift(CompoundTag t) {
        String soort = t.getStringOr("Soort", "");
        int sterren = Math.max(1, Math.min(3, t.getIntOr("Sterren", 1)));
        String actie = t.getStringOr("Actie", "");
        String actieKey = "gui.guhs.safari.foto.actie." + actie;
        Component bijwoord = actie.isEmpty() || !Teksten.bestaat(actieKey) ? Component.empty()
                : Component.translatable(actieKey).append(" ");
        Component dier = Component.translatable(t.getBooleanOr("Baby", false) ? "gui.guhs.safari.foto.baby" : "gui.guhs.safari.foto.dier",
                bijwoord, Component.translatable("gui.guhs.safari.foto.naam." + soort));   // (the name inside a sentence: "rode panda")
        Component waar = t.getBooleanOr("Nacht", false) ? Component.translatable("gui.guhs.safari.foto.nacht")
                : t.getBooleanOr("Familie", false) ? Component.translatable("gui.guhs.safari.foto.familie")
                : t.getBooleanOr("Samen", false) ? Component.translatable("gui.guhs.safari.foto.samen")
                : t.getBooleanOr("Dichtbij", false) ? Component.translatable("gui.guhs.safari.foto.dichtbij") : Component.empty();
        return Component.translatable("gui.guhs.safari.foto.zin." + sterren, dier, waar);
    }

    /** Lang keys exist on the client (I18n); on the server every key is taken as existing. */
    public static final class Teksten {
        public static volatile java.util.function.Predicate<String> bestaat = k -> true;

        static boolean bestaat(String key) {
            return bestaat.test(key);
        }

        private Teksten() {
        }
    }
}
