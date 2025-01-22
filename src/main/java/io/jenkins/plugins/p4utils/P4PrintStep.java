package io.jenkins.plugins.p4utils;

import com.perforce.p4java.server.IOptionsServer;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.StaplerRequest;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

public class P4PrintStep extends Step {

    private final String p4FilePath;
    private boolean toFile;
    private final String localFile;

    @DataBoundConstructor
    public P4PrintStep(String p4FilePath, boolean toFile, String localFile) {
        this.p4FilePath = p4FilePath;
        this.toFile = toFile;
        this.localFile = localFile;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(context, p4FilePath, toFile, localFile);
    }

    public String getP4FilePath() {
        return p4FilePath;
    }

    public boolean isToFile() {
        return toFile;
    }

    public String getLocalFile() {
        return localFile;
    }

    private static class Execution extends SynchronousStepExecution<String> {
        private final String p4FilePath;
        private final boolean toFile;
        private final String localFile;

        protected Execution(@Nonnull StepContext context, String p4FilePath, boolean toFile, String localFile) {
            super(context);
            this.p4FilePath = p4FilePath;
            this.toFile = toFile;
            this.localFile = localFile;
        }

        @Override
        protected String run() {
            IOptionsServer server = null;
            TaskListener listener = null;
            try {
                listener = getContext().get(TaskListener.class);
            }
            catch (Exception e) {}

            try {
                P4PrintGlobalConfiguration globalConfig = P4PrintGlobalConfiguration.get();
                String p4server = globalConfig.getP4server();
                String p4login = globalConfig.getP4login();
                String p4password = globalConfig.getP4password();

                if(toFile) {
                    FilePath workspace = getContext().get(FilePath.class);
                    Computer computer = getContext().get(Computer.class);
                    computer.getChannel().call(new P4PrintToFileCallable(Paths.get(workspace.getRemote(),
                            localFile).toString(), p4FilePath, listener, p4server, p4login,
                            p4password));
                    return "OK!" ;
                }
                else {

                    server = P4Utils.connectToPerforce(p4server, p4login, p4password);
                    InputStream printResult = P4Utils.getP4PrintInputStream(server, p4FilePath);

                    listener.getLogger().println("P4 Print OK!");
                    return P4Utils.convertInputStreamToString(printResult);
                }

            }
            catch (Exception e)
            {
                listener.getLogger().println("P4 Print Error: " + e.getMessage());
            }
            finally {
                if (server != null) {
                    try {
                        server.disconnect();
                    } catch (Exception e) {
                        listener.getLogger().println("P4 Print Error: " + e.getMessage());
                    }
                }
            }
            return "" ;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {


        public DescriptorImpl() {
            load();
        }


        @Override
        public String getFunctionName() {
            return "p4print";
        }

        @Override
        public String getDisplayName() {
            return "P4 Print";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {
            req.bindJSON(this, json.getJSONObject("p4print"));
            return true;
        }
    }
}