/*
 * Règles de l'automate cellulaire :
 *  - incidence de deux photons -> ils rebondissent s'ils sont perpendiculaires
 *  - incidence de trois photons -> rebond si 2 photons forment une ligne
 * */

/* Applet Problems ?
 * from: http://forums.sun.com/thread.jspa?threadID=655119
 * when the path file name contains a space, it can cause problems to load the applet!
 * moving to a directory without space works!
 *
 *
 */


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
//import java.net.MalformedURLException;
import java.net.URL;


public class Recca extends Applet implements Runnable
{
    private Grid grille;
    private Thread gameThread = null;
    private int genTime;
    private String lawDir;

    private Choice cWogs = new Choice();;

    private final String clear = "Clear";
    private final String alea = "Random";
//  private final String oscil1 = "Oscil1";
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
    //private boolean maxSpeed = false;

    Panel controls;
    Panel lawsPanel;


    public void init()
    {
        int cellSize;
        int cellCols;
        int cellRows;
        String param;

        // set background
        setBackground( new Color( 0x999999 ) );

        // read parameters from HTML
        param = getParameter("cellsize");
        if ( param == null) {
            cellSize = 5;
        } else
            cellSize = Integer.valueOf( param ).intValue();

        param = getParameter("cellcols");
        if ( param == null ) {
            cellCols = 150;
        } else
            cellCols = Integer.valueOf( param ).intValue();

        param = getParameter("cellrows");
        if ( param == null ) {
            cellRows = 150;
        } else
            cellRows = Integer.valueOf( param ).intValue();

        param = getParameter("gentime");
        if ( param == null ) {
            genTime = 0;
        } else
            genTime = Integer.valueOf( param ).intValue();

        // create components and add them to container
        grille = new Grid( cellSize, cellCols, cellRows );

        // ajouter les lois
        Choice cLaws = new Choice();
        cLaws.add("Rules");
        //try {
        /**/
            File wols = new File("wogs");
            File[] children = wols.listFiles();
            if (children != null) {
                for (int i=0; i<children.length; i++) {
                    // Get filename of file or directory
                    if(children[i].isDirectory()) {
                        String[] hasLaw = children[i].list( new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.equals("laws.wol");
                            }});
                        if(hasLaw != null && hasLaw.length > 0)
                            cLaws.add(children[i].getName());
                    }
                }
            }
            /*/
            ZipInputStream zi = new ZipInputStream(getClass().getClassLoader().getResourceAsStream("wogs.zip"));
            ZipEntry zie;
            // ajouter automatiquement tout ce qu'il y a dans les ressources
            int nb = 0;
            for (int i=0; (zie = zi.getNextEntry()) != null ;i++) {
                String dir = "wogs/";
                String lawName = "/laws.wol";
                if(zie.getName().startsWith(dir) && zie.getName().endsWith(lawName)) {
                    String name = zie.getName().substring(dir.length(), zie.getName().length()-lawName.length());
                    cLaws.add(name);
                    nb++;
                }
            }
            System.out.println( nb + " lois chargées");
        } catch (IOException e) {
            showStatus( "Error while loading resources." );
        }
        /**/

        cLaws.addItemListener( new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("loading laws " + arg);
                    changeRule(arg);
              }});

        reloadCWogs();

        cWogs.addItemListener( new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("loading pattern " + arg);
                    changePattern(arg);
              }});


        Choice cSpeed = new Choice();
        cSpeed.addItemListener( new ItemListener() {
              public void itemStateChanged(ItemEvent evt) {
                    String arg = evt.getItem().toString();
                    System.out.println("loading pattern " + arg);
                    changeSpeed(arg);
              }});

        cSpeed.add( slow );
        cSpeed.add( fast );
        cSpeed.add( faster );
        cSpeed.add( maxSpeed );
        cSpeed.select(1);
        genTime = 30;

        /*
        Choice palettes = new Choice();
        palettes.addItem( pal1 );
        palettes.addItem( pal2 );
        palettes.addItem( pal3 );
        palettes.select(0);
        grille.setPalette(0);
        */



        startstopButton = new Button( startLabel );
        startstopButton.addActionListener( new ActionListener() {
              public void actionPerformed(ActionEvent evt) {
                    //String arg = evt.getItem().toString();
                    //System.out.println("loading pattern " + arg);
                    //changeSpeed(arg);
                  actionStartStop();
            }});
        editionButton = new Button( edition );

        controls = new Panel();
        controls.add( cLaws );
        controls.add( cWogs );
        controls.add( new Button( clear ));
        controls.add( startstopButton );
        controls.add( new Button( nextLabel ));
        controls.add( new Button(reverseLabel) );
        controls.add( new Button(fileLabel) );
        controls.add( new Button(saveLabel) );
        controls.add( cSpeed );
        //controls.add( palettes );
        controls.add( editionButton );
        controls.add( new Button( gridLabel ));
        controls.add( new Button( loisLabel ));


        // Le panneau des lois
        //Choice choiceLaws = new Choice();
        //choiceLaws.add( "laws1.txt" );

        lawsPanel = new Panel();
        //lawsPanel.add( choiceLaws );
        lawsPanel.add( new Button( chargerLoisLabel ));
        lawsPanel.add( new Button( sauverLoisLabel ));
        lawsPanel.add( new Button( automateLabel ));


        setLayout(new BorderLayout());
        //add( "North", lawsPanel);
        add( "North", controls );
        grille.setMode(grille.MODE_AUTOMATE);
        add( "Center", grille );
        setVisible(true);
        resize( getPreferredSize() );
        validate();


        // We can now give parameters to the URL!!
        // example:
        // file:///D:/Mes%20documents/Projets/RECCA/RECCA.html?rule=Strings&pattern=12&speed=Fast&start=true
        // with direct link to the applet:
        // file:///D:/Mes%20documents/Projets/RECCA/RECCA.html?rule=Strings&pattern=12&speed=Fast&start=true#applet

        System.out.println("href =" + getDocumentBase());
        String ruleName = getQueryValue( "rule" );
        System.out.println("ruleName = " + ruleName);
        if ( ruleName != null )
            changeRule( ruleName );
        String patternName = getQueryValue( "pattern" );
        System.out.println("patternName = " + patternName);
        if ( patternName != null )
            changePattern( patternName );
        String speedValue = getQueryValue( "speed" );
        System.out.println("speedValue = " + speedValue);
        if ( speedValue != null )
            changeSpeed( speedValue );
        String startValue = getQueryValue( "start" );
        System.out.println("startValue = " + startValue );
        if ( startValue != null )
            actionStartStop();

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

    private void changeRule(String ruleName) {
        /**/
        try {
            //FileInputStream laws = new FileInputStream("wogs/" + ruleName + "/laws.wol");
            DataInputStream laws = new DataInputStream(
                    (new URL( getCodeBase(), "wogs/" + ruleName + "/laws.wol" )).openStream() ) ;

            lawDir = ruleName;
            grille.chargerLois(laws);
            reloadCWogs();
        } catch (IOException e) {
            System.out.println("Error while loading laws " + ruleName);
            showStatus("Error while loading resource.");
        }
        /*/
        ZipInputStream zi = new ZipInputStream(getClass().getClassLoader()
                .getResourceAsStream("wogs.zip"));
        ZipEntry zie;
        try {
            do {
                zie = zi.getNextEntry();
            } while (zie != null && !zie.getName().equals("wogs/" + ruleName + "/laws.wol"));
            if (zie != null){
                lawDir = ruleName;
                grille.chargerLois(zi);
                reloadCWogs();
            }
            else
                showStatus("Laws not found.");
        } catch (IOException e) {
            showStatus("Error while loading resource.");
        }
        /**/
    }

    private void changePattern(String patternName) {
        if (alea.equals(patternName)) // random field
        {
            grille.randomField();
            grille.repaint();
        } else {
            /**/
            try {
                //FileInputStream pat = new FileInputStream("wogs/" + lawDir + "/" + patternName + ".wog");
                DataInputStream pat = new DataInputStream(
                        (new URL( getCodeBase(), "wogs/" + lawDir + "/" + patternName + ".wog" )).openStream() ) ;
                drawStream(pat);
            } catch (IOException e) {
                System.out.println("Error while loading pattern " + patternName);
                showStatus("Error while loading resource.");
            }
            /*/
            ZipInputStream zi = new ZipInputStream(getClass().getClassLoader()
                    .getResourceAsStream("wogs.zip"));
            ZipEntry zie;
            try {
                do {
                    zie = zi.getNextEntry();
                } while (zie != null && !zie.getName().equals("wogs/" + lawDir + "/" + patternName + ".wog"));
                if (zie != null)
                    drawStream(zi);
            } catch (IOException e) {
                showStatus("Error while loading resource.");
            }
            /**/
        }
    }

    private void reloadCWogs() {
        /*
         * wog.zip must contain a directory named "wogs" containing subdirectories which contains files named wogs
         * and a law file name laws.wol
         */
        cWogs.removeAll();
        cWogs.add( "Patterns" );
        cWogs.add( alea );

        /**/
        File wols = new File("wogs/" + lawDir);
        File[] children = wols.listFiles( new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".wog");
            }});
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                String name = children[i].getName();
                System.out.println(children[i].getName());
                cWogs.add(name.substring(0, name.length()-4));
                //nb++;
            }
        }
        /*/
        try {
            ZipInputStream zi = new ZipInputStream(getClass().getClassLoader().getResourceAsStream("wogs.zip"));
            int nb=0;
            ZipEntry zie;
            // ajouter automatiquement tout ce qu'il y a dans les ressources
            for (int i=0; (zie = zi.getNextEntry()) != null ;i++) {
                String dir = "wogs/" + lawDir + "/";
                if(zie.getName().startsWith(dir) && zie.getName().endsWith(".wog")) {
                    String name = zie.getName().substring(dir.length(), zie.getName().length()-4);
                    System.out.println(zie.getName());
                    cWogs.add(name);
                    nb++;
                }
            }
            System.out.println( nb + " éléments chargés");
        } catch (IOException e) {
            showStatus( "Error while loading resources." );
        }
        /**/
        validate();
    }

    private void changeSpeed(String arg) {
        if (slow.equals(arg))
            genTime = 200;
        else if (fast.equals(arg))
            genTime = 30;
        else if (faster.equals(arg))
            genTime = 0;
        else if (maxSpeed.equals(arg))
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
        while (gameThread != null) {
            grille.next();
            grille.repaint();
            if(genTime != -1)
                try {
                    Thread.sleep( genTime );
                } catch (InterruptedException e){}
        }
    }

    public boolean action(Event evt, Object arg) {
        if (clear.equals(arg)) // clear
        {
            grille.clear();
            grille.repaint();
            return true;
        } else if (nextLabel.equals(arg)) // next
        {
            grille.next();
            grille.repaint();
            return true;
        } else if (fileLabel.equals(arg)) {
            ouvrirFichier();
            grille.repaint();
            return true;
        } else if (saveLabel.equals(arg)) {
            sauver();
            return true;
        } else if (reverseLabel.equals(arg)) {
            grille.reverse();
            grille.repaint();
            return true;
        } else if (pal1.equals(arg)) {
            grille.setPalette(0);
            grille.repaint();
            return true;
        } else if (pal2.equals(arg)) {
            grille.setPalette(1);
            grille.repaint();
            return true;
        } else if (pal3.equals(arg)) {
            grille.setPalette(2);
            grille.repaint();
            return true;
        } else if (edition.equals(arg)) {
            grille.invertEdition();
            editionButton.setLabel(endEdition);
            grille.repaint();
            return true;
        } else if (endEdition.equals(arg)) {
            grille.invertEdition();
            editionButton.setLabel(edition);
            grille.repaint();
            return true;
        } else if (gridLabel.equals(arg)) {
            grille.invertGridMode();
            grille.repaint();
            return true;
        } else if (loisLabel.equals(arg)) {
            remove( controls );
            add( "North", lawsPanel);
            grille.setMode(grille.MODE_LOIS);
            grille.repaint();
            validate();
            return true;
        } else if (automateLabel.equals(arg)) {
            remove( lawsPanel );
            add( "North", controls);
            grille.setMode(grille.MODE_AUTOMATE);
            grille.initPalettes();
            grille.repaint();
            validate();
            return true;
        } else if (sauverLoisLabel.equals(arg)) {
            sauverLois();
            return true;
        } else if (chargerLoisLabel.equals(arg)) {
            chargerLois();
            //grille.initPalettes();
            //grille.repaint();
            return true;
        } else {
        }
        return false;
    }

    public String getAppletInfo()
    {
        return "RECCA\nCopyright 2005 Laurent Orseau";
    }


    // draws the shape to canvas
    public void drawShape( int shapeWidth, int shapeHeight, int shape[][] ) {
        if ( !grille.drawShape( shapeWidth, shapeHeight, shape ) )
            showStatus( "Shape too large." );
        else {
            showStatus( "" );
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
        if ( !grille.drawShape( shapeWidth, shapeHeight, shapeb ) )
            showStatus( "Shape too large." );
        else {
            showStatus( "File correctly opened. " +shapeWidth +" "+ shapeHeight );
            grille.repaint();
        }

    }

    // draws a file to canvas
    public void drawFichier(String fichier) {
        try {
            drawStream(new FileInputStream(fichier));

        } catch(FileNotFoundException e) {
            showStatus( "File not found." );
        } catch(IOException e) {
            showStatus( "Error while reading file." );
        }

    }

    // draws an URL to canvas
    public void drawURL(URL url) {
        try {
            drawStream(url.openStream());

        } catch(FileNotFoundException e) {
            showStatus( "File not found." );
        } catch(IOException e) {
            showStatus( "Error while reading file." );
        }

    }

    private void ouvrirFichier() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog( parent, "Open wog file", FileDialog.LOAD );
        filedialog.setFile("*.wog");
        filedialog.setVisible(true);
        if ( filedialog.getFile() != null ) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            drawFichier(filepath);
        }
    }

    private void sauver() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog( parent, "Save wog file", FileDialog.SAVE );
        filedialog.setFile("*.wog");
        filedialog.setVisible(true);
        if ( filedialog.getFile() != null ) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += ".wog";

            OutputStream fileWriter;
            try {
                fileWriter = new FileOutputStream(filepath);
                grille.sauver(fileWriter);
                fileWriter.close();
            } catch (FileNotFoundException e) {
                showStatus( "Cannot open file for writing." );
            } catch (IOException e) {
                showStatus( "Error saving file." );
            }
        }

    }

    private void chargerLois() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog( parent, "Open laws file", FileDialog.LOAD );
        filedialog.setFile("*.wol");
        filedialog.setVisible(true);
        if ( filedialog.getFile() != null ) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            try {
                grille.chargerLois(new FileInputStream(filepath));
            } catch (FileNotFoundException e) {
                showStatus( "Cannot open file for writing." );
            } catch (IOException e) {
                showStatus( "Error saving file." );
            }
        }
    }

    private void sauverLois() {
        String filename;
        String filepath;

        Frame parent = new Frame();

        FileDialog filedialog = new FileDialog( parent, "Save laws file", FileDialog.SAVE );
        filedialog.setFile("*.wol");
        filedialog.setVisible(true);
        if ( filedialog.getFile() != null ) {
            filename = filedialog.getFile();
            filepath = filedialog.getDirectory()+filename;
            if(filename.indexOf('.') == -1)
                filepath += ".wol";

            OutputStream fileWriter;
            try {
                fileWriter = new FileOutputStream(filepath);
                grille.sauverLois(fileWriter);
                fileWriter.close();
            } catch (FileNotFoundException e) {
                showStatus( "Cannot open file for writing." );
            } catch (IOException e) {
                showStatus( "Error saving file." );
            }
        }

    }
}



/*
 * Bouton Undo !
 * Edition en mode diagonale OU normale
 * Afficher les lignes de couleur différente pour se repérer (genre tous les 16, 8, 32...)
 */

/*
 * Propriétés :
 * - il n'est pas possible d'avoir un debut _puis_ un cycle, chaque chose fait partie du cycle
 * ou alors ce n'est pas cyclique : puisque sinon, quand on fait reverse, on a d'abord un cycle
 * puis une fin, or si c'est un cycle on ne peut pas en sortir, donc pas de fin.
 */
