package lsfusion.server.logics.property.env;

import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentFormFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentFormFormulaProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.form}"), SQLSession.formParam, paramClass.getUpSet());

        finalizeInit();
    }
}