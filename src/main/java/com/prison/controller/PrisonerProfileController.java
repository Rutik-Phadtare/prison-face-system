package com.prison.controller;

import com.prison.dao.PrisonerDao;
import com.prison.model.Prisoner;
import com.prison.util.PythonRunnerUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
// NOTE: com.lowagie.text.Image is used fully-qualified below to avoid
// conflict with javafx.scene.image.Image used for the ImageView.

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PrisonerProfileController {

    // ── FXML Nodes ─────────────────────────────────────────────────────────
    @FXML private ImageView prisonerImage;
    @FXML private TextField  nameField, ageField, nationalityField, aadharField;
    @FXML private TextField  heightField, weightField, marksField, addressField;
    @FXML private TextField  emergencyContactField, emergencyPhoneField;
    @FXML private TextField  lawyerNameField, lawyerPhoneField;
    @FXML private ComboBox<String> genderBox, bloodTypeBox, crimeBox, cellBox;
    @FXML private ComboBox<String> dangerBox, behaviorBox;
    @FXML private Spinner<Integer> yearSpinner, monthSpinner;
    @FXML private DatePicker startDatePicker;
    @FXML private TextArea descriptionArea, incidentArea, visitorLogArea;
    @FXML private Label statusBadge, dangerBadge;
    @FXML private Label profileModeLabel, profileTitleLabel;
    @FXML private Button photoButton, printProfileBtn, behavioralLogBtn, saveBtn;
    @FXML private ScrollPane mainScrollPane;

    // ── State ──────────────────────────────────────────────────────────────
    private Prisoner       currentPrisoner;
    private final PrisonerDao dao = new PrisonerDao();
    private Runnable       onSaveCallback;

    /**
     * true  = registering a new prisoner (INSERT mode).
     * false = editing an existing prisoner (UPDATE mode).
     *
     * When the user clicks "Add Photo" in new-prisoner mode, we auto-save the
     * prisoner record first (to obtain a DB-assigned ID), then launch the Python
     * training script with that ID. After that, this flag flips to false.
     */
    private boolean isNewPrisoner = false;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        genderBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        bloodTypeBox.setItems(FXCollections.observableArrayList(
                "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-", "Unknown"));
        crimeBox.setEditable(true);
        crimeBox.setItems(FXCollections.observableArrayList(
                "Murder", "Robbery", "Theft", "Assault", "Fraud",
                "Rape", "Kidnapping", "Cyber Crime",
                "Drug Trafficking", "Terrorism", "Arson",
                "Extortion", "Human Trafficking", "Counterfeiting"));
        dangerBox.setItems(FXCollections.observableArrayList(
                "LOW", "MEDIUM", "HIGH", "MAXIMUM"));
        behaviorBox.setItems(FXCollections.observableArrayList(
                "EXCELLENT", "GOOD", "FAIR", "POOR"));

        loadCellList();

        yearSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0));
        monthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 11, 0));
        yearSpinner.setEditable(true);
        monthSpinner.setEditable(true);

        // Live danger badge
        dangerBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> applyDangerBadge(nv));

        setupFastScroll();
    }

    private void loadCellList() {

        // Set default date to today for the DatePicker
        startDatePicker.setValue(LocalDate.now());

        javafx.collections.ObservableList<String> cells =
                FXCollections.observableArrayList();
        for (int i = 1; i <= 100; i++) {
            int count = dao.countPrisonersInCell(String.valueOf(i));
            if (count < 2) cells.add(String.valueOf(i));
        }
        if (currentPrisoner != null && currentPrisoner.getCellNo() != null
                && !cells.contains(currentPrisoner.getCellNo())) {
            cells.add(0, currentPrisoner.getCellNo());
        }
        cellBox.setItems(cells);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  🆕 NEW PRISONER MODE  (mirrors setNewGuardMode)
    // ══════════════════════════════════════════════════════════════════════
    public void setNewPrisonerMode() {
        isNewPrisoner   = true;
        currentPrisoner = null;

        nameField.setText("");
        ageField.setText("");
        nationalityField.setText("");
        aadharField.setText("");
        heightField.setText("");
        weightField.setText("");
        marksField.setText("");
        addressField.setText("");
        emergencyContactField.setText("");
        emergencyPhoneField.setText("");
        lawyerNameField.setText("");
        lawyerPhoneField.setText("");
        descriptionArea.setText("");
        incidentArea.setText("");
        visitorLogArea.setText("");

        if (profileModeLabel != null) profileModeLabel.setText("REGISTER NEW INMATE");
        if (profileTitleLabel != null) profileTitleLabel.setText("New Prisoner Registration");

        stylePhotoBtn(true);
        styleRegisterBtn();
        hidePrintButtons();
        applyStatusBadge("IN CUSTODY");
        applyDangerBadge("LOW");
        dangerBox.setValue("LOW");
        behaviorBox.setValue("GOOD");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOAD EXISTING PRISONER  (mirrors setGuard)
    // ══════════════════════════════════════════════════════════════════════
    public void setPrisoner(Prisoner p) {
        this.currentPrisoner = p;

        nameField.setText(nvl(p.getName()));
        ageField.setText(p.getAge() > 0 ? String.valueOf(p.getAge()) : "");
        nationalityField.setText(nvl(p.getNationality()));
        aadharField.setText(nvl(p.getAadharNumber()));
        heightField.setText(nvl(p.getHeight()));
        weightField.setText(nvl(p.getWeight()));
        marksField.setText(nvl(p.getIdentificationMarks()));
        addressField.setText(nvl(p.getHomeAddress()));
        emergencyContactField.setText(nvl(p.getEmergencyContact()));
        emergencyPhoneField.setText(nvl(p.getEmergencyPhone()));
        lawyerNameField.setText(nvl(p.getLawyerName()));
        lawyerPhoneField.setText(nvl(p.getLawyerPhone()));

        genderBox.setValue(p.getGender());
        bloodTypeBox.setValue(p.getBloodType());
        crimeBox.setValue(p.getCrime());
        cellBox.setValue(p.getCellNo());
        dangerBox.setValue(p.getDangerLevel() != null ? p.getDangerLevel() : "LOW");
        behaviorBox.setValue(p.getBehaviorRating() != null ? p.getBehaviorRating() : "GOOD");

        yearSpinner.getValueFactory().setValue(p.getSentenceYears());
        monthSpinner.getValueFactory().setValue(0);
        startDatePicker.setValue(p.getSentenceStartDate());

        descriptionArea.setText(nvl(p.getDescription()));
        incidentArea.setText(nvl(p.getIncidentNotes()));
        visitorLogArea.setText(nvl(p.getVisitorLog()));

        applyStatusBadge(p.getStatus());
        applyDangerBadge(p.getDangerLevel());
        loadPrisonerImage(p.getPrisonerId());
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  📷 PHOTO BUTTON — mirrors uploadPhoto() in GuardProfileController
    //
    //  NEW PRISONER     → minimal validation, auto-save to get DB id,
    //                     flip to UPDATE mode, launch Python training.
    //  EXISTING PRISONER → launch Python training directly.
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void uploadPhoto() {
        if (isNewPrisoner) {
            // ── Minimal validation ───────────────────────────────────────
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showError("Name Required",
                        "Please enter the prisoner's full name before capturing a photo.");
                return;
            }
            if (crimeBox.getValue() == null) {
                showError("Crime Required",
                        "Please select or enter the crime before capturing a photo.");
                return;
            }
            if (cellBox.getValue() == null) {
                showError("Cell Required",
                        "Please select a cell number before capturing a photo.");
                return;
            }

            try {
                // Build partial record and auto-save to get a real DB id
                Prisoner p = buildPartialPrisoner();
                int id = dao.saveAndReturnId(p);
                if (id <= 0) {
                    showError("Save Failed",
                            "Could not create the prisoner record. Please try again.");
                    return;
                }

                p.setPrisonerId(id);
                currentPrisoner = p;
                isNewPrisoner   = false;   // now in UPDATE mode

                if (profileModeLabel != null) profileModeLabel.setText("INMATE RECORDS SYSTEM");
                if (profileTitleLabel != null) profileTitleLabel.setText("Prisoner Profile");
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
            if (currentPrisoner == null) return;
            launchTrainingAsync(currentPrisoner.getPrisonerId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PYTHON WEBCAM TRAINING — background thread (mirrors launchTrainingAsync)
    // ══════════════════════════════════════════════════════════════════════
    private void launchTrainingAsync(int prisonerId) {
        photoButton.setDisable(true);
        photoButton.setText("📡  Camera Opening…");

        new Thread(() -> {

            // ── Option A: use PythonRunnerUtil (must block until script exits) ──
            PythonRunnerUtil.trainFace("PRISONER", prisonerId);

            /*
             * ── Option B: call script directly (always blocking) ──────────────
             *    Use if PythonRunnerUtil.trainFace() is non-blocking.
             *
             *    String dir = "python-face";
             *    ProcessBuilder pb = new ProcessBuilder(
             *            "python", dir + "/train.py",
             *            "PRISONER", String.valueOf(prisonerId));
             *    pb.directory(new java.io.File(dir));
             *    pb.inheritIO();
             *    try { pb.start().waitFor(); } catch (Exception ignored) {}
             */

            Platform.runLater(() -> {
                File photo = new File(
                        "python-face/photos/prisoners/" + prisonerId + ".jpg");
                if (photo.exists()) {
                    prisonerImage.setImage(new Image(
                            photo.toURI() + "?t=" + System.currentTimeMillis()));
                }
                photoButton.setDisable(false);
                photoButton.setText("🔄  Retake Photo");
                photoButton.setStyle(
                        "-fx-background-color: #f39c12; -fx-text-fill: white; " +
                                "-fx-font-weight: 700; -fx-font-size: 14; -fx-padding: 12 25; " +
                                "-fx-background-radius: 8; -fx-cursor: hand;");
            });

        }, "prisoner-training-thread").start();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  💾 SAVE / UPDATE PROFILE  (mirrors updateGuard)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void savePrisoner() {
        if (isNewPrisoner) {
            if (!validateInputs()) return;
            try {
                Prisoner p = new Prisoner();
                // This method MUST capture the DatePicker value (see below)
                mapFormToPrisoner(p);

                int id = dao.saveAndReturnId(p);
                if (id <= 0) {
                    showError("Database Error", "Could not save the prisoner.");
                    return;
                }
                p.setPrisonerId(id);
                currentPrisoner = p;
                isNewPrisoner = false;

               // updateUIForExistingPrisoner(); // Helper to clean up the code

                if (onSaveCallback != null) onSaveCallback.run();

                new Alert(Alert.AlertType.INFORMATION, "Prisoner registered! ID: PRN-" + String.format("%06d", id)).show();

            } catch (Exception e) {
                showError("Error", e.getMessage());
                e.printStackTrace();
            }

        } else {
            if (currentPrisoner == null) return;
            if (!validateInputs()) return;
            try {
                // Update the object with current UI values (including the date)
                mapFormToPrisoner(currentPrisoner);

                // Save to Database
                dao.update(currentPrisoner);

                // AUTO-REFRESH UI Badge and Danger Level
                applyStatusBadge(currentPrisoner.getStatus());
                applyDangerBadge(currentPrisoner.getDangerLevel());

                // Trigger main table refresh
                if (onSaveCallback != null) onSaveCallback.run();

                new Alert(Alert.AlertType.INFORMATION, "Prisoner Profile Updated & Database Synced!").show();

            } catch (Exception e) {
                showError("Error", e.getMessage());
                e.printStackTrace();
            }
        }
    }
    // ══════════════════════════════════════════════════════════════════════
    //  FORM → MODEL  (full + partial, mirrors mapFormToGuard / buildPartialGuard)
    // ══════════════════════════════════════════════════════════════════════
    private void mapFormToPrisoner(Prisoner p) {
        p.setName(nameField.getText().trim());
        try { p.setAge(Integer.parseInt(ageField.getText().trim())); }
        catch (Exception ignored) {}
        p.setGender(genderBox.getValue());
        p.setNationality(nationalityField.getText());
        p.setAadharNumber(aadharField.getText());
        p.setBloodType(bloodTypeBox.getValue());
        p.setHeight(heightField.getText());
        p.setWeight(weightField.getText());
        p.setIdentificationMarks(marksField.getText());
        p.setHomeAddress(addressField.getText());
        p.setEmergencyContact(emergencyContactField.getText());
        p.setEmergencyPhone(emergencyPhoneField.getText());
        p.setLawyerName(lawyerNameField.getText());
        p.setLawyerPhone(lawyerPhoneField.getText());
        p.setCrime(crimeBox.getValue());
        p.setCellNo(cellBox.getValue());
        p.setDangerLevel(dangerBox.getValue() != null ? dangerBox.getValue() : "LOW");
        p.setBehaviorRating(behaviorBox.getValue() != null ? behaviorBox.getValue() : "GOOD");

        LocalDate start = startDatePicker.getValue();
        p.setSentenceStartDate(start);

        int years = yearSpinner.getValue();
        int months = monthSpinner.getValue();
        p.setSentenceYears(years);

        if (start != null) {
            // Automatically calculate release date
            LocalDate release = start.plusYears(years).plusMonths(months);
            p.setReleaseDate(release);

            // Auto-update status based on date
            p.setStatus(release.isAfter(LocalDate.now()) ? "IN_CUSTODY" : "RELEASED");
        }

        p.setDescription(descriptionArea.getText());
        p.setIncidentNotes(incidentArea.getText());
        p.setVisitorLog(visitorLogArea.getText());
    }

    private Prisoner buildPartialPrisoner() {
        Prisoner p = new Prisoner();
        p.setName(nameField.getText().trim());
        p.setCrime(crimeBox.getValue());
        p.setCellNo(cellBox.getValue());
        p.setDangerLevel(dangerBox.getValue() != null ? dangerBox.getValue() : "LOW");
        p.setBehaviorRating(behaviorBox.getValue() != null ? behaviorBox.getValue() : "GOOD");
        p.setStatus("IN_CUSTODY");
        LocalDate start = startDatePicker.getValue() != null
                ? startDatePicker.getValue() : LocalDate.now();
        p.setSentenceStartDate(start);
        int years  = yearSpinner.getValue();
        int months = monthSpinner.getValue();
        p.setSentenceYears(years);
        p.setReleaseDate(start.plusYears(years).plusMonths(months));
        try { p.setAge(Integer.parseInt(ageField.getText().trim())); }
        catch (Exception ignored) {}
        p.setGender(genderBox.getValue());
        p.setNationality(nationalityField.getText());
        p.setAadharNumber(aadharField.getText());
        p.setDescription(descriptionArea.getText());
        p.setIncidentNotes(incidentArea.getText());
        p.setVisitorLog(visitorLogArea.getText());
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════════════════
    private boolean validateInputs() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Full name is required."); return false;
        }
        // Date Validation
        if (startDatePicker.getValue() == null) {
            showError("Validation Error", "Sentence Start Date is required.");
            return false;
        }

        if (yearSpinner.getValue() == 0 && monthSpinner.getValue() == 0) {
            showError("Validation Error", "Sentence duration cannot be zero.");
            return false;
        }
        if (crimeBox.getValue() == null) {
            showError("Validation Error", "Please select or enter a crime."); return false;
        }
        if (cellBox.getValue() == null) {
            showError("Validation Error", "Please select a cell number."); return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  🖨️ PRINT PRISONER PROFILE  (crimson theme — layout unchanged)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handlePrintProfile() {
        if (currentPrisoner == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("PRISONER_RECORD_PRN-" +
                String.format("%06d", currentPrisoner.getPrisonerId()) + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(nameField.getScene().getWindow());
        if (file == null) return;

        try {
            Color crimson  = new Color(127, 29, 29);
            Color darkCrim = new Color(69, 10, 10);
            Color lightCrim= new Color(255, 240, 240);
            Color midCrim  = new Color(250, 220, 220);

            Document document = new Document(PageSize.A4, 40, 40, 55, 60);
            PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(file));

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2.5f); cb.setColorStroke(crimson);
                        cb.rectangle(28, 28,
                                doc.getPageSize().getWidth() - 56,
                                doc.getPageSize().getHeight() - 56);
                        cb.stroke();
                        cb.setLineWidth(0.5f);
                        cb.setColorStroke(new Color(200, 150, 150));
                        cb.rectangle(33, 33,
                                doc.getPageSize().getWidth() - 66,
                                doc.getPageSize().getHeight() - 66);
                        cb.stroke();
                        cb.setLineWidth(1f);
                        cb.moveTo(38, 50);
                        cb.lineTo(doc.getPageSize().getWidth() - 38, 50);
                        cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA, 7,
                                new Color(140, 80, 80));
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CLASSIFIED — RESTRICTED ACCESS", ff), 40, 36, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Department of Corrections & Prison Security", ff),
                                doc.getPageSize().getWidth() / 2, 36, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber(), ff),
                                doc.getPageSize().getWidth() - 40, 36, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // ── Header ───────────────────────────────────────────────────
            PdfPTable ht = new PdfPTable(new float[]{1, 4, 1});
            ht.setWidthPercentage(100); ht.setSpacingAfter(18f);
            ht.addCell(logoCell());
            PdfPCell cc = new PdfPCell(); cc.setBorder(PdfPCell.NO_BORDER);
            cc.setHorizontalAlignment(Element.ALIGN_CENTER); cc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph dp = new Paragraph("DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, crimson));
            dp.setAlignment(Element.ALIGN_CENTER); dp.setSpacingAfter(6f); cc.addElement(dp);
            Paragraph tp = new Paragraph("OFFICIAL INMATE RECORD",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, crimson));
            tp.setAlignment(Element.ALIGN_CENTER); tp.setSpacingAfter(4f); cc.addElement(tp);
            Paragraph sub = new Paragraph("Security Classification: RESTRICTED",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(120, 50, 50)));
            sub.setAlignment(Element.ALIGN_CENTER); cc.addElement(sub);
            ht.addCell(cc); ht.addCell(logoCell()); document.add(ht);

            // ── Meta bar ─────────────────────────────────────────────────
            PdfPTable mt = new PdfPTable(3); mt.setWidthPercentage(100); mt.setSpacingAfter(14f);
            Font mf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            addMetaCell(mt, "PRISONER ID: PRN-" + String.format("%06d", currentPrisoner.getPrisonerId()), mf, crimson);
            addMetaCell(mt, "STATUS: " + statusBadge.getText() + "  |  RISK: " + dangerBadge.getText(), mf, darkCrim);
            addMetaCell(mt, "ISSUED: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), mf, crimson);
            document.add(mt);

            // ── Identity + Photo row ──────────────────────────────────────
            Font sec  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, crimson);
            Font lbl  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 40, 40));
            Font val  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

            PdfPTable mainT = new PdfPTable(new float[]{3f, 2.8f});
            mainT.setWidthPercentage(100); mainT.setSpacingAfter(14f); mainT.setKeepTogether(true);

            PdfPCell dataCell = new PdfPCell();
            dataCell.setBorder(PdfPCell.BOX); dataCell.setBorderColor(crimson); dataCell.setBorderWidth(1.8f);
            dataCell.setPadding(14); dataCell.setBackgroundColor(lightCrim); dataCell.setVerticalAlignment(Element.ALIGN_TOP);
            Paragraph secH = new Paragraph("INMATE IDENTIFICATION", sec); secH.setSpacingAfter(10f); dataCell.addElement(secH);
            addFieldToCell(dataCell, "PRISONER NUMBER", "PRN-" + String.format("%06d", currentPrisoner.getPrisonerId()), lbl, val);
            addFieldToCell(dataCell, "FULL LEGAL NAME", nameField.getText().toUpperCase(), lbl, val);
            addFieldToCell(dataCell, "AGE / GENDER", ageField.getText() + " yrs  |  " + nvl(genderBox.getValue()), lbl, val);
            addFieldToCell(dataCell, "NATIONALITY", nvl(nationalityField.getText()), lbl, val);
            addFieldToCell(dataCell, "NATIONAL ID / AADHAR", nvl(aadharField.getText()), lbl, val);
            addFieldToCell(dataCell, "BLOOD TYPE", nvl(bloodTypeBox.getValue()), lbl, val);
            addFieldToCell(dataCell, "HEIGHT / WEIGHT", nvl(heightField.getText()) + " / " + nvl(weightField.getText()), lbl, val);
            dataCell.addElement(new Paragraph("IDENTIFICATION MARKS", lbl));
            Paragraph imv = new Paragraph(nvl(marksField.getText()), val); imv.setSpacingAfter(0f); dataCell.addElement(imv);
            mainT.addCell(dataCell);

            PdfPCell photoCell = new PdfPCell();
            photoCell.setBorder(PdfPCell.BOX); photoCell.setBorderColor(crimson); photoCell.setBorderWidth(1.8f);
            photoCell.setPadding(12); photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoCell.setVerticalAlignment(Element.ALIGN_TOP); photoCell.setBackgroundColor(Color.WHITE);
            Paragraph ph = new Paragraph("OFFICIAL PHOTOGRAPH",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, crimson));
            ph.setAlignment(Element.ALIGN_CENTER); ph.setSpacingAfter(8f); photoCell.addElement(ph);
            File imgFile = new File("python-face/photos/prisoners/" + currentPrisoner.getPrisonerId() + ".jpg");
            if (imgFile.exists()) {
                com.lowagie.text.Image pi =
                        com.lowagie.text.Image.getInstance(imgFile.getAbsolutePath());
                pi.scaleToFit(145, 165); pi.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(pi);
            } else {
                Paragraph np = new Paragraph("\n\n\nNO PHOTOGRAPH\nON FILE\n\n\n",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY));
                np.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(np);
            }
            Paragraph pd = new Paragraph("\nPhoto Date: " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                    FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY));
            pd.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(pd);
            String dl = dangerBadge.getText();
            Color dlC = switch (dl) {
                case "MAXIMUM" -> new Color(127, 29, 29);
                case "HIGH"    -> new Color(185, 28, 28);
                case "MEDIUM"  -> new Color(180, 100, 0);
                default        -> new Color(20, 100, 20);
            };
            Paragraph dlP = new Paragraph("\nDANGER: " + dl,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, dlC));
            dlP.setAlignment(Element.ALIGN_CENTER); photoCell.addElement(dlP);
            mainT.addCell(photoCell); document.add(mainT);

            DateTimeFormatter dateStyle = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

            // ── Criminal record ───────────────────────────────────────────
            addSectionWithRows(document, sec, "CRIMINAL RECORD & SENTENCE",
                    new String[][]{
                            {"Crime",           nvl(crimeBox.getValue())},
                            {"Cell Number",     nvl(cellBox.getValue())},
                            {"Danger Level",    dangerBadge.getText()},
                            {"Behavior Rating", nvl(behaviorBox.getValue())},
                            {"Sentence",        yearSpinner.getValue() + " years, " + monthSpinner.getValue() + " months"},
                            {"Start Date",      startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : ""},
                            {"Release Date",    currentPrisoner.getReleaseDate() != null ? currentPrisoner.getReleaseDate().toString() : ""},
                            {"Current Status",  statusBadge.getText()}
                    }, crimson, midCrim);

            // ── Contacts ─────────────────────────────────────────────────
            addSectionWithRows(document, sec, "CONTACT & LEGAL INFORMATION",
                    new String[][]{
                            {"Home Address",      nvl(addressField.getText())},
                            {"Emergency Contact", nvl(emergencyContactField.getText())},
                            {"Emergency Phone",   nvl(emergencyPhoneField.getText())},
                            {"Legal Counsel",     nvl(lawyerNameField.getText())},
                            {"Lawyer Phone",      nvl(lawyerPhoneField.getText())}
                    }, crimson, midCrim);

            // ── Notes ─────────────────────────────────────────────────────
            if (!descriptionArea.getText().trim().isEmpty())
                addNoteSection(document, sec, "CASE NOTES / GENERAL REMARKS",
                        descriptionArea.getText(), crimson);

            // ── Signature ─────────────────────────────────────────────────
            addSignatureBlock(document, crimson);

            // ── Verification strip ────────────────────────────────────────
            addVerificationStrip(document, crimson);

            document.close();
            new Alert(Alert.AlertType.INFORMATION,
                    "Prisoner Profile Exported Successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  📋 BEHAVIORAL LOG PDF  (crimson theme — layout unchanged)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleBehavioralLog() {
        if (currentPrisoner == null) return;
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("BEHAVIORAL_LOG_PRN-" +
                String.format("%06d", currentPrisoner.getPrisonerId()) + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(nameField.getScene().getWindow());
        if (file == null) return;

        try {
            Color crimson  = new Color(127, 29, 29);
            Color darkCrim = new Color(69, 10, 10);
            Color lightCrim= new Color(255, 240, 240);
            Color midCrim  = new Color(250, 220, 220);

            Document document = new Document(PageSize.A4, 40, 40, 55, 60);
            PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(file));

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document doc) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        cb.setLineWidth(2.5f); cb.setColorStroke(crimson);
                        cb.rectangle(28, 28,
                                doc.getPageSize().getWidth() - 56,
                                doc.getPageSize().getHeight() - 56); cb.stroke();
                        cb.setLineWidth(0.5f); cb.setColorStroke(new Color(200, 150, 150));
                        cb.rectangle(33, 33,
                                doc.getPageSize().getWidth() - 66,
                                doc.getPageSize().getHeight() - 66); cb.stroke();
                        cb.setLineWidth(1f); cb.moveTo(38, 50);
                        cb.lineTo(doc.getPageSize().getWidth() - 38, 50); cb.stroke();
                        Font ff = FontFactory.getFont(FontFactory.HELVETICA, 7,
                                new Color(140, 80, 80));
                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                                new Phrase("CONFIDENTIAL BEHAVIORAL RECORD", ff), 40, 36, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Inmate Monitoring & Disciplinary Log", ff),
                                doc.getPageSize().getWidth() / 2, 36, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                                new Phrase("Page " + w.getPageNumber(), ff),
                                doc.getPageSize().getWidth() - 40, 36, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            // ── Header ───────────────────────────────────────────────────
            Font sec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, crimson);
            PdfPTable ht = new PdfPTable(new float[]{1, 4, 1});
            ht.setWidthPercentage(100); ht.setSpacingAfter(18f);
            ht.addCell(logoCell());
            PdfPCell cc = new PdfPCell(); cc.setBorder(PdfPCell.NO_BORDER);
            cc.setHorizontalAlignment(Element.ALIGN_CENTER); cc.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph dp = new Paragraph("DEPARTMENT OF CORRECTIONS & PRISON SECURITY",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, crimson));
            dp.setAlignment(Element.ALIGN_CENTER); dp.setSpacingAfter(6f); cc.addElement(dp);
            Paragraph tp = new Paragraph("BEHAVIORAL & ACTIVITY MONITORING LOG",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, crimson));
            tp.setAlignment(Element.ALIGN_CENTER); tp.setSpacingAfter(4f); cc.addElement(tp);
            Paragraph sub = new Paragraph(
                    "Incident Reports  ·  Disciplinary Actions  ·  Visitation & Communication Logs",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(120, 50, 50)));
            sub.setAlignment(Element.ALIGN_CENTER); cc.addElement(sub);
            ht.addCell(cc); ht.addCell(logoCell()); document.add(ht);

            // ── Meta bar ─────────────────────────────────────────────────
            PdfPTable mt = new PdfPTable(3); mt.setWidthPercentage(100); mt.setSpacingAfter(14f);
            Font mf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            addMetaCell(mt, "PRISONER: " + nameField.getText().toUpperCase() +
                    "  (PRN-" + String.format("%06d", currentPrisoner.getPrisonerId()) + ")", mf, crimson);
            addMetaCell(mt, "BEHAVIOR RATING: " + nvl(behaviorBox.getValue()) +
                    "  |  CELL: " + nvl(cellBox.getValue()), mf, darkCrim);
            addMetaCell(mt, "REPORT DATE: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), mf, crimson);
            document.add(mt);

            // ── Inmate summary ────────────────────────────────────────────
            addSectionWithRows(document, sec, "INMATE SUMMARY",
                    new String[][]{
                            {"Name",            nameField.getText()},
                            {"Crime",           nvl(crimeBox.getValue())},
                            {"Danger Level",    dangerBadge.getText()},
                            {"Current Status",  statusBadge.getText()},
                            {"Release Date",    currentPrisoner.getReleaseDate() != null
                                    ? currentPrisoner.getReleaseDate().toString() : ""},
                            {"Behavior Rating", nvl(behaviorBox.getValue())}
                    }, crimson, midCrim);

            // ── Incident / Disciplinary log ───────────────────────────────
            addBigTextSection(document, sec,
                    "INCIDENT REPORTS, DISCIPLINARY ACTIONS & STAFF EVALUATIONS",
                    incidentArea.getText(), lightCrim,
                    "Date | Type | Description | Action Taken | Reporting Officer");

            // ── Activity / Visitor log ────────────────────────────────────
            addBigTextSection(document, sec,
                    "ACTIVITY MONITORING — VISITATION, TELEPHONE CALLS & DIGITAL USAGE",
                    visitorLogArea.getText(), lightCrim,
                    "Date | Type (Visit/Call/Tablet) | Contact Person | Duration | Notes");

            // ── Periodic assessment checklist ─────────────────────────────
            document.add(new Paragraph("\n"));
            Paragraph chH = new Paragraph("PERIODIC ASSESSMENT CHECKLIST", sec);
            chH.setSpacingAfter(8f); document.add(chH);

            PdfPTable cl = new PdfPTable(new float[]{3f, 1f, 1f, 1f});
            cl.setWidthPercentage(100); cl.setSpacingAfter(15f);
            Font chLbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font chVal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            for (String h : new String[]{"Assessment Item", "Satisfactory", "Unsatisfactory", "N/A"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, chLbl));
                hc.setBackgroundColor(new Color(185, 28, 28)); hc.setPadding(7);
                hc.setHorizontalAlignment(Element.ALIGN_CENTER); cl.addCell(hc);
            }
            String[][] checks = {
                    {"Follows cell rules and regulations"},
                    {"Participates in rehabilitation programs"},
                    {"Maintains personal hygiene"},
                    {"Shows respectful conduct toward staff"},
                    {"No unauthorised communications detected"},
                    {"Visits conducted within permitted guidelines"},
                    {"Digital / tablet usage within policy"},
                    {"No contraband found during recent inspections"}
            };
            boolean alt = false;
            for (String[] row : checks) {
                Color rb = alt ? lightCrim : Color.WHITE; alt = !alt;
                PdfPCell rc = new PdfPCell(new Phrase(row[0], chVal));
                rc.setBackgroundColor(rb); rc.setPadding(8); cl.addCell(rc);
                for (int i = 0; i < 3; i++) {
                    PdfPCell box = new PdfPCell(new Phrase("  ☐  ", chVal));
                    box.setBackgroundColor(rb); box.setPadding(8);
                    box.setHorizontalAlignment(Element.ALIGN_CENTER); cl.addCell(box);
                }
            }
            document.add(cl);

            addSignatureBlock(document, crimson);
            addVerificationStrip(document, crimson);

            document.close();
            new Alert(Alert.AlertType.INFORMATION,
                    "Behavioral Log Exported Successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export Failed: " + e.getMessage()).show();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUTTON STYLING HELPERS  (mirrors GuardProfileController exactly)
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
                    "-fx-background-color: #b91c1c; -fx-text-fill: white; -fx-font-weight: 700; " +
                            "-fx-font-size: 14; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private void styleRegisterBtn() {
        saveBtn.setText("✔  REGISTER PRISONER");
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; " +
                "-fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10;");
    }

    private void styleSaveBtn() {
        saveBtn.setText("SAVE PROFILE");
        saveBtn.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: white; " +
                "-fx-font-weight: 800; -fx-padding: 15; -fx-background-radius: 10;");
    }

    private void hidePrintButtons() {
        printProfileBtn.setVisible(false);  printProfileBtn.setManaged(false);
        behavioralLogBtn.setVisible(false); behavioralLogBtn.setManaged(false);
    }

    private void showPrintButtons() {
        printProfileBtn.setVisible(true);  printProfileBtn.setManaged(true);
        behavioralLogBtn.setVisible(true); behavioralLogBtn.setManaged(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SHARED PDF HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private PdfPCell logoCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            com.lowagie.text.Image l =
                    com.lowagie.text.Image.getInstance("src/main/resources/images/logo.jpeg");
            l.scaleToFit(58, 58); c.addElement(l);
        } catch (Exception ignored) {}
        return c;
    }

    private void addMetaCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg); c.setBorder(PdfPCell.NO_BORDER);
        c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER); t.addCell(c);
    }

    private void addFieldToCell(PdfPCell cell, String label, String value,
                                Font lf, Font vf) {
        Paragraph lp = new Paragraph(label, lf); lp.setSpacingAfter(2f); cell.addElement(lp);
        Paragraph vp = new Paragraph(value != null && !value.isEmpty() ? value : "—", vf);
        vp.setSpacingAfter(10f); cell.addElement(vp);
    }

    private void addSectionWithRows(Document doc, Font sec, String title,
                                    String[][] rows, Color crimson, Color midCrim) throws Exception {
        PdfPTable s = new PdfPTable(1); s.setWidthPercentage(100);
        s.setSpacingBefore(8f); s.setSpacingAfter(14f); s.setKeepTogether(true);
        PdfPCell h = new PdfPCell(new Paragraph(title, sec));
        h.setBorder(PdfPCell.NO_BORDER); h.setBackgroundColor(new Color(255, 230, 230));
        h.setPadding(8); h.setPaddingLeft(10); s.addCell(h);
        PdfPTable inner = new PdfPTable(2); inner.setWidthPercentage(100);
        inner.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        Font lf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, crimson);
        Font vf = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        for (String[] row : rows) {
            PdfPCell c1 = new PdfPCell(new Paragraph(row[0], lf));
            c1.setBackgroundColor(midCrim); c1.setPadding(9); c1.setPaddingLeft(11);
            c1.setBorderColor(new Color(210, 160, 160)); c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            PdfPCell c2 = new PdfPCell(new Paragraph(row[1] != null && !row[1].isEmpty() ? row[1] : "—", vf));
            c2.setPadding(9); c2.setPaddingLeft(11); c2.setBorderColor(new Color(210, 160, 160));
            c2.setVerticalAlignment(Element.ALIGN_MIDDLE); c2.setBackgroundColor(Color.WHITE);
            inner.addCell(c1); inner.addCell(c2);
        }
        PdfPCell d = new PdfPCell(inner); d.setBorder(PdfPCell.NO_BORDER); d.setPadding(0);
        s.addCell(d); doc.add(s);
    }

    private void addNoteSection(Document doc, Font sec, String title,
                                String content, Color crimson) throws Exception {
        PdfPTable ns = new PdfPTable(1); ns.setWidthPercentage(100);
        ns.setSpacingBefore(8f); ns.setSpacingAfter(16f); ns.setKeepTogether(true);
        PdfPCell nh = new PdfPCell(new Paragraph(title, sec));
        nh.setBorder(PdfPCell.NO_BORDER); nh.setBackgroundColor(new Color(255, 230, 230));
        nh.setPadding(8); nh.setPaddingLeft(10); ns.addCell(nh);
        PdfPCell nc = new PdfPCell(new Paragraph(content,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK)));
        nc.setBorder(PdfPCell.BOX); nc.setBorderColor(new Color(200, 150, 150));
        nc.setPadding(12); nc.setMinimumHeight(65); nc.setBackgroundColor(Color.WHITE);
        ns.addCell(nc); doc.add(ns);
    }

    private void addBigTextSection(Document doc, Font sec, String title,
                                   String content, Color lightCrim,
                                   String columnHint) throws Exception {
        PdfPTable s = new PdfPTable(1); s.setWidthPercentage(100);
        s.setSpacingBefore(10f); s.setSpacingAfter(14f);
        PdfPCell hd = new PdfPCell(new Paragraph(title, sec));
        hd.setBorder(PdfPCell.NO_BORDER); hd.setBackgroundColor(new Color(255, 230, 230));
        hd.setPadding(8); hd.setPaddingLeft(10); s.addCell(hd);
        PdfPCell hint = new PdfPCell(new Paragraph("Format: " + columnHint,
                FontFactory.getFont(FontFactory.HELVETICA, Font.ITALIC, 8, new Color(140, 80, 80))));
        hint.setBorder(PdfPCell.NO_BORDER); hint.setBackgroundColor(new Color(255, 242, 242));
        hint.setPadding(5); hint.setPaddingLeft(10); s.addCell(hint);
        String body = content != null && !content.trim().isEmpty()
                ? content : "\n\n\n\n\n\n\n\n\n";
        PdfPCell bc = new PdfPCell(new Paragraph(body,
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK)));
        bc.setBorder(PdfPCell.BOX); bc.setBorderColor(new Color(200, 150, 150));
        bc.setPadding(12); bc.setMinimumHeight(130); bc.setBackgroundColor(Color.WHITE);
        s.addCell(bc); doc.add(s);
    }

    private void addSignatureBlock(Document doc, Color crimson) throws Exception {
        PdfPTable st = new PdfPTable(2); st.setWidthPercentage(100); st.setSpacingBefore(28f);
        Font sf = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        PdfPCell d = new PdfPCell(); d.setBorder(PdfPCell.NO_BORDER);
        d.addElement(new Paragraph("Date: ___________________", sf)); st.addCell(d);
        PdfPCell s2 = new PdfPCell(); s2.setBorder(PdfPCell.NO_BORDER);
        s2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph sl = new Paragraph("_____________________________", sf); sl.setAlignment(Element.ALIGN_RIGHT); s2.addElement(sl);
        Paragraph slb = new Paragraph("Authorised Officer Signature",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        slb.setAlignment(Element.ALIGN_RIGHT); s2.addElement(slb);
        Paragraph sti = new Paragraph("Prison Superintendent / Warden",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK));
        sti.setAlignment(Element.ALIGN_RIGHT); s2.addElement(sti);
        st.addCell(s2); doc.add(st);
    }

    private void addVerificationStrip(Document doc, Color crimson) throws Exception {
        doc.add(new Paragraph("\n"));
        PdfPTable vt = new PdfPTable(1); vt.setWidthPercentage(100);
        PdfPCell vc = new PdfPCell(new Paragraph(
                "CLASSIFIED DOCUMENT. Unauthorised access, reproduction or disclosure is a criminal offence under applicable law. " +
                        "Document ID: DOC-" + System.currentTimeMillis() % 1000000 + " | Generated: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")),
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(120, 50, 50))));
        vc.setBorder(PdfPCell.TOP); vc.setBorderColor(new Color(180, 100, 100));
        vc.setHorizontalAlignment(Element.ALIGN_CENTER); vc.setPadding(9);
        vc.setBackgroundColor(new Color(255, 245, 245)); vt.addCell(vc); doc.add(vt);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MISC
    // ══════════════════════════════════════════════════════════════════════
    private void applyStatusBadge(String status) {
        if (statusBadge == null) return;
        statusBadge.setText(status);
        String color = "IN_CUSTODY".equals(status) || "IN CUSTODY".equals(status)
                ? "#16a34a" : "#dc2626";
        statusBadge.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                "-fx-padding:10 20; -fx-background-radius:20; -fx-font-weight:bold;");
    }

    private void applyDangerBadge(String level) {
        if (dangerBadge == null || level == null) return;
        dangerBadge.setText(level);
        String color = switch (level) {
            case "MAXIMUM" -> "#450a0a";
            case "HIGH"    -> "#7f1d1d";
            case "MEDIUM"  -> "#92400e";
            default        -> "#14532d";
        };
        dangerBadge.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; " +
                "-fx-padding:9 18; -fx-background-radius:20; -fx-font-weight:bold;");
    }

    private void loadPrisonerImage(int id) {
        try {
            File f = new File("python-face/photos/prisoners/" + id + ".jpg");
            if (f.exists()) prisonerImage.setImage(new Image(f.toURI().toString()));
        } catch (Exception ignored) {}
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

    private String nvl(String s) {
        return s != null && !s.isEmpty() ? s : "";
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }
}