import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * QR tool for the printable card (scripts/make-card.ps1). Text goes in and out through UTF-8 files, so
 * nothing depends on the console's code page.
 *   encode <text file> <out.svg>   the text as a QR code (error correction M, 4-module quiet zone), as SVG
 *   decode <image.png> <out.txt>   the text of the QR code found in the image (exit 2 when none)
 */
public class QrTool {

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("encode")) {
            String text = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8).strip();
            Files.writeString(Path.of(args[2]), svg(text), StandardCharsets.UTF_8);
        } else if (args.length == 3 && args[0].equals("decode")) {
            String text = decode(Path.of(args[1]));
            if (text == null) {
                System.err.println("no QR code found in " + args[1]);
                System.exit(2);
            }
            Files.writeString(Path.of(args[2]), text, StandardCharsets.UTF_8);
        } else {
            System.err.println("usage: encode <text file> <out.svg> | decode <image.png> <out.txt>");
            System.exit(1);
        }
    }

    private static String svg(String text) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 4);
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                if (m.get(x, y)) path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + m.getWidth() + " " + m.getHeight()
            + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
            + "<path fill=\"#000\" d=\"" + path + "\"/></svg>\n";
    }

    private static String decode(Path image) throws Exception {
        BufferedImage img = ImageIO.read(image.toFile());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        try {
            Result r = new MultiFormatReader().decode(bitmap, hints);
            return r.getText();
        } catch (com.google.zxing.NotFoundException e) {
            return null;
        }
    }
}
