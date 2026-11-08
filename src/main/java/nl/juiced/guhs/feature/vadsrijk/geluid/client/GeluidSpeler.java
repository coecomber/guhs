package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.KernMuziek;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.kern.client.SceneSpeler;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;
import nl.juiced.guhs.world.GuhTime;

/**
 * T-GELUID: the music of Het Vadsrijk outside the towns (the town tunes stay with {@code stadsmuziek}).
 * <ul>
 *   <li><b>Area music</b>: walk into a streek and after 5-15 s its theme fades in (~1-2 min); then it is quiet for 1-3 min
 *       (only the ambience; sliders pauze_min/pauze_max), then it plays again. A new streek: the old theme fades out and the new
 *       one starts after a short breath. De Lampionkust has a day and an evening theme, Hoogvads two that alternate.</li>
 *   <li><b>Boss themes</b>: while the boss bar shows (RijkHud hook), the boss's own theme loops (by its entity type, or a sound
 *       id the boss names itself via {@code VadsrijkBaas#muziek()}); it fades out when the bar goes.</li>
 *   <li><b>Stingers</b>: a few seconds of "ta-daa"; the music dips while one plays; two at once queue up.</li>
 *   <li>Vanilla's music is off inside the Vadsrijk (silence there = ambience), unless the area-music slider is 0.</li>
 * </ul>
 * Never during a cutscene or a Vadsrijk scene. Everything uses {@link SoundSource#MUSIC}, so Minecraft's Music slider applies on
 * top of ours.
 */
public final class GeluidSpeler {
    static final float GEBIED_VOL = 0.8f, BAAS_VOL = 0.9f, STINGER_VOL = 0.9f;
    private static final RandomSource R = RandomSource.create();
    static int klok;
    @Nullable
    private static ClientLevel vorigLevel;
    // area music
    @Nullable
    private static Lus gebied;
    @Nullable
    private static String laatsteStreek;
    private static int volgendeOp = 200, wegSinds = -1;
    private static final Map<String, Integer> AFWISSEL = new HashMap<>();
    // boss
    @Nullable
    private static Lus baas;
    @Nullable
    private static String baasThema;
    private static int wachtEntity = -1;
    // U-AUDIO2: the last-phase loop (runs in sync with 'baas' from the start, silent until the boss reaches its last phase)
    @Nullable
    private static Lus baasFase;
    private static int baasEntity = -1, faseVoorbeeldOp = -1;
    private static final java.util.Set<Integer> LAATSTE_FASE = new java.util.HashSet<>();
    private static String wachtId = "";
    // stingers + the dev preview
    @Nullable
    private static Lus stinger, voorbeeld;
    private static final Deque<String> RIJ = new ArrayDeque<>();

    private GeluidSpeler() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GeluidSpeler::onSelectMusic);
    }

    static SoundEvent event(String id) {
        return SoundEvent.createVariableRangeEvent(Guhs.id(id));
    }

    // ---- the tick ---------------------------------------------------------------------------------------------------------

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
        if (wachtEntity >= 0 && klok % 5 == 0) {
            zet(wachtEntity, wachtId);
        }
        if (baas != null && baas.klaar()) {
            baas = null;   // (baasThema stays until the bar goes: a missing file is not retried every bar update)
        }
        if (baasFase != null && baasFase.klaar()) {
            baasFase = null;
        }
        if (faseVoorbeeldOp >= 0 && klok >= faseVoorbeeldOp) {   // dev: the phase preview switches now
            faseVoorbeeldOp = -1;
            naarLaatsteFase();
        }
        if (voorbeeld != null && voorbeeld.klaar()) {
            voorbeeld = null;
        }
        stingerTick(mc);
        gebiedTick(mc);
    }

    /** Something else owns the music now. */
    private static boolean stilte() {
        return baas != null || RijkHud.baasActief() || SceneSpeler.actief() || CutsceneSpeler.actief() || voorbeeld != null;
    }

    // ---- area music -------------------------------------------------------------------------------------------------------

    @Nullable
    private static GeluidPlan.Streek streek(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (mc.level == null || p == null || !Vadsrijk.in(mc.level)) {
            return null;
        }
        return GeluidPlan.streekOp(p.getX(), p.getY(), p.getZ());
    }

    private static void gebiedTick(Minecraft mc) {
        GeluidPlan.Streek s = streek(mc);
        List<String> themas = GeluidPlan.themas(s);
        boolean mag = !themas.isEmpty() && GeluidConfig.gebied() > 0;
        if (gebied != null && gebied.klaar()) {
            boolean vanzelf = !gebied.gaatUit();
            gebied = null;
            // a piece that ended by itself: the long silence; one that was faded away (new streek, a fight): a short breath
            volgendeOp = klok + (vanzelf ? GeluidConfig.pauze(R) : 160 + R.nextInt(240));
        }
        if (gebied != null) {
            gebied.duikDoel = stinger != null ? 0.3f : 1f;
            if (stilte()) {
                gebied.uit(20);
            } else if (!mag || !themas.contains(gebied.naam)) {
                if (wegSinds < 0) {
                    wegSinds = klok;
                }
                if (klok - wegSinds > 60) {   // a 3 s grace for border hopping, then a 6 s fade
                    gebied.uit(120);
                }
            } else {
                wegSinds = -1;
            }
            return;
        }
        wegSinds = -1;
        if (!mag || stilte()) {
            return;
        }
        if (!s.id().equals(laatsteStreek)) {
            laatsteStreek = s.id();
            volgendeOp = Math.min(volgendeOp, klok + 100 + R.nextInt(200));   // just arrived: the streek's voice after 5-15 s
        }
        if (klok < volgendeOp) {
            return;
        }
        start(mc, kies(mc, s, themas));
    }

    private static String kies(Minecraft mc, GeluidPlan.Streek s, List<String> themas) {
        if (themas.size() == 1) {
            return themas.get(0);
        }
        if (themas.contains("lampionkust_avond")) {
            long t = GuhTime.timeOfDay(mc.level);
            return t > 11500 && t < 23500 ? "lampionkust_avond" : "lampionkust_dag";
        }
        String dn = GeluidPlan.dagNacht(themas, nacht(mc));   // U-Z6MUZIEK: De Zoutvlakte by day / by night
        if (dn != null) {
            return dn;
        }
        int i = AFWISSEL.getOrDefault(s.id(), R.nextInt(themas.size()));
        AFWISSEL.put(s.id(), i + 1);
        return themas.get(i % themas.size());
    }

    private static boolean nacht(Minecraft mc) {
        long t = GuhTime.timeOfDay(mc.level);
        return t > 12500 && t < 23500;
    }

    private static void start(Minecraft mc, String thema) {
        mc.getMusicManager().stopPlaying();
        gebied = new Lus(thema, event(GeluidPlan.gebiedId(thema)), SoundSource.MUSIC, () -> GEBIED_VOL * GeluidConfig.gebied(), 120, false);
        mc.getSoundManager().play(gebied);
    }

    /** Vanilla's music stays off inside the Vadsrijk (its silence is ambience), unless our area music is switched off. */
    private static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getBackgroundMusic() != null) {
            return;
        }
        if (gebied != null || baas != null || stinger != null || voorbeeld != null
                || mc.level != null && Vadsrijk.in(mc.level) && GeluidConfig.gebied() > 0) {
            event.overrideMusic(null);
        }
    }

    // ---- boss themes ------------------------------------------------------------------------------------------------------

    /**
     * The boss bar changed (RijkHud): entityId -1 = gone. muziek = the boss's own sound id ("" or kern's generic id = pick one by
     * the boss's entity type). Returns true: T-GELUID owns the boss music (kern's old BaasMuziek stays quiet).
     */
    public static boolean zet(int entityId, String muziek) {
        Minecraft mc = Minecraft.getInstance();
        // U-Z6MUZIEK: u-z6raid names its music per phase (vr.z6raid.muziek.<baas>[_n]): one theme, the phase = its fase loop
        GeluidPlan.Alias alias = GeluidPlan.alias(muziek);
        if (alias != null) {
            muziek = Guhs.MODID + ":" + GeluidPlan.baasId(alias.thema());
        }
        if (entityId < 0) {
            wachtEntity = -1;
            if (baas != null) {
                baas.uit(50);
            }
            if (baasFase != null) {
                baasFase.uit(50);
            }
            baasThema = null;
            baasEntity = -1;
            return true;
        }
        String thema = baasThemaVoor(mc, entityId, muziek);
        if (thema == null) {
            // the boss is not on the client yet: try again in a moment
            wachtEntity = entityId;
            wachtId = muziek;
            return true;
        }
        wachtEntity = -1;
        baasEntity = entityId;
        if (!(thema.equals(baasThema) && (baas == null || !baas.gaatUit()))) {
            speelBaas(mc, thema);
        }
        if (alias != null) {   // U-Z6MUZIEK: the raid's phase id says intense or not (the constellations' window goes both ways)
            if (alias.laatsteFase()) {
                LAATSTE_FASE.add(entityId);
                naarLaatsteFase();
            } else if (LAATSTE_FASE.remove(entityId) && baas != null && baasFase != null) {
                baas.mengDoel = 1f;
                baasFase.mengDoel = 0f;
            }
        }
        return true;
    }

    @Nullable
    private static String baasThemaVoor(Minecraft mc, int entityId, String muziek) {
        if (!muziek.isEmpty() && !muziek.equals(KernMuziek.BAAS.toString())) {
            return muziek;   // the boss names its own music
        }
        if (mc.level == null) {
            return null;
        }
        Entity e = mc.level.getEntity(entityId);
        if (e == null) {
            return null;
        }
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return Guhs.MODID + ":" + GeluidPlan.baasId(GeluidPlan.baasThema(type.getPath()));
    }

    private static void speelBaas(Minecraft mc, String volId) {
        if (baas != null) {
            baas.uit(30);
        }
        if (baasFase != null) {
            baasFase.uit(30);
            baasFase = null;
        }
        if (gebied != null) {
            gebied.uit(20);
        }
        baasThema = volId;
        if (GeluidConfig.bazen() <= 0) {
            baas = null;
            return;
        }
        Identifier id = Identifier.tryParse(volId);
        if (id == null) {
            return;
        }
        mc.getMusicManager().stopPlaying();
        String kort = id.getPath().substring(id.getPath().lastIndexOf('.') + 1);
        baas = new Lus(kort, SoundEvent.createVariableRangeEvent(id), SoundSource.MUSIC, () -> BAAS_VOL * GeluidConfig.bazen(), 30, true);
        // U-AUDIO2: its last-phase loop starts at the same moment (silent), so the crossfade later lands on the same spot
        String t = GeluidPlan.themaVanId(volId);
        if (t != null) {
            baasFase = new Lus(t + "_fase", event(GeluidPlan.faseId(t)), SoundSource.MUSIC, () -> BAAS_VOL * GeluidConfig.bazen(), 30, true);
            baasFase.meng = baasFase.mengDoel = 0f;
            if (LAATSTE_FASE.contains(baasEntity)) {   // (joined the fight late, already in the last phase)
                baasFase.meng = baasFase.mengDoel = 1f;
                baas.meng = baas.mengDoel = 0f;
            }
        }
        mc.getSoundManager().play(baas);
        if (baasFase != null) {
            mc.getSoundManager().play(baasFase);
        }
    }

    /** U-AUDIO2 (server: the VadsrijkBaas phase hook): boss entityId reached phase fase of max; the last one = the intense loop. */
    static void fase(int entityId, int fase, int max) {
        if (fase < max) {
            return;
        }
        if (LAATSTE_FASE.size() > 64) {
            LAATSTE_FASE.clear();
        }
        LAATSTE_FASE.add(entityId);
        if (entityId == baasEntity) {
            naarLaatsteFase();
        }
    }

    /** Crossfade (2 s) from the boss theme into its last-phase loop (the base keeps running silent, in sync). */
    private static void naarLaatsteFase() {
        if (baas != null && baasFase != null) {
            baas.mengDoel = 0f;
            baasFase.mengDoel = 1f;
        }
    }

    /** U-AUDIO2: the boss is beaten: its music makes room and its own fanfare plays (the shared stinger for an unknown boss). */
    static void fanfare(String thema) {
        if (baas != null) {
            baas.uit(20);
        }
        if (baasFase != null) {
            baasFase.uit(20);
        }
        stinger(GeluidPlan.BAAS_THEMAS.contains(thema) ? "fanfare." + thema : "baas_verslagen");
    }

    /** Dev: a boss theme, and after 12 s its last phase fades in (stop with /guhs vadsrijk geluid stop). */
    static void faseVoorbeeld(String thema) {
        Minecraft mc = Minecraft.getInstance();
        stopVoorbeeld();
        if (gebied != null) {
            gebied.uit(20);
        }
        baasEntity = -2;
        LAATSTE_FASE.remove(-2);
        baasThema = Guhs.MODID + ":" + GeluidPlan.baasId(thema);
        speelBaas(mc, baasThema);
        faseVoorbeeldOp = klok + 240;
    }

    // ---- stingers ---------------------------------------------------------------------------------------------------------

    public static void stinger(String soort) {
        if (GeluidConfig.stingers() <= 0 || !GeluidPlan.STINGERS.contains(soort) && !soort.startsWith("fanfare.")) {
            return;
        }
        if (stinger != null && !stinger.klaar()) {
            if (!RIJ.contains(soort) && !soort.equals(stinger.naam) && RIJ.size() < 2) {
                RIJ.add(soort);
            }
            return;
        }
        speelStinger(soort);
    }

    private static void speelStinger(String soort) {
        Minecraft mc = Minecraft.getInstance();
        String id = soort.startsWith("fanfare.") ? GeluidPlan.fanfareId(soort.substring(8)) : GeluidPlan.stingerId(soort);   // U-AUDIO2
        stinger = new Lus(soort, event(id), SoundSource.MUSIC, () -> STINGER_VOL * GeluidConfig.stingers(), 1, false);
        mc.getSoundManager().play(stinger);
    }

    private static void stingerTick(Minecraft mc) {
        if (stinger != null && stinger.klaar()) {
            stinger = null;
            if (!RIJ.isEmpty()) {
                speelStinger(RIJ.poll());
            }
        }
        if (baas != null) {
            baas.duikDoel = stinger != null ? 0.5f : 1f;
        }
        if (baasFase != null) {
            baasFase.duikDoel = stinger != null ? 0.5f : 1f;
        }
    }

    // ---- dev --------------------------------------------------------------------------------------------------------------

    /** Dev preview: play one track right now (other music fades away while it plays). */
    static void voorbeeld(String soort, String naam) {
        Minecraft mc = Minecraft.getInstance();
        stopVoorbeeld();
        if (gebied != null) {
            gebied.uit(20);
        }
        if (baas != null) {
            baas.uit(20);
        }
        String id = soort.equals("gebied") ? GeluidPlan.gebiedId(naam) : soort.equals("stad") ? "vr.stad.muziek." + naam   // (U-Z6MUZIEK)
                : GeluidPlan.baasId(naam);
        voorbeeld = new Lus(naam, event(id), SoundSource.MUSIC, () -> 0.9, 10, false);
        mc.getSoundManager().play(voorbeeld);
    }

    static void stopVoorbeeld() {
        if (voorbeeld != null) {
            Minecraft.getInstance().getSoundManager().stop(voorbeeld);
            voorbeeld = null;
        }
    }

    /** Dev: everything off (area, boss preview, stinger). */
    static void stopAlles() {
        Minecraft mc = Minecraft.getInstance();
        stopVoorbeeld();
        for (Lus l : new Lus[] {gebied, stinger, baas, baasFase}) {
            if (l != null) {
                mc.getSoundManager().stop(l);
            }
        }
        gebied = stinger = baas = baasFase = null;
        baasThema = null;
        faseVoorbeeldOp = -1;
        RIJ.clear();
        volgendeOp = klok + GeluidConfig.pauze(R);
    }

    /** Dev: skip the silence (the streek's theme starts within a second). */
    static void nu() {
        stopVoorbeeld();
        if (gebied != null) {
            Minecraft.getInstance().getSoundManager().stop(gebied);
            gebied = null;
        }
        volgendeOp = klok + 10;
    }

    static void info() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        GeluidPlan.Streek s = streek(mc);
        String nu = gebied != null ? "speelt: " + gebied.naam : stilte() ? "stil (baas/scene/voorbeeld)"
                : s == null || s.themas().isEmpty() ? "geen gebiedsmuziek hier (stad of buiten de zones)"
                : "stilte, nog " + Math.max(0, (volgendeOp - klok) / 20) + " s";
        p.sendSystemMessage(Component.literal("Gebied: " + (g == null ? "-" : g.id()) + "  streek: " + (s == null ? "-" : s.id())
                + "  thema's: " + (s == null ? "-" : String.join(", ", s.themas())) + "  bed: " + GebiedSfeerSpeler.huidig()).withStyle(ChatFormatting.AQUA));
        p.sendSystemMessage(Component.literal("Muziek: " + nu + (baasThema != null ? "  |  baas: " + baasThema
                + (baasFase != null && baasFase.mengDoel > 0.5f ? " (laatste fase)" : "") : "")
                + "  |  schuifjes: gebied " + pct(GeluidConfig.gebied()) + " bazen " + pct(GeluidConfig.bazen()) + " stingers "
                + pct(GeluidConfig.stingers()) + " omgeving " + pct(GeluidConfig.omgeving())).withStyle(ChatFormatting.AQUA));
    }

    private static String pct(float f) {
        return Math.round(f * 100) + "%";
    }

    /** Leaving a world or a server: everything off at once. */
    private static void wis() {
        Minecraft mc = Minecraft.getInstance();
        for (Lus l : new Lus[] {gebied, baas, baasFase, stinger, voorbeeld}) {
            if (l != null) {
                mc.getSoundManager().stop(l);
            }
        }
        gebied = baas = baasFase = stinger = voorbeeld = null;
        LAATSTE_FASE.clear();
        baasEntity = -1;
        faseVoorbeeldOp = -1;
        baasThema = null;
        laatsteStreek = null;
        wachtEntity = -1;
        RIJ.clear();
        volgendeOp = klok + 200;
    }
}
