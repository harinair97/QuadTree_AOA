import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// QuadTree Node
class QuadTreeNode {
    int x, y, size;
    int value;
    boolean isLeaf;
    QuadTreeNode[] children;

    public QuadTreeNode(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.isLeaf = false;
        this.children = new QuadTreeNode[4]; // TL, TR, BL, BR
    }
}

// QuadTree compression class
public class QuadTreeCompression {
    int[][] image;
    int threshold;

    public QuadTreeCompression(int[][] image, int threshold) {
        this.image = image;
        this.threshold = threshold;
    }

    // Build QuadTree recursively
    public QuadTreeNode build(int x, int y, int size) {
        if (size <= 1 || isHomogeneous(x, y, size)) {
            QuadTreeNode leaf = new QuadTreeNode(x, y, size);
            leaf.isLeaf = true;
            leaf.value = averageValue(x, y, size);
            return leaf;
        }

        QuadTreeNode node = new QuadTreeNode(x, y, size);
        int half = size / 2;
        node.children[0] = build(x, y, half);              // top-left
        node.children[1] = build(x + half, y, half);      // top-right
        node.children[2] = build(x, y + half, half);      // bottom-left
        node.children[3] = build(x + half, y + half, half);// bottom-right
        return node;
    }

    private boolean isHomogeneous(int x, int y, int size) {
        int sum = 0, sumSq = 0;
        for (int i = x; i < x + size; i++)
            for (int j = y; j < y + size; j++) {
                sum += image[i][j];
                sumSq += image[i][j] * image[i][j];
            }
        int n = size * size;
        double variance = (sumSq - (sum * sum) / (double)n) / n;
        return variance <= threshold;
    }

    private int averageValue(int x, int y, int size) {
        int sum = 0;
        for (int i = x; i < x + size; i++)
            for (int j = y; j < y + size; j++)
                sum += image[i][j];
        return sum / (size * size);
    }

    // Count total nodes in QuadTree
    public int countNodes(QuadTreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf) return 1;
        int count = 1;
        for (QuadTreeNode child : node.children)
            count += countNodes(child);
        return count;
    }

    // Reconstruct compressed image from QuadTree
    public void reconstructImage(QuadTreeNode node, int[][] output) {
        if (node.isLeaf) {
            for (int i = node.x; i < node.x + node.size; i++)
                for (int j = node.y; j < node.y + node.size; j++)
                    output[i][j] = node.value;
        } else {
            for (QuadTreeNode child : node.children)
                if (child != null) reconstructImage(child, output);
        }
    }

    // Load and resize image to power-of-2 square
    public static int[][] loadAndResizeGrayscaleImage(String path, int size) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();

        int[][] gray = new int[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                int rgb = resized.getRGB(i, j);
                int r = (rgb >> 16) & 0xFF;
                int gVal = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[i][j] = (r + gVal + b) / 3;  // convert to grayscale
            }
        return gray;
    }

    // Save grayscale image
    public static void saveGrayscaleImage(int[][] imgArray, String outputPath) throws IOException {
        int width = imgArray.length;
        int height = imgArray[0].length;
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int gray = imgArray[i][j];
                int rgb = (gray << 16) | (gray << 8) | gray;
                outImage.setRGB(i, j, rgb);
            }
        ImageIO.write(outImage, "png", new File(outputPath));
    }

    public static void main(String[] args) throws IOException {
        String datasetFolder = "dataset_images"; // Put images here
        String outputFolder = "compressed_images";
        new File(outputFolder).mkdirs();

        int targetSize = 256; // Resize all images to 256x256
        List<Integer> thresholds = Arrays.asList(5, 10, 20, 50);

        File folder = new File(datasetFolder);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null) {
            System.out.println("No images found in dataset.");
            return;
        }

        // Create CSV writer
        FileWriter csvWriter = new FileWriter("performance.csv");
        csvWriter.append("Image,Threshold,Runtime(ms),Compression Ratio\n");

        for (File f : files) {
            int[][] img = loadAndResizeGrayscaleImage(f.getAbsolutePath(), targetSize);

            for (int threshold : thresholds) {
                QuadTreeCompression qt = new QuadTreeCompression(img, threshold);

                long start = System.nanoTime();
                QuadTreeNode root = qt.build(0, 0, img.length);
                long end = System.nanoTime();

                double runtimeMs = (end - start) / 1e6;
                int totalNodes = qt.countNodes(root);
                double compressionRatio = (double) totalNodes / (img.length * img[0].length);

                // Write to CSV
                String baseName = f.getName().substring(0, f.getName().lastIndexOf('.'));
                csvWriter.append(String.format("%s,%d,%.2f,%.3f\n", baseName, threshold, runtimeMs, compressionRatio));

                // Save reconstructed compressed image
                int[][] compressed = new int[img.length][img[0].length];
                qt.reconstructImage(root, compressed);
                String outPath = outputFolder + "/" + baseName + "_t" + threshold + ".png";
                saveGrayscaleImage(compressed, outPath);
            }
        }

        csvWriter.flush();
        csvWriter.close();
        System.out.println("Performance CSV saved to performance.csv");
        System.out.println("All images compressed and saved to " + outputFolder);
    }
}
