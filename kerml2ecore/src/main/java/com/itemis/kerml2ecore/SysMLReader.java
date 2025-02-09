package com.itemis.kerml2ecore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.omg.kerml.xtext.KerMLStandaloneSetup;
import org.omg.sysml.lang.sysml.Package;
import org.omg.sysml.lang.sysml.SysMLPackage;

import com.google.common.collect.Streams;
import com.google.inject.Injector;

public class SysMLReader {

    private static final Logger logger = Logger.getLogger(SysMLReader.class);

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("i", "input", true, "Input directory");
        options.addOption("o", "output", true, "Output file");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error("Error parsing command line options", e);
            formatter.printHelp("SysMLReader", options);
            return;
        }

        String inputDir = cmd.getOptionValue("input");
        String outputFileName = cmd.getOptionValue("output");

        if (inputDir == null || outputFileName == null) {
            logger.error("Usage: SysMLReader -i <input_directory> -o <output_file>");
            formatter.printHelp("SysMLReader", options);
            return;
        }

        SysMLReader sreader = new SysMLReader();
        sreader.read(inputDir, outputFileName);
    }

    public File read(String inputDir, String outputFileName) {
        Injector injector = new KerMLStandaloneSetup().createInjectorAndDoEMFRegistration();
        SysMLPackage registerMe = SysMLPackage.eINSTANCE;
        System.out.println(registerMe);
        injector.injectMembers(this);

        logger.info("Loading resources from " + inputDir);
        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
        try {
            Files.walk(Paths.get(inputDir)).filter(p -> p.getFileName().toString().endsWith(".kerml"))
            .map(p -> p.toAbsolutePath().normalize())
            .forEach(p -> {
                logger.info("Loading " + p.toString());
                Resource scalarResource = resourceSet.getResource(URI.createFileURI(p.toString()), true);
            });
        } catch (IOException e) {
            logger.error("Fatal error while loading", e);
        }
         
        var pack = Streams.stream(resourceSet.getAllContents())
                .filter(Package.class::isInstance)
                .map(Package.class::cast)
                .findFirst().get();

        var transformer = new Kerml2Ecore();
        return transformer.createFile(resourceSet, outputFileName);
        // Streams.stream(resource.getAllContents()).forEach(System.out::println);

    }
}
