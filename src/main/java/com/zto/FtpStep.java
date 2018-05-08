package com.zto;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by liming on 2017-5-6 006.
 */
public class FtpStep extends AbstractStepImpl {

    private final String commitid;
    private final String fileName;

    @DataBoundConstructor
    public FtpStep(String commitid,String fileName){
        this.commitid = commitid;
        this.fileName = fileName;
    }

    public String getCommitid() {
        return commitid;
    }

    public String getFileName() {
        return fileName;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(FtpExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "uploadFtp";
        }

        @Override
        public String getDisplayName() {
            return "upload a local file to ftp server in pipeline step";
        }
    }
}
