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
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;

/**
 * The {@link SoulissT42Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tonino Fazio - Initial contribution
 * @author Luca Calcaterra - Refactor for OH3
 */

@NonNullByDefault
public class SoulissT42Handler extends SoulissGenericHandler {
    @Nullable
    Configuration gwConfigurationMap;
    byte t4nRawState;

    public SoulissT42Handler(Thing thing) {
        super(thing);
    }

    // called on every status change or change request
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getAsString().split(":")[3].equals(SoulissBindingConstants.T4N_REARMALARM_CHANNEL)) {
            if (command instanceof OnOffType) {
                switch (command.toFullString()) {
                    case "ON":
                        commandSEND(SoulissBindingProtocolConstants.SOULISS_T4N_REARM);
                        this.setState(StringType.valueOf(SoulissBindingConstants.T4N_REARMOFF_MESSAGE_CHANNEL));
                        break;
                }

            }
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);

        gwConfigurationMap = thingGeneric.getConfiguration();
        if (gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND) != null) {
            bSecureSend = ((Boolean) gwConfigurationMap.get(SoulissBindingConstants.CONFIG_SECURE_SEND)).booleanValue();
        }
    }

    public void setState(PrimitiveType state) {
        if (state instanceof StringType) {
            switch (String.valueOf(state)) {
                case SoulissBindingConstants.T4N_ALARMON_MESSAGE_CHANNEL:
                    this.updateState(SoulissBindingConstants.T4N_STATUSALARM_CHANNEL, OnOffType.ON);
                    break;
                case SoulissBindingConstants.T4N_ALARMOFF_MESSAGE_CHANNEL:
                    this.updateState(SoulissBindingConstants.T4N_STATUSALARM_CHANNEL, OnOffType.OFF);
                    break;
            }
        }
        // // Resetto il tasto di rearm. Questo perchè se premuto non torna da solo in off
        updateState(SoulissBindingConstants.T4N_REARMALARM_CHANNEL, OnOffType.OFF);

        super.setLastStatusStored();
    }

    @Override
    public void setRawState(byte rawState) {
        // update Last Status stored time
        super.setLastStatusStored();
        // update item state only if it is different from previous
        if (t4nRawState != rawState) {
            @Nullable
            OnOffType onOffVal = getOhStateOnOffFromSoulissVal(rawState);
            if (onOffVal != null) {
                this.setState(onOffVal);
            }
        }
        t4nRawState = rawState;
    }

    @Override
    public byte getRawState() {
        return t4nRawState;
    }

    @Override
    public byte getExpectedRawState(byte bCmd) {
        if (bSecureSend) {
            if (bCmd == SoulissBindingProtocolConstants.SOULISS_T4N_REARM) {
                return SoulissBindingProtocolConstants.SOULISS_T4N_ANTITHEFT;
            }
        }
        return -1;
    }
}
