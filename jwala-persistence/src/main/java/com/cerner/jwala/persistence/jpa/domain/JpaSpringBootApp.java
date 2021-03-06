package com.cerner.jwala.persistence.jpa.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import javax.persistence.*;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Jlkwison on 6/1/2017
 */
@Entity(name = "springBootApp")
@NamedQueries({@NamedQuery(name = JpaSpringBootApp.QUERY_FIND_BY_NAME, query = "SELECT s FROM springBootApp s WHERE lower(s.name) = lower(:name)")})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class JpaSpringBootApp  extends AbstractEntity<JpaSpringBootApp> {

    public static final String QUERY_FIND_BY_NAME = "QUERY_SPRING_BOOT_APP_FIND_BY_NAME";
    public static final String PARAM_NAME = "name";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Size(min = 2, max = 200, message = "{app.name.length.msg}")
    private String name;

    private String hostNames;

    private String archiveFilename;

    private String archiveFile;

    private String gitHubLink;

    @OneToOne (targetEntity = JpaMedia.class)
    @JsonDeserialize(using = JpaMediaDeserializer.class)
    private JpaMedia jdkMedia;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostNames() {
        return hostNames;
    }

    public void setHostNames(String hostName) {
        this.hostNames = hostName;
    }

    public String getArchiveFilename() {
        return archiveFilename;
    }

    public void setArchiveFilename(String archiveFilename) {
        this.archiveFilename = archiveFilename;
    }

    public JpaMedia getJdkMedia() {
        return jdkMedia;
    }

    public void setJdkMedia(JpaMedia jdkMedia) {
        this.jdkMedia = jdkMedia;
    }

    public String getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(String archiveFile) {
        this.archiveFile = archiveFile;
    }

    public String getGitHubLink() {
        return gitHubLink;
    }

    public void setGitHubLink(String gitHubLink) {
        this.gitHubLink = gitHubLink;
    }
}
