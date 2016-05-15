package org.jenkinsci.plugins.scmutils;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CommitersStep extends AbstractStepImpl {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public CommitersStep() {
        super();
    }

    @Override
    public StepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<String> {

        @StepContextParameter
        transient Run build;
        @StepContextParameter
        transient TaskListener taskListener;

        @Override
        protected String run() throws Exception {
            StringBuilder rt = new StringBuilder();
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets;
            if (build instanceof WorkflowRun) {
                changeSets = ((WorkflowRun) build).getChangeSets();
            } else {
                throw new UnsupportedOperationException();
            }
            Set<String> emails = new TreeSet<>();
            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
                for (Object i : changeSet.getItems()) {
                    if (i instanceof ChangeLogSet.Entry) {
                        ChangeLogSet.Entry change = (ChangeLogSet.Entry) i;
                        hudson.model.User u = change.getAuthor();
                        Mailer.UserProperty email = u.getProperty(Mailer.UserProperty.class);
                        if (email != null && email.getAddress() != null && !email.getAddress().isEmpty()) {
                            try {
                                emails.add((new InternetAddress(email.getAddress())).getAddress());
                            } catch (AddressException e) {
                                taskListener.error(e.toString());
                            }
                        }
                    }
                }
            }
            Iterator<String> iterator = emails.iterator();
            while (iterator.hasNext()) {
                rt.append(iterator.next());
                if (iterator.hasNext()) {
                    rt.append(" ");
                }
            }
            return rt.toString();
        }
    }

    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "commiters";
        }

        @Override
        public String getDisplayName() {
            return "Commiters";
        }
    }
}
