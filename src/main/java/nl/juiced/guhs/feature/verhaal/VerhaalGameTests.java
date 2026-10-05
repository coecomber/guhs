package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandEvents;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.band.Wolkjes;
import nl.juiced.guhs.feature.hemel.HemelFeature;
import nl.juiced.guhs.feature.hemel.Herinnering;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeBlockEntity;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.feature.piep.SchillyEntity;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.core.UUIDUtil;
/**
 * Game tests of the 3.0 fundament (feature/verhaal and the core spots of CONTRACT_30 §3/§4): the story guhs (tameable once per
 * player, story copies never tamed, babies normal), the VariantGedrag hooks (tick, travel, ridden speed, climbing, VOEREN x
 * voerFactor, chores x draagFactor), NpcRollen by plek, Praat's answer routing, "In de wolkjes" (the Herinnering star, the
 * dead record, Wolkjes.terug), huisje ownership (non-owners can't change or break it), the generic shoulder, the Guhdex pages.
 * Template verhaal_test_wei: 12 x 12 grass at y 0 (things stand on it at helper y 2... the floor is at helper y 1).
 */
public class VerhaalGameTests {
    private static final String WEI = "verhaal_test_wei";
    private static final String BATCH = "verhaal";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    // =================================================================================================================
    // the story guhs
    // =================================================================================================================

    /** A story guh is tameable once per player, only after its questline; another player has a chance of their own. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalGuhEenKeerPerSpeler(GameTestHelper helper) {
        ServerPlayer a = speler(helper, new BlockPos(2, 1, 2)), b = speler(helper, new BlockPos(9, 1, 9));
        for (ServerPlayer p : List.of(a, b)) {
            VerhaalGuhs.vergeet(p, VerhaalGuh.BALTOGUH);
        }
        Vec3 plek = helper.absoluteVec(new Vec3(6.5, 1, 6.5));
        helper.assertTrue(!VerhaalGuhs.magTemmen(a, VerhaalGuh.BALTOGUH) && VerhaalGuhs.tem(a, VerhaalGuh.BALTOGUH, plek) == null,
                "not before the questline");
        VerhaalGuhs.geefVrij(a, VerhaalGuh.BALTOGUH);
        helper.assertTrue(VerhaalGuhs.isVrij(a, VerhaalGuh.BALTOGUH) && VerhaalGuhs.magTemmen(a, VerhaalGuh.BALTOGUH), "released for a");
        GuhEntity balto = VerhaalGuhs.tem(a, VerhaalGuh.BALTOGUH, plek);
        helper.assertTrue(balto != null && balto.isTame() && balto.isOwnedBy(a) && balto.getVariant() == GuhVariant.BALTOGUH
                && Band.isBandGuh(balto) && balto.isRideable() && !balto.isBaby(), "a's own grown-up, rideable Baltoguh (a band guh)");
        helper.assertTrue(VerhaalGuhs.heeftGetemd(a, VerhaalGuh.BALTOGUH) && !VerhaalGuhs.magTemmen(a, VerhaalGuh.BALTOGUH)
                && VerhaalGuhs.tem(a, VerhaalGuh.BALTOGUH, plek) == null, "a second time: no");
        helper.assertTrue(nl.juiced.guhs.world.GuhWorldData.get(a.level().getServer()).player(a.getUUID()).tamed.contains(GuhVariant.BALTOGUH),
                "its Guhdex page is tamed");
        helper.assertTrue(VerhaalGuhs.tem(b, VerhaalGuh.BALTOGUH, plek) == null, "b hasn't done the questline");
        VerhaalGuhs.geefVrij(b, VerhaalGuh.BALTOGUH);
        GuhEntity vanB = VerhaalGuhs.tem(b, VerhaalGuh.BALTOGUH, plek);
        helper.assertTrue(vanB != null && vanB.isOwnedBy(b) && vanB != balto, "b gets their own");
        helper.assertTrue(!GuhVariant.BALTOGUH.isCharacter() && GuhVariant.BALTOGUH.isVerhaalGuh() && GuhDex.TAMEABLE.contains(GuhVariant.STITCH626)
                && VerhaalGuh.van(GuhVariant.MEWTWO) == VerhaalGuh.MEWTWO && VerhaalGuh.byId("stitch626") == VerhaalGuh.STITCH626, "the story variants");
        for (ServerPlayer p : List.of(a, b)) {
            VerhaalGuhs.vergeet(p, VerhaalGuh.BALTOGUH);
        }
        balto.discard();
        vanB.discard();
        weg(helper, a, b);
        helper.succeed();
    }

    /** A story copy (in a structure): kaas knabbels never tame it, and it can't be hurt, pushed or bred. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalKopieWordtNooitGetemd(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(3, 1, 6));
        ServerLevel level = helper.getLevel();
        GuhEntity kopie = VerhaalGuhs.maakKopie(level, VerhaalGuh.MEWTWO, helper.absolutePos(new BlockPos(6, 1, 6)));
        helper.assertTrue(VerhaalGuhs.isKopie(kopie) && VerhaalGuhs.kopieVan(kopie) == VerhaalGuh.MEWTWO && kopie.getVariant() == GuhVariant.MEWTWO,
                "a copy of the Guhtwo");
        helper.assertTrue(kopie.isInvulnerable() && !kopie.isPushable() && kopie.isPersistenceRequired() && !kopie.removeWhenFarAway(1000)
                && VerhaalGuhs.isKopieOveral(kopie), "can't be hurt or pushed, never despawns, the flag is synced");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        for (int i = 0; i < 40; i++) {
            kopie.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(!kopie.isTame() && kopie.getOwnerUUID() == null, "40 knabbels later: still nobody's");
        helper.assertTrue(p.getMainHandItem().getCount() == 64, "and it didn't eat them (its story decides what a click does)");
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onAnimalTame(kopie, p), "the tame event is cancelled for a copy");
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 1, 9));
        wild.setVariant(GuhVariant.BALTOGUH);
        helper.assertTrue(net.neoforged.neoforge.event.EventHooks.onAnimalTame(wild, p), "a story variant is never tamed the normal way");
        helper.assertTrue(!kopie.canMate(wild) && !kopie.hurtServer(level, level.damageSources().generic(), 5f), "no breeding, no harm");
        kopie.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The babies of a story guh are normal guhs (a story guh is one of a kind). */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalBabysZijnGewoon(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity mama = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 4));
        GuhEntity papa = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 1, 4));
        mama.tame(p);
        papa.tame(p);
        mama.setVariant(GuhVariant.STITCH626);
        papa.setVariant(GuhVariant.MEWTWO);
        for (int i = 0; i < 12; i++) {
            var baby = (GuhEntity) mama.getBreedOffspring(helper.getLevel(), papa);
            helper.assertTrue(baby != null && baby.getVariant() == GuhVariant.NORMAL, "a normal baby: " + (baby == null ? null : baby.getVariant()));
            baby.discard();
        }
        weg(helper, p);
        helper.succeed();
    }

    // =================================================================================================================
    // the VariantGedrag hooks
    // =================================================================================================================

    /** A test behaviour on a variant nobody owns: tick, travel, ridden speed, climbing, VOEREN x2, chores x2 (restored after). */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void verhaalVariantGedragHooks(GameTestHelper helper) {
        GuhVariant v = GuhVariant.MINT;
        VariantGedrag oud = VariantGedragen.van(v);
        AtomicInteger ticks = new AtomicInteger(), travels = new AtomicInteger(), gegeten = new AtomicInteger();
        VariantGedrag test = new VariantGedrag() {
            @Override
            public void tick(GuhEntity guh) {
                ticks.incrementAndGet();
            }

            @Override
            public boolean travel(GuhEntity guh, Vec3 input) {
                travels.incrementAndGet();
                return false;
            }

            @Override
            public float riddenSpeed(GuhEntity guh, float speed) {
                return speed * 3;
            }

            @Override
            public boolean opKlimbaar(GuhEntity guh) {
                return true;
            }

            @Override
            public int voerFactor(GuhEntity guh) {
                return 2;
            }

            @Override
            public void gegeten(GuhEntity guh, ServerPlayer p, ItemStack snack) {
                gegeten.incrementAndGet();
            }

            @Override
            public int draagFactor(net.minecraft.world.entity.Mob guh) {
                return 2;
            }

            @Override
            public String speciaalKnop() {
                return "gui.guhs.verhaal.verder";
            }
        };
        VariantGedragen.zet(v, test);
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity gewoon = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 1, 3));
        GuhEntity mint = helper.spawn(ModEntities.GUH.get(), new BlockPos(8, 1, 8));
        gewoon.tame(p);
        mint.tame(p);
        mint.setVariant(v);
        helper.runAfterDelay(20, () -> {
            try {
                helper.assertTrue(ticks.get() > 10 && travels.get() > 10, "tick and travel are called: " + ticks + " / " + travels);
                helper.assertTrue(mint.onClimbable() && !gewoon.onClimbable(), "it climbs (626: walls and ceilings)");
                try {
                    var m = GuhEntity.class.getDeclaredMethod("getRiddenSpeed", Player.class);
                    m.setAccessible(true);
                    float s1 = (float) m.invoke(gewoon, p), s2 = (float) m.invoke(mint, p);
                    helper.assertTrue(Math.abs(s2 - 3 * s1) < 1e-4 || Math.abs(s2 - 3 * (float) mint.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)) < 1e-4, "ridden speed x3: " + s1 + " / " + s2);
                } catch (ReflectiveOperationException e) {
                    helper.fail("getRiddenSpeed: " + e);
                }
                // VOEREN: the same snack, twice the hearts (only the VOEREN hearts: a favourite snack may add its own)
                java.util.Map<Integer, Integer> voeren = new java.util.concurrent.ConcurrentHashMap<>();
                Band.opHartjes((g, eigenaar, erbij, reden) -> {
                    if (reden == Reden.VOEREN) {
                        voeren.merge(g.getId(), erbij, Integer::sum);
                    }
                });
                BandEvents.voer(gewoon, p, new ItemStack(ModItems.GUH_CUPCAKE.get(), 4));
                BandEvents.voer(mint, p, new ItemStack(ModItems.GUH_CUPCAKE.get(), 4));
                int h1 = voeren.getOrDefault(gewoon.getId(), 0), h2 = voeren.getOrDefault(mint.getId(), 0);
                helper.assertTrue(h1 >= Reden.VOEREN.standaard() && h2 == 2 * h1 && gegeten.get() == 1, "VOEREN x voerFactor: " + h1 + " / " + h2);
                helper.assertTrue(VariantGedragen.draagFactor(mint) == 2 && VariantGedragen.draagFactor(gewoon) == 1
                        && VariantGedragen.draagFactor(helper.spawn(EntityType.PIG, new BlockPos(1, 1, 9))) == 1, "draagFactor");
                // a chore trip: the lampjes round takes twice as many lamps
                helper.assertTrue(lampjesRondje(helper, p, gewoon) == nl.juiced.guhs.feature.klusjes.LampjesKlus.PER_KEER
                        && lampjesRondje(helper, p, mint) == 2 * nl.juiced.guhs.feature.klusjes.LampjesKlus.PER_KEER, "a chore trip x draagFactor");
            } finally {
                if (oud != null) {
                    VariantGedragen.zet(v, oud);
                } else {
                    VariantGedragen.weg(v);
                }
            }
            gewoon.discard();
            mint.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    /** How many unlit lamps one evening lampjes trip of this guh claims (the round it plans), from a huisje with 14 lamps. */
    private static int lampjesRondje(GameTestHelper helper, ServerPlayer p, GuhEntity guh) {
        ServerLevel level = helper.getLevel();
        Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(6, 1, 1)), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
        nl.juiced.guhs.feature.huisje.HuisjeGoal.TEST_DAGDEEL.put(h.pos(), nl.juiced.guhs.feature.knus.Dagdeel.AVOND);
        List<BlockPos> lampen = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            BlockPos l = new BlockPos(1 + (i % 7) * 1, 1, 7 + (i / 7) * 3);
            helper.setBlock(l, nl.juiced.guhs.feature.klusjes.KlusjesFeature.GUHLAMPJE.get().defaultBlockState()
                    .setValue(nl.juiced.guhs.feature.klusjes.GuhlampjeBlock.LIT, false));
            lampen.add(helper.absolutePos(l));
        }
        nl.juiced.guhs.feature.klusjes.KlusGebied.vergeet();
        Klus klus = Klusjes.van("lampjes");
        var taak = klus.zoek(level, h, guh);
        int n = 0;
        if (taak != null) {
            taak.tick();   // (the first tick plans the round: every lamp of it is claimed)
            for (BlockPos l : lampen) {
                n += nl.juiced.guhs.feature.klusjes.KlusGebied.geclaimd(level, l) ? 1 : 0;
            }
            taak.stop();
        }
        nl.juiced.guhs.feature.huisje.HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
        level.removeBlock(h.pos(), false);
        return n;
    }

    // =================================================================================================================
    // NpcRollen and Praat
    // =================================================================================================================

    /** A 3.0 character gets the role of its plek (roleData guhs_plek), else the default; the older kinds keep theirs. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalNpcRollenPerPlek(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        List<String> gezegd = new CopyOnWriteArrayList<>();
        NpcRollen.zet(GuhNpcEntity.Kind.TIKIGUH, "verhaal_test", new NpcRole() {
            @Override
            public void talk(GuhNpcEntity npc, ServerPlayer player) {
                gezegd.add("talk");
            }

            @Override
            public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
                gezegd.add("antwoord " + optie);
            }
        });
        GuhNpcEntity hier = npc(helper, GuhNpcEntity.Kind.TIKIGUH, new BlockPos(4, 1, 4), "verhaal_test");
        GuhNpcEntity daar = npc(helper, GuhNpcEntity.Kind.TIKIGUH, new BlockPos(8, 1, 4), "");
        NpcRole rol = Features.role(GuhNpcEntity.Kind.TIKIGUH);
        helper.assertTrue(rol != null && NpcRollen.rol(GuhNpcEntity.Kind.TIKIGUH) == rol, "a 3.0 kind always has a (dispatching) role");
        rol.talk(hier, p);
        helper.assertTrue(gezegd.equals(List.of("talk")), "the plek role talks: " + gezegd);
        rol.talk(daar, p);
        helper.assertTrue(gezegd.size() == 1, "another plek: the default role (not this one)");
        helper.assertTrue(NpcRollen.rol(GuhNpcEntity.Kind.REISGUH) == null && Features.role(GuhNpcEntity.Kind.SJOELGUH) != null
                && NpcRollen.isVerhaalKind(GuhNpcEntity.Kind.TIKIGUH) && !NpcRollen.isVerhaalKind(GuhNpcEntity.Kind.BOUWVAKKERGUH), "the older kinds");
        // the talking screen: without a sleutel the NPC's role gets the answer
        Praat.open(p, hier, null, "gui.guhs.verhaal.verder", new Object[0], new Praat.Optie(3, "gui.guhs.verhaal.verder"));
        Praat.antwoord(p, hier, 3);
        helper.assertTrue(gezegd.contains("antwoord 3"), "the role's antwoord: " + gezegd);
        hier.discard();
        daar.discard();
        Praat.vergeet(p);
        weg(helper, p);
        helper.succeed();
    }

    /** A scene with a sleutel: its listener gets the option (whoever spoke), and -1 when it was read / closed. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalPraatAntwoorden(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        List<String> gehoord = new CopyOnWriteArrayList<>();
        Praat.luister("verhaal_test", (speler, spreker, optie) -> gehoord.add(speler.getUUID().equals(p.getUUID()) + " "
                + (spreker == null ? "-" : spreker.getType().toShortString()) + " " + optie));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 4));
        Praat.scene(p, "verhaal_test", List.of(new Praat.Regel(guh, "", "gui.guhs.verhaal.kopie.guh"),
                        new Praat.Regel(null, "entity.guhs.guh", "gui.guhs.verhaal.kopie.mewtwo", "x")),
                new Praat.Optie(1, "gui.guhs.verhaal.verder"), new Praat.Optie(2, "gui.guhs.verhaal.verder"));
        helper.assertTrue("verhaal_test".equals(Praat.lopend(p)), "the scene is open");
        Praat.antwoord(p, guh, 2);
        helper.assertTrue(gehoord.equals(List.of("true guh 2")), "option 2 from the guh: " + gehoord);
        Praat.antwoord(p, null, -1);
        helper.assertTrue(gehoord.size() == 2 && gehoord.get(1).equals("true - -1") && Praat.lopend(p) == null, "read to the end: " + gehoord);
        guh.discard();
        weg(helper, p);
        helper.succeed();
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at, String plek) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), at);
        npc.setKind(kind);
        if (!plek.isEmpty()) {
            npc.roleData.putString(NpcRollen.PLEK, plek);
        }
        return npc;
    }

    // =================================================================================================================
    // "In de wolkjes... njeg"
    // =================================================================================================================

    /** A band guh that dies: a Herinnering star, a dead record ("In de wolkjes"), and Wolkjes.terug brings the same guh back. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 300)
    public static void verhaalWolkjesEnTerug(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 1, 6));
        guh.tame(p);
        guh.setVariant(GuhVariant.CHOCO);
        guh.setCustomName(Component.literal("Wolkje"));
        guh.wear(GuhClothes.RED_BOWTIE);
        guh.setGuhScale(1.4f);
        Band.geefHartjes(guh, p, 150, Reden.OVERIG);
        UUID id = guh.getUUID();
        int hartjes = Band.hartjes(guh);
        guh.kill(helper.getLevel());
        helper.assertTrue(Wolkjes.isDood(p.level().getServer(), p.getUUID(), id) && Wolkjes.dood(p.level().getServer(), p.getUUID()).stream().anyMatch(d -> d.bandId().equals(id)
                && d.naam().getString().equals("Wolkje") && d.variant().equals("choco") && d.hartjes() == hartjes), "in the wolkjes, with its name, look and hearts");
        helper.assertTrue(GuhVolger.plek(p.level().getServer(), p.getUUID(), id).soort() == PlekSoort.IN_DE_WOLKJES, "Waar is hij? In de wolkjes... njeg");
        List<ItemEntity> sterren = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(6, 1, 6))).inflate(3),
                e -> e.getItem().is(HemelFeature.HERINNERING.get()));
        helper.assertTrue(sterren.size() == 1 && Herinnering.data(sterren.get(0).getItem()).read("Band", UUIDUtil.CODEC).orElseThrow().equals(id)
                && Herinnering.data(sterren.get(0).getItem()).getStringOr("Naam", "").equals("Wolkje"), "one Herinnering star with its name");
        sterren.forEach(Entity::discard);
        helper.assertTrue(Wolkjes.terug(helper.getLevel(), p, UUID.randomUUID(), p.position()) == null, "not a dead guh: nothing");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntity(id) == null, "(the body is gone first)");
            GuhEntity terug = Wolkjes.terug(helper.getLevel(), p, id, helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
            helper.assertTrue(terug != null && terug.getUUID().equals(id) && terug.isAlive() && terug.getHealth() == terug.getMaxHealth()
                    && terug.isOwnedBy(p) && terug.getVariant() == GuhVariant.CHOCO && terug.getClothes(GuhClothes.Slot.NECK) == GuhClothes.RED_BOWTIE
                    && "Wolkje".equals(terug.getCustomName().getString()) && Math.abs(terug.getGuhScale() - 1.4f) < 1e-3, "the very same guh is back");
            helper.assertTrue(Band.hartjes(terug) == hartjes && !Wolkjes.isDood(p.level().getServer(), p.getUUID(), id)
                    && GuhVolger.plek(p.level().getServer(), p.getUUID(), id).soort() == PlekSoort.WERELD, "with all its hearts, alive again");
            helper.assertTrue(nl.juiced.guhs.feature.band.Dagboek.heeftEersteKeer(p.level().getServer(), p.getUUID(), id, "terug_uit_de_wolkjes"),
                    "its dagboek: terug uit de wolkjes");
            helper.assertTrue(Wolkjes.terug(helper.getLevel(), p, id, p.position()) == null, "a second time: it's alive, nothing");
            terug.discard();
            weg(helper, p);
        });
    }

    // =================================================================================================================
    // huisje ownership, the shoulder, the Guhdex
    // =================================================================================================================

    /** Only the owner (or an op) changes or breaks a huisje; the screen data says whose it is. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalHuisjeAlleenVanDeEigenaar(GameTestHelper helper) {
        ServerPlayer eigenaar = speler(helper, new BlockPos(1, 1, 8)), ander = speler(helper, new BlockPos(2, 1, 8));
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(5, 1, 3));
        Huisje h = HuisjeBlock.bouw(level, pos, Direction.SOUTH, HuisjeMaat.KLEIN, eigenaar.getUUID());
        Huisjes.zetEigenaarNaam(level.getServer(), h, "Juiced");
        String naam = h.naam();
        helper.assertTrue(h.eigenaar().equals(eigenaar.getUUID()) && Huisjes.magBewerken(eigenaar, h) && !Huisjes.magBewerken(ander, h),
                "the owner may, someone else not");
        helper.assertTrue(Huisjes.vanWie(h).getString().contains("Juiced"), "Dit is het huisje van Juiced: " + Huisjes.vanWie(h).getString());
        var data = HuisjePayloads.data(ander, h);
        helper.assertTrue(data.read("Eigenaar", UUIDUtil.CODEC).orElseThrow().equals(eigenaar.getUUID()) && !data.getBooleanOr("MagBewerken", false)
                && data.getStringOr("EigenaarNaam", "").equals("Juiced") && HuisjePayloads.data(eigenaar, h).getBooleanOr("MagBewerken", false), "the screen data");
        HuisjePayloads.doe(ander, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.NAAM.ordinal(), "", "Mijn huisje nu", false, -1));
        helper.assertTrue(Huisjes.op(level.getServer(), level.dimension(), h.pos()).naam().equals(naam), "someone else can't rename it");
        var state = level.getBlockState(pos);
        var deel = level.getBlockState(pos.above());
        helper.assertTrue(level.getBlockEntity(pos) instanceof HuisjeBlockEntity be && be.eigenaar() != null && !be.magBreken(ander) && be.magBreken(eigenaar),
                "the block knows its owner (synced)");
        helper.assertTrue(state.getDestroyProgress(ander, level, pos) == 0f && deel.getDestroyProgress(ander, level, pos.above()) == 0f
                && state.getDestroyProgress(eigenaar, level, pos) > 0f, "it doesn't even crack for someone else");
        ander.gameMode.destroyBlock(pos.above());
        ander.gameMode.destroyBlock(pos);
        helper.assertTrue(level.getBlockState(pos).getBlock() instanceof HuisjeBlock && Huisjes.op(level.getServer(), level.dimension(), h.pos()) != null,
                "someone else can't break it");
        HuisjePayloads.doe(eigenaar, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.NAAM.ordinal(), "", "Villa Test", false, -1));
        helper.assertTrue(Huisjes.op(level.getServer(), level.dimension(), h.pos()).naam().equals("Villa Test"), "the owner renames it");
        eigenaar.gameMode.destroyBlock(pos);
        helper.assertTrue(!(level.getBlockState(pos).getBlock() instanceof HuisjeBlock) && Huisjes.op(level.getServer(), level.dimension(), h.pos()) == null,
                "the owner can break it");
        weg(helper, eigenaar, ander);
        helper.succeed();
    }

    /** Any maatje can sit on your shoulder now (here a Schilly): the tag keeps its type, it hops down as itself. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalSchouderVoorElkMaatje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 2));
        SchillyEntity schilly = helper.spawn(PiepFeature.SCHILLY.get(), new BlockPos(5, 1, 5));
        schilly.tame(p);
        schilly.setCustomName(Component.literal("Schelpie"));
        helper.assertTrue(!schilly.kanOpSchouder() && schilly.schouderSchaal() == 1f && schilly.oppakGeluid() == PiepFeature.SCHILLY_PLOP.get(),
                "the PiepMaatje defaults");
        Schouder.zet(p, schilly);
        helper.assertTrue(schilly.isRemoved() && Schouder.heeft(p) && p.getPersistentData().getCompoundOrEmpty(Schouder.KEY).getStringOr("id", "").equals("guhs:schilly"),
                "on the shoulder, with its type");
        PiepMaatje eraf = Schouder.eraf(p, helper.absoluteVec(new Vec3(3.5, 1, 3.5)));
        helper.assertTrue(eraf instanceof SchillyEntity s && s.isOwnedBy(p) && "Schelpie".equals(s.getCustomName().getString()) && !Schouder.heeft(p),
                "hopped down as the same Schilly");
        eraf.dier().discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The Guhdex: every 3.0 page is in it and counts; the story guhs are tameable pages, the others characters or creatures. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void verhaalGuhdexPaginas(GameTestHelper helper) {
        for (GuhVariant v : GuhVariant.values()) {
            if (v.ordinal() > GuhVariant.ROOKGUH.ordinal() && !GuhDex.EXTRA.contains(v)) {   // (1.2.8: the Bleekwoud's two are bonus pages)
                helper.assertTrue(GuhDex.ENTRIES.contains(v) && GuhDex.TELLEND.contains(v), v + ": a counting page");
                helper.assertTrue(v.isVerhaalGuh() == GuhDex.TAMEABLE.contains(v) && v.isVerhaalGuh() != v.isCharacter(), v + ": tameable only if a story guh");
            }
        }
        for (GuhNpcEntity.Kind kind : GuhNpcEntity.Kind.values()) {
            // (bbq2: the kinds after TIKIGUH have no Guhdex page: no spoilers, and "compleet" stays reachable)
            if (NpcRollen.isVerhaalKind(kind) && kind.ordinal() <= GuhNpcEntity.Kind.TIKIGUH.ordinal()) {
                GuhVariant page = GuhVariant.ofCharacter(kind);
                helper.assertTrue(page != null && page.npcKind() == kind && GuhDex.ENTRIES.contains(page), kind + ": its character page");
            }
        }
        for (GuhVariant c : List.of(GuhVariant.MEW, GuhVariant.PLUISVINKJE, GuhVariant.GUHXOLOTL, GuhVariant.SHUCKLE)) {
            helper.assertTrue(c.isCharacter() && c.npcKind() == null, c + ": a creature page");
        }
        helper.assertTrue(GuhDex.ENTRIES.get(GuhDex.ENTRIES.size() - 1) == GuhVariant.values()[GuhVariant.values().length - 1]
                && GuhDex.EXTRA.equals(java.util.Set.of(GuhVariant.ROOKGUH, GuhVariant.KRAAKGUH, GuhVariant.KRAAK_MIKA)),
                "appended in enum order; the bonus pages are the Rookguh and (1.2.8) the two of the Bleekwoud");
        // the range overload: a creature page seen from further away (only here when no slice registered this page yet)
        if (!GuhDex.isCreaturePage(GuhVariant.PLUISVINKJE)) {
            ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
            GuhDex.creaturePage(GuhVariant.PLUISVINKJE, () -> EntityType.CHICKEN, 8);
            helper.spawn(EntityType.CHICKEN, new BlockPos(7, 1, 1));
            GuhDex.seeCreatures(p);
            helper.assertTrue(nl.juiced.guhs.world.GuhWorldData.get(p.level().getServer()).player(p.getUUID()).seen.contains(GuhVariant.PLUISVINKJE),
                    "seen from 6 blocks away (range 8)");
            helper.assertTrue(nl.juiced.guhs.feature.gids.GidsFeature.heeft(p, "diertjes/root"), "the Diertjes tab opens");
            weg(helper, p);
        }
        helper.succeed();
    }
}
