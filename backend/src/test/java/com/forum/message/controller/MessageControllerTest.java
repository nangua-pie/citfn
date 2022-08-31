package com.forum.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.api.ApiResponse;
import com.forum.common.exception.BusinessException;
import com.forum.common.exception.GlobalExceptionHandler;
import com.forum.common.security.AuthenticatedUser;
import com.forum.common.security.CurrentUser;
import com.forum.message.dto.ConversationVO;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.dto.SendMessageRequest;
import com.forum.message.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for the private message API.
 *
 * <p>Runs against a standalone MockMvc so the assertions are about the unified
 * response envelope, parameter validation and the global exception handler
 * rather than about the service, which is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageControllerTest {

    private static final Long USER_ID = 12L;

    @Mock
    private MessageService messageService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MessageController(messageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void signIn() {
        AuthenticatedUser user = new AuthenticatedUser(USER_ID, "alice", List.of("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @Test
    @DisplayName("conversation list returns the success envelope")
    void listReturnsEnvelope() throws Exception {
        signIn();
        when(messageService.listConversations(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/im/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("an unauthenticated request is rejected with 40100")
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/im/conversations"))
                // The unified envelope carries the failure code; the transport
                // status stays 200 so clients branch on one field.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));

        verify(messageService, never()).listConversations(any());
    }

    @Test
    @DisplayName("history rejects a page size above the documented maximum")
    void historyRejectsOversizedPage() throws Exception {
        signIn();

        mockMvc.perform(get("/api/im/conversations/100/messages").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));

        verify(messageService, never()).loadHistory(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("history passes the id cursor straight through")
    void historyForwardsCursor() throws Exception {
        signIn();
        when(messageService.loadHistory(USER_ID, 100L, 9520L, 30)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/im/conversations/100/messages")
                        .param("beforeId", "9520")
                        .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(messageService).loadHistory(USER_ID, 100L, 9520L, 30);
    }

    @Test
    @DisplayName("send rejects a body without a receiver")
    void sendRejectsMissingReceiver() throws Exception {
        signIn();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "messageType", "TEXT",
                "content", "hello"));

        mockMvc.perform(post("/api/im/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));

        verify(messageService, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("send rejects an unsupported message type")
    void sendRejectsUnknownType() throws Exception {
        signIn();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "receiverId", 25,
                "messageType", "VIDEO",
                "content", "hello"));

        mockMvc.perform(post("/api/im/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));

        verify(messageService, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("send forwards a valid payload and returns the persisted message")
    void sendForwardsValidPayload() throws Exception {
        signIn();
        ImMessageVO message = ImMessageVO.builder()
                .messageId(9521L)
                .conversationId(100L)
                .senderId(USER_ID)
                .receiverId(25L)
                .messageType("TEXT")
                .content("hello")
                .status("SENT")
                .build();
        when(messageService.sendMessage(eq(USER_ID), any(SendMessageRequest.class))).thenReturn(message);

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "receiverId", 25,
                "messageType", "TEXT",
                "content", "hello"));

        mockMvc.perform(post("/api/im/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.messageId").value(9521))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @DisplayName("a business failure is mapped to its application code")
    void businessFailureMapsToCode() throws Exception {
        signIn();
        when(messageService.sendMessage(eq(USER_ID), any(SendMessageRequest.class)))
                .thenThrow(BusinessException.forbidden("this user does not accept private messages"));

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "receiverId", 25,
                "messageType", "TEXT",
                "content", "hello"));

        mockMvc.perform(post("/api/im/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("this user does not accept private messages"));
    }

    @Test
    @DisplayName("opening a conversation returns the caller view")
    void openConversationReturnsView() throws Exception {
        signIn();
        when(messageService.openConversation(USER_ID, 25L)).thenReturn(ConversationVO.builder()
                .id(100L).userId(USER_ID).targetUserId(25L)
                .targetNickname("bob").unreadCount(0).isPinned(0).targetOnline(true)
                .build());

        mockMvc.perform(post("/api/im/conversations/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.targetNickname").value("bob"));
    }

    @Test
    @DisplayName("unread count is returned as an object the header badge can bind to")
    void unreadCountShape() throws Exception {
        signIn();
        when(messageService.unreadCount(USER_ID)).thenReturn(4);

        mockMvc.perform(get("/api/im/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));
    }

    @Test
    @DisplayName("CurrentUser rejects a request with no principal")
    void currentUserRequiresPrincipal() {
        SecurityContextHolder.clearContext();
        org.assertj.core.api.Assertions
                .assertThatThrownBy(CurrentUser::requireUserId)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("the envelope always carries the four documented fields")
    void envelopeShapeIsStable() {
        ApiResponse<String> response = ApiResponse.success("payload");
        org.assertj.core.api.Assertions.assertThat(response.getCode()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.getMessage()).isEqualTo("success");
        org.assertj.core.api.Assertions.assertThat(response.getData()).isEqualTo("payload");
        org.assertj.core.api.Assertions.assertThat(response.getTraceId()).isNull();
    }

    @Test
    @DisplayName("marking a conversation read is delegated to the service")
    void markConversationReadDelegatesToService() throws Exception {
        signIn();

        mockMvc.perform(post("/api/im/conversations/100/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(messageService).markConversationRead(USER_ID, 100L);
    }

    @Test
    @DisplayName("marking a conversation read needs a session")
    void markConversationReadRequiresASession() throws Exception {
        mockMvc.perform(post("/api/im/conversations/100/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40100));

        verify(messageService, never()).markConversationRead(any(), any());
    }

    @Test
    @DisplayName("a conversation the caller does not own reads as 40400")
    void markConversationReadHidesForeignConversations() throws Exception {
        signIn();
        org.mockito.Mockito.doThrow(BusinessException.notFound("conversation not found"))
                .when(messageService).markConversationRead(USER_ID, 999L);

        mockMvc.perform(post("/api/im/conversations/999/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400));
    }
}
