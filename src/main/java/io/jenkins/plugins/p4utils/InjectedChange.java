package io.jenkins.plugins.p4utils;

import java.util.List;

public class InjectedChange {
    private String author;
    private String id;
    private String description;
    private List<InjectedChangeLogAffectedFile> files;
    private long timestamp;

    public InjectedChange (String author, String id, String description, List<InjectedChangeLogAffectedFile> files, long timestamp) {
        this.author = author;
        this.id = id;
        this.description = description;
        this.files = files;
        this.timestamp = timestamp;
    }

    public InjectedChange() {}

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<InjectedChangeLogAffectedFile> getFiles() {
        return files;
    }

    public void setFiles(List<InjectedChangeLogAffectedFile> files) {
        this.files = files;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
