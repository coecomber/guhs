package nl.juiced.guhs.feature.fossiel;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Fossiel-broedmachine's block entity: slot 0 holds a fossil, slot 1 kaasknabbels (both fill from pipes and hoppers
 * too). While it has vadskracht and a fossil, it first eats {@link FossielFeature#KNABBELS} knabbels, then broods for
 * {@link FossielFeature#BROED_TICKS} ticks (a whole in-game day: about one oerguh per day per machine), and then the egg
 * hatches: a baby oerguh of the fossil's kind ({@link OerSoort}) pops out on top, tame for whoever placed the machine. The
 * kind and the progress are synced (the egg turns in its glass: client/BroedmachineRenderer).
 */
public class BroedmachineBlockEntity extends MachineBlockEntity {
    public static final int FOSSIEL = 0, KNABBEL = 1;
    /** Synced every this many ticks while brooding (the egg's progress for the renderer and the hover text). */
    private static final int SYNC = 100;

    @Nullable
    private OerSoort ei;
    private int voortgang;

    public BroedmachineBlockEntity(BlockPos pos, BlockState state) {
        super(FossielFeature.BROEDMACHINE_BE.get(), pos, state, FossielFeature.KRACHT, 2);
    }

    /** The kind in the glass (the fossil being brooded, else the fossil in slot 0), or null. */
    @Nullable
    public OerSoort ei() {
        return ei;
    }

    /** Brooding progress 0..1. */
    public float voortgang() {
        return Math.min(1f, voortgang / (float) FossielFeature.BROED_TICKS);
    }

    @Override
    protected boolean isInvoer(int vak) {
        return true;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return false;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        ItemStack stack = wat.toStack(1);
        return vak == FOSSIEL ? OerSoort.van(stack) != null : stack.is(ModItems.KAAS_KNABBELS.get());
    }

    private ItemStack fossiel() {
        ItemResource r = vakken().getResource(FOSSIEL);
        return r.isEmpty() ? ItemStack.EMPTY : r.toStack((int) vakken().getAmountAsLong(FOSSIEL));
    }

    private long knabbels() {
        return Kisten.tel(vakken(), s -> s.is(ModItems.KAAS_KNABBELS.get()));
    }

    @Override
    protected boolean kanWerken() {
        return OerSoort.van(fossiel()) != null && (voortgang > 0 || knabbels() >= FossielFeature.KNABBELS);
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (voortgang == 0) {
            Kisten.neem(vakken(), s -> s.is(ModItems.KAAS_KNABBELS.get()), FossielFeature.KNABBELS);
            ei = OerSoort.van(fossiel());
            sl.playSound(null, worldPosition, SoundEvents.SNIFFER_EGG_PLOP, SoundSource.BLOCKS, 1f, 1.1f);
            sync();
        }
        voortgang++;
        if (voortgang % SYNC == 0) {
            sync();
            setChanged();
        }
        if (voortgang % 40 == 0) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                    1, 0.2, 0.15, 0.2, 0);
        }
        if (voortgang >= FossielFeature.BROED_TICKS) {
            uitkomen(sl);
        }
    }

    /** The egg hatches: the fossil is used up and a baby oerguh stands on top of the machine. */
    @Nullable
    public GuhEntity uitkomen(ServerLevel sl) {
        OerSoort soort = ei != null ? ei : OerSoort.van(fossiel());
        voortgang = 0;
        ei = null;
        if (soort == null) {
            sync();
            return null;
        }
        Kisten.neem(vakken(), s -> OerSoort.van(s) == soort, 1);
        GuhEntity baby = ModEntities.GUH.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (baby == null) {
            sync();
            return null;
        }
        baby.setVariant(soort.variant);
        baby.setAge(-24000);
        baby.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, sl.getRandom().nextFloat() * 360f, 0f);
        ServerPlayer baas = eigenaar() == null ? null : sl.getServer().getPlayerList().getPlayer(eigenaar());
        if (baas != null) {
            baby.tame(baas);
        } else if (eigenaar() != null) {
            baby.setOwnerUUID(eigenaar());
            baby.setTame(true, true);
        }
        sl.addFreshEntity(baby);
        if (baby.isTame()) {
            nl.juiced.guhs.feature.band.BandEvents.geboren(baby);
        }
        sl.playSound(null, worldPosition, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1f, 1.2f);
        sl.sendParticles(ParticleTypes.HEART, baby.getX(), baby.getY() + 0.6, baby.getZ(), 8, 0.4, 0.3, 0.4, 0.02);
        if (baas != null) {
            baas.sendSystemMessage(Component.translatable("gui.guhs.fossiel.uitgekomen", Component.translatable("entity.guhs.guh." + soort.variant.id()))
                    .withStyle(ChatFormatting.GOLD));
            nl.juiced.guhs.quest.GuhAdvancements.grant(baas, "fossiel_uitgebroed");
            nl.juiced.guhs.quest.GuhAdvancements.grant(baas, "fossiel_" + soort.id);
        }
        sync();
        setChanged();
        return baby;
    }

    /** For tests and the status line: jump the brooding to this many ticks. */
    public void zetVoortgang(int ticks) {
        voortgang = ticks;
        if (ei == null) {
            ei = OerSoort.van(fossiel());
        }
        sync();
    }

    public int voortgangTicks() {
        return voortgang;
    }

    @Override
    protected void vakkenVeranderd() {
        if (voortgang == 0) {
            OerSoort nu = OerSoort.van(fossiel());
            if (nu != ei) {
                ei = nu;
                sync();
            }
        }
    }

    /** The status a click shows. */
    public Component status() {
        OerSoort soort = ei != null ? ei : OerSoort.van(fossiel());
        if (soort == null) {
            return Component.translatable("gui.guhs.fossiel.status.leeg");
        }
        Component naam = Component.translatable("entity.guhs.guh." + soort.variant.id());
        if (voortgang > 0) {
            return Component.translatable(heeftKracht() ? "gui.guhs.fossiel.status.broedt" : "gui.guhs.fossiel.status.broedt_slaapt",
                    naam, Math.round(voortgang() * 100));
        }
        if (knabbels() < FossielFeature.KNABBELS) {
            return Component.translatable("gui.guhs.fossiel.status.knabbels", naam, knabbels(), FossielFeature.KNABBELS);
        }
        return Component.translatable("gui.guhs.fossiel.status.kracht", naam, FossielFeature.KRACHT);
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putString("Ei", ei == null ? "" : ei.id);
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        ei = OerSoort.byId(in.getStringOr("Ei", ""));
        voortgang = in.getIntOr("Voortgang", 0);
    }
}
