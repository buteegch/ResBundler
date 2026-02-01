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

        // Sort packs based on priority
        PackPriorityConfig config = PackPriorityConfig.getInstance();
        config.load();
        String folderName = subFolder.getFileName().toString();

        packPaths.sort((p1, p2) -> {
            String n1 = p1.getFileName().toString();
            String n2 = p2.getFileName().toString();
            int pr1 = config.getPriority(folderName, n1);
            int pr2 = config.getPriority(folderName, n2);
            if (pr1 == 0 && pr2 == 0) return n1.compareToIgnoreCase(n2);
            if (pr1 == 0) return 1;
            if (pr2 == 0) return -1;
            int c = Integer.compare(pr1, pr2);
            if (c != 0) return c;
            return n1.compareToIgnoreCase(n2);
        });

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
