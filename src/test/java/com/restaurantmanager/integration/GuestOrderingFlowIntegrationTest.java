package com.restaurantmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk through the exact product flow: admin sets up a table and
 * a menu item, a guest scans the QR, browses the menu, places an order, and
 * staff progresses that order's status.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestOrderingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void guestCanScanQrBrowseMenuAndOrder_andStaffCanProgressTheOrder() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 1. Restaurant owner registers -> gets an admin JWT
        Map<String, String> registerBody = Map.of(
                "restaurantName", "The Test Bistro",
                "slug", "test-bistro-" + unique,
                "address", "1 Main St",
                "phone", "555-0100",
                "adminName", "Alice Admin",
                "adminEmail", "alice-" + unique + "@example.com",
                "adminPassword", "supersecret123"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register-restaurant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andReturn();
        String adminToken = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.token");

        // 2. Admin creates a table
        MvcResult tableResult = mockMvc.perform(post("/api/v1/admin/tables")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableNumber\":\"7\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String qrToken = JsonPath.read(tableResult.getResponse().getContentAsString(), "$.qrToken");

        // 3. Admin creates a menu category + item
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/menu-categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mains\",\"displayOrder\":0}"))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");

        Map<String, Object> itemBody = Map.of(
                "categoryId", categoryId,
                "name", "Margherita Pizza",
                "description", "Tomato, mozzarella, basil",
                "price", 12.50,
                "foodType", "VEG",
                "displayOrder", 0
        );
        MvcResult itemResult = mockMvc.perform(post("/api/v1/admin/menu-items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemBody)))
                .andExpect(status().isCreated())
                .andReturn();
        String menuItemId = JsonPath.read(itemResult.getResponse().getContentAsString(), "$.id");

        // 4. Guest scans the QR code
        mockMvc.perform(get("/api/v1/public/qr/{qrToken}", qrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value("7"));

        // 5. Guest starts a session with their name
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/public/guest-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\":\"" + qrToken + "\",\"guestName\":\"Bob Guest\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String guestToken = JsonPath.read(sessionResult.getResponse().getContentAsString(), "$.guestToken");
        String restaurantId = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.user.restaurantId");

        // 6. Guest browses the public menu and sees the item
        mockMvc.perform(get("/api/v1/public/restaurants/{id}/menu", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].name").value("Margherita Pizza"));

        // 7. Guest places an order
        String orderBody = "{\"items\":[{\"menuItemId\":\"" + menuItemId + "\",\"quantity\":2,\"notes\":\"extra basil\"}],\"notes\":\"Table by the window\"}";
        MvcResult orderResult = mockMvc.perform(post("/api/v1/guest/orders")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(25.0))
                .andReturn();
        String orderId = JsonPath.read(orderResult.getResponse().getContentAsString(), "$.id");

        // 8. Staff/admin sees it in the live queue
        mockMvc.perform(get("/api/v1/staff/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));

        // 9. Staff accepts the order
        mockMvc.perform(patch("/api/v1/staff/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 10. Guest can see the updated status on their own order
        MvcResult guestOrderResult = mockMvc.perform(get("/api/v1/guest/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andReturn();
        String status = JsonPath.read(guestOrderResult.getResponse().getContentAsString(), "$.status");
        assertThat(status).isEqualTo("ACCEPTED");

        // 11. A guest without a valid token cannot reach staff endpoints
        mockMvc.perform(get("/api/v1/staff/orders")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }
}
