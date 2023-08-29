package com.runwangguo.fileuploader;
import java.io.*;
import java.nio.file.Files;
import com.jcraft.jsch.*;

public class Uploader {
	private static final String UPLOAD_PATH = ConfigReader.getStringValue("uploadPath");
    private static final String DEV_HOST = ConfigReader.getStringValue("devHost");
    private static final String TEST_HOST = ConfigReader.getStringValue("testHost");


    public static void uploadFileToDev(File file) throws JSchException, SftpException, IOException {	
    	// 创建临时目录用于解压缩文件
        File tempDir = Files.createTempDirectory("temp").toFile();
        // 解压缩文件
        FileUtils.unzipFile(file, tempDir);
        // 查找index.html文件所在的文件夹
        String folderPath = null;
        File indexHtmlFile = FileUtils.findIndexHtmlFile(tempDir);
        if (indexHtmlFile != null) {
            folderPath = indexHtmlFile.getParent();
        } 
        
        try {       	       	
            // String folderPath = findIndexHtmlFolder(file);
            if (folderPath != null) {
                String destinationPath = UPLOAD_PATH + FileUploaderUI.dropdown.getSelectedItem();
                sshUploadFolder(folderPath, destinationPath, DEV_HOST);
            } else {
            	FileUploaderUI.logTextArea.append("index.html file not found in the compressed file.\n");
            	FileUploaderUI.logTextArea.append("Please check that the file you uploaded is correct!\n");
            }
        }
        finally {
        	// 删除临时目录
        	FileUtils.deleteDirectory(tempDir);
        	File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
            	FileUploaderUI.logTextArea.append("The temporary folder has not been deleted! \n");
            } else {
            	FileUploaderUI.logTextArea.append("The temp folder has been deleted! \n");
            }
        }
    }

    public static void uploadFileToTest(File file) throws JSchException, SftpException, IOException {
    	// 创建临时目录用于解压缩文件
        File tempDir = Files.createTempDirectory("temp").toFile();
        // 解压缩文件
        FileUtils.unzipFile(file, tempDir);
        // 查找index.html文件所在的文件夹
        String folderPath = null;
        File indexHtmlFile = FileUtils.findIndexHtmlFile(tempDir);
        if (indexHtmlFile != null) {
            folderPath = indexHtmlFile.getParent();
        }
        try {
            // String folderPath = findIndexHtmlFolder(file);
            if (folderPath != null) {
                String destinationPath = UPLOAD_PATH + FileUploaderUI.dropdown.getSelectedItem();
                sshUploadFolder(folderPath, destinationPath, TEST_HOST);
            } else {
            	FileUploaderUI.logTextArea.append("index.html file not found in the compressed file.\n");
            	FileUploaderUI.logTextArea.append("Please check that the file you uploaded is correct!\n");
            }
        } 
        finally {
        	// 删除临时目录
        	FileUtils.deleteDirectory(tempDir);
        	File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
            	FileUploaderUI.logTextArea.append("The temporary folder has not been deleted! \n");
            } else {
            	FileUploaderUI.logTextArea.append("The temp folder has been deleted! \n");
            }
        }
    }
    
    public static void uploadFolder(ChannelSftp channel, String sourceFolderPath, String destinationFolderPath) throws SftpException {
    	// logTextArea.append("uploadFolder started\n");
    	File sourceFolder = new File(sourceFolderPath);
        if (!sourceFolder.isDirectory()) {
        	FileUploaderUI.logTextArea.append("Source path is not a folder\n");
            return;
        }
       
        // channel.mkdir(destinationFolderPath);
        File[] files = sourceFolder.listFiles();
        if (files != null) {
        	int totalFiles = files.length;
        	FileUploaderUI.progressBar.setMaximum(totalFiles);
            int uploadedFiles = 0;	
            for (File file : files) {
                if (file.isFile()) {
                    channel.put(file.getAbsolutePath(), destinationFolderPath + "/" + file.getName());
                    uploadedFiles++;
                    FileUploaderUI.progressBar.setValue(uploadedFiles);
                } else if (file.isDirectory()) {
                    uploadFolder(channel, file.getAbsolutePath(), destinationFolderPath + "/" + file.getName());
                }
            }
        }
    }
    
    public static void sshUploadFolder(String sourceFolderPath, String destinationFolderPath, String host) throws JSchException, SftpException {
    	 
    	FileUploaderUI.logTextArea.append("start sshUploadFolder \n");
    	FileUploaderUI.logTextArea.append("The temporary folder is " + sourceFolderPath + "\n");
    	FileUploaderUI.logTextArea.append("The destination path is " + host + ":" + destinationFolderPath + "\n");
    	
        int port = ConfigReader.getIntegerValue("port");
        String username = ConfigReader.getStringValue("username");
        String password = ConfigReader.getStringValue("password");

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        uploadFolder(channel, sourceFolderPath, destinationFolderPath);

        channel.disconnect();
        session.disconnect();

        FileUploaderUI.logTextArea.append("Folder uploaded successfully to " + host + destinationFolderPath + "\n");
    }
}
