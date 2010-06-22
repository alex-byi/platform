package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.table.MapKeysTable;
import platform.server.logics.table.TableFactory;
import platform.server.session.*;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.client.RemoteFormView;

import java.sql.SQLException;
import java.util.*;

@GenericImmutable
public abstract class Property<T extends PropertyInterface> extends AbstractNode implements MapKeysInterface<T> {

    public final String sID;

    public String caption;

    public String toString() {
        return caption;
    }

    public int ID=0;

    public final Collection<T> interfaces;

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends PropertyInterface> boolean intersect(Property<P> property, Map<P,T> map) {
        return !getClassWhere().and(new ClassWhere<T>(property.getClassWhere(),map)).isFalse();
    }

    @GenericLazy
    public boolean allInInterface(Map<T,? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere());
    }

    @GenericLazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }

    public Property(String sID, String caption, List<T> interfaces) {
        this.sID = sID;
        this.caption = caption;
        this.interfaces = interfaces;

        changeExpr = new PullExpr(toString() + " value");
    }

    public Map<Time, TimeChangeDataProperty<T>> timeChanges = new HashMap<Time, TimeChangeDataProperty<T>>();

    protected void fillDepends(Set<Property> depends, boolean derived) {
    }

    public boolean notDeterministic() {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, false);
        for(Property property : depends)
            if(property.notDeterministic())
                return true;
        return false;
    }

    public Set<Property> getDepends() {
        Set<Property> depends = new HashSet<Property>();
        fillDepends(depends, true);
        return depends;
    }

    @GenericLazy
    public Map<T, KeyExpr> getMapKeys() {
        Map<T, KeyExpr> result = new HashMap<T, KeyExpr>();
        for(T propertyInterface : interfaces)
            result.put(propertyInterface,new KeyExpr(propertyInterface.toString()));
        return result;
    }

    protected static class DefaultChanges extends Changes<DefaultChanges> {
        private DefaultChanges() {
        }
        public final static DefaultChanges EMPTY = new DefaultChanges();

        private DefaultChanges(DefaultChanges changes, SessionChanges merge) {
            super(changes, merge);
        }
        public DefaultChanges addChanges(SessionChanges changes) {
            return new DefaultChanges(this, changes);
        }

        private DefaultChanges(DefaultChanges changes, DefaultChanges merge) {
            super(changes, merge);
        }
        public DefaultChanges add(DefaultChanges changes) {
            return new DefaultChanges(this, changes);
        }

        public DefaultChanges(DefaultChanges changes, MapValuesTranslate mapValues) {
            super(changes, mapValues);
        }
        public DefaultChanges translate(MapValuesTranslate mapValues) {
            return new DefaultChanges(this, mapValues);
        }
    }

    public static Modifier<DefaultChanges> defaultModifier = new Modifier<DefaultChanges>(){
        public DefaultChanges newChanges() {
            return DefaultChanges.EMPTY;
        }
        public SessionChanges getSession() {
            return SessionChanges.EMPTY;
        }
        public DefaultChanges fullChanges() {
            return DefaultChanges.EMPTY;
        }

        public DefaultChanges used(Property property, DefaultChanges usedChanges) {
            return usedChanges;
        }
        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof DefaultChanges;
        }
    };

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, defaultModifier,null);
    }

    public <U extends Changes<U>> Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier<U> modifier, WhereBuilder changedWhere) {

        assert joinImplement.size()==interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = modifier.changed(this, joinImplement, changedExprWhere);

        if(changedExpr==null && isStored()) {
            if(!hasChanges(modifier)) // если нету изменений
                return mapTable.table.join(BaseUtils.join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if(usePreviousStored())
                changedExpr = calculateExpr(joinImplement, modifier, changedExprWhere);
        }

        if(changedExpr!=null) {
            if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        } else
            return calculateExpr(joinImplement, modifier, changedWhere);
    }

    public Expr calculateExpr(Map<T, ? extends Expr> joinImplement) {
        return calculateExpr(joinImplement, defaultModifier, null);
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);

    @GenericLazy
    public ClassWhere<T> getClassWhere() {
        return getQuery("value").getClassWhere(new ArrayList<String>());
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(Map<T, DataObject> mapValues, Map<T, ConcreteClass> mapClasses) {
        Map<T,Expr> mapExprs = new HashMap<T, Expr>();
        for(Map.Entry<T,DataObject> keyField : mapValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object,mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String,String>(new HashMap<String,KeyExpr>(),getExpr(mapExprs),"value").
            getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();
    }

    public abstract Type getType();

    @GenericLazy
    public Type getInterfaceType(T propertyInterface) {
        return getQuery("value").getKeyType(propertyInterface);
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    protected abstract <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier);
    public <U extends Changes<U>> U aspectGetUsedChanges(Modifier<U> modifier) {
        return modifier.used(this, calculateUsedChanges(modifier));
    }
    public <U extends Changes<U>> U getUsedChanges(Modifier<U> modifier) {
        return aspectGetUsedChanges(modifier);
    }
    public boolean hasChanges(Modifier<? extends Changes> modifier) {
        return getUsedChanges(modifier).hasChanges();
    }

    @GenericLazy
    <JV> Query<T,JV> getQuery(JV value) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return new Query<T,JV>(mapKeys, getExpr(mapKeys),value);
    }

    public boolean isObject() {
        return true;
    }

    public PropertyField field;

    protected abstract Map<T, ValueClass> getMapClasses();
    protected abstract ClassWhere<Field> getClassWhere(PropertyField storedField);

    public boolean cached = false;

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent
    public void markStored(TableFactory tableFactory) {
        mapTable = tableFactory.getMapTable(getMapClasses());

        PropertyField storedField = new PropertyField(sID,getType());
        mapTable.table.addField(storedField, getClassWhere(storedField));

        // именно после так как высчитали, а то сама себя stored'ом считать будет
        field = storedField;

        assert !cached;
    }
    public boolean isStored() {
        return field !=null && (!DataSession.reCalculateAggr || this instanceof StoredDataProperty); // для тестирования 2-е условие
    }

    public boolean isFalse = false;
    public boolean checkChange = true;

    public PropertyMapImplement<?,T> getChangeImplement() {
        return new PropertyMapImplement<T,T>(this, BaseUtils.toMap(new HashSet<T>(interfaces)));
    }

    public Object read(SQLSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        String readValue = "readvalue";
        Query<T,Object> readQuery = new Query<T, Object>(this);

        readQuery.putKeyWhere(keys);

        readQuery.properties.put(readValue, getExpr(readQuery.mapKeys,modifier,null));
        return BaseUtils.singleValue(readQuery.execute(session)).get(readValue);
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier<? extends Changes> modifier) throws SQLException {
        return session.getObjectValue(read(session,keys,modifier),getType());
    }

    public Expr getIncrementExpr(Map<KeyField, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, ? extends Expr> joinKeys = BaseUtils.join(mapTable.mapKeys,joinImplement);
        WhereBuilder incrementWhere = new WhereBuilder();
        Expr incrementExpr = getExpr(joinKeys, modifier, incrementWhere);
        changedWhere.add(incrementWhere.toWhere().and(incrementExpr.getWhere().or(getExpr(joinKeys).getWhere()))); // если старые или новые изменились
        return incrementExpr;
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean usePreviousStored();

    @GenericLazy
    public <P extends PropertyInterface> MaxChangeProperty<T,P> getMaxChangeProperty(Property<P> change) {
        return new MaxChangeProperty<T,P>(this,change);
    }

    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        return modifier.newChanges();
    }
    
    public MapDataChanges<T> getDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return new MapDataChanges<T>();
    }

    public Map<T,Expr> getChangeExprs() {
        Map<T,Expr> result = new HashMap<T, Expr>();
        for(T propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.changeExpr);
        return result;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public final Expr changeExpr;

    private DataChanges getDataChanges(Modifier<? extends Changes> modifier, boolean toNull) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<T>(mapKeys,toNull?CaseExpr.NULL: changeExpr,CompareWhere.compare(mapKeys, getChangeExprs())),null, modifier).changes;
    }

    public Modifier<? extends Changes> getChangeModifier(Modifier<? extends Changes> modifier, boolean toNull) {
        // строим Where для изменения
        return new DataChangesModifier(modifier, getDataChanges(modifier,toNull));
    }

    public Collection<DataProperty> getDataChanges() { // не должно быть Action'ов
        return (Collection<DataProperty>)((Collection<? extends Property>) getDataChanges(defaultModifier, false).keys());
    }

    protected MapDataChanges<T> getJoinDataChanges(Map<T, ? extends Expr> implementExprs, Expr expr, Where where, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        MapDataChanges<T> result = getDataChanges(new PropertyChange<T>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, true, mapKeys),
                GroupExpr.create(implementExprs, ValueExpr.TRUE, where, true, mapKeys).getWhere()),
                changedImplementWhere, modifier);
        if(changedWhere!=null) changedWhere.add(new Query<T,Object>(mapKeys,changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    public PropertyMapImplement<T,T> getImplement() {
        return new PropertyMapImplement<T,T>(this, BaseUtils.toMap(new HashSet<T>(interfaces)));
    }

    public void setConstraint(boolean checkChange) {
        isFalse = true;
        this.checkChange = checkChange;                
    }

    // используется если создаваемый WhereBuilder нужен только если задан changed 
    public static WhereBuilder cascadeWhere(WhereBuilder changed) {
        return changed==null?null:new WhereBuilder();
    }

    public List<ClientAction> execute(Map<T, DataObject> keys, DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteFormView executeForm, Map<T, PropertyObjectInterface> mapObjects, GroupObjectImplement groupObject) throws SQLException {
        return getChangeImplement().execute(keys, session, value, modifier, executeForm, mapObjects, groupObject);
    }
}
