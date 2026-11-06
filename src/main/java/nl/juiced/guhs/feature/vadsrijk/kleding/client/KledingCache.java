package nl.juiced.guhs.feature.vadsrijk.kleding.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.kleding.KledingPayloads;

/**
 * T-KLEDING client state: how every player looks (synced), your own glide rules (synced), and the effects sliders
 * (config/guhs_kleding.properties): "Effecten van anderen" and "Eigen effecten", each ALLES / MINDER / UIT.
 */
public final class KledingCache {
    /** The effects slider (FULL / SOFT / OFF). */
    public enum Niveau {
        ALLES, MINDER, UIT;

        public Niveau volgende() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final Map<UUID, CompoundTag> UITERLIJK = new ConcurrentHashMap<>();
    /** Your own glide rules (server). */
    static volatile KledingPayloads.Staat staat = new KledingPayloads.Staat(false, false, "", false);
    static volatile Niveau anderen = Niveau.ALLES, eigen = Niveau.ALLES;
    private static boolean geladen;

    private KledingCache() {
    }

    public static CompoundTag uiterlijk(UUID u) {
        CompoundTag t = UITERLIJK.get(u);
        return t == null ? new CompoundTag() : t;
    }

    static void zet(UUID u, CompoundTag t) {
        UITERLIJK.put(u, t);
    }

    static void wis() {
        UITERLIJK.clear();
        staat = new KledingPayloads.Staat(false, false, "", false);
    }

    public static String keuze(UUID u, String sleutel) {
        return uiterlijk(u).getStringOr(sleutel, "");
    }

    /** The wing being worn ("vleugel:..."), or null. */
    @Nullable
    public static Cosmetica.Ding vleugel(UUID u) {
        Cosmetica.Ding d = Cosmetica.van(keuze(u, "rug"));
        return d != null && d.soort() == Cosmetica.Soort.VLEUGEL ? d : null;
    }

    /** The cosmetic cape being worn, or null. */
    @Nullable
    public static Cosmetica.Ding kaap(UUID u) {
        Cosmetica.Ding d = Cosmetica.van(keuze(u, "rug"));
        return d != null && d.soort() == Cosmetica.Soort.KAAP ? d : null;
    }

    public static int verf(UUID u) {
        return uiterlijk(u).getIntOr("verf", -1);
    }

    public static int naamkleur(UUID u) {
        return uiterlijk(u).getIntOr("naamkleur", -1);
    }

    public static KledingPayloads.Staat staat() {
        return staat;
    }

    // ---- the sliders ---------------------------------------------------------------------------------------------------------------

    public static Niveau anderen() {
        laad();
        return anderen;
    }

    public static Niveau eigen() {
        laad();
        return eigen;
    }

    static void zetAnderen(Niveau n) {
        anderen = n;
        bewaar();
    }

    static void zetEigen(Niveau n) {
        eigen = n;
        bewaar();
    }

    private static Path pad() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("guhs_kleding.properties");
    }

    private static synchronized void laad() {
        if (geladen) {
            return;
        }
        geladen = true;
        Path p = pad();
        if (!Files.exists(p)) {
            return;
        }
        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(p)) {
            pr.load(r);
            anderen = Niveau.valueOf(pr.getProperty("anderen", "ALLES"));
            eigen = Niveau.valueOf(pr.getProperty("eigen", "ALLES"));
        } catch (IOException | IllegalArgumentException e) {
            // (a broken file: defaults)
        }
    }

    private static synchronized void bewaar() {
        Properties pr = new Properties();
        pr.setProperty("anderen", anderen.name());
        pr.setProperty("eigen", eigen.name());
        try {
            Files.createDirectories(pad().getParent());
            try (Writer w = Files.newBufferedWriter(pad())) {
                pr.store(w, "Guhs Vadsrijk: effecten (ALLES / MINDER / UIT)");
            }
        } catch (IOException e) {
            // (read-only: keep it for this session)
        }
    }
}
