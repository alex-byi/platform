package lsfusion.server.logics.property.oraction;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.comb.ListPermutations;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.Settings;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;
import lsfusion.server.logics.classes.LogicalClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.event.ApplyGlobalEvent;
import lsfusion.server.logics.event.Link;
import lsfusion.server.logics.event.LinkType;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyClassImplement;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.*;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.infer.AlgType;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.struct.group.AbstractPropertyNode;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.logics.BusinessLogics.linkComparator;

public abstract class ActionOrProperty<T extends PropertyInterface> extends AbstractPropertyNode {
    public static final GetIndex<PropertyInterface> genInterface = new GetIndex<PropertyInterface>() {
        public PropertyInterface getMapValue(int i) {
            return new PropertyInterface(i);
        }};

    private int ID = 0;
    private String dbName;
    protected String canonicalName;
    public String annotation;

    private boolean local = false;
    
    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView
    public LocalizedString caption;

    public LocalizedString localizedToString() {
        LocalizedString result = LocalizedString.create(getSID());
        if (caption != null) {
            result = LocalizedString.concatList(result, " '", caption, "'");    
        }
        if (debugInfo != null) {
            result = LocalizedString.concat(result, " [" + debugInfo + "]");
        }
        return result;
    } 
    
    public String toString() {
        String result;
        if (canonicalName != null) {
            result = canonicalName;
        } else {
            String topName = getTopName();
            result = topName != null ? "at " + topName : getPID();
        }
        
        LocalizedString caption;
        if (this.caption != null && this.caption != LocalizedString.NONAME) {
            caption = this.caption;
        } else {
            caption = getTopCaption();
        }
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }

        if (debugInfo != null) {
            result += " [" + debugInfo + "]";
        }
        return result;
    }

    protected DebugInfo debugInfo;
    
    public abstract DebugInfo getDebugInfo();

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public ValueClass[] getInterfaceClasses(ImOrderSet<T> listInterfaces, ClassType classType) { // notification, load, lazy, dc, obsolete, в конструкторах при определении классов действий в основном
        return listInterfaces.mapList(getInterfaceClasses(classType)).toArray(new ValueClass[listInterfaces.size()]);
    }
    public abstract ImMap<T, ValueClass> getInterfaceClasses(ClassType type);

    public abstract boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny);

    public ActionOrProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
        this.interfaces = interfaces.getSet();
        this.orderInterfaces = interfaces;

        setContextMenuAction(ServerResponse.GROUP_CHANGE, LocalizedString.create("{logics.property.groupchange}"));

//        notFinalized.put(this, ExceptionUtils.getStackTrace());
    }

    public final ImSet<T> interfaces;
    private final ImOrderSet<T> orderInterfaces;
    protected ImOrderSet<T> getOrderInterfaces() {
        return orderInterfaces;
    }

    public int getInterfaceCount() {
        return interfaces.size();
    }
    
    public ImOrderSet<T> getReflectionOrderInterfaces() {
        return orderInterfaces;
    }
    
    public ImOrderSet<T> getFriendlyOrderInterfaces() { 
        return orderInterfaces; 
    }

    public Type getInterfaceType(T propertyInterface) {
        return getInterfaceType(propertyInterface, ClassType.materializeChangePolicy);
    }

    public Type getWhereInterfaceType(T propertyInterface) {
        return getInterfaceType(propertyInterface, ClassType.wherePolicy);
    }

    public Type getInterfaceType(T propertyInterface, ClassType classType) {
        ValueClass valueClass = getInterfaceClasses(classType).get(propertyInterface);
        return valueClass != null ? valueClass.getType() : null;
    }

    public abstract boolean isNotNull();

    public String getDBName() {
        return dbName;
    }

    public String getName() {
        if (isNamed()) {
            return PropertyCanonicalNameParser.getName(canonicalName);
        }
        return null;
    }

    public String getNamespace() {
        if (isNamed()) {
            return PropertyCanonicalNameParser.getNamespace(canonicalName);
        }
        return null;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String namespace, String name, List<ResolveClassSet> signature, ImOrderSet<T> signatureOrder, DBNamingPolicy policy) {
        assert name != null && namespace != null;
        this.canonicalName = PropertyCanonicalNameUtils.createName(namespace, name, signature);
        this.dbName = policy.transformPropertyCNToDBName(canonicalName);

        setExplicitClasses(signatureOrder, signature);
    }

    public void setCanonicalName(String canonicalName, DBNamingPolicy policy) {
        checkCanonicalName(canonicalName);
        this.canonicalName = canonicalName;
        this.dbName = policy.transformPropertyCNToDBName(canonicalName);
    }

    private void checkCanonicalName(String canonicalName) {
        assert canonicalName != null;
        PropertyCanonicalNameParser.getName(canonicalName);
        PropertyCanonicalNameParser.getNamespace(canonicalName);
    }

    final public boolean isNamed() {
        return canonicalName != null;
    }

    // для всех    
    private String mouseBinding;
    private Object keyBindings;
    private Object contextMenuBindings;
    private Object editActions;

    public void setMouseAction(String actionSID) {
        setMouseBinding(actionSID);
    }

    public void setMouseBinding(String mouseBinding) {
        this.mouseBinding = mouseBinding;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = MapFact.mMap(MapFact.override());
        }
        ((MMap<KeyStroke, String>)keyBindings).add(ks, actionSID);
    }

    public String getMouseBinding() {
        return mouseBinding;
    }

    public ImMap<KeyStroke, String> getKeyBindings() {
        return (ImMap<KeyStroke, String>)(keyBindings == null ? MapFact.EMPTY() : keyBindings);
    }

    @NFLazy
    public void setContextMenuAction(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null || contextMenuBindings instanceof EmptyOrderMap) {
            contextMenuBindings = MapFact.mOrderMap(MapFact.override());
        }
        ((MOrderMap<String, LocalizedString>)contextMenuBindings).add(actionSID, caption);
    }

    public ImOrderMap<String, LocalizedString> getContextMenuBindings() {
        return (ImOrderMap<String, LocalizedString>)(contextMenuBindings == null ? MapFact.EMPTYORDER() : contextMenuBindings);
    }

    @NFLazy
    public void setEditAction(String editActionSID, ActionMapImplement<?, T> editActionImplement) {
        if (editActions == null || editActions instanceof EmptyRevMap) {
            editActions = MapFact.mMap(MapFact.override());
        }
        ((MMap<String, ActionMapImplement<?, T>>)editActions).add(editActionSID, editActionImplement);
    }

    @LongMutable
    private ImMap<String, ActionMapImplement<?, T>> getEditActions() {
        return (ImMap<String, ActionMapImplement<?, T>>)(editActions == null ? MapFact.EMPTY() : editActions);
    }

    public ActionMapImplement<?, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    public ActionMapImplement<?, T> getEditAction(String editActionSID, Property filterProperty) {
        ActionMapImplement<?, T> editAction = getEditActions().get(editActionSID);
        if (editAction != null) {
            return editAction;
        }

        if(GROUP_CHANGE.equals(editActionSID))
            return null;

        assert CHANGE.equals(editActionSID) || CHANGE_WYS.equals(editActionSID) || EDIT_OBJECT.equals(editActionSID);

        return getDefaultEditAction(editActionSID, filterProperty);
    }

    public abstract ActionMapImplement<?, T> getDefaultEditAction(String editActionSID, Property filterProperty);

    public boolean checkEquals() {
        return this instanceof Property;
    }

    public ImRevMap<T, T> getIdentityInterfaces() {
        return interfaces.toRevMap();
    }

    public boolean hasChild(ActionOrProperty prop) {
        return prop.equals(this);
    }

    public boolean hasNFChild(ActionOrProperty prop, Version version) {
        return hasChild(prop);
    }
    
    public ImOrderSet<ActionOrProperty> getProperties() {
        return SetFact.singletonOrder((ActionOrProperty) this);
    }
    
    public static void cleanPropCaches() {
        hashProps.clear();
    }

    private static class CacheEntry {
        private final ActionOrProperty property;
        private final ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses;

        private ImList<PropertyClassImplement> result;
        
        public CacheEntry(ActionOrProperty property, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses) {
            this.property = property;
            this.mapClasses = mapClasses;
        }

        public ImRevMap<ValueClassWrapper, ValueClassWrapper> map(CacheEntry entry) {
            if(!(mapClasses.size() == entry.mapClasses.size() && BaseUtils.hashEquals(property, entry.property)))
                return null;

            MRevMap<ValueClassWrapper, ValueClassWrapper> mResult = MapFact.mRevMap();
            for(int i=0,size=mapClasses.size();i<size;i++) {
                ImSet<ValueClassWrapper> wrappers = mapClasses.getValue(i);
                ImSet<ValueClassWrapper> entryWrappers = entry.mapClasses.get(mapClasses.getKey(i));
                if(entryWrappers == null || wrappers.size() != entryWrappers.size())
                    return null;
                for(int j=0,sizeJ=wrappers.size();j<sizeJ;j++)
                    mResult.revAdd(wrappers.get(j), entryWrappers.get(j));
            }
            return mResult.immutableRev();
        }
        
        public int hash() {
            int result = 0;
            for(int i=0,size=mapClasses.size();i<size;i++) {
                result += mapClasses.getKey(i).hashCode() ^ mapClasses.getValue(i).size();
            }
            
            return 31 * result + property.hashCode();
        }
    }    
    final static LRUSVSMap<Integer, MAddCol<CacheEntry>> hashProps = new LRUSVSMap<>(LRUUtil.G2);

    // вся оптимизация в общем то для drillDown
    protected ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, Version version) {
        if(valueClasses.size() == 1) { // доп оптимизация для DrillDown
            if(interfaces.size() == 1 && isInInterface(MapFact.singleton(interfaces.single(), valueClasses.single().valueClass.getUpSet()), true))
                return ListFact.<PropertyClassImplement>singleton(createClassImplement(valueClasses.toOrderSet(), SetFact.singletonOrder(interfaces.single())));
            return ListFact.EMPTY();
        }

        CacheEntry entry = new CacheEntry(this, mapClasses); // кэширование
        int hash = entry.hash();
        MAddCol<CacheEntry> col = hashProps.get(hash);
        if(col == null) {
            col = ListFact.mAddCol();
            hashProps.put(hash, col);                    
        } else {
            synchronized (col) {
                for (CacheEntry cachedEntry : col.it()) {
                    final ImRevMap<ValueClassWrapper, ValueClassWrapper> map = cachedEntry.map(entry);
                    if (map != null) {
                        return cachedEntry.result.mapListValues(new GetValue<PropertyClassImplement, PropertyClassImplement>() {
                            public PropertyClassImplement getMapValue(PropertyClassImplement value) {
                                return value.map(map);
                            }
                        });
                    }
                }
            }
        }
        
        ImList<PropertyClassImplement> result = getProperties(FormEntity.getSubsets(valueClasses));
        
        entry.result = result;
        synchronized (col) {
            col.add(entry);
        }
        
        return result;
    }
    
    private ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : classLists) {
            if (interfaces.size() == classes.size()) {
                final ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
                for (ImOrderSet<T> mapping : new ListPermutations<>(getOrderInterfaces())) {
                    ImMap<T, AndClassSet> propertyInterface = mapping.mapOrderValues(new GetIndexValue<AndClassSet, T>() {
                        public AndClassSet getMapValue(int i, T value) {
                            return orderClasses.get(i).valueClass.getUpSet();
                        }});
                    if (isInInterface(propertyInterface, true)) {
                        mResultList.add(createClassImplement(orderClasses, mapping));
                    }
                }
            }
        }
        return mResultList.immutableList();
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping);

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
    }

//    private static ConcurrentHashMap<Property, String> notFinalized = new ConcurrentHashMap<Property, String>();

    public void finalizeAroundInit() {
        super.finalizeAroundInit();

//        notFinalized.remove(this);
        
        editActions = editActions == null ? MapFact.EMPTY() : ((MMap)editActions).immutable();
        keyBindings = keyBindings == null ? MapFact.EMPTY() : ((MMap)keyBindings).immutable();
        contextMenuBindings = contextMenuBindings == null ? MapFact.EMPTYORDER() : ((MOrderMap)contextMenuBindings).immutableOrder();
    }

    public void prereadCaches() {
        getInterfaceClasses(ClassType.strictPolicy);
        getInterfaceClasses(ClassType.signaturePolicy);
    }

    protected abstract ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events);

    private ImOrderSet<Link> links;
    @ManualLazy
    public ImOrderSet<Link> getSortedLinks(boolean events) { // чисто для лексикографики
        if(links==null) {
            links = calculateLinks(events).mapMergeSetValues(new GetValue<Link, Pair<ActionOrProperty<?>, LinkType>>() {
                public Link getMapValue(Pair<ActionOrProperty<?>, LinkType> value) {
                    return new Link(ActionOrProperty.this, value.first, value.second);
                }}).sortSet(linkComparator); // sorting for determenism, no need to cache because it's called once for each property
        }
        return links;
    }
    public void dropLinks() {
        links = null;
    }
    public abstract ImSet<SessionProperty> getSessionCalcDepends(boolean events);

    public abstract ImSet<OldProperty> getParseOldDepends(); // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики

    public ImSet<OldProperty> getOldDepends() {
        // без событий, так как либо используется в глобальных событиях когда вычисляемые события \ удаления отдельно отрабатываются
        // в локальных же событиях вычисляемые и должны браться на начало сессии
        return getSessionCalcDepends(false).mapMergeSetValues(new GetValue<OldProperty, SessionProperty>() {
            public OldProperty getMapValue(SessionProperty value) {
                return value.getOldProperty();
            }});
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list) {
        return getOrderInterfaces().mapOrderRevValues(new GetIndexValue<V, T>() {
            public V getMapValue(int i, T value) {
                return list.get(i);
            }
        });
    }

    public boolean drillDownInNewSession() {
        return false;
    }

    public ActionOrProperty showDep; // assert что не null когда events не isEmpty

    protected static <T extends PropertyInterface> ImMap<T, ResolveClassSet> getPackedSignature(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        return interfaces.mapList(ListFact.fromJavaList(signature)).removeNulls();
    }

    public List<ResolveClassSet> getExplicitClasses(ImOrderSet<T> interfaces) {
        if(explicitClasses == null)
            return null;
        return interfaces.mapList(explicitClasses).toJavaList();
    }

    public void setExplicitClasses(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        this.explicitClasses = getPackedSignature(interfaces, signature);
    }
    
    public String getPID() {
        return "p" + ID;
    }
    
    public String getSID() {
        return canonicalName != null ? canonicalName : getPID(); 
    }
    
    public String getTopName() {
        if (debugInfo != null) {
            return debugInfo.getTopName();
        }
        return null;
    }
    
    public LocalizedString getTopCaption() {
        if (debugInfo != null) {
            return debugInfo.getTopCaption();
        }
        return null;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    protected ImMap<T, ResolveClassSet> explicitClasses; // без nulls

    protected interface Checker<V> {
        boolean checkEquals(V expl, V calc);
    }

    //
    protected static <T, V> ImMap<T, V> getExplicitCalcInterfaces(ImSet<T> interfaces, ImMap<T, V> explicitInterfaces, Callable<ImMap<T,V>> calcInterfaces, String caption, ActionOrProperty property, Checker<V> checker) {
        
        ImMap<T, V> inferred = null;
        if (explicitInterfaces != null)
            inferred = explicitInterfaces;

        if (inferred == null || inferred.size() < interfaces.size() || AlgType.checkExplicitInfer) {
            try {
                ImMap<T, V> calcInferred = calcInterfaces.call();
                if (calcInferred == null) {
                    return null;
                }
                if (inferred == null)
                    inferred = calcInferred;
                else {
                    if (AlgType.checkExplicitInfer) checkExplicitCalcInterfaces(checker, caption + property, inferred, calcInferred);
                    inferred = calcInferred.override(inferred); // тут возможно replaceValues достаточно, но не так просто оценить
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return inferred;
    }

    private static  <T, V> boolean checkExplicitCalcInterfaces(Checker<V> checker, String caption, ImMap<T, V> inferred, ImMap<T, V> calcInferred) {
        for(int i=0, size = inferred.size(); i<size; i++) {
            T key = inferred.getKey(i);
            V calcValue = calcInferred.get(key);
            V inferValue = inferred.getValue(i);
            if((calcValue != null || inferValue != null) && (calcValue == null || inferValue == null || !checker.checkEquals(calcValue, inferValue))) {
                System.out.println(caption + ", CALC : " + calcInferred + ", INF : " + inferred);
                return false;
            }
        }
        return true;
    }

    public String getChangeExtSID() {
        return null;
    }

    public void inheritCaption(ActionOrProperty property) {
        caption = property.caption;         
    }
    
    public interface DefaultProcessor {
        // из-за inherit entity и view могут быть другого свойства
        void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form);
        void proceedDefaultDesign(PropertyDrawView propertyView);
    }

    // + caption, который одновременно и draw и не draw
    public static class DrawOptions {
        
        // свойства, но пока реализовано как для всех
        private int charWidth;
        private Dimension valueSize;
        private Boolean valueFlex;

        // свойства, но пока реализовано как для всех
        private String regexp;
        private String regexpMessage;
        private Boolean echoSymbols;

        // действия, но пока реализовано как для всех
        private Boolean askConfirm;
        private String askConfirmMessage;

        // свойства, но пока реализовано как для всех
        private String eventID;

        // для всех
        private ImageIcon image;
        private String iconPath;

        // для всех
        private Compare defaultCompare;

        // для всех
        private KeyStroke changeKey;
        private Boolean showChangeKey;

        // для всех
        private Boolean shouldBeLast;

        // для всех
        private ClassViewType forceViewType;
        
        // для всех 
        private ImList<DefaultProcessor> processors = ListFact.EMPTY();
        
        public void proceedDefaultDraw(PropertyDrawEntity<?> entity, FormEntity form) {
            entity.shouldBeLast = BaseUtils.nvl(shouldBeLast, false);
            entity.forceViewType = forceViewType;
            entity.askConfirm = BaseUtils.nvl(askConfirm, false);
            entity.askConfirmMessage = askConfirmMessage;
            entity.eventID = eventID;

            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDraw(entity, form);
        }

        public void proceedDefaultDesign(PropertyDrawView propertyView) {
            if(propertyView.isCalcProperty()) {
                if (propertyView.getType() instanceof LogicalClass) 
                    propertyView.editOnSingleClick = Settings.get().getEditLogicalOnSingleClick();
            } else
                propertyView.editOnSingleClick = Settings.get().getEditActionOnSingleClick();

            if(propertyView.getCharWidth() == 0)
                propertyView.setCharWidth(charWidth);
            if(propertyView.getValueFlex() == null)
                propertyView.setValueFlex(valueFlex);
            if(propertyView.getValueSize() == null)
                propertyView.setValueSize(valueSize);
            if (propertyView.design.imagePath == null && iconPath != null) {
                propertyView.design.imagePath = iconPath;
                propertyView.design.setImage(image);
            }
            if (propertyView.changeKey == null)
                propertyView.changeKey = changeKey;
            if (propertyView.showChangeKey == null)
                propertyView.showChangeKey = BaseUtils.nvl(showChangeKey, true);
            if (propertyView.regexp == null)
                propertyView.regexp = regexp;
            if (propertyView.regexpMessage == null)
                propertyView.regexpMessage = regexpMessage;
            if (propertyView.echoSymbols == null)
                propertyView.echoSymbols = BaseUtils.nvl(echoSymbols, false);
            
            if(propertyView.defaultCompare == null)
                propertyView.defaultCompare = defaultCompare;

            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDesign(propertyView);
        }
        
        public void inheritDrawOptions(DrawOptions options) {
            if(charWidth == 0)
                setCharWidth(options.charWidth);

            if(iconPath == null) {
                setImage(options.image);
                setIconPath(options.iconPath);
            }

            if(defaultCompare == null)
                setDefaultCompare(options.defaultCompare);

            if(regexp == null)
                setRegexp(options.regexp);
            if(regexpMessage == null)
                setRegexpMessage(options.regexpMessage);
            if(echoSymbols == null)
                setEchoSymbols(options.echoSymbols);
            
            if(askConfirm == null)
                setAskConfirm(options.askConfirm);
            if(askConfirmMessage == null)
                setAskConfirmMessage(options.askConfirmMessage);
            
            if(eventID == null)
                setEventID(options.eventID);
            
            if(changeKey == null)
                setChangeKey(options.changeKey);
            if(showChangeKey == null)
                setShowChangeKey(options.showChangeKey);
            
            if(shouldBeLast == null)
                setShouldBeLast(options.shouldBeLast);
            
            if(forceViewType == null)
                setForceViewType(options.forceViewType);
            
            processors = options.processors.addList(processors);
        }

        // setters
        
        public void addProcessor(DefaultProcessor processor) {
            processors = processors.addList(processor);
        }

        public void setFixedCharWidth(int charWidth) {
            setCharWidth(charWidth);
            setValueFlex(false);
        }

        public void setImage(String iconPath) {
            this.setIconPath(iconPath);
            setImage(new ImageIcon(ActionOrProperty.class.getResource("/images/" + iconPath)));
        }

        public Compare getDefaultCompare() {
            return defaultCompare;
        }

        public void setDefaultCompare(String defaultCompare) {
            this.defaultCompare = ActionOrPropertyUtils.stringToCompare(defaultCompare);
        }

        public void setDefaultCompare(Compare defaultCompare) {
            this.defaultCompare = defaultCompare;
        }


        public void setCharWidth(int charWidth) {
            this.charWidth = charWidth;
        }
        public void setValueFlex(Boolean flex) {
            this.valueFlex = flex;
        }

        public void setRegexp(String regexp) {
            this.regexp = regexp;
        }

        public void setRegexpMessage(String regexpMessage) {
            this.regexpMessage = regexpMessage;
        }

        public void setEchoSymbols(Boolean echoSymbols) {
            this.echoSymbols = echoSymbols;
        }

        public void setAskConfirm(Boolean askConfirm) {
            this.askConfirm = askConfirm;
        }

        public void setAskConfirmMessage(String askConfirmMessage) {
            this.askConfirmMessage = askConfirmMessage;
        }

        public void setEventID(String eventID) {
            this.eventID = eventID;
        }

        public void setImage(ImageIcon image) {
            this.image = image;
        }

        public void setIconPath(String iconPath) {
            this.iconPath = iconPath;
        }

        public void setChangeKey(KeyStroke changeKey) {
            this.changeKey = changeKey;
        }

        public void setShowChangeKey(Boolean showEditKey) {
            this.showChangeKey = showEditKey;
        }

        public void setShouldBeLast(Boolean shouldBeLast) {
            this.shouldBeLast = shouldBeLast;
        }

        public void setForceViewType(ClassViewType forceViewType) {
            this.forceViewType = forceViewType;
        }
    }

    public DrawOptions drawOptions = new DrawOptions();
    
    protected ApplyGlobalEvent event;
    // важно кэшировать так как equals'ов пока нет, а они важны (в общем то только для Stored, и для RemoveClasses )
    public ApplyGlobalEvent getApplyEvent() {
        return null;        
    }
}
