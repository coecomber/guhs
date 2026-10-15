package nl.juiced.guhs.feature.park.plek;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.4.2 (park-plekjes b): the sulking Mika (NPC MOKMIKA) on the bench of the plekje mika_bankje, right under the sign
 * "Verboden voor Mika's".
 * <ul>
 *   <li>Talk to him: he grumbles (four lines). He is a Mika: come too close (sit next to him, stand in front of his nose)
 *       and he shoves you away ({@link Duwtje}: knockback only, never damage), "Dit is MIJN bankje!".</li>
 *   <li>Right-click him with a kaasknabbel: he takes it, is very happy, and leaves to find a bench where Mika's are allowed
 *       (hearts, a hop, a poof). The hidden advancement parkb_mika_blij.</li>
 *   <li>He is away for the rest of the day, per park copy ({@link Weg}, SavedData guhs:parkb_mika_weg: the copy's Bezetting
 *       tag -> the day he left); the next day (the overworld's day) {@link Bezetting} brings him back ({@link #maak}).</li>
 * </ul>
 */
public final class MikaBankje implements NpcRole {
    /** RoleData: the game time he leaves (after his thanks). */
    public static final String WEG_OM = "parkb_weg_om";
    public static final int ZINNEN = 4, VERTREK = 50;
    /** How close is too close (blocks), and how often he shoves the same player (ticks). */
    public static final double TE_DICHT = 1.35;
    public static final int DUW_PAUZE = 60;
    private static final String Q = "quest.guhs.parkb.mika.";
    /** (not saved) player -> the game time of their last shove. */
    private static final Map<UUID, Long> GEDUWD = new HashMap<>();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        if (npc.roleData.contains(WEG_OM)) {
            return;   // (he is already leaving)
        }
        ItemStack hand = p.getMainHandItem();
        if (hand.is(ModItems.KAAS_KNABBELS.get())) {
            blij(npc, p, hand);
            return;
        }
        GuhQuests.say(p, npc, Q + "mok" + p.getRandom().nextInt(ZINNEN));
        npc.level().playSound(null, npc.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 0.6f);
    }

    /** A kaasknabbel: he is happy and goes. */
    public static void blij(GuhNpcEntity npc, ServerPlayer p, ItemStack knabbel) {
        ServerLevel level = (ServerLevel) npc.level();
        knabbel.consume(1, p);
        GuhQuests.say(p, npc, Q + "blij");
        level.playSound(null, npc.blockPosition(), ModSounds.GUH_EAT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        level.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.0, npc.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
        npc.setDeltaMovement(npc.getDeltaMovement().add(0, 0.4, 0));
        npc.hurtMarked = true;
        npc.roleData.putLong(WEG_OM, level.getGameTime() + VERTREK);
        Weg.get(level).zet(sleutel(level, npc), DuivenOma.dag(level));
        GuhAdvancements.grant(p, "parkb_mika_blij");
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }
        if (npc.roleData.contains(WEG_OM)) {
            if (level.getGameTime() >= npc.roleData.getLongOr(WEG_OM, 0L)) {
                vertrek(level, npc);
            }
            return;
        }
        if ((npc.tickCount + npc.getId()) % 10 != 0) {
            return;
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(TE_DICHT, 0.5, TE_DICHT))) {
            if (p.isSpectator() || !Duwtje.mag(p)) {
                continue;
            }
            Long laatst = GEDUWD.get(p.getUUID());
            if (laatst != null && level.getGameTime() - laatst < DUW_PAUZE && level.getGameTime() >= laatst) {
                continue;
            }
            GEDUWD.put(p.getUUID(), level.getGameTime());
            duw(npc, p);
        }
    }

    /** "Dit is MIJN bankje!": off the bench, a shove away from him (never damage). */
    public static void duw(GuhNpcEntity npc, ServerPlayer p) {
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Duwtje.duw(p, p.position().subtract(npc.position()), 0.7);
        GuhQuests.say(p, npc, Q + "duw");
        npc.level().playSound(null, npc.blockPosition(), ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 0.5f);
    }

    /** He leaves: a poof, and the players near read that he is happy. */
    static void vertrek(ServerLevel level, GuhNpcEntity npc) {
        level.sendParticles(ParticleTypes.POOF, npc.getX(), npc.getY() + 0.5, npc.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.0, npc.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(16))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.mika.weg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        npc.discard();
    }

    /** The key of this Mika's park copy: his Bezetting tag ("parkb_mika@&lt;start chunk&gt;"), else his spot. */
    static String sleutel(ServerLevel level, Entity npc) {
        String tag = npc.getPersistentData().getStringOr(Bezetting.TAG, "");
        if (tag.startsWith(PlekB.MIKA_ID + "@")) {
            return tag;
        }
        StructureStart start = Bezetting.start(level, ParkFeature.STRUCTUUR, npc.blockPosition());
        return start != null ? Bezetting.tag(PlekB.MIKA_ID, start) : PlekB.MIKA_ID + "#" + npc.blockPosition().asLong();
    }

    /** (Bezetting) the Mika of a park copy, unless he left today. */
    @Nullable
    static GuhNpcEntity maak(ServerLevel level, Vec3 plek, Rotation draai) {
        StructureStart start = Bezetting.start(level, ParkFeature.STRUCTUUR, BlockPos.containing(plek));
        if (start != null && Weg.get(level).weg(Bezetting.tag(PlekB.MIKA_ID, start), DuivenOma.dag(level))) {
            return null;
        }
        return PlekB.zitNpc(level, GuhNpcEntity.Kind.MOKMIKA, plek, PlekB.MIKA_YAW, draai);
    }

    // =====================================================================================================================
    /** Which Mika left on which day (key: the park copy's tag). */
    public static class Weg extends SavedData {
        public static final SavedDataType<Weg> TYPE = GuhSavedData.tagType("parkb_mika_weg", Weg::new, Weg::load, Weg::save);
        private final Map<String, Long> dagen = new HashMap<>();

        public static Weg get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        /** Is the Mika of this copy away on this day? */
        public boolean weg(String sleutel, long dag) {
            Long d = dagen.get(sleutel);
            return d != null && d == dag;
        }

        public void zet(String sleutel, long dag) {
            dagen.put(sleutel, dag);
            setDirty();
        }

        /** (tests, /guhs parkb mika terug) every Mika back at once; how many were away. */
        public int allesTerug() {
            int n = dagen.size();
            dagen.clear();
            setDirty();
            return n;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            CompoundTag d = new CompoundTag();
            dagen.forEach(d::putLong);
            tag.put("Dagen", d);
            return tag;
        }

        private static Weg load(CompoundTag tag) {
            Weg w = new Weg();
            CompoundTag d = tag.getCompoundOrEmpty("Dagen");
            for (String k : d.keySet()) {
                w.dagen.put(k, d.getLongOr(k, -1L));
            }
            return w;
        }
    }
}
