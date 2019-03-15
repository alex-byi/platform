package lsfusion.server.logics;

import com.google.common.collect.Iterables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.GlobalVersion;
import lsfusion.server.base.version.LastVersion;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.Time;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.EvalActionProperty;
import lsfusion.server.language.LazyActionProperty;
import lsfusion.server.language.MetaCodeFragment;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.linear.LA;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.change.AddObjectAction;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.interactive.ConfirmActionProperty;
import lsfusion.server.logics.action.interactive.MessageAction;
import lsfusion.server.logics.action.session.ApplyAction;
import lsfusion.server.logics.action.session.CancelActionProperty;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.NewSessionActionProperty;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.constraint.OutFormSelector;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.interactive.GroupObjectProp;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.focus.FocusActionProperty;
import lsfusion.server.logics.form.interactive.action.input.InputActionProperty;
import lsfusion.server.logics.form.interactive.action.input.RequestAction;
import lsfusion.server.logics.form.interactive.action.seek.SeekGroupObjectActionProperty;
import lsfusion.server.logics.form.interactive.action.seek.SeekObjectActionProperty;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.interactive.FormInteractiveActionProperty;
import lsfusion.server.logics.form.open.stat.ExportActionProperty;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.open.stat.PrintActionProperty;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.IntegrationFormEntity;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.json.ExportJSONActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.xml.ExportXMLActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.csv.ExportCSVActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.dbf.ExportDBFActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.table.ExportTableActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.xls.ExportXLSActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.json.ImportJSONActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.xml.ImportXMLActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.csv.ImportCSVActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.dbf.ImportDBFActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.table.ImportTableActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.xls.ImportXLSActionProperty;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.DefaultIcon;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.data.*;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.implement.*;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyUtils;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.*;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.drilldown.DrillDownFormEntity;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.ActionPropertyDebugger;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.ClassCanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.resolve.ResolveManager;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors;
import lsfusion.server.physics.dev.integration.internal.to.StringFormulaProperty;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static lsfusion.base.BaseUtils.add;
import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createStatic;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.*;

// modules logics in theory should be in dev.module.package but in this class it's more about logics, than about modularity
public abstract class LogicsModule {
    protected static final Logger logger = Logger.getLogger(LogicsModule.class);

    protected static final ActionPropertyDebugger debugger = ActionPropertyDebugger.getInstance();

    // после этого шага должны быть установлены name, namespace, requiredModules
    public abstract void initModuleDependencies() throws RecognitionException;

    public abstract void initMetaAndClasses() throws RecognitionException;

    public abstract void initTables() throws RecognitionException;

    public abstract void initMainLogic() throws FileNotFoundException, RecognitionException;

    public abstract void initIndexes() throws RecognitionException;

    public String getErrorsDescription() { return "";}

    public BaseLogicsModule baseLM;

    protected Map<String, List<LP<?>>> namedProperties = new HashMap<>();
    protected Map<String, List<LA<?>>> namedActions = new HashMap<>();
    
    protected final Map<String, AbstractGroup> groups = new HashMap<>();
    protected final Map<String, CustomClass> classes = new HashMap<>();
    protected final Map<String, AbstractWindow> windows = new HashMap<>();
    protected final Map<String, NavigatorElement> navigatorElements = new HashMap<>();
    protected final Map<String, FormEntity> namedForms = new HashMap<>();
    protected final Map<String, ImplementTable> tables = new HashMap<>();
    protected final Map<Pair<String, Integer>, MetaCodeFragment> metaCodeFragments = new HashMap<>();

    private final Set<FormEntity> unnamedForms = new HashSet<>();
    private final Map<LP<?>, LocalPropertyData> locals = new HashMap<>();


    protected final Map<LAP<?, ?>, List<ResolveClassSet>> propClasses = new HashMap<>();
    

    protected Map<String, List<LogicsModule>> namespaceToModules = new LinkedHashMap<>();
    
    protected ResolveManager resolveManager;
    
    protected LogicsModule() {
        resolveManager = new ResolveManager(this);        
    }

    public LogicsModule(String name, String namespace, LinkedHashSet<String> requiredNames) {
        this();
        this.name = name;
        this.namespace = namespace;
        this.requiredNames = requiredNames;
    }

    private String name;
    private String namespace;
    private LinkedHashSet<String> requiredNames;
    private List<String> namespacePriority;
    private boolean defaultNamespace;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected String elementCanonicalName(String name) {
        return CanonicalNameUtils.createCanonicalName(getNamespace(), name);
    }
    
    public String getLogName(int moduleCount, int orderNum) {
        String result = "#" + orderNum + " of " + moduleCount + " " + name;
        if (order != null)
            result += " (actual: " + (order + 1) + ")";
        return result;
    }

    public Iterable<LP<?>> getNamedProperties() {
        return Iterables.concat(namedProperties.values());
    }

    public Iterable<LA<?>> getNamedActions() {
        return Iterables.concat(namedActions.values());
    }

    public Iterable<LP<?>> getNamedProperties(String name) {
        return createEmptyIfNull(namedProperties.get(name));
    }

    public Iterable<LA<?>> getNamedActions(String name) {
        return createEmptyIfNull(namedActions.get(name));
    }

    public Collection<CustomClass> getClasses() {
        return classes.values();
    }
    
    private <T extends LAP<?, ?>> Iterable<T> createEmptyIfNull(Collection<T> col) {
        if (col == null) {
            return Collections.emptyList();
        } else {
            return col;
        }
    }
    
    protected void addModuleLP(LAP<?, ?> lp) {
        String name = null;
        assert getNamespace().equals(lp.property.getNamespace());
        if (lp instanceof LA) {
            name = ((LA<?>)lp).property.getName();
            putLPToMap(namedActions, (LA) lp, name);
        } else if (lp instanceof LP) {
            name = ((LP<?>)lp).property.getName();
            putLPToMap(namedProperties, (LP)lp, name);
        }
        assert name != null;
    }

    private <T extends LAP<?, ?>> void putLPToMap(Map<String, List<T>> moduleMap, T lp, String name) {
        if (!moduleMap.containsKey(name)) {
            moduleMap.put(name, new ArrayList<T>());
        }
        moduleMap.get(name).add(lp);
    }

    @NFLazy
    protected <P extends PropertyInterface, T extends LAP<P, ?>> void makeActionOrPropertyPublic(T lp, String name, List<ResolveClassSet> signature) {
        lp.property.setCanonicalName(getNamespace(), name, signature, lp.listInterfaces, baseLM.getDBNamingPolicy());
        propClasses.put(lp, signature);
        addModuleLP(lp);
    }

    protected void makePropertyPublic(LP<?> lp, String name, ResolveClassSet... signature) {
        makePropertyPublic(lp, name, Arrays.asList(signature));
    }
    
    protected void makeActionPublic(LA<?> lp, String name, ResolveClassSet... signature) {
        makeActionPublic(lp, name, Arrays.asList(signature));
    }

    protected <P extends PropertyInterface> void makePropertyPublic(LP<P> lp, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(lp, name, signature);
    }
    
    protected <P extends PropertyInterface> void makeActionPublic(LA<P> lp, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(lp, name, signature);
    }

    public AbstractGroup getGroup(String name) {
        return groups.get(name);
    }

    protected void addGroup(AbstractGroup group) {
        assert !groups.containsKey(group.getName());
        groups.put(group.getName(), group);
    }

    public CustomClass getClass(String name) {
        return classes.get(name);
    }

    protected void addModuleClass(CustomClass valueClass) {
        assert !classes.containsKey(valueClass.getName());
        classes.put(valueClass.getName(), valueClass);
    }

    public ImplementTable getTable(String name) {
        return tables.get(name);
    }

    protected void addModuleTable(ImplementTable table) {
        // В классе Table есть метод getName(), который используется для других целей, в частности
        // в качестве имени таблицы в базе данных, поэтому пока приходится использовать отличный от
        // остальных элементов системы способ получения простого имени 
        String name = CanonicalNameUtils.getName(table.getCanonicalName());
        assert !tables.containsKey(name);
        tables.put(name, table);
    }

    protected <T extends AbstractWindow> T addWindow(T window) {
        assert !windows.containsKey(window.getName());
        windows.put(window.getName(), window);
        return window;
    }

    public AbstractWindow getWindow(String name) {
        return windows.get(name);
    }

    public MetaCodeFragment getMetaCodeFragment(String name, int paramCnt) {
        return metaCodeFragments.get(new Pair<>(name, paramCnt));
    }

    protected void addMetaCodeFragment(MetaCodeFragment fragment) {
        assert !metaCodeFragments.containsKey(new Pair<>(fragment.getName(), fragment.parameters.size()));
        metaCodeFragments.put(new Pair<>(fragment.getName(), fragment.parameters.size()), fragment);
    }

    protected void setBaseLogicsModule(BaseLogicsModule baseLM) {
        this.baseLM = baseLM;
    }

    public FunctionSet<Version> visible;
    public Integer order;
    public boolean temporary;
    private final Version version = new Version() {
        public boolean canSee(Version version) {
            assert !(version instanceof LastVersion);
            return version instanceof GlobalVersion || visible.contains(version);
        }

        public Integer getOrder() {
            return order;
        }

        public int compareTo(Version o) {
            return getOrder().compareTo(o.getOrder());
        }

        public boolean isTemporary() {
            return temporary;
        }
    };
    public Version getVersion() {
        return version;
    }
    
    protected AbstractGroup addAbstractGroup(String name, LocalizedString caption, AbstractGroup parent) {
        return addAbstractGroup(name, caption, parent, true);
    }

    protected AbstractGroup addAbstractGroup(String name, LocalizedString caption, AbstractGroup parent, boolean toCreateContainer) {
        AbstractGroup group = new AbstractGroup(elementCanonicalName(name), caption);
        Version version = getVersion();
        if (parent != null) {
            parent.add(group, version);
        } else {
            if (baseLM.privateGroup != null && !temporary)
                baseLM.privateGroup.add(group, version);
        }
        group.system = !toCreateContainer;
        addGroup(group);
        return group;
    }

    protected void storeCustomClass(CustomClass customClass) {
        addModuleClass(customClass);
    }

    protected BaseClass addBaseClass(String canonicalName, LocalizedString caption, String staticCanonicalName, LocalizedString staticCanonicalCaption) {
        BaseClass baseClass = new BaseClass(canonicalName, caption, staticCanonicalName, staticCanonicalCaption, getVersion());
        storeCustomClass(baseClass);
        storeCustomClass(baseClass.staticObjectClass);
        return baseClass;
    }

    protected static void printStaticObjectsChanges(String path, String staticName, List<String> sids) {
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path, true));
            for (String sid : sids) {
                w.print("OBJECT " + sid + " -> " + staticName + "." + sid + "\n");
            }
            w.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ConcreteCustomClass addConcreteClass(String name, LocalizedString caption, List<String> objNames, List<LocalizedString> objCaptions, ImList<CustomClass> parents) {
        if(!objNames.isEmpty())
            parents = parents.addList(getBaseClass().staticObjectClass);
        parents = checkEmptyParents(parents);
        ConcreteCustomClass customClass = new ConcreteCustomClass(elementCanonicalName(name), caption, getVersion(), parents);
        customClass.addStaticObjects(objNames, objCaptions, getVersion());
        storeCustomClass(customClass);
        return customClass;
    }

    private ImList<CustomClass> checkEmptyParents(ImList<CustomClass> parents) {
        if(parents.isEmpty())
            parents = ListFact.<CustomClass>singleton(getBaseClass());
        return parents;
    }

    protected AbstractCustomClass addAbstractClass(String name, LocalizedString caption, ImList<CustomClass> parents) {
        parents = checkEmptyParents(parents);
        AbstractCustomClass customClass = new AbstractCustomClass(elementCanonicalName(name), caption, getVersion(), parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected ImplementTable addTable(String name, boolean isFull, boolean isExplicit, ValueClass... classes) {
        String canonicalName = elementCanonicalName(name);
        ImplementTable table = baseLM.tableFactory.include(CanonicalNameUtils.toSID(canonicalName), getVersion(), classes);
        table.setCanonicalName(canonicalName);
        addModuleTable(table);
        
        if(isFull) {
            if(classes.length == 1)
                table.markedFull = true;
            else
                markFull(table, classes);
        } else
            table.markedExplicit = isExplicit;
        return table;
    }

    protected void markFull(ImplementTable table, ValueClass... classes) {
        // создаем IS
        ImList<ValueClass> listClasses = ListFact.toList(classes);
        PropertyRevImplement<?, Integer> mapProperty = IsClassProperty.getProperty(listClasses.toIndexedMap()); // тут конечно стремновато из кэша брать, так как остальные гарантируют создание
        LP<?> lcp = addJProp(mapProperty.createLP(ListFact.consecutiveList(listClasses.size(), 0)), ListFact.consecutiveList(listClasses.size()).toArray(new Integer[listClasses.size()]));
//        addProperty(null, lcp);

        // делаем public, persistent
        makePropertyPublic(lcp, PropertyCanonicalNameUtils.fullPropPrefix + table.getName(), ClassCanonicalNameUtils.getResolveList(classes));
        addPersistent(lcp, table);

        // помечаем fullField из помеченного свойства
        table.setFullField(lcp.property.field);
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    // ------------------- DATA ----------------- //

    protected LP addDProp(LocalizedString caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(caption, params, value);
        LP lp = addProperty(null, new LP<>(dataProperty));
        return lp;
    }

    // ------------------- Loggable ----------------- //

    protected <D extends PropertyInterface> LP addDCProp(LocalizedString caption, int whereNum, LP<D> derivedProp, Object... params) {
        Pair<ValueClass[], ValueClass> signature = getSignature(derivedProp, whereNum, params);

        // выполняем само создание свойства
        StoredDataProperty dataProperty = new StoredDataProperty(caption, signature.first, signature.second);
        LP derDataProp = addProperty(null, new LP<>(dataProperty));

        derDataProp.setEventChange(derivedProp, whereNum, params);
        return derDataProp;
    }

    protected <D extends PropertyInterface> LP addLogProp(LocalizedString caption, int whereNum, LP<D> derivedProp, Object... params) {
        Pair<ValueClass[], ValueClass> signature = getSignature(derivedProp, whereNum, params);

        // выполняем само создание свойства
        StoredDataProperty dataProperty = new StoredDataProperty(caption, signature.first, LogicalClass.instance);
        return addProperty(null, new LP<>(dataProperty));
    }

    private <D extends PropertyInterface> Pair<ValueClass[], ValueClass> getSignature(LP<D> derivedProp, int whereNum, Object[] params) {
        // придется создавать Join свойство чтобы считать его класс
        int dersize = getIntNum(params);
        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(dersize);

        final ImList<PropertyInterfaceImplement<JoinProperty.Interface>> list = readCalcImplements(listInterfaces, params);

        assert whereNum == list.size() - 1; // один ON CHANGED, то есть union делать не надо (выполняется, так как только в addLProp работает)

        AndFormulaProperty andProperty = new AndFormulaProperty(list.size());
        ImMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>addExcl(
                        andProperty.andInterfaces.mapValues(new GetIndex<PropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public PropertyInterfaceImplement<JoinProperty.Interface> getMapValue(int i) {
                                return list.get(i);
                            }
                        }), andProperty.objectInterface, mapCalcListImplement(derivedProp, listInterfaces));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<>(LocalizedString.NONAME, listInterfaces,
                new PropertyImplement<>(andProperty, mapImplement));
        LP<JoinProperty.Interface> listProperty = new LP<>(joinProperty, listInterfaces);

        // получаем классы
        ValueClass[] commonClasses = listProperty.getInterfaceClasses(ClassType.logPolicy); // есть и другие obsolete использования
        ValueClass valueClass = listProperty.property.getValueClass(ClassType.logPolicy);
        return new Pair<>(commonClasses, valueClass);
    }

    // ------------------- Scripted DATA ----------------- //

    protected LP addSDProp(LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        return addSDProp(null, false, caption, isLocalScope, value, nestedType, params);
    }

    protected LP addSDProp(AbstractGroup group, boolean persistent, LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        SessionDataProperty prop = new SessionDataProperty(caption, params, value);
        if (isLocalScope) {
            prop.setLocal(true);
        }
        prop.nestedType = nestedType;
        return addProperty(group, new LP<>(prop));
    }

    // ------------------- Form actions ----------------- //


    protected LA addFormAProp(LocalizedString caption, CustomClass cls, LA action) {
        return addIfAProp(caption, is(cls), 1, action, 1); // по идее можно просто exec сделать, но на всякий случай
    }

    protected LA addEditAProp(LocalizedString caption, CustomClass cls) {
        cls.markUsed(true);
        return addFormAProp(caption, cls, baseLM.getFormEdit());
    }

    protected LA addDeleteAProp(LocalizedString caption, CustomClass cls) {
        return addFormAProp(caption, cls, baseLM.getFormDelete());
    }

    // loggable, security, drilldown
    public LA addMFAProp(LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        return addMFAProp(null, caption, form, objectsToSet, newSession);
    }
    public LA addMFAProp(AbstractGroup group, LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        LA result = addIFAProp(caption, form, objectsToSet, true, WindowFormType.FLOAT, false);
        return addSessionScopeAProp(group, newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION, result);
    }

    protected <O extends ObjectSelector> LA addIFAProp(LocalizedString caption, FormSelector<O> form, ImOrderSet<O> objectsToSet, boolean syncType, WindowFormType windowType, boolean forbidDuplicate) {
        return addIFAProp(null, caption, form, objectsToSet, ListFact.toList(false, objectsToSet.size()), ManageSessionType.AUTO, FormEntity.DEFAULT_NOCANCEL, syncType, windowType, forbidDuplicate, false, false);
    }
    protected <O extends ObjectSelector> LA addIFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ManageSessionType manageSession, Boolean noCancel, boolean syncType, WindowFormType windowType, boolean forbidDuplicate, boolean checkOnOk, boolean readonly) {
        return addIFAProp(group, caption, form, objectsToSet, nulls, ListFact.<O>EMPTY(), ListFact.<LP>EMPTY(), ListFact.<Boolean>EMPTY(), manageSession, noCancel, ListFact.<O>EMPTY(), ListFact.<Property>EMPTY(), syncType, windowType, forbidDuplicate, checkOnOk, readonly);
    }
    protected <O extends ObjectSelector> LA addIFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ImList<O> inputObjects, ImList<LP> inputProps, ImList<Boolean> inputNulls, ManageSessionType manageSession, Boolean noCancel, ImList<O> contextObjects, ImList<Property> contextProperties, boolean syncType, WindowFormType windowType, boolean forbidDuplicate, boolean checkOnOk, boolean readonly) {
        return addProperty(group, new LA<>(new FormInteractiveActionProperty<>(caption, form, objectsToSet, nulls, inputObjects, inputProps, inputNulls, contextObjects, contextProperties, manageSession, noCancel, syncType, windowType, forbidDuplicate, checkOnOk, readonly)));
    }
    protected <O extends ObjectSelector> LA<?> addPFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, Property printerProperty, LP sheetNameProperty, FormPrintType staticType, boolean syncType, Integer selectTop, Property passwordProperty, LP targetProp, boolean removeNullsAndDuplicates) {
        return addProperty(group, new LA<>(new PrintActionProperty<>(caption, form, objectsToSet, nulls, staticType, syncType, selectTop, passwordProperty, sheetNameProperty, targetProp, printerProperty, baseLM.formPageCount, removeNullsAndDuplicates)));
    }
    protected <O extends ObjectSelector> LA addEFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, boolean noHeader, String separator, boolean noEscape, String charset, Property root, Property tag, LP singleExportFile, ImMap<GroupObjectEntity, LP> exportFiles) {
        ExportActionProperty<O> exportAction;
        switch(staticType) {
            case XML:
                exportAction = new ExportXMLActionProperty<O>(caption, form, objectsToSet, nulls, staticType, singleExportFile, charset, root, tag);
                break;
            case JSON:
                exportAction = new ExportJSONActionProperty<O>(caption, form, objectsToSet, nulls, staticType, singleExportFile, charset);
                break;
            case CSV:
                exportAction = new ExportCSVActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, noHeader, separator, noEscape);
                break;
            case XLS:
                exportAction = new ExportXLSActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, false, noHeader);
                break;
            case XLSX:
                exportAction = new ExportXLSActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, true, noHeader);
                break;
            case DBF:
                exportAction = new ExportDBFActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
                break;
            case TABLE:
                exportAction = new ExportTableActionProperty<>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
                break;
            default:
                throw new UnsupportedOperationException();                
        }
        return addProperty(group, new LA<>(exportAction));
    }

    protected <O extends ObjectSelector> LA addAutoImportFAProp(FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll, String separator, boolean noHeader, boolean noEscape, String charset, boolean hasWhere) {
        // getExtension(FILE(prm1))
        // FOR x = getExtension(prm1) DO {
        //    CASE EXCLUSIVE
        //          WHEN x = type.getExtension
        //              IMPORT type form...
        // }
        
        Object[] cases = new Object[0];
        boolean isPlain = !groupFiles.isEmpty();
        for(FormIntegrationType importType : FormIntegrationType.values()) 
            if(importType.isPlain() == isPlain) {
                cases = add(cases, add(new Object[] {addJProp(baseLM.equals2, 1, addCProp(StringClass.text, LocalizedString.create(importType.getExtension(), false))), paramsCount + 1 }, // WHEN x = type.getExtension()
                    directLI(addImportFAProp(importType, formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasWhere)))); // IMPORT type form...
            }        
        
        return addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, paramsCount, null, false, true, 0, false,
                add(add(getUParams(paramsCount), 
                        new Object[] {addJProp(baseLM.equals2, 1, baseLM.getExtension, 2), paramsCount + 1, 1}), // FOR x = getExtension(FILE(prm1))  
                        directLI(addCaseAProp(true, cases))));  // CASE EXCLUSIVE
    }
    
    protected <O extends ObjectSelector> LA addImportFAProp(FormIntegrationType format, FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll, String separator, boolean noHeader, boolean noEscape, String charset, boolean hasWhere) {
        ImportAction importAction;

        if(format == null)
            return addAutoImportFAProp(formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasWhere);

        switch (format) {
            // hierarchical
            case XML:
                importAction = new ImportXMLActionProperty(paramsCount, formEntity, charset);
                break;
            case JSON:
                importAction = new ImportJSONActionProperty(paramsCount, formEntity, charset);
                break;
            // plain
            case CSV:
                importAction = new ImportCSVActionProperty(paramsCount, groupFiles, formEntity, charset, noHeader, noEscape, separator);
                break;
            case DBF:
                importAction = new ImportDBFActionProperty(paramsCount, groupFiles, formEntity, charset, hasWhere);
                break;
            case XLS:
            case XLSX:
                importAction = new ImportXLSActionProperty(paramsCount, groupFiles, formEntity, charset, noHeader, sheetAll);
                break;
            case TABLE:
                importAction = new ImportTableActionProperty(paramsCount, groupFiles, formEntity, charset);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return addProperty(null, new LA<>(importAction));
    }

    // ------------------- Change Class action ----------------- //

    protected LA addChangeClassAProp(ConcreteObjectClass cls, int resInterfaces, int changeIndex, boolean extendedContext, boolean conditional, Object... params) {
        int innerIntCnt = resInterfaces + (extendedContext ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerIntCnt);
        ImOrderSet<PropertyInterface> mappedInterfaces = extendedContext ? innerInterfaces.removeOrderIncl(innerInterfaces.get(changeIndex)) : innerInterfaces;
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<PropertyInterface, PropertyInterface> conditionalPart = (PropertyMapImplement<PropertyInterface, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces) : null);

        return addAProp(new ChangeClassAction<>(cls, false, innerInterfaces.getSet(),
                mappedInterfaces, innerInterfaces.get(changeIndex), conditionalPart, getBaseClass()));
    }

    // ------------------- Export property action ----------------- //
    protected LA addExportPropertyAProp(LocalizedString caption, FormIntegrationType type, int resInterfaces, List<String> aliases, List<Boolean> literals, ImOrderMap<String, Boolean> orders,
                                        LP singleExportFile, boolean conditional, Property root, Property tag, String separator,
                                        boolean noHeader, boolean noEscape, String charset, boolean attr, Object... params) throws FormEntity.AlreadyDefined {
        int extraParamsCount = (root != null ? 1 : 0) + (tag != null ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> exprs = readImplements.subList(resInterfaces, readImplements.size() - (conditional ? 1 : 0) - extraParamsCount);
        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());
        
        // determining where
        PropertyInterfaceImplement<PropertyInterface> where = conditional ? readImplements.get(readImplements.size() - 1 - extraParamsCount) : null;
        where = DerivedProperty.getFullWhereProperty(innerInterfaces.getSet(), mapInterfaces.getSet(), where, exprs.getCol());

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, null, mapInterfaces, aliases, literals, exprs, where, orders, attr, version);
        ImOrderSet<ObjectEntity> objectsToSet = mapInterfaces.mapOrder(form.mapObjects);
        ImList<Boolean> nulls = ListFact.toList(true, mapInterfaces.size());
        
        ImMap<GroupObjectEntity, LP> exportFiles = MapFact.EMPTY();
        if(type.isPlain()) {
            exportFiles = MapFact.singleton(form.groupObject == null ? GroupObjectEntity.NULL : form.groupObject, singleExportFile);
            singleExportFile = null;
        }            
                
        // creating action
        return addEFAProp(null, caption, form, objectsToSet, nulls, type, noHeader, separator, noEscape, charset, root, tag, singleExportFile, exportFiles);
    }

    protected LA addImportPropertyAProp(FormIntegrationType type, int paramsCount, List<String> aliases, List<Boolean> literals, ImList<ValueClass> paramClasses, LP<?> whereLCP, String separator, boolean noHeader, boolean noEscape, String charset, boolean sheetAll, boolean attr, boolean hasWhere, Object... params) throws FormEntity.AlreadyDefined {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> exprs = readCalcImplements(innerInterfaces, params);

        // determining where
        PropertyInterfaceImplement<PropertyInterface> where = innerInterfaces.size() == 1 && whereLCP != null ? whereLCP.getImplement(innerInterfaces.single()) : null;

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, paramClasses, SetFact.<PropertyInterface>EMPTYORDER(), aliases, literals, exprs, where, MapFact.<String, Boolean>EMPTYORDER(), attr, version);
        
        // create action
        return addImportFAProp(type, form, paramsCount, SetFact.singletonOrder(form.groupObject), sheetAll, separator, noHeader, noEscape, charset, hasWhere);
    }

    // ------------------- Set property action ----------------- //

    protected <C extends PropertyInterface, W extends PropertyInterface> LA addSetPropertyAProp(AbstractGroup group, LocalizedString caption, int resInterfaces,
                                                                                                boolean conditional, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<W, PropertyInterface> conditionalPart = (PropertyMapImplement<W, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + 2) : DerivedProperty.createTrue());
        return addProperty(group, new LA<>(new SetAction<C, W, PropertyInterface>(caption,
                innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart,
                (PropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), readImplements.get(resInterfaces + 1))));
    }

    // ------------------- List action ----------------- //

    protected LA addListAProp(Object... params) {
        return addListAProp(SetFact.<SessionDataProperty>EMPTY(), params);
    }
    protected LA addListAProp(ImSet<SessionDataProperty> localsInScope, Object... params) {
        return addListAProp(null, 0, LocalizedString.NONAME, localsInScope, params);
    }
    protected LA addListAProp(int removeLast, Object... params) {
        return addListAProp(null, removeLast, LocalizedString.NONAME, SetFact.<SessionDataProperty>EMPTY(), params);
    }
    protected LA addListAProp(LocalizedString caption, Object... params) {
        return addListAProp(null, 0, caption, SetFact.<SessionDataProperty>EMPTY(), params);        
    }
    protected LA addListAProp(AbstractGroup group, int removeLast, LocalizedString caption, ImSet<SessionDataProperty> localsInScope, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        return addProperty(group, new LA<>(new ListActionProperty(caption, listInterfaces,
                readActionImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params), localsInScope)));
    }

    protected LA addAbstractListAProp(boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LA<>(new ListActionProperty(LocalizedString.NONAME, isChecked, isLast, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- Try action ----------------- //

    protected LA addTryAProp(AbstractGroup group, LocalizedString caption, boolean hasCatch, boolean hasFinally, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 1 && readImplements.size() <= 3;

        ActionMapImplement<?, PropertyInterface> tryAction = (ActionMapImplement<?, PropertyInterface>) readImplements.get(0);
        ActionMapImplement<?, PropertyInterface> catchAction = (ActionMapImplement<?, PropertyInterface>) (hasCatch ? readImplements.get(1) : null);
        ActionMapImplement<?, PropertyInterface> finallyAction = (ActionMapImplement<?, PropertyInterface>) (hasFinally ? (readImplements.get(hasCatch ? 2 : 1)) : null);
        return addProperty(group, new LA<>(new TryAction(caption, listInterfaces, tryAction, catchAction, finallyAction)));
    }
    
    // ------------------- If action ----------------- //

    protected LA addIfAProp(Object... params) {
        return addIfAProp(null, LocalizedString.NONAME, false, params);
    }

    protected LA addIfAProp(LocalizedString caption, Object... params) {
        return addIfAProp(null, caption, false, params);
    }

    protected LA addIfAProp(AbstractGroup group, LocalizedString caption, boolean not, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        return addProperty(group, new LA(CaseActionProperty.createIf(caption, not, listInterfaces, (PropertyInterfaceImplement<PropertyInterface>) readImplements.get(0),
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(1), readImplements.size() == 3 ? (ActionMapImplement<?, PropertyInterface>) readImplements.get(2) : null)));
    }

    // ------------------- Case action ----------------- //

    protected LA addCaseAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);

        MList<ActionCase<PropertyInterface>> mCases = ListFact.mList();
        for (int i = 0; i*2+1 < readImplements.size(); i++) {
            mCases.add(new ActionCase<>((PropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2), (ActionMapImplement<?, PropertyInterface>) readImplements.get(i*2+1)));
        }
        if(readImplements.size() % 2 != 0) {
            mCases.add(new ActionCase<>(DerivedProperty.createTrue(), (ActionMapImplement<?, PropertyInterface>) readImplements.get(readImplements.size() - 1)));
        }
        return addProperty(null, new LA<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, listInterfaces, mCases.immutableList())));
    }

    protected LA addMultiAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);

        MList<ActionMapImplement> mCases = ListFact.mList();
        for (int i = 0; i < readImplements.size(); i++) {
            mCases.add((ActionMapImplement) readImplements.get(i));
        }
        return addProperty(null, new LA<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, mCases.immutableList(), listInterfaces)));
    }

    protected LA addAbstractCaseAProp(ListCaseAction.AbstractType type, boolean isExclusive, boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LA<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, isChecked, isLast, type, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- For action ----------------- //

    protected LA addForAProp(LocalizedString caption, boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, CustomClass addClass, boolean autoSet, boolean hasCondition, int noInline, boolean forceInline, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(innerInterfaces, params);

        int implCnt = readImplements.size();

        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());

        PropertyMapImplement<?, PropertyInterface> ifProp = hasCondition? (PropertyMapImplement<?, PropertyInterface>) readImplements.get(resInterfaces) : null;

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                BaseUtils.<ImList<PropertyInterfaceImplement<PropertyInterface>>>immutableCast(readImplements.subList(resInterfaces + (hasCondition ? 1 : 0), implCnt - (hasElse ? 2 : 1) - (addClass != null ? 1: 0) - noInline)).toOrderExclSet().toOrderMap(!ascending);

        PropertyInterface addedInterface = addClass!=null ? (PropertyInterface) readImplements.get(implCnt - (hasElse ? 3 : 2) - noInline) : null;

        ActionMapImplement<?, PropertyInterface> elseAction =
                !hasElse ? null : (ActionMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 2 - noInline);

        ActionMapImplement<?, PropertyInterface> action =
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 1 - noInline);

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(readImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        return addProperty(null, new LA<>(
                new ForAction<>(caption, innerInterfaces.getSet(), mapInterfaces, ifProp, orders, ordersNotNull, action, elseAction, addedInterface, addClass, autoSet, recursive, noInlineInterfaces, forceInline))
        );
    }

    // ------------------- JOIN ----------------- //

    public LA addJoinAProp(LA action, Object... params) {
        return addJoinAProp(LocalizedString.NONAME, action, params);
    }

    protected LA addJoinAProp(LocalizedString caption, LA action, Object... params) {
        return addJoinAProp(null, caption, action, params);
    }

    protected LA addJoinAProp(AbstractGroup group, LocalizedString caption, LA action, Object... params) {
        return addJoinAProp(group, caption, null, action, params);
    }

    protected LA addJoinAProp(AbstractGroup group, LocalizedString caption, ValueClass[] classes, LA action, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(listInterfaces, params);
        return addProperty(group, new LA(new JoinAction(caption, listInterfaces, mapActionImplement(action, readImplements))));
    }

    // ------------------------ APPLY / CANCEL ----------------- //

    protected LA addApplyAProp(AbstractGroup group, LocalizedString caption, LA action, boolean singleApply,
                               FunctionSet<SessionDataProperty> keepSessionProps, boolean serializable) {
        
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        ApplyAction applyAction = new ApplyAction(baseLM, actionImplement, caption, listInterfaces, keepSessionProps, serializable);
        actionImplement.property.singleApply = singleApply;
        return addProperty(group, new LA<>(applyAction));
    }

    protected LA addCancelAProp(AbstractGroup group, LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {

        CancelActionProperty applyAction = new CancelActionProperty(caption, keepSessionProps);
        return addProperty(group, new LA<>(applyAction));
    }

    // ------------------- SESSION SCOPE ----------------- //

    protected LA addSessionScopeAProp(FormSessionScope sessionScope, LA action) {
        return addSessionScopeAProp(null, sessionScope, action);
    }
    protected LA addSessionScopeAProp(AbstractGroup group, FormSessionScope sessionScope, LA action) {
        return addSessionScopeAProp(group, sessionScope, action, SetFact.<LP>EMPTY());
    }
    protected LA addSessionScopeAProp(FormSessionScope sessionScope, LA action, ImCol<LP> nestedProps) {
        return addSessionScopeAProp(null, sessionScope, action, nestedProps);
    }
    protected LA addSessionScopeAProp(AbstractGroup group, FormSessionScope sessionScope, LA action, ImCol<LP> nestedProps) {
        if(sessionScope.isNewSession()) {
            action = addNewSessionAProp(null, action, sessionScope.isNestedSession(), false, false, nestedProps.mapMergeSetValues(new GetValue<SessionDataProperty, LP>() {
                public SessionDataProperty getMapValue(LP value) {
                    return (SessionDataProperty) value.property;
                }
            }));
        }
        return action;
    }

    // ------------------- NEWSESSION ----------------- //

    protected LA addNewSessionAProp(AbstractGroup group,
                                    LA action, boolean isNested, boolean singleApply, boolean newSQL,
                                    FunctionSet<SessionDataProperty> migrateSessionProps) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        NewSessionActionProperty actionProperty = new NewSessionActionProperty(
                LocalizedString.NONAME, listInterfaces, actionImplement, singleApply, newSQL, migrateSessionProps, isNested);
        
        actionProperty.drawOptions.inheritDrawOptions(action.property.drawOptions);
        actionProperty.inheritCaption(action.property);
        
        return addProperty(group, new LA<>(actionProperty));
    }

    protected LA addNewThreadAProp(AbstractGroup group, LocalizedString caption, boolean withConnection, boolean hasPeriod, boolean hasDelay, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        PropertyInterfaceImplement connection = withConnection ? (PropertyInterfaceImplement) readImplements.get(1) : null;
        PropertyInterfaceImplement period = hasPeriod ? (PropertyInterfaceImplement) readImplements.get(1) : null;
        PropertyInterfaceImplement delay = hasDelay ? (PropertyInterfaceImplement) readImplements.get(hasPeriod ? 2 : 1) : null;
        return addProperty(group, new LA(new NewThreadActionProperty(caption, listInterfaces, (ActionMapImplement) readImplements.get(0), period, delay, connection)));
    }

    protected LA addNewExecutorAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        return addProperty(group, new LA(new NewExecutorActionProperty(caption, listInterfaces,
                (ActionMapImplement) readImplements.get(0), (PropertyInterfaceImplement) readImplements.get(1))));
    }

    // ------------------- Request action ----------------- //

    protected LA addRequestAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2;

        ActionMapImplement<?, PropertyInterface> elseAction =  readImplements.size() == 3 ? (ActionMapImplement<?, PropertyInterface>) readImplements.get(2) : null;
        return addProperty(group, new LA(new RequestAction(caption, listInterfaces, 
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(0), (ActionMapImplement<?, PropertyInterface>) readImplements.get(1),
                elseAction))
        );
    }

    // ------------------- Input ----------------- //

    protected LAP addInputAProp(AbstractGroup group, LocalizedString caption, DataClass dataClass, LP<?> targetProp, Object... params) {
        return addJoinAProp(group, caption, addInputAProp(dataClass, targetProp != null ? targetProp.property : null), params);
    }
    @IdentityStrongLazy
    protected LA addInputAProp(DataClass dataClass, Property targetProp) { // так как у LP нет 
        return addProperty(null, new LA(new InputActionProperty(LocalizedString.create("Input"), dataClass, targetProp != null ? new LP(targetProp) : null)));
    }

    // ------------------- Constant ----------------- //

    protected <T extends PropertyInterface> LP addUnsafeCProp(DataClass valueClass, Object value) {
        ValueProperty.checkLocalizedString(value, valueClass);
        return baseLM.addCProp(valueClass, valueClass instanceof StringClass ? value : valueClass.read(value));
    }

    protected <T extends PropertyInterface> LP addCProp(StaticClass valueClass, Object value) {
        return baseLM.addCProp(valueClass, value);
    }

    // ------------------- Random ----------------- //

    protected LP addRMProp(LocalizedString caption) {
        return addProperty(null, new LP<>(new RandomFormulaProperty(caption)));
    }

    // ------------------- FORMULA ----------------- //

    protected LP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, null, paramCount);
    }

    protected LP addSFProp(CustomFormulaSyntax formula, int paramCount, boolean hasNotNull) {
        return addSFProp(formula, null, paramCount, hasNotNull);
    }

    protected LP addSFProp(String formula, DataClass value, int paramCount) {
        return addSFProp(new CustomFormulaSyntax(formula), value, paramCount, false);
    }
    
    protected LP addSFProp(CustomFormulaSyntax formula, DataClass value, int paramCount, boolean hasNotNull) {
        return addProperty(null, new LP<>(new StringFormulaProperty(value, formula, paramCount, hasNotNull)));
    }

    // ------------------- Операции сравнения ----------------- //

    protected LP addCFProp(Compare compare) {
        return addProperty(null, new LP<>(new CompareFormulaProperty(compare)));
    }

    // ------------------- Алгебраические операции ----------------- //

    protected LP addSumProp() {
        return addProperty(null, new LP<>(new FormulaImplProperty(LocalizedString.create("sum"), 2, new SumFormulaImpl())));
    }

    protected LP addMultProp() {
        return addProperty(null, new LP<>(new FormulaImplProperty(LocalizedString.create("multiply"), 2, new MultiplyFormulaImpl())));
    }

    protected LP addSubtractProp() {
        return addProperty(null, new LP<>(new FormulaImplProperty(LocalizedString.create("subtract"), 2, new SubtractFormulaImpl())));
    }

    protected LP addDivideProp() {
        return addProperty(null, new LP<>(new FormulaImplProperty(LocalizedString.create("divide"), 2, new DivideFormulaImpl())));
    }

    // ------------------- cast ----------------- //

    protected <P extends PropertyInterface> LP addCastProp(DataClass castClass) {
        return baseLM.addCastProp(castClass);
    }

    // ------------------- Операции со строками ----------------- //

    protected <P extends PropertyInterface> LP addSProp(int intNum) {
        return addSProp(intNum, " ");
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum, String separator) {
        return addProperty(null, new LP<>(new StringConcatenateProperty(LocalizedString.create("{logics.join}"), intNum, separator)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum) {
        return addInsensitiveSProp(intNum, " ");
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum, String separator) {
        return addProperty(null, new LP<>(new StringConcatenateProperty(LocalizedString.create("{logics.join}"), intNum, separator, true)));
    }

    // ------------------- AND ----------------- //

    protected LP addAFProp(boolean... nots) {
        return addAFProp(null, nots);
    }

    protected LP addAFProp(AbstractGroup group, boolean... nots) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(nots.length + 1);
        MList<Boolean> mList = ListFact.mList(nots.length);
        boolean wasNot = false;
        for(boolean not : nots) {
            mList.add(not);
            wasNot = wasNot || not;
        }
        if(wasNot)
            return mapLProp(group, false, DerivedProperty.createAnd(interfaces, mList.immutableList()), interfaces);
        else
            return addProperty(group, new LP<>(new AndFormulaProperty(nots.length)));
    }

    // ------------------- concat ----------------- //

    protected LP addCCProp(int paramCount) {
        return addProperty(null, new LP<>(new ConcatenateProperty(paramCount)));
    }

    protected LP addDCCProp(int paramIndex) {
        return addProperty(null, new LP<>(new DeconcatenateProperty(paramIndex, baseLM.baseClass)));
    }

    // ------------------- JOIN (продолжение) ----------------- //

    public LP addJProp(LP mainProp, Object... params) {
        return addJProp( false, mainProp, params);
    }

    protected LP addJProp(boolean user, LP mainProp, Object... params) {
        return addJProp(user, 0, mainProp, params);
    }
    protected LP addJProp(boolean user, int removeLast, LP<?> mainProp, Object... params) {

        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<JoinProperty.Interface>> listImplements = readCalcImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params);
        JoinProperty<?> property = new JoinProperty(LocalizedString.NONAME, listInterfaces, user,
                mapCalcImplement(mainProp, listImplements));

        for(Property andProp : mainProp.property.getAndProperties())
            property.drawOptions.inheritDrawOptions(andProp.drawOptions);

        return addProperty(null, new LP<>(property, listInterfaces));
    }

    // ------------------- mapLProp ----------------- //

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, new LP<>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    protected <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return mapLProp(group, persistent, implement, property.listInterfaces);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, new LP<>(implement.property, listImplements.toOrderExclSet().mapOrder(implement.mapping.toRevExclMap().reverse())));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty property, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<GroupProperty.Interface<P>, PropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    // ------------------- Order property ----------------- //

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, boolean persistent, LocalizedString caption, PartitionType partitionType, boolean ascending, boolean ordersNotNull, boolean includeLast, int partNum, Object... params) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(interfaces, params);

        ImSet<PropertyInterfaceImplement<PropertyInterface>> partitions = listImplements.subList(0, partNum).toOrderSet().getSet();
        ImList<PropertyInterfaceImplement<PropertyInterface>> mainProp = listImplements.subList(partNum, partNum + 1);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createOProp(caption, partitionType, interfaces.getSet(), mainProp, partitions, orders, ordersNotNull, includeLast), interfaces);
    }

    protected <P extends PropertyInterface> LP addRProp(AbstractGroup group, boolean persistent, LocalizedString caption, Cycle cycle, ImList<Integer> resInterfaces, ImRevMap<Integer, Integer> mapPrev, Object... params) {
        int innerCount = getIntNum(params);
        final ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerCount);
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplement = readCalcImplements(innerInterfaces, params);

        GetValue<PropertyInterface, Integer> getInnerInterface = new GetValue<PropertyInterface, Integer>() {
            public PropertyInterface getMapValue(Integer value) {
                return innerInterfaces.get(value);
            }
        };

        final ImOrderSet<RecursiveProperty.Interface> interfaces = RecursiveProperty.getInterfaces(resInterfaces.size());
        ImRevMap<RecursiveProperty.Interface, PropertyInterface> mapInterfaces = resInterfaces.mapListRevKeyValues(new GetIndex<RecursiveProperty.Interface>() {
            public RecursiveProperty.Interface getMapValue(int i) {
                return interfaces.get(i);
            }}, getInnerInterface);
        ImRevMap<PropertyInterface, PropertyInterface> mapIterate = mapPrev.mapRevKeyValues(getInnerInterface, getInnerInterface); // старые на новые

        PropertyMapImplement<?, PropertyInterface> initial = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(0);
        PropertyMapImplement<?, PropertyInterface> step = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(1);

        assert initial.property.getType() instanceof IntegralClass == (step.property.getType() instanceof IntegralClass);
        if(!(initial.property.getType() instanceof IntegralClass) && (cycle == Cycle.NO || (cycle==Cycle.IMPOSSIBLE && persistent))) {
            PropertyMapImplement<?, PropertyInterface> one = createStatic(1L, LongClass.instance);
            initial = createAnd(innerInterfaces.getSet(), one, initial);
            step = createAnd(innerInterfaces.getSet(), one, step);
        }

        RecursiveProperty<PropertyInterface> property = new RecursiveProperty<>(caption, interfaces, cycle,
                mapInterfaces, mapIterate, initial, step);
        if(cycle==Cycle.NO)
            addConstraint(property.getConstrainedProperty(), false);

        LP result = new LP<>(property, interfaces);
//        if (convertToLogical)
//            return addJProp(group, name, false, caption, baseLM.notZero, directLI(addProperty(null, persistent, result)));
//        else
            return addProperty(group, result);
    }

    // ------------------- Ungroup property ----------------- //

    protected <L extends PropertyInterface> LP addUGProp(AbstractGroup group, boolean persistent, boolean over, LocalizedString caption, int intCount, boolean ascending, boolean ordersNotNull, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<PropertyInterfaceImplement<PropertyInterface>>() {
            public PropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i);
            }});
        PropertyInterfaceImplement<PropertyInterface> restriction = listImplements.get(partNum);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createUGProp(caption, innerInterfaces.getSet(),
                new PropertyImplement<>(ungroup.property, groupImplement), orders, ordersNotNull, restriction, over), innerInterfaces);
    }

    protected <L extends PropertyInterface> LP addPGProp(AbstractGroup group, boolean persistent, int roundlen, boolean roundfirst, LocalizedString caption, int intCount, List<ResolveClassSet> explicitInnerClasses, boolean ascending, boolean ordersNotNull, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<PropertyInterfaceImplement<PropertyInterface>>() {
            public PropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i);
            }});
        PropertyInterfaceImplement<PropertyInterface> proportion = listImplements.get(partNum);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createPGProp(caption, roundlen, roundfirst, baseLM.baseClass, innerInterfaces, explicitInnerClasses,
                new PropertyImplement<>(ungroup.property, groupImplement), proportion, orders, ordersNotNull), innerInterfaces);
    }

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(AbstractGroup group, String title, LF maxGroupProp, LF unGroupProp, Object... params) {
          List<LI> lParams = readLI(params);
          List<LI> lUnGroupParams = lParams.subList(0,unGroupProp.listInterfaces.size());
          List<LI> orderParams = lParams.subList(unGroupProp.listInterfaces.size(),lParams.size());

          int intNum = maxGroupProp.listInterfaces.size();

          // "двоим" интерфейсы, для результ. св-ва
          // ставим equals'ы на группировочные свойства (раздвоенные)
          List<Object[]> groupParams = new ArrayList<Object[]>();
          groupParams.add(directLI(maxGroupProp));
          for(LI li : lUnGroupParams)
              groupParams.add(li.compare(equals2, this, intNum));

          boolean[] andParams = new boolean[groupParams.size()-1];
          for(int i=0;i<andParams.length;i++)
              andParams[i] = false;
          LF groupPropSet = addJProp(addAFProp(andParams),BaseUtils.add(groupParams));

          for(int i=0;i<intNum;i++) { // докинем не достающие порядки
              boolean found = false;
              for(LI order : orderParams)
                  if(order instanceof LII && ((LII)order).intNum==i+1) {
                      found = true;
                      break;
                  }
              if(!found)
                  orderParams.add(new LII(i+1));
          }

          // ставим на предшествие сначала order'а, потом всех интерфейсов
          LF[] orderProps = new LF[orderParams.size()];
          for(int i=0;i<orderParams.size();i++) {
              orderProps[i] = (addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(greater2, this, intNum))));
              groupPropSet = addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(equals2, this, intNum)));
          }
          LF groupPropPrev = addSUProp(Union.OVERRIDE, orderProps);

          // группируем суммируя по "задвоенным" св-вам maxGroup
          Object[] remainParams = new Object[intNum];
          for(int i=1;i<=intNum;i++)
              remainParams[i-1] = i+intNum;
          LF remainPrev = addSGProp(groupPropPrev, remainParams);

          // создадим группировочное св-во с маппом на общий интерфейс, нужно поубирать "дырки"


          // возвращаем MIN2(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
          LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
          LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
          LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
          LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
          LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
          return addJProp(group, title, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
      }
    */

    protected ImOrderSet<PropertyInterface> genInterfaces(int interfaces) {
        return SetFact.toOrderExclSet(interfaces, ActionOrProperty.genInterface);
    }

    // ------------------- GROUP SUM ----------------- //

    protected LP addSGProp(AbstractGroup group, boolean persistent, boolean notZero, LocalizedString caption, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, persistent, notZero, caption, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LP addSGProp(AbstractGroup group, boolean persistent, boolean notZero, LocalizedString caption, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> implement) {
        ImList<PropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<>(caption, innerInterfaces.getSet(), listImplements, implement.get(0));
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- Override property ----------------- //

    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addOGProp(group, persist, caption, type, numOrders, ordersNotNull, descending, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }
    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        int numExprs = type.numExprs();
        ImList<PropertyInterfaceImplement<T>> props = listImplements.subList(0, numExprs);
        ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders = listImplements.subList(numExprs, numExprs + numOrders).toOrderSet().toOrderMap(descending);
        ImList<PropertyInterfaceImplement<T>> groups = listImplements.subList(numExprs + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<>(caption, innerInterfaces.getSet(), groups.getCol(), props, type, orders, ordersNotNull);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persist, property, groups);
    }

    // ------------------- GROUP MAX ----------------- //

    protected LP addMGProp(AbstractGroup group, boolean persist, LocalizedString caption, boolean min, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        return addMGProp(group, persist, new LocalizedString[]{caption}, 1, min, interfaces, explicitInnerClasses, params)[0];
    }

    protected LP[] addMGProp(AbstractGroup group, boolean persist, LocalizedString[] captions, int exprs, boolean min, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addMGProp(group, persist, captions, exprs, min, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, LocalizedString[] captions, int exprs, boolean min, ImOrderSet<T> listInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        LP[] result = new LP[exprs];

        MSet<Property> mOverridePersist = SetFact.mSet();

        ImList<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(exprs, listImplements.size());
        ImList<PropertyImplement<?, PropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(captions, listInterfaces, explicitInnerClasses, baseLM.baseClass,
                listImplements.subList(0, exprs), groupImplements.getCol(), mOverridePersist, min);

        ImSet<Property> overridePersist = mOverridePersist.immutable();

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);

        if (persist) {
            if (overridePersist.size() > 0) {
                for (Property property : overridePersist)
                    addProperty(null, new LP(property));
            } else
                for (LP lcp : result) addPersistent(lcp);
        }

        return result;
    }

    // ------------------- CGProperty ----------------- //

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, LP<PropertyInterface> dataProp, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addCGProp(group, checkChange, persistent, caption, dataProp, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, LP<P> dataProp, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        CycleGroupProperty<T, P> property = new CycleGroupProperty<>(caption, innerInterfaces.getSet(), listImplements.subList(1, listImplements.size()).getCol(), listImplements.get(0), dataProp == null ? null : dataProp.property);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        // нужно добавить ограничение на уникальность
        addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements.subList(1, listImplements.size()));
    }

//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, LocalizedString caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {

    // ------------------- GROUP AGGR ----------------- //

    protected LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... props) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addAGProp(group, checkChange, persistent, caption, noConstraint, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, props));
    }

    protected <T extends PropertyInterface<T>, I extends PropertyInterface> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        T aggrInterface = (T) listImplements.get(0);
        PropertyInterfaceImplement<T> whereProp = listImplements.get(1);
        ImList<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(2, listImplements.size());

        AggregateGroupProperty<T> aggProp = AggregateGroupProperty.create(caption, innerInterfaces.getSet(), whereProp, aggrInterface, groupImplements.toOrderExclSet().getSet());
        aggProp.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);
        return addAGProp(group, checkChange, persistent, noConstraint, aggProp, groupImplements);
    }

    // чисто для generics
    private <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, boolean noConstraint, AggregateGroupProperty<T> property, ImList<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        if(!noConstraint)
            addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- UNION ----------------- //

    protected LP addUProp(AbstractGroup group, LocalizedString caption, Union unionType, String separator, int[] coeffs, Object... params) {
        return addUProp(group, false, caption, unionType, null, coeffs, params);
    }

    protected LP addUProp(AbstractGroup group, boolean persistent, LocalizedString caption, Union unionType, String separator, int[] coeffs, Object... params) {

        assert (unionType==Union.SUM)==(coeffs!=null);
        assert (unionType==Union.STRING_AGG)==(separator !=null);

        int intNum = getIntNum(params);
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        ImList<PropertyInterfaceImplement<UnionProperty.Interface>> listOperands = readCalcImplements(listInterfaces, params);

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
            case MIN:
                property = new MaxUnionProperty(unionType == Union.MIN, caption, listInterfaces, listOperands.getCol());
                break;
            case SUM:
                MMap<PropertyInterfaceImplement<UnionProperty.Interface>, Integer> mMapOperands = MapFact.mMap(MapFact.<PropertyInterfaceImplement<UnionProperty.Interface>>addLinear());
                for(int i=0;i<listOperands.size();i++)
                    mMapOperands.add(listOperands.get(i), coeffs[i]);
                property = new SumUnionProperty(caption, listInterfaces, mMapOperands.immutable());
                break;
            case OVERRIDE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands, false, false, false);
                break;
            case XOR:
                property = new XorUnionProperty(caption, listInterfaces, listOperands);
                break;
            case EXCLUSIVE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands.getCol(), false);
                break;
            case CLASS:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands.getCol(), true);
                break;
            case CLASSOVERRIDE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands, true, false, false);
                break;
            case STRING_AGG:
                property = new StringAggUnionProperty(caption, listInterfaces, listOperands, separator);
                break;
        }

        return addProperty(group, new LP<>(property, listInterfaces));
    }

    protected LP addAUProp(AbstractGroup group, boolean persistent, boolean isExclusive, boolean isChecked, boolean isLast, CaseUnionProperty.Type type, LocalizedString caption, ValueClass valueClass, ValueClass... interfaces) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, new LP<>(
                new CaseUnionProperty(isExclusive, isChecked, isLast, type, caption, listInterfaces, valueClass, listInterfaces.mapList(ListFact.toList(interfaces))), listInterfaces));
    }

    protected LP addCaseUProp(AbstractGroup group, boolean persistent, LocalizedString caption, boolean isExclusive, Object... params) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(getIntNum(params));
        MList<CalcCase<UnionProperty.Interface>> mListCases = ListFact.mList();
        ImList<PropertyMapImplement<?,UnionProperty.Interface>> mapImplements = (ImList<PropertyMapImplement<?, UnionProperty.Interface>>) (ImList<?>) readCalcImplements(listInterfaces, params);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            mListCases.add(new CalcCase<>(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            mListCases.add(new CalcCase<>(new PropertyMapImplement<PropertyInterface, UnionProperty.Interface>((Property<PropertyInterface>) baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, new LP<>(new CaseUnionProperty(caption, listInterfaces, isExclusive, mListCases.immutableList()), listInterfaces));
    }

    public static List<ResolveClassSet> getSignatureForLogProperty(List<ResolveClassSet> basePropSignature, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>(basePropSignature);
        signature.add(systemEventsLM.currentSession.property.getValueClass(ClassType.aroundPolicy).getResolveSet());
        return signature;
    }
    
    public static List<ResolveClassSet> getSignatureForLogProperty(LP lp, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>();
        for (ValueClass cls : lp.getInterfaceClasses(ClassType.logPolicy)) {
            signature.add(cls.getResolveSet());
        }
        return getSignatureForLogProperty(signature, systemEventsLM);    
    } 

    public static String getLogPropertyCN(LP<?> lp, String logNamespace, SystemEventsLogicsModule systemEventsLM) {
        String namespace = PropertyCanonicalNameParser.getNamespace(lp.property.getCanonicalName());
        String name = getLogPropertyName(namespace, lp.property.getName());

        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        return PropertyCanonicalNameUtils.createName(logNamespace, name, signature);
    }

    private static String getLogPropertyName(LP<?> lp, boolean drop) {
        String namespace = PropertyCanonicalNameParser.getNamespace(lp.property.getCanonicalName());
        return getLogPropertyName(namespace, lp.property.getName(), drop);
    }

    private static String getLogPropertyName(String namespace, String name) {
        return getLogPropertyName(namespace, name, false);
    }


    private static String getLogPropertyName(String namespace, String name, boolean drop) {
        return (drop ? PropertyCanonicalNameUtils.logDropPropPrefix : PropertyCanonicalNameUtils.logPropPrefix) + namespace + "_" + name;
    }
    
    public static String getLogPropertyCN(String logNamespace, String namespace, String name, List<ResolveClassSet> signature) {
        return PropertyCanonicalNameUtils.createName(logNamespace, getLogPropertyName(namespace, name), signature);            
    } 
    
    // ------------------- Loggable ----------------- //
    // todo [dale]: тут конечно страх, во-первых, сигнатура берется из интерфейсов свойства (issue #48),
    // во-вторых руками markStored вызывается, чтобы обойти проблему с созданием propertyField из addDProp 
    public LP addLProp(SystemEventsLogicsModule systemEventsLM, LP lp) {
        assert lp.property.isNamed();
        String name = getLogPropertyName(lp, false);
        
        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        
        LP result = addDCProp(LocalizedString.create("{logics.log}" + " " + lp.property), 1, lp, add(new Object[]{addJProp(baseLM.equals2, 1, systemEventsLM.currentSession), lp.listInterfaces.size() + 1}, directLI(lp)));

        makePropertyPublic(result, name, signature);
        ((StoredDataProperty)result.property).markStored(baseLM.tableFactory);
        return result;
    }

    public LP addLDropProp(SystemEventsLogicsModule systemEventsLM, LP lp) {
        String name = getLogPropertyName(lp, true);

        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);

        LP equalsProperty = addJProp(baseLM.equals2, 1, systemEventsLM.currentSession);
        LP logDropProperty = addLogProp(LocalizedString.create("{logics.log}" + " " + lp.property + " {drop}"), 1, lp, add(new Object[]{equalsProperty, lp.listInterfaces.size() + 1}, directLI(lp)));

        LP changedProperty = addCHProp(lp, IncrementType.DROP, PrevScope.EVENT);
        LP whereProperty = addJProp(baseLM.and1, add(directLI(changedProperty), new Object[] {equalsProperty, changedProperty.listInterfaces.size() + 1}));

        Object[] params = directLI(baseLM.vtrue);
        if (whereProperty != null) {
            params = BaseUtils.add(params, directLI(whereProperty));
        }
        logDropProperty.setEventChange(systemEventsLM, true, params);

        makePropertyPublic(logDropProperty, name, signature);
        ((StoredDataProperty)logDropProperty.property).markStored(baseLM.tableFactory);

        return logDropProperty;
    }

    private LP toLogical(LP property) {
        return addJProp(baseLM.and1, add(baseLM.vtrue, directLI(property)));
    }

    private LP convertToLogical(LP property) {
        if (!isLogical(property)) {
            property = toLogical(property);
        }
        return property;
    }

    protected boolean isLogical(LP<?> property) {
        if(property == null)
            return false;

        Type type = property.property.getType();
        return type != null && type.equals(LogicalClass.instance);
    }

    public LP addLWhereProp(LP logValueProperty, LP logDropProperty) {
        return addUProp(null, LocalizedString.NONAME, Union.OVERRIDE, null, null, add(directLI(convertToLogical(logValueProperty)), directLI(logDropProperty)));
                
    }

    // ------------------- CONCAT ----------------- //

    protected LP addSFUProp(int intNum, String separator) {
        return addSFUProp(separator, intNum);
    }

    protected LP addSFUProp(String separator, int intNum) {
        return addUProp(null, false, LocalizedString.create("{logics.join}"), Union.STRING_AGG, separator, null, getUParams(intNum));
    }

    // ------------------- ACTION ----------------- //

    public LA addAProp(Action property) {
        return addAProp(null, property);
    }

    public LA addAProp(AbstractGroup group, Action property) {
        return addProperty(group, new LA(property));
    }

    // ------------------- MESSAGE ----------------- //

    protected LA addMAProp(String title, boolean noWait, Object... params) {
        return addMAProp(null, LocalizedString.NONAME, title, noWait, params);
    }

    protected LA addMAProp(AbstractGroup group, LocalizedString caption, String title, boolean noWait, Object... params) {
        return addJoinAProp(group, caption, addMAProp(title, noWait), params);
    }

    @IdentityStrongLazy
    protected LA addMAProp(String title, boolean noWait) {
        return addProperty(null, new LA(new MessageAction(LocalizedString.create("Message"), title, noWait)));
    }

    public LA addFocusActionProp(PropertyDrawEntity propertyDrawEntity) {
        return addProperty(null, new LA(new FocusActionProperty(propertyDrawEntity)));
    }

    // ------------------- CONFIRM ----------------- //


    protected LA addConfirmAProp(String title, boolean yesNo, LP targetProp, Object... params) {
        return addConfirmAProp(null, LocalizedString.NONAME, title, yesNo, targetProp, params);
    }

    protected LA addConfirmAProp(AbstractGroup group, LocalizedString caption, String title, boolean yesNo, LP<?> targetProp, Object... params) {
        return addJoinAProp(group, caption, addConfirmAProp(title, yesNo, targetProp != null ? targetProp.property : null), params);
    }

    @IdentityStrongLazy
    protected LA addConfirmAProp(String title, boolean yesNo, Property property) {
        return addProperty(null, new LA(new ConfirmActionProperty(LocalizedString.create("Confirm"), title, yesNo, property != null ? new LP(property) : null)));
    }

    // ------------------- Async Update Action ----------------- //

    protected LA addAsyncUpdateAProp(Object... params) {
        return addAsyncUpdateAProp(LocalizedString.NONAME, params);
    }

    protected LA addAsyncUpdateAProp(LocalizedString caption, Object... params) {
        return addAsyncUpdateAProp(null, caption, params);
    }

    protected LA addAsyncUpdateAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        return addJoinAProp(group, caption, addAsyncUpdateAProp(), params);
    }

    @IdentityStrongLazy
    protected LA addAsyncUpdateAProp() {
        return addProperty(null, new LA(new AsyncUpdateEditValueAction(LocalizedString.create("Async Update"))));
    }

    // ------------------- EVAL ----------------- //

    public LA addEvalAProp(LP<?> scriptSource, List<LP<?>> params, boolean action) {
        return addAProp(null, new EvalActionProperty(LocalizedString.NONAME, scriptSource, params, action));
    }

    // ------------------- DRILLDOWN ----------------- //

    public void setupDrillDownProperty(Property property, boolean isLightStart) {
        if (property.supportsDrillDown()) {
            LA<?> drillDownFormProperty = isLightStart ? addLazyAProp((Property) property) : addDDAProp((Property) property);
            Action formProperty = drillDownFormProperty.property;
            property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
            property.setEditAction(formProperty.getSID(), formProperty.getImplement(property.getReflectionOrderInterfaces()));
        }
    }
    
    public LA addDrillDownAProp(LP<?> property) {
        return addDDAProp(property);
    }

    public LA<?> addDDAProp(LP property) {
        assert property.property.getReflectionOrderInterfaces().equals(property.listInterfaces);
        if (property.property instanceof Property && ((Property) property.property).supportsDrillDown())
            return addDDAProp((Property) property.property);
        else 
            throw new UnsupportedOperationException();
    }

    private String nameForDrillDownAction(Property property, List<ResolveClassSet> signature) {
        assert property.isNamed();
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(property.getCanonicalName(), baseLM.getClassFinder());
        String name = PropertyCanonicalNameUtils.drillDownPrefix + parser.getNamespace() + "_" + property.getName();
        signature.addAll(parser.getSignature());
        return name;
    }

    public LA<?> addDDAProp(Property property) {
        List<ResolveClassSet> signature = new ArrayList<>();
        DrillDownFormEntity drillDownFormEntity = property.getDrillDownForm(this, null);
        LA result = addMFAProp(baseLM.drillDownGroup, LocalizedString.create("{logics.property.drilldown.action}"), drillDownFormEntity, drillDownFormEntity.paramObjects, property.drillDownInNewSession());
        if (property.isNamed()) {
            String name = nameForDrillDownAction(property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    public LA<?> addLazyAProp(Property property) {
        LA result = addAProp(null, new LazyActionProperty(LocalizedString.create("{logics.property.drilldown.action}"), property));
        if (property.isNamed()) {
            List<ResolveClassSet> signature = new ArrayList<>();
            String name = nameForDrillDownAction(property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    public SessionDataProperty getAddedObjectProperty() {
        return baseLM.getAddedObjectProperty();
    }

    public LP getIsActiveFormProperty() {
        return baseLM.getIsActiveFormProperty();
    }

    // ---------------------- VALUE ---------------------- //

    public LP getObjValueProp(FormEntity formEntity, ObjectEntity obj) {
        return baseLM.getObjValueProp(formEntity, obj);
    }

    // ---------------------- Add Object ---------------------- //

    public <T extends PropertyInterface, I extends PropertyInterface> LA addAddObjAProp(CustomClass cls, boolean autoSet, int resInterfaces, boolean conditional, boolean resultExists, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<T, PropertyInterface> resultPart = (PropertyMapImplement<T, PropertyInterface>)
                (resultExists ? readImplements.get(resInterfaces) : null);
        PropertyMapImplement<T, PropertyInterface> conditionalPart = (PropertyMapImplement<T, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + (resultExists ? 1 : 0)) : null);

        return addAProp(null, new AddObjectAction(cls, innerInterfaces.getSet(), readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart, resultPart, MapFact.<PropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false, autoSet));
    }

    public LA getAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass explicitClass) {
        return baseLM.getAddObjectAction(formEntity, obj, explicitClass);
    }

    // ---------------------- Delete Object ---------------------- //

    public LA addDeleteAction(CustomClass cls, FormSessionScope scope) {
//        LA delete = addChangeClassAProp(baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
//
//        LA<?> result = addIfAProp(LocalizedString.create("{logics.delete}"), baseLM.sessionOwners, // IF sessionOwners() THEN 
//                delete, 1, // DELETE
//                addListAProp( // ELSE
//                        addConfirmAProp("lsFusion", addCProp(StringClass.text, LocalizedString.create("{form.instance.do.you.really.want.to.take.action} '{logics.delete}'"))), // CONFIRM
//                        addIfAProp(baseLM.confirmed, // IF confirmed() THEN
//                                addListAProp(
//                                        delete, 1, // DELETE
//                                        baseLM.apply), 1), 1 // apply()
//                ), 1);
        
        LA<?> result = addDeleteAProp(LocalizedString.create("{logics.delete}"), cls);

        result.property.setSimpleDelete(true);
        setDeleteActionOptions(result);

        return addSessionScopeAProp(scope, result);
    }

    protected void setDeleteActionOptions(LA property) {
        setFormActions(property);

        property.setImage("delete.png");
        property.setChangeKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        property.setShowChangeKey(false);
    }

    // ---------------------- Add Form ---------------------- //

    protected LA addAddFormAction(CustomClass cls, ObjectEntity contextObject, FormSessionScope scope) {
        LocalizedString caption = LocalizedString.NONAME;

        // NEW AUTOSET x=X DO {
        //      REQUEST
        //          edit(x);
        //      DO
        //          SEEK co=x;
        //      ELSE
        //          IF sessionOwners THEN
        //              DELETE x;
        // }

        LA result = addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, 0, cls, true, false, 0, false,
                1, //NEW x=X
                addRequestAProp(null, caption, // REQUEST
                        baseLM.getPolyEdit(), 1, // edit(x);
                        (contextObject != null ? addOSAProp(contextObject, UpdateType.LAST, 1) : baseLM.getEmptyObject()), 1, // DO SEEK co = x
                        addIfAProp(baseLM.sessionOwners, baseLM.getPolyDelete(), 1), 1 // ELSE IF seekOwners THEN delete(x)
                ), 1
        );
//        LA result = addListAProp(
//                            addAddObjAProp(cls, true, 0, false, true, addedProperty), // NEW (FORM with AUTOSET), addAddObjAProp(cls, false, true, 0, false, true, addedProperty),
//                            addJoinAProp(addListAProp( // так хитро делается чтобы заnest'ить addedProperty (иначе apply его сбрасывает)
//                                    addDMFAProp(caption, cls, ManageSessionType.AUTO, true), 1, // FORM EDIT class OBJECT prm
//                                    addSetPropertyAProp(1, false, 1, addedProperty, 1), 1), // addedProperty <- prm
//                            addedProperty)); // FORM EDIT class OBJECT prm
//
//        LP formResultProperty = baseLM.getFormResultProperty();
//        result = addListAProp(LocalizedString.create("{logics.add}"), result,
//                addIfAProp(addJProp(baseLM.equals2, formResultProperty, addCProp(baseLM.formResult, "ok")), // IF formResult == ok
//                        (contextObject != null ? addJoinAProp(addOSAProp(contextObject, true, 1), addedProperty) : baseLM.getEmpty()), // THEN (contextObject != null) SEEK exf.o prm
//                        (addIfAProp(baseLM.sessionOwners, addJoinAProp(getDeleteAction(cls, contextObject, FormSessionScope.OLDSESSION), addedProperty)))) // ELSE IF sessionOwners DELETE prm, // предполагается что если нет
//                         );

        setAddActionOptions(result, contextObject);
        
        return addSessionScopeAProp(scope, result);
    }

    protected void setAddActionOptions(LA property, final ObjectEntity objectEntity) {

        setFormActions(property);

        property.setImage("add.png");
        property.setChangeKey(KeyStrokes.getAddActionPropertyKeyStroke());
        property.setShowChangeKey(false);

        if(objectEntity != null) {
            property.addProcessor(new ActionOrProperty.DefaultProcessor() {
                public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                    if(entity.toDraw == null)
                        entity.toDraw = objectEntity.groupTo;
                }
                public void proceedDefaultDesign(PropertyDrawView propertyView) {
                }
            });
        }
    }

    // ---------------------- Edit Form ---------------------- //

    protected LA addEditFormAction(FormSessionScope scope, CustomClass customClass) {
        LA result = addEditAProp(LocalizedString.create("{logics.edit}"), customClass);

        setEditActionOptions(result);

        return addSessionScopeAProp(scope, result);
    }
    
    private void setFormActions(LA result) {
        result.setShouldBeLast(true);
        result.setForceViewType(ClassViewType.TOOLBAR);
    }

    private void setEditActionOptions(LA result) {
        setFormActions(result);
        
        result.setImage("edit.png");
        result.setChangeKey(KeyStrokes.getEditActionPropertyKeyStroke());
        result.setShowChangeKey(false);
    }

    public LA addProp(Action prop) {
        return addProp(null, prop);
    }

    public LA addProp(AbstractGroup group, Action prop) {
        return addProperty(group, new LA(prop));
    }

    public LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(AbstractGroup group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    protected void addPropertyToGroup(ActionOrProperty<?> property, AbstractGroup group) {
        Version version = getVersion();
        if (group != null) {
            group.add(property, version);
        } else if (!property.isLocal() && !temporary) {
            baseLM.privateGroup.add(property, version);
        }
    }

    protected <T extends LAP<?, ?>> T addProperty(AbstractGroup group, T lp) {
        addPropertyToGroup(lp.property, group);
        return lp;
    }

    public void addIndex(LP lp) {
        ImOrderSet<String> keyNames = SetFact.toOrderExclSet(lp.listInterfaces.size(), new GetIndex<String>() {
            public String getMapValue(int i) {
                return "key"+i;
            }});
        addIndex(keyNames, directLI(lp));
    }

    public void addIndex(Property property) {
        addIndex(new LP(property));
    }

    public void addIndex(ImOrderSet<String> keyNames, Object... params) {
        ImList<PropertyObjectInterfaceImplement<String>> index = ActionOrPropertyUtils.readObjectImplements(keyNames, params);
        ThreadLocalContext.getDbManager().addIndex(index);
    }

    protected void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property, null);
    }

    protected void addPersistent(LP lp, ImplementTable table) {
        addPersistent((AggregateProperty) lp.property, table);
    }

    private void addPersistent(AggregateProperty property, ImplementTable table) {
        assert property.isNamed();

        logger.debug("Initializing stored property " + property + "...");
        property.markStored(baseLM.tableFactory, table);
    }

    // нужен так как иначе начинает sID расширять

    public <T extends PropertyInterface> LP<T> addOldProp(LP<T> lp, PrevScope scope) {
        return baseLM.addOldProp(lp, scope);
    }

    public <T extends PropertyInterface> LP<T> addCHProp(LP<T> lp, IncrementType type, PrevScope scope) {
        return baseLM.addCHProp(lp, type, scope);
    }

    public <T extends PropertyInterface> LP addClassProp(LP<T> lp) {
        return baseLM.addClassProp(lp);
    }

    @IdentityStrongLazy // для ID
    public LP addGroupObjectProp(GroupObjectEntity groupObject, GroupObjectProp prop) {
        PropertyRevImplement<ClassPropertyInterface, ObjectEntity> filterProperty = groupObject.getProperty(prop);
        return addProperty(null, new LP<>(filterProperty.property, groupObject.getOrderObjects().mapOrder(filterProperty.mapping.reverse())));
    }
    
    protected LA addOSAProp(ObjectEntity object, UpdateType type, Object... params) {
        return addOSAProp(null, LocalizedString.NONAME, object, type, params);
    }

    protected LA addOSAProp(AbstractGroup group, LocalizedString caption, ObjectEntity object, UpdateType type, Object... params) {
        return addJoinAProp(group, caption, addOSAProp(object, type), params);
    }

    @IdentityStrongLazy // для ID
    public LA addOSAProp(ObjectEntity object, UpdateType type) {
        SeekObjectActionProperty seekProperty = new SeekObjectActionProperty(object, type);
        return addProperty(null, new LA<>(seekProperty));
    }

    protected LA addGOSAProp(GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type, Object... params) {
        return addGOSAProp(null, LocalizedString.NONAME, object, objects, type, params);
    }

    protected LA addGOSAProp(AbstractGroup group, LocalizedString caption, GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type, Object... params) {
        return addJoinAProp(group, caption, addGOSAProp(object, objects, type), params);
    }

    @IdentityStrongLazy // для ID
    public LA addGOSAProp(GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type) {
        List<ValueClass> objectClasses = new ArrayList<>();
        for (ObjectEntity obj : objects) {
            objectClasses.add(obj.baseClass);
        }
        SeekGroupObjectActionProperty seekProperty = new SeekGroupObjectActionProperty(object, objects, type, objectClasses.toArray(new ValueClass[objectClasses.size()]));
        return addProperty(null, new LA<>(seekProperty));
    }

    public void addConstraint(Property property, boolean checkChange) {
        addConstraint(property, null, checkChange);
    }

    public void addConstraint(Property property, Property messageProperty, boolean checkChange) {
        addConstraint(property, messageProperty, checkChange, null);
    }

    public void addConstraint(Property property, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(addProp(property), null, checkChange, debugPoint);
    }

    public void addConstraint(Property property, Property messageProperty, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(addProp(property), messageProperty == null ? null : addProp(messageProperty), checkChange, debugPoint);
    }

    public void addConstraint(LP<?> lp, LP<?> messageLP, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(lp, messageLP, (checkChange ? Property.CheckType.CHECK_ALL : Property.CheckType.CHECK_NO), null, Event.APPLY, this, debugPoint);
    }

    protected void addConstraint(LP<?> lp, LP<?> messageLP, Property.CheckType type, ImSet<Property<?>> checkProps, Event event, LogicsModule lm, DebugInfo.DebugPoint debugPoint) {
        if(!(lp.property).noDB())
            lp = addCHProp(lp, IncrementType.SET, event.getScope());
        // assert что lp уже в списке properties
        setConstraint(lp.property, messageLP == null ? null : messageLP.property, type, event, checkProps, debugPoint);
    }

    public <T extends PropertyInterface> void setConstraint(Property property, Property messageProperty, Property.CheckType type, Event event, ImSet<Property<?>> checkProperties, DebugInfo.DebugPoint debugPoint) {
        assert type != Property.CheckType.CHECK_SOME || checkProperties != null;
        assert property.noDB();

        property.checkChange = type;
        property.checkProperties = checkProperties;

        ActionMapImplement<ClassPropertyInterface, ClassPropertyInterface> logAction;
//            logAction = new LogPropertyActionProperty<T>(property, messageProperty).getImplement();
        //  PRINT OUT property MESSAGE NOWAIT;
        logAction = (ActionMapImplement<ClassPropertyInterface, ClassPropertyInterface>) addPFAProp(null, LocalizedString.concat("Constraint - ",property.caption), new OutFormSelector<T>(property, messageProperty), ListFact.<ObjectSelector>EMPTY(), ListFact.<Boolean>EMPTY(), null, null, FormPrintType.MESSAGE, false, 30, null, null, true).property.getImplement();
        ActionMapImplement<?, ClassPropertyInterface> constraintAction =
                DerivedProperty.createListAction(
                        SetFact.<ClassPropertyInterface>EMPTY(),
                        ListFact.toList(logAction,
                                baseLM.cancel.property.getImplement(SetFact.<ClassPropertyInterface>EMPTYORDER())
                        )
                );
        constraintAction.mapEventAction(this, DerivedProperty.createAnyGProp(property).getImplement(), event, true, debugPoint);
        addProp(constraintAction.property);
    }

    public <T extends PropertyInterface> void addEventAction(Event event, boolean descending, boolean ordersNotNull, int noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));

        ImList<lsfusion.server.logics.property.oraction.PropertyInterfaceImplement> listImplements = readImplements(innerInterfaces, params);
        int implCnt = listImplements.size();

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = BaseUtils.immutableCast(listImplements.subList(2, implCnt - noInline).toOrderSet().toOrderMap(descending));

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(listImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        addEventAction(innerInterfaces.getSet(), (ActionMapImplement<?, PropertyInterface>) listImplements.get(0), (PropertyMapImplement<?, PropertyInterface>) listImplements.get(1), orders, ordersNotNull, event, noInlineInterfaces, forceInline, false, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(Action<P> actionProperty, PropertyMapImplement<?, P> whereImplement, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        addEventAction(actionProperty.interfaces, actionProperty.getImplement(), whereImplement, orders, ordersNotNull, event, SetFact.<P>EMPTY(), false, resolve, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ImSet<P> innerInterfaces, ActionMapImplement<?, P> actionProperty, PropertyMapImplement<?, P> whereImplement, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, ImSet<P> noInline, boolean forceInline, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        if(!(whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, event.getScope());

        Action<? extends PropertyInterface> action =
                innerInterfaces.isEmpty() ?
                    DerivedProperty.createIfAction(innerInterfaces, whereImplement, actionProperty, null).property :
                    DerivedProperty.createForAction(innerInterfaces, SetFact.<P>EMPTY(), whereImplement, orders, ordersNotNull, actionProperty, null, false, noInline, forceInline).property;

        if(debugPoint != null) { // создано getEventDebugPoint
            if(debugger.isEnabled()) // topContextActionPropertyDefinitionBodyCreated
                debugger.setNewDebugStack(action);

            assert action.getDelegationType(true) == ActionDelegationType.AFTER_DELEGATE;
            ScriptingLogicsModule.setDebugInfo(true, debugPoint, action); // actionPropertyDefinitionBodyCreated
        }

//        action.setStrongUsed(whereImplement.property); // добавить сильную связь, уже не надо поддерживается более общий механизм - смотреть на Session Calc
//        action.caption = "WHEN " + whereImplement.property + " " + actionProperty;
        addProp(action);

        addBaseEvent(action, event, resolve, false);
    }

    public <P extends PropertyInterface> void addBaseEvent(Action<P> action, Event event, boolean resolve, boolean single) {
        action.addEvent(event.base, event.session);
        if(event.after != null)
            action.addStrongUsed(event.after);
        action.singleApply = single;
        action.resolve = resolve;
    }

    public <P extends PropertyInterface> void addAspectEvent(int interfaces, ActionImplement<P, Integer> action, String mask, boolean before) {
        // todo: непонятно что пока с полными каноническими именами и порядками параметров делать
    }

    public <P extends PropertyInterface, T extends PropertyInterface> void addAspectEvent(Action<P> action, ActionMapImplement<T, P> aspect, boolean before) {
        if(before)
            action.addBeforeAspect(aspect);
        else
            action.addAfterAspect(aspect);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, Integer... mapping) {
        follows(first, null, ListFact.toList(new PropertyFollowsDebug(true, null), new PropertyFollowsDebug(false, null)), Event.APPLY, second, mapping);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(final LP<T> first, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event, LP<L> second, final Integer... mapping) {
        addFollows(first.property, new PropertyMapImplement<>(second.property, second.getRevMap(first.listInterfaces, mapping)), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void setNotNull(Property<T> property, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        PropertyMapImplement<L, T> mapClasses = (PropertyMapImplement<L, T>) IsClassProperty.getMapProperty(property.getInterfaceClasses(ClassType.logPolicy));
        property.setNotNull = true;
        addFollows(mapClasses.property, new PropertyMapImplement<>(property, mapClasses.mapping.reverse()),
                LocalizedString.concatList(LocalizedString.create("{logics.property} "), property.caption, " [" + property.getSID(), LocalizedString.create("] {logics.property.not.defined}")),
                debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(Property<T> property, PropertyMapImplement<L, T> implement, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        addFollows(property, implement, LocalizedString.create("{logics.property.violated.consequence.from}" + "(" + this + ") => (" + implement.property + ")"), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(Property<T> property, PropertyMapImplement<L, T> implement, LocalizedString caption, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
//        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);

        for(PropertyFollowsDebug option : options) {
            assert !option.isTrue || property.interfaces.size() == implement.mapping.size(); // assert что количество
            ActionMapImplement<?, T> setAction = option.isTrue ? implement.getSetNotNullAction(true) : property.getSetNotNullAction(false);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE " + option.isTrue + " : " + property + " => " + implement.property;
                PropertyMapImplement<?, T> condition;
                if(option.isFull)
                    condition = DerivedProperty.createAndNot(property, implement).mapChanged(IncrementType.SET, event.getScope());
                else {
                    if (option.isTrue)
                        condition = DerivedProperty.createAndNot(property.getChanged(IncrementType.SET, event.getScope()), implement);
                    else
                        condition = DerivedProperty.createAnd(property, implement.mapChanged(IncrementType.DROP, event.getScope()));
                }
                setAction.mapEventAction(this, condition, event, true, option.debugPoint);
            }
        }

        Property constraint = DerivedProperty.createAndNot(property, implement).property;
        constraint.caption = caption;
        addConstraint(constraint, false, debugPoint);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LP<P> lp, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp, null, Event.APPLY, resolve);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LP<P> lp, DebugInfo.DebugPoint debugPoint, Event event, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp.property, debugPoint, resolve, event);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> ActionMapImplement<P, T> mapActionListImplement(LA<P> property, ImOrderSet<T> mapList) {
        return new ActionMapImplement<>(property.property, getMapping(property, mapList));
    }
    public static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<P, T> mapCalcListImplement(LP<P> property, ImOrderSet<T> mapList) {
        return new PropertyMapImplement<>(property.property, getMapping(property, mapList));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> ImRevMap<P, T> getMapping(LAP<P, ?> property, final ImOrderSet<T> mapList) {
        return property.getRevMap(mapList);
    }

    protected void makeUserLoggable(SystemEventsLogicsModule systemEventsLM, LP... lps) {
        for (LP lp : lps)
            lp.makeUserLoggable(this, systemEventsLM);
    }

    public LP not() {
        return baseLM.not();
    }

    // получает свойство is
    public LP<?> is(ValueClass valueClass) {
        return baseLM.is(valueClass);
    }

    public LP object(ValueClass valueClass) {
        return baseLM.object(valueClass);
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    protected NavigatorElement addNavigatorFolder(String canonicalName, LocalizedString caption) {
        NavigatorElement elem = new NavigatorFolder(canonicalName, caption);
        addNavigatorElement(elem);
        return elem;
    }

    protected NavigatorAction addNavigatorAction(LA<?> property, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorAction = new NavigatorAction(property.property, canonicalName, caption, null, "/images/action.png", DefaultIcon.ACTION);
        addNavigatorElement(navigatorAction);
        return navigatorAction;
    }

    protected LA<?> getNavigatorAction(FormEntity form) {
        return baseLM.getFormNavigatorAction(form);
    }

    protected NavigatorElement addNavigatorForm(FormEntity form, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorForm = new NavigatorAction(getNavigatorAction(form).property, canonicalName, caption, form, "/images/form.png", DefaultIcon.FORM);

        addNavigatorElement(navigatorForm);
        return navigatorForm;
    }
    
    public Collection<NavigatorElement> getNavigatorElements() {
        return navigatorElements.values();
    }

    public Collection<FormEntity> getNamedForms() {
        return namedForms.values();
    } 
    
    public Collection<ImplementTable> getTables() {
        return tables.values();    
    }
    
    // в том числе и приватные 
    public Collection<FormEntity> getAllModuleForms() {
        List<FormEntity> elements = new ArrayList<>();
        elements.addAll(unnamedForms);
        elements.addAll(namedForms.values());
        return elements;
    }
    
    public NavigatorElement getNavigatorElement(String name) {
        return navigatorElements.get(name);
    }

    public FormEntity getForm(String name) {
        return namedForms.get(name);
    }
    
    public <T extends FormEntity> T addFormEntity(T form) {
        if (form.isNamed()) {
            addNamedForm(form);
        } else {
            addPrivateForm(form);
        }
        return form;
    }
    
    @NFLazy
    private void addNavigatorElement(NavigatorElement element) {
        assert !navigatorElements.containsKey(element.getName());
        navigatorElements.put(element.getName(), element);
    }

    @NFLazy
    private void addNamedForm(FormEntity form) {
        assert !namedForms.containsKey(form.getName());
        namedForms.put(form.getName(), form);
    }
    
    @NFLazy
    private void addPrivateForm(FormEntity form) {
        unnamedForms.add(form);
    }

    public void addFormActions(FormEntity form, ObjectEntity object, FormSessionScope scope) {
        Version version = getVersion();
        form.addPropertyDraw(getAddFormAction(form, object, null, scope, version), version);
        form.addPropertyDraw(getEditFormAction(object, null, scope, version), version, object);
        form.addPropertyDraw(getDeleteAction(object, scope), version, object);
    }

    public LA getAddFormAction(FormEntity contextForm, ObjectEntity contextObject, CustomClass explicitClass, FormSessionScope scope, Version version) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass)contextObject.baseClass;
        return baseLM.getAddFormAction(cls, contextForm, contextObject, scope);
    }

    public LA getEditFormAction(ObjectEntity object, CustomClass explicitClass, FormSessionScope scope, Version version) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass) object.baseClass;
        return baseLM.getEditFormAction(cls, scope);
    }

    public LA getDeleteAction(ObjectEntity object, FormSessionScope scope) {
        CustomClass cls = (CustomClass) object.baseClass;
        return getDeleteAction(cls, object, scope);
    }
    public LA getDeleteAction(CustomClass cls, ObjectEntity object, FormSessionScope scope) {
        return baseLM.getDeleteAction(cls, scope);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(boolean defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public Set<String> getRequiredNames() {
        return requiredNames;
    }

    public void setRequiredNames(LinkedHashSet<String> requiredNames) {
        this.requiredNames = requiredNames;
    }

    public List<String> getNamespacePriority() {
        return namespacePriority;
    }

    public void setNamespacePriority(List<String> namespacePriority) {
        this.namespacePriority = namespacePriority;
    }

    public List<ResolveClassSet> getParamClasses(LAP<?, ?> lp) {
        List<ResolveClassSet> paramClasses;
        if (lp instanceof LP && locals.containsKey(lp)) {
            paramClasses = locals.get(lp).signature;
        } else {
            paramClasses = propClasses.get(lp);
        }
        return paramClasses == null ? Collections.<ResolveClassSet>nCopies(lp.listInterfaces.size(), null) : paramClasses;                   
    }

    // для обратной совместимости
    public void addFormFixedFilter(FormEntity form, FilterEntity filter) {
        form.addFixedFilter(filter, getVersion());
    }

    public RegularFilterGroupEntity newRegularFilterGroupEntity(int id) {
        return new RegularFilterGroupEntity(id, getVersion());
    }

    public void addFormHintsIncrementTable(FormEntity form, LP... lps) {
        form.addHintsIncrementTable(getVersion(), lps);
    }

    public int getModuleComplexity() {
        return 1;
    }
    
    public List<LogicsModule> getRequiredModules(String namespace) {
        return namespaceToModules.get(namespace);
    }

    public Set<String> getRequiredNamespaces() {
        return namespaceToModules.keySet();
    }
    
    public Map<String, List<LogicsModule>> getNamespaceToModules() {
        return namespaceToModules;
    }

    public BaseClass getBaseClass() {
        return baseLM.baseClass;
    }

    public AbstractGroup getRootGroup() {
        return baseLM.rootGroup;
    }

    public AbstractGroup getPublicGroup() {
        return baseLM.publicGroup;
    }

    public AbstractGroup getBaseGroup() {
        return baseLM.baseGroup;
    }

    public AbstractGroup getRecognizeGroup() {
        return baseLM.recognizeGroup;
    }

    public static class LocalPropertyData {
        public String name;
        public List<ResolveClassSet> signature;

        public LocalPropertyData(String name, List<ResolveClassSet> signature) {
            this.name = name;
            this.signature = signature;
        }
    }

    protected <P extends PropertyInterface> void addLocal(LP<P> lcp, LocalPropertyData data) {
        locals.put(lcp, data);
        lcp.property.setCanonicalName(getNamespace(), data.name, data.signature, lcp.listInterfaces, baseLM.getDBNamingPolicy());
    }

    protected void removeLocal(LP<?> lcp) {
        assert locals.containsKey(lcp);
        locals.remove(lcp);
    }

    public List<ResolveClassSet> getLocalSignature(LP<?> lcp) {
        assert locals.containsKey(lcp);
        return locals.get(lcp).signature;
    }

    public Map<LP<?>, LocalPropertyData> getLocals() {
        return locals;
    }

    public LP<?> resolveProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findProperty(compoundName, params);
    }

    public LP<?> resolveAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractProperty(compoundName, params, prioritizeNotEquals);
    }

    public LA<?> resolveAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAction(compoundName, params);
    }

    public LA<?> resolveAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractAction(compoundName, params, prioritizeNotEquals);
    }

    public ValueClass resolveClass(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findClass(compoundName);
    }

    public MetaCodeFragment resolveMetaCodeFragment(String compoundName, int paramCnt) throws ResolvingErrors.ResolvingError {
        return resolveManager.findMetaCodeFragment(compoundName, paramCnt);
    }
    
    public AbstractGroup resolveGroup(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findGroup(compoundName);
    }
    
    public AbstractWindow resolveWindow(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findWindow(compoundName);    
    }
    
    public FormEntity resolveForm(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findForm(compoundName);
    }
    
    public NavigatorElement resolveNavigatorElement(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findNavigatorElement(compoundName);
    } 
    
    public ImplementTable resolveTable(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findTable(compoundName);
    }
}
