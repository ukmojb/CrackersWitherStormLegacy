package com.wdcftgg.witherstormmod.common.entity;

import net.minecraft.entity.player.EntityPlayerMP;

/** 在客户端确认维度世界已创建后，重新发送远距风暴实体的追踪数据。 */
public interface DistantStormTrackingResync {
    void witherstormmod$resyncDistantStorms(EntityPlayerMP player);
}
