package com.itemis.kerml2ecore.service.server.service;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import static org.springframework.http.HttpStatus.OK;
import com.itemis.kerml2ecore.SysMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);
    protected final SysMLReader sysMLReader = new SysMLReader();

    public File uploadModel(MultipartFile file) throws IOException {
        // if (file.isEmpty()) {
        //     throw new EmptyFileException("No file was uploaded.");
        // }
        Path tempDir = Files.createTempDirectory("kerml2ecore");

        logger.info("Temporary directory created at: {}", tempDir);

        File zipFile = new File(tempDir.toFile(), file.getOriginalFilename());
        try (OutputStream os = Files.newOutputStream(zipFile.toPath())) {
            os.write(file.getBytes());
        }
        logger.info("Uploaded ZIP file saved to: {}", zipFile.getAbsolutePath());
        var files = new ArrayList<File>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                logger.info("Zip File: {}", zipEntry.getName());
                File extractedFile = new File(tempDir.toFile(), zipEntry.getName());

                // Step 4: Create directories if necessary
                if (zipEntry.isDirectory()) {
                    extractedFile.mkdirs();
                } else {
                    // Step 5: Extract file content
                    // Especially Windows zip might not generate dedicated entry for directories, so 
                    // be prepared
                    if(!extractedFile.getParentFile().exists()) {
                        extractedFile.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    logger.info("Extracted file: {}", extractedFile.getAbsolutePath());
                    files.add(extractedFile);
                }
                zis.closeEntry();
            }
        } catch(Exception e) {
            logger.error("Error while extracting archive "+e.getMessage());
        }

        return sysMLReader.read(tempDir.toString(),"output.ecore");
       
    }

    

}
