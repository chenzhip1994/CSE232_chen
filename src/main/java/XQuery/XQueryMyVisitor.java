package XQuery;

import java.util.*;

import Antlr.XQueryBaseVisitor;
import Antlr.XQueryParser;
import org.w3c.dom.*;
import XPath.*;

import javax.xml.parsers.ParserConfigurationException;


public class XQueryMyVisitor extends XQueryBaseVisitor<LinkedList<Node>> {
    // cur is the returned node for
    private LinkedList<Node> cur;

    private HashMap<String, LinkedList<Node>> vars;

    private XQueryUtils xUtils;

    public XQueryMyVisitor() throws ParserConfigurationException {
        this.cur = new LinkedList<>();
        this.vars = new HashMap<>();
        this.xUtils = new XQueryUtils();
    }

    @Override
    public LinkedList<Node> visitXqVariable(XQueryParser.XqVariableContext ctx) {
        String name = ctx.Variable().getText();
        LinkedList<Node> res = new LinkedList<>();
        if (vars.containsKey(name)) {
            res = vars.get(name);
            this.cur = res;
        }
        return res;
    }

    @Override
    public LinkedList<Node> visitXqConstant(XQueryParser.XqConstantContext ctx) {
        String stringCons = ctx.StringConstant().getText();
        Node res = xUtils.makeText(stringCons.substring(1, stringCons.length() - 1));
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
    public LinkedList<Node> visitXqwithP(XQueryParser.XqwithPContext ctx) {
        return visit(ctx.xq());
    }

    @Override
    public LinkedList<Node> visitXqRp(XQueryParser.XqRpContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        visit(ctx.xq());
        res = visit(ctx.rp());
        this.cur = XPathUtils.unique(res);        // unique 在 XPathMyVisitor 中是静态方法
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitXqAll(XQueryParser.XqAllContext ctx) {
        LinkedList<Node> tempRes = visit(ctx.xq());
        this.cur = XPathUtils.descendantsOrSelves(tempRes);
        this.cur = XPathUtils.unique(visit(ctx.rp()));
        return this.cur;
    }


    @Override
    public LinkedList<Node> visitXqTwoXq(XQueryParser.XqTwoXqContext ctx) {
        LinkedList<Node> copycur = new LinkedList<>(cur);
        LinkedList<Node> res1 = new LinkedList<>();
        LinkedList<Node> res2 = new LinkedList<>();
        res1 = visit(ctx.xq(0));
        this.cur = copycur;
        res2 = visit(ctx.xq(1));
        LinkedList<Node> ans = new LinkedList<Node>(res1);
        ans.addAll(res2);
        this.cur = ans;
        return this.cur;
    }

    @Override
    public LinkedList<Node> visitXqTag(XQueryParser.XqTagContext ctx) {
        String tagName = ctx.NAME(0).getText();
        Node res = xUtils.makeElement(tagName, visit(ctx.xq()));
        LinkedList<Node> ans = new LinkedList<>();
        ans.add(res);
        this.cur = ans;
        return this.cur;
    }

    /**
     * Let just change the variables, and does not change the context
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitXqLet(XQueryParser.XqLetContext ctx) {
        HashMap<String, LinkedList<Node>> oldvars = new HashMap<>(this.vars);
        visit(ctx.letClause());
        visit(ctx.xq());
        this.vars = oldvars;
        return this.cur;
    }

    /**
     * FLWR, ilterate over all the let clause, and all values in each let clause
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitXqFLWR(XQueryParser.XqFLWRContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        HashMap<String, LinkedList<Node>> oldVars = new HashMap<>(this.vars);
        doFLWRIterate(ctx, 0, res);
        this.cur = res;
        this.vars = oldVars;
        return this.cur;
    }

    /**
     * doFLER iteration, the result is in the parameter nodes
     *
     * @param ctx   Current tree context
     * @param depth iteration number (how many variables in for clause has been handled)
     * @param nodes result returned by the return clause
     */
    public void doFLWRIterate(XQueryParser.XqFLWRContext ctx, int depth, LinkedList<Node> nodes) {
        if (depth < ctx.forClause().Variable().size()) {
            String varName = ctx.forClause().Variable(depth).getText();
            LinkedList<Node> xqres = visit(ctx.forClause().xq(depth));
            for (Node n : xqres) {
                this.vars.remove(varName);
                LinkedList<Node> temp = new LinkedList<>();
                temp.add(n);
                this.vars.put(varName, temp);
                //递归调用doFLWR处理嵌套for
                doFLWRIterate(ctx, depth + 1, nodes);
            }
        } else {
//            iterate ends, begin to process let, where and return
            HashMap<String, LinkedList<Node>> oldVars = new HashMap<>(this.vars);
            if (ctx.letClause() != null) {
                visit(ctx.letClause());
            }
            if (ctx.whereClause() == null || visit(ctx.whereClause()).size() > 0) {
                nodes.addAll(visit(ctx.returnClause()));
            }
            this.vars = oldVars;
        }
    }

    /**
     * This function should never be called
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitForClause(XQueryParser.ForClauseContext ctx) {
        return null;
    }

    /**
     * This function should only be called by FLWR.
     * Let clause does not change the context
     *
     * @param ctx
     * @return TODO: should we save the context for let clause?
     */
    @Override
    public LinkedList<Node> visitLetClause(XQueryParser.LetClauseContext ctx) {

        int num = ctx.Variable().size();
        for (int i = 0; i < num; i++) {
            HashMap<String, LinkedList<Node>> oldvars = new HashMap<>(this.vars);
            LinkedList<Node> res = visit(ctx.xq(i));
            this.vars = oldvars;
            this.vars.put(ctx.Variable(i).getText(), res);
        }
        return null;
    }

    @Override
    public LinkedList<Node> visitWhereClause(XQueryParser.WhereClauseContext ctx) {
        LinkedList<Node> res =  visit(ctx.cond());
        return res;
    }

    @Override
    public LinkedList<Node> visitReturnClause(XQueryParser.ReturnClauseContext ctx) {
        LinkedList<Node> res = visit(ctx.xq());
        return res;
    }

    /**
     * : xq ('=' | 'eq') xq    #CondValueEqual
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondValueEqual(XQueryParser.CondValueEqualContext ctx) {
        LinkedList<Node> curCopy = new LinkedList<>(this.cur);
        LinkedList<Node> left = visit(ctx.xq(0));
        this.cur = curCopy;
        LinkedList<Node> right = visit(ctx.xq(1));
        this.cur = curCopy;
        if (XPathUtils.haveEqualNodes(left, right)) {
            return this.cur;
        } else {
            return new LinkedList<>();
        }
    }

    /**
     * | xq ('==' | 'is') xq   #CondIdentityEqual
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondIdentityEqual(XQueryParser.CondIdentityEqualContext ctx) {
        LinkedList<Node> curCopy = new LinkedList<>(this.cur);
        LinkedList<Node> left = visit(ctx.xq(0));
        this.cur = curCopy;
        LinkedList<Node> right = visit(ctx.xq(1));
        this.cur = curCopy;
        if (XPathUtils.haveSameNodes(left, right)) {
            return this.cur;
        } else {
            return new LinkedList<>();
        }
    }

    /**
     * 'empty' '(' xq ')'   # CondEmpty
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondEmpty(XQueryParser.CondEmptyContext ctx) {
        LinkedList<Node> curCopy = new LinkedList<>(this.cur);
        LinkedList<Node> targNodes = visit(ctx.xq());
        this.cur = curCopy;
        if (targNodes.isEmpty()) {
            return this.cur;
        } else {
            return new LinkedList<>();
        }
    }

    /**
     * 'some' Variable 'in' xq (',' Variable 'in' xq)* 'satisfies' cond         # CondSome
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondSome(XQueryParser.CondSomeContext ctx) {
        LinkedList<Node> curCopy = new LinkedList<>(this.cur);
        HashMap<String, LinkedList<Node>> varsCopy = new HashMap<>(this.vars);
        int count = ctx.Variable().size();
        for (int i = 0; i < count; ++i) {
            String varName = ctx.Variable(i).getText();
            LinkedList<Node> xqRes = visit(ctx.xq(i));
            this.vars.put(varName, xqRes);
        }
        LinkedList<Node> condRes = visit(ctx.cond());
        this.vars = varsCopy;
        this.cur = curCopy;
        return condRes;
    }

    /**
     * '(' cond ')'   #CondWithP
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondWithP(XQueryParser.CondWithPContext ctx) {
        return visit(ctx.cond());
    }

    /**
     * cond 'and' cond     #CondAnd
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondAnd(XQueryParser.CondAndContext ctx) {
        if (visit(ctx.cond(0)).isEmpty() || visit(ctx.cond(1)).isEmpty()) {
            return new LinkedList<>();
        } else {
            return this.cur;
        }
    }

    /**
     * cond 'or' cond    # CondOr
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondOr(XQueryParser.CondOrContext ctx) {
        if (visit(ctx.cond(0)).isEmpty() && visit(ctx.cond(1)).isEmpty()) {
            return new LinkedList<>();
        } else {
            return this.cur;
        }
    }

    /**
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCondNot(XQueryParser.CondNotContext ctx) {
        if (visit(ctx.cond()).isEmpty()) {
            return this.cur;
        } else {
            return new LinkedList<>();
        }
    }


//
//    @Override
//    public LinkedList<Node> visitApChildren(XQueryParser.ApChildrenContext ctx) {
//        return super.visitApChildren(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitApAll(XQueryParser.ApAllContext ctx) {
//        return super.visitApAll(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitApDoc(XQueryParser.ApDocContext ctx) {
//        return super.visitApDoc(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitAllChildren(XQueryParser.AllChildrenContext ctx) {
//        return super.visitAllChildren(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitRpwithP(XQueryParser.RpwithPContext ctx) {
//        return super.visitRpwithP(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitTxt(XQueryParser.TxtContext ctx) {
//        return super.visitTxt(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitTagName(XQueryParser.TagNameContext ctx) {
//        return super.visitTagName(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitRpAll(XQueryParser.RpAllContext ctx) {
//        return super.visitRpAll(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitParent(XQueryParser.ParentContext ctx) {
//        return super.visitParent(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitAttribute(XQueryParser.AttributeContext ctx) {
//        return super.visitAttribute(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitRpChildren(XQueryParser.RpChildrenContext ctx) {
//        return super.visitRpChildren(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitCurrent(XQueryParser.CurrentContext ctx) {
//        return super.visitCurrent(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitTwoRp(XQueryParser.TwoRpContext ctx) {
//        return super.visitTwoRp(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
//        return super.visitRpFilter(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltEqual(XQueryParser.FltEqualContext ctx) {
//        return super.visitFltEqual(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltRp(XQueryParser.FltRpContext ctx) {
//        return super.visitFltRp(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltAnd(XQueryParser.FltAndContext ctx) {
//        return super.visitFltAnd(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltIs(XQueryParser.FltIsContext ctx) {
//        return super.visitFltIs(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltOr(XQueryParser.FltOrContext ctx) {
//        return super.visitFltOr(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltNot(XQueryParser.FltNotContext ctx) {
//        return super.visitFltNot(ctx);
//    }
//
//    @Override
//    public LinkedList<Node> visitFltwithP(XQueryParser.FltwithPContext ctx) {
//        return super.visitFltwithP(ctx);
//    }




/*
The following part is the xpath function
 */

    /**
     * doc '/' rp
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public LinkedList<Node> visitApChildren(XQueryParser.ApChildrenContext ctx) {
        visit(ctx.doc());
        this.cur = XPathUtils.unique(visit(ctx.rp()));
        return this.cur;

    }

    /**
     * doc '//' rp                 # ApAll
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public LinkedList<Node> visitApAll(XQueryParser.ApAllContext ctx) {
        cur = visit(ctx.doc());
        cur = XPathUtils.descendantsOrSelves(cur);
        cur = XPathUtils.unique(visit(ctx.rp()));
        return cur;
    }


    /**
     * 'doc' '(' FPath ')'     #ApDoc
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitApDoc(XQueryParser.ApDocContext ctx) {
        LinkedList<Node> res = XPathUtils.LoadXMLFile(ctx.StringConstant().getText());
        cur = res;
        return res;

    }

    /**
     * NAME                          # TagName
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitTagName(XQueryParser.TagNameContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        String leafName = ctx.getText();
        for (Node n : this.cur) {
            LinkedList<Node> c = XPathUtils.getChildren(n);
            for (Node nn : c) {
                if ((nn.getNodeName()).equals(leafName))
                    res.add(nn);
            }
        }
        this.cur = res;
        return res;
    }

    /**
     * '.'                          # Current
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitCurrent(XQueryParser.CurrentContext ctx) {
        return this.cur;
    }

    /**
     * '..'                         # Parent
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitParent(XQueryParser.ParentContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for (Node n : this.cur) {
            ans.addAll(XPathUtils.getParent(n));
        }
        cur = ans;
        return ans;
    }

    /**
     * '*'                          # AllChildren
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitAllChildren(XQueryParser.AllChildrenContext ctx) {
        LinkedList<Node> ans = new LinkedList<>();
        for (Node n : this.cur) {
            ans.addAll(XPathUtils.getChildren(n));
        }
        this.cur = ans;
        return ans;
    }

    /**
     * 'text()'                     # Txt
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitTxt(XQueryParser.TxtContext ctx) {
        LinkedList<Node> res = new LinkedList<Node>();
        for (Node n : cur) {
            res.addAll(XPathUtils.getTxt(n));
        }
        cur = res;
        return res;
    }

    /**
     * '@' NAME                     # Attribute
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitAttribute(XQueryParser.AttributeContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        //get attribute name
        String leafName = ctx.getText();
        for (Node n : cur) {
            res.addAll(XPathUtils.getAttrib(n, leafName));
        }
        cur = res;
        return res;
    }

    /**
     * '(' rp ')'                   # RpwithP
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitRpwithP(XQueryParser.RpwithPContext ctx) {
        return visit(ctx.rp());
    }

    /**
     * rp '/' rp                    # RpChildren
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitRpChildren(XQueryParser.RpChildrenContext ctx) {
        LinkedList<Node> res = new LinkedList<>();
        visit(ctx.rp(0));
        res = visit(ctx.rp(1));
        this.cur = XPathUtils.unique(res);
        return this.cur;
    }

    /**
     * rp '//' rp                   # RpAll
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitRpAll(XQueryParser.RpAllContext ctx) {
        visit(ctx.rp(0));
        LinkedList<Node> temp = XPathUtils.descendantsOrSelves(cur);
        cur = temp;
        LinkedList<Node> ans = visit(ctx.rp(1));
        cur = XPathUtils.unique(ans);
        return ans;
    }

    /**
     * rp '[' filter ']'            # RpFilter
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) {
        LinkedList<Node> preNodes = visit(ctx.rp());
        LinkedList<Node> ans = new LinkedList<Node>();
        for (Node n : preNodes) {
            LinkedList<Node> tempNodes = new LinkedList<Node>();
            tempNodes.add(n);
            this.cur = tempNodes;
            if (!visit(ctx.filter()).isEmpty())
                ans.add(n);
        }
        this.cur = ans;
        return ans;
    }

    /**
     * rp ',' rp                    # TwoRp
     *
     * @param ctx
     * @return
     */
    @Override
    public LinkedList<Node> visitTwoRp(XQueryParser.TwoRpContext ctx) {
        LinkedList<Node> copycur = new LinkedList<>(cur);
        LinkedList<Node> res1 = new LinkedList<>();
        LinkedList<Node> res2 = new LinkedList<>();
        res1 = visit(ctx.rp(0));
        cur = copycur;
        res2 = visit(ctx.rp(1));
        LinkedList<Node> ans = new LinkedList<Node>(res1);
        ans.addAll(res2);
        cur = ans;
        return ans;
    }


    /**
     * rp                           # FltRp
     * Note: filter functions should not change the current list of nodes
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltRp(XQueryParser.FltRpContext ctx) {
        LinkedList<Node> curCopy = this.cur;
        LinkedList<Node> filterNodes = visit(ctx.rp());
        this.cur = curCopy;
        return filterNodes;
    }

    /**
     * rp '=' rp                    # FltEqual
     * rp 'eq' rp                   # FltEqual
     * Note: filter functions should not change the current list of nodes
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltEqual(XQueryParser.FltEqualContext ctx) {
        LinkedList<Node> nodes = this.cur;
        LinkedList<Node> l = visit(ctx.rp(0));
        this.cur = nodes;
        LinkedList<Node> r = visit(ctx.rp(1));
        this.cur = nodes;
        if (XPathUtils.haveEqualNodes(l, r)) {
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
    public LinkedList<Node> visitFltIs(XQueryParser.FltIsContext ctx) {
        LinkedList<Node> curCopy = this.cur;
        LinkedList<Node> l = visit(ctx.rp(0));
        this.cur = curCopy;
        LinkedList<Node> r = visit(ctx.rp(1));
        this.cur = curCopy;
        if (XPathUtils.haveSameNodes(l, r)) {
            return this.cur;
        }
        return new LinkedList<>();
    }

    /**
     * '(' filter ')'               # FltwithP
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltwithP(XQueryParser.FltwithPContext ctx) {
        return visit(ctx.filter());
    }

    /**
     * filter 'and' filter          # FltAnd
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltAnd(XQueryParser.FltAndContext ctx) {
        if ((visit(ctx.filter(0)).isEmpty()) || (visit(ctx.filter(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.cur;
    }

    /**
     * filter 'or' filter           # FltOr
     * Note: filter functions should not change the current list of nodes
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltOr(XQueryParser.FltOrContext ctx) {
        if ((visit(ctx.filter(0)).isEmpty()) && (visit(ctx.filter(1)).isEmpty())) {
            return new LinkedList<>();
        }
        return this.cur;
    }

    /**
     * 'not' filter                 # FltNot
     * Note: filter functions should not change the current list of nodes
     *
     * @param
     * @return
     */
    @Override
    public LinkedList<Node> visitFltNot(XQueryParser.FltNotContext ctx) {
        if (visit(ctx.filter()).isEmpty()) {
            return this.cur;
        }
        return new LinkedList<>();
    }


}