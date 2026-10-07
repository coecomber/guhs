package nl.juiced.guhs.feature.guhpixel.among;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.among.model.Kleur;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.Tekst;

/**
 * The three ship things as decoration at home (bought from the Verkoper-guh), each with its own little joke:
 * <ul>
 *   <li>the <b>Noodknop</b> calls your own guhs nearby to a meeting: they walk up, sit around the button, go through an
 *   agenda of three points and decide nothing (unless one of them wears the red space suit: then Rood is sus). A guh that
 *   sits, rides, sleeps in a huisje or is busy elsewhere stays where it is;</li>
 *   <li>out of the <b>Ventilatieluik</b> a guh in a space suit peeks now and then (and when you click it): a stand-in that
 *   looks around and ducks away again;</li>
 *   <li>the <b>Taakjes-paneel</b> opens one of the eight task mini-games, just for fun ("Beloning: niks, njeg").</li>
 * </ul>
 * Nothing here is saved: a meeting that is running when the server stops is simply over.
 */
public final class AmongThuis {
    public static final double STRAAL = 24.0;
    public static final int MAX_GUHS = 8, AGENDA_PUNTEN = 3, AGENDA_TEKSTEN = 8, BESLUITEN = 5;
    /** How long a meeting takes: walking up, then a line every so often, in ticks. */
    public static int LOOPTIJD = 100, REGELTIJD = 70, AFKOEL = 200, GLUURTIJD = 70;

    private static final class Vergadering {
        final GlobalPos plek;
        final UUID speler;
        final List<UUID> guhs = new ArrayList<>();
        final int[] punten = new int[AGENDA_PUNTEN];
        int tick;

        Vergadering(GlobalPos plek, UUID speler) {
            this.plek = plek;
            this.speler = speler;
        }
    }

    private static final Map<GlobalPos, Vergadering> LOPEND = new HashMap<>();
    private static final Map<GlobalPos, Long> AFKOELEN = new HashMap<>();
    private static final Random RNG = new Random();

    // --- the Noodknop ------------------------------------------------------------------------------------------------------

    /** The guhs of this player that can come to a meeting at this spot. */
    static List<GuhEntity> beschikbaar(ServerLevel level, BlockPos pos, ServerPlayer p) {
        List<GuhEntity> uit = new ArrayList<>();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(STRAAL, 8, STRAAL))) {
            if (guh.getType() == ModEntities.GUH.get() && guh.isAlive() && guh.isOwnedBy(p) && !guh.isOrderedToSit() && GuhKiezer.bezet(guh) == null
                    && !GuhHooks.isBezig(guh) && uit.size() < MAX_GUHS) {
                uit.add(guh);
            }
        }
        return uit;
    }

    /** A click on a Noodknop at home. True when a meeting started. */
    public static boolean vergadering(ServerLevel level, BlockPos pos, ServerPlayer p) {
        GlobalPos plek = GlobalPos.of(level.dimension(), pos.immutable());
        if (LOPEND.containsKey(plek)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis.vergadering.bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Long tot = AFKOELEN.get(plek);
        if (tot != null && tot > level.getGameTime()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis.vergadering.net_geweest").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        List<GuhEntity> guhs = beschikbaar(level, pos, p);
        level.playSound(null, pos, AmongSlice.GELUID_VERGADERING.get(), SoundSource.BLOCKS, 0.8f, 1f);
        if (guhs.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis.noodknop").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Vergadering v = new Vergadering(plek, p.getUUID());
        for (GuhEntity guh : guhs) {
            v.guhs.add(guh.getUUID());
        }
        List<Integer> teksten = new ArrayList<>();
        for (int i = 0; i < AGENDA_TEKSTEN; i++) {
            teksten.add(i);
        }
        java.util.Collections.shuffle(teksten, RNG);
        for (int i = 0; i < AGENDA_PUNTEN; i++) {
            v.punten[i] = teksten.get(i);
        }
        LOPEND.put(plek, v);
        p.sendSystemMessage(Component.translatable("gui.guhs.among.thuis.vergadering.begin", guhs.size()).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public static boolean vergadert(ServerLevel level, BlockPos pos) {
        return LOPEND.containsKey(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    static void tick(MinecraftServer server) {
        if (LOPEND.isEmpty()) {
            return;
        }
        for (Iterator<Vergadering> it = LOPEND.values().iterator(); it.hasNext();) {
            Vergadering v = it.next();
            ServerLevel level = server.getLevel(v.plek.dimension());
            if (level == null || !tick(level, v)) {
                it.remove();
                if (level != null) {
                    AFKOELEN.put(v.plek, level.getGameTime() + AFKOEL);
                    for (UUID id : v.guhs) {
                        if (level.getEntity(id) instanceof GuhEntity guh) {
                            GuhHooks.bezig(guh, 0);
                        }
                    }
                }
            }
        }
    }

    /** One tick of a meeting; false when it is over. */
    private static boolean tick(ServerLevel level, Vergadering v) {
        BlockPos pos = v.plek.pos();
        if (!level.isLoaded(pos) || !level.getBlockState(pos).is(AmongSlice.NOODKNOP.get())) {
            return false;
        }
        v.tick++;
        Vec3 midden = Vec3.atBottomCenterOf(pos);
        List<GuhEntity> aanwezig = new ArrayList<>();
        int n = v.guhs.size();
        for (int i = 0; i < n; i++) {
            if (!(level.getEntity(v.guhs.get(i)) instanceof GuhEntity guh) || !guh.isAlive() || guh.isOrderedToSit() || guh.isPassenger()) {
                continue;
            }
            aanwezig.add(guh);
            GuhHooks.bezig(guh, 40);
            // every guh has its own spot in a circle around the button
            double hoek = Math.PI * 2 * i / n;
            double x = midden.x + Math.cos(hoek) * 1.9, z = midden.z + Math.sin(hoek) * 1.9;
            if (guh.distanceToSqr(x, guh.getY(), z) > 1.2) {
                if ((v.tick + i) % 10 == 0) {
                    guh.getNavigation().moveTo(x, midden.y, z, 1.15);
                }
            } else {
                guh.getNavigation().stop();
                guh.getLookControl().setLookAt(midden.x, midden.y + 0.4, midden.z);
            }
        }
        if (aanwezig.isEmpty()) {
            return false;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(v.speler);
        boolean hoort = p != null && p.level() == level && p.distanceToSqr(midden) < 40 * 40;
        int na = v.tick - LOOPTIJD;
        if (na >= 0 && na % REGELTIJD == 0) {
            int punt = na / REGELTIJD;
            GuhEntity spreker = aanwezig.get(punt % aanwezig.size());
            Vec3 bij = spreker.position().add(0, 1.0, 0);
            level.sendParticles(ParticleTypes.NOTE, bij.x, bij.y, bij.z, 1, 0.1, 0.1, 0.1, 0);
            if (punt < AGENDA_PUNTEN) {
                if (hoort) {
                    p.sendSystemMessage(Component.translatable("gui.guhs.among.thuis.vergadering.regel", spreker.getDisplayName(),
                            Component.translatable("gui.guhs.among.thuis.agenda." + v.punten[punt], punt + 1)).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                // the decision: nothing, unless somebody wears the red space suit
                GuhEntity rood = null;
                for (GuhEntity guh : aanwezig) {
                    if (guh.getClothes(GuhClothes.Slot.BODY) == GuhClothes.AMONG_RUIMTEPAKJE_ROOD) {
                        rood = guh;
                    }
                }
                if (hoort) {
                    Component besluit = rood != null ? Component.translatable("gui.guhs.among.thuis.besluit.rood", rood.getDisplayName())
                            : Component.translatable("gui.guhs.among.thuis.besluit." + RNG.nextInt(BESLUITEN));
                    p.sendSystemMessage(Component.translatable("gui.guhs.among.thuis.vergadering.regel", spreker.getDisplayName(), besluit).withStyle(ChatFormatting.GOLD));
                }
                level.playSound(null, pos, AmongSlice.GELUID_STEM.get(), SoundSource.BLOCKS, 0.8f, 1f);
                for (GuhEntity guh : aanwezig) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + 0.8, guh.getZ(), 3, 0.3, 0.2, 0.3, 0);
                }
                return false;
            }
        }
        return true;
    }

    static void leeg() {
        LOPEND.clear();
        AFKOELEN.clear();
    }

    // --- the Ventilatieluik --------------------------------------------------------------------------------------------------

    /** A guh in a space suit peeks out of this vent for a moment; false when one is peeking already (or in a round's ship). */
    public static boolean gluur(ServerLevel level, BlockPos pos) {
        if (Guhpixel.in(level, pos) || !level.getEntitiesOfClass(AmongGuhEntity.class, new AABB(pos).inflate(0.5, 1, 0.5)).isEmpty()) {
            return false;
        }
        AmongGuhEntity e = AmongSlice.AMONG_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return false;
        }
        e.setKleur(Kleur.op(RNG.nextInt(Kleur.values().length)));
        float yaw = RNG.nextInt(4) * 90f;
        e.snapTo(pos.getX() + 0.5, pos.getY() - AmongGuhEntity.GLUUR_DIEP, pos.getZ() + 0.5, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.gluur(GLUURTIJD);
        level.addFreshEntity(e);
        level.playSound(null, pos, AmongSlice.GELUID_LUIK.get(), SoundSource.BLOCKS, 0.5f, 1.1f);
        return true;
    }

    // --- the Taakjes-paneel --------------------------------------------------------------------------------------------------

    private static final Map<UUID, CompoundTag> OPGAVEN = new HashMap<>();
    private static final Map<UUID, String> OPGAVE_SOORT = new HashMap<>();

    /** Opens one of the eight task mini-games on this player's screen, for fun. */
    public static String taak(ServerPlayer p) {
        return taak(p, Taken.SOORTEN.get(RNG.nextInt(Taken.SOORTEN.size())));
    }

    /** (Also the dev command) this kind of task on the player's screen. */
    public static String taak(ServerPlayer p, String soort) {
        CompoundTag opgave = TaakSoorten.van(soort).opgave(RNG, 0);
        OPGAVEN.put(p.getUUID(), opgave);
        OPGAVE_SOORT.put(p.getUUID(), soort);
        CompoundTag data = new CompoundTag();
        data.putInt("Paneel", -1);
        data.putBoolean("Thuis", true);
        data.putString("Soort", soort);
        data.putInt("Stap", 1);
        data.putInt("Stappen", 1);
        Tekst.put(data, "Kamer", Component.empty());
        data.put("Opgave", opgave.copy());
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.TAAK, data));
        return soort;
    }

    /** What the home panel asked this player (tests). */
    public static CompoundTag opgave(ServerPlayer p) {
        return OPGAVEN.getOrDefault(p.getUUID(), new CompoundTag());
    }

    /** The home panel's task is done: a compliment, and no reward at all. */
    public static boolean taakKlaar(ServerPlayer p, CompoundTag resultaat) {
        CompoundTag opgave = OPGAVEN.remove(p.getUUID());
        String soort = OPGAVE_SOORT.remove(p.getUUID());
        if (opgave == null || soort == null || !TaakSoorten.van(soort).geldig(opgave, resultaat)) {
            return false;
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.among.thuis.taak_klaar").withStyle(ChatFormatting.GREEN));
        return true;
    }

    static void weg(ServerPlayer p) {
        OPGAVEN.remove(p.getUUID());
        OPGAVE_SOORT.remove(p.getUUID());
    }

    private AmongThuis() {
    }
}
