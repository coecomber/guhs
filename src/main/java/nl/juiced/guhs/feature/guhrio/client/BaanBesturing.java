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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStuk;
import nl.juiced.guhs.feature.guhrio.GuhrioWezen;

/**
 * Your keys and your body in a level (the player's own game moves the player, so this is where the lane really holds you).
 * <ul>
 *     <li>The keys ({@link BaanInput}): A and D walk left and right on the screen, which is back and forth along the lane;
 *     space jumps (let go early for a small hop), S or sneak ducks (and goes down a pipe), W is for doors, sprint runs.
 *     Under it is the normal walking of the game: you are simply turned to face along the lane and "walk forward".</li>
 *     <li>The line: before and after every step you are put on the lane's line ({@link Baan#stap}), your sideways speed is
 *     gone, a corner turns your speed with you, the two ends stop you.</li>
 *     <li>What your game sees happen, the moment it happens, and tells the server ({@link GuhrioPayloads.Actie}): your
 *     head under a block, a coin, a creature you land on (you bounce at once) or that touches you, ducking on a pipe.</li>
 *     <li>The little films: sinking into a pipe and rising out of the other, sliding down the flagpole.</li>
 * </ul>
 * The numbers at the top are the feel of the game; change them here.
 */
public final class BaanBesturing {
    /** In the air you steer much better than normal: this much speed per tick towards where you push, up to these speeds. */
    public static final double LUCHT_STUUR = 0.03, LUCHT_LOOP = 0.21, LUCHT_REN = 0.29;
    /** Letting go of space while still rising this fast cuts the jump (a small hop). */
    public static final double HOP_VANAF = 0.18, HOP_REST = 0.45;
    /** The bounce off a creature: normal, and with space held. */
    public static final double STUITER = 0.52, STUITER_HOOG = 0.82;
    /** Sliding down the flagpole (blocks per tick). */
    public static final double GLIJ = 0.14;

    /** The piece of the lane you are on, the way you face along it (+1 further, -1 back). */
    private static int stuk;
    private static int kijk = 1;
    private static float yaw;
    /** Rising from a jump of your own (so letting go of space may cut it). */
    private static boolean sprong;
    private static double valVoor;
    private static boolean wasW;
    /** 0 not in a pipe; 1 sinking in; 2 waiting inside for the server; 3 rising out. */
    private static int pijpFase;
    private static int pijpTick;
    private static double pijpX, pijpZ, pijpTop, pijpDiep;
    @Nullable
    private static BlockPos mast;
    private static int rustStamp, rustDuik;
    private static final Map<Integer, Integer> RUST_WEZEN = new HashMap<>();

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

    /** No keys now: in a pipe, at the flagpole. */
    private static boolean stil() {
        return pijpFase != 0 || mast != null;
    }

    static boolean inPijp() {
        return pijpFase != 0;
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
        sprong = false;
        mast = null;
        yaw = yawVan(baan);
        if (!(p.input instanceof BaanInput)) {
            p.input = new BaanInput();
        }
    }

    static void einde() {
        pijpFase = 0;
        mast = null;
        sprong = false;
        RUST_WEZEN.clear();
    }

    /** You were put back at your flag. */
    static void terug(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        pijpFase = 0;
        sprong = false;
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
        if (p == null || !GuhrioClient.speelt()) {
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
        if (!(p.input instanceof BaanInput)) {
            p.input = new BaanInput();
        }
        if (p.getAbilities().flying) {
            p.getAbilities().flying = false;
        }
        RUST_WEZEN.replaceAll((id, t) -> t - 1);
        RUST_WEZEN.values().removeIf(t -> t <= 0);
        if (rustStamp > 0) {
            rustStamp--;
        }
        if (rustDuik > 0) {
            rustDuik--;
        }
        valVoor = p.getDeltaMovement().y;
        if (stil()) {
            return;
        }
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
        // in the air: steer
        if (!p.onGround() && teken != 0 && !p.isInWater()) {
            double top = p.isSprinting() ? LUCHT_REN : LUCHT_LOOP;
            if (langs * teken < top) {
                langs = teken > 0 ? Math.min(top, langs + LUCHT_STUUR) : Math.max(-top, langs - LUCHT_STUUR);
            }
        }
        double vy = v.y;
        // a small hop: space let go while still rising from your own jump
        if (sprong) {
            if (p.onGround() || vy <= 0) {
                sprong = false;
            } else if (!mc.options.keyJump.isDown() && vy > HOP_VANAF) {
                vy *= HOP_REST;
                sprong = false;
            }
        }
        p.setDeltaMovement(d.getStepX() * langs, vy, d.getStepZ() * langs);
        if (Math.abs(op.x() - p.getX()) > 1e-6 || Math.abs(op.z() - p.getZ()) > 1e-6) {
            p.setPos(op.x(), p.getY(), op.z());
        }
        yaw = yawVan(baan);
        p.setYRot(yaw);
        p.setXRot(0f);
    }

    // =====================================================================================================================
    // every tick, after you moved (before your game tells the server where you are)
    // =====================================================================================================================

    static void na(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
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
        if (!p.onGround() && valVoor <= 0 && p.getDeltaMovement().y > 0.3 && mc.options.keyJump.isDown()) {
            sprong = true;                                    // (left the ground by a jump of your own this tick)
        }
        Level level = p.level();
        kop(p, level);
        stukken(p, level);
        wezens(p, level, mc.options.keyJump.isDown());
        toetsen(p, level, mc.options);
    }

    /** Your head hit something on the way up: the block above you hops, and the server hears of it. */
    private static void kop(LocalPlayer p, Level level) {
        if (!(p.verticalCollision && !p.verticalCollisionBelow && valVoor > 0.05)) {
            return;
        }
        Baan baan = GuhrioClient.baan();
        Direction d = baan.richting(stuk);
        double y = p.getBoundingBox().maxY + 0.2;
        // the block over the middle of you, else the one over your front or back edge
        for (double uit : new double[]{0, 0.29, -0.29}) {
            BlockPos pos = BlockPos.containing(p.getX() + d.getStepX() * uit, y, p.getZ() + d.getStepZ() * uit);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GuhrioStuk && !state.getCollisionShape(level, pos).isEmpty()) {
                if (level.getBlockEntity(pos) instanceof GuhrioBlocks.StukBlockEntity be) {
                    be.bots = level.getGameTime();
                }
                if (state.getBlock() instanceof GuhrioBlocks.VraagBlok) {
                    if (!GuhrioClient.raad(pos, 1)) {
                        p.playSound(SoundEvents.STONE_HIT, 0.6f, 0.7f);
                    }
                } else if (state.getBlock() instanceof GuhrioBlocks.SteenBlok && GuhrioClient.kracht == GuhrioSpel.Kracht.SUPER.ordinal()) {
                    GuhrioClient.raad(pos, 1);
                } else {
                    p.playSound(SoundEvents.STONE_HIT, 0.6f, 0.9f);
                }
                stuur(GuhrioPayloads.Actie.BOTS, pos, 0);
                sprong = false;
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
                    if (state.getBlock() instanceof GuhrioBlocks.MuntBlok && GuhrioClient.staat(pos) == 0
                            && box.intersects(x + 0.2, y + 0.1, z + 0.2, x + 0.8, y + 0.9, z + 0.8) && GuhrioClient.raad(pos, 1)) {
                        stuur(GuhrioPayloads.Actie.RAAK, pos.immutable(), 0);
                    }
                }
            }
        }
    }

    /** The creatures you touch: on top of one you bounce, anything else is for the server to judge. */
    private static void wezens(LocalPlayer p, Level level, boolean spatie) {
        AABB box = p.getBoundingBox();
        for (Entity e : level.getEntities(p, box.inflate(0.05), x -> x instanceof GuhrioWezen && x.isAlive())) {
            GuhrioWezen w = (GuhrioWezen) e;
            if (RUST_WEZEN.containsKey(e.getId())) {
                continue;
            }
            boolean vanBoven = valVoor < -0.02 && p.yo >= e.getY() + e.getBbHeight() * 0.5;
            if (w.stampbaar() && vanBoven && rustStamp == 0) {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, spatie ? STUITER_HOOG : STUITER, v.z);
                p.resetFallDistance();
                sprong = false;
                rustStamp = 3;
                RUST_WEZEN.put(e.getId(), 12);
                p.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8f, 1.2f);
                stuur(GuhrioPayloads.Actie.STAMP, p.blockPosition(), e.getId());
            } else if (w.gevaarlijk()) {
                RUST_WEZEN.put(e.getId(), 12);
                stuur(GuhrioPayloads.Actie.GERAAKT, p.blockPosition(), e.getId());
            }
        }
    }

    /** S on a pipe, W at a door. */
    private static void toetsen(LocalPlayer p, Level level, Options o) {
        boolean s = (o.keyDown.isDown() || o.keyShift.isDown()) && Minecraft.getInstance().screen == null;
        boolean w = o.keyUp.isDown() && Minecraft.getInstance().screen == null;
        if (s && p.onGround() && rustDuik == 0) {
            BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
            if (level.getBlockState(onder).getBlock() instanceof GuhrioBlocks.PijpBlok) {
                rustDuik = 15;
                stuur(GuhrioPayloads.Actie.DUIK, onder, 0);
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

    private static void stuur(int soort, BlockPos pos, int wezen) {
        ClientPacketDistributor.sendToServer(new GuhrioPayloads.Actie(soort, pos, wezen));
    }

    // =====================================================================================================================
    // the pipe and the flagpole
    // =====================================================================================================================

    /** The server lets you into the pipe whose mouth is at {@code pos}: you sink in. */
    static void pijpIn(LocalPlayer p, BlockPos pos) {
        pijpFase = 1;
        pijpTick = 0;
        zetPijp(p, pos);
        p.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.7f, 0.7f);
    }

    /** The server put you in the other pipe (mouth at {@code pos}): you rise out. */
    static void pijpUit(LocalPlayer p, BlockPos pos) {
        pijpFase = 3;
        pijpTick = 0;
        zetPijp(p, pos);
        Baan baan = GuhrioClient.baan();
        if (baan != null) {
            stuk = baan.plek(pijpX, pijpZ).stuk();
        }
    }

    private static void zetPijp(LocalPlayer p, BlockPos pos) {
        pijpX = pos.getX() + 0.5;
        pijpZ = pos.getZ() + 0.5;
        pijpTop = pos.getY() + 1;
        pijpDiep = GuhrioSpel.pijpDiepte(p.level(), pos);
        sprong = false;
    }

    private static void pijp(LocalPlayer p) {
        pijpTick++;
        double t = Mth.clamp(pijpTick / (double) GuhrioSpel.PIJP_TICKS, 0, 1);
        double y = switch (pijpFase) {
            case 1 -> pijpTop - pijpDiep * t;
            case 3 -> pijpTop - pijpDiep * (1 - t);
            default -> pijpTop - pijpDiep;
        };
        p.setPos(pijpX, y, pijpZ);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        if (pijpFase == 1 && t >= 1) {
            pijpFase = 2;
            pijpTick = 0;
        } else if (pijpFase == 2 && pijpTick > 60) {
            pijpFase = 0;                                        // (the server never took us through: step out again)
            p.setPos(pijpX, pijpTop, pijpZ);
        } else if (pijpFase == 3 && t >= 1) {
            pijpFase = 0;
            p.setOnGround(true);
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
        sprong = false;
        p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
    }
}
