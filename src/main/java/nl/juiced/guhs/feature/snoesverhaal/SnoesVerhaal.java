package nl.juiced.guhs.feature.snoesverhaal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubKleding;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomSlapen;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * 1.8.0 Guhdromenland, part F (bouwplan F; ontwerp 1b, rounds 5, 6 and 11): the story of Snoes, the guh who can't sleep.
 * <ol>
 *   <li><b>Proloog</b>: once a player has a tamed guh, the first time they click a bed in the evening a ruffled guh sits beside
 *       it: Snoes. Talk to him three times and he lets himself be tucked in (on a sleeping place close by, when there is one).
 *       Sleep in that bed with Snoes beside it and you dream: the Droomplein, where Bubbels the dream-guide Blub waits.</li>
 *   <li><b>Five pieces</b> in any order (the islands call {@link DroomVoortgang#geef}): each piece gives its weerpotje
 *       ({@link DroomBronnen}), a hidden advancement and a word from Bubbels. Snoes (at home), the Zandguh, Oma-guh and the
 *       Rommelguh give hints ({@link DroomNpc#zet}).</li>
 *   <li><b>Finale</b> (all five pieces, back on the Droomplein): his own family day ({@link SnoesScenes#FINALE}); afterwards
 *       Snoes lives on the plein (SNOES_BLIJ in his box), the player can see and tame wild Blubs
 *       ({@link DroomVoortgang#zetFinale}), gets the regenboog-potje, and Bubbels comes home with them as their first Blub.</li>
 * </ol>
 * State per player in {@code GuhQuests.saved(player)["guhs_snoes_verhaal"]}: Fase, Snoes (uuid of the Snoes at their bed),
 * Bed, Gids (uuid of Bubbels in the dream), GidsThuis, Welkom.
 */
public final class SnoesVerhaal {
    static final String KEY = "guhs_snoes_verhaal";
    /** Tags: the Snoes at a player's bed, Bubbels in the dream, the Snoes who lives on the plein after the story. */
    public static final String TAG_THUIS = "guhs_snoes_thuis", TAG_GIDS = "guhs_droomgids", TAG_PLEIN = "guhs_snoes_plein";
    /** roleData of a home Snoes: whose Snoes he is; how far the first talk got. */
    static final String VAN = "SnoesVan", STAP = "SnoesStap";
    /** How close Snoes must be to the bed (blocks), and how far the finale starts from the plein's arrival spot. */
    static final int BED_BEREIK = 6, FINALE_BEREIK = 24, SLAAPPLEK_ZOEK = 6;
    /** Where the plein things stand, relative to the plein's arrival spot (also the finale scene's anchor). */
    static final BlockPos DOOS = new BlockPos(-2, 0, 5), KATTENHUISJE = new BlockPos(2, 0, 5);
    static final BlockPos[] KOEKEN = {new BlockPos(-1, 0, 8), new BlockPos(0, 0, 8), new BlockPos(1, 0, 8)};

    /** The story's phases (per player). */
    public enum Fase {
        NIETS, BIJ_BED, INGESTOPT, DROOMT, KLAAR;

        static Fase van(String s) {
            for (Fase f : values()) {
                if (f.name().equals(s)) {
                    return f;
                }
            }
            return NIETS;
        }
    }

    /** Lines that come a moment later (Bubbels talking): per player, (game time, speaker uuid, key, args). */
    private record Regel(long op, UUID spreker, String key, Object[] args) {
    }

    private static final Map<UUID, Deque<Regel>> LATER = new ConcurrentHashMap<>();

    private SnoesVerhaal() {
    }

    // =====================================================================================================================
    // state
    // =====================================================================================================================

    static CompoundTag data(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        saved.put(KEY, t);
        return t;
    }

    public static Fase fase(ServerPlayer p) {
        return Fase.van(data(p).getStringOr("Fase", ""));
    }

    static void zetFase(ServerPlayer p, Fase f) {
        data(p).putString("Fase", f.name());
    }

    @Nullable
    private static UUID uuid(ServerPlayer p, String key) {
        String s = data(p).getStringOr(key, "");
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static Entity zoek(ServerPlayer p, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                return e;
            }
        }
        return null;
    }

    /** The Snoes at this player's bed (null: none, or not loaded). */
    @Nullable
    public static GuhNpcEntity snoesThuis(ServerPlayer p) {
        return zoek(p, uuid(p, "Snoes")) instanceof GuhNpcEntity n ? n : null;
    }

    /** Bubbels in the dream (null: none, or not loaded). */
    @Nullable
    public static BlubGuhEntity gids(ServerPlayer p) {
        return zoek(p, uuid(p, "Gids")) instanceof BlubGuhEntity b ? b : null;
    }

    /** Forgets this player's story (DroomVoortgang "reset" comes here too): their Snoes and Bubbels go away. */
    public static void vergeet(ServerPlayer p) {
        GuhNpcEntity s = snoesThuis(p);
        if (s != null) {
            s.discard();
        }
        BlubGuhEntity g = gids(p);
        if (g != null && g.entityTags().contains(TAG_GIDS)) {
            g.discard();
        }
        GuhQuests.saved(p).remove(KEY);
        LATER.remove(p.getUUID());
    }

    // =====================================================================================================================
    // the prologue: Snoes at your bed
    // =====================================================================================================================

    static void onKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND || DroomReis.inDroom(p)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof BedBlock) || fase(p) != Fase.NIETS
                || !DroomSlapen.bedtijd(level) || !heeftTammeGuh(p)) {
            return;
        }
        if (verschijn(p, event.getPos()) != null) {
            event.setCanceled(true);                // (not to sleep yet: there is somebody at your bed)
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Does this player have a tamed guh (one of theirs close by)? */
    static boolean heeftTammeGuh(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(GuhEntity.class, p.getBoundingBox().inflate(64),
                g -> g.isAlive() && g.isTame() && g.isOwnedBy(p)).isEmpty();
    }

    /** Snoes appears beside this bed (or this spot) for this player; null when there is no room. */
    @Nullable
    public static GuhNpcEntity verschijn(ServerPlayer p, BlockPos bed) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos plek = vrijePlek(level, bed);
        if (plek == null) {
            return null;
        }
        GuhNpcEntity oud = snoesThuis(p);
        if (oud != null) {
            oud.discard();
        }
        Vec3 at = Vec3.atBottomCenterOf(plek);
        float yaw = (float) Math.toDegrees(Math.atan2(-(bed.getX() + 0.5 - at.x), bed.getZ() + 0.5 - at.z));
        GuhNpcEntity snoes = DroomNpc.spawn(level, at, yaw, Kind.SNOES);
        snoes.addTag(TAG_THUIS);
        snoes.roleData.putString(VAN, p.getUUID().toString());
        snoes.setInvulnerable(true);
        snoes.setPersistenceRequired();
        data(p).putString("Snoes", snoes.getUUID().toString());
        data(p).putLong("Bed", bed.asLong());
        zetFase(p, Fase.BIJ_BED);
        p.sendSystemMessage(Component.translatable("gui.guhs.snoes.verschijnt").withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, snoes, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 0.75f);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5, at.z, 6, 0.3, 0.3, 0.3, 0.01);
        GuhAdvancements.grant(p, "snoes_proloog");
        return snoes;
    }

    /** A free spot right next to a bed (air with air above, something to stand on). */
    @Nullable
    static BlockPos vrijePlek(ServerLevel level, BlockPos bed) {
        List<BlockPos> rond = new ArrayList<>();
        for (BlockPos b : new BlockPos[]{bed, bed.relative(level.getBlockState(bed).getBlock() instanceof BedBlock
                ? BedBlock.getConnectedDirection(level.getBlockState(bed)) : Direction.NORTH)}) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                rond.add(b.relative(d));
            }
        }
        rond.add(bed.above());
        for (BlockPos c : rond) {
            if (level.getBlockState(c).isAir() && level.getBlockState(c.above()).isAir()
                    && level.getBlockState(c.below()).isFaceSturdy(level, c.below(), Direction.UP)) {
                return c.immutable();
            }
        }
        return null;
    }

    /** A player talks to Snoes (the story's talk for Kind SNOES). */
    static boolean praatSnoes(ServerPlayer p, GuhNpcEntity snoes) {
        if (!snoes.entityTags().contains(TAG_THUIS)) {                    // a Snoes in the dream
            if (fase(p) == Fase.DROOMT) {
                GuhQuests.say(p, snoes, "gui.guhs.snoes.droom." + p.getRandom().nextInt(3), DroomVoortgang.aantal(p));
                return true;
            }
            return false;
        }
        if (!snoes.roleData.getStringOr(VAN, "").equals(p.getUUID().toString())) {
            GuhQuests.say(p, snoes, "gui.guhs.snoes.ander");
            return true;
        }
        switch (fase(p)) {
            case BIJ_BED -> {
                int stap = snoes.roleData.getIntOr(STAP, 0);
                if (stap < 2) {
                    GuhQuests.say(p, snoes, "gui.guhs.snoes.proloog." + stap);
                    snoes.roleData.putInt(STAP, stap + 1);
                } else if (!DroomSlapen.bedtijd((ServerLevel) snoes.level())) {
                    GuhQuests.say(p, snoes, "gui.guhs.snoes.geen_bedtijd");
                } else {
                    instoppen(p, snoes);
                }
            }
            case INGESTOPT -> GuhQuests.say(p, snoes, "gui.guhs.snoes.wakker_liggen");
            case DROOMT -> GuhQuests.say(p, snoes, "gui.guhs.snoes.thuis." + p.getRandom().nextInt(3), DroomVoortgang.aantal(p));
            default -> GuhQuests.say(p, snoes, "gui.guhs.snoes.proloog.0");
        }
        return true;
    }

    /** Tucks Snoes in: onto a sleeping place close by (the doos, a kattenhuisje...) when there is one, else where he sits. */
    static void instoppen(ServerPlayer p, GuhNpcEntity snoes) {
        ServerLevel level = (ServerLevel) snoes.level();
        BlockPos beste = null;
        double d = Double.MAX_VALUE;
        for (BlockPos b : BlockPos.betweenClosed(snoes.blockPosition().offset(-SLAAPPLEK_ZOEK, -2, -SLAAPPLEK_ZOEK),
                snoes.blockPosition().offset(SLAAPPLEK_ZOEK, 2, SLAAPPLEK_ZOEK))) {
            if (level.getBlockState(b).is(DromenFeature.SLAAPPLEKKEN) && b.distSqr(snoes.blockPosition()) < d) {
                d = b.distSqr(snoes.blockPosition());
                beste = b.immutable();
            }
        }
        if (beste != null) {
            snoes.snapTo(beste.getX() + 0.5, beste.getY() + 0.4, beste.getZ() + 0.5, snoes.getYRot(), 0f);
        }
        zetFase(p, Fase.INGESTOPT);
        GuhQuests.say(p, snoes, beste != null ? "gui.guhs.snoes.ingestopt" : "gui.guhs.snoes.ingestopt_vloer");
        p.sendSystemMessage(Component.translatable("gui.guhs.snoes.ga_slapen").withStyle(ChatFormatting.LIGHT_PURPLE));
        level.sendParticles(ParticleTypes.HEART, snoes.getX(), snoes.getY() + 0.7, snoes.getZ(), 4, 0.3, 0.2, 0.3, 0);
    }

    /** Asleep in the bed with Snoes tucked in beside it: off to the dream (the Droomplein). */
    static void onSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isSleeping() || p.getSleepTimer() < DroomSlapen.INSLAAP_TICKS
                || DroomReis.inDroom(p) || fase(p) != Fase.INGESTOPT) {
            return;
        }
        GuhNpcEntity snoes = snoesThuis(p);
        BlockPos bed = p.getSleepingPos().orElse(p.blockPosition());
        if (snoes == null || snoes.level() != p.level() || snoes.distanceToSqr(Vec3.atCenterOf(bed)) > BED_BEREIK * BED_BEREIK) {
            return;
        }
        zetFase(p, Fase.DROOMT);
        if (!DroomReis.naarDroom(p, DroomEilanden.PLEIN)) {
            zetFase(p, Fase.INGESTOPT);
        }
    }

    // =====================================================================================================================
    // Bubbels, the dream guide
    // =====================================================================================================================

    static void onDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (event.getTo().equals(DromenFeature.DIM)) {
            Fase f = fase(p);
            if (f == Fase.DROOMT || (f == Fase.KLAAR && !data(p).getBooleanOr("GidsThuis", false))) {
                roepGids(p);
            }
        } else if (event.getFrom().equals(DromenFeature.DIM)) {
            gidsWeg(p);
        }
    }

    /** Bubbels comes to the player in the dream (made when needed); the first time with his welcome. */
    public static BlubGuhEntity roepGids(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        BlubGuhEntity gids = gids(p);
        if (gids == null || gids.level() != level) {
            if (gids != null) {
                gids.discard();
            }
            gids = maakBubbels(level, p);
            gids.snapTo(p.getX() + 1, p.getY(), p.getZ() + 1, p.getYRot() + 180, 0f);
            gids.addTag(TAG_GIDS);
            level.addFreshEntity(gids);
            data(p).putString("Gids", gids.getUUID().toString());
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, gids.getX(), gids.getY() + 0.4, gids.getZ(), 16, 0.3, 0.3, 0.3, 0.02);
        if (!data(p).getBooleanOr("Welkom", false)) {
            data(p).putBoolean("Welkom", true);
            for (int i = 0; i < 4; i++) {
                later(p, gids, 30 + i * 70, "gui.guhs.snoes.gids.welkom." + i);
            }
        } else if (fase(p) == Fase.DROOMT) {
            later(p, gids, 30, "gui.guhs.snoes.gids.terug", DroomVoortgang.aantal(p));
            status(p);
        }
        return gids;
    }

    /** A new Bubbels: a plain blue Blub with a pink strikje, tamed to this player. */
    static BlubGuhEntity maakBubbels(ServerLevel level, ServerPlayer p) {
        BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(level, EntitySpawnReason.EVENT);
        b.zetWaarden(new BlubTrekking.Waarden(35, BlubTrekking.Soort.BLAUW, 0, 1.0f));
        b.tame(p);
        BlubKleding.Stuk strik = BlubKleding.stuk("strik_" + DyeColor.PINK.getName());
        if (strik != null) {
            b.kleedAan(BlubKleding.Plek.STRIK, strik);
        }
        b.setCustomName(Component.translatable("gui.guhs.snoes.gids.naam"));
        b.setPersistenceRequired();
        return b;
    }

    /** Leaving the dream: Bubbels stays behind (gone), or after the finale he comes home as the player's own first Blub. */
    static void gidsWeg(ServerPlayer p) {
        BlubGuhEntity gids = gids(p);
        if (fase(p) == Fase.KLAAR && !data(p).getBooleanOr("GidsThuis", false)) {
            ServerLevel thuis = (ServerLevel) p.level();
            BlubGuhEntity mee = maakBubbels(thuis, p);
            if (gids != null) {
                mee.zetWaarden(gids.waarden());
            }
            mee.snapTo(p.getX() + 0.8, p.getY(), p.getZ() + 0.8, p.getYRot(), 0f);
            thuis.addFreshEntity(mee);
            data(p).putBoolean("GidsThuis", true);
            GuhQuests.say(p, mee, "gui.guhs.snoes.gids.thuis");
            thuis.sendParticles(ParticleTypes.HEART, mee.getX(), mee.getY() + 0.6, mee.getZ(), 6, 0.3, 0.2, 0.3, 0);
        }
        if (gids != null) {
            gids.discard();
        }
        data(p).remove("Gids");
    }

    /** The HUD line: how many pieces, and which are still missing. */
    static void status(ServerPlayer p) {
        List<String> nog = new ArrayList<>();
        for (Stukje s : Stukje.values()) {
            if (!DroomVoortgang.heeft(p, s)) {
                nog.add(s.id());
            }
        }
        Component lijst = Component.empty();
        for (int i = 0; i < nog.size(); i++) {
            lijst = lijst.copy().append(Component.translatable("gui.guhs.snoes.stukje." + nog.get(i)));
            if (i < nog.size() - 1) {
                lijst = lijst.copy().append(", ");
            }
        }
        p.sendOverlayMessage((nog.isEmpty() ? Component.translatable("gui.guhs.snoes.status.alles")
                : Component.translatable("gui.guhs.snoes.status", DroomVoortgang.aantal(p), lijst)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A line of the speaker for this player, a few ticks later. */
    static void later(ServerPlayer p, Entity spreker, int ticks, String key, Object... args) {
        LATER.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>())
                .add(new Regel(p.level().getGameTime() + ticks, spreker.getUUID(), key, args));
    }

    // =====================================================================================================================
    // the pieces (DroomVoortgang listener)
    // =====================================================================================================================

    static void opVoortgang(ServerPlayer p, String wat) {
        if ("reset".equals(wat)) {
            vergeet(p);
            return;
        }
        Stukje s = Stukje.van(wat);
        if (s == null) {
            return;
        }
        if (fase(p).ordinal() < Fase.DROOMT.ordinal()) {
            zetFase(p, Fase.DROOMT);        // (a piece found some other way, e.g. a test command: the story is on)
        }
        DroomBronnen.geef(p, DroomBronnen.weertjeVoor(s));
        GuhAdvancements.grant(p, "snoes_" + s.id());
        BlubGuhEntity gids = gids(p);
        Entity spreker = gids != null ? gids : p;
        if (gids != null) {
            later(p, gids, 10, "gui.guhs.snoes.gids.stukje." + s.id(), DroomVoortgang.aantal(p));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.snoes.gids.stukje." + s.id(), DroomVoortgang.aantal(p))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (DroomVoortgang.alleStukjes(p)) {
            later(p, spreker, 90, "gui.guhs.snoes.gids.alles");
        }
        status(p);
    }

    // =====================================================================================================================
    // the finale
    // =====================================================================================================================

    /** The arrival spot of the Droomplein: the anchor of the finale and of Snoes' corner. */
    static BlockPos pleinAnker() {
        return BlockPos.containing(DroomEilanden.wereld(DroomEilanden.PLEIN, DroomEilanden.opzet(DroomEilanden.PLEIN).aankomst().plek()));
    }

    /** Plays the finale for this player on the Droomplein; false when it can't start now. */
    public static boolean startFinale(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !DromenFeature.isDroom(level) || Cutscenes.bezig(p)) {
            return false;
        }
        BlockPos anker = pleinAnker();
        hoekje(level, anker);
        return Cutscenes.speel(p, SnoesScenes.FINALE, anker, Rotation.NONE, SnoesVerhaal::naFinale);
    }

    /** After the finale (also the test command): Snoes is helped, the rewards, and Snoes moves to the plein. */
    public static void naFinale(ServerPlayer p) {
        DroomVoortgang.zetFinale(p);                       // (also "Snoes geholpen": the wild Blubs show themselves now)
        DroomBronnen.geef(p, Weertje.REGENBOOG);
        GuhAdvancements.grant(p, "snoes_finale");
        zetFase(p, Fase.KLAAR);
        GuhNpcEntity thuis = snoesThuis(p);
        if (thuis != null) {
            thuis.discard();                               // (he lives on the Droomplein now)
        }
        data(p).remove("Snoes");
        p.sendSystemMessage(Component.translatable("gui.guhs.snoes.klaar").withStyle(ChatFormatting.GOLD));
        if (p.level() instanceof ServerLevel level && DromenFeature.isDroom(level)) {
            BlockPos anker = pleinAnker();
            hoekje(level, anker);
            pleinSnoes(level, anker);
            Vec3 m = Vec3.atCenterOf(anker.offset(0, 6, 5));
            level.sendParticles(ParticleTypes.END_ROD, m.x, m.y, m.z, 80, 5, 2, 5, 0.02);
            level.sendParticles(ParticleTypes.FIREWORK, m.x, m.y, m.z, 40, 4, 2, 4, 0.05);
            BlubGuhEntity gids = gids(p);
            if (gids != null) {
                later(p, gids, 40, "gui.guhs.snoes.gids.mee");
            }
        }
    }

    /** Snoes' corner on the plein: the box and the kattenhuisje (when the plein has none of its own) and the pink cakes. */
    static void hoekje(ServerLevel level, BlockPos anker) {
        if (vind(level, anker, nl.juiced.guhs.feature.dromen.DromenFeature.DOOS_KNABBELS.get()) == null) {
            zet(level, anker.offset(DOOS), DromenFeature.DOOS_KNABBELS.get().defaultBlockState());
        }
        if (vind(level, anker, DromenFeature.KATTENHUISJES.get("roze").get()) == null) {
            zet(level, anker.offset(KATTENHUISJE), DromenFeature.KATTENHUISJES.get("roze").get().defaultBlockState());
        }
        Block koek = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(nl.juiced.guhs.Guhs.id("roze_guh_koek"));
        if (koek != null && koek != net.minecraft.world.level.block.Blocks.AIR) {
            for (BlockPos k : KOEKEN) {
                zet(level, anker.offset(k), koek.defaultBlockState());
            }
        }
    }

    /** Places a block only on an empty spot with ground under it (never over the plein's own build). */
    private static void zet(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
    }

    /** The nearest block of this kind within 20 blocks of the plein's arrival spot. */
    @Nullable
    static BlockPos vind(ServerLevel level, BlockPos anker, Block block) {
        for (BlockPos b : BlockPos.betweenClosed(anker.offset(-20, -4, -20), anker.offset(20, 6, 20))) {
            if (level.getBlockState(b).is(block)) {
                return b.immutable();
            }
        }
        return null;
    }

    /** Exactly one happy Snoes on the plein, asleep in (on) his box. */
    static void pleinSnoes(ServerLevel level, BlockPos anker) {
        BlockPos doos = vind(level, anker, DromenFeature.DOOS_KNABBELS.get());
        Vec3 plek = doos != null ? new Vec3(doos.getX() + 0.5, doos.getY() + 0.4, doos.getZ() + 0.5) : Vec3.atBottomCenterOf(anker.offset(DOOS));
        List<GuhNpcEntity> er = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(anker).inflate(48), n -> n.entityTags().contains(TAG_PLEIN));
        if (!er.isEmpty()) {
            for (int i = 1; i < er.size(); i++) {
                er.get(i).discard();
            }
            return;
        }
        GuhNpcEntity s = DroomNpc.spawn(level, plek, 180f, Kind.SNOES_BLIJ);
        s.addTag(TAG_PLEIN);
        s.setInvulnerable(true);
        s.setPersistenceRequired();
    }

    /** The happy Snoes on the plein greets everybody (the story's talk for Kind SNOES_BLIJ). */
    static boolean praatSnoesBlij(ServerPlayer p, GuhNpcEntity snoes) {
        if (DroomVoortgang.finaleKlaar(p)) {
            GuhQuests.say(p, snoes, "gui.guhs.snoes.blij." + p.getRandom().nextInt(4), p.getDisplayName());
        } else {
            GuhQuests.say(p, snoes, "gui.guhs.snoes.blij.vreemde");
        }
        return true;
    }

    // =====================================================================================================================
    // the other dream NPCs: hints in story context
    // =====================================================================================================================

    /** A hint of an island NPC while this player's story is on (else false: their own jokes). */
    static boolean hint(ServerPlayer p, GuhNpcEntity npc, Stukje stukje) {
        if (fase(p) != Fase.DROOMT || p.getRandom().nextInt(3) == 0) {
            return false;                                       // (now and then still a joke)
        }
        String wie = npc.getKind().name().toLowerCase(Locale.ROOT);
        GuhQuests.say(p, npc, "gui.guhs.snoes.hint." + wie + (DroomVoortgang.heeft(p, stukje) ? ".klaar" : ".nog"));
        return true;
    }

    /** The Wekkerguh in story context (one that is not the plein's own wake-up guh, e.g. in a scene). */
    static boolean praatWekker(ServerPlayer p, GuhNpcEntity npc) {
        if (fase(p) != Fase.DROOMT) {
            return false;
        }
        GuhQuests.say(p, npc, "gui.guhs.snoes.hint.wekkerguh", 5 - DroomVoortgang.aantal(p));
        return true;
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long nu = server.overworld().getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Deque<Regel> q = LATER.get(p.getUUID());
            while (q != null && !q.isEmpty() && p.level().getGameTime() >= q.peek().op()) {
                Regel r = q.poll();
                Entity spreker = ((ServerLevel) p.level()).getEntity(r.spreker());
                if (spreker != null) {
                    GuhQuests.say(p, spreker, r.key(), r.args());
                } else {
                    p.sendSystemMessage(Component.translatable(r.key(), r.args()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
        if (nu % 20 != 7) {
            return;
        }
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        if (droom == null || droom.players().isEmpty()) {
            return;
        }
        BlockPos anker = pleinAnker();
        boolean iemandKlaar = false;
        for (ServerPlayer p : droom.players()) {
            iemandKlaar |= DroomVoortgang.finaleKlaar(p);
            if (fase(p) == Fase.DROOMT && DroomVoortgang.alleStukjes(p) && !DroomVoortgang.finaleKlaar(p)
                    && p.blockPosition().distSqr(anker) < FINALE_BEREIK * FINALE_BEREIK && !Cutscenes.bezig(p)) {
                startFinale(p);
            }
            if ((fase(p) == Fase.DROOMT || (fase(p) == Fase.KLAAR && !data(p).getBooleanOr("GidsThuis", false))) && gids(p) == null) {
                roepGids(p);                                  // (Bubbels got lost: he comes back)
            }
        }
        if (iemandKlaar && droom.isLoaded(anker)) {
            pleinSnoes(droom, anker);
        }
    }

    /** (Tests) whether a line is waiting for this player. */
    static boolean wachtRegel(ServerPlayer p) {
        Deque<Regel> q = LATER.get(p.getUUID());
        return q != null && !q.isEmpty();
    }

    /** (Tests) the Snoes at the bed's step counter. */
    static int stap(GuhNpcEntity snoes) {
        return snoes.roleData.getIntOr(STAP, 0);
    }

    /** Used by BlubSnoes-style checks elsewhere: is this player done with the story? */
    public static boolean klaar(ServerPlayer p) {
        return fase(p) == Fase.KLAAR && BlubSnoes.heeft(p);
    }
}
