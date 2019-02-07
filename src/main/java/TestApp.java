import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

public class TestApp {
    public static void main(String[] args) {
        new TestApp().run(args);
    }

    public void run(String[] args){
        try {
            String inputFilePath = "input.txt";
            if(args.length > 1){
                inputFilePath = args[0];
            }else{
                System.out.println("use default input file");
            }
            System.out.println("input file: "+ inputFilePath);
            FileInputStream input = new FileInputStream(inputFilePath);
            ANTLRInputStream antlrStr = new ANTLRInputStream (input);
            LinkedList<Node> res = doQuery(antlrStr);
            if(res.size()<=0){
                System.out.println("Empty result");
            }else{
                System.out.println("Find results: number :"+res.size());
                for(Node node : res){
                    System.out.println(node.toString());
                }
            }
            this.generateResultFile(res);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public  LinkedList<Node> doQuery(ANTLRInputStream ANTLRInput) {
        XPathLexer xPathLexer = new XPathLexer(ANTLRInput);
        CommonTokenStream tokens = new CommonTokenStream(xPathLexer);
        XPathParser xPathParser = new XPathParser(tokens);
        // Parse using ap (Absolute Path) as root rule
        ParseTree xPathTree = xPathParser.ap();
        XPathMyVisitor xPathVisitor = new XPathMyVisitor();
        return xPathVisitor.visit(xPathTree);
    }

    public void generateResultFile(LinkedList<Node> resLs){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("root");
            doc.appendChild(rootElement);
            for(Node node : resLs){
                node = doc.importNode(node,true);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node.cloneNode(true);
                    rootElement.appendChild(elem);
                }else if(node.getNodeType() == Node.ATTRIBUTE_NODE){
                    Element elem = doc.createElement("AttrResult");
                    elem.setAttribute(node.getNodeName(),node.toString());
                    rootElement.appendChild(elem);
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
