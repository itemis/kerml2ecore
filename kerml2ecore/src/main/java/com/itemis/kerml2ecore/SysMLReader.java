package com.itemis.kerml2ecore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        SysMLReader sreader = new SysMLReader();

        sreader.read(args[0]);
    }

    public File read(String args) {
        Injector injector = new KerMLStandaloneSetup().createInjectorAndDoEMFRegistration();
        SysMLPackage registerMe = SysMLPackage.eINSTANCE;
        System.out.println(registerMe);
        injector.injectMembers(this);

        logger.info("Loading resources from "+args);
        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
        try {
            // Files.list(Paths.get(args)).forEach(x -> logger.info(x.getFileName()));
            Files.walk(Paths.get(args)).filter(p -> p.getFileName().toString().endsWith(".kerml"))
            
            .map(p -> p.toAbsolutePath().normalize())
            .forEach( p -> {
                logger.info("Loading "+p.toString());
                Resource scalarResource = resourceSet.getResource(URI.createFileURI(p.toString()), true);
            });
        } catch (IOException e) {
            logger.error("Fatal error while loading");
            e.printStackTrace();
        }
         
        var pack = Streams.stream(resourceSet.getAllContents())
                .filter(Package.class::isInstance)
                .map(Package.class::cast)
                .findFirst().get();

        var transformer = new Kerml2Ecore();
        return transformer.createFile(resourceSet);
        // Streams.stream(resource.getAllContents()).forEach(System.out::println);

    }
}
