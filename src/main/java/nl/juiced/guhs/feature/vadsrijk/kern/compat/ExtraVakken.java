package nl.juiced.guhs.feature.vadsrijk.kern.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Soft compat for accessory slots (Curios, Accessories) in the inventory swap ({@code kern.Inventaris}): no hard dependency,
 * everything through reflection on their public API, checked once at startup.
 * <ul>
 *   <li>Curios: {@code CuriosApi.getCuriosInventory(entity)} -> {@code Optional<ICuriosItemHandler>} ->
 *   {@code getEquippedCurios()} -> getSlots / getStackInSlot / setStackInSlot.</li>
 *   <li>Accessories: {@code AccessoriesCapability.get(entity)} -> {@code getContainers()} (a map) -> every container's
 *   {@code getAccessories()} and {@code getCosmeticAccessories()} (vanilla {@link Container}s).</li>
 * </ul>
 * If a mod is present but its API doesn't look as expected, its slots are NOT swapped (that gear stays equipped in both worlds;
 * our damage pipeline ignores vanilla attributes, but passive effects of such gear still work) and the server log says so.
 * Tests can add their own provider to {@link #ALLE}.
 */
public final class ExtraVakken {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** One accessory slot: read and write. */
    public interface Vak {
        ItemStack get();

        void set(ItemStack s);
    }

    /** A source of extra slots (a mod). {@code vakken} must list the slots in a stable order. */
    public interface Bron {
        String id();

        List<Vak> vakken(ServerPlayer p);
    }

    public static final List<Bron> ALLE = new CopyOnWriteArrayList<>();
    /** Mods that are present but whose slots could not be reached (for the log / the notes). */
    public static final List<String> NIET_GELUKT = new CopyOnWriteArrayList<>();

    private ExtraVakken() {
    }

    @Nullable
    public static Bron van(String id) {
        for (Bron b : ALLE) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    public static void init() {
        if (ModList.get().isLoaded("curios")) {
            Bron b = curios();
            if (b != null) {
                ALLE.add(b);
                LOGGER.info("Vadsrijk: Curios slots are part of the inventory swap");
            } else {
                NIET_GELUKT.add("curios");
                LOGGER.warn("Vadsrijk: Curios is present but its API was not as expected: its slots are NOT swapped");
            }
        }
        if (ModList.get().isLoaded("accessories")) {
            Bron b = accessories();
            if (b != null) {
                ALLE.add(b);
                LOGGER.info("Vadsrijk: Accessories slots are part of the inventory swap");
            } else {
                NIET_GELUKT.add("accessories");
                LOGGER.warn("Vadsrijk: Accessories is present but its API was not as expected: its slots are NOT swapped");
            }
        }
    }

    @Nullable
    private static Bron curios() {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method inv = api.getMethod("getCuriosInventory", LivingEntity.class);
            return new Bron() {
                @Override
                public String id() {
                    return "curios";
                }

                @Override
                public List<Vak> vakken(ServerPlayer p) {
                    List<Vak> uit = new ArrayList<>();
                    try {
                        Object o = inv.invoke(null, p);
                        Object handler = o instanceof Optional<?> opt ? opt.orElse(null) : o;
                        if (handler == null) {
                            return uit;
                        }
                        Object items = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
                        Method slots = items.getClass().getMethod("getSlots");
                        Method get = items.getClass().getMethod("getStackInSlot", int.class);
                        Method set = items.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
                        slots.setAccessible(true);
                        get.setAccessible(true);
                        set.setAccessible(true);
                        int n = (int) slots.invoke(items);
                        for (int i = 0; i < n; i++) {
                            int slot = i;
                            uit.add(new Vak() {
                                @Override
                                public ItemStack get() {
                                    try {
                                        return (ItemStack) get.invoke(items, slot);
                                    } catch (ReflectiveOperationException e) {
                                        return ItemStack.EMPTY;
                                    }
                                }

                                @Override
                                public void set(ItemStack s) {
                                    try {
                                        set.invoke(items, slot, s);
                                    } catch (ReflectiveOperationException e) {
                                        throw new IllegalStateException(e);
                                    }
                                }
                            });
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        LOGGER.warn("Vadsrijk: reading the Curios slots failed", e);
                        uit.clear();
                    }
                    return uit;
                }
            };
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    @Nullable
    private static Bron accessories() {
        try {
            Class<?> cap = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Method get = cap.getMethod("get", LivingEntity.class);
            return new Bron() {
                @Override
                public String id() {
                    return "accessories";
                }

                @Override
                public List<Vak> vakken(ServerPlayer p) {
                    List<Vak> uit = new ArrayList<>();
                    try {
                        Object c = get.invoke(null, p);
                        if (c instanceof Optional<?> opt) {
                            c = opt.orElse(null);
                        }
                        if (c == null) {
                            return uit;
                        }
                        Object map = c.getClass().getMethod("getContainers").invoke(c);
                        if (!(map instanceof Map<?, ?> m)) {
                            return uit;
                        }
                        List<Object> sleutels = new ArrayList<>(m.keySet());
                        sleutels.sort((a, b) -> String.valueOf(a).compareTo(String.valueOf(b)));
                        for (Object k : sleutels) {
                            Object container = m.get(k);
                            for (String naam : new String[] {"getAccessories", "getCosmeticAccessories"}) {
                                Object inh = container.getClass().getMethod(naam).invoke(container);
                                if (inh instanceof Container con) {
                                    for (int i = 0; i < con.getContainerSize(); i++) {
                                        int slot = i;
                                        uit.add(new Vak() {
                                            @Override
                                            public ItemStack get() {
                                                return con.getItem(slot);
                                            }

                                            @Override
                                            public void set(ItemStack s) {
                                                con.setItem(slot, s);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        LOGGER.warn("Vadsrijk: reading the Accessories slots failed", e);
                        uit.clear();
                    }
                    return uit;
                }
            };
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
