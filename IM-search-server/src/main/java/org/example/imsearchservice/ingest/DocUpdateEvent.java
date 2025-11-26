package org.example.imsearchservice.ingest;

import java.util.List;

public class DocUpdateEvent {

    private Long docId;
    private String title;
    private String contentText;
    private String fileUrl;
    private List<Long> allowUsers;
    private List<Long> allowDepts;
    private Boolean isPublic;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public List<Long> getAllowUsers() {
        return allowUsers;
    }

    public void setAllowUsers(List<Long> allowUsers) {
        this.allowUsers = allowUsers;
    }

    public List<Long> getAllowDepts() {
        return allowDepts;
    }

    public void setAllowDepts(List<Long> allowDepts) {
        this.allowDepts = allowDepts;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }
}
