CodeMirror.defineMode("fusion", function(config, parserConfig) {
    function words(str) {
        var obj = {}, words = str.split(" ");
        for (var i = 0; i < words.length; ++i) {
            obj[words[i]] = true;
        }
        return obj;
    }

    var types = words("INTEGER DOUBLE LONG BOOLEAN DATE DATETIME TEXT STRING ISTRING VARISTRING VARSTRING TIME");
    var keywords = words("ABSTRACT ACTION ACTIVE ACTIVATE ADDFORM AFTER " +
                           "AGGR ALL AND APPEND APPLY AS ASON ASSIGN ASYNCUPDATE ATTACH " +
                           "ATTR AUTO AUTOREFRESH AUTOSET BACKGROUND BCC BEFORE BODY BOTTOM BREAK BY CANCEL CANONICALNAME " +
                           "CASE CATCH CC CENTER CHANGE CHANGECLASS CHANGED CHANGEWYS CHARSET CHECK " +
                           "CHECKED CLASS CLIENT CLOSE COLOR COLUMNS COMPLEX CONCAT CONFIRM CONNECTION CONSTRAINT " +
                           "CONTAINERH CONTAINERV CONTEXTFILTER CSV CUSTOM CUSTOMLINK CUSTOMFILE CYCLES DATA DBF DEFAULT DEFAULTCOMPARE DELAY DELETE " +
                           "DESC DESIGN DIALOG DO DOC DOCKED DOCKEDMODAL DOCX DRAWROOT " +
                           "DROP DROPCHANGED DROPSET ECHO EDIT EDITABLE EDITFORM EDITKEY " +
                           "ELSE EMAIL END EQUAL EVAL EVENTID EVENTS EXCELFILE EXCELLINK " +
                           "EXCEPTLAST EXCLUSIVE EXEC EXPORT EXTEND EXTERNAL FALSE FILE FILTER FILTERGROUP " +
                           "FILTERS FINALLY FIRST FIXED FIXEDCHARWIDTH FOCUS FOLDER FOOTER FOR FORCE FOREGROUND " +
                           "FORM FORMS FORMULA FROM FULL FULLSCREEN GOAFTER GRID GROUP GROUPCHANGE HALIGN HEADER HEADERS " +
                           "HIDE HIDESCROLLBARS HIDETITLE HINTNOUPDATE HINTTABLE HORIZONTAL " + 
                           "HTML HTTP IF IMAGE IMAGEFILE IMAGELINK IMPORT IMPOSSIBLE IN INCREMENT INDEX " +
                           "INDEXED INIT INITFILTER INLINE INPUT IS JAVA JOIN JSON LAST LEFT LENGTH LIKE LIMIT " +
                           "LIST LOADFILE LOCAL LOGGABLE LSF MANAGESESSION MAX MAXCHARWIDTH MDB " +
                           "MEMO MESSAGE META MIN MINCHARWIDTH MODAL MODULE MOVE MS MULTI NAGGR NAME NAMESPACE " +
                           "NAVIGATOR NESTED NEW NEWEXECUTOR NEWSESSION NEWSQL NEWTHREAD NO NOCANCEL NOHEADER NOHINT NONULL NOT NOWAIT NULL NUMERIC OBJECT " +
                           "OBJECTS OK ON OPEN OPTIMISTICASYNC OR ORDER OVERRIDE PAGESIZE " +
                           "PANEL PARENT PARTITION PASSWORD PDF PDFFILE PDFLINK PERIOD MATERIALIZED PG POSITION " +
                           "PREFCHARWIDTH PREV PRINT PRIORITY PROPERTIES PROPERTY " + 
                           "PROPORTION QUERYOK QUERYCLOSE QUICKFILTER READ READONLY READONLYIF RECURSION REFLECTION REGEXP REMOVE " +
                           "REPORTFILES REQUEST REQUIRE RESOLVE RETURN RGB RICHTEXT RIGHT ROOT " +
                           "ROUND RTF SCHEDULE SCROLL SEEK SELECTOR SESSION SET SETCHANGED SHORTCUT SHOW SHOWDROP " +
                           "SHOWIF SINGLE SHEET SPLITH SPLITV SQL START STEP STRETCH STRICT STRUCT SUBJECT " +
                           "SUM TAB TABBED TABLE TEXTHALIGN TEXTVALIGN THEN THREADS TIME TO TODRAW " +
                           "TOOLBAR TOP TREE TRUE TRY UNGROUP UPDATE VALIGN VALUE " +
                           "VERTICAL VIEW WHEN WHERE WHILE WINDOW WORDFILE WORDLINK WRITE XLS XLSX XML XOR YES");

    var isOperatorChar = /[+\-*&%=<>!?|@#]/;

    return {
        token: function(stream, state) {
            if (stream.eatSpace()) {
                return null;
            }

            var ch = stream.next();
            if (ch == "'") {
                var escaped = false, next;
                while ((next = stream.next()) != null) {
                    if (next == ch && !escaped) {
                        return "string";
                    }
                    escaped = !escaped && next == "\\";
                }
                return "string";
            } else if (/[\[\]{}\(\),;\:\.]/.test(ch)) {
                return "bracket";
            } else if (/\d/.test(ch)) {
                stream.match(/^\d*(\.\d+)?/);
                return "number";
            } else if (ch == "/") {
                if (stream.eat("/")) {
                    stream.skipToEnd();
                    return "comment";
                }
                return "operator";
            } else if (ch == "$") {
                stream.eatWhile(/[\d]/);
                return "attribute";
            } else if (isOperatorChar.test(ch)) {
                stream.eatWhile(isOperatorChar);
                return "operator";
            } else {
                stream.eatWhile(/[\w]/);
                var word = stream.current();
                var istype = types.propertyIsEnumerable(word) && types[word];

                if (istype) {
                    return "type";
                }
                var iskeyword = keywords.propertyIsEnumerable(word) && keywords[word];
                return iskeyword ? "keyword" : "variable";
            }
        }
    };
});
