package nl.juiced.guhs.feature.elftocht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * De Elf-Guhjestocht: the rewards (+1 per speed mark), starting the tour (skates, card, start line, countdown), the
 * fixed stamp order and split colours, the finish (elfstempels, the kruisje only once, the board, the advancement),
 * free skating, the skates' speed on ice, the warm drinks at the stalls, the night lights, the cheering audience, the
 * Stempelguh's village, the shop and clothing sources, protection, a tamed Pinguh coming along, and the structure type,
 * its placement and noise.
 */
public class ElftochtGameTests {
    private static final String BAAN = "elftocht_test_baan";
    private static final String EMPTY = "empty";

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, int dorp) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == kind && (dorp == 0 || StempelRole.dorp(n) == dorp));
        helper.assertTrue(npcs.size() == 1, "one " + kind + " " + dorp + ": " + npcs.size());
        return npcs.get(0);
    }

    /** Where the template's floor is (relative y), found at a spot that is always ice or grass. */
    private static int vloer(GameTestHelper helper) {
        for (int y = 0; y < 4; y++) {
            var state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(6, y, 8)));
            if (state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                return y;
            }
        }
        return 0;
    }

    /** The spot (feet, block centre) standing on the floor at template x/z. */
    private static Vec3 op(GameTestHelper helper, int x, int z) {
        return helper.absoluteVec(new Vec3(x + 0.5, vloer(helper) + 1, z + 0.5));
    }

    /** The block at template x/z, dy above the floor. */
    private static BlockPos blok(GameTestHelper helper, int x, int dy, int z) {
        return helper.absolutePos(new BlockPos(x, vloer(helper) + dy, z));
    }

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        @SuppressWarnings("removal")
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Vec3 at = op(helper, x, z);
        p.snapTo(at.x, at.y, at.z);
        var saved = GuhQuests.saved(p);
        for (String key : new String[]{ElftochtTocht.PB, ElftochtTocht.BESTE, ElftochtTocht.RITTEN, ElftochtTocht.KRUISJE}) {
            saved.remove(key);
        }
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            ElftochtTocht.stop(p, null);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n + (p.getOffhandItem().is(item) ? p.getOffhandItem().getCount() : 0);
    }

    // --- the rules ------------------------------------------------------------------------------------------------------

    /** Always 12 elfstempels, +2 for every speed mark beaten (2.10.1: more generous; each rule is +2), and the split colours. */
    @GuhTest(template = EMPTY, batch = "elftocht_regels")
    public static void elftochtBeloningPerSnelheidsgrens(GameTestHelper helper) {
        helper.assertTrue(ElftochtTocht.munten(ElftochtTocht.MAX_TICKS) == ElftochtTocht.BASIS && ElftochtTocht.BASIS == 12, "a slow tour: 12");
        helper.assertTrue(ElftochtTocht.PER_GRENS == 2 && ElftochtTocht.EERSTE_KEER == 5, "+2 per mark, +5 the first time");
        for (int i = 0; i < ElftochtTocht.SNEL.length; i++) {
            int at = ElftochtTocht.SNEL[i];
            helper.assertTrue(ElftochtTocht.munten(at - 1) == ElftochtTocht.munten(at) + 2, "mark " + i + " is +2");
            helper.assertTrue(ElftochtTocht.munten(at - 1) == 12 + 2 * (i + 1), "mark " + i + ": " + ElftochtTocht.munten(at - 1));
        }
        helper.assertTrue(ElftochtTocht.munten(1) == 20 && ElftochtTocht.bonus(1) == 4 && ElftochtTocht.snelheid(1) == 8, "at most 8 extra");
        // generous: one normal tour (no speed bonus) buys the most expensive shop item, a first fast tour most of the shop
        helper.assertTrue(ElftochtTocht.BASIS >= SchaatsmeesterRole.PRIJS_TRUITJE, "a normal tour buys the truitje");
        helper.assertTrue(ElftochtTocht.kleur(100, 120) == ChatFormatting.GREEN && ElftochtTocht.kleur(130, 120) == ChatFormatting.RED
                && ElftochtTocht.kleur(120, 120) == ChatFormatting.YELLOW && ElftochtTocht.kleur(100, 0) == ChatFormatting.YELLOW, "split colours");
        helper.assertTrue(ElftochtTocht.VOLGORDE.length == 11 && ElftochtTocht.VOLGORDE[0] == 2 && ElftochtTocht.VOLGORDE[10] == 1,
                "the order: 2..11, Guhwarden last");
        helper.succeed();
    }

    // --- the tour -------------------------------------------------------------------------------------------------------

    /** Schaatsmeester Guhglij lends skates and a card, puts you on the start line; the stamps only count in the right order. */
    @GuhTest(template = BAAN, batch = "elftocht_start", timeoutTicks = 200)
    public static void elftochtStartEnStempelvolgorde(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 3, 4);
        helper.assertTrue(ElftochtTocht.start(meester, p, false), "the tour starts");
        helper.assertTrue(ElftochtTocht.isBezig(p) && ElftochtTocht.opTocht(p) && Minigames.ELFTOCHT.equals(Minigames.playing(p)), "on the tour");
        helper.assertTrue(tel(p, ElftochtFeature.SCHAATSEN.get()) == 1 && tel(p, ElftochtFeature.STEMPELKAART.get()) == 1, "skates and a card");
        Vec3 start = op(helper, 5, 6);
        helper.assertTrue(Math.hypot(p.getX() - start.x, p.getZ() - start.z) < 0.6 && Math.abs(p.getY() - meester.getY()) < 0.01,
                "on the start line: " + p.position() + " vs " + start);
        helper.assertTrue(ElftochtTocht.rit(p).aftellen() > 0, "the countdown");
        GuhNpcEntity snuh = npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 2), ijlguh = npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 3);
        helper.assertFalse(ElftochtTocht.stempel(snuh, p, 2), "no stamps before the whistle");
        ElftochtTocht.testTijd(p, 400);
        helper.assertFalse(ElftochtTocht.stempel(ijlguh, p, 3), "IJlguh before Snuh doesn't count");
        helper.assertTrue(ElftochtTocht.stempel(snuh, p, 2), "Snuh first: PLOF");
        helper.assertTrue(ElftochtTocht.rit(p).volgende() == 1 && ElftochtTocht.rit(p).splits()[0] == 400, "the split");
        helper.assertFalse(ElftochtTocht.stempel(snuh, p, 2), "the same stamp twice doesn't count");
        ItemStack kaart = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(ElftochtFeature.STEMPELKAART.get())).findFirst().orElseThrow();
        helper.assertTrue(StempelkaartItem.stempels(kaart) == 1, "the card has one stamp");
        helper.assertTrue(ElftochtTocht.stempel(ijlguh, p, 3), "then IJlguh");
        weg(helper, p);
        helper.succeed();
    }

    /** The finish: elfstempels (12 + speed, +5 the first time), the kruisje only the first time, the board, the advancement; the loan ends. */
    @GuhTest(template = BAAN, batch = "elftocht_finish", timeoutTicks = 200)
    public static void elftochtFinishBeloningEnKruisje(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        GuhNpcEntity guhwarden = npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 1);
        ServerPlayer p = speler(helper, 3, 4);
        helper.assertTrue(ElftochtTocht.start(meester, p, false), "start 1");
        ElftochtTocht.testStempels(p, 10);
        helper.assertFalse(ElftochtTocht.opTocht(p) && ElftochtTocht.stempel(npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 2), p, 2), "Snuh again: no");
        int snel = ElftochtTocht.SNEL[3] - 1;
        ElftochtTocht.testTijd(p, snel);
        helper.assertTrue(ElftochtTocht.stempel(guhwarden, p, 1), "the finish stamp");
        helper.assertFalse(ElftochtTocht.isBezig(p), "the tour is over");
        helper.assertTrue(tel(p, ElftochtFeature.ELFSTEMPEL.get()) == 25, "12 + 8 + 5 elfstempels: " + tel(p, ElftochtFeature.ELFSTEMPEL.get()));
        helper.assertTrue(tel(p, ElftochtFeature.KRUISJE_ITEM.get()) == 1, "the Elf-Guhjeskruisje");
        helper.assertTrue(tel(p, ElftochtFeature.SCHAATSEN.get()) == 0 && tel(p, ElftochtFeature.STEMPELKAART.get()) == 0, "skates and card back");
        helper.assertTrue(Scorebord.top(p.level().getServer(), ElftochtTocht.BOARD).stream().anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == snel),
                "on the board");
        helper.assertTrue(ElftochtVoortgang.heeft(p, "elftocht_uitgereden") && ElftochtVoortgang.heeft(p, "elftocht_snel"), "the advancements");
        helper.assertTrue(GuhQuests.saved(p).getIntArray(ElftochtTocht.PB).orElse(new int[0]).length == 11
                && GuhQuests.saved(p).getIntArray(ElftochtTocht.PB).orElse(new int[0])[10] == snel, "the best tour's splits");
        // a second, slow tour: 12 elfstempels, no second kruisje, the best splits stay
        helper.assertTrue(ElftochtTocht.start(meester, p, false), "start 2");
        ElftochtTocht.testStempels(p, 10);
        ElftochtTocht.testTijd(p, ElftochtTocht.SNEL[0] + 20);
        helper.assertTrue(ElftochtTocht.stempel(guhwarden, p, 1), "finish 2");
        helper.assertTrue(tel(p, ElftochtFeature.ELFSTEMPEL.get()) == 25 + 12, "12 more: " + tel(p, ElftochtFeature.ELFSTEMPEL.get()));
        helper.assertTrue(tel(p, ElftochtFeature.KRUISJE_ITEM.get()) == 1, "still one kruisje");
        helper.assertTrue(GuhQuests.saved(p).getIntArray(ElftochtTocht.PB).orElse(new int[0])[10] == snel, "the record stays");
        helper.assertTrue(GuhQuests.saved(p).getIntOr(ElftochtTocht.RITTEN, 0) == 2, "two tours");
        weg(helper, p);
        helper.succeed();
    }

    /** The whole Elf-Guhjestocht is the tour (DESIGN 2.10 §9.8): in the far corner of its square, much farther from the start
     *  than the old circle of 220, you keep your skates; a little over the edge still counts; only well outside it, and
     *  after a few seconds of grace (coming back in time resets it), do the skates go back. */
    @GuhTest(template = BAAN, batch = "elftocht_gebied", timeoutTicks = 200)
    public static void elftochtSchaatsenOpDeHeleTocht(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 3, 4);
        helper.assertTrue(ElftochtTocht.start(meester, p, false), "the tour starts");
        helper.assertTrue(ElftochtTocht.rit(p).gebied == null, "a test rink is no tour square: the old circle applies");
        // the tour's square as in the world (256 x 256), the player in its far corner and the start (Guhwarden) near the
        // opposite corner: about 300 blocks away
        BlockPos hier = p.blockPosition();
        BlockPos start = hier.offset(-230, 0, -226);
        BoundingBox tocht = new BoundingBox(start.getX() - 20, hier.getY() - 30, start.getZ() - 30, hier.getX() + 2, hier.getY() + 40, hier.getZ() + 3);
        ElftochtTocht.Rit rit = ElftochtTocht.testRit(p, start, tocht);
        helper.assertTrue(Math.hypot(p.getX() - start.getX(), p.getZ() - start.getZ()) > ElftochtTocht.VERLATEN, "farther than the old circle");
        for (int i = 0; i < ElftochtTocht.GRATIE + 20; i++) {
            ElftochtTocht.tick(p);
        }
        helper.assertTrue(ElftochtTocht.opTocht(p) && tel(p, ElftochtFeature.SCHAATSEN.get()) == 1, "the far corner: still on the tour, skates on");
        helper.assertTrue(ElftochtTocht.opDeTocht(rit, tocht.maxX() + 1 + ElftochtTocht.MARGE - 0.5, p.getZ()), "just over the edge still counts");
        helper.assertFalse(ElftochtTocht.opDeTocht(rit, tocht.maxX() + 1 + ElftochtTocht.MARGE + 0.5, p.getZ()), "past the margin: off the tour");
        // the square moves away from the player: now they're off the tour
        rit.gebied = tocht.moved(-60, 0, 0);
        for (int i = 0; i < ElftochtTocht.GRATIE - 10; i++) {
            ElftochtTocht.tick(p);
        }
        helper.assertTrue(ElftochtTocht.opTocht(p), "a few seconds of grace");
        rit.gebied = tocht;
        ElftochtTocht.tick(p);
        rit.gebied = tocht.moved(-60, 0, 0);
        for (int i = 0; i < ElftochtTocht.GRATIE - 10; i++) {
            ElftochtTocht.tick(p);
        }
        helper.assertTrue(ElftochtTocht.opTocht(p), "back on the tour in time: the grace starts over");
        for (int i = 0; i < 12; i++) {
            ElftochtTocht.tick(p);
        }
        helper.assertFalse(ElftochtTocht.isBezig(p), "too long off the tour: the ride stops");
        helper.assertTrue(tel(p, ElftochtFeature.SCHAATSEN.get()) == 0 && tel(p, ElftochtFeature.STEMPELKAART.get()) == 0, "skates and card back");
        weg(helper, p);
        helper.succeed();
    }

    /** Free skating: skates, no card, no stamps; "Stoppen" takes the skates back. Friends ride at the same time. */
    @GuhTest(template = BAAN, batch = "elftocht_vrij", timeoutTicks = 200)
    public static void elftochtVrijSchaatsenEnVrienden(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 3, 4), vriend = speler(helper, 4, 4);
        helper.assertTrue(ElftochtTocht.start(meester, p, true), "free skating");
        helper.assertTrue(ElftochtTocht.isBezig(p) && !ElftochtTocht.opTocht(p), "skating, not on the tour");
        helper.assertTrue(tel(p, ElftochtFeature.SCHAATSEN.get()) == 1 && tel(p, ElftochtFeature.STEMPELKAART.get()) == 0, "skates, no card");
        helper.assertFalse(ElftochtTocht.stempel(npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 2), p, 2), "no stamps when skating freely");
        helper.assertTrue(ElftochtTocht.start(meester, vriend, false) && ElftochtTocht.opTocht(vriend), "a friend starts the tour at the same time");
        helper.assertTrue(ElftochtTocht.start(meester, p, false) && ElftochtTocht.opTocht(p), "from free skating onto the tour");
        SchaatsmeesterRole.action(meester, p, SchaatsmeesterRole.STOP);
        helper.assertFalse(ElftochtTocht.isBezig(p), "stopped");
        helper.assertTrue(tel(p, ElftochtFeature.SCHAATSEN.get()) == 0, "skates back");
        helper.assertTrue(ElftochtTocht.opTocht(vriend), "the friend still rides");
        weg(helper, p, vriend);
        helper.succeed();
    }

    /** With skates on ice you're fast; off the ice (grass) you walk; without skates nothing changes. */
    @GuhTest(template = BAAN, batch = "elftocht_schaatsen", timeoutTicks = 200)
    public static void elftochtSchaatsenGlijdenOpIjs(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 6, 8);
        ElftochtTocht.start(meester, p, true);
        ItemStack skates = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(ElftochtFeature.SCHAATSEN.get())).findFirst().orElseThrow();
        p.setItemInHand(InteractionHand.MAIN_HAND, skates.copy());
        skates.setCount(0);
        Vec3 ijs = op(helper, 6, 8);
        p.snapTo(ijs.x, ijs.y, ijs.z);
        p.setOnGround(true);
        double basis = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        ElftochtSchaatsen.tick(p);
        helper.assertTrue(ElftochtSchaatsen.schaatst(p), "skating on the ice");
        helper.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(ElftochtSchaatsen.MODIFIER)
                && p.getAttributeValue(Attributes.MOVEMENT_SPEED) > basis * 1.3, "faster on the ice");
        Vec3 gras = op(helper, 6, 0);
        p.snapTo(gras.x, gras.y, gras.z);
        p.setOnGround(true);
        p.tickCount += ElftochtSchaatsen.SPRONG + 5;
        ElftochtSchaatsen.tick(p);
        helper.assertFalse(p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(ElftochtSchaatsen.MODIFIER), "walking on the grass");
        p.snapTo(ijs.x, ijs.y, ijs.z);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ElftochtSchaatsen.tick(p);
        helper.assertFalse(ElftochtSchaatsen.schaatst(p), "no skates, no skating");
        weg(helper, p);
        helper.succeed();
    }

    /** The stall's tray gives a warm drink (then it's still too hot for a while); drinking it gives the speed boost. */
    @GuhTest(template = BAAN, batch = "elftocht_kopjes", timeoutTicks = 100)
    public static void elftochtWarmeChocovetGeeftBoost(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 19, 13);
        BlockPos kopjes = blok(helper, 20, 2, 12);
        helper.assertTrue(helper.getLevel().getBlockState(kopjes).is(ElftochtFeature.KOPJES.get()), "the tray on the counter");
        helper.assertTrue(KopjesBlock.pak(p, KopjesBlock.Soort.CHOCOVET, kopjes), "a cup");
        helper.assertTrue(tel(p, ElftochtFeature.WARME_CHOCOVET.get()) == 1, "warme chocovet");
        helper.assertFalse(KopjesBlock.pak(p, KopjesBlock.Soort.CHOCOVET, kopjes), "too hot for another one");
        helper.assertTrue(KopjesBlock.pak(p, KopjesBlock.Soort.SNERT, kopjes), "snert is another pot");
        ItemStack cup = new ItemStack(ElftochtFeature.WARME_CHOCOVET.get());
        cup.finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(MobEffects.SPEED), "warm inside: the boost");
        weg(helper, p);
        helper.succeed();
    }

    /** The lampions and vuurkorven light up at night and go out in the morning. */
    @GuhTest(template = BAAN, batch = "elftocht_licht", timeoutTicks = 100)
    public static void elftochtLichtenAanInDeNacht(GameTestHelper helper) {
        var level = helper.getLevel();
        long was = nl.juiced.guhs.world.GuhTime.dayTime(level);
        BlockPos lampion = blok(helper, 22, 1, 12), korf = blok(helper, 22, 1, 14);
        try {
            nl.juiced.guhs.world.GuhTime.setDayTime(level, 18000);
            helper.assertTrue(NachtlichtBlock.nacht(level), "midnight is night");
            NachtlichtBlock.bijwerken(level, lampion, level.getBlockState(lampion));
            NachtlichtBlock.bijwerken(level, korf, level.getBlockState(korf));
            helper.assertTrue(level.getBlockState(lampion).getValue(NachtlichtBlock.LIT) && level.getBlockState(korf).getValue(NachtlichtBlock.LIT),
                    "lit at night");
            helper.assertTrue(level.getBlockState(lampion).getLightEmission(level, lampion) == 15, "bright");
            nl.juiced.guhs.world.GuhTime.setDayTime(level, 6000);
            NachtlichtBlock.bijwerken(level, lampion, level.getBlockState(lampion));
            NachtlichtBlock.bijwerken(level, korf, level.getBlockState(korf));
            helper.assertFalse(level.getBlockState(lampion).getValue(NachtlichtBlock.LIT) || level.getBlockState(korf).getValue(NachtlichtBlock.LIT),
                    "out by day");
        } finally {
            nl.juiced.guhs.world.GuhTime.setDayTime(level, was);
        }
        helper.succeed();
    }

    /** An audience guh cheers when a skater glides past (VAHOEG, a wave or a dance), then catches its breath. */
    @GuhTest(template = BAAN, batch = "elftocht_publiek", timeoutTicks = 100)
    public static void elftochtPubliekJuichtVoorSchaatsers(GameTestHelper helper) {
        List<GuhEntity> publiek = helper.getLevel().getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(1),
                g -> g.entityTags().contains(ElftochtPubliek.TAG));
        helper.assertTrue(publiek.size() == 1, "one audience guh: " + publiek.size());
        GuhEntity guh = publiek.get(0);
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 12, 8);
        helper.assertTrue(ElftochtPubliek.schaatserBij(guh) == null, "nobody skating yet");
        ElftochtTocht.start(meester, p, true);
        Vec3 bij = op(helper, 12, 8);
        p.snapTo(bij.x, bij.y, bij.z);
        p.setDeltaMovement(0.35, 0, 0);
        helper.assertTrue(ElftochtPubliek.schaatserBij(guh) == p, "a skater gliding past");
        // (a guh only starts an emote standing on the ground: give it a moment to land after the template placed it)
        helper.succeedWhen(() -> {
            helper.assertTrue(guh.onGround(), "the audience guh stands");
            ElftochtPubliek.juich(guh, p);
            helper.assertTrue(ElftochtPubliek.heeftGejuicht(guh) && guh.emotes.current() != null, "cheering: " + guh.emotes.current());
            weg(helper, p);
        });
    }

    /** A Stempelguh knows its village from its tag and is called after it. */
    @GuhTest(template = BAAN, batch = "elftocht_stempelguh", timeoutTicks = 100)
    public static void elftochtStempelguhKentZijnDorp(GameTestHelper helper) {
        GuhNpcEntity snuh = npc(helper, GuhNpcEntity.Kind.STEMPELGUH, 2);
        helper.assertTrue(StempelRole.dorp(snuh) == 2, "village 2");
        helper.succeedWhen(() -> helper.assertTrue(snuh.getCustomName() != null
                && snuh.getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("gui.guhs.elftocht.stempelguh.2"), "named Stempelguh van Snuh: " + snuh.getCustomName()));
    }

    /** Guhglij's shop: the four tour clothes for elfstempels (their only source), warm drinks and lamps; loaned things are loaned. */
    @GuhTest(template = BAAN, batch = "elftocht_winkel", timeoutTicks = 100)
    public static void elftochtWinkelEnKledingbronnen(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        var offers = ElftochtFeature.schaatsmeester().offers(meester);
        for (GuhClothes c : ElftochtVoortgang.KLEDING) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(c)) && o.getCostA().is(ElftochtFeature.ELFSTEMPEL.get())),
                    "sells " + c);
            helper.assertTrue("elftocht".equals(KledingBronnen.bron(c)), "the source of " + c + ": " + KledingBronnen.bron(c));
        }
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ElftochtFeature.WARME_CHOCOVET.get())), "chocovet");
        helper.assertTrue(Features.isLoaned(new ItemStack(ElftochtFeature.SCHAATSEN.get())) && Features.isLoaned(new ItemStack(ElftochtFeature.STEMPELKAART.get()))
                && !Features.isLoaned(new ItemStack(ElftochtFeature.ELFSTEMPEL.get())), "the skates and the card are loaned");
        helper.assertTrue(ElftochtFeature.stempelguh() instanceof StempelRole && ElftochtFeature.schaatsmeester() instanceof SchaatsmeesterRole, "the roles");
        helper.succeed();
    }

    /** The tour can't be broken (survival), creative may. */
    @GuhTest(template = EMPTY, batch = "elftocht_regels")
    public static void elftochtBescherming(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 1, 1);
        helper.assertTrue(ElftochtProtection.denied(p, true), "no breaking the tour");
        helper.assertFalse(ElftochtProtection.denied(p, false), "outside it's fine");
        p.setGameMode(GameType.CREATIVE);
        helper.assertFalse(ElftochtProtection.denied(p, true), "creative may");
        weg(helper, p);
        helper.succeed();
    }

    /** A tamed Pinguh of a skater comes along (never gets lost behind). */
    @GuhTest(template = BAAN, batch = "elftocht_pinguh", timeoutTicks = 100)
    public static void elftochtPinguhGlijdtMee(GameTestHelper helper) {
        GuhNpcEntity meester = npc(helper, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, 0);
        ServerPlayer p = speler(helper, 3, 8);
        ElftochtTocht.start(meester, p, true);
        GuhEntity pinguh = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(pinguh != null, "a guh");
        Vec3 ver = op(helper, 23, 14).add(20, 0, 0);
        pinguh.snapTo(ver.x, ver.y, ver.z);
        pinguh.setVariant(GuhVariant.PINGUH);
        pinguh.tame(p);
        helper.getLevel().addFreshEntity(pinguh);
        Vec3 bij = op(helper, 3, 8);
        p.snapTo(bij.x, bij.y, bij.z);
        ElftochtPubliek.pinguhs(p);
        helper.assertTrue(pinguh.distanceTo(p) < 5, "the Pinguh came along: " + pinguh.distanceTo(p));
        helper.assertTrue(PinguhMeeglijden.glijdtMee(pinguh), "the Pinguh belly-slides along (PinguhMeeglijden)");
        helper.assertTrue(PinguhMeeglijden.meeglijders(p).contains(pinguh), "it is the skater's");
        ElftochtTocht.stop(p, null);
        helper.assertFalse(PinguhMeeglijden.glijdtMee(pinguh), "after the ride it just follows as usual");
        pinguh.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The structure: our own type (voorrang 900, room for the 256 x 256 tour), our placement with cells, and the noise the
     *  placement makes from the seed is exactly the terrain's noise. */
    @GuhTest(template = EMPTY, batch = "elftocht_structuur", timeoutTicks = 200)
    public static void elftochtStructuurPlaatsingEnRuis(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var structure = access.lookupOrThrow(Registries.STRUCTURE).getValue(ElftochtFeature.STRUCTURE);
        helper.assertTrue(structure instanceof ElftochtStructure s && s.voorrang() == 900 && s.keepClear() >= 136, "the structure: " + structure);
        ElftochtStructure tocht = (ElftochtStructure) structure;
        var set = access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id("elfguhjestocht"));
        helper.assertTrue(set != null && set.placement() instanceof ElftochtPlacement p && p.spacing() == tocht.cell(), "the placement: "
                + (set == null ? null : set.placement()));
        ElftochtPlacement placement = (ElftochtPlacement) set.placement();
        helper.assertTrue(placement.vlak().minVlak() > 0 && placement.vlak().minVlak() == tocht.vlak().minVlak()
                && placement.vlak().zee().isPresent() && placement.vlak().knuffel().isPresent(), "the dead-flat polder rules (sea + Knuffeldal masks)");
        Holder<NormalNoise.NoiseParameters> noise = placement.noise();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        for (long seed : new long[]{1L, 20290601L, -42L}) {
            RandomState state = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            NormalNoise terrein = state.getOrCreateNoise(noise.unwrapKey().orElseThrow());
            NormalNoise eigen = ElftochtPiek.noise(seed, noise);
            for (int i = 0; i < 20; i++) {
                double x = i * 731.0 - 5000, z = i * -377.0 + 2000;
                helper.assertTrue(Math.abs(terrein.getValue(x, 0, z) - eigen.getValue(x, 0, z)) < 1e-12, "the same noise at " + x + "," + z);
            }
            // the potential start chunk of a cell is the chunk of its tour spot: near the peak, inside the cell, where the square
            // lies best on dead-flat polder (the placement and the structure use the same rules)
            for (int c = -3; c <= 3; c++) {
                var chunk = placement.getPotentialStructureChunk(seed, c * placement.spacing(), -c * placement.spacing());
                ElftochtPiek.Peak peak = ElftochtPiek.peak(seed, noise, placement.spacing(), c, -c);
                ElftochtPiek.Spot spot = ElftochtPiek.spot(seed, noise, placement.vlak(), placement.spacing(), c, -c);
                helper.assertTrue(chunk.x() == spot.x() >> 4 && chunk.z() == spot.z() >> 4, "the spot's chunk");
                helper.assertTrue(Math.abs(spot.x() - peak.x()) <= ElftochtPiek.ZOEK && Math.abs(spot.z() - peak.z()) <= ElftochtPiek.ZOEK, "near the peak");
                helper.assertTrue(Math.floorDiv(chunk.x(), placement.spacing()) == c && Math.floorDiv(chunk.z(), placement.spacing()) == -c, "inside its cell");
                ElftochtPiek.Spot plek = tocht.plek(seed, chunk.x(), chunk.z());
                helper.assertTrue(plek == null || plek.equals(spot) && spot.vlak() >= placement.vlak().minVlak(), "the structure agrees: " + plek);
            }
        }
        helper.succeed();
    }
}
