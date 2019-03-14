package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.base.ThreadUtils;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.property.init.GroupPropertiesSingleTask;
import lsfusion.server.base.task.TaskRunner;

import java.sql.SQLException;
import java.util.Iterator;

public abstract class MultiThreadActionProperty extends ScriptingAction {
    private ClassPropertyInterface threadCountInterface;
    private ClassPropertyInterface propertyTimeoutInterface;

    public MultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
        propertyTimeoutInterface = i.next();
    }


    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        TaskRunner taskRunner = new TaskRunner(context.getBL());
        GroupPropertiesSingleTask task = createTask();
        task.setBL(context.getBL());
        boolean errorOccurred = false;
        try {
            ObjectValue threadCount = context.getKeyValue(threadCountInterface);
            ObjectValue propertyTimeout = context.getKeyValue(propertyTimeoutInterface);
            taskRunner.runTask(task, ServerLoggers.serviceLogger, threadCount == null ? null : (Integer) threadCount.getValue(),
                    propertyTimeout == null ? null : (Integer) propertyTimeout.getValue(), context);
        } catch (InterruptedException e) {
            errorOccurred = true;
            
            task.logTimeoutTasks();
            
            taskRunner.shutdownNow();            
            taskRunner.interruptThreadPoolProcesses(context);       
            
            ThreadUtils.interruptThread(context, Thread.currentThread());
        } finally {
            context.delayUserInterfaction(createMessageClientAction(task, errorOccurred));
        }
    }

    protected abstract GroupPropertiesSingleTask createTask();

    protected abstract String getCaptionError();

    protected abstract MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred);
}