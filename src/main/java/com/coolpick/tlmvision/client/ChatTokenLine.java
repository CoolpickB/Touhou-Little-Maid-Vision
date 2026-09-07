package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.VisionConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * TLM's chat screen shows one "Token" figure, the player's lifetime spend, which is not the
 * number history compaction compares. This draws both: the last request's size against the
 * compress limit that would trigger a summary, and the lifetime total.
 */
public final class ChatTokenLine {
    private ChatTokenLine() { }

    public static boolean enabled() { return VisionConfig.chatImprovements(); }

    public static void render(GuiGraphics graphics, Font font, EditBox input, EntityMaid maid, int spent, int maxSpend) {
        int context = maid.getAiChatManager().getLastChatTokenUsage();
        int limit = AIConfig.getMaidHistoryCompressTokenLimit();
        Component text = Component.translatable("tlmvision.tokens.line",
                format(context), format(limit), format(spent), maxSpend == Integer.MAX_VALUE ? "∞" : format(maxSpend));
        int left = input.getX() - 6;
        int right = input.getX() + input.getInnerWidth() + 6;
        graphics.pose().pushPose();
        graphics.pose().scale(.5f, .5f, 1);
        int x = Math.round((left + right) / 2f / .5f) - font.width(text) / 2;
        int y = Math.round((input.getY() - 14) / .5f);
        // Amber once the next request would trip compaction, so the summary pass is not a surprise.
        graphics.drawString(font, text, x, y, context >= limit ? 0xE8C26A : 0xADADAD, false);
        graphics.pose().popPose();
    }

    private static String format(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1_000_000) return "%.1fK".formatted(count / 1000d);
        return "%.1fM".formatted(count / 1_000_000d);
    }
}
