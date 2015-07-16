import java.util.Arrays;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.zip.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

public class Recca extends JFrame implements ActionListener {
    private Grid grille;
    private String lawDir;

    private JButton buttonClear = new JButton("Clear");
    private JButton buttonFile = new JButton("Load");
    private JButton buttonSave = new JButton("Save");
    private JButton buttonNext = new JButton("Next");
    private JButton buttonReverse = new JButton("Reverse");
    private JButton buttonGrid = new JButton("Grid");
    private JButton buttonLaws = new JButton("Laws");
    private JButton buttonAutomaton = new JButton("Go Back");
    private JButton buttonLoadLaws = new JButton("Load Laws");
    private JButton buttonSaveLaws = new JButton("Save Laws");

    private final String alea = "Random";
    private final String slow = "Slow";
    private final String fast = "Normal";
    private final String faster = "Fast";
    private final String maxSpeed = "Max";
    private final String nextLabel = "Next";
    private final String startLabel = "Start";
    private final String stopLabel = "Stop";
    private final String edition = "Editor";
    private final String endEdition = "End Edit";

    private final String patternDir = "patterns";
    private final String patternExt = ".pat";
    private final String lawExt = ".law";

    private JToggleButton buttonStart = new JToggleButton(startLabel);
    private JToggleButton buttonEdition = new JToggleButton(edition);
    private JToggleButton buttonEditionLaws = new JToggleButton(edition);

    private JComboBox<String> cLaws = new JComboBox<>();
    private JComboBox<String> cPats = new JComboBox<>();
    private JComboBox<String> cSpeed = new JComboBox<>();

    private JPanel controls;
    private JPanel lawsPanel;
    
    private Timer timer;

    public Recca() {
        super("Recca");
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dispose();
            }});
            
        int cellSize = 5;
        int cellCols = 150;
        int cellRows = 150;
        String param;

        // set background
        setBackground(Color.BLACK);

        // create components and add them to container
        grille = new Grid(this, cellSize, cellCols, cellRows);
        
        timer = new Timer(100, this);

        // add the laws
        cLaws.addItem("Rules");
        reloadRules();
        cLaws.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Loading laws " + arg);
                    changeRule(arg);
              }});

        reloadCPats();
        cPats.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Loading pattern " + arg);
                    changePattern(arg);
              }});


        cSpeed.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Changing speed " + arg);
                    changeSpeed(arg);
              }});

        cSpeed.addItem(slow);
        cSpeed.addItem(fast);
        cSpeed.addItem(faster);
        cSpeed.addItem(maxSpeed);

        JButton jbutt;
        JToggleButton togbutt;

        controls = new JPanel();
        controls.setLayout(new FlowLayout());
        controls.add(cLaws);
        controls.add(cPats);
        controls.add(jbutt = buttonClear); jbutt.addActionListener(this);
        controls.add(togbutt = buttonStart); togbutt.addActionListener(this);
        controls.add(jbutt = buttonNext); jbutt.addActionListener(this);
        controls.add(jbutt = buttonReverse); jbutt.addActionListener(this);
        controls.add(jbutt = buttonFile); jbutt.addActionListener(this);
        controls.add(jbutt = buttonSave); jbutt.addActionListener(this);
        controls.add(cSpeed);
        controls.add(togbutt = buttonEdition); togbutt.addActionListener(this);
        controls.add(jbutt = buttonGrid); jbutt.addActionListener(this);
        controls.add(jbutt = buttonLaws); jbutt.addActionListener(this);


        lawsPanel = new JPanel();
        lawsPanel.setLayout(new FlowLayout());
        lawsPanel.add(jbutt = buttonLoadLaws); jbutt.addActionListener(this);
        lawsPanel.add(jbutt = buttonSaveLaws); jbutt.addActionListener(this);
        lawsPanel.add(jbutt = buttonAutomaton); jbutt.addActionListener(this);
        lawsPanel.add(togbutt = buttonEditionLaws); togbutt.addActionListener(this);


        setLayout(new BorderLayout());
        add("North", controls);
        grille.setMode(grille.MODE_AUTOMATE);
        add("Center", grille);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
        setSize(getPreferredSize());

        pack(); // set window to appropriate size (for its elements)
        
        cSpeed.setSelectedItem(fast);
        
    }

    private void myShowStatus(String status) {
        System.out.println(status);
    }

    private InputStream nameToInputStream(String name) throws IOException {
        return new FileInputStream(name);
    }

    private void changeRule(String ruleName) {
        try {
            InputStream lawStream = nameToInputStream(patternDir + "/" + ruleName + "/laws" + lawExt);
            grille.chargerLois(lawStream);
            lawDir = ruleName; // set it afterwards in case the above fails
            reloadCPats();
        } catch(IOException e) {
            System.out.println("Error while loading laws " + ruleName);
            System.out.println(e.toString());
        }
    }

    private void changePattern(String patternName) {
        if(alea.equals(patternName)) {
            grille.randomField();
            grille.repaint();
        } else {
            try {
                drawStream(nameToInputStream(patternDir + "/" + lawDir + "/" + patternName + patternExt));
            } catch(IOException e) {
                System.out.println("Error while loading pattern " + patternName);
                myShowStatus("Error while loading resource.");
            }
        }
    }

    private void reloadRules() {
        File laws = new File(patternDir);
        File[] children = laws.listFiles();
        if(children != null) {
            Arrays.sort(children);
            for(int i = 0; i<children.length; i++) {
                // Get filename of file or directory
                if(children[i].isDirectory()) {
                    String[] hasLaw = children[i].list(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.equals("laws" + lawExt);
                        }});
                    if(hasLaw != null && hasLaw.length > 0)
                        cLaws.addItem(children[i].getName());
                }
            }
        }
    }

    private void reloadCPats() {
        cPats.removeAllItems();
        cPats.addItem("Patterns");
        cPats.addItem(alea);

        File laws = new File(patternDir + "/" + lawDir);
        File[] children = laws.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(patternExt);
            }});
        if(children != null) {
            Arrays.sort(children);
            for(int i=0; i<children.length; i++) {
                String name = children[i].getName();
                System.out.println(children[i].getName());
                cPats.addItem(name.substring(0, name.length()-4));
            }
        }
       validate();
    }

    private void changeSpeed(String arg) {
        if(slow.equals(arg))
            timer.setDelay(200);
        else if(fast.equals(arg))
            timer.setDelay(30);
        else if(faster.equals(arg))
            timer.setDelay(1);
        else if(maxSpeed.equals(arg))
            timer.setDelay(0);
    }

    public void start() {
        timer.restart();
        buttonStart.setText(stopLabel);
        buttonStart.setSelected(true);
    }

    public void stop() {
        timer.stop();
        buttonStart.setText(startLabel);
        buttonStart.setSelected(false);
    }
    
    public void startStop() {
        if(buttonStart.isSelected()){
            stop();
        } else {
            start();
        }
    }

    public void actionStartStop() {
        // reverse of startStop() as we just pressed the button
        if(buttonStart.isSelected()){
            start();
        } else {
            stop();
        }
    }
    
    public void actionPerformed(ActionEvent ev) {
        Object source = ev.getSource();
        if(source == timer){
            grille.next();
            grille.repaint();
			getToolkit().sync();
        } else if(source == buttonClear) {
            grille.clear();
            grille.repaint();
        } else if(source == buttonNext) {
            grille.next();
            grille.repaint();
        } else if(source == buttonStart) {
            actionStartStop();
        } else if(source == buttonEdition || source == buttonEditionLaws) {
             JToggleButton tBtn = (JToggleButton)ev.getSource();
            if(tBtn.isSelected()){
                grille.setEdition(true);
                tBtn.setText(endEdition);
                grille.repaint();
            } else{
                grille.setEdition(false);
                tBtn.setText(edition);
                grille.repaint();
            }
        } else if(source == buttonFile) {
            ouvrirFichier();
            grille.repaint();
        } else if(source == buttonSave) {
            sauver();
        } else if(source == buttonReverse) {
            grille.reverse();
            grille.repaint();
        } else if(source == buttonGrid) {
            grille.invertGridMode();
            grille.repaint();
        } else if(source == buttonLaws) {
            remove(controls);
            add("North", lawsPanel);
            grille.setMode(grille.MODE_LOIS);
            grille.repaint();
            revalidate();
            lawsPanel.repaint();
        } else if(source == buttonAutomaton) {
            remove(lawsPanel);
            invalidate();
            add("North", controls);
            grille.setMode(grille.MODE_AUTOMATE);
            grille.initPalettes();
            grille.repaint();
            revalidate();
            controls.repaint();
        } else if(source == buttonSaveLaws) {
            sauverLois();
        } else if(source == buttonLoadLaws) {
            chargerLois();
            grille.repaint();
        }
    }

    // draws the shape to canvas
    public void drawShape(int shapeWidth, int shapeHeight, int shape[][]) {
        if(!grille.drawShape(shapeWidth, shapeHeight, shape))
            myShowStatus("Shape too large.");
        else {
            myShowStatus("");
            grille.repaint();
        }
    }

    public void drawStream(InputStream fileReader) throws IOException {
        DataInputStream dis = new DataInputStream(fileReader);

        int shapeWidth;
        int shapeHeight;
        int shape[][];
        int shapeb[];

        dis.readInt(); // on skippe le premier octet... pas tres propre !
        shapeWidth = dis.readInt();
        shapeWidth = shapeWidth*256  + dis.readInt();
        shapeWidth = shapeWidth*256 + dis.readInt();
        dis.readInt(); // on skippe le premier octet... pas tres propre !
        shapeHeight = dis.readInt();
        shapeHeight = shapeHeight*256 + dis.readInt();
        shapeHeight = shapeHeight*256 + dis.readInt();

        shapeb = new int[shapeWidth*shapeHeight];
        for(int i=0; i < shapeWidth*shapeHeight; i++)
            shapeb[i] = dis.readInt();
        dis.close();
        fileReader.close();
        if(!grille.drawShape(shapeWidth, shapeHeight, shapeb))
            myShowStatus("Shape too large.");
        else {
            myShowStatus("File correctly opened. " +shapeWidth +" "+ shapeHeight);
            grille.repaint();
        }

    }

    // draws a file to canvas
    public void drawFichier(String fichier) {
        try {
            drawStream(new FileInputStream(fichier));

        } catch(FileNotFoundException e) {
            myShowStatus("File not found.");
        } catch(IOException e) {
            myShowStatus("Error while reading file.");
        }

    }

    // draws an URL to canvas
    public void drawURL(URL url) {
        try {
            drawStream(url.openStream());

        } catch(FileNotFoundException e) {
            myShowStatus("File not found.");
        } catch(IOException e) {
            myShowStatus("Error while reading file.");
        }
    }

    private void ouvrirFichier() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog(parent, "Open pattern file", FileDialog.LOAD);
        filedialog.setFile("*" + patternExt);
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            drawFichier(filepath);
        }
    }

    private void sauver() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog(parent, "Save pattern file", FileDialog.SAVE);
        filedialog.setFile("*" + patternExt);
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += patternExt;

            OutputStream fileWriter;
            try {
                fileWriter = new FileOutputStream(filepath);
                grille.sauver(fileWriter);
                fileWriter.close();
            } catch(FileNotFoundException e) {
                myShowStatus("Cannot open file for writing.");
            } catch(IOException e) {
                myShowStatus("Error saving file.");
            }
        }
    }

    private void chargerLois() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog(parent, "Open laws file", FileDialog.LOAD);
        filedialog.setFile("*" + lawExt);
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            try {
                grille.chargerLois(new FileInputStream(filepath));
            } catch(FileNotFoundException e) {
                myShowStatus("Cannot open file for writing.");
            } catch(IOException e) {
                myShowStatus("Error saving file.");
            }
        }
    }

    private void sauverLois() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog(parent, "Save laws file", FileDialog.SAVE);
        filedialog.setFile("*" + lawExt);
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += lawExt;

            OutputStream fileWriter;
            try {
                fileWriter = new FileOutputStream(filepath);
                grille.sauverLois(fileWriter);
                fileWriter.close();
            } catch(FileNotFoundException e) {
                myShowStatus("Cannot open file for writing.");
            } catch(IOException e) {
                myShowStatus("Error saving file.");
            }
        }
    }

    private static void createAndShowGUI() {
        Frame myFrame = new Recca();
        myFrame.setLocationRelativeTo(null);
        myFrame.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

/*
 * TODO
 * - Undo button
 * - Edit mode in diagonal
 * - Choose what symmetries to apply
 * - Add #iterations
 */
