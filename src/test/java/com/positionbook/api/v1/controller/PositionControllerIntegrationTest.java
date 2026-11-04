package com.positionbook.api.v1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PositionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsTradeEventAndReturnsCurrentPosition() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"eventId":901,"eventType":"BUY","accountId":"API_ACC","securityId":"API_SEC","quantity":25}]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].quantity").value(25));

        mockMvc.perform(get("/api/v1/positions/API_ACC/API_SEC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(25));
    }

    @Test
    void returnsNotFoundWhenPositionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/positions/MISSING_ACC/MISSING_SEC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.localized_message").value(
                        "Position not found for account MISSING_ACC and security MISSING_SEC"));
    }

    @Test
    void rejectsInvalidRequestFields() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"eventId":902,"eventType":"BUY","accountId":" ","securityId":"API_SEC","quantity":25}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.sub_errors[0].message").value("Account ID is required"));
    }

    @Test
    void returnsStructuredValidationErrorForUnsupportedEventType() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"eventId":905,"eventType":"TEST","accountId":"API_ACC","securityId":"API_SEC","quantity":25}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.localized_message").value("Validation error occurred"))
                .andExpect(jsonPath("$.error.sub_errors[0].message")
                        .value("Event type must be BUY, SELL, or CANCEL"));
    }

    @Test
    void returnsStructuredValidationErrorForEmptyEventType() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"eventId":906,"eventType":"","accountId":"API_ACC","securityId":"API_SEC","quantity":25}]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.sub_errors[0].message").value("Event type is required"));
    }

    @Test
    void returnsUnprocessableEntityForInvalidTradeQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"eventId":903,"eventType":"BUY","accountId":"API_ACC","securityId":"API_SEC","quantity":0}]
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.sub_errors[0].message").value(
                        "Quantity must be greater than zero for BUY and SELL. Event Id: 903"));
    }

    @Test
    void rejectsDuplicateOriginalEventIdsAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"eventId":904,"eventType":"BUY","accountId":"API_ACC","securityId":"API_SEC","quantity":10},
                                  {"eventId":904,"eventType":"BUY","accountId":"API_ACC","securityId":"API_SEC","quantity":5}
                                ]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void exposesOpenApiSpecificationForPositionEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Position Book API"))
                .andExpect(jsonPath("$.paths['/api/v1/events'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['500'].description")
                        .value("Unexpected server error"))
                .andExpect(jsonPath("$.paths['/api/v1/positions/{accountId}/{securityId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/positions/{accountId}/{securityId}'].get.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/positions/{accountId}/{securityId}'].get.responses['400']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/positions/{accountId}/{securityId}'].get.responses['404']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/positions/{accountId}/{securityId}'].get.responses['500'].description")
                        .value("Unexpected server error"))
                .andExpect(jsonPath("$.components.schemas.ApiResponse.properties.status.example").value("OK"))
                .andExpect(jsonPath("$.components.schemas.ApiResponse.properties.code.example").value("200"))
                .andExpect(jsonPath("$.components.schemas.TradeEventRequest.properties.eventType.enum[0]").value("BUY"));
    }

    @Test
    void exposesActuatorHealthAndApplicationInfo() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Position Book"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"));
    }
}
