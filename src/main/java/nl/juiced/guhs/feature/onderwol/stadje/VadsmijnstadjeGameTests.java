package nl.juiced.guhs.feature.onderwol.stadje;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;
import nl.juiced.guhs.feature.kaasmijn.Mijnguh;
import nl.juiced.guhs.feature.mijnhuisje.MijnHulpje;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.onderwol.bouw.GrotBouwStructure;
import nl.juiced.guhs.feature.techmachine.VadsmolenBlockEntity;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * Game tests of the Vadsmijnstadje (tools/features/onderwol_stadje.py): the structure and its set, the template's anchors,
 * its villagers (guh villagers with guh jobs, each with its workstation close by, a bed for everyone), its texts; and the
 * Mijnguh of the Mijnguhdorp starting the quest Mijnguh-hulpje. Template {@code onderwol_bouw_test} (onderwol_structuren1.py).
 */
public class VadsmijnstadjeGameTests {
    private static final String VELD = "onderwol_bouw_test";
    private static final String BATCH = "vadsmijnstadje";
    /** How far a villager may stand from its workstation in the template (it stands in its own house). */
    private static final double WERKPLEK_BIJ = 6.0;

    private static CompoundTag template(ServerLevel level) {
        var res = level.getServer().getResourceManager().getResource(Guhs.id("structure/onderwol/" + Vadsmijnstadje.NAAM + "/start.nbt"));
        if (res.isEmpty()) {
            throw new IllegalStateException("no template onderwol/vadsmijnstadje/start");
        }
        try (InputStream in = res.get().open()) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Blok(BlockPos pos, BlockState state) {
    }

    private static List<Blok> blokken(ServerLevel level, CompoundTag t) {
        var lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        ListTag palette = t.getListOrEmpty("palette");
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(lookup, palette.getCompoundOrEmpty(i)));
        }
        List<Blok> out = new ArrayList<>();
        ListTag blocks = t.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompoundOrEmpty(i);
            ListTag p = b.getListOrEmpty("pos");
            out.add(new Blok(new BlockPos(p.getIntOr(0, 0), p.getIntOr(1, 0), p.getIntOr(2, 0)), states.get(b.getIntOr("state", 0))));
        }
        return out;
    }

    /** The structure: a guhs:grot_bouw in the knuffelsteen layer, with an entrance hut, about every 1500 blocks, in the Superkompas. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void vadsmijnstadjeIsGeregistreerd(GameTestHelper helper) {
        var structuren = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Structure> s = structuren.getOptional(ResourceKey.create(Registries.STRUCTURE, Guhs.id(Vadsmijnstadje.NAAM)));
        helper.assertTrue(s.isPresent() && s.get() instanceof GrotBouwStructure, "the Vadsmijnstadje is a guhs:grot_bouw");
        GrotBouwStructure g = (GrotBouwStructure) s.get();
        helper.assertTrue(g.bandOnder() >= 20 && g.maxY() <= 41, "its floor lies in the knuffelsteen layer: " + g.bandOnder() + ".." + g.maxY());
        helper.assertTrue(g.ingang().isPresent() && g.ingang().get().kans() >= 1.0f, "always a liftschacht hut on the surface");
        helper.assertTrue(g.keepClear() >= 30, "it claims the room of its whole hall: " + g.keepClear());
        helper.assertTrue(g.biomes().size() > 5, "it can stand under the Guhmension's biomes");
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        Optional<StructureSet> set = sets.getOptional(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(Vadsmijnstadje.NAAM)));
        helper.assertTrue(set.isPresent() && set.get().placement() instanceof RandomSpreadStructurePlacement, "a random spread set");
        RandomSpreadStructurePlacement p = (RandomSpreadStructurePlacement) set.get().placement();
        helper.assertTrue(p.spacing() >= 80 && p.spacing() <= 110, "about every 1500 blocks: spacing " + p.spacing());
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("ondergrond") && c.structures().contains(Vadsmijnstadje.NAAM)),
                "in the Superkompas tab ondergrond");
        helper.succeed();
    }

    /** The start template has its anchor and the shaft's marker. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void vadsmijnstadjeAnkers(GameTestHelper helper) {
        var pools = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        StructureTemplatePool pool = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/" + Vadsmijnstadje.NAAM + "/start")))
                .orElse(null);
        helper.assertTrue(pool != null, "its start pool");
        for (StructurePoolElement e : pool.getShuffledTemplates(RandomSource.create(1))) {
            var jigsaws = e.getShuffledJigsawBlocks(helper.getLevel().getStructureManager(), BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE,
                    RandomSource.create(1));
            helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(Guhs.id(Vadsmijnstadje.NAAM + "_midden"))), "the anchor");
            helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(GrotBouwStructure.INGANG)), "the shaft's marker");
        }
        helper.succeed();
    }

    /**
     * The villagers: real guh villagers (type guhs:guh) with guh jobs, all four new professions among them; every employed
     * one has a workstation of its own job close by (one workstation each); a bed for everyone; a bell; IJzerguhlems; the
     * running Guhrad.
     */
    @GuhTest(template = VELD, batch = BATCH)
    public static void vadsmijnstadjeVillagers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CompoundTag t = template(level);
        List<Blok> blokken = blokken(level, t);
        ListTag entities = t.getListOrEmpty("entities");
        int villagers = 0, golems = 0, kinderen = 0;
        Set<String> beroepen = new HashSet<>();
        Set<BlockPos> bezet = new HashSet<>();
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag e = entities.getCompoundOrEmpty(i);
            CompoundTag nbt = e.getCompoundOrEmpty("nbt");
            String id = nbt.getStringOr("id", "");
            if (id.equals("guhs:ijzerguhlem")) {
                golems++;
                continue;
            }
            if (!id.equals("minecraft:villager")) {
                continue;
            }
            villagers++;
            ListTag pos = e.getListOrEmpty("pos");
            double x = pos.getDoubleOr(0, 0), y = pos.getDoubleOr(1, 0), z = pos.getDoubleOr(2, 0);
            Optional<Entity> made = EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), nbt), level, EntitySpawnReason.LOAD);
            helper.assertTrue(made.isPresent() && made.get() instanceof Villager, "villager " + i + " loads");
            Villager v = (Villager) made.get();
            helper.assertTrue(ModVillagers.isGuhVillager(v), "villager " + i + " is a guh villager");
            Holder<VillagerProfession> beroep = v.getVillagerData().profession();
            if (v.isBaby()) {
                kinderen++;
                continue;
            }
            helper.assertTrue(ModVillagers.isGuhProfession(beroep), "villager " + i + " has a guh job: " + beroep);
            helper.assertTrue(v.getVillagerXp() > 0, "villager " + i + " keeps its job (xp)");
            beroepen.add(beroep.unwrapKey().orElseThrow().identifier().getPath());
            Blok werkplek = null;
            for (Blok b : blokken) {
                if (bezet.contains(b.pos()) || b.pos().getCenter().distanceTo(new net.minecraft.world.phys.Vec3(x, y + 0.5, z)) > WERKPLEK_BIJ) {
                    continue;
                }
                Optional<Holder<PoiType>> poi = PoiTypes.forState(b.state());
                if (poi.isPresent() && beroep.value().acquirableJobSite().test(poi.get())) {
                    werkplek = b;
                    break;
                }
            }
            helper.assertTrue(werkplek != null, "villager " + i + " (" + beroep.getRegisteredName() + ") has its own workstation close by");
            bezet.add(werkplek.pos());
        }
        long bedden = blokken.stream().filter(b -> b.state().getBlock() instanceof BedBlock && b.state().getValue(BedBlock.PART) == BedPart.HEAD).count();
        helper.assertTrue(villagers >= 10, "about ten villagers: " + villagers);
        helper.assertTrue(kinderen >= 1 && villagers - kinderen >= 8, "grown-ups and children");
        for (String nieuw : List.of("toverguh", "vadsmonteur", "mijnwerker", "kristalslijper")) {
            helper.assertTrue(beroepen.contains(nieuw), "a " + nieuw + " lives here");
        }
        helper.assertTrue(bedden >= villagers, bedden + " beds for " + villagers + " villagers");
        helper.assertTrue(blokken.stream().anyMatch(b -> b.state().is(Blocks.BELL)), "a bell (the meeting point)");
        helper.assertTrue(golems >= 1, "an IJzerguhlem keeps watch");
        helper.assertTrue(blokken.stream().anyMatch(b -> b.state().is(ModBlocks.GUH_WHEEL.get())), "the Guhrad-centrale");
        helper.assertTrue(GuhVillagersFeature.WERKPLEKKEN.stream().allMatch(w -> blokken.stream().anyMatch(b -> b.state().is(w.get()))),
                "all four new workstations");
        helper.succeed();
    }

    /** The Superkompas texts and every sign line of the template exist in nl_nl and en_us. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void vadsmijnstadjeTeksten(GameTestHelper helper) {
        String alles = template(helper.getLevel()).toString();
        Matcher m = Pattern.compile("sign\\.guhs\\.onderwol_bouw\\.stadje\\.[a-z0-9_]+").matcher(alles);
        Set<String> keys = new HashSet<>();
        while (m.find()) {
            keys.add(m.group());
        }
        helper.assertTrue(keys.size() >= 30, "the signs use translatable lines: " + keys.size());
        for (String taal : List.of("nl_nl", "en_us")) {
            try (InputStream in = VadsmijnstadjeGameTests.class.getResourceAsStream("/assets/guhs/lang/" + taal + ".json")) {
                JsonObject lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                helper.assertTrue(lang.has("structure.guhs." + Vadsmijnstadje.NAAM) && lang.has("structure.guhs." + Vadsmijnstadje.NAAM + ".tooltip"),
                        taal + ": the Superkompas texts");
                for (String k : keys) {
                    helper.assertTrue(lang.has(k), taal + ": " + k);
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        helper.succeed();
    }

    /** The Mijnguh of the Mijnguhdorp starts the quest Mijnguh-hulpje at once (no kaasmijn welcome first). */
    @GuhTest(template = VELD, batch = BATCH)
    public static void vadsmijnstadjeDorpMijnguhGeeftHulpje(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.MIJNGUH);
        npc.roleData.putString(NpcRollen.PLEK, Mijnguh.DORP);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        helper.assertTrue(MijnHulpje.stand(player) == MijnHulpje.NIET, "not asked yet");
        Features.role(GuhNpcEntity.Kind.MIJNGUH).talk(npc, player);
        helper.assertTrue(MijnHulpje.stand(player) == MijnHulpje.GEVRAAGD, "the dorp Mijnguh asked for help");
        helper.assertTrue(!nl.juiced.guhs.quest.GuhQuests.saved(player).getBooleanOr(Mijnguh.MET_KEY, false), "no kaasmijn welcome first");
        npc.discard();
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /**
     * 1.7.0 (ronde 45-47): the town's vadskracht really runs in the placed template (feature/onderwol/bouw/KrachtProef):
     * three Guhraden whole and running (two in the centrale, one at the ore line), every machine awake: the 19 Vadslampen
     * burn and light around them, the Glasslijperij grinds and bakes, and the Ertslijn brings bars (the chest cart's raw ore,
     * through the hopper and the Guhoven) and zand (the Vadsmolen) into the Mijnwerkerskist.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 700)
    public static void vadsmijnstadjeCentraleDraait(GameTestHelper helper) {
        fabriek(helper);
    }

    /** The same, with the town placed turned (as the jigsaw placement does 3 times in 4): it all turns with it. */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 700, rotationSteps = 1)
    public static void vadsmijnstadjeCentraleDraaitGedraaid(GameTestHelper helper) {
        fabriek(helper);
    }

    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 700, rotationSteps = 2)
    public static void vadsmijnstadjeFabriekGedraaid2(GameTestHelper helper) {
        fabriek(helper);
    }

    private static void fabriek(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.draait(helper, 3, 23, 0, g -> {
            nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.lampenBranden(helper, g, 19);
            long molens = g.machines().stream().filter(m -> level.getBlockEntity(m) instanceof nl.juiced.guhs.feature.techmachine.VadsmolenBlockEntity).count();
            helper.assertTrue(molens == 2 && g.ovens().size() == 2, "two Vadsmolens and two Guhovens: " + molens + ", " + g.ovens().size());
            for (BlockPos o : g.ovens()) {
                var oven = (nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity) level.getBlockEntity(o);
                helper.assertTrue(level.getBlockState(o).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)
                        || !oven.getItem(2).isEmpty(), "the Guhoven at " + o + " bakes");
            }
            int baren = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.inKisten(helper, net.minecraft.world.item.Items.IRON_INGOT)
                    + nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.inKisten(helper, net.minecraft.world.item.Items.COPPER_INGOT)
                    + nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.inKisten(helper, net.minecraft.world.item.Items.GOLD_INGOT);
            helper.assertTrue(baren > 0, "bars from the cart's ore in the Mijnwerkerskist (cart -> hopper -> Guhoven -> hopper -> chest)");
            helper.assertTrue(nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.inKisten(helper, net.minecraft.world.item.Items.SAND) > 0,
                    "zand from the Vadsmolen in the Mijnwerkerskist");
        });
    }

    /** The two wheels of the centrale (the ones nearest the middle of the town), nearest first. */
    private static List<BlockPos> centrale(GameTestHelper helper, nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.Gevonden g) {
        net.minecraft.world.phys.Vec3 midden = helper.getBounds().getCenter();
        List<BlockPos> wielen = new ArrayList<>(g.wielen());
        wielen.sort(java.util.Comparator.comparingDouble(w -> w.getCenter().distanceToSqr(midden.x, w.getY() + 0.5, midden.z)));
        return wielen.subList(0, 2);
    }

    /**
     * The levers and the switch of the centrale: a lever in front of either wheel gives that guh a break, and then the net
     * (28 VK of lamps and glass works) has too little vadskracht and stands still: every lamp of the town goes out, the ore
     * line (its own wheel) runs on. Lever back: all burns again. The Vadsschakelaar by the centrale cuts the lamps' line:
     * the lamps go out, the wheels run on.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 900)
    public static void vadsmijnstadjeHendelsEnSchakelaar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var g = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.zoek(helper);
        List<BlockPos> centrale = centrale(helper, g);
        BlockPos ertsrad = g.wielen().stream().filter(w -> !centrale.contains(w)).findFirst().orElseThrow();
        List<BlockPos> hendels = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.hendels(helper, centrale);
        helper.assertTrue(hendels.size() == 2, "a lever in front of each centrale wheel: " + hendels);
        helper.assertTrue(nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.hendels(helper, List.of(ertsrad)).size() == 1, "and one at the ore line");
        List<BlockPos> lampen = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.lampen(helper, g);
        List<BlockPos> dorpslampen = lampen.stream().filter(l -> l.distManhattan(ertsrad) > 3).toList();
        helper.assertTrue(dorpslampen.size() == 18, "18 lamps on the centrale: " + dorpslampen.size());
        BlockPos midden = BlockPos.containing(helper.getBounds().getCenter());
        BlockPos schakelaar = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.schakelaars(helper).stream()
                .min(java.util.Comparator.comparingInt(p -> p.distManhattan(midden.atY(p.getY())))).orElseThrow();
        int[] fase = {0};
        long[] sinds = {0};
        helper.succeedWhen(() -> {
            long nu = helper.getTick();
            boolean lampenAan = dorpslampen.stream().allMatch(l -> ((nl.juiced.guhs.feature.vadskracht.MachineBlockEntity) level.getBlockEntity(l)).heeftKracht());
            boolean lampenUit = dorpslampen.stream().noneMatch(l -> ((nl.juiced.guhs.feature.vadskracht.MachineBlockEntity) level.getBlockEntity(l)).heeftKracht());
            switch (fase[0]) {
                case 0 -> {   // everything burns: one guh takes a break
                    helper.assertTrue(lampenAan, "all town lamps burn first");
                    nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.trek(level, hendels.get(0));
                    fase[0] = 1;
                    sinds[0] = nu;
                    helper.fail("lever pulled");
                }
                case 1 -> {   // too little for the net: every lamp out; the ore line runs on
                    helper.assertTrue(lampenUit, "one guh's break: too little vadskracht, every town lamp out");
                    helper.assertTrue(!nl.juiced.guhs.feature.vadskracht.VadsKracht.net(level, centrale.get(1)).draait()
                            && !nl.juiced.guhs.feature.vadskracht.VadsKracht.net(level, centrale.get(0)).draait(), "the centrale's net stands still");
                    helper.assertTrue(nl.juiced.guhs.feature.vadskracht.VadsKracht.net(level, ertsrad).draait(), "the ore line runs on");
                    nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.trek(level, hendels.get(0));
                    fase[0] = 2;
                    helper.fail("lever back");
                }
                case 2 -> {   // all burns again: now the switch
                    helper.assertTrue(lampenAan, "the lamps burn again");
                    nl.juiced.guhs.feature.techbeurs.VadsschakelaarBlock.zet(level, schakelaar, false);
                    fase[0] = 3;
                    helper.fail("switch off");
                }
                case 3 -> {   // the line cut: lamps out, the wheels run
                    helper.assertTrue(lampenUit, "the switch cuts the lamps' line: every town lamp out");
                    for (BlockPos w : centrale) {
                        helper.assertTrue(nl.juiced.guhs.feature.vadskracht.VadsKracht.net(level, w).draait(), "the wheel at " + w + " runs on");
                    }
                    nl.juiced.guhs.feature.techbeurs.VadsschakelaarBlock.zet(level, schakelaar, true);
                    fase[0] = 4;
                    helper.fail("switch on");
                }
                default -> helper.assertTrue(lampenAan, "and on again");
            }
        });
    }

    /**
     * The centrale's guh belongs to the town ("Vast", no owner): right-clicking does not give it to you, and breaking the
     * wheel lets it hop out as a wild guh instead of dropping it as a picked-up guh item.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 60)
    public static void vadsmijnstadjeCentraleGuhBlijft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var g = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.zoek(helper);
        helper.assertTrue(g.wielen().size() == 3, "three Guhraden (two in the centrale, one at the ore line)");
        BlockPos wiel = centrale(helper, g).get(0);
        var be = (nl.juiced.guhs.block.entity.GuhWheelBlockEntity) level.getBlockEntity(wiel);
        helper.assertTrue(be != null && be.isVast() && be.hasGuh() && be.getGuhOwner() == null, "the town's guh runs in it, without an owner");
        net.minecraft.world.phys.AABB rond = new net.minecraft.world.phys.AABB(wiel).inflate(3);
        int voor = level.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, rond).size();
        level.destroyBlock(wiel, true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, rond).size() == voor + 1, "the guh hopped out");
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, rond.inflate(2)).stream()
                    .noneMatch(i -> i.getItem().is(nl.juiced.guhs.registry.ModItems.PICKED_UP_GUH.get())), "no picked-up guh item");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------
    // the Bijvuller (the hidden refiller of the showcase)
    // ---------------------------------------------------------------------------------------------------------------------

    /** Every block entity of this kind in the test's bounds. */
    @SuppressWarnings("unchecked")
    private static <T> List<T> alle(GameTestHelper helper, Class<T> soort) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.phys.AABB box = helper.getBounds();
        List<T> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1)) {
            if (level.getBlockState(p).hasBlockEntity() && soort.isInstance(level.getBlockEntity(p))) {
                out.add((T) level.getBlockEntity(p));
            }
        }
        return out;
    }

    private static <T extends BlockEntity> List<T> gemerkt(GameTestHelper helper, Class<T> soort) {
        return alle(helper, soort).stream().filter(be -> be.getPersistentData().contains(Bijvuller.MERK)).toList();
    }

    private static List<MinecartChest> karren(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(MinecartChest.class, helper.getBounds(),
                k -> k.isAlive() && k.getPersistentData().contains(Bijvuller.MERK));
    }

    /** The gravel in a Vadsmolen's in slot. */
    private static int gruis(VadsmolenBlockEntity m) {
        var r = m.vakken().getResource(VadsmolenBlockEntity.IN);
        return !r.isEmpty() && r.toStack().is(Items.GRAVEL) ? m.vakken().getAmountAsInt(VadsmolenBlockEntity.IN) : 0;
    }

    private static void leeg(VadsmolenBlockEntity m) {
        m.vakken().set(VadsmolenBlockEntity.IN, ItemResource.EMPTY, 0);
    }

    /**
     * The Bijvuller after a long day: the molens empty, the cart's iron gone (as 20 bars in the Mijnwerkerskist), the chest
     * full of zand, the glass oven full of glas. Nothing happens before its round; then the molens have their gravel again,
     * the chest keeps 4 bars (16 went back into the cart as ore: never more ore than there were bars) and 16 zand, the glass
     * oven 8 glas, and the cart got no ore it did not have.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 100)
    public static void vadsmijnstadjeBijvullerVult(GameTestHelper helper) {
        bijvuller(helper);
    }

    /** The same in a turned town. */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 100, rotationSteps = 1)
    public static void vadsmijnstadjeBijvullerVultGedraaid(GameTestHelper helper) {
        bijvuller(helper);
    }

    private static void bijvuller(GameTestHelper helper) {
        List<Bijvuller.Kern> beheer = alle(helper, Bijvuller.Kern.class);
        helper.assertTrue(beheer.size() == 1, "one Bijvuller under the town: " + beheer.size());
        var molens = gemerkt(helper, VadsmolenBlockEntity.class);
        var kisten = gemerkt(helper, ChestBlockEntity.class);
        var ovens = gemerkt(helper, GuhOvenBlockEntity.class);
        var karren = karren(helper);
        helper.assertTrue(molens.size() == 2 && kisten.size() == 1 && ovens.size() == 1 && karren.size() == 1,
                "its marks: 2 molens, the Mijnwerkerskist, the glass oven, the ore cart: " + molens.size() + " " + kisten.size() + " "
                        + ovens.size() + " " + karren.size());
        var kist = kisten.get(0);
        var oven = ovens.get(0);
        var kar = karren.get(0);
        int koper = Bijvuller.tel(kar, Items.RAW_COPPER), goud = Bijvuller.tel(kar, Items.RAW_GOLD);
        // a long day
        molens.forEach(VadsmijnstadjeGameTests::leeg);
        Bijvuller.neem(kar, Items.RAW_IRON, 64);
        Bijvuller.stop(kist, Items.IRON_INGOT, 20);
        Bijvuller.stop(kist, Items.SAND, 64);
        oven.setItem(2, new ItemStack(Items.GLASS, 64));
        helper.runAfterDelay(20, () -> {
            for (var m : molens) {
                helper.assertTrue(gruis(m) == 0, "nothing before its round");
            }
            helper.assertTrue(Bijvuller.tel(kar, Items.RAW_IRON) == 0, "no ore before its round");
            beheer.get(0).versnel();
            helper.runAfterDelay(3, () -> {
                for (var m : molens) {
                    helper.assertTrue(gruis(m) >= 60, "the molen at " + m.getBlockPos() + " has its gravel again: " + gruis(m));
                }
                int karIjzer = Bijvuller.tel(kar, Items.RAW_IRON);
                int kistIjzer = Bijvuller.tel(kist, Items.IRON_INGOT);
                helper.assertTrue(kistIjzer >= 4 && kistIjzer <= 5, "the chest keeps 4 bars to show: " + kistIjzer);
                helper.assertTrue(karIjzer >= 14 && karIjzer <= 16, "the other 16 went back into the cart as ore: " + karIjzer);
                helper.assertTrue(Bijvuller.tel(kar, Items.RAW_COPPER) <= koper && Bijvuller.tel(kar, Items.RAW_GOLD) <= goud,
                        "no ore out of nothing");
                int zand = Bijvuller.tel(kist, Items.SAND);
                helper.assertTrue(zand >= 16 && zand <= 18, "the chest keeps about 16 zand: " + zand);
                int glas = oven.getItem(2).is(Items.GLASS) ? oven.getItem(2).getCount() : 0;
                helper.assertTrue(glas >= 8 && glas <= 9, "the glass oven keeps 8 glas: " + glas);
                helper.succeed();
            });
        });
    }

    /**
     * What a player broke stays broken: no molen put back, a chest put in the Mijnwerkerskist's place is not touched, a
     * cart that was broken does not come back; what still stands (the Glasslijperij's molen) is looked after as before.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 60)
    public static void vadsmijnstadjeBijvullerLaatSloopMetRust(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Bijvuller.Kern> beheer = alle(helper, Bijvuller.Kern.class);
        var kist = gemerkt(helper, ChestBlockEntity.class).get(0);
        BlockPos kistPlek = kist.getBlockPos();
        var molens = new ArrayList<>(gemerkt(helper, VadsmolenBlockEntity.class));
        molens.sort(java.util.Comparator.comparingInt(m -> m.getBlockPos().distManhattan(kistPlek)));
        BlockPos weg = molens.get(0).getBlockPos();
        var blijft = molens.get(1);
        helper.assertTrue(weg.distManhattan(kistPlek) <= 3, "the ore line's molen stands by the chest");
        level.destroyBlock(weg, false);
        level.destroyBlock(kistPlek, false);
        level.setBlockAndUpdate(kistPlek, Blocks.CHEST.defaultBlockState());
        var nieuw = (ChestBlockEntity) level.getBlockEntity(kistPlek);
        Bijvuller.stop(nieuw, Items.SAND, 40);
        for (var k : karren(helper)) {
            k.discard();
        }
        leeg(blijft);
        beheer.get(0).versnel();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(level.getBlockState(weg).isAir(), "the broken molen is not put back");
            helper.assertTrue(level.getBlockEntity(kistPlek) instanceof ChestBlockEntity c && Bijvuller.tel(c, Items.SAND) >= 40,
                    "a chest a player put in its place is left alone");
            helper.assertTrue(karren(helper).isEmpty() && level.getEntitiesOfClass(MinecartChest.class, helper.getBounds()).size() == 1,
                    "no new ore cart (only the depot's)");
            helper.assertTrue(gruis(blijft) >= 60, "the Glasslijperij's molen still gets its gravel: " + gruis(blijft));
            helper.succeed();
        });
    }
}
