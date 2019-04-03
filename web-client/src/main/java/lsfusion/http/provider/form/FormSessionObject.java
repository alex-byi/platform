package lsfusion.http.provider.form;

import lsfusion.client.form.ClientForm;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FormSessionObject<T> {
    public final ClientForm clientForm;
    public final RemoteFormInterface remoteForm;
    public final String sessionID;
    
    public final List<File> savedTempFiles;

    public int requestIndex = 0;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, String sessionID) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.sessionID = sessionID;
        
        savedTempFiles = clientForm != null ? new ArrayList<File>() : null;
    }
}
