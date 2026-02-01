package com.example.combinedpacks.mixin;

import com.example.combinedpacks.gui.PriorityConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackScreen.class)
public class PackScreenMixin extends Screen {
    protected PackScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Priorities"), (button) -> {
            this.client.setScreen(PriorityConfigScreen.createScreen(this));
        }).dimensions(this.width - 120, 20, 100, 20).build());
    }
}
