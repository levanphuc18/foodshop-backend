package com.foodshop.repository;

import com.foodshop.entity.District;
import com.foodshop.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findAllByDistrict(District district);
}
