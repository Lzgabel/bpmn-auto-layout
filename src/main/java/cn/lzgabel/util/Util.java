package cn.lzgabel.util;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;

public class Util {
    private static final BpmnXMLConverter BPMN_CONVERTER = new BpmnXMLConverter();
    public static String write(BpmnModel bpmnModel)
            throws FactoryConfigurationError {
        byte[] xmlBytes = BPMN_CONVERTER.convertToXML(bpmnModel);
        String xml = new String(xmlBytes);
        return xml;
    }

    public static BpmnModel read(String xml)
            throws XMLStreamException, FactoryConfigurationError {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()));
        BpmnModel model = BPMN_CONVERTER.convertToBpmnModel(reader);
        return model;
    }
}
