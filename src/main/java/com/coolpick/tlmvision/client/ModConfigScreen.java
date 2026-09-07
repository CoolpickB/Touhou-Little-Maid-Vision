package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.VisionConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Mods list config: client-wide settings, unlike the per-maid menus TLM's hub holds. */
public final class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("tlmvision.config.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        int x = this.width / 2 - 155, y = this.height / 4 + 24;
        addRenderableWidget(Button.builder(fixesLabel(), button -> {
            VisionConfig config = VisionConfig.load();
            config.chatImprovements = !config.chatImprovements;
            config.save();
            VisionConfig.refreshChatImprovements();
            button.setMessage(fixesLabel());
        }).bounds(x, y, 310, 20).build());
        addRenderableWidget(new MultiLineTextWidget(x, y + 26,
                Component.translatable("tlmvision.config.chat_improvements.detail"), this.font).setMaxWidth(310));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private static Component fixesLabel() {
        return Component.translatable("tlmvision.config.chat_improvements", CommonComponents.optionStatus(VisionConfig.load().chatImprovements));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 + 4, 0xFFFFFF);
    }

    @Override public void onClose() { this.minecraft.setScreen(this.parent); }
}
