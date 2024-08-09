package com.xmlReader.xml_comparer.CommandLineRunner;

import com.xmlReader.xml_comparer.Service.XmlComparerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommandLineAppRunner implements CommandLineRunner {

    private final XmlComparerService xmlComparerService;

    public CommandLineAppRunner(XmlComparerService xmlComparerService) {
        this.xmlComparerService = xmlComparerService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java -jar xml-comparer.jar <file1.xml> <file2.xml> <output.xlsx>");
            System.exit(1);
        }

        String filePath1 = args[0];
        String filePath2 = args[1];
        String outputPath = args[2];

        xmlComparerService.compareAndGenerateReport(filePath1, filePath2, outputPath);
    }
}
