/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSMetricFetcher;
import static com.appdynamics.extensions.tibco.collectors.AbstractMetricCollector.objectMapper;
import com.appdynamics.extensions.tibco.metrics.Metric;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.google.common.base.Strings;
import com.tibco.tibjms.admin.ConnectionInfo;
import com.tibco.tibjms.admin.DurableInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

/**
 * @author Anwar Fazaa
 */
public class ConnectionsMetricCollector extends AbstractMetricCollector {

    private static final Logger logger = Logger.getLogger(DurableMetricCollector.class);
    private final Phaser phaser;
    private List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;
    private Map<String, String> queueTopicMetricPrefixes;


    public ConnectionsMetricCollector(TibjmsAdmin conn, List<Pattern> includePatterns, boolean showSystem,
                                  boolean showTemp, Metrics metrics, String metricPrefix, Phaser phaser, List<com.appdynamics.extensions.metrics.Metric> collectedMetrics, Map<String, String> queueTopicMetricPrefixes) {
        super(conn, includePatterns, showSystem, showTemp, metrics, metricPrefix);
        this.phaser = phaser;
        this.phaser.register();
        this.collectedMetrics = collectedMetrics;
        this.queueTopicMetricPrefixes = queueTopicMetricPrefixes;
    }

    public void run() {

        if (logger.isDebugEnabled()) {
            logger.debug("Collecting connections info");
        }

        try {
            ConnectionInfo[] connections = conn.getConnections();

            if (connections == null) {
                logger.warn("Unable to get connections metrics");
            } else {
                

                        //for (ConnectionInfo connectionsInfo : connections) {
                        // This is disabled for now ( for testing purpose we will monitor all users names + connections )
                        //if (shouldMonitorDestination(connectionsInfo.getUserName(), includePatterns, showSystem, showTemp, TibcoEMSMetricFetcher.DestinationType.DURABLE, logger)) {
                        logger.info("Publishing connections metrics for Connection ");
                        List<com.appdynamics.extensions.metrics.Metric> userConnectionsMetrics = getConnectionsInfo(connections);
                        collectedMetrics.addAll(userConnectionsMetrics);
                    //}
                //}
                // try to get one entry ( we need to iterate through all connections and check the values ).
                
                
                
            }
        } catch (TibjmsAdminException e) {
            logger.error("Error while collecting durable metrics", e);
        } finally {
            logger.debug("DurableMetricCollector Phaser arrived");
            phaser.arriveAndDeregister();
        }
    }

    private List<com.appdynamics.extensions.metrics.Metric> getConnectionsInfo(ConnectionInfo[] connectionsInfo) {

        List<com.appdynamics.extensions.metrics.Metric> collectedMetrics;
        collectedMetrics = new ArrayList<>();
        Map<String,Integer> connectionsState = new HashMap<>();  
        
        String prefix = "Users|";
       
        
        for (ConnectionInfo  connection : connectionsInfo) {
            // we need to check if the connection is started + our result map already contais the key.
            // if username has no running connections metric entry will not be created
            if (connectionsState.containsKey(connection.getUserName())){
                if (connection.isStarted()) {
                    connectionsState.put(connection.getUserName() , connectionsState.get(connection.getUserName()) + 1);
                } else {
                    connectionsState.put(connection.getUserName() , connectionsState.get(connection.getUserName()) + 0);
                }
            } else {
                if (connection.isStarted()) {
                    connectionsState.put(connection.getUserName(), 1);
                } else {
                    connectionsState.put(connection.getUserName(), 0);
                }                
            }
        }
        
       
        
        //converting Map to metrics list
        Iterator it = connectionsState.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            
            StringBuilder sb = new StringBuilder(metricPrefix);
            sb.append("|");
            if (!Strings.isNullOrEmpty(prefix)) {
                sb.append("Users").append("|");
            }
            sb.append(pair.getKey().toString());

            String fullMetricPath = sb.toString();
            
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            com.appdynamics.extensions.metrics.Metric thisMetric = new com.appdynamics.extensions.metrics.Metric(pair.getKey().toString(), pair.getValue().toString(), fullMetricPath ,"OBSERVATION","CURRENT","COLLECTIVE" );
            collectedMetrics.add(thisMetric);
            it.remove(); // avoids a ConcurrentModificationException
        }
        
        
        return collectedMetrics;
    }
    
    
  
}