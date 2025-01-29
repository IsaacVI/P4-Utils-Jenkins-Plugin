package io.jenkins.plugins.p4utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.*;
import hudson.tasks.*;
import io.jenkins.cli.shaded.org.apache.commons.io.FileUtils;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class InjectP4ChangeLog extends Builder implements SimpleBuildStep {

    private final String p4client;
    private final String changesRange;
    private final String limitChanges;

    @DataBoundConstructor
    public InjectP4ChangeLog(String p4client, String changesRange, String limitChanges) {
        this.p4client = p4client;
        this.changesRange = changesRange;
        this.limitChanges = limitChanges;
    }

    public String getP4client() {
        return p4client;
    }
    public String getChangesRange() {
        return changesRange;
    }
    public String getLimitChanges() {
        return limitChanges;
    }


    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {

        if(run instanceof WorkflowRun ab) {

            IOptionsServer server = null;
            String file = null;

            try {

                List<InjectedChange> changelists = new ArrayList<>();
                P4PrintGlobalConfiguration configuration = P4PrintGlobalConfiguration.get();
                server = P4Utils.connectToPerforce(configuration.getP4server(), configuration.getP4login(), configuration.getP4password());
                if(p4client!=null && StringUtils.isNotBlank(p4client)) {
                    server.setCurrentClient(server.getClient(p4client));
                }
                GetChangelistsOptions options = new GetChangelistsOptions()
                        .setType(IChangelist.Type.SUBMITTED)
                        .setLongDesc(true);

                if(limitChanges!=null && StringUtils.isNotBlank(limitChanges) && StringUtils.isNumeric(limitChanges)) {
                    options.setMaxMostRecent(Integer.parseInt(limitChanges));
                }

                List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(changesRange);

                var changes =server.getChangelists(fileSpecs,options );
                for (var c : changes){
                    var files = server.getChangelistFiles(c.getId());

                    changelists.add(new InjectedChange(c.getUsername(),
                            String.valueOf(c.getId()),
                            c.getDescription().replace("\n", " "),
                            files.stream().map(x->x.getDepotPathString()).toList(),
                            c.getDate().getTime()));
                }
                ObjectMapper objectMapper = new ObjectMapper();
                file = outputChangeLog(objectMapper.writeValueAsString(changelists), ab);
            }
            catch (Exception e)
            {
                listener.getLogger().println("Inject Changes: " + e.getMessage());
            }

            finally {
                if (server != null) {
                    try {
                        server.disconnect();
                    } catch (Exception e) {
                        listener.getLogger().println("Inject Changes: " + e.getMessage());
                    }
                }
            }

            org.jenkinsci.plugins.workflow.job.WorkflowRun.SCMListenerImpl scmListenerImpl = new org.jenkinsci.plugins.workflow.job.WorkflowRun.SCMListenerImpl();
            try {
                scmListenerImpl.onCheckout(run, new SCM() {
                    @Override
                    public ChangeLogParser createChangeLogParser() {
                        return new InjectedChangeLogParser();
                    }
                }, workspace, listener, new File(file), null);
            } catch (Exception e) {

                listener.getLogger().println("Inject Changes:" + e.getMessage());
            }
        }
    }

    private String outputChangeLog(String text, WorkflowRun run) throws IOException {
        String buildDir = run.getArtifactsDir().getPath().replace("archive", "");
        String changelogPath = buildDir + "changelog" + UUID.randomUUID().toString();
        File f = new File(changelogPath);
        FileUtils.writeStringToFile(f, text);
        return changelogPath;
    }

    @Symbol("injectP4ChangeLog")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Add custom changelog entry";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

}

