package com.foodshop.repository;

import com.foodshop.entity.District;
import com.foodshop.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, String> {
    List<District> findAllByProvince(Province province);
}
