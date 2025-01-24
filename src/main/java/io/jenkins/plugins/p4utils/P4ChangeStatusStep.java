package io.jenkins.plugins.p4utils;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.server.IOptionsServer;
import hudson.Extension;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class P4ChangeStatusStep extends Step {

    private final String changeList;

    @DataBoundConstructor
    public P4ChangeStatusStep(String changeList) {
        this.changeList = changeList;
    }

    public String getChangeList() {
        return changeList;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return  new P4ChangeStatusStep.Execution(context, changeList);
    }

    private static class Execution extends SynchronousStepExecution<String> {
        private final String changeList;

        protected Execution(@Nonnull StepContext context, String changeList) {
            super(context);
            this.changeList = changeList;
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

                server = P4Utils.connectToPerforce(p4server, p4login, p4password);

                IChangelist changelist = server.getChangelist(Integer.parseInt(changeList));
                return changelist.getStatus().toString();

            }
            catch (Exception e)
            {
                listener.getLogger().println("P4 Change Error: " + e.getMessage());
            }
            finally {
                if (server != null) {
                    try {
                        server.disconnect();
                    } catch (Exception e) {
                        listener.getLogger().println("P4 Change Error: " + e.getMessage());
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
            return "p4change";
        }

        @Override
        public String getDisplayName() {
            return "P4 Change";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {
            req.bindJSON(this, json.getJSONObject("p4change"));
            return true;
        }
    }
}
