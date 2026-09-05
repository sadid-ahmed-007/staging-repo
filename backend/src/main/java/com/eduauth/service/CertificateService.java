package com.eduauth.service;

import com.eduauth.model.Certificate;
import com.eduauth.model.Enrollment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Handles certificate-level business logic:
 *  - Share link generation (serial + encrypted DOB)
 *  - PDF certificate generation with QR code (mirrors the Blade template layout)
 */
@Service
public class CertificateService {

    private static final String FRONTEND_BASE = "http://localhost:5173";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DeviceRgb NAVY   = new DeviceRgb(0x0d, 0x2b, 0x5e);
    private static final DeviceRgb GOLD   = new DeviceRgb(0xbd, 0xa8, 0x53);
    private static final DeviceRgb DARK   = new DeviceRgb(0x33, 0x41, 0x55);

    private final EncryptionService encryptionService;

    public CertificateService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    // ── Share link ────────────────────────────────────────────────────────────

    /**
     * Build the public verification share link for a certificate.
     * Format: http://localhost:5173/verify?s={serial}&v={encryptedDOB}
     */
    public String buildShareLink(Certificate cert) {
        if (cert.getStudent() == null || cert.getStudent().getDateOfBirth() == null) {
            return FRONTEND_BASE + "/verify?s=" + cert.getSerial();
        }
        String dob = cert.getStudent().getDateOfBirth().toString(); // yyyy-MM-dd
        String encryptedDob = encryptionService.encryptDOB(dob);
        return FRONTEND_BASE + "/verify?s=" + cert.getSerial() + "&v=" + encryptedDob;
    }

    // ── PDF generation ────────────────────────────────────────────────────────

    /**
     * Generate a PDF for the given certificate and return it as a ResponseEntity
     * with Content-Type: application/pdf and filename={serial}.pdf.
     */
    public ResponseEntity<byte[]> generatePdf(Certificate cert) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);

            // A4 Landscape
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document doc = new Document(pdfDoc);
            doc.setMargins(40, 40, 40, 40);

            // Outer border (navy, 6pt)
            drawBorder(pdfDoc, NAVY, 6f, 20f);
            // Inner border (gold, 1.5pt)
            drawBorder(pdfDoc, GOLD, 1.5f, 30f);

            PdfFont serifBold = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.TIMES_BOLD);
            PdfFont serifItalic = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.TIMES_ITALIC);
            PdfFont serif = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN);
            PdfFont helvetica = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont helveticaBold = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            // ── Issue date & convocation date (top corners) ───────────────
            // We'll add them as small paragraphs aligned left/right
            Table topRow = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(8);

            Cell leftDate = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.LEFT);
            if (cert.getConvocationDate() != null) {
                leftDate.add(new Paragraph()
                        .add(new Text("Convocation Date: ").setFont(helveticaBold).setFontSize(9).setFontColor(NAVY))
                        .add(new Text(cert.getConvocationDate().format(DATE_FMT)).setFont(helvetica).setFontSize(9).setFontColor(DARK)));
            }

            Cell rightDate = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph()
                            .add(new Text("Issue Date: ").setFont(helveticaBold).setFontSize(9).setFontColor(NAVY))
                            .add(new Text(cert.getIssueDate().format(DATE_FMT)).setFont(helvetica).setFontSize(9).setFontColor(DARK)));

            topRow.addCell(leftDate).addCell(rightDate);
            doc.add(topRow);

            // ── University name ───────────────────────────────────────────
            String institutionName = cert.getInstitution() != null
                    ? cert.getInstitution().getName()
                    : "Institution";

            doc.add(new Paragraph(institutionName)
                    .setFont(serifBold)
                    .setFontSize(28)
                    .setFontColor(NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(1.5f)
                    .setMarginTop(20));

            // ── Certificate of Achievement ────────────────────────────────
            doc.add(new Paragraph("Certificate of Achievement")
                    .setFont(serifItalic)
                    .setFontSize(36)
                    .setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10));

            // ── Body text ─────────────────────────────────────────────────
            String studentName = cert.getStudentDisplayName();
            
            // Construct the degree text (e.g. "Bachelor of Science" or "BSc (Hons) Computer Science")
            // Fallbacks ensure we show the most specific degree title available without repeating words
            String degreeText = (cert.getCertificateLevel() != null ? cert.getCertificateLevel() : "")
                    + (cert.getCertificateName() != null && !cert.getCertificateName().equals(cert.getCertificateLevel())
                       ? " " + cert.getCertificateName() : "");
            
            // Only append "in [Major]" if a major is explicitly provided
            String majorText = cert.getMajor() != null ? " in " + cert.getMajor() : "";

            Paragraph body = new Paragraph()
                    .add(new Text("This is to certify that ").setFont(serif).setFontSize(18).setFontColor(DARK))
                    .add(new Text(studentName).setFont(serifBold).setFontSize(18).setFontColor(DARK))
                    .add(new Text(" has successfully completed the requirements for the degree of ").setFont(serif).setFontSize(18).setFontColor(DARK))
                    .add(new Text(degreeText.trim()).setFont(serifBold).setFontSize(18).setFontColor(DARK))
                    .add(new Text(majorText + ".").setFont(serifBold).setFontSize(18).setFontColor(DARK))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(22)
                    .setMarginLeft(40)
                    .setMarginRight(40);
            doc.add(body);

            // ── Meta lines ────────────────────────────────────────────────
            Enrollment enrollment = cert.getEnrollment();
            String rollNum = enrollment != null ? enrollment.getRollNumber() : null;
            if (rollNum == null && enrollment != null) rollNum = enrollment.getEnrollmentNumber();

            if (rollNum != null) {
                doc.add(metaLine("Student ID: ", rollNum, serif, serifBold));
            }
            if (cert.getSession() != null) {
                doc.add(metaLine("Academic Session: ", cert.getSession(), serif, serifBold));
            }

            // CGPA / Degree Class
            boolean hasClass = cert.getDegreeClass() != null
                    && !cert.getDegreeClass().isBlank()
                    && !cert.getDegreeClass().equalsIgnoreCase("N/A")
                    && !cert.getDegreeClass().equalsIgnoreCase("none");
            if (cert.getCgpa() != null || hasClass) {
                StringBuilder meta = new StringBuilder();
                if (cert.getCgpa() != null) {
                    meta.append("CGPA: ").append(String.format("%.2f", cert.getCgpa())).append("/4.00");
                }
                if (cert.getCgpa() != null && hasClass) meta.append("  |  ");
                if (hasClass) meta.append("Class: ").append(cert.getDegreeClass());
                doc.add(new Paragraph(meta.toString())
                        .setFont(serifBold).setFontSize(14).setFontColor(DARK)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // ── Footer: QR code (left) + authority signature (right) ──────
            String shareLink = buildShareLink(cert);
            byte[] qrBytes = generateQrCode(shareLink, 120);

            Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(24);

            // Left — QR code + serial
            Cell leftCell = new Cell()
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM)
                    .setHorizontalAlignment(HorizontalAlignment.LEFT);

            com.itextpdf.layout.element.Image qrImage = new com.itextpdf.layout.element.Image(
                    ImageDataFactory.create(qrBytes)).setWidth(100).setHeight(100);
            leftCell.add(qrImage);
            leftCell.add(new Paragraph("Serial No: " + cert.getSerial())
                    .setFont(helvetica).setFontSize(9).setFontColor(new DeviceRgb(0x77, 0x77, 0x77)));
            footer.addCell(leftCell);

            // Right — signature line + authority
            Cell rightCell = new Cell()
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.BOTTOM)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT);

            // Signature line
            rightCell.add(new Paragraph("_____________________________")
                    .setFont(serif).setFontSize(11).setFontColor(DARK)
                    .setTextAlignment(TextAlignment.RIGHT));
            rightCell.add(new Paragraph(cert.getAuthorityName() != null ? cert.getAuthorityName() : "Issuing Authority")
                    .setFont(helveticaBold).setFontSize(13).setFontColor(NAVY)
                    .setTextAlignment(TextAlignment.RIGHT));
            rightCell.add(new Paragraph(cert.getAuthorityTitle() != null ? cert.getAuthorityTitle() : "Authorized Signatory")
                    .setFont(helvetica).setFontSize(11).setFontColor(DARK)
                    .setTextAlignment(TextAlignment.RIGHT));
            footer.addCell(rightCell);

            doc.add(footer);
            doc.close();

            byte[] pdf = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", cert.getSerial() + ".pdf");
            headers.setContentLength(pdf.length);

            return ResponseEntity.ok().headers(headers).body(pdf);

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Paragraph metaLine(String label, String value, PdfFont regular, PdfFont bold) {
        return new Paragraph()
                .add(new Text(label).setFont(regular).setFontSize(14).setFontColor(DARK))
                .add(new Text(value).setFont(bold).setFontSize(14).setFontColor(DARK))
                .setTextAlignment(TextAlignment.CENTER);
    }

    /** Generate a QR code PNG byte array for the given content at the given size. */
    private byte[] generateQrCode(String content, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }

    /** Draw a rectangle border on all pages at the given margin distance from the page edge. */
    private void drawBorder(PdfDocument pdfDoc, DeviceRgb color, float lineWidth, float margin) {
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            com.itextpdf.kernel.pdf.PdfPage page = pdfDoc.getPage(i);
            com.itextpdf.kernel.geom.Rectangle pageSize = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.setStrokeColor(color)
                    .setLineWidth(lineWidth)
                    .rectangle(
                            margin, margin,
                            pageSize.getWidth()  - 2 * margin,
                            pageSize.getHeight() - 2 * margin)
                    .stroke();
            canvas.release();
        }
    }
}
