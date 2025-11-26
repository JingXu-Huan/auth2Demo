package org.example.imsearchservice.domain;

import java.time.Instant;
import java.util.List;

public class DocumentChunkDoc {

    private String chunkId;
    private Long parentDocId;
    private String title;
    private String contentChunk;
    private List<Float> vector;
    private List<Long> allowUsers;
    private List<Long> allowDepts;
    private Boolean isPublic;
    private Instant createdAt;

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public Long getParentDocId() {
        return parentDocId;
    }

    public void setParentDocId(Long parentDocId) {
        this.parentDocId = parentDocId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentChunk() {
        return contentChunk;
    }

    public void setContentChunk(String contentChunk) {
        this.contentChunk = contentChunk;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
