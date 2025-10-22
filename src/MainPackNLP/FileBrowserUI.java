package MainPackNLP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class FileBrowserUI extends JPanel {
    private final JList<FileItem> fileList;
    private final DefaultListModel<FileItem> listModel;
    private final java.util.List<String> fullPaths;
    private final Map<String, ImageIcon> iconCache;
    private final Map<String, Boolean> expandedFolders;
    private String strsubtitle ;
    private String strtitle;
    
    // Couleurs modernes
    private static final Color BG_COLOR = new Color(249, 250, 251);
    private static final Color SELECTED_COLOR = new Color(219, 234, 254);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color FOLDER_COUNT_COLOR = new Color(99, 102, 241);

    public FileBrowserUI(String rootPath, boolean isReadOnly , String strsubtitle , String strtitle) {
    	this.strsubtitle = strsubtitle;
    	this.strtitle =  strtitle;
        setLayout(new BorderLayout(0, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fullPaths = new ArrayList<>();
        iconCache = new HashMap<>();
        expandedFolders = new HashMap<>();
        
        // Creer les icones
        createIcons();
        
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new ModernFileRenderer());
        fileList.setBackground(BG_COLOR);
        fileList.setSelectionBackground(SELECTED_COLOR);
        fileList.setSelectionForeground(TEXT_PRIMARY);
        fileList.setFixedCellHeight(40);
        
        // Ajouter listener pour expand/collapse
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = fileList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    FileItem item = listModel.get(index);
                    if (item.isDirectory) {
                        toggleFolder(item);
                    }
                }
            }
        });
        
        loadFilesRecursively(new File(rootPath), "", 0, true);
        
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(listScroll, BorderLayout.CENTER);
        
        // En-tête moderne
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel title = new JLabel(this.strtitle);
        title.setIcon(iconCache.get("folder"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        title.setIconTextGap(10);
        
        JLabel subtitle = new JLabel(this.strsubtitle);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_SECONDARY);
        
        JPanel titlePanel = new JPanel(new BorderLayout(0, 5));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.CENTER);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Compteur de fichiers
        JLabel countLabel = new JLabel(getFileCount() + " fichiers");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        countLabel.setForeground(TEXT_SECONDARY);
        headerPanel.add(countLabel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
    }
    
    private void toggleFolder(FileItem folder) {
        String path = folder.fullPath;
        boolean wasExpanded = expandedFolders.getOrDefault(path, true);
        expandedFolders.put(path, !wasExpanded);
        
        // Reconstruire la liste
        listModel.clear();
        fullPaths.clear();
        loadFilesRecursively(new File(folder.rootPath), "", 0, true);
        fileList.revalidate();
        fileList.repaint();
    }
    
    private void createIcons() {
        iconCache.put("folder", createFolderIcon(new Color(251, 191, 36)));
        iconCache.put("folder_open", createFolderOpenIcon(new Color(251, 191, 36)));
        iconCache.put("folder_closed", createFolderClosedIcon(new Color(251, 191, 36)));
        iconCache.put("txt", createFileIcon(new Color(59, 130, 246)));
        iconCache.put("pdf", createFileIcon(new Color(239, 68, 68)));
        iconCache.put("doc", createFileIcon(new Color(37, 99, 235)));
        iconCache.put("xml", createFileIcon(new Color(234, 88, 12)));
        iconCache.put("json", createFileIcon(new Color(16, 185, 129)));
        iconCache.put("default", createFileIcon(new Color(107, 114, 128)));
    }
    
    private ImageIcon createFolderIcon(Color color) {
        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(color);
        int[] xPoints = {2, 10, 10, 22, 22, 2};
        int[] yPoints = {8, 8, 6, 6, 20, 20};
        g2.fillPolygon(xPoints, yPoints, 6);
        
        g2.setColor(color.darker());
        g2.fillRoundRect(2, 8, 20, 12, 2, 2);
        
        g2.dispose();
        return new ImageIcon(img);
    }
    
    private ImageIcon createFolderOpenIcon(Color color) {
        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(color);
        g2.fillRoundRect(2, 6, 20, 14, 2, 2);
        g2.setColor(color.darker());
        g2.fillRoundRect(4, 10, 16, 8, 2, 2);
        
        // Triangle vers le bas
        g2.setColor(TEXT_PRIMARY);
        int[] xp = {6, 10, 14};
        int[] yp = {9, 13, 9};
        g2.fillPolygon(xp, yp, 3);
        
        g2.dispose();
        return new ImageIcon(img);
    }
    
    private ImageIcon createFolderClosedIcon(Color color) {
        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(color);
        int[] xPoints = {2, 10, 10, 22, 22, 2};
        int[] yPoints = {8, 8, 6, 6, 20, 20};
        g2.fillPolygon(xPoints, yPoints, 6);
        
        g2.setColor(color.darker());
        g2.fillRoundRect(2, 8, 20, 12, 2, 2);
        
        // Triangle vers la droite
        g2.setColor(TEXT_PRIMARY);
        int[] xp = {7, 13, 7};
        int[] yp = {10, 14, 18};
        g2.fillPolygon(xp, yp, 3);
        
        g2.dispose();
        return new ImageIcon(img);
    }
    
    private ImageIcon createFileIcon(Color color) {
        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(color);
        g2.fillRoundRect(5, 3, 14, 18, 2, 2);
        
        g2.setColor(color.darker());
        int[] xPoints = {19, 19, 15};
        int[] yPoints = {3, 7, 3};
        g2.fillPolygon(xPoints, yPoints, 3);
        
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(8, 9, 8, 1, 1, 1);
        g2.fillRoundRect(8, 12, 6, 1, 1, 1);
        g2.fillRoundRect(8, 15, 7, 1, 1, 1);
        
        g2.dispose();
        return new ImageIcon(img);
    }

    private int loadFilesRecursively(File folder, String prefix, int depth, boolean parentExpanded) {
        File[] files = folder.listFiles();
        if (files == null) return 0;
        
        Arrays.sort(files, Comparator.comparing(File::getName));
        
        int fileCount = 0;
        
        // Compter les fichiers dans ce dossier
        for (File f : files) {
            if (f.isFile()) {
                fileCount++;
            } else if (f.isDirectory()) {
                fileCount += countFilesInFolder(f);
            }
        }
        
        for (File f : files) {
            if (f.isDirectory()) {
                boolean isExpanded = expandedFolders.getOrDefault(f.getAbsolutePath(), false);
                int subFileCount = countFilesInFolder(f);
                
                if (parentExpanded) {
                    listModel.addElement(new FileItem(
                        f.getName(), 
                        true, 
                        0, 
                        depth, 
                        isExpanded,
                        f.getAbsolutePath(),
                        folder.getAbsolutePath(),
                        subFileCount
                    ));
                }
                
                if (isExpanded && parentExpanded) {
                    loadFilesRecursively(f, prefix + "  ", depth + 1, true);
                }
            } else {
                if (parentExpanded) {
                    long size = f.length();
                    listModel.addElement(new FileItem(
                        f.getName(), 
                        false, 
                        size, 
                        depth, 
                        false,
                        f.getAbsolutePath(),
                        folder.getAbsolutePath(),
                        0
                    ));
                    fullPaths.add(f.getAbsolutePath());
                }
            }
        }
        
        return fileCount;
    }
    
    private int countFilesInFolder(File folder) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    count++;
                } else if (f.isDirectory()) {
                    count += countFilesInFolder(f);
                }
            }
        }
        return count;
    }

    private int getFileCount() {
        return fullPaths.size();
    }

    public List<File> getSelectedFiles() {
        List<File> selected = new ArrayList<>();
        int[] indices = fileList.getSelectedIndices();
        int fileCounter = 0;
        
        for (int i = 0; i < listModel.size(); i++) {
            FileItem item = listModel.get(i);
            if (!item.isDirectory) {
                if (isSelectedIndex(i, indices)) {
                    selected.add(new File(fullPaths.get(fileCounter)));
                }
                fileCounter++;
            }
        }
        return selected;
    }

    private boolean isSelectedIndex(int index, int[] selectedIndices) {
        for (int sel : selectedIndices) {
            if (sel == index) return true;
        }
        return false;
    }

    // Classe interne pour representer un element de fichier
    static class FileItem {
        String name;
        boolean isDirectory;
        long size;
        int depth;
        boolean isExpanded;
        String fullPath;
        String rootPath;
        int fileCount;
        
        FileItem(String name, boolean isDirectory, long size, int depth, boolean isExpanded, 
                 String fullPath, String rootPath, int fileCount) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.depth = depth;
            this.isExpanded = isExpanded;
            this.fullPath = fullPath;
            this.rootPath = rootPath;
            this.fileCount = fileCount;
        }
    }

    // Renderer personnalise moderne
    class ModernFileRenderer extends JPanel implements ListCellRenderer<FileItem> {
        private JLabel iconLabel;
        private JLabel nameLabel;
        private JLabel sizeLabel;
        private JLabel countLabel;
        
        ModernFileRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 12));
            
            iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(24, 24));
            
            nameLabel = new JLabel();
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            
            countLabel = new JLabel();
            countLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            countLabel.setForeground(FOLDER_COUNT_COLOR);
            
            sizeLabel = new JLabel();
            sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            sizeLabel.setForeground(TEXT_SECONDARY);
            
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            leftPanel.setOpaque(false);
            leftPanel.add(iconLabel);
            leftPanel.add(nameLabel);
            leftPanel.add(countLabel);
            
            add(leftPanel, BorderLayout.WEST);
            add(sizeLabel, BorderLayout.EAST);
        }
        
        @Override
        public Component getListCellRendererComponent(
                JList<? extends FileItem> list, FileItem item, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            // Ajouter l'indentation
            int leftMargin = 8 + (item.depth * 20);
            setBorder(BorderFactory.createEmptyBorder(6, leftMargin, 6, 12));
            
            if (item.isDirectory) {
                // Choisir l'icone selon l'etat
                if (item.isExpanded) {
                    iconLabel.setIcon(iconCache.get("folder_open"));
                } else {
                    iconLabel.setIcon(iconCache.get("folder_closed"));
                }
                
                nameLabel.setText(item.name);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                nameLabel.setForeground(TEXT_PRIMARY);
                
                // Afficher le nombre de fichiers
                if (item.fileCount > 0) {
                    countLabel.setText("(" + item.fileCount + ")");
                } else {
                    countLabel.setText("");
                }
                
                sizeLabel.setText("");
            } else {
                // Icone selon le type de fichier
                String fileName = item.name.toLowerCase();
                ImageIcon icon;
                if (fileName.endsWith(".txt")) {
                    icon = iconCache.get("txt");
                } else if (fileName.endsWith(".pdf")) {
                    icon = iconCache.get("pdf");
                } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                    icon = iconCache.get("doc");
                } else if (fileName.endsWith(".xml")) {
                    icon = iconCache.get("xml");
                } else if (fileName.endsWith(".json")) {
                    icon = iconCache.get("json");
                } else {
                    icon = iconCache.get("default");
                }
                iconLabel.setIcon(icon);
                
                nameLabel.setText(item.name);
                nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                nameLabel.setForeground(TEXT_PRIMARY);
                countLabel.setText("");
                sizeLabel.setText(formatFileSize(item.size));
            }
            
            if (isSelected) {
                setBackground(SELECTED_COLOR);
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : BG_COLOR);
            }
            
            return this;
        }
        
        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}