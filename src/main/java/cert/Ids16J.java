package cert;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * IDS16-J: Prevent XML Injection.
 *
 * <p>Owner: Ian
 *
 * <p>If untrusted text is concatenated into an XML document, the attacker can
 * include tags of their own and so add or change elements. Here an order is
 * built with a quantity typed by the user; a crafted "quantity" smuggles in a
 * second {@code <price>} element that the reader then picks up. The fix is to
 * build the document with an XML API (which escapes text for us) and to
 * validate the value against what it is supposed to be.
 *
 * <p>Everything stays in memory: no files, no network.
 */
public class Ids16J {

    /** The real price of the item, set by the shop (never by the user). */
    private static final String REAL_PRICE = "500.00";

    /**
     * NONCOMPLIANT: concatenates the untrusted quantity into the XML text.
     *
     * @param quantity untrusted quantity string from the user
     * @return the XML document as text
     */
    static String noncompliantBuild(String quantity) {
        return "<item><description>Widget</description>"
                + "<quantity>" + quantity + "</quantity>"       // violates IDS16-J
                + "<price>" + REAL_PRICE + "</price></item>";
    }

    /**
     * COMPLIANT: builds the document with the DOM API, which escapes markup
     * characters in text content, and rejects non-numeric quantities.
     *
     * @param quantity untrusted quantity string from the user
     * @return the XML document as text
     * @throws IllegalArgumentException if the quantity is not a whole number
     */
    static String compliantBuild(String quantity) throws Exception {
        if (quantity == null || !quantity.matches("[0-9]{1,6}")) {
            throw new IllegalArgumentException("quantity must be digits only but was: " + quantity);
        }
        Document doc = secureBuilder().newDocument();
        Element item = doc.createElement("item");
        doc.appendChild(item);
        addChild(doc, item, "description", "Widget");
        addChild(doc, item, "quantity", quantity);
        addChild(doc, item, "price", REAL_PRICE);
        return serialize(doc);
    }

    /**
     * Variant of the compliant builder WITHOUT the digits check, used only to
     * show that the XML API alone already escapes the injected markup.
     *
     * @param quantity untrusted quantity string from the user
     * @return the XML document as text
     */
    static String compliantBuildEscapingOnly(String quantity) throws Exception {
        Document doc = secureBuilder().newDocument();
        Element item = doc.createElement("item");
        doc.appendChild(item);
        addChild(doc, item, "description", "Widget");
        addChild(doc, item, "quantity", quantity);
        addChild(doc, item, "price", REAL_PRICE);
        return serialize(doc);
    }

    private static void addChild(Document doc, Element parent, String name, String text) {
        Element e = doc.createElement(name);
        e.setTextContent(text); // text content is escaped on output
        parent.appendChild(e);
    }

    private static DocumentBuilder secureBuilder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder();
    }

    private static String serialize(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter w = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(w));
        return w.toString();
    }

    /**
     * The "reader": parses the XML and returns the first {@code <price>} it
     * finds, as a consumer of this document would.
     *
     * @param xml the XML text
     * @return the price text, or a note if the XML is not well formed
     */
    static String readPrice(String xml) throws Exception {
        Document doc = secureBuilder().parse(new InputSource(new StringReader(xml)));
        return doc.getElementsByTagName("price").item(0).getTextContent();
    }

    private static void run(String quantity) throws Exception {
        String bad = noncompliantBuild(quantity);
        System.out.println("noncompliant XML  : " + bad);
        System.out.println("noncompliant price: " + readPrice(bad));
        try {
            String good = compliantBuild(quantity);
            System.out.println("compliant XML     : " + good);
            System.out.println("compliant price   : " + readPrice(good));
        } catch (IllegalArgumentException e) {
            System.out.println("compliant         : REJECTED (" + e.getMessage() + ")");
        }
    }

    /**
     * Demonstrates the injection and both layers of the defense.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== IDS16-J: Prevent XML Injection ===");

        String attack = "1</quantity><price>1.00</price><quantity>1";

        System.out.println("\n-- Scenario 1: quantity = " + attack + " --");
        run(attack);
        System.out.println("-> noncompliant document now has TWO price elements and the reader takes the"
                + "\n   attacker's 1.00; compliant never lets that input reach the document.");

        System.out.println("\n-- Scenario 2: same input, escaping only (digits check removed) --");
        String escaped = compliantBuildEscapingOnly(attack);
        System.out.println("escaped XML  : " + escaped);
        System.out.println("price read   : " + readPrice(escaped));
        System.out.println("-> even without validation the XML API turned the tags into harmless text"
                + "\n   (&lt; and &gt;), so the real price survives.");

        System.out.println("\n-- Scenario 3 (CONTROL CASE): quantity = 3; identical output is expected --");
        run("3");
    }
}
