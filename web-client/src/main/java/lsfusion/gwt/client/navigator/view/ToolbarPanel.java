package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class ToolbarPanel extends NavigatorPanel {

    public ToolbarPanel(boolean vertical, GNavigatorWindow window) {
        super(vertical);

        addStyleName("navbar-expand"); // navbar-expand to set horizontal paddings (vertical are set in navbar-text)

        setAlignment(vertical, panel, window);
    }

    private static void setAlignment(boolean vertical, ResizableComplexPanel panel, GNavigatorWindow toolbarWindow) {
        if (vertical) {
            panel.addStyleName(toolbarWindow.alignmentX == GNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentX == GNavigatorWindow.RIGHT_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            // adding margin-auto to make all buttons visible when scroll appears
            panel.addStyleName(toolbarWindow.alignmentY == GNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center my-auto" :
                    (toolbarWindow.alignmentY == GNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        } else {
            panel.addStyleName(toolbarWindow.alignmentY == GNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentY == GNavigatorWindow.BOTTOM_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            // adding margin-auto to make all buttons visible when scroll appears
            panel.addStyleName(toolbarWindow.alignmentX == GNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center mx-auto" :
                    (toolbarWindow.alignmentX == GNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        }
    }

    public static boolean hasBorder(Widget widget) {
        return MainFrame.useBootstrap ? GwtClientUtils.hasClassNamePrefix(widget.getElement().getClassName(), WindowsController.BACKGROUND_PREFIX) : false;
    }

    public static boolean isPopupOver(Widget widget) {
        return widget.getElement().hasClassName(WindowsController.NAVBAR_POPUP_OVER_SELECTED_HOVER);
    }
}
