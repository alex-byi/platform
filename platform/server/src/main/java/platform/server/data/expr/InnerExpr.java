package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.HashMap;

@TranslateExprLazy
public abstract class InnerExpr extends VariableClassExpr implements JoinData {

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    public VariableExprSet calculateExprFollows() {
        VariableExprSet result = new VariableExprSet(getJoinFollows());
        result.add(this);
        return result;
    }

    public abstract VariableExprSet getJoinFollows();

    public abstract class NotNull extends DataWhere {

        public InnerExpr getExpr() {
            return InnerExpr.this;
        }

        protected DataWhereSet calculateFollows() {
            return new DataWhereSet(getJoinFollows());
        }

        public String getSource(CompileSource compile) {
            return InnerExpr.this.getSource(compile) + " IS NOT NULL";
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return InnerExpr.this.getSource(compile) + " IS NULL";
        }

        public Where translate(MapTranslate translator) {
            return InnerExpr.this.translate(translator).getWhere();
        }
        public Where translateQuery(QueryTranslator translator) {
            return InnerExpr.this.translateQuery(translator).getWhere();
        }

        public void enumerate(ContextEnumerator enumerator) {
            InnerExpr.this.enumerate(enumerator);
        }

        protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            InnerExpr.this.fillAndJoinWheres(joins,andWhere);
        }

        public int hashContext(HashContext hashContext) {
            return InnerExpr.this.hashContext(hashContext);
        }

        @Override
        public boolean twins(AbstractSourceJoin o) {
            return InnerExpr.this.equals(((NotNull) o).getExpr());
        }
    }

    public static <K> VariableExprSet getExprFollows(Map<K, BaseExpr> map) {
        return new VariableExprSet(map.values());
    }

    public void fillFollowSet(DataWhereSet fillSet) {
        fillSet.add((DataWhere) getWhere());
    }

}
