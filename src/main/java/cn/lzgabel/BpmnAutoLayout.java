package cn.lzgabel;


import cn.lzgabel.bpmn.generator.internal.generated.model.*;
import cn.lzgabel.layouter.SimpleGridLayouter;
import cn.lzgabel.util.BpmnInOut;
import cn.lzgabel.util.Util;
import cn.lzgabel.util.XmlParser;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BpmnAutoLayout {

    /**
     * 保存原始 extensionElements
     */
    private static Map<String, TExtensionElements> extensionElementsMap;

    public static String layout(String bpmn) throws Exception {
        extensionElementsMap = new HashMap<>(16);
        XmlParser xmlParser = new XmlParser();
        TDefinitions originDefinitions = xmlParser.unmarshall(bpmn);
        originDefinitions.getRootElement().forEach(rootElement -> {
            TRootElement tRootElement = rootElement.getValue();
            if (tRootElement instanceof TProcess) {
                TProcess process = (TProcess) tRootElement;
                process.getFlowElement().forEach(flowElement -> {
                    TFlowElement element = flowElement.getValue();
                    stashExtensitionElements(element);
                });
            } else {
                stashExtensitionElements(tRootElement);
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

        // 定义 exporter
        layoutedDefinitions.setExporter("BPMNLayouter");
        layoutedDefinitions.setExporterVersion("1.0.0");

        // 还原 extensionElements
        layoutedDefinitions.getRootElement().forEach(rootElement -> {
            TRootElement tRootElement = rootElement.getValue();
            if (tRootElement instanceof TProcess) {
                TProcess process = (TProcess) tRootElement;
                process.getFlowElement().forEach(flowElement -> {
                    TFlowElement element = flowElement.getValue();
                    unStashExtensitionElements(element);
                });
            } else {
                unStashExtensitionElements(tRootElement);
            }
        });

        BpmnInOut bpmnInOut = new BpmnInOut(xmlParser);
        String layoutedXml = bpmnInOut.writeToBpmn(layoutedDefinitions);
        return layoutedXml;
    }

    private static void stashExtensitionElements(TBaseElement element) {
        if (element instanceof TSubProcess) {
            TSubProcess subProcess = (TSubProcess) element;
            subProcess.getFlowElement().forEach(sub -> {
                TFlowElement subElement = sub.getValue();
                stashExtensitionElements(subElement);
            });
        } else {
            TExtensionElements extensionElements = element.getExtensionElements();
            if (Objects.nonNull(extensionElements)) {
                extensionElementsMap.put(element.getId(), extensionElements);
            }
        }
    }

    private static void unStashExtensitionElements(TBaseElement element) {
        if (element instanceof TSubProcess) {
            TSubProcess subProcess = (TSubProcess) element;
            subProcess.getFlowElement().forEach(sub -> {
                TFlowElement subElement = sub.getValue();
                unStashExtensitionElements(subElement);
            });
        } else {
            if (extensionElementsMap.containsKey(element.getId())) {
                element.setExtensionElements(extensionElementsMap.get(element.getId()));
            }
        }
    }
}
