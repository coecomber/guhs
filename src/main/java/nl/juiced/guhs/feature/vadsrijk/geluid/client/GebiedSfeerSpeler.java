package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.world.GuhTime;

/**
 * T-GELUID, the ambience pass (roadmap §5.3): per streek a soft bed loop (birds + leaves, water, a cave, a temple hum, bees,
 * waves) and now and then a sweet sound somewhere around the player; crickets at night; caves underground. No strong wind
 * anywhere (user wish). Follows Minecraft's "Omgeving" slider and our own "omgeving" slider.
 * <p>
 * Two bed slots: the main bed (by day the streek's bed; at night the bird beds give way to the crickets) and an extra one (at
 * night the crickets on top of the water beds: lake, ravine, coast). The towns keep their own beds (stadsmuziek), only Honingtak
 * also gets the honey buzz. The old random sounds of WereldSfeer/Z2Sfeer/Z3Sfeer keep running; ours are softer and fill in.
 */
final class GebiedSfeerSpeler {
    static final float BED_VOL = 0.45f, LOS_VOL = 0.55f;
    private static final RandomSource R = RandomSource.create();
    @Nullable
    private static Lus hoofd, extra, voorbeeld;
    private static int klok, volgendeOp, voorbeeldTot;
    @Nullable
    private static ClientLevel vorigLevel;
    private static String huidig = "-";

    private GebiedSfeerSpeler() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    static String huidig() {
        return huidig;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != vorigLevel) {
            vorigLevel = mc.level;
            wis();
        }
        if (mc.isPaused()) {
            return;
        }
        klok++;
        if (voorbeeld != null && voorbeeld.klaar()) {
            voorbeeld = null;
        } else if (voorbeeld != null && klok > voorbeeldTot) {
            voorbeeld.uit(40);
        }
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        String bed = null;
        boolean nacht = false;
        if (l != null && p != null && Vadsrijk.in(l) && GeluidConfig.omgeving() > 0 && voorbeeld == null) {
            GeluidPlan.Streek s = GeluidPlan.streekOp(p.getX(), p.getY(), p.getZ());
            long t = GuhTime.timeOfDay(l);
            nacht = t > 13000 && t < 23000;
            bed = s == null ? null : s.bed();
            // underground (not in zone 3: its tree is layered, its areas already say root/trunk/branch)
            if ((s == null || !s.id().startsWith("z3_")) && onder(l, p)) {
                bed = s != null && GeluidPlan.binnenBed(s.bed()) ? s.bed() : "grot";
            }
        }
        // (U-Z6MUZIEK: nachtBed = "sterren" in the Zijdeland - crickets + star twinkles + singing salt - else "krekels")
        String hoofdBed = bed == null ? null : nacht && GeluidPlan.vogelBed(bed) ? GeluidPlan.nachtBed(bed) : bed;
        String extraBed = bed != null && nacht && !GeluidPlan.vogelBed(bed) && !GeluidPlan.binnenBed(bed) ? GeluidPlan.nachtBed(bed) : null;
        hoofd = slot(mc, hoofd, hoofdBed);
        extra = slot(mc, extra, extraBed);
        huidig = hoofdBed == null ? "-" : hoofdBed + (extraBed != null ? " + " + extraBed : "");
        // now and then a sweet sound (night: the night sounds, except in caves)
        if (hoofdBed == null || klok < volgendeOp) {
            return;
        }
        volgendeOp = klok + 100 + R.nextInt(140);
        String los = nacht && !GeluidPlan.binnenBed(hoofdBed) ? GeluidPlan.nachtBed(bed) : hoofdBed;   // (U-Z6MUZIEK)
        double a = R.nextDouble() * Math.PI * 2, d = 6 + R.nextDouble() * 10;
        l.playLocalSound(p.getX() + Math.cos(a) * d, p.getY() + 1 + R.nextDouble() * 4, p.getZ() + Math.sin(a) * d,
                GeluidSpeler.event(GeluidPlan.sfeerId(los)), SoundSource.AMBIENT, LOS_VOL * GeluidConfig.omgeving() * (0.75f + R.nextFloat() * 0.25f),
                0.95f + R.nextFloat() * 0.1f, false);
    }

    /** Underground: well below the surface and (almost) no sky light (so the shade under giant mushroom caps does not count). */
    private static boolean onder(ClientLevel l, LocalPlayer p) {
        BlockPos b = p.blockPosition().above();
        int maaiveld = l.getHeight(Heightmap.Types.WORLD_SURFACE, b.getX(), b.getZ());
        return b.getY() < maaiveld - 6 && l.getBrightness(LightLayer.SKY, b) < 4;
    }

    /** Keep, swap (crossfade) or stop one bed slot. */
    @Nullable
    private static Lus slot(Minecraft mc, @Nullable Lus nu, @Nullable String bed) {
        if (nu != null && nu.klaar()) {
            nu = null;
        }
        if (nu != null && !Objects.equals(nu.naam, bed)) {
            nu.uit(60);   // the sound manager keeps ticking it: it fades out by itself
            nu = null;
        }
        if (nu == null && bed != null) {
            nu = new Lus(bed, GeluidSpeler.event(GeluidPlan.bedId(bed)), SoundSource.AMBIENT, () -> BED_VOL * GeluidConfig.omgeving(), 80, true);
            mc.getSoundManager().play(nu);
        }
        return nu;
    }

    /** Dev: listen to one bed (12 s of it; the area beds pause meanwhile). */
    static void voorbeeld(String bed) {
        Minecraft mc = Minecraft.getInstance();
        stop();
        voorbeeld = new Lus(bed, GeluidSpeler.event(GeluidPlan.bedId(bed)), SoundSource.AMBIENT, () -> 0.8, 10, true);
        mc.getSoundManager().play(voorbeeld);
        voorbeeldTot = klok + 240;
    }

    static void stop() {
        if (voorbeeld != null) {
            Minecraft.getInstance().getSoundManager().stop(voorbeeld);
            voorbeeld = null;
        }
    }

    private static void wis() {
        Minecraft mc = Minecraft.getInstance();
        for (Lus x : new Lus[] {hoofd, extra, voorbeeld}) {
            if (x != null) {
                mc.getSoundManager().stop(x);
            }
        }
        hoofd = extra = voorbeeld = null;
    }
}
