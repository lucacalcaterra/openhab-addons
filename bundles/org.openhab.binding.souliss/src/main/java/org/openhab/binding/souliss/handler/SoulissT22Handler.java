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
package org.openhab.binding.souliss.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.souliss.SoulissBindingConstants;
import org.openhab.binding.souliss.SoulissBindingProtocolConstants;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.RefreshType;

/**
 * The {@link SoulissT22Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */
@NonNullByDefault
public class SoulissT22Handler extends SoulissGenericHandler {
    @Nullable
    Configuration gwConfigurationMap;
    byte t2nRawState;

    public SoulissT22Handler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
        gwConfigurationMap = thingGeneric.getConfiguration();
        if (gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND) != null) {
            bSecureSend = ((Boolean) gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND)).booleanValue();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ROLLERSHUTTER_CHANNEL:
                    break;
            }
        } else {
            switch (channelUID.getId()) {
                case SoulissBindingConstants.ROLLERSHUTTER_CHANNEL:
                    if (command.equals(UpDownType.UP)) {
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T2N_OPEN_CMD);
                    } else if (command.equals(UpDownType.DOWN)) {
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T2N_CLOSE_CMD);
                    } else if (command.equals(StopMoveType.STOP)) {
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T2N_STOP_CMD);
                    }
                    break;
                case SoulissBindingConstants.ONOFF_CHANNEL:
                    if (command.equals(OnOffType.ON)) {
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T2N_OPEN_CMD_LOCAL);
                    } else if (command.equals(OnOffType.OFF)) {
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T2N_CLOSE_CMD_LOCAL);
                    }
                    break;
            }
        }
    }

    public void setState(PrimitiveType state) {
        if (state instanceof PercentType) {
            this.updateState(SoulissBindingConstants.ROLLERSHUTTER_CHANNEL, (PercentType) state);

        }
    }

    public void setStateMessage(String rollershutterMessage) {
        this.updateState(SoulissBindingConstants.ROLLERSHUTTER_STATE_CHANNEL_CHANNEL,
                StringType.valueOf(rollershutterMessage));
    }

    @Override
    public void setRawState(byte rawState) {
        // update Last Status stored time
        super.setLastStatusStored();
        // update item state only if it is different from previous
        if (t2nRawState != rawState) {
            this.setState(getOhStateT22FromSoulissVal(rawState));

            if (rawState == SoulissBindingProtocolConstants.SOULISS_T2N_OPEN_CMD) {
                this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_OPENING_CHANNEL);
            } else if (rawState == SoulissBindingProtocolConstants.SOULISS_T2N_CLOSE_CMD) {
                this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_CLOSING_CHANNEL);
            }
            switch (rawState) {
                case SoulissBindingProtocolConstants.SOULISS_T2N_COIL_STOP:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_STOP_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_COIL_OFF:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_OPENING_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_LIMSWITCH_CLOSE:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_LIMITSWITCH_CLOSE_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_LIMSWITCH_OPEN:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_LIMITSWITCH_OPEN_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_NOLIMSWITCH:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_LIMITSWITCH_OPEN_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_TIMER_OFF:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_TIMER_OFF);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_STATE_OPEN:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_STATE_OPEN_CHANNEL);
                    break;
                case SoulissBindingProtocolConstants.SOULISS_T2N_STATE_CLOSE:
                    this.setStateMessage(SoulissBindingConstants.ROLLERSHUTTER_MESSAGE_STATE_CLOSE_CHANNEL);
                    break;
            }
            t2nRawState = rawState;
        }
    }

    private PercentType getOhStateT22FromSoulissVal(short sVal) {
        int iState = 0;
        switch (sVal) {
            case SoulissBindingProtocolConstants.SOULISS_T2N_COIL_OPEN:
                iState = 0;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_COIL_CLOSE:
                iState = 100;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_COIL_STOP:
                iState = 50;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_LIMSWITCH_CLOSE:
                iState = 100;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_LIMSWITCH_OPEN:
                iState = 0;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_NOLIMSWITCH:
                iState = 50;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_TIMER_OFF:
                iState = 50;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_STATE_OPEN:
                iState = 0;
                break;
            case SoulissBindingProtocolConstants.SOULISS_T2N_STATE_CLOSE:
                iState = 100;
                break;
        }
        return PercentType.valueOf(String.valueOf(iState));
    }

    @Override
    public byte getRawState() {
        return t2nRawState;
    }

    @Override
    public byte getExpectedRawState(byte bCmd) {
        if (bSecureSend) {
            if (bCmd == SoulissBindingProtocolConstants.SOULISS_T2N_OPEN_CMD) {
                return SoulissBindingProtocolConstants.SOULISS_T2N_COIL_OPEN;
            } else if (bCmd == SoulissBindingProtocolConstants.SOULISS_T2N_CLOSE_CMD) {
                return SoulissBindingProtocolConstants.SOULISS_T2N_COIL_CLOSE;
            } else if (bCmd >= SoulissBindingProtocolConstants.SOULISS_T2N_STOP_CMD) {
                return SoulissBindingProtocolConstants.SOULISS_T2N_COIL_STOP;
            }
        }
        return -1;
    }
}
