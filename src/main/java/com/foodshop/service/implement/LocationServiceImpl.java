package com.foodshop.service.implement;

import com.foodshop.dto.response.DistrictResponse;
import com.foodshop.dto.response.ProvinceResponse;
import com.foodshop.dto.response.WardResponse;
import com.foodshop.entity.District;
import com.foodshop.entity.Province;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.LocationMapper;
import com.foodshop.repository.DistrictRepository;
import com.foodshop.repository.ProvinceRepository;
import com.foodshop.repository.WardRepository;
import com.foodshop.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final LocationMapper locationMapper;

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAll().stream()
                .map(locationMapper::toProvinceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvince(String provinceCode) {
        Province province = provinceRepository.findById(provinceCode)
                .orElseThrow(() -> new GlobalException(GlobalCode.NOT_FOUND, "Province not found with code: " + provinceCode));
        
        return districtRepository.findAllByProvince(province).stream()
                .map(locationMapper::toDistrictResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWardsByDistrict(String districtCode) {
        District district = districtRepository.findById(districtCode)
                .orElseThrow(() -> new GlobalException(GlobalCode.NOT_FOUND, "District not found with code: " + districtCode));
        
        return wardRepository.findAllByDistrict(district).stream()
                .map(locationMapper::toWardResponse)
                .collect(Collectors.toList());
    }
}
