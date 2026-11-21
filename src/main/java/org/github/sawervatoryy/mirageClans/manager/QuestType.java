package org.github.sawervatoryy.mirageClans.manager;

/** Тип клановых квестов. */
public enum QuestType {
    /** Убийство мобов (target — тип сущности или ANY). */
    KILL_MOB,
    /** Убийство игроков. */
    KILL_PLAYER,
    /** Добыча блоков (target — материал или ANY). */
    BREAK_BLOCK,
    /** Пополнение казны деньгами (Vault). */
    TREASURY_DEPOSIT,
    /** Наигранное время участников (в минутах). */
    PLAYTIME
}
