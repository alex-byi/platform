package platform.server.logics.property;

import platform.server.classes.DoubleClass;
import platform.server.classes.ValueClass;
import platform.server.logics.SessionDataProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeChangeDataProperty<T extends PropertyInterface> extends SessionDataProperty {

    public final Map<ClassPropertyInterface, T> mapInterfaces = new HashMap<ClassPropertyInterface, T>();

    public TimeChangeDataProperty(String sID, String caption, ValueClass[] classes, List<T> propInterfaces) {
        super(sID, caption, classes, DoubleClass.instance);

        int i=0;
        for(ClassPropertyInterface propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface, propInterfaces.get(i++));
    }
}
