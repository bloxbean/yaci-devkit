package com.bloxbean.cardano.yacicli.cip30.controller;

import com.bloxbean.cardano.yacicli.cip30.dto.SignDataResponse;
import com.bloxbean.cardano.yacicli.cip30.dto.SignTxResponse;
import com.bloxbean.cardano.yacicli.cip30.service.Cip30Service;
import com.bloxbean.cardano.yacicli.cip30.service.TransferService;
import com.bloxbean.cardano.yacicli.cip30.service.WalletAccountService;
import com.bloxbean.cardano.yacicli.cip30.service.WalletSigningService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletApiControllerTest {
    private WalletSigningService walletSigningService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        walletSigningService = mock(WalletSigningService.class);

        WalletApiController controller = new WalletApiController(
                mock(WalletAccountService.class),
                walletSigningService,
                mock(TransferService.class),
                mock(Cip30Service.class),
                mock(ClusterService.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void signTransactionReturnsConcreteJsonResponse() throws Exception {
        when(walletSigningService.signTransaction(eq("84a0"), eq("0")))
                .thenReturn(Optional.of(SignTxResponse.builder()
                        .witnessSet("a100")
                        .txHash("abcd")
                        .build()));

        mockMvc.perform(post("/api/v1/wallet/sign-tx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"txCbor":"84a0","accountId":"0","partialSign":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.witnessSet").value("a100"))
                .andExpect(jsonPath("$.txHash").value("abcd"));
    }

    @Test
    void signDataReturnsConcreteJsonResponse() throws Exception {
        when(walletSigningService.signData(eq("48656c6c6f"), eq("00"), eq("0")))
                .thenReturn(Optional.of(SignDataResponse.builder()
                        .signature("8458")
                        .key("a401")
                        .build()));

        mockMvc.perform(post("/api/v1/wallet/sign-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address":"00","payload":"48656c6c6f","accountId":"0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.signature").value("8458"))
                .andExpect(jsonPath("$.key").value("a401"));
    }

    @Test
    void signDataFailureReturnsBadRequest() throws Exception {
        when(walletSigningService.signData(eq("48656c6c6f"), eq("00"), eq("0")))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/wallet/sign-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address":"00","payload":"48656c6c6f","accountId":"0"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Failed to sign data")));
    }
}
