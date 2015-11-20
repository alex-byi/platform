package lsfusion.interop.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PendingRemoteInterface extends Remote {
    public Object[] createAndExecute(MethodInvocation creator, MethodInvocation[] invocations) throws RemoteException;
    String getRemoteActionMessage() throws RemoteException;
    List<Object> getRemoteActionMessageList() throws RemoteException;
}
