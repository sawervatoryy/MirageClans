# MirageClans v0.1
Клановая система для **Paper 1.21.4** (Java 21): анимированные GUI, роли и матрица прав, казна (Vault + PlayerPoints),
общий склад, дома, клановые квесты, прокачка, SQLite / MySQL / MariaDB, PlaceholderAPI.

## Сборка
```
mvn clean package
```
Jar появится в `target/`. Библиотеки (HikariCP, sqlite-jdbc, mariadb-java-client) **не шейдятся** — Paper скачивает их сам
через секцию `libraries` в `plugin.yml` (нужен доступ сервера к Maven Central при первом запуске).

Зависимости (soft): Vault, PlayerPoints, PlaceholderAPI, ProtocolLib (только для режима подсветки GLOW).

## Команды
| Команда | Описание |
|---|---|
| `/clan` (`/c`, `/clans`) | главное меню (при `use-gui: false` — справка) |
| `/clan create <название> <тег>` · `disband confirm` · `leave` | создание / роспуск / выход |
| `/clan invite <игрок>` · `accept [клан]` · `deny [клан]` | приглашения |
| `/clan kick\|promote\|demote\|transfer <игрок>` · `setrole <игрок> <роль>` | управление участниками |
| `/clan info [клан]` · `members` · `top [level\|points\|balance]` | информация и рейтинг |
| `/clan deposit\|withdraw <money\|points> <сумма>` · `balance` · `history` | казна |
| `/clan sethome [имя]` · `home [имя]` · `delhome <имя>` · `homes` | дома |
| `/clan storage` · `quests` · `level` · `upgrade` | склад, квесты, уровни |
| `/clan ff [on\|off]` · `glow <off\|glow\|name>` · `glow color <цвет>` | PvP и подсветка |
| `/clan role list\|create <название>\|delete <id>\|perm <id> <право>` | роли |
| `/cc <сообщение>` · `/cc` | клановый чат / режим-переключатель |
| `/clan reload` (`mirageclans.admin`) | перезагрузка конфигов |

## Права ролей
`TREASURY_DEPOSIT, TREASURY_WITHDRAW, STORAGE, HOME_TELEPORT, HOME_MANAGE, KICK, INVITE, PVP_MANAGE, MANAGE_ROLES, UPGRADE, SETTINGS`.
Лидер имеет все права. Роли настраиваются в GUI (`Роли и права`) и в `config.yml → roles.templates`.

## PlaceholderAPI (`%mirageclans_<id>%`)
`name, tag, role, leader, level, xp, xp_needed, xp_left, xp_percent, balance, points, members_online, members_total,
members_max, friendly_fire, homes, has_clan`.
Relational (TAB): `%rel_mirageclans_color%` (цвет ника соклановца), `%rel_mirageclans_same%`.
Значения для игрока без клана — `config.yml → placeholders.no-clan`.

## Конфиги
`config.yml` (настройки, уровни, роли), `lang.yml` (все сообщения), `menus.yml` (все меню и кнопки), `quests.yml` (квесты).
Дизайн: основной цвет `&f`, выделительный `&#00ACFF`, скобки `« » ‹ › ⌈ ⌋`.

## Архитектура
`manager` (логика) · `storage` (БД, всё в отдельном потоке) · `gui` (меню) · `listener` · `command` · `hook` · `model` · `config`.
* Данные кланов — в памяти, запись в БД асинхронная (однопоточная очередь → порядок записей сохраняется).
* Казна: атомарные операции под монитором клана, сначала списание у источника — потом зачисление (без дюпов).
* Склад: один общий `Inventory` на клан, заблокированные слоты — предмет-заглушка; сериализация в главном потоке, запись асинхронно.
* Friendly Fire: владелец определяется для стрел, трезубцев, зелий (в т.ч. туманных), динамита и питомцев.

## Заметки
* Режим подсветки **GLOW** показывает свечение только соклановцам (пакеты через ProtocolLib). Режим **NAME_COLOR** красит ники
  через scoreboard-команду (видно всем) — для персональной раскраски используйте `%rel_mirageclans_color%` в TAB.
* Анимация главного меню — «волна» от центра, настраивается в `config.yml → animation`.
