package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.ApplicationContext;
import lsfusion.client.descriptor.CustomConstructible;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.descriptor.nodes.ComponentNode;
import lsfusion.client.descriptor.nodes.ContainerNode;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.*;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.interop.form.layout.ContainerType.*;

public class ClientContainer extends ClientComponent implements AbstractContainer<ClientContainer, ClientComponent>, CustomConstructible {

    private String caption;
    private String description;

    public List<ClientComponent> children = new ArrayList<ClientComponent>();

    private byte type = ContainerType.CONTAINER;

    public ClientContainer() {
    }

    public ClientContainer(ApplicationContext context) {
        super(context);

        customConstructor();
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, description);

        outStream.writeByte(type);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        caption = pool.readString(inStream);
        description = pool.readString(inStream);

        type = inStream.readByte();
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getContainerDefaultConstraints(super.getDefaultConstraints());
    }

    public void customConstructor() {
        initAggregateObjects(getContext());
    }

    @Override
    public String toString() {
        String result = caption == null ? "" : caption;
        if (description == null)
            result += " (";
        else
            result += (result.isEmpty() ? "" : " ") + "(" + description + ",";
        result += getID();
        result += ")";
        return result + "[sid:" + getSID() + "]";
    }

    @Override
    public ComponentNode getNode() {
        return new ContainerNode(this);
    }

    public void removeFromChildren(ClientComponent component) {
        component.container = null;
        children.remove(component);

        updateDependency(this, "children");
    }

    public void addToChildren(int index, ClientComponent component) {
        add(index, component);
        updateDependency(this, "children");
    }

    public void addToChildren(ClientComponent component) {
        addToChildren(children.size(), component);
    }

    public void add(ClientComponent component) {
        add(children.size(), component);
    }

    public void add(int index, ClientComponent component) {
        if (component.container != null) {
            component.container.removeFromChildren(component);
        }
        children.add(index, component);
        component.container = this;
    }

    public void moveChild(ClientComponent compFrom, ClientComponent compTo) {
        BaseUtils.moveElement(children, compFrom, compTo);
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    public void setCaption(String caption) {
        setRawCaption(caption);
    }

    @Override
    public String getCaption() {
        if (caption == null || caption.equals("")) {
            return (description == null) ? "" : description;
        } else
            return caption;
    }

    //приходится выделять отдельное свойство, чтобы можно было редактировать и при этом возвращать более хитрый caption
    public String getRawCaption() {
        return caption;
    }

    public void setRawCaption(String caption) {
        this.caption = caption;
        updateDependency(this, "rawCaption");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateDependency(this, "description");
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getStringType() {  // usage через reflection
        return ContainerType.getTypeNamesList().get((int) type);
    }

    public void setStringType(String type) {
        this.type = (byte) ContainerType.getTypeNamesList().indexOf(type);
        updateDependency(this, "stringType");
    }

    public boolean isTabbedPane() {
        return type == ContainerType.TABBED_PANE;
    }

    public boolean isSplitPane() {
        return type == ContainerType.SPLIT_PANE_HORIZONTAL || type == SPLIT_PANE_VERTICAL;
    }

    @Override
    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public ClientContainer findContainerBySID(String sID) {
        if (sID.equals(this.sID)) return this;
        for (ClientComponent comp : children) {
            if (comp instanceof ClientContainer) {
                ClientContainer result = ((ClientContainer) comp).findContainerBySID(sID);
                if (result != null) return result;
            }
        }
        return null;
    }

    public boolean isAncestorOf(ClientContainer container) {
        return container != null && (equals(container) || isAncestorOf(container.container));
    }

    public List<ClientComponent> getChildren() {
        return children;
    }

    @Override
    public DoNotIntersectSimplexConstraint getChildConstraints() {
        if (type == CONTAINERV) {
            return SingleSimplexConstraint.TOTHE_BOTTOM;
        } else if (type == CONTAINERH) {
            return SingleSimplexConstraint.TOTHE_RIGHT;
        }else if (type == CONTAINERVH) {
            return SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
        } else {
            return super.getChildConstraints();
        }
    }

}
