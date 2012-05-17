package platform.server.form.entity;

import platform.server.form.instance.ActionPropertyObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Map;

public class ActionPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, ActionProperty<P>> implements Instantiable<ActionPropertyObjectInstance<P>>, ServerCustomSerializable {

    public ActionPropertyObjectEntity(ActionProperty<P> property, Map<P, PropertyObjectInterfaceEntity> mapping, String creationScript) {
        super(property, mapping, creationScript);
    }

    public ActionPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }
}
