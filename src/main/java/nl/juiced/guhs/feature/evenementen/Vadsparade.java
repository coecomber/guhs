package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import org.joml.Vector3f;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The Vadsparade: the Tamboerguh (in the parade outfit) drums in front, ten dressed-up guhs march behind it in a line,
 * hopping to the beat, along a winding route over the Guhmension (to the tune of "Vader Jacob"). Walk along with it
 * until the very end: confetti, and a piece of the parade outfit for your guh (only ever from the parade: the sjako,
 * the paradejasje and the trommeltje, one per parade, in that order; after that a random piece again for another guh).
 */
public class Vadsparade extends Evenement {
    public static final int GUHS = 10;
    public static final double SPACING = 1.8, SPEED = 0.1;
    public static final int ROUTE_LENGTH = 110, MIN_ROUTE = 60;
    /** The confetti at the end. */
    public static final int FINALE = 20 * 6;
    /** Walking along: this close to the parade for at least this share of the way, and this close at the end. */
    public static final double ALONG_RANGE = 16, ALONG_SHARE = 0.6, END_RANGE = 24;
    /** The parade outfit, in the order you get it. */
    public static final GuhClothes[] OUTFIT = {GuhClothes.VADSPARADE_SJAKO, GuhClothes.VADSPARADE_JASJE, GuhClothes.VADSPARADE_TROMMELTJE};
    static final String PIECES = "guhs_parade_stukken";

    /** What the paraders wear (never the parade outfit itself: that one's only for the Tamboerguh, and for you). */
    private static final GuhClothes[][] FESTIVE = {
            {GuhClothes.PARTY_HAT, GuhClothes.KERMIS_HOED, GuhClothes.DISCO_AFRO, GuhClothes.ORANGE_CROWN, GuhClothes.JOCKEY_PET,
                    GuhClothes.WIZARD_HAT, GuhClothes.PIRATE_HAT, GuhClothes.STRAW_HAT},
            {GuhClothes.RED_BOWTIE, GuhClothes.KERMIS_STRIK, GuhClothes.BLACK_BOWTIE, GuhClothes.WINTER_SCARF},
            {GuhClothes.HEART_GLASSES, GuhClothes.SUNGLASSES, GuhClothes.DISCO_BRIL},
            {GuhClothes.STRIPED_SWEATER, GuhClothes.KERMIS_JASJE, GuhClothes.DISCO_GLITTERPAK, GuhClothes.ORANGE_SHIRT, GuhClothes.PINK_ONESIE}};
    /** Only the common colours: a parade guh must not count as having seen a rare variant in the Guhdex. */
    private static final GuhVariant[] COLOURS = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};

    /**
     * "Vader Jacob" in G, as (note block note 0..24, length in eighths): four bars of 8 eighths, each twice.
     * 13 = G, 15 = A, 17 = B, 18 = C, 20 = D, 22 = E, 8 = the low D.
     */
    static final int[][] TUNE = {
            {13, 2}, {15, 2}, {17, 2}, {13, 2}, {13, 2}, {15, 2}, {17, 2}, {13, 2},
            {17, 2}, {18, 2}, {20, 4}, {17, 2}, {18, 2}, {20, 4},
            {20, 1}, {22, 1}, {20, 1}, {18, 1}, {17, 2}, {13, 2}, {20, 1}, {22, 1}, {20, 1}, {18, 1}, {17, 2}, {13, 2},
            {13, 2}, {8, 2}, {13, 4}, {13, 2}, {8, 2}, {13, 4}};
    /** Ticks per eighth note (a brisk march: 120 beats a minute). */
    static final int EIGHTH = 5;
    static final int TUNE_EIGHTHS = 64;
    private static final Map<Integer, Integer> NOTE_AT = new HashMap<>();

    static {
        int at = 0;
        for (int[] note : TUNE) {
            NOTE_AT.put(at, note[0]);
            at += note[1];
        }
        if (at != TUNE_EIGHTHS) {
            throw new IllegalStateException("the parade tune should be " + TUNE_EIGHTHS + " eighths, not " + at);
        }
    }

    private static final DustParticleOptions[] CONFETTI = {
            new DustParticleOptions(0xFF73BF /* 1, 0.45, 0.75 */, 1.2f), new DustParticleOptions(0xFFD940 /* 1, 0.85, 0.25 */, 1.2f),
            new DustParticleOptions(0x8CE6BF /* 0.55, 0.9, 0.75 */, 1.2f), new DustParticleOptions(0x80B2FF /* 0.5, 0.7, 1 */, 1.2f),
            new DustParticleOptions(0xBF80FF /* 0.75, 0.5, 1 */, 1.2f)};

    final ParadeRoute route;
    final List<ParadeGuhEntity> guhs = new ArrayList<>();
    /** How far along the route the Tamboerguh is. */
    double s;
    int walkTicks;
    /** Ticks into the finale (-1: still marching). */
    int finale = -1;
    final Map<UUID, Integer> along = new HashMap<>();
    final List<UUID> rewarded = new ArrayList<>();

    public Vadsparade(ServerLevel level, ParadeRoute route) {
        super(EvenementType.PARADE, level, route.at(0), 1);
        this.route = route;
        this.s = start();
        this.duration = walkingTicks(route) + FINALE;
    }

    /** Where the Tamboerguh starts: far enough along that the whole line fits on the route behind it. */
    static double start() {
        return GUHS * SPACING + 1;
    }

    static int walkingTicks(ParadeRoute route) {
        return Mth.ceil((route.length() - start()) / SPEED);
    }

    /** A route near this player (starting a little way off, crossing in front of them); null when there's no room. */
    @Nullable
    public static ParadeRoute planNear(ServerLevel level, Vec3 player, RandomSource random) {
        ParadeRoute.Terrain terrain = (x, z) -> {
            var ground = Evenementen.ground(level, x, z, player.y, true);
            return ground == null || Math.abs(ground.getY() - player.y) > 12 ? null : ground.getY();
        };
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = 8 + random.nextDouble() * 6;
            double heading = angle + (random.nextBoolean() ? Math.PI / 2 : -Math.PI / 2) + (random.nextDouble() - 0.5) * 0.6;
            ParadeRoute route = ParadeRoute.plan(terrain, random, player.x + Math.cos(angle) * dist, player.z + Math.sin(angle) * dist,
                    heading, ROUTE_LENGTH, MIN_ROUTE);
            if (route != null) {
                return route;
            }
        }
        return null;
    }

    public List<ParadeGuhEntity> guhs() {
        return guhs;
    }

    public ParadeRoute route() {
        return route;
    }

    public double distanceAlong() {
        return s;
    }

    public boolean inFinale() {
        return finale >= 0;
    }

    public int alongTicks(ServerPlayer player) {
        return along.getOrDefault(player.getUUID(), 0);
    }

    public boolean rewarded(ServerPlayer player) {
        return rewarded.contains(player.getUUID());
    }

    // --- the guhs ------------------------------------------------------------------------------------------------------

    @Override
    protected void begin() {
        RandomSource random = level.getRandom();
        for (int k = 0; k <= GUHS; k++) {
            ParadeGuhEntity guh = EvenementenFeature.PARADE_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (k == 0) {
                guh.setDrummer(true);
                guh.setGuhScale(1.25f);
                guh.setPersonality(GuhPersonality.BRAVE);
                for (GuhClothes piece : OUTFIT) {
                    guh.wear(piece);
                }
            } else {
                guh.setGuhScale(0.75f + random.nextFloat() * 0.4f);
                guh.setVariant(COLOURS[random.nextInt(COLOURS.length)]);
                guh.setPersonality(GuhPersonality.PLAYFUL);
                for (GuhClothes[] slot : FESTIVE) {
                    if (random.nextInt(3) != 0) {
                        guh.wear(slot[random.nextInt(slot.length)]);
                    }
                }
            }
            double at = s - k * SPACING;
            Vec3 p = route.at(at);
            guh.snapTo(p.x, p.y, p.z, route.yawAt(at), 0);
            guh.setYHeadRot(guh.getYRot());
            guh.setYBodyRot(guh.getYRot());
            entities.add(guh.getUUID());
            level.addFreshEntity(guh);
            guhs.add(guh);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 2f, 1f);
    }

    @Override
    public Vec3 center() {
        return center;
    }

    @Override
    protected void tickEvent() {
        ParadeGuhEntity drummer = guhs.isEmpty() ? null : guhs.get(0);
        if (drummer == null || drummer.isRemoved()) {
            say("gui.guhs.evenement.parade.gone");
            end(false);
            return;
        }
        center = drummer.position();
        if (finale < 0) {
            march();
            if (s >= route.length() - 1.0e-6) {
                finale = 0;
                startFinale();
            }
        } else {
            finaleTick();
            finale++;
        }
    }

    private void march() {
        s = Math.min(route.length(), s + SPEED);
        for (int k = 0; k < guhs.size(); k++) {
            ParadeGuhEntity guh = guhs.get(k);
            if (guh.isRemoved()) {
                continue;
            }
            double at = s - k * SPACING;
            Vec3 p = route.at(at);
            // the paraders hop to the beat (each a little after the one in front); the Tamboerguh walks tall
            double hop = k == 0 ? 0 : 0.14 * Math.abs(Math.sin(Math.PI * walkTicks / (EIGHTH * 2.0) - k * 0.5));
            float yaw = route.yawAt(at);
            guh.setPos(p.x, p.y + hop, p.z);
            guh.setYRot(yaw);
            guh.setYBodyRot(yaw);
            guh.setYHeadRot(yaw);
            guh.setDeltaMovement(Vec3.ZERO);
        }
        music(walkTicks);
        if (walkTicks % 10 == 0) {
            trackWalkers();
        }
        if (walkTicks % 80 == 40) {
            guhs.get(1 + level.getRandom().nextInt(guhs.size() - 1)).triggerAnim("action", "happy");
        }
        walkTicks++;
    }

    /** The drum (every beat), a little hi-hat in between, and the flute playing the tune. */
    private void music(int tick) {
        ParadeGuhEntity drummer = guhs.get(0);
        if (tick % EIGHTH != 0) {
            return;
        }
        int eighth = tick / EIGHTH;
        if (eighth % 2 == 0) {
            SoundEvent drum = (eighth / 2) % 2 == 0 ? SoundEvents.NOTE_BLOCK_BASEDRUM.value() : SoundEvents.NOTE_BLOCK_SNARE.value();
            level.playSound(null, drummer.getX(), drummer.getY(), drummer.getZ(), drum, SoundSource.RECORDS, 2.2f, 1f);
            if ((eighth / 2) % 4 == 0) {
                level.sendParticles(ParticleTypes.NOTE, drummer.getX(), drummer.getY() + drummer.getBbHeight() + 0.4, drummer.getZ(), 1,
                        0, 0, 0, level.getRandom().nextDouble());
            }
        } else {
            level.playSound(null, drummer.getX(), drummer.getY(), drummer.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.RECORDS, 0.9f, 1.2f);
        }
        Integer note = NOTE_AT.get(eighth % TUNE_EIGHTHS);
        if (note != null) {
            ParadeGuhEntity flute = guhs.get(Math.min(3, guhs.size() - 1));
            float pitch = (float) Math.pow(2, (note - 12) / 12.0);
            level.playSound(null, flute.getX(), flute.getY(), flute.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, 2.2f, pitch);
        }
    }

    /** Who is walking along with the parade? */
    private void trackWalkers() {
        for (ServerPlayer player : players()) {
            if (nearParade(player, ALONG_RANGE)) {
                along.merge(player.getUUID(), 10, Integer::sum);
            }
        }
    }

    private boolean nearParade(ServerPlayer player, double range) {
        for (ParadeGuhEntity guh : guhs) {
            if (!guh.isRemoved() && guh.distanceTo(player) <= range) {
                return true;
            }
        }
        return false;
    }

    /** Walked along far enough, and still there at the end? */
    public boolean walkedAlong(ServerPlayer player) {
        return alongTicks(player) >= walkTicks * ALONG_SHARE && nearParade(player, END_RANGE);
    }

    // --- the finale ------------------------------------------------------------------------------------------------------

    private void startFinale() {
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.RECORDS, 3f, 1f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_LEVELUP, SoundSource.RECORDS, 1.5f, 1.2f);
        for (ParadeGuhEntity guh : guhs) {
            guh.triggerAnim("action", "happy");
        }
        for (ServerPlayer player : players()) {
            if (walkedAlong(player)) {
                reward(player);
            } else {
                player.sendSystemMessage(announce(Component.translatable("gui.guhs.evenement.parade.too_late")));
            }
        }
    }

    private void finaleTick() {
        if (finale % 20 == 0 && finale < FINALE - 20) {
            confetti(center, 3.5);
            for (ServerPlayer player : players()) {
                if (player.distanceTo(guhs.get(0)) < END_RANGE) {
                    confetti(player.position(), 2.5);
                }
            }
            level.playSound(null, center.x, center.y, center.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.RECORDS, 2f,
                    0.9f + level.getRandom().nextFloat() * 0.3f);
        }
        if (finale % 10 == 0) {
            ParadeGuhEntity guh = guhs.get(level.getRandom().nextInt(guhs.size()));
            guh.triggerAnim("action", "happy");
            guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        }
    }

    /** A burst of colourful confetti falling around this spot (only particles: nothing to clean up). */
    private void confetti(Vec3 at, double spread) {
        for (DustParticleOptions colour : CONFETTI) {
            level.sendParticles(colour, at.x, at.y + 3.5, at.z, 14, spread, 0.8, spread, 0.02);
        }
        level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 3, at.z, 10, spread * 0.6, 0.5, spread * 0.6, 0.05);
    }

    /** The prize for walking along: the next piece of the parade outfit (and some knabbels). */
    public void reward(ServerPlayer player) {
        if (rewarded.contains(player.getUUID())) {
            return;
        }
        rewarded.add(player.getUUID());
        GuhClothes piece = nextPiece(player, level.getRandom());
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.clothingItem(piece)));
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.KAAS_KNABBELS.get(), 8));
        Component name = Component.translatable("item.guhs." + piece.id());
        player.sendSystemMessage(announce(Component.translatable("gui.guhs.evenement.parade.reward", name)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("gui.guhs.evenement.parade.title")
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("gui.guhs.evenement.parade.subtitle", name)
                .withStyle(ChatFormatting.GOLD)));
        GuhAdvancements.grant(player, "evenement_parade");
        if (hasWholeOutfit(player)) {
            GuhAdvancements.grant(player, "evenement_paradepakje");
        }
    }

    /** The first piece this player hasn't had yet (sjako, jasje, trommeltje); after that a random one. Remembers it. */
    public static GuhClothes nextPiece(ServerPlayer player, RandomSource random) {
        CompoundTag data = GuhQuests.saved(player);
        int had = data.getIntOr(PIECES, 0);
        for (int i = 0; i < OUTFIT.length; i++) {
            if ((had & (1 << i)) == 0) {
                data.putInt(PIECES, had | (1 << i));
                return OUTFIT[i];
            }
        }
        return OUTFIT[random.nextInt(OUTFIT.length)];
    }

    public static boolean hasWholeOutfit(ServerPlayer player) {
        return GuhQuests.saved(player).getIntOr(PIECES, 0) == (1 << OUTFIT.length) - 1;
    }

    @Override
    protected void finish(boolean completed) {
        if (!completed && finale < 0) {
            say("gui.guhs.evenement.parade.stopped");
        }
    }

    @Override
    protected void cleanUp(Entity entity) {
        level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.4, entity.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        entity.discard();
    }
}
