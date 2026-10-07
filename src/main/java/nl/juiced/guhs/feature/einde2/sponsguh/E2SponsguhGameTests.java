package nl.juiced.guhs.feature.einde2.sponsguh;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhTime;

/**
 * Game tests of the slice sponsguh_wezens: the cast (every type spawns, wears its model's animations, can't be hurt, keeps
 * to its Honk), the click list, the four flags, the buddy and the pet (one per player, never lost), the soak rule, the daily
 * burger, the contagious laugh, the grill with and without the recipe hook, the ride on David Hasselguh, the
 * Knabbelburgermobiel (driving, refusing an edge, only for whom it was made), the blocks and the clothes.
 * The rooms are written by tools/features/einde2_sponsguh_wezens.py (test_kamers).
 */
public class E2SponsguhGameTests {
    private static final String VLOER = "einde2_sponsguh_wezens_test_vloer", WEG = "einde2_sponsguh_wezens_test_weg";
    private static final String BATCH = "einde2_sponsguh_wezens";
    /** The raised road of the room WEG (template coordinates; the same numbers as the python module). */
    private static final int WEG_Y = 5, WEG_X0 = 2, WEG_X1 = 12, WEG_Z0 = 5, WEG_Z1 = 9;

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, z + 0.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            Maatjes.vergeet(player, "sponsguh");
            Maatjes.vergeet(player, "gerrit");
            Knabbelburgermobiel.ruimOp(player);
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends SponsguhWezen> T maak(GameTestHelper helper, Cast cast, int x, int z) {
        return helper.spawn((EntityType<T>) E2SponsguhFeature.type(cast), new BlockPos(x, 2, z));
    }

    private static JsonObject bron(String pad) {
        try (InputStream in = E2SponsguhGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static boolean bestaat(String pad) {
        try (InputStream in = E2SponsguhGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /** Every character: its type, its hitbox, its model, textures and animations, its spawn egg, and that nothing hurts it. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2SponsguhCast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int n = 0;
        for (Cast cast : Cast.values()) {
            SponsguhWezen wezen = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.COMMAND);
            helper.assertTrue(wezen != null && wezen.cast() == cast, cast + " can be made and knows who it is");
            helper.assertTrue(Math.abs(wezen.getBbWidth() - cast.breed) < 1e-3 && Math.abs(wezen.getBbHeight() - cast.hoog) < 1e-3, cast + ": the hitbox of the table");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id(cast.id() + "_spawn_egg")), cast + " has a spawn egg");
            helper.assertTrue(bestaat("/assets/guhs/geckolib/models/entity/" + cast.model() + ".geo.json"), cast + ": its model");
            JsonObject anims = bron("/assets/guhs/geckolib/animations/entity/" + cast.model() + ".animation.json");
            helper.assertTrue(anims != null, cast + ": its animation file");
            for (String naam : cast.animaties()) {
                helper.assertTrue(anims.getAsJsonObject("animations").has(naam), cast + ": the animation " + naam);
            }
            for (int soort = 0; soort < Math.max(1, cast.soorten); soort++) {
                String tex = cast.soorten > 0 ? cast.id() + "_" + soort : cast.id();
                helper.assertTrue(bestaat("/assets/guhs/textures/entity/" + tex + ".png"), cast + ": the texture " + tex);
                helper.assertTrue(cast != Cast.LOOPGRAAFMONSTER || bestaat("/assets/guhs/textures/entity/" + tex + "_glowmask.png"), "the monster's eyes glow: " + tex);
            }
            // the animation phase B sets: a pose stays, a one-shot plays on top of it, an unknown name does nothing
            wezen.zetAnimatie("geen_animatie");
            helper.assertTrue(wezen.animatie().equals("idle"), cast + ": an unknown animation is ignored");
            for (String naam : cast.animaties()) {
                if (naam.startsWith("vlag_") || naam.startsWith("soort_")) {
                    helper.assertTrue(!cast.heeft(naam), cast + ": " + naam + " is not for zetAnimatie");
                    continue;
                }
                wezen.zetAnimatie(naam);
                helper.assertTrue(wezen.animatie().equals(naam), cast + " plays " + naam + ": " + wezen.animatie());
                helper.assertTrue(Cast.DOORLOPEND.contains(naam) == wezen.houding().equals(naam) || naam.equals("idle"), cast + ": " + naam + " lasts only when it is a pose");
                wezen.zetAnimatie("idle");
            }
            n++;
        }
        helper.assertTrue(n == 20, "the cast is twenty: " + n);
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(Guhs.id("knabbelburgermobiel_spawn_egg")) && !BuiltInRegistries.ITEM.containsKey(Guhs.id("knabbelburgermobiel")),
                "the Knabbelburgermobiel has no item at all");
        helper.assertTrue(bron("/assets/guhs/geckolib/animations/entity/knabbelburgermobiel.animation.json").getAsJsonObject("animations").has("rij"), "the car's animations");
        // in the world: can't be hurt, can't be pushed, gets a look of its own, never despawns
        SponsguhWezen patguh = maak(helper, Cast.PATGUH, 3, 3);
        SponsguhWezen monster = maak(helper, Cast.LOOPGRAAFMONSTER, 8, 8);
        float leven = patguh.getHealth();
        patguh.hurtServer(level, level.damageSources().generic(), 100f);
        patguh.hurtServer(level, level.damageSources().lava(), 100f);
        helper.assertTrue(patguh.isAlive() && patguh.getHealth() == leven && !patguh.isPushable() && !patguh.removeWhenFarAway(999), "lief and safe");
        helper.assertTrue(patguh.isMika() == false && E2SponsguhFeature.type(Cast.OCTOLIAM).create(level, EntitySpawnReason.COMMAND).isMika(), "guh and Mika know what they are");
        helper.assertTrue(GuhDex.EXTRA.contains(GuhVariant.SPONSGUH) && GuhDex.EXTRA.contains(GuhVariant.GERRIT), "the two pages are bonus pages");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(monster.soort() >= 0 && monster.soort() < 3 && patguh.soort() == 0, "a look of its own: " + monster.soort());
            patguh.zetAnimatie("lach");
            helper.assertTrue(patguh.animatie().equals("lach"), "the one-shot plays");
        });
        helper.runAfterDelay(3 + Cast.duur("lach") + 2, () -> {
            helper.assertTrue(patguh.animatie().equals("idle"), "and then it is over: " + patguh.animatie());
            helper.succeed();
        });
    }

    /** What a building's template writes: Honk (it keeps to its spot), Soort and the flags, read from NBT. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2SponsguhUitTemplate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        record Geval(String id, String nbt) {
        }
        List<Geval> gevallen = List.of(new Geval("bikinibodem_guh", "Honk:6,Soort:4"), new Geval("loopgraafmonster", "Soort:2"),
                new Geval("meneer_krabguh", "Bevroren:1b"), new Geval("neptuguh", "Kroon:0b"), new Geval("neptuguh", ""), new Geval("juf_pufguh", "Opgeblazen:1b"),
                new Geval("dennis_mika", "OpMotor:1b,Stil:1b"));
        SponsguhWezen[] wezens = new SponsguhWezen[gevallen.size()];
        for (int i = 0; i < wezens.length; i++) {
            CompoundTag tag;
            try {
                tag = net.minecraft.nbt.TagParser.parseCompoundFully("{id:\"guhs:" + gevallen.get(i).id() + "\"," + gevallen.get(i).nbt() + "}");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalStateException(e);
            }
            Vec3 at = helper.absoluteVec(new Vec3(2.5 + 1.8 * i, 2, 4.5));
            Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.STRUCTURE, en -> {
                en.snapTo(at.x, at.y, at.z, 0, 0);
                return en;
            });
            helper.assertTrue(e instanceof SponsguhWezen, gevallen.get(i).id() + " loads from NBT");
            level.addFreshEntity(e);
            wezens[i] = (SponsguhWezen) e;
        }
        helper.assertTrue(wezens[0].soort() == 4 && wezens[1].soort() == 2, "Soort from NBT");
        helper.assertTrue(((MeneerKrabguhEntity) wezens[2]).isBevroren() && !((NeptuguhEntity) wezens[3]).heeftKroon() && ((NeptuguhEntity) wezens[4]).heeftKroon(),
                "Bevroren, Kroon (on by default)");
        helper.assertTrue(((JufPufguhEntity) wezens[5]).isOpgeblazen() && ((DennisMikaEntity) wezens[6]).isOpMotor() && wezens[6].stil(), "Opgeblazen, OpMotor, Stil");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(wezens[0].honk() == 6 && wezens[0].blockPosition().equals(wezens[0].thuisplek()), "its Honk: home is where the template put it");
            helper.assertTrue(wezens[1].thuisplek() == null, "no Honk, no home");
            helper.succeed();
        });
    }

    /** BIJ_KLIK: the first listener that says true takes the click; without one the character says its fixed line. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2SponsguhKlik(GameTestHelper helper) {
        ServerPlayer player = speler(helper, 5, 5);
        SponsguhWezen sandy = maak(helper, Cast.SANDY_GUH, 6, 5);
        int[] gehoord = {0, 0};
        BiFunction<SponsguhWezen, ServerPlayer, Boolean> nee = (w, p) -> {
            if (w == sandy) {
                gehoord[0]++;
            }
            return false;
        };
        BiFunction<SponsguhWezen, ServerPlayer, Boolean> ja = (w, p) -> {
            if (w == sandy && p == player) {
                gehoord[1]++;
                return true;
            }
            return false;
        };
        sandy.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(sandy.animatie().equals("praat"), "no listener: the fixed line with the talking animation");
        SponsguhWezen.BIJ_KLIK.add(nee);
        SponsguhWezen.BIJ_KLIK.add(ja);
        helper.runAfterDelay(30, () -> {
            try {
                sandy.zetAnimatie("idle");
                sandy.mobInteract(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(gehoord[0] == 1 && gehoord[1] == 1, "both listeners were asked, in order: " + gehoord[0] + ", " + gehoord[1]);
                helper.assertTrue(!sandy.animatie().equals("praat"), "the listener took the click: no fixed line");
            } finally {
                SponsguhWezen.BIJ_KLIK.remove(nee);
                SponsguhWezen.BIJ_KLIK.remove(ja);
                weg(helper, player);
            }
            helper.succeed();
        });
    }

    /** The four flags: Opgeblazen (twice the size), Kroon, OpMotor, Bevroren (no animation, no walking). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2SponsguhVlaggen(GameTestHelper helper) {
        JufPufguhEntity juf = maak(helper, Cast.JUF_PUFGUH, 3, 3);
        NeptuguhEntity neptuguh = maak(helper, Cast.NEPTUGUH, 7, 3);
        DennisMikaEntity dennis = maak(helper, Cast.DENNIS_MIKA, 11, 3);
        MeneerKrabguhEntity krab = maak(helper, Cast.MENEER_KRABGUH, 3, 9);
        float breed = juf.getBbWidth();
        juf.setOpgeblazen(true);
        helper.assertTrue(juf.isOpgeblazen() && Math.abs(juf.getBbWidth() - 2 * breed) < 1e-3 && !juf.magLopen(), "blown up: twice the size, she stays put");
        juf.setOpgeblazen(false);
        helper.assertTrue(!juf.isOpgeblazen() && Math.abs(juf.getBbWidth() - breed) < 1e-3, "and small again");
        helper.assertTrue(neptuguh.heeftKroon(), "Neptuguh wears his crown");
        neptuguh.setKroon(false);
        helper.assertTrue(!neptuguh.heeftKroon(), "until somebody takes it");
        dennis.setOpMotor(true);
        helper.assertTrue(dennis.isOpMotor() && dennis.beweging(true).equals("rij") && dennis.beweging(false).equals("idle"), "on the bike he rides");
        dennis.setOpMotor(false);
        helper.assertTrue(dennis.beweging(true).equals("loop"), "off it he walks");
        krab.setBevroren(true);
        krab.zetAnimatie("tel_geld");
        helper.assertTrue(krab.isBevroren() && krab.animatie().equals("idle") && !krab.magLopen(), "frozen: no animation, no walking");
        krab.setBevroren(false);
        krab.zetAnimatie("tel_geld");
        helper.assertTrue(krab.animatie().equals("tel_geld") && !krab.magLopen(), "thawed he counts his knabbels (and stands still for it)");
        helper.succeed();
    }

    /** The buddy and the pet: one per player, the same one comes again, a new one after it died, each player their own. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2SponsguhMaatje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer anna = speler(helper, 4, 4), bas = speler(helper, 10, 10);
        helper.assertTrue(!Sponsguh.heeft(anna) && Sponsguh.roep(anna) == null && !Gerrit.heeft(anna) && Gerrit.roep(anna) == null, "nobody has one at first");
        SponsguhEntity eerste = Sponsguh.geef(anna);
        helper.assertTrue(eerste != null && Sponsguh.heeft(anna) && eerste.isVan(anna) && !Sponsguh.heeft(bas), "given: hers, not his");
        helper.assertTrue(eerste.distanceToSqr(anna) < 9, "it stands next to her");
        helper.assertTrue(nl.juiced.guhs.world.GuhWorldData.get(level.getServer()).player(anna.getUUID()).seen.contains(GuhVariant.SPONSGUH), "its page fills");
        helper.assertTrue(Sponsguh.geef(anna) == eerste && Sponsguh.roep(anna) == eerste, "given again: the same one comes");
        SponsguhEntity vanBas = Sponsguh.geef(bas);
        helper.assertTrue(vanBas != null && vanBas != eerste && vanBas.isVan(bas), "every player their own");
        Sponsguh.zetThuis(anna, true);
        helper.assertTrue(eerste.thuis() && Maatjes.thuis(anna, "sponsguh"), "told to stay");
        helper.assertTrue(Sponsguh.roep(anna) == eerste && !eerste.thuis(), "called: it follows again");
        GerritEntity gerrit = Gerrit.geef(anna);
        helper.assertTrue(gerrit != null && Gerrit.heeft(anna) && Gerrit.geef(anna) == gerrit && gerrit.isVan(anna), "Gerrit: one, hers");
        // "death": the entity is gone; roep makes a new one, and a stale copy tidies itself up
        eerste.discard();
        SponsguhEntity nieuw = Sponsguh.roep(anna);
        helper.assertTrue(nieuw != null && nieuw != eerste && nieuw.isVan(anna) && nieuw.isAlive(), "never lost: a new one comes");
        SponsguhEntity kopie = helper.spawn(E2SponsguhFeature.SPONSGUH.get(), new BlockPos(12, 2, 3));
        kopie.zetEigenaar(anna.getUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(kopie.isRemoved(), "a copy that is not the one of her record takes itself away");
            long n = level.getEntitiesOfClass(SponsguhEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40), e -> e.isVan(anna)).size();
            helper.assertTrue(n == 1 && nieuw.isAlive() && vanBas.isAlive(), "exactly one each: " + n);
            weg(helper, anna, bas);
        });
    }

    /** The soak rule: a flowing block yes, a single source yes, a pond no, a protected place no; and the buddy soaks ONE block. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2SponsguhZuigRegel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 7, 7);
        BlockState water = Blocks.WATER.defaultBlockState(), saus = ModBlocks.KAAS_SAUS.get().defaultBlockState();
        // every puddle in its own little stone tub, so nothing flows during the test
        BlockPos los = new BlockPos(2, 2, 2), stromend = new BlockPos(5, 2, 2), sausje = new BlockPos(8, 2, 2), beschermd = new BlockPos(11, 2, 2);
        BlockPos[] vijver = {new BlockPos(2, 2, 11), new BlockPos(3, 2, 11), new BlockPos(2, 2, 12), new BlockPos(3, 2, 12)};
        for (BlockPos p : new BlockPos[] {los, stromend, sausje, beschermd}) {
            for (BlockPos rond : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 0, 1))) {
                helper.setBlock(rond, Blocks.STONE);
            }
        }
        for (BlockPos rond : BlockPos.betweenClosed(new BlockPos(1, 2, 10), new BlockPos(4, 2, 13))) {
            helper.setBlock(rond, Blocks.STONE);
        }
        helper.setBlock(los, water);
        helper.setBlock(stromend, water.setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(sausje, saus);
        helper.setBlock(beschermd, water);
        for (BlockPos p : vijver) {
            helper.setBlock(p, water);
        }
        BoundingBox doos = BoundingBox.fromCorners(helper.absolutePos(beschermd.offset(-1, -1, -1)), helper.absolutePos(beschermd.offset(1, 1, 1)));
        Einde2Bescherming.TEST_AREAS.add(doos);
        try {
            helper.assertTrue(SponsguhEntity.magOpzuigen(level, helper.absolutePos(los), player), "a single source block: a spill");
            helper.assertTrue(SponsguhEntity.magOpzuigen(level, helper.absolutePos(stromend), player), "a flowing block: a spill");
            helper.assertTrue(SponsguhEntity.magOpzuigen(level, helper.absolutePos(sausje), player), "kaassaus too");
            for (BlockPos p : vijver) {
                helper.assertTrue(!SponsguhEntity.magOpzuigen(level, helper.absolutePos(p), player), "a pond is not a spill: " + p);
            }
            helper.assertTrue(!SponsguhEntity.magOpzuigen(level, helper.absolutePos(beschermd), player), "never in a protected place");
            helper.assertTrue(!SponsguhEntity.magOpzuigen(level, helper.absolutePos(new BlockPos(7, 2, 7)), player)
                    && !SponsguhEntity.magOpzuigen(level, helper.absolutePos(new BlockPos(2, 1, 2)), player), "air and stone are no spill");
            // the buddy next to the sauce tub and the flowing block: ONE block goes, the source first
            SponsguhEntity buddy = helper.spawn(E2SponsguhFeature.SPONSGUH.get(), new BlockPos(7, 3, 3));
            helper.assertTrue(buddy.zuig(player), "it soaks");
            helper.assertTrue(helper.getBlockState(sausje).isAir() && !helper.getBlockState(stromend).isAir(), "one block, the single source first");
            helper.assertTrue(buddy.animatie().equals("zuig"), "with its soak animation");
            helper.assertTrue(buddy.zuig(player) && helper.getBlockState(stromend).isAir(), "then the flowing one");
            helper.assertTrue(helper.getBlockState(vijver[0]).getFluidState().isSource() && helper.getBlockState(beschermd).getFluidState().isSource(),
                    "the pond and the protected puddle stay");
        } finally {
            Einde2Bescherming.TEST_AREAS.remove(doos);
            weg(helper, player);
        }
        helper.succeed();
    }

    /** The buddy bakes one Knabbelburger per Minecraft day for its owner, and its laugh makes the guhs nearby hop. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void einde2SponsguhBurgerEnLach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 4, 4);
        SponsguhEntity buddy = Sponsguh.geef(player);
        helper.assertTrue(buddy != null, "a buddy");
        buddy.zetStil(true);
        GuhEntity dichtbij = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 2, 6));
        helper.spawn(ModEntities.GUH.get(), new BlockPos(14, 2, 14));
        Vec3 ver = helper.absoluteVec(new Vec3(14.5, 2, 14.5));
        helper.assertTrue(buddy.position().distanceTo(ver) > SponsguhEntity.LACH_BEREIK && buddy.distanceTo(dichtbij) < SponsguhEntity.LACH_BEREIK, "one guh near, one far");
        int[] lachers = {-1};
        helper.runAfterDelay(5, () -> lachers[0] = buddy.lach());
        long vandaag = GuhTime.day(level);
        buddy.zetBurgerDag(vandaag - 1);                                     // (as if it last baked yesterday)
        int[] klaarOp = {-1};
        helper.onEachTick(() -> {
            int n = player.getInventory().countItem(E2SponsguhFeature.KNABBELBURGER.get());
            if (n >= 1 && klaarOp[0] < 0) {
                klaarOp[0] = (int) helper.getTick();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lachers[0] == 1, "the laugh reached the one guh within reach: " + lachers[0]);
            helper.assertTrue(klaarOp[0] >= 0, "today's burger");
            helper.assertTrue(helper.getTick() > klaarOp[0] + 120, "wait and see that there is no second one");
            int n = player.getInventory().countItem(E2SponsguhFeature.KNABBELBURGER.get());
            helper.assertTrue(n == 1 && buddy.burgerDag() == vandaag && !buddy.bakt(), "one burger a day: " + n);
            weg(helper, player);
        });
    }

    /** The grill: nothing for who does not know the recipe, one burger in five seconds for who does. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 300)
    public static void einde2SponsguhGrill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 5, 5);
        BlockPos grill = new BlockPos(7, 2, 5);
        helper.setBlock(grill, E2SponsguhFeature.KROKANTE_GRILL.get());
        BlockPos abs = helper.absolutePos(grill);
        helper.assertTrue(!SponsguhBlokken.GrillBlock.bak(level.getBlockState(abs), level, abs, player), "empty pockets: nothing to bake");
        player.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        player.getInventory().add(new ItemStack(Items.BREAD, 2));
        helper.assertTrue(!Einde2Haken.KNABBELBURGER_RECEPT.test(player), "by default nobody knows the recipe");
        helper.assertTrue(!SponsguhBlokken.GrillBlock.bak(level.getBlockState(abs), level, abs, player), "the recipe is secret");
        helper.assertTrue(player.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == 2 && player.getInventory().countItem(Items.BREAD) == 2
                && !level.getBlockState(abs).getValue(SponsguhBlokken.GrillBlock.BAKT), "and it costs nothing");
        Predicate<Player> oud = Einde2Haken.KNABBELBURGER_RECEPT;
        java.util.UUID wie = player.getUUID();
        Einde2Haken.KNABBELBURGER_RECEPT = p -> p.getUUID().equals(wie) || oud.test(p);
        boolean begonnen;
        try {
            begonnen = SponsguhBlokken.GrillBlock.bak(level.getBlockState(abs), level, abs, player);
        } finally {
            Einde2Haken.KNABBELBURGER_RECEPT = oud;
        }
        helper.assertTrue(begonnen && level.getBlockState(abs).getValue(SponsguhBlokken.GrillBlock.BAKT), "with the recipe it bakes");
        helper.assertTrue(player.getInventory().countItem(ModItems.KAAS_KNABBELS.get()) == 1 && player.getInventory().countItem(Items.BREAD) == 1, "one knabbel and one bread");
        helper.assertTrue(!SponsguhBlokken.GrillBlock.bak(level.getBlockState(abs), level, abs, player), "one at a time");
        long start = helper.getTick();
        helper.succeedWhen(() -> {
            List<ItemEntity> burgers = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2),
                    i -> i.getItem().is(E2SponsguhFeature.KNABBELBURGER.get()));
            helper.assertTrue(burgers.size() == 1 && burgers.get(0).getItem().getCount() == 1, "one Knabbelburger pops out");
            helper.assertTrue(!level.getBlockState(abs).getValue(SponsguhBlokken.GrillBlock.BAKT) && helper.getTick() - start >= SponsguhBlokken.GrillBlock.BAKTIJD - 2,
                    "after about five seconds");
            burgers.forEach(Entity::discard);
            weg(helper, player);
        });
    }

    /** David Hasselguh carries a rider to the goal: nobody gets off on the way, the story hears the landing. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void einde2SponsguhRit(GameTestHelper helper) {
        ServerPlayer player = speler(helper, 2, 2);
        Vec3 start = player.position();
        Vec3 doel = helper.absoluteVec(new Vec3(12.5, 2, 12.5));
        boolean[] aangekomen = {false};
        Consumer<ServerPlayer> hoor = p -> aangekomen[0] |= p == player;
        Hasselguh.AANGEKOMEN.add(hoor);
        helper.assertTrue(!Hasselguh.draagt(player), "not riding yet");
        helper.assertTrue(Hasselguh.draag(player, doel), "a Hasselguh comes and takes the player");
        helper.assertTrue(Hasselguh.draagt(player) && player.getVehicle() instanceof DavidHasselguhEntity, "on his back");
        DavidHasselguhEntity david = (DavidHasselguhEntity) player.getVehicle();
        helper.assertTrue(!Hasselguh.draag(player, doel), "one ride at a time");
        SponsguhWezen patguh = maak(helper, Cast.PATGUH, 3, 3);
        helper.assertTrue(david.zetMee(patguh) && patguh.getVehicle() == david, "the cast rides along");
        helper.runAfterDelay(8, () -> {
            player.stopRiding();
            helper.assertTrue(player.getVehicle() == david && Hasselguh.draagt(player), "no getting off on the way");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!Hasselguh.draagt(player) && player.getVehicle() == null, "set down");
            helper.assertTrue(aangekomen[0], "the story heard the landing");
            helper.assertTrue(player.position().distanceTo(doel) < 1.0 && player.position().distanceTo(start) > 8, "at the goal: " + player.position());
            helper.assertTrue(patguh.getVehicle() == null && patguh.position().distanceTo(doel) < 3, "the cast got off there too");
            Hasselguh.AANGEKOMEN.remove(hoor);
            weg(helper, player);
        });
    }

    /** A ride that is broken off puts the player back where it began. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2SponsguhRitStop(GameTestHelper helper) {
        ServerPlayer player = speler(helper, 2, 2);
        Vec3 start = player.position();
        DavidHasselguhEntity wachtend = maak(helper, Cast.DAVID_HASSELGUH, 4, 4);
        helper.assertTrue(Hasselguh.draag(player, helper.absoluteVec(new Vec3(12.5, 4, 12.5))) && player.getVehicle() == wachtend,
                "the Hasselguh that waits nearby is the one that carries");
        boolean[] gestopt = {false};
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(player.position().distanceTo(start) > 0.5, "under way");
            Hasselguh.stop(player);
            gestopt[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gestopt[0] && !Hasselguh.draagt(player) && player.getVehicle() == null, "stopped");
            helper.assertTrue(wachtend.isAlive() && !wachtend.onderweg(), "he stays: he was not made for this ride");
            helper.assertTrue(player.position().distanceTo(start) < 0.5, "back where it began: " + player.position());
            weg(helper, player);
        });
    }

    /** The Knabbelburgermobiel: only for whom it was made, it drives, it refuses the edge of the road, and it is tidied up. */
    @GuhTest(template = WEG, batch = BATCH, timeoutTicks = 500)
    public static void einde2SponsguhMobiel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer rijder = speler(helper, 1, 1), ander = speler(helper, 1, 3);
        double z = (WEG_Z0 + WEG_Z1 + 1) / 2.0;
        Vec3 plek = helper.absoluteVec(new Vec3(WEG_X0 + 2.0, WEG_Y + 2, z)), einde = helper.absoluteVec(new Vec3(WEG_X1 + 1.0, WEG_Y + 2, z));
        float yaw = (float) (Mth.atan2(einde.z - plek.z, einde.x - plek.x) * Mth.RAD_TO_DEG) - 90f;
        KnabbelburgermobielEntity mobiel = Knabbelburgermobiel.zet(level, plek, yaw, rijder);
        helper.assertTrue(mobiel != null && Knabbelburgermobiel.van(rijder) == mobiel && rijder.getUUID().equals(mobiel.voor()), "made for the driver");
        helper.assertTrue(mobiel.magRijden(rijder) && !mobiel.magRijden(ander), "only for whom it was made");
        helper.assertTrue(!mobiel.stapIn(ander) && mobiel.getPassengers().isEmpty(), "somebody else can't take an empty car");
        helper.assertTrue(mobiel.stapIn(rijder) && mobiel.bestuurder() == rijder, "its player takes the wheel");
        helper.assertTrue(mobiel.stapIn(ander) && mobiel.bestuurder() == rijder && mobiel.getPassengers().size() == 2, "and then others ride along");
        SponsguhWezen sponsguh = maak(helper, Cast.SPONSGUH, 3, 3);
        helper.assertTrue(mobiel.zetMee(sponsguh) && !mobiel.zetMee(maak(helper, Cast.PATGUH, 4, 3)), "three seats: the cast rides too, then it is full");
        KnabbelburgermobielEntity tweede = Knabbelburgermobiel.zet(level, helper.absoluteVec(new Vec3(3.5, 2, 12.5)), 0, null);
        helper.assertTrue(tweede != null && tweede.magRijden(ander) && tweede.voor() == null, "a car for nobody in particular: anybody drives");
        tweede.discard();
        mobiel.proefInvoer = new float[] {1f, 0f};                           // the driver holds "forwards"
        double rand = einde.distanceTo(plek);
        boolean[] reed = {false};
        helper.onEachTick(() -> reed[0] |= mobiel.rijdt());
        helper.succeedWhen(() -> {
            double gereden = mobiel.position().multiply(1, 0, 1).distanceTo(plek.multiply(1, 0, 1));
            helper.assertTrue(reed[0] && gereden > rand - 3.0, "it drove along the road: " + gereden + " of " + rand);
            helper.assertTrue(mobiel.snelheid() == 0 && !mobiel.rijdt(), "and stopped");
            helper.assertTrue(gereden < rand - 0.3 && mobiel.onGround() && Math.abs(mobiel.getY() - plek.y) < 1.2, "before the edge, still on the road: " + mobiel.position());
            helper.assertTrue(helper.getTick() > 150, "and it stays there with the pedal down");
            helper.assertTrue(!KnabbelburgermobielEntity.heeftGrond(level, einde.add(einde.subtract(plek).normalize().scale(1.5))), "beyond the road there is no ground");
            Knabbelburgermobiel.ruimOp(rijder);
            helper.assertTrue(mobiel.isRemoved() && Knabbelburgermobiel.van(rijder) == null && rijder.getVehicle() == null, "tidied up: gone, everybody out");
            sponsguh.discard();
            weg(helper, rijder, ander);
        });
    }

    /** The blocks: Karen's face, the bell, the bubble blower (a guh comes to play), the lantern's light, the door, the food. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void einde2SponsguhBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 2, 2);
        BlockPos karen = new BlockPos(3, 2, 6), tafel = new BlockPos(5, 2, 6), blazer = new BlockPos(10, 2, 10);
        helper.setBlock(karen, E2SponsguhFeature.KAREN.get());
        helper.setBlock(tafel, E2SponsguhFeature.BESTELTAFEL.get());
        helper.setBlock(blazer, E2SponsguhFeature.BELLENBLAAS.get());
        BlockPos absKaren = helper.absolutePos(karen), absTafel = helper.absolutePos(tafel), absBlazer = helper.absolutePos(blazer);
        helper.assertTrue(Karen.gezicht(level, absKaren) == Karen.NEUTRAAL, "Karen starts neutral");
        Karen.zetGezicht(level, absKaren, Karen.BOOS);
        helper.assertTrue(Karen.gezicht(level, absKaren) == Karen.BOOS, "her face changes");
        Karen.zetGezicht(level, absKaren, 99);
        helper.assertTrue(Karen.gezicht(level, absKaren) == Karen.ZUCHT, "clamped to her four faces");
        Karen.zetGezicht(level, absTafel, Karen.BLIJ);
        helper.assertTrue(Karen.gezicht(level, absTafel) == -1 && level.getBlockState(absTafel).is(E2SponsguhFeature.BESTELTAFEL.get()), "no Karen there: nothing happens");
        BlockPos[] gebeld = {null};
        BiConsumer<ServerPlayer, BlockPos> hoor = (p, pos) -> gebeld[0] = p == player ? pos : null;
        Besteltafel.BIJ_BEL.add(hoor);
        try {
            Besteltafel.bel(level, absTafel, player);
        } finally {
            Besteltafel.BIJ_BEL.remove(hoor);
        }
        helper.assertTrue(absTafel.equals(gebeld[0]), "the bell tells who rang which table");
        helper.assertTrue(E2SponsguhFeature.SPOOKLANTAARN.get().defaultBlockState().getLightEmission() == 12, "the ghost lantern: light 12");
        helper.assertTrue(E2SponsguhFeature.VRUCHTHUIS_DEUR.get() instanceof DoorBlock, "a real door");
        FoodProperties burger = new ItemStack(E2SponsguhFeature.KNABBELBURGER.get()).get(DataComponents.FOOD);
        FoodProperties jam = new ItemStack(E2SponsguhFeature.KWALLENJAM.get()).get(DataComponents.FOOD);
        helper.assertTrue(burger != null && burger.nutrition() == 7 && jam != null && jam.nutrition() == 3, "Knabbelburger 7, kwallenjam 3");
        for (var blok : E2SponsguhFeature.BLOKKEN) {
            helper.assertTrue(blok.get().asItem() != Items.AIR, blok.getId() + " has its item");
        }
        // the toy: a guh finds the blower and comes to chase the bubbles
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 10));
        KlusTaak[] spel = {null};
        helper.onEachTick(() -> {
            if (spel[0] == null) {
                spel[0] = BellenSpel.SOORT.zoek(level, guh, guh.blockPosition(), 12);     // (the blower's point of interest needs a tick)
            } else {
                spel[0].tick();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(spel[0] instanceof BellenSpel b && b.blazer().equals(absBlazer), "the guh found the bubble blower");
            helper.assertTrue(((BellenSpel) spel[0]).speelt() && guh.blockPosition().distManhattan(absBlazer) <= 5, "and plays at it");
            helper.assertTrue(BellenSpel.SOORT.tel(level, absBlazer, 12, p -> true).get(0).aantal() == 1, "the huisje overview counts it");
            weg(helper, player);
        });
    }

    /** The seven clothes: all from the one source of the story, each with its item. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void einde2SponsguhKleding(GameTestHelper helper) {
        helper.assertTrue(E2SponsguhFeature.KLEDING.size() == 7, "seven pieces");
        for (GuhClothes stuk : E2SponsguhFeature.KLEDING) {
            helper.assertTrue(E2SponsguhFeature.KLEDINGBRON.equals(KledingBronnen.bron(stuk)), stuk + " comes from the Sponsguh story");
            helper.assertTrue(ModItems.CLOTHES.get(stuk) != null && stuk.id().startsWith("sponsguh_"), stuk + " has its item");
        }
        helper.assertTrue(KledingBronnen.van(E2SponsguhFeature.KLEDINGBRON).equals(E2SponsguhFeature.KLEDING), "and nothing else does");
        helper.assertTrue(GuhClothes.SPONSGUH_VIERKANTE_BROEK.slot == GuhClothes.Slot.BODY && GuhClothes.SPONSGUH_ANTENNES.slot == GuhClothes.Slot.OREN
                && GuhClothes.SPONSGUH_HELDENCAPE.slot == GuhClothes.Slot.BACK && GuhClothes.SPONSGUH_LUCHTHELM.slot == GuhClothes.Slot.HEAD, "their slots");
        helper.succeed();
    }
}
