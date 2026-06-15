package dev.velmax.velkoth.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for pre-parsed MiniMessage templates. Resolves placeholders
 * dynamically by walking the component tree, achieving near 0% CPU overhead.
 */
public final class TemplateCache {

    private final Map<String, Component> templates = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final TagResolver preParseResolvers = TagResolver.builder()
            .resolver(Placeholder.component("arena", Component.text("%%ARENA%%")))
            .resolver(Placeholder.component("player", Component.text("%%PLAYER%%")))
            .resolver(Placeholder.component("time", Component.text("%%TIME%%")))
            .resolver(Placeholder.component("capturer", Component.text("%%CAPTURER%%")))
            .build();

    public Component getTemplate(String rawTemplate) {
        if (rawTemplate == null) {
            return Component.empty();
        }
        return templates.computeIfAbsent(rawTemplate, t -> mm.deserialize(t, preParseResolvers));
    }

    public void clear() {
        templates.clear();
    }

    /**
     * Resolves placeholders in a pre-parsed component tree recursively.
     * Bypasses regex and MiniMessage parsing completely.
     */
    public Component resolve(Component component, String arena, String player, String time, String capturer) {
        if (component instanceof TextComponent textComp) {
            String content = textComp.content();
            String newContent = content;
            if (arena != null && newContent.contains("%%ARENA%%")) {
                newContent = newContent.replace("%%ARENA%%", arena);
            }
            if (player != null && newContent.contains("%%PLAYER%%")) {
                newContent = newContent.replace("%%PLAYER%%", player);
            }
            if (time != null && newContent.contains("%%TIME%%")) {
                newContent = newContent.replace("%%TIME%%", time);
            }
            if (capturer != null && newContent.contains("%%CAPTURER%%")) {
                newContent = newContent.replace("%%CAPTURER%%", capturer);
            }

            Component result = newContent.equals(content) ? textComp : textComp.content(newContent);

            if (!component.children().isEmpty()) {
                List<Component> newChildren = new ArrayList<>(component.children().size());
                for (Component child : component.children()) {
                    newChildren.add(resolve(child, arena, player, time, capturer));
                }
                result = result.children(newChildren);
            }
            return result;
        } else {
            if (!component.children().isEmpty()) {
                List<Component> newChildren = new ArrayList<>(component.children().size());
                for (Component child : component.children()) {
                    newChildren.add(resolve(child, arena, player, time, capturer));
                }
                return component.children(newChildren);
            }
            return component;
        }
    }
}
