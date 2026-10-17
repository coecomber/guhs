package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 "Sponsguh", slice sp_zij: De Vliegende Guhlander ({@value SpZijLijnen#GUHLANDER}, four steps).
 * <ol start="0">
 *   <li>wait at the mooring ({@code guhlander_trap}, the ring of stones east of Patguh's rock): a visible count of
 *       {@value #WACHT_EERST} seconds over the hotbar (stepping off starts it again, never a hidden clock); then the ship comes
 *       out of the mist (the overlay {@code vliegende_guhlander} with its bubble stream) and the scene {@code guhlander};</li>
 *   <li>up the bubble stream to the deck ({@code guhlander_dek}): the scene {@code hakker} (the scary story by candle light,
 *       it was onions), then the captain blows you softly back down to the mooring;</li>
 *   <li>three ghost lanterns blew into town ({@code zij_lantaarn_1..3}, things of ONE player): walk over them;</li>
 *   <li>back to the mooring (a shorter wait when the ship is gone), up to the captain: the lanterns back; reward the
 *       spooklantaarn (one, and its recipe), then he blows you down again.</li>
 * </ol>
 * The ship: the overlay is real blocks (a column you can ride, a deck you can stand on: per-player fake blocks would leave
 * the server thinking you float), so it is shared: it stands while any player near needs it ({@link #nodig}) and goes when
 * nobody needs it and nobody is on board. Others see a ghost ship drift in over the Schelpstraat now and then, as the
 * DESIGN has it. The captain on deck is each player's own ({@link SpZijEigen}). Nobody is ever hurt by a fall within the
 * ship's footprint ({@link #opVal}).
 */
public final class Guhlander {
    static final String T = SpZij.P + "guhlander.";
    static final String LAAG = "vliegende_guhlander";
    static final String KAPITEIN = "kapitein";
    /** The waits at the mooring (seconds): the first time, and when the ship comes back for you. */
    static final int WACHT_EERST = 10, WACHT_TERUG = 5;
    static final int LANTAARNS = 3;
    static final double PAK = 1.7;
    /** Players further than this from the mooring no longer need the ship. */
    static final double NODIG_BEREIK = 128;
    /** The ship's footprint in template coordinates (the overlay at 126..146 x 160..168), with room round it. */
    static final int VOET_X0 = 120, VOET_X1 = 152, VOET_Z0 = 154, VOET_Z1 = 174;

    /** Players for whom the ship came (their wait was done): they need it until their step moves past it. */
    static final Set<UUID> SCHIP_VOOR = ConcurrentHashMap.newKeySet();
    /** When a player's wait at the mooring began (server ticks). */
    private static final Map<UUID, Long> WACHT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> AF_GEMELD = new ConcurrentHashMap<>();
    /** Is the overlay in the world now (as far as this server run knows), and did we clean up a leftover once? */
    static volatile boolean staat, opgeruimd;

    private Guhlander() {
    }

    static Verhaallijn ln() {
        return SpZijLijnen.GUHLANDER_LIJN;
    }

    // =====================================================================================================================
    // the wait at the mooring (pure: the game test drives it)
    // =====================================================================================================================

    /**
     * One tick of the wait: {@code opTrap} = the player stands on the mooring. Returns the seconds still to wait (0: done,
     * -1: not waiting). Stepping off forgets the wait.
     */
    static int wacht(UUID id, boolean opTrap, long nu, int seconden) {
        if (!opTrap) {
            WACHT.remove(id);
            return -1;
        }
        long sinds = WACHT.computeIfAbsent(id, u -> nu);
        long over = seconden * 20L - (nu - sinds);
        if (over <= 0) {
            WACHT.remove(id);
            return 0;
        }
        return (int) ((over + 19) / 20);
    }

    // =====================================================================================================================
    // every 5 ticks while the player is in town
    // =====================================================================================================================

    static void tick(ServerPlayer p, ServerLevel level, long nu) {
        int stap = ln().stap(p);
        Vec3 trap = SpZij.voeten(level, "guhlander_trap"), dek = SpZij.voeten(level, "guhlander_dek");
        if (trap == null || dek == null) {
            return;
        }
        if (stap == SpZijLijnen.G_LANTAARNS || stap >= SpZijLijnen.G_KLAAR) {
            SCHIP_VOOR.remove(p.getUUID());
        }
        // the captain of this player on his deck, while the ship is there for them
        boolean kapitein = staat && SCHIP_VOOR.contains(p.getUUID()) && (stap == SpZijLijnen.G_DEK || stap == SpZijLijnen.G_TERUG)
                && p.position().distanceTo(dek) < 64;
        Vec3 kp = SpZij.voeten(level, "spookkapitein");
        if (kapitein && kp != null) {
            SpZijEigen.wezen(level, p, KAPITEIN, Cast.SPOOKKAPITEIN, kp, -90f);
        } else if (!Praat.bezig(p)) {
            SpZijEigen.weg(level, p, KAPITEIN);         // (never while he still talks to you: the talk would close)
        }
        if (stap != SpZijLijnen.G_LANTAARNS) {
            SpZijEigen.wegAlles(level, p, "lantaarn_");
        }
        if (Cutscenes.bezig(p) || Praat.bezig(p) || !SpZij.open(p)) {
            return;
        }
        // (the ship stands for somebody else and you are on board already: it is there for you too)
        if (staat && (stap == SpZijLijnen.G_DEK || stap == SpZijLijnen.G_TERUG) && opDek(p.position(), dek)) {
            SCHIP_VOOR.add(p.getUUID());
        }
        // the wait at the mooring: before the ship, and whenever it is not there for you at steps 1 and 3
        boolean wachtNodig = stap == SpZijLijnen.G_WACHT || (stap == SpZijLijnen.G_DEK || stap == SpZijLijnen.G_TERUG) && !(staat && SCHIP_VOOR.contains(p.getUUID()));
        if (wachtNodig) {
            boolean opTrap = SpZij.bij(p.position(), trap, 2.5);
            int over = wacht(p.getUUID(), opTrap, nu, stap == SpZijLijnen.G_WACHT ? WACHT_EERST : WACHT_TERUG);
            if (over > 0) {
                SpZij.balk(p, T + "wacht", over);
                if (nu % 20 < 5) {
                    level.sendParticles(p, ParticleTypes.ASH, true, false, trap.x, trap.y + 4, trap.z, 30, 4, 3, 4, 0.0);
                    SpZij.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.5f + over * 0.05f);
                }
            } else if (over == 0) {
                schipKomt(p, level, stap);
            } else if (WACHT.containsKey(p.getUUID())) {
                WACHT.remove(p.getUUID());
            }
            return;
        }
        switch (stap) {
            case SpZijLijnen.G_DEK -> {
                if (opDek(p.position(), dek)) {
                    hakker(p, level);
                }
            }
            case SpZijLijnen.G_LANTAARNS -> lantaarns(p, level);
            case SpZijLijnen.G_TERUG -> {
                if (opDek(p.position(), dek)) {
                    terug(p, level);
                }
            }
            default -> {
            }
        }
    }

    static boolean opDek(Vec3 pos, Vec3 dek) {
        return SpZij.bij(pos, dek, 4.0) && pos.y >= dek.y - 1.0;
    }

    /** The wait is done: the ship comes for this player (step 0: with its scene; later: it simply drifts in). */
    static void schipKomt(ServerPlayer p, ServerLevel level, int stap) {
        SCHIP_VOOR.add(p.getUUID());
        plaats(level);
        SpZij.geluid(p, SoundEvents.BELL_BLOCK, 0.8f, 0.5f);
        if (stap == SpZijLijnen.G_WACHT) {
            BlockPos anker = SpZij.plek(level, "guhlander_trap");
            if (anker == null || !Cutscenes.speel(p, SpZijScenes.GUHLANDER, anker, Rotation.NONE, s -> ln().verder(s, SpZijLijnen.G_WACHT))) {
                ln().verder(p, SpZijLijnen.G_WACHT);
            }
        } else {
            SpZij.titel(p, T + "terug.titel", T + "terug.onder");
        }
    }

    /** Step 1 on deck: the scary story, then softly down to the mooring. */
    static void hakker(ServerPlayer p, ServerLevel level) {
        BlockPos anker = SpZij.plek(level, "guhlander_dek");
        if (anker == null) {
            return;
        }
        if (!Cutscenes.speel(p, SpZijScenes.HAKKER, anker, Rotation.NONE, s -> {
            if (ln().verder(s, SpZijLijnen.G_DEK)) {
                omlaag(s);
            }
        }) && !Cutscenes.bezig(p) && ln().verder(p, SpZijLijnen.G_DEK)) {
            omlaag(p);
        }
    }

    /** Step 2: the lanterns of ONE player round town. */
    static void lantaarns(ServerPlayer p, ServerLevel level) {
        for (int i = 1; i <= LANTAARNS; i++) {
            String naam = "lantaarn_" + i;
            Vec3 plek = SpZij.voeten(level, "zij_lantaarn_" + i);
            if (plek == null || ln().vlag(p, naam) || p.position().distanceTo(plek) > 64) {
                SpZijEigen.weg(level, p, naam);
                continue;
            }
            SpZijEigen.ding(level, p, naam, "guhs:spooklantaarn", plek.add(0, 0.5, 0), 1.2f);
            level.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, true, plek.x, plek.y + 1.0, plek.z, 2, 0.2, 0.3, 0.2, 0.01);
            if (SpZij.bij(p.position(), plek, PAK)) {
                pak(p, level, i);
            }
        }
    }

    /** One lantern picked up (true: it was new); the third moves the line on. */
    static boolean pak(ServerPlayer p, @Nullable ServerLevel level, int i) {
        String naam = "lantaarn_" + i;
        if (ln().stap(p) != SpZijLijnen.G_LANTAARNS || ln().vlag(p, naam)) {
            return false;
        }
        ln().vlag(p, naam, true);
        if (level != null) {
            SpZijEigen.weg(level, p, naam);
        }
        int n = gevonden(p);
        SpZij.geluid(p, SoundEvents.ITEM_PICKUP, 1f, 0.8f);
        SpZij.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.7f + n * 0.15f);
        SpZij.titel(p, T + "lantaarn.titel", T + "lantaarn.onder", n, LANTAARNS);
        if (n >= LANTAARNS) {
            SpZij.merk(p, T + "lantaarn.alle");
            ln().verder(p, SpZijLijnen.G_LANTAARNS);
        }
        return true;
    }

    static int gevonden(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= LANTAARNS; i++) {
            n += ln().vlag(p, "lantaarn_" + i) ? 1 : 0;
        }
        return n;
    }

    /** Step 3 on deck: the lanterns back to the captain; the reward; then down again. */
    static void terug(ServerPlayer p, ServerLevel level) {
        Entity kapitein = SpZijEigen.get(level, p, KAPITEIN);
        if (!ln().verder(p, SpZijLijnen.G_TERUG)) {
            return;
        }
        if (kapitein instanceof SponsguhWezen w) {
            w.zetAnimatie("juich");
            Praat.scene(p, DANK, List.of(new Praat.Regel(w, "", T + "dank.1"), new Praat.Regel(w, "", T + "dank.2"),
                    new Praat.Regel(w, "", T + "dank.3")));
        } else {
            SpZij.stem(p, naam(), T + "dank.1");
            SpZij.stem(p, naam(), T + "dank.3");
        }
        beloon(p);
        // down again when the talk is read (the listener), or in any case in a while
        nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 20 * 40, s -> {
            Vec3 dek = SpZij.voeten(s.level(), "guhlander_dek");
            if (dek != null && opDek(s.position(), dek) && !Praat.bezig(s)) {
                omlaag(s);
            }
        });
    }

    static final String DANK = "einde2_sp_zij_guhlander_dank";

    /** The reward of the line: one ghost lantern and its recipe (once). */
    static void beloon(ServerPlayer p) {
        if (SpZij.eenmalig(ln(), p, "spooklantaarn", new ItemStack(E2SponsguhFeature.SPOOKLANTAARN.get().asItem()))) {
            ResourceKey<Recipe<?>> recept = ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Guhs.id("spooklantaarn"));
            p.awardRecipesByKey(List.of(recept));
            SpZij.titel(p, T + "klaar.titel", T + "klaar.onder");
            SpZij.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 0.9f);
        }
    }

    /** The captain blows the player softly down to the mooring. */
    static void omlaag(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 trap = SpZij.voeten(level, "guhlander_trap");
        if (trap == null) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        p.teleportTo(level, trap.x, trap.y + 0.1, trap.z + 1.0, Set.of(), 180f, 0f, true);
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
        SpZij.geluid(p, SoundEvents.PHANTOM_FLAP, 0.8f, 0.7f);
        level.sendParticles(ParticleTypes.CLOUD, trap.x, trap.y + 0.5, trap.z + 1.0, 20, 0.4, 0.4, 0.4, 0.05);
    }

    // =====================================================================================================================
    // the ship (shared overlay)
    // =====================================================================================================================

    /** Puts the overlay (once; false: its chunks are not loaded yet). */
    static boolean plaats(ServerLevel level) {
        if (staat) {
            return true;
        }
        if (Plekken.plaatsLaag(level, SpZij.BB, LAAG)) {
            staat = true;
            opgeruimd = true;
            BlockPos dek = SpZij.plek(level, "guhlander_dek");
            if (dek != null) {
                level.sendParticles(ParticleTypes.SOUL, dek.getX() + 0.5, dek.getY() + 2, dek.getZ() + 0.5, 80, 8, 4, 3, 0.02);
            }
        }
        return staat;
    }

    /** Does anybody need the ship now (a player it came for, near the mooring, whose step still wants it)? */
    static boolean nodig(MinecraftServer server, Vec3 trap) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!SCHIP_VOOR.contains(p.getUUID()) || p.level().dimension() != GuheindeFeature.GUHEINDE) {
                continue;
            }
            int stap = ln().stap(p);
            if ((stap <= SpZijLijnen.G_DEK || stap == SpZijLijnen.G_TERUG || Cutscenes.bezig(p)) && p.position().distanceTo(trap) < NODIG_BEREIK) {
                return true;
            }
        }
        return false;
    }

    /** Once a second: the ship comes for whoever needs it, and goes when nobody does (and nobody stands on board). */
    static void serverTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel level = SpZij.guheinde(server);
        Vec3 trap = level == null ? null : SpZij.voeten(level, "guhlander_trap");
        if (trap == null) {
            return;
        }
        boolean nodig = nodig(server, trap);
        if (nodig) {
            plaats(level);
            return;
        }
        if (!staat && opgeruimd) {
            return;
        }
        // nobody needs it: away (also a leftover of a run that ended with the ship out), once nobody is on board
        BlockPos o = SpZij.oorsprong(level);
        Plekken.Laag laag = Plekken.blad(SpZij.BB).lagen().get(LAAG);
        if (o == null || laag == null || !level.hasChunksAt(o.offset(laag.pos()), o.offset(laag.pos()).offset(laag.grootte()))) {
            return;
        }
        BlockPos a = o.offset(laag.pos());
        AABB aanBoord = new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(a.offset(laag.grootte()))).inflate(1, 2, 1).move(0, 4, 0);
        if (!level.getEntitiesOfClass(ServerPlayer.class, aanBoord, q -> q.getY() > a.getY() + 6).isEmpty()) {
            return;
        }
        for (ServerPlayer q : server.getPlayerList().getPlayers()) {
            SpZijEigen.weg(level, q, KAPITEIN);
        }
        if (Plekken.ruimLaag(level, SpZij.BB, LAAG)) {
            staat = false;
            opgeruimd = true;
        }
    }

    /** No fall hurts within the ship's footprint (a jump off the deck, a login on a deck that went away). */
    static void opVal(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        BlockPos o = SpZij.oorsprong(p.level());
        if (o == null) {
            return;
        }
        int x = p.getBlockX() - o.getX(), z = p.getBlockZ() - o.getZ();
        if (x >= VOET_X0 && x <= VOET_X1 && z >= VOET_Z0 && z <= VOET_Z1) {
            event.setDamageMultiplier(0f);
        }
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    /** {@code SponsguhWezen.BIJ_KLIK}: the ghost captain (only ever a player's own copy on the deck). */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        if (w.cast() != Cast.SPOOKKAPITEIN || !Boomkoepel.isOnze(w, p, "spookkapitein")) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        w.zetAnimatie("spook");
        int stap = ln().stap(p);
        ServerLevel level = p.level();
        Vec3 dek = SpZij.voeten(level, "guhlander_dek");
        if (stap == SpZijLijnen.G_DEK && dek != null && opDek(p.position(), dek)) {
            hakker(p, level);
        } else if (stap == SpZijLijnen.G_TERUG && dek != null && opDek(p.position(), dek)) {
            terug(p, level);
        } else if (stap >= SpZijLijnen.G_KLAAR) {
            GuhQuests.say(p, w, T + "na." + (1 + Math.floorMod(p.level().getGameTime() / 40, 2)));
        } else {
            GuhQuests.say(p, w, T + "klik");
        }
        return true;
    }

    static String naam() {
        return "entity.guhs.spookkapitein";
    }

    static void vergeet(UUID id) {
        WACHT.remove(id);
        AF_GEMELD.remove(id);
        SCHIP_VOOR.remove(id);
    }

    /** (tests, dev) the server forgets the ship's state (a new run). */
    static void reset() {
        staat = false;
        opgeruimd = false;
        SCHIP_VOOR.clear();
        WACHT.clear();
    }
}
