package com.cerner.jwala.common.domain.model.springboot;

import com.cerner.jwala.common.domain.model.media.Media;

/**
 * Created on 6/1/2017.
 */
public class SpringBootApp {

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHostnames() {
        return hostnames;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

    public Media getJdkMedia() {
        return jdkMedia;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHostnames(String hostnames) {
        this.hostnames = hostnames;
    }

    public void setArchiveFileName(String archiveFileName) {
        this.archiveFileName = archiveFileName;
    }

    public void setJdkMedia(Media jdkMedia) {
        this.jdkMedia = jdkMedia;
    }

    private Long id;
    private String name;
    private String hostnames;
    private String archiveFileName;
    private String archiveFile;
    private Media jdkMedia;
    private String gitHunLink;

    public SpringBootApp() {

    }

    public SpringBootApp(Long id, String name, String hostnames, String archiveFileName, String archiveFile, Media jdkMedia, String gitHubLink) {
        this.id = id;
        this.name = name;
        this.hostnames = hostnames;
        this.archiveFile = archiveFile;
        this.archiveFileName = archiveFileName;
        this.jdkMedia = jdkMedia;
        this.gitHunLink = gitHubLink;
    }

    public String getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(String archiveFile) {
        this.archiveFile = archiveFile;
    }

    public String getGitHunLink() {
        return gitHunLink;
    }

    public void setGitHunLink(String gitHunLink) {
        this.gitHunLink = gitHunLink;
    }
}
