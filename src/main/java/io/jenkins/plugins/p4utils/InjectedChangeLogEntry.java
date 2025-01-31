package io.jenkins.plugins.p4utils;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class InjectedChangeLogEntry extends ChangeLogSet.Entry {
    private final String author;
    private final String message;
    private final String changeListNumber;
    private final List<InjectedChangeLogAffectedFile> files;
    private final long timestamp;

    public InjectedChangeLogEntry(String author, String message, String changeListNumber, long timestamp, List<InjectedChangeLogAffectedFile> files) {
        this.author = author;
        this.message = message;
        this.changeListNumber = changeListNumber;
        this.files = files;
        this.timestamp = timestamp;
    }

    @Override
    public String getMsg() {
        return message;
    }

    @Override
    public User getAuthor() {
        return User.get(author);
    }

    public List<String> getFiles() {
        return files.stream().map(x->x.getPath() + " #" + x.getEditType().getName()).toList();
    }

    public String getDate() {
        return new Date(getTimestamp()).toString();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        return files.stream().map(x->x.getPath()).toList();
    }

    @Override
    public Collection<InjectedChangeLogAffectedFile> getAffectedFiles() {
        return files;
    }

    @Override
    public String getCommitId() {
        return changeListNumber;
    }

}
