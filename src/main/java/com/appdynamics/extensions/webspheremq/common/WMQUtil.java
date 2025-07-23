/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.common;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.appdynamics.extensions.webspheremq.metricscollector.MetricsCollector;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WMQUtil {
    public static final Logger logger = ExtensionsLoggerFactory.getLogger(WMQUtil.class);

    private static Pattern METRIC_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static Pattern METRIC_TIME_PATTERN = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{2}$");

    /**
     * Returns master data structure,This map will contain only those metrics which are to be reported to controller.<br>
     * It contains metric type as key and a map of metric and WMQMetricOverride as value,<br>
     * entryset of internal map implicitly represents metrics to be reported.
     */
    public static Map<String, Map<String, WMQMetricOverride>> getMetricsToReportFromConfigYml(List<Map> mqMetrics) {
        Map<String, Map<String, WMQMetricOverride>> metricsMap = Maps.newHashMap();
        for (Map mqMetric : mqMetrics) {
            String metricType = (String) mqMetric.get("metricsType");
            List includeMetrics = (List) ((Map) mqMetric.get("metrics")).get("include");
            Map<String, WMQMetricOverride> metricToReport = Maps.newHashMap();
            if (includeMetrics != null) {
                metricToReport = gatherMetricNamesByApplyingIncludeFilter(includeMetrics);
            }
            metricsMap.put(metricType, metricToReport);
        }
        return metricsMap;
    }

    /**
     * Returns master data structure,This map will contain only those metrics which are to be excluded.<br>
     * It contains metric type as key and a list of metric keys to be excluded.
     */
    public static List<String> getMetricsToExcludeFromConfigYml(List<Map> mqMetrics, String metricType) {
        List<String> excludeMetrics = null;
        for (Map mqMetric : mqMetrics) {
            if (StringUtils.equalsIgnoreCase(metricType, (String) mqMetric.get("metricsType"))) {
                excludeMetrics = (List) ((Map) mqMetric.get("metrics")).get("exclude");
                break;
            }
        }
        return excludeMetrics;
    }

    public static boolean isMetricExcluded(String metricKey, List<String> excludedMetrics) {
        boolean excluded = false;
        metricKey = metricKey.trim();
        if (excludedMetrics == null || excludedMetrics.isEmpty()) {
            return false;
        }
        for (String excludedMetric : excludedMetrics) {
            if (StringUtils.equalsIgnoreCase(metricKey, excludedMetric)) {
                excluded = true;
                break;
            }
        }
        return excluded;
    }

    private static Map<String, WMQMetricOverride> gatherMetricNamesByApplyingIncludeFilter(List includeMetrics) {
        Map<String, WMQMetricOverride> overrideMap = Maps.newHashMap();
        for (Object inc : includeMetrics) {
            Map metric = (Map) inc;
            // Get the First Entry which is the metric
            Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
            String metricName = firstEntry.getKey().toString();
            Map<String, ?> metricPropsMap = (Map<String, ?>) metric.get(metricName);
            WMQMetricOverride override = new WMQMetricOverride();
            override.setIbmCommand((String) metricPropsMap.get("ibmCommand"));
            override.setIbmConstant((String) metricPropsMap.get("ibmConstant"));
            override.setMetricProperties(metricPropsMap);
            if (override.getConstantValue() == -1) {
                // Only add the metric which is valid, if constant value
                // resolutes to -1 then it is invalid.
                logger.warn("{} is not a valid valid metric, this metric will not be processed", override.getIbmConstant());
            } else {
                overrideMap.put(metricName, override);
            }
            logger.debug("Override Definition: " + override.toString());
        }
        return overrideMap;
    }

    public static String getQueueManagerNameFromConfig(QueueManager queueManager) {
        if (!Strings.isNullOrEmpty(queueManager.getDisplayName())) {
            return queueManager.getDisplayName();
        } else {
            return queueManager.getName();
        }
    }

    /**
     * converts input date value to integer value representing the number of days lapsed from current date
     * expects the DATE format as following:'yyyy-MM-dd'
     * @param metricValue
     * @return
     * @throws ParseException
     */
    public static int getDateDifferenceInDays(String metricValue) throws ParseException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currentDate = LocalDate.now();
        LocalDate inputDate = LocalDate.parse(metricValue.trim(), dtf);
        return (int) ChronoUnit.DAYS.between(inputDate, currentDate);
    }

    /**
     * converts input time value to integer value representing the number of hours lapsed from current time
     * expects the Time format as following:'hh.mm.ss'
     * @param metricValue
     * @return
     * @throws ParseException
     */
    public static int getTimeDifferenceInHours(String metricValue) throws ParseException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH.mm.ss");
        LocalTime dayStartTime = LocalTime.of(0,0,0);
        LocalTime inputTime = LocalTime.parse(metricValue.trim(), dtf);
        return  (int) ChronoUnit.SECONDS.between(dayStartTime, inputTime);
    }

    public static boolean isDateValue(String value) {
        return METRIC_DATE_PATTERN.matcher(value).matches();
    }

    public static boolean isTimeValue(String value) {
        return METRIC_TIME_PATTERN.matcher(value).matches();
    }
}
