package lsfusion.server.logics.action.flow;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

import static lsfusion.server.logics.property.PropertyFact.createForAction;

public class IfAction extends KeepContextAction {

    private final PropertyInterfaceImplement<PropertyInterface> ifProp;
    private final ActionMapImplement<?, PropertyInterface> trueAction;
    private final ActionMapImplement<?, PropertyInterface> falseAction;

    public <I extends PropertyInterface> IfAction(LocalizedString caption, boolean not, ImOrderSet<I> innerInterfaces, PropertyMapImplement<?, I> ifProp, ActionMapImplement<?, I> trueAction, ActionMapImplement<?, I> falseAction) {
        super(caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.ifProp = ifProp.map(mapInterfaces);
        ActionMapImplement<?, PropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionMapImplement<?, PropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
        if (!not) {
            this.trueAction = mapTrue;
            this.falseAction = mapFalse;
        } else {
            this.trueAction = mapFalse;
            this.falseAction = mapTrue;
        }

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return PropertyFact.createIfElseUProp(interfaces, ifProp,
                trueAction != null ? trueAction.mapCalcWhereProperty() : null,
                falseAction !=null ? falseAction.mapCalcWhereProperty() : null);
    }

    public ImSet<Action> getDependActions() {
        ImSet<Action> result = SetFact.EMPTY();
        if (trueAction != null) {
            result = result.merge(trueAction.action);
        }
        if (falseAction != null) {
            result = result.merge(falseAction.action);
        }
        return result;
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        MSet<Property> used = SetFact.mSet();
        ifProp.mapFillDepends(used);
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type trueType = trueAction == null ? null : trueAction.action.getSimpleRequestInputType(optimistic, inRequest);
        Type falseType = falseAction == null ? null : falseAction.action.getSimpleRequestInputType(optimistic, inRequest);

        if (!optimistic) {
            if (trueType == null) {
                return null;
            }
            if (falseAction != null && falseType == null) {
                return null;
            }
        }

        return trueType == null
               ? falseType
               : falseType == null
                 ? trueType
                 : trueType.getCompatible(falseType);
    }

    @Override
    public CustomClass getSimpleAdd() {
        return null; // пока ничего не делаем, так как на клиенте придется, "отменять" изменения
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return super.getSimpleDelete(); // по аналогии с верхним
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        if (readIf(context)) {
            if (trueAction != null) {
                return trueAction.execute(context);
            }
        } else {
            if (falseAction != null) {
                return falseAction.execute(context);
            }
        }
        return FlowResult.FINISH;
    }

    private boolean readIf(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return ifProp.read(context, context.getKeys()) != null;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return falseAction == null; // нужно разбивать на if true и if false, потом реализуем
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ForAction.getPushWhere(ifProp);
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForAction.pushFor(interfaces, ifProp, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> createForAction(context1, where, orders1, ordersNotNull1, trueAction.map(mapInnerInterfaces), null, false, SetFact.EMPTY(), false));
    }
}
