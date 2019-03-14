package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.DataObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class DefaultChangeObjectActionProperty extends SystemExplicitAction {

    private final ObjectEntity object;
    
    public DefaultChangeObjectActionProperty(ValueClass baseClass, ObjectEntity object) {
        super(LocalizedString.create("CO", false), baseClass);
        this.object = object;
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        final FormInstance formInstance = context.getFormFlowInstance();
        ObjectInstance objectInstance = formInstance.instanceFactory.getInstance(object);
        if (objectInstance.groupTo.curClassView.isPanel()) { // в grid'е диалог не имеет смысла
            final ObjectValue oldValue = objectInstance.getObjectValue();
            ObjectValue changeValue;
            if (objectInstance instanceof CustomObjectInstance) {
                final CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                changeValue = context.requestUserObject(
                        formInstance.createChangeObjectDialogRequest(customObjectInstance.getBaseClass(), oldValue, customObjectInstance.groupTo, context.stack)
                );
            } else {
                changeValue = context.requestUserData(((DataObjectInstance) objectInstance).getBaseClass(), oldValue.getValue());
            }
            if (changeValue != null) {
                formInstance.changeObject(objectInstance, changeValue);
            }
        }
    }
}
