package lsfusion.gwt.client.base.result;

import net.customware.gwt.dispatch.shared.Result;

public class BooleanResult implements Result {
    public Boolean value;

    @SuppressWarnings("UnusedDeclaration")
    public BooleanResult() {
    }

    public BooleanResult(Boolean value) {
        this.value = value;
    }
}
