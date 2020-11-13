/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.riscocloud.internal.handler;

import static org.openhab.binding.riscocloud.internal.RiscoCloudBindingConstants.CHANNEL_ARMEDSTATE;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.riscocloud.internal.api.json.cpstate.Partition;
import org.openhab.binding.riscocloud.internal.config.PartitionConfig;
import org.openhab.binding.riscocloud.internal.exceptions.RiscoCloudCommException;
import org.openhab.binding.riscocloud.internal.exceptions.RiscoCloudLoginException;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoCloudPartitionHandler} is responsible for handling partitions commands, which are
 * sent to one of the channels.
 *
 * @author Luca Calcaterra - Initial contribution
 */

public class RiscoCloudPartitionHandler extends BaseThingHandler {

    // private static final int EFFECTIVE_FLAG_POWER = 0x01;
    // private static final int EFFECTIVE_FLAG_OPERATION_MODE = 0x02;
    // private static final int EFFECTIVE_FLAG_TEMPERATURE = 0x04;
    // private static final int EFFECTIVE_FLAG_FAN_SPEED = 0x08;
    // private static final int EFFECTIVE_FLAG_VANE_VERTICAL = 0x10;
    // private static final int EFFECTIVE_FLAG_VANE_HORIZONTAL = 0x100;

    private final Logger logger = LoggerFactory.getLogger(RiscoCloudPartitionHandler.class);
    private PartitionConfig config;
    private RiscoCloudAccountHandler riscoCloudHandler;
    private Partition partitionStatus;
    private ScheduledFuture<?> refreshTask;

    public RiscoCloudPartitionHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing {} handler.", getThing().getThingTypeUID());

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
            return;
        }

        config = getConfigAs(PartitionConfig.class);
        logger.debug("Partition config: {}", config);

        initializeBridge(bridge.getHandler(), bridge.getStatus());
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
        riscoCloudHandler = null;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        Bridge bridge = getBridge();
        if (bridge != null) {
            initializeBridge(bridge.getHandler(), bridgeStatusInfo.getStatus());
        }
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus bridgeStatus) {
        logger.debug("initializeBridge {} for thing {}", bridgeStatus, getThing().getUID());

        if (thingHandler != null && bridgeStatus != null) {
            riscoCloudHandler = (RiscoCloudAccountHandler) thingHandler;

            if (bridgeStatus == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
                startAutomaticRefresh();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command '{}' to channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            logger.debug("Refresh command not supported");
            return;
        }

        if (riscoCloudHandler == null) {
            logger.warn("No connection to RiscoCloud available, ignoring command");
            return;
        }

        // if (deviceStatus == null) {
        // logger.info("No initial data available, bridge is probably offline. Ignoring command");
        // return;
        // }

        // DeviceStatus cmdtoSend = getDeviceStatusCopy(deviceStatus);
        // cmdtoSend.setEffectiveFlags(0);

        // switch (channelUID.getId()) {
        // case CHANNEL_POWER:
        // cmdtoSend.setPower(command == OnOffType.ON);
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_POWER);
        // break;
        // case CHANNEL_OPERATION_MODE:
        // cmdtoSend.setOperationMode(Integer.parseInt(command.toString()));
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_OPERATION_MODE);
        // break;
        // case CHANNEL_SET_TEMPERATURE:
        // BigDecimal val = null;
        // if (command instanceof QuantityType) {
        // QuantityType<Temperature> quantity = new QuantityType<Temperature>(command.toString())
        // .toUnit(CELSIUS);
        // if (quantity != null) {
        // val = quantity.toBigDecimal().setScale(1, RoundingMode.HALF_UP);
        // // round nearest .5
        // double v = Math.round(val.doubleValue() * 2) / 2.0;
        // cmdtoSend.setSetTemperature(v);
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_TEMPERATURE);
        // }
        // }
        // if (val == null) {
        // logger.debug("Can't convert '{}' to set temperature", command);
        // }
        // break;
        // case CHANNEL_FAN_SPEED:
        // cmdtoSend.setSetFanSpeed(Integer.parseInt(command.toString()));
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_FAN_SPEED);
        // break;
        // case CHANNEL_VANE_VERTICAL:
        // cmdtoSend.setVaneVertical(Integer.parseInt(command.toString()));
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_VANE_VERTICAL);
        // break;
        // case CHANNEL_VANE_HORIZONTAL:
        // cmdtoSend.setVaneHorizontal(Integer.parseInt(command.toString()));
        // cmdtoSend.setEffectiveFlags(EFFECTIVE_FLAG_VANE_HORIZONTAL);
        // break;
        // default:
        // logger.debug("Read-only or unknown channel {}, skipping update", channelUID);
        // }
        //
        // if (cmdtoSend.getEffectiveFlags() > 0) {
        // cmdtoSend.setHasPendingCommand(true);
        // cmdtoSend.setDeviceID(config.deviceID);
        // try {
        // DeviceStatus newDeviceStatus = riscoCloudHandler.sendDeviceStatus(cmdtoSend);
        // updateChannels(newDeviceStatus);
        // } catch (RiscoCloudLoginException e) {
        // logger.warn("Command '{}' to channel '{}' failed due to login error, reason {}. ", command, channelUID,
        // e.getMessage(), e);
        // } catch (RiscoCloudCommException e) {
        // logger.warn("Command '{}' to channel '{}' failed, reason {}. ", command, channelUID, e.getMessage(), e);
        // }
        // } else {
        // logger.debug("Nothing to send");
        // }
    }

    // private DeviceStatus getDeviceStatusCopy(DeviceStatus deviceStatus) {
    // DeviceStatus copy = new DeviceStatus();
    // synchronized (this) {
    // copy.setPower(deviceStatus.getPower());
    // copy.setOperationMode(deviceStatus.getOperationMode());
    // copy.setSetTemperature(deviceStatus.getSetTemperature());
    // copy.setSetFanSpeed(deviceStatus.getSetFanSpeed());
    // copy.setVaneVertical(deviceStatus.getVaneVertical());
    // copy.setVaneHorizontal(deviceStatus.getVaneHorizontal());
    // copy.setEffectiveFlags(deviceStatus.getEffectiveFlags());
    // copy.setHasPendingCommand(deviceStatus.getHasPendingCommand());
    // copy.setDeviceID(deviceStatus.getDeviceID());
    // }
    // return copy;
    // }

    private void startAutomaticRefresh() {
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleWithFixedDelay(this::getDeviceDataAndUpdateChannels, 1,
                    config.pollingInterval, TimeUnit.SECONDS);
        }
    }

    private void getDeviceDataAndUpdateChannels() {
        if (riscoCloudHandler.isConnected()) {
            logger.debug("Update device '{}' channels", getThing().getThingTypeUID());
            try {
                Partition partition = riscoCloudHandler.getCpState().getStatus().getPartitions()
                        .get(config.partitionID - 1);

                // logger.debug("status");
                // Optional.ofNullable(config.buildingID));
                updateChannels(partition);
            } catch (RiscoCloudLoginException e) {
                logger.debug("Login error occurred during device '{}' polling, reason {}. ",
                        getThing().getThingTypeUID(), e.getMessage(), e);
            } catch (RiscoCloudCommException e) {
                logger.debug("Error occurred during device '{}' polling, reason {}. ", getThing().getThingTypeUID(),
                        e.getMessage(), e);

            }
        } else {
            logger.debug("Connection to RiscoCloud is not open, skipping periodic update");
        }

    }

    private synchronized void updateChannels(Partition newPartitionStatus) {

        partitionStatus = newPartitionStatus;
        for (Channel channel : getThing().getChannels()) {
            updateChannels(channel.getUID().getId(), partitionStatus);
        }
    }

    private void updateChannels(String channelId, Partition partitionStatus) {
        switch (channelId) {
            case CHANNEL_ARMEDSTATE:
                updateState(CHANNEL_ARMEDSTATE, new StringType(partitionStatus.getArmedState().toString()));
                break;
        }
        // switch (channelId) {
        // case CHANNEL_POWER:
        // updateState(CHANNEL_POWER, deviceStatus.getPower() ? OnOffType.ON : OnOffType.OFF);
        // break;
        // case CHANNEL_OPERATION_MODE:
        // updateState(CHANNEL_OPERATION_MODE, new StringType(deviceStatus.getOperationMode().toString()));
        // break;
        // case CHANNEL_SET_TEMPERATURE:
        // updateState(CHANNEL_SET_TEMPERATURE,
        // new QuantityType<>(deviceStatus.getSetTemperature(), SIUnits.CELSIUS));
        // break;
        // case CHANNEL_FAN_SPEED:
        // updateState(CHANNEL_FAN_SPEED, new StringType(deviceStatus.getSetFanSpeed().toString()));
        // break;
        // case CHANNEL_VANE_HORIZONTAL:
        // updateState(CHANNEL_VANE_HORIZONTAL, new StringType(deviceStatus.getVaneHorizontal().toString()));
        // break;
        // case CHANNEL_VANE_VERTICAL:
        // updateState(CHANNEL_VANE_VERTICAL, new StringType(deviceStatus.getVaneVertical().toString()));
        // break;
        // case CHANNEL_OFFLINE:
        // updateState(CHANNEL_OFFLINE, deviceStatus.getOffline() ? OnOffType.ON : OnOffType.OFF);
        // break;
        // case CHANNEL_HAS_PENDING_COMMAND:
        // updateState(CHANNEL_HAS_PENDING_COMMAND,
        // deviceStatus.getHasPendingCommand() ? OnOffType.ON : OnOffType.OFF);
        // break;
        // case CHANNEL_ROOM_TEMPERATURE:
        // updateState(CHANNEL_ROOM_TEMPERATURE, new DecimalType(deviceStatus.getRoomTemperature()));
        // break;
        // case CHANNEL_LAST_COMMUNICATION:
        // updateState(CHANNEL_LAST_COMMUNICATION,
        // new DateTimeType(convertDateTime(deviceStatus.getLastCommunication())));
        // break;
        // case CHANNEL_NEXT_COMMUNICATION:
        // updateState(CHANNEL_NEXT_COMMUNICATION,
        // new DateTimeType(convertDateTime(deviceStatus.getNextCommunication())));
        // break;
        // }
    }

    // private ZonedDateTime convertDateTime(String dateTime) {
    // return ZonedDateTime.ofInstant(LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    // ZoneOffset.UTC, ZoneId.systemDefault());
    // }
}
