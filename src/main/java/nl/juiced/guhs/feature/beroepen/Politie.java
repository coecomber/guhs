package nl.juiced.guhs.feature.beroepen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Inspecteur Vahoegsma (POLITIEGUH), in the hall of the Politiebureautje: "De Knabbeldief-zaak" (once per player).
 * <ol>
 *   <li>Talk to him: the town's knabbel stock is gone from the knabbelkluis! A trail of vadsige pink paw prints
 *   ({@link PootafdrukBlock}) runs from the kluis ({@code beroepen_kluisplek}) to one of the hiding places
 *   ({@code beroepen_verstopplek}, a different one each time), where the {@link KnabbeldiefMikaEntity} sits munching on
 *   the stolen sack ({@link KnabbelbuitBlock}) (step 1).</li>
 *   <li>Follow the prints. When you come close the Knabbeldief giggles "betrapt!" and runs off (a Mika never does
 *   anything else). Right-click the sack: the knabbels are found (step 2), the prints are swept up.</li>
 *   <li>Tell Vahoegsma: case closed. The police cap and the uniform.</li>
 * </ol>
 * One detective at a time. roleData: Speler/Sinds/Laatst, Kluis, Plekken (hiding places), Vorige (the last one used),
 * Spoor (the paw prints laid), Buit (where the sack is), Mika (its UUID).
 */
public final class Politie implements NpcRole {
    public static final Politie ROLE = new Politie();
    public static final Beroep BEROEP = Beroep.POLITIE;
    /** How far from Vahoegsma the kluis and the hiding places may be (the Beroepenstraat is long). */
    public static final int BEREIK = 80;
    /** A paw print every this many steps. */
    public static final int STAP = 2;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.9f);
        if (BeroepenVoortgang.klaar(player, BEROEP)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.bedankt" + player.getRandom().nextInt(3));
            return;
        }
        boolean mijn = player.getUUID().equals(BeroepenHulp.speler(npc));
        int stap = BeroepenVoortgang.stap(player, BEROEP);
        if (stap == 2) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.opgelost");
            BeroepenVoortgang.rondAf(player, BEROEP, npc);
            if (mijn) {
                stop(npc);
            }
            return;
        }
        if (BeroepenHulp.bezet(npc, player)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.bezet");
            return;
        }
        if (!mijn || stap == 0) {
            start(npc, player);
        } else {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.volg");
        }
    }

    /** The case starts: the trail, the sack, the Knabbeldief. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel level = (ServerLevel) npc.level();
        stop(npc);
        BlockPos kluis = kluis(npc);
        List<BlockPos> plekken = plekken(npc);
        if (kluis == null || plekken.isEmpty()) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.niks");
            return false;
        }
        // a different hiding place than last time, with a route to it
        List<BlockPos> kandidaten = new ArrayList<>(plekken);
        java.util.Collections.shuffle(kandidaten, new java.util.Random(level.getGameTime() ^ player.getUUID().hashCode()));
        long vorige = npc.roleData.getLongOr("Vorige", 0L);
        kandidaten.sort((a, b) -> Boolean.compare(a.asLong() == vorige, b.asLong() == vorige));
        BlockPos plek = null;
        List<BlockPos> route = List.of();
        for (BlockPos p : kandidaten) {
            route = BeroepenHulp.route(level, kluis, p, BEREIK + 16, 60000);
            if (!route.isEmpty()) {
                plek = p;
                break;
            }
        }
        if (plek == null) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.niks");
            return false;
        }
        // the paw prints: every STAP steps on the air along the route, turned the way the thief walked
        List<BlockPos> spoor = new ArrayList<>();
        for (int i = 1; i < route.size() - 1; i++) {
            if (i % STAP != 0) {
                continue;
            }
            BlockPos p = route.get(i);
            if (level.getBlockState(p).isAir()) {
                BlockPos next = route.get(i + 1);
                Direction d = Direction.getApproximateNearest(next.getX() - p.getX(), 0, next.getZ() - p.getZ());
                if (d.getAxis().isVertical()) {
                    d = Direction.NORTH;
                }
                level.setBlock(p, BeroepenFeature.POOTAFDRUK.get().defaultBlockState().setValue(PootafdrukBlock.FACING, d), 3);
                spoor.add(p);
            }
        }
        level.setBlock(plek, BeroepenFeature.KNABBELBUIT.get().defaultBlockState(), 3);
        KnabbeldiefMikaEntity mika = BeroepenFeature.KNABBELDIEF_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mika != null) {
            BlockPos bij = plek;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (BeroepenHulp.staanbaar(level, plek.relative(d))) {
                    bij = plek.relative(d);
                    break;
                }
            }
            mika.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
            mika.buit(plek);
            level.addFreshEntity(mika);
            npc.roleData.store("Mika", UUIDUtil.CODEC, mika.getUUID());
        }
        npc.roleData.putLongArray("Spoor", BeroepenHulp.longs(spoor));
        npc.roleData.putLong("Buit", plek.asLong());
        npc.roleData.putLong("Vorige", plek.asLong());
        BeroepenHulp.begin(npc, player);
        BeroepenVoortgang.zet(player, BEROEP, 1);
        level.playSound(null, npc.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.8f, 1.2f);
        GuhQuests.say(player, npc, "quest.guhs.beroepen.politie.start");
        return true;
    }

    /** The empty knabbelkluis (where the trail begins; found once, remembered). */
    @Nullable
    public static BlockPos kluis(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Kluis")) {
            List<BlockPos> ps = BeroepenHulp.zoek((ServerLevel) npc.level(), npc.blockPosition(), 24, 8, 8, s -> s.is(BeroepenFeature.KLUISPLEK.get()));
            if (ps.isEmpty()) {
                return null;
            }
            npc.roleData.putLong("Kluis", ps.get(0).asLong());
        }
        return BlockPos.of(npc.roleData.getLongOr("Kluis", 0L));
    }

    /** The Knabbeldief's hiding places (found once, remembered; a sack lying there still counts as one). */
    public static List<BlockPos> plekken(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Plekken")) {
            List<BlockPos> ps = BeroepenHulp.zoek((ServerLevel) npc.level(), npc.blockPosition(), BEREIK, 8, 8,
                    s -> s.is(BeroepenFeature.VERSTOPPLEK.get()) || s.is(BeroepenFeature.KNABBELBUIT.get()));
            if (ps.isEmpty()) {
                return ps;
            }
            npc.roleData.putLongArray("Plekken", BeroepenHulp.longs(ps));
        }
        return BeroepenHulp.posities(npc.roleData, "Plekken");
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 20 == 0 && BeroepenHulp.speler(npc) != null && BeroepenHulp.verlopen(npc)) {
            ServerPlayer p = BeroepenHulp.online(npc);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("gui.guhs.beroepen.politie.verlopen").withStyle(ChatFormatting.GRAY));
                if (BeroepenVoortgang.stap(p, BEROEP) == 1) {
                    BeroepenVoortgang.zet(p, BEROEP, 0);
                }
            }
            stop(npc);
        }
    }

    /**
     * The sack of knabbels is found (right-clicked) by this player: step 2 when it's their case. The sack goes (the hiding
     * place is free again), the prints are swept up, the Knabbeldief runs if it's still there.
     */
    public static boolean gevonden(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (BeroepenVoortgang.stap(player, BEROEP) != 1) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.politie.niet_van_jou").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        level.setBlock(pos, BeroepenFeature.VERSTOPPLEK.get().defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.8f);
        BeroepenVoortgang.zet(player, BEROEP, 2);
        player.sendSystemMessage(Component.translatable("gui.guhs.beroepen.politie.gevonden").withStyle(ChatFormatting.GOLD));
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(BEREIK + 16),
                n -> n.getKind() == GuhNpcEntity.Kind.POLITIEGUH && n.roleData.getLongOr("Buit", 0L) == pos.asLong() && n.roleData.contains("Buit"))) {
            veeg(npc);
            KnabbeldiefMikaEntity mika = mika(npc);
            if (mika != null) {
                mika.schrik(player);
            }
            npc.roleData.remove("Buit");
        }
        return true;
    }

    @Nullable
    static KnabbeldiefMikaEntity mika(GuhNpcEntity npc) {
        if (!npc.roleData.read("Mika", UUIDUtil.CODEC).isPresent()) {
            return null;
        }
        Entity e = ((ServerLevel) npc.level()).getEntity(npc.roleData.read("Mika", UUIDUtil.CODEC).orElseThrow());
        return e instanceof KnabbeldiefMikaEntity m && m.isAlive() ? m : null;
    }

    /** Sweeps up the paw prints (only the ones that are still there). */
    static void veeg(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        for (BlockPos p : BeroepenHulp.posities(npc.roleData, "Spoor")) {
            if (level.getBlockState(p).is(BeroepenFeature.POOTAFDRUK.get())) {
                level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        npc.roleData.remove("Spoor");
    }

    /** The case ends (solved, or its detective left): prints swept up, the sack back to a hiding place, the thief gone. */
    public static void stop(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        veeg(npc);
        if (npc.roleData.contains("Buit")) {
            BlockPos buit = BlockPos.of(npc.roleData.getLongOr("Buit", 0L));
            BlockState s = level.getBlockState(buit);
            if (s.is(BeroepenFeature.KNABBELBUIT.get())) {
                level.setBlock(buit, BeroepenFeature.VERSTOPPLEK.get().defaultBlockState(), 3);
            }
            npc.roleData.remove("Buit");
        }
        KnabbeldiefMikaEntity mika = mika(npc);
        if (mika != null) {
            mika.poef();
        }
        npc.roleData.remove("Mika");
        BeroepenHulp.eind(npc);
    }

    /** (Tests) the paw prints of the current case. */
    public static List<BlockPos> spoor(GuhNpcEntity npc) {
        return BeroepenHulp.posities(npc.roleData, "Spoor");
    }

    /** (Tests) where the sack lies now (null: none). */
    @Nullable
    public static BlockPos buit(GuhNpcEntity npc) {
        return npc.roleData.contains("Buit") ? BlockPos.of(npc.roleData.getLongOr("Buit", 0L)) : null;
    }
}
