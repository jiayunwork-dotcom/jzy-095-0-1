package com.example.notchfatigue.web;

import com.example.notchfatigue.config.AppConfig;
import com.example.notchfatigue.service.ExampleRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 层端到端测试（Spring 上下文 + MockMvc），
 * 顺带验证 Spring 能正常装配（{@link AppConfig}、{@link ExampleRunner} 启动载入算例不报错）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String STEEL_BODY = """
            {
              "material": {"e": 206000, "k": 1200, "n": 0.2},
              "kt": 3,
              "nominalStress": 200
            }
            """;

    @Test
    void singlePointSolveReturnsPlasticResultAndElasticBaseline() throws Exception {
        mockMvc.perform(post("/api/notch/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STEEL_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nominalStress").value(200.0))
                .andExpect(jsonPath("$.elasticStress").value(600.0))
                .andExpect(jsonPath("$.plastic").value(true))
                .andExpect(jsonPath("$.localStress").value(NumberMatchers.lessThan(600.0)))
                .andExpect(jsonPath("$.localStrain").value(
                        NumberMatchers.greaterThan(600.0 / 206000.0)));
    }

    @Test
    void elasticPointIsExactlyKtTimesNominalOverHttp() throws Exception {
        String body = """
                {
                  "material": {"e": 206000, "k": 1200, "n": 0.2},
                  "kt": 3,
                  "nominalStress": 1
                }
                """;
        mockMvc.perform(post("/api/notch/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localStress").value(3.0))
                .andExpect(jsonPath("$.plastic").value(false));
    }

    @Test
    void sequenceEndpointReturnsAllPoints() throws Exception {
        String body = """
                {
                  "material": {"e": 206000, "k": 1200, "n": 0.2},
                  "kt": 3,
                  "nominalStresses": [0, 50, 100, 200, 400]
                }
                """;
        mockMvc.perform(post("/api/notch/sequence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.points[0].localStress").value(0.0))
                .andExpect(jsonPath("$.points[4].plastic").value(true));
    }

    @Test
    void exampleEndpointReturnsBuiltInKtThreeCase() throws Exception {
        mockMvc.perform(get("/api/notch/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kt").value(3.0))
                .andExpect(jsonPath("$.points", hasSize(19)))
                .andExpect(jsonPath("$.material.k").value(1200.0));
    }

    @Test
    void unloadEndpointReturnsRanges() throws Exception {
        String body = """
                {
                  "material": {"e": 206000, "k": 1200, "n": 0.2},
                  "kt": 3,
                  "peakNominalStress": 200,
                  "targetNominalStress": 100
                }
                """;
        mockMvc.perform(post("/api/notch/unload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deltaStress").value(NumberMatchers.greaterThan(0.0)));
    }

    @Test
    void invalidKtReturns400WithReason() throws Exception {
        String body = """
                {
                  "material": {"e": 206000, "k": 1200, "n": 0.2},
                  "kt": 0.5,
                  "nominalStress": 100
                }
                """;
        mockMvc.perform(post("/api/notch/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reason", containsString("Kt")));
    }

    @Test
    void nonPositiveEReturns400WithReason() throws Exception {
        String body = """
                {
                  "material": {"e": -1, "k": 1200, "n": 0.2},
                  "kt": 3,
                  "nominalStress": 100
                }
                """;
        mockMvc.perform(post("/api/notch/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.reason", containsString("弹性模量")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/notch/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }
}
