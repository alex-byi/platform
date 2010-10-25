package platform.client.descriptor.filter;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.IsClassFilterNode;
import platform.client.serialization.ClientSerializationPool;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class IsClassFilterDescriptor extends PropertyFilterDescriptor {

    private ClientObjectClass objectClass;
    public void setObjectClass(ClientObjectClass objectClass) {
        this.objectClass = objectClass;

        IncrementDependency.update(this, "objectClass");
    }
    public ClientObjectClass getObjectClass() {
        return objectClass;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (property == null) return null;
        return property.getGroupObject(groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        
        outStream.writeInt(objectClass.ID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        objectClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inStream);
    }

    @Override
    public FilterNode createNode(Object group) {
        return new IsClassFilterNode((GroupObjectDescriptor) group, this);
    }

    @Override
    public String toString() {
        String result = "";
        if (property != null)
            result += property;
        if (objectClass != null)
            result += " - класса " + objectClass;
        if (result.isEmpty()) result = "КЛАСС";
        return result;
    }
}
