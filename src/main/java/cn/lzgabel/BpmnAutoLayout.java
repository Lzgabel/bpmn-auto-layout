package cn.lzgabel;


import cn.lzgabel.bpmn.generator.internal.generated.model.ObjectFactory;
import cn.lzgabel.bpmn.generator.internal.generated.model.TDefinitions;
import cn.lzgabel.layouter.Layouter;
import cn.lzgabel.util.BpmnNamespacePrefixMapper;
import cn.lzgabel.util.Util;
import jakarta.xml.bind.*;
import org.activiti.bpmn.model.BpmnModel;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class BpmnAutoLayout {

    private static final Marshaller marshaller;
    private static final Unmarshaller unmarshaller;

    public static String layout(String bpmn) throws Exception {
        BpmnModel bpmnModel = Util.read(bpmn);

        // layout
        new Layouter(bpmnModel).execute();

        // 重写headers, 命名空间
        TDefinitions originDefinitions = unmarshall(bpmn);
        TDefinitions layoutedDefinitions = unmarshall(Util.write(bpmnModel));

        // 重写定义 exporter
        originDefinitions.setExporter("BPMNLayouter");
        originDefinitions.setExporterVersion("1.0.0");
        originDefinitions.getBPMNDiagram().clear();
        originDefinitions.getBPMNDiagram().addAll(layoutedDefinitions.getBPMNDiagram());
        return marshal(originDefinitions);
    }

    static {
        try {
            JAXBContext context = JAXBContext.newInstance(TDefinitions.class);
            marshaller = context.createMarshaller();
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to initialize the JAXBContext", e);
        }
    }

    public static String marshal(TDefinitions definitions) {
        try {
            JAXBElement<TDefinitions> root = new ObjectFactory().createDefinitions(definitions);
            StringWriter stringWriter = new StringWriter();
            // 1) 见 createMarshaller() 隐藏报文头 2) 自定义生成
            stringWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            createMarshaller().marshal(root, stringWriter);
            return stringWriter.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal", e);
        }
    }

    private static Marshaller createMarshaller() throws JAXBException {
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        try {
            marshaller.setProperty("cn.lzgabel.jaxb.namespacePrefixMapper", new BpmnNamespacePrefixMapper());
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 1) 隐去报文头的生成, Marshaller.JAXB_FRAGMENT默认为false
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        } catch (PropertyException e) {
            // In case another JAXB implementation is used
            // do not stop processing, namespace prefixes will be generated automatically in that case
            e.printStackTrace();
        }
        return marshaller;
    }

    public static TDefinitions unmarshall(String xml) {
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            JAXBElement<TDefinitions> root = unmarshaller.unmarshal(source, TDefinitions.class);
            return root.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal", e);
        }
    }


}
