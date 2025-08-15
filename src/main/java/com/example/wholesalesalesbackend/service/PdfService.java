package com.example.wholesalesalesbackend.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfService {

    static class UnifiedEntry {
        LocalDate date;
        String description;
        Double amount;
        boolean isReturn;
        boolean isDeposit;

        public UnifiedEntry(LocalDate date, String description, Double amount, boolean isReturn, boolean isDeposit) {
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.isReturn = isReturn;
            this.isDeposit = isDeposit;
        }

        public LocalDate getDate() {
            return date;
        }
    }

    public ByteArrayInputStream generateSalesPdf(
            String clientName,
            List<SaleEntry> sales,
            List<Deposit> depositEntries,
            LocalDate from,
            LocalDate to,
            boolean isAllClient,
            Double depositAmount,
            java.time.LocalDateTime depositDateTime,
            Double oldBalance) {

        if (oldBalance == null)
            oldBalance = 0.0;

        LocalDate indiaToday = LocalDate.now(); // no conversion needed
        LocalDate finalDate = (to != null ? to : indiaToday);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            java.text.DecimalFormat noDecimalFormat = new java.text.DecimalFormat("#");

            // Title
            BaseColor pinkColor = new BaseColor(255, 105, 180);
            Font fontShopTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, pinkColor);

            Paragraph shopTitle = new Paragraph("Arihant Mobile Shop", fontShopTitle);
            shopTitle.setAlignment(Element.ALIGN_CENTER);
            shopTitle.setSpacingAfter(20f);
            document.add(shopTitle);

            // Report Info
            document.add(new Paragraph("Sales Report for → " + clientName, fontBold));
            document.add(new Paragraph("Pdf Generated date → " + indiaToday.format(formatter)));
            if (from != null && to != null) {
                document.add(new Paragraph(
                        "" + from.format(formatter) + " se " + to.format(formatter) + " tak ki report",
                        fontBold));
            }
            document.add(Chunk.NEWLINE);

            // Old Balance
            String oldBalPrefix = (from != null)
                    ? "(" + from.minusDays(1).format(formatter) + ") Tak Ka Pending Amount = ₹"
                    : "Pending Amount = ₹";
            Paragraph oldBalanceLine = new Paragraph(oldBalPrefix + noDecimalFormat.format(oldBalance), redFont);
            oldBalanceLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(oldBalanceLine);
            document.add(Chunk.NEWLINE);

            // ===== Merge Sales & Deposits into unified list (no timezone conversion needed) =====
            List<UnifiedEntry> unifiedList = new ArrayList<>();

            for (SaleEntry sale : sales) {
                // DB already stores IST, so just extract LocalDate
                unifiedList.add(new UnifiedEntry(
                        sale.getSaleDateTime().toLocalDate(),
                        sale.getAccessoryName(),
                        sale.getTotalPrice(),
                        Boolean.TRUE.equals(sale.isReturnFlag()),
                        false));
            }

            for (Deposit dep : depositEntries) {
                // DB already stores IST, just extract LocalDate
                unifiedList.add(new UnifiedEntry(
                        dep.getDepositDate().toLocalDate(),
                        dep.getNote(),
                        -dep.getAmount(), // deposits negative
                        false,
                        true));
            }

            unifiedList.sort(Comparator.comparing(UnifiedEntry::getDate));

            // ===== Create unified table =====
            PdfPTable table = new PdfPTable(4); // Sr, Date, Description, Amount
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 10f, 25f, 45f, 20f });

            // Header
            Stream.of("Sr", "Date", "Description", "Amount").forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontBold));
                cell.setBackgroundColor(new BaseColor(135, 206, 250));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            });

            double totalSales = 0.0;
            double totalDeposits = 0.0;
            int srNo = 1;

            for (UnifiedEntry entry : unifiedList) {
                BaseColor rowColor = null;
                if (entry.isDeposit)
                    rowColor = new BaseColor(255, 105, 180); // faint red
                if (entry.isReturn)
                    rowColor = new BaseColor(255, 255, 153); // faint yellow

                PdfPCell srCell = new PdfPCell(new Phrase(String.valueOf(srNo++), fontNormal));
                PdfPCell dateCell = new PdfPCell(new Phrase(entry.date.format(formatter), fontNormal));
                PdfPCell descCell = new PdfPCell(new Phrase(entry.description, fontNormal));
                PdfPCell amountCell = new PdfPCell(
                        new Phrase((entry.amount < 0 ? "-" : "") + "₹" + noDecimalFormat.format(Math.abs(entry.amount)),
                                fontNormal));
                amountCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);

                if (rowColor != null) {
                    srCell.setBackgroundColor(rowColor);
                    dateCell.setBackgroundColor(rowColor);
                    descCell.setBackgroundColor(rowColor);
                    amountCell.setBackgroundColor(rowColor);
                }

                table.addCell(srCell);
                table.addCell(dateCell);
                table.addCell(descCell);
                table.addCell(amountCell);

                if (!entry.isDeposit)
                    totalSales += entry.amount;
                else
                    totalDeposits += entry.amount;
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // ===== Final Amount highlighted in red =====
            double finalBalance = oldBalance + totalSales + totalDeposits;

            Font finalFont = new Font(fontBold.getFamily(), fontBold.getSize() * 1, fontBold.getStyle(),
                    fontBold.getColor());

            Chunk finalChunk = new Chunk(
                    finalDate.format(formatter) + " Final Amount = ₹" + noDecimalFormat.format(finalBalance),
                    finalFont);
            finalChunk.setBackground(BaseColor.RED);

            Paragraph finalBalanceLine = new Paragraph(finalChunk);
            finalBalanceLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(finalBalanceLine);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Footer
            Paragraph footer = new Paragraph(
                    "Thank You For Purchasing\nContact on Vishal Jain Mobile No → 9537886555",
                    fontBold);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20f);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
