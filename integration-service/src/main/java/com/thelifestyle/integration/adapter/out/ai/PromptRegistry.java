package com.thelifestyle.integration.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelifestyle.integration.domain.PromptDefinition;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// One small registry, not a framework: reads every `prompts/*.json` file
// under the classpath at startup into a Map<masterType, PromptDefinition>,
// keeping the highest version file when a master type has more than one
// (`{masterType}.v{n}.json`). Adding a master type or a new prompt version
// is "add a file," never a Java change here. A malformed prompt file fails
// startup loudly (IllegalStateException) rather than silently running with
// half the master types unsupported.
@Component
public class PromptRegistry {
    private final Map<String, PromptDefinition> byMasterType = new HashMap<>();

    public PromptRegistry(ObjectMapper objectMapper) {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:prompts/*.json");
            var versions = new HashMap<String, Integer>();
            for (var resource : resources) {
                var definition = objectMapper.readValue(resource.getInputStream(), PromptDefinition.class);
                var version = parseVersion(definition.version());
                if (version >= versions.getOrDefault(definition.masterType(), -1)) {
                    versions.put(definition.masterType(), version);
                    byMasterType.put(definition.masterType(), definition);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load prompt definitions from classpath:prompts/*.json", ex);
        }
    }

    public Optional<PromptDefinition> find(String masterType) {
        return Optional.ofNullable(byMasterType.get(masterType));
    }

    private static int parseVersion(String version) {
        try {
            return Integer.parseInt(version.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
