package nl.juiced.guhs.feature.koepel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.theehuis.TheehuisFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * De Draaimolen (guhs:draaimolen, 3 x 2 x 2): the first guh-pretpark attraction. A kaasknabbel is a ticket (right-click with
 * one): for {@link KoepelGetallen#RIT_TICKS} ticks it turns on {@link KoepelGetallen#DRAAIMOLEN} vadskracht, plays its little
 * tune, and every tame guh within {@link KoepelGetallen#MOLEN_BEREIK} blocks rides along: it gets happy
 * ({@link Band#maakBlij}) for {@link KoepelGetallen#MOLEN_BLIJ_TICKS} ticks, and a happy guh does its klusjes better and runs a
 * Guhrad harder. You ride along too: "gezellig".
 */
public class DraaimolenBlockEntity extends MachineBlockEntity implements KoepelBlock.KoepelMachine {
    /** The little tune (note block pitches, a bar per second; it loops). */
    private static final float[] DEUNTJE = {1.0f, 1.26f, 1.5f, 1.26f, 1.0f, 1.12f, 1.33f, 1.12f, 0.89f, 1.0f, 1.19f, 1.5f};

    private int rit;

    public DraaimolenBlockEntity(BlockPos pos, BlockState state) {
        super(KoepelFeature.DRAAIMOLEN_BE.get(), pos, state, KoepelGetallen.DRAAIMOLEN, 0);
    }

    public int rit() {
        return rit;
    }

    /** One ticket (also for tests): the ride (re)starts. */
    public void kaartje() {
        boolean was = rit > 0;
        rit = KoepelGetallen.RIT_TICKS;
        if (!was) {
            meld();
        }
        sync();
    }

    @Override
    protected boolean aan() {
        return rit > 0;
    }

    @Override
    protected boolean kanWerken() {
        return rit > 0;
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
        long t = sl.getGameTime();
        if (t % 10 == 0) {
            float toon = DEUNTJE[(int) ((t / 10) % DEUNTJE.length)];
            sl.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, toon);
        }
        if (t % 40 == 0) {
            AABB rond = new AABB(worldPosition).inflate(KoepelGetallen.MOLEN_BEREIK);
            for (GuhEntity guh : sl.getEntitiesOfClass(GuhEntity.class, rond, g -> g.isTame() && g.isAlive())) {
                Band.maakBlij(guh, KoepelGetallen.MOLEN_BLIJ_TICKS);
                sl.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(4))) {
                p.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, 600, 0, true, true));
            }
        }
        if (--rit <= 0) {
            rit = 0;
            meld();
            sync();
        }
    }

    @Override
    public boolean klik(ServerPlayer speler, ItemStack inHand) {
        if (MegaOvenBlockEntity.isKnabbel(inHand)) {
            inHand.shrink(1);
            kaartje();
            speler.sendOverlayMessage((heeftKracht() ? Component.translatable("gui.guhs.koepel.molen.rit")
                    : Component.translatable("gui.guhs.koepel.molen.slaapt", KoepelGetallen.DRAAIMOLEN)).withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        if (inHand.isEmpty()) {
            speler.sendOverlayMessage(Component.translatable(rit > 0 ? "gui.guhs.koepel.molen.draait" : "gui.guhs.koepel.molen.kaartje")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        return false;
    }

    /** A ring of little notes that turns round (client). */
    @Override
    protected void clientTick() {
        if (!bezig() || level == null || level.getGameTime() % 4 != 0) {
            return;
        }
        double hoek = level.getGameTime() * 0.15;
        for (int i = 0; i < 4; i++) {
            double h = hoek + i * Math.PI / 2;
            level.addParticle(ParticleTypes.NOTE, worldPosition.getX() + 0.5 + Math.cos(h) * 1.3, worldPosition.getY() + 1.4,
                    worldPosition.getZ() + 1.0 + Math.sin(h) * 1.3, (i * 6 % 24) / 24.0, 0, 0);
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.koepel.molen.uitlezing", rit / 20).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Rit", rit);
    }

    @Override
    protected void laden(ValueInput in) {
        rit = in.getIntOr("Rit", 0);
    }
}
