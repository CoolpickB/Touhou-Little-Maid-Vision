package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.TlmVisionHelper;
import com.coolpick.tlmvision.VisionConfig;
import com.coolpick.tlmvision.mixin.HistoryScrollAccessor;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.HistoryAIChatScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Drag bar for TLM's chat history, which otherwise moves only by wheel, always opens at the
 * oldest message and forgets where you were. Driven by screen events rather than injections so
 * it needs no method hooks in TLM's screen; only the private scroll state is shadowed.
 */
@EventBusSubscriber(modid = TlmVisionHelper.MOD_ID, value = Dist.CLIENT)
public final class HistoryScroll {
    private static final int BAR_WIDTH = 4;
    private static final int MIN_THUMB = 16;
    /** Reopening the same maid returns to where you were reading; a different maid starts at the newest. */
    private static int rememberedMaid = -1;
    private static double rememberedScroll;
    private static boolean dragging;

    private HistoryScroll() { }

    private static HistoryAIChatScreen target(Screen screen) {
        if (!(screen instanceof HistoryAIChatScreen history) || !VisionConfig.chatImprovements()) return null;
        return history;
    }
    /** Content taller than the window: how far the view can travel, as a positive span. */
    private static double travel(HistoryScrollAccessor state) {
        return Math.max(0, state.tlmvision$maxHeight() - state.tlmvision$historyBottom());
    }
    private static void clamp(HistoryScrollAccessor state) {
        double travel = travel(state);
        state.tlmvision$setScroll(Math.clamp(state.tlmvision$scroll(), -travel, 0));
    }

    @SubscribeEvent public static void opened(ScreenEvent.Init.Post event) {
        VisionConfig.refreshChatImprovements();
        HistoryAIChatScreen history = target(event.getScreen());
        if (history == null) return;
        HistoryScrollAccessor state = (HistoryScrollAccessor) history;
        if (travel(state) <= 0) return;
        int maid = state.tlmvision$maid().getId();
        state.tlmvision$setScroll(maid == rememberedMaid ? rememberedScroll : -travel(state));
        rememberedMaid = maid;
        clamp(state);
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        HistoryAIChatScreen history = target(event.getScreen());
        if (history == null) return;
        HistoryScrollAccessor state = (HistoryScrollAccessor) history;
        rememberedScroll = state.tlmvision$scroll();
        double travel = travel(state);
        if (travel <= 0) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int top = state.tlmvision$historyTop(), bottom = state.tlmvision$historyBottom();
        int x = barX(state);
        graphics.fill(x, top, x + BAR_WIDTH, bottom, 0x66000000);
        int thumb = thumbHeight(state);
        int y = top + (int) Math.round(-state.tlmvision$scroll() / travel * (bottom - top - thumb));
        graphics.fill(x, y, x + BAR_WIDTH, y + thumb, dragging ? 0xFFC9A0A0 : 0xFF8A6A6A);
    }

    @SubscribeEvent public static void press(ScreenEvent.MouseButtonPressed.Pre event) {
        HistoryAIChatScreen history = target(event.getScreen());
        if (history == null || event.getButton() != 0) return;
        HistoryScrollAccessor state = (HistoryScrollAccessor) history;
        if (travel(state) <= 0) return;
        int x = barX(state);
        if (event.getMouseX() < x || event.getMouseX() > x + BAR_WIDTH) return;
        if (event.getMouseY() < state.tlmvision$historyTop() || event.getMouseY() > state.tlmvision$historyBottom()) return;
        dragging = true;
        moveTo(state, event.getMouseY());
        event.setCanceled(true);
    }

    @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Pre event) {
        HistoryAIChatScreen history = target(event.getScreen());
        if (history == null || !dragging) return;
        moveTo((HistoryScrollAccessor) history, event.getMouseY());
        event.setCanceled(true);
    }

    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (dragging && target(event.getScreen()) != null) dragging = false;
    }

    /** Put the middle of the thumb under the cursor, then let the clamp handle the ends. */
    private static void moveTo(HistoryScrollAccessor state, double mouseY) {
        int top = state.tlmvision$historyTop(), bottom = state.tlmvision$historyBottom();
        int thumb = thumbHeight(state);
        int span = bottom - top - thumb;
        if (span <= 0) return;
        double fraction = (mouseY - top - thumb / 2.0) / span;
        state.tlmvision$setScroll(-Math.clamp(fraction, 0, 1) * travel(state));
        clamp(state);
    }
    private static int thumbHeight(HistoryScrollAccessor state) {
        int window = state.tlmvision$historyBottom() - state.tlmvision$historyTop();
        int content = state.tlmvision$maxHeight() - state.tlmvision$historyTop();
        return Math.max(MIN_THUMB, (int) ((long) window * window / Math.max(1, content)));
    }
    /** TLM scissors the message column to posX +- 128; the bar sits on its inner right edge. */
    private static int barX(HistoryScrollAccessor state) {
        return state.tlmvision$posX() + 128 - BAR_WIDTH;
    }
}
