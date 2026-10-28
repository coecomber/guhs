package nl.juiced.guhs.feature.techbeurs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import nl.juiced.guhs.feature.techbezorg.StepstationBlockEntity;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;

/**
 * {@code guhs:beurs_beheer}: the hidden caretaker of the Guhtechniek-beurs (one under the floor of every copy). A showcase
 * must run for ever without a player: machines eat their input and fill their output. Every second it looks after the
 * block entities around it that carry the mark {@code NeoForgeData.guhs_beurs} (written by the template, so it turns with
 * the building):
 * <ul>
 *   <li>{@code Vul: [{id, count}]}: keeps at least half of each in it (put in from above: a Guhoven's top is its input);</li>
 *   <li>{@code Leeg: n}: as soon as n or more items can be taken out (from below: the out slots), takes them all ("sold");</li>
 *   <li>{@code LeegSaus: mB}: the same for sauce (a Sausvat that is nearly full is emptied);</li>
 *   <li>{@code VulSaus: mB} (1.8.0): keeps at least half of that much kaassaus in it (a Saustank, the Kaaskas, the
 *       Knuffelsteen-generator);</li>
 *   <li>{@code Station: 1b}: a Stepstation adopts the Haltepaaltjes of its stand (a template cannot link them: they hold
 *       absolute positions);</li>
 *   <li>{@code Hak: ticks}: a Plantagebak whose tree has stood this long is chopped, so it plants a new one;</li>
 *   <li>{@code Guhs: n}: a Knuffelgenerator or Disco-dynamo keeps n beursguhs on its floor: the guhs of the template
 *       (they come without AI so they cannot wander off before this) are tamed for the beurs ({@link #EIGENAAR}), told to sit
 *       and put on the floor, one per block of it; a guh that got off is put back.</li>
 * </ul>
 * It finds them through the block entities of the loaded chunks within {@link #BEREIK} (again every {@link #OPNIEUW}
 * ticks), never through a block scan.
 * <p>
 * 1.8.0: the first time it ticks it also stamps the annex, the Fabriekshal and the Koepel ({@link BeursUitbouw}; it keeps
 * the version it stamped, so an old beurs gets them too) and protects them.
 */
public class BeursBeheer extends BaseEntityBlock {
    public static final MapCodec<BeursBeheer> CODEC = simpleCodec(BeursBeheer::new);
    /** How far around it (horizontally) it looks; up and down {@link #HOOG} (1.8.0: plus the boxes of the annex and the floors). */
    public static final int BEREIK = 48, HOOG = 24;
    static final int ELKE = 20, OPNIEUW = 600;
    public static final String MERK = "guhs_beurs";
    /** The owner of the beurs' own guhs (nobody: a fixed UUID no player has). */
    public static final java.util.UUID EIGENAAR = java.util.UUID.fromString("6775682d-6265-7572-732d-000000000001");

    public BeursBeheer(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l, pos, s, be) -> {
            if (be instanceof Kern k && l instanceof ServerLevel server) {
                k.tick(server);
            }
        };
    }

    /** The caretaker's block entity (it saves only the version of the stamped annex + floors: it finds everything again). */
    public static class Kern extends BlockEntity {
        private final List<BlockPos> gemerkt = new ArrayList<>();
        private final Set<BlockPos> geadopteerd = new HashSet<>();
        private final Map<BlockPos, Integer> boomTijd = new HashMap<>();
        private int teller;
        private int zoekTeller;
        /** The {@link BeursUitbouw#VERSIE} stamped here (saved). */
        private int uitbouw;
        private boolean uitbouwBekeken;
        /** The hall it stands in, once {@link #uitbouw} found it (then it also looks after the annex and the floors). */
        @javax.annotation.Nullable
        private BeursUitbouw.Hal hal;

        public Kern(BlockPos pos, BlockState state) {
            super(TechbeursFeature.BEURS_BEHEER_BE.get(), pos, state);
        }

        void tick(ServerLevel level) {
            if (++teller < ELKE) {
                return;
            }
            teller = 0;
            if (!uitbouwBekeken && !(level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) {
                uitbouwBekeken = true;
                uitbouw(level);
            }
            if (zoekTeller <= 0 || gemerkt.isEmpty()) {
                zoek(level);
                zoekTeller = OPNIEUW;
            }
            zoekTeller -= ELKE;
            for (BlockPos p : gemerkt) {
                if (level.isLoaded(p) && level.getBlockEntity(p) instanceof BlockEntity be && !be.isRemoved()) {
                    try {
                        verzorg(level, be);
                    } catch (RuntimeException e) {
                        // (one stand that misbehaves must not stop the others)
                    }
                }
            }
        }

        /**
         * The annex and the upper floors ({@link BeursUitbouw}): stamped when this caretaker's version is older, protected
         * after every load. (Game tests call it themselves: the game test server leaves it alone.) True when it stamped.
         */
        public boolean uitbouw(ServerLevel level) {
            hal = BeursUitbouw.vind(level, worldPosition);
            if (hal == null) {
                return false;
            }
            boolean gestempeld = false;
            if (uitbouw < BeursUitbouw.VERSIE && BeursUitbouw.bouw(level, hal, uitbouw == 0)) {
                uitbouw = BeursUitbouw.VERSIE;
                setChanged();
                gestempeld = true;
                zoekTeller = 0;
                gemerkt.clear();
            }
            if (uitbouw > 0) {
                BeursUitbouw.bescherm(level, hal);
            } else {
                hal = null;
            }
            return gestempeld;
        }

        /** (Tests) the version of the annex + floors stamped here. */
        public int uitbouwVersie() {
            return uitbouw;
        }

        @Override
        protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
            super.loadAdditional(in);
            uitbouw = in.getIntOr("Uitbouw", 0);
        }

        @Override
        protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput uit) {
            super.saveAdditional(uit);
            uit.putInt("Uitbouw", uitbouw);
        }

        /** Every marked block entity in the loaded chunks around it. */
        void zoek(ServerLevel level) {
            gemerkt.clear();
            int cx0 = (worldPosition.getX() - BEREIK) >> 4, cx1 = (worldPosition.getX() + BEREIK) >> 4;
            int cz0 = (worldPosition.getZ() - BEREIK) >> 4, cz1 = (worldPosition.getZ() + BEREIK) >> 4;
            for (int cx = cx0; cx <= cx1; cx++) {
                for (int cz = cz0; cz <= cz1; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        BlockPos p = be.getBlockPos();
                        if (Math.abs(p.getX() - worldPosition.getX()) <= BEREIK && Math.abs(p.getZ() - worldPosition.getZ()) <= BEREIK
                                && Math.abs(p.getY() - worldPosition.getY()) <= HOOG && be.getPersistentData().contains(MERK)) {
                            gemerkt.add(p.immutable());
                        }
                    }
                }
            }
            if (hal != null) {
                for (BeursUitbouw.Stuk s : BeursUitbouw.STUKKEN) {
                    net.minecraft.world.level.levelgen.structure.BoundingBox doos = hal.doos(s);
                    for (int cx = doos.minX() >> 4; cx <= doos.maxX() >> 4; cx++) {
                        for (int cz = doos.minZ() >> 4; cz <= doos.maxZ() >> 4; cz++) {
                            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                            if (chunk == null) {
                                continue;
                            }
                            for (BlockEntity be : chunk.getBlockEntities().values()) {
                                BlockPos p = be.getBlockPos();
                                if (doos.isInside(p) && be.getPersistentData().contains(MERK) && !gemerkt.contains(p)) {
                                    gemerkt.add(p.immutable());
                                }
                            }
                        }
                    }
                }
            }
        }

        /** (Tests) what it looks after. */
        public List<BlockPos> gemerkt() {
            return List.copyOf(gemerkt);
        }

        private void verzorg(ServerLevel level, BlockEntity be) {
            CompoundTag r = be.getPersistentData().getCompoundOrEmpty(MERK);
            BlockPos p = be.getBlockPos();
            ListTag vul = r.getListOrEmpty("Vul");
            if (!vul.isEmpty()) {
                ResourceHandler<ItemResource> in = Kisten.van(level, p, Direction.UP);
                if (in != null) {
                    for (Tag t : vul) {
                        if (t instanceof CompoundTag c) {
                            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(c.getStringOr("id", "minecraft:air")));
                            int wil = c.getIntOr("count", 16);
                            long heeft = Kisten.tel(in, s -> s.is(item));
                            if (heeft * 2 < wil) {
                                Kisten.stop(in, new ItemStack(item, (int) (wil - heeft)));
                            }
                        }
                    }
                }
            }
            int leeg = r.getIntOr("Leeg", 0);
            if (leeg > 0) {
                ResourceHandler<ItemResource> uit = Kisten.van(level, p, Direction.DOWN);
                if (uit != null && Kisten.tel(uit, s -> true) >= leeg) {
                    for (int i = 0; i < 64 && !Kisten.neem(uit, s -> true, 64).isEmpty(); i++) {
                        // ("sold to the visitors")
                    }
                }
            }
            int vulSaus = r.getIntOr("VulSaus", 0);
            if (vulSaus > 0) {
                ResourceHandler<FluidResource> tank = be instanceof nl.juiced.guhs.feature.techsaus.SausMachineBlockEntity m ? m.alleTanks()
                        : level.getCapability(Capabilities.Fluid.BLOCK, p, null);
                if (tank != null) {
                    FluidResource kaassaus = nl.juiced.guhs.feature.vadskracht.Sauzen.kaassaus();
                    long heeft = 0;
                    for (int i = 0; i < tank.size(); i++) {
                        if (tank.getResource(i).equals(kaassaus)) {
                            heeft += tank.getAmountAsLong(i);
                        }
                    }
                    if (heeft * 2 < vulSaus) {
                        nl.juiced.guhs.feature.vadskracht.Sauzen.stop(tank, kaassaus, (int) (vulSaus - heeft), false);
                    }
                }
            }
            int leegSaus = r.getIntOr("LeegSaus", 0);
            if (leegSaus > 0) {
                ResourceHandler<FluidResource> tank = level.getCapability(Capabilities.Fluid.BLOCK, p, null);
                if (tank != null) {
                    long totaal = 0;
                    for (int i = 0; i < tank.size(); i++) {
                        totaal += tank.getAmountAsLong(i);
                    }
                    if (totaal >= leegSaus) {
                        try (Transaction tx = Transaction.openRoot()) {
                            for (int i = 0; i < tank.size(); i++) {
                                FluidResource f = tank.getResource(i);
                                if (!f.isEmpty()) {
                                    tank.extract(i, f, (int) Math.min(Integer.MAX_VALUE, tank.getAmountAsLong(i)), tx);
                                }
                            }
                            tx.commit();
                        }
                    }
                }
            }
            if (r.getBooleanOr("Station", false) && be instanceof StepstationBlockEntity station && !geadopteerd.contains(p)) {
                station.adopteer(level, true);
                geadopteerd.add(p.immutable());
            }
            int guhs = r.getIntOr("Guhs", 0);
            if (guhs > 0) {
                zetGuhs(level, p, be.getBlockState(), guhs);
            }
            int hak = r.getIntOr("Hak", 0);
            if (hak > 0 && be instanceof PlantagebakBlockEntity bak) {
                if (bak.stand() == PlantagebakBlockEntity.Stand.BOOM) {
                    int t = boomTijd.merge(p.immutable(), ELKE, Integer::sum);
                    if (t >= hak) {
                        bak.hak(null);
                        boomTijd.remove(p);
                    }
                } else {
                    boomTijd.remove(p);
                }
            }
        }

        /** The beursguhs of a Knuffelgenerator / Disco-dynamo: tamed, sitting, one on each block of its floor. */
        private void zetGuhs(ServerLevel level, BlockPos kern, BlockState state, int n) {
            List<BlockPos> vloer = new ArrayList<>();
            vloer.add(kern);
            vloer.addAll(nl.juiced.guhs.feature.vadskracht.Meerblok.delen(kern, state));
            vloer.sort(java.util.Comparator.comparingInt((BlockPos b) -> b.getY()).thenComparing(b -> b.distManhattan(kern)));
            List<nl.juiced.guhs.entity.GuhEntity> guhs = level.getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class,
                    new net.minecraft.world.phys.AABB(kern).inflate(5, 2, 5), g -> g.isAlive() && (!g.isTame() || EIGENAAR.equals(eigenaar(g))));
            guhs.sort(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(kern.getCenter())));
            double hoogte = state.getBlock() instanceof nl.juiced.guhs.feature.techbron.DiscoDynamoBlock ? 0.25 : 0.375;
            for (int i = 0; i < Math.min(n, guhs.size()); i++) {
                nl.juiced.guhs.entity.GuhEntity g = guhs.get(i);
                if (!g.isTame() || g.isNoAi()) {
                    g.setNoAi(false);
                    g.setTame(true, false);
                    g.setOwnerUUID(EIGENAAR);
                    g.setWandering(false);
                    g.setPersistenceRequired();
                }
                if (!g.isOrderedToSit()) {
                    g.setOrderedToSit(true);
                    g.setInSittingPose(true);
                }
                BlockPos plek = vloer.get(i % vloer.size());
                if (!g.blockPosition().equals(plek)) {
                    g.snapTo(plek.getX() + 0.5, plek.getY() + hoogte, plek.getZ() + 0.5, g.getYRot(), 0);
                }
            }
        }

        @javax.annotation.Nullable
        private static java.util.UUID eigenaar(nl.juiced.guhs.entity.GuhEntity g) {
            var ref = g.getOwnerReference();
            return ref == null ? null : ref.getUUID();
        }
    }
}
