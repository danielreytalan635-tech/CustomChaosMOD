package me.yourname.customchaos;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side-only Chaos Control menu.
 *
 * This is deliberately a vanilla 9x3 chest menu. The server sends the normal
 * vanilla menu packet, so an unmodded Minecraft client can render it.
 */
public final class ChaosMenu extends ChestMenu {
    private static final int ROWS = 3;
    private static final int BUTTON_SLOTS = 27;
    private final SimpleContainer buttonInventory;
    private final Map<Integer, String> actions = new HashMap<>();

    private ChaosMenu(int syncId, Inventory playerInventory, SimpleContainer buttons) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, buttons, ROWS);
        this.buttonInventory = buttons;
        installButtonSlots();
    }

    private void installButtonSlots() {
        // The parent constructor already added the normal container slots.
        // We replace the first 27 slots with non-interactive button slots.
        for (int i = 0; i < BUTTON_SLOTS; i++) {
            final int slotIndex = i;
            this.slots.set(slotIndex, new Slot(buttonInventory, slotIndex, 0, 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInventory, ignoredPlayer) ->
                createMenu(syncId, playerInventory),
            Component.literal("CustomChaos Control")
        ));
    }

    private static ChaosMenu createMenu(int syncId, Inventory playerInventory) {
        SimpleContainer buttons = new SimpleContainer(BUTTON_SLOTS);
        ChaosMenu menu = new ChaosMenu(syncId, playerInventory, buttons);
        menu.populate();
        return menu;
    }

    private void populate() {
        buttonInventory.clearContent();

        button(10, Items.FIREWORK_STAR, "Confetti", "confetti");
        button(11, Items.SPYGLASS, "Night Vision", "nightvision");
        button(12, Items.FEATHER, "Yeet Yourself", "yeet");
        button(13, Items.RABBIT_FOOT, "Super Jump", "superjump");
        button(14, Items.SUGAR, "Speed", "speed");
        button(15, Items.GLOWSTONE_DUST, "Glow", "glow");
        button(16, Items.HEART_OF_THE_SEA, "Hearts", "hearts");

        button(19, Items.FLINT_AND_STEEL, "Flame Burst", "flame");
        button(20, Items.ENDER_PEARL, "Portal Burst", "portal");
        button(21, Items.SNOWBALL, "Snowstorm", "snowstorm");
        button(22, Items.AMETHYST_SHARD, "Magic Burst", "magic");
        button(23, Items.SLIME_BALL, "Bounce", "bounce");
        button(24, Items.GOLD_INGOT, "Coin Flip", "coinflip");
        button(25, Items.REDSTONE, "Earthquake", "earthquake");

        button(26, Items.BARRIER, "Close", "__close__");

        // Decorative separators / headers.
        decorative(0, Items.NETHER_STAR, "Chaos Control");
        // Keep the remaining slots visually simple so every item exists on 26.2.
        decorative(1, Items.GLASS_PANE, "Player Effects");
        decorative(8, Items.GLASS_PANE, "Player Effects");
        decorative(17, Items.GLASS_PANE, "World / Visual");
    }

    private void button(int slot, net.minecraft.world.item.Item item, String label, String command) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        buttonInventory.setItem(slot, stack);
        actions.put(slot, command);
    }

    private void decorative(int slot, net.minecraft.world.item.Item item, String label) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        buttonInventory.setItem(slot, stack);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < BUTTON_SLOTS) {
            if (player instanceof ServerPlayer serverPlayer) {
                String command = actions.get(slotId);
                if ("__close__".equals(command)) {
                    serverPlayer.closeContainer();
                } else if (command != null) {
                    ChaosCommands.executeMenuCommand(serverPlayer, command);
                }
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        // Prevent moving button items into/out of the menu.
        if (slot >= 0 && slot < BUTTON_SLOTS) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != buttonInventory && super.canDragTo(slot);
    }
}
