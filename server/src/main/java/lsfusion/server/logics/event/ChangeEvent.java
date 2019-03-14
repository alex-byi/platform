package lsfusion.server.logics.event;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ChangeEvent<C extends PropertyInterface> {

    public static final PrevScope scope = PrevScope.DB;

    protected final Property<C> writeTo; // что меняем
    public final PropertyMapImplement<? extends PropertyInterface, C> where;

    public final PropertyInterfaceImplement<C> writeFrom;

    public Property<?> getWhere() {
        return where.property;
    }

    public ChangeEvent(Property<C> writeTo, PropertyInterfaceImplement<C> writeFrom, PropertyMapImplement<?, C> where) {
        assert ((Property)where.property).noDB();
        this.writeTo = writeTo;
        this.where = where;
        this.writeFrom = writeFrom;
    }

    public ImSet<OldProperty> getOldDepends() {
        ImSet<OldProperty> result = where.mapOldDepends();
        if(Settings.get().isUseEventValuePrevHeuristic())
            return result;
        return result.merge(writeFrom.mapOldDepends());
    }

    public ImSet<Property> getDepends() {
        MSet<Property> mResult = SetFact.mSet();
        where.mapFillDepends(mResult);
        writeFrom.mapFillDepends(mResult);
        return mResult.immutable();
    }

    public PropertyChange<C> getChange(PropertyChanges changes, ImMap<C, Expr> joinValues) {
        ImRevMap<C, KeyExpr> mapKeys = writeTo.getMapKeys();
        ImMap<C, Expr> mapExprs = MapFact.override(mapKeys, joinValues);

        Where changeWhere = where.mapExpr(mapExprs, changes).getWhere();
        if(changeWhere.isFalse()) // для оптимизации
            return writeTo.getNoChange();

        mapExprs = PropertyChange.simplifyExprs(mapExprs, changeWhere);
        Expr writeExpr = writeFrom.mapExpr(mapExprs, changes);
//        if(!isWhereFull())
//            changeWhere = changeWhere.and(writeExpr.getWhere().or(writeTo.getExpr(mapExprs, changes).getWhere()));
        return new PropertyChange<>(mapKeys, changeWhere, writeExpr, joinValues);
    }

    public DataChanges getDataChanges(PropertyChanges changes, ImMap<C, Expr> joinValues) {
        return writeTo.getDataChanges(getChange(changes, joinValues), changes);
    }

    public ImSet<Property> getUsedDataChanges(StructChanges changes) {
        ImSet<Property> usedChanges = where.property.getUsedChanges(changes);
        if(!changes.hasChanges(usedChanges)) // для верхней оптимизации
            return usedChanges;

        return SetFact.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends()));
    }

    public boolean isData() {
        return writeTo instanceof DataProperty;
    }
}
