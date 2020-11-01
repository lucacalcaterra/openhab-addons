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
package org.openhab.binding.riscocloud.internal.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.openhab.binding.riscocloud.internal.exceptions.RiscoCloudCommException;
import org.openhab.binding.riscocloud.internal.exceptions.RiscoCloudLoginException;
import org.openhab.core.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RiscoCloudConnection} Manage connection to Risco Cloud .
 *
 * @author Luca Calcaterra - Initial Contribution
 */
public class RiscoCloudConnection {

    private static final String LOGIN_URL = "https://app.melcloud.com/Mitsubishi.Wifi.Client/Login/ClientLogin";
    private static final String DEVICE_LIST_URL = "https://app.melcloud.com/Mitsubishi.Wifi.Client/User/ListDevices";
    private static final String DEVICE_URL = "https://app.melcloud.com/Mitsubishi.Wifi.Client/Device";

    private static final int TIMEOUT_MILLISECONDS = 10000;

    // Gson objects are safe to share across threads and are somewhat expensive to construct. Use a single instance.
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    private final Logger logger = LoggerFactory.getLogger(RiscoCloudConnection.class);

    private boolean isConnected = false;
    private String sessionKey;

    public void login(String username, String password, String languageId)
            throws RiscoCloudCommException, RiscoCloudLoginException {
        setConnected(false);
        sessionKey = null;
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("Email", username);
        jsonReq.addProperty("Password", password);
        jsonReq.addProperty("Language", languageId);
        jsonReq.addProperty("AppVersion", "1.17.5.0");
        jsonReq.addProperty("Persist", false);
        jsonReq.addProperty("CaptchaResponse", (String) null);
        InputStream data = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));

        try {
            String loginResponse = HttpUtil.executeUrl("POST", LOGIN_URL, null, data, "application/json",
                    TIMEOUT_MILLISECONDS);
            logger.debug("Login response: {}", loginResponse);
            // LoginClientResponse resp = gson.fromJson(loginResponse, LoginClientResponse.class);
            // if (resp.getErrorId() != null) {
            // String errorMsg = String.format("Login failed, error code: %s", resp.getErrorId());
            // if (resp.getErrorMessage() != null) {
            // errorMsg = String.format("%s (%s)", errorMsg, resp.getErrorMessage());
            // }
            // throw new RiscoCloudLoginException(errorMsg);
            // }
            // sessionKey = resp.getLoginData().getContextKey();
            // setConnected(true);
        } catch (IOException | JsonSyntaxException e) {
            throw new RiscoCloudCommException(String.format("Login error, reason: %s", e.getMessage()), e);
        }
    }

    // public List<Device> fetchDeviceList() throws RiscoCloudCommException {
    // assertConnected();
    // try {
    // String response = HttpUtil.executeUrl("GET", DEVICE_LIST_URL, getHeaderProperties(), null, null,
    // TIMEOUT_MILLISECONDS);
    // logger.debug("Device list response: {}", response);
    // List<Device> devices = new ArrayList<>();
    // ListDevicesResponse[] buildings = gson.fromJson(response, ListDevicesResponse[].class);
    // Arrays.asList(buildings).forEach(building -> {
    // if (building.getStructure().getDevices() != null) {
    // devices.addAll(building.getStructure().getDevices());
    // }
    // building.getStructure().getAreas().forEach(area -> {
    // if (area.getDevices() != null) {
    // devices.addAll(area.getDevices());
    // }
    // });
    // building.getStructure().getFloors().forEach(floor -> {
    // if (floor.getDevices() != null) {
    // devices.addAll(floor.getDevices());
    // }
    // floor.getAreas().forEach(area -> {
    // if (area.getDevices() != null) {
    // devices.addAll(area.getDevices());
    // }
    // });
    // });
    // });
    // logger.debug("Found {} devices", devices.size());
    //
    // return devices;
    // } catch (IOException | JsonSyntaxException e) {
    // setConnected(false);
    // throw new RiscoCloudCommException("Error occurred during device list poll", e);
    // }
    // }
    //
    // public DeviceStatus fetchDeviceStatus(int deviceId, int buildingId) throws RiscoCloudCommException {
    // assertConnected();
    // String url = DEVICE_URL + String.format("/Get?id=%d&buildingID=%d", deviceId, buildingId);
    // try {
    // String response = HttpUtil.executeUrl("GET", url, getHeaderProperties(), null, null, TIMEOUT_MILLISECONDS);
    // logger.debug("Device status response: {}", response);
    // DeviceStatus deviceStatus = gson.fromJson(response, DeviceStatus.class);
    // return deviceStatus;
    // } catch (IOException | JsonSyntaxException e) {
    // setConnected(false);
    // throw new RiscoCloudCommException("Error occurred during device status fetch", e);
    // }
    // }
    //
    // public DeviceStatus sendDeviceStatus(DeviceStatus deviceStatus) throws RiscoCloudCommException {
    // assertConnected();
    // String content = gson.toJson(deviceStatus, DeviceStatus.class);
    // logger.debug("Sending device status: {}", content);
    // InputStream data = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    // try {
    // String response = HttpUtil.executeUrl("POST", DEVICE_URL + "/SetAta", getHeaderProperties(), data,
    // "application/json", TIMEOUT_MILLISECONDS);
    // logger.debug("Device status sending response: {}", response);
    // return gson.fromJson(response, DeviceStatus.class);
    // } catch (IOException | JsonSyntaxException e) {
    // setConnected(false);
    // throw new RiscoCloudCommException("Error occurred during device command sending", e);
    // }
    // }

    public synchronized boolean isConnected() {
        return isConnected;
    }

    private synchronized void setConnected(boolean state) {
        isConnected = state;
    }

    private Properties getHeaderProperties() {
        Properties headers = new Properties();
        headers.put("X-MitsContextKey", sessionKey);
        return headers;
    }

    private void assertConnected() throws RiscoCloudCommException {
        if (!isConnected) {
            throw new RiscoCloudCommException("Not connected to RiscoCloud");
        }
    }
}
