package nl.juiced.guhs.feature.guhriow3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.LevelWezen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (guhrio-w3): de Grote Nether-Mika, the boss of the Kasteel van de Grote Nether-Mika: a big Mika with horns, a red
 * mane and a green spiked shell on his back. He stands on the bridge of the duel arena and is beaten in three rounds:
 * <ol>
 *     <li>He walks, throws slow coals ({@link KooltjeEntity}) and jumps (his landing is a shockwave that shoves). Run under
 *     him while he is in the air and pull the lever ({@link GuhrioW3Blocks.HendelBlok}) behind him: the far half of the
 *     bridge drops into the sauce. He jumps to the half that is left.</li>
 *     <li>On the shorter bridge he tucks into his shell and rolls up and down ({@link #ROLT}); jump over him. After three
 *     bonks against the wall the ?-block of the arena fills up: a Vuurpeper.</li>
 *     <li>A thrown knabbel bounces the rolling shell back ({@link #knabbel}); the third time he can't brake, rolls off the
 *     broken end and splashes into the sauce. He climbs out on the far side, unharmed and sulking, the bridge comes back,
 *     and everybody in the arena gets the end scene ({@link GuhrioW3Feature#EINDE}) and has won the duel.</li>
 * </ol>
 * He never hurts anybody: a touch is {@link GuhrioSpel#geraakt} (your power-up, or back to your flag) and the shockwave
 * is a {@link Duwtje}. He is shared by everybody in the arena (they fight him together, and all of them win), he is never
 * saved and gone with his level; the next visitor finds him back on a whole bridge. The bridge and the lever are the one
 * thing of Super Guhrio that really changes in the world for a moment (everybody in the arena sees the same fight): he
 * mends them when he appears, when the fight starts again and when he goes.
 * <p>
 * He moves by sums, not by the game's physics: {@link #plek} is where he is along the lane (in the level's own frame) and
 * {@link #hoogte} how far above the bridge deck. The model: tools/features/guhrio_w3_modellen.py, drawn by
 * client.GuhrioW3Client.
 */
public class GroteNetherMikaEntity extends LevelWezen {
    public static final int INTRO = 0, LOOPT = 1, GOOIT = 2, HURKT = 3, SPRINGT = 4, LANDT = 5, SCHRIKT = 6, WACHT = 7, TREKT_IN = 8,
            ROLT = 9, BONKT = 10, REMT = 11, TERUG = 12, WANKELT = 13, VALT = 14, PLONS = 15, KLIMT = 16, MOKT = 17;
    /** His box standing, as a shell and sitting. */
    public static final float BREED = 2.0f, HOOG = 3.0f, SCHILD_BREED = 1.7f, SCHILD_HOOG = 1.5f, ZIT_HOOG = 2.3f;
    /** Ticks of each stand. */
    public static final int INTRO_TICKS = 70, LOOP_TICKS = 34, GOOI_TICKS = 18, HURK_TICKS = 12, SPRING_TICKS = 25, LAND_TICKS = 14,
            SCHRIK_TICKS = 60, KLAAR_TICKS = 40, INTREK_TICKS = 14, BONK_TICKS = 8, REM_TICKS = 34, WANKEL_TICKS = 36, PLONS_TICKS = 46,
            KLIM_TICKS = 50, SCENE_NA = 36;
    /** The jump: he rises 5 blocks (a big player is 2.7 high) and is 25 ticks in the air. */
    public static final double SPRONG = 0.8, ZWAARTE = 0.064, SPRONG_VER = 4.5;
    public static final double LOOP_SNEL = 0.05, ROL_SNEL = 0.24, ROL_SNELLER = 0.05, TERUG_SNEL = 0.42;
    /** How far the shockwave of a landing reaches along the lane, and how hard it shoves. */
    public static final double SCHOK = 6.0, SCHOK_KRACHT = 0.9;
    /** Bonks against the wall before round 3, knabbels that bounce him back before he falls. */
    public static final int BONKEN = 3, TREFFERS = 3;
    /** How deep the sauce lies under the deck. */
    public static final double SAUS = -4.0;

    private static final EntityDataAccessor<Integer> DATA_STAND = SynchedEntityData.defineId(GroteNetherMikaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RONDE = SynchedEntityData.defineId(GroteNetherMikaEntity.class, EntityDataSerializers.INT);
    /** +1: he faces (or rolls) further along the lane, -1: back. */
    private static final EntityDataAccessor<Integer> DATA_KIJK = SynchedEntityData.defineId(GroteNetherMikaEntity.class, EntityDataSerializers.INT);
    /** The way "further along the lane" points (Direction 2D), for the drawing. */
    private static final EntityDataAccessor<Integer> DATA_LANGS = SynchedEntityData.defineId(GroteNetherMikaEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent balk = new ServerBossEvent(Mth.createInsecureUUID(this.random), Component.translatable("entity.guhs.grote_nether_mika"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
    @Nullable
    private DuelPlan plan;
    /** The lane's s of the start block, and the height you stand at. */
    private double s0, y0;
    /** Where he is: along the lane in the level's own frame, and above the bridge deck. */
    private double plek, hoogte;
    private int ticks, bonken, treffers, rolTeken = -1, klaar;
    private boolean gooiBeurt;
    /** A scripted hop (the lever was pulled under him): from where, to where, from how high. */
    private double hopVan, hopNaar, hopHoog;
    private double valX, valY;
    /** The next column of the bridge that drops (counting down to the break) / comes back (counting up), or none. */
    private int breek = Integer.MIN_VALUE, herstel = Integer.MIN_VALUE;
    /** Who has had the end scene of this fight. */
    private final Set<UUID> winnaars = new HashSet<>();

    /** Client: ticks in this stand, the walk and the shell's spin. */
    public int standTicks;
    public float loop, loopO, rol, rolO;
    private int standWas = -1;

    public GroteNetherMikaEntity(EntityType<? extends GroteNetherMikaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAND, INTRO);
        builder.define(DATA_RONDE, 1);
        builder.define(DATA_KIJK, -1);
        builder.define(DATA_LANGS, Direction.EAST.get2DDataValue());
    }

    public int stand() {
        return this.entityData.get(DATA_STAND);
    }

    public int ronde() {
        return this.entityData.get(DATA_RONDE);
    }

    public int kijk() {
        return this.entityData.get(DATA_KIJK);
    }

    public Direction langs() {
        return Direction.from2DDataValue(this.entityData.get(DATA_LANGS));
    }

    /** How often a knabbel bounced him back in round 3. */
    public int treffers() {
        return treffers;
    }

    public int bonken() {
        return bonken;
    }

    /** Where he is along the lane, in the level's own frame (the start block is 0). */
    public double plek() {
        return plek;
    }

    public double hoogte() {
        return hoogte;
    }

    private void zetStand(int stand) {
        this.entityData.set(DATA_STAND, stand);
        ticks = 0;
    }

    private void zetKijk(int teken) {
        this.entityData.set(DATA_KIJK, teken < 0 ? -1 : 1);
    }

    /** Tucked into his shell (the smaller box)? */
    public static boolean schild(int stand) {
        return stand >= TREKT_IN && stand <= VALT;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        int stand = stand();
        return schild(stand) ? EntityDimensions.fixed(SCHILD_BREED, SCHILD_HOOG)
                : stand >= PLONS ? EntityDimensions.fixed(BREED, ZIT_HOOG) : EntityDimensions.fixed(BREED, HOOG);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_STAND.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    /** (a cutscene's actor: {@code Stand} says how he sits there) */
    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        this.entityData.set(DATA_STAND, tag.getIntOr("Stand", stand()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    // =====================================================================================================================
    // the client: only what the drawing needs
    // =====================================================================================================================

    @Override
    protected void clientTick() {
        int stand = stand();
        if (stand != standWas) {
            standWas = stand;
            standTicks = 0;
        }
        standTicks++;
        loopO = loop;
        rolO = rol;
        Vec3 d = this.position().subtract(this.xo, this.yo, this.zo);
        double langs = Math.sqrt(d.x * d.x + d.z * d.z);
        if (stand == LOOPT || stand == KLIMT) {
            loop += (float) langs * 4f;
        }
        if (stand == ROLT || stand == TERUG || stand == VALT) {
            // a wheel of about 0.8 blocks radius: it turns as far as it rolls
            rol += (float) Math.toDegrees(Math.max(langs, 0.1) / 0.8) * kijk();
        } else if (stand == BONKT || stand == WANKELT) {
            rol += (float) Math.sin(standTicks * 0.9) * 6f;
        }
    }

    // =====================================================================================================================
    // the fight
    // =====================================================================================================================

    @Override
    protected void serverTick(ServerLevel level) {
        if (baan == null || actief == null) {
            return;                                           // (a loose one, summoned by hand: it only stands there)
        }
        if (plan == null) {
            begin(level);
        }
        ticks++;
        brug(level);
        if (this.tickCount % 5 == 0) {
            houBij(level);
        }
        switch (stand()) {
            case INTRO -> intro(level);
            case LOOPT -> loopt(level);
            case GOOIT -> {
                kijkNaar(doel());
                if (ticks == GOOI_TICKS / 2) {
                    gooi(level, kijk());
                }
                if (ticks > GOOI_TICKS) {
                    zetStand(LOOPT);
                }
            }
            case HURKT -> {
                if (ticks > HURK_TICKS) {
                    spring(level);
                }
            }
            case SPRINGT -> {
                plek += valX;
                hoogte = SPRONG * ticks - 0.5 * ZWAARTE * ticks * ticks;
                if (ticks >= SPRING_TICKS || hoogte <= 0) {
                    hoogte = 0;
                    plek = Mth.clamp(plek, plan.loopMin(), plan.loopMax());
                    zetStand(LANDT);
                    stamp(level);
                }
            }
            case LANDT -> {
                if (ticks > LAND_TICKS) {
                    zetStand(LOOPT);
                }
            }
            case SCHRIKT -> schrikt(level);
            case WACHT -> wacht(level);
            case TREKT_IN -> {
                if (ticks > INTREK_TICKS) {
                    // (he tucks in at the broken end: the only way to roll is back to the wall)
                    rolTeken = plek > (plan.rolMin() + plan.rolMax()) / 2 ? -1 : 1;
                    zetKijk(rolTeken);
                    zetStand(ROLT);
                }
            }
            case ROLT -> rolt(level);
            case BONKT -> {
                if (ticks > BONK_TICKS) {
                    rolTeken = 1;
                    zetKijk(1);
                    zetStand(ROLT);
                }
            }
            case REMT -> remt(level);
            case TERUG -> {
                plek += TERUG_SNEL;
                if (plek >= plan.rolMax()) {
                    if (treffers >= TREFFERS) {
                        valX = 0.3;
                        valY = 0.22;
                        zetStand(VALT);
                        geluid(level, SoundEvents.RAVAGER_STUNNED, 1.0f, 1.3f);
                    } else {
                        plek = plan.rolMax() + 0.45;
                        zetStand(WANKELT);
                        geluid(level, ModSounds.MIKA_HURT.get(), 1.0f, 0.6f);
                    }
                }
            }
            case WANKELT -> {
                if (ticks > WANKEL_TICKS) {
                    plek = plan.rolMax();
                    zetStand(TREKT_IN);
                }
            }
            case VALT -> {
                plek += valX;
                valY -= ZWAARTE;
                hoogte += valY;
                if (hoogte <= SAUS) {
                    plons(level);
                }
            }
            case PLONS -> {
                if (ticks > PLONS_TICKS) {
                    plek = plan.brugTot() + 0.5 - 0.9;
                    hoogte = SAUS + 0.5;
                    this.setInvisible(false);
                    zetKijk(-1);
                    zetStand(KLIMT);
                }
            }
            case KLIMT -> {
                // up the side of the ledge first, then onto it
                int op = KLIM_TICKS * 3 / 5;
                if (ticks <= op) {
                    hoogte = Mth.lerp(ticks / (double) op, SAUS + 0.5, 0);
                } else {
                    hoogte = 0;
                    plek = Mth.lerp((ticks - op) / (double) (KLIM_TICKS - op), plan.brugTot() + 0.5 - 0.9, plan.mokPlek());
                }
                if (ticks % 4 == 0) {
                    Vec3 p = this.position();
                    level.sendParticles(ParticleTypes.DRIPPING_LAVA, p.x, p.y + 1.2, p.z, 4, 0.7, 0.6, 0.7, 0);
                }
                if (ticks >= KLIM_TICKS) {
                    plek = plan.mokPlek();
                    zetStand(MOKT);
                    herstel = plan.breuk();
                    geluid(level, ModSounds.MIKA_HURT.get(), 1.0f, 0.5f);
                }
            }
            default -> mokt(level);
        }
        plaats();
    }

    /** The first tick in a level: where everything is, a whole bridge, and his opening roar. */
    private void begin(ServerLevel level) {
        plan = DuelPlan.van(level.getServer());
        BlockPos anker = actief.level.anker();
        s0 = baan.plek(anker.getX() + 0.5, anker.getZ() + 0.5).s();
        y0 = anker.getY();
        plek = baan.plek(getX(), getZ()).s() - s0;
        hoogte = 0;
        this.entityData.set(DATA_LANGS, baan.richting(stuk).get2DDataValue());
        maakHeel(level);
        this.entityData.set(DATA_RONDE, 1);
        zetKijk(-1);
        zetStand(INTRO);
    }

    /** Sets him on his spot of this tick. */
    private void plaats() {
        Vec3 p = baan.punt(s0 + plek, y0 + hoogte);
        this.setPos(p.x, p.y, p.z);
        Direction d = baan.richting(stuk);
        int k = kijk();
        this.setYRot((float) Math.toDegrees(Math.atan2(-d.getStepX() * k, d.getStepZ() * k)));
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** A player's place along the lane in the level's own frame. */
    private double eigenX(ServerPlayer p) {
        return baan.plek(p.getX(), p.getZ()).s() - s0;
    }

    /** Is this player really in the fight now (on this lane, not in a pipe, not watching something)? */
    private boolean doetMee(ServerPlayer p) {
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        return s != null && s.actief == actief && s.lane() == baan && !s.inPijp() && !s.klaar() && !Cutscenes.bezig(p);
    }

    /** The nearest player in the fight, or null. */
    @Nullable
    private ServerPlayer doel() {
        ServerPlayer beste = null;
        double af = Double.MAX_VALUE;
        for (ServerPlayer p : spelers()) {
            if (doetMee(p)) {
                double a = Math.abs(eigenX(p) - plek);
                if (a < af) {
                    af = a;
                    beste = p;
                }
            }
        }
        return beste;
    }

    private void kijkNaar(@Nullable ServerPlayer p) {
        if (p != null && Math.abs(eigenX(p) - plek) > 0.5) {
            zetKijk(eigenX(p) < plek ? -1 : 1);
        }
    }

    private void intro(ServerLevel level) {
        if (doel() == null) {
            ticks = Math.min(ticks, 1);                       // (whoever is here still reads the narrator card: he waits)
            return;
        }
        kijkNaar(doel());
        if (ticks == 12) {
            geluid(level, SoundEvents.RAVAGER_ROAR, 1.2f, 0.7f);
            zeg("gui.guhs.guhriow3.intro", ChatFormatting.RED);
        }
        if (ticks > INTRO_TICKS) {
            zeg("gui.guhs.guhriow3.ronde1", ChatFormatting.YELLOW);
            zetStand(LOOPT);
        }
    }

    /** Round 1: towards the nearest player, as far as his part of the bridge goes; then a coal or a jump, in turns. */
    private void loopt(ServerLevel level) {
        ServerPlayer d = doel();
        if (d != null) {
            kijkNaar(d);
            double naar = Mth.clamp(eigenX(d), plan.loopMin(), plan.loopMax());
            if (Math.abs(naar - plek) > 0.4) {
                plek += naar > plek ? LOOP_SNEL : -LOOP_SNEL;
            }
        }
        if (ticks > LOOP_TICKS) {
            gooiBeurt = !gooiBeurt;
            zetStand(gooiBeurt && d != null ? GOOIT : HURKT);
            if (stand() == HURKT) {
                geluid(level, ModSounds.MIKA_AMBIENT.get(), 1.0f, 0.5f);
            }
        }
    }

    /** Off the ground: straight up, or a few blocks towards the nearest player (never off his part of the bridge). */
    private void spring(ServerLevel level) {
        ServerPlayer d = doel();
        double naar = plek;
        if (d != null && this.random.nextInt(3) != 0) {
            naar = plek + Mth.clamp(eigenX(d) - plek, -SPRONG_VER, SPRONG_VER);
        }
        naar = Mth.clamp(naar, plan.loopMin(), plan.loopMax());
        valX = (naar - plek) / SPRING_TICKS;
        zetStand(SPRINGT);
        geluid(level, SoundEvents.RAVAGER_STEP, 1.0f, 0.6f);
    }

    /** He lands: the deck shakes, and whoever stands on the ground nearby is shoved away from him. */
    private void stamp(ServerLevel level) {
        Vec3 hier = baan.punt(s0 + plek, y0);
        geluid(level, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 0.6f);
        geluid(level, SoundEvents.ANVIL_LAND, 0.5f, 0.5f);
        Direction d = baan.richting(stuk);
        for (int k = -5; k <= 5; k++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, hier.x + d.getStepX() * k, hier.y + 0.1, hier.z + d.getStepZ() * k, 2, 0.2, 0.05, 0.2, 0.01);
        }
        level.sendParticles(ParticleTypes.POOF, hier.x, hier.y + 0.1, hier.z, 16, 1.0, 0.1, 1.0, 0.04);
        for (ServerPlayer p : spelers()) {
            if (!doetMee(p) || !p.onGround() || !Duwtje.mag(p)) {
                continue;
            }
            double af = eigenX(p) - plek;
            if (Math.abs(af) <= SCHOK && Math.abs(p.getY() - y0) < 1.6) {
                int weg = af < 0 ? -1 : 1;
                Duwtje.duw(p, new Vec3(d.getStepX() * weg, 0, d.getStepZ() * weg), SCHOK_KRACHT);
            }
        }
    }

    /** Throws a slow coal along the lane. */
    private void gooi(ServerLevel level, int teken) {
        int n = 0;
        for (Entity e : actief.los) {
            if (e instanceof KooltjeEntity && !e.isRemoved()) {
                n++;
            }
        }
        if (n >= KooltjeEntity.TEGELIJK) {
            return;
        }
        KooltjeEntity kool = GuhrioW3Feature.KOOLTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (kool == null) {
            return;
        }
        Vec3 uit = baan.punt(s0 + plek + teken * 1.3, y0 + hoogte + 1.0);
        kool.snapTo(uit.x, uit.y, uit.z, 0f, 0f);
        kool.zetBaan(actief, baan, null);
        kool.gooi(teken, y0 + KooltjeEntity.VLIEGHOOGTE);
        level.addFreshEntity(kool);
        actief.los.add(kool);
        geluid(level, SoundEvents.BLAZE_SHOOT, 0.7f, 0.7f);
    }

    /**
     * The lever is pulled (round 1 only): the far half of the bridge drops, column by column, and he hops to the half that
     * stays. True when it did something.
     */
    public boolean hendel(ServerPlayer door) {
        if (plan == null || ronde() != 1 || stand() >= SCHRIKT || !(level() instanceof ServerLevel level)) {
            return false;
        }
        this.entityData.set(DATA_RONDE, 2);
        breek = plan.brugTot();
        herstel = Integer.MIN_VALUE;
        zetHendel(level, true);
        hopVan = plek;
        hopHoog = hoogte;
        hopNaar = Math.min(plek, plan.rolMax() - 1.2);
        zetStand(SCHRIKT);
        zetKijk(1);
        geluid(level, SoundEvents.LEVER_CLICK, 1.0f, 0.6f);
        geluid(level, SoundEvents.CHAIN_BREAK, 1.0f, 0.6f);
        geluid(level, ModSounds.MIKA_HURT.get(), 1.2f, 0.5f);
        zeg("gui.guhs.guhriow3.hendel", ChatFormatting.GOLD);
        for (ServerPlayer p : spelers()) {
            GuhAdvancements.grant(p, "guhrio_w3_hendel");
        }
        return true;
    }

    private void schrikt(ServerLevel level) {
        int hop = 26;
        if (ticks <= hop) {
            double t = ticks / (double) hop;
            plek = Mth.lerp(t, hopVan, hopNaar);
            hoogte = hopHoog * (1 - t) + (Math.abs(hopNaar - hopVan) > 0.1 || hopHoog > 0.1 ? 3.2 : 0.8) * Math.sin(Math.PI * t);
        } else {
            hoogte = 0;
        }
        if (ticks > SCHRIK_TICKS) {
            klaar = 0;
            zetStand(WACHT);
        }
    }

    /** Is somebody on his half of the arena (not on the far ledge)? */
    private boolean iemandHier() {
        for (ServerPlayer p : spelers()) {
            if (doetMee(p) && eigenX(p) < plan.breuk() - 0.4) {
                return true;
            }
        }
        return false;
    }

    /** Round 2 and 3, between rolls: he waits at the broken end until somebody is on his half; a coal now and then. */
    private void wacht(ServerLevel level) {
        ServerPlayer d = doel();
        kijkNaar(d);
        if (d != null && ticks % 90 == 45) {
            gooi(level, kijk());
        }
        if (ticks % 120 == 20) {
            for (ServerPlayer p : spelers()) {
                if (doetMee(p) && eigenX(p) > plan.breuk()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.guhriow3.pijp").withStyle(ChatFormatting.GREEN));
                }
            }
        }
        klaar = iemandHier() ? klaar + 1 : 0;
        if (klaar > KLAAR_TICKS) {
            if (ronde() == 2 && bonken == 0) {
                zeg("gui.guhs.guhriow3.ronde2", ChatFormatting.YELLOW);
            }
            zetStand(TREKT_IN);
            geluid(level, SoundEvents.ARMOR_EQUIP_TURTLE.value(), 1.0f, 0.6f);
        }
    }

    private double rolSnel() {
        return ROL_SNEL + (ronde() == 3 ? 0.03 + ROL_SNELLER * treffers : 0);
    }

    private void rolt(ServerLevel level) {
        plek += rolTeken * rolSnel();
        if (ticks % 3 == 0) {
            Vec3 p = this.position();
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.1, p.z, 2, 0.3, 0.05, 0.3, 0.02);
        }
        if (rolTeken < 0 && plek <= plan.rolMin()) {
            plek = plan.rolMin();
            bonken++;
            zetStand(BONKT);
            geluid(level, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 0.6f);
            Vec3 p = this.position();
            level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.6, p.z, 8, 0.3, 0.4, 0.3, 0.03);
        } else if (rolTeken > 0 && plek >= plan.rolMax()) {
            plek = plan.rolMax();
            zetStand(REMT);
        }
    }

    /** He brakes at the broken end and peeks out of his shell: a coal, and then again. After three bonks round 3 starts. */
    private void remt(ServerLevel level) {
        if (ticks == 1) {
            zetKijk(-1);
            if (ronde() == 2 && bonken >= BONKEN) {
                this.entityData.set(DATA_RONDE, 3);
                zeg("gui.guhs.guhriow3.ronde3", ChatFormatting.YELLOW);
                geluid(level, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.8f);
                houBij(level);
            }
        }
        if (ticks == REM_TICKS / 2 && iemandHier()) {
            gooi(level, -1);
        }
        if (ticks > REM_TICKS) {
            if (iemandHier()) {
                zetStand(TREKT_IN);
            } else {
                klaar = 0;
                zetStand(WACHT);
            }
        }
    }

    private void plons(ServerLevel level) {
        Vec3 p = baan.punt(s0 + plek, y0 + SAUS + 0.2);
        level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 40, 0.9, 0.2, 0.9, 0.2);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.6, p.z, 24, 0.8, 0.6, 0.8, 0.03);
        level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.4, p.z, 60, 0.9, 0.4, 0.9, 0.3);
        geluid(level, SoundEvents.BUCKET_EMPTY_LAVA, 1.4f, 0.6f);
        geluid(level, SoundEvents.GENERIC_SPLASH, 1.2f, 0.6f);
        this.setInvisible(true);
        balk.removeAllPlayers();
        zetStand(PLONS);
        zeg("gui.guhs.guhriow3.plons", ChatFormatting.GOLD);
    }

    /**
     * He sits on the far ledge, sulking. The bridge comes back; then everybody in the arena gets the end scene and has won.
     * Somebody who walks in later finds him ready for a new fight.
     */
    private void mokt(ServerLevel level) {
        if (ticks % 30 == 15) {
            Vec3 p = this.position();
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + ZIT_HOOG + 0.3, p.z, 3, 0.2, 0.1, 0.2, 0.01);
        }
        if (ticks == SCENE_NA) {
            for (ServerPlayer p : spelers()) {
                if (winnaars.add(p.getUUID())) {
                    einde(p);
                }
            }
        }
        if (ticks > SCENE_NA + 60 && ticks % 20 == 0) {
            for (ServerPlayer p : spelers()) {
                if (doetMee(p) && !winnaars.contains(p.getUUID())) {
                    opnieuw(level);
                    return;
                }
            }
        }
    }

    /** The end scene for this player; afterwards (or at once, when the scene can't play) the duel is won. */
    private void einde(ServerPlayer p) {
        Direction kant = actief.level.kant();
        Rotation draai = Rotation.NONE;
        for (Rotation r : Rotation.values()) {
            if (r.rotate(Direction.EAST) == kant) {
                draai = r;
            }
        }
        if (!Cutscenes.speel(p, GuhrioW3Feature.EINDE, actief.level.anker(), draai, GroteNetherMikaEntity::gewonnen)) {
            gewonnen(p);
        }
    }

    /** The duel is won for this player: the questline, the advancements, a line with the time, and out to the tower room. */
    public static void gewonnen(ServerPlayer p) {
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        GuhAdvancements.grant(p, "guhrio_w3_taart");
        GidsFeature.grant(p, "guhrio/guhrio_w3_taart");
        GuhrioKasteel.winDuel(p);
        if (s != null) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhriow3.gewonnen", GuhrioSpel.tijd(s.ticks)).withStyle(ChatFormatting.GOLD));
            GuhrioSpel.stop(p, GuhrioSpel.Einde.KLAAR);
        }
    }

    /** A new fight: a whole bridge, round 1, back on his spot. */
    public void opnieuw(ServerLevel level) {
        if (plan == null) {
            return;
        }
        maakHeel(level);
        winnaars.clear();
        bonken = 0;
        treffers = 0;
        klaar = 0;
        gooiBeurt = false;
        this.setInvisible(false);
        this.entityData.set(DATA_RONDE, 1);
        plek = thuis == null ? (plan.loopMin() + plan.loopMax()) / 2 : baan.plek(thuis.getX() + 0.5, thuis.getZ() + 0.5).s() - s0;
        hoogte = 0;
        zetKijk(-1);
        zetStand(INTRO);
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.8, 1.0, 0.8, 0.04);
        zeg("gui.guhs.guhriow3.opnieuw", ChatFormatting.RED);
    }

    // =====================================================================================================================
    // the bridge, the lever, the ?-block, the bar
    // =====================================================================================================================

    private BlockPos wereld(int x, int y) {
        return actief.level.wereld(new BlockPos(x, y, 0));
    }

    @Nullable
    private BlockState brugBlok() {
        Block blok = BuiltInRegistries.BLOCK.getValue(Identifier.parse(plan.blok()));
        return blok == Blocks.AIR ? null : blok.defaultBlockState();
    }

    /** One column of the bridge drops / comes back every other tick. */
    private void brug(ServerLevel level) {
        if (this.tickCount % 2 != 0) {
            return;
        }
        if (breek != Integer.MIN_VALUE) {
            BlockPos pos = wereld(breek, -1);
            BlockState nu = level.getBlockState(pos);
            if (!nu.isAir()) {
                level.levelEvent(2001, pos, Block.getId(nu));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            if (--breek < plan.breuk()) {
                breek = Integer.MIN_VALUE;
            }
        } else if (herstel != Integer.MIN_VALUE) {
            BlockState blok = brugBlok();
            BlockPos pos = wereld(herstel, -1);
            if (blok != null && !level.getBlockState(pos).is(blok.getBlock())) {
                level.setBlock(pos, blok, 3);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 0.8f + 0.03f * (herstel - plan.breuk()));
                level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.02);
            }
            if (++herstel > plan.brugTot()) {
                herstel = Integer.MIN_VALUE;
            }
        }
    }

    /** The whole bridge is there and the lever stands up (at once: before a fight, and when he goes). */
    private void maakHeel(ServerLevel level) {
        breek = Integer.MIN_VALUE;
        herstel = Integer.MIN_VALUE;
        BlockState blok = brugBlok();
        if (blok != null) {
            for (int x = plan.brugVan(); x <= plan.brugTot(); x++) {
                BlockPos pos = wereld(x, -1);
                if (level.isLoaded(pos) && !level.getBlockState(pos).is(blok.getBlock())) {
                    level.setBlock(pos, blok, 3);
                }
            }
        }
        zetHendel(level, false);
    }

    private void zetHendel(ServerLevel level, boolean getrokken) {
        BlockPos pos = actief.level.wereld(plan.hendel());
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GuhrioW3Blocks.HendelBlok && state.getValue(GuhrioW3Blocks.HendelBlok.GETROKKEN) != getrokken) {
            level.setBlock(pos, state.setValue(GuhrioW3Blocks.HendelBlok.GETROKKEN, getrokken), 3);
        }
    }

    /**
     * Every few ticks: the bar of everybody in the arena, the narrator card for whoever is here for the first time, and the
     * ?-block: empty for everybody until round 3, and from then on full again for whoever has no Vuurpeper (so losing it
     * never ends the fight).
     */
    private void houBij(ServerLevel level) {
        int stand = stand();
        if (stand < PLONS) {
            for (ServerPlayer p : spelers()) {
                balk.addPlayer(p);
            }
            for (ServerPlayer p : java.util.List.copyOf(balk.getPlayers())) {
                GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                if (s == null || s.actief != actief) {
                    balk.removePlayer(p);
                }
            }
            balk.setProgress(ronde() == 1 ? 1f : ronde() == 2 ? 2 / 3f : (TREFFERS - Math.min(TREFFERS, treffers)) / (3f * TREFFERS));
        }
        BlockPos vraag = actief.level.wereld(plan.vraag());
        boolean vol = ronde() == 3 && stand < PLONS;
        for (ServerPlayer p : spelers()) {
            GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
            if (s == null || s.actief != actief) {
                continue;
            }
            GuhrioW3Feature.kaart(p);
            if (!vol && s.staat(vraag) == 0) {
                GuhrioSpel.zetStaat(p, s, vraag, 1);
            } else if (vol && s.staat(vraag) != 0 && s.kracht != GuhrioSpel.Kracht.VUUR) {
                GuhrioSpel.zetStaat(p, s, vraag, 0);
                level.sendParticles(p, ParticleTypes.FLAME, false, false, vraag.getX() + 0.5, vraag.getY() + 0.5, vraag.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
            }
        }
    }

    private void geluid(ServerLevel level, SoundEvent geluid, float volume, float toon) {
        level.playSound(null, getX(), getY(), getZ(), geluid, SoundSource.HOSTILE, volume, toon);
    }

    /** A line above the hotbar for everybody in the arena. */
    private void zeg(String key, ChatFormatting kleur, Object... args) {
        for (ServerPlayer p : spelers()) {
            p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            balk.removeAllPlayers();
            if (plan != null && actief != null && reason.shouldDestroy() && this.level() instanceof ServerLevel level) {
                maakHeel(level);
            }
        }
        super.remove(reason);
    }

    // =====================================================================================================================
    // GuhrioWezen
    // =====================================================================================================================

    @Override
    public boolean stampbaar() {
        return false;                                         // (horns, a mane, a shell full of spikes: nobody lands on him)
    }

    @Override
    public boolean gevaarlijk() {
        int stand = stand();
        return stand < PLONS && stand != SCHRIKT && stand != VALT && !this.isInvisible();
    }

    @Override
    public void stamp(ServerPlayer player, GuhrioSpel.Sessie sessie) {
        raakt(player, sessie);
    }

    /**
     * A knabbel hit him. Only in round 3, while his shell rolls at the players: it bounces back to the broken end. The third
     * time he rolls off it.
     */
    @Override
    public boolean knabbel(ServerPlayer gooier, GuhrioSpel.Sessie sessie) {
        if (!(level() instanceof ServerLevel level) || plan == null) {
            return false;
        }
        if (ronde() != 3 || stand() != ROLT || rolTeken > 0) {
            geluid(level, SoundEvents.SHIELD_BLOCK.value(), 0.6f, 1.6f);
            return false;
        }
        treffers++;
        rolTeken = 1;
        zetKijk(1);
        zetStand(TERUG);
        geluid(level, SoundEvents.SHIELD_BLOCK.value(), 1.0f, 0.8f);
        geluid(level, ModSounds.MIKA_HURT.get(), 1.0f, 0.8f);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.8, getZ(), 16, 0.5, 0.5, 0.5, 0.3);
        for (ServerPlayer p : spelers()) {
            GuhAdvancements.grant(p, "guhrio_w3_schild");
        }
        if (treffers < TREFFERS) {
            zeg("gui.guhs.guhriow3.treffer", ChatFormatting.GREEN, TREFFERS - treffers);
        }
        houBij(level);
        return true;
    }

    @Override
    public boolean schild(@Nullable ServerPlayer schopper) {
        return false;                                         // (somebody else's shell bounces off him)
    }

    // --- for the game tests and the dev command -----------------------------------------------------------------------

    /** (tests, dev) Jumps to this round: 1 a new fight, 2 the lever is pulled, 3 the ?-block is full, 4 he falls. */
    public void devRonde(ServerLevel level, int ronde) {
        if (plan == null) {
            if (baan == null || actief == null) {
                return;
            }
            begin(level);
        }
        if (ronde <= 1) {
            opnieuw(level);
            return;
        }
        if (ronde() == 1 && stand() < SCHRIKT) {
            ServerPlayer wie = spelers().isEmpty() ? null : spelers().get(0);
            if (wie != null) {
                zetStand(LOOPT);
                hendel(wie);
            }
        }
        if (ronde >= 3 && ronde() < 3) {
            bonken = BONKEN;
            breek = Integer.MIN_VALUE;
            for (int x = plan.breuk(); x <= plan.brugTot(); x++) {
                level.setBlock(wereld(x, -1), Blocks.AIR.defaultBlockState(), 3);
            }
            plek = plan.rolMax();
            hoogte = 0;
            zetStand(REMT);
        }
        if (ronde >= 4) {
            this.entityData.set(DATA_RONDE, 3);
            treffers = TREFFERS;
            rolTeken = 1;
            zetStand(TERUG);
        }
    }
}
