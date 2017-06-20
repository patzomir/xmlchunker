package com.patzo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.rmi.runtime.Log;

import javax.xml.namespace.QName;
import javax.xml.parsers.*;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.nio.file.*;
import java.util.stream.*;

/**
 * Created by patzo on 08.06.17.
 */
public class xmlChunker {
    XMLInputFactory inFactory = XMLInputFactory.newInstance();
    XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
    OutputStream outputStream;
    int chunk_number = 1;
    XMLEventReader eventReader;
    int bytes = 0;
    Document doc;
    Transformer transformer;
    DocumentBuilder docBuilder;
    List<List<String>> not_split = new ArrayList<List<String>>();
    String outputDir;

    public static void main(String[] args){
        if (args.length != 3) {
            System.out.println("Usage: java -jar xmlChunker.jar [XML file to be chunked] [config file] [output dir]");
        }
        new xmlChunker(args[2]).execute(args);
    }

    public xmlChunker(String outputDir){
        this.outputDir = outputDir;
    }

    public void execute(String[] args) {
        try {
            setNot_split(args[1]);
        }catch (IOException e){System.out.println("CONFIIIIG ERROR");}
        initializeProperties();

            List<File> filesInFolder = new ArrayList<File>();
            filesInFolder.add(new File(args[0]));


        for (File inputFile: filesInFolder) {
                File outputFile = new File(inputFile.getName());
                try {

                    eventReader =
                            inFactory.createXMLEventReader(
                                    new FileReader(inputFile));

                    iterateXML(outputFile);

                } catch (IOException ex) {
                    System.out.println("");
                } catch (XMLStreamException ex){
                    System.out.println("a");
                } catch (TransformerException ex){
                    System.out.println("b");
                }
            }

    }

    private void initializeProperties(){
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.newDocument();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
        }catch(ParserConfigurationException ex) {
        }catch(TransformerConfigurationException ex) { System.out.println();}
    }

    private void iterateXML(File outputFile)throws
            XMLStreamException, IOException, TransformerException {
        Node context = doc;


        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            bytes = bytes + event.toString().length();
            if (bytes > 150 * 3 * 1000 * 1000 &&
                    event.getEventType() == XMLStreamConstants.END_ELEMENT &&
                    !(isWithinNodes(context.getParentNode()))) {

                setNextOutputStream(outputFile.getName());
                context = cleanDoc(context.getParentNode());
                continue;
            }
            switch (event.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    setNextOutputStream(outputFile.getName());
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    QName name = event.asStartElement().getName();

                    Element curr = doc.createElementNS(name.getNamespaceURI(),
                            name.getLocalPart());
                    context.appendChild(curr);
                    context = curr;

                    Iterator<Attribute> iterator = event.asStartElement().getAttributes();
                    while(iterator.hasNext()){
                        Attribute attr = iterator.next();
                        ((Element) context).setAttributeNS(attr.getName().getNamespaceURI(),
                                attr.getName().getLocalPart(), attr.getValue());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    context = context.getParentNode();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    context.appendChild(doc.createTextNode(event.asCharacters().getData()));
                    break;
            }
        }
    }

    private void setNextOutputStream(String outputFile) throws IOException, XMLStreamException,
            TransformerException {
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputDir + File.separator + chunk_number + "_" + outputFile));
        transformer.transform(source, result);
        bytes = 0;
        chunk_number ++;
    }

    private boolean isWithinNodes(Node current){
        for (List<String> r : not_split){
            if(isWithinNode(current, (String)r.toArray()[0], (String)r.toArray()[1]))
                return true;
        }
        return false;
    }

    private boolean isWithinNode(Node current, String parentNamespaceURI, String parentLocal){
        while(current.getParentNode() != doc){
            if ((parentNamespaceURI != null ||
                    current.getNamespaceURI().equals(parentNamespaceURI)) &&
                    current.getLocalName().equals(parentLocal))
                return true;
            current = current.getParentNode();
        }
        return false;
    }

    private Node cleanDoc(Node reach_elem){
        Document doc2 = docBuilder.newDocument();

        Node current = reach_elem;
        Node prev = null;
        Node out = null;
        while(current != doc){
            Node temp = current.cloneNode(false);

            if (out == null)
                out = temp;
            current = current.getParentNode();
            if (prev != null)
                temp.appendChild(prev);
            prev = temp;
        }

        current = doc2.adoptNode(prev);
        doc2.appendChild(current);
        doc = doc2;
        return out;
    }
    private void setNot_split(String configPath) throws  IOException{
        BufferedReader br = new BufferedReader(new FileReader(configPath));
        try {
            String line = br.readLine();

            while (line != null) {
                String[] row = line.split(";");
                List<String> temp = new ArrayList<String>();
                temp.add(row[0]);
                temp.add(row[1]);
                not_split.add(temp);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
    }

}

