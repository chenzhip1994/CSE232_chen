package XQuery;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.LinkedList;


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

    //TODO: makeText
    public Node makeText(String stringConstant) {
        return doc.createTextNode(stringConstant);
    }

}
