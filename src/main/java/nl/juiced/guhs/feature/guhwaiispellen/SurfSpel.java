package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Surfing with Lilo-guh at the surf beach of Guhwai'i (3.0). Lilo-guh at the surf shack (plek "surf") lends you a
 * surfplankje and paddles out with you: a set of waves rolls in ({@link SurfGolven}, makkelijk / medium / lastig), you
 * catch them, ride them, pump for speed, jump off the lip for a knabbeldraai, hide in the tube and try never to fall
 * behind the break. Lilo-guh surfs right beside you on her own board.
 * <p>
 * The ride is {@link SurfSim}: the surfer's game runs it itself (smooth) and sends its keys for every step
 * ({@code guhwaiispellen_surf_stuur}); here the same steps run again in the same order and count. The score gives
 * schelpjesmunten (lastig +50 %), a personal record per level, the level's world top 3 (floating by the shack) and the
 * advancements. You can't get hurt or hungry while surfing; hold sneak for a moment to paddle back to the beach.
 */
public final class SurfSpel {
    /** The actions of Lilo-guh's screen: START + level ordinal, STOP. */
    public static final int START = 10, STOP = 2;
    public static final String SPEL = "guhwaii_surfen";
    /** Keys that arrive late are stepped in order; with no keys for this long the ride goes on by itself (a stuck game ends). */
    public static final int STIL = 200;
    /** Hold sneak this long to paddle back to the beach (the game ends, the score counts). */
    public static final int STOP_TICKS = 30;
    /** The welcome present of your first surf game. */
    public static final int EERSTE_MUNTEN = 4;

    private static final Map<UUID, SurfSpel> SPELERS = new ConcurrentHashMap<>();

    final UUID speler;
    final UUID npc;
    final BlockPos npcPos;
    final Niveau niveau;
    final Surfplek.Spot spot;
    final SurfSim sim;
    final SurfSim lilo;
    final int seed;
    @Nullable
    SurfPlankEntity bord;
    @Nullable
    SurfPlankEntity liloBord;
    private final ArrayDeque<int[]> invoer = new ArrayDeque<>();
    private int stil, sneak;
    private boolean klaar;
    /** 1.2.7: the world of this beach (a surfer who changed dimension is not put on "the beach" of the other world). */
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensie;

    private SurfSpel(ServerPlayer p, GuhNpcEntity npc, Niveau niveau, Surfplek.Spot spot, int seed) {
        this.speler = p.getUUID();
        this.npc = npc.getUUID();
        this.npcPos = npc.blockPosition();
        this.dimensie = npc.level().dimension();
        this.niveau = niveau;
        this.spot = spot;
        this.seed = seed;
        SurfGolven golven = new SurfGolven(niveau, seed);
        this.sim = new SurfSim(golven);
        this.lilo = new SurfSim(new SurfGolven(niveau, seed));
    }

    @Nullable
    public static SurfSpel van(Player p) {
        return SPELERS.get(p.getUUID());
    }

    public static boolean surft(Player p) {
        return SPELERS.containsKey(p.getUUID());
    }

    public SurfSim sim() {
        return sim;
    }

    public SurfSim lilo() {
        return lilo;
    }

    public Niveau niveau() {
        return niveau;
    }

    // --- Lilo-guh at the surf shack ------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        boolean bezig = surft(player);
        CompoundTag saved = GuhwaiiSpellenFeature.data(player);
        GuhQuests.say(player, npc, bezig ? "quest.guhs.guhwaiispellen.surf.bezig" : saved.getBooleanOr("EersteSurf", false)
                ? "quest.guhs.guhwaiispellen.surf.hallo" + (1 + player.getRandom().nextInt(4)) : "quest.guhs.guhwaiispellen.surf.welkom");
        CompoundTag data = GuhwaiiSpellenFeature.scherm(player, "surf");
        data.putBoolean("Mine", bezig);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new GuhwaiiSpellenPayloads.Open(npc.getId(), data));
    }

    public static void actie(GuhNpcEntity npc, ServerPlayer player, int actie) {
        if (player.distanceToSqr(npc) > 100 && !surft(player)) {
            return;
        }
        if (actie == STOP) {
            SurfSpel s = van(player);
            if (s != null) {
                s.einde((ServerLevel) player.level(), player, true);
            }
        } else if (actie >= START && actie < START + Niveau.values().length) {
            start(npc, player, Niveau.of(actie - START));
        }
    }

    /** Starts a game on this level: the board, Lilo-guh beside you, the first wave on its way. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau) {
        ServerLevel level = (ServerLevel) npc.level();
        if (surft(player) || Minigames.refuse(player, npc, SPEL)) {
            return false;
        }
        Surfplek.Spot spot = spot(npc);
        if (spot == null) {
            GuhQuests.say(player, npc, "quest.guhs.guhwaiispellen.surf.geen_golven");
            return false;
        }
        return start(npc, player, niveau, spot, level.getRandom().nextInt());
    }

    /** (Also for the tests: a given spot and seed.) */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau, Surfplek.Spot spot, int seed) {
        ServerLevel level = (ServerLevel) npc.level();
        SurfSpel s = new SurfSpel(player, npc, niveau, spot, seed);
        SurfPlankEntity bord = GuhwaiiSpellenBlocks.SURFPLANK.get().create(level, EntitySpawnReason.TRIGGERED);
        SurfPlankEntity liloBord = GuhwaiiSpellenBlocks.SURFPLANK.get().create(level, EntitySpawnReason.TRIGGERED);
        if (bord == null || liloBord == null) {
            return false;
        }
        bord.zetSpot(spot, niveau, seed, false);
        bord.volg(s.sim);
        liloBord.zetSpot(spot, niveau, seed, true);
        s.lilo.stap(0);                                          // (Lilo paddles one step ahead, a little to the side)
        liloBord.volg(s.lilo);
        level.addFreshEntity(bord);
        level.addFreshEntity(liloBord);
        s.bord = bord;
        s.liloBord = liloBord;
        SPELERS.put(player.getUUID(), s);
        player.stopRiding();
        Vec3 p = bord.position();
        player.teleportTo(level, p.x, p.y + SurfPlankEntity.STAAN, p.z, java.util.Set.of(), spot.strandYaw(), 10, true);
        player.startRiding(bord, true, true);
        Minigames.startKeeping(player);
        if (GuhQuests.count(player, GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get()) == 0) {
            player.getInventory().add(new ItemStack(GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get()));
        }
        CompoundTag start = new CompoundTag();
        start.putInt("Bord", bord.getId());
        start.putInt("Lilo", liloBord.getId());
        start.putLong("Origin", spot.origin().asLong());
        start.putDouble("Hoek", spot.hoek());
        start.putInt("Niveau", niveau.ordinal());
        start.putInt("Seed", seed);
        start.putInt("Record", GuhwaiiSpellenFeature.data(player).getIntOr("Surf_" + niveau.id(), 0));
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new GuhwaiiSpellenPayloads.SurfStart(start));
        GuhwaiiSpellenFeature.titel(player, Component.translatable("gui.guhs.guhwaiispellen.surf.titel").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.guhwaiispellen.surf.titel.sub", niveau.naam()), 50);
        player.sendSystemMessage(Component.translatable("quest.guhs.guhwaiispellen.surf.uitleg").withStyle(ChatFormatting.AQUA));
        level.playSound(null, npc.blockPosition(), GuhwaiiSpellenBlocks.ALOHA.get(), SoundSource.NEUTRAL, 1f, 1f);
        return true;
    }

    /** The surf spot of this Lilo-guh (found once, then remembered in her roleData). */
    @Nullable
    public static Surfplek.Spot spot(GuhNpcEntity npc) {
        CompoundTag rd = npc.roleData;
        if (rd.contains("SurfOrigin")) {
            return new Surfplek.Spot(BlockPos.of(rd.getLongOr("SurfOrigin", 0L)), rd.getDoubleOr("SurfHoek", 0.0));
        }
        Surfplek.Spot spot = Surfplek.zoek(npc.level(), npc.blockPosition());
        if (spot != null) {
            rd.putLong("SurfOrigin", spot.origin().asLong());
            rd.putDouble("SurfHoek", spot.hoek());
        }
        return spot;
    }

    // --- the ride ------------------------------------------------------------------------------------------------------

    /** The keys of step `stap` from the surfer's game. */
    public static void invoer(ServerPlayer player, int bordId, int stap, int bits) {
        SurfSpel s = van(player);
        if (s == null || s.bord == null || s.bord.getId() != bordId || s.klaar) {
            return;
        }
        if (s.invoer.size() < 400) {
            s.invoer.add(new int[]{stap, bits});
        }
    }

    /** (Tests) step the ride with these keys right away. */
    public void stapNu(ServerPlayer player, int bits) {
        stap((ServerLevel) player.level(), player, bits);
    }

    /** Every tick of the surfer (server). */
    static void tick(ServerPlayer player) {
        SurfSpel s = van(player);
        if (s == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (s.bord == null || s.bord.isRemoved() || player.getVehicle() != s.bord) {
            s.einde(level, player, false);
            return;
        }
        Minigames.keep(player);
        // hold sneak: back to the beach
        if (player.isShiftKeyDown()) {
            if (++s.sneak == 10) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhwaiispellen.surf.sneak").withStyle(ChatFormatting.GRAY));
            }
            if (s.sneak >= STOP_TICKS) {
                s.einde(level, player, true);
                return;
            }
        } else {
            s.sneak = 0;
        }
        int n = 0;
        while (!s.invoer.isEmpty() && n < 6 && !s.klaar) {
            int[] in = s.invoer.poll();
            if (in[0] != s.sim.step() + 1) {
                continue;                                         // (a step we already had)
            }
            s.stap(level, player, in[1]);
            n++;
            s.stil = 0;
        }
        if (n == 0 && ++s.stil > STIL && !s.klaar) {
            s.stap(level, player, 0);                            // (no keys any more: the ride goes on by itself)
        }
        if (s.klaar && player.isAlive()) {
            s.einde(level, player, true);
        }
    }

    private void stap(ServerLevel level, ServerPlayer player, int bits) {
        List<SurfSim.Gebeurtenis> events = sim.stap(bits);
        lilo.stap(SurfSim.liloInvoer(lilo));
        if (bord != null) {
            bord.volg(sim);
        }
        if (liloBord != null) {
            liloBord.volg(lilo);
        }
        for (SurfSim.Gebeurtenis e : events) {
            gebeurt(level, player, e);
        }
        // the waves break with a roar (for everyone on the beach), once per wave
        SurfGolven g = sim.golven();
        for (int k = 0; k < g.golven().size(); k++) {
            if ((int) Math.ceil(g.breekStap(k)) == sim.step()) {
                Vec3 w = spot.wereld(SurfGolven.U_BREEK, g.golf(k).piek(), 0);
                level.playSound(null, w.x, w.y, w.z, GuhwaiiSpellenBlocks.GOLF_BREEKT.get(), SoundSource.AMBIENT, 2.2f, 0.9f + 0.1f * (k % 3));
            }
        }
    }

    private void gebeurt(ServerLevel level, ServerPlayer player, SurfSim.Gebeurtenis e) {
        Vec3 p = bord == null ? player.position() : bord.position();
        switch (e.soort()) {
            case VANG -> level.playSound(null, p.x, p.y, p.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6f, 1.3f);
            case TRUC, CUTBACK, TUBE -> {
                level.playSound(null, p.x, p.y, p.z, GuhwaiiSpellenBlocks.UKELELE.get(), SoundSource.PLAYERS, 0.8f, 1.0f + 0.1f * Math.min(4, sim.mult()));
                level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.3, p.z, 30, 0.6, 0.3, 0.6, 0.2);
                level.sendParticles(ParticleTypes.HEART, p.x, p.y + 2.2, p.z, 2, 0.4, 0.2, 0.4, 0);
                if (e.soort() == SurfSim.Soort.TRUC && (e.naam().startsWith("draai") || e.naam().startsWith("dubbel") || e.naam().startsWith("drie"))) {
                    GuhwaiiSpellenFeature.grant(player, "guhwaii_spellen_knabbeldraai");
                }
                if (e.soort() == SurfSim.Soort.TUBE) {
                    GuhwaiiSpellenFeature.grant(player, "guhwaii_spellen_tube");
                }
            }
            case TUBE_IN -> level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y + 1, p.z, 12, 0.5, 0.5, 0.5, 0.05);
            case PLONS -> {
                level.playSound(null, p.x, p.y, p.z, GuhwaiiSpellenBlocks.PLONS.get(), SoundSource.PLAYERS, 1f, 1f);
                level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.3, p.z, 60, 0.8, 0.4, 0.8, 0.3);
                level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 20, 0.5, 0.2, 0.5, 0.1);
                Entity lilo = level.getEntity(npc);
                if (lilo != null) {
                    GuhQuests.say(player, lilo, "quest.guhs.guhwaiispellen.surf.plons" + (1 + level.getRandom().nextInt(4)));
                }
            }
            case GOLF_KLAAR -> {
                level.playSound(null, p.x, p.y, p.z, GuhwaiiSpellenBlocks.ALOHA.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
                GuhwaiiSpellenFeature.grant(player, "guhwaii_spellen_surf");
            }
            case KLAAR -> klaar = true;
            default -> {
            }
        }
    }

    // --- the end -------------------------------------------------------------------------------------------------------

    /** The game is over (telt: the score counts, also when you paddled back early). */
    void einde(ServerLevel level, ServerPlayer player, boolean telt) {
        if (SPELERS.remove(player.getUUID()) != this) {
            return;
        }
        klaar = true;
        int score = sim.score();
        CompoundTag data = GuhwaiiSpellenFeature.data(player);
        CompoundTag uitslag = new CompoundTag();
        uitslag.putInt("Score", score);
        uitslag.putInt("Niveau", niveau.ordinal());
        if (telt && player.isAlive()) {
            int munten = niveau.munten(SurfSim.munten(score));
            if (munten > 0) {
                Minigames.give(player, new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), munten));
                level.playSound(null, player.blockPosition(), GuhwaiiSpellenBlocks.SCHELPJE.get(), SoundSource.PLAYERS, 1f, 1f);
            }
            uitslag.putInt("Munten", munten);
            int oud = data.getIntOr("Surf_" + niveau.id(), 0);
            if (score > oud) {
                data.putInt("Surf_" + niveau.id(), score);
                uitslag.putBoolean("Record", true);
            }
            player.sendSystemMessage(Component.translatable("quest.guhs.guhwaiispellen.surf.uitslag", score, niveau.naam(), munten,
                    sim.gereden(), sim.trucs()).withStyle(ChatFormatting.GOLD));
            if (score > oud) {
                player.sendSystemMessage((oud > 0 ? Component.translatable("quest.guhs.guhwaiispellen.record", score, oud)
                        : Component.translatable("quest.guhs.guhwaiispellen.record_eerste", score)).withStyle(ChatFormatting.YELLOW));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
            if (score > 0) {
                int plek = Scorebord.submit(player, "surfen_" + niveau.id(), score, false);
                uitslag.putInt("Plek", plek);
                GuhwaiiSpellenFeature.gespeeld(player, "surfen_" + niveau.id());
                nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guhwaii_spellen_surf_" + niveau.id());
            }
            if (niveau == Niveau.LASTIG && score >= LASTIG_KONING) {
                GuhwaiiSpellenFeature.grant(player, "guhwaii_spellen_surf_lastig");
            }
            if (!data.getBooleanOr("EersteSurf", false)) {
                data.putBoolean("EersteSurf", true);
                Minigames.give(player, new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), EERSTE_MUNTEN));
                Minigames.give(player, new ItemStack(GuhwaiiSpellenBlocks.SURFPLANK_REK.get()));
                Entity lilo = level.getEntity(npc);
                if (lilo != null) {
                    GuhQuests.say(player, lilo, "quest.guhs.guhwaiispellen.surf.eerste");
                }
            }
        }
        opruimen(level, player);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new GuhwaiiSpellenPayloads.SurfEinde(uitslag));
    }

    /** A score on lastig that makes you the queen or king of the waves (the challenge). */
    public static final int LASTIG_KONING = 6000;

    private void opruimen(ServerLevel level, ServerPlayer player) {
        if (player.getVehicle() == bord) {
            player.stopRiding();
        }
        if (bord != null) {
            bord.discard();
        }
        if (liloBord != null) {
            liloBord.discard();
        }
        // the loaned board goes back to Lilo-guh
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        Minigames.forget(player);
        // back on the beach, next to Lilo-guh
        if (player.isAlive() && player.level() == level && level.dimension() == dimensie) {
            Vec3 beach = Vec3.atBottomCenterOf(npcPos).add(Vec3.atLowerCornerOf(spot.origin().subtract(npcPos)).normalize().scale(-1.5));
            BlockPos b = BlockPos.containing(beach);
            while (!level.getBlockState(b).isAir() && b.getY() < npcPos.getY() + 4) {
                b = b.above();
            }
            player.teleportTo(level, beach.x, b.getY(), beach.z, java.util.Set.of(), spot.strandYaw() + 180, 0, true);
        }
    }

    // --- events --------------------------------------------------------------------------------------------------------

    /** You can't step off the board on the way (hold sneak to paddle back to the beach). */
    static void opAfstappen(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof ServerPlayer player && event.getEntityBeingMounted() instanceof SurfPlankEntity) {
            SurfSpel s = van(player);
            if (s != null && !s.klaar && player.isAlive()) {
                event.setCanceled(true);
            }
        }
    }

    static void opUitloggen(ServerPlayer player) {
        SurfSpel s = SPELERS.get(player.getUUID());
        if (s != null) {
            s.einde((ServerLevel) player.level(), player, false);
        }
    }

    static void vergeetAlles() {
        SPELERS.clear();
    }
}
