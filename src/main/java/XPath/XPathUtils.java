package XPath;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class XPathUtils {

    /*
    The following part is the XPath utils function
     */
    public static LinkedList<Node> LoadXMLFile(String fn) {
        LinkedList<Node> nodes = new LinkedList<>();
        try {
            // Remove quotes (first and last character)
            File xmlFile = new File(fn.substring(1, fn.length() - 1));
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            // Ignore non-relevant whitespace (only works if the XML has an associated DTD)
            docFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(xmlFile);
            // Normalize document
            doc.getDocumentElement().normalize();
            nodes.add(doc);
        } catch (Exception e) {
            return new LinkedList<>();
        }
        return nodes;
    }


    /**
     * get all the children of node n. Convert the NodeList to LinkedList of node;
     * @param
     * @return a List of the children
     */
    public static LinkedList<Node> getChildren(Node n){
        LinkedList<Node> children = new LinkedList<>();
        NodeList l = n.getChildNodes();
        for(int i=0;i<l.getLength();i++){
            children.add(l.item(i));
        }
        return children;
    }

    public static LinkedList<Node>  getTxt(Node n) {
        LinkedList<Node> res = new LinkedList<>();
        LinkedList<Node> children = getChildren(n);
        for (Node c : children) {
            if ((c.getNodeType() == Node.TEXT_NODE) && (c.getTextContent() != null) && (!c.getTextContent().isEmpty())) {
                res.add(c);
            }
        }
        return res;
    }


    public  static LinkedList<Node> getAttrib(Node n, String attName) {
        LinkedList<Node> nodes = new LinkedList<>();
        if (n.getNodeType() != Node.ELEMENT_NODE) {
            return nodes;
        }
        Element e = (Element) n;
        if (!e.hasAttribute(attName)) {
            return nodes;
        }
        nodes.add(e.getAttributeNode(attName));
        return nodes;
    }


    public static LinkedList<Node>  descendantsOrSelves(LinkedList<Node> nodes) {
        LinkedList<Node> res = new LinkedList<>();
        LinkedList<Node> q = new LinkedList<>();
        q.addAll(nodes);
        res.addAll(nodes);
        while(!q.isEmpty()){
            Node n = q.get(0);
            q.remove(0);
            LinkedList<Node> tempChildren = getChildren(n);
            q.addAll(tempChildren);
            res.addAll(tempChildren);
        }
        return res;
    }

    /**
     * Returns a list of nodes without duplicates
     *
     * @param nodes List of nodes with possible duplicates
     * @return List of nodes without duplicates
     */
    public static LinkedList<Node> unique(List<Node> nodes) {
        LinkedList<Node> uNodes = new LinkedList<>();
        for (Node n : nodes) {
            if (!uNodes.contains(n)) {
                uNodes.add(n);
            }
        }
        return uNodes;
    }


    /**
     * Returns the parent of a node
     *
     * @param n Node
     * @return Singleton list containing the parent of the element node, if it has a parent
     * - an empty list otherwise
     */
    public static LinkedList<Node> getParent(Node n) {
        LinkedList<Node> nodes = new LinkedList<>();
        // Attribute node's parent is accessed differently
        Node p = (n.getNodeType() == Node.ATTRIBUTE_NODE) ?
                ((Attr) n).getOwnerElement()
                : n.getParentNode();
        if (p != null) {
            nodes.add(p);
        }
        return nodes;
    }

    /**
     * Checks whether there exists one node on the first list that is equal to one node on the second list
     *
     * @param ls First list of nodes
     * @param rs Second list of nodes
     * @return ∃ l ∈ ls ∃ r ∈ rs / l eq r
     */
    public static boolean haveEqualNodes(LinkedList<Node> ls, LinkedList<Node> rs) {
        for (Node l : ls) {
            for (Node r : rs) {
                if (l.isEqualNode(r)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Checks whether there exists one node on the first list that is the same as one node on the second list
     * @param ls First list of nodes
     * @param rs Second list of nodes
     * @return ∃ l ∈ ls ∃ r ∈ rs / l is r
     */
    public static boolean haveSameNodes(LinkedList<Node> ls, LinkedList<Node> rs) {
        for (Node l : ls) {
            for (Node r : rs) {
                if (l.isSameNode(r)) {
                    return true;
                }
            }
        }
        return false;
    }
}
