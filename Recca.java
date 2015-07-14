/* Applet Problems ?
 * from: http://forums.sun.com/thread.jspa?threadID=655119
 * when the path file name contains a space, it can cause problems to load the applet!
 * moving to a directory without space works!
 *
 */

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

public class Recca extends Frame implements Runnable, ActionListener {
    private Grid grille;
    private Thread gameThread = null;
    private int genTime = 0;
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

	private Choice cLaws = new Choice();
    private Choice cWogs = new Choice();

    Panel controls;
    Panel lawsPanel;

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
        setBackground(new Color(0x999999));

        // create components and add them to container
        grille = new Grid(this, cellSize, cellCols, cellRows);

        // add the laws
        cLaws.add("Rules");
        reloadRules();
        cLaws.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Loading laws " + arg);
                    changeRule(arg);
              }});

        reloadCWogs();
        cWogs.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Loading pattern " + arg);
                    changePattern(arg);
              }});


        Choice cSpeed = new Choice();
        cSpeed.addItemListener(new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("Changing speed " + arg);
                    changeSpeed(arg);
              }});

        cSpeed.add(slow);
        cSpeed.add(fast);
        cSpeed.add(faster);
        cSpeed.add(maxSpeed);
        cSpeed.select(1);
        genTime = 30;

        /*
        Choice palettes = new Choice();
        palettes.addItem(pal1);
        palettes.addItem(pal2);
        palettes.addItem(pal3);
        palettes.select(0);
        grille.setPalette(0);
        */

        JButton jbutt;
        JToggleButton togbutt;

        controls = new Panel();
        controls.setLayout(new FlowLayout());
        controls.add(cLaws);
        controls.add(cWogs);
        controls.add(jbutt = buttonClear);
        jbutt.addActionListener(this);
        controls.add(togbutt = buttonStart); togbutt.addActionListener(this);
        controls.add(jbutt = buttonNext); jbutt.addActionListener(this);
        controls.add(jbutt = buttonReverse); jbutt.addActionListener(this);
        controls.add(jbutt = buttonFile); jbutt.addActionListener(this);
        controls.add(jbutt = buttonSave); jbutt.addActionListener(this);
        controls.add(cSpeed);
        //controls.add(palettes);
        controls.add(togbutt = buttonEdition); togbutt.addActionListener(this);
        controls.add(jbutt = buttonGrid); jbutt.addActionListener(this);
        controls.add(jbutt = buttonLaws); jbutt.addActionListener(this);


        lawsPanel = new Panel();
        lawsPanel.setLayout(new FlowLayout());
        lawsPanel.add(jbutt = buttonLoadLaws); jbutt.addActionListener(this);
        lawsPanel.add(jbutt = buttonSaveLaws); jbutt.addActionListener(this);
        lawsPanel.add(jbutt = buttonAutomaton); jbutt.addActionListener(this);
        lawsPanel.add(togbutt = buttonEditionLaws); togbutt.addActionListener(this);


        setLayout(new BorderLayout());
        add("North", controls);
        grille.setMode(grille.MODE_AUTOMATE);
        add("Center", grille);
        setVisible(true);
        resize(getPreferredSize());
        validate();

		pack(); // set window to appropriate size (for its elements)
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
            reloadCWogs();
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
						cLaws.add(children[i].getName());
				}
			}
		}
	}

    private void reloadCWogs() {
        cWogs.removeAll();
        cWogs.add("Patterns");
        cWogs.add(alea);

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
                cWogs.add(name.substring(0, name.length()-4));
            }
        }
       validate();
    }

    private void changeSpeed(String arg) {
        if(slow.equals(arg))
            genTime = 200;
        else if(fast.equals(arg))
            genTime = 30;
        else if(faster.equals(arg))
            genTime = 0;
        else if(maxSpeed.equals(arg))
            genTime = -1;
    }

    // no start() to prevent starting immediately
    public void start2() {
        if(gameThread == null) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void stop() {
        if(gameThread != null) {
            //gameThread.stop();
            gameThread = null;
        }
    }

    public void run() {
        long t = System.currentTimeMillis();
        while(gameThread != null) {
            grille.next();
            if(genTime != -1) {
                grille.repaint();
                try {
                    Thread.sleep(genTime);
                } catch(InterruptedException e) {}
			} else {
                long t2 = System.currentTimeMillis();
                if(t2 - t > 100) {
                    t = t2;
                    grille.repaint();
                }
            }
        }
    }
    
    public void actionStartStop() {
		if(buttonStart.isSelected()){
			start2();
			buttonStart.setLabel(stopLabel);
		} else {
			stop();
			buttonStart.setLabel(startLabel);
		}
	}
    
    public void actionPerformed(ActionEvent ev) {
		Object source = ev.getSource();
		if(source == buttonClear)
        {
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
				grille.invertEdition();
				tBtn.setLabel(endEdition);
				grille.repaint();
			} else{
				grille.invertEdition();
				tBtn.setLabel(edition);
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

    public void handleKeystroke(int key) {
		switch(key) {
		case 10: // Enter
            actionStartStop();
			break;
        case 32: // Space
            grille.next();
            grille.repaint();
            break;
        case 8: // Backspace
			// One step back
            grille.reverse();
            grille.next();
            grille.reverse();
            grille.repaint();
            break;
        default:
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

	public static void main(String[] args) {
		Frame myFrame = new Recca();
		myFrame.setLocationRelativeTo(null);
		myFrame.setVisible(true);
	}
}



/*
 * TODO
 * - Undo button
 * - Edit mode in diagonal
 * - Choose what symmetries to apply
 * - Add #iterations
 * - Rotate pattern
 */
