package XQuery;
import Antlr.XQueryOptLexer;
import Antlr.XQueryOptParser;
import Antlr.XQueryParser;
import Antlr.XQueryLexer;
import XQueryRewriter.XQueryRewriterVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

public class XQueryTestApp {
    public static void main(String[] args) {
        new XQueryTestApp().run(args);
    }
    public void run(String[] args){
        String inputFilePath = "input.txt";
        boolean optimizeOpen = false;
        if(args.length > 0){
            optimizeOpen = true;
        }else{
            System.out.println("use default input file, input.txt");
        }
        System.out.println("input file:" +inputFilePath);
        System.out.println("Optimizer Open:"+optimizeOpen);
        try {
            System.out.println("input file: "+ inputFilePath);
            FileInputStream input = null;
            input = new FileInputStream(inputFilePath);
            ANTLRInputStream antlrStr = new ANTLRInputStream (input);
            LinkedList<Node> res ;
            if(optimizeOpen){
                String optimizedQuery = optimizeQuery(antlrStr);
                res = doQuery(optimizedQuery);
            }else{
                res = doQuery(antlrStr);
            }
            generateResultFile(res);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String optimizeQuery(ANTLRInputStream ANTLRInput){
        XQueryOptLexer xQueryLexer = new XQueryOptLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xQueryLexer);
        XQueryOptParser xQueryParser = new XQueryOptParser(tokens);
        ParseTree xPathTree = xQueryParser.xq();
        XQueryRewriterVisitor myVisitor = new XQueryRewriterVisitor();
        String optString = myVisitor.visit(xPathTree);
        System.out.println("----optimized query----");
        System.out.println(optString);
        return optString;
    }

    public  LinkedList<Node> doQuery(String inputStr) throws ParserConfigurationException {
        XQueryLexer xQueryLexer = new XQueryLexer(new ANTLRInputStream(inputStr));
        CommonTokenStream tokens = new CommonTokenStream(xQueryLexer);
        XQueryParser xQueryParser = new XQueryParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xQueryParser.xq();
        XQueryMyVisitor queryMyVisitor = new XQueryMyVisitor();
        return queryMyVisitor.visit(xPathTree);
    }

    public  LinkedList<Node> doQuery(ANTLRInputStream ANTLRInput) throws ParserConfigurationException {
        XQueryLexer xQueryLexer = new XQueryLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xQueryLexer);
        XQueryParser xQueryParser = new XQueryParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xQueryParser.xq();
        XQueryMyVisitor queryMyVisitor = new XQueryMyVisitor();
        return queryMyVisitor.visit(xPathTree);
    }

    public void generateResultFile(LinkedList<Node> resLs){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            if(resLs.size() <=0){

            }else if(resLs.size()<=1){
                Node ownNode = doc.importNode(resLs.get(0),true);
                doc.appendChild(ownNode);
            }else{
                Element rootElement = doc.createElement("root");
                doc.appendChild(rootElement);
                for(Node node : resLs){
                    node = doc.importNode(node,true);
                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element elem = (Element) node.cloneNode(true);
                        rootElement.appendChild(elem);
                    }
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("result.xml"));
            transformer.transform(source, result);

            System.out.println("Result file saved!" + " as result.xml");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
