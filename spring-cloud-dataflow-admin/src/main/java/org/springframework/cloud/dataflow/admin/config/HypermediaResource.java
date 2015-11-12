package org.springframework.cloud.dataflow.admin.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

public class HypermediaResource<T extends PagedResources.PageMetadata> extends ResourceSupport {
    private final T content;

    @JsonCreator
    public HypermediaResource(@JsonProperty("content") T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }
}
