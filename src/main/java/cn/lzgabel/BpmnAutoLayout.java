package cn.lzgabel;


import cn.lzgabel.bpmn.generator.internal.generated.model.*;
import cn.lzgabel.layouter.SimpleGridLayouter;
import cn.lzgabel.util.BpmnInOut;
import cn.lzgabel.util.Util;
import cn.lzgabel.util.XmlParser;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BpmnAutoLayout {

    public static String layout(String bpmn) throws Exception {
        XmlParser xmlParser = new XmlParser();
        TDefinitions originDefinitions = xmlParser.unmarshall(bpmn);

        // 保存原始 extensionElements
        Map<String, TExtensionElements> extensionElementsMap = new HashMap<>();
        originDefinitions.getRootElement().forEach(rootElement -> {
            TRootElement tRootElement = rootElement.getValue();
            if (tRootElement instanceof TProcess) {
                TProcess process = (TProcess) tRootElement;
                process.getFlowElement().forEach(flowElement -> {
                    TFlowElement element = flowElement.getValue();
                    TExtensionElements extensionElements = element.getExtensionElements();
                    if (Objects.nonNull(extensionElements)) {
                        List<Object> any = extensionElements.getAny();

                        any.forEach(e -> {
                            System.out.println(e);
                        });

                        extensionElementsMap.put(element.getId(), extensionElements);
                    }
                });
            }
        });

        BpmnModel model = Util.readFromBpmn(bpmn);
        SimpleGridLayouter layouter = new SimpleGridLayouter(model);
        try {
            layouter.layoutModelToGrid(false);
        } catch (Exception e) {
            layouter = new SimpleGridLayouter(model);
            layouter.layoutModelToGrid(false);
        }
        layouter.applyGridToModel();

        // layouted xml
        byte[] xmlBytes = new BpmnXMLConverter().convertToXML(model);
        String xml = new String(xmlBytes);

        // 重写headers, 命名空间
        TDefinitions layoutedDefinitions = xmlParser.unmarshall(xml);

        // 还原 extensionElements
        layoutedDefinitions.getRootElement().forEach(rootElement -> {
            TRootElement tRootElement = rootElement.getValue();
            if (tRootElement instanceof TProcess) {
                TProcess process = (TProcess) tRootElement;
                process.getFlowElement().forEach(flowElement -> {
                    TFlowElement element = flowElement.getValue();
                    if (extensionElementsMap.containsKey(element.getId())) {
                        element.setExtensionElements(extensionElementsMap.get(element.getId()));
                    }
                });
            }
        });

        BpmnInOut bpmnInOut = new BpmnInOut(xmlParser);
        String layoutedXml = bpmnInOut.writeToBpmn(layoutedDefinitions);
        return layoutedXml;
    }
}
