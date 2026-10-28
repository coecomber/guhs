package nl.juiced.guhs.feature.koepel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * Het Knabbel-weerstation (guhs:knabbel_weerstation, a tower of 1 x 2 x 1): while it has vadskracht
 * ({@link KoepelGetallen#WEERSTATION}) and knabbels to eat (one per {@link KoepelGetallen#WEERSTATION_KNABBEL_TICKS} ticks; a
 * knabbelblok counts as 9), every Weer vadser within {@link KoepelGetallen#WEERSTATION_BEREIK} blocks makes its weather over
 * {@link KoepelGetallen#WEERSTATION_STRAAL} blocks: your whole base. It works through the Weer vadser's radius hook
 * ({@code WeerZones.STRAAL_VOOR}, wrapped in {@link KoepelFeature}); it takes the biggest radius, so the Wolkenantennes of the
 * Weer vadser itself still count when they are bigger.
 */
public class WeerstationBlockEntity extends MachineBlockEntity implements KoepelBlock.KoepelMachine {
    /** Running stations per dimension: position -> until (game time). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> ACTIEF = new HashMap<>();

    private int voorraad;
    private int teller;

    public WeerstationBlockEntity(BlockPos pos, BlockState state) {
        super(KoepelFeature.WEERSTATION_BE.get(), pos, state, KoepelGetallen.WEERSTATION, 1);
    }

    /** The radius a Weer vadser at pos gets from a running station near it (0: none). */
    public static synchronized int straalVoor(ServerLevel level, BlockPos vadser) {
        Map<BlockPos, Long> m = ACTIEF.get(level.dimension());
        if (m == null) {
            return 0;
        }
        long nu = level.getGameTime();
        int r = KoepelGetallen.WEERSTATION_BEREIK;
        for (Map.Entry<BlockPos, Long> e : m.entrySet()) {
            if (e.getValue() >= nu && e.getKey().distSqr(vadser) <= (double) r * r) {
                return KoepelGetallen.WEERSTATION_STRAAL;
            }
        }
        return 0;
    }

    private static synchronized void markeer(ServerLevel level, BlockPos pos) {
        ACTIEF.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), level.getGameTime() + 40);
    }

    static synchronized void wis(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> m = ACTIEF.get(level.dimension());
        if (m != null) {
            m.remove(pos);
        }
    }

    public int voorraad() {
        return voorraad;
    }

    private void bijvullen() {
        if (voorraad > 0) {
            return;
        }
        ItemResource r = vakken().getResource(0);
        int n = vakken().getAmountAsInt(0);
        if (r.isEmpty() || n == 0) {
            return;
        }
        ItemStack s = r.toStack(1);
        int waarde = MegaOvenBlockEntity.isKnabbelblok(s) ? 9 : MegaOvenBlockEntity.isKnabbel(s) ? 1 : 0;
        if (waarde > 0) {
            vakken().set(0, n - 1 == 0 ? ItemResource.EMPTY : r, n - 1);
            voorraad += waarde;
            sync();
        }
    }

    @Override
    protected boolean kanWerken() {
        bijvullen();
        return voorraad > 0;
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
        markeer(sl, worldPosition);
        if (++teller >= KoepelGetallen.WEERSTATION_KNABBEL_TICKS) {
            teller = 0;
            voorraad--;
            sync();
        }
        if (sl.getGameTime() % 40 == 0) {
            sl.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 2.2, worldPosition.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        ItemStack s = wat.toStack(1);
        return MegaOvenBlockEntity.isKnabbel(s) || MegaOvenBlockEntity.isKnabbelblok(s);
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return true;
    }

    @Override
    public boolean klik(ServerPlayer speler, ItemStack inHand) {
        if (!inHand.isEmpty() && past(0, ItemResource.of(inHand))) {
            ItemStack kopie = inHand.copy();
            int rest = KoepelVakken.stop(vakken(), kopie, 0, 1);
            inHand.shrink(inHand.getCount() - rest);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.koepel.station.gevoerd", voorraad + KoepelVakken.tel(vakken(),
                    vakken().getResource(0).isEmpty() ? net.minecraft.world.item.Items.AIR : vakken().getResource(0).getItem(), 0, 1))
                    .withStyle(ChatFormatting.AQUA));
            return true;
        }
        if (inHand.isEmpty()) {
            speler.sendOverlayMessage((heeftKracht() && voorraad > 0
                    ? Component.translatable("gui.guhs.koepel.station.aan", KoepelGetallen.WEERSTATION_STRAAL, KoepelGetallen.WEERSTATION_BEREIK)
                    : Component.translatable("gui.guhs.koepel.station.uit", KoepelGetallen.WEERSTATION)).withStyle(ChatFormatting.AQUA));
            return true;
        }
        return false;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.koepel.station.uitlezing", voorraad).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Voorraad", voorraad);
        uit.putInt("Teller", teller);
    }

    @Override
    protected void laden(ValueInput in) {
        voorraad = in.getIntOr("Voorraad", 0);
        teller = in.getIntOr("Teller", 0);
    }
}
