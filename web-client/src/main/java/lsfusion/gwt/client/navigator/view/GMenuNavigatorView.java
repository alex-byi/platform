package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GMenuNavigatorWindow;

import java.util.Set;

public class GMenuNavigatorView extends GNavigatorView {
    public GMenuNavigatorView(GMenuNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, new Label(window.caption), navigatorController);
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getHeight() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getWidth() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
