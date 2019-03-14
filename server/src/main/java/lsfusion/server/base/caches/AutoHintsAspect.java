package lsfusion.server.base.caches;

import lsfusion.base.*;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.hash.HashCodeKeys;
import lsfusion.server.base.caches.hash.HashContext;
import lsfusion.server.base.caches.hash.HashValues;
import lsfusion.server.data.HandledException;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.infer.AlgType;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.Settings;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.MapCacheAspect;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.SessionModifier;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.max;

@Aspect
public class AutoHintsAspect {

    public static ThreadLocal<SessionModifier> catchAutoHint = new ThreadLocal<>();
    public static ThreadLocal<Boolean> catchNotFirst = new ThreadLocal<>();

    public static ThreadLocal<Integer> catchDisabledRepeat = new ThreadLocal<>();
    public static void pushDisabledRepeat() {
        Integer prev = catchDisabledRepeat.get();
        if(prev == null)
            prev = 0;
        catchDisabledRepeat.set(prev + 1);
    }
    private static boolean isDisabledRepeat() {
        Integer disabled = catchDisabledRepeat.get();
        return disabled != null && disabled > 0;
    }
    public static void popDisabledRepeat() {
        int prev = catchDisabledRepeat.get();
        assert prev > 0;
        if(prev == 1)
            catchDisabledRepeat.remove();
        else
            catchDisabledRepeat.set(prev - 1);
    }

    // limitHints - тоже надо включать
    public static class AutoHintImplement<P extends PropertyInterface> extends AbstractInnerContext<AutoHintImplement<P>> {
        private final PropertyChanges usedChanges;
        private final ImMap<P, Expr> joinImplement;
        private final ImMap<CalcProperty, Byte> usedHints;
        private final ImMap<CalcProperty, ValuesContext> usedPrereads;

        public AutoHintImplement(CalcProperty<P> property, ImMap<P, Expr> joinImplement, SessionModifier modifier) throws SQLException, SQLHandledException {
            usedChanges = property.getUsedChanges(modifier.getPropertyChanges());
            this.joinImplement = joinImplement;

            ImSet<CalcProperty> depends = property.getRecDepends();
            ImFilterValueMap<CalcProperty,Byte> mvUsedHints = depends.mapFilterValues();
            ImFilterValueMap<CalcProperty,ValuesContext> mvUsedPrereads = depends.mapFilterValues();
            for(int i=0,size=depends.size();i<size;i++) {
                CalcProperty dependProperty = depends.get(i);

                byte result = 0;
                if(dependProperty.isFull(AlgType.hintType) && modifier.allowHintIncrement(dependProperty)) {
                    result |= 1;
                    if(modifier.forceHintIncrement(dependProperty))
                        result |= 2;
                    if(modifier.allowNoUpdate(dependProperty)) {
                        result |= 4;
                        if(modifier.forceNoUpdate(dependProperty))
                            result |= 8;
                    }
                }
                if(result!=0)
                    mvUsedHints.mapValue(i, result);

                ValuesContext cachePreread = modifier.cacheAllowPrereadValues(dependProperty);
                if(cachePreread!=null)
                    mvUsedPrereads.mapValue(i, cachePreread);
            }
            usedHints = mvUsedHints.immutableValue();
            usedPrereads = mvUsedPrereads.immutableValue();
        }

        public boolean equalsInner(AutoHintImplement<P> o) {
            return BaseUtils.nullHashEquals(joinImplement, o.joinImplement) && BaseUtils.hashEquals(usedChanges, o.usedChanges) && BaseUtils.hashEquals(usedHints, o.usedHints) && BaseUtils.hashEquals(usedPrereads, o.usedPrereads);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * (31 * (31 * usedChanges.hashValues(hashContext.values) + (joinImplement==null ? 0 : AbstractOuterContext.hashOuter(joinImplement, hashContext))) + usedHints.hashCode()) + MapValuesIterable.hash(usedPrereads, hashContext.values);
        }

        public ImSet<ParamExpr> getKeys() {
            if(joinImplement==null)
                return SetFact.EMPTY();
            return AbstractOuterContext.getOuterColKeys(joinImplement.values());
        }

        public ImSet<Value> getValues() {
            ImSet<Value> result = usedChanges.getContextValues().merge(MapValuesIterable.getContextValues(usedPrereads));
            if(joinImplement!=null)
                result = AbstractOuterContext.getOuterColValues(joinImplement.values()).merge(result);
            return result;
        }

        private AutoHintImplement(AutoHintImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            joinImplement = implement.joinImplement == null ? null : translator.translate(implement.joinImplement);
            usedHints = implement.usedHints;
            usedPrereads = translator.mapValues().translateValues(implement.usedPrereads);
        }

        public AutoHintImplement<P> translate(MapTranslate translator) {
            return new AutoHintImplement<>(this, translator);
        }
    }

    public <P extends PropertyInterface> Object callAutoHint(ProceedingJoinPoint thisJoinPoint, CalcProperty<P> property, ImMap<P, Expr> joinImplement, Modifier modifier) throws Throwable {
        if(!Settings.get().isDisableAutoHints() && modifier instanceof SessionModifier) { // && property.hasChanges(modifier) иначе в рекурсию уходит при changeModifier'е, надо было бы внутрь перенести
            SessionModifier sessionModifier = (SessionModifier) modifier;

            Result<Hint> resultHint = new Result<>();
            Object result = proceedCached(thisJoinPoint, property, joinImplement, sessionModifier, resultHint);
            if(result!=null || isDisabledRepeat())
                return result;

            resultHint.result.resolve(sessionModifier);

            Boolean isFirst = null;
            if(resultHint.result instanceof PrereadHint && (isFirst = catchNotFirst.get())==null) // оптимизация
                catchNotFirst.set(true);

            result = ReflectionUtils.invokeTransp(((MethodSignature) thisJoinPoint.getSignature()).getMethod(), thisJoinPoint.getTarget(), thisJoinPoint.getArgs());

            if(isFirst == null) {
                catchNotFirst.set(null);
                sessionModifier.clearPrereads();
            }

            return result;
        } else
            return thisJoinPoint.proceed();
    }

    private <P extends PropertyInterface> Object proceedCached(ProceedingJoinPoint thisJoinPoint, CalcProperty<P> property, ImMap<P, Expr> joinImplement, SessionModifier sessionModifier, Result<Hint> resultHint) throws Throwable {
        if(Settings.get().isDisableAutoHintCaches())
            return proceed(thisJoinPoint, sessionModifier, resultHint);

        AutoHintImplement<P> implement = new AutoHintImplement<>(property, joinImplement, sessionModifier);
        MAddCol<MapCacheAspect.CacheResult<AutoHintImplement,Hint>> hashCaches = MapCacheAspect.getCachedCol(property, implement.getInnerComponents(true).hash, MapCacheAspect.Type.AUTOHINT);
        synchronized(hashCaches) {
            Hint cacheHint = null;
            for(MapCacheAspect.CacheResult<AutoHintImplement,Hint> cache : hashCaches.it()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        cacheHint = ((Hint<?>) cache.result).translateValues(mapValues);
                        break;
                    }
                }
            }

            if(cacheHint==null || MapCacheAspect.checkCaches()) {
                CacheStats.incrementMissed(CacheStats.CacheType.AUTOHINT);

                Object result = proceed(thisJoinPoint, sessionModifier, resultHint);
                if(result!=null)
                    return result;

                if(cacheHint!=null)
                    MapCacheAspect.logCaches(cacheHint, resultHint.result, thisJoinPoint, "HINT", property);
                else
                    MapCacheAspect.cacheNoBig(implement, hashCaches, resultHint.result);
            } else {
                resultHint.set(cacheHint);
                CacheStats.incrementHit(CacheStats.CacheType.AUTOHINT);
            }

            return null;
        }
    }

    private Object proceed(ProceedingJoinPoint thisJoinPoint, SessionModifier sessionModifier, Result<Hint> resultHint) throws Throwable {
        SessionModifier prevModifier = catchAutoHint.get();
        catchAutoHint.set(sessionModifier);
        Object result;
        try {
            result = thisJoinPoint.proceed();
            assert catchAutoHint.get()==sessionModifier;
//                        assert cacheHint == null; вообще не работает так как могла вычислится с другим modifier'ом и закэшироваться
            return result; // ничего не пишем в кэш
        } catch (HintException e) {
            Hint hint = e.hint;
            assert catchAutoHint.get()==sessionModifier;

            resultHint.set(hint);
            return null;
        } finally {
            catchAutoHint.set(prevModifier);
        }
    }

    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getExpr(lsfusion.base.col.interfaces.immutable.ImMap, Modifier, lsfusion.server.data.where.WhereBuilder)) && target(property) && args(map, modifier, changedWhere)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap map, Modifier modifier, WhereBuilder changedWhere) throws Throwable {
        return callAutoHint(thisJoinPoint, property, map, modifier);
    }
    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getIncrementChange(Modifier)) && target(property) && args(modifier)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, CalcProperty property, Modifier modifier) throws Throwable {
        return callAutoHint(thisJoinPoint, property, null, modifier);
    }

    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getQuery(lsfusion.server.logics.property.infer.CalcType,PropertyChanges,lsfusion.server.logics.property.PropertyQueryType,*)) && target(property) && args(calcType, propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, AMap interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, calcType, propChanges, queryType, interfaceValues);
    }

    private static boolean hintHasChanges(Where changed, CalcProperty property, PropertyChanges propChanges) {
        if(changed!=null)
            return !changed.isFalse();
        return property.hasChanges(propChanges);
    }
    private static Object getQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, AMap interfaceValues) throws Throwable {
        assert property.isNotNull(calcType.getAlgInfo());
        
        if(!property.isFull(calcType.getAlgInfo()) || !calcType.isExpr())
            return thisJoinPoint.proceed();

        SessionModifier catchHint = catchAutoHint.get();
        if(calcType.isExpr() && catchHint!=null && catchHint.allowPrereadValues(property,interfaceValues)) // если есть не "прочитанные" параметры - значения, вычисляем
            throw new HintException(new PrereadHint(property, interfaceValues));

        IQuery<?, String> result = (IQuery) thisJoinPoint.proceed();
        if(queryType == PropertyQueryType.RECURSIVE)
            return result;

        if(calcType.isExpr() && catchHint != null && !propChanges.isEmpty() // проверка на пустоту для оптимизации при старте 
                && catchHint.allowHintIncrement(property) && !property.isNoHint()) { // неправильно так как может быть не changed
            Where changed = null;
            if(queryType.needChange())
                changed = result.getExpr("changed").getWhere();

            if(catchHint.forceHintIncrement(property) && hintHasChanges(changed, property, propChanges)) // сразу кидаем exception
                throw new HintException(new IncrementHint(property, true));
            boolean allowNoUpdate = catchHint.allowNoUpdate(property);
            if(allowNoUpdate && catchHint.forceNoUpdate(property) && hintHasChanges(changed, property, propChanges))
                throw new HintException(new IncrementHint(property, false));

            Expr expr = result.getExpr("value");
            long exprComplexity = expr.getComplexity(false);
            long whereComplexity = 0;
            if(changed!=null)
                whereComplexity = changed.getComplexity(false);
            long complexity = max(exprComplexity, whereComplexity);

            if(interfaceValues.isEmpty() || (complexity > catchHint.getLimitHintIncrementValueComplexity() && !property.isComplex())) { // нужен чтобы не цеплять много лишних записей, потенциально конечно опасно, для этого сделан отдельный порог и проверка на complex (но все равно не максимально надежное решение, в общем случае надо по аналогии с preread change'и для interfaceValues хранить)
                int limitComplexity = catchHint.getLimitHintIncrementComplexity();
                if(complexity > limitComplexity && property.hasChanges(propChanges)) { // сложность большая, если нет изменений то ничем не поможешь
                    if (interfaceValues.isEmpty() && queryType == PropertyQueryType.FULLCHANGED) {
                        ImRevMap<?, KeyExpr> mapKeys = result.getMapKeys();
                        if(whereComplexity > limitComplexity || exprComplexity > property.getExpr(mapKeys).getComplexity(false) * catchHint.getLimitComplexityGrowthCoeff()) { // вторая проверка как и использование limitComplexity а не prevComplexity (max с ним), чтобы заставить ее более агрессивно hint'ить, когда общая complexity зашкаливает (min не используется чтобы не hint'ить раньше времени)
                            long baseLimit = catchHint.getLimitHintIncrementStat();
                            long maxCountUsed = catchHint.getMaxCountUsed(property);
                            // будем считать что рост сложности полиномиальный (квадратичный с учетом того что A x B, выполняется за условно AB операций), в то же время сложность агрегации условно линейный
                            long limit = BaseUtils.max(maxCountUsed, baseLimit) * complexity * complexity / limitComplexity / limitComplexity; // возможно надо будет все же экспоненту сделать 
                            if (changed.isFalse() || changed.getFullStatKeys(mapKeys.valuesSet(), StatType.HINTCHANGE).getRows().lessEquals(new Stat(limit))) { // тут может быть проблема с интервалами (см. CalcProperty.allowHintIncrement)
                                throw new HintException(new IncrementHint(property, true));
                            }
                            if(allowNoUpdate && complexity > catchHint.getLimitHintNoUpdateComplexity()) {
                                System.out.println("AUTO HINT NOUPDATE" + property);
                                throw new HintException(new IncrementHint(property, false));
                            }
                        }
                    } else // запускаем getQuery уже без interfaceValues, соответственно уже оно если надо (в смысле что статистика будет нормальной) кинет exception
                        property.getQuery(calcType, propChanges, PropertyQueryType.FULLCHANGED, MapFact.EMPTY());
                }
            }
        }
        return result;
    }


    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getJoinExpr(lsfusion.base.col.interfaces.immutable.ImMap,lsfusion.server.logics.property.infer.CalcType,PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,calcType,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть

        if(!property.isFull(calcType.getAlgInfo()) || !calcType.isExpr())
            return thisJoinPoint.proceed();

        SessionModifier catchHint = catchAutoHint.get();

        ImMap<PropertyInterface, Expr> joinValues;
        if(catchHint!=null && catchHint.allowPrereadValues(property, joinValues = CalcProperty.getJoinValues(joinExprs))) {
            throw new HintException(new PrereadHint(property, joinValues));
        }

        if(catchHint == null || !catchHint.allowHintIncrement(property)) // неправильно так как может быть не changed
            return thisJoinPoint.proceed();

        WhereBuilder cascadeWhere = CalcProperty.cascadeWhere(changedWhere);
        Expr result = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, calcType, propChanges, cascadeWhere});

        long complexity = BaseUtils.max(result.getComplexity(false), (changedWhere != null ? cascadeWhere.toWhere().getComplexity(false) : 0));
        if(complexity > Settings.get().getLimitHintIncrementComplexityCoeff() && property.hasChanges(propChanges))
            property.getQuery(calcType, propChanges, PropertyQueryType.FULLCHANGED, MapFact.EMPTY()); // по аналогии с верхним
        
        if(changedWhere!=null) changedWhere.add(cascadeWhere.toWhere());
        return result;
    }

    public static class PrereadHint extends Hint<PrereadHint> {

        public final CalcProperty<PropertyInterface> property;
        public final ImMap<PropertyInterface, Expr> values;
        public PrereadHint(CalcProperty property, ImMap<PropertyInterface, Expr> values) {
            this.property = property;
            this.values = values;
        }

        public ImSet<Value> getValues() {
            return AbstractOuterContext.getOuterColValues(values.values());
        }

        protected PrereadHint translate(MapValuesTranslate translator) {
            return new PrereadHint(property, translator.mapKeys().translate(values));
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return values.equals(((PrereadHint) o).values) && property.equals(((PrereadHint) o).property);
        }

        protected int hash(HashValues hash) {
            return 31 * property.hashCode() + AbstractOuterContext.hashOuter(values, new HashContext(HashCodeKeys.instance, hash));
        }

        public void resolve(SessionModifier modifier) throws SQLException, SQLHandledException {
            modifier.addPrereadValues(property, values);
        }
    }

    public static class IncrementHint extends Hint<IncrementHint> {

        public final CalcProperty property;
        public final boolean lowstat;
        public IncrementHint(CalcProperty property, boolean lowstat) {
            this.property = property;
            this.lowstat = lowstat;
        }

        protected IncrementHint translate(MapValuesTranslate translator) {
            return this;
        }

        public ImSet<Value> getValues() {
            return SetFact.EMPTY();
        }

        protected int hash(HashValues hash) {
            return 31 * property.hashCode() + (lowstat ? 1 : 0);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return lowstat == ((IncrementHint) o).lowstat && property.equals(((IncrementHint) o).property);
        }

        @Override
        public void resolve(SessionModifier modifier) throws SQLException, SQLHandledException {
            if(lowstat)
                modifier.addHintIncrement(property);
            else
                modifier.addNoUpdate(property);
        }
    }

    public abstract static class Hint<H extends Hint<H>> extends AbstractValuesContext<H> {

        public abstract void resolve(SessionModifier modifier) throws SQLException, SQLHandledException;
    }

    public static class HintException extends RuntimeException implements HandledException {
        public final Hint hint;

        public HintException(Hint hint) {
            this.hint = hint;
        }

        public boolean willDefinitelyBeHandled() {
            return true;
        }
    }
}
