package com.group_finity.mascot.x11;


import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.GC;


/**
 * The image window with alpha.
 * {@link #setImage(WindowsNativeImage)} set in {@link WindowsNativeImage} can be displayed on the desktop.
 * 
 * {@link #setAlpha(int)} may be specified when the concentration of view.
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
class X11TranslucentWindow extends JWindow implements TranslucentWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * To view images.
	 */
	private X11NativeImage image;
	
	private JPanel panel;
    private X11 x11 = X11.INSTANCE;
    private Display dpy = x11.XOpenDisplay(null);
    private X11.Window win = null;
	private float alpha = 1.0f;
	private JWindow alphaWindow = this;
    private X11.XWindowAttributes xwa = new X11.XWindowAttributes();

	public X11TranslucentWindow() {
		super(com.sun.jna.platform.WindowUtils.getAlphaCompatibleGraphicsConfiguration());
		this.init();

		this.panel = new JPanel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(final Graphics g) {
				g.drawImage(getImage().getManagedImage(), 0, 0, null);
			}
		};
		this.setContentPane(this.panel);
        x11.XGetWindowAttributes(dpy, win, xwa);

	}

	private void init() {
		System.setProperty("sun.java2d.noddraw", "true");
		System.setProperty("sun.java2d.opengl", "true");
	}

	@Override
	public void setVisible(final boolean b) {
		super.setVisible(b);
		if (b) {
			com.sun.jna.platform.WindowUtils.setWindowTransparent(this, true);
		}
	}
	
    private com.sun.jna.Memory buffer;
    private int[] pixels;

	@SuppressWarnings("deprecation")
	private void updateX11() {	 
     
     //   X11 x11 = X11.INSTANCE;
     //   X11.Window win = X11.Window.None;
     //   Display dpy = x11.XOpenDisplay(null);
        try {
  
        //        win = new X11.Window((int)Native.getWindowID(alphaWindow));
                if (win == null) {
                	win = new X11.Window((int)Native.getWindowID(alphaWindow));
                } 
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                alphaWindow.setSize(w, h);
                
                
                if (buffer == null || buffer.getSize() != w*h*4) {
                    buffer = new com.sun.jna.Memory(w*h*4);
                    pixels = new int[w*h];
                }

                BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics g = buf.getGraphics();
                g.drawImage(image.getManagedImage(), 0, 0, w, h, null);
                
                GC gc = x11.XCreateGC(dpy, win, new NativeLong(0), null);
        
                try {
                    Raster raster = buf.getData();
                    int[] pixel = new int[4];
                    
                    for (int y=0;y < h;y++) {
                        for (int x=0;x < w;x++) {
                            raster.getPixel(x, y, pixel);
                            int alpha = (pixel[3]&0xFF)<<24;
                            int red = (pixel[2]&0xFF);
                            int green = (pixel[1]&0xFF)<<8;
                            int blue = (pixel[0]&0xFF)<<16;
                            pixels[y*w+x] = alpha|red|green|blue;
                        }
                    }
                
                    //X11.XWindowAttributes xwa = new X11.XWindowAttributes();
                    //x11.XGetWindowAttributes(dpy, win, xwa);
                    X11.XImage image = x11.XCreateImage(dpy, xwa.visual,
                                                        32, X11.ZPixmap,
                                                        0, buffer, w, h, 32, w*4);
                    buffer.write(0, pixels, 0, pixels.length);
                   
                    x11.XPutImage(dpy, win, gc, image, 0,0,0,0,w,h);
                    x11.XFree(image.getPointer());
                   
                }
                finally {
                    if (gc != null)
                        x11.XFreeGC(dpy, gc);
                }

        }
        finally {
    /*        if (dpy != null)
                x11.XCloseDisplay(dpy); */
        } 
            
        if (!alphaWindow.isVisible()) {
            alphaWindow.setVisible(true);
            // hack for initial refresh (X11)
            repaint();
        } 
    }
	    
	@Override
	protected void addImpl(final Component comp, final Object constraints, final int index) {
		super.addImpl(comp, constraints, index);
		if (comp instanceof JComponent) {
			final JComponent jcomp = (JComponent) comp;
			jcomp.setOpaque(false);
		}
	}

	public void setAlpha(final float alpha) {
		com.sun.jna.platform.WindowUtils.setWindowAlpha(alphaWindow, alpha);
	}

	public float getAlpha() {
		return this.alpha;
	}
	
	@Override
	public JWindow asJWindow() {
		return this;
	}

	@Override
	public String toString() {
		return "LayeredWindow[hashCode="+hashCode()+",bounds="+getBounds()+"]";
	}
	
	public X11NativeImage getImage() {
		return this.image;
	}

	public void setImage(final NativeImage image) {
		this.image = (X11NativeImage)image;
	}

	
	public void repaint(Graphics g){
	       updateX11();
	}
	
	public void updateImage() {
		validate();
		updateX11();
	}

}
