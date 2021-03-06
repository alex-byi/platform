package lsfusion.http.provider.session;

import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface SessionProvider {

    SessionSessionObject createSession(RemoteLogicsInterface remoteLogics, HttpServletRequest request, String sessionID) throws RemoteException;
    SessionSessionObject getSessionSessionObject(String sessionID) throws SessionInvalidatedException;
    void removeSessionSessionObject(String sessionID) throws RemoteException;

}
