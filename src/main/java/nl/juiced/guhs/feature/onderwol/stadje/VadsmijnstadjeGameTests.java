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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;
import nl.juiced.guhs.feature.kaasmijn.Mijnguh;
import nl.juiced.guhs.feature.mijnhuisje.MijnHulpje;
import nl.juiced.guhs.feature.onderwol.bouw.GrotBouwStructure;
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
     * 1.7.0 playtest: the Guhrad-centrale really runs in the placed template: the wheel stands whole, its guh runs, its
     * net gives vadskracht (feature/onderwol/bouw/KrachtProef).
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 200)
    public static void vadsmijnstadjeCentraleDraait(GameTestHelper helper) {
        nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.draait(helper, 1, 0, 0, g -> { });
    }

    /** The same, with the town placed turned (as the jigsaw placement does 3 times in 4): the wheel turns with it. */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 200, rotationSteps = 1)
    public static void vadsmijnstadjeCentraleDraaitGedraaid(GameTestHelper helper) {
        nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.draait(helper, 1, 0, 0, g -> { });
    }

    /**
     * The centrale's guh belongs to the town ("Vast", no owner): right-clicking does not give it to you, and breaking the
     * wheel lets it hop out as a wild guh instead of dropping it as a picked-up guh item.
     */
    @GuhTest(template = "onderwol/vadsmijnstadje/start", batch = BATCH, timeoutTicks = 60)
    public static void vadsmijnstadjeCentraleGuhBlijft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var g = nl.juiced.guhs.feature.onderwol.bouw.KrachtProef.zoek(helper);
        helper.assertTrue(g.wielen().size() == 1, "one Guhrad");
        BlockPos wiel = g.wielen().get(0);
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
}
