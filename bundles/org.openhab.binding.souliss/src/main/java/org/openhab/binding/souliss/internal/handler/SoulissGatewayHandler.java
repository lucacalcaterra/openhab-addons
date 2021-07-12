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
package org.openhab.binding.souliss.internal.handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.SoulissBindingConstants;
import org.openhab.binding.souliss.internal.config.GatewayConfig;
import org.openhab.binding.souliss.internal.discovery.DiscoverResult;
import org.openhab.binding.souliss.internal.discovery.SoulissGatewayDiscovery;
import org.openhab.binding.souliss.internal.protocol.CommonCommands;
import org.openhab.binding.souliss.internal.protocol.SendDispatcherRunnable;
import org.openhab.binding.souliss.internal.protocol.UDPListenDiscoverRunnable;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SoulissGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
public class SoulissGatewayHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SoulissGatewayHandler.class);

    private ExecutorService udpExecutorService = Executors
            .newSingleThreadExecutor(new NamedThreadFactory("binding-souliss"));

    private @Nullable ScheduledFuture<?> pingScheduler;
    private @Nullable ScheduledFuture<?> subscriptionScheduler;
    private @Nullable ScheduledFuture<?> healthScheduler;

    private CommonCommands soulissCommands = new CommonCommands();

    boolean bGatewayDetected = false;

    private @Nullable SoulissGatewayDiscovery discoveryService;

    @Nullable
    public DiscoverResult discoverResult = null;

    public boolean thereIsAThingDetection = true;

    private Bridge bridge;

    private int nodes;
    private int maxTypicalXnode;
    private int countPingKo = 0;

    public GatewayConfig gwConfig = new GatewayConfig();

    public SoulissGatewayHandler(Bridge br) {
        super(br);
        bridge = br;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // do nothing
    }

    @Override
    public void initialize() {
        gwConfig = getConfigAs(GatewayConfig.class);

        logger.debug("Starting UDP server on Souliss Default Port for Topics (Publish&Subcribe)");

        // new runnable udp listener
        var udpServerDefaultPortRunnableClass = new UDPListenDiscoverRunnable(this.bridge, this.discoverResult);
        // and exec on thread
        this.udpExecutorService.execute(udpServerDefaultPortRunnableClass);

        // JOB PING
        var soulissGatewayJobPingRunnable = new SoulissGatewayJobPing(this.bridge);
        pingScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobPingRunnable, 2, this.gwConfig.pingInterval,
                TimeUnit.SECONDS);
        // JOB SUBSCRIPTION
        var soulissGatewayJobSubscriptionRunnable = new SoulissGatewayJobSubscription(bridge);
        subscriptionScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobSubscriptionRunnable, 5,
                this.gwConfig.subscriptionInterval, TimeUnit.SECONDS);

        // JOB HEALTH OF NODES
        var soulissGatewayJobHealthyRunnable = new SoulissGatewayJobHealthy(this.bridge);
        healthScheduler = scheduler.scheduleWithFixedDelay(soulissGatewayJobHealthyRunnable, 5,
                this.gwConfig.healthyInterval, TimeUnit.SECONDS);

        // il ciclo Send è schedulato con la costante
        // SoulissBindingConstants.SEND_DISPATCHER_MIN_DELAY_cicleInMillis
        // internamente il ciclo viene rallentato al timer impostato da configurazione (PaperUI o File)
        var soulissSendDispatcherRunnable = new SendDispatcherRunnable(this.bridge);
        scheduler.scheduleWithFixedDelay(soulissSendDispatcherRunnable, 15,
                SoulissBindingConstants.SEND_DISPATCHER_MIN_DELAY_CYCLE_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void dbStructAnswerReceived() {
        soulissCommands.sendTypicalRequestFrame(this.gwConfig, nodes);
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    int iPosNodeSlot = 2;

    public int getNodes() {
        var maxNode = 0;
        for (Thing thing : getThing().getThings()) {
            if (thing.getThingTypeUID().equals(SoulissBindingConstants.TOPICS_THING_TYPE)) {
                continue;
            }
            var cfg = thing.getConfiguration();
            var props = cfg.getProperties();
            var pNode = props.get("node");
            if (pNode != null) {
                int thingNode = Integer.parseInt(pNode.toString());

                if (thingNode > maxNode) {
                    maxNode = thingNode;
                }
            }
            // alla fine la lunghezza della lista sarà uguale al numero di nodi presenti
        }
        return maxNode + 1;
    }

    public void setMaxTypicalXnode(int maxTypicalXnode) {
        this.maxTypicalXnode = maxTypicalXnode;
    }

    public int getMaxTypicalXnode() {
        return maxTypicalXnode;
    }

    /**
     * The {@link gatewayDetected} is used to notify that UDPServer decoded a Ping Response from gateway
     *
     * @author Tonino Fazio - Initial contribution
     * @author Luca Calcaterra - Refactor for OH3
     */

    public void gatewayDetected() {
        logger.debug("Setting Gateway ONLINE");
        updateStatus(ThingStatus.ONLINE);
        countPingKo = 0; // reset counter
    }

    public void pingSent() {
        if (++countPingKo > 3) {
            var bridgeHandler = bridge.getHandler();
            if (bridgeHandler != null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                        "Gateway " + bridgeHandler.getThing().getUID() + " do not respond to " + countPingKo + " ping");
            }
        }
    }

    public synchronized void sendSubscription() {
        if (this.gwConfig.gatewayLanAddress.length() > 0) {
            int totNodes = getNodes();
            soulissCommands.sendSUBSCRIPTIONframe(this.gwConfig, totNodes);

        }
        logger.debug("Sent subscription packet");
    }

    public void setThereIsAThingDetection() {
        thereIsAThingDetection = true;
    }

    public void resetThereIsAThingDetection() {
        thereIsAThingDetection = false;
    }

    public @Nullable SoulissGatewayDiscovery getDiscoveryService() {
        return this.discoveryService;
    }

    public void setDiscoveryService(SoulissGatewayDiscovery discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void dispose() {
        var localPingScheduler = this.pingScheduler;
        if (localPingScheduler != null) {
            localPingScheduler.cancel(true);
        }
        var localSubscriptionScheduler = this.subscriptionScheduler;
        if (localSubscriptionScheduler != null) {
            localSubscriptionScheduler.cancel(true);
        }
        var localHealthScheduler = this.healthScheduler;
        if (localHealthScheduler != null) {
            localHealthScheduler.cancel(true);
        }
        this.udpExecutorService.shutdownNow();
        super.dispose();
    }
}
