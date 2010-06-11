package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ButtonCellView extends JButton implements CellView {

    public ButtonCellView(ClientCellView key) {
        super(key.getFullCaption());

        key.design.designComponent(this);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                listener.cellValueChanged(null);
            }
        });
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;
    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object ivalue) {
        // собственно, а как в Button нужно устанавливать value
    }

    public void startEditing() {
        doClick(500);
    }
}
