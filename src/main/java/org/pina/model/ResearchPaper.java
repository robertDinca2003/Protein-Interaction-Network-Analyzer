package org.pina.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResearchPaper {
    private String pmid;
    private String title;
    private List<String> authors;
    private LocalDate publicationDate;

    public ResearchPaper(String pmid, String title, List<String> authors, LocalDate publicationDate) {
        this.pmid = pmid;
        this.title = title;
        this.authors = new ArrayList<>(authors);
        this.publicationDate = publicationDate;
    }

    public String getPmid() {
        return pmid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return List.copyOf(authors);
    }

    public void setAuthors(List<String> authors) {
        this.authors = new ArrayList<>(authors);
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }
}
