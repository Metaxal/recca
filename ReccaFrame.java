import java.awt.*;
import java.awt.event.*;

public class ReccaFrame extends Frame {
	
	public ReccaFrame() {
		super("Recca");
		
		Recca ReccaApplet = new Recca();
		// Call applet's init method (since Java App does not
		// call it as a browser automatically does)
		ReccaApplet.init(false);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				dispose();
			}});
			
		add(ReccaApplet, BorderLayout.CENTER);
		pack(); // set window to appropriate size (for its elements)
	}
 
	public static void main(String[] args) {
		Frame myFrame = new ReccaFrame();
			
		myFrame.setVisible(true);
	}

}
