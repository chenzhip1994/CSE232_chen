package XPath;

import java.io.File;
import java.util.*;
import javax.xml.parsers.*;

import Antlr.XPathBaseVisitor;
import Antlr.XPathParser;
import org.w3c.dom.*;


public class XPathMyVisitor extends XPathBaseVisitor<LinkedList<Node>> {
    // cur is the returned node for
    private LinkedList<Node> cur;

    public XPathMyVisitor(){
        this.cur = new LinkedList<>();
    }


    public static  LinkedList<Node> LoadXMLFile(String fn) {
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

/**
 * The following part is the visitor function
 */
    /**
     * doc '/' rp
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public LinkedList<Node> visitApChildren(XPathParser.ApChildrenContext ctx) {
        visit(ctx.doc());
        this.cur = unique(visit(ctx.rp()));
        return this.cur;

    }
    /**
     * doc '//' rp                 # ApAll
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public LinkedList<Node> visitApAll(XPathParser.ApAllContext ctx) {
        cur = visit(ctx.doc());
        cur = descendantsOrSelves(cur);
        cur = unique(visit(ctx.rp()));
        return cur;
    }


    /**
     * 'doc' '(' FPath ')'     #ApDoc
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitApDoc(XPathParser.ApDocContext ctx) {
        LinkedList<Node> res = LoadXMLFile(ctx.FPath().getText());
        cur = res;
        return res;

    }

    /**
     * NAME                          # TagName
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitTagName(XPathParser.TagNameContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        String leafName = ctx.getText();
        for(Node n: this.cur){
            LinkedList<Node> c = getChildren(n);
            for(Node nn:c){
                if((nn.getNodeName()).equals(leafName))
                    res.add(nn);
            }
        }
        this.cur = res;
        return res;
    }

    /**
     * '.'                          # Current
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCurrent(XPathParser.CurrentContext ctx) {
        return this.cur;
    }

    /**
     * '..'                         # Parent
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitParent(XPathParser.ParentContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for(Node n : this.cur){
            ans.addAll(getParent(n));
        }
        cur = ans;
        return ans;
    }

    /**
     * '*'                          # AllChildren
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitAllChildren(XPathParser.AllChildrenContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for(Node n : this.cur){
            ans.addAll(getChildren(n));
        }
        this.cur = ans;
        return ans;
    }

    /**
     *  'text()'                     # Txt
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitTxt(XPathParser.TxtContext ctx) {
        LinkedList<Node> res = new LinkedList<Node>();
        for(Node n:cur){
            res.addAll(getTxt(n));
        }
        cur = res;
        return res;
    }

    /**
     *  '@' NAME                     # Attribute
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitAttribute(XPathParser.AttributeContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        //get attribute name
        String leafName = ctx.getText();
        for(Node n:cur){
            res.addAll(getAttrib(n,leafName));
        }
        cur = res;
        return res;
    }

    /**
     * '(' rp ')'                   # RpwithP
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node>  visitRpwithP(XPathParser.RpwithPContext ctx) {
        return visit(ctx.rp());
    }

    /**
     * rp '/' rp                    # RpChildren
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node>  visitRpChildren(XPathParser.RpChildrenContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        visit(ctx.rp(0));
        res = visit(ctx.rp(1));
        this.cur = this.unique(res);
        return this.cur;
    }

    /**
     * rp '//' rp                   # RpAll
     * @param ctx
     * @return
     */
    @Override
    public  LinkedList<Node> visitRpAll(XPathParser.RpAllContext ctx) {
        visit(ctx.rp(0));
        LinkedList<Node> temp = descendantsOrSelves(cur);
        cur = temp;
        LinkedList<Node> ans = visit(ctx.rp(1));
        cur = this.unique(ans);
        return ans;
    }

    /**
     * rp '[' filter ']'            # RpFilter
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node>  visitRpFilter(XPathParser.RpFilterContext ctx) {
        LinkedList<Node> preNodes = visit(ctx.rp());
        LinkedList<Node> ans = new LinkedList<Node>();
        for(Node n: preNodes){
            LinkedList<Node> tempNodes = new LinkedList<Node>();
            tempNodes.add(n);
            this.cur = tempNodes;
            if( !visit(ctx.filter()).isEmpty() )
                ans.add(n);
        }
        this.cur = ans;
        return ans;
    }

    /**
     * rp ',' rp                    # TwoRp
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node>  visitTwoRp(XPathParser.TwoRpContext ctx) {
        LinkedList<Node> copycur = new LinkedList<>(cur);
        LinkedList<Node> res1 = new LinkedList<>();
        LinkedList<Node> res2 = new LinkedList<>();
        res1 = visit(ctx.rp(0));
        cur  = copycur;
        res2 = visit(ctx.rp(1));
        LinkedList<Node> ans = new LinkedList<Node>(res1);
        ans.addAll(res2);
        cur = ans;
        return ans;
    }



    /**
     * rp                           # FltRp
     * Note: filter functions should not change the current list of nodes
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltRp(XPathParser.FltRpContext ctx) {
        LinkedList<Node> curCopy = this.cur;
        LinkedList<Node> filterNodes = visit(ctx.rp());
        this.cur = curCopy;
        return filterNodes;
    }

    /**
     * rp '=' rp                    # FltEqual
     * rp 'eq' rp                   # FltEqual
     * Note: filter functions should not change the current list of nodes
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltEqual(XPathParser.FltEqualContext ctx) {
        LinkedList<Node> nodes = this.cur;
        LinkedList<Node> l = visit(ctx.rp(0));
        this.cur = nodes;
        LinkedList<Node> r = visit(ctx.rp(1));
        this.cur = nodes;
        if (haveEqualNodes(l, r)) {
            return this.cur;
        }
        return new LinkedList<>();
    }

    /**
     * rp '==' rp                   # FltIs
     * rp 'is' rp                   # FltIsfunctions should not change the current list of nodes
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltIs(XPathParser.FltIsContext ctx) {
        LinkedList<Node> curCopy = this.cur;
        LinkedList<Node> l = visit(ctx.rp(0));
        this.cur = curCopy;
        LinkedList<Node> r = visit(ctx.rp(1));
        this.cur = curCopy;
        if (haveSameNodes(l, r)) {
            return this.cur;
        }
        return new LinkedList<>();
    }

    /**
     *
     * '(' filter ')'               # FltwithP
     * @param
     * @return
     */
    @Override
    public LinkedList<Node>  visitFltwithP(XPathParser.FltwithPContext ctx){
        return visit(ctx.filter());
    }

    /**
     * filter 'and' filter          # FltAnd
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltAnd(XPathParser.FltAndContext ctx) {
        if ((visit(ctx.filter(0)).isEmpty()) || (visit(ctx.filter(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.cur;
    }

    /**
     * filter 'or' filter           # FltOr
     * Note: filter functions should not change the current list of nodes
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltOr(XPathParser.FltOrContext ctx) {
        if ((visit(ctx.filter(0)).isEmpty()) && (visit(ctx.filter(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.cur;
    }

    /**
     * 'not' filter                 # FltNot
     * Note: filter functions should not change the current list of nodes
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltNot(XPathParser.FltNotContext ctx) {
        if (visit(ctx.filter()).isEmpty()) {
            return this.cur;
        }
        return new LinkedList<>();
    }

}