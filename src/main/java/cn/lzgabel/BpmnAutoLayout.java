package cn.lzgabel;


import cn.lzgabel.bpmn.generator.internal.generated.model.TDefinitions;
import cn.lzgabel.layouter.SimpleGridLayouter;
import cn.lzgabel.util.BpmnInOut;
import cn.lzgabel.util.Util;
import cn.lzgabel.util.XmlParser;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;

public class BpmnAutoLayout {

    public static String layout(String bpmn) throws Exception {
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
        XmlParser xmlParser = new XmlParser();
        TDefinitions definitions = xmlParser.unmarshall(xml);
        BpmnInOut bpmnInOut = new BpmnInOut(xmlParser);
        String layoutedXml = bpmnInOut.writeToBpmn(definitions);
        return layoutedXml;
    }
}
