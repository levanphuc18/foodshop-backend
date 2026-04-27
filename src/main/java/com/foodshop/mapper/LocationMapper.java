package com.foodshop.mapper;

import com.foodshop.dto.response.DistrictResponse;
import com.foodshop.dto.response.ProvinceResponse;
import com.foodshop.dto.response.WardResponse;
import com.foodshop.entity.District;
import com.foodshop.entity.Province;
import com.foodshop.entity.Ward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    ProvinceResponse toProvinceResponse(Province province);

    @Mapping(source = "province.code", target = "provinceCode")
    DistrictResponse toDistrictResponse(District district);

    @Mapping(source = "district.code", target = "districtCode")
    WardResponse toWardResponse(Ward ward);
}
