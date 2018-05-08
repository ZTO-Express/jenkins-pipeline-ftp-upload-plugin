package com.zto;

import com.google.common.base.Preconditions;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by liming on 2017-5-6 006.
 */
public class FtpUtil {

    public static boolean upload(String ftpServerIp,String port, String ftpUsername, String ftpPassword, File localFile, String ftpDir) throws IOException {
        FTPClient ftp = new FTPClient();

        ftp.connect(ftpServerIp,Integer.valueOf(port));
        Preconditions.checkState(FTPReply.isPositiveCompletion(ftp.getReplyCode()), ftp.getReplyString());
        ftp.login(ftpUsername, ftpPassword);
        Preconditions.checkState(FTPReply.isPositiveCompletion(ftp.getReplyCode()), ftp.getReplyString());
        ftp.enterLocalPassiveMode();
        createDirecroty(ftpDir, ftp);

        ftp.setFileType(ftp.BINARY_FILE_TYPE);
        FileInputStream fileInputStream = new FileInputStream(localFile);
        boolean b = ftp.storeFile(localFile.getName(), fileInputStream);
        Preconditions.checkState(FTPReply.isPositiveCompletion(ftp.getReplyCode()), ftp.getReplyString());

        fileInputStream.close();
        ftp.logout();
        ftp.disconnect();
        return b;
    }

    public static boolean createDirecroty(String remoteDir,FTPClient ftpClient) throws IOException {
        List<String> pathNames = new ArrayList<>();
        File remoteDirFile = new File(remoteDir);
        while(remoteDirFile != null){
            pathNames.add(remoteDirFile.getName());
            remoteDirFile = remoteDirFile.getParentFile();
        }
        Collections.reverse(pathNames);
        for (String pathName : pathNames) {
            String[] files = ftpClient.listNames();
            if(files == null || Stream.of(files).noneMatch(pathName::equals)){
                ftpClient.makeDirectory(pathName);
            }
            ftpClient.changeWorkingDirectory(pathName);
        }
        Preconditions.checkState(FTPReply.isPositiveCompletion(ftpClient.getReplyCode()), ftpClient.getReplyString());
        return true;
    }

}
