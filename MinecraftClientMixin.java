package dev.wellfeed.mixin;

import dev.wellfeed.modules.EspModule;
import dev.wellfeed.modules.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla's glow-outline pass draw other players through walls (ESP).
 * Reuses the renderer's existing outline path, so it costs nothing extra.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void wellfeed$espGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!EspModule.INSTANCE.glowActive() || !(entity instanceof PlayerEntity)) return;
        boolean self = entity == ((MinecraftClient) (Object) this).player;
        if (!self || FreecamModule.INSTANCE.active()) {
            cir.setReturnValue(true);
        }
    }
}
