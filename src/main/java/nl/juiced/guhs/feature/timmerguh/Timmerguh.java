package nl.juiced.guhs.feature.timmerguh;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.klusjes.KlusjesFeature;
import nl.juiced.guhs.feature.speelgoed.KnabbelbalEntity;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * De Timmerguh (NPC {@code TIMMERGUH}) on the bouwplaats of every Knuffeldal town: "Samen een huisje bouwen" (DESIGN_30 par. 1).
 * <ol>
 *   <li>He tells you about his half-built guhhuisje (the talking screen, {@link Praat}); you say you'll help: bring
 *   {@link #PLANKEN} planks (any wood) and {@link #WOL} pink wool ({@link TimmerguhVoortgang#MATERIAAL}).</li>
 *   <li>Hand them in: he loans you the dakpluisjes, one for every see-through ghost tile of the oortjesdak
 *   ({@link TimmerguhVoortgang#DAK}). Climb the steigertje and lay them ({@link DakpluisjeItem}); the last one: the flag goes up
 *   on the top (de vlag in top!), and you get a small guhhuisje, "our first huisje" ({@link TimmerguhVoortgang#BEWONER}).</li>
 *   <li>Place it (or any huisje of yours) and let one of your guhs live in it (Band moment HUISJE_IN, or already living there
 *   when you come back); tell the Timmerguh: the bouwboekje (the recipes of all three sizes), a small huisje as a present and
 *   the timmermanshelmpje ({@link TimmerguhVoortgang#KLAAR}).</li>
 *   <li>Optional: put a toy (glijbaantje, wip, schommel, tunnel or a knabbelbal) and a guhlampje in the home area of one of
 *   your huisjes: the gereedschapsriem with hamertje and duimstok ({@link TimmerguhVoortgang#KNUS}).</li>
 * </ol>
 * The roof belongs to the NPC (every town has its own); when someone starts while it is finished, the Timmerguh starts a
 * new one ("the last one already has a guh family living in it!"): the tiles are ghosts again. Nobody ever blocks you.
 * roleData: {@code Plekken} (the roof spots, found once), {@code Delen} (what goes on each), {@code Vlag} (the flag's spot).
 */
public final class Timmerguh implements NpcRole {
    public static final Timmerguh ROLE = new Timmerguh();
    public static final int PLANKEN = 16, WOL = 8, BEREIK = 16;
    /** Answer ids of the talking screen. */
    public static final int HELP = 1, UITLEG = 2, INLEVEREN = 3, OKE = 4;

    private static final String T = "quest.guhs.timmerguh.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.9f);
        GuhAdvancements.grant(player, "timmerguh_gesproken");
        GidsFeature.grant(player, "verhalen/timmerguh_bouwplaats");
        int stap = TimmerguhVoortgang.stap(player);
        switch (stap) {
            case TimmerguhVoortgang.NIEUW -> {
                if (plekken(npc).isEmpty()) {
                    praat(player, npc, "niks");
                } else {
                    praat(player, npc, "hallo", new Object[] {}, new Praat.Optie(HELP, T + "optie.help"), new Praat.Optie(UITLEG, T + "optie.wat"));
                }
            }
            case TimmerguhVoortgang.MATERIAAL -> materiaal(npc, player);
            case TimmerguhVoortgang.DAK -> {
                int open = open(npc);
                if (plekken(npc).isEmpty()) {
                    praat(player, npc, "niks");
                } else if (open == 0) {
                    dakAf(npc, player);
                } else {
                    int heb = GuhQuests.count(player, TimmerguhFeature.DAKPLUISJE.get());
                    if (heb < open) {
                        geefPluisjes(player, open - heb);
                    }
                    praat(player, npc, "dak_nog", new Object[] {open});
                }
            }
            case TimmerguhVoortgang.BEWONER -> {
                if (heeftBewoner(player)) {
                    klaar(npc, player);
                } else {
                    if (Huisjes.vanEigenaar(player.server, player.getUUID()).isEmpty()
                            && GuhQuests.count(player, HuisjeFeature.KLEIN.get().asItem()) == 0) {
                        Minigames.give(player, new ItemStack(HuisjeFeature.KLEIN.get()));      // (lost it? here's another one)
                    }
                    praat(player, npc, "bewoner_nog");
                }
            }
            default -> {
                if (GuhQuests.count(player, TimmerguhFeature.BOUWBOEKJE.get()) == 0 && !heeftBoekjeErgens(player)) {
                    Minigames.give(player, new ItemStack(TimmerguhFeature.BOUWBOEKJE.get()));
                    praat(player, npc, "boekje_kwijt");
                } else if (stap == TimmerguhVoortgang.KLAAR && isKnus(player)) {
                    knus(npc, player);
                } else if (stap == TimmerguhVoortgang.KLAAR) {
                    praat(player, npc, "knus_tip");
                } else {
                    praat(player, npc, "bedankt" + player.getRandom().nextInt(3));
                }
            }
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        int stap = TimmerguhVoortgang.stap(player);
        if (optie == UITLEG && stap == TimmerguhVoortgang.NIEUW) {
            praat(player, npc, "uitleg", new Object[] {}, new Praat.Optie(HELP, T + "optie.help"));
        } else if (optie == HELP && stap == TimmerguhVoortgang.NIEUW) {
            TimmerguhVoortgang.zet(player, TimmerguhVoortgang.MATERIAAL);
            praat(player, npc, "vraag", new Object[] {PLANKEN, WOL}, new Praat.Optie(OKE, T + "optie.oke"));
        } else if (optie == INLEVEREN && stap == TimmerguhVoortgang.MATERIAAL) {
            inleveren(npc, player);
        } else if (optie == OKE) {
            Praat.sluit(player);
        }
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    private static void materiaal(GuhNpcEntity npc, ServerPlayer player) {
        int planken = planken(player), wol = GuhQuests.count(player, Items.PINK_WOOL);
        if (planken >= PLANKEN && wol >= WOL) {
            praat(player, npc, "materiaal_heb", new Object[] {PLANKEN, WOL}, new Praat.Optie(INLEVEREN, T + "optie.inleveren"));
        } else {
            praat(player, npc, "nodig", new Object[] {Math.min(planken, PLANKEN), PLANKEN, Math.min(wol, WOL), WOL});
        }
    }

    /** Step 1 -> 2: takes the planks and wool, (a new roof if the last one is finished), loans the dakpluisjes. */
    public static boolean inleveren(GuhNpcEntity npc, ServerPlayer player) {
        if (planken(player) < PLANKEN || GuhQuests.count(player, Items.PINK_WOOL) < WOL || plekken(npc).isEmpty()) {
            materiaal(npc, player);
            return false;
        }
        neemPlanken(player, PLANKEN);
        GuhQuests.take(player, Items.PINK_WOOL, WOL);
        TimmerguhVoortgang.zet(player, TimmerguhVoortgang.DAK);
        GuhAdvancements.grant(player, "timmerguh_materiaal");
        boolean nieuw = open(npc) == 0;
        if (nieuw) {
            sloop(npc);
        }
        geefPluisjes(player, open(npc));
        npc.level().playSound(null, npc.blockPosition(), nl.juiced.guhs.feature.beroepen.BeroepenFeature.HAMER.get(), SoundSource.NEUTRAL, 1f, 1f);
        praat(player, npc, nieuw ? "nieuw_dak" : "dak_uitleg", new Object[] {open(npc)}, new Praat.Optie(OKE, T + "optie.aan_de_slag"));
        return true;
    }

    /** The roof is on (for this player at step 2): the flag, the small huisje "our first", step 3. */
    static void dakAf(GuhNpcEntity npc, ServerPlayer player) {
        if (TimmerguhVoortgang.stap(player) != TimmerguhVoortgang.DAK) {
            return;
        }
        TimmerguhVoortgang.zet(player, TimmerguhVoortgang.BEWONER);
        neemPluisjes(player);
        GuhAdvancements.grant(player, "timmerguh_dak");
        GidsFeature.grant(player, "verhalen/timmerguh_dak");
        Minigames.give(player, new ItemStack(HuisjeFeature.KLEIN.get()));
        GuhQuests.say(player, npc, T + "dak_af");
        GuhQuests.hint(player, T + "hint.bewoner");
        if (heeftBewoner(player)) {                // (someone already lives in a huisje of yours: straight on)
            GuhAdvancements.grant(player, "timmerguh_bewoner");
        }
    }

    /** Step 3 -> 4: the bouwboekje, a small huisje as a present, the timmermanshelmpje. */
    public static void klaar(GuhNpcEntity npc, ServerPlayer player) {
        if (TimmerguhVoortgang.stap(player) != TimmerguhVoortgang.BEWONER) {
            return;
        }
        TimmerguhVoortgang.zet(player, TimmerguhVoortgang.KLAAR);
        GuhAdvancements.grant(player, "timmerguh_bewoner");
        GuhAdvancements.grant(player, "timmerguh_klaar");
        GidsFeature.grant(player, "verhalen/timmerguh_bewoner");
        GidsFeature.grant(player, "verhalen/timmerguh_klaar");
        Minigames.give(player, new ItemStack(TimmerguhFeature.BOUWBOEKJE.get()));
        Minigames.give(player, new ItemStack(HuisjeFeature.KLEIN.get()));
        Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.TIMMER_HELMPJE)));
        feestje((ServerLevel) npc.level(), npc.blockPosition().above(2));
        praat(player, npc, "klaar", new Object[] {}, new Praat.Optie(OKE, T + "optie.dankjewel"));
    }

    /** The optional step: the gereedschapsriem. */
    public static void knus(GuhNpcEntity npc, ServerPlayer player) {
        if (TimmerguhVoortgang.stap(player) != TimmerguhVoortgang.KLAAR) {
            return;
        }
        TimmerguhVoortgang.zet(player, TimmerguhVoortgang.KNUS);
        GuhAdvancements.grant(player, "timmerguh_knus");
        GidsFeature.grant(player, "verhalen/timmerguh_knus");
        Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.TIMMER_GEREEDSCHAPSRIEM)));
        feestje((ServerLevel) npc.level(), npc.blockPosition().above(2));
        praat(player, npc, "knus_klaar", new Object[] {}, new Praat.Optie(OKE, T + "optie.dankjewel"));
    }

    /** Band moment: one of your guhs moved into a huisje. At step 3 that's what the Timmerguh wanted to hear. */
    static void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (m != Moment.HUISJE_IN || guh.getServer() == null) {
            return;
        }
        ServerPlayer eigenaar = speler != null ? speler : Band.eigenaarOnline(guh);
        if (eigenaar != null && TimmerguhVoortgang.stap(eigenaar) == TimmerguhVoortgang.BEWONER) {
            GuhAdvancements.grant(eigenaar, "timmerguh_bewoner");
            GidsFeature.grant(eigenaar, "verhalen/timmerguh_bewoner");
            eigenaar.displayClientMessage(Component.translatable("gui.guhs.timmerguh.vertel_het", guh.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
    }

    // =====================================================================================================================
    // the roof
    // =====================================================================================================================

    /** The roof spots round the Timmerguh (ghost tiles, found once and remembered with what goes on each). */
    public static List<BlockPos> plekken(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Plekken")) {
            List<BlockPos> ps = new ArrayList<>();
            List<Integer> delen = new ArrayList<>();
            ServerLevel level = (ServerLevel) npc.level();
            BlockPos at = npc.blockPosition();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = -BEREIK; x <= BEREIK; x++) {
                for (int z = -BEREIK; z <= BEREIK; z++) {
                    if (!level.hasChunkAt(at.offset(x, 0, z))) {
                        continue;
                    }
                    for (int y = -3; y <= 16; y++) {
                        p.set(at.getX() + x, at.getY() + y, at.getZ() + z);
                        BlockState s = level.getBlockState(p);
                        if (s.is(TimmerguhFeature.DAKPLEK.get())) {
                            ps.add(p.immutable());
                            delen.add(s.getValue(DakplekBlock.DEEL).ordinal());
                        }
                    }
                }
            }
            if (ps.isEmpty()) {
                return ps;
            }
            long[] longs = new long[ps.size()];
            for (int i = 0; i < longs.length; i++) {
                longs[i] = ps.get(i).asLong();
            }
            npc.roleData.putLongArray("Plekken", longs);
            npc.roleData.putIntArray("Delen", delen);
        }
        List<BlockPos> out = new ArrayList<>();
        for (long l : npc.roleData.getLongArray("Plekken")) {
            out.add(BlockPos.of(l));
        }
        return out;
    }

    private static DakplekBlock.Deel deel(GuhNpcEntity npc, int i) {
        int[] delen = npc.roleData.getIntArray("Delen");
        DakplekBlock.Deel[] alle = DakplekBlock.Deel.values();
        return i < delen.length ? alle[Math.floorMod(delen[i], alle.length)] : DakplekBlock.Deel.DAK;
    }

    /** How many ghost tiles are still open. */
    public static int open(GuhNpcEntity npc) {
        int n = 0;
        for (BlockPos p : plekken(npc)) {
            if (npc.level().getBlockState(p).is(TimmerguhFeature.DAKPLEK.get())) {
                n++;
            }
        }
        return n;
    }

    /** A new roof: every spot is a ghost tile again (what went there before is "in the other huisje"), the flag comes down. */
    public static void sloop(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<BlockPos> ps = plekken(npc);
        for (int i = 0; i < ps.size(); i++) {
            level.setBlock(ps.get(i), TimmerguhFeature.DAKPLEK.get().defaultBlockState().setValue(DakplekBlock.DEEL, deel(npc, i)), 3);
        }
        if (npc.roleData.contains("Vlag")) {
            BlockPos v = BlockPos.of(npc.roleData.getLong("Vlag"));
            if (level.getBlockState(v).getBlock() instanceof BannerBlock) {
                level.setBlock(v, Blocks.AIR.defaultBlockState(), 3);
            }
            if (level.getBlockState(v.below()).is(Blocks.SPRUCE_FENCE)) {
                level.setBlock(v.below(), Blocks.AIR.defaultBlockState(), 3);
            }
            npc.roleData.remove("Vlag");
        }
    }

    /** De vlag in top: a pink flag on a pole on the top of the dome (between the ears), and a little party. */
    public static void vlag(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<BlockPos> ps = plekken(npc);
        if (ps.isEmpty()) {
            return;
        }
        double x = 0, z = 0;
        int y = Integer.MIN_VALUE;
        for (int i = 0; i < ps.size(); i++) {
            BlockPos p = ps.get(i);
            x += p.getX();
            z += p.getZ();
            if (deel(npc, i) == DakplekBlock.Deel.DAK) {
                y = Math.max(y, p.getY());
            }
        }
        BlockPos paal = BlockPos.containing(x / ps.size(), y + 1, z / ps.size());
        if (level.getBlockState(paal).isAir() && level.getBlockState(paal.above()).isAir()) {
            level.setBlock(paal, Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            level.setBlock(paal.above(), Blocks.PINK_BANNER.defaultBlockState()
                    .setValue(BannerBlock.ROTATION, Math.floorMod(Math.round(npc.getYRot() / 22.5f), 16)), 3);
            npc.roleData.putLong("Vlag", paal.above().asLong());
        }
        feestje(level, paal.above());
    }

    private static void feestje(ServerLevel level, BlockPos at) {
        level.sendParticles(ParticleTypes.FIREWORK, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 40, 0.8, 0.8, 0.8, 0.15);
        level.sendParticles(ParticleTypes.HEART, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 12, 1.0, 0.6, 1.0, 0.1);
        level.playSound(null, at, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 1f, 1f);
        level.playSound(null, at, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.4f);
    }

    /**
     * A bit of roof was laid near a Timmerguh: when it was the last one, the flag goes up and everyone round about who was
     * laying tiles (step 2) is done with the roof.
     */
    public static void gelegd(ServerLevel level, BlockPos pos, ServerPlayer player) {
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(BEREIK + 8),
                n -> n.getKind() == GuhNpcEntity.Kind.TIMMERGUH)) {
            if (!plekken(npc).contains(pos)) {
                continue;
            }
            int open = open(npc);
            if (open > 0) {
                player.displayClientMessage(Component.translatable("gui.guhs.timmerguh.nog", open).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
            vlag(npc);
            for (Player p : level.players()) {
                if (p instanceof ServerPlayer sp && sp.distanceTo(npc) < 40 && TimmerguhVoortgang.stap(sp) == TimmerguhVoortgang.DAK) {
                    dakAf(npc, sp);
                }
            }
            return;
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    private static void praat(ServerPlayer player, GuhNpcEntity npc, String key) {
        praat(player, npc, key, new Object[] {});
    }

    private static void praat(ServerPlayer player, GuhNpcEntity npc, String key, Object[] args, Praat.Optie... opties) {
        Praat.open(player, npc, null, T + key, args, opties);
    }

    static int planken(ServerPlayer player) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ItemTags.PLANKS)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static void neemPlanken(ServerPlayer player, int n) {
        for (ItemStack s : player.getInventory().items) {
            if (n > 0 && s.is(ItemTags.PLANKS)) {
                int take = Math.min(n, s.getCount());
                s.shrink(take);
                n -= take;
            }
        }
    }

    static void geefPluisjes(ServerPlayer player, int n) {
        if (n > 0) {
            Minigames.give(player, new ItemStack(TimmerguhFeature.DAKPLUISJE.get(), n));
        }
    }

    /** Leftover dakpluisjes go back to the Timmerguh. */
    static void neemPluisjes(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(TimmerguhFeature.DAKPLUISJE.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** Does one of this player's guhs live in one of their huisjes? */
    public static boolean heeftBewoner(ServerPlayer player) {
        for (Huisje h : Huisjes.vanEigenaar(player.server, player.getUUID())) {
            for (UUID id : h.bewoners()) {
                if ("guh".equals(h.soort(id))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The bouwboekje in the ender chest counts too (it's not lost then). */
    private static boolean heeftBoekjeErgens(ServerPlayer player) {
        for (ItemStack s : player.getEnderChestInventory().getItems()) {
            if (s.is(TimmerguhFeature.BOUWBOEKJE.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The optional step: is there a huisje of this player (in a loaded spot) with a toy (a glijbaantje, wip, schommel, tunnel
     * or a knabbelbal) and a guhlampje in its home area?
     */
    public static boolean isKnus(ServerPlayer player) {
        for (Huisje h : Huisjes.vanEigenaar(player.server, player.getUUID())) {
            ServerLevel level = player.server.getLevel(h.dim());
            if (level != null && level.isLoaded(h.pos()) && speeltje(level, h) && lampje(level, h)) {
                return true;
            }
        }
        return false;
    }

    static boolean speeltje(ServerLevel level, Huisje h) {
        BlockPos m = BlockPos.containing(h.midden());
        boolean toestel = level.getPoiManager().getInRange(t -> t.is(SpeelgoedFeature.POI.getKey()), m, Huisjes.BEREIK,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).anyMatch(r -> h.inGebied(r.getPos()));
        return toestel || !level.getEntitiesOfClass(KnabbelbalEntity.class, h.gebied()).isEmpty();
    }

    static boolean lampje(ServerLevel level, Huisje h) {
        AABB g = h.gebied();
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(g.minX, g.minY, g.minZ), BlockPos.containing(g.maxX, g.maxY, g.maxZ))) {
            if (level.getBlockState(p).is(KlusjesFeature.GUHLAMPJE.get()) && h.inGebied(p)) {
                return true;
            }
        }
        return false;
    }

    /** (Tests) the flag's spot, or null. */
    @Nullable
    public static BlockPos vlagPlek(GuhNpcEntity npc) {
        return npc.roleData.contains("Vlag") ? BlockPos.of(npc.roleData.getLong("Vlag")) : null;
    }

    /** (Tests) forget the remembered roof spots. */
    public static void vergeet(GuhNpcEntity npc) {
        CompoundTag t = npc.roleData;
        t.remove("Plekken");
        t.remove("Delen");
        t.remove("Vlag");
    }

    private Timmerguh() {
    }
}
