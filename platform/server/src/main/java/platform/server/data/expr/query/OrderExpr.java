package platform.server.data.expr.query;

import platform.base.*;
import platform.server.Settings;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.DataClass;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.pull.ExclPullCases;
import platform.server.data.expr.cases.pull.ExclWherePullCases;
import platform.server.data.expr.cases.pull.ExprPullCases;
import platform.server.data.query.*;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

public class OrderExpr extends QueryExpr<KeyExpr, OrderExpr.Query,OrderJoin> implements JoinData {

    public final OrderType orderType;

    public static class Query extends AbstractOuterContext<Query> {
        public Expr expr;
        public OrderedMap<Expr, Boolean> orders;
        public Set<Expr> partitions;

        public Query(Expr expr, OrderedMap<Expr, Boolean> orders, Set<Expr> partitions) {
            this.expr = expr;
            this.orders = orders;
            this.partitions = partitions;
        }

        @ParamLazy
        public Query translateOuter(MapTranslate translator) {
            return new Query(expr.translateOuter(translator),translator.translate(orders),translator.translate(partitions));
        }

        public boolean twins(TwinImmutableInterface o) {
            return expr.equals(((Query) o).expr) && orders.equals(((Query) o).orders) && partitions.equals(((Query) o).partitions);
        }

        @IdentityLazy
        public int hashOuter(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<Expr,Boolean> order : orders.entrySet())
                hash = hash * 31 + order.getKey().hashOuter(hashContext) ^ order.getValue().hashCode();
            hash = hash * 31;
            for(Expr partition : partitions)
                hash += partition.hashOuter(hashContext);
            return hash * 31 + expr.hashOuter(hashContext);
        }

        @IdentityLazy
        public Where getWhere() {
            return expr.getWhere().and(getGroupWhere());
        }

        @IdentityLazy
        public Where getGroupWhere() {
            return Expr.getWhere(orders.keySet()).and(Expr.getWhere(partitions));
        }

        @IdentityLazy
        public Type getType() {
            return expr.getType(getWhere());
        }

        @Override
        public String toString() {
            return "INNER(" + expr + "," + orders + "," + partitions + ")";
        }

        public SourceJoin[] getEnum() { // !!! Включим ValueExpr.TRUE потому как в OrderSelect.getSource - при проталкивании partition'а может создать TRUE
            return AbstractSourceJoin.merge(partitions,AbstractSourceJoin.merge(orders.keySet(), expr, ValueExpr.TRUE));
        }

        @IdentityLazy
        public Query pack() { // пока так
            return new Query(expr.pack(), orders, partitions);
        }
    }

    private OrderExpr(OrderType orderType, Map<KeyExpr,BaseExpr> group, Expr expr, OrderedMap<Expr, Boolean> orders, Set<Expr> partitions) {
        this(orderType, new Query(expr, orders, partitions),group);
    }

    // трансляция
    private OrderExpr(OrderExpr orderExpr, MapTranslate translator) {
        super(orderExpr, translator);
        orderType = orderExpr.orderType;
    }

    public OrderExpr translateOuter(MapTranslate translator) {
        return new OrderExpr(this,translator);
    }

    private OrderExpr(OrderType orderType, Query query, Map<KeyExpr, BaseExpr> group) {
        super(query, group);
        this.orderType = orderType;
    }

    protected OrderExpr createThis(Query query, Map<KeyExpr, BaseExpr> group) {
        return new OrderExpr(orderType, query, group);
    }

    public Type getType(KeyType keyType) {
        return query.getType();
    }

    public Where getFullWhere() {
        return query.getWhere();
    }

    public Where calculateWhere() {
        return getFullWhere().map(group);
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return new ExprPullCases<KeyExpr>() {
            protected Expr proceedBase(Map<KeyExpr, BaseExpr> map) {
                return createBase(orderType, map, query.expr, query.orders, query.partitions);
            }
        }.proceed(translator.translate(group));
    }

    @Override
    public String toString() {
        return "ORDER(" + query + "," + group + ")";
    }

    @Override
    public OrderJoin getGroupJoin() {
        return new OrderJoin(innerContext.getKeys(), innerContext.getValues(),query.getWhere(), Settings.instance.isPushOrderWhere() ?query.partitions:new HashSet<Expr>(),group);
    }

    // проталкивает внутрь partition'а Where
    public static Where getPartitionWhere(boolean cached, Where trueWhere, Map<KeyExpr, BaseExpr> group, Set<Expr> partitions) {
        Map<Object, Expr> partitionMap = BaseUtils.toObjectMap(partitions);
        if(cached) {
            platform.server.data.query.Query<KeyExpr,Object> mapQuery = new platform.server.data.query.Query<KeyExpr,Object>(BaseUtils.toMap(group.keySet())); // для кэша через Query
            mapQuery.properties.putAll(partitionMap);
            Join<Object> joinQuery = mapQuery.join(group);
            return GroupExpr.create(joinQuery.getExprs(),trueWhere,partitionMap).getWhere();
        } else
            return GroupExpr.create(new QueryTranslator(group).translate(partitionMap),trueWhere,partitionMap).getWhere();
    }

    @Override
    public Expr packFollowFalse(Where falseWhere) {
        Map<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(orderType, packedQuery.expr, packedQuery.orders, packedQuery.partitions, packedGroup);
        else
            return this;
    }

    protected static Expr createBase(OrderType orderType, Map<KeyExpr, BaseExpr> group, Expr expr, OrderedMap<Expr, Boolean> orders, Set<Expr> partitions) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Map<KeyExpr,BaseExpr> restGroup = new HashMap<KeyExpr, BaseExpr>();
        Set<Expr> restPartitions = new HashSet<Expr>(partitions);
        Map<KeyExpr,BaseExpr> translate = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> groupKey : group.entrySet())
            if(groupKey.getValue().isValue() && restPartitions.remove(groupKey.getKey()))
                translate.put(groupKey.getKey(), groupKey.getValue());
            else
                restGroup.put(groupKey.getKey(), groupKey.getValue());
        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate);
            expr = expr.translateQuery(translator);
            orders = translator.translate(orders);
            restPartitions = translator.translate(restPartitions);
        }

        return BaseExpr.create(new OrderExpr(orderType, restGroup,expr,orders,restPartitions));
    }

    public static Expr create(final OrderType orderType, final Expr expr, final OrderedMap<Expr, Boolean> orders, final Set<Expr> partitions, Map<KeyExpr, ? extends Expr> group) {
        return new ExprPullCases<KeyExpr>() {
            protected Expr proceedBase(Map<KeyExpr, BaseExpr> map) {
                return createBase(orderType, map, expr, orders, partitions);
            }
        }.proceed(group);
    }

    @Override
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        Type type = query.getType();
        if(type instanceof DataClass)
            return (AndClassSet) type;
        else {
            QuickMap<KeyExpr, AndClassSet> keyClasses = new SimpleMap<KeyExpr, AndClassSet>();
            for(Map.Entry<KeyExpr, BaseExpr> groupEntry : group.entrySet())
                keyClasses.add(groupEntry.getKey(), groupEntry.getValue().getAndClassSet(and));
            final ClassExprWhere keyWhere = new ClassExprWhere(keyClasses);

            return new ExclWherePullCases<AndClassSet>() {
                protected AndClassSet initEmpty() {
                    return null;
                }
                protected AndClassSet proceedBase(Where data, BaseExpr baseExpr) {
                    return data.getClassWhere().and(keyWhere).getAndClassSet(baseExpr);
                }
                protected AndClassSet add(AndClassSet op1, AndClassSet op2) {
                    if(op1 == null)
                        return op2;
                    if(op2 == null)
                        return op1;
                    return op1.or(op2);
                }
            }.proceed(query.getGroupWhere(), query.expr);
        }
    }
}
