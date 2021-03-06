package com.cerner.jwala.ws.rest.v1.service.springboot.impl;

import com.cerner.jwala.persistence.jpa.domain.JpaSpringBootApp;
import com.cerner.jwala.service.media.MediaService;
import com.cerner.jwala.service.springboot.SpringBootService;
import com.cerner.jwala.ws.rest.v1.response.ResponseBuilder;
import com.cerner.jwala.ws.rest.v1.service.springboot.SpringBootServiceRest;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 6/1/2017.
 */
@Service
public class SpringBootServiceRestImpl implements SpringBootServiceRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootServiceRestImpl.class);

    @Autowired
    SpringBootService springBootService;

    @Autowired
    MediaService mediaService;

    @Override
    public Response controlSpringBoot(String name, String command, String hostname) {
        LOGGER.info("Control Spring Boot {} with command {} to host {}", name, command, hostname);
        return ResponseBuilder.ok(springBootService.controlSpringBoot(name, command, hostname));
    }

    @Override
    public Response generateAndDeploy(String name) {
        LOGGER.info("Generate and deploy Spring Boot {}", name);
        try {
            return ResponseBuilder.ok(springBootService.generateAndDeploy(name));
        } catch (FileNotFoundException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @Override
    public Response createSpringBoot(List<Attachment> attachments) {
        LOGGER.info("Create Spring Boot {}", attachments);

        if (attachments == null || attachments.isEmpty()) {
            LOGGER.error("Expected non-empty attachments. Returning without creating Media.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final Map<String, Object> springBootDataMap = new HashMap<>();
        final Map<String, Object> springBootFileDataMap = new HashMap<>();

        attachments.forEach(attachment -> {
            try {
                // when using the REST API from Chef a null attachment gets added somewhere between the execute_rest and here
                if (attachment.getHeaders().size() < 2) {
                    return;
                }
                final String fieldName = attachment.getDataHandler().getName();
                if (attachment.getHeader("Content-Type") == null) {
                    if (fieldName.equalsIgnoreCase("jdkMedia")) {
                        String mediaId = IOUtils.toString(attachment.getDataHandler().getInputStream());
                        springBootDataMap.put(fieldName, mediaService.find(new Long(mediaId)));
                    } else {
                        springBootDataMap.put(fieldName,
                                IOUtils.toString(attachment.getDataHandler().getInputStream(), Charset.defaultCharset()));
                    }
                } else {
                    springBootFileDataMap.put("filename", fieldName);
                    springBootFileDataMap.put("content", new BufferedInputStream(attachment.getDataHandler().getInputStream()));
                }
            } catch (final IOException e) {
                LOGGER.error("Failed to retrieve attachments!", e);
                Response.status(Response.Status.BAD_REQUEST).build();
            }
        });

        return ResponseBuilder.created(springBootService.createSpringBoot(springBootDataMap, springBootFileDataMap));
    }

    @Override
    public Response updateSpringBoot(JpaSpringBootApp springBootApp) {
        LOGGER.info("Update Spring Boot {}", springBootApp);
        return ResponseBuilder.ok(springBootService.update(springBootApp));
    }

    @Override
    public Response removeSpringBoot(String name) {
        LOGGER.info("Remove Spring Boot {}", name);
        springBootService.remove(name);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Override
    public Response findSpringBoot(String springBootAppName) {
        LOGGER.info("Get Spring Boot {}", springBootAppName);
        return ResponseBuilder.ok(springBootService.find(springBootAppName));
    }

    @Override
    public Response findSpringBootApps() {
        return ResponseBuilder.ok(springBootService.findAll());
    }

    @Override
    public Response findSpringBootApp(Long id) {
        return  ResponseBuilder.ok(springBootService.find(id));
    }

    @Override
    public Response getUrlResponse(final String url) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (final IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
