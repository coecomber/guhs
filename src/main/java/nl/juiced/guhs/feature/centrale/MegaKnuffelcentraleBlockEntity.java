package nl.juiced.guhs.feature.centrale;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techbron.BronBlockEntity;
import nl.juiced.guhs.feature.techbron.GuhTrek;
import nl.juiced.guhs.feature.techbron.TechbronGetallen;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Mega-knuffelcentrale's source: {@link CentraleGetallen#PER_GUH} VK for every tamed guh that cuddles on the giant
 * cushion (at most {@link CentraleGetallen#MAX_GUHS}), times how well they are cared for ({@link #zorgProcent}):
 * <ul>
 *   <li>food: every cuddling guh eats a portion per {@link CentraleGetallen#VOER_SECONDEN} s from the food compartment
 *       (kaasknabbels, roze broodjes, blocks of kaasknabbels); no food: {@link CentraleGetallen#HONGER_PROCENT}%;</li>
 *   <li>fresh blankets: one wool per in-game day; stale blankets: {@link CentraleGetallen#GEEN_DEKENS_PROCENT}%.</li>
 * </ul>
 * Guhs come by themselves like on the Knuffelgenerator ({@link GuhTrek}); lying there makes them blij. Pipes and hoppers
 * fill both compartments ({@link #handler}), nothing comes out that way. One per net ({@link BronSoort#MEGA_KNUFFELCENTRALE}).
 */
public class MegaKnuffelcentraleBlockEntity extends BronBlockEntity {
    /** How far the spots are apart: a little more than a guh is wide (0.9), so guhs on their spots do not push each other. */
    private static final double TUSSEN = 0.95, BREED_TUSSEN = 0.92;

    private final GuhTrek.Groep groep = new GuhTrek.Groep(CentraleGetallen.MAX_GUHS, Emote.SLAPEN);
    private final Bakje bakje = new Bakje();
    private int guhs;
    /** Portions taken out of the food compartment that are not eaten yet. */
    private int porties;
    /** Guh-seconds of cuddling since the last portion was eaten. */
    private int gegeten;
    /** Until when (game time) the blankets are fresh. */
    private long dekensTot;
    /** Was there food at the last look? */
    private boolean gevoerd;

    public MegaKnuffelcentraleBlockEntity(BlockPos pos, BlockState state) {
        super(CentraleFeature.CENTRALE_BE.get(), pos, state);
    }

    // =====================================================================================================================
    // what goes in
    // =====================================================================================================================

    public static boolean isVoer(ItemStack stack) {
        return portiesVan(stack) > 0;
    }

    public static boolean isWol(ItemStack stack) {
        return stack.is(ItemTags.WOOL);
    }

    /** How many portions one of this item is (0: not food for the centrale). */
    public static int portiesVan(ItemStack stack) {
        if (stack.is(ModItems.KAAS_KNABBELS.get())) {
            return CentraleGetallen.PORTIE_KNABBEL;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return switch (id) {
            case "guhs:block_of_kaasknabbels" -> CentraleGetallen.PORTIE_BLOK;
            case "guhs:roze_broodje" -> CentraleGetallen.PORTIE_BROODJE;
            default -> 0;
        };
    }

    /** Puts food or wool of this stack in (the stack shrinks unless {@code gratis}); returns how many went in. */
    public int stop(ItemStack stack, boolean gratis) {
        if (!isVoer(stack) && !isWol(stack)) {
            return 0;
        }
        ItemStack rest = Kisten.stop(bakje, stack);
        int erin = stack.getCount() - rest.getCount();
        if (erin > 0 && !gratis) {
            stack.shrink(erin);
        }
        if (erin > 0) {
            sync();
        }
        return erin;
    }

    /** How many food items lie in the compartment, plus the portions already taken out. */
    public long voorraad() {
        return Kisten.tel(bakje, MegaKnuffelcentraleBlockEntity::isVoer);
    }

    /** How many wool lies in the blanket compartment. */
    public long wol() {
        return Kisten.tel(bakje, MegaKnuffelcentraleBlockEntity::isWol);
    }

    // =====================================================================================================================
    // care
    // =====================================================================================================================

    public int guhs() {
        return guhs;
    }

    /** (tests, commands) Sets how many guhs cuddle, without waiting for them to walk over. */
    public void zetGuhs(int aantal) {
        guhs = Math.max(0, Math.min(CentraleGetallen.MAX_GUHS, aantal));
        sync();
    }

    public boolean dekensVers() {
        return level != null && level.getGameTime() < dekensTot;
    }

    public boolean gevoerd() {
        return gevoerd;
    }

    /** How well the guhs are cared for right now, in percent. */
    public int zorgProcent() {
        if (!gevoerd) {
            return CentraleGetallen.HONGER_PROCENT;
        }
        return dekensVers() ? 100 : CentraleGetallen.GEEN_DEKENS_PROCENT;
    }

    /** Is there a portion (in hand, or a food item to take)? Takes a food item when needed. */
    private boolean heeftVoer() {
        if (porties > 0) {
            return true;
        }
        ItemStack uit = Kisten.neem(bakje, MegaKnuffelcentraleBlockEntity::isVoer, 1);
        if (uit.isEmpty()) {
            return false;
        }
        porties += portiesVan(uit);
        return true;
    }

    /** Fresh blankets when the old ones are stale and there is wool. */
    private void ververs(ServerLevel level) {
        if (level.getGameTime() < dekensTot) {
            return;
        }
        if (!Kisten.neem(bakje, MegaKnuffelcentraleBlockEntity::isWol, 1).isEmpty()) {
            dekensTot = level.getGameTime() + CentraleGetallen.DEKENS_TICKS;
            setChanged();
        }
    }

    /** One second of care: the cuddling guhs eat, the blankets get freshened. Public for tests and commands. */
    public void verzorg(ServerLevel level) {
        ververs(level);
        boolean was = gevoerd;
        if (guhs <= 0) {
            gevoerd = heeftVoer();
        } else {
            gegeten += guhs;
            gevoerd = heeftVoer();
            while (gevoerd && gegeten >= CentraleGetallen.VOER_SECONDEN) {
                gegeten -= CentraleGetallen.VOER_SECONDEN;
                porties--;
                gevoerd = heeftVoer();
            }
            if (!gevoerd) {
                gegeten = Math.min(gegeten, CentraleGetallen.VOER_SECONDEN);
            }
        }
        if (was != gevoerd) {
            sync();
        } else {
            setChanged();
        }
    }

    // =====================================================================================================================
    // vadskracht
    // =====================================================================================================================

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.MEGA_KNUFFELCENTRALE;
    }

    @Override
    public int vadsAanbod() {
        return Math.round(Math.min(guhs, CentraleGetallen.MAX_GUHS) * CentraleGetallen.PER_GUH * zorgProcent() / 100f);
    }

    @Override
    protected Snoet gezicht() {
        return guhs <= 0 ? Snoet.SLAAPT : guhs >= CentraleGetallen.MAX_GUHS && zorgProcent() >= 100 ? Snoet.VOL : Snoet.WERKT;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (guhs <= 0) {
            regels.accept(Component.translatable("gui.guhs.centrale.leeg", TechbronGetallen.BEREIK).withStyle(ChatFormatting.GRAY));
        } else {
            regels.accept(Component.translatable("gui.guhs.centrale.guhs", guhs, CentraleGetallen.MAX_GUHS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        regels.accept(zorgRegel());
    }

    private Component zorgRegel() {
        if (!gevoerd) {
            return Component.translatable("gui.guhs.centrale.honger", CentraleGetallen.HONGER_PROCENT).withStyle(ChatFormatting.RED);
        }
        if (!dekensVers()) {
            return Component.translatable("gui.guhs.centrale.dekens_oud", CentraleGetallen.GEEN_DEKENS_PROCENT).withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("gui.guhs.centrale.tevreden", voorraad(), wol()).withStyle(ChatFormatting.GREEN);
    }

    /** What a click with an empty hand (or with food or wool) says. */
    public Component status() {
        return Component.translatable("gui.guhs.centrale.status", guhs, CentraleGetallen.MAX_GUHS, zorgProcent(), voorraad(), wol());
    }

    // =====================================================================================================================
    // the guhs
    // =====================================================================================================================

    /** The sixteen spots: a grid of 6 x 3 over the middle of the 5 x 3 cushion, the two back corners left out. */
    public List<Vec3> plekken() {
        AABB vloer = vloer();
        Vec3 midden = new Vec3((vloer.minX + vloer.maxX) / 2, vloer.minY + MegaKnuffelcentraleBlock.HOOGTE / 16.0, (vloer.minZ + vloer.maxZ) / 2);
        Direction facing = getBlockState().getValue(BronBlock.FACING);
        Direction rechts = facing.getClockWise(), achter = facing.getOpposite();
        List<Vec3> uit = new ArrayList<>(CentraleGetallen.MAX_GUHS);
        for (int rij = 0; rij < 3 && uit.size() < CentraleGetallen.MAX_GUHS; rij++) {
            for (int kol = 0; kol < 6 && uit.size() < CentraleGetallen.MAX_GUHS; kol++) {
                double r = (kol - 2.5) * BREED_TUSSEN, v = (rij - 1) * TUSSEN;
                uit.add(midden.add(rechts.getStepX() * r + achter.getStepX() * v, 0, rechts.getStepZ() * r + achter.getStepZ() * v));
            }
        }
        return uit;
    }

    /** The box a guh must be in to lie on the cushion. */
    public AABB zone() {
        return zone(0.5);
    }

    @Override
    protected void tik(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), TechbronGetallen.KIJK) != 0) {
            return;
        }
        kijk(level);
        verzorg(level);
        if (guhs > 0 && level.getRandom().nextInt(4) == 0) {
            AABB z = vloer();
            level.sendParticles(ParticleTypes.HEART, z.getCenter().x, z.minY + 0.8, z.getCenter().z, 1, 1.2, 0.1, 1.2, 0);
        }
    }

    /** One look at the guhs around the cushion (once per second; tests call it to skip the wait). */
    public void kijk(ServerLevel level) {
        int nu = groep.kijk(level, worldPosition, zone(), zone(0), plekken(), Float.NaN);
        for (GuhEntity guh : groep.bezig()) {
            Band.maakBlij(guh, TechbronGetallen.BLIJ_NA_KNUFFEL);
        }
        if (nu > 0) {
            beloon("mega_knuffelcentrale");
        }
        if (nu != guhs) {
            guhs = nu;
            sync();
        }
    }

    // =====================================================================================================================
    // pipes and hoppers: food and wool in, nothing out
    // =====================================================================================================================

    public ResourceHandler<ItemResource> handler(@Nullable Direction kant) {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return bakje.size();
            }

            @Override
            public ItemResource getResource(int index) {
                return bakje.getResource(index);
            }

            @Override
            public long getAmountAsLong(int index) {
                return bakje.getAmountAsLong(index);
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return bakje.getCapacityAsLong(index, resource);
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return bakje.isValid(index, resource);
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return bakje.insert(index, resource, amount, transaction);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    /** Slot 0: food, slot 1: wool. */
    private final class Bakje extends ItemStacksResourceHandler {
        Bakje() {
            super(2);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index == 0 ? resource.test(MegaKnuffelcentraleBlockEntity::isVoer) : resource.test(MegaKnuffelcentraleBlockEntity::isWol);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return CentraleGetallen.BAKJE_MAX;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    }

    // =====================================================================================================================
    // removal, saving
    // =====================================================================================================================

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            groep.laatLos(server, pos);
            for (ItemStack stack : bakje.copyToList()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        guhs = Math.max(0, in.getIntOr("Guhs", 0));
        porties = Math.max(0, in.getIntOr("Porties", 0));
        gegeten = Math.max(0, in.getIntOr("Gegeten", 0));
        dekensTot = in.getLongOr("DekensTot", 0L);
        gevoerd = in.getBooleanOr("Gevoerd", false);
        bakje.deserialize(in.childOrEmpty("Bakje"));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        uit.putInt("Guhs", guhs);
        uit.putInt("Porties", porties);
        uit.putInt("Gegeten", gegeten);
        uit.putLong("DekensTot", dekensTot);
        uit.putBoolean("Gevoerd", gevoerd);
        bakje.serialize(uit.child("Bakje"));
    }
}
