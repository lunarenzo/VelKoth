package dev.velmax.velkoth.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Cache for pre-rendered MiniMessage components. Resolves dynamic placeholders
 * natively via TagResolvers to ensure perfect layout/gradient styling. Caches the
 * resulting component trees based on their resolved state to achieve near-zero CPU overhead.
 */
public final class TemplateCache {

    private final MiniMessage mm = MiniMessage.miniMessage();

    // Bounded thread-safe LRU cache for rendered components
    private final Map<ResolutionKey, Component> cache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(512, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ResolutionKey, Component> eldest) {
                    return size() > 512;
                }
            }
    );

    /**
     * Cache key for placeholder resolution state.
     */
    public record ResolutionKey(
            String template,
            @Nullable String arena,
            @Nullable String player,
            @Nullable String time,
            @Nullable String capturer
    ) {}

    /**
     * Resolves dynamic placeholders in a MiniMessage template string.
     * Uses a high-performance cache to avoid re-parsing identical states.
     *
     * @param template The raw MiniMessage template string
     * @param arena    The arena display name, or null
     * @param player   The player name, or null
     * @param time     The time string, or null
     * @param capturer The capturer name, or null
     * @return The fully resolved and rendered Adventure Component
     */
    public Component resolve(
            @Nullable String template,
            @Nullable String arena,
            @Nullable String player,
            @Nullable String time,
            @Nullable String capturer
    ) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }

        var key = new ResolutionKey(template, arena, player, time, capturer);

        // Fast path: cache hit
        Component cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Slow path: resolve placeholders natively during deserialization
        // This ensures tags (e.g. <arena>) inside gradients/styles are sized and colored perfectly.
        TagResolver.Builder builder = TagResolver.builder();

        if (arena != null) {
            builder.resolver(Placeholder.unparsed("arena", arena));
        }
        if (player != null) {
            builder.resolver(Placeholder.unparsed("player", player));
        }
        if (time != null) {
            builder.resolver(Placeholder.unparsed("time", time));
        }
        if (capturer != null) {
            builder.resolver(Placeholder.unparsed("capturer", capturer));
        }

        Component rendered = mm.deserialize(template, builder.build());
        cache.put(key, rendered);
        return rendered;
    }

    /**
     * Clears all cached resolutions.
     */
    public void clear() {
        cache.clear();
    }
}
