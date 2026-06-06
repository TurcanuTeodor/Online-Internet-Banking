package ro.app.client.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ro.app.client.audit.AuditService;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientContactService;
import ro.app.client.service.ClientProfileService;
import ro.app.client.service.ClientViewProjectionService;

/**
 * Teste unitare pentru ClientController (JUnit 4).
 *
 * Pattern testat: Facade (Structural) — controllerul delega catre servicii specializate
 * si verifica ownership inainte de orice operatie sensibila.
 *
 * JwtPrincipal construit cu "new" (este un record simplu cu 4 parametri).
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientControllerTest {

    @Mock
    private ClientProfileService clientProfileService;
    @Mock
    private ClientContactService clientContactService;
    @Mock
    private ClientViewProjectionService clientViewProjectionService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClientController clientController;

    private JwtPrincipal clientPrincipal;

    @Before
    public void setUp() {
        // JwtPrincipal este un record: (username, clientId, role, encryptionKey)
        clientPrincipal = new JwtPrincipal("ion.pop", 123L, "CLIENT", "test-encryption-key");
    }

    // ── updateContact ─────────────────────────────────────────────────────────

    @Test
    public void updateContact_ownershipCheckCalled_withCorrectClientId() {
        // Arrange
        ContactInfoDTO dto = new ContactInfoDTO();
        Mockito.when(clientContactService.updateClientContactInfo(
                Mockito.eq(123L), Mockito.any(ContactInfoDTO.class),
                Mockito.anyString(), Mockito.isNull()))
               .thenReturn(dto);

        // Act
        ResponseEntity<ContactInfoDTO> response = clientController.updateContact(
                123L, dto, null, clientPrincipal);

        // Assert
        Mockito.verify(ownershipChecker).checkOwnership(clientPrincipal, 123L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void updateContact_returnsUpdatedDto() {
        // Arrange
        ContactInfoDTO dto = new ContactInfoDTO();
        dto.setEmail("ion.pop@example.com");
        Mockito.when(clientContactService.updateClientContactInfo(
                Mockito.eq(123L), Mockito.any(ContactInfoDTO.class),
                Mockito.anyString(), Mockito.isNull()))
               .thenReturn(dto);

        // Act
        ResponseEntity<ContactInfoDTO> response = clientController.updateContact(
                123L, dto, null, clientPrincipal);

        // Assert
        assertEquals("ion.pop@example.com", response.getBody().getEmail());
    }
}
