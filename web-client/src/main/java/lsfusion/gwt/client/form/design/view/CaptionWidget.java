package lsfusion.gwt.client.form.design.view;

import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.SizedWidget;

public class CaptionWidget {

    public SizedWidget widget;

    public GFlexAlignment horzAlignment;
    public GFlexAlignment vertAlignment;

    public CaptionWidget(SizedWidget widget, GFlexAlignment horzAlignment, GFlexAlignment vertAlignment) {
        this.widget = widget;
        this.horzAlignment = horzAlignment;
        this.vertAlignment = vertAlignment;
    }
}
