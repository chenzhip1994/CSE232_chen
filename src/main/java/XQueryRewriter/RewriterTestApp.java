package XQueryRewriter;

import Antlr.XQueryLexer;
import Antlr.XQueryOptLexer;
import Antlr.XQueryOptParser;
import Antlr.XQueryParser;
import XQuery.XQueryMyVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

public class RewriterTestApp {
    public static void main(String[] args) {
        new RewriterTestApp().run(args);
    }
    public void run(String[] args){
        String inputFilePath = "input.txt";
        if(args.length > 0){
            inputFilePath = args[0];
        }else{
            System.out.println("use default input file, input.txt");
        }
        try {
            System.out.println("input file: "+ inputFilePath);
            FileInputStream input = null;
            input = new FileInputStream(inputFilePath);
            ANTLRInputStream antlrStr = new ANTLRInputStream (input);
            String res = getOptimizedStr(antlrStr);
            System.out.println(res);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOptimizedStr(ANTLRInputStream ANTLRInput){
        XQueryOptLexer xQueryLexer = new XQueryOptLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xQueryLexer);
        XQueryOptParser xQueryParser = new XQueryOptParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xQueryParser.xq();
        XQueryRewriterVisitor myVisitor = new XQueryRewriterVisitor();
        return myVisitor.visit(xPathTree);
    }

    public  String getQueryStr(ANTLRInputStream ANTLRInput) {
        XQueryOptLexer xQueryLexer = new XQueryOptLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xQueryLexer);
        XQueryOptParser xQueryParser = new XQueryOptParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xQueryParser.xq();
        XQueryStringVisitor myVisitor = new XQueryStringVisitor();
        return myVisitor.visit(xPathTree);
    }
}
