package cn.lzgabel;

import java.nio.file.Files;
import java.nio.file.Paths;


public class App {


    public static void main(String[] args) throws Exception {
        String filename = "A.1.0";
        layoutFile(filename);
    }

    static void layoutFile(String filename) throws Exception {
        String filePath = "res/" + filename + ".bpmn";
        String layout = BpmnAutoLayout.layout(new String(Files.readAllBytes(Paths.get(filePath))));
        System.out.println(layout);
    }
}
