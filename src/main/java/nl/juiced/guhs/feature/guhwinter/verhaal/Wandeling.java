package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.guhwinter.hoentjes.GuhwinterHoentjes;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs;
import nl.juiced.guhs.feature.guhwinter.npc.WinterGuh;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;

/**
 * Guhwinter (verhaal slice): the kerstwandeling around the pond (chapter 2, step 4), played LIVE with the real plein guhs:
 * the six and Anthony walk the {@code wandel_*} route in a little group, a waterhoen crosses the path (ALL SIX: "NOWAY EEN
 * WATERHOEN!????"), a bit later an alpine hoentje hops by (even more extreme), Anthony takes far too many photos. The
 * hoentjes come from {@link GuhwinterHoentjes#spawnScene}. One walk at a time: whoever clicks one of the six while it runs
 * joins it; everyone in it who is still near at the end gets the step. If all walkers leave, it stops and can start again.
 * Lines are chat lines of the guhs ({@link GuhwinterNpcs#say}); keys {@code guhwinter.verhaal.wandel.*}.
 */
public final class Wandeling {
    static final List<String> ZES = List.of("joost", "suus", "liek", "lin", "evi", "niels");
    /** The walkers in formation order: Joost in front, Anthony at the back with his camera. */
    static final List<String> GROEP = List.of("joost", "suus", "evi", "niels", "liek", "lin", "anthony");
    private static final String W = "guhwinter.verhaal.wandel.";
    /** "wait until the group arrived" (at most {@link #MAX_LOPEN} ticks). */
    private static final int LOPEN = -1, MAX_LOPEN = 170;

    @Nullable
    private static Wandeling lopend;

    @FunctionalInterface
    private interface Actie {
        /** Does its thing; returns how many ticks to wait, or {@link #LOPEN}. */
        int doe(Wandeling w);
    }

    private final ServerLevel level;
    private final Set<UUID> spelers = new LinkedHashSet<>();
    private final List<Vec3> route;
    private final List<Actie> script = new ArrayList<>();
    private int i, wacht, lopen, alleenTicks;

    private Wandeling(ServerLevel level, List<Vec3> route) {
        this.level = level;
        this.route = route;
        int n = route.size();
        int waterhoen = Math.max(1, Math.min(3, n - 2)), alpine = Math.max(waterhoen + 1, Math.min(7, n - 2));
        script.add(w -> w.groepNaar(0));
        script.add(w -> w.zeg("evi", "start.0", 50));
        script.add(w -> w.zeg("suus", "start.1", 60));
        script.add(w -> w.zeg("joost", "start.2", 50));
        for (int k = 1; k <= waterhoen; k++) {
            int punt = k;
            script.add(w -> w.groepNaar(punt));
            if (k == 2) {
                script.add(w -> w.zeg("niels", "vijver", 60));
                script.add(w -> w.zeg("lin", "koud", 40));
            }
        }
        // --- the waterhoen ---
        script.add(w -> w.hoentje(GuhwinterHoentjes.WATERHOEN, waterhoen, true));
        script.add(w -> w.noway("noway_water", GuhwinterHoentjes.WATERHOEN, Emote.SCHRIKKEN, 1));
        script.add(w -> w.foto("foto.0"));
        script.add(w -> w.zeg("evi", "water.0", 55));
        script.add(w -> w.zeg("liek", "water.1", 45));
        script.add(w -> w.zeg("niels", "water.2", 60));
        for (int k = waterhoen + 1; k <= alpine; k++) {
            int punt = k;
            script.add(w -> w.groepNaar(punt));
            if (k == waterhoen + 2) {
                script.add(w -> w.zeg("suus", "hint.0", 65));
                script.add(w -> w.zeg("joost", "hint.1", 50));
                script.add(w -> w.zeg("suus", "hint.2", 50));
            }
        }
        // --- the alpine hoentje (even more extreme) ---
        script.add(w -> w.hoentje(GuhwinterHoentjes.ALPINEHOEN, alpine, false));
        script.add(w -> w.noway("noway_alpine", GuhwinterHoentjes.ALPINEHOEN, Emote.VAHOEG, 2));
        script.add(w -> w.foto("foto.1"));
        script.add(w -> w.zeg("niels", "alpine.0", 65));
        script.add(w -> w.zeg("lin", "alpine.1", 45));
        script.add(w -> w.zeg("evi", "alpine.2", 45));
        script.add(w -> w.zeg("joost", "alpine.3", 50));
        for (int k = alpine + 1; k < n; k++) {
            int punt = k;
            script.add(w -> w.groepNaar(punt));
        }
        script.add(w -> w.zeg("evi", "einde.0", 55));
        script.add(w -> w.zeg("suus", "einde.1", 60));
        script.add(w -> {
            w.klaar();
            return 0;
        });
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Wandeling w = lopend;
            if (w != null) {
                w.tick();
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> lopend = null);
    }

    static boolean isZes(String naam) {
        return ZES.contains(naam);
    }

    /** Is this player walking along right now? */
    static boolean doetMee(ServerPlayer p) {
        Wandeling w = lopend;
        return w != null && w.spelers.contains(p.getUUID());
    }

    static boolean bezig() {
        return lopend != null;
    }

    /** One of the six (or Anthony) was clicked at the wandeling step: start the walk, or join the one that runs. */
    static void klik(ServerPlayer p) {
        Wandeling w = lopend;
        if (w != null) {
            if (w.spelers.add(p.getUUID())) {
                p.sendSystemMessage(Component.translatable(W + "mee").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        start(p);
    }

    /** Starts the walk with this player (false: no route, or the guhs are not here). */
    static boolean start(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || lopend != null) {
            return false;
        }
        List<Vec3> route = new ArrayList<>();
        for (GuhwinterAnkers.Anker a : GuhwinterAnkers.reeks("wandel_")) {
            route.add(a.vec());
        }
        if (route.size() < 4 || GuhwinterNpcs.get(sl, "joost") == null) {
            p.sendSystemMessage(Component.translatable(W + "niemand").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Wandeling w = new Wandeling(sl, route);
        w.spelers.add(p.getUUID());
        for (ServerPlayer ander : sl.players()) {
            if (ander != p && ander.distanceToSqr(route.get(0)) < 20 * 20 && WinterVerhaal.KERST.stap(ander) == WinterVerhaal.K_WANDELING
                    && WinterVerhaal.open(ander, WinterVerhaal.Hoofdstuk.KERST)) {
                w.spelers.add(ander.getUUID());
            }
        }
        lopend = w;
        p.sendSystemMessage(Component.translatable(W + "begin").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** (dev) stops a walk that runs. */
    static boolean stop() {
        Wandeling w = lopend;
        if (w == null) {
            return false;
        }
        w.laatLos();
        lopend = null;
        return true;
    }

    // =====================================================================================================================
    // the walk
    // =====================================================================================================================

    private void tick() {
        Entity joost = GuhwinterNpcs.get(level, "joost");
        if (joost == null || !iemandBij(joost)) {
            if (++alleenTicks > 200) {
                for (ServerPlayer p : spelers()) {
                    p.sendSystemMessage(Component.translatable(W + "gestopt").withStyle(ChatFormatting.GRAY));
                }
                laatLos();
                lopend = null;
            }
            return;
        }
        alleenTicks = 0;
        if (nogEenSprong > 0 && --nogEenSprong == 0) {
            for (String naam : ZES) {
                spring(naam);
            }
        }
        if (lopen > 0) {
            lopen--;
            boolean aangekomen = !GuhwinterNpcs.isWalking(level, "joost");
            if (!aangekomen && lopen > 0) {
                return;
            }
            lopen = 0;
            wacht = 6;
        }
        if (wacht > 0) {
            wacht--;
            return;
        }
        if (i >= script.size()) {
            lopend = null;
            return;
        }
        int t = script.get(i++).doe(this);
        if (t == LOPEN) {
            lopen = MAX_LOPEN;
        } else {
            wacht = t;
        }
    }

    private boolean iemandBij(Entity joost) {
        for (ServerPlayer p : spelers()) {
            if (p.level() == level && p.distanceToSqr(joost) < 40 * 40) {
                return true;
            }
        }
        return false;
    }

    private List<ServerPlayer> spelers() {
        List<ServerPlayer> uit = new ArrayList<>();
        for (UUID id : spelers) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** The group walks to route point k: Joost in front, the others two by two behind him, Anthony last. */
    private int groepNaar(int k) {
        Vec3 doel = route.get(k);
        Vec3 van = k > 0 ? route.get(k - 1) : route.get(Math.min(1, route.size() - 1));
        Vec3 d = k > 0 ? doel.subtract(van) : van.subtract(doel);
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        d = len < 1e-3 ? new Vec3(0, 0, 1) : new Vec3(d.x / len, 0, d.z / len);
        Vec3 dwars = new Vec3(-d.z, 0, d.x);
        for (int j = 0; j < GROEP.size(); j++) {
            int rij = (j + 1) / 2;
            double kant = j == 0 ? 0 : (j % 2 == 1 ? 0.9 : -0.9);
            Vec3 plek = doel.subtract(d.scale(rij * 1.6)).add(dwars.scale(kant));
            GuhwinterNpcs.walkTo(level, GROEP.get(j), plek, 0.9);
        }
        return LOPEN;
    }

    private int zeg(String wie, String key, int ticks) {
        GuhwinterNpcs.say(level, wie, Component.translatable(W + key));
        return ticks;
    }

    @Nullable
    private Entity hoen;

    /** A hoentje crosses the path between route point k and the next one (the waterhoen out of the pond, the alpine one towards it). */
    private int hoentje(String soort, int k, boolean uitVijver) {
        Vec3 a = route.get(k), b = route.get(Math.min(k + 1, route.size() - 1));
        Vec3 midden = a.add(b).scale(0.5);
        Vec3 d = b.subtract(a);
        double len = Math.max(1e-3, Math.sqrt(d.x * d.x + d.z * d.z));
        Vec3 dwars = new Vec3(-d.z / len, 0, d.x / len);
        Vec3 vijver = VerhaalStappen.anker("vijver_midden");
        if (vijver != null && dwars.dot(vijver.subtract(midden)) < 0) {
            dwars = dwars.scale(-1);   // (dwars points to the pond now)
        }
        Vec3 van = uitVijver ? midden.add(dwars.scale(4.5)) : midden.subtract(dwars.scale(6));
        Vec3 naar = uitVijver ? midden.subtract(dwars.scale(5)) : midden.add(dwars.scale(2.5));
        hoen = GuhwinterHoentjes.spawnScene(level, soort, van, naar);
        for (String naam : GROEP) {
            GuhwinterNpcs.lookAt(level, naam, midden.add(0, 0.3, 0), 120);
        }
        return 30;
    }

    /** ALL SIX at once, staggered by a tick or two, with a jump (two for the alpine one), an emote, and your own NOWAY. */
    private int noway(String key, String soort, Emote emote, int sprongen) {
        if (hoen != null && hoen.isAlive()) {
            for (String naam : GROEP) {
                GuhwinterNpcs.lookAt(level, naam, hoen, 80);
            }
        }
        for (String naam : ZES) {
            GuhwinterNpcs.say(level, naam, Component.translatable(W + key));
            GuhwinterNpcs.emote(level, naam, emote);
            spring(naam);
        }
        if (sprongen > 1) {
            nogEenSprong = 12;   // (a second jump, a bit later: tick)
        }
        for (ServerPlayer p : spelers()) {
            if (p.distanceToSqr(route.get(0)) < 64 * 64) {
                GuhwinterHoentjes.noway(p, soort);
            }
        }
        return 50;
    }

    private int nogEenSprong;

    private void spring(String naam) {
        if (GuhwinterNpcs.get(level, naam) instanceof WinterGuh g && g.onGround()) {
            g.setDeltaMovement(g.getDeltaMovement().add(0, 0.45, 0));
            g.hurtMarked = true;
        }
    }

    /** Anthony takes a photo (a flash and a click), and says something about it. */
    private int foto(String key) {
        Entity anthony = GuhwinterNpcs.get(level, "anthony");
        if (anthony != null) {
            level.sendParticles(ParticleTypes.END_ROD, anthony.getX(), anthony.getEyeY(), anthony.getZ(), 10, 0.25, 0.2, 0.25, 0.05);
            level.playSound(null, anthony.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.NEUTRAL, 1.0f, 1.7f);
            level.playSound(null, anthony.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.NEUTRAL, 1.0f, 1.9f);
        }
        GuhwinterNpcs.say(level, "anthony", Component.translatable(W + key));
        return 60;
    }

    /** The end: everybody still near gets the step; the guhs go back to their spots. */
    private void klaar() {
        Vec3 eind = route.get(route.size() - 1);
        for (ServerPlayer p : spelers()) {
            if (p.level() == level && p.distanceToSqr(eind) < 40 * 40 && WinterVerhaal.KERST.verder(p, WinterVerhaal.K_WANDELING)) {
                p.sendSystemMessage(Component.translatable(W + "klaar").withStyle(ChatFormatting.YELLOW));
                VerhaalStappen.rustig(p);
            }
        }
        laatLos();
        lopend = null;
    }

    private void laatLos() {
        for (String naam : GROEP) {
            GuhwinterNpcs.release(level, naam);
        }
        if (hoen != null && hoen.isAlive()) {
            hoen.discard();
        }
    }
}
