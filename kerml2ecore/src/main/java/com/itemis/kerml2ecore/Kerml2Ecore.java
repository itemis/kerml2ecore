package com.itemis.kerml2ecore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import org.omg.sysml.lang.sysml.Package;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Streams;

import org.omg.sysml.lang.sysml.Class;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.LiteralInfinity;
import org.omg.sysml.lang.sysml.LiteralInteger;
import org.omg.sysml.lang.sysml.LiteralString;
import org.omg.sysml.lang.sysml.MetadataFeature;
import org.omg.sysml.lang.sysml.MultiplicityRange;
import org.omg.sysml.lang.sysml.FeatureValue;
public class Kerml2Ecore {

    private static final Logger logger = Logger.getLogger(Kerml2Ecore.class);

    EcoreFactory factory = EcoreFactory.eINSTANCE;

    public HashBiMap<Element, EClass> classMap = HashBiMap.create();

    public Optional<MetadataFeature> getMetaDataAnnotation(ResourceSet rs) {
        var metaData = Streams.stream(rs.getAllContents()).filter(MetadataFeature.class::isInstance)
        .map(MetadataFeature.class::cast)
        .filter(mf -> mf.getType().get(0).getName().equals("ECoreAnnotation"))
        .findFirst();

        return metaData;
    }

    public void modifyTargetPackage(EPackage target, MetadataFeature metaData) {
        Streams.stream(metaData.eAllContents()).filter(FeatureValue.class::isInstance)
        .map(FeatureValue.class::cast)
        .forEach(x -> {
            var featureName =  ((Feature)x.eContainer()).getName();
            System.out.println("FeatureName:"+featureName);
            var featureValue = ((Feature)x.eContainer()).getOwnedRelationship()
        .stream().filter(FeatureValue.class::isInstance).map(FeatureValue.class::cast).findFirst().get().getValue();
    
            System.out.println("FeatureValue:"+featureValue);

            switch(featureName) {
                case "modelName" : 
                    target.setName(((LiteralString)featureValue).getValue());
                    break;
                case "nsURI" : 
                    target.setNsURI(((LiteralString)featureValue).getValue());
                    break;    
                case "nsPrefix" : 
                    target.setNsPrefix(((LiteralString)featureValue).getValue());
                    break;    
            }

        });
    }

    public EPackage transform(ResourceSet input) {
        var result = factory.createEPackage();
        System.out.println("***");
        var ecao = getMetaDataAnnotation(input);
       
        if(ecao.isPresent()) {

            modifyTargetPackage(result, ecao.get());
            
        }

        Streams.stream(input.getAllContents()).filter(Package.class::isInstance)
                .map(Package.class::cast).forEach(p -> {
               
                
            p.getMember().forEach(member -> {
                if (member instanceof Class) {
                    result.getEClassifiers().add(
                            transformClass((Class) member));
                }
            });

            // p.getMember().forEach(member -> {
            //     if (member instanceof Package) {
            //         result.getESubpackages().add(transform((Package) member));
            //     }
            // });
        });
        return result;
    }

    public EClass transformClass(Class input) {
        var result = factory.createEClass();
        classMap.put(input, result);
        result.setName(input.getName());
        return result;
    }

    public void transformProperties(Class input) {
        input.getOwnedMember().forEach(member -> {
            if (member instanceof Feature) {
                var eClass = classMap.get(input);
                var eProperty = factory.createEAttribute();
                eProperty.setName(member.getName());
                eClass.getEStructuralFeatures().add(eProperty);

                var mu = ((Feature) member).getMultiplicity();
                if (mu instanceof MultiplicityRange mur) {
                    var upb = mur.getUpperBound();

                    switch (upb) {
                        case LiteralInfinity li:
                            eProperty.setUpperBound(-1);
                            break;

                        case LiteralInteger li:
                            eProperty.setUpperBound(li.getValue());
                            break;

                        default:
                            System.err.println("Unknown upper bound");

                            break;
                    }
                }

                switch (((Feature) member).getType().get(0).getName()) {
                    case "String":
                        eProperty.setEType(EcorePackage.eINSTANCE.getEString());
                        break;

                    case "Boolean":
                        eProperty.setEType(EcorePackage.eINSTANCE.getEBoolean());
                        break;

                    default:
                        eProperty.setEType(classMap.get(((Feature) member).getType().get(0)));
                        break;
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public ResourceSet createResources(ResourceSet input) {
        var resourceSet = new ResourceSetImpl();
        var outputRes = resourceSet.createResource(URI.createFileURI("./schnitzel.ecore"));
        var transformedRoot = transform(input);
        outputRes.getContents().add(transformedRoot);

        Streams.stream(input.getAllContents())
                .filter(Class.class::isInstance)
                .map(Class.class::cast).forEach(c -> transformProperties(c));

        try {
            outputRes.save(new HashMap());
            System.out.println("Saved " + outputRes.getURI());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resourceSet;

    }

    public File createFile(ResourceSet input) {
        ResourceSet resourceSet = createResources(input);
        
        return new File("./schnitzel.ecore");


    }
}


