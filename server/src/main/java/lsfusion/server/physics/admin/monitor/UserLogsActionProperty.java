package lsfusion.server.physics.admin.monitor;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.UserLogsClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UserLogsActionProperty extends ScriptingAction {

    public UserLogsActionProperty(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Map<String, RawFileData> logFiles = (Map<String, RawFileData>) context.requestUserInteraction(new UserLogsClientAction());
        if (logFiles != null && !logFiles.isEmpty()) {
            File zipFile = null;
            try {
                zipFile = makeZipFile(logFiles);
                try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                    ObjectValue currentConnection = findProperty("currentConnection[]").readClasses(newContext);
                    if (currentConnection instanceof DataObject) findProperty("fileUserLogs[Connection]").change(new FileData(new RawFileData(zipFile), "zip"), newContext, (DataObject) currentConnection);
                    newContext.apply();
                } catch (ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (zipFile != null && !zipFile.delete())
                    zipFile.deleteOnExit();
            }
        }
    }

    private File makeZipFile(Map<String, RawFileData> logFiles) throws IOException {
        File zipFile = File.createTempFile("zip", ".zip");
        FileOutputStream fos = new FileOutputStream(zipFile);
        try (ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Map.Entry<String, RawFileData> logFile : logFiles.entrySet()) {
                InputStream bis = logFile.getValue().getInputStream();
                zos.putNextEntry(new ZipEntry(logFile.getKey()));
                byte[] buf = new byte[1024];
                int len;
                while ((len = bis.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                bis.close();
            }
        }
        return zipFile;
    }
}