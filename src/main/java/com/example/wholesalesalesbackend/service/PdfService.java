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
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfService {

    public ByteArrayInputStream generateSalesPdf(
            String clientName,
            List<SaleEntry> sales,
            List<Deposit> depositEntries,
            LocalDate from,
            LocalDate to,
            boolean isAllClient,
            Double depositAmount,
            LocalDateTime depositDateTime,
            Double oldBalance) {

        if (oldBalance == null) {
            oldBalance = 0.0;
        }

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        LocalDate indiaToday = LocalDate.now(indiaZone);
        LocalDate finalDate = (to != null ? to : indiaToday);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);
            Font blueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLUE);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            java.text.DecimalFormat noDecimalFormat = new java.text.DecimalFormat("#");

            BaseColor pinkColor = new BaseColor(255, 105, 180);
            Font fontShopTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, pinkColor);

            Paragraph shopTitle = new Paragraph("Arihant Mobile Shop", fontShopTitle);
            shopTitle.setAlignment(Element.ALIGN_CENTER);
            shopTitle.setSpacingAfter(20f);
            document.add(shopTitle);

            document.add(new Paragraph("Sales Report for → " + clientName, fontBold));
            document.add(new Paragraph("Pdf Generated date → " + indiaToday.format(formatter)));

            if (from != null && to != null) {
                document.add(new Paragraph(
                        "" + from.format(formatter) + " se " + to.format(formatter) + " tak ki report",
                        blueFont));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable balanceTable = new PdfPTable(1);
            balanceTable.setWidthPercentage(50);
            balanceTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            String oldBalPrefix;
            if (from != null) {
                LocalDate modifiedFromDateBeforeOneDay = from.minusDays(1);
                oldBalPrefix = "(" + modifiedFromDateBeforeOneDay.format(formatter) + ") Tak Ka Pending Amount = ₹";
            } else {
                oldBalPrefix = "Pending Amount = ₹";
            }

            PdfPCell oldBalanceCell = new PdfPCell(
                    new Phrase(oldBalPrefix + noDecimalFormat.format(oldBalance), redFont));
            oldBalanceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            oldBalanceCell.setBorder(Rectangle.NO_BORDER);
            oldBalanceCell.setNoWrap(true);
            balanceTable.addCell(oldBalanceCell);

            document.add(balanceTable);
            document.add(Chunk.NEWLINE);

            Paragraph purchaseHeading = new Paragraph("Purchase Entry Table", fontBold);
            purchaseHeading.setAlignment(Element.ALIGN_LEFT);
            document.add(purchaseHeading);
            document.add(Chunk.NEWLINE);

            int columnCount = isAllClient ? 5 : 4;
            PdfPTable salesTable = new PdfPTable(columnCount);
            salesTable.setWidthPercentage(100);
            if (isAllClient) {
                salesTable.setWidths(new float[] { 10f, 20f, 40f, 10f, 20f });
            } else {
                salesTable.setWidths(new float[] { 10f, 20f, 40f, 30f });
            }

            BaseColor headerBlue = new BaseColor(135, 206, 250);
            Stream.of(isAllClient
                    ? new String[] { "Sr", "Date", "Accessory", "Client", "Price" }
                    : new String[] { "Sr", "Date", "Accessory", "Price" })
                    .forEach(header -> {
                        PdfPCell cell = new PdfPCell(new Phrase(header, fontBold));
                        cell.setBackgroundColor(headerBlue);
                        salesTable.addCell(cell);
                    });

            int sr = 1;
            Double totalSales = 0.0;
            BaseColor yellow = new BaseColor(255, 255, 153);

            for (SaleEntry sale : sales) {
                boolean isReturn = Boolean.TRUE.equals(sale.isReturnFlag());

                PdfPCell srCell = new PdfPCell(new Phrase(String.valueOf(sr++), fontNormal));

                // ✅ Directly format if stored in IST
                PdfPCell dateCell = new PdfPCell(new Phrase(
                        sale.getSaleDateTime().format(formatter),
                        fontNormal));

                PdfPCell accessoryCell = new PdfPCell(new Phrase(sale.getAccessoryName(), fontNormal));
                PdfPCell clientCell = null;
                if (isAllClient) {
                    clientCell = new PdfPCell(new Phrase(sale.getClient().getName(), fontNormal));
                }
                PdfPCell priceCell = new PdfPCell(
                        new Phrase("₹" + noDecimalFormat.format(sale.getTotalPrice()), fontNormal));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

                if (isReturn) {
                    srCell.setBackgroundColor(yellow);
                    dateCell.setBackgroundColor(yellow);
                    accessoryCell.setBackgroundColor(yellow);
                    if (clientCell != null)
                        clientCell.setBackgroundColor(yellow);
                    priceCell.setBackgroundColor(yellow);
                }

                salesTable.addCell(srCell);
                salesTable.addCell(dateCell);
                salesTable.addCell(accessoryCell);
                if (isAllClient)
                    salesTable.addCell(clientCell);
                salesTable.addCell(priceCell);

                totalSales += sale.getTotalPrice();
            }

            PdfPCell emptyRow = new PdfPCell(new Phrase(" "));
            emptyRow.setColspan(columnCount);
            emptyRow.setBorder(Rectangle.NO_BORDER);
            salesTable.addCell(emptyRow);

            BaseColor faintBlue = new BaseColor(224, 247, 250);
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total Price", fontBold));
            totalLabel.setColspan(columnCount - 1);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(faintBlue);
            salesTable.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase("₹" + noDecimalFormat.format(totalSales), blueFont));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setBackgroundColor(faintBlue);
            salesTable.addCell(totalValue);

            document.add(salesTable);
            document.add(Chunk.NEWLINE);

            if (!depositEntries.isEmpty()) {
                double interimFinal = oldBalance + totalSales;
                Chunk interimChunk = new Chunk(
                        "Final Amount = " + noDecimalFormat.format(interimFinal), fontBold);
                interimChunk.setBackground(new BaseColor(255, 255, 153));

                Paragraph interimLine = new Paragraph(interimChunk);
                interimLine.setAlignment(Element.ALIGN_RIGHT);
                document.add(interimLine);
                document.add(Chunk.NEWLINE);
            }

            Double totalDeposits = 0.0;
            if (depositEntries != null && !depositEntries.isEmpty()) {
                Paragraph paymentHeading = new Paragraph("Payment Entry Table", fontBold);
                paymentHeading.setAlignment(Element.ALIGN_LEFT);
                document.add(paymentHeading);
                document.add(Chunk.NEWLINE);

                PdfPTable depositTable = new PdfPTable(4);
                depositTable.setWidthPercentage(100);
                depositTable.setWidths(new float[] { 10f, 25f, 25f, 30f });

                BaseColor headerRed = new BaseColor(255, 153, 153);
                Stream.of(new String[] { "Sr", "Date", "Payment Mode", "Deposit" }).forEach(header -> {
                    PdfPCell cell = new PdfPCell(new Phrase(header, fontBold));
                    cell.setBackgroundColor(headerRed);
                    depositTable.addCell(cell);
                });

                int dr = 1;

                for (Deposit dep : depositEntries) {
                    depositTable.addCell(new PdfPCell(new Phrase(String.valueOf(dr++), fontNormal)));

                    // ✅ Directly format if stored in IST
                    depositTable.addCell(new PdfPCell(new Phrase(
                            dep.getDepositDate().format(formatter),
                            fontNormal)));

                    depositTable.addCell(new PdfPCell(new Phrase(dep.getNote(), fontNormal)));

                    PdfPCell amountCell = new PdfPCell(
                            new Phrase("₹" + noDecimalFormat.format(dep.getAmount()), fontNormal));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    depositTable.addCell(amountCell);

                    totalDeposits += dep.getAmount();
                }

                PdfPCell emptyDepRow = new PdfPCell(new Phrase(" "));
                emptyDepRow.setColspan(4);
                emptyDepRow.setBorder(Rectangle.NO_BORDER);
                depositTable.addCell(emptyDepRow);

                BaseColor faintPink = new BaseColor(255, 228, 225);
                PdfPCell totalDepLabel = new PdfPCell(new Phrase("Total Deposits", fontBold));
                totalDepLabel.setColspan(3);
                totalDepLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalDepLabel.setBackgroundColor(faintPink);
                depositTable.addCell(totalDepLabel);

                PdfPCell totalDepValue = new PdfPCell(new Phrase("₹" + noDecimalFormat.format(totalDeposits), redFont));
                totalDepValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalDepValue.setBackgroundColor(faintPink);
                depositTable.addCell(totalDepValue);

                document.add(depositTable);
                document.add(Chunk.NEWLINE);
            }

            Double finalBalance = oldBalance + totalSales - totalDeposits;

            Paragraph finalBalanceLine = new Paragraph(
                    finalDate.format(formatter) + " Final Amount = " + noDecimalFormat.format(finalBalance),
                    redFont);
            finalBalanceLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(finalBalanceLine);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

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
