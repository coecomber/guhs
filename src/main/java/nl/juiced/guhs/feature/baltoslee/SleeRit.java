package nl.juiced.guhs.feature.baltoslee;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * One sled ride over the Nomguh route (one per player), on the server.
 * <ul>
 *   <li>{@link Modus#TOCHT}: the medicine ride (balto's questline starts it through {@link SleeTocht#startMedicijn}): through a
 *   heavy storm to the berghut (pause: Baltoguh fetches the medicine chest, balto calls {@link #verder}), then back with a time
 *   limit; on the way back, at the dieptepunt, the storm is at its worst (pause: the wolf moment, {@link #stormKlaartOp},
 *   verder). On time at the hospital = AANKOMST; too late = TE_LAAT ("Njeg, nog een keer!").</li>
 *   <li>{@link Modus#SPRINT}: the sledesprint against Steele-Mika (after the story): there and around the berghut and back
 *   without stopping, a lighter storm by level, your time on the board; beat Steele-Mika for extra sledebelletjes.</li>
 * </ul>
 * On the way: gusts push the sled sideways ({@link SleeRijden#windvlaag}), avalanches come down the slopes (be on the far side
 * of the track when the snow crosses it, or you are buried: dig out, a little back), ice bridges are slippery and narrow
 * (fall off: plof, back to the bridge's start), the dogs get cold in the storm (slower) and warm up when you stop at a
 * vuurkorf. Nothing ever hurts: the rider is protected like in every minigame, and "failing" is a cute puff of snow.
 * <p>
 * The rider's game drives the sled and reports every tick ({@link #meld}); the server checks each report against the
 * sled's speed and the current generation of resets, and handles everything that happens on that stretch. Without reports
 * (game tests) the server rides by itself, with an optional autopilot.
 */
public final class SleeRit {
    public enum Modus { TOCHT, SPRINT }

    public enum Einde { AANKOMST, TE_LAAT, GESTOPT, WEG, FINISH }

    public static final String GAME_TOCHT = "nomguh_tocht", GAME_SPRINT = "sledesprint";
    public static final int AFTEL = 60, PAUZE_MAX = 600, RUST_TICKS = 70, VAST_LAWINE = 50, VAST_PLOF = 40, STIL = 8, UITSTAP_HOUD = 30;
    /** Stop this long (ticks) near a vuurkorf to rest. */
    public static final int RUST_STIL = 10;
    /** The avalanche: you are safe on the far side of the track, at least this part of its half width away from the middle. */
    public static final double LAWINE_VEILIG = 0.35;
    /** Steele-Mika's speed per level (blocks per tick; you trot at 0.28 and run at 0.46). */
    public static final double[] STEELE_SNELHEID = {0.25, 0.31, 0.37};
    /** The storm of the sledesprint per level; the medicine ride's storm. */
    public static final float[] SPRINT_STORM = {0.22f, 0.42f, 0.65f};
    public static final float TOCHT_STORM_HEEN = 0.72f, TOCHT_STORM_TERUG = 0.85f, STORM_HELDER = 0.1f;
    /** Prizes of the sledesprint (sledebelletjes): per race, beating Steele-Mika, the first race of a level, a personal record. */
    public static final int MUNTEN_BASIS = 3, MUNTEN_STEELE = 3, MUNTEN_EERSTE = 2, MUNTEN_RECORD = 2;

    private static final Map<UUID, SleeRit> RIJDERS = new ConcurrentHashMap<>();

    public final UUID id = UUID.randomUUID();
    public final UUID speler;
    public final Modus modus;
    public final Niveau niveau;
    public final RitRoute route;
    private final ResourceKey<Level> dim;
    private final int seed;
    private UUID slee;
    @Nullable
    private UUID steeleSlee, kopie, steeleNpc;

    private int fase = SleeEntity.WACHT, pauze = SleeEntity.GEEN, been;
    private SleeRijden.Stand stand = new SleeRijden.Stand();
    private int aftel, pauzeTicks, vastTicks, stilTicks, uitstappen;
    private float storm, warmte = 100;
    private boolean helder, kist, stopt;
    /** Has the rider's game ever reported (else the server rides by itself: game tests)? */
    private boolean clientGezien;
    private boolean berghutGemeld, dieptepuntGemeld;
    private int tijdTerug, limiet, rijTijd;
    private long lastTick, clientTick = -1000;
    private final Set<Integer> lawineGedaan = new HashSet<>(), rustGebruikt = new HashSet<>(), ijsGevallen = new HashSet<>(), rustGehint = new HashSet<>();
    private int bedolven, ontweken, plof, gerust;
    // Steele-Mika (sledesprint)
    private final SleeRijden.Stand steele = new SleeRijden.Stand();
    private int steeleBeen, steeleKlaar = -1, voorsprongTeken, steeleSpraak;
    /** (Tests) 0 = off, 1 = rides well (dodges, stays on the bridges), 2 = rides badly (into every avalanche). */
    public int autopiloot;
    /** (Tests) the autopilot stops at the rest points. */
    public boolean autoRust;

    private SleeRit(ServerPlayer player, Modus modus, Niveau niveau, RitRoute route) {
        this.speler = player.getUUID();
        this.modus = modus;
        this.niveau = niveau;
        this.route = route;
        this.dim = player.level().dimension();
        this.seed = player.getRandom().nextInt();
        this.lastTick = player.level().getGameTime();
        this.limiet = route.tijd(modus == Modus.TOCHT ? "makkelijk" : niveau.id());
        this.storm = modus == Modus.TOCHT ? TOCHT_STORM_HEEN : SPRINT_STORM[niveau.ordinal()];
    }

    // --- who rides ----------------------------------------------------------------------------------------------------------

    @Nullable
    public static SleeRit van(Player player) {
        return RIJDERS.get(player.getUUID());
    }

    public static boolean rijdt(Player player) {
        return RIJDERS.containsKey(player.getUUID());
    }

    public static boolean rijdt(Player player, Modus modus) {
        SleeRit r = RIJDERS.get(player.getUUID());
        return r != null && r.modus == modus;
    }

    public UUID slee() {
        return slee;
    }

    public int fase() {
        return fase;
    }

    public int pauze() {
        return pauze;
    }

    public int been() {
        return been;
    }

    public SleeRijden.Stand stand() {
        return stand;
    }

    public float storm() {
        return storm;
    }

    public float warmte() {
        return warmte;
    }

    public boolean kist() {
        return kist;
    }

    public int bedolven() {
        return bedolven;
    }

    public int ontweken() {
        return ontweken;
    }

    public int plof() {
        return plof;
    }

    public int gerust() {
        return gerust;
    }

    public int rijTijd() {
        return rijTijd;
    }

    public int tijdTerug() {
        return tijdTerug;
    }

    public int limiet() {
        return limiet;
    }

    public int steeleKlaar() {
        return steeleKlaar;
    }

    public boolean bezig() {
        return RIJDERS.get(speler) == this;
    }

    // --- starting ------------------------------------------------------------------------------------------------------------

    /**
     * Starts a ride for player over this route (world coordinates). Returns the ride, or null (already riding, busy with
     * another game). kopie = Baltoguh's story copy (hidden at home while he pulls the sled), npc = Steele-Mika (who talks).
     */
    @Nullable
    public static SleeRit start(ServerPlayer player, NomguhRoute wereld, Modus modus, Niveau niveau, @Nullable GuhEntity kopie,
                                @Nullable GuhNpcEntity npc) {
        ServerLevel level = player.level();
        String game = modus == Modus.TOCHT ? GAME_TOCHT : GAME_SPRINT;
        if (rijdt(player) || Minigames.busyElsewhere(player, game)) {
            return null;
        }
        RitRoute route = RitRoute.maak(level, wereld);
        SleeRit rit = new SleeRit(player, modus, niveau, route);
        SleeBaan b = route.baan(0);
        double w = b.breedte(0);
        SleeEntity sled = BaltoSleeFeature.SLEE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (sled == null) {
            return null;
        }
        rit.stand = new SleeRijden.Stand(0, modus == Modus.SPRINT ? -0.45 * w : 0, 0, 0);
        sled.zet(route, modus == Modus.TOCHT ? SleeEntity.TOCHT : SleeEntity.SPRINT, niveau.ordinal(), rit.seed);
        sled.volg(SleeEntity.WACHT, SleeEntity.GEEN, 0, rit.stand);
        level.addFreshEntity(sled);
        rit.slee = sled.getUUID();
        if (modus == Modus.SPRINT) {
            SleeEntity st = BaltoSleeFeature.SLEE.get().create(level, EntitySpawnReason.TRIGGERED);
            if (st != null) {
                rit.steele.lat = 0.45 * w;
                st.zet(route, SleeEntity.STEELE, niveau.ordinal(), rit.seed);
                st.volg(SleeEntity.WACHT, SleeEntity.GEEN, 0, rit.steele);
                level.addFreshEntity(st);
                rit.steeleSlee = st.getUUID();
            }
        }
        if (kopie != null) {
            rit.kopie = kopie.getUUID();
            verstop(kopie, true);
        }
        rit.steeleNpc = npc == null ? null : npc.getUUID();
        RIJDERS.put(player.getUUID(), rit);

        player.stopRiding();
        player.closeContainer();
        Vec3 at = sled.position();
        player.teleportTo(level, at.x, at.y, at.z, java.util.Set.of(), sled.getYRot(), 10f, true);
        player.startRiding(sled, true, true);
        Minigames.startKeeping(player);
        level.playSound(null, sled.blockPosition(), BaltoSleeFeature.BELLEN.get(), SoundSource.PLAYERS, 1f, 1f);
        if (modus == Modus.TOCHT) {
            player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.tocht.start").withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.sprint.start", niveau.naam()).withStyle(ChatFormatting.AQUA));
            rit.steeleZegt(player, "gui.guhs.baltoslee.steele.start");
        }
        player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.besturing").withStyle(ChatFormatting.GRAY));
        rit.sync(level, sled);
        if (modus == Modus.TOCHT) {
            SleeTocht.meld(player, SleeTocht.Moment.START);
        }
        return rit;
    }

    /** Baltoguh's story copy stays at home, invisible, while "he" pulls the sled (and comes back when it's over). */
    static void verstop(GuhEntity kopie, boolean ja) {
        kopie.setInvisible(ja);
        kopie.setSilent(ja);
        if (ja) {
            kopie.getPersistentData().putBoolean(VERSTOPT, true);
        } else {
            kopie.getPersistentData().remove(VERSTOPT);
        }
    }

    /** A copy that was hidden while a ride pulled it (and the ride is gone: the server stopped): it comes back. */
    static final String VERSTOPT = "guhs_baltoslee_verstopt";

    static boolean trektNog(UUID kopie) {
        for (SleeRit r : RIJDERS.values()) {
            if (kopie.equals(r.kopie)) {
                return true;
            }
        }
        return false;
    }

    // --- every tick ------------------------------------------------------------------------------------------------------------

    /** A sled's server tick: its ride drives it (the rider's sled ticks the whole ride). False: no ride (it should go). */
    static boolean tickVan(SleeEntity sled) {
        for (SleeRit rit : RIJDERS.values()) {
            if (sled.getUUID().equals(rit.slee)) {
                rit.tick((ServerLevel) sled.level(), sled);
                return true;
            }
            if (sled.getUUID().equals(rit.steeleSlee)) {
                return true;
            }
        }
        return false;
    }

    /** One tick of the ride (tests call it directly to ride faster). */
    public void tick(ServerLevel level) {
        if (level.getEntity(slee) instanceof SleeEntity sled) {
            tick(level, sled);
        } else {
            einde(level, Einde.WEG);
        }
    }

    void tick(ServerLevel level, SleeEntity sled) {
        long now = level.getGameTime();
        lastTick = now;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(speler);
        if (player == null || !player.isAlive() || player.level() != level || player.distanceToSqr(sled) > 64 * 64) {
            einde(level, Einde.WEG);
            return;
        }
        Minigames.keep(player);
        if (player.getVehicle() != sled && !stopt) {
            player.startRiding(sled, true, true);                       // (you can't get off on the way: hold sneak to stop)
        }
        switch (fase) {
            case SleeEntity.WACHT -> aftellen(level, player, sled);
            case SleeEntity.RIJDT -> {
                if (!clientGezien || now - clientTick > STIL) {      // no word from the rider's game: the server rides itself
                    double sOud = stand.s;
                    SleeRijden.stap(stand, autoInvoer(), route, been, storm, warmte, seed);
                    verwerk(level, player, sled, sOud);
                }
                if (fase == SleeEntity.RIJDT) {
                    rusten(level, player, sled);
                }
            }
            case SleeEntity.PAUZE -> {
                pauzeTicks++;
                if (pauze == SleeEntity.RUST) {
                    warmte = Math.min(100, warmte + 1.6f);
                    if (pauzeTicks % 20 == 5) {
                        level.sendParticles(ParticleTypes.HEART, sled.getX(), sled.getY() + 1.2, sled.getZ(), 3, 1.4, 0.3, 1.4, 0);
                    }
                    if (pauzeTicks >= RUST_TICKS) {
                        fase = SleeEntity.RIJDT;
                        pauze = SleeEntity.GEEN;
                        player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.rust.verder").withStyle(ChatFormatting.GOLD));
                        geluid(level, sled, BaltoSleeFeature.WOEF.get(), 1f, 1.1f);
                    }
                } else if (pauzeTicks >= PAUZE_MAX) {
                    if (pauze == SleeEntity.DIEPTEPUNT && !helder) {
                        stormKlaartOp(player);                     // (nobody told the story: the storm clears by itself)
                    }
                    verder(player);
                }
            }
            case SleeEntity.VAST -> {
                if (--vastTicks <= 0) {
                    fase = SleeEntity.RIJDT;
                    clientTick = now;
                    player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.weer_los").withStyle(ChatFormatting.GOLD));
                }
            }
            default -> {
            }
        }
        if (!bezig()) {
            return;
        }
        weer(level);
        klok(level, player);
        if (!bezig()) {
            return;
        }
        if (modus == Modus.SPRINT) {
            tegenstander(level, player);
        }
        if (player.isShiftKeyDown() && fase != SleeEntity.KLAAR) {
            if (++uitstappen >= UITSTAP_HOUD) {
                player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.gestopt").withStyle(ChatFormatting.LIGHT_PURPLE));
                einde(level, Einde.GESTOPT);
                return;
            }
        } else {
            uitstappen = 0;
        }
        sync(level, sled);
        if (player.getVehicle() == sled) {
            sled.positionRider(player);
        }
    }

    private void aftellen(ServerLevel level, ServerPlayer player, SleeEntity sled) {
        if (aftel % 20 == 0 && aftel < AFTEL) {
            int n = 3 - aftel / 20;
            titel(player, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.translatable(modus == Modus.TOCHT ? "gui.guhs.baltoslee.klaar.tocht" : "gui.guhs.baltoslee.klaar.sprint"), 0, 18, 2);
            notifySound(player,SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1f);
        }
        if (++aftel >= AFTEL) {
            fase = SleeEntity.RIJDT;
            clientTick = level.getGameTime();
            titel(player, Component.translatable("gui.guhs.baltoslee.hup").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 0, 20, 10);
            notifySound(player,SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 2f);
            geluid(level, sled, BaltoSleeFeature.WOEF.get(), 1.2f, 1f);
            geluid(level, sled, BaltoSleeFeature.BELLEN.get(), 1f, 1.1f);
        }
    }

    /** The storm follows the story: heavy on the medicine ride (worst near the dieptepunt, clear after the howl), by level in the race. */
    private void weer(ServerLevel level) {
        float doel;
        if (modus == Modus.SPRINT) {
            doel = SPRINT_STORM[niveau.ordinal()];
        } else if (helder) {
            doel = STORM_HELDER;
        } else if (been == 0) {
            doel = TOCHT_STORM_HEEN;
        } else {
            doel = TOCHT_STORM_TERUG;
            for (RitRoute.Zone z : route.zones(1, RitRoute.Soort.DIEPTEPUNT)) {
                double tot = z.s0() - stand.s;
                if (tot < 32 && tot > -2) {
                    doel = Math.max(doel, 1f - (float) Math.max(0, tot) / 32f * 0.15f);
                }
            }
        }
        storm += Mth.clamp(doel - storm, -0.01f, 0.01f);
        if (fase == SleeEntity.RIJDT) {
            warmte = Math.max(0, warmte - storm * 0.04f);
        }
    }

    /** The clocks: the race time, the time left for the way back (too late: TE_LAAT). */
    private void klok(ServerLevel level, ServerPlayer player) {
        if (fase == SleeEntity.WACHT || fase == SleeEntity.KLAAR) {
            return;
        }
        rijTijd++;
        if (modus == Modus.TOCHT && been == 1 && !(fase == SleeEntity.PAUZE && pauze != SleeEntity.RUST)) {
            tijdTerug++;
            int over = limiet - tijdTerug;
            if (over == 20 * 30 || over == 20 * 10) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.tijd.bijna", over / 20).withStyle(ChatFormatting.RED));
            }
            if (tijdTerug >= limiet) {
                titel(player, Component.translatable("gui.guhs.baltoslee.te_laat").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.baltoslee.te_laat.sub"), 5, 60, 20);
                player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.te_laat.uitleg").withStyle(ChatFormatting.LIGHT_PURPLE));
                einde(level, Einde.TE_LAAT);
            }
        }
    }

    // --- the rider's reports --------------------------------------------------------------------------------------------------------

    /** The rider's game: sled in generation gen of its resets, on leg been at s, lat, going v. */
    public void meld(ServerPlayer player, int gen, int beenC, double s, double lat, double v, double latV) {
        ServerLevel level = player.level();
        if (fase != SleeEntity.RIJDT || !(level.getEntity(slee) instanceof SleeEntity sled) || gen != sled.gen() || beenC != been) {
            return;                                                  // (about an old spot: a reset or a new leg won)
        }
        long now = level.getGameTime();
        long dt = Math.max(1, Math.min(10, now - clientTick));
        clientTick = now;
        clientGezien = true;
        SleeBaan b = route.baan(been);
        double sOud = stand.s;
        double maxS = sOud + dt * SleeRijden.TOP * 1.25 + 0.3;        // (no faster than the dogs can run)
        stand.s = Mth.clamp(s, sOud, Math.min(maxS, b.lengte));
        double w = b.breedte(stand.s);
        boolean ijs = route.zone(been, RitRoute.Soort.IJSBRUG, stand.s) != null;
        double rand = ijs ? w + SleeRijden.IJS_RAND + 1.0 : w + SleeRijden.BUITEN;
        stand.lat = Mth.clamp(lat, stand.lat - 0.5 * dt, stand.lat + 0.5 * dt);
        stand.lat = Mth.clamp(stand.lat, -rand, rand);
        stand.v = Mth.clamp(v, 0, SleeRijden.TOP * 1.2);
        stand.latV = Mth.clamp(latV, -0.4, 0.4);
        verwerk(level, player, sled, sOud);
    }

    /** What happens on the stretch from sOud to where the sled is now: avalanches, ice bridges, the dieptepunt, the leg's end. */
    private void verwerk(ServerLevel level, ServerPlayer player, SleeEntity sled, double sOud) {
        SleeBaan b = route.baan(been);
        List<RitRoute.Zone> zones = route.zones(been);
        for (int i = 0; i < zones.size(); i++) {
            RitRoute.Zone z = zones.get(i);
            if (z.soort() == RitRoute.Soort.LAWINE && !lawineGedaan.contains(i) && sOud < z.midden() && stand.s >= z.midden()) {
                lawineGedaan.add(i);
                double w = b.breedte(z.midden());
                Vec3 gevaar = b.op(z.midden(), z.kant() * (w + 2.5));
                level.sendParticles(ParticleTypes.SNOWFLAKE, gevaar.x, gevaar.y + 1.5, gevaar.z, 80, 2.5, 1.5, 2.5, 0.25);
                level.sendParticles(ParticleTypes.CLOUD, gevaar.x, gevaar.y + 0.8, gevaar.z, 30, 2.5, 0.8, 2.5, 0.05);
                geluid(level, sled, BaltoSleeFeature.LAWINE.get(), 1.4f, 1f);
                if (stand.lat * -z.kant() >= LAWINE_VEILIG * w) {
                    ontweken++;
                    level.broadcastEntityEvent(sled, SleeEntity.EV_ONTWEKEN);
                    player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.lawine.ontweken").withStyle(ChatFormatting.GREEN));
                    adv(player, "balto_slee_lawine");
                } else {
                    bedolven++;
                    lawineGedaan.remove(i);                         // (the next time past it counts again)
                    level.broadcastEntityEvent(sled, SleeEntity.EV_BEDOLVEN);
                    titel(player, Component.translatable("gui.guhs.baltoslee.lawine.boef").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.baltoslee.lawine.bedolven"), 0, 40, 10);
                    zetTerug(level, sled, z.s0() - 6, -z.kant() * 0.55 * b.breedte(Math.max(0, z.s0() - 6)), VAST_LAWINE);
                    return;
                }
            }
            if (z.soort() == RitRoute.Soort.IJSBRUG && sOud < z.s1() && stand.s >= z.s1() && !ijsGevallen.contains(i)) {
                adv(player, "balto_slee_ijsbrug");                // (over the whole bridge without falling off)
            }
            if (z.soort() == RitRoute.Soort.IJSBRUG && sOud < z.s0() && stand.s >= z.s0()) {
                ijsGevallen.remove(i);
                player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.ijsbrug").withStyle(ChatFormatting.AQUA));
                geluid(level, sled, BaltoSleeFeature.IJS.get(), 0.8f, 1f);
            }
        }
        if (SleeRijden.vanDeBrug(route, been, stand)) {
            RitRoute.Zone z = route.zone(been, RitRoute.Soort.IJSBRUG, stand.s);
            plof++;
            ijsGevallen.add(zones.indexOf(z));
            level.broadcastEntityEvent(sled, SleeEntity.EV_PLOF);
            titel(player, Component.translatable("gui.guhs.baltoslee.plof").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.baltoslee.plof.sub"), 0, 40, 10);
            geluid(level, sled, BaltoSleeFeature.PLOF.get(), 1.2f, 1f);
            zetTerug(level, sled, (z == null ? stand.s : z.s0()) - 3, 0, VAST_PLOF);
            return;
        }
        if (modus == Modus.TOCHT && been == 1 && !dieptepuntGemeld) {
            for (RitRoute.Zone z : route.zones(1, RitRoute.Soort.DIEPTEPUNT)) {
                if (stand.s >= z.s0()) {
                    dieptepuntGemeld = true;
                    stand.v = 0;
                    pauzeer(SleeEntity.DIEPTEPUNT);
                    titel(player, Component.translatable("gui.guhs.baltoslee.dieptepunt").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.baltoslee.dieptepunt.sub"), 10, 70, 20);
                    sync(level, sled);
                    SleeTocht.meld(player, SleeTocht.Moment.DIEPTEPUNT);
                    return;
                }
            }
        }
        if (stand.s >= b.lengte - 0.05) {
            eindeBeen(level, player, sled);
        }
    }

    /** Stop near a vuurkorf to rest: the dogs warm up (and get their paws rubbed). */
    private void rusten(ServerLevel level, ServerPlayer player, SleeEntity sled) {
        List<RitRoute.Zone> zones = route.zones(been);
        for (int i = 0; i < zones.size(); i++) {
            RitRoute.Zone z = zones.get(i);
            if (z.soort() != RitRoute.Soort.RUST || !z.in(stand.s) || rustGebruikt.contains(i)) {
                continue;
            }
            if (rustGehint.add(been * 100 + i)) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.rust.hint").withStyle(ChatFormatting.GOLD));
            }
            if (stand.v < 0.04) {
                if (++stilTicks >= RUST_STIL) {
                    stilTicks = 0;
                    rustGebruikt.add(i);
                    gerust++;
                    pauzeer(SleeEntity.RUST);
                    stand.v = 0;
                    level.broadcastEntityEvent(sled, SleeEntity.EV_RUST);
                    geluid(level, sled, BaltoSleeFeature.VUURKORF.get(), 1f, 1f);
                    player.sendSystemMessage(Component.translatable(modus == Modus.TOCHT ? "gui.guhs.baltoslee.rust.tocht" : "gui.guhs.baltoslee.rust.sprint")
                            .withStyle(ChatFormatting.GOLD));
                    adv(player, "balto_slee_rustpunt");
                }
            } else {
                stilTicks = 0;
            }
            return;
        }
        stilTicks = 0;
    }

    private void pauzeer(int reden) {
        fase = SleeEntity.PAUZE;
        pauze = reden;
        pauzeTicks = 0;
    }

    /** Buried or fallen: the sled goes back to (s, lat) and the dogs dig it out first. */
    private void zetTerug(ServerLevel level, SleeEntity sled, double s, double lat, int vast) {
        stand = new SleeRijden.Stand(Math.max(0, s), lat, 0, 0);
        fase = SleeEntity.VAST;
        vastTicks = vast;
        clientTick = level.getGameTime();
        sled.nieuweGen();
        sled.sneeuwwolk(60);
        geluid(level, sled, BaltoSleeFeature.PLOF.get(), 1f, 0.8f);
        sync(level, sled);
        sled.sneeuwwolk(40);
    }

    /** The end of a leg: at the berghut (pause, or around it in the race), or at the finish. */
    private void eindeBeen(ServerLevel level, ServerPlayer player, SleeEntity sled) {
        if (been == 0) {
            if (modus == Modus.TOCHT) {
                if (!berghutGemeld) {
                    berghutGemeld = true;
                    kist = true;
                    stand.v = 0;
                    pauzeer(SleeEntity.BERGHUT);
                    titel(player, Component.translatable("gui.guhs.baltoslee.berghut").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.baltoslee.berghut.sub"), 5, 60, 15);
                    geluid(level, sled, BaltoSleeFeature.WOEF.get(), 1f, 1.2f);
                    sync(level, sled);
                    SleeTocht.meld(player, SleeTocht.Moment.BERGHUT);
                }
            } else {
                nieuwBeen(level, stand.v * 0.6);
                level.broadcastEntityEvent(sled, SleeEntity.EV_KEER);
                player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.sprint.keer").withStyle(ChatFormatting.GOLD));
                geluid(level, sled, BaltoSleeFeature.BELLEN.get(), 1f, 1.2f);
            }
            return;
        }
        if (modus == Modus.TOCHT) {
            if (tijdTerug <= limiet) {
                aankomst(level, player);
            }
        } else {
            finish(level, player);
        }
    }

    private void nieuwBeen(ServerLevel level, double v) {
        been = 1;
        double w = route.baan(1).breedte(0);
        stand = new SleeRijden.Stand(0, modus == Modus.SPRINT ? -0.45 * w : 0, v, 0);
        lawineGedaan.clear();
        rustGebruikt.clear();
        ijsGevallen.clear();
        stilTicks = 0;
        clientTick = level.getGameTime();
    }

    // --- the story's hooks (SleeTocht) -----------------------------------------------------------------------------------------------

    /** After the berghut / the dieptepunt: go on. */
    public void verder(ServerPlayer player) {
        if (fase != SleeEntity.PAUZE || pauze == SleeEntity.RUST) {
            return;
        }
        ServerLevel level = player.level();
        if (pauze == SleeEntity.BERGHUT) {
            nieuwBeen(level, 0);
            player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.terug", tijdTekst(limiet))
                    .withStyle(ChatFormatting.AQUA));
            if (level.getEntity(slee) instanceof SleeEntity s) {
                geluid(level, s, BaltoSleeFeature.WOEF.get(), 1.2f, 1f);
                geluid(level, s, BaltoSleeFeature.BELLEN.get(), 1f, 1f);
            }
        }
        fase = SleeEntity.RIJDT;
        pauze = SleeEntity.GEEN;
        clientTick = level.getGameTime();
    }

    /** After the wolf howl: the storm clears for the rest of the ride. */
    public void stormKlaartOp(ServerPlayer player) {
        if (!helder) {
            helder = true;
            player.sendOverlayMessage(Component.translatable("gui.guhs.baltoslee.helder").withStyle(ChatFormatting.AQUA));
        }
    }

    // --- the end ----------------------------------------------------------------------------------------------------------------------

    private void aankomst(ServerLevel level, ServerPlayer player) {
        titel(player, Component.translatable("gui.guhs.baltoslee.aankomst").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.baltoslee.aankomst.sub", tijdTekst(Math.max(0, limiet - tijdTerug))), 5, 70, 20);
        player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.aankomst.uitleg").withStyle(ChatFormatting.GOLD));
        notifySound(player,BaltoSleeFeature.FANFARE.get(), SoundSource.PLAYERS, 1f, 1f);
        adv(player, "balto_slee_tocht");
        CompoundTag d = data(player);
        d.putInt("Tochten", d.getIntOr("Tochten", 0) + 1);
        einde(level, Einde.AANKOMST);
    }

    private void finish(ServerLevel level, ServerPlayer player) {
        int tijd = rijTijd;
        boolean gewonnen = steeleKlaar < 0 || tijd < steeleKlaar;
        CompoundTag d = data(player);
        String n = niveau.id();
        int ritten = d.getIntOr("Ritten_" + n, 0) + 1;
        int oud = d.getIntOr("Best_" + n, 0);
        boolean record = oud <= 0 || tijd < oud;
        d.putInt("Ritten_" + n, ritten);
        if (record) {
            d.putInt("Best_" + n, tijd);
        }
        if (gewonnen) {
            d.putInt("Gewonnen_" + n, d.getIntOr("Gewonnen_" + n, 0) + 1);
        }
        int munten = niveau.munten(MUNTEN_BASIS + (gewonnen ? MUNTEN_STEELE : 0) + (ritten == 1 ? MUNTEN_EERSTE : 0) + (record && oud > 0 ? MUNTEN_RECORD : 0));
        Minigames.give(player, new ItemStack(BaltoSleeFeature.SLEDEBELLETJE.get(), munten));
        titel(player, Component.translatable(gewonnen ? "gui.guhs.baltoslee.sprint.gewonnen" : "gui.guhs.baltoslee.sprint.finish")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.baltoslee.sprint.tijd", tijdTekst(tijd)).withStyle(ChatFormatting.WHITE), 0, 60, 15);
        player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.sprint.uitslag", niveau.naam(), tijdTekst(tijd), munten, ontweken, bedolven)
                .withStyle(ChatFormatting.AQUA));
        if (record && oud > 0) {
            player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.sprint.record", tijdTekst(tijd), tijdTekst(oud)).withStyle(ChatFormatting.YELLOW));
        }
        steeleZegt(player, gewonnen ? "gui.guhs.baltoslee.steele.verloren" : "gui.guhs.baltoslee.steele.gewonnen");
        if (gewonnen) {
            player.sendSystemMessage(Component.translatable("gui.guhs.baltoslee.steele.uitgelachen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Scorebord.submit(player, bord(niveau), tijd, true);
        notifySound(player,gewonnen ? BaltoSleeFeature.FANFARE.get() : SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1f, 1f);
        adv(player, "balto_slee_sprint");
        adv(player, "balto_slee_sprint_" + n);
        if (gewonnen) {
            adv(player, "balto_slee_steele");
            if (niveau == Niveau.LASTIG) {
                adv(player, "balto_slee_lastig");
            }
        }
        einde(level, Einde.FINISH);
    }

    /** The board of a level: sledesprint_makkelijk / _medium / _lastig. */
    public static String bord(Niveau niveau) {
        return GAME_SPRINT + "_" + niveau.id();
    }

    /** Ends the ride (the sleds go in a puff of snow, the rider gets off at the hospital or the stable). */
    public void einde(ServerLevel level, Einde einde) {
        if (!bezig()) {
            return;
        }
        RIJDERS.remove(speler);
        stopt = true;
        fase = SleeEntity.KLAAR;
        ServerLevel home = level.getServer().getLevel(dim);
        ServerLevel lv = home != null ? home : level;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(speler);
        if (player != null && player.getVehicle() instanceof SleeEntity) {
            player.stopRiding();
        }
        for (UUID id : new UUID[]{slee, steeleSlee}) {
            Entity e = id == null ? null : lv.getEntity(id);
            if (e instanceof SleeEntity s) {
                s.sneeuwwolk(24);
                s.discard();
            }
        }
        if (kopie != null && lv.getEntity(kopie) instanceof GuhEntity k) {
            verstop(k, false);
        }
        if (player != null && player.level() == lv && player.isAlive()) {
            Vec3 uit = einde == Einde.AANKOMST ? route.ziekenhuis : route.stal;
            if (uit != Vec3.ZERO) {
                player.teleportTo(lv, uit.x, uit.y, uit.z, java.util.Set.of(), player.getYRot(), 0, true);
            }
            player.fallDistance = 0;
            if (einde == Einde.WEG) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
            }
        }
        if (modus == Modus.TOCHT && player != null) {
            SleeTocht.meld(player, switch (einde) {
                case AANKOMST, FINISH -> SleeTocht.Moment.AANKOMST;
                case TE_LAAT -> SleeTocht.Moment.TE_LAAT;
                default -> SleeTocht.Moment.GESTOPT;
            });
        }
    }

    // --- Steele-Mika (sledesprint) --------------------------------------------------------------------------------------------------

    private void tegenstander(ServerLevel level, ServerPlayer player) {
        if (fase == SleeEntity.WACHT || steeleKlaar >= 0) {
            return;
        }
        SleeBaan b = route.baan(steeleBeen);
        double f = 1 + 0.06 * Math.sin(rijTijd / 37.0);
        if (route.zone(steeleBeen, RitRoute.Soort.IJSBRUG, steele.s) != null) {
            f *= 0.72;
        }
        if (steeleBeen == 0 && steele.s > b.lengte - 10) {
            f *= 0.75;                                              // (around the berghut)
        }
        double jij = totaal(been, stand.s), hij = totaal(steeleBeen, steele.s);
        if (hij - jij > 40) {
            f *= 0.88;                                              // (he looks back and laughs: njeh-heh)
        } else if (jij - hij > 25) {
            f *= 1.07;                                              // (he tries very hard)
        }
        double doelV = STEELE_SNELHEID[niveau.ordinal()] * f;
        steele.v += Mth.clamp(doelV - steele.v, -0.01, 0.01);
        steele.s = Math.min(b.lengte, steele.s + steele.v);
        double w = b.breedte(steele.s);
        double doelLat = 0.45 * w;
        for (RitRoute.Zone z : route.zones(steeleBeen, RitRoute.Soort.LAWINE)) {
            if (steele.s > z.s0() - 16 && steele.s < z.midden() + 2) {
                doelLat = -z.kant() * 0.65 * w;                     // (he knows the slopes: he dodges)
            }
        }
        if (route.zone(steeleBeen, RitRoute.Soort.IJSBRUG, steele.s) != null) {
            doelLat = 0;
        }
        steele.lat += Mth.clamp(doelLat - steele.lat, -0.08, 0.08);
        if (steele.s >= b.lengte - 0.05) {
            if (steeleBeen == 0) {
                steeleBeen = 1;
                steele.s = 0;
                steele.lat = 0.45 * route.baan(1).breedte(0);
            } else {
                steeleKlaar = rijTijd;
                steeleZegt(player, "gui.guhs.baltoslee.steele.eerst");
            }
        }
        // who is ahead: a little boast when he passes you
        int teken = hij > jij + 2 ? 1 : jij > hij + 2 ? -1 : 0;
        if (teken != 0 && teken != voorsprongTeken) {
            if (voorsprongTeken != 0 && rijTijd - steeleSpraak > 200) {
                steeleSpraak = rijTijd;
                steeleZegt(player, teken > 0 ? "gui.guhs.baltoslee.steele.inhalen" : "gui.guhs.baltoslee.steele.ingehaald");
            }
            voorsprongTeken = teken;
        }
        if (steeleSlee != null && level.getEntity(steeleSlee) instanceof SleeEntity st) {
            st.volg(steeleKlaar >= 0 ? SleeEntity.KLAAR : SleeEntity.RIJDT, SleeEntity.GEEN, steeleBeen, steele);
            st.zetStaat(storm, 100, false, -1, -1, 0);
        }
    }

    private double totaal(int b, double s) {
        return b == 0 ? s : route.baan(0).lengte + s;
    }

    private void steeleZegt(ServerPlayer player, String key) {
        Entity npc = steeleNpc == null ? null : player.level().getEntity(steeleNpc);
        Component naam = npc != null ? npc.getDisplayName() : Component.translatable("entity.guhs.guh_npc.steele_mika");
        player.sendSystemMessage(Component.literal("<").append(naam).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE)));
    }

    // --- the autopilot (tests) --------------------------------------------------------------------------------------------------

    private SleeRijden.Invoer autoInvoer() {
        if (autopiloot == 0) {
            return SleeRijden.Invoer.NIKS;
        }
        SleeBaan b = route.baan(been);
        double w = b.breedte(stand.s);
        double doel = 0;
        for (RitRoute.Zone z : route.zones(been, RitRoute.Soort.LAWINE)) {
            if (stand.s > z.s0() - 20 && stand.s < z.midden() + 1) {
                doel = (autopiloot == 1 ? -z.kant() : z.kant()) * 0.7 * w;
            }
        }
        double vooruit = 1;
        if (autoRust) {
            for (int i = 0; i < route.zones(been).size(); i++) {
                RitRoute.Zone z = route.zones(been).get(i);
                if (z.soort() == RitRoute.Soort.RUST && z.in(stand.s) && !rustGebruikt.contains(i)) {
                    vooruit = -1;
                }
            }
        }
        double stuur = Mth.clamp((doel - stand.lat) * 2, -1, 1);
        if (route.zone(been, RitRoute.Soort.IJSBRUG, stand.s) != null) {
            stuur = Mth.clamp(-stand.lat * 3 - stand.latV * 25, -1, 1);
            if (autopiloot == 2) {
                stuur = 1;
            }
        }
        return new SleeRijden.Invoer(vooruit, stuur);
    }

    // --- feedback, data ---------------------------------------------------------------------------------------------------------

    private void sync(ServerLevel level, SleeEntity sled) {
        int klok = modus == Modus.SPRINT ? (fase == SleeEntity.WACHT ? 0 : rijTijd) : been == 1 ? Math.max(0, limiet - tijdTerug) : -1;
        float tegen = modus == Modus.SPRINT ? (float) (steeleBeen + steele.s / Math.max(1, route.baan(steeleBeen).lengte)) : -1;
        int wacht = fase == SleeEntity.WACHT ? AFTEL - aftel : fase == SleeEntity.PAUZE ? (pauze == SleeEntity.RUST ? RUST_TICKS : PAUZE_MAX) - pauzeTicks
                : fase == SleeEntity.VAST ? vastTicks : 0;
        sled.volg(fase, pauze, been, stand);
        sled.zetStaat(storm, warmte, kist, klok, tegen, wacht);
    }

    private static void geluid(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    static void adv(ServerPlayer player, String name) {
        GuhAdvancements.grant(player, name);
        if (!name.startsWith("balto_slee_sprint_")) {
            GidsFeature.grant(player, "verhalen/" + name);
        }
    }

    static void titel(ServerPlayer player, Component title, @Nullable Component subtitle, int in, int stay, int out) {
        if (player.connection == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** m:ss.t */
    public static String tijdTekst(int ticks) {
        return nl.juiced.guhs.quest.Highscores.tijd(ticks);
    }

    /** The player's own sled data (saved, survives dying). */
    public static CompoundTag data(Player player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains("guhs_baltoslee")) {
            saved.put("guhs_baltoslee", new CompoundTag());
        }
        return saved.getCompoundOrEmpty("guhs_baltoslee");
    }

    public static int best(Player player, Niveau n) {
        return data(player).getIntOr("Best_" + n.id(), 0);
    }

    // --- events -------------------------------------------------------------------------------------------------------------------

    /** Logging out, dying, changing dimension: the ride is over. */
    public static void spelerWeg(ServerPlayer player) {
        SleeRit rit = RIJDERS.get(player.getUUID());
        if (rit != null) {
            ServerLevel home = player.level().getServer().getLevel(rit.dim);
            rit.einde(home != null ? home : player.level(), Einde.WEG);
        }
    }

    /** Every player tick: a ride whose sled stopped ticking (unloaded, gone) is over. */
    public static void controleer(ServerPlayer player) {
        SleeRit rit = RIJDERS.get(player.getUUID());
        if (rit == null) {
            return;
        }
        ServerLevel home = player.level().getServer().getLevel(rit.dim);
        ServerLevel level = home != null ? home : player.level();
        if (level.getGameTime() - rit.lastTick > 60) {
            rit.einde(level, Einde.WEG);
        }
    }

    public static void opSchade(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && rijdt(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    public static void opAfstappen(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() instanceof SleeEntity
                && !player.level().isClientSide()) {
            SleeRit rit = RIJDERS.get(player.getUUID());
            if (rit != null && !rit.stopt) {
                event.setCanceled(true);
            }
        }
    }

    public static void vergeetAlles() {
        RIJDERS.clear();
    }

    // --- for tests ----------------------------------------------------------------------------------------------------------------

    /** Skips the countdown. */
    public void slaAftellenOver(ServerLevel level) {
        while (fase == SleeEntity.WACHT && bezig()) {
            tick(level);
        }
    }

    /** Makes the sled stuck / buried time pass at once. */
    public void graafUit() {
        if (fase == SleeEntity.VAST) {
            vastTicks = 1;
        }
    }

    public int gen(ServerLevel level) {
        return level.getEntity(slee) instanceof SleeEntity s ? s.gen() : -1;
    }

    public boolean helder() {
        return helder;
    }

    /** (Tests) as if the rider's game never said anything: the server rides by itself. */
    public void vergeetClient() {
        clientGezien = false;
    }

    public void zetLimiet(int ticks) {
        this.limiet = ticks;
    }
    /** 26.1: ServerPlayer#playNotifySound is gone (same packet as 1.21.1's). */
    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        notifySound(p, net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch);
    }

    private static void notifySound(net.minecraft.server.level.ServerPlayer p, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, source, p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }
}
