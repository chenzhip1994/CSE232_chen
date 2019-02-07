import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Node;

import java.io.FileInputStream;
import java.util.LinkedList;

public class TestApp {
    public static void main(String[] args) {
        new TestApp().run();
    }

    public void run(){
        try {
            String inputFilePath = "/Users/patrickchen/Desktop/CSE232/j_caesar/input.txt";
            FileInputStream input = new FileInputStream(inputFilePath);
            ANTLRInputStream antlrStr = new ANTLRInputStream (input);
            LinkedList<Node> res = Query(antlrStr);
            if(res.size()<=0){
                System.out.println("enpty result");
            }
            for(Node node : res){
                System.out.println(node.toString());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public  LinkedList<Node> Query(ANTLRInputStream ANTLRInput) {
        XPathLexer xPathLexer = new XPathLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xPathLexer);
        XPathParser xPathParser = new XPathParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xPathParser.ap();
        XPathMyVisitor xPathVisitor = new XPathMyVisitor();
        return xPathVisitor.visit(xPathTree);
    }
}
