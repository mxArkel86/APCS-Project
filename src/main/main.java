package main;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.Timer;

import javax.swing.JFrame;

public class main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1280, 720);
		frame.setResizable(false);
		
		Application app = new Application();
		frame.add(app);
		app.Initialize(frame, 1280, 720);
		
		app.setSize(frame.getWidth(), frame.getHeight());
		frame.repaint();
		
		frame.setVisible(true);
	}
	
}
