package com.cirf.dashboard.domain.scan.service.enums;

import lombok.Getter;
import software.amazon.awssdk.regions.Region;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum AwsRegion {
    US_EAST_1(Region.US_EAST_1, "US East (N. Virginia)"),
    US_EAST_2(Region.US_EAST_2, "US East (Ohio)"),
    US_WEST_1(Region.US_WEST_1, "US West (N. California)"),
    US_WEST_2(Region.US_WEST_2, "US West (Oregon)"),
    AF_SOUTH_1(Region.AF_SOUTH_1, "Africa (Cape Town)"),
    AP_EAST_1(Region.AP_EAST_1, "Asia Pacific (Hong Kong)"),
    AP_SOUTH_1(Region.AP_SOUTH_1, "Asia Pacific (Mumbai)"),
    AP_NORTHEAST_1(Region.AP_NORTHEAST_1, "Asia Pacific (Tokyo)"),
    AP_NORTHEAST_2(Region.AP_NORTHEAST_2, "Asia Pacific (Seoul)"),
    AP_NORTHEAST_3(Region.AP_NORTHEAST_3, "Asia Pacific (Osaka)"),
    AP_SOUTHEAST_1(Region.AP_SOUTHEAST_1, "Asia Pacific (Singapore)"),
    AP_SOUTHEAST_2(Region.AP_SOUTHEAST_2, "Asia Pacific (Sydney)"),
    CA_CENTRAL_1(Region.CA_CENTRAL_1, "Canada (Central)"),
    EU_CENTRAL_1(Region.EU_CENTRAL_1, "Europe (Frankfurt)"),
    EU_WEST_1(Region.EU_WEST_1, "Europe (Ireland)"),
    EU_WEST_2(Region.EU_WEST_2, "Europe (London)"),
    EU_WEST_3(Region.EU_WEST_3, "Europe (Paris)"),
    EU_SOUTH_1(Region.EU_SOUTH_1, "Europe (Milan)"),
    EU_NORTH_1(Region.EU_NORTH_1, "Europe (Stockholm)"),
    ME_SOUTH_1(Region.ME_SOUTH_1, "Middle East (Bahrain)"),
    SA_EAST_1(Region.SA_EAST_1, "South America (São Paulo)");

    private final Region region;
    private final String description;

    AwsRegion(Region region, String description) {
        this.region = region;
        this.description = description;
    }

    public static List<Region> getAllRegions() {
        return Arrays.stream(values())
                .map(AwsRegion::getRegion)
                .collect(Collectors.toList());
    }

    public static AwsRegion fromRegion(Region region) {
        return Arrays.stream(values())
                .filter(awsRegion -> awsRegion.getRegion().equals(region))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown region: " + region.id()));
    }

    public static boolean isValidRegion(String regionName) {
        return Arrays.stream(values())
                .anyMatch(awsRegion -> awsRegion.getRegion().id().equals(regionName));
    }
}
