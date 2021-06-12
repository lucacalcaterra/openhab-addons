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
package org.openhab.binding.souliss.internal;

import static org.openhab.binding.souliss.internal.SoulissBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.internal.discovery.SoulissGatewayDiscovery;
import org.openhab.binding.souliss.internal.handler.SoulissGatewayHandler;
import org.openhab.binding.souliss.internal.handler.SoulissT11Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT12Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT13Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT14Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT16Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT18Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT19Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT1AHandler;
import org.openhab.binding.souliss.internal.handler.SoulissT22Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT31Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT41Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT42Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT51Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT52Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT53Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT54Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT55Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT56Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT57Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT61Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT62Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT63Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT64Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT65Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT66Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT67Handler;
import org.openhab.binding.souliss.internal.handler.SoulissT68Handler;
import org.openhab.binding.souliss.internal.handler.SoulissTopicsHandler;
import org.openhab.binding.souliss.internal.protocol.NetworkParameters;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SoulissHandlerFactory} is responsible for creating things and thingGeneric
 * handlers. It fire when a new thingGeneric is added.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
@Component(configurationPid = "binding.souliss", service = ThingHandlerFactory.class)
public class SoulissHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(SoulissHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegMap = new HashMap<>();

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(GATEWAY_THING_TYPE)) {
            if (!NetworkParameters.getHashTableGateways().isEmpty()) {
                // logger.warn("Multiple gateways configuration is not supported");
                throw new UnsupportedOperationException("Multiple gateways configuration is not supported");

            }

            SoulissGatewayHandler bridgeHandler = new SoulissGatewayHandler((Bridge) thing);
            this.registerDiscoveryService(bridgeHandler);

            // get last byte of IP number
            Configuration gwConfigurationMap = thing.getConfiguration();
            String ipAddressOnLAN = (String) gwConfigurationMap.get(SoulissBindingConstants.CONFIG_IP_ADDRESS);
            NetworkParameters.addGateway((byte) Integer.parseInt(ipAddressOnLAN.split("\\.")[3]), thing);
            return bridgeHandler;
        } else if (thingTypeUID.equals(T11_THING_TYPE)) {
            logger.debug("Create handler for T11 '{}'", thingTypeUID);
            return new SoulissT11Handler(thing);
        } else if (thingTypeUID.equals(T12_THING_TYPE)) {
            logger.debug("Create handler for T12 '{}'", thingTypeUID);
            return new SoulissT12Handler(thing);
        } else if (thingTypeUID.equals(T13_THING_TYPE)) {
            logger.debug("Create handler for T13 '{}'", thingTypeUID);
            return new SoulissT13Handler(thing);
        } else if (thingTypeUID.equals(T14_THING_TYPE)) {
            logger.debug("Create handler for T14 '{}'", thingTypeUID);
            return new SoulissT14Handler(thing);
        } else if (thingTypeUID.equals(T16_THING_TYPE)) {
            logger.debug("Create handler for T16 '{}'", thingTypeUID);
            return new SoulissT16Handler(thing);
        } else if (thingTypeUID.equals(T18_THING_TYPE)) {
            logger.debug("Create handler for T18 '{}'", thingTypeUID);
            return new SoulissT18Handler(thing);
        } else if (thingTypeUID.equals(T19_THING_TYPE)) {
            logger.debug("Create handler for T19 '{}'", thingTypeUID);
            return new SoulissT19Handler(thing);
        } else if (thingTypeUID.equals(T1A_THING_TYPE)) {
            logger.debug("Create handler for T1A '{}'", thingTypeUID);
            return new SoulissT1AHandler(thing);
        } else if (thingTypeUID.equals(T21_THING_TYPE) || (thingTypeUID.equals(T22_THING_TYPE))) {
            logger.debug("Create handler for T21/T22 '{}'", thingTypeUID);
            return new SoulissT22Handler(thing);
        } else if (thingTypeUID.equals(T31_THING_TYPE)) {
            logger.debug("Create handler for T31 '{}'", thingTypeUID);
            return new SoulissT31Handler(thing);
        } else if (thingTypeUID.equals(T41_THING_TYPE)) {
            logger.debug("Create handler for T41 '{}'", thingTypeUID);
            return new SoulissT41Handler(thing);
        } else if (thingTypeUID.equals(T42_THING_TYPE)) {
            logger.debug("Create handler for T42 '{}'", thingTypeUID);
            return new SoulissT42Handler(thing);
        } else if (thingTypeUID.equals(T51_THING_TYPE)) {
            logger.debug("Create handler for T51 '{}'", thingTypeUID);
            return new SoulissT51Handler(thing);
        } else if (thingTypeUID.equals(T52_THING_TYPE)) {
            logger.debug("Create handler for T52 '{}'", thingTypeUID);
            return new SoulissT52Handler(thing);
        } else if (thingTypeUID.equals(T53_THING_TYPE)) {
            logger.debug("Create handler for T53 '{}'", thingTypeUID);
            return new SoulissT53Handler(thing);
        } else if (thingTypeUID.equals(T54_THING_TYPE)) {
            logger.debug("Create handler for T54 '{}'", thingTypeUID);
            return new SoulissT54Handler(thing);
        } else if (thingTypeUID.equals(T55_THING_TYPE)) {
            logger.debug("Create handler for T55 '{}'", thingTypeUID);
            return new SoulissT55Handler(thing);
        } else if (thingTypeUID.equals(T56_THING_TYPE)) {
            logger.debug("Create handler for T56 '{}'", thingTypeUID);
            return new SoulissT56Handler(thing);
        } else if (thingTypeUID.equals(T57_THING_TYPE)) {
            logger.debug("Create handler for T57 '{}'", thingTypeUID);
            return new SoulissT57Handler(thing);
        } else if (thingTypeUID.equals(T61_THING_TYPE)) {
            logger.debug("Create handler for T61 '{}'", thingTypeUID);
            return new SoulissT61Handler(thing);
        } else if (thingTypeUID.equals(T62_THING_TYPE)) {
            logger.debug("Create handler for T62 '{}'", thingTypeUID);
            return new SoulissT62Handler(thing);
        } else if (thingTypeUID.equals(T63_THING_TYPE)) {
            logger.debug("Create handler for T63 '{}'", thingTypeUID);
            return new SoulissT63Handler(thing);
        } else if (thingTypeUID.equals(T64_THING_TYPE)) {
            logger.debug("Create handler for T64 '{}'", thingTypeUID);
            return new SoulissT64Handler(thing);
        } else if (thingTypeUID.equals(T65_THING_TYPE)) {
            logger.debug("Create handler for T65 '{}'", thingTypeUID);
            return new SoulissT65Handler(thing);
        } else if (thingTypeUID.equals(T66_THING_TYPE)) {
            logger.debug("Create handler for T66 '{}'", thingTypeUID);
            return new SoulissT66Handler(thing);
        } else if (thingTypeUID.equals(T67_THING_TYPE)) {
            logger.debug("Create handler for T67 '{}'", thingTypeUID);
            return new SoulissT67Handler(thing);
        } else if (thingTypeUID.equals(T68_THING_TYPE)) {
            logger.debug("Create handler for T68 '{}'", thingTypeUID);
            return new SoulissT68Handler(thing);
        } else if (thingTypeUID.equals(TOPICS_THING_TYPE)) {
            logger.debug("Create handler for Action Messages (Topics) '{}'", thingTypeUID);
            NetworkParameters.addTopics(thing.getUID().getId(), thing);
            return new SoulissTopicsHandler(thing);
        }

        return null;
    }

    /**
     * Register a discovery service for an IP bridge handler.
     *
     * @param bridgeHandler IP bridge handler for which to register the discovery service
     */
    private synchronized void registerDiscoveryService(SoulissGatewayHandler bridgeHandler) {
        logger.debug("Registering XML device discovery service.");
        SoulissGatewayDiscovery discoveryService = new SoulissGatewayDiscovery(bridgeHandler);
        bridgeHandler.setDiscoveryService(discoveryService);
        discoveryServiceRegMap.put(bridgeHandler.getThing().getUID(),
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, null));
    }
}
