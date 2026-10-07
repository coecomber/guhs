package nl.juiced.guhs.feature.guhrio.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.BaanSprong;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStuk;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;
import nl.juiced.guhs.feature.guhrio.GuhrioWezen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Your keys and your body in a level (the player's own game moves the player, so this is where the lane really holds you).
 * <ul>
 *     <li>The keys ({@link BaanInput}): A and D walk left and right on the screen, which is back and forth along the lane;
 *     space jumps (hold it for a high jump, tap for a hop), S or sneak ducks (and goes down a pipe), W is for doors, sprint
 *     runs, a mouse button throws a knabbel (Vuurpeper) or shoots Guhshi's tongue, Q twice leaves the level. Under it is
 *     the normal walking of the game: you are simply turned to face along the lane and "walk forward".</li>
 *     <li>The line: before and after every step you are put on the lane's line ({@link Baan#stap}), your sideways speed is
 *     gone, a corner turns your speed with you, the two ends stop you.</li>
 *     <li>What your game sees happen, the moment it happens, and tells the server ({@link GuhrioPayloads.Actie}): your
 *     head under a block (a hidden block appears right before it), a coin, a creature you land on (you bounce at once) or
 *     that touches you, ducking on a pipe or walking into a sideways one, standing on a switch.</li>
 *     <li>What carries you: a moving platform takes you along; on Guhshi a held jump flutters.</li>
 *     <li>The little films: into a pipe and out of the other (in any direction), sliding down the flagpole.</li>
 * </ul>
 * A cutscene always wins: while {@code Cutscenes.bezig} nothing here touches you. The feel of the game is the numbers of
 * {@link BaanSprong} (the jump itself: the sums there are what this class does every tick, and what the generators walk
 * every level with) together with GuhrioSpel's jump strength, gravity and speed, and the few numbers at the top here.
 */
public final class BaanBesturing {
    /** (the jump's numbers live in {@link BaanSprong}; these names stay for whoever reads them here) */
    public static final double LUCHT_STUUR = BaanSprong.LUCHT_STUUR, LUCHT_LOOP = BaanSprong.LUCHT_LOOP, LUCHT_REN = BaanSprong.LUCHT_REN;
    public static final double STIJG_LICHTER = BaanSprong.STIJG_LICHTER, HOP_REST = BaanSprong.HOP_REST, VAL_ERBIJ = BaanSprong.VAL_ERBIJ,
            VAL_MAX = BaanSprong.VAL_MAX;
    /** The bounce off a creature: normal, and with space held. */
    public static final double STUITER = 0.48, STUITER_HOOG = 0.70;
    /** Sliding down the flagpole (blocks per tick). */
    public static final double GLIJ = 0.14;
    public static final int FLADDER_TICKS = BaanSprong.FLADDER_TICKS;
    public static final double FLADDER = BaanSprong.FLADDER;
    /** Ticks between two throws / licks, and how long "press Q again" waits. */
    public static final int ACTIE_RUST = 6, STOP_WACHT = 40;

    /** The piece of the lane you are on, the way you face along it (+1 further, -1 back). */
    private static int stuk;
    private static int kijk = 1;
    private static float yaw;
    /** Your jump from tick to tick (rising from a jump of your own, Guhshi's flutter). */
    private static final BaanSprong.Staat SPRONG = new BaanSprong.Staat();
    private static double valVoor;
    private static boolean wasW, cutscene;
    /** 0 not in a pipe; 1 going in; 2 waiting inside for the server; 3 coming out. */
    private static int pijpFase;
    private static int pijpTick;
    private static Vec3 pijpVan = Vec3.ZERO, pijpNaar = Vec3.ZERO;
    private static boolean pijpStaat;
    @Nullable
    private static BlockPos mast;
    private static int rustStamp, rustDuik, rustActie, stopWacht;
    private static final Map<Integer, Integer> RUST_WEZENS = new HashMap<>();
    private static final Map<BlockPos, Integer> RUST_STAP = new HashMap<>();
    /** What carries you (a platform) and where it was last tick. */
    @Nullable
    private static Entity drager;
    private static Vec3 dragerWas = Vec3.ZERO;

    private BaanBesturing() {
    }

    /** Your keys in a level. */
    static final class BaanInput extends ClientInput {
        @Override
        public void tick() {
            Options o = Minecraft.getInstance().options;
            if (stil()) {
                this.keyPresses = Input.EMPTY;
                this.moveVector = Vec2.ZERO;
                return;
            }
            boolean loop = teken(o) != 0;
            this.keyPresses = new Input(loop, false, false, false, o.keyJump.isDown(), o.keyDown.isDown() || o.keyShift.isDown(), o.keySprint.isDown());
            this.moveVector = loop ? new Vec2(0f, 1f) : Vec2.ZERO;
        }
    }

    /** -1 left on the screen, +1 right, 0 neither (or both). */
    private static int teken(Options o) {
        if (Minecraft.getInstance().screen != null) {
            return 0;
        }
        return (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
    }

    /** No keys now: in a pipe, at the flagpole, watching a cutscene. */
    private static boolean stil() {
        return pijpFase != 0 || mast != null || cutscene;
    }

    static boolean inPijp() {
        return pijpFase != 0;
    }

    /** The piece of the lane you are on. */
    static int stukNu() {
        return stuk;
    }

    /** The way you face along the lane (+1 further, -1 back). */
    static int kijk() {
        return kijk;
    }

    // =====================================================================================================================

    /** A level (or another lane of it) starts for you here. */
    static void begin(@Nullable LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        if (p == null || baan == null) {
            return;
        }
        stuk = baan.plek(p.getX(), p.getZ()).stuk();
        kijk = 1;
        SPRONG.sprong = false;
        mast = null;
        drager = null;
        yaw = yawVan(baan);
        if (!(p.input instanceof BaanInput)) {
            p.input = new BaanInput();
        }
    }

    static void einde() {
        pijpFase = 0;
        mast = null;
        SPRONG.sprong = false;
        cutscene = false;
        drager = null;
        stopWacht = 0;
        RUST_WEZENS.clear();
        RUST_STAP.clear();
    }

    /** You were put back at your flag. */
    static void terug(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        pijpFase = 0;
        SPRONG.sprong = false;
        drager = null;
        if (baan != null) {
            stuk = baan.plek(p.getX(), p.getZ()).stuk();
        }
    }

    private static float yawVan(Baan baan) {
        Direction d = baan.richting(stuk);
        return (float) Math.toDegrees(Math.atan2(-d.getStepX() * kijk, d.getStepZ() * kijk));
    }

    /** Every frame: you look along the lane, whatever the mouse does. */
    static void kijkVast() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !GuhrioClient.speelt() || Cutscenes.bezig(p)) {
            return;
        }
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setXRot(0f);
        p.xRotO = 0f;
        p.setYHeadRot(yaw);
        p.yHeadRotO = yaw;
    }

    // =====================================================================================================================
    // every tick, before you move
    // =====================================================================================================================

    static void voor(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
            if (p.input instanceof BaanInput) {
                GuhrioClient.uit();
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (Cutscenes.bezig(p)) {
            cutscene = true;                                   // (the scene has the camera and the keys; the level waits)
            drager = null;
            return;
        }
        if (cutscene) {
            cutscene = false;
            stuk = baan.plek(p.getX(), p.getZ()).stuk();
            BaanCamera.begin(p);
        }
        if (!(p.input instanceof BaanInput)) {
            p.input = new BaanInput();
        }
        if (p.getAbilities().flying) {
            p.getAbilities().flying = false;
        }
        RUST_WEZENS.replaceAll((id, t) -> t - 1);
        RUST_WEZENS.values().removeIf(t -> t <= 0);
        RUST_STAP.replaceAll((pos, t) -> t - 1);
        RUST_STAP.values().removeIf(t -> t <= 0);
        if (rustStamp > 0) {
            rustStamp--;
        }
        if (rustDuik > 0) {
            rustDuik--;
        }
        if (rustActie > 0) {
            rustActie--;
        }
        if (stopWacht > 0) {
            stopWacht--;
        }
        valVoor = p.getDeltaMovement().y;
        if (stil()) {
            return;
        }
        draag(p);
        int scherm = teken(mc.options);
        int teken = scherm * baan.schermRechts();
        if (teken != 0) {
            kijk = teken;
        }
        Baan.Stap op = baan.stap(stuk, p.getX(), p.getZ());
        int nieuw = baan.kies(op.stuk(), op.s(), teken);
        Vec3 v = p.getDeltaMovement();
        Direction oud = baan.richting(stuk), d = baan.richting(nieuw);
        double langs = v.x * oud.getStepX() + v.z * oud.getStepZ();
        stuk = nieuw;
        boolean lucht = !p.onGround() && !p.isInWater();
        // in the air: steer
        if (lucht) {
            langs = BaanSprong.stuur(langs, teken, p.isSprinting());
        }
        // the jump: a held one rises longer, letting go cuts it, falling is heavier, Guhshi flutters
        boolean spatie = mc.options.keyJump.isDown() && mc.screen == null;
        double vy = SPRONG.val(v.y, p.onGround(), lucht, spatie, GuhrioClient.guhshi != 0);
        if (SPRONG.fladderde && SPRONG.fladder % 4 == 1) {
            p.playSound(SoundEvents.PARROT_FLY, 0.5f, 1.5f);
        }
        // a hidden block right above your head on the way up: it is there now (your head will find it this very step)
        if (vy > 0 && lucht) {
            onzichtbaar(p, p.level(), d, vy);
        }
        p.setDeltaMovement(d.getStepX() * langs, vy, d.getStepZ() * langs);
        if (Math.abs(op.x() - p.getX()) > 1e-6 || Math.abs(op.z() - p.getZ()) > 1e-6) {
            p.setPos(op.x(), p.getY(), op.z());
        }
        yaw = yawVan(baan);
        p.setYRot(yaw);
        p.setXRot(0f);
    }

    /** What carries you (a platform) moved: you move with it. */
    private static void draag(LocalPlayer p) {
        if (drager == null) {
            return;
        }
        if (!drager.isAlive() || !(drager instanceof GuhrioWezen w) || !w.draagt()) {
            drager = null;
            return;
        }
        Vec3 nu = drager.position();
        Vec3 d = nu.subtract(dragerWas);
        dragerWas = nu;
        AABB vak = drager.getBoundingBox();
        AABB ik = p.getBoundingBox();
        boolean erop = p.getDeltaMovement().y <= 0.05 && Math.abs(ik.minY - (vak.maxY - d.y)) < 0.4
                && ik.maxX > vak.minX - 0.3 && ik.minX < vak.maxX + 0.3 && ik.maxZ > vak.minZ - 0.3 && ik.minZ < vak.maxZ + 0.3;
        if (!erop || d.lengthSqr() > 4) {
            drager = null;
            return;
        }
        p.setPos(p.getX() + d.x, vak.maxY, p.getZ() + d.z);
        p.setOnGround(true);
        p.resetFallDistance();
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, Math.max(v.y, 0), v.z);
    }

    /** Rising under a hidden block you have not found yet: your own game makes it solid for you now. */
    private static void onzichtbaar(LocalPlayer p, Level level, Direction d, double vy) {
        AABB box = p.getBoundingBox();
        for (double uit : new double[]{0, 0.29, -0.29}) {
            // (the cell your head is about to enter: a rise is less than a block a tick, so its top now and after the step)
            for (double y : new double[]{box.maxY + 0.01, box.maxY + vy + 0.05}) {
                BlockPos pos = BlockPos.containing(p.getX() + d.getStepX() * uit, y, p.getZ() + d.getStepZ() * uit);
                if (level.getBlockState(pos).getBlock() instanceof GuhrioStukken.OnzichtbaarBlok && GuhrioClient.staat(pos) == 0
                        && box.maxY <= pos.getY() + 1e-3) {
                    GuhrioClient.raad(pos.immutable(), 1);
                    return;
                }
            }
        }
    }

    // =====================================================================================================================
    // every tick, after you moved (before your game tells the server where you are)
    // =====================================================================================================================

    static void na(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        if (baan == null || Cutscenes.bezig(p)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (pijpFase != 0) {
            pijp(p);
            return;
        }
        if (mast != null) {
            // down the pole, then stand still until the server lets you go
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(0, p.onGround() ? 0 : Math.max(v.y, -GLIJ), 0);
            p.setPos(mast.getX() + 0.5, p.getY(), mast.getZ() + 0.5);
            return;
        }
        Baan.Stap op = baan.stap(stuk, p.getX(), p.getZ());
        Vec3 v = p.getDeltaMovement();
        if (op.stuk() != stuk || op.eind()) {
            Direction oud = baan.richting(stuk), d = baan.richting(op.stuk());
            double langs = op.eind() ? 0 : v.x * oud.getStepX() + v.z * oud.getStepZ();
            p.setDeltaMovement(d.getStepX() * langs, v.y, d.getStepZ() * langs);
            stuk = op.stuk();
            yaw = yawVan(baan);
        }
        if (Math.abs(op.x() - p.getX()) > 1e-6 || Math.abs(op.z() - p.getZ()) > 1e-6) {
            p.setPos(op.x(), p.getY(), op.z());
        }
        boolean spatie = mc.options.keyJump.isDown() && mc.screen == null;
        if (SPRONG.na(p.onGround(), valVoor, p.getDeltaMovement().y, spatie)) {
            drager = null;                                    // (left the ground by a jump of your own this tick)
        }
        Level level = p.level();
        kop(p, level);
        stukken(p, level);
        wezens(p, level, spatie);
        toetsen(p, level, mc.options, baan);
    }

    /** Your head hit something on the way up: the block above you hops, and the server hears of it. */
    private static void kop(LocalPlayer p, Level level) {
        if (!(p.verticalCollision && !p.verticalCollisionBelow) || p.onGround()) {
            return;                                           // (only a bump on the way up)
        }
        Baan baan = GuhrioClient.baan();
        Direction d = baan.richting(stuk);
        double y = p.getBoundingBox().maxY + 0.2;
        // the block over the middle of you, else the one over your front or back edge
        for (double uit : new double[]{0, 0.29, -0.29}) {
            BlockPos pos = BlockPos.containing(p.getX() + d.getStepX() * uit, y, p.getZ() + d.getStepZ() * uit);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GuhrioStuk && !state.getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.of(p)).isEmpty()) {
                if (level.getBlockEntity(pos) instanceof GuhrioBlocks.StukBlockEntity be) {
                    be.bots = level.getGameTime();
                }
                if (state.getBlock() instanceof GuhrioBlocks.VraagBlok) {
                    if (!GuhrioClient.raad(pos, 1)) {
                        p.playSound(SoundEvents.STONE_HIT, 0.6f, 0.7f);
                    }
                } else if (state.getBlock() instanceof GuhrioBlocks.SteenBlok && GuhrioClient.kracht != GuhrioSpel.Kracht.GEEN.ordinal()) {
                    GuhrioClient.raad(pos, 1);
                } else {
                    p.playSound(SoundEvents.STONE_HIT, 0.6f, 0.9f);
                }
                stuur(GuhrioPayloads.Actie.BOTS, pos, 0);
                SPRONG.sprong = false;
                return;
            }
        }
    }

    /** The pieces your body is in: a coin is yours at once. */
    private static void stukken(LocalPlayer p, Level level) {
        AABB box = p.getBoundingBox();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if ((state.getBlock() instanceof GuhrioBlocks.MuntBlok || state.getBlock() instanceof GuhrioStukken.VadsmuntBlok)
                            && GuhrioClient.staat(pos) == 0 && box.intersects(x + 0.2, y + 0.1, z + 0.2, x + 0.8, y + 0.9, z + 0.8)
                            && GuhrioClient.raad(pos, 1)) {
                        stuur(GuhrioPayloads.Actie.RAAK, pos.immutable(), 0);
                    }
                }
            }
        }
    }

    /** The creatures you touch: on top of one you bounce, a platform carries you, anything else is for the server to judge. */
    private static void wezens(LocalPlayer p, Level level, boolean spatie) {
        AABB box = p.getBoundingBox();
        for (Entity e : level.getEntities(p, box.inflate(0.05, 0.2, 0.05), x -> x instanceof GuhrioWezen && x.isAlive() && !x.isInvisible())) {
            GuhrioWezen w = (GuhrioWezen) e;
            if (w.draagt()) {
                AABB vak = e.getBoundingBox();
                if (drager != e && p.getDeltaMovement().y <= 0.05 && Math.abs(box.minY - vak.maxY) < 0.15) {
                    drager = e;
                    dragerWas = e.position();
                }
                continue;
            }
            if (RUST_WEZENS.containsKey(e.getId()) || !w.raaktVak(box.inflate(0.05))) {
                continue;
            }
            boolean vanBoven = valVoor < -0.02 && p.yo >= e.getY() + e.getBbHeight() * 0.5;
            if (w.stampbaar() && vanBoven && rustStamp == 0) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, spatie ? STUITER_HOOG : STUITER, v.z);
                p.resetFallDistance();
                SPRONG.sprong = spatie;
                SPRONG.fladder = 0;
                rustStamp = 3;
                RUST_WEZENS.put(e.getId(), 12);
                p.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8f, 1.2f);
                stuur(GuhrioPayloads.Actie.STAMP, p.blockPosition(), e.getId());
            } else if (w.aanraakbaar()) {
                RUST_WEZENS.put(e.getId(), 12);
                stuur(GuhrioPayloads.Actie.GERAAKT, p.blockPosition(), e.getId());
            }
        }
    }

    /** S on a pipe, walking into a sideways pipe, W at a door, standing on a switch. */
    private static void toetsen(LocalPlayer p, Level level, Options o, Baan baan) {
        boolean vrij = Minecraft.getInstance().screen == null;
        boolean s = (o.keyDown.isDown() || o.keyShift.isDown()) && vrij;
        boolean w = o.keyUp.isDown() && vrij;
        if (p.onGround()) {
            BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
            BlockState state = level.getBlockState(onder);
            if (s && rustDuik == 0 && state.getBlock() instanceof GuhrioBlocks.PijpBlok) {
                rustDuik = 15;
                stuur(GuhrioPayloads.Actie.DUIK, onder, 0);
            }
            if (state.getBlock() instanceof GuhrioStukken.SchakelaarBlok && !RUST_STAP.containsKey(onder)) {
                RUST_STAP.put(onder.immutable(), 20);
                stuur(GuhrioPayloads.Actie.STAP, onder, 0);
            }
            // a sideways pipe: walk into its mouth
            int teken = teken(o) * baan.schermRechts();
            if (teken != 0 && p.horizontalCollision && rustDuik == 0) {
                Direction d = baan.richting(stuk);
                Direction loopt = teken > 0 ? d : d.getOpposite();
                BlockPos voor = BlockPos.containing(p.getX() + loopt.getStepX() * 0.8, p.getY() + 0.2, p.getZ() + loopt.getStepZ() * 0.8);
                BlockState mond = level.getBlockState(voor);
                if (mond.getBlock() instanceof GuhrioBlocks.PijpBlok && mond.getValue(GuhrioBlocks.PijpBlok.FACING) == loopt.getOpposite()
                        && mond.getValue(GuhrioBlocks.PijpBlok.INGANG)) {
                    rustDuik = 15;
                    stuur(GuhrioPayloads.Actie.DUIK, voor, 0);
                }
            }
        }
        if (w && !wasW) {
            BlockPos hier = p.blockPosition();
            for (BlockPos pos : new BlockPos[]{hier, hier.above()}) {
                if (level.getBlockState(pos).getBlock() instanceof GuhrioStuk) {
                    stuur(GuhrioPayloads.Actie.DEUR, pos, 0);
                    break;
                }
            }
        }
        wasW = w;
    }

    /** A mouse button in a level: throw a knabbel (Vuurpeper) or Guhshi's tongue, the way you face. */
    static void actieToets() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || stil() || rustActie > 0 || (GuhrioClient.guhshi == 0 && GuhrioClient.kracht != GuhrioSpel.Kracht.VUUR.ordinal())) {
            return;
        }
        rustActie = ACTIE_RUST;
        p.swing(InteractionHand.MAIN_HAND);
        stuur(GuhrioPayloads.Actie.GOOI, p.blockPosition(), kijk);
    }

    /** Q in a level: once asks, twice (within two seconds) leaves. */
    static void stopToets() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || Cutscenes.bezig(p)) {
            return;
        }
        if (stopWacht > 0) {
            stopWacht = 0;
            stuur(GuhrioPayloads.Actie.STOP, p.blockPosition(), 0);
        } else {
            stopWacht = STOP_WACHT;
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("gui.guhs.guhrio.stoppen"), false);
        }
    }

    private static void stuur(int soort, BlockPos pos, int wezen) {
        ClientPacketDistributor.sendToServer(new GuhrioPayloads.Actie(soort, pos, wezen));
    }

    // =====================================================================================================================
    // the pipe and the flagpole
    // =====================================================================================================================

    /** The server lets you into the pipe whose mouth is at {@code pos}: you go in. */
    static void pijpIn(LocalPlayer p, BlockPos pos) {
        BlockState state = p.level().getBlockState(pos);
        pijpFase = 1;
        pijpTick = 0;
        pijpVan = GuhrioBlocks.PijpBlok.buiten(p.level(), pos, state);
        pijpNaar = GuhrioBlocks.PijpBlok.binnen(p.level(), pos, state);
        SPRONG.sprong = false;
        drager = null;
        p.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.7f, 0.7f);
    }

    /** The server put you in the other pipe (mouth at {@code pos}): you come out. */
    static void pijpUit(LocalPlayer p, BlockPos pos) {
        BlockState state = p.level().getBlockState(pos);
        pijpFase = 3;
        pijpTick = 0;
        pijpVan = GuhrioBlocks.PijpBlok.binnen(p.level(), pos, state);
        pijpNaar = GuhrioBlocks.PijpBlok.buiten(p.level(), pos, state);
        // (out of a mouth that hangs from above you drop; out of any other you stand)
        pijpStaat = !(state.getBlock() instanceof GuhrioBlocks.PijpBlok) || state.getValue(GuhrioBlocks.PijpBlok.FACING) != Direction.DOWN;
        SPRONG.sprong = false;
        Baan baan = GuhrioClient.baan();
        if (baan != null) {
            stuk = baan.plek(pijpNaar.x, pijpNaar.z).stuk();
        }
    }

    private static void pijp(LocalPlayer p) {
        pijpTick++;
        double t = Mth.clamp(pijpTick / (double) GuhrioSpel.PIJP_TICKS, 0, 1);
        Vec3 plek = switch (pijpFase) {
            case 1, 3 -> pijpVan.lerp(pijpNaar, t);
            default -> pijpNaar;
        };
        p.setPos(plek.x, plek.y, plek.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        if (pijpFase == 1 && t >= 1) {
            pijpFase = 2;
            pijpTick = 0;
        } else if (pijpFase == 2 && pijpTick > 60) {
            pijpFase = 0;                                        // (the server never took us through: step out again)
            p.setPos(pijpVan.x, pijpVan.y, pijpVan.z);
        } else if (pijpFase == 3 && t >= 1) {
            pijpFase = 0;
            if (pijpStaat) {
                p.setOnGround(true);
            }
        }
    }

    /** How far the pipe film is, for the fade: 0 open .. 1 black. */
    static float pijpDonker() {
        double t = Mth.clamp(pijpTick / (double) GuhrioSpel.PIJP_TICKS, 0, 1);
        return switch (pijpFase) {
            case 1 -> (float) Mth.clamp((t - 0.5) * 2, 0, 1);
            case 2 -> 1f;
            case 3 -> (float) Mth.clamp(1 - t * 2, 0, 1);
            default -> 0f;
        };
    }

    /** The flagpole at {@code pos}: you slide down it and wait. */
    static void klaar(LocalPlayer p, BlockPos pos) {
        mast = pos.immutable();
        SPRONG.sprong = false;
        drager = null;
        p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
    }
}
