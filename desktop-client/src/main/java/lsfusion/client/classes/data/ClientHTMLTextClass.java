package lsfusion.client.classes.data;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TextPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TextPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

import java.awt.*;

public class ClientHTMLTextClass extends ClientTextClass{

    public ClientHTMLTextClass() {
        super("html");
    }

    @Override
    public byte getTypeId() {
        return DataType.HTMLTEXT;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, true);
    }

    @Override
    public boolean trimTooltip() {
        return false;
    }
}
