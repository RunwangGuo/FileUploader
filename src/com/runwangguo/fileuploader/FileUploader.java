package com.runwangguo.fileuploader;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import com.jcraft.jsch.*;


public class FileUploader extends JFrame {
	private static final String UPLOAD_PATH = "";
    private static final String DEV_HOST = "";
    private static final String TEST_HOST = "";

    private JComboBox<String> dropdown;
    private JTextArea logTextArea;

    public FileUploader() {
        setTitle("File Uploader Designed by Runwang Guo  ~~拖拽后请等待一段时间 不要重复拖拽~~");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建下拉框
        String[] options = {"请选择您要部署的程序", "", "", "", "", "", "", "", ""};
        dropdown = new JComboBox<>(options);
        add(dropdown, BorderLayout.NORTH);

        // 创建拖拽区域
        JPanel dragPanel = new JPanel(new GridLayout(1, 3));
        dragPanel.setBackground(Color.YELLOW);
        dragPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel devPanel = createDragArea("dev", Color.GREEN);
        JPanel testPanel = createDragArea("test", Color.BLUE);
        JPanel devTestPanel = createDragArea("dev&&test", Color.RED);

        dragPanel.add(devPanel);
        dragPanel.add(testPanel);
        dragPanel.add(devTestPanel);

        add(dragPanel, BorderLayout.CENTER);

        // 创建日志文本框
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setBackground(Color.WHITE);
        logTextArea.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // 将滚动面板的首选高度设置为指定值 以此增大文本框占用面积
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 200));
        add(scrollPane, BorderLayout.SOUTH);

        // 设置文本框自动滚动
        DefaultCaret caret = (DefaultCaret) logTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        pack();
        // 设置窗口默认启动大小
        setSize(800,600);
        setLocationRelativeTo(null);
    }

	private JPanel createDragArea(String name, Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(color);
        panel.setBorder(BorderFactory.createTitledBorder(name));
        panel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                logTextArea.append("Notice! ! ! Before you drag and drop, select the item corresponding to the drop-down box\n");
                Transferable transferable = support.getTransferable();
                try {
                    java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        if (file.getName().endsWith(".zip") || file.getName().endsWith(".rar")) {
                            if (name.equals("dev")) {
                            	logTextArea.append("start uploadFileToDev\n");
                                uploadFileToDev(file);
                            } else if (name.equals("test")) {
                            	logTextArea.append("start uploadFileToTest\n");
                                uploadFileToTest(file);
                            } else if (name.equals("dev&&test")) {
                                uploadFileToDev(file);
                                uploadFileToTest(file);
                            }
                        } else {
                        	logTextArea.append("Invalid file format. Only .zip or .rar files are allowed.\n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

        return panel;
    }
	
	
    private void uploadFileToDev(File file) throws JSchException, SftpException, IOException {	
    	// 创建临时目录用于解压缩文件
        File tempDir = Files.createTempDirectory("temp").toFile();
        // 解压缩文件
        unzipFile(file, tempDir);
        // 查找index.html文件所在的文件夹
        String folderPath = null;
        File indexHtmlFile = findIndexHtmlFile(tempDir);
        if (indexHtmlFile != null) {
            folderPath = indexHtmlFile.getParent();
        } 
        
        try {       	       	
            // String folderPath = findIndexHtmlFolder(file);
            if (folderPath != null) {
                String destinationPath = UPLOAD_PATH + dropdown.getSelectedItem();
                sshUploadFolder(folderPath, destinationPath, DEV_HOST);
            } else {
                logTextArea.append("index.html file not found in the compressed file.\n");
                logTextArea.append("Please check that the file you uploaded is correct!\n");
            }
        }
        finally {
        	// 删除临时目录
            deleteDirectory(tempDir);
        	File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
            	logTextArea.append("The temporary folder has not been deleted! \n");
            } else {
            	logTextArea.append("The temp folder has been deleted! \n");
            }
        }
    }


    private void uploadFileToTest(File file) throws JSchException, SftpException, IOException {
    	// 创建临时目录用于解压缩文件
        File tempDir = Files.createTempDirectory("temp").toFile();
        // 解压缩文件
        unzipFile(file, tempDir);
        // 查找index.html文件所在的文件夹
        String folderPath = null;
        File indexHtmlFile = findIndexHtmlFile(tempDir);
        if (indexHtmlFile != null) {
            folderPath = indexHtmlFile.getParent();
        }
        try {
            // String folderPath = findIndexHtmlFolder(file);
            if (folderPath != null) {
                String destinationPath = UPLOAD_PATH + dropdown.getSelectedItem();
                sshUploadFolder(folderPath, destinationPath, TEST_HOST);
            } else {
                logTextArea.append("index.html file not found in the compressed file.\n");
                logTextArea.append("Please check that the file you uploaded is correct!\n");
            }
        } 
        finally {
        	// 删除临时目录
            deleteDirectory(tempDir);
        	File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
            	logTextArea.append("The temporary folder has not been deleted! \n");
            } else {
            	logTextArea.append("The temp folder has been deleted! \n");
            }
        }
    }
    
    private void unzipFile(File file, File destinationDir) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(destinationDir, entry.getName());

            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                entryDestination.getParentFile().mkdirs();
                try (InputStream in = zipFile.getInputStream(entry);
                     OutputStream out = new FileOutputStream(entryDestination)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        }

        zipFile.close();
    }
    
    private File findIndexHtmlFile(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File indexHtmlFile = findIndexHtmlFile(file);
                    if (indexHtmlFile != null) {
                        return indexHtmlFile;
                    }
                } else if (file.getName().equalsIgnoreCase("index.html")) {
                    return file;
                }
            }
        }
        return null;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }


    private void sshUploadFolder(String sourceFolderPath, String destinationFolderPath, String host) throws JSchException, SftpException {
 
    	logTextArea.append("start sshUploadFolder \n");
    	logTextArea.append("The temporary folder is " + sourceFolderPath + "\n");
    	logTextArea.append("The destination path is " + host + ":" + destinationFolderPath + "\n");
        int port = 00;
        String username = "";
        String password = "";

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

        logTextArea.append("Folder uploaded successfully to " + host + destinationFolderPath + "\n");
    }

    private void uploadFolder(ChannelSftp channel, String sourceFolderPath, String destinationFolderPath) throws SftpException {
    	// logTextArea.append("uploadFolder started\n");
    	File sourceFolder = new File(sourceFolderPath);
        if (!sourceFolder.isDirectory()) {
            logTextArea.append("Source path is not a folder\n");
            return;
        }
       
        // channel.mkdir(destinationFolderPath);

        File[] files = sourceFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    channel.put(file.getAbsolutePath(), destinationFolderPath + "/" + file.getName());
                } else if (file.isDirectory()) {
                    uploadFolder(channel, file.getAbsolutePath(), destinationFolderPath + "/" + file.getName());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileUploader fileUploader = new FileUploader();
            fileUploader.setVisible(true);
        });
    }
}
