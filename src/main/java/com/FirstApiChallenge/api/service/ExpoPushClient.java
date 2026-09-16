package com.FirstApiChallenge.api.service;

import java.util.Map;

public interface ExpoPushClient {

    ExpoPushTicket send(String token, String title, String body, Map<String, Object> data) throws Exception;
}
