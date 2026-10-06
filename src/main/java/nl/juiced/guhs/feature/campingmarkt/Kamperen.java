package nl.juiced.guhs.feature.campingmarkt;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (camping-markt): the Grillcamping. Questline {@link CampingmarktFeature#CAMPING} (per player):
 * <ol start="0">
 *   <li>talk to the Kampbaas-guh: a tent bag;</li>
 *   <li>pitch your tent on a free pitch ({@link #zetOp}) and hammer in its four pegs ({@link #slaHaring});</li>
 *   <li>ask the Houthakker-guh for fire wood and split {@link #HOUT_NODIG} logs on his chopping block ({@link #gehakt});</li>
 *   <li>put the wood on the big camp fire: the party starts ({@link Kampvuur});</li>
 *   <li>roast {@link #MARSHMALLOWS} marshmallows golden brown ({@link RoosterstokItem}) and tell the Kampbaas-guh: the recipe
 *       card of the Plantagebak and the camping outfit.</li>
 * </ol>
 * <b>Any number of players, side by side.</b> A pitched tent is the small template campingmarkt_tent placed over the pitch;
 * {@link Herstel} folds it up again (the template campingmarkt_plek) after {@link #TENT_TICKS}, so the pitch is free for the
 * next player. There are four pitches; whose tent stands where does not matter: what counts is in the player's own data
 * (the flag "tent", the pegs they hammered in, the counters "hout" and "geroosterd").
 * The residents of the camping ({@link #KAMPEERDERS}) are ordinary guhs that belong to the building: they can't be tamed, they
 * stay around their own spot and they come to the fire when it burns.
 */
public final class Kamperen {
    /** The two small templates (tools/features/camping_markt_bouw.py plek_leeg / plek_tent) and where the sign stands in them. */
    public static final Identifier PLEK = Guhs.id("campingmarkt_plek"), TENT = Guhs.id("campingmarkt_tent");
    public static final BlockPos PLEK_BORD = new BlockPos(3, 0, 4);
    /** How long a pitched tent stands before it is folded up for the next player (two minutes). */
    public static final int TENT_TICKS = 2400;
    public static final int HARINGEN = 4, HOUT_NODIG = 6, MARSHMALLOWS = 3, TIPS = 4;
    /** The player's own flags and counters in the questline. */
    public static final String TENT_VLAG = "tent", HOUT_VLAG = "hout", HOUT = "hout", GEROOSTERD = "geroosterd";
    /** (player data) the pegs of their tent this player hammered in: block positions. */
    public static final String HARING_PLEKKEN = "guhs_campingmarkt_haringen";
    /** (a resident's own data) where it lives. */
    public static final String THUIS = "guhs_campingmarkt_thuis";
    /** A resident that walks stays this close to its spot; further away it is put back. */
    public static final int THUIS_STRAAL = 9, THUIS_TERUG = 20;

    /** A resident guh of the camping (tools/features/camping_markt_bouw.py KAMPEERDERS, the same order). */
    public record Kampeerder(BlockPos plek, String variant, float schaal, boolean zit, float yaw, @Nullable GuhClothes hoofd,
                             @Nullable GuhClothes nek, @Nullable GuhClothes rug) {
    }

    public static final List<Kampeerder> KAMPEERDERS = List.of(
            new Kampeerder(new BlockPos(22, 5, 18), "asguh", 1.0f, true, 0f, GuhClothes.CAMPINGMARKT_HOEDJE, null, null),
            new Kampeerder(new BlockPos(18, 5, 22), "choco", 0.95f, true, -90f, null, GuhClothes.CAMPINGMARKT_HALSDOEK, null),
            new Kampeerder(new BlockPos(12, 4, 23), "mint", 1.0f, false, -90f, GuhClothes.CAMPINGMARKT_HOEDJE, null, GuhClothes.CAMPINGMARKT_RUGZAK),
            new Kampeerder(new BlockPos(30, 4, 17), "normal", 1.05f, false, 135f, null, GuhClothes.CAMPINGMARKT_HALSDOEK, GuhClothes.CAMPINGMARKT_RUGZAK),
            new Kampeerder(new BlockPos(13, 4, 17), "asguh", 0.6f, false, 20f, null, GuhClothes.CAMPINGMARKT_HALSDOEK, null));
    public static final String KAMPEERDER_ID = "campingmarkt_kampeerder_";
    public static final int KAMPEERDER_ZINNEN = 6;

    private Kamperen() {
    }

    static void meld(ServerPlayer p, String key, ChatFormatting kleur, Object... args) {
        p.sendOverlayMessage(Component.translatable("gui.guhs.campingmarkt." + key, args).withStyle(kleur));
    }

    // =================================================================================================================
    // the tent
    // =================================================================================================================

    /** How the small templates are turned for a sign that looks this way (in them the door looks south). */
    public static Rotation draai(Direction deur) {
        return switch (deur) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** The corner (template 0, 0, 0) of the pitch whose sign stands at {@code bord}. */
    public static BlockPos hoek(BlockPos bord, Rotation draai) {
        return bord.subtract(StructureTemplate.transform(PLEK_BORD, Mirror.NONE, draai, BlockPos.ZERO));
    }

    /** A player clicked the sign of a pitch: with the tent bag, on the right step, their tent is pitched there. */
    public static void zetOp(ServerPlayer p, BlockPos bord) {
        ServerLevel level = p.level();
        BlockState state = level.getBlockState(bord);
        if (!(state.getBlock() instanceof CampingmarktBlocks.Kampeerplek)) {
            return;
        }
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        int stap = lijn.stap(p);
        if (state.getValue(CampingmarktBlocks.Kampeerplek.BEZET)) {
            meld(p, stap == 1 && lijn.vlag(p, TENT_VLAG) ? "plek.haringen" : stap == 1 ? "plek.bezet" : "plek.tent", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (stap < 1) {
            meld(p, "plek.eerst_praten", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (stap > 1) {
            meld(p, "plek.klaar", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (GuhQuests.count(p, CampingmarktFeature.TENTZAK.get()) == 0) {
            meld(p, "plek.geen_zak", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (!plaatsTent(level, bord)) {
            meld(p, "plek.geen_ruimte", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        lijn.vlag(p, TENT_VLAG, true);
        GuhQuests.saved(p).remove(HARING_PLEKKEN);
        meld(p, "plek.staat", ChatFormatting.GOLD);
    }

    /**
     * Pitches a tent on the pitch of this sign: the template campingmarkt_tent over it, and {@link Herstel} puts the empty
     * pitch back after {@link #TENT_TICKS}. False when something is in the way (or a tent stands there already).
     */
    public static boolean plaatsTent(ServerLevel level, BlockPos bord) {
        BlockState state = level.getBlockState(bord);
        if (!(state.getBlock() instanceof CampingmarktBlocks.Kampeerplek) || state.getValue(CampingmarktBlocks.Kampeerplek.BEZET)) {
            return false;
        }
        Direction deur = state.getValue(CampingmarktBlocks.Kampeerplek.FACING);
        Rotation draai = draai(deur);
        BlockPos hoek = hoek(bord, draai);
        Optional<StructureTemplate> tent = level.getStructureManager().get(TENT);
        if (tent.isEmpty()) {
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(draai).setMirror(Mirror.NONE).setIgnoreEntities(true);
        BoundingBox box = tent.get().getBoundingBox(settings, hoek);
        for (BlockPos q : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!q.equals(bord) && !level.getBlockState(q).canBeReplaced()) {
                return false;
            }
        }
        // nobody ends up inside the canvas: whoever stands on the pitch steps out in front of the door
        Vec3 voor = Vec3.atBottomCenterOf(bord.relative(deur));
        for (Player speler : level.getEntitiesOfClass(Player.class, AABB.of(box))) {
            speler.teleportTo(voor.x, voor.y, voor.z);
        }
        tent.get().placeInWorld(level, hoek, hoek, settings, level.getRandom(), Block.UPDATE_ALL);
        Herstel.na(level, hoek, PLEK, draai, TENT_TICKS);
        Vec3 midden = AABB.of(box).getCenter();
        level.playSound(null, bord, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.2f, 0.7f);
        level.playSound(null, bord, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
        level.sendParticles(ParticleTypes.POOF, midden.x, midden.y, midden.z, 30, 1.6, 0.8, 1.4, 0.02);
        return true;
    }

    /** The pegs of their own tent this player hammered in so far. */
    public static int haringen(ServerPlayer p) {
        return GuhQuests.saved(p).getLongArray(HARING_PLEKKEN).orElse(new long[0]).length;
    }

    /** A player hit a tent peg: it goes in; four pegs of their own tent and the tent stands (step 1 is done). */
    public static void slaHaring(ServerPlayer p, BlockPos pos, BlockState state) {
        ServerLevel level = p.level();
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.25f, 1.9f);
        if (!state.getValue(CampingmarktBlocks.Haring.VAST)) {
            level.setBlock(pos, state.setValue(CampingmarktBlocks.Haring.VAST, true), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.1);
        }
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        if (lijn.stap(p) != 1) {
            return;   // (not pitching a tent right now: a tap for fun)
        }
        if (!lijn.vlag(p, TENT_VLAG)) {
            meld(p, "haring.eerst_tent", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        CompoundTag saved = GuhQuests.saved(p);
        long[] al = saved.getLongArray(HARING_PLEKKEN).orElse(new long[0]);
        long plek = pos.asLong();
        if (Arrays.stream(al).anyMatch(l -> l == plek)) {
            meld(p, "haring.al", ChatFormatting.YELLOW, al.length, HARINGEN);
            return;
        }
        long[] nu = Arrays.copyOf(al, al.length + 1);
        nu[al.length] = plek;
        if (nu.length < HARINGEN) {
            saved.putLongArray(HARING_PLEKKEN, nu);
            meld(p, "haring.tik", ChatFormatting.YELLOW, nu.length, HARINGEN);
            return;
        }
        saved.remove(HARING_PLEKKEN);
        lijn.vlag(p, TENT_VLAG, false);
        GuhQuests.take(p, CampingmarktFeature.TENTZAK.get(), 64);
        lijn.verder(p, 1);
        meld(p, "haring.klaar", ChatFormatting.GOLD);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 1.5, 0.6, 1.5, 0.0);
    }

    // =================================================================================================================
    // the fire wood
    // =================================================================================================================

    /** A player split a log on the chopping block: it counts while they are chopping for the Houthakker-guh. */
    public static void gehakt(ServerPlayer p, BlockPos pos) {
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        if (lijn.stap(p) != 2) {
            return;
        }
        if (!lijn.vlag(p, HOUT_VLAG)) {
            meld(p, "hak.eerst_praten", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        int n = lijn.teller(p, HOUT) + 1;
        lijn.teller(p, HOUT, n);
        if (n < HOUT_NODIG) {
            meld(p, "hak.tjak", ChatFormatting.YELLOW, n, HOUT_NODIG);
            return;
        }
        lijn.verder(p, 2);
        Minigames.give(p, new ItemStack(CampingmarktFeature.BRANDHOUT.get()));
        meld(p, "hak.klaar", ChatFormatting.GOLD);
        p.level().playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** How many marshmallows (the item tag guhs:knus/marshmallow) the player carries. */
    public static int marshmallows(ServerPlayer p) {
        int n = 0;
        for (ItemStack stack : p.getInventory().getNonEquipmentItems()) {
            if (stack.is(KnusTags.MARSHMALLOW)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    // =================================================================================================================
    // the two guhs
    // =================================================================================================================

    /** The Kampbaas-guh: gives the tent bag, sends you on, hands out the roasting stick and, at the end, the rewards. */
    public static final class KampbaasRol extends QuestRol {
        private static final String Q = "quest.guhs.campingmarkt.kampbaas.";

        public KampbaasRol() {
            super(CampingmarktFeature.CAMPING);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            Verhaallijn lijn = CampingmarktFeature.CAMPING;
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.0f);
            switch (stap) {
                case 0 -> {
                    zeg(p, npc, Q + "hallo1");
                    zeg(p, npc, Q + "hallo2");
                    geef(p, new ItemStack(CampingmarktFeature.TENTZAK.get()));
                    verder(p, 0);
                    hint(p, "quest.guhs.campingmarkt.hint.tent");
                }
                case 1 -> {
                    if (geefAlsKwijt(p, CampingmarktFeature.TENTZAK.get())) {
                        zeg(p, npc, Q + "tentzak_weer");
                    } else if (lijn.vlag(p, TENT_VLAG)) {
                        zeg(p, npc, Q + "haringen", HARINGEN - haringen(p));
                    } else {
                        zeg(p, npc, Q + "tent");
                    }
                }
                case 2 -> zeg(p, npc, Q + "hout");
                case 3 -> zeg(p, npc, Q + "vuur");
                case 4 -> {
                    int n = lijn.teller(p, GEROOSTERD);
                    if (n >= MARSHMALLOWS) {
                        klaar(npc, p);
                        return;
                    }
                    boolean stok = geefAlsKwijt(p, CampingmarktFeature.ROOSTERSTOK.get());
                    boolean zakje = marshmallows(p) == 0;
                    if (zakje) {
                        geef(p, new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 4));
                    }
                    zeg(p, npc, Q + (stok ? "stok" : zakje ? "marshmallows_weer" : "rooster"), MARSHMALLOWS - n);
                    if (stok) {
                        zeg(p, npc, Q + "stok_uitleg");
                        hint(p, "quest.guhs.campingmarkt.hint.rooster");
                    }
                }
                default -> {
                    if (geefAlsKwijt(p, CampingmarktFeature.RECEPT_PLANTAGEBAK.get())) {
                        zeg(p, npc, Q + "recept_weer");
                    } else {
                        zeg(p, npc, Q + "tip" + npc.getRandom().nextInt(TIPS));
                    }
                    npc.openShop(p);
                }
            }
        }

        /** Step 4 -> done: the recipe card of the Plantagebak and the camping outfit, once. */
        private void klaar(GuhNpcEntity npc, ServerPlayer p) {
            if (!verder(p, 4)) {
                return;
            }
            zeg(p, npc, Q + "klaar1");
            zeg(p, npc, Q + "klaar2");
            geefEenmalig(p, "beloning", new ItemStack(CampingmarktFeature.RECEPT_PLANTAGEBAK.get()),
                    new ItemStack(ModItems.clothingItem(GuhClothes.CAMPINGMARKT_HOEDJE)), new ItemStack(ModItems.clothingItem(GuhClothes.CAMPINGMARKT_HALSDOEK)),
                    new ItemStack(ModItems.clothingItem(GuhClothes.CAMPINGMARKT_RUGZAK)));
            zichtbaar(p, "barbecuether/camping_markt_camping");
            npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.4, npc.getZ(), 8, 0.5, 0.4, 0.5, 0.02);
            }
        }

        /** After the questline: marshmallows, a new roasting stick, and tent canvas to build your own tents at home. */
        @Nullable
        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            MerchantOffers offers = new MerchantOffers();
            offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 3), new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 4)));
            offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 8), new ItemStack(CampingmarktFeature.ROOSTERSTOK.get())));
            for (String kleur : CampingmarktFeature.KLEUREN) {
                offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 6), new ItemStack(CampingmarktFeature.TENTDOEK.get(kleur).blok().get(), 8)));
            }
            return offers;
        }
    }

    static MerchantOffer offer(ItemCost prijs, ItemStack wat) {
        return new MerchantOffer(prijs, Optional.empty(), wat, Integer.MAX_VALUE, 0, 0);
    }

    /** The Houthakker-guh: lets you chop on his block, and gives the bundle of fire wood again when you lost it. */
    public static final class HouthakkerRol extends QuestRol {
        private static final String Q = "quest.guhs.campingmarkt.houthakker.";

        public HouthakkerRol() {
            super(CampingmarktFeature.CAMPING);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            Verhaallijn lijn = CampingmarktFeature.CAMPING;
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.75f);
            if (stap < 2) {
                zeg(p, npc, Q + "te_vroeg");
            } else if (stap == 2) {
                if (!lijn.vlag(p, HOUT_VLAG)) {
                    lijn.vlag(p, HOUT_VLAG, true);
                    zeg(p, npc, Q + "hallo1");
                    zeg(p, npc, Q + "hallo2");
                    hint(p, "quest.guhs.campingmarkt.hint.hak");
                } else {
                    zeg(p, npc, Q + "nog", HOUT_NODIG - lijn.teller(p, HOUT));
                }
            } else if (stap == 3) {
                zeg(p, npc, geefAlsKwijt(p, CampingmarktFeature.BRANDHOUT.get()) ? Q + "hout_weer" : Q + "naar_vuur");
            } else {
                zeg(p, npc, Q + "tip" + npc.getRandom().nextInt(TIPS));
                if (stap >= 5) {
                    npc.openShop(p);
                }
            }
        }

        /** After the questline he sells wood of the Guhbarbecuether. */
        @Nullable
        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            MerchantOffers offers = new MerchantOffers();
            offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 4), new ItemStack(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.SATE_STAM.get(), 8)));
            offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 4), new ItemStack(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.WORST_STAM.get(), 8)));
            offers.add(offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 2), new ItemStack(net.minecraft.world.item.Items.CAMPFIRE)));
            return offers;
        }
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        return switch (stap) {
            case 1 -> List.of(Verhaallijn.nodig("guhs:campingmarkt_tentzak", GuhQuests.count(p, CampingmarktFeature.TENTZAK.get()) > 0
                            || lijn.vlag(p, TENT_VLAG) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:campingmarkt_haring", lijn.vlag(p, TENT_VLAG) ? haringen(p) : 0, HARINGEN));
            case 2 -> List.of(Verhaallijn.nodig("guhs:campingmarkt_hakblok", "gui.guhs.campingmarkt.nodig.hout", lijn.teller(p, HOUT), HOUT_NODIG));
            case 3 -> List.of(Verhaallijn.nodig("guhs:campingmarkt_brandhout", GuhQuests.count(p, CampingmarktFeature.BRANDHOUT.get()), 1));
            case 4 -> List.of(Verhaallijn.nodig("guhs:marshmallow_knabbel", "gui.guhs.campingmarkt.nodig.goudbruin", lijn.teller(p, GEROOSTERD), MARSHMALLOWS));
            default -> List.of();
        };
    }

    // =================================================================================================================
    // the residents
    // =================================================================================================================

    /** Makes resident i of the camping (not yet in the world): what {@link Bezetting} calls for a copy that misses it. */
    @Nullable
    public static Entity kampeerder(ServerLevel level, Vec3 plek, Rotation draai, int i) {
        Kampeerder k = KAMPEERDERS.get(i);
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.byId(k.variant()));
        guh.setGuhScale(k.schaal());
        for (GuhClothes c : new GuhClothes[]{k.hoofd(), k.nek(), k.rug()}) {
            if (c != null) {
                guh.wear(c);
            }
        }
        guh.setYRot(k.yaw());
        float y = guh.rotate(draai);
        guh.snapTo(plek.x, plek.y, plek.z, y, 0f);
        guh.setYBodyRot(y);
        guh.setYHeadRot(y);
        if (k.zit()) {
            guh.setOrderedToSit(true);
            guh.setInSittingPose(true);
        }
        guh.setPersistenceRequired();
        return guh;
    }

    /** Which resident of the camping this guh is (-1: none). */
    public static int kampeerder(GuhEntity guh) {
        String tag = guh.getPersistentData().getStringOr(Bezetting.TAG, "");
        if (!tag.startsWith(KAMPEERDER_ID)) {
            return -1;
        }
        int at = tag.indexOf('@');
        try {
            return Integer.parseInt(tag.substring(KAMPEERDER_ID.length(), at < 0 ? tag.length() : at));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * (GuhHooks.tick, every guh, server side) a resident that walks keeps to the camping: its first spot is its home
     * ({@link #THUIS}); strayed further than {@link #THUIS_TERUG} it is put back there.
     */
    public static void bewonerTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 40 == 0 && kampeerder(guh) >= 0) {
            houdThuis(guh);
        }
    }

    /** What {@link #bewonerTick} does for a resident, every two seconds. */
    static void houdThuis(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        if (!data.contains(THUIS)) {
            data.putLong(THUIS, guh.blockPosition().asLong());
            guh.setHomeTo(guh.blockPosition(), THUIS_STRAAL);
            return;
        }
        BlockPos thuis = BlockPos.of(data.getLongOr(THUIS, 0L));
        if (!guh.hasHome()) {
            guh.setHomeTo(thuis, THUIS_STRAAL);
        }
        if (!guh.isOrderedToSit() && guh.distanceToSqr(Vec3.atBottomCenterOf(thuis)) > THUIS_TERUG * THUIS_TERUG) {
            guh.getNavigation().stop();
            guh.teleportTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 0.5);
        }
    }

    /** (GuhHooks.klik) a click on a resident: it says something about camping; it is never fed, tamed or dressed. */
    public static InteractionResult bewonerKlik(GuhEntity guh, Player player, InteractionHand hand) {
        int i = kampeerder(guh);
        if (i < 0) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            long nu = p.level().getGameTime();
            if (nu - p.getPersistentData().getLongOr("guhs_campingmarkt_praat", 0L) > 20) {
                p.getPersistentData().putLong("guhs_campingmarkt_praat", nu);
                int zin = Math.floorMod(i * 2 + (int) (nu / 600), KAMPEERDER_ZINNEN);
                GuhQuests.say(p, guh, "quest.guhs.campingmarkt.kampeerder." + zin);
                guh.playSound(ModSounds.GUH_AMBIENT.get(), 1f, guh.getVoicePitch());
                GuhAdvancements.grant(p, "camping_markt_kampeerder");
            }
        }
        return InteractionResult.SUCCESS;
    }
}
