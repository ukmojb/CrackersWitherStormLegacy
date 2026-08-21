package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 对齐上游 PhasometerItem 继承 SpyglassItem 带来的开镜减速：上游 MixinPlayer 把望远镜
 * 视为 isScoping，原版随后减半鼠标灵敏度。1.12.2 没有 isScoping，直接在镜头转向入口减速。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererTurnMixin {

    /**
     * 上游 1.20.1 开镜减速系数是 0.5；1.12.2 的鼠标灵敏度公式比上游多乘了
     * 8.0F（EntityRenderer.updateCameraAndRender 中的 f*f*f*8.0F），因此这里
     * 用 0.5F / 8.0F 补偿，使 1.12.2 开镜后的实际转向量与上游 Spyglass 一致。
     */
    private static final float PHASOMETER_TURN_SCALE = 0.0625F;

    @Redirect(method = "updateCameraAndRender(FJ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"))
    private void witherstormmod$slowPhasometerTurn(EntityPlayerSP player, float yaw,
                                                   float pitch) {
        if (PhasometerOverlay.isScoping(player)) {
            player.turn(yaw * PHASOMETER_TURN_SCALE, pitch * PHASOMETER_TURN_SCALE);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
