package io.jenkins.plugins.p4utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.perforce.p4java.core.file.FileAction;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;


public class InjectedChangeLogAffectedFile implements ChangeLogSet.AffectedFile {

    private String path;
    private int editAction;

    public InjectedChangeLogAffectedFile() {
    }

    public InjectedChangeLogAffectedFile(String path, FileAction fileAction) {
        this.path = path;
        SetFileAction(fileAction);
    }

    public InjectedChangeLogAffectedFile(String path, EditType editType) {
        this.path = path;
        SetEditType(editType);
    }

    public InjectedChangeLogAffectedFile(String path, int editType) {
        this.path = path;
        this.editAction = editType;
    }

    public void SetPath(String path) {
        this.path = path;
    }

    public void SetEditAction(int editAction) {
        this.editAction = editAction;
    }

    @JsonIgnore
    public void SetEditType(EditType editType) {
        if (editType.equals(EditType.ADD)) {
            this.editAction = 1;
        } else if (editType.equals(EditType.DELETE)) {
            this.editAction = 2;
        } else {
            this.editAction = 0;
        }
    }

    @JsonIgnore
    public void SetFileAction(FileAction editType) {
        switch (editType)
        {
            case ADD:
            case ADDED:
                this.editAction = 1;
                break;
            case DELETE:
            case DELETED:
            case PURGE:
                this.editAction = 2;
                break;
            default:
                this.editAction = 0;
                break;

        }
    }

    @Override
    public String getPath() {
        return path;
    }

    public int getEditAction() {
        return editAction;
    }

    @JsonIgnore
    @Override
    public EditType getEditType() {
        switch (editAction){
            case 1:
                return EditType.ADD;
            case 2:
                return EditType.DELETE;
            default:
                return EditType.EDIT;
        }
    }


    @JsonIgnore
    public FileAction getFileAction() {
        switch (editAction){
            case 1:
                return FileAction.ADD;
            case 2:
                return FileAction.DELETE;
            default:
                return FileAction.EDIT;
        }
    }
}
