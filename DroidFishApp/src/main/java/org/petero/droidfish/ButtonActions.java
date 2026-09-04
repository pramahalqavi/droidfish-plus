/*
    DroidFish - An Android chess program.
    Copyright (C) 2012  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

/**
 * Handle all actions connected to a button.
 */
public class ButtonActions {
    private View itemView;
    private ImageButton button;
    private String name;
    private int longClickDialog;
    private int menuTitle;

    private UIAction mainAction = null;
    private ArrayList<UIAction> menuActions = new ArrayList<>();

    private static final int maxMenuActions = 6;

    /** Constructor. */
    public ButtonActions(String buttonName, int longClickDialog, int menuTitle) {
        itemView = null;
        button = null;
        name = buttonName;
        this.longClickDialog = longClickDialog;
        this.menuTitle = menuTitle;
    }

    public boolean isEnabled() {
        if (mainAction != null)
            return true;
        for (UIAction a : menuActions)
            if (a != null)
                return true;
        return false;
    }

    /** Connect GUI button and optional parent navigation item view. */
    public void setItemView(View itemView, ImageButton button, final Activity activity, final Runnable onActionClicked) {
        this.itemView = itemView;
        this.button = button;
        View.OnClickListener clickListener = v -> {
            if (onActionClicked != null)
                onActionClicked.run();
            if (mainAction != null) {
                if (mainAction.enabled())
                    mainAction.run();
            } else {
                showMenu(activity);
            }
        };
        View.OnLongClickListener longClickListener = v -> showMenu(activity);

        if (itemView != null) {
            itemView.setOnClickListener(clickListener);
            itemView.setOnLongClickListener(longClickListener);
        }
        if (button != null) {
            button.setOnClickListener(clickListener);
            button.setOnLongClickListener(longClickListener);
        }
    }

    public void setItemView(View itemView, ImageButton button, final Activity activity) {
        setItemView(itemView, button, activity, null);
    }

    /** Connect GUI button. */
    public void setImageButton(ImageButton button, final Activity activity) {
        setItemView(null, button, activity);
    }

    private boolean showMenu(Activity activity) {
        boolean haveActions = false;
        boolean haveEnabledActions = false;
        for (UIAction a : menuActions) {
            if (a != null) {
                haveActions = true;
                if (a.enabled())
                    haveEnabledActions = true;
            }
        }
        if (haveActions) {
            if (haveEnabledActions) {
                activity.removeDialog(longClickDialog);
                activity.showDialog(longClickDialog);
            }
            return true;
        }
        return false;
    }

    /** Get menu title resource. */
    public int getMenuTitle() {
        return menuTitle;
    }

    /** Get a menu action. */
    public ArrayList<UIAction> getMenuActions() {
        return menuActions;
    }

    /** Update button actions from preferences settings. */
    public void readPrefs(SharedPreferences settings, ActionFactory factory) {
        boolean visible = false;
        String actionId = settings.getString("button_action_" + name + "_0", "");
        mainAction = factory.getAction(actionId);
        if (mainAction != null)
            visible = true;
        menuActions.clear();
        for (int i = 0; i < maxMenuActions; i++) {
            actionId = settings.getString("button_action_" + name + "_" + (i+1), "");
            if (actionId.isEmpty() && name.equals("custom1") && (i + 1 == 6)) {
                actionId = "toggleMoveClassifications";
            }
            UIAction a = factory.getAction(actionId);
            if (a != null)
                visible = true;
            menuActions.add(a);
        }
        if (itemView != null)
            itemView.setVisibility(visible ? View.VISIBLE : View.GONE);
        else if (button != null)
            button.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Get icon resource for button. */
    public int getIcon() {
        int ret = -1;
        if (mainAction != null)
            ret = mainAction.getIcon();
        if (ret == -1)
            ret = R.raw.custom;
        return ret;
    }

    /** Get navigation bar label text for this action. */
    public String getLabel(Context context, int defaultResId) {
        if (mainAction != null) {
            String id = mainAction.getId();
            if ("flipboard".equals(id))
                return context.getString(R.string.nav_action_flip);
            if ("toggleAnalysis".equals(id))
                return context.getString(R.string.nav_action_action);
            int nameRes = mainAction.getName();
            if (nameRes != 0) {
                String full = context.getString(nameRes);
                if (full.startsWith("Toggle "))
                    full = full.substring(7);
                return full;
            }
        }
        return context.getString(defaultResId);
    }
}
