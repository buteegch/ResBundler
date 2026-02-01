package com.example.combinedpacks;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PackPriorityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("combinedpacks");
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("combinedpacks_priority.txt");
    
    // Map<CombinedPackName, Map<InnerPackName, Integer>>
    private Map<String, Map<String, Integer>> priorities = new HashMap<>();

    private static final PackPriorityConfig INSTANCE = new PackPriorityConfig();

    public static PackPriorityConfig getInstance() {
        return INSTANCE;
    }

    public void load() {
        priorities.clear();
        if (Files.exists(CONFIG_PATH)) {
            try {
                List<String> lines = Files.readAllLines(CONFIG_PATH);
                String currentSection = null;
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    if (line.startsWith("[") && line.endsWith("]")) {
                        currentSection = line.substring(1, line.length() - 1);
                        priorities.putIfAbsent(currentSection, new HashMap<>());
                    } else if (currentSection != null && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        String packName = parts[0].trim();
                        try {
                            int priority = Integer.parseInt(parts[1].trim());
                            priorities.get(currentSection).put(packName, priority);
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load priority config", e);
            }
        }
    }

    public void setPriority(String combinedPackName, String innerPackName, int priority) {
        priorities.computeIfAbsent(combinedPackName, k -> new HashMap<>()).put(innerPackName, priority);
    }

    public void removePriority(String combinedPackName, String innerPackName) {
        if (priorities.containsKey(combinedPackName)) {
            priorities.get(combinedPackName).remove(innerPackName);
        }
    }

    public void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Priority Configuration\n");
        sb.append("# Format: [CombinedPackName]\n");
        sb.append("# InnerPackName=Priority\n");
        sb.append("# Lower number = Higher priority\n\n");
        
        List<String> sections = new ArrayList<>(priorities.keySet());
        Collections.sort(sections);
        
        for (String section : sections) {
            sb.append("[").append(section).append("]\n");
            Map<String, Integer> entries = priorities.get(section);
            
            List<String> keys = new ArrayList<>(entries.keySet());
            Collections.sort(keys);
            
            for (String key : keys) {
                sb.append(key).append("=").append(entries.get(key)).append("\n");
            }
            sb.append("\n");
        }
        
        try {
            Files.writeString(CONFIG_PATH, sb.toString());
        } catch (IOException e) {
            LOGGER.error("Failed to save priority config", e);
        }
    }

    public int getPriority(String combinedPackName, String innerPackName) {
        return priorities.getOrDefault(combinedPackName, Collections.emptyMap())
                         .getOrDefault(innerPackName, 0);
    }
    
    public Map<String, Map<String, Integer>> getPriorities() {
        return priorities;
    }
}
