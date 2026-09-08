package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.VisionConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Mods list config: client-wide settings, unlike the per-maid menus TLM's hub holds. */
public final class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("tlmvision.config.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        int x = this.width / 2 - 155, y = this.height / 4 + 24;
        addRenderableWidget(Button.builder(observationLabel(), button -> {
            VisionConfig saved = VisionConfig.load();
            saved.showObservation = !saved.showObservation;
            saved.save();
            button.setMessage(observationLabel());
        }).bounds(x, y, 310, 20).build());
        VisionConfig config = VisionConfig.load();
        addRenderableWidget(new DiscreteSlider(x, y + 26, 310, config.maxMaids,
                new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, value -> Component.translatable("tlmvision.config.max_maids", value), value -> {
                    VisionConfig saved = VisionConfig.load(); saved.maxMaids = value; saved.save();
                }));
        addRenderableWidget(new DiscreteSlider(x, y + 52, 310, (int) Math.round(config.groupRange),
                new int[] {0, 4, 8, 16, 32, 48, 64, 96, 128, 160, 192, 224, 256}, ModConfigScreen::groupRangeLabel, value -> {
                    VisionConfig saved = VisionConfig.load(); saved.groupRange = value; saved.save();
                }));
        addRenderableWidget(new DiscreteSlider(x, y + 78, 310, config.maxTokens,
                new int[] {512, 1024, 2048, 4096}, value -> Component.translatable("tlmvision.config.max_tokens", value), value -> {
                    VisionConfig saved = VisionConfig.load(); saved.maxTokens = value; saved.save();
        }));
        addRenderableWidget(Button.builder(fixesLabel(), button -> {
            VisionConfig saved = VisionConfig.load();
            saved.chatImprovements = !saved.chatImprovements;
            saved.save();
            VisionConfig.refreshChatImprovements();
            button.setMessage(fixesLabel());
        }).bounds(x, y + 104, 310, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private static Component fixesLabel() {
        return Component.translatable("tlmvision.config.chat_improvements", CommonComponents.optionStatus(VisionConfig.load().chatImprovements));
    }

    private static Component observationLabel() {
        return Component.translatable("tlmvision.config.show_observation", CommonComponents.optionStatus(VisionConfig.load().showObservation));
    }

    private static Component groupRangeLabel(int range) {
        return range == 0 ? Component.translatable("tlmvision.config.group_range.nearest")
                : Component.translatable("tlmvision.config.group_range", range);
    }

    private static final class DiscreteSlider extends AbstractSliderButton {
        private final int[] values;
        private final IntFunction<Component> label;
        private final IntConsumer save;
        private int savedValue;

        DiscreteSlider(int x, int y, int width, int initial, int[] values, IntFunction<Component> label, IntConsumer save) {
            super(x, y, width, 20, Component.empty(), indexOf(values, initial) / (double) (values.length - 1));
            this.values = values;
            this.label = label;
            this.save = save;
            this.savedValue = selected();
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(label.apply(selected()));
        }

        @Override protected void applyValue() {
            int selected = selected();
            value = indexOf(values, selected) / (double) (values.length - 1);
            updateMessage();
            if (selected != savedValue) {
                savedValue = selected;
                save.accept(selected);
            }
        }

        private int selected() {
            return values[Math.clamp((int) Math.round(value * (values.length - 1)), 0, values.length - 1)];
        }

        private static int indexOf(int[] values, int wanted) {
            int closest = 0;
            for (int index = 1; index < values.length; index++) {
                if (Math.abs(values[index] - wanted) < Math.abs(values[closest] - wanted)) closest = index;
            }
            return closest;
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 + 4, 0xFFFFFF);
    }

    @Override public void onClose() { this.minecraft.setScreen(this.parent); }
}
