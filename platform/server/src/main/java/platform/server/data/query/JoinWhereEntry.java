package platform.server.data.query;

import platform.server.where.Where;

public class JoinWhereEntry {
    Where join;
    Where where;

    JoinWhereEntry(Where iJoin, Where iWhere) {
        join = iJoin;
        where = iWhere;
    }
}
