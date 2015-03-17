/* Applet Problems ?
 * from: http://forums.sun.com/thread.jspa?threadID=655119
 * when the path file name contains a space, it can cause problems to load the applet!
 * moving to a directory without space works!
 *
 */

import java.util.Arrays;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
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

public class Recca extends Applet implements Runnable {
    private Grid grille;
    private Thread gameThread = null;
    private int genTime = 0;
    private String lawDir;
    private boolean isApplet = false;

    private final String clear = "Clear";
    private final String alea = "Random";
    private final String slow = "Slow";
    private final String fast = "Normal";
    private final String faster = "Fast";
    private final String maxSpeed = "Max";
    private final String nextLabel = "Next";
    private final String startLabel = "Start";
    private final String stopLabel = "Stop";
    private final String fileLabel = "Load";
    private final String saveLabel = "Save";
    private final String reverseLabel = "Reverse";
    private final String pal1 = "Palette1";
    private final String pal2 = "Palette2";
    private final String pal3 = "Palette3";
    private final String edition = "Editor";
    private final String endEdition = "End Edit";
    private final String gridLabel = "Grid";
    private final String loisLabel = "Laws";
    private final String automateLabel = "Automaton";
    private final String chargerLoisLabel = "Load Laws";
    private final String sauverLoisLabel = "Save Laws";

    private Button startstopButton;
    private Button editionButton;

	private Choice cLaws = new Choice();
    private Choice cWogs = new Choice();

    Panel controls;
    Panel lawsPanel;

    // This method is called automatically when this is an applet
    // running in a browser
    public void init() {
		init(true);
	}

	// This method is called manually with isApplet=false when
	// run in a frame
    public void init(boolean isApplet) {
        int cellSize = 5;
        int cellCols = 150;
        int cellRows = 150;
        String param;

        this.isApplet = isApplet;

        // set background
        setBackground(new Color(0x999999));

        // read parameters from HTML if in an applet
        if(isApplet) {
			param = getParameter("cellsize");
			if(param!= null)
				cellSize = Integer.valueOf(param).intValue();

			param = getParameter("cellsize");
			if(param!= null)
				cellSize = Integer.valueOf(param).intValue();

			param = getParameter("cellcols");
			if(param!= null)
				cellCols = Integer.valueOf(param).intValue();

			param = getParameter("cellrows");
			if(param!= null)
				cellRows = Integer.valueOf(param).intValue();

			param = getParameter("gentime");
			if(param!= null)
				genTime = Integer.valueOf(param).intValue();
		}

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

        startstopButton = new Button(startLabel);
        startstopButton.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent evt) {
                  actionStartStop();
            }});
        editionButton = new Button(edition);

        controls = new Panel();
        controls.add(cLaws);
        controls.add(cWogs);
        controls.add(new Button(clear));
        controls.add(startstopButton);
        controls.add(new Button(nextLabel));
        controls.add(new Button(reverseLabel));
        controls.add(new Button(fileLabel));
        controls.add(new Button(saveLabel));
        controls.add(cSpeed);
        //controls.add(palettes);
        controls.add(editionButton);
        controls.add(new Button(gridLabel));
        controls.add(new Button(loisLabel));


        lawsPanel = new Panel();
        lawsPanel.add(new Button(chargerLoisLabel));
        lawsPanel.add(new Button(sauverLoisLabel));
        lawsPanel.add(new Button(automateLabel));


        setLayout(new BorderLayout());
        add("North", controls);
        grille.setMode(grille.MODE_AUTOMATE);
        add("Center", grille);
        setVisible(true);
        resize(getPreferredSize());
        validate();


		if(isApplet) {
			// We can now give parameters to the URL!!
			// example:
			// file:///D:/Mes%20documents/Projets/RECCA/RECCA.html?rule=Strings&pattern=12&speed=Fast&start=true
			// with direct link to the applet:
			// file:///D:/Mes%20documents/Projets/RECCA/RECCA.html?rule=Strings&pattern=12&speed=Fast&start=true#applet

			System.out.println("href =" + getDocumentBase());
			String ruleName = getQueryValue("rule");
			System.out.println("ruleName = " + ruleName);
			if(ruleName != null)
				changeRule(ruleName);
			String patternName = getQueryValue("pattern");
			System.out.println("patternName = " + patternName);
			if(patternName != null)
				changePattern(patternName);
			String speedValue = getQueryValue("speed");
			System.out.println("speedValue = " + speedValue);
			if(speedValue != null)
				changeSpeed(speedValue);
			String startValue = getQueryValue("start");
			System.out.println("startValue = " + startValue);
			if(startValue != null)
				actionStartStop();

		}
    }

    private String getQueryValue(String name) {
        URL href = getDocumentBase();
        if(href == null)
            return null;
        String query = href.getQuery();
        if(query == null)
            return null;
        String[] pairs = query.split("&");
        for(int i = 0; i < pairs.length; i++)  {
            String[] pair = pairs[i].split("=");
            if(pair[0].equals(name))
                    return pair[1];
        }
        return null;
    }

    private void myShowStatus(String status) {
		if(isApplet) {
			myShowStatus(status);
		} else {
			System.out.println(status);
		}

	}

    private InputStream nameToInputStream(String name) throws IOException {
		if(isApplet) {
			return new DataInputStream((new URL(getCodeBase(), name)).openStream());
		} else {
			return new FileInputStream(name);
		}
	}

    private void changeRule(String ruleName) {
        try {
			InputStream lawStream = nameToInputStream("wogs/" + ruleName + "/laws.wol");
            grille.chargerLois(lawStream);
            lawDir = ruleName; // set it afterwards in case the above fails
            reloadCWogs();
        } catch(IOException e) {
            System.out.println("Error while loading laws " + ruleName);
            System.out.println(e.toString());
            if(isApplet)
				myShowStatus("Error while loading resource.");
        }
    }

    private void changePattern(String patternName) {
        if(alea.equals(patternName)) {
            grille.randomField();
            grille.repaint();
        } else {
            try {
                drawStream(nameToInputStream("wogs/" + lawDir + "/" + patternName + ".wog"));
            } catch(IOException e) {
                System.out.println("Error while loading pattern " + patternName);
                myShowStatus("Error while loading resource.");
            }
        }
    }
    
    private void reloadRules() {
		File wols = new File("wogs");
		File[] children = wols.listFiles();
		if(children != null) {
			Arrays.sort(children);
			for(int i = 0; i<children.length; i++) {
				// Get filename of file or directory
				if(children[i].isDirectory()) {
					String[] hasLaw = children[i].list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.equals("laws.wol");
						}});
					if(hasLaw != null && hasLaw.length > 0)
						cLaws.add(children[i].getName());
				}
			}
		}
	}

    private void reloadCWogs() {
		// wog.zip must contain a directory named "wogs" containing subdirectories which contains files named wogs
        // and a law file name laws.wol
        cWogs.removeAll();
        cWogs.add("Patterns");
        cWogs.add(alea);

        File wols = new File("wogs/" + lawDir);
        File[] children = wols.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".wog");
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

    private void actionStartStop() {
        if(startstopButton.getLabel().equals(startLabel)) {
            start2();
            startstopButton.setLabel(stopLabel);
        } else {
            stop();
            startstopButton.setLabel(startLabel);
        }
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
        while(gameThread != null) {
            grille.next();
            grille.repaint();
            if(genTime != -1) {
                try {
                    Thread.sleep(genTime);
                } catch(InterruptedException e) {}
			}
        }
    }

    public boolean action(Event evt, Object arg) {
        if(clear.equals(arg)) // clear
        {
            grille.clear();
            grille.repaint();
            return true;
        } else if(nextLabel.equals(arg)) { // next
            grille.next();
            grille.repaint();
            return true;
        } else if(fileLabel.equals(arg)) {
            ouvrirFichier();
            grille.repaint();
            return true;
        } else if(saveLabel.equals(arg)) {
            sauver();
            return true;
        } else if(reverseLabel.equals(arg)) {
            grille.reverse();
            grille.repaint();
            return true;
        } else if(pal1.equals(arg)) {
            grille.setPalette(0);
            grille.repaint();
            return true;
        } else if(pal2.equals(arg)) {
            grille.setPalette(1);
            grille.repaint();
            return true;
        } else if(pal3.equals(arg)) {
            grille.setPalette(2);
            grille.repaint();
            return true;
        } else if(edition.equals(arg)) {
            grille.invertEdition();
            editionButton.setLabel(endEdition);
            grille.repaint();
            return true;
        } else if(endEdition.equals(arg)) {
            grille.invertEdition();
            editionButton.setLabel(edition);
            grille.repaint();
            return true;
        } else if(gridLabel.equals(arg)) {
            grille.invertGridMode();
            grille.repaint();
            return true;
        } else if(loisLabel.equals(arg)) {
            remove(controls);
            add("North", lawsPanel);
            grille.setMode(grille.MODE_LOIS);
            grille.repaint();
            validate();
            return true;
        } else if(automateLabel.equals(arg)) {
            remove(lawsPanel);
            add("North", controls);
            grille.setMode(grille.MODE_AUTOMATE);
            grille.initPalettes();
            grille.repaint();
            validate();
            return true;
        } else if(sauverLoisLabel.equals(arg)) {
            sauverLois();
            return true;
        } else if(chargerLoisLabel.equals(arg)) {
            chargerLois();
            //grille.initPalettes();
            //grille.repaint();
            return true;
        } else {
        }
        return false;
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
            grille.reverse();
            grille.next();
            grille.reverse();
            break;
        default:
        }
    }

    public String getAppletInfo()
    {
        return "RECCA\nCopyright 2005 Laurent Orseau";
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

        FileDialog filedialog = new FileDialog(parent, "Open wog file", FileDialog.LOAD);
        filedialog.setFile("*.wog");
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

        FileDialog filedialog = new FileDialog(parent, "Save wog file", FileDialog.SAVE);
        filedialog.setFile("*.wog");
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += ".wog";

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
        filedialog.setFile("*.wol");
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
        filedialog.setFile("*.wol");
        filedialog.setVisible(true);
        if(filedialog.getFile() != null) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += ".wol";

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
}



/*
 * TODO
 * - Undo button
 * - Edit mode in diagonal
 * - Choose what symmetries to apply
 */

/*
 * Propriétés :
 * - il n'est pas possible d'avoir un debut _puis_ un cycle, chaque chose fait partie du cycle
 * ou alors ce n'est pas cyclique : puisque sinon, quand on fait reverse, on a d'abord un cycle
 * puis une fin, or si c'est un cycle on ne peut pas en sortir, donc pas de fin.
 */
