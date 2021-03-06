/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cismm;

import static cismm.MirrorControlForm.cur_mode;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import org.micromanager.utils.MathFunctions;
import org.micromanager.utils.ReportingUtils;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.json.Test;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ImageUtils;
/**
 *
 * @author phsiao
 */
public class Util {
    //protected static AtomicBoolean is_freerun_running = new AtomicBoolean(false);
    //protected static AtomicBoolean is_calibration_running = new AtomicBoolean(false);
    //protected static AtomicBoolean stop_calibration_requested = new AtomicBoolean(false);
    
    //protected static AtomicBoolean stop_requested = new AtomicBoolean(false);
    //protected static AtomicBoolean is_running = new AtomicBoolean(false);
    public static AtomicBoolean is_stop_requested = new AtomicBoolean(false);
    
    public static String jar_path() {
        /*
             * A raw string could be like:
             * 1. file:C:\Program Files\Micro-Manager-1.4\mmplugins\DualAxisMirror.jar!/
             * 2. /C:/Program Files/Micro-Manager-1.4/scripts/DualAxisMirror.jar
             *
        */
        String nativeDir = null;
        try {
            //String path = MirrorControlForm.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            //String decodedPath = URLDecoder.decode(path, "UTF-8");
            nativeDir = URLDecoder.decode(MirrorControlForm.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            if (nativeDir.contains("!")) {
                nativeDir = nativeDir.substring(nativeDir.indexOf(":") + 1, nativeDir.lastIndexOf(File.separator));
            } else {
                nativeDir = nativeDir.substring(1, nativeDir.lastIndexOf('/'));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nativeDir;
    }
    
    
//    public static Process run_external_program(String prog, List<String> args, boolean wait_till_done) {
//        Process proc = null;
//        try {
//            args.add(0, prog);
//
//            ProcessBuilder pb = new ProcessBuilder(args);
//            proc = pb.start();
//            
//            if (wait_till_done) {
//                proc.waitFor();
//                proc.destroy();
//            }
//            
//        } catch (IOException ex) {
//            Logger.getLogger(MirrorControlForm.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(MirrorControlForm.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return proc;
//    }
    public static File getJarDir(Class aclass) {
        URL url;
        String extURL;      //  url.toExternalForm();

        // get an url
        try {
            url = aclass.getProtectionDomain().getCodeSource().getLocation();
              // url is in one of two forms
              //        ./build/classes/   NetBeans test
              //        jardir/JarName.jar  froma jar
        } catch (SecurityException ex) {
            url = aclass.getResource(aclass.getSimpleName() + ".class");
            // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/U:/Fred/java/Tools/UI/build/classes
            //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
        }

        // convert to external form
        extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar"))   // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        else {  // from getResource
            String suffix = "/"+(aclass.getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
        }

        // convert back to url
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
            // leave url unchanged; probably does not happen
        }

        // convert url to File
        try {
            return new File(url.toURI());
        } catch(URISyntaxException ex) {
            return new File(url.getPath());
        }
    }
    
    // This assumes that the called program will terminate
    public static void run_external_program(String prog, List<String> args) {
        try {
            args.add(0, jar_path() + File.separator + prog);
            
            /*
            try{
                PrintWriter writer = new PrintWriter("C:\\Users\\phsiao\\Desktop\\the-file-name.txt", "UTF-8");
                writer.println(args.toString());
                writer.close();
            } catch (IOException e) {
               // do something
            }
            */
            ProcessBuilder pb = new ProcessBuilder(args);
            Process proc = pb.start();
            proc.waitFor();
            proc.destroy();
        } catch (IOException ex) {
            Logger.getLogger(MirrorControlForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Illuminate a spot at position x,y.
     */
    public static void set_voltage(String daq_str, double x, double y) {
        
        if (daq_str == null) {
            JOptionPane.showMessageDialog(null, "Analog input ports are required");
            return;
        }
        
        
        if (x >= NI.min_v_x && x <= (NI.v_range_x + NI.min_v_x)
         && y >= NI.min_v_y && y <= (NI.v_range_y + NI.min_v_y))
        {
            final List<String> args = new ArrayList<String>();    
            args.add(daq_str);
            args.add(Double.toString(x));
            args.add(Double.toString(y));      
            run_external_program("two_ao_update.exe", args);            
        }
    }
    
    // copied from https://valelab4.ucsf.edu/svn/micromanager2/trunk/mmstudio/src/org/micromanager/utils/ImageUtils.java
    public static int unsignedValue(byte b) {
        // Sign-extend, then mask
        return ((int) b) & 0x000000ff;
    }

    public static int unsignedValue(short s) {
        // Sign-extend, then mask
        return ((int) s) & 0x0000ffff;
    }
    public static Point findMaxPixel(ImageProcessor proc) {       
        final Object pixels = proc.getPixels();       
        int max = Integer.MIN_VALUE;
        int max_ind = -1;
        int pix_max = 65536;
        double threshhold = 0.18;
        
        if (pixels instanceof byte[]) {
            byte[] bytes = (byte[]) pixels;
            for (int i = 0; i < bytes.length; ++i) {
                int new_val = unsignedValue(bytes[i]);
                if (new_val > max) {
                    max = new_val;
                    max_ind = i;
                }        
            }
            pix_max = 255;
        }
        if (pixels instanceof short[]) {
            short[] shorts = (short[]) pixels;
            for (int i = 0; i < shorts.length; ++i) {
                int new_val = unsignedValue(shorts[i]);
                if (new_val > max) {
                    max = new_val;
                    max_ind = i;
                }               
            }
            pix_max = 65535;
        }       
        if (max < pix_max * threshhold) {
            return new Point(-2, -2);
        } else {
            int width = proc.getWidth();
            return new Point(max_ind % width, max_ind / width);
        }
    }
    
    // Find the brightest spot in an ImageProcessor. The image is first blurred
    // and then the pixel with maximum intensity is returned.
    public static Point findPeak(ImageProcessor proc) {
        /*
        ImageProcessor blurImage = proc.duplicate(); 
        blurImage.setRoi((Roi) null);
        GaussianBlur blur = new GaussianBlur();
        blur.blurGaussian(blurImage, 5, 5, 0.01);
        Point x = findMaxPixel(blurImage);
        */
        
        Point x = findMaxPixel(proc);
        x.translate(1, 1);
        return x;
    }
    /**
     * Display a spot using the projection device, and return its current
     * location on the camera. Does not do sub-pixel localization, but could
     * (just would change its return type, most other code would be OK with
     * this)
     */
    public static Point measureSpotOnCamera(CMMCore core, ScriptInterface app,
                    String daq_str, Point2D.Double projectionPoint)
    {
        
        try {
            set_voltage(daq_str, projectionPoint.x, projectionPoint.y);
            core.snapImage();
            TaggedImage image = core.getTaggedImage();
            ImageProcessor proc1 = ImageUtils.makeMonochromeProcessor(image);
            Point maxPt = findPeak(proc1);

            app.displayImage(image);
            app.getSnapLiveWin().getImagePlus().setRoi(new PointRoi(maxPt.x, maxPt.y));

            return maxPt;
        } catch (Exception e) {
            ReportingUtils.showError(e);
            return null;
        }
    }
    /**
     * Illuminate a spot at ptSLM, measure its location on the camera, and add
     * the resulting point pair to the spotMap.
     */
    public static void measureAndAddToSpotMap(CMMCore core, ScriptInterface app,
            String daq_str, Map<Point2D.Double, Point2D.Double> spotMap, 
            Point2D.Double ptSLM)
    {
        
        Point ptCam = measureSpotOnCamera(core, app, daq_str, ptSLM);
        if (ptCam != null && ptCam.x >= 0) {
            Point2D.Double ptCamDouble = new Point2D.Double(ptCam.x, ptCam.y);
            spotMap.put(ptCamDouble, ptSLM);
        }
    }
    
    /**
     * Illuminates and images five control points near the center, and return an
     * affine transform mapping from image coordinates to phototargeter
     * coordinates.
     */
    public static AffineTransform generateLinearMapping(CMMCore core, ScriptInterface app,
           String daq_str)
    {
        double spacing = Math.min(NI.v_range_x, NI.v_range_y) / 10;  // use 10% of galvo/SLM range
        Map<Point2D.Double, Point2D.Double> p_to_v_map = new HashMap<Point2D.Double, Point2D.Double>();

        for (double i = NI.min_v_x; i <= NI.max_v_x; i += spacing) {
            for (double j = NI.min_v_y; j <= NI.max_v_y; j += spacing) {
                if (is_stop_requested.get()) {
                    return null;
                }
                measureAndAddToSpotMap(core, app, daq_str, p_to_v_map, new Point2D.Double(i, j));       
            }
        }
        
        try {
            return MathFunctions.generateAffineTransformFromPointPairs(p_to_v_map);
        } catch (Exception e) {
            //throw new RuntimeException("Spots aren't detected as expected. Is DMD in focus and roughly centered in camera's field of view?");
            throw new RuntimeException(e.getMessage());
        }     
    }
    
    
    /**
     * Simple utility methods for points
     *
     * Adds a point to an existing polygon.
     */
    private static void addVertex(Polygon polygon, Point p) {
        polygon.addPoint(p.x, p.y);
    }
    /**
     * Converts a Point with double values for x,y to a point with x and y
     * rounded to the nearest integer.
     */
    public static Point toIntPoint(Point2D.Double pt) {
        return new Point((int) (0.5 + pt.x), (int) (0.5 + pt.y));
    }
    
    public static Point2D.Double toDoublePoint(Point pt) {
        return new Point2D.Double(pt.x, pt.y);
    }
    
    public static Map<Polygon, AffineTransform> generateNonlinearMapping(
            CMMCore core, ScriptInterface app, String daq_str,
            AffineTransform affine_map) {
      
        final int nGrid = 7;
        Point2D.Double slmPoint[][] = new Point2D.Double[1 + nGrid][1 + nGrid];
        Point2D.Double camPoint[][] = new Point2D.Double[1 + nGrid][1 + nGrid];

        final int padding = 25;
        final double cam_width = (double) core.getImageWidth() - padding * 2;
        final double cam_height = (double) core.getImageHeight() - padding * 2;
        final double cam_step_x = cam_width / nGrid;
        final double cam_step_y = cam_height / nGrid;

        for (int i = 0; i <= nGrid; i++) {
            for (int j = 0; j <= nGrid; j++) {
                if (is_stop_requested.get()) {
                    return null;
                }
                slmPoint[i][j] = (Point2D.Double) affine_map.transform(
                        new Point2D.Double(cam_step_x * i + padding,
                        cam_step_y * j + padding), null);
            }
        }
        // tabulate the camera spot at each of SLM grid points
        for (int i = 0; i <= nGrid; ++i) {
            for (int j = 0; j <= nGrid; ++j) {
                if (is_stop_requested.get()) {
                    return null;
                }
                
                Point spot = measureSpotOnCamera(core, app, daq_str, slmPoint[i][j]);
                if (spot != null) {
                    camPoint[i][j] = toDoublePoint(spot);
                }
                
            }
        }

        

        // now make a grid of (square) polygons (in camera's coordinate system)
        // and generate an affine transform for each of these square regions
        Map<Polygon, AffineTransform> bigMap = new HashMap<Polygon, AffineTransform>();
        for (int i = 0; i <= nGrid - 1; ++i) {
            for (int j = 0; j <= nGrid - 1; ++j) {
                if (is_stop_requested.get()) {
                   return null;
                }
                Polygon poly = new Polygon();
                addVertex(poly, toIntPoint(camPoint[i][j]));
                addVertex(poly, toIntPoint(camPoint[i][j + 1]));
                addVertex(poly, toIntPoint(camPoint[i + 1][j + 1]));
                addVertex(poly, toIntPoint(camPoint[i + 1][j]));

                Map<Point2D.Double, Point2D.Double> map = new HashMap<Point2D.Double, Point2D.Double>();
                map.put(camPoint[i][j], slmPoint[i][j]);
                map.put(camPoint[i][j + 1], slmPoint[i][j + 1]);
                map.put(camPoint[i + 1][j], slmPoint[i + 1][j]);
                map.put(camPoint[i + 1][j + 1], slmPoint[i + 1][j + 1]);
                double srcDX = Math.abs((camPoint[i + 1][j].x - camPoint[i][j].x)) / 4;
                double srcDY = Math.abs((camPoint[i][j + 1].y - camPoint[i][j].y)) / 4;
                double srcTol = Math.max(srcDX, srcDY);

                try {
                    AffineTransform transform = MathFunctions.generateAffineTransformFromPointPairs(map, srcTol, Double.MAX_VALUE);
                    bigMap.put(poly, transform);
                } catch (Exception e) {
                    ReportingUtils.logError("Bad cell in mapping.");
                }
            }
        }
        return bigMap;
    }
    
    public static Point2D.Double transformPoint(Map<Polygon, AffineTransform> mapping, Point2D.Double pt) {
        Set<Polygon> set = mapping.keySet();
        // First find out if the given point is inside a cell, and if so,
        // transform it with that cell's AffineTransform.

        for (Polygon poly : set) {
            if (poly.contains(pt)) {
                return (Point2D.Double) mapping.get(poly).transform(pt, null);
            }
        }

        // The point isn't inside any cell, so use the global mapping
        return (Point2D.Double) cur_mode.first_mapping.transform(pt, null);

        // The point isn't inside any cell, so search for the closest cell
        // and use the AffineTransform from that.
        /*
         double minDistance = Double.MAX_VALUE;
         Polygon bestPoly = null;
         for (Polygon poly : set) {
         double distance = meanPosition2D(getVertices(poly)).distance(pt.x, pt.y);
         if (minDistance > distance) {
         bestPoly = poly;
         minDistance = distance;
         }
         }
         if (bestPoly == null) {
         throw new RuntimeException("Unable to map point to device.");
         }
         return (Point2D.Double) mapping.get(bestPoly).transform(pt, null);
         */
    }
}
