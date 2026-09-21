package me.yourname.customchaos;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChaosScreen extends Screen {
    private final Screen parent;
    private static final int BUTTON_WIDTH = 170;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 24;

    public ChaosScreen(Screen parent) {
        super(Component.literal("CustomChaos Control"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.width / 2 - BUTTON_WIDTH - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int top = this.height / 2 - 100;

        addCommandButton("Confetti", "chaos confetti", left, top);
        addCommandButton("Hearts", "chaos hearts", left, top + 28);
        addCommandButton("Fire Burst", "chaos flame", left, top + 56);
        addCommandButton("Portal Burst", "chaos portal", left, top + 84);

        addCommandButton("Super Jump", "chaos superjump", right, top);
        addCommandButton("Speed", "chaos speed", right, top + 28);
        addCommandButton("Night Vision", "chaos nightvision", right, top + 56);
        addCommandButton("Coin Flip", "chaos coinflip", right, top + 84);

        addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(this.width / 2 - 85, top + 124, 170, BUTTON_HEIGHT)
                .build()
        );
    }

    private void addCommandButton(String label, String command, int x, int y) {
        addRenderableWidget(
            Button.builder(Component.literal(label), button -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(command);
                }
            }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build()
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
