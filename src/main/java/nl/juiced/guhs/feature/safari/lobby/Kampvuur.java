package nl.juiced.guhs.feature.safari.lobby;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhTime;

/**
 * The kampvuuravond at the lodge (test round 3, design ronde 45): every evening a storyteller guh sits at the campfire by
 * the lodge (from {@link #VAN} to {@link #TOT}, time of day). A click: a short funny animal story ({@link #VERHALEN} of them,
 * a few new ones every real day, then they come round again), and once a day three marshmallows for the fire. Cosy, no
 * further gameplay. The storyteller goes home (a puff) when the evening is over.
 */
public final class Kampvuur {
    public static final int VAN = 11500, TOT = 14500, VERHALEN = 9, PER_DAG = 3;
    static final String TAG = "guhs_safari_verteller";
    private static final double KOP_Y = 2.35;

    public static boolean avond(long tijdVanDeDag) {
        long t = Math.floorMod(tijdVanDeDag, 24000L);
        return t >= VAN && t < TOT;
    }

    /** The story number (0..VERHALEN-1) the n-th click of a player tonight hears on real day d. */
    public static int verhaal(long dag, int n) {
        return (int) Math.floorMod(dag * PER_DAG + Math.floorMod(n, PER_DAG), VERHALEN);
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 23) {
            return;
        }
        ServerLevel level = Safari.level(server);
        Beleving b = Beleving.get();
        if (level == null || b.verteller == null || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        BlockPos plek = b.verteller;
        if (!level.hasChunk(plek.getX() >> 4, plek.getZ() >> 4) || !level.areEntitiesLoaded(ChunkPos.pack(plek))) {
            return;
        }
        Vec3 pos = Vec3.atBottomCenterOf(plek);
        List<GuhNpcEntity> er = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(3), e -> e.getKind() == GuhNpcEntity.Kind.SAFARI_VERTELLER);
        boolean avond = avond(GuhTime.timeOfDay(level));
        for (int i = avond ? 1 : 0; i < er.size(); i++) {
            GuhNpcEntity e = er.get(i);
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            e.discard();
        }
        if (!avond) {
            Scorebord.show(level, pos.add(0, KOP_Y, 0), "safari_npc_verteller", Component.empty());
            return;
        }
        if (er.isEmpty()) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc == null) {
                return;
            }
            npc.setKind(GuhNpcEntity.Kind.SAFARI_VERTELLER);
            npc.snapTo(pos.x, pos.y, pos.z, b.vertellerYaw, 0f);
            npc.setYHeadRot(b.vertellerYaw);
            npc.setYBodyRot(b.vertellerYaw);
            npc.addTag(TAG);
            level.addFreshEntity(npc);
            level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.02);
        }
        Scorebord.show(level, pos.add(0, KOP_Y, 0), "safari_npc_verteller",
                Component.translatable("entity.guhs.guh_npc.safari_verteller").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (level.getRandom().nextInt(3) == 0 && Beleving.get().kampvuur != null) {
            BlockPos v = Beleving.get().kampvuur;
            level.sendParticles(ParticleTypes.FLAME, v.getX() + 0.5, v.getY() + 0.6, v.getZ() + 0.5, 2, 0.15, 0.1, 0.15, 0.01);
        }
    }

    /** The storyteller: a story, and the day's marshmallows. */
    static final NpcRole VERTELLER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            var server = p.level().getServer();
            int n = SafariSpeler.vandaag(server, p.getUUID(), "kampvuur:verhaal");
            SafariSpeler.telDag(server, p.getUUID(), "kampvuur:verhaal", 1, 1000);
            String verhaal = "gui.guhs.safari.verteller.verhaal." + verhaal(Klok.dag(), n);
            if (SafariSpeler.telDag(server, p.getUUID(), "kampvuur:marshmallow", 1, 1) > 0) {
                Minigames.give(p, new ItemStack(LobbyItems.MARSHMALLOW.get(), 3));
                Praat.open(p, npc, null, "gui.guhs.safari.verteller.welkom", new Object[]{Component.translatable(verhaal)},
                        new Praat.Optie(1, "gui.guhs.safari.verteller.optie.nog"), new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
            } else {
                Praat.open(p, npc, null, verhaal, new Object[0],
                        new Praat.Optie(1, "gui.guhs.safari.verteller.optie.nog"), new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
            }
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (optie == 1) {
                talk(npc, p);
            }
        }
    };

    private Kampvuur() {
    }
}
