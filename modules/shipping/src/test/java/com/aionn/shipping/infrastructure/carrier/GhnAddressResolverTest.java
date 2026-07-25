package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GhnAddressResolverTest {

    private static final String PROVINCE_PATH = "/shiip/public-api/master-data/province";
    private static final String DISTRICT_PATH = "/shiip/public-api/master-data/district";
    private static final String WARD_PATH = "/shiip/public-api/master-data/ward";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AddressLookupPort addressLookupPort;

    private GhnStubServer stub;
    private GhnAddressResolver resolver;

    @BeforeEach
    void setUp() {
        stub = GhnStubServer.start();
        GhnProperties properties = new GhnProperties(stub.baseUrl(), "test-token", "shop-9",
                1454, "21211", null, 2, 2, null, null, null);
        resolver = new GhnAddressResolver(properties, addressLookupPort, objectMapper);
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    private static ShipmentAddress address() {
        return new ShipmentAddress("Receiver", "0912345678", "12 Modulith Street",
                "VN-HN-HK-PX", "VN-HN-HK", "VN-HN", "VN");
    }

    private void vnResolves(String provinceName, String districtCode, String districtName,
            String wardCode, String wardName) {
        when(addressLookupPort.resolve(any(), any(), any())).thenReturn(Optional.of(
                new AddressLookupPort.ResolvedAddress("VN-HN", provinceName,
                        districtCode, districtName, wardCode, wardName)));
    }

    private void stubGhnMasterData(String provinces, String districts, String wards) {
        stub.stub(PROVINCE_PATH, provinces);
        stub.stub(DISTRICT_PATH, districts);
        stub.stub(WARD_PATH, wards);
    }

    private static String provinces() {
        return "{\"code\":200,\"data\":[{\"ProvinceID\":201,\"ProvinceName\":\"Hà Nội\"},"
                + "{\"ProvinceID\":202,\"ProvinceName\":\"Hồ Chí Minh\"}]}";
    }

    private static String districts() {
        return "{\"code\":200,\"data\":[{\"DistrictID\":1454,\"DistrictName\":\"Quận Hoàn Kiếm\","
                + "\"GovernmentCode\":\"VN-HN-HK\"},"
                + "{\"DistrictID\":1455,\"DistrictName\":\"Quận Ba Đình\"}]}";
    }

    private static String wards() {
        return "{\"code\":200,\"data\":[{\"WardCode\":\"21211\",\"WardName\":\"Phường Phúc Xá\","
                + "\"GovernmentCode\":\"VN-HN-HK-PX\"},"
                + "{\"WardCode\":\"21212\",\"WardName\":\"Phường Cửa Nam\"}]}";
    }

    // --- happy paths ----------------------------------------------------------

    @Test
    void resolvesProvinceDistrictAndWardByCode() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stubGhnMasterData(provinces(), districts(), wards());

        GhnAddressResolver.ResolvedGhn resolved = resolver.resolve(address());

        assertThat(resolved.provinceId()).isEqualTo(201);
        assertThat(resolved.districtId()).isEqualTo(1454);
        assertThat(resolved.wardCode()).isEqualTo("21211");
    }

    @Test
    void matchesByNameWhenGovernmentCodeIsAbsent() {
        vnResolves("Hà Nội", "NO-CODE", "Quận Ba Đình", "NO-CODE", "Phường Cửa Nam");
        stubGhnMasterData(provinces(), districts(), wards());

        GhnAddressResolver.ResolvedGhn resolved = resolver.resolve(address());

        assertThat(resolved.districtId()).isEqualTo(1455);
        assertThat(resolved.wardCode()).isEqualTo("21212");
    }

    @Test
    void matchingIgnoresDiacriticsCaseAndAdministrativePrefixes() {
        vnResolves("thanh pho Ha Noi", "NO-CODE", "HOAN KIEM", "NO-CODE", "phuc xa");
        stubGhnMasterData(provinces(), districts(), wards());

        GhnAddressResolver.ResolvedGhn resolved = resolver.resolve(address());

        assertThat(resolved.provinceId()).isEqualTo(201);
        assertThat(resolved.districtId()).isEqualTo(1454);
        assertThat(resolved.wardCode()).isEqualTo("21211");
    }

    @Test
    void matchingFoldsVietnameseDStrokeSoNamesLikeBaDinhResolve() {
        vnResolves("Ha Noi", "NO-CODE", "Ba Dinh", "NO-CODE", "Cua Nam");
        stubGhnMasterData(provinces(), districts(), wards());

        GhnAddressResolver.ResolvedGhn resolved = resolver.resolve(address());

        assertThat(resolved.districtId()).isEqualTo(1455);
    }

    @Test
    void matchesViaNameExtensionAliasWhenPrimaryNameDiffers() {
        vnResolves("Thu Do", "NO-CODE", "Hoan Kiem", "NO-CODE", "Phuc Xa");
        stubGhnMasterData(
                "{\"code\":200,\"data\":[{\"ProvinceID\":201,\"ProvinceName\":\"Hà Nội\","
                        + "\"NameExtension\":[\"Thủ Đô\",\"HN\"]}]}",
                districts(), wards());

        assertThat(resolver.resolve(address()).provinceId()).isEqualTo(201);
    }

    @Test
    void cachesMasterDataAcrossRepeatedResolutions() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stubGhnMasterData(provinces(), districts(), wards());

        resolver.resolve(address());
        resolver.resolve(address());
        resolver.resolve(address());

        assertThat(stub.hitsAt(PROVINCE_PATH)).isEqualTo(1);
        assertThat(stub.hitsAt(DISTRICT_PATH)).isEqualTo(1);
        assertThat(stub.hitsAt(WARD_PATH)).isEqualTo(1);
    }

    @Test
    void sendsTokenHeaderAndProvinceIdWhenFetchingDistricts() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stubGhnMasterData(provinces(), districts(), wards());

        resolver.resolve(address());

        assertThat(stub.headerReceivedAt(PROVINCE_PATH, "Token")).isEqualTo("test-token");
        assertThat(stub.bodyReceivedAt(DISTRICT_PATH)).contains("\"province_id\":201");
    }

    // --- failure paths --------------------------------------------------------

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("address is required")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void rejectsAddressUnknownToIdentityLookup() {
        when(addressLookupPort.resolve(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Unknown VN address")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void failsWhenGhnHasNoMatchingProvince() {
        vnResolves("Atlantis", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stubGhnMasterData(provinces(), districts(), wards());

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("no matching province")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }

    @Test
    void failsWhenGhnHasNoMatchingDistrict() {
        vnResolves("Hà Nội", "NO-CODE", "Quận Không Tồn Tại", "VN-HN-HK-PX", "Phường Phúc Xá");
        stubGhnMasterData(provinces(), districts(), wards());

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("no matching district");
    }

    @Test
    void failsWhenGhnHasNoMatchingWard() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "NO-CODE", "Phường Không Tồn Tại");
        stubGhnMasterData(provinces(), districts(), wards());

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("no matching ward");
    }

    @Test
    void failsWhenTargetNameIsBlankAndCodeDoesNotMatch() {
        vnResolves("", "NO-CODE", "", "NO-CODE", "");
        stubGhnMasterData(provinces(), districts(), wards());

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("no matching province");
    }

    @Test
    void surfacesCarrierBusinessErrorWhenMasterDataCallFails() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stub.stub(PROVINCE_PATH, 200, "{\"code\":401,\"message\":\"invalid token\"}");

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("invalid token")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }

    @Test
    void surfacesCarrierErrorOnHttpFailureStatus() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stub.stub(PROVINCE_PATH, 503, "{\"message\":\"upstream down\"}");

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("upstream down");
    }

    @Test
    void treatsNonArrayDataAsEmptyMasterDataList() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stub.stub(PROVINCE_PATH, "{\"code\":200,\"data\":{\"unexpected\":\"shape\"}}");

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("no matching province");
    }

    @Test
    void wrapsMalformedMasterDataResponse() {
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");
        stub.stub(PROVINCE_PATH, "not-json");

        assertThatThrownBy(() -> resolver.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("GHN provinces error");
    }

    @Test
    void wrapsTransportFailure() {
        GhnProperties unreachable = new GhnProperties("http://127.0.0.1:1", "token", "shop-9",
                1454, "21211", null, 2, 2, null, null, null);
        GhnAddressResolver offline = new GhnAddressResolver(unreachable, addressLookupPort, objectMapper);
        vnResolves("Hà Nội", "VN-HN-HK", "Quận Hoàn Kiếm", "VN-HN-HK-PX", "Phường Phúc Xá");

        assertThatThrownBy(() -> offline.resolve(address()))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }
}
