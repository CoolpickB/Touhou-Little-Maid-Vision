package com.coolpick.tlmvision.compat;
import com.coolpick.tlmvision.TlmVisionHelper;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;
@LittleMaidExtension
public class VisionMaidCompat implements ILittleMaid {
    @Override public void registerAIChatSerializer(com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister register) {
        register.register(com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType.LLM, OpenCodeGoSite.TYPE, new OpenCodeGoSite.Serializer());
    }
    @Override public void bindMaidBauble(BaubleManager manager) {
        manager.bind(TlmVisionHelper.THIRD_EYE.get(), new IMaidBauble() {
            @Override public boolean syncClient(EntityMaid maid, ItemStack stack) { return true; }
        });
    }
    public static boolean wearsEye(EntityMaid maid) {
        var inventory = maid.getMaidBauble();
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            if (inventory.getStackInSlot(slot).is(TlmVisionHelper.THIRD_EYE.get())) return true;
        return false;
    }
}
