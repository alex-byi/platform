package lsfusion.server.logics.form.stat.integration.importing.plain.csv;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainIterator;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;

public class ImportCSVActionProperty extends ImportPlainActionProperty<ImportCSVIterator> {
    private boolean noHeader;
    private boolean noEscape;
    private String separator;
    
    public ImportCSVActionProperty(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean noHeader, boolean noEscape, String separator) {
        super(paramsCount, groupFiles, formEntity, charset != null ? charset : ExternalUtils.defaultCSVCharset);
        this.noHeader = noHeader;
        this.noEscape = noEscape;
        this.separator = separator == null ? ExternalUtils.defaultCSVSeparator : separator;
    }

    @Override
    public ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException {
        return new ImportCSVIterator(fieldTypes, file, charset, noHeader, noEscape, separator);
    }

    protected boolean indexBased() {
        return noHeader;
    }
}