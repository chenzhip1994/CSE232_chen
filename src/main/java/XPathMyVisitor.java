import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class XPathMyVisitor extends XPathBaseVisitor<LinkedList<Node>> {
    // cur is the returned node for
    private LinkedList<Node> cur;

    public XPathMyVisitor(){
        this.cur = new LinkedList<>();
    }


    public  LinkedList<Node> root(String fn) {
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
            return new LinkedList<Node>();
        }
        return nodes;
    }


    public LinkedList<Node> getChildren(Node n){
        LinkedList<Node> children = new LinkedList<>();
        NodeList l = n.getChildNodes();
        for(int i=0;i<l.getLength();i++){
            children.add(l.item(i));
        }
        return children;
    }

    public LinkedList<Node>  getTxt(Node n) {
        LinkedList<Node> res = new LinkedList<>();
        LinkedList<Node> children = getChildren(n);
        for (Node c : children) {
            if ((c.getNodeType() == Node.TEXT_NODE) && (c.getTextContent() != null) && (!c.getTextContent().isEmpty())) {
                res.add(c);
            }
        }
        return res;
    }

    public LinkedList<Node>  descendantsOrSelves(LinkedList<Node> nodes) {
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

    @Override
    public LinkedList<Node> visitApDoc(XPathParser.ApDocContext ctx) {
        LinkedList<Node> res = root(ctx.fname().getText());
        cur = res;
        return res;

    }


    @Override
    public LinkedList<Node> visitCurrent(XPathParser.CurrentContext ctx) {
        return cur;
    }

    @Override
    public LinkedList<Node> visitAllChildren(XPathParser.AllChildrenContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for(int i=0;i<cur.size();i++){
            ans.addAll(getChildren(cur.get(i)));
        }
        cur = ans;
        return ans;
    }

    @Override
    public LinkedList<Node>  visitRpwithP(XPathParser.RpwithPContext ctx) {
        return visit(ctx.rp());
    }

    @Override
    public LinkedList<Node>  visitRpChildren(XPathParser.RpChildrenContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        visit(ctx.rp(0));
        res = visit(ctx.rp(1));
        cur = res;
        return res;
    }
    @Override
    public LinkedList<Node> visitParent(XPathParser.ParentContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for(int i=0;i<cur.size();i++){
            ans.add(cur.get(i).getParentNode());
        }
        cur = ans;
        return ans;
    }
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


    //TODO modify this method
    @Override
    public LinkedList<Node> visitTxt(XPathParser.TxtContext ctx) {
        LinkedList<Node> res = new LinkedList<Node>();
        for(Node n:cur){
            res.addAll(getTxt(n));
        }
        cur = res;
        return res;
    }

    @Override
    public LinkedList<Node> visitTagName(XPathParser.TagNameContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        String leafName = ctx.getText();
        for(Node n:cur){
            LinkedList<Node> c = getChildren(n);
            for(Node nn:c){
                if((nn.getNodeName()).equals(leafName))
                    res.add(nn);
            }
        }
        cur = res;
        return res;
    }

    public  LinkedList<Node> getAttrib(Node n, String attName) {
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

    @Override
    public  LinkedList<Node> visitRpAll(XPathParser.RpAllContext ctx) {
        visit(ctx.rp(0));
        LinkedList<Node> temp = descendantsOrSelves(cur);
        cur = temp;
        LinkedList<Node> ans = visit(ctx.rp(1));
        cur = ans;
        return ans;
    }
    @Override
    public LinkedList<Node>  visitRpFilter(XPathParser.RpFilterContext ctx) {
        LinkedList<Node> res = visit(ctx.rp());
        LinkedList<Node> ans = new LinkedList<Node>();
        for(Node n:res){
            LinkedList<Node> temp = new LinkedList<Node>();
            temp.add(n);
            cur = temp;
            if( !visit(ctx.filter()).isEmpty() )
                ans.add(n);
        }
        cur = ans;
        return ans;
    }



}