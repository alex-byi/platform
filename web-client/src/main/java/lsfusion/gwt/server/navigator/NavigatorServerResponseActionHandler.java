package lsfusion.gwt.server.navigator;

import lsfusion.gwt.server.form.provider.FormSessionObject;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.navigator.NavigatorRequestAction;
import lsfusion.interop.form.ServerResponse;

import java.io.IOException;

public abstract class NavigatorServerResponseActionHandler<A extends NavigatorRequestAction> extends NavigatorActionHandler<A, ServerResponseResult> {

    protected NavigatorServerResponseActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(A action, ServerResponse serverResponse) throws IOException {
        return FormServerResponseActionHandler.getServerResponseResult(new FormSessionObject(null, null, action.sessionID), serverResponse, servlet);
    }
}
