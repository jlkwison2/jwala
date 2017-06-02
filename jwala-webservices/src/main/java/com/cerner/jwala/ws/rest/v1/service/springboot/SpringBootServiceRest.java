package com.cerner.jwala.ws.rest.v1.service.springboot;

import com.cerner.jwala.persistence.jpa.domain.JpaSpringBootApp;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created on 6/1/2017.
 */
@Path("/springboot")
@Produces(MediaType.APPLICATION_JSON)
public interface SpringBootServiceRest {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Response createSpringBoot(final List<Attachment> attachments);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateSpringBoot(JpaSpringBootApp springBoot);

    @DELETE
    @Path("/{springBootName}")
    Response removeSpringBoot(@PathParam("springBootName") String name);

    @GET
    @Path("/{springBootAppName}")
    Response findSpringBoot(@PathParam("springBootAppName") String springBootAppName);

    @PUT
    @Path("/control/{springBootName}/{command}/{hostname}")
    Response controlSpringBoot(@PathParam("springBootName") String name, @PathParam("command") String command, @PathParam("hostname") String hostname);

    @PUT
    @Path("/generate/{springBootName}")
    Response generateAndDeploy(@PathParam("springBootName") String name);

    @GET
    Response findSpringBootApps();

    @GET
    @Path("/app/{id}")
    Response findSpringBootApp(@PathParam("id") Long id);

    @GET
    @Path("/url")
    Response getUrlResponse(@QueryParam("val") String url);
}
