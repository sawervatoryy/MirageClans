package org.github.sawervatoryy.mirageClans.model;

import java.util.UUID;

/**
 * Запись истории казны.
 *
 * @param clanId     клан
 * @param player     UUID игрока (может быть null для системных записей)
 * @param playerName имя игрока / источника
 * @param currency   MONEY или POINTS
 * @param action     DEPOSIT / WITHDRAW / QUEST
 * @param amount     сумма
 * @param timestamp  время (мс)
 */
public record TreasuryEntry(UUID clanId, UUID player, String playerName, String currency, String action,
                            double amount, long timestamp) {
}
