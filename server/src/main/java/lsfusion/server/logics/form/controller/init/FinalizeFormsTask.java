package lsfusion.server.logics.form.controller.init;

import lsfusion.server.logics.form.struct.FormEntity;

public class FinalizeFormsTask extends GroupFormsTask {

    protected boolean prerun() {
        getBL().markFormsForFinalization();
        getBL().getCheckConstrainedProperties(); // to avoid the concurrent calculation
        return true;
    }

    protected void runTask(FormEntity form) {
        form.finalizeAroundInit();
    }

    public String getCaption() {
        return "Finalizing forms";
    }
}
