package ar.edu.utn.frba.ddsi.front.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> {
    private List<T> content = new ArrayList<>();
    private int totalPages;
    private long totalElements;

    public PageResponse() {
    }

    public PageResponse(List<T> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}
