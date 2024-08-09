package com.xmlReader.xml_comparer.Service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class XmlComparerService {

    public void compareAndGenerateReport(String filePath1, String filePath2, String outputPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc1 = builder.parse(new File(filePath1));
            Document doc2 = builder.parse(new File(filePath2));

            List<Map<String, String>> dataAC = extractValues(doc1.getDocumentElement());
            List<Map<String, String>> dataBC = extractValues(doc2.getDocumentElement());

            List<Map<String, String>> onlyInAC = new ArrayList<>(dataAC);
            onlyInAC.removeAll(dataBC);

            List<Map<String, String>> onlyInBC = new ArrayList<>(dataBC);
            onlyInBC.removeAll(dataAC);

            List<Map<String, String>> commonData = new ArrayList<>(dataAC);
            commonData.retainAll(dataBC);

            Workbook workbook = new XSSFWorkbook();

            createSheet(workbook, "Only in BC", onlyInBC);
            createSheet(workbook, "Only in AC", onlyInAC);
            createDifferenceSheet(workbook, "Difference between BC and AC", commonData, dataAC, dataBC);

            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    workbook.close();
                } catch (IOException e) {
                    System.err.println("Error closing workbook: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("Excel file created successfully!");

        } catch (Exception e) {
            System.err.println("Error processing XML files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Map<String, String>> extractValues(Element element) {
        List<Map<String, String>> data = new ArrayList<>();
        try {
            NodeList nodeList = element.getElementsByTagName("datacapsule");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element datacapsule = (Element) nodeList.item(i);

                Map<String, String> headerMap = new LinkedHashMap<>();
                Element header = (Element) datacapsule.getElementsByTagName("dataheader").item(0);
                if (header != null) {
                    headerMap.put("issuerName", getElementText(header, "issuer_name"));
                    headerMap.put("curveId", getElementText(header, "curve_id"));
                    headerMap.put("ccy", getElementText(header, "ccy"));
                    headerMap.put("running", getElementText(header, "running"));
                    headerMap.put("unit", getElementText(header, "unit"));
                    headerMap.put("curvePath", getElementText(header, "curve_path"));
                    headerMap.put("issuerType", getElementText(header, "issuer_type"));
                }

                NodeList dataList = datacapsule.getElementsByTagName("data");
                for (int j = 0; j < dataList.getLength(); j++) {
                    Element dataElement = (Element) dataList.item(j);
                    Map<String, String> dataMap = new LinkedHashMap<>(headerMap);
                    dataMap.put("tenor", getElementText(dataElement, "tenor"));
                    dataMap.put("spread", getElementText(dataElement, "spread"));
                    dataMap.put("vol", getElementText(dataElement, "vol"));
                    dataMap.put("origSprd", getElementText(dataElement, "orig_sprd"));
                    dataMap.put("fee", getElementText(dataElement, "fee"));
                    data.add(dataMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting values from XML: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                return node.getTextContent();
            }
        }
        return "";
    }

    private void createSheet(Workbook workbook, String sheetName, List<Map<String, String>> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = { "issuerName", "curveId", "ccy", "running", "unit", "curvePath", "issuerType", "tenor", "spread", "vol", "origSprd", "fee" };
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (Map<String, String> rowMap : data) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            for (String header : headers) {
                Cell cell = row.createCell(cellNum++);
                cell.setCellValue(rowMap.getOrDefault(header, ""));
            }
        }
    }

    private void createDifferenceSheet(Workbook workbook, String sheetName, List<Map<String, String>> commonData, List<Map<String, String>> dataAC, List<Map<String, String>> dataBC) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = { "issuerNameBC", "issuerNameAC", "curveIdBC", "curveIdAC", "ccyBC", "ccyAC", "runningBC", "runningAC", "unitBC", "unitAC", "curvePathBC", "curvePathAC", "issuerTypeBC", "issuerTypeAC", "tenorBC", "tenorAC", "spreadBC", "spreadAC", "volBC", "volAC", "origSprdBC", "origSprdAC", "feeBC", "feeAC" };
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (Map<String, String> bcMap : dataBC) {
            for (Map<String, String> acMap : dataAC) {
                if (compareMaps(bcMap, acMap)) {
                    Row row = sheet.createRow(rowNum++);
                    int cellNum = 0;
                    for (String header : headers) {
                        String value = getDifferenceValue(header, bcMap, acMap);
                        Cell cell = row.createCell(cellNum++);
                        cell.setCellValue(value);
                    }
                }
            }
        }
    }

    private boolean compareMaps(Map<String, String> map1, Map<String, String> map2) {
        return map1.get("curveId").equals(map2.get("curveId")) &&
                map1.get("tenor").equals(map2.get("tenor"));
    }

    private String getDifferenceValue(String header, Map<String, String> bcMap, Map<String, String> acMap) {
        String[] parts = header.split("(?<=\\D)(?=\\d)");
        String key = parts[0].toLowerCase();
        if (header.endsWith("BC")) {
            return bcMap.getOrDefault(key, "");
        } else if (header.endsWith("AC")) {
            return acMap.getOrDefault(key, "");
        }
        return "";
    }
}
