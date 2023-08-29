package com.runwangguo.fileuploader;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;


public class FileUploaderUI extends JFrame {

    static JComboBox<String> dropdown;
    static JTextArea logTextArea;
    static JProgressBar progressBar;
    

    public FileUploaderUI() {
        setTitle("File Uploader Designed by Runwang Guo  ~~拖拽后请等待一段时间 不要重复拖拽~~");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建下拉框
        String[] options = ConfigReader.getStringArray("projectList");
        String[] dropdownOptions = new String[options.length + 1];
        dropdownOptions[0] = "请选择您要部署的程序";
        System.arraycopy(options, 0, dropdownOptions, 1, options.length);
        dropdown = new JComboBox<>(dropdownOptions);
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
        //add(dragPanel, BorderLayout.CENTER);
        
        // 创建进度条并初始化
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(Color.PINK); // 设置进度条颜色为粉红色
        
        
        // 避免布局重叠
        // 创建一个父容器，将拖拽区域和进度条放在其中
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(dragPanel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);        
        
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
        
        // 检查配置文件是否存在
        File configFile = new File("config.yml");
        if (!configFile.exists()) {
            String warningMessage = "配置文件不存在，请联系管理员生成配置文件。";
            JOptionPane.showMessageDialog(this, warningMessage, "警告", JOptionPane.WARNING_MESSAGE);
            System.exit(0); // Exit the application
        }
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
                            	new Thread(() -> {
                                    try {
                                        Uploader.uploadFileToDev(file);
                                        logTextArea.append("uploadFileToDev completed\n");
                                    } catch (IOException | JSchException | SftpException e) {
                                        logTextArea.append("Error uploading file to dev: " + e.getMessage() + "\n");
                                    }
                                }).start();
                            	// Uploader.uploadFileToDev(file);
                            } else if (name.equals("test")) {
                            	logTextArea.append("start uploadFileToTest\n");
                            	new Thread(() -> {
                                    try {
                                        Uploader.uploadFileToTest(file);
                                        logTextArea.append("uploadFileToTest completed\n");
                                    } catch (IOException | JSchException | SftpException e) {
                                        logTextArea.append("Error uploading file to test: " + e.getMessage() + "\n");
                                    }
                                }).start();
                            } else if (name.equals("dev&&test")) {
                            	logTextArea.append("start uploadFileToDev\n");
                                new Thread(() -> {
                                    try {
                                        Uploader.uploadFileToDev(file);
                                        logTextArea.append("uploadFileToDev completed\n");
                                    } catch (IOException | JSchException | SftpException e) {
                                        logTextArea.append("Error uploading file to dev: " + e.getMessage() + "\n");
                                    }
                                }).start();
                                logTextArea.append("start uploadFileToTest\n");
                                new Thread(() -> {
                                    try {
                                        Uploader.uploadFileToTest(file);
                                        logTextArea.append("uploadFileToTest completed\n");
                                    } catch (IOException | JSchException | SftpException e) {
                                        logTextArea.append("Error uploading file to test: " + e.getMessage() + "\n");
                                    }
                                }).start();
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
	

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	FileUploaderUI FileUploaderUI = new FileUploaderUI();
        	FileUploaderUI.setVisible(true);
        });
    }
}
