package com.cirf.dashboard.domain.scan.service.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AwsRegion Enum 테스트")
class AwsRegionTest {

    @Test
    @DisplayName("모든 AWS 리전을 조회한다")
    void getAllRegions_ReturnsAllRegions() {
        // when
        List<Region> regions = AwsRegion.getAllRegions();

        // then
        assertThat(regions).isNotEmpty();
        assertThat(regions).hasSize(AwsRegion.values().length);
        assertThat(regions).contains(Region.US_EAST_1, Region.US_WEST_2, Region.AP_NORTHEAST_2);
    }

    @Test
    @DisplayName("Region 객체로 AwsRegion을 찾는다")
    void fromRegion_ValidRegion_ReturnsAwsRegion() {
        // given
        Region usEast1 = Region.US_EAST_1;

        // when
        AwsRegion awsRegion = AwsRegion.fromRegion(usEast1);

        // then
        assertThat(awsRegion).isEqualTo(AwsRegion.US_EAST_1);
        assertThat(awsRegion.getRegion()).isEqualTo(usEast1);
        assertThat(awsRegion.getDescription()).isEqualTo("US East (N. Virginia)");
    }

    @Test
    @DisplayName("유효하지 않은 Region으로 찾을 때 예외 발생")
    void fromRegion_InvalidRegion_ThrowsException() {
        // given
        Region unknownRegion = Region.of("unknown-region");

        // when & then
        assertThatThrownBy(() -> AwsRegion.fromRegion(unknownRegion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown region");
    }

    @Test
    @DisplayName("리전 이름으로 유효성을 검증한다")
    void isValidRegion_ValidRegionName_ReturnsTrue() {
        // when & then
        assertThat(AwsRegion.isValidRegion("us-east-1")).isTrue();
        assertThat(AwsRegion.isValidRegion("ap-northeast-2")).isTrue();
        assertThat(AwsRegion.isValidRegion("eu-west-1")).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 리전 이름은 false를 반환한다")
    void isValidRegion_InvalidRegionName_ReturnsFalse() {
        // when & then
        assertThat(AwsRegion.isValidRegion("invalid-region")).isFalse();
        assertThat(AwsRegion.isValidRegion("us-east-99")).isFalse();
        assertThat(AwsRegion.isValidRegion("")).isFalse();
        assertThat(AwsRegion.isValidRegion(null)).isFalse();
    }

    @Test
    @DisplayName("각 AwsRegion이 올바른 Region과 설명을 가진다")
    void awsRegion_HasCorrectRegionAndDescription() {
        // given
        AwsRegion usEast1 = AwsRegion.US_EAST_1;
        AwsRegion apNortheast2 = AwsRegion.AP_NORTHEAST_2;
        AwsRegion euWest1 = AwsRegion.EU_WEST_1;

        // then
        assertThat(usEast1.getRegion()).isEqualTo(Region.US_EAST_1);
        assertThat(usEast1.getDescription()).isEqualTo("US East (N. Virginia)");

        assertThat(apNortheast2.getRegion()).isEqualTo(Region.AP_NORTHEAST_2);
        assertThat(apNortheast2.getDescription()).isEqualTo("Asia Pacific (Seoul)");

        assertThat(euWest1.getRegion()).isEqualTo(Region.EU_WEST_1);
        assertThat(euWest1.getDescription()).isEqualTo("Europe (Ireland)");
    }

    @Test
    @DisplayName("모든 enum 값이 유니크한 Region을 가진다")
    void allEnumValues_HaveUniqueRegions() {
        // when
        List<Region> regions = AwsRegion.getAllRegions();

        // then
        assertThat(regions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("주요 AWS 리전들이 포함되어 있다")
    void awsRegion_ContainsMajorRegions() {
        // when
        List<Region> regions = AwsRegion.getAllRegions();

        // then
        assertThat(regions).contains(
                Region.US_EAST_1,      // Virginia
                Region.US_WEST_2,      // Oregon
                Region.EU_WEST_1,      // Ireland
                Region.AP_NORTHEAST_1, // Tokyo
                Region.AP_NORTHEAST_2, // Seoul
                Region.AP_SOUTHEAST_1  // Singapore
        );
    }
}