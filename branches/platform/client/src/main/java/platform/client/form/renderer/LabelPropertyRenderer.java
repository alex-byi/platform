package platform.client.form.renderer;

import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.Format;

class LabelPropertyRenderer extends JLabel { //DefaultTableCellRenderer {

    Format format;
    Color background;

    LabelPropertyRenderer(Format iformat, ComponentDesign design) {
        super();

        format = iformat;
        setBorder(new EmptyBorder(1, 3, 2, 2));
        setOpaque(true);

        if (design != null)
            design.designCell(this);

        background = getBackground();
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(background);
    }

}
