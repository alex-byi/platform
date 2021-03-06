package lsfusion.server.data.expr.join.classes;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.ContextEnumerator;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.classes.ClassWhere;

public class InnerExprFollows<K extends OuterContext> extends InnerFollows<K> implements OuterContext<InnerExprFollows<K>> { //InnerFollows<InnerExprFollows<K>>

    private InnerExprFollows(ImMap<K, ImSet<IsClassField>> fields) {
        super(fields);
    }

    public class OuterContext extends AbstractOuterContext<OuterContext> {

        protected ImSet<lsfusion.server.data.caches.OuterContext> calculateOuterDepends() {
            return (ImSet<lsfusion.server.data.caches.OuterContext>) fields.keys();
        }

        protected OuterContext translate(MapTranslate translator) {
            return new InnerExprFollows<>(translator.translateOuterKeys(fields)).getOuter();
        }

        public int hash(HashContext hash) {
            return AbstractOuterContext.hashKeysOuter(fields, hash);
        }

        protected boolean calcTwins(TwinImmutableObject o) {
            return getThis().equals(((OuterContext) o).getThis());
        }

        public InnerExprFollows<K> getThis() {
            return InnerExprFollows.this;
        }
    }
    private OuterContext outer;
    @ManualLazy
    public OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }

    private final static InnerExprFollows EMPTYEXPR = new InnerExprFollows(MapFact.EMPTY());
    public static <K extends lsfusion.server.data.caches.OuterContext> InnerExprFollows<K> EMPTYEXPR() {
        return EMPTYEXPR;
    }

    public InnerExprFollows(ClassWhere<K> classWhere, ImSet<K> keys) {
        super(classWhere, keys, null);
    }

    public ImSet<ParamExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        return getOuter().getOuterStaticValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<lsfusion.server.data.caches.OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public InnerExprFollows<K> translateOuter(MapTranslate translator) {
        return getOuter().translateOuter(translator).getThis();
    }
    public boolean enumerate(ContextEnumerator enumerator) {
        return getOuter().enumerate(enumerator);
    }
    public InnerExprFollows<K> pack() {
        throw new UnsupportedOperationException(); // getOuter
    }
    public long getComplexity(boolean outer) {
        throw new UnsupportedOperationException(); // return getOuter().getComplexity(outer);
    }
}
