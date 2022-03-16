package cn.lzgabel.util;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;

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
