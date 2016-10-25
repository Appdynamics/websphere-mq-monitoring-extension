package com.appdynamics.extensions.webspheremq.config;


import java.util.HashMap;
import java.util.Map;

public class MetricOverride {
    private String metricKey;
    protected String prefix;
    protected String postfix;
    protected String aggregator;
    protected String timeRollup;
    protected String clusterRollup;
    protected double multiplier;
    protected boolean disabled;
    protected String alias;
    protected Map<String,String> otherProps = new HashMap<String,String>();


    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    public String getAggregator() {
        return aggregator;
    }

    public void setAggregator(String aggregator) {
        this.aggregator = aggregator;
    }

    public String getTimeRollup() {
        return timeRollup;
    }

    public void setTimeRollup(String timeRollup) {
        this.timeRollup = timeRollup;
    }

    public String getClusterRollup() {
        return clusterRollup;
    }

    public void setClusterRollup(String clusterRollup) {
        this.clusterRollup = clusterRollup;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<String, String> getOtherProps() {
        return otherProps;
    }


}
