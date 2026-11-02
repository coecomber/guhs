package nl.juiced.guhs.feature.erfenis.client;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

/**
 * Keeps the JourneyMap minimap away while the overview or the world map is open. JourneyMap is only a dev runtime mod (no
 * compile dependency), and it draws its minimap BEHIND every open screen (its setting "allowMiniMapBehindScreens", from
 * a hook in ClientHooks.drawScreen, not through the HUD events), which shows through our transparent overview. So, by
 * reflection and only in memory (never saved): that setting off while one of our screens is open, and back afterwards.
 * Without JourneyMap (or if its insides change) this quietly does nothing.
 */
public final class MinimapStil {
    @Nullable
    private static Boolean oud;
    private static boolean kapot;

    private MinimapStil() {
    }

    /** Our screen opens: no minimap behind it. */
    public static void uit() {
        if (oud != null || kapot) {
            return;
        }
        Object veld = veld();
        if (veld == null) {
            return;
        }
        try {
            Object waarde = veld.getClass().getMethod("get").invoke(veld);
            if (waarde instanceof Boolean b) {
                oud = b;
                zet(veld, false);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            kapot = true;
        }
    }

    /** Our screen is gone: the setting as it was. */
    public static void terug() {
        if (oud == null) {
            return;
        }
        Object veld = veld();
        if (veld != null) {
            try {
                zet(veld, oud);
            } catch (ReflectiveOperationException | RuntimeException e) {
                kapot = true;
            }
        }
        oud = null;
    }

    private static void zet(Object veld, boolean aan) throws ReflectiveOperationException {
        for (Method m : veld.getClass().getMethods()) {
            if (m.getName().equals("set") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(Boolean.class)) {
                m.invoke(veld, Boolean.valueOf(aan));
                return;
            }
        }
    }

    @Nullable
    private static Object veld() {
        if (kapot) {
            return null;
        }
        try {
            Class<?> jm = Class.forName("journeymap.client.JourneymapClient");
            Object client = jm.getMethod("getInstance").invoke(null);
            Object core = client == null ? null : jm.getMethod("getCoreProperties").invoke(client);
            return core == null ? null : core.getClass().getField("allowMiniMapBehindScreens").get(core);
        } catch (ClassNotFoundException e) {
            kapot = true;   // (no JourneyMap: never look again)
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            kapot = true;
            return null;
        }
    }
}
