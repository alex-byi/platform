package platform.server.logics.scripted;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.interop.ModalityType;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.classes.sets.UpClassSet;
import platform.server.data.Union;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.type.ConcatenateType;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.classes.AbstractClassWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.ActionPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.window.*;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.PanelLocation;
import platform.server.logics.panellocation.ShortcutPanelLocation;
import platform.server.logics.panellocation.ToolbarPanelLocation;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.ListActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.table.ImplementTable;
import platform.server.mail.AttachmentFormat;
import platform.server.mail.EmailActionProperty;
import platform.server.mail.EmailActionProperty.FormStorageType;

import javax.mail.Message;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static platform.base.BaseUtils.*;
import static platform.server.logics.PropertyUtils.*;
import static platform.server.logics.scripted.ScriptingLogicsModule.InsertPosition.IN;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {

    private final static Logger scriptLogger = Logger.getLogger(ScriptingLogicsModule.class);

    private final CompoundNameResolver<LP<?, ?>> lpResolver = new LPNameResolver();
    private final CompoundNameResolver<AbstractGroup> groupResolver = new AbstractGroupNameResolver();
    private final CompoundNameResolver<NavigatorElement> navigatorResolver = new NavigatorElementNameResolver();
    private final CompoundNameResolver<AbstractWindow> windowResolver = new WindowNameResolver();
    private final MetaCodeFragmentNameResolver metaCodeFragmentResolver = new MetaCodeFragmentNameResolver();
    private final CompoundNameResolver<ImplementTable> tableResolver = new TableNameResolver();
    private final CompoundNameResolver<ValueClass> classResolver = new ClassNameResolver();

    private String code = null;
    private String filename = null;
    private final BusinessLogics<?> BL;
    private List<String> namespacePriority;
    private final ScriptingErrorLog errLog;
    private ScriptParser parser;
    private List<String> warningList = new ArrayList<String>();

    private Map<String, LP<?, ?>> currentLocalProperties = new HashMap<String, LP<?, ?>>();

    private Map<String, List<LogicsModule>> namespaceToModules = new LinkedHashMap<String, List<LogicsModule>>();

    public BusinessLogics<?> getBL() {
        return BL;
    }

    public enum ConstType { INT, REAL, STRING, LOGICAL, ENUM, LONG, DATE, DATETIME, COLOR, NULL }
    public enum InsertPosition {IN, BEFORE, AFTER}
    public enum WindowType {MENU, PANEL, TOOLBAR, TREE}
    public enum GroupingType {SUM, MAX, MIN, CONCAT, UNIQUE, EQUAL}

    private Map<String, DataClass> primitiveTypeAliases = BaseUtils.buildMap(
            asList("INTEGER", "DOUBLE", "LONG", "DATE", "BOOLEAN", "DATETIME", "TEXT", "TIME", "WORDFILE", "IMAGEFILE", "PDFFILE", "CUSTOMFILE", "EXCELFILE", "COLOR"),
            Arrays.<DataClass>asList(IntegerClass.instance, DoubleClass.instance, LongClass.instance, DateClass.instance, LogicalClass.instance,
                    DateTimeClass.instance, TextClass.instance, TimeClass.instance, WordClass.instance, ImageClass.instance, PDFClass.instance,
                    DynamicFormatFileClass.instance, ExcelClass.instance, ColorClass.instance)
    );

    private ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        setBaseLogicsModule(baseModule);
        this.BL = BL;
        errLog = new ScriptingErrorLog("");
        parser = new ScriptParser(errLog);
    }

    public ScriptingLogicsModule(String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        this(baseModule, BL);
        this.filename = filename;
    }

    public ScriptingLogicsModule(InputStream stream, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(stream, "utf-8", baseModule, BL);
    }

    public ScriptingLogicsModule(InputStream stream, String charsetName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) throws IOException {
        this(baseModule, BL);
        this.code = IOUtils.readStreamToString(stream, charsetName);
    }

    public ScriptingLogicsModule(BaseLogicsModule<?> baseModule, BusinessLogics<?> BL, String code) {
        this(baseModule, BL);
        this.code = code;
    }

    private void setModuleName(String moduleName) {
        setName(moduleName);
        errLog.setModuleName(moduleName);
    }

    private CharStream createStream() throws IOException {
        if (code != null) {
            return new ANTLRStringStream(code);
        } else {
            return new ANTLRFileStream(filename, "UTF-8");
        }
    }

    public ScriptingErrorLog getErrLog() {
        return errLog;
    }

    public ScriptParser getParser() {
        return parser;
    }

    protected LogicsModule findModule(String name) throws ScriptingErrorLog.SemanticErrorException {
        LogicsModule module = BL.getModule(name);
        checkModule(module, name);
        return module;
    }

    public String transformStringLiteral(String captionStr) {
        String caption = captionStr.replace("\\'", "'");
        caption = caption.replace("\\n", "\n");
        caption = caption.replace("\\r", "\r");
        caption = caption.replace("\\t", "\t");
        return caption.substring(1, caption.length()-1);
    }

    private DataClass getPredefinedClass(String name) {
        if (primitiveTypeAliases.containsKey(name)) {
            return primitiveTypeAliases.get(name);
        } else if (name.startsWith("STRING[")) {
            name = name.substring("STRING[".length(), name.length() - 1);
            return StringClass.get(Integer.parseInt(name));
        } else if (name.startsWith("ISTRING[")) {
            name = name.substring("ISTRING[".length(), name.length() - 1);
            return InsensitiveStringClass.get(Integer.parseInt(name));
        } else if (name.startsWith("NUMERIC[")) {
            String length = name.substring("NUMERIC[".length(), name.indexOf(","));
            String precision = name.substring(name.indexOf(",") + 1, name.length() - 1);
            return NumericClass.get(Integer.parseInt(length), Integer.parseInt(precision));
        }
        return null;
    }

    private Type getPredefinedType(String name) {
        if ("OBJECT".equals(name)) {
            return ObjectType.instance;
        } else {
            return getPredefinedClass(name);
        }
    }

    public ObjectEntity[] getMappingObjectsArray(FormEntity form, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity[] objects = new ObjectEntity[mapping.size()];
        for (int i = 0; i < mapping.size(); i++) {
            objects[i] = getObjectEntityByName(form, mapping.get(i));
        }
        return objects;
    }

    public ObjectEntity getObjectEntityByName(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity obj = form.getObject(name);
        if (obj == null) {
            getErrLog().emitObjectNotFoundError(parser, name);
        }
        return obj;
    }

    public MappedProperty getPropertyWithMapping(FormEntity form, String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        LP<?, ?> property = findLPByCompoundName(name);
        if (property.property.interfaces.size() != mapping.size()) {
            getErrLog().emitParamCountError(parser, property, mapping.size());
        }
        return new MappedProperty(property, getMappingObjectsArray(form, mapping));
    }

    public ValueClass findClassByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        ValueClass valueClass = getPredefinedClass(name);
        if (valueClass == null) {
            valueClass = classResolver.resolve(name);
        }
        checkClass(valueClass, name);
        return valueClass;
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract, boolean isStatic,
                                 List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedClass(" + className + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + isStatic + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        checkDuplicateClass(className);
        checkStaticClassConstraints(className, isStatic, isAbstract, instNames, instCaptions);
        checkClassParents(parentNames);

        String caption = (captionStr == null ? className : captionStr);

        CustomClass[] parents;
        if (!isStatic && parentNames.isEmpty()) {
            parents = new CustomClass[] {baseLM.baseClass};
        } else {
            parents = new CustomClass[parentNames.size()];
            for (int i = 0; i < parentNames.size(); i++) {
                String parentName = parentNames.get(i);
                parents[i] = (CustomClass) findClassByCompoundName(parentName);
            }
        }

        assert !(isStatic && isAbstract);
        if (isStatic) {
            String[] captions = new String[instCaptions.size()];
            for (int i = 0; i < instCaptions.size(); i++) {
                captions[i] = (instCaptions.get(i) == null ? null : instCaptions.get(i));
            }
            addStaticClass(className, caption, instNames.toArray(new String[instNames.size()]), captions, parents);
        } else if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, parents);
        }
    }

    public void extendClass(String className, List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("extendClass(" + className + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        CustomClass cls = (CustomClass) findClassByCompoundName(className);
        boolean isStatic = cls instanceof StaticCustomClass;
        if (isStatic) {
            List<String> tmpNames = new ArrayList<String>(Arrays.asList(((StaticCustomClass) cls).getSids()));
            List<String> tmpCaptions = new ArrayList<String>(Arrays.asList(((StaticCustomClass) cls).getNames()));
            tmpNames.addAll(instNames);
            tmpCaptions.addAll(instCaptions);
            instNames = tmpNames;
            instCaptions = tmpCaptions;
        }

        checkStaticClassConstraints(className, isStatic, false, instNames, instCaptions);
        checkClassParents(parentNames);

        if (isStatic) {
            StaticCustomClass staticCls = (StaticCustomClass) cls;
            staticCls.changeInstances(instNames.toArray(new String[instNames.size()]), instCaptions.toArray(new String[instCaptions.size()]));
        }

        for (String parentName : parentNames) {
            CustomClass parentClass = (CustomClass) findClassByCompoundName(parentName);
            if (cls.parents.contains(parentClass)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            cls.addParentClass(parentClass);
        }
    }

    public AbstractGroup findGroupByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractGroup group = groupResolver.resolve(name);
        checkGroup(group, name);
        return group;
    }

    public LAP<?> findLAPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LAP<?>) findLPByCompoundName(name);
    }

    public LCP<?> findLCPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        return (LCP<?>) findLPByCompoundName(name);
    }

    public LP<?, ?> findLPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (currentLocalProperties.containsKey(name)) {
            return currentLocalProperties.get(name);
        }

        LP<?, ?> property = lpResolver.resolve(name);
        checkProperty(property, name);
        return property;
    }

    public Set<String> copyCurrentLocalProperties() {
        return new HashSet<String>(currentLocalProperties.keySet());
    }

    public AbstractWindow findWindowByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractWindow window = windowResolver.resolve(name);
        checkWindow(window, name);
        return window;
    }

    public FormEntity findFormByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement navigator = navigatorResolver.resolve(name);
        checkForm(navigator, name);
        return (FormEntity) navigator;
    }

    public MetaCodeFragment findMetaCodeFragmentByCompoundName(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        metaCodeFragmentResolver.setParamCnt(paramCnt); // todo [dale]: криво, надо по-хорошему как-то обобщить resolver
        MetaCodeFragment code = metaCodeFragmentResolver.resolve(name);
        checkMetaCodeFragment(code, name);
        return code;
    }

    public NavigatorElement findNavigatorElementByName(String name) throws ScriptingErrorLog.SemanticErrorException {
        NavigatorElement element = navigatorResolver.resolve(name);
        checkNavigatorElement(element, name);
        return element;
    }

    public ImplementTable findTableByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        ImplementTable table = tableResolver.resolve(name);
        checkTable(table, name);
        return table;
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGroup(" + groupName + ", " + (captionStr==null ? "" : captionStr) + ", " + (parentName == null ? "null" : parentName) + ");");
        checkDuplicateGroup(groupName);
        String caption = (captionStr == null ? groupName : captionStr);
        AbstractGroup parentGroup = (parentName == null ? null : findGroupByCompoundName(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, String caption, String title) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedForm(" + formName + ", " + caption + ", " + title + ");");
        checkDuplicateNavigatorElement(formName);
        caption = (caption == null ? formName : caption);
        return new ScriptingFormEntity(this, new FormEntity(null, formName, caption, title));
    }

    public ScriptingFormView createScriptedFormView(String formName, String caption, boolean applyDefault) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedFormView(" + formName + ", " + applyDefault + ");");

        FormEntity form = findFormByCompoundName(formName);
        FormView formView = applyDefault ? new DefaultFormView(form) : new FormView(form);
        ScriptingFormView scriptingView = new ScriptingFormView(formView, this);
        if (caption != null) {
            formView.caption = caption;
        }

        form.setRichDesign(formView);

        return scriptingView;
    }

    public ScriptingFormView getDesignForExtending(String formName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("getDesignForExtending(" + formName + ");");
        FormEntity form = findFormByCompoundName(formName);
        return new ScriptingFormView(form.getRichDesign(), this);
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedForm(" + form + ");");
        addFormEntity(form.getForm());
    }

    public ScriptingFormEntity getFormForExtending(String name) throws ScriptingErrorLog.SemanticErrorException {
        FormEntity form = findFormByCompoundName(name);
        return new ScriptingFormEntity(this, form);
    }

    public LCP addScriptedDProp(String returnClass, List<String> paramClasses, boolean sessionProp, boolean innerProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDProp(" + returnClass + ", " + paramClasses + ", " + innerProp + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }

        if (sessionProp) {
            return addSDProp(genSID(), "", value, params);
        } else {
            if (innerProp) {
                return addDProp(genSID(), "", value, params);
            } else {
                StoredDataProperty storedProperty = new StoredDataProperty(genSID(), "", params, value);
                return addProperty(null, new LCP<ClassPropertyInterface>(storedProperty));
            }
        }
    }

    public LP<?, ?> addScriptedAbstractProp(String returnClass, List<String> paramClasses, boolean isExclusive) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAbstractProp(" + returnClass + ", " + paramClasses + ", " + isExclusive + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }
        return addAUProp(null, genSID(), isExclusive, false, "", value, params);
    }

    public LP addScriptedAbstractActionProp(int paramCnt) {
        scriptLogger.info("addScriptedAbstractActionProp(" + paramCnt + ");");
        return addAbstractListAProp(paramCnt);
    }

    public void addImplementationToAbstract(String abstractPropName, List<String> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addImplementationToAbstract(" + abstractPropName + ", " + context + ", " + implement + ", " + when + ");");

        LP abstractLP = findLPByCompoundName(abstractPropName);
        checkParamCount(abstractLP, context.size());
        checkAbstractProperty(abstractLP, abstractPropName);

        List<LPWithParams> allProps = new ArrayList<LPWithParams>();
        allProps.add(implement);
        if(when!=null) {
            checkCalculationProperty(when.property);
            allProps.add(when);
        }
        List<Object> params = getParamsPlainList(allProps);
        if (abstractLP instanceof LCP) {
            checkCalculationProperty(implement.property);
            ((LCP) abstractLP).addOperand(when!=null, params.toArray());
        } else if (abstractLP instanceof LAP) {
            checkActionProperty(implement.property);
            ImList<ActionPropertyMapImplement<?, PropertyInterface>> actionImplements = readActionImplements(abstractLP.listInterfaces, params.toArray());
            ((ListActionProperty) abstractLP.property).addAction(actionImplements.get(0));
        } else assert false;
    }

    public int getParamIndex(String param, List<String> namedParams, boolean dynamic, boolean insideRecursion) throws ScriptingErrorLog.SemanticErrorException {
        int index = -1;
        if (namedParams != null) {
            index = namedParams.indexOf(param);
        }
        if (index < 0 && param.startsWith("$")) {
            if (Character.isDigit(param.charAt(1))) {
                index = Integer.parseInt(param.substring(1)) - 1;
                if (index < 0 || !dynamic && namedParams != null && index >= namedParams.size()) {
                    errLog.emitParamIndexError(parser, index + 1, namedParams == null ? 0 : namedParams.size());
                }
            } else if (!insideRecursion) {
                errLog.emitRecursiveParamsOutideRecursionError(parser, param);
            } else if (namedParams != null && namedParams.indexOf(param.substring(1)) < 0 && !dynamic) {
                errLog.emitParamNotFoundError(parser, param.substring(1));
            }
        }
        if (index < 0 && namedParams != null && (dynamic || param.startsWith("$") && insideRecursion)) {
            index = namedParams.size();
            namedParams.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, param);
        }
        return index;
    }

    public static class LPWithParams {
        public LP property;
        public List<Integer> usedParams;

        public LPWithParams(LP property, List<Integer> usedParams) {
            this.property = property;
            this.usedParams = usedParams;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", property, usedParams);
        }
    }

    private boolean isTrivialParamList(List<Object> paramList) {
        int index = 1;
        for (Object param : paramList) {
            if (!(param instanceof Integer) || ((Integer)param) != index) return false;
            ++index;
        }
        return true;
    }

    public void addSettingsToProperty(LP property, String name, String caption, List<String> namedParams, String groupName, boolean isPersistent, String tableName, Boolean notNullResolve, boolean notNullSession) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addSettingsToProperty(" + property.property.getSID() + ", " + name + ", " + caption + ", " +
                           namedParams + ", " + groupName + ", " + isPersistent  + ", " + tableName + ");");
        checkDuplicateProperty(name);
        checkDistinctParameters(namedParams);
        checkNamedParams(property, namedParams);

        changePropertyName(property, name); // должно идти первым

        AbstractGroup group = (groupName == null ? null : findGroupByCompoundName(groupName));
        property.property.caption = (caption == null ? name : caption);
        addPropertyToGroup(property.property, group);

        ImplementTable targetTable = null;
        if (tableName != null) {
            targetTable = findTableByCompoundName(tableName);
            if (!targetTable.equalClasses(((LCP<?>)property).property.getInterfaceClasses())) {
                // todo : проверка неправильная - должна быть на ClassWhere
                //errLog.emitWrongClassesForTable(parser, name, tableName);
            }
        }
        if (property.property instanceof StoredDataProperty) {
            ((StoredDataProperty)property.property).markStored(baseLM.tableFactory, targetTable);
        } else if (isPersistent && (property.property instanceof AggregateProperty)) {
            addPersistent((LCP) property, targetTable);
        }

        if (notNullResolve != null) {
            setNotNull((LCP)property, notNullSession, notNullResolve ? PropertyFollows.RESOLVE_FALSE : PropertyFollows.RESOLVE_NOTHING);
        }

        if (property.property instanceof CalcProperty) {
            checkPropertyValue(property, name);
            checkClassWhere(property, name);
        }
        addNamedParams(property.property.getSID(), namedParams);
    }

    public void setPanelLocation(LP property, boolean toolbar, String onlyPropertySID, boolean defaultProperty) throws ScriptingErrorLog.SemanticErrorException {
        PanelLocation panelLocation;
        if (toolbar) {
            panelLocation = new ToolbarPanelLocation();
        } else {
            panelLocation = new ShortcutPanelLocation(defaultProperty);
            if (onlyPropertySID != null)
                ((ShortcutPanelLocation) panelLocation).setOnlyProperty(findLPByCompoundName(onlyPropertySID));
        }
        property.setPanelLocation(panelLocation);
    }

    public void setFixedCharWidth(LP property, Integer fixedCharWidth) {
        if (fixedCharWidth != null && fixedCharWidth > 0)
            property.setFixedCharWidth(fixedCharWidth);
    }

    public void setMinCharWidth(LP property, Integer minCharWidth) {
        if (minCharWidth != null)
            property.setMinimumCharWidth(minCharWidth);
    }

    public void setMaxCharWidth(LP property, Integer maxCharWidth) {
        if (maxCharWidth != null)
            property.setMaximumCharWidth(maxCharWidth);
    }

    public void setPrefCharWidth(LP property, Integer prefCharWidth) {
        if (prefCharWidth != null)
            property.setPreferredCharWidth(prefCharWidth);
    }

    public void setImage(LP property, String path) {
        property.setImage(path);
    }

    public void setEditKey(LP property, String code, Boolean showEditKey) {
        property.setEditKey(KeyStroke.getKeyStroke(code));
        if (showEditKey != null)
            property.setShowEditKey(showEditKey);
    }

    public void setAutoset(LP property, boolean autoset) {
        ((LCP)property).setAutoset(autoset);
    }

    public void setAskConfirm(LP property, boolean askConfirm) {
        property.setAskConfirm(askConfirm);
    }

    public void setRegexp(LP property, String regexp, String regexpMessage) {
        property.setRegexp(regexp);
        if (regexpMessage != null) {
            property.setRegexpMessage(regexpMessage);
        }
    }

    public void makeLoggable(LP property, Boolean isLoggable) {
        if (isLoggable != null && isLoggable && property != null)
            ((LCP)property).makeLoggable(baseLM);
    }

    public void setEchoSymbols(LP property) {
        property.setEchoSymbols(true);
    }

    public void setAggProp(LP property) {
        ((CalcProperty)property.property).aggProp = true;
    }

    public void setScriptedEditAction(LP property, String actionType, LPWithParams action) {
        List<Object> params = getParamsPlainList(Arrays.asList(action));
        ImList<ActionPropertyMapImplement> actionImplements = readActionImplements(property.listInterfaces, params.toArray());
        property.property.setEditAction(actionType, actionImplements.get(0));
    }

    public void setEventId(LP property, String id) {
        property.property.eventID = id;
    }

    private <T extends LP> void changePropertyName(T lp, String name) {
        removeModuleLP(lp);
        setPropertySID(lp, name, false);
        lp.property.freezeSID();
        addModuleLP(lp);
    }

    public LPWithParams addScriptedJProp(LP mainProp, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        //            assert mainProp instanceof LAP; should use JoinAction
        if(mainProp instanceof LAP)
            return addScriptedJoinAProp(mainProp, paramProps);

        checkParamCount(mainProp, paramProps.size());
        List<Object> resultParams = getParamsPlainList(paramProps);
        LP prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
        } else {
            scriptLogger.info("addScriptedJProp(" + mainProp.property.getSID() + ", " + resultParams + ");");
            prop = addJProp("", (LCP) mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    private LCP getRelationProp(String op) {
        if (op.equals("==")) {
            return baseLM.equals2;
        } else if (op.equals("!=")) {
            return baseLM.diff2;
        } else if (op.equals(">")) {
            return baseLM.greater2;
        } else if (op.equals("<")) {
            return baseLM.less2;
        } else if (op.equals(">=")) {
            return baseLM.groeq2;
        } else if (op.equals("<=")) {
            return baseLM.lsoeq2;
        }
        assert false;
        return null;
    }

    private LCP getArithProp(String op) {
        if (op.equals("+")) {
            return baseLM.sum;
        } else if (op.equals("-")) {
            return baseLM.subtract;
        } else if (op.equals("*")) {
            return baseLM.multiply;
        } else if (op.equals("/")) {
            return baseLM.divide;
        }
        assert false;
        return null;
    }

    public LPWithParams addScriptedEqualityProp(String op, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedRelationalProp(String op, LPWithParams leftProp, LPWithParams rightProp) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(getRelationProp(op), asList(leftProp, rightProp));
    }

    public LPWithParams addScriptedOrProp(List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        LPWithParams res = properties.get(0);
        if (properties.size() > 1) {
            res = addScriptedUProp(Union.OVERRIDE, properties, "OR");
        }
        return res;
    }

    public LPWithParams addScriptedAndProp(List<Boolean> nots, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert nots.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        if (nots.size() > 0) {
            boolean[] notsArray = new boolean[nots.size()];
            for (int i = 0; i < nots.size(); i++) {
                notsArray[i] = nots.get(i);
            }
//            assert properties.get(0).property instanceof LCP; // assert что calculation'ы а не action'ы
            if(properties.get(0).property instanceof LAP) { // если action то подставляем if, потом надо будет запретить верхним assert'ом
                curLP = properties.get(0);
                for(int i=0;i<notsArray.length;i++)
                    curLP = addScriptedIfAProp(properties.get(i+1), curLP, null, notsArray[i]);
            } else
                curLP = addScriptedJProp(and(notsArray), properties);
        }
        return curLP;
    }

    public LPWithParams addScriptedIfElseUProp(LPWithParams ifProp, LPWithParams thenProp, LPWithParams elseProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIfElseUProp(" + ifProp + ", " + thenProp + ", " + elseProp + ");");
        assert thenProp.property instanceof LCP && elseProp.property instanceof LCP; // assert что calculation'ы а не action'ы
        return addScriptedUProp(Union.EXCLUSIVE,
                                asList(addScriptedJProp(and(false), asList(thenProp, ifProp)),
                                       addScriptedJProp(and(true), asList(elseProp, ifProp))),
                                "IF");
    }

    public LPWithParams addScriptedCaseUProp(List<LPWithParams> whenProps, List<LPWithParams> thenProps, LPWithParams defaultProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCaseUProp(" + whenProps  + "->" + thenProps + ");");

        assert whenProps.size() > 0 && whenProps.size() == thenProps.size();

        checkCasePropertyParams(whenProps, thenProps);

        List<LPWithParams> caseParamProps = new ArrayList<LPWithParams>();
        for (int i = 0; i < whenProps.size(); i++) {
            caseParamProps.add(whenProps.get(i));
            caseParamProps.add(thenProps.get(i));
        }
        caseParamProps.add(defaultProp);

        LP caseProp = addCaseUProp(null, genSID(), false, "", getParamsPlainList(caseParamProps).toArray());
        return new LPWithParams(caseProp, mergeAllParams(caseParamProps));
    }

    public LPWithParams addScriptedFileAProp(boolean loadFile, LPWithParams property) {
        scriptLogger.info("addScriptedFileAProp(" + loadFile + ", " + property + ");");
        LAP<?> res;
        if (loadFile) {
            res = addLFAProp((LCP) property.property);
        } else {
            res = addOFAProp((LCP) property.property);
        }
        return new LPWithParams(res, property.usedParams);
    }

    public LP addScriptedCustomActionProp(String javaClassName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCustomActionProp(" + javaClassName + ");");
        try {
            return baseLM.addAProp(null, (ActionProperty) Class.forName(javaClassName).getConstructor(this.getClass()).newInstance(this));
        } catch (ClassNotFoundException e) {
            errLog.emitClassNotFoundError(parser, javaClassName);
        } catch (Exception e) {
            errLog.emitCreatingClassInstanceError(parser, javaClassName);
        }
        return null;
    }

    public LPWithParams addScriptedEmailProp(LPWithParams fromProp,
                                             LPWithParams subjProp,
                                             List<Message.RecipientType> recipTypes,
                                             List<LPWithParams> recipProps,
                                             List<String> forms,
                                             List<FormStorageType> formTypes,
                                             List<OrderedMap<String, LPWithParams>> mapObjects,
                                             List<LPWithParams> attachNames,
                                             List<AttachmentFormat> attachFormats) throws ScriptingErrorLog.SemanticErrorException {

        List<LPWithParams> allProps = new ArrayList<LPWithParams>();

        if (fromProp != null) {
            allProps.add(fromProp);
        }
        allProps.add(subjProp);
        allProps.addAll(recipProps);

        for (int i = 0; i < forms.size(); ++i) {
            allProps.addAll(mapObjects.get(i).values());
            if (formTypes.get(i) == FormStorageType.ATTACH && attachNames.get(i) != null) {
                allProps.add(attachNames.get(i));
            }
        }

        Object[] allParams = getParamsPlainList(allProps).toArray();

        ImOrderSet<PropertyInterface> tempContext = genInterfaces(getIntNum(allParams));
        ValueClass[] eaClasses = CalcProperty.getCommonClasses(tempContext, readCalcImplements(tempContext, allParams).getCol());

        LAP<ClassPropertyInterface> eaPropLP = addEAProp(null, "", "", eaClasses, null, null);
        EmailActionProperty eaProp = (EmailActionProperty) eaPropLP.property;

        ImList<CalcPropertyInterfaceImplement<ClassPropertyInterface>> allImplements = readCalcImplements(eaPropLP.listInterfaces, allParams);

        int i = 0;
        if (fromProp != null) {
            eaProp.setFromAddress(allImplements.get(i++));
        } else {
            // по умолчанию используем стандартный fromAddress
            eaProp.setFromAddress(new CalcPropertyMapImplement((CalcProperty) BL.emailLM.fromAddress.property));
        }
        eaProp.setSubject(allImplements.get(i++));

        for (Message.RecipientType recipType : recipTypes) {
            eaProp.addRecipient(allImplements.get(i++), recipType);
        }

        for (int j = 0; j < forms.size(); ++j) {
            String formName = forms.get(j);
            FormStorageType formType = formTypes.get(j);
            FormEntity form = findFormByCompoundName(formName);

            Map<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>> objectsImplements = new HashMap<ObjectEntity, CalcPropertyInterfaceImplement<ClassPropertyInterface>>();
            for (Map.Entry<String, LPWithParams> entry : mapObjects.get(j).entrySet()) {
                objectsImplements.put(findObjectEntity(form, entry.getKey()), allImplements.get(i++));
            }

            if (formType == FormStorageType.ATTACH) {
                CalcPropertyInterfaceImplement<ClassPropertyInterface> attachNameProp = attachNames.get(j) != null ? allImplements.get(i++) : null;
                eaProp.addAttachmentForm(form, attachFormats.get(j), objectsImplements, attachNameProp);
            } else {
                eaProp.addInlineForm(form, objectsImplements);
            }
        }

        return new LPWithParams(eaPropLP, mergeAllParams(allProps));
    }

    public LPWithParams addScriptedAdditiveOrProp(List<String> operands, List<LPWithParams> properties) {
        assert operands.size() + 1 == properties.size();
        
        LPWithParams res = properties.get(0);
        if (operands.size() > 0) {
            scriptLogger.info("addScriptedAdditiveOrProp(" + operands + ", " + properties + ");");
            List<Object> resultParams;
            int[] coeffs = new int[properties.size()];
            for (int i = 0; i < coeffs.length; i++) {
                if (i == 0 || operands.get(i-1).equals("(+)")) {
                    coeffs[i] = 1;
                } else {
                    coeffs[i] = -1;
                }
            }
            resultParams = getParamsPlainList(properties);
            res = new LPWithParams(addUProp(null, "", Union.SUM, null, coeffs, resultParams.toArray()), mergeAllParams(properties));
        }
        return res;    
    }
    
    public LPWithParams addScriptedAdditiveProp(List<String> operands, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(getArithProp(op), asList(curLP, properties.get(i)));
        }
        return curLP;
    }


    public LPWithParams addScriptedMultiplicativeProp(List<String> operands, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = properties.get(0);
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(getArithProp(op), asList(curLP, properties.get(i)));
        }
        return curLP;
    }

    public LPWithParams addScriptedUnaryMinusProp(LPWithParams prop) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(baseLM.minus, asList(prop));
    }

    private boolean doesExtendContext(List<LPWithParams> list, List<LPWithParams> orders) {
        Set<Integer> listContext = new HashSet<Integer>();
        for(LPWithParams lp : list)
            if(lp.property != null)
                listContext.addAll(lp.usedParams);
        return !listContext.containsAll(mergeAllParams(orders));
    }
    private List<Integer> mergeAllParams(List<LPWithParams> lpList) {
        Set<Integer> s = new TreeSet<Integer>();
        for (LPWithParams mappedLP : lpList) {
            s.addAll(mappedLP.usedParams);
        }
        return new ArrayList<Integer>(s);
    }

    private List<Integer> mergeIntLists(List<List<Integer>> lists) {
        Set<Integer> s = new TreeSet<Integer>();
        for (List<Integer> list : lists) {
            s.addAll(list);
        }
        return new ArrayList<Integer>(s);
    }


    public LPWithParams addScriptedListAProp(boolean newSession, boolean doApply, boolean singleApply, Set<String> upLocalNames, String used, List<LPWithParams> properties, List<String> localPropNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedListAProp(" + newSession + ", " + doApply + ", " + properties + ");");

        ImSet<SessionDataProperty> sessionUsed = used != null ? SetFact.singleton((SessionDataProperty) findLPByCompoundName(used).property) : SetFact.<SessionDataProperty>EMPTY();
        MExclSet<SessionDataProperty> mUpLocal = SetFact.mExclSet(upLocalNames.size()); // exception кидается
        for(String name : upLocalNames)
            mUpLocal.exclAdd((SessionDataProperty) findLPByCompoundName(name).property);

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);

        LAP<?> listLP = addListAProp(resultParams.toArray());
        for (String propName : localPropNames) {
            currentLocalProperties.remove(propName);
        }

        return !newSession
               ? new LPWithParams(listLP, usedParams)
               : new LPWithParams(addNewSessionAProp(null, genSID(), "", listLP, doApply, singleApply, mUpLocal.immutable(), sessionUsed), usedParams);
    }

    public LPWithParams addScriptedRequestUserInputAProp(String typeId, String chosenKey, LPWithParams action) throws ScriptingErrorLog.SemanticErrorException {
        Type requestValueType = getPredefinedType(typeId);

        LPWithParams prop;
        if (action == null) {
            if (!(requestValueType instanceof DataClass)) {
                errLog.emitRequestUserInputDataTypeError(parser, typeId);
            }

            prop = new LPWithParams(addRequestUserDataAProp(null, genSID(), "", (DataClass) requestValueType), new ArrayList<Integer>());
        } else {
            prop = new LPWithParams(addRequestUserInputAProp(null, genSID(), "", (LAP<?>) action.property, requestValueType, chosenKey), newArrayList(action.usedParams));
        }
        return prop;
    }

    public LCP addLocalDataProperty(String name, String returnClassName, List<String> paramClassNames) throws ScriptingErrorLog.SemanticErrorException {
        checkLocalDataPropertyName(name);

        LCP res = addScriptedDProp(returnClassName, paramClassNames, true, false);
        currentLocalProperties.put(name, res);
        return res;
    }

    public LPWithParams addScriptedJoinAProp(LP mainProp, List<LPWithParams> properties) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedJoinAProp(" + mainProp + ", " + properties + ", " + ");");
        checkParamCount(mainProp, properties.size());

        List<Object> resultParams = getParamsPlainList(properties);
        List<Integer> usedParams = mergeAllParams(properties);
        LP prop = addJoinAProp(null, genSID(), "", (LAP<?>) mainProp, resultParams.toArray());
        return new LPWithParams(prop, usedParams);
    }

    public LP addScriptedAddObjProp(String className) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAddObjProp(" + className + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        return getAddObjectAction((CustomClass) cls, false, baseLM.getAddedObjectProperty());
    }

    public LP addScriptedAddFormAction(String className, boolean session) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAddFormAction(" + className + ", " + session + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        return getScriptAddFormAction((CustomClass) cls, session);
    }

    public LP addScriptedEditFormAction(String className, boolean session) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEditFormAction(" + className + ", " + session + ");");
        ValueClass cls = findClassByCompoundName(className);
        checkAddActionsClass(cls);
        return getScriptEditFormAction((CustomClass) cls, session);
    }

    public LPWithParams addScriptedConfirmProp(int length, LPWithParams msgProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConfirmProp(" + length + ", " + msgProp + ");");
        return addScriptedJoinAProp(addConfirmAProp("LS Fusion", length), asList(msgProp));
    }

    public LPWithParams addScriptedMessageProp(int length, LPWithParams msgProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedMessageProp(" + length + ", " + msgProp + ");");
        return addScriptedJoinAProp(addMAProp("LS Fusion", length), asList(msgProp));
    }

    public LPWithParams addScriptedChangeClassAProp(LPWithParams param, String className) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedChangeClassAProp(" + className + ")");
        ValueClass cls = findClassByCompoundName(className);
        checkChangeClassActionClass(cls);
        LAP<?> res = addChangeClassAProp((ConcreteCustomClass) cls);
        return new LPWithParams(res,  param.usedParams);
    }

    public LPWithParams addScriptedEvalActionProp(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEvalActionProp(" + property + ")");
        Type exprType = property.property.property.getType();
        if (!(exprType instanceof StringClass || exprType instanceof TextClass)) {
            errLog.emitEvalExpressionError(parser);
        }
        LAP<?> res = addEvalAProp((LCP) property.property);
        return new LPWithParams(res, property.usedParams);
    }

    public LPWithParams addScriptedSetPropertyAProp(List<String> context, LPWithParams toProperty, LPWithParams fromProperty, LPWithParams whereProperty) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSetPropertyAProp(" + context + ", " + toProperty + ", " + fromProperty + ", " + whereProperty + ");");
        if (toProperty.property == null) {
            errLog.emitLeftSideMustBeAProperty(parser);
        }

        List<LPWithParams> lpList = BaseUtils.toList(toProperty, fromProperty);
        if (whereProperty != null) {
            lpList.add(whereProperty);
        }
        List<Integer> allParams = mergeAllParams(lpList);

        //все использованные параметры, которые были в старом контексте, идут на вход результирующего свойства
        List<Integer> resultInterfaces = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex >= context.size()) {
                break;
            }
            resultInterfaces.add(paramIndex);
        }

        List<LPWithParams> paramsList = new ArrayList<LPWithParams>();
        for (int resI : resultInterfaces) {
            paramsList.add(new LPWithParams(null, asList(resI)));
        }
        paramsList.add(toProperty);
        paramsList.add(fromProperty);
        if (whereProperty != null) {
            paramsList.add(whereProperty);
        }
        List<Object> resultParams = getParamsPlainList(paramsList);
        LP result = addSetPropertyAProp(null, genSID(), "", resultInterfaces.size(), whereProperty != null, resultParams.toArray());
        return new LPWithParams(result, resultInterfaces);
    }

    public LPWithParams addScriptedIfAProp(LPWithParams condition, LPWithParams trueAction, LPWithParams falseAction) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedIfAProp(condition, trueAction, falseAction, false);
    }

    // временно после появления NOT в явную можно будет убрать
    private LPWithParams addScriptedIfAProp(LPWithParams condition, LPWithParams trueAction, LPWithParams falseAction, boolean not) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIfAProp(" + condition + ", " + trueAction + ", " + falseAction + ");");
        List<LPWithParams> propParams = toList(condition, trueAction);
        if (falseAction != null) {
            propParams.add(falseAction);
        }
        List<Integer> allParams = mergeAllParams(propParams);
        LP result = addIfAProp(null, genSID(), "", not, getParamsPlainList(propParams).toArray());
        return new LPWithParams(result, allParams);
    }

    public LPWithParams addScriptedForAProp(List<String> oldContext, LPWithParams condition, List<LPWithParams> orders, LPWithParams action, LPWithParams elseAction, Integer addNum, String addClassName, boolean recursive, boolean descending) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedForAProp(" + oldContext + ", " + condition + ", " + orders + ", " + action + ", " + elseAction + ", " + recursive + ", " + descending + ");");

        boolean ordersNotNull = (condition !=null ? doesExtendContext(Collections.singletonList(condition), orders) : !orders.isEmpty());

        List<LPWithParams> creationParams = new ArrayList<LPWithParams>();
        if(condition!=null)
            creationParams.add(condition);
        creationParams.addAll(orders);
        if(addNum!=null)
            creationParams.add(new LPWithParams(null, asList(addNum)));
        if (elseAction != null) {
            creationParams.add(elseAction);
        }
        creationParams.add(action);
        List<Integer> allParams = mergeAllParams(creationParams);

        List<Integer> usedParams = new ArrayList<Integer>();
        for (int paramIndex : allParams) {
            if (paramIndex < oldContext.size()) {
                usedParams.add(paramIndex);
            }
        }

        checkForActionPropertyConstraints(recursive, usedParams, allParams);

        List<LPWithParams> allCreationParams = new ArrayList<LPWithParams>();
        for (int usedParam : usedParams) {
            allCreationParams.add(new LPWithParams(null, asList(usedParam)));
        }
        allCreationParams.addAll(creationParams);

        LP result = addForAProp(null, genSID(), "", !descending, ordersNotNull, recursive, elseAction != null, usedParams.size(), 
                addClassName != null ? (CustomClass)findClassByCompoundName(addClassName) : null, condition!=null, getParamsPlainList(allCreationParams).toArray());
        return new LPWithParams(result, usedParams);
    }

    public LPWithParams getTerminalFlowActionProperty(boolean isBreak) {
        return new LPWithParams(isBreak ? baseLM.flowBreak : baseLM.flowReturn, new ArrayList<Integer>());
    }

    private List<Object> getCoeffParamsPlainList(List<LPWithParams> mappedPropsList, Integer[] coeffs) {
        List<LP> props = new ArrayList<LP>();
        List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
        for (LPWithParams mappedProp : mappedPropsList) {
            props.add(mappedProp.property);
            usedParams.add(mappedProp.usedParams);
        }
        return getCoeffParamsPlainList(props, usedParams, coeffs);
    } 
    
    private List<Object> getParamsPlainList(List<LPWithParams>... mappedPropLists) {
        List<LP> props = new ArrayList<LP>();
        List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
        for (List<LPWithParams> mappedPropList : mappedPropLists) {
            for (LPWithParams mappedProp : mappedPropList) {
                props.add(mappedProp.property);
                usedParams.add(mappedProp.usedParams);
            }
        }
        return getCoeffParamsPlainList(props, usedParams, null);
    }

    private List<Object> getCoeffParamsPlainList(List<LP> paramProps, List<List<Integer>> usedParams, Integer[] coeffs) {
        assert coeffs == null || paramProps.size() == coeffs.length;
        List<Integer> allUsedParams = mergeIntLists(usedParams);
        List<Object> resultParams = new ArrayList<Object>();

        for (int i = 0; i < paramProps.size(); i++) {
            LP property = paramProps.get(i);
            if (property != null) {
                if (coeffs != null) {
                    resultParams.add(coeffs[i]);
                }
                resultParams.add(property);
                for (int paramIndex : usedParams.get(i)) {
                    int localParamIndex = allUsedParams.indexOf(paramIndex);
                    assert localParamIndex >= 0;
                    resultParams.add(localParamIndex + 1);
                }
            } else {
                if (coeffs != null) {
                    resultParams.add(coeffs[i]);
                }
                int localParamIndex = allUsedParams.indexOf(usedParams.get(i).get(0));
                assert localParamIndex >= 0;
                resultParams.add(localParamIndex + 1);
            }
        }
        return resultParams;
    }

    public LCP addScriptedGProp(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps, List<LPWithParams> orderProps,
                                  boolean ascending, LPWithParams whereProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGProp(" + type + ", " + mainProps + ", " + groupProps + ", " + orderProps + ", " +
                                            ascending + ", " + whereProp + ");");

        checkGPropOrderConsistence(type, orderProps.size());
        checkGPropAggregateConsistence(type, mainProps.size());
        checkGPropUniqueConstraints(type, mainProps, groupProps);

        List<LPWithParams> whereProps = new ArrayList<LPWithParams>();
        if (type == GroupingType.UNIQUE) {
            if (whereProp != null) {
                whereProps.add(whereProp);
            } else {
                whereProps.add(new LPWithParams(null, asList(mainProps.get(0).usedParams.get(0))));
            }
        }
        List<Object> resultParams = getParamsPlainList(mainProps, whereProps, orderProps, groupProps);

        boolean ordersNotNull = doesExtendContext(mergeLists(mainProps, groupProps), orderProps);

        int groupPropParamCount = mergeAllParams(mergeLists(mainProps, groupProps, orderProps)).size();
        LCP resultProp = null;
        if (type == GroupingType.SUM) {
            resultProp = addSGProp(null, genSID(), false, false, "", groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.MAX || type == GroupingType.MIN) {
            resultProp = addMGProp(null, genSID(), false, "", type == GroupingType.MIN, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.CONCAT) {
            resultProp = addOGProp(null, genSID(), false, "", GroupType.STRING_AGG, orderProps.size(), ordersNotNull, !ascending, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.UNIQUE) {
            resultProp = addAGProp(null, false, genSID(), false, "", false, groupPropParamCount, resultParams.toArray());
        } else if (type == GroupingType.EQUAL) {
            resultProp = addCGProp(null, false, genSID(), false, "", null, groupPropParamCount, resultParams.toArray());
        }
        return resultProp;
    }

    public LPWithParams addScriptedUProp(Union unionType, List<LPWithParams> paramProps, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedUProp(" + unionType + ", " + paramProps + ");");
        checkUnionPropertyParams(paramProps, errMsgPropType);

        int[] coeffs = null;
        if (unionType == Union.SUM) {
            coeffs = new int[paramProps.size()];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = 1;
            }
        }
        List<Object> resultParams = getParamsPlainList(paramProps);
        LCP prop = addUProp(null, "", unionType, null, coeffs, resultParams.toArray());
        return new LPWithParams(prop, mergeAllParams(paramProps));
    }

    public LPWithParams addScriptedPartitionProp(PartitionType partitionType, LP ungroupProp, boolean strict, int precision, boolean isAscending,
                                                 boolean useLast, int groupPropsCnt, List<LPWithParams> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedPartitionProp(" + partitionType + ", " + ungroupProp + ", " + strict + ", " + precision + ", " +
                                                        isAscending + ", " + useLast + ", " + groupPropsCnt + ", " + paramProps + ");");
        checkPartitionWindowConsistence(partitionType, useLast);
        checkPartitionUngroupConsistence(ungroupProp, groupPropsCnt);

        boolean ordersNotNull = doesExtendContext(paramProps.subList(0, groupPropsCnt + 1), paramProps.subList(groupPropsCnt + 1, paramProps.size()));

        List<Object> resultParams = getParamsPlainList(paramProps);
        List<Integer> usedParams = mergeAllParams(paramProps);
        LP prop;
        if (partitionType == PartitionType.SUM || partitionType == PartitionType.PREVIOUS) {
            prop = addOProp(null, genSID(), false, "", partitionType, isAscending, ordersNotNull, useLast, groupPropsCnt, resultParams.toArray());
        } else if (partitionType == PartitionType.DISTR_CUM_PROPORTION) {
            prop = addPGProp(null, genSID(), false, precision, strict, "", usedParams.size(), isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        } else {
            prop = addUGProp(null, genSID(), false, strict, "", usedParams.size(), isAscending, ordersNotNull, (LCP) ungroupProp, resultParams.toArray());
        }
        return new LPWithParams(prop, usedParams);
    }

    public LPWithParams addScriptedCCProp(List<LPWithParams> params) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedCCProp(" + params + ");");
        return addScriptedJProp(addCCProp(params.size()), params);
    }

    public LPWithParams addScriptedDCCProp(LPWithParams ccProp, int index) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDCCProp(" + ccProp + ", " + index + ");");
        checkDeconcatenateIndex(ccProp, index);
        return addScriptedJProp(addDCCProp(index - 1), Arrays.asList(ccProp));
    }

    public LCP addScriptedSFProp(String typeName, String formulaText) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedSFProp(" + typeName + ", " + formulaText + ");");
        Set<Integer> params = findFormulaParameters(formulaText);
        checkFormulaParameters(params);
        if (typeName != null) {
            ValueClass cls = findClassByCompoundName(typeName);
            checkFormulaClass(cls);
            return addSFProp(transformFormulaText(formulaText), (DataClass) cls, params.size());
        } else {
            return addSFProp(transformFormulaText(formulaText), params.size());
        }
    }

    private Set<Integer> findFormulaParameters(String text) {
        Set<Integer> params = new HashSet<Integer>();
        Pattern pattern = Pattern.compile("\\$\\d+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String group = matcher.group();
            int paramNumber = Integer.valueOf(group.substring(1));
            params.add(paramNumber);
        }
        return params;
    }

    private String transformFormulaText(String text) {
        return text.replaceAll("\\$(\\d+)", "prm$1");
    }

    public LPWithParams addScriptedRProp(List<String> context, LPWithParams zeroStep, LPWithParams nextStep, Cycle cycleType) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedRProp(" + context + ", " + zeroStep + ", " + nextStep + ", " + cycleType + ");");

        List<Integer> usedParams = mergeAllParams(asList(zeroStep, nextStep));
        checkRecursionContext(context, usedParams);

        MOrderExclSet<Integer> mMainParams = SetFact.mOrderExclSetMax(usedParams.size());
        Map<Integer, Integer> usedToResult = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            if (!context.get(usedParams.get(i)).startsWith("$")) {
                mMainParams.add(i);
                usedToResult.put(usedParams.get(i), i);
            }
        }
        ImOrderSet<Integer> mainParams = mMainParams.immutableOrder();

        Map<Integer, Integer> mapPrev = new HashMap<Integer, Integer>();
        for (int i = 0; i < usedParams.size(); i++) {
            String param = context.get(usedParams.get(i)); // usedParams и context orderSet / revMap'ы
            if (param.startsWith("$")) {
                mapPrev.put(i, usedToResult.get(context.indexOf(param.substring(1))));
            }
        }

        List<Object> resultParams = getParamsPlainList(Arrays.asList(zeroStep, nextStep));
        LP res = addRProp(null, genSID(), false, "", cycleType, mainParams, MapFact.fromJavaRevMap(mapPrev), resultParams.toArray());

        List<Integer> resUsedParams = new ArrayList<Integer>();
        for (Integer usedParam : usedParams) {
            if (!context.get(usedParam).startsWith("$")) {
                resUsedParams.add(usedParam);
            }
        }
        return new LPWithParams(res, resUsedParams);
    }

    public LCP addConstantProp(ConstType type, Object value) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addConstantProp(" + type + ", " + value + ");");

        switch (type) {
            case INT: return addCProp(IntegerClass.instance, value);
            case LONG: return addCProp(LongClass.instance, value);
            case REAL: return addNumericConst((String) value);
            case STRING: return addCProp(StringClass.get(((String)value).length()), value);
            case LOGICAL: return addCProp(LogicalClass.instance, value);
            case DATE: return addCProp(DateClass.instance, value);
            case DATETIME: return addCProp(DateTimeClass.instance, value);
            case ENUM: return addStaticClassConst((String) value);
            case COLOR: return addCProp(ColorClass.instance, value);
            case NULL: return baseLM.vnull;
        }
        return null;
    }

    private LCP addNumericConst(String value) {
        return addCProp(NumericClass.get(value.length(), value.length() - value.indexOf('.') - 1), Double.parseDouble(value));
    }

    public java.sql.Date dateLiteralToDate(String text) {
        return new java.sql.Date(Integer.parseInt(text.substring(0, 4)) - 1900, Integer.parseInt(text.substring(5, 7)) - 1, Integer.parseInt(text.substring(8, 10)));
    }

    public Timestamp dateTimeLiteralToTimestamp(String text) {
        return new Timestamp(Integer.parseInt(text.substring(0, 4)) - 1900, Integer.parseInt(text.substring(5, 7)) - 1, Integer.parseInt(text.substring(8, 10)),
                Integer.parseInt(text.substring(11, 13)), Integer.parseInt(text.substring(14, 16)), 0, 0);
    }

    public LPWithParams addScriptedFAProp(String formName, List<String> objectNames, List<LPWithParams> mapping, List<LPWithParams> props, String className, ModalityType modalityType, FormSessionScope sessionScope, boolean checkOnOk) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFAProp(" + formName + ", " + objectNames + ", " + mapping + ", " + props + ", " + className + ", " + modalityType + ", " + sessionScope + ");");

        FormEntity form = findFormByCompoundName(formName);
        checkFormActionObjectsMapping(objectNames, mapping);

        DataClass cls = null;
        if (className != null) {
            ValueClass valueClass = findClassByCompoundName(className);
            checkFormDataClass(valueClass);
            cls = (DataClass) valueClass;
        }

        ObjectEntity[] objects = new ObjectEntity[objectNames.size()];
        for (int i = 0; i < objectNames.size(); i++) {
            objects[i] = findObjectEntity(form, objectNames.get(i));
        }

        ActionPropertyObjectEntity<?>[] propObjects = new ActionPropertyObjectEntity<?>[props == null ? 0 : props.size()];
        if (props != null) {
            for (int i = 0; i < props.size(); i++) {
                PropertyObjectInterfaceEntity[] params = new PropertyObjectInterfaceEntity[props.get(i).usedParams.size()];
                for (int j = 0; j < props.get(i).usedParams.size(); j++) {
                    params[j] = objects[props.get(i).usedParams.get(j)];
                }
                propObjects[i] = form.addPropertyObject((LAP<?>) props.get(i).property, params);
            }
        }
        assert propObjects.length<=1;
        assert cls==null;
        LPWithParams res = new LPWithParams(addFAProp(null, genSID(), "", form, objects, propObjects.length==0?null:propObjects[0], sessionScope, modalityType, checkOnOk), new ArrayList<Integer>());
        if (mapping.size() > 0) {
            res = addScriptedJoinAProp(res.property, mapping);
        }
        return res;
    }

    public ObjectEntity findObjectEntity(FormEntity form, String objectName) throws ScriptingErrorLog.SemanticErrorException {
        ObjectEntity result = form.getObject(objectName);
        if (result == null) {
            errLog.emitObjectNotFoundError(parser, objectName);
        }
        return result;
    }

    public void addScriptedMetaCodeFragment(String name, List<String> params, List<String> tokens, String code, int lineNumber) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedMetaCodeFragment(" + name + ", " + params + ", " + tokens + ", " + lineNumber + ");");

        checkDuplicateMetaCodeFragment(name, params.size());
        checkDistinctParameters(params);

        MetaCodeFragment fragment = new MetaCodeFragment(params, tokens, code, getName(), lineNumber);
        addMetaCodeFragment(name, fragment);
    }

    public void runMetaCode(String name, List<String> params, int lineNumber) throws RecognitionException {
        MetaCodeFragment metaCode = findMetaCodeFragmentByCompoundName(name, params.size());
        checkMetaCodeParamCount(metaCode, params.size());

        String code = metaCode.getCode(params);
        parser.runMetaCode(this, code, metaCode, metaCodeCallString(name, metaCode, params), lineNumber);
    }

    private String metaCodeCallString(String name, MetaCodeFragment metaCode, List<String> actualParams) {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(name);
        builder.append("(");
        for (int i = 0; i < actualParams.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(metaCode.getParameters().get(i));
            builder.append("=");
            builder.append(actualParams.get(i));
        }
        builder.append(")");
        return builder.toString();
    }

    public List<String> grabMetaCode(String metaCodeName) throws ScriptingErrorLog.SemanticErrorException {
        return parser.grabMetaCode(metaCodeName);
    }

    private LCP addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.indexOf('.');
        assert pointPos > 0;
        assert name.indexOf('.') == name.lastIndexOf('.');

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos+1);
        LCP resultProp = null;

        ValueClass cls = findClassByCompoundName(className);
        if (cls instanceof StaticCustomClass) {
            StaticCustomClass staticClass = (StaticCustomClass) cls;
            if (staticClass.hasSID(instanceName)) {
                resultProp = addCProp(staticClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static class instance", instanceName);
            }
        } else {
            errLog.emitNonStaticHasInstancesError(parser, className);
        }
        return resultProp;
    }

    public LCP addScriptedTypeProp(String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addTypeProp(" + className + ", " + (bIs ? "IS" : "AS") + ");");
        if (bIs) {
            return is(findClassByCompoundName(className));
        } else {
            return object(findClassByCompoundName(className));
        }
    }

    public LP addScriptedTypeExprProp(LP mainProp, LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(mainProp, asList(property)).property;
    }

    public void addScriptedConstraint(LP property, boolean checked, List<String> propNames, String message) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedConstraint(" + property + ", " + checked + ", " + propNames + ", " + message + ");");
        if (!property.property.check()) {
            errLog.emitConstraintPropertyAlwaysNullError(parser);
        }
        property.property.caption = message;
        ImSet<CalcProperty<?>> checkedProps = null;
        CalcProperty.CheckType type = (checked ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO);
        if (checked && propNames != null) {
            MSet<CalcProperty<?>> mCheckedProps = SetFact.mSet();
            for (String propName : propNames) {
                mCheckedProps.add((CalcProperty<?>) findLPByCompoundName(propName).property);
            }
            type = CalcProperty.CheckType.CHECK_SOME;
            checkedProps = mCheckedProps.immutable();
        }
        addConstraint((LCP<?>) property, type, checkedProps, this);
    }

    public LPWithParams addScriptedSpecialProp(String propType, LPWithParams property) {
        scriptLogger.info("addScriptedSpecialProp(" + propType + ", " + property + ");");
        LCP newProp = null;
        if (propType.equals("PREV")) {
            newProp = addOldProp((LCP) property.property);
        } else if (propType.equals("CHANGED")) {
            newProp = addCHProp((LCP) property.property, IncrementType.CHANGE);
        } else if (propType.equals("ASSIGNED")) {
            newProp = addCHProp((LCP) property.property, IncrementType.SET);
        } else if (propType.equals("CLASS")) {
            newProp = addClassProp((LCP) property.property);
        }
        return new LPWithParams(newProp, property.usedParams);
    }

    public void addScriptedFollows(String mainPropName, List<String> namedParams, List<Integer> options, List<LPWithParams> props, List<Boolean> sessions) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedFollows(" + mainPropName + ", " + namedParams + ", " + options + ", " + props + ", " + sessions + ");");
        LCP mainProp = (LCP) findLPByCompoundName(mainPropName);
        checkProperty(mainProp, mainPropName);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);

        for (int i = 0; i < props.size(); i++) {
            Integer[] params = new Integer[props.get(i).usedParams.size()];
            for (int j = 0; j < params.length; j++) {
                params[j] = props.get(i).usedParams.get(j) + 1;
            }
            follows(mainProp, options.get(i), sessions.get(i), (LCP) props.get(i).property, params);
        }
    }

    public void addScriptedWriteWhen(String mainPropName, List<String> namedParams, LPWithParams valueProp, LPWithParams whenProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedWriteWhen(" + mainPropName + ", " + namedParams + ", " + valueProp + ", " + whenProp + ");");
        LP mainProp = findLPByCompoundName(mainPropName);
        checkParamCount(mainProp, namedParams.size());
        checkDistinctParameters(namedParams);
        checkCalculationProperty(mainProp);

        List<Object> params = getParamsPlainList(asList(valueProp, whenProp));
        ((LCP)mainProp).setEventChange(params.toArray());
    }

    public void addScriptedEvent(LPWithParams whenProp, LPWithParams event, List<LPWithParams> orders, boolean descending, boolean session) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedEvent(" + whenProp + ", " + event + ", " + orders + ", " + descending + ", " + session + ");");
        checkActionProperty(event.property);
        List<Object> params = getParamsPlainList(asList(whenProp), orders);
        ((LAP)event.property).setEventAction(this, session, descending, false, params.toArray());
    }

    public void addScriptedGlobalEvent(LPWithParams event, boolean session, boolean single, String showDep, List<String> sPrevStarts) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGlobalEvent(" + event + ", " + session + ");");
        checkActionProperty(event.property);
        checkEventNoParameters(event.property);
        ActionProperty action = (ActionProperty) event.property.property;
        if(showDep!=null)
            action.showDep = findLPByCompoundName(showDep).property;
        ImSet<CalcProperty> prevStart = null;
        if(sPrevStarts!=null) {
            MExclSet<CalcProperty> mPrevStart = SetFact.mExclSet(); // функционально из-за exception'а не сделаешь
            for(String sPrevStart : sPrevStarts)
                mPrevStart.exclAdd(findLCPByCompoundName(sPrevStart).property);
            prevStart = mPrevStart.immutable();
        }
        addBaseEvent(action, session, false, single, prevStart);
    }

    public void addScriptedAspect(String mainPropName, List<String> mainPropParams, LPWithParams actionProp, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedAspect(" + mainPropName + ", " + mainPropParams + ", " + actionProp + ", " + before + ");");
        LP mainProp = findLPByCompoundName(mainPropName);
        checkParamCount(mainProp, mainPropParams.size());
        checkDistinctParameters(mainPropParams); // todo [dale]: надо, наверное, это вынести в отдельный метод
        checkActionProperty(actionProp.property);
        checkActionProperty(mainProp);

        List<Object> params = getParamsPlainList(Arrays.asList(actionProp));
        ImList<ActionPropertyMapImplement> actionImplements = readActionImplements(mainProp.listInterfaces, params.toArray());
        addAspectEvent((ActionProperty) mainProp.property, actionImplements.get(0), before);
    }

    public void addScriptedTable(String name, List<String> classIds) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedTable(" + name + ", " + classIds + ");");
        checkDuplicateTable(name);

        ValueClass[] classes = new ValueClass[classIds.size()];
        for (int i = 0; i < classIds.size(); i++) {
            classes[i] = findClassByCompoundName(classIds.get(i));
        }
        addTable(name, classes);
    }

    public void addScriptedIndex(List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedIndex(" + propNames + ");");
        LCP[] lps = new LCP[propNames.size()];
        for (int i = 0; i < propNames.size(); i++) {
            lps[i] = (LCP) findLPByCompoundName(propNames.get(i));
        }
        addIndex(lps);
    }

    public void addScriptedLoggable(List<String> propNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedLoggable(" + propNames + ");");

        for (String name : propNames) {
            LCP lp = (LCP) findLPByCompoundName(name);
            lp.makeLoggable(baseLM);
        }
    }

    public void addScriptedWindow(WindowType type, String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        if (scriptLogger.isInfoEnabled()) {
            scriptLogger.info("addScriptedWindow(" + name + ", " + type + ", " + caption + ", " + options + ");");
        }

        checkDuplicateWindow(name);

        NavigatorWindow window = null;
        switch (type) {
            case MENU:
                window = createMenuWindow(name, caption, options);
                break;
            case PANEL:
                window = createPanelWindow(name, caption, options);
                break;
            case TOOLBAR:
                window = createToolbarWindow(name, caption, options);
                break;
            case TREE:
                window = createTreeWindow(caption, options);
                break;
        }

        window.drawRoot = nvl(options.getDrawRoot(), false);
        window.drawScrollBars = nvl(options.getDrawScrollBars(), true);
        window.titleShown = nvl(options.getDrawTitle(), true);

        addWindow(name, window);
    }

    private MenuNavigatorWindow createMenuWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        DockPosition dp = options.getDockPosition();
        if (dp == null) {
            errLog.emitWindowPositionNotSpecified(parser, name);
        }

        MenuNavigatorWindow window = new MenuNavigatorWindow(null, caption, dp.x, dp.y, dp.width, dp.height);
        window.orientation = orientation.asMenuOrientation();

        return window;
    }

    private PanelNavigatorWindow createPanelWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            errLog.emitWindowOrientationNotSpecified(parser, name);
        }

        PanelNavigatorWindow window = new PanelNavigatorWindow(orientation.asToolbarOrientation(), null, caption);
        if (dockPosition != null) {
            window.setDockPosition(dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        }
        return window;
    }

    private ToolBarNavigatorWindow createToolbarWindow(String name, String caption, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        Orientation orientation = options.getOrientation();
        BorderPosition borderPosition = options.getBorderPosition();
        DockPosition dockPosition = options.getDockPosition();

        if (orientation == null) {
            errLog.emitWindowOrientationNotSpecified(parser, name);
        }

        if (borderPosition != null && dockPosition != null) {
            errLog.emitWindowPositionConflict(parser, name);
        }

        ToolBarNavigatorWindow window;
        if (borderPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption, borderPosition.asLayoutConstraint());
        } else if (dockPosition != null) {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption, dockPosition.x, dockPosition.y, dockPosition.width, dockPosition.height);
        } else {
            window = new ToolBarNavigatorWindow(orientation.asToolbarOrientation(), null, caption);
        }

        HAlign hAlign = options.getHAlign();
        VAlign vAlign = options.getVAlign();
        HAlign thAlign = options.getTextHAlign();
        VAlign tvAlign = options.getTextVAlign();
        if (hAlign != null) {
            window.alignmentX = hAlign.asToolbarAlign();
        }
        if (vAlign != null) {
            window.alignmentY = vAlign.asToolbarAlign();
        }
        if (thAlign != null) {
            window.horizontalTextPosition = thAlign.asTextPosition();
        }
        if (tvAlign != null) {
            window.verticalTextPosition = tvAlign.asTextPosition();
        }
        return window;
    }

    private TreeNavigatorWindow createTreeWindow(String caption, NavigatorWindowOptions options) {
        TreeNavigatorWindow window = new TreeNavigatorWindow(null, caption);
        DockPosition dp = options.getDockPosition();
        if (dp != null) {
            window.setDockPosition(dp.x, dp.y, dp.width, dp.height);
        }
        return window;
    }


    public void hideWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        findWindowByCompoundName(name).visible = false;
    }

    public NavigatorElement createScriptedNavigatorElement(String name, String caption, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName, String actionName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("createScriptedNavigatorElement(" + name + ", " + caption + ");");

        assert name != null && caption != null && anchorElement != null;

        checkDuplicateNavigatorElement(name);

        NavigatorElement newElement;

        if (actionName != null) {
            LAP<?> actionProperty = (LAP<?>) findLPByCompoundName(actionName);
            checkActionProperty(actionProperty);

            newElement = addNavigatorAction(name, caption, actionProperty);
        } else {
            newElement = addNavigatorElement(name, caption);
        }


        setupNavigatorElement(newElement, caption, pos, anchorElement, windowName);

        return newElement;
    }

    public void setupNavigatorElement(NavigatorElement<?> element, String caption, InsertPosition pos, NavigatorElement<?> anchorElement, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("setupNavigatorElement(" + element.getSID() + ", " + caption + ", " + pos + ", " + anchorElement + ", " + windowName + ");");

        assert element != null;

        if (caption != null) {
            element.caption = caption;
        }

        if (windowName != null) {
            setNavigatorElementWindow(element, windowName);
        }

        if (pos != null && anchorElement != null) {
            moveElement(element, pos, anchorElement);
        }
    }

    private void moveElement(NavigatorElement element, InsertPosition pos, NavigatorElement anchorElement) throws ScriptingErrorLog.SemanticErrorException {
        assert anchorElement != null && pos != null;
        NavigatorElement parent = null;
        if (pos == IN) {
            parent = anchorElement;
        } else {
            parent = anchorElement.getParent();
            if (parent == null) {
                errLog.emitIllegalInsertBeforeAfterNavigatorElement(parser, anchorElement.getSID());
            }
        }

        if (element.isAncestorOf(parent)) {
            errLog.emitIllegalMoveNavigatorToSubnavigator(parser, element.getSID(), parent.getSID());
        }

        switch (pos) {
            case IN:
                parent.add(element);
                break;
            case BEFORE:
                parent.addBefore(element, anchorElement);
                break;
            case AFTER:
                parent.addAfter(element, anchorElement);
                break;
        }
    }

    public void setNavigatorElementWindow(NavigatorElement element, String windowName) throws ScriptingErrorLog.SemanticErrorException {
        assert element != null && windowName != null;

        AbstractWindow window = findWindowByCompoundName(windowName);
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, windowName);
        }

        if (window instanceof NavigatorWindow) {
            element.window = (NavigatorWindow) window;
        } else {
            errLog.emitAddToSystemWindowError(parser, windowName);
        }
    }

    private void checkGroup(AbstractGroup group, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (group == null) {
            errLog.emitGroupNotFoundError(parser, name);
        }
    }

    private void checkClass(ValueClass cls, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (cls == null) {
            errLog.emitClassNotFoundError(parser, name);
        }
    }

    private void checkProperty(LP lp, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, name);
        }
    }

    private void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
        }
    }

    private void checkNamespace(String namespaceName) throws ScriptingErrorLog.SemanticErrorException {
        if (!namespaceToModules.containsKey(namespaceName)) {
            errLog.emitNamespaceNotFoundError(parser, namespaceName);
        }
    }

    private void checkWindow(AbstractWindow window, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, name);
        }
    }

    private void checkNavigatorElement(NavigatorElement element, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (element == null) {
            errLog.emitNavigatorElementNotFoundError(parser, name);
        }
    }

    private void checkTable(ImplementTable table, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (table == null) {
            errLog.emitTableNotFoundError(parser, name);
        }
    }

    private void checkForm(NavigatorElement navElement, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (!(navElement instanceof FormEntity)) {
            errLog.emitFormNotFoundError(parser, name);
        }
    }

    private void checkMetaCodeFragment(MetaCodeFragment code, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (code == null) {
            errLog.emitMetaCodeFragmentNotFoundError(parser, name);
        }
    }

    private void checkParamCount(LP mainProp, int paramCount) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramCount) {
            errLog.emitParamCountError(parser, mainProp, paramCount);
        }
    }

    private void checkPropertyValue(LP property, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property instanceof CalcProperty && !property.property.check()) {
            errLog.emitPropertyAlwaysNullError(parser, name);
        }
    }

    private void checkDuplicateClass(String className) throws ScriptingErrorLog.SemanticErrorException {
        if (classResolver.findInNamespace(getNamespace(), className) != null) {
            errLog.emitAlreadyDefinedError(parser, "class", className);
        }
    }

    private void checkDuplicateGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        if (groupResolver.findInNamespace(getNamespace(), groupName) != null) {
            errLog.emitAlreadyDefinedError(parser, "group", groupName);
        }
    }

    private void checkDuplicateProperty(String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (lpResolver.findInNamespace(getNamespace(), propName) != null) {
            errLog.emitAlreadyDefinedError(parser, "property", propName);
        }
    }

    private void checkDuplicateWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (windowResolver.findInNamespace(getNamespace(), name) != null) {
            errLog.emitAlreadyDefinedError(parser, "window", name);
        }
    }

    private void checkDuplicateNavigatorElement(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (navigatorResolver.findInNamespace(getNamespace(), name) != null) {
            errLog.emitAlreadyDefinedError(parser, "form or navigator", name);
        }
    }

    private void checkDuplicateMetaCodeFragment(String name, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        metaCodeFragmentResolver.setParamCnt(paramCnt);
        if (metaCodeFragmentResolver.findInNamespace(getNamespace(), name) != null) {
            errLog.emitAlreadyDefinedError(parser, "meta code", name);
        }
    }

    private void checkDuplicateTable(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (tableResolver.findInNamespace(getNamespace(), name) != null) {
            errLog.emitAlreadyDefinedError(parser, "table", name);
        }
    }

    private void checkUnionPropertyParams(List<LPWithParams> uPropParams, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        int paramCnt = uPropParams.get(0).property.property.interfaces.size();
        if (mergeAllParams(uPropParams).size() != paramCnt) {
            errLog.emitUnionArgumentsEqualParamsCountError(parser, errMsgPropType);
        }
        for (LPWithParams lp : uPropParams) {
            if (lp.property.property.interfaces.size() != paramCnt) {
                errLog.emitUnionArgumentsEqualParamsCountError(parser, errMsgPropType);
            }
        }
    }

    private void checkCasePropertyParams(List<LPWithParams> whenProps, List<LPWithParams> thenProps) throws ScriptingErrorLog.SemanticErrorException {
        int paramCnt = thenProps.get(0).property.property.interfaces.size();
        for (int i = 1; i < thenProps.size(); i++) {
            LPWithParams thenProp = thenProps.get(i);
            if (thenProp.property.property.interfaces.size() != paramCnt) {
                errLog.emitCasePropDiffThenParamsCountError(parser);
            }
        }

        List<Integer> thenParams = mergeAllParams(thenProps);

        for (LPWithParams whenProp : whenProps) {
            for (int whenParam : whenProp.usedParams) {
                if (!thenParams.contains(whenParam)) {
                    errLog.emitCasePropWhenParamMissingInThenParams(parser);
                }
            }
        }
    }

    private void checkStaticClassConstraints(String className, boolean isStatic, boolean isAbstract, List<String> instNames, List<String> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isStatic && isAbstract) {
            errLog.emitAbstractStaticClassError(parser);
        } else if (!isStatic && instNames.size() > 0) {
            errLog.emitNonStaticHasInstancesError(parser, className);
        } else if (isStatic) {
            Set<String> names = new HashSet<String>();
            for (String name : instNames) {
                if (names.contains(name)) {
                    errLog.emitAlreadyDefinedError(parser, "instance", name);
                }
                names.add(name);
            }
        }
    }

    private void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        Set<String> parentsSet = new HashSet<String>();
        for (String parentName : parents) {
            ValueClass valueClass = findClassByCompoundName(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }
            if (valueClass instanceof StaticCustomClass) {
                errLog.emitStaticClassAsParentError(parser, parentName);
            }

            if (parentsSet.contains(parentName)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            parentsSet.add(parentName);
        }
    }

    private void checkFormulaClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormulaReturnClassError(parser);
        }
    }

    private void checkFormDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormDataClassError(parser);
        }
    }

    private void checkChangeClassActionClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof ConcreteCustomClass)) {
            errLog.emitChangeClassActionClassError(parser);
        }
    }

    private void checkFormulaParameters(Set<Integer> params) throws ScriptingErrorLog.SemanticErrorException {
        for (int param : params) {
            if (param == 0 || param > params.size()) {
                errLog.emitParamIndexError(parser, param, params.size());
            }
        }
    }

    private void checkNamedParams(LP property, List<String> namedParams) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() != namedParams.size() && !namedParams.isEmpty()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    private void checkDistinctParameters(List<String> params) throws ScriptingErrorLog.SemanticErrorException {
        Set<String> paramsSet = new HashSet<String>(params);
        if (paramsSet.size() < params.size()) {
            errLog.emitDistinctParamNamesError(parser);
        }
    }

    private void checkMetaCodeParamCount(MetaCodeFragment code, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (code.parameters.size() != paramCnt) {
            errLog.emitParamCountError(parser, code.parameters.size(), paramCnt);
        }
    }

    private void checkGPropOrderConsistence(GroupingType type, int orderParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && orderParamsCnt > 0) {
            errLog.emitRedundantOrderGPropError(parser, type);
        }
    }

    private void checkGPropAggregateConsistence(GroupingType type, int aggrParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && aggrParamsCnt > 1) {
            errLog.emitMultipleAggrGPropError(parser, type);
        }
        if (type == GroupingType.CONCAT && aggrParamsCnt != 2) {
            errLog.emitConcatAggrGPropError(parser);
        }
    }

    private void checkGPropUniqueConstraints(GroupingType type, List<LPWithParams> mainProps, List<LPWithParams> groupProps) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.UNIQUE) {
            if (mainProps.get(0).property != null) {
                errLog.emitNonObjectAggrUniqueGPropError(parser);
            }
            //todo [dale]: добавить ошибку для группировочных свойств
        }
    }

    public void checkActionAllParamsUsed(List<String> context, LP property, boolean ownContext) throws ScriptingErrorLog.SemanticErrorException {
        if (ownContext && context.size() > property.property.interfaces.size()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    public void checkActionProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LAP<?>)) {
            errLog.emitNotActionPropertyError(parser);
        }
    }

    public void checkAddActionsClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitAddActionsClassError(parser);
        }
    }

    public void checkCalculationProperty(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property instanceof LCP<?>)) {
            errLog.emitNotCalculationPropertyError(parser);
        }
    }

    public void checkActionLocalContext(List<String> oldContext, List<String> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (oldContext.size() != newContext.size()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    private void checkForActionPropertyConstraints(boolean isRecursive, List<Integer> oldContext, List<Integer> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (!isRecursive && oldContext.size() == newContext.size()) {
            errLog.emitForActionSameContextError(parser);
        }
    }

    private void checkRecursionContext(List<String> context, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        for (String param : context) {
            if (param.startsWith("$")) {
                int indexPlain = context.indexOf(param.substring(1));
                if (indexPlain < 0) {
                    errLog.emitParamNotFoundError(parser, param.substring(1));
                }
                if (!usedParams.contains(indexPlain)) {
                    errLog.emitParameterNotUsedInRecursionError(parser, param.substring(1));
                }
            }
        }
    }

    public void checkNecessaryProperty(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property == null) {
            errLog.emitNecessaryPropertyError(parser);
        }
    }

    public void checkDeconcatenateIndex(LPWithParams property, int index) throws ScriptingErrorLog.SemanticErrorException {
        Type propType = property.property.property.getType();
        if (propType instanceof ConcatenateType) {
            int concatParts = ((ConcatenateType) propType).getPartsCount();
            if (index <= 0 || index > concatParts) {
                errLog.emitDeconcatIndexError(parser, index, concatParts);
            }
        } else {
            errLog.emitDeconcatError(parser);
        }
    }

    private void checkPartitionWindowConsistence(PartitionType partitionType, boolean useLast) throws ScriptingErrorLog.SemanticErrorException {
        if (!useLast && (partitionType != PartitionType.SUM && partitionType != PartitionType.PREVIOUS)) {
            errLog.emitIllegalWindowPartitionError(parser);
        }
    }

    private void checkPartitionUngroupConsistence(LP ungroupProp, int groupPropCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (ungroupProp != null && ungroupProp.property.interfaces.size() != groupPropCnt) {
            errLog.emitUngroupParamsCntPartitionError(parser, groupPropCnt);
        }
    }

    private void checkFormActionObjectsMapping(List<String> objects, List<LPWithParams> mapping) throws ScriptingErrorLog.SemanticErrorException {
        if (objects.size() != mapping.size() && mapping.size() > 0) {
            errLog.emitFormActionObjectsMappingError(parser);
        }
    }

    private void checkLocalDataPropertyName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (currentLocalProperties.containsKey(name)) {
            errLog.emitAlreadyDefinedError(parser, "local property", name);
        } else if (lpResolver.resolve(name) != null) {
            errLog.emitAlreadyDefinedError(parser, "property", name);
        }
    }

    private void checkClassWhere(LP property, String name) {
        ClassWhere<Integer> classWhere = property.getClassWhere();
        boolean needWarning = false;
        if (classWhere.wheres.length > 1) {
            needWarning = true;
        } else {
            AbstractClassWhere.And<Integer> where = classWhere.wheres[0];
            for (int i = 0; i < where.size(); ++i) {
                AndClassSet acSet = where.getValue(i);
                if (acSet instanceof UpClassSet && ((UpClassSet)acSet).wheres.length > 1 ||
                    acSet instanceof OrObjectClassSet && ((OrObjectClassSet)acSet).up.wheres.length > 1) {

                    needWarning = true;
                    break;
                }
            }
        }
        if (needWarning) {
            warningList.add(" Property " + name + " has class where " + classWhere);
        }
    }

    public void checkAbstractProperty(LP property, String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property.property instanceof CaseUnionProperty && ((CaseUnionProperty)property.property).isAbstract()) &&
            !(property.property instanceof ListActionProperty && ((ListActionProperty)property.property).isAbstract())) {
            errLog.emitNotAbstractPropertyError(parser, propName);
        }
    }

    public void checkEventNoParameters(LP property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() > 0) {
            errLog.emitEventNoParametersError(parser);
        }
    }

    public void initModulesAndNamespaces(List<String> requiredModules, List<String> namespacePriority) throws ScriptingErrorLog.SemanticErrorException {
        initNamespacesToModules(this, new HashSet<LogicsModule>());

        if (getNamespace().contains("_")) {
            errLog.emitNamespaceNameError(parser, getNamespace());
        }

        if (namespacePriority.contains(getNamespace())) {
            errLog.emitOwnNamespacePriorityError(parser, getNamespace());
        }

        for (String namespaceName : namespacePriority) {
            checkNamespace(namespaceName);
        }

        for (String moduleName : requiredModules) {
            checkModule(BL.getModule(moduleName), moduleName);
        }

        Set<String> prioritySet = new HashSet<String>();
        for (String namespaceName : namespacePriority) {
            if (prioritySet.contains(namespaceName)) {
                errLog.emitNonUniquePriorityListError(parser, namespaceName);
            }
            prioritySet.add(namespaceName);
        }
    }

    public boolean semicolonNeeded() {
        return parser.semicolonNeeded();
    }

    public void setPropertyScriptInfo(LP property, String script, int lineNumber) {
        property.setCreationScript(script);
        property.setCreationPath(parser.getCurrentScriptPath(getName(), lineNumber, "\n"));
    }

    private void parseStep(ScriptParser.State state) throws RecognitionException {
        try {
            parser.initParseStep(this, createStream(), state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initNamespacesToModules(LogicsModule module, Set<LogicsModule> visitedModules) {
        visitedModules.add(module);
        String namespaceName = module.getNamespace();
        if (!namespaceToModules.containsKey(namespaceName)) {
            namespaceToModules.put(namespaceName, BaseUtils.toList(module));
        } else {
            namespaceToModules.get(namespaceName).add(module);
        }
        for (String requiredModuleName : module.getRequiredModules()) {
            LogicsModule requiredModule = BL.getModule(requiredModuleName);
            assert requiredModule != null;
            if (!visitedModules.contains(requiredModule)) {
                initNamespacesToModules(requiredModule, visitedModules);
            }
        }
    }

    @Override
    public void initModuleDependencies() throws RecognitionException {
        parseStep(ScriptParser.State.PRE);
    }

    @Override
    public void initModule() throws RecognitionException {
        parseStep(ScriptParser.State.INIT);
    }

    @Override
    public void initClasses() throws RecognitionException {
        initBaseClassAliases();
        parseStep(ScriptParser.State.CLASS);
    }

    @Override
    public void initTables() throws RecognitionException {
        parseStep(ScriptParser.State.TABLE);
    }

    @Override
    public void initGroups() throws RecognitionException {
        initBaseGroupAliases();
        parseStep(ScriptParser.State.GROUP);
    }

    @Override
    public void initProperties() throws RecognitionException {
        warningList.clear();
        parseStep(ScriptParser.State.PROP);
    }

    @Override
    public void initIndexes() throws RecognitionException {
        parseStep(ScriptParser.State.INDEX);
        if (!parser.isInsideMetacode()) {
            showWarnings();
        }
    }

    public void initScriptingModule(String name, String namespace, List<String> requiredModules, List<String> namespacePriority) {
        setModuleName(name);
        setNamespace(namespace == null ? name : namespace);
        setRequiredModules(requiredModules);
        this.namespacePriority = namespacePriority;
    }

    public void initAliases() {
        initBaseGroupAliases();
        initBaseClassAliases();
    }

    private void showWarnings() {
        for (String warningText : warningList) {
            scriptLogger.warn("WARNING!" + warningText);
        }
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
    }

    @Override
    public String getNamePrefix() {
        return getNamespace();
    }

    public abstract class CompoundNameResolver<T> {
        public T findInNamespace(String namespaceName, String name) {
            T result = null;
            for (LogicsModule module : namespaceToModules.get(namespaceName)) {
                if ((result = resolveInModule(module, name)) != null) {
                    return result;
                }
            }
            return result;
        }

        private List<LogicsModule> findInRequiredModules(String name, List<String> namespaces) {
            List<LogicsModule> outModules = new ArrayList<LogicsModule>();

            for (String namespaceName : namespaces) {
                for (LogicsModule module : namespaceToModules.get(namespaceName)) {
                    if (resolveInModule(module, name) != null) {
                        outModules.add(module);
                        return outModules;
                    }
                }
            }

            Set<String> checkedNamespaces = new HashSet<String>(namespaces);
            for (Map.Entry<String, List<LogicsModule>> e : namespaceToModules.entrySet()) {
                if (!checkedNamespaces.contains(e.getKey())) {
                    for (LogicsModule module : e.getValue()) {
                        if (resolveInModule(module, name) != null) {
                            outModules.add(module);
                            break;
                        }
                    }
                }
            }
            return outModules;
        }

        public final T resolve(String name) throws ScriptingErrorLog.SemanticErrorException {
            T result = null;
            int dotPosition = name.indexOf('.');
            if (dotPosition > 0) {
                String namespaceName = name.substring(0, dotPosition);
                checkNamespace(namespaceName);
                result = findInNamespace(namespaceName, name.substring(dotPosition + 1));
            } else {
                result = resolveInModule(ScriptingLogicsModule.this, name);
                if (result == null) {
                    List<String> namespaces = new ArrayList<String>();
                    namespaces.add(getNamespace());
                    namespaces.addAll(namespacePriority);
                    List<LogicsModule> containingModules = findInRequiredModules(name, namespaces);
                    if (containingModules.size() > 1) {
                        errLog.emitAmbiguousNameError(parser, containingModules, name);
                    } else if (containingModules.size() == 1) {
                        result = resolveInModule(containingModules.get(0), name);
                    }
                }
            }
            return result;
        }

        public abstract T resolveInModule(LogicsModule module, String simpleName);
    }

    private class LPNameResolver extends CompoundNameResolver<LP<?, ?>> {
        @Override
        public LP<?, ?> resolveInModule(LogicsModule module, String simpleName) {
            return module.getLPByName(simpleName);
        }
    }

    private class AbstractGroupNameResolver extends CompoundNameResolver<AbstractGroup> {
        @Override
        public AbstractGroup resolveInModule(LogicsModule module, String simpleName) {
            return module.getGroupByName(simpleName);
        }
    }

    private class NavigatorElementNameResolver extends CompoundNameResolver<NavigatorElement> {
        @Override
        public NavigatorElement resolveInModule(LogicsModule module, String simpleName) {
            return module.getNavigatorElementByName(simpleName);
        }
    }

    private class WindowNameResolver extends CompoundNameResolver<AbstractWindow> {
        @Override
        public AbstractWindow resolveInModule(LogicsModule module, String simpleName) {
            return module.getWindowByName(simpleName);
        }
    }

    private class MetaCodeFragmentNameResolver extends CompoundNameResolver<MetaCodeFragment> {
        private int paramCnt;

        public void setParamCnt(int paramCnt) {
            this.paramCnt = paramCnt;
        }

        @Override
        public MetaCodeFragment resolveInModule(LogicsModule module, String simpleName) {
            return module.getMetaCodeFragmentByName(simpleName, paramCnt);
        }
    }

    private class TableNameResolver extends CompoundNameResolver<ImplementTable> {
        @Override
        public ImplementTable resolveInModule(LogicsModule module, String simpleName) {
            return module.getTableByName(simpleName);
        }
    }

    private class ClassNameResolver extends CompoundNameResolver<ValueClass> {
        @Override
        public ValueClass resolveInModule(LogicsModule module, String simpleName) {
            return module.getClassByName(simpleName);
        }
    }
}
