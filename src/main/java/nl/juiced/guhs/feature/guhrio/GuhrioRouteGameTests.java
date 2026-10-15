package nl.juiced.guhs.feature.guhrio;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhriow1.Binnentuin;
import nl.juiced.guhs.feature.guhriow2.GuhrioW2;
import nl.juiced.guhs.feature.guhriow3.GroteNetherMikaEntity;
import nl.juiced.guhs.feature.paleizen.PaleisProef;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the whole Kasteel van de Grote Nether-Mika (run with {@code -Pgt=GuhrioRouteGameTests}; two batches: the
 * castle is big and stands alone).
 * <ul>
 *     <li>{@code guhrioroute}: the jump. A mock player is moved the way the player's own game moves one ({@link Benen}: the
 *     sums of {@link BaanSprong}, which client.BaanBesturing uses every tick, around vanilla's own jump and move), and what
 *     he reaches must be what tools/features/guhrio_loop.py worked out (data/guhs/guhrio_route/sprong.json): the generators
 *     walk every level with those numbers. The dev commands that put big templates down are only there in a dev run. A
 *     player the server lost in a level is put at its entrance.</li>
 *     <li>{@code guhriokasteel}: the castle itself, as worldgen places it ({@link PaleisProef}: the very tiles, a copy the
 *     world knows), and ONE player who plays it from the gate of 1-1 to the tower room: every level's route of
 *     guhrio_loop.py step by step (jumps are really jumped where the ground is a block; pipes, doors, switches, the egg,
 *     the hatching, Guhshi, the bush and the hitching post are the engine's own), all 18 big vadsmunten, every flagpole
 *     with what hangs on it (the next gate opens, the questline moves, the hall takes you back), the duel, the princess.
 *     Afterwards: a player who stands in a sealed hall without playing its level is put at its gate, and a green travel
 *     pipe never leads into one.</li>
 * </ul>
 */
public class GuhrioRouteGameTests {
    private static final String BATCH = "guhrioroute", KASTEEL = "guhriokasteel", KAMER = "guhrio_test_baan", LEVEL = "guhrio_route_test";
    private static final int Z = 3;

    static {
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "R", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), new BlockPos(18, 0, 0), new BlockPos(-1, 0, 0), null));
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static JsonObject lees(ServerLevel level, String naam) {
        var res = level.getServer().getResourceManager().getResource(Guhs.id("guhrio_route/" + naam + ".json"));
        if (res.isEmpty()) {
            throw new IllegalStateException("no data/guhs/guhrio_route/" + naam + ".json: run the generators");
        }
        try (Reader reader = res.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // =====================================================================================================================
    // the legs of a player
    // =====================================================================================================================

    /**
     * Moves a server-side player one tick at a time the way the player's own game does in a level: client.BaanBesturing's
     * rules before the move ({@link BaanSprong}), then what vanilla's LivingEntity.aiStep / travelInAir do (the jump, the
     * push of the legs, {@code Entity.move} against the real blocks, gravity and friction), then BaanBesturing's "was that a
     * jump of your own". The lane runs along {@link #langs}. {@code kracht} / {@code snel}: a weaker jump for the walk of the
     * generators (jump power, and a factor on every speed along the lane); 0 / 1: the game's own.
     */
    static final class Benen {
        final ServerPlayer p;
        final Direction langs;
        final BaanSprong.Staat staat = new BaanSprong.Staat();
        double kracht, snel = 1.0;
        boolean gevlogen;

        Benen(ServerPlayer p, Direction langs) {
            this.p = p;
            this.langs = langs;
            p.setOnGround(true);
            p.setDeltaMovement(0, -p.getGravity() * 0.98f, 0);
        }

        double langs(Vec3 v) {
            return v.x * langs.getStepX() + v.z * langs.getStepZ();
        }

        /** {@code teken}: +1 further along the lane, -1 back, 0 nothing; {@code spatie}: the jump key. */
        void tik(int teken, boolean spatie, boolean guhshi) {
            ServerLevel level = p.level();
            Vec3 v = p.getDeltaMovement();
            double voor = v.y;
            boolean grond = p.onGround();
            // BaanBesturing.voor
            double l = langs(v);
            if (!grond) {
                l = BaanSprong.stuur(l / snel, teken, false) * snel;
            }
            double vy = staat.val(v.y, grond, !grond, spatie, guhshi);
            // LivingEntity.aiStep: tiny speeds are nothing, the jump
            if (l * l < 9.0e-6) {
                l = 0;
            }
            if (Math.abs(vy) < 0.003) {
                vy = 0;
            }
            p.setDeltaMovement(langs.getStepX() * l, vy, langs.getStepZ() * l);
            if (teken != 0) {
                p.setYRot((float) Math.toDegrees(Math.atan2(-langs.getStepX() * teken, langs.getStepZ() * teken)));
            }
            if (spatie && grond) {
                if (kracht > 0) {
                    v = p.getDeltaMovement();
                    p.setDeltaMovement(v.x, Math.max(kracht, v.y), v.z);
                } else {
                    p.jumpFromGround();
                }
            }
            // LivingEntity.travelInAir
            BlockPos onder = BlockPos.containing(p.getX(), p.getY() - 0.500001, p.getZ());
            float blok = grond ? level.getBlockState(onder).getFriction(level, onder, p) : 1.0f;
            float wrijf = blok * 0.91f;
            p.setSpeed((float) p.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float duw = (float) ((grond ? p.getSpeed() * (0.21600002f / (blok * blok * blok)) : 0.02f) * snel);
            p.moveRelative(duw, new Vec3(0, 0, teken == 0 ? 0 : 0.98f));
            p.move(MoverType.SELF, p.getDeltaMovement());
            v = p.getDeltaMovement();
            p.setDeltaMovement(v.x * wrijf, (v.y - p.getGravity()) * 0.98f, v.z * wrijf);
            p.resetFallDistance();
            // BaanBesturing.na
            staat.na(p.onGround(), voor, p.getDeltaMovement().y, spatie);
            gevlogen |= !p.onGround();
        }

        /**
         * One try of the walker (guhrio_loop.Fysica.patronen): the way d, a walking start v0, W ticks of walking, the jump
         * key held H ticks, steering after a ticks for L ticks, braking in the air afterwards. Returns the ticks it took to
         * come to rest, or -1 (still moving after {@code max} ticks).
         */
        int patroon(int d, int v0, int w, int h, int a, int l, boolean rem, boolean guhshi, double rest, int max) {
            p.setDeltaMovement(langs.getStepX() * v0 * d * rest, -p.getGravity() * 0.98f, langs.getStepZ() * v0 * d * rest);
            p.setOnGround(true);
            gevlogen = false;
            staat.sprong = false;
            staat.fladder = 0;
            int einde = w + Math.max(h < 99 ? h : 0, a + (l < 99 ? l : 0)) + 2;
            for (int t = 0; t < max; t++) {
                boolean geland = gevlogen && p.onGround();
                boolean stuurt = t >= w + a && t < w + a + l;
                int teken = t < w || stuurt ? d : 0;
                double vl = langs(p.getDeltaMovement());
                if (rem && t >= w + a + l && !p.onGround() && Math.abs(vl) > 0.04) {
                    teken = vl > 0 ? -1 : 1;
                }
                if (geland) {
                    teken = 0;
                }
                tik(teken, t >= w && t < w + h && !geland, guhshi);
                if (p.onGround() && Math.abs(langs(p.getDeltaMovement())) < 0.003 && (gevlogen || t >= einde)) {
                    return t + 1;
                }
            }
            return -1;
        }
    }

    private static BlockPos baan(GameTestHelper helper, int lang) {
        for (int x = 1; x <= lang; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioFeature.GROND.get());
        }
        BlockPos start = new BlockPos(2, 2, Z);
        helper.setBlock(start, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(start);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(abs)).zetLevel(LEVEL);
        return abs;
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double y) {
        Vec3 v = helper.absoluteVec(new Vec3(x, y, Z + 0.5));
        p.snapTo(v.x, v.y, v.z);
    }

    /**
     * The jump of a level, measured with the game's own code on the server: how high a held jump rises, how high a tap of
     * two ticks, how far a walking jump goes - and that is what the generators' walker counts with (sprong.json). A step of
     * three blocks can be taken with the whole jump and not with the walker's weaker one (which is why the walker says
     * where a level needs one).
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhrioRouteSprongKlopt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        JsonObject getallen = lees(level, "sprong");
        BlockPos startAbs = baan(helper, 21);
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "in the little level (the jump, the weight and the speed of a level are on)");
        Direction oost = Direction.EAST;
        double grond = helper.absoluteVec(new Vec3(0, 2, 0)).y;
        // a held jump, straight up
        zet(helper, p, 4.5, 2);
        Benen benen = new Benen(p, oost);
        double top = 0;
        for (int t = 0; t < 40; t++) {
            benen.tik(0, true, false);
            top = Math.max(top, p.getY() - grond);
            if (t > 2 && p.onGround()) {
                break;
            }
        }
        helper.assertTrue(Math.abs(top - getallen.get("hoog").getAsDouble()) < 0.01, "a held jump rises " + top + ", the walker counts with " + getallen.get("hoog"));
        // a tap of two ticks
        zet(helper, p, 4.5, 2);
        benen = new Benen(p, oost);
        top = 0;
        for (int t = 0; t < 40; t++) {
            benen.tik(0, t < 2, false);
            top = Math.max(top, p.getY() - grond);
            if (t > 2 && p.onGround()) {
                break;
            }
        }
        helper.assertTrue(Math.abs(top - getallen.get("tik").getAsDouble()) < 0.01, "a tap rises " + top + ", the walker counts with " + getallen.get("tik"));
        // a walking jump: a few steps, then space held
        zet(helper, p, 3.5, 2);
        benen = new Benen(p, oost);
        for (int t = 0; t < 12; t++) {
            benen.tik(1, false, false);
        }
        helper.assertTrue(Math.abs(benen.langs(p.getDeltaMovement()) / 0.546 - getallen.get("loop").getAsDouble()) < 0.002,
                "walking speed (blocks a tick) " + benen.langs(p.getDeltaMovement()) / 0.546 + ", the walker counts with " + getallen.get("loop"));
        double van = p.getX();
        int lucht = 0;
        for (int t = 0; t < 60; t++) {
            benen.tik(1, true, false);
            lucht++;
            if (t > 2 && p.onGround()) {
                break;
            }
        }
        helper.assertTrue(Math.abs(p.getX() - van - getallen.get("ver").getAsDouble()) < 0.02,
                "a walking jump goes " + (p.getX() - van) + " far in " + lucht + " ticks, the walker counts with " + getallen.get("ver"));
        // a wall of three: the whole jump takes it, the walker's weaker jump does not
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(12, y, Z), GuhrioFeature.BLOK.get());
            helper.setBlock(new BlockPos(13, y, Z), GuhrioFeature.BLOK.get());
        }
        for (boolean zwak : new boolean[]{false, true}) {
            zet(helper, p, 11.5, 2);
            benen = new Benen(p, oost);
            if (zwak) {
                benen.kracht = getallen.get("kracht_marge").getAsDouble();
                benen.snel = getallen.get("snel_marge").getAsDouble();
            }
            for (int t = 0; t < 40; t++) {
                benen.tik(1, true, false);
                if (t > 2 && p.onGround()) {
                    break;
                }
            }
            double hoog = p.getY() - grond;
            helper.assertTrue(zwak ? hoog < 0.01 : Math.abs(hoog - 3) < 0.01, (zwak ? "the weaker jump" : "the whole jump") + " against a wall of three ends "
                    + hoog + " up");
        }
        weg(helper, p);
        helper.succeed();
    }

    /**
     * The commands that put big templates into the world (the test levels: a box of air of 84 x 28 x 20; the whole castle)
     * are only registered in a dev run: with the switch off none of them is there, and everything else is.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhrioRouteDevCommandos(GameTestHelper helper) {
        for (boolean dev : new boolean[]{false, true}) {
            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            GuhrioEvents.registreer(d, dev);
            CommandNode<CommandSourceStack> guhrio = d.getRoot().getChild("guhs").getChild("guhrio");
            for (String naam : GuhrioEvents.DEV_COMMANDOS) {
                helper.assertTrue((guhrio.getChild(naam) != null) == dev, "/guhs guhrio " + naam + (dev ? " is a dev command" : " does not exist in a released game"));
            }
            for (String naam : List.of("start", "stop", "terug", "kracht", "guhshi", "kanaal", "gooi", "ei", "gehaald", "munten", "wis", "kasteel", "level", "info")) {
                helper.assertTrue(guhrio.getChild(naam) != null, "/guhs guhrio " + naam + " is always there");
            }
        }
        helper.assertTrue(GuhrioEvents.DEV_COMMANDOS.containsAll(List.of("testlevel", "testhoek", "bouwkasteel")), "the three that place templates");
        helper.succeed();
    }

    /**
     * The server stopped while a player was in a level (the level halls of the castle are sealed): at the next login the
     * player stands at that level's entrance, and the jump, the weight and the size of a level are off again. Somebody who
     * left a level the normal way is left where they are.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhrioRouteLoginNaCrash(GameTestHelper helper) {
        BlockPos startAbs = baan(helper, 21);
        ServerPlayer p = speler(helper), q = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "in the level");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        BlockPos ingang = s.level().ingang();
        helper.assertTrue(ingang != null && GuhrioSpel.spaar(p).contains("Binnen"), "the level's entrance is remembered with the player while he plays");
        GuhrioSpel.zetKracht(p, s, GuhrioSpel.Kracht.SUPER);
        zet(helper, p, 12.5, 2);
        double sprong = p.getAttributeValue(Attributes.JUMP_STRENGTH), schaal = p.getAttributeValue(Attributes.SCALE);
        helper.assertTrue(sprong > 0.6 && schaal > 1.4, "the jump and the size of a level: " + sprong + " / " + schaal);
        // the crash: the server forgets every session, the player's saved data still says "in a level"
        GuhrioSpel.vergeetAlles();
        helper.assertTrue(GuhrioSpel.sessie(p) == null && GuhrioSpel.spaar(p).contains("Binnen"), "the server lost the session, the player still has the mark");
        GuhrioSpel.login(p);
        helper.assertTrue(p.blockPosition().equals(ingang), "put at the level's entrance: " + p.blockPosition() + " / " + ingang);
        helper.assertTrue(!GuhrioSpel.spaar(p).contains("Binnen"), "the mark is gone");
        helper.assertTrue(p.getAttributeValue(Attributes.JUMP_STRENGTH) < 0.5 && p.getAttributeValue(Attributes.SCALE) < 1.01
                && p.getAttributeValue(Attributes.GRAVITY) < 0.09, "the jump, the weight and the size are normal again");
        // somebody who was never in a level (or left it the normal way) is left alone
        zet(helper, q, 9.5, 2);
        BlockPos was = q.blockPosition();
        GuhrioSpel.login(q);
        helper.assertTrue(q.blockPosition().equals(was), "a player without the mark stays where he is");
        helper.assertTrue(GuhrioSpel.start(q, startAbs), "in");
        GuhrioSpel.stop(q, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(!GuhrioSpel.spaar(q).contains("Binnen"), "leaving a level takes the mark away");
        weg(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the castle, played from 1-1 to the princess
    // =====================================================================================================================

    /** What the play-through is doing now: called every tick until it says true (then the next). */
    private record Taak(String naam, int geduld, BooleanSupplier klaar) {
    }

    /** One player's play-through of the castle. */
    private static final class Tocht {
        final GameTestHelper helper;
        final ServerLevel level;
        final ServerPlayer p;
        final BlockPos hoek;
        final List<Taak> taken = new ArrayList<>();
        int nu, wacht, tik;
        String fout;
        // the level that is being walked
        GuhrioLevel.Geplaatst g;
        int startS, startY;
        JsonArray banen;
        double kracht, snel, rest;
        int gesprongen, verplaatst;
        final List<String> anders = new ArrayList<>();

        Tocht(GameTestHelper helper, ServerPlayer p, BlockPos hoek) {
            this.helper = helper;
            this.level = helper.getLevel();
            this.p = p;
            this.hoek = hoek;
        }

        void dan(String naam, int geduld, BooleanSupplier klaar) {
            taken.add(new Taak(naam, geduld, klaar));
        }

        void eens(String naam, Runnable doe) {
            dan(naam, 1, () -> {
                doe.run();
                return true;
            });
        }

        void eis(boolean goed, String wat) {
            if (!goed && fout == null) {
                fout = wat;
            }
        }

        /** Every game tick: the player's tick (the level, the story engine, everything that hangs on it), then the task. */
        void tick() {
            if (fout != null || nu >= taken.size()) {
                return;
            }
            tik++;
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            Taak t = taken.get(nu);
            boolean klaar;
            try {
                klaar = t.klaar.getAsBoolean();
            } catch (RuntimeException e) {
                fout = t.naam + ": " + e;
                return;
            }
            if (fout != null) {
                fout = t.naam + ": " + fout;
            } else if (klaar) {
                nu++;
                wacht = 0;
            } else if (++wacht > t.geduld) {
                fout = t.naam + ": still waiting after " + t.geduld + " ticks";
            }
        }

        boolean klaar() {
            return nu >= taken.size();
        }

        GuhrioSpel.Sessie s() {
            return GuhrioSpel.sessie(p);
        }

        BlockPos cel(int s, int y) {
            return g.wereld(new BlockPos(s - startS, y - startY, 0));
        }

        BlockPos cel(JsonArray a) {
            return cel(a.get(0).getAsInt(), a.get(1).getAsInt());
        }

        /** A point of the lane's plane (s along the lane, counted like the cells; y up) in the world. */
        Vec3 punt(double s, double y) {
            BlockPos a = g.anker();
            Direction k = g.kant();
            double ver = s - (startS + 0.5);
            return new Vec3(a.getX() + 0.5 + k.getStepX() * ver, a.getY() + (y - startY), a.getZ() + 0.5 + k.getStepZ() * ver);
        }

        void zet(BlockPos cel) {
            p.snapTo(cel.getX() + 0.5, cel.getY(), cel.getZ() + 0.5);
            p.setDeltaMovement(Vec3.ZERO);
            p.setOnGround(true);
        }

        /** Is there a block under this cell that holds the player up? */
        boolean grond(BlockPos cel) {
            BlockState onder = level.getBlockState(cel.below());
            return !onder.getCollisionShape(level, cel.below(), net.minecraft.world.phys.shapes.CollisionContext.of(p)).isEmpty();
        }

        /** A deck (a moving platform, a falling block) whose spot is near under this cell: the walker waits for it there. */
        boolean dek(BlockPos cel) {
            for (GuhrioSpel.Stuk stuk : s().actief.stukken) {
                if ((stuk.state().getBlock() instanceof GuhrioStukken.PlatformPlek || stuk.state().getBlock() instanceof GuhrioStukken.ValblokPlek)
                        && stuk.pos().distManhattan(cel) <= 18) {
                    return true;
                }
            }
            return false;
        }

        /** After a step: the player is where the walker says, as big, on or off Guhshi, in nothing solid, on something. */
        void staat(JsonObject stap, String wat) {
            if (!stap.has("naar")) {
                return;
            }
            JsonArray naar = stap.getAsJsonArray("naar");
            BlockPos cel = cel(naar.get(1).getAsInt(), naar.get(2).getAsInt());
            GuhrioSpel.Sessie s = s();
            eis(s != null, wat + ": the level ended");
            if (s == null) {
                return;
            }
            p.refreshDimensions();
            eis(p.blockPosition().equals(cel), wat + ": the player is at " + p.blockPosition() + ", the walker at " + cel + " (cell " + naar + ")");
            eis(s.baan == naar.get(0).getAsInt(), wat + ": on lane " + s.baan + ", the walker on " + naar.get(0));
            // (blocks only: a platform's deck is an entity one stands ON, and touching one counts as a collision)
            eis(level.noBlockCollision(p, p.getBoundingBox()), wat + ": the player stands in a block at cell " + naar);
            eis(grond(cel) || dek(cel), wat + ": nothing to stand on at cell " + naar);
            eis(s.kracht.ordinal() == stap.get("kracht").getAsInt(), wat + ": power " + s.kracht + ", the walker has " + stap.get("kracht"));
            eis(s.guhshi == stap.get("guhshi").getAsBoolean(), wat + ": on Guhshi " + s.guhshi + ", the walker " + stap.get("guhshi"));
        }

        /** A jump of the walker, really jumped (when both ends stand on blocks): true when the player landed where he says. */
        boolean spring(JsonObject stap, BlockPos doel) {
            JsonArray a = stap.getAsJsonArray("p");
            Vec3 was = p.position();
            if (!grond(p.blockPosition()) || !grond(doel)) {
                return false;
            }
            p.refreshDimensions();
            Benen benen = new Benen(p, g.kant());
            benen.kracht = kracht;
            benen.snel = snel;
            int ticks = benen.patroon(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt(), a.get(4).getAsInt(),
                    a.get(5).getAsInt(), a.get(6).getAsInt() != 0, s().guhshi, rest, 140);
            gesprongen++;
            boolean goed = ticks > 0 && p.blockPosition().getY() == doel.getY() && p.getBoundingBox().intersects(new AABB(doel).inflate(0.05, 0.5, 0.05));
            if (!goed) {
                anders.add(g.level().wereld() + " " + stap.get("p") + " from " + BlockPos.containing(was) + " ended at " + p.position() + " in " + ticks
                        + " ticks, not at " + doel);
            }
            return goed;
        }
    }

    private static BlockPos poort(ServerLevel level, BlockPos hoek, BlockPos start) {
        // the gates stand in the walls of the level hall of the keep (x 39..87, z 34..82, floor y 26)
        for (BlockPos pos : BlockPos.betweenClosed(hoek.offset(39, 27, 34), hoek.offset(87, 28, 82))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GuhrioBlocks.PoortBlok && start.equals(GuhrioBlocks.PoortBlok.start(level, pos, state))) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** The steps of one run of a level (a list of the route file) as tasks of the play-through. */
    private static void loop(Tocht t, String naam, JsonArray stappen, BlockPos start, double kracht, double snel, double rest) {
        ServerPlayer p = t.p;
        for (int i = 0; i < stappen.size(); i++) {
            JsonObject stap = stappen.get(i).getAsJsonObject();
            String soort = stap.get("t").getAsString();
            String wat = naam + " step " + i + " " + soort;
            switch (soort) {
                case "loop", "lift", "sprong" -> t.eens(wat, () -> {
                    JsonArray naar = stap.has("naar") ? stap.getAsJsonArray("naar") : null;
                    if (stap.has("raak")) {
                        // the jump touches a big vadsmunt in the air: the player's own game reports it from that very spot
                        int n = stap.get("raak").getAsInt();
                        BlockPos munt = t.cel(stap.getAsJsonArray("cel"));
                        t.eis(stap.has("punt"), wat + ": the walker knows no spot where the jump touches vadsmunt " + n);
                        if (stap.has("punt")) {
                            Vec3 punt = t.punt(stap.getAsJsonArray("punt").get(0).getAsDouble(), stap.getAsJsonArray("punt").get(1).getAsDouble());
                            p.snapTo(punt.x, punt.y, punt.z);
                            p.refreshDimensions();
                            // (the spot is written with three decimals: a body that just touches a wall there may be a hair inside it)
                            t.eis(t.level.noBlockCollision(p, p.getBoundingBox().deflate(0.002)), wat + ": the spot where the jump touches the vadsmunt is inside a block: " + punt);
                            GuhrioSpel.actie(p, GuhrioPayloads.Actie.RAAK, munt, 0);
                            t.eis((t.s().vads >> n & 1) != 0, wat + ": vadsmunt " + n + " at " + munt + " was not taken from " + punt);
                        }
                    } else if (naar != null && soort.equals("sprong") && t.kracht >= 0) {
                        // really jump it; whatever the result, go on from where the walker stands
                        t.spring(stap, t.cel(naar.get(1).getAsInt(), naar.get(2).getAsInt()));
                    }
                    if (stap.has("langs")) {
                        // the pieces the jump passes on its way (a bush, a hitching post, Guhshi): the server sees the player in
                        // each of them for a tick, as it does when a real player's game reports where he is
                        for (JsonElement e : stap.getAsJsonArray("langs")) {
                            BlockPos c = t.cel(e.getAsJsonArray());
                            p.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                            GuhrioSpel.tick(p);
                        }
                    }
                    if (naar != null) {
                        t.zet(t.cel(naar.get(1).getAsInt(), naar.get(2).getAsInt()));
                        t.verplaatst++;
                        GuhrioSpel.tick(p);
                        t.staat(stap, wat);
                    }
                });
                case "raak" -> t.eens(wat, () -> {
                    int n = stap.get("n").getAsInt();
                    GuhrioSpel.tick(p);
                    t.eis(t.s() != null && (t.s().vads >> n & 1) != 0, wat + ": standing in vadsmunt " + n + " did not take it");
                });
                case "pijp" -> {
                    // (a Hapbloem that is out of its pipe: wait until it is down)
                    t.dan(wat + " in", 600, () -> {
                        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, t.cel(stap.getAsJsonArray("mond")), 0);
                        return t.s().inPijp();
                    });
                    t.dan(wat + " uit", 80, () -> !t.s().inPijp());
                    t.eens(wat + " staat", () -> {
                        // the server has put the player deep inside the other mouth (the player's own game lets him rise, drop
                        // or step out of it): that must be the mouth the walker came out of, and where it lets you out must be
                        // where he stands (out of a mouth that hangs from above you drop: he stands where you land)
                        JsonArray naar = stap.getAsJsonArray("naar");
                        BlockPos cel = t.cel(naar.get(1).getAsInt(), naar.get(2).getAsInt()), uit = t.cel(stap.getAsJsonArray("uit"));
                        BlockState mond = t.level.getBlockState(uit);
                        t.eis(mond.getBlock() instanceof GuhrioBlocks.PijpBlok, wat + ": no pipe mouth at " + uit);
                        Vec3 binnen = GuhrioBlocks.PijpBlok.binnen(t.level, uit, mond), buiten = GuhrioBlocks.PijpBlok.buiten(t.level, uit, mond);
                        t.eis(p.position().distanceTo(binnen) < 0.2, wat + ": the pipe took the player to " + p.position() + ", the walker comes out of the mouth at " + uit);
                        BlockPos staat = BlockPos.containing(buiten.x, buiten.y + 0.01, buiten.z);
                        t.eis(staat.getX() == cel.getX() && staat.getZ() == cel.getZ() && staat.getY() >= cel.getY()
                                        && (staat.getY() == cel.getY() || mond.getValue(GuhrioBlocks.PijpBlok.FACING) == Direction.DOWN),
                                wat + ": the mouth at " + uit + " lets you out at " + staat + ", the walker stands at " + cel);
                        t.zet(cel);
                        t.staat(stap, wat);
                    });
                }
                case "deur" -> t.eens(wat, () -> {
                    GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, t.cel(stap.getAsJsonArray("cel")), 0);
                    t.staat(stap, wat);
                });
                case "bots", "stap" -> t.eens(wat, () -> {
                    BlockPos blok = t.cel(stap.getAsJsonArray("blok"));
                    GuhrioSpel.Sessie s = t.s();
                    GuhrioSpel.actie(p, soort.equals("bots") ? GuhrioPayloads.Actie.BOTS : GuhrioPayloads.Actie.STAP, blok, 0);
                    if (stap.has("kanaal")) {
                        t.eis((s.kanalen >> stap.get("kanaal").getAsInt() & 1) != 0, wat + ": the switch at " + blok + " did not switch channel " + stap.get("kanaal"));
                    } else {
                        t.eis(s.staat(blok) == 1, wat + ": the block at " + blok + " (" + t.level.getBlockState(blok).getBlock() + ") did not answer");
                    }
                    t.staat(stap, wat);
                });
                case "knabbel" -> {
                    int kanaal = stap.get("kanaal").getAsInt();
                    int[] sinds = {0};
                    t.dan(wat, 400, () -> {
                        GuhrioSpel.Sessie s = t.s();
                        if ((s.kanalen >> kanaal & 1) != 0) {
                            return true;
                        }
                        if (sinds[0]++ % 45 == 0) {
                            t.eis(GuhrioSpel.gooi(p, s, stap.get("teken").getAsInt()) || sinds[0] > 1, wat + ": no knabbel could be thrown (power " + s.kracht + ")");
                        }
                        return false;
                    });
                    t.eens(wat + " staat", () -> t.staat(stap, wat));
                }
                case "schild" -> {
                    int kanaal = stap.get("kanaal").getAsInt(), teken = stap.get("teken").getAsInt();
                    SchildMikaEntity[] mika = {null};
                    int[] fase = {0};
                    t.dan(wat, 20000, () -> {
                        GuhrioSpel.Sessie s = t.s();
                        if ((s.kanalen >> kanaal & 1) != 0) {
                            return true;
                        }
                        if (!(s.actief.wezens.get(t.cel(stap.getAsJsonArray("mika"))) instanceof SchildMikaEntity m) || m.isRemoved() || m.weg()) {
                            return false;                         // (he comes when his chunk ticks)
                        }
                        mika[0] = m;
                        Direction k = t.g.kant();
                        if (m.stand() == SchildMikaEntity.LOOPT || (m.stand() == SchildMikaEntity.GLIJDT && fase[0]++ > 200)) {
                            // land on him: he pulls into his shell
                            p.snapTo(m.getX(), m.getY() + 0.9, m.getZ());
                            GuhrioSpel.actie(p, GuhrioPayloads.Actie.STAMP, p.blockPosition(), m.getId());
                            fase[0] = 0;
                        } else if (m.stand() == SchildMikaEntity.SCHILD) {
                            // walk into the shell from the side away from the switch: it slides off
                            p.snapTo(m.getX() - k.getStepX() * teken * 0.9, m.getY(), m.getZ() - k.getStepZ() * teken * 0.9);
                            GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, p.blockPosition(), m.getId());
                        }
                        return false;
                    });
                    t.eens(wat + " staat", () -> {
                        JsonArray naar = stap.getAsJsonArray("naar");
                        t.zet(t.cel(naar.get(1).getAsInt(), naar.get(2).getAsInt()));
                        t.staat(stap, wat);
                    });
                }
                case "krimp" -> {
                    // a creature touches you: Guhshi runs off, or else the power-up is gone
                    t.dan(wat, 200, () -> {
                        GuhrioSpel.Sessie s = t.s();
                        if (s.kracht.ordinal() == stap.get("kracht").getAsInt() && s.guhshi == stap.get("guhshi").getAsBoolean()) {
                            return true;
                        }
                        GuhrioSpel.geraakt(p, s);
                        return false;
                    });
                    t.eens(wat + " staat", () -> t.staat(stap, wat));
                }
                case "ei" -> {
                    t.dan(wat, 60, () -> GuhrioKasteel.heeftEi(p) && (t.s().kanalen & 0x80) != 0);
                    t.eens(wat + " staat", () -> t.staat(stap, wat));
                }
                case "uit" -> {
                    t.dan(wat, 200, () -> GuhrioW2.uitgebroed(p) && !nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p));
                    t.eens(wat + " staat", () -> {
                        JsonArray naar = stap.getAsJsonArray("naar");
                        t.zet(t.cel(naar.get(1).getAsInt(), naar.get(2).getAsInt()));
                        GuhrioSpel.tick(p);
                        t.staat(stap, wat);
                    });
                }
                case "opnieuw" -> t.eens(wat, () -> {
                    GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
                    t.eis(GuhrioSpel.start(p, start), wat + ": the level does not start again");
                    t.kracht = kracht;
                    t.snel = snel;
                    t.rest = rest;
                });
                case "mast" -> t.eens(wat, () -> {
                    GuhrioSpel.tick(p);
                    t.eis(t.s() != null && t.s().klaar(), wat + ": the flagpole did not end the level");
                });
                default -> t.eens(wat, () -> t.eis(false, wat + ": the test does not know this step"));
            }
        }
    }

    /**
     * ONE player plays the whole castle, in the real castle: in through the gate of 1-1, every level's route of the
     * generators' walker (with the three big vadsmunten on the way), every flagpole and what hangs on it, the egg, the
     * hatching, the duel, the tower room with the princess. Then the sealed halls and the travel pipes.
     */
    // (the timeout is in ticks and the test server runs a thousand of them a second: what waits for a chunk's entities
    //  waits for real time, so every wait here is generous; a step that goes wrong fails the test at once)
    @GuhTest(template = "empty", batch = KASTEEL, timeoutTicks = 120000)
    public static void guhrioRouteHeleKasteel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 150, 0));
        for (int cx = (hoek.getX() - 16) >> 4; cx <= (hoek.getX() + 144) >> 4; cx++) {
            for (int cz = (hoek.getZ() - 16) >> 4; cz <= (hoek.getZ() + 144) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }
        var kopie = PaleisProef.bouw(level, GuhrioKasteel.STRUCTUUR, BlockPos.ZERO, hoek, true);
        JsonObject getallen = lees(level, "sprong");
        double zwak = getallen.get("kracht_marge").getAsDouble(), traag = getallen.get("snel_marge").getAsDouble();
        ServerPlayer p = speler(helper);
        Tocht t = new Tocht(helper, p, hoek);
        List<GuhrioKasteel.Hal> hallen = GuhrioKasteel.hallen(level.getServer());
        helper.assertTrue(hallen.size() == 7, "seven sealed halls (six levels and the duel): " + hallen.size());
        // the sauce in the trench under every lane gets its tick now (worldgen gives it one when the chunk is finished): by
        // the end of the play-through it would have spread along the trench, if the generator had not closed every pit
        int[] bronnen = {0};
        for (GuhrioKasteel.Hal hal : hallen) {
            BoundingBox d = hal.doos();
            for (BlockPos pos : BlockPos.betweenClosed(hoek.offset(d.minX(), d.minY(), d.minZ()), hoek.offset(d.maxX(), d.minY(), d.maxZ()))) {
                net.minecraft.world.level.material.FluidState saus = level.getFluidState(pos);
                if (saus.isSource()) {
                    bronnen[0]++;
                    level.scheduleTick(pos.immutable(), saus.getType(), 2);
                }
            }
        }
        helper.assertTrue(bronnen[0] > 80, "the pits of the levels and the duel hold their sauce: " + bronnen[0] + " cells");
        BlockPos halUit = hoek.offset(63, 27, 60);
        int[] vads = {0};
        for (String id : GuhrioKasteel.LEVELS) {
            GuhrioKasteel.Hal hal = hallen.stream().filter(h -> h.level().equals(id)).findFirst().orElseThrow();
            BlockPos start = hoek.offset(hal.start());
            JsonObject route = lees(level, id);
            String naam = route.get("wereld").getAsString();
            double rust = getallen.get("loop").getAsDouble() * traag * 0.546;
            // in through the gate in the level hall
            t.dan(naam + " the gate", 200, () -> {
                BlockPos poort = poort(level, hoek, start);
                t.eis(poort != null, naam + ": no gate in the level hall leads to the start block at " + start);
                t.eis(level.getBlockState(start).getBlock() instanceof GuhrioBlocks.StartBlok, naam + ": no start block at " + start);
                if (poort == null) {
                    return false;
                }
                p.snapTo(poort.getX() + 0.5, poort.getY(), poort.getZ() + 0.5);
                level.getBlockState(poort).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(poort), Direction.UP, poort, false));
                GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                if (s == null) {
                    return false;
                }
                t.eis(s.level().level().id().equals(id), naam + ": the gate started " + s.level().level().id());
                t.g = s.level();
                t.startS = route.getAsJsonArray("start").get(0).getAsInt();
                t.startY = route.getAsJsonArray("start").get(1).getAsInt();
                t.kracht = zwak;
                t.snel = traag;
                t.rest = rust;
                t.eis(p.blockPosition().equals(start), naam + ": the level starts at " + p.blockPosition() + ", its start block is at " + start);
                t.eis(GuhrioKasteel.heeftEi(p) == route.get("ei").getAsBoolean(), naam + ": the walker walked this level " + (route.get("ei").getAsBoolean() ? "with" : "without") + " the egg");
                return true;
            });
            loop(t, naam, route.getAsJsonArray("stappen"), start, zwak, traag, rust);
            // what only the whole jump reaches: a run of its own for each
            for (JsonElement e : route.getAsJsonArray("krap")) {
                JsonObject krap = e.getAsJsonObject();
                t.eens(naam + " again for " + krap.get("wat").getAsString(), () -> {
                    if (GuhrioSpel.sessie(p) != null) {
                        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
                    }
                    t.eis(GuhrioSpel.start(p, start), naam + ": the level does not start again");
                    t.g = GuhrioSpel.sessie(p).level();
                    t.kracht = 0;
                    t.snel = 1;
                    t.rest = getallen.get("loop").getAsDouble() * 0.546;
                });
                loop(t, naam + " (whole jump)", krap.getAsJsonArray("stappen"), start, 0, 1, getallen.get("loop").getAsDouble() * 0.546);
            }
            // the flagpole lets go after a few seconds: back in the level hall, the level counts, its three vadsmunten too
            int nr = GuhrioKasteel.LEVELS.indexOf(id);
            t.dan(naam + " the flagpole lets go", 200, () -> {
                GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
                if (s != null && !s.klaar()) {
                    // (the last run was one for a vadsmunt: walk out, the level is done already)
                    GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
                    return false;
                }
                return s == null;
            });
            t.eens(naam + " done", () -> {
                t.eis(GuhrioKasteel.gehaald(p, id), naam + " does not count as finished");
                t.eis(GuhrioKasteel.vadsmunten(p, id) == 7, naam + ": the three big vadsmunten: " + Integer.toBinaryString(GuhrioKasteel.vadsmunten(p, id)));
                t.eis(GuhrioKasteel.LIJN.stap(p) == nr + 2 || GuhrioKasteel.LIJN.klaar(p), naam + ": the questline is at step " + GuhrioKasteel.LIJN.stap(p));
                t.eis(p.blockPosition().closerThan(halUit, 30), naam + ": after the level the player is at " + p.blockPosition() + ", the hall is at " + halUit);
                t.eis(Math.abs(p.getAttributeValue(Attributes.JUMP_STRENGTH) - 0.42) < 0.01 && p.getAttributeValue(Attributes.SCALE) < 1.01,
                        naam + ": outside a level the jump and the size are normal");
                vads[0] += 3;
                t.eis(GuhrioKasteel.alleVadsmunten(p) == vads[0], naam + ": " + GuhrioKasteel.alleVadsmunten(p) + " big vadsmunten so far, not " + vads[0]);
                if (nr + 1 < GuhrioKasteel.LEVELS.size()) {
                    GuhrioLevel volgende = GuhrioLevel.vind(level.getServer(), GuhrioKasteel.LEVELS.get(nr + 1));
                    t.eis(volgende != null && GuhrioKasteel.magIn(p, volgende), naam + ": the next level is open now");
                }
            });
            if (id.equals(Binnentuin.LEVEL_2)) {
                // Pad-guh at the end of 1-2 thanks you a moment after the flagpole (world 1's own listener)
                t.dan(naam + " Pad-guh's thanks", 300, () -> Binnentuin.bedankt(p) == 1);
            }
            if (id.equals("kasteel_2_2")) {
                t.eens(naam + " Guhshi", () -> t.eis(GuhrioKasteel.heeftEi(p) && GuhrioW2.uitgebroed(p), naam + ": the egg is found and hatched"));
            }
        }
        // the duel: in through the great gate, the card, his roar; then he is knocked off his bridge (the three rounds have
        // their own test in GuhrioW3GameTests) and everything after that is the game's own: the splash, the sulk, the end
        // scene, the win, the tower room
        GuhrioKasteel.Hal duel = hallen.stream().filter(h -> h.level().equals(GuhrioKasteel.DUEL)).findFirst().orElseThrow();
        BlockPos duelStart = hoek.offset(duel.start());
        GroteNetherMikaEntity[] mika = {null};
        t.eens("all eighteen", () -> t.eis(GuhrioKasteel.alleVadsmunten(p) == GuhrioKasteel.VADSMUNTEN
                && nl.juiced.guhs.feature.gids.GidsFeature.heeft(p, "quest/guhrio_vadsmunten"), "all 18 big vadsmunten and their advancement"));
        t.dan("the duel's gate", 200, () -> {
            BlockPos poort = poort(level, hoek, duelStart);
            t.eis(poort != null, "no gate leads to the duel");
            if (poort == null) {
                return false;
            }
            p.snapTo(poort.getX() + 0.5, poort.getY(), poort.getZ() + 0.5);
            level.getBlockState(poort).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(poort), Direction.UP, poort, false));
            return GuhrioSpel.sessie(p) != null && GuhrioSpel.sessie(p).level().level().id().equals(GuhrioKasteel.DUEL);
        });
        t.dan("the Grote Nether-Mika walks", 20000, () -> {
            for (Entity e : GuhrioSpel.wezens(GuhrioSpel.sessie(p).actief)) {
                if (e instanceof GroteNetherMikaEntity m) {
                    mika[0] = m;
                }
            }
            return mika[0] != null && mika[0].stand() == GroteNetherMikaEntity.LOOPT;
        });
        t.eens("off the bridge with him", () -> mika[0].devRonde(level, 4));
        t.dan("the end scene, the win, the tower room", 1500, () -> GuhrioSpel.sessie(p) == null && GuhrioKasteel.duelGewonnen(p));
        t.eens("the tower room", () -> {
            BoundingBox kamer = new BoundingBox(hoek.getX() + 53, hoek.getY() + 43, hoek.getZ() + 58, hoek.getX() + 73, hoek.getY() + 49, hoek.getZ() + 70);
            t.eis(kamer.isInside(p.blockPosition()), "after the duel the player stands at " + p.blockPosition() + ", not in the tower room " + kamer);
            t.eis(GuhrioKasteel.LIJN.klaar(p), "the questline of the castle is done");
            t.eis(VerhaalGuhs.isVrij(p, VerhaalGuh.GUHSHI), "Guhshi is yours now (the reward slice heard of the duel)");
        });
        t.dan("the princess and Guhshi are at home", 40000, () -> {
            AABB kamer = new AABB(hoek.getX() + 53, hoek.getY() + 43, hoek.getZ() + 58, hoek.getX() + 74, hoek.getY() + 50, hoek.getZ() + 71);
            return level.getEntitiesOfClass(GuhNpcEntity.class, kamer, n -> n.getKind() == GuhNpcEntity.Kind.PERZIKGUH).size() == 1
                    && level.getEntitiesOfClass(GuhEntity.class, kamer, n -> n.getVariant() == GuhVariant.GUHSHI).size() == 1;
        });
        // --- a sealed hall lets nobody stay who does not play its level; a travel pipe never leads into one ---------------
        GuhrioKasteel.Hal eerste = hallen.get(0);
        BlockPos inHal = hoek.offset(eerste.start()).above(2);
        BlockPos[] poort11 = {null};
        t.eens("lost in a hall", () -> {
            GuhrioKasteel.Uitweg uit = GuhrioKasteel.halBij(level, inHal);
            t.eis(uit != null && uit.hal().level().equals(eerste.level()), "the start block of 1-1 stands in the hall of 1-1");
            t.eis(GuhrioKasteel.halBij(level, halUit) == null && GuhrioKasteel.halBij(level, hoek.offset(63, 27, 118)) == null,
                    "the level hall of the keep and the forecourt are no sealed halls");
            if (uit != null) {
                poort11[0] = uit.poort();
                t.eis(poort11[0].closerThan(halUit, 30), "the way out of the hall of 1-1 is its gate in the level hall: " + poort11[0]);
            }
            p.snapTo(inHal.getX() + 0.5, inHal.getY(), inHal.getZ() + 0.5);
        });
        t.dan("put back at the gate", 200, () -> poort11[0] != null && p.blockPosition().equals(poort11[0]));
        t.eens("travel pipes", () -> {
            BlockState mond = nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningFeature.PIJP.get().defaultBlockState()
                    .setValue(nl.juiced.guhs.feature.guhriobeloning.PijpBlock.MOND, true);
            BlockPos a = hoek.offset(45, 27, 60), inDeHal = hoek.offset(17, 27, 60), verder = hoek.offset(47, 27, 76);
            level.setBlock(a, mond, 3);
            level.setBlock(inDeHal, mond, 3);
            t.eis(GuhrioKasteel.halBij(level, inDeHal.above()) != null, "the second pipe stands in the hall of 1-1");
            t.eis(inDeHal.equals(nl.juiced.guhs.feature.guhriobeloning.Pijpreis.partner(level, a)), "for nobody in particular the two pipes are a pair");
            t.eis(nl.juiced.guhs.feature.guhriobeloning.Pijpreis.partner(level, a, p) == null, "no traveller is sent into a sealed hall");
            level.setBlock(verder, mond, 3);
            t.eis(verder.equals(nl.juiced.guhs.feature.guhriobeloning.Pijpreis.partner(level, a, p)), "a pipe further off, outside the hall, is the partner then");
        });
        t.eens("the sauce stayed in its pits", () -> {
            int bron = 0, stroomt = 0;
            for (GuhrioKasteel.Hal hal : hallen) {
                BoundingBox d = hal.doos();
                for (BlockPos pos : BlockPos.betweenClosed(hoek.offset(d.minX(), d.minY(), d.minZ()), hoek.offset(d.maxX(), d.minY(), d.maxZ()))) {
                    net.minecraft.world.level.material.FluidState saus = level.getFluidState(pos);
                    if (saus.isSource()) {
                        bron++;
                    } else if (!saus.isEmpty()) {
                        stroomt++;
                    }
                }
            }
            t.eis(stroomt == 0 && bron == bronnen[0], stroomt + " cells of the trenches hold flowing sauce (" + bron + " of " + bronnen[0] + " cells of sauce are where they were put)");
        });
        helper.onEachTick(() -> {
            t.tick();
            if (t.fout != null) {
                helper.fail(t.fout + " (task " + t.nu + " of " + t.taken.size() + ", game tick " + t.tik + ")");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(t.fout == null, String.valueOf(t.fout));
            helper.assertTrue(t.klaar(), "the play-through is at " + (t.nu < t.taken.size() ? t.taken.get(t.nu).naam : "the end") + " (task " + t.nu + " of "
                    + t.taken.size() + ", game tick " + t.tik + ")");
            // most jumps of the walker are really jumped: nearly all must end where he says (a few fall just outside what
            // a mock player's legs do the same: said in the test's last line, never silently)
            helper.assertTrue(t.gesprongen > 100 && t.anders.size() * 20 <= t.gesprongen, t.anders.size() + " of " + t.gesprongen
                    + " jumps ended somewhere else than the walker says: " + t.anders);
            org.slf4j.LoggerFactory.getLogger("guhs").info("guhrio route: {} moves, {} jumps really jumped, {} of them ended elsewhere: {}", t.verplaatst,
                    t.gesprongen, t.anders.size(), t.anders);
            GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            // take the castle away again
            Kopieen.testWissen(level);
            BoundingBox doos = kopie.getBoundingBox();
            for (Entity e : level.getEntitiesOfClass(Entity.class, AABB.of(doos).inflate(4), e -> !(e instanceof Player))) {
                e.discard();
            }
            for (BlockPos pos : BlockPos.betweenClosed(doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ())) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
            for (int cx = (hoek.getX() - 16) >> 4; cx <= (hoek.getX() + 144) >> 4; cx++) {
                for (int cz = (hoek.getZ() - 16) >> 4; cz <= (hoek.getZ() + 144) >> 4; cz++) {
                    level.setChunkForced(cx, cz, false);
                }
            }
        });
    }
}
