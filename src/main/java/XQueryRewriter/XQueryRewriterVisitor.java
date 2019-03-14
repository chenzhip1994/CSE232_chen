package XQueryRewriter;

import Antlr.XQueryOptParser;

import java.util.*;

public class XQueryRewriterVisitor extends XQueryStringVisitor{

    class VNode extends Object{
        String name;
        String queryClause;
        VNode parent;
        public VNode(String name,String queryClause,VNode parent){
            this.name = name;
            this.queryClause = queryClause;
            this.parent = parent;
        }
        public VNode findRoot(){
            VNode cur = this;
            while (cur.parent!=null){
                cur = cur.parent;
            }
            return cur;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
    }

    class QTree{
        VNode root;
        Map<String,VNode> nodesMap;

        public QTree(String rootName,String queryClause){
            nodesMap = new HashMap<>();
            VNode nRoot = new VNode(rootName,queryClause,null);
            this.root = nRoot;
            nodesMap.put(rootName,root);
        }
        public QTree(VNode nRoot){
            nodesMap = new HashMap<>();

            this.root = nRoot;
            nRoot.parent = null;
            nodesMap.put(nRoot.name,root);
        }
        public void addNode(VNode newNode){
            this.nodesMap.put(newNode.name,newNode);
        }
        public VNode findNode(String variableName){
            if(this.nodesMap.containsKey(variableName)){
                return nodesMap.get(variableName);
            }else{
                return null;
            }
        }

        LinkedList<VNode> getBFSList(){
            LinkedList<VNode> res = new LinkedList<>();
            Queue<VNode> queue = new LinkedList<>();
            queue.add(root);
            while (!queue.isEmpty()){
                VNode curr = queue.poll();
                res.add(curr);
                for(String key : nodesMap.keySet()){
                    VNode temp = nodesMap.get(key);
                    if(temp.parent == curr){
                        queue.add(temp);
                    }
                }
            }
            return res;
        }
    }
    class QueryForest{
        /**
         * The map root -> sub Query Variables
         */
        public LinkedList<QTree> queryTrees;
        public Map<VNode, Set<VNode>> condVarEqualMap;
        public Map<VNode,List<String>> condVarRestrictMap;
        public List<String> freeRestrictList;
        public Set<VNode> joinedNodes;

        public boolean inReturn = false;
        public QueryForest(){
            this.condVarEqualMap = new HashMap<>();
            this.condVarRestrictMap = new HashMap<>();
            this.freeRestrictList = new LinkedList<>();
            this.queryTrees = new LinkedList<>();
            this.joinedNodes = new HashSet<>();
        }

        public VNode findNodes(String vname){
            for(QTree qtree: this.queryTrees){
                if(qtree.findNode(vname)!=null){
                    return qtree.findNode(vname);
                }
            }
            return null;
        }
        public QTree findTreebyNode(String vname){
            for(QTree qtree: this.queryTrees){
                if(qtree.findNode(vname)!=null){
                    return qtree;
                }
            }
            return null;
        }

        public String getNextQuery(){
            if(queryTrees.size()<=0){
                System.err.println("get sub query tree error");
                return null;
            }
            QTree qtree = queryTrees.poll();
            StringBuilder queryStr = new StringBuilder();
            //for
            queryStr.append(" for ");
            List<String> varQueryList = new LinkedList<>();
            List<VNode> bfsNodeList = qtree.getBFSList();
            for(VNode node : bfsNodeList){
                String varQuery = node.name + " in "+node.queryClause;
                varQueryList.add(varQuery);
            }
            queryStr.append(String.join(",", varQueryList));

            //where
            List<String> whereStrList = new LinkedList<>();
//            whereStrList.addAll(queryForest.freeRestrictList);

            for(VNode node : bfsNodeList){
                if(condVarRestrictMap.containsKey(node)){
                    List<String> constantVals = condVarRestrictMap.get(node);
                    for(String constant : constantVals){
                        String condStr = node.name +" = "+ constant;
                        whereStrList.add(condStr);
                    }
                }
            }

            for(VNode left : bfsNodeList){
                for(VNode right : bfsNodeList){
                  if(condVarEqualMap.containsKey(left) && condVarEqualMap.get(left).contains(right)){
                      String condStr = left.name + " = "+ right.name;
                      whereStrList.add(condStr);
                      condVarEqualMap.get(left).remove(right);
                      condVarEqualMap.get(right).remove(left);
                  }
                }
            }

            if(whereStrList.size()>0){
                queryStr.append(" where ");
                String allWheres = String.join(" and ", whereStrList);
                queryStr.append(allWheres);
            }

            //return
            queryStr.append(" return ");
            LinkedList<String> retValList = new LinkedList<>();
            for(VNode node : bfsNodeList){
                String var = node.name;
                String tagName= node.name.substring(1);
                String varTagStr = "<" + tagName + ">{" + var + "}</" + tagName + ">";
                retValList.add(varTagStr);
            }
            String retStr = "<tuple>{" + String.join(" , ", retValList) + "}</tuple>";
            queryStr.append(retStr);
            joinedNodes.addAll(bfsNodeList);
            return queryStr.toString();

        }

    }

    QueryForest queryForest = new QueryForest();



    /**
     * xq
     *     : 'for' Variable 'in' path (',' Variable 'in' path)* 'where' cond 'return' returnClause   #XqFWR
     *     ;
     * @param ctx
     * @return
     */
    @Override
    public String visitXqFWR(XQueryOptParser.XqFWRContext ctx) {
        /**
         * Fisrt, collect all the variables, creat the dependecy forest
         */
        for(int i=0;i<ctx.Variable().size();++i){
            String variable = ctx.Variable(i).getText();
            String queryPath = visit(ctx.path(i));
            if(queryPath.trim().startsWith("doc")){
                VNode newRoot = new VNode(variable,queryPath,null);
                QTree newTree = new QTree(newRoot);
                queryForest.queryTrees.add(newTree);
            }else{
                String parentName = queryPath.split("/")[0];
                QTree qTree = queryForest.findTreebyNode(parentName);
                if(qTree == null){
                    System.err.println("variable dependency error");
                    return null;
                }
                VNode parentNode = qTree.findNode(parentName);
                VNode newNode = new VNode(variable,queryPath,parentNode);
                qTree.addNode(newNode);
            }
        }
        visit(ctx.cond());

        StringBuilder resultQuery = new StringBuilder();
        resultQuery.append("for $tuple in ");

        String queryAcc = queryForest.getNextQuery();

        while (queryForest.queryTrees.size()>0){
            List<VNode> nextNodes = queryForest.queryTrees.peek().getBFSList();

            LinkedList<String> leftStrLs = new LinkedList<>();
            LinkedList<String> rightStrLs = new LinkedList<>();
            for(VNode leftNode : queryForest.joinedNodes){
                if(queryForest.condVarEqualMap.containsKey(leftNode)){
                    Set<VNode> leftEqualSet = queryForest.condVarEqualMap.get(leftNode);
                    for(VNode rightNode:nextNodes){
                        if(leftEqualSet.contains(rightNode)){
                            leftStrLs.add(leftNode.name.substring(1));
                            rightStrLs.add(rightNode.name.substring(1));
                            queryForest.condVarEqualMap.get(leftNode).remove(rightNode);
                            queryForest.condVarEqualMap.get(rightNode).remove(leftNode);
                        }
                    }
                }
            }
            String nextQueryStr  = queryForest.getNextQuery();
            queryAcc = "join(" +
                    queryAcc + "," +
                    nextQueryStr + "," +
                    "[" + String.join(",", leftStrLs) + "]," +
                    "[" + String.join(",", rightStrLs) + "])";

        }

        queryForest.inReturn = true;
        String retClauseStr = visit(ctx.returnClause());
        queryAcc = queryAcc+" return"+retClauseStr;
        resultQuery.append(queryAcc);

        queryForest = new QueryForest();
        return resultQuery.toString();
    }

    @Override
    public String visitPath(XQueryOptParser.PathContext ctx) {
        if(queryForest.inReturn){
            StringBuilder pathStr = new StringBuilder();
            if(ctx.StringConstant()!=null){
                pathStr.append(" doc(" + ctx.StringConstant().getText() + ") ");
            }else{
                String varName = ctx.Variable().getText().substring(1);
                pathStr.append( "$tuple/" + varName + "/*");
            }
            for(int i=0;i<ctx.NAME().size();++i){
                pathStr.append(ctx.sep(i).getText());
                pathStr.append(ctx.NAME(i).getText());
            }
            if(ctx.sep().size() > ctx.NAME().size()){
                pathStr.append(ctx.sep(ctx.sep().size()-1).getText() + "text()");
            }
            return pathStr.toString();
        }else {
            return super.visitPath(ctx);
        }
    }

    @Override
    public String visitSep(XQueryOptParser.SepContext ctx) {
        return super.visitSep(ctx);
    }

    @Override
    public String visitXqreturnVar(XQueryOptParser.XqreturnVarContext ctx) {
        String varName = ctx.Variable().getText().substring(1);
        String retVarStr = "$tuple/" + varName + "/*";
        return retVarStr;
    }

    @Override
    public String visitXqTworeturn(XQueryOptParser.XqTworeturnContext ctx) {
        String str1 = visit(ctx.returnClause(0));
        String str2 = visit(ctx.returnClause(1));
        String res = str1+" , "+str2;
        return res;
    }

    @Override
    public String visitXqreturnpath(XQueryOptParser.XqreturnpathContext ctx) {
        String pathStr = visit(ctx.path());
        return pathStr;
    }

    @Override
    public String visitXqTagreturn(XQueryOptParser.XqTagreturnContext ctx) {
        String tagName = ctx.NAME().get(0).getText();
        String returnClauseStr = visit(ctx.returnClause());
        String res = " <"+ tagName + ">" + "{ "+returnClauseStr +" }"+"</" +tagName+"> ";
        return res;
    }

    @Override
    public String visitCondAnd(XQueryOptParser.CondAndContext ctx) {
        String cond1 = visit(ctx.cond(0));
        String cond2 = visit(ctx.cond(1));
        String resStr = cond1+" and "+cond2;
        return resStr;
    }

    @Override
    public String visitCondEqual(XQueryOptParser.CondEqualContext ctx) {

        if(ctx.Variable().size()>=2){
            String left = (ctx.Variable(0).getText());
            String right = (ctx.Variable(1).getText());
            VNode leftNode = queryForest.findNodes(left);
            VNode rightNode = queryForest.findNodes(right);

            queryForest.condVarEqualMap.putIfAbsent(leftNode,new HashSet<>());
            queryForest.condVarEqualMap.get(leftNode).add(rightNode);

            queryForest.condVarEqualMap.putIfAbsent(rightNode,new HashSet<>());
            queryForest.condVarEqualMap.get(rightNode).add(leftNode);

        }else if(ctx.StringConstant().size()>=2){
            String left=ctx.StringConstant(0).getText();
            String right = (ctx.StringConstant(1).getText());
            queryForest.freeRestrictList.add(" "+ left+" = "+right+" ");
        }else{
            String constValue = (ctx.StringConstant(0).getText());
            String varStr = (ctx.Variable(0).getText());
            VNode node = queryForest.findNodes(varStr);
            queryForest.condVarRestrictMap.putIfAbsent(node,new LinkedList<>());
            queryForest.condVarRestrictMap.get(node).add(constValue);
        }

        return super.visitCondEqual(ctx);
    }
}
