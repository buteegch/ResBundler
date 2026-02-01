package com.example.combinedpacks;

import net.minecraft.resource.*;
import net.minecraft.text.Text;
import net.minecraft.util.path.SymlinkFinder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CombinedResourcePackProvider implements ResourcePackProvider {
    private final Path packsDir;
    private final ResourcePackSource source;
    private final SymlinkFinder symlinkFinder;

    public CombinedResourcePackProvider(Path packsDir, ResourcePackSource source, SymlinkFinder symlinkFinder) {
        this.packsDir = packsDir;
        this.source = source;
        this.symlinkFinder = symlinkFinder;
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        if (!Files.isDirectory(packsDir)) {
            return;
        }

        try (Stream<Path> stream = Files.list(packsDir)) {
            stream.filter(Files::isDirectory)
                  .forEach(subFolder -> processSubFolder(subFolder, profileAdder));
        } catch (IOException e) {
            CombinedPacksMod.LOGGER.error("Failed to list resource packs", e);
        }
    }

    private void processSubFolder(Path subFolder, Consumer<ResourcePackProfile> profileAdder) {
        if (Files.exists(subFolder.resolve("pack.mcmeta"))) {
            return;
        }

        List<Path> packPaths = new ArrayList<>();
        try (Stream<Path> inner = Files.list(subFolder)) {
            List<Path> sorted = inner.sorted().toList();
            for (Path p : sorted) {
                if (p.getFileName().toString().startsWith(".")) continue;
                
                // Check if it looks like a pack (dir or zip)
                if (Files.isDirectory(p)) {
                     if (Files.exists(p.resolve("pack.mcmeta"))) {
                         packPaths.add(p);
                     }
                } else if (p.toString().endsWith(".zip")) {
                     packPaths.add(p);
                }
            }
        } catch (IOException e) {
            return;
        }

        if (packPaths.isEmpty()) {
            return;
        }

        String id = "combined/" + subFolder.getFileName().toString();
        Text displayName = Text.literal(subFolder.getFileName().toString());
        
        ResourcePackProfile.PackFactory factory = new ResourcePackProfile.PackFactory() {
            @Override
            public ResourcePack open(ResourcePackInfo info) {
                return new CombinedResourcePack(id, packPaths, source);
            }

            @Override
            public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
                return open(info);
            }
        };
        
        ResourcePackInfo info = new ResourcePackInfo(id, displayName, source, Optional.empty());
        
        ResourcePackProfile profile = ResourcePackProfile.create(
            info,
            factory,
            ResourceType.CLIENT_RESOURCES,
            new ResourcePackPosition(false, ResourcePackProfile.InsertionPosition.TOP, false)
        );
        
        if (profile != null) {
            profileAdder.accept(profile);
        }
    }
}
