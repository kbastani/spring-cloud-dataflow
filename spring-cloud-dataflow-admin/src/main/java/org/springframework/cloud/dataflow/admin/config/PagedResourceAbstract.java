package org.springframework.cloud.dataflow.admin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.util.List;

/**
 * Created by kbastani on 11/12/15.
 */
public class PagedResourceAbstract<T> extends PageImpl<T> {

    private PagedResources.PageMetadata metadata;

    @JsonProperty("page")
    public PagedResources.PageMetadata getMetadata() {
        return metadata;
    }

    public PagedResourceAbstract(List<T> content) {
        this(content, null, null == content ? 0 : content.size());
    }

    /**
     * Constructor of {@code PageImpl}.
     *
     * @param content  the content of this page, must not be {@literal null}.
     * @param pageable the paging information, can be {@literal null}.
     * @param total    the total amount of items available
     */
    public PagedResourceAbstract(List<T> content, Pageable pageable, long total) {
        super(content.subList(pageable.getOffset(), pageable.getPageSize() + pageable.getOffset() > (int) total ? (int) Math.max(total - 1, 0) : pageable.getPageSize()), pageable, total);
        this.metadata = new PagedResources.PageMetadata(getSize(), getNumber(), getTotalElements());
    }
}
