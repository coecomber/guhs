package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the vetbootje slice (batch einde2_vetbootje, room einde2_vetbootje_test_kamer: a stone floor of 15 x 15):
 * one boat per player and an idempotent grant, the recall (the same body when it is loaded, a new one with the same looks
 * and chest when it is lost, an old body taking itself away), the looks through the API and through the werf's answer,
 * the chest in the fleet book and its saving, sailing (slow, hovering, never falling), guhs hopping aboard and off, and the
 * balloon: given once, rising with the burner, sinking slowly, drifting on the wind, landing, folding, an old copy going lek.
 */
public class E2VetbootjeGameTests {
    private static final String KAMER = "einde2_vetbootje_test_kamer";
    private static final String BATCH = "einde2_vetbootje";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return p;
    }

    /** Takes the players and everything of this slice in (and above) the room away again; the rooms of the other tests stand close by. */
    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        ServerLevel level = helper.getLevel();
        for (Entity e : level.getEntities((Entity) null, helper.getBounds().inflate(1, 40, 1), e -> e instanceof VetbootjeEntity || e instanceof HeteluchtballonEntity)) {
            e.ejectPassengers();
            e.discard();
        }
        for (ServerPlayer p : players) {
            p.stopRiding();
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int aantal(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static List<VetbootjeEntity> boten(GameTestHelper helper, ServerPlayer van) {
        return helper.getLevel().getEntitiesOfClass(VetbootjeEntity.class, helper.getBounds().inflate(24), b -> b.isAlive() && b.isVan(van));
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer eigenaar, int x, int z) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(x, 2, z));
        guh.tame(eigenaar);
        guh.setGuhScale(1f);                                 // (a random vahoege guh would be too vads for a cushion)
        guh.setWandering(false);
        guh.setTeleportEnabled(false);
        return guh;
    }

    /** One vetbootje per player: the grant is idempotent, the fluitje and the werfboekje come once, everybody has their own. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2VetbootjeEenPerSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 7, 4), q = speler(helper, 4, 9);
        helper.assertFalse(Vetbootje.heeft(p), "nobody starts with a vetbootje");
        helper.assertTrue(Vetbootje.roep(p) == null, "no boat, nothing to call");
        VetbootjeEntity a = Vetbootje.geef(p);
        helper.assertTrue(a != null && a.isAlive() && a.isVan(p) && Vetbootje.heeft(p), "the grant gives a boat that is the player's");
        helper.assertTrue(aantal(p, E2VetbootjeFeature.VETBOOTJE_FLUITJE.get()) == 1 && aantal(p, E2VetbootjeFeature.VETBOOTJE_WERFBOEKJE.get()) == 1,
                "the fluitje and the werfboekje come with it");
        helper.assertTrue(advancement(p, Vetbootje.GEKREGEN), "the hidden advancement is granted");
        helper.assertTrue(a.kleur() == Vetbootje.STANDAARD_KLEUR && a.zeil() == Zeil.MIKA, "a new boat: bruin with the Mika sail");
        helper.assertTrue(a.naam().getContents() instanceof TranslatableContents t && t.getKey().equals("gui.guhs.einde2_vetbootje.naam.standaard"),
                "a new boat has the default name");
        VetbootjeEntity b = Vetbootje.geef(p);
        helper.assertTrue(b == a, "a second grant brings the same boat");
        helper.assertTrue(aantal(p, E2VetbootjeFeature.VETBOOTJE_FLUITJE.get()) == 1 && boten(helper, p).size() == 1, "and no second fluitje or boat");
        helper.assertFalse(Vetbootje.heeft(q), "another player has none of it");
        VetbootjeEntity c = Vetbootje.geef(q);
        helper.assertTrue(c != a && c.isVan(q) && !c.isVan(p) && !a.magSturen(q) && a.magSturen(p), "every player their own boat");
        helper.assertTrue(boten(helper, p).size() == 1 && boten(helper, q).size() == 1, "one each");
        weg(helper, p, q);
        helper.succeed();
    }

    /**
     * The recall: a loaded boat glides over (the same body), a lost one is made again with a new number, and an old body
     * that turns up again takes itself away.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2VetbootjeRoep(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 7, 3);
        VetbootjeEntity a = Vetbootje.geef(p);
        BlockPos ver = helper.absolutePos(new BlockPos(2, 9, 13));
        a.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5, 90, 0);
        VetbootjeEntity b = Vetbootje.roep(p);
        helper.assertTrue(b == a && a.distanceTo(p) < 6, "the loaded boat comes itself: " + a.distanceTo(p));
        int nr = a.nr;
        a.discard();                                         // lost (unloaded far away, gone)
        VetbootjeEntity c = Vetbootje.roep(p);
        helper.assertTrue(c != null && c != a && c.isAlive() && c.isVan(p) && c.nr == nr + 1, "a lost boat is made again, with the next number");
        helper.assertTrue(boten(helper, p).size() == 1, "still one boat");
        // the old body is loaded again somewhere: it knows it was replaced
        VetbootjeEntity oud = E2VetbootjeFeature.VETBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        oud.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5, 0, 0);
        oud.zetEigenaar(p.getUUID());
        oud.nr = nr;
        level.addFreshEntity(oud);
        // and a boat of nobody stays
        VetbootjeEntity leen = Vetbootje.leen(level, new Vec3(ver.getX() + 4.5, ver.getY(), ver.getZ() - 6.5), 0, DyeColor.LIME, Zeil.STER, null);
        helper.succeedWhen(() -> {
            helper.assertTrue(oud.isRemoved(), "the replaced body takes itself away");
            helper.assertTrue(c.isAlive() && leen != null && leen.isAlive() && leen.eigenaar() == null, "the real one and the leenbootje stay");
            helper.assertTrue(Vetbootje.geladen(p) == c, "the book knows the body that counts");
            weg(helper, p);
        });
    }

    /** The looks: through the API, kept on a new body, and through the werf screen's answer (checked and cleaned). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2VetbootjeWerf(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 7, 4), q = speler(helper, 3, 10);
        VetbootjeEntity a = Vetbootje.geef(p);
        Vetbootje.stel(p, DyeColor.PINK, Zeil.HART, Component.literal("Glibberguh"));
        helper.assertTrue(a.kleur() == DyeColor.PINK && a.zeil() == Zeil.HART && a.naam().getString().equals("Glibberguh"), "stel paints the loaded boat");
        a.discard();
        VetbootjeEntity b = Vetbootje.roep(p);
        helper.assertTrue(b != null && b.kleur() == DyeColor.PINK && b.zeil() == Zeil.HART && b.naam().getString().equals("Glibberguh"),
                "a new body has the same looks");
        helper.assertTrue(WerfMenu.kies(p, DyeColor.LIME.getId(), Zeil.STER.ordinal(), "  §cDe   Vette\nKnabbel van de allerlangste naam ooit  "), "the werf's answer is taken");
        String naam = b.naam().getString();
        helper.assertTrue(b.kleur() == DyeColor.LIME && b.zeil() == Zeil.STER, "and painted on");
        helper.assertTrue(naam.startsWith("cDe VetteKnabbel") && naam.length() <= Vetbootje.NAAM_MAX && naam.indexOf('§') < 0 && !naam.contains("  "),
                "the typed name is cleaned and cut: '" + naam + "'");
        helper.assertFalse(WerfMenu.kies(p, 16, 0, "x") || WerfMenu.kies(p, -1, 0, "x") || WerfMenu.kies(p, 0, Zeil.values().length, "x"), "nonsense is refused");
        helper.assertTrue(b.kleur() == DyeColor.LIME && b.zeil() == Zeil.STER, "and changes nothing");
        helper.assertTrue(WerfMenu.kies(p, DyeColor.LIME.getId(), Zeil.STER.ordinal(), "   "), "an empty name is fine");
        helper.assertTrue(b.naam().getContents() instanceof TranslatableContents t && t.getKey().equals("gui.guhs.einde2_vetbootje.naam.standaard"),
                "it gives the default name back");
        helper.assertFalse(WerfMenu.kies(q, 0, 0, "Stiekem"), "no boat, no werf");
        helper.assertTrue(WerfMenu.schoon("a\tb\u0007c") .equals("abc") && WerfMenu.schoon("").isEmpty(), "control characters are dropped");
        weg(helper, p, q);
        helper.succeed();
    }

    /** The chest lives in the fleet book: kept when the body is lost, saved with the world, everybody their own. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2VetbootjeKist(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 7, 4), q = speler(helper, 3, 10);
        VetbootjeEntity a = Vetbootje.geef(p);
        Container kist = a.kist();
        helper.assertTrue(kist != null && kist.getContainerSize() == 27 && kist.isEmpty(), "a chest of 27, empty at first");
        kist.setItem(0, new ItemStack(Items.DIAMOND, 3));
        kist.setItem(26, new ItemStack(ModItems.KAAS_KNABBELS.get(), 7));
        Vetbootje.stel(p, DyeColor.CYAN, Zeil.KRUIMEL, Component.literal("SS Kaaskorst"));
        a.discard();
        VetbootjeEntity b = Vetbootje.roep(p);
        Container terug = b.kist();
        helper.assertTrue(terug.getItem(0).is(Items.DIAMOND) && terug.getItem(0).getCount() == 3 && terug.getItem(26).is(ModItems.KAAS_KNABBELS.get())
                && terug.getItem(26).getCount() == 7, "the chest is kept when the boat is made again");
        // the chest's door: only for the owner, at the boat
        Meevaren.Kist deur = new Meevaren.Kist(terug, b);
        helper.assertTrue(deur.stillValid(p) && !deur.stillValid(q), "only the owner opens it");
        VetbootjeEntity vanQ = Vetbootje.geef(q);
        helper.assertTrue(vanQ.kist() != terug && vanQ.kist().isEmpty(), "another player's chest is another chest");
        VetbootjeEntity leen = Vetbootje.leen(helper.getLevel(), b.position().add(0, 3, 0), 0, DyeColor.RED, Zeil.STREEP, null);
        helper.assertTrue(leen.kist() == null, "a leenbootje has no chest");
        // the book survives saving
        Vloot boek = Vloot.load(Vloot.van(helper.getLevel().getServer()).save(new CompoundTag()));
        Vloot.Boot blad = boek.boot(p.getUUID());
        helper.assertTrue(blad.heeft && blad.nr == b.nr && blad.kleur == DyeColor.CYAN && blad.zeil == Zeil.KRUIMEL && blad.naam != null
                && blad.naam.getString().equals("SS Kaaskorst") && b.getUUID().equals(blad.entiteit), "the page is saved: looks, number, body");
        helper.assertTrue(blad.kist.getItem(0).is(Items.DIAMOND) && blad.kist.getItem(0).getCount() == 3 && blad.kist.getItem(26).getCount() == 7,
                "and the chest with it");
        helper.assertTrue(boek.boot(q.getUUID()).heeft && boek.boot(q.getUUID()).kist.isEmpty(), "per player");
        terug.clearContent();
        weg(helper, p, q);
        helper.succeed();
    }

    /** Sailing: slowly up to speed and never faster, to a stop by itself, up with the key, and it hovers: no fall, ever. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2VetbootjeVaart(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(2, 4, 7));
        VetbootjeEntity boot = Vetbootje.leen(helper.getLevel(), new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), -90f, DyeColor.BROWN, Zeil.MIKA, null);
        Vec3 start = boot.position();
        double snelst = 0;
        for (int i = 0; i < 60; i++) {
            Vec3 voor = boot.position();
            boot.vaar(new VetbootjeEntity.Invoer(1, 0, 0));
            snelst = Math.max(snelst, boot.position().distanceTo(voor));
        }
        double gevaren = boot.getX() - start.x;
        helper.assertTrue(gevaren > 6 && gevaren < 60 * VetbootjeEntity.TOP + 0.01, "it sails ahead (facing +x): " + gevaren);
        helper.assertTrue(snelst <= VetbootjeEntity.TOP + 1e-6 && Math.abs(boot.snelheid - VetbootjeEntity.TOP) < 1e-9, "slowly: never faster than its top speed, " + snelst);
        helper.assertTrue(Math.abs(boot.getY() - start.y) < 1e-6 && Math.abs(boot.getZ() - start.z) < 0.01, "level and straight");
        for (int i = 0; i < 40; i++) {
            boot.vaar(VetbootjeEntity.Invoer.NIKS);
        }
        helper.assertTrue(boot.snelheid == 0, "nothing pressed: it glides to a stop");
        float yaw = boot.getYRot();
        for (int i = 0; i < 20; i++) {
            boot.vaar(new VetbootjeEntity.Invoer(0, 1, 1));
        }
        helper.assertTrue(boot.getYRot() > yaw + 20 && boot.getY() > start.y + 1, "it turns on the spot and rises: " + (boot.getY() - start.y));
        for (int i = 0; i < 30; i++) {
            boot.vaar(VetbootjeEntity.Invoer.NIKS);
        }
        double hoog = boot.getY();
        helper.assertTrue(boot.stijg == 0 && hoog > start.y + 1.5, "and stays up there");
        // a rider who leaves high in the air floats down
        ServerPlayer p = speler(helper, 7, 7);
        boot.snapTo(boot.getX(), boot.getY() + 6, boot.getZ(), boot.getYRot(), 0);
        hoog = boot.getY();
        helper.assertTrue(p.startRiding(boot, true, true) && boot.getControllingPassenger() == p, "anybody takes the wheel of a leenbootje");
        p.stopRiding();
        helper.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "off the boat in mid-air: slow falling");
        double hoogte = hoog;
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(Math.abs(boot.getY() - hoogte) < 1e-6 && boot.isAlive(), "with nobody aboard it hovers where it was left: " + (boot.getY() - hoogte));
            weg(helper, p);
            helper.succeed();
        });
    }

    /** Guhs: a kaasknabbel click lets your own guhs hop aboard (two cushions), the owner still fits, a call keeps them, the next click lets them off. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2VetbootjeGuhs(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 7, 4), q = speler(helper, 12, 12);
        VetbootjeEntity boot = Vetbootje.geef(p);
        GuhEntity een = guh(helper, p, 6, 6), twee = guh(helper, p, 8, 6), drie = guh(helper, p, 7, 9), vreemd = guh(helper, q, 9, 8);
        int erin = Meevaren.wissel(boot, p, VetbootjeEntity.MEEVAARDERS - boot.meevaarders());
        helper.assertTrue(erin == 2 && boot.meevaarders() == 2, "two own guhs hop aboard: " + erin);
        helper.assertTrue(vreemd.getVehicle() == null && List.of(een, twee, drie).stream().filter(g -> g.getVehicle() == boot).count() == 2,
                "the nearest two of the owner's, never somebody else's guh");
        helper.assertTrue(p.startRiding(boot) && boot.getControllingPassenger() == p && boot.getPassengers().size() == 3, "the owner still has the wheel");
        helper.assertFalse(q.startRiding(boot), "and then it is full");
        p.stopRiding();
        BlockPos hoek = helper.absolutePos(new BlockPos(3, 2, 12));
        p.snapTo(hoek.getX() + 0.5, hoek.getY(), hoek.getZ() + 0.5, 180, 0);
        VetbootjeEntity zelfde = Vetbootje.roep(p);
        helper.assertTrue(zelfde == boot && boot.meevaarders() == 2 && boot.distanceTo(p) < 6, "the called boat brings its guhs along");
        int eruit = Meevaren.wissel(boot, p, VetbootjeEntity.MEEVAARDERS - boot.meevaarders());
        helper.assertTrue(eruit == -2 && boot.getPassengers().isEmpty(), "full: the next click lets them off again: " + eruit);
        helper.assertTrue(q.startRiding(boot) && boot.getControllingPassenger() == null && boot.meevaarders() == 1, "a friend rides along on a cushion, without the wheel");
        q.stopRiding();
        for (GuhEntity g : List.of(een, twee, drie, vreemd)) {
            g.discard();
        }
        weg(helper, p, q);
        helper.succeed();
    }

    /**
     * The heteluchtballon: given once, unfolded from its item, up with the burner, slowly down without, along with the wind,
     * standing still on the ground, folded up again; and after a new copy the old one is lek.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void einde2VetbootjeBallon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 3, 3);
        Item item = E2VetbootjeFeature.HETELUCHTBALLON_ITEM.get();
        helper.assertFalse(Heteluchtballon.heeft(p), "nobody starts with a balloon");
        Heteluchtballon.geef(p);
        Heteluchtballon.geef(p);
        helper.assertTrue(Heteluchtballon.heeft(p) && aantal(p, item) == 1 && advancement(p, Heteluchtballon.GEKREGEN), "given once");
        ItemStack stapel = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                stapel = p.getInventory().getItem(i);
            }
        }
        helper.assertTrue(stapel.is(item) && p.getUUID().equals(Heteluchtballon.eigenaar(stapel)), "the folded balloon knows whose it is");
        BlockPos vloer = helper.absolutePos(new BlockPos(7, 2, 7));
        Vec3 plek = new Vec3(vloer.getX() + 0.5, vloer.getY(), vloer.getZ() + 0.5);
        HeteluchtballonEntity ballon = Heteluchtballon.vouwUit(level, plek, 0, stapel);
        helper.assertTrue(ballon != null && ballon.isAlive() && ballon.magSturen(p), "it unfolds on the floor");
        stapel.shrink(1);
        // the burner
        double snelst = 0;
        for (int i = 0; i < 80; i++) {
            double voor = ballon.getY();
            ballon.zweef(true, false);
            snelst = Math.max(snelst, ballon.getY() - voor);
        }
        double hoog = ballon.getY() - plek.y;
        helper.assertTrue(hoog > 5 && snelst <= HeteluchtballonEntity.STIJG + 1e-6, "burner on: it rises, gently: " + hoog + " / " + snelst);
        helper.assertTrue(Math.abs(ballon.getX() - plek.x) < 1e-6 && Math.abs(ballon.getZ() - plek.z) < 1e-6, "without a rider no wind takes it");
        // the wind
        Vec3 wind = HeteluchtballonEntity.wind(level, ballon.getY(), level.getGameTime());
        Vec3 voorWind = ballon.position();
        for (int i = 0; i < 40; i++) {
            ballon.zweef(i % 3 == 0, true);                  // (about level: a puff now and then)
        }
        Vec3 drift = ballon.position().subtract(voorWind);
        double flat = Math.hypot(drift.x, drift.z);
        helper.assertTrue(flat > 0.5 && flat <= 40 * HeteluchtballonEntity.WIND + 1e-6, "manned and in the air the wind takes it along, slowly: " + flat);
        helper.assertTrue(wind.horizontalDistance() <= HeteluchtballonEntity.WIND + 1e-9 && wind.horizontalDistance() > 0.02, "a slow wind");
        helper.assertTrue(HeteluchtballonEntity.windstreek(new Vec3(0, 0, 1)) == 0 && HeteluchtballonEntity.windstreek(new Vec3(-1, 0, 0)) == 2
                && HeteluchtballonEntity.windstreek(new Vec3(0, 0, -1)) == 4 && HeteluchtballonEntity.windstreek(new Vec3(1, 0, 0)) == 6, "the compass points");
        Vec3 hogerop = HeteluchtballonEntity.wind(level, ballon.getY() + HeteluchtballonEntity.WINDTRAP / 2, level.getGameTime());
        helper.assertTrue(hogerop.dot(wind) < 0, "half a turn of the wind stair higher it blows the other way");
        // a rider who steps out high in the air floats down
        helper.assertTrue(p.startRiding(ballon, true, true) && ballon.getControllingPassenger() == p, "the owner has the burner");
        p.stopRiding();
        helper.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "out of the basket in mid-air: slow falling");
        // down again: slowly, and still on the ground
        ballon.snapTo(plek.x, ballon.getY(), plek.z, 0, 0);
        ballon.setDeltaMovement(Vec3.ZERO);
        double diepst = 0;
        int tikken = 0;
        while (!(ballon.onGround() && ballon.getY() - plek.y < 0.01) && tikken < 2000) {
            double voor = ballon.getY();
            ballon.zweef(false, false);
            diepst = Math.min(diepst, ballon.getY() - voor);
            tikken++;
        }
        helper.assertTrue(ballon.onGround() && Math.abs(ballon.getY() - plek.y) < 0.01, "burner off: it comes down onto the floor (" + tikken + " ticks)");
        helper.assertTrue(diepst >= -HeteluchtballonEntity.ZAK - 1e-6 && tikken > 80, "slowly: " + diepst);
        Vec3 geland = ballon.position();
        for (int i = 0; i < 20; i++) {
            ballon.zweef(false, true);
        }
        helper.assertTrue(ballon.position().distanceTo(geland) < 0.01, "on the ground it stands still, wind or not");
        // folding
        ballon.vouwOp(p);
        helper.assertTrue(ballon.isRemoved() && aantal(p, item) == 1, "folded: the item is back");
        // a new copy: the old item and an old unfolded balloon are lek
        ItemStack oud = Heteluchtballon.stapel(p.getUUID(), 1);
        HeteluchtballonEntity tweede = Heteluchtballon.vouwUit(level, plek, 0, oud);
        helper.assertTrue(tweede != null, "copy 1 still counts");
        helper.assertTrue(Heteluchtballon.opnieuw(p), "a new copy for somebody who had one");
        helper.assertTrue(Heteluchtballon.vouwUit(level, plek.add(3, 0, 3), 0, oud) == null, "the old item is lek");
        HeteluchtballonEntity los = Heteluchtballon.vouwUit(level, plek.add(-3, 0, 3), 0, new ItemStack(item));
        helper.assertTrue(los != null && los.eigenaar() == null && los.magSturen(p), "a loose balloon belongs to nobody");
        ServerPlayer q = speler(helper, 12, 3);
        helper.assertFalse(Heteluchtballon.opnieuw(q), "no new copy for somebody who never had one");
        helper.succeedWhen(() -> {
            helper.assertTrue(tweede.isRemoved(), "the old unfolded balloon takes itself away");
            helper.assertTrue(los.isAlive(), "the loose one stays");
            weg(helper, p, q);
        });
    }
}
