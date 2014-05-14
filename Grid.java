import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.*;
import java.util.*;
/*
 * This automaton class is based on Fredkin'd Billiard Ball machine with a Moore neighbourhood.
 *
 */

/**
 * @author lorseau
 *
 */
class Grid extends Canvas
{
	private int mode;
	//private int palette;
	private int cellSize;
	private int cellRows;
	private int cellCols;
	private int cells1[][];
	private int cells2[][];
	private int cellsT[][];
	private int xEdit, yEdit, xEdit2, yEdit2;
	private boolean edition;
	private boolean gridMode;
	private Image offScreenImage = null;
	private Graphics offScreenGraphics;
	
	private int trans[] = new int[512]; 
	/*
	{
			0,0,0,0,0,80,0,0,0,0,160,0,0,0,0,0,0,0,0,145,
			65,81,0,0,0,49,0,0,0,0,0,0,0,0,0,38,0,0,35,0,
			130,0,162,0,0,0,0,0,0,25,98,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,20,0,0,0,84,76,0,0,0,0,0,70,0,0,0,
			5,21,0,0,69,170,0,0,0,0,0,0,0,0,0,0,0,0,50,0,
			
			196,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,40,0,0,0,0,0,0,200,168,0,
			152,0,0,0,0,19,0,0,0,0,0,0,140,0,0,0,0,0,0,0,
			10,0,42,0,0,0,0,0,138,0,85,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,100,0,0,0,
			
			137,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	*/
	private int tabEnumInt[]; // transformation table between linearly ordered and order by sum (via EnumInt)
	private Color[][] palettes; 
	private Color palette[];
	
	static int dirs[][] = {{-1,0,1,1,1,0,-1,-1,0}, {-1,-1,-1,0,1,1,1,0,0}};
	/*
	 * 1 2 3
	 * 8 9 4
	 * 7 6 5
	 */
	
	public final int MODE_LOIS 		= 0;
	public final int MODE_AUTOMATE 	= 1;
	
	public Grid( int cellSize, int cellCols, int cellRows ) {
		mode = MODE_AUTOMATE;
		cells1 = new int[cellCols][cellRows];
		cells2 = new int[cellCols][cellRows];
		this.cellSize = cellSize;
		this.cellCols = cellCols;
		this.cellRows = cellRows;
		
		xEdit = cellCols/2;
		yEdit = cellRows/2;
		xEdit2 = cellCols/2;
		yEdit2 = cellRows/2;
		edition = false;
		
		setBounds(0, 0, cellSize*cellCols-1, cellSize*cellRows-1);
		clear();
		
		gridMode = false;
		
		/* initial shape
		for(int x=0; x < 8; x++)
			for(int y=0; y < 8; y++)
				cells1[(cellCols/4-4+x)][(cellRows/4-4+y)] = 255;
		*/
		
		// any 0 is replaced by the same value as the cell: files are more legible 
		for(int i=0; i < 512; i++)
			if(trans[i] == 0)
				trans[i] = i;

		tabEnumInt = new int[512];
		EnumInt ei = new EnumInt(9);
		for(int i = 0; i < 512; i++)
			tabEnumInt[i] = ei.next();
		
		initPalettes();
		palette = palettes[0];
	}
	
	public void initPalettes() {
		palettes = new Color[3][512];
		
		int pal = 0;
		for( int i=0; i < 512; i++ ) {
			palettes[pal][i] = Color.WHITE;
		}

		pal++;
		int[][] rgb = {{0,0,0},{96, 160, 255},{255, 96, 160},{160, 255, 96},{96, 255, 160},
				{160, 96, 255},{255, 160, 96},{255, 255, 0},{127,127,127},{255, 255, 255}};
		
		for( int i=0; i < 512; i++ ) {
				int n = nbUn(i);
				palettes[pal][i] = new Color(rgb[n][0], rgb[n][1], rgb[n][2]);
		}
		
		pal++;
		for( int i=0; i < 512; i++ ) {
			if(trans[i] == i)
				palettes[pal][i] = Color.MAGENTA;
			else
				palettes[pal][i] = Color.WHITE;
		}
	}
	
	public int tourneGaucheInt(int i, int n) {
		return ((i << n) & 255) | (i >> (8-n));
	}
	
	public void setMode(int mode) {
		this.mode = mode;
		if(mode == MODE_AUTOMATE)
			cellSize = 5;
		else
			cellSize = 9;
	}

	public synchronized void invertEdition() {
		edition = !edition;
		//System.out.println("edition = "+edition);
	}
	
	public synchronized void invertGridMode() {
		gridMode = !gridMode;
		//System.out.println("edition = "+edition);
	}
	
	public synchronized boolean keyUp(java.awt.Event evt, int key) {
		return false;
	}
	
	public synchronized void modifZone(int n, int x1, int y1, int x2, int y2) {
		if(n == 0)
			for(int i=x1; i < x2; i++)
				for(int j=y1; j < y2; j++)
					cells1[i][j] = 0;
		else
			for(int i=x1; i < x2; i++)
				for(int j=y1; j < y2; j++)
					cells1[i][j] = cells1[i][j] ^ n;
	}
	
	/*
		cell before transformation and after could be put on one same cell.
		white = photons belongs to before and after
		red = belongs to before, not to after
		blue = belongs to after, not to before
		Simpler? Not sure...
	*/
	/*
	 * ... and modifies all the symmetric cells at the same time.
	 */
	// THERE MAY BE SOME PROBLEMS HERE !
	// if some cells are modified several times because of symmetries ! 
	public synchronized void modifCellLaws(int n, int x, int y) {
		int x2 = (x-1-2)/5;
		int y2 = (y-1)/2;
		int c = tabEnumInt[y2*16+x2];
		System.out.println("x="+x+" y="+y+" x2="+x2+" y2="+y2);

		if(n == 0)
			trans[c] = 0;
		else
			trans[c] = trans[c] ^ n; // bitwise XOR
		
		int[] modified = new int[512]; // init to 0
		modified[c] = 1;
		
		// apply the same transformation to all the rotation of the cell
		int c2 = c;
		for(int i = 0; i < 8; i++) {
			int cc = rotation9(c2);
			if(modified[cc] == 0) {
				trans[cc] = rotation9(trans[c2]);
				modified[cc] = 1;
			}
			c2 = cc; 
		}
		// now do the same with the symmetric of the cell
		 c2 = hzSymetry9(c);
		if(modified[c2] == 0) {
			trans[c2] = hzSymetry9(trans[c]);
			modified[c2] = 1;
		}
		for(int i = 0; i < 8; i++) {
			int cc = rotation9(c2);
			if(modified[cc] == 0) {
				trans[cc] = rotation9(trans[c2]);
				modified[cc] = 1;
			}
			c2 = cc; 
		}
	}
	
	/*
	 * given an 9 bit cell, find all the rotations
	 * the 9th bit, corresponding to the cell not moving, cannot be changed
	 */
	public synchronized int rotation9(int c) {
		return ((c << 1) & 255) | (c & 256) | ((c >> 7) & 1);
	}
	
	public synchronized int hzSymetry9(int c) {
		return (c & 256) | (c & 128) | (c & 8) |
		((c & 1) << 6) | ((c & 2) << 4) | ((c & 4) << 2) |
		((c & 64) >> 6)  | ((c & 32) >> 4) | ((c & 16) >> 2)
		;
	}
	
	public synchronized boolean keyDown(java.awt.Event evt, int key) {
		System.out.println("touche = "+key);
		if(!edition)
			return true;

		int xi = Math.min(xEdit, xEdit2);
		int yi = Math.min(yEdit, yEdit2);
		int dx = Math.abs(xEdit2-xEdit)+1;
		int dy = Math.abs(yEdit2-yEdit)+1;
		
		int x, y;
		if(evt.shiftDown()) {
			x = xEdit2;
			y = yEdit2;
		} else {
			x = xEdit;
			y = yEdit;
		}
		
		int tm[] = {0, 64, 32, 16, 128, 256, 8, 1, 2, 4};
		if(key > 47 && key < 58)
			if(mode == MODE_AUTOMATE)
				modifZone(tm[key-48], xi, yi, xi+dx, yi+dy);
			else if(mode == MODE_LOIS)
				modifCellLaws(tm[key-48], xi, yi);

		boolean modif = key >1003 && key < 1008;
		switch(key) {
		case 1004: // haut
			x = xd(x, 1);
			y = yd(y, 1);
			break;
		case 1005: // bas
			x = xd(x, 5);
			y = yd(y, 5);
			break;
		case 1006: //gauche
			x = xd(x, 7);
			y = yd(y, 7);
			break;
		case 1007: //droite
			x = xd(x, 3);
			y = yd(y, 3);
			break;
		case 3: // Ctrl-c
			cellsT = new int[dx][dy];
			for(int i=0; i < dx; i++)
				for(int j=0; j < dy; j++)
					cellsT[i][j] = cells1[i+xi][j+yi]; 
			break;
		case 22: // Ctrl-v
			for(int i=0; i < Math.min(cellsT.length, cellCols-xEdit); i++)
				for(int j=0; j < Math.min(cellsT[0].length, cellRows-yEdit); j++)
					if(cellsT[i][j] != 0)
						cells1[xEdit+i][yEdit+j] = cellsT[i][j]; 
			break;
		default:
			//System.out.println("mauvaise pioche...");
		}
		if(evt.shiftDown()) {
			xEdit2 = x;
			yEdit2 = y;
		} else {
			xEdit = x;
			yEdit = y;
			if(modif) {
				xEdit2 = xEdit;
				yEdit2 = yEdit;
			}
		}
		
		//ShowStatus("("+xEdit+","+yEdit+") -> ("+xEdit2+","+yEdit2+")");
		repaint();
		return false;
	}
	
	public synchronized boolean mouseDown(java.awt.Event evt, int x, int y) {
		int x2 = x/cellSize;
		int y2 = y/cellSize;
		if(x2<0) x2 = 0;
		if(x2>=cellCols) x2 = cellCols-1;
		if(y2<0) y2 = 0;
		if(y2>=cellRows) y2 = cellCols-1;
		xEdit = x2;
		yEdit = y2;

		setEdit2(x, y);
		/*// toggle cell
		 try {
		 cells1[x/cellSize][y/cellSize] =  (1-cellUnderMouse);
		 } catch ( java.lang.ArrayIndexOutOfBoundsException e ) {}
		 */
		repaint();
		return true;
	}
	
	public synchronized void setEdit2(int x, int y) {
		int x2 = x/cellSize;
		int y2 = y/cellSize;
		if(x2<0) x2 = 0;
		if(x2>=cellCols) x2 = cellCols-1;
		if(y2<0) y2 = 0;
		if(y2>=cellRows) y2 = cellCols-1;
		xEdit2 = x2;
		yEdit2 = y2;
	}

	public synchronized boolean mouseUp(java.awt.Event evt, int x, int y) {
		setEdit2(x, y);
		repaint();
		return true;
	}
	
	public synchronized boolean mouseDrag(java.awt.Event evt, int x, int y) {
		setEdit2(x, y);
		repaint();
		return true;
	}
	
	public synchronized void update( Graphics g)
	{
		Dimension d = getSize();
		if((offScreenImage == null) ) {
			offScreenImage = createImage( d.width, d.height );
			offScreenGraphics = offScreenImage.getGraphics();
		}
		paint(offScreenGraphics);
		g.drawImage( offScreenImage, 0, 0, null );
	}
	
	public void setPalette(int pal) {
		palette = palettes[pal];
	}
	
	private void paintGrid(Graphics g) {
		g.setColor( Color.getHSBColor(0.0f, 1.0f, 0.25f));
		for( int x=0; x<cellCols*cellSize; x+=8*cellSize )
			g.fillRect( x, 0, cellSize-1, cellSize*cellRows-1 );
		for( int y=0; y<cellRows*cellSize; y+=8*cellSize )
			g.fillRect( 0, y, cellSize*cellCols-1, cellSize-1 );

		g.setColor( Color.getHSBColor(0.0f, 1.0f, 0.5f));
		for( int x=0; x<cellCols*cellSize; x+=16*cellSize )
			g.fillRect( x, 0, cellSize-1, cellSize*cellRows-1 );
		for( int y=0; y<cellRows*cellSize; y+=16*cellSize )
			g.fillRect( 0, y, cellSize*cellCols-1, cellSize-1 );

		g.setColor( Color.getHSBColor(0.0f, 1.0f, 0.75f));
		for( int x=0; x<cellCols*cellSize; x+=32*cellSize )
			g.fillRect( x, 0, cellSize-1, cellSize*cellRows-1 );
		for( int y=0; y<cellRows*cellSize; y+=32*cellSize )
			g.fillRect( 0, y, cellSize*cellCols-1, cellSize-1 );
	}

	private void paintCell(Graphics g, int x2, int y2, int dcell, int c) {
		//g.fillRect( x2, y2, 1, 1);
		int i = 0;
		for(; c > 0 && i < 8; i++, c = c >> 1)
			if( (c & 1) == 1)
				g.drawLine(x2, y2, x2+dirs[0][i]*dcell, y2+dirs[1][i]*dcell);
		int dcell2 = dcell >> 1;		
		if( (c & 1) == 1)
			g.fillRect(x2-dcell2, y2-dcell2, dcell+1, dcell+1);
	}

	private void paintAutomate(Graphics g) {
		int dcell = cellSize/2;
		for( int y=0; y<cellRows; y++ )
			for( int x=0; x<cellCols; x++ ) {
				int c = cells1[x][y]; 
				if ( c  > 0 ) {
					g.setColor( palette[c] ); 
					int x2 = x*cellSize+dcell;
					int y2 = y*cellSize+dcell;
					paintCell(g, x2, y2, dcell, c);
					/*
					g.fillRect( x2, y2, 1, 1);
					for(int i = 0; c > 0; i++, c = c >> 1)
						if( (c & 1) == 1)
							g.drawLine(x2, y2, x2+dirs[0][i]*dcell, y2+dirs[1][i]*dcell);
							*/
				}
			}
	}
	
	private void paintLois(Graphics g) {
		int dcell = cellSize/2;
		for( int y=0; y<32; y++ )
			for( int x=0; x<16; x++ ) {
				g.setColor( Color.white );
				int cell = tabEnumInt[y*16+x];
				int c = cell;
				if ( c  > 0 ) {
					int x2 = (x*5+1)*cellSize+dcell;
					int y2 = (y*2+1)*cellSize+dcell;
					paintCell(g, x2, y2, dcell, c);
					/*
					g.fillRect( x2, y2, 1, 1);
					for(int i = 0; c > 0; i++, c = c >> 1)
						if( (c & 1) == 1)
							g.drawLine(x2, y2, x2+dirs[0][i]*dcell, y2+dirs[1][i]*dcell);
							*/
				}

				//System.out.println("y="+y+" x="+x+" y*16+x="+(y*16+x)+" cell="+cell);
				int c2 = trans[cell]; 
				if(c2 != cell)
					g.setColor( Color.red );
				if ( c2  > 0 ) {
					int x2 = (x*5+1+2)*cellSize+dcell;
					int y2 = (y*2+1)*cellSize+dcell;
					paintCell(g, x2, y2, dcell, c2);
					/*
					g.fillRect( x2, y2, 1, 1);
					for(int i = 0; c2 > 0; i++, c2 = c2 >> 1)
						if( (c2 & 1) == 1)
							g.drawLine(x2, y2, x2+dirs[0][i]*dcell, y2+dirs[1][i]*dcell);
							*/
				}
			}
	}

	public void paint(Graphics g) {
		// draw background (MSIE doesn't do that)
		g.setColor( Color.black );
		g.fillRect( 0, 0, cellSize*cellCols-1, cellSize*cellRows-1 );
		// draw grid
		g.setColor( getBackground() );
		if(gridMode)
			paintGrid(g);
			
		// draw populated cells
	
		if(edition)
		{
			g.setColor( Color.blue );
			int x = Math.min(xEdit, xEdit2)*cellSize;
			int y = Math.min(yEdit, yEdit2)*cellSize;
			int w = (Math.abs(xEdit2-xEdit)+1)*cellSize;
			int l = (Math.abs(yEdit2-yEdit)+1)*cellSize;
			g.fillRect(x, y, w, l);
		}
		
		if(mode == MODE_AUTOMATE) {
			paintAutomate(g);
		} else if(mode == MODE_LOIS) {
			paintLois(g);
		}
	}
	
	public int nbUn(int c) {
		int n = 0;
		for(int i=0; i < 9; i++) {
			n += c & 1;
			c = (c >> 1);
		}
		
		return n;
	}
	
	// clears canvas
	public synchronized void clear() {
		for( int x=0; x<cellCols; x++ ) {
			for( int y=0; y<cellRows; y++ ) {
				cells1[x][y] = 0;
			}
		}
	}
	
	public synchronized int xd(int x, int d) {
		x += dirs[0][d];
		if(x < 0)
			x += cellCols;
		else if(x >= cellCols)
			x -= cellCols;
		
		return x; 
	}
	
	public synchronized int yd(int y, int d) {
		y += dirs[1][d];
		if(y < 0)
			y += cellRows;
		else if(y >= cellRows)
			y -= cellRows;
		
		return y;
	}
	
	// create next generation of shape
	public synchronized void next() {
		int x;
		int y;
		
		// clear the buffer
		for( x=0; x<cellCols; x++ ) {
			for( y=0; y<cellRows; y++ ) {
				cells2[x][y] = 0;
			}
		}
		
		// move photons 
		for( x=0; x<cellCols; x++ ) {
			for( y=0; y<cellRows; y++ ) {
				int c = cells1[x][y]; 
				if(c > 0){
					for(int i=1, j=0; i < 512; i*=2, j++) {
						if((c & i) > 0)
							cells2[xd(x, j)][yd(y, j)] |= i;
					} 
				}
			}
		}

		// transforms cells
		for( x=0; x<cellCols; x++ )
			for( y=0; y<cellRows; y++ )
				cells2[x][y] = trans[cells2[x][y]];  
		
		int[][] tabtemp = cells1;
		cells1 = cells2;
		cells2 = tabtemp; 
	}
	
	// draws shape in cells
	// returns false if shape doesn't fit
	public synchronized boolean drawShape( int shapeWidth, int shapeHeight, int shape[][] ) {
		int xOffset;
		int yOffset;
		
		if ( shapeWidth>cellCols || shapeHeight>cellRows )
			return false; // shape doesn't fit on canvas
		
		// center the shape
		xOffset = (cellCols-shapeWidth)/2;
		yOffset = (cellRows-shapeHeight)/2;
		clear();
		for(int x=0; x < shapeWidth; x++)
			for(int y=0; y < shapeHeight; y++)
				cells1[xOffset+x][yOffset+y] = shape[x][y];
		return true;
	}
	
	public synchronized boolean drawShape( int shapeWidth, int shapeHeight, int shape[] ) {
		int xOffset;
		int yOffset;
		
		if ( shapeWidth>cellCols || shapeHeight>cellRows )
			return false; // shape doesn't fit on canvas
		
		// center the shape
		xOffset = (cellCols-shapeWidth)/2;
		yOffset = (cellRows-shapeHeight)/2;
		clear();
		for(int x=0; x < shapeWidth; x++)
			for(int y=0; y < shapeHeight; y++)
			{
				int c = shape[x+y*shapeWidth];
				if(c < 0)
					c += 256;
				cells1[xOffset+x][yOffset+y] = c; 
			}
		return true;
	}
	
	public synchronized void randomField() {
		Random r = new Random();
		for( int x=0; x<cellCols; x++ ) {
			for( int y=0; y<cellRows; y++ ) {
				if(r.nextInt(100) < 2)
					cells1[x][y] = r.nextInt(256);
			}
		}
	}
	
	public synchronized void reverse() {
		int[] trans2 = new int[512];
		for(int i = 0; i < 512; i++)
			trans2[i] = -1;
		
		for(int i = 0; i < 512; i++) {
			if(trans2[trans[i]] >= 0) //  && trans2[trans[i]] != i)
				System.out.println("Warning! the laws are not reversible ! law "+i+" and "+trans[i]);
			trans2[trans[i]] = i;
			//System.out.println(trans[i] + " -> " + i + "  ");
		}
		
		// "heal" non-reversible laws
		for(int i = 0; i < 512; i++)
			if(trans2[i] == -1) {
				trans2[i] = i;
			}
			
		trans = trans2;
		
		
		for( int x=0; x<cellCols; x++ )
			for( int y=0; y<cellRows; y++ )
			{
				int c = cells1[x][y];
				cells1[x][y] = trans[(c >> 4) + ((c & 15) << 4)];
			}
	}
	
	public void sauver(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		/*if(edition && !(xEdit2 == xEdit && yEdit2 == yEdit)) { // ne sauver que la selection
			int x = Math.min(xEdit, xEdit2);
			int y = Math.min(yEdit, yEdit2);
			int x2 = Math.max(xEdit, xEdit2);
			int y2 = Math.max(yEdit, yEdit2);
			int w = (Math.abs(xEdit2-xEdit)+1);
			int l = (Math.abs(yEdit2-yEdit)+1);

			os.write(w >> 24);		
			os.write(w >> 16);		
			os.write(w >> 8);		
			os.write(w);
			
			os.write(l >> 24);		
			os.write(l >> 16);		
			os.write(l >> 8);		
			os.write(l);

			for(int j=y; j <= y2; j++)
				for(int i=x; i <= x2; i++)
					os.write(cells1[i][j]);
		}
		else */{
			dos.writeInt(cellCols >> 24);		
			dos.writeInt(cellCols >> 16);		
			dos.writeInt(cellCols >> 8);		
			dos.writeInt(cellCols);
			
			dos.writeInt(cellRows >> 24);		
			dos.writeInt(cellRows >> 16);		
			dos.writeInt(cellRows >> 8);		
			dos.writeInt(cellRows);
			
			for(int j=0; j < cellRows; j++)
				for(int i=0; i < cellCols; i++)
					dos.writeInt(cells1[i][j]);
		}
	}
	/*
	public void chargerLoisTxt(String filename) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader( filename ) );
		String lineIn = null;
		StringTokenizer st = null;
		String token = null;
		int n = 0;
		while( ( lineIn = br.readLine() ) != null ) {
			st = new StringTokenizer( lineIn, " \t\r" );
			while( st.hasMoreTokens() ) {
				token = st.nextToken();
				trans[n] = Integer.parseInt(token); 
				System.out.print(trans[n]+" ");
				n++;
				if(n % 32 == 0)
					System.out.println("");
			}
		}
		System.out.println("n = "+n);
		repaint();
	}*/
	
	public void chargerLois(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		for(int i = 0; i < 512; i++)
			trans[i] = dis.readInt();
		
		repaint();
	}

	public void sauverLois(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		for(int i = 0; i < 512; i++)
			dos.writeInt(trans[i]);
	}

	
}
