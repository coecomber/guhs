package nl.juiced.guhs.feature.bio.bouwwolk1;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The wolkenhoeder at his hut on the big island (the existing NPC kind WOLKENHOEDER with the place role
 * {@value #PLEK}; at the hemelkapelletje he keeps his old role). He teaches every player to make cloud blocks, in four
 * little lessons that go on by themselves as the player does them ({@link #geschoren}, {@link #gemaakt},
 * {@link #geplaatst}, {@link #gerust}; BouwWolk1Events listens), with a modest reward at the end, and gives each player
 * one wolkenschaapje to take home, on a lead. He also herds his fold ({@link Kudde}).
 * <p>
 * Per player (GuhQuests.saved): {@value #LES} = the step (0 not begun, 1 shear, 2 make a cloud block, 3 build a cloud
 * stair, 4 rest, 5 come for the reward, 6 done), {@value #SCHAAPJE} = got the schaapje, {@value #PRAATJE} = which
 * line of small talk comes next.
 */
public final class Hoeder implements NpcRole {
    public static final String PLEK = "wolkenhoeder_hut";
    public static final String LES = "guhs_bio_bouw_wolk1_les", SCHAAPJE = "guhs_bio_bouw_wolk1_schaapje", PRAATJE = "guhs_bio_bouw_wolk1_praatje";
    public static final int NIET = 0, KNIPPEN = 1, MAKEN = 2, BOUWEN = 3, RUSTEN = 4, BELONING = 5, KLAAR = 6;
    /** Answer ids. */
    public static final int JA = 1, LATER = 2, WIL_SCHAAPJE = 3, DAG = 4;
    /** The reward: this much wolkenpluis and one wolkenlamp (and shears at the start, for who has none). */
    public static final int PLUIS = 6;
    public static final int PRAATJES = 4;
    private static final String Q = "quest.guhs.wolkenhoeder_hut.", G = "gui.guhs.wolkenhoeder_hut.";

    public static int stap(ServerPlayer speler) {
        return GuhQuests.saved(speler).getIntOr(LES, NIET);
    }

    static void zetStap(ServerPlayer speler, int stap) {
        GuhQuests.saved(speler).putInt(LES, stap);
    }

    public static boolean heeftSchaapje(ServerPlayer speler) {
        return GuhQuests.saved(speler).getBooleanOr(SCHAAPJE, false);
    }

    public static Component naam() {
        return Component.translatable("entity.guhs.guh_npc.wolkenhoeder");
    }

    // --- what counts ------------------------------------------------------------------------------------------------

    /** A piece of cloud: wolkenblok in either colour, as block, slab or stairs (slice blokken-wolk; by id, never by class). */
    public static boolean isWolk(BlockState state) {
        return state.getBlock() != Blocks.AIR && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(Guhs.MODID)
                && BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("wolkenblok");
    }

    public static boolean isWolk(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return !stack.isEmpty() && id.getNamespace().equals(Guhs.MODID) && id.getPath().startsWith("wolkenblok");
    }

    /** A cloud bench or a cloud bed. */
    public static boolean isRustplek(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals(Guhs.MODID) && (id.getPath().equals("wolkenbank") || id.getPath().equals("wolkenbed"));
    }

    /** Is the cloud at {@code pos} part of a stair of three cloud pieces, each one step up and one step along from the last? */
    public static boolean trapje(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int plaats = 0; plaats < 3; plaats++) {             // pos is the lowest, the middle or the highest step
                boolean alle = true;
                for (int i = 0; i < 3 && alle; i++) {
                    alle = isWolk(level.getBlockState(pos.relative(d, i - plaats).above(i - plaats)));
                }
                if (alle) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int tel(ServerPlayer speler, java.util.function.Predicate<ItemStack> wat) {
        int n = 0;
        for (ItemStack s : speler.getInventory().getNonEquipmentItems()) {
            if (wat.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    // --- the lessons, as the player does them -------------------------------------------------------------------------

    /** The player sheared a wolkenschaapje. */
    public static void geschoren(ServerPlayer speler) {
        if (stap(speler) == KNIPPEN) {
            verder(speler, MAKEN, "stap2");
        }
    }

    /** The player made this at a crafting grid. */
    public static void gemaakt(ServerPlayer speler, ItemStack wat) {
        if (stap(speler) == MAKEN && isWolk(wat)) {
            verder(speler, BOUWEN, "stap3");
        }
    }

    /** The player placed a block here. */
    public static void geplaatst(ServerPlayer speler, BlockGetter level, BlockPos pos) {
        if (stap(speler) == BOUWEN && isWolk(level.getBlockState(pos)) && trapje(level, pos)) {
            verder(speler, RUSTEN, "stap4");
        }
    }

    /** The player sat down on a cloud bench or lay down on a cloud bed. */
    public static void gerust(ServerPlayer speler) {
        if (stap(speler) == RUSTEN) {
            verder(speler, BELONING, "terug");
        }
    }

    private static void verder(ServerPlayer speler, int naar, String tekst) {
        zetStap(speler, naar);
        speler.sendOverlayMessage(Component.translatable(G + "les", naar - 1).withStyle(ChatFormatting.LIGHT_PURPLE));
        Buiten.zeg(speler, naam(), Q + tekst);
        speler.level().playSound(null, speler.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    /** What the player carries may show a lesson is done already (pluis in the bag, a cloud block made elsewhere). */
    static void haalIn(ServerPlayer speler) {
        Item pluis = Bio.item("wolkenpluis", Items.AIR);
        if (stap(speler) == KNIPPEN && pluis != Items.AIR && tel(speler, s -> s.is(pluis)) > 0) {
            zetStap(speler, MAKEN);
        }
        if (stap(speler) == MAKEN && tel(speler, Hoeder::isWolk) > 0) {
            zetStap(speler, BOUWEN);
        }
    }

    // --- talking ------------------------------------------------------------------------------------------------------

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer speler) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.25f);
        Bewijs.geef(speler, Bewijs.HUT_GEVONDEN);
        int voor = stap(speler);
        haalIn(speler);
        int stap = stap(speler);
        switch (stap) {
            case NIET -> Praat.open(speler, npc, null, Q + "hallo", new Object[0], new Praat.Optie(JA, G + "optie.ja"), new Praat.Optie(LATER, G + "optie.later"));
            case KNIPPEN, MAKEN, BOUWEN, RUSTEN -> Praat.open(speler, npc, null, Q + "stap" + stap + (stap == voor ? ".nog" : ""), new Object[0]);
            case BELONING -> {
                beloon(speler);
                Praat.open(speler, npc, null, Q + "klaar", new Object[0], new Praat.Optie(WIL_SCHAAPJE, G + "optie.schaapje"), new Praat.Optie(DAG, G + "optie.dag"));
            }
            default -> {
                CompoundTag d = GuhQuests.saved(speler);
                int n = d.getIntOr(PRAATJE, 0);
                d.putInt(PRAATJE, n + 1);
                if (heeftSchaapje(speler)) {
                    Praat.open(speler, npc, null, Q + "praatje" + Math.floorMod(n, PRAATJES), new Object[0]);
                } else {
                    Praat.open(speler, npc, null, Q + "praatje" + Math.floorMod(n, PRAATJES), new Object[0], new Praat.Optie(WIL_SCHAAPJE, G + "optie.schaapje"),
                            new Praat.Optie(DAG, G + "optie.dag"));
                }
            }
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer speler, int optie) {
        switch (optie) {
            case JA -> {
                if (stap(speler) == NIET) {
                    zetStap(speler, KNIPPEN);
                    if (tel(speler, s -> s.is(Items.SHEARS)) == 0) {
                        GuhQuests.give(speler, Items.SHEARS);
                        Buiten.zeg(speler, naam(), Q + "schaar");
                    }
                    Praat.open(speler, npc, null, Q + "stap1", new Object[0]);
                }
            }
            case LATER -> Praat.open(speler, npc, null, Q + "later", new Object[0]);
            case WIL_SCHAAPJE -> Praat.open(speler, npc, null, Q + (geefSchaapje(npc, speler) ? "schaapje" : "schaapje_al"), new Object[0]);
            case DAG -> Praat.open(speler, npc, null, Q + "dag", new Object[0]);
            default -> {
            }
        }
    }

    /** The reward of the lesson, once: wolkenpluis and a wolkenlamp. */
    static void beloon(ServerPlayer speler) {
        if (stap(speler) != BELONING) {
            return;
        }
        zetStap(speler, KLAAR);
        geef(speler, new ItemStack(Bio.item("wolkenpluis", Items.WHITE_WOOL), PLUIS));
        geef(speler, new ItemStack(Bio.item("wolkenlamp", Items.LANTERN)));
        Bewijs.geef(speler, Bewijs.HUT_LES);
        speler.level().playSound(null, speler.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    private static void geef(ServerPlayer speler, ItemStack stack) {
        if (!speler.getInventory().add(stack)) {
            speler.drop(stack, false);
        }
    }

    /**
     * One wolkenschaapje to take home, once per player and only after the lesson: a new one (never one of the fold), on
     * a lead in the hand of the player. The lead makes it the player's for good (slice dieren: kept, saved, never tidied up).
     */
    public static boolean geefSchaapje(GuhNpcEntity npc, ServerPlayer speler) {
        if (heeftSchaapje(speler) || stap(speler) != KLAAR || !(npc.level() instanceof ServerLevel level)) {
            return false;
        }
        Entity e = Kudde.soort().map(t -> t.create(level, EntitySpawnReason.TRIGGERED)).orElse(null);
        if (!(e instanceof Mob schaapje)) {
            return false;
        }
        double x = (npc.getX() + speler.getX()) / 2, z = (npc.getZ() + speler.getZ()) / 2;
        schaapje.snapTo(x, Math.max(npc.getY(), speler.getY()) + 0.1, z, speler.getYRot() + 180, 0);
        schaapje.setPersistenceRequired();
        level.addFreshEntity(schaapje);
        schaapje.setLeashedTo(speler, true);
        level.sendParticles(ParticleTypes.HEART, x, schaapje.getY() + 1.2, z, 6, 0.4, 0.3, 0.4, 0.0);
        GuhQuests.saved(speler).putBoolean(SCHAAPJE, true);
        Bewijs.geef(speler, Bewijs.HUT_SCHAAPJE);
        return true;
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if ((npc.tickCount + npc.getId()) % Kudde.ELKE == 0 && npc.level() instanceof ServerLevel level) {
            Kudde.hoed(level, npc);
        }
    }
}
