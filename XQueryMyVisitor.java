import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class XQueryMyVisitor extends XQueryBaseVisitor<LinkedList<Node>>{
    // cur is the returned node for
    private LinkedList<Node> cur;

    private HashMap<String,LinkedList<Node>> vars;

    private XPathMyVisitor xPathMyVisitor;   // 创建XPathMyVisitor的实例，主要是为了调用里面的unique, getchildren 等方法

    /**
     * Document - Used to make element and text
     */
    private Document doc;

    public XQueryMyVisitor(){
        this.cur = new LinkedList<>();
        this.vars = new HashMap<>();
    }



    //TODO: makeELement
    public Node makeElement(String tagname,LinkedList<Node> l){
        Node elem = doc.createElement(tagname);
        for (Node n : l) {
            Node c = doc.importNode(n, true);
            elem.appendChild(c);
        }
        return elem;
    }
    //TODO: makeText
    public Node makeText(String stringConstant){
        return doc.createTextNode(stringConstant);
    }

    @Override
    public LinkedList<Node> visitXqVariable(XQueryParser.XqVariableContext ctx) {
        String name = ctx.Variable().getText();
        LinkedList<Node> res = new LinkedList<>();
        if(vars.containsKey(name)){
            res = vars.get(name);
            this.cur  = res;
        }
        return res;
    }



    @Override
    public LinkedList<Node> visitXqConstant(XQueryParser.XqConstantContext ctx) {
        String stringCons = ctx.StringConstant().getText();
        Node res = makeText(stringCons.substring(1,stringCons.length()-1));
        LinkedList<Node> ans = new LinkedList<>();
        ans.add(res);
        this.cur = ans;
        return this.cur;

    }

    @Override
    public LinkedList<Node> visitXqAp(XQueryParser.XqApContext ctx) {
        return visit(ctx.ap());
    }

    @Override
    public  LinkedList<Node> visitXqwithP(XQueryParser.XqwithPContext ctx) {
        return visit(ctx.xq());
    }

    @Override
    public  LinkedList<Node>  visitXqRp(XQueryParser.XqRpContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        visit(ctx.xq());
        res = visit(ctx.rp());
        this.cur = XPathMyVisitor.unique(res);        // unique 在 XPathMyVisitor 中是静态方法
        return this.cur;
    }

    @Override public LinkedList<Node> visitXqAll(XQueryParser.XqAllContext ctx){
        this.cur = xPathMyVisitor.descendantsOrSelves(visit(ctx.xq()));
        this.cur = XPathMyVisitor.unique(visit(ctx.rp()));
        return this.cur;
    }


    @Override
    public LinkedList<Node> visitXqTwoXq(XQueryParser.XqTwoXqContext ctx) {
        LinkedList<Node> copycur = new LinkedList<>(cur);
        LinkedList<Node> res1 = new LinkedList<>();
        LinkedList<Node> res2 = new LinkedList<>();
        res1 = visit(ctx.xq(0));
        this.cur  = copycur;
        res2 = visit(ctx.xq(1));
        LinkedList<Node> ans = new LinkedList<Node>(res1);
        ans.addAll(res2);
        this.cur = ans;
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitXqTag(XQueryParser.XqTagContext ctx) {
        String tagName = ctx.NAME(0).getText();
        Node res = makeElement(tagName, visit(ctx.xq()));
        LinkedList<Node> ans = new LinkedList<>();
        ans.add(res);
        this.cur = ans;
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitXqLet(XQueryParser.XqLetContext ctx) {
        HashMap<String,LinkedList<Node>> oldvars = new HashMap<>(this.vars);
        visit(ctx.letClause());
        visit(ctx.xq());
        this.vars = oldvars;
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitForClause(XQueryParser.ForClauseContext ctx) {
        return null;
    }

    @Override public LinkedList<Node> visitLetClause(XQueryParser.LetClauseContext ctx) {
        int num = ctx.Variable().size();
        for(int i=0;i<num;i++) {
            HashMap<String, LinkedList<Node>> oldvars = new HashMap<>(this.vars);
            LinkedList<Node> res = visit(ctx.xq(i));
            this.vars = oldvars;
            this.vars.put(ctx.Variable(i).getText(), res);
        }
        return null;
    }

    @Override
    public LinkedList<Node> visitWhereClause(XQueryParser.WhereClauseContext ctx) {
        visit(ctx.cond());
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitReturnClause(XQueryParser.ReturnClauseContext ctx) {
        visit(ctx.xq());
        return this.cur;
    }


}