package MainPackNLP;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.formdev.flatlaf.FlatLightLaf;

public class MainDashboardUI extends JFrame {

    private FileBrowserUI sourceBrowser, outputBrowser;
    private JPanel statsPanel, chartPanel;
    private DefaultTableModel activityTableModel;

    private static final String SRC_DIR = "src/data/data-test";
    private static final String OUT_DIR = "Map_Out";

    // 🎨 Couleurs modernes
    private final Color primary = new Color(79, 70, 229);
    private final Color success = new Color(16, 185, 129);
    private final Color warning = new Color(249, 115, 22);
    private final Color info = new Color(59, 130, 246);
    private final Color bg = new Color(245, 247, 250);
    private final Color card = Color.WHITE;

    // =========================================================
    public MainDashboardUI() {
        super("Tableau de bord SAFAR - NLP Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(bg);

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}

        add(createHeader(), BorderLayout.NORTH);

        // --- Tabs principales ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.setBackground(Color.WHITE);

        tabs.addTab("Tableau de bord", createDashboardPanel());
        tabs.addTab("Analyse Map1", createJsonAnalysisPanel("Map_Out/Frequence", "تحليل Map1 (الترددات)"));
        tabs.addTab("Analyse Map2", createJsonAnalysisPanel("Map_Out/Stemming", "تحليل Map2 (الجذور)"));
        tabs.addTab("Journal d’activité", createActivityLogTab());

        add(tabs, BorderLayout.CENTER);
        setVisible(true);
        new File(OUT_DIR).mkdirs();
    }

    // =========================================================
    // HEADER
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(card);
        header.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        JLabel title = new JLabel("Analyse de textes arabes - SAFAR");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(17, 24, 39));

        JButton btnMap1 = createButton("Générer Map1", primary);
        JButton btnMap2 = createButton("Générer Map2", success);
        JButton btnMap3 = createButton("Générer Map3 (pas en coure realise)", this.warning);

        btnMap1.addActionListener(e -> generateMap1());
        btnMap2.addActionListener(e -> generateMap2());
        btnMap3.addActionListener(e -> generateMap3());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(btnMap1);
        right.add(btnMap2);
        right.add(btnMap3);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Amiri", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 38));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(color.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }

    // =========================================================
    // DASHBOARD
    private JPanel createDashboardPanel() {
        JPanel center = new JPanel(new BorderLayout(15, 15));
        center.setBackground(bg);
        center.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(700);
        split.setDividerSize(10);
        split.setBorder(null);

        split.setLeftComponent(createSourcePanel());
        split.setRightComponent(createOutputPanel());
        center.add(split, BorderLayout.CENTER);

        JPanel analytics = new JPanel(new BorderLayout(15, 15));
        analytics.setBackground(bg);
        analytics.add(createStatsPanel(), BorderLayout.NORTH);
        analytics.add(createChartPanel(), BorderLayout.CENTER);
        center.add(analytics, BorderLayout.SOUTH);

        return center;
    }

    private JPanel createSourcePanel() {
        JPanel p = createCardPanel("Fichiers Sources", "Sélectionnez les fichiers à analyser", primary);
        sourceBrowser = new FileBrowserUI(SRC_DIR, false ,"Cliquez sur un dossier pour l'ouvrir/fermer Ctrl pour selection multiple","Sélectionnez les fichiers à analyser");
        p.add(sourceBrowser, BorderLayout.CENTER);
        return p;
    }

    private JPanel createOutputPanel() {
        JPanel p = createCardPanel("Résultats générés", "Fichiers Map1 et Map2", success);
        outputBrowser = new FileBrowserUI(OUT_DIR, true ,"","Map1 : Frequence et Map2 : Stemming et Map3 : *****");
        JButton refresh = createButton("Actualiser", success);
        refresh.addActionListener(e -> refreshOutput());
        p.add(refresh, BorderLayout.SOUTH);
        p.add(outputBrowser, BorderLayout.CENTER);
        return p;
    }

    private JPanel createCardPanel(String title, String subtitle, Color lineColor) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(lineColor, 2));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        t.setForeground(lineColor);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(107, 114, 128));

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(Color.WHITE);
        head.add(t);
        head.add(sub);

        card.add(head, BorderLayout.NORTH);
        return card;
    }

    private JPanel createStatsPanel() {
        statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setBackground(bg);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        return statsPanel;
    }

    private JPanel createChartPanel() {
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        return chartPanel;
    }

    private void updateStats(Map<String, Integer> freq, int fileCount) {
        statsPanel.removeAll();
        int total = freq.values().stream().mapToInt(i -> i).sum();
        int unique = freq.size();
        statsPanel.add(createStatCard("Mots totaux", total, primary));
        statsPanel.add(createStatCard("Mots uniques", unique, success));
        statsPanel.add(createStatCard("Fichiers analysés", fileCount, warning));
        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private JPanel createStatCard(String label, int value, Color color) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(color);
        c.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel v = new JLabel(String.valueOf(value), SwingConstants.CENTER);
        v.setForeground(Color.WHITE);
        v.setFont(new Font("Segoe UI", Font.BOLD, 28));
        c.add(l, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        return c;
    }

    // =========================================================
    // JOURNAL D’ACTIVITÉ
    private JPanel createActivityLogTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel header = new JLabel("Journal d’activité");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(primary);
        panel.add(header, BorderLayout.NORTH);

        String[] columns = {"Heure", "Type", "Message"};
        activityTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(activityTableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void log(String msg) {
        String type = msg.startsWith("⚠") ? "Avertissement" :
                msg.startsWith("❌") ? "Erreur" :
                msg.startsWith("✅") ? "Succès" : "Info";
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        if (activityTableModel != null)
            activityTableModel.addRow(new Object[]{time, type, msg});
    }

    // =========================================================
    // MAP 1 / MAP 2
    private void generateMap1() {
        try {
            log("▶ Génération Map1...");
            List<File> files = sourceBrowser.getSelectedFiles();
            if (files.isEmpty()) { log("⚠ Aucun fichier sélectionné !"); return; }
            MapGenerator.generateFrequencyMap(files, SRC_DIR);
            log("✅ Map1 générée !");
            showAnalytics("Map_Out/Frequence");
            refreshOutput();
        } catch (Exception e) { log("❌ Erreur : " + e.getMessage()); }
    }

    private void generateMap2() {
        try {
            log("🌿 Génération Map2...");
            File[] files = new File("Map_Out/Frequence").listFiles((d, n) -> n.endsWith(".json"));
            if (files == null || files.length == 0) { log("⚠ Aucune Map1 trouvée."); return; }
            File latest = Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).get();
            MapGenerator.generateStemmingMap(latest.getAbsolutePath());
            log("✅ Map2 générée !");
            refreshOutput();
        } catch (Exception e) { log("❌ Erreur : " + e.getMessage()); }
    }
    
    private void generateMap3() {
        JOptionPane.showMessageDialog(
            this,
            "La génération de Map3 n’est pas encore disponible.\n\nCette fonctionnalité sera ajoutée dans une prochaine version.",
            "Fonctionnalité non disponible",
            JOptionPane.INFORMATION_MESSAGE
        );
    }


    private void refreshOutput() {
        Container parent = outputBrowser.getParent();
        parent.remove(outputBrowser);
        outputBrowser = new FileBrowserUI(OUT_DIR, true ,"","la Sorties est actualisées");
        parent.add(outputBrowser);
        parent.revalidate();
        parent.repaint();
        log("↻ Sorties actualisées.");
    }

    private void showAnalytics(String folderPath) {
        try {
            File[] files = new File(folderPath).listFiles((d, n) -> n.endsWith(".json"));
            if (files == null || files.length == 0) return;
            File latest = Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).get();
            Map<String, Map<String, Double>> map = new Gson().fromJson(new FileReader(latest), Map.class);
            Map<String, Integer> freq = new HashMap<>();
            map.values().forEach(inner -> inner.forEach((k, v) -> freq.put(k, freq.getOrDefault(k, 0) + v.intValue())));
            updateStats(freq, map.size());
        } catch (Exception ex) { log("Erreur d'analyse : " + ex.getMessage()); }
    }

    // =========================================================
    // ANALYSE JSON MAP1 / MAP2 / MAP3
 // =========================================================
 // ANALYSE JSON MAP1 / MAP2 / MAP3 (version professionnelle)
 private JPanel createJsonAnalysisPanel(String folderPath, String title) {
     JPanel panel = new JPanel(new BorderLayout(15, 15));
     panel.setBackground(bg);
     panel.setBorder(new EmptyBorder(20, 30, 20, 30));
     panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

     // ---- En-tête ----
     JLabel header = new JLabel("🔍 " + title, SwingConstants.RIGHT);
     header.setFont(new Font("Amiri", Font.BOLD, 24));
     header.setForeground(primary);
     panel.add(header, BorderLayout.NORTH);

     // ---- Sélection de fichiers ----
     JComboBox<File> comboFiles = new JComboBox<>();
     comboFiles.setFont(new Font("Amiri", Font.PLAIN, 16));
     comboFiles.setMaximumRowCount(8);
     comboFiles.setBackground(Color.WHITE);
     comboFiles.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
             super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
             if (value instanceof File f) setText("📁 " + f.getName());
             setHorizontalAlignment(SwingConstants.RIGHT);
             return this;
         }
     });

     JButton refreshBtn = createButton("🔄 تحديث", info);
     JButton analyzeBtn = createButton("🔍 تحليل الملف", success);

     JPanel topPanel = new JPanel(new BorderLayout(10, 10));
     topPanel.setBackground(bg);
     topPanel.add(comboFiles, BorderLayout.CENTER);

     JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
     btnPanel.setOpaque(false);
     btnPanel.add(refreshBtn);
     btnPanel.add(analyzeBtn);
     topPanel.add(btnPanel, BorderLayout.SOUTH);

     panel.add(topPanel, BorderLayout.NORTH);

     // ---- Zone de résultats ----
     JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
     resultPanel.setBackground(bg);

     // Cartes statistiques
     JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 15, 15));
     cardsGrid.setBackground(bg);

     JPanel totalPanel = createResultCardPanel("إجمالي الكلمات", "0", "🧮", primary);
     JPanel uniquePanel = createResultCardPanel("الكلمات الفريدة", "0", "✨", success);
     JPanel filePanel = createResultCardPanel("عدد الملفات", "0", "📄", info);

     cardsGrid.add(totalPanel);
     cardsGrid.add(uniquePanel);
     cardsGrid.add(filePanel);
     resultPanel.add(cardsGrid, BorderLayout.NORTH);

     // Panneau TOP 5 clair et professionnel
     JPanel topWordsPanel = new JPanel(new BorderLayout());
     topWordsPanel.setBackground(Color.WHITE);
     topWordsPanel.setBorder(new CompoundBorder(
             new LineBorder(warning, 2, true),
             new EmptyBorder(20, 20, 20, 20)
     ));
     topWordsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

     JLabel topTitle = new JLabel("أكثر 28 كلمات تكراراً", SwingConstants.RIGHT);
     topTitle.setFont(new Font("Amiri", Font.BOLD, 20));
     topTitle.setForeground(warning.darker());
     topWordsPanel.add(topTitle, BorderLayout.NORTH);

     JTextPane topList = new JTextPane();
     topList.setEditable(false);
     topList.setFont(new Font("Amiri", Font.PLAIN, 18));
     topList.setForeground(new Color(40, 40, 40));
     topList.setBackground(Color.WHITE);
     topWordsPanel.add(new JScrollPane(topList), BorderLayout.CENTER);

     resultPanel.add(topWordsPanel, BorderLayout.CENTER);
     panel.add(resultPanel, BorderLayout.CENTER);

     // ---- Rafraîchir fichiers ----
     Runnable refreshFiles = () -> {
         comboFiles.removeAllItems();
         File dir = new File(folderPath);
         File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
         if (files == null || files.length == 0) {
             log("⚠ لا توجد ملفات JSON في: " + folderPath);
             return;
         }
         Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
         for (File f : files) comboFiles.addItem(f);
         log("✅ تم العثور على " + files.length + " ملف JSON");
     };

     refreshBtn.addActionListener(e -> refreshFiles.run());

     // ---- Analyse JSON ----
     analyzeBtn.addActionListener(e -> {
         File selected = (File) comboFiles.getSelectedItem();
         if (selected == null) {
             log("⚠ الرجاء اختيار ملف JSON أولاً");
             return;
         }

         try (FileReader reader = new FileReader(selected)) {
             Object parsed = new Gson().fromJson(reader, Object.class);
             Map<String, Integer> freq = new HashMap<>();

             if (parsed instanceof Map<?, ?> outerMap) {
                 for (var entry : outerMap.entrySet()) {
                     Object val = entry.getValue();
                     if (val instanceof Map<?, ?> inner) {
                         for (var e2 : inner.entrySet()) {
                             if (e2.getValue() instanceof Number n)
                                 freq.put(e2.getKey().toString(),
                                         freq.getOrDefault(e2.getKey().toString(), 0) + n.intValue());
                         }
                     } else if (val instanceof Number n) {
                         freq.put(entry.getKey().toString(), n.intValue());
                     }
                 }
             }

             if (freq.isEmpty()) {
                 log("⚠ Aucun mot trouvé dans le fichier JSON.");
                 topList.setText("❌ لا توجد نتائج في هذا الملف.");
                 return;
             }

             int total = freq.values().stream().mapToInt(i -> i).sum();
             int unique = freq.size();

          // ✅ Top 20 mots répartis sur 4 colonnes de 5 mots chacune
             List<Map.Entry<String, Integer>> top20 = freq.entrySet().stream()
                     .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                     .limit(28)
                     .collect(Collectors.toList());

             // ✅ Mise à jour cartes
             ((JLabel) totalPanel.getClientProperty("valueLabel")).setText(String.valueOf(total));
             ((JLabel) uniquePanel.getClientProperty("valueLabel")).setText(String.valueOf(unique));
             ((JLabel) filePanel.getClientProperty("valueLabel")).setText(String.valueOf(((Map<?, ?>) parsed).size()));

             // ✅ Construction HTML (table 4 colonnes × 5 lignes)
             StringBuilder sb = new StringBuilder();
             sb.append("<table dir='rtl' style='width:100%; border-collapse:collapse; font-family:Amiri; "
                     + "font-size:18px; color:#222; line-height:1.8em; text-align:right;'>");

             for (int row = 0; row < 7; row++) {
                 sb.append("<tr>");
                 for (int col = 0; col < 4; col++) {
                     int index = row + col * 7;
                     if (index < top20.size()) {
                         var e2 = top20.get(index);
                         sb.append("<td style='padding:6px 12px;'>")
                           .append("<span style='color:#555; font-weight:bold;'>").append(index + 1).append(". </span>")
                           .append("<span style='color:#111;'>").append(e2.getKey()).append("</span>")
                           .append(" <span style='color:#d35400; font-weight:bold;'>(").append(e2.getValue()).append(")</span>")
                           .append("</td>");
                     } else {
                         sb.append("<td></td>");
                     }
                 }
                 sb.append("</tr>");
             }
             sb.append("</table>");

             // ✅ Application du contenu dans le JTextPane
             topList.setContentType("text/html");
             topList.setText("<html><body dir='rtl' style='margin:10px;'>"
                     + sb.toString()
                     + "</body></html>");



             resultPanel.revalidate();
             resultPanel.repaint();
             log("✅ تم تحليل الملف بنجاح: " + selected.getName());

         } catch (Exception ex) {
             log("❌ خطأ أثناء التحليل: " + ex.getMessage());
         }
     });

     refreshFiles.run();
     return panel;
 }


//=========================================================
//Carte dynamique (panel) pour affichage clair et mise à jour facile
private JPanel createResultCardPanel(String label, String value, String emoji, Color accent) {
  JPanel card = new JPanel(new BorderLayout(10, 10));
  card.setBackground(Color.WHITE);
  card.setBorder(new CompoundBorder(
          new LineBorder(accent, 2, true),
          new EmptyBorder(20, 20, 20, 20)
  ));
  card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

  JLabel icon = new JLabel(emoji, SwingConstants.CENTER);
  icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
  icon.setForeground(accent);

  JLabel valueLabel = new JLabel(value, SwingConstants.RIGHT);
  valueLabel.setFont(new Font("Amiri", Font.BOLD, 22));
  valueLabel.setForeground(accent.darker());

  JLabel textLabel = new JLabel(label, SwingConstants.RIGHT);
  textLabel.setFont(new Font("Amiri", Font.PLAIN, 15));
  textLabel.setForeground(new Color(50, 50, 50));

  JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 6));
  textPanel.setOpaque(false);
  textPanel.add(valueLabel);
  textPanel.add(textLabel);

  card.add(icon, BorderLayout.WEST);
  card.add(textPanel, BorderLayout.CENTER);

  // ✅ On stocke les composants pour mise à jour facile
  card.putClientProperty("valueLabel", valueLabel);
  card.putClientProperty("textLabel", textLabel);

  return card;
}


    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainDashboardUI::new);
    }
}
