package dev.wellfeed.mixin;

import dev.wellfeed.modules.FullbrightModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: while the module is on, the gamma option reads as 16.0.
 * The saved option value is never touched, so turning it off restores your setting.
 */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void wellfeed$fullbright(CallbackInfoReturnable<T> cir) {
        if (!FullbrightModule.active()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if ((Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) Double.valueOf(16.0));
        }
    }
}
