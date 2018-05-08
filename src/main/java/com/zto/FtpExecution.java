package com.zto;

import com.google.common.base.Preconditions;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.ArtifactManager;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liming on 2017-5-6 006.
 */
public class FtpExecution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient Launcher launcher;

    @Inject
    private transient FtpStep step;

    @Override
    protected Boolean run() throws Exception {


        artifact();
        return true;
    }

    private void artifact() throws IOException, InterruptedException {
        EnvVars envVars = getContext().get(hudson.EnvVars.class);
        PrintStream logger = listener.getLogger();
        String ftpServerIp = envVars.get("ftpServerIp");
        String ftpUsername = envVars.get("ftpUsername");
        String ftpPassword = envVars.get("ftpPassword");
        String ftpPort = envVars.get("ftpPort");
        String ftpRootPath = envVars.get("ftpRootPath");
        String jobName = envVars.get("JOB_NAME");
        checkEnvVars(ftpServerIp);
        checkEnvVars(ftpUsername);
        checkEnvVars(ftpPassword);
        checkEnvVars(ftpPort);

        ftpServerIp = ftpServerIp.trim();
        ftpUsername = ftpUsername.trim();
        ftpPassword = ftpPassword.trim();
        ftpPort = ftpPort.trim();

        listener.getLogger().println(String.format("保存构建：%s", step.getFileName()));
        UploadToFtpInSlave uploadToFtpInSlave = new UploadToFtpInSlave(listener, build.getRootDir().getAbsolutePath(), step.getCommitid(), step.getFileName(), ftpRootPath, jobName, ftpServerIp, ftpPort, ftpUsername, ftpPassword);
        Map<String, String> files = ws.act(uploadToFtpInSlave);
        ArtifactManager artifactManager = build.pickArtifactManager();
        artifactManager.archive(ws, getContext().get(Launcher.class), new BuildListenerAdapter(getContext().get(TaskListener.class)), files);
    }

    private void checkEnvVars(String vars) {
        Preconditions.checkArgument(StringUtils.isNotBlank(vars), "%s can not be empty , please set the global environment variables in jenkins", vars);
    }

    private static final class UploadToFtpInSlave extends MasterToSlaveFileCallable<Map<String,String>> {
        private static final long serialVersionUID = 1;
        private final String fileName;
        private final TaskListener listener;
        private final String commitId;
        private File archiveFile;
        private File md5File;
        private String rootDir;
        private String ftpRootPath;
        private String jobName;
        private String ftpServerIp;
        private String ftpPort;
        private String ftpUsername;
        private String ftpPassword;

        UploadToFtpInSlave(TaskListener listener, String rootDir, String commitId, String fileName, String ftpRootPath, String jobName, String ftpServerIp, String ftpPort, String ftpUsername, String ftpPassword) {
            this.listener = listener;
            this.rootDir = rootDir;
            this.commitId = commitId;
            this.fileName = fileName;
            this.ftpRootPath = ftpRootPath;
            this.jobName = jobName;
            this.ftpServerIp = ftpServerIp;
            this.ftpPort = ftpPort;
            this.ftpUsername = ftpUsername;
            this.ftpPassword = ftpPassword;
        }

        @Override
        public Map<String, String> invoke(File basedir, VirtualChannel channel) throws IOException, InterruptedException {

            try {
                Boolean upload = upload(basedir);
                if(!upload){
                    throw new Throwable("文件上传失败");
                }
            } catch (Throwable throwable) {
                throw new IOException(throwable);
            }

            archiveFile.renameTo(new File(archiveFile.getAbsoluteFile()+".bak"));
            md5File.renameTo(new File(md5File.getAbsoluteFile()+".bak"));
            archiveFile.createNewFile();
            md5File.createNewFile();
            Map<String, String> r = new HashMap<>();
            r.put(archiveFile.getName(), fileName);
            r.put(md5File.getName(), fileName+".md5");
            return r;
        }



        private File createMD5File(File file) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(file);
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            fileInputStream.close();
            File md5File = new File(file.getAbsoluteFile() + ".md5");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(md5File));
            bufferedWriter.write(md5Hex + " " + file.getName());
            bufferedWriter.close();
            return md5File;
        }


        public Boolean upload(File basedir) throws Throwable {

            this.archiveFile = new File(basedir.getAbsolutePath()  + File.separator + fileName);
            String ftpDir = ftpRootPath + File.separator + jobName + File.separator + commitId;
            boolean upload = false;
            try {
                upload = FtpUtil.upload(ftpServerIp, ftpPort, ftpUsername, ftpPassword, archiveFile, ftpDir);
                this.md5File = createMD5File(archiveFile);
                upload = upload & FtpUtil.upload(ftpServerIp,ftpPort,ftpUsername,ftpPassword,md5File,ftpDir);
            } catch (IOException e) {
                throw e;
            }
            if (upload) {
                listener.getLogger().println(String.format("upload file %s to %s success", archiveFile.getAbsolutePath(), ftpDir));
            } else {
                listener.getLogger().println(String.format("upload file %s to %s fail", archiveFile.getAbsolutePath(), ftpDir));
            }
            return upload;
        }
    }


}
