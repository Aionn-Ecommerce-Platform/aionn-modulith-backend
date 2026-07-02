package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.address.request.CreateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.request.UpdateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.response.AddressResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.address.AddressDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.address.command.*;
import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.application.port.in.address.*;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.AddressType;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerWebTest {

        @Mock
        private ListAddressesQueryPort listAddressesQueryPort;
        @Mock
        private CreateAddressInputPort createAddressInputPort;
        @Mock
        private UpdateAddressInputPort updateAddressInputPort;
        @Mock
        private DeleteAddressInputPort deleteAddressInputPort;
        @Mock
        private SetDefaultAddressInputPort setDefaultAddressInputPort;
        @Mock
        private AddressDtoMapper addressDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AddressController controller = new AddressController(listAddressesQueryPort, createAddressInputPort,
                                updateAddressInputPort, deleteAddressInputPort, setDefaultAddressInputPort,
                                addressDtoMapper);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new IdentityExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(
                                                new ClientIpArgumentResolver(new ClientIpResolver()),
                                                new MockAuthenticationArgumentResolver())
                                .build();
        }

        @Test
        void listReturnsAllAddressesForUser() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                AddressResult addr1 = new AddressResult("addr-1", "user-123", "John Doe", "0912345678",
                                "01", "Hanoi", "001", "Ba Dinh", "00001", "Cong Vi Ward",
                                "123 Main St, Apt 4", "123 Main St, Apt 4, Cong Vi Ward, Ba Dinh, Hanoi",
                                AddressType.HOME, true, now, now);
                AddressResult addr2 = new AddressResult("addr-2", "user-123", "Jane Smith", "0987654321",
                                "79", "Ho Chi Minh", "760", "District 1", "26734", "Ben Nghe Ward",
                                "456 Work Ave", "456 Work Ave, Ben Nghe Ward, District 1, Ho Chi Minh",
                                AddressType.OFFICE, false, now, now);
                List<AddressResult> results = List.of(addr1, addr2);

                AddressResponse resp1 = new AddressResponse("addr-1", "John Doe", "0912345678",
                                "01", "Hanoi", "001", "Ba Dinh", "00001", "Cong Vi Ward",
                                "123 Main St, Apt 4", "123 Main St, Apt 4, Cong Vi Ward, Ba Dinh, Hanoi",
                                AddressType.HOME, true, now, now);
                AddressResponse resp2 = new AddressResponse("addr-2", "Jane Smith", "0987654321",
                                "79", "Ho Chi Minh", "760", "District 1", "26734", "Ben Nghe Ward",
                                "456 Work Ave", "456 Work Ave, Ben Nghe Ward, District 1, Ho Chi Minh",
                                AddressType.OFFICE, false, now, now);

                when(listAddressesQueryPort.execute("alice@example.com")).thenReturn(results);
                when(addressDtoMapper.toResponses(results)).thenReturn(List.of(resp1, resp2));

                mockMvc.perform(get("/api/v1/addresses")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].contactName").value("John Doe"))
                                .andExpect(jsonPath("$.data[1].contactName").value("Jane Smith"));

                verify(listAddressesQueryPort).execute("alice@example.com");
        }

        @Test
        void createCreatesNewAddress() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                AddressResult result = new AddressResult("addr-new-789", "user-123", "Bob Johnson", "0901234567",
                                "01", "Hanoi", "002", "Hoan Kiem", "00019", "Hang Bong Ward",
                                "789 Elm St, Suite 10", "789 Elm St, Suite 10, Hang Bong Ward, Hoan Kiem, Hanoi",
                                AddressType.HOME, false, now, now);
                AddressResponse response = new AddressResponse("addr-new-789", "Bob Johnson", "0901234567",
                                "01", "Hanoi", "002", "Hoan Kiem", "00019", "Hang Bong Ward",
                                "789 Elm St, Suite 10", "789 Elm St, Suite 10, Hang Bong Ward, Hoan Kiem, Hanoi",
                                AddressType.HOME, false, now, now);

                when(addressDtoMapper.toCreateCommand(eq("alice@example.com"), any(CreateAddressRequest.class)))
                                .thenReturn(new CreateAddressCommand("user-123", "Bob Johnson", "0901234567",
                                                "01", "002", "00019", "789 Elm St, Suite 10", AddressType.HOME, false));
                when(createAddressInputPort.execute(any())).thenReturn(result);
                when(addressDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/addresses")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "Bob Johnson",
                                                  "phone": "0901234567",
                                                  "provinceCode": "01",
                                                  "districtCode": "002",
                                                  "wardCode": "00019",
                                                  "detailAddress": "789 Elm St, Suite 10",
                                                  "type": "HOME",
                                                  "isDefault": false
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.addressId").value("addr-new-789"))
                                .andExpect(jsonPath("$.data.contactName").value("Bob Johnson"));

                verify(createAddressInputPort).execute(any());
        }

        @Test
        void updateUpdatesExistingAddress() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                AddressResult result = new AddressResult("addr-123", "user-123", "Alice Updated", "0998877665",
                                "79", "Ho Chi Minh", "766", "District 7", "27127", "Tan Thuan Dong Ward",
                                "999 Business Rd, Floor 5",
                                "999 Business Rd, Floor 5, Tan Thuan Dong Ward, District 7, Ho Chi Minh",
                                AddressType.OFFICE, false, now, now);
                AddressResponse response = new AddressResponse("addr-123", "Alice Updated", "0998877665",
                                "79", "Ho Chi Minh", "766", "District 7", "27127", "Tan Thuan Dong Ward",
                                "999 Business Rd, Floor 5",
                                "999 Business Rd, Floor 5, Tan Thuan Dong Ward, District 7, Ho Chi Minh",
                                AddressType.OFFICE, false, now, now);

                when(addressDtoMapper.toUpdateCommand(eq("alice@example.com"), eq("addr-123"),
                                any(UpdateAddressRequest.class)))
                                .thenReturn(new UpdateAddressCommand("user-123", "addr-123", "Alice Updated",
                                                "0998877665",
                                                "79", "766", "27127", "999 Business Rd, Floor 5", AddressType.OFFICE));
                when(updateAddressInputPort.execute(any())).thenReturn(result);
                when(addressDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(put("/api/v1/addresses/addr-123")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "Alice Updated",
                                                  "phone": "0998877665",
                                                  "provinceCode": "79",
                                                  "districtCode": "766",
                                                  "wardCode": "27127",
                                                  "detailAddress": "999 Business Rd, Floor 5",
                                                  "type": "OFFICE"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.contactName").value("Alice Updated"));

                verify(updateAddressInputPort).execute(any());
        }

        @Test
        void deleteDeletesAddress() throws Exception {
                when(addressDtoMapper.toDeleteCommand("alice@example.com", "addr-456"))
                                .thenReturn(new DeleteAddressCommand("user-123", "addr-456"));
                doNothing().when(deleteAddressInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/addresses/addr-456")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Address deleted"));

                verify(deleteAddressInputPort).execute(any());
        }

        @Test
        void setDefaultMarksAddressAsDefault() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                AddressResult result = new AddressResult("addr-789", "user-123", "Mark Default", "0911223344",
                                "01", "Hanoi", "005", "Cau Giay", "00169", "Dich Vong Ward",
                                "321 Pine St", "321 Pine St, Dich Vong Ward, Cau Giay, Hanoi",
                                AddressType.HOME, true, now, now);
                AddressResponse response = new AddressResponse("addr-789", "Mark Default", "0911223344",
                                "01", "Hanoi", "005", "Cau Giay", "00169", "Dich Vong Ward",
                                "321 Pine St", "321 Pine St, Dich Vong Ward, Cau Giay, Hanoi",
                                AddressType.HOME, true, now, now);

                when(addressDtoMapper.toSetDefaultCommand("alice@example.com", "addr-789"))
                                .thenReturn(new SetDefaultAddressCommand("user-123", "addr-789"));
                when(setDefaultAddressInputPort.execute(any())).thenReturn(result);
                when(addressDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(patch("/api/v1/addresses/addr-789/default")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.isDefault").value(true));

                verify(setDefaultAddressInputPort).execute(any());
        }

        @Test
        void createRejectsBlankContactName() throws Exception {
                mockMvc.perform(post("/api/v1/addresses")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "",
                                                  "phone": "0901234567",
                                                  "provinceCode": "01",
                                                  "districtCode": "002",
                                                  "wardCode": "00019",
                                                  "detailAddress": "789 Elm St",
                                                  "type": "HOME",
                                                  "isDefault": false
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(createAddressInputPort);
        }

        @Test
        void createRejectsInvalidAddressType() throws Exception {
                mockMvc.perform(post("/api/v1/addresses")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "Bob",
                                                  "phone": "0901234567",
                                                  "provinceCode": "01",
                                                  "districtCode": "002",
                                                  "wardCode": "00019",
                                                  "detailAddress": "789 Elm St",
                                                  "type": "WAREHOUSE",
                                                  "isDefault": false
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));

                verifyNoInteractions(createAddressInputPort);
        }

        @Test
        void createWhenAddressLimitExceededReturns409() throws Exception {
                when(addressDtoMapper.toCreateCommand(eq("alice@example.com"), any(CreateAddressRequest.class)))
                                .thenReturn(new CreateAddressCommand("user-123", "Bob", "0901234567",
                                                "01", "002", "00019", "789 Elm St", AddressType.HOME, false));
                when(createAddressInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED));

                mockMvc.perform(post("/api/v1/addresses")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "Bob",
                                                  "phone": "0901234567",
                                                  "provinceCode": "01",
                                                  "districtCode": "002",
                                                  "wardCode": "00019",
                                                  "detailAddress": "789 Elm St",
                                                  "type": "HOME",
                                                  "isDefault": false
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_108"));
        }

        @Test
        void updateWhenAddressNotFoundReturns404() throws Exception {
                when(addressDtoMapper.toUpdateCommand(eq("alice@example.com"), eq("addr-missing"),
                                any(UpdateAddressRequest.class)))
                                .thenReturn(new UpdateAddressCommand("user-123", "addr-missing", "Bob", "0901234567",
                                                "01", "002", "00019", "789 Elm St", AddressType.HOME));
                when(updateAddressInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.ADDRESS_NOT_FOUND));

                mockMvc.perform(put("/api/v1/addresses/addr-missing")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "contactName": "Bob",
                                                  "phone": "0901234567",
                                                  "provinceCode": "01",
                                                  "districtCode": "002",
                                                  "wardCode": "00019",
                                                  "detailAddress": "789 Elm St",
                                                  "type": "HOME"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_302"));
        }

        @Test
        void deleteWhenDefaultAddressReturns409() throws Exception {
                when(addressDtoMapper.toDeleteCommand("alice@example.com", "addr-default"))
                                .thenReturn(new DeleteAddressCommand("user-123", "addr-default"));
                doThrow(new IdentityException(IdentityErrorCode.DEFAULT_ADDRESS_CANNOT_BE_DELETED))
                                .when(deleteAddressInputPort).execute(any());

                mockMvc.perform(delete("/api/v1/addresses/addr-default")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_303"));
        }

        @Test
        void setDefaultWhenAddressNotFoundReturns404() throws Exception {
                when(addressDtoMapper.toSetDefaultCommand("alice@example.com", "addr-missing"))
                                .thenReturn(new SetDefaultAddressCommand("user-123", "addr-missing"));
                when(setDefaultAddressInputPort.execute(any()))
                                .thenThrow(new IdentityException(IdentityErrorCode.ADDRESS_NOT_FOUND));

                mockMvc.perform(patch("/api/v1/addresses/addr-missing/default")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode").value("IDENTITY_302"));
        }

        @Test
        void unauthorizedRequestReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/addresses"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(listAddressesQueryPort);
        }
}
