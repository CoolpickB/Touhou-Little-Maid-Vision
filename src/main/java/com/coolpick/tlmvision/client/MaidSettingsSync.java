package com.coolpick.tlmvision.client;

import com.coolpick.tlmvision.network.MaidSettingsPayload;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatSerializable;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;

public final class MaidSettingsSync {
    private MaidSettingsSync() {}
    public static void receive(MaidSettingsPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getEntity(payload.maidId()) instanceof EntityMaid maid) {
            var data = new MaidAIChatSerializable();
            data.readFromTag(payload.settings());
            maid.getAiChatManager().copyFrom(data);
        }
    }
}
