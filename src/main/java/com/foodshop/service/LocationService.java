package com.foodshop.service;

import com.foodshop.dto.response.DistrictResponse;
import com.foodshop.dto.response.ProvinceResponse;
import com.foodshop.dto.response.WardResponse;

import java.util.List;

public interface LocationService {
    List<ProvinceResponse> getAllProvinces();
    List<DistrictResponse> getDistrictsByProvince(String provinceCode);
    List<WardResponse> getWardsByDistrict(String districtCode);
}
