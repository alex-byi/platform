package lsfusion.server.physics.dev.id.name;

import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.link.*;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.classes.sets.ResolveConcatenateClassSet;
import lsfusion.server.logics.classes.sets.ResolveOrObjectClassSet;
import lsfusion.server.logics.classes.sets.ResolveUpClassSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassCanonicalNameUtils {
    public static final String ConcatenateClassNameLBracket = "(";
    public static final String ConcatenateClassNameRBracket = ")";
    public static final String ConcatenateClassNamePrefix = "CONCAT";
    
    public static final String OrObjectClassSetNameLBracket = "{";
    public static final String OrObjectClassSetNameRBracket = "}";
    
    public static final String UpClassSetNameLBracket = "(";
    public static final String UpClassSetNameRBracket = ")";
    
    // CONCAT(CN1, ..., CNk)
    public static String createName(ResolveConcatenateClassSet ccs) {
        ResolveClassSet[] classes = ccs.getClasses();
        String sid = ConcatenateClassNamePrefix + ConcatenateClassNameLBracket; 
        for (ResolveClassSet set : classes) {
            sid += (sid.length() > 1 ? "," : "");
            sid += set.getCanonicalName();
        }
        return sid + ConcatenateClassNameRBracket; 
    }
    
    // {UpCN, SetCN1, ..., SetCNk}
    public static String createName(ResolveOrObjectClassSet cs) {
        if (cs.set.size() == 0) {
            return cs.up.getCanonicalName();
        } else {
            String sid = OrObjectClassSetNameLBracket; 
            sid += cs.up.getCanonicalName();
            for (int i = 0; i < cs.set.size(); i++) {
                sid += ",";
                sid += cs.set.get(i).getCanonicalName();
            }
            return sid + OrObjectClassSetNameRBracket; 
        }
    }
    
    // (CN1, ..., CNk) 
    public static String createName(ResolveUpClassSet up) {
        if (up.wheres.length == 1) {
            return up.wheres[0].getCanonicalName();
        }
        String sid = UpClassSetNameLBracket;
        for (CustomClass cls : up.wheres) {
            sid += (sid.length() > 1 ? "," : "");
            sid += cls.getCanonicalName();
        }
        return sid + UpClassSetNameRBracket;
    }

    public static DataClass defaultStringClassObj = StringClass.text;
    public static DataClass defaultNumericClassObj = NumericClass.get(5, 2);
    
    public static DataClass getCanonicalNameDataClass(String name) {
        return canonicalDataClassNames.get(name); 
    }
    
    private static Map<String, DataClass> canonicalDataClassNames = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("DATE", DateClass.instance);
        put("DATETIME", DateTimeClass.instance );
        put("TIME", TimeClass.instance);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get());
        put("IMAGEFILE", ImageClass.get());
        put("PDFFILE", PDFClass.get());
        put("RAWFILE", CustomStaticFormatFileClass.get());
        put("FILE", DynamicFormatFileClass.get());
        put("EXCELFILE", ExcelClass.get());
        put("CSVFILE", CSVClass.get());
        put("HTMLFILE", HTMLClass.get());
        put("JSONFILE", JSONClass.get());
        put("XMLFILE", XMLClass.get());
        put("TABLEFILE", TableClass.get());
        put("WORDLINK", WordLinkClass.get(false));
        put("IMAGELINK", ImageLinkClass.get(false));
        put("PDFLINK", PDFLinkClass.get(false));
        put("RAWLINK", CustomStaticFormatLinkClass.get());
        put("LINK", DynamicFormatLinkClass.get(false));
        put("EXCELLINK", ExcelLinkClass.get(false));
        put("CSVLINK", CSVLinkClass.get(false));
        put("HTMLLINK", HTMLLinkClass.get(false));
        put("JSONLINK", JSONLinkClass.get(false));
        put("XMLLINK", XMLLinkClass.get(false));
        put("TABLELINK", TableLinkClass.get(false));
        put("COLOR", ColorClass.instance);
        put("STRING", defaultStringClassObj);
        put("NUMERIC", defaultNumericClassObj);
    }};

    public static DataClass getScriptedDataClass(String name) {
        assert !name.contains(" ");
        if (scriptedSimpleDataClassNames.containsKey(name)) {
            return scriptedSimpleDataClassNames.get(name);
        } else if (name.matches("^((STRING\\[\\d+\\])|(ISTRING\\[\\d+\\])|(VARSTRING\\[\\d+\\])|(VARISTRING\\[\\d+\\])|(NUMERIC\\[\\d+,\\d+\\]))$")) {
            if (name.startsWith("STRING[")) {
                name = name.substring("STRING[".length(), name.length() - 1);
                return StringClass.get(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("ISTRING[")) {
                name = name.substring("ISTRING[".length(), name.length() - 1);
                return StringClass.geti(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("VARSTRING[")) {
                name = name.substring("VARSTRING[".length(), name.length() - 1);
                return StringClass.getv(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("VARISTRING[")) {
                name = name.substring("VARISTRING[".length(), name.length() - 1);
                return StringClass.getvi(new ExtInt(Integer.parseInt(name)));
            } else if (name.startsWith("NUMERIC[")) {
                String length = name.substring("NUMERIC[".length(), name.indexOf(","));
                String precision = name.substring(name.indexOf(",") + 1, name.length() - 1);
                return NumericClass.get(Integer.parseInt(length), Integer.parseInt(precision));
            }            
        }
        return null;
    }
    
    private static Map<String, DataClass> scriptedSimpleDataClassNames = new HashMap<String, DataClass>() {{
        put("INTEGER", IntegerClass.instance);
        put("DOUBLE", DoubleClass.instance);
        put("LONG", LongClass.instance);
        put("BOOLEAN", LogicalClass.instance);
        put("DATE", DateClass.instance);
        put("DATETIME", DateTimeClass.instance);
        put("TIME", TimeClass.instance);
        put("YEAR", YearClass.instance);
        put("WORDFILE", WordClass.get());
        put("IMAGEFILE", ImageClass.get());
        put("PDFFILE", PDFClass.get());
        put("RAWFILE", CustomStaticFormatFileClass.get());
        put("FILE", DynamicFormatFileClass.get());
        put("EXCELFILE", ExcelClass.get());
        put("CSVFILE", CSVClass.get());
        put("HTMLFILE", HTMLClass.get());
        put("JSONFILE", JSONClass.get());
        put("XMLFILE", XMLClass.get());
        put("TABLEFILE", TableClass.get());
        put("WORDLINK", WordLinkClass.get(false));
        put("IMAGELINK", ImageLinkClass.get(false));
        put("PDFLINK", PDFLinkClass.get(false));
        put("RAWLINK", CustomStaticFormatLinkClass.get());
        put("LINK", DynamicFormatLinkClass.get(false));
        put("EXCELLINK", ExcelLinkClass.get(false));
        put("CSVLINK", CSVLinkClass.get(false));
        put("HTMLLINK", HTMLLinkClass.get(false));
        put("JSONLINK", JSONLinkClass.get(false));
        put("XMLLINK", XMLLinkClass.get(false));
        put("TABLELINK", TableLinkClass.get(false));
        put("COLOR", ColorClass.instance);
        put("TEXT", StringClass.text);
        put("RICHTEXT", StringClass.richText);
    }};

    public static List<ResolveClassSet> getResolveList(ValueClass[] classes) {
        List<ResolveClassSet> classSets;
        classSets = new ArrayList<>();
        for (ValueClass cls : classes) {
            classSets.add(cls.getResolveSet());
        }
        return classSets;
    }
}
