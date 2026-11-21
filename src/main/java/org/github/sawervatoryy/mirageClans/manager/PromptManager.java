package org.github.sawervatoryy.mirageClans.manager;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Ввод текста через чат (для GUI: название клана, сумма, имя игрока и т.д.). */
public final class PromptManager {

    private record Prompt(Consumer<String> handler, long expiresAt) {
    }

    private final MirageClans plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public PromptManager(MirageClans plugin) {
        this.plugin = plugin;
    }

    /** Закрывает меню, показывает подсказку и ждёт следующее сообщение игрока. */
    public void ask(Player player, String langKey, Consumer<String> handler) {
        player.closeInventory();
        prompts.put(player.getUniqueId(), new Prompt(handler, System.currentTimeMillis() + 60_000L));
        plugin.lang().send(player, langKey);
        plugin.lang().send(player, "prompt.hint");
    }

    /** Забирает обработчик (одноразово). @return null, если запроса нет или он истёк */
    public Consumer<String> take(UUID id) {
        Prompt p = prompts.remove(id);
        if (p == null || p.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return p.handler();
    }

    public void cancel(UUID id) {
        prompts.remove(id);
    }
}
