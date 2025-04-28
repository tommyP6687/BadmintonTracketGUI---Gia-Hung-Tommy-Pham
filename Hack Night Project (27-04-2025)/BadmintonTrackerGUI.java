import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class BadmintonTrackerGUI extends JFrame {
    static final String DB_URL = "jdbc:sqlite:badminton.db";
    JTable table;
    DefaultTableModel model;
    JComboBox<String> filterComboBox;
    JComboBox<String> statComboBox;
    JTextField smashesInField, smashesOutField, dropsInField, dropsOutField, clearsInField, clearsOutField, netKillsInField, netKillsOutField;
    GraphPanel graphPanel;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "SQLite JDBC Driver not found!");
            System.exit(1);
        }
    }

    public BadmintonTrackerGUI() {
        setTitle("Badminton Tracker");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initDB();

        JTabbedPane tabs = new JTabbedPane();

        JPanel addPanel = new JPanel(new GridLayout(9, 2, 10, 10));
        smashesInField = new JTextField();
        smashesOutField = new JTextField();
        dropsInField = new JTextField();
        dropsOutField = new JTextField();
        clearsInField = new JTextField();
        clearsOutField = new JTextField();
        netKillsInField = new JTextField();
        netKillsOutField = new JTextField();

        addPanel.add(new JLabel("Smashes In:"));
        addPanel.add(smashesInField);
        addPanel.add(new JLabel("Smashes Out:"));
        addPanel.add(smashesOutField);
        addPanel.add(new JLabel("Drops In:"));
        addPanel.add(dropsInField);
        addPanel.add(new JLabel("Drops Out:"));
        addPanel.add(dropsOutField);
        addPanel.add(new JLabel("Clears In:"));
        addPanel.add(clearsInField);
        addPanel.add(new JLabel("Clears Out:"));
        addPanel.add(clearsOutField);
        addPanel.add(new JLabel("Net Kills In:"));
        addPanel.add(netKillsInField);
        addPanel.add(new JLabel("Net Kills Out:"));
        addPanel.add(netKillsOutField);

        JButton addButton = new JButton("Add Match");
        addButton.addActionListener(e -> addMatch());
        addPanel.add(addButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        addPanel.add(exitButton);

        tabs.add("Add Match", addPanel);

        JPanel viewPanel = new JPanel(new BorderLayout());
        model = new DefaultTableModel(new String[]{"DateTime", "Smashes In", "Smashes Out", "Drops In", "Drops Out", "Clears In", "Clears Out", "Net Kills In", "Net Kills Out"}, 0);
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        viewPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.addActionListener(e -> loadDashboard());
        JButton clearButton = new JButton("Clear All Matches");
        clearButton.addActionListener(e -> clearAllMatches());
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        viewPanel.add(buttonPanel, BorderLayout.SOUTH);

        tabs.add("View Matches", viewPanel);

        JPanel graphControlPanel = new JPanel(new FlowLayout());
        filterComboBox = new JComboBox<>(new String[]{"Second", "Day", "Week", "Month", "Year"});
        statComboBox = new JComboBox<>(new String[]{"All", "Smashes In", "Smashes Out", "Drops In", "Drops Out", "Clears In", "Clears Out", "Net Kills In", "Net Kills Out"});

        graphControlPanel.add(new JLabel("View by:"));
        graphControlPanel.add(filterComboBox);
        graphControlPanel.add(new JLabel("Stat:"));
        graphControlPanel.add(statComboBox);

        JButton generateGraphButton = new JButton("Generate Progress Graph");
        generateGraphButton.addActionListener(e -> {
            graphPanel.currentStat = (String) statComboBox.getSelectedItem();
            graphPanel.updateData();
        });
        graphControlPanel.add(generateGraphButton);

        graphPanel = new GraphPanel();

        JPanel graphPage = new JPanel(new BorderLayout());
        graphPage.add(graphControlPanel, BorderLayout.NORTH);
        graphPage.add(graphPanel, BorderLayout.CENTER);

        tabs.add("View Progress", graphPage);

        add(tabs, BorderLayout.CENTER);

        AccuracyPanel accuracyPanel = new AccuracyPanel();
        tabs.add("Shot Accuracy", accuracyPanel);


        setVisible(true);
    }

    public void initDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS matches ("
                    + "datetime TEXT,"
                    + "smashes_in INTEGER,"
                    + "smashes_out INTEGER,"
                    + "drops_in INTEGER,"
                    + "drops_out INTEGER,"
                    + "clears_in INTEGER,"
                    + "clears_out INTEGER,"
                    + "netkills_in INTEGER,"
                    + "netkills_out INTEGER)";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMatch() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO matches (datetime, smashes_in, smashes_out, drops_in, drops_out, clears_in, clears_out, netkills_in, netkills_out) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pstmt.setString(1, datetime);
            pstmt.setInt(2, Integer.parseInt(smashesInField.getText()));
            pstmt.setInt(3, Integer.parseInt(smashesOutField.getText()));
            pstmt.setInt(4, Integer.parseInt(dropsInField.getText()));
            pstmt.setInt(5, Integer.parseInt(dropsOutField.getText()));
            pstmt.setInt(6, Integer.parseInt(clearsInField.getText()));
            pstmt.setInt(7, Integer.parseInt(clearsOutField.getText()));
            pstmt.setInt(8, Integer.parseInt(netKillsInField.getText()));
            pstmt.setInt(9, Integer.parseInt(netKillsOutField.getText()));
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Match added successfully!");
            clearInputFields();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding match.");
        }
    }

    public void clearAllMatches() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM matches");
            JOptionPane.showMessageDialog(this, "All matches cleared successfully!");
            loadDashboard();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error clearing matches.");
        }
    }    

    public void loadDashboard() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM matches")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("datetime"),
                        rs.getInt("smashes_in"),
                        rs.getInt("smashes_out"),
                        rs.getInt("drops_in"),
                        rs.getInt("drops_out"),
                        rs.getInt("clears_in"),
                        rs.getInt("clears_out"),
                        rs.getInt("netkills_in"),
                        rs.getInt("netkills_out")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    

    public void clearInputFields() {
        smashesInField.setText("");
        smashesOutField.setText("");
        dropsInField.setText("");
        dropsOutField.setText("");
        clearsInField.setText("");
        clearsOutField.setText("");
        netKillsInField.setText("");
        netKillsOutField.setText("");
    }
    

    class GraphPanel extends JPanel {
        List<List<Integer>> allData = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String currentStat = "All";
    
        public void updateData() {
            allData.clear();
            labels.clear();
        
            int statIndex = switch (currentStat) {
                case "Smashes In" -> 0;
                case "Smashes Out" -> 1;
                case "Drops In" -> 2;
                case "Drops Out" -> 3;
                case "Clears In" -> 4;
                case "Clears Out" -> 5;
                case "Net Kills In" -> 6;
                case "Net Kills Out" -> 7;
                default -> -1;
            };
        
            String filter = (String) filterComboBox.getSelectedItem(); // <-- get selected time unit
        
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM matches")) {
        
                while (rs.next()) {
                    String datetime = rs.getString("datetime");
        
                    // Group based on selected filter
                    String key = switch (filter) {
                        case "Second" -> datetime.substring(0, 19);
                        case "Day" -> datetime.substring(0, 10);
                        case "Month" -> datetime.substring(0, 7);
                        case "Year" -> datetime.substring(0, 4);
                        case "Week" -> datetime.substring(0, 8) + "W"; // approximate week
                        default -> datetime;
                    };
        
                    if (!labels.contains(key)) {
                        labels.add(key);
                        if (statIndex == -1) {
                            allData.add(new ArrayList<>(List.of(0, 0, 0, 0, 0, 0, 0, 0)));
                        } else {
                            allData.add(new ArrayList<>(List.of(0)));
                        }
                    }
        
                    int idx = labels.indexOf(key);
                    List<Integer> stats = allData.get(idx);
        
                    if (statIndex == -1) {
                        stats.set(0, stats.get(0) + rs.getInt("smashes_in"));
                        stats.set(1, stats.get(1) + rs.getInt("smashes_out"));
                        stats.set(2, stats.get(2) + rs.getInt("drops_in"));
                        stats.set(3, stats.get(3) + rs.getInt("drops_out"));
                        stats.set(4, stats.get(4) + rs.getInt("clears_in"));
                        stats.set(5, stats.get(5) + rs.getInt("clears_out"));
                        stats.set(6, stats.get(6) + rs.getInt("netkills_in"));
                        stats.set(7, stats.get(7) + rs.getInt("netkills_out"));
                    } else {
                        String[] columns = {"smashes_in", "smashes_out", "drops_in", "drops_out", "clears_in", "clears_out", "netkills_in", "netkills_out"};
                        stats.set(0, stats.get(0) + rs.getInt(columns[statIndex]));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        
            repaint();
        }                
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (allData.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));

            int padding = 50;
            int width = getWidth() - 2 * padding;
            int height = getHeight() - 2 * padding;

            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            String title = currentStat.equals("All") ? "Badminton Progress Over Time" : currentStat + " Progress Over Time";
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (getWidth() - titleWidth) / 2, padding / 2);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

            int max = allData.stream().flatMap(List::stream).max(Integer::compare).orElse(1);
            int min = allData.stream().flatMap(List::stream).min(Integer::compare).orElse(0);
            if (min == max) {
                min = 0;
                max = max + 1;
            }

            int paddingMargin = Math.max(1, (max - min) / 10);
            min = Math.max(0, min - paddingMargin);
            max = max + paddingMargin;

            int range = max - min;

            g2.setColor(Color.BLACK);
            g2.drawLine(padding, padding, padding, getHeight() - padding);
            g2.drawLine(padding, getHeight() - padding, getWidth() - padding, getHeight() - padding);

            g2.setColor(Color.LIGHT_GRAY);
            int step;
            if (range > 10) {
                step = (int) Math.ceil(range / 5.0);
            } else if (range > 5) {
                step = 2;
            } else {
                step = 1;
            }

            for (int value = min; value <= max; value += step) {
                int y = padding + (height * (max - value)) / (max - min);
                g2.drawLine(padding, y, getWidth() - padding, y);
                g2.setColor(Color.BLACK);
                g2.drawString(Integer.toString(value), padding - 40, y + 5);
                g2.setColor(Color.LIGHT_GRAY);
            }

            g2.setColor(Color.LIGHT_GRAY);
            int pointSpacing = allData.size() > 1 ? width / (allData.size() - 1) : width;
            for (int i = 0; i < allData.size(); i++) {
                int x = padding + i * pointSpacing;
                g2.drawLine(x, padding, x, getHeight() - padding);
                g2.setColor(Color.BLACK);
                if (i < labels.size()) {
                    String label = labels.get(i);
                    if (label.length() > 10) label = label.substring(5); // shorten if too long
                    g2.drawString(label, x - 15, getHeight() - padding + 20);
                }
                g2.setColor(Color.LIGHT_GRAY);
            }

            g2.setColor(Color.BLACK);
            g2.drawString("Time", getWidth() / 2, getHeight() - 10);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String yLabel = currentStat.equals("All") ? "Shots" : currentStat;
            int yLabelWidth = g2.getFontMetrics().stringWidth(yLabel);
            g2.drawString(yLabel, padding / 4, padding - 10);

            if (currentStat.equals("All")) {
                Color[] colors = {
                    Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, 
                    Color.MAGENTA, Color.CYAN, Color.PINK, Color.YELLOW
                };
                String[] statNames = {
                    "Smashes In", "Smashes Out",
                    "Drops In", "Drops Out",
                    "Clears In", "Clears Out",
                    "Net Kills In", "Net Kills Out"
                };
                for (int stat = 0; stat < 8; stat++) { 
                    drawStatLine(g2, colors[stat], stat, padding, height, pointSpacing, min, max);
                }
                drawLegend(g2, colors, statNames, padding);
            } else {
                int statIndex = getStatIndex(currentStat);
                drawStatLine(g2, Color.BLUE, statIndex, padding, height, pointSpacing, min, max);
            }
        }

        private int getStatIndex(String statName) {
            return switch (statName) {
                case "Smashes In" -> 0;
                case "Smashes Out" -> 1;
                case "Drops In" -> 2;
                case "Drops Out" -> 3;
                case "Clears In" -> 4;
                case "Clears Out" -> 5;
                case "Net Kills In" -> 6;
                case "Net Kills Out" -> 7;
                default -> 0;
            };
        }        

        private void drawStatLine(Graphics2D g2, Color color, int statIndex, int padding, int height, int pointSpacing, int min, int max) {
            g2.setColor(color);

            int prevX = padding;
            int prevY = padding + (height * (max - (currentStat.equals("All") ? allData.get(0).get(statIndex) : allData.get(0).get(0)))) / (max - min);

            for (int i = 1; i < allData.size(); i++) {
                int x = padding + i * pointSpacing;
                int y = padding + (height * (max - (currentStat.equals("All") ? allData.get(i).get(statIndex) : allData.get(i).get(0)))) / (max - min);

                g2.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }

        private void drawLegend(Graphics2D g2, Color[] colors, String[] statNames, int padding) {
            int legendX = getWidth() - 250;  // Adjusted to not overlap
            int legendY = padding;
        
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
            for (int i = 0; i < colors.length; i++) {
                g2.setColor(colors[i]);
                g2.fillRect(legendX, legendY + i * 20, 10, 10);
                g2.setColor(Color.BLACK);
                g2.drawString(statNames[i], legendX + 15, legendY + 10 + i * 20);
            }
        }        
    }
    
    class AccuracyPanel extends JPanel {
        JComboBox<String> shotTypeComboBox;
        AccuracyPieChart pieChart;
    
        public AccuracyPanel() {
            setLayout(new BorderLayout());
    
            JPanel controlPanel = new JPanel(new FlowLayout());
            shotTypeComboBox = new JComboBox<>(new String[]{"Smashes", "Drops", "Clears", "Net Kills"});
    
            JButton generateButton = new JButton("Generate Accuracy Pie Chart");
            generateButton.addActionListener(e -> {
                pieChart.setCurrentShotType((String) shotTypeComboBox.getSelectedItem());
                pieChart.updateData();
            });
    
            controlPanel.add(new JLabel("Shot Type:"));
            controlPanel.add(shotTypeComboBox);
            controlPanel.add(generateButton);
    
            pieChart = new AccuracyPieChart();
    
            add(controlPanel, BorderLayout.NORTH);
            add(pieChart, BorderLayout.CENTER);
        }
    }    
    
    class AccuracyPieChart extends JPanel {
        double accuracy = 0.0; // Target accuracy (e.g., 0.75 means 75%)
        double animatedAccuracy = 0.0; // Progressively animated value
    
        Timer animationTimer;
        String currentShotType = "Smashes"; // Default shot type

        public void setCurrentShotType(String shotType) {
            this.currentShotType = shotType;
        }

        // This method queries the database and sets accuracy
        public void updateData() {
            int made = 0;
            int total = 0;

            try (Connection conn = DriverManager.getConnection(BadmintonTrackerGUI.DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM matches")) {

                while (rs.next()) {
                    switch (currentShotType) {
                        case "Smashes" -> {
                            made += rs.getInt("smashes_in");
                            total += rs.getInt("smashes_in") + rs.getInt("smashes_out");
                        }
                        case "Drops" -> {
                            made += rs.getInt("drops_in");
                            total += rs.getInt("drops_in") + rs.getInt("drops_out");
                        }
                        case "Clears" -> {
                            made += rs.getInt("clears_in");
                            total += rs.getInt("clears_in") + rs.getInt("clears_out");
                        }
                        case "Net Kills" -> {
                            made += rs.getInt("netkills_in");
                            total += rs.getInt("netkills_in") + rs.getInt("netkills_out");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (total == 0) {
                setAccuracy(0.0);
            } else {
                setAccuracy((double) made / total); // set target accuracy to animate
            }
        }
    
        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
            animatedAccuracy = 0.0;
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            animationTimer = new Timer(10, e -> animate());
            animationTimer.start();
        }
    
        private void animate() {
            animatedAccuracy += 0.01;
            if (animatedAccuracy >= accuracy) {
                animatedAccuracy = accuracy;
                animationTimer.stop();
            }
            repaint();
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
    
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            int size = Math.min(getWidth(), getHeight()) - 100;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
    
            int angle = (int) Math.round(animatedAccuracy * 360);
    
            // "Made" (Blue section)
            g2.setColor(Color.BLUE);
            g2.fillArc(x, y, size, size, 90, -angle);
    
            // "Missed" (Light Gray section)
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillArc(x, y, size, size, 90 - angle, -(360 - angle));
    
            // Outline circle
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(x, y, size, size);
    
            // Red percentage text
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.setColor(Color.RED);
            String percentString = String.format("%.1f%%", animatedAccuracy * 100);
            int textWidth = g2.getFontMetrics().stringWidth(percentString);
            g2.drawString(percentString, (getWidth() - textWidth) / 2, getHeight() / 2 + 10);
    
            // Legend
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            int legendX = getWidth() - 150;
            int legendY = getHeight() - 100;
    
            g2.setColor(Color.BLUE);
            g2.fillRect(legendX, legendY, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Made", legendX + 20, legendY + 12);
    
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(legendX, legendY + 25, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Missed", legendX + 20, legendY + 37);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BadmintonTrackerGUI::new);
    }
}

//add another tab of a bar to show their accuracy for certain shots
//*remember to note down for ppl on how to run the program: enter command */