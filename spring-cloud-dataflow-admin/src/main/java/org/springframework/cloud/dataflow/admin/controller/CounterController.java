/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.cloud.dataflow.admin.config.PagedResourceAbstract;
import org.springframework.cloud.dataflow.rest.resource.CounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows interaction with Counters.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CounterController {

    public static final String COUNTER_PREFIX = "counter.";

    @Autowired
    private MetricRepository metricRepository;

    private final ResourceAssembler<Metric<Double>, CounterResource> counterResourceAssembler =
            new DeepCounterResourceAssembler();

    protected final ResourceAssembler<Metric<Double>, ? extends MetricResource> shallowResourceAssembler =
            new ShallowMetricResourceAssembler();

    /**
     * List Counters that match the given criteria.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public Page<Metric<Double>> list(
            Pageable pageable,
            PagedResourcesAssembler<Metric<Double>> pagedAssembler,
            @RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
        /* Page */
        Iterable<Metric<?>> metrics = metricRepository.findAll(/* pageable */);
        List<Metric<Double>> content = filterCounters(metrics);
        content.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return new PagedResourceAbstract<>(content, pageable, content.size());
    }

    /**
     * Retrieve information about a specific counter.
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    public CounterResource display(@PathVariable("name") String name) {
        Metric<Double> c = findCounter(name);
        return counterResourceAssembler.toResource(c);
    }

    /**
     * Delete (reset) a specific counter.
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    protected void delete(@PathVariable("name") String name) {
        Metric<Double> c = findCounter(name);
        metricRepository.reset(c.getName());
    }

    /**
     * Find a given counter, taking care of name conversion between the Spring Boot domain and our domain.
     *
     * @throws MetricsMvcEndpoint.NoSuchMetricException if the counter does not exist
     */
    private Metric<Double> findCounter(@PathVariable("name") String name) {
        @SuppressWarnings("unchecked")
        Metric<Double> c = (Metric<Double>) metricRepository.findOne(COUNTER_PREFIX + name);
        if (c == null) {
            throw new MetricsMvcEndpoint.NoSuchMetricException(name);
        }
        return c;
    }


    /**
     * Filter the list of Boot metrics to only return those that are counters.
     */
    @SuppressWarnings("unchecked")
    private <T extends Number> List<Metric<T>> filterCounters(Iterable<Metric<?>> input) {
        List<Metric<T>> result = new ArrayList<>();
        for (Metric<?> metric : input) {
            if (metric.getName().startsWith(COUNTER_PREFIX)) {
                result.add(new Metric<T>(metric.getName().replace(COUNTER_PREFIX, ""), (T) metric.getValue()));
            }
        }
        return result;
    }

    /**
     * Base class for a ResourceAssembler that builds shallow resources for metrics
     * (exposing only their names, and hence their "self" rel).
     *
     * @author Eric Bottard
     */
    static class ShallowMetricResourceAssembler extends
            ResourceAssemblerSupport<Metric<Double>, MetricResource> {

        public ShallowMetricResourceAssembler() {
            super(CounterController.class, MetricResource.class);
        }

        @Override
        public MetricResource toResource(Metric<Double> entity) {
            return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
        }

        @Override
        protected MetricResource instantiateResource(Metric<Double> entity) {
            return new MetricResource(entity.getName().substring(COUNTER_PREFIX.length()));
        }

    }

    /**
     * Knows how to assemble {@link CounterResource}s out of counter {@link Metric}s.
     *
     * @author Eric Bottard
     */
    static class DeepCounterResourceAssembler extends
            ResourceAssemblerSupport<Metric<Double>, CounterResource> {

        public DeepCounterResourceAssembler() {
            super(CounterController.class, CounterResource.class);
        }

        @Override
        public CounterResource toResource(Metric<Double> entity) {
            return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
        }

        @Override
        protected CounterResource instantiateResource(Metric<Double> entity) {
            return new CounterResource(entity.getName().substring(COUNTER_PREFIX.length()), entity.getValue().longValue());
        }

    }

}
