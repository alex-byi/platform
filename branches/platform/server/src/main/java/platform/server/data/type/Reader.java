package platform.server.data.type;

import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.Query;

import java.util.Map;

public interface Reader<T> {
    T read(Object value);

    void prepareClassesQuery(Expr expr, Query<?, Object> query, BaseClass baseClass);
    ConcreteClass readClass(Expr expr, Map<Object, Object> classes, BaseClass baseClass, KeyType keyType);
}
