package cn.lzgabel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;

import cn.lzgabel.bpmn.generator.internal.generated.model.TDefinitions;
import cn.lzgabel.util.BpmnInOut;
import cn.lzgabel.util.Util;
import cn.lzgabel.util.XmlParser;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.io.FileUtils;
import org.jdom2.Element;

import cn.lzgabel.layouter.SimpleGridLayouter;

public class App
{

    private static boolean move = false;

	public static void main( String[] args ) throws Exception
    {
    	String filename = "A.1.0";
    	layoutFile(filename);
    }

	private static void layoutFiles(String[] files)
			throws Exception {
		for(String file : files)
		{
			layoutFile(file);
		}
	}

    static void layoutFile(String filename) throws Exception
    {
    	String filePath = "res/" + filename + ".bpmn";
    	File file = new File(filePath);
    	File copy = new File(filePath + "copy");
    	FileUtils.copyFile(file, copy);

    	HashMap<String, Element> extensionMap = Util.removeAndGetElementsFromXML(filePath + "copy", "extensionElements");

    	BpmnModel model = Util.readBPMFile(copy);

    	SimpleGridLayouter layouter = new SimpleGridLayouter(model);
    	try {
    		layouter.layoutModelToGrid(move);
    	}catch(Exception e) {
        	layouter = new SimpleGridLayouter(model);
    		layouter.layoutModelToGrid(false);
    	}
    	layouter.applyGridToModel();

    	String name = "target/" + filename + "_layout.bpmn";
    	Util.writeModel(model, name);
    	Util.addXMLElementsBackToFile(extensionMap, name);

    	copy.delete();
    }
}
