package com.example.temp.model;

public class NotesModel {
    private final String id;
    private final String name;
    private final String subject;
    private final String year;        // can be null/empty
    private final String uploadedBy;
    private final String filePath;    // e.g. "ai/AI_UNIT_1.pdf.pdf"

    public NotesModel(String id, String name, String subject, String year, String uploadedBy, String filePath) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.year = year;
        this.uploadedBy = uploadedBy;
        this.filePath = filePath;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getYear() { return year; }
    public String getUploadedBy() { return uploadedBy; }
    public String getFilePath() { return filePath; }
}
