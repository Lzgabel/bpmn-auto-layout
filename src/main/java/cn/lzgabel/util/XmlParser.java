/*
 * Copyright 2020 Bonitasoft S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.lzgabel.util;

import cn.lzgabel.bpmn.generator.internal.generated.model.ObjectFactory;
import cn.lzgabel.bpmn.generator.internal.generated.model.TDefinitions;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlParser {

    private static final JAXBContext context = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(TDefinitions.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to initialize the JAXBContext", e);
        }
    }

    public String marshal(TDefinitions definitions) {
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

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        try {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new BpmnNamespacePrefixMapper());
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 1) 隐去报文头的生成, Marshaller.JAXB_FRAGMENT默认为false
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        } catch(PropertyException e) {
            // In case another JAXB implementation is used
            // do not stop processing, namespace prefixes will be generated automatically in that case
            e.printStackTrace();
        }
        return marshaller;
    }

    public TDefinitions unmarshall(String xml) {
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            JAXBElement<TDefinitions> root = context.createUnmarshaller().unmarshal(source, TDefinitions.class);
            return root.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to marshal", e);
        }
    }

}
