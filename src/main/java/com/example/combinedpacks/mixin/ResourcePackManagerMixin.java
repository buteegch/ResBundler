package com.example.combinedpacks.mixin;

import com.example.combinedpacks.CombinedResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.path.SymlinkFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
    @Shadow @Mutable
    private Set<ResourcePackProvider> providers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addCustomProvider(ResourcePackProvider[] providers, CallbackInfo ci) {
        Set<ResourcePackProvider> newProviders = new HashSet<>(this.providers);
        newProviders.add(new CombinedResourcePackProvider(
            Path.of("resourcepacks").toAbsolutePath(), 
            ResourcePackSource.create(name -> Text.literal("Combined Pack"), true),
            new SymlinkFinder(path -> true)
        ));
        this.providers = newProviders;
    }
}
