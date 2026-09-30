package nl.juiced.guhs.feature.knuffelbad;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A ride down one of the Knuffelbad's slides (one rider per slide at a time).
 * <ol>
 *   <li>Start (right-click the slide's start gate): you sit in a zwembandje at the top, rubber ducks float down the
 *   slide (sometimes a special one, very rarely the golden one), a countdown: 3, 2, 1, VAHOEG!</li>
 *   <li>Down you go ({@link GlijPad}: the same path and speed on the server and in your game). Steer left and right to
 *   pick up the ducks: every duck is points, ducks in a row make a combo.</li>
 *   <li>PLONS into the pool or the foam: points for finishing (and for every duck), eendjesmunten, your record, the
 *   world's top 3 of this slide (Scorebord / Guhdex highscores), special ducks for the badeendjes collection.</li>
 * </ol>
 * Your own game tells the server where you are every tick (tau, lateral: {@code knuffelbad_stuur}); ducks are picked up
 * on that timeline, so what you see is what counts. Without word from your game (a game test) the server rides by
 * itself. While riding you can't get hurt, hungry or out of breath, and you can't get out (except: hold sneak).
 */
public final class GlijRit {
    public static final int AFTEL = 60, PLONS_TICKS = 40, WACHT_OP_EIND = 20, STIL = 10, UITSTAP_HOUD = 20;
    /** How close the ring's middle must come to a duck. */
    public static final double PAK = 1.15;
    public static final int PUNTEN_FINISH = 20, PUNTEN_ALLE = 50, COMBO_BONUS = 2, COMBO_MAX = 10;
    /** Prizes (2.8: every rule one more than its base): eendjesmunten for the score, the first ride, a record, all ducks. */
    public static final int EERSTE_MUNTEN = 3 + 1, RECORD_MUNTEN = 1 + 1, ALLE_MUNTEN = 2 + 1;

    public static int munten(int score) {
        return 1 + Math.max(0, score) / 40 + 1;
    }

    /** Points for a duck in a combo of n (the n-th in a row). */
    public static int punten(Eendsoort soort, int combo) {
        return soort.punten + Math.min(Math.max(0, combo - 1), COMBO_MAX) * COMBO_BONUS;
    }

    public enum Einde { KLAAR, WEG, GESTOPT, UITGESTAPT }

    public static final class Eend {
        public final UUID id;
        public final double s, lat;
        public final Eendsoort soort;
        public final Vec3 pos;
        public boolean gepakt, gemist;

        Eend(UUID id, double s, double lat, Eendsoort soort, Vec3 pos) {
            this.id = id;
            this.s = s;
            this.lat = lat;
            this.soort = soort;
            this.pos = pos;
        }
    }

    private static final Map<UUID, GlijRit> RIJDERS = new ConcurrentHashMap<>();       // by rider
    private static final Map<String, GlijRit> BANEN = new ConcurrentHashMap<>();       // by slide (dimension + start gate)
    private static final Set<UUID> RITTEN = ConcurrentHashMap.newKeySet();

    public final UUID id = UUID.randomUUID();
    public final UUID speler;
    public final Glijbaan glijbaan;
    public final GlijPad.Baan baan;
    private final ResourceKey<Level> dim;
    private final String sleutel;
    private UUID bandje;
    private final List<Eend> eenden = new ArrayList<>();
    private int fase = ZwembandjeEntity.WACHT;
    private int aftel, plons, wachtOpEind, uitstappen;
    private double tau, lat;
    /** The rider's own game: the last tau it told us, and when. */
    private double clientTau = -1;
    private long clientTick = -1000;
    private double laatsteS;
    private int score, gepakt, combo, besteCombo, laatstePunten;
    private final Set<Eendsoort> speciaal = EnumSet.noneOf(Eendsoort.class);
    private long lastTick;
    private boolean plonsGedaan, stopt;
    /** (Tests) steers the ride by itself towards the next duck. */
    public boolean autopiloot;

    private GlijRit(ServerPlayer player, Glijbaan glijbaan, BlockPos start, Direction facing) {
        this.speler = player.getUUID();
        this.glijbaan = glijbaan;
        this.baan = new GlijPad.Baan(glijbaan.pad(), start, facing);
        this.dim = player.level().dimension();
        this.sleutel = dim.identifier() + "@" + start.asLong();
        this.lastTick = player.level().getGameTime();
    }

    // --- who is where -------------------------------------------------------------------------------------------------------

    @Nullable
    public static GlijRit van(Player player) {
        return RIJDERS.get(player.getUUID());
    }

    public static boolean rijdt(Player player) {
        return RIJDERS.containsKey(player.getUUID());
    }

    public static boolean bestaat(UUID rit) {
        return RITTEN.contains(rit);
    }

    @Nullable
    public static GlijRit op(Level level, BlockPos start) {
        return BANEN.get(level.dimension().identifier() + "@" + start.asLong());
    }

    public int fase() {
        return fase;
    }

    public double tau() {
        return tau;
    }

    public int score() {
        return score;
    }

    public int gepakt() {
        return gepakt;
    }

    public List<Eend> eenden() {
        return eenden;
    }

    public Set<Eendsoort> speciaal() {
        return speciaal;
    }

    @Nullable
    public UUID bandje() {
        return bandje;
    }

    // --- starting --------------------------------------------------------------------------------------------------------

    /** The player right-clicked a slide's start gate. Returns true when the ride started. */
    public static boolean start(ServerPlayer player, BlockPos start, Glijbaan glijbaan, Direction facing) {
        ServerLevel level = player.level();
        if (rijdt(player)) {
            return false;
        }
        GlijRit bezet = op(level, start);
        if (bezet != null) {
            ServerPlayer other = level.getServer().getPlayerList().getPlayer(bezet.speler);
            player.sendOverlayMessage(Component.translatable("gui.guhs.knuffelbad.bezet", other == null ? "?" : other.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (Minigames.busyElsewhere(player, Minigames.KNUFFELBAD)) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.minigame.busy").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        GlijRit rit = new GlijRit(player, glijbaan, start, facing);
        ZwembandjeEntity ring = KnuffelbadFeature.ZWEMBANDJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ring == null) {
            return false;
        }
        ring.zetBaan(glijbaan, start, facing);
        ring.volg(ZwembandjeEntity.WACHT, 0, 0);
        level.addFreshEntity(ring);
        rit.bandje = ring.getUUID();
        RIJDERS.put(player.getUUID(), rit);
        KnuffelbadVoortgang.gevonden(player);
        BANEN.put(rit.sleutel, rit);
        RITTEN.add(rit.id);
        rit.zetEendjes(level, level.getRandom());

        player.stopRiding();
        player.closeContainer();
        Vec3 at = ring.position();
        player.teleportTo(level, at.x, at.y, at.z, java.util.Set.of(), ring.getYRot(), 10f, true);
        player.startRiding(ring, true, true);
        Minigames.startKeeping(player);
        level.playSound(null, ring.blockPosition(), KnuffelbadFeature.FLUIT.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
        player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.start." + glijbaan.id(), rit.eenden.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.besturing").withStyle(ChatFormatting.GRAY));
        rit.hud(player);
        return true;
    }

    /** The rubber ducks for this ride, on the slide's duck spots: mostly plain ones, sometimes a special one. */
    private void zetEendjes(ServerLevel level, RandomSource random) {
        List<Eendsoort> kan = Eendsoort.SPECIAAL.stream().filter(e -> e != Eendsoort.GOUDEN_EENDJE && e.op(glijbaan)).toList();
        int goud = random.nextFloat() < Eendsoort.KANS_GOUD ? random.nextInt(Math.max(1, baan.pad().eendjes.size())) : -1;
        int i = 0;
        for (float[] spot : baan.pad().eendjes) {
            Eendsoort soort = i == goud ? Eendsoort.GOUDEN_EENDJE
                    : random.nextFloat() < Eendsoort.KANS_SPECIAAL && !kan.isEmpty() ? kan.get(random.nextInt(kan.size())) : Eendsoort.NORMAAL;
            maakEend(level, spot[0], spot[1], soort);
            i++;
        }
    }

    /** One duck on the slide at s, lateral lat. */
    Eend maakEend(ServerLevel level, double s, double lat, Eendsoort soort) {
        GlijPad.Stand st = baan.stand(s, lat);
        Vec3 pos = st.midden(baan.pad().ring);
        BadeendjeEntity duck = KnuffelbadFeature.BADEENDJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (duck == null) {
            return null;
        }
        Vec3 t = st.tangent();
        duck.snapTo(pos.x, pos.y - 0.12, pos.z, (float) Math.toDegrees(Math.atan2(-t.x, t.z)) + 180f, 0);
        duck.setSoort(soort);
        duck.setRit(id);
        level.addFreshEntity(duck);
        Eend e = new Eend(duck.getUUID(), s, lat, soort, pos);
        eenden.add(e);
        return e;
    }

    // --- every tick ------------------------------------------------------------------------------------------------------------

    /** The ring's server tick: drives its ride. False: the ring has no ride (it should go). */
    static boolean tickVan(ZwembandjeEntity ring) {
        for (GlijRit rit : RIJDERS.values()) {
            if (ring.getUUID().equals(rit.bandje)) {
                rit.tick((ServerLevel) ring.level(), ring);
                return true;
            }
        }
        return false;
    }

    /** One ride tick (tests call it directly to ride faster). */
    public void tick(ServerLevel level) {
        if (level.getEntity(bandje) instanceof ZwembandjeEntity ring) {
            tick(level, ring);
        } else {
            einde(level, Einde.WEG);
        }
    }

    void tick(ServerLevel level, ZwembandjeEntity ring) {
        lastTick = level.getGameTime();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(speler);
        if (player == null || !player.isAlive() || player.level() != level || player.distanceToSqr(ring) > 48 * 48) {
            einde(level, Einde.WEG);
            return;
        }
        Minigames.keep(player);
        if (player.getVehicle() != ring && !stopt) {
            player.startRiding(ring, true, true);                  // (you can't get out on the way: hold sneak to stop)
        }
        GlijPad pad = baan.pad();
        switch (fase) {
            case ZwembandjeEntity.WACHT -> {
                if (aftel % 20 == 0 && aftel < AFTEL) {
                    int n = 3 - aftel / 20;
                    titel(player, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.knuffelbad.klaar"), 0, 18, 2);
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.NOTE_BLOCK_PLING.value()), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, player.getRandom().nextLong()));
                }
                if (++aftel >= AFTEL) {
                    fase = ZwembandjeEntity.GLIJDT;
                    tau = 0;
                    titel(player, Component.translatable("gui.guhs.knuffelbad.vahoeg").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), null, 0, 20, 10);
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.NOTE_BLOCK_PLING.value()), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 2f, player.getRandom().nextLong()));
                    level.playSound(null, ring.blockPosition(), KnuffelbadFeature.GLIJDEN.get(), SoundSource.PLAYERS, 1f, 1f);
                    hud(player);
                }
            }
            case ZwembandjeEntity.GLIJDT -> {
                if (tau < pad.duur()) {
                    tau += 1;
                }
                if (autopiloot) {
                    lat = stuurNaarEend(pad.sAt(tau));
                }
                // no word from the rider's game (a game test, or a hiccup): the server rides the timeline itself
                if (level.getGameTime() - clientTick > STIL) {
                    volgTijdlijn(level, player, Math.max(clientTau, tau - 1), tau, lat, lat);
                    clientTau = tau;
                }
                effecten(level, player, ring, pad.sAt(tau));
                if (tau >= pad.duur() && (clientTau >= pad.duur() - 1 || ++wachtOpEind > WACHT_OP_EIND)) {
                    fase = ZwembandjeEntity.PLONS;
                    plons = 0;
                }
            }
            default -> {
                if (++plons >= PLONS_TICKS) {
                    klaar(level, player);
                    return;
                }
            }
        }
        if (player.isShiftKeyDown() && fase != ZwembandjeEntity.PLONS) {
            if (++uitstappen >= UITSTAP_HOUD) {
                einde(level, Einde.UITGESTAPT);
                return;
            }
        } else {
            uitstappen = 0;
        }
        ring.volg(fase, fase == ZwembandjeEntity.WACHT ? 0 : tau, lat);
        if (player.getVehicle() == ring) {
            ring.positionRider(player);                       // (also when the ride is ticked faster than the world: game tests)
        }
    }

    /** The rider's game: at tau it was at lateral lat (every tick, knuffelbad_stuur). */
    public void meld(ServerPlayer player, double tauC, double latC) {
        if (fase != ZwembandjeEntity.GLIJDT) {
            return;
        }
        ServerLevel level = player.level();
        clientTick = level.getGameTime();
        double latFrom = lat;
        lat = Mth.clamp(latC, lat - 0.35, lat + 0.35);        // (no jumping across the slide)
        lat = Mth.clamp(lat, -1, 1);
        tauC = Math.min(tauC, tau + 3);                       // (it can't be ahead of the server)
        if (tauC <= clientTau) {
            return;                                           // (that bit of the slide was done already)
        }
        volgTijdlijn(level, player, Math.max(clientTau, 0), tauC, latFrom, lat);
        clientTau = tauC;
    }

    /** The ring went from tau a to tau b (lateral la to lb): the ducks it passed close enough to are picked up. */
    private void volgTijdlijn(ServerLevel level, ServerPlayer player, double a, double b, double la, double lb) {
        GlijPad pad = baan.pad();
        double s0 = pad.sAt(a), s1 = pad.sAt(b);
        if (s1 <= s0) {
            return;
        }
        int steps = Math.max(1, (int) Math.ceil((s1 - s0) / 0.3));
        for (Eend e : eenden) {
            if (e.gepakt || e.gemist || e.s < s0 - 2 || e.s > s1 + 2) {
                continue;
            }
            for (int k = 0; k <= steps; k++) {
                double f = (double) k / steps;
                double s = Mth.lerp(f, s0, s1);
                Vec3 m = baan.stand(s, Mth.lerp(f, la, lb)).midden(pad.ring);
                if (m.distanceToSqr(e.pos) < PAK * PAK) {
                    pak(level, player, e);
                    break;
                }
            }
        }
        // ducks left behind (2 blocks past): the combo is over
        for (Eend e : eenden) {
            if (!e.gepakt && !e.gemist && e.s < s1 - 2) {
                e.gemist = true;
                if (combo > 0) {
                    combo = 0;
                    hud(player);
                }
            }
        }
        laatsteS = s1;
    }

    private void pak(ServerLevel level, ServerPlayer player, Eend e) {
        e.gepakt = true;
        combo++;
        besteCombo = Math.max(besteCombo, combo);
        laatstePunten = punten(e.soort, combo);
        score += laatstePunten;
        gepakt++;
        if (e.soort.speciaal()) {
            speciaal.add(e.soort);
            player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.speciaal", e.soort.naam()).withStyle(ChatFormatting.GOLD));
        }
        Entity duck = level.getEntity(e.id);
        Vec3 at = duck != null ? duck.position() : e.pos;
        if (duck != null) {
            duck.discard();
        }
        level.playSound(player, at.x, at.y, at.z, KnuffelbadFeature.EENDJE_PIEP.get(), SoundSource.PLAYERS, 1f, 0.9f + Math.min(combo, 10) * 0.05f);
        level.sendParticles(e.soort.speciaal() ? KnuffelbadFeature.GLINSTERING.get() : KnuffelbadFeature.ZEEPBELLETJE.get(), at.x, at.y + 0.2, at.z,
                e.soort.speciaal() ? 14 : 7, 0.25, 0.2, 0.25, 0.02);
        hud(player);
    }

    /** (Autopilot for tests) the lateral of the next duck ahead. */
    private double stuurNaarEend(double s) {
        Eend next = null;
        for (Eend e : eenden) {
            if (!e.gepakt && !e.gemist && e.s >= s - 0.5 && (next == null || e.s < next.s)) {
                next = e;
            }
        }
        return next == null ? lat : Mth.approach((float) lat, (float) next.lat, 0.3f);
    }

    /** The big splash and the sounds of the ride for the people watching (the rider's own game does its own). */
    private void effecten(ServerLevel level, ServerPlayer player, ZwembandjeEntity ring, double s) {
        Double plonsS = baan.pad().merk.get("plons");
        if (!plonsGedaan && plonsS != null && s >= plonsS) {
            plonsGedaan = true;
            Vec3 p = ring.position();
            level.playSound(player, p.x, p.y, p.z, KnuffelbadFeature.PLONS_GELUID.get(), SoundSource.PLAYERS, 2f, glijbaan == Glijbaan.GROTE_PLONS ? 0.8f : 1f);
            level.sendParticles(KnuffelbadFeature.PLONS.get(), p.x, p.y + 0.3, p.z, glijbaan == Glijbaan.GROTE_PLONS ? 120 : 60, 1.2, 0.4, 1.2, 0.35);
            level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.5, p.z, 80, 1.6, 0.5, 1.6, 0.2);
            if (glijbaan == Glijbaan.ROZE_TRECHTER) {
                level.sendParticles(KnuffelbadFeature.SCHUIMVLOKJE.get(), p.x, p.y + 0.5, p.z, 60, 1.5, 0.6, 1.5, 0.08);
                level.playSound(player, p.x, p.y, p.z, KnuffelbadFeature.SCHUIM.get(), SoundSource.PLAYERS, 1.2f, 1f);
            }
        }
    }

    // --- the end ------------------------------------------------------------------------------------------------------------

    private void klaar(ServerLevel level, ServerPlayer player) {
        int eendjes = eenden.size();
        score += PUNTEN_FINISH;
        boolean alle = eendjes > 0 && gepakt == eendjes;
        if (alle) {
            score += PUNTEN_ALLE;
        }
        CompoundTag data = data(player);
        String sid = glijbaan.id();
        int oudRecord = data.getIntOr("Best_" + sid, 0);
        int ritten = data.getIntOr("Ritten_" + sid, 0) + 1;
        boolean record = score > oudRecord;
        data.putInt("Ritten_" + sid, ritten);
        if (record) {
            data.putInt("Best_" + sid, score);
        }
        data.putInt("Eendjes", data.getIntOr("Eendjes", 0) + gepakt);
        int munten = munten(score) + (ritten == 1 ? EERSTE_MUNTEN : 0) + (record && oudRecord > 0 ? RECORD_MUNTEN : 0) + (alle ? ALLE_MUNTEN : 0);
        Minigames.give(player, new ItemStack(KnuffelbadFeature.EENDJESMUNT.get(), munten));

        titel(player, Component.translatable("gui.guhs.knuffelbad.plons").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.knuffelbad.eindscore", score, gepakt, eendjes).withStyle(ChatFormatting.WHITE), 0, 50, 15);
        player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.uitslag", glijbaan.naam(), score, gepakt, eendjes, besteCombo, munten)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (alle) {
            player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.alle_eendjes").withStyle(ChatFormatting.GOLD));
        }
        if (record) {
            player.sendSystemMessage(Component.translatable(oudRecord > 0 ? "gui.guhs.knuffelbad.record" : "gui.guhs.knuffelbad.eerste_record", score, oudRecord)
                    .withStyle(ChatFormatting.YELLOW));
        }
        Scorebord.submit(player, glijbaan.board(), score, false);
        // the Knus tab: rides per slide, ducks, the best score, the special ducks
        KnusVoortgang.tel(player, KnuffelbadVoortgang.teller(glijbaan), 1);
        KnusVoortgang.tel(player, KnuffelbadVoortgang.EENDJES, gepakt);
        KnusVoortgang.hoogste(player, KnuffelbadVoortgang.BESTE, score);
        for (Eendsoort e : speciaal) {
            KnusVoortgang.ontdek(player, KnuffelbadVoortgang.BADEENDJES, e.id());
        }
        GuhAdvancements.grant(player, "knuffelbad_" + sid);
        KnuffelbadVoortgang.toon(player, "knuffelbad_glijbaan");
        boolean alleDrie = true;
        for (Glijbaan g : Glijbaan.values()) {
            alleDrie &= data.getIntOr("Ritten_" + g.id(), 0) > 0;
        }
        if (alleDrie) {
            GuhAdvancements.grant(player, "knuffelbad_alle_glijbanen");
            KnuffelbadVoortgang.toon(player, "knuffelbad_alle_glijbanen");
        }
        if (KnusVoortgang.ontdekt(player, KnuffelbadVoortgang.BADEENDJES).size() >= Eendsoort.SPECIAAL.size()) {
            GuhAdvancements.grant(player, "knuffelbad_badeendjes_vol");
            KnuffelbadVoortgang.toon(player, "knuffelbad_badeendjes");
        }
        Badmeester.toonScores(level, baan.start());
        Badmeester.toonPoortBord(level, baan.start(), glijbaan);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.7f, 1.3f, player.getRandom().nextLong()));
        opruimen(level, true);
    }

    /** Ends a ride early (the rider left, logged out, held sneak...). */
    public void einde(ServerLevel level, Einde einde) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(speler);
        if (player != null && einde == Einde.UITGESTAPT) {
            player.sendSystemMessage(Component.translatable("gui.guhs.knuffelbad.uitgestapt").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        opruimen(level, einde != Einde.WEG);
    }

    /** The ring and the ducks go (bubbles), the rider gets off at the edge of the pool (never up in the air). */
    private void opruimen(ServerLevel level, boolean terug) {
        RIJDERS.remove(speler);
        BANEN.remove(sleutel);
        RITTEN.remove(id);
        stopt = true;
        ServerLevel home = level.getServer().getLevel(dim);
        ServerLevel lv = home != null ? home : level;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(speler);
        if (player != null && player.getVehicle() instanceof ZwembandjeEntity) {
            player.stopRiding();
        }
        Entity ring = lv.getEntity(bandje);
        if (ring != null) {
            lv.sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), ring.getX(), ring.getY() + 0.3, ring.getZ(), 16, 0.4, 0.2, 0.4, 0.02);
            ring.discard();
        }
        for (Eend e : eenden) {
            Entity duck = lv.getEntity(e.id);
            if (duck != null) {
                duck.discard();
            }
        }
        if (player != null) {
            hudUit(player);
            if (player.level() == lv && player.isAlive()) {
                Vec3 uit = baan.uitstap();
                player.teleportTo(lv, uit.x, uit.y, uit.z, java.util.Set.of(), baan.uitstapYaw(), 0, true);
                player.fallDistance = 0;
                if (!terug) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
                }
            }
        }
    }

    // --- events -------------------------------------------------------------------------------------------------------------

    /** Logging out, dying, changing dimension: the ride is over (before the player is saved: never saved up on the slide). */
    public static void spelerWeg(ServerPlayer player) {
        GlijRit rit = RIJDERS.get(player.getUUID());
        if (rit != null) {
            ServerLevel home = player.level().getServer().getLevel(rit.dim);
            rit.opruimen(home != null ? home : player.level(), true);
        }
    }

    /** Every player tick: a ride whose ring stopped ticking (unloaded, gone) is over. */
    public static void controleer(ServerPlayer player) {
        GlijRit rit = RIJDERS.get(player.getUUID());
        if (rit == null) {
            return;
        }
        ServerLevel home = player.level().getServer().getLevel(rit.dim);
        ServerLevel level = home != null ? home : player.level();
        if (level.getGameTime() - rit.lastTick > 60) {
            rit.einde(level, Einde.WEG);
        }
    }

    /** Riders can't get hurt (bumps, falls, the ride itself). */
    public static void opSchade(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && rijdt(player)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** You can't get out of the ring on the way (only by holding sneak, which stops the ride). */
    public static void opAfstappen(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() instanceof ZwembandjeEntity) {
            GlijRit rit = RIJDERS.get(player.getUUID());
            if (rit != null && !rit.stopt && !player.level().isClientSide()) {
                event.setCanceled(true);
            }
        }
    }

    public static void vergeetAlles() {
        RIJDERS.clear();
        BANEN.clear();
        RITTEN.clear();
    }

    // --- data and feedback -----------------------------------------------------------------------------------------------------

    /** The player's own knuffelbad data (saved, survives dying): rides and best score per slide, all ducks. */
    public static CompoundTag data(Player player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains("guhs_knuffelbad")) {
            saved.put("guhs_knuffelbad", new CompoundTag());
        }
        return saved.getCompoundOrEmpty("guhs_knuffelbad");
    }

    public static int best(Player player, Glijbaan g) {
        return data(player).getIntOr("Best_" + g.id(), 0);
    }

    public static int ritten(Player player, Glijbaan g) {
        return data(player).getIntOr("Ritten_" + g.id(), 0);
    }

    static void titel(ServerPlayer player, Component title, @Nullable Component subtitle, int in, int stay, int out) {
        if (player.connection == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** The panel on the rider's screen. */
    void hud(ServerPlayer player) {
        CompoundTag d = new CompoundTag();
        d.putBoolean("Actief", true);
        d.putInt("Baan", glijbaan.ordinal());
        d.putInt("Score", score);
        d.putInt("Gepakt", gepakt);
        d.putInt("Totaal", eenden.size());
        d.putInt("Combo", combo);
        d.putInt("Punten", laatstePunten);
        d.putInt("Best", best(player, glijbaan));
        List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), glijbaan.board());
        d.putInt("Record", top.isEmpty() ? 0 : top.get(0).score());
        KnuffelbadPayloads.naar(player, new KnuffelbadPayloads.Hud(d));
    }

    static void hudUit(ServerPlayer player) {
        CompoundTag d = new CompoundTag();
        d.putBoolean("Actief", false);
        KnuffelbadPayloads.naar(player, new KnuffelbadPayloads.Hud(d));
    }

    // --- for tests ----------------------------------------------------------------------------------------------------------

    /** Skips the countdown. */
    public void slaAftellenOver(ServerLevel level) {
        while (fase == ZwembandjeEntity.WACHT && RIJDERS.containsValue(this)) {
            tick(level);
        }
    }

    public boolean bezig() {
        return RIJDERS.containsValue(this);
    }
}
