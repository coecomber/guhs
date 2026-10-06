package nl.juiced.guhs.feature.paleizen;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (paleizen): "De tolbrug van het Mika-brugpaleis" (Verhaallijn {@code tolwachter}, per player):
 * <ol start="0">
 *   <li>talk to the Tolwachter-Mika in the mouth of the tolhuis;</li>
 *   <li>pay {@link #TOL} kaasknabbels, or guess {@link #RAADSELS_NODIG} of his riddles (a wrong answer costs nothing: another
 *       riddle). Until then the gate shoves you back out ({@link #poort}: a Duwtje, per player; the passage itself is open);</li>
 *   <li>lay the {@link #RIJEN} missing rows of planks in the bridge ({@link #legPlank}: one plank of his lays a row). Every
 *       row falls out again {@link #HERSTEL_TICKS} ticks after it was laid, so the next player finds the gap again; under the
 *       gap hangs a scaffold, nobody falls far;</li>
 *   <li>ring the tolbel in the bell tower on the far side ({@link #bel});</li>
 *   <li>back to the Tolwachter: free passage for ever, and the bridge building set.</li>
 * </ol>
 */
public final class TolQuest {
    static final String Q = "quest.guhs.paleizen.tol.", GUI = "gui.guhs.paleizen.";
    public static final int TOL = 8, RAADSELS_NODIG = 3, RIJEN = 5;
    /** A row of planks stays this long (a minute), then the gap is back for the next player. */
    public static final int HERSTEL_TICKS = 1200;
    /** The right answer (0, 1, 2 = a, b, c) of every riddle: tools/features/paleizen_tekst.py RAADSELS (the self-check compares). */
    static final int[] GOED = {0, 1, 2, 0, 1, 2};
    private static final int OPTIE_TOL = 1, OPTIE_RAADSELS = 2, OPTIE_LATER = 3, OPTIE_ANTWOORD = 10;

    public static final Verhaallijn LIJN = Verhaallijn.maak("tolwachter", "barbecue").stappen(5).icoon("minecraft:bell")
            .nodig((p, stap) -> stap == 1 ? List.of(Verhaallijn.nodig("guhs:kaas_knabbels", GUI + "nodig.tol", GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()), TOL))
                    : stap == 2 ? List.of(Verhaallijn.nodig("guhs:paleizen_losse_plank", GuhQuests.count(p, PaleizenFeature.LOSSE_PLANK.get()), 1)) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("minecraft:bell", GUI + "beloning.doorgang", magDoor(p)),
                    Verhaallijn.beloning("guhs:paleizen_recept_brug", klaar(p)), Verhaallijn.beloning("guhs:paleizen_brugplank", klaar(p)),
                    Verhaallijn.beloning("guhs:paleizen_brugleuning", klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, PaleisPlekken.BRUGPALEIS,
                    Component.translatable("structure.guhs." + PaleisPlekken.BRUGPALEIS)))
            .registreer();

    static final Rol ROL = new Rol();

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.TOLWACHTER_MIKA, ROL);
        Bezetting.npc("paleizen_tolwachter", PaleisPlekken.BRUGPALEIS, null, PaleisPlekken.Brug.TOLWACHTER_MIKA, GuhNpcEntity.Kind.TOLWACHTER_MIKA, null, 90f);
    }

    static boolean klaar(ServerPlayer p) {
        return LIJN.klaar(p);
    }

    /** May this player walk through the gate (toll paid or riddles guessed, in their own questline)? */
    public static boolean magDoor(ServerPlayer p) {
        return LIJN.stap(p) >= 2;
    }

    // --- the Tolwachter-Mika ---------------------------------------------------------------------------------------------

    static final class Rol extends QuestRol {
        Rol() {
            super(LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            switch (stap) {
                case 0, 1 -> {
                    LIJN.begin(p);
                    verder(p, 0);
                    scherm(p, npc, Q + "hallo", new Praat.Optie(OPTIE_TOL, Q + "optie.tol"), new Praat.Optie(OPTIE_RAADSELS, Q + "optie.raadsels"),
                            new Praat.Optie(OPTIE_LATER, Q + "optie.later"));
                }
                case 2 -> {
                    int nog = openRijen(p);
                    if (nog == 0 && LIJN.teller(p, "planken") == 0) {
                        zeg(p, npc, Q + "brug_wacht");   // (somebody else just mended it: it falls apart again in a minute)
                    } else if (GuhQuests.count(p, PaleizenFeature.LOSSE_PLANK.get()) < Math.max(1, nog)) {
                        geefPlanken(p, Math.max(1, nog));
                        zeg(p, npc, Q + "brug_kwijt");
                    } else {
                        zeg(p, npc, Q + "brug_nog", nog);
                    }
                    hint(p, Q + "hint.brug");
                }
                case 3 -> {
                    zeg(p, npc, Q + "bel_nog");
                    hint(p, Q + "hint.bel");
                }
                case 4 -> {
                    if (verder(p, 4)) {
                        scherm(p, npc, Q + "klaar", new Praat.Optie(4, Q + "optie.dank"));
                        geefEenmalig(p, "bouwset", new ItemStack(PaleizenFeature.RECEPT_BRUG.get()), new ItemStack(PaleizenFeature.BRUGPLANK.get(), 16),
                                new ItemStack(PaleizenFeature.BRUGLEUNING.get(), 8));
                        zichtbaar(p, "barbecuether/paleizen_tolwachter");
                        p.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.3f);
                    }
                }
                default -> {
                    if (geefAlsKwijt(p, PaleizenFeature.RECEPT_BRUG.get())) {
                        zeg(p, npc, Q + "tekening_kwijt");   // (the recipe card: again when it was lost)
                    } else {
                        zeg(p, npc, Q + "dank" + p.getRandom().nextInt(3));
                    }
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 1) {
                return;
            }
            if (optie == OPTIE_TOL) {
                int heb = GuhQuests.count(p, ModItems.KAAS_KNABBELS.get());
                if (heb < TOL) {
                    zeg(p, npc, Q + "tol_tekort", heb);
                    return;
                }
                GuhQuests.take(p, ModItems.KAAS_KNABBELS.get(), TOL);
                p.level().playSound(null, npc, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1f, 1.4f);
                zeg(p, npc, Q + "tol_betaald");
                door(npc, p);
            } else if (optie == OPTIE_RAADSELS) {
                vraag(npc, p);
            } else if (optie >= OPTIE_ANTWOORD && optie < OPTIE_ANTWOORD + 3) {
                int raadsel = Math.floorMod(LIJN.teller(p, "raadsel"), GOED.length);
                LIJN.teller(p, "raadsel", raadsel + 1);                    // (right or wrong: the next one is another riddle)
                if (optie - OPTIE_ANTWOORD != GOED[raadsel]) {
                    zeg(p, npc, Q + "raadsel_fout");
                    vraag(npc, p);
                    return;
                }
                int goed = LIJN.teller(p, "goed") + 1;
                LIJN.teller(p, "goed", goed);
                if (goed < RAADSELS_NODIG) {
                    zeg(p, npc, Q + "raadsel_goed", goed);
                    vraag(npc, p);
                    return;
                }
                zeg(p, npc, Q + "raadsels_klaar");
                verborgen(p, "paleizen_raadsels");
                door(npc, p);
            }
        }

        /** The riddle this player is at, with its three answers. */
        private void vraag(GuhNpcEntity npc, ServerPlayer p) {
            int raadsel = Math.floorMod(LIJN.teller(p, "raadsel"), GOED.length);
            String key = Q + "raadsel." + raadsel;
            scherm(p, npc, key, new Praat.Optie(OPTIE_ANTWOORD, key + ".a"), new Praat.Optie(OPTIE_ANTWOORD + 1, key + ".b"),
                    new Praat.Optie(OPTIE_ANTWOORD + 2, key + ".c"));
        }

        /** Toll paid or riddles guessed: the gate is open for this player, and here are the planks for the gap. */
        private void door(GuhNpcEntity npc, ServerPlayer p) {
            if (verder(p, 1)) {
                geefPlanken(p, RIJEN);
                zeg(p, npc, Q + "brug");
                hint(p, Q + "hint.brug");
            }
        }
    }

    /** Tops the player's planks up to {@code n}. */
    private static void geefPlanken(ServerPlayer p, int n) {
        int heb = GuhQuests.count(p, PaleizenFeature.LOSSE_PLANK.get());
        if (heb < n) {
            nl.juiced.guhs.feature.Minigames.give(p, new ItemStack(PaleizenFeature.LOSSE_PLANK.get(), n - heb));
        }
    }

    // --- the gate --------------------------------------------------------------------------------------------------------

    /** (not saved) when a player last got the "first pay" message, and for how many ticks in a row they stood in the gate. */
    private static final Map<UUID, long[]> POORT = new ConcurrentHashMap<>();

    /**
     * Every tick for a player near a Mika-brugpaleis: whoever stands in the passage of the tolhuis without having paid is
     * shoved back out towards the mouth (and put back in front of it when they keep pushing). True when it shoved.
     */
    public static boolean poort(ServerPlayer p, @Nullable StructureStart brug) {
        if (brug == null || magDoor(p) || p.isSpectator() || p.getAbilities().instabuild) {
            POORT.remove(p.getUUID());
            return false;
        }
        BoundingBox poort = PaleisPlekken.wereld(brug, PaleisPlekken.Brug.TOLPOORT);
        Vec3 buiten = PaleisPlekken.voet(brug, PaleisPlekken.Brug.TOLPOORT_BUITEN);
        if (poort == null || buiten == null || !poort.isInside(p.blockPosition())) {
            long[] s = POORT.get(p.getUUID());
            if (s != null) {
                s[1] = 0;
            }
            return false;
        }
        if (!Duwtje.mag(p)) {
            return false;
        }
        long nu = p.level().getGameTime();
        long[] s = POORT.computeIfAbsent(p.getUUID(), u -> new long[]{Long.MIN_VALUE / 2, 0});
        if (nu - s[0] >= 60) {
            s[0] = nu;
            p.sendOverlayMessage(Component.translatable(GUI + "tol.halt").withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), PaleizenFeature.MOPPER.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        }
        if (++s[1] > 30) {
            // (kept walking into it, came in from behind or was pushed in: put back in front of the mouth)
            s[1] = 0;
            Duwtje.terug(p, p.level().dimension(), buiten, p.getYRot());
        } else {
            Duwtje.duw(p, buiten.subtract(p.position()), 0.7);
        }
        return true;
    }

    // --- the planks ------------------------------------------------------------------------------------------------------

    /** How many of the five rows are missing at the bridge this player is at (0: whole, or no bridge near). */
    public static int openRijen(ServerPlayer p) {
        StructureStart brug = PaleisPlekken.kopie(p.level(), PaleisPlekken.BRUGPALEIS, p.blockPosition());
        return brug == null ? 0 : openRijen(p.level(), brug);
    }

    static int openRijen(ServerLevel level, StructureStart brug) {
        int open = 0;
        BoundingBox gat = PaleisPlekken.Brug.GAT;
        for (int x = gat.minX(); x <= gat.maxX(); x++) {
            open += rijOpen(level, brug, x) ? 1 : 0;
        }
        return open;
    }

    private static boolean rijOpen(ServerLevel level, StructureStart brug, int x) {
        BoundingBox gat = PaleisPlekken.Brug.GAT;
        for (int z = gat.minZ(); z <= gat.maxZ(); z++) {
            BlockPos pos = PaleisPlekken.wereld(brug, new BlockPos(x, gat.minY(), z));
            if (pos != null && !level.getBlockState(pos).is(PaleizenFeature.BRUGPLANK.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A player right-clicks with a plank of the Tolwachter: near the gap of a Mika-brugpaleis, on the mending step, the
     * nearest missing row (from the tolhuis side on) is laid. Returns true when the click was about the bridge at all (so the
     * caller swallows it); the row falls out again after {@link #HERSTEL_TICKS}.
     */
    public static boolean legPlank(ServerPlayer p, ItemStack plank) {
        if (!plank.is(PaleizenFeature.LOSSE_PLANK.get())) {
            return false;
        }
        ServerLevel level = p.level();
        StructureStart brug = PaleisPlekken.kopie(level, PaleisPlekken.BRUGPALEIS, p.blockPosition());
        BoundingBox gat = brug == null ? null : PaleisPlekken.wereld(brug, PaleisPlekken.Brug.GAT);
        if (gat == null || !gat.inflatedBy(8).isInside(p.blockPosition())) {
            bericht(p, Component.translatable(GUI + "tol.plank.ver"));
            return true;
        }
        if (LIJN.stap(p) != 2) {
            bericht(p, Component.translatable(GUI + (LIJN.stap(p) < 2 ? "tol.plank.niet_nu" : "tol.plank.heel")));
            return true;
        }
        BoundingBox lokaal = PaleisPlekken.Brug.GAT;
        for (int x = lokaal.minX(); x <= lokaal.maxX(); x++) {
            if (!rijOpen(level, brug, x)) {
                continue;
            }
            BlockState planken = PaleizenFeature.BRUGPLANK.get().defaultBlockState();
            for (int z = lokaal.minZ(); z <= lokaal.maxZ(); z++) {
                BlockPos pos = PaleisPlekken.wereld(brug, new BlockPos(x, lokaal.minY(), z));
                if (pos == null || level.getBlockState(pos).is(PaleizenFeature.BRUGPLANK.get())) {
                    continue;
                }
                BlockState was = level.getBlockState(pos);
                level.setBlock(pos, planken, Block.UPDATE_ALL);
                Herstel.na(level, pos, was.isAir() ? Blocks.AIR.defaultBlockState() : was, HERSTEL_TICKS);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, planken), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.05);
            }
            if (!p.getAbilities().instabuild) {
                plank.shrink(1);
            }
            LIJN.teller(p, "planken", LIJN.teller(p, "planken") + 1);
            level.playSound(null, p.blockPosition(), PaleizenFeature.PLANK.get(), SoundSource.BLOCKS, 1f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            int nog = openRijen(level, brug);
            if (nog == 0) {
                brugHeel(level, gat);
            } else if (plank.isEmpty() && GuhQuests.count(p, PaleizenFeature.LOSSE_PLANK.get()) == 0) {
                bericht(p, Component.translatable(GUI + "tol.plank.op", nog));
            } else {
                bericht(p, Component.translatable(GUI + "tol.plank", nog));
            }
            return true;
        }
        bericht(p, Component.translatable(GUI + "tol.plank.heel"));
        return true;
    }

    /** The last row is in: everyone at the gap who laid a row of it has mended the bridge (their own step goes on). */
    private static void brugHeel(ServerLevel level, BoundingBox gat) {
        Vec3 midden = Vec3.atCenterOf(gat.getCenter());
        level.playSound(null, gat.getCenter(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(midden) < 24 * 24 && LIJN.stap(p) == 2 && LIJN.teller(p, "planken") > 0 && LIJN.verder(p, 2)) {
                GuhQuests.take(p, PaleizenFeature.LOSSE_PLANK.get(), GuhQuests.count(p, PaleizenFeature.LOSSE_PLANK.get()));
                bericht(p, Component.translatable(GUI + "tol.brug_heel"));
                GuhQuests.hint(p, Q + "hint.bel");
            }
        }
    }

    // --- the bell --------------------------------------------------------------------------------------------------------

    /** A player rang a bell: the tolbel of a Mika-brugpaleis, on the bell step, takes the questline on. True when it did. */
    public static boolean bel(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        if (!level.getBlockState(pos).is(Blocks.BELL) || !PaleisPlekken.is(level, PaleisPlekken.BRUGPALEIS, pos, PaleisPlekken.Brug.BEL)) {
            return false;
        }
        int stap = LIJN.stap(p);
        if (stap == 3 && LIJN.verder(p, 3)) {
            bericht(p, Component.translatable(GUI + "tol.bel"));
            GuhQuests.hint(p, Q + "hint.terug");
            level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.6, 0.4, 0.6, 0.0);
            return true;
        }
        if (stap < 2) {
            bericht(p, Component.translatable(GUI + "tol.bel.niet_nu"));
        }
        return false;
    }

    private static void bericht(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static void vergeet(UUID speler) {
        POORT.remove(speler);
    }

    private TolQuest() {
    }
}
