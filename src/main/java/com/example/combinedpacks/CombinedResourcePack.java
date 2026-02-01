package com.example.combinedpacks;

import net.minecraft.resource.*;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CombinedResourcePack implements ResourcePack {
    private final String id;
    private final List<ResourcePack> packs = new ArrayList<>();
    private final List<FileSystem> fileSystemsToClose = new ArrayList<>();
    private final ResourcePack metadataSource;

    public CombinedResourcePack(String id, List<Path> paths, ResourcePackSource source) {
        this.id = id;
        
        // Create packs from paths
        for (Path path : paths) {
            ResourcePack pack = createPack(path, source);
            if (pack != null) {
                this.packs.add(pack);
            }
        }
        
        if (packs.isEmpty()) {
            // Should not happen if provider checked
            this.metadataSource = null; 
        } else {
            this.metadataSource = packs.get(0);
        }
    }
    
    private ResourcePack createPack(Path path, ResourcePackSource source) {
        String packId = path.getFileName().toString();
        ResourcePackInfo info = new ResourcePackInfo(packId, Text.literal(packId), source, Optional.empty());
        
        try {
            if (Files.isDirectory(path)) {
                if (Files.exists(path.resolve("pack.mcmeta"))) {
                     return new DirectoryResourcePack(info, path);
                }
            } else if (path.toString().endsWith(".zip")) {
                 FileSystem fs = FileSystems.newFileSystem(path, (ClassLoader)null);
                 fileSystemsToClose.add(fs);
                 Path root = fs.getPath("/");
                 return new DirectoryResourcePack(info, root);
            }
        } catch (IOException e) {
             // Log error?
        }
        return null;
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        for (ResourcePack pack : packs) {
            InputSupplier<InputStream> supplier = pack.open(type, id);
            if (supplier != null) {
                return supplier;
            }
        }
        return null;
    }

    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
         for (ResourcePack pack : packs) {
            InputSupplier<InputStream> supplier = pack.openRoot(segments);
            if (supplier != null) {
                return supplier;
            }
        }
        return null;
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        Map<Identifier, InputSupplier<InputStream>> resources = new HashMap<>();
        
        // Iterate reverse so higher priority (lower index) overrides lower priority
        for (int i = packs.size() - 1; i >= 0; i--) {
            packs.get(i).findResources(type, namespace, prefix, resources::put);
        }
        
        resources.forEach(consumer::accept);
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> namespaces = new HashSet<>();
        for (ResourcePack pack : packs) {
            namespaces.addAll(pack.getNamespaces(type));
        }
        return namespaces;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataSerializer<T> metaReader) throws IOException {
        if (metadataSource == null) return null;
        return metadataSource.parseMetadata(metaReader);
    }

    @Override
    public ResourcePackInfo getInfo() {
        if (metadataSource == null) return new ResourcePackInfo(id, Text.literal(id), ResourcePackSource.create(name -> Text.literal("Combined Pack"), true), Optional.empty());
        return metadataSource.getInfo();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() {
        for (ResourcePack pack : packs) {
            pack.close();
        }
        for (FileSystem fs : fileSystemsToClose) {
            try {
                fs.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
