package com.appdynamics.extensions.webspheremq.config;

import java.util.HashSet;
import java.util.Set;

public class ResourceFilters {

    private Set<String> include = new HashSet<String>();
    private Set<ExcludeFilters> exclude = new HashSet<ExcludeFilters>();

    public Set<String> getInclude() {
        return include;
    }

    public void setInclude(Set<String> include) {
        this.include = include;
    }

    public Set<ExcludeFilters> getExclude() {
        return exclude;
    }

    public void setExclude(Set<ExcludeFilters> exclude) {
        this.exclude = exclude;
    }
}
