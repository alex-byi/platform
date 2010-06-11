package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.cell.CellView;
import platform.client.logics.ClientCellView;
import platform.interop.CellDesign;

import java.text.Format;
import java.io.IOException;
import java.awt.*;

public interface ClientType {

    int getMinimumWidth(FontMetrics fontMetrics);
    int getPreferredWidth(FontMetrics fontMetrics);
    int getMaximumWidth(FontMetrics fontMetrics);

    Format getDefaultFormat();

    PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design);
    CellView getPanelComponent(ClientCellView key, ClientForm form);

    PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format, CellDesign design) throws IOException, ClassNotFoundException;
    PropertyEditorComponent getClassComponent(ClientForm form, ClientCellView property, Object value, Format format) throws IOException, ClassNotFoundException;

}
