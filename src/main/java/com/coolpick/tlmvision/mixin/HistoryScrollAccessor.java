package com.coolpick.tlmvision.mixin;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.HistoryAIChatScreen;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** TLM keeps the history scroll state private; the drag bar in HistoryScroll reads and moves it. */
@Pseudo
@Mixin(value = HistoryAIChatScreen.class, remap = false)
public interface HistoryScrollAccessor {
    @Accessor("scroll") double tlmvision$scroll();
    @Accessor("scroll") void tlmvision$setScroll(double scroll);
    @Accessor("maxHeight") int tlmvision$maxHeight();
    @Accessor("historyTop") int tlmvision$historyTop();
    @Accessor("historyBottom") int tlmvision$historyBottom();
    @Accessor("posX") int tlmvision$posX();
    @Accessor("maid") EntityMaid tlmvision$maid();
}
