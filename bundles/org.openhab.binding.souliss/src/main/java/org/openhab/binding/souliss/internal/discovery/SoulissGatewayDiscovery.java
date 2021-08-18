/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.souliss.internal.discovery;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.SoulissProtocolConstants;
import org.openhab.binding.souliss.internal.handler.SoulissGatewayHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link soulissHandlerFactory} is responsible for creating things and thingGeneric
 * handlers.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
// @Component(service = DiscoveryService.class, configurationPid = "discovery.souliss")
public class SoulissGatewayDiscovery extends AbstractDiscoveryService implements DiscoverResult {
    private @Nullable ScheduledFuture<?> discoveryJob = null;
    private final Logger logger = LoggerFactory.getLogger(SoulissGatewayDiscovery.class);

    private @Nullable SoulissDiscoverJob soulissDiscoverRunnableClass = null;

    private SoulissGatewayHandler soulissGwHandler;

    public SoulissGatewayDiscovery(SoulissGatewayHandler bridgeHandler) {
        super(SoulissBindingConstants.SUPPORTED_THING_TYPES_UIDS, SoulissBindingConstants.DISCOVERY_TIMEOUT_IN_SECONDS,
                false);
        this.soulissGwHandler = bridgeHandler;
        bridgeHandler.discoverResult = this;
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    /**
     * The {@link gatewayDetected} callback used to create the Gateway
     */
    @Override
    public void gatewayDetected(InetAddress addr, String id) {
        logger.debug("Souliss gateway found: {} ", addr.getHostName());

        String label = "Souliss Gateway " + (Byte.parseByte(id) & 0xFF);
        Map<String, Object> properties = new TreeMap<>();
        properties.put(SoulissBindingConstants.CONFIG_IP_ADDRESS, addr.getHostAddress());
        var gatewayUID = new ThingUID(SoulissBindingConstants.GATEWAY_THING_TYPE,
                Integer.toString((Byte.parseByte(id) & 0xFF)));
        var discoveryResult = DiscoveryResultBuilder.create(gatewayUID).withLabel(label)
                .withRepresentationProperty(SoulissBindingConstants.CONFIG_IP_ADDRESS).withProperties(properties)
                .build();
        thingDiscovered(discoveryResult);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Scan Service");

        // create discovery class
        if (soulissDiscoverRunnableClass == null) {
            soulissDiscoverRunnableClass = new SoulissDiscoverJob(this.soulissGwHandler);

            discoveryJob = scheduler.scheduleWithFixedDelay(soulissDiscoverRunnableClass, 100,
                    SoulissBindingConstants.DISCOVERY_RESEND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
            logger.debug("Start Discovery Job");
        }
    }

    @Override
    protected synchronized void stopScan() {
        ScheduledFuture<?> localDiscoveryJob = this.discoveryJob;
        if (localDiscoveryJob != null) {
            localDiscoveryJob.cancel(false);
            localDiscoveryJob = null;
            soulissDiscoverRunnableClass = null;
            logger.debug("Discovery Job Stopped");
        }
        super.stopScan();
    }

    @Override
    public void thingDetectedActionMessages(String topicNumber, String sTopicVariant) {
        ThingUID thingUID = null;
        var label = "";
        DiscoveryResult discoveryResult;
        String sNodeID = topicNumber + SoulissBindingConstants.UUID_NODE_SLOT_SEPARATOR + sTopicVariant;

        var gatewayUID = this.soulissGwHandler.getThing().getUID();
        thingUID = new ThingUID(SoulissBindingConstants.TOPICS_THING_TYPE, gatewayUID, sNodeID);
        label = "Topic. Number: " + topicNumber + ", Variant: " + sTopicVariant;

        discoveryResult = DiscoveryResultBuilder.create(thingUID).withLabel(label).withProperty("number", topicNumber)
                .withProperty("variant", sTopicVariant).withBridge(gatewayUID).build();
        thingDiscovered(discoveryResult);
    }

    @Override
    public void thingDetectedTypicals(byte lastByteGatewayIP, byte typical, byte node, byte slot) {
        ThingUID thingUID = null;
        var label = "";
        DiscoveryResult discoveryResult;
        SoulissGatewayHandler gwHandler = this.soulissGwHandler;
        if (lastByteGatewayIP == (byte) Integer.parseInt(gwHandler.gwConfig.gatewayLanAddress.split("\\.")[3])) {
            String sNodeId = node + SoulissBindingConstants.UUID_NODE_SLOT_SEPARATOR + slot;

            ThingUID gatewayUID = gwHandler.getThing().getUID();

            switch (typical) {
                case SoulissProtocolConstants.SOULISS_T11:
                    thingUID = new ThingUID(SoulissBindingConstants.T11_THING_TYPE, gatewayUID, sNodeId);
                    label = "T11: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T12:
                    thingUID = new ThingUID(SoulissBindingConstants.T12_THING_TYPE, gatewayUID, sNodeId);
                    label = "T12: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T13:
                    thingUID = new ThingUID(SoulissBindingConstants.T13_THING_TYPE, gatewayUID, sNodeId);
                    label = "T13: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T14:
                    thingUID = new ThingUID(SoulissBindingConstants.T14_THING_TYPE, gatewayUID, sNodeId);
                    label = "T14: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T16:
                    thingUID = new ThingUID(SoulissBindingConstants.T16_THING_TYPE, gatewayUID, sNodeId);
                    label = "T16: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T18:
                    thingUID = new ThingUID(SoulissBindingConstants.T18_THING_TYPE, gatewayUID, sNodeId);
                    label = "T18: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T19:
                    thingUID = new ThingUID(SoulissBindingConstants.T19_THING_TYPE, gatewayUID, sNodeId);
                    label = "T19: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T1A:
                    thingUID = new ThingUID(SoulissBindingConstants.T1A_THING_TYPE, gatewayUID, sNodeId);
                    label = "T1A: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T21:
                    thingUID = new ThingUID(SoulissBindingConstants.T21_THING_TYPE, gatewayUID, sNodeId);
                    label = "T21: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T22:
                    thingUID = new ThingUID(SoulissBindingConstants.T22_THING_TYPE, gatewayUID, sNodeId);
                    label = "T22: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T41_ANTITHEFT_MAIN:
                    thingUID = new ThingUID(SoulissBindingConstants.T41_THING_TYPE, gatewayUID, sNodeId);
                    label = "T41: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T42_ANTITHEFT_PEER:
                    thingUID = new ThingUID(SoulissBindingConstants.T42_THING_TYPE, gatewayUID, sNodeId);
                    label = "T42: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T31:
                    thingUID = new ThingUID(SoulissBindingConstants.T31_THING_TYPE, gatewayUID, sNodeId);
                    label = "T31: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T52_TEMPERATURE_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T52_THING_TYPE, gatewayUID, sNodeId);
                    label = "T52: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T53_HUMIDITY_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T53_THING_TYPE, gatewayUID, sNodeId);
                    label = "T53: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T54_LUX_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T54_THING_TYPE, gatewayUID, sNodeId);
                    label = "T54: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T55_VOLTAGE_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T55_THING_TYPE, gatewayUID, sNodeId);
                    label = "T55: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T56_CURRENT_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T56_THING_TYPE, gatewayUID, sNodeId);
                    label = "T56: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T57_POWER_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T57_THING_TYPE, gatewayUID, sNodeId);
                    label = "T57: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T61:
                    thingUID = new ThingUID(SoulissBindingConstants.T61_THING_TYPE, gatewayUID, sNodeId);
                    label = "T61: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T62_TEMPERATURE_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T62_THING_TYPE, gatewayUID, sNodeId);
                    label = "T62: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T63_HUMIDITY_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T63_THING_TYPE, gatewayUID, sNodeId);
                    label = "T63: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T64_LUX_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T64_THING_TYPE, gatewayUID, sNodeId);
                    label = "T64: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T65_VOLTAGE_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T65_THING_TYPE, gatewayUID, sNodeId);
                    label = "T65: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T66_CURRENT_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T66_THING_TYPE, gatewayUID, sNodeId);
                    label = "T66: node " + node + ", slot " + slot;
                    break;
                case SoulissProtocolConstants.SOULISS_T67_POWER_SENSOR:
                    thingUID = new ThingUID(SoulissBindingConstants.T67_THING_TYPE, gatewayUID, sNodeId);
                    label = "T67: node " + node + ", slot " + slot;
                    break;
            }
            if (thingUID != null) {
                label = "[" + gwHandler.getThing().getUID().getAsString() + "] " + label;

                discoveryResult = DiscoveryResultBuilder.create(thingUID).withLabel(label).withProperty("node", node)
                        .withProperty("slot", slot).withBridge(gwHandler.getThing().getUID()).build();
                thingDiscovered(discoveryResult);
                gwHandler.setThereIsAThingDetection();

            }
        }
    }
}
