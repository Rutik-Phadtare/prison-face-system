package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

// Explicit Imports for JavaFX - This fixes the "Ambiguous Reference" and "Cannot resolve method"
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

// PDF Imports - We avoid importing 'TextField' from here to prevent conflicts
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.ColumnText;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GuardProfileController {

    @FXML private ImageView guardImage;
    @FXML private TextField nameField, ageField, addressField, transferField, salaryField;
    @FXML private TextField aadharField, phoneField, batchIdField, emailField;
    @FXML private ComboBox<String> shiftBox, genderBox, designationBox;
    @FXML private Label statusBadge;
    @FXML private DatePicker joiningDatePicker, birthDatePicker;
    @FXML private TextArea descriptionArea;
    @FXML private ScrollPane mainScrollPane;

    private Guard currentGuard;
    private final GuardDao guardDao = new GuardDao();
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        shiftBox.setItems(FXCollections.observableArrayList("Morning (06-14)", "Evening (14-22)", "Night (22-06)", "On Leave"));
        genderBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        designationBox.setItems(FXCollections.observableArrayList("Gate Guard", "Tower Guard", "Control Room", "Escort Officer", "Supervisor"));

        designationBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSalary(newVal));
        setupValidations();
        setupFastScroll();
    }

    private void setupValidations() {
        phoneField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) phoneField.setText(newVal.replaceAll("[^\\d]", ""));
            if (phoneField.getText().length() > 10) phoneField.setText(old);
        });
    }

    private void setupFastScroll() {
        if (mainScrollPane != null && mainScrollPane.getContent() != null) {
            mainScrollPane.getContent().setOnScroll(ev -> {
                double deltaY = ev.getDeltaY() * 3;
                double height = mainScrollPane.getContent().getBoundsInLocal().getHeight();
                mainScrollPane.setVvalue(mainScrollPane.getVvalue() - deltaY / height);
            });
        }
    }

    private void updateSalary(String role) {
        if (role == null) return;
        double amount = switch (role) {
            case "Supervisor" -> 5500.0;
            case "Control Room" -> 4800.0;
            case "Tower Guard" -> 4200.0;
            default -> 3500.0;
        };
        salaryField.setText(String.valueOf(amount));
    }

    public void setGuard(Guard g) {
        this.currentGuard = g;
        nameField.setText(g.getName());
        ageField.setText(String.valueOf(g.getAge()));
        addressField.setText(g.getAddress());
        transferField.setText(g.getTransferFrom());
        genderBox.setValue(g.getGender());
        birthDatePicker.setValue(g.getBirthDate());
        shiftBox.setValue(g.getShift());
        joiningDatePicker.setValue(g.getJoiningDate());
        descriptionArea.setText(g.getDescription());
        designationBox.setValue(g.getDesignation());
        salaryField.setText(String.valueOf(g.getSalary()));
        aadharField.setText(g.getAadharNumber() != null ? g.getAadharNumber() : "");
        phoneField.setText(g.getPhoneNumber() != null ? g.getPhoneNumber() : "");
        batchIdField.setText(g.getBatchId() != null ? g.getBatchId() : "");
        emailField.setText(g.getEmail() != null ? g.getEmail() : "");
        applyStatusColor(calculateLiveStatus(g.getShift()));
        loadGuardImage(g.getGuardId());
    }

    @FXML
    private void updateGuard() {
        if (currentGuard == null) return;
        currentGuard.setName(nameField.getText());
        currentGuard.setAadharNumber(aadharField.getText());
        currentGuard.setPhoneNumber(phoneField.getText());
        currentGuard.setBatchId(batchIdField.getText());
        currentGuard.setEmail(emailField.getText());
        currentGuard.setDesignation(designationBox.getValue());

        guardDao.update(currentGuard);
        if (onSaveCallback != null) onSaveCallback.run();
        new Alert(Alert.AlertType.INFORMATION, "Database Updated Successfully").show();
    }

    @FXML
    private void handlePrint() {
        if (currentGuard == null) return;
        generateOfficialPDF("GOV_RECORD_GUARD_", "OFFICIAL PERSONNEL RECORD", false);
    }

    @FXML
    private void handlePrintPaySlip() {
        if (currentGuard == null) return;
        generateOfficialPDF("PAYSLIP_GUARD_", "MONTHLY EARNINGS STATEMENT", true);
    }

    private void generateOfficialPDF(String filePrefix, String docTitle, boolean isPaySlip) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(filePrefix + (batchIdField.getText().isEmpty() ? String.valueOf(currentGuard.getGuardId()) : batchIdField.getText()) + ".pdf");
        File file = fileChooser.showSaveDialog(nameField.getScene().getWindow());

        if (file != null) {
            try {
                // Create document with borders
                Document document = new Document(PageSize.A4, 40, 40, 50, 60);
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));

                // Page border and footer event
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        try {
                            PdfContentByte cb = writer.getDirectContent();

                            // OUTER BORDER - Professional frame
                            cb.setLineWidth(2f);
                            cb.setColorStroke(new Color(0, 51, 102));
                            cb.rectangle(30, 30, document.getPageSize().getWidth() - 60, document.getPageSize().getHeight() - 60);
                            cb.stroke();

                            // INNER BORDER - Double frame effect
                            cb.setLineWidth(0.5f);
                            cb.rectangle(35, 35, document.getPageSize().getWidth() - 70, document.getPageSize().getHeight() - 70);
                            cb.stroke();

                            // Footer line
                            cb.setLineWidth(1f);
                            cb.moveTo(40, 50);
                            cb.lineTo(document.getPageSize().getWidth() - 40, 50);
                            cb.stroke();

                            // Footer text
                            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                            Phrase footerLeft = new Phrase("CONFIDENTIAL - FOR OFFICIAL USE ONLY", footerFont);
                            Phrase footerCenter = new Phrase("Department of Corrections & Prison Security", footerFont);
                            Phrase footerRight = new Phrase("Page " + writer.getPageNumber(), footerFont);

                            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, footerLeft, 40, 35, 0);
                            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footerCenter, document.getPageSize().getWidth() / 2, 35, 0);
                            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footerRight, document.getPageSize().getWidth() - 40, 35, 0);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                document.open();

                // Color scheme
                Color darkBlue = new Color(0, 51, 102);
                Color lightBlue = new Color(240, 245, 250);
                Color lightGray = new Color(250, 250, 250);

                // ==================== HEADER SECTION ====================
                PdfPTable headerTable = new PdfPTable(new float[]{1, 4, 1});
                headerTable.setWidthPercentage(100);
                headerTable.setSpacingAfter(20f);

                // Left Logo
                PdfPCell leftLogoCell = new PdfPCell();
                leftLogoCell.setBorder(PdfPCell.NO_BORDER);
                leftLogoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                leftLogoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                try {
                    com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
                    logo.scaleToFit(60, 60);
                    leftLogoCell.addElement(logo);
                } catch (Exception e) {
                    Paragraph logoText = new Paragraph("LOGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8));
                    logoText.setAlignment(Element.ALIGN_CENTER);
                    leftLogoCell.addElement(logoText);
                }
                headerTable.addCell(leftLogoCell);

                // Center Text
                PdfPCell centerTextCell = new PdfPCell();
                centerTextCell.setBorder(PdfPCell.NO_BORDER);
                centerTextCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                centerTextCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                Font deptFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, darkBlue);
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, darkBlue);
                Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

                Paragraph dept = new Paragraph("DEPARTMENT OF CORRECTIONS\n& PRISON SECURITY", deptFont);
                dept.setAlignment(Element.ALIGN_CENTER);
                dept.setSpacingAfter(8f);
                centerTextCell.addElement(dept);

                // Decorative line
                PdfPTable lineTable = new PdfPTable(1);
                lineTable.setWidthPercentage(80);
                PdfPCell lineCell = new PdfPCell();
                lineCell.setBorder(PdfPCell.NO_BORDER);
                lineCell.setBorderWidthTop(2f);
                lineCell.setBorderColorTop(darkBlue);
                lineCell.setFixedHeight(0f);
                lineTable.addCell(lineCell);
                centerTextCell.addElement(lineTable);

                Paragraph title = new Paragraph("\n" + docTitle, titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(3f);
                centerTextCell.addElement(title);

                Paragraph subtitle = new Paragraph("Security Clearance Level: Restricted", subtitleFont);
                subtitle.setAlignment(Element.ALIGN_CENTER);
                centerTextCell.addElement(subtitle);

                headerTable.addCell(centerTextCell);

                // Right Logo
                PdfPCell rightLogoCell = new PdfPCell();
                rightLogoCell.setBorder(PdfPCell.NO_BORDER);
                rightLogoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                rightLogoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                try {
                    com.lowagie.text.Image logo2 = com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
                    logo2.scaleToFit(60, 60);
                    rightLogoCell.addElement(logo2);
                } catch (Exception e) {
                    Paragraph logoText = new Paragraph("LOGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8));
                    logoText.setAlignment(Element.ALIGN_CENTER);
                    rightLogoCell.addElement(logoText);
                }
                headerTable.addCell(rightLogoCell);

                document.add(headerTable);

                // ==================== METADATA BAR ====================
                PdfPTable metaTable = new PdfPTable(3);
                metaTable.setWidthPercentage(100);
                metaTable.setSpacingAfter(15f);

                Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);

                String batchId = batchIdField.getText() != null && !batchIdField.getText().isEmpty() ? batchIdField.getText() : "N/A";
                addMetaCell(metaTable, isPaySlip ? "PAYSLIP ID: " + batchId : "BATCH ID: " + batchId, metaFont, darkBlue);
                addMetaCell(metaTable, "STATUS: " + statusBadge.getText(), metaFont, darkBlue);
                addMetaCell(metaTable, "ISSUED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), metaFont, darkBlue);

                document.add(metaTable);

                if (isPaySlip) {
                    // ==================== PAYSLIP DESIGN ====================
                    generatePaySlipContent(document, darkBlue, lightBlue, lightGray);
                } else {
                    // ==================== PROFILE DESIGN ====================
                    generateProfileContent(document, darkBlue, lightBlue, lightGray);
                }

                // ==================== SIGNATURE SECTION ====================
                PdfPTable signatureTable = new PdfPTable(2);
                signatureTable.setWidthPercentage(100);
                signatureTable.setSpacingBefore(30f);

                Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

                PdfPCell dateCell = new PdfPCell();
                dateCell.setBorder(PdfPCell.NO_BORDER);
                Paragraph dateLabel = new Paragraph("Date: ___________________", signFont);
                dateLabel.setSpacingAfter(5f);
                dateCell.addElement(dateLabel);

                PdfPCell signCell = new PdfPCell();
                signCell.setBorder(PdfPCell.NO_BORDER);
                signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                Paragraph signLine = new Paragraph("_____________________________", signFont);
                signLine.setAlignment(Element.ALIGN_RIGHT);
                signCell.addElement(signLine);
                Paragraph signLabel = new Paragraph("Authorized Official Signature", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
                signLabel.setAlignment(Element.ALIGN_RIGHT);
                signCell.addElement(signLabel);
                Paragraph officialTitle = new Paragraph("Chief Security Officer", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK));
                officialTitle.setAlignment(Element.ALIGN_RIGHT);
                signCell.addElement(officialTitle);

                signatureTable.addCell(dateCell);
                signatureTable.addCell(signCell);

                document.add(signatureTable);

                // ==================== DOCUMENT VERIFICATION ====================
                document.add(new Paragraph("\n"));
                PdfPTable verificationTable = new PdfPTable(1);
                verificationTable.setWidthPercentage(100);

                Font verifyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
                PdfPCell verifyCell = new PdfPCell(new Paragraph(
                        "This is an official government document. Unauthorized reproduction, alteration, or disclosure is prohibited by law. " +
                                "Document ID: DOC-" + System.currentTimeMillis() % 1000000 + " | Generated: " +
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")), verifyFont));
                verifyCell.setBorder(PdfPCell.TOP);
                verifyCell.setBorderColor(Color.LIGHT_GRAY);
                verifyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                verifyCell.setPadding(10);
                verifyCell.setBackgroundColor(new Color(245, 245, 245));
                verificationTable.addCell(verifyCell);

                document.add(verificationTable);

                document.close();
                new Alert(Alert.AlertType.INFORMATION, "Official Document Exported Successfully").show();

            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
            }
        }
    }

    private void generateProfileContent(Document document, Color darkBlue, Color lightBlue, Color lightGray) throws Exception {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkBlue);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 100, 100));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

        // ==================== MAIN DATA TABLE WITH PHOTO ====================
        PdfPTable mainTable = new PdfPTable(new float[]{3.2f, 2.8f});
        mainTable.setWidthPercentage(100);
        mainTable.setSpacingAfter(15f);
        mainTable.setKeepTogether(true); // Prevent splitting

        // --- LEFT COLUMN: PERSONAL DATA ---
        PdfPCell dataCell = new PdfPCell();
        dataCell.setBorder(PdfPCell.BOX);
        dataCell.setBorderColor(darkBlue);
        dataCell.setBorderWidth(1.5f);
        dataCell.setPadding(15);
        dataCell.setBackgroundColor(lightGray);
        dataCell.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph personalInfoHeader = new Paragraph("PERSONNEL IDENTIFICATION", sectionFont);
        personalInfoHeader.setSpacingAfter(12f);
        dataCell.addElement(personalInfoHeader);

        addFieldToCell(dataCell, "IDENTIFICATION NUMBER", "GJR-" + String.format("%06d", currentGuard.getGuardId()), labelFont, valueFont);
        addFieldToCell(dataCell, "FULL LEGAL NAME", nameField.getText().toUpperCase(), labelFont, valueFont);
        addFieldToCell(dataCell, "BATCH ID", batchIdField.getText(), labelFont, valueFont);
        addFieldToCell(dataCell, "CURRENT ASSIGNMENT", designationBox.getValue(), labelFont, valueFont);
        addFieldToCell(dataCell, "DATE OF BIRTH", birthDatePicker.getValue() != null ? birthDatePicker.getValue().toString() : "N/A", labelFont, valueFont);

        // Remove spacing after last field
        dataCell.addElement(new Paragraph("GENDER", labelFont));
        Paragraph genderValue = new Paragraph(genderBox.getValue() != null && !genderBox.getValue().isEmpty() ? genderBox.getValue() : "N/A", valueFont);
        genderValue.setSpacingAfter(0f);
        dataCell.addElement(genderValue);

        mainTable.addCell(dataCell);

        // --- RIGHT COLUMN: PHOTO ---
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(PdfPCell.BOX);
        photoCell.setBorderColor(darkBlue);
        photoCell.setBorderWidth(1.5f);
        photoCell.setPadding(12);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_TOP);
        photoCell.setBackgroundColor(Color.WHITE);

        Paragraph photoHeader = new Paragraph("OFFICIAL PHOTOGRAPH", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, darkBlue));
        photoHeader.setAlignment(Element.ALIGN_CENTER);
        photoHeader.setSpacingAfter(10f);
        photoCell.addElement(photoHeader);

        String imgPath = "python-face/photos/guards/" + currentGuard.getGuardId() + ".jpg";
        File imgFile = new File(imgPath);
        if (imgFile.exists()) {
            com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(imgFile.getAbsolutePath());
            pdfImg.scaleToFit(150, 170);
            pdfImg.setAlignment(Element.ALIGN_CENTER);
            photoCell.addElement(pdfImg);
        } else {
            Paragraph noPhoto = new Paragraph("\n\n\nNO PHOTOGRAPH\nON FILE\n\n\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY));
            noPhoto.setAlignment(Element.ALIGN_CENTER);
            photoCell.addElement(noPhoto);
        }

        Paragraph photoDate = new Paragraph("\nPhoto Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY));
        photoDate.setAlignment(Element.ALIGN_CENTER);
        photoCell.addElement(photoDate);

        mainTable.addCell(photoCell);

        document.add(mainTable);

        // ==================== CONTACT INFORMATION ====================
        PdfPTable contactSection = new PdfPTable(1);
        contactSection.setWidthPercentage(100);
        contactSection.setSpacingBefore(8f);
        contactSection.setSpacingAfter(15f);
        contactSection.setKeepTogether(true); // Keep section together

        // Header cell
        PdfPCell contactHeaderCell = new PdfPCell(new Paragraph("CONTACT INFORMATION", sectionFont));
        contactHeaderCell.setBorder(PdfPCell.NO_BORDER);
        contactHeaderCell.setBackgroundColor(new Color(240, 245, 250));
        contactHeaderCell.setPadding(8);
        contactHeaderCell.setPaddingLeft(10);
        contactSection.addCell(contactHeaderCell);

        // Data table inside
        PdfPTable contactTable = new PdfPTable(2);
        contactTable.setWidthPercentage(100);
        contactTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

        addDetailRow(contactTable, "Aadhar Number", aadharField.getText(), darkBlue, lightBlue);
        addDetailRow(contactTable, "Phone Number", phoneField.getText(), darkBlue, lightBlue);
        addDetailRow(contactTable, "Email Address", emailField.getText(), darkBlue, lightBlue);
        addDetailRow(contactTable, "Residential Address", addressField.getText(), darkBlue, lightBlue);

        PdfPCell contactDataCell = new PdfPCell(contactTable);
        contactDataCell.setBorder(PdfPCell.NO_BORDER);
        contactDataCell.setPadding(0);
        contactSection.addCell(contactDataCell);

        document.add(contactSection);

        // ==================== EMPLOYMENT DETAILS ====================
        PdfPTable employmentSection = new PdfPTable(1);
        employmentSection.setWidthPercentage(100);
        employmentSection.setSpacingBefore(8f);
        employmentSection.setSpacingAfter(15f);
        employmentSection.setKeepTogether(true); // Keep section together

        // Header cell
        PdfPCell empHeaderCell = new PdfPCell(new Paragraph("EMPLOYMENT SPECIFICATION", sectionFont));
        empHeaderCell.setBorder(PdfPCell.NO_BORDER);
        empHeaderCell.setBackgroundColor(new Color(240, 245, 250));
        empHeaderCell.setPadding(8);
        empHeaderCell.setPaddingLeft(10);
        employmentSection.addCell(empHeaderCell);

        // Data table inside
        PdfPTable detailTable = new PdfPTable(2);
        detailTable.setWidthPercentage(100);
        detailTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

        addDetailRow(detailTable, "Shift Schedule", shiftBox.getValue(), darkBlue, lightBlue);
        addDetailRow(detailTable, "Annual Salary Grade", "$" + salaryField.getText(), darkBlue, lightBlue);
        addDetailRow(detailTable, "Current Duty Status", statusBadge.getText(), darkBlue, lightBlue);
        addDetailRow(detailTable, "Enlistment Date", joiningDatePicker.getValue() != null ? joiningDatePicker.getValue().toString() : "N/A", darkBlue, lightBlue);
        addDetailRow(detailTable, "Transfer From", transferField.getText(), darkBlue, lightBlue);
        addDetailRow(detailTable, "Age", ageField.getText() + " years", darkBlue, lightBlue);

        PdfPCell empDataCell = new PdfPCell(detailTable);
        empDataCell.setBorder(PdfPCell.NO_BORDER);
        empDataCell.setPadding(0);
        employmentSection.addCell(empDataCell);

        document.add(employmentSection);

        // ==================== NOTES SECTION ====================
        if (descriptionArea.getText() != null && !descriptionArea.getText().trim().isEmpty()) {
            PdfPTable notesSection = new PdfPTable(1);
            notesSection.setWidthPercentage(100);
            notesSection.setSpacingBefore(8f);
            notesSection.setSpacingAfter(20f);
            notesSection.setKeepTogether(true); // Keep section together

            // Header cell
            PdfPCell notesHeaderCell = new PdfPCell(new Paragraph("NOTES / BIOMETRIC DATA / SPECIAL REMARKS", sectionFont));
            notesHeaderCell.setBorder(PdfPCell.NO_BORDER);
            notesHeaderCell.setBackgroundColor(new Color(240, 245, 250));
            notesHeaderCell.setPadding(8);
            notesHeaderCell.setPaddingLeft(10);
            notesSection.addCell(notesHeaderCell);

            // Content cell
            PdfPCell notesContentCell = new PdfPCell(new Paragraph(descriptionArea.getText(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
            notesContentCell.setBorder(PdfPCell.BOX);
            notesContentCell.setBorderColor(new Color(200, 200, 200));
            notesContentCell.setPadding(12);
            notesContentCell.setMinimumHeight(70);
            notesContentCell.setBackgroundColor(Color.WHITE);
            notesSection.addCell(notesContentCell);

            document.add(notesSection);
        }
    }

    private void generatePaySlipContent(Document document, Color darkBlue, Color lightBlue, Color lightGray) throws Exception {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkBlue);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 100, 100));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, darkBlue);

        // ==================== EMPLOYEE INFO ====================
        PdfPTable empTable = new PdfPTable(2);
        empTable.setWidthPercentage(100);
        empTable.setSpacingAfter(20f);

        PdfPCell empInfoCell = new PdfPCell();
        empInfoCell.setBorder(PdfPCell.BOX);
        empInfoCell.setBorderColor(darkBlue);
        empInfoCell.setBorderWidth(1.5f);
        empInfoCell.setPadding(15);
        empInfoCell.setBackgroundColor(lightGray);

        Paragraph empHeader = new Paragraph("EMPLOYEE INFORMATION", sectionFont);
        empHeader.setSpacingAfter(10f);
        empInfoCell.addElement(empHeader);

        addFieldToCell(empInfoCell, "NAME", nameField.getText().toUpperCase(), labelFont, valueFont);
        addFieldToCell(empInfoCell, "ID", "GJR-" + String.format("%06d", currentGuard.getGuardId()), labelFont, valueFont);
        addFieldToCell(empInfoCell, "DESIGNATION", designationBox.getValue(), labelFont, valueFont);
        addFieldToCell(empInfoCell, "BATCH ID", batchIdField.getText(), labelFont, valueFont);

        empTable.addCell(empInfoCell);

        // Payment Period Cell
        PdfPCell periodCell = new PdfPCell();
        periodCell.setBorder(PdfPCell.BOX);
        periodCell.setBorderColor(darkBlue);
        periodCell.setBorderWidth(1.5f);
        periodCell.setPadding(15);
        periodCell.setBackgroundColor(lightGray);

        Paragraph periodHeader = new Paragraph("PAYMENT PERIOD", sectionFont);
        periodHeader.setSpacingAfter(10f);
        periodCell.addElement(periodHeader);

        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        addFieldToCell(periodCell, "MONTH", currentMonth, labelFont, valueFont);
        addFieldToCell(periodCell, "PAYMENT DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")), labelFont, valueFont);
        addFieldToCell(periodCell, "SHIFT", shiftBox.getValue(), labelFont, valueFont);
        addFieldToCell(periodCell, "STATUS", statusBadge.getText(), labelFont, valueFont);

        empTable.addCell(periodCell);

        document.add(empTable);

        // ==================== EARNINGS BREAKDOWN ====================
        Paragraph earningsHeader = new Paragraph("EARNINGS BREAKDOWN", sectionFont);
        earningsHeader.setSpacingAfter(10f);
        document.add(earningsHeader);

        double baseSalary = 0;
        try {
            baseSalary = Double.parseDouble(salaryField.getText());
        } catch (Exception e) {
            baseSalary = 0;
        }

        double hra = baseSalary * 0.20;
        double transportAllowance = 500.0;
        double specialAllowance = baseSalary * 0.10;
        double totalEarnings = baseSalary + hra + transportAllowance + specialAllowance;

        PdfPTable earningsTable = new PdfPTable(2);
        earningsTable.setWidthPercentage(100);
        earningsTable.setSpacingAfter(15f);

        addPayslipRow(earningsTable, "Basic Salary", String.format("$%.2f", baseSalary), darkBlue, lightBlue, false);
        addPayslipRow(earningsTable, "House Rent Allowance (20%)", String.format("$%.2f", hra), darkBlue, lightBlue, false);
        addPayslipRow(earningsTable, "Transport Allowance", String.format("$%.2f", transportAllowance), darkBlue, lightBlue, false);
        addPayslipRow(earningsTable, "Special Allowance (10%)", String.format("$%.2f", specialAllowance), darkBlue, lightBlue, false);
        addPayslipRow(earningsTable, "TOTAL EARNINGS", String.format("$%.2f", totalEarnings), darkBlue, new Color(200, 230, 201), true);

        document.add(earningsTable);

        // ==================== DEDUCTIONS ====================
        Paragraph deductionsHeader = new Paragraph("DEDUCTIONS", sectionFont);
        deductionsHeader.setSpacingAfter(10f);
        document.add(deductionsHeader);

        double providentFund = baseSalary * 0.12;
        double incomeTax = baseSalary * 0.05;
        double insurance = 150.0;
        double totalDeductions = providentFund + incomeTax + insurance;

        PdfPTable deductionsTable = new PdfPTable(2);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setSpacingAfter(15f);

        addPayslipRow(deductionsTable, "Provident Fund (12%)", String.format("$%.2f", providentFund), darkBlue, lightBlue, false);
        addPayslipRow(deductionsTable, "Income Tax (5%)", String.format("$%.2f", incomeTax), darkBlue, lightBlue, false);
        addPayslipRow(deductionsTable, "Insurance Premium", String.format("$%.2f", insurance), darkBlue, lightBlue, false);
        addPayslipRow(deductionsTable, "TOTAL DEDUCTIONS", String.format("$%.2f", totalDeductions), darkBlue, new Color(255, 205, 210), true);

        document.add(deductionsTable);

        // ==================== NET PAYABLE ====================
        double netPayable = totalEarnings - totalDeductions;

        PdfPTable netTable = new PdfPTable(1);
        netTable.setWidthPercentage(100);
        netTable.setSpacingAfter(20f);

        PdfPCell netCell = new PdfPCell();
        netCell.setBorder(PdfPCell.BOX);
        netCell.setBorderColor(darkBlue);
        netCell.setBorderWidth(2f);
        netCell.setPadding(20);
        netCell.setBackgroundColor(new Color(232, 245, 233));
        netCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph netLabel = new Paragraph("NET PAYABLE AMOUNT", sectionFont);
        netLabel.setAlignment(Element.ALIGN_CENTER);
        netLabel.setSpacingAfter(10f);
        netCell.addElement(netLabel);

        Paragraph netAmount = new Paragraph(String.format("$%.2f", netPayable), totalFont);
        netAmount.setAlignment(Element.ALIGN_CENTER);
        netCell.addElement(netAmount);

        netTable.addCell(netCell);
        document.add(netTable);

        // ==================== PAYMENT DETAILS ====================
        Paragraph paymentHeader = new Paragraph("PAYMENT DETAILS", sectionFont);
        paymentHeader.setSpacingAfter(10f);
        document.add(paymentHeader);

        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        paymentTable.setSpacingAfter(15f);

        addDetailRow(paymentTable, "Payment Mode", "Direct Bank Transfer", darkBlue, lightBlue);
        addDetailRow(paymentTable, "Bank Name", "Government Treasury Bank", darkBlue, lightBlue);
        addDetailRow(paymentTable, "Account Holder", nameField.getText(), darkBlue, lightBlue);

        document.add(paymentTable);

        // Note
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);
        noteTable.setSpacingAfter(20f);

        PdfPCell noteCell = new PdfPCell(new Paragraph(
                "NOTE: This is a computer-generated payslip and does not require a physical signature. " +
                        "All amounts are in USD. For any discrepancies, please contact the HR department within 7 days.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY)));
        noteCell.setBorder(PdfPCell.BOX);
        noteCell.setBorderColor(Color.LIGHT_GRAY);
        noteCell.setPadding(10);
        noteCell.setBackgroundColor(new Color(255, 248, 225));
        noteTable.addCell(noteCell);

        document.add(noteTable);
    }

    private void addFieldToCell(PdfPCell cell, String label, String value, Font labelFont, Font valueFont) {
        Paragraph labelPara = new Paragraph(label, labelFont);
        labelPara.setSpacingAfter(3f);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value != null && !value.isEmpty() ? value : "N/A", valueFont);
        valuePara.setSpacingAfter(12f);
        cell.addElement(valuePara);
    }

    private void addMetaCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDetailRow(PdfPTable table, String label, String value, Color darkBlue, Color lightBlue) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkBlue);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        PdfPCell c1 = new PdfPCell(new Paragraph(label, labelFont));
        c1.setBackgroundColor(lightBlue);
        c1.setPadding(10);
        c1.setPaddingLeft(12);
        c1.setBorderColor(new Color(220, 220, 220));
        c1.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell c2 = new PdfPCell(new Paragraph(value != null && !value.isEmpty() ? value : "N/A", valueFont));
        c2.setPadding(10);
        c2.setPaddingLeft(12);
        c2.setBorderColor(new Color(220, 220, 220));
        c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c2.setBackgroundColor(Color.WHITE);

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addPayslipRow(PdfPTable table, String label, String value, Color darkBlue, Color bgColor, boolean isBold) {
        Font labelFont = isBold ?
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkBlue) :
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font valueFont = isBold ?
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkBlue) :
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        PdfPCell c1 = new PdfPCell(new Paragraph(label, labelFont));
        c1.setBackgroundColor(bgColor);
        c1.setPadding(10);
        c1.setBorderColor(new Color(200, 200, 200));
        if (isBold) c1.setBorderWidth(1.5f);

        PdfPCell c2 = new PdfPCell(new Paragraph(value, valueFont));
        c2.setBackgroundColor(bgColor);
        c2.setPadding(10);
        c2.setBorderColor(new Color(200, 200, 200));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (isBold) c2.setBorderWidth(1.5f);

        table.addCell(c1);
        table.addCell(c2);
    }

    private String calculateLiveStatus(String shift) {
        if (shift == null || shift.equalsIgnoreCase("On Leave")) return "INACTIVE";
        try {
            String hoursOnly = shift.replaceAll("[^0-9-]", "");
            String[] parts = hoursOnly.split("-");
            int startHour = Integer.parseInt(parts[0]);
            int endHour = Integer.parseInt(parts[1]);
            int currentHour = LocalTime.now().getHour();
            boolean isActive = (startHour < endHour) ? (currentHour >= startHour && currentHour < endHour) : (currentHour >= startHour || currentHour < endHour);
            return isActive ? "ACTIVE" : "INACTIVE";
        } catch (Exception e) {
            return "INACTIVE";
        }
    }

    private void applyStatusColor(String status) {
        statusBadge.setText(status);
        String color = status.equals("ACTIVE") ? "#16a34a" : "#dc2626";
        statusBadge.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-padding:10 20; -fx-background-radius:20; -fx-font-weight:bold;");
    }

    private void loadGuardImage(int guardId) {
        try {
            File file = new File("python-face/photos/guards/" + guardId + ".jpg");
            if (file.exists()) guardImage.setImage(new Image(file.toURI().toString()));
        } catch (Exception ignored) {}
    }

    @FXML
    private void uploadPhoto() {
        FileChooser chooser = new FileChooser();
        File f = chooser.showOpenDialog(guardImage.getScene().getWindow());
        if (f != null) guardImage.setImage(new Image(f.toURI().toString()));
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}