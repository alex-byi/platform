package platform.server.data.expr.where;

import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;
import platform.server.classes.DataClass;
import platform.server.classes.UnknownClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.SingleClassExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import net.jcip.annotations.Immutable;

@Immutable
public class IsClassWhere extends DataWhere {

    private final SingleClassExpr expr;
    private final AndClassSet classes;

    // для того чтобы один и тот же JoinSelect все использовали
    private final IsClassExpr classExpr;

    public IsClassWhere(SingleClassExpr expr, AndClassSet classes) {
        this.expr = expr;
        this.classes = classes;

        assert !this.classes.isEmpty();
        if(this.classes instanceof ObjectClassSet)
            classExpr = (IsClassExpr) expr.classExpr(((ObjectClassSet) this.classes).getBaseClass());
        else
            classExpr = null;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return expr.getSource(compile) + " isClass(" + classes + ")";

        return ((ObjectClassSet)classes).getWhereString(classExpr.getSource(compile));
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    @Override
    protected String getNotSource(CompileSource compile) {
        return ((ObjectClassSet)classes).getNotWhereString(classExpr.getSource(compile));
    }

    @ParamLazy
    public Where translateDirect(DirectTranslator translator) {
        return new IsClassWhere(expr.translateDirect(translator),classes);
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return expr.translateQuery(translator).isClass(classes);
    }

    public void enumerate(SourceEnumerator enumerator) {
        expr.enumerate(enumerator);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        if(classes instanceof ObjectClassSet)
            classExpr.fillJoinWheres(joins,andWhere);
        else
            expr.fillJoinWheres(joins,andWhere);        
    }

    protected DataWhereSet calculateFollows() {
        return new DataWhereSet(expr.getExprFollows());
    }

    public static boolean isObjectValueClass(AndClassSet set) {
        return !(set instanceof UnknownClass || set instanceof DataClass);
    }

    public InnerJoins getInnerJoins() {
        if(expr instanceof KeyExpr && isObjectValueClass(classes))
            return new InnerJoins((KeyExpr)expr, ((ObjectClassSet)classes).getBaseClass() ,this);
        return expr.getWhere().getInnerJoins().and(new InnerJoins(this));
    }
    public ClassExprWhere calculateClassWhere() {
        return expr.getClassWhere(classes).and(expr.getWhere().getClassWhere());
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        return expr.hashContext(hashContext) + classes.hashCode()*31;
    }

    public boolean twins(AbstractSourceJoin obj) {
        return expr.equals(((IsClassWhere)obj).expr) && classes.equals(((IsClassWhere)obj).classes);
    }
}
