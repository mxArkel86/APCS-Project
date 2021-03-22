package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Application extends JPanel implements KeyListener {
	BufferedImage PLAYER_RES = null;
	ArrayList<BufferedImage> asteroidTextures = new ArrayList<BufferedImage>();
	BufferedImage space;
	double PLAYER_ACCELERATION = 0.05;
	double PLAYER_TURN_SPEED = 0.03;
	double FRICTION_CONST = 0.02;
	int ASTEROID_BUFFER = 61;
	int ASTEROID_SIZE_MAX = 110;
	int ASTEROID_SIZE_MIN = 25;
	double ASTEROID_SPEED_MAX = 2.25;
	double ASTEROID_SPEED_MIN = 0.2;
	int SIZE_X;
	int SIZE_Y;
	int PLAYER_SIZE = 50;
	ReentrantLock mutex = new ReentrantLock();
	
	Timer timer = null;
	int score = 500;
	double player_rot = Math.PI/4;
	boolean game_lost = false;
	boolean enable_continue = false;
	ArrayList<Asteroid> asteroids = new ArrayList<Asteroid>();
	double player_velocity_x = 0;
	double player_velocity_y = 0;
	double player_x = 200;
	double player_y = 200;
	boolean w_pressed = false;
	boolean s_pressed = false;
	boolean a_pressed = false;
	boolean d_pressed = false;
	boolean right_pressed = false;
	boolean left_pressed = false;
	boolean space_pressed = false;
	
	public void Initialize(JFrame frame, int x, int y) {
		PLAYER_RES = GetImage(new File("bin/res/ship.png"));
		asteroidTextures.add(GetImage(new File("bin/res/asteroid.png")));
		asteroidTextures.add(GetImage(new File("bin/res/asteroid2.png")));
		space = GetImage(new File("bin/res/space.png"));
		JPanel panel = this;
		SIZE_X = x;
		SIZE_Y = y;
		player_x = x/2;
		player_y = y/2;
		
		frame.addKeyListener(this);
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				if(game_lost==false) {
				if(right_pressed&!left_pressed) {
					player_rot+=PLAYER_TURN_SPEED;
				}
				if(left_pressed&!right_pressed) {
					player_rot-=PLAYER_TURN_SPEED;
				}
				if(player_rot>2*Math.PI)
					player_rot-=2*Math.PI;
				if(player_rot<0)
					player_rot+=2*Math.PI;
				
				double front = 0;
				double side = 0;
				if(w_pressed)
					front+=1;
				if(s_pressed)
					front-=1;
				if(a_pressed)
					side-=1;
				if(d_pressed)
					side+=1;
				
				double change_y = Math.sin(player_rot)*front+Math.cos(player_rot)*side;
				double change_x = Math.cos(player_rot)*front-Math.sin(player_rot)*side;
					
				player_velocity_x+=change_x*PLAYER_ACCELERATION;
				player_velocity_y+=change_y*PLAYER_ACCELERATION;
					
				
				if(player_velocity_x!=0) {
					if(Math.abs(player_velocity_x)>FRICTION_CONST)
						player_velocity_x+=Math.copySign(1, player_velocity_x)*-FRICTION_CONST;
					else
						player_velocity_x = 0;
				}
				if(player_velocity_y!=0) {
					if(Math.abs(player_velocity_y)>FRICTION_CONST)
						player_velocity_y+=Math.copySign(1, player_velocity_y)*-FRICTION_CONST;
					else
						player_velocity_y = 0;
				}
				
				player_x+=player_velocity_x;
				player_y+=player_velocity_y;
				
				
				
				mutex.lock();
				ArrayList<Asteroid> removeQueue = new ArrayList<Asteroid>();
				ArrayList<Asteroid> addQueue = new ArrayList<Asteroid>();
				for(int i = 0;i<asteroids.size();i++) {
					Asteroid a = asteroids.get(i);
					a.x+=Math.cos(a.angle)*a.speed;
					a.y+=Math.sin(a.angle)*a.speed;
					
					if(a.x>SIZE_X + ASTEROID_BUFFER || a.x<-ASTEROID_BUFFER) {
						asteroids.remove(a);
						a.removed = true;
						continue;
					}else
					if(a.y>SIZE_Y + ASTEROID_BUFFER || a.x<-ASTEROID_BUFFER) {
						asteroids.remove(a);
						a.removed = true;
						continue;
					}
					
					if(DistanceFormula(a.x, a.y, player_x, player_y)<(a.size + PLAYER_SIZE)/2 - 15) {
						mutex.unlock();
						game_lost = true;
						enable_continue = false;
						timer.schedule(new TimerTask() {
							@Override
							public void run() {
								enable_continue = true;
							}
			
						}, 2000);
						return;
					}
					
					for(int j = 0;j<asteroids.size();j++) {
						Asteroid a2 = asteroids.get(j);
						if(a2!=a && !a2.removed && !a.removed) {
							if(DistanceFormula(a.x, a.y, a2.x, a2.y)<(a2.size + a.size)/2.1) {
								asteroids.remove(a2);
								asteroids.remove(a);
								
								Asteroid af = new Asteroid();
								af.x = (a.x + a2.x)/2;
								af.y = (a.y + a2.y)/2;
								double speed_x = Math.cos(a.angle)*a.speed + Math.cos(a2.angle)*a2.speed;
								double speed_y = Math.sin(a.angle)*a.speed + Math.sin(a2.angle)*a2.speed;
								af.angle = Math.atan2(speed_y, speed_x);
								double speed = Math.sqrt(Math.pow(speed_x, 2) + Math.pow(speed_y, 2));
								if(speed>ASTEROID_SPEED_MAX)
									af.speed = ASTEROID_SPEED_MAX;
								else if(speed<ASTEROID_SPEED_MIN*0.5)
									af.speed = ASTEROID_SPEED_MIN*0.5;
								else
									af.speed = speed;
								double size = Math.abs(a.size - a2.size)*1.2;
								if(size>ASTEROID_SIZE_MAX)
									size = ASTEROID_SIZE_MAX;
								else if(size<ASTEROID_SIZE_MIN*0.75)
									continue;
								else
									af.size=size;
								af.texture = (int)Math.round(Math.random() * (asteroidTextures.size()-1));

								asteroids.add(af);
							}
						}
						
					}
				}
				
				for(Asteroid a : removeQueue) {
					asteroids.remove(a);
				}
				for(Asteroid a : addQueue) {
					asteroids.add(a);
				}
				
				if(game_lost==false && asteroids.size()<Math.ceil(score/3))
					SpawnAsteroid();
				mutex.unlock();
				
			}
			}
			
		}, 0, 10);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				panel.repaint();
			}
			
		}, 0, 16);
		
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(game_lost==false)
				score++;
			}
			
		}, 0, 1000);
	}
	
	public double DistanceFormula(double x, double y, double x1, double y1) {
		return Math.sqrt(Math.pow(x-x1, 2) + Math.pow(y-y1, 2));
	}
	
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D graphics2D = (Graphics2D) g;
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON); 
		
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
	            RenderingHints.VALUE_RENDER_QUALITY); 

	   // Set anti-alias for text
		graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	            RenderingHints.VALUE_TEXT_ANTIALIAS_ON); 
		
		g.drawImage(space, 0, 0, SIZE_X, SIZE_Y, null);
		
		
		DrawImageRotate(g, PLAYER_RES, (int)(player_x-PLAYER_SIZE/2), (int)(player_y-PLAYER_SIZE/2), PLAYER_SIZE, PLAYER_SIZE, player_rot+Math.PI/2);
		
	
		g.setColor(Color.BLUE);
		
		mutex.lock();
		for(int i = 0;i<asteroids.size();i++) {
			Asteroid a = asteroids.get(i);
			//g.drawRect((int)(a.x-a.size/2), (int)(a.y-a.size/2), (int)a.size, (int)a.size);
			try {
				DrawImageRotate(g, asteroidTextures.get(a.texture), (int)(a.x-a.size/2), (int)(a.y-a.size/2), (int)a.size, (int)a.size, a.rot);
			}catch(java.awt.image.ImagingOpException e) {
				asteroids.remove(a);
			}
		}
		mutex.unlock();
		
		DrawStats(g);
		
		if(game_lost==true) {
			g.setColor(Color.GREEN);
			Font f1 = new Font("Arial", Font.BOLD, 32);
			Font f2 = new Font("Arial", Font.BOLD, 26);
			g.setFont(f1);
			String phrase="Game Over! Score: " + score;
			g.drawString(phrase, (SIZE_X - g.getFontMetrics(f1).stringWidth(phrase))/2, (SIZE_Y - g.getFontMetrics(f1).getAscent())/2);
			if(enable_continue) {
				g.setColor(Color.CYAN);
				g.setFont(f2);
				String phrase2 = "retry (press any key)";
				g.drawString(phrase2, (SIZE_X - g.getFontMetrics(f2).stringWidth(phrase2))/2, (SIZE_Y - g.getFontMetrics(f2).getAscent())/2 + g.getFontMetrics(f1).getAscent() + 15);
			}
		}
		
	}
	
	private void DrawStats(Graphics g) {
		g.setColor(Color.WHITE);
		int length = 50;
		Font f = new Font("Arial", Font.BOLD, 16);
		g.setFont(f);
		g.drawString("SCORE=" + score, 10, 10+g.getFontMetrics(f).getAscent());
		g.drawString("ASTEROIDS=" + asteroids.size(), 10, (10+g.getFontMetrics(f).getAscent())*2);
		g.drawString("MAX ASTEROIDS=" + (int)Math.ceil(score/3), 10, (10+g.getFontMetrics(f).getAscent())*3);
		//g.drawLine((int)player_x, (int)player_y, (int)(player_x+Math.cos(player_rot)*length), (int)(player_y+Math.sin(player_rot)*length));
	}

	double ANGLE_MAX = Math.PI/2;
	public void SpawnAsteroid() {
		int side = (int)Math.round(Math.random()*3);
		double angle_variance = Math.random()*ANGLE_MAX-ANGLE_MAX/2;
		double speed = Math.random() *(ASTEROID_SPEED_MAX-ASTEROID_SPEED_MIN) + ASTEROID_SPEED_MIN;
		double angle = 0;
		double size = Math.random()*(ASTEROID_SIZE_MAX-ASTEROID_SIZE_MIN)+ASTEROID_SIZE_MIN;
		double rot = Math.random()*Math.PI*2;
		int texture = (int)Math.round(Math.random() * (asteroidTextures.size()-1));
		double x = SIZE_X/2;
		double y = SIZE_Y/2;
		if(side==0) {//top
			angle = Math.PI/2 + angle_variance;
			x = Math.random() * SIZE_X;
			y = -ASTEROID_SIZE_MAX;
		}else if(side==1) {//right
			angle = -Math.PI + angle_variance; 
			y = Math.random() * SIZE_Y;
			x = SIZE_X;
		}else if(side==2) {//bottom
			angle = -Math.PI/2 + angle_variance; 
			x = Math.random() * SIZE_X;
			y = SIZE_Y;
		}else if(side==3) {//left
			angle = angle_variance;
			y = Math.random() * SIZE_Y;
			x = -ASTEROID_SIZE_MAX;
		}
		Asteroid a = new Asteroid();
		a.x = x;
		a.y = y;
		a.speed = speed;
		a.angle = angle;
		a.size = size;
		a.rot = rot;
		a.texture = texture;
		
		mutex.lock();
		asteroids.add(a);
		mutex.unlock();
	}
	
	public BufferedImage GetImage(File path) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return image;
	}
	public void DrawImageRotate(Graphics g, BufferedImage image, int x, int y, int w1, int h1, double r) {
		double scale = w1*1.0/image.getWidth();
		double locationX = w1 / 2;
		double locationY = h1 / 2;
		AffineTransform tx = AffineTransform.getRotateInstance(r, locationX, locationY);
		AffineTransform tx2 = AffineTransform.getScaleInstance(scale,scale);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		AffineTransformOp op2 = new AffineTransformOp(tx2, AffineTransformOp.TYPE_BILINEAR);

		// Drawing the rotated image at the required drawing locations
		g.drawImage(op.filter(op2.filter(image, null), null), x, y, null);
	}
	
	private void KeyAction(KeyEvent e, boolean b) {
		if(game_lost) {
			if(enable_continue==false)
				return;
			game_lost = false;
			ResetGame();
			return;
		}
		switch(e.getKeyCode()) {
		case KeyEvent.VK_W:
			w_pressed = b;
			break;
		case KeyEvent.VK_S:
			s_pressed = b;
			break;
		case KeyEvent.VK_A:
			a_pressed = b;
			break;
		case KeyEvent.VK_D:
			d_pressed = b;
			break;
		case KeyEvent.VK_RIGHT:
			right_pressed = b;
			break;
		case KeyEvent.VK_LEFT:
			left_pressed = b;
			break;
		case KeyEvent.VK_SPACE:
			space_pressed = b;
			break;
		}
	}

	private void ResetGame() {
		asteroids.clear();
		player_x = SIZE_X/2;
		player_y = SIZE_Y/2;
		player_velocity_x = 0;
		player_velocity_y = 0;
		s_pressed = false;
		a_pressed = false;
		w_pressed = false;
		d_pressed = false;
		space_pressed = false;
		right_pressed = false;
		left_pressed = false;
		score = 0;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		KeyAction(e, true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		KeyAction(e, false);
	}
}
