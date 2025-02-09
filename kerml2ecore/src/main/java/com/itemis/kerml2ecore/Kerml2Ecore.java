package com.itemis.kerml2ecore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
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

/**
 * This class provides methods to transform a KerML model to an Ecore model.
 * It includes functionality to map KerML elements to Ecore elements and to
 * handle metadata annotations.
 */
public class Kerml2Ecore {

    private static final Logger logger = Logger.getLogger(Kerml2Ecore.class);

    EcoreFactory factory = EcoreFactory.eINSTANCE;

    public HashBiMap<Element, EClass> classMap = HashBiMap.create();

    private static final Set<String> ATTRIBUTE_TYPES = Set.of("String", "Boolean", "Integer", "Real", "UnlimitedNatural", "Date");

    /**
     * Retrieves the metadata annotation from the given ResourceSet.
     *
     * @param rs the ResourceSet to search for metadata annotations
     * @return an Optional containing the MetadataFeature if found, otherwise an empty Optional
     */
    public Optional<MetadataFeature> getMetaDataAnnotation(ResourceSet rs) {
        var metaData = Streams.stream(rs.getAllContents()).filter(MetadataFeature.class::isInstance)
        .map(MetadataFeature.class::cast)
        .filter(mf -> mf.getType().get(0).getName().equals("ECoreAnnotation"))
        .findFirst();

        return metaData;
    }

    /**
     * Modifies the target EPackage based on the provided metadata.
     *
     * @param target the EPackage to modify
     * @param metaData the MetadataFeature containing the modifications
     */
    public void modifyTargetPackage(EPackage target, MetadataFeature metaData) {
        Streams.stream(metaData.eAllContents()).filter(FeatureValue.class::isInstance)
        .map(FeatureValue.class::cast)
        .forEach(x -> {
            var featureName =  ((Feature)x.eContainer()).getName();
            logger.info("FeatureName:" + featureName);
            var featureValue = ((Feature)x.eContainer()).getOwnedRelationship()
        .stream().filter(FeatureValue.class::isInstance).map(FeatureValue.class::cast).findFirst().get().getValue();
    
            logger.info("FeatureValue:" + featureValue);

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

    /**
     * Transforms the given ResourceSet into an EPackage.
     *
     * @param input the ResourceSet to transform
     * @return the resulting EPackage
     */
    public EPackage transform(ResourceSet input) {
        var result = factory.createEPackage();
        logger.info("***");
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

    /**
     * Transforms the given Class into an EClass.
     *
     * @param input the Class to transform
     * @return the resulting EClass
     */
    public EClass transformClass(Class input) {
        var result = factory.createEClass();
        classMap.put(input, result);
        result.setName(input.getName());
        return result;
    }

    /**
     * Transforms the properties of the given Class.
     *
     * @param input the Class whose properties are to be transformed
     */
    public void transformProperties(Class input) {
        input.getOwnedMember().forEach(member -> {
            if (member instanceof Feature mem) {
                var eClass = classMap.get(input);
                var eProperty = shouldBeAttribute(mem) ? factory.createEAttribute() : factory.createEReference();
                eProperty.setName(member.getName());
                eClass.getEStructuralFeatures().add(eProperty);

                setMultiplicity(mem, eProperty);
                setEType(mem, eProperty);
            }
        });
    }

    /**
     * Sets the multiplicity of the given EStructuralFeature based on the provided Feature.
     *
     * @param member the Feature to use for setting the multiplicity
     * @param eProperty the EStructuralFeature to modify
     */
    private void setMultiplicity(Feature member, EStructuralFeature eProperty) {
        var mu = member.getMultiplicity();
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
                    throw new IllegalArgumentException("Unknown upper bound type: " + upb.getClass().getName());
            }
        }
    }

    /**
     * Sets the EType of the given EStructuralFeature based on the provided Feature.
     *
     * @param member the Feature to use for setting the EType
     * @param eProperty the EStructuralFeature to modify
     */
    private void setEType(Feature member, EStructuralFeature eProperty) {
        String typeName = member.getType().get(0).getName();
        if (ATTRIBUTE_TYPES.contains(typeName)) {
            switch (typeName) {
                case "String":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEString());
                    break;
                case "Boolean":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEBoolean());
                    break;
                case "Integer":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEInt());
                    break;
                case "Real":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEDouble());
                    break;
                case "UnlimitedNatural":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEInt());
                    break;
                case "Date":
                    eProperty.setEType(EcorePackage.eINSTANCE.getEDate());
                    break;
            }
        } else {
            eProperty.setEType(classMap.get(member.getType().get(0)));
        }
    }

    /**
     * Determines if the given Feature should be an attribute.
     *
     * @param f the Feature to check
     * @return true if the Feature should be an attribute, false otherwise
     */
    public boolean shouldBeAttribute(Feature f) {
        return ATTRIBUTE_TYPES.contains(f.getType().get(0).getName());
    }

    /**
     * Creates resources from the given ResourceSet and saves them to the specified output file.
     *
     * @param input the ResourceSet to transform
     * @param outputFileName the name of the output file
     * @return the resulting ResourceSet
     */
    @SuppressWarnings("rawtypes")
    public ResourceSet createResources(ResourceSet input, String outputFileName) {
        var resourceSet = new ResourceSetImpl();
        var outputRes = resourceSet.createResource(URI.createFileURI(outputFileName));
        var transformedRoot = transform(input);
        outputRes.getContents().add(transformedRoot);

        Streams.stream(input.getAllContents())
                .filter(Class.class::isInstance)
                .map(Class.class::cast).forEach(c -> transformProperties(c));

        try {
            outputRes.save(new HashMap());
            logger.info("Saved " + outputRes.getURI());
        } catch (IOException e) {
            logger.error("Error saving resource", e);
        }
        return resourceSet;
    }

    /**
     * Creates a file from the given ResourceSet and saves it to the specified output file.
     *
     * @param input the ResourceSet to transform
     * @param outputFileName the name of the output file
     * @return the resulting File
     */
    public File createFile(ResourceSet input, String outputFileName) {
        ResourceSet resourceSet = createResources(input, outputFileName);
        return new File(outputFileName);
    }
}
