package org.github.sawervatoryy.mirageClans.gui.menu;

import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.gui.Menu;

/** Универсальное меню подтверждения. */
public final class ConfirmMenu extends Menu {

    private final String text;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(MirageClans plugin, Player player, String text, Runnable onConfirm, Runnable onCancel) {
        super(plugin, player);
        this.text = text;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected String menuId() {
        return "confirm";
    }

    @Override
    protected void build() {
        put("info", null, "text", text);
        put("confirm", e -> onConfirm.run());
        put("cancel", e -> onCancel.run());
    }
}
