package org.github.sawervatoryy.mirageClans.util;

/**
 * Результат операции: успех/ошибка + ключ сообщения из lang.yml и плейсхолдеры.
 *
 * @param ok  успешна ли операция
 * @param key ключ lang.yml (может быть null — тогда ничего не отправляется)
 * @param kv  пары плейсхолдер/значение
 */
public record Result(boolean isOk, String key, String[] kv) {

    public static Result ok() {
        return new Result(true, null, new String[0]);
    }

    public static Result ok(String key, String... kv) {
        return new Result(true, key, kv);
    }

    public static Result fail(String key, String... kv) {
        return new Result(false, key, kv);
    }
}
