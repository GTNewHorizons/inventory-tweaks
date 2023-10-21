package invtweaks.forge.asm.compatibility;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class CompatibilityConfigLoader extends DefaultHandler {

    private Map<String, ContainerInfo> config;

    public CompatibilityConfigLoader(Map<String, ContainerInfo> compatibilityConfig) {
        config = compatibilityConfig;
    }

    public static Map<String, ContainerInfo> load(String filePath) throws Exception {
        Map<String, ContainerInfo> config = new HashMap<String, ContainerInfo>();

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(new File(filePath), new CompatibilityConfigLoader(config));

        return config;
    }

    /**
     * Receive notification of the start of an element.
     *
     * @param uri        The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace
     *                   processing is not being performed.
     * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being
     *                   performed.
     * @param qName      The qualified name (with prefix), or the empty string if qualified names are not available.
     * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty
     *                   Attributes object.
     * @see org.xml.sax.ContentHandler#startElement
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("chest".equals(qName) || "inventory".equals(qName)) {
            ContainerInfo info = new ContainerInfo();
            String className = attributes.getValue("class");

            if (className == null) {
                return;
            }

            if ("chest".equals(qName)) {
                info.validChest = true;
                String rowSizeAttr = attributes.getValue("row_size");
                if (rowSizeAttr != null) {
                    info.rowSize = Short.parseShort(rowSizeAttr);
                }
                info.largeChest = Boolean.parseBoolean(attributes.getValue("large_chest"));
            } else {
                info.validInventory = true;
            }
            info.showButtons = !Boolean.parseBoolean(attributes.getValue("disable_buttons"));

            config.put(className, info);
        }
    }
}
