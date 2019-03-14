package XQueryRewriter;

import Antlr.XQueryOptBaseVisitor;
import Antlr.XQueryOptParser;

/**
 * Base string visitor, convert the xquery to string
 */


/**
 * xq
 *     : 'for' Variable 'in' path (',' Variable 'in' path)* 'where' cond 'return' returnClause   #XqFWR
 *     ;
 */
public class XQueryStringVisitor extends XQueryOptBaseVisitor<String> {
    @Override
    public String visitXqFWR(XQueryOptParser.XqFWRContext ctx) {
        StringBuilder resultStr = new StringBuilder();
        resultStr.append(" for ");
        for(int i=0;i<ctx.Variable().size();++i){
            resultStr.append(ctx.Variable(i).getText());
            resultStr.append(" in ");
            resultStr.append(visit(ctx.path(i)));
            if (i != (ctx.Variable().size() - 1)) {
                resultStr.append(" , ");
            }
        }
        if(ctx.cond()!= null){
            String condStr = visit(ctx.cond());
            resultStr.append(" where ");
            resultStr.append(condStr);
        }

        String returnClauseStr = visit(ctx.returnClause());
        resultStr.append(" return ");
        resultStr.append(returnClauseStr);
        return resultStr.toString();
    }

    @Override
    public String visitPath(XQueryOptParser.PathContext ctx) {
        StringBuilder pathStr = new StringBuilder();
        if(ctx.StringConstant()!=null){
            pathStr.append(" doc(" + ctx.StringConstant().getText() + ") ");
        }else{
            pathStr.append(ctx.Variable().getText());
        }
        for(int i=0;i<ctx.NAME().size();++i){
            pathStr.append(ctx.sep(i).getText());
            pathStr.append(ctx.NAME(i).getText());
        }
        if(ctx.sep().size() > ctx.NAME().size()){
            pathStr.append(ctx.sep(ctx.sep().size()-1).getText() + "text()");
        }
        return pathStr.toString();
    }

    @Override
    public String visitSep(XQueryOptParser.SepContext ctx) {
        return super.visitSep(ctx);
    }

    /**
     *     : Variable   #XqreturnVar
     * @param ctx
     * @return
     */
    @Override
    public String visitXqreturnVar(XQueryOptParser.XqreturnVarContext ctx) {
        String res = ctx.Variable().getText();
        return res;
    }

    /**
     * returnClause  ',' returnClause         #XqTworeturn
     * @param ctx
     * @return
     */
    @Override
    public String visitXqTworeturn(XQueryOptParser.XqTworeturnContext ctx) {
        String str1 = visit(ctx.returnClause(0));
        String str2 = visit(ctx.returnClause(1));
        String res = str1+" , "+str2;
        return res;
    }


    /**
     *     | path         #Xqreturnpath
     * @param ctx
     * @return
     */
    @Override
    public String visitXqreturnpath(XQueryOptParser.XqreturnpathContext ctx) {
        String pathStr = visit(ctx.path());
        return pathStr;
    }

    /**
     * '<' NAME '>' '{'returnClause'}' '</' NAME '>'   #XqTagreturn
     * @param ctx
     * @return
     */
    @Override
    public String visitXqTagreturn(XQueryOptParser.XqTagreturnContext ctx) {

        String tagName = ctx.NAME().get(0).getText();
        String returnClauseStr = visit(ctx.returnClause());
        String res = " <"+ tagName + ">" + "{ "+returnClauseStr +" }"+"</" +tagName+"> ";
        return res;
    }

    /**
     * | cond 'and' cond            # CondAnd
     * @param ctx
     * @return
     */
    @Override
    public String visitCondAnd(XQueryOptParser.CondAndContext ctx) {
        String cond1 = visit(ctx.cond(0));
        String cond2 = visit(ctx.cond(1));
        String resStr = cond1+" and "+cond2;
        return resStr;
    }

    /**
     * (Variable|StringConstant) ('=' | 'eq') (Variable|StringConstant)       # CondEqual
     * @param ctx
     * @return
     */
    @Override
    public String visitCondEqual(XQueryOptParser.CondEqualContext ctx) {
        String left;
        String right;
        if(ctx.Variable().size()>=2){
            left = (ctx.Variable(0).getText());
            right = (ctx.Variable(1).getText());
        }else if(ctx.StringConstant().size()>=2){
            left=ctx.StringConstant(0).getText();
            right = (ctx.StringConstant(1).getText());
        }else{
            left = (ctx.StringConstant(0).getText());
            right = (ctx.Variable(0).getText());
        }
        String resStr = left+" = "+ right;
        return resStr;
    }
}
