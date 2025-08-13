package com.example.wholesalesalesbackend.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.model.SaleEntry;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfService {

    public ByteArrayInputStream generateSalesPdf(
            String clientName,
            List<SaleEntry> sales,
            LocalDate from,
            LocalDate to,
            boolean isAllClient,
            Double depositAmount,
            LocalDateTime depositDateTime,
            Double oldBalance) {

        if (oldBalance == null) {
            oldBalance = 0.0;
        }
        // Ensure India timezone
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        LocalDate indiaToday = LocalDate.now(indiaZone);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

            // Title
            Paragraph shopTitle = new Paragraph("<---------------- Arihant Mobile Wholesale Shop -------------->",
                    fontHeader);
            shopTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(shopTitle);

            // Report Info
            document.add(new Paragraph("Sales Report for → " + clientName, fontBold));

            document.add(new Paragraph("Pdf Generated date → " + indiaToday.format(formatter)));
            Font blueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLUE);

            if (from != null && to != null) {
                String formattedFrom = from.format(formatter);
                String formattedTo = to.format(formatter);
                document.add(new Paragraph("Date from: " + formattedFrom + "  se  " + formattedTo + " tak ki report",
                        blueFont));
            }
            document.add(Chunk.NEWLINE);

            // ===== Old Balance Row =====
            PdfPTable balanceTable = new PdfPTable(1);
            balanceTable.setWidthPercentage(50);
            balanceTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            if (from != null) {
                LocalDate modifiedFromDateBeforeOneDay = from.minusDays(1);
                String dateInString = "(" + modifiedFromDateBeforeOneDay.format(formatter) + ")";
                PdfPCell oldBalanceCell = new PdfPCell(
                        new Phrase(dateInString + " Tak Ka Pending Amount = ₹" + oldBalance, redFont));
                oldBalanceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                oldBalanceCell.setBorder(Rectangle.NO_BORDER);
                oldBalanceCell.setNoWrap(true);
                balanceTable.addCell(oldBalanceCell);
            }

            document.add(balanceTable);
            document.add(Chunk.NEWLINE);

            // ===== Sales Table =====
            int columnCount = isAllClient ? 5 : 4;
            PdfPTable table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);
            if (isAllClient) {
                table.setWidths(new float[] { 10f, 20f, 40f, 10f, 20f });
            } else {
                table.setWidths(new float[] { 10f, 20f, 40f, 30f });
            }

            // Table Header
            Stream.of(isAllClient
                    ? new String[] { "Sr", "Date", "Accessory", "Client", "Total Price" }
                    : new String[] { "Sr", "Date", "Accessory", "Total Price" })
                    .forEach(header -> {
                        PdfPCell cell = new PdfPCell(new Phrase(header, fontBold));
                        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        table.addCell(cell);
                    });

            // Table Rows
            int sr = 1;
            Double totalSales = 0.0;
            BaseColor yellow = new BaseColor(255, 255, 153);

            for (SaleEntry sale : sales) {
                boolean isReturn = Boolean.TRUE.equals(sale.isReturnFlag());

                PdfPCell srCell = new PdfPCell(new Phrase(String.valueOf(sr++), fontNormal));
                PdfPCell dateCell = new PdfPCell(
                        new Phrase(sale.getSaleDateTime().atZone(ZoneId.systemDefault())
                                .withZoneSameInstant(indiaZone)
                                .toLocalDate().format(formatter), fontNormal));
                PdfPCell accessoryCell = new PdfPCell(new Phrase(sale.getAccessoryName(), fontNormal));
                PdfPCell clientCell = null;
                if (isAllClient) {
                    clientCell = new PdfPCell(new Phrase(sale.getClient().getName(), fontNormal));
                }
                PdfPCell priceCell = new PdfPCell(new Phrase("₹" + sale.getTotalPrice(), fontNormal));

                if (isReturn) {
                    srCell.setBackgroundColor(yellow);
                    dateCell.setBackgroundColor(yellow);
                    accessoryCell.setBackgroundColor(yellow);
                    if (clientCell != null)
                        clientCell.setBackgroundColor(yellow);
                    priceCell.setBackgroundColor(yellow);
                }

                table.addCell(srCell);
                table.addCell(dateCell);
                table.addCell(accessoryCell);
                if (isAllClient) {
                    table.addCell(clientCell);
                }
                table.addCell(priceCell);

                totalSales += sale.getTotalPrice();
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // ===== Final Summary =====
            Double finalBalance = oldBalance + totalSales;

            PdfPTable summaryTable = new PdfPTable(1);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell finalCell = new PdfPCell(new Phrase(
                    to.format(formatter) + " Ka Final Amount = ₹" + finalBalance,
                    redFont // Entire text green
            ));
            finalCell.setBorder(Rectangle.NO_BORDER);
            finalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            finalCell.setNoWrap(true);
            summaryTable.addCell(finalCell);

            document.add(summaryTable);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Footer
            Paragraph footer = new Paragraph("Thank You For Purchasing\nContact on Vishal Jain Mobile No → 9537886555",
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

