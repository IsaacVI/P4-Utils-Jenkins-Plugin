package io.jenkins.plugins.p4utils;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.nio.file.Paths;

@ExportedBean
public class P4Print extends SCM {

    private final String p4FilePath;
    private final String localFile;

    @Exported
    public String getP4FilePath() {
        return p4FilePath;
    }

    @Exported
    public String getLocalFile() {
        return localFile;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?,?> project, Launcher launcher,
                                                   FilePath workspace, TaskListener listener,
                                                   SCMRevisionState _baseline) throws IOException, InterruptedException
    {
        return new PollingResult(PollingResult.Change.SIGNIFICANT);
    }

    @Override
    public void checkout(Run<?,?> build, Launcher launcher, FilePath workspace, TaskListener listener,
                         File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException
    {
        Computer computer = workspace.toComputer();
        P4PrintGlobalConfiguration configuration = P4PrintGlobalConfiguration.get();
        computer.getChannel().call(new P4PrintToFileCallable(Paths.get(workspace.getRemote(), localFile).toString(),
                p4FilePath, listener, configuration.p4server, configuration.p4login, configuration.p4password));
    }



    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?,?> build, FilePath workspace, Launcher launcher,
                                                   TaskListener listener) throws IOException, InterruptedException
    {
        return null;
    }

    @DataBoundConstructor
    public P4Print(String p4FilePath, String localFile) {
        this.p4FilePath = p4FilePath;
        this.localFile = localFile;
    }

    @java.lang.Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeLog();
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor implements PersistentDescriptor {

        public DescriptorImpl() {
            super(P4Print.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "P4Print";
        }

        public String getTfExecutable() {
            return "p4p";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {
            req.bindJSON(this, json.getJSONObject("p4print"));
            return true;
        }

        @Override
        public boolean isApplicable(final Job project) {
            return true;
        }
    }
}
