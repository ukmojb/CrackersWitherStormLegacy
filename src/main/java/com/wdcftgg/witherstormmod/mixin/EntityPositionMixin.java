package com.wdcftgg.witherstormmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;







@Mixin(Entity.class)
public abstract class EntityPositionMixin {
    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void witherstormmod$ensureNonNullPosition(CallbackInfoReturnable<BlockPos> callback) {
        if (callback.getReturnValue() == null) {
            Entity self = (Entity) (Object) this;
            callback.setReturnValue(new BlockPos(
                    Math.floor(self.posX), Math.floor(self.posY + 0.5D), Math.floor(self.posZ)));
        }
    }

    @Inject(method = "getEntityBoundingBox", at = @At("RETURN"), cancellable = true)
    private void witherstormmod$ensureNonNullBoundingBox(CallbackInfoReturnable<AxisAlignedBB> callback) {
        if (callback.getReturnValue() == null) {
            Entity self = (Entity) (Object) this;
            float halfWidth = self.width / 2.0F;
            callback.setReturnValue(new AxisAlignedBB(
                    self.posX - halfWidth, self.posY, self.posZ - halfWidth,
                    self.posX + halfWidth, self.posY + self.height, self.posZ + halfWidth));
        }
    }
}
