package com.prison.controller;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;
import com.prison.util.PythonRunnerUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

// Explicit JavaFX Imports
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

// PDF Imports
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GuardProfileController {

    // ── FXML Nodes ─────────────────────────────────────────────────────────
    @FXML private ImageView guardImage;
    @FXML private TextField nameField, ageField, addressField, transferField, salaryField;
    @FXML private TextField aadharField, phoneField, batchIdField, emailField;
    @FXML private ComboBox<String> shiftBox, genderBox, designationBox;
    @FXML private Label statusBadge;
    @FXML private DatePicker joiningDatePicker, birthDatePicker;
    @FXML private TextArea descriptionArea;
    @FXML private ScrollPane mainScrollPane;
    @FXML private Label profileModeLabel;
    @FXML private Label profileTitleLabel;
    @FXML private Button photoButton;
    @FXML private Button printInfoBtn;
    @FXML private Button paySlipBtn;
    @FXML private Button saveBtn;

    // ── State ──────────────────────────────────────────────────────────────
    private Guard      currentGuard;
    private final GuardDao guardDao = new GuardDao();
    private Runnable   onSaveCallback;

    private boolean isNewGuard = false;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        shiftBox.setItems(FXCollections.observableArrayList(
                "Morning (06-14)", "Evening (14-22)", "Night (22-06)", "On Leave"));
        genderBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        designationBox.setItems(FXCollections.observableArrayList(
                "Gate Guard", "Tower Guard", "Control Room", "Escort Officer", "Supervisor"));

        designationBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> updateSalary(newVal));

        setupValidations();
        setupFastScroll();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NEW GUARD MODE
    // ══════════════════════════════════════════════════════════════════════
    public void setNewGuardMode() {
        isNewGuard   = true;
        currentGuard = null;
        salaryField.setEditable(true);

        if (profileModeLabel != null) profileModeLabel.setText("REGISTER NEW PERSONNEL");
        if (profileTitleLabel != null) profileTitleLabel.setText("New Guard Registration");

        stylePhotoBtn(true);
        styleRegisterBtn();
        hidePrintButtons();
        applyStatusColor("INACTIVE");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOAD EXISTING GUARD
    // ══════════════════════════════════════════════════════════════════════
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
        aadharField.setText(g.getAadharNumber()  != null ? g.getAadharNumber()  : "");
        phoneField.setText(g.getPhoneNumber()    != null ? g.getPhoneNumber()   : "");
        batchIdField.setText(g.getBatchId()      != null ? g.getBatchId()       : "");
        emailField.setText(g.getEmail()          != null ? g.getEmail()         : "");

        applyStatusColor(calculateLiveStatus(g.getShift()));
        loadGuardImage(g.getGuardId());
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PHOTO BUTTON
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void uploadPhoto() {

        if (isNewGuard) {
            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                showError("Name Required",
                        "Please enter the guard's full name before capturing a photo.");
                return;
            }
            if (designationBox.getValue() == null) {
                showError("Designation Required",
                        "Please select a designation before capturing a photo.");
                return;
            }
            if (shiftBox.getValue() == null) {
                showError("Shift Required",
                        "Please select a shift before capturing a photo.");
                return;
            }

            try {
                syncDatePickerValue(birthDatePicker);
                syncDatePickerValue(joiningDatePicker);

                Guard g = buildPartialGuard();

                int id = guardDao.saveAndReturnId(g);
                if (id <= 0) {
                    showError("Save Failed",
                            "Could not create the guard record. Please try again.");
                    return;
                }

                g.setGuardId(id);
                currentGuard = g;
                isNewGuard   = false;

                if (profileModeLabel != null) profileModeLabel.setText("PERSONNEL MANAGEMENT");
                if (profileTitleLabel != null) profileTitleLabel.setText("Guard Profile");
                stylePhotoBtn(false);
                styleSaveBtn();
                showPrintButtons();

                if (onSaveCallback != null) onSaveCallback.run();

                launchTrainingAsync(id);

            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "An unexpected error occurred: " + e.getMessage());
            }

        } else {
            if (currentGuard == null) return;
            launchTrainingAsync(currentGuard.getGuardId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RUN PYTHON TRAINING ON A BACKGROUND THREAD
    // ══════════════════════════════════════════════════════════════════════
    private void launchTrainingAsync(int guardId) {
        photoButton.setDisable(true);
        photoButton.setText("📡  Camera Opening…");

        new Thread(() -> {

            PythonRunnerUtil.trainFace("GUARD", guardId);

            Platform.runLater(() -> {
                File photo = new File(
                        "python-face/photos/guards/" + guardId + ".jpg");

                if (photo.exists()) {
                    // Load via InputStream to bypass JavaFX URL cache — always shows latest photo
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(photo)) {
                        guardImage.setImage(new Image(fis));
                    } catch (Exception ignored) {}
                }

                photoButton.setDisable(false);
                photoButton.setText("🔄  Retake Photo");
                photoButton.setStyle(
                        "-fx-background-color: #f39c12; -fx-text-fill: white; " +
                                "-fx-font-weight: 700; -fx-font-size: 14; -fx-padding: 12 25; " +
                                "-fx-background-radius: 8; -fx-cursor: hand;");
            });

        }, "guard-training-thread").start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SAVE / UPDATE PROFILE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void updateGuard() {

        if (isNewGuard) {
            if (!validateInputs()) return;

            try {
                syncDatePickerValue(birthDatePicker);
                syncDatePickerValue(joiningDatePicker);

                Guard g = new Guard();
                mapFormToGuard(g);

                int id = guardDao.saveAndReturnId(g);
                if (id <= 0) {
                    showError("Database Error", "Could not save the guard. Please try again.");
                    return;
                }

                g.setGuardId(id);
                currentGuard = g;
                isNewGuard   = false;

                if (profileModeLabel != null) profileModeLabel.setText("PERSONNEL MANAGEMENT");
                if (profileTitleLabel != null) profileTitleLabel.setText("Guard Profile");
                stylePhotoBtn(false);
                styleSaveBtn();
                showPrintButtons();

                applyStatusColor(g.getStatus());
                if (onSaveCallback != null) onSaveCallback.run();

                // ── Refresh image immediately after new save ──
                loadGuardImage(id);

                new Alert(Alert.AlertType.INFORMATION,
                        "Guard registered! ID: GJR-" + String.format("%06d", id) +
                                "\nTip: click 'Change Photo' to capture a face for recognition.").show();

            } catch (NumberFormatException e) {
                showError("Invalid Input", "Age and Salary must be numbers.");
            } catch (Exception e) {
                showError("Error", e.getMessage());
                e.printStackTrace();
            }

        } else {
            if (currentGuard == null) return;
            if (!validateInputs()) return;

            try {
                syncDatePickerValue(birthDatePicker);
                syncDatePickerValue(joiningDatePicker);
                mapFormToGuard(currentGuard);
                guardDao.update(currentGuard);

                applyStatusColor(currentGuard.getStatus());
                if (onSaveCallback != null) onSaveCallback.run();

                // ── Refresh image immediately after update ──
                loadGuardImage(currentGuard.getGuardId());

                new Alert(Alert.AlertType.INFORMATION,
                        "Guard Profile Updated & Database Synced!").show();

            } catch (NumberFormatException e) {
                showError("Invalid Input", "Age and Salary must be numbers.");
            } catch (Exception e) {
                showError("Error", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FORM → MODEL HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private void mapFormToGuard(Guard g) {
        g.setName(nameField.getText().trim());
        try { g.setAge(Integer.parseInt(ageField.getText().trim())); } catch (Exception ignored) {}
        g.setAddress(addressField.getText());
        g.setTransferFrom(transferField.getText());
        g.setGender(genderBox.getValue());
        g.setBirthDate(birthDatePicker.getValue());
        g.setShift(shiftBox.getValue());
        g.setJoiningDate(joiningDatePicker.getValue());
        g.setDescription(descriptionArea.getText());
        g.setDesignation(designationBox.getValue());
        try { g.setSalary(Double.parseDouble(salaryField.getText().trim())); } catch (Exception ignored) {}
        g.setAadharNumber(aadharField.getText());
        g.setPhoneNumber(phoneField.getText());
        g.setBatchId(batchIdField.getText());
        g.setEmail(emailField.getText());
        g.setStatus(calculateLiveStatus(g.getShift()));
    }

    private Guard buildPartialGuard() {
        Guard g = new Guard();
        g.setName(nameField.getText().trim());
        g.setDesignation(designationBox.getValue());
        g.setShift(shiftBox.getValue());
        g.setStatus(calculateLiveStatus(g.getShift()));
        try { g.setAge(Integer.parseInt(ageField.getText().trim())); } catch (Exception ignored) {}
        g.setAddress(addressField.getText());
        g.setTransferFrom(transferField.getText());
        g.setGender(genderBox.getValue());
        g.setBirthDate(birthDatePicker.getValue());
        g.setJoiningDate(joiningDatePicker.getValue());
        g.setDescription(descriptionArea.getText());
        try { g.setSalary(Double.parseDouble(salaryField.getText().trim())); } catch (Exception ignored) {}
        g.setAadharNumber(aadharField.getText());
        g.setPhoneNumber(phoneField.getText());
        g.setBatchId(batchIdField.getText());
        g.setEmail(emailField.getText());
        return g;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════════════
    private boolean validateInputs() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Full name is required."); return false;
        }
        if (designationBox.getValue() == null) {
            showError("Validation Error", "Please select a designation."); return false;
        }
        if (shiftBox.getValue() == null) {
            showError("Validation Error", "Please select a shift."); return false;
        }
        String phone  = phoneField.getText();
        String aadhar = aadharField.getText();
        String email  = emailField.getText();
        if (phone == null || phone.length() != 10 || !phone.matches("\\d+")) {
            showError("Validation Error", "Phone number must be exactly 10 digits."); return false;
        }
        if (aadhar == null || aadhar.length() != 12 || !aadhar.matches("\\d+")) {
            showError("Validation Error", "Aadhar number must be exactly 12 digits."); return false;
        }
        if (email == null || !email.contains("@") || !email.toLowerCase().endsWith(".com")) {
            showError("Validation Error", "Email must contain @ and end with .com."); return false;
        }
        return true;
    }

    private void setupValidations() {
        phoneField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) phoneField.setText(newVal.replaceAll("[^\\d]", ""));
            if (phoneField.getText().length() > 10) phoneField.setText(old);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUTTON STYLING HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private void stylePhotoBtn(boolean isNew) {
        if (isNew) {
            photoButton.setText("📷  Add Photo");
            photoButton.setStyle(
                    "-fx-background-color: #2dce89; -fx-text-fill: white; -fx-font-weight: 700; " +
                            "-fx-font-size: 14; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            photoButton.setText("🔄  Change Photo");
            photoButton.setStyle(
                    "-fx-background-color: #5e72e4; -fx-text-fill: white; -fx-font-weight: 700; " +
                            "-fx-font-size: 14; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private void styleRegisterBtn() {
        saveBtn.setText("✔  REGISTER GUARD");
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; " +
                "-fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10;");
    }

    private void styleSaveBtn() {
        saveBtn.setText("SAVE PROFILE");
        saveBtn.setStyle("-fx-background-color: #5e72e4; -fx-text-fill: white; " +
                "-fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10;");
    }

    private void hidePrintButtons() {
        printInfoBtn.setVisible(false); printInfoBtn.setManaged(false);
        paySlipBtn.setVisible(false);   paySlipBtn.setManaged(false);
    }

    private void showPrintButtons() {
        printInfoBtn.setVisible(true); printInfoBtn.setManaged(true);
        paySlipBtn.setVisible(true);   paySlipBtn.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PRINT
    // ══════════════════════════════════════════════════════════════════════
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
        fileChooser.setInitialFileName(filePrefix +
                (batchIdField.getText().isEmpty()
                        ? String.valueOf(currentGuard.getGuardId())
                        : batchIdField.getText()) + ".pdf");
        File file = fileChooser.showSaveDialog(nameField.getScene().getWindow());
        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 60);
            PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(file));

            Color darkBlue  = new Color(0, 51, 102);
            Color lightBlue = new Color(240, 245, 250);
            Color lightGray = new Color(250, 250, 250);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2f);  cb.setColorStroke(new Color(0,51,102));
                        cb.rectangle(30,30,doc.getPageSize().getWidth()-60,doc.getPageSize().getHeight()-60); cb.stroke();
                        cb.setLineWidth(0.5f);
                        cb.rectangle(35,35,doc.getPageSize().getWidth()-70,doc.getPageSize().getHeight()-70); cb.stroke();
                        cb.setLineWidth(1f); cb.moveTo(40,50); cb.lineTo(doc.getPageSize().getWidth()-40,50); cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA,7,Color.GRAY);
                        ColumnText.showTextAligned(cb,Element.ALIGN_LEFT,   new Phrase("CONFIDENTIAL - FOR OFFICIAL USE ONLY",ff),40,35,0);
                        ColumnText.showTextAligned(cb,Element.ALIGN_CENTER, new Phrase("Department of Corrections & Prison Security",ff),doc.getPageSize().getWidth()/2,35,0);
                        ColumnText.showTextAligned(cb,Element.ALIGN_RIGHT,  new Phrase("Page "+w.getPageNumber(),ff),doc.getPageSize().getWidth()-40,35,0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // Header
            PdfPTable ht = new PdfPTable(new float[]{1,4,1});
            ht.setWidthPercentage(100); ht.setSpacingAfter(20f);
            ht.addCell(logoCell());
            PdfPCell cc = new PdfPCell();
            cc.setBorder(PdfPCell.NO_BORDER); cc.setHorizontalAlignment(Element.ALIGN_CENTER); cc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph dp = new Paragraph("DEPARTMENT OF CORRECTIONS\n& PRISON SECURITY", FontFactory.getFont(FontFactory.HELVETICA_BOLD,14,darkBlue));
            dp.setAlignment(Element.ALIGN_CENTER); dp.setSpacingAfter(8f); cc.addElement(dp);
            PdfPTable lt = new PdfPTable(1); lt.setWidthPercentage(80);
            PdfPCell lc2 = new PdfPCell(); lc2.setBorder(PdfPCell.NO_BORDER); lc2.setBorderWidthTop(2f); lc2.setBorderColorTop(darkBlue); lc2.setFixedHeight(0f); lt.addCell(lc2); cc.addElement(lt);
            Paragraph tp = new Paragraph("\n"+docTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD,20,darkBlue));
            tp.setAlignment(Element.ALIGN_CENTER); tp.setSpacingAfter(3f); cc.addElement(tp);
            Paragraph sp = new Paragraph("Security Clearance Level: Restricted", FontFactory.getFont(FontFactory.HELVETICA,9,Color.DARK_GRAY));
            sp.setAlignment(Element.ALIGN_CENTER); cc.addElement(sp);
            ht.addCell(cc); ht.addCell(logoCell()); document.add(ht);

            // Meta bar
            PdfPTable mt = new PdfPTable(3); mt.setWidthPercentage(100); mt.setSpacingAfter(15f);
            Font mf = FontFactory.getFont(FontFactory.HELVETICA,8,Color.WHITE);
            String bid = batchIdField.getText()!=null&&!batchIdField.getText().isEmpty() ? batchIdField.getText() : "N/A";
            addMetaCell(mt, isPaySlip?"PAYSLIP ID: "+bid:"BATCH ID: "+bid, mf, darkBlue);
            addMetaCell(mt, "STATUS: "+statusBadge.getText(), mf, darkBlue);
            addMetaCell(mt, "ISSUED: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), mf, darkBlue);
            document.add(mt);

            if (isPaySlip) generatePaySlipContent(document, darkBlue, lightBlue, lightGray);
            else           generateProfileContent(document, darkBlue, lightBlue, lightGray);

            // Signature
            PdfPTable st = new PdfPTable(2); st.setWidthPercentage(100); st.setSpacingBefore(30f);
            Font sf = FontFactory.getFont(FontFactory.HELVETICA,9,Color.BLACK);
            PdfPCell d2 = new PdfPCell(); d2.setBorder(PdfPCell.NO_BORDER); d2.addElement(new Paragraph("Date: ___________________",sf)); st.addCell(d2);
            PdfPCell s2 = new PdfPCell(); s2.setBorder(PdfPCell.NO_BORDER); s2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph sl2=new Paragraph("_____________________________",sf); sl2.setAlignment(Element.ALIGN_RIGHT); s2.addElement(sl2);
            Paragraph slb=new Paragraph("Authorized Official Signature",FontFactory.getFont(FontFactory.HELVETICA,8,Color.GRAY)); slb.setAlignment(Element.ALIGN_RIGHT); s2.addElement(slb);
            Paragraph sti=new Paragraph("Chief Security Officer",FontFactory.getFont(FontFactory.HELVETICA_BOLD,9,Color.BLACK)); sti.setAlignment(Element.ALIGN_RIGHT); s2.addElement(sti);
            st.addCell(s2); document.add(st);

            // Verification strip
            document.add(new Paragraph("\n"));
            PdfPTable vt = new PdfPTable(1); vt.setWidthPercentage(100);
            PdfPCell vc = new PdfPCell(new Paragraph(
                    "This is an official government document. Unauthorized reproduction, alteration, or disclosure is prohibited by law. " +
                            "Document ID: DOC-"+System.currentTimeMillis()%1000000+" | Generated: "+
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")),
                    FontFactory.getFont(FontFactory.HELVETICA,7,Color.DARK_GRAY)));
            vc.setBorder(PdfPCell.TOP); vc.setBorderColor(Color.LIGHT_GRAY);
            vc.setHorizontalAlignment(Element.ALIGN_CENTER); vc.setPadding(10);
            vc.setBackgroundColor(new Color(245,245,245)); vt.addCell(vc); document.add(vt);

            document.close();
            new Alert(Alert.AlertType.INFORMATION,"Official Document Exported Successfully").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,"Export Failed: "+e.getMessage()).show();
        }
    }

    // ── Profile body ─────────────────────────────────────────────────────
    private void generateProfileContent(Document document, Color darkBlue, Color lightBlue, Color lightGray) throws Exception {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,darkBlue);
        Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,9,new Color(100,100,100));
        Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,Color.BLACK);

        PdfPTable mainTable = new PdfPTable(new float[]{3.2f,2.8f});
        mainTable.setWidthPercentage(100); mainTable.setSpacingAfter(15f); mainTable.setKeepTogether(true);

        PdfPCell dataCell = new PdfPCell();
        dataCell.setBorder(PdfPCell.BOX); dataCell.setBorderColor(darkBlue); dataCell.setBorderWidth(1.5f);
        dataCell.setPadding(15); dataCell.setBackgroundColor(lightGray); dataCell.setVerticalAlignment(Element.ALIGN_TOP);
        Paragraph ph = new Paragraph("PERSONNEL IDENTIFICATION",sectionFont); ph.setSpacingAfter(12f); dataCell.addElement(ph);
        addFieldToCell(dataCell,"IDENTIFICATION NUMBER","GJR-"+String.format("%06d",currentGuard.getGuardId()),labelFont,valueFont);
        addFieldToCell(dataCell,"FULL LEGAL NAME",nameField.getText().toUpperCase(),labelFont,valueFont);
        addFieldToCell(dataCell,"BATCH ID",batchIdField.getText(),labelFont,valueFont);
        addFieldToCell(dataCell,"CURRENT ASSIGNMENT",designationBox.getValue(),labelFont,valueFont);
        addFieldToCell(dataCell,"DATE OF BIRTH",birthDatePicker.getValue()!=null?birthDatePicker.getValue().toString():"N/A",labelFont,valueFont);
        dataCell.addElement(new Paragraph("GENDER",labelFont));
        Paragraph gv=new Paragraph(genderBox.getValue()!=null&&!genderBox.getValue().isEmpty()?genderBox.getValue():"N/A",valueFont);
        gv.setSpacingAfter(0f); dataCell.addElement(gv);
        mainTable.addCell(dataCell);

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(PdfPCell.BOX); photoCell.setBorderColor(darkBlue); photoCell.setBorderWidth(1.5f);
        photoCell.setPadding(12); photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_TOP); photoCell.setBackgroundColor(Color.WHITE);
        Paragraph phpH=new Paragraph("OFFICIAL PHOTOGRAPH",FontFactory.getFont(FontFactory.HELVETICA_BOLD,9,darkBlue));
        phpH.setAlignment(Element.ALIGN_CENTER); phpH.setSpacingAfter(10f); photoCell.addElement(phpH);
        File imgFile=new File("python-face/photos/guards/"+currentGuard.getGuardId()+".jpg");
        if (imgFile.exists()) {
            com.lowagie.text.Image pi=com.lowagie.text.Image.getInstance(imgFile.getAbsolutePath());
            pi.scaleToFit(150,170); pi.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(pi);
        } else {
            Paragraph np=new Paragraph("\n\n\nNO PHOTOGRAPH\nON FILE\n\n\n",FontFactory.getFont(FontFactory.HELVETICA_BOLD,10,Color.GRAY));
            np.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(np);
        }
        Paragraph pd=new Paragraph("\nPhoto Date: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                FontFactory.getFont(FontFactory.HELVETICA,7,Color.GRAY));
        pd.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(pd);
        mainTable.addCell(photoCell); document.add(mainTable);

        // Contact section
        addSectionWithRows(document,sectionFont,"CONTACT INFORMATION",
                new String[][]{
                        {"Aadhar Number",aadharField.getText()},
                        {"Phone Number",phoneField.getText()},
                        {"Email Address",emailField.getText()},
                        {"Residential Address",addressField.getText()}
                },darkBlue,lightBlue);

        // Employment section
        addSectionWithRows(document,sectionFont,"EMPLOYMENT SPECIFICATION",
                new String[][]{
                        {"Shift Schedule",shiftBox.getValue()},
                        {"Annual Salary Grade"," "+salaryField.getText()},
                        {"Current Duty Status",statusBadge.getText()},
                        {"Enlistment Date",joiningDatePicker.getValue()!=null?joiningDatePicker.getValue().toString():"N/A"},
                        {"Transfer From",transferField.getText()},
                        {"Age",ageField.getText()+" years"}
                },darkBlue,lightBlue);

        // Notes
        if (descriptionArea.getText()!=null&&!descriptionArea.getText().trim().isEmpty()) {
            PdfPTable ns=new PdfPTable(1); ns.setWidthPercentage(100); ns.setSpacingBefore(8f); ns.setSpacingAfter(20f); ns.setKeepTogether(true);
            PdfPCell nh=new PdfPCell(new Paragraph("NOTES / BIOMETRIC DATA / SPECIAL REMARKS",sectionFont));
            nh.setBorder(PdfPCell.NO_BORDER); nh.setBackgroundColor(new Color(240,245,250)); nh.setPadding(8); nh.setPaddingLeft(10); ns.addCell(nh);
            PdfPCell nc2=new PdfPCell(new Paragraph(descriptionArea.getText(),FontFactory.getFont(FontFactory.HELVETICA,10,Color.BLACK)));
            nc2.setBorder(PdfPCell.BOX); nc2.setBorderColor(new Color(200,200,200)); nc2.setPadding(12); nc2.setMinimumHeight(70); nc2.setBackgroundColor(Color.WHITE);
            ns.addCell(nc2); document.add(ns);
        }
    }

    // ── Payslip body ─────────────────────────────────────────────────────
    private void generatePaySlipContent(Document document, Color darkBlue, Color lightBlue, Color lightGray) throws Exception {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,darkBlue);
        Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,9,new Color(100,100,100));
        Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,Color.BLACK);
        Font totalFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,14,darkBlue);

        PdfPTable empT=new PdfPTable(2); empT.setWidthPercentage(100); empT.setSpacingAfter(20f);
        PdfPCell ei=new PdfPCell(); ei.setBorder(PdfPCell.BOX); ei.setBorderColor(darkBlue); ei.setBorderWidth(1.5f); ei.setPadding(15); ei.setBackgroundColor(lightGray);
        Paragraph eh=new Paragraph("EMPLOYEE INFORMATION",sectionFont); eh.setSpacingAfter(10f); ei.addElement(eh);
        addFieldToCell(ei,"NAME",nameField.getText().toUpperCase(),labelFont,valueFont);
        addFieldToCell(ei,"ID","GJR-"+String.format("%06d",currentGuard.getGuardId()),labelFont,valueFont);
        addFieldToCell(ei,"DESIGNATION",designationBox.getValue(),labelFont,valueFont);
        addFieldToCell(ei,"BATCH ID",batchIdField.getText(),labelFont,valueFont);
        empT.addCell(ei);

        PdfPCell pc=new PdfPCell(); pc.setBorder(PdfPCell.BOX); pc.setBorderColor(darkBlue); pc.setBorderWidth(1.5f); pc.setPadding(15); pc.setBackgroundColor(lightGray);
        Paragraph pp=new Paragraph("PAYMENT PERIOD",sectionFont); pp.setSpacingAfter(10f); pc.addElement(pp);
        addFieldToCell(pc,"MONTH",LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")),labelFont,valueFont);
        addFieldToCell(pc,"PAYMENT DATE",LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),labelFont,valueFont);
        addFieldToCell(pc,"SHIFT",shiftBox.getValue(),labelFont,valueFont);
        addFieldToCell(pc,"STATUS",statusBadge.getText(),labelFont,valueFont);
        empT.addCell(pc); document.add(empT);

        Paragraph earnH=new Paragraph("EARNINGS BREAKDOWN",sectionFont); earnH.setSpacingAfter(10f); document.add(earnH);
        double base=0; try{base=Double.parseDouble(salaryField.getText());}catch(Exception ignored){}
        double hra=base*0.20, transport=500.0, special=base*0.10, total=base+hra+transport+special;
        PdfPTable et=new PdfPTable(2); et.setWidthPercentage(100); et.setSpacingAfter(15f);
        addPayslipRow(et,"Basic Salary",          String.format("%.2f",base),      darkBlue,lightBlue,false);
        addPayslipRow(et,"HRA (20%)",             String.format("%.2f",hra),       darkBlue,lightBlue,false);
        addPayslipRow(et,"Transport Allowance",   String.format("%.2f",transport), darkBlue,lightBlue,false);
        addPayslipRow(et,"Special Allowance (10%)",String.format("%.2f",special),  darkBlue,lightBlue,false);
        addPayslipRow(et,"TOTAL EARNINGS",        String.format("Rs.%.2f",total),     darkBlue,new Color(200,230,201),true);
        document.add(et);

        Paragraph dedH=new Paragraph("DEDUCTIONS",sectionFont); dedH.setSpacingAfter(10f); document.add(dedH);
        double pf=base*0.12, tax=base*0.05, ins=150.0, tDed=pf+tax+ins;
        PdfPTable dt=new PdfPTable(2); dt.setWidthPercentage(100); dt.setSpacingAfter(15f);
        addPayslipRow(dt,"Provident Fund (12%)",String.format("%.2f",pf),   darkBlue,lightBlue,false);
        addPayslipRow(dt,"Income Tax (5%)",     String.format("%.2f",tax),  darkBlue,lightBlue,false);
        addPayslipRow(dt,"Insurance Premium",   String.format("%.2f",ins),  darkBlue,lightBlue,false);
        addPayslipRow(dt,"TOTAL DEDUCTIONS",    String.format("Rs.%.2f",tDed), darkBlue,new Color(255,205,210),true);
        document.add(dt);

        double net=total-tDed;
        PdfPTable nt=new PdfPTable(1); nt.setWidthPercentage(100); nt.setSpacingAfter(20f);
        PdfPCell nc3=new PdfPCell(); nc3.setBorder(PdfPCell.BOX); nc3.setBorderColor(darkBlue); nc3.setBorderWidth(2f);
        nc3.setPadding(20); nc3.setBackgroundColor(new Color(232,245,233)); nc3.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph nl=new Paragraph("NET PAYABLE AMOUNT",sectionFont); nl.setAlignment(Element.ALIGN_CENTER); nl.setSpacingAfter(10f); nc3.addElement(nl);
        Paragraph na=new Paragraph(String.format("Rs.%.2f",net),totalFont); na.setAlignment(Element.ALIGN_CENTER); nc3.addElement(na);
        nt.addCell(nc3); document.add(nt);

        Paragraph payH=new Paragraph("PAYMENT DETAILS",sectionFont); payH.setSpacingAfter(10f); document.add(payH);
        PdfPTable pt2=new PdfPTable(2); pt2.setWidthPercentage(100); pt2.setSpacingAfter(15f);
        addDetailRow(pt2,"Payment Mode","Direct Bank Transfer",darkBlue,lightBlue);
        addDetailRow(pt2,"Bank Name","Government Treasury Bank",darkBlue,lightBlue);
        addDetailRow(pt2,"Account Holder",nameField.getText(),darkBlue,lightBlue);
        document.add(pt2);

        PdfPTable noteT=new PdfPTable(1); noteT.setWidthPercentage(100); noteT.setSpacingAfter(20f);
        PdfPCell noteC=new PdfPCell(new Paragraph(
                "NOTE: Computer-generated payslip. No physical signature required. " +
                        "All amounts in INR. Report discrepancies to HR within 7 days.",
                FontFactory.getFont(FontFactory.HELVETICA,8,Color.DARK_GRAY)));
        noteC.setBorder(PdfPCell.BOX); noteC.setBorderColor(Color.LIGHT_GRAY);
        noteC.setPadding(10); noteC.setBackgroundColor(new Color(255,248,225));
        noteT.addCell(noteC); document.add(noteT);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  REUSABLE PDF HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private PdfPCell logoCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            com.lowagie.text.Image logo =
                    com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
            logo.scaleToFit(60,60); cell.addElement(logo);
        } catch (Exception e) {
            cell.addElement(new Paragraph("LOGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD,8)));
        }
        return cell;
    }

    private void addSectionWithRows(Document doc, Font sectionFont, String title,
                                    String[][] rows, Color darkBlue, Color lightBlue) throws Exception {
        PdfPTable section=new PdfPTable(1); section.setWidthPercentage(100);
        section.setSpacingBefore(8f); section.setSpacingAfter(15f); section.setKeepTogether(true);
        PdfPCell head=new PdfPCell(new Paragraph(title,sectionFont));
        head.setBorder(PdfPCell.NO_BORDER); head.setBackgroundColor(new Color(240,245,250));
        head.setPadding(8); head.setPaddingLeft(10); section.addCell(head);
        PdfPTable inner=new PdfPTable(2); inner.setWidthPercentage(100);
        inner.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        for (String[] row:rows) addDetailRow(inner,row[0],row[1],darkBlue,lightBlue);
        PdfPCell data=new PdfPCell(inner); data.setBorder(PdfPCell.NO_BORDER); data.setPadding(0);
        section.addCell(data); doc.add(section);
    }

    private void addFieldToCell(PdfPCell cell,String label,String value,Font lf,Font vf) {
        Paragraph lp=new Paragraph(label,lf); lp.setSpacingAfter(3f); cell.addElement(lp);
        Paragraph vp=new Paragraph(value!=null&&!value.isEmpty()?value:"N/A",vf); vp.setSpacingAfter(12f); cell.addElement(vp);
    }

    private void addMetaCell(PdfPTable table,String text,Font font,Color bg) {
        PdfPCell c=new PdfPCell(new Phrase(text,font));
        c.setBackgroundColor(bg); c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(c);
    }

    private void addDetailRow(PdfPTable table,String label,String value,Color darkBlue,Color lightBlue) {
        Font lf=FontFactory.getFont(FontFactory.HELVETICA_BOLD,10,darkBlue);
        Font vf=FontFactory.getFont(FontFactory.HELVETICA,10,Color.BLACK);
        PdfPCell c1=new PdfPCell(new Paragraph(label,lf));
        c1.setBackgroundColor(lightBlue); c1.setPadding(10); c1.setPaddingLeft(12);
        c1.setBorderColor(new Color(220,220,220)); c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
        PdfPCell c2=new PdfPCell(new Paragraph(value!=null&&!value.isEmpty()?value:"N/A",vf));
        c2.setPadding(10); c2.setPaddingLeft(12); c2.setBorderColor(new Color(220,220,220));
        c2.setVerticalAlignment(Element.ALIGN_MIDDLE); c2.setBackgroundColor(Color.WHITE);
        table.addCell(c1); table.addCell(c2);
    }

    private void addPayslipRow(PdfPTable table,String label,String value,
                               Color darkBlue,Color bgColor,boolean isBold) {
        Font lf=isBold?FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,darkBlue):FontFactory.getFont(FontFactory.HELVETICA,10,Color.BLACK);
        Font vf=isBold?FontFactory.getFont(FontFactory.HELVETICA_BOLD,11,darkBlue):FontFactory.getFont(FontFactory.HELVETICA,10,Color.BLACK);
        PdfPCell c1=new PdfPCell(new Paragraph(label,lf));
        c1.setBackgroundColor(bgColor); c1.setPadding(10); c1.setBorderColor(new Color(200,200,200));
        if(isBold) c1.setBorderWidth(1.5f);
        PdfPCell c2=new PdfPCell(new Paragraph(value,vf));
        c2.setBackgroundColor(bgColor); c2.setPadding(10); c2.setBorderColor(new Color(200,200,200));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT); if(isBold) c2.setBorderWidth(1.5f);
        table.addCell(c1); table.addCell(c2);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MISC
    // ══════════════════════════════════════════════════════════════════════
    private void setupFastScroll() {
        if (mainScrollPane!=null&&mainScrollPane.getContent()!=null) {
            mainScrollPane.getContent().setOnScroll(ev -> {
                double deltaY=ev.getDeltaY()*3;
                double height=mainScrollPane.getContent().getBoundsInLocal().getHeight();
                mainScrollPane.setVvalue(mainScrollPane.getVvalue()-deltaY/height);
            });
        }
    }

    private void updateSalary(String role) {
        if (role == null) return;
        double amount = switch (role) {
            case "Supervisor"     -> 65000.0;
            case "Control Room"   -> 48000.0;
            case "Escort Officer" -> 42000.0;
            case "Tower Guard"    -> 38000.0;
            case "Gate Guard"     -> 35000.0;
            default               -> 30000.0;
        };
        salaryField.setText(String.valueOf(amount));
    }
    private String calculateLiveStatus(String shift) {
        if (shift==null||shift.equalsIgnoreCase("On Leave")) return "INACTIVE";
        try {
            String[] parts=shift.replaceAll("[^0-9-]","").split("-");
            int s=Integer.parseInt(parts[0]),e=Integer.parseInt(parts[1]),c=LocalTime.now().getHour();
            return ((s<e)?(c>=s&&c<e):(c>=s||c<e))?"ACTIVE":"INACTIVE";
        } catch(Exception ex){return "INACTIVE";}
    }

    private void applyStatusColor(String status) {
        statusBadge.setText(status);
        String color="ACTIVE".equals(status)?"#16a34a":"#dc2626";
        statusBadge.setStyle("-fx-background-color:"+color+"; -fx-text-fill:white; " +
                "-fx-padding:10 20; -fx-background-radius:20; -fx-font-weight:bold;");
    }

    private void loadGuardImage(int guardId) {
        try {
            File f = new File("python-face/photos/guards/" + guardId + ".jpg");
            if (f.exists()) {
                // Load via InputStream to bypass JavaFX URL cache — always shows latest photo
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                    guardImage.setImage(new Image(fis));
                }
            }
        } catch (Exception ignored) {}
    }

    private void syncDatePickerValue(DatePicker picker) {
        if (picker==null||!picker.isEditable()) return;
        try {
            String text=picker.getEditor().getText();
            picker.setValue((text==null||text.trim().isEmpty())?null:picker.getConverter().fromString(text));
        } catch(Exception e){System.err.println("Date parse error: "+e.getMessage());}
    }

    private void showError(String title,String message) {
        Alert a=new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
}