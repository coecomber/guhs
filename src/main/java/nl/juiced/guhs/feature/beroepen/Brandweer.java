package nl.juiced.guhs.feature.beroepen;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Brandweercommandant Blusguh (BRANDWEERGUH), by the door of the Brandweerkazerne. His job (once per player):
 * <ol>
 *   <li>Talk to him: the marshmallow campfires on the oefenterrein ({@link MarshmallowvuurBlock}, within {@link #BEREIK})
 *   flare up (the siren wails) and you get the loaned {@link BrandslangItem guh-brandslang} (step 1).</li>
 *   <li>Spray every fire out. When the last one hisses out: "HELP! Een guhtje in de boom!" - a little guhtje sits on the
 *   branch high up in the tall tree (the {@code beroepen_guhtjeplek} marker) (step 2).</li>
 *   <li>Climb the ladder and right-click the guhtje: it jumps into your arms and lands by Blusguh. Done: the fire helmet
 *   and the fire jacket. Blusguh takes his hose back.</li>
 * </ol>
 * One helper at a time (the fires are for everyone to see); the session ends by itself when its helper leaves.
 * roleData: Speler/Sinds/Laatst (the session), Vuren (the pits), Boom (the guhtje's spot), Guhtje (its UUID), Weg (when
 * the rescued guhtje goes home).
 */
public final class Brandweer implements NpcRole {
    public static final Brandweer ROLE = new Brandweer();
    public static final Beroep BEROEP = Beroep.BRANDWEER;
    /** How far from Blusguh the pits and the tree may be. */
    public static final int BEREIK = 28;
    public static final String BOOMGUHTJE = "guhs_beroepen_boomguhtje", NPC = "guhs_beroepen_npc";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.95f);
        if (BeroepenVoortgang.klaar(player, BEROEP)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.bedankt" + player.getRandom().nextInt(3));
            return;
        }
        if (BeroepenHulp.bezet(npc, player)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.bezet");
            return;
        }
        boolean mijn = player.getUUID().equals(BeroepenHulp.speler(npc));
        int stap = BeroepenVoortgang.stap(player, BEROEP);
        if (!mijn || stap == 0) {
            start(npc, player);
        } else if (stap == 1) {
            controleer(npc);
            if (BeroepenVoortgang.stap(player, BEROEP) == 1) {
                GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.nog", brandend(npc));
                geefSlang(player);
            }
        } else {
            if (guhtje(npc) == null) {
                zetGuhtje(npc);
            }
            GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.boom");
        }
    }

    /** The job starts: the fires flare up, the siren, the hose. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel level = (ServerLevel) npc.level();
        stop(npc, false);
        List<BlockPos> vuren = vuren(npc);
        if (vuren.isEmpty() || boom(npc) == null) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.niks");
            return false;
        }
        for (BlockPos p : vuren) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof MarshmallowvuurBlock) {
                level.setBlock(p, s.setValue(MarshmallowvuurBlock.VUUR, MarshmallowvuurBlock.MAX), 3);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
            }
        }
        BeroepenHulp.begin(npc, player);
        BeroepenVoortgang.zet(player, BEROEP, 1);
        level.playSound(null, npc.blockPosition(), BeroepenFeature.SIRENE.get(), SoundSource.NEUTRAL, 1.2f, 1.0f);
        GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.start", vuren.size());
        geefSlang(player);
        return true;
    }

    private static void geefSlang(ServerPlayer player) {
        if (GuhQuests.count(player, BeroepenFeature.GUH_BRANDSLANG.get()) == 0) {
            Minigames.give(player, new ItemStack(BeroepenFeature.GUH_BRANDSLANG.get()));
        }
    }

    /** The pits round Blusguh (found once, remembered). */
    public static List<BlockPos> vuren(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Vuren")) {
            List<BlockPos> ps = BeroepenHulp.zoek((ServerLevel) npc.level(), npc.blockPosition(), BEREIK, 8, 8,
                    s -> s.getBlock() instanceof MarshmallowvuurBlock);
            if (ps.isEmpty()) {
                return ps;
            }
            npc.roleData.putLongArray("Vuren", BeroepenHulp.longs(ps));
        }
        return BeroepenHulp.posities(npc.roleData, "Vuren");
    }

    /** The guhtje's spot in the tree (null: none). */
    @Nullable
    public static BlockPos boom(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Boom")) {
            List<BlockPos> ps = BeroepenHulp.zoek((ServerLevel) npc.level(), npc.blockPosition(), BEREIK, 8, 16,
                    s -> s.is(BeroepenFeature.GUHTJEPLEK.get()));
            if (ps.isEmpty()) {
                return null;
            }
            npc.roleData.putLong("Boom", ps.get(0).asLong());
        }
        return BlockPos.of(npc.roleData.getLong("Boom"));
    }

    /** How many fires still burn. */
    public static int brandend(GuhNpcEntity npc) {
        int n = 0;
        for (BlockPos p : vuren(npc)) {
            BlockState s = npc.level().getBlockState(p);
            if (s.getBlock() instanceof MarshmallowvuurBlock && s.getValue(MarshmallowvuurBlock.VUUR) > 0) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 20 == 0) {
            controleer(npc);
        }
    }

    /** Every second: all fires out -> the guhtje in the tree; the rescued guhtje goes home; a session that ran out ends. */
    public static void controleer(GuhNpcEntity npc) {
        long now = npc.level().getGameTime();
        if (npc.roleData.contains("Weg") && now >= npc.roleData.getLong("Weg")) {
            npc.roleData.remove("Weg");
            GuhEntity g = guhtje(npc);
            if (g != null) {
                ((ServerLevel) npc.level()).sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 0.3, g.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
                g.discard();
            }
            npc.roleData.remove("Guhtje");
        }
        if (BeroepenHulp.speler(npc) == null) {
            return;
        }
        ServerPlayer p = BeroepenHulp.online(npc);
        if (BeroepenHulp.verlopen(npc)) {
            if (p != null) {
                p.sendSystemMessage(Component.translatable("gui.guhs.beroepen.brandweer.verlopen").withStyle(ChatFormatting.GRAY));
                BeroepenVoortgang.zet(p, BEROEP, 0);
            }
            stop(npc, false);
            return;
        }
        if (p != null && BeroepenVoortgang.stap(p, BEROEP) == 1 && brandend(npc) == 0) {
            BeroepenVoortgang.zet(p, BEROEP, 2);
            zetGuhtje(npc);
            npc.level().playSound(null, npc.blockPosition(), BeroepenFeature.SIRENE.get(), SoundSource.NEUTRAL, 1.0f, 1.25f);
            GuhQuests.say(p, npc, "quest.guhs.beroepen.brandweer.allemaal_uit");
        }
    }

    /** The guhtje of this job (null: none now). */
    @Nullable
    public static GuhEntity guhtje(GuhNpcEntity npc) {
        if (!npc.roleData.hasUUID("Guhtje")) {
            return null;
        }
        Entity e = ((ServerLevel) npc.level()).getEntity(npc.roleData.getUUID("Guhtje"));
        return e instanceof GuhEntity g && g.isAlive() ? g : null;
    }

    /** A little guhtje, stuck up in the tree. */
    @Nullable
    public static GuhEntity zetGuhtje(GuhNpcEntity npc) {
        BlockPos plek = boom(npc);
        GuhEntity oud = guhtje(npc);
        if (oud != null) {
            oud.discard();
        }
        if (plek == null) {
            return null;
        }
        ServerLevel level = (ServerLevel) npc.level();
        GuhEntity g = ModEntities.GUH.get().create(level);
        if (g == null) {
            return null;
        }
        g.moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, npc.getYRot(), 0);
        g.setGuhScale(0.45f);
        g.setNoAi(true);
        g.setInvulnerable(true);
        g.setPersistenceRequired();
        g.getPersistentData().putBoolean(BOOMGUHTJE, true);
        g.getPersistentData().putUUID(NPC, npc.getUUID());
        g.getPersistentData().putBoolean("guhs_knuffeldal_checked", true);
        g.setCustomName(Component.translatable("entity.guhs.beroepen_boomguhtje"));
        g.setCustomNameVisible(true);
        level.addFreshEntity(g);
        level.sendParticles(ParticleTypes.CLOUD, g.getX(), g.getY() + 0.4, g.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
        npc.roleData.putUUID("Guhtje", g.getUUID());
        npc.roleData.remove("Weg");
        return g;
    }

    /**
     * A player right-clicks the guhtje in the tree: if they're the one helping Blusguh (step 2), it jumps down into their
     * arms and lands by Blusguh - the job is done. Returns true when it was rescued.
     */
    public static boolean red(ServerPlayer player, GuhEntity g) {
        if (!(player.level() instanceof ServerLevel level) || !g.getPersistentData().hasUUID(NPC)) {
            return false;
        }
        Entity e = level.getEntity(g.getPersistentData().getUUID(NPC));
        if (!(e instanceof GuhNpcEntity npc)) {
            return false;
        }
        if (!player.getUUID().equals(BeroepenHulp.speler(npc)) || BeroepenVoortgang.stap(player, BEROEP) != 2) {
            player.displayClientMessage(Component.translatable("gui.guhs.beroepen.brandweer.guhtje_wacht").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        Vec3 bij = npc.position().add(npc.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
        level.sendParticles(ParticleTypes.CLOUD, g.getX(), g.getY() + 0.3, g.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
        g.teleportTo(bij.x, npc.getY(), bij.z);
        level.sendParticles(ParticleTypes.HEART, bij.x, npc.getY() + 0.8, bij.z, 10, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, g.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.6f);
        g.getPersistentData().putBoolean(BOOMGUHTJE, false);
        g.setCustomName(Component.translatable("entity.guhs.beroepen_boomguhtje.gered"));
        GuhQuests.say(player, npc, "quest.guhs.beroepen.brandweer.gered");
        BeroepenVoortgang.rondAf(player, BEROEP, npc);
        neemSlang(player);
        BeroepenHulp.eind(npc);
        npc.roleData.putLong("Weg", level.getGameTime() + 100);
        return true;
    }

    /** Blusguh gets his hose back. */
    static void neemSlang(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(BeroepenFeature.GUH_BRANDSLANG.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** The session ends: the fires go out (unless it was a success, then they're out already), the guhtje leaves. */
    public static void stop(GuhNpcEntity npc, boolean geslaagd) {
        ServerLevel level = (ServerLevel) npc.level();
        for (BlockPos p : vuren(npc)) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof MarshmallowvuurBlock && s.getValue(MarshmallowvuurBlock.VUUR) > 0) {
                level.setBlock(p, s.setValue(MarshmallowvuurBlock.VUUR, 0), 3);
            }
        }
        if (!geslaagd) {
            GuhEntity g = guhtje(npc);
            if (g != null && g.getPersistentData().getBoolean(BOOMGUHTJE)) {
                g.discard();
                npc.roleData.remove("Guhtje");
            }
        }
        ServerPlayer p = BeroepenHulp.online(npc);
        if (p != null) {
            neemSlang(p);
        }
        BeroepenHulp.eind(npc);
    }

    /** (Tests) is this the guhtje of a job? */
    public static boolean isBoomguhtje(Entity e) {
        return e.getPersistentData().getBoolean(BOOMGUHTJE);
    }

    /** (Tests) whose job: the helper's UUID. */
    @Nullable
    public static UUID helper(GuhNpcEntity npc) {
        return BeroepenHulp.speler(npc);
    }
}
