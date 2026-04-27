package com.foodshop.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvinceResponse {
    private String code;
    private String name;
    private String fullName;
    private String codeName;
}
