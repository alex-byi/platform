package lsfusion.server.logics.property.actions.exporting.xml;

import lsfusion.base.IOUtils;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class XMLFormExporter extends HierarchicalFormExporter {
    private String charset = "utf-8";

    public XMLFormExporter(ReportGenerationData reportData) {
        super(reportData);
    }

    @Override
    public byte[] exportNodes(List<Node> rootNodes) throws IOException {
        File file = null;
        try {
            Element rootElement = new Element("export");
            for (Node rootNode : rootNodes)
                exportNode(rootElement, rootNode);

            file = File.createTempFile("exportForm", ".xml");
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(charset));
            try(PrintWriter fw = new PrintWriter(file)) {
                xmlOutput.output(new Document(rootElement), fw);
            }
            return IOUtils.getFileBytes(file);
        } finally {
            if (file != null && !file.delete())
                file.deleteOnExit();
        }
    }

    private void exportNode(Element parentElement, AbstractNode node) {
        if (node instanceof Leaf)
            parentElement.addContent(((Leaf) node).getValue());
        else if (node instanceof Node) {
            for (Map.Entry<String, List<AbstractNode>> child : ((Node) node).getChildren()) {
                Element element = new Element(child.getKey());
                for (AbstractNode childNode : child.getValue())
                    exportNode(element, childNode);
                parentElement.addContent(element);
            }
        }
    }
}