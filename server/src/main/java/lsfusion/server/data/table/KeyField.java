package lsfusion.server.data.table;

import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.integral.IntegerClass;

import java.io.DataInputStream;
import java.io.IOException;

public class KeyField extends Field implements Comparable<KeyField> {
    public KeyField(String name, Type type) {super(name,type);}

    public static KeyField dumb = new KeyField("dumb", IntegerClass.instance) {
        @Override
        public String getDeclare(SQLSyntax syntax, TypeEnvironment typeEnv) {
            return super.getDeclare(syntax, typeEnv) + " default 0";
        }
    };
    public KeyField(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public byte getType() {
        return 0;
    }

    public int compareTo(KeyField o) {
        return name.compareTo(o.name); 
    }
}
