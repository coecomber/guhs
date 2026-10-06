package nl.juiced.guhs.feature.techbuis;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.Mikas;

/**
 * De Snuffelsensor ({@code guhs:snuffelsensor}): a nose that smells who is near. It gives a redstone signal while at least
 * one of what it sniffs for (guhs, Mika's, players, or all of them) is within its reach; a comparator reads how many (up
 * to 15). Use it to pick what it sniffs for; sneak + use changes how far it smells (2, 4 or 8 blocks).
 */
public class SnuffelsensorBlockEntity extends SensorBlockEntity {
    /** What the nose sniffs for. */
    public enum Wat {
        GUHS, MIKAS, SPELERS, ALLES;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        boolean ruikt(Entity e) {
            return switch (this) {
                case GUHS -> isGuh(e);
                case MIKAS -> isMika(e);
                case SPELERS -> isSpeler(e);
                case ALLES -> isGuh(e) || isMika(e) || isSpeler(e);
            };
        }
    }

    /** Sniffed every this many ticks. */
    public static final int MEET = 10;
    private static final int[] STRALEN = {2, 4, 8};

    private Wat wat = Wat.GUHS;
    private int straal = 4;
    private int geroken;

    public SnuffelsensorBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.SNUFFELSENSOR_BE.get(), pos, state);
    }

    static boolean isGuh(Entity e) {
        return e instanceof GuhEntity;
    }

    /** A Mika: every Mika you can fight (the tag guhs:mikas), and everything else of ours that is called a Mika. */
    static boolean isMika(Entity e) {
        return Mikas.isMika(e) || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().contains("mika");
    }

    static boolean isSpeler(Entity e) {
        return e instanceof Player speler && !speler.isSpectator();
    }

    public Wat wat() {
        return wat;
    }

    public int straal() {
        return straal;
    }

    /** How many it smelled at its last sniff. */
    public int geroken() {
        return geroken;
    }

    public void zetWat(Wat nieuw) {
        wat = nieuw;
        setChanged();
    }

    public void zetStraal(int blokken) {
        straal = Math.max(1, Math.min(8, blokken));
        setChanged();
    }

    @Override
    protected void meet(ServerLevel level, long nu) {
        if (Math.floorMod(nu + worldPosition.hashCode(), MEET) != 0) {
            return;
        }
        geroken = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(straal), e -> e.isAlive() && wat.ruikt(e)).size();
        zet(geroken > 0, geroken);
    }

    @Override
    public void klik(ServerPlayer player, boolean sluipt) {
        if (sluipt) {
            int i = 0;
            while (i < STRALEN.length && STRALEN[i] != straal) {
                i++;
            }
            zetStraal(STRALEN[(i + 1) % STRALEN.length]);
        } else {
            zetWat(Wat.values()[(wat.ordinal() + 1) % Wat.values().length]);
        }
        tik();
        player.sendOverlayMessage(instelling());
    }

    private Component instelling() {
        return Component.translatable("gui.guhs.techbuis.snuffel.zoekt", Component.translatable("gui.guhs.techbuis.snuffel.wat." + wat.id()), straal);
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(instelling().copy().withStyle(ChatFormatting.GRAY));
        if (heeftKracht()) {
            regels.accept(Component.translatable("gui.guhs.techbuis.snuffel.ruikt", geroken).withStyle(geroken > 0 ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putString("Wat", wat.id());
        uit.putInt("Straal", straal);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        String naam = in.getStringOr("Wat", Wat.GUHS.id());
        wat = Wat.GUHS;
        for (Wat w : Wat.values()) {
            if (w.id().equals(naam)) {
                wat = w;
            }
        }
        straal = Math.max(1, Math.min(8, in.getIntOr("Straal", 4)));
    }
}
