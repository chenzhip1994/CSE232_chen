package XQuery;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.LinkedList;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;


public class XQueryUtils {
    Document doc;

    public XQueryUtils() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw e;
        }
        this.doc = builder.newDocument();
    }


    public Node makeElement(String tagName, LinkedList<Node> l) {
        Element elem = doc.createElement(tagName);
        for (Node n : l) {
            Node c = doc.importNode(n, true);
            if (c.getNodeType() == Node.ATTRIBUTE_NODE) {
                elem.setAttributeNode((Attr) c);
            } else {
                elem.appendChild(c);
            }
        }
        return elem;
    }

    /**
     * convertNodeToHtml is to convert node to string representation
     * copyright:
     * https://stackoverflow.com/questions/4412848/xml-node-to-string-in-java
     * @param node
     * @return
     */

    public String convertNodeToHtml(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    //TODO: makeText
    public Node makeText(String stringConstant) {
        return doc.createTextNode(stringConstant);
    }

}
