package com.example.combinedpacks.gui;

import com.example.combinedpacks.PackPriorityConfig;
import com.example.combinedpacks.mixin.ScreenAccessor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PriorityConfigScreen {
    
    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Combined Packs Priorities"));
        
        builder.setSavingRunnable(() -> {
            PackPriorityConfig.getInstance().save();
        });

        builder.setAfterInitConsumer(screen -> {
            ((ScreenAccessor) screen).invokeAddDrawableChild(ButtonWidget.builder(Text.literal("Reload Resources"), (btn) -> {
                PackPriorityConfig.getInstance().save(); // Save first
                MinecraftClient.getInstance().reloadResources();
                MinecraftClient.getInstance().setScreen(createScreen(parent)); // Re-open to update sorting
            }).dimensions(screen.width - 120, 10, 110, 20).build());
        });
        
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Priorities"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        Path packsDir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        
        if (Files.exists(packsDir)) {
            try (Stream<Path> stream = Files.list(packsDir)) {
                stream.filter(Files::isDirectory).sorted().forEach(subFolder -> {
                    // Skip if it is a normal pack (has pack.mcmeta directly)
                    if (Files.exists(subFolder.resolve("pack.mcmeta"))) return;
                    
                    String folderName = subFolder.getFileName().toString();
                    
                    try (Stream<Path> inner = Files.list(subFolder)) {
                        List<String> innerPacks = inner
                            .filter(p -> {
                                if (p.getFileName().toString().startsWith(".")) return false;
                                if (Files.isDirectory(p)) {
                                    return Files.exists(p.resolve("pack.mcmeta"));
                                }
                                return p.toString().endsWith(".zip");
                            })
                            .map(p -> p.getFileName().toString())
                            .sorted((n1, n2) -> {
                                int pr1 = PackPriorityConfig.getInstance().getPriority(folderName, n1);
                                int pr2 = PackPriorityConfig.getInstance().getPriority(folderName, n2);
                                if (pr1 == 0 && pr2 == 0) return n1.compareToIgnoreCase(n2);
                                if (pr1 == 0) return 1;
                                if (pr2 == 0) return -1;
                                int c = Integer.compare(pr1, pr2);
                                if (c != 0) return c;
                                return n1.compareToIgnoreCase(n2);
                            })
                            .toList();
                            
                        if (!innerPacks.isEmpty()) {
                            var subCat = entryBuilder.startSubCategory(Text.literal(folderName));
                            subCat.setExpanded(true);
                            
                            java.util.Map<String, IntegerListEntry> folderEntries = new java.util.HashMap<>();

                            for (String packName : innerPacks) {
                                int currentPriority = PackPriorityConfig.getInstance().getPriority(folderName, packName);
                                var fieldBuilder = entryBuilder.startIntField(Text.literal(packName), currentPriority)
                                        .setDefaultValue(0)
                                        .setMin(Integer.MIN_VALUE)
                                        .setMax(Integer.MAX_VALUE)
                                        .setTooltip(Text.literal("Lower number = Higher priority. 0 = Lowest Priority (Default)"))
                                        .setErrorSupplier(newValue -> {
                                            if (newValue == 0) return java.util.Optional.empty();
                                            for (java.util.Map.Entry<String, IntegerListEntry> entry : folderEntries.entrySet()) {
                                                if (entry.getKey().equals(packName)) continue; // Skip self
                                                if (entry.getValue().getValue() != null && entry.getValue().getValue().equals(newValue)) {
                                                    return java.util.Optional.of(Text.literal("Priority " + newValue + " is already used by " + entry.getKey()));
                                                }
                                            }
                                            return java.util.Optional.empty();
                                        })
                                        .setSaveConsumer(newValue -> {
                                            if (newValue == 0) {
                                                PackPriorityConfig.getInstance().removePriority(folderName, packName);
                                            } else {
                                                PackPriorityConfig.getInstance().setPriority(folderName, packName, newValue);
                                            }
                                        });
                                
                                IntegerListEntry entry = fieldBuilder.build();
                                folderEntries.put(packName, entry);
                                subCat.add(entry);
                            }
                            category.addEntry(subCat.build());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return builder.build();
    }
}
