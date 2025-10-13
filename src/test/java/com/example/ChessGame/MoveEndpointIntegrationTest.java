package com.example.ChessGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class MoveEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        // start a new game via POST /start
        mockMvc.perform(MockMvcRequestBuilders.post("/start")
                .param("whitePlayerName", "W")
                .param("blackPlayerName", "B"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testMoveAjaxReturnsJson() throws Exception {
        // perform a simple move - initial position: move white pawn e2 to e3
        mockMvc.perform(MockMvcRequestBuilders.post("/move")
                .param("from", "e2")
                .param("to", "e3")
                .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}
