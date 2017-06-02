package com.cerner.jwala.service.springboot.impl;

import com.cerner.jwala.common.domain.model.group.Group;
import com.cerner.jwala.common.domain.model.media.Media;
import com.cerner.jwala.common.domain.model.springboot.SpringBootApp;
import com.cerner.jwala.common.properties.ApplicationProperties;
import com.cerner.jwala.common.properties.PropertyKeys;
import com.cerner.jwala.dao.MediaDao;
import com.cerner.jwala.dao.SpringBootAppDao;
import com.cerner.jwala.persistence.jpa.domain.JpaMedia;
import com.cerner.jwala.persistence.jpa.domain.JpaSpringBootApp;
import com.cerner.jwala.service.binarydistribution.BinaryDistributionService;
import com.cerner.jwala.service.repository.RepositoryService;
import com.cerner.jwala.service.resource.ResourceContentGeneratorService;
import com.cerner.jwala.service.resource.impl.ResourceGeneratorType;
import com.cerner.jwala.service.springboot.SpringBootService;
import com.cerner.jwala.service.springboot.SpringBootServiceException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created on 6/1/2017.
 */
@Service
public class SpringBootServiceImpl implements SpringBootService {

    @Autowired
    private SpringBootAppDao springBootAppDao;

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    @Qualifier("mediaRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    ResourceContentGeneratorService resourceContentGeneratorService;

    @Autowired
    private BinaryDistributionService binaryDistributionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootServiceImpl.class);

    @Override
    @Transactional
    public JpaSpringBootApp controlSpringBoot(String name, String command, String hostname) {
        LOGGER.info("Run command {} for Spring Boot app {}", command, name);
        if (binaryDistributionService.runCommand(hostname, "net " + command + " " + name)) {
            LOGGER.info("Successfully called {} on {}", command, name);
            return springBootAppDao.find(name);
        } else {
            String errMsg = MessageFormat.format("Failed to run command {0} on Spring Boot {1} on host {2}", command, name, hostname);
            LOGGER.error(errMsg);
            throw new SpringBootServiceException(errMsg);
        }
    }

    @Override
    @Transactional
    public JpaSpringBootApp generateAndDeploy(String name) throws FileNotFoundException {
        LOGGER.info("Start generate and deploy of Spring Boot app {}", name);

        final JpaSpringBootApp springBootApp = springBootAppDao.find(name);

        // create the generated dir
        String appGeneratedDir = prepareStagingDir(springBootApp);
        LOGGER.info("Created the generated dir {}", appGeneratedDir);

        // create the XML file
        createSpringBootXml(springBootApp, appGeneratedDir);

        // create the Spring Boot app jar
        createSpringBootAppJar(springBootApp, appGeneratedDir);

        // create the Spring Boot exe
        createSpringBootExe(springBootApp, appGeneratedDir);

        // deploy the JDK binary
        deploySpringBootArtifacts(springBootApp, appGeneratedDir);

        return springBootApp;
    }

    private void createJarFromGit(String gitUrl, String name, String appGeneratedDir) {
        LOGGER.info("The gitUri {}", gitUrl);
        File gitDestFile = new File(appGeneratedDir + "/clone");
        try {
            LOGGER.info("The name {}", name);
            Git git = Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(gitDestFile)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        //  do a mvn build
        InvocationRequest request = new DefaultInvocationRequest();
        LOGGER.info("first");
        // request.getBaseDirectory();
        request.setPomFile(new File(gitDestFile.getAbsolutePath() + "/pom.xml"));
        LOGGER.info("second");
        request.setGoals(Arrays.asList("clean", "package"));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(System.getenv("M2_HOME")));
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            String errMsg = MessageFormat.format("Failed to build maven project from {0}", gitUrl);
            LOGGER.error(errMsg, e);
            throw new SpringBootServiceException(errMsg);
        }

        // your directory
        File[] matchingFiles = new File(gitDestFile + "/target").listFiles((dir, name1) -> name1.endsWith(".jar"));
        String appFilePath = "";
        if (matchingFiles.length == 1) {
            appFilePath = matchingFiles[0].getAbsolutePath();
        } else {
            String errMsg = MessageFormat.format("Expecting only 1 file for the Spring boot jar from Git URL after running maven build: {0}", gitUrl);
            LOGGER.error(errMsg);
            throw new SpringBootServiceException(errMsg);
        }

        // copy the jar
        try {
            final File srcFile = new File(appFilePath);
            final File destFile = new File(appGeneratedDir + "/" + name + ".jar");
            LOGGER.info("Copying {} to destination {}", srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Failed to copy {0} to jar file", appFilePath);
            LOGGER.error(errMsg, e);
            throw new SpringBootServiceException(errMsg);
        }
    }

    private void deleteExistingService(String hostname, String name) {
        binaryDistributionService.runCommandIgnoreFailure(hostname, "net stop " + name);
        binaryDistributionService.runCommandIgnoreFailure(hostname, "sc delete " + name);
    }

    private void installSpringBootApp(String hostname, JpaSpringBootApp springBootApp) {
        final String name = springBootApp.getName();
        LOGGER.info("Install the app {} as a windows service", name);
        String appDestDir = getSpringBootDestDir(name);
        String exePath = appDestDir + "/" + name + ".exe";
        binaryDistributionService.runCommand(hostname, exePath + " install");
    }

    private void deploySpringBootArtifacts(JpaSpringBootApp springBootApp, String appGeneratedDir) {
        final String name = springBootApp.getName();
        LOGGER.info("Distribute the artifacts for the Spring Boot app {}", name);
        String hostNames = springBootApp.getHostNames();
        for (String hostname : Arrays.asList(hostNames.split("\\s*,\\s*"))) {
            deleteExistingService(hostname, name);

            LOGGER.info("Distribute the JDK");
            binaryDistributionService.distributeMedia(name, hostname, new Group[]{}, new ModelMapper().map(springBootApp.getJdkMedia(), Media.class));

            String appDestDir = getSpringBootDestDir(name);
            LOGGER.info("Create the parent directory {}", appDestDir);
            if (binaryDistributionService.remoteFileCheck(hostname, appDestDir)) {
                binaryDistributionService.backupFile(hostname, appDestDir);
            }
            binaryDistributionService.remoteCreateDirectory(hostname, appDestDir);

            LOGGER.info("Copy the spring boot artifactors from the data/generated directory {}", appGeneratedDir);
            final String xmlFileName = name + ".xml";
            final String jarFileName = name + ".jar";
            final String exeFileName = name + ".exe";
            binaryDistributionService.remoteSecureCopyFile(hostname, appGeneratedDir + "/" + xmlFileName, appDestDir + "/" + xmlFileName);
            binaryDistributionService.remoteSecureCopyFile(hostname, appGeneratedDir + "/" + jarFileName, appDestDir + "/" + jarFileName);
            binaryDistributionService.remoteSecureCopyFile(hostname, appGeneratedDir + "/" + exeFileName, appDestDir + "/" + exeFileName);

            // install the app as a windows service
            installSpringBootApp(hostname, springBootApp);
        }
    }

    private String getSpringBootDestDir(String name) {
        String dataDir = ApplicationProperties.getRequired(PropertyKeys.REMOTE_JAWALA_DATA_DIR);
        return dataDir + "/" + name;
    }

    private void createSpringBootExe(JpaSpringBootApp springBootApp, String appGeneratedDir) {
        LOGGER.info("Create Spring Boot .exe for {}", springBootApp.getName());

        try {
            File appExe = new File(ApplicationProperties.get(PropertyKeys.ROGUE_WINDOWS_EXE_TEMPLATE));
            final File destFile = new File(appGeneratedDir + "/" + springBootApp.getName() + ".exe");
            LOGGER.info("Copying {} to destination {}", appExe.getAbsolutePath(), destFile.getAbsolutePath());
            FileUtils.copyFile(appExe, destFile);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Failed to create Spring Boot exe for {0}", springBootApp.getName());
            LOGGER.error(errMsg, e);
            throw new SpringBootServiceException(errMsg);
        }
    }

    private void createSpringBootAppJar(JpaSpringBootApp springBootApp, String appGeneratedDir) {
        LOGGER.info("Create jar for {} in {}", springBootApp.getName(), appGeneratedDir);

        if (null != springBootApp.getGitHubLink() && !springBootApp.getGitHubLink().isEmpty()) {
            createJarFromGit(springBootApp.getGitHubLink(), springBootApp.getName(), appGeneratedDir);
        } else {
            String appFilePath = springBootApp.getArchiveFile();
            try {
                final File srcFile = new File(appFilePath);
                final File destFile = new File(appGeneratedDir + "/" + springBootApp.getName() + ".jar");
                LOGGER.info("Copying {} to destination {}", srcFile.getAbsolutePath(), destFile.getAbsolutePath());
                FileUtils.copyFile(srcFile, destFile);
            } catch (IOException e) {
                String errMsg = MessageFormat.format("Failed to copy {0} to jar file", springBootApp.getArchiveFile());
                LOGGER.error(errMsg, e);
                throw new SpringBootServiceException(errMsg);
            }
        }
    }

    private String prepareStagingDir(JpaSpringBootApp springBootApp) {
        String generatedDir = ApplicationProperties.get(PropertyKeys.PATHS_GENERATED_RESOURCE_DIR);
        File springBootAppGeneratedDir = new File(generatedDir + "/" + springBootApp.getName());

        try {
            if (springBootAppGeneratedDir.exists()) {
                File backupDir = new File(springBootAppGeneratedDir + "_" + System.currentTimeMillis());
                FileUtils.moveDirectory(springBootAppGeneratedDir, backupDir);
            }

            FileUtils.forceMkdir(springBootAppGeneratedDir);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Failed to make directory {0}", springBootAppGeneratedDir.getAbsolutePath());
            LOGGER.error(errMsg, e);
            throw new SpringBootServiceException(errMsg);
        }

        return springBootAppGeneratedDir.getAbsolutePath();
    }

    private void createSpringBootXml(JpaSpringBootApp springBootApp, String appGeneratedDir) throws FileNotFoundException {
        LOGGER.info("Creating XML for {}", springBootApp.getName());

        InputStream templateData = null;
        try {
            templateData = new FileInputStream(new File(ApplicationProperties.getRequired(PropertyKeys.ROGUE_WINDOWS_XML_TEMPLATE)));
            Scanner scanner = new Scanner(templateData).useDelimiter("\\A");
            String springBootXmlTemplateContent = scanner.hasNext() ? scanner.next() : "";

            String templateContent = resourceContentGeneratorService.generateContent("spring-boot.xml.tpl", springBootXmlTemplateContent, null, new ModelMapper().map(springBootApp, SpringBootApp.class), ResourceGeneratorType.TEMPLATE);
            LOGGER.info(templateContent);

            File springBootXml = new File(appGeneratedDir + "/" + springBootApp.getName() + ".xml");
            try {
                LOGGER.info("Writing XML content to {}", springBootXml.getAbsolutePath());
                FileUtils.writeStringToFile(springBootXml, templateContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                String errMsg = MessageFormat.format("Failed to write XML content to {0}.xml", springBootApp.getName());
                LOGGER.error(errMsg, e);
                throw new SpringBootServiceException(errMsg);
            }

        } finally {
            IOUtils.closeQuietly(templateData);
        }
    }

    @Override
    @Transactional
    public JpaSpringBootApp createSpringBoot(Map<String, Object> springBootDataMap, Map<String, Object> springBootFileDataMap) {
        LOGGER.info("Create Spring Boot service create spring boot data map {} and file data map {}", springBootDataMap, springBootFileDataMap);

        final JpaSpringBootApp springBootApp = new JpaSpringBootApp();
        springBootApp.setName((String) springBootDataMap.get("name"));
        springBootApp.setHostNames((String) springBootDataMap.get("hostNames"));
        springBootApp.setJdkMedia((JpaMedia) springBootDataMap.get("jdkMedia"));

        try {
            springBootAppDao.find(springBootApp.getName());
            final String msg = MessageFormat.format("Spring Boot already exists with name {0}", springBootApp.getName());
            LOGGER.error(msg);
            throw new SpringBootServiceException(msg);
        } catch (NoResultException e) {
            LOGGER.debug("No Spring Boot name conflict, ignoring not found exception for creating Spring Boot app ", e);
        }

        final String gitHubLink = (String) springBootDataMap.get("gitHubLink");
        if (null != gitHubLink && !gitHubLink.isEmpty()) {
            springBootApp.setGitHubLink(gitHubLink);
        } else {
            // filename can be the full path or just the name that is why we need to convert it to Paths
            // to extract the base name e.g. c:/jdk.zip -> jdk.zip or jdk.zip -> jdk.zip
            final String filename = Paths.get((String) springBootFileDataMap.get("filename")).getFileName().toString();
            final String uploadedFilePath = repositoryService.upload(filename, (BufferedInputStream) springBootFileDataMap.get("content"));
            springBootApp.setArchiveFile(uploadedFilePath);
            springBootApp.setArchiveFilename(filename);
        }

        return springBootAppDao.create(springBootApp);
    }

    @Override
    @Transactional
    public JpaSpringBootApp update(JpaSpringBootApp springBootApp) {
        LOGGER.info("Update Spring Boot service {}", springBootApp);

        final JpaSpringBootApp tempSpringBootApp = springBootAppDao.findById(springBootApp.getId());
        springBootApp.setArchiveFile(tempSpringBootApp.getArchiveFile());
        springBootApp.setArchiveFilename(tempSpringBootApp.getArchiveFilename());
        springBootApp.setJdkMedia(mediaDao.findById(springBootApp.getJdkMedia().getId()));

        return springBootAppDao.update(springBootApp);
    }

    @Override
    public void remove(String name) {
        LOGGER.info("Spring Boot service remove {}", name);
        springBootAppDao.remove(springBootAppDao.find(name));
    }

    @Override
    @Transactional
    public JpaSpringBootApp find(String name) {
        LOGGER.info("Spring Boot find service {}", name);
        return springBootAppDao.find(name);
    }

    @Override
    public List<JpaSpringBootApp> findAll() {
        return springBootAppDao.findAll();
    }
}
