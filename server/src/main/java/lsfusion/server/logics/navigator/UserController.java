package lsfusion.server.logics.navigator;

import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface UserController {

    boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException;
    Long getCurrentUserRole();
}
