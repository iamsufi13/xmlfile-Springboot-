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

            System.out.println("Data AC: " + dataAC);
            System.out.println("Data BC: " + dataBC);

            Workbook workbook = new XSSFWorkbook();
            createSheet(workbook, "Only in BC", findOnlyInFirstList(dataBC, dataAC));
            createSheet(workbook, "Only in AC", findOnlyInFirstList(dataAC, dataBC));
            createDifferenceSheet(workbook, "Difference between BC and AC", dataBC, dataAC);

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
                return node.getTextContent().trim();
            }
        }
        return "";
    }

    private List<Map<String, String>> findOnlyInFirstList(List<Map<String, String>> list1, List<Map<String, String>> list2) {
        List<Map<String, String>> onlyInFirstList = new ArrayList<>();
        for (Map<String, String> map1 : list1) {
            boolean matchFound = false;
            for (Map<String, String> map2 : list2) {
                if (map1.equals(map2)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                onlyInFirstList.add(map1);
            }
        }
        return onlyInFirstList;
    }

    private void createSheet(Workbook workbook, String sheetName, List<Map<String, String>> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = 0;
        if (!data.isEmpty()) {
            Row headerRow = sheet.createRow(rowNum++);
            Set<String> headers = data.get(0).keySet();
            int cellNum = 0;
            for (String header : headers) {
                headerRow.createCell(cellNum++).setCellValue(header);
            }

            for (Map<String, String> rowMap : data) {
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (String header : headers) {
                    Cell cell = row.createCell(cellNum++);
                    cell.setCellValue(rowMap.getOrDefault(header, ""));
                }
            }
        } else {
            System.out.println("No data found for sheet: " + sheetName);
        }
    }

    private void createDifferenceSheet(Workbook workbook, String sheetName, List<Map<String, String>> dataBC, List<Map<String, String>> dataAC) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = { "issuerNameBC", "issuerNameAC", "curveIdBC", "curveIdAC", "ccyBC", "ccyAC", "runningBC", "runningAC", "unitBC", "unitAC", "curvePathBC", "curvePathAC", "issuerTypeBC", "issuerTypeAC", "tenorBC", "tenorAC", "spreadBC", "spreadAC", "volBC", "volAC", "origSprdBC", "origSprdAC", "feeBC", "feeAC" };
        int cellNum = 0;
        for (String header : headers) {
            headerRow.createCell(cellNum++).setCellValue(header);
        }

        for (Map<String, String> bcMap : dataBC) {
            boolean matchFound = false;
            for (Map<String, String> acMap : dataAC) {
                if (compareMaps(bcMap, acMap)) {
                    matchFound = true;
                    Row row = sheet.createRow(rowNum++);
                    cellNum = 0;
                    for (String header : headers) {
                        String value = getDifferenceValue(header, bcMap, acMap);
                        Cell cell = row.createCell(cellNum++);
                        cell.setCellValue(value);
                    }
                    break; // Assuming a one-to-one match, break the loop once a match is found
                }
            }
            if (!matchFound) {
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (String header : headers) {
                    String value = getDifferenceValue(header, bcMap, null);
                    Cell cell = row.createCell(cellNum++);
                    cell.setCellValue(value);
                }
            }
        }

        for (Map<String, String> acMap : dataAC) {
            boolean matchFound = false;
            for (Map<String, String> bcMap : dataBC) {
                if (compareMaps(bcMap, acMap)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (String header : headers) {
                    String value = getDifferenceValue(header, null, acMap);
                    Cell cell = row.createCell(cellNum++);
                    cell.setCellValue(value);
                }
            }
        }
    }

    private boolean compareMaps(Map<String, String> map1, Map<String, String> map2) {
        // Adjust this logic based on the criteria for matching entries
        return map1.getOrDefault("issuerName", "").equals(map2.getOrDefault("issuerName", "")) &&
                map1.getOrDefault("curveId", "").equals(map2.getOrDefault("curveId", ""));
    }

    private String getDifferenceValue(String header, Map<String, String> mapBC, Map<String, String> mapAC) {
        String valueBC = (mapBC != null) ? mapBC.getOrDefault(header, "") : "";
        String valueAC = (mapAC != null) ? mapAC.getOrDefault(header, "") : "";
        return (valueBC.equals(valueAC) || valueBC.isEmpty() || valueAC.isEmpty()) ? valueBC : valueAC;
    }
}
