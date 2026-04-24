package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.DistrictResponse;
import com.foodshop.dto.response.ProvinceResponse;
import com.foodshop.dto.response.WardResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceResponse>> getProvinces() {
        return new ApiResponse<>(GlobalCode.SUCCESS, locationService.getAllProvinces());
    }

    @GetMapping("/districts/{provinceCode}")
    public ApiResponse<List<DistrictResponse>> getDistricts(@PathVariable String provinceCode) {
        return new ApiResponse<>(GlobalCode.SUCCESS, locationService.getDistrictsByProvince(provinceCode));
    }

    @GetMapping("/wards/{districtCode}")
    public ApiResponse<List<WardResponse>> getWards(@PathVariable String districtCode) {
        return new ApiResponse<>(GlobalCode.SUCCESS, locationService.getWardsByDistrict(districtCode));
    }
}
