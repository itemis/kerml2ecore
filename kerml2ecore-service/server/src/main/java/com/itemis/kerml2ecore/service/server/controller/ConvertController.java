package com.itemis.kerml2ecore.service.server.controller;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.itemis.kerml2ecore.service.server.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;




@Controller
public class ConvertController {

	private static final Logger logger = LoggerFactory.getLogger(ConvertController.class);

	protected final UploadService uploadService;


	@Autowired
	public ConvertController(UploadService uploadService) {
		this.uploadService = uploadService;
		

	}

	

	@PostMapping(value = "/kerml2ecore")
	public @ResponseBody byte[] handleFileUpload(@RequestParam("file") MultipartFile file) {

		try {
			File f = uploadService.uploadModel(file);
			logger.info("Donwnloading file: "+f.getAbsolutePath());
			InputStream in = new FileInputStream(f.getAbsolutePath());
    		return  org.apache.commons.io.IOUtils.toByteArray(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		return null;
	}


}