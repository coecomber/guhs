package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.geluid.Geroezemoes;
import nl.juiced.guhs.feature.vadsrijk.geluid.StemPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.world.GuhTime;

/**
 * U-GEROEZEMOES: the soft guh crowd murmur in the busy places of Het Vadsrijk (see {@link Geroezemoes}). Client only.
 * <ul>
 *   <li>Every second: which busy place you are in, how many guhs are within {@link Geroezemoes#STRAAL} blocks (story NPCs,
 *       Hoogvads' townsfolk, riders), day or night -> the intensity. It glides (a few seconds), so walking around never jumps.</li>
 *   <li>Three bed loops (rustig/druk/markt) crossfade by the intensity ({@link Geroezemoes#bedMix}); everywhere at once, soft
 *       (at most {@link #BASIS} x the slider), under the town music. During a boss fight it dips to a quarter.</li>
 *   <li>Now and then one clear little voice at one of the guhs around you (njeg, giggle, guh, oeh, squeak, blub): it waits for
 *       the world sounds' global pause ({@link WereldGeluiden#losVrij}), so the two never pile up.</li>
 * </ul>
 * Slider "geroezemoes" (guhs-vadsrijk-geluid.toml, default 60) on top of Minecraft's "Omgeving".
 */
public final class GeroezemoesSpeler {
    /** The bed loudness at full intensity and slider 100 (slider 60 -> ~0.27: a background, never a wall of voices). */
    static final float BASIS = 0.45f;
    static final float LOS_VOL = 0.38f;
    private static final RandomSource R = RandomSource.create();
    private static final Lus[] BED = new Lus[3];
    private static final float[] DOEL = new float[3];
    private static final int[] STIL = new int[3];
    private static final List<Entity> GUHS = new ArrayList<>();
    @Nullable
    private static ClientLevel vorigLevel;
    private static int klok, volgendeLos = 200;
    private static float nu, doel;
    /** Dev: a forced intensity 0..1 (-1 = automatic). */
    private static float forceer = -1;
    @Nullable
    private static String plek;
    private static boolean groep, markt, nacht;

    private GeroezemoesSpeler() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    /** A guh that makes the crowd: a Vadsrijk NPC, a townsfolk prop (figurant), a rider. */
    static boolean guh(Entity e) {
        String p = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        return p.startsWith("vr_npc") || p.equals("vr_z3_baas_figurant") || p.equals("vr_ruiter_guh")
                || p.equals("vr_sl_guh");   // V-INT: u-leven-stad's town guhs (children, walkers, terrace chatters)
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        if (l != vorigLevel) {
            vorigLevel = l;
            wis();
        }
        if (l == null || p == null || mc.isPaused()) {
            return;
        }
        klok++;
        float vol = GeluidConfig.geroezemoes();
        boolean in = (Vadsrijk.in(l) || forceer >= 0) && vol > 0;   // (a forced dev intensity plays anywhere)
        if (klok % 20 == 0) {
            meet(l, p, in);
        }
        nu += (doel - nu) * 0.025f;   // ~2 s to settle
        float duik = RijkHud.baasActief() ? 0.25f : 1f;
        float[] mix = Geroezemoes.bedMix(nu);
        for (int k = 0; k < 3; k++) {
            DOEL[k] = in ? BASIS * vol * mix[k] * duik : 0f;
            bed(mc, k);
        }
        if (in && nu > 0.05f && klok >= volgendeLos && WereldGeluiden.losVrij()) {
            speelLos(mc, l, p, Geroezemoes.kiesLos(plek, R.nextDouble()), vol);
            WereldGeluiden.losGebruikt();
            volgendeLos = klok + Geroezemoes.pauze(nu, R.nextDouble());
        }
    }

    private static void meet(ClientLevel l, LocalPlayer p, boolean in) {
        GUHS.clear();
        if (!in || !Vadsrijk.in(l)) {
            doel = in && forceer >= 0 ? forceer : 0;
            return;
        }
        GUHS.addAll(l.getEntities(p, new AABB(p.blockPosition()).inflate(Geroezemoes.STRAAL), GeroezemoesSpeler::guh));
        GUHS.removeIf(e -> e.distanceToSqr(p) > Geroezemoes.STRAAL * Geroezemoes.STRAAL);
        groep = false;
        for (Entity e : GUHS) {
            int n = 0;
            for (Entity o : GUHS) {
                if (o.distanceToSqr(e) <= 36) {
                    n++;
                }
            }
            if (n >= 3) {
                groep = true;
                break;
            }
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        plek = g == null ? null : Geroezemoes.plekVan(g.id(), g.stad());
        markt = g != null && Geroezemoes.marktWijk(g.id());
        long t = GuhTime.timeOfDay(l);
        nacht = t > 13000 && t < 23000;
        doel = forceer >= 0 ? forceer : Geroezemoes.intensiteit(plek, GUHS.size(), groep, markt, nacht);
    }

    private static void bed(Minecraft mc, int k) {
        Lus b = BED[k];
        if (b != null && b.klaar()) {
            BED[k] = b = null;
        }
        if (DOEL[k] > 0.002f) {
            STIL[k] = 0;
            if (b == null || b.gaatUit()) {
                final int i = k;
                BED[k] = new Lus("geroezemoes_" + Geroezemoes.BEDDEN.get(k), GeluidSpeler.event(Geroezemoes.bedId(Geroezemoes.BEDDEN.get(k))),
                        SoundSource.AMBIENT, () -> DOEL[i], 40, true);
                mc.getSoundManager().play(BED[k]);
            }
        } else if (b != null && !b.gaatUit() && ++STIL[k] > 100) {
            b.uit(40);   // (silent for 5 s: let the loop go)
            BED[k] = null;
        }
    }

    /** One clear voice at a guh of the crowd (or, if none is loaded near, somewhere 6-12 blocks around you). */
    private static void speelLos(Minecraft mc, ClientLevel l, LocalPlayer p, String soort, float vol) {
        Entity bron = null;
        List<Entity> dichtbij = new ArrayList<>();
        for (Entity e : GUHS) {
            double d = e.distanceToSqr(p);
            if (e.isAlive() && d > 9 && d < 18 * 18) {
                dichtbij.add(e);
            }
        }
        if (!dichtbij.isEmpty()) {
            bron = dichtbij.get(R.nextInt(dichtbij.size()));
        }
        Vec3 c;
        float toon;
        if (bron != null) {
            c = new Vec3(bron.getX(), bron.getEyeY(), bron.getZ());
            String naam = bron.hasCustomName() ? bron.getCustomName().getString() : "guh" + bron.getId();
            toon = StemPlan.toon(naam);
        } else {
            double a = R.nextDouble() * Math.PI * 2, d = 6 + R.nextDouble() * 6;
            c = new Vec3(p.getX() + Math.cos(a) * d, p.getEyeY() + R.nextDouble() * 2, p.getZ() + Math.sin(a) * d);
            toon = 0.85f + R.nextFloat() * 0.45f;
        }
        if (soort.equals("piep") || soort.equals("blub")) {
            toon = 0.95f + R.nextFloat() * 0.15f;
        }
        float v = LOS_VOL * vol * (0.6f + 0.4f * Math.min(1f, nu + 0.2f));
        mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(Geroezemoes.losId(soort)), SoundSource.AMBIENT, v,
                toon * (0.97f + R.nextFloat() * 0.06f), R, c.x, c.y, c.z));
    }

    // ---- dev ---------------------------------------------------------------------------------------------------------------

    /**
     * Dev ({@code /guhs vadsrijk geluid geroezemoes <naam>}): rustig/druk/markt = force that bed's intensity, njeg/lach/guh/oeh/
     * piep/blub = one voice 5 blocks in front of you, auto = back to automatic, info = what it hears now, kracht:<0-100> = force an
     * intensity.
     */
    static void dev(String naam) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            return;
        }
        switch (naam) {
            case "rustig" -> zetForceer(0.3f);
            case "druk" -> zetForceer(0.6f);
            case "markt" -> zetForceer(1f);
            case "auto" -> zetForceer(-1f);
            case "info" -> info(p);
            default -> {
                if (naam.startsWith("kracht:")) {
                    try {
                        zetForceer(Math.max(0, Math.min(100, Integer.parseInt(naam.substring(7)))) / 100f);
                    } catch (NumberFormatException ignored) {
                        // (a broken number: nothing)
                    }
                } else if (Geroezemoes.LOS.contains(naam)) {
                    Vec3 c = p.getEyePosition().add(p.getLookAngle().scale(5));
                    mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(Geroezemoes.losId(naam)), SoundSource.AMBIENT,
                            LOS_VOL * Math.max(0.5f, GeluidConfig.geroezemoes()), 1f, R, c.x, c.y, c.z));
                }
            }
        }
    }

    private static void zetForceer(float f) {
        forceer = f;
        doel = f >= 0 ? f : doel;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.sendSystemMessage(Component.literal(f < 0 ? "Geroezemoes: weer automatisch." : "Geroezemoes vastgezet op " + Math.round(f * 100)
                    + "% (terug: /guhs vadsrijk geluid geroezemoes auto)").withStyle(ChatFormatting.AQUA));
        }
    }

    private static void info(LocalPlayer p) {
        float[] m = Geroezemoes.bedMix(nu);
        p.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                "Geroezemoes: plek %s%s | guhs dichtbij %d%s | %s | intensiteit %.0f%% (doel %.0f%%%s) | bedden rustig %.2f druk %.2f markt %.2f | schuif %d%%",
                plek == null ? "-" : plek, markt ? " (markt)" : "", GUHS.size(), groep ? " (groepje)" : "", nacht ? "nacht" : "dag",
                nu * 100, doel * 100, forceer >= 0 ? ", vastgezet" : "", m[0], m[1], m[2], Math.round(GeluidConfig.geroezemoes() * 100)))
                .withStyle(ChatFormatting.AQUA));
    }

    private static void wis() {
        Minecraft mc = Minecraft.getInstance();
        for (int k = 0; k < 3; k++) {
            if (BED[k] != null) {
                mc.getSoundManager().stop(BED[k]);
                BED[k] = null;
            }
            DOEL[k] = 0;
        }
        GUHS.clear();
        nu = doel = 0;
        forceer = -1;
    }
}
