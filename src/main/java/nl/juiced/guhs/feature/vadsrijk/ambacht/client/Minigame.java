package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;

/**
 * The optional 2-4 s gathering minigame (VOORSTEL §5.2), drawn under the crosshair; right-click (the use key) plays it:
 * <ul>
 *   <li>Mijnen "Pluisader": 3 taps on the glitter (the pink zone moves after every tap);</li>
 *   <li>Houthakken "Kerfritme": 2 quick taps, a faster marker;</li>
 *   <li>Plukken "Zachtjes" / Imkeren "Rook": HOLD the key, the bar fills; let go in the green;</li>
 *   <li>Vissen "Beet!": wait for the bite, then tap at once (too early = nothing).</li>
 * </ul>
 * The timing is random every time (no macro scores perfect). The score 0..2 is the quality. Not playing at all = after 4 s the
 * normal (gewoon) harvest; sneak-click skips the game.
 */
public final class Minigame {
    enum Soort {
        TIKKEN, LOSLATEN, BEET
    }

    private static final RandomSource R = RandomSource.create();
    @Nullable
    private static String spot;
    private static Ambacht ambacht = Ambacht.MIJNEN;
    private static Soort soort = Soort.TIKKEN;
    private static long start, beet;
    private static int nodig, gedaan, punten;
    private static double doel, breed, snelheid, fase;
    private static boolean vast;
    private static long holdStart;
    private static String uitslag = "";
    private static long uitslagTot;

    private Minigame() {
    }

    public static boolean bezig() {
        return spot != null;
    }

    static void start(String id, Ambacht a) {
        spot = id;
        ambacht = a;
        start = System.currentTimeMillis();
        gedaan = 0;
        punten = 0;
        vast = false;
        fase = R.nextDouble() * Math.PI * 2;
        switch (a) {
            case MIJNEN -> {
                soort = Soort.TIKKEN;
                nodig = 3;
                snelheid = 1.6 + R.nextDouble() * 0.5;
            }
            case HOUTHAKKEN -> {
                soort = Soort.TIKKEN;
                nodig = 2;
                snelheid = 2.3 + R.nextDouble() * 0.5;
            }
            case VISSEN -> {
                soort = Soort.BEET;
                nodig = 1;
                beet = start + 900 + R.nextInt(1600);
            }
            default -> {
                soort = Soort.LOSLATEN;
                nodig = 1;
                snelheid = a == Ambacht.IMKEREN ? 0.45 : 0.6;
            }
        }
        nieuwDoel();
        geluid(a == Ambacht.VISSEN ? SoundEvents.FISHING_BOBBER_THROW : SoundEvents.UI_BUTTON_CLICK.value(), 1.4f);
    }

    private static void nieuwDoel() {
        breed = soort == Soort.LOSLATEN ? 0.16 : 0.18;
        doel = 0.2 + R.nextDouble() * 0.6;
    }

    /** Where the marker is now (0..1). */
    private static double marker() {
        double t = (System.currentTimeMillis() - start) / 1000.0;
        if (soort == Soort.LOSLATEN) {
            return vast ? Math.min(1, (System.currentTimeMillis() - holdStart) / 1000.0 * snelheid) : 0;
        }
        return 0.5 + 0.5 * Math.sin(t * snelheid * Math.PI + fase);
    }

    private static int score(double m) {
        double d = Math.abs(m - doel);
        return d <= breed * 0.25 ? 2 : d <= breed * 0.5 ? 1 : 0;
    }

    /** The use key went down (true: we used it). */
    static boolean tik() {
        if (spot == null) {
            return false;
        }
        switch (soort) {
            case TIKKEN -> {
                int s = score(marker());
                punten += s;
                gedaan++;
                geluid(s == 2 ? SoundEvents.AMETHYST_BLOCK_CHIME : s == 1 ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.WOOL_HIT, 1.0f + 0.2f * s);
                nieuwDoel();
                if (gedaan >= nodig) {
                    klaar(eind());
                }
            }
            case BEET -> {
                long nu = System.currentTimeMillis();
                int s = nu < beet ? 0 : nu - beet < 350 ? 2 : nu - beet < 700 ? 1 : 0;
                klaar(s);
            }
            case LOSLATEN -> {
                if (!vast) {
                    vast = true;
                    holdStart = System.currentTimeMillis();
                }
            }
        }
        return true;
    }

    private static int eind() {
        double gem = punten / (double) Math.max(1, nodig);
        return gem >= 1.6 ? 2 : gem >= 0.9 ? 1 : 0;
    }

    static void tick() {
        if (spot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long nu = System.currentTimeMillis();
        if (soort == Soort.LOSLATEN && vast && !mc.options.keyUse.isDown()) {
            klaar(score(marker()));
            return;
        }
        if (soort == Soort.LOSLATEN && vast && marker() >= 1) {
            klaar(0);   // (held too long: squashed)
            return;
        }
        if (soort == Soort.BEET && nu > beet && nu - beet < 60) {
            geluid(SoundEvents.FISHING_BOBBER_SPLASH, 1.2f);
        }
        if (nu - start > (soort == Soort.BEET ? 4500 : 4000)) {
            klaar(soort == Soort.TIKKEN ? eind() : 0);
        }
    }

    static void klaar(int score) {
        String id = spot;
        spot = null;
        if (id == null) {
            return;
        }
        stuur(id, score, false);
        uitslag = score == 2 ? "vadsrijk.ambacht.mini.perfect" : score == 1 ? "vadsrijk.ambacht.mini.goed" : "vadsrijk.ambacht.mini.gewoon";
        uitslagTot = System.currentTimeMillis() + 1200;
    }

    static void stop() {
        spot = null;
    }

    static void stuur(String id, int score, boolean over) {
        CompoundTag a = new CompoundTag();
        a.putString("spot", id);
        a.putInt("score", score);
        a.putBoolean("over", over);
        ClientPacketDistributor.sendToServer(new AmbachtPayloads.Doe("oogst", a));
    }

    private static void geluid(net.minecraft.sounds.SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    /** The bar under the crosshair. */
    static void teken(GuiGraphicsExtractor g, Font font) {
        int cx = g.guiWidth() / 2, y = g.guiHeight() / 2 + 18;
        if (spot == null) {
            if (System.currentTimeMillis() < uitslagTot) {
                g.centeredText(font, Component.translatable(uitslag), cx, y, 0xFFFFC8E8);
            }
            return;
        }
        int w = 120, x0 = cx - w / 2;
        g.centeredText(font, Component.translatable("vadsrijk.ambacht.mini." + ambacht.id).withStyle(ChatFormatting.BOLD), cx, y - 11, 0xFFFFFFFF);
        g.fill(x0 - 2, y - 2, x0 + w + 2, y + 10, 0xC0402040);
        g.fill(x0, y, x0 + w, y + 8, 0xFF5A3A5A);
        if (soort == Soort.BEET) {
            boolean nu = System.currentTimeMillis() >= beet;
            g.fill(x0, y, x0 + w, y + 8, nu ? 0xFF4AC87A : 0xFF3A5A8A);
            g.centeredText(font, Component.translatable(nu ? "vadsrijk.ambacht.mini.beet" : "vadsrijk.ambacht.mini.wacht"), cx, y + 12, 0xFFFFFFFF);
            return;
        }
        int dx = (int) ((doel - breed / 2) * w), dw = (int) (breed * w);
        g.fill(x0 + dx, y, x0 + dx + dw, y + 8, soort == Soort.LOSLATEN ? 0xFF4AC87A : 0xFFFF6FB5);
        g.fill(x0 + dx + dw * 3 / 8, y, x0 + dx + dw * 5 / 8, y + 8, 0xFFFFFFFF);
        int m = x0 + (int) (marker() * w);
        if (soort == Soort.LOSLATEN) {
            g.fill(x0, y + 2, m, y + 6, 0xC0FFE070);
        }
        g.fill(m - 1, y - 3, m + 1, y + 11, 0xFFFFFFFF);
        String hint = soort == Soort.LOSLATEN ? (vast ? "vadsrijk.ambacht.mini.los" : "vadsrijk.ambacht.mini.houd") : "vadsrijk.ambacht.mini.tik";
        g.centeredText(font, Component.translatable(hint, gedaan, nodig), cx, y + 12, 0xFFFFE8F4);
    }
}
