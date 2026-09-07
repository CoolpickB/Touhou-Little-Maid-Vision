package com.coolpick.tlmvision.client;

import com.coolpick.tlmvision.TlmVisionHelper;
import com.coolpick.tlmvision.VisionConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.TreeMap;

/**
 * Vision model picker, styled after TLM's own hub (centered dark panel, flat
 * dark buttons). Lists every model on every enabled OpenAI-compatible LLM
 * site TLM knows about (same pool as "select llm site or model"), plus a None
 * row that falls back to the maid's current chat model.
 */
public class VisionScreen extends com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen {
    private VisionList list;
    private FlatColorButton observation;

    public VisionScreen(Screen parent, SharedState state, boolean insufficientPermissions) {
        super(parent, state, insufficientPermissions);
    }
    @Override protected Type getType() { return null; }
    @Override public void reopenSelf(Map<String, LLMSite> llm, Map<String, com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite> tts) {
        state.llmSites.clear(); state.llmSites.putAll(llm);
        state.ttsSites.clear(); state.ttsSites.putAll(tts);
        minecraft.setScreen(new VisionScreen(parent, state, insufficientPermissions));
    }
    @Override protected void initContent() {
        this.list = new VisionList(this.minecraft, getContentWidth(), 144, getContentY() + 50, 22, VisionConfig.load());
        this.list.setX(getContentX());
        this.addRenderableWidget(this.list);
        this.observation = this.addRenderableWidget(new FlatColorButton(getContentX() + 6, getContentY() + 33,
                getContentWidth() - 12, 14, observationLabel(VisionConfig.load()), button -> {
            VisionConfig config = VisionConfig.load();
            config.showObservation = !config.showObservation;
            config.save();
            button.setMessage(observationLabel(config));
        }));
    }
    private static Component observationLabel(VisionConfig config) {
        return Component.translatable("tlmvision.screen.vision.show_observation",
                CommonComponents.optionStatus(config.showObservation));
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, Component.translatable("tlmvision.screen.vision.title"), getContentX() + 6, getContentY() + 5, 0xFFFFFF, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(currentSelectionLine().getString(), getContentWidth() - 12), getContentX() + 6, getContentY() + 22, 0xAAAAAA, false);
    }

    private Component currentSelectionLine() {
        VisionConfig config = VisionConfig.load();
        if (!config.hasPinned()) {
            return Component.translatable("tlmvision.screen.vision.none_selected");
        }
        return Component.translatable("tlmvision.screen.vision.pinned", config.pinnedSite, config.pinnedModel);
    }

    private void reselect() {
        VisionConfig config = VisionConfig.load();
        this.list.replaceEntries(config);
    }

    /** One row per model, grouped by site, with a leading None row. */
    private class VisionList extends ObjectSelectionList<VisionList.VisionEntry> {
        VisionList(Minecraft minecraft, int width, int height, int y, int itemHeight, VisionConfig config) {
            super(minecraft, width, height, y, itemHeight);
            replaceEntries(config);
        }

        void replaceEntries(VisionConfig config) {
            this.clearEntries();
            String selected = config.hasPinned() ? config.pinnedSite + "\0" + config.pinnedModel : null;
            this.addEntry(new VisionEntry(null, null,
                    Component.translatable("tlmvision.screen.vision.none").withStyle(ChatFormatting.YELLOW),
                    selected == null));
            Map<String, LLMSite> sites = new TreeMap<>(AvailableSites.LLM_SITES);
            for (Map.Entry<String, LLMSite> siteEntry : sites.entrySet()) {
                LLMSite site = siteEntry.getValue();
                if (site == null || !site.enabled() || !(site instanceof LLMOpenAISite openAi)) {
                    continue;
                }
                Map<String, String> models = new TreeMap<>(openAi.models());
                for (Map.Entry<String, String> model : models.entrySet()) {
                    boolean isSelected = (siteEntry.getKey() + "\0" + model.getKey()).equals(selected);
                    Component label = Component.literal(siteEntry.getKey() + " - " + model.getValue());
                    this.addEntry(new VisionEntry(siteEntry.getKey(), model.getKey(), label, isSelected));
                }
            }
            if (this.children().isEmpty()) {
                TlmVisionHelper.LOGGER.warn("[tlmvision] No enabled OpenAI-compatible LLM sites found");
            }
        }

        @Override
        public int getRowWidth() {
            return getContentWidth() - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        class VisionEntry extends ObjectSelectionList.Entry<VisionEntry> {
            private final String siteId;
            private final String modelId;
            private final Component label;
            private boolean selected;

            VisionEntry(String siteId, String modelId, Component label, boolean selected) {
                this.siteId = siteId;
                this.modelId = modelId;
                this.label = label;
                this.selected = selected;
            }

            @Override
            public Component getNarration() {
                return this.label;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                VisionConfig config = VisionConfig.load();
                config.pinnedSite = this.siteId == null ? "" : this.siteId;
                config.pinnedModel = this.modelId == null ? "" : this.modelId;
                config.save();
                VisionScreen.this.reselect();
                return true;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                if (this.selected) {
                    graphics.fill(left - 2, top - 1, left + width + 2, top + height, 0xFF434343);
                } else if (hovering) {
                    graphics.fill(left - 2, top - 1, left + width + 2, top + height, 0x40222222);
                }
                graphics.drawString(VisionScreen.this.font, this.label, left + 6, top + (height - 8) / 2,
                        this.selected ? 0xFFFF55 : 0xDDDDDD, false);
            }
        }
    }
}
