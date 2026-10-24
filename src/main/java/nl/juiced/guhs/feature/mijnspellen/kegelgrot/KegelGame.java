package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.mijnspellen.MijnspelBlokken;
import nl.juiced.guhs.feature.mijnspellen.Mijnspel;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A game of guh bowling with De Guhde: one player at a time per Kegelgrot (the others watch from the benches). You pick a
 * lane (recht, bochtig, hindernisjes = makkelijk, medium, lastig) and get the pink wool ball. Ten frames, scored exactly
 * like real bowling ({@link Bowling}); after every ball the fallen pins are swept away, a fresh rack of ten comes at the
 * start of every frame (and after a strike or spare in the 10th). The pins and the ball roll by {@link KegelBaan}.
 * <p>
 * Kegelknopen: the score / 4 x 0.7 / 1.0 / 1.45 (makkelijk / medium / lastig), at most {@link #MAX_MUNTEN}, +5 for beating
 * your own record on that lane ({@link #munten}). The world's top 3 per lane float above De Guhde. While you bowl you
 * can't get hurt or hungry. Games live in memory (a restart ends them; pins and ball then disappear by themselves). The
 * lanes are found once by their foul lines (kegelgrot_baanbegin) around De Guhde.
 */
public final class KegelGame {
    public static final int OPTIE_MAKKELIJK = 1, OPTIE_MEDIUM = 2, OPTIE_LASTIG = 3, OPTIE_WINKEL = 4, OPTIE_UITLEG = 5, OPTIE_STOP = 6;
    public static final int VEGEN = 36, NA_TIJD = 60, IDLE_LIMIT = 20 * 90, MAX_MUNTEN = 90;
    static final double[] MUNTEN = {0.7, 1.0, 1.45};
    /** The approach: where you may stand to bowl (lane frame). */
    public static final double AANLOOP_VAN = -7, AANLOOP_TOT = 0.4, AANLOOP_BREED = 2.6;
    public static final int SCAN = 32, WEG = 40, SCORES_TICKS = 100;
    public static final String BORD = "kegelen";
    static final String BEST = "guhs_kegelen_best_", POTJES = "guhs_kegelen_potjes";

    public enum Phase { IDLE, AANLOOP, ROLLEN, VEGEN, NA }

    private static final Map<UUID, KegelGame> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> SPELERS = new ConcurrentHashMap<>();
    private static final int STALE_TICKS = 60;

    private final UUID npcId;
    @Nullable
    private UUID player;
    private String playerName = "";
    private Phase phase = Phase.IDLE;
    private Niveau niveau = Niveau.MEDIUM;
    private int timer;
    private long lastTick, laatsteWorp;
    private KegelBaan baan = new KegelBaan(KegelBaan.Soort.RECHT);
    private Bowling blad = new Bowling();
    private final UUID[] kegels = new UUID[10];
    @Nullable
    private UUID bal, schuif;
    private boolean wasGoot;

    private boolean scanned;
    private final BlockPos[] begin = new BlockPos[3];
    private final Direction[] richting = {Direction.SOUTH, Direction.SOUTH, Direction.SOUTH};

    private KegelGame(UUID npcId) {
        this.npcId = npcId;
    }

    public static KegelGame of(GuhNpcEntity npc) {
        KegelGame game = GAMES.computeIfAbsent(npc.getUUID(), KegelGame::new);
        if (!game.scanned) {
            game.load(npc.roleData);
        }
        return game;
    }

    public static boolean isPlaying(Player p) {
        return SPELERS.containsKey(p.getUUID());
    }

    public boolean isRunning() {
        return player != null;
    }

    public boolean isPlayedBy(Player p) {
        return p.getUUID().equals(player);
    }

    boolean owns(KegelEntity e) {
        if (!isRunning()) {
            return false;
        }
        UUID id = e.getUUID();
        if (id.equals(bal) || id.equals(schuif)) {
            return true;
        }
        for (UUID k : kegels) {
            if (id.equals(k)) {
                return true;
            }
        }
        return false;
    }

    public Phase phase() {
        return phase;
    }

    public Bowling blad() {
        return blad;
    }

    public KegelBaan baan() {
        return baan;
    }

    public Niveau niveau() {
        return niveau;
    }

    // --- the lane's frame ----------------------------------------------------------------------------------------------------

    public Vec3 punt(Niveau n, double u, double v, double dy) {
        Direction f = richting[n.ordinal()], r = f.getClockWise();
        BlockPos b = begin[n.ordinal()];
        return new Vec3(b.getX() + 0.5 + f.getStepX() * u + r.getStepX() * v, b.getY() + 1 + dy,
                b.getZ() + 0.5 + f.getStepZ() * u + r.getStepZ() * v);
    }

    /** {u, v} of a world point in this lane's frame. */
    public double[] lokaal(Niveau n, Vec3 pos) {
        Direction f = richting[n.ordinal()], r = f.getClockWise();
        BlockPos b = begin[n.ordinal()];
        double dx = pos.x - (b.getX() + 0.5), dz = pos.z - (b.getZ() + 0.5);
        return new double[] {dx * f.getStepX() + dz * f.getStepZ(), dx * r.getStepX() + dz * r.getStepZ()};
    }

    /** The world yaw of a direction (du, dv) in the lane's frame. */
    private float yaw(double du, double dv) {
        Direction f = richting[niveau.ordinal()], r = f.getClockWise();
        double x = f.getStepX() * du + r.getStepX() * dv, z = f.getStepZ() * du + r.getStepZ() * dv;
        return (float) (Math.atan2(-x, z) * 180 / Math.PI);
    }

    @Nullable
    public BlockPos begin(Niveau n) {
        return begin[n.ordinal()];
    }

    // --- talking to De Guhde -------------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer p) {
        KegelGame game = of(npc);
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.85f);
        KegelgrotFeature.checkKleding(p);
        if (game.isPlayedBy(p)) {
            Praat.open(p, npc, null, "quest.guhs.kegelen.bezig_jij", new Object[] {game.blad.score()},
                    new Praat.Optie(OPTIE_STOP, "gui.guhs.kegelen.optie.stop"));
            return;
        }
        if (game.isRunning()) {
            Praat.open(p, npc, null, "quest.guhs.kegelen.bezig", new Object[] {game.playerName},
                    new Praat.Optie(OPTIE_WINKEL, "gui.guhs.kegelen.optie.winkel"), new Praat.Optie(OPTIE_UITLEG, "gui.guhs.kegelen.optie.uitleg"));
            return;
        }
        Praat.open(p, npc, null, "quest.guhs.kegelen.hallo" + (1 + npc.getRandom().nextInt(4)),
                new Object[] {bestTekst(p, Niveau.MAKKELIJK), bestTekst(p, Niveau.MEDIUM), bestTekst(p, Niveau.LASTIG)},
                new Praat.Optie(OPTIE_MAKKELIJK, "gui.guhs.kegelen.optie.makkelijk"), new Praat.Optie(OPTIE_MEDIUM, "gui.guhs.kegelen.optie.medium"),
                new Praat.Optie(OPTIE_LASTIG, "gui.guhs.kegelen.optie.lastig"), new Praat.Optie(OPTIE_WINKEL, "gui.guhs.kegelen.optie.winkel"),
                new Praat.Optie(OPTIE_UITLEG, "gui.guhs.kegelen.optie.uitleg"));
    }

    public static void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        if (p.distanceToSqr(npc) > 100) {
            return;
        }
        KegelGame game = of(npc);
        switch (optie) {
            case OPTIE_MAKKELIJK, OPTIE_MEDIUM, OPTIE_LASTIG -> {
                Praat.sluit(p);
                game.start(npc, p, Niveau.of(optie - OPTIE_MAKKELIJK));
            }
            case OPTIE_WINKEL -> {
                Praat.sluit(p);
                npc.openShop(p);
            }
            case OPTIE_UITLEG -> Praat.open(p, npc, null, "quest.guhs.kegelen.uitleg", new Object[0],
                    new Praat.Optie(OPTIE_MAKKELIJK, "gui.guhs.kegelen.optie.makkelijk"), new Praat.Optie(OPTIE_MEDIUM, "gui.guhs.kegelen.optie.medium"),
                    new Praat.Optie(OPTIE_LASTIG, "gui.guhs.kegelen.optie.lastig"));
            case OPTIE_STOP -> {
                Praat.sluit(p);
                if (game.isPlayedBy(p)) {
                    game.einde(npc, p, "quest.guhs.kegelen.gestopt");
                }
            }
            default -> {
            }
        }
    }

    // --- a game ------------------------------------------------------------------------------------------------------------

    public boolean start(GuhNpcEntity npc, ServerPlayer p, Niveau n) {
        ServerLevel world = (ServerLevel) npc.level();
        if (isPlayedBy(p)) {
            return false;
        }
        if (isRunning()) {
            GuhQuests.say(p, npc, "quest.guhs.kegelen.bezig", playerName);
            return false;
        }
        if (Minigames.refuse(p, npc, KegelgrotFeature.ID)) {
            return false;
        }
        if (!scan(npc, true) || begin[n.ordinal()] == null) {
            GuhQuests.say(p, npc, "quest.guhs.kegelen.kapot");
            return false;
        }
        if (p.getInventory().getFreeSlot() < 0 && !p.getMainHandItem().isEmpty()) {
            GuhQuests.say(p, npc, "quest.guhs.kegelen.vol");
            return false;
        }
        niveau = n;
        player = p.getUUID();
        playerName = p.getGameProfile().name();
        SPELERS.put(player, npcId);
        blad = new Bowling();
        baan = new KegelBaan(KegelBaan.Soort.values()[n.ordinal()]);
        opzetten(world, true);
        lastTick = laatsteWorp = world.getGameTime();
        geefBal(p);
        Minigames.startKeeping(p);
        p.clearFire();
        Vec3 plek = punt(n, -2.5, 0, 0);
        p.teleportTo(world, plek.x, plek.y, plek.z, java.util.Set.of(), richting[n.ordinal()].toYRot(), 8f, true);
        phase = Phase.AANLOOP;
        GuhQuests.say(p, npc, "quest.guhs.kegelen.start_" + n.id());
        p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.hoe").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Let go of the ball: a throw (if it's your turn and you stand on the approach). */
    public static void gooi(ServerPlayer p, float kracht) {
        UUID npc = SPELERS.get(p.getUUID());
        KegelGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.doeGooi(p, kracht, p.isShiftKeyDown());
        }
    }

    /** A throw by this player: from where they stand, the way they look; recht = no hook. */
    public boolean doeGooi(ServerPlayer p, float kracht, boolean recht) {
        if (phase != Phase.AANLOOP) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.kegelen.wacht").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        double[] at = lokaal(niveau, p.position());
        if (at[0] < AANLOOP_VAN || at[0] > AANLOOP_TOT || Math.abs(at[1]) > AANLOOP_BREED) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.kegelen.sta").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Vec3 look = p.getLookAngle();
        Direction f = richting[niveau.ordinal()], r = f.getClockWise();
        double langs = look.x * f.getStepX() + look.z * f.getStepZ(), dwars = look.x * r.getStepX() + look.z * r.getStepZ();
        if (langs < 0.2) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.kegelen.kijk").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        double spin = recht ? 0 : -Math.max(-1, Math.min(1, at[1] / 1.2));
        return gooiMet(p, kracht, at[1], Math.atan2(dwars, langs), spin);
    }

    /** A throw with everything given (also the tests). */
    public boolean gooiMet(ServerPlayer p, float kracht, double v0, double hoek, double spin) {
        ServerLevel world = p.level();
        if (phase != Phase.AANLOOP) {
            return false;
        }
        baan.gooi(v0, KegelBaan.snelheid(kracht), hoek, spin, world.getRandom().nextLong(), world.getGameTime());
        KegelBaan.Bal b = baan.bal();
        Vec3 pos = punt(niveau, b.u, b.v, 0);
        KegelEntity e = KegelEntity.create(world, KegelgrotFeature.BAL.get(), this, pos, yaw(b.du, b.dv));
        bal = e.getUUID();
        wasGoot = false;
        phase = Phase.ROLLEN;
        laatsteWorp = world.getGameTime();
        world.playSound(null, BlockPos.containing(pos), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
        world.playSound(null, BlockPos.containing(pos), SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.15f + kracht * 0.2f, 1.6f);
        return true;
    }

    public void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        if (npc.tickCount % SCORES_TICKS == 1) {
            if (!scanned && world.getNearestPlayer(npc, 32) != null) {
                scan(npc, false);
            }
            showScores(npc);
        }
        if (player == null) {
            return;
        }
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || Mijnspel.afstand(p, npc) > WEG) {
            if (p != null) {
                removeBal(p);
            }
            einde(npc, p != null && p.isAlive() && p.level() == world ? p : null, "quest.guhs.kegelen.weggelopen");
            return;
        }
        if (npc.tickCount % 20 == 0) {
            Minigames.keep(p);
            p.clearFire();
            if (phase != Phase.NA && !p.getInventory().hasAnyMatching(s -> s.is(KegelgrotFeature.WOLBAL.get())) && p.getInventory().getFreeSlot() >= 0) {
                geefBal(p);                                       // (lost it somehow? here it is again)
            }
        }
        if (baan.soort == KegelBaan.Soort.HINDERNIS) {
            schuif(world);
        }
        switch (phase) {
            case AANLOOP -> {
                if (world.getGameTime() - laatsteWorp > IDLE_LIMIT) {
                    removeBal(p);
                    einde(npc, p, "quest.guhs.kegelen.te_lang");
                    return;
                }
            }
            case ROLLEN -> {
                boolean rolt = baan.tik();
                int g = baan.gebeurd();
                zetEntities(world);
                if ((g & 1) != 0) {
                    Vec3 at = punt(niveau, KegelBaan.KOP, 0, 0.5);
                    world.playSound(null, BlockPos.containing(at), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.7f, 1.7f + world.getRandom().nextFloat() * 0.3f);
                    world.playSound(null, BlockPos.containing(at), SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.9f, 1.2f);
                }
                if ((g & 2) != 0) {
                    KegelBaan.Bal b = baan.bal();
                    Vec3 at = punt(niveau, b.u, b.v, 0.2);
                    world.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_BLOCK_HIT, SoundSource.BLOCKS, 0.6f, 1.3f);
                }
                if ((g & 4) != 0) {
                    wasGoot = true;
                    p.sendOverlayMessage(Component.translatable("gui.guhs.kegelen.goot").withStyle(ChatFormatting.GRAY));
                }
                if (!rolt) {
                    worpKlaar(npc, p);
                }
            }
            case VEGEN -> {
                if (--timer <= 0) {
                    if (blad.klaar()) {
                        afgelopen(npc, p);
                    } else {
                        opzetten(world, blad.nieuweKegels());
                        phase = Phase.AANLOOP;
                        laatsteWorp = world.getGameTime();
                    }
                }
            }
            case NA -> {
                if (--timer <= 0) {
                    einde(npc, p, null);
                    return;
                }
            }
            default -> {
            }
        }
        if (isRunning() && npc.tickCount % 10 == 0 && phase == Phase.AANLOOP) {
            int[] pos = blad.positie();
            p.sendOverlayMessage(Component.translatable("gui.guhs.kegelen.balk", Math.min(10, pos[0] + 1), pos[1] + 1, blad.score(),
                    blad.staand()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** All rolled out: count the pins, tell the player, sweep. */
    private void worpKlaar(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        int staandVoor = blad.staand();
        int[] pos = blad.positie();
        int n = baan.omgevallen();
        blad.gooi(n);
        weg(world, bal);
        bal = null;
        Component titel, sub;
        if (n == staandVoor && staandVoor == Bowling.KEGELS) {
            titel = Component.translatable("gui.guhs.kegelen.strike").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            sub = Component.translatable("gui.guhs.kegelen.strike_sub");
            Mijnspel.geluid(p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f, 1f);
            Mijnspel.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.4f);
            Vec3 at = punt(niveau, KegelBaan.KOP + 0.6, 0, 1);
            world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y, at.z, 40, 1, 0.6, 1, 0.2);
            GuhAdvancements.grant(p, "kegelen_strike");
            GuhQuests.say(p, npc, "quest.guhs.kegelen.strike" + (1 + world.getRandom().nextInt(3)));
        } else if (n == staandVoor && n > 0) {
            titel = Component.translatable("gui.guhs.kegelen.spare").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            sub = Component.translatable("gui.guhs.kegelen.spare_sub");
            Mijnspel.geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 1f, 1.5f);
            Vec3 at = punt(niveau, KegelBaan.KOP + 0.6, 0, 1);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 25, 1, 0.5, 1, 0.1);
        } else if (n == 0) {
            titel = Component.translatable(wasGoot ? "gui.guhs.kegelen.goot_titel" : "gui.guhs.kegelen.mis").withStyle(ChatFormatting.GRAY);
            sub = Component.translatable("gui.guhs.kegelen.mis_sub");
            Mijnspel.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 0.8f, 0.7f);
        } else {
            titel = Component.translatable("gui.guhs.kegelen.kegels", n).withStyle(ChatFormatting.LIGHT_PURPLE);
            sub = Component.translatable("gui.guhs.kegelen.kegels_sub", staandVoor - n);
        }
        Mijnspel.titel(p, titel, sub, 2, 28, 8);
        int[] na = blad.positie();
        if (na[0] != pos[0] || blad.klaar()) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.frame", pos[0] + 1, blad.symbolen(), blad.score())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        phase = Phase.VEGEN;
        timer = VEGEN;
    }

    /** Puts the pins up: a fresh rack of ten, or only sweeps away the fallen ones. */
    private void opzetten(ServerLevel world, boolean nieuw) {
        if (nieuw) {
            boolean[] alle = new boolean[10];
            java.util.Arrays.fill(alle, true);
            baan.opzetten(alle);
        } else {
            baan.vegen();
        }
        for (KegelBaan.Kegel k : baan.kegels()) {
            if (k.weg) {
                weg(world, kegels[k.nr]);
                kegels[k.nr] = null;
                continue;
            }
            Vec3 pos = punt(niveau, k.u, k.v, 0);
            if (kegels[k.nr] != null && world.getEntity(kegels[k.nr]) instanceof KegelEntity e) {
                e.zet(pos, yaw(-1, 0));
                e.om(false);
            } else {
                kegels[k.nr] = KegelEntity.create(world, KegelgrotFeature.KEGEL.get(), this, pos, yaw(-1, 0)).getUUID();
                world.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.4, pos.z, 3, 0.1, 0.2, 0.1, 0.01);
            }
        }
        if (nieuw) {
            Vec3 at = punt(niveau, KegelBaan.KOP + 0.6, 0, 0.5);
            world.playSound(null, BlockPos.containing(at), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1.2f);
        } else {
            Vec3 at = punt(niveau, KegelBaan.KOP + 0.6, 0, 0.5);
            world.playSound(null, BlockPos.containing(at), SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 1f, 1f);
        }
    }

    /** Moves the entities to where the physics put them. */
    private void zetEntities(ServerLevel world) {
        for (KegelBaan.Kegel k : baan.kegels()) {
            if (kegels[k.nr] == null || !(world.getEntity(kegels[k.nr]) instanceof KegelEntity e)) {
                continue;
            }
            if (k.weg) {
                e.discard();
                kegels[k.nr] = null;
                continue;
            }
            double dy = baan.soort == KegelBaan.Soort.RECHT && Math.abs(k.v) > KegelBaan.HALF + 0.2 ? -0.25 : 0;
            e.zet(punt(niveau, k.u, k.v, dy), k.om ? yaw(Math.cos(k.ligHoek), Math.sin(k.ligHoek)) : e.getYRot());
            e.om(k.om);
        }
        KegelBaan.Bal b = baan.bal();
        if (bal != null && world.getEntity(bal) instanceof KegelEntity e) {
            if (b.weg) {
                e.discard();
                bal = null;
            } else {
                e.zet(punt(niveau, b.u, b.v, b.goot ? -0.25 : 0), yaw(b.du, b.dv));
                e.rol((float) b.rol);
            }
        }
    }

    /** The sliding Mika-blokje of the obstacle lane. */
    private void schuif(ServerLevel world) {
        Vec3 pos = punt(niveau, KegelBaan.SCHUIF_U, KegelBaan.schuifV(world.getGameTime()), 0);
        if (schuif != null && world.getEntity(schuif) instanceof KegelEntity e) {
            e.zet(pos, yaw(-1, 0));
        } else {
            schuif = KegelEntity.create(world, KegelgrotFeature.SCHUIF.get(), this, pos, yaw(-1, 0)).getUUID();
        }
    }

    private static void weg(ServerLevel world, @Nullable UUID id) {
        if (id != null && world.getEntity(id) instanceof KegelEntity e) {
            e.discard();
        }
    }

    /** All ten frames done: De Guhde adds it up. */
    private void afgelopen(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        int score = blad.score();
        CompoundTag saved = GuhQuests.saved(p);
        String key = BEST + niveau.id();
        int oud = saved.contains(key) ? saved.getIntOr(key, 0) : -1;
        boolean record = oud >= 0 && score > oud;
        if (oud < 0 || score > oud) {
            saved.putInt(key, score);
        }
        saved.putInt(POTJES, saved.getIntOr(POTJES, 0) + 1);
        int munten = munten(niveau, score, record);
        p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.telling", score, niveau.naam(), blad.strikes())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal(blad.symbolen()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (record) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.record", score, oud).withStyle(ChatFormatting.GOLD));
        } else if (oud < 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.eerste_record", score).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.geen_record", oud).withStyle(ChatFormatting.GRAY));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.munten", munten).withStyle(ChatFormatting.LIGHT_PURPLE));
        Minigames.give(p, new ItemStack(KegelgrotFeature.KEGELKNOOP.get(), munten));
        GuhAdvancements.grant(p, "kegelen_gespeeld");
        if (score >= 150) {
            GuhAdvancements.grant(p, "kegelen_150");
        }
        if (score >= 300) {
            GuhAdvancements.grant(p, "kegelen_perfect");
        }
        if (Scorebord.submit(p, BORD + "_" + niveau.id(), score, false) == 1) {
            p.sendSystemMessage(Component.translatable("quest.guhs.kegelen.grot_record").withStyle(ChatFormatting.GOLD));
        }
        showScores(npc);
        Mijnspel.titel(p, Component.translatable("gui.guhs.kegelen.klaar", score).withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.kegelen.klaar_sub", munten), 5, 60, 15);
        Mijnspel.geluid(p, score >= 150 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 0.8f, 1.1f);
        GuhQuests.say(p, npc, "quest.guhs.kegelen.oordeel_" + (score >= 200 ? "top" : score >= 130 ? "goed" : score >= 70 ? "aardig" : "oefenen"));
        removeBal(p);
        phase = Phase.NA;
        timer = NA_TIJD;
    }

    /** Kegelknopen for a game: the score / 4 x 0.7 / 1.0 / 1.45 (at most {@link #MAX_MUNTEN}), +5 for a record. */
    public static int munten(Niveau n, int score, boolean record) {
        return Math.min(MAX_MUNTEN, (int) Math.round(score / 4.0 * MUNTEN[n.ordinal()])) + (record ? 5 : 0);
    }

    private void einde(GuhNpcEntity npc, @Nullable ServerPlayer p, @Nullable String melding) {
        ServerLevel world = (ServerLevel) npc.level();
        if (p != null) {
            removeBal(p);
            if (melding != null) {
                GuhQuests.say(p, npc, melding);
            }
        }
        opruimen(world);
        if (player != null) {
            SPELERS.remove(player, npcId);
        }
        player = null;
        phase = Phase.IDLE;
    }

    private void opruimen(ServerLevel world) {
        for (int i = 0; i < 10; i++) {
            weg(world, kegels[i]);
            kegels[i] = null;
        }
        weg(world, bal);
        weg(world, schuif);
        bal = schuif = null;
    }

    public static void leave(ServerPlayer p) {
        UUID npc = SPELERS.remove(p.getUUID());
        removeBal(p);
        KegelGame game = npc == null ? null : GAMES.get(npc);
        if (game != null && game.isPlayedBy(p)) {
            game.player = null;
            game.phase = Phase.IDLE;
        }
    }

    public static boolean hasGuhdeNearby(ServerPlayer p) {
        UUID npc = SPELERS.get(p.getUUID());
        KegelGame game = npc == null ? null : GAMES.get(npc);
        return game != null && p.level().getEntity(npc) instanceof GuhNpcEntity && p.level().getGameTime() - game.lastTick <= STALE_TICKS;
    }

    // --- the ball in your hand ------------------------------------------------------------------------------------------------

    private static void geefBal(ServerPlayer p) {
        removeBal(p);
        Inventory inv = p.getInventory();
        ItemStack stack = new ItemStack(KegelgrotFeature.WOLBAL.get());
        if (inv.getSelectedItem().isEmpty()) {
            inv.setItem(inv.getSelectedSlot(), stack);
            return;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                inv.setSelectedSlot(i);
                if (p.connection != null) {
                    p.connection.send(new ClientboundSetHeldSlotPacket(i));
                }
                return;
            }
        }
        int free = inv.getFreeSlot();
        if (free >= 0) {
            inv.setItem(free, inv.getSelectedItem());
            inv.setItem(inv.getSelectedSlot(), stack);
        } else {
            inv.add(stack);
        }
    }

    public static void removeBal(Player p) {
        p.getInventory().clearOrCountMatchingItems(s -> s.is(KegelgrotFeature.WOLBAL.get()), -1, p.inventoryMenu.getCraftSlots());
        if (p.containerMenu.getCarried().is(KegelgrotFeature.WOLBAL.get())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    // --- finding the lanes ------------------------------------------------------------------------------------------------------

    public boolean scan(GuhNpcEntity npc, boolean force) {
        if (scanned) {
            return true;
        }
        ServerLevel world = (ServerLevel) npc.level();
        BlockPos c = npc.blockPosition();
        BlockPos min = c.offset(-SCAN, -6, -SCAN), max = c.offset(SCAN, 6, SCAN);
        if (!force && !world.hasChunksAt(min, max)) {
            return false;
        }
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState s = world.getBlockState(pos);
            if (s.is(KegelgrotFeature.BAANBEGIN.get())) {
                int b = s.getValue(MijnspelBlokken.BAAN);
                begin[b] = pos.immutable();
                richting[b] = s.getValue(MijnspelBlokken.Begin.FACING);
                any = true;
            }
        }
        if (!any) {
            return false;
        }
        scanned = true;
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < 3; i++) {
            if (begin[i] != null) {
                tag.putLong("Begin" + i, begin[i].asLong());
                tag.putString("Richting" + i, richting[i].getName());
            }
        }
        npc.roleData.put("Kegelgrot", tag);
        return true;
    }

    private void load(CompoundTag data) {
        CompoundTag tag = data.getCompoundOrEmpty("Kegelgrot");
        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (tag.contains("Begin" + i)) {
                begin[i] = BlockPos.of(tag.getLongOr("Begin" + i, 0L));
                Direction d = Direction.byName(tag.getStringOr("Richting" + i, "south"));
                richting[i] = d == null || d.getAxis().isVertical() ? Direction.SOUTH : d;
                any = true;
            }
        }
        scanned = any;
    }

    /** (tests) use this lane without scanning. */
    public void testBegin(Niveau n, BlockPos pos, Direction f) {
        begin[n.ordinal()] = pos;
        richting[n.ordinal()] = f;
        scanned = true;
    }

    /** (tests) the pin entities standing now. */
    public List<KegelEntity> testKegels(ServerLevel world) {
        List<KegelEntity> out = new ArrayList<>();
        for (UUID id : kegels) {
            if (id != null && world.getEntity(id) instanceof KegelEntity e) {
                out.add(e);
            }
        }
        return out;
    }

    /** (tests) the rest of the game as these balls (a whole sheet), and De Guhde counts at once. */
    public void testBlad(int... worpen) {
        blad = new Bowling();
        for (int w : worpen) {
            blad.gooi(w);
        }
        phase = Phase.VEGEN;
        timer = 1;
    }

    /** (tests) skip the sweeping pause. */
    public void testVegenKlaar() {
        if (phase == Phase.VEGEN) {
            timer = 1;
        }
    }

    // --- the scoreboard -------------------------------------------------------------------------------------------------------

    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        List<String> boards = new ArrayList<>();
        List<Component> headings = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add(BORD + "_" + n.id());
            headings.add(Component.translatable("gui.guhs.kegelen.baan." + n.id()));
        }
        Component text = Scorebord.text(world.getServer(), Component.translatable("gui.guhs.scorebord.kegelen"), boards, headings, s -> s + " pt");
        Scorebord.show(world, npc.position().add(0, 2.6, 0), BORD, text);
    }

    public static int best(Player p, Niveau n) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(BEST + n.id()) ? saved.getIntOr(BEST + n.id(), 0) : -1;
    }

    static Component bestTekst(Player p, Niveau n) {
        int b = best(p, n);
        return Component.literal(b < 0 ? "-" : String.valueOf(b));
    }
}
