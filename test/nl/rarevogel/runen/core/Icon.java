package nl.rarevogel.runen.core;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
/** Draws the Viking longship app icon. */
public class Icon {
    public static void main(String[] a) throws Exception {
        int S = 1024;
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(S / 100.0, S / 100.0);
        // badge
        Shape badge = new RoundRectangle2D.Double(2, 2, 96, 96, 30, 30);
        g.setPaint(new GradientPaint(0, 2, new Color(0x2B4A63), 0, 98, new Color(0x0E1A26)));
        g.fill(badge);
        g.setClip(badge);
        // moon
        g.setPaint(new RadialGradientPaint(70, 26, 26, new float[]{0, 0.35f, 1}, new Color[]{new Color(0xFFF1D58A, true), new Color(0x66E2AE4C, true), new Color(0x00E2AE4C, true)}));
        g.fill(new Ellipse2D.Double(44, 0, 52, 52));
        g.setColor(new Color(0xF4DE9C));
        g.fill(new Ellipse2D.Double(63, 19, 14, 14));
        // sea
        g.setPaint(new GradientPaint(0, 70, new Color(0x1F4F6E), 0, 100, new Color(0x0B2233)));
        Path2D sea = new Path2D.Double();
        sea.moveTo(0, 74);
        for (int i = 0; i <= 10; i++) sea.quadTo(i * 10 + 5, 70 + (i % 2) * 2, i * 10 + 10, 74);
        sea.lineTo(100, 100); sea.lineTo(0, 100); sea.closePath();
        g.fill(sea);
        // ship, scaled a little so the dragon head stays inside the badge
        AffineTransform base = g.getTransform();
        g.translate(50, 59); g.scale(0.88, 0.88); g.translate(-50, -56);
        // mast
        g.setColor(new Color(0x4A2E1A));
        g.fill(new Rectangle2D.Double(48.6, 20, 2.8, 50));
        // sail with stripes, slightly bellied
        Path2D sail = new Path2D.Double();
        sail.moveTo(29, 24); sail.quadTo(50, 21, 71, 24); sail.lineTo(69, 55); sail.quadTo(50, 59, 31, 55); sail.closePath();
        Shape old = g.getClip();
        g.setColor(new Color(0xEFE4CC)); g.fill(sail);
        g.clip(sail);
        g.setColor(new Color(0xC23B2A));
        for (int i = 0; i < 5; i++) g.fill(new Rectangle2D.Double(29 + i * 9 + 1.5, 18, 4.5, 45));
        g.setClip(old); g.clip(badge);
        g.setColor(new Color(0x5A3A22));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(sail);
        // yard
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(26, 24, 50, 20.5, 74, 24));
        // rune on the sail: raido, the journey
        Font rf = Font.createFont(Font.TRUETYPE_FONT, new File("assets/runic.ttf")).deriveFont(22f);
        g.setFont(rf);
        FontMetrics fm = g.getFontMetrics();
        String r = "ᚱ";
        float rw = fm.stringWidth(r);
        g.setColor(new Color(0x40000000, true));
        g.drawString(r, 50 - rw / 2 + 0.6f, 47.6f);
        g.setColor(new Color(0xF2C35A));
        g.drawString(r, 50 - rw / 2, 47);
        // hull
        Path2D hull = new Path2D.Double();
        hull.moveTo(10, 56);              // prow tip area
        hull.curveTo(14, 66, 22, 72, 34, 73);
        hull.lineTo(66, 73);
        hull.curveTo(78, 72, 86, 66, 90, 56);
        hull.curveTo(80, 61, 70, 62, 60, 62);
        hull.lineTo(40, 62);
        hull.curveTo(30, 62, 20, 61, 10, 56);
        hull.closePath();
        g.setPaint(new GradientPaint(0, 58, new Color(0x8A5A32), 0, 73, new Color(0x4A2C16)));
        g.fill(hull);
        // planks
        g.setColor(new Color(0x33200F));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new CubicCurve2D.Double(14, 61, 24, 68, 40, 67.5, 50, 67.5));
        g.draw(new CubicCurve2D.Double(86, 61, 76, 68, 60, 67.5, 50, 67.5));
        // shields on the gunwale
        Color[] sc = {new Color(0xE2AE4C), new Color(0xC23B2A), new Color(0xEFE4CC), new Color(0x2E6A8E)};
        for (int i = 0; i < 6; i++) {
            double x = 30 + i * 8;
            g.setColor(sc[i % 4]); g.fill(new Ellipse2D.Double(x - 3.4, 58.3, 6.8, 6.8));
            g.setColor(new Color(0x3A2412)); g.setStroke(new BasicStroke(0.6f)); g.draw(new Ellipse2D.Double(x - 3.4, 58.3, 6.8, 6.8));
            g.setColor(new Color(0xB9B9B9)); g.fill(new Ellipse2D.Double(x - 1, 60.7, 2, 2));
        }
        // dragon prow (left) and curled stern (right)
        g.setPaint(new GradientPaint(0, 40, new Color(0x8A5A32), 0, 60, new Color(0x5A3820)));
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D neck = new Path2D.Double();
        neck.moveTo(12, 58); neck.curveTo(6, 52, 6, 44, 10, 38);
        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(neck);
        Path2D head = new Path2D.Double();
        head.moveTo(12.5, 40.5); head.lineTo(8.5, 41); head.lineTo(3.2, 38.6); head.lineTo(7.4, 37.4);
        head.lineTo(2.6, 35); head.lineTo(8, 31.6); head.lineTo(11.5, 29.2); head.lineTo(12.6, 31.4);
        head.lineTo(15.6, 30.6); head.lineTo(14.6, 33.6); head.lineTo(17, 34.6); head.lineTo(14.4, 36.4); head.closePath();
        g.setPaint(new GradientPaint(0, 29, new Color(0x9A6A3C), 0, 41, new Color(0x5A3820)));
        g.fill(head);
        g.setColor(new Color(0x3A2412)); g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(head);
        g.setColor(new Color(0xF2C35A)); g.fill(new Ellipse2D.Double(8.6, 33.2, 1.8, 1.8)); // eye
        g.setColor(new Color(0xC23B2A)); g.fill(new Polygon(new int[]{0}, new int[]{0}, 0));
        g.setPaint(new GradientPaint(0, 40, new Color(0x8A5A32), 0, 60, new Color(0x5A3820)));
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D stern = new Path2D.Double();
        stern.moveTo(89, 57); stern.curveTo(95, 50, 94, 41, 88, 40); stern.curveTo(84.5, 40, 84, 44, 87, 45);
        g.draw(stern);
        g.setTransform(base);
        // foam
        g.setColor(new Color(0x88FFFFFF, true));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(22, 75.5, 50, 78, 78, 75.5));
        g.setClip(null);
        // gold rim
        g.setColor(new Color(0xE2AE4C));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new RoundRectangle2D.Double(3.1, 3.1, 93.8, 93.8, 28, 28));
        g.dispose();
        ImageIO.write(img, "png", new File(a[0]));
    }
}
