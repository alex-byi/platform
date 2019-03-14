package lsfusion.server.physics.admin.backup;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static lsfusion.base.file.IOUtils.readFileToString;

public class BackupActionProperty extends ScriptingAction {

    public BackupActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        makeBackup(context, false);
    }

    protected void makeBackup(ExecutionContext context, boolean partial) {
        try (ExecutionContext.NewSession newContext = context.newSession()) {

            Date currentDate = Calendar.getInstance().getTime();
            long currentTime = currentDate.getTime();
            String backupFileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(currentDate);

            List<String> excludeTables = partial ? getExcludeTables(context) : new ArrayList<String>();

            String backupFilePath = context.getDbManager().getBackupFilePath(backupFileName);
            if (backupFilePath != null) {
                String backupFileLogPath = backupFilePath + ".log";
                String backupFileExtension = backupFilePath.substring(backupFilePath.lastIndexOf("."), backupFilePath.length());

                DataObject backupObject = newContext.addObject((ConcreteCustomClass) findClass("Backup"));
                findProperty("date[Backup]").change(new java.sql.Date(currentTime), newContext, backupObject);
                findProperty("time[Backup]").change(new java.sql.Time(currentTime), newContext, backupObject);
                findProperty("file[Backup]").change(backupFilePath, newContext, backupObject);
                findProperty("name[Backup]").change(backupFileName + backupFileExtension, newContext, backupObject);
                findProperty("fileLog[Backup]").change(backupFileLogPath, newContext, backupObject);

                if(partial) {
                    findProperty("partial[Backup]").change(true, newContext, backupObject);
                    for (String excludeTable : excludeTables) {
                        ObjectValue tableObject = findProperty("table[VARISTRING[100]]").readClasses(newContext, new DataObject(excludeTable));
                        if (tableObject instanceof DataObject)
                            findProperty("exclude[Backup,Table]").change(true, newContext, backupObject, (DataObject) tableObject);
                    }
                }

                newContext.apply();

                backupObject = new DataObject((Long)backupObject.object, (ConcreteCustomClass)findClass("Backup")); // обновляем класс после backup

                context.getDbManager().backupDB(context, backupFileName, excludeTables);

                findProperty("backupFilePath[]").change(backupFilePath, context.getSession());
                findProperty("backupFileName[]").change(backupFileName + backupFileExtension, context.getSession());

                findProperty("log[Backup]").change(readFileToString(backupFileLogPath), newContext, backupObject);
                newContext.apply();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private List<String> getExcludeTables(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr tableExpr = new KeyExpr("Table");
        ImRevMap<Object, KeyExpr> tableKeys = MapFact.<Object, KeyExpr>singletonRev("Table", tableExpr);

        QueryBuilder<Object, Object> tableQuery = new QueryBuilder<>(tableKeys);
        tableQuery.addProperty("sidTable", findProperty("sid[Table]").getExpr(context.getModifier(), tableExpr));
        tableQuery.and(findProperty("exclude[Table]").getExpr(context.getModifier(), tableExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableResult = tableQuery.execute(context.getSession());

        List<String> excludeTables = new ArrayList<>();
        for (ImMap<Object, Object> entry : tableResult.values()) {

            String sidTable = (String) entry.get("sidTable");
            if (sidTable != null)
                excludeTables.add(sidTable.trim());
        }
        return excludeTables;
    }

    @Override
    public ImMap<Property, Boolean> aspectChangeExtProps() {
        try {
            return getChangeProps((Property) findProperty("date[Backup]").property, (Property) findProperty("time[Backup]").property);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return null;
        }
    }
}
