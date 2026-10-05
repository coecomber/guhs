package nl.juiced.guhs.feature.fossielmijn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (fossiel-mijn): the Fossiel-opgraving. Questline {@link FossielmijnFeature#ARCHEOLOOG} (per player):
 * <ol start="0">
 *   <li>talk to the Archeoloog-guh: he gives the Guhkwastje;</li>
 *   <li>brush five bones out of the bottenzand: every spot of sand gives each player one bone ({@link #strijk});</li>
 *   <li>put the five bones on the stand ({@link #zetOpRek}): the skeleton grows, for this player only;</li>
 *   <li>tell him: the Tyrannoguhrus-beeldje, and the brush is yours.</li>
 * </ol>
 * Nothing changes in the world. What a player did lives in their saved guh data: the counters "gevonden" (0..5) and
 * "geplaatst" (a mask of bones) of the questline, the spots they brushed ({@link #GEKWAST}) and the day those count for
 * ({@link #DAG}). The client gets the mask and the spots ({@link FossielmijnPayloads.Stand}) and draws the sand and the
 * stand accordingly. From step 2 on every spot of sand has a small find for every player once a (game) day: modest, and
 * what makes the brush a reward worth keeping.
 */
public final class Opgraving {
    /** The spots of bottenzand this player brushed empty (block positions as longs). */
    public static final String GEKWAST = "guhs_fossielmijn_gekwast";
    /** The game day the daily finds of {@link #GEKWAST} are of. */
    public static final String DAG = "guhs_fossielmijn_dag";
    public static final String GEVONDEN = "gevonden", GEPLAATST = "geplaatst";
    public static final int AANTAL = 5, ALLES = (1 << AANTAL) - 1;
    /** Strokes of the brush on one spot before it gives something (a stroke every 10 ticks, like any brushing). */
    public static final int STREKEN = 4;
    /** No more spots than this are remembered per player (the oldest go first). */
    public static final int MAX_PLEKKEN = 64;
    public static final int TIPS = 4;

    private record Bezig(BlockPos pos, int streken) {
    }

    /** (not saved) which spot a player is brushing and how many strokes it had. */
    private static final Map<UUID, Bezig> BEZIG = new HashMap<>();

    private Opgraving() {
    }

    // --- the per-player state -----------------------------------------------------------------------------------------------

    /** The bone ({@link FossielmijnFeature#BOTTEN} index) that is found as number n (0..4): the tail first, the skull last. */
    public static int volgorde(int n) {
        return AANTAL - 1 - n;
    }

    public static int gevonden(ServerPlayer p) {
        return FossielmijnFeature.ARCHEOLOOG.teller(p, GEVONDEN);
    }

    /** The mask of the bones this player put on the stand (bit i = {@link FossielmijnFeature#BOTTEN} i). */
    public static int geplaatst(ServerPlayer p) {
        return FossielmijnFeature.ARCHEOLOOG.teller(p, GEPLAATST) & ALLES;
    }

    public static long[] gekwast(ServerPlayer p) {
        return GuhQuests.saved(p).getLongArray(GEKWAST).orElse(new long[0]);
    }

    public static boolean isGekwast(ServerPlayer p, BlockPos pos) {
        long l = pos.asLong();
        for (long g : gekwast(p)) {
            if (g == l) {
                return true;
            }
        }
        return false;
    }

    private static void onthoud(ServerPlayer p, BlockPos pos) {
        long[] oud = gekwast(p);
        int start = Math.max(0, oud.length + 1 - MAX_PLEKKEN);
        long[] nieuw = new long[oud.length - start + 1];
        System.arraycopy(oud, start, nieuw, 0, oud.length - start);
        nieuw[nieuw.length - 1] = pos.asLong();
        GuhQuests.saved(p).putLongArray(GEKWAST, nieuw);
    }

    /** The game day (the Barbecuether's own clock stands still: the server's clock counts). */
    public static long dag(ServerPlayer p) {
        return p.level().getServer().overworld().getGameTime() / 24000L;
    }

    /** A new day: the daily finds are back (only once the quest's own bones are all found). True when something changed. */
    static boolean nieuweDag(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        long dag = dag(p);
        if (FossielmijnFeature.ARCHEOLOOG.stap(p) < 2 || saved.getLongOr(DAG, dag) == dag) {
            return false;
        }
        saved.putLong(DAG, dag);
        saved.putLongArray(GEKWAST, new long[0]);
        return true;
    }

    /** What the client draws: the mask of the stand and the brushed spots. Sent at login and whenever it changes. */
    public static void sync(ServerPlayer p) {
        nieuweDag(p);
        CompoundTag data = new CompoundTag();
        data.putInt("Rek", geplaatst(p));
        data.putLongArray("Gekwast", gekwast(p));
        FossielmijnPayloads.send(p, new FossielmijnPayloads.Stand(data));
    }

    private static void meld(ServerPlayer p, String key, ChatFormatting kleur, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
    }

    // --- brushing -----------------------------------------------------------------------------------------------------------

    /**
     * Right click on bottenzand with a brush: brushing starts here, on both sides (the dig is a protected building, where
     * items are not used on blocks; this is the one thing a brush may do there, and it changes nothing).
     */
    public static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof BrushItem && event.getLevel().getBlockState(event.getPos()).is(FossielmijnFeature.BOTTENZAND.get())) {
            event.getEntity().startUsingItem(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /** Every tick a player brushes: a stroke lands every 10 ticks (the brush's own rhythm) on the sand they look at. */
    public static void opKwast(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getItem().getItem() instanceof BrushItem)) {
            return;
        }
        int verstreken = event.getItem().getUseDuration(p) - event.getDuration() + 1;
        if (verstreken % 10 != 5) {
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(p, EntitySelector.CAN_BE_PICKED, p.blockInteractionRange());
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && p.level().getBlockState(block.getBlockPos()).is(FossielmijnFeature.BOTTENZAND.get())) {
            strijk(p, block.getBlockPos());
        } else {
            BEZIG.remove(p.getUUID());
        }
    }

    /** One stroke of this player's brush on this spot of bottenzand. True when the stroke was the one that found something. */
    public static boolean strijk(ServerPlayer p, BlockPos pos) {
        Bezig b = BEZIG.get(p.getUUID());
        int streken = b != null && b.pos.equals(pos) ? b.streken + 1 : 1;
        if (streken < STREKEN) {
            BEZIG.put(p.getUUID(), new Bezig(pos.immutable(), streken));
            return false;
        }
        BEZIG.remove(p.getUUID());
        return vind(p, pos);
    }

    /** The sand is brushed clean: what does this player find here? True when they got something. */
    public static boolean vind(ServerPlayer p, BlockPos pos) {
        var lijn = FossielmijnFeature.ARCHEOLOOG;
        int stap = lijn.stap(p);
        if (stap == 0) {
            meld(p, "gui.guhs.fossielmijn.eerst_praten", ChatFormatting.LIGHT_PURPLE);
            return false;
        }
        boolean nieuw = nieuweDag(p);
        if (isGekwast(p, pos)) {
            meld(p, stap == 1 ? "gui.guhs.fossielmijn.gekwast" : "gui.guhs.fossielmijn.dag_leeg", ChatFormatting.LIGHT_PURPLE);
            if (nieuw) {
                sync(p);
            }
            return false;
        }
        onthoud(p, pos);
        ServerLevel level = p.level();
        BlockState zand = level.getBlockState(pos);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, zand), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 14, 0.25, 0.05, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 1f, 1f);
        int gevonden = gevonden(p);
        if (stap == 1 && gevonden < AANTAL) {
            ItemStack bot = new ItemStack(FossielmijnFeature.BOTTEN.get(volgorde(gevonden)).get());
            Component naam = bot.getHoverName();
            Minigames.give(p, bot);
            lijn.teller(p, GEVONDEN, gevonden + 1);
            if (gevonden + 1 >= AANTAL) {
                // (the marks of the quest's own spots stay for today; from tomorrow on the sand has its daily finds)
                GuhQuests.saved(p).putLong(DAG, dag(p));
                lijn.verder(p, 1);
                meld(p, "gui.guhs.fossielmijn.alle_botten", ChatFormatting.GOLD);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            } else {
                meld(p, "gui.guhs.fossielmijn.vond", ChatFormatting.YELLOW, naam, gevonden + 1);
            }
        } else {
            ItemStack vondst = dagvondst(p);
            Component naam = vondst.getHoverName();
            Minigames.give(p, vondst);
            GuhAdvancements.grant(p, "fossiel_mijn_dagvondst");
            meld(p, "gui.guhs.fossielmijn.dagvondst", ChatFormatting.YELLOW, naam);
        }
        sync(p);
        return true;
    }

    /** A daily find: modest (a few knabbels, a bone, a charred bone, now and then a zoutkristal or a nugget). */
    static ItemStack dagvondst(ServerPlayer p) {
        var random = p.getRandom();
        int worp = random.nextInt(100);
        if (worp < 40) {
            return new ItemStack(ModItems.KAAS_KNABBELS.get(), 2 + random.nextInt(3));
        } else if (worp < 65) {
            return new ItemStack(Items.BONE, 1 + random.nextInt(2));
        } else if (worp < 80) {
            return new ItemStack(BarbecuetherFeature.VERKOOLD_GUHBOT.get());
        } else if (worp < 92) {
            return new ItemStack(FossielmijnFeature.ZOUTKRISTAL.get());
        }
        return new ItemStack(Items.GOLD_NUGGET, 1 + random.nextInt(3));
    }

    // --- the stand ----------------------------------------------------------------------------------------------------------

    /** A click on the stand: a bone this player carries and has not put on it yet goes on (the one in the hand first). */
    public static void zetOpRek(ServerPlayer p, BlockPos pos) {
        var lijn = FossielmijnFeature.ARCHEOLOOG;
        int stap = lijn.stap(p);
        int mask = geplaatst(p);
        if (mask == ALLES) {
            meld(p, lijn.klaar(p) ? "gui.guhs.fossielmijn.rek.klaar" : "gui.guhs.fossielmijn.rek.af", ChatFormatting.GOLD);
            return;
        }
        if (stap == 0) {
            meld(p, "gui.guhs.fossielmijn.rek.leeg", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        int welk = -1;
        Item inHand = p.getMainHandItem().getItem();
        for (int i = 0; i < AANTAL; i++) {
            Item bot = FossielmijnFeature.BOTTEN.get(i).get();
            if ((mask & (1 << i)) == 0 && GuhQuests.count(p, bot) > 0 && (welk < 0 || bot == inHand)) {
                welk = i;
            }
        }
        if (welk < 0) {
            meld(p, "gui.guhs.fossielmijn.rek.geen", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        Item bot = FossielmijnFeature.BOTTEN.get(welk).get();
        Component naam = new ItemStack(bot).getHoverName();
        GuhQuests.take(p, bot, 1);
        mask |= 1 << welk;
        lijn.teller(p, GEPLAATST, mask);
        ServerLevel level = p.level();
        level.playSound(null, pos, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.9f + Integer.bitCount(mask) * 0.08f);
        if (mask == ALLES) {
            lijn.zet(p, 3);
            meld(p, "gui.guhs.fossielmijn.rek.af", ChatFormatting.GOLD);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.8, 0.6, 0.8, 0.02);
        } else {
            meld(p, "gui.guhs.fossielmijn.rek.gezet", ChatFormatting.YELLOW, naam, Integer.bitCount(mask));
        }
        sync(p);
    }

    // --- the Archeoloog-guh ---------------------------------------------------------------------------------------------------

    /** The role of the Archeoloog-guh: a different talk for every player (their own step of the questline). */
    public static final class Rol extends QuestRol {
        public Rol() {
            super(FossielmijnFeature.ARCHEOLOOG);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.9f);
            Item kwastje = FossielmijnFeature.KWASTJE.get();
            switch (stap) {
                case 0 -> {
                    zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.hallo1");
                    zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.hallo2");
                    geefAlsKwijt(p, kwastje);
                    verder(p, 0);
                }
                case 1 -> {
                    if (geefAlsKwijt(p, kwastje)) {
                        zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.kwast_weer");
                    }
                    botWeer(npc, p);
                    zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.zoek", gevonden(p));
                }
                case 2 -> {
                    botWeer(npc, p);
                    zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.bouw", Integer.bitCount(geplaatst(p)));
                }
                case 3 -> {
                    if (verder(p, 3)) {
                        zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.klaar1");
                        zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.klaar2");
                        geefEenmalig(p, "beeldje", new ItemStack(FossielmijnFeature.FOSSIELBEELDJE.get()));
                        geefAlsKwijt(p, kwastje);
                        zichtbaar(p, "barbecuether/fossiel_mijn_archeoloog");
                        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
                        if (npc.level() instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1.2, npc.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
                        }
                    }
                }
                default -> {
                    if (geefAlsKwijt(p, kwastje)) {
                        zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.kwast_weer");
                    } else {
                        zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.tip" + npc.getRandom().nextInt(TIPS));
                    }
                }
            }
        }

        /** A bone that was found but is neither on the stand nor in the player's pockets: he has a plaster cast of it. */
        private void botWeer(GuhNpcEntity npc, ServerPlayer p) {
            int gevonden = gevonden(p), mask = geplaatst(p);
            boolean gegeven = false;
            for (int n = 0; n < Math.min(gevonden, AANTAL); n++) {
                int i = volgorde(n);
                Item bot = FossielmijnFeature.BOTTEN.get(i).get();
                if ((mask & (1 << i)) == 0 && GuhQuests.count(p, bot) == 0) {
                    geef(p, new ItemStack(bot));
                    gegeven = true;
                }
            }
            if (gegeven) {
                zeg(p, npc, "quest.guhs.fossielmijn.archeoloog.bot_weer");
            }
        }
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<nl.juiced.guhs.feature.gids.VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        return switch (stap) {
            case 1 -> List.of(nl.juiced.guhs.feature.verhaal.Verhaallijn.nodig("guhs:fossielmijn_bottenzand", gevonden(p), AANTAL));
            case 2 -> List.of(nl.juiced.guhs.feature.verhaal.Verhaallijn.nodig("guhs:fossielmijn_skeletrek", Integer.bitCount(geplaatst(p)), AANTAL));
            default -> List.of();
        };
    }

    /** (tests, the op command) forget what this player brushed and is brushing. */
    public static void wis(Player p) {
        BEZIG.remove(p.getUUID());
        CompoundTag saved = GuhQuests.saved(p);
        saved.remove(GEKWAST);
        saved.remove(DAG);
    }
}
