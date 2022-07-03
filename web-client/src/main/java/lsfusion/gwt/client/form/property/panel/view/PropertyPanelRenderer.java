package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.CaptionWidget;
import lsfusion.gwt.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class PropertyPanelRenderer extends PanelRenderer {

    private SizedWidget sizedView;

    private Label label;

    public PropertyPanelRenderer(final GFormController form, ActionOrPropertyValueController controller, GPropertyDraw property, GGroupObjectValue columnKey, Result<CaptionWidget> captionContainer) {
        super(form, controller, property, columnKey);

        value.addStyleName("dataPanelRendererPanel");

        SizedWidget valueWidget = value.setSized();

        if (property.notNull || property.hasChangeAction)
            appendCorners(property.notNull, valueWidget.widget);

        sizedView = initCaption(valueWidget, property, captionContainer);

        finalizeInit();
    }

    private SizedWidget initCaption(SizedWidget valuePanel, GPropertyDraw property, Result<CaptionWidget> captionContainer) {
        if(property.caption == null) // if there is no (empty) static caption and no dynamic caption
            return valuePanel;

        label = new Label();
        label.addStyleName("alignPanelLabel");
        label.addStyleName("dataPanelRendererPanel");

        if (this.property.captionFont != null)
            this.property.captionFont.apply(label.getElement().getStyle());

        GFlexAlignment panelCaptionAlignment = property.getPanelCaptionAlignment(); // vertical alignment
        boolean captionLast = property.isPanelCaptionLast();
        SizedWidget sizedLabel = new SizedWidget(label, property.getCaptionWidth(), property.getCaptionHeight());

        if(property.isAlignCaption() && captionContainer != null) { // align caption
            if(!panelCaptionAlignment.equals(GFlexAlignment.END))
                captionLast = false; // it's odd having caption last for alignments other than END

            captionContainer.set(new CaptionWidget(captionLast ? valuePanel : sizedLabel, GFlexAlignment.START, panelCaptionAlignment));

            return captionLast ? sizedLabel : valuePanel;
        }

        SizedFlexPanel panel = new SizedFlexPanel(property.panelCaptionVertical);

        if (!captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        valuePanel.addFill(panel);

        if (captionLast)
            sizedLabel.add(panel, panelCaptionAlignment);

        return new SizedWidget(panel);
    }

    public void appendCorners(boolean notNull, Widget panel) {
        Element panelElement = panel.getElement();
//        panelElement.getStyle().setPosition(Style.Position.RELATIVE); // for corners (setStatic sets position absolute, so we don't need to do this for setStatic)
//        DivElement changeEventSign = Document.get().createDivElement();
        panelElement.addClassName("rightBottomCornerTriangle");
        if(notNull)
            panelElement.addClassName("notNullCornerTriangle");
        else
            panelElement.addClassName("changeActionCornerTriangle");
//        panelElement.appendChild(changeEventSign);
    }

    @Override
    public SizedWidget getSizedWidget() {
        return sizedView;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label != null ? label : super.getTooltipWidget();
    }

    private boolean notEmptyText;
    protected void setLabelText(String text) {
        if(label == null) {
            assert text == null || text.isEmpty();
            return;
        }

        label.setText(text);

        boolean notEmptyText = text != null && !text.isEmpty();
        if(this.notEmptyText != notEmptyText) {
            if(notEmptyText)
                label.addStyleName("notEmptyPanelLabel");
            else
                label.removeStyleName("notEmptyPanelLabel");
            this.notEmptyText = notEmptyText;
        }
    }
}
