package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.PhasometerOverlay;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;





@Mixin(EntityRenderer.class)
public abstract class EntityRendererTurnMixin {






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
