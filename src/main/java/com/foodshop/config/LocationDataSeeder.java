package com.foodshop.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodshop.entity.District;
import com.foodshop.entity.Province;
import com.foodshop.entity.Ward;
import com.foodshop.repository.DistrictRepository;
import com.foodshop.repository.ProvinceRepository;
import com.foodshop.repository.WardRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LocationDataSeeder implements CommandLineRunner {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://provinces.open-api.vn/api/?depth=3";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (provinceRepository.count() > 0) {
            log.info("Location data already exists. Skipping seeding.");
            return;
        }

        log.info("Starting to seed location data from {}...", API_URL);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ProvinceDTO[] provinceDTOs = objectMapper.readValue(response.body(), ProvinceDTO[].class);
            
            List<Province> provinces = new ArrayList<>();
            List<District> allDistricts = new ArrayList<>();
            List<Ward> allWards = new ArrayList<>();

            for (ProvinceDTO pDto : provinceDTOs) {
                Province province = Province.builder()
                        .code(String.valueOf(pDto.getCode()))
                        .name(pDto.getName())
                        .fullName(pDto.getFullName())
                        .codeName(pDto.getCodeName())
                        .build();
                provinces.add(province);

                if (pDto.getDistricts() != null) {
                    for (DistrictDTO dDto : pDto.getDistricts()) {
                        District district = District.builder()
                                .code(String.valueOf(dDto.getCode()))
                                .name(dDto.getName())
                                .fullName(dDto.getFullName())
                                .codeName(dDto.getCodeName())
                                .province(province)
                                .build();
                        allDistricts.add(district);

                        if (dDto.getWards() != null) {
                            for (WardDTO wDto : dDto.getWards()) {
                                Ward ward = Ward.builder()
                                        .code(String.valueOf(wDto.getCode()))
                                        .name(wDto.getName())
                                        .fullName(wDto.getFullName())
                                        .codeName(wDto.getCodeName())
                                        .district(district)
                                        .build();
                                allWards.add(ward);
                            }
                        }
                    }
                }
            }

            log.info("Saving {} provinces...", provinces.size());
            provinceRepository.saveAll(provinces);
            
            log.info("Saving {} districts...", allDistricts.size());
            districtRepository.saveAll(allDistricts);
            
            log.info("Saving {} wards...", allWards.size());
            wardRepository.saveAll(allWards);

            log.info("Location data seeding completed successfully!");
        } else {
            log.error("Failed to fetch location data. Status code: {}", response.statusCode());
        }
    }

    @Data
    private static class ProvinceDTO {
        private String name;
        private int code;
        @JsonProperty("division_type")
        private String divisionType;
        @JsonProperty("codename")
        private String codeName;
        @JsonProperty("phone_code")
        private int phoneCode;
        @JsonProperty("districts")
        private List<DistrictDTO> districts;

        public String getFullName() {
            return name; // Public API doesn't have full_name for provinces, name is enough
        }
    }

    @Data
    private static class DistrictDTO {
        private String name;
        private int code;
        @JsonProperty("division_type")
        private String divisionType;
        @JsonProperty("codename")
        private String codeName;
        @JsonProperty("province_code")
        private int provinceCode;
        @JsonProperty("wards")
        private List<WardDTO> wards;

        public String getFullName() {
            return name;
        }
    }

    @Data
    private static class WardDTO {
        private String name;
        private int code;
        @JsonProperty("division_type")
        private String divisionType;
        @JsonProperty("codename")
        private String codeName;
        @JsonProperty("district_code")
        private int districtCode;

        public String getFullName() {
            return name;
        }
    }
}
