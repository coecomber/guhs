package nl.juiced.guhs.feature.ringh2;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (ring-h2): Guhvendel, the elf house of chapter 2 "De Raad van Guhrond" (structure guhs:guhvendel, template and
 * geometry: tools/features/ring_h2_bouw.py; the module's self-check compares {@link #KRING}, {@link #BEL}, {@link #MIDDEN} and
 * {@link #BEWONERS} with that file). Everything is per player (the questline {@link RingH2Feature#LIJN}):
 * <ol start="0">
 *   <li>{@link #REIS}: the narrator card the first time the player is in the Barbecuether with chapter 1 done, then the walk
 *       to Guhvendel; walking into the cirque is the arrival ({@link #aankomst}).</li>
 *   <li>{@link #WELKOM}: Guhrond on the porch of the hall.</li>
 *   <li>{@link #KENNIS}: meet the six of the fellowship, all over the house ({@link GuhvendelRol}; a flag per character, in
 *       any order, also before Guhrond's welcome).</li>
 *   <li>{@link #RAADSBEL}: ring the council bell ({@link #luid}): the cutscene of the council that argues who may eat the
 *       ring ({@link RingH2Scenes#RAAD}).</li>
 *   <li>{@link #MELDEN}: tell Guhrond that you will carry it ({@link #meldAan}): the cutscene of the fellowship
 *       ({@link RingH2Scenes#GENOOTSCHAP}), provisions once.</li>
 *   <li>{@link #VERTREK}: leave with Guhdalf ({@link #vertrek}); chapter 3 opens.</li>
 * </ol>
 * The cast stands in the template ({@link #BEWONERS}): every character only exists for players whose own step is in its
 * range (ring-kern's Zicht), so before the council they are all over the house, after the bell they sit in the council
 * ring, and after the chapter only Guhrond is left. {@code Bezetting} repairs a copy that lost one.
 */
public final class Guhvendel {
    public static final String STRUCTUUR = "guhvendel";
    /** The NpcRollen plek of every character of this house, and the id of the narrator card. */
    public static final String PLEK = "guhvendel", KAART = "ring_h2";
    /** Template coordinates: the stone table in the middle of the council ring (the anchor of both cutscenes). */
    public static final BlockPos KRING = new BlockPos(41, 10, 27);
    /** Template coordinates: the council bell. */
    public static final BlockPos BEL = new BlockPos(36, 11, 24);
    /**
     * Template coordinates: the middle of the cirque (where you stand), how far its floor reaches, and how far above the
     * floor the template sets air (its dome: a copy on a high cave floor must stay under the bedrock roof).
     */
    public static final BlockPos MIDDEN = new BlockPos(30, 9, 35);
    public static final int KOM = 24, KOEPEL = 23;

    /** The steps of {@link RingH2Feature#LIJN}. */
    public static final int REIS = 0, WELKOM = 1, KENNIS = 2, RAADSBEL = 3, MELDEN = 4, VERTREK = 5, STAPPEN = 6;

    /** The six a player meets before the council (Guhdalf they know already). */
    public static final List<GuhNpcEntity.Kind> GEZELSCHAP = List.of(GuhNpcEntity.Kind.ARAGUH, GuhNpcEntity.Kind.LEGUHLAS, GuhNpcEntity.Kind.GIMGUH,
            GuhNpcEntity.Kind.BOROMIKA, GuhNpcEntity.Kind.MERRIE, GuhNpcEntity.Kind.PIPPGUH);

    /** A character of the template: the block it stands in, how far above its bottom, its yaw, the steps it exists for. */
    public record Bewoner(String id, GuhNpcEntity.Kind kind, int x, int y, int z, double dy, float yaw, int van, int tot) {
        public BlockPos blok() {
            return new BlockPos(x, y, z);
        }
    }

    public static final List<Bewoner> BEWONERS = List.of(
            new Bewoner("ringh2_guhrond_stoep", GuhNpcEntity.Kind.GUHROND, 24, 10, 25, 0.0, 270f, 0, 3),
            new Bewoner("ringh2_guhdalf_voor", GuhNpcEntity.Kind.GUHDALF, 34, 9, 30, 0.0, 135f, 0, 3),
            new Bewoner("ringh2_araguh_voor", GuhNpcEntity.Kind.ARAGUH, 34, 9, 55, 0.0, 0f, 0, 3),
            new Bewoner("ringh2_leguhlas_voor", GuhNpcEntity.Kind.LEGUHLAS, 18, 9, 44, 0.0, 270f, 0, 3),
            new Bewoner("ringh2_gimguh_voor", GuhNpcEntity.Kind.GIMGUH, 21, 9, 44, 0.0, 90f, 0, 3),
            new Bewoner("ringh2_boromika_voor", GuhNpcEntity.Kind.BOROMIKA, 15, 10, 30, 0.0, 0f, 0, 3),
            new Bewoner("ringh2_merrie_voor", GuhNpcEntity.Kind.MERRIE, 40, 9, 44, 0.0, 180f, 0, 3),
            new Bewoner("ringh2_pippguh_voor", GuhNpcEntity.Kind.PIPPGUH, 42, 9, 44, 0.0, 180f, 0, 3),
            new Bewoner("ringh2_guhrond_raad", GuhNpcEntity.Kind.GUHROND, 41, 10, 22, 0.5, 0f, 4, 5),
            new Bewoner("ringh2_guhdalf_raad", GuhNpcEntity.Kind.GUHDALF, 44, 10, 23, 0.5, 45f, 4, 5),
            new Bewoner("ringh2_araguh_raad", GuhNpcEntity.Kind.ARAGUH, 38, 10, 23, 0.5, 315f, 4, 5),
            new Bewoner("ringh2_leguhlas_raad", GuhNpcEntity.Kind.LEGUHLAS, 46, 10, 26, 0.5, 90f, 4, 5),
            new Bewoner("ringh2_gimguh_raad", GuhNpcEntity.Kind.GIMGUH, 46, 10, 28, 0.5, 90f, 4, 5),
            new Bewoner("ringh2_boromika_raad", GuhNpcEntity.Kind.BOROMIKA, 44, 10, 31, 0.5, 135f, 4, 5),
            new Bewoner("ringh2_merrie_raad", GuhNpcEntity.Kind.MERRIE, 49, 9, 31, 0.0, 60f, 4, 5),
            new Bewoner("ringh2_pippguh_raad", GuhNpcEntity.Kind.PIPPGUH, 50, 9, 29, 0.0, 80f, 4, 5),
            new Bewoner("ringh2_guhrond_thuis", GuhNpcEntity.Kind.GUHROND, 24, 10, 27, 0.0, 270f, 6, 99));

    /** A copy of Guhvendel in the world: the cutscene anchor, the bell, the middle of the cirque, how it is turned, its box. */
    public record Oord(BlockPos anker, BlockPos bel, BlockPos midden, Rotation draai, BoundingBox doos) {
        /** Inside the cirque (on its floor, not on or behind the rock around it)? */
        public boolean inKom(Vec3 plek) {
            double dx = plek.x - (midden.getX() + 0.5), dz = plek.z - (midden.getZ() + 0.5);
            return dx * dx + dz * dz <= KOM * KOM && plek.y >= doos.minY() && plek.y <= doos.maxY() + 1;
        }
    }

    private static final String ONTMOET = "ontmoet_", PROVIAND = "proviand";
    static final String SLUIPLES = "sluiples", GESNOEPT = "gesnoept";

    private Guhvendel() {
    }

    // =====================================================================================================================
    // where things are
    // =====================================================================================================================

    static Verhaallijn lijn() {
        return RingH2Feature.LIJN;
    }

    /** Does the story play in this level (the Barbecuether; tests: everywhere)? */
    static boolean inWereld(ServerLevel level) {
        return Ring.OVERAL || level.dimension() == BarbecuetherFeature.BARBECUETHER;
    }

    /** The copy of Guhvendel at this spot (a piece within 48 blocks), or null. */
    @Nullable
    public static Oord oord(ServerLevel level, BlockPos bij) {
        StructureStart start = Bezetting.start(level, STRUCTUUR, bij);
        if (start == null) {
            return null;
        }
        BlockPos anker = Kopieen.wereld(start, null, KRING), bel = Kopieen.wereld(start, null, BEL), midden = Kopieen.wereld(start, null, MIDDEN);
        if (anker == null || bel == null || midden == null) {
            return null;
        }
        // (the box of the piece itself: a start's own box is 12 blocks wider all round, for the terrain adaptation)
        BoundingBox doos = start.getPieces().isEmpty() ? start.getBoundingBox() : start.getPieces().get(0).getBoundingBox();
        return new Oord(anker, bel, midden, Kopieen.draai(start, null), doos);
    }

    /** Is this player inside the cirque of a copy? */
    public static boolean binnen(ServerPlayer p) {
        Oord o = oord(p.level(), p.blockPosition());
        return o != null && o.inKom(p.position());
    }

    // =====================================================================================================================
    // the cast in the template
    // =====================================================================================================================

    /** Registers every character with Bezetting: a copy that lost one gets it back (the template brings them the first time). */
    static void registreer() {
        for (Bewoner b : BEWONERS) {
            Bezetting.wezen(b.id(), STRUCTUUR, null, b.blok(), (level, plek, draai) -> maak(b, level, plek, draai));
        }
    }

    /** One character of the template, as the template has it (not added to the world). */
    @Nullable
    static GuhNpcEntity maak(Bewoner b, ServerLevel level, Vec3 plek, Rotation draai) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return null;
        }
        npc.setKind(b.kind());
        npc.roleData.putString(NpcRollen.PLEK, PLEK);
        npc.setYRot(b.yaw());
        float yaw = npc.rotate(draai);
        npc.snapTo(plek.x, plek.y + b.dy(), plek.z, yaw, 0f);
        npc.setYBodyRot(yaw);
        npc.setYHeadRot(yaw);
        npc.setInvulnerable(true);
        Zicht.alleenBij(npc, lijn().id(), b.van(), b.tot());
        return npc;
    }

    // =====================================================================================================================
    // meeting the fellowship
    // =====================================================================================================================

    public static boolean heeftOntmoet(ServerPlayer p, GuhNpcEntity.Kind kind) {
        return lijn().vlag(p, ONTMOET + kind.id());
    }

    /** How many of the six this player has met. */
    public static int ontmoet(ServerPlayer p) {
        int n = 0;
        for (GuhNpcEntity.Kind kind : GEZELSCHAP) {
            n += heeftOntmoet(p, kind) ? 1 : 0;
        }
        return n;
    }

    /** "Araguh, Gimguh en Pippguh": the ones this player still has to meet. */
    public static Component nogTeOntmoeten(ServerPlayer p) {
        List<GuhNpcEntity.Kind> rest = new ArrayList<>();
        for (GuhNpcEntity.Kind kind : GEZELSCHAP) {
            if (!heeftOntmoet(p, kind)) {
                rest.add(kind);
            }
        }
        MutableComponent uit = Component.empty();
        for (int i = 0; i < rest.size(); i++) {
            if (i > 0) {
                uit.append(i == rest.size() - 1 ? Component.translatable("quest.guhs.ringh2.en") : Component.literal(", "));
            }
            uit.append(Component.translatable("entity.guhs.guh_npc." + rest.get(i).id()).withStyle(ChatFormatting.YELLOW));
        }
        return uit;
    }

    /** This player met this character (true the first time). Everybody met and Guhrond spoken to: on to the bell. */
    static boolean zetOntmoet(ServerPlayer p, GuhNpcEntity.Kind kind) {
        if (!GEZELSCHAP.contains(kind) || heeftOntmoet(p, kind)) {
            return false;
        }
        lijn().vlag(p, ONTMOET + kind.id(), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh2.kennis.teller", ontmoet(p), GEZELSCHAP.size()).withStyle(ChatFormatting.GOLD));
        kennisKlaar(p);
        return true;
    }

    /** At the step "meet everybody" with everybody met: the next step (the bell). */
    static boolean kennisKlaar(ServerPlayer p) {
        if (ontmoet(p) >= GEZELSCHAP.size() && lijn().verder(p, KENNIS)) {
            GuhQuests.hint(p, "quest.guhs.ringh2.kennis.klaar");
            return true;
        }
        return false;
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** Once a second for a player (RingH2Feature's tick listener; the tests call it themselves). */
    static void seconde(ServerPlayer p) {
        Verhaallijn lijn = lijn();
        if (p.isSpectator() || !inWereld(p.level()) || !lijn.aanDeBeurt(p) || lijn.klaar(p)) {
            return;
        }
        int stap = lijn.stap(p);
        if (stap == REIS) {
            if (!Verteller.gezien(p, KAART)) {
                // the start of the chapter: the card with the map (false while the player watches something else: next second)
                Verteller.toon(p, KAART, speler -> lijn().begin(speler));
                return;
            }
            Oord o = oord(p.level(), p.blockPosition());
            if (o != null && o.inKom(p.position())) {
                aankomst(p);
            }
        } else if (stap == RAADSBEL) {
            // the bell beckons, only for the player whose turn it is
            Oord o = oord(p.level(), p.blockPosition());
            if (o != null && o.bel().closerToCenterThan(p.position(), 32)) {
                p.level().sendParticles(p, ParticleTypes.NOTE, false, false, o.bel().getX() + 0.5, o.bel().getY() + 1.2, o.bel().getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0.5);
            }
        }
    }

    /** The player walked into Guhvendel: step 0 is done. */
    static boolean aankomst(ServerPlayer p) {
        if (!Ring.aanZet(p, lijn(), REIS) || !lijn().verder(p, REIS)) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh2.aankomst").withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.8f);
        GuhEntity sam = Sam.van(p);
        if (sam != null) {
            GuhQuests.say(p, sam, "quest.guhs.ringh2.sam.aankomst");
        }
        return true;
    }

    /** A click on a bell: the council bell of a copy rings for the story. */
    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!p.level().getBlockState(pos).is(Blocks.BELL) || !inWereld(p.level())) {
            return;
        }
        Oord o = oord(p.level(), pos);
        if (o != null && o.bel().equals(pos)) {
            luid(p, o);
        }
    }

    /**
     * The council bell is rung. For the player whose step it is: the council (cutscene), then the next step; anybody else
     * hears what they still have to do, or just the bell. True when the council started.
     */
    static boolean luid(ServerPlayer p, Oord o) {
        Verhaallijn lijn = lijn();
        if (Ring.aanZet(p, lijn, RAADSBEL)) {
            if (!Cutscenes.speel(p, RingH2Scenes.RAAD, o.anker(), o.draai(), Guhvendel::naRaad)) {
                return false;   // (already watching something)
            }
            Sam.wacht(p, RingH2Scenes.RAAD.duur() + 60);
            return true;
        }
        if (!lijn.aanDeBeurt(p)) {
            return false;
        }
        int stap = lijn.stap(p);
        if (stap < RAADSBEL) {
            if (stap == KENNIS) {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringh2.bel.te_vroeg", nogTeOntmoeten(p)).withStyle(ChatFormatting.GRAY));
            } else {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringh2.bel.eerst_guhrond").withStyle(ChatFormatting.GRAY));
            }
        } else {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringh2.bel.galm").withStyle(ChatFormatting.GRAY));
        }
        return false;
    }

    /** The council is over (they are still arguing): somebody has to say something. */
    static void naRaad(ServerPlayer p) {
        if (lijn().verder(p, RAADSBEL)) {
            Ring.behaald(p, "ring_h2_raad");
            GuhQuests.hint(p, "quest.guhs.ringh2.raad.na");
        }
    }

    /** "Ik neem de ring wel mee!": the cutscene of the fellowship, then the provisions and the last step. */
    static boolean meldAan(ServerPlayer p, @Nullable Entity bij) {
        if (!Ring.aanZet(p, lijn(), MELDEN)) {
            return false;
        }
        Oord o = oord(p.level(), bij != null ? bij.blockPosition() : p.blockPosition());
        if (o == null) {
            naGenootschap(p);   // (a Guhrond that stands nowhere near a copy: no scene to play)
            return true;
        }
        if (!Cutscenes.speel(p, RingH2Scenes.GENOOTSCHAP, o.anker(), o.draai(), Guhvendel::naGenootschap)) {
            return false;
        }
        Sam.wacht(p, RingH2Scenes.GENOOTSCHAP.duur() + 60);
        return true;
    }

    /** The fellowship is formed: Guhrond's provisions (once per player), on to the farewell. */
    static void naGenootschap(ServerPlayer p) {
        if (!lijn().verder(p, MELDEN)) {
            return;
        }
        if (lijn().eenmalig(p, PROVIAND)) {
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 6));
            Minigames.give(p, new ItemStack(RingFeature.STOOFPOTJE.get(), 2));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh2.genootschap.na").withStyle(ChatFormatting.GOLD));
        GuhQuests.hint(p, "quest.guhs.ringh2.genootschap.hint");
    }

    /** "Op weg!": the chapter is done, the road to Knabbelmoria opens. */
    static boolean vertrek(ServerPlayer p, @Nullable Entity guhdalf) {
        if (!Ring.aanZet(p, lijn(), VERTREK) || !lijn().verder(p, VERTREK)) {
            return false;
        }
        Ring.behaald(p, "ring_h2_genootschap");
        if (guhdalf != null) {
            GuhQuests.say(p, guhdalf, "quest.guhs.ringh2.guhdalf.op_weg");
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.1f);
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh2.klaar").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** RingH2Feature's tick listener. */
    static void opTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % 20 == 7) {
            seconde(p);
        }
    }

    /** (dev / tests) forget what this player did in Guhvendel. */
    public static void wis(ServerPlayer p) {
        lijn().wis(p);
        Verteller.vergeet(p, KAART);
        Cutscenes.vergeet(p, RingH2Scenes.RAAD.id());
        Cutscenes.vergeet(p, RingH2Scenes.GENOOTSCHAP.id());
    }

    /** The scenes of this house, for the commands. */
    static List<Cutscene> scenes() {
        return List.of(RingH2Scenes.RAAD, RingH2Scenes.GENOOTSCHAP);
    }
}
